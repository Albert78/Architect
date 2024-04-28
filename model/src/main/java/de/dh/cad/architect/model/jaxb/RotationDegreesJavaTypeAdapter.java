/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021 - 2023  Daniel Höh
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class RotationDegreesJavaTypeAdapter extends XmlAdapter<RotationDegreesJavaTypeAdapter.RotationDegreesProxy, Float> {
    public static class RotationDegreesProxy {
        protected float mValue;

        public RotationDegreesProxy() {
            // For JAXB
        }

        public RotationDegreesProxy(float value) {
            mValue = value;
        }

        @XmlAttribute(name = "value")
        public String getValue() {
            return mValue + " deg";
        }

        public void setValue(String value) {
            String[] tokens = value.split(" ");
            if (tokens.length == 1) {
                // Don't parse unit
            } else if (tokens.length == 2) {
                String unit = tokens[1];
                if (!"deg".equals(unit)) {
                    throw new RuntimeException("Unit '" + unit + "' not supported in angle string '" + value + "'. Expected 'deg'");
                }
            }
            String valueStr = tokens[0];
            try {
                mValue = Float.parseFloat(valueStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Unable to parse angle string '" + value + "'; expected a string like '22.5 deg'");
            }
        }

        public static RotationDegreesProxy from(Float v) {
            return new RotationDegreesProxy(v);
        }

        public Float toDegValue() {
            return mValue;
        }
    }

    @Override
    public Float unmarshal(RotationDegreesProxy v) throws Exception {
        return v.toDegValue();
    }

    @Override
    public RotationDegreesProxy marshal(Float v) throws Exception {
        return RotationDegreesProxy.from(v);
    }
}
