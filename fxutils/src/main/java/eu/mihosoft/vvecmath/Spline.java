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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.mihosoft.vvecmath;

import java.util.Collection;
import java.util.List;

/**
 * Spline base class for 1, 2 and 3 dimensional spline curves.
 *
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
abstract class Spline {

    /**
     * This is a highly improved version of the following code: based on
     * http://www.java-gaming.org/index.php?topic=9830.20
     *
     * Does not rely on reflection for switching vector components based on
     * dimension
     *
     * @param valueCollection
     * @param coordIndex
     * @param cubicCollection
     */
    public void calcNaturalCubic(List<Vector3d> valueCollection, int coordIndex,
            Collection<Cubic> cubicCollection) {
        
        int num = valueCollection.size() - 1;

        double[] gamma = new double[num + 1];
        double[] delta = new double[num + 1];
        double[] D = new double[num + 1];

        int i;
        /*
           We solve the equation
          [2 1       ] [D[0]]   [3(x[1] - x[0])  ]
          |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
          |  1 4 1   | | .  | = |      .         |
          |    ..... | | .  |   |      .         |
          |     1 4 1| | .  |   |3(x[n] - x[n-2])|
          [       1 2] [D[n]]   [3(x[n] - x[n-1])]
          
          by using row operations to convert the matrix to upper triangular
          and then back sustitution.  The D[i] are the derivatives at the knots.
         */
        gamma[0] = 1.0f / 2.0f;
        for (i = 1; i < num; i++) {
            gamma[i] = 1.0f / (4.0f - gamma[i - 1]);
        }
        gamma[num] = 1.0f / (2.0f - gamma[num - 1]);

        double p0 = getByIndex(valueCollection.get(0), coordIndex);
        double p1 = getByIndex(valueCollection.get(1), coordIndex);

        delta[0] = 3.0f * (p1 - p0) * gamma[0];
        for (i = 1; i < num; i++) {
            p0 = getByIndex(valueCollection.get(i - 1), coordIndex);
            p1 = getByIndex(valueCollection.get(i + 1), coordIndex);
            delta[i] = (3.0f * (p1 - p0) - delta[i - 1]) * gamma[i];
        }
        p0 = getByIndex(valueCollection.get(num - 1), coordIndex);
        p1 = getByIndex(valueCollection.get(num), coordIndex);

        delta[num] = (3.0f * (p1 - p0) - delta[num - 1]) * gamma[num];

        D[num] = delta[num];
        for (i = num - 1; i >= 0; i--) {
            D[i] = delta[i] - gamma[i] * D[i + 1];
        }

        /*
           now compute the coefficients of the cubics 
         */
        cubicCollection.clear();

        for (i = 0; i < num; i++) {
            p0 = getByIndex(valueCollection.get(i), coordIndex);
            p1 = getByIndex(valueCollection.get(i + 1), coordIndex);

            cubicCollection.add(new Cubic(
                    p0,
                    D[i],
                    3 * (p1 - p0) - 2 * D[i] - D[i + 1],
                    2 * (p0 - p1) + D[i] + D[i + 1]
            )
            );
        }
    }

    private double getByIndex(Vector3d v, int index) {;
        switch (index) {
            case 0:
                return v.x();
            case 1:
                return v.y();
            case 2:
                return v.z();
        }

        throw new RuntimeException("Illegal index specified: " + index);
    }

    static class Cubic {

        private final double a;
        private final double b;

        private final double c;
        private final double d;

        public Cubic(double a, double b, double c, double d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        public double eval(double u) {
            return (((d * u) + c) * u + b) * u + a;
        }
    }
}
