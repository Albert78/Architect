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
package de.dh.cad.architect.ui.controls;

import java.util.Optional;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.GuideLine.GuideLineDirection;
import de.dh.cad.architect.ui.controller.UiController;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class HorizontalRuler extends AbstractRuler {
    public HorizontalRuler(UiController uiController) {
        super(uiController);
        prefHeightProperty().set(RULER_WIDTH);

        setOnMouseClicked(mouseEvent -> {
            if (!mouseEvent.isStillSincePress()) {
                return;
            }
            mouseEvent.consume();
            Point2D localPoint = sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            createGuideLine(GuideLineDirection.Vertical, localPoint.getX());
        });

        updateView();
    }

    @Override
    protected double getTranslation() {
        return mTranslation.getX();
    }

    @Override
    protected double getVisibleRange() {
        return getWidth();
    }

    @Override
    protected double getTextOffset(Bounds textLayoutBounds) {
        return -textLayoutBounds.getWidth() / 2;
    }

    @Override
    protected void configureGuideLinesArea() {
        mGuideArrowsArea.translateYProperty().bind(heightProperty().subtract(GUIDE_ARROW_HEIGHT));
    }

    @Override
    protected void updateGuideArrowPosition(GuideArrow guideArrow, double pos) {
        guideArrow.setTranslateX(pos);
    }

    @Override
    public void doUpdateView() {
        for (GuideArrow guideArrow : mGuideArrows.values()) {
            updateGuideArrow(guideArrow);
        }

        ObservableList<Node> children = mRulerContentsArea.getChildren();
        children.clear();

        double height = getHeight();
        double width = getWidth();

        children.add(new Line(0, 0, 0, height)); // Start
        children.add(new Line(0, 0, width, 0)); // End
        children.add(new Line(0, height - 1, width, height - 1));
        for (ScaleLabel sl : calculateScaleLabels()) {
            double position = sl.getPosition();
            double scaleLength = sl.getScaleLength();
            double textPosition = sl.getTextPosition();
            Optional<Text> oLabel = sl.getOLabel();
            children.add(new Line(position, height - scaleLength, position, height - 1));
            oLabel.ifPresent(label -> {
                label.setX(textPosition);
                label.setY(label.getLayoutBounds().getHeight());
                children.add(label);
            });
        }
    }

    @Override
    public void setCursorMarker(Length position) {
        if (position == null) {
            mCursorMarker.setVisible(false);
        } else {
            double height = getHeight();
            double x = lengthToPosition(position);
            mCursorMarker.setStartX(x);
            mCursorMarker.setStartY(0);
            mCursorMarker.setEndX(x);
            mCursorMarker.setEndY(height);
            mCursorMarker.setVisible(true);
        }
    }
}
