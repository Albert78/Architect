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

import java.util.ArrayList;
import java.util.List;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeType;

public class BasePolylineShapeAncillary extends Abstract2DAncillaryObject {
    protected final Polyline mShape = new Polyline();
    protected final double mStrokeWidth;
    protected final boolean mCompensateStrokeScale;

    public BasePolylineShapeAncillary(boolean compensateStrokeScale, ConstructionView parentView) {
        super(parentView);
        mCompensateStrokeScale = compensateStrokeScale;

        mShape.setStroke(ANCILLARY_OBJECTS_COLOR);
        mStrokeWidth = 5;
        mShape.setStrokeWidth(mStrokeWidth);
        mShape.setFill(null);

        addScaled(mShape);
        setMouseTransparent(true);
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        if (mCompensateStrokeScale) {
            mShape.setStrokeWidth(mStrokeWidth * scaleCompensation);
        }
    }

    /**
     * Updates the corner positions of the underlaying polygon in model coordinates.
     * @param points Points of the polygon, we support 2 points (= line) and more than 2 points. The polygon is automatically closed,
     * the starting point must not be added at the end again.
     */
    public void updatePositions(List<Position2D> points, StrokeType strokeType) {
        List<Double> coords = new ArrayList<>();
        for (Position2D pos : points) {
            coords.add(CoordinateUtils.lengthToCoords(pos.getX()));
            coords.add(CoordinateUtils.lengthToCoords(pos.getY()));
        }
        updateCoords(coords, strokeType);
    }

    /**
     * Updates the corner coordinates of the underlaying polygon in view coordinates.
     * @param coords Coordinates of the polygon, for each point, {@code coords} must contain the X and Y coordinate.
     */
    public void updateCoords(List<Double> coords, StrokeType strokeType) {
        int numPoints = coords.size();
        if (numPoints % 2 == 1 || numPoints < 4) {
            return;
        }
        if (numPoints == 4) {
            // Polyline is not drawn if we have 2 points and StroktType.INSIDE
            strokeType = StrokeType.CENTERED;
        }
        mShape.setStrokeType(strokeType);
        if (coords.size() > 4) {
            // Close shape
            coords = new ArrayList<>(coords);
            coords.add(coords.get(0));
            coords.add(coords.get(1));
        }
        mShape.getPoints().setAll(coords);
    }
}
