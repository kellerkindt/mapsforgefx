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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.WindowEvent;

import javax.naming.Context;

import org.apache.log4j.Logger;
import org.mapsforge.core.model.GeoPoint;

public class MapsforgeMapContextMenu extends ContextMenu {

	private Map<String, Menu>			menues		= new HashMap<>();
    private List<ContextEntry> 			showOnce	= new ArrayList<>();

    private Map<ContextEntry, MenuItem> items 		= new HashMap<>();

    private Point2D positionScreen  = new Point2D(0, 0);
    private Point2D positionMap		= new Point2D(0, 0);

    private MapsforgeMap mapsforge = null;
    
    // TODO workaround
    private List<MenuItem> 		toAdd 				= new ArrayList<>();
    private SeparatorMenuItem	dynamicSeparator	= new SeparatorMenuItem();

    public MapsforgeMapContextMenu(MapsforgeMap map) {
    	this.mapsforge = map;
    	

		addEventHandler(WindowEvent.WINDOW_HIDDEN, new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				// invoke auto-remove
				Logger.getLogger(getClass()).debug("-- clenaup -- ");
				remove(showOnce);
				showOnce.clear();
				getItems().remove(dynamicSeparator);
				
//				// force gc
//				Logger.getLogger(getClass()).debug("-- gc --");
//				WeakReference<Object> ref = new WeakReference<Object>(new Object());
//				while (ref.get() != null) {
//					System.gc();
//				}
				
				Logger.getLogger(getClass()).debug("-- cleanup done --");
			}
		});
    }
    
	@Override
	public void show(Node anchor, double screenX, double screenY) {
		// do not use this, use the other one
		throw new SecurityException("MapX and MapY coordinates are required");
		
	}
    
    public void show(Node anchor, double screenX, double screenY, double mapX, double mapY) {
    	this.positionScreen	= new Point2D(screenX, screenY);
    	this.positionMap	= new Point2D(mapX, mapY);
    	
    	getItems().add(dynamicSeparator);
    	getItems().addAll(toAdd);
    	toAdd.clear();
    	
		super.show(anchor, screenX, screenY);
		
    }
    
    /**
     * Removes all {@link MenuItem}s from this
     * {@link MenuItem} that are in the given {@link Collection}
     * 
     * @param entries {@link ContextEntry}s to remove
     */
    public void remove (final Collection<ContextEntry> entries) {
	    MapsforgeMap.runInFXThread(new Runnable() {
			@Override
			public void run() {
				// remove them all
				List<MenuItem> toRemove = new ArrayList<>();
				
				for (ContextEntry entry : entries) {
					collectToRemove(entry, toRemove);
				}
						
				removeRaw(toRemove);
			}
	    }, true);
    }

    /**
     * @param entry {@link ContextEntry} to remove
     * @return Whether the {@link MenuItem} was removed 
     */
    public boolean remove(final ContextEntry entry) {
    	
    	// do not even create the Runnable, if the given
    	// ContextEntry is invalid
	    MapsforgeMap.runInFXThread(new Runnable() {
			@Override
			public void run() {
				removeRaw( collectToRemove(entry, new ArrayList<MenuItem>()) );
			}
	    }, true);
	    return true;
    }
    
    /**
     * Collects all {@link MenuItem}s that need to be removed
     * to remove the given {@link ContextEntry}. Does also remove
     * entries in some cases (i.e.  from a {@link Menu}) directly!
     * 
     * @param entry		{@link ContextEntry} that shall be removed
     * @param toRemove	A {@link List} to add all {@link MenuItem}s that need to be removed
     * @return The given {@link List}
     */
    private List<MenuItem> collectToRemove (ContextEntry entry, List<MenuItem> toRemove) {
    	// get the parent
		MenuItem	item	= items.remove(entry);
		Menu 		menu 	= item != null ? item.getParentMenu() : null;
		
		if (menu != null) {
			// remove it from the menu
			menu.getItems().remove(item);
			
			// remove the menu if empty
			if (menu.getItems().isEmpty()) {
				menues.remove(menu.getText());
				toRemove.add(menu);
			}
			
		} else {
			// remove it from the actual context menu
			toRemove.add(item);
		}
		
		return toRemove;
    }

    /**
     * Removes the given {@link Collection} directly from the
     * {@link MenuItem}s of this {@link ContextMenu}
     * @param items {@link Collection} of {@link MenuItem}s to remove
     */
    private void removeRaw (Collection<MenuItem> items) {
    	// remove them all from the items
    	getItems().removeAll(items);
    }
    
    /**
     * Adds the given {@link MenuItem} either to the
     * internal temporary list - if the {@link ContextMenu}
     * isn't shown currently - or directly to the {@link ContextMenu}
     * Adding many items to an {@link ContextMenu} slow it enormous down
     * 
     * @param item {@link Menu} to add
     */
    private void addRaw (MenuItem item) {
    	/*
    	 *  getItems().add(...) does not like to be 
    	 *  called very often, so add it to a temporary
    	 *  list until it is shown
    	 */
    	if (isShowing()) {
    		getItems().add(item);
    	} else {
    		toAdd.add(item);
    	}
    }
    

    /**
     * NOTE: needs to be called by the FX Thread
     * 
     * @param entry {@link ContextEntry} to add
     * @param once  Whether to show the {@link ContextEntry} only once (auto-remove)
     * @param group The {@link Menu} group or null for the default group
     */
    private void addFX (final ContextEntry entry, final boolean once, final String group) {
		// create the item
		final MenuItem item = new MenuItem();

		// bind the text and add the action listener
		item.textProperty().bind(entry.textProperty());
		item.setOnAction(new EventHandler<ActionEvent>() {
		    @Override
		    public void handle(ActionEvent event) {
				// contact the
				entry.onAction(new ContextActionEvent(mapsforge, entry, positionScreen, positionMap));
		    }
		});
		
		// add the MenuItem to correct Menu
		if (group == null) {
			// no menu
			addRaw(item);
			
		} else {
			// get the menu
			Menu menu = menues.get(group);
			
			if (menu == null) {
				menu = new Menu(group);
				menues.put(group, menu);
				addRaw(menu);
			}
			
			menu.getItems().add(item);
		}
		
		// add the binding
		items.put(entry, item);

		if (once) {
			showOnce.add(entry);
		}
    }
    
    
    /**
     * @param group Name of the group to create
     */
    public void createGroup (String group) {
    	createGroup(group, false);
    }

    /**
     * @param group		Name of the group to create
     * @param overwrite Whether to overwrite the old menu (if it exists)
     */
    public void createGroup (String group, boolean overwrite) {
    	if (!menues.containsKey(group) || overwrite) {
    		
    		// be sure to delete the old one
    		if (menues.containsKey(group)) {
    			getItems().remove( menues.remove(group) );
    		}
    		
    		Menu menu = new Menu(group);
    		menues.put(group, menu);
    		getItems().add(menu);
    	}
    }

    /**
     * @param entry {@link Context} to add
     */
    public void add(ContextEntry entry) {
    	add(entry, false);
    }
    
    /**
     * @param entry {@link ContextEntry} to add
     * @param once  Whether to show the {@link MenuItem} only once (auto-remove)
     */
    public void add(ContextEntry entry, boolean once) {
    	add(entry, once, null);
    }
    

    /**
     * @param entry {@link ContextEntry} to add
     * @param group The {@link Menu} group or null for the default group
     */
    public void add(ContextEntry entry, String group) {
    	add(entry, false, group);
    }

    /**
     * @param entry {@link ContextEntry} to add
     * @param once  Whether to show the {@link MenuItem} only once (auto-remove)
     * @param group The {@link Menu} group or null for the default group
     */
    public void add(final ContextEntry entry, final boolean once, final String group) {
		MapsforgeMap.runInFXThread(new Runnable() {
		    @Override
		    public void run() {
				addFX(entry, once, group);
		    }
		});
    }
    
    /**
     * @param entries Collections of {@link ContextEntry}s to add
     */
    public void add(Collection<ContextEntry> entries) {
    	add(entries, false);
    }
    
    /**
     * @param entries Collections of {@link MenuItem}s to add
     * @param once    Whether to show the given {@link MenuItem}s only once (auto-remove)
     */
    public void add(final Collection<ContextEntry> entries, final boolean once) {
    	add(entries, once, null);
    }
    
    /**
     * @param entries Collections of {@link MenuItem}s to add
     * @param once    Whether to show the given {@link MenuItem}s only once (auto-remove)
     * @param group The {@link Menu} group or null for the default group
     */
    public void add(final Collection<ContextEntry> entries, final boolean once, final String group) {    	
    	MapsforgeMap.runInFXThread(new Runnable() {
			@Override
			public void run() {
				for (ContextEntry entry : entries) {
					addFX(entry, once, group);
				}
			}
		});
    }
    
    

    public interface ContextEntry {

		/**
		 * @return The text to display in the {@link ContextMenu}
		 */
		public ReadOnlyStringProperty textProperty();
	
		/**
		 * Is called, if the {@link MenuItem} for this
		 * {@link ContextEntry} has been clicked on
		 * @param event The {@link ContextActionEvent} with additional information
		 */
		public void onAction(ContextActionEvent event);
    }

    public class ContextActionEvent {

		private Point2D positionScreen 		= null;
		private Point2D positionMap			= null;
		private MapsforgeMap 	mapsforge 	= null;
		private ContextEntry	entry 		= null;
		private GeoPoint 		geoPoint 	= null;
	
		public ContextActionEvent(MapsforgeMap map, ContextEntry entry, Point2D positionScreen, Point2D positionMap) {
		    this.mapsforge 		= map;
		    this.entry 			= entry;
		    this.positionScreen = positionScreen;
		    this.positionMap	= positionMap;
		    this.geoPoint 		= mapsforge.getGeoPoint(positionMap.getX(), positionMap.getY());
		}
	
		/**
		 * The position the initial event took place, relative to the screen origin,
		 * mostly the position where the {@link ContextMenu} has been opened
		 * @return The position for this {@link ContextActionEvent}
		 */
		public Point2D getPositionScreen() {
		    return positionScreen;
		}
		
		/**
		 * The position the initial event took place, relative to the map origin,
		 * mostly the position where the {@link ContextMenu} has been opened
		 * @return The position for this {@link ContextActionEvent}
		 */
		public Point2D getPositionMap () {
			return positionMap;
		}
	
		public GeoPoint getGeoLocation() {
		    return geoPoint;
		}
	
		/**
		 * @return The current {@link MapsforgeMap}
		 */
		public MapsforgeMap getMapsforgeMap() {
		    return mapsforge;
		}
	
		/**
		 * @return The {@link ContextEntry} this event is associated with
		 */
		public ContextEntry getContextEntry() {
		    return entry;
		}
	
		public MapsforgeMapContextMenu getContextMenu() {
		    return MapsforgeMapContextMenu.this;
		}

    }
}
