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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallOutline;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.IntermediatePoint.IntermediatePointCallback;
import de.dh.cad.architect.ui.objects.WallReconciler.DividedWallParts;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.wall.ChangeWallsVisualFeedbackManager;
import de.dh.utils.fx.Vector2D;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

/**
 * Ground plan representation of a wall (fragment).
 */
public class WallConstructionRepresentation extends AbstractAnchoredObjectConstructionRepresentation implements IModificationFeatureProvider {
    protected final Polygon mBorder;
    protected final Text mWallEndA;
    protected final Scale mWallEndAScale;
    protected final Text mWallEndB;
    protected final Scale mWallEndBScale;
    protected final Text mWallSide1;
    protected final Scale mWallSide1Scale;
    protected final Text mWallSide2;
    protected final Scale mWallSide2Scale;

    protected ChangeWallsVisualFeedbackManager mFeedbackManager = null;

    protected IntermediatePoint mIntermediatePoint = null;
    protected boolean mModificationFeaturesEnabled = false;
    protected boolean mInvalidState = false;

    public WallConstructionRepresentation(BaseAnchoredObject wall, ConstructionView parentView) {
        super(wall, parentView);
        mBorder = new Polygon();
        mWallEndA = new Text("A");
        mWallEndB = new Text("B");
        mWallSide1 = new Text("1");
        mWallSide2 = new Text("2");
        addScaled(mBorder);
        mWallEndAScale = addUnscaled(mWallEndA);
        mWallEndBScale = addUnscaled(mWallEndB);
        mWallSide1Scale = addUnscaled(mWallSide1);
        mWallSide2Scale = addUnscaled(mWallSide2);

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

    public Wall getWall() {
        return (Wall) mModelObject;
    }

    @Override
    public ConstructionView getParentView() {
        return (ConstructionView) mParentView;
    }

    protected void updateProperties() {
        mBorder.setFill(Color.DARKGRAY);
        boolean selected = isSelected();
        updateWallLabelsVisibility();
        if (selected) {
            mBorder.setStroke(SELECTED_OBJECTS_COLOR);
        } else {
            mBorder.setStroke(Color.BLACK.deriveColor(1, 1, 1, 0.5));
        }
        if (isObjectFocused()) {
            setViewOrder(Constants.VIEW_ORDER_WALL + Constants.VIEW_ORDER_OFFSET_FOCUSED);
        } else {
            setViewOrder(Constants.VIEW_ORDER_WALL + Constants.VIEW_ORDER_OFFSET_NORMAL);
        }
        mBorder.setStrokeType(StrokeType.INSIDE);
        mBorder.setStrokeLineJoin(StrokeLineJoin.BEVEL);
        configureMainBorderDefault(mBorder);
    }

    protected void configureForInvalidState() {
        mBorder.getPoints().clear();
        mInvalidState = true;
    }

    protected void updateWallLabelsVisibility() {
        if (mInvalidState) {
            mWallEndA.setVisible(false);
            mWallEndB.setVisible(false);
            mWallSide1.setVisible(false);
            mWallSide2.setVisible(false);
            return;
        }
        boolean focused = isObjectFocused();
        boolean spotted = isObjectSpotted();
        boolean wallLabelsVisible = focused || spotted;
        mWallEndA.setVisible(wallLabelsVisible);
        mWallEndB.setVisible(wallLabelsVisible);
        mWallSide1.setVisible(wallLabelsVisible);
        mWallSide2.setVisible(wallLabelsVisible);
    }

    protected void updateShape() {
        Wall wall = getWall();

        Optional<WallAnchorPositions> oWap = wall.extractWallAnchorPositions();
        Optional<WallOutline> oWallOutlineCW = oWap.map(wap -> wap.calculateWallOutlineCW());
        if (oWallOutlineCW.isPresent()) {
            List<Double> coords = new ArrayList<>();
            List<Position2D> groundPoints = oWallOutlineCW.get().calculateAllGroundPoints();
            mInvalidState = false;
            updateWallLabelsVisibility();
            for (Position2D pos : groundPoints) {
                coords.add(CoordinateUtils.lengthToCoords(pos.getX()));
                coords.add(CoordinateUtils.lengthToCoords(pos.getY()));
            }
            mBorder.getPoints().setAll(coords);

            double wallThickness2 = CoordinateUtils.lengthToCoords(wall.getThickness()) / 2;
            Position2D handlePosA = wall.getAnchorWallHandleA().projectionXY();
            Vector2D handleA = new Vector2D(CoordinateUtils.lengthToCoords(handlePosA.getX()), CoordinateUtils.lengthToCoords(handlePosA.getY()));
            Position2D handlePosB = wall.getAnchorWallHandleB().projectionXY();
            Vector2D handleB = new Vector2D(CoordinateUtils.lengthToCoords(handlePosB.getX()), CoordinateUtils.lengthToCoords(handlePosB.getY()));
            Vector2D middle = Vector2D.center(handleA, handleB);
            Vector2D vectorBA = handleA.minus(handleB);
            Vector2D v12_2 = vectorBA.getNormalCCW();

            positionWallEndLabel(mWallEndA, mWallEndAScale, handleA, handleB);
            positionWallEndLabel(mWallEndB, mWallEndBScale, handleB, handleA);
            positionWallSideLabel(mWallSide1, mWallSide1Scale, middle, v12_2.negated(), wallThickness2);
            positionWallSideLabel(mWallSide2, mWallSide2Scale, middle, v12_2, wallThickness2);
        } else {
            configureForInvalidState();
        }
    }

    protected void positionText(Text wallEndLabel, Scale labelScale, double textMiddleX, double textMiddleY, double textWidth) {
        // Scale compensation is not necessary for the text because text is unscaled
        wallEndLabel.setX(textMiddleX - textWidth / 2);
        wallEndLabel.setY(textMiddleY);
        labelScale.setPivotX(textMiddleX);
        labelScale.setPivotY(textMiddleY);
        wallEndLabel.setTextOrigin(VPos.CENTER);
    }

    protected void positionWallEndLabel(Text wallEndLabel, Scale labelScale, Vector2D thisSideHandle, Vector2D otherSideHandle) {
        double scaleCompensation = getParentView().getScaleCompensation();

        Bounds textBounds = wallEndLabel.getLayoutBounds();
        Vector2D u = Vector2D.between(thisSideHandle, otherSideHandle);
        double length = u.getLength();
        double tbs = textBounds.getHeight() * scaleCompensation;
        double offsetLength = Math.min(tbs * 1.5, length / 2 - tbs);
        Vector2D offsetWD = u.toUnitVector().times(offsetLength);
        double textMiddleX = thisSideHandle.getX()
            + offsetWD.getX();
        double textMiddleY = thisSideHandle.getY()
            + offsetWD.getY();
        positionText(wallEndLabel, labelScale, textMiddleX, textMiddleY, textBounds.getWidth());
    }

    protected void positionWallSideLabel(Text wallSideLabel, Scale labelScale, Vector2D middlePoint, Vector2D vSide, double wallThickness2) {
        double scaleCompensation = getParentView().getScaleCompensation();

        Bounds textBounds = wallSideLabel.getLayoutBounds();
        double dist = // Starting at middle of wall...
                        wallThickness2 // ... to the side
                        + 8 * Math.sqrt(scaleCompensation) // ... 8 pixels further but compensate border stroke thickness a bit, looks good
                        + (textBounds.getWidth() / 2) * scaleCompensation; // ... + half of text width
        vSide = vSide.scaleToLength(dist);
        double textMiddleX = middlePoint.getX() + vSide.getX();
        double textMiddleY = middlePoint.getY() + vSide.getY();
        positionText(wallSideLabel, labelScale, textMiddleX, textMiddleY, textBounds.getWidth());
    }

    protected IntermediatePoint createIntermediatePoint() {
        IntermediatePoint result = new IntermediatePoint(getParentView(), new IntermediatePointCallback() {
            @Override
            protected void detachIntermediatePoint(IntermediatePoint source) {
                mIntermediatePoint = null;
                addIntermediatePoints();
            }

            @Override
            protected Anchor createHandleAnchor(IntermediatePoint source, Anchor anchorBefore, Anchor anchorAfter, Position2D bendPosition) {
                UiController uiController = mParentView.getUiController();

                Wall thisWall = getWall();
                DividedWallParts res = WallReconciler.divideWall(thisWall, bendPosition, uiController);

                Wall wallPartEndA = res.getWallPartEndA();
                Wall wallPartEndB2 = res.getWallPartEndB();
                updateWallAnchors(wallPartEndA, wallPartEndB2);

                return thisWall.getAnchorWallHandleB();
            }

            // Hack: Update the representations of the changed anchors to match new owner situation -
            // We use internal knowledge about what must be done to switch the owner of the anchor representation
            protected void updateWallAnchors(Wall wall1, Wall wall2) {
                Collection<Anchor> changedAnchors = CollectionUtils.union(wall1.getAnchors(), wall2.getAnchors());
                for (Anchor anchor : changedAnchors) {
                    AnchorConstructionRepresentation arepr = (AnchorConstructionRepresentation) mParentView.getRepresentationByModelId(anchor.getId());
                    if (arepr != null) {
                        arepr.detachOwnerListeners();
                        arepr.updateProperties();
                    }
                }
            }
        });
        return result;
    }

    protected void addIntermediatePoints() {
        if (mIntermediatePoint == null) {
            mIntermediatePoint = createIntermediatePoint();
        }
        updateIntermediatePoints();
    }

    protected void updateIntermediatePoints() {
        Wall wall = getWall();
        boolean visible = !wall.isHidden();
        if (mIntermediatePoint != null) {
            mIntermediatePoint.update(wall.getAnchorWallHandleA(), wall.getAnchorWallHandleB());
            mIntermediatePoint.setVisible(visible);
        }
    }

    protected void removeIntermediatePoints() {
        if (mIntermediatePoint != null) {
            mIntermediatePoint.dispose();
            mIntermediatePoint = null;
        }
    }

    @Override
    public void enableModificationFeatures() {
        mModificationFeaturesEnabled = true;
        addIntermediatePoints();
    }

    @Override
    public void disableModificationFeatures() {
        mModificationFeaturesEnabled = false;
        removeIntermediatePoints();
    }

    @Override
    public boolean isAnchorDragSupported(Anchor anchor) {
        return mModificationFeaturesEnabled && Wall.isWallHandleAnchor(anchor);
    }

    @Override
    public void dragAnchor(Anchor anchor,
        Point2D startDragPoint, Point2D currentDragPoint,
        Position2D targetPosition, boolean shiftDown, boolean altDown, boolean controlDown) {
        super.dragAnchor(anchor, startDragPoint, currentDragPoint, targetPosition, shiftDown, altDown, controlDown);
        if (mFeedbackManager == null) {
            return;
        }
        mFeedbackManager.updateVisualObjects();
    }

    @Override
    public void startAnchorDrag(Anchor anchor,
        Point2D startDragPoint, Position2D startDragPosition) {
        super.startAnchorDrag(anchor, startDragPoint, startDragPosition);
        if (mFeedbackManager != null) {
            mFeedbackManager.removeVisualObjects();
        }
        mFeedbackManager = new ChangeWallsVisualFeedbackManager(getParentView());
        mFeedbackManager.initializeForDragWallHandle(anchor);
        mFeedbackManager.updateVisualObjects();
    }

    @Override
    public void endAnchorDrag(Anchor anchor) {
        super.endAnchorDrag(anchor);
        if (mFeedbackManager == null) {
            return;
        }
        mFeedbackManager.removeVisualObjects();
        mFeedbackManager = null;
    }

    @Override
    public void dragAnchorDock(Anchor anchor,
        Position2D targetPosition, boolean shiftDown, boolean altDown, boolean controlDown) {
        Optional<Position2D> oPreferredSnapPos;
        if (controlDown) {
            oPreferredSnapPos = Optional.empty();
        } else if (mFeedbackManager != null) {
            oPreferredSnapPos = mFeedbackManager.correctDragPosition(targetPosition);
        } else {
            oPreferredSnapPos = Optional.empty();
        }
        super.dragAnchorDock(anchor, oPreferredSnapPos.orElse(targetPosition), shiftDown, altDown, controlDown);
    }

    @Override
    protected Optional<Shape> getShapeForIntersectionCheck() {
        return Optional.of(mBorder);
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        updateShape();
        if (mIntermediatePoint == null) {
            // Intermediate point not visible ATM
            return;
        }
        mIntermediatePoint.updateScale(scaleCompensation);
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateShape();
        updateIntermediatePoints();
        updateProperties();
    }
}
