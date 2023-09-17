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

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.utils.Axis;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.DragControl;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Scale;

/**
 * Point which is drawn between anchors, can be dragged by the user to be able to insert a new anchor point in between.
 * {@link IntermediatePoint} objects are autonomous objects whose only touch point to the object's representation instance
 * is that it's shape is added in the context of the surrounding instance of {@link Abstract2DRepresentation}.
 *
 * One or more instances of this class typically are held by a ground plan representation as long as it should be possible
 * to create new anchors between other anchors.
 * While the owner ground plan representation shows intermediate points, the anchors before and after it can be changed
 * by calling {@link #update(Anchor, Anchor)}, this will re-configure this instance, the intermediate point shape will move
 * between the two anchors.
 * When the user starts dragging an intermediate point, that event will immediately trigger the creation of a new anchor which
 * takes over the position of the former intermediate point. That event will detach the intermediate point from its original
 * owner ground plan representation, which is signalled by the callback function
 * {@link IntermediatePointCallback#detachIntermediatePoint(IntermediatePoint)}. When this function is called, the owner ground plan
 * representation should remove any references to the intermediate point and must not call any more methods on it; from that event on,
 * the intermediate point will manage its own lifetime. It will continue to exist as long as the user continues the dragging gesture.
 * After that gesture is finished, the intermediate point will automatically remove its shape, which will release any references to
 * this instance and thus makes this intermediate point ready for garbage collection.
 */
public class IntermediatePoint extends Abstract2DAncillaryObject {
    static class IntermediatePointDragOperation extends DragControl {
        private Anchor mCreatedAnchor = null;
        private Point2D mDragPointCenterOffset = null;

        public void reset() {
            mCreatedAnchor = null;
        }

        public void setCreatedAnchor(Anchor createdAnchor) {
            mCreatedAnchor = createdAnchor;
        }

        public Anchor getCreatedAnchor() {
            return mCreatedAnchor;
        }

        public Point2D getDragPointCenterOffset() {
            return mDragPointCenterOffset;
        }

        public void setDragPointCenterOffset(Point2D value) {
            mDragPointCenterOffset = value;
        }
    }

    public static abstract class IntermediatePointCallback {
        /**
         * The callee must give up responsibility for the passed {@link IntermediatePoint}. This means, the callee
         * must not change the state of the given point. This method is called before
         * {@link #createAnchor(IntermediatePoint, Anchor, Anchor, Position2D)} is called.
         */
        protected abstract void detachIntermediatePoint(IntermediatePoint source);

        protected abstract Anchor createHandleAnchor(IntermediatePoint source, Anchor anchorBefore, Anchor anchorAfter, Position2D bendPosition);
    }

    protected static final double INTERMEDIATE_POINT_CIRCLE_RADIUS = 5;

    protected final Abstract2DView mParentView;
    protected final IntermediatePointCallback mCallback;
    protected final Circle mShape;
    protected final Scale mScaleCompensation;
    protected Anchor mAnchorBefore = null;
    protected Anchor mAnchorAfter = null;

    public IntermediatePoint(ConstructionView parentView, IntermediatePointCallback callback) {
        super(parentView);
        mParentView = parentView;
        mCallback = callback;
        var dragOperation = new IntermediatePointDragOperation() {
            boolean FirstMoveEvent = true;
        };
        mShape = new Circle(INTERMEDIATE_POINT_CIRCLE_RADIUS);
        mScaleCompensation = addUnscaled(mShape);
        mParentView.addAncillaryObject(this);
        setViewOrder(Constants.VIEW_ORDER_INTERACTION);

        Pane root = mParentView.getTransformedRoot();

        mShape.setOnMouseEntered(mouseEvent -> {
            if (dragOperation.getCreatedAnchor() == null) {
                mShape.getScene().setCursor(Cursor.HAND);
            }
        });
        mShape.setOnMouseExited(mouseEvent -> {
            if (dragOperation.getCreatedAnchor() == null) {
                // No drag operation ongoing
                mShape.getScene().setCursor(Cursor.DEFAULT);
            }
        });
        mShape.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }
            dragOperation.reset();
            Point2D localPoint = root.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            // Start point of cursor
            dragOperation.setPoint(localPoint);
            dragOperation.FirstMoveEvent = true;
            // Delta distance start cursor point -> shape center
            dragOperation.setDragPointCenterOffset(new Point2D(mShape.getCenterX(), mShape.getCenterY()).subtract(localPoint));
            mShape.getScene().setCursor(Cursor.MOVE);
        });
        mShape.setOnMouseReleased(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }
            // This even is also fired if the mouse is not at the shape's position. This is necessary because we use this
            // even as finishing event for the drag operation.
            Anchor createdAnchor = dragOperation.getCreatedAnchor();
            if (createdAnchor == null) {
                // No drag operation yet
                mShape.getScene().setCursor(Cursor.DEFAULT); // We don't know if we are over the shape, so better play it safe and set default cursor
            } else {
                // Drag operation completed
                mShape.getScene().setCursor(Cursor.DEFAULT);
                // This IntermediatePoint is now consumed and cannot be reused. This is necessary because
                // IntermediatePoint must take control of its shape by its own as soon as we created a new anchor.
                dispose();
                dragEnd(createdAnchor);
            }
        });
        mShape.setOnMouseDragged(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                return;
            }

            Point2D localPoint = root.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            Point2D localPointWithOffset = localPoint.add(dragOperation.getDragPointCenterOffset());
            Position2D position = CoordinateUtils.point2DToPosition2D(localPointWithOffset);

            Anchor createdAnchor = dragOperation.getCreatedAnchor();
            if (createdAnchor == null) {
                createdAnchor = mCallback.createHandleAnchor(IntermediatePoint.this, mAnchorBefore, mAnchorAfter, position);
                if (createdAnchor == null) {
                    // Operation not possible - don't continue
                    return;
                }
                mCallback.detachIntermediatePoint(this);
                // Hopefully, the system created a UI representation for the new anchor...
                dragOperation.setCreatedAnchor(createdAnchor);
                mShape.setVisible(false);
                dragStart(createdAnchor, localPointWithOffset);
            } else {
                drag(createdAnchor, dragOperation.getPoint().add(dragOperation.getDragPointCenterOffset()), localPointWithOffset, position,
                    dragOperation.FirstMoveEvent, mouseEvent.isShiftDown(), mouseEvent.isAltDown(), mouseEvent.isControlDown());
                dragOperation.FirstMoveEvent = false;
            }
        });
    }

    protected Abstract2DRepresentation getAnchorOwnerRepresentation(Anchor anchor) {
        return mParentView.getRepresentationByModelId(anchor.getAnchorOwner().getId());
    }

    protected void dragStart(Anchor anchor, Point2D startPoint) {
        Abstract2DRepresentation repr = getAnchorOwnerRepresentation(anchor);
        if (repr != null) {
            repr.startAnchorDrag(anchor, startPoint, CoordinateUtils.point2DToPosition2D(startPoint));
        }
    }

    protected void dragEnd(Anchor anchor) {
        Abstract2DRepresentation repr = getAnchorOwnerRepresentation(anchor);
        if (repr != null) {
            repr.endAnchorDrag(anchor);
        }
    }

    protected void drag(Anchor anchor, Point2D dragStartPos, Point2D currentPos, Position2D currentPosition, boolean firstMoveEvent, boolean shiftDown, boolean altDown, boolean controlDown) {
        Abstract2DRepresentation repr = getAnchorOwnerRepresentation(anchor);
        if (repr != null) {
            repr.dragAnchor(anchor, dragStartPos, currentPos, currentPosition, firstMoveEvent, shiftDown, altDown, controlDown);
        }
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        updateShape();
    }

    public void updateShape() {
        if (mAnchorAfter == null || mAnchorBefore == null) {
            return;
        }
        Position2D positionAfter = mAnchorAfter.getPosition().projectionXY();
        Position2D positionBefore = mAnchorBefore.getPosition().projectionXY();
        double x = CoordinateUtils.lengthToCoords(positionAfter.getX().plus(positionBefore.getX()).times(0.5), Axis.X);
        double y = CoordinateUtils.lengthToCoords(positionAfter.getY().plus(positionBefore.getY()).times(0.5), Axis.Y);
        mShape.setCenterX(x);
        mShape.setCenterY(y);
        mScaleCompensation.setPivotX(x);
        mScaleCompensation.setPivotY(y);
    }

    public void update(Anchor anchorBefore, Anchor AnchorAfter) {
        mAnchorBefore = anchorBefore;
        mAnchorAfter = AnchorAfter;
        updateShape();
    }

    @Override
    public void dispose() {
        mParentView.removeAncillaryObject(getId());
    }
}