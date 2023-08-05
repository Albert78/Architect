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
package de.dh.cad.architect.model.wallmodel;

public class WallOutlineConnection {
    protected WallSurface mSurface;
    protected WallOutlineCorner mPrevious;
    protected WallOutlineCorner mNext;
    protected boolean mNeighborBorder;

    public WallOutlineConnection(WallSurface surface, WallOutlineCorner previous, WallOutlineCorner next, boolean isNeighborBorder) {
        mSurface = surface;
        mPrevious = previous;
        mNext = next;
        mNeighborBorder = isNeighborBorder;
    }

    public WallSurface getSurface() {
        return mSurface;
    }

    public void setSurface(WallSurface value) {
        mSurface = value;
    }

    public WallOutlineCorner getPrevious() {
        return mPrevious;
    }

    public void setPrevious(WallOutlineCorner value) {
        mPrevious = value;
    }

    public WallOutlineCorner getNext() {
        return mNext;
    }

    public void setNext(WallOutlineCorner value) {
        mNext = value;
    }

    public boolean sameSurface(WallOutlineConnection other) {
        return mSurface.equals(other.mSurface);
    }

    public boolean isNeighborBorder() {
        return mNeighborBorder;
    }

    public void setNeighborBorder(boolean value) {
        mNeighborBorder = value;
    }
}