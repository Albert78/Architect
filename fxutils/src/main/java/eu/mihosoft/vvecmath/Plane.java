/*
 * Copyright 2017-2019 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Michael Hoffer <info@michaelhoffer.de>.
 */
package eu.mihosoft.vvecmath;

/**
 * Represents a plane in 3D space.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class Plane {

    public static final double TOL = 1e-12;

    /**
     * XY plane.
     */
    public static final Plane XY_PLANE = new Plane(Vector3d.ZERO, Vector3d.Z_ONE);
    /**
     * XZ plane.
     */
    public static final Plane XZ_PLANE = new Plane(Vector3d.ZERO, Vector3d.Y_ONE);
    /**
     * YZ plane.
     */
    public static final Plane YZ_PLANE = new Plane(Vector3d.ZERO, Vector3d.X_ONE);

    /**
     * Normal vector.
     */
    private final Vector3d normal;
    /**
     * Distance to origin.
     */
    private final Vector3d anchor;

    /**
     * Constructor. Creates a new plane defined by its normal vector and an
     * anchor point
     *
     * @param normal plane normal
     * @param dist distance to origin
     */
    private Plane(Vector3d anchor, Vector3d normal) {
        this.normal = normal.normalized();
        this.anchor = anchor;
    }

    /**
     * Creates a plane defined by the the specified points. The anchor point of
     * the plane is the centroid of the triangle (a,b,c).
     *
     * @param a first point
     * @param b second point
     * @param c third point
     * @return a plane
     */
    public static Plane fromPoints(Vector3d a, Vector3d b, Vector3d c) {
        Vector3d normal = b.minus(a).crossed(c.minus(a)).normalized();

        Vector3d anchor = Vector3d.zero();

        anchor = anchor.plus(a);
        anchor = anchor.plus(b);
        anchor = anchor.plus(c);

        anchor = anchor.times(1.0 / 3.0);

        return new Plane(anchor, normal);
    }

    /**
     * Creates a plane defined by an anchor point and a normal vector.
     *
     * @param p anchor point
     * @param n plane normal
     * @return a plane
     */
    public static Plane fromPointAndNormal(Vector3d p, Vector3d n) {
        return new Plane(p, n);
    }

    @Override
    public Plane clone() {
        return new Plane(anchor, normal);
    }

    /**
     * Returns a flipped copy of this plane.
     * @return flipped coppy of this plane
     */
    public Plane flipped() {
        return new Plane(anchor, normal.negated());
    }

    /**
     * Return the distance of this plane to the origin.
     *
     * @return distance of this plane to the origin
     */
    public double getDist() {
        return anchor.magnitude();
    }

    /**
     * Return the anchor point of this plane.
     *
     * @return anchor point of this plane
     */
    public Vector3d getAnchor() {
        return anchor;
    }

    /**
     * Returns the normal of this plane.
     * @return the normal of this plane
     */
    public Vector3d getNormal() {
        return normal;
    }

    /**
     * Projects the specified point onto this plane.
     *
     * @param p point to project
     * @return projection of p onto this plane
     */
    public Vector3d project(Vector3d p) {

        // dist:   the distance of this plane to the origin
        // anchor: is the anchor point of the plane (closest point to origin)
        // n:      the plane normal
        //
        // a) project (p-anchor) onto n
        Vector3d projV = normal.project(p.minus(anchor));

        // b) subtract projection from p to get projP
        Vector3d projP = p.minus(projV);

        return projP;
    }

    /**
     * Returns the shortest distance between the specified point and this plane.
     *
     * @param p point
     * @return the shortest distance between the specified point and this plane
     */
    public double distance(Vector3d p) {
        return p.minus(project(p)).magnitude();
    }

    /**
     * Determines whether the specified point is in front of, in back of or on
     * this plane.
     *
     * @param p point to check
     * @param TOL tolerance
     * @return {@code 1}, if p is in front of the plane, {@code -1}, if the
     * point is in the back of this plane and {@code 0} if the point is on this
     * plane
     */
    public int compare(Vector3d p, double TOL) {

        // angle between vector n and vector (p-anchor)
        double t = this.normal.dot(p.minus(anchor));
        return (t < -TOL) ? -1 : (t > TOL) ? 1 : 0;
    }
    
    /**
     * Determines whether the specified point is in front of, in back of or on
     * this plane.
     *
     * @param p point to check
     * 
     * @return {@code 1}, if p is in front of the plane, {@code -1}, if the
     * point is in the back of this plane and {@code 0} if the point is on this
     * plane
     */
    public int compare(Vector3d p) {

        // angle between vector n and vector (p-anchor)
        double t = this.normal.dot(p.minus(anchor));
        return (t < -TOL) ? -1 : (t > TOL) ? 1 : 0;
    }

}
