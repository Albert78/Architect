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

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.DragControl;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.utils.Vector2D;
import de.dh.utils.fx.MouseHandlerContext;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

public class SupportObjectConstructionRepresentation extends AbstractAnchoredObjectConstructionRepresentation {
    public interface IMoveHandler {
        void move(de.dh.cad.architect.model.coords.Vector2D delta, boolean firstMoveEvent);
    }

    protected final ImageView mImage;
    protected final Rectangle mSpotRectangle;

    protected MouseHandlerContext mCollectiveMoveHandler = null;

    public SupportObjectConstructionRepresentation(SupportObject supportObject, Abstract2DView parentView) {
        super(supportObject, parentView);

        AssetLoader assetLoader = parentView.getAssetLoader();
        Image image = assetLoader.loadSupportObjectPlanViewImage(supportObject.getSupportObjectDescriptorRef(), true);
        //setViewOrder(Constants.VIEW_ORDER_SUPPORT_OBJECT); -- set in updateProperties()
        mSpotRectangle = new Rectangle();
        mSpotRectangle.getStrokeDashArray().setAll(3d, 10d);
        addScaled(mSpotRectangle);
        mImage = new ImageView(image);
        addScaled(mImage);

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
    }

    public SupportObject getSupportObject() {
        return (SupportObject) mModelObject;
    }

    @Override
    public ConstructionView getParentView() {
        return (ConstructionView) mParentView;
    }

    protected void updateProperties() {
        // Transparent fill of the spot rectangle is needed to make it react to the intersection check even if it is only intersected in the middle part
        mSpotRectangle.setFill(Color.BLACK.deriveColor(0, 0, 0, 0));
        mSpotRectangle.setStrokeWidth(2);
        mSpotRectangle.setStrokeType(StrokeType.OUTSIDE);
        boolean selected = isSelected();
        if (selected) {
            mSpotRectangle.setStroke(SELECTED_OBJECTS_COLOR);
        } else {
            mSpotRectangle.setStroke(Color.BLACK);
        }
        mSpotRectangle.setVisible(isObjectSpotted() || isObjectEmphasized() || selected);
        if (isObjectEmphasized()) {
            mSpotRectangle.getStrokeDashArray().setAll(6d, 8d);
        } else {
            mSpotRectangle.getStrokeDashArray().clear();
        }

        SupportObject supportObject = getSupportObject();
        // TODO: What can we do to make chairs appear below the table?
        if (isObjectFocused()) {
            setViewOrder(Constants.VIEW_ORDER_SUPPORT_OBJECTS + Constants.VIEW_ORDER_OFFSET_FOCUSED);
        } else {
            double viewOrderModifier = (supportObject.getHeight().plus(supportObject.getElevation())).divideBy(Length.ofCM(250));
            setViewOrder(Constants.VIEW_ORDER_SUPPORT_OBJECTS - viewOrderModifier + Constants.VIEW_ORDER_OFFSET_NORMAL);
        }
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        updateShape();
    }

    protected void updateShape() {
        SupportObject supportObject = getSupportObject();
        Point2D center = CoordinateUtils.positionToPoint2D(supportObject.getHandleAnchor().getPosition().projectionXY());
        Vector2D size = CoordinateUtils.dimensions2DToUiVector2D(supportObject.getSize());
        Point2D tl = new Point2D(center.getX() - size.getX() / 2, center.getY() - size.getY() / 2);
        Point3D rotationAxis = new Point3D(0, 0, 1);
        // Rotation direction is negated against global coordinate system
        float rotationDeg = -supportObject.getRotationDeg();

        mSpotRectangle.setX(tl.getX());
        mSpotRectangle.setY(tl.getY());
        mSpotRectangle.setWidth(size.getX());
        mSpotRectangle.setHeight(size.getY());
        mSpotRectangle.setRotationAxis(rotationAxis);
        mSpotRectangle.setRotate(rotationDeg);
        mImage.setX(tl.getX());
        mImage.setY(tl.getY());
        mImage.setFitWidth(size.getX());
        mImage.setFitHeight(size.getY());
        mImage.setRotationAxis(rotationAxis);
        mImage.setRotate(rotationDeg);
    }

    public void enableCollectiveMove(IMoveHandler moveHandler) {
        disableCollectiveMove();
        final var dragControl = new DragControl() {
            boolean FirstMoveEvent = true;
        };

        // Drag handler is installed when modification features are turned on
        mCollectiveMoveHandler = createDragHandler(
            op -> { // Start drag
                dragControl.setPoint(op);
                dragControl.FirstMoveEvent = true;
            },
            (op, dp, sp) -> { // Drag
                Point2D last = dragControl.getPoint();
                de.dh.cad.architect.model.coords.Vector2D delta = CoordinateUtils.point2DToVector2D(dp.subtract(last));
                moveHandler.move(delta, dragControl.FirstMoveEvent);
                dragControl.setPoint(dp);
                dragControl.FirstMoveEvent = false;
            },
            (op, dp, sp) -> { // End drag
                // Nothing to do
            }, Cursor.MOVE, Cursor.CLOSED_HAND).install(this);
    }

    public void disableCollectiveMove() {
        if (mCollectiveMoveHandler != null) {
            mCollectiveMoveHandler.uninstall(this);
        }
        mCollectiveMoveHandler = null;
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateShape();
        updateProperties();
    }

    @Override
    protected Optional<Shape> getShapeForIntersectionCheck() {
        return Optional.of(mSpotRectangle);
    }
}
