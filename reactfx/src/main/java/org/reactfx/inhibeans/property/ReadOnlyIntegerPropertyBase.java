package org.reactfx.inhibeans.property;

import org.reactfx.Guard;

/**
 * Inhibitory version of {@link javafx.beans.property.ReadOnlyIntegerPropertyBase}.
 */
@Deprecated
public abstract class ReadOnlyIntegerPropertyBase
extends javafx.beans.property.ReadOnlyIntegerPropertyBase
implements Property<Number> {

    private int blocked = 0;
    private boolean fireOnRelease = false;

    @Override
    public Guard block() {
        ++blocked;
        return ((Guard) this::release).closeableOnce();
    }

    private void release() {
        assert blocked > 0;
        if(--blocked == 0 && fireOnRelease) {
            fireOnRelease = false;
            super.fireValueChangedEvent();
        }
    }

    @Override
    protected void fireValueChangedEvent() {
        if(blocked > 0) {
            fireOnRelease = true;
        } else {
            super.fireValueChangedEvent();
        }
    }
}
