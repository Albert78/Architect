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
package de.dh.cad.architect.ui.view.construction.feedback.wall.endings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.WallReconciler;
import de.dh.cad.architect.ui.objects.WallReconciler.DividedWallParts;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWall;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWallAnchor;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWallsModel;
import de.dh.cad.architect.ui.view.construction.feedback.wall.PrincipalWallAncillaryWallsModel;
import de.dh.cad.architect.utils.IdGenerator;

/**
 * Represents a wall end whose creation will break an existing wall into two parts, docked together at
 * the break point. The new wall end will be docked to the break point's anchor dock.
 */
public class WallBreakPointWallEnding extends AbstractWallEnding {
    protected static class BreakWallEndConfiguration extends AbstractWallEndConfiguration {
        protected final AncillaryWallAnchor mWallEndABreakPointHandle;
        protected final AncillaryWallAnchor mWallEndAOuterHandle;
        protected final AncillaryWallAnchor mWallEndBBreakPointHandle;
        protected final AncillaryWallAnchor mWallEndBOuterHandle;
        protected final AncillaryWallAnchor mAncillaryWallDistantHandle;
        protected final AncillaryWallAnchor mAncillaryWallBreakPointHandle;
        protected final Optional<AncillaryWallAnchor> mOOppositeAncillaryWallDistantHandle;
        protected final Optional<AncillaryWallAnchor> mOOppositeAncillaryWallBreakPointHandle;

        public BreakWallEndConfiguration(WallBreakPointWallEnding wallEndData, PrincipalWallAncillaryWallsModel ancillaryWallsModel,
            AncillaryWallAnchor wallEndABreakPointHandle, AncillaryWallAnchor wallEndAOuterHandle,
            AncillaryWallAnchor wallEndBBreakPointHandle, AncillaryWallAnchor wallEndBOuterHandle,
            AncillaryWallAnchor ancillaryWallCWDistantHandle, AncillaryWallAnchor ancillaryWallCWBreakPointHandle,
            Optional<AncillaryWallAnchor> oOppositeAncillaryWallDistantHandle, Optional<AncillaryWallAnchor> oOppositeAncillaryWallBreakPointHandle,
            WallBevelType wallBevel, boolean openWallEnd) {
            super(wallEndData, ancillaryWallsModel, wallBevel, openWallEnd);
            mWallEndABreakPointHandle = wallEndABreakPointHandle;
            mWallEndAOuterHandle = wallEndAOuterHandle;
            mWallEndBBreakPointHandle = wallEndBBreakPointHandle;
            mWallEndBOuterHandle = wallEndBOuterHandle;
            mAncillaryWallDistantHandle = ancillaryWallCWDistantHandle;
            mAncillaryWallBreakPointHandle = ancillaryWallCWBreakPointHandle;
            mOOppositeAncillaryWallDistantHandle = oOppositeAncillaryWallDistantHandle;
            mOOppositeAncillaryWallBreakPointHandle = oOppositeAncillaryWallBreakPointHandle;
        }

        public AncillaryWallAnchor getWallEndABreakPointHandle() {
            return mWallEndABreakPointHandle;
        }

        public AncillaryWallAnchor getWallEndAOuterHandle() {
            return mWallEndAOuterHandle;
        }

        public AncillaryWallAnchor getWallEndBBreakPointHandle() {
            return mWallEndBBreakPointHandle;
        }

        public AncillaryWallAnchor getWallEndBOuterHandle() {
            return mWallEndBOuterHandle;
        }

        public AncillaryWallAnchor getAncillaryWallDistantHandle() {
            return mAncillaryWallDistantHandle;
        }

        public AncillaryWallAnchor getAncillaryWallBreakPointHandle() {
            return mAncillaryWallBreakPointHandle;
        }

        public Optional<AncillaryWallAnchor> getOppositeAncillaryWallDistantHandle() {
            return mOOppositeAncillaryWallDistantHandle;
        }

        public Optional<AncillaryWallAnchor> getOppositeAncillaryWallBreakPointHandle() {
            return mOOppositeAncillaryWallBreakPointHandle;
        }
    }

    protected static final Length MIN_DISTANCE_FROM_END = Length.ofCM(20);

    protected final Wall mConnectWall;
    protected final double mBreakPointRatioFromA;

    public WallBreakPointWallEnding(Wall connectWall, double breakPointRatioFromA) {
        mConnectWall = connectWall;
        mBreakPointRatioFromA = breakPointRatioFromA;
    }

    public static Optional<WallBreakPointWallEnding> tryFindNearWallBreak(Position2D startPosition, Collection<Wall> walls) {
        for (Wall wall : walls) {
            Optional<WallBreakPointWallEnding> oRes = tryFindWallBreak(wall, startPosition);
            if (oRes.isPresent()) {
                return oRes;
            }
        }
        return Optional.empty();
    }

    public static Optional<WallBreakPointWallEnding> tryFindWallBreak(Wall wall, Position2D startPosition) {
        Position2D wallHandleA = wall.getAnchorWallHandleA().projectionXY();
        Position2D wallHandleB = wall.getAnchorWallHandleB().projectionXY();
        Length wallThickness = wall.getThickness();
        Vector2D vWall = Vector2D.between(wallHandleA, wallHandleB);
        double wallLength = vWall.getLength().inCM();
        Vector2D vWallU = vWall.toUnitVector(LengthUnit.CM);
        Vector2D vDelta = Vector2D.between(wallHandleA, startPosition);
        double parallelDistance = vWallU.dotProduct(vDelta, LengthUnit.CM);
        double minDistanceFromEnd = MIN_DISTANCE_FROM_END.inCM();
        if (parallelDistance < minDistanceFromEnd || parallelDistance > wallLength - minDistanceFromEnd) {
            // Outside of wall in wall's direction
            return Optional.empty();
        }
        Vector2D vWallOrthogonal = vWall.getNormalCCW().toUnitVector(LengthUnit.CM);
        double orthogonalDistance = vWallOrthogonal.dotProduct(vDelta, LengthUnit.CM);
        if (Math.abs(orthogonalDistance) > wallThickness.inCM() / 2) {
            return Optional.empty();
        }
        double breakPointRatioFromA = parallelDistance / wallLength;
        return Optional.of(new WallBreakPointWallEnding(wall, breakPointRatioFromA));
    }

    public Wall getConnectWall() {
        return mConnectWall;
    }

    public double getBreakPointRatioFromA() {
        return mBreakPointRatioFromA;
    }

    @Override
    public Position2D getPosition() {
        Position2D posA = mConnectWall.getAnchorWallHandleA().projectionXY();
        Position2D posB = mConnectWall.getAnchorWallHandleB().projectionXY();
        return Position2D.pointBetween(posA, posB, mBreakPointRatioFromA);
    }

    @Override
    public Optional<Anchor> getOConnectedAnchor() {
        return Optional.empty();
    }

    @Override
    public Optional<Wall> getOConnectedWall() {
        return Optional.of(mConnectWall);
    }

    protected BreakWallEndConfiguration tryGetCompatibleEndConfiguration(AbstractWallEndConfiguration wallEndConfiguration) {
        AbstractWallEnding wallEndData = wallEndConfiguration.getWallEnding();
        if (!(wallEndData instanceof WallBreakPointWallEnding) || !(wallEndConfiguration instanceof BreakWallEndConfiguration)) {
            return null;
        }
        WallBreakPointWallEnding oldWE = (WallBreakPointWallEnding) wallEndData;
        if (!mConnectWall.equals(oldWE.mConnectWall)) {
            return null;
        }
        return (BreakWallEndConfiguration) wallEndConfiguration;
    }

    /**
     * Adds all dock participants of the given {@code dockAnchor}, except {@code mConnectWall},
     * to the ancillary walls model, also docking the given {@code connectWallAncillaryHandle}.
     */
    private void addDock(AncillaryWallAnchor connectWallAncillaryHandle, Anchor dockAnchor, AncillaryWallsModel ancillaryWallsModel) {
        List<IWallAnchor> result = new ArrayList<>();
        for (Anchor anchor : new ArrayList<>(dockAnchor.getAllDockedAnchors())) {
            if (Wall.isWallHandleAnchor(anchor)) {
                BaseAnchoredObject anchorOwner = anchor.getAnchorOwner();
                if (anchorOwner == mConnectWall) {
                    continue;
                }
                AncillaryWall wall = ancillaryWallsModel.getWallsById().get(anchorOwner.getId());
                if (wall == null) {
                    // If wall was not added yet, add it
                    wall = ancillaryWallsModel.addWall((Wall) anchorOwner);
                }
                AncillaryWallAnchor participant = ancillaryWallsModel.getAnchorsById().get(anchor.getId());
                result.add(participant);
                participant.setDockedAnchors(result);
            }
        }
        result.add(connectWallAncillaryHandle);
        connectWallAncillaryHandle.setDockedAnchors(result);
    }

    public AbstractWallEndConfiguration configureEnding(AncillaryWallAnchor ancillaryWallBreakPointHandle, AncillaryWallAnchor ancillaryWallDistantHandle,
            Optional<AncillaryWallAnchor> oOppositeAncillaryWallDistantHandle, Optional<AncillaryWallAnchor> oOppositeAncillaryWallBreakPointHandle,
            PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel, boolean openWallEnd) {

        Collection<AncillaryWall> wallParts = new ArrayList<>();
        // All 4 walls are located with their A end at the breakpoint
        Anchor handleA = mConnectWall.getAnchorWallHandleA();
        Anchor handleB = mConnectWall.getAnchorWallHandleB();
        Map<String, AncillaryWall> wallsById = ancillaryWallsModel.getWallsById();
        Map<String, AncillaryWallAnchor> anchorsById = ancillaryWallsModel.getAnchorsById();
        AncillaryWall wallPartA = AncillaryWall.create(IdGenerator.generateUniqueId(AncillaryWall.class),
            IdGenerator.generateUniqueId(), IdGenerator.generateUniqueId(), wallsById, anchorsById);
        wallPartA.setRealWall(mConnectWall);
        AncillaryWallAnchor wallEndABreakPointHandle = wallPartA.getAnchorWallHandleA();
        AncillaryWallAnchor wallEndAOuterHandle = wallPartA.getAnchorWallHandleB();
        wallEndAOuterHandle.setPosition(handleA.projectionXY());
        // Unfortunately, we also have to add the walls, which are docked to the outer handles, to our model;
        // this is necessary to make the dimensioning feedbacks on the virtual wall ends take the wall junctions into account.
        addDock(wallEndAOuterHandle, handleA, ancillaryWallsModel);
        wallParts.add(wallPartA);
        AncillaryWall wallPartB = AncillaryWall.create(IdGenerator.generateUniqueId(AncillaryWall.class),
            IdGenerator.generateUniqueId(), IdGenerator.generateUniqueId(), wallsById, anchorsById);
        wallPartB.setRealWall(mConnectWall);
        AncillaryWallAnchor wallEndBBreakPointHandle = wallPartB.getAnchorWallHandleA();
        AncillaryWallAnchor wallEndBOuterHandle = wallPartB.getAnchorWallHandleB();
        wallEndBOuterHandle.setPosition(handleB.projectionXY());
        // See comment above
        addDock(wallEndBOuterHandle, handleB, ancillaryWallsModel);
        wallParts.add(wallPartB);
        ancillaryWallsModel.addWallsForFeedback(true, false, wallParts);
        Collection<AncillaryWallAnchor> anchors = new ArrayList<>(Arrays.asList(wallEndABreakPointHandle, wallEndBBreakPointHandle, ancillaryWallBreakPointHandle));
        oOppositeAncillaryWallBreakPointHandle.ifPresent(a -> anchors.add(a));
        dockAnchors(anchors);
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(ancillaryWallBreakPointHandle, wallBevel);
        return new  BreakWallEndConfiguration(this, ancillaryWallsModel,
            wallEndABreakPointHandle, wallEndAOuterHandle,
            wallEndBBreakPointHandle, wallEndBOuterHandle,
            ancillaryWallDistantHandle, ancillaryWallBreakPointHandle,
            oOppositeAncillaryWallDistantHandle, oOppositeAncillaryWallBreakPointHandle,
            wallBevel, openWallEnd);
    }

    @Override
    public AbstractWallEndConfiguration configureWallStart(PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel, boolean openWallEnd) {
        AncillaryWallAnchor ancillaryWallBreakPointHandle = ancillaryWallsModel.getPrincipalWallStartAnchor();
        AncillaryWallAnchor ancillaryWallDistantHandle = ancillaryWallsModel.getPrincipalWallEndAnchor();
        Optional<AncillaryWallAnchor> oOppositeAncillaryWallDistantHandle;
        Optional<AncillaryWallAnchor> oOppositeAncillaryWallBreakPointHandle;
        if (openWallEnd) {
            AncillaryWall oppositeAncillaryWall = ancillaryWallsModel.createNewWall();
            oOppositeAncillaryWallDistantHandle = Optional.of(oppositeAncillaryWall.getAnchorWallHandleB());
            oOppositeAncillaryWallBreakPointHandle = Optional.of(oppositeAncillaryWall.getAnchorWallHandleA());
        } else {
            oOppositeAncillaryWallDistantHandle = Optional.empty();
            oOppositeAncillaryWallBreakPointHandle = Optional.empty();
        }
        return configureEnding(ancillaryWallBreakPointHandle, ancillaryWallDistantHandle,
            oOppositeAncillaryWallDistantHandle, oOppositeAncillaryWallBreakPointHandle,
            ancillaryWallsModel, wallBevel, openWallEnd);
    }

    @Override
    public boolean tryUpdateWallStart(AbstractWallEndConfiguration wallEndConfiguration, Position2D wallEndPosition) {
        BreakWallEndConfiguration wec = tryGetCompatibleEndConfiguration(wallEndConfiguration);
        if (wec == null) {
            return false;
        }
        updateWallStart(wec);
        return true;
    }

    protected void updateEnding(BreakWallEndConfiguration wec) {
        Position2D breakPointPos = getPosition();
        wec.getWallEndABreakPointHandle().setPosition(breakPointPos);
        wec.getWallEndBBreakPointHandle().setPosition(breakPointPos);
        wec.getAncillaryWallBreakPointHandle().setPosition(breakPointPos);
        wec.getOppositeAncillaryWallBreakPointHandle().ifPresent(oppositeAncillaryWallBreakPointHandle -> oppositeAncillaryWallBreakPointHandle.setPosition(breakPointPos));
        wec.setWallEnding(this);
    }

    public void updateWallStart(BreakWallEndConfiguration wec) {
        updateEnding(wec);
        if (wec.isOpenWallEnd()) {
            Position2D posA = mConnectWall.getAnchorWallHandleA().projectionXY();
            Position2D posB = mConnectWall.getAnchorWallHandleB().projectionXY();
            Vector2D vWall = Vector2D.between(posA, posB);
            Vector2D normal = vWall.getNormalCW().scaleToLength(AbstractWallEndConfiguration.SINGLE_WALL_LENGTH);
            wec.getAncillaryWallDistantHandle().setPosition(getPosition().plus(normal));
            wec.getOppositeAncillaryWallDistantHandle().ifPresent(oppositeAncillaryWallDistantHandle -> oppositeAncillaryWallDistantHandle.setPosition(getPosition().minus(normal)));
            wec.getAncillaryWallsModel().setWallAnchorForMovingHandleFeedback(wec.getAncillaryWallBreakPointHandle());
        }
    }

    protected static void dockAnchors(Collection<AncillaryWallAnchor> anchors) {
        List<IWallAnchor> dockedAnchors = new ArrayList<>(anchors);
        for (AncillaryWallAnchor anchor : anchors) {
            anchor.setDockedAnchors(dockedAnchors);
        }
    }

    @Override
    public AbstractWallEndConfiguration configureWallEnd(PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel) {
        AncillaryWallAnchor ancillaryWallBreakPointHandle = ancillaryWallsModel.getPrincipalWallEndAnchor();
        AncillaryWallAnchor ancillaryWallDistantHandle = ancillaryWallsModel.getPrincipalWallStartAnchor();
        return configureEnding(ancillaryWallBreakPointHandle, ancillaryWallDistantHandle,
            Optional.empty(), Optional.empty(),
            ancillaryWallsModel, wallBevel, false);
    }

    @Override
    public boolean tryUpdateWallEnd(AbstractWallEndConfiguration wallEndConfiguration, Position2D wallStartPosition) {
        BreakWallEndConfiguration wec = tryGetCompatibleEndConfiguration(wallEndConfiguration);
        if (wec == null) {
            return false;
        }
        updateWallEnd(wec);
        return true;
    }

    public void updateWallEnd(BreakWallEndConfiguration wec) {
        updateEnding(wec);
    }

    @Override
    protected void configureFinalWall(AbstractWallEndConfiguration wallEndConfiguration, Wall wall, Anchor wallEndHandle,
        UiController uiController, ChangeSet changeSet) {
        DividedWallParts wallParts = WallReconciler.divideWall(mConnectWall, getPosition(), uiController);
        uiController.doDock(wallEndHandle, wallParts.getWallPartEndA().getAnchorWallHandleB(), changeSet);
    }
}
