/**
 * Polygon.java
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
package eu.mihosoft.jcsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vvecmath.Transform;
import eu.mihosoft.vvecmath.Vector3d;

/**
 * Represents a convex polygon.
 *
 * Each convex polygon has a {@code shared} property, which is shared between
 * all polygons that are clones of each other or where split from the same
 * polygon. This can be used to define per-polygon properties (such as surface
 * color).
 */
public final class Polygon {
    /**
     * Polygon vertices.
     */
    public final List<Vertex> vertices;

    /**
     * Shared property (can be used for shared color etc.).
     */
    private PropertyStorage shared;

    private boolean valid = true;

    /**
     * Plane defined by this polygon.
     *
     * <b>Note:</b> Uses first three vertices to define the plane.
     */
    public final Plane _csg_plane;
    private eu.mihosoft.vvecmath.Plane plane;

    /**
     * Returns the plane defined by this triangle.
     */
    public eu.mihosoft.vvecmath.Plane getPlane() {
        return plane;
    }

    /**
     * Decomposes the specified concave polygon into convex polygons.
     *
     * @param points The points that define the polygon.
     * @return The decomposed concave polygons (list of convex polygons).
     */
    public static List<Polygon> fromConcavePoints(Vector3d... points) {
        return fromConcavePoints(Arrays.asList(points));
    }

    /**
     * Decomposes the specified concave polygon into convex polygons.
     *
     * @param points the points that define the polygon
     * @return the decomposed concave polygon (list of convex polygons)
     */
    public static List<Polygon> fromConcavePoints(List<Vector3d> points) {
        Polygon p = fromPoints(points);

        return PolygonUtil.concaveToConvex(p);
    }

    /**
     * Indicates whether this polyon is valid, i.e., if its vertex vectors are linearly independent and
     * thus the polygon has a surface area > 0.
     *
     * @return
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Creates a new polygon that consists of the specified vertices.
     *
     * <b>Note:</b> The vertices used to initialize a polygon must be coplanar and form a convex loop.
     *
     * @param vertices Polygon vertices.
     * @param shared Shared storage to be used.
     */
    public Polygon(List<Vertex> vertices, PropertyStorage shared) {
        this.vertices = vertices;
        this.shared = shared;
        this._csg_plane = Plane.createFromPoints(
                vertices.get(0).pos,
                vertices.get(1).pos,
                vertices.get(2).pos);
        this.plane = eu.mihosoft.vvecmath.Plane.
                fromPointAndNormal(centroid(), _csg_plane.normal);

        validateAndInit();
    }

    /**
     * Creates a new polygon that consists of the specified vertices.
     *
     * <b>Note:</b> The vertices used to initialize a polygon must be coplanar and form a convex loop.
     *
     * @param vertices Polygon vertices
     */
    public Polygon(List<Vertex> vertices) {
        this(vertices, null);
    }

    /**
     * Constructor. Creates a new polygon that consists of the specified
     * vertices.
     *
     * <b>Note:</b> the vertices used to initialize a polygon must be coplanar
     * and form a convex loop.
     *
     * @param vertices polygon vertices
     *
     */
    public Polygon(Vertex... vertices) {
        this(Arrays.asList(vertices));
    }

    private void validateAndInit() {
        if (vertices.size() < 3) {
            throw new RuntimeException(
                    "Invalid polygon: at least 3 vertices expected, got: "
                    + vertices.size());
        }
        for (Vertex v : vertices) {
            v.normal = _csg_plane.normal;
        }
        if (Vector3d.ZERO.equals(_csg_plane.normal)) {
            valid = false;
            throw new RuntimeException(
                "Invalid polygon with a zero-width normal! Probably, duplicate points have been specified!" + toStlString());
        }
    }

    @Override
    public Polygon clone() {
        List<Vertex> newVertices = new ArrayList<>();
        this.vertices.forEach((vertex) -> {
            newVertices.add(vertex.clone());
        });
        return new Polygon(newVertices, getStorage());
    }

    /**
     * Flips this polygon, which will change this polygon.
     *
     * @return This polygon.
     */
    public Polygon flip() {
        vertices.forEach((vertex) -> {
            vertex.flip();
        });
        Collections.reverse(vertices);

        _csg_plane.flip();
        this.plane = plane.flipped();

        return this;
    }

    /**
     * Returns a flipped copy of this polygon.
     *
     * <b>Note:</b> This polygon is not modified.
     *
     * @return a flipped copy of this polygon
     */
    public Polygon flipped() {
        return clone().flip();
    }

    /**
     * Returns this polygon in STL string format.
     *
     * @return this polygon in STL string format
     */
    public String toStlString() {
        return toStlString(new StringBuilder()).toString();
    }

    /**
     * Returns this polygon in STL string format.
     *
     * @param sb string builder
     *
     * @return the specified string builder
     */
    public StringBuilder toStlString(StringBuilder sb) {

        if (this.vertices.size() >= 3) {

            // TODO: improve the triangulation?
            //
            // STL requires triangular polygons.
            // If our polygon has more vertices, create
            // multiple triangles:
            String firstVertexStl = this.vertices.get(0).toStlString();
            for (int i = 0; i < this.vertices.size() - 2; i++) {
                sb.
                        append("  facet normal ").append(this._csg_plane.normal.toStlString()).append("\n").
                        append("    outer loop\n").
                        append("      ").append(firstVertexStl).append("\n").
                        append("      ");
                this.vertices.get(i + 1).toStlString(sb).append("\n").
                        append("      ");
                this.vertices.get(i + 2).toStlString(sb).append("\n").
                        append("    endloop\n").
                        append("  endfacet\n");
            }
        }

        return sb;
    }

    /**
     * Returns a triangulated version of this polygon.
     *
     * @return triangles
     */
    public List<Polygon> toTriangles() {
        List<Polygon> result = new ArrayList<>();

        if (this.vertices.size() >= 3) {
            // TODO: improve the triangulation?
            //
            // If our polygon has more vertices, create
            // multiple triangles:
            Vertex firstVertexStl = this.vertices.get(0);
            for (int i = 0; i < this.vertices.size() - 2; i++) {

                // create triangle
                Polygon polygon = Polygon.fromPoints(
                        firstVertexStl.pos,
                        this.vertices.get(i + 1).pos,
                        this.vertices.get(i + 2).pos
                );

                result.add(polygon);
            }
        }

        return result;
    }

    /**
     * Translates this polygon.
     *
     * @param v the vector that defines the translation
     * @return this polygon
     */
    public Polygon translate(Vector3d v) {
        vertices.forEach((vertex) -> {
            vertex.pos = vertex.pos.plus(v);
        });

        Vector3d a = this.vertices.get(0).pos;
        Vector3d b = this.vertices.get(1).pos;
        Vector3d c = this.vertices.get(2).pos;

        // TODO plane update correct?
        this._csg_plane.normal = b.minus(a).crossed(c.minus(a));

        this.plane = eu.mihosoft.vvecmath.Plane.
                fromPointAndNormal(centroid(), _csg_plane.normal);

        return this;
    }

    /**
     * Returns a translated copy of this polygon.
     *
     * <b>Note:</b> this polygon is not modified.
     *
     * @param v the vector that defines the translation
     *
     * @return a translated copy of this polygon
     */
    public Polygon translated(Vector3d v) {
        return clone().translate(v);
    }

    /**
     * Applies the specified transformation to this polygon.
     *
     * <b>Note:</b> if the applied transformation performs a mirror operation
     * the vertex order of this polygon is reversed.
     *
     * @param transform the transformation to apply
     *
     * @return this polygon
     */
    public Polygon transform(Transform transform) {
        this.vertices.stream().forEach(
                (v) -> {
                    v.transform(transform);
                }
        );

        Vector3d a = this.vertices.get(0).pos;
        Vector3d b = this.vertices.get(1).pos;
        Vector3d c = this.vertices.get(2).pos;

        this._csg_plane.normal = b.minus(a).crossed(c.minus(a)).normalized();
        this._csg_plane.dist = this._csg_plane.normal.dot(a);

        this.plane = eu.mihosoft.vvecmath.Plane.
                fromPointAndNormal(centroid(), _csg_plane.normal);

        vertices.forEach((vertex) -> {
            vertex.normal = plane.getNormal();
        });

        if (transform.isMirror()) {
            // the transformation includes mirroring. flip polygon
            flip();

        }
        return this;
    }

    /**
     * Returns a transformed copy of this polygon.
     *
     * <b>Note:</b> if the applied transformation performs a mirror operation
     * the vertex order of this polygon is reversed.
     *
     * <b>Note:</b> this polygon is not modified.
     *
     * @param transform the transformation to apply
     * @return a transformed copy of this polygon
     */
    public Polygon transformed(Transform transform) {
        return clone().transform(transform);
    }

    /**
     * Creates a polygon from the specified point list.
     *
     * @param points the points that define the polygon
     * @param shared shared property storage
     * @return a polygon defined by the specified point list
     */
    public static Polygon fromPoints(List<Vector3d> points,
            PropertyStorage shared) {
        return fromPoints(points, shared, null);
    }

    /**
     * Creates a polygon from the specified point list.
     *
     * @param points the points that define the polygon
     * @return a polygon defined by the specified point list
     */
    public static Polygon fromPoints(List<Vector3d> points) {
        return fromPoints(points, new PropertyStorage(), null);
    }

    /**
     * Creates a polygon from the specified points.
     *
     * @param points the points that define the polygon
     * @return a polygon defined by the specified point list
     */
    public static Polygon fromPoints(Vector3d... points) {
        return fromPoints(Arrays.asList(points), new PropertyStorage(), null);
    }

    /**
     * Creates a polygon from the specified point list.
     *
     * @param points the points that define the polygon
     * @param shared
     * @param plane may be null
     * @return a polygon defined by the specified point list
     */
    private static Polygon fromPoints(
            List<Vector3d> points, PropertyStorage shared, Plane plane) {

        Vector3d normal
                = (plane != null) ? plane.normal.clone() : null;

        if (normal == null) {
            normal = Plane.createFromPoints(
                    points.get(0),
                    points.get(1),
                    points.get(2)).normal;
        }

        List<Vertex> vertices = new ArrayList<>();

        for (Vector3d p : points) {
            Vector3d vec = p.clone();
            Vertex vertex = new Vertex(vec, normal);
            vertices.add(vertex);
        }

        return new Polygon(vertices, shared);
    }

    /**
     * Returns the bounds of this polygon.
     *
     * @return bouds of this polygon
     */
    public Bounds getBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;

        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < vertices.size(); i++) {

            Vertex vert = vertices.get(i);

            if (vert.pos.x() < minX) {
                minX = vert.pos.x();
            }
            if (vert.pos.y() < minY) {
                minY = vert.pos.y();
            }
            if (vert.pos.z() < minZ) {
                minZ = vert.pos.z();
            }

            if (vert.pos.x() > maxX) {
                maxX = vert.pos.x();
            }
            if (vert.pos.y() > maxY) {
                maxY = vert.pos.y();
            }
            if (vert.pos.z() > maxZ) {
                maxZ = vert.pos.z();
            }

        } // end for vertices

        return new Bounds(
                Vector3d.xyz(minX, minY, minZ),
                Vector3d.xyz(maxX, maxY, maxZ));
    }

    public Vector3d centroid() {
        Vector3d sum = Vector3d.zero();

        for (Vertex v : vertices) {
            sum = sum.plus(v.pos);
        }

        return sum.times(1.0 / vertices.size());
    }

    /**
     * Indicates whether the specified point is contained within this polygon.
     *
     * @param p point
     * @return {@code true} if the point is inside the polygon or on one of the
     * edges; {@code false} otherwise
     */
    public boolean contains(Vector3d p) {

        // P not on the plane
        if (plane.distance(p) > Plane.EPSILON) {
            return false;
        }

        // if P is on one of the vertices, return true
        for (int i = 0; i < vertices.size() - 1; i++) {
            if (p.minus(vertices.get(i).pos).magnitude() < Plane.EPSILON) {
                return true;
            }
        }

        // if P is on the plane, we proceed with projection to XY plane
        //
        // P1--P------P2
        //     ^
        //     |
        // P is on the segment if( dist(P1,P) + dist(P2,P) - dist(P1,P2) < TOL)
        for (int i = 0; i < vertices.size() - 1; i++) {

            Vector3d p1 = vertices.get(i).pos;
            Vector3d p2 = vertices.get(i + 1).pos;

            boolean onASegment = p1.minus(p).magnitude() + p2.minus(p).magnitude()
                    - p1.minus(p2).magnitude() < Plane.EPSILON;

            if (onASegment) {
                return true;
            }
        }

        // find projection plane
        // we start with XY plane
        int coordIndex1 = 0;
        int coordIndex2 = 1;

        boolean orthogonalToXY = Math.abs(eu.mihosoft.vvecmath.Plane.XY_PLANE.getNormal()
                .dot(plane.getNormal())) < Plane.EPSILON;

        boolean foundProjectionPlane = false;
        if (!orthogonalToXY && !foundProjectionPlane) {
            coordIndex1 = 0;
            coordIndex2 = 1;
            foundProjectionPlane = true;
        }

        boolean orthogonalToXZ = Math.abs(eu.mihosoft.vvecmath.Plane.XZ_PLANE.getNormal()
                .dot(plane.getNormal())) < Plane.EPSILON;

        if (!orthogonalToXZ && !foundProjectionPlane) {
            coordIndex1 = 0;
            coordIndex2 = 2;
            foundProjectionPlane = true;
        }

        boolean orthogonalToYZ = Math.abs(eu.mihosoft.vvecmath.Plane.YZ_PLANE.getNormal()
                .dot(plane.getNormal())) < Plane.EPSILON;

        if (!orthogonalToYZ && !foundProjectionPlane) {
            coordIndex1 = 1;
            coordIndex2 = 2;
            foundProjectionPlane = true;
        }

        // see from http://www.java-gaming.org/index.php?topic=26013.0
        // see http://alienryderflex.com/polygon/
        // see http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
        int i, j = vertices.size() - 1;
        boolean oddNodes = false;
        double x = p.get(coordIndex1);
        double y = p.get(coordIndex2);
        for (i = 0; i < vertices.size(); i++) {
            double xi = vertices.get(i).pos.get(coordIndex1);
            double yi = vertices.get(i).pos.get(coordIndex2);
            double xj = vertices.get(j).pos.get(coordIndex1);
            double yj = vertices.get(j).pos.get(coordIndex2);
            if ((yi < y && yj >= y
                    || yj < y && yi >= y)
                    && (xi <= x || xj <= x)) {
                oddNodes ^= (xi + (y - yi) / (yj - yi) * (xj - xi) < x);
            }
            j = i;
        }
        return oddNodes;

    }

    public boolean contains(Polygon p) {
        for (Vertex v : p.vertices) {
            if (!contains(v.pos)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Indicates whether this polygon is defined counter-clockwise.
     * @return {@code true} if this polygon is defined counter-clockwise; {@code false} otherwise
     */
    public boolean isCCW() {
        return PolygonUtil.isCCW_XY(vertices
            .stream()
            .map(v -> v.pos)
            .collect(Collectors.toList()));
    }

    public PropertyStorage getStorage() {
        if (shared == null) {
            shared = new PropertyStorage();
        }

        return shared;
    }

    public void setStorage(PropertyStorage storage) {
        shared = storage;
    }
}
