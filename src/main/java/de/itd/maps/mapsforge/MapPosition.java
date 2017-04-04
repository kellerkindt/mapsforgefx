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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.mapsforge.core.util.MercatorProjection;



public class MapPosition implements MapPoint {

	private DoubleProperty	latitude	= new SimpleDoubleProperty();
	private DoubleProperty	longitude	= new SimpleDoubleProperty();
	
	private IntegerProperty	zoomLevel	= new SimpleIntegerProperty();
	
	private DoubleProperty	x			= new SimpleDoubleProperty();
	private DoubleProperty	y 			= new SimpleDoubleProperty();

	protected boolean		calculates	= false;
	
	

	/**
	 * Will use 15 as zoom level
	 */
	public MapPosition (double latitude, double longitude) {
		this(new SimpleIntegerProperty(15), latitude, longitude);
	}
	
	public MapPosition (MapView view, double latitude, double longitude) {
		this(view.zoomProperty(), latitude, longitude);
	}
	
	public MapPosition (ReadOnlyIntegerProperty zoomLevel, double latitude, double longitude) {
		this.latitude	.set(latitude);
		this.longitude	.set(longitude);
		
		bindToZoomLevel(zoomLevel);
		
		
		// add the listener, to calculate the x and y values
		this.zoomLevel.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				onZoomLevelChanged(newValue.byteValue());
			}
		});
		
		
		// add the listeners, to calculate the latitude and longitude values
		this.x.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (!calculates) {
					movePixel(oldValue.doubleValue()-newValue.doubleValue(), 0);
				}
			}
		});
		
		this.y.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (!calculates) {
					movePixel(0, oldValue.doubleValue()-newValue.doubleValue());
				}
			}
		});
		
		
		// add the listeners, to calculate the x and y values
		this.latitude.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				calculates = true;
				updateY();
				calculates = false;
			}
		});
		
		this.longitude.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				calculates = true;
				updateX();
				calculates = false;
			}
		});
	}
	
	@Override
	public void bindToZoomLevel(ReadOnlyIntegerProperty zoomLevel) {
		this.zoomLevel.unbind();
		this.zoomLevel.bind(zoomLevel);
		
		// re-calculate the new x and y values
		onZoomLevelChanged((byte)zoomLevel.get());
	}
	
	@Override
	public ReadOnlyIntegerProperty zoomProperty() {
		return zoomLevel;
	}
	
	/**
	 * Handles changes on the zoom-level
	 * @param newZoomLevel
	 */
	private void onZoomLevelChanged (byte newZoomLevel) {
		calculates = true;
		updateX();
		updateY();
		calculates = false;
	}
	
	/**
	 * @return The current zoom-level
	 */
	private byte getZoomLevel() {
		return (byte)this.zoomLevel.get();
	}

	
	
	/**
	 * Calculates the x (pixel) value
	 * for the current zoom-level and
	 * longitude coordinate
	 */
	protected void updateX() {
		if (getZoomLevel() >= 0) {
			this.x.set(MercatorProjection.longitudeToPixelX(
					getLongitude(),
					getZoomLevel()
					));
		}
	}
	
	
	/**
	 * Calculates the y (pixel) value
	 * for the current zoom-level and
	 * latitude coordinate 
	 */
	protected void updateY() {
		if (getZoomLevel() >= 0) {
			this.y.set(MercatorProjection.latitudeToPixelY(
					getLatitude(),
					getZoomLevel()
					));
		}
	}

	
	@Override
	public void movePixel(double x, double y) {
		// calculate the new x and y values
		double newX	= this.x.get() + x;
		double newY	= this.y.get() + y;
		
		// calculate the latitude and longitude value
		double lat	= MercatorProjection.pixelYToLatitude (newY, getZoomLevel());
		double lon	= MercatorProjection.pixelXToLongitude(newX, getZoomLevel());
		
		set(lat, lon);
	}
	
	
	@Override
	public void moveCoordinate(double latitude, double longitude) {
		set(getLatitude()+latitude, getLongitude()+longitude);
	}

	@Override
	public double getLatitude() {
		return this.latitude.get();
	}

	@Override
	public void setLatitude(double latitude) {
		this.latitude.set(latitude);
	}

	@Override
	public double getLongitude() {
		return this.longitude.get();
	}

	@Override
	public void setLongitude(double longitude) {
		this.longitude.set(longitude);
	}

	@Override
	public void set(double latitude, double longitude) {
		this.latitude	.set(latitude);
		this.longitude	.set(longitude);
	}

	@Override
	public double getX() {
		return this.x.get();
	}

	@Override
	public double getY() {
		return this.y.get();
	}

	@Override
	public DoubleProperty latitudeProperty() {
		return this.latitude;
	}

	@Override
	public DoubleProperty longitudeProperty() {
		return this.longitude;
	}

	@Override
	public DoubleProperty xProperty() {
		return this.x;
	}

	@Override
	public DoubleProperty yProperty() {
		return this.y;
	}
}
