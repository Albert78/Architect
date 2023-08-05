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
package de.dh.cad.architect.model.wallmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.dh.cad.architect.model.coords.AnchorTarget;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Wall;

public class AdaptedModelAnchor implements IWallAnchor {
    protected final Anchor mAnchor;
    protected final Optional<Map<String, AnchorTarget>> mOverriddenAnchorPositions;

    public AdaptedModelAnchor(Anchor anchor) {
        this(anchor, Optional.empty());
    }

    public AdaptedModelAnchor(Anchor anchor, Map<String, AnchorTarget> overriddenAnchorPositions) {
        this(anchor, Optional.of(overriddenAnchorPositions));
    }

    public AdaptedModelAnchor(Anchor anchor, Optional<Map<String, AnchorTarget>> overriddenAnchorPositions) {
        mAnchor = anchor;
        mOverriddenAnchorPositions = overriddenAnchorPositions;
    }

    @Override
    public String getId() {
        return mAnchor.getId();
    }

    public Anchor getModelAnchor() {
        return mAnchor;
    }

    @Override
    public boolean isReferenceAnchor() {
        return true;
    }

    public Optional<Map<String, AnchorTarget>> getOverriddenAnchorPositions() {
        return mOverriddenAnchorPositions;
    }

    @Override
    public IWall getOwner() {
        return new AdaptedModelWall((Wall) mAnchor.getAnchorOwner(), mOverriddenAnchorPositions);
    }

    @Override
    public Position2D getPosition() {
        return (mOverriddenAnchorPositions
                .map(overriddenAnchorPositions -> overriddenAnchorPositions.get(getId())
                    .getTargetPosition()).orElse(mAnchor.getPosition()).projectionXY());
    }

    @Override
    public List<IWallAnchor> getAllDockedAnchors() {
        return wrapWallAnchors(mAnchor.getAllDockedAnchors());
    }

    @Override
    public Optional<WallDockEnd> getHandleAnchorDockEnd() {
        return Wall.isWallHandleAnchor(mAnchor) ? Optional.of(Wall.getWallEndOfHandle(mAnchor)) : Optional.empty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAnchor);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AdaptedModelAnchor other = (AdaptedModelAnchor) obj;
        return Objects.equals(mAnchor, other.mAnchor);
    }

    @Override
    public String toString() {
        return mAnchor.getId() + " at " + mAnchor.getPosition().axesAndCoordsToString();
    }

    public static List<IWallAnchor> wrapWallAnchors(Collection<? extends BaseObject> modelObjects) {
        return wrapWallAnchors(modelObjects, Optional.empty());
    }

    public static List<IWallAnchor> wrapWallAnchors(Collection<? extends BaseObject> modelObjects, Optional<Map<String, AnchorTarget>> overriddenAnchorPositions) {
        List<IWallAnchor> adaptedAnchors = new ArrayList<>();
        for (BaseObject bo : modelObjects) {
            if (!(bo instanceof Anchor)) {
                continue;
            }
            Anchor anchor = (Anchor) bo;
            if (!(anchor.getAnchorOwner() instanceof Wall)) {
                continue;
            }
            adaptedAnchors.add(new AdaptedModelAnchor(anchor, overriddenAnchorPositions));
        }
        return adaptedAnchors;
    }
}
