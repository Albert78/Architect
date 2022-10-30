package de.dh.utils.fx.viewsfx;

public class DockViewLocationDescriptor extends AbstractDockableViewLocationDescriptor {
    protected final String mDockHostId;

    public DockViewLocationDescriptor(String dockHostId) {
        mDockHostId = dockHostId;
    }

    public String getDockHostId() {
        return mDockHostId;
    }
}
