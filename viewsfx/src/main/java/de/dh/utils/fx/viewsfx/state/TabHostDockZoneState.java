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
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public final class TabHostDockZoneState extends AbstractDockZoneState {
    protected String mTabDockHostId = null;
    protected List<ViewDockState> mViewDockState = new ArrayList<>();

    /**
     * Gets the ID of the tab dock host. Don't mix up with the ID of the dock zone which is
     * also assigned to a tab dock host.
     */
    @XmlElement(name = "TabDockHostId")
    public String getTabDockHostId() {
        return mTabDockHostId;
    }

    public void setTabDockHostId(String value) {
        mTabDockHostId = value;
    }

    @XmlElementWrapper(name = "ViewDockStates")
    @XmlElement(name = "View")
    public List<ViewDockState> getViewDockStates() {
        return mViewDockState;
    }
}
