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

import static java.lang.Math.abs;
import java.util.Random;

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Vector3dImpl implements Vector3d {

    protected double x;
    protected double y;
    protected double z;

    /**
     * Creates a new vector.
     *
     * @param x x value
     * @param y y value
     * @param z z value
     */
    public Vector3dImpl(double x, double y, double z) {

        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a new vector with specified {@code x}, {@code y} and
     * {@code z = 0}.
     *
     * @param x x value
     * @param y y value
     */
    public Vector3dImpl(double x, double y) {

        this.x = x;
        this.y = y;
        this.z = 0;
    }

    @Override
    public Vector3d clone() {
        return new Vector3dImpl(x, y, z);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    Vector3d set(double... xyz) {
        
        if(xyz.length > 3) {
            throw new IllegalArgumentException(
                    "Wrong number of components. "
                            + "Expected number of components <= 3, got: " + xyz.length);
        }
        
        for (int i = 0; i < xyz.length; i++) {
            set(i, xyz[i]);
        }

        return this;
    }

    void setX(double x) {
        this.x = x;
    }

    void setY(double y) {
        this.y = y;
    }

    void setZ(double z) {
        this.z = z;
    }

    Vector3d set(int i, double value) {
        switch (i) {
            case 0:
                setX(value);
                break;
            case 1:
                setY(value);
                break;
            case 2:
                setZ(value);
            default:
                throw new RuntimeException("Illegal index: " + i);
        }

        return this;
    }

    @Override
    public String toString() {
        return VectorUtilInternal.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        return VectorUtilInternal.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return VectorUtilInternal.getHashCode(this);
    }    

}
