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

public class VerticalRuler extends AbstractRuler {
    public VerticalRuler(UiController uiController) {
        super(uiController);
        prefWidthProperty().set(RULER_WIDTH);

        setOnMouseClicked(mouseEvent -> {
            if (!mouseEvent.isStillSincePress()) {
                return;
            }
            mouseEvent.consume();
            Point2D localPoint = sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            createGuideLine(GuideLineDirection.Horizontal, localPoint.getY());
        });

        updateView();
    }

    @Override
    protected double getTranslation() {
        return mTranslation.getY();
    }

    @Override
    protected double getVisibleRange() {
        return getHeight();
    }

    @Override
    protected double getTextOffset(Bounds textLayoutBounds) {
        return textLayoutBounds.getHeight() / 2;
    }

    @Override
    protected void configureGuideLinesArea() {
        mGuideArrowsArea.translateXProperty().bind(widthProperty().subtract(GUIDE_ARROW_HEIGHT));
    }

    @Override
    protected void updateGuideArrowPosition(GuideArrow guideArrow, double pos) {
        guideArrow.setTranslateY(pos);
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

        children.add(new Line(0, 0, width, 0)); // Start
        children.add(new Line(0, height, width, height)); // End
        children.add(new Line(width - 1, 0, width - 1, height));
        for (ScaleLabel sl : calculateScaleLabels()) {
            double position = sl.getPosition();
            double scaleLength = sl.getScaleLength();
            double textPosition = sl.getTextPosition();
            Optional<Text> oLabel = sl.getOLabel();
            children.add(new Line(width - scaleLength, position, width - 1, position));
            oLabel.ifPresent(label -> {
                label.setRotate(-90);
                label.setX(label.getLayoutBounds().getHeight()/2 - label.getLayoutBounds().getWidth()/2);
                label.setY(textPosition);
                children.add(label);
            });
        }
    }

    @Override
    public void setCursorMarker(Length position) {
        if (position == null) {
            mCursorMarker.setVisible(false);
        } else {
            double width = getWidth();
            double y = lengthToPosition(position);
            mCursorMarker.setStartX(0);
            mCursorMarker.setStartY(y);
            mCursorMarker.setEndX(width);
            mCursorMarker.setEndY(y);
            mCursorMarker.setVisible(true);
        }
    }
}
