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

import java.text.ParseException;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dh.cad.architect.model.coords.Dimensions3D;
import de.dh.cad.architect.model.coords.Length;

public class Dimensions3DJavaTypeAdapter extends XmlAdapter<Dimensions3DJavaTypeAdapter.Dimensions3DProxy, Dimensions3D> {
    public static class Dimensions3DProxy {
        protected Length mX;
        protected Length mY;
        protected Length mZ;

        public Dimensions3DProxy() {
            // For JAXB
        }

        public Dimensions3DProxy(Length x, Length y, Length z) {
            mX = x;
            mY = y;
            mZ = z;
        }

        @XmlAttribute(name = "x")
        public String getX() {
            return mX.toTransportableString();
        }

        public void setX(String value) {
            try {
                mX = Length.fromTransportableString(value);
            } catch (ParseException e) {
                throw new RuntimeException("Unable to parse length string '" + value + "'");
            }
        }

        @XmlAttribute(name = "y")
        public String getY() {
            return mY.toTransportableString();
        }

        public void setY(String value) {
            try {
                mY = Length.fromTransportableString(value);
            } catch (ParseException e) {
                throw new RuntimeException("Unable to parse length string '" + value + "'");
            }
        }

        @XmlAttribute(name = "z")
        public String getZ() {
            return mZ.toTransportableString();
        }

        public void setZ(String value) {
            try {
                mZ = Length.fromTransportableString(value);
            } catch (ParseException e) {
                throw new RuntimeException("Unable to parse length string '" + value + "'");
            }
        }

        public static Dimensions3DProxy from(Dimensions3D v) {
            return v == null ? null : new Dimensions3DProxy(v.getWidth(), v.getHeight(), v.getDepth());
        }

        public Dimensions3D toDimensions3D() {
            return new Dimensions3D(mX, mY, mZ);
        }
    }

    @Override
    public Dimensions3D unmarshal(Dimensions3DProxy v) throws Exception {
        return v.toDimensions3D();
    }

    @Override
    public Dimensions3DProxy marshal(Dimensions3D v) throws Exception {
        return Dimensions3DProxy.from(v);
    }
}
