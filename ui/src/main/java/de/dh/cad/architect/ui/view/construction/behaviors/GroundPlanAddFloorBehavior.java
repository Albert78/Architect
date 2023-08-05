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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controller.UiController.DockConflictStrategy;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractAnchoredObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.FloorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.WallConstructionRepresentation;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.GroundPlanLowerUIElementFilter;
import de.dh.cad.architect.ui.view.construction.feedback.AncillaryPosition;
import de.dh.cad.architect.ui.view.construction.feedback.polygon.AddPolylineShapeVisualFeedbackManager;

/**
 * Behavior to create floors.
 */
public class GroundPlanAddFloorBehavior extends AbstractConstructionSelectAnchorOrPositionBehavior {
    protected static Length DEFAULT_HEIGHT = Length.ofCM(0);

    // TODO: Make those editable in toolbar controls for StartWallBehavior and AddWallBehavior
    protected int mLevel = 0;
    protected Length mHeight = DEFAULT_HEIGHT;

    protected AddPolylineShapeVisualFeedbackManager mFeedbackManager = null; // Lives from install() to uninstall()

    protected final List<AncillaryPosition> mPositions;

    public GroundPlanAddFloorBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        this(parentMode, Collections.emptyList());
    }

    public GroundPlanAddFloorBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode, List<AncillaryPosition> previousPositions) {
        super(parentMode);
        mPositions = new ArrayList<>(previousPositions);
    }

    @Override
    protected void initializeActions() {
        mActionsList.add(createCancelBehaviorAction(Strings.ACTION_GROUND_PLAN_CANCEL_ADD_FLOOR_TITLE));
    }

    @Override
    protected void initializeUiElementFilter() {
        setUIElementFilter(new GroundPlanLowerUIElementFilter());
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        // No actions for this behavior yet
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return false;
    }

    @Override
    protected void setDefaultUserHint() {
        setUserHint(Strings.GROUND_PLAN_ADD_FLOOR_BEHAVIOR_DEFAULT_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.GROUND_PLAN_ADD_FLOOR_BEHAVIOR_TITLE;
    }

    @Override
    protected void showPositionFeedback(Position2D pos) {
        setUserHint(MessageFormat.format(Strings.GROUND_PLAN_ADD_FLOOR_BEHAVIOR_POSITION_AIMED, pos.axesAndCoordsToString()));
        updateGhostFloorAndFeedback(pos, Optional.empty());
    }

    @Override
    protected void showAnchorFeedback(AnchorConstructionRepresentation dockAnchorRepr) {
        if (dockAnchorRepr == null) {
            setDefaultUserHint();
        } else {
            Anchor dockAnchor = dockAnchorRepr.getAnchor();
            setUserHint(MessageFormat.format(Strings.GROUND_PLAN_ADD_FLOOR_BEHAVIOR_ANCHOR_AIMED, BaseObjectUIRepresentation.getObjName(dockAnchor.getAnchorOwner())));
            updateGhostFloorAndFeedback(dockAnchor.getPosition().projectionXY(), Optional.of(dockAnchor));
        }
    }

    protected void commitFloor() {
        if (mPositions.size() < 3) {
            setUserHint(Strings.GROUND_PLAN_ADD_FLOOR_BEHAVIOR_TOO_FEW_POSITIONS);
            mParentMode.resetBehavior();
            return;
        }
        List<IModelChange> changeTrace = new ArrayList<>();
        Floor floor = Floor.create(mLevel, mHeight, null,
            mPositions
                .stream()
                .map(afp -> afp.getPosition())
                .collect(Collectors.toList()),
            getPlan(), changeTrace);
        UiController uiController = getUiController();
        List<Anchor> edgeHandleAnchors = floor.getEdgeHandleAnchors();
        for (int i = 0; i < mPositions.size(); i++) {
            AncillaryPosition ap = mPositions.get(i);
            int currentI = i;
            ap.getOAnchor().ifPresent(apAnchor -> {
                Anchor floorAnchor = edgeHandleAnchors.get(currentI);
                uiController.doDock(floorAnchor, apAnchor, DockConflictStrategy.SkipDock, changeTrace);
            });
        }
        uiController.notifyChange(changeTrace, Strings.FLOOR_ADD_CHANGE);
        mParentMode.resetBehavior();
    }

    @Override
    protected void triggerPoint(Position2D endPos) {
        AncillaryPosition newPosition = new AncillaryPosition(endPos, Optional.empty());
        if (mPositions.contains(newPosition)) {
            return;
        }
        mPositions.add(newPosition);
        mFeedbackManager.updateVisualObjects(mPositions);
    }

    @Override
    protected void triggerAnchor(AnchorConstructionRepresentation dockAnchorRepr) {
        Anchor dockAnchor = dockAnchorRepr.getAnchor();
        AncillaryPosition newPosition = new AncillaryPosition(dockAnchor.projectionXY(), Optional.of(dockAnchor));
        if (mPositions.contains(newPosition)) {
            return;
        }
        mPositions.add(newPosition);
        mFeedbackManager.updateVisualObjects(mPositions);
    }

    @Override
    protected void secondaryButtonClicked() {
        commitFloor();
    }

    protected List<AncillaryPosition> createPositions(Position2D endPosition, Optional<Anchor> oEndDockAnchor) {
        List<AncillaryPosition> result = new ArrayList<>(mPositions);
        AncillaryPosition newPosition = new AncillaryPosition(endPosition, oEndDockAnchor);
        if (!result.contains(newPosition)) {
            result.add(newPosition);
        }
        return result;
    }

    protected void updateGhostFloorAndFeedback(Position2D endPosition, Optional<Anchor> oEndDockAnchor) {
        List<AncillaryPosition> positions = createPositions(endPosition, oEndDockAnchor);
        mFeedbackManager.updateVisualObjects(positions);
    }

    @Override
    protected boolean isPossibleDockTargetAnchor(Anchor anchor) {
        if (isPossibleDockTargetAnchorForFloor(anchor)) {
            return mPositions
                    .stream()
                    .filter(afp -> afp.getPosition().equals(anchor.getPosition().projectionXY()))
                    .findAny().isEmpty();
        }
        return false;
    }

    @Override
    protected AbstractAnchoredObjectConstructionRepresentation getDockTargetElementIfSupported(Abstract2DRepresentation repr) {
        return getDockElementForFloorIfSupported(repr);
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);

        ConstructionView constructionView = getView();
        mFeedbackManager = new AddPolylineShapeVisualFeedbackManager(constructionView);
        mFeedbackManager.updateVisualObjects(mPositions);
    }

    @Override
    public void uninstall() {
        mFeedbackManager.removeVisualObjects();

        super.uninstall();
    }

    public static boolean isPossibleDockTargetAnchorForFloor(Anchor anchor) {
        return Wall.isLowerWallCornerAnchor(anchor) || Floor.isEdgeHandleAnchor(anchor);
    }

    public static AbstractAnchoredObjectConstructionRepresentation getDockElementForFloorIfSupported(Abstract2DRepresentation repr) {
        if (repr instanceof WallConstructionRepresentation || repr instanceof FloorConstructionRepresentation) {
            return (AbstractAnchoredObjectConstructionRepresentation) repr;
        }
        return null;
    }
}
