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
package de.dh.cad.architect.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;

/**
 * Application configuration, contains application-wide settings. Plan related settings don't go here.
 */
public class Configuration implements IConfig {
    protected static final String KEY_LAST_PLAN_FILE_DIRECTORY = "LastPlanFileDirectory";
    protected static final String KEY_LAST_PLAN_FILE_NAME = "LastPlanFileName";

    protected static final int PLAN_FILE_HISTORY_SIZE = 5;

    protected static final String KEY_PLAN_FILE_HISTORY_PREFIX = "PlanFileHistory_";
    protected static final String KEY_LAST_WINDOW_STATE = "LastWindowState";

    protected final Preferences mPrefs;

    protected Configuration(Preferences prefs) {
        mPrefs = prefs;
    }

    public static Configuration from(Preferences prefs) {
        return new Configuration(prefs);
    }

    public void save() {
        try {
            mPrefs.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException("Error saving preferences", e);
        }
    }

    public Path getLastPlanFilePath() {
        String dirStr = mPrefs.get(KEY_LAST_PLAN_FILE_DIRECTORY, null);
        String nameStr = mPrefs.get(KEY_LAST_PLAN_FILE_NAME, null);
        return StringUtils.isEmpty(dirStr) || StringUtils.isEmpty(nameStr) ? null : Paths.get(dirStr).resolve(nameStr);
    }

    public void setLastPlanFilePath(Path filePath) {
        if (filePath == null) {
            mPrefs.remove(KEY_LAST_PLAN_FILE_NAME);
        } else {
            Path directory = filePath.getParent();
            String fileName = filePath.getFileName().toString();
            mPrefs.put(KEY_LAST_PLAN_FILE_DIRECTORY, directory.toString());
            mPrefs.put(KEY_LAST_PLAN_FILE_NAME, fileName.toString());

            putLastPlanFileToFrontInHistory(filePath);
        }
    }

    public void putLastPlanFileToFrontInHistory(Path filePath) {
        List<Path> history = getPlanFilesHistory();
        while (history.remove(filePath)) { /* Intentionally empty */ }
        history.add(0, filePath);
        writePlanFileHistory(history);
    }

    public Path getLastPlanFileDirectory() {
        String str = mPrefs.get(KEY_LAST_PLAN_FILE_DIRECTORY, null);
        return StringUtils.isEmpty(str) ? null : Paths.get(str);
    }

    public void setLastPlanFileDirectory(Path directoryPath) {
        if (directoryPath == null) {
            mPrefs.remove(KEY_LAST_PLAN_FILE_DIRECTORY);
        } else {
            mPrefs.put(KEY_LAST_PLAN_FILE_DIRECTORY, directoryPath.toString());
        }
    }

    public List<Path> getPlanFilesHistory() {
        List<Path> result = new ArrayList<>();
        for (int i = 0; i < PLAN_FILE_HISTORY_SIZE; i++) {
            String str = mPrefs.get(KEY_PLAN_FILE_HISTORY_PREFIX + i, null);
            if (StringUtils.isEmpty(str)) {
                break;
            }
            Path path = Paths.get(str);
            result.add(path);
        }
        return result;
    }

    public void writePlanFileHistory(List<Path> history) {
        for (int i = 0; i < PLAN_FILE_HISTORY_SIZE; i++) {
            String key = KEY_PLAN_FILE_HISTORY_PREFIX + i;
            if (i < history.size()) {
                mPrefs.put(key, history.get(i).toString());
            } else {
                mPrefs.remove(key);
            }
        }
    }

    public void clearPlanFilesHistory() {
        for (int i = 0; i < PLAN_FILE_HISTORY_SIZE; i++) {
            mPrefs.remove(KEY_PLAN_FILE_HISTORY_PREFIX + i);
        }
    }

    public String getLastWindowState() {
        return mPrefs.get(KEY_LAST_WINDOW_STATE, null);
    }

    public void setLastWindowState(String value) {
        mPrefs.put(KEY_LAST_WINDOW_STATE, value);
    }

    ////////////////////////////////////////////////////////////////// Generic getters/setters ///////////////////////////////////////////////////////

    @Override
    public String getString(String key, String def) {
        return mPrefs.get(key, def);
    }

    @Override
    public void setString(String key, String value) {
        mPrefs.put(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return mPrefs.getBoolean(key, def);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        mPrefs.putBoolean(key, value);
    }

    @Override
    public byte[] getByteArray(String key, byte[] def) {
        return mPrefs.getByteArray(key, def);
    }

    @Override
    public void setByteArray(String key, byte[] value) {
        mPrefs.putByteArray(key, value);
    }

    @Override
    public int getInt(String key, int def) {
        return mPrefs.getInt(key, def);
    }

    @Override
    public void setInt(String key, int value) {
        mPrefs.putInt(key, value);
    }

    @Override
    public long getLong(String key, long def) {
        return mPrefs.getLong(key, def);
    }

    @Override
    public void setLong(String key, long value) {
        mPrefs.putLong(key, value);
    }

    @Override
    public float getFloat(String key, float def) {
        return mPrefs.getFloat(key, def);
    }

    @Override
    public void setFloat(String key, float value) {
        mPrefs.putFloat(key, value);
    }

    @Override
    public double getDouble(String key, double def) {
        return mPrefs.getDouble(key, def);
    }

    @Override
    public void setDouble(String key, double value) {
        mPrefs.putDouble(key, value);
    }

    @Override
    public void remove(String key) {
        mPrefs.remove(key);
    }
}
