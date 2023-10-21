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

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.AncillaryPosition;
import de.dh.utils.Vector2D;
import de.dh.utils.fx.shapes.CurvedArrow;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * Paints an ancillary arrow in the form of an arc from a start to an end position.
 */
public class ConnectionArrowAncillary extends Abstract2DAncillaryObject {
    protected static final double CENTER_DIST = 500;
    protected static final double DIST_POINTS = 5;
    protected static final double ARROW_STROKE_WIDTH = 4;
    protected static final Paint ARROW_COLOR = Color.FORESTGREEN;

    protected final CurvedArrow mArrow = new CurvedArrow();

    protected Position2D mStartPosition = null;
    protected Position2D mEndPosition = null;

    public ConnectionArrowAncillary(Abstract2DView parentView) {
        super(parentView);
        configureArrow();
        mArrow.setVisible(false);

        addScaled(mArrow);
        setMouseTransparent(true);
    }

    protected void configureArrow() {
        mArrow.setColor(ARROW_COLOR);
        mArrow.setArrowCurveStrokeWidth(ARROW_STROKE_WIDTH);
    }

    public ConnectionArrowAncillary create(AncillaryPosition startPosition, AncillaryPosition endPosition, ConstructionView parentView) {
        ConnectionArrowAncillary result = new ConnectionArrowAncillary(parentView);
        result.update(startPosition, endPosition);
        return result;
    }

    public void update(AncillaryPosition startPosition, AncillaryPosition endPosition) {
        mStartPosition = startPosition == null ? null : startPosition.getPosition();
        mEndPosition = endPosition == null ? null : endPosition.getPosition();

        updateVisuals();
    }

    protected void updateVisuals() {
        if (mStartPosition == null || mEndPosition == null) {
            mArrow.setVisible(false);
            return;
        }

        mArrow.setScaleCompensation(getScaleCompensation());
        Vector2D startPos = CoordinateUtils.positionToVector2D(mStartPosition);
        Vector2D endPos = CoordinateUtils.positionToVector2D(mEndPosition);
        mArrow.setSourcePosition(startPos);
        mArrow.setTargetPosition(endPos);
        mArrow.setVisible(true);
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        updateVisuals();
    }
}
