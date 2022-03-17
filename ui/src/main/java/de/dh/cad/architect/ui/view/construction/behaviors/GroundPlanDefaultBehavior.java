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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.construction.GroundPlanUIElementFilter;

public class GroundPlanDefaultBehavior extends AbstractConstructionBehavior {
    public GroundPlanDefaultBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
        setUIElementFilter(new GroundPlanUIElementFilter());
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        if (selectedObjects.isEmpty()) {
            // No object selected

            // Add actions
            actions.add(createAddWallAction());
            actions.add(createAddDimensioningAction());
            actions.add(createAddFloorAction());
            actions.add(createAddCeilingAction());
        } else if (selectedObjects.size() == 1) {
            // One object selected
            BaseObject selectedObject = selectedObjects.get(0);

            if (selectedObject instanceof Anchor) {
                //Anchor anchor = (Anchor) selectedObject;
                // No actions provided at the moment
            } else if (selectedObject instanceof ObjectsGroup group) {
                // Ungrouping
                actions.add(createUngroupAction(group));

                // Only put actions here for objects which cannot be edited with one of the Edit behaviors,
                // e.g. objects whose representations implement IModificationFeatureProvider.
                // If such an object is selected, we automatically change into the appropriate behavior
                // so we will never come to this point for such objects.
            }
        } else {
            // More than 1 object selected
            BaseObject first = selectedObjects.get(0);
            BaseObject second = selectedObjects.get(1);

            // Grouping
            actions.add(createGroupAction(selectedObjects));

            if (selectedObjects.size() == 2) {
                // 2 objects selected

                if (first instanceof Wall firstWall && second instanceof Wall secondWall) {
                    if (canJoinWalls(firstWall, secondWall)) {
                        actions.add(createJoinWallsAction(firstWall, secondWall));
                    }
                }

                // No actions provided at the moment
            } else {
                // More than 2 objects selected
                BaseObject third = selectedObjects.get(2);

                if (selectedObjects.size() == 3) {
                    // 3 objects selected

                    if (first instanceof Anchor firstAnchor && second instanceof Anchor secondAnchor && third instanceof Anchor thirdAnchor) {
                        if (firstAnchor.is3D() && secondAnchor.is3D() && thirdAnchor.is3D()) {
                            actions.add(createAddCeilingAction(firstAnchor, secondAnchor, thirdAnchor));
                            actions.add(createAddCoveringAction(firstAnchor, secondAnchor, thirdAnchor));
                        }
                    }
                } else {
                    // More than 3 objects selected
                }
            }
        }

        // Delete
        if (!selectedRootObjects.isEmpty()) {
            actions.add(createRemoveObjectsAction(selectedRootObjects));
        }

        mActionsList.setAll(actions);
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    public String getTitle() {
        return Strings.GROUNDPLAN_DEFAULT_BEHAVIOR_TITLE;
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
    }

    @Override
    public void uninstall() {
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        super.uninstall();
    }
}