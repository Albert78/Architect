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
package de.dh.cad.architect.ui.view.construction.feedback.wall;

import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallOutline;
import de.dh.cad.architect.model.wallmodel.WallOutlineConnection;
import de.dh.cad.architect.model.wallmodel.WallSurface;
import de.dh.cad.architect.ui.objects.DimensioningAncillary;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.utils.MathUtils;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.IVisualObjectFeedback;
import de.dh.cad.architect.utils.IdGenerator;

public class WallDimensioningsFeedback implements IVisualObjectFeedback<IWall> {
    protected static final double SIDE_DIMENSIONING_LABEL_DISTANCE = 30;
    protected static final double BASE_DIMENSIONING_LABEL_DISTANCE = 50;

    protected static final Length LENGTH_MINIMUM_DIFF = Length.ofMM(3);

    protected final ConstructionView mParentView;
    protected final IWall mWall;
    protected final String mId;
    protected DimensioningAncillary mSideOneDimensioning = null;
    protected DimensioningAncillary mSideTwoDimensioning = null;
    protected DimensioningAncillary mBaseDimensioning = null;

    public WallDimensioningsFeedback(ConstructionView parentView, IWall wall) {
        mParentView = parentView;
        mWall = wall;
        mId = IdGenerator.generateUniqueId(WallDimensioningsFeedback.class);
    }

    @Override
    public String getObjectId() {
        return mId;
    }

    public IWall getWall() {
        return mWall;
    }

    private static boolean samePositions(Position2D x1, Position2D x2, Position2D y1, Position2D y2) {
        Vector2D vX12 = Vector2D.between(x1, x2);
        Vector2D v1 = Vector2D.between(x1, y1);
        Vector2D v2 = Vector2D.between(x1, y2);
        double x12x12 = vX12.dotProduct(vX12, LengthUnit.MM);
        double x12v1 = vX12.dotProduct(v1, LengthUnit.MM);
        double x12v2 = vX12.dotProduct(v2, LengthUnit.MM);
        if (MathUtils.almostEqual(x12v2, x12x12, MathUtils.EPSILON_DOUBLE_EQUALITY)) {
            return MathUtils.almostEqual(x12v1, 0, MathUtils.EPSILON_DOUBLE_EQUALITY);
        } else if (MathUtils.almostEqual(x12v1, x12x12, MathUtils.EPSILON_DOUBLE_EQUALITY)) {
            return MathUtils.almostEqual(x12v2, 0, MathUtils.EPSILON_DOUBLE_EQUALITY);
        }
        return false;
    }

    @Override
    public void update() {
        IWallAnchor handleA = mWall.getAnchorWallHandleA();
        IWallAnchor handleB = mWall.getAnchorWallHandleB();
        boolean needBaseDimensioning =
                        handleA.getAllDockedAnchors().size() > 1 ||
                        handleB.getAllDockedAnchors().size() > 1;

        // Dimensionings
        if (mSideOneDimensioning == null) {
            mSideOneDimensioning = new DimensioningAncillary(mParentView);
            mParentView.addAncillaryObject(mSideOneDimensioning);
        }
        if (mSideTwoDimensioning == null) {
            mSideTwoDimensioning = new DimensioningAncillary(mParentView);
            mParentView.addAncillaryObject(mSideTwoDimensioning);
        }

        Optional<WallOutline> oWallOutline = WallAnchorPositions.calculateDockSituation(mWall).map(wap -> wap.calculateWallOutlineCW());
        if (oWallOutline.isPresent()) {
            WallOutline wallOutline = oWallOutline.get();
            boolean sideOneVisible = true;
            Position2D handleAPosition = handleA.getPosition();
            Position2D handleBPosition = handleB.getPosition();
            List<WallOutlineConnection> surfaceOneConnections = wallOutline.getConnections(WallSurface.One);
            if (surfaceOneConnections.isEmpty()) {
                sideOneVisible = false;
            } else {
                Position2D startSurfacePos = surfaceOneConnections.get(0).getNext().getPosition();
                Position2D endSurfacePos = surfaceOneConnections.get(surfaceOneConnections.size() - 1).getPrevious().getPosition();

                needBaseDimensioning &= !samePositions(handleAPosition, handleBPosition, startSurfacePos, endSurfacePos);

                mSideOneDimensioning.setProperties(startSurfacePos, endSurfacePos, 0, SIDE_DIMENSIONING_LABEL_DISTANCE, true);
            }
            mSideOneDimensioning.setVisible(sideOneVisible);

            boolean sideTwoVisible = true;
            List<WallOutlineConnection> surfaceTwoConnections = wallOutline.getConnections(WallSurface.Two);
            if (surfaceTwoConnections.isEmpty()) {
                sideTwoVisible = false;
            } else {
                Position2D startSurfacePos = surfaceTwoConnections.get(0).getPrevious().getPosition();
                Position2D endSurfacePos = surfaceTwoConnections.get(surfaceTwoConnections.size() - 1).getNext().getPosition();

                needBaseDimensioning &= !samePositions(handleAPosition, handleBPosition, startSurfacePos, endSurfacePos);

                mSideTwoDimensioning.setProperties(startSurfacePos, endSurfacePos, 0, -SIDE_DIMENSIONING_LABEL_DISTANCE, true);
            }
            mSideTwoDimensioning.setVisible(sideTwoVisible);

            if (needBaseDimensioning) {
                if (mBaseDimensioning == null) {
                    mBaseDimensioning = new DimensioningAncillary(mParentView);
                    mParentView.addAncillaryObject(mBaseDimensioning);
                }
                mBaseDimensioning.setProperties(handleAPosition, handleBPosition,
                    CoordinateUtils.lengthToCoords(mWall.getThickness()) / 2, BASE_DIMENSIONING_LABEL_DISTANCE, true);
            } else {
                if (mBaseDimensioning != null) {
                    mParentView.removeAncillaryObject(mBaseDimensioning.getId());
                    mBaseDimensioning = null;
                }
            }
        }
    }

    @Override
    public void install() {
        // Nothing to do, objects are dynamically created in update()
    }

    @Override
    public void uninstall() {
        if (mSideOneDimensioning != null) {
            mParentView.removeAncillaryObject(mSideOneDimensioning.getAncillaryObjectId());
            mSideOneDimensioning = null;
        }
        if (mSideTwoDimensioning != null) {
            mParentView.removeAncillaryObject(mSideTwoDimensioning.getAncillaryObjectId());
            mSideTwoDimensioning = null;
        }
        if (mBaseDimensioning != null) {
            mParentView.removeAncillaryObject(mBaseDimensioning.getAncillaryObjectId());
            mBaseDimensioning = null;
        }
    }
}
