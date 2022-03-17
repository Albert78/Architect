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
package de.dh.cad.architect.ui.view.construction.feedback.wall.endings;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWall;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWallAnchor;
import de.dh.cad.architect.ui.view.construction.feedback.wall.AncillaryWallsModel;
import de.dh.cad.architect.ui.view.construction.feedback.wall.PrincipalWallAncillaryWallsModel;
import de.dh.cad.architect.utils.IdGenerator;

/**
 * Represents a wall end at an arbitrary position without connection to existing walls.
 */
public class UnconnectedWallEnding extends AbstractWallEnding {
    protected static final String CORNER_WALL_ID_A_CW = "Corner-A-CW";
    protected static final String CORNER_WALL_ID_A_CCW = "Corner-A-CCW";
    protected static final String CORNER_WALL_ID_B_CW = "Corner-B-CW";
    protected static final String CORNER_WALL_ID_B_CCW = "Corner-B-CCW";

    protected static final Length CORNER_WALL_LENGTH = Length.ofCM(50);

    protected final Position2D mPosition;
    protected boolean mDrawWallCW = false;
    protected boolean mDrawWallCCW = false;
    protected Length mWallThickness = Length.ofCM(30);

    public UnconnectedWallEnding(Position2D position) {
        mPosition = position;
    }

    @Override
    public Position2D getPosition() {
        return mPosition;
    }

    public boolean isDrawWallCW() {
        return mDrawWallCW;
    }

    public void setDrawWallCW(boolean value) {
        mDrawWallCW = value;
    }

    public boolean isDrawWallCCW() {
        return mDrawWallCCW;
    }

    public void setDrawWallCCW(boolean value) {
        mDrawWallCCW = value;
    }

    public Length getWallThickness() {
        return mWallThickness;
    }

    public void setWallThickness(Length value) {
        mWallThickness = value;
    }

    protected SimpleWallEndConfiguration tryGetCompatibleEndConfiguration(AbstractWallEndConfiguration wallEndConfiguration) {
        if (!(wallEndConfiguration instanceof SimpleWallEndConfiguration)) {
            return null;
        }
        AbstractWallEnding wallEndData = wallEndConfiguration.getWallEnding();
        if (wallEndData instanceof UnconnectedWallEnding uwe) {
            if (uwe.isDrawWallCW() != isDrawWallCW() || uwe.isDrawWallCCW() != isDrawWallCCW()) {
                return null;
            }
        } else {
            return null;
        }
        return (SimpleWallEndConfiguration) wallEndConfiguration;
    }

    @Override
    public Optional<Anchor> getOConnectedAnchor() {
        return Optional.empty();
    }

    @Override
    public Optional<Wall> getOConnectedWall() {
        return Optional.empty();
    }

    protected void createAncillaryCornerWall(String wallId, AncillaryWallAnchor cornerWallAnchor, WallBevelType wallBevelCornerWalls, AncillaryWallsModel ancillaryWallsModel) {
        Map<String, AncillaryWall> wallsById = ancillaryWallsModel.getWallsById();
        Map<String, AncillaryWallAnchor> anchorsById = ancillaryWallsModel.getAnchorsById();
        AncillaryWall cornerWall = AncillaryWall.create(wallId,
            IdGenerator.generateUniqueId(), IdGenerator.generateUniqueId(), wallsById, anchorsById);
        AncillaryWallAnchor cornerWallHandleA = cornerWall.getAnchorWallHandleA();
        AncillaryWallAnchor cornerWallHandleB = cornerWall.getAnchorWallHandleB();
        List<IWallAnchor> dockedAnchors = cornerWallAnchor.getAllDockedAnchors();
        cornerWallHandleA.setDockedAnchors(dockedAnchors);
        dockedAnchors.add(cornerWallHandleA);
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(cornerWallHandleA, wallBevelCornerWalls);
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(cornerWallHandleB, wallBevelCornerWalls);
    }

    protected void createCornerWalls(AncillaryWallAnchor virtualWallHandleAnchor, String cornerWallCWId, String cornerWallCCWId, PrincipalWallAncillaryWallsModel ancillaryWallsModel) {
        if (isDrawWallCW()) {
            createAncillaryCornerWall(cornerWallCWId, virtualWallHandleAnchor, WallBevelType.DEFAULT, ancillaryWallsModel);
        }
        if (isDrawWallCCW()) {
            createAncillaryCornerWall(cornerWallCCWId, virtualWallHandleAnchor, WallBevelType.DEFAULT, ancillaryWallsModel);
        }

        virtualWallHandleAnchor.getAllDockedAnchors().add(virtualWallHandleAnchor);
    }

    @Override
    public AbstractWallEndConfiguration configureWallStart(PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel, boolean openWallEnd) {
        SimpleWallEndConfiguration result = new SimpleWallEndConfiguration(this, ancillaryWallsModel, wallBevel, openWallEnd);
        AncillaryWallAnchor startHandle = ancillaryWallsModel.getPrincipalWallStartAnchor();
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(startHandle, wallBevel);
        createCornerWalls(startHandle, CORNER_WALL_ID_A_CW, CORNER_WALL_ID_A_CCW, ancillaryWallsModel);
        return result;
    }

    protected static void updateCornerWallIfPresent(AncillaryWall cornerWall, Position2D startPosition, Position2D endPosition, Length wallThickness) {
        if (cornerWall != null) {
            AncillaryWallAnchor cornerWallHandleA = cornerWall.getAnchorWallHandleA();
            cornerWallHandleA.setPosition(startPosition);
            AncillaryWallAnchor cornerWallHandleB = cornerWall.getAnchorWallHandleB();
            cornerWallHandleB.setPosition(endPosition);
            cornerWall.setThickness(wallThickness);
        }
    }

    protected static void updateCornerWalls(AncillaryWallAnchor cornerWallAnchor, Position2D wallOtherEnd, String cornerWallCWId, String cornerWallCCWId, Length wallThickness,
        PrincipalWallAncillaryWallsModel ancillaryWallsModel) {

        // Mirroring Y is necessary for weird JavaFX coordinate system
        Vector2D vectorAB = cornerWallAnchor.getPosition().minus(wallOtherEnd).mirrorY();
        Vector2D normalCW = vectorAB.getNormalCW().scaleToLength(CORNER_WALL_LENGTH).mirrorY();

        Position2D cornerWallsDockPosition = cornerWallAnchor.getPosition();
        updateCornerWallIfPresent(ancillaryWallsModel.getWallsById().get(cornerWallCWId), cornerWallsDockPosition, cornerWallsDockPosition.plus(normalCW), wallThickness);
        updateCornerWallIfPresent(ancillaryWallsModel.getWallsById().get(cornerWallCCWId), cornerWallsDockPosition, cornerWallsDockPosition.minus(normalCW), wallThickness);
    }

    @Override
    public boolean tryUpdateWallStart(AbstractWallEndConfiguration wallEndConfiguration, Position2D wallEndPosition) {
        SimpleWallEndConfiguration wec = tryGetCompatibleEndConfiguration(wallEndConfiguration);
        if (wec == null) {
            return false;
        }
        updateWallStart(wec, wallEndPosition);
        return true;
    }

    public void updateWallStart(SimpleWallEndConfiguration wallEndConfiguration, Position2D wallEndPosition) {
        PrincipalWallAncillaryWallsModel ancillaryWallsModel = wallEndConfiguration.getAncillaryWallsModel();
        AncillaryWallAnchor startHandle = ancillaryWallsModel.getPrincipalWallStartAnchor();
        startHandle.setPosition(mPosition);
        if (wallEndConfiguration.isOpenWallEnd()) {
            wallEndConfiguration.configureOpenWallEnd(WallDirection.E);
            ancillaryWallsModel.setWallAnchorForMovingHandleFeedback(startHandle);
        }
        updateCornerWalls(startHandle, wallEndPosition, CORNER_WALL_ID_A_CW, CORNER_WALL_ID_A_CCW, getWallThickness(), ancillaryWallsModel);
        wallEndConfiguration.setWallEnding(this);
    }

    @Override
    public AbstractWallEndConfiguration configureWallEnd(PrincipalWallAncillaryWallsModel ancillaryWallsModel, WallBevelType wallBevel) {
        SimpleWallEndConfiguration result = new SimpleWallEndConfiguration(this, ancillaryWallsModel, wallBevel, false);
        AncillaryWallAnchor endHandle = ancillaryWallsModel.getPrincipalWallEndAnchor();
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(endHandle, wallBevel);
        createCornerWalls(endHandle, CORNER_WALL_ID_B_CW, CORNER_WALL_ID_B_CCW, ancillaryWallsModel);
        return result;
    }

    @Override
    public boolean tryUpdateWallEnd(AbstractWallEndConfiguration wallEndConfiguration, Position2D wallStartPosition) {
        SimpleWallEndConfiguration wec = tryGetCompatibleEndConfiguration(wallEndConfiguration);
        if (wec == null) {
            return false;
        }
        updateWallEnd(wec, wallStartPosition);
        return true;
    }

    public void updateWallEnd(SimpleWallEndConfiguration wallEndConfiguration, Position2D wallStartPosition) {
        PrincipalWallAncillaryWallsModel ancillaryWallsModel = wallEndConfiguration.getAncillaryWallsModel();
        AncillaryWallAnchor endHandle = ancillaryWallsModel.getPrincipalWallEndAnchor();
        endHandle.setPosition(mPosition);
        updateCornerWalls(endHandle, wallStartPosition, CORNER_WALL_ID_B_CW, CORNER_WALL_ID_B_CCW, getWallThickness(), ancillaryWallsModel);
        wallEndConfiguration.setWallEnding(this);
    }

    @Override
    protected void configureFinalWall(AbstractWallEndConfiguration wallEndConfiguration, Wall wall, Anchor wallEndHandle,
        UiController uiController, ChangeSet changeSet) {
        uiController.doSetHandleAnchorPosition(wallEndHandle, mPosition, changeSet);
    }

    @Override
    public String toString() {
        return "Unconnected wall end at " + mPosition;
    }
}
