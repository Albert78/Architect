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

import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.utils.Axis;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.DragControl2D;
import de.dh.cad.architect.ui.view.construction.UiPlanPosition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Scale;

public class AnchorConstructionRepresentation extends Abstract2DRepresentation {
    protected static final double CIRCLE_RADIUS = 6;

    protected final Circle mShape;
    protected final Scale mShapeScaleCorrection;

    protected ChangeListener<Number> OWNER_VIEW_ORDER_CHANGE_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            updateProperties();
        }
    };

    protected Abstract2DRepresentation mTrackedOwner = null;

    public AnchorConstructionRepresentation(Anchor anchor, Abstract2DView parentView) {
        super(anchor, parentView);
        mShape = new Circle(CIRCLE_RADIUS);
        setViewOrder(Constants.VIEW_ORDER_ANCHOR);
        mShapeScaleCorrection = addUnscaled(mShape);
        mShapeScaleCorrection.pivotXProperty().bind(mShape.centerXProperty());
        mShapeScaleCorrection.pivotYProperty().bind(mShape.centerYProperty());

        ChangeListener<Boolean> propertiesUpdaterListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                updateProperties();
            }
        };
        selectedProperty().addListener(propertiesUpdaterListener);
        objectSpottedProperty().addListener(propertiesUpdaterListener);
        objectFocusedProperty().addListener(propertiesUpdaterListener);
        objectEmphasizedProperty().addListener(propertiesUpdaterListener);

        // We always install the drag handlers; the actual drag operation is enabled using the isDragSupported() method
        installDragHandlers();
        tryAttachOwnerListeners();
    }

    @Override
    public void dispose() {
        super.dispose();
        detachOwnerListeners();
    }

    public Abstract2DRepresentation getAnchorOwnerRepresentation() {
        BaseAnchoredObject anchorOwner = getAnchor().getAnchorOwner();
        return anchorOwner == null ? null : mParentView.getRepresentationByModelId(anchorOwner.getId());
    }

    protected Abstract2DRepresentation getAnchorDockOwnerRepresentation() {
        BaseAnchoredObject rootAnchorOwner = getAnchor().getRootMasterOfAnchorDock().getAnchorOwner();
        return mParentView.getRepresentationByModelId(rootAnchorOwner.getId());
    }

    public Anchor getAnchor() {
        return (Anchor) mModelObject;
    }

    protected void updateProperties() {
        Anchor anchor = getAnchor();
        mShape.setStrokeType(StrokeType.OUTSIDE);
        tryAttachOwnerListeners();
        Abstract2DRepresentation anchorOwnerRepresentation = getAnchorOwnerRepresentation();
        final double ownerViewOrder = anchorOwnerRepresentation == null ? Constants.VIEW_ORDER_ANCHOR : anchorOwnerRepresentation.getViewOrder();
        double viewOrderOffset = Constants.VIEW_ORDER_OFFSET_NORMAL;
        Color strokeColor;
        if (anchor.getDockMaster().isPresent()) {
            strokeColor = Color.ORANGERED;
        } else {
            strokeColor = Color.GOLD;
        }
        if (isSelected()) {
            strokeColor = SELECTED_OBJECTS_COLOR;
            viewOrderOffset = Constants.VIEW_ORDER_OFFSET_FOCUSED;
        }
        mShape.setStroke(strokeColor);
        if (isObjectFocused()) {
            viewOrderOffset = Constants.VIEW_ORDER_OFFSET_FOCUSED;
        }
        if (isObjectSpotted()) {
            mShape.setStrokeWidth(4);
        } else {
            mShape.setStrokeWidth(3);
        }
        if (isObjectEmphasized()) {
            mShape.getStrokeDashArray().setAll(3d, 6d);
        } else {
            mShape.getStrokeDashArray().clear();
        }
        setViewOrder(ownerViewOrder - 1 + viewOrderOffset);
    }

    protected void updateCenter() {
        Position2D position = getAnchor().getPosition().projectionXY();
        mShape.setCenterX(CoordinateUtils.lengthToCoords(position.getX(), Axis.X));
        mShape.setCenterY(CoordinateUtils.lengthToCoords(position.getY(), Axis.Y));
    }

    /**
     * Tries to attach to our owner representation, if it is already present in plan.
     * This method is a hack to be able to find our owner, even if it's representation is registered after this
     * representation.
     */
    protected void tryAttachOwnerListeners() {
        if (mTrackedOwner != null) {
            return;
        }
        Abstract2DRepresentation anchorOwnerRepresentation = getAnchorOwnerRepresentation();
        if (anchorOwnerRepresentation != null) {
            mTrackedOwner = anchorOwnerRepresentation;
            mTrackedOwner.viewOrderProperty().addListener(OWNER_VIEW_ORDER_CHANGE_LISTENER);
        }
    }

    protected void detachOwnerListeners() {
        if (mTrackedOwner == null) {
            return;
        }
        mTrackedOwner.viewOrderProperty().removeListener(OWNER_VIEW_ORDER_CHANGE_LISTENER);
        mTrackedOwner = null;
    }

    // Could be moved to Abstract2DUiObject, if needed
    protected void installDragHandlers() {
        var dragControl = new DragControl2D() {
            boolean FirstMoveEvent = true;
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
            dragStart(pos.getModelPosition());
        });
        setOnMouseReleased(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }
            getScene().setCursor(Cursor.DEFAULT); // FIXME: We cannot know if we are still over the node or not
            dragEnd();
        });
        setOnMouseDragged(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                return;
            }
            if (!isDragSupported()) {
                return;
            }
            UiPlanPosition pos = mParentView.getPlanPositionFromScene(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            drag(dragControl.getPosition().getModelPosition(), pos.getModelPosition(), dragControl.FirstMoveEvent, mouseEvent.isShiftDown(), mouseEvent.isAltDown(), mouseEvent.isControlDown());
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

    // Could be moved ot Abstract2DUiObject, if needed
    protected void uninstallDragHandlers() {
        setOnMousePressed(null);
        setOnMouseReleased(null);
        setOnMouseDragged(null);
        setOnMouseEntered(null);
        setOnMouseExited(null);
    }

    protected boolean isDragSupported() {
        Abstract2DRepresentation anchorOwnerRepresentation = getAnchorDockOwnerRepresentation();
        return anchorOwnerRepresentation != null && anchorOwnerRepresentation.isAnchorDragSupported(getAnchor());
    }

    protected void dragStart(Position2D pos) {
        Abstract2DRepresentation repr = getAnchorDockOwnerRepresentation();
        if (repr != null) {
            repr.startAnchorDrag(getAnchor(), pos);
        }
    }

    protected void dragEnd() {
        Abstract2DRepresentation repr = getAnchorDockOwnerRepresentation();
        if (repr != null) {
            repr.endAnchorDrag(getAnchor());
        }
    }

    protected void drag(Position2D dragStartPos, Position2D currentPos, boolean firstMoveEvent, boolean shiftDown, boolean altDown, boolean controlDown) {
        Abstract2DRepresentation repr = getAnchorDockOwnerRepresentation();
        if (repr != null) {
            repr.dragAnchor(getAnchor(), dragStartPos, currentPos, firstMoveEvent, shiftDown, altDown, controlDown);
        }
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateCenter();
        updateProperties();
    }

    @Override
    protected Optional<Shape> getShapeForIntersectionCheck() {
        return Optional.of(mShape);
    }
}
