/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021 - 2023  Daniel Höh
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 *******************************************************************************/
package de.dh.cad.architect.libraryimporter.sh3d;

import java.util.MissingResourceException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class AbstractCatalog {
    protected static String removeTrailingSlashFromPath(String optionalString) {
        if (StringUtils.isEmpty(optionalString)) {
            return optionalString;
        }
        while (optionalString.startsWith("/") || optionalString.startsWith("\\")) {
            optionalString = optionalString.substring(1);
        }
        return optionalString;
      }

    /**
       * Returns the value of <code>propertyKey</code> in <code>resource</code>,
       * or <code>defaultValue</code> if the property doesn't exist.
       */
    protected static String getOptionalString(Properties resource, String propertyKey, String defaultValue) {
        try {
            String value = resource.getProperty(propertyKey);
            if (StringUtils.isEmpty(value)) {
                return defaultValue;
            }
            return value;
        } catch (MissingResourceException ex) {
            return defaultValue;
        }
    }

    /**
       * Returns the value of <code>propertyKey</code> in <code>resource</code>,
       * or <code>defaultValue</code> if the property doesn't exist.
       */
    protected static float getOptionalFloat(Properties resource, String propertyKey, float defaultValue) {
        try {
            String value = resource.getProperty(propertyKey);
            if (StringUtils.isEmpty(value)) {
                return defaultValue;
            }
            return Float.parseFloat(value);
        } catch (MissingResourceException ex) {
            return defaultValue;
        }
    }

    /**
       * Returns the boolean value of <code>propertyKey</code> in <code>resource</code>,
       * or <code>defaultValue</code> if the property doesn't exist.
       */
    protected static boolean getOptionalBoolean(Properties resource, String propertyKey, boolean defaultValue) {
        try {
            String value = resource.getProperty(propertyKey);
            if (StringUtils.isEmpty(value)) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value);
        } catch (MissingResourceException ex) {
            return defaultValue;
        }
    }
}
