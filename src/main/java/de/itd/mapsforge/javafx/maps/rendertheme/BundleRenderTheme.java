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

package de.itd.mapsforge.javafx.maps.rendertheme;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.mapsforge.map.rendertheme.XmlRenderTheme;

public class BundleRenderTheme implements XmlRenderTheme {

	public static final String PATH_SEPARATOR	= "/";
	
	private static final long serialVersionUID = 463968434846500198L;
	private InputStream inputStream;
	private String pathPrefix;

	public BundleRenderTheme(String pathUnderResources) {
		pathPrefix	= new File(pathUnderResources).getParent();
		inputStream = getClass().getClassLoader().getResourceAsStream(pathUnderResources);
		
		if (!pathPrefix.endsWith(PATH_SEPARATOR)) {
			pathPrefix += PATH_SEPARATOR;
		}
		
		if (!pathPrefix.startsWith(PATH_SEPARATOR)) {
			pathPrefix = PATH_SEPARATOR + pathPrefix;
		}
	}

	@Override
	public String getRelativePathPrefix() {
		return pathPrefix;
	}

	@Override
	public InputStream getRenderThemeAsStream() throws FileNotFoundException {
		return inputStream;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((inputStream == null) ? 0 : inputStream.hashCode());
		result = prime * result
				+ ((pathPrefix == null) ? 0 : pathPrefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BundleRenderTheme other = (BundleRenderTheme) obj;
		if (inputStream == null) {
			if (other.inputStream != null)
				return false;
		} else if (!inputStream.equals(other.inputStream))
			return false;
		if (pathPrefix == null) {
			if (other.pathPrefix != null)
				return false;
		} else if (!pathPrefix.equals(other.pathPrefix))
			return false;
		return true;
	}

}
