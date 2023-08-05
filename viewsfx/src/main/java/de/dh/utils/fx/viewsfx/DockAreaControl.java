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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.dh.utils.fx.sash.SashEx;
import de.dh.utils.fx.viewsfx.DockViewLocationDescriptor.DockZoneSplitting;
import de.dh.utils.fx.viewsfx.state.AbstractDockZoneState;
import de.dh.utils.fx.viewsfx.utils.SashUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/**
 * Root area where dockable controls can be docked.
 *
 * The application will embed this control into it's node structure to create a dockable area.
 *
 * If no dockable's are docked in this container, it will contain a single empty tab dock host.
 * Depending on the dock situation, there are zero or more sash dock hosts, containing the tab dock host controls.
 */
public class DockAreaControl extends BorderPane implements IDockZoneParent {
    protected IDockZone mRootDockHost;
    protected Map<String, AbstractDockableViewLocationDescriptor> mTabDockHostLocationDescriptors;

    protected DockAreaControl(String dockAreaId) {
        setId(dockAreaId);
    }

    public static DockAreaControl create(String dockAreaId) {
        DockAreaControl result = new DockAreaControl(dockAreaId);

        TabDockHost tdh = TabDockHost.create(result);
        result.initialize(tdh);
        DockSystem.getDockAreaControlsRegistry().put(dockAreaId, result);
        return result;
    }

    protected void initialize(IDockZone rootDockHost) {
        mRootDockHost = rootDockHost;
        setCenter((Parent) mRootDockHost);
    }

    public AbstractDockZoneState saveLayout() {
        return mRootDockHost.saveLayout();
    }

    public void clearViews() {
        mRootDockHost.clearViews();
    }

    public void restoreLayout(AbstractDockZoneState rootDockZoneState) {
        clearViews();
        ViewsRegistry viewsRegistry = DockSystem.getViewsRegistry();
        if (rootDockZoneState != null) {
            initialize(IDockZone.restoreLayout(this, rootDockZoneState, viewsRegistry));
        }
    }

    @Override
    public String getDockAreaId() {
        return getId();
    }

    public IDockZone getRootDockHost() {
        return mRootDockHost;
    }

    public Optional<IDockZone> findDockZoneById(String dockZoneId) {
        return mRootDockHost.findDockZoneById(dockZoneId);
    }

    public Optional<TabDockHost> findTabDockHostById(String tabDockHostId) {
        return mRootDockHost.findTabDockHostById(tabDockHostId);
    }

    public Optional<TabDockHost> getOrTryCreateDockHost(DockViewLocationDescriptor location) {
        String tabDockHostId = location.getTabDockHostId();
        Optional<TabDockHost> result = findTabDockHostById(tabDockHostId);
        if (result.isPresent()) {
            return result;
        }

        List<DockZoneSplitting> dockLocationBuildSteps = location.getDockLocationBuildSteps();
        for (int i = dockLocationBuildSteps.size() - 1; i >= 0; i--) {
            DockZoneSplitting dzs = dockLocationBuildSteps.get(i);
            String dockZoneId = dzs.getDockZoneId();
            Optional<IDockZone> oZone = findDockZoneById(dockZoneId);
            if (oZone.isPresent()) {
                IDockZone zone = oZone.get();
                return Optional.of(zone.split(dzs.getSideOfNewDockZone(), tabDockHostId, location.getNewDockZoneId(), dzs.getDividerPosition()));
            }
        }
        return Optional.empty();
    }

    public TabDockHost getFirstLeaf() {
        IDockZone current = mRootDockHost;
        while (current instanceof SashDockHost sdh) {
            current = (IDockZone) sdh.getSash().getFirstChild();
        }
        return (TabDockHost) current;
    }

    @Override
    public void invalidateLeaf(TabDockHost child) {
        // Nothing to do, single tab dock host will remain empty
    }

    @Override
    public void compressDockHierarchy(SashDockHost obsoleteSash, IDockZone moveUpChild) {
        obsoleteSash.disposeAndReplace(moveUpChild);
        mRootDockHost = moveUpChild;
        moveUpChild.occupyDockZone(obsoleteSash.getDockZoneId(), this);
        setCenter((Parent) moveUpChild);
    }

    @Override
    public SashDockHost replaceWithSash(IDockZone replaceChild, String newChildDockZoneId, DockSide emptySide) {
        if (replaceChild != mRootDockHost) {
            throw new IllegalStateException("Inner node is not our root dock host");
        }
        SashDockHost result = SashDockHost.create(replaceChild.getDockZoneId(), this);
        SashEx sash = result.getSash();
        SashUtils.setSashItem(sash, emptySide.opposite(), (Node) replaceChild);
        replaceChild.occupyDockZone(newChildDockZoneId, result);
        mRootDockHost = result;
        setCenter(result);
        return result;
    }
}
