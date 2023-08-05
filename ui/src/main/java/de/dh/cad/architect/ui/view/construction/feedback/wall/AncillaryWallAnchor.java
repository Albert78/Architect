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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;

public class AncillaryWallAnchor implements IWallAnchor {
    protected final String mId;
    protected final AncillaryWall mOwner;
    protected final WallDockEnd mWallDockEnd;

    protected final Map<String, AncillaryWallAnchor> mWallsById;
    protected final Map<String, AncillaryWallAnchor> mAnchorsById;

    protected Position2D mPosition = Position2D.zero();
    protected List<IWallAnchor> mDockedAnchors = new ArrayList<>();
    protected boolean mReferenceAnchor = false;

    public AncillaryWallAnchor(String id, WallDockEnd wallDockEnd, AncillaryWall owner, Map<String, AncillaryWallAnchor> wallsById, Map<String, AncillaryWallAnchor> anchorsById) {
        mId = id;
        mWallDockEnd = wallDockEnd;
        mOwner = owner;
        mWallsById = wallsById;
        mAnchorsById = anchorsById;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public AncillaryWall getOwner() {
        return mOwner;
    }

    @Override
    public Position2D getPosition() {
        return mPosition;
    }

    public void setPosition(Position2D value) {
        mPosition = value;
    }

    @Override
    public boolean isReferenceAnchor() {
        return mReferenceAnchor;
    }

    public void setReferenceAnchor(boolean value) {
        mReferenceAnchor = value;
    }

    @Override
    public List<IWallAnchor> getAllDockedAnchors() {
        return mDockedAnchors;
    }

    // For simplicity reasons, we hold the same collection in each dock participant
    public void setDockedAnchors(List<IWallAnchor> value) {
        mDockedAnchors = value;
    }

    @Override
    public Optional<WallDockEnd> getHandleAnchorDockEnd() {
        return Optional.of(mWallDockEnd);
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IWallAnchor other = (IWallAnchor) obj;
        return Objects.equals(mId, other.getId());
    }

    @Override
    public String toString() {
        return "Ancillary wall anchor " + mId + " at [" + mPosition.axesAndCoordsToString() + "]";
    }
}
