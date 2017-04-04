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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.Queue;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.Image;

import org.apache.log4j.Logger;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.graphics.Bitmap;

import de.itd.mapsforge.javafx.maps.graphics.FXBitmap;
import de.itd.mapsforge.javafx.maps.mapgenerator.MapGeneratorJob;
import de.itd.mapsforge.javafx.maps.mapgenerator.TileCache;

public class FileTileCache implements TileCache {
	
	public static final String FILE_ENDING	= ".tile";
	
	private String	directory;
	private boolean keepRunning;
	
	private Runnable completeListener;
	private Queue<FileTileCacheEntry>	toWrite = new LinkedList<>();
	private Queue<FileTileCacheEntry>	toRead	= new LinkedList<>();
	
	private Logger			logger		= null;
	private IntegerProperty	capacity	= new SimpleIntegerProperty(Integer.MAX_VALUE);
	private IntegerProperty load		= new SimpleIntegerProperty(0);
	
	private Thread writer = null;
	private Thread reader = null;
	
	
	public FileTileCache (String directory) {
		this.directory		= directory;
		this.keepRunning	= true;
		this.logger			= Logger.getLogger(getClass());
		
		(writer = new Thread(getClass().getSimpleName()+"-Writer"){
			@Override
			public void run() {
				threadWriter();
			}
		}).start();;
		
		(reader = new Thread(getClass().getSimpleName()+"-Reader"){
			public void run() {
				threadReader();
			}
		}).start();
	}
	
	/**
	 * Whether the {@link Thread}s should keep running
	 */
	private synchronized boolean keepRunning () {
		return keepRunning;
	}
	
	/**
	 * @param keepRunning Whether to keep the {@link Thread}s running
	 */
	private synchronized void setKeepRunning (boolean keepRunning) {
		this.keepRunning = keepRunning;
	}
	
	/**
	 * This method should be called by the {@link Thread}(s)
	 * that should write the entries
	 */
	private void threadWriter () {
		// declare the variables once, so it doesn't need to be done in each loop
		FileOutputStream 	fos = null;
		MapGeneratorJob		job	= null;
		File				dst	= null;
		
		while (keepRunning()) {
			// initialize
			FileTileCacheEntry entry = null;
			
			synchronized (toWrite) {
				// nothing to do?
				if (toWrite.isEmpty()) {
					try { toWrite.wait(); } catch (Throwable t) {}
					continue;
				}
				
				// something available
				entry = toWrite.poll();
			}
			

			
			
			// can be written?
			if (entry.canWrite()) {
				try {

					// be sure the file is allowed to be created
					freeSlots(1);
					
					
					// set the variables
					job = entry.getMapGeneratorJob();
					dst = getFile(job.tile);
					fos = new FileOutputStream(dst);
					
					// write
					entry.write(fos);
					
				} catch (Throwable t) {
					logger.error("Couldn't read tile from frile", t);
					
				} finally {
					// flush and - what is actually really important - close the stream
					try { fos.flush(); } catch (Throwable t) {}
					try { fos.close(); } catch (Throwable t) {}
				}
			}
		}
	}
	
	/**
	 * This method should be called by the {@link Thread}(s)
	 * that should load the requested entries
	 */
	private void threadReader () {
		// declare the variables once, so it doesn't need to be done in each loop
		FileInputStream	fis	= null;
		MapGeneratorJob	job	= null;
		File			src	= null;
		
		while (keepRunning()) {
			// initialize
			FileTileCacheEntry entry = null;
			
			synchronized (toRead) {
				// nothing to do?
				if (toRead.isEmpty()) {
					try { toRead.wait(); } catch (Throwable t) {}
					continue;
				}
				
				// something available
				entry = toRead.poll();
			}
			
			// has been written before?
			if (entry.isWritten()) {
				
				try {
					job = entry.getMapGeneratorJob();
					src = getFile(job.tile);
					fis = new FileInputStream(src);
					
					// read the image
					entry.read(fis);
					
					// contact the listener
					if (completeListener != null) {
						completeListener.run();
					}
					
				} catch (Throwable t) {
					logger.error("Couldn't write tile to file", t);
					
				} finally {
					// be sure, that the stream is closed
					try { fis.close(); } catch (Throwable t) {}
				}
				
			} else {
				// add it on top, to be read later, when it has been written
				synchronized (toRead) {
					toRead.add(entry);
				}
			}
		}
	}
	
	/**
	 * The {@link Runnable} to execute (from different {@link Thread}s)
	 * if an {@link Image} has been completely loaded
	 * @param runnable {@link Runnable} to set
	 */
	public void setOnLoadCompleteListener (Runnable runnable) {
		this.completeListener = runnable;
	}
	

	@Override
	public boolean containsKey(MapGeneratorJob mapGeneratorJob) {
		return getFile(mapGeneratorJob.tile).exists();
	}

	@Override
	public void destroy() {
		// forbid the thread to keep running
		setKeepRunning(false);
		
		// notify the reader, if it is waiting
		synchronized (toWrite) {
			toWrite.notifyAll();
		}
		
		// notify the writer, if it is waiting
		synchronized (toRead) {
			toRead.notifyAll();
		}

		// wait until the thread have ended
		try { reader.join(); } catch (Throwable t) { t.printStackTrace(); }
		try { writer.join(); } catch (Throwable t) { t.printStackTrace(); }
		

		// delete all files in the directory that ends with .tile
		File dir = new File(directory);
		
		if (dir.exists() && dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				if (file.getAbsolutePath().endsWith(FILE_ENDING)) {
					file.delete();
				}
			}
		}
	}
	
	private FileTileCacheEntry getEntry (MapGeneratorJob job) {
		// create a new one
		FileTileCacheEntry entryNew = new FileTileCacheEntry(job);
		
		// check whether there is already a entry
		synchronized (toWrite) {
			for (FileTileCacheEntry entry : toWrite) {
				if (entry != null && entry.equals(entryNew)) {
					return entry;
				}
			}
		}
		
		// its new, return the new one
		return entryNew;
	}

	@Override
	public FXBitmap get(MapGeneratorJob mapGeneratorJob) {
		// checks and creates the new entry (if allowed)
		if (!containsKey(mapGeneratorJob)) {
			return null;
		}
		
		// get the entry
		FileTileCacheEntry 		entry	= getEntry(mapGeneratorJob);
		Image					image	= entry != null ? entry.getImage() : null;
		
		if (entry != null && image != null) {
			// add it to the queue to be read
			synchronized (toRead) {
				if (!toRead.contains(entry)) {
					toRead.add(entry);
					toRead.notifyAll();
				}
			}
			
			// return the wrapper
			return new FXBitmap(image);
		}
		
		return null;
	}
	
	/**
	 * @return The current load, including the pending write requests
	 */
	public int getLoad () {
		synchronized (load) {
			return load.get();
		}
	}

	@Override
	public int getCapacity() {
		synchronized (capacity) {
			return capacity.get();
		}
	}

	@Override
	public boolean isPersistent() {
		return true;
	}
	
	/**
	 * @param tile {@link Tile} to get the {@link File} for
	 * @return The {@link File} for the given {@link Tile}
	 */
	private File getFile (Tile tile) {
		return new File(directory, getFileName(tile));
	}
	
	/**
	 * @param tile {@link Tile} to get the filename for
	 * @return The filename for the given {@link Tile}
	 */
	private String getFileName (Tile tile) {
		return ("x="+tile.tileX+",y="+tile.tileY+",z="+((int)tile.zoomLevel)) + FILE_ENDING;
	}

	@Override
	public synchronized void put(MapGeneratorJob job, Bitmap bitmap) {
		if (bitmap instanceof FXBitmap) {
			// do not overwrite
			if (containsKey(job)) {
				return;
			}
			
			// create and add to the queue to be written
			synchronized (toWrite) {
				toWrite.add( new FileTileCacheEntry(job, ((FXBitmap)bitmap).image) );
				toWrite.notifyAll();
			}
		}
	}
	
	/**
	 * Deletes as many files as needed, until
	 * there can be created the given amount of files
	 * @param amount Amount of files that should be able to create
	 */
	private void freeSlots (int amount) {
		// update the current load
		updateLoad();
		
		// cannot free more space than the maximum size
		if (amount > getCapacity()) {
			amount = getCapacity();
		}
		
		// while there isn't enough space
		while (getLoad()+amount > getCapacity()) {
			if (!deleteOldest()) {
				throw new OutOfMemoryError("Limit reached, couldn't free slots");
			}
			
			// update
			updateLoad();
		}
	}
	
	/**
	 * Updates the current load, based on the amount of files in the
	 * storage directory that match with their name endings
	 */
	private void updateLoad () {
		synchronized (load) {
			load.set(new File(directory).list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(FILE_ENDING);
				}
			}).length);
		}
	}
	
	/**
	 * Tries to delete the oldest file
	 * @return Whether a {@link File} was deleted or not
	 */
	private boolean deleteOldest () {
		File oldestFile = null;
		long oldestDate = Long.MAX_VALUE;
		
		for (File file : new File(directory).listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(FILE_ENDING);
			}
		})) {
			
			// is older?
			if (file.lastModified() < oldestDate) {
				oldestFile = file;
				oldestDate = file.lastModified();
			}
			
		}
		
		// remove the oldest if found
		return (oldestFile != null && oldestFile.delete());
	}

	@Override
	public void setCapacity(int capacity) {
		synchronized (this.capacity) {
			this.capacity.set(capacity);
		}
	}

	@Override
	public void setPersistent(boolean persistent) {
		// do nothing
	}
	
	/**
	 * @return The {@link IntegerProperty} for the capacity of this {@link FileTileCache}
	 */
	public IntegerProperty capacityProperty () {
		return capacity;
	}
	
	/**
	 * @return The {@link ReadOnlyIntegerProperty} for the current load of this {@link FileTileCache}
	 */
	public ReadOnlyIntegerProperty loadProperty () {
		return load;
	}
}
