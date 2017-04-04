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

package de.itd.maps.mapsforge.properties;

import java.util.Map;

import de.itd.maps.mapsforge.tiles.MemoryTileCache;

public class GraphicsProperties extends PropertiesBase {

	public GraphicsProperties () {
		super();
	}
	
	public GraphicsProperties (PropertiesBase base) {
		super(base);
	}
	
	public GraphicsProperties (Map<String, Object> map) {
		super(map);
	}
	
	/**
	 * @return The capacity of the {@link MemoryTileCache}
	 */
	public int getMemoryTileCacheCapacity () {
		return get("capacity-cache-memory", 500);
	}
	
	public String getFileTileCachePath () {
		return get("path-cache-file", "mapdata/cache");
	}
}
