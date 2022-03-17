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
package de.dh.cad.architect.model.wallmodel;

import de.dh.cad.architect.model.coords.Length;

/**
 * Virtual image of a model wall which is used to abstract the "real world" for several computation algorithms.
 * {@link IWall} is implemented by {@link AdaptedModelWall}, which mirrors the real world wall and wall connections,
 * but is also implemented by other classes which present a virtual state (like visual feedback when adding a new wall
 * which is not present in model yet).
 */
public interface IWall extends Comparable<IWall> {
    String getId();

    IWallAnchor getAnchorWallHandleA();
    IWallAnchor getAnchorWallHandleB();

    default IWallAnchor getAnchorWallHandle(WallDockEnd dockEnd) {
        if (dockEnd == WallDockEnd.A) {
            return getAnchorWallHandleA();
        } else {
            return getAnchorWallHandleB();
        }
    }

    WallBevelType getWallBevelA();
    void setWallBevelA(WallBevelType value);
    WallBevelType getWallBevelB();
    void setWallBevelB(WallBevelType value);

    default WallBevelType getAnchorWallBevelType(WallDockEnd dockEnd) {
        if (dockEnd == WallDockEnd.A) {
            return getWallBevelA();
        } else {
            return getWallBevelB();
        }
    }

    Length getThickness();

    boolean representsRealWall();

    default boolean hasNeighborWallA() {
        return getAnchorWallHandleA().getAllDockedAnchors().size() > 1;
    }

    default boolean hasNeighborWallB() {
        return getAnchorWallHandleA().getAllDockedAnchors().size() > 1;
    }

    @Override
    default int compareTo(IWall o) {
        return getId().compareTo(o.getId());
    }
}
