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

public interface Properties {

	/**
	 * @param key Key to get the value for
	 * @return The value for the given key or null
	 */
	public Object get(String key);
	
	/**
	 * @param key			Key to get the value for
	 * @param alternative	Value to return if there is no value set (or its null)
	 * @return The value for the given key or the alternative value
	 */
	public <T> T get(String key, T alternative);
	
	/**
	 * @param key	Key to get the value for
	 * @param clazz {@link Class} that the value needs to be to be returned
	 * @return The value for the given key of the given {@link Class} or null
	 */
	public <T> T get(String key, Class<T> clazz);
	
	/**
	 * @param key	Key to bind the value to
	 * @param value Value to set for the given key
	 */
	public void set(String key, Object value);
	
	/**
	 * @return An {@link Iterable} of all keys
	 */
	public Iterable<String> iterableKeys ();
	
	/**
	 * @return An {@link Iterable} of all values
	 */
	public Iterable<Object> iterableValues ();
}
