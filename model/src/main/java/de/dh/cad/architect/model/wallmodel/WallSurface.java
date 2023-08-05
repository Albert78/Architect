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
package de.dh.cad.architect.model.wallmodel;

/**
 * Enumeration of six surfaces of a wall, those surfaces correspond with the areas which can be painted on a wall.
 * {@link WallSurface#One} and {@link WallSurface#Two} are the two sides of the wall which correspond with
 * the wall sides 1 and 2, {@link WallSurface#A} and {@link WallSurface#B} are the two scuncheons at wall end A and B.
 */
public enum WallSurface {
    A("Surface-A"),
    B("Surface-B"),
    One("Surface-Side-1"),
    Two("Surface-Side-2"),
    Top("Surface-Top"),
    Bottom("Surface-Bottom"),
    Embrasure("Surface-Embrasures");

    private String mSurfaceType;

    private WallSurface(String type) {
        mSurfaceType = type;
    }

    public String getSurfaceType() {
        return mSurfaceType;
    }

    public static WallSurface ofWallEnd(WallDockEnd wallEnd) {
        switch (wallEnd) {
        case A:
            return A;
        case B:
            return B;
        }
        throw new RuntimeException("No surface available for Wall end " + wallEnd);
    }

    public static WallSurface ofWallSide(int wallSide) {
        switch (wallSide) {
        case 1:
            return One;
        case 2:
            return Two;
        default:
            throw new RuntimeException("Wall side " + wallSide + " doesn't exist");
        }
    }

    public static WallSurface ofWallSurfaceType(String type) {
        for (WallSurface wallSurface : values()) {
            if (wallSurface.getSurfaceType().equals(type)) {
                return wallSurface;
            }
        }
        throw new IllegalArgumentException("No wall surface of type " + type + "");
    }
}