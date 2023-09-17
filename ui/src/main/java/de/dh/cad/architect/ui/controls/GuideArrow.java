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
    public static final int GUIDE_ARROW_HEIGHT = 12;
    public static final int GUIDE_ARROW_WIDTH = 10;

    protected final GuideLine mGuideLine;

    public GuideArrow(GuideLine guideLine) {
        mGuideLine = guideLine;
        Double[] points = new Double[] {
            -GUIDE_ARROW_WIDTH / 2.0, 0d,
            GUIDE_ARROW_WIDTH / 2.0, 0d,
            0d, (double) GUIDE_ARROW_HEIGHT};
        getPoints().setAll(points);
    }

    public GuideLine getGuideLine() {
        return mGuideLine;
    }
}
