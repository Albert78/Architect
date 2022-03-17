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
package de.dh.cad.architect.model.coords;

public class Vector2D {
    protected static final Vector2D ZERO = new Vector2D(Length.ZERO, Length.ZERO);

    public static final Vector2D X1M = new Vector2D(Length.ofM(1), Length.ZERO);

    protected final Length mX;
    protected final Length mY;

    public Vector2D(Length x, Length y) {
        mX = x;
        mY = y;
    }

    public static Vector2D zero() {
        return ZERO;
    }

    public static Vector2D between(Position2D pos1, Position2D pos2) {
        return pos2.minus(pos1);
    }

    public Length getX() {
        return mX;
    }

    public Length getY() {
        return mY;
    }

    public Vector3D upscale(Length z) {
        return new Vector3D(mX, mY, z);
    }

    public Vector2D negated() {
        return new Vector2D(Length.ofInternalFormat(-mX.inInternalFormat()), Length.ofInternalFormat(-mY.inInternalFormat()));
    }

    public Vector2D plus(Vector2D other) {
        return new Vector2D(mX.plus(other.mX), mY.plus(other.mY));
    }

    public Vector2D minus(Vector2D other) {
        return new Vector2D(mX.minus(other.mX), mY.minus(other.mY));
    }

    public Vector2D times(double factor) {
        return new Vector2D(mX.times(factor), mY.times(factor));
    }

    /**
     * Returns this vector rotated counter-clockwise by the given angle.
     */
    public Vector2D rotate(double angle) {
        double a = (angle * Math.PI) / 180;
        double sin = Math.sin(a);
        double cos = Math.cos(a);
        double xInternal = mX.inInternalFormat();
        double yInternal = mY.inInternalFormat();
        return new Vector2D(
            Length.ofInternalFormat(xInternal * cos - yInternal * sin),
            Length.ofInternalFormat(xInternal * sin + yInternal * cos));
    }

    /**
     * Returns the angle between vector {@code a1} and {@code a2} in degrees from 0-360 degrees.
     */
    public static double angleBetween(Vector2D a1, Vector2D a2) {
        double x1 = a1.getX().inInternalFormat();
        double y1 = a1.getY().inInternalFormat();
        double x2 = a2.getX().inInternalFormat();
        double y2 = a2.getY().inInternalFormat();

        double res = Math.atan2(x1*y2 - y1*x2, x1*x2 + y1*y2) * 180/Math.PI;
        return res < 0 ? 360 + res : res;
    }

    public boolean isParallelTo(Vector2D v, double epsilon) {
        // Parallel => determinant u|v == 0: u.x * v.y - v.x * u.y == 0
        Vector2D tu = toUnitVector(LengthUnit.M);
        Vector2D vu = v.toUnitVector(LengthUnit.M);
        double ux = tu.getX().inInternalFormat();
        double uy = tu.getY().inInternalFormat();
        double vx = vu.getX().inInternalFormat();
        double vy = vu.getY().inInternalFormat();
        return Math.abs(ux * vy - vx * uy) <= epsilon;
    }

    public boolean isOrthogonalTo(Vector2D v, double epsilon) {
        // Orthogonal => u dot v == 0
        return Math.abs(toUnitVector(LengthUnit.M).dotProduct(v, LengthUnit.M)) <= epsilon;
    }

    public Length getLength() {
        double xInternal = mX.inInternalFormat();
        double yInternal = mY.inInternalFormat();
        return Length.ofInternalFormat(Math.sqrt(xInternal * xInternal + yInternal * yInternal));
    }

    public Vector2D scaleToLength(Length length) {
        return toUnitVector(LengthUnit.MM).times(length.inMM());
    }

    public Vector2D scale(double sX, double sY) {
        return new Vector2D(mX.times(sX), mY.times(sY));
    }

    public Vector2D toUnitVector(LengthUnit lengthUnit) {
        double length = getLength().inUnit(lengthUnit);
        return new Vector2D(Length.of(mX.inUnit(lengthUnit) / length, lengthUnit), Length.of(mY.inUnit(lengthUnit) / length, lengthUnit));
    }

    public Vector2D getNormalCW() {
        return new Vector2D(mY, mX.negated());
    }

    public Vector2D getNormalCCW() {
        return new Vector2D(mY.negated(), mX);
    }

    public double dotProduct(Vector2D v, LengthUnit unit) {
        return mX.inUnit(unit) * v.mX.inUnit(unit) + mY.inUnit(unit) * v.mY.inUnit(unit);
    }

    public Vector2D mirrorY() {
        return new Vector2D(mX, Length.ofInternalFormat(-mY.inInternalFormat()));
    }

    public String coordsToString() {
        return mX + "; " + mY;
    }

    public String axesAndCoordsToString() {
        return "X=" + mX + "; Y=" + mY;
    }

    @Override
    public String toString() {
        return "Vector2D [" + axesAndCoordsToString() + "]";
    }
}
