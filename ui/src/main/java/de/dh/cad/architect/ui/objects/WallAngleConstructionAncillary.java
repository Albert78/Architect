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

import de.dh.cad.architect.model.coords.Angle;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

public class WallAngleConstructionAncillary extends Abstract2DAncillaryObject {
    protected static class ArcAngles {
        protected final double mStartAngle;
        protected final double mEndAngle;
        protected final double mDelta;

        public ArcAngles(double startAngle, double endAngle) {
            mStartAngle = startAngle;
            mEndAngle  = endAngle;
            double angle = endAngle - startAngle;
            while (angle < 0) {
                angle += 360;
            }
            mDelta = angle;
        }

        protected static double normalizeAngle(double angle) {
            double result = angle;
            while (result < 0) {
                result += 360;
            }
            while (result >= 360) {
                result -= 360;
            }
            return result;
        }

        public static ArcAngles normalized(double startAngle, double endAngle) {
            startAngle = normalizeAngle(startAngle);
            endAngle = normalizeAngle(endAngle);
            double angle = endAngle - startAngle;
            while (angle < 0) {
                angle += 360;
            }
            if (angle > 180.9 || // Always take the smaller angle, i.e. <= 180, take rounding into account
                            (angle > 179 && angle < 181 && startAngle > endAngle)) { // Always flip to the same side in the borderline
                double temp = startAngle;
                startAngle = endAngle;
                endAngle = temp;
            }
            return new ArcAngles(startAngle, endAngle);
        }

        public double getStartAngle() {
            return mStartAngle;
        }

        public double getEndAngle() {
            return mEndAngle;
        }

        public double getDelta() {
            return mDelta;
        }
    }

    protected static final double ARC_RADIUS = 80;
    protected static final double LINE_LENGTH2 = 20;
    protected static final double ANGULAR_LINE_LENGTH2 = 3;

    protected final Arc mArc = new Arc();
    protected final Scale mArcScale;
    protected final Line mLineStart = new Line();
    protected final Scale mLineStartScale;
    protected final Rotate mLineStartRotate = new Rotate();
    protected final Line mAngularLineStart = new Line();
    protected final Scale mAngularLineStartScale;
    protected final Rotate mAngularLineStartRotate = new Rotate();
    protected final Line mLineEnd = new Line();
    protected final Scale mLineEndScale;
    protected final Rotate mLineEndRotate = new Rotate();
    protected final Line mAngularLineEnd = new Line();
    protected final Scale mAngularLineEndScale;
    protected final Rotate mAngularLineEndRotate = new Rotate();

    protected final Text mText = new Text();
    protected final Scale mTextScale;
    protected final Rotate mTextRotate = new Rotate();

    public WallAngleConstructionAncillary(ConstructionView parentView) {
        super(parentView);
        Color linesStrokeColor = Color.BLACK.deriveColor(1, 1, 1, 0.5);
        mArc.setStroke(linesStrokeColor);
        mArc.setFill(null);
        mArc.setType(ArcType.OPEN);
        mArc.setStrokeLineCap(StrokeLineCap.BUTT);

        mLineStart.setStroke(linesStrokeColor);
        mLineStart.getStrokeDashArray().setAll(5d, 10d);
        mLineStart.getTransforms().setAll(mLineStartRotate);
        mLineStartScale = addUnscaled(mLineStart);

        mAngularLineStart.setStroke(linesStrokeColor);
        mAngularLineStart.getTransforms().setAll(mAngularLineStartRotate);
        mAngularLineStartScale = addUnscaled(mAngularLineStart);

        mLineEnd.setStroke(linesStrokeColor);
        mLineEnd.getStrokeDashArray().setAll(5d, 10d);
        mLineEnd.getTransforms().setAll(mLineEndRotate);
        mLineEndScale = addUnscaled(mLineEnd);

        mAngularLineEnd.setStroke(linesStrokeColor);
        mAngularLineEnd.getTransforms().setAll(mAngularLineEndRotate);
        mAngularLineEndScale = addUnscaled(mAngularLineEnd);

        mText.setTextOrigin(VPos.BOTTOM);
        mText.getTransforms().setAll(mTextRotate);

        mArcScale = addUnscaled(mArc);
        mTextScale = addUnscaled(mText);
        setMouseTransparent(true);

        setViewOrder(Constants.VIEW_ORDER_ANCILLARY + Constants.VIEW_ORDER_OFFSET_FOCUSED - 1); // More in foreground than normal ancillary objects and more than focused objects
    }

    public void update(Position2D centerPosition, Angle startAngle, Angle endAngle, boolean turnToSmallerSide) {
        Point2D centerPoint = CoordinateUtils.positionToPoint2D(centerPosition);
        double centerX = centerPoint.getX();
        double centerY = centerPoint.getY();
        mArcScale.setPivotX(centerX);
        mArcScale.setPivotY(centerY);
        mTextScale.setPivotX(centerX);
        mTextScale.setPivotY(centerY);
        mArc.setCenterX(centerX);
        mArc.setCenterY(centerY);

        double startAngleDeg = startAngle.getAngleDeg();
        double endAngleDeg = endAngle.getAngleDeg();
        ArcAngles angles = turnToSmallerSide ? ArcAngles.normalized(startAngleDeg, endAngleDeg) : new ArcAngles(startAngleDeg, endAngleDeg);
        double angle = angles.getDelta();
        double normalizedStartAngle = angles.getStartAngle();
        double normalizedEndAngle = angles.getEndAngle();
        mArc.setStartAngle(360 - normalizedStartAngle);
        mArc.setLength(-angle);
        mArc.setRadiusX(ARC_RADIUS);
        mArc.setRadiusY(ARC_RADIUS);

        configureEndPoint(mLineStart, mLineStartScale, mLineStartRotate, mAngularLineStart, mAngularLineStartScale, mAngularLineStartRotate, normalizedStartAngle, centerPoint);
        configureEndPoint(mLineEnd, mLineEndScale, mLineEndRotate, mAngularLineEnd, mAngularLineEndScale, mAngularLineEndRotate, normalizedEndAngle, centerPoint);

        mText.setText(String.valueOf(Math.round(Math.abs(angle))) + "°");
        double textWidth = mText.getLayoutBounds().getWidth();
        double textX = centerX - textWidth / 2;
        double textY;

        double rotateAngle = (normalizedStartAngle + angle / 2 + 90 + 360) % 360;
        if (rotateAngle > 91 && rotateAngle < 269) {
            textY = centerY + ARC_RADIUS + 3;
            mText.setTextOrigin(VPos.TOP);
            rotateAngle += 180;
        } else {
            mText.setTextOrigin(VPos.BOTTOM);
            textY = centerY - ARC_RADIUS - 3;
        }
        mText.setX(textX);
        mText.setY(textY);
        mTextRotate.setAngle(rotateAngle);
        mTextRotate.setPivotX(centerX);
        mTextRotate.setPivotY(centerY);
    }

    protected void configureEndPoint(Line line, Scale lineScale, Rotate lineRotate, Line angularLine, Scale angularLineScale, Rotate angularLineRotate, double angle, Point2D centerPoint) {
        lineScale.setPivotX(centerPoint.getX());
        lineScale.setPivotY(centerPoint.getY());
        line.setStartX(centerPoint.getX() + ARC_RADIUS - LINE_LENGTH2);
        line.setStartY(centerPoint.getY());
        line.setEndX(centerPoint.getX() + ARC_RADIUS + LINE_LENGTH2);
        line.setEndY(centerPoint.getY());
        lineRotate.setPivotX(centerPoint.getX());
        lineRotate.setPivotY(centerPoint.getY());
        lineRotate.setAngle(angle);

        angularLineScale.setPivotX(centerPoint.getX());
        angularLineScale.setPivotY(centerPoint.getY());
        angularLine.setStartX(centerPoint.getX() + ARC_RADIUS - ANGULAR_LINE_LENGTH2);
        angularLine.setStartY(centerPoint.getY() - ANGULAR_LINE_LENGTH2);
        angularLine.setEndX(centerPoint.getX() + ARC_RADIUS + ANGULAR_LINE_LENGTH2);
        angularLine.setEndY(centerPoint.getY() + ANGULAR_LINE_LENGTH2);
        angularLineRotate.setPivotX(centerPoint.getX());
        angularLineRotate.setPivotY(centerPoint.getY());
        angularLineRotate.setAngle(angle);
    }
}
