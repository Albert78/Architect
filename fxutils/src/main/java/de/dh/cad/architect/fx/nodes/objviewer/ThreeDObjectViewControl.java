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
package de.dh.cad.architect.fx.nodes.objviewer;


import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewConfiguration.CameraType;
import de.dh.utils.fx.FxUtils;
import de.dh.utils.fx.ImageUtils;
import de.dh.utils.fx.LightConfiguration;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.ParallelCamera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class ThreeDObjectViewControl extends StackPane {
    private static final int PERSPECTIVE_CAMERA_TRANSLATE_Z = -200; // To avoid clipping at near clip
    private static final int PARALLEL_CAMERA_TRANSLATE_Z = 0; // Center camera at object's position because the clipping range for parallel camera seems to be very small / hard coded?

    // We normalize the extents of the object via a scale transform to simplify the interaction between the different aspects like
    // the light indicator, the camera's field of view and its clipping panes, default scale parameters etc.
    private static final int NORMALIZED_OBJECT_SIZE = 100;
    private static final int NORMALIZED_LIGHT_DRAG_INDICATOR_SIZE = 50;

    private static Logger log = LoggerFactory.getLogger(ThreeDObjectViewControl.class);

    protected EventHandler<ScrollEvent> mZoomEventHandler = new EventHandler<>() {
        @Override
        public void handle(ScrollEvent event) {
            zoom(-event.getDeltaY());
        }
    };
    protected Group mRootGroup;
    protected PointLight mPointLight;
    protected Group mPointLightGroup;
    protected AmbientLight mAmbientLight;
    protected SubScene mSubScene;
    protected PerspectiveCamera mPerspectiveCamera;
    protected ParallelCamera mParallelCamera;

    protected Scale mObjectSizeCompensationScale = new Scale();

    protected Group mTransformRoot;

    protected final ObjectProperty<CameraType> mCameraTypeProperty = new SimpleObjectProperty<>(CameraType.Perspective);

    protected final DoubleProperty mPointLightIntensityProperty = new SimpleDoubleProperty();
    protected final DoubleProperty mAmbientLightIntensityProperty = new SimpleDoubleProperty();

    protected final DoubleProperty mLightAngleX = new SimpleDoubleProperty(0);
    protected final DoubleProperty mLightAngleZ = new SimpleDoubleProperty(0);
    protected final DoubleProperty mLightDistance = new SimpleDoubleProperty(0);
    protected final Node mLightDragIndicator;

    protected final DoubleProperty mRotationAngleX = new SimpleDoubleProperty(0);
    protected final DoubleProperty mRotationAngleY = new SimpleDoubleProperty(0);
    protected final DoubleProperty mScaleFactor = new SimpleDoubleProperty(1);

    protected final Rotate mXRotate = new Rotate(0, Rotate.X_AXIS);
    protected final Rotate mYRotate = new Rotate(0, Rotate.Y_AXIS);

    protected CoordinateSystemNode mCoordinateSystem = null;

    protected Node mObjView;

    protected final Scale mScale = new Scale();

    public ThreeDObjectViewControl() {
        mLightDragIndicator = loadFlashlight();

        mTransformRoot = new Group();

        mPointLight = new PointLight();
        mPointLightGroup = new Group(mPointLight, mLightDragIndicator);
        mAmbientLight = new AmbientLight(Color.grayRgb(70));

        class LightIntensityToLightColorMapper implements ChangeListener<Number> {
            LightBase mLight;

            LightIntensityToLightColorMapper(LightBase light) {
                mLight = light;
            }

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                double intensity = newValue.doubleValue();
                mLight.setColor(lightIntensityToColor(intensity));
            }
        }

        mPointLightIntensityProperty.addListener(new LightIntensityToLightColorMapper(mPointLight));
        mAmbientLightIntensityProperty.addListener(new LightIntensityToLightColorMapper(mAmbientLight));

        mRootGroup = new Group(mTransformRoot, mAmbientLight, mPointLightGroup);
        mRootGroup.getTransforms().add(mObjectSizeCompensationScale);
        mSubScene = new SubScene(mRootGroup, 0, 0, true, SceneAntialiasing.BALANCED);

        DoubleBinding halfViewportWidthProperty = mSubScene.widthProperty().divide(2);
        DoubleBinding halfViewportHeightProperty = mSubScene.heightProperty().divide(2);

        /*
         * With fixedEyeAtCameraZero set to true, the object's projection would automatically be
         * moved to the viewport's middle position and the camera's position in Z direction would be
         * adjusted to match the field of view.
         *
         * See PerspectiveCamera#computePosition(Vec3d):
         *
         *
         *  final double halfViewWidth = getViewWidth() / 2.0;
         *  final double halfViewHeight = getViewHeight() / 2.0;
         *  final double halfViewDim = isVerticalFieldOfView()
         *          ? halfViewHeight : halfViewWidth;
         *  final double distanceZ = halfViewDim
         *          / Math.tan(Math.toRadians(getFieldOfView() / 2.0));
         *
         *  position.set(halfViewWidth, halfViewHeight, -distanceZ);
         *
         *
         * Actually, this is the desired behavior.
         * But since we want the projection's size to be equal for PerspectiveCamera and ParallelCamera and
         * ParallelCamera doesn't support that automatic translate/scale function, we switch
         * fixedEyeAtCameraZero to false in PerspectiveCamera.
         *
         * The manual projection correction to make the object's projection appear in the middle of the viewport and
         * extend over a good-looking fraction of the viewport, includes two bindings:
         * - Move the root object's origin of coordinates to the center of the viewport
         * - Scale the object
         */
        mPerspectiveCamera = new PerspectiveCamera(false);
        mPerspectiveCamera.setNearClip(0.1);
        mPerspectiveCamera.setFarClip(10000);

        // Translation doesn't grow or shrink the projection for PerspectiveCamera if fixedEyeAtCameraZero is false,
        // but translation is necessary to move near clip area away from the object
        mPerspectiveCamera.setTranslateZ(PERSPECTIVE_CAMERA_TRANSLATE_Z);
        mPerspectiveCamera.translateXProperty().bind(halfViewportWidthProperty.negate());
        mPerspectiveCamera.translateYProperty().bind(halfViewportHeightProperty.negate());

        mParallelCamera = new ParallelCamera();
        mParallelCamera.setNearClip(0.1);
        mParallelCamera.setFarClip(10000);

        mParallelCamera.setTranslateZ(PARALLEL_CAMERA_TRANSLATE_Z);
        mParallelCamera.translateXProperty().bind(halfViewportWidthProperty.negate());
        mParallelCamera.translateYProperty().bind(halfViewportHeightProperty.negate());

        mCameraTypeProperty.addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends CameraType> observable, CameraType oldValue, CameraType newValue) {
                applyCameraType();
            }
        });

        applyCameraType();
        mSubScene.setFill(Color.SILVER);

        mSubScene.setManaged(false); // Necessary to prevent the outer panel from taking the scene's size into account for its own size
        mSubScene.heightProperty().bind(heightProperty());
        mSubScene.widthProperty().bind(widthProperty());

        Pane infoPane = createInfoPane();
        getChildren().addAll(mSubScene, infoPane);
        FxUtils.addClippingToBounds(this);

        setLightDistance(NORMALIZED_OBJECT_SIZE * 1.5);

        Rotate lightXRotate = new Rotate(0, Rotate.X_AXIS);
        Rotate lightZRotate = new Rotate(0, Rotate.Z_AXIS);
        lightXRotate.angleProperty().bind(mLightAngleX);
        lightZRotate.angleProperty().bind(mLightAngleZ);
        mLightDragIndicator.setVisible(false);
        Translate lightTranslate = new Translate();
        lightTranslate.yProperty().bind(mLightDistance.negate());
        mPointLight.getTransforms().add(lightTranslate);
        Translate lightDragIndicatorTranslate = new Translate();
        lightDragIndicatorTranslate.yProperty().bind(mLightDistance.negate());
        mLightDragIndicator.getTransforms().add(0, lightDragIndicatorTranslate);
        mPointLightGroup.getTransforms().addAll(lightXRotate, lightZRotate, mScale);

        mXRotate.angleProperty().bind(mRotationAngleX);
        mYRotate.angleProperty().bind(mRotationAngleY);
        mTransformRoot.getTransforms().addAll(mXRotate, mYRotate, mScale);

        mScale.setPivotX(0);
        mScale.setPivotY(0);
        mScale.setPivotZ(0);

        mScale.xProperty().bind(mScaleFactor);
        mScale.yProperty().bind(mScaleFactor);
        mScale.zProperty().bind(mScaleFactor);

        initMouse();

        applyConfiguration(ThreeDObjectViewConfiguration.standardPerspective());
    }

    protected static String[] getToolTipInfoLines() {
        return new String[] {
            Strings.TOOLTIP_LINE_1,
            Strings.TOOLTIP_LINE_2,
            Strings.TOOLTIP_LINE_3,
            Strings.TOOLTIP_LINE_4,
        };
    }

    protected static Pane createInfoPane() {
        Button infoButton = new Button("i");
        infoButton.setLayoutX(10);
        infoButton.setLayoutY(10);
        infoButton.setOnAction(event -> {
            Tooltip tt = new Tooltip(StringUtils.join(getToolTipInfoLines(), "\n"));

            Point2D p = infoButton.localToScreen(0, 0);

            tt.setAutoHide(true);
            tt.show(infoButton, p.getX(), p.getY());
        });

        AnchorPane result = new AnchorPane(infoButton);
        AnchorPane.setRightAnchor(infoButton, 10d);
        AnchorPane.setBottomAnchor(infoButton, 10d);
        return result;
    }

    protected static Node loadFlashlight() {
        Node lightDragIndicator;
        try {
            lightDragIndicator = Flashlight.create();
            FxUtils.normalizeAndCenter(lightDragIndicator, NORMALIZED_LIGHT_DRAG_INDICATOR_SIZE, true);
            Rotate rotate = new Rotate(90, new Point3D(1, 0, 0));
            lightDragIndicator.getTransforms().add(0, rotate);
        } catch (IOException e) {
            log.warn("Error loading light drag indicator", e);
            lightDragIndicator = new Group();
        }
        return lightDragIndicator;
    }

    protected static Color lightIntensityToColor(double intensity) {
        if (intensity > 1) {
            intensity = 1;
        }
        if (intensity < 0) {
            intensity = 0;
        }
        return Color.gray(intensity);
    }

    protected void initMouse() {
        var dragContext = new Object() {
            double StartAnchorX;
            double StartAnchorY;
            double StartAnchorAngleX = 0;
            double StartAnchorAngleY = 0;
            double StartAnchorLightAngleX = 0;
            double StartAnchorLightAngleZ = 0;
        };

        setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                setCameraType(getCameraType() == CameraType.Perspective ? CameraType.Parallel : CameraType.Perspective);
                return;
            }
            dragContext.StartAnchorX = event.getSceneX();
            dragContext.StartAnchorY = event.getSceneY();
            dragContext.StartAnchorAngleX = mRotationAngleX.get();
            dragContext.StartAnchorAngleY = mRotationAngleY.get();
            dragContext.StartAnchorLightAngleX = mLightAngleX.get();
            dragContext.StartAnchorLightAngleZ = mLightAngleZ.get();

            if (event.isPrimaryButtonDown()) {
                mLightDragIndicator.setVisible(true);
            }
        });

        setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                mLightAngleX.set(dragContext.StartAnchorLightAngleX - (dragContext.StartAnchorY - event.getSceneY()));
                mLightAngleZ.set(dragContext.StartAnchorLightAngleZ - (dragContext.StartAnchorX - event.getSceneX()));
            } else if (event.isSecondaryButtonDown()) {
                mRotationAngleX.set(dragContext.StartAnchorAngleX - (dragContext.StartAnchorY - event.getSceneY()));
                mRotationAngleY.set(dragContext.StartAnchorAngleY + dragContext.StartAnchorX - event.getSceneX());
            }
        });

        setOnMouseReleased(event -> {
            mLightDragIndicator.setVisible(false);
        });
        addEventHandler(ScrollEvent.SCROLL, mZoomEventHandler);
    }

    public ThreeDObjectViewConfiguration getConfiguration() {
        return ThreeDObjectViewConfiguration.standardPerspective()
                .setCameraType(getCameraType())

                .setRotationAngleX(mRotationAngleX.get())
                .setRotationAngleY(mRotationAngleY.get())
                .setScaleFactor(getScale())

                .setPointLightOn(isPointLightOn())
                .setPointLightIntensity(getPointLightIntensity())

                .setAmbientLightOn(isAmbientLightOn())
                .setAmbientLightIntensity(getAmbientLightIntensity())

                .setLightAngleX(mLightAngleX.get())
                .setLightAngleZ(mLightAngleZ.get())

                .setCoordinateSystemVisible(mCoordinateSystem != null);
    }

    public void applyConfiguration(ThreeDObjectViewConfiguration config) {
        setCameraType(config.getCameraType());

        mRotationAngleX.set(config.getRotationAngleX());
        mRotationAngleY.set(config.getRotationAngleY());
        mScaleFactor.set(config.getScaleFactor());

        setPointLightOn(config.isPointLightOn());
        setPointLightIntensity(config.getPointLightIntensity());
        setAmbientLightOn(config.isAmbientLightOn());
        setAmbientLightIntensity(config.getAmbientLightIntensity());

        mLightAngleX.set(config.getLightAngleX());
        mLightAngleZ.set(config.getLightAngleZ());
        setCoordinateSystemVisible(config.isCoordinateSystemVisible());
    }

    public ObservableValue<CameraType> cameraTypeProperty() {
        return mCameraTypeProperty;
    }

    public CameraType getCameraType() {
        return mCameraTypeProperty.get();
    }

    public void setCameraType(CameraType value) {
        mCameraTypeProperty.set(value);
    }

    protected CameraType getActualCameraType() {
        return mSubScene.getCamera() instanceof PerspectiveCamera ? CameraType.Perspective : CameraType.Parallel;
    }

    protected void applyCameraType() {
        CameraType target = getCameraType();
        if (getActualCameraType() == target) {
            return;
        }
        switch (target) {
        case Parallel:
            mSubScene.setCamera(mParallelCamera);
            break;
        case Perspective:
            mSubScene.setCamera(mPerspectiveCamera);
            break;
        }
    }

    public BooleanProperty pointLightOnProperty() {
        return mPointLight.lightOnProperty();
    }

    public boolean isPointLightOn() {
        return pointLightOnProperty().get();
    }

    public void setPointLightOn(boolean value) {
        pointLightOnProperty().set(value);
    }

    public BooleanProperty ambientLightOnProperty() {
        return mAmbientLight.lightOnProperty();
    }

    public boolean isAmbientLightOn() {
        return ambientLightOnProperty().get();
    }

    public void setAmbientLightOn(boolean value) {
        ambientLightOnProperty().set(value);
    }

    public DoubleProperty pointLightIntensityProperty() {
        return mPointLightIntensityProperty;
    }

    public double getPointLightIntensity() {
        return mPointLightIntensityProperty.get();
    }

    public void setPointLightIntensity(double value) {
        mPointLightIntensityProperty.set(value);
    }

    public DoubleProperty ambientLightIntensityProperty() {
        return mAmbientLightIntensityProperty;
    }

    public double getAmbientLightIntensity() {
        return mAmbientLightIntensityProperty.get();
    }

    public void setAmbientLightIntensity(double value) {
        mAmbientLightIntensityProperty.set(value);
    }

    public double getRotationAngleX() {
        return mRotationAngleX.get();
    }

    public void setRotationAngleX(double value) {
        mRotationAngleX.set(value);
    }

    public double getRotationAngleY() {
        return mRotationAngleY.get();
    }

    public void setRotationAngleY(double value) {
        mRotationAngleY.set(value);
    }

    public double getScale() {
        return mScaleFactor.get();
    }

    public void setScale(double value) {
        mScaleFactor.set(value);
    }

    public double getLightAngleX() {
        return mLightAngleX.get();
    }

    public void setLightAngleX(double value) {
        mLightAngleX.set(value);
    }

    public double getLightAngleZ() {
        return mLightAngleZ.get();
    }

    public void setLightAngleZ(double value) {
        mLightAngleZ.set(value);
    }

    public double getLightDistance() {
        return mLightDistance.get();
    }

    public void setLightDistance(double value) {
        mLightDistance.set(value);
    }

    public Node getObjView() {
        return mObjView;
    }

    /**
     * Sets the object for this view.
     * @param value The 3D object to be shown.
     * @param normalizeToSize The size the object should be stretched to in its maximum extent.
     * This is normally a bit less then the the minimum X/Y extend of the desired size of this control.
     */
    public void setObjView(Node value, int normalizeToSize) {
        double scale = normalizeToSize / NORMALIZED_OBJECT_SIZE;
        mObjectSizeCompensationScale.setX(scale);
        mObjectSizeCompensationScale.setY(scale);
        mObjectSizeCompensationScale.setZ(scale);

        ObservableList<Node> children = mTransformRoot.getChildren();
        if (mObjView != null) {
            children.remove(mObjView);
        }
        mObjView = value;
        if (mObjView != null) {
            children.add(mObjView);
            FxUtils.normalizeAndCenter(mObjView, NORMALIZED_OBJECT_SIZE, true);
        }
    }

    public boolean isCoordinateSystemVisible() {
        return mCoordinateSystem != null;
    }

    public void setCoordinateSystemVisible(boolean value) {
        boolean visible = mCoordinateSystem != null;
        if (value == visible) {
            return;
        }
        ObservableList<Node> rootChildren = mRootGroup.getChildren();
        if (mCoordinateSystem != null) {
            rootChildren.remove(mCoordinateSystem);
            mCoordinateSystem = null;
        }
        if (value) {
            mCoordinateSystem = new CoordinateSystemNode();
            rootChildren.add(mCoordinateSystem);
            mCoordinateSystem.getTransforms().addAll(mXRotate, mYRotate);
        }
    }

    protected void zoom(double value) {
        double val = value < 0 ? 0.9 : 1.1;
        scale(val);
    }

    protected void scale(double value) {
        setScale(getScale() * value);
    }

    public Image takeSnapshot(int imageWidth, int imageHeight) {
        return takeSnapshot(imageWidth, imageHeight, Color.TRANSPARENT);
    }

    public Image takeSnapshot(int imageWidth, int imageHeight, Color backgroundColor) {
        // Borrow object for snapshot
        ObservableList<Node> children = mTransformRoot.getChildren();
        children.remove(mObjView);
        Group root = new Group(mObjView);
        try {
            root.getTransforms().addAll(mXRotate, mYRotate);
            return ImageUtils.takeSnapshot(root, imageWidth, imageHeight, backgroundColor, getCameraType(),
                new LightConfiguration(isPointLightOn(), lightIntensityToColor(getPointLightIntensity()),
                    getLightAngleX(), getLightAngleZ(), getLightDistance()),
                new LightConfiguration(isAmbientLightOn(), lightIntensityToColor(getAmbientLightIntensity())));
        } finally {
            root.getChildren().remove(mObjView);
            children.add(mObjView);
        }
    }
}
