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
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.SupportObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.SupportObjectConstructionRepresentation.IMoveHandler;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.ObjectReconcileOperation;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.SupportObjectsUIElementFilter;
import de.dh.cad.architect.ui.view.construction.feedback.supportobjects.EditSupportObjectsVisualFeedbackManager;
import de.dh.cad.architect.ui.view.construction.feedback.supportobjects.EditSupportObjectsVisualFeedbackManager.SupportObjectLocationData;

/**
 * Behavior to handle the selection of one or more support objects.
 */
public class EditSelectedSupportObjectsBehavior extends AbstractConstructionBehavior {
    protected Collection<SupportObjectConstructionRepresentation> mInstalledSupportObjectReprs = new ArrayList<>();
    protected IMoveHandler mMoveHandler = (delta, firstMoveEvent) -> {
        UiController uiController = getUiController();
        List<IModelChange> changeTrace = new ArrayList<>();
        // TODO: Create own mergeable model change for moving this collection of support objects,
        // that change must override the here generated changes, like it is done in class UiController#MergeableSetAnchorDockPositionChange
        for (SupportObjectConstructionRepresentation repr : mInstalledSupportObjectReprs) {
            SupportObject so = repr.getSupportObject();
            Anchor handleAnchor = so.getHandleAnchor();
            Position2D targetPosition = handleAnchor.getPosition().projectionXY().plus(delta);
            uiController.doSetHandleAnchorPosition(handleAnchor, targetPosition, changeTrace);
        }
        uiController.notifyChange(changeTrace, Strings.SUPPORT_OBJECT_MOVE_CHANGE, !firstMoveEvent);
    };

    protected EditSupportObjectsVisualFeedbackManager mFeedbackManager = null; // Lives from install() to uninstall()
    protected final SupportObjectInfoControl mSOInfo = new SupportObjectInfoControl();

    public EditSelectedSupportObjectsBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
        setUIElementFilter(new SupportObjectsUIElementFilter());
    }

    protected static Optional<Collection<SupportObjectConstructionRepresentation>> tryCollectSupportObjectRepresentations(
        Collection<Abstract2DRepresentation> reprs, ConstructionView view) {
        Collection<SupportObjectConstructionRepresentation> result = new ArrayList<>();
        for (Abstract2DRepresentation repr : reprs) {
            if (!(repr instanceof SupportObjectConstructionRepresentation)) {
                continue;
            }
            SupportObjectConstructionRepresentation soRepr = (SupportObjectConstructionRepresentation) repr;
            result.add(soRepr);
        }
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    public static boolean isApplicable(Collection<String> selectedObjectIds, ConstructionView view) {
        // This behavior is applicable if all selected objects are support objects
        return tryCollectSupportObjectRepresentations(view.getRepresentationsByIds(selectedObjectIds), view)
                        .map(sos -> sos.size() == selectedObjectIds.size())
                        .orElse(false);
    }

    protected void updateVisualFeedback() {
        List<SupportObjectLocationData> locationData = new ArrayList<>();
        for (SupportObjectConstructionRepresentation repr : mInstalledSupportObjectReprs) {
            SupportObject so = repr.getSupportObject();

            SupportObjectLocationData sold = new SupportObjectLocationData(
                so.getId(),
                so.getHandleAnchor().getPosition().projectionXY(),
                so.getSize(),
                so.getRotationDeg(),
                so.getHeight(),
                so.getElevation());
            locationData.add(sold);
        }
        mFeedbackManager.updateVisualObjects(locationData);
    }

    protected boolean trySetObjects(Collection<Abstract2DRepresentation> selectedReprs) {
        for (SupportObjectConstructionRepresentation repr : mInstalledSupportObjectReprs) {
            repr.disableCollectiveMove();
        }
        mInstalledSupportObjectReprs.clear();

        Optional<Collection<SupportObjectConstructionRepresentation>> oSOReprs = tryCollectSupportObjectRepresentations(selectedReprs, (ConstructionView) mView);
        if (oSOReprs.isEmpty()) {
            return false;
        }
        Collection<SupportObjectConstructionRepresentation> selectedSOReprs = oSOReprs.get();
        for (SupportObjectConstructionRepresentation soRepr : selectedSOReprs) {
            soRepr.enableCollectiveMove(mMoveHandler);
            mInstalledSupportObjectReprs.add(soRepr);
        }
        updateVisualFeedback();
        return true;
    }

    @Override
    public void onObjectsChanged(Collection<Abstract2DRepresentation> reprs) {
        if (!CollectionUtils.intersection(mInstalledSupportObjectReprs, reprs).isEmpty()) {
            updateVisualFeedback();
        }
    }

    protected Collection<SupportObject> getInstalledSupportObjects() {
        return mInstalledSupportObjectReprs
                .stream()
                .map(soRepr -> (SupportObject) soRepr.getModelObject())
                .collect(Collectors.toList());
    }

    @Override
    protected void updateToSelection(Collection<Abstract2DRepresentation> selectedReprs) {
        if (!trySetObjects(selectedReprs)) {
            mParentMode.resetBehavior();
            return;
        }
        updateInteractionsPane();
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        if (selectedObjects.isEmpty()) {
            // No object selected, actually we should not reach this code
        } else {
            // 1 or more objects selected

            Collection<SupportObject> selectedSupportObjects = new ArrayList<>();
            for (BaseObject bo : selectedRootObjects) {
                if (bo instanceof SupportObject) {
                    selectedSupportObjects.add((SupportObject) bo);
                }
            }
            actions.add(createCopySupportObjectAction(selectedSupportObjects));

            if (selectedObjects.size() > 1) {
                // Grouping
                actions.add(createGroupAction(selectedObjects));
            }
        }

        // Delete
        if (!selectedRootObjects.isEmpty()) {
            actions.add(createRemoveObjectsAction(selectedRootObjects));
        }

        mActionsList.setAll(actions);
    }

    protected IContextAction createCopySupportObjectAction(Collection<SupportObject> supportObjects) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return supportObjects.size() == 1
                        ? MessageFormat.format(Strings.ACTION_GOUND_PLAN_COPY_SUPPORT_OBJECT_TITLE, BaseObjectUIRepresentation.getShortName(supportObjects.iterator().next()))
                        : MessageFormat.format(Strings.ACTION_GOUND_PLAN_COPY_SUPPORT_OBJECTS_TITLE, supportObjects.size());
            }

            @Override
            public void execute() {
                mParentMode.setBehavior(SupportObjectsAddSupportObjectBehavior.copySupportObjects(supportObjects, mParentMode));
            }
        };
    }

    protected void createInteractionsPane() {
        // TODO: Populate interactions control for support objects
//        setInteractionsControl(new InteractionsControl(mSOInfo, Strings.INTERACTIONS_TAB_SELECTED_SUPPORT_OBJECT_TITLE, false));
    }

    protected void updateInteractionsPane() {
        Collection<SupportObject> supportObjects = getInstalledSupportObjects();
        mSOInfo.setSupportObjects(supportObjects);
    }

    @Override
    public void setDefaultUserHint() {
        setUserHint(Strings.SUPPORT_OBJECTS_EDIT_BEHAVIOR_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.SUPPORT_OBJECTS_EDIT_BEHAVIOR_TITLE;
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        UiController uiController = view.getUiController();
        Plan plan = uiController.getPlan();
        mFeedbackManager = new EditSupportObjectsVisualFeedbackManager(this, (ConstructionView) view,
            newLocationData -> {
                List<IModelChange> changeTrace = new ArrayList<>();
                Collection<SupportObject> positionChangedObjects = new ArrayList<>();
                for (SupportObjectLocationData locationData : newLocationData) {
                    SupportObject supportObject = (SupportObject) plan.getObjectById(locationData.getModelId());
                    Position2D newCenterPoint = locationData.getCenterPoint();
                    if (!newCenterPoint.equals(supportObject.getCenterPoint())) {
                        Anchor handleAnchor = supportObject.getHandleAnchor();
                        handleAnchor.setPosition(handleAnchor.getPosition().withXY(newCenterPoint), changeTrace);
                        positionChangedObjects.add(supportObject);
                    }

                    Dimensions2D newSize = locationData.getSize();
                    if (!newSize.equals(supportObject.getSize())) {
                        supportObject.setSize(newSize, changeTrace);
                    }

                    float newRotation = locationData.getRotationDeg();
                    if (newRotation != supportObject.getRotationDeg()) {
                        supportObject.setRotationDeg(newRotation, changeTrace);
                    }

                    Length newHeight = locationData.getHeight();
                    if (!newHeight.equals(supportObject.getHeight())) {
                        supportObject.setHeight(newHeight, changeTrace);
                    }

                    Length newElevation = locationData.getElevation();
                    if (!newElevation.equals(supportObject.getElevation())) {
                        supportObject.setElevation(newElevation, changeTrace);
                    }
                }
                ObjectReconcileOperation oro = new ObjectReconcileOperation("Change support objects", positionChangedObjects);
                uiController.doReconcileObjects(oro, changeTrace);
                uiController.notifyChange(changeTrace, Strings.SUPPORT_OBJECT_SET_PROPERTY_CHANGE, true);
            });
        mFeedbackManager.install();
        setDefaultUserHint();
        createInteractionsPane();
        super.install(view);
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
    }

    @Override
    public void uninstall() {
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        mFeedbackManager.uninstall();
        mFeedbackManager = null;
        super.uninstall();
    }
}