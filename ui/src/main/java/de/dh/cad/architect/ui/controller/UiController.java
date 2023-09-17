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
package de.dh.cad.architect.ui.controller;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.MacroChange;
import de.dh.cad.architect.model.changes.ObjectChange;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.GuideLine.GuideLineDirection;
import de.dh.cad.architect.model.objects.IObjectsContainer;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.ui.ApplicationController;
import de.dh.cad.architect.ui.IConfig;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.ThreeDObject;
import de.dh.cad.architect.ui.objects.AbstractObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.AbstractObjectUIRepresentation.Cardinality;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.ObjectProperties;
import de.dh.cad.architect.ui.objects.ObjectTypesRegistry;
import de.dh.cad.architect.ui.objecttree.ObjectTreeControl;
import de.dh.cad.architect.ui.persistence.UiState;
import de.dh.cad.architect.ui.properties.PropertiesControl;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.MainWindow;
import de.dh.cad.architect.ui.view.ObjectReconcileOperation;
import de.dh.cad.architect.utils.IdGenerator;
import de.dh.utils.fx.SimpleObservableListWrapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

public class UiController {
    protected final IConfig mConfiguration;
    protected final Property<Plan> mPlanProperty;
    protected final SimpleObservableListWrapper<String> mSelectedObjectIds = new SimpleObservableListWrapper<>(new ArrayList<>());
    protected final StringProperty mFocusedObjectId = new SimpleStringProperty(null);
    protected final List<ObjectsChangeHandler> mChangeHandlers = new ArrayList<>();
    protected final List<IObjectContextMenuProvider> mContextMenuProviders = new ArrayList<>();
    protected final ChangeHistory mChangeHistory = new ChangeHistory();
    protected final ObjectProperty<ChangeEntry> mNextUndoOperation = new SimpleObjectProperty<>(null);
    protected final ObjectProperty<ChangeEntry> mNextRedoOperation = new SimpleObjectProperty<>(null);

    // Initialized in initialize(...)
    protected ApplicationController mApplicationController;
    protected MainWindow mMainWindow;

    protected AbstractPlanView<?, ?> mCurrentView = null;

    public UiController(Property<Plan> planProperty, IConfig configuration) {
        mPlanProperty = planProperty;
        mConfiguration = configuration;
    }

    public void initialize(Stage primaryStage, ApplicationController applicationController) {
        mApplicationController = applicationController;
        mMainWindow = MainWindow.create(applicationController, this);
        mMainWindow.show(primaryStage);
        mMainWindow.initializeAfterShow();

        ObjectTreeControl objectTreeControl = mMainWindow.getObjectTreeControl();

        mSelectedObjectIds.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                updateProperties();
                if (mSelectedObjectIds.size() == 1) {
                    mFocusedObjectId.set(mSelectedObjectIds.iterator().next());
                } else {
                    mFocusedObjectId.set(null);
                }
            }
        });
        addChangeHandler(new ObjectsChangeHandler() {
            @Override
            public void objectsRemoved(Collection<BaseObject> removedObjects) {
                objectTreeControl.objectsRemoved(removedObjects);
            }

            @Override
            public void objectsChanged(Collection<BaseObject> changedObjects) {
                objectTreeControl.objectsChanged(changedObjects);
                if (changedObjects.stream().map(bo -> bo.getId()).anyMatch(id -> mSelectedObjectIds.contains(id))) {
                    // A selected object is in the set of changed objects -> protential visible property change
                    updateProperties();
                }
            }

            @Override
            public void objectsAdded(Collection<BaseObject> addedObjects) {
                objectTreeControl.objectsAdded(addedObjects);
            }
        });
        updateProperties();

        ObservableList<String> objectsTreeSelectedObjectIds = objectTreeControl.selectedObjectIds();
        mPlanProperty.addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Plan> observable, Plan oldValue, Plan newValue) {
                mChangeHistory.clear();
                objectsTreeSelectedObjectIds.clear();
                objectTreeControl.setInput(getPlan());
                objectsTreeSelectedObjectIds.addAll(mSelectedObjectIds);
            }
        });

        objectTreeControl.setContextMenuProviders(mContextMenuProviders);

        // Reflect changes of selected objects set to object tree selection
        mSelectedObjectIds.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                if (mSuppressSelectionChanges) {
                    return;
                }
                mSuppressSelectionChanges = true;
                try {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            objectsTreeSelectedObjectIds.addAll(c.getAddedSubList());
                        }
                        if (c.wasRemoved()) {
                            objectsTreeSelectedObjectIds.removeAll(c.getRemoved());
                        }
                    }
                } finally {
                    mSuppressSelectionChanges = false;
                }
            }
        });
        // Reflect changes of object tree selection to selected objects set
        objectsTreeSelectedObjectIds.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                if (mSuppressSelectionChanges) {
                    return;
                }
                mSuppressSelectionChanges = true;
                try {
                    setSelectedObjectIds(objectsTreeSelectedObjectIds);
                } finally {
                    mSuppressSelectionChanges = false;
                }
            }
        });
    }

    public void shutdown() {
        mMainWindow.shutdown();
    }

    public void setCurrentView(AbstractPlanView<?, ?> view) {
        mCurrentView = view;
        updateProperties();
    }

    public UiState getUiState() {
        return new UiState(mMainWindow.getMainWindowState());
    }

    public void setUiState(UiState uiState) {
        mMainWindow.setMainWindowState(uiState.getMainWindowState());
    }

    public ApplicationController getApplicationController() {
        return mApplicationController;
    }

    public void addContextMenuProvider(IObjectContextMenuProvider contextMenuProvider) {
        mContextMenuProviders.add(contextMenuProvider);
        ObjectTreeControl objectTreeControl = mMainWindow.getObjectTreeControl();
        objectTreeControl.setContextMenuProviders(mContextMenuProviders);
    }

    public void removeContextMenuProvider(IObjectContextMenuProvider contextMenuProvider) {
        mContextMenuProviders.remove(contextMenuProvider);
        ObjectTreeControl objectTreeControl = mMainWindow.getObjectTreeControl();
        objectTreeControl.setContextMenuProviders(mContextMenuProviders);
    }

    public List<IObjectContextMenuProvider> getContextMenuProviders() {
        return mContextMenuProviders;
    }

    public IConfig getConfiguration() {
        return mConfiguration;
    }

    public void updateProperties() {
        Plan plan = getPlan();
        List<BaseObject> selectedObjects = mSelectedObjectIds
            .stream()
            .map(id -> plan.getObjectById(id))
            .filter(obj -> obj != null)
            .collect(Collectors.toList());
        updatePropertiesView(selectedObjects);
    }

    protected void updatePropertiesView(List<BaseObject> selectedObjects) {
        PropertiesControl propertiesControl = mMainWindow.getPropertiesControl();
        String placeholder = calculatePropertiesViewPlaceholder(selectedObjects);
        propertiesControl.setPlaceholder(placeholder);
        if (selectedObjects.size() == 0) {
            propertiesControl.setEmptyInput();
        } else if (selectedObjects.size() == 1) {
            BaseObject bo = selectedObjects.get(0);
            AbstractObjectUIRepresentation uiRepresentation = ObjectTypesRegistry.getUIRepresentation(bo.getClass());
            if (uiRepresentation == null) {
                propertiesControl.setEmptyInput();
            } else {
                propertiesControl.setSingleInput(uiRepresentation.getProperties(bo, this));
            }
        } else {
            Collection<ObjectProperties> properties = new ArrayList<>(selectedObjects.size());
            Class<? extends BaseObject> clz = null;
            boolean different = false;
            for (BaseObject bo : selectedObjects) {
                Class<? extends BaseObject> currentClass = bo.getClass();
                AbstractObjectUIRepresentation uiRepresentation = ObjectTypesRegistry.getUIRepresentation(currentClass);
                if (clz == null) {
                    clz = currentClass;
                } else if (!clz.equals(currentClass)) {
                    different = true;
                    break;
                }
                if (uiRepresentation != null) {
                    properties.add(uiRepresentation.getProperties(bo, this));
                }
            }
            if (different || properties.size() != selectedObjects.size()) {
                propertiesControl.setEmptyInput();
            } else {
                propertiesControl.setMultipleInput(properties);
            }
        }
    }

    /**
     * Calculates the string to be displayed as placeholder if we cannot show the properties for the selected
     * object.
     */
    protected String calculatePropertiesViewPlaceholder(List<BaseObject> selectedObjects) {
        int numObjectsSelected = selectedObjects.size();
        if (numObjectsSelected == 0) {
            return Strings.PROPERTIES_NO_OBJECT_SELECTED;
        } else if (numObjectsSelected > 1) {
            Object str = selectedObjects
                    .stream()
                    .collect(Collectors.groupingBy(o -> o.getClass()))
                    .entrySet()
                    .stream()
                    .map(e ->
                        e.getValue().size() + " " + BaseObjectUIRepresentation.getObjectTypeName(e.getKey(), e.getValue().size() == 1 ? Cardinality.Singular : Cardinality.Plural))
                    .collect(Collectors.joining(", "));
            return MessageFormat.format(Strings.PROPERTIES_OBJECTS_SELECTED_X, str);
        } else {
            return Strings.PROPERTIES_NO_PROPERTIES_TO_SHOW;
        }
    }

    protected boolean mSuppressSelectionChanges = false;

    public Property<Plan> planProperty() {
        return mPlanProperty;
    }

    public Plan getPlan() {
        return mPlanProperty.getValue();
    }

    public void setPlan(Plan value) {
        mPlanProperty.setValue(value);
    }

    public AssetManager getAssetManager() {
        return mApplicationController.getAssetManager();
    }

    public StringProperty focusedObjectId() {
        return mFocusedObjectId;
    }

    // Using an ObservableList here instead of an ObservableSet to prevent performance issues with the inperformant SetChangeListener
    public ObservableList<String> selectedObjectIds() {
        return mSelectedObjectIds;
    }

    /**
     * Sets the object ids of the selcted objects.
     */
    public void setSelectedObjectIds(Collection<String> ids) {
        Collection<String> added = CollectionUtils.subtract(ids, mSelectedObjectIds);
        Collection<String> removed = CollectionUtils.subtract(mSelectedObjectIds, ids);
        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }
        mSelectedObjectIds.publicBeginChange();
        if (!removed.isEmpty()) {
            mSelectedObjectIds.removeAll(removed);
        }
        if (!added.isEmpty()) {
            mSelectedObjectIds.addAll(added);
        }
        mSelectedObjectIds.publicEndChange();
    }

    public void setSelectedObjectId(String id) {
        setSelectedObjectIds(Arrays.asList(id));
    }

    public Collection<? extends BaseObject> getObjectsById(Collection<String> ids) {
        Plan plan = getPlan();
        return ids.stream().map(id -> plan.getObjectById(id)).collect(Collectors.toList());
    }

    public void addChangeHandler(ObjectsChangeHandler handler) {
        mChangeHandlers.add(handler);
    }

    public void removeChangeHandler(ObjectsChangeHandler handler) {
        mChangeHandlers.remove(handler);
    }

    public void removeObject(BaseObject object) {
        List<IModelChange> changeTrace = new ArrayList<>();
        doRemoveObject(object, changeTrace);
        notifyChange(changeTrace, MessageFormat.format(Strings.REMOVE_OBJECTS_CHANGE, 1));
    }

    public void removeObjects(Collection<? extends BaseObject> objects) {
        List<IModelChange> changeTrace = new ArrayList<>();
        doRemoveObjects(objects, changeTrace);
        notifyChange(changeTrace, MessageFormat.format(Strings.REMOVE_OBJECTS_CHANGE, objects.size()));
    }

    public void doRemoveObjects(Collection<? extends BaseObject> objects, List<IModelChange> changeTrace) {
        for (BaseObject object : objects) {
            doRemoveObject(object, changeTrace);
        }
    }

    public void doRemoveObject(BaseObject object, List<IModelChange> changeTrace) {
        if (object instanceof ObjectsGroup group) {
            // Delete objects in group recursively - this behavior is different than the behavior implemented in model
            doRemoveObjects(new ArrayList<>(group.getGroupedObjects()), changeTrace);
        }
        // Remove children
        if (object instanceof IObjectsContainer parent) {
            for (BaseObject child : new ArrayList<>(parent.getOwnedChildren())) {
                doRemoveObject(child, changeTrace);
            }
        }
        // Undock object
        if (object instanceof BaseAnchoredObject bao) {
            for (Anchor anchor : new ArrayList<>(bao.getAnchors())) {
                doRemoveAnchorFromDock(anchor, changeTrace);
            }
        }
        object.delete(changeTrace);
    }

    public enum DockConflict {
        SourceNotAHandle;
    }

    public static class DockConflictDescription {
        protected final Anchor mSource;
        protected final Anchor mDesignatedTarget;
        protected final DockConflict mType;

        public DockConflictDescription(Anchor source, Anchor designatedTarget, DockConflict type) {
            mSource = source;
            mDesignatedTarget = designatedTarget;
            mType = type;
        }

        public DockConflict getType() {
            return mType;
        }

        public Anchor getSource() {
            return mSource;
        }

        public Anchor getDesignatedTarget() {
            return mDesignatedTarget;
        }
    }

    public enum DockConflictStrategy {
        SkipDock, Exception;
    }

    /**
     * Creates a dock conflict result for the situation that the source object is not a handle anchor and thus cannot be docked.
     */
    protected static Optional<Collection<DockConflictDescription>> dockConflictSourceNotAHandle(Anchor source, Anchor designatedTarget) {
        return Optional.of(Arrays.asList(new DockConflictDescription(source, designatedTarget, DockConflict.SourceNotAHandle)));
    }

    /**
     * Creates a dock result without conflicts.
     */
    protected static Optional<Collection<DockConflictDescription>> dockOk() {
        return Optional.empty();
    }

    protected Collection<BaseAnchoredObject> getAllDockedObjects(BaseAnchoredObject obj) {
        Collection<BaseAnchoredObject> result = new ArrayList<>(obj.getAnchors()
                        .stream()
                        .flatMap(a -> a.getAllDockedAnchors().stream())
                        .map(Anchor::getAnchorOwner)
                        .toList());
        result.remove(obj);
        return result;
    }

    public void removeAnchorFromDock(Anchor anchor) {
        List<IModelChange> changeTrace = new ArrayList<>();
        doRemoveAnchorFromDock(anchor, changeTrace);
        notifyChange(changeTrace, Strings.REMOVE_ANCHOR_FROM_DOCK_CHANGE);
    }

    /**
     * Removes a single anchor from a dock, if present, retaining all other anchors in the dock.
     * After this method returns, the given anchor is not docked any more nor has it any more dock slaves.
     * If there were dock slaves before, this method attaches them to the former dock master or, if not present,
     * to the first slave dock anchor, if possible.
     * Attention: This method might not succeed with attaching all objects to the resulting dock hierarchy,
     * e.g. one of the anchor's slaves might have the same owner then the anchors's master, which would cause
     * a conflict. So the dock hierarchy will only be restored as far as possible without causing conflicts.
     */
    public void doRemoveAnchorFromDock(Anchor anchor, List<IModelChange> changeTrace) {
        Collection<BaseAnchoredObject> reconcileObjects = doRemoveAnchorFromDock_Internal(anchor, changeTrace);
        ObjectReconcileOperation oro = new ObjectReconcileOperation("Cleanup after removing anchor from dock operation", reconcileObjects);
        doReconcileObjects(oro, changeTrace);
    }

    /**
     * Removes the given anchor from its dock as described for {@link #doRemoveAnchorFromDock(Anchor, List)} but leaves out the
     * object reconcile operation at the end. Instead, returns all objects which need to be reconciled later.
     */
    public Collection<BaseAnchoredObject> doRemoveAnchorFromDock_Internal(Anchor anchor, List<IModelChange> changeTrace) {
        Collection<BaseAnchoredObject> reconcileObjects = anchor.getAllDockOwners();
        Optional<Anchor> oDockMaster = anchor.getDockMaster();
        if (oDockMaster.isPresent()) {
            Anchor dockMaster = oDockMaster.get();
            anchor.setDockMaster(null, changeTrace);
            // Move our dock slaves to our dock master
            for (Anchor dockSlave : new ArrayList<>(anchor.getDockSlaves())) {
                reconcileObjects.addAll(doDock_Internal(dockSlave, dockMaster, DockConflictStrategy.SkipDock, changeTrace));
            }
        } else {
            Iterator<Anchor> iterator = new ArrayList<>(anchor.getDockSlaves()).iterator();
            if (iterator.hasNext()) {
                Anchor newDockMaster = iterator.next();
                newDockMaster.setDockMaster(null, changeTrace);
                while (iterator.hasNext()) {
                    Anchor dockSlave = iterator.next();
                    dockSlave.undockFromDockMaster(changeTrace); // Force undocking, even if the dock operation in the next line runs in a conflict (and thus might fail)
                    reconcileObjects.addAll(doDock_Internal(dockSlave, newDockMaster, DockConflictStrategy.SkipDock, changeTrace));
                }
            }
        }
        return reconcileObjects;
    }

    /**
     * Binds the position of the handle anchor and all its dependent anchors to the given target anchor.
     * @param handle Source anchor to bind to {@code targetAnchor}. This anchor must not be {@link Anchor#isManaged() managed}.
     * @param targetAnchor Anchor to which the handle anchor should be bound.
     * @param conflictStrategy Strategy to choose if the dock process is not possible.
     */
    public void dock(Anchor handle, Anchor targetAnchor, DockConflictStrategy conflictStrategy) {
        List<IModelChange> changeTrace = new ArrayList<>();
        doDock(handle, targetAnchor, conflictStrategy, changeTrace);
        notifyChange(changeTrace, Strings.DOCK_ANCHOR_CHANGE);
    }

    public Optional<Collection<DockConflictDescription>> checkDockConflicts(Anchor sourceAnchor, Anchor targetAnchor) {
        if (!sourceAnchor.isHandle()) {
            return dockConflictSourceNotAHandle(sourceAnchor, targetAnchor);
        }

        return dockOk();
    }

    public void doDock(Anchor sourceHandleAnchor, Anchor targetAnchor, DockConflictStrategy conflictStrategy, List<IModelChange> changeTrace) {
        Collection<BaseAnchoredObject> reconcileObjects = doDock_Internal(sourceHandleAnchor, targetAnchor, conflictStrategy, changeTrace);

        ObjectReconcileOperation oro = new ObjectReconcileOperation("Dock anchor", reconcileObjects);
        doReconcileObjects(oro, changeTrace);
    }

    public Collection<BaseAnchoredObject> doDock_Internal(Anchor sourceHandleAnchor, Anchor targetAnchor, DockConflictStrategy conflictStrategy, List<IModelChange> changeTrace) {
        if (sourceHandleAnchor.getDockMaster()
                        .map(dm -> Boolean.valueOf(dm.equals(targetAnchor))) // Already docked to target anchor
                        .orElse(Boolean.FALSE)) {
            // Already docked to target anchor
            return Collections.emptyList();
        }
        Optional<Collection<DockConflictDescription>> oConflicts = checkDockConflicts(sourceHandleAnchor, targetAnchor);
        if (oConflicts.isPresent()) {
            switch (conflictStrategy) {
            case SkipDock:
                return Collections.emptyList();
            case Exception:
                throw new RuntimeException("Cannot dock source anchor <" + sourceHandleAnchor + "> to anchor dock of target anchor <" + targetAnchor + ">");
            }
        }

        sourceHandleAnchor.undockFromDockMaster(changeTrace);
        IPosition targetPosition = targetAnchor.getPosition();
        sourceHandleAnchor.setDockMaster(targetAnchor, changeTrace);

        return doSetDockPosition_Internal(sourceHandleAnchor, targetPosition, changeTrace);
    }

    /**
     * Undocks the given anchor from its dock master, retaining all its dock slaves docked, if any.
     * To remove an anchor completely from a dock, call {@link #doRemoveAnchorFromDock(Anchor, ChangeSet)}.
     */
    public void doUndock(Anchor handleToUndock, List<IModelChange> changeTrace) {
        List<BaseAnchoredObject> formerDockOwners = handleToUndock.getAllDockOwners();
        handleToUndock.undockFromDockMaster(changeTrace);

        ObjectReconcileOperation oro = new ObjectReconcileOperation("Undock operation", formerDockOwners);
        doReconcileObjects(oro, changeTrace);
    }

    public void doUndockIfDocked(Anchor anchorToUndock, List<IModelChange> changeTrace) {
        if (anchorToUndock.getDockMaster().isPresent()) {
            doUndock(anchorToUndock, changeTrace);
        }
    }

    public ObjectsGroup groupObjects(Collection<BaseObject> objects, String groupName) {
        List<IModelChange> changeTrace = new ArrayList<>();
        ObjectsGroup group = doGroupObjects(groupName, objects, changeTrace);
        notifyChange(changeTrace, MessageFormat.format(Strings.GROUP_OBJECTS_CHANGE, objects.size()));
        return group;
    }

    /**
     * Groups the given objects in a new group of the given name.
     * This is a convenience method for just creating a new {@link ObjectsGroup} and adding objects to it.
     * @return Group of objects.
     */
    public ObjectsGroup doGroupObjects(String groupName, Collection<BaseObject> objects, List<IModelChange> changeTrace) {
        if (objects.isEmpty()) {
            throw new IllegalStateException("Cannot create an empty objects group");
        }
        ObjectsGroup group = ObjectsGroup.create(IdGenerator.generateUniqueId(ObjectsGroup.class), groupName, getPlan(), changeTrace);
        for (BaseObject object : objects) {
            group.addObject(object, changeTrace);
        }
        return group;
    }

    // The ungroup operation is nothing more than just deleting the group; for clarity, we retain this operation
    public void ungroup(ObjectsGroup group) {
        List<IModelChange> changeTrace = new ArrayList<>();
        group.removeAllGroupedObjects(changeTrace);
        group.delete(changeTrace);
        notifyChange(changeTrace, Strings.UNGROUP_OBJECTS_CHANGE);
    }

    public void setObjectsVisibilityByIds(Collection<String> objIds, boolean hidden) {
        Plan plan = getPlan();
        setObjectsVisibility(objIds
            .stream()
            .map(objId -> plan.getObjectById(objId))
            .collect(Collectors.toList()), hidden);
    }

    public void setObjectsVisibility(Collection<? extends BaseObject> objs, boolean hidden) {
        List<IModelChange> changeTrace = new ArrayList<>();
        doSetObjectsVisibility(objs, hidden, changeTrace);
        notifyChange(changeTrace, Strings.SET_OBJECTS_VISIBILITY_CHANGE);
    }

    public void doSetObjectsVisibility(Collection<? extends BaseObject> objs, boolean hidden, List<IModelChange> changeTrace) {
        for (BaseObject obj : objs) {
            if (obj.isHidden() != hidden) {
                obj.setHidden(hidden, changeTrace);
            }
            if (obj instanceof BaseAnchoredObject bao) {
                doSetObjectsVisibility(bao.getAnchors(), hidden, changeTrace);
            }
            if (obj instanceof ObjectsGroup group) {
                doSetObjectsVisibility(group.getGroupedObjects(), hidden, changeTrace);
            }
        }
    }

    public void doSetHandleAnchorPosition(Anchor handleAnchor, IPosition position, List<IModelChange> changeTrace) {
        if (!handleAnchor.isHandle()) {
            throw new RuntimeException("Cannot set position of anchor <" + handleAnchor + ">, it is not a handle");
        }
        Optional<Anchor> oDockMaster = handleAnchor.getDockMaster();
        if (oDockMaster.isPresent()) {
            throw new RuntimeException("Cannot set position of anchor <" + handleAnchor + ">, it is docked to anchor <" + oDockMaster.get() + ">");
        }
        doSetDockPosition(handleAnchor, position, changeTrace);
    }

    public class MergeableSetAnchorDockPositionChange extends ObjectChange {
        protected final Anchor mRootMasterOfDock;
        protected final IPosition mOldPosition;

        public MergeableSetAnchorDockPositionChange(Anchor rootMasterOfDock, IPosition oldPosition, List<IModelChange> setAnchorDockInnerChanges) {
            mRootMasterOfDock = rootMasterOfDock;
            mOldPosition = oldPosition;

            MacroChange consolidatedChange = MacroChange.create(setAnchorDockInnerChanges, false);
            objectsAdded(consolidatedChange.getAdditions());
            objectsModified(consolidatedChange.getModifications());
            objectsRemoved(consolidatedChange.getRemovals());
        }

        @Override
        public Optional<IModelChange> tryMerge(IModelChange oldChange) {
            if (oldChange instanceof MergeableSetAnchorDockPositionChange sadpc && sadpc.mRootMasterOfDock.equals(mRootMasterOfDock)) {
                return Optional.of(oldChange);
            }
            return Optional.empty();
        }

        @Override
        public void undo(List<IModelChange> undoChangeTrace) {
            doSetDockPosition(mRootMasterOfDock, mOldPosition, undoChangeTrace);
        }
    }

    public void setDockPosition(Anchor anchor, IPosition position, boolean tryMergeChange) {
        List<IModelChange> changeTrace = new ArrayList<>();
        doSetDockPosition(anchor, position, changeTrace);
        notifyChange(changeTrace, Strings.SET_DOCK_POSITION_CHANGE, tryMergeChange);
    }

    protected void doSetDockPosition(Anchor anchor, IPosition position, List<IModelChange> changeTrace) {
        doSetDockPosition_Internal(anchor, position, changeTrace, true);
    }

    /**
     * Sets all positions of anchors docked to the same dock of the given anchor.
     * @param generateMergableChange Will generate a mergeable change for repeated calls to this method. This is wanted for
     * operations where the undo operation should skip repeated dock positions change, for example in case of a drag operation.
     * ATTENTION: With parameter {@code generateMergeableChange} set to {@code true}, this method assumes that all docked
     * anchors already had the same (dock) position. For the initial docking operation, that parameter must be set to
     * {@code false}.
     */
    protected void doSetDockPosition_Internal(Anchor anchor, IPosition position, List<IModelChange> changeTrace, boolean generateMergeableChange) {
        IPosition oldPosition = anchor.getPosition();
        Anchor rootMasterOfDock = anchor.getRootMasterOfAnchorDock();
        List<IModelChange> innerChangeTrace = new ArrayList<>();

        Collection<BaseAnchoredObject> reconcileObjects = doSetDockPosition_Internal(anchor, position, innerChangeTrace);

        ObjectReconcileOperation oro = new ObjectReconcileOperation("Set dock position", reconcileObjects);
        doReconcileObjects(oro, innerChangeTrace);

        if (generateMergeableChange) {
            changeTrace.add(new MergeableSetAnchorDockPositionChange(rootMasterOfDock, oldPosition, innerChangeTrace));
        } else {
            changeTrace.addAll(innerChangeTrace);
        }
    }

    protected Collection<BaseAnchoredObject> doSetDockPosition_Internal(Anchor anchor, IPosition position, List<IModelChange> changeTrace) {
        Collection<BaseAnchoredObject> reconcileObjects = new ArrayList<>();
        for (Anchor changeAnchor : anchor.getAllDockedAnchors()) {
            BaseAnchoredObject owner = changeAnchor.getAnchorOwner();
            changeAnchor.setPosition(ObjectReconcileOperation.calculateTargetPositionForAnchor(changeAnchor, position), changeTrace);
            reconcileObjects.add(owner);
        }

        return reconcileObjects;
    }

    public void setHandleAnchorPosition(Anchor anchor, IPosition position, boolean tryMergeChange) {
        List<IModelChange> changeTrace = new ArrayList<>();
        doSetHandleAnchorPosition(anchor, position, changeTrace);
        notifyChange(changeTrace, Strings.SET_HANDLE_ANCHOR_POSITION, tryMergeChange);
    }

    public void doReconcileObjects(ObjectReconcileOperation oro, List<IModelChange> changeTrace) {
        oro.reconcileObjects(changeTrace);
    }

    public void createGuideLine(GuideLineDirection direction, Length position) {
        List<IModelChange> changeTrace = new ArrayList<>();
        GuideLine.create(BaseObjectUIRepresentation.generateSimpleName(getPlan().getGuideLines().values(), GuideLine.class),
            direction, position, getPlan(), changeTrace);
        notifyChange(changeTrace, Strings.CREATE_GUIDE_LINE_CHANGE);
    }

    public void deleteGuideLine(GuideLine guideLine) {
        removeObject(guideLine);
    }

    public void setGuideLinePosition(GuideLine guideLine, Length position) {
        List<IModelChange> changeTrace = new ArrayList<>();
        guideLine.setPosition(position, changeTrace);
        notifyChange(changeTrace, Strings.GUIDE_LINE_SET_PROPERTY_CHANGE);
    }

    public void setWallBevelTypeOfAnchorDock(Anchor anchorDock, WallBevelType wallBevel) {
        List<IModelChange> changeTrace = new ArrayList<>();
        doSetWallBevelTypeOfAnchorDock(anchorDock, wallBevel, changeTrace);
        notifyChange(changeTrace, Strings.WALL_SET_PROPERTY_CHANGE);
    }

    public void doSetWallBevelTypeOfAnchorDock(Anchor anchorDock, WallBevelType wallBevel, List<IModelChange> changeTrace) {
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(anchorDock, wallBevel, changeTrace);
        for (Anchor dockedAnchor : anchorDock.getAllDockedAnchors()) {
            BaseAnchoredObject owner = dockedAnchor.getAnchorOwner();
            if (!(owner instanceof Wall)) {
                continue;
            }
            Wall wall = (Wall) owner;
            wall.healObject(Collections.singleton(Wall.HEAL_OUTLINE), changeTrace);
        }
    }

    public SupportObject createNewSupportObject(SupportObjectDescriptor supportObjectDescriptor, Position2D pos) throws IOException {
        List<IModelChange> changeTrace = new ArrayList<>();
        SupportObject result = doCreateNewSupportObject(supportObjectDescriptor, pos, changeTrace);
        notifyChange(changeTrace, Strings.SUPPORT_OBJECT_CREATE_CHANGE);
        return result;
    }

    public SupportObject doCreateNewSupportObject(SupportObjectDescriptor supportObjectDescriptor, Position2D pos, List<IModelChange> changeTrace) throws IOException {
        Plan plan = getPlan();
        AssetLoader assetLoader = getAssetManager().buildAssetLoader();
        ThreeDObject obj = assetLoader.loadSupportObject3DObject(supportObjectDescriptor, Optional.empty(), false);
        if (obj == null) {
            throw new IOException("Error creating support object");
        }
        Set<String> meshIds = obj.getSurfaceMeshViews().stream().map(mv -> mv.getId()).collect(Collectors.toSet());
        SupportObject result = SupportObject.create(
            BaseObjectUIRepresentation.generateSimpleName(getPlan().getSupportObjects().values(), supportObjectDescriptor.getName()),
            supportObjectDescriptor.getSelfRef(), pos,
            new Dimensions2D(supportObjectDescriptor.getWidth(), supportObjectDescriptor.getDepth()),
            supportObjectDescriptor.getHeight(), 0, supportObjectDescriptor.getElevation(), meshIds, plan, changeTrace);
        return result;
    }

    public void notifyChange(List<IModelChange> combinedChange, String changeDescription) {
        notifyChange(combinedChange, changeDescription, false);
    }

    public void notifyChange(List<IModelChange> combinedChange, String changeDescription, boolean tryMergeChange) {
        if (combinedChange.size() == 1) {
            notifyChange(combinedChange.get(0), changeDescription, tryMergeChange);
        } else {
            notifyChange(MacroChange.create(combinedChange, tryMergeChange), changeDescription, tryMergeChange);
        }
    }

    public void notifyChange(IModelChange change, String changeDescription) {
        notifyChange(change, changeDescription, false);
    }

    public void notifyChange(IModelChange change, String changeDescription, boolean tryMergeChange) {
        ChangeEntry changeEntry = new ChangeEntry(change, changeDescription);
        mChangeHistory.pushChange(changeEntry, tryMergeChange);
        checkUndoRedo();
        fireChanges(change);
    }

    public ReadOnlyObjectProperty<ChangeEntry> nextUndoOperationProperty() {
        return mNextUndoOperation;
    }

    public ReadOnlyObjectProperty<ChangeEntry> nextRedoOperationProperty() {
        return mNextRedoOperation;
    }

    public boolean canUndo() {
        return mNextUndoOperation.getValue() != null;
    }

    public boolean canRedo() {
        return mNextRedoOperation.getValue() != null;
    }

    protected void checkUndoRedo() {
        mNextUndoOperation.setValue(mChangeHistory.tryPeekNextUndoChange().orElse(null));
        mNextRedoOperation.setValue(mChangeHistory.tryPeekNextRedoChange().orElse(null));
    }

    public void undo() {
        ChangeEntry undoChange = mChangeHistory.undo();
        checkUndoRedo();
        fireChanges(undoChange.getModelChange());
    }

    public void redo() {
        ChangeEntry redoChange = mChangeHistory.redo();
        checkUndoRedo();
        fireChanges(redoChange.getModelChange());
    }

    protected void fireChanges(IModelChange change) {
        Collection<BaseObject> additions = change.getAdditions();
        Collection<BaseObject> removals = change.getRemovals();
        boolean objectSetUnChanged = additions.isEmpty() && removals.isEmpty();

        if (objectSetUnChanged) {
            // "Simple" change which won't impact our selected object ids
            fireObjectsChanged(change.getModifications());
        } else {
            // "Complex" change with additions and/or removals which potentially can impact our selected objet ids
            mSelectedObjectIds.publicBeginChange();
            // TODO: We should also begin/end change of object tree here
            try {
                fireObjectsRemoved(removals);
                fireObjectsAdded(additions);
                fireObjectsChanged(change.getModifications());
                mSelectedObjectIds.removeAll(removals
                    .stream()
                    .map(BaseObject::getId)
                    .toList());
            } finally {
                mSelectedObjectIds.publicEndChange();
            }
        }
    }

    protected void fireObjectsAdded(Collection<? extends BaseObject> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        Collection<BaseObject> objs = Collections.unmodifiableCollection(objects);
        for (ObjectsChangeHandler handler : mChangeHandlers) {
            handler.objectsAdded(objs);
        }
    }

    protected void fireObjectsRemoved(Collection<? extends BaseObject> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        Collection<BaseObject> objs = Collections.unmodifiableCollection(objects);
        for (ObjectsChangeHandler handler : mChangeHandlers) {
            handler.objectsRemoved(objs);
        }
    }

   protected void fireObjectsChanged(Collection<? extends BaseObject> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }
        Collection<BaseObject> objs = Collections.unmodifiableCollection(objects);
        for (ObjectsChangeHandler handler : mChangeHandlers) {
            handler.objectsChanged(objs);
        }
    }

   public static Collection<BaseObject> unwrapGroups(Collection<BaseObject> objects) {
       Collection<BaseObject> result = new ArrayList<>(objects.size() * 2);
       for (BaseObject obj : objects) {
           if (obj instanceof ObjectsGroup group) {
               result.addAll(unwrapGroups(group.getGroupedObjects()));
           } else {
               result.add(obj);
           }
       }
       return result;
   }
}
