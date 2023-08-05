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
package de.dh.utils.fx.viewsfx.layout;

/**
 * Contains the layout blueprint for a sash dockzone.
 */
public final class SashLayoutDescriptor extends AbstractDockLayoutDescriptor {
    public static enum Orientation {
        Horizontal, Vertical;
    }

    public static enum MaximizedZone {
        ZoneA, ZoneB, None;
    }

    protected final Orientation mOrientation;
    protected final double mDividerPosition;
    protected MaximizedZone mMaximizedZone = MaximizedZone.None;
    protected final AbstractDockLayoutDescriptor mZoneA;
    protected final AbstractDockLayoutDescriptor mZoneB;

    public SashLayoutDescriptor(String dockZoneId, Orientation orientation, double dividerPosition, AbstractDockLayoutDescriptor zoneA, AbstractDockLayoutDescriptor zoneB) {
        super(dockZoneId);
        mOrientation = orientation;
        mDividerPosition = dividerPosition;
        mZoneA = zoneA;
        mZoneB = zoneB;
    }

    public AbstractDockLayoutDescriptor getZoneA() {
        return mZoneA;
    }

    public AbstractDockLayoutDescriptor getZoneB() {
        return mZoneB;
    }

    public Orientation getOrientation() {
        return mOrientation;
    }

    public double getDividerPosition() {
        return mDividerPosition;
    }

    public MaximizedZone getMaximizedZone() {
        return mMaximizedZone;
    }

    public void setMaximizedZone(MaximizedZone value) {
        mMaximizedZone = value;
    }
}
