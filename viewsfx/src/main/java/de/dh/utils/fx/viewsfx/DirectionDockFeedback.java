package de.dh.utils.fx.viewsfx;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

public class DirectionDockFeedback implements IDockFeedback {
    protected final IDockZone mParent;
    protected final Bounds mBounds;

    protected Rectangle mFeedbackShape = null;

    public DirectionDockFeedback(IDockZone parent, Bounds bounds) {
        mParent = parent;
        mBounds = bounds;
    }

    @Override
    public void install() {
        if (mFeedbackShape != null) {
            throw new IllegalStateException("Feedback is already installed");
        }
        mFeedbackShape = new Rectangle(mBounds.getMinX(), mBounds.getMinY(), mBounds.getWidth(), mBounds.getHeight());
        mFeedbackShape.setFill(null);
        mFeedbackShape.setStroke(Color.BLUE);
        mFeedbackShape.setStrokeWidth(2);
        mFeedbackShape.setStrokeType(StrokeType.INSIDE);
        mParent.getFeedbackPane().getChildren().add(mFeedbackShape);
    }

    @Override
    public void uninstall() {
        if (mFeedbackShape == null) {
            throw new IllegalStateException("Feedback is not installed");
        }
        mParent.getFeedbackPane().getChildren().remove(mFeedbackShape);
        mFeedbackShape = null;
    }
}
