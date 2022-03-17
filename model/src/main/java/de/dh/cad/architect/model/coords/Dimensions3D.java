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

/**
 * Defines a 3D volume.
 * In fact, {@link Dimensions3D} and {@link Position3D} are almost identical but {@link Position3D}
 * represents a point in the room while {@link Dimensions3D} represents an extent.
 */
public class Dimensions3D {
    protected static final Dimensions3D EMPTY = new Dimensions3D(Length.ZERO, Length.ZERO, Length.ZERO);

    protected final Length mWidth;
    protected final Length mHeight;
    protected final Length mDepth;

    public Dimensions3D(Length width, Length height, Length depth) {
        mHeight = height;
        mWidth = width;
        mDepth = depth;
    }

    public static Dimensions3D empty() {
        return EMPTY;
    }

    public Length getWidth() {
        return mWidth;
    }

    public Length getHeight() {
        return mHeight;
    }

    public Length getDepth() {
        return mDepth;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mDepth.hashCode();
        result = prime * result + mHeight.hashCode();
        result = prime * result + mWidth.hashCode();
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
        Dimensions3D other = (Dimensions3D) obj;
        if (!mDepth.equals(other.mDepth))
            return false;
        if (!mHeight.equals(other.mHeight))
            return false;
        if (!mWidth.equals(other.mWidth))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Dimensions3D [Width=" + mWidth + "; Height=" + mHeight + "; Depth=" + mDepth + "]";
    }
}
