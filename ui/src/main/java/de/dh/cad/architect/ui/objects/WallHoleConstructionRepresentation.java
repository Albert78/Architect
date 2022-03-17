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
package de.dh.cad.architect.ui.objects;

import java.util.Optional;

import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.objects.WallHole;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.utils.fx.MouseHandlerContext;
import de.dh.utils.fx.Vector2D;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class WallHoleConstructionRepresentation extends Abstract2DRepresentation implements IModificationFeatureProvider {
    protected class MoveBarNodes {
        protected final Rectangle mMoveBar;
        protected final Node mDoubleArrow;
        public MoveBarNodes(Rectangle moveBar, Node doubleArrow) {
            mMoveBar = moveBar;
            mDoubleArrow = doubleArrow;
        }
        public Rectangle getMoveBar() {
            return mMoveBar;
        }
        public Node getDoubleArrow() {
            return mDoubleArrow;
        }
        public void setVisible(boolean value) {
            mMoveBar.setVisible(value);
            mDoubleArrow.setVisible(value);
        }
        public void setOpacity(double value) {
            mMoveBar.setOpacity(value);
            mDoubleArrow.setOpacity(value);
        }
    }

    protected class MoveBarDragHandlers {
        protected final MoveBarNodes mMoveBarNodes;
        protected final WallDockEnd mWallHoleEnd;
        protected Point2D mLastPoint;
        protected boolean mDragging = false;
        protected boolean mMouseOver = false;

        public MoveBarDragHandlers(MoveBarNodes moveBarNodes, WallDockEnd wallHoleEnd) {
            mMoveBarNodes = moveBarNodes;
            mWallHoleEnd = wallHoleEnd;
        }

        public void onStartDrag(Point2D originalPoint) {
            mLastPoint= originalPoint;
            setDragging(true);
        }

        public void onDragging(Point2D origPoint, Point2D dragPoint, Point2D pointInScene) {
            Vector2D moveDelta = CoordinateUtils.point2DToUiVector2D(dragPoint.subtract(mLastPoint));
            dragWallHoleEnd(moveDelta, mWallHoleEnd);
            mLastPoint = dragPoint;
        }

        public void onDragEnd(Point2D origPoint, Point2D dragPoint, Point2D pointInScene) {
            setDragging(false);
        }

        public void onMouseEntered(MouseEvent event) {
            setMouseOver(true);
        }

        public void onMouseExited(MouseEvent event) {
            setMouseOver(false);
        }

        public MoveBarNodes getMoveBarNodes() {
            return mMoveBarNodes;
        }

        public Point2D getLastPoint() {
            return mLastPoint;
        }

        public void setLastPoint(Point2D value) {
            mLastPoint = value;
        }

        public boolean isDragging() {
            return mDragging;
        }

        public void setDragging(boolean value) {
            mDragging = value;
            updateOpacity();
        }

        public boolean isMouseOver() {
            return mMouseOver;
        }

        public void setMouseOver(boolean value) {
            mMouseOver = value;
            updateOpacity();
        }

        protected void updateOpacity() {
            double maxOpacity = 1;
            double minOpacity = 0;

            if (mMouseOver || mDragging) {
                mMoveBarNodes.setOpacity(maxOpacity);
            } else {
                mMoveBarNodes.setOpacity(minOpacity);
            }
        }
    }

    protected static final double MOVE_BAR_WIDTH = 10;
    protected static final double MOVE_ARROW_LENGTH = 20;
    protected static final double MOVE_ARROW_STROKE_THICKNESS = 4;

    protected static final Length MIN_WALLHOLE_WIDTH = Length.ofCM(10);

    protected final Polygon mBorder;
    protected final MoveBarNodes mMoveBarA;
    protected final MoveBarNodes mMoveBarB;
    protected final MouseHandlerContext mModificationMouseContext;
    protected boolean mModificationFeaturesEnabled = false;

    public WallHoleConstructionRepresentation(WallHole wallHole, Abstract2DView parentView) {
        super(wallHole, parentView);
        mBorder = new Polygon();
        mBorder.setStrokeType(StrokeType.INSIDE);
        addScaled(mBorder);
        setViewOrder(Constants.VIEW_ORDER_WALL_HOLE);

        var dragContext = new Object() {
            Point2D lastPoint;
        };
        // Drag handler for wall hole
        mModificationMouseContext = createDragHandler(
            op -> {
                dragContext.lastPoint = op;
            },
            (op, dp, sp) -> {
                Vector2D moveDelta = CoordinateUtils.point2DToUiVector2D(dp.subtract(dragContext.lastPoint));
                dragWallHole(moveDelta);
                dragContext.lastPoint = dp;
            },
            null, Cursor.MOVE, Cursor.MOVE); // Don't install it here

        mMoveBarA = createMoveBarNodes(WallDockEnd.A);
        mMoveBarA.setVisible(false);
        mMoveBarB = createMoveBarNodes(WallDockEnd.B);
        mMoveBarB.setVisible(false);

        ChangeListener<Boolean> propertiesUpdaterListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                updateProperties();
            }
        };

        selectedProperty().addListener(propertiesUpdaterListener);
        objectSpottedProperty().addListener(propertiesUpdaterListener);
        objectFocusedProperty().addListener(propertiesUpdaterListener);
        objectEmphasizedProperty().addListener(propertiesUpdaterListener);
    }

    protected static Rectangle buildMoveBarShape() {
        Rectangle result = new Rectangle();

        result.setStrokeType(StrokeType.CENTERED);
        result.setStroke(Color.BLACK);
        result.setFill(Color.ORANGE);

        return result;
    }

    protected static Group buildDoubleArrowShape() {
        Line centerLine = new Line(-MOVE_ARROW_LENGTH + MOVE_ARROW_STROKE_THICKNESS, 0, MOVE_ARROW_LENGTH - MOVE_ARROW_STROKE_THICKNESS, 0);
        centerLine.setStroke(Color.BLACK);
        centerLine.setStrokeWidth(MOVE_ARROW_STROKE_THICKNESS);

        Polyline arrowheadL = new Polyline(
            -2*MOVE_ARROW_LENGTH / 3,
            -MOVE_ARROW_LENGTH / 3,
            -MOVE_ARROW_LENGTH,
            0,
            -2*MOVE_ARROW_LENGTH / 3,
            MOVE_ARROW_LENGTH / 3
            );
        arrowheadL.setStroke(Color.BLACK);
        arrowheadL.setStrokeWidth(MOVE_ARROW_STROKE_THICKNESS);

        Polyline arrowheadR = new Polyline(
            2*MOVE_ARROW_LENGTH / 3,
            MOVE_ARROW_LENGTH / 3,
            MOVE_ARROW_LENGTH,
            0,
            2*MOVE_ARROW_LENGTH / 3,
            -MOVE_ARROW_LENGTH / 3
            );
        arrowheadR.setStroke(Color.BLACK);
        arrowheadR.setStrokeWidth(MOVE_ARROW_STROKE_THICKNESS);

        return new Group(centerLine, arrowheadL, arrowheadR);
    }

    protected MoveBarNodes createMoveBarNodes(WallDockEnd wallHoleEnd) {
        Node doubleArrow = buildDoubleArrowShape();
        doubleArrow.setMouseTransparent(true);
        addScaled(doubleArrow);
        Rectangle moveBar = buildMoveBarShape();
        addScaled(moveBar);
        MoveBarNodes result = new MoveBarNodes(moveBar, doubleArrow);
        MoveBarDragHandlers dragHandlers = new MoveBarDragHandlers(result, wallHoleEnd);
        createDragHandler(
            dragHandlers::onStartDrag,
            dragHandlers::onDragging,
            dragHandlers::onDragEnd,
            Cursor.MOVE, Cursor.MOVE).install(moveBar);
        moveBar.addEventHandler(MouseEvent.MOUSE_ENTERED, dragHandlers::onMouseEntered);
        moveBar.addEventHandler(MouseEvent.MOUSE_EXITED, dragHandlers::onMouseExited);
        dragHandlers.updateOpacity();
        return result;
    }

    public void dragWallHole(Vector2D moveDelta) {
        WallHole wallHole = getWallHole();
        Wall wall = wallHole.getWall();

        Vector2D cornerA1 = CoordinateUtils.positionToVector2D(wall.getAnchorWallCornerLA1().requirePosition3D());
        Vector2D cornerB1 = CoordinateUtils.positionToVector2D(wall.getAnchorWallCornerLB1().requirePosition3D());
        Vector2D wallDirectionU = cornerB1.minus(cornerA1).toUnitVector();
        double moveWidth = moveDelta.dotProduct(wallDirectionU);
        Length moveWidthL = CoordinateUtils.coordsToLength(moveWidth);

        Length distanceFromWallEnd = wallHole.getDistanceFromWallEnd();
        Length newDistanceFromWallEnd = wallHole.getDockEnd() == WallDockEnd.A
                        ? distanceFromWallEnd.plus(moveWidthL)
                        : distanceFromWallEnd.minus(moveWidthL);
        wallHole.setDistanceFromWallEnd(newDistanceFromWallEnd);
        UiController uiController = mParentView.getUiController();
        WallHoleReconciler.updateWallHoleAnchors(wallHole, uiController);
    }

    protected void dragWallHoleEnd(Vector2D moveDelta, WallDockEnd dragWallEnd) {
        WallHole wallHole = getWallHole();
        Wall wall = wallHole.getWall();

        Vector2D wallHandleA = CoordinateUtils.positionToVector2D(wall.getAnchorWallHandleA().requirePosition2D());
        Vector2D wallHandleB = CoordinateUtils.positionToVector2D(wall.getAnchorWallHandleB().requirePosition2D());
        Vector2D wallDirectionU = wallHandleB.minus(wallHandleA).toUnitVector();
        double moveWidth = moveDelta.dotProduct(wallDirectionU);
        Length moveWidthL = CoordinateUtils.coordsToLength(moveWidth);
        WallDockEnd dockEnd = wallHole.getDockEnd();
        Dimensions2D dimensions = wallHole.getDimensions();

        Length deltaWidth;
        Length deltaDistance;
        if (dockEnd == WallDockEnd.A) {
            if (dragWallEnd.equals(WallDockEnd.A)) {
                deltaDistance = moveWidthL;
                deltaWidth = moveWidthL.negated();
            } else {
                deltaDistance = Length.ZERO;
                deltaWidth = moveWidthL;
            }
        } else { // Dock end B
            if (dragWallEnd.equals(WallDockEnd.B)) {
                deltaDistance = moveWidthL.negated();
                deltaWidth = moveWidthL;
            } else {
                deltaDistance = Length.ZERO;
                deltaWidth = moveWidthL.negated();
            }
        }
        Length newWidth = dimensions.getX().plus(deltaWidth);
        if (newWidth.lt(MIN_WALLHOLE_WIDTH)) {
            deltaDistance = deltaDistance.minus(MIN_WALLHOLE_WIDTH.minus(newWidth).times(Math.signum(deltaDistance.inMM())));
            newWidth = MIN_WALLHOLE_WIDTH;
        }

        wallHole.setDistanceFromWallEnd(wallHole.getDistanceFromWallEnd().plus(deltaDistance));
        wallHole.setDimensions(dimensions.withX(newWidth));
        UiController uiController = mParentView.getUiController();
        WallHoleReconciler.updateWallHoleAnchors(wallHole, uiController);
    }

    public WallHole getWallHole() {
        return (WallHole) mModelObject;
    }

    @Override
    public ConstructionView getParentView() {
        return (ConstructionView) mParentView;
    }

    protected void updateProperties() {
        mBorder.setFill(Color.WHITE);
        if (isSelected()) {
            mBorder.setStroke(SELECTED_OBJECTS_COLOR);
        } else {
            mBorder.setStroke(Color.BLACK);
        }
        if (isObjectFocused()) {
            setViewOrder(Constants.VIEW_ORDER_WALL_HOLE + Constants.VIEW_ORDER_OFFSET_FOCUSED);
        } else {
            setViewOrder(Constants.VIEW_ORDER_WALL_HOLE + Constants.VIEW_ORDER_OFFSET_NORMAL);
        }
        configureMainBorderDefault(mBorder);
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        updateShape();
    }

    protected void updateShape() {
        WallHole wallHole = getWallHole();
        Wall wall = wallHole.getWall();

        Vector2D handleA = CoordinateUtils.positionToVector2D(wall.getAnchorWallHandleA().getPosition());
        Vector2D handleB = CoordinateUtils.positionToVector2D(wall.getAnchorWallHandleB().getPosition());

        Length wallSideLengthL = wall.calculateBaseLength();

        double distanceFromWallEndA = CoordinateUtils.lengthToCoords(wallHole.getDistanceFromWallEndA(wallSideLengthL));
        Dimensions2D holeDimensions = wallHole.getDimensions();
        double holeWidthC = CoordinateUtils.lengthToCoords(holeDimensions.getX());
        double thicknessC = CoordinateUtils.lengthToCoords(wall.getThickness());
        Vector2D vLongSideU = handleB.minus(handleA).toUnitVector();
        Vector2D vShortSide = vLongSideU.getNormalCCW().scaleToLength(thicknessC);
        Vector2D vShortSide2 = vShortSide.times(0.5);
        Vector2D vHoleEndA = vLongSideU.times(distanceFromWallEndA);
        Vector2D vHoleEndB = vHoleEndA.plus(vLongSideU.times(holeWidthC));

        Vector2D pA1 = handleA.plus(vHoleEndA.minus(vShortSide2));
        Vector2D pA2 = pA1.plus(vShortSide);
        Vector2D pB1 = handleA.plus(vHoleEndB.minus(vShortSide2));
        Vector2D pB2 = pB1.plus(vShortSide);

        mBorder.getPoints().setAll(
            pA1.getX(),
            pA1.getY(),
            pA2.getX(),
            pA2.getY(),
            pB2.getX(),
            pB2.getY(),
            pB1.getX(),
            pB1.getY(),
            pA1.getX(),
            pA1.getY());

        positionMoveBar(mMoveBarA, pA1, pA2);
        positionMoveBar(mMoveBarB, pB1, pB2);
    }

    protected void positionMoveBar(MoveBarNodes moveBarNodes, Vector2D p1, Vector2D p2) {
        Vector2D v12 = p2.minus(p1);
        Vector2D middle = p1.plus(p2).times(0.5);
        double angle = Vector2D.angleBetween(Vector2D.X_ONE, v12);
        double scaleCompensation = getScaleCompensation();

        Rectangle moveBar = moveBarNodes.getMoveBar();
        double barLength = v12.getLength() * 0.8;
        moveBar.setX(-barLength / 2);
        moveBar.setWidth(barLength);
        double barWidth = MOVE_BAR_WIDTH * scaleCompensation;
        moveBar.setY(-barWidth / 2);
        moveBar.setHeight(barWidth);
        moveBar.setStrokeWidth(2 * scaleCompensation);
        moveBar.getTransforms().setAll(new Translate(middle.getX(), middle.getY()), new Rotate(angle));

        Node doubleArrow = moveBarNodes.getDoubleArrow();
        doubleArrow.getTransforms().setAll(new Translate(middle.getX(), middle.getY()), new Rotate(angle + 90), new Scale(scaleCompensation, scaleCompensation));
    }

    @Override
    public void enableModificationFeatures() {
        if (mModificationFeaturesEnabled) {
            return;
        }
        mModificationFeaturesEnabled = true;

        mModificationMouseContext.install(this);
        mMoveBarA.setVisible(true); // Visible but opacity = 0 until mouse is over
        mMoveBarB.setVisible(true);
    }

    @Override
    public void disableModificationFeatures() {
        if (!mModificationFeaturesEnabled) {
            return;
        }
        mModificationFeaturesEnabled = false;

        mModificationMouseContext.uninstall(this);
        mMoveBarA.setVisible(false);
        mMoveBarB.setVisible(false);
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateShape();
        updateProperties();
    }

    @Override
    protected Optional<Shape> getShapeForIntersectionCheck() {
        return Optional.of(mBorder);
    }
}
