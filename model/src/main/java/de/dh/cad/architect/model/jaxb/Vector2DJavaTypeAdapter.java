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

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Vector2D;

public class Vector2DJavaTypeAdapter extends XmlAdapter<Vector2DJavaTypeAdapter.Vector2DProxy, Vector2D> {
    public static class Vector2DProxy {
        protected Length mX;
        protected Length mY;

        public Vector2DProxy() {
            // For JAXB
        }

        public Vector2DProxy(Length x, Length y) {
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
                throw new RuntimeException("Unable to parse length string '" + value + "'", e);
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
                throw new RuntimeException("Unable to parse length string '" + value + "'", e);
            }
        }

        public static Vector2DProxy from(Vector2D v) {
            return v == null ? null : new Vector2DProxy(v.getX(), v.getY());
        }

        public Vector2D toVector2D() {
            return new Vector2D(mX, mY);
        }
    }

    @Override
    public Vector2D unmarshal(Vector2DProxy v) throws Exception {
        return v.toVector2D();
    }

    @Override
    public Vector2DProxy marshal(Vector2D v) throws Exception {
        return Vector2DProxy.from(v);
    }
}
