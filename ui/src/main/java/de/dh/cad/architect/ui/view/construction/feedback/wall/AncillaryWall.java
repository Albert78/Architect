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
package de.dh.cad.architect.ui.view.construction.feedback.wall;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;

public class AncillaryWall implements IWall {
    protected final String mId;
    protected final AncillaryWallAnchor mWallHandleA;
    protected final AncillaryWallAnchor mWallHandleB;

    protected final Map<String, AncillaryWall> mWallsById;
    protected final Map<String, AncillaryWallAnchor> mAnchorsById;

    protected Wall mRealWall = null;

    protected WallBevelType mWallBevelA = WallBevelType.Bevel;
    protected WallBevelType mWallBevelB = WallBevelType.Bevel;
    protected Length mThickness = Length.ofCM(30);

    public AncillaryWall(String id, String handleAId, String handleBId, Map<String, AncillaryWall> wallsById, Map<String, AncillaryWallAnchor> anchorsById) {
        mId = id;
        mWallHandleA = new AncillaryWallAnchor(handleAId, WallDockEnd.A, this, anchorsById, anchorsById);
        mWallHandleB = new AncillaryWallAnchor(handleBId, WallDockEnd.B, this, anchorsById, anchorsById);

        mWallsById = wallsById;
        mAnchorsById = anchorsById;
    }

    public static AncillaryWall create(String id, String handleAId, String handleBId, Map<String, AncillaryWall> wallsById, Map<String, AncillaryWallAnchor> anchorsById) {
        AncillaryWall result = new AncillaryWall(id, handleAId, handleBId, wallsById, anchorsById);
        wallsById.put(result.getId(), result);
        AncillaryWallAnchor anchorWallHandleA = result.getAnchorWallHandleA();
        AncillaryWallAnchor anchorWallHandleB = result.getAnchorWallHandleB();
        anchorsById.put(anchorWallHandleA.getId(), anchorWallHandleA);
        anchorsById.put(anchorWallHandleB.getId(), anchorWallHandleB);
        return result;
    }

    public static AncillaryWall fromWall(Wall wall, Map<String, AncillaryWall> wallsById, Map<String, AncillaryWallAnchor> anchorsById) {
        Anchor handleA = wall.getAnchorWallHandleA();
        Anchor handleB = wall.getAnchorWallHandleB();
        AncillaryWall result = create(wall.getId(), handleA.getId(), handleB.getId(), wallsById, anchorsById);
        result.setRealWall(wall);
        result.getAnchorWallHandleA().setPosition(handleA.getPosition().projectionXY());
        result.getAnchorWallHandleB().setPosition(handleB.getPosition().projectionXY());

        result.setThickness(wall.getThickness());
        result.setWallBevelA(wall.getWallBevelA(), null);
        result.setWallBevelB(wall.getWallBevelB(), null);
        return result;
    }

    public Wall getRealWall() {
        return mRealWall;
    }

    public void setRealWall(Wall value) {
        mRealWall = value;
        mWallHandleA.setReferenceAnchor(true);
        mWallHandleB.setReferenceAnchor(true);
    }

    @Override
    public boolean representsRealWall() {
        return mRealWall != null;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public AncillaryWallAnchor getAnchorWallHandleA() {
        return mWallHandleA;
    }

    @Override
    public AncillaryWallAnchor getAnchorWallHandleB() {
        return mWallHandleB;
    }

    @Override
    public WallBevelType getWallBevelA() {
        return mWallBevelA;
    }

    @Override
    public void setWallBevelA(WallBevelType value, List<IModelChange> changeTrace) {
        mWallBevelA = value;
        // No need to add changes to the change trace - we're no model element
    }

    @Override
    public WallBevelType getWallBevelB() {
        return mWallBevelB;
    }

    @Override
    public void setWallBevelB(WallBevelType value, List<IModelChange> changeTrace) {
        mWallBevelB = value;
        // No need to add changes to the change trace - we're no model element
    }

    @Override
    public Length getThickness() {
        return mThickness;
    }

    public void setThickness(Length value) {
        mThickness = value;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IWall other = (IWall) obj;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public String toString() {
        return "Ancillary wall " + mId + " from [" + mWallHandleA.getPosition().axesAndCoordsToString() + "] to [" + mWallHandleB.getPosition().axesAndCoordsToString() + "]";
    }
}
