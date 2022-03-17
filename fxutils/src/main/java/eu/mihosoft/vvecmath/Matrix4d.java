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
 *
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
class Matrix4d {

    public double m00;
    public double m01;
    public double m02;
    public double m03;
    public double m10;
    public double m11;
    public double m12;
    public double m13;
    public double m20;
    public double m21;
    public double m22;
    public double m23;
    public double m30;
    public double m31;
    public double m32;
    public double m33;

    public Matrix4d() {
    }

    public Matrix4d(double[] v) {

        this.m00 = v[0];
        this.m01 = v[1];
        this.m02 = v[2];
        this.m03 = v[3];

        this.m10 = v[4];
        this.m11 = v[5];
        this.m12 = v[6];
        this.m13 = v[7];

        this.m20 = v[8];
        this.m21 = v[9];
        this.m22 = v[10];
        this.m23 = v[11];

        this.m30 = v[12];
        this.m31 = v[13];
        this.m32 = v[14];
        this.m33 = v[15];

    }

    public void set(double[] values) {

        if(values == null || values.length == 0) {
            values = new double[16];
        }

        this.m00 = values[0];
        this.m01 = values[1];
        this.m02 = values[2];
        this.m03 = values[3];

        this.m10 = values[4];
        this.m11 = values[5];
        this.m12 = values[6];
        this.m13 = values[7];

        this.m20 = values[8];
        this.m21 = values[9];
        this.m22 = values[10];
        this.m23 = values[11];

        this.m30 = values[12];
        this.m31 = values[13];
        this.m32 = values[14];
        this.m33 = values[15];
    }

    public double[] get(double... values) {

        if(values == null || values.length == 0) {
            values = new double[16];
        }

        values[0] = this.m00;
        values[1] = this.m01;
        values[2] = this.m02;
        values[3] = this.m03;

        values[4] = this.m10;
        values[5] = this.m11;
        values[6] = this.m12;
        values[7] = this.m13;

        values[8] = this.m20;
        values[9] = this.m21;
        values[10] = this.m22;
        values[11] = this.m23;

        values[12] = this.m30;
        values[13] = this.m31;
        values[14] = this.m32;
        values[15] = this.m33;

        return values;
    }

    /**
     * Multiplies this matrix with the specified matrix.
     *
     * @param m matrix to multiply
     */
    public final void mul(Matrix4d m) {

        double m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23,
                m30, m31, m32, m33;  // vars for temp result matrix

        m00 = this.m00 * m.m00 + this.m01 * m.m10
                + this.m02 * m.m20 + this.m03 * m.m30;

        m01 = this.m00 * m.m01 + this.m01 * m.m11
                + this.m02 * m.m21 + this.m03 * m.m31;

        m02 = this.m00 * m.m02 + this.m01 * m.m12
                + this.m02 * m.m22 + this.m03 * m.m32;

        m03 = this.m00 * m.m03 + this.m01 * m.m13
                + this.m02 * m.m23 + this.m03 * m.m33;

        m10 = this.m10 * m.m00 + this.m11 * m.m10
                + this.m12 * m.m20 + this.m13 * m.m30;

        m11 = this.m10 * m.m01 + this.m11 * m.m11
                + this.m12 * m.m21 + this.m13 * m.m31;

        m12 = this.m10 * m.m02 + this.m11 * m.m12
                + this.m12 * m.m22 + this.m13 * m.m32;

        m13 = this.m10 * m.m03 + this.m11 * m.m13
                + this.m12 * m.m23 + this.m13 * m.m33;

        m20 = this.m20 * m.m00 + this.m21 * m.m10
                + this.m22 * m.m20 + this.m23 * m.m30;

        m21 = this.m20 * m.m01 + this.m21 * m.m11
                + this.m22 * m.m21 + this.m23 * m.m31;

        m22 = this.m20 * m.m02 + this.m21 * m.m12
                + this.m22 * m.m22 + this.m23 * m.m32;

        m23 = this.m20 * m.m03 + this.m21 * m.m13
                + this.m22 * m.m23 + this.m23 * m.m33;

        m30 = this.m30 * m.m00 + this.m31 * m.m10
                + this.m32 * m.m20 + this.m33 * m.m30;

        m31 = this.m30 * m.m01 + this.m31 * m.m11
                + this.m32 * m.m21 + this.m33 * m.m31;

        m32 = this.m30 * m.m02 + this.m31 * m.m12
                + this.m32 * m.m22 + this.m33 * m.m32;

        m33 = this.m30 * m.m03 + this.m31 * m.m13
                + this.m32 * m.m23 + this.m33 * m.m33;

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;

        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
    }

//    public final double getScale() {
//
//        double[] tmp_rot = new double[9];  // scratch matrix
//
//        double[] tmp_scale = new double[3];  // scratch matrix
//
//        getScaleRotate(tmp_scale, tmp_rot);
//
//        return (Matrix3d.maxOf3Values(tmp_scale));
//
//    }

//    private final void getScaleRotate(double scales[], double rots[]) {
//
//        double[] tmp = new double[9];  // scratch matrix
//
//        tmp[0] = m00;
//
//        tmp[1] = m01;
//
//        tmp[2] = m02;
//
//        tmp[3] = m10;
//
//        tmp[4] = m11;
//
//        tmp[5] = m12;
//
//        tmp[6] = m20;
//
//        tmp[7] = m21;
//
//        tmp[8] = m22;
//
//        Matrix3d.compute_svd(tmp, scales, rots);
//
//        return;
//
//    }

    public final double determinant() {

        double det;

        det = m00 * (m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32
                - m13 * m22 * m31 - m11 * m23 * m32 - m12 * m21 * m33);

        det -= m01 * (m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32
                - m13 * m22 * m30 - m10 * m23 * m32 - m12 * m20 * m33);

        det += m02 * (m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31
                - m13 * m21 * m30 - m10 * m23 * m31 - m11 * m20 * m33);

        det -= m03 * (m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31
                - m12 * m21 * m30 - m10 * m22 * m31 - m11 * m20 * m32);

        return (det);

    }

}
