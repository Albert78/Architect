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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.GuideLine.GuideLineDirection;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.utils.fx.FxUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class Ruler extends Pane {
    protected static final double GUIDE_ARROW_VISIBLE_OPACITY = 1.0;
    protected static final double GUIDE_ARROW_INVISIBLE_OPACITY = 0.3;

    protected class ScaleLabel {
        protected final double mPosition;
        protected final double mScaleLength;
        protected final Optional<Text> mOLabel;
        protected final double mTextPosition;
        public ScaleLabel(double position, double scaleLength, Optional<Text> oLabel, double textPosition) {
            mPosition = position;
            mScaleLength = scaleLength;
            mOLabel = oLabel;
            mTextPosition = textPosition;
        }
        public double getPosition() {
            return mPosition;
        }
        public double getScaleLength() {
            return mScaleLength;
        }
        public Optional<Text> getOLabel() {
            return mOLabel;
        }
        public double getTextPosition() {
            return mTextPosition;
        }
    }

    private static final double GUIDE_ARROW_DRAG_SNAP_TO_ORIGINAL_POS_THRESHOLD = 100;

    public static final int RULER_WIDTH = 30;
    public static final int SCALE_LENGTH = 10;
    public static final int MIDDLE_SCALE_LENGTH = 7;
    public static final int TENTH_SCALE_LENGTH = 5;

    protected final UiController mUIController;
    protected final GuideLineDirection mDirection;
    protected final Group mRulerContentsArea;
    protected final Group mGuideArrowsArea;
    protected final Line mCursorMarker;
    protected final Map<String, GuideArrow> mGuideArrows = new TreeMap<>();
    protected double mScale = 1;
    protected double mTranslation = 0;

    public Ruler(UiController uiController, GuideLineDirection direction) {
        mUIController = uiController;
        mDirection = direction;
        mGuideArrowsArea = new Group();
        mRulerContentsArea = new Group();
        mCursorMarker = new Line();
        mCursorMarker.setMouseTransparent(true);
        mCursorMarker.setVisible(false);

        // Guide arrows are in front of ruler contents
        mCursorMarker.setViewOrder(0);
        mGuideArrowsArea.setViewOrder(1);
        mRulerContentsArea.setViewOrder(2);

        mGuideArrowsArea.translateYProperty().bind(heightProperty().subtract(GuideArrow.GUIDE_ARROW_HEIGHT));
        ObservableList<Node> children = getChildren();
        children.addAll(mGuideArrowsArea, mRulerContentsArea, mCursorMarker);

        setStyle("-fx-background-color: white;");
        FxUtils.addClippingToBounds(this);

        ChangeListener<Number> updateListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                updateView();
            }
        };
        heightProperty().addListener(updateListener);
        widthProperty().addListener(updateListener);

        setPrefHeight(RULER_WIDTH);

        setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY || !mouseEvent.isStillSincePress()) {
                return;
            }
            mouseEvent.consume();
            Point2D localPoint = sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            createGuideLine(localPoint.getX());
        });

        updateView();
    }

    protected double getVisibleRange() {
        return getWidth();
    }

    protected double getTextOffset(Bounds textLayoutBounds) {
        return -textLayoutBounds.getWidth() / 2;
    }

    public void doUpdateView() {
        for (GuideArrow guideArrow : mGuideArrows.values()) {
            updateGuideArrow(guideArrow);
        }

        ObservableList<Node> children = mRulerContentsArea.getChildren();
        children.clear();

        double height = getHeight();
        double width = getWidth();

        children.add(new Line(0, 0, 0, height)); // Left
        children.add(new Line(0, 0, width, 0)); // Top
        children.add(new Line(width, 0, width, height)); // Right
        children.add(new Line(0, height - 1, width, height - 1)); // Bottom
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

    public void updateView() {
        if (getHeight() == 0.0 || getWidth() == 0.0) {
            return;
        }
        doUpdateView();
    }

    public void setTransform(double scale, double translation) {
        mScale = scale;
        mTranslation = translation;
        updateView();
    }

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

    protected void configureGuideArrow(GuideArrow guideArrow) {
        var dragControl = new Object() {
            boolean Hidden;
        };
        guideArrow.setViewOrder(Constants.VIEW_ORDER_RULER_GUIDE_ARROW);
        GuideLine guideLine = guideArrow.getGuideLine();

        guideArrow.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }
            getScene().setCursor(Cursor.MOVE);
        });
        guideArrow.setOnMouseReleased(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (dragControl.Hidden) {
                deleteGuideLine(guideLine);
            }
            getScene().setCursor(Cursor.DEFAULT);
        });
        guideArrow.setOnMouseDragged(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                return;
            }
            Point2D localPoint = sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            if (Math.abs(localPoint.getY()) > GUIDE_ARROW_DRAG_SNAP_TO_ORIGINAL_POS_THRESHOLD) {
                guideArrow.setVisible(false);
                dragControl.Hidden = true;
            } else {
                setGuideLinePosition(guideLine, localPoint.getX());
                if (dragControl.Hidden) {
                    guideArrow.setVisible(true);
                }
                dragControl.Hidden = false;
            }
        });
        guideArrow.setOnMouseEntered(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                getScene().setCursor(Cursor.HAND);
            }
        });
        guideArrow.setOnMouseExited(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
        guideArrow.setOnMouseClicked(mouseEvent -> {
            if (!mouseEvent.isStillSincePress()) {
                return;
            }
            mUIController.setSelectedObjectId(guideLine.getId());
            mouseEvent.consume();
        });
    }

    protected double updateGuideArrow(GuideArrow guideArrow) {
        GuideLine guideLine = guideArrow.getGuideLine();
        double pos = lengthToPosition(guideLine.getPosition());
        guideArrow.setTranslateX(pos);

        if (mUIController.selectedObjectIds().contains(guideLine.getId())) {
            guideArrow.setFill(Abstract2DRepresentation.SELECTED_OBJECTS_COLOR);
        } else {
            guideArrow.setFill(Color.BLACK);
        }

        guideArrow.setOpacity(guideLine.isHidden() ? GUIDE_ARROW_INVISIBLE_OPACITY : GUIDE_ARROW_VISIBLE_OPACITY);
        return pos;
    }

    public void addGuideLine(GuideLine guideLine) {
        GuideArrow guideArrow = new GuideArrow(guideLine);
        mGuideArrows.put(guideLine.getId(), guideArrow);
        mGuideArrowsArea.getChildren().add(guideArrow);
        configureGuideArrow(guideArrow);
    }

    public void removeGuideLine(GuideLine guideLine) {
        GuideArrow guideArrow = mGuideArrows.get(guideLine.getId());
        mGuideArrowsArea.getChildren().remove(guideArrow);
    }

    public double updateGuideLine(GuideLine guideLine) {
        GuideArrow guideArrow = mGuideArrows.get(guideLine.getId());
        return updateGuideArrow(guideArrow);
    }

    public void clearGuideLines() {
        mGuideArrows.clear();
        mGuideArrowsArea.getChildren().clear();
    }

    public void createGuideLine(double position) {
        mUIController.createGuideLine(mDirection == GuideLineDirection.Horizontal
                ? GuideLineDirection.Vertical
                : GuideLineDirection.Horizontal, positionToLength(position));
    }

    public void deleteGuideLine(GuideLine guideLine) {
        mUIController.deleteGuideLine(guideLine);
    }

    protected void setGuideLinePosition(GuideLine guideLine, double position) {
        mUIController.setGuideLinePosition(guideLine, positionToLength(position));
    }

    protected Length positionToLength(double position) {
        return CoordinateUtils.coordsToLength((position - mTranslation) / mScale, null);
    }

    protected double lengthToPosition(Length position) {
        return CoordinateUtils.lengthToCoords(position, null) * mScale + mTranslation;
    }

    protected List<ScaleLabel> calculateScaleLabels() {
        double visibleRange = getVisibleRange();

        // Length of our visible range
        double rangeStartPos = -mTranslation / mScale;
        Length rangeStart = CoordinateUtils.coordsToLength(rangeStartPos, null);

        Length scaleDelta = Length.ofMM(1);
        double minSizePerLabel = 50 / mScale;
        while (CoordinateUtils.lengthToCoords(scaleDelta, null) < minSizePerLabel) {
            scaleDelta = scaleDelta.times(10);
        }
        LengthUnit lengthUnit = scaleDelta.getBestUnitForDisplay();
        double scaleDeltaDiff = CoordinateUtils.lengthToCoords(scaleDelta, null) * mScale;

        List<ScaleLabel> result = new ArrayList<>();
        Length p = scaleDelta.times((int) rangeStart.divideBy(scaleDelta) - 1);
        while (true) {
            // Main label
            String label = p.toHumanReadableString(lengthUnit, true);
            Text text = new Text(label);
            double currentPos = (CoordinateUtils.lengthToCoords(p, null) - rangeStartPos) * mScale;
            double textStart = currentPos + getTextOffset(text.getLayoutBounds());
            if (textStart > visibleRange) {
                break;
            }
            result.add(new ScaleLabel(currentPos, SCALE_LENGTH, Optional.of(text), textStart));
            boolean printMiddle = scaleDeltaDiff > 70;
            boolean printMiddleLabel = scaleDeltaDiff > 150;
            // 10th scale line
            if (scaleDeltaDiff > 100) {
                for (int i = 1; i < 10; i++) {
                    if (printMiddle && i == 5) {
                        continue;
                    }
                    result.add(new ScaleLabel(currentPos + i * scaleDeltaDiff/10, TENTH_SCALE_LENGTH, Optional.empty(), 0));
                }
            }
            if (printMiddle) {
                double middlePos = currentPos + scaleDeltaDiff/2;
                if (printMiddleLabel) {
                    String label2 = p.plus(scaleDelta.times(0.5)).toHumanReadableString(lengthUnit, true);
                    Text middleLabel = new Text(label2);
                    double middleTextStart = middlePos + getTextOffset(middleLabel.getLayoutBounds());
                    result.add(new ScaleLabel(middlePos, MIDDLE_SCALE_LENGTH, Optional.of(middleLabel), middleTextStart));
                }
                result.add(new ScaleLabel(middlePos, MIDDLE_SCALE_LENGTH, Optional.empty(), 0));
            }

            p = p.plus(scaleDelta);
        }
        return result;
    }
}
