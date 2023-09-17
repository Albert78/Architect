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
package de.dh.cad.architect.ui.objects;

import java.text.MessageFormat;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.utils.Axis;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.DragControl;
import de.dh.utils.Vector2D;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

public class DimensioningVisual {
    protected static final double EPSILON = 0.01;

    protected final Abstract2DUiObject mParent;

    protected final Line mBorderLine1;
    protected final Line mBorderLine2;
    protected final Line mAcrossLine1;
    protected final Line mAcrossLine2;
    protected final Line mAngularLine1;
    protected final Line mAngularLine2;
    protected final Text mText;
    protected final Scale mTextScaleCorrection;
    protected Vector2D mP1;
    protected Vector2D mP2;
    protected Point2D mDP1;

    protected BooleanProperty mMouseOverProperty = new SimpleBooleanProperty(false);
    protected DoubleProperty mLabelDistanceProperty = new SimpleDoubleProperty(50);
    protected DoubleProperty mStrokeWidthProperty = new SimpleDoubleProperty(1d);

    public DimensioningVisual(Abstract2DUiObject parent) {
        mParent = parent;

        mBorderLine1 = new Line();
        mBorderLine2 = new Line();
        mAcrossLine1 = new Line();
        mAcrossLine2 = new Line();
        mAngularLine1 = new Line();
        mAngularLine2 = new Line();
        mText = new Text();
        setBorderLineDefaultStrokeDash();
        mText.setTextOrigin(VPos.CENTER);
        mParent.addScaled(mBorderLine1);
        mParent.addScaled(mBorderLine2);
        mParent.addScaled(mAcrossLine1);
        mParent.addScaled(mAcrossLine2);
        mParent.addScaled(mAngularLine1);
        mParent.addScaled(mAngularLine2);
        mTextScaleCorrection = mParent.addUnscaled(mText);
    }

    public void enableDrag() {
        var dragControl = new DragControl() {
            double initialLabelDistance;
            Vector2D vDistance;
        };

        Pane transformedRoot = mParent.getParentView().getTransformedRoot();

        mText.setOnMousePressed(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                return;
            }
            Point2D localPoint = transformedRoot.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            // record a delta distance for the drag and drop operation.
            dragControl.setPoint(localPoint);
            dragControl.vDistance = mP2.minus(mP1).toUnitVector().getNormalCW();
            dragControl.initialLabelDistance = getLabelDistance();

            mParent.getScene().setCursor(Cursor.MOVE);
        });
        mText.setOnMouseReleased(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }
            mParent.getScene().setCursor(Cursor.HAND);
        });
        mText.setOnMouseDragged(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                return;
            }
            Point2D localPoint = transformedRoot.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            Point2D delta = localPoint.subtract(dragControl.getPoint());

            double dist = -dragControl.vDistance.dotProduct(new Vector2D(delta.getX(), delta.getY()));
            setLabelDistance(dragControl.initialLabelDistance + dist);
        });
        mText.setOnMouseEntered(mouseEvent -> {
            mMouseOverProperty.set(true);
            mParent.getScene().setCursor(Cursor.HAND);
        });
        mText.setOnMouseExited(mouseEvent -> {
            mMouseOverProperty.set(false);
            mParent.getScene().setCursor(Cursor.DEFAULT);
        });
    }

    public void disableDrag() {
        mText.setOnMousePressed(null);
        mText.setOnMouseReleased(null);
        mText.setOnMouseDragged(null);
        mText.setOnMouseEntered(mouseEvent -> {
            mMouseOverProperty.set(true);
        });
        mText.setOnMouseExited(mouseEvent -> {
            mMouseOverProperty.set(false);
        });
    }

    protected void setArtifactsColor(Color color) {
        mBorderLine1.setStroke(color);
        mBorderLine2.setStroke(color);
        mAcrossLine1.setStroke(color);
        mAcrossLine2.setStroke(color);
        mAngularLine1.setStroke(color);
        mAngularLine2.setStroke(color);
    }

    public void setProperties(Color color, boolean isHighlight, boolean isEmphasis) {
        setArtifactsColor(color.deriveColor(1, 1, 1, 0.5));
        mText.setFill(color);
        int strokeWidth = 1;
        if (isHighlight) {
            mText.setFont(Font.font(null, FontWeight.BOLD, -1));
            strokeWidth = 2;
        } else {
            mText.setFont(null);
        }
        setStrokeWidth(strokeWidth);

        if (isEmphasis) {
            configureEmphasisStrokeDash(mBorderLine1, isEmphasis);
            configureEmphasisStrokeDash(mBorderLine2, isEmphasis);
        } else {
            setBorderLineDefaultStrokeDash();
        }
        configureEmphasisStrokeDash(mAcrossLine1, isEmphasis);
        configureEmphasisStrokeDash(mAcrossLine2, isEmphasis);
        configureEmphasisStrokeDash(mAngularLine1, isEmphasis);
        configureEmphasisStrokeDash(mAngularLine2, isEmphasis);
    }

    protected void configureEmphasisStrokeDash(Line line, boolean isEmphasis) {
        if (isEmphasis) {
            line.getStrokeDashArray().setAll(6d, 8d);
        } else {
            line.getStrokeDashArray().clear();
        }
    }

    protected void setBorderLineDefaultStrokeDash() {
        mBorderLine1.getStrokeDashArray().setAll(20d, 5d, 7d, 5d);
        mBorderLine2.getStrokeDashArray().setAll(20d, 5d, 7d, 5d);
    }

    protected void updateShape(Position2D position1, Position2D position2, Optional<String> oLabel, double scaleCompensation) {
        double labelDistance = -getLabelDistance();

        // Points
        double x1 = CoordinateUtils.lengthToCoords(position1.getX(), Axis.X);
        double y1 = CoordinateUtils.lengthToCoords(position1.getY(), Axis.Y);
        double x2 = CoordinateUtils.lengthToCoords(position2.getX(), Axis.X);
        double y2 = CoordinateUtils.lengthToCoords(position2.getY(), Axis.Y);

        mP1 = new Vector2D(x1, y1);
        mP2 = new Vector2D(x2, y2);

        // Vector pos1 -> pos2
        Vector2D v = mP2.minus(mP1);

        double distance = v.getLength();
        Vector2D vu = v.toUnitVector();
        Vector2D nu = vu.getNormalCW();
        Vector2D vu45 = vu.rotate(-45);

        // Distant points
        Vector2D vDistance = nu.times(labelDistance);
        double signumVD = Math.signum(labelDistance);
        Vector2D vOverhang = nu.times(labelDistance + 10 * scaleCompensation * signumVD);
        Vector2D vu45Scaled = vu45.times(7 * scaleCompensation);
        Vector2D vAngular45p = vDistance.plus(vu45Scaled);
        Vector2D vAngular45m = vDistance.minus(vu45Scaled);

        Vector2D dp1 = mP1.plus(vDistance);
        mDP1 = new Point2D(dp1.getX(), dp1.getY());
        Vector2D dp1Overhang = mP1.plus(vOverhang);
        Vector2D dp1AngularP = mP1.plus(vAngular45p);
        Vector2D dp1AngularM = mP1.plus(vAngular45m);
        Vector2D dp2 = mP2.plus(vDistance);
        Vector2D dp2Overhang = mP2.plus(vOverhang);
        Vector2D dp2AngularP = mP2.plus(vAngular45p);
        Vector2D dp2AngularM = mP2.plus(vAngular45m);

        mBorderLine1.setStartX(dp1Overhang.getX());
        mBorderLine1.setStartY(dp1Overhang.getY());
        mBorderLine1.setEndX(x1);
        mBorderLine1.setEndY(y1);
        DoubleBinding strokeWidth = mStrokeWidthProperty.multiply(scaleCompensation);
        mBorderLine1.strokeWidthProperty().bind(strokeWidth);

        mBorderLine2.setStartX(dp2Overhang.getX());
        mBorderLine2.setStartY(dp2Overhang.getY());
        mBorderLine2.setEndX(x2);
        mBorderLine2.setEndY(y2);
        mBorderLine2.strokeWidthProperty().bind(strokeWidth);

        Length length = CoordinateUtils.coordsToLength(distance, null);
        String lengthStr = length.toNormalPlanString();
        if (oLabel.isPresent()) {
            mText.setText(MessageFormat.format(oLabel.get(), lengthStr));
        } else {
            mText.setText(lengthStr);
        }
        double textWidth = mText.getLayoutBounds().getWidth(); // Text is unscaled!
        double acrossLineLength = (distance - (textWidth + 20) * scaleCompensation) / 2;
        Vector2D mp = dp1.plus(v.times(0.5));
        mText.setX(mp.getX() - textWidth / 2);
        mText.setY(mp.getY());
        mTextScaleCorrection.setPivotX(mp.getX());
        mTextScaleCorrection.setPivotY(mp.getY());
        double rotation = Math.signum(vu.getY()) * Math.acos(vu.getX()) * 180 / Math.PI;
        if (rotation < -90 + EPSILON || rotation > 90 + EPSILON) { // Use slightly bigger comparison angles to make text always flip to the same side with angles near 90 degrees
            rotation += 180;
        }
        mText.setRotate(rotation);

        mAcrossLine1.setStartX(dp1.getX());
        mAcrossLine1.setStartY(dp1.getY());
        Vector2D al1e = dp1.plus(vu.times(acrossLineLength));
        mAcrossLine1.setEndX(al1e.getX());
        mAcrossLine1.setEndY(al1e.getY());
        mAcrossLine1.strokeWidthProperty().bind(strokeWidth);

        Vector2D al2s = dp2.minus(vu.times(acrossLineLength));
        mAcrossLine2.setStartX(al2s.getX());
        mAcrossLine2.setStartY(al2s.getY());
        mAcrossLine2.setEndX(dp2.getX());
        mAcrossLine2.setEndY(dp2.getY());
        mAcrossLine2.strokeWidthProperty().bind(strokeWidth);

        mAngularLine1.setStartX(dp1AngularP.getX());
        mAngularLine1.setStartY(dp1AngularP.getY());
        mAngularLine1.setEndX(dp1AngularM.getX());
        mAngularLine1.setEndY(dp1AngularM.getY());
        mAngularLine1.strokeWidthProperty().bind(strokeWidth);

        mAngularLine2.setStartX(dp2AngularP.getX());
        mAngularLine2.setStartY(dp2AngularP.getY());
        mAngularLine2.setEndX(dp2AngularM.getX());
        mAngularLine2.setEndY(dp2AngularM.getY());
        mAngularLine2.strokeWidthProperty().bind(strokeWidth);
    }

    public Text getText() {
        return mText;
    }

    public DoubleProperty labelDistanceProperty() {
        return mLabelDistanceProperty;
    }

    public double getLabelDistance() {
        return mLabelDistanceProperty.get();
    }

    public void setLabelDistance(double value) {
        if (mLabelDistanceProperty.get() == value) {
            return;
        }
        mLabelDistanceProperty.set(value);
    }

    public DoubleProperty strokeWidthProperty() {
        return mStrokeWidthProperty;
    }

    public double getStrokeWidth() {
        return mStrokeWidthProperty.get();
    }

    public void setStrokeWidth(double value) {
        if (mStrokeWidthProperty.get() == value) {
            return;
        }
        mStrokeWidthProperty.set(value);
    }

    public BooleanProperty mouseOverProperty() {
        return mMouseOverProperty;
    }

    public boolean isMouseOver() {
        return mMouseOverProperty.get();
    }
}
