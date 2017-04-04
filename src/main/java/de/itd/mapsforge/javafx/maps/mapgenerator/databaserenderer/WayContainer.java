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

package de.itd.mapsforge.javafx.maps.mapgenerator.databaserenderer;

import org.mapsforge.core.model.Point;
import org.mapsforge.map.reader.Way;

/**
 * modified 2013-10-29 by mwatzko,
 * added the {@link Way} attribute
 */
class WayContainer implements ShapeContainer {
	
    final Point[][] coordinates;
    
    /**
     *  The {@link Way} for this {@link WayContainer}, can be null
     */
    final Way       way;
    
    WayContainer(Point[][] coordinates) {
           this(null, coordinates);
    }

    WayContainer(Way way, Point[][] coordinates) {
            this.way         = way;
            this.coordinates = coordinates;
    }

    @Override
    public ShapeType getShapeType() {
            return ShapeType.WAY;
    }
}
