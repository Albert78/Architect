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
package de.dh.cad.architect.ui.view;

import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.IModelBasedObject;
import de.dh.cad.architect.ui.view.construction.behaviors.EditSelectedAnchorBehavior;
import de.dh.cad.architect.ui.view.construction.behaviors.GroundPlanDefaultBehavior;
import de.dh.cad.architect.utils.ObjectStringAdapter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public abstract class AbstractViewBehavior<TRepr extends IModelBasedObject, TAnc extends Node> {
    protected final ChangeListener<Boolean> OBJECT_SPOTTED_LISTENER = (observable, oldValue, newValue) -> {
        ReadOnlyProperty<?> prop = (ReadOnlyProperty<?>) observable;
        @SuppressWarnings("unchecked")
        TRepr obj = (TRepr) prop.getBean();
        onObjectSpotChanged(obj, newValue);
    };

    protected final EventHandler<? super KeyEvent> SCENE_KEY_HANDLER_ESCAPE_BEHAVIOR = event -> {
        if (event.getCode().equals(KeyCode.ESCAPE)) {
            event.consume();
            getParentMode().resetBehavior();
        }
    };

    protected final EventHandler<? super KeyEvent> SCENE_KEY_HANDLER_SPACE_TOGGLE_OBJECT_VISIBILITY = event -> {
        if (event.getCode() == KeyCode.SPACE) {
            event.consume();
            List<BaseObject> selectedObjects = getSelectedObjects();
            if (selectedObjects.isEmpty()) {
                return;
            }
            BaseObject firstSelectedObject = selectedObjects.get(0);
            getUiController().setObjectsVisibility(selectedObjects, !firstSelectedObject.isHidden());
        }
    };

    protected final EventHandler<? super KeyEvent> SCENE_KEY_HANDLER_DELETE_SELECTED_OBJECTS = event -> {
        if (event.getCode().equals(KeyCode.DELETE)) {
            event.consume();
            UiController uiController = getUiController();
            uiController.removeObjects(uiController.getObjectsById(uiController.selectedObjectIds()));
        }
    };

    protected final EventHandler<MouseEvent> MOUSE_CLICK_HANDLER_SELECT_OBJECT = new EventHandler<>() {
        @Override
        public void handle(MouseEvent event) {
            // Check if primary button...
            MouseButton button = event.getButton();
            if (button != MouseButton.PRIMARY) {
                return;
            }
            // ... and not moving
            if (!event.isStillSincePress()) {
                return;
            }
            event.consume();
            UiController uiController = getUiController();
            Collection<String> selectedObjectIds = uiController.selectedObjectIds();

            IModelBasedObject modelObject = (IModelBasedObject) event.getSource();
            boolean controlDown = event.isControlDown();
            if (!controlDown) {
                selectedObjectIds = Collections.emptySet();
            }
            selectedObjectIds = new TreeSet<>(selectedObjectIds);

            String selectObjectId = modelObject.getModelId();
            if (controlDown && selectedObjectIds.contains(selectObjectId)) {
                selectedObjectIds.remove(selectObjectId);
            } else {
                selectedObjectIds.add(selectObjectId);
            }
            uiController.setSelectedObjectIds(selectedObjectIds);
        }
    };

    protected static int ACTIONS_TOOLBAR_HEIGHT = 40;

    protected final StringProperty mUserHintProperty = new SimpleStringProperty(null);

    // Actions list is presented in the actions toolbar
    protected final ObservableList<IContextAction> mActionsList = FXCollections.observableArrayList();

    // Behavior specific toolbar beneath the menu
    protected final ToolBar mActionsToolBar;

    protected AbstractUIElementFilter<TRepr> mUIElementFilter = null;

    // Behavior specific actions Tab which is presented next to the properties tab
    protected final ObjectProperty<InteractionsControl> mInteractionsControlProperty = new SimpleObjectProperty<>();

    protected AbstractPlanView<TRepr, TAnc> mView = null;
    protected ListChangeListener<IContextAction> mActionsListChangeListener = new ListChangeListener<>() {
        @Override
        public void onChanged(Change<? extends IContextAction> c) {
            updateActionsButtons();
        }
    };

    protected final AbstractUiMode<TRepr, TAnc> mParentMode;

    public AbstractViewBehavior(AbstractUiMode<TRepr, TAnc> parentMode) {
        mParentMode = parentMode;

        mActionsToolBar = new ToolBar();
        mActionsToolBar.setPrefHeight(ACTIONS_TOOLBAR_HEIGHT);
    }

    /**
     * Gets the parent mode of this behavior. The parent mode acts as a container for use-case specific behaviors and should in fact
     * not be very interesting for behaviors.
     */
    public AbstractUiMode<TRepr, TAnc> getParentMode() {
        return mParentMode;
    }

    public UiController getUiController() {
        return mParentMode.getUiController();
    }

    public AbstractPlanView<TRepr, TAnc> getView() {
        return mView;
    }

    protected Plan getPlan() {
        return getUiController().getPlan();
    }

    public StringProperty userHintProperty() {
        return mUserHintProperty;
    }

    public String getUserHint() {
        return mUserHintProperty.get();
    }

    // TODO: Add timeout for hint, replace hint automatically by default hint when timeout is expired
    public void setUserHint(String value) {
        mUserHintProperty.set(value);
    }

    public ObjectProperty<InteractionsControl> interactionsControlProperty() {
        return mInteractionsControlProperty;
    }

    public InteractionsControl getInteractionsControl() {
        return mInteractionsControlProperty.get();
    }

    public void setInteractionsControl(InteractionsControl value) {
        mInteractionsControlProperty.set(value);
    }

    public ObservableList<IContextAction> getContextActions() {
        return mActionsList;
    }

    /**
     * Gets the toolbar with the behavior specific actions.
     * This toolbar is automatically filled from the {@link #getContextActions() context actions list}.
     */
    public ToolBar getActionsToolBar() {
        return mActionsToolBar;
    }

    public abstract String getTitle();

    public AbstractUIElementFilter<TRepr> getUIElementFilter() {
        return mUIElementFilter;
    }

    public void setUIElementFilter(AbstractUIElementFilter<TRepr> filter) {
        mUIElementFilter = filter;
    }

    protected List<BaseObject> getSelectedObjects() {
        Plan plan = getPlan();
        UiController uiController = getUiController();
        ObservableList<String> selectedObjectIds = uiController.selectedObjectIds();
        int numSelectedObjects = selectedObjectIds.size();
        List<BaseObject> selectedObjects = new ArrayList<>(numSelectedObjects);

        for (String objectId : selectedObjectIds) {
            BaseObject bo = plan.getObjectById(objectId);
            selectedObjects.add(bo);
        }
        return selectedObjects;
    }

    protected void updateActionsListToSelection() {
        Plan plan = getPlan();

        UiController uiController = getUiController();
        ObservableList<String> selectedObjectIds = uiController.selectedObjectIds();
        int numSelectedObjects = selectedObjectIds.size();
        List<BaseObject> selectedObjects = new ArrayList<>(numSelectedObjects);
        List<BaseObject> selectedRootObjects = new ArrayList<>(numSelectedObjects);

        for (String objectId : selectedObjectIds) {
            BaseObject bo = plan.getObjectById(objectId);
            selectedObjects.add(bo);
            if (plan.isRootObject(bo)) {
                selectedRootObjects.add(bo);
            }
        }

        updateActionsList(selectedObjects, selectedRootObjects);
    }

    protected void updateToSelection() {
        UiController uiController = getUiController();
        ObservableList<String> selectedObjectIds = uiController.selectedObjectIds();
        Collection<TRepr> selectedReprs = getView().getRepresentationsByIds(selectedObjectIds);
        updateToSelection(selectedReprs);
    }

    /**
     * Updates the action buttons to match the current situation.
     */
    protected void updateActionsButtons() {
        ObservableList<Node> actionsBoxChildren = mActionsToolBar.getItems();
        actionsBoxChildren.clear();

        for (IContextAction action : mActionsList) {
            Button actionButton = new Button(action.getTitle());
            actionButton.setPadding(new Insets(5, 5, 5, 5));
            actionButton.setOnAction(event -> {
                action.execute();
            });
            actionsBoxChildren.add(actionButton);
        }
    }

    protected void configureObjects() {
        for (TRepr repr : getView().getRepresentationsById().values()) {
            configureObject(repr);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Behavior change methods
    // Override, if the subclass needs a different dispatching of behaviors.
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Checks if the given new selection still matches to this behavior and switches to another
     * behavior, if necessary.
     * @param selectedReprs New selection situation.
     * @return {@code true} if we could switch to another behavior.
     * {@code false} if we don't want to switch away from this behavior. In that case, the caller will
     * call several update methods on this behavior like
     * {@link #updateToSelection(Collection)}, {@link #configureObjects()}, {@link #updateActionsListToSelection()} and
     * {@link #setDefaultUserHint()}.
     */
    // To be overridden, if another behavior calculation strategy is desired for the subclass
    protected boolean updateBehavior(Collection<TRepr> selectedReprs) {
        AbstractViewBehavior<TRepr, TAnc> targetBehavior = mParentMode.getBehaviorForSelectedReprs(selectedReprs);
        return trySetBehaviorAfterSelectionChange(targetBehavior);
    }

    /**
     * Tries to switch to the given desired behavior but first checks if the current
     * behavior can also handle the current (changed) situation. This check is needed to suppress
     * behavior changes for behaviors which want to maintain their state, for example after a selection change,
     * for example {@link EditSelectedAnchorBehavior} doesn't want to dispose and recreate its interaction pane.
     */
    protected boolean trySetBehaviorAfterSelectionChange(AbstractViewBehavior<TRepr, TAnc> desiredBehavior) {
        AbstractViewBehavior<TRepr, TAnc> currentBehavior = mParentMode.getBehavior();
        if (!currentBehavior.getClass().equals(desiredBehavior.getClass())
                || !currentBehavior.canHandleSelectionChange()) {
            mParentMode.setBehavior(desiredBehavior);
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this behavior is able to handle the current situation.
     * This method will NOT check whether another behavior better fits the current situation - this
     * is done by the prior behavior dispatch step.
     *
     * Typically, behaviors with an internal state connected to the current selection situation will
     * return {@code false} - this will cause the engine to recalculate the new applicable behavior for the
     * new (selection) state.
     * Behaviors should return {@code true},
     *
     * - if they are not related to the current selection (for example {@link GroundPlanDefaultBehavior})
     * - if their internal state matches the current situation, for example behaviors to edit an object of a special
     *   object class will check if the selection didn't change
     * - if they are able to "migrate" to the current, changed situation (for example by exchanging their
     *   selected object in focus, see {@link EditSelectedAnchorBehavior}).
     */
    protected abstract boolean canHandleSelectionChange();

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods of the "state machine" to be overridden.
    // Behaviors are designed to control most of the plan design actions and object behaviors, so to ease the implementation
    // of sub classes, most of the common events are present as overridable methods here.
    // More methods can be added if they support common usecases.
    // Behaviors will also need to add own event handlers on other components, if needed; such handlers must be maintained
    // using #install() and #uninstall().
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract void setDefaultUserHint();
    protected abstract void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects);

    protected void updateToSelection(Collection<TRepr> selectedReprs) {
        // Called if a compatible selection change occured
    }

    /**
     * Configures the given UI object for this behavior. This includes adding event handlers and setting states on that object.
     * This method is called for each UI representation, also for those which are added during the lifetime of this behavior.
     * This method might be called more than once for an UI representation, make sure that event handlers are not added multiple times.
     */
    // To be overridden
    protected void configureObject(TRepr repr) {
        configureDefaultObjectHandlers(repr);

        if (mUIElementFilter != null) {
            mUIElementFilter.configure(repr);
        }
    }

    /**
     * Removes the behavior-specific configuration of the given UI object when this behavior is uninstalled or when the object is removed.
     * This removes all event handlers and behavior-specific states which were set in {@link #configureObject(TRepr)}.
     * @param objectRemoved This parameter is {@code true} if the object was completely removed from the UI, e.g. if it was deleted. In that case,
     * the reversion of behavior-specific states can be omitted if they only affect the object's UI representation and don't affect other objects.
     */
    // To be overridden
    protected void unconfigureObject(TRepr repr, boolean objectRemoved) {
        if (mUIElementFilter != null) {
            mUIElementFilter.unconfigure(repr);
        }

        unconfigureDefaultObjectHandlers(repr);
    }

    /**
     * Updates the behavior-specific object settings/decoration to a changed state of the given object.
     * Make sure that all behavior-specific event handlers and object state settings are reverted in {@link #unconfigureObject(TRepr, boolean)}.
     */
    // To be overridden
    protected void updateObject(TRepr repr) {
        if (mUIElementFilter != null) {
            mUIElementFilter.configure(repr);
        }
    }

    // To be overridden
    protected void onObjectSpotChanged(TRepr repr, boolean isSpotted) {
        if (isSpotted) {
            setUserHint(MessageFormat.format(Strings.BEHAVIORS_OBJ_NAME, BaseObjectUIRepresentation.getObjName(repr.getModelObject())));
        } else {
            setDefaultUserHint();
        }
    }

    // To be overridden
    public void onObjectFocusChanged(TRepr repr, boolean focused) {
        // Nothing to do here
    }

    // To be overridden
    public void onObjectsAdded(Collection<TRepr> reprs) {
        for (TRepr repr : reprs) {
            configureObject(repr);
        }
    }

    // To be overridden
    public void onObjectsRemoved(Collection<TRepr> reprs) {
        for (TRepr repr : reprs) {
            unconfigureObject(repr, true);
        }
    }

    // To be overridden
    public void onObjectsChanged(Collection<TRepr> reprs) {
        for (TRepr repr : reprs) {
            updateObject(repr);
        }
    }

    // To be overridden
    public void onObjectsSelectionChanged(Collection<TRepr> removedSelectionReprs,
        Collection<TRepr> addedSelectionReprs) {
        UiController uiController = getUiController();
        ObservableList<String> selectedObjectIds = uiController.selectedObjectIds();
        Collection<TRepr> selectedReprs = getView().getRepresentationsByIds(selectedObjectIds);

        if (!updateBehavior(selectedReprs)) {
            updateToSelection(selectedReprs);
            configureObjects();
            updateActionsListToSelection();
            setDefaultUserHint();
        }
    }

    // To be overridden
    public void install(AbstractPlanView<TRepr, TAnc> view) {
        mView = view;
        updateActionsButtons();
        mActionsList.addListener(mActionsListChangeListener);

        installDefaultViewHandlers();
        for (TRepr repr : getView().getRepresentationsById().values()) {
            configureObject(repr);
        }

        updateToSelection();
        updateActionsListToSelection();
        setDefaultUserHint();
    }

    // To be overridden
    public void uninstall() {
        for (TRepr repr : getView().getRepresentationsById().values()) {
            unconfigureObject(repr, false);
        }
        uninstallDefaultViewHandlers();

        mActionsList.removeListener(mActionsListChangeListener);
        mActionsToolBar.getItems().clear();
        mView = null;
    }

    // Can be overridden, if necessary
    protected void installDefaultViewHandlers() {
        // To be overridden
    }

    // Must be overridden if #installDefaultHandlers() is overridden
    protected void uninstallDefaultViewHandlers() {
        // To be overridden
    }

    /**
     * Adds the default object handlers to the given object. This method can be called in sub classes in overriding implementations of
     * {@link #configureObject(TRepr)}. To revoke the handlers, call {@link #unconfigureDefaultObjectHandlers(TRepr)}.
     */
    protected abstract void configureDefaultObjectHandlers(TRepr repr);

    /**
     * Removes the default object handlers from the given object which were configured via {@link #configureDefaultObjectHandlers(TRepr)}.
     * This method should be called in overriding implementations of {@link #unconfigureObject(TRepr, boolean)} if {@link #configureObject(TRepr)}
     * called {@link #configureDefaultObjectHandlers(TRepr)}.
     */
    protected abstract void unconfigureDefaultObjectHandlers(TRepr repr);

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods supporting default use-cases like adding default event handlers etc.
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void installDefaultEscapeBehaviorKeyHandler() {
        getView().getScene().addEventHandler(KeyEvent.KEY_PRESSED, SCENE_KEY_HANDLER_ESCAPE_BEHAVIOR);
    }

    protected void uninstallDefaultEscapeBehaviorKeyHandler() {
        getView().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, SCENE_KEY_HANDLER_ESCAPE_BEHAVIOR);
    }

    protected void installDefaultSpaceToggleObjectVisibilityKeyHandler() {
        getView().getScene().addEventHandler(KeyEvent.KEY_PRESSED, SCENE_KEY_HANDLER_SPACE_TOGGLE_OBJECT_VISIBILITY);
    }

    protected void uninstallDefaultSpaceToggleObjectVisibilityKeyHandler() {
        getView().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, SCENE_KEY_HANDLER_SPACE_TOGGLE_OBJECT_VISIBILITY);
    }

    protected void installDefaultDeleteObjectsKeyHandler() {
        getView().getScene().addEventHandler(KeyEvent.KEY_PRESSED, SCENE_KEY_HANDLER_DELETE_SELECTED_OBJECTS);
    }

    protected void uninstallDefaultDeleteObjectsKeyHandler() {
        getView().getScene().removeEventHandler(KeyEvent.KEY_PRESSED, SCENE_KEY_HANDLER_DELETE_SELECTED_OBJECTS);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Default actions
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected IContextAction createCancelBehaviorAction(String title) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public void execute() {
                mParentMode.resetBehavior();
            }
        };
    }

    protected IContextAction createGroupAction(List<BaseObject> objectsToGroup) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return MessageFormat.format(Strings.GROUP_N_OBJECTS_ACTION_TITLE, objectsToGroup.size());
            }

            @Override
            public void execute() {
                TextInputDialog tid = new TextInputDialog();
                tid.setTitle(Strings.GROUP_ACTION_EDIT_GROUP_NAME_DIALOG_TITLE);
                tid.setHeaderText(Strings.GROUP_ACTION_EDIT_GROUP_NAME_DIALOG_HEADER_TEXT);
                tid.setContentText(Strings.GROUP_ACTION_EDIT_GROUP_NAME_DIALOG_CONTENT_TEXT);
                Optional<String> res = tid.showAndWait();
                res.ifPresent(groupName -> {
                    UiController uiController = getUiController();
                    ObjectsGroup group = uiController.groupObjects(objectsToGroup, groupName);
                    uiController.setSelectedObjectId(group.getId());
                    AbstractViewBehavior.this.updateActionsListToSelection();
                });
            }
        };
    }

    protected IContextAction createUngroupAction(ObjectsGroup group) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return MessageFormat.format(Strings.UNGROUP_ACTION_TITLE, BaseObjectUIRepresentation.getShortName(group));
            }

            @Override
            public void execute() {
                Collection<String> objectIdsFromGroup = group.getGroupedObjectIds();
                UiController uiController = getUiController();
                uiController.ungroup(group);
                uiController.setSelectedObjectIds(objectIdsFromGroup);
            }
        };
    }

    protected IContextAction createAddObjectsToGroupAction(Collection<BaseObject> objectsToAdd) {
        int numObjects = objectsToAdd.size();
        return new IContextAction() {
            @Override
            public String getTitle() {
                return MessageFormat.format(new ChoiceFormat(Strings.CF_ADD_OBJECTS_TO_GROUP_ACTION_TITLE).format(numObjects), numObjects);
            }

            @Override
            public void execute() {
                Collection<ObjectsGroup> groups = getPlan().getGroups().values();
                ChoiceDialog<ObjectStringAdapter<ObjectsGroup>> choiceDialog =
                        new ChoiceDialog<>(null, ObjectStringAdapter.wrap(groups, BaseObjectUIRepresentation::getShortName));
                choiceDialog.setTitle(MessageFormat.format(
                        new ChoiceFormat(Strings.CF_ADD_OBJECTS_TO_GROUP_GROUP_CHOICE_DIALOG_TITLE).format(numObjects),
                        numObjects
                ));
                choiceDialog.setHeaderText(
                        MessageFormat.format(
                                new ChoiceFormat(Strings.CF_ADD_OBJECTS_TO_GROUP_GROUP_CHOICE_DIALOG_HEADER_TEXT).format(numObjects),
                            StringUtils.join(objectsToAdd
                                    .stream()
                                    .map(BaseObjectUIRepresentation::getShortName)
                                    .toList()), ", "));
                Optional<ObjectStringAdapter<ObjectsGroup>> oRes = choiceDialog.showAndWait();
                oRes.ifPresent(osaGroup -> {
                    ObjectsGroup group = osaGroup.getObj();
                    UiController uiController = getUiController();
                    List<IModelChange> changeTrace = new ArrayList<>();
                    group.addAllObjects(objectsToAdd, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.ADD_OBJECTS_TO_GROUP_CHANGE);
                    AbstractViewBehavior.this.updateActionsListToSelection();
                });
            }
        };
    }

    /**
     * Retrieves all groups which completely contain the given objects.
     */
    protected Collection<ObjectsGroup> getCompletelyContainingGroups(Collection<? extends BaseObject> objects) {
        return getPlan().getGroups().values()
            .stream()
            .filter(group -> group.getGroupedObjects().containsAll(objects))
            .toList();
    }

    protected IContextAction createRemoveObjectsFromGroupAction(Collection<BaseObject> objectsToRemove) {
        int numObjects = objectsToRemove.size();
        return new IContextAction() {
            @Override
            public String getTitle() {
                return MessageFormat.format(
                        new ChoiceFormat(Strings.CF_REMOVE_OBJECTS_FROM_GROUP_ACTION_TITLE).format(numObjects),
                        numObjects);
            }

            @Override
            public void execute() {
                Collection<ObjectsGroup> groups = getCompletelyContainingGroups(objectsToRemove);
                if (groups.isEmpty()) {
                    new Alert(AlertType.ERROR,
                            MessageFormat.format(
                                    new ChoiceFormat(Strings.CF_REMOVE_OBJECTS_FROM_GROUP_NO_GROUPS_ERROR_TEXT).format(numObjects),
                                    numObjects),
                            ButtonType.OK)
                        .showAndWait();
                    return;
                }
                ChoiceDialog<ObjectStringAdapter<ObjectsGroup>> choiceDialog =
                        new ChoiceDialog<>(null, ObjectStringAdapter.wrap(groups, g -> BaseObjectUIRepresentation.getShortName(g)));
                choiceDialog.setTitle(MessageFormat.format(
                        new ChoiceFormat(Strings.CF_REMOVE_OBJECTS_FROM_GROUP_GROUP_CHOICE_DIALOG_TITLE).format(numObjects),
                        numObjects));
                choiceDialog.setHeaderText(
                        MessageFormat.format(
                                new ChoiceFormat(Strings.CF_REMOVE_OBJECTS_FROM_GROUP_GROUP_CHOICE_DIALOG_HEADER_TEXT).format(numObjects),
                                objectsToRemove
                                    .stream()
                                    .map(BaseObjectUIRepresentation::getShortName)
                                    .toList()));
                Optional<ObjectStringAdapter<ObjectsGroup>> oRes = choiceDialog.showAndWait();
                oRes.ifPresent(osaGroup -> {
                    ObjectsGroup group = osaGroup.getObj();
                    UiController uiController = getUiController();
                    List<IModelChange> changeTrace = new ArrayList<>();
                    group.removeAllObjects(objectsToRemove, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.REMOVE_OBJECTS_FROM_GROUP_CHANGE);
                    AbstractViewBehavior.this.updateActionsListToSelection();
                });
            }
        };
    }

    /**
     * Adds actions when multiple objects are selected.
     */
    protected void addGroupingActionsForSelection(List<BaseObject> selectedObjects, Collection<IContextAction> actions) {
        if (!selectedObjects.isEmpty()) {
            // Create group for selected objects
            if (selectedObjects.size() > 1) {
                actions.add(createGroupAction(selectedObjects));
            }

            // Add selected objects to existing group
            Collection<ObjectsGroup> groups = new ArrayList<>(getPlan().getGroups().values());
            groups.removeAll(selectedObjects); // Don't offer group as target which is contained in the selection
            groups.removeAll(selectedObjects.stream().flatMap(bo -> bo.getGroups().stream()).toList());
            if (!groups.isEmpty() && !selectedObjects.isEmpty()) {
                actions.add(createAddObjectsToGroupAction(selectedObjects));
            }

            // Remove objects from group action
            Collection<ObjectsGroup> possibleRemoveGroups = getCompletelyContainingGroups(selectedObjects);
            if (!possibleRemoveGroups.isEmpty()) {
                actions.add(createRemoveObjectsFromGroupAction(selectedObjects));
            }
        }
    }

    protected IContextAction createRemoveObjectsAction(Collection<? extends BaseObject> rootObjects) {
        int numObjects = rootObjects.size();
        return new IContextAction() {
            @Override
            public String getTitle() {
                if (numObjects == 1) {
                    return MessageFormat.format(Strings.ACTION_GROUND_PLAN_REMOVE_SINGLE_OBJECT_TITLE, BaseObjectUIRepresentation.getShortName(rootObjects.iterator().next()));
                }
                return MessageFormat.format(Strings.ACTION_GROUND_PLAN_REMOVE_OBJECTS_TITLE, numObjects);
            }

            @Override
            public void execute() {
                UiController uiController = getUiController();
                uiController.setSelectedObjectIds(Collections.emptyList());
                List<IModelChange> changeTrace = new ArrayList<>();
                uiController.doRemoveObjects(rootObjects, changeTrace);
                uiController.notifyChange(changeTrace, MessageFormat.format(Strings.REMOVE_OBJECTS_CHANGE, numObjects));
            }
        };
    }
}
