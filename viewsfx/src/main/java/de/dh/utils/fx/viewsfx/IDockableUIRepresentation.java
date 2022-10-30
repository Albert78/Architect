package de.dh.utils.fx.viewsfx;

import de.dh.utils.fx.viewsfx.TabDockHost.DockableTabControl;
import javafx.scene.Node;

/**
 * UI control representing a {@link Dockable} in its current state.
 * If the underlaying dockable is docked, this is a {@link DockableTabControl}, if it's floating, this is a {@link DockableFloatingStage}.
 */
public interface IDockableUIRepresentation {
    /**
     * Disposes this ui representation for the underlaying dockable, releasing the dockable's UI control to be migrated to another dockable UI representation.
     * The former dock host will be cleaned up after this call and thus be in a consistent state.
     */
    Node dispose();
}
