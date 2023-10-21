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
package de.dh.cad.architect.ui.objects;

import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.utils.IdGenerator;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

public class Abstract2DAncillaryObject extends Abstract2DUiObject {
    public static Color ANCILLARY_OBJECTS_COLOR = Color.LIGHTBLUE;
    public static Color ANCILLARY_FEEDBACK_OBJECTS_COLOR = Color.DARKBLUE;

    public static int ANCILLARY_OBJECTS_DEFAULT_STROKE_WIDTH = 5;

    protected Abstract2DAncillaryObject(Abstract2DView parentView) {
        super(parentView);
        setId(IdGenerator.generateUniqueId("Ancillary"));
    }

    public String getAncillaryObjectId() {
        return getId();
    }

    protected void configureAncillaryStroke(Shape shape) {
        shape.setStroke(ANCILLARY_OBJECTS_COLOR);
        shape.setStrokeWidth(ANCILLARY_OBJECTS_DEFAULT_STROKE_WIDTH);
        shape.setFill(null);
        shape.setStrokeType(StrokeType.CENTERED);
    }

    public void removeFromView() {
        mParentView.removeAncillaryObject(getAncillaryObjectId());
    }
}
