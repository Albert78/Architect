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

public class Vector3D {
    public static final Vector3D EMPTY = new Vector3D(Length.ZERO, Length.ZERO, Length.ZERO);

    protected final Length mX;
    protected final Length mY;
    protected final Length mZ;

    public Vector3D(Length x, Length y, Length z) {
        mX = x;
        mY = y;
        mZ = z;
    }

    public static Vector3D empty() {
        return EMPTY;
    }

    public Length getX() {
        return mX;
    }

    public Length getY() {
        return mY;
    }

    public Length getZ() {
        return mZ;
    }

    public Vector3D negated() {
        return new Vector3D(Length.ofInternalFormat(-mX.inInternalFormat()), Length.ofInternalFormat(-mY.inInternalFormat()), Length.ofInternalFormat(-mZ.inInternalFormat()));
    }

    public Vector3D plus(Vector3D other) {
        return new Vector3D(mX.plus(other.mX), mY.plus(other.mY), mZ.plus(other.mZ));
    }

    public Vector3D minus(Vector3D other) {
        return new Vector3D(mX.minus(other.mX), mY.minus(other.mY), mZ.minus(other.mZ));
    }

    public Vector3D times(double factor) {
        return new Vector3D(mX.times(factor), mY.times(factor), mZ.times(factor));
    }

    public Length getLength() {
        double xInternal = mX.inInternalFormat();
        double yInternal = mY.inInternalFormat();
        double zInternal = mZ.inInternalFormat();
        return Length.ofInternalFormat(Math.sqrt(xInternal * xInternal + yInternal * yInternal + zInternal * zInternal));
    }

    public Vector3D toUnitVector(LengthUnit lengthUnit) {
        double length = getLength().inUnit(lengthUnit);
        return new Vector3D(
            Length.of(mX.inUnit(lengthUnit) / length, lengthUnit),
            Length.of(mY.inUnit(lengthUnit) / length, lengthUnit),
            Length.of(mZ.inUnit(lengthUnit) / length, lengthUnit));
    }

    public Length dotProduct(Vector3D v) {
        return Length.ofInternalFormat(mX.inInternalFormat() * v.mX.inInternalFormat() + mY.inInternalFormat() * v.mY.inInternalFormat() + mZ.inInternalFormat() * v.mZ.inInternalFormat());
    }

    public Vector3D crossProduct(Vector3D other) {
        return new Vector3D(
            Length.ofInternalFormat(mY.inInternalFormat() * other.mZ.inInternalFormat() - mZ.inInternalFormat() * other.mY.inInternalFormat()),
            Length.ofInternalFormat(mZ.inInternalFormat() * other.mX.inInternalFormat() - mX.inInternalFormat() * other.mZ.inInternalFormat()),
            Length.ofInternalFormat(mX.inInternalFormat() * other.mY.inInternalFormat() - mY.inInternalFormat() * other.mX.inInternalFormat()));
    }

    @Override
    public String toString() {
        return "Vector3D [X=" + mX + ", Y=" + mY + ", Z=" + mZ + "]";
    }
}
