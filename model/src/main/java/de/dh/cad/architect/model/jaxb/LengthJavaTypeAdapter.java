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

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.coords.Length;

public class LengthJavaTypeAdapter extends XmlAdapter<LengthJavaTypeAdapter.LengthProxy, Length> {
    public static class LengthProxy {
        protected String mVal;

        public LengthProxy() {
            // For JAXB
        }

        public LengthProxy(String value) {
            mVal = value;
        }

        @XmlAttribute(name = "value")
        public String getValue() {
            return mVal;
        }

        public void setValue(String value) {
            mVal = value;
        }

        public static LengthProxy from(Length v) {
            return v == null ? null : new LengthProxy(v.toTransportableString());
        }

        public Length toLength() {
            if (StringUtils.isEmpty(mVal)) {
                return null;
            }
            try {
                return Length.fromTransportableString(mVal);
            } catch (ParseException e) {
                throw new RuntimeException("Unable to parse length string '" + mVal + "'");
            }
        }
    }

    @Override
    public Length unmarshal(LengthProxy v) throws Exception {
        return v.toLength();
    }

    @Override
    public LengthProxy marshal(Length v) throws Exception {
        return LengthProxy.from(v);
    }
}
