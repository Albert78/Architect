package de.dh.cad.architect.ui.controls;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.GuideLine.GuideLineDirection;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.utils.Vector2D;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class VerticalRuler extends Pane {
    protected final Ruler mRuler;

    public VerticalRuler(UiController uiController) {
        mRuler = new Ruler(uiController, GuideLineDirection.Vertical);

        Translate translate = new Translate();
        translate.yProperty().bind(heightProperty());
        mRuler.getTransforms().addAll(translate, new Rotate(-90, 0, 0));
        mRuler.prefWidthProperty().bind(heightProperty());
        prefWidthProperty().bind(mRuler.prefHeightProperty());

        getChildren().add(mRuler);
    }

    public void clearGuideLines() {
        mRuler.clearGuideLines();
    }

    public double addGuideLine(GuideLine guideLine) {
        mRuler.addGuideLine(guideLine);
        return updateGuideLine(guideLine);
    }

    public void removeGuideLine(GuideLine guideLine) {
        mRuler.removeGuideLine(guideLine);
    }

    public double updateGuideLine(GuideLine guideLine) {
        return mRuler.updateGuideLine(guideLine) - getHeight();
    }

    public void setTransform(double scale, Vector2D translation) {
        mRuler.setTransform(scale, getHeight() - translation.getY());
    }

    public void setCursorMarker(Position2D pos) {
        mRuler.setCursorMarker(pos == null ? null : pos.getY());
    }

    public void updateView() {
        mRuler.updateView();
    }
}
