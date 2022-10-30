package de.dh.utils.fx.viewsfx.io;

import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso({
    TabHostDockZoneSettings.class,
    SplitterDockZoneSettings.class
})
public abstract sealed class AbstractDockZoneSettings permits TabHostDockZoneSettings, SplitterDockZoneSettings {
    // Abstract super class
}
