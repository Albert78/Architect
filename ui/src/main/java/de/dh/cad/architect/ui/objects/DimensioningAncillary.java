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

import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import javafx.scene.paint.Color;

public class DimensioningAncillary extends Abstract2DAncillaryObject {
    protected final DimensioningVisual mVisual = new DimensioningVisual(this);

    protected Position2D mPosition1 = Position2D.zero();
    protected Position2D mPosition2 = Position2D.zero();
    protected double mLabelDistanceScaled = 0;
    protected double mLabelDistanceUnscaled = 20;
    protected boolean mValid = true;

    public DimensioningAncillary(Abstract2DView parentView) {
        super(parentView);
        setMouseTransparent(true);
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        updateShape();
    }

    protected void updateShape() {
        double scaleCompensation = getScaleCompensation();

        if (mValid) {
            mVisual.setProperties(Color.BLACK, false, false);
        } else {
            mVisual.setProperties(Color.RED, false, false);
        }

        mVisual.setLabelDistance(getCalculatedLabelDistance());
        mVisual.updateShape(mPosition1, mPosition2, Optional.empty(), scaleCompensation);
    }

    public double getCalculatedLabelDistance() {
        return mLabelDistanceScaled + mLabelDistanceUnscaled * getScaleCompensation();
    }

    /**
     * Sets the properties for the dimensioning visual. We support setting the label's distance to the base positions' line for two use-cases:
     * <ul>
     * <li>The label's distance will scale together with the plan's view as if the dimensioning would be drawn on the plan's paper.</li>
     * <li>The label's distance will be a constant size which will not change if the plan view scales. This is used for temporary support dimensionings during visual feedbacks.</li>
     * </ul>
     * The final label's distance is calculated as the sum of the two values, so just leave one or the other <code>0</code> or use both of them simultaneously.
     * @param pos1 Position of the first anchor point.
     * @param pos2 Position of the second anchor point.
     * @param labelDistanceScaled Scaled distance of the label which will grow and shrink as the plan view is scaled.
     * @param labelDistanceUnscaled Unscaled distance of the label which will always ve shown as the given, constant number of view pixels.
     */
    public void setProperties(Position2D pos1, Position2D pos2, double labelDistanceScaled, double labelDistanceUnscaled, boolean valid) {
        mValid = valid;
        mPosition1 = pos1;
        mPosition2 = pos2;
        mLabelDistanceScaled = labelDistanceScaled;
        mLabelDistanceUnscaled = labelDistanceUnscaled;
        updateShape();
    }
}
