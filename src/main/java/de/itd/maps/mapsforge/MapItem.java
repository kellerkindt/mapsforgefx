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

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.CircleBuilder;
import de.itd.maps.mapsforge.MapsforgeMapContextMenu.ContextEntry;

public class MapItem extends Group implements Positionable {
	
	private MapPoint	position 	= null;
	private boolean		isBound 	= false;

	private DoubleProperty 	offsetX 			= new SimpleDoubleProperty(0);
	private DoubleProperty 	offsetY 			= new SimpleDoubleProperty(0);
	private BooleanProperty autoscalingEnabled 	= new SimpleBooleanProperty(true);

	private List<ContextEntry> contextEntries	= new ArrayList<>();

	/**
	 * Creates an Empty MapItem
	 * 
	 * @param lat
	 * @param lon
	 */
	public MapItem(double lat, double lon) {
		init(lat, lon);
	}

	/**
	 * Creates a Node as MapItem
	 * 
	 * @param lat
	 * @param lon
	 * @param node
	 */
	public MapItem(double lat, double lon, Node node) {
		this(lat, lon);
		this.getChildren().add(node);
	}

	/**
	 * Creates a simple circle as mapItem
	 * 
	 * @param lat
	 * @param lon
	 * @param radius
	 *            the radius of the circle
	 * @param fill
	 *            Fill Color
	 * @param stroke
	 *            Stroke Color
	 */
	public MapItem(double lat, double lon, double radius, Color fill, Color stroke) {
		this(lat, lon, CircleBuilder.create().centerX(0).centerY(0)
				.radius(radius).stroke(fill).strokeWidth(radius / 6)
				.fill(stroke).build());
	}

	private void init(double lat, double lon) {

		// create the MapPoint
		this.position = new MapPosition(lat, lon);

		// add the listener to set the scale
		this.position.zoomProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				setScale(newValue.byteValue());
			}
		});

		// bind the layout property
		layoutXProperty().bind(position.xProperty().add(offsetX));
		layoutYProperty().bind(position.yProperty().add(offsetY));
	}

	/**
	 * @return Whether the {@link MapView} is already set
	 * @deprecated Use {@link #isZoomLevelBound()} instead
	 */
	public boolean isMapViewSet() {
		return isZoomLevelBound();
	}

	/**
	 * @return Whether the position is bound to a zoom-level
	 */
	public boolean isZoomLevelBound() {
		return isBound;
	}

	/**
	 * @param view
	 *            Binds this {@link MapItem} to the given {@link MapView}
	 */
	public void bindToZoomLevel(MapView view) {
		this.bindToZoomLevel(view.zoomProperty());
	}

	/**
	 * @param zoomLevel
	 *            Binds this {@link MapItem} to the given zoom-level
	 */
	public void bindToZoomLevel(ReadOnlyIntegerProperty zoomLevel) {
		this.position.bindToZoomLevel(zoomLevel);
		this.isBound = true;
		
		// update it
		this.setScale((byte)zoomLevel.get());
	}

	/**
	 * Sets the x, y and z scale for this {@link MapItem}
	 * 
	 * @param zoomLevel
	 */
	private void setScale(byte zoomLevel) {
		// get the scale for the given zoom-level
		double scale = getScale(zoomLevel);
		
		// set the scale
		setScaleX(scale);
		setScaleY(scale);
	}

	/**
	 * This method was mentioned to be overwritten by any sub-class, to set the
	 * own scale ratio for the given zoom-level
	 * 
	 * @param zoomLevel
	 *            zoom-level to get the scale for
	 * @return The scale for the given zoom-level
	 */
	protected double getScale(byte zoomLevel) {
		if (isAutoscalingEnabled())
			return Math.pow(2, zoomLevel) / 20000;
		else
			return 1d;
	}

	/**
	 * @return The {@link DoubleProperty} that contains the latitude value
	 */
	public DoubleProperty latitudeProperty() {
		return position.latitudeProperty();
	}

	/**
	 * @return The {@link DoubleProperty} that contains the longitude value
	 */
	public DoubleProperty longitudeProperty() {
		return position.longitudeProperty();
	}

	/**
	 * @deprecated Use {@link #offsetXProperty()}
	 */
	public DoubleProperty getOffsetXProperty() {
		return offsetX;
	}

	/**
	 * @deprecated Use {@link #offsetYProperty()}
	 */
	public DoubleProperty getOffsetYProperty() {
		return offsetY;
	}

	/**
	 * @return The {@link DoubleProperty} that contains the x offset
	 */
	public DoubleProperty offsetXProperty() {
		return offsetX;
	}

	/**
	 * @return The {@link DoubleProperty} that contains the y offset
	 */
	public DoubleProperty offsetYProperty() {
		return offsetY;
	}

	/**
	 * @return The zoom-level that is used to calculate the current values
	 */
	public ReadOnlyIntegerProperty zoomProperty() {
		return position.zoomProperty();
	}

	/**
	 * @return the autoscalingEnabled
	 */
	public boolean isAutoscalingEnabled() {
		return autoscalingEnabled.get();
	}

	/**
	 * @param autoscalingEnabled
	 *            the autoscalingEnabled to set
	 */
	public void setAutoscalingEnabled(boolean autoscalingEnabled) {
		this.autoscalingEnabled.set(autoscalingEnabled);
	}

	/**
	 * @return The {@link ContextEntry}s for this {@link MapItem}
	 */
	public List<ContextEntry> getContextEntries () {
		return contextEntries;
	}
}
