package de.dh.utils.fx.viewsfx.io;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class FloatingViewSettings {
    protected String mViewId = null;
    protected int mFloatingPositionX = -1;
    protected int mFloatingPositionY = -1;
    protected int mFloatingWidth = -1;
    protected int mFloatingHeight = -1;

    public FloatingViewSettings(String viewId,
        int floatingPositionX, int floatingPositionY,
        int floatingWidth, int floatingHeight) {
        mViewId = viewId;
        mFloatingPositionX = floatingPositionX;
        mFloatingPositionY = floatingPositionY;
        mFloatingWidth = floatingWidth;
        mFloatingHeight = floatingHeight;
    }

    public FloatingViewSettings() {
        // For JAXB
    }

    @XmlAttribute(name = "viewId")
    public String getViewId() {
        return mViewId;
    }

    public void setViewId(String value) {
        mViewId = value;
    }

    @XmlElement(name = "FloatingPositionX")
    public int getFloatingPositionX() {
        return mFloatingPositionX;
    }

    public void setFloatingPositionX(int value) {
        mFloatingPositionX = value;
    }

    @XmlElement(name = "FloatingPositionY")
    public int getFloatingPositionY() {
        return mFloatingPositionY;
    }

    public void setFloatingPositionY(int value) {
        mFloatingPositionY = value;
    }

    @XmlElement(name = "FloatingWidth")
    public int getFloatingWidth() {
        return mFloatingWidth;
    }

    public void setFloatingWidth(int value) {
        mFloatingWidth = value;
    }

    @XmlElement(name = "FloatingHeight")
    public int getFloatingHeight() {
        return mFloatingHeight;
    }

    public void setFloatingHeight(int value) {
        mFloatingHeight = value;
    }
}
