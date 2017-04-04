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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;

import org.mapsforge.core.model.Tile;

import de.itd.mapsforge.javafx.maps.mapgenerator.MapGeneratorJob;

public class FileTileCacheEntry {

	private Image			image;
	private MapGeneratorJob	job;

	private boolean			alive;
	private boolean			written;
	
	
	public FileTileCacheEntry(MapGeneratorJob job) {
		this.job		= job;
		this.written	= true;
	}
	
	public FileTileCacheEntry(MapGeneratorJob job, Image image) {
		this.job		= job;
		this.image		= image;
	}
	
	/**
	 * @return Whether this entry is alive / can be used
	 */
	public synchronized boolean isAlive () {
		return alive;
	}
	
	/**
	 *  Marks this entry as dead,
	 *  will also free all resources
	 */
	public synchronized void kill () {
		this.alive	= false;
		this.job	= null;
	}
	
	/**
	 * @return The current {@link Image} or creates a new dummy {@link Image} (on which will be written on {@link #read()})
	 */
	public synchronized Image getImage () {
		if (image == null) {
			// new writable image
			image		= new WritableImage(Tile.TILE_SIZE, Tile.TILE_SIZE);

			// write a dummy value to the image
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					((WritableImage)image).getPixelWriter().setColor(x, y, Color.LIMEGREEN);
				}
			}
		}
		
		// return the new image
		return image;
	}
	
	/**
	 * @return The {@link MapGeneratorJob} for this {@link Image}
	 */
	public synchronized MapGeneratorJob getMapGeneratorJob () {
		return job;
	}
	
	/**
	 * @return Whether the {@link Image} has been written to {@link File}
	 */
	public synchronized boolean isWritten () {
		return written;
	}
	
	/**
	 * @return Whether the {@link Image} can be written to {@link File}
	 */
	public synchronized boolean canWrite () {
		return image != null;
	}
	
	/**
	 * Loads the {@link Image} from the file.
	 * Will not close the given {@link InputStream}
	 * @throws IOException
	 */
	public void read (InputStream is) throws IOException {
		// get the pixel writer
		PixelWriter writer 	= ((WritableImage)image).getPixelWriter();
		int			width	= (int)image.getWidth();
		int			height	= (int)image.getHeight();		
		
		
		try {
			
			// 4x since blue, read, green and alpha needs each a byte
			byte buffer[] = new byte[(width * height)*4];
			
			// read the file all at once
			is.read(buffer);
			
			// set the image with the read data
			writer.setPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), buffer, 0, width*4);
		} finally {
			// cleanup, reference has been return on getImage -> no need to keep it in this class
			synchronized (this) {
				this.image = null;
			}
		}
	}
	
	/**
	 * Writes the current {@link Image} to file.
	 * Will neither flush nor close the given {@link OutputStream}
	 * @param os {@link OutputStream} to write to
	 * @throws IOException
	 */
	public void write (OutputStream os) throws IOException {
		// get the pixel reader
		PixelReader reader;
		int			width;
		int			height;
		
		reader	= image.getPixelReader();
		width	= (int)image.getWidth();
		height	= (int)image.getHeight();
		
		// 4x since blue, read, green and alpha needs each a byte
		byte buffer[] = new byte[(width * height)*4];
		
		// save anything to the buffer (width x 4 ->  since blue, read, green and alpha needs each a byte)
		reader.getPixels(0, 0, width, height, WritablePixelFormat.getByteBgraInstance(), buffer, 0, width*4);
		
		// write everything to the file
		os.write(buffer);
		
		// has been written to file successfully
		this.written = true;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj instanceof FileTileCacheEntry) {
			return this.getMapGeneratorJob().equals(((FileTileCacheEntry) obj).getMapGeneratorJob());
		}
		
		return false;
	}
}
