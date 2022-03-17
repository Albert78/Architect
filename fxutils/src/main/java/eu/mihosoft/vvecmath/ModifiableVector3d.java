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
 * Modifiable 3d vector.
 *
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
public interface ModifiableVector3d extends Vector3d {

    /**
     * Sets the specified vector components.
     *
     * @param xyz vector components to set (number of components {@code <= 3} are valid)
     * @return this vector
     */
    Vector3d set(double... xyz);

    /**
     * Sets the i-th component of this vector.
     *
     * @param i component index
     * @param value value to set
     * @return this vector
     */
    Vector3d set(int i, double value);

    /**
     * Sets the {@code x} component of this vector.
     *
     * @param x component to set
     */
    default void setX(double x) {
        set(0, x);
    }

    /**
     * Sets the {@code y} component of this vector.
     *
     * @param y component to set
     */
    default void setY(double y) {
        set(1, y);
    }

    /**
     * Sets the {@code z} component of this vector.
     *
     * @param z component to set
     */
    default void setZ(double z) {
        set(2, z);
    }

    /**
     * Adds the specified vector to this vector.
     *
     * @param v the vector to add
     *
     * <b>Note:</b> this vector <b>is</b> not modified.
     *
     * @return this vector
     */
    default Vector3d add(Vector3d v) {
        this.setX(x() + v.x());
        this.setY(y() + v.y());
        this.setZ(z() + v.z());

        return this;
    }

    /**
     * Adds the specified vector to this vector.
     *
     * @param x x coordinate of the vector to add
     * @param y y coordinate of the vector to add
     * @param z z coordinate of the vector to add
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    default Vector3d add(double x, double y, double z) {
        this.setX(x() + x);
        this.setX(y() + y);
        this.setX(z() + z);

        return this;
    }

    /**
     * Subtracts the specified vector from this vector.
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @param v vector to subtract
     * @return this vector
     */
    default Vector3d subtract(Vector3d v) {
        this.setX(x() - v.x());
        this.setY(y() - v.y());
        this.setZ(z() - v.z());

        return this;
    }

    /**
     * Subtracts the specified vector from this vector.
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @param x x coordinate of the vector to subtract
     * @param y y coordinate of the vector to subtract
     * @param z z coordinate of the vector to subtract
     *
     * @return this vector
     */
    default Vector3d subtract(double x, double y, double z) {
        this.setX(x() - x);
        this.setY(y() - y);
        this.setZ(z() - z);

        return this;
    }

    /**
     * Multiplies this vector with the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    default Vector3d multiply(double a) {
        setX(x() * a);
        setY(y() * a);
        setZ(z() * a);

        return this;
    }

    /**
     * Multiplies this vector with the specified vector.
     *
     * @param a the vector
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    default Vector3d multiply(Vector3d a) {
        setX(x() * a.x());
        setY(y() * a.y());
        setZ(z() * a.z());

        return this;
    }

    /**
     * Devides this vector with the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    default Vector3d divide(double a) {
        setX(x() / a);
        setY(y() / a);
        setZ(z() / a);

        return this;
    }

    /**
     * Divides this vector with the specified vector.
     *
     * @param v the vector
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    default Vector3d divide(Vector3d v) {
        setX(x() / v.x());
        setY(y() / v.y());
        setZ(z() / v.z());

        return this;
    }

    /**
     * Stores the cross product of this vector and the specified vector in this
     * vector.
     *
     * <b>Note:</b> this vector <b>is</b>modified.
     *
     * @param a the vector
     *
     * @return this vector
     */
    default Vector3d cross(Vector3d a) {
        setX(this.y() * a.z() - this.z() * a.y());
        setY(this.z() * a.x() - this.x() * a.z());
        setZ(this.x() * a.y() - this.y() * a.x());

        return this;
    }

    /**
     * Negates this vector.
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    default Vector3d negate() {
        return multiply(-1.0);
    }

    /**
     * Normalizes this vector with length {@code 1}.
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    default Vector3d normalize() {
        return this.divide(this.magnitude());
    }

}
