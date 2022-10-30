package de.dh.utils.fx.viewsfx.io;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public final class SplitterDockZoneSettings extends AbstractDockZoneSettings {
    public static enum Orientation {
        Horizontal, Vertical;
    }

    protected AbstractDockZoneSettings mZoneSettingsA;
    protected AbstractDockZoneSettings mZoneSettingsB;
    protected Orientation mOrientation;
    protected double mDividerPosition;

    @XmlElement(name = "ZoneA")
    public AbstractDockZoneSettings getZoneSettingsA() {
        return mZoneSettingsA;
    }

    public void setZoneSettingsA(AbstractDockZoneSettings value) {
        mZoneSettingsA = value;
    }

    @XmlElement(name = "ZoneB")
    public AbstractDockZoneSettings getZoneSettingsB() {
        return mZoneSettingsB;
    }

    public void setZoneSettingsB(AbstractDockZoneSettings value) {
        mZoneSettingsB = value;
    }

    @XmlAttribute(name = "orientation")
    public Orientation getOrientation() {
        return mOrientation;
    }

    public void setOrientation(Orientation value) {
        mOrientation = value;
    }

    @XmlElement(name = "DividerPosition")
    public double getDividerPosition() {
        return mDividerPosition;
    }

    public void setDividerPosition(double value) {
        mDividerPosition = value;
    }
}
