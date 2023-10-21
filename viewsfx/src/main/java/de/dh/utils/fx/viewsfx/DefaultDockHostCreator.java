package de.dh.utils.fx.viewsfx;

public class DefaultDockHostCreator implements IDockHostCreator {
    @Override
    public TabDockHost createTabDockHost(IDockZoneParent parentDockHost, String dockZoneId, String tabDockHostId) {
        return TabDockHost.create(parentDockHost, dockZoneId, tabDockHostId);
    }

    @Override
    public SashDockHost createSashDockHost(String dockZoneId, IDockZoneParent dockZoneParent) {
        return SashDockHost.create(dockZoneId, dockZoneParent);
    }
}
