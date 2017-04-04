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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.WindowEvent;

import org.apache.log4j.Logger;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import de.itd.maps.mapsforge.MapsforgeMapContextMenu.ContextActionEvent;
import de.itd.maps.mapsforge.MapsforgeMapContextMenu.ContextEntry;
import de.itd.maps.mapsforge.properties.GraphicsProperties;
import de.itd.maps.mapsforge.tiles.ExecuteMapforge;
import de.itd.maps.mapsforge.tiles.FileTileCache;
import de.itd.maps.mapsforge.tiles.LiveRenderRule;
import de.itd.maps.mapsforge.tiles.MemoryTileCache;
import de.itd.mapsforge.javafx.maps.mapgenerator.TileCache;
import de.itd.mapsforge.javafx.maps.rendertheme.BundleRenderTheme;

public class MapsforgeMap extends StackPane {

    private Canvas canvas = new Canvas();
    private ExecuteMapforge mapforge = null;
    private MapView mapView = null;
    private GraphicsProperties properties = null;

    private MouseEvent positionBefore = null;
    private MouseEvent positionFirst = null;

    private Pane mapItems = new Pane();
    private Rectangle enforcedBounds = new Rectangle(0, 0, 100, 100);
    private MapsforgeMapContextMenu contextMenu;

    private Pane selections = new Pane();
    private Rectangle selection = null;
    
    private int prioMouseDrag = 100;

    // automatically sorts by key...
    private Map<Integer, Pane> layers = new TreeMap<>();

    private Map<EventType<?>, Event> lastEvents = new HashMap<EventType<?>, Event>();

    /**
     * those properties are a temporary workaround for compatibility to the old
     * version they have no effect.
     */
    private final DoubleProperty enforcedWidthProperty = new SimpleDoubleProperty();
    private final DoubleProperty enforcedHeightProperty = new SimpleDoubleProperty();

    public MapsforgeMap() {
    	this(new MapViewPosition(48.718712, 9.363629, (byte) 16));
    }

    public MapsforgeMap(GeoPoint point) {
    	this(new MapViewPosition(point.latitude, point.longitude, (byte) 16));
    }

    /**
     * Uses the {@link InternalRenderTheme#OSMARENDER} as {@link XmlRenderTheme},
     * alternative you can call {@link #MapsforgeMap(MapView, XmlRenderTheme)}
     * and load your {@link XmlRenderTheme} from file with {@link BundleRenderTheme}
     * @param view {@link MapView} to control the view on the map
     */
    public MapsforgeMap(MapView view) {
		// this(view, InternalRenderTheme.OSMARENDER); TODO
		this(view, new BundleRenderTheme("osmarender/osmarender.xml"));
    }

    public MapsforgeMap(MapView view, XmlRenderTheme renderTheme) {

		// auto-size configuration
		AnchorPane.setTopAnchor(this, 0.0);
		AnchorPane.setBottomAnchor(this, 0.0);
		AnchorPane.setRightAnchor(this, 0.0);
		AnchorPane.setLeftAnchor(this, 0.0);
	
		this.mapView 		= view;
		this.properties 	= new GraphicsProperties(); // TODO do not only use the default settings, load it from XML...
		this.contextMenu 	= new MapsforgeMapContextMenu(this);
		
		// add the listener to allow redrawing
    	contextMenu.addEventHandler(WindowEvent.WINDOW_HIDDEN, new EventHandler<WindowEvent>() {
    		@Override
    		public void handle(WindowEvent event) {
    			// hidden --> resume the engine
    			mapforge.setPaused(false);
    		}
		});
    	
		
		this.mapforge = new ExecuteMapforge(canvas, mapView, renderTheme, properties);
	
		// add the canvas and the mapItems
		this.getChildren().add(canvas);
		this.getChildren().add(mapItems);
		this.getChildren().add(selections);
	
		this.setClip(enforcedBounds);
	
		// bind direct does not work properly?
		heightProperty().addListener(new ChangeListener<Number>() {
		    @Override
		    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				// set the new height
				enforcedBounds.setHeight(newValue.doubleValue());
				canvas.setHeight(newValue.doubleValue());
		
				// redraw
				updateMapLater(false);
		    }
	
		});
	
		widthProperty().addListener(new ChangeListener<Number>() {
		    @Override
		    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				// set the new width
				enforcedBounds.setWidth(newValue.doubleValue());
				canvas.setWidth(newValue.doubleValue());
		
				// redraw
				updateMapLater(false);
		    }
		});
	
		// mouse dragged event
		this.setOnMouseDragged(new EventHandler<MouseEvent>() {
		    @Override
		    public void handle(MouseEvent paramT) {
			MapsforgeMap.this.onMouseDragged(paramT);
	
			// save the event
			update(paramT);
		    }
		});
	
		// mouse released event
		this.setOnMouseReleased(new EventHandler<MouseEvent>() {
		    @Override
		    public void handle(MouseEvent paramT) {
				MapsforgeMap.this.onMouseReleased(paramT);
				
				// save the event
				update(paramT);
		    }
		});
	
		// log any mouse event
		this.setOnMouseClicked(new EventHandler<MouseEvent>() {
		    @Override
		    public void handle(MouseEvent paramT) {
			onMouseClicked(paramT);
	
			// save the event
			update(paramT);
		    }
		});
	
		this.setOnScroll(new EventHandler<ScrollEvent>() {
		    public void handle(ScrollEvent paramT) {
				onScroll(paramT);
		
				// save the event
				update(paramT);
		    }
		});
    }

    /**
     * Updates the last-{@link Event}-entry for the givens {@link Event}
     * {@link EventType}
     * 
     * @param event
     *            {@link Event} to save
     */
    private void update(Event event) {
    	lastEvents.put(event.getEventType(), event);
    }

    /**
     * @param fast
     *            Whether just to redraw anything, or false for also searching
     *            and generating new images
     */
    public void updateMap(boolean fast) {
		mapforge.redrawTiles(!fast);
		mapforge.execute();
    }

    /**
     * Updates the map, but later
     * @param fast Whether just to redraw anything, or false for also searching and generatning new tiles
     */
    public void updateMapLater(final boolean fast) {
		Platform.runLater(new Runnable() {
	
		    @Override
		    public void run() {
		    	updateMap(fast);
		    }
		});
    }

    public void onMouseClicked(MouseEvent event) {
		if (event.getClickCount() == 2) {
		    if (event.getButton().equals(MouseButton.PRIMARY)) {
			changeZoom(event.getX(), event.getY(), +1);
		    }
	
		    else if (event.getButton().equals(MouseButton.SECONDARY)) {
			changeZoom(event.getX(), event.getY(), -1);
		    }
		}
    }
    
//    /**
//     * @param x X coordinate
//     * @param y Y coordinate
//     * @return The MapItem at the given position or null
//     */
//	@SuppressWarnings("deprecation")
//    private Node pickNode (double x, double y) {
//    	/*
//    	 * See http://stackoverflow.com/questions/14420569/javafx-2-2-get-node-at-coordinates-visual-tree-hit-testing ...
//    	 * So ignore in this case, until JDK8
//    	 */
//    	return mapItems.impl_pickNode(x, y);
//    }
    
    /**
     * Collects the {@link MapItem} at the given position
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return The MapItems for the given position or a empty list
     */
    public List<MapItem> pickMapItems (double x, double y) {
    	return pickMapItems(x, y, new ArrayList<MapItem>(), mapItems);
    }
    
    /**
     * @param x	X coordinate to check for
     * @param y Y coordinate to check for
     * @param list	{@link List} to add {@link Node}s to
     * @param pane	{@link Pane} to search in
     * @return The given {@link List}
     */
    private List<MapItem> pickMapItems (double x, double y, List<MapItem> list, Pane pane) {
    	for (Node node : pane.getChildren()) {
    		
    		if (node instanceof Pane) {
    			pickMapItems(x, y, list, ((Pane)node));
    			
    		} else if (node instanceof MapItem) {
    			double dx = x - node.getLayoutX();
    			double dy = y - node.getLayoutY();
    			
    			dx /= node.getScaleX();
    			dy /= node.getScaleY();
    			
    			if (node.contains(dx, dy)) {
    				list.add((MapItem)node);
    			}
    		}
    	}
    	return list;
    }
    
    
    /**
     * @param screenX X Coordinate on the screen
     * @param screenY Y coordinate on the screen
     * @param x X Coordinate relative to the parent
     * @param y Y Coordinate relative to the parent
     */
    public void onContextMenuRequested (double screenX, double screenY, double x, double y) {
    	// get the nodes that are at that position
    	List<MapItem> 					list 	= pickMapItems(x, y);
    	Map<String, List<ContextEntry>>	options	= new HashMap<>();

    	final String groupNameGrouped = "Grouped"; // TODO
    	
    	
    	Logger.getLogger(getClass()).debug("Going to create the groups");
    	// collect the options for each item to be able to group them
    	for (MapItem node: list) {
    		for (ContextEntry entry : node.getContextEntries()) {
    			String				text	= entry.textProperty().get();
    			List<ContextEntry> 	l		= options.get( text );
    			
    			if (l == null) {
    				l = new ArrayList<>();
    				options.put(text, l);
    			}
    			
    			l.add(entry);
    		}
//    		counter +=1;
    	}
    	
    	Logger.getLogger(getClass()).debug("Going to link grouped menues");
    	List<ContextEntry> contextEntries = new ArrayList<>();
    	for (final Entry<String, List<ContextEntry>> entry : options.entrySet()) {
    		
    		contextEntries.add(new ContextEntry() {
    			
    			private StringProperty text = new SimpleStringProperty(entry.getKey() + " ("+entry.getValue().size()+")");
				
				@Override
				public ReadOnlyStringProperty textProperty() {
					return text;
				}
				
				@Override
				public void onAction(ContextActionEvent event) {
					// pass the event
					for (ContextEntry e : entry.getValue()) {
						e.onAction(event);
					}
				}
			});
    		
    	}
    	
    	// add them all
    	Logger.getLogger(getClass()).debug("Going to add the entries: "+contextEntries.size());
    	contextMenu.add(contextEntries, true, groupNameGrouped);
    	
    	// add the actual menu items
    	for (MapItem node: list) {
    		contextMenu.add(node.getContextEntries(), true, node.toString());
    	}
    	
    	Logger.getLogger(getClass()).debug("Going to disable canvas-redraw");
    	// stop redrawing as long as the menu is shown
    	mapforge.setPaused(true);
    	
    	Logger.getLogger(getClass()).debug("Going to show ContextMenu");
    	// show the menu
    	contextMenu.show(canvas, screenX, screenY, x, y);
    	
    	Logger.getLogger(getClass()).debug("End of request-processing");
    }

    /**
     * @param event
     *            {@link MouseEvent} for this action
     */
    public void onMouseReleased(final MouseEvent event) {

		// has not moved?
		if (positionBefore == null && MouseButton.SECONDARY.equals(event.getButton())) {
			
			if (event.getClickCount() == 1) {
				// move it slightly to the down right, so the mouse is still
				// on the canvas, and a double-click can be recognized
				int offset = 1; // TODO in the new design of java 8 more (like 15) is needed, bug or design change?
				onContextMenuRequested(event.getScreenX()+offset, event.getScreenY()-offset, event.getX()+offset, event.getY()-offset);
				return;
				
			} else {
				// double click, no context menu requested
				contextMenu.hide();
				
			}
		}
	
		this.positionBefore	= null;
		this.positionFirst	= null;
		this.selections.getChildren().clear();
	
		if (selection != null) {
		    GeoPoint start = getGeoPoint(selection.getLayoutX(), selection.getLayoutY());
		    GeoPoint end = getGeoPoint(
			    selection.getLayoutX() + selection.getWidth(),
			    selection.getLayoutY() + selection.getHeight()
			    );
		    GeoPoint center = getGeoPoint(
			    selection.getLayoutX() + selection.getWidth() / 2,
			    selection.getLayoutY() + selection.getHeight() / 2);
	
		    double maxZoom = MapViewPosition.ZOOM_MAX;
		    double minZoom = MapViewPosition.ZOOM_MIN;
	
		    double width = MercatorProjection.longitudeToPixelX(start.longitude, mapView.getZoomLevel())
			    - MercatorProjection.longitudeToPixelX(end.longitude, mapView.getZoomLevel());
		    double height = MercatorProjection.latitudeToPixelY(start.latitude, mapView.getZoomLevel())
			    - MercatorProjection.latitudeToPixelY(end.latitude, mapView.getZoomLevel());
	
		    double zoomModifier = -1;
		    int zoom = mapView.getZoomLevel();
	
		    if (width < getWidth() && height < getHeight()) {
		    	zoomModifier = 1;
		    }
	
		    for (; zoom >= minZoom && zoom < maxZoom; zoom += zoomModifier) {
				// calculate the new width and height
				width = MercatorProjection.longitudeToPixelX(end.longitude, (byte) zoom)
					- MercatorProjection.longitudeToPixelX(start.longitude, (byte) zoom);
				height = MercatorProjection.latitudeToPixelY(end.latitude, (byte) zoom)
					- MercatorProjection.latitudeToPixelY(start.latitude, (byte) zoom);
	
				if (width >= getWidth() || height >= getHeight()) {
				    zoom -= zoomModifier;
				    break;
				}
		    }
	
		    // be sure its not above max or below min
		    zoom = Math.max(zoom, MapViewPosition.ZOOM_MIN);
		    zoom = Math.min(zoom, MapViewPosition.ZOOM_MAX);
		    mapView.set(center.latitude, center.longitude, (byte) zoom);
		    selection = null;
		}
	
		this.setPaused(false);
		this.updateMap(false);
    }

    /**
     * @param event
     *            {@link MouseEvent} for this action
     */
    private void onMouseDragged(MouseEvent event) {

		if (selection == null && event.isPrimaryButtonDown() && event.isShiftDown()) {
		    selection = new Rectangle();
		    selection.layoutXProperty().set(event.getX());
		    selection.layoutYProperty().set(event.getY());
	
		    selection.setFill(Color.TRANSPARENT);
		    selection.setStroke(Color.BLUE);
	
		    selections.getChildren().add(selection);
		    positionFirst = event;
		}
	
		else if (selection == null && positionBefore != null) {
			getMapView().unfollow (prioMouseDrag);
		    getMapView().movePixel(positionBefore.getX() - event.getX(), positionBefore.getY() - event.getY());
	
		    // position has been moved, update the map
		    updateMap(true);
		}
	
		else if (selection != null) {
		    double width = event.getX() - positionFirst.getX();
		    double height = event.getY() - positionFirst.getY();
	
		    if (width < 0) {
			width *= -1;
			selection.setLayoutX(positionFirst.getX() - width);
		    }
	
		    if (height < 0) {
			height *= -1;
			selection.setLayoutY(positionFirst.getY() - height);
		    }
	
		    selection.setWidth(width);
		    selection.setHeight(height);
		}
	
		this.positionBefore = event;
		this.setPaused(true);
    }
    
    /**
     * @param priority The priority to use, to un-follow on a mouse-drag
     */
    public void setPriorityMouseDrag (int priority) {
    	this.prioMouseDrag = priority;
    }
    
    /**
     * @return The priority used to un-follow on a mouse-drag
     */
    public int getPriorityMouseDrag () {
    	return this.prioMouseDrag;
    }

    /**
     * Zooms into or out the map and redraws it
     * 
     * @param event
     *            The {@link ScrollEvent} that was thrown
     */
    private void onScroll(ScrollEvent event) {
		if (event.getDeltaY() > 0) {
		    // zoom in
		    changeZoom(event.getX(), event.getY(), +1);
	
		} else if (event.getDeltaY() < 0) {
		    // zoom out
		    changeZoom(event.getX(), event.getY(), -1);
		}
    }

    /**
     * Changes the zoom level by the given modifier,
     * and moves the map, so that the given coordinate
     * is on the same GPS coordinate as before
     * @param x	X coordinate on the screen
     * @param y Y coordinate on the screen
     * @param modifier Zoom modifier
     */
    private void changeZoom(double x, double y, int modifier) {
		// get the GeoPoint position, where the mouse is at this moment
		GeoPoint target = getGeoPoint(x, y);
	
		// change the zoom
		mapView.setZoomLevel((byte) (mapView.getZoomLevel() + modifier));
	
		// get the GeoPoint position for the same mouse position, but new zoom
		// level
		GeoPoint current = getGeoPoint(x, y);
	
		// move the difference
		mapView.moveCoordinate(target.latitude - current.latitude, target.longitude - current.longitude);
		updateMap(false);
    }

    /**
     * Executes the given {@link Runnable} in the FX-Thread, if this
     * method is called in the FX-Thread, it is executed immediately
     * 
     * @param runnable
     *            {@link Runnable} to execute
     */
    public static void runInFXThread(Runnable runnable) {
		runInFXThread(runnable, false);
    }
    
    /**
     * Executes the given {@link Runnable} in the FX-Thread, if this
     * method is called in the FX-Thread, it is executed immediately
     * 
     * @param runnable	{@link Runnable} to execute
     * @param block		Whether to block until executed, if this isn't the FX-Thread
     */
    public static void runInFXThread(final Runnable runnable, boolean block) {
    	
    	if (Platform.isFxApplicationThread()) {
    		// just run it
		    runnable.run();
		    
		} else {
			// value to wait and notify, and to indicate whether already executed
	    	final BooleanProperty lock = new SimpleBooleanProperty(!block);
	    				
		    Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					try {
						// execute
						runnable.run();
						
					} finally {
						// notify
						synchronized (lock) {
							lock.set(true);
							lock.notifyAll();
						}
						
					}
				}
			});
		    
		    synchronized (lock) {
		    	// not executed yet?
		    	if (!lock.get()) {
		    		try {
		    			
		    			// wait for notify
		    			lock.wait();
		    			
		    		} catch (Throwable t) {
		    			throw new RuntimeException(t);
		    		}
		    	}
		    }
		}
    }
    
    
    /**
     * Adds the given Node to the {@link Group} of map-items. Will set the
     * {@link MapView}, if the given {@link Node} is a {@link MapView} and
     * {@link MapItem#isMapViewSet()} is false
     * 
     * @param item
     * 
     *            {@link Node} to add
     * 
     */
    public void addMapItem(MapItem item) {
    	addMapItem(null, item);
    }

    /**
     * Adds the given Node to the {@link Group} of map-items. Will set the
     * {@link MapView}, if the given {@link Node} is a {@link MapView} and
     * {@link MapItem#isMapViewSet()} is false
     * 
     * @param layerLevel
     * 
     * @param container
     * 
     *            {@link Node} to add
     * 
     */
    public void addMapItem(Integer layerLevel, final MapItem container) {
		// "null" is not permitted
		final Integer layer = layerLevel != null ? layerLevel : 0;
	
		// is the Node a MapItem?
		if (container instanceof MapItem) {
		    MapItem item = ((MapItem) container);
		    
	
		    // set the MapView if not already set
		    if (!item.isZoomLevelBound()) {
		    	item.bindToZoomLevel(mapView);
		    }
	
		    // bind the offsets
		    item.offsetXProperty().bind(mapView.xProperty().subtract(mapforge.offsetXProperty()).negate());
		    item.offsetYProperty().bind(mapView.yProperty().subtract(mapforge.offsetYProperty()).negate());
		}
	
		// add it
		runInFXThread(new Runnable() {
		    @Override
		    public void run() {
	
				// create the layer if it does not exist yet
				if (!layers.containsKey(layer)) {
				    // create and add the new pane
					Pane pane = new Pane();
					
					layers.put(layer, pane);
		
				    // re-add all the layers, but sorted
				    mapItems.getChildren().clear();
				    mapItems.getChildren().addAll(layers.values());
				}
		
				// add the new container
				layers.get(layer).getChildren().add(container);
		    }
		});
    }

    /**
     * @param container
     *            {@link Node} to remove
     * @return Whether this {@link MapsforgeMap} contained the given {@link Node}
     */
    public boolean removeMapItem(final Node container) {

		for (final Pane pane : layers.values()) {
		    if (pane.getChildren().contains(container)) {
				runInFXThread(new Runnable() {
				    @Override
				    public void run() {
				    	pane.getChildren().remove(container);
				    }
				});
				return true;
		    }
		}
	
		// not removed
		return false;
    }


    /**
     * @return {@link ObservableList} of all added {@link MapItem}s
     */
//  @SuppressWarnings("unchecked")
    public Iterable<Node> getMapItems() {
	
//		List<Node> nodes = new ArrayList<>();
//		for (Pane layer : layers.values()) {
//		    nodes.addAll(layer.getChildren());
//		}
//	
//		return nodes;
		
		/*
		 * In-line Iterator, should be faster,
		 * since there is no list copying
		 */
		return new Iterable<Node>() {
			@Override
			public Iterator<Node> iterator() {
				return new Iterator<Node>() {
					
					private Iterator<Pane>	itrBig		= layers.values().iterator();
					private Iterator<Node>	itrSmall;

					@Override
					public boolean hasNext() {
						return (itrSmall != null && itrSmall.hasNext())
							|| (itrBig   != null && itrBig.hasNext());
					}

					@Override
					public Node next() {
						if (itrSmall == null || !itrSmall.hasNext()) {
							itrSmall = itrBig.next().getChildren().iterator();
						}
						
						return itrSmall.next();
					}

					@Override
					public void remove() {
						itrSmall.next();
					}
					
				};
			}
		};
    }

    /**
     * @param x
     *            Coordinate x
     * @param y
     *            Coordinate y
     * @return The {@link GeoPoint} (so the GPS-coordinates) for the given x and
     *         y coordinate on the this map
     */
    public GeoPoint getGeoPoint(double x, double y) {
		// get the absolute x and y values
		x = getMapView().getX() + (x - canvas.getWidth() / 2);
		y = getMapView().getY() + (y - canvas.getHeight() / 2);
	
		// get the latitude and longitude value
		double latitude = MercatorProjection.pixelYToLatitude(y, getMapView()
			.getZoomLevel());
		double longitude = MercatorProjection.pixelXToLongitude(x, getMapView()
			.getZoomLevel());
	
		return new GeoPoint(latitude, longitude);
    }

    /**
     * @return The last {@link Event} for the given {@link EventType} or null
     */
    @SuppressWarnings("unchecked")
    // since T extends Event, this is totally fine...
    public <T extends Event> T getLastEventFor(EventType<T> type) {
    	return (T) lastEvents.get(type);
    }

    /**
     * Sets the position at the center of the map
     * 
     * @param lat
     *            Latitude coordinate
     * @param lon
     *            Longitude coordinate
     * @deprecated Use {@link MapView#set(double, double)} on {@link #getMapView()}
     */
    public void setCenter(double lat, double lon) {
    	mapView.set(lat, lon);
    }

    /**
     * Sets the zoom level for the current position
     * 
     * @param zoomLevel
     *            Zoom-Level to set
     * @deprecated Use {@link MapView#setZoomLevel(byte)} on {@link #getMapView()}
     */
    public void setZoom(byte zoomLevel) {
    	mapView.setZoomLevel(zoomLevel);
    }

    /**
     * Loads the map from the given {@link File}
     * 
     * @param file
     *            {@link File} to load the map from
     */
    public void loadMap(File file) {
    	this.mapforge.load(file);
    }

    /**
     * @return Whether there is already a loaded map
     */
    public boolean hasLoadedMap() {
    	return this.mapforge.hasLoaded();
    }

    /**
     * @return The {@link MapView} for this {@link MapsforgeMap}
     */
    public MapView getMapView() {
    	return mapView;
    }

    /**
     * @return the enforcedWidthProperty
     */
    @Deprecated
    public DoubleProperty enforcedWidthProperty() {
    	return enforcedWidthProperty;
    }

    /**
     * @return the enforceHeightDoubleProperty
     */
    @Deprecated
    public DoubleProperty enforcedHeightProperty() {
    	return enforcedHeightProperty;
    }

    public void shutdown() {
    	// TODO
    }

    /**
     * This {@link BooleanProperty} describes whether the {@link FileTileCache}
     * is allowed to be used. If the value is set to false, every {@link Tile}
     * is needs to be rendered before it can be displayed. Also it will be
     * discarded if the {@link MemoryTileCache} is full
     * 
     * @return The {@link BooleanProperty} that indicates whether the
     *         {@link FileTileCache} is allowed to be used
     */
    public BooleanProperty useFileTileCacheProperty() {
    	return mapforge.getMemoryTileCache().useFileTileCacheProperty();
    }

    /**
     * this {@link ReadOnlyIntegerProperty} describes how many {@link Tile}s are
     * currently in the {@link FileTileCache}
     * 
     * @return The {@link ReadOnlyIntegerProperty} for the current usage of the
     *         {@link FileTileCache}
     */
    public ReadOnlyIntegerProperty fileTileCacheTileCount() {
    	return mapforge.getFileTileCache().loadProperty();
    }

    /**
     * This {@link IntegerProperty} describes the maximum capacity of the
     * {@link FileTileCache}
     * 
     * @return The {@link IntegerProperty} for the capacity of the
     *         {@link FileTileCache}
     */
    public IntegerProperty fileTileCacheCapacity() {
    	return mapforge.getFileTileCache().capacityProperty();
    }

    /**
     * This {@link ReadOnlyIntegerProperty} describes how many {@link Tile}s are
     * currently in the {@link MemoryTileCache}
     * 
     * @return The {@link ReadOnlyIntegerProperty} for the current usage of the
     *         {@link MemoryTileCache}
     */
    public ReadOnlyIntegerProperty memoryTileCacheTileCount() {
    	return mapforge.getMemoryTileCache().loadProperty();
    }

    /**
     * This {@link IntegerProperty} describes the maximum capacity of the
     * {@link MemoryTileCache}
     * 
     * @return The {@link IntegerProperty} for the capacity of the
     *         {@link MemoryTileCache}
     */
    public IntegerProperty memoryTileCacheCapacity() {
    	return mapforge.getMemoryTileCache().capacityProperty();
    }
    
    /**
     * Clears the {@link TileCache} in the memory
     */
    public void clearMemoryTileCache () {
    	mapforge.getMemoryTileCache().clear();
    }

    /**
     * @return the mapContextMenu
     */
    public MapsforgeMapContextMenu getMapContextMenu() {
    	return contextMenu;
    }

    /**
     * @param pause Whether the drawing of new {@link Tile}s should be paused
     */
    public void setPaused(boolean pause) {
    	mapforge.setPaused(pause);
    }

    /**
     * @return Whether the drawing of {@link Tile}s is currently paused
     */
    public boolean isPaused() {
    	return mapforge.isPaused();
    }
    
    /**
     * Destroys this instance and frees the memory
     */
    public void destroy () {
    	mapforge.destroy();
    }
    
    /**
     * @return whether this instance has been destroyed
     */
    public boolean isDestroyed () {
    	return mapforge.isDestroyed();
    }
    
    /**
     * @return The {@link LiveRenderRule} to modify what to render live
     */
    public LiveRenderRule getLiveRenderRule () {
    	return mapforge.getLiveRenderRule();
    }

    /**
     * Returns the last right clicks position
     * @deprecated Still in use? Use {@link #getLastEventFor(EventType)}, returns last {@link MouseEvent#MOUSE_CLICKED}
     * @return
     */
    public Point2D getLastRightMouseClickPosition() {

		// get the last mouse-click-event
		MouseEvent event = getLastEventFor(MouseEvent.MOUSE_CLICKED);
	
		if (event != null) {
		    // to Point2D
		    return new Point2D(event.getX(), event.getY());
		}
	
		else {
		    // nothing available
		    return null;
		}
    }
}
