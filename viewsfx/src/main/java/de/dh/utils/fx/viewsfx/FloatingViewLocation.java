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

import de.dh.utils.fx.viewsfx.utils.JavaFXUtils;
import javafx.geometry.Bounds;
import javafx.stage.Window;

public class FloatingViewLocation extends AbstractDockableViewLocation {
    protected final Bounds mFloatingArea;
    protected final Window mOwnerWindow;

    public FloatingViewLocation(Bounds floatingArea, Window ownerWindow) {
        mFloatingArea = floatingArea;
        mOwnerWindow = ownerWindow;
    }

    public Bounds getFloatingArea() {
        return mFloatingArea;
    }

    public Window getOwnerWindow() {
        return mOwnerWindow;
    }

    @Override
    public AbstractDockableViewLocationDescriptor createDescriptor() {
        return new FloatingViewLocationDescriptor(mFloatingArea);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " for floating area " + JavaFXUtils.toString2D(mFloatingArea) + " and window " + mOwnerWindow;
    }
}
