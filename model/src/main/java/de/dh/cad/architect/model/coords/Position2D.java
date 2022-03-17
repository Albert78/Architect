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

import javax.xml.bind.annotation.XmlType;

/**
 * Defines a point in 2D space.
 */
@XmlType(name="Position2D")
public class Position2D implements IPosition {
    protected static final Position2D EMPTY = new Position2D(Length.ZERO, Length.ZERO);

    protected final Length mX;
    protected final Length mY;

    public Position2D(Length x, Length y) {
        mX = x;
        mY = y;
    }

    public static Position2D zero() {
        return EMPTY;
    }

    public static Position2D pointBetween(Position2D pos1, Position2D pos2, double ratio) {
        return pos1.plus(pos2.minus(pos1).times(ratio));
    }

    public static Position2D centerOf(Position2D pos1, Position2D pos2) {
        return pointBetween(pos1, pos2, 0.5);
    }

    @Override
    public Length getX() {
        return mX;
    }

    @Override
    public Length getY() {
        return mY;
    }

    public Position2D withX(Length x) {
        return new Position2D(x, mY);
    }

    public Position2D movedX(Length x) {
        return withX(mX.plus(x));
    }

    public Position2D withY(Length y) {
        return new Position2D(mX, y);
    }

    public Position2D movedY(Length y) {
        return withY(mY.plus(y));
    }

    @Override
    public Position2D withXY(Position2D xy) {
        return xy;
    }

    public Length distance(Position2D other) {
        return distance(this, other);
    }

    public Position2D scale(double sX, double sY, Position2D pivot) {
        return pivot.plus(
            this.minus(pivot).scale(sX, sY));
    }

    public static Length distance(Position2D first, Position2D second) {
        double x1 = first.getX().inMM();
        double y1 = first.getY().inMM();
        double x2 = second.getX().inMM();
        double y2 = second.getY().inMM();
        return Length.ofMM(Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
    }

    public Length distanceX(Position2D other) {
        return mX.minus(other.mX);
    }

    public Length distanceY(Position2D other) {
        return mY.minus(other.mY);
    }

    public Vector2D minus(Position2D other) {
        return new Vector2D(mX.minus(other.getX()), mY.minus(other.getY()));
    }

    public Position2D minus(Vector2D other) {
        return new Position2D(mX.minus(other.getX()), mY.minus(other.getY()));
    }

    public Position2D plus(Vector2D v) {
        return new Position2D(mX.plus(v.getX()), mY.plus(v.getY()));
    }

    public Position3D upscale() {
        return new Position3D(mX, mY, Length.ZERO);
    }

    public Position3D upscale(Length z) {
        return new Position3D(mX, mY, z);
    }

    public Position2D rotateAround(double angleDeg, Position2D pivotPosition) {
        return pivotPosition.plus(
                        this.minus(pivotPosition)
                        .rotate(angleDeg));
    }

    @Override
    public Position2D projectionXY() {
        return this;
    }

    @Override
    public Position3D withZ(Length height) {
        return upscale(height);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mX.hashCode();
        result = prime * result + mY.hashCode();
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
        Position2D other = (Position2D) obj;
        if (!mX.equals(other.mX))
            return false;
        if (!mY.equals(other.mY))
            return false;
        return true;
    }

    @Override
    public String coordsToString() {
        return mX + "; " + mY;
    }

    @Override
    public String axesAndCoordsToString() {
        return "X=" + mX + "; Y=" + mY;
    }

    @Override
    public String toString() {
        return "Position2D [" + axesAndCoordsToString() + "]";
    }
}
