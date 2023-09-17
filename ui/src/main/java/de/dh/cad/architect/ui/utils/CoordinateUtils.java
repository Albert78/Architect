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
package de.dh.cad.architect.ui.utils;

import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.utils.Vector2D;
import de.dh.utils.Vector3D;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.geometry.Point2D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

public class CoordinateUtils {
    public static final double DELTA = 3;

    // Method is a duplicate of CoordinateSystemConfiguration#createTransformArchitectToJavaFx()
    public static Transform createTransformArchitectToJavaFx() {
        return new Rotate(180, Rotate.X_AXIS);
    }

    public static double lengthToCoords(Length value, Axis axis) {
        return axis == Axis.Y || axis == Axis.Z ? -value.inCM() : value.inCM();
    }

    /**
     * Converts a UI coordinate to a model length.
     * @param axis Whether we convert an X, Y or Z value. This is necessary to know because JavaFX inverts Y and Z.
     * The {@code axis} parameter might also be {@code null}, in this case, the value will be interpreted as an absolute / signless value.
     */
    public static Length coordsToLength(double value, Axis axis) {
        return Length.ofCM(axis == Axis.Y || axis == Axis.Z ? -value : value);
    }

    public static Point2D positionToPoint2D(IPosition position, boolean convertToJavaFX) {
        return new Point2D(lengthToCoords(position.getX(), convertToJavaFX ? Axis.X : null), lengthToCoords(position.getY(), convertToJavaFX ? Axis.Y : null));
    }

    public static Point2D positionToPoint2D(IPosition position) {
        return positionToPoint2D(position, true);
    }

    public static Vector2D point2DToUiVector2D(Point2D point) {
        return new Vector2D(point.getX(), point.getY());
    }

    public static Vector2D toUiVector2D(Length x, Length y, boolean convertToJavaFX) {
        return new Vector2D(lengthToCoords(x, convertToJavaFX ? Axis.X : null), lengthToCoords(y, convertToJavaFX ? Axis.Y : null));
    }

    public static Vector2D positionToVector2D(IPosition position) {
        return positionToVector2D(position, true);
    }

    public static Vector2D positionToVector2D(IPosition position, boolean convertToJavaFX) {
        return toUiVector2D(position.getX(), position.getY(), convertToJavaFX);
    }

    public static Vector2D modelVector2DToUiVector2D(de.dh.cad.architect.model.coords.Vector2D v) {
        return toUiVector2D(v.getX(), v.getY(), true);
    }

    public static Vector2D dimensions2DToUiVector2D(Dimensions2D v) {
        return toUiVector2D(v.getX(), v.getY(), false);
    }

    public static Position2D coordsToPosition2D(double x, double y, boolean convertFromJavaFX) {
        return new Position2D(coordsToLength(x, convertFromJavaFX ? Axis.X : null), coordsToLength(y, convertFromJavaFX ? Axis.Y : null));
    }

    public static Position2D coordsToPosition2D(double x, double y) {
        return coordsToPosition2D(x, y, true);
    }

    public static Position2D point2DToPosition2D(Point2D point, boolean convertFromJavaFX) {
        return coordsToPosition2D(point.getX(), point.getY(), convertFromJavaFX);
    }

    public static Position2D point2DToPosition2D(Point2D point) {
        return point2DToPosition2D(point, true);
    }

    public static de.dh.cad.architect.model.coords.Vector2D coordsToVector2D(double x, double y, boolean convertFromJavaFX) {
        return new de.dh.cad.architect.model.coords.Vector2D(coordsToLength(x, convertFromJavaFX ? Axis.X : null), coordsToLength(y, convertFromJavaFX ? Axis.Y : null));
    }

    public static de.dh.cad.architect.model.coords.Vector2D point2DToVector2D(Point2D point, boolean convertFromJavaFX) {
        return coordsToVector2D(point.getX(), point.getY(), convertFromJavaFX);
    }

    public static de.dh.cad.architect.model.coords.Vector2D point2DToVector2D(Point2D point) {
        return point2DToVector2D(point, true);
    }

    public static Vector3D position3DToVector3D(Position3D position) {
        return position3DToVector3D(position, true);
    }

    public static Vector3D position3DToVector3D(Position3D position, boolean convertToJavaFX) {
        return new Vector3D(lengthToCoords(position.getX(), convertToJavaFX ? Axis.X : null),
            lengthToCoords(position.getY(), convertToJavaFX ? Axis.Y : null),
            lengthToCoords(position.getZ(), convertToJavaFX ? Axis.Z : null));
    }

    public static Vector3d position2DToVecMathVector3d(IPosition position) {
        return position2DToVecMathVector3d(position, true);
    }

    public static Vector3d to2DMathVector3d(Length x, Length y, boolean convertToJavaFX) {
        return Vector3d.xy(lengthToCoords(x, convertToJavaFX ? Axis.X : null), lengthToCoords(y, convertToJavaFX ? Axis.Y : null));
    }

    public static Vector3d position2DToVecMathVector3d(IPosition position, boolean convertToJavaFX) {
        return to2DMathVector3d(position.getX(), position.getY(), convertToJavaFX);
    }

    public static Vector3d to3DMathVector3d(Length x, Length y, Length z, boolean convertToJavaFX) {
        return Vector3d.xyz(lengthToCoords(x, convertToJavaFX ? Axis.X : null),
            lengthToCoords(y, convertToJavaFX ? Axis.Y : null),
            lengthToCoords(z, convertToJavaFX ? Axis.Z : null));
    }

    public static Vector3d position3DToVecMathVector3d(Position3D position, boolean convertToJavaFX) {
        return to3DMathVector3d(position.getX(), position.getY(), position.getZ(), convertToJavaFX);
    }

    public static Vector3d position3DToVecMathVector3d(Position3D position) {
        return position3DToVecMathVector3d(position, true);
    }

    public static Vector3d vector2DToVecMathVector3d(de.dh.cad.architect.model.coords.Vector2D v, boolean convertToJavaFX) {
        return to2DMathVector3d(v.getX(), v.getY(), convertToJavaFX);
    }

    public static Vector3d vector2DToVecMathVector3d(de.dh.cad.architect.model.coords.Vector2D v) {
        return vector2DToVecMathVector3d(v, true);
    }

    public static Vector3d vector3DToVecMathVector3d(de.dh.cad.architect.model.coords.Vector3D v, boolean convertToJavaFX) {
        return to3DMathVector3d(v.getX(), v.getY(), v.getZ(), convertToJavaFX);
    }

    public static Vector3d vector3DToVecMathVector3d(de.dh.cad.architect.model.coords.Vector3D v) {
        return vector3DToVecMathVector3d(v, true);
    }

    public static Vector3D vecMathVectorToVector3D(Vector3d v) {
        return new Vector3D(v.getX(), v.getY(), v.getZ());
    }
}
