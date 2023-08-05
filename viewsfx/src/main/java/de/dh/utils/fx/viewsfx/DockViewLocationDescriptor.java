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

/**
 * Recipe which describes a dock location and, if it doesn't exist yet, where to create it.
 *
 * Since the system doesn't allow split dock zones with just one single child, on a given application state,
 * a dock location can only be created one step further than the last existing dock zone.
 *
 * The identification / creation process for a dock host according to this recipe is like this:
 *
 * If the tab dock host with the given dock host id already exists, return this.
 * Else take the last {@link DockZoneSplitting} whose dock zone already exist, split that
 * dock zone and create the new tab dock host on the given side.
 */
public class DockViewLocationDescriptor extends AbstractDockableViewLocationDescriptor {
    /**
     * Iteratively executed dock zone split to come to the desired location.
     */
    public static class DockZoneSplitting {
        protected final String mDockZoneId; // Where to start
        protected final DockSide mSideOfNewDockZone; // where to create new zone
        protected final double mDividerPosition;

        public DockZoneSplitting(String dockZoneId, DockSide sideOfNewDockZone, double dividerPosition) {
            mDockZoneId = dockZoneId;
            mSideOfNewDockZone = sideOfNewDockZone;
            mDividerPosition = dividerPosition;
        }

        public String getDockZoneId() {
            return mDockZoneId;
        }

        public DockSide getSideOfNewDockZone() {
            return mSideOfNewDockZone;
        }

        public double getDividerPosition() {
            return mDividerPosition;
        }
    }

    /**
     * Builder for a path to the desired dock zone location.
     */
    public static class DockZonePathBuilder {
        protected final String mDockAreaId;
        protected final List<DockZoneSplitting> mDockLocationBuildSteps = new ArrayList<>();
        protected String mCurrentDockZoneId;

        public DockZonePathBuilder(String dockAreaId, String baseDockZoneId) {
            mDockAreaId = dockAreaId;
            mCurrentDockZoneId = baseDockZoneId;
        }

        public DockZonePathBuilder declareDockZoneSplit(DockSide sideOfNewDockZone, String newDockZoneId, double dividerPosition) {
            mDockLocationBuildSteps.add(new DockZoneSplitting(mCurrentDockZoneId, sideOfNewDockZone, dividerPosition));
            mCurrentDockZoneId = newDockZoneId;
            return this;
        }

        public DockViewLocationDescriptor declareDockHostLocation(String tabDockHostId) {
            return new DockViewLocationDescriptor(mDockAreaId, tabDockHostId, mCurrentDockZoneId, mDockLocationBuildSteps);
        }
    }

    protected final String mDockAreaId; // ID of the docking area
    protected final List<DockZoneSplitting> mDockLocationBuildSteps; // Contains path to the position where to build new dock host, if it doesn't exist yet
    protected final String mNewDockZoneId; // Zone ID of new tab dock host, if created
    protected final String mTabDockHostId; // Host ID of tab dock host

    public DockViewLocationDescriptor(String dockAreaId, String tabDockHostId, String newDockZoneId, List<DockZoneSplitting> dockLocationBuildSteps) {
        mDockAreaId = dockAreaId;
        mTabDockHostId = tabDockHostId;
        mNewDockZoneId = newDockZoneId;
        mDockLocationBuildSteps = dockLocationBuildSteps;
    }

    public static DockZonePathBuilder startingAt(String dockHostId, String baseDockZoneId) {
        return new DockZonePathBuilder(dockHostId, baseDockZoneId);
    }

    public String getDockAreaId() {
        return mDockAreaId;
    }

    public String getTabDockHostId() {
        return mTabDockHostId;
    }

    public String getNewDockZoneId() {
        return mNewDockZoneId;
    }

    public List<DockZoneSplitting> getDockLocationBuildSteps() {
        return mDockLocationBuildSteps;
    }
}
