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
package de.dh.cad.architect.ui.objects;

import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.utils.IdGenerator;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class Abstract3DAncillaryObject extends Node {
    public static Color ANCILLARY_OBJECTS_COLOR = Color.LIGHTBLUE;

    protected final Abstract2DView mParentView;

    protected Abstract3DAncillaryObject(Abstract2DView parentView) {
        mParentView = parentView;
        setId(IdGenerator.generateUniqueId("Ancillary"));
    }

    public UiController getUiController() {
        return mParentView.getUiController();
    }

    public String getAncillaryObjectId() {
        return getId();
    }

    public void removeFromView() {
        mParentView.removeAncillaryObject(getAncillaryObjectId());
    }
}
