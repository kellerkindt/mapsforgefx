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

package de.itd.maps.mapsforge.tiles;

import java.util.HashSet;
import java.util.Set;

public class LiveRenderRule {

	public static enum Drawable {
		AREA,
		AREA_CAPTION,
		AREA_SYMBOL,
		POINT_OF_INTEREST,
		POINT_OF_INTEREST_CAPTION,
		POINT_OF_INTEREST_CIRCLE,
		POINT_OF_INTEREST_SYMBOL,
		WAY,
		WAY_SYMBOL,
		WAY_TEXT,
		WAY_SEPARATOR,
		BACKGROUND_WATER,
		;
	}
	
	private Set<Drawable> set = new HashSet<>();
	
	/**
	 * @param render
	 * @return Whether it is allowed to render the given {@link Drawable}
	 */
	public synchronized boolean isAllowed (Drawable render) {
		return !set.contains(render);
	}
	
	/**
	 * @param type		{@link Drawable} to set
	 * @param allowed	Whether to allow to render the given {@link Drawable}
	 */
	public synchronized void setAllowed (Drawable type, boolean allowed) {
		if (allowed) {
			set.remove(type);
		} else {
			set.add(type);
		}
	}
}
