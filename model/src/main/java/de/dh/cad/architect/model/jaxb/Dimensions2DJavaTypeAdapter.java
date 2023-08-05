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

import java.text.ParseException;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;

public class Dimensions2DJavaTypeAdapter extends XmlAdapter<Dimensions2DJavaTypeAdapter.Dimensions2DProxy, Dimensions2D> {
    public static class Dimensions2DProxy {
        protected Length mX;
        protected Length mY;

        public Dimensions2DProxy() {
            // For JAXB
        }

        public Dimensions2DProxy(Length x, Length y) {
            mX = x;
            mY = y;
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

        public static Dimensions2DProxy from(Dimensions2D v) {
            return v == null ? null : new Dimensions2DProxy(v.getX(), v.getY());
        }

        public Dimensions2D toDimensions2D() {
            return new Dimensions2D(mX, mY);
        }
    }

    @Override
    public Dimensions2D unmarshal(Dimensions2DProxy v) throws Exception {
        return v.toDimensions2D();
    }

    @Override
    public Dimensions2DProxy marshal(Dimensions2D v) throws Exception {
        return Dimensions2DProxy.from(v);
    }
}
