package de.dh.utils.fx.shapes;

import de.dh.utils.Vector2D;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;

public class CurvedArrow extends Group {
    protected static final int ANGLE_CP_1 = 120;
    protected static final int ANGLE_CP_2 = 100;
    protected static final int CONTROL_POINT_1_DISTANCE = 50;
    protected static final int CONTROL_POINT_2_DISTANCE = 60;
    protected static final int ARROW_SIZE = 20;

    protected final ObjectProperty<Vector2D> mSourcePositionProperty = new SimpleObjectProperty<>(Vector2D.EMPTY);
    protected final ObjectProperty<Vector2D> mTargetPositionProperty = new SimpleObjectProperty<>(Vector2D.EMPTY);
    protected final DoubleProperty mEndPointDistanceProperty = new SimpleDoubleProperty(10);
    protected final DoubleProperty mCurveStrokeWidthProperty = new SimpleDoubleProperty(4);
    protected final ObjectProperty<Paint> mColorProperty = new SimpleObjectProperty<>(Color.BLACK);
    protected final DoubleProperty mScaleCompensationProperty = new SimpleDoubleProperty(1);

    protected final Arrow mArrowTip;
    protected final CubicCurve mCurve;
    protected final Scale mScaleCorrection = new Scale();

    public CurvedArrow(Arrow arrow) {
        mArrowTip = arrow;
        mArrowTip.getTransforms().add(mScaleCorrection);
        mArrowTip.fillProperty().bind(mColorProperty);

        mCurve = new CubicCurve();
        mCurve.setStrokeLineCap(StrokeLineCap.ROUND);
        mCurve.strokeProperty().bind(mColorProperty);
        mCurve.setFill(null);

        ObservableList<Node> children = getChildren();
        children.addAll(mCurve, mArrowTip);

        ChangeListener<Vector2D> pointsListener = (prop, oldV, newV) -> {
            updatePoints();
        };
        mSourcePositionProperty.addListener(pointsListener);
        mTargetPositionProperty.addListener(pointsListener);

        mCurve.strokeWidthProperty().bind(mCurveStrokeWidthProperty.multiply(mScaleCompensationProperty));
        mScaleCorrection.xProperty().bind(mScaleCompensationProperty);
        mScaleCorrection.yProperty().bind(mScaleCompensationProperty);

        updatePoints();
    }

    public CurvedArrow() {
        this(Arrow.createDefault());
    }

    public Arrow getArrowTip() {
        return mArrowTip;
    }

    public CubicCurve getCurve() {
        return mCurve;
    }

    /**
     * Updates the curve's start and end points and the control points for the arc, according to the
     * arrow's source and target positions.
     * This method can be overridden to change the curve's shape.
     */
    protected void updatePoints() {
        Vector2D source = getSourcePosition();
        Vector2D target = getTargetPosition();
        double endPointDistance = getEndPointDistance();
        double scaleCompensation = getScaleCompensation();
        Vector2D s2t = target.minus(source);

        // To have a well-looking curve, we position both control points near the target position.
        // Those vectors are the directions of the control points starting at the target position of the curve.
        Vector2D t2c1 = s2t.rotate(-ANGLE_CP_1).toUnitVector();
        Vector2D t2c2 = s2t.rotate(-ANGLE_CP_2).toUnitVector();
        double angleT2 = Vector2D.angleBetween(Vector2D.X_ONE, t2c2);

        // The control points determine the tangents of the curve's start and end and must be given as
        // points in the curve's coordinate system, where the vector from the curve's start and end points is
        // interpreted as the tangent.
        // The bow of the curve should not become bigger when we are scaled, thus we compensate the scaling with
        // the inverse value of the scale factor, which is the scale compensation.
        Vector2D c1 = t2c1.times(CONTROL_POINT_1_DISTANCE * scaleCompensation).plus(target);
        Vector2D c2 = t2c2.times(CONTROL_POINT_2_DISTANCE * scaleCompensation).plus(target);

        // The start and end points of the curve are positioned with a small distance to the source and target positions,
        // in direction of the curve's tangent. It should just look as if the line starts a bit later and stops
        // a bit earlier, like if you would draw it on paper.
        // Those distances should not become bigger when we are scaled, so we compensate the distance with the scale compensation.
        double scaledEndPointDistance = endPointDistance * scaleCompensation;
        Vector2D s = c1.minus(source).scaleToLength(scaledEndPointDistance).plus(source);
        Vector2D e = t2c2.times(scaledEndPointDistance).plus(target);

        // Since e is the logical end of our arrow, it represents the tip of the arrow. We must end our curve a bit earlier
        // to avoid the curve's stroke end overspreading the arrow's tip.
        Vector2D lE = t2c2.times((endPointDistance + ARROW_SIZE / 2) * scaleCompensation).plus(target);

        mCurve.setStartX(s.getX());
        mCurve.setStartY(s.getY());
        mCurve.setEndX(lE.getX());
        mCurve.setEndY(lE.getY());

        mCurve.setControlX1(c1.getX());
        mCurve.setControlY1(c1.getY());
        mCurve.setControlX2(c2.getX());
        mCurve.setControlY2(c2.getY());

        mArrowTip.setPosition(e);
        mArrowTip.setAngle(angleT2 + 180);
    }

    public Vector2D getSourcePosition() {
        return mSourcePositionProperty.get();
    }

    public void setSourcePosition(Vector2D value) {
        mSourcePositionProperty.set(value);
    }

    public ObjectProperty<Vector2D> getSourcePositionProperty() {
        return mSourcePositionProperty;
    }

    public Vector2D getTargetPosition() {
        return mTargetPositionProperty.get();
    }

    public void setTargetPosition(Vector2D value) {
        mTargetPositionProperty.set(value);
    }

    public ObjectProperty<Vector2D> getTargetPositionProperty() {
        return mTargetPositionProperty;
    }

    public double getEndPointDistance() {
        return mEndPointDistanceProperty.get();
    }

    public void setEndPointDistance(double value) {
        mEndPointDistanceProperty.set(value);
    }

    public DoubleProperty getEndPointDistanceProperty() {
        return mEndPointDistanceProperty;
    }

    public double getArrowCurveStrokeWidth() {
        return mCurveStrokeWidthProperty.get();
    }

    public void setArrowCurveStrokeWidth(double value) {
        mCurveStrokeWidthProperty.set(value);
    }

    public DoubleProperty getArrowCurveStrokeWidthProperty() {
        return mCurveStrokeWidthProperty;
    }

    public Paint getColor() {
        return mColorProperty.get();
    }

    public void setColor(Paint value) {
        mColorProperty.set(value);
    }

    public ObjectProperty<Paint> getColorProperty() {
        return mColorProperty;
    }

    public double getScaleCompensation() {
        return mScaleCompensationProperty.get();
    }

    public void setScaleCompensation(double value) {
        mScaleCompensationProperty.set(value);
    }

    public DoubleProperty getScaleCompensationProperty() {
        return mScaleCompensationProperty;
    }
}
