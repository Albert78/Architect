package de.dh.utils.fx.viewsfx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.utils.fx.viewsfx.io.DesktopSettings;
import de.dh.utils.fx.viewsfx.io.FloatingViewSettings;
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

        /**
         * Gets the view's current {@link Dockable}, if the view is already alive, else, this method first creates the view and it's
         * {@link Dockable} before returning it.
         */
        public final Dockable<T> getOrCreateDockable() {
            if (mODockable.isEmpty()) {
                Dockable<T> dockable = createDockable();
                dockable.addOnBeforeClosingHandler(d -> {
                    mLastViewLocation = d.getViewLocation().map(vl -> vl.toDescriptor());
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
         * Returns the preferred location where this view should be created. This can be a dock host or a floating area.
         */
        protected abstract Optional<AbstractDockableViewLocation> getOrCreatePreferredViewLocation(IDockZoneProvider dockZoneProvider);

        /**
         * Returns a fallback view location. This method is a convenience method which can be called from
         * {@link #getOrCreatePreferredViewLocation()} to simply determine the first possible dock target of the
         * given {@code dockHostControl}.
         */
        protected static AbstractDockableViewLocation getFallbackViewLocation() {
            Iterator<DockHostControl> i = DockSystem.getDockHostControlsRegistry().values().iterator();
            if (!i.hasNext()) {
                throw new RuntimeException("Unable to find a valid dock zone for view location");
            }
            DockHostControl dockHostControl = i.next();
            return new DockViewLocation(dockHostControl.getFirstLeaf());
        }

        /**
         * Tries to locate the view location where this view was closed the last time, if possible. If the
         * last view location is not present any more in the dock hierarchy, this method will try to
         * create it, if possible.
         */
        protected Optional<AbstractDockableViewLocation> reviveLastViewLocation(Window ownerWindow) {
            return mLastViewLocation.flatMap(lvl -> {
                if (lvl instanceof DockViewLocationDescriptor dvld) {
                    String dockHostId = dvld.getDockHostId();
                    return DockSystem.tryGetOrCreateDockZone(dockHostId)
                            .map(dockZone -> (AbstractDockableViewLocation) new DockViewLocation(dockZone));
                } else if (lvl instanceof FloatingViewLocationDescriptor fvld) {
                    return Optional.of(new FloatingViewLocation(fvld.getFloatingArea(), ownerWindow));
                } else {
                    throw new IllegalArgumentException("Handling for view location descriptor of type " + lvl.getClass() + " is not implemented");
                }
            });
        }

        /**
         * Makes this view visible at its preferred location.
         */
        public void ensureVisible(Window floatingWindowsOwner) {
            Dockable<T> dockable = getOrCreateDockable();
            if (dockable.isDocked() || dockable.isFloating()) {
                return;
            }
            AbstractDockableViewLocation viewLocation = reviveLastViewLocation(floatingWindowsOwner).orElseGet(() -> {
                return getOrCreatePreferredViewLocation(DockSystem::tryGetOrCreateDockZone)
                        .orElseGet(() -> getFallbackViewLocation());
            });
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

    public void storeFloatingViewsSettings(DesktopSettings settings) {
        List<FloatingViewSettings> viewsSettings = settings.getFloatingViewsSettings();
        for (ViewLifecycleManager<?> viewLifecycleManager : mViewLifecycleManagers.values()) {
            String viewId = viewLifecycleManager.getViewId();
            viewLifecycleManager.getDockable().ifPresent(dockable -> {
                dockable.getViewLocation().ifPresent(dvl -> {
                    if (dvl instanceof FloatingViewLocation fvl)   {
                       Bounds floatingArea = fvl.getFloatingArea();
                       viewsSettings.add(new FloatingViewSettings(viewId,
                           (int) floatingArea.getMinX(), (int) floatingArea.getMinY(),
                           (int) floatingArea.getWidth(), (int) floatingArea.getHeight()));
                   }
                });
            });
        }
    }

    public void restoreFloatingViews(DesktopSettings settings, Window floatingWindowsOwner) {
        List<FloatingViewSettings> viewSettings = settings.getFloatingViewsSettings();
        for (FloatingViewSettings vs : viewSettings) {
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
