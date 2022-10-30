package de.dh.utils.fx.viewsfx;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import de.dh.utils.fx.viewsfx.io.AbstractDockZoneSettings;
import de.dh.utils.fx.viewsfx.io.DesktopSettings;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/**
 * Place where dockable controls can be docked.
 *
 * The application will embed this control into it's node structure to create a dockable area.
 *
 * If no dockable's are docked in this container, it will contain a single empty tab dock host.
 * Depending on the dock situation, there are zero or more splitter dock hosts, containing the tab dock host controls.
 */
public class DockHostControl extends BorderPane implements IDockHostParent {
    protected static class DockZoneBuilder {
        protected final String mDockZoneId;
        protected final BiFunction<DockHostControl, IDockZoneProvider, TabDockHost> mDockZoneCreator;

        public DockZoneBuilder(String dockZoneId, BiFunction<DockHostControl, IDockZoneProvider, TabDockHost> dockZoneCreator) {
            mDockZoneId = dockZoneId;
            mDockZoneCreator = dockZoneCreator;
        }

        public String getDockZoneId() {
            return mDockZoneId;
        }

        public BiFunction<DockHostControl, IDockZoneProvider, TabDockHost> getDockZoneCreator() {
            return mDockZoneCreator;
        }
    }

    public static class Builder {
        protected final String mDockHostId;
        protected final String mMainDockZoneId;
        protected ViewsRegistry mViewsRegistry;
        protected final Map<String, DockZoneBuilder> mDockZoneBuilders = new HashMap<>();

        public Builder(String dockHostId, String mainDockZoneId) {
            mDockHostId = dockHostId;
            mMainDockZoneId = mainDockZoneId;
        }

        public Builder addDockZone(String dockZoneId, BiFunction<DockHostControl, IDockZoneProvider, TabDockHost> dockZoneCreator) {
            DockZoneBuilder result = new DockZoneBuilder(dockZoneId, dockZoneCreator);
            mDockZoneBuilders.put(dockZoneId, result);
            return this;
        }

        public DockHostControl create() {
            DockHostControl result = new DockHostControl(mDockHostId);

            TabDockHost tdh = TabDockHost.create(result, mMainDockZoneId);
            result.initialize(tdh, mDockZoneBuilders);
            return result;
        }

        public DockHostControl restoreFromSettings(DesktopSettings desktopSettings) {
            ViewsRegistry viewsRegistry = DockSystem.getViewsRegistry();
            Map<String, AbstractDockZoneSettings> dockZoneHierarchies = desktopSettings.getDockZoneHierarchies();
            AbstractDockZoneSettings dockZoneHierarchy = dockZoneHierarchies.get(mDockHostId);
            if (dockZoneHierarchy == null) {
                return create();
            } else {
                DockHostControl result = new DockHostControl(mDockHostId);
                result.initialize(IDockZone.restoreFromSettings(result, dockZoneHierarchy, viewsRegistry), mDockZoneBuilders);
                return result;
            }
        }

        public DockHostControl createOrRestoreFromSettings(Optional<DesktopSettings> oDesktopSettings) {
            return oDesktopSettings.map(desktopSettings -> restoreFromSettings(desktopSettings)).orElseGet(() -> create());
        }
    }

    protected IDockZone mRootDockHost;
    protected Map<String, DockZoneBuilder> mDockZoneBuilders;

    protected DockHostControl(String dockHostId) {
        setId(dockHostId);
    }

    public void saveViewHierarchy(DesktopSettings desktopSettings) {
        desktopSettings.getDockZoneHierarchies().put(getDockHostId(), mRootDockHost.saveHierarchy());
    }

    protected void initialize(IDockZone rootDockHost, Map<String, DockZoneBuilder> dockZoneBuilders) {
        mRootDockHost = rootDockHost;
        setCenter((Parent) mRootDockHost);
        mDockZoneBuilders = dockZoneBuilders;
        DockSystem.getDockHostControlsRegistry().put(getDockHostId(), this);
    }

    public String getDockHostId() {
        return getId();
    }

    public IDockZone getRootDockHost() {
        return mRootDockHost;
    }

    public IDockZone findDockZoneById(String dockZoneId) {
        return mRootDockHost.findDockZoneById(dockZoneId);
    }

    public Optional<TabDockHost> getOrTryCreateDockZone(String dockZoneId) {
        TabDockHost result = (TabDockHost) findDockZoneById(dockZoneId);
        if (result != null) {
            return Optional.of(result);
        }
        DockZoneBuilder dockZoneBuilder = mDockZoneBuilders.get(dockZoneId);
        if (dockZoneBuilder == null) {
            return Optional.empty();
        }
        return Optional.of(dockZoneBuilder.getDockZoneCreator().apply(this, this::getOrTryCreateDockZone));
    }

    public TabDockHost getFirstLeaf() {
        IDockZone current = mRootDockHost;
        while (current instanceof SplitterDockHost sdh) {
            current = (IDockZone) sdh.getItems().get(0);
        }
        return (TabDockHost) current;
    }

    @Override
    public void invalidateLeaf(TabDockHost child) {
        // Nothing to do, single tab dock host will remain empty
    }

    @Override
    public void compressDockHierarchy(SplitterDockHost obsoleteSplitter, IDockZone moveUpChild) {
        obsoleteSplitter.disposeAndReplace(moveUpChild);
        mRootDockHost = moveUpChild;
        moveUpChild.setParentDockHost(this);
        setCenter((Parent) moveUpChild);
    }

    @Override
    public SplitterDockHost replaceWithSplitter(IDockZone replaceChild) {
        if (replaceChild != mRootDockHost) {
            throw new IllegalStateException("Inner node is not our root dock host");
        }
        SplitterDockHost result = SplitterDockHost.create(this);
        result.getItems().add((Parent) replaceChild);
        mRootDockHost = result;
        setCenter(result);
        return result;
    }
}
