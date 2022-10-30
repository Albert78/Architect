package de.dh.utils.fx.viewsfx;

import java.util.Optional;

public interface IDockZoneProvider {
    public Optional<TabDockHost> getOrTryCreateDockZone(String dockZoneId);
}
