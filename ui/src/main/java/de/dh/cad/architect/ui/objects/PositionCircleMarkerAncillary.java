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

import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.AncillaryPosition;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Scale;

/**
 * Paints an ancillary circle at a given position.
 */
public class PositionCircleMarkerAncillary extends Abstract2DAncillaryObject {
    protected final Circle mShape = new Circle();
    protected final Scale mScaleCompensation;

    public PositionCircleMarkerAncillary(Abstract2DView parentView) {
        super(parentView);
        configureAncillaryStroke(mShape);
        mShape.setVisible(false);
        mShape.setRadius(20);

        mScaleCompensation = addUnscaled(mShape);
        setMouseTransparent(true);
    }

    public PositionCircleMarkerAncillary create(AncillaryPosition position, ConstructionView parentView) {
        PositionCircleMarkerAncillary result = new PositionCircleMarkerAncillary(parentView);
        result.update(position);
        return result;
    }

    public void update(AncillaryPosition position) {
        if (position == null) {
            mShape.setVisible(false);
            return;
        }
        Point2D center = CoordinateUtils.positionToPoint2D(position.getPosition());
        mShape.setCenterX(center.getX());
        mShape.setCenterY(center.getY());

        mShape.setVisible(true);
        mScaleCompensation.setPivotX(center.getX());
        mScaleCompensation.setPivotY(center.getY());
    }
}
