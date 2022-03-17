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

import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;

public class PositionJavaTypeAdapter extends XmlAdapter<PositionJavaTypeAdapter.PositionProxy, IPosition> {
    public static class PositionProxy {
        protected Length mX;
        protected Length mY;
        protected Length mZ;

        public PositionProxy() {
            // For JAXB
        }

        public PositionProxy(Length x, Length y, Length z) {
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
            return mZ == null ? null : mZ.toTransportableString();
        }

        public void setZ(String value) {
            try {
                mZ = value == null ? null : Length.fromTransportableString(value);
            } catch (ParseException e) {
                throw new RuntimeException("Unable to parse length string '" + value + "'");
            }
        }

        public static PositionProxy from(IPosition v) {
            if (v instanceof Position3D p3d) {
                return new PositionProxy(p3d.getX(), p3d.getY(), p3d.getZ());
            } else {
                return new PositionProxy(v.getX(), v.getY(), null);
            }
        }

        public IPosition toPosition() {
            return mZ == null ? new Position2D(mX, mY) : new Position3D(mX, mY, mZ);
        }
    }

    @Override
    public IPosition unmarshal(PositionProxy v) throws Exception {
        return v.toPosition();
    }

    @Override
    public PositionProxy marshal(IPosition v) throws Exception {
        return PositionProxy.from(v);
    }
}
