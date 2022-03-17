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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
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
import de.dh.cad.architect.ui.view.construction.GroundPlanUIElementFilter;
import de.dh.cad.architect.ui.view.construction.feedback.AncillaryPosition;
import de.dh.cad.architect.ui.view.construction.feedback.polygon.AddPolylineShapeVisualFeedbackManager;

/**
 * Behavior to create ceilings.
 */
public class GroundPlanAddCeilingBehavior extends AbstractConstructionSelectAnchorOrPositionBehavior {
    protected AddPolylineShapeVisualFeedbackManager mFeedbackManager = null; // Lives from install() to uninstall()

    protected final List<AncillaryPosition> mPositions;

    public GroundPlanAddCeilingBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        this(parentMode, Collections.emptyList());
    }

    public GroundPlanAddCeilingBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode, List<AncillaryPosition> previousPositions) {
        super(parentMode);
        mPositions = new ArrayList<>(previousPositions);
    }

    @Override
    protected void initializeActions() {
        mActionsList.add(createCancelBehaviorAction(Strings.ACTION_GROUND_PLAN_CANCEL_ADD_CEILING_TITLE));
    }

    @Override
    protected void initializeUiElementFilter() {
        setUIElementFilter(new GroundPlanUIElementFilter());
    }

    @Override
    protected void setDefaultUserHint() {
        setUserHint(Strings.GROUND_PLAN_ADD_CEILING_BEHAVIOR_DEFAULT_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.GROUND_PLAN_ADD_CEILING_BEHAVIOR_TITLE;
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
    protected void showPositionFeedback(Position2D pos) {
        showAnchorFeedback(null);
    }

    @Override
    protected void showAnchorFeedback(AnchorConstructionRepresentation dockAnchorRepr) {
        if (dockAnchorRepr == null) {
            setDefaultUserHint();
        } else {
            Anchor dockAnchor = dockAnchorRepr.getAnchor();
            setUserHint(MessageFormat.format(Strings.GROUND_PLAN_ADD_CEILING_BEHAVIOR_ANCHOR_AIMED, BaseObjectUIRepresentation.getObjName(dockAnchor.getAnchorOwner())));
            updateGhostCeilingAndFeedback(dockAnchor);
        }
    }

    protected void commitCeiling(Anchor anchor1, Anchor anchor2, Anchor anchor3) {
        ChangeSet addChangeSet = new ChangeSet();
        Ceiling ceiling = Ceiling.create(null, anchor1.getPosition3D(Length.ZERO), anchor2.getPosition3D(Length.ZERO), anchor3.getPosition3D(Length.ZERO),
            getPlan(), addChangeSet);
        UiController uiController = getUiController();
        uiController.notifyChanges(addChangeSet);
        ChangeSet changeSet = new ChangeSet();
        uiController.doDock(ceiling.getAnchorA(), anchor1, changeSet);
        uiController.doDock(ceiling.getAnchorB(), anchor2, changeSet);
        uiController.doDock(ceiling.getAnchorC(), anchor3, changeSet);
        uiController.notifyChanges(changeSet);
        mParentMode.resetBehavior();
    }

    @Override
    protected void triggerPoint(Position2D endPos) {
        // Nothing to do
    }

    @Override
    protected void triggerAnchor(AnchorConstructionRepresentation dockAnchorRepr) {
        Anchor dockAnchor = dockAnchorRepr.getAnchor();
        AncillaryPosition newPosition = new AncillaryPosition(dockAnchor.projectionXY(), Optional.of(dockAnchor));
        if (mPositions.contains(newPosition)) {
            return;
        }
        mPositions.add(newPosition);
        if (mPositions.size() == 3) {
            commitCeiling(mPositions.get(0).getOAnchor().get(),
                mPositions.get(1).getOAnchor().get(),
                mPositions.get(2).getOAnchor().get());
        }
    }

    protected List<AncillaryPosition> createPositions(Anchor endDockAnchor) {
        List<AncillaryPosition> result = new ArrayList<>(mPositions);
        AncillaryPosition newPosition = new AncillaryPosition(endDockAnchor.projectionXY(), Optional.of(endDockAnchor));
        if (!result.contains(newPosition)) {
            result.add(newPosition);
        }
        return result;
    }

    protected void updateGhostCeilingAndFeedback(Anchor endDockAnchor) {
        List<AncillaryPosition> positions = createPositions(endDockAnchor);
        mFeedbackManager.updateVisualObjects(positions);
    }

    @Override
    protected boolean isPossibleDockTargetAnchor(Anchor anchor) {
        if (isPossibleDockTargetAnchorForCeiling(anchor)) {
            return mPositions
                    .stream()
                    .filter(afp -> afp.getOAnchor().get().equals(anchor))
                    .findAny().isEmpty();
        }
        return false;
    }

    @Override
    protected void secondaryButtonClicked() {
        mParentMode.resetBehavior();
    }

    @Override
    protected AbstractAnchoredObjectConstructionRepresentation getDockTargetElementIfSupported(Abstract2DRepresentation repr) {
        return getDockElementForCeilingIfSupported(repr);
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

    public static boolean isPossibleDockTargetAnchorForCeiling(Anchor anchor) {
        return Wall.isUpperWallCornerAnchor(anchor) || Floor.isEdgeHandleAnchor(anchor);
    }

    public static AbstractAnchoredObjectConstructionRepresentation getDockElementForCeilingIfSupported(Abstract2DRepresentation repr) {
        if (repr instanceof WallConstructionRepresentation || repr instanceof FloorConstructionRepresentation) {
            return (AbstractAnchoredObjectConstructionRepresentation) repr;
        }
        return null;
    }
}
