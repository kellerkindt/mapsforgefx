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

import org.mapsforge.map.reader.MapDatabase;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


public class MapViewPosition extends MapPosition implements MapView {
		
	public static final int ZOOM_MAX = Byte.MAX_VALUE;
	public static final int ZOOM_MIN = 8;	/** TODO since {@link MapDatabase#MAXIMUM_ZOOM_TABLE_OBJECTS} is limited to 65535**/ 
	
	private IntegerProperty zoomLevel	= null;
	private Positionable	follow		= null;
	private int				followPrio	= -1;
	
	public MapViewPosition (double latitude, double longitude, byte zoom) {
		this(latitude, longitude, new SimpleIntegerProperty(zoom));
	}
	
	public MapViewPosition (double latitude, double longitude, IntegerProperty zoomLevel) { 
		super(zoomLevel, latitude, longitude);
		
		// set the zoom-level
		this.zoomLevel = zoomLevel;
		
		// set max an min values for the zoom level (0-254, byte)
		this.zoomLevel.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (newValue.intValue() >  ZOOM_MAX) {
					MapViewPosition.this.zoomLevel.set(ZOOM_MAX);
				}
				
				else if (newValue.intValue() < ZOOM_MIN) {
					MapViewPosition.this.zoomLevel.set(ZOOM_MIN);
				}
			}
		});
		
	}
	
	@Override
	public void follow(Positionable pos) {
		follow(pos, Integer.MAX_VALUE);
	}
	
	@Override
	public void follow(Positionable pos, int priority) {
		if (isFollowing()) {
			if (!unfollow(priority)) {
				return;
			}
		}
		
		this.follow		= pos;
		this.followPrio	= priority;
		this.latitudeProperty()	.bind(pos.latitudeProperty());
		this.longitudeProperty().bind(pos.longitudeProperty());
	}
	
	@Override
	public void unfollow() {
		unfollow(Integer.MAX_VALUE);
	}
	
	@Override
	public boolean unfollow(int priority) {
		if (priority >= followPrio && follow != null) {
			this.latitudeProperty()	.unbind();
			this.longitudeProperty().unbind();
			this.follow = null;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isFollowing() {
		return follow != null;
	}
	
	@Override
	public boolean isFollowing(Positionable positionable) {
		return (positionable != null) == (follow != null)
			&& (positionable != null && positionable.equals(follow));
	}

	@Override
	public byte getZoomLevel() {
		return (byte)zoomLevel.get();
	}

	@Override
	public void setZoomLevel(byte zoom) {
		this.zoomLevel.set(zoom);
	}

	@Override
	public void set(double latitude, double longitude, byte zoomLevel) {
		// do not set another position if following an MapItem
		if (!isFollowing()) {
			this.set(latitude, longitude);
		}
		this.zoomLevel.set(zoomLevel);
	}

	@Override
	public void move(double x, double y) {
		// do not move, if following an MapItem
		if (!isFollowing()) {
			super.movePixel(x, y);
		}
	}
	
	@Override
	public void moveCoordinate(double latitude, double longitude) {
		// only, if not following an item
		if (!isFollowing()) {
			super.moveCoordinate(latitude, longitude);
		}
	}
	
	@Override
	public void movePixel(double x, double y) {
		// only, if not following an item
		if (!isFollowing()) {
			super.movePixel(x, y);
		}
	}

	@Override
	public IntegerProperty zoomProperty() {
		return zoomLevel;
	}
}
