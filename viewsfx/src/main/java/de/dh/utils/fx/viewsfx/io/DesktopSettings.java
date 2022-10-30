package de.dh.utils.fx.viewsfx.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "DesktopSettings")
public class DesktopSettings {
    protected List<FloatingViewSettings> mFloatingViewsSettings = new ArrayList<>();
    protected Map<String, AbstractDockZoneSettings> mDockZoneHierarchies = new HashMap<>();

    @XmlElementWrapper(name = "FloatingViews")
    @XmlElement(name = "View")
    public List<FloatingViewSettings> getFloatingViewsSettings() {
        return mFloatingViewsSettings;
    }

    @XmlElement(name = "DockZoneHierarchies")
    @XmlJavaTypeAdapter(value = NamedDockHierarchiesTypeAdapter.class)
    public Map<String, AbstractDockZoneSettings> getDockZoneHierarchies() {
        return mDockZoneHierarchies;
    }

    public void setDockZoneHierarchies(Map<String, AbstractDockZoneSettings> value) {
        mDockZoneHierarchies = value;
    }
}
