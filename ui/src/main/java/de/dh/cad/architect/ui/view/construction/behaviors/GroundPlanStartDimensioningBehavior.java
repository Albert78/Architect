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
import java.util.List;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractAnchoredObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.construction.GroundPlanUIElementFilter;

/**
 * First part of the two-part behavior to create dimensionings. Second part is {@link GroundPlanAddDimensioningBehavior}.
 */
public class GroundPlanStartDimensioningBehavior extends AbstractConstructionSelectAnchorOrPositionBehavior {
    public GroundPlanStartDimensioningBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
    }

    @Override
    protected void initializeActions() {
        mActionsList.add(createCancelBehaviorAction(Strings.ACTION_GROUND_PLAN_CANCEL_CREATE_DIMENSIONING_TITLE));
    }

    @Override
    protected void initializeUiElementFilter() {
        setUIElementFilter(new GroundPlanUIElementFilter());
    }

    @Override
    protected void setDefaultUserHint() {
        setUserHint(Strings.GROUND_PLAN_START_DIMENSIONING_BEHAVIOR_DEFAULT_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.GROUND_PLAN_START_DIMENSIONING_BEHAVIOR_TITLE;
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
        setDefaultUserHint();
    }

    @Override
    protected void showAnchorFeedback(AnchorConstructionRepresentation anchorRepr) {
        if (anchorRepr == null) {
            setDefaultUserHint();
        } else {
            Anchor anchor = anchorRepr.getAnchor();
            setUserHint(MessageFormat.format(Strings.GROUND_PLAN_START_DIMENSIONING_BEHAVIOR_ANCHOR_AIMED, BaseObjectUIRepresentation.getObjName(anchor.getAnchorOwner())));
        }
    }

    @Override
    protected void triggerPoint(Position2D pos) {
        GroundPlanAddDimensioningBehavior behavior = GroundPlanAddDimensioningBehavior.startingAtPosition(pos, mParentMode);
        mParentMode.setBehavior(behavior);
    }


    @Override
    protected void triggerAnchor(AnchorConstructionRepresentation anchorRepr) {
        Anchor anchor = anchorRepr.getAnchor();
        GroundPlanAddDimensioningBehavior behavior = GroundPlanAddDimensioningBehavior.startingAtDockAnchor(anchor, mParentMode);
        mParentMode.setBehavior(behavior);
    }

    @Override
    protected boolean isPossibleDockTargetAnchor(Anchor anchor) {
        return isPossibleDockTargetAnchorForDimensioning(anchor);
    }

    @Override
    protected AbstractAnchoredObjectConstructionRepresentation getDockTargetElementIfSupported(Abstract2DRepresentation repr) {
        return getDockElementForDimensioningIfSupported(repr);
    }

    public static boolean isPossibleDockTargetAnchorForDimensioning(Anchor anchor) {
        return anchor.getAnchorOwner().isPossibleDimensioningDockTargetAnchor(anchor);
    }

    public static AbstractAnchoredObjectConstructionRepresentation getDockElementForDimensioningIfSupported(Abstract2DRepresentation repr) {
        if (repr instanceof AbstractAnchoredObjectConstructionRepresentation aocr) {
            return aocr;
        }
        return null;
    }
}
