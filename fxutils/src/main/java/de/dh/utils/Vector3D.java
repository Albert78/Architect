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
package de.dh.utils;

public class Vector3D {
    public static final Vector3D ZERO = new Vector3D(0, 0, 0);
    public static final Vector3D X1 = new Vector3D(1, 0, 0);
    public static final Vector3D Y1 = new Vector3D(0, 1, 0);
    public static final Vector3D Z1 = new Vector3D(0, 0, 1);

    protected final double mX;
    protected final double mY;
    protected final double mZ;

    public Vector3D(double x, double y, double z) {
        mX = x;
        mY = y;
        mZ = z;
    }

    public static Vector3D empty() {
        return ZERO;
    }

    public double getX() {
        return mX;
    }

    public double getY() {
        return mY;
    }

    public double getZ() {
        return mZ;
    }

    public Vector3D negated() {
        return new Vector3D(-mX, -mY, -mZ);
    }

    public Vector3D negatedX() {
        return new Vector3D(-mX, mY, mZ);
    }

    public Vector3D negatedY() {
        return new Vector3D(mX, -mY, mZ);
    }

    public Vector3D negatedZ() {
        return new Vector3D(mX, mY, -mZ);
    }

    public Vector3D plus(Vector3D other) {
        return new Vector3D(mX + other.mX, mY + other.mY, mZ + other.mZ);
    }

    public Vector3D minus(Vector3D other) {
        return new Vector3D(mX - other.mX, mY - other.mY, mZ - other.mZ);
    }

    public Vector3D times(double factor) {
        return new Vector3D(mX * factor, mY * factor, mZ * factor);
    }

    public double getLength() {
        return Math.sqrt(mX * mX + mY * mY + mZ * mZ);
    }

    public Vector3D toUnitVector() {
        double length = getLength();
        return new Vector3D(mX / length, mY / length, mZ / length);
    }

    public double dotProduct(Vector3D v) {
        return mX * v.mX + mY * v.mY + mZ * v.mZ;
    }

    public Vector3D crossProduct(Vector3D other) {
        return new Vector3D(
            mY*other.mZ - mZ*other.mY,
            mZ*other.mX - mX*other.mZ,
            mX*other.mY - mY*other.mX);
    }

    @Override
    public String toString() {
        return "Vector3D [X=" + mX + ", Y=" + mY + ", Z=" + mZ + "]";
    }
}
