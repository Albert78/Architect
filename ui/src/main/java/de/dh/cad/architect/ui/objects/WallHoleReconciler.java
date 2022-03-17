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

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.objects.WallHole;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.view.DefaultObjectReconciler;
import de.dh.cad.architect.ui.view.ObjectReconcileOperation;

public class WallHoleReconciler extends DefaultObjectReconciler {
    /**
     * Updates the anchors connected to the given {@code wallHole} after a change of the wall hole's parameters.
     * Internally, this method uses an anchor move operation to update the wall hole's anchors and connected objects.
     */
    public static void updateWallHoleAnchors(WallHole wallHole, UiController uiController) {
        ChangeSet changeSet = new ChangeSet();
        doUpdateWallHoleAnchors(wallHole, uiController, changeSet);
        uiController.notifyChanges(changeSet);
    }

    /**
     * Updates the anchors connected to the given {@code wallHole} after a change of the wall hole's parameters.
     * Internally, this method uses an anchor move operation to update the wall hole's anchors and connected objects.
     */
    public static void doUpdateWallHoleAnchors(WallHole wallHole, UiController uiController, ChangeSet changeSet) {
        changeSet.changed(wallHole);
        ObjectReconcileOperation omo = new ObjectReconcileOperation("Update Wall Hole Anchors");
        omo.tryAddObjectToProcess(wallHole);
        uiController.doReconcileObjects(omo, changeSet);
    }

    public static void swapWallHoleDockEnd(WallHole wallHole, UiController uiController) {
        ChangeSet changeSet = new ChangeSet();
        doSwapWallHoleDockEnd(wallHole, uiController, changeSet);
        uiController.notifyChanges(changeSet);
    }

    public static void doSwapWallHoleDockEnd(WallHole wallHole, UiController uiController, ChangeSet changeSet) {
        changeSet.changed(wallHole);
        WallDockEnd currentDockEnd = wallHole.getDockEnd();
        Wall wall = wallHole.getWall();
        Length wallSideLength = wall.calculateBaseLength();
        Length distanceFromOtherWallEnd = wallSideLength
                        .minus(wallHole.getDimensions().getX())
                        .minus(wallHole.getDistanceFromWallEnd());
        wallHole.setDockEnd(currentDockEnd.opposite());
        wallHole.setDistanceFromWallEnd(distanceFromOtherWallEnd);
        doUpdateWallHoleAnchors(wallHole, uiController, changeSet);
    }
}
