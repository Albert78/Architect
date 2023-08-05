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
package de.dh.cad.architect.model.coords;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Defines a 2D area, this is a start position and an end position (or start position plus X/Y extend).
 */
public class Area2D {
    protected final Length mX1;
    protected final Length mY1;
    protected final Length mX2;
    protected final Length mY2;

    public Area2D(Length x1, Length y1, Length x2, Length y2) {
        mX1 = x1;
        mY1 = y1;
        mX2 = x2;
        mY2 = y2;
    }

    public static Area2D create(Position2D from, Position2D to) {
        return new Area2D(from.getX(), from.getY(), to.getX(), to.getY());
    }

    public static Area2D ofPoint(Position2D position) {
        Length x = position.getX();
        Length y = position.getY();
        return new Area2D(x, y, x, y);
    }

    public static Area2D createBoundingBox(Collection<Position2D> positions) {
        Length minX = null;
        Length minY = null;
        Length maxX = null;
        Length maxY = null;
        for (Position2D pos : positions) {
            Length x = pos.getX();
            Length y = pos.getY();
            if (minX == null || x.lt(minX)) {
                minX = x;
            }
            if (minY == null || y.lt(minY)) {
                minY = y;
            }
            if (maxX == null || x.gt(maxX)) {
                maxX = x;
            }
            if (maxY == null || y.gt(maxY)) {
                maxY = y;
            }
        }
        return new Area2D(minX, minY, maxX, maxY);
    }

    public static Area2D union(Collection<Area2D> areas) {
        Collection<Position2D> positions = new ArrayList<>(areas.size() * 2);
        for (Area2D part : areas) {
            positions.add(part.getStartPoint());
            positions.add(part.getEndPoint());
        }
        return createBoundingBox(positions);
    }

    public Length getX1() {
        return mX1;
    }

    public Length getY1() {
        return mY1;
    }

    public Length getX2() {
        return mX2;
    }

    public Length getY2() {
        return mY2;
    }

    public Position2D getStartPoint() {
        return new Position2D(mX1, mY1);
    }

    public Position2D getEndPoint() {
        return new Position2D(mX2, mY2);
    }

    /**
     * Gets the extend of this area in X direction (or width).
     */
    public Length getExtendX() {
        return mX2.minus(mX1);
    }

    /**
     * Gets the extend of this area in Y direction (or height).
     */
    public Length getExtendY() {
        return mY2.minus(mY1);
    }

    public boolean contains(Position2D point) {
        Length px = point.getX();
        Length py = point.getY();
        return px.gt(mX1) && px.lt(mX2) // Inside range X
                && py.gt(mY1) && py.lt(mY2); // Inside range y
    }

    public boolean hasIntersection(Area2D other) {
        return other.getX1().lt(mX2) && other.getX2().gt(mX1) // Intersection X
                && other.getY1().lt(mY2) && other.getY2().gt(mY1); // Intersection y
    }

    public Optional<Area2D> intersection(Area2D other) {
        if (!hasIntersection(other)) {
            return Optional.empty();
        }
        return Optional.of(new Area2D(Length.max(other.getX1(), mX1), Length.max(other.getY1(), mY1),
            Length.min(other.getX2(), mX2), Length.min(other.getY2(), mY2)));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mX1.hashCode();
        result = prime * result + mY1.hashCode();
        result = prime * result + mX2.hashCode();
        result = prime * result + mY2.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Area2D other = (Area2D) obj;
        if (!mX1.equals(other.mX1))
            return false;
        if (!mY1.equals(other.mY1))
            return false;
        if (!mX2.equals(other.mX2))
            return false;
        if (!mY2.equals(other.mY2))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Area2D [X1 = " + mX1 + "; Y1 = " + mY1 + "; X2 = " + mX2 + "; Y2 = " + mY2 + "]";
    }
}
