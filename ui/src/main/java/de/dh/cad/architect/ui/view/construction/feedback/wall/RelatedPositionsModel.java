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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import de.dh.cad.architect.model.coords.Angle;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.ui.view.construction.feedback.wall.VirtualLinesCenter.Distance;

/**
 * Model calculating the data for the {@link RelatedPositionsFeedback related positions feedback}.
 */
public class RelatedPositionsModel {
    public static class RelatedPosition {
        protected Position2D mPosition = null;
        protected Angle mAngle = null;

        public RelatedPosition(Position2D position, Angle angle) {
            mPosition = position;
            mAngle = angle;
        }

        public Position2D getPosition() {
            return mPosition;
        }

        public Angle getAngle() {
            return mAngle;
        }
    }

    public static final Length EPSILON_VIRTUAL_LINE = Length.ofCM(1);
    public static final Length MIN_DISTANCE_VIRTUAL_LINES_CENTER = Length.ofCM(1);

    protected final Collection<VirtualLinesCenter> mVirtualLinesCenters;
    protected final Collection<? extends IWallAnchor> mWallAnchorsOfInterest;
    protected final Map<String, RelatedPosition> mRelatedPositions = new TreeMap<>(); // Artificial related position id -> Related position

    public RelatedPositionsModel(Collection<? extends IWallAnchor> wallAnchorsOfInterest, Collection<VirtualLinesCenter> virtualLinesCenters) {
        mWallAnchorsOfInterest = wallAnchorsOfInterest;
        mVirtualLinesCenters = virtualLinesCenters;
    }

    public static RelatedPositionsModel create(IWallAnchor wallAnchorOfInterest, Collection<VirtualLinesCenter> virtualLinesCenters) {
        return new RelatedPositionsModel(Arrays.asList(wallAnchorOfInterest), virtualLinesCenters);
    }

    /**
     * Returns the anchors of interest for which this model calculates the related positions.
     */
    public Collection<? extends IWallAnchor> getWallAnchorsOfInterest() {
        return mWallAnchorsOfInterest;
    }

    public Map<String, RelatedPosition> getRelatedPositions() {
        return mRelatedPositions;
    }

    public void update() {
        Collection<String> unconfirmedIds = new ArrayList<>(mRelatedPositions.keySet());
        for (IWallAnchor anchorOfInterest : mWallAnchorsOfInterest) {
            // Update related virtual lines
            for (VirtualLinesCenter virtualLineCenter : mVirtualLinesCenters) {
                Position2D centerPosition = virtualLineCenter.getCenterPosition();
                Position2D positionOfInterest = anchorOfInterest.getPosition();
                if (centerPosition.distance(positionOfInterest).lt(MIN_DISTANCE_VIRTUAL_LINES_CENTER)) {
                    continue;
                }
                Distance distance = virtualLineCenter.calculateSmallestLineDistance(positionOfInterest);
                if (distance.getDistance().gt(EPSILON_VIRTUAL_LINE)) {
                    continue;
                }
                // Id for related position: Position of virtual lines center plus snapped angle
                // -> This should be equal even if multiple virtual lines center instances are present for the same center position
                Angle angle = distance.getAngleToDistance();
                String relatedPositionId = String.format("%.2f/%.2f:%.2fdeg", centerPosition.getX().inCM(), centerPosition.getY().inCM(), angle.getAngleDeg());
                unconfirmedIds.remove(relatedPositionId);
                mRelatedPositions.computeIfAbsent(relatedPositionId, id -> new RelatedPosition(centerPosition, angle));
            }
        }
        for (String obsoleteRelatedPositionId : unconfirmedIds) {
            mRelatedPositions.remove(obsoleteRelatedPositionId);
        }
    }
}
