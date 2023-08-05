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
 * Defines a 2D extend, i.e. a width and a height (or X and Y extend), without a start location.
 * In fact, {@link Dimensions2D} and {@link Position2D} are almost identical but {@link Position2D}
 * represents a point in a plane while {@link Dimensions2D} represents an extend.
 */
public class Dimensions2D {
    protected static final Dimensions2D EMPTY = new Dimensions2D(Length.ZERO, Length.ZERO);

    protected final Length mX;
    protected final Length mY;

    public Dimensions2D(Length x, Length y) {
        mX = x;
        mY = y;
    }

    public static Dimensions2D empty() {
        return EMPTY;
    }

    public Length getX() {
        return mX;
    }

    public Length getY() {
        return mY;
    }

    public Dimensions2D withX(Length value) {
        return new Dimensions2D(value, mY);
    }

    public Dimensions2D withY(Length value) {
        return new Dimensions2D(mX, value);
    }

    public Dimensions2D plus(Length x, Length y) {
        return new Dimensions2D(mX.plus(x), mY.plus(y));
    }

    public Dimensions2D scale(double factor) {
        return new Dimensions2D(mX.times(factor), mY.times(factor));
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
        Dimensions2D other = (Dimensions2D) obj;
        if (!mX.equals(other.mX))
            return false;
        if (!mY.equals(other.mY))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Dimensions2D [X = " + mX + "; Y = " + mY + "]";
    }
}
