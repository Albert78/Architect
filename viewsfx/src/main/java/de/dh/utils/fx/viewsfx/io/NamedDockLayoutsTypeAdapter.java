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
package de.dh.utils.fx.viewsfx.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dh.utils.fx.viewsfx.io.NamedDockLayoutsTypeAdapter.NamedDockLayoutsWrapper;
import de.dh.utils.fx.viewsfx.state.AbstractDockZoneState;

public class NamedDockLayoutsTypeAdapter extends XmlAdapter<NamedDockLayoutsWrapper, Map<String, AbstractDockZoneState>> {
    public static class NamedDockLayout {
        protected String mId;
        protected AbstractDockZoneState mRoot;

        public NamedDockLayout() {
            // For JAXB
        }

        public NamedDockLayout(String id, AbstractDockZoneState hierarchyRoot) {
            mId = id;
            mRoot = hierarchyRoot;
        }

        @XmlAttribute(name = "id")
        public String getId() {
            return mId;
        }

        public void setId(String value) {
            mId = value;
        }

        @XmlElement(name = "Root")
        public AbstractDockZoneState getRoot() {
            return mRoot;
        }

        public void setRoot(AbstractDockZoneState value) {
            mRoot = value;
        }
    }

    public static class NamedDockLayoutsWrapper {
        protected List<NamedDockLayout> mDockLayouts = new ArrayList<>();

        @XmlElement(name = "DockLayout")
        public List<NamedDockLayout> getDockHierarchies() {
            return mDockLayouts;
        }

        public static NamedDockLayoutsWrapper wrap(Map<String, AbstractDockZoneState> v) {
            NamedDockLayoutsWrapper result = new NamedDockLayoutsWrapper();
            Collection<NamedDockLayout> dockHierarchies = result.getDockHierarchies();
            for (Entry<String, AbstractDockZoneState> hierarchyRoot : v.entrySet()) {
                dockHierarchies.add(new NamedDockLayout(hierarchyRoot.getKey(), hierarchyRoot.getValue()));
            }
            return result;
        }

        public Map<String, AbstractDockZoneState> unwrap() {
            Map<String, AbstractDockZoneState> result = new TreeMap<>(); // TreeMap for stable sorting
            for (NamedDockLayout hierarchyRoot : mDockLayouts) {
                result.put(hierarchyRoot.getId(), hierarchyRoot.getRoot());
            }
            return result;
        }
    }

    @Override
    public Map<String, AbstractDockZoneState> unmarshal(NamedDockLayoutsWrapper v) throws Exception {
        if (v != null) {
            return v.unwrap();
        }
        return Collections.emptyMap();
    }

    @Override
    public NamedDockLayoutsWrapper marshal(Map<String, AbstractDockZoneState> v) throws Exception {
        return NamedDockLayoutsWrapper.wrap(v);
    }
}
