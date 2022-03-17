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

import static eu.mihosoft.vvecmath.StoredVector3d.getStructSize;

/**
 * A modifiable 3d vector that is stored in an external double array.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public interface ModifiableStoredVector3d
        extends StoredVector3d, ModifiableVector3d {

    /**
     * Creates a new stored vector from the specified double array.
     *
     * @param storage double array used to store the vector
     * @param offset the storage offset used by the vector
     * @param stride the stride used to store the vector elements (x,y,z)
     * @return a new stored vector from the specified double array
     */
    static ModifiableStoredVector3d from(double[] storage, int offset, int stride) {
        StoredVector3dImpl result = new StoredVector3dImpl();
        result.setStorage(storage);
        result.setOffset(offset);
        result.setStride(stride);
        return result;
    }

    /**
     * Creates a new modifiable stored vector from the specified double array.
     *
     * @param storage double array used to store the vector
     * @param offset the storage offset used by the vector
     * @return a new stored vector from the specified double array
     */
    static ModifiableStoredVector3d from(double[] storage, int offset) {
        return from(storage, offset, getStructSize());
    }

    /**
     * Creates a new modifiable stored vector from the specified double array.
     *
     * @param storage double array used to store the vector
     * @param v the vector who's storage offset and stride shall be used
     * @return a new stored vector from the specified double array
     */
    static ModifiableStoredVector3d from(double[] storage, StoredVector3d v) {
        return from(storage, v.getOffset(), v.getStride());
    }
}
