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
package de.dh.utils.fx;

import javafx.scene.paint.Color;

public class LightConfiguration {
    protected final boolean mLightOn;
    protected final Color mLightColor;

    // Only used for PointLight
    protected final double mLightAngleX;
    protected final double mLightAngleZ;
    protected final double mLightDistance;

    /**
     * Constructor for point light.
     */
    public LightConfiguration(boolean isLightOn, Color lightColor, double lightAngleX, double lightAngleZ, double lightDistance) {
        mLightOn = isLightOn;
        mLightColor = lightColor;
        mLightAngleX = lightAngleX;
        mLightAngleZ = lightAngleZ;
        mLightDistance = lightDistance;
    }

    /**
     * Constructor for ambient light.
     */
    public LightConfiguration(boolean isLightOn, Color lightColor) {
        mLightOn = isLightOn;
        mLightColor = lightColor;
        mLightAngleX = 0;
        mLightAngleZ = 0;
        mLightDistance = 0;
    }

    public boolean isLightOn() {
        return mLightOn;
    }

    public Color getLightColor() {
        return mLightColor;
    }

    public double getLightAngleX() {
        return mLightAngleX;
    }

    public double getLightAngleZ() {
        return mLightAngleZ;
    }

    public double getLightDistance() {
        return mLightDistance;
    }
}