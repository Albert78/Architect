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

import javax.xml.bind.annotation.XmlType;

/**
 * Defines a point in 3D space.
 */
@XmlType(name="Position3D")
public class Position3D implements IPosition {
    protected static final Position3D EMPTY = new Position3D(Length.ZERO, Length.ZERO, Length.ZERO);

    protected final Length mX;
    protected final Length mY;
    protected final Length mZ;

    public Position3D(Length x, Length y, Length z) {
        mX = x;
        mY = y;
        mZ = z;
    }

    public static Position3D zero() {
        return EMPTY;
    }

    public static Position3D fromVector(Vector3D v) {
        return new Position3D(v.getX(), v.getY(), v.getZ());
    }

    @Override
    public Length getX() {
        return mX;
    }

    @Override
    public Length getY() {
        return mY;
    }

    public Length getZ() {
        return mZ;
    }

    @Override
    public Position3D withX(Length x) {
        return new Position3D(x, mY, mZ);
    }

    public Position3D movedX(Length x) {
        return withX(mX.plus(x));
    }

    @Override
    public Position3D withY(Length y) {
        return new Position3D(mX, y, mZ);
    }

    public Position3D movedY(Length y) {
        return withY(mY.plus(y));
    }

    @Override
    public Position3D withXY(Position2D xy) {
        return new Position3D(xy.getX(), xy.getY(), mZ);
    }

    @Override
    public Position3D withZ(Length z) {
        return new Position3D(mX, mY, z);
    }

    public Position3D movedZ(Length z) {
        return withZ(mZ.plus(z));
    }

    public Position3D moved(Length x, Length y, Length z) {
        return new Position3D(mX.plus(x), mY.plus(y), mZ.plus(z));
    }

    public Position3D withXY(Length x, Length y) {
        return new Position3D(x, y, mZ);
    }

    public Length distance(Position3D other) {
        return distance(this, other);
    }

    public static Length distance(Position3D first, Position3D second) {
        double x1 = first.getX().inMM();
        double y1 = first.getY().inMM();
        double z1 = first.getZ().inMM();
        double x2 = second.getX().inMM();
        double y2 = second.getY().inMM();
        double z2 = second.getZ().inMM();
        return Length.ofMM(Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1)));
    }

    @Override
    public Position2D projectionXY() {
        return new Position2D(mX, mY);
    }

    public Vector3D toVector() {
        return new Vector3D(mX, mY, mZ);
    }

    public Position3D plus(Vector3D v) {
        return new Position3D(mX.plus(v.getX()), mY.plus(v.getY()), mZ.plus(v.getZ()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mX.hashCode();
        result = prime * result + mY.hashCode();
        result = prime * result + mZ.hashCode();
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
        Position3D other = (Position3D) obj;
        if (!mX.equals(other.mX))
            return false;
        if (!mY.equals(other.mY))
            return false;
        if (!mZ.equals(other.mZ))
            return false;
        return true;
    }

    @Override
    public String coordsToString() {
        return mX + "; " + mY + "; " + mZ;
    }

    @Override
    public String axesAndCoordsToString() {
        return "X=" + mX + "; Y=" + mY + "; Z=" + mZ;
    }

    @Override
    public String toString() {
        return "Position3D [" + axesAndCoordsToString() + "]";
    }
}
