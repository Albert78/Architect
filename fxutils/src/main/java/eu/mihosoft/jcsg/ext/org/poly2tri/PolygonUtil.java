/**
 * PolygonUtil.java
 *
 * Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Michael Hoffer
 * <info@michaelhoffer.de>.
 */
package eu.mihosoft.jcsg.ext.org.poly2tri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.mihosoft.jcsg.Edge;
import eu.mihosoft.jcsg.Polygon;
import eu.mihosoft.jcsg.Vertex;
import eu.mihosoft.vvecmath.Vector3d;

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class PolygonUtil {

    private PolygonUtil() {
        throw new AssertionError("Don't instantiate me!", null);
    }

    /**
     * Converts a CSG polygon to a poly2tri polygon (including holes)
     * @param polygon the polygon to convert
     * @return a CSG polygon to a poly2tri polygon (including holes)
     */
    private static eu.mihosoft.jcsg.ext.org.poly2tri.Polygon fromCSGPolygon(
            eu.mihosoft.jcsg.Polygon polygon) {

        // convert polygon
        List< PolygonPoint> points = new ArrayList<>();
        for (Vertex v : polygon.vertices) {
            PolygonPoint vp = new PolygonPoint(v.pos.x(), v.pos.y(), v.pos.z());
            points.add(vp);
        }

        eu.mihosoft.jcsg.ext.org.poly2tri.Polygon result
                = new eu.mihosoft.jcsg.ext.org.poly2tri.Polygon(points);

        // convert holes
        Optional<List<Polygon>> holesOfPresult
                = polygon.
                getStorage().getValue(Edge.KEY_POLYGON_HOLES);
        if (holesOfPresult.isPresent()) {
            List<Polygon> holesOfP = holesOfPresult.get();

            holesOfP.stream().forEach((hP) -> {
                result.addHole(fromCSGPolygon(hP));
            });
        }

        return result;
    }

    // Only X and Y coordinates are taken into account
    public static int orientation(Vector3d p, Vector3d q, Vector3d r) {
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

    public static List<Vector3d> cleanupPolygonPoints(List<Vector3d> points) {
        List<Vector3d> result = new ArrayList<>(points);
        if (result.size() < 3) {
            throw new IllegalArgumentException("Polygon must have at least 3 points");
        }
        int i = 0;
        while (i < result.size()) {
            int i1 = i;
            int i2 = (i + 1) % result.size();
            int i3 = (i + 2) % result.size();
            Vector3d p1 = result.get(i1);
            Vector3d p2 = result.get(i2);
            Vector3d p3 = result.get(i3);
            if (orientation(p1, p2, p3) == 0) {
                result.remove(i2);
                if (result.size() < 3) {
                    throw new IllegalArgumentException("Illegal polygon, all points are colinear");
                }
            } else {
                i++;
            }
        }
        return result;
    }

    // Only X and Y coordinates are taken into account
    // TODO: Check if the Z coordinate doesn't matter or if it must be the same for all input points.
    //       Check what happens with the Z coordinate of the result
    public static List<Polygon> concaveToConvex(Polygon concave) {
        List<Polygon> result = new ArrayList<>();

        Vector3d normal = concave.vertices.get(0).normal.clone();

        boolean cw = !concave.isCCW();

        eu.mihosoft.jcsg.ext.org.poly2tri.Polygon p = fromCSGPolygon(concave);

        Poly2Tri.triangulate(p);

        List<DelaunayTriangle> triangles = p.getTriangles();

        List<Vertex> triPoints = new ArrayList<>();

        for (DelaunayTriangle t : triangles) {
            int counter = 0;
            for (TriangulationPoint tp : t.points) {
                triPoints.add(new Vertex(
                        Vector3d.xyz(tp.getX(), tp.getY(), tp.getZ()),
                        normal));
                if (counter == 2) {
                    if (!cw) {
                        Collections.reverse(triPoints);
                    }
                    Polygon poly =
                            new Polygon(triPoints, concave.getStorage());
                    result.add(poly);
                    counter = 0;
                    triPoints = new ArrayList<>();
                } else {
                    counter++;
                }
            }
        }

        return result;
    }

    /**
     * Returns the information if the direction of the polygon defined by the given 2D (!!!) points is counter-clockwise.
     * Be aware that this function only takes the X and Y coordinates of the given points into account.
     */
    public static boolean isCCW_XY(List<Vector3d> points) {
        double sum = 0;
        int numPoints = points.size();
        for (int i = 0; i < numPoints; i++) {
            Vector3d pointI = points.get(i);
            Vector3d pointI1 = points.get((i + 1) % numPoints);
            sum += (pointI1.getX() - pointI.getX()) * (pointI1.getY() + pointI.getY());
        }
        return sum < 0;
    }
}
