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
package de.dh.cad.architect.ui.view.construction.feedback.supportobjects;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.BaseImageAncillary;
import de.dh.cad.architect.ui.objects.BasePolylineShapeAncillary;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.utils.Cursors;
import de.dh.cad.architect.ui.utils.MathUtils;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.utils.fx.ImageUtils;
import de.dh.utils.fx.Vector2D;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
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
            mRotationDeg = rotationDeg;
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
    protected static final String SIZE_IMAGE_RESOURCE = "Size2.png";
    protected static final String CHANGE_HEIGHT_IMAGE_RESOURCE = "ChangeHeight.png";
    protected static final String CHANGE_ELEVATION_IMAGE_RESOURCE = "ChangeElevation.png";
    protected static final int MODIFICATION_SYMBOLS_SIZE = Constants.TWO_D_INFO_SYMBOLS_SIZE;

    protected final ConstructionView mView;
    protected final SupportObjectsUpdateHandler mObjectsUpdateHandler;

    protected BaseImageAncillary mRotateFeature = null;
    protected BaseImageAncillary mResizeFeature = null;
    protected BaseImageAncillary mChangeHeightFeature = null;
    protected BaseImageAncillary mChangeElevationFeature = null;

    protected BasePolylineShapeAncillary mBoundingBoxShape = null;
    protected Vector2D mBoundingBoxP1 = null; // Top-left point in unrotated state
    protected Vector2D mObjectsCenter = null; // Center position of the bounding box / of all selected objects

    // Order matters: We derive the rotation of the visual bounding box from the rotation of the first object
    protected List<SupportObjectLocationData> mSupportObjects = new ArrayList<>();

    public EditSupportObjectsVisualFeedbackManager(ConstructionView view, SupportObjectsUpdateHandler objectsUpdateHandler) {
        mView = view;
        mObjectsUpdateHandler = objectsUpdateHandler;
    }

    protected float getBoundingBoxRotationDeg() {
        return mSupportObjects.isEmpty() ? 0 : mSupportObjects.get(0).getRotationDeg();
    }

    protected void rotateSupportObjects(List<SupportObjectLocationData> origObjectsData, float angleDeg, Vector2D pivot) {
        Position2D pivotPosition = CoordinateUtils.coordsToPosition2D(pivot.getX(), pivot.getY());

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

    protected Position2D scaleInRotatedCoordinateSystem(Position2D point, double rotation, double scaleX, double scaleY, Position2D pivotPosition) {
        // Simplified form of:
        // point.rotateAround(-rotation, pivotPosition).scale(scaleX, scaleY, pivotPosition).rotateAround(rotation, pivotPosition);
        return pivotPosition.plus(
            point
                .minus(pivotPosition)
                .rotate(-rotation)
                .scale(scaleX, scaleY)
                .rotate(rotation));
    }

    /**
     * Scales the given objects in X and Y directions in a rotated coordinate system according to a pivot position.
     * It is necessary to make it so difficult because when the bounding box is rotated, the scale operation must
     * be applied in direction of the rotated bounding box which corresponds to a rotated coordinate system.
     * In that rotated system, the user can scale in X and Y directions.
     * The scale operation itself has a pivot operation which corresponds to the bounding box corner
     * in the opposite of the scale image.
     * @param origObjectsData Contains original center position and size of the objects to be resized.
     * @param pivot Center position of the rotate and scale operations.
     * @param scaleX Scale factor in X direction in the rotated coordinate system.
     * @param scaleY Scale factor in Y direction in the rotated coordinate system.
     */
    protected void scaleSupportObjects(List<SupportObjectLocationData> origObjectsData, Vector2D pivot, double scaleCoordinateSystemRotationDeg, double scaleX, double scaleY) {
        Position2D pivotPosition = CoordinateUtils.coordsToPosition2D(pivot.getX(), pivot.getY());

        List<SupportObjectLocationData> changedObjects = new ArrayList<>();
        for (SupportObjectLocationData sold : origObjectsData) {
            float rotationDeg = sold.getRotationDeg();
            Position2D origCenterPoint = sold.getCenterPoint();
            Dimensions2D origSize = sold.getSize();

            Position2D newCenterPoint = scaleInRotatedCoordinateSystem(origCenterPoint, scaleCoordinateSystemRotationDeg, scaleX, scaleY, pivotPosition);

            de.dh.cad.architect.model.coords.Vector2D sizeV = new de.dh.cad.architect.model.coords.Vector2D(origSize.getX(), origSize.getY()).rotate(rotationDeg);
            Position2D origBr = origCenterPoint.plus(sizeV.times(0.5));
            Position2D newBr = scaleInRotatedCoordinateSystem(origBr, scaleCoordinateSystemRotationDeg, scaleX, scaleY, pivotPosition);

            de.dh.cad.architect.model.coords.Vector2D newSizeV = newBr.minus(newCenterPoint).times(2);
            de.dh.cad.architect.model.coords.Vector2D newSize = newSizeV.rotate(-rotationDeg);

            if (newSize.getX().lt(MINIMUM_OBJECT_SIZE) || newSize.getY().lt(MINIMUM_OBJECT_SIZE)) {
                // Break the complete resize operation if one of the objects would become too small
                return;
            }

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
                dragRotateContext.vOp = new Vector2D(op.getX(), op.getY()).minus(mObjectsCenter);

                dragRotateContext.origObjectsData = new ArrayList<>(mSupportObjects);
            },
            (op, dp, sp) -> {
                Vector2D vDp = new Vector2D(dp.getX(), dp.getY()).minus(dragRotateContext.origObjectsCenter);
                float angle = (float) Vector2D.angleBetween(dragRotateContext.vOp, vDp);
                rotateSupportObjects(dragRotateContext.origObjectsData, angle, dragRotateContext.origObjectsCenter);
            }, null, Cursors.createCursorRotate(), Cursors.createCursorRotate());

        mResizeFeature = new BaseImageAncillary(
            ImageUtils.loadSquareIcon(EditSupportObjectsVisualFeedbackManager.class, SIZE_IMAGE_RESOURCE, MODIFICATION_SYMBOLS_SIZE),
            mView);
        mResizeFeature.setViewOrder(Constants.VIEW_ORDER_INTERACTION);
        mView.addAncillaryObject(mResizeFeature);
        var dragSizeContext = new Object() {
            // Start positions of the bounding rect; the rect moves during the operation so we correlate the drag positions to the starting rect
            Vector2D startP1;
            Vector2D origVRotated;
            float coordinateSystemRotationDeg;

            List<SupportObjectLocationData> origObjectsData;
        };
        mResizeFeature.installDragHandler(
            op -> {
                dragSizeContext.coordinateSystemRotationDeg = getBoundingBoxRotationDeg();
                dragSizeContext.startP1 = mBoundingBoxP1; // Opposite edge of resize image
                dragSizeContext.origVRotated = new Vector2D(op.getX(), op.getY()).minus(dragSizeContext.startP1).rotate(-dragSizeContext.coordinateSystemRotationDeg);
                // Avoid division by zero below
                dragSizeContext.origVRotated = new Vector2D(Math.max(dragSizeContext.origVRotated.getX(), 5), Math.max(dragSizeContext.origVRotated.getY(), 5));

                dragSizeContext.origObjectsData = new ArrayList<>(mSupportObjects);
            },
            (op, dp, sp) -> {
                Vector2D newVRotated = new Vector2D(dp.getX(), dp.getY()).minus(dragSizeContext.startP1).rotate(-dragSizeContext.coordinateSystemRotationDeg);
                scaleSupportObjects(dragSizeContext.origObjectsData,
                    dragSizeContext.startP1,
                    getBoundingBoxRotationDeg(),
                    newVRotated.getX() / dragSizeContext.origVRotated.getX(),
                    newVRotated.getY() / dragSizeContext.origVRotated.getY());
            },
            null, Cursor.SE_RESIZE, Cursor.SE_RESIZE);

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
                Length delta = CoordinateUtils.coordsToLength(-dp.subtract(op).getY()); // Y grows to the bottom in JavaFX

                tooltip.setText(MessageFormat.format(Strings.DRAG_HEIGHT, deltaString(delta)));
                tooltip.show(mBoundingBoxShape, window.getX() + sp.getX(), window.getY() + sp.getY());

                changeSupportObjectsHeight(changeHeightContext.origObjectsData, delta);
            },
            (op, dp, sp) -> {
                tooltip.hide();
            }, Cursor.V_RESIZE, Cursor.V_RESIZE);

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
                Length delta = CoordinateUtils.coordsToLength(-dp.subtract(op).getY()); // Y grows to the bottom in JavaFX

                tooltip.setText(MessageFormat.format(Strings.DRAG_ELEVATION, deltaString(delta)));
                tooltip.show(mBoundingBoxShape, window.getX() + sp.getX(), window.getY() + sp.getY());

                changeSupportObjectsElevation(changeElevationContext.origObjectsData, delta);
            },
            (op, dp, sp) -> {
                tooltip.hide();
            }, Cursor.V_RESIZE, Cursor.V_RESIZE);
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

    // Could be moved to some utils class
    private static BoundingBox unionBox(BoundingBox a, Point2D p1, Point2D p2, Point2D p3, Point2D p4) {
        double minX = MathUtils.min4(p1.getX(), p2.getX(), p3.getX(), p4.getX());
        double maxX = MathUtils.max4(p1.getX(), p2.getX(), p3.getX(), p4.getX());
        double minY = MathUtils.min4(p1.getY(), p2.getY(), p3.getY(), p4.getY());
        double maxY = MathUtils.max4(p1.getY(), p2.getY(), p3.getY(), p4.getY());

        minX = Math.min(minX, a.getMinX());
        minY = Math.min(minY, a.getMinY());
        maxX = Math.max(maxX, a.getMaxX());
        maxY = Math.max(maxY, a.getMaxY());

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    @SuppressWarnings("null")
    public void updateVisualObjects(List<SupportObjectLocationData> locationData) {
        mSupportObjects = locationData;
        if (locationData.isEmpty()) {
            mBoundingBoxShape.removeFromView();
            mBoundingBoxShape = null;
            return;
        }

        float boxRotation = 0;
        BoundingBox boundingBox = null;
        Rotate boxRotate = null;
        for (SupportObjectLocationData sold : locationData) {
            Position2D centerPoint = sold.getCenterPoint();
            double centerX = CoordinateUtils.lengthToCoords(centerPoint.getX());
            double centerY = CoordinateUtils.lengthToCoords(centerPoint.getY());
            Dimensions2D size = sold.getSize();
            double width = CoordinateUtils.lengthToCoords(size.getX());
            double height = CoordinateUtils.lengthToCoords(size.getY());
            BoundingBox soBox = new BoundingBox(
                centerX - width / 2,
                centerY - height / 2,
                width, height);
            float soRotation = sold.getRotationDeg();
            if (boundingBox == null) {
                // First object, take rotation from that object
                boxRotation = soRotation;
                boundingBox = soBox;
                boxRotate = new Rotate(boxRotation, centerX, centerY);
            } else {
                Rotate soRotate = new Rotate(soRotation, centerX, centerY);
                try {
                    Point2D tl = boxRotate.inverseTransform(soRotate.transform(new Point2D(soBox.getMinX(), soBox.getMinY())));
                    Point2D tr = boxRotate.inverseTransform(soRotate.transform(new Point2D(soBox.getMaxX(), soBox.getMinY())));
                    Point2D bl = boxRotate.inverseTransform(soRotate.transform(new Point2D(soBox.getMinX(), soBox.getMaxY())));
                    Point2D br = boxRotate.inverseTransform(soRotate.transform(new Point2D(soBox.getMaxX(), soBox.getMaxY())));

                    boundingBox = unionBox(boundingBox, tl, tr, bl, br);
                } catch (NonInvertibleTransformException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (boundingBox != null) {
            // P1 ----- P2
            // |        |
            // |        |
            // P4 ----- P3
            Point2D p1 = boxRotate.transform(new Point2D(boundingBox.getMinX(), boundingBox.getMinY()));
            Point2D p2 = boxRotate.transform(new Point2D(boundingBox.getMaxX(), boundingBox.getMinY()));
            Point2D p3 = boxRotate.transform(new Point2D(boundingBox.getMaxX(), boundingBox.getMaxY()));
            Point2D p4 = boxRotate.transform(new Point2D(boundingBox.getMinX(), boundingBox.getMaxY()));
            List<Double> coords = Arrays.asList(
                p1.getX(), p1.getY(),
                p2.getX(), p2.getY(),
                p3.getX(), p3.getY(),
                p4.getX(), p4.getY());
            mBoundingBoxShape.updateCoords(coords, StrokeType.OUTSIDE);
            mObjectsCenter = new Vector2D((p3.getX() + p1.getX()) / 2, (p3.getY() + p1.getY()) / 2);
            mBoundingBoxP1 = new Vector2D(p1.getX(), p1.getY());

            mChangeHeightFeature.setPosition(p1);
            mRotateFeature.setPosition(p2);
            mResizeFeature.setPosition(p3);
            mChangeElevationFeature.setPosition(p4);
        }
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
