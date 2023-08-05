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

public class FloatingViewState {
    protected String mViewId = null;
    protected int mFloatingPositionX = -1;
    protected int mFloatingPositionY = -1;
    protected int mFloatingWidth = -1;
    protected int mFloatingHeight = -1;

    public FloatingViewState(String viewId,
        int floatingPositionX, int floatingPositionY,
        int floatingWidth, int floatingHeight) {
        mViewId = viewId;
        mFloatingPositionX = floatingPositionX;
        mFloatingPositionY = floatingPositionY;
        mFloatingWidth = floatingWidth;
        mFloatingHeight = floatingHeight;
    }

    public FloatingViewState() {
        // For JAXB
    }

    @XmlAttribute(name = "viewId")
    public String getViewId() {
        return mViewId;
    }

    public void setViewId(String value) {
        mViewId = value;
    }

    @XmlElement(name = "FloatingPositionX")
    public int getFloatingPositionX() {
        return mFloatingPositionX;
    }

    public void setFloatingPositionX(int value) {
        mFloatingPositionX = value;
    }

    @XmlElement(name = "FloatingPositionY")
    public int getFloatingPositionY() {
        return mFloatingPositionY;
    }

    public void setFloatingPositionY(int value) {
        mFloatingPositionY = value;
    }

    @XmlElement(name = "FloatingWidth")
    public int getFloatingWidth() {
        return mFloatingWidth;
    }

    public void setFloatingWidth(int value) {
        mFloatingWidth = value;
    }

    @XmlElement(name = "FloatingHeight")
    public int getFloatingHeight() {
        return mFloatingHeight;
    }

    public void setFloatingHeight(int value) {
        mFloatingHeight = value;
    }
}
