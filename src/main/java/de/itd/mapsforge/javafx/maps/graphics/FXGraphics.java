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

import java.io.InputStream;

import javafx.scene.image.Image;

import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.graphics.Paint;
import org.mapsforge.map.rendertheme.GraphicAdapter;

public class FXGraphics implements GraphicAdapter {
	
	public static final FXGraphics INSTANCE = new FXGraphics();
	
	private FXGraphics () {
		// do nothing
	}
	
	@Override
    public Bitmap decodeStream(InputStream inputStream) {
		return new FXBitmap(new Image(inputStream));
    }

    @Override
    public int getColor(org.mapsforge.map.rendertheme.GraphicAdapter.Color color) {
    	switch (color) {
    		case BLACK:
    			return java.awt.Color.BLACK.getRGB();
    			
    		case CYAN:
    			return java.awt.Color.CYAN.getRGB();
    			
    		case TRANSPARENT:
    			return java.awt.Color.TRANSLUCENT;
    			
    		case WHITE:
    			return java.awt.Color.WHITE.getRGB();
    	}
    	
    	throw new IllegalArgumentException("unknown color value: " + color);

            
    }

    @Override
    public Paint getPaint() {
    	return new FXPaint();
    }

    @Override
    public int parseColor(String colorString) {
    	
    	javafx.scene.paint.Color color = javafx.scene.paint.Color.web(colorString);
    	
    	float r = (float)color.getRed();
    	float g = (float)color.getGreen();
    	float b = (float)color.getBlue();
    	float a = (float)color.getOpacity();
    	
    	int	red		= (int)(r * 255);
    	int green	= (int)(g * 255);
    	int blue	= (int)(b * 255);
    	int alpha	= (int)(a * 255);
    	
    	// TODO colorString is ARGB but #web thinks of RGBA
    	// convert to ARGB
    	if (/*isARGB &&*/ colorString.startsWith("#") ? colorString.length() > 7 : colorString.length() > 6) {
    		return (red	 	<< 24)
    	    	|  (green	<< 16)
    	    	|  (blue	<<  8)
    	    	|  (alpha	<<  0);
    	} else {
	    	return (alpha	<< 24)
	    		|  (red		<< 16)
	    		|  (green	<<  8)
	    		|  (blue	<<  0);
    	}
    }

}
