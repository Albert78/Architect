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
import java.util.Optional;

import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.construction.SupportObjectsUIElementFilter;
import de.dh.cad.architect.ui.view.libraries.AssetChooserDialog;

public class SupportObjectsDefaultBehavior extends AbstractConstructionBehavior {
    public SupportObjectsDefaultBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
        setUIElementFilter(new SupportObjectsUIElementFilter());
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        if (selectedObjects.isEmpty()) {
            // No object selected

            // Add actions
            actions.add(createAddSupportObjectAction());
        } else if (selectedObjects.size() == 1) {
            // One object selected
            BaseObject selectedObject = selectedObjects.get(0);

            if (selectedObject instanceof Anchor) {
                //Anchor anchor = (Anchor) selectedObject;
                // No actions provided at the moment
            } else {
                if (selectedObject instanceof ObjectsGroup group) {
                    // Ungrouping
                    actions.add(createUngroupAction(group));
                }

                // We don't reach this position if the selected object can be handled by the Edit behavior,
                // (i.e. it's representation implements IModificationFeatureProvider).
                // So only put actions here for objects which cannot be edited with the Edit behavior.
            }
        } else {
            // More than 1 object selected

            // Grouping
            actions.add(createGroupAction(selectedObjects));
        }

        // TODO: Restrict action to support objects?

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
        return Strings.SUPPORT_OBJECTS_MODE_BEHAVIOR_TITLE;
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

    protected IContextAction createAddSupportObjectAction() {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GOUND_PLAN_ADD_SUPPORT_OBJECT_TITLE;
            }

            @Override
            public void execute() {
                AssetChooserDialog<SupportObjectDescriptor> dialog = AssetChooserDialog.createWithProgressIndicator(
                    getUiController().getAssetManager().buildAssetLoader(), Strings.SUPPORT_OBJECTS_DEFAULT_BEHAVIOR_CHOOSE_SUPPORT_OBJECT_TO_ADD, AssetType.SupportObject);

                Optional<SupportObjectDescriptor> soResult = dialog.showAndWait();

                if (soResult.isPresent()) {
                    SupportObjectDescriptor sod = soResult.get();

                    mParentMode.setBehavior(SupportObjectsAddSupportObjectBehavior.newSupportObject(sod, mParentMode));
                }
            }
        };
    }
}