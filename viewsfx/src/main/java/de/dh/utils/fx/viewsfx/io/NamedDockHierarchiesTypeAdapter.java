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

import de.dh.utils.fx.viewsfx.io.NamedDockHierarchiesTypeAdapter.NamedDockHierarchiesWrapper;

public class NamedDockHierarchiesTypeAdapter extends XmlAdapter<NamedDockHierarchiesWrapper, Map<String, AbstractDockZoneSettings>> {
    public static class NamedDockHierarchy {
        protected String mId;
        protected AbstractDockZoneSettings mRoot;

        public NamedDockHierarchy() {
            // For JAXB
        }

        public NamedDockHierarchy(String id, AbstractDockZoneSettings hierarchyRoot) {
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
        public AbstractDockZoneSettings getRoot() {
            return mRoot;
        }

        public void setRoot(AbstractDockZoneSettings value) {
            mRoot = value;
        }
    }

    public static class NamedDockHierarchiesWrapper {
        protected List<NamedDockHierarchy> mDockHierarchies = new ArrayList<>();

        @XmlElement(name = "DockHierarchy")
        public List<NamedDockHierarchy> getDockHierarchies() {
            return mDockHierarchies;
        }

        public static NamedDockHierarchiesWrapper wrap(Map<String, AbstractDockZoneSettings> v) {
            NamedDockHierarchiesWrapper result = new NamedDockHierarchiesWrapper();
            Collection<NamedDockHierarchy> dockHierarchies = result.getDockHierarchies();
            for (Entry<String, AbstractDockZoneSettings> hierarchyRoot : v.entrySet()) {
                dockHierarchies.add(new NamedDockHierarchy(hierarchyRoot.getKey(), hierarchyRoot.getValue()));
            }
            return result;
        }

        public Map<String, AbstractDockZoneSettings> unwrap() {
            Map<String, AbstractDockZoneSettings> result = new TreeMap<>(); // TreeMap for stable sorting
            for (NamedDockHierarchy hierarchyRoot : mDockHierarchies) {
                result.put(hierarchyRoot.getId(), hierarchyRoot.getRoot());
            }
            return result;
        }
    }

    @Override
    public Map<String, AbstractDockZoneSettings> unmarshal(NamedDockHierarchiesWrapper v) throws Exception {
        if (v != null) {
            return v.unwrap();
        }
        return Collections.emptyMap();
    }

    @Override
    public NamedDockHierarchiesWrapper marshal(Map<String, AbstractDockZoneSettings> v) throws Exception {
        return NamedDockHierarchiesWrapper.wrap(v);
    }
}
