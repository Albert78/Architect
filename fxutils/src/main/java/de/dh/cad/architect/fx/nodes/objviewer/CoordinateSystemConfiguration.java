package de.dh.cad.architect.fx.nodes.objviewer;

import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

public class CoordinateSystemConfiguration {
    public enum Axis {
        X, Y, Z
    }

    public static class AxisConfiguration {
        protected final Axis mAxis;
        protected String mMinLabel;
        protected String mMaxLabel;

        public AxisConfiguration(Axis axis, String minLabel, String maxLabel) {
            mAxis = axis;
            mMinLabel = minLabel;
            mMaxLabel = maxLabel;
        }

        public Axis getAxis() {
            return mAxis;
        }

        public String getMinLabel() {
            return mMinLabel;
        }

        public void setMinLabel(String value) {
            mMinLabel = value;
        }

        public String getMaxLabel() {
            return mMaxLabel;
        }

        public void setMaxLabel(String value) {
            mMaxLabel = value;
        }
    }

    protected final AxisConfiguration mXAxisConfig;
    protected final AxisConfiguration mYAxisConfig;
    protected final AxisConfiguration mZAxisConfig;

    protected final Transform mCoordinateTransform;

    public CoordinateSystemConfiguration(Transform coordinateTransform,
        AxisConfiguration xAxisConfig, AxisConfiguration yAxisConfig, AxisConfiguration zAxisConfig) {
        mCoordinateTransform = coordinateTransform;
        mXAxisConfig = xAxisConfig;
        mYAxisConfig = yAxisConfig;
        mZAxisConfig = zAxisConfig;
    }

    public static CoordinateSystemConfiguration javaFx() {
        return new CoordinateSystemConfiguration(new Affine(),
            new AxisConfiguration(Axis.X, "-X", "+X"),
            new AxisConfiguration(Axis.Y, "-Y", "+Y"),
            new AxisConfiguration(Axis.Z, "-Z", "+Z"));
    }

    public static CoordinateSystemConfiguration architect() {
        return new CoordinateSystemConfiguration(createTransformArchitectToJavaFx(),
            new AxisConfiguration(Axis.X, "-X", "+X"),
            new AxisConfiguration(Axis.Y, "+Z", "-Z"),
            new AxisConfiguration(Axis.Z, "-Y", "+Y"));
    }

    // Attention: This is not the same as for displaying objects in Architect's 3D view!
    // Here, we use the Y axis for Z
    public static Transform createTransformArchitectToJavaFx() {
        return new Rotate(90, Rotate.X_AXIS);
    }

    /**
     * Gets the transformation which needs to be applied to an object given in its native coordinate
     * system (as shown on the visual axis configurations) to fit the JavaFX coordinate system.
     */
    public Transform getCoordinateTransform() {
        return mCoordinateTransform;
    }

    public AxisConfiguration getXAxisConfig() {
        return mXAxisConfig;
    }

    public AxisConfiguration getYAxisConfig() {
        return mYAxisConfig;
    }

    public AxisConfiguration getZAxisConfig() {
        return mZAxisConfig;
    }
}
