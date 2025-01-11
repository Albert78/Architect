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
package de.dh.cad.architect.ui.objects;

import java.util.Objects;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.DragControl2D;
import de.dh.cad.architect.ui.view.construction.UiPlanPosition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

/**
 * UI representation of a model object.
 */
// Will be subclassed for each model class (Wall, Floor, ...)
public abstract class Abstract2DRepresentation extends Abstract2DUiObject implements IModelBasedObject {
    public static final Color SELECTED_OBJECTS_COLOR = Color.BLUE;

    public static final double OPACITY_GRAYED_OUT = 0.3;
    public static final double OPACITY_DEFAULT = 1;

    protected final BooleanProperty mSelectedProperty = new SimpleBooleanProperty(this, "isSelected", false);
    protected final BooleanProperty mObjectSpottedProperty = new SimpleBooleanProperty(this, "isSpotted", false);
    protected final BooleanProperty mObjectFocusedProperty = new SimpleBooleanProperty(this, "isFocused", false);
    protected final BooleanProperty mObjectEmphasizedProperty = new SimpleBooleanProperty(this, "isFocused", false);

    protected final BaseObject mModelObject;

    // In the constructor, the sub class should create all graphical shapes for this representation
    // and bind to the selected property, updating their UI elements if necessary.
    // Each created shape must be added using the add(Shape, boolean) method and removed using the remove(Shape) method.
    protected Abstract2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        super(parentView);
        mModelObject = modelObject;
    }

    @Override
    public void updateToModel() {
        // To be overridden
    }

    protected void installDragHandlers() {
        var dragControl = new DragControl2D() {
            boolean FirstMoveEvent = true;
            Object context = null;
        };

        setOnMousePressed(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (!isDragSupported()) {
                return;
            }
            UiPlanPosition pos = mParentView.getPlanPositionFromScene(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            dragControl.setPosition(pos);
            dragControl.FirstMoveEvent = true;
            getScene().setCursor(Cursor.MOVE);
            dragControl.context = dragStart(pos.getModelPosition());
        });
        setOnMouseReleased(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }
            getScene().setCursor(Cursor.DEFAULT); // FIXME: We cannot know if we are still over the node or not
            dragEnd(dragControl.context);
        });
        setOnMouseDragged(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                return;
            }
            if (!isDragSupported()) {
                return;
            }
            UiPlanPosition pos = mParentView.getPlanPositionFromScene(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            drag(dragControl.getPosition().getModelPosition(), pos.getModelPosition(), dragControl.FirstMoveEvent,
                    mouseEvent.isShiftDown(), mouseEvent.isAltDown(), mouseEvent.isControlDown(), dragControl.context);
            dragControl.FirstMoveEvent = false;
        });
        setOnMouseEntered(mouseEvent -> {
            if (!isDragSupported()) {
                return;
            }
            if (!mouseEvent.isPrimaryButtonDown()) {
                getScene().setCursor(Cursor.HAND);
            }
        });
        setOnMouseExited(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
    }

    protected void uninstallDragHandlers() {
        setOnMousePressed(null);
        setOnMouseReleased(null);
        setOnMouseDragged(null);
        setOnMouseEntered(null);
        setOnMouseExited(null);
    }

    protected boolean isDragSupported() {
        return false;
    }

    protected Object dragStart(Position2D pos) {
        // To be overridden if isDragSupported()
        return null;
    }

    protected void drag(Position2D dragStartPos, Position2D currentPos, boolean firstMoveEvent, boolean shiftDown, boolean altDown, boolean controlDown, Object context) {
        // To be overridden if isDragSupported()
    }

    protected void dragEnd(Object context) {
        // To be overridden if isDragSupported()
    }

    public void enableMouseOverSpot() {
        mObjectSpottedProperty.bind(mouseOverProperty());
    }

    public void disableMouseOverSpot() {
        mObjectSpottedProperty.unbind();
        mObjectSpottedProperty.set(false);
    }

    @Override
    public BaseObject getModelObject() {
        return mModelObject;
    }

    @Override
    public String getModelId() {
        return mModelObject.getId();
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

    protected void configureMainBorderDefault(Shape shape) {
        if (isObjectEmphasized()) {
            shape.getStrokeDashArray().setAll(6d, 8d);
        } else {
            shape.getStrokeDashArray().clear();
        }
        if (isObjectSpotted()) {
            shape.setStrokeWidth(3);
        } else {
            shape.setStrokeWidth(1);
        }
    }

    public boolean isAnchorDragSupported(Anchor anchor) {
        return false;
    }

    public boolean isEditHandle(Anchor anchor) {
        return anchor.isHandle();
    }

    /**
     * Called when a drag operation starts on one of our anchors.
     * @param anchor Anchor to be dragged.
     * @param startDragPos Position where the drag operation started.
     * @return Context object which is passed back in {@link #dragAnchor(Anchor, Position2D, Position2D, boolean, boolean, boolean, boolean, Object)}
     * and {@link #endAnchorDrag(Anchor, Object)}.
     */
    public Object startAnchorDrag(Anchor anchor, Position2D startDragPos) {
        // To be overridden, if D&D is supported for anchors
        return null;
    }

    /**
     * Called if a drag operation ends on one of our anchors.
     * @param anchor Anchor being dragged.
     * @param context context object returned from {@link #startAnchorDrag(Anchor, Position2D)}.
     */
    public void endAnchorDrag(Anchor anchor, Object context) {
        // To be overridden, if D&D is supported for anchors
    }

    /**
     * Called if one of our anchors is dragged.
     * @param anchor Anchor being dragged.
     * @param startDragPos Position where the drag operation started.
     * @param currentDragPos Current position of the drag.
     * @param shiftDown True if the shift key is pressed.
     * @param altDown True if the alt key is pressed.
     * @param controlDown True if the control key is pressed.
     * @param context context object returned from {@link #startAnchorDrag(Anchor, Position2D)}.
     */
    public void dragAnchor(Anchor anchor,
        Position2D startDragPos, Position2D currentDragPos, boolean firstMoveEvent,
        boolean shiftDown, boolean altDown, boolean controlDown, Object context) {
        // Override if drag behavior should be different
        dragAnchorDock(anchor, currentDragPos, firstMoveEvent, shiftDown, altDown, controlDown, context);
    }

    public void dragAnchorDock(Anchor anchor, Position2D targetPosition, boolean firstMoveEvent,
        boolean shiftDown, boolean altDown, boolean controlDown, Object context) {
        Anchor masterOfDock = anchor.getRootMasterOfAnchorDock();
        if (!masterOfDock.isHandle()) {
            return;
        }
        UiController uiController = getUiController();
        uiController.setHandleAnchorPosition(masterOfDock, targetPosition, !firstMoveEvent);
    }

    protected Optional<Shape> getShapeForIntersectionCheck() {
        return Optional.empty();
    }

    public boolean intersects(Shape other) {
        return isVisible() && getShapeForIntersectionCheck().map(shape -> {
            Shape intersect = Shape.intersect(shape, other);
            return intersect.getBoundsInLocal().getWidth() != -1;
        }).orElseGet(() -> intersects(other.getBoundsInLocal()));
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
        Abstract2DRepresentation other = (Abstract2DRepresentation) obj;
        return Objects.equals(mModelObject, other.mModelObject);
    }

    @Override
    public String toString() {
        return mModelObject.toString();
    }
}
