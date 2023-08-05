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

import java.util.Collection;
import java.util.Map;

public class PerspectiveDescriptor {
    protected final Map<String, AbstractDockLayoutDescriptor> mDockAreaLayouts;
    protected final Collection<FloatingViewLayoutDescriptor> mFloatingViewLayouts;

    protected final Collection<String> mDefaultViewIds;

    public PerspectiveDescriptor(Map<String, AbstractDockLayoutDescriptor> dockAreaLayouts,
        Collection<FloatingViewLayoutDescriptor> floatingViewLayouts,
        Collection<String> defaultViews) {
        mDockAreaLayouts = dockAreaLayouts;
        mFloatingViewLayouts = floatingViewLayouts;
        mDefaultViewIds = defaultViews;
    }

    public Map<String, AbstractDockLayoutDescriptor> getDockAreaLayouts() {
        return mDockAreaLayouts;
    }

    public Collection<FloatingViewLayoutDescriptor> getFloatingViewLayouts() {
        return mFloatingViewLayouts;
    }

    public Collection<String> getDefaultViewIds() {
        return mDefaultViewIds;
    }
}
