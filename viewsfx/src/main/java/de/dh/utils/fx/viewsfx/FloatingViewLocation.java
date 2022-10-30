package de.dh.utils.fx.viewsfx;

import javafx.geometry.Bounds;
import javafx.stage.Window;

public class FloatingViewLocation extends AbstractDockableViewLocation {
    protected final Bounds mFloatingArea;
    protected final Window mOwnerWindow;

    public FloatingViewLocation(Bounds floatingArea, Window ownerWindow) {
        mFloatingArea = floatingArea;
        mOwnerWindow = ownerWindow;
    }

    public Bounds getFloatingArea() {
        return mFloatingArea;
    }

    public Window getOwnerWindow() {
        return mOwnerWindow;
    }

    @Override
    public AbstractDockableViewLocationDescriptor toDescriptor() {
        return new FloatingViewLocationDescriptor(mFloatingArea);
    }
}
