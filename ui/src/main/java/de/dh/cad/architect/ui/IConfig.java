/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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

/**
 * Generic configuration interface.
 */
public interface IConfig {
    String getString(String key, String def);
    void setString(String key, String value);
    boolean getBoolean(String key, boolean def);
    void setBoolean(String key, boolean value);
    byte[] getByteArray(String key, byte[] def);
    void setByteArray(String key, byte[] value);
    int getInt(String key, int def);
    void setInt(String key, int value);
    long getLong(String key, long def);
    void setLong(String key, long value);
    float getFloat(String key, float def);
    void setFloat(String key, float value);
    double getDouble(String key, double def);
    void setDouble(String key, double value);
    void remove(String key);
}
