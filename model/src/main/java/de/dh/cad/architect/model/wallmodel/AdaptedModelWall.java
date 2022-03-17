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
package de.dh.cad.architect.model.wallmodel;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.coords.AnchorTarget;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Wall;

public class AdaptedModelWall implements IWall {
    protected final Wall mWall;
    protected final Optional<Map<String, AnchorTarget>> mOverriddenAnchorPositions;

    public AdaptedModelWall(Wall wall) {
        this(wall, Optional.empty());
    }

    public AdaptedModelWall(Wall wall, Map<String, AnchorTarget> overriddenAnchorPositions) {
        this(wall, Optional.of(overriddenAnchorPositions));
    }

    public AdaptedModelWall(Wall wall, Optional<Map<String, AnchorTarget>> overriddenAnchorPositions) {
        mWall = wall;
        mOverriddenAnchorPositions = overriddenAnchorPositions;
    }

    @Override
    public String getId() {
        return mWall.getId();
    }

    public Wall getModelWall() {
        return mWall;
    }

    @Override
    public boolean representsRealWall() {
        return true;
    }

    public Optional<Map<String, AnchorTarget>> getOverriddenAnchorPositions() {
        return mOverriddenAnchorPositions;
    }

    @Override
    public IWallAnchor getAnchorWallHandleA() {
        return new AdaptedModelAnchor(mWall.getAnchorWallHandleA(), mOverriddenAnchorPositions);
    }

    @Override
    public IWallAnchor getAnchorWallHandleB() {
        return new AdaptedModelAnchor(mWall.getAnchorWallHandleB(), mOverriddenAnchorPositions);
    }

    @Override
    public WallBevelType getWallBevelA() {
        return mWall.getWallBevelA();
    }

    @Override
    public void setWallBevelA(WallBevelType value) {
        mWall.setWallBevelA(value);
    }

    @Override
    public WallBevelType getWallBevelB() {
        return mWall.getWallBevelB();
    }

    @Override
    public void setWallBevelB(WallBevelType value) {
        mWall.setWallBevelB(value);
    }

    @Override
    public Length getThickness() {
        return mWall.getThickness();
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
        return mWall.getId() + " between " + getAnchorWallHandleA() + " <-> " + getAnchorWallHandleB();
    }

    public static Collection<IWall> wrapWalls(Collection<? extends BaseObject> modelObjects) {
        return wrapWalls(modelObjects, Optional.empty());
    }

    public static Collection<IWall> wrapWalls(Collection<? extends BaseObject> modelObjects, Optional<Map<String, AnchorTarget>> overriddenAnchorPositions) {
        Collection<IWall> adaptedWalls = new TreeSet<>();
        for (BaseObject bo : modelObjects) {
            if (bo instanceof Wall wall) {
                adaptedWalls.add(new AdaptedModelWall(wall, overriddenAnchorPositions));
            }
        }
        return adaptedWalls;
    }

    public static Collection<BaseObject> unwrapWalls(Collection<IWall> adaptedObjects) {
        return adaptedObjects
                .stream()
                .map(wall -> ((AdaptedModelWall) wall).getModelWall())
                .collect(Collectors.toList());
    }
}
