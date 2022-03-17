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
package de.dh.cad.architect.model.wallmodel;

import de.dh.cad.architect.model.coords.Position2D;

public class WallOutlineCorner {
    protected Position2D mPosition;
    protected WallOutlineConnection mPrevious;
    protected WallOutlineConnection mNext;

    public WallOutlineCorner(Position2D position, WallOutlineConnection previous, WallOutlineConnection next) {
        mPosition = position;
        mPrevious = previous;
        mNext = next;
    }

    public Position2D getPosition() {
        return mPosition;
    }

    public void setPosition(Position2D value) {
        mPosition = value;
    }

    public WallOutlineConnection getPrevious() {
        return mPrevious;
    }

    public void setPrevious(WallOutlineConnection value) {
        mPrevious = value;
    }

    public WallOutlineConnection getNext() {
        return mNext;
    }

    public void setNext(WallOutlineConnection value) {
        mNext = value;
    }
}