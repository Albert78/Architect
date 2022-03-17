/**
 * Extrude.java
 *
 * Copyright 2014-2017 Michael Hoffer <info@michaelhoffer.de>. All rights
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
package eu.mihosoft.jcsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vvecmath.Transform;
import eu.mihosoft.vvecmath.Vector3d;

/**
 * Extrudes concave and convex polygons.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class Extrude {
    private Extrude() {
        throw new AssertionError("Don't instantiate me!", null);
    }

    /**
     * Extrudes the specified path (convex or concave polygon without holes or
     * intersections) into the specified direction.
     *
     * @param dir Direction to extrude.
     * @param points Path (convex or concave polygon without holes or
     * intersections) which is used as top and bottom polygon.
     *
     * @return A CSG object that consists of the extruded polygon.
     */
    public static CSG points(Vector3d dir, Vector3d... points) {
        return extrude(dir, toCCWPolygon(Arrays.asList(points)));
    }

    /**
     * Extrudes the specified path (convex or concave polygon without holes or
     * intersections) into the specified direction.
     *
     * @param dir Direction to extrude.
     * @param points Path (convex or concave polygon without holes or
     * intersections) which is used as top and bottom polygon.
     *
     * @return A CSG object that consists of the extruded polygon.
     */
    public static CSG points(Vector3d dir, List<Vector3d> points) {
        return extrude(dir, toCCWPolygon(points));
    }

    /**
     * Extrudes the specified path (convex or concave polygon without holes or
     * intersections) into the specified direction.
     *
     * @param dir Direction to extrude.
     * @param points Path (convex or concave polygon without holes or
     * intersections) which is used as top and bottom polygon.
     *
     * @return A list containing the extruded polygon.
     */
    public static List<Polygon> points(Vector3d dir, boolean top, boolean bottom, Vector3d... points) {
        return extrude(dir, toCCWPolygon(Arrays.asList(points)), top, bottom);
    }

    /**
     * Extrudes the specified path (convex or concave polygon without holes or
     * intersections) into the specified direction.
     *
     * @param dir Direction to extrude.
     * @param points Path (convex or concave polygon without holes or
     * intersections) which is used as top and bottom polygon.
     *
     * @return A list containing the extruded polygon.
     */
    public static List<Polygon> points(Vector3d dir, boolean top, boolean bottom, List<Vector3d> points) {
        return extrude(dir, toCCWPolygon(points), top, bottom);
    }

    /**
     * Combines two polygons into one CSG object. Polygons p1 and p2 are treated as top and
     * bottom of a tube segment with p1 and p2 as the profile. <b>Note:</b> both polygons must have the
     * same number of vertices. This method does not guarantee intersection-free CSGs. It is in the
     * responsibility of the caller to ensure that the orientation of p1 and p2 allow for
     * intersection-free combination of both.
     *
     * @param p1 First polygon.
     * @param p2 Second polygon.
     * @return List of polygons.
     */
    public static CSG combine(Polygon p1, Polygon p2) {
        return CSG.fromPolygons(combine(p1, p2, true, true));
    }

    /**
     * Combines two polygons into one CSG object. Polygons p1 and p2 are treated as top and
     * bottom of a tube segment with p1 and p2 as the profile. <b>Note:</b> both polygons must have the
     * same number of vertices. This method does not guarantee intersection-free CSGs. It is in the
     * responsibility of the caller to ensure that the orientation of p1 and p2 allow for
     * intersection-free combination of both.
     *
     * @param p1 First polygon.
     * @param p2 Second polygon.
     * @param bottom Defines whether to close the bottom of the tube.
     * @param top Defines whether to close the top of the tube.
     * @return List of polygons.
     */
    public static List<Polygon> combine(Polygon p1, Polygon p2, boolean bottom, boolean top) {
        List<Polygon> newPolygons = new ArrayList<>();

        if (p1.vertices.size() != p2.vertices.size()) {
            throw new RuntimeException("Polygons must have the same number of vertices");
        }

        int numVertices = p1.vertices.size();

        if (bottom) {
            newPolygons.add(p1.flipped());
        }

        for (int i = 0; i < numVertices; i++) {

            int nexti = (i + 1) % numVertices;

            Vector3d bottomV1 = p1.vertices.get(i).pos;
            Vector3d topV1 = p2.vertices.get(i).pos;
            Vector3d bottomV2 = p1.vertices.get(nexti).pos;
            Vector3d topV2 = p2.vertices.get(nexti).pos;

            List<Vector3d> pPoints;

            pPoints = Arrays.asList(bottomV2, topV2, topV1);
            newPolygons.add(Polygon.fromPoints(pPoints, p1.getStorage()));
            pPoints = Arrays.asList(bottomV2,  topV1, bottomV1);
            newPolygons.add(Polygon.fromPoints(pPoints, p1.getStorage()));
        }

        if (top) {
            newPolygons.add(p2);
        }

        return newPolygons;
    }

    private static CSG extrude(Vector3d dir, Polygon polygon1) {
        List<Polygon> newPolygons = new ArrayList<>();

        if (dir.z()<0) {
            throw new IllegalArgumentException("z < 0 currently not supported for extrude: " + dir);
        }

        List<Polygon> p1convex = PolygonUtil.concaveToConvex(polygon1);
        newPolygons.addAll(p1convex);
        Polygon polygon2 = polygon1.translated(dir);

        int numvertices = polygon1.vertices.size();
        for (int i = 0; i < numvertices; i++) {

            int nexti = (i + 1) % numvertices;

            Vector3d bottomV1 = polygon1.vertices.get(i).pos;
            Vector3d topV1 = polygon2.vertices.get(i).pos;
            Vector3d bottomV2 = polygon1.vertices.get(nexti).pos;
            Vector3d topV2 = polygon2.vertices.get(nexti).pos;

            List<Vector3d> pPoints = Arrays.asList(bottomV2, topV2, topV1, bottomV1);

            newPolygons.add(Polygon.fromPoints(pPoints, polygon1.getStorage()));
        }

        // Calculate top polygons
        for (Polygon polygon : p1convex) {
            newPolygons.add(polygon.flipped().translated(dir));
        }

        return CSG.fromPolygons(newPolygons);
    }

    private static List<Polygon> extrude(Vector3d dir, Polygon polygon1, boolean top, boolean bottom) {
        List<Polygon> newPolygons = new ArrayList<>();

        if (bottom) {
            newPolygons.addAll(PolygonUtil.concaveToConvex(polygon1));
        }

        Polygon polygon2 = polygon1.translated(dir);

        Transform rot = Transform.unity();

        Vector3d a = polygon2.getPlane().getNormal().normalized();
        Vector3d b = dir.normalized();

        Vector3d c = a.crossed(b);

        double l = c.magnitude(); // sine of angle

        if (l > 1e-9) {

            Vector3d axis = c.times(1.0 / l);
            double angle = a.angle(b);

            double sx = 0;
            double sy = 0;
            double sz = 0;

            int n = polygon2.vertices.size();

            for (Vertex v : polygon2.vertices) {
                sx += v.pos.x();
                sy += v.pos.y();
                sz += v.pos.z();
            }

            Vector3d center = Vector3d.xyz(sx / n, sy / n, sz / n);

            rot = rot.rot(center, axis, angle * Math.PI / 180.0);

            for (Vertex v : polygon2.vertices) {
                v.pos = rot.transform(v.pos);
            }
        }

        int numvertices = polygon1.vertices.size();
        for (int i = 0; i < numvertices; i++) {

            int nexti = (i + 1) % numvertices;

            Vector3d bottomV1 = polygon1.vertices.get(i).pos;
            Vector3d topV1 = polygon2.vertices.get(i).pos;
            Vector3d bottomV2 = polygon1.vertices.get(nexti).pos;
            Vector3d topV2 = polygon2.vertices.get(nexti).pos;

            List<Vector3d> pPoints = Arrays.asList(bottomV2, topV2, topV1, bottomV1);

            newPolygons.add(Polygon.fromPoints(pPoints, polygon1.getStorage()));
        }

        polygon2 = polygon2.flipped();
        List<Polygon> topPolygons = PolygonUtil.concaveToConvex(polygon2);
        if (top) {
            newPolygons.addAll(topPolygons);
        }

        return newPolygons;
    }

    /**
     * Turns the points to counter-clockwise direction.
     */
    static Polygon toCCWPolygon(List<Vector3d> points) {
        if (!PolygonUtil.isCCW(points)) {
            points = new ArrayList<>(points);
            Collections.reverse(points);
        }

        return Polygon.fromPoints(points);
    }

    /**
     * Turns the points to clockwise direction.
     */
    static Polygon toCWPolygon(List<Vector3d> points) {
        if (PolygonUtil.isCCW(points)) {
            points = new ArrayList<>(points);
            Collections.reverse(points);
        }

        return Polygon.fromPoints(points);
    }
}
