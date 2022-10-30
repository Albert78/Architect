package de.dh.utils.fx.viewsfx;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.utils.fx.viewsfx.TabDockHost.DockableTabControl;
import de.dh.utils.fx.viewsfx.ViewsRegistry.ViewLifecycleManager;
import de.dh.utils.fx.viewsfx.io.AbstractDockZoneSettings;
import de.dh.utils.fx.viewsfx.io.SplitterDockZoneSettings;
import de.dh.utils.fx.viewsfx.io.SplitterDockZoneSettings.Orientation;
import de.dh.utils.fx.viewsfx.io.TabHostDockZoneSettings;
import de.dh.utils.fx.viewsfx.io.ViewDockSettings;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;

public sealed interface IDockZone permits TabDockHost, SplitterDockHost {
    Pane getFeedbackPane();
    IDockZone findDockZoneById(String dockZoneId);
    IDockHostParent getParentDockHost();
    void setParentDockHost(IDockHostParent value);

    static Orientation toSettingsOrientation(javafx.geometry.Orientation orientation) {
        return switch (orientation) {
        case HORIZONTAL: yield Orientation.Horizontal;
        case VERTICAL: yield Orientation.Vertical;
        };
    }

    static javafx.geometry.Orientation toSplitterOrientation(Orientation orientation) {
        return switch (orientation) {
        case Horizontal: yield javafx.geometry.Orientation.HORIZONTAL;
        case Vertical: yield javafx.geometry.Orientation.VERTICAL;
        };
    }

    default AbstractDockZoneSettings saveHierarchy() {
        if (this instanceof TabDockHost tdh) {
            TabHostDockZoneSettings result = new TabHostDockZoneSettings();
            result.setDockHostId(tdh.getDockZoneId());
            List<ViewDockSettings> viewDockSettings = result.getViewDockSettings();
            for (Tab tab : tdh.getTabs()) {
                DockableTabControl dtc = (DockableTabControl) tab;
                ViewDockSettings vds = new ViewDockSettings();
                vds.setViewId(dtc.getDockable().getViewId());
                if (tab.isSelected()) {
                    vds.setSelected(true);
                }
                Dockable<?> dockable = dtc.getDockable();
                vds.setLastFloatingWidth(dockable.getLastFloatingWidth());
                vds.setLastFloatingHeight(dockable.getLastFloatingHeight());
                viewDockSettings.add(vds);
            }
            return result;
        } else if (this instanceof SplitterDockHost sdh) {
            SplitterDockZoneSettings result = new SplitterDockZoneSettings();
            result.setOrientation(toSettingsOrientation(sdh.getOrientation()));
            result.setZoneSettingsA(sdh.getZoneA().saveHierarchy());
            result.setZoneSettingsB(sdh.getZoneB().saveHierarchy());
            result.setDividerPosition(sdh.getDividerPosition());
            return result;
        } else {
            throw new RuntimeException("Sealed class, this case is not permitted");
        }
    }

    static IDockZone restoreFromSettings(IDockHostParent parentDockHost, AbstractDockZoneSettings dockZoneSettings, ViewsRegistry viewsRegistry) {
        Logger log = LoggerFactory.getLogger(ViewsRegistry.class);
        if (dockZoneSettings instanceof TabHostDockZoneSettings tdhdzs) {
            String dockZoneId = tdhdzs.getDockHostId();
            TabDockHost result = TabDockHost.create(parentDockHost, dockZoneId);
            DockableTabControl selectedTab = null;
            for (ViewDockSettings vds : tdhdzs.getViewDockSettings()) {
                String viewId = vds.getViewId();
                ViewLifecycleManager<?> viewLifecycleManager = viewsRegistry.getViewLifecycleManager(viewId);
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
                dtc.getDockable().visibleProperty().endChange();
            }
            return result;
        } else if (dockZoneSettings instanceof SplitterDockZoneSettings sdzs) {
            SplitterDockHost result = SplitterDockHost.create(parentDockHost);
            result.setOrientation(toSplitterOrientation(sdzs.getOrientation()));
            result.setZones(
                restoreFromSettings(result, sdzs.getZoneSettingsA(), viewsRegistry),
                restoreFromSettings(result, sdzs.getZoneSettingsB(), viewsRegistry),
                sdzs.getDividerPosition());
            return result;
        } else {
            throw new IllegalArgumentException("Dock zone settings type '" + dockZoneSettings.getClass() + "' is not supported");
        }
    }

    default TabDockHost split(DockSide side, String newDockZoneId) {
        return split(side, newDockZoneId, 0.5);
    }

    default TabDockHost split(DockSide side, String newDockZoneId, double dividerPosition) {
        IDockHostParent parentDockHost = getParentDockHost();

        SplitterDockHost sdh = parentDockHost.replaceWithSplitter(this);
        setParentDockHost(sdh);
        TabDockHost result = TabDockHost.create(sdh, newDockZoneId);
        ObservableList<Node> splitterItems = sdh.getItems();

        switch (side) {
        case North:
            sdh.setOrientation(javafx.geometry.Orientation.VERTICAL);
            splitterItems.add(0, result);
            break;
        case South:
            sdh.setOrientation(javafx.geometry.Orientation.VERTICAL);
            splitterItems.add(1, result);
            break;
        case East:
            sdh.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
            splitterItems.add(1, result);
            break;
        case West:
            sdh.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
            splitterItems.add(0, result);
            break;
        default:
            throw new RuntimeException("Unsupported dock orientation " + side);
        }
        sdh.getSplitPane().setDividerPositions(dividerPosition);

        return result;
    }
}
