/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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
package de.dh.utils.fx;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

/**
 * Generic class supporting mouse operations on a node by storing related mouse handlers, e.g. for a combined
 * mouse enter-move-press-drag-release-exit operation.
 */
public class MouseHandlerContext {
    protected final EventHandler<MouseEvent> mMousePressedHandler;
    protected final EventHandler<MouseEvent> mMouseReleasedHandler;
    protected final EventHandler<MouseEvent> mMouseMovedHandler;
    protected final EventHandler<MouseEvent> mMouseDraggedHandler;
    protected final EventHandler<MouseEvent> mMouseEnteredHandler;
    protected final EventHandler<MouseEvent> mMouseExitedHandler;

    public MouseHandlerContext(EventHandler<MouseEvent> mousePressedHandler, EventHandler<MouseEvent> mouseReleasedHandler, EventHandler<MouseEvent> mouseMovedHandler,
        EventHandler<MouseEvent> mouseDraggedHandler, EventHandler<MouseEvent> mouseEnteredHandler, EventHandler<MouseEvent> mouseExitedHandler) {
        mMousePressedHandler = mousePressedHandler;
        mMouseReleasedHandler = mouseReleasedHandler;
        mMouseMovedHandler = mouseMovedHandler;
        mMouseDraggedHandler = mouseDraggedHandler;
        mMouseEnteredHandler = mouseEnteredHandler;
        mMouseExitedHandler = mouseExitedHandler;
    }

    public MouseHandlerContext install(Node node) {
        if (mMousePressedHandler != null) {
            node.addEventHandler(MouseEvent.MOUSE_PRESSED, mMousePressedHandler);
        }
        if (mMouseReleasedHandler != null) {
            node.addEventHandler(MouseEvent.MOUSE_RELEASED, mMouseReleasedHandler);
        }
        if (mMouseMovedHandler != null) {
            node.addEventHandler(MouseEvent.MOUSE_MOVED, mMouseMovedHandler);
        }
        if (mMouseDraggedHandler != null) {
            node.addEventHandler(MouseEvent.MOUSE_DRAGGED, mMouseDraggedHandler);
        }
        if (mMouseEnteredHandler != null) {
            node.addEventHandler(MouseEvent.MOUSE_ENTERED, mMouseEnteredHandler);
        }
        if (mMouseExitedHandler != null) {
            node.addEventHandler(MouseEvent.MOUSE_EXITED, mMouseExitedHandler);
        }
        return this;
    }

    public MouseHandlerContext uninstall(Node node) {
        if (mMousePressedHandler != null) {
            node.removeEventHandler(MouseEvent.MOUSE_PRESSED, mMousePressedHandler);
        }
        if (mMouseReleasedHandler != null) {
            node.removeEventHandler(MouseEvent.MOUSE_RELEASED, mMouseReleasedHandler);
        }
        if (mMouseMovedHandler != null) {
            node.removeEventHandler(MouseEvent.MOUSE_MOVED, mMouseMovedHandler);
        }
        if (mMouseDraggedHandler != null) {
            node.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mMouseDraggedHandler);
        }
        if (mMouseEnteredHandler != null) {
            node.removeEventHandler(MouseEvent.MOUSE_ENTERED, mMouseEnteredHandler);
        }
        if (mMouseExitedHandler != null) {
            node.removeEventHandler(MouseEvent.MOUSE_EXITED, mMouseExitedHandler);
        }
        return this;
    }
}
