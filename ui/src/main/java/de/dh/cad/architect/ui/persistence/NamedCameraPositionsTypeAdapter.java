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
package de.dh.cad.architect.ui.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dh.cad.architect.ui.persistence.NamedCameraPositionsTypeAdapter.NamedCameraPositionsWrapper;

public class NamedCameraPositionsTypeAdapter extends XmlAdapter<NamedCameraPositionsWrapper, Map<String, CameraPosition>> {
    public static class NamedCameraPosition extends CameraPosition {
        protected String mName;

        public NamedCameraPosition() {
            // For JAXB
        }

        public NamedCameraPosition(CameraPosition position, String name) {
            super(position);
            mName = name;
        }

        @XmlElement(name = "Name")
        public String getName() {
            return mName;
        }

        public void setName(String value) {
            mName = value;
        }
    }

    public static class NamedCameraPositionsWrapper {
        protected List<NamedCameraPosition> mCameraPositions = new ArrayList<>();

        @XmlElement(name = "Camera")
        public List<NamedCameraPosition> getCameraPositions() {
            return mCameraPositions;
        }

        public static NamedCameraPositionsWrapper wrap(Map<String, CameraPosition> v) {
            NamedCameraPositionsWrapper result = new NamedCameraPositionsWrapper();
            Collection<NamedCameraPosition> cameraPositions = result.getCameraPositions();
            for (Entry<String, CameraPosition> namedPosition : v.entrySet()) {
                cameraPositions.add(new NamedCameraPosition(namedPosition.getValue(), namedPosition.getKey()));
            }
            return result;
        }
    }

    @Override
    public Map<String, CameraPosition> unmarshal(NamedCameraPositionsWrapper v) throws Exception {
        Map<String, CameraPosition> result = new TreeMap<>();
        if (v != null) {
            for (NamedCameraPosition cameraPosition : v.getCameraPositions()) {
                result.put(cameraPosition.getName(), cameraPosition);
            }
        }
        return result;
    }

    @Override
    public NamedCameraPositionsWrapper marshal(Map<String, CameraPosition> v) throws Exception {
        return NamedCameraPositionsWrapper.wrap(v);
    }
}
