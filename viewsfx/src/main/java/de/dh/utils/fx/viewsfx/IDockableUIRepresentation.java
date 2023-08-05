/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021 - 2023  Daniel Höh
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 *******************************************************************************/
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
