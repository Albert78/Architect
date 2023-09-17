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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.model.wallmodel.WallEndView;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controller.UiController.DockConflictStrategy;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractAnchoredObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.InteractionsControl;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWallsModel;
import de.dh.cad.architect.ui.view.construction.feedback.wall.ChangeWallsVisualFeedbackManager;
import de.dh.cad.architect.ui.view.construction.feedback.wall.PrincipalWallAncillaryWallsModel;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.AbstractWallEnding;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.AbstractWallEnding.AbstractWallEndConfiguration;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.DockedWallEnding;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.UnconnectedWallEnding;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Second part of the two-part behavior to create walls. First part is {@link GroundPlanStartWallBehavior}.
 */
public class GroundPlanAddWallBehavior extends AbstractGroundPlanCreateWallBehavior {
    protected static final Length MIN_WALL_THICKNESS = Length.ofCM(5);
    protected static final Length DEFAULT_WALL_THICKNESS = Constants.DEFAULT_WALL_THICKNESS;

    protected final Property<Length> mThicknessProperty = new SimpleObjectProperty<>(DEFAULT_WALL_THICKNESS);
    protected final Property<WallBevelType> mWallBevelAProperty = new SimpleObjectProperty<>(WallBevelType.DEFAULT);
    protected final Property<WallBevelType> mWallBevelBProperty = new SimpleObjectProperty<>(WallBevelType.DEFAULT);
    protected final Property<Length> mHeightAProperty = new SimpleObjectProperty<>(Constants.DEFAULT_WALL_HEIGHT);
    protected final Property<Length> mHeightBProperty = new SimpleObjectProperty<>(Constants.DEFAULT_WALL_HEIGHT);
    protected final Property<Boolean> mDrawStartWallCWProperty = new SimpleBooleanProperty(true);
    protected final Property<Boolean> mDrawStartWallCCWProperty = new SimpleBooleanProperty(true);
    protected final Property<Length> mStartWallsThicknessProperty = new SimpleObjectProperty<>(DEFAULT_WALL_THICKNESS);
    protected final Property<Boolean> mDrawEndWallCWProperty = new SimpleBooleanProperty(true);
    protected final Property<Boolean> mDrawEndWallCCWProperty = new SimpleBooleanProperty(true);
    protected final Property<Length> mEndWallsThicknessProperty = new SimpleObjectProperty<>(DEFAULT_WALL_THICKNESS);

    protected ChangeWallsVisualFeedbackManager mFeedbackManager = null; // Lives from install() to uninstall()
    protected AddWallInteractionsPaneControl mInteractionsPane = null;

    protected AbstractWallEndConfiguration mWallStartConfig = null;
    protected AbstractWallEndConfiguration mWallEndConfig = null;

    protected final AbstractWallEnding mWallStartEnding;
    protected final Collection<Wall> mForbiddenDockWalls;
    protected final Collection<Anchor> mForbiddenDockAnchors;

    public GroundPlanAddWallBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode, Optional<Anchor> oStartDockAnchor, AbstractWallEnding startWallEnding) {
        super(parentMode);
        mWallStartEnding = startWallEnding;
        Collection<Wall> connectedWalls = new ArrayList<>();
        mWallStartEnding.getOConnectedAnchor().ifPresent(connectedAnchor -> {
            connectedWalls.addAll(connectedAnchor.getAllDockOwners()
                .stream()
                .filter(owner -> (owner instanceof Wall))
                .map(owner -> (Wall) owner)
                .toList());
        });
        mWallStartEnding.getOConnectedWall().ifPresent(connectedWall -> {
            connectedWalls.add(connectedWall);
        });
        mForbiddenDockWalls = connectedWalls;
        mForbiddenDockAnchors = new ArrayList<>(connectedWalls
                .stream()
                .flatMap(wall -> wall.getAnchors().stream())
                .flatMap(anchor -> anchor.getAllDockedAnchors().stream())
                .toList());

        ChangeListener<Length> lengthPropertyChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Length> observable, Length oldValue, Length newValue) {
                resetFeedbackManager();
            }
        };
        ChangeListener<WallBevelType> wallBevelPropertyChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends WallBevelType> observable, WallBevelType oldValue, WallBevelType newValue) {
                resetFeedbackManager();
            }
        };
        ChangeListener<Boolean> booleanPropertyChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                resetFeedbackManager();
            }
        };
        mThicknessProperty.addListener(lengthPropertyChangeListener);
        mWallBevelAProperty.addListener(wallBevelPropertyChangeListener);
        mWallBevelBProperty.addListener(wallBevelPropertyChangeListener);
        mHeightAProperty.addListener(lengthPropertyChangeListener);
        mHeightBProperty.addListener(lengthPropertyChangeListener);
        mDrawStartWallCWProperty.addListener(booleanPropertyChangeListener);
        mDrawStartWallCCWProperty.addListener(booleanPropertyChangeListener);
        mStartWallsThicknessProperty.addListener(lengthPropertyChangeListener);
        mDrawEndWallCWProperty.addListener(booleanPropertyChangeListener);
        mDrawEndWallCCWProperty.addListener(booleanPropertyChangeListener);
        mEndWallsThicknessProperty.addListener(lengthPropertyChangeListener);
    }

    public static GroundPlanAddWallBehavior startingWithStartWallEnding(AbstractWallEnding startWallEnding, Length wallThickness, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        GroundPlanAddWallBehavior result = new GroundPlanAddWallBehavior(parentMode, Optional.empty(), startWallEnding);
        Optional<Anchor> oConnectedAnchor = startWallEnding.getOConnectedAnchor();
        if (oConnectedAnchor.isPresent()) {
            Anchor connectedAnchor = oConnectedAnchor.get();
            result.initializeFromPredecessorWallEnd(WallEndView.fromWallHandle(connectedAnchor));
        } else {
            startWallEnding.getOConnectedWall().ifPresent(connectedWall -> {
                result.initializeFromPredecessorWall(connectedWall);
            });
        }
        result.setThickness(wallThickness);
        return result;
    }

    public Property<Length> thicknessProperty() {
        return mThicknessProperty;
    }

    public Length getThickness() {
        return mThicknessProperty.getValue();
    }

    public Length getThicknessOrDefault() {
        return getSafeLength(getThickness(), MIN_WALL_THICKNESS, DEFAULT_WALL_THICKNESS);
    }

    public void setThickness(Length value) {
        mThicknessProperty.setValue(value);
    }

    public Property<WallBevelType> wallBevelAProperty() {
        return mWallBevelAProperty;
    }

    public WallBevelType getWallBevelA() {
        return mWallBevelAProperty.getValue();
    }

    public void setWallBevelA(WallBevelType value) {
        mWallBevelAProperty.setValue(value);
    }

    public Property<WallBevelType> wallBevelBProperty() {
        return mWallBevelBProperty;
    }

    public WallBevelType getWallBevelB() {
        return mWallBevelBProperty.getValue();
    }

    public void setWallBevelB(WallBevelType value) {
        mWallBevelBProperty.setValue(value);
    }

    public Property<Length> heightAProperty() {
        return mHeightAProperty;
    }

    public Length getHeightA() {
        return mHeightAProperty.getValue();
    }

    public void setHeightA(Length value) {
        mHeightAProperty.setValue(value);
    }

    public Property<Length> heightBProperty() {
        return mHeightBProperty;
    }

    public Length getHeightB() {
        return mHeightBProperty.getValue();
    }

    public void setHeightB(Length value) {
        mHeightBProperty.setValue(value);
    }

    public Property<Boolean> drawStartWallCWProperty() {
        return mDrawStartWallCWProperty;
    }

    public boolean isDrawStartWallCW() {
        return mDrawStartWallCWProperty.getValue();
    }

    public void setDrawStartWallCW(boolean value) {
        mDrawStartWallCWProperty.setValue(value);
    }

    public Property<Boolean> drawStartWallCCWProperty() {
        return mDrawStartWallCCWProperty;
    }

    public boolean isDrawStartWallCCW() {
        return mDrawStartWallCCWProperty.getValue();
    }

    public void setDrawStartWallCCW(boolean value) {
        mDrawStartWallCCWProperty.setValue(value);
    }

    public Property<Boolean> drawEndWallCWProperty() {
        return mDrawEndWallCWProperty;
    }

    public boolean isDrawEndWallCW() {
        return mDrawEndWallCWProperty.getValue();
    }

    public void setDrawEndWallCW(boolean value) {
        mDrawEndWallCWProperty.setValue(value);
    }

    public Property<Length> startWallsThicknessProperty() {
        return mStartWallsThicknessProperty;
    }

    public Length getStartWallsThickness() {
        return mStartWallsThicknessProperty.getValue();
    }

    public Length getStartWallsThicknessOrDefault() {
        return getSafeLength(getStartWallsThickness(), MIN_WALL_THICKNESS, DEFAULT_WALL_THICKNESS);
    }

    public void setStartWallsThickness(Length value) {
        mStartWallsThicknessProperty.setValue(value);
    }

    public Property<Boolean> drawEndWallCCWProperty() {
        return mDrawEndWallCCWProperty;
    }

    public boolean isDrawEndWallCCW() {
        return mDrawEndWallCCWProperty.getValue();
    }

    public void setDrawEndWallCCW(boolean value) {
        mDrawEndWallCCWProperty.setValue(value);
    }

    public Property<Length> endWallsThicknessProperty() {
        return mEndWallsThicknessProperty;
    }

    public Length getEndWallsThickness() {
        return mEndWallsThicknessProperty.getValue();
    }

    public Length getEndWallsThicknessOrDefault() {
        return getSafeLength(getEndWallsThickness(), MIN_WALL_THICKNESS, DEFAULT_WALL_THICKNESS);
    }

    public void setEndWallsThickness(Length value) {
        mEndWallsThicknessProperty.setValue(value);
    }

    public Length getSafeLength(Length value, Length minValue, Length defaultValue) {
        if (value == null || !value.isValid() || value.lt(minValue)) {
            value = defaultValue;
        }
        return value;
    }

    protected void initializeFromPredecessorWallEnd(WallEndView wallEndView) {
        WallBevelType wallBevel = wallEndView.getWallBevel();
        Length wallHeight = wallEndView.getWallHeight();
        Wall predecessorWall = wallEndView.getWall();
        Length thickness = predecessorWall.getThickness();
        setWallBevelA(wallBevel);
        setWallBevelB(wallBevel);
        setHeightA(wallHeight);
        setHeightB(wallHeight);
        setThickness(thickness);
    }

    protected void initializeFromPredecessorWall(Wall wall) {
        WallBevelType wallBevel = wall.getWallBevelA();
        Length wallHeight = wall.getHeightA();
        Length thickness = wall.getThickness();
        setWallBevelA(wallBevel);
        setWallBevelB(wallBevel);
        setHeightA(wallHeight);
        setHeightB(wallHeight);
        setThickness(thickness);
    }

    @Override
    protected void initializeActions() {
        mActionsList.add(createCancelBehaviorAction(Strings.ACTION_GROUND_PLAN_CANCEL_ADD_WALL_TITLE));
    }

    @Override
    public void setDefaultUserHint() {
        setUserHint(Strings.GROUND_PLAN_ADD_WALL_BEHAVIOR_DEFAULT_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.GROUND_PLAN_ADD_WALL_BEHAVIOR_TITLE;
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        // No actions for this behavior yet
    }

    @Override
    protected Position2D calculateCorrectedPosition(Position2D pos) {
        Optional<Position2D> oPreferredSnapPos = mFeedbackManager.correctDragPosition(pos);
        if (oPreferredSnapPos.isPresent()) {
            return oPreferredSnapPos.get();
        }
        return super.calculateCorrectedPosition(pos);
    }

    @Override
    protected void showPositionFeedback(Position2D pos) {
        setUserHint(MessageFormat.format(Strings.GROUND_PLAN_ADD_WALL_BEHAVIOR_END_POSITION_AIMED, pos.axesAndCoordsToString()));
        updateFeedback(pos, Optional.empty());
    }

    @Override
    protected void showAnchorFeedback(AnchorConstructionRepresentation dockAnchorRepr) {
        if (dockAnchorRepr == null) {
            setDefaultUserHint();
        } else {
            Anchor dockAnchor = dockAnchorRepr.getAnchor();
            setUserHint(MessageFormat.format(Strings.GROUND_PLAN_ADD_WALL_BEHAVIOR_ANCHOR_AIMED, BaseObjectUIRepresentation.getObjName(dockAnchor.getAnchorOwner())));
            updateFeedback(dockAnchor.getPosition().projectionXY(), Optional.of(dockAnchor));
        }
    }

    protected void commitWall() {
        Length thickness = getThickness();
        Length heightA = getHeightA();
        Length heightB = getHeightB();
        List<IModelChange> changeTrace = new ArrayList<>();
        Wall wall = Wall.createFromHandlePositions(
            BaseObjectUIRepresentation.generateSimpleName(getPlan().getWalls().values(), Wall.class),
            thickness, heightA, heightB,
            Position2D.zero(),
            Position2D.zero().movedX(Length.ofM(1)),
            getPlan(), changeTrace);

        UiController uiController = getUiController();
        mWallStartConfig.configureFinalWall(wall, wall.getAnchorWallHandleA(), uiController, DockConflictStrategy.SkipDock, changeTrace);
        mWallEndConfig.configureFinalWall(wall, wall.getAnchorWallHandleB(), uiController, DockConflictStrategy.SkipDock, changeTrace);
        uiController.notifyChange(changeTrace, Strings.WALL_ADD_CHANGE);

        // Automatically continue next wall
        GroundPlanAddWallBehavior nextWallBehavior = startingWithStartWallEnding(new DockedWallEnding(wall.getAnchorWallHandleB()), getThickness(), mParentMode);
        nextWallBehavior.setDrawStartWallCW(false);
        nextWallBehavior.setDrawStartWallCCW(false);
        nextWallBehavior.setDrawEndWallCW(isDrawEndWallCW());
        nextWallBehavior.setDrawEndWallCCW(isDrawEndWallCCW());
        WallBevelType continuingBevel = getWallBevelB(); // Continue next wall with the end bevel (= B)
        nextWallBehavior.setWallBevelA(continuingBevel);
        nextWallBehavior.setWallBevelB(continuingBevel);
        Length continuingHeight = heightB;
        nextWallBehavior.setHeightA(continuingHeight);
        nextWallBehavior.setHeightB(continuingHeight);
        mParentMode.setBehavior(nextWallBehavior);
        // Or: Break adding wall process:
        //mParentMode.resetBehavior();
    }

    @Override
    protected void triggerPoint(Position2D endPos) {
        commitWall();
    }


    @Override
    protected void triggerAnchor(AnchorConstructionRepresentation dockAnchorRepr) {
        commitWall();
    }

    @Override
    protected boolean isPossibleDockTargetAnchor(Anchor anchor) {
        if (GroundPlanStartWallBehavior.isPossibleDockTargetAnchorForWall(anchor)) {
            AbstractWallEnding startWallEnding = mWallStartEnding;
            // Start and end anchors must not be docked to the same wall
            boolean isStartWallAnchor = startWallEnding.getOConnectedWall().map(startWall -> anchor.getAnchorOwner().equals(startWall)).orElse(false);
            return !isStartWallAnchor;
        }
        return false;
    }

    @Override
    protected AbstractAnchoredObjectConstructionRepresentation getDockTargetElementIfSupported(Abstract2DRepresentation repr) {
        return GroundPlanStartWallBehavior.getDockElementForWallIfSupported(repr);
    }

    // ******************************************** Feedback calculation ***********************************

    @Override
    protected UnconnectedWallEnding createUnconnectedWallEnd(Position2D position, boolean isStart) {
        UnconnectedWallEnding result = new UnconnectedWallEnding(position);
        if (isStart) {
            configUnconnectedStartWallEnd(result);
        } else {
            configUnconnectedEndWallEnd(result);
        }
        return result;
    }


    protected void configUnconnectedStartWallEnd(UnconnectedWallEnding wallEnding) {
        configWallEnd(wallEnding, isDrawStartWallCW(), isDrawStartWallCCW(), getStartWallsThicknessOrDefault());
    }

    protected void configUnconnectedEndWallEnd(UnconnectedWallEnding wallEnding) {
        configWallEnd(wallEnding, isDrawEndWallCW(), isDrawEndWallCCW(), getEndWallsThicknessOrDefault());
    }

    protected void configWallEnd(AbstractWallEnding wallEnding, boolean drawWallCW, boolean drawWallCCW, Length wallThickness) {
        if (!(wallEnding instanceof UnconnectedWallEnding)) {
            return;
        }
        UnconnectedWallEnding uwe = (UnconnectedWallEnding) wallEnding;
        uwe.setDrawWallCW(drawWallCW);
        uwe.setDrawWallCCW(drawWallCCW);
        uwe.setWallThickness(wallThickness);
    }

    /**
     * Completely (re)builds all visual feedback for the given new wall's end situation.
     */
    protected void initializeFeedbackManager(AbstractWallEnding newWallEndEnding) {
        PrincipalWallAncillaryWallsModel ancillaryWallsModel = new PrincipalWallAncillaryWallsModel();
        ancillaryWallsModel.getPrincipalWall().setThickness(getThicknessOrDefault());

        if (mWallStartEnding instanceof UnconnectedWallEnding uwe) {
            configUnconnectedStartWallEnd(uwe);
        }
        mWallStartConfig = mWallStartEnding.configureWallStart(ancillaryWallsModel, getWallBevelA(), false);
        mWallStartEnding.tryUpdateWallStart(mWallStartConfig, newWallEndEnding.getPosition());

        if (newWallEndEnding instanceof UnconnectedWallEnding uwe) {
            configUnconnectedEndWallEnd(uwe);
        }
        mWallEndConfig = newWallEndEnding.configureWallEnd(ancillaryWallsModel, getWallBevelB());
        newWallEndEnding.tryUpdateWallEnd(mWallEndConfig, mWallStartEnding.getPosition());
        ancillaryWallsModel.getPrincipalWallStartAnchor().setReferenceAnchor(true);
        ancillaryWallsModel.setWallAnchorForMovingHandleFeedback(ancillaryWallsModel.getPrincipalWallEndAnchor());
        ancillaryWallsModel.addWallsForFeedback(true, true, ancillaryWallsModel.getPrincipalWall());

        mFeedbackManager.initializeForAddWall(ancillaryWallsModel);
    }

    /**
     * Rebuilds the feedback manager for the current virtual wall's end situation, to be
     * called e.g. after properties like wall thickness, beveltype etc. have changed.
     */
    protected void resetFeedbackManager() {
        if (mFeedbackManager == null || mWallEndConfig == null) {
            return;
        }
        initializeFeedbackManager(mWallEndConfig.getWallEnding());
    }

    /**
     * Entry point to update the virtual situation to the given new changed virtual wall's end situation.
     *
     * Dispatches to {@link #updateAncillaryWallsModel(AncillaryWallsModel, Position2D)} and
     * {@link #initializeFeedbackManager(Position2D, Optional)} depending on the extent of the change compared to the
     * former situation.
     */
    protected void updateFeedback(Position2D endPosition, Optional<Anchor> oEndDockAnchor) {
        AbstractWallEnding newWallEndEnding = findNewWallEnding(endPosition, oEndDockAnchor, false, mWallEndConfig,
            mForbiddenDockWalls, mForbiddenDockAnchors);
        mWallStartEnding.tryUpdateWallStart(mWallStartConfig, newWallEndEnding.getPosition());
        if (mWallEndConfig == null || !newWallEndEnding.tryUpdateWallEnd(mWallEndConfig, mWallStartEnding.getPosition())) {
            initializeFeedbackManager(newWallEndEnding);
        }

        mFeedbackManager.updateVisualObjects();
    }

    // ********************************** End feedback calculation ************************************

    protected void buildInteractionsPane() {
        mInteractionsPane = new AddWallInteractionsPaneControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mInteractionsPane.initializeValues(getThickness(), getWallBevelA(), getWallBevelB(), getHeightA(), getHeightB(),
                isDrawStartWallCW(), isDrawStartWallCCW(), isDrawEndWallCW(), isDrawEndWallCCW());
        mDrawStartWallCWProperty.bind(mInteractionsPane.getDrawStartWallCwProperty());
        mDrawStartWallCCWProperty.bind(mInteractionsPane.getDrawStartWallCcwProperty());
        mStartWallsThicknessProperty.bind(mInteractionsPane.getStartWallsThicknessProperty());
        mDrawEndWallCWProperty.bind(mInteractionsPane.getDrawEndWallCwProperty());
        mDrawEndWallCCWProperty.bind(mInteractionsPane.getDrawEndWallCcwProperty());
        mEndWallsThicknessProperty.bind(mInteractionsPane.getEndWallsThicknessProperty());

        mThicknessProperty.bind(mInteractionsPane.getThicknessProperty());
        mWallBevelAProperty.bind(mInteractionsPane.getWallBevelStartProperty());
        mWallBevelBProperty.bind(mInteractionsPane.getWallBevelEndProperty());
        mHeightAProperty.bind(mInteractionsPane.getHeightStartProperty());
        mHeightBProperty.bind(mInteractionsPane.getHeightEndProperty());
        if (!(mWallStartEnding instanceof UnconnectedWallEnding)) {
            mInteractionsPane.disableStartOrthogonalWallSettings();
        }
    }

    protected void createInteractionsTab() {
        buildInteractionsPane();
        setInteractionsControl(new InteractionsControl(mInteractionsPane, Strings.GROUND_PLAN_ADD_WALL_BEHAVIOR_INTERACTIONS_TAB_TITLE, true));
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);

        ConstructionView constructionView = getView();
        mFeedbackManager = new ChangeWallsVisualFeedbackManager(constructionView);
        initializeFeedbackManager(createUnconnectedWallEnd(mWallStartEnding.getPosition().movedX(Length.ofCM(50)), false));

        createInteractionsTab();
    }

    @Override
    public void uninstall() {
        mFeedbackManager.uninstall();
        mFeedbackManager = null;
        mWallStartConfig = null;
        mWallEndConfig = null;

        super.uninstall();
    }
}
