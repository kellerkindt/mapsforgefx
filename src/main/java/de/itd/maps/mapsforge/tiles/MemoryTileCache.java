 /*
 * Copyright (c) 2013 Michael Watzko and IT-Designers GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.itd.maps.mapsforge.tiles;
 
import java.util.LinkedList;
import java.util.Queue;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.graphics.Bitmap;

import de.itd.mapsforge.javafx.maps.graphics.FXBitmap;
import de.itd.mapsforge.javafx.maps.mapgenerator.MapGeneratorJob;
import de.itd.mapsforge.javafx.maps.mapgenerator.TileCache;

/**
 * @author Watzko, based on "http://code.google.com/p/mapsforge/"
 */
public class MemoryTileCache implements TileCache {
	
	private IntegerProperty	load			= new SimpleIntegerProperty();
	private IntegerProperty	capacity		= new SimpleIntegerProperty();
	private BooleanProperty useFileCache	= new SimpleBooleanProperty(true);
	
	private double			clearFact;
	private boolean			autoClear;
	
	private ObservableMap<MapGeneratorJob, Image>	images	= FXCollections.observableHashMap();
	private Queue<MapGeneratorJob>					queue	= new LinkedList<MapGeneratorJob>();	// queue to find the oldest jobs to remove
	
	private FileTileCache	fileCache	= null;
	
	/**
	 * Initializes this {@link TileCache} with a capacity of 0
	 */
	public MemoryTileCache () {
		this(0);
	}
	
	/**
	 * @param capacity The amount of {@link Tile}s to cache
	 */
	public MemoryTileCache(int capacity) {
		this (capacity, true, .15, true);
	}
	
	/**
	 * @param fileCache {@link FileTileCache} to use
	 */
	public void setFileCache (FileTileCache fileCache) {
		this.fileCache = fileCache;
	}
 
	/**
	 * @param capacity		The amount of {@link Tile}s to cache
	 * @param autoCleanup	Whether to cleanup if the cache is full - will automatically remove more than needed {@link Image}s if full
	 * @param clearFact		The % amount of elements to remove on a cleanup
	 * @param useFileCache	Whether to use the {@link FileTileCache}
	 */
	public MemoryTileCache(int capacity, boolean autoClear, double clearFact, boolean useFileCache) {
		// check whether the capacity is valid
		checkCapacity(capacity);

		this.autoClear	= autoClear;
		this.clearFact	= clearFact;
		this.useFileCache.set(useFileCache);

		// listener to react on capacity changes
		this.capacity.set(capacity);
		this.capacity.addListener(new InvalidationListener() {
			
			@Override
			public void invalidated(Observable observable) {
				remove(0);
				images = FXCollections.observableMap(images);
				System.gc();
			}
		});
		
		
		// listener to update the usage, there is no property for the size :(
		this.images.addListener(new InvalidationListener() {
			
			@Override
			public void invalidated(Observable observable) {
				if (load.get() != images.size()) {
					load.set(images.size());
				}
			}
		});
	}
 
	
	@Override
	public synchronized boolean containsKey(MapGeneratorJob mapGeneratorJob) {
		 return images.containsKey(mapGeneratorJob);
	}
 
	@Override
	public synchronized void destroy() {
		images	.clear();
		queue	.clear();
	}
 
	@Override
	public synchronized Bitmap get(MapGeneratorJob job) {
		// update the job state
		queue.remove(job);
		queue.add	(job);		
		
		// just return the value
		return new FXBitmap(images.get(job));
	}
	
	
	/**
	 * @return The {@link ReadOnlyIntegerProperty} to get the actual load of this {@link TileCache}
	 */
	public ReadOnlyIntegerProperty loadProperty () {
		return load;
	}
	
	/**
	 * @return The {@link IntegerProperty} for the capacity of this {@link TileCache}
	 */
	public IntegerProperty capacityProperty() {
		return capacity;
	}
	
	/**
	 * @return The {@link BooleanProperty} that describes whether the {@link FileTileCache} is allowed to be used
	 */
	public BooleanProperty useFileTileCacheProperty () {
		return useFileCache;
	}
 
	@Override
	public int getCapacity() {
	  return this.capacity.get();
	}
 
	@Override
	public boolean isPersistent() {
	  return false;
	}
	
	/**
	 * Removes one ore more {@link Image}s from the cache
	 */
	private void removeOldest () {
		// get the amount to remove
		int toRemove = autoClear ? (int)(getCapacity() * clearFact) : 1;
		
		// has to be at least one
		if (toRemove < 1) {
			toRemove = 1;
		}
		
		// remove them
		for (int i = 0; i < toRemove; i++) {
			
			MapGeneratorJob job		= queue.poll();
			Image			image	= images.remove(job);
			
			
			// add it to the file-cache if available
			if (fileCache != null && image != null && useFileCache.get()) {
				fileCache.put(job, new FXBitmap(image));
			}
		}
	}
	
	/**
	 * Removes all entries
	 */
	public void clear () {
		queue	.clear();
		images	.clear();
	}
	
	/**
	 * Removes as many entries as needed to have the given amount of free slots
	 * @param amount Amount of slots to free
	 */
	public void remove (int amount) {
		// check if not valid
		if (amount >= capacity.get()) {
			amount = capacity.get();
		}
		
		// remove while there are to many
		while (images.size() > capacity.get()-amount) {
			
			// remove the oldest
			removeOldest();
		}
	}
 
	@Override
	public synchronized void put(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		Image image = ((FXBitmap)bitmap).image;
		
		// be sure at least one slot is free - as we want to add one image
		remove(1);
		
		// do not have a job listed twice
		queue.remove(mapGeneratorJob);
		
		// put the new one in the cache
		queue .add(mapGeneratorJob);
		images.put(mapGeneratorJob, image);
	}
 
	@Override
	public void setCapacity(int capacity) {
		this.capacity.set(capacity);
	}
 
	@Override
	public void setPersistent(boolean persistent) {
		throw new UnsupportedOperationException();
	}
	

	/**
	 * Checks whether the given capacity is negative,
	 * if so, it throws an {@link IllegalArgumentException},
	 * if not, the given capacity is returned
	 * @param capacity The capacity to check
	 * @return The given capacity
	 * @throws IllegalArgumentException If the capacity is negative
	 */
	private static int checkCapacity(int capacity) throws IllegalArgumentException {
	  if (capacity < 0) {
		 throw new IllegalArgumentException("capacity must not be negative: " + capacity);
	  }
	  return capacity;
	}
 }