package de.dh.cad.architect.ui.view.construction;

import de.dh.cad.architect.model.coords.Position2D;
import javafx.geometry.Point2D;

public abstract class UiPlanPosition {
    public static final UiPlanPosition EMPTY = new UiPlanPosition(new Point2D(0, 0)) {
        @Override
        public Position2D getModelPosition() {
            return Position2D.zero();
        }

        @Override
        public Point2D getPointOnPlan() {
            return new Point2D(0, 0);
        }
    };

    protected final Point2D mPointInScene;

    public UiPlanPosition(Point2D pointInScene) {
        mPointInScene = pointInScene;
    }

    public UiPlanPosition(double sceneX, double sceneY) {
        this(new Point2D(sceneX, sceneY));
    }

    public abstract Position2D getModelPosition();

    public abstract Point2D getPointOnPlan();

    public Point2D getPointInScene() {
        return mPointInScene;
    }

    @Override
    public String toString() {
        return String.valueOf(mPointInScene);
    }
}
