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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.objects.WallHole;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.IModificationFeatureProvider;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.construction.GroundPlanUIElementFilter;

/**
 * Behavior to edit a single selected object, e.g. provides an action to delete it. This behavior is also the entry point to edit
 * anchor docks; this behavior shows all handle anchors of the selected object; if the user clicks on a handle, we switch to
 * {@link EditSelectedAnchorBehavior}.
 */
public class EditSelectedObjectBehavior extends AbstractConstructionBehavior {
    protected class EditSelectedObjectUIElementFilter extends GroundPlanUIElementFilter {
        @SuppressWarnings("unlikely-arg-type")
        @Override
        public boolean isUIElementVisible(Abstract2DRepresentation repr) {
            if (super.isUIElementVisible(repr)) {
                return true;
            }
            if (repr instanceof AnchorConstructionRepresentation anchorRepr) {
                if (Objects.equals(repr, mEditObject)) {
                    return true;
                }
                Abstract2DRepresentation anchorOwnerRepresentation = anchorRepr.getAnchorOwnerRepresentation();
                if (Objects.equals(anchorOwnerRepresentation, mEditObject) && mEditObject.isEditHandle(anchorRepr.getAnchor())) {
                    // Show handles of edit object
                    return true;
                }
            }
            return false;
        }
    }

    protected IModificationFeatureProvider mEditObject = null;

    public EditSelectedObjectBehavior(Abstract2DRepresentation editRepr, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
        mEditObject = (IModificationFeatureProvider) editRepr;
        setUIElementFilter(new EditSelectedObjectUIElementFilter());
    }

    public static boolean canEditObject(Abstract2DRepresentation repr) {
        return repr instanceof IModificationFeatureProvider && !repr.getModelObject().isHidden();
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        // Delete
        if (!selectedRootObjects.isEmpty()) {
            actions.add(createRemoveObjectsAction(selectedRootObjects));
        }

        if (selectedObjects.size() == 1) {
            BaseObject selectedObject = selectedObjects.get(0);
            if (selectedObject instanceof Ceiling ceiling) {
                actions.add(createEditCeilingAnchorsAction(ceiling));
            } else if (selectedObject instanceof Wall wall) {
                // Add wall hole
                actions.add(createAddWallHoleAction(wall));
                actions.add(createDivideWallLengthAction(wall));
            } else if (selectedObject instanceof WallHole wallHole) {
                // Delete wall hole
                actions.add(createRemoveWallHoleAction(wallHole));
            }
        }

        mActionsList.setAll(actions);
    }

    @SuppressWarnings("unlikely-arg-type")
    @Override
    protected void configureObject(Abstract2DRepresentation repr) {
        super.configureObject(repr);
        if (Objects.equals(mEditObject, repr)) {
            mEditObject.enableModificationFeatures();
        } else if (repr instanceof IModificationFeatureProvider mfp) {
            mfp.disableModificationFeatures();
        }
    }

    @Override
    protected void unconfigureObject(Abstract2DRepresentation repr, boolean objectRemoved) {
        if (repr instanceof IModificationFeatureProvider mfp) {
            mfp.disableModificationFeatures();
        }
        super.unconfigureObject(repr, objectRemoved);
    }

    @Override
    public void onObjectsChanged(Collection<Abstract2DRepresentation> reprs) {
        super.onObjectsChanged(reprs);
        updateActionsListToSelection();
    }

    @Override
    protected void updateToSelection(Collection<Abstract2DRepresentation> selectedReprs) {
        Abstract2DRepresentation singleSelectedRepr = selectedReprs.size() == 1 ? selectedReprs.iterator().next() : null;

        IModificationFeatureProvider oldEditObject = mEditObject;
        mEditObject = null;
        if (oldEditObject != null) {
            configureObject((Abstract2DRepresentation) oldEditObject);
        }

        if (EditSelectedAnchorBehavior.canEditObject(singleSelectedRepr)) {
            mParentMode.setBehavior(new EditSelectedAnchorBehavior((AnchorConstructionRepresentation) singleSelectedRepr, mParentMode));
            return;
        }
        if (canEditObject(singleSelectedRepr)) {
            mEditObject = (IModificationFeatureProvider) singleSelectedRepr;
            configureObject((Abstract2DRepresentation) mEditObject);
        } else {
            mParentMode.resetBehavior();
            return;
        }
    }

    @Override
    protected void setDefaultUserHint() {
        Abstract2DRepresentation repr = (Abstract2DRepresentation) mEditObject;
        if (repr == null) {
            super.setDefaultUserHint();
            return;
        }
        setUserHint(MessageFormat.format(Strings.GROUND_PLAN_EDIT_OBJECT_USER_HINT, BaseObjectUIRepresentation.getObjName(repr.getModelObject())));
    }

    @Override
    public String getTitle() {
        return Strings.GROUND_PLAN_EDIT_OBJECT_BEHAVIOR_TITLE;
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
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