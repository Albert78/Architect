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
package de.dh.cad.architect.ui.objects;

import java.util.Objects;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * UI representation of a model object.
 */
// Will be subclassed for each model class (Wall, Floor, ...) visible in 3D view
public abstract class Abstract3DRepresentation extends Group implements IModelBasedObject {
    public static abstract class ObjectSurface {
        protected final Abstract3DRepresentation mOwnerRepr;
        protected final String mSurfaceTypeId;

        public ObjectSurface(Abstract3DRepresentation ownerRepr, String surfaceTypeId) {
            mOwnerRepr = ownerRepr;
            mSurfaceTypeId = surfaceTypeId;
        }

        public Abstract3DRepresentation getOwnerRepr() {
            return mOwnerRepr;
        }

        public String getSurfaceTypeId() {
            return mSurfaceTypeId;
        }

        public abstract AssetRefPath getMaterialRef();
        public abstract boolean assignMaterial(AssetRefPath materialRef);
    }

    public static final Color SELECTED_OBJECTS_COLOR = Color.LIGHTBLUE;

    protected final BooleanProperty mSelectedProperty = new SimpleBooleanProperty(this, "isSelected", false);
    protected final BooleanProperty mObjectSpottedProperty = new SimpleBooleanProperty(this, "isSpotted", false);
    protected final BooleanProperty mObjectFocusedProperty = new SimpleBooleanProperty(this, "isFocused", false);
    protected final BooleanProperty mObjectEmphasizedProperty = new SimpleBooleanProperty(this, "isFocused", false);

    protected final SimpleObjectProperty<ObjectSurface> mMouseOverSurfaceProperty = new SimpleObjectProperty<>(null);

    protected final BooleanProperty mMouseOverProperty = new SimpleBooleanProperty(this, "isMouseOver", false);

    protected final EventHandler<MouseEvent> MOUSE_OVER_MOUSE_ENTERED_LISTENER = mouseEvent -> {
        mMouseOverProperty.set(true);
    };

    protected final EventHandler<MouseEvent> MOUSE_OVER_MOUSE_EXITED_LISTENER = mouseEvent -> {
        mMouseOverProperty.set(false);
    };

    protected ChangeListener<Boolean> MOUSE_OVER_SPOT_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            mObjectSpottedProperty.set(newValue);
        }
    };

    protected final Abstract3DView mParentView;
    protected final BaseObject mModelObject;

    protected Abstract3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        mParentView = parentView;
        mModelObject = modelObject;
        mParentView.registerRepresentation(mModelObject.getId(), this);

        addEventHandler(MouseEvent.MOUSE_ENTERED, MOUSE_OVER_MOUSE_ENTERED_LISTENER);
        addEventHandler(MouseEvent.MOUSE_EXITED, MOUSE_OVER_MOUSE_EXITED_LISTENER);
    }

    public Property<Boolean> selectedProperty() {
        return mSelectedProperty;
    }

    @Override
    public boolean isSelected() {
        return mSelectedProperty.get();
    }

    @Override
    public void setSelected(boolean value) {
        mSelectedProperty.set(value);
    }

    public BooleanProperty objectSpottedProperty() {
        return mObjectSpottedProperty;
    }

    @Override
    public boolean isObjectSpotted() {
        return mObjectSpottedProperty.get();
    }

    @Override
    public void setObjectSpotted(boolean value) {
        mObjectSpottedProperty.set(value);
    }

    public BooleanProperty objectFocusedProperty() {
        return mObjectFocusedProperty;
    }

    @Override
    public boolean isObjectFocused() {
        return mObjectFocusedProperty.get();
    }

    @Override
    public void setObjectFocused(boolean value) {
        mObjectFocusedProperty.set(value);
    }

    public BooleanProperty objectEmphasizedProperty() {
        return mObjectEmphasizedProperty;
    }

    @Override
    public boolean isObjectEmphasized() {
        return mObjectEmphasizedProperty.get();
    }

    @Override
    public void setObjectEmphasized(boolean value) {
        mObjectEmphasizedProperty.set(value);
    }

    public BooleanProperty mouseOverProperty() {
        return mMouseOverProperty;
    }

    public boolean isMouseOver() {
        return mMouseOverProperty.get();
    }

    public ObjectProperty<ObjectSurface> mouseOverSurfaceProperty() {
        return mMouseOverSurfaceProperty;
    }

    public ObjectSurface getMouseOverSurface() {
        return mMouseOverSurfaceProperty.get();
    }

    /**
     * Called after this view was removed from the plan.
     * Can remove event handlers etc.
     */
    public void dispose() {
        mParentView.unregisterRepresentation(mModelObject.getId());
    }

    @Override
    public void updateToModel() {
        setVisible(!mModelObject.isHidden());
    }

    public void enableMouseOverSpot() {
        BooleanProperty property = mouseOverProperty();
        property.removeListener(MOUSE_OVER_SPOT_LISTENER); // To be sure we don't add it twice
        property.addListener(MOUSE_OVER_SPOT_LISTENER);
    }

    public void disableMouseOverSpot() {
        mouseOverProperty().removeListener(MOUSE_OVER_SPOT_LISTENER);
    }

    protected void markSurfaceNode(Node node, ObjectSurface surface) {
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            mMouseOverSurfaceProperty.set(surface);
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            mMouseOverSurfaceProperty.set(null);
        });
    }

    /**
     * Adds the given 3D node as a part of this model object representation.
     * @param node Node which is part of this model object representation. The node will be added to displayed objects,
     * all gestures like selection, dragging etc. will be available for the given node.
     */
    protected void add(Node node) {
        node.setUserData(this);
        getChildren().add(node);
    }

    protected void remove(Node node) {
        node.setUserData(null);
        getChildren().remove(node);
    }

    @Override
    public BaseObject getModelObject() {
        return mModelObject;
    }

    @Override
    public String getModelId() {
        return mModelObject.getId();
    }

    public AssetLoader getAssetLoader() {
        return mParentView.getAssetLoader();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mModelObject);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Abstract3DRepresentation other = (Abstract3DRepresentation) obj;
        return Objects.equals(mModelObject, other.mModelObject);
    }

    @Override
    public String toString() {
        return mModelObject.toString();
    }
}
