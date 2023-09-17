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
package de.dh.cad.architect.ui.view.construction.feedback.supportobjects;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.dh.cad.architect.model.coords.Bounds2D;
import de.dh.cad.architect.model.coords.Box2D;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.BaseImageAncillary;
import de.dh.cad.architect.ui.objects.BasePolylineShapeAncillary;
import de.dh.cad.architect.ui.utils.Axis;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.utils.Cursors;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.behaviors.AbstractConstructionBehavior;
import de.dh.utils.fx.ImageUtils;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.StrokeType;
import javafx.stage.Window;

public class EditSupportObjectsVisualFeedbackManager {
    public static class SupportObjectLocationData {
        protected final String mModelId;
        protected final Position2D mCenterPoint;
        protected final Dimensions2D mSize;
        protected final float mRotationDeg;
        protected final Length mHeight;
        protected final Length mElevation;

        public SupportObjectLocationData(String modelId,
            Position2D centerPoint,
            Dimensions2D size,
            float rotationDeg,
            Length height,
            Length elevation) {
            mModelId = modelId;
            mCenterPoint = centerPoint;
            mSize = size;
            mRotationDeg = rotationDeg % 360;
            mHeight = height;
            mElevation = elevation;
        }

        public String getModelId() {
            return mModelId;
        }

        public Position2D getCenterPoint() {
            return mCenterPoint;
        }

        public Dimensions2D getSize() {
            return mSize;
        }

        /**
         * Gets the rotation of the underlaying support object in degrees around the center point in clockwise direction.
         */
        public float getRotationDeg() {
            return mRotationDeg;
        }

        public Length getHeight() {
            return mHeight;
        }

        public Length getElevation() {
            return mElevation;
        }

        public SupportObjectLocationData withSize(Dimensions2D size) {
            return new SupportObjectLocationData(mModelId, mCenterPoint, size, mRotationDeg, mHeight, mElevation);
        }

        public SupportObjectLocationData withCenterPoint(Position2D centerPoint) {
            return new SupportObjectLocationData(mModelId, centerPoint, mSize, mRotationDeg, mHeight, mElevation);
        }

        public SupportObjectLocationData withRotationDeg(float rotationDeg) {
            return new SupportObjectLocationData(mModelId, mCenterPoint, mSize, rotationDeg, mHeight, mElevation);
        }

        public SupportObjectLocationData withHeight(Length height) {
            return new SupportObjectLocationData(mModelId, mCenterPoint, mSize, mRotationDeg, height, mElevation);
        }

        public SupportObjectLocationData withElevation(Length elevation) {
            return new SupportObjectLocationData(mModelId, mCenterPoint, mSize, mRotationDeg, mHeight, elevation);
        }
    }

    public interface SupportObjectsUpdateHandler {
        void updateSupportObjects(List<SupportObjectLocationData> objectsData);
    }

    protected static final Length MINIMUM_OBJECT_SIZE = Length.ofCM(10);

    protected static final String ROTATE_IMAGE_RESOURCE = "Rotate.png";
    protected static final String SIZE_IMAGE_RESOURCE = "Resize.png";
    protected static final String CHANGE_HEIGHT_IMAGE_RESOURCE = "ChangeHeight.png";
    protected static final String CHANGE_ELEVATION_IMAGE_RESOURCE = "ChangeElevation.png";
    protected static final int MODIFICATION_SYMBOLS_SIZE = Constants.TWO_D_INFO_SYMBOLS_SIZE;

    protected final ConstructionView mView;
    protected final AbstractConstructionBehavior mBehavior;
    protected final SupportObjectsUpdateHandler mObjectsUpdateHandler;

    protected BaseImageAncillary mRotateFeature = null;
    protected BaseImageAncillary mResizeFeature = null;
    protected BaseImageAncillary mChangeHeightFeature = null;
    protected BaseImageAncillary mChangeElevationFeature = null;

    protected BasePolylineShapeAncillary mBoundingBoxShape = null;
    protected Vector2D mBoundingBoxResizeOpposite = null; // Top-left point in unrotated state
    protected Vector2D mObjectsCenter = null; // Center position of the bounding box / of all selected objects

    // Order matters: We derive the rotation of the visual bounding box from the rotation of the first object
    protected List<SupportObjectLocationData> mSupportObjects = new ArrayList<>();

    public EditSupportObjectsVisualFeedbackManager(AbstractConstructionBehavior behavior, ConstructionView view, SupportObjectsUpdateHandler objectsUpdateHandler) {
        mBehavior = behavior;
        mView = view;
        mObjectsUpdateHandler = objectsUpdateHandler;
    }

    protected double getBoundingBoxRotationDeg() {
        return mSupportObjects.isEmpty() ? 0 : mSupportObjects.get(0).getRotationDeg();
    }

    protected void rotateSupportObjects(List<SupportObjectLocationData> origObjectsData, float angleDeg, Position2D pivotPosition) {
        List<SupportObjectLocationData> changedObjects = new ArrayList<>();
        for (SupportObjectLocationData sold : origObjectsData) {
            float origRotationDeg = sold.getRotationDeg();
            Position2D origCenterPoint = sold.getCenterPoint();

            Position2D newCenterPoint = origCenterPoint.rotateAround(angleDeg, pivotPosition);

            SupportObjectLocationData newSOLD = sold
                            .withCenterPoint(newCenterPoint)
                            .withRotationDeg(origRotationDeg + angleDeg);
            changedObjects.add(newSOLD);
        }
        mObjectsUpdateHandler.updateSupportObjects(changedObjects);
    }

    protected Position2D scaleInRotatedCoordinateSystem(Position2D point, double rotation, double scale, Position2D pivotPosition) {
        // Simplified form of:
        // point.rotateAround(-rotation, pivotPosition).scale(scale, scale, pivotPosition).rotateAround(rotation, pivotPosition);
        return pivotPosition.plus(
            point
                .minus(pivotPosition)
                .rotate(-rotation)
                .scale(scale, scale)
                .rotate(rotation));
    }

    /**
     * Scales the given objects by a factor in a rotated coordinate system according to a pivot position.
     * The scale operation will maintain the relative position of all objects among each other.
     * It is necessary to make it so difficult because when the bounding box is rotated, the scale operation must
     * be applied in direction of the rotated bounding box which corresponds to a rotated coordinate system.
     * The scale operation itself has a pivot position which corresponds to the bounding box corner
     * in the opposite of the scale image.
     * @param origObjectsData Contains original center position and size of the objects to be resized.
     * @param pivot Center position of the rotate and scale operations.
     * @param scale Scale factor.
     */
    protected void scaleSupportObjects(List<SupportObjectLocationData> origObjectsData, Position2D pivotPosition, double scaleCoordinateSystemRotationDeg, double scale) {
        List<SupportObjectLocationData> changedObjects = new ArrayList<>();
        for (SupportObjectLocationData sold : origObjectsData) {
            Position2D origCenterPoint = sold.getCenterPoint();
            Dimensions2D origSize = sold.getSize();

            Position2D newCenterPoint = scaleInRotatedCoordinateSystem(origCenterPoint, scaleCoordinateSystemRotationDeg, scale, pivotPosition);
            Vector2D newSize = origSize.toVector().times(scale);
            newSize = newSize.enlarge(Length.ofCM(5), Length.ofCM(5));

            SupportObjectLocationData newSOLD = sold
                            .withSize(new Dimensions2D(newSize.getX(), newSize.getY()))
                            .withCenterPoint(newCenterPoint);
            changedObjects.add(newSOLD);
        }
        mObjectsUpdateHandler.updateSupportObjects(changedObjects);
    }

    protected void changeSupportObjectsHeight(List<SupportObjectLocationData> origObjectsData, Length deltaHeight) {
        List<SupportObjectLocationData> changedObjects = new ArrayList<>();
        for (SupportObjectLocationData sold : origObjectsData) {
            Length origHeight = sold.getHeight();
            Length height = origHeight.plus(deltaHeight);

            SupportObjectLocationData newSOLD = sold
                            .withHeight(height);
            changedObjects.add(newSOLD);
        }
        mObjectsUpdateHandler.updateSupportObjects(changedObjects);
    }

    protected void changeSupportObjectsElevation(List<SupportObjectLocationData> origObjectsData, Length deltaElevation) {
        List<SupportObjectLocationData> changedObjects = new ArrayList<>();
        for (SupportObjectLocationData sold : origObjectsData) {
            Length origElevation = sold.getElevation();
            Length elevation = origElevation.plus(deltaElevation);

            SupportObjectLocationData newSOLD = sold
                            .withElevation(elevation);
            changedObjects.add(newSOLD);
        }
        mObjectsUpdateHandler.updateSupportObjects(changedObjects);
    }

    /**
     * Returns the given length as string, inclusive {@code '+'} or {@code '-'} sign.
     */
    protected String deltaString(Length delta) {
        String sign = delta.lt(Length.ZERO) ? "" : "+"; // In negative case, Length.toString() already contains a "-"
        return sign + delta.toString();
    }

    /**
     * Installs the four ancillary modification feature image objects in the view.
     */
    protected void installModificationFeatures() {
        Window window = mView.getScene().getWindow();
        Tooltip tooltip = new Tooltip();
        tooltip.setHideOnEscape(true);
        tooltip.setAutoHide(true);

        mRotateFeature = new BaseImageAncillary(
            ImageUtils.loadSquareIcon(EditSupportObjectsVisualFeedbackManager.class, ROTATE_IMAGE_RESOURCE, MODIFICATION_SYMBOLS_SIZE),
            mView);
        mRotateFeature.setViewOrder(Constants.VIEW_ORDER_INTERACTION);
        mView.addAncillaryObject(mRotateFeature);
        var dragRotateContext = new Object() {
            Vector2D origObjectsCenter;
            Vector2D vOp;

            List<SupportObjectLocationData> origObjectsData;
        };
        mRotateFeature.installDragHandler(
            op -> {
                dragRotateContext.origObjectsCenter = mObjectsCenter;
                dragRotateContext.vOp = CoordinateUtils.coordsToVector2D(op.getX(), op.getY(), true).minus(mObjectsCenter);

                dragRotateContext.origObjectsData = new ArrayList<>(mSupportObjects);
            },
            (op, dp, sp) -> {
                Vector2D vDp = CoordinateUtils.coordsToVector2D(dp.getX(), dp.getY(), true).minus(dragRotateContext.origObjectsCenter);
                float angle = (float) Vector2D.angleBetween(dragRotateContext.vOp, vDp);
                rotateSupportObjects(dragRotateContext.origObjectsData, angle, dragRotateContext.origObjectsCenter.toPosition2D());
            }, null, Cursors.createCursorRotate(), Cursors.createCursorRotate());
        mRotateFeature.mouseOverProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mBehavior.setUserHint(Strings.SUPPORT_OBJECT_ROTATE_USER_HINT);
            } else {
                mBehavior.setDefaultUserHint();
            }
        });

        mResizeFeature = new BaseImageAncillary(
            ImageUtils.loadSquareIcon(EditSupportObjectsVisualFeedbackManager.class, SIZE_IMAGE_RESOURCE, MODIFICATION_SYMBOLS_SIZE),
            mView);
        mResizeFeature.setViewOrder(Constants.VIEW_ORDER_INTERACTION);
        mView.addAncillaryObject(mResizeFeature);
        var dragSizeContext = new Object() {
            // Start positions of the bounding rect; the rect moves during the operation so we correlate the drag positions to the starting rect
            Vector2D pivotPosition;
            Vector2D origV;
            double coordinateSystemRotationDeg;

            List<SupportObjectLocationData> origObjectsData;
        };
        mResizeFeature.installDragHandler(
            op -> {
                dragSizeContext.coordinateSystemRotationDeg = getBoundingBoxRotationDeg();
                dragSizeContext.pivotPosition = mBoundingBoxResizeOpposite; // Opposite edge of resize image
                Vector2D origV = CoordinateUtils.point2DToVector2D(op, true)
                        .minus(dragSizeContext.pivotPosition);
                dragSizeContext.origV = origV;
                dragSizeContext.origObjectsData = new ArrayList<>(mSupportObjects);
            },
            (op, dp, sp) -> {
                Vector2D origV = dragSizeContext.origV;
                Vector2D pivotPosition = dragSizeContext.pivotPosition;
                Vector2D newV = CoordinateUtils.point2DToVector2D(dp, true)
                        .minus(pivotPosition);

                List<SupportObjectLocationData> objectsData = dragSizeContext.origObjectsData;
                if (objectsData.size() == 1) {
                    // In case we only have one single object selected, we allow scaling in X and Y directions independently
                    // which will just change the width and height of the object.
                    SupportObjectLocationData sold = objectsData.get(0);
                    float rotationDeg = sold.getRotationDeg();
                    Vector2D newSize = newV.rotate(-dragSizeContext.coordinateSystemRotationDeg); // = Object size in object coordinate system
                    newSize = newSize.mirrorY().enlarge(Length.ofCM(5), Length.ofCM(5)); // Negated Y because the position of our scale symbol is smaller than the pivot position
                    Dimensions2D origSize = sold.getSize();
                    double scaleX = newSize.getX().divideBy(origSize.getX());
                    double scaleY = newSize.getY().divideBy(origSize.getY());
                    Position2D origCenterPoint = sold.getCenterPoint();
                    Position2D newCenterPoint = origCenterPoint
                                    .minus(pivotPosition)
                                    .rotate(-rotationDeg)
                                    .scale(scaleX, scaleY)
                                    .rotate(rotationDeg)
                                    .plus(pivotPosition);
                    SupportObjectLocationData soldNew = sold
                                    .withSize(Dimensions2D.ofAbs(newSize))
                                    .withCenterPoint(newCenterPoint);
                    mObjectsUpdateHandler.updateSupportObjects(Arrays.asList(soldNew));
                } else {
                    // In case we have multiple objects selected, we restrict the scale operation to the direction of
                    // origV which means a uniform scale operation in X and Y directions. This is necessary because in
                    // a compound of multiple objects, we can have different objects rotation angles and thus if we would allow
                    // to scale in X and Y directions independently, we would need to shere objects with other rotation directions.
                    // So the simple solution is just to prevent other scale operations than just a uniform scale in X and Y direction.
                    Vector2D origVU = origV.toUnitVector(LengthUnit.CM);
                    Length origLength = origV.getLength();
                    Length newLength_origDirection = Length.ofCM(origVU.dotProduct(newV, LengthUnit.CM)).enlarge(Length.ofCM(10));
                    double scale = newLength_origDirection.divideBy(origLength);

                    scaleSupportObjects(objectsData,
                        pivotPosition.toPosition2D(),
                        dragSizeContext.coordinateSystemRotationDeg,
                        scale);
                }
            },
            null, Cursor.SE_RESIZE, Cursor.SE_RESIZE);
        mResizeFeature.mouseOverProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mBehavior.setUserHint(Strings.SUPPORT_OBJECT_RESIZE_USER_HINT);
            } else {
                mBehavior.setDefaultUserHint();
            }
        });

        mChangeHeightFeature = new BaseImageAncillary(
            ImageUtils.loadSquareIcon(EditSupportObjectsVisualFeedbackManager.class, CHANGE_HEIGHT_IMAGE_RESOURCE, MODIFICATION_SYMBOLS_SIZE),
            mView);
        mChangeHeightFeature.setViewOrder(Constants.VIEW_ORDER_INTERACTION);
        mView.addAncillaryObject(mChangeHeightFeature);
        var changeHeightContext = new Object() {
            List<SupportObjectLocationData> origObjectsData;
        };
        mChangeHeightFeature.installDragHandler(
            op -> {
                changeHeightContext.origObjectsData = new ArrayList<>(mSupportObjects);
            },
            (op, dp, sp) -> {
                Length delta = CoordinateUtils.coordsToLength(dp.subtract(op).getY(), Axis.Y);

                tooltip.setText(MessageFormat.format(Strings.DRAG_HEIGHT, deltaString(delta)));
                tooltip.show(mBoundingBoxShape, window.getX() + sp.getX(), window.getY() + sp.getY());

                changeSupportObjectsHeight(changeHeightContext.origObjectsData, delta);
            },
            (op, dp, sp) -> {
                tooltip.hide();
            }, Cursor.V_RESIZE, Cursor.V_RESIZE);
        mChangeHeightFeature.mouseOverProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mBehavior.setUserHint(Strings.SUPPORT_OBJECT_CHANGE_HEIGHT_USER_HINT);
            } else {
                mBehavior.setDefaultUserHint();
            }
        });

        mChangeElevationFeature = new BaseImageAncillary(
            ImageUtils.loadSquareIcon(EditSupportObjectsVisualFeedbackManager.class, CHANGE_ELEVATION_IMAGE_RESOURCE, MODIFICATION_SYMBOLS_SIZE),
            mView);
        mChangeElevationFeature.setViewOrder(Constants.VIEW_ORDER_INTERACTION);
        mView.addAncillaryObject(mChangeElevationFeature);
        var changeElevationContext = new Object() {
            List<SupportObjectLocationData> origObjectsData;
        };
        mChangeElevationFeature.installDragHandler(
            op -> {
                changeElevationContext.origObjectsData = new ArrayList<>(mSupportObjects);
            },
            (op, dp, sp) -> {
                Length delta = CoordinateUtils.coordsToLength(dp.subtract(op).getY(), Axis.Y);

                tooltip.setText(MessageFormat.format(Strings.DRAG_ELEVATION, deltaString(delta)));
                tooltip.show(mBoundingBoxShape, window.getX() + sp.getX(), window.getY() + sp.getY());

                changeSupportObjectsElevation(changeElevationContext.origObjectsData, delta);
            },
            (op, dp, sp) -> {
                tooltip.hide();
            }, Cursor.V_RESIZE, Cursor.V_RESIZE);
        mChangeElevationFeature.mouseOverProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mBehavior.setUserHint(Strings.SUPPORT_OBJECT_CHANGE_ELEVATION_USER_HINT);
            } else {
                mBehavior.setDefaultUserHint();
            }
        });

        mBoundingBoxShape = new BasePolylineShapeAncillary(true, mView);
        mView.addAncillaryObject(mBoundingBoxShape);
    }

    /**
     * Removes the four ancillary modification feature images from the view.
     */
    protected void removeModificationFeatures() {
        mView.removeAncillaryObject(mRotateFeature.getAncillaryObjectId());
        mRotateFeature = null;
        mView.removeAncillaryObject(mResizeFeature.getAncillaryObjectId());
        mResizeFeature = null;
        mView.removeAncillaryObject(mChangeHeightFeature.getAncillaryObjectId());
        mChangeHeightFeature = null;
        mView.removeAncillaryObject(mChangeElevationFeature.getAncillaryObjectId());
        mChangeElevationFeature = null;
    }

    public void updateVisualObjects(List<SupportObjectLocationData> locationData) {
        mSupportObjects = locationData;

        // We think the bounding box in its own coordinate system, rotated by boxRotation around the center point of the first object.
        // This is necessary to be able to include all contained object corners with the box's X- and Y-coordinates.
        // Thus to calculate the box, we need to:
        // (1) Rotate each object bounds around the object's center with the object's rotation to put it in final, global coordinates
        // (2) Inverse rotate the object's bounds around the bounding box's rotation center with the bounding box's rotation
        //     to put the object's bounds in the bounding box's coordinate system
        // (3) Union those transformed object's bounds coordinates with the bounding box
        //
        // (4) At the end, we transform the final bounding box's coordinates back to global coordinates to show the box in the UI,
        // this is done by rotating the (rotated) bounding box's coordinates by the bounding box's rotation around it's rotation center.

        // Data of the bounding box which contains all objects.
        // This box is rotated like the first selected object.
        float boxRotation = 0;
        Bounds2D boundingBoxRotated = null;
        Position2D boundingBoxRotationCenter = null;

        for (SupportObjectLocationData sold : locationData) {
            // Data of the current object
            Position2D soCenterPoint = sold.getCenterPoint();
            Dimensions2D soSize = sold.getSize();
            Bounds2D soBox = Bounds2D.of(soCenterPoint.minus(soSize.toVector().times(0.5)), soSize);
            float soRotation = sold.getRotationDeg();

            if (boundingBoxRotated == null) {
                // First object, take rotation from that object.
                // The box is initialized with the bounds of the object and will grow with each object we loop through.
                boxRotation = soRotation;
                boundingBoxRotated = soBox;
                boundingBoxRotationCenter = soCenterPoint;
            } else {
                Box2D soBoxFinal = soBox.rotateAround(soRotation, soCenterPoint); // Step (1)
                Box2D soBoxFinalInBBCoordinates = soBoxFinal.rotateAround(-boxRotation, boundingBoxRotationCenter); // Step (2)
                boundingBoxRotated = boundingBoxRotated.union(soBoxFinalInBBCoordinates); // Step (3)
            }
        }
        if (boundingBoxRotated == null) {
            mBoundingBoxShape.removeFromView();
            mBoundingBoxShape = null;
            return;
        }

        Box2D finalBoundingBox = boundingBoxRotated.rotateAround(boxRotation, boundingBoxRotationCenter);
        // X1/Y2 ----- X2/Y2
        //   |           |
        //   |           |
        // X1/Y1 ----- X2/Y1
        Position2D x1y1 = finalBoundingBox.getX1Y1();
        Position2D x1y2 = finalBoundingBox.getX1Y2();
        Position2D x2y1 = finalBoundingBox.getX2Y1();
        Position2D x2y2 = finalBoundingBox.getX2Y2();
        Point2D fx_x1y1 = CoordinateUtils.positionToPoint2D(x1y1, true);
        Point2D fx_x1y2 = CoordinateUtils.positionToPoint2D(x1y2, true);
        Point2D fx_x2y1 = CoordinateUtils.positionToPoint2D(x2y1, true);
        Point2D fx_x2y2 = CoordinateUtils.positionToPoint2D(x2y2, true);
        List<Double> coords = Arrays.asList(
            fx_x1y1.getX(), fx_x1y1.getY(),
            fx_x2y1.getX(), fx_x2y1.getY(),
            fx_x2y2.getX(), fx_x2y2.getY(),
            fx_x1y2.getX(), fx_x1y2.getY());
        mBoundingBoxShape.updateCoords(coords, StrokeType.OUTSIDE);
        mObjectsCenter = new Vector2D(x1y1.getX().plus(x2y2.getX()).divideBy(2), x1y1.getY().plus(x2y2.getY()).divideBy(2));
        mBoundingBoxResizeOpposite = x1y2.toVector2D();

        mChangeHeightFeature.setPosition(x1y2);
        mRotateFeature.setPosition(x2y2);
        mChangeElevationFeature.setPosition(x1y1);
        mResizeFeature.setPosition(x2y1);
    }

    /**
     * Installs the ancillary modification images in the view.
     */
    public void install() {
        installModificationFeatures();
        // Visual objects are updated in separate calls to updateVisualObjects()
    }

    public void uninstall() {
        updateVisualObjects(Collections.emptyList());
        removeModificationFeatures();
    }
}
