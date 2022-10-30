package de.dh.utils.fx.viewsfx;

public class DockViewLocation extends AbstractDockableViewLocation {
    protected final TabDockHost mDockHost;

    public DockViewLocation(TabDockHost dockHost) {
        mDockHost = dockHost;
    }

    public TabDockHost getDockHost() {
        return mDockHost;
    }

    @Override
    public AbstractDockableViewLocationDescriptor toDescriptor() {
        return new DockViewLocationDescriptor(mDockHost.getDockZoneId());
    }
}
