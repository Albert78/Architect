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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controller.UiController.DockConflictStrategy;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWallAnchor;
import de.dh.cad.architect.ui.view.construction.feedback.wall.PrincipalWallAncillaryWallsModel;

public class DockedWallEnding extends AbstractWallEnding {
    protected final Anchor mDockedAnchor;

    public DockedWallEnding(Anchor dockedAnchor) {
        mDockedAnchor = dockedAnchor;
    }

    public Anchor getDockedAnchor() {
        return mDockedAnchor;
    }

    @Override
    public Position2D getPosition() {
        return mDockedAnchor.getPosition().projectionXY();
    }

    @Override
    public Optional<Anchor> getOConnectedAnchor() {
        return Optional.of(mDockedAnchor);
    }

    @Override
    public Optional<Wall> getOConnectedWall() {
        return Optional.of((Wall) mDockedAnchor.getAnchorOwner());
    }

    protected SimpleWallEndConfiguration tryGetCompatibleEndConfiguration(AbstractWallEndConfiguration wallEndConfiguration) {
        AbstractWallEnding wallEndData = wallEndConfiguration.getWallEnding();
        if (!(wallEndData instanceof DockedWallEnding) || !(wallEndConfiguration instanceof SimpleWallEndConfiguration)) {
            return null;
        }
        DockedWallEnding oldWE = (DockedWallEnding) wallEndData;
        if (!mDockedAnchor.equals(oldWE.mDockedAnchor)) {
            return null;
        }
        return (SimpleWallEndConfiguration) wallEndConfiguration;
    }

    protected SimpleWallEndConfiguration configureWallEnding(AncillaryWallAnchor handle, PrincipalWallAncillaryWallsModel ancillaryWallsModel,
            WallBevelType wallBevel, boolean openWallEnd) {
        SimpleWallEndConfiguration result = new SimpleWallEndConfiguration(this, ancillaryWallsModel, wallBevel, openWallEnd);
        ancillaryWallsModel.updateVirtualHandle(handle, mDockedAnchor.projectionXY(), wallBevel, Optional.of(mDockedAnchor));
        AncillaryWallAnchor startHandle = result.getAncillaryWallsModel().getPrincipalWallStartAnchor();
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(startHandle, wallBevel, null);
        return result;
    }

    @Override
    public AbstractWallEndConfiguration configureWallStart(PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel, boolean openWallEnd) {
        return configureWallEnding(ancillaryWallsModel.getPrincipalWallStartAnchor(), ancillaryWallsModel, wallBevel, openWallEnd);
    }

    @Override
    public boolean tryUpdateWallStart(AbstractWallEndConfiguration wallEndConfiguration, Position2D wallEndPosition) {
        SimpleWallEndConfiguration wec = tryGetCompatibleEndConfiguration(wallEndConfiguration);
        if (wec == null) {
            return false;
        }
        updateWallStart(wec);
        return true;
    }

    protected List<IWallAnchor> updateEnding(AncillaryWallAnchor handle, SimpleWallEndConfiguration wallEndConfiguration) {
        List<IWallAnchor> otherDockedAnchors = new ArrayList<>(handle.getAllDockedAnchors());
        otherDockedAnchors.remove(handle);

        for (IWallAnchor otherAnchor : otherDockedAnchors) {
            Position2D dockPosition = otherAnchor.getPosition();
            handle.setPosition(dockPosition);
            break;
        }
        wallEndConfiguration.setWallEnding(this);
        return otherDockedAnchors;
    }

    public void updateWallStart(SimpleWallEndConfiguration wallEndConfiguration) {
        AncillaryWallAnchor startHandle = wallEndConfiguration.getAncillaryWallsModel().getPrincipalWallStartAnchor();
        List<IWallAnchor> otherDockedAnchors = updateEnding(startHandle, wallEndConfiguration);
        if (wallEndConfiguration.isOpenWallEnd()) {
            WallDirection direction = getBestNewWallDirection(otherDockedAnchors);
            wallEndConfiguration.configureOpenWallEnd(direction);
        }
    }

    @Override
    public AbstractWallEndConfiguration configureWallEnd(PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel) {
        return configureWallEnding(ancillaryWallsModel.getPrincipalWallEndAnchor(), ancillaryWallsModel, wallBevel, false);
    }

    @Override
    public boolean tryUpdateWallEnd(AbstractWallEndConfiguration wallEndConfiguration, Position2D wallStartPosition) {
        SimpleWallEndConfiguration wec = tryGetCompatibleEndConfiguration(wallEndConfiguration);
        if (wec == null) {
            return false;
        }
        updateWallEnd(wec);
        return true;
    }

    public void updateWallEnd(SimpleWallEndConfiguration wallEndConfiguration) {
        AncillaryWallAnchor endHandle = wallEndConfiguration.getAncillaryWallsModel().getPrincipalWallEndAnchor();
        updateEnding(endHandle, wallEndConfiguration);
    }

    @Override
    protected void configureFinalWall(AbstractWallEndConfiguration wallEndConfiguration, Wall wall, Anchor wallEndHandle,
        UiController uiController, DockConflictStrategy dockConflictStrategy, List<IModelChange> changeTrace) {
        uiController.doDock(wallEndHandle, mDockedAnchor, dockConflictStrategy, changeTrace);
    }

    @Override
    public String toString() {
        return "Docked wall end at <" + mDockedAnchor + ">";
    }
}
