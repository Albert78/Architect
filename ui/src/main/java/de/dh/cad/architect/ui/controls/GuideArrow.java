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
package de.dh.cad.architect.ui.controls;

import de.dh.cad.architect.model.objects.GuideLine;
import javafx.scene.shape.Polygon;

public class GuideArrow extends Polygon {
    protected final GuideLine mGuideLine;
    protected final AbstractRuler mParent;

    public GuideArrow(GuideLine guideLine, AbstractRuler parent) {
        mGuideLine = guideLine;
        mParent = parent;
        Double[] points;
        switch (guideLine.getDirection()) {
        case Vertical:
            points = new Double[] {
                -AbstractRuler.GUIDE_ARROW_WIDTH / 2.0, 0d,
                AbstractRuler.GUIDE_ARROW_WIDTH / 2.0, 0d,
                0d, (double) AbstractRuler.GUIDE_ARROW_HEIGHT};
            break;
        case Horizontal:
            points = new Double[] {
                0d, -AbstractRuler.GUIDE_ARROW_WIDTH / 2.0,
                0d, AbstractRuler.GUIDE_ARROW_WIDTH / 2.0,
                (double) AbstractRuler.GUIDE_ARROW_HEIGHT, 0d};
            break;
        default:
            throw new RuntimeException("Invalid guide line direction '" + guideLine.getDirection() + "'");
        }
        getPoints().setAll(points);
    }

    public GuideLine getGuideLine() {
        return mGuideLine;
    }
}
