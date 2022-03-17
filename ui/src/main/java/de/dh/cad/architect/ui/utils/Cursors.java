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
package de.dh.cad.architect.ui.utils;

import java.net.URL;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;

public class Cursors {
    protected static final URL CURSOR_FORBIDDEN = Cursors.class.getResource("CursorForbidden.png");
    protected static final int CURSOR_FORBIDDEN_HOTSPOT_X = 214;
    protected static final int CURSOR_FORBIDDEN_HOTSPOT_Y = 214;

    protected static final URL CURSOR_DOCK = Cursors.class.getResource("CursorDock3.png");
    protected static final int CURSOR_DOCK_HOTSPOT_X = 225;
    protected static final int CURSOR_DOCK_HOTSPOT_Y = 150;

    protected static final URL CURSOR_UNDOCK = Cursors.class.getResource("CursorUndock3.png");
    protected static final int CURSOR_UNDOCK_HOTSPOT_X = 225;
    protected static final int CURSOR_UNDOCK_HOTSPOT_Y = 150;

    protected static final URL CURSOR_ROTATE = Cursors.class.getResource("CursorRotate2.png");
    protected static final int CURSOR_ROTATE_HOTSPOT_X = 105;
    protected static final int CURSOR_ROTATE_HOTSPOT_Y = 60;

    protected static final URL CURSOR_CROSSHAIR = Cursors.class.getResource("CursorCrosshair.png");
    protected static final int CURSOR_CROSSHAIR_HOTSPOT_X = 64;
    protected static final int CURSOR_CROSSHAIR_HOTSPOT_Y = 64;

    protected static final URL CURSOR_PAINT = Cursors.class.getResource("CursorPaintBrush.png");
    protected static final int CURSOR_PAINT_HOTSPOT_X = 2;
    protected static final int CURSOR_PAINT_HOTSPOT_Y = 511;

    protected static final URL CURSOR_PICK_MATERIAL = Cursors.class.getResource("CursorPickMaterial.png");
    protected static final int CURSOR_PICK_MATERIAL_HOTSPOT_X = 12;
    protected static final int CURSOR_PICK_MATERIAL_HOTSPOT_Y = 150;

    protected static final URL CURSOR_RESET = Cursors.class.getResource("CursorReset.png");
    protected static final int CURSOR_RESET_HOTSPOT_X = 260;
    protected static final int CURSOR_RESET_HOTSPOT_Y = 210;

    public static Cursor createCursorForbidden() {
        return new ImageCursor(new Image(CURSOR_FORBIDDEN.toString()), CURSOR_FORBIDDEN_HOTSPOT_X, CURSOR_FORBIDDEN_HOTSPOT_Y);
    }

    public static Cursor createCursorDock() {
        return new ImageCursor(new Image(CURSOR_DOCK.toString()), CURSOR_DOCK_HOTSPOT_X, CURSOR_DOCK_HOTSPOT_Y);
    }

    public static Cursor createCursorUndock() {
        return new ImageCursor(new Image(CURSOR_UNDOCK.toString()), CURSOR_UNDOCK_HOTSPOT_X, CURSOR_UNDOCK_HOTSPOT_Y);
    }

    public static Cursor createCursorRotate() {
        return new ImageCursor(new Image(CURSOR_ROTATE.toString()), CURSOR_ROTATE_HOTSPOT_X, CURSOR_ROTATE_HOTSPOT_Y);
    }

    public static Cursor createCursorCrossHair() {
        return new ImageCursor(new Image(CURSOR_CROSSHAIR.toString()), CURSOR_CROSSHAIR_HOTSPOT_X, CURSOR_CROSSHAIR_HOTSPOT_Y);
    }

    public static Cursor createCursorPaint() {
        return new ImageCursor(new Image(CURSOR_PAINT.toString()), CURSOR_PAINT_HOTSPOT_X, CURSOR_PAINT_HOTSPOT_Y);
    }

    public static Cursor createCursorPickMaterial() {
        return new ImageCursor(new Image(CURSOR_PICK_MATERIAL.toString()), CURSOR_PICK_MATERIAL_HOTSPOT_X, CURSOR_PICK_MATERIAL_HOTSPOT_Y);
    }

    public static Cursor createCursorReset() {
        return new ImageCursor(new Image(CURSOR_RESET.toString()), CURSOR_RESET_HOTSPOT_X, CURSOR_RESET_HOTSPOT_Y);
    }
}
