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

import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import org.mapsforge.map.graphics.Align;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.graphics.Cap;
import org.mapsforge.map.graphics.FontFamily;
import org.mapsforge.map.graphics.FontStyle;
import org.mapsforge.map.graphics.Style;

public class FXPaint implements org.mapsforge.map.graphics.Paint {
	
	
	public		Bitmap			bitmap;
	private		int				color = 0xFFFFFFFF;
	
	public	 	Color			fxColor;
	public		Double[]		lineStrokeDashArray = null;	// line.getStrokeDashArray.add(lineStrokeDashArray);
	public		StrokeLineCap	lineCap;
	public		double			strokeWidth;
	public		Style			stlye;
	public		TextAlignment	textAlign;
	public		double			textSize;
	public		Font			font = Font.getDefault();
	
	private Text				text		= new Text();
	
	public FXPaint () {
		setColor(java.awt.Color.BLACK.getRGB());
	}
	
	@Override
	public int getColor() {
		return color;
	}
	
	@Override
	public int getTextHeight(String text) {
		this.text.setText(text);
		return (int)(Math.round(this.text.getLayoutBounds().getHeight()));
	}
	
	@Override
	public int getTextWidth(String text) {
		this.text.setText(text);
		return (int)Math.round(this.text.getLayoutBounds().getWidth());
	}
	
	@Override
	public void setBitmapShader(Bitmap bitmap) {
		if (bitmap == null) {
			return;
		}
		
		this.bitmap	= bitmap;
		
		// not implementable in JavaFX?
	}

	@Override
	public void setColor(int color) {
		// color is ARGB
		this.color		= color;
		this.fxColor	= new Color(
				
				// on not transparent:
				((0x000000FF & (color >>> 16))/255d),
				((0x000000FF & (color >>>  8))/255d),
				((0x000000FF & (color >>>  0))/255d),
				((0x000000FF & (color >>> 24))/255d)
//				this.color.getRed()		/255d,
//				this.color.getGreen()	/255d,
//				this.color.getBlue()	/255d,
//				this.color.getAlpha()	/255d
				
//				// on transparent:
//				((color >>> 24) & 0x000000FF) / 255d,
//				((color >>> 16) & 0x000000FF) / 255d,
//				((color >>>  8) & 0x000000FF) / 255d,
//				((color >>>  0) & 0x000000FF) / 255d
				);
	}
	
	@Override
	public void setDashPathEffect(float[] strokeDashArray) {
		// create the new array
		lineStrokeDashArray	= new Double[strokeDashArray.length];
		
		// copy it
		for (int i = 0; i < strokeDashArray.length; i++) {
			lineStrokeDashArray[i] = new Double(strokeDashArray[i]);
		}
	}
	
	@Override
	public void setStrokeCap(Cap cap) {
		lineCap = StrokeLineCap.valueOf(cap.name());
	}
	
	@Override
	public void setStrokeWidth(float width) {
		this.strokeWidth = width;
	}
	
	@Override
	public void setStyle(Style style) {
		this.stlye	= style;
	}
	

	/**
	 * Shall be called if the font has changed,
	 * will update the font-metrics
	 */
	public void onFontChanged () {
		// get the current font size
		int size = (int)font.getSize();
		
		// 0.X -> size of 0 -> set it to 1
		if (size <= 0) {
			size = 1;
		}
		
		// set the new FontMetrics
		text.setFont(font);
	}
	
	@Override
	public void setTextAlign(Align align) {
		switch (align) {
			case CENTER:
				textAlign = TextAlignment.CENTER;
				break;
				
			case LEFT:
				textAlign = TextAlignment.LEFT;
				break;
				
			case RIGHT:
				textAlign = TextAlignment.RIGHT;
				break;
		
		}
		
	}
	
	@Override
	public void setTextSize(float textSize) {
		this.font 		= new Font(font.getName(), textSize);//Font.font(font.getName(), textSize);
		this.textSize	= textSize;
		
		// the font has changed...
		onFontChanged();
	}
	
	@Override
	public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
		
		FontWeight	weight	= FontWeight.NORMAL;
		FontPosture posture	= FontPosture.REGULAR;
		String		name	= null;
		
		switch (fontStyle) {
			case BOLD:
				weight	= FontWeight.BOLD;
				break;
				
			case BOLD_ITALIC:
				weight	= FontWeight.BOLD;
				posture = FontPosture.ITALIC;
				break;
				
			case ITALIC:
				posture	= FontPosture.ITALIC;
				break;
				
			case NORMAL:
				break;
		
		}
		
		switch (fontFamily) {
		
			case DEFAULT_BOLD:
				weight	= FontWeight.BOLD;
			case DEFAULT:
				name = Font.getDefault().getFamily();
				break;
				
			case MONOSPACE:
				name	= "Verdana";
				break;
				
			case SANS_SERIF:
				name	= "SansSerif";	
				break;
				
			case SERIF:
				name	= "Serif";
				break;
		
		}
		
		
		this.font	= Font.font(name, weight, posture, textSize);
		

		// the font has changed...
		onFontChanged();
	}
	
	@Override
	public void destroy() {
		if (bitmap != null) {
			bitmap.destroy();
		}
	}
}
