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
package de.dh.cad.architect.ui.view.construction.feedback.polygon;

import java.util.List;
import java.util.stream.Collectors;

import de.dh.cad.architect.ui.objects.BasePolylineShapeAncillary;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.AncillaryPosition;
import javafx.scene.shape.StrokeType;

public class AddPolylineShapeVisualFeedbackManager {
    protected final ConstructionView mView;
    protected BasePolylineShapeAncillary mPolylineAncillaryObject = null;

    public AddPolylineShapeVisualFeedbackManager(ConstructionView view) {
        mView = view;
    }

    public BasePolylineShapeAncillary getPolylineShape() {
        return mPolylineAncillaryObject;
    }

    public void updateVisualObjects(List<AncillaryPosition> positions) {
        if (positions == null) {
            removeVisualObjects();
        } else {
            if (mPolylineAncillaryObject == null) {
                mPolylineAncillaryObject = new BasePolylineShapeAncillary(true, mView);
                mView.addAncillaryObject(mPolylineAncillaryObject);
            }
            mPolylineAncillaryObject.updatePositions(positions
                .stream()
                .map(AncillaryPosition::getPosition)
                .collect(Collectors.toList()), StrokeType.INSIDE);
        }
    }

    public void removeVisualObjects() {
        if (mPolylineAncillaryObject != null) {
            mView.removeAncillaryObject(mPolylineAncillaryObject.getAncillaryObjectId());
            mPolylineAncillaryObject = null;
        }
    }
}
