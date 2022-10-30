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
package de.dh.cad.architect.model.wallmodel;

import java.util.Optional;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.Angle;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.MathUtils;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Wall;

/**
 * Positions of the four wall corner anchors in 2D space / on the floor according to current docking situation.
 */
public class WallAnchorPositions {
    protected static class WallEndAnchorPositions {
        protected final Position2D mLeftCorner;
        protected final Optional<Position2D> mOLeftBevelApexCorner;
        protected final Position2D mHandle;
        protected final Optional<Position2D> mORightBevelApexCorner;
        protected final Position2D mRightCorner;
        protected final boolean mHasNeighbor;

        public WallEndAnchorPositions(Position2D leftCorner, Optional<Position2D> oLeftBevelApexCorner, Position2D handle, Optional<Position2D> oRightBevelApexCorner, Position2D rightCorner, boolean hasNeighbor) {
            mLeftCorner = leftCorner;
            mOLeftBevelApexCorner = oLeftBevelApexCorner;
            mHandle = handle;
            mORightBevelApexCorner = oRightBevelApexCorner;
            mRightCorner = rightCorner;
            mHasNeighbor = hasNeighbor;
        }

        public static WallEndAnchorPositions fromThickness(Position2D handlePosition, Vector2D wallFN, Length thickness, boolean hasNeighbor) {
            Vector2D vLeft = wallFN.getNormalCCW().toUnitVector(LengthUnit.DEFAULT).times(thickness.inInternalFormat() / 2);
            return new WallEndAnchorPositions(handlePosition.plus(vLeft), Optional.empty(), handlePosition, Optional.empty(), handlePosition.minus(vLeft), hasNeighbor);
        }

        public Position2D getLeftCorner() {
            return mLeftCorner;
        }

        public Optional<Position2D> getOLeftBevelApexCorner() {
            return mOLeftBevelApexCorner;
        }

        public Position2D getHandle() {
            return mHandle;
        }

        public Optional<Position2D> getORightBevelApexCorner() {
            return mORightBevelApexCorner;
        }

        public Position2D getRightCorner() {
            return mRightCorner;
        }

        public boolean hasNeighbor() {
            return mHasNeighbor;
        }
    }

    protected static class WallEndBasePositions {
        protected final IWallAnchor mNearHandleAnchor;
        protected final IWallAnchor mFarHandleAnchor;
        protected final Length mThickness;

        public WallEndBasePositions(IWallAnchor nearHandleAnchor, IWallAnchor farHandleAnchor, Length thickness) {
            mNearHandleAnchor = nearHandleAnchor;
            mFarHandleAnchor = farHandleAnchor;
            mThickness = thickness;
        }

        public IWallAnchor getNearHandleAnchor() {
            return mNearHandleAnchor;
        }

        public Position2D getNearHandlePosition() {
            return mNearHandleAnchor.getPosition();
        }

        public IWallAnchor getFarHandleAnchor() {
            return mFarHandleAnchor;
        }

        public Position2D getFarHandlePosition() {
            return mFarHandleAnchor.getPosition();
        }

        public Length getThickness() {
            return mThickness;
        }

        /**
         * Vector pointing from this wall's far side anchor to the anchor at the near side.
         */
        public Vector2D getVectorFN() {
            return mNearHandleAnchor.getPosition().minus(mFarHandleAnchor.getPosition());
        }

        /**
         * Vector orthogonal to {@link #getVectorFN() the FN vector}, pointing in clockwise direction, with a length of the {@link #getThickness() thickness} of the wall.
         */
        public Vector2D getVectorCW() {
            return getVectorFN().getNormalCW().scaleToLength(mThickness);
        }

        public Vector2D getVectorCCW() {
            return getVectorCW().negated();
        }
    }

    protected static class NeighbourWallData extends WallEndBasePositions {
        protected final double mAngle;
        protected final boolean mHasBevelPriority;

        public NeighbourWallData(IWallAnchor nearHandleAnchor, IWallAnchor farHandleAnchor, double angle, Length thickness, boolean hasBevelPriority) {
            super(nearHandleAnchor, farHandleAnchor, thickness);
            mAngle = angle;
            mHasBevelPriority = hasBevelPriority;
        }

        public double getAngle() {
            return mAngle;
        }

        public boolean hasBevelPriority() {
            return mHasBevelPriority;
        }
    }

    protected static final double EPSILON = 0.001;
    protected static final Length WALL_MIN_LENGTH = Length.ofMM(5);

    // Handles
    protected final Position2D mHandleA;
    protected final Position2D mHandleB;

    // Neighbor situation
    protected final boolean mHasNeighborA;
    protected final boolean mHasNeighborB;

    // Wall end 1
    protected final Position2D mCornerA1;
    protected final Optional<Position2D> mOA1BevelApex;
    protected final Position2D mCornerA2;
    protected final Optional<Position2D> mOA2BevelApex;

    // Wall end 2
    protected final Position2D mCornerB1;
    protected final Optional<Position2D> mOB1BevelApex;
    protected final Position2D mCornerB2;
    protected final Optional<Position2D> mOB2BevelApex;

    public WallAnchorPositions(Position2D handleA, Position2D handleB, boolean hasNeighborA, boolean hasNeighborB,
        Position2D cornerA1, Optional<Position2D> oA1BevelApex,
        Position2D cornerA2, Optional<Position2D> oA2BevelApex,
        Position2D cornerB1, Optional<Position2D> oB1BevelApex,
        Position2D cornerB2, Optional<Position2D> oB2BevelApex) {
        mHandleA = handleA;
        mHandleB = handleB;
        mHasNeighborA = hasNeighborA;
        mHasNeighborB = hasNeighborB;
        mCornerA1 = cornerA1;
        mOA1BevelApex = oA1BevelApex;
        mCornerA2 = cornerA2;
        mOA2BevelApex = oA2BevelApex;
        mCornerB1 = cornerB1;
        mOB1BevelApex = oB1BevelApex;
        mCornerB2 = cornerB2;
        mOB2BevelApex = oB2BevelApex;
    }

    public static WallAnchorPositions fromHandles(
            Position2D handleA, Position2D handleB, Length thickness,
            boolean hasNeighborA, boolean hasNeighborB) {
        Vector2D vab = handleB.minus(handleA);
        Vector2D vnu12 = vab.getNormalCW().toUnitVector(LengthUnit.MM);
        Vector2D vnh = vnu12.times(thickness.inMM() / 2);
        return new WallAnchorPositions(handleA, handleB,
            hasNeighborA, hasNeighborB,
            handleA.minus(vnh), Optional.empty(),
            handleA.plus(vnh), Optional.empty(),
            handleB.minus(vnh), Optional.empty(),
            handleB.plus(vnh), Optional.empty());
    }

    public static WallAnchorPositions fromHandles(Anchor anchorWallHandleA, Anchor anchorWallHandleB, Length thickness, boolean hasNeighborA, boolean hasNeighborB) {
        return fromHandles(anchorWallHandleA.requirePosition2D(), anchorWallHandleB.requirePosition2D(), thickness, hasNeighborA, hasNeighborB);
    }

    public static WallAnchorPositions fromSide1(Position2D cornerA1, Position2D cornerB1, Length thickness, boolean hasNeighborA, boolean hasNeighborB) {
        Vector2D vab = cornerB1.minus(cornerA1);
        Vector2D vnu12 = vab.getNormalCW().toUnitVector(LengthUnit.MM);
        Vector2D vc = vnu12.times(thickness.inMM());
        Vector2D vch = vc.times(0.5);
        return new WallAnchorPositions(
            cornerA1.plus(vch), cornerB1.plus(vch),
            hasNeighborA, hasNeighborB,
            cornerA1, Optional.empty(),
            cornerA1.plus(vc), Optional.empty(),
            cornerB1, Optional.empty(),
            cornerB1.plus(vc), Optional.empty());
    }

    public static WallAnchorPositions fromSide2(Position2D cornerA2, Position2D cornerB2, Length thickness, boolean hasNeighborA, boolean hasNeighborB) {
        Vector2D vab = cornerB2.minus(cornerA2);
        Vector2D vnu21 = vab.getNormalCCW().toUnitVector(LengthUnit.MM);
        Vector2D vc = vnu21.times(thickness.inMM());
        Vector2D vch = vc.times(0.5);
        return new WallAnchorPositions(
            cornerA2.plus(vch), cornerB2.plus(vch),
            hasNeighborA, hasNeighborB,
            cornerA2.plus(vc), Optional.empty(),
            cornerA2, Optional.empty(),
            cornerB2.plus(vc), Optional.empty(),
            cornerB2, Optional.empty());
    }

    protected static WallAnchorPositions fromWallEnds(WallEndAnchorPositions endA, WallEndAnchorPositions endB) {
        return new WallAnchorPositions(
            endA.getHandle(), endB.getHandle(), endA.hasNeighbor(), endB.hasNeighbor(),
            endA.getRightCorner(), endA.getORightBevelApexCorner(),
            endA.getLeftCorner(), endA.getOLeftBevelApexCorner(),
            endB.getLeftCorner(), endB.getOLeftBevelApexCorner(),
            endB.getRightCorner(), endB.getORightBevelApexCorner());
    }

    public static Optional<WallAnchorPositions> calculateDockSituation(IWall wall) {
        // Abstract base positions from concrete wall ends
        Length thickness = wall.getThickness();
        IWallAnchor anchorWallHandleA = wall.getAnchorWallHandleA();
        IWallAnchor anchorWallHandleB = wall.getAnchorWallHandleB();
        WallEndBasePositions basePositionsA = new WallEndBasePositions(anchorWallHandleA, anchorWallHandleB, thickness);
        WallEndBasePositions basePositionsB = new WallEndBasePositions(anchorWallHandleB, anchorWallHandleA, thickness);

        // Calculate wall end from positions
        Optional<WallEndAnchorPositions> oWeapA = calculateWallEndAnchorPositions(basePositionsA);
        Optional<WallEndAnchorPositions> oWeapB = calculateWallEndAnchorPositions(basePositionsB);

        Position2D posHandleA = anchorWallHandleA.getPosition();
        Position2D posHandleB = anchorWallHandleB.getPosition();
        WallEndAnchorPositions weapA = oWeapA.orElse(WallEndAnchorPositions.fromThickness(posHandleA, posHandleA.minus(posHandleB), thickness, wall.hasNeighborWallA()));
        WallEndAnchorPositions weapB = oWeapB.orElse(WallEndAnchorPositions.fromThickness(posHandleB, posHandleB.minus(posHandleA), thickness, wall.hasNeighborWallB()));

        // Map wall end positions to concrete wall ends
        return Optional.of(WallAnchorPositions.fromWallEnds(weapA, weapB));
    }

    /**
     * Gets the wall bevel which is set for the anchor dock of the given wall handle anchor.
     * The bevel type will be taken from the priority docked anchor, so it can not just be read from any wall, it should
     * be read using this method.
     */
    // Attention: Same algorithm in method #forBaseEndPositions()
    public static Optional<WallBevelType> getWallBevelTypeOfAnchorDock(IWallAnchor wallHandleAnchor) {
        for (IWallAnchor adjacentAnchor : wallHandleAnchor.getAllDockedAnchors()) {
            IWall dockedWall = adjacentAnchor.getOwner();
            IWallAnchor dockedHandleAnchorA = dockedWall.getAnchorWallHandleA();
            IWallAnchor dockedHandleAnchorB = dockedWall.getAnchorWallHandleB();
            if (dockedHandleAnchorA.equals(adjacentAnchor)) {
                return Optional.of(dockedWall.getWallBevelA());
            } else if (dockedHandleAnchorB.equals(adjacentAnchor)) {
                return Optional.of(dockedWall.getWallBevelB());
            } // else docked wall is not docked at a handle so we don't recognize it as neighbour
        }
        return Optional.empty();
    }

    /**
     * Sets the wall bevel which is set for the anchor dock of the given wall handle anchor.
     * The bevel type will be taken from the priority docked anchor, so it can not just be set to any wall, it should
     * be set using this method.
     * Attention: After setting the wall bevel, the wall bevel apexes must be reconciled using an object reconcile operation.
     */
    public static void setWallBevelTypeOfAnchorDock(Anchor wallHandleAnchor, WallBevelType bevelType, ChangeSet changeSet) {
        for (Anchor adjacentAnchor : wallHandleAnchor.getAllDockedAnchors()) {
            Wall dockedWall = (Wall) adjacentAnchor.getAnchorOwner();
            Anchor dockedHandleAnchorA = dockedWall.getAnchorWallHandleA();
            Anchor dockedHandleAnchorB = dockedWall.getAnchorWallHandleB();
            if (dockedHandleAnchorA.equals(adjacentAnchor)) {
                dockedWall.setWallBevelA(bevelType);
                changeSet.changed(dockedWall);
            } else if (dockedHandleAnchorB.equals(adjacentAnchor)) {
                dockedWall.setWallBevelB(bevelType);
                changeSet.changed(dockedWall);
            } // else docked wall is not docked at a handle so we don't recognize it as neighbour
        }
    }

    public static void setWallBevelTypeOfAnchorDock(IWallAnchor wallHandleAnchor, WallBevelType bevelType) {
        for (IWallAnchor adjacentAnchor : wallHandleAnchor.getAllDockedAnchors()) {
            adjacentAnchor.getHandleAnchorDockEnd().ifPresent(wallDockEnd -> {
                IWall dockedWall = adjacentAnchor.getOwner();
                if (wallDockEnd == WallDockEnd.A) {
                    dockedWall.setWallBevelA(bevelType);
                } else if (wallDockEnd == WallDockEnd.B) {
                    dockedWall.setWallBevelB(bevelType);
                }
            });
        }
    }

    protected static Optional<WallEndAnchorPositions> calculateWallEndAnchorPositions(WallEndBasePositions basePositions) {
        IWallAnchor thisNearHandleAnchor = basePositions.getNearHandleAnchor();
        Position2D thisNearHandlePosition = thisNearHandleAnchor.getPosition();
        IWallAnchor thisFarHandleAnchor = basePositions.getFarHandleAnchor();
        Position2D thisFarHandlePosition = thisFarHandleAnchor.getPosition();
        Vector2D thisWallFN = basePositions.getVectorFN();
        Length thisWallFNLength = thisWallFN.getLength();
        Vector2D thisWallCW2 = basePositions.getVectorCW().times(0.5); // Pointing from this handle in clockwise direction
        Vector2D thisWallCCW2 = thisWallCW2.negated(); // Pointing from this handle in counter clockwise direction
        Length thisWallThickness = basePositions.getThickness();
        WallBevelType bevelType = null;

        if (thisWallFNLength.lt(WALL_MIN_LENGTH)) {
            return Optional.empty();
        }

        // If two or more walls are connected, we need the wall with the lowest angle (the wall left)
        // and the wall with the biggest angle (the wall right),
        // those are the outer walls which determine the corners. We can ignore all other walls in between
        // because their junction does not concern our wall anchors and is calculated in their own context.
        // If only one neighbour wall is connected, that wall counts both as left and as right wall.
        NeighbourWallData leftWallData = null;
        NeighbourWallData rightWallData = null;

        boolean thisAnchorPassed = false; // Anchors before this anchor have bevel priority, anchors after this anchors are subordinate

        for (IWallAnchor adjacentAnchor : thisNearHandleAnchor.getAllDockedAnchors()) {
            IWall dockedWall = adjacentAnchor.getOwner();
            IWallAnchor dockedHandleAnchorA = dockedWall.getAnchorWallHandleA();
            IWallAnchor dockedHandleAnchorB = dockedWall.getAnchorWallHandleB();
            WallBevelType dockedWallBevelType;
            IWallAnchor neighbourFarHandleAnchor;
            if (dockedHandleAnchorA.equals(adjacentAnchor)) {
                neighbourFarHandleAnchor = dockedHandleAnchorB;
                dockedWallBevelType = dockedWall.getWallBevelA();
            } else if (dockedHandleAnchorB.equals(adjacentAnchor)) {
                neighbourFarHandleAnchor = dockedHandleAnchorA;
                dockedWallBevelType = dockedWall.getWallBevelB();
            } else {
                // Docked wall is not docked at a handle so we don't recognize it as neighbour
                continue;
            }

            // First dock anchor determines the bevel type of this dock
            // ATTENTION: Same algorithm in method #getWallBevelTypeOfAnchorDock()
            if (bevelType == null) {
                bevelType = dockedWallBevelType;
            }

            if (adjacentAnchor.equals(thisNearHandleAnchor)) {
                thisAnchorPassed = true;
                // Skip our own dock entry
                continue;
            }

            Vector2D neighbourFN = thisNearHandlePosition.minus(neighbourFarHandleAnchor.getPosition()); // This handle position = other handle position at this side
            if (neighbourFN.getLength().lt(WALL_MIN_LENGTH)) {
                // Neighbour wall too short and thus invalid; treat it as if it would not exist
                continue;
            }
            double angle = Angle.angleBetween(neighbourFN, thisWallFN).getAngleDeg();
            if (Math.abs(angle % 360) < EPSILON) {
                // Neighbour wall is parallel, skip
            }
            if (leftWallData == null || leftWallData.getAngle() > angle) {
                leftWallData = new NeighbourWallData(thisNearHandleAnchor, neighbourFarHandleAnchor, angle, dockedWall.getThickness(), !thisAnchorPassed);
            }
            if (rightWallData == null || rightWallData.getAngle() < angle) {
                rightWallData = new NeighbourWallData(thisNearHandleAnchor, neighbourFarHandleAnchor, angle, dockedWall.getThickness(), !thisAnchorPassed);
            }
        }

        if (leftWallData == null || rightWallData == null) {
            // Trivial case: No other (valid) wall is connected
            return Optional.of(WallEndAnchorPositions.fromThickness(thisNearHandlePosition, thisWallFN, thisWallThickness, false));
        }

        // Left (CCW) situation
        Position2D leftCorner;
        Optional<Position2D> oLeftBevelApexCorner = Optional.empty();
        {
            double leftAngle = leftWallData.getAngle(); // Angle between thisWallFN and otherWallFN in degrees
            Position2D thisNearCCW = thisNearHandlePosition.plus(thisWallCCW2);
            Position2D thisFarCCW = thisFarHandlePosition.plus(thisWallCCW2);
            Vector2D otherCW = leftWallData.getVectorCW().times(0.5);
            Position2D otherNearHandlePosition = leftWallData.getNearHandlePosition();
            Position2D otherFarHandlePosition = leftWallData.getFarHandlePosition();
            Position2D otherNearCW = otherNearHandlePosition.plus(otherCW);
            Position2D otherFarCW = otherFarHandlePosition.plus(otherCW);
            Vector2D otherFN = otherNearHandlePosition.minus(otherFarHandlePosition);
            Length thickerWallThickness = Length.max(thisWallThickness, leftWallData.getThickness());
            boolean includeLeftBevelApex = !leftWallData.hasBevelPriority();

            if (Math.abs(leftAngle) < EPSILON) {
                // Other wall parallel, located over this wall (pointing to same direction)
                return Optional.empty(); // Invalid situation, (hopefully) prevented by check above
            } else if (Math.abs(leftAngle - 180) < EPSILON) {
                leftCorner = thisNearCCW; // Walls nearly parallel, other wall is elongation of this wall
            } else if (leftAngle < 180) {
                 leftCorner = MathUtils.calculateLinesIntersectionPoint(
                    thisNearCCW, thisFarCCW,
                    otherNearCW, otherFarCW).orElse(null);

                if (leftCorner == null || leftCorner.distance(thisNearCCW).gt(thisWallFNLength)
                        || leftCorner.distance(otherNearCW).gt(otherFN.getLength())) {
                    leftCorner = thisNearCCW; // Intersection point outside wall range of shorter wall
                }
            } else { // leftAngle > 180
                if (bevelType == WallBevelType.Miter) {
                    leftCorner = MathUtils.calculateLinesIntersectionPoint(
                        thisNearCCW, thisFarCCW,
                        otherNearCW, otherFarCW).orElse(null);
                    if (leftCorner == null || leftAngle > 270 && (
                            leftCorner.distance(thisNearHandlePosition).gt(thickerWallThickness))) {
                        // Walls build a sharp angle, intersection point is too far away; fallback to bevel
                        leftCorner = thisNearCCW;
                        if (includeLeftBevelApex) {
                            oLeftBevelApexCorner = Optional.of(otherNearCW);
                        }
                    }
                } else if (bevelType == WallBevelType.Bevel) {
                    leftCorner = thisNearCCW;
                    if (includeLeftBevelApex) {
                        oLeftBevelApexCorner = Optional.of(otherNearCW);
                    }
                } else throw new IllegalArgumentException("Unexpected value for wall bevel type: '" + bevelType + "'");
            }
        }

        // Right (CW) situation
        Position2D rightCorner;
        Optional<Position2D> oRightBevelApexCorner = Optional.empty();
        {
            double rightAngle = rightWallData.getAngle(); // Angle between thisWallFN and otherWallFN in degrees
            Position2D thisNearCW = thisNearHandlePosition.plus(thisWallCW2);
            Position2D thisFarCW = thisFarHandlePosition.plus(thisWallCW2);
            Vector2D otherCCW = rightWallData.getVectorCCW().times(0.5);
            Position2D otherNearHandlePosition = rightWallData.getNearHandlePosition();
            Position2D otherFarHandlePosition = rightWallData.getFarHandlePosition();
            Position2D otherNearCCW = otherNearHandlePosition.plus(otherCCW);
            Position2D otherFarCCW = otherFarHandlePosition.plus(otherCCW);
            Vector2D otherFN = otherNearHandlePosition.minus(otherFarHandlePosition);
            Length thickerWallThickness = Length.max(thisWallThickness, rightWallData.getThickness());
            boolean includeRightBevelApex = !rightWallData.hasBevelPriority();

            if (Math.abs(rightAngle) < EPSILON) {
                // Other wall parallel, located over this wall (pointing to same direction)
                return Optional.empty(); // Invalid situation, (hopefully) prevented by check above
            } else if (Math.abs(rightAngle - 180) < EPSILON) {
                rightCorner = thisNearCW; // Walls nearly parallel, other wall is elongation of this wall
            } else if (rightAngle > 180) {
                rightCorner = MathUtils.calculateLinesIntersectionPoint(
                    thisNearCW, thisFarCW,
                    otherNearCCW, otherFarCCW).orElse(null);

                if (rightCorner == null || rightCorner.distance(thisNearCW).gt(thisWallFNLength)
                        || rightCorner.distance(otherNearCCW).gt(otherFN.getLength())) {
                    rightCorner = thisNearCW; // Intersection point outside wall range of shorter wall
                }
            } else { // rightAngle < 180
                if (bevelType == WallBevelType.Miter) {
                    rightCorner = MathUtils.calculateLinesIntersectionPoint(
                        thisNearCW, thisFarCW,
                        otherNearCCW, otherFarCCW).orElse(null);
                    if (rightCorner == null || rightAngle < 90 && (
                            rightCorner.distance(thisNearHandlePosition).gt(thickerWallThickness))) {
                        // Walls build a sharp angle, intersection point is too far away; fallback to bevel
                        rightCorner = thisNearCW;
                        if (includeRightBevelApex) {
                            oRightBevelApexCorner = Optional.of(otherNearCCW);
                        }
                    }
                } else if (bevelType == WallBevelType.Bevel) {
                    rightCorner = thisNearCW;
                    if (includeRightBevelApex) {
                        oRightBevelApexCorner = Optional.of(otherNearCCW);
                    }
                } else throw new IllegalArgumentException("Unexpected value for wall bevel type: '" + bevelType + "'");
            }
        }
        return Optional.of(new WallEndAnchorPositions(leftCorner, oLeftBevelApexCorner, thisNearHandlePosition, oRightBevelApexCorner, rightCorner, true));
    }

    public Position2D getHandleA() {
        return mHandleA;
    }

    public Position2D getHandleB() {
        return mHandleB;
    }

    public Position2D getCornerA1() {
        return mCornerA1;
    }

    public Optional<Position2D> getOA1BevelApex() {
        return mOA1BevelApex;
    }

    public Position2D getCornerA2() {
        return mCornerA2;
    }

    public Optional<Position2D> getOA2BevelApex() {
        return mOA2BevelApex;
    }

    public Position2D getCornerB1() {
        return mCornerB1;
    }

    public Optional<Position2D> getOB1BevelApex() {
        return mOB1BevelApex;
    }

    public Position2D getCornerB2() {
        return mCornerB2;
    }

    public Optional<Position2D> getOB2BevelApex() {
        return mOB2BevelApex;
    }

    public WallOutline calculateWallOutlineCW() {
        WallOutline result = new WallOutline(mCornerA1);
        result.addCorner(WallSurface.One, false, mCornerB1);
        mOB1BevelApex.ifPresent(b1BevelApex -> result.addCorner(WallSurface.B, false, b1BevelApex));
        result.addCorner(WallSurface.B, mHasNeighborB, mHandleB);
        mOB2BevelApex.ifPresent(b2BevelApex -> result.addCorner(WallSurface.B, mHasNeighborB, b2BevelApex));
        result.addCorner(WallSurface.B, true, mCornerB2);
        result.addCorner(WallSurface.Two, false, mCornerA2);
        mOA2BevelApex.ifPresent(a2BevelApex -> result.addCorner(WallSurface.A, false, a2BevelApex));
        result.addCorner(WallSurface.A, mHasNeighborA, mHandleA);
        mOA1BevelApex.ifPresent(a1BevelApex -> result.addCorner(WallSurface.A, mHasNeighborA, a1BevelApex));
        result.closeOutline(WallSurface.A, true);
        return result;
    }
}