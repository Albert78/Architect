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

public interface IPosition {
    String coordsToString();
    String axesAndCoordsToString();
    Position2D projectionXY();
    Length getX();
    Length getY();
    IPosition withX(Length x);
    IPosition withY(Length Y);
    Position3D withZ(Length height);
    IPosition withXY(Position2D xy);
    IPosition movedXY(Vector2D delta);
}
