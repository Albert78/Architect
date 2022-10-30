package de.dh.utils.fx.viewsfx;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class TabDockFeedback implements IDockFeedback {
    protected final TabDockHost mParent;
    protected final Bounds mPosition;

    protected Line mFeedbackLine;

    public TabDockFeedback(TabDockHost parent, Bounds position) {
        mParent = parent;
        mPosition = position;
    }

    @Override
    public void install() {
        if (mFeedbackLine != null) {
            throw new IllegalStateException("Feedback is already installed");
        }

        mFeedbackLine = new Line(mPosition.getMinX(), mPosition.getMinY(), mPosition.getMaxX(), mPosition.getMaxY());
        mFeedbackLine.setStroke(Color.BLUE);
        mFeedbackLine.setStrokeWidth(2);
        mParent.getFeedbackPane().getChildren().add(mFeedbackLine);
    }

    @Override
    public void uninstall() {
        if (mFeedbackLine == null) {
            throw new IllegalStateException("Feedback is not installed");
        }
        mParent.getFeedbackPane().getChildren().remove(mFeedbackLine);
        mFeedbackLine = null;
    }
}
