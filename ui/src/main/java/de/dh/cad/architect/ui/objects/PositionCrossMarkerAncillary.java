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
import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;

/**
 * Paints an ancillary cross at a given position.
 */
public class PositionCrossMarkerAncillary extends Abstract2DAncillaryObject {
    protected static final double CROSS_SIZE = 20;

    protected final Line mSlashLine = new Line();
    protected final Line mBackslashLine = new Line();
    protected final Group mCrossShapes = new Group(mSlashLine, mBackslashLine);
    protected final Scale mScaleCompensation;

    public PositionCrossMarkerAncillary(Abstract2DView parentView) {
        super(parentView);
        configureAncillaryStroke(mSlashLine);
        configureAncillaryStroke(mBackslashLine);
        mCrossShapes.setVisible(false);

        mScaleCompensation = addUnscaled(mCrossShapes);
        setMouseTransparent(true);
    }

    public PositionCrossMarkerAncillary create(AncillaryPosition position, ConstructionView parentView) {
        PositionCrossMarkerAncillary result = new PositionCrossMarkerAncillary(parentView);
        result.update(position);
        return result;
    }

    public void update(AncillaryPosition position) {
        if (position == null) {
            mCrossShapes.setVisible(false);
            return;
        }
        Point2D center = CoordinateUtils.positionToPoint2D(position.getPosition());
        double crossSize2 = CROSS_SIZE / 2;
        mSlashLine.setStartX(center.getX() - crossSize2);
        mSlashLine.setStartY(center.getY() - crossSize2);
        mSlashLine.setEndX(center.getX() + crossSize2);
        mSlashLine.setEndY(center.getY() + crossSize2);
        mBackslashLine.setStartX(center.getX() + crossSize2);
        mBackslashLine.setStartY(center.getY() - crossSize2);
        mBackslashLine.setEndX(center.getX() - crossSize2);
        mBackslashLine.setEndY(center.getY() + crossSize2);

        mCrossShapes.setVisible(true);
        mScaleCompensation.setPivotX(center.getX());
        mScaleCompensation.setPivotY(center.getY());
    }
}
