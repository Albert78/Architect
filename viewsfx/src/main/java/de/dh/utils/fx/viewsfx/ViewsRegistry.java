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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.utils.fx.viewsfx.TabDockHost.DockableTabControl;
import de.dh.utils.fx.viewsfx.state.FloatingViewState;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.stage.Window;

public class ViewsRegistry {
    /**
     * Lifecycle managing wrapper around a dockable application view which can be not initialized yet, initialized but not
     * visible or visible, on user demand. It manages the lifetime of the enclosed {@link Dockable} object
     * and it's docking position by providing a preferred docking location and remembering the last docking location.
     *
     * This lifecycle manager represents the wrapped view during the complete application
     * lifetime, this manager is created before the view becomes visible and, if the view is closed during application lifetime,
     * remains valid until the application ends. Thus it can potentially control multiple lifecycles of the view and it's dockable.
     *
     * An application view is something like a properties view, an outline, a console view etc.
     * @param <T> Type of the view.
     */
    public static abstract class ViewLifecycleManager<T extends Node> {
        protected final String mViewId;
        protected final boolean mReusableView;
        protected Optional<Dockable<T>> mODockable = Optional.empty();
        protected Optional<AbstractDockableViewLocationDescriptor> mLastViewLocation = Optional.empty();

        public ViewLifecycleManager(boolean reusableView) {
            this(UUID.randomUUID().toString(), reusableView);
        }

        public ViewLifecycleManager(String viewId, boolean reusableView) {
            mViewId = viewId;
            mReusableView = reusableView;
        }

        public String getViewId() {
            return mViewId;
        }

        /**
         * Gets the information whether this view is reusable, i.e. if the view's {@link Dockable} should be preserved
         * when it is closed by the user and reused the next time the view is requested.
         * If {@link #isReusableView()} is false, the view will be discarded on close and rebuilt on demand.
         */
        public final boolean isReusableView() {
            return mReusableView;
        }

        /**
         * Gets the view's UI control in form of a {@link Dockable} wrapper, if the view is alive. If the view was not shown
         * yet or if it was closed, the view is not alive and the return value will be an {@link Optional#empty() empty Optional}.
         */
        public final Optional<Dockable<T>> getDockable() {
            return mODockable;
        }

        public Optional<? extends AbstractDockableViewLocation> getCurrentViewLocation() {
            return mODockable.flatMap(d -> d.getViewLocation());
        }

        public Optional<AbstractDockableViewLocationDescriptor> getLastViewLocationDescriptor() {
            return mLastViewLocation;
        }

        /**
         * Gets the view's current {@link Dockable}, if the view is already alive, else, this method first creates the view and it's
         * {@link Dockable} before returning it.
         */
        public final Dockable<T> getOrCreateDockable() {
            if (mODockable.isEmpty()) {
                Dockable<T> dockable = createDockable();
                dockable.addOnBeforeClosingHandler(d -> {
                    mLastViewLocation = d.getViewLocation().map(vl -> vl.createDescriptor());
                });
                dockable.addOnClosedHandler(d -> {
                    if (!mReusableView) {
                        mODockable = Optional.empty();
                    }
                });
                mODockable = Optional.of(dockable);
            }
            return mODockable.get();
        }

        /**
         * Creates the underlaying view, returning a {@link Dockable} for it.
         * This method will be called by demand, when this view is becoming visible.
         * If the underlaying view is configured to be {@link #isReusableView() reusable}, this method
         * will only be called once and the returned dockable object will be reused even after the underlaying
         * view is closed. If it's not reusable, the returned dockable might become discarded when the view is closed
         * and this method might be called again when the view should become visible again.
         */
        protected abstract Dockable<T> createDockable();

        /**
         * Returns the preferred location where this view should be created. This can be a dock host somewhere in a
         * dock zones hierarchy or a floating area, represented by instances of class {@link DockViewLocationDescriptor}
         * or {@link FloatingViewLocationDescriptor}.
         */
        protected abstract Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation();

        /**
         * Returns a fallback view location. This method will be used as the last alternative if any other
         * try to get a possible view location fails. The default implementation simply determines the first
         * possible dock host of the first dock area control.
         */
        protected static AbstractDockableViewLocation getFallbackViewLocation() {
            Iterator<DockAreaControl> i = DockSystem.getDockAreaControlsRegistry().values().iterator();
            if (!i.hasNext()) {
                throw new RuntimeException("Unable to find a valid dock zone for view location");
            }
            DockAreaControl dockAreaControl = i.next();
            return new DockViewLocation(dockAreaControl.getFirstLeaf());
        }

        /**
         * Tries to locate or create the given view location, if possible.
         */
        protected Optional<AbstractDockableViewLocation> tryMaterializeViewLocation(AbstractDockableViewLocationDescriptor location, Window ownerWindow) {
            if (location instanceof DockViewLocationDescriptor dvld) {
                return DockSystem.tryGetOrCreateTabDockHost(dvld)
                        .map(tabDockHost -> (AbstractDockableViewLocation) new DockViewLocation(tabDockHost));
            } else if (location instanceof FloatingViewLocationDescriptor fvld) {
                return Optional.of(new FloatingViewLocation(fvld.getFloatingArea(), ownerWindow));
            } else {
                throw new IllegalArgumentException("Handling for view location descriptor of type " + location.getClass() + " is not implemented");
            }
        }

        /**
         * Makes this view visible at its preferred location.
         */
        public void ensureVisible(Window floatingWindowsOwner, boolean focus) {
            Dockable<T> dockable = getOrCreateDockable();
            if (dockable.getDockableUIRepresentation() instanceof DockableFloatingStage dfs) {
                if (focus) {
                    dfs.requestFocus();
                }
                return;
            }
            if (dockable.getDockableUIRepresentation() instanceof DockableTabControl dtc) {
                if (focus) {
                    dtc.selectTab();
                }
                return;
            }
            AbstractDockableViewLocation viewLocation = mLastViewLocation
                    .or(this::getPreferredViewLocation)
                    .flatMap(lvl -> tryMaterializeViewLocation(lvl, floatingWindowsOwner))
                    .orElseGet(ViewLifecycleManager::getFallbackViewLocation);
            dockable.tryMakeVisible(viewLocation);
        }

        /**
         * Closes the underlaying {@link Dockable}.
         */
        public void close() {
            getDockable().ifPresent(dockable -> {
                // The OnClose handler which is attached to the dockable in this class will handle the reusable flag
                dockable.close();
            });
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ViewsRegistry.class);

    protected final Map<String, ViewLifecycleManager<?>> mViewLifecycleManagers = new HashMap<>();

    public void addView(ViewLifecycleManager<?> viewManager) {
        mViewLifecycleManagers.put(viewManager.getViewId(), viewManager);
    }

    public void removeView(String viewId) {
        mViewLifecycleManagers.remove(viewId);
    }

    public List<FloatingViewState> storeFloatingViewStates() {
        List<FloatingViewState> result = new ArrayList<>();
        for (ViewLifecycleManager<?> viewLifecycleManager : mViewLifecycleManagers.values()) {
            String viewId = viewLifecycleManager.getViewId();
            viewLifecycleManager.getDockable().ifPresent(dockable -> {
                dockable.getViewLocation().ifPresent(dvl -> {
                    if (dvl instanceof FloatingViewLocation fvl)   {
                       Bounds floatingArea = fvl.getFloatingArea();
                       result.add(new FloatingViewState(viewId,
                           (int) floatingArea.getMinX(), (int) floatingArea.getMinY(),
                           (int) floatingArea.getWidth(), (int) floatingArea.getHeight()));
                   }
                });
            });
        }
        return result;
    }

    public void restoreFloatingViews(Collection<FloatingViewState> floatingViewStates, Window floatingWindowsOwner) {
        for (FloatingViewState vs : floatingViewStates) {
            String viewId = vs.getViewId();

            ViewLifecycleManager<?> viewLifecycleManager = mViewLifecycleManagers.get(viewId);
            if (viewLifecycleManager == null) {
                log.warn("Unable to restore view settings for view '" + viewId + "', view is not present in registry");
                continue;
            }
            Dockable<?> dockable = viewLifecycleManager.getOrCreateDockable();
            dockable.toFloatingState(floatingWindowsOwner, vs.getFloatingPositionX(), vs.getFloatingPositionY(), vs.getFloatingWidth(), vs.getFloatingHeight());
        }
    }

    public ViewLifecycleManager<?> getViewLifecycleManager(String viewId) {
        return mViewLifecycleManagers.get(viewId);
    }
}
