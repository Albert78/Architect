package de.dh.cad.architect.ui.controls;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.GuideLine.GuideLineDirection;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.utils.Vector2D;
import javafx.scene.layout.Pane;

public class HorizontalRuler extends Pane {
    protected final Ruler mRuler;

    public HorizontalRuler(UiController uiController) {
        mRuler = new Ruler(uiController, GuideLineDirection.Horizontal);

        mRuler.prefWidthProperty().bind(widthProperty());
        prefHeightProperty().bind(mRuler.prefHeightProperty());

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
        return mRuler.updateGuideLine(guideLine);
    }

    public void setTransform(double scale, Vector2D translation) {
        mRuler.setTransform(scale, translation.getX());
    }

    public void setCursorMarker(Position2D pos) {
        mRuler.setCursorMarker(pos == null ? null : pos.getX());
    }

    public void updateView() {
        mRuler.updateView();
    }
}
