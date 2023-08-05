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
package de.dh.cad.architect.ui.view.construction.feedback.wall;

import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.ui.objects.BasePolylineShapeAncillary;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.IVisualObjectFeedback;
import javafx.scene.shape.StrokeType;

/**
 * Draws the visual feedback for a {@link IWall wall}.
 */
public class WallShapeFeedback implements IVisualObjectFeedback<IWall> {
    protected final ConstructionView mParentView;
    protected final IWall mWall;
    protected final BasePolylineShapeAncillary mWallShape;

    public WallShapeFeedback(ConstructionView parentView, IWall wall) {
        mParentView = parentView;
        mWall = wall;
        mWallShape = new BasePolylineShapeAncillary(true, parentView);
    }

    @Override
    public String getObjectId() {
        return mWallShape.getId();
    }

    public IWall getWall() {
        return mWall;
    }

    @Override
    public void update() {
        Optional<List<Position2D>> oGroundPoints = WallAnchorPositions.calculateDockSituation(mWall).map(wap -> wap.calculateWallOutlineCW().calculateAllGroundPoints());
        if (oGroundPoints.isEmpty()) {
            mWallShape.setVisible(false);
            return;
        }
        mWallShape.setVisible(true);
        List<Position2D> groundPoints = oGroundPoints.get();
        mWallShape.updatePositions(groundPoints, StrokeType.INSIDE);
    }

    @Override
    public void install() {
        mParentView.addAncillaryObject(mWallShape);
    }

    @Override
    public void uninstall() {
        mParentView.removeAncillaryObject(mWallShape.getId());
    }
}
