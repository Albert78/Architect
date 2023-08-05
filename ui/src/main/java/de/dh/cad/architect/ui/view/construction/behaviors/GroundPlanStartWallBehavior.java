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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.InteractionsControl;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.wall.ChangeWallsVisualFeedbackManager;
import de.dh.cad.architect.ui.view.construction.feedback.wall.PrincipalWallAncillaryWallsModel;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.AbstractWallEnding;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.AbstractWallEnding.AbstractWallEndConfiguration;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.UnconnectedWallEnding;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * First part of the two-part behavior to create walls. Second part is {@link GroundPlanAddWallBehavior}.
 */
public class GroundPlanStartWallBehavior extends AbstractGroundPlanCreateWallBehavior {
    protected final Property<Length> mThicknessProperty = new SimpleObjectProperty<>(Constants.DEFAULT_WALL_THICKNESS);
    protected final Property<WallBevelType> mWallBevelProperty = new SimpleObjectProperty<>(WallBevelType.DEFAULT);

    protected AbstractWallEndConfiguration mWallStartConfig = null;

    protected ChangeWallsVisualFeedbackManager mFeedbackManager = null; // Lives from install() to uninstall()
    protected StartWallInteractionsPaneControl mInteractionsPane = null;

    public GroundPlanStartWallBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
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
        mThicknessProperty.addListener(lengthPropertyChangeListener);
        mWallBevelProperty.addListener(wallBevelPropertyChangeListener);
    }

    public Property<Length> thicknessProperty() {
        return mThicknessProperty;
    }

    public Length getThickness() {
        return mThicknessProperty.getValue();
    }

    public void setThickness(Length value) {
        mThicknessProperty.setValue(value);
    }

    public Property<WallBevelType> wallBevelProperty() {
        return mWallBevelProperty;
    }

    public WallBevelType getWallBevel() {
        return mWallBevelProperty.getValue();
    }

    public void setWallBevel(WallBevelType value) {
        mWallBevelProperty.setValue(value);
    }

    @Override
    protected void initializeActions() {
        // Cancel add wall
        mActionsList.add(createCancelBehaviorAction(Strings.ACTION_GROUND_PLAN_CANCEL_CREATE_WALL_TITLE));
    }

    @Override
    protected void setDefaultUserHint() {
        setUserHint(Strings.GROUND_PLAN_START_WALL_BEHAVIOR_DEFAULT_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.GROUND_PLAN_START_WALL_BEHAVIOR_TITLE;
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

    /**
     * Completely (re)builds all visual feedback for the given new wall's start situation.
     */
    protected void initializeFeedbackManager(AbstractWallEnding newWallStartEnding) {
        PrincipalWallAncillaryWallsModel ancillaryWallsModel = new PrincipalWallAncillaryWallsModel();
        ancillaryWallsModel.getPrincipalWall().setThickness(getThickness());

        WallBevelType wallBevel = getWallBevel();
        mWallStartConfig = newWallStartEnding.configureWallStart(ancillaryWallsModel, wallBevel, true);
        newWallStartEnding.tryUpdateWallStart(mWallStartConfig, Position2D.zero());
        mFeedbackManager.initializeForAddWall(ancillaryWallsModel);
    }

    /**
     * Rebuilds the feedback manager for the current virtual wall's start situation, to be
     * called e.g. after properties like wall thickness have changed.
     */
    protected void resetFeedbackManager() {
        if (mFeedbackManager == null || mWallStartConfig == null) {
            return;
        }
        initializeFeedbackManager(mWallStartConfig.getWallEnding());
    }

    protected void updateWallStart(Position2D startPosition, Optional<Anchor> oStartDockAnchor) {
        AbstractWallEnding newWallStartEnding = findNewWallEnding(startPosition, oStartDockAnchor, true, mWallStartConfig,
            Collections.emptyList(), Collections.emptyList());
        if (mWallStartConfig == null || !newWallStartEnding.tryUpdateWallStart(mWallStartConfig, Position2D.zero())) {
            initializeFeedbackManager(newWallStartEnding);
        }

        mFeedbackManager.updateVisualObjects();
    }

    @Override
    protected void showPositionFeedback(Position2D pos) {
        updateWallStart(pos, Optional.empty());
    }

    @Override
    protected void showAnchorFeedback(AnchorConstructionRepresentation dockAnchorRepr) {
        if (dockAnchorRepr == null) {
            setDefaultUserHint();
        } else {
            Anchor dockAnchor = dockAnchorRepr.getAnchor();
            updateWallStart(dockAnchor.getPosition().projectionXY(), Optional.of(dockAnchor));
        }
    }

    protected void doTrigger() {
        GroundPlanAddWallBehavior behavior = GroundPlanAddWallBehavior.startingWithStartWallEnding(mWallStartConfig.getWallEnding(), getThickness(), mParentMode);
        behavior.setWallBevelA(getWallBevel());
        mParentMode.setBehavior(behavior);
    }

    @Override
    protected void triggerPoint(Position2D pos) {
        doTrigger();
    }

    @Override
    protected void triggerAnchor(AnchorConstructionRepresentation anchorRepr) {
        doTrigger();
    }

    protected void buildInteractionsPane() {
        mInteractionsPane = new StartWallInteractionsPaneControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mInteractionsPane.initializeValues(getThickness(), getWallBevel());
        mThicknessProperty.bind(mInteractionsPane.getThicknessProperty());
        mWallBevelProperty.bind(mInteractionsPane.getWallBevelStartProperty());
    }

    protected void createInteractionsTab() {
        buildInteractionsPane();
        setInteractionsControl(new InteractionsControl(mInteractionsPane, Strings.GROUND_PLAN_START_WALL_BEHAVIOR_INTERACTIONS_TAB_TITLE, true));
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);

        ConstructionView constructionView = getView();
        mFeedbackManager = new ChangeWallsVisualFeedbackManager(constructionView);
        initializeFeedbackManager(new UnconnectedWallEnding(Position2D.zero()));

        createInteractionsTab();
    }

    @Override
    public void uninstall() {
        mFeedbackManager.uninstall();
        mFeedbackManager = null;

        mWallStartConfig = null;

        super.uninstall();
    }
}
