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
package de.dh.utils.fx.viewsfx.state;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public final class SashDockZoneState extends AbstractDockZoneState {
    public static enum Orientation {
        Horizontal, Vertical;
    }

    public static enum MaximizedZone {
        ZoneA, ZoneB, None;
    }

    protected AbstractDockZoneState mZoneStateA;
    protected AbstractDockZoneState mZoneStateB;
    protected Orientation mOrientation;
    protected double mDividerPosition;
    protected MaximizedZone mMaximizedZone = MaximizedZone.None;

    @XmlElement(name = "ZoneA")
    public AbstractDockZoneState getZoneStateA() {
        return mZoneStateA;
    }

    public void setZoneStateA(AbstractDockZoneState value) {
        mZoneStateA = value;
    }

    @XmlElement(name = "ZoneB")
    public AbstractDockZoneState getZoneStateB() {
        return mZoneStateB;
    }

    public void setZoneStateB(AbstractDockZoneState value) {
        mZoneStateB = value;
    }

    @XmlAttribute(name = "orientation")
    public Orientation getOrientation() {
        return mOrientation;
    }

    public void setOrientation(Orientation value) {
        mOrientation = value;
    }

    /**
     * Gets the divider's position, if no zone is maximized,
     * else gets the original divider's position (in non-maximized state).
     */
    @XmlElement(name = "DividerPosition")
    public double getDividerPosition() {
        return mDividerPosition;
    }

    public void setDividerPosition(double value) {
        mDividerPosition = value;
    }

    @XmlElement(name = "MaximizedZone")
    public MaximizedZone getMaximizedZone() {
        return mMaximizedZone;
    }

    public void setMaximizedZone(MaximizedZone value) {
        mMaximizedZone = value;
    }
}
