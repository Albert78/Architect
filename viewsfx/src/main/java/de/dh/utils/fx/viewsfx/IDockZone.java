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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.utils.fx.sash.Sash;
import de.dh.utils.fx.sash.SashEx.PaneSide;
import de.dh.utils.fx.sash.SashEx.StateOverride;
import de.dh.utils.fx.viewsfx.TabDockHost.DockableTabControl;
import de.dh.utils.fx.viewsfx.state.AbstractDockZoneState;
import de.dh.utils.fx.viewsfx.state.SashDockZoneState;
import de.dh.utils.fx.viewsfx.state.SashDockZoneState.MaximizedZone;
import de.dh.utils.fx.viewsfx.state.SashDockZoneState.Orientation;
import de.dh.utils.fx.viewsfx.state.TabHostDockZoneState;
import de.dh.utils.fx.viewsfx.state.ViewDockState;
import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;

public sealed interface IDockZone permits TabDockHost, SashDockHost {
    /**
     * Gets the id of this dock zone. The conceptual dock zones (in contrast to the instances implementing it a given time)
     * remain stable in their dock zone hierarchy and so does their dock zone id.
     * Dock hosts (tab, sash) instances will occupy a dock zone at a given time but when the docking situation changes,
     * the dock host at a given dock zone changes. To implement that behavior, we change the dock zone ID of a dock host over time
     * to always reflect the dock zone the dock host is currently occupying.
     */
    String getDockZoneId();

    /**
     * Gets our parent - either a {@link SashDockHost} or the root {@link DockAreaControl}.
     */
    IDockZoneParent getDockZoneParent();

    default String getDockAreaId() {
        return getDockZoneParent().getDockAreaId();
    }

    /**
     * Lets this instance occupy / move to the given dock zone.
     */
    void occupyDockZone(String dockZoneId, IDockZoneParent dockZoneParent);

    boolean isAlive();
    Pane getFeedbackPane();
    Optional<IDockZone> findDockZoneById(String dockZoneId);
    Optional<TabDockHost> findTabDockHostById(String tabDockHostId);
    void clearViews();

    default SashDockHost getParentDockHost() {
        IDockZoneParent parent = getDockZoneParent();
        if (parent instanceof SashDockHost sdh) {
            return sdh;
        }
        return null;
    }

    static Orientation toSettingsOrientation(javafx.geometry.Orientation orientation) {
        return switch (orientation) {
        case HORIZONTAL: yield Orientation.Horizontal;
        case VERTICAL: yield Orientation.Vertical;
        };
    }

    static javafx.geometry.Orientation toFxOrientation(Orientation orientation) {
        return switch (orientation) {
        case Horizontal: yield javafx.geometry.Orientation.HORIZONTAL;
        case Vertical: yield javafx.geometry.Orientation.VERTICAL;
        };
    }

    default AbstractDockZoneState saveLayout() {
        if (this instanceof TabDockHost tdh) {
            TabHostDockZoneState result = new TabHostDockZoneState();
            result.setTabDockHostId(tdh.getTabDockHostId());
            result.setDockZoneId(tdh.getDockZoneId());
            List<ViewDockState> viewDockStates = result.getViewDockStates();
            for (Tab tab : tdh.getTabs()) {
                DockableTabControl dtc = (DockableTabControl) tab;
                ViewDockState vds = new ViewDockState();
                vds.setViewId(dtc.getDockable().getViewId());
                if (tab.isSelected()) {
                    vds.setSelected(true);
                }
                Dockable<?> dockable = dtc.getDockable();
                vds.setLastFloatingWidth(dockable.getLastFloatingWidth());
                vds.setLastFloatingHeight(dockable.getLastFloatingHeight());
                viewDockStates.add(vds);
            }
            return result;
        } else if (this instanceof SashDockHost sdh) {
            SashDockZoneState result = new SashDockZoneState();
            result.setDockZoneId(sdh.getDockZoneId());
            result.setOrientation(toSettingsOrientation(sdh.getOrientation()));
            result.setZoneStateA(sdh.getZoneA().saveLayout());
            result.setZoneStateB(sdh.getZoneB().saveLayout());
            Optional<StateOverride> oStateOverride = sdh.getSash().getStateOverride();
            if (oStateOverride.isEmpty()) {
                result.setDividerPosition(sdh.getDividerPosition());
            } else {
                StateOverride stateOverride = oStateOverride.get();
                result.setDividerPosition(stateOverride.getDividerPosition());
                result.setMaximizedZone(stateOverride.getMinimizedPane() == PaneSide.First ? MaximizedZone.ZoneB : MaximizedZone.ZoneA);
            }
            return result;
        } else {
            throw new RuntimeException("Sealed interface " + IDockZone.class.getSimpleName() + " doesn't permit sub class " + getClass().getSimpleName());
        }
    }

    static IDockZone restoreLayout(IDockZoneParent parentDockHost, AbstractDockZoneState dockZoneSettings,
        IViewsManager viewsManager, IDockHostCreator dockHostCreator) {
        Logger log = LoggerFactory.getLogger(ViewsRegistry.class);
        if (dockZoneSettings instanceof TabHostDockZoneState tdhdzs) {
            String dockZoneId = tdhdzs.getDockZoneId();
            String tabDockHostId = tdhdzs.getTabDockHostId();
            TabDockHost result = dockHostCreator.createTabDockHost(parentDockHost, dockZoneId, tabDockHostId);
            DockableTabControl selectedTab = null;
            for (ViewDockState vds : tdhdzs.getViewDockStates()) {
                String viewId = vds.getViewId();
                ViewLifecycleManager<?> viewLifecycleManager = viewsManager.getViewLifecycleManager(viewId);
                if (viewLifecycleManager == null) {
                    log.warn("Unable to find declaration for view '" + viewId + "', view is not present in registry");
                    continue;
                }
                Dockable<?> dockable = viewLifecycleManager.getOrCreateDockable();
                dockable.visibleProperty().beginChange();
                DockableTabControl tab = dockable.dockLast(result);
                if (vds.isSelected()) {
                    selectedTab = tab;
                }
                dockable.setLastFloatingWidth(vds.getLastFloatingWidth());
                dockable.setLastFloatingHeight(vds.getLastFloatingHeight());
            }
            result.getTabPane().getSelectionModel().select(selectedTab);
            for (Tab tab : result.getTabs()) {
                DockableTabControl dtc = (DockableTabControl) tab;
                Platform.runLater(() -> {
                    dtc.getDockable().visibleProperty().endChange();
                });
            }
            return result;
        } else if (dockZoneSettings instanceof SashDockZoneState sdzs) {
            String dockZoneId = sdzs.getDockZoneId();
            SashDockHost result = dockHostCreator.createSashDockHost(dockZoneId, parentDockHost);
            result.setOrientation(toFxOrientation(sdzs.getOrientation()));
            result.setZones(
                restoreLayout(result, sdzs.getZoneStateA(), viewsManager, dockHostCreator),
                restoreLayout(result, sdzs.getZoneStateB(), viewsManager, dockHostCreator),
                sdzs.getDividerPosition());
            // Maximize pane after setting the divider's position because the position will be
            // used for the overridden state in our sash
            switch (sdzs.getMaximizedZone()) {
            case ZoneA:
                result.getSash().maximizeFirstPane();
                break;
            case ZoneB:
                result.getSash().maximizeLastPane();
                break;
            default:
                break;
            }
            return result;
        } else {
            throw new IllegalArgumentException("Dock zone settings type '" + dockZoneSettings.getClass() + "' is not supported");
        }
    }

    default TabDockHost split(DockSide side, String newDockHostId, String newDockZoneId, IDockHostCreator dockHostCreator) {
        return split(side, newDockHostId, newDockZoneId, 0.5, dockHostCreator);
    }

    default TabDockHost split(DockSide side, String newDockHostId, String newDockZoneId, double dividerPosition, IDockHostCreator dockHostCreator) {
        IDockZoneParent dockZoneParent = getDockZoneParent();

        SashDockHost sdh = dockZoneParent.replaceWithSash(this, newDockZoneId, side, dockHostCreator);
        TabDockHost result = dockHostCreator.createTabDockHost(sdh, newDockZoneId, newDockHostId);
        Sash sash = sdh.getSash();

        switch (side) {
        case North:
            sdh.setOrientation(javafx.geometry.Orientation.VERTICAL);
            sash.setFirstChild(result);
            break;
        case South:
            sdh.setOrientation(javafx.geometry.Orientation.VERTICAL);
            sash.setLastChild(result);
            break;
        case East:
            sdh.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
            sash.setLastChild(result);
            break;
        case West:
            sdh.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
            sash.setFirstChild(result);
            break;
        default:
            throw new RuntimeException("Unsupported dock orientation " + side);
        }
        sdh.getSash().setDividerPosition(dividerPosition);

        return result;
    }
}
