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
package de.dh.utils.fx.viewsfx.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.utils.fx.viewsfx.io.NamedDockLayoutsTypeAdapter;

@XmlRootElement(name = "ViewsLayoutState")
public class ViewsLayoutState {
    protected List<FloatingViewState> mFloatingWindowStates = new ArrayList<>();
    protected Map<String, AbstractDockZoneState> mDockLayouts = new HashMap<>();

    public ViewsLayoutState() {
        // For JAXB
    }

    public ViewsLayoutState(Map<String, AbstractDockZoneState> dockLayouts, Collection<FloatingViewState> floatingViewStates) {
        mDockLayouts.putAll(dockLayouts);
        mFloatingWindowStates.addAll(floatingViewStates);
    }

    @XmlElementWrapper(name = "FloatingViews")
    @XmlElement(name = "View")
    public List<FloatingViewState> getFloatingWindowStates() {
        return mFloatingWindowStates;
    }

    @XmlElement(name = "DockLayouts")
    @XmlJavaTypeAdapter(value = NamedDockLayoutsTypeAdapter.class)
    public Map<String, AbstractDockZoneState> getDockLayouts() {
        return mDockLayouts;
    }

    public void setDockLayouts(Map<String, AbstractDockZoneState> value) {
        mDockLayouts = value;
    }
}
