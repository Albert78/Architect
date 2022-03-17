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
package de.dh.cad.architect.ui.view;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.ReconcileResult;

public interface IObjectReconciler {
    /**
     * Calculates the impact of a move of handle anchors or the change of properties to its owner object.
     * Given the changed object situation, this method sets new properties and the new positions for all dependent anchors
     * of the owner object according to the new situation, taking all object constraints into account.
     * For example, a wall has a defined thickness and has handle anchors at both sides. A move operation to one of the wall handle anchors
     * will have an effect to the corner anchors. If such dependencies are affected, the positions of all dependent anchors are updated.
     * @param ownerObject Object which was changed, either by movements of handle anchors or by changing properties which causes a reconcile operation.
     * @param changeSet Changeset to add changes which were made during this operation.
     */
    ReconcileResult reconcileObjectChange(BaseAnchoredObject ownerObject, ChangeSet changeSet);
}
