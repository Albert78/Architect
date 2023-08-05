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
package de.dh.cad.architect.ui.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.MacroChange;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.objects.WallHole;
import de.dh.cad.architect.model.wallmodel.SingleWallBendPoint;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;
import de.dh.cad.architect.model.wallmodel.WallEndView;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controller.UiController.DockConflictStrategy;
import de.dh.cad.architect.ui.view.DefaultObjectReconciler;
import de.dh.cad.architect.ui.view.ObjectReconcileOperation;

public class WallReconciler extends DefaultObjectReconciler {
    public static class DividedWallParts {
        protected final Wall mWallPartEndA;
        protected final Wall mWallPartEndB;

        public DividedWallParts(Wall wallPartEndA, Wall wallPartEndB) {
            mWallPartEndA = wallPartEndA;
            mWallPartEndB = wallPartEndB;
        }

        public Wall getWallPartEndA() {
            return mWallPartEndA;
        }

        public Wall getWallPartEndB() {
            return mWallPartEndB;
        }
    }

    /**
     * Updates all corner anchors according to the current wall handle positions via an object reconcile operation.
     */
    public static boolean doUpdateWallAnchors(Wall wall, UiController uiController, List<IModelChange> changeTrace) {
        ObjectReconcileOperation omo = new ObjectReconcileOperation("Update Wall Anchors");
        omo.tryAddObjectToProcess(wall);
        for (WallHole wallHole : wall.getWallHoles()) {
            omo.tryAddObjectToProcess(wallHole);
        }
        uiController.doReconcileObjects(omo, changeTrace);
        return true;
    }

    /**
     * Tests if an anchor of {@code wall1} is docked to a wall handle anchor of {@code wall2} and returns it, if one was found.
     */
    public static Optional<Anchor> getJoiningAnchor(Wall wall1, Wall wall2) {
        Collection<Anchor> handlesWall1 = Arrays.asList(wall1.getAnchorWallHandleA(), wall1.getAnchorWallHandleB());
        Collection<Anchor> joiningAnchors = CollectionUtils.union(CollectionUtils.intersection(wall2.getAnchorWallHandleA().getAllDockedAnchors(), handlesWall1),
                        CollectionUtils.intersection(wall2.getAnchorWallHandleB().getAllDockedAnchors(), handlesWall1));
        return joiningAnchors.size() == 1 ? Optional.of(joiningAnchors.iterator().next()) : Optional.empty();
    }

    public static boolean canStraightenWallBendPoint(Anchor bendPointAnchor) {
        SingleWallBendPoint wallDock = SingleWallBendPoint.fromAnchorDock(bendPointAnchor).orElse(null);
        if (wallDock == null) {
            // The given anchor is not a wall bend point
            return false;
        }
        return true;
    }

    /**
     * Transfers the dock situation of the source anchor to the target anchor, i.e. docks
     * the target anchor to the dock master of source and docks all dock slaves from source
     * to the target anchor.
     */
    protected static void doTransferDocks(Anchor source, Anchor target, UiController uiController, List<IModelChange> changeTrace) {
        for (Anchor dockedAnchor : new ArrayList<>(source.getDockSlaves())) {
            uiController.doDock(dockedAnchor, target, DockConflictStrategy.SkipDock, changeTrace);
        }
        source.getDockMaster().ifPresent(sourceDockMaster -> {
            uiController.doDock(target, sourceDockMaster, DockConflictStrategy.SkipDock, changeTrace);
        });
    }

    public static boolean straightenWallBendPoint(Anchor bendPointAnchor, UiController uiController) {
        List<IModelChange> changeTrace = new ArrayList<>();
        boolean result = doStraightenWallBendPoint(bendPointAnchor, uiController, changeTrace);
        uiController.notifyChange(changeTrace, Strings.WALL_STRAIGHTEN_BENDPOINT_CHANGE);
        return result;
    }

    // TODO: Is it possible to do all this stuff with only one reconcile operation at the end?
    public static boolean doStraightenWallBendPoint(Anchor bendPointAnchor, UiController uiController, List<IModelChange> changeTrace) {
        SingleWallBendPoint bendPoint = SingleWallBendPoint.fromAnchorDock(bendPointAnchor).orElse(null);
        if (bendPoint == null) {
            // The given anchor is not a wall bend point
            return false;
        }
        // Merge walls; retain first wall
        Wall result = (Wall) bendPoint.getWallDockAnchor1().getAnchorOwner();
        Wall removeWall = (Wall) bendPoint.getWallDockAnchor2().getAnchorOwner();

        WallEndView resultWallDockEndView = bendPoint.getWall1DockEnd();
        WallEndView removeWallDockEndView = bendPoint.getWall2DockEnd();
        WallEndView removeWallOppositeEnd = removeWallDockEndView.getOppositeWallEndView();

        List<IModelChange> innerChangeTrace = new ArrayList<>(); // Changes where only modifications are relevant

        resultWallDockEndView.setWallHeight(removeWallOppositeEnd.getWallHeight(), innerChangeTrace);

        // Move windows from removeWall to resultWall
        WallDockEnd removeWallDockEnd = removeWallDockEndView.getWallEnd();
        for (WallHole wallHole : new ArrayList<>(removeWall.getWallHoles())) {
            // First ensure wall holes are docked at the wall end which will survive the operation.
            // (If the removed anchor is not located at the direct connection between the two other anchors,
            // i.e. the two walls are not in a line, I don't know how I should calculate the new wall hole
            // position if it is docked to the anchor which will be removed)
            if (wallHole.getDockEnd().equals(removeWallDockEnd)) {
                WallHoleReconciler.doSwapWallHoleDockEnd(wallHole, uiController, innerChangeTrace);
            }
            removeWall.removeOwnedChild_Internal(wallHole, innerChangeTrace);
            result.addWallHole_Internal(wallHole, innerChangeTrace);
        }

        // Remove middle anchors from their dock if they are docked, they won't be present any more later
        Collection<Anchor> middleAnchors = new ArrayList<>();
        middleAnchors.addAll(resultWallDockEndView.getAllAnchors());
        middleAnchors.addAll(removeWallDockEndView.getAllAnchors());
        for (Anchor middleAnchor : middleAnchors) {
            uiController.doRemoveAnchorFromDock(middleAnchor, innerChangeTrace);
        }

        // Anchors to be transferred to result wall
        Anchor aEndB = removeWallOppositeEnd.getHandleAnchor();
        Anchor aEndL_CCW = removeWallOppositeEnd.getCornerL_CCW();
        Anchor aEndL_CW = removeWallOppositeEnd.getCornerL_CW();
        Anchor aEndU_CCW = removeWallOppositeEnd.getCornerU_CCW();
        Anchor aEndU_CW = removeWallOppositeEnd.getCornerU_CW();

        removeWall.removeAnchor_Internal(aEndB, innerChangeTrace);
        removeWall.removeAnchor_Internal(aEndL_CCW, innerChangeTrace);
        removeWall.removeAnchor_Internal(aEndL_CW, innerChangeTrace);
        removeWall.removeAnchor_Internal(aEndU_CCW, innerChangeTrace);
        removeWall.removeAnchor_Internal(aEndU_CW, innerChangeTrace);

        // Anchors to be transferred to removeWall
        Anchor aMidB = resultWallDockEndView.getHandleAnchor();
        Anchor aMidL_CCW = resultWallDockEndView.getCornerL_CCW();
        Anchor aMidL_CW = resultWallDockEndView.getCornerL_CW();
        Anchor aMidU_CCW = resultWallDockEndView.getCornerU_CCW();
        Anchor aMidU_CW = resultWallDockEndView.getCornerU_CW();

        result.removeAnchor_Internal(aMidB, innerChangeTrace);
        result.removeAnchor_Internal(aMidL_CCW, innerChangeTrace);
        result.removeAnchor_Internal(aMidL_CW, innerChangeTrace);
        result.removeAnchor_Internal(aMidU_CCW, innerChangeTrace);
        result.removeAnchor_Internal(aMidU_CW, innerChangeTrace);

        // Transfer anchors
        result.addAnchor_Internal(aEndB, innerChangeTrace);
        result.addAnchor_Internal(aEndL_CCW, innerChangeTrace);
        result.addAnchor_Internal(aEndL_CW, innerChangeTrace);
        result.addAnchor_Internal(aEndU_CCW, innerChangeTrace);
        result.addAnchor_Internal(aEndU_CW, innerChangeTrace);

        removeWall.addAnchor_Internal(aMidB, innerChangeTrace);
        removeWall.addAnchor_Internal(aMidL_CCW, innerChangeTrace);
        removeWall.addAnchor_Internal(aMidL_CW, innerChangeTrace);
        removeWall.addAnchor_Internal(aMidU_CCW, innerChangeTrace);
        removeWall.addAnchor_Internal(aMidU_CW, innerChangeTrace);

        // Remove obsolete wall
        uiController.doRemoveObject(removeWall, innerChangeTrace);

        // Reconcile wall and all other docked objects
        ObjectReconcileOperation oro = new ObjectReconcileOperation("Merge walls", Arrays.asList(result));
        uiController.doReconcileObjects(oro, innerChangeTrace);

        changeTrace.add(MacroChange.create(innerChangeTrace, false));
        return true;
    }

    /**
     * Divides the given wall into two and positions the created break point at the given bend position.
     * Given the old wall OW with handle positions OW|A and OW|B, the new walls will be located like this:
     * <pre>
     * WallPartEndA|A - WallPartEndA|B <docked to> WallPartEndB|A - WallPartEndB|B
     * </pre>
     * <pre>
     * WallPartEndA|A located at OW|A, WallPartEndA|B and WallPartEndB|A located at bendPosition, WallPartEndB|B located at OW|B.
     * </pre>
     */
    public static DividedWallParts divideWall(Wall wall, Position2D bendPosition, UiController uiController) {
        List<IModelChange> changeTrace = new ArrayList<>();
        DividedWallParts result = doDivideWall(wall, bendPosition, uiController, changeTrace);
        uiController.notifyChange(changeTrace, Strings.WALL_DIVIDE_CHANGE);
        return result;
    }

    public static DividedWallParts doDivideWall(Wall wall, Position2D bendPosition, UiController uiController, List<IModelChange> changeTrace) {
        // If the wall's top is angular, we'll put the diagonal part to wall part end B and make wall part end A horizontal
        Length heightEndB = wall.getHeightB();
        Length thickness = wall.getThickness();

        List<IModelChange> innerChangeTrace = new ArrayList<>();

        // TODO: Generate names for new wall parts from old name

        // The following operation will shrink this wall so that it's B anchor matches the bend position.
        // The new wall will reach from the bend position to the old B end anchors.

        // We do a trick to avoid undocking the B end anchors: We create the new wall with zero size
        // at the bend position and then swap its B end anchors with the B end anchors of the wall to be divided
        Wall wallPartEndB = Wall.createFromHandlePositions(null, thickness, wall.getHeightA(), heightEndB,
                bendPosition, bendPosition, wall.getOwnerContainer(), innerChangeTrace);

        // Collect B side anchors from wallPartEndB (anchors are located at the middle of the whole wall -> aMidXXX)
        Anchor aMidB = wallPartEndB.getAnchorWallHandleB();
        Anchor aMidLB1 = wallPartEndB.getAnchorWallCornerLB1();
        Anchor aMidLB2 = wallPartEndB.getAnchorWallCornerLB2();
        Anchor aMidUB1 = wallPartEndB.getAnchorWallCornerUB1();
        Anchor aMidUB2 = wallPartEndB.getAnchorWallCornerUB2();

        // Remove B anchors from wallPartEndB
        wallPartEndB.removeAnchor_Internal(aMidB, innerChangeTrace);
        wallPartEndB.removeAnchor_Internal(aMidLB1, innerChangeTrace);
        wallPartEndB.removeAnchor_Internal(aMidLB2, innerChangeTrace);
        wallPartEndB.removeAnchor_Internal(aMidUB1, innerChangeTrace);
        wallPartEndB.removeAnchor_Internal(aMidUB2, innerChangeTrace);

        // Collect B anchors from thisWall (anchors are located at the end of the whole wall -> aEndXXX)
        Anchor aEndB = wall.getAnchorWallHandleB();
        Anchor aEndLB1 = wall.getAnchorWallCornerLB1();
        Anchor aEndLB2 = wall.getAnchorWallCornerLB2();
        Anchor aEndUB1 = wall.getAnchorWallCornerUB1();
        Anchor aEndUB2 = wall.getAnchorWallCornerUB2();

        // Remove B anchors from this wall
        wall.removeAnchor_Internal(aEndB, innerChangeTrace);
        wall.removeAnchor_Internal(aEndLB1, innerChangeTrace);
        wall.removeAnchor_Internal(aEndLB2, innerChangeTrace);
        wall.removeAnchor_Internal(aEndUB1, innerChangeTrace);
        wall.removeAnchor_Internal(aEndUB2, innerChangeTrace);

        // Add former B end anchors from thisWall to wallPartEndB
        wallPartEndB.addAnchor_Internal(aEndB, innerChangeTrace);
        wallPartEndB.addAnchor_Internal(aEndLB1, innerChangeTrace);
        wallPartEndB.addAnchor_Internal(aEndLB2, innerChangeTrace);
        wallPartEndB.addAnchor_Internal(aEndUB1, innerChangeTrace);
        wallPartEndB.addAnchor_Internal(aEndUB2, innerChangeTrace);

        // Add former B end anchors from wallPartEndB to thisWall
        wall.addAnchor_Internal(aMidB, innerChangeTrace);
        wall.addAnchor_Internal(aMidLB1, innerChangeTrace);
        wall.addAnchor_Internal(aMidLB2, innerChangeTrace);
        wall.addAnchor_Internal(aMidUB1, innerChangeTrace);
        wall.addAnchor_Internal(aMidUB2, innerChangeTrace);

        wall.setHeightB(wall.getHeightA(), innerChangeTrace);
        // TODO: Allocate each wall hole to one wall end, depending on position

        Anchor wallPartEndBHandleA = wallPartEndB.getAnchorWallHandleA();
        Collection<BaseAnchoredObject> changedWalls = Arrays.asList(wall, wallPartEndB);
        ObjectReconcileOperation omo = new ObjectReconcileOperation("Update Wall Anchors", changedWalls);
        uiController.doReconcileObjects(omo, innerChangeTrace);

        uiController.doDock(wallPartEndBHandleA, aMidB, DockConflictStrategy.SkipDock, innerChangeTrace);

        changeTrace.add(MacroChange.create(innerChangeTrace, false));
        return new DividedWallParts(wall, wallPartEndB);
    }

    public static DividedWallParts divideWall(Wall wall, Length breakPointdistance, WallDockEnd fromWallEnd, UiController uiController) {
        Position2D posB = wall.getAnchorWallHandleB().getPosition().projectionXY();
        Position2D posA = wall.getAnchorWallHandleA().getPosition().projectionXY();
        Vector2D vWall = Vector2D.between(posA, posB);
        Position2D bendPosition = fromWallEnd == WallDockEnd.A
                        ? posA.plus(vWall.scaleToLength(breakPointdistance))
                        : posB.minus(vWall.scaleToLength(breakPointdistance));
        return divideWall(wall, bendPosition, uiController);
    }
}
