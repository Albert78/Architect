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
package de.dh.utils.fx.viewsfx;

import java.util.HashMap;
import java.util.Map;

/**
 * Views manager for a more or less static set of views which is already known in advance.
 */
public class ViewsRegistry implements IViewsManager {
    protected final Map<String, ViewLifecycleManager<?>> mViewLifecycleManagers = new HashMap<>();

    public void addView(ViewLifecycleManager<?> viewManager) {
        mViewLifecycleManagers.put(viewManager.getViewId(), viewManager);
    }

    public void removeView(String viewId) {
        mViewLifecycleManagers.remove(viewId);
    }

    @Override
    public Map<String, ViewLifecycleManager<?>> getViewLifecycleManagers() {
        return mViewLifecycleManagers;
    }

    @Override
    public ViewLifecycleManager<?> getViewLifecycleManager(String viewId) {
        return mViewLifecycleManagers.get(viewId);
    }
}
