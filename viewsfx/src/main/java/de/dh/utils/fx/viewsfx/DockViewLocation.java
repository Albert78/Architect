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
package de.dh.utils.fx.viewsfx;

import java.util.ArrayList;
import java.util.List;

import de.dh.utils.fx.viewsfx.DockViewLocationDescriptor.DockZoneSplitting;
import de.dh.utils.fx.viewsfx.utils.SashUtils;
import javafx.scene.Node;

public class DockViewLocation extends AbstractDockableViewLocation {
    protected final TabDockHost mTabDockHost;

    public DockViewLocation(TabDockHost tabDockHost) {
        mTabDockHost = tabDockHost;
    }

    public TabDockHost getTabDockHost() {
        return mTabDockHost;
    }

    @Override
    public AbstractDockableViewLocationDescriptor createDescriptor() {
        IDockZone currentDockZone = mTabDockHost;
        SashDockHost parentDockHost = mTabDockHost.getParentDockHost();
        List<DockZoneSplitting> dockLocationBuildSteps = new ArrayList<>();
        while (parentDockHost != null) {
            dockLocationBuildSteps.add(0, new DockZoneSplitting(parentDockHost.getDockZoneId(),
                SashUtils.getDockSideOfItem(parentDockHost.getSash(), (Node) currentDockZone)
                    .orElseThrow(() -> new IllegalStateException("Invalid state of dock host")),
                parentDockHost.getDividerPosition()));
            currentDockZone = parentDockHost;
            parentDockHost = parentDockHost.getParentDockHost();
        }
        return new DockViewLocationDescriptor(mTabDockHost.getDockAreaId(), mTabDockHost.getTabDockHostId(), mTabDockHost.getDockZoneId(), dockLocationBuildSteps);
    }
}
