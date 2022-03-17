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

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.utils.fx.Vector3D;
import eu.mihosoft.vvecmath.Transform;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.geometry.Point2D;

public class MathUtils {
    public static final double EPSILON_DOUBLE_EQUALITY = 0.1;

    public static Vector3d findOrthogonalVector(Vector3d v) {
        double vx = v.getX();
        double vy = v.getY();
        double vz = v.getZ();
        double ax = Math.abs(vx);
        double ay = Math.abs(vy);
        double az = Math.abs(vz);
        if (ax > ay && ax > az) {
            // Calculate intersection point at Y + 1
            return calculatePlaneLineIntersectionPoint(Vector3d.ZERO, v, Vector3d.xyz(v.getX(), v.getY() + 1, v.getZ()), v);
        } else {
            // Calculate intersection point at X + 1
            return calculatePlaneLineIntersectionPoint(Vector3d.ZERO, v, Vector3d.xyz(v.getX() + 1, v.getY(), v.getZ()), v);
        }
    }

    public static class RotationData {
        protected final double mAngle;
        protected final Vector3d mAxis;

        public RotationData(double angle, Vector3d axis) {
            mAngle = angle;
            mAxis = axis;
        }

        public double getAngle() {
            return mAngle;
        }

        public Vector3d getAxis() {
            return mAxis;
        }
    }

    public static Transform rot(Transform t, Vector3d from, Vector3d to) {
        RotationData rotationData = calculateRotation(from, to);
        Vector3d axis = rotationData.getAxis();
        double angle = rotationData.getAngle();
        return angle == 0 ? t : t.rot(Vector3d.ZERO, axis, angle);
    }

    public static RotationData calculateRotation(Vector3d from, Vector3d to) {
        Vector3d a = from.normalized();
        Vector3d b = to.normalized();
        Vector3d c = a.crossed(b);

        double l = c.magnitude(); // sine of angle

        if (l > 1e-9) {
            Vector3d axis = c.normalized();
            double angle = a.angle(b);
            if (a.dot(b) < 0) {
                // Wrong direction, rotate by 180 degrees
                angle += 180;
            }

            return new RotationData(angle, axis);
        } else {
            // Vectors linearly dependent
            if (from.dot(to) < 0) {
                // Wrong direction, rotate by 180 degrees
                // We need to find a rotation vector in the plane which is normal to a
                // Try to find vector with a big angle to a
                Vector3d axis = MathUtils.findOrthogonalVector(a);
                return new RotationData(180, axis);
            }
        }

        return new RotationData(0, Vector3d.Z_ONE);
    }

    /**
     * Finds the intersection point of two lines in 2D.
     */
    public static Point2D calculateLinesIntersectionPoint(Point2D l1p1, Point2D l1p2, Point2D l2p1, Point2D l2p2) {
        return new Point2D(
            ((l2p2.getX() - l2p1.getX()) * (l1p2.getX()*l1p1.getY() - l1p1.getX()*l1p2.getY()) - (l1p2.getX() - l1p1.getX()) * (l2p2.getX()*l2p1.getY() - l2p1.getX()*l2p2.getY())) /
            ((l2p2.getY() - l2p1.getY()) * (l1p2.getX() - l1p1.getX()) - (l1p2.getY() - l1p1.getY()) * (l2p2.getX() - l2p1.getX())),

            ((l1p1.getY() - l1p2.getY()) * (l2p2.getX()*l2p1.getY() - l2p1.getX()*l2p2.getY()) - (l2p1.getY() - l2p2.getY())*(l1p2.getX()*l1p1.getY() - l1p1.getX()*l1p2.getY())) /
            ((l2p2.getY() - l2p1.getY())* (l1p2.getX() - l1p1.getX()) - (l1p2.getY() - l1p1.getY())*(l2p2.getX() - l2p1.getX())));
    }

    // The following code is from https://www.geeksforgeeks.org/how-to-check-if-a-given-point-lies-inside-a-polygon/

    // Define Infinite (Using INT_MAX caused overflow problems)
    static int INF = 10000;

    /**
     * Given three colinear points p, q, r, this function checks if point q lies on line segment 'pr'
     */
    public static boolean onSegment(Point2D p, Point2D q, Point2D r) {
        return q.getX() <= Math.max(p.getX(), r.getX())
                && q.getX() >= Math.min(p.getX(), r.getX())
                && q.getY() <= Math.max(p.getY(), r.getY())
                && q.getY() >= Math.min(p.getY(), r.getY());
    }

    /**
     * Finds the orientation of ordered triplet (p, q, r).
     * @return 0 --> p, q and r are colinear, 1 --> Clockwise, 2 --> Counterclockwise
     */
    public static int orientation(Point2D p, Point2D q, Point2D r) {
        double px = p.getX();
        double py = p.getY();
        double qx = q.getX();
        double qy = q.getY();
        double rx = r.getX();
        double ry = r.getY();
        double val = (qy - py) * (rx - qx) - (qx - px) * (ry - qy);

        if (val == 0) {
            return 0; // colinear
        }
        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }

    /**
     * Checks if the given line segments p1q1 and p2q2 intersect.
     */
    public static boolean doIntersect(Point2D p1, Point2D q1, Point2D p2, Point2D q2) {
        // Find the four orientations needed for
        // general and special cases
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        // General case
        if (o1 != o2 && o3 != o4) {
            return true;
        }

        // Special Cases
        // p1, q1 and p2 are colinear and
        // p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1, p2, q1)) {
            return true;
        }

        // p1, q1 and p2 are colinear and
        // q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1, q2, q1)) {
            return true;
        }

        // p2, q2 and p1 are colinear and
        // p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2, p1, q2)) {
            return true;
        }

        // p2, q2 and q1 are colinear and
        // q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2, q1, q2)) {
            return true;
        }

        // Doesn't fall in any of the above cases
        return false;
    }

    /**
     * Checks if the given point lies inside the given polygon.
     */
    public static boolean isInside(Position2D polygon[], Position2D p) {
        Point2D _polygon[] = new Point2D[polygon.length];
        for (int i = 0; i < polygon.length; i++) {
            _polygon[i] = CoordinateUtils.positionToPoint2D(polygon[i]);
        }
        Point2D _p = CoordinateUtils.positionToPoint2D(p);

        int n = polygon.length;
        // There must be at least 3 vertices in polygon[]
        if (n < 3) {
            return false;
        }

        // Create a point for line segment from p to infinite
        Point2D extreme = new Point2D(INF, CoordinateUtils.lengthToCoords(p.getY()));

        // Count intersections of the above line
        // with sides of polygon
        int count = 0, i = 0;
        do {
            int next = (i + 1) % n;

            // Check if the line segment from 'p' to
            // 'extreme' intersects with the line
            // segment from 'polygon[i]' to 'polygon[next]'
            if (doIntersect(_polygon[i], _polygon[next], _p, extreme)) {
                // If the point 'p' is colinear with line
                // segment 'i-next', then check if it lies
                // on segment. If it lies, return true, otherwise false
                if (orientation(_polygon[i], _p, _polygon[next]) == 0) {
                    return onSegment(_polygon[i], _p, _polygon[next]);
                }

                count++;
            }
            i = next;
        } while (i != 0);

        // Return true if count is odd, false otherwise
        return (count % 2 == 1);
    }

    // Code from https://www.mathematik-oberstufe.de/vektoren/a/abstand-punkt-gerade-formel.html
    public static double calculatePointLineDistance(Vector3D x, Vector3D linePoint, Vector3D lineDirection) {
        Vector3D lineDirectionU = lineDirection.toUnitVector();
        return x.minus(linePoint).crossProduct(lineDirectionU).getLength();
    }

    public static double calculatePointLineDistance(Vector3d x, Vector3d linePoint, Vector3d lineDirection) {
        Vector3d lineDirectionU = lineDirection.normalized();
        return x.minus(linePoint).crossed(lineDirectionU).magnitude();
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
    public static Vector3D calculatePlaneLineIntersectionPoint(Vector3D planePoint, Vector3D planeNormal, Vector3D linePoint, Vector3D lineDirection) {
        Vector3D ldu = lineDirection.toUnitVector();
        if (planeNormal.dotProduct(ldu) == 0) {
            return null;
        }

        double t = (planeNormal.dotProduct(planePoint) - planeNormal.dotProduct(linePoint)) / planeNormal.dotProduct(ldu);
        return linePoint.plus(ldu.times(t));
    }

    /**
     * Same as {@link #calculatePlaneLineIntersectionPoint(Vector3D, Vector3D, Vector3D, Vector3D)} but for {@link Vector3d}.
     */
    public static Vector3d calculatePlaneLineIntersectionPoint(Vector3d planePoint, Vector3d planeNormal, Vector3d linePoint, Vector3d lineDirection) {
        Vector3d ldu = lineDirection.normalized();
        if (planeNormal.dot(ldu) == 0) {
            return null;
        }

        double t = (planeNormal.dot(planePoint) - planeNormal.dot(linePoint)) / planeNormal.dot(ldu);
        return linePoint.plus(ldu.times(t));
    }

    public static double min4(double v1, double v2, double v3, double v4) {
        return Math.min(Math.min(v1, v2), Math.min(v3, v4));
    }

    public static double max4(double v1, double v2, double v3, double v4) {
        return Math.max(Math.max(v1, v2), Math.max(v3, v4));
    }

    /**
     * Compares two doubles for equality, evaluating the values to equal if their absolute difference is smaller than {@code epsilon}.
     * Use {@link #EPSILON_DOUBLE_EQUALITY} as standard value.
     */
    public static boolean almostEqual(double v1, double v2, double epsilon) {
        return Math.abs(v2 - v1) < epsilon;
    }
}
