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

import java.util.List;
import java.util.Optional;

public class MathUtils {
    public static final Length EPSILON_L = Length.ofMM(0.1);

    public static boolean isAlmostEqual(Position2D p1, Position2D p2) {
        return isAlmostEqual(p1, p2, EPSILON_L);
    }

    public static boolean isAlmostEqual(Position2D p1, Position2D p2, Length maxDistance) {
        return p1.getX().difference(p2.getX()).le(maxDistance) && p1.getY().difference(p2.getY()).le(maxDistance);
    }

    // Code from https://stackoverflow.com/questions/5666222/3d-line-plane-intersection
    /**
     * Determines the point of intersection between a plane defined by a point and a normal vector and a line defined by a point and a direction vector.
     * @param planePoint A point on the plane.
     * @param planeNormal The normal vector of the plane.
     * @param linePoint A point on the line.
     * @param lineDirection The direction vector of the line.
     * @return The point of intersection between the line and the plane, null if the line is parallel to the plane.
     */
    public static Optional<Position3D> calculatePlaneLineIntersectionPoint(Position3D planePoint, Vector3D planeNormal, Position3D linePoint, Vector3D lineDirection) {
        Vector3D ldu = lineDirection.toUnitVector(LengthUnit.MM);
        if (planeNormal.dotProduct(ldu).inInternalFormat() == 0) {
            return Optional.empty();
        }

        double t = planeNormal.dotProduct(planePoint.toVector()).minus(planeNormal.dotProduct(linePoint.toVector())).divideBy(planeNormal.dotProduct(ldu));
        return Optional.of(linePoint.plus(ldu.times(t)));
    }

    /**
     * Finds the intersection point of two lines in 2D.
     */
    public static Optional<Position2D> calculateLinesIntersectionPoint(Position2D l1p1, Position2D l1p2, Position2D l2p1, Position2D l2p2) {
        double l1p1x = l1p1.getX().inMM();
        double l1p1y = l1p1.getY().inMM();
        double l1p2x = l1p2.getX().inMM();
        double l1p2y = l1p2.getY().inMM();
        double l2p1x = l2p1.getX().inMM();
        double l2p1y = l2p1.getY().inMM();
        double l2p2x = l2p2.getX().inMM();
        double l2p2y = l2p2.getY().inMM();
        double v1x = l1p2x - l1p1x;
        double v1y = l1p2y - l1p1y;
        double v2x = l2p2x - l2p1x;
        double v2y = l2p2y - l2p1y;
        Position2D res = new Position2D(
            Length.ofMM(
                (v2x * (l1p2x*l1p1y - l1p1x*l1p2y) - v1x * (l2p2x*l2p1y - l2p1x*l2p2y)) /
                (v2y * v1x - v1y * v2x)),

            Length.ofMM(
                (-v1y * (l2p2x*l2p1y - l2p1x*l2p2y) + v2y * (l1p2x*l1p1y - l1p1x*l1p2y)) /
                (v2y * v1x - v1y * v2x)));
        return res.getX().isValid() && res.getY().isValid() ? Optional.of(res) : Optional.empty();
    }

    /**
     * Finds the orientation of ordered triplet (p, q, r).
     * @return 0 --> p, q and r are colinear, 1 --> Clockwise, 2 --> Counterclockwise
     */
    public static int orientation(Position2D p, Position2D q, Position2D r) {
        double px = p.getX().inInternalFormat();
        double py = p.getY().inInternalFormat();
        double qx = q.getX().inInternalFormat();
        double qy = q.getY().inInternalFormat();
        double rx = r.getX().inInternalFormat();
        double ry = r.getY().inInternalFormat();
        double val = (qy - py) * (rx - qx) - (qx - px) * (ry - qy);

        if (val == 0) {
            return 0; // colinear
        }
        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }

    public static boolean isColinear(Position2D p, Position2D q, Position2D r) {
        return orientation(p, q, r) == 0;
    }

    public static Position2D calculateCentroid(List<Position2D> edgePositions) {
        int n = edgePositions.size();
        if (n == 0) {
            return Position2D.zero();
        }
        Position2D p1 = edgePositions.get(0);
        if (n == 1) {
            return p1;
        }
        if (n == 2) {
            Position2D p2 = edgePositions.get(1);
            return Position2D.centerOf(p1, p2);
        }
        // n >= 3
        double a = 0; // Area sum
        double xs = 0; // Centoid x
        double ys = 0; // Centoid y
        for (int i = 0; i < n; i++) {
            Position2D posI = edgePositions.get(i);
            Position2D posI1 = edgePositions.get((i + 1) % n);
            double xi = posI.getX().inCM();
            double yi = posI.getY().inCM();
            double xi1 = posI1.getX().inCM();
            double yi1 = posI1.getY().inCM();
            a += xi * yi1 - xi1 * yi;

            xs += (xi + xi1) * (xi * yi1 - xi1 * yi);
            ys += (yi + yi1) * (xi * yi1 - xi1 * yi);
        }
        return new Position2D(Length.ofCM(xs / (3 * a)), Length.ofCM(ys / (3 * a)));
    }
}
