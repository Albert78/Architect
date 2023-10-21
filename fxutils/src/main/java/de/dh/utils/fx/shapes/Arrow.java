package de.dh.utils.fx.shapes;

import de.dh.utils.Vector2D;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;

public class Arrow extends Polygon {
    protected final Rotate mR = new Rotate();

    public Arrow(double... points) {
        super(points);
        mR.setAxis(Rotate.Z_AXIS);
        getTransforms().add(mR);
    }

    public static Arrow createDefault() {
        double[] arrowShape = new double[] {0, 0, 10, 20, -10, 20};
        return new Arrow(arrowShape);
    }

    public double getAngle() {
        return mR.getAngle();
    }

    public void setAngle(double value) {
        mR.setAngle(value + 90);
    }

    public Vector2D getPosition() {
        return new Vector2D(getTranslateX(), getTranslateY());
    }

    public void setPosition(Vector2D arrowTip) {
        setTranslateX(arrowTip.getX());
        setTranslateY(arrowTip.getY());
    }
}
