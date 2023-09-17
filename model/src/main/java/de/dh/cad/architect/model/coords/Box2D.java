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

/**
 * Defines a 2D box, in an arbitrary coordinate system. The important point is that we store all three corners separately to be able to express rotated points.
 *
 */
public class Box2D {
    protected static final Box2D EMPTY = new Box2D(Position2D.EMPTY, Position2D.EMPTY, Position2D.EMPTY, Position2D.EMPTY);

    protected final Position2D mX1Y1;
    protected final Position2D mX2Y1;
    protected final Position2D mX1Y2;
    protected final Position2D mX2Y2;

    public Box2D(Position2D x1y1, Position2D x2y1, Position2D x1y2, Position2D x2y2) {
        mX1Y1 = x1y1;
        mX2Y1  = x2y1;
        mX1Y2 = x1y2;
        mX2Y2 = x2y2;
    }

    public static Box2D empty() {
        return EMPTY;
    }

    public Position2D getX1Y1() {
        return mX1Y1;
    }

    public Position2D getX2Y1() {
        return mX2Y1;
    }

    public Position2D getX1Y2() {
        return mX1Y2;
    }

    public Position2D getX2Y2() {
        return mX2Y2;
    }

    /**
     * Returns a copy of this box, scaled according to 0/0.
     */
    public Box2D scale(double factor) {
        return new Box2D(
            mX1Y1.toVector2D().times(factor).toPosition2D(),
            mX2Y1.toVector2D().times(factor).toPosition2D(),
            mX1Y2.toVector2D().times(factor).toPosition2D(),
            mX2Y2.toVector2D().times(factor).toPosition2D());
    }

    /**
     * Returns a copy of this box, scaled according to a pivot point.
     */
    public Box2D scale(double factor, Position2D pivot) {
        return new Box2D(
            mX1Y1.minus(pivot).times(factor).addedTo(pivot),
            mX2Y1.minus(pivot).times(factor).addedTo(pivot),
            mX1Y2.minus(pivot).times(factor).addedTo(pivot),
            mX2Y2.minus(pivot).times(factor).addedTo(pivot));
    }

    /**
     * Returns a copy of this box whose corners are rotated counter-clockwise by <code>angleDeg</code> around 0/0.
     */
    public Box2D rotate(double angleDeg) {
        return new Box2D(
            mX1Y1.rotate(angleDeg),
            mX2Y1.rotate(angleDeg),
            mX1Y2.rotate(angleDeg),
            mX2Y2.rotate(angleDeg));
    }

    /**
     * Returns a copy of this box whose corners are rotated counter-clockwise by <code>angleDeg</code> around a pivot point.
     */
    public Box2D rotateAround(double angleDeg, Position2D pivot) {
        return new Box2D(
            mX1Y1.rotateAround(angleDeg, pivot),
            mX2Y1.rotateAround(angleDeg, pivot),
            mX1Y2.rotateAround(angleDeg, pivot),
            mX2Y2.rotateAround(angleDeg, pivot));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mX1Y1.hashCode();
        result = prime * result + mX1Y2.hashCode();
        result = prime * result + mX2Y1.hashCode();
        result = prime * result + mX2Y2.hashCode();
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
        Box2D other = (Box2D) obj;
        if (!mX1Y1.equals(other.mX1Y1))
            return false;
        if (!mX1Y2.equals(other.mX1Y2))
            return false;
        if (!mX2Y1.equals(other.mX2Y1))
            return false;
        if (!mX2Y2.equals(other.mX2Y2))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Box2D [X1Y1 = " + mX1Y1 + "; X1Y2 = " + mX1Y2 + "; X2Y1 = " + mX2Y1 + "; X2Y2 = " + mX2Y2 + "]";
    }
}
