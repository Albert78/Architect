package de.dh.utils.fx.viewsfx.io;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public final class TabHostDockZoneSettings extends AbstractDockZoneSettings {
    protected String mDockHostId = null;
    protected List<ViewDockSettings> mViewDockSettings = new ArrayList<>();

    public String getDockHostId() {
        return mDockHostId;
    }

    @XmlElement(name = "DockHostId")
    public void setDockHostId(String value) {
        mDockHostId = value;
    }

    @XmlElementWrapper(name = "ViewDockSettings")
    @XmlElement(name = "View")
    public List<ViewDockSettings> getViewDockSettings() {
        return mViewDockSettings;
    }
}
