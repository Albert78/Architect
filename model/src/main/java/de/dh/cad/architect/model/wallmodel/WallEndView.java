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

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Wall;

/**
 * Represents a view on a wall end which abstracts from the wall direction.
 */
public class WallEndView {
    protected final Wall mWall;
    protected final WallDockEnd mWallEnd;

    public WallEndView(Wall wall, WallDockEnd wallEnd) {
        mWall = wall;
        mWallEnd = wallEnd;
    }

    public static WallEndView fromWallHandle(Anchor wallHandle) {
        return new WallEndView((Wall) wallHandle.getAnchorOwner(), Wall.getWallEndOfHandle(wallHandle));
    }

    public Wall getWall() {
        return mWall;
    }

    public WallDockEnd getWallEnd() {
        return mWallEnd;
    }

    public WallEndView getOppositeWallEndView() {
        return new WallEndView(mWall, mWallEnd.opposite());
    }

    public Anchor getHandleAnchor() {
        return mWallEnd == WallDockEnd.A
                        ? mWall.getAnchorWallHandleA()
                        : mWall.getAnchorWallHandleB();
    }

    /**
     * Returns the lower corner anchors of the underlaying wall end.
     */
    public Collection<Anchor> getLowerCornerAnchors() {
        return mWallEnd == WallDockEnd.A
                        ? Arrays.asList(mWall.getAnchorWallCornerLA1(), mWall.getAnchorWallCornerLA2())
                        : Arrays.asList(mWall.getAnchorWallCornerLB1(), mWall.getAnchorWallCornerLB2());
    }

    /**
     * Returns the upper corner anchors of the underlaying wall end.
     */
    public Collection<Anchor> getUpperCornerAnchors() {
        return mWallEnd == WallDockEnd.A
                        ? Arrays.asList(mWall.getAnchorWallCornerUA1(), mWall.getAnchorWallCornerUA2())
                        : Arrays.asList(mWall.getAnchorWallCornerUB1(), mWall.getAnchorWallCornerUB2());
    }

    /**
     * Returns all corner anchors of the underlaying wall end.
     */
    public Collection<Anchor> getAllCornerAnchors() {
        Collection<Anchor> result = new ArrayList<>();
        result.addAll(getLowerCornerAnchors());
        result.addAll(getUpperCornerAnchors());
        return result;
    }

    /**
     * Returns all anchors (corners and handle) of the underlaying wall end.
     */
    public Collection<Anchor> getAllAnchors() {
        Collection<Anchor> result = new ArrayList<>();
        result.addAll(getLowerCornerAnchors());
        result.addAll(getUpperCornerAnchors());
        result.add(getHandleAnchor());
        return result;
    }

    /**
     * Returns the lower corner anchor which is located in clockwise direction from the corner anchor of this wall end.
     */
    public Anchor getCornerL_CW() {
        return mWallEnd == WallDockEnd.A
                        ? mWall.getAnchorWallCornerLA1()
                        : mWall.getAnchorWallCornerLB2();
    }

    /**
     * Returns the lower corner anchor which is located in counter-clockwise direction from the corner anchor of this wall end.
     */
    public Anchor getCornerL_CCW() {
        return mWallEnd == WallDockEnd.A
                        ? mWall.getAnchorWallCornerLA2()
                        : mWall.getAnchorWallCornerLB1();
    }

    /**
     * Returns the upper corner anchor which is located in clockwise direction from the corner anchor of this wall end.
     */
    public Anchor getCornerU_CW() {
        return mWallEnd == WallDockEnd.A
                        ? mWall.getAnchorWallCornerUA1()
                        : mWall.getAnchorWallCornerUB2();
    }

    /**
     * Returns the upper corner anchor which is located in counter-clockwise direction from the corner anchor of this wall end.
     */
    public Anchor getCornerU_CCW() {
        return mWallEnd == WallDockEnd.A
                        ? mWall.getAnchorWallCornerUA2()
                        : mWall.getAnchorWallCornerUB1();
    }

    public WallBevelType getWallBevel() {
        return mWallEnd == WallDockEnd.A
                        ? mWall.getWallBevelA()
                        : mWall.getWallBevelB();
    }

    public Length getWallHeight() {
        return mWallEnd == WallDockEnd.A
                        ? mWall.getHeightA()
                        : mWall.getHeightB();
    }
}
