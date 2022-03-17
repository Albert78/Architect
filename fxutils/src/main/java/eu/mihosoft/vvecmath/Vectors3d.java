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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Utility class for generating large amounts of vectors.
 *
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
public final class Vectors3d {
    
    private Vectors3d() {
        throw new AssertionError("Don't instantiate me!");
    }

    /**
     * Converts the specified x-values to a list of vectors.
     *
     * @param xValues x values
     * @return list of vectors
     */
    public static List<Vector3d> x(double... xValues) {
        return DoubleStream.of(xValues).mapToObj(x -> Vector3d.x(x)).collect(Collectors.toList());
    }

    /**
     * Converts the specified y-values to a list of vectors.
     *
     * @param yValues y values
     * @return list of vectors
     */
    public static List<Vector3d> y(double... yValues) {
        return DoubleStream.of(yValues).mapToObj(y -> Vector3d.y(y)).collect(Collectors.toList());
    }

    /**
     * Converts the specified z-values to a list of vectors.
     *
     * @param zValues z values
     * @return list of vectors
     */
    public static List<Vector3d> z(double... zValues) {
        return DoubleStream.of(zValues).mapToObj(z -> Vector3d.z(z)).collect(Collectors.toList());
    }

    /**
     * Converts the specified (x,y)-values to a list of vectors.
     *
     * @param xyValues (x,y) values
     * @return list of vectors
     */
    public static List<Vector3d> xy(double... xyValues) {

        if (xyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Number of specified values must be a multiple of 2!");
        }

        return IntStream.range(1, xyValues.length).filter(i->(i+1)%2==0)
                .mapToObj(i -> Vector3d.xy(xyValues[i - 1], xyValues[i])).
                collect(Collectors.toList());
    }

    /**
     * Converts the specified (x,z)-values to a list of vectors.
     *
     * @param xzValues (x,z) values
     * @return list of vectors
     */
    public static List<Vector3d> xz(double... xzValues) {

        if (xzValues.length % 2 != 0) {
            throw new IllegalArgumentException("Number of specified values must be a multiple of 2!");
        }

        return IntStream.range(1, xzValues.length).filter(i->(i+1)%2==0)
                .mapToObj(i -> Vector3d.xz(xzValues[i - 1], xzValues[i])).
                collect(Collectors.toList());
    }

    /**
     * Converts the specified (y,z)-values to a list of vectors.
     *
     * @param yzValues (y,z) values
     * @return list of vectors
     */
    public static List<Vector3d> yz(double... yzValues) {

        if (yzValues.length % 2 != 0) {
            throw new IllegalArgumentException("Number of specified values must be a multiple of 2!");
        }

        return IntStream.range(1, yzValues.length).filter(i->(i+1)%2==0)
                .mapToObj(i -> Vector3d.xy(yzValues[i - 1], yzValues[i])).
                collect(Collectors.toList());
    }

    /**
     * Converts the specified (x,y,z)-values to a list of vectors.
     *
     * @param xyzValues (x,y,z) values
     * @return list of vectors
     */
    public static List<Vector3d> xyz(double... xyzValues) {

        if (xyzValues.length % 3 != 0) {
            throw new IllegalArgumentException("Number of specified values must be a multiple of 3!");
        }

        return IntStream.range(2, xyzValues.length).filter(i->(i+1)%3==0)
                .mapToObj(i -> Vector3d.xyz(xyzValues[i - 2], xyzValues[i - 1], xyzValues[i])).
                collect(Collectors.toList());
    }
}
