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

package de.itd.maps.mapsforge;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;

public interface MapPoint extends Positionable {
	
	
	/**
	 * @param zoomLevel New zoom-level to bin to
	 */
	public void bindToZoomLevel (ReadOnlyIntegerProperty zoomLevel);
	
	/**
	 * @return The {@link ReadOnlyIntegerProperty} that this {@link MapPoint} is using for calculation
	 */
	public ReadOnlyIntegerProperty zoomProperty();
	
	/**
	 * @return The latitude coordinate of the current point
	 */
	public double getLatitude ();
	
	/**
	 * Sets the latitude value of the current point
	 * @param latitude latitude value to set
	 */
	public void setLatitude (double latitude);
	
	
	/**
	 * @return The longitude coordinate of the current point
	 */
	public double getLongitude ();
	
	/**
	 * Sets the longitude value of the current point
	 * @param longitude longitude value to set
	 */
	public void setLongitude (double longitude);
	
	
	/**
	 * Sets both coordinate values
	 * @param latitude	The latitude value to set
	 * @param longitude	The longitude value to set
	 */
	public void set (double latitude, double longitude);
	
	
	/**
	 * @return The absolute X value for the longitude on the map
	 */
	public double getX ();
	
	/**
	 * @return The absolute Y value for the latitude on the map
	 */
	public double getY ();
	
	/**
	 * Moves the map by the given x and y value
	 * @param x		X value to move by
	 * @param y		Y value to move by
	 */
	public void movePixel (double x, double y);
	
	/**
	 * Moves the map by the given coordinates
	 * @param latitude	Latitude value to move by
	 * @param longitude	Longitude value to move by
	 */
	public void moveCoordinate (double latitude, double longitude);
	
	/**
	 * @return The {@link DoubleProperty} that is used for the latitude value
	 */
	public DoubleProperty latitudeProperty();
	
	/**
	 * @return The {@link DoubleProperty} that is used for the longitude value
	 */
	public DoubleProperty longitudeProperty();
	
	/**
	 * @return The {@link DoubleProperty} that is used for the x value
	 */
	public DoubleProperty xProperty ();
	
	/**
	 * @return The {@link DoubleProperty} that is used for the y value
	 */
	public DoubleProperty yProperty ();
}
