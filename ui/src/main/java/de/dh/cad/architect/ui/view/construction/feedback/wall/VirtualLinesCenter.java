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
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.coords.Angle;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;

/**
 * Contains position and likely follow-up angles for a moved anchor.
 */
public class VirtualLinesCenter {
    public class Distance {
        protected final Length mDistance;
        protected final Angle mAngleToDistance;

        public Distance(Length distance, Angle angleToDistance) {
            mDistance = distance;
            mAngleToDistance = angleToDistance;
        }

        public Length getDistance() {
            return mDistance;
        }

        public Angle getAngleToDistance() {
            return mAngleToDistance;
        }

        public VirtualLinesCenter getVirtualLinesCenter() {
            return VirtualLinesCenter.this;
        }

        @Override
        public String toString() {
            return String.format("%.4f", mDistance) + " to line P = " + getCenterPosition().axesAndCoordsToString() + " in direction " + mAngleToDistance;
        }
    }

    protected final String mId;
    protected final Position2D mCenterPosition;
    protected final Collection<Angle> mAngles = new ArrayList<>();

    public VirtualLinesCenter(String id, Position2D centerPosition) {
        mId = id;
        mCenterPosition = centerPosition;
    }

    public String getId() {
        return mId;
    }

    public Position2D getCenterPosition() {
        return mCenterPosition;
    }

    public Collection<Angle> getAngles() {
        return mAngles;
    }

    public Length calculateDistance(Vector2D vectorFromZero, Angle virtualLineAngle) {
        Vector2D lineVector = Vector2D.X1M.rotate(virtualLineAngle.getAngleDeg());
        // Dot product in cm of lineVector (length 1 m) times or vector -> this is the area of 1 m times the requested distance, in cm.
        // --> Divide by 100 is the distance in cm
        return Length.ofCM(Math.abs(lineVector.getNormalCW().dotProduct(vectorFromZero, LengthUnit.CM)) / 100);
    }

    public Distance calculateSmallestLineDistance(Position2D pos) {
        Vector2D vectorFromZero = Vector2D.between(mCenterPosition, pos);
        Angle vectorAngle = Angle.ofVector(vectorFromZero);
        Angle nearestAngle = vectorAngle.findNearestAngle(Angle.withOppositeAngles(mAngles), null).get();
        Length distance = calculateDistance(vectorFromZero, nearestAngle);
        return new Distance(distance, nearestAngle);
    }

    @Override
    public String toString() {
        return "Center = " + mCenterPosition.axesAndCoordsToString() + " directions [" + StringUtils.join(mAngles, ", ") + "]";
    }
}
