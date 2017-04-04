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

package de.itd.mapsforge.javafx.maps.mapgenerator.databaserenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import de.itd.mapsforge.javafx.maps.graphics.FXPaint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;

import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.graphics.FontFamily;
import org.mapsforge.map.graphics.FontStyle;
import org.mapsforge.map.graphics.Style;
import org.mapsforge.map.reader.Way;

import de.itd.mapsforge.javafx.maps.graphics.FXBitmap;

/**
 * A CanvasRasterer uses a Canvas for drawing.
 * 
 * @see <a
 *      href="http://developer.android.com/reference/android/graphics/Canvas.html">Canvas</a>
 */
public class CanvasRasterer {
	
	// TODO
	public static final String TAG_HIGHWAY	= "highway";
	public static final String TAG_LANES	= "lanes";
	public static final String TAG_WATERWAY	= "waterway";
	
	
	// TODO
	public static class WayHelper {

		private Map<String, String> tags = new HashMap<String, String>();
		private Way					way  = null;
		
		public WayHelper (Way way) {
			this.way = way;
			
			for (Tag tag : way.tags) {
				tags.put(tag.key, tag.value);
			}
		}
		
		/**
		 * @param key Key to check
		 * @return Whether the {@link Way} has the given tag
		 */
		public boolean hasTag (String key) {
			return tags.containsKey(key);
		}
		
		/**
		 * @param key Key to get the value for
		 * @return The value of the tag for the given key or null
		 */
		public String getTag (String key) {
			return tags.get(key);
		}
		
		/**
		 * @return The {@link Way} of this {@link WayHelper}
		 */
		public Way getWay () {
			return way;
		}
	}
	
	
	
	/*
	 * It's used in the Android-APP this way, so don't ask why (i don't know...)
	 */
	public static final double OFFSET_TEXT_VERTICAL		= 3;
	public static final double OFFSET_TEXT_HORIZONTAL	= 0;
	
	private static FXPaint createPaint() {
		return new FXPaint();
	}

	// private static final FXPaint PAINT_BITMAP_FILTER = createPaint();
	private static final FXPaint PAINT_TILE_COORDINATES = createPaint();
	private static final FXPaint PAINT_TILE_COORDINATES_STROKE = createPaint();
	// private static final FXPaint PAINT_TILE_FRAME = createPaint();
	private static final float[] TILE_FRAME = new float[] { 0, 0, 0,
			Tile.TILE_SIZE, 0, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE,
			Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, 0, Tile.TILE_SIZE,
			0, 0, 0 };

	private static void configurePaints() {
		PAINT_TILE_COORDINATES.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		PAINT_TILE_COORDINATES.setTextSize(20);
		PAINT_TILE_COORDINATES.setColor(java.awt.Color.BLACK.getRGB());

		PAINT_TILE_COORDINATES_STROKE.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
		PAINT_TILE_COORDINATES_STROKE.setStyle(Style.STROKE);
		PAINT_TILE_COORDINATES_STROKE.setStrokeWidth(1);
		PAINT_TILE_COORDINATES_STROKE.setTextSize(20);
		PAINT_TILE_COORDINATES_STROKE.setColor(java.awt.Color.WHITE.getRGB());
	}

	private GraphicsContext					context;
	private Canvas							canvas;
	
	private boolean							beganPath		= false;
	private Map<Way, ShapePaintContainer>	wayContainers	= new WeakHashMap<>();
	

	public CanvasRasterer() {
		// prepare a dummy canvas
		setCanvas(new Canvas(Tile.TILE_SIZE, Tile.TILE_SIZE));
		
		configurePaints();
	}
	
	private void beginPath () {
		if (!beganPath) {
			beganPath = true;
			context.beginPath();
		}
	}
	
	private void closePath () {
		if (beganPath) {
			beganPath = false;
			context.closePath();
		}
	}
	
	/**
	 * Draws the given line to the given height
	 * @param string	String to draw
	 * @param offsetY	height to use
	 */
	private void drawTileCoordinate(String string, int offsetY) {
		context.save();
		
		// draw the text
		drawText(PAINT_TILE_COORDINATES_STROKE, string, 20, offsetY);
		drawText(PAINT_TILE_COORDINATES,		string, 20, offsetY);
		
		context.restore();
	}

	/**
	 * Draws the given text at the given position, uses the {@link FXPaint} to
	 * configure the {@link GraphicsContext}
	 * 
	 * @param paint 	{@link FXPaint} to get the formation from
	 * @param text 		Text to draw
	 * @param x 		X coordinate to place the text on
	 * @param y 		Y coordinate to place the text on
	 */
	private void drawText(FXPaint paint, String text, double x, double y) {
		context.setLineWidth(paint.strokeWidth);
		context.setFont(paint.font);

		if (paint.stlye == Style.FILL) {
			context.setFill(paint.fxColor);
			context.fillText(text, x, y);

		} else {
			context.setStroke(paint.fxColor);
			context.strokeText(text, x, y);
		}
	}

	/**
	 * Draws the given {@link List} of {@link PointTextContainer}s
	 * @param pointTextContainers {@link List} of {@link PointTextContainer} to draw
	 */
	public void drawNodes(List<PointTextContainer> pointTextContainers) {
		context.save();
		
		for (int index = pointTextContainers.size() - 1; index >= 0; --index) {
			PointTextContainer pointTextContainer = pointTextContainers.get(index);

			// get the paints
			FXPaint paintBack	= (FXPaint) pointTextContainer.paintBack;
			FXPaint paintFront	= (FXPaint) pointTextContainer.paintFront;
			

			// draw the back
			if (paintBack != null) {
				drawText(paintBack, pointTextContainer.text, pointTextContainer.x, pointTextContainer.y);
			}

			// draw the front
			if (paintFront != null) {
				drawText(paintFront, pointTextContainer.text, pointTextContainer.x, pointTextContainer.y);
			}
		}
		context.restore();
	}

	/**
	 * Draws the given {@link List} of {@link SymbolContainer}s
	 * @param symbolContainers {@link List} of {@link SymbolContainer}s to draw
	 */
	public void drawSymbols(List<SymbolContainer> symbolContainers) {
		context.save();

		for (int index = symbolContainers.size() - 1; index >= 0; --index) {
			SymbolContainer symbolContainer = symbolContainers.get(index);

			context.restore();
			context.save();

			Point point = symbolContainer.point;
			Image image = ((FXBitmap) symbolContainer.symbol).image;

			int pivotX = 0;
			int pivotY = 0;

			if (symbolContainer.alignCenter) {
				pivotX = -(symbolContainer.symbol.getWidth() 	/ 2);
				pivotY = -(symbolContainer.symbol.getHeight() 	/ 2);
			}

			// draw the image
			context.translate	(point.x, point.y);
			context.rotate		(symbolContainer.rotation);
			context.drawImage	(image, pivotX, pivotY);

		}

		context.restore();
	}

	/**
	 * Draws the {@link Tile}-Coordinates of the given {@link Tile}
	 * @param tile {@link Tile} to get the coordinates from
	 */
	public void drawTileCoordinates(Tile tile) {
		drawTileCoordinate("X: " + tile.tileX, 30);
		drawTileCoordinate("Y: " + tile.tileY, 60);
		drawTileCoordinate("Z: " + tile.zoomLevel, 90);
	}

	/**
	 * Draws a frame around the current {@link Tile}
	 */
	public void drawTileFrame() {
		context.save();

		context.beginPath();
		context.moveTo(TILE_FRAME[0], TILE_FRAME[1]);

		for (int i = 2; i < TILE_FRAME.length; i += 2) {
			context.lineTo(TILE_FRAME[i], TILE_FRAME[i + 1]);
		}

		// draw
		context.stroke();
		context.closePath();
		
		context.restore();
	}

	/**
	 * Draws the given {@link List} of {@link WayTextContainer}s
	 * @param wayTextContainers {@link List} of {@link WayTextContainer}s to draw
	 */
	public void drawWayNames(List<WayTextContainer> wayTextContainers) {
		context.save();

		for (int index = wayTextContainers.size() - 1; index >= 0; --index) {
			WayTextContainer wayTextContainer = wayTextContainers.get(index);

			context.restore();
			context.save();

			double[] textCoordinates = wayTextContainer.coordinates;

			double deltaX = textCoordinates[2] - textCoordinates[0];
			double deltaY = textCoordinates[3] - textCoordinates[1];

			double radians = Math.atan2(deltaY, deltaX);
			double degrees = Math.toDegrees(radians);

			double distance = Math.sqrt(Math.pow(deltaX, 2)
							+ Math.pow(deltaY, 2));

			FXPaint paint = (FXPaint) wayTextContainer.paint;

			context.setFont(paint.font);
			context.setLineWidth(paint.strokeWidth);

			context.translate(textCoordinates[0], textCoordinates[1]);
			context.rotate(degrees);

			if (paint.stlye == Style.FILL) {
				context.setFill(paint.fxColor);
				context.fillText(wayTextContainer.text, OFFSET_TEXT_HORIZONTAL, OFFSET_TEXT_VERTICAL, distance);
				
			} else {
				context.setStroke(paint.fxColor);
				context.strokeText(wayTextContainer.text, OFFSET_TEXT_HORIZONTAL, OFFSET_TEXT_VERTICAL, distance);
			}
			
		}

		context.restore();
	}

	/**
	 * Draws the given {@link ShapePaintContainer}s
	 * @param drawWays {@link ShapePaintContainer}s to draw
	 */
	public void drawWays(List<List<List<ShapePaintContainer>>> drawWays) {
		int levelsPerLayer = drawWays.get(0).size();
		context.save();
		

		/*
		 *  ######################################################
		 *  #                                                    #
		 *  #     Workaround for Bug, where the tunnels are      #
		 *  #     drawn on a wrong layer (to deep), and          #
		 *  #     therefore not visible                          #
		 *  #                                                    #
		 *  ######################################################
		 *  
		 *  Patch does not work (2014-03-06):
		 *  https://groups.google.com/forum/#!msg/mapsforge-dev/x54kHlyKiBM/7X4t96OyE1cJ
		 *  
		 *  TODO Check for a new version / patch,
		 *       Current solution is quit bad!
		 *       
		 *  Workaround description:
		 *       Create a new layer below all the others
		 *       and add all the elements that are drawn
		 *       above the tunnels - but shouldn't ("landuse") -
		 *       to it, since they are drawn above the tunnels
		 *       
		 *  Workaround takes ~1ms (i7-3537U)
		 */
		Set<WayHelper> ways = new HashSet<WayHelper>();
		List<List<List<ShapePaintContainer>>> drawWaysNew = new ArrayList<>();
		
		// add one more list, where to move the wrongly containers to
		for (int i = 0; i < drawWays.size()+1; i++) {
			drawWaysNew.add( new ArrayList<List<ShapePaintContainer>>() );
		}
		
		// iterate :D
		for (int layer = 0, layers = drawWays.size(); layer < layers; ++layer) {
			List<List<ShapePaintContainer>> shapePaintContainers = drawWays.get(layer);

			for (int level = 0; level < levelsPerLayer; ++level) {
				List<ShapePaintContainer> wayList = shapePaintContainers.get(level);

				for (int index = wayList.size() - 1; index >= 0; --index) {
					ShapePaintContainer shapePaintContainer = wayList.get(index);
					
					// flag to decide, whether this is going to be to the new list
					boolean toList = false;
					
					
					if (shapePaintContainer.shapeContainer instanceof WayContainer) {
						for (Tag tag : ((WayContainer)shapePaintContainer.shapeContainer).way.tags) {
							if ("landuse".equalsIgnoreCase(tag.key)) {
								toList = true;
								break;
							}
						}
						
					}
					
					
					// get the list, the elements needs to be added to
					List<List<ShapePaintContainer>> listList = drawWaysNew.get(toList ? 0 : (layer+1));

					// add new levels until the target level is available
					while (listList.size() <= level) {
						listList.add( new ArrayList<ShapePaintContainer>() );
					}

					// add it to the new list
					listList.get(level).add(shapePaintContainer);
				}
			}
		}
		
		// replace the old lists
		drawWays = drawWaysNew;
		
		/*
		 *  ######################################################
		 *  
		 *                     END of Workaround
		 *        
		 *  ######################################################
		 */
		
		
		

		for (int layer = 0, layers = drawWays.size(); layer < layers; ++layer) {
			List<List<ShapePaintContainer>> shapePaintContainers = drawWays.get(layer);

			for (int level = 0; level < shapePaintContainers.size(); ++level) {
				List<ShapePaintContainer> wayList = shapePaintContainers.get(level);

				for (int index = wayList.size() - 1; index >= 0; --index) {
					ShapePaintContainer shapePaintContainer = wayList.get(index);

					
					FXPaint		paint	= (FXPaint) shapePaintContainer.paint;
					FXBitmap	bitmap	= (FXBitmap) paint.bitmap;
					
					WayHelper	way		= null;
					boolean		lanes	= false;

					/* ignore a draw / stroke request with the line-width of 0
					 * MapsForge thinks of 0 -> no line, but JavaFX handles
					 * 0 as the thinnest possible line -> there is a line
					 * Also, it would take processing time... ^^
					 */
					if (paint.strokeWidth <= 0 && Style.STROKE == paint.stlye) {
						continue;
					}
					
					context.restore();
					context.save();

					switch (shapePaintContainer.shapeContainer.getShapeType()) {
						case CIRCLE:
							CircleContainer	circleContainer = (CircleContainer) shapePaintContainer.shapeContainer;
							Point			point			= circleContainer.point;

							beginPath();
							context.arcTo(point.x, point.y,
									point.x + 2 * circleContainer.radius,
									point.y + 2 * circleContainer.radius,
									circleContainer.radius);
							break;
	
						case WAY:
							WayContainer	wayContainer	= (WayContainer) shapePaintContainer.shapeContainer;
							Point[][]		coordinates		= wayContainer.coordinates;
							
							way = new WayHelper(wayContainer.way);

							
							if (wayContainers.containsKey(wayContainer.way)) {
								lanes	= ways.contains(way)
										&& paint.lineStrokeDashArray != null
										&& paint.lineStrokeDashArray.length > 0;
								
								if (!lanes) {
									ways.add(way);
								}
								
							} else if (hasMultipleLanes(way)) {
								wayContainers.put(wayContainer.way, shapePaintContainer);
							}
	
							// ignore lanes
							if (!lanes) {
								beginPath();
								strokeLine(coordinates, paint.lineStrokeDashArray);
							}
							break;
					}
					
					// prepare the default paint
					Paint fxPaint = paint.fxColor;
					
					// use the the bitmap, if available
					if (bitmap != null) {
						fxPaint = new ImagePattern(bitmap.image, 0, 0, bitmap.getWidth(), bitmap.getHeight(), false);
					}
					
					// decide whether to stroke or fill
					if (paint.stlye == Style.STROKE) {
						context.setStroke	(fxPaint);
						context.setLineWidth(paint.strokeWidth);

						// set the line cap
						if (paint.lineCap != null) {
							context.setLineCap(paint.lineCap);
						}

						
						// decide whether this is a normal stroke call or a request to draw the lanes
						// TODO remove
						if (lanes) {
							// draw the lane separators if possible
							drawLaneSeparators(way, paint.lineStrokeDashArray[0]);
							
						} else {
							context.stroke();
						}

					} else {
						// fill the area
						context.setFill(fxPaint);
						context.fill();
					}

					closePath();
				}
			}
		}

		context.restore();
	}
	
	/**
	 * Draws a line at the given coordinates
	 * and for the given dash-array
	 * @param coordinates	Coordinates to draw the line on
	 * @param dashArray		Dash-Array to use to draw the line
	 */
	private void strokeLine (Point[][] coordinates, Double[] dashArray) {
		// prepare the helper for the dashed line
		DashedLineHelper	helper		= new DashedLineHelper(dashArray);

		for (int i = 0; i < coordinates.length; i++) {
			Point[] points = coordinates[i];
			Point before = points[0];
			Point next = null;

			// there must be 2 points to draw a line
			if (points.length < 2) {
				continue;
			}

			// go to the first point
			context.moveTo(before.x, before.y);
			
			for (int n = 1; n < points.length; n++) {

				// tell the helper the two points to draw a line
				// between
				helper.setPoints(before, points[n]);

				do {
					// get the next point to move or line to
					next = helper.nextPoint();

					// move or line to
					if (helper.line()) {
						context.lineTo(next.x, next.y);
					} else {
						context.moveTo(next.x, next.y);
					}

				} while (!next.equals(points[n]));

				// remember the current point for the next point
				before = points[n];
			}
		}
	}

	/**
	 * Fills the current {@link Tile} with the given RGB-color
	 * @param color RGB-color to use
	 */
	public void fill(int color) {
		java.awt.Color aColor = new java.awt.Color(color);
		
		context.save();
		context.setFill(new Color(
				aColor.getRed() 	/ 255d,
				aColor.getGreen() 	/ 255d,
				aColor.getBlue() 	/ 255d,
				aColor.getAlpha() 	/ 255d)
				);
		
		context.fillRect(
				0,
				0,
				canvas.getWidth(),
				canvas.getHeight() );
		context.restore();
	}

	/**
	 * Sets the {@link Canvas} to draw on
	 * @param canvas {@link Canvas} to set
	 */
	public void setCanvas(Canvas canvas) {
		this.canvas		= canvas;
		this.context 	= canvas.getGraphicsContext2D();
		
		this.wayContainers	.clear();
	}
	
	
	
	/*
	 * >>>>>>>>>>>>>>>>>>>>>>>>
	 * 
	 *   Way lane stuff start
	 * 
	 * >>>>>>>>>>>>>>>>>>>>>>>>
	 */

	
	/**
	 * Draws all lane separators for the given {@link Way}
	 * with the given dash array content
	 * @param helper 			{@link WayHelper} to draw the lane separators on
	 * @param dashArrayContent	Dash-Array content to use
	 */
	private void drawLaneSeparators (WayHelper helper, double dashArrayContent) {
		ShapePaintContainer	shapeContainer	= wayContainers.get(helper);
		WayContainer		wayContainer	= (WayContainer)shapeContainer.shapeContainer;
		Point[][]			coordinates		= wayContainer.coordinates;
		
		
		FXPaint	paint 	= (FXPaint)shapeContainer.paint;
		
		int		count 	= getLaneCount(helper);
		double	width 	= paint.strokeWidth / ((double)count);
		boolean	even	= count % 2 == 0;
			
		for (int i = 0; i < count/2; i++) {
			
			if (even) {
				drawLaneSeparator(manipulate(coordinates, width*i), dashArrayContent);
				
				if (i > 0) {
					drawLaneSeparator(manipulate(coordinates, width*i*-1), dashArrayContent);
				}
			}
			
			
			else {
				drawLaneSeparator(manipulate(coordinates, (width/2d + width*i)),	dashArrayContent);
				drawLaneSeparator(manipulate(coordinates, (width/2d + width*i)*-1), dashArrayContent);
			}
		}
	}

	
	/**
	 * Draws a lane separator on the given coordinates
	 * and the given dash-array value
	 * @param coordinates		Coordinates to draw the lane on
	 * @param dashArrayContent	Dash-Array content to use
	 */
	private void drawLaneSeparator (Point[][] coordinates, double dashArrayContent) {
		closePath();
		beginPath();
		
		strokeLine(coordinates, new Double[]{dashArrayContent, dashArrayContent});

		context.stroke();
	}

	
	/**
	 * Manipulates / moves all given points
	 * The points are moved away from the given points
	 * and let them act like a center
	 * Will return the given points,
	 * if the requested move amount is 0
	 * @param points	Points to move
	 * @param by		Move amount / Amount to move the points with
	 * @return A new array with the moved points or the given points
	 */
	private Point[][] manipulate (Point[][] points, double by) {
		// nothing to do?
		if (by == 0) {
			return points;
		}
		
		// prepare
		Point[][] manipulated = new Point[points.length][];
		
		for (int i = 0; i < points.length; i++) {
			manipulated[i] = manipulate(points[i], by);
		}
		
		return manipulated;
	}
	
	/**
	 * Manipulates / moves all given points
	 * The points are moved away from the given points
	 * and let them act like a center
	 * Will return the given points,
	 * if the requested move amount is 0
	 * @param points	Points to move
	 * @param by		Move amount / Amount to move the points with
	 * @return A new array with the moved points or the given points
	 */
	private Point[] manipulate (Point[] points, double by) {
		Point[] manipulated = new Point[points.length];
		Point	last		= points[0];
		boolean	first		= true;
		
		for (int i = 1; i < points.length; i++) {
			
			double dx 	= points[i].x - last.x;
			double dy 	= points[i].y - last.y;
			
			double rad	= Math.atan2(dx, dy);
			
			if (first) {
				manipulated[0]	= manipulate(last, rad, by);
				first			= false;
			}
			
			manipulated[i]	= manipulate(points[i], rad, by);
			last			= points[i];
		}
		
		
		return manipulated;
	}
	
	/**
	 * Moves the given point for the given
	 * angle and width
	 * @param point	{@link Point} to move
	 * @param rad	Angle
	 * @param by	Width
	 * @return The new / moved {@link Point}
	 */
	private Point manipulate (Point point, double rad, double by) {
		double nx = point.x + (by * Math.cos(rad));
		double ny = point.y + (by * Math.sin(rad));
		
		return new Point(nx, ny);
	}

	
	/**
	 * @param helper {@link WayHelper} to get the lane-count from
	 * @return The count of lanes for the given {@link WayHelper}
	 */
	private int getLaneCount (WayHelper helper) {
		String value = helper.getTag( TAG_LANES );
		return value == null ? 1 : Integer.parseInt(value);
	}
	
	private boolean hasMultipleLanes (WayHelper helper) {
		return getLaneCount(helper) > 1;
	}
	

	
	/*
	 * <<<<<<<<<<<<<<<<<<<<<<<<
	 * 
	 *    Way lane stuff end
	 * 
	 * <<<<<<<<<<<<<<<<<<<<<<<<
	 */
}
