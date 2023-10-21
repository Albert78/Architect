package de.dh.utils.fx.viewsfx;

import java.util.Map;

public interface IViewsManager {
    Map<String, ViewLifecycleManager<?>> getViewLifecycleManagers();
    ViewLifecycleManager<?> getViewLifecycleManager(String viewId);
}