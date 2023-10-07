package de.dh.utils.fx;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;

public class SimpleToolTip {
    protected final Tooltip mToolTip;
    protected boolean mToolTipVisible;

    public SimpleToolTip() {
        mToolTip = new Tooltip();
        mToolTip.setHideOnEscape(true);
        mToolTip.setAutoHide(true);
        mToolTipVisible = false;
    }

    public void setToolTipText(String text) {
        mToolTip.setText(text);
    }

    public void showToolTip(Node ownerNode, double anchorX, double anchorY) {
        if (mToolTipVisible) {
            return;
        }
        mToolTipVisible = true;
        mToolTip.show(ownerNode, anchorX, anchorY);
    }

    public void hideToolTip() {
        mToolTipVisible = false;
        mToolTip.hide();
    }
}
