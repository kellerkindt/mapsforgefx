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

package de.itd.mapsforge.javafx.maps.graphics;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import org.mapsforge.map.graphics.Bitmap;

public class FXBitmap implements Bitmap {
	
	public final Image image;
	
	public FXBitmap (Image image) {
		this.image	= image;
	}

	@Override
	public void destroy() {
		image.cancel();
	}

	@Override
	public int getHeight() {
		return (int)image.getHeight();
	}


	@Override
	public int getWidth() {
		return (int)image.getWidth();
	}
	
	@Override
	public int[] getPixels() {
		int			pixels[]	= new int[getHeight() * getWidth()];
		PixelReader	reader		= image.getPixelReader();
		
		// check!
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				pixels[y*getHeight()+x] = reader.getArgb(x, y);
			}
		}
		
		return pixels;
	}


}
