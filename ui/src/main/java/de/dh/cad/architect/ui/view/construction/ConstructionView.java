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
package de.dh.cad.architect.ui.view.construction;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.GuideLine.GuideLineDirection;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controls.HorizontalRuler;
import de.dh.cad.architect.ui.controls.VerticalRuler;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.Abstract2DUiObject;
import de.dh.cad.architect.ui.persistence.ConstructionViewState;
import de.dh.cad.architect.ui.persistence.ViewState;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.NullMode;
import de.dh.cad.architect.ui.view.OptionalCoordinates2D;
import de.dh.cad.architect.ui.view.construction.behaviors.AbstractConstructionBehavior;
import de.dh.utils.fx.ImageUtils;
import de.dh.utils.fx.Vector2D;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Affine;

public class ConstructionView extends Abstract2DView {
    protected static final double SNAP_DELTA = 10;

    protected static final String ICON_SELECTION_MODE_RESOURCE = "SelectionMode.png";
    protected static final String ICON_GROUND_PLAN_MODE_RESOURCE = "GroundPlanMode.png";
    protected static final String ICON_SUPPORT_OBJECTS_MODE_RESOURCE = "SupportObjectsMode.png";

    protected static final int TOOL_BAR_ICON_SIZE = 32;

    protected EventHandler<MouseEvent> mCursorMarkerMoveEventHandler = null;
    protected EventHandler<MouseEvent> mCursorMarkerExitEventHandler = null;

    protected final SelectionMode mSelectionMode;
    protected final GroundPlanMode mGroundPlanMode;
    protected final SupportObjectsMode mSupportObjectsMode;

    protected ToggleButton mSelectionModeButton = null;
    protected ToggleButton mGroundPlanModeButton = null;
    protected ToggleButton mSupportObjectsModeButton = null;

    protected HorizontalRuler mHorizontalRuler;
    protected VerticalRuler mVerticalRuler;
    protected AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> mConstructionMode = new NullMode<>(); // Initialization with dummy value for simplification of initialization
    protected Affine mRootTransform = new Affine(); // Contains the combined transformation matrix of all translations (dragged plan) and scales (zoom)
    protected double mScale = 1.0; // Redundant combined scale value of all zoom gestures
    protected double mScaleCompensation = 1.0; // 1/mCombinedScale
    protected Vector2D mCombinedTranslation = new Vector2D(0, 0);
    protected Map<String, Line> mGuideLines = new TreeMap<>();
    protected ConstructionViewState mSavedViewState;

    public ConstructionView(UiController uiController) {
        super(uiController);

        initializeViewState();

        mSelectionMode = new SelectionMode(mUiController);
        mGroundPlanMode = new GroundPlanMode(mUiController);
        mSupportObjectsMode = new SupportObjectsMode(mUiController);

        addRulers();
    }

    @Override
    protected void uninitialize() {
        setBehavior(null);

        takeViewStateFromView();

        mGuideLines.clear();
        mVerticalRuler.clearGuideLines();
        mHorizontalRuler.clearGuideLines();

        mSelectionModeButton = null;
        mGroundPlanModeButton = null;
        mSupportObjectsModeButton = null;

        super.uninitialize();
    }

    protected void initializeViewState() {
        mSavedViewState = new ConstructionViewState();
        mSavedViewState.setScale(1.0);
    }

    @Override
    protected void initialize() {
        mSelectionModeButton = new ToggleButton();
        mSelectionModeButton.setGraphic(ImageUtils.loadSquareIcon(ConstructionView.class, ICON_SELECTION_MODE_RESOURCE, TOOL_BAR_ICON_SIZE));
        mSelectionModeButton.setTooltip(new Tooltip(Strings.CONSTRUCTION_SELECTION_MODE_ACTION_TOOLTIP));
        mSelectionModeButton.setOnAction(action -> {
            setConstructionMode(mSelectionMode);
        });

        mGroundPlanModeButton = new ToggleButton();
        mGroundPlanModeButton.setGraphic(ImageUtils.loadSquareIcon(ConstructionView.class, ICON_GROUND_PLAN_MODE_RESOURCE, TOOL_BAR_ICON_SIZE));
        mGroundPlanModeButton.setTooltip(new Tooltip(Strings.CONSTRUCTION_MODE_ACTION_TOOLTIP));
        mGroundPlanModeButton.setOnAction(action -> {
            setConstructionMode(mGroundPlanMode);
        });

        mSupportObjectsModeButton = new ToggleButton();
        mSupportObjectsModeButton.setGraphic(ImageUtils.loadSquareIcon(ConstructionView.class, ICON_SUPPORT_OBJECTS_MODE_RESOURCE, TOOL_BAR_ICON_SIZE));
        mSupportObjectsModeButton.setTooltip(new Tooltip(Strings.SUPPORT_OBJECTS_MODE_ACTION_TOOLTIP));
        mSupportObjectsModeButton.setOnAction(action -> {
            setConstructionMode(mSupportObjectsMode);
        });

        super.initialize();
        setToolBarContributionItems(mSelectionModeButton, mGroundPlanModeButton, mSupportObjectsModeButton);

        updateViewToViewState();
        updateToTransform(true);
// TODO: Move to stylesheet
mCenterPane.setStyle("-fx-background-color: cornsilk;");
        setConstructionMode(mGroundPlanMode);
    }

    @Override
    protected void initializeFromPlan() {
        Plan plan = getPlan();

        addUIRepresentations(plan.getDimensionings().values());
        addUIRepresentations(plan.getAnchors().values());
        addUIRepresentations(plan.getFloors().values());
        addUIRepresentations(plan.getWalls().values());
        addUIRepresentations(plan.getWalls().values().stream().flatMap(w -> w.getWallHoles().stream()).collect(Collectors.toList()));
        addUIRepresentations(plan.getCeilings().values());
        addUIRepresentations(plan.getCoverings().values());
        addUIRepresentations(plan.getSupportObjects().values());
        for (Abstract2DRepresentation repr : mRepresentationsById.values()) {
            repr.updateToModel();
        }
        for (GuideLine guideLine : plan.getGuideLines().values()) {
            addGuideLine(guideLine);
        }
    }

    public AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> getConstructionMode() {
        return mConstructionMode;
    }

    public void setConstructionMode(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> value) {
        AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> currentMode = mConstructionMode;
        if (currentMode != null) {
            currentMode.uninstall();
        }
        if (value != null) {
            value.install(this);
        }
        mConstructionMode = value;

        mSelectionModeButton.setSelected(false);
        mGroundPlanModeButton.setSelected(false);
        mSupportObjectsModeButton.setSelected(false);
        if (value instanceof SelectionMode) {
            mSelectionModeButton.setSelected(true);
        } else if (value instanceof GroundPlanMode) {
            mGroundPlanModeButton.setSelected(true);
        } else if (value instanceof SupportObjectsMode) {
            mSupportObjectsModeButton.setSelected(true);
        }
    }

    public Affine getRootTransform() {
        return mRootTransform;
    }

    @Override
    public Class<? extends ViewState> getViewStateClass() {
        return ConstructionViewState.class;
    }

    @Override
    public Optional<ConstructionViewState> getViewState() {
        takeViewStateFromView();
        return Optional.of(mSavedViewState);
    }

    @Override
    public void setViewState(ViewState viewState) {
        mSavedViewState = (ConstructionViewState) viewState;
        updateViewToViewState();
    }

    protected void takeViewStateFromView() {
        if (!isAlive()) {
            return;
        }
        mSavedViewState.setScale(mScale);
        mSavedViewState.setTranslateX(mCombinedTranslation.getX());
        mSavedViewState.setTranslateY(mCombinedTranslation.getY());
    }

    protected void updateViewToViewState() {
        if (!isAlive()) {
            return;
        }
        mScale = mSavedViewState.getScale();
        mScaleCompensation = 1 / mScale;
        double tx = mSavedViewState.getTranslateX();
        double ty = mSavedViewState.getTranslateY();
        mCombinedTranslation = new Vector2D(tx, ty);

        mRootTransform.setToIdentity();
        mRootTransform.appendTranslation(tx, ty);
        mRootTransform.appendScale(mScale, mScale);
        updateToTransform(true);
    }

    protected void addRulers() {
        mHorizontalRuler = new HorizontalRuler(mUiController);
        mVerticalRuler = new VerticalRuler(mUiController);
        BorderPane topRulerParent = new BorderPane();
        topRulerParent.setCenter(mHorizontalRuler);
        Pane corner = new Pane();
        corner.minWidthProperty().bind(mVerticalRuler.widthProperty());
        topRulerParent.setLeft(corner);
        setTop(topRulerParent);
        setLeft(mVerticalRuler);
    }

    public double getScale() {
        return mScale;
    }

    public double getScaleCompensation() {
        return mScaleCompensation;
    }

    @Override
    public AbstractConstructionBehavior getBehavior() {
        return (AbstractConstructionBehavior) super.getBehavior();
    }

    @Override
    public void addAncillaryObject(Abstract2DAncillaryObject aao) {
        aao.updateScale(mScaleCompensation);
        super.addAncillaryObject(aao);
    }

    @Override
    protected Collection<Abstract2DRepresentation> doAddUIRepresentations(Collection<? extends BaseObject> addedObjects) {
        Collection<Abstract2DRepresentation> result = super.doAddUIRepresentations(addedObjects);
        for (Abstract2DRepresentation repr : result) {
            repr.updateScale(mScaleCompensation);
        }
        return result;
    }

    @Override
    protected void onModelObjectsAdded(Collection<BaseObject> addedObjects) {
        super.onModelObjectsAdded(addedObjects);

        for (BaseObject baseObject : addedObjects) {
            if (baseObject instanceof GuideLine guideLine) {
               addGuideLine(guideLine);
            }
        }
    }

    @Override
    protected void onModelObjectsRemoved(Collection<BaseObject> removedObjects) {
        super.onModelObjectsRemoved(removedObjects);

        for (BaseObject baseObject : removedObjects) {
            if (baseObject instanceof GuideLine guideLine) {
               removeGuideLine(guideLine);
            }
        }
    }

    @Override
    protected void onModelObjectsUpdated(Collection<BaseObject> changedObjects) {
        super.onModelObjectsUpdated(changedObjects);
        boolean horizontalGuideLinesChanged = false;
        boolean verticalGuideLinesChanged = false;
        for (BaseObject baseObject : changedObjects) {
            if (baseObject instanceof GuideLine guideLine) {
                if (guideLine.getDirection() == GuideLineDirection.Vertical) {
                    horizontalGuideLinesChanged = true;
                } else {
                    verticalGuideLinesChanged = true;
                }
                updateGuideLine(guideLine);
            }
        }
        if (horizontalGuideLinesChanged) {
            mHorizontalRuler.updateView();
        }
        if (verticalGuideLinesChanged) {
            mVerticalRuler.updateView();
        }
    }

    @Override
    protected void handleObjectsSelectionChanged(Collection<String> removedSelectionIds, Collection<String> addedSelectionIds) {
        super.handleObjectsSelectionChanged(removedSelectionIds, addedSelectionIds);
        Map<String, GuideLine> guideLines = getPlan().getGuideLines();
        for (String id : CollectionUtils.union(removedSelectionIds, addedSelectionIds)) {
            GuideLine guideLine = guideLines.get(id);
            if (guideLine != null) {
                updateGuideLine(guideLine);
            }
        }
    }

    protected void updateGuideLineColor(String guideLineId, Line guideLine) {
        if (mUiController.selectedObjectIds().contains(guideLineId)) {
            guideLine.setStroke(Abstract2DRepresentation.SELECTED_OBJECTS_COLOR);
        } else {
            guideLine.setStroke(Color.DARKGRAY);
        }
    }

    protected void addGuideLine(GuideLine guideLine) {
        Line line = new Line();
        line.setViewOrder(Constants.VIEW_ORDER_GUIDE_LINE);
        String guideLineId = guideLine.getId();
        updateGuideLineColor(guideLineId, line);
        line.setStrokeWidth(1);
        line.getStrokeDashArray().setAll(5d, 10d);
        line.setMouseTransparent(true);
        mTopLayer.getChildren().add(line);
        mGuideLines.put(guideLineId, line);
        GuideLineDirection direction = guideLine.getDirection();
        if (direction == GuideLineDirection.Vertical) {
            double pos = mHorizontalRuler.addGuideLine(guideLine);
            line.setStartX(pos);
            line.setStartY(0);
            line.setEndX(pos);
            line.endYProperty().bind(mCenterPane.heightProperty());
        } else if (direction == GuideLineDirection.Horizontal) {
            double pos = mVerticalRuler.addGuideLine(guideLine);
            line.setStartX(0);
            line.setStartY(pos);
            line.endXProperty().bind(mCenterPane.widthProperty());
            line.setEndY(pos);
        } else {
            throw new RuntimeException("Unknown guideline direction '" + direction + "'");
        }
    }

    protected void updateGuideLine(GuideLine guideLine) {
        String guideLineId = guideLine.getId();
        Line line = mGuideLines.get(guideLineId);
        updateGuideLineColor(guideLineId, line);
        if (guideLine.getDirection() == GuideLineDirection.Vertical) {
            double pos = mHorizontalRuler.updateGuideLine(guideLine);
            line.setStartX(pos);
            line.setEndX(pos);
        } else {
            double pos = mVerticalRuler.updateGuideLine(guideLine);
            line.setStartY(pos);
            line.setEndY(pos);
        }
        line.setVisible(!guideLine.isHidden());
    }

    protected void removeGuideLine(GuideLine guideLine) {
        String guideLineId = guideLine.getId();
        Line line = mGuideLines.get(guideLineId);
        mTopLayer.getChildren().remove(line);
        if (guideLine.getDirection() == GuideLineDirection.Vertical) {
            mHorizontalRuler.removeGuideLine(guideLine);
        } else {
            mVerticalRuler.removeGuideLine(guideLine);
        }
        mGuideLines.remove(guideLineId);
    }

    protected void updateAllGuideLines() {
        Plan plan = getPlan();
        for (String guideLineId : mGuideLines.keySet()) {
            GuideLine guideLine = plan.getGuideLines().get(guideLineId);
            updateGuideLine(guideLine);
        }
    }

    public Position2D snapToGuideLines(Position2D pos) {
        OptionalCoordinates2D oc = findNearestGuideLineCoordinates(pos.getX(), pos.getY());
        return oc.overlay(pos);
    }

    public OptionalCoordinates2D findNearestGuideLineCoordinates(Length intendedX, Length intendedY) {
        Length nearestX = null;
        GuideLine nearestGuideLineX = null;
        Length nearestY = null;
        GuideLine nearestGuideLineY = null;
        Length snapDelta = CoordinateUtils.coordsToLength(SNAP_DELTA / mScale);
        for (GuideLine guideLine : getPlan().getGuideLines().values()) {
            GuideLineDirection direction = guideLine.getDirection();
            if (direction == GuideLineDirection.Vertical) {
                Length distX = guideLine.getPosition().minus(intendedX).abs();
                if (distX.lt(snapDelta)) {
                    if (nearestX == null || distX.lt(nearestX)) {
                        nearestX = distX;
                        nearestGuideLineX = guideLine;
                    }
                }
            } else if (direction == GuideLineDirection.Horizontal) {
                Length distY = guideLine.getPosition().minus(intendedY).abs();
                if (distY.lt(snapDelta)) {
                    if (nearestY == null || distY.lt(nearestY)) {
                        nearestY = distY;
                        nearestGuideLineY = guideLine;
                    }
                }
            } else {
                throw new RuntimeException("Invalid guide line direction '" + direction + "'");
            }
        }
        return new OptionalCoordinates2D(
            nearestGuideLineX == null ? Optional.empty() : Optional.of(nearestGuideLineX.getPosition()),
            nearestGuideLineY == null ? Optional.empty() : Optional.of(nearestGuideLineY.getPosition()));
    }

    @Override
    public String getTitle() {
        return Strings.CONSTRUCTION_VIEW_TITLE;
    }

    public void scale(double coefficient, double sceneX, double sceneY) {
        Point2D cpPoint = mCenterPane.sceneToLocal(sceneX, sceneY);
        Point2D rPoint = mTransformedRoot.sceneToLocal(sceneX, sceneY);

        double transX = mCombinedTranslation.getX();
        double transY = mCombinedTranslation.getY();

        transX = transX + (1 - coefficient) * (cpPoint.getX() - transX);
        transY = transY + (1 - coefficient) * (cpPoint.getY() - transY);

        mCombinedTranslation = new Vector2D(transX, transY);
        mScale *= coefficient;
        mScaleCompensation = 1 / mScale;
        mRootTransform.appendScale(coefficient, coefficient, rPoint.getX(), rPoint.getY());
        updateToTransform(true);
    }

    public void translate(double x, double y) {
        mCombinedTranslation = new Vector2D(mCombinedTranslation.getX() + x * mScale, mCombinedTranslation.getY() + y * mScale);
        mRootTransform.appendTranslation(x, y);
        updateToTransform(false);
    }

    /**
     * Updates all UI elements to changed values in {@link #mCombinedTranslation}, {@link #mScale}, {@link #mScaleCompensation} and {@link #mRootTransform}.
     * @param updateScaleCompensation Set this to {@code true} if {@link #mScaleCompensation} was changed. This will update the scale compensation
     * in all UI representations.
     */
    protected void updateToTransform(boolean updateScaleCompensation) {
        if (updateScaleCompensation) {
            for (Abstract2DRepresentation repr : mRepresentationsById.values()) {
                repr.updateScale(mScaleCompensation);
            }
            for (Abstract2DUiObject obj : mAncillaryObjectsById.values()) {
                obj.updateScale(mScaleCompensation);
            }
        }
        mTransformedRoot.getTransforms().setAll(mRootTransform);
        mHorizontalRuler.setTransform(mScale, mCombinedTranslation);
        mVerticalRuler.setTransform(mScale, mCombinedTranslation);
        updateAllGuideLines();
    }

    public Point2D getPointInPlanFromScene(double sceneX, double sceneY) {
        return mTransformedRoot.sceneToLocal(sceneX, sceneY);
    }

    public Position2D getPlanPositionFromScene(double sceneX, double sceneY) {
        Point2D pt = getPointInPlanFromScene(sceneX, sceneY);
        return CoordinateUtils.coordsToPosition2D(pt.getX(), pt.getY());
    }

    public void enableRulerCursorMarker() {
        if (mCursorMarkerMoveEventHandler == null) {
            mCursorMarkerMoveEventHandler = new EventHandler<>() {
                @Override
                public void handle(MouseEvent event) {
                    Position2D pos = getPlanPositionFromScene(event.getSceneX(), event.getSceneY());
                    mHorizontalRuler.setCursorMarker(pos.getX());
                    mVerticalRuler.setCursorMarker(pos.getY());
                }
            };
            addEventHandler(MouseEvent.MOUSE_MOVED, mCursorMarkerMoveEventHandler);
            addEventHandler(MouseEvent.MOUSE_DRAGGED, mCursorMarkerMoveEventHandler);
        }
        if (mCursorMarkerExitEventHandler == null) {
            mCursorMarkerExitEventHandler = new EventHandler<>() {
                @Override
                public void handle(MouseEvent event) {
                    mHorizontalRuler.setCursorMarker(null);
                    mVerticalRuler.setCursorMarker(null);
                }
            };
            addEventHandler(MouseEvent.MOUSE_EXITED, mCursorMarkerExitEventHandler);
        }
    }

    public void disableRulerCursorMarker() {
        if (mCursorMarkerMoveEventHandler != null) {
            removeEventHandler(MouseEvent.MOUSE_MOVED, mCursorMarkerMoveEventHandler);
            removeEventHandler(MouseEvent.MOUSE_DRAGGED, mCursorMarkerMoveEventHandler);
            mCursorMarkerMoveEventHandler = null;
        }
        if (mCursorMarkerExitEventHandler != null) {
            removeEventHandler(MouseEvent.MOUSE_EXITED, mCursorMarkerExitEventHandler);
            mCursorMarkerExitEventHandler = null;
        }
        mHorizontalRuler.setCursorMarker(null);
        mVerticalRuler.setCursorMarker(null);
    }
}
