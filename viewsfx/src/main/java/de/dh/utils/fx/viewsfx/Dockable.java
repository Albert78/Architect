package de.dh.utils.fx.viewsfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import de.dh.utils.fx.viewsfx.TabDockHost.DockableTabControl;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Controller class which wraps each dockable control/node of the application.
 *
 * This dockable controller holds a reference to the dockable UI control as well as additional properties describing
 * the dockable's capabilities, e.g. whether it's closeable or if it supports to be a floatable window.
 * Furthermore, this controller holds several constraints for the dockable UI control.
 *
 * The lifetime of this controller is the same as the dockable control it wraps. It is created by the application typically at
 * the creation time of the UI control and should be discarded when the underlaying UI control's lifetime ends. If the UI control
 * supports to be reused after closing it, it can be revived by just calling any of the {@code dockXX} or {@code toFloatingState} methods.
 *
 * This dockable can be docked, floating or none of the two (see {@link #dockedProperty()}, {@link #floatingProperty()}).
 * At the time it is docked or floating, this object is referenced by the dock hosts ({@link TabDockHost}, {@link DockableFloatingStage}).
 * When it's neither docked nor floating, this object together with its contained {@link #getView() view} is not referenced by any of the
 * docking system's objects. If it's referenced from the application, it can be revived by just docking to any dock host or by making
 * it floating. If it's not referenced by the application, its lifetime (and the lifetime of the enclosed view) ends at the time when
 * it is closed by the user.
 *
 * Application components may communicate with the underlaying UI control without the knowledge about the dock system and the dock situation, if
 * they neither interfere with the UI control's location in the node hierarchy nor with it's visibility state.
 * If the application wants to control the docking situation (e.g. docking or undocking a UI control programmatically), it should
 * do that using this dockable controller.
 */
public class Dockable<T extends Node> {
    protected final ChangeListener<Boolean> VISIBILITY_CHECK_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            checkVisible();
        }
    };

    // TODO: Make configurable
    protected double mDefaultWidth = 800;
    protected double mDefaultHeight = 600;

    protected Double mLastFloatingWidth = null;
    protected Double mLastFloatingHeight = null;

    protected final T mView;
    protected final String mViewId;
    protected final boolean mShowCloseButton;

    protected final StringProperty mTitleProperty = new SimpleStringProperty();
    protected final ObjectProperty<Object> mDataProperty = new SimpleObjectProperty<>();
    protected final ObjectProperty<Node> mFocusControlProperty = new SimpleObjectProperty<>();

    protected final BooleanProperty mFloatableProperty = new SimpleBooleanProperty(true);
    // TODO: Property to control capability: Maximize on double click

    protected final ReadOnlyBooleanWrapper mDockedProperty = new ReadOnlyBooleanWrapper(false);
    protected final ReadOnlyBooleanWrapper mFloatingProperty = new ReadOnlyBooleanWrapper(false);
    protected final TransactionalBooleanProperty mVisibleProperty = new TransactionalBooleanProperty(false);

    protected final ReadOnlyObjectWrapper<IDockableUIRepresentation> mDockableUIRepresentationProperty = new ReadOnlyObjectWrapper<>();

    protected final List<Consumer<Dockable<?>>> mOnBeforeClosingHandlers = new ArrayList<>();
    protected final List<Consumer<Dockable<?>>> mOnClosedHandlers = new ArrayList<>();
    protected Function<Dockable<?>, Boolean> mOnCloseRequestHandler = null;

    protected final List<Consumer<Dockable<?>>> mOnBeforeUiRepresentationDisposingHandlers = new ArrayList<>();
    protected final List<Consumer<Dockable<?>>> mOnUiRepresentationInstalledHandlers = new ArrayList<>();

    public Dockable(T view, String viewId, String title, boolean showCloseButton) {
        mView = view;
        mViewId = viewId;
        mShowCloseButton = showCloseButton;
        mTitleProperty.set(title);
    }

    public static <T extends Node> Dockable<T> of(T view, String viewId, String title, boolean showCloseButton) {
        return new Dockable<>(view, viewId, title, showCloseButton);
    }

    public static <T extends Node> Dockable<T> of(T view, String title, boolean showCloseButton) {
        String viewId = UUID.randomUUID().toString();
        return new Dockable<>(view, viewId, title, showCloseButton);
    }

    public T getView() {
        return mView;
    }

    public String getViewId() {
        return mViewId;
    }

    public boolean isShowCloseButton() {
        return mShowCloseButton;
    }

    public BooleanProperty floatableProperty() {
        return mFloatableProperty;
    }

    public boolean isFloatable() {
        return mFloatableProperty.get();
    }

    public void setFloatable(boolean value) {
        mFloatableProperty.set(value);
    }

    public ObjectProperty<Object> dataProperty() {
        return mDataProperty;
    }

    /**
     * Custom object which can be freely used by the application to store additional info to a dockable.
     */
    public Object getData() {
        return mDataProperty.get();
    }

    public void setData(Object value) {
        mDataProperty.set(value);
    }

    public ObjectProperty<Node> focusControlProperty() {
        return mFocusControlProperty;
    }

    /**
     * Gets the optional control inside this dockable which should request the focus instead of our docking tab.
     */
    public Node getFocusControl() {
        return mFocusControlProperty.get();
    }

    public void setFocusControl(Node value) {
        mFocusControlProperty.set(value);
    }

    public StringProperty titleProperty() {
        return mTitleProperty;
    }

    public String getDockableTitle() {
        return mTitleProperty.get();
    }

    public void setDockableTitle(String value) {
        mTitleProperty.set(value);
    }

    public ReadOnlyBooleanProperty floatingProperty() {
        return mFloatingProperty;
    }

    public boolean isFloating() {
        return mFloatingProperty.get();
    }

    public ReadOnlyBooleanProperty dockedProperty() {
        return mDockedProperty;
    }

    public boolean isDocked() {
        return mDockedProperty.get();
    }

    public TransactionalBooleanProperty visibleProperty() {
        return mVisibleProperty;
    }

    public boolean isVisible() {
        return mVisibleProperty.getValue();
    }

    public Double getLastFloatingWidth() {
        return mLastFloatingWidth;
    }

    public void setLastFloatingWidth(Double value) {
        mLastFloatingWidth = value;
    }

    public Double getLastFloatingHeight() {
        return mLastFloatingHeight;
    }

    public void setLastFloatingHeight(Double value) {
        mLastFloatingHeight = value;
    }

    public Function<Dockable<?>, Boolean> getCloseRequestHandler() {
        return mOnCloseRequestHandler;
    }

    public void setOnCloseRequestHandler(Function<Dockable<?>, Boolean> handler) {
        mOnCloseRequestHandler = handler;
    }

    public void addOnBeforeClosingHandler(Consumer<Dockable<?>> handler) {
        mOnBeforeClosingHandlers.add(handler);
    }

    public void removeOnBeforeClosingHandler(Consumer<Dockable<?>> handler) {
        mOnBeforeClosingHandlers.remove(handler);
    }

    public void addOnClosedHandler(Consumer<Dockable<?>> handler) {
        mOnClosedHandlers.add(handler);
    }

    public void removeOnClosedHandler(Consumer<Dockable<?>> handler) {
        mOnClosedHandlers.remove(handler);
    }

    public ReadOnlyObjectProperty<IDockableUIRepresentation> dockableUIRepresentationProperty() {
        return mDockableUIRepresentationProperty.getReadOnlyProperty();
    }

    public void addOnBeforeUiRepresentationDisposingHandler(Consumer<Dockable<?>> handler) {
        mOnBeforeUiRepresentationDisposingHandlers.add(handler);
    }

    public void removeOnBeforeUiRepresentationDisposingHandler(Consumer<Dockable<?>> handler) {
        mOnBeforeUiRepresentationDisposingHandlers.remove(handler);
    }

    public void addOnUiRepresentationInstalledHandler(Consumer<Dockable<?>> handler) {
        mOnUiRepresentationInstalledHandlers.add(handler);
    }

    public void removeOnUiRepresentationInstalledHandler(Consumer<Dockable<?>> handler) {
        mOnUiRepresentationInstalledHandlers.remove(handler);
    }

    /**
     * Returns the wrapper control/stage which contains the dockable UI control.
     */
    public IDockableUIRepresentation getDockableUIRepresentation() {
        return mDockableUIRepresentationProperty.get();
    }

    public Optional<AbstractDockableViewLocation> getViewLocation() {
        IDockableUIRepresentation dockableUIRepresentation = getDockableUIRepresentation();
        if (dockableUIRepresentation instanceof DockableTabControl dtc) {
            return Optional.of(new DockViewLocation(dtc.getDockHost()));
        }
        if (dockableUIRepresentation instanceof DockableFloatingStage dfs) {
            return Optional.of(new FloatingViewLocation(new BoundingBox(dfs.getX(), dfs.getY(), dfs.getWidth(), dfs.getHeight()), dfs.getOwner()));
        }
        return Optional.empty();
    }

    protected void checkVisible() {
        IDockableUIRepresentation dockableUIRepresentation = getDockableUIRepresentation();
        if (dockableUIRepresentation instanceof DockableFloatingStage dfs) {
            mVisibleProperty.set(true);
            return;
        }
        if (dockableUIRepresentation instanceof DockableTabControl dtc) {
            mVisibleProperty.set(dtc.isSelected());
            return;
        }
        mVisibleProperty.set(false);
    }

    /**
     * Removes the underlaying dockable UI control from its current docking host, leaving it outside
     * any node hierarchy.
     * This dockable can be abandoned after this method was called or it might be reused by using
     * one of its {@code dockAtXXX} methods later.
     */
    public void detachFromDockingHost() {
        IDockableUIRepresentation dockableUIRepresentation = getDockableUIRepresentation();
        if (dockableUIRepresentation == null) {
            // Not docked, i.e. already detached
            return;
        }
        fireBeforeUiRepresentationDisposing();
        mVisibleProperty.beginChange();
        dockableUIRepresentation.dispose();
        if (getDockableUIRepresentation() instanceof DockableTabControl dtc) {
            // In fact this is not necessary because the DockableTabControl won't be reused, but we never know...
            dtc.selectedProperty().removeListener(VISIBILITY_CHECK_LISTENER);
        }
        mDockableUIRepresentationProperty.set(null);
        mFloatingProperty.set(false);
        mDockedProperty.set(false);
        checkVisible();
        mVisibleProperty.endChange();
    }

    public DockableTabControl dockLast(TabDockHost host) {
        return dockAt(host, host.getTabs().size());
    }

    /**
     * Docks this dockable at the given tab dock host at the given tab position.
     * This dockable will be detached from its former docking host before docking at the new position.
     */
    public DockableTabControl dockAt(TabDockHost host, int beforePosition) {
        IDockableUIRepresentation dockableUIRepresentation = getDockableUIRepresentation();
        int currentDockPosition = host.getDockPosition(dockableUIRepresentation);
        if (currentDockPosition != -1) { // Already docked at that host
            if (currentDockPosition == beforePosition || currentDockPosition + 1 == beforePosition) {
                // Nothing to do, control is already docked at that position
                return (DockableTabControl) dockableUIRepresentation;
            }
            if (currentDockPosition < beforePosition) {
                beforePosition--;
            }
        }
        mVisibleProperty.beginChange();
        detachFromDockingHost();
        DockableTabControl result = host.addDockable(this, beforePosition);
        mDockableUIRepresentationProperty.set(result);
        result.selectedProperty().addListener(VISIBILITY_CHECK_LISTENER);
        mDockedProperty.set(true);
        checkVisible();
        mVisibleProperty.endChange();
        fireUiRepresentationInstalled();
        return result;
    }

    public DockableTabControl dockAt(IDockZone dockZone, DockSide side) {
        return dockAt(dockZone, side, UUID.randomUUID().toString());
    }

    /**
     * Docks this dockable in the given dockZone at the side given by {@code side} by dividing that zone into two zones with a splitter.
     * This dockable will be detached from its former docking host before docking at the new position.
     * @param targetDockZone DockZone which should be divided to host the current content and this dockable.
     * @param side The side of the DockZone where this dockable should be docked.
     * @param newDockZoneId ID of the new dock zone which will arise for this dockable to be docked at the declared place.
     * @return The dockable's new tab representation object.
     */
    public DockableTabControl dockAt(IDockZone targetDockZone, DockSide side, String newDockZoneId) {
        // Check for invalid situations
        if (targetDockZone instanceof TabDockHost) {
            TabDockHost targetPane = (TabDockHost) targetDockZone;
            IDockableUIRepresentation dockableUIRepresentation = getDockableUIRepresentation();
            if (targetPane.isSingleChild(dockableUIRepresentation)) {
                // Nothing to do, control is already docked at that position.
                return (DockableTabControl) dockableUIRepresentation;
            }
            // No other situation will invalidate/remove the tab dock host, so we can be sure
            // the dock zone will be valid after detaching from it.
        }

        mVisibleProperty.beginChange();

        // In case the dockable is the single child of its tab dock host (which means the
        // tab dock host will disappear by undocking)
        // AND
        // the user docks the dockable to the same splitter parent where the tab host is already located
        // (which might make sense if the dockable is put to the other side or if it should be docked at the
        // other splitter orientation),
        // then that splitter parent will disappear by the following undocking operation.
        // In fact we would need to remember the position of the splitter in its parent (which in fact is
        // the correct definition of a "docking zone") instead of representing the docking zone by the
        // component which is currently docked there (the splitter).
        // To prevent overcomplication, we just store the "new citizen" of the docking zone inside the splitter,
        // in case the splitter is removed by the undocking operation. That solves completely our problem without
        // new formalization and model elements.
        detachFromDockingHost();
        if (targetDockZone instanceof SplitterDockHost) {
            SplitterDockHost splitterDockZone = (SplitterDockHost) targetDockZone;
            targetDockZone = splitterDockZone.getReplacementOrSelf();
        }

        TabDockHost newSibling = targetDockZone.split(side, newDockZoneId);

        DockableTabControl result = newSibling.addDockable(this, 0);
        mDockableUIRepresentationProperty.set(result);
        mDockedProperty.set(true);
        checkVisible();
        mVisibleProperty.endChange();
        fireUiRepresentationInstalled();
        return result;
    }

    public DockableFloatingStage toFloatingState(Window ownerStage, double x, double y) {
        return toFloatingState(ownerStage, x, y,
            mLastFloatingWidth == null ? mView.prefWidth(mDefaultWidth) : mLastFloatingWidth,
            mLastFloatingHeight == null ? mView.prefHeight(mDefaultHeight) : mLastFloatingHeight);
    }

    public DockableFloatingStage toFloatingState(Window ownerStage, double x, double y, double w, double h) {
        if (!mFloatableProperty.get()) {
            // Not allowed to be floatable
            return null;
        }

        IDockableUIRepresentation dockableUIRepresentation = getDockableUIRepresentation();
        if (dockableUIRepresentation instanceof DockableFloatingStage dfs) {
            // Already floating
            return dfs;
        }

        mVisibleProperty.beginChange();
        detachFromDockingHost();
        DockableFloatingStage result = new DockableFloatingStage(this, ownerStage);
        result.show(x, y, w, h);
        mDockableUIRepresentationProperty.set(result);
        mFloatingProperty.set(true);

        mLastFloatingWidth = w;
        mLastFloatingHeight = h;
        result.widthProperty().addListener((obs, oldVal, newVal) -> {
            mLastFloatingWidth = newVal.doubleValue();
        });
        result.heightProperty().addListener((obs, oldVal, newVal) -> {
            mLastFloatingHeight = newVal.doubleValue();
        });

        checkVisible();
        mVisibleProperty.endChange();
        fireUiRepresentationInstalled();
        return result;
    }

    public boolean tryMakeVisible(AbstractDockableViewLocation targetViewLocation) {
        if (targetViewLocation instanceof DockViewLocation dvl) {
            TabDockHost dockHost = dvl.getDockHost();
            if (dockHost.isAlive()) {
                dockLast(dockHost);
                return true;
            }
        }
        if (targetViewLocation instanceof FloatingViewLocation fvl) {
            Bounds area = fvl.getFloatingArea();
            toFloatingState(fvl.getOwnerWindow(), area.getMinX(), area.getMinY());
            return true;
        }
        return false;
    }

    public void close() {
        Function<Dockable<?>, Boolean> closeRequestHandler = mOnCloseRequestHandler;
        boolean closeAccepted = closeRequestHandler == null ? true : closeRequestHandler.apply(this);
        if (closeAccepted) {
            fireBeforeClosing();
            detachFromDockingHost();
            fireClosed();
        }
    }

    protected void fireBeforeClosing() {
        for (Consumer<Dockable<?>> handler : mOnBeforeClosingHandlers) {
            handler.accept(this);
        }
    }

    protected void fireClosed() {
        for (Consumer<Dockable<?>> handler : mOnClosedHandlers) {
            handler.accept(this);
        }
    }

    protected void fireBeforeUiRepresentationDisposing() {
        for (Consumer<Dockable<?>> handler : mOnBeforeUiRepresentationDisposingHandlers) {
            handler.accept(this);
        }
    }

    protected void fireUiRepresentationInstalled() {
        for (Consumer<Dockable<?>> handler : mOnUiRepresentationInstalledHandlers) {
            handler.accept(this);
        }
    }
}
