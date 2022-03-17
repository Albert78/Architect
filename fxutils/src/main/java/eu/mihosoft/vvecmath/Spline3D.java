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

import java.util.ArrayList;
import java.util.List;


/**
 * 3d Spline.
 * 
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
public final class Spline3D extends Spline {

    private final List<Vector3d> points;

    private final List<Cubic> xCubics;
    private final List<Cubic> yCubics;
    private final List<Cubic> zCubics;

    /**
     * Creates a new spline.
     */
    public Spline3D() {
        this.points = new ArrayList<>();

        this.xCubics = new ArrayList<>();
        this.yCubics = new ArrayList<>();
        this.zCubics = new ArrayList<>();

    }

    /**
     * Adds a control point to this spline.
     * @param point point to add
     */
    public void addPoint(Vector3d point) {
        this.points.add(point);
    }

    /**
     * Returns all control points.
     * @return control points
     */
    public List<Vector3d> getPoints() {
        return points;
    }

    /**
     * Calculates this spline.
     */
    public void calcSpline() {
        calcNaturalCubic(points, 0, xCubics);
        calcNaturalCubic(points, 1, yCubics);
        calcNaturalCubic(points, 2, zCubics);
    }

    /**
     * Returns a point on the spline curve.
     * @param position position on the curve, range {@code [0, 1)}
     * 
     * @return a point on the spline curve
     */
    public Vector3d getPoint(double position) {
        position = position * xCubics.size();
        int cubicNum = (int) position;
        double cubicPos = (position - cubicNum);

        return Vector3d.xyz(xCubics.get(cubicNum).eval(cubicPos),
                yCubics.get(cubicNum).eval(cubicPos),
                zCubics.get(cubicNum).eval(cubicPos));
    }
}
