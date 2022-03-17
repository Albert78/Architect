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
import java.util.Collection;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractAnchoredObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.FloorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.WallConstructionRepresentation;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.AbstractWallEnding;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.AbstractWallEnding.AbstractWallEndConfiguration;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.DockedWallEnding;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.UnconnectedWallEnding;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.WallBreakPointWallEnding;

/**
 * First part of the two-part behavior to create walls. Second part is {@link GroundPlanAddWallBehavior}.
 */
public abstract class AbstractGroundPlanCreateWallBehavior extends AbstractConstructionSelectAnchorOrPositionBehavior {
    protected AbstractGroundPlanCreateWallBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return false;
    }

    @Override
    protected void secondaryButtonClicked() {
        mParentMode.resetBehavior();
    }

    protected UnconnectedWallEnding createUnconnectedWallEnd(Position2D position, boolean isStart) {
        return new UnconnectedWallEnding(position);
    }

    protected AbstractWallEnding findNewWallEnding(Position2D position, Optional<Anchor> oDockAnchor, boolean isStart,
        AbstractWallEndConfiguration oldWallEndConfiguration, Collection<Wall> forbiddenDockWalls, Collection<Anchor> forbiddenDockAnchors) {
        AbstractWallEnding result;
        if (oDockAnchor.isPresent()) {
            Anchor startDockAnchor = oDockAnchor.get();
            if (!forbiddenDockAnchors.contains(startDockAnchor)) {
                result = new DockedWallEnding(startDockAnchor);
                setUserHint(MessageFormat.format(Strings.GROUND_PLAN_CREATE_WALL_BEHAVIOR_ANCHOR_AIMED,
                        BaseObjectUIRepresentation.getObjName(startDockAnchor.getAnchorOwner())));
                return result;
            }
        } else {
            // Start position is not an existing anchor, check if we're over an existing wall we can connect to

            // First check current wall, if we had already a wall as break point wall
            Optional<WallBreakPointWallEnding> oWallBreakPoint = Optional.empty();
            AbstractWallEnding oldWallEndData = oldWallEndConfiguration == null ? null : oldWallEndConfiguration.getWallEnding();
            if (oldWallEndData instanceof WallBreakPointWallEnding oldWBPWE) {
                oWallBreakPoint = WallBreakPointWallEnding.tryFindWallBreak(oldWBPWE.getConnectWall(), position);
            }
            // Else check all walls
            if (oWallBreakPoint.isEmpty()) {
                Collection<Wall> possibleWalls = new ArrayList<>(getView().getPlan().getWalls().values());
                possibleWalls.removeAll(forbiddenDockWalls);
                oWallBreakPoint = WallBreakPointWallEnding.tryFindNearWallBreak(position, possibleWalls);
            }
            if (oWallBreakPoint.isPresent()) {
                result = oWallBreakPoint.get();
                setUserHint(Strings.GROUND_PLAN_CREATE_WALL_BEHAVIOR_BREAK_WALL);
                return result;
            }
        }
        result = createUnconnectedWallEnd(position, isStart);
        setUserHint(MessageFormat.format(Strings.GROUND_PLAN_CREATE_WALL_BEHAVIOR_START_POSITION_AIMED, position.axesAndCoordsToString()));
        return result;
    }

    @Override
    protected boolean isPossibleDockTargetAnchor(Anchor anchor) {
        return isPossibleDockTargetAnchorForWall(anchor);
    }

    @Override
    protected AbstractAnchoredObjectConstructionRepresentation getDockTargetElementIfSupported(Abstract2DRepresentation repr) {
        return getDockElementForWallIfSupported(repr);
    }

    public static boolean isPossibleDockTargetAnchorForWall(Anchor anchor) {
        return Wall.isWallHandleAnchor(anchor) || Floor.isEdgeHandleAnchor(anchor);
    }

    public static AbstractAnchoredObjectConstructionRepresentation getDockElementForWallIfSupported(Abstract2DRepresentation repr) {
        if (repr instanceof WallConstructionRepresentation || repr instanceof FloorConstructionRepresentation) {
            return (AbstractAnchoredObjectConstructionRepresentation) repr;
        }
        return null;
    }
}
