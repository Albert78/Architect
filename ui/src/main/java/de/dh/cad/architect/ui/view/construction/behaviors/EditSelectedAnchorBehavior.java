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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controls.AnchorDockInfoControl;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.CeilingReconciler;
import de.dh.cad.architect.ui.objects.FloorReconciler;
import de.dh.cad.architect.ui.objects.WallReconciler;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.InteractionsControl;
import de.dh.cad.architect.ui.view.construction.EditedAnchorUIElementFilter;
import javafx.application.Platform;

/**
 * Behavior to edit a single selected anchor.
 */
public class EditSelectedAnchorBehavior extends AbstractConstructionBehavior {
    protected AnchorConstructionRepresentation mEditAnchorRepr = null;
    protected Abstract2DRepresentation mAnchorOwnerRepresentation = null;
    protected final AnchorDockInfoControl mDockInfo;

    public EditSelectedAnchorBehavior(AnchorConstructionRepresentation editAnchorRepr, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
        mDockInfo = new AnchorDockInfoControl(getUiController());
        initializeForAnchorRepr(editAnchorRepr);
    }

    public static boolean canEditObject(Abstract2DRepresentation repr) {
        return repr instanceof AnchorConstructionRepresentation;
    }

    protected void initializeForAnchorRepr(AnchorConstructionRepresentation editAnchorRepr) {
        mEditAnchorRepr = editAnchorRepr;
        mAnchorOwnerRepresentation = mEditAnchorRepr.getAnchorOwnerRepresentation();
        setUIElementFilter(new EditedAnchorUIElementFilter(editAnchorRepr));
    }

    @Override
    protected void configureObject(Abstract2DRepresentation repr) {
        super.configureObject(repr);
        repr.setObjectEmphasized(repr.equals(mAnchorOwnerRepresentation));
    }

    @Override
    protected void unconfigureObject(Abstract2DRepresentation repr, boolean objectRemoved) {
        if (!objectRemoved) {
            repr.setObjectEmphasized(false);
        }
        super.unconfigureObject(repr, objectRemoved);
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        Anchor selectedAnchor = mEditAnchorRepr.getAnchor();
        BaseAnchoredObject anchorOwner = selectedAnchor.getAnchorOwner();

        if (selectedAnchor.isHandle()) {
            if (anchorOwner instanceof Wall) {
                if (WallReconciler.canStraightenWallBendPoint(selectedAnchor)) {
                    actions.add(createStraightenWallBendPointAction(selectedAnchor));
                }
                if (Wall.isWallHandleAnchor(selectedAnchor)) {
                    actions.add(createAddWallAction(selectedAnchor));
                }
            } else if (anchorOwner instanceof Floor) {
                if (FloorReconciler.canRemoveCorner(selectedAnchor)) {
                    actions.add(createRemoveFloorCornerAction(selectedAnchor));
                }
            } else if (anchorOwner instanceof Ceiling) {
                if (CeilingReconciler.canRemoveCorner(selectedAnchor)) {
                    actions.add(createRemoveCeilingCornerAction(selectedAnchor));
                }
            }
            actions.add(createPermanentDockBehaviorAction(selectedAnchor));
            actions.add(createSoftDockBehaviorAction(selectedAnchor));

            if (selectedAnchor.getDockMaster().isPresent() || !selectedAnchor.getDockSlaves().isEmpty()) {
                actions.add(createUndockBehaviorAction(selectedAnchor));
            }
        }

        mActionsList.setAll(actions);
    }

    protected IContextAction createStraightenWallBendPointAction(Anchor anchor) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_STRAIGHTEN_WALL_BEND_POINT_TITLE;
            }

            @Override
            public void execute() {
                tryStraightenWallBendPoint(anchor);
            }
        };
    }

    protected IContextAction createRemoveFloorCornerAction(Anchor anchor) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_REMOVE_FLOOR_CORNER_TITLE;
            }

            @Override
            public void execute() {
                List<IModelChange> changeTrace = new ArrayList<>();
                UiController uiController = getUiController();
                FloorReconciler.removeFloorCorner(anchor, uiController, changeTrace);
                uiController.notifyChange(changeTrace, Strings.FLOOR_REMOVE_CORNER_CHANGE);
            }
        };
    }

    protected IContextAction createRemoveCeilingCornerAction(Anchor anchor) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_REMOVE_CEILING_CORNER_TITLE;
            }

            @Override
            public void execute() {
                List<IModelChange> changeTrace = new ArrayList<>();
                UiController uiController = getUiController();
                CeilingReconciler.removeCeilingCorner(anchor, uiController, changeTrace);
                uiController.notifyChange(changeTrace, Strings.CEILING_REMOVE_CORNER_CHANGE);
            }
        };
    }

    protected IContextAction createPermanentDockBehaviorAction(Anchor anchor) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.PERMANENT_DOCK_ANCHOR_ACTION_TITLE;
            }

            @Override
            public void execute() {
                AnchorConstructionRepresentation handleAnchorRepresentation = (AnchorConstructionRepresentation) getView().getRepresentationByModelId(anchor.getId());
                mParentMode.setBehavior(new PermanentDockBehavior(handleAnchorRepresentation, mParentMode));
            }
        };
    }

    protected IContextAction createSoftDockBehaviorAction(Anchor anchor) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.SOFT_DOCK_ANCHOR_ACTION_TITLE;
            }

            @Override
            public void execute() {
                AnchorConstructionRepresentation handleAnchorRepresentation = (AnchorConstructionRepresentation) getView().getRepresentationByModelId(anchor.getId());
                mParentMode.setBehavior(new SoftDockBehavior(handleAnchorRepresentation, mParentMode));
            }
        };
    }

    protected IContextAction createUndockBehaviorAction(Anchor anchor) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.UNDOCK_ANCHOR_ACTION_TITLE;
            }

            @Override
            public void execute() {
                getUiController().removeAnchorFromDock(anchor);
            }
        };
    }

    protected void createInteractionsPane() {
        setInteractionsControl(new InteractionsControl(mDockInfo, Strings.INTERACTIONS_TAB_SELECTED_ANCHOR_TITLE, false));
    }

    protected void updateInteractionsPane() {
        mDockInfo.updateForAnchor(mEditAnchorRepr.getAnchor());
    }

    @Override
    protected void updateToSelection(Collection<Abstract2DRepresentation> selectedReprs) {
        AnchorConstructionRepresentation selectedAnchorRepr = null;
        if (selectedReprs.size() == 1) {
            Abstract2DRepresentation selectedRepr = selectedReprs.iterator().next();
            if (selectedRepr instanceof AnchorConstructionRepresentation) {
                selectedAnchorRepr = (AnchorConstructionRepresentation) selectedRepr;
            }
        }

        if (selectedAnchorRepr == null) {
            throw new IllegalStateException("Cannot handle selected object constellation");
        }

        initializeForAnchorRepr(selectedAnchorRepr);

        // The following will potentially change the selection again, which will cause another
        // change handling process. So to avoid interference, we decouple it from the current call stack.
        Platform.runLater(() -> {
            updateInteractionsPane();
        });
    }

    @Override
    public void onObjectsChanged(Collection<Abstract2DRepresentation> reprs) {
        super.onObjectsChanged(reprs);
        if (reprs.contains(mEditAnchorRepr)) {
            updateToSelection();
        }
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    public void setDefaultUserHint() {
        setUserHint(
            MessageFormat.format(Strings.GROUND_PLAN_EDIT_ANCHOR_USER_HINT,
                BaseObjectUIRepresentation.getObjName(mEditAnchorRepr.getAnchor())));
    }

    @Override
    public String getTitle() {
        return Strings.GROUND_PLAN_EDIT_ANCHOR_BEHAVIOR_TITLE;
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);
        createInteractionsPane();
        updateInteractionsPane();
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
    }

    @Override
    public void uninstall() {
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        super.uninstall();
    }
}