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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import org.apache.log4j.Logger;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import de.itd.maps.mapsforge.MapView;
import de.itd.maps.mapsforge.properties.GraphicsProperties;
import de.itd.mapsforge.javafx.maps.DebugSettings;
import de.itd.mapsforge.javafx.maps.graphics.FXBitmap;
import de.itd.mapsforge.javafx.maps.mapgenerator.JobParameters;
import de.itd.mapsforge.javafx.maps.mapgenerator.MapGeneratorJob;
import de.itd.mapsforge.javafx.maps.mapgenerator.TileCache;
import de.itd.mapsforge.javafx.maps.mapgenerator.databaserenderer.DatabaseRenderer;

/**
 * Base on the class "MapView extends ViewGroup" form the mapforge package,
 * starts the process for generating and showing the map
 * 
 * @author P. Trumpp, Watzko and unknown ("http://code.google.com/p/mapsforge/")
 * @version 1.0 11.04.2013
 */
public class ExecuteMapforge {

	private FileTileCache	fileCache;
	private MemoryTileCache	memoryTileCache;
	private JobParameters 	jobParameters;

	private DatabaseRenderer 	databaseRenderer	= null;
	private MapDatabase 		mapDatabase 		= new MapDatabase();
	private File 				mapFile 			= null;

	private Queue<MapGeneratorJob> jobs = new LinkedList<>();

	private DoubleProperty offsetX = new SimpleDoubleProperty(0);
	private DoubleProperty offsetY = new SimpleDoubleProperty(0);

	private DebugSettings	debugSettings;
	private Canvas 			canvas;
	private Image 			emptyImage;
	private GraphicsContext graphics;
	private MapView 		mapView;

	private long tileLeft 	= 0;
	private long tileTop 	= 0;
	private long tileRight 	= 0;
	private long tileBottom = 0;

	private Runnable runnable;
	private Runnable redrawListener;
	
	private Logger				logger		= null;
	private GraphicsProperties	properties	= null;
	
	// whether the drawing of tiles is currently paused
	private boolean				pauseTileDrawing	= false;
	private boolean				isDestroyed			= false;

	public ExecuteMapforge(Canvas canvas, MapView info, XmlRenderTheme renderTheme, GraphicsProperties graphicsProperties) {
		
		this.logger		= Logger.getLogger(getClass());
		this.properties	= graphicsProperties;
		
		this.canvas		= canvas;
		this.graphics	= canvas.getGraphicsContext2D();
		
		
		// if the TileCache changes, just redraw
		this.redrawListener = new Runnable() {
			@Override
			public void run() {
				// be sure this is called in the FX-Thread
				if (!Platform.isFxApplicationThread()) {
					Platform.runLater(redrawListener);
					return;
				}
				
				// redraw the tiles if in FX-Thread
				redrawTiles(false);
			}
		};

		this.debugSettings		= new DebugSettings(false, false, false);
		this.databaseRenderer 	= new DatabaseRenderer(mapDatabase);

		this.fileCache			= new FileTileCache(properties.getFileTileCachePath());
		this.fileCache.setOnLoadCompleteListener(redrawListener);
		
		this.memoryTileCache	= new MemoryTileCache(properties.getMemoryTileCacheCapacity());
		this.memoryTileCache.setFileCache(fileCache);

		this.jobParameters		= new JobParameters(renderTheme, 1.0F);
		this.mapView 			= info;


		// create the image, that should be shown while the actual image is
		// rendered / in queue
		Canvas canvasDummy = new Canvas(Tile.TILE_SIZE, Tile.TILE_SIZE);
		GraphicsContext canvasGraphics = canvasDummy.getGraphicsContext2D();

		canvasGraphics.setFill(Color.LIGHTGRAY);
		canvasGraphics.fillRect(0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);

		emptyImage = canvasDummy.snapshot(null, null);
	}
	
	/**
	 * @return The {@link LiveRenderRule} to modify what to render live
	 */
	public LiveRenderRule getLiveRenderRule () {
		return databaseRenderer.getRenderRule();
	}

	/**
	 * Loads the given {@link File}
	 * 
	 * @param file
	 *            {@link File} to load from
	 */
	public void load(File file) {
		// save the file
		this.mapFile = file;

		// open the file
		FileOpenResult result = mapDatabase.openFile(mapFile);
		
		// log the result
		logger.debug("Opened file="+file.getAbsolutePath()+", succeeded="+result.isSuccess()+", errorMessage="+result.getErrorMessage());
		
		// stop, if file couldn't be opened
		if (!result.isSuccess()) {
			throw new RuntimeException("Couldn't open file, errorMessage="+(result != null ? result.getErrorMessage() : null));
		}
		
		

		// redraw after load
		redrawTiles(true);
		execute();
	}
	
	/**
	 * @return Whether there is already a file loaded
	 */
	public boolean hasLoaded () {
		return mapDatabase.hasOpenFile();
	}

	/**
	 * Sets the {@link MapPosition} of the center of the canvas
	 * 
	 * @param latitude
	 *            Latitude coordinate to set
	 * @param longitude
	 *            Longitude coordinate to set
	 * @param zoom
	 *            Zoom level to set
	 */
	public void setMapPosition(double latitude, double longitude, byte zoom) {
		mapView.set(latitude, longitude);
		mapView.setZoomLevel(zoom);
	}

	/**
	 * Sets the {@link MapPosition} of the center of the canvas
	 * 
	 * @param latitude
	 *            Latitude coordinate to set
	 * @param longitude
	 *            Longitude coordinate to set
	 */
	public void setMapPosition(double latitude, double longitude) {
		setMapPosition(latitude, longitude, mapView.getZoomLevel());
	}
	
	
	
	private void drawBackground () {
		
		// TODO use latest zoom level not +/-1
		
		// get the center of the canvas
		offsetX.set(canvas.getWidth() / 2);
		offsetY.set(canvas.getHeight() / 2);

		// convert from relative to the MapView(at center of the Canvas) to
		// relative to the Canvas (0,0)
		double 	pixelX 	= mapView.getX() - offsetX.get();
		double 	pixelY 	= mapView.getY() - offsetY.get();
		byte	zoom	= (byte)(mapView.getZoomLevel()+1);// TODO

		tileLeft 	= MercatorProjection.pixelXToTileX	(pixelX, zoom);
		tileTop 	= MercatorProjection.pixelYToTileY	(pixelY, zoom);
		tileRight 	= MercatorProjection.pixelXToTileX	(pixelX + canvas.getWidth(),  zoom);
		tileBottom 	= MercatorProjection.pixelYToTileY	(pixelY + canvas.getHeight(), zoom);
		
//		final double tileXCenter	= MercatorProjection.pixelXToTileX(pixelX+canvas.getWidth() /2, zoom);
//		final double tileYCenter	= MercatorProjection.pixelXToTileX(pixelY+canvas.getHeight()/2, zoom);

		int tileWidth	= Tile.TILE_SIZE;
		int tileHeight	= Tile.TILE_SIZE;

		int offSetX = -(int) (pixelX % tileWidth);
		int offSetY = -(int) (pixelY % tileHeight);

		for (long tileY = tileTop; tileY <= tileBottom; tileY += 1L) {
			for (long tileX = tileLeft; tileX <= tileRight; tileX += 1L) {
				
				

				// get the tile and the job for the current x and y coordinate
				Tile			tile 			= new Tile				(tileX, tileY, zoom);
				MapGeneratorJob mapGeneratorJob = new MapGeneratorJob	(tile, mapFile, jobParameters, debugSettings);

				// the variable where the cached image is going to be placed
				Image image = null;

				// is there already a rendered image in the memory?
				if (this.memoryTileCache.containsKey(mapGeneratorJob)) {
					// get the image
					image = ((FXBitmap) this.memoryTileCache.get(mapGeneratorJob)).image;
				}
				
				
				// is there already a rendered image as file?
				else if (this.memoryTileCache.useFileTileCacheProperty().get() && this.fileCache.containsKey(mapGeneratorJob)) {
					
					// get the image
					image = this.fileCache.get(mapGeneratorJob).image;
					
					// add it to the memory cache
					this.memoryTileCache.put(mapGeneratorJob, new FXBitmap(image));
				}

				
				// draw the image if it was found
				if (image != null) {
					// draw it on the canvas
					graphics.drawImage(image, offSetX + tileWidth
							* (tileX - tileLeft), offSetY + tileHeight
							* (tileY - tileTop), tileWidth, tileHeight);
				}
				
			}
		}
		
		
	}
	

	/**
	 * Redraws the tiles and creates new jobs if needed
	 */
	public void redrawTiles() {
		redrawTiles(true);
	}

	/**
	 * Redraws the tiles and creates new jobs to create tiles if needed and requested
	 * 
	 * @param createJobs
	 *            Whether create new jobs if possible (will remove all old jobs)
	 */
	public void redrawTiles(boolean createJobs) {
		// list to add the new jobs temporarily
		List<MapGeneratorJob> jobs = new ArrayList<MapGeneratorJob>();
		
		drawBackground();
		
		// get the center of the canvas
		offsetX.set(canvas.getWidth() / 2);
		offsetY.set(canvas.getHeight() / 2);

		// convert from relative to the MapView(at center of the Canvas) to
		// relative to the Canvas (0,0)
		double pixelX = mapView.getX() - offsetX.get();
		double pixelY = mapView.getY() - offsetY.get();

		tileLeft 	= MercatorProjection.pixelXToTileX	(pixelX, mapView.getZoomLevel());
		tileTop 	= MercatorProjection.pixelYToTileY	(pixelY, mapView.getZoomLevel());
		tileRight 	= MercatorProjection.pixelXToTileX	(pixelX + canvas.getWidth(), mapView.getZoomLevel());
		tileBottom 	= MercatorProjection.pixelYToTileY	(pixelY + canvas.getHeight(), mapView.getZoomLevel());
		
		final double tileXCenter	= MercatorProjection.pixelXToTileX(pixelX+canvas.getWidth() /2, mapView.getZoomLevel());
		final double tileYCenter	= MercatorProjection.pixelXToTileX(pixelY+canvas.getHeight()/2, mapView.getZoomLevel());

		int tileWidth	= Tile.TILE_SIZE;
		int tileHeight	= Tile.TILE_SIZE;

		int offSetX = -(int) (pixelX % tileWidth);
		int offSetY = -(int) (pixelY % tileHeight);

		for (long tileY = tileTop; tileY <= tileBottom; tileY += 1L) {
			for (long tileX = tileLeft; tileX <= tileRight; tileX += 1L) {

				// get the tile and the job for the current x and y coordinate
				Tile			tile 			= new Tile				(tileX, tileY, mapView.getZoomLevel());
				MapGeneratorJob mapGeneratorJob = new MapGeneratorJob	(tile, mapFile, jobParameters, debugSettings);

				// the variable where the cached image is going to be placed
				Image image = null;

				// is there already a rendered image in the memory?
				if (this.memoryTileCache.containsKey(mapGeneratorJob)) {
					// get the image
					image = ((FXBitmap) this.memoryTileCache.get(mapGeneratorJob)).image;
				}
				
				
				// is there already a rendered image as file?
				else if (this.memoryTileCache.useFileTileCacheProperty().get() && this.fileCache.containsKey(mapGeneratorJob)) {
					
					// get the image
					image = this.fileCache.get(mapGeneratorJob).image;
					
					// add it to the memory cache
					this.memoryTileCache.put(mapGeneratorJob, new FXBitmap(image));
				}

				// no image found? --> create the job and create a new image
				if (image == null) {

					// add the job only if requested, and there isn't already a
					// job for this tile
					if (createJobs) {
						jobs.add(mapGeneratorJob);
					}

					// draw the empty image
					image = emptyImage;
				}

				// draw the image if it was found
				if (image != null) {
					// draw it on the canvas
					graphics.drawImage(image, offSetX + tileWidth
							* (tileX - tileLeft), offSetY + tileHeight
							* (tileY - tileTop), tileWidth, tileHeight);
				}
			}
		}
		
		if (createJobs) {
			// order the jobs by distance to the center
			Collections.sort(jobs, new Comparator<MapGeneratorJob>() {
	
				@Override
				public int compare(MapGeneratorJob o1, MapGeneratorJob o2) {
					// get the difference to the center for the first job
					double dx1 = o1.tile.tileX - tileXCenter;
					double dy1 = o1.tile.tileY - tileYCenter;
					
					// get the difference to the center for the second job
					double dx2 = o2.tile.tileX - tileXCenter;
					double dy2 = o2.tile.tileY - tileYCenter;
					
					double d1  = Math.sqrt( Math.pow(dx1, 2) + Math.pow(dy1, 2) );
					double d2  = Math.sqrt( Math.pow(dx2, 2) + Math.pow(dy2, 2) );
					
					return Double.compare(d1, d2);
				}
			});
			
			// add to to the jobs
			this.jobs.clear();
			this.jobs.addAll(jobs);
		}
	}

	/**
	 * @return The {@link DoubleProperty} containing the current x offset
	 */
	public ReadOnlyDoubleProperty offsetXProperty() {
		return offsetX;
	}

	/**
	 * @return The {@link DoubleProperty} containing the current y offset
	 */
	public ReadOnlyDoubleProperty offsetYProperty() {
		return offsetY;
	}

	/**
	 * @return The {@link TileCache} that caches everything in the memory
	 */
	public MemoryTileCache getMemoryTileCache() {
		return memoryTileCache;
	}

	/**
	 * @return The {@link TileCache} that caches everything in a file / files
	 */
	public FileTileCache getFileTileCache() {
		return fileCache;
	}
	
	/**
	 * @return The {@link DebugSettings} for the rendering
	 */
	public DebugSettings getDebugSettings () {
		return debugSettings;
	}
	
	/**
	 * @return Whether the drawing of {@link Tile}s is currently paused
	 */
	public synchronized boolean isPaused () {
		return pauseTileDrawing;
	}
	
	/**
	 * @param pause Whether to pause the drawing of {@link Tile}s
	 */
	public synchronized void setPaused (boolean pause) {
		this.pauseTileDrawing = pause;
	}
	
	/**
	 * Destroys this instance and the {@link TileCache}s
	 */
	public void destroy () {
		synchronized (this) {
			// return if already is destroyed
			if (isDestroyed()) {
				return;
			}
			
			// set destroyed
			isDestroyed = true;
		}
		
		
		// stop redrawing
		setPaused(true);
		
		// destroy the cashes / free the memory
		fileCache		.destroy();
		memoryTileCache	.destroy();
	}
	
	/**
	 * @return Whether this instance has been destroyed
	 */
	public synchronized boolean isDestroyed () {
		return isDestroyed;
	}
	

	/**
	 * Just executes the given {@link MapGeneratorJob}
	 * 
	 * @param job {@link MapGeneratorJob} to execute
	 */
	private void executeJob(final MapGeneratorJob job) {
		// create the canvas to draw on
		final Canvas canvas = new Canvas(Tile.TILE_SIZE, Tile.TILE_SIZE);
		
		// queue all the data that needs to be drawn
		databaseRenderer.executeJob(job, canvas);
		
		// cache the tile
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				// add it to the cache
				memoryTileCache.put(job, new FXBitmap(canvas.snapshot(null, null)));
				redrawTiles();
				
				// notify
				synchronized (job) {
					job.notifyAll();
				}
			}
		});

		// block until it has been painted
		synchronized (job) {
			try { job.wait(/*1000*/); } catch (Throwable t) {}
		}

	}

	/**
	 * Creates {@link Runnable}s for all available jobs, this will empty the
	 * job-{@link Queue} but without filling the {@link TileCache} at the same
	 * time! The {@link TileCache} will be filled, if the
	 * {@link MapGeneratorJob} was executed, be sure you don't add
	 * {@link MapGeneratorJob} for the same {@link Tile}s in the meantime!
	 */
	public synchronized void execute() {
		// currently running a Runnable or is paused?
		if (runnable != null || isPaused()) {
			return;
		}

		// create the Runnable, that actually executes the jobs
		runnable = new Runnable() {
			@Override
			public void run() {
				// go through all available jobs
				while (!jobs.isEmpty() && !isPaused()) {
					// get next job to draw
					final MapGeneratorJob job = jobs.poll();
					
					if (job == null) {
						continue;
					}

					// execute the job
					executeJob(job);
				}

				// clear the runnable attribute, so it can be created on the
				// next call
				synchronized (ExecuteMapforge.this) {
					runnable = null;
				}
			}
		};

		// do not run the Runnable in the FX-Thread (it would block the GUI...)
		if (Platform.isFxApplicationThread()) {
			new Thread(runnable, "ExecuteMapforge Rendering").start();
		} else {
			runnable.run();
		}

	}

}
