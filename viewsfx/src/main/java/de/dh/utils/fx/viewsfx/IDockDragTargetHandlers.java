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

/**
 * Event handlers to be implemented for target UI objects for a drag/dock operation.
 * The event handler methods {@link #handleDragEntered(DockDragOperation, double, double)} and {@link #handleDragOver(DockDragOperation, double, double)}
 * are expected to call {@link DockDragOperation#setCurrentDockTarget(IDockTarget, java.util.Optional)} if the given drag position represents a docking target,
 * else they should call {@link DockDragOperation#removeDockFeedback()}.
 * Handler method {@link #handleDragExited(DockDragOperation)} should call {@link DockDragOperation#removeDockFeedback()} (and clean up possible local dock feedback resources).
 *
 * {@link #handleDragDropped(DockDragOperation, double, double)} doesn't need to remove the dragging feedback because method
 * {@link #handleDragExited(DockDragOperation)} will also be called in case of a drop.
 */
public interface IDockDragTargetHandlers {
    /**
     * Called when the mouse enters the underlaying UI element while dragging a dockable.
     */
    boolean handleDragEntered(DockDragOperation operation, double dragPositionX, double dragPositionY);

    /**
     * Called when the mouse drags over the underlaying UI element while dragging a dockable.
     */
    boolean handleDragOver(DockDragOperation operation, double dragPositionX, double dragPositionY);

    /**
     * Called when the mouse leaves the underlaying UI element while dragging a dockable.
     * This method is always called when {@link #handleDragEntered(DockDragOperation, double, double)} was called before.
     */
    boolean handleDragExited(DockDragOperation operation);

    /**
     * Called when the mouse drops a currently dragged dockable on the underlaying UI element.
     */
    boolean handleDragDropped(DockDragOperation operation, double dragPositionX, double dragPositionY);
}
