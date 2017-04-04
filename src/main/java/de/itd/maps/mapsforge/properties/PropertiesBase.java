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

import java.util.HashMap;
import java.util.Map;

public class PropertiesBase implements Properties {
	
	private Map<String, Object> map = null;
	
	public PropertiesBase () {
		this(new HashMap<String, Object>());
	}
	
	public PropertiesBase (PropertiesBase base) {
		this(new HashMap<>(base.map));
	}
	
	public PropertiesBase (Map<String, Object> map) {
		this.map = map;
	}

	@Override
	public Object get(String key) {
		return map.get(key);
	}

	@Override
	@SuppressWarnings("unchecked") // it is checked
	public <T> T get(String key, T alternative) {
		// try to get the value
		T value = get(key, (Class<T>)alternative.getClass());
		
		// decide what to return
		if (value == null) {
			return alternative;
		} else {
			return value;
		}
	}

	@Override
	@SuppressWarnings("unchecked") // it is checked
	public <T> T get(String key, Class<T> clazz) {
		// get the value
		Object value = get(key);
		
		// if null, nothing needs to be cast
		if (value == null || clazz == null || !clazz.isInstance(value)) {
			return null;
		} else {
			return (T)value;
		}
	}

	@Override
	public void set(String key, Object value) {
		map.put(key, value);
	}

	@Override
	public Iterable<String> iterableKeys() {
		return map.keySet();
	}

	@Override
	public Iterable<Object> iterableValues() {
		return map.values();
	}

}
