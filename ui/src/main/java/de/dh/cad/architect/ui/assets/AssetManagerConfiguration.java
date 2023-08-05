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
package de.dh.cad.architect.ui.assets;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;

/**
 * Application configuration, contains application-wide settings. Plan related settings don't go here.
 */
public class AssetManagerConfiguration {
    protected static final String KEY_NUM_OPEN_ASSET_LIBRARIES = "NumOpenAssetLibraries";
    protected static final String KEY_ASSET_LIBRARY_PREFIX = "AssetLibrary";
    protected static final String KEY_LAST_ASSET_LIBRARY_PATH = "LastAssetLibraryPath";
    protected static final String KEY_LAST_3D_RESOURCE_PATH = "Last3DResourcePath";
    protected static final String KEY_LAST_MATERIAL_PATH = "LastMaterialPath";
    protected static final String KEY_LAST_IMAGE_PATH = "LastImagePath";

    protected static final String KEY_LAST_IMPORTED_LIBRARY_PATH = "LastImportedLibraryPath";
    protected static final String KEY_LAST_CHOOSEN_EXTERNAL_LIBRARY_PATH = "LastChoosenExternalLibraryPath";

    protected final Preferences mPrefs;

    protected AssetManagerConfiguration(Preferences prefs) {
        mPrefs = prefs;
    }

    public static AssetManagerConfiguration from(Preferences prefs) {
        return new AssetManagerConfiguration(prefs);
    }

    public void save() {
        try {
            mPrefs.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException("Error saving preferences", e);
        }
    }

    public Collection<Path> getOpenAssetLibraries() {
        int numOpenAssetLibraries = mPrefs.getInt(KEY_NUM_OPEN_ASSET_LIBRARIES, 0);
        Collection<Path> result = new ArrayList<>();
        for (int i = 0; i < numOpenAssetLibraries; i++) {
            String path = mPrefs.get(KEY_ASSET_LIBRARY_PREFIX + i, null);
            if (!StringUtils.isEmpty(path)) {
                result.add(Paths.get(path));
            }
        }
        return result;
    }

    public void setOpenAssetLibraries(Collection<Path> libraryPaths) {
        mPrefs.putInt(KEY_NUM_OPEN_ASSET_LIBRARIES, libraryPaths.size());
        int i = 0;
        for (Path path : libraryPaths) {
            mPrefs.put(KEY_ASSET_LIBRARY_PREFIX + i, path.toString());
            i++;
        }
    }

    protected Optional<Path> readOptionalPathStr(String key) {
        String pathStr = mPrefs.get(key, null);
        if (StringUtils.isEmpty(pathStr)) {
            return Optional.empty();
        }
        return Optional.of(Paths.get(pathStr));
    }

    protected void setPathEntry(String key, Path path) {
        mPrefs.put(key, path.toString());
    }

    public Optional<Path> getLastAssetLibraryPath() {
        return readOptionalPathStr(KEY_LAST_ASSET_LIBRARY_PATH);
    }

    public void setLastAssetLibraryPath(Path value) {
        setPathEntry(KEY_LAST_ASSET_LIBRARY_PATH, value);
    }

    public Optional<Path> getLastMaterialPath() {
        return readOptionalPathStr(KEY_LAST_MATERIAL_PATH);
    }

    public void setLastMaterialPath(Path value) {
        setPathEntry(KEY_LAST_MATERIAL_PATH, value);
    }

    public Optional<Path> getLastImagePath() {
        return readOptionalPathStr(KEY_LAST_IMAGE_PATH);
    }

    public void setLastImagePath(Path value) {
        setPathEntry(KEY_LAST_IMAGE_PATH, value);
    }

    public Optional<Path> getLast3DResourcePath() {
        return readOptionalPathStr(KEY_LAST_3D_RESOURCE_PATH);
    }

    public void setLast3DResourcePath(Path value) {
        setPathEntry(KEY_LAST_3D_RESOURCE_PATH, value);
    }

    /**
     * To be used by library importer modules.
     */
    public Optional<Path> getLastImportedLibraryPath() {
        return readOptionalPathStr(KEY_LAST_IMPORTED_LIBRARY_PATH);
    }

    public void setLastImportedLibraryPath(Path value) {
        setPathEntry(KEY_LAST_IMPORTED_LIBRARY_PATH, value);
    }

    public Optional<Path> getLastChoosenExternalLbraryPath() {
        return readOptionalPathStr(KEY_LAST_CHOOSEN_EXTERNAL_LIBRARY_PATH);
    }

    public void setLastChoosenExternalLibraryPath(Path value) {
        setPathEntry(KEY_LAST_CHOOSEN_EXTERNAL_LIBRARY_PATH, value);
    }
}
