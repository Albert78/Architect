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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.Wall;

public class SingleWallBendPoint {
    protected final Anchor mWallDockAnchor1;
    protected final Anchor mWallDockAnchor2;

    public SingleWallBendPoint(Anchor wallDockAnchor1, Anchor wallDockAnchor2) {
        if (!wallDockAnchor1.isHandle() || !wallDockAnchor2.isHandle()) {
            throw new IllegalArgumentException("Anchors of a wall bendpoint must be wall handle anchors");
        }
        mWallDockAnchor1 = wallDockAnchor1;
        mWallDockAnchor2 = wallDockAnchor2;
    }

    public static Optional<SingleWallBendPoint> fromAnchorDock(Anchor anchor) {
        List<Anchor> dockedWallAnchors = new ArrayList<>();
        for (Anchor dockedAnchor : anchor.getAllDockedAnchors()) {
            if (!dockedAnchor.isHandle()) {
                continue;
            }
            BaseAnchoredObject anchorOwner = dockedAnchor.getAnchorOwner();
            if (anchorOwner instanceof Wall) {
                dockedWallAnchors.add(dockedAnchor);
            }
        }
        if (dockedWallAnchors.size() != 2) {
            return Optional.empty();
        }
        return Optional.of(new SingleWallBendPoint(dockedWallAnchors.get(0), dockedWallAnchors.get(1)));
    }

    public Anchor getWallDockAnchor1() {
        return mWallDockAnchor1;
    }

    public Anchor getWallDockAnchor2() {
        return mWallDockAnchor2;
    }

    public WallEndView getWall1DockEnd() {
        return WallEndView.fromWallHandle(mWallDockAnchor1);
    }

    public WallEndView getWall2DockEnd() {
        return WallEndView.fromWallHandle(mWallDockAnchor2);
    }

    public Collection<Anchor> getAnchors() {
        return Arrays.asList(mWallDockAnchor1, mWallDockAnchor2);
    }
}
