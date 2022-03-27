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

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
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
import javafx.beans.property.Property;
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
                updateProperties();
                objectTreeControl.objectsRemoved(removedObjects);
            }

            @Override
            public void objectsChanged(Collection<BaseObject> changedObjects) {
                updateProperties();
                objectTreeControl.objectsChanged(changedObjects);
            }

            @Override
            public void objectsAdded(Collection<BaseObject> addedObjects) {
                updateProperties();
                objectTreeControl.objectsAdded(addedObjects);
            }
        });
        updateProperties();

        ObservableList<String> objectsTreeSelectedObjectIds = objectTreeControl.selectedObjectIds();
        mPlanProperty.addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Plan> observable, Plan oldValue, Plan newValue) {
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
        ChangeSet changeSet = new ChangeSet();
        doRemoveObject(object, changeSet);
        fireChanges(changeSet);
    }

    public void removeObjects(Collection<? extends BaseObject> objects) {
        ChangeSet changeSet = new ChangeSet();
        doRemoveObjects(objects, changeSet);
        fireChanges(changeSet);
    }

    public void doRemoveObjects(Collection<? extends BaseObject> objects, ChangeSet changeSet) {
        mSelectedObjectIds.publicBeginChange();
        try {
            for (BaseObject object : objects) {
                doRemoveObject(object, changeSet);
            }
        } finally {
            mSelectedObjectIds.publicEndChange();
        }
    }

    public void doRemoveObject(BaseObject object, ChangeSet changeSet) {
        Collection<BaseAnchoredObject> allDockedObjects = new ArrayList<>();
        mSelectedObjectIds.publicBeginChange();
        try {
            if (object instanceof ObjectsGroup group) {
                // Delete objects in group recursively - this behavior is different than the behavior implemented in model
                Collection<BaseObject> objectsInGroup = group.getGroupedObjects();
                for (BaseObject obj : new ArrayList<>(objectsInGroup)) {
                    doRemoveObject(obj, changeSet);
                }
            }
            // Remove children
            if (object instanceof IObjectsContainer parent) {
                for (BaseObject child : new ArrayList<>(parent.getOwnedChildren())) {
                    doRemoveObject(child, changeSet);
                }
            }
            // Undock object
            if (object instanceof BaseAnchoredObject bao) {
                for (Anchor anchor : new ArrayList<>(bao.getAnchors())) {
                    doRemoveAnchorFromDock(anchor, changeSet);
                }
            }
            Collection<? extends BaseObject> deletedObjects = object.delete(changeSet);
            allDockedObjects.removeAll(deletedObjects);
            Collection<String> deletedIds = deletedObjects
                .stream()
                .map(obj -> obj.getId())
                .collect(Collectors.toList());
            mSelectedObjectIds.removeAll(deletedIds);
        } finally {
            mSelectedObjectIds.publicEndChange();
        }
        ObjectReconcileOperation oro = new ObjectReconcileOperation("Cleanup after remove operation", allDockedObjects);
        doReconcileObjects(oro, changeSet);
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
        ChangeSet changeSet = new ChangeSet();
        doRemoveAnchorFromDock(anchor, changeSet);
        notifyChanges(changeSet);
    }

    /**
     * Removes a single anchor from a dock, if present, retaining all other anchors in the dock.
     * After this method returns, the given anchor is not docked any more nor has it any more dock slaves.
     * If there were dock slaves before, this method attaches them to the former dock master or, if not present,
     * to the first slave dock anchor.
     */
    public void doRemoveAnchorFromDock(Anchor anchor, ChangeSet changeSet) {
        Optional<Anchor> oDockMaster = anchor.getODockMaster();
        if (oDockMaster.isPresent()) {
            Anchor dockMaster = oDockMaster.get();
            doUndockIfDocked(anchor, changeSet);
            // Move our dock slaves to our dock master
            for (Anchor dockSlave : new ArrayList<>(anchor.getDockSlaves())) {
                doDock(dockSlave, dockMaster, changeSet);
            }
        } else {
            Iterator<Anchor> iterator = new ArrayList<>(anchor.getDockSlaves()).iterator();
            if (iterator.hasNext()) {
                Anchor newDockMaster = iterator.next();
                doUndock(newDockMaster, changeSet);
                while (iterator.hasNext()) {
                    Anchor dockSlave = iterator.next();
                    doDock(dockSlave, newDockMaster, changeSet);
                }
            }
        }
    }

    /**
     * Binds the position of the handle anchor and all its dependent anchors to the given target anchor.
     * @param handle Source anchor to bind to {@code targetAnchor}. This anchor must not be {@link Anchor#isManaged() managed}.
     * @param targetAnchor Anchor to which the handle anchor should be bound.
     */
    public void dock(Anchor handle, Anchor targetAnchor) {
        ChangeSet changeSet = new ChangeSet();
        doDock(handle, targetAnchor, changeSet);
        fireChanges(changeSet);
    }

    public boolean canDock(Anchor sourceAnchor, Anchor targetAnchor) {
        if (!sourceAnchor.isHandle()) {
            return false;
        }
        // Those would be contained in the resulting dock from target side
        Collection<BaseAnchoredObject> targetDockOwners = targetAnchor.getAllDockOwners();

        // Those would be contained in the resulting dock from source side
        Collection<BaseAnchoredObject> sourceDockSlaveOwners = sourceAnchor.getAllDockedAnchorsDownStream()
                        .stream()
                        .map(Anchor::getAnchorOwner)
                        .collect(Collectors.toList());
        if (!CollectionUtils.intersection(targetDockOwners, sourceDockSlaveOwners).isEmpty()) {
            // Docking the anchors would connect two anchors of the same owner object
            return false;
        }
        return true;
    }

    public void doDock(Anchor sourceHandleAnchor, Anchor targetAnchor, ChangeSet changeSet) {
        if (!canDock(sourceHandleAnchor, targetAnchor)) {
            throw new RuntimeException("Cannot dock source anchor <" + sourceHandleAnchor + "> to anchor dock of target anchor <" + targetAnchor + ">");
        }

        changeSet.changed(sourceHandleAnchor);
        changeSet.changed(targetAnchor);
        changeSet.changed(sourceHandleAnchor.getAnchorOwner());
        changeSet.changed(targetAnchor.getAnchorOwner());

        sourceHandleAnchor.getODockMaster().ifPresent(dockMaster -> {
            dockMaster.getDockSlaves().remove(sourceHandleAnchor);
            changeSet.changed(dockMaster);
            changeSet.changed(dockMaster.getAnchorOwner());
        });
        IPosition targetPosition = targetAnchor.getPosition();
        sourceHandleAnchor.setODockMaster(Optional.of(targetAnchor));
        targetAnchor.getDockSlaves().add(sourceHandleAnchor);

        doSetDockPosition(sourceHandleAnchor, targetPosition, changeSet);
    }

    /**
     * Undocks the given anchor from its dock master, retaining all its dock slaves docked, if any.
     * To remove an anchor completely from a dock, call {@link #doRemoveAnchorFromDock(Anchor, ChangeSet)}.
     */
    public void doUndock(Anchor handleToUndock, ChangeSet changeSet) {
        Collection<Anchor> allDockedAnchors = handleToUndock.getAllDockedAnchors();
        handleToUndock.getODockMaster().ifPresent(dockMaster -> {
            dockMaster.getDockSlaves().remove(handleToUndock);
        });
        handleToUndock.setODockMaster(Optional.empty());

        Collection<BaseAnchoredObject> owners = new ArrayList<>();
        for (Anchor changeAnchor : allDockedAnchors) {
            BaseAnchoredObject owner = changeAnchor.getAnchorOwner();
            changeSet.changed(changeAnchor);
            changeSet.changed(owner);
            owners.add(owner);
        }

        ObjectReconcileOperation oro = new ObjectReconcileOperation("Undock operation", owners);
        doReconcileObjects(oro, changeSet);
    }

    public void undock(Anchor handle) {
        ChangeSet changeSet = new ChangeSet();
        doUndock(handle, changeSet);
        fireChanges(changeSet);
    }

    public void doUndockIfDocked(Anchor anchorToUndock, ChangeSet changeSet) {
        if (anchorToUndock.getODockMaster().isPresent()) {
            doUndock(anchorToUndock, changeSet);
        }
    }

    /**
     * Transfers the dock situation of the source anchor to the target anchor, i.e. docks
     * the target anchor to the dock master of source and docks all dock slaves from source
     * to the target anchor.
     */
    public void doTransferDocks(Anchor source, Anchor target, ChangeSet changeSet) {
        for (Anchor dockedAnchor : new ArrayList<>(source.getDockSlaves())) {
            doDock(dockedAnchor, target, changeSet);
        }
        source.getODockMaster().ifPresent(sourceDockMaster -> {
            doDock(target, sourceDockMaster, changeSet);
        });
    }

    public ObjectsGroup groupObjects(Collection<BaseObject> objects, String groupName) {
        ChangeSet changeSet = new ChangeSet();
        ObjectsGroup group = doGroupObjects(groupName, objects, changeSet);
        fireChanges(changeSet);
        return group;
    }

    /**
     * Groups the given objects in a new group of the given name.
     * This is a convenience method for just creating a new {@link ObjectsGroup} and adding objects to it.
     * @return Group of objects.
     */
    public ObjectsGroup doGroupObjects(String groupName, Collection<BaseObject> objects, ChangeSet changeSet) {
        if (objects.isEmpty()) {
            throw new IllegalStateException("Cannot create an empty objects group");
        }
        ObjectsGroup group = new ObjectsGroup(IdGenerator.generateUniqueId(ObjectsGroup.class), groupName, getPlan(), changeSet);
        for (BaseObject object : objects) {
            group.addObject(object, changeSet);
        }
        changeSet.changed(group);
        return group;
    }

    // The ungroup operation is nothing more than just deleting the group; for clarity, we retain this operation
    public void ungroup(ObjectsGroup group) {
        ChangeSet changeSet = new ChangeSet();
        group.removeAllGroupedObjects(changeSet);
        fireChanges(changeSet);
    }

    public void setObjectsVisibilityFromId(Collection<String> objIds, boolean hidden) {
        Plan plan = getPlan();
        setObjectsVisibility(objIds
            .stream()
            .map(objId -> plan.getObjectById(objId))
            .collect(Collectors.toList()), hidden);
    }

    public void setObjectsVisibility(Collection<? extends BaseObject> objs, boolean hidden) {
        ChangeSet changeSet = new ChangeSet();
        doSetObjectsVisibility(objs, hidden, changeSet);
        fireChanges(changeSet);
    }

    public void doSetObjectsVisibility(Collection<? extends BaseObject> objs, boolean hidden, ChangeSet changeSet) {
        for (BaseObject obj : objs) {
            if (obj.isHidden() != hidden) {
                obj.setHidden(hidden);
                changeSet.changed(obj);
            }
            if (obj instanceof BaseAnchoredObject bao) {
                doSetObjectsVisibility(bao.getAnchors(), hidden, changeSet);
            }
            if (obj instanceof ObjectsGroup group) {
                doSetObjectsVisibility(group.getGroupedObjects(), hidden, changeSet);
            }
        }
    }

    public void doSetHandleAnchorPosition(Anchor handleAnchor, IPosition position, ChangeSet changeSet) {
        if (!handleAnchor.isHandle()) {
            throw new RuntimeException("Cannot set position of anchor <" + handleAnchor + ">, it is not a handle");
        }
        Optional<Anchor> oDockMaster = handleAnchor.getODockMaster();
        if (oDockMaster.isPresent()) {
            throw new RuntimeException("Cannot set position of anchor <" + handleAnchor + ">, it is docked to anchor <" + oDockMaster.get() + ">");
        }
        doSetDockPosition(handleAnchor, position, changeSet);
    }

    protected void doSetDockPosition(Anchor anchor, IPosition position, ChangeSet changeSet) {
        Collection<BaseAnchoredObject> owners = new ArrayList<>();
        for (Anchor changeAnchor : anchor.getAllDockedAnchors()) {
            BaseAnchoredObject owner = changeAnchor.getAnchorOwner();
            changeAnchor.setPosition(ObjectReconcileOperation.calculateTargetPositionForAnchor(changeAnchor, position));
            changeSet.changed(changeAnchor);
            changeSet.changed(owner);
            owners.add(owner);
        }

        ObjectReconcileOperation oro = new ObjectReconcileOperation("Set Dock Position", owners);
        doReconcileObjects(oro, changeSet);
    }

    public void setHandleAnchorPosition(Anchor anchor, IPosition position) {
        ChangeSet changeSet = new ChangeSet();
        doSetHandleAnchorPosition(anchor, position, changeSet);
        fireChanges(changeSet);
    }

    public void doReconcileObjects(ObjectReconcileOperation oro, ChangeSet changeSet) {
        oro.reconcileObjects(changeSet);
    }

    public void reconcileObjects(ObjectReconcileOperation oro) {
        ChangeSet changeSet = new ChangeSet();
        doReconcileObjects(oro, changeSet);
        fireChanges(changeSet);
    }

    public void createGuideLine(GuideLineDirection direction, Length position) {
        ChangeSet changeSet = new ChangeSet();
        new GuideLine(IdGenerator.generateUniqueId("GuideLine"), null, direction, position, getPlan(), changeSet);
        fireChanges(changeSet);
    }

    public void deleteGuideLine(GuideLine guideLine) {
        removeObject(guideLine);
    }

    public void setGuideLinePosition(GuideLine guideLine, Length position) {
        guideLine.setPosition(position);
        fireObjectsChanged(Arrays.asList(guideLine));
    }

    public void setWallBevelTypeOfAnchorDock(Anchor anchorDock, WallBevelType wallBevel) {
        ChangeSet changeSet = new ChangeSet();
        doSetWallBevelTypeOfAnchorDock(anchorDock, wallBevel, changeSet);
        notifyChanges(changeSet);
    }

    public void doSetWallBevelTypeOfAnchorDock(Anchor anchorDock, WallBevelType wallBevel, ChangeSet changeSet) {
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(anchorDock, wallBevel, changeSet);
        for (Anchor dockedAnchor : anchorDock.getAllDockedAnchors()) {
            BaseAnchoredObject owner = dockedAnchor.getAnchorOwner();
            if (!(owner instanceof Wall)) {
                continue;
            }
            Wall wall = (Wall) owner;
            wall.healObject(Collections.singleton(Wall.HEAL_OUTLINE), changeSet);
        }
    }

    public SupportObject createNewSupportObject(SupportObjectDescriptor supportObjectDescriptor, Position2D pos) throws IOException {
        ChangeSet changeSet = new ChangeSet();
        SupportObject result = doCreateNewSupportObject(supportObjectDescriptor, pos, changeSet);
        notifyChanges(changeSet);
        return result;
    }

    public SupportObject doCreateNewSupportObject(SupportObjectDescriptor supportObjectDescriptor, Position2D pos, ChangeSet changeSet) throws IOException {
        Plan plan = getPlan();
        AssetLoader assetLoader = getAssetManager().buildAssetLoader();
        ThreeDObject obj = assetLoader.loadSupportObject3DObject(supportObjectDescriptor, Optional.empty(), false);
        if (obj == null) {
            throw new IOException("Error creating support object");
        }
        Set<String> meshIds = obj.getMeshViews().stream().map(mv -> mv.getId()).collect(Collectors.toSet());
        SupportObject result = SupportObject.create(supportObjectDescriptor.getName(), pos,
            new Dimensions2D(supportObjectDescriptor.getWidth(), supportObjectDescriptor.getDepth()),
            supportObjectDescriptor.getHeight(), 0, supportObjectDescriptor.getElevation(), meshIds, plan, changeSet);
        result.setSupportObjectDescriptorRef(supportObjectDescriptor.getSelfRef());
        return result;
    }

    public void notifyObjectsAdded(BaseObject... objects) {
        fireObjectsAdded(Arrays.asList(objects));
    }

    public void notifyObjectsRemoved(BaseObject... objects) {
        fireObjectsRemoved(Arrays.asList(objects));
    }

    public void notifyObjectsChanged(BaseObject... objects) {
        fireObjectsChanged(Arrays.asList(objects));
    }

    public void notifyObjectsAdded(Collection<BaseObject> objects) {
        fireObjectsAdded(objects);
    }

    public void notifyObjectsRemoved(Collection<BaseObject> objects) {
        fireObjectsRemoved(objects);
    }

    public void notifyObjectsChanged(Collection<BaseObject> objects) {
        fireObjectsChanged(objects);
    }

    public void notifyChanges(ChangeSet changeSet) {
        fireChanges(changeSet);
    }

    protected void fireChanges(ChangeSet changeSet) {
        fireObjectsAdded(changeSet.getAdditions());
        fireObjectsRemoved(changeSet.getRemovals());
        fireObjectsChanged(changeSet.getChanges());
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
