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

import java.util.ArrayList;
import java.util.List;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.MathUtils;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;

public class WallOutline {
    protected WallOutlineCorner mFirstCorner;
    protected WallOutlineCorner mLastCorner;
    protected int mNumCorners;
    protected Position2D mWallEndAPosition = null;
    protected Position2D mWallEndBPosition = null;

    public WallOutline(Position2D start) {
        mFirstCorner = new WallOutlineCorner(start, null, null);
        mLastCorner = mFirstCorner;
        mNumCorners = 1;
    }

    public boolean isClosed() {
        return mLastCorner == null;
    }

    public int getNumCorners() {
        return mNumCorners;
    }

    public void addCorner(WallSurface connectionSurface, boolean isNeighborBorder, Position2D targetCorner) {
        WallOutlineConnection connection = new WallOutlineConnection(connectionSurface, mLastCorner, null, isNeighborBorder);
        mLastCorner.setNext(connection);
        mLastCorner = new WallOutlineCorner(targetCorner, connection, null);
        connection.setNext(mLastCorner);
        mNumCorners++;
    }

    public WallOutlineCorner getFirstCorner() {
        return mFirstCorner;
    }

    public WallOutlineCorner getLastCorner() {
        return mLastCorner;
    }

    public Position2D getWallEndAPosition() {
        return mWallEndAPosition;
    }

    public Position2D getWallEndBPosition() {
        return mWallEndBPosition;
    }

    public List<WallOutlineCorner> getCornersAsList() {
        List<WallOutlineCorner> result = new ArrayList<>();
        WallOutlineCorner current = mFirstCorner;
        do {
            result.add(current);
            current = current.getNext().getNext();
        } while (current != mFirstCorner);
        return result;
    }

    public Length calculateOverhang(Position2D referenceCorner, Vector2D direction) {
        Length maxOverhang = Length.ZERO;
        for (WallOutlineCorner corner : getCornersAsList()) {
            Length overhang = Length.ofInternalFormat(corner.getPosition().minus(referenceCorner).dotProduct(direction, LengthUnit.DEFAULT));
            if (overhang.gt(maxOverhang)) {
                maxOverhang = overhang;
            }
        }
        return maxOverhang;
    }

    /**
     * Gets the length this wall extends over wall end A.
     */
    public Length calculateOverhangEndA() {
        Position2D handleA = mWallEndAPosition;
        Position2D handleB = mWallEndBPosition;
        Vector2D vBA = handleA.minus(handleB);
        return calculateOverhang(handleA, vBA);
    }

    /**
     * Gets the length this wall extends over wall end B.
     */
    public Length calculateOverhangEndB() {
        Position2D handleA = mWallEndAPosition;
        Position2D handleB = mWallEndBPosition;
        Vector2D vAB = handleB.minus(handleA);
        return calculateOverhang(handleB, vAB);
    }

    /**
     * Gets the length this wall extends over wall side 1.
     */
    public Length calculateOverhangSide1() {
        Position2D handleA = mWallEndAPosition;
        Position2D handleB = mWallEndBPosition;
        Vector2D vba = handleA.minus(handleB);
        Vector2D v21 = vba.getNormalCW();
        Position2D a1 = handleA.plus(v21.times(0.5));
        return calculateOverhang(a1, v21);
    }

    /**
     * Gets the length this wall extends over wall side 2.
     */
    public Length calculateOverhangSide2() {
        Position2D handleA = mWallEndAPosition;
        Position2D handleB = mWallEndBPosition;
        Vector2D vba = handleA.minus(handleB);
        Vector2D v12 = vba.getNormalCCW();
        Position2D a2 = handleA.plus(v12.times(0.5));
        return calculateOverhang(a2, v12);
    }

    public void closeOutline(WallSurface connectionSurface, boolean isNeighborBorder) {
        if (isClosed()) {
            throw new RuntimeException("Wall outline is already closed");
        }
        WallOutlineConnection connection = new WallOutlineConnection(connectionSurface, mLastCorner, mFirstCorner, isNeighborBorder);
        mLastCorner.setNext(connection);
        mFirstCorner.setPrevious(connection);
        mLastCorner = null;
    }

    // Must be closed when calling this because we don't touch mLastCorner for simplicity
    private void removeCorner(WallOutlineCorner corner) {
        WallOutlineCorner previousCorner = corner.getPrevious().getPrevious();
        WallOutlineConnection nextConnection = corner.getNext();
        previousCorner.setNext(nextConnection);
        nextConnection.setPrevious(previousCorner);
        if (corner == mFirstCorner) {
            mFirstCorner = previousCorner;
        }
    }

    public void removeColinearSegments() {
        if (!isClosed()) {
            // It would be possible to implement the cleanup mechanism on non-closed wall outlines but
            // that is more complicated because we would always have to handle the case when we come to the last corner.
            // We only need this algorithm for closed wall outlines, so we just reject calls when not closed.
            throw new RuntimeException("Wall outline must be closed before we can cleanup");
        }
        if (mNumCorners < 4) {
            throw new RuntimeException("Wall outline polygon must have at least four corners");
        }
        WallOutlineCorner current = mFirstCorner;
        do {
            // Remove coincident points
            if (MathUtils.isAlmostEqual(current.getPosition(), current.getNext().getNext().getPosition())) {
                removeCorner(current.getNext().getNext());
            } else {
                // Remove colinear segments
                WallOutlineConnection connection1 = current.getNext();
                WallOutlineCorner corner1 = connection1.getNext();
                WallOutlineConnection connection2 = corner1.getNext();
                WallOutlineCorner corner2 = connection2.getNext();
                if (connection1.sameSurface(connection2) && MathUtils.isColinear(
                    current.getPosition(),
                    corner1.getPosition(),
                    corner2.getPosition())) {
                    removeCorner(corner1);
                } else {
                    current = current.getNext().getNext();
                }
            }
            if (mNumCorners < 4) {
                throw new RuntimeException("Invalid wall outline polygon");
            }
        } while (current != null && current != mFirstCorner);
    }

    public void markWallEnd(WallDockEnd wallEnd, Position2D position) {
        if (wallEnd == WallDockEnd.A) {
            mWallEndAPosition = position;
        } else {
            mWallEndBPosition = position;
        }
    }

    public void markLastCornerWallEnd(WallDockEnd wallEnd) {
        if (wallEnd == WallDockEnd.A) {
            mWallEndAPosition = mLastCorner.getPosition();
        } else {
            mWallEndBPosition = mLastCorner.getPosition();
        }
    }

    /**
     * Returns the wall's base polygon points as {@link Position2D} objects.
     */
    public List<Position2D> calculateAllGroundPoints() {
        List<Position2D> result = new ArrayList<>();
        WallOutlineCorner current = mFirstCorner;
        do {
            result.add(current.getPosition());
            current = current.getNext().getNext();
        } while (current != mFirstCorner);
        return result;
    }

    public List<WallOutlineConnection> getConnections(WallSurface surface) {
        List<WallOutlineConnection> result = new ArrayList<>();
        WallOutlineConnection current = mFirstCorner.mNext;
        while (true) {
            if (current != null && current.getSurface().equals(surface)) {
                result.add(current);
            }
            WallOutlineCorner nextCorner = current == null ? null : current.getNext();
            if (nextCorner == mFirstCorner) {
                break;
            }
            current = nextCorner == null ? null : nextCorner.getNext();
        }
        return result;
    }
}