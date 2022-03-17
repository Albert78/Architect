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

import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;

/**
 * Part of the abstraction model for connected walls.
 * @see {@link IWall}.
 */
public interface IWallAnchor extends Comparable<IWallAnchor> {
    String getId();
    IWall getOwner();
    Position2D getPosition();

    /**
     * Gets the information whether this anchor counts as a reference for feedback calculation
     * for the current moving point. A reference point is relevant for snapping horizontally or vertically
     * to that point.
     */
    boolean isReferenceAnchor();

    /**
     * Returns all anchors which are docked to this anchor inclusive this anchor itself.
     * The list is in the dock order of the anchors.
     */
    List<? extends IWallAnchor> getAllDockedAnchors();

    default boolean isDocked() {
        return !getAllDockedAnchors().isEmpty();
    }

    Optional<WallDockEnd> getHandleAnchorDockEnd();

    @Override
    default int compareTo(IWallAnchor o) {
        return getId().compareTo(o.getId());
    }
}
