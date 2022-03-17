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

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.wall.Angle;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

public class ReferenceAngleConstructionAncillary extends Abstract2DAncillaryObject {
    protected static final double LINE_OPPOSITE_LENGTH = 10;
    protected static final double LINE_LENGTH = 50;

    protected final Line mLine = new Line();
    protected final Scale mLineScale;
    protected final Rotate mLineRotate = new Rotate();

    public ReferenceAngleConstructionAncillary(ConstructionView parentView) {
        super(parentView);
        mLine.setStroke(ANCILLARY_FEEDBACK_OBJECTS_COLOR);
        mLine.setStrokeWidth(3);
        mLine.getStrokeDashArray().setAll(5d, 10d, 15d, 10d);
        mLine.getTransforms().setAll(mLineRotate);
        mLineScale = addUnscaled(mLine);

        setMouseTransparent(true);

        setViewOrder(Constants.VIEW_ORDER_ANCILLARY + Constants.VIEW_ORDER_OFFSET_FOCUSED - 1); // More in foreground than normal ancillary objects and more than focused objects
    }

    public void update(Position2D centerPosition, Angle angle) {
        Point2D centerPoint = CoordinateUtils.positionToPoint2D(centerPosition);
        double centerX = centerPoint.getX();
        double centerY = centerPoint.getY();
        mLineScale.setPivotX(centerX);
        mLineScale.setPivotY(centerY);

        double angleDeg = angle.getAngleDeg();

        mLineScale.setPivotX(centerPoint.getX());
        mLineScale.setPivotY(centerPoint.getY());
        mLine.setStartX(centerPoint.getX() - LINE_OPPOSITE_LENGTH);
        mLine.setStartY(centerPoint.getY());
        mLine.setEndX(centerPoint.getX() + LINE_LENGTH);
        mLine.setEndY(centerPoint.getY());
        mLineRotate.setPivotX(centerPoint.getX());
        mLineRotate.setPivotY(centerPoint.getY());
        mLineRotate.setAngle(angleDeg);
    }
}
