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

import static java.lang.Math.acos;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public interface Vector3d {

    //////////////////////////////////
    // Constants
    //////////////////////////////////

    /**
     * Unity vector {@code (1, 1, 1)}.
     */
    Vector3d UNITY = new Vector3dImpl(1, 1, 1);

    /**
     * Vector {@code (1, 0, 0)}.
     */
    Vector3d X_ONE = new Vector3dImpl(1, 0, 0);

    /**
     * Vector {@code (0, 1, 0)}.
     */
    Vector3d Y_ONE = new Vector3dImpl(0, 1, 0);

    /**
     * Vector {@code (0, 0, 0)}.
     */
    Vector3d ZERO = new Vector3dImpl(0, 0, 0);

    /**
     * Vector {@code (0, 0, 1)}.
     */
    Vector3d Z_ONE = new Vector3dImpl(0, 0, 1);


    //////////////////////////////////
    // Factory methods
    //////////////////////////////////

    /**
     * Creates a new vector with specified {@code x}
     *
     * @param x x value
     * @return a new vector {@code [x,0,0]}
     *
     */
    public static Vector3d x(double x) {
        return new Vector3dImpl(x, 0, 0);
    }

    /**
     * Creates a new vector with specified {@code y}
     *
     * @param y y value
     * @return a new vector {@code [0,y,0]}
     *
     */
    public static Vector3d y(double y) {
        return new Vector3dImpl(0, y, 0);
    }

    /**
     * Creates a new vector with specified {@code z}
     *
     * @param z z value
     * @return a new vector {@code [0,0,z]}
     *
     */
    public static Vector3d z(double z) {
        return new Vector3dImpl(0, 0, z);
    }

    /**
     * Creates a new vector with specified {@code x}, {@code y} and
     * {@code z = 0}.
     *
     * @param x x value
     * @param y y value
     * @return
     */
    public static Vector3d xy(double x, double y) {
        return new Vector3dImpl(x, y);
    }

    /**
     * Creates a new vector with specified {@code x}, {@code y} and {@code z}.
     *
     * @param x x value
     * @param y y value
     * @param z z value
     * @return a new vector
     */
    public static Vector3d xyz(double x, double y, double z) {
        return new Vector3dImpl(x, y, z);
    }

    /**
     * Creates a new vector with specified {@code y} and {@code z}.
     *
     * @param y y value
     * @param z z value
     * @return a new vector
     */
    public static Vector3d yz(double y, double z) {
        return new Vector3dImpl(0, y, z);
    }

    /**
     * Creates a new vector with specified {@code x} and {@code z}.
     *
     * @param x x value
     * @param z z value
     * @return a new vector
     */
    public static Vector3d xz(double x, double z) {
        return new Vector3dImpl(x, 0, z);
    }

    /**
     * Creates a new vector {@code (0,0,0)}.
     *
     * @return a new vector
     */
    public static Vector3d zero() {
        return new Vector3dImpl(0, 0, 0);
    }

    /**
     * Creates a new vector {@code (1,1,1)}.
     *
     * @return a new vector
     */
    public static Vector3d unity() {
        return new Vector3dImpl(0, 0, 0);
    }

    //////////////////////////////////
    // Cloning and mutabilitization
    //////////////////////////////////

    /**
     * Returns a clone of this vector.
     *
     * @return a clone of this vector
     */
    Vector3d clone();


    /**
     * Clones the specified vector.
     *
     * @param source vector toclone
     * @return cloned vector
     */
    public static Vector3d clone(Vector3d source) {
        return new Vector3dImpl(source.x(), source.y(), source.z());
    }

    /**
     * Returns a modifiable copy of this vector.
     *
     * @return a modifiable copy of this vector
     */
    default ModifiableVector3d asModifiable() {
        return new ModifiableVector3dImpl(this.x(), this.y(), this.z());
    }

    //////////////////////////////////
    // Getters
    //////////////////////////////////


    /**
     * Returns the {@code x} component of this vector.
     *
     * @return the {@code x} component of this vector
     */
    double getX();

    /**
     * Returns the {@code y} component of this vector.
     *
     * @return the {@code y} component of this vector
     */
    double getY();

    /**
     * Returns the {@code z} component of this vector.
     *
     * @return the {@code z} component of this vector
     */
    double getZ();

    /**
     * Returns the components {code x,y,z} as double array.
     *
     * @return the components {code x,y,z} as double array
     */
    default double[] get() {
        return new double[]{x(), y(), z()};
    }

    /**
     * Returns the i-th component of this vector.
     *
     * @param i component index
     * @return the i-th component of this vector
     */
    default double get(int i) {
        switch (i) {
            case 0:
                return x();
            case 1:
                return y();
            case 2:
                return z();
            default:
                throw new RuntimeException("Illegal index: " + i);
        }
    }

    /**
     * Returns the x component of this vector
     *
     * @return x component of this vector
     */
    default double x() {
        return getX();
    }

    /**
     * Returns the y component of this vector
     *
     * @return y component of this vector
     */
    default double y() {
        return getY();
    }

    /**
     * Returns the z component of this vector
     *
     * @return z component of this vector
     */
    default double z() {
        return getZ();
    }

    //////////////////////////////////
    // Vectorspace addition
    //////////////////////////////////

    /**
     * Returns the sum of this vector and the specified vector.
     *
     * @param v the vector to add
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the sum of this vector and the specified vector
     */
    default Vector3d plus(Vector3d v) {
        return plus(v.x(), v.y(), v.z());
    }

    /**
     * Returns the sum of this vector and the specified vector.
     *
     * @param x x coordinate of the vector to add
     * @param y y coordinate of the vector to add
     * @param z z coordinate of the vector to add
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the sum of this vector and the specified vector
     */
    default Vector3d plus(double x, double y, double z) {
        return Vector3d.xyz(this.x() + x, this.y() + y, this.z() + z);
    }

    /**
     * Returns the sum of this vector and the specified vector.
     *
     * @param v the vector to add
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the sum of this vector and the specified vector
     */
    default Vector3d added(Vector3d v) {
        return plus(v.x(), v.y(), v.z());
    }


    /**
     * Returns the difference of this vector and the specified vector.
     *
     * @param v the vector to subtract
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the difference of this vector and the specified vector
     */
    default Vector3d minus(Vector3d v) {
        return minus(v.x(), v.y(), v.z());
    }

    /**
     * Returns the difference of this vector and the specified vector.
     *
     * @param x x coordinate of the vector to subtract
     * @param y y coordinate of the vector to subtract
     * @param z z coordinate of the vector to subtract
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the difference of this vector and the specified vector
     */
    default Vector3d minus(double x, double y, double z) {
        return Vector3d.xyz(this.x() - x, this.y() - y, this.z() - z);
    }

    /**
     * Returns the difference of this vector and the specified vector.
     *
     * @param v the vector to subtract
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the difference of this vector and the specified vector
     */
    default Vector3d subtracted(Vector3d v) {
        return minus(v.x(), v.y(), v.z());
    }


    //////////////////////////////////
    // Vectorspace scalar mul.
    //////////////////////////////////


    /**
     * Returns the product of this vector and the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the product of this vector and the specified value
     */
    default Vector3d times(double a) {
        return new Vector3dImpl(x() * a, y() * a, z() * a);
    }

    /**
     * Returns the product of this vector and the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the product of this vector and the specified value
     */
    default Vector3d multiplied(double a) {
        return times(a);
    }

    /**
     * Returns this vector divided by the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return this vector divided by the specified value
     */
    default Vector3d divided(double a) {
        return new Vector3dImpl(x() / a, y() / a, z() / a);
    }


    //////////////////////////////////
    // Hadamard product
    //////////////////////////////////

    /**
     * Returns the product of this vector and the specified vector.
     *
     * @param a the vector
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the product of this vector and the specified vector
     */
    default Vector3d times(Vector3d a) {
        return times(a.x(), a.y(), a.z());
    }

    /**
     * Returns the product of this vector and the specified vector.
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the product of this vector and the specified vector
     */
    default Vector3d times(double x, double y, double z) {
        return Vector3d.xyz(this.x() * x, this.y() * y, this.z() * z);
    }


    /**
     * Returns the product of this vector and the specified vector.
     *
     * @param a the vector
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return the product of this vector and the specified vector
     */
    default Vector3d multiplied(Vector3d a) {
        return times(a.x(), a.y(), a.z());
    }


    //////////////////////////////////
    // Inner product
    //////////////////////////////////

    /**
     * Returns the dot product of this vector and the specified vector.
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @param a the second vector
     *
     * @return the dot product of this vector and the specified vector
     */
    default double dot(Vector3d a) {
        return this.x() * a.x() + this.y() * a.y() + this.z() * a.z();
    }

    //////////////////////////////////
    // Cross product
    //////////////////////////////////

    /**
     * Returns the cross product of this vector and the specified vector.
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @param a the vector
     *
     * @return the cross product of this vector and the specified vector.
     */
    default Vector3d crossed(Vector3d a) {
        return new Vector3dImpl(
                this.y() * a.z() - this.z() * a.y(),
                this.z() * a.x() - this.x() * a.z(),
                this.x() * a.y() - this.y() * a.x()
        );
    }


    //////////////////////////////////
    // Misc
    //////////////////////////////////

    /**
     * Calculates the magnitude of this vector. If the squared magnitude of this vector is
     * needed, use {@link #magnitudeSq()}.
     *
     * <b>Note:</b> This vector is not modified.
     */
    default double magnitude() {
        return Math.sqrt(this.dot(this));
    }

    /**
     * Calculates the squared magnitude of this vector
     * (<code>this.dot(this)</code>).
     *
     * <b>Note:</b> This vector is not modified.
     */
    default double magnitudeSq() {
        return this.dot(this);
    }

    /**
     * Returns the angle between this and the specified vector in degrees.
     */
    default double angle(Vector3d v) {
        double val = this.dot(v) / (this.magnitude() * v.magnitude());
        return acos(max(min(val, 1), -1)) * 180.0 / Math.PI; // compensate rounding errors
    }

    /**
     * Returns the distance between the specified point and this point.
     */
    default double distance(Vector3d p) {
        return minus(p).magnitude();
    }

    /**
     * Returns a new vector which is orthogonal to this vector.
     */
    default Vector3d orthogonal() {
        return  Math.abs(z()) < Math.abs(x())  ? Vector3d.xy(y(),-x()) : Vector3d.yz(-z(),y());
    }

    /**
     * Returns a normalized copy of this vector with length {@code 1}.
     *
     * <b>Note:</b> This vector is not modified.
     */
    default Vector3d normalized() {
        return this.divided(this.magnitude());
    }

    /**
     * Returns a negated copy of this vector.
     *
     * <b>Note:</b> This vector is not modified.
     */
    default Vector3d negated() {
        return new Vector3dImpl(-x(), -y(), -z());
    }

    /**
     * Linearly interpolates between this and the specified vector.
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @param a Second vector.
     * @param t Interpolation value.
     *
     * @return Copy of this vector if {@code t = 0}; copy of a if {@code t = 1};
     * the point midway between this and the specified vector if {@code t = 0.5}
     */
    default Vector3d lerp(Vector3d a, double t) {
        return this.plus(a.minus(this).times(t));
    }


    /**
     * Projects the specified vector onto this vector.
     *
     * @param v Vector to project onto this vector.
     * @return The projection of the specified vector onto this vector.
     */
    public default Vector3d project(Vector3d v) {

        double pScale = v.dot(this) / this.magnitudeSq();

        return this.times(pScale);
    }

    /**
     * Indicates whether the two given points are collinear with this
     * vector/point.
     *
     * @param p2 Second point.
     * @param p3 Third point.
     *
     * @return {@code true} if all three points are collinear; {@code false} otherwise.
     */
     default boolean collinear(Vector3d p2, Vector3d p3) {

        // The points p1, p2, p3 are collinear (are on the same line segment)
        // if and only if the largest of the lenghts of
        //
        //   a = P1P2,
        //   b = P1P2
        //   c = P2P3
        //
        // is equal to the sum of the other two.
        //
        // Explanation:
        //
        // If p1, p2 and p3 are on the same line then the point in the 'middle'
        // cuts the line segment into two smaller pieces. That is the sum of the
        // lengths of the smaller pieces is equal to the largest one.
        //
        // That is, the following expression determines whether the three points
        // are collinear
        //
        //   boolean collinear = largest > (second + third)

        double a = this.distance(p2);
        double b = this.distance(p3);
        double c = p2.distance(p3);

        double largest;
        double second;
        double third;

        if (a > b && a > c) {
            // a is largest
            largest = a;
            second = b;
            third = c;
        } else if (b > a && b > c) {
            // b is largest
            largest = b;
            second = a;
            third = c;
        } else if (c > a && c > b) {
            // c is largest
            largest = c;
            second = a;
            third = b;
        } else {
            // lengths are not distinct.
            //
            // there are two possibilities:
            //
            //   a: they are vertices of a equilateral triangle   -> false
            //   b: they are zero, i.e., all points are identical -> true
            //
            return a == 0 && b == 0 && c == 0;
        }

        return Math.abs(largest  - (second + third)) < Plane.TOL;

    }

    //////////////////////////////////
    // Transformation
    //////////////////////////////////


    /**
     * Returns a transformed copy of this vector.
     *
     * @param transform The transform to apply.
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @return A transformed copy of this vector.
     */
    default Vector3d transformed(Transform transform) {
        return transform.transform(this.asModifiable());
    }

    /**
     * Returns a transformed copy of this vector.
     *
     * @param transform The transform to apply.
     *
     * <b>Note:</b> This vector is not modified.
     *
     * @param amount Transformation amount (0 = 0%, 1 = 100%).
     *
     * @return A transformed copy of this vector.
     */
    default Vector3d transformed(Transform transform, double amount) {
        return transform.transform(this.asModifiable(), amount);
    }



    //////////////////////////////////
    // String
    //////////////////////////////////

    /**
     * Returns this vector in STL string format.
     *
     * @return this vector in STL string format
     */
    default String toStlString() {
        return toStlString(new StringBuilder()).toString();
    }

    /**
     * Returns this vector in STL string format.
     *
     * @param sb string builder
     * @return the specified string builder
     */
    default StringBuilder toStlString(StringBuilder sb) {
        return sb.append(this.x()).append(" ").
                append(this.y()).append(" ").
                append(this.z());
    }

    /**
     * Returns this vector in OBJ string format.
     *
     * @return this vector in OBJ string format
     */
    default String toObjString() {
        return toObjString(new StringBuilder()).toString();
    }

    /**
     * Returns this vector in OBJ string format.
     *
     * @param sb string builder
     * @return the specified string builder
     */
    default StringBuilder toObjString(StringBuilder sb) {
        return sb.append(this.x()).append(" ").
                append(this.y()).append(" ").
                append(this.z());
    }
}
