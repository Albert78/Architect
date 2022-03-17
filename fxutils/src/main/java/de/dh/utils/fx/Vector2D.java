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
package de.dh.utils.fx;

public class Vector2D {
    public static final Vector2D EMPTY = new Vector2D(0, 0);
    public static final Vector2D X_ONE = new Vector2D(1, 0);
    public static final Vector2D Y_ONE = new Vector2D(0, 1);

    protected final double mX;
    protected final double mY;

    public Vector2D(double x, double y) {
        mX = x;
        mY = y;
    }

    public static Vector2D empty() {
        return EMPTY;
    }

    public static Vector2D center(Vector2D v1, Vector2D v2) {
        return v2.plus(v1).times(0.5);
    }

    public static Vector2D between(Vector2D v1, Vector2D v2) {
        return v2.minus(v1);
    }

    public double getX() {
        return mX;
    }

    public double getY() {
        return mY;
    }

    public Vector2D negated() {
        return new Vector2D(-mX, -mY);
    }

    public Vector2D negatedX() {
        return new Vector2D(-mX, mY);
    }

    public Vector2D negatedY() {
        return new Vector2D(mX, -mY);
    }

    public Vector2D plus(Vector2D other) {
        return new Vector2D(mX + other.mX, mY + other.mY);
    }

    public Vector2D plus(double x, double y) {
        return new Vector2D(mX + x, mY + y);
    }

    public Vector2D minus(Vector2D other) {
        return new Vector2D(mX - other.mX, mY - other.mY);
    }

    public Vector2D minus(double x, double y) {
        return new Vector2D(mX - x, mY - y);
    }

    public Vector2D times(double factor) {
        return new Vector2D(mX * factor, mY * factor);
    }

    public Vector2D rotate(double angle) {
        double a = (angle * Math.PI) / 180;
        double sin = Math.sin(a);
        double cos = Math.cos(a);
        return new Vector2D(
            mX * cos - mY * sin,
            mX * sin + mY * cos);
    }

    public double getLength() {
        return Math.sqrt(mX * mX + mY * mY);
    }

    /**
     * Returns the angle between vector {@code a1} and {@code a2} in degrees from 0-360 degrees.
     */
    public static double angleBetween(Vector2D a1, Vector2D a2) {
        double x1 = a1.getX();
        double y1 = a1.getY();
        double x2 = a2.getX();
        double y2 = a2.getY();

        double res = Math.atan2(x1*y2-y1*x2,x1*x2+y1*y2) * 180/Math.PI;
        return res < 0 ? 360 + res : res;
    }

    public boolean isParallelTo(Vector2D v) {
        return mX * v.mY == v.mX * mY;
    }

    public Vector2D toUnitVector() {
        double length = getLength();
        return new Vector2D(mX / length, mY / length);
    }

    public Vector2D scaleToLength(double length) {
        return toUnitVector().times(length);
    }

    public Vector2D getNormalCW() {
        return new Vector2D(mY, -mX);
    }

    public Vector2D getNormalCCW() {
        return new Vector2D(-mY, mX);
    }

    public double dotProduct(Vector2D v) {
        return mX * v.mX + mY * v.mY;
    }

    @Override
    public String toString() {
        return "Vector2D [X=" + mX + ", Y=" + mY + "]";
    }
}
