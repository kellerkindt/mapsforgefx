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

import org.mapsforge.core.model.Point;

public class DashedLineHelper {

	private int			n	= 0;
	private Double[]	lineStrokeDashArray;
	
	private Point		last;
	private Point		p2;
	
	private double		deltaX;
	private double		deltaY;
	
//	private double		radians;
//	private double		degrees;
	private double		distance;
	private double		lastLength = 0.00001;
	
	public DashedLineHelper (Double[] lineStroekDashArray) {
		this.lineStrokeDashArray	= lineStroekDashArray;
	}
	
	public void setPoints (Point p1, Point p2) {
		this.last	= p1;
		this.p2 	= p2;
		
		deltaX = p2.x - p1.x;
		deltaY = p2.y - p1.y;
		
//		radians = Math.atan2(deltaY, deltaX);
//		degrees = Math.toDegrees(radians);
		
		distance = Math.sqrt( Math.pow(deltaX, 2) + Math.pow(deltaY, 2) );
	}
	
	
	/**
	 * @return The maximum length for the next line (visible or not)
	 */
	public double nextLength () {
		if (lineStrokeDashArray == null || lineStrokeDashArray.length == 0) {
			return Double.MAX_VALUE;
		}
		
		// how long should the last line be
		double currentLength = lineStrokeDashArray[n];
		
		// last line was not as long as expected?
		if (currentLength - lastLength > 0.01 && lastLength > 0) {
			return currentLength-lastLength;
		}
		
		// next length (restart?)
		if (++n >= lineStrokeDashArray.length) {
			n = 0;
		}
		
		return lineStrokeDashArray[n];
	}
	
	/**
	 * @return The next {@link Point} to move or line to
	 */
	public Point nextPoint () {
		if (lineStrokeDashArray == null || lineStrokeDashArray.length == 0) {
			return p2;
		}
		
		double length = nextLength();
		
		double dx = (deltaX * length) / distance;
		double dy = (deltaY * length) / distance;
		
		double px = last.x +dx;
		double py = last.y +dy;
		
		// does it change from + to - or - to +?
		if (px-p2.x > 0 != last.x-p2.x > 0 || Double.isNaN(py)) {
			px = p2.x;
		}
		
		// does it change from + to - or - to +?
		if (py-p2.y > 0 != last.y-p2.y > 0 || Double.isNaN(py)) {
			py = p2.y;
		}
		
		
		dx = p2.x - px;
		dy = p2.y - py;
		
		lastLength = Math.sqrt( Math.pow(dx, 2) + Math.pow(dy, 2) );
		
		return (last = new Point(px, py));
	}
	
	/**
	 * @return Whether to line to the next point or to move
	 */
	public boolean line () {
		return n % 2 == 0;
	}
}
