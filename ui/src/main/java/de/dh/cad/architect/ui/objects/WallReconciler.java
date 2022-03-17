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
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.ChangeSet;
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
import de.dh.cad.architect.ui.controller.UiController;
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
    public static boolean updateWallAnchors(Wall wall, UiController uiController, ChangeSet changeSet) {
        ObjectReconcileOperation omo = new ObjectReconcileOperation("Update Wall Anchors");
        omo.tryAddObjectToProcess(wall);
        for (WallHole wallHole : wall.getWallHoles()) {
            omo.tryAddObjectToProcess(wallHole);
        }
        uiController.doReconcileObjects(omo, changeSet);
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

    // TODO: Is it possible to do all this stuff with only one reconcile operation at the end?
    public static boolean straightenWallBendPoint(Anchor bendPointAnchor, UiController uiController, ChangeSet changeSet) {
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

        // Move windows from second wall to first wall
        WallDockEnd removeWallDockEnd = removeWallDockEndView.getWallEnd();
        for (WallHole wallHole : new ArrayList<>(removeWall.getWallHoles())) {
            // First ensure wall holes are docked at the wall end which will survive the operation
            // (if the removed anchor is not located at the direct connection between the two other anchors,
            // i.e. the two walls are not in a line, I don't know how I should calculate the new wall hole
            // position if it is docked to the anchor which will be removed)
            if (wallHole.getDockEnd().equals(removeWallDockEnd)) {
                WallHoleReconciler.doSwapWallHoleDockEnd(wallHole, uiController, changeSet);
            }
            ChangeSet dummy = new ChangeSet();
            removeWall.removeOwnedChild_Internal(wallHole, dummy);
            result.addWallHole_Internal(wallHole, dummy);
            changeSet.changed(result, wallHole);
        }

        // Remove middle anchors from their dock if they are docked, they won't be present any more later
        Collection<Anchor> middleAnchors = new ArrayList<>();
        middleAnchors.addAll(resultWallDockEndView.getAllAnchors());
        middleAnchors.addAll(removeWallDockEndView.getAllAnchors());
        for (Anchor middleAnchor : middleAnchors) {
            uiController.doRemoveAnchorFromDock(middleAnchor, changeSet);
        }

        WallEndView removeWallOppositeEnd = removeWallDockEndView.getOppositeWallEndView();
        // Transfer all docks from the removed wall end to the result wall end
        uiController.doTransferDocks(removeWallOppositeEnd.getHandleAnchor(), resultWallDockEndView.getHandleAnchor(), changeSet);
        uiController.doTransferDocks(removeWallOppositeEnd.getCornerL_CCW(), resultWallDockEndView.getCornerL_CCW(), changeSet);
        uiController.doTransferDocks(removeWallOppositeEnd.getCornerL_CW(), resultWallDockEndView.getCornerL_CW(), changeSet);
        uiController.doTransferDocks(removeWallOppositeEnd.getCornerU_CCW(), resultWallDockEndView.getCornerU_CCW(), changeSet);
        uiController.doTransferDocks(removeWallOppositeEnd.getCornerU_CW(), resultWallDockEndView.getCornerU_CW(), changeSet);

        uiController.doSetHandleAnchorPosition(resultWallDockEndView.getHandleAnchor().getRootMasterOfAnchorDock(), removeWallOppositeEnd.getHandleAnchor().getPosition(), changeSet);

        // Remove obsolete wall
        uiController.doRemoveObject(removeWall, changeSet);

        // Reconcile wall and all other docked objects
        ObjectReconcileOperation oro = new ObjectReconcileOperation("Merge walls", Arrays.asList(result));
        uiController.doReconcileObjects(oro, changeSet);
        return true;
    }

    /**
     * Divides the given wall into two and positions the created break point at the given bend position.
     * Given the old wall OW with handle positions OW|A and OW|B, the new walls will be located like this:
     * WallPartEndA|A - WallPartEndA|B <docked to> WallPartEndB|A - WallPartEndB|B
     */
    public static DividedWallParts divideWall(Wall wall, Position2D bendPosition, UiController uiController) {
        Length heightEndB = wall.getHeightB(); // If wall is angular, put the diagonal part to wall end B and make wall end A horizontal
        Length thickness = wall.getThickness();

        ChangeSet changeSet = new ChangeSet();
        // TODO: Generate names for new wall parts from old name

        // The following operation will shrink this wall so that it's B anchor matches the bend position.
        // The new wall will reach from the bend position to the old B end anchors.

        // We do a trick to avoid undocking the B end anchors: We create the new wall with zero size
        // at the bend position and then swap its B end anchors with the B end anchors of this wall
        Wall wallPartEndB = Wall.createFromHandlePositions(null, thickness, wall.getHeightB(), heightEndB,
                bendPosition, bendPosition, wall.getOwnerContainer(), changeSet);

        // Collect B anchors from wallPartEndB (anchors are located at the middle of the whole wall -> aMidXXX)
        Anchor aMidB = wallPartEndB.getAnchorWallHandleB();
        Anchor aMidLB1 = wallPartEndB.getAnchorWallCornerLB1();
        Anchor aMidLB2 = wallPartEndB.getAnchorWallCornerLB2();
        Anchor aMidUB1 = wallPartEndB.getAnchorWallCornerUB1();
        Anchor aMidUB2 = wallPartEndB.getAnchorWallCornerUB2();

        // Remove B anchors from wallPartEndB
        wallPartEndB.removeAnchor_Internal(aMidB);
        wallPartEndB.removeAnchor_Internal(aMidLB1);
        wallPartEndB.removeAnchor_Internal(aMidLB2);
        wallPartEndB.removeAnchor_Internal(aMidUB1);
        wallPartEndB.removeAnchor_Internal(aMidUB2);

        // Collect B anchors from thisWall (anchors are located at the end of the whole wall -> aEndXXX)
        Anchor aEndB = wall.getAnchorWallHandleB();
        Anchor aEndLB1 = wall.getAnchorWallCornerLB1();
        Anchor aEndLB2 = wall.getAnchorWallCornerLB2();
        Anchor aEndUB1 = wall.getAnchorWallCornerUB1();
        Anchor aEndUB2 = wall.getAnchorWallCornerUB2();

        // Remove B anchors from this wall
        wall.removeAnchor_Internal(aEndB);
        wall.removeAnchor_Internal(aEndLB1);
        wall.removeAnchor_Internal(aEndLB2);
        wall.removeAnchor_Internal(aEndUB1);
        wall.removeAnchor_Internal(aEndUB2);

        // Add former B end anchors from thisWall to wallPartEndB
        wallPartEndB.addAnchor_Internal(aEndB);
        wallPartEndB.addAnchor_Internal(aEndLB1);
        wallPartEndB.addAnchor_Internal(aEndLB2);
        wallPartEndB.addAnchor_Internal(aEndUB1);
        wallPartEndB.addAnchor_Internal(aEndUB2);

        // Add former B end anchors from wallPartEndB to thisWall
        wall.addAnchor_Internal(aMidB);
        wall.addAnchor_Internal(aMidLB1);
        wall.addAnchor_Internal(aMidLB2);
        wall.addAnchor_Internal(aMidUB1);
        wall.addAnchor_Internal(aMidUB2);

        changeSet.changed(aMidB, aMidLB1, aMidLB2, aMidUB1, aMidUB2, aEndB, aEndLB1, aEndLB2, aEndUB1, aEndUB2);

        wall.setHeightB(wall.getHeightA());
        // TODO: Allocate each wall hole to one wall end, depending on position

        Anchor wallPartEndBHandleA = wallPartEndB.getAnchorWallHandleA();
        Collection<BaseAnchoredObject> changedWalls = Arrays.asList(wall, wallPartEndB);
        ObjectReconcileOperation omo = new ObjectReconcileOperation("Update Wall Anchors", changedWalls);
        uiController.doReconcileObjects(omo, changeSet);

        uiController.doDock(wallPartEndBHandleA, aMidB, changeSet);
        uiController.notifyChanges(changeSet);
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
