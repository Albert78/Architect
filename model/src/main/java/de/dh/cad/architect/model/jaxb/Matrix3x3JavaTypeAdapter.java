/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 *******************************************************************************/
package de.dh.cad.architect.model.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.commons.lang3.StringUtils;

/**
 * XML type adapter to marshal a 3x3 float array to a string with 9 float numbers separated by spaces.
 * The format of the string is {@code "v00 v01 v02 v10 v11 v12 v20 v21 v22"}.
 */
public class Matrix3x3JavaTypeAdapter extends XmlAdapter<String, float[][]> {
    @Override
    public float[][] unmarshal(String matrixString) throws Exception {
        try {
            if (StringUtils.isEmpty(matrixString)) {
                return null;
            }
            String [] values = matrixString.split(" ", 9);

            if (values.length == 9) {
              return new float [][] {{Float.parseFloat(values[0]),
                                      Float.parseFloat(values[1]),
                                      Float.parseFloat(values[2])},
                                     {Float.parseFloat(values[3]),
                                      Float.parseFloat(values[4]),
                                      Float.parseFloat(values[5])},
                                     {Float.parseFloat(values[6]),
                                      Float.parseFloat(values[7]),
                                      Float.parseFloat(values[8])}};
            } else {
              return null;
            }
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public String marshal(float[][] v) throws Exception {
        return Float.toString(v[0][0])
                + " " + Float.toString(v[0][1])
                + " " + Float.toString(v[0][2])
                + " " + Float.toString(v[1][0])
                + " " + Float.toString(v[1][1])
                + " " + Float.toString(v[1][2])
                + " " + Float.toString(v[2][0])
                + " " + Float.toString(v[2][1])
                + " " + Float.toString(v[2][2]);
    }
}
