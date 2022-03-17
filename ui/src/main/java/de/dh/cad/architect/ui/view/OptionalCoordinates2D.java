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
package de.dh.cad.architect.ui.view;

import java.util.Optional;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;

public class OptionalCoordinates2D {
    protected final Optional<Length> mOX;
    protected final Optional<Length> mOY;

    public OptionalCoordinates2D(Optional<Length> oX, Optional<Length> oY) {
        mOX = oX;
        mOY = oY;
    }

    public Optional<Length> getOX() {
        return mOX;
    }

    public Optional<Length> getOY() {
        return mOY;
    }

    public Position2D overlay(Position2D pos) {
        return new Position2D(mOX.orElse(pos.getX()), mOY.orElse(pos.getY()));
    }
}