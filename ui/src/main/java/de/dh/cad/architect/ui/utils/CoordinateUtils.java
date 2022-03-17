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
package de.dh.cad.architect.ui.utils;

import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.utils.fx.Vector2D;
import de.dh.utils.fx.Vector3D;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.geometry.Point2D;

public class CoordinateUtils {
    public static final double DELTA = 3;

    public static double lengthToCoords(Length value) {
        return value.inCM();
    }

    public static Length coordsToLength(double value) {
        return Length.ofCM(value);
    }

    public static Point2D positionToPoint2D(IPosition position) {
        return new Point2D(lengthToCoords(position.getX()), lengthToCoords(position.getY()));
    }

    public static Vector2D positionToVector2D(IPosition position) {
        return new Vector2D(lengthToCoords(position.getX()), lengthToCoords(position.getY()));
    }

    public static Vector2D modelVector2DToUiVector2D(de.dh.cad.architect.model.coords.Vector2D v) {
        return new Vector2D(lengthToCoords(v.getX()), lengthToCoords(v.getY()));
    }

    public static Vector2D dimensions2DToUiVector2D(Dimensions2D v) {
        return new Vector2D(lengthToCoords(v.getX()), lengthToCoords(v.getY()));
    }

    public static Position2D coordsToPosition2D(double x, double y) {
        return new Position2D(coordsToLength(x), coordsToLength(y));
    }

    public static Position2D point2DToPosition2D(Point2D point) {
        return new Position2D(coordsToLength(point.getX()), coordsToLength(point.getY()));
    }

    public static Vector2D point2DToUiVector2D(Point2D point) {
        return new Vector2D(point.getX(), point.getY());
    }

    public static de.dh.cad.architect.model.coords.Vector2D point2DToVector2D(Point2D point) {
        return new de.dh.cad.architect.model.coords.Vector2D(coordsToLength(point.getX()), coordsToLength(point.getY()));
    }

    public static Vector3D position3DToVector3D(Position3D position) {
        return new Vector3D(lengthToCoords(position.getX()), lengthToCoords(position.getY()), lengthToCoords(position.getZ()));
    }

    public static Vector3d position2DToVecMathVector3d(IPosition position) {
        return Vector3d.xy(lengthToCoords(position.getX()), lengthToCoords(position.getY()));
    }

    public static Vector3d position3DToVecMathVector3d(Position3D position) {
        return Vector3d.xyz(lengthToCoords(position.getX()), lengthToCoords(position.getY()), lengthToCoords(position.getZ()));
    }

    public static Vector3d vector2DToVecMathVector3d(de.dh.cad.architect.model.coords.Vector2D v) {
        return Vector3d.xy(lengthToCoords(v.getX()), lengthToCoords(v.getY()));
    }

    public static Vector3d vector3DToVecMathVector3d(de.dh.cad.architect.model.coords.Vector3D v) {
        return Vector3d.xyz(lengthToCoords(v.getX()), lengthToCoords(v.getY()), lengthToCoords(v.getZ()));
    }

    public static Vector3D vecMathVectorToVector3D(Vector3d v) {
        return new Vector3D(v.getX(), v.getY(), v.getZ());
    }
}
