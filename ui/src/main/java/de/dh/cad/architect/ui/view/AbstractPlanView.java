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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.controller.ObjectsChangeHandler;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.IModelBasedObject;
import de.dh.cad.architect.ui.persistence.ViewState;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Shape;

public abstract class AbstractPlanView<TRepr extends IModelBasedObject, TAnc extends Node> extends BorderPane {
    protected final ObjectsChangeHandler OBJECTS_CHANGE_HANDLER = new ObjectsChangeHandler() {
        @Override
        public void objectsRemoved(Collection<BaseObject> removedObjects) {
            AbstractPlanView.this.handleObjectsRemoved(removedObjects);
        }

        @Override
        public void objectsChanged(Collection<BaseObject> changedObjects) {
            AbstractPlanView.this.handleObjectsChanged(changedObjects);
        }

        @Override
        public void objectsAdded(Collection<BaseObject> addedObjects) {
            AbstractPlanView.this.handleObjectsAdded(addedObjects);
        }
    };

    protected final ListChangeListener<String> SELECTED_OBJECTS_CHANGE_HANDLER = new ListChangeListener<>() {
        @Override
        public void onChanged(Change<? extends String> c) {
            while (c.next()) {
                List<String> removedSelectionIds = Collections.emptyList();
                List<String> addedSelectionIds = Collections.emptyList();
                if (c.wasRemoved()) {
                    removedSelectionIds = new ArrayList<>(c.getRemoved());
                }
                if (c.wasAdded()) {
                    // It's necessary to copy the added sub list to make the callee able to change the selected
                    // objects without interfering the argument
                    addedSelectionIds = new ArrayList<>(c.getAddedSubList());
                }
                AbstractPlanView.this.handleObjectsSelectionChanged(removedSelectionIds, addedSelectionIds);
            }
        }
    };

    protected final ChangeListener<String> FOCUSED_OBJECT_CHANGE_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            if (oldValue != null) {
                AbstractPlanView.this.handleObjectFocusChanged(oldValue, false);
            }
            if (newValue != null) {
                AbstractPlanView.this.handleObjectFocusChanged(newValue, true);
            }
        }
    };

    protected final UiController mUiController;
    protected ObjectProperty<AbstractViewBehavior<TRepr, TAnc>> mBehaviorProperty = new SimpleObjectProperty<>();
    protected AssetLoader mAssetLoader = null;

    protected final Map<String, TRepr> mRepresentationsById = new HashMap<>();
    protected final Map<String, TAnc> mAncillaryObjectsById = new HashMap<>();

    public AbstractPlanView(UiController uiController) {
        mUiController = uiController;
    }

    public UiController getUiController() {
        return mUiController;
    }

    public Plan getPlan() {
        return mUiController.getPlan();
    }

    public abstract boolean canClose();

    public abstract boolean isAlive();

    public void revive() {
        if (isAlive()) {
            return;
        }
        initialize();
        handleObjectsSelectionChanged(Collections.emptyList(), mUiController.selectedObjectIds());
    }

    public void dispose() {
        if (!isAlive()) {
            return;
        }
        uninitialize();
    }

    protected void initialize() {
        mAssetLoader = mUiController.getAssetManager().buildAssetLoader();

        mUiController.selectedObjectIds().addListener(SELECTED_OBJECTS_CHANGE_HANDLER);
        mUiController.focusedObjectId().addListener(FOCUSED_OBJECT_CHANGE_LISTENER);
        mUiController.addChangeHandler(OBJECTS_CHANGE_HANDLER);

        initializeFromPlan();
    }

    protected void uninitialize() {
        mUiController.removeChangeHandler(OBJECTS_CHANGE_HANDLER);
        mUiController.focusedObjectId().removeListener(FOCUSED_OBJECT_CHANGE_LISTENER);
        mUiController.selectedObjectIds().removeListener(SELECTED_OBJECTS_CHANGE_HANDLER);
        mAssetLoader = null;

        mRepresentationsById.clear();
        mAncillaryObjectsById.clear();
    }

    protected abstract void initializeFromPlan();

    public AssetLoader getAssetLoader() {
        return mAssetLoader;
    }

    public abstract String getTitle();

    public ObjectProperty<AbstractViewBehavior<TRepr, TAnc>> behaviorProperty() {
        return mBehaviorProperty;
    }

    public AbstractViewBehavior<TRepr, TAnc> getBehavior() {
        return mBehaviorProperty.get();
    }

    public void setBehavior(AbstractViewBehavior<TRepr, TAnc> value) {
        AbstractViewBehavior<TRepr, TAnc> currentBehavior = getBehavior();
        if (currentBehavior != null) {
            currentBehavior.uninstall(); // Will also discard any change handler on the context actions list
        }
        mBehaviorProperty.set(value);
        if (value != null) {
            value.install(this);
        }
    }

    public Map<String, TRepr> getRepresentationsById() {
        return mRepresentationsById;
    }

    public Map<String, TAnc> getAncillaryObjectsById() {
        return mAncillaryObjectsById;
    }

    public TRepr getRepresentationByModelId(String id) {
        return mRepresentationsById.get(id);
    }

    @SuppressWarnings("unchecked")
    public TRepr getRepresentation(Shape shape) {
        return (TRepr) shape.getUserData();
    }

    public Collection<TRepr> getAllRepresentations() {
        return mRepresentationsById.values();
    }

    public Collection<TRepr> getRepresentationsByIds(Collection<String> objIds) {
        Collection<TRepr> result = new ArrayList<>(objIds.size());
        for (String id : objIds) {
            TRepr repr = mRepresentationsById.get(id);
            if (repr != null) {
                result.add(repr);
            }
        }
        return result;
    }

    public abstract Collection<Node> getToolBarContributionItems();
    public abstract Class<? extends ViewState> getViewStateClass();
    public abstract Optional<? extends ViewState> getViewState();
    public abstract void setViewState(ViewState viewState);
    protected abstract Collection<TRepr> doAddUIRepresentations(Collection<? extends BaseObject> addedObjects);
    protected abstract Collection<TRepr> doRemoveUIRepresentations(Collection<? extends BaseObject> removedObjects);

    /**
     * Adds an ancillary, temporary object to the view.
     */
    public abstract void addAncillaryObject(TAnc aao);

    /**
     * Removes an ancillary object from the view.
     */
    public abstract void removeAncillaryObject(String id);

    // To be overridden
    protected void uiRepresentationsChanged(Collection<TRepr> changedReprs) {
        // Nothing to do here
    }

    // To be overridden
    protected void uiRepresentationsSelectionChanged(Collection<TRepr> removedSelectionReprs, Collection<TRepr> addedSelectionReprs) {
        // Nothing to do here
    }

    // To be overridden
    protected void uiRepresentationFocusChanged(TRepr repr, boolean focused) {
        // Nothing to do here
    }

    protected void onModelObjectsAdded(Collection<BaseObject> addedObjects) {
        addUIRepresentations(addedObjects);
    }

    protected void onModelObjectsRemoved(Collection<BaseObject> removedObjects) {
        removeUIRepresentations(removedObjects);
    }

    protected void onModelObjectsUpdated(Collection<BaseObject> changedObjects) {
        Collection<TRepr> changedReprs = new ArrayList<>();
        for (BaseObject baseObject : changedObjects) {
            TRepr objRepr = getRepresentationByModelId(baseObject.getId());
            if (objRepr == null) {
                continue;
            }
            objRepr.updateToModel();
            changedReprs.add(objRepr);
        }
        uiRepresentationsChanged(changedReprs);
        getBehavior().onObjectsChanged(changedReprs);
    }

    protected void handleObjectsSelectionChanged(Collection<String> removedSelectionIds, Collection<String> addedSelectionIds) {
        Collection<TRepr> removedSelectionReprs = getRepresentationsByIds(removedSelectionIds);
        Collection<TRepr> addedSelectionReprs = getRepresentationsByIds(addedSelectionIds);

        for (TRepr repr : removedSelectionReprs) {
            repr.setSelected(false);
        }
        for (TRepr repr : addedSelectionReprs) {
            repr.setSelected(true);
        }
        uiRepresentationsSelectionChanged(removedSelectionReprs, addedSelectionReprs);
        getBehavior().onObjectsSelectionChanged(removedSelectionReprs, addedSelectionReprs);
    }

    protected final void handleObjectFocusChanged(String objId, boolean focused) {
        TRepr repr = mRepresentationsById.get(objId);
        if (repr == null) {
            return;
        }
        repr.setObjectFocused(focused);
        uiRepresentationFocusChanged(repr, focused);
        getBehavior().onObjectFocusChanged(repr, focused);
    }

    public final Collection<TRepr> addUIRepresentations(Collection<? extends BaseObject> addedObjects) {
        Collection<TRepr> result = doAddUIRepresentations(addedObjects);
        getBehavior().onObjectsAdded(result);
        return result;
    }

    public final Collection<TRepr> removeUIRepresentations(Collection<? extends BaseObject> removedObjects) {
        Collection<TRepr> result = doRemoveUIRepresentations(removedObjects);
        getBehavior().onObjectsRemoved(result);
        return result;
    }

    protected void registerRepresentation(String modelId, TRepr uiRepr) {
        mRepresentationsById.put(modelId, uiRepr);
    }

    protected void unregisterRepresentation(String modelId) {
        mRepresentationsById.remove(modelId);
    }

    // Local handler implementations
    protected final void handleObjectsAdded(Collection<BaseObject> addedObjects) {
        onModelObjectsAdded(addedObjects);
    }

    protected final void handleObjectsRemoved(Collection<BaseObject> removedObjects) {
        onModelObjectsRemoved(removedObjects);
    }

    protected final void handleObjectsChanged(Collection<BaseObject> changedObjects) {
        changedObjects = UiController.unwrapGroups(changedObjects);
        onModelObjectsUpdated(changedObjects);
    }
}
