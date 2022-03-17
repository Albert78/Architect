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
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.view.DefaultObjectReconciler;

public class CeilingReconciler extends DefaultObjectReconciler {
    public CeilingReconciler() {
        super();
    }

    public static boolean canRemoveCorner(Anchor bendPointHandleAnchor) {
        if (!Ceiling.isEdgeHandleAnchor(bendPointHandleAnchor)) {
            return false;
        }
        Ceiling ceiling = (Ceiling) bendPointHandleAnchor.getAnchorOwner();
        if (ceiling.getNumEdges() <= 3) {
            return false;
        }
        return true;
    }

    public static boolean removeCeilingCorner(Anchor bendPointHandleAnchor, UiController uiController, ChangeSet changeSet) {
        Ceiling ceiling = (Ceiling) bendPointHandleAnchor.getAnchorOwner();
        if (ceiling.getNumEdges() <= 3) {
            return false;
        }

        // Remove anchors from docks
        Anchor edgePositionAnchor = ceiling.getEdgePositionAnchorForConnectedEdgeHandleAnchor(bendPointHandleAnchor);
        uiController.doRemoveAnchorFromDock(bendPointHandleAnchor, changeSet);
        uiController.doRemoveAnchorFromDock(edgePositionAnchor, changeSet);

        ceiling.removeEdge(bendPointHandleAnchor, changeSet);

        return true;
    }
}
