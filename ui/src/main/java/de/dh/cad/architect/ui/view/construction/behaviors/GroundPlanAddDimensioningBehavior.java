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
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Dimensioning;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controller.UiController.DockConflictStrategy;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractAnchoredObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.DimensioningAncillary;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.GroundPlanUIElementFilter;

/**
 * Second part of the two-part behavior to create dimensionings. First part is {@link GroundPlanStartDimensioningBehavior}.
 */
public class GroundPlanAddDimensioningBehavior extends AbstractConstructionSelectSecondAnchorOrPositionBehavior {
    protected static final double DIMENSIONING_LABEL_DISTANCE = 40;

    protected DimensioningAncillary mFeedback = null;

    public GroundPlanAddDimensioningBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode, Optional<Anchor> oStartDockAnchor, Optional<Position2D> startPosition) {
        super(parentMode, oStartDockAnchor, startPosition);
    }

    public static GroundPlanAddDimensioningBehavior startingAtDockAnchor(Anchor startDockAnchor, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        GroundPlanAddDimensioningBehavior result = new GroundPlanAddDimensioningBehavior(parentMode, Optional.of(startDockAnchor), Optional.empty());
        return result;
    }

    public static GroundPlanAddDimensioningBehavior startingAtPosition(Position2D position, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        GroundPlanAddDimensioningBehavior result = new GroundPlanAddDimensioningBehavior(parentMode, Optional.empty(), Optional.of(position));
        return result;
    }

    @Override
    protected void initializeActions() {
        mActionsList.add(createCancelBehaviorAction(Strings.ACTION_GROUND_PLAN_CANCEL_ADD_DIMENSIONING_TITLE));
    }

    @Override
    protected void initializeUiElementFilter() {
        setUIElementFilter(new GroundPlanUIElementFilter());
    }

    @Override
    public void setDefaultUserHint() {
        setUserHint(Strings.GROUND_PLAN_ADD_DIMENSIONING_BEHAVIOR_DEFAULT_USER_HINT);
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
    public String getTitle() {
        return Strings.GROUND_PLAN_ADD_DIMENSIONING_BEHAVIOR_TITLE;
    }

    @Override
    protected void showPositionFeedback(Position2D pos) {
        setDefaultUserHint();
        if (pos == null) {
            removeFeedback();
        } else {
            updateFeedback(pos);
        }
    }

    @Override
    protected void showAnchorFeedback(AnchorConstructionRepresentation dockAnchorRepr) {
        if (dockAnchorRepr == null) {
            setDefaultUserHint();
            removeFeedback();
        } else {
            Anchor dockAnchor = dockAnchorRepr.getAnchor();
            setUserHint(MessageFormat.format(Strings.GROUND_PLAN_ADD_DIMENSIONING_BEHAVIOR_ANCHOR_AIMED, BaseObjectUIRepresentation.getObjName(dockAnchor.getAnchorOwner())));
            updateFeedback(dockAnchor);
        }
    }

    protected void commitDimensioning(
        Optional<Position2D> oEndPosition, Optional<Anchor> oEndDockAnchor) {
        List<IModelChange> changeTrace = new ArrayList<>();
        Position2D startPosition = mOStartDockAnchor.map(a -> a.getPosition().projectionXY()).or(() -> mOStartPosition).get();
        Position2D endPosition = oEndDockAnchor.map(a -> a.getPosition().projectionXY()).or(() -> oEndPosition).get();
        Dimensioning dimensioning = Dimensioning.create(
            BaseObjectUIRepresentation.generateSimpleName(getPlan().getDimensionings().values(), Dimensioning.class),
            startPosition, endPosition, mFeedback.getCalculatedLabelDistance(), getPlan(), changeTrace);
        UiController uiController = getUiController();
        mOStartDockAnchor.ifPresent(startDockAnchor -> {
            uiController.doDock(dimensioning.getAnchor1(), startDockAnchor, DockConflictStrategy.SkipDock, changeTrace);
        });
        oEndDockAnchor.ifPresent(endDockAnchor -> {
            uiController.doDock(dimensioning.getAnchor2(), endDockAnchor, DockConflictStrategy.SkipDock, changeTrace);
        });
        uiController.notifyChange(changeTrace, Strings.DIMENSIONING_ADD_CHANGE);
        mParentMode.resetBehavior();
    }

    @Override
    protected void triggerPoint(Position2D endPos) {
        commitDimensioning(Optional.of(endPos), Optional.empty());
    }

    @Override
    protected void triggerAnchor(AnchorConstructionRepresentation dockAnchorRepr) {
        commitDimensioning(Optional.empty(), Optional.of(dockAnchorRepr.getAnchor()));
    }

    protected void removeFeedback() {
        if (mFeedback != null) {
            ConstructionView view = getView();
            view.removeAncillaryObject(mFeedback.getAncillaryObjectId());
            mFeedback = null;
        }
    }

    protected void updateFeedback(Position2D endPosition, boolean valid) {
        if (mFeedback == null) {
            ConstructionView view = getView();
            mFeedback = new DimensioningAncillary(view);
            view.addAncillaryObject(mFeedback);
        }
        mFeedback.setProperties(getStartPosition(), endPosition, 0, DIMENSIONING_LABEL_DISTANCE, valid);
    }

    protected void updateFeedback(Position2D endPositionWithoutAnchor) {
        updateFeedback(endPositionWithoutAnchor, false);
    }

    protected void updateFeedback(Anchor endDockAnchor) {
        updateFeedback(endDockAnchor.getPosition().projectionXY(), true);
    }

    @Override
    protected boolean isPossibleDockTargetAnchor(Anchor anchor) {
        return mOStartDockAnchor.map(startDockAnchor ->
            GroundPlanStartDimensioningBehavior.isPossibleDockTargetAnchorForDimensioning(anchor) && !anchor.equals(startDockAnchor)).orElse(false);
    }

    @Override
    protected AbstractAnchoredObjectConstructionRepresentation getDockTargetElementIfSupported(Abstract2DRepresentation repr) {
        return GroundPlanStartDimensioningBehavior.getDockElementForDimensioningIfSupported(repr);
    }

    @Override
    public void uninstall() {
        removeFeedback();

        super.uninstall();
    }
}
