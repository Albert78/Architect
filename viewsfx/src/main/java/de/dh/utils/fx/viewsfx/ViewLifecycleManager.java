package de.dh.utils.fx.viewsfx;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import de.dh.utils.fx.viewsfx.TabDockHost.DockableTabControl;
import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Lifecycle managing wrapper around a dockable application view which can be initialized and/or shown later.
 * The view's state can be visible, not visible yet but already initialized or even not initialized yet, on user demand.
 * The lifecycle manager manages the lifetime of the enclosed {@link Dockable} object and it's docking position by providing
 * a preferred docking location and remembering the last docking location.
 * For the application, the view's lifecycle manager acts as a handle to control the wrapped view like creating it
 * on demand, let it become visible, request it's current state, focus it etc.
 *
 * This lifecycle manager represents the view during the complete application lifetime. It is created before the view becomes
 * visible and, if the view is closed during application lifetime, typically remains valid until the application ends.
 * Some views / dockables can be closed, other cannot. The lifetime of this view lifecycle manager spans over all view's and
 * it's dockable's lifecycles.
 *
 * An application view is something like a properties view, an outline, a console view etc. but can also be something dynamic
 * like a code editor. For the well-known views (like properties, object tree etc.), their lifecycle manager will typically be
 * created at application startup and just stored in the views manager. For dynamic views like file editors, the views manager
 * might create the view lifecycle manager on demand.
 *
 * The application should provide a view lifecycle manager for each view which is managed by the system.
 *
 * <b>Hint about application references to dockables (or their views):</b>
 * If the application directly references dockables (or their views), the housekeeping of those references should be in-sync with
 * the corresponding view lifecycle manager, i.e. if the dockable is closable and not reusable, the application should also
 * drop it's references to the dockable and it's view. This can be accomplished by
 * {@link Dockable#addOnClosedHandler(java.util.function.Consumer) adding a close handler} to the dockable.
 *
 * @param <T> Type of the view's root node.
 */
public abstract class ViewLifecycleManager<T extends Node> {
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
     * If {@link #isReusableView()} is false, the dockable and its view will be discarded on close and rebuilt when needed
     * by calling {@link #createDockable(String)} again.
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
            Dockable<T> dockable = createDockable(mViewId);
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
     * @param viewId Id of the view being created. This view id must be passed to the created dockable as it's view id.
     */
    protected abstract Dockable<T> createDockable(String viewId);

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
    public void ensureVisible(Window floatingWindowsOwner, boolean bringToFront, boolean focus) {
        Dockable<T> dockable = getOrCreateDockable();
        if (dockable.getDockableUIRepresentation() instanceof DockableFloatingStage dfs) {
            if (bringToFront) {
                boolean alwaysOnTop = dfs.isAlwaysOnTop();
                dfs.setAlwaysOnTop(true);
                dfs.setAlwaysOnTop(alwaysOnTop);
                dfs.toFront();
            }
            if (focus) {
                dfs.requestFocus();
            }
            return;
        }
        if (dockable.getDockableUIRepresentation() instanceof DockableTabControl dtc) {
            if (focus) {
                dtc.selectTab();
                dockable.requestFocus();
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