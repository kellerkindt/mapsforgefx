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

import javafx.beans.property.IntegerProperty;

public interface MapView extends MapPoint {

	/**
	 * @return The current zoom level of the map
	 */
	public byte getZoomLevel ();
	
	/**
	 * Sets the zoom for this view
	 * @param zoom Zoom to set
	 */
	public void setZoomLevel (byte zoom);	
	

	/**
	 * Sets all values
	 * @param latitude	The latitude value to set
	 * @param longitude	The longitude value to set
	 * @param zoomLevel The zoom-level to set
	 */
	public void set (double latitude, double longitude, byte zoomLevel);
	
	/**
	 * Lets the {@link MapView} follow the given {@link Positionable}
	 * @param pos {@link Positionable} to follow
	 */
	public void follow (Positionable pos);
	
	/**
	 * Lets the {@link MapView} follow the given {@link Positionable}
	 * 
	 * @param pos		{@link Positionable} to follow
	 * @param priority	Priority to assign to this
	 */
	public void follow (Positionable pos, int priority);
	
	/**
	 * Removes any link to any given {@link MapItem}
	 * if existent
	 */
	public void unfollow ();
		
	/**
	 * Removes any link to any given {@link MapItem},
	 * if the given priority is higher or equal to the assigned priority
	 * 
	 * @param priority Priority of this un-follow request
	 * @return Whether the un-follow request was successfully
	 */
	public boolean unfollow (int priority);
	
	/**
	 * @return Whether this {@link MapView} is following a {@link MapItem}
	 */
	public boolean isFollowing ();
	
	/**
	 * @param positionable {@link Positionable} to check against
	 * @return Whether this {@link MapView} if following the given {@link Positionable}
	 */
	public boolean isFollowing (Positionable positionable);
	
	/**
	 * Moves the map for the given x and y coordinates
	 * @param x	X coordinate to move
	 * @param y	Y coordinate to move
	 * @deprecated Use {@link #movePixel(double, double)} instead
	 */
	public void move (double x, double y);
	
	/**
	 * Moves the map by the given x and y value
	 * @param x	X value to move
	 * @param y Y value to move
	 */
	public void movePixel (double x, double y);
	
	/**
	 * Moves the map by the given coordinates
	 * @param latitude	Latitude value to move by
	 * @param longitude	Longitude value to move by
	 */
	public void moveCoordinate (double latitude, double longitude);
	
	/**
	 * @return The {@link IntegerProperty}, that is used for the zoom level (byte, so 0-254)
	 */
	public IntegerProperty zoomProperty();
	
}
