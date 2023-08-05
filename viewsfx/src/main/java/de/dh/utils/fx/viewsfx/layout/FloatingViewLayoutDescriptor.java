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

public class FloatingViewLayoutDescriptor {
    protected final String mViewId;
    protected final int mFloatingPositionX;
    protected final int mFloatingPositionY;
    protected final int mFloatingWidth;
    protected final int mFloatingHeight;

    public FloatingViewLayoutDescriptor(String viewId,
        int floatingPositionX, int floatingPositionY,
        int floatingWidth, int floatingHeight) {
        mViewId = viewId;
        mFloatingPositionX = floatingPositionX;
        mFloatingPositionY = floatingPositionY;
        mFloatingWidth = floatingWidth;
        mFloatingHeight = floatingHeight;
    }

    public String getViewId() {
        return mViewId;
    }

    public int getFloatingPositionX() {
        return mFloatingPositionX;
    }

    public int getFloatingPositionY() {
        return mFloatingPositionY;
    }

    public int getFloatingWidth() {
        return mFloatingWidth;
    }

    public int getFloatingHeight() {
        return mFloatingHeight;
    }
}
