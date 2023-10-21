package de.dh.utils.fx.viewsfx;

public interface IDockHostCreator {
    TabDockHost createTabDockHost(IDockZoneParent parentDockHost, String dockZoneId, String tabDockHostId);
    SashDockHost createSashDockHost(String dockZoneId, IDockZoneParent dockZoneParent);
}
