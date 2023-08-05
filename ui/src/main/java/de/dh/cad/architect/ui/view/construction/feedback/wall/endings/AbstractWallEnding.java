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
package de.dh.cad.architect.ui.view.construction.feedback.wall.endings;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Angle;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controller.UiController.DockConflictStrategy;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWallAnchor;
import de.dh.cad.architect.ui.view.construction.feedback.wall.PrincipalWallAncillaryWallsModel;

/**
 * Represents the target situation how a new wall's end should be integrated into the given
 * real object's situation.
 * This includes the information whether the new wall's end should it be docked to an existing anchor
 * or whether an existing wall should be broken into two parts, docking the new wall end to the bend point.
 */
public abstract class AbstractWallEnding {
    public abstract static class AbstractWallEndConfiguration {
        /**
         * Length of an ancillary wall representing a new wall's start.
         */
        protected static final Length SINGLE_WALL_LENGTH = Length.ofM(1);

        protected AbstractWallEnding mWallEnding;
        protected final PrincipalWallAncillaryWallsModel mAncillaryWallsModel;
        protected final WallBevelType mWallBevel;
        protected final boolean mOpenWallEnd;

        public AbstractWallEndConfiguration(AbstractWallEnding wallEndData, PrincipalWallAncillaryWallsModel ancillaryWallsModel,
                WallBevelType wallBevel, boolean openWallEnd) {
            mWallEnding = wallEndData;
            mAncillaryWallsModel = ancillaryWallsModel;
            mWallBevel = wallBevel;
            mOpenWallEnd = openWallEnd;
        }

        public AbstractWallEnding getWallEnding() {
            return mWallEnding;
        }

        public void setWallEnding(AbstractWallEnding value) {
            mWallEnding = value;
        }

        public PrincipalWallAncillaryWallsModel getAncillaryWallsModel() {
            return mAncillaryWallsModel;
        }

        public WallBevelType getWallBevel() {
            return mWallBevel;
        }

        public boolean isOpenWallEnd() {
            return mOpenWallEnd;
        }

        protected void configureOpenWallEnd(WallDirection direction) {
            AncillaryWallAnchor startHandle = mAncillaryWallsModel.getPrincipalWallStartAnchor();
            AncillaryWallAnchor endHandle = mAncillaryWallsModel.getPrincipalWallEndAnchor();
            Position2D startPosition = startHandle.getPosition();
            Position2D pos;
            switch (direction) {
            case N:
                pos = startPosition.movedY(SINGLE_WALL_LENGTH.negated());
                break;
            case E:
                pos = startPosition.movedX(SINGLE_WALL_LENGTH);
                break;
            case S:
                pos = startPosition.movedY(SINGLE_WALL_LENGTH);
                break;
            case W:
                pos = startPosition.movedX(SINGLE_WALL_LENGTH.negated());
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + direction);
            }
            endHandle.setPosition(pos);
        }

        public void configureFinalWall(Wall wall, Anchor wallEndHandle, UiController uiController, DockConflictStrategy dockConflictStrategy, List<IModelChange> changeTrace) {
            uiController.doSetWallBevelTypeOfAnchorDock(wallEndHandle, mWallBevel, changeTrace);
            mWallEnding.configureFinalWall(this, wall, wallEndHandle, uiController, dockConflictStrategy, changeTrace);
        }
    }

    protected static class SimpleWallEndConfiguration extends AbstractWallEndConfiguration {
        public SimpleWallEndConfiguration(AbstractWallEnding wallEndData, PrincipalWallAncillaryWallsModel ancillaryWallsModel,
                WallBevelType wallBevel, boolean openWallEnd) {
            super(wallEndData, ancillaryWallsModel, wallBevel, openWallEnd);
        }
    }

    public abstract Position2D getPosition();

    /**
     * Optionally returns the (existing) model anchor to which the new wall end should be docked.
     */
    public abstract Optional<Anchor> getOConnectedAnchor();

    /**
     * Optionally returns the (existing) model wall to which the new wall end should be docked.
     */
    public abstract Optional<Wall> getOConnectedWall();

    public abstract AbstractWallEndConfiguration configureWallStart(PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel, boolean openWallEnd);
    public abstract boolean tryUpdateWallStart(AbstractWallEndConfiguration wallEndConfiguration, Position2D wallEndPosition);

    public abstract AbstractWallEndConfiguration configureWallEnd(PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel);
    public abstract boolean tryUpdateWallEnd(AbstractWallEndConfiguration wallEndConfiguration, Position2D wallStartPosition);

    protected abstract void configureFinalWall(AbstractWallEndConfiguration abstractWallEndConfiguration,
        Wall wall, Anchor wallEndHandle, UiController uiController, DockConflictStrategy dockConflictStrategy, List<IModelChange> changeTrace);

    protected static WallDirection getWallDirection(IWallAnchor startHandle) {
        // Calculate the direction of the wall
        IWall wall = startHandle.getOwner();
        WallDockEnd otherSide = startHandle.getHandleAnchorDockEnd().map(de -> de.opposite()).orElse(null);
        if (otherSide == null) {
            // Should not happen
            return WallDirection.W;
        }
        Vector2D vWall = Vector2D.between(
            startHandle.getPosition(),
            wall.getAnchorWallHandle(otherSide).getPosition());
        double angle = Angle.angleBetween(vWall, Vector2D.X1M).getAngleDeg();
        if (Math.abs(angle - 90) <= 45) {
            return WallDirection.N;
        } else if (Math.abs(angle - 180) <= 45) {
            return WallDirection.W;
        } else if (Math.abs(angle - 270) <= 45) {
            return WallDirection.S;
        } else {
            return WallDirection.E;
        }
    }

    protected static WallDirection getBestNewWallDirection(Collection<IWallAnchor> existingDockParticipants) {
        // All directions...
        Set<WallDirection> possibleDirections = new HashSet<>(Arrays.asList(WallDirection.E, WallDirection.S, WallDirection.W, WallDirection.N));
        for (IWallAnchor anchor : existingDockParticipants) {
            // ... remove rough directions of existing walls
            possibleDirections.remove(getWallDirection(anchor));
        }
        // ... remaining directions are "free", so take first of them as possible direction for new wall
        Iterator<WallDirection> iFreeDirections = possibleDirections.iterator();
        return iFreeDirections.hasNext() ? iFreeDirections.next() : WallDirection.E;
    }
}
