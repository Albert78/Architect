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
package de.dh.cad.architect.libraryeditor;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Application configuration, contains application-wide settings. Plan related settings don't go here.
 */
public class LibraryEditorConfiguration {
    protected static final String KEY_LAST_WINDOW_STATE = "LastWindowState";
    protected static final String KEY_SHOW_COORDINATE_SYSTEM = "ShowCoordinateSystem";
    protected static final String KEY_AUTOCOMPILE = "AutoCompile";

    protected final Preferences mPrefs;

    protected LibraryEditorConfiguration(Preferences prefs) {
        mPrefs = prefs;
    }

    public static LibraryEditorConfiguration from(Preferences prefs) {
        return new LibraryEditorConfiguration(prefs);
    }

    public void save() {
        try {
            mPrefs.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException("Error saving preferences", e);
        }
    }

    public String getLastWindowState() {
        return mPrefs.get(KEY_LAST_WINDOW_STATE, null);
    }

    public void setLastWindowState(String value) {
        mPrefs.put(KEY_LAST_WINDOW_STATE, value);
    }

    public boolean isShowCoordinateSystem() {
        return mPrefs.getBoolean(KEY_SHOW_COORDINATE_SYSTEM, true);
    }

    public void setShowCoordinateSystem(boolean value) {
        mPrefs.putBoolean(KEY_SHOW_COORDINATE_SYSTEM, value);
    }

    public boolean isAutoCompile() {
        return mPrefs.getBoolean(KEY_AUTOCOMPILE, true);
    }

    public void setAutoCompile(boolean value) {
        mPrefs.putBoolean(KEY_AUTOCOMPILE, value);
    }
}
