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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import de.dh.cad.architect.model.coords.Angle;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.utils.SortedPair;

/**
 * Model holding the alignment information between some walls of interrest and a set of reference walls.
 */
public class WallsAlignmentModel {
    /**
     * Holds the connection info of two docked walls including their angles.
     */
    public static class BendPointAngle {
        protected final IWallAnchor mThisAnchor;
        protected final IWallAnchor mConnectedAnchor;
        protected Angle mThisWallAngle;
        protected Angle mNeighbourAngle;

        public BendPointAngle(IWallAnchor thisAnchor, IWallAnchor connectedAnchor) {
            mThisAnchor = thisAnchor;
            mConnectedAnchor = connectedAnchor;
        }

        public IWallAnchor getThisAnchor() {
            return mThisAnchor;
        }

        public IWallAnchor getConnectedAnchor() {
            return mConnectedAnchor;
        }

        public Angle getThisWallAngle() {
            return mThisWallAngle;
        }

        public void setThisWallAngle(Angle value) {
            mThisWallAngle = value;
        }

        public Angle getNeighbourAngle() {
            return mNeighbourAngle;
        }

        public void setNeighbourAngle(Angle value) {
            mNeighbourAngle = value;
        }

        @Override
        public String toString() {
            return "Neighbour wall at anchor " + mConnectedAnchor.getId() + ": Angle = " + mNeighbourAngle;
        }
    }

    /**
     * Contains the information about a distant wall in relation to this wall.
     */
    public static class POWallState {
        protected final IWall mDistantWall;
        protected boolean mIsParallel;
        protected boolean mIsOrthogonal;

        public POWallState(IWall otherWall) {
            mDistantWall = otherWall;
        }

        public IWall getOtherWall() {
            return mDistantWall;
        }

        public boolean isParallel() {
            return mIsParallel;
        }

        public void setParallel(boolean value) {
            mIsParallel = value;
        }

        public boolean isOrthogonal() {
            return mIsOrthogonal;
        }

        public void setOrthogonal(boolean value) {
            mIsOrthogonal = value;
        }

        @Override
        public String toString() {
            return "Wall " + mDistantWall.getId() + ": Parallel = " + mIsParallel + "; orthogonal = " + mIsOrthogonal;
        }
    }

    /**
     * Represents the detected states of a wall of interest, its own states and the relation to other walls.
     */
    public static class WallStates {
        protected final IWall mWallOfInterest;
        protected Angle mOrientationAngle = null;
        protected final Map<SortedPair<IWallAnchor>, BendPointAngle> mNeighbourWallAngles = new TreeMap<>(); // (Anchor1, anchor2) -> bend point angle
        protected final Map<String, POWallState> mPOWallStates = new TreeMap<>(); // Other wall id -> other wall state

        public WallStates(IWall wallOfInterest) {
            mWallOfInterest = wallOfInterest;
        }

        /**
         * Returns the wall for which this states object contains the values.
         */
        public IWall getWallOfInterest() {
            return mWallOfInterest;
        }

        /**
         * Gets the information whether the wall of interest is horizontal.
         */
        public boolean isHorizontal() {
            return Math.abs(mOrientationAngle.getAngleDeg() % 180) < EPSILON;
        }

        /**
         * Gets the information whether the wall of interest is vertical.
         */
        public boolean isVertical() {
            return Math.abs((mOrientationAngle.getAngleDeg() + 90) % 180) < EPSILON;
        }

        /**
         * Gets the angle of the wall of interest in relation to the Vector {@link Vector2D#X1M}.
         */
        public Angle getOrientationAngle() {
            return mOrientationAngle;
        }

        /**
         * Gets a map of connected anchor tuples to wall angles of that bend point.
         */
        public Map<SortedPair<IWallAnchor>, BendPointAngle> getNeighbourWallAngles() {
            return mNeighbourWallAngles;
        }

        /**
         * Gets a map of ids of distant (= non neighbour) walls to wall states of that distant wall related to this wall.
         * E.g. if this wall is A and a distant wall B is parallel to A, the returned map will contain an entry for
         * B containing the information that B is parallel.
         */
        public Map<String, POWallState> getPOWallStates() {
            return mPOWallStates;
        }

        protected boolean isParallel(Vector2D vReference, Vector2D vThisWall) {
            return vThisWall.isParallelTo(vReference, EPSILON_PARALLEL);
        }

        protected boolean isOrthogonal(Vector2D vReference, Vector2D vThisWall) {
            return vThisWall.isOrthogonalTo(vReference, EPSILON_ORTHOGONAL);
        }

        public void update(Collection<IWall> referenceWalls, Collection<VirtualLinesCenter> virtualLines) {
            IWall wallOfInterest = getWallOfInterest();
            IWallAnchor handleA = wallOfInterest.getAnchorWallHandleA();
            IWallAnchor handleB = wallOfInterest.getAnchorWallHandleB();
            Vector2D vThisWallA = getVWall(handleA);
            Vector2D vThisWallB = getVWall(handleB);
            mOrientationAngle = Angle.angleBetween(vThisWallA, Vector2D.X1M);

            // Update neighbour walls
            Collection<SortedPair<IWallAnchor>> unconfirmedBendpoints = new ArrayList<>(mNeighbourWallAngles.keySet());
            Collection<SortedPair<IWallAnchor>> confirmedAnglesA = updateNeighbourAngles(
                handleA,
                handleB.getPosition(),
                mNeighbourWallAngles,
                vThisWallA);
            unconfirmedBendpoints.removeAll(confirmedAnglesA);
            Collection<SortedPair<IWallAnchor>> confirmedAnglesB = updateNeighbourAngles(
                handleB,
                handleA.getPosition(),
                mNeighbourWallAngles,
                vThisWallB);
            unconfirmedBendpoints.removeAll(confirmedAnglesB);
            for (SortedPair<IWallAnchor> obsoleteBendpoint : unconfirmedBendpoints) {
                mNeighbourWallAngles.remove(obsoleteBendpoint);
            }

            // Update distant walls
            Collection<String> unconfirmedIds = new ArrayList<>(mPOWallStates.keySet());
            for (IWall referenceWall : referenceWalls) {
                String referenceWallId = referenceWall.getId();
                if (referenceWall.equals(wallOfInterest)) {
                    continue;
                }
                unconfirmedIds.remove(referenceWallId);
                POWallState referenceWallState = mPOWallStates.computeIfAbsent(referenceWallId, id -> new POWallState(referenceWall));
                Vector2D vReference = getVWall(referenceWall.getAnchorWallHandleA());
                referenceWallState.setParallel(isParallel(vReference, vThisWallA));
                referenceWallState.setOrthogonal(isOrthogonal(vReference, vThisWallA));
            }
            for (String obsoleteWallId : unconfirmedIds) {
                mPOWallStates.remove(obsoleteWallId);
            }
        }

        protected Collection<SortedPair<IWallAnchor>> updateNeighbourAngles(IWallAnchor checkAnchor, Position2D otherPosition,
            Map<SortedPair<IWallAnchor>, BendPointAngle> neighbourWallStates, Vector2D vThisWall) {
            Collection<SortedPair<IWallAnchor>> processedAngles = new ArrayList<>();
            Angle thisWallAngle = Angle.ofVector(vThisWall);
            for (IWallAnchor dockedAnchor : checkAnchor.getAllDockedAnchors()) {
                if (Objects.equals(dockedAnchor, checkAnchor)) {
                    continue;
                }
                // checkAnchor = our anchor
                // dockedAnchor = connected wall's anchor

                IWall otherWall = dockedAnchor.getOwner();
                if (!otherWall.representsRealWall() || !dockedAnchor.isHandle()) {
                    continue;
                }
                Vector2D vReference = getVWall(dockedAnchor);
                SortedPair<IWallAnchor> key = new SortedPair<>(checkAnchor, dockedAnchor);
                BendPointAngle neighbourWallAngle = neighbourWallStates.computeIfAbsent(key, k -> new BendPointAngle(checkAnchor, dockedAnchor));
                processedAngles.add(key);
                Angle neighbourAngle = Angle.ofVector(vReference);
                neighbourWallAngle.setThisWallAngle(thisWallAngle);
                neighbourWallAngle.setNeighbourAngle(neighbourAngle);
            }
            return processedAngles;
        }
    }

    protected static final double EPSILON = 0.01;
    public static final double EPSILON_ORTHOGONAL = 0.01;
    public static final double EPSILON_PARALLEL = 4000;

    protected final Map<String, WallStates> mWallStates = new TreeMap<>();
    protected final Collection<IWall> mReferenceWalls; // All walls for reference, if we have multiple walls of interest, those will also be included as reference walls
    protected final Collection<VirtualLinesCenter> mVirtualLinesCenters;

    public WallsAlignmentModel(Collection<? extends IWall> wallsOfInterest, Collection<IWall> referenceWalls, Collection<VirtualLinesCenter> virtualLinesCenters) {
        for (IWall wallOfInterest : wallsOfInterest) {
            mWallStates.computeIfAbsent(wallOfInterest.getId(), wallId -> new WallStates(wallOfInterest));
        }
        mReferenceWalls = referenceWalls;
        mVirtualLinesCenters = virtualLinesCenters;
    }

    /**
     * Creates an instance of the alignment model based on a given walls dock situation of the walls of interest, the
     * wall positions of the given reference walls and the positions of intersting virtual lines.
     * The created instance can be updated to a changed underlaying situation as long as the set of walls of interest,
     * the set of reference walls, the docking situation and the virtual lines don't change.
     */
    public static WallsAlignmentModel create(Collection<? extends IWall> wallsOfInterest, Collection<IWall> referenceWalls,
        Collection<VirtualLinesCenter> interestingVirtualLinesCenters) {
        return new WallsAlignmentModel(wallsOfInterest, referenceWalls, interestingVirtualLinesCenters);
    }

    /**
     * Updates all wall alignment states to the current situation of the underlaying walls.
     * @return Map of alignmentstate id to alignment state. The alignment state instances will be preserved as long as they are valid,
     * so their id's don't change until they become invalid.
     */
    public void updateWallStates() {
        for (WallStates wallStates : mWallStates.values()) {
            wallStates.update(mReferenceWalls, mVirtualLinesCenters);
        }
    }

    public Map<String, WallStates> getWallStates() {
        return mWallStates;
    }

    public Collection<IWall> getReferenceWalls() {
        return mReferenceWalls;
    }

    public static Vector2D getVWall(IWallAnchor startWallHandleAnchor) {
        return startWallHandleAnchor.getOwner().getAnchorWallHandle(
            startWallHandleAnchor.getHandleAnchorDockEnd().get().opposite()).getPosition().minus(startWallHandleAnchor.getPosition());
    }
}
