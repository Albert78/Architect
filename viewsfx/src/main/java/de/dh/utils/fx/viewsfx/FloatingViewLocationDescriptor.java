package de.dh.utils.fx.viewsfx;

import javafx.geometry.Bounds;

public class FloatingViewLocationDescriptor extends AbstractDockableViewLocationDescriptor {
    protected final Bounds mFloatingArea;

    public FloatingViewLocationDescriptor(Bounds floatingArea) {
        mFloatingArea = floatingArea;
    }

    public Bounds getFloatingArea() {
        return mFloatingArea;
    }
}
