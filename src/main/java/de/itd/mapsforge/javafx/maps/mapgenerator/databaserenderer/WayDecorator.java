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

import java.util.List;

import org.mapsforge.core.model.Point;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.graphics.Paint;

final class WayDecorator {
        /**
         * Minimum distance in pixels before the symbol is repeated.
         */
        private static final int DISTANCE_BETWEEN_SYMBOLS = 200;

        /**
         * Minimum distance in pixels before the way name is repeated.
         */
        private static final int DISTANCE_BETWEEN_WAY_NAMES = 500;

        /**
         * Distance in pixels to skip from both ends of a segment.
         */
        private static final int SEGMENT_SAFETY_DISTANCE = 30;

        static void renderSymbol(Bitmap symbolBitmap, boolean alignCenter, boolean repeatSymbol, Point[][] coordinates,
                        List<SymbolContainer> waySymbols) {
                int skipPixels = SEGMENT_SAFETY_DISTANCE;

                // get the first way point coordinates
                double previousX = coordinates[0][0].x;
                double previousY = coordinates[0][0].y;

                // draw the symbol on each way segment
                float segmentLengthRemaining;
                float segmentSkipPercentage;
                float symbolAngle;
                for (int i = 1; i < coordinates[0].length; ++i) {
                        // get the current way point coordinates
                        double currentX = coordinates[0][i].x;
                        double currentY = coordinates[0][i].y;

                        // calculate the length of the current segment (Euclidian distance)
                        double diffX = currentX - previousX;
                        double diffY = currentY - previousY;
                        double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);
                        segmentLengthRemaining = (float) segmentLengthInPixel;

                        while (segmentLengthRemaining - skipPixels > SEGMENT_SAFETY_DISTANCE) {
                                // calculate the percentage of the current segment to skip
                                segmentSkipPercentage = skipPixels / segmentLengthRemaining;

                                // move the previous point forward towards the current point
                                previousX += diffX * segmentSkipPercentage;
                                previousY += diffY * segmentSkipPercentage;
                                symbolAngle = (float) Math.toDegrees(Math.atan2(currentY - previousY, currentX - previousX));

                                Point point = new Point(previousX, previousY);
                                waySymbols.add(new SymbolContainer(symbolBitmap, point, alignCenter, symbolAngle));

                                // check if the symbol should only be rendered once
                                if (!repeatSymbol) {
                                        return;
                                }

                                // recalculate the distances
                                diffX = currentX - previousX;
                                diffY = currentY - previousY;

                                // recalculate the remaining length of the current segment
                                segmentLengthRemaining -= skipPixels;

                                // set the amount of pixels to skip before repeating the symbol
                                skipPixels = DISTANCE_BETWEEN_SYMBOLS;
                        }

                        skipPixels -= segmentLengthRemaining;
                        if (skipPixels < SEGMENT_SAFETY_DISTANCE) {
                                skipPixels = SEGMENT_SAFETY_DISTANCE;
                        }

                        // set the previous way point coordinates for the next loop
                        previousX = currentX;
                        previousY = currentY;
                }
        }

        static void renderText(String textKey, Paint fill, Paint stroke, Point[][] coordinates,
                        List<WayTextContainer> wayNames) {
                // calculate the way name length plus some margin of safety
                int wayNameWidth = fill.getTextWidth(textKey) + 10;

                int skipPixels = 0;

                // get the first way point coordinates
                double previousX = coordinates[0][0].x;
                double previousY = coordinates[0][0].y;

                // find way segments long enough to draw the way name on them
                for (int i = 1; i < coordinates[0].length; ++i) {
                        // get the current way point coordinates
                        double currentX = coordinates[0][i].x;
                        double currentY = coordinates[0][i].y;

                        // calculate the length of the current segment (Euclidian distance)
                        double diffX = currentX - previousX;
                        double diffY = currentY - previousY;
                        double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);

                        if (skipPixels > 0) {
                                skipPixels -= segmentLengthInPixel;
                        } else if (segmentLengthInPixel > wayNameWidth) {
                                double[] wayNamePath = new double[4];
                                // check to prevent inverted way names
                                if (previousX <= currentX) {
                                        wayNamePath[0] = previousX;
                                        wayNamePath[1] = previousY;
                                        wayNamePath[2] = currentX;
                                        wayNamePath[3] = currentY;
                                } else {
                                        wayNamePath[0] = currentX;
                                        wayNamePath[1] = currentY;
                                        wayNamePath[2] = previousX;
                                        wayNamePath[3] = previousY;
                                }
                                wayNames.add(new WayTextContainer(wayNamePath, textKey, fill));
                                if (stroke != null) {
                                        wayNames.add(new WayTextContainer(wayNamePath, textKey, stroke));
                                }

                                skipPixels = DISTANCE_BETWEEN_WAY_NAMES;
                        }

                        // store the previous way point coordinates
                        previousX = currentX;
                        previousY = currentY;
                }
        }

        private WayDecorator() {
                throw new IllegalStateException();
        }
}

