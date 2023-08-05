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
package de.dh.cad.architect.ui.view;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.ui.ApplicationController;
import de.dh.cad.architect.ui.Configuration;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.ChangeEntry;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.logoutput.LogOutputControl;
import de.dh.cad.architect.ui.objects.IModelBasedObject;
import de.dh.cad.architect.ui.objecttree.ObjectTreeControl;
import de.dh.cad.architect.ui.persistence.MainWindowState;
import de.dh.cad.architect.ui.persistence.ViewState;
import de.dh.cad.architect.ui.properties.PropertiesControl;
import de.dh.cad.architect.ui.scriptconsole.ScriptConsoleControl;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.libraries.LibraryManagerMainWindow;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.fx.viewsfx.AbstractDockableViewLocationDescriptor;
import de.dh.utils.fx.viewsfx.DockAreaControl;
import de.dh.utils.fx.viewsfx.DockSystem;
import de.dh.utils.fx.viewsfx.Dockable;
import de.dh.utils.fx.viewsfx.DockableFloatingStage;
import de.dh.utils.fx.viewsfx.ViewsRegistry;
import de.dh.utils.fx.viewsfx.ViewsRegistry.ViewLifecycleManager;
import de.dh.utils.fx.viewsfx.io.ViewsLayoutStateIO;
import de.dh.utils.fx.viewsfx.layout.AbstractDockLayoutDescriptor;
import de.dh.utils.fx.viewsfx.layout.PerspectiveDescriptor;
import de.dh.utils.fx.viewsfx.layout.SashLayoutDescriptor;
import de.dh.utils.fx.viewsfx.layout.SashLayoutDescriptor.Orientation;
import de.dh.utils.fx.viewsfx.layout.TabHostLayoutDescriptor;
import de.dh.utils.fx.viewsfx.state.ViewsLayoutState;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

public class MainWindow implements Initializable {
    protected ChangeListener<InteractionsControl> INTERACTIONS_CONTROL_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends InteractionsControl> observable, InteractionsControl oldValue, InteractionsControl newValue) {
            removeInteractionsControl();
            if (newValue != null) {
                addInteractionsControl(newValue.getControl(), newValue.getTitle(), newValue.hasPriority());
            }
        }
    };

    protected final ChangeListener<AbstractViewBehavior<? extends IModelBasedObject, ? extends Node>> VIEW_BEHAVIOR_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends AbstractViewBehavior<? extends IModelBasedObject, ? extends Node>> observable,
            AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> oldValue,
            AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> newValue) {
            unbindViewBehavior();
            bindViewBehavior(newValue);
        }
    };

    public static final String MAIN_DOCK_HOST_ID = "Main";

    public static final String DOCK_ZONE_ID_MAIN = "Main"; // Complete area of window

    public static final String DOCK_ZONE_ID_MAIN_LEFT = "MainLeft"; // Left area of Main

    public static final String DOCK_ZONE_ID_MAIN_LEFT_UPPER = "MainLeftUpper"; // Upper area of MainLeft
    public static final String DOCK_ZONE_ID_MAIN_LEFT_LOWER = "MainLeftLower"; // Lower area of MainLeft

    public static final String DOCK_ZONE_ID_MAIN_CENTER = "MainCenter"; // Right (center) area of Main

    public static final String DOCK_ZONE_ID_MAIN_CENTER_BOTTOM = "MainCenterBottom"; // Lower area of MainCenter
    public static final String DOCK_ZONE_ID_MAIN_CENTER_MAIN = "MainCenterMain"; // Upper area of MainCenter

    public static final String TAB_DOCK_HOST_ID_VIEWS_1 = "Views1"; // Properties view
    public static final String TAB_DOCK_HOST_ID_VIEWS_2 = "Views2"; // Objects tree view

    public static final String TAB_DOCK_HOST_ID_ADDITIONAL_VIEWS = "AdditionalViews"; // Scripting and additional views
    public static final String TAB_DOCK_HOST_ID_EDITORS = "Editors"; // Editor views

    public static final String VIEW_ID_PROPERTIES = "Properties";
    public static final String VIEW_ID_INTERACTIONS_PANE = "InteractionsPane";
    public static final String VIEW_ID_OBJECT_TREE = "ObjectTree";
    public static final String VIEW_ID_SCRIPT_CONSOLE = "ScriptConsole";
    public static final String VIEW_ID_LOG_OUTPUT = "LogOutput";
    public static final String VIEW_ID_CONSTRUCTION_AREA = "ConstructionArea";
    public static final String VIEW_ID_3D_PRESENTATION = "3dPresentation";

    public static final int WINDOW_SIZE_X = 1000;
    public static final int WINDOW_SIZE_Y = 800;
    public static final int LEFT_AREA_WIDTH = 400;

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    protected static final String FXML = "MainWindow.fxml";

    protected final ApplicationController mApplicationController;
    protected final UiController mUIController;

    protected ThreeDView mThreeDView = null;
    protected ConstructionView mConstructionView = null;
    protected PropertiesControl mPropertiesControl = null;
    protected ObjectTreeControl mObjectTreeControl = null;
    protected ScriptConsoleControl mScriptConsoleControl = null;
    protected LogOutputControl mLogOutputControl = null;

    protected ViewLifecycleManager<ConstructionView> mConstructionViewManager = null;
    protected ViewLifecycleManager<ThreeDView> mThreeDViewManager = null;
    protected ViewLifecycleManager<PropertiesControl> mPropertiesViewManager = null;
    protected ViewLifecycleManager<Parent> mInteractionsPaneViewManager = null;
    protected ViewLifecycleManager<ObjectTreeControl> mObjectTreeViewManager = null;
    protected ViewLifecycleManager<ScriptConsoleControl> mScriptConsoleViewManager = null;
    protected ViewLifecycleManager<LogOutputControl> mLogOutputViewManager = null;

    protected final List<AbstractPlanView<? extends IModelBasedObject, ? extends Node>> mPlanViews = new ArrayList<>();
    protected AbstractPlanView<? extends IModelBasedObject, ? extends Node> mActiveView = null;
    protected AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> mViewBehavior = null;
    protected StackPane mInteractionsPaneParent = new StackPane();

    protected DockAreaControl mMainDockHost = null;

    @FXML
    protected MenuBar mMenuBar;

    @FXML
    protected Parent mRoot;

    @FXML
    protected StackPane mDockHostParent;

    @FXML
    protected MenuItem mFileNewMenuItem;

    @FXML
    protected MenuItem mFileOpenMenuItem;

    @FXML
    protected Menu mFileRecentMenu;

    @FXML
    protected MenuItem mFileSaveMenuItem;

    @FXML
    protected MenuItem mFileSaveAsMenuItem;

    @FXML
    protected MenuItem mFileQuitMenuItem;

    @FXML
    protected MenuItem mUndoMenuItem;

    @FXML
    protected MenuItem mRedoMenuItem;

    @FXML
    protected MenuItem mOpenLibraryManagerMenuItem;

    @FXML
    protected Menu mWindowMenu;

    @FXML
    protected MenuItem mInvisibleWindowMenuItem;

    @FXML
    protected MenuItem mResetPerspectiveMenuItem;

    @FXML
    protected MenuItem mInfoMenuItem;

    @FXML
    protected Pane mMenuArea;

    @FXML
    protected Label mUserHintLabel;

    protected MainWindow(ApplicationController applicationController, UiController uiController) {
        mApplicationController = applicationController;
        mUIController = uiController;
        FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource(FXML));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MainWindow create(ApplicationController applicationController, UiController uiController) {
        log.info("Creating main window");
        return new MainWindow(applicationController, uiController);
    }

    protected <T extends AbstractPlanView<?, ?>> Dockable<T> createDockableForView(T view, String viewId, String viewTitle) {
        Dockable<T> result = Dockable.of(view, viewId, viewTitle, true);
        result.setOnCloseRequestHandler(d -> {
            return view.canClose();
        });
        result.visibleProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    openView(view);
                } else {
                    closeView(view);
                }
            }
        });
        result.setFocusControl(view);
        result.floatingProperty().addListener((obs, oldVal, newVal) -> {
            checkViewState(view, result);
        });
        view.focusedProperty().addListener((obs, oldVal, newVal) -> {
            checkViewState(view, result);
        });
        return result;
    }

    protected void checkViewState(AbstractPlanView<?, ?> view, Dockable<?> dockable) {
        Pane viewMenuArea = view.getViewMenuArea();
        if (dockable.isFloating()) {
            if (view == mActiveView) {
                deactivateView();
            }
            view.setMenuArea(viewMenuArea);
            return;
        } else {
            if (view.isFocused()) {
                activateView(view);
            }
        }
    }

    // Creation/Restoration of views needs access to the scene, so this
    // method must be called when the stage has already been shown.
    protected void initializeViewsAndDockZones(Optional<ViewsLayoutState> oViewsLayoutState) {
        DockSystem.getFloatingStages().addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends DockableFloatingStage> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        c.getAddedSubList().forEach(st -> {
                            st.getScene().getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());
                        });
                    }
                }
            }
        });

        // Create the view lifecycle managers.
        ViewsRegistry viewsRegistry = DockSystem.getViewsRegistry();
        viewsRegistry.addView(mConstructionViewManager = new ViewLifecycleManager<>(VIEW_ID_CONSTRUCTION_AREA, true) {
            @Override
            protected Dockable<ConstructionView> createDockable() {
                return createDockableForView(mConstructionView, VIEW_ID_CONSTRUCTION_AREA, mConstructionView.getTitle());
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_EDITORS);
            }
        });
        viewsRegistry.addView(mThreeDViewManager = new ViewLifecycleManager<>(VIEW_ID_3D_PRESENTATION, true) {
            @Override
            protected Dockable<ThreeDView> createDockable() {
                return createDockableForView(mThreeDView, VIEW_ID_3D_PRESENTATION, mThreeDView.getTitle());
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_EDITORS);
            }
        });
        viewsRegistry.addView(mPropertiesViewManager = new ViewLifecycleManager<>(VIEW_ID_PROPERTIES, true) {
            @Override
            protected Dockable<PropertiesControl> createDockable() {
                Dockable<PropertiesControl> result = Dockable.of(mPropertiesControl, VIEW_ID_PROPERTIES, Strings.PROPERTIES_VIEW_TITLE, false);
                return result;
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_VIEWS_1);
            }
        });
        viewsRegistry.addView(mInteractionsPaneViewManager = new ViewLifecycleManager<>(VIEW_ID_INTERACTIONS_PANE, true) {
            @Override
            protected Dockable<Parent> createDockable() {
                Dockable<Parent> result = Dockable.of(mInteractionsPaneParent, VIEW_ID_INTERACTIONS_PANE, "-", false);
                return result;
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                ViewLifecycleManager<?> propertiesView = viewsRegistry.getViewLifecycleManager(VIEW_ID_PROPERTIES);
                if (propertiesView != null) {
                    Optional<AbstractDockableViewLocationDescriptor> vld = propertiesView.getCurrentViewLocation().map(vl -> vl.createDescriptor()).or(() -> propertiesView.getLastViewLocationDescriptor());
                    if (vld.isPresent()) {
                        // If properties view is present, try to dock at the same tab dock host
                        return vld;
                    }
                }
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_VIEWS_1);
            }
        });
        viewsRegistry.addView(mObjectTreeViewManager = new ViewLifecycleManager<>(VIEW_ID_OBJECT_TREE, true) {
            @Override
            protected Dockable<ObjectTreeControl> createDockable() {
                Dockable<ObjectTreeControl> result = Dockable.of(mObjectTreeControl, VIEW_ID_OBJECT_TREE, Strings.OBJECT_TREE_VIEW_TITLE, false);
                return result;
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_VIEWS_2);
            }
        });
        viewsRegistry.addView(mScriptConsoleViewManager = new ViewLifecycleManager<>(VIEW_ID_SCRIPT_CONSOLE, true) {
            @Override
            protected Dockable<ScriptConsoleControl> createDockable() {
                Dockable<ScriptConsoleControl> result = Dockable.of(mScriptConsoleControl, VIEW_ID_SCRIPT_CONSOLE, Strings.SCRIPT_CONSOLE_VIEW_TITLE, true);
                return result;
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_ADDITIONAL_VIEWS);
            }
        });
        viewsRegistry.addView(mLogOutputViewManager = new ViewLifecycleManager<>(VIEW_ID_LOG_OUTPUT, true) {
            @Override
            protected Dockable<LogOutputControl> createDockable() {
                Dockable<LogOutputControl> result = Dockable.of(mLogOutputControl, VIEW_ID_LOG_OUTPUT, Strings.LOG_OUTPUT_VIEW_TITLE, true);
                return result;
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_ADDITIONAL_VIEWS);
            }
        });

        Map<String, AbstractDockLayoutDescriptor> dockAreaLayouts = new HashMap<>();
        dockAreaLayouts.put(MAIN_DOCK_HOST_ID,
            new SashLayoutDescriptor(DOCK_ZONE_ID_MAIN,
                Orientation.Horizontal, 0.15,
                new SashLayoutDescriptor(DOCK_ZONE_ID_MAIN_LEFT,
                    Orientation.Vertical, 0.5,
                    new TabHostLayoutDescriptor(DOCK_ZONE_ID_MAIN_LEFT_UPPER, TAB_DOCK_HOST_ID_VIEWS_1),
                    new TabHostLayoutDescriptor(DOCK_ZONE_ID_MAIN_LEFT_LOWER, TAB_DOCK_HOST_ID_VIEWS_2)),
                new SashLayoutDescriptor(DOCK_ZONE_ID_MAIN_CENTER,
                    Orientation.Vertical, 0.7,
                    new TabHostLayoutDescriptor(DOCK_ZONE_ID_MAIN_CENTER_MAIN, TAB_DOCK_HOST_ID_EDITORS),
                    new TabHostLayoutDescriptor(DOCK_ZONE_ID_MAIN_CENTER_BOTTOM, TAB_DOCK_HOST_ID_ADDITIONAL_VIEWS))));
        PerspectiveDescriptor perspectiveLayout = new PerspectiveDescriptor(
            dockAreaLayouts,
            Collections.emptyList(),
            Arrays.asList(
                VIEW_ID_CONSTRUCTION_AREA,
                VIEW_ID_3D_PRESENTATION,
                VIEW_ID_PROPERTIES,
                VIEW_ID_OBJECT_TREE,
                VIEW_ID_SCRIPT_CONSOLE,
                VIEW_ID_LOG_OUTPUT));

        DockSystem.setPerspectiveLayout(perspectiveLayout);

        // Step 2: Create all needed dock host controls and install the controls in the node hierarchy.
        // When restoring the dock host control, all views and dock zones must have been declared.
        mMainDockHost = DockAreaControl.create(MAIN_DOCK_HOST_ID);
        mDockHostParent.getChildren().add(mMainDockHost);

        // Step 3: Restore or initialize perspective layout
        if (oViewsLayoutState.isPresent()) {
            ViewsLayoutState viewsLayoutState = oViewsLayoutState.get();
            DockSystem.restoreLayoutState(viewsLayoutState);
        } else {
            DockSystem.resetPerspective();
        }

        mApplicationController.planProperty().addListener(new ChangeListener<Plan>() {
            @Override
            public void changed(ObservableValue<? extends Plan> observable, Plan oldValue, Plan newValue) {
                for (AbstractPlanView<? extends IModelBasedObject, ? extends Node> planView : mPlanViews) {
                    if (planView.isAlive()) {
                        closeView(planView);
                        openView(planView);
                    }
                }
                updateRecentFilesMenu();
            }
        });

        updateRecentFilesMenu();

        Platform.runLater(() -> {
            if (mConstructionViewManager.getDockable().map(d -> d.isVisible()).orElse(false)) {
                mConstructionView.requestFocus();
            } else if (mThreeDViewManager.getDockable().map(d -> d.isVisible()).orElse(false)) {
                mThreeDView.requestFocus();
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mConstructionView = new ConstructionView(mUIController);
        mThreeDView = new ThreeDView(mUIController);
        mPlanViews.add(mConstructionView);
        mPlanViews.add(mThreeDView);
        mPropertiesControl = PropertiesControl.create();
        mObjectTreeControl = ObjectTreeControl.create();
        mObjectTreeControl.setObjectsVisibilityChanger((objs, hidden) -> {
            mUIController.setObjectsVisibility(objs, hidden);
        });
        mLogOutputControl = LogOutputControl.create();
        mScriptConsoleControl = ScriptConsoleControl.create(mUIController, mLogOutputControl.getLogOutputWriter());

        mFileNewMenuItem.setOnAction(this::onFileNewAction);
        mFileOpenMenuItem.setOnAction(this::onFileOpenAction);
        mFileSaveMenuItem.setOnAction(this::onFileSaveAction);
        mFileSaveAsMenuItem.setOnAction(this::onFileSaveAsAction);
        mFileQuitMenuItem.setOnAction(this::onFileQuitAction);

        mUndoMenuItem.setOnAction(this::onUndoAction);
        mUIController.nextUndoOperationProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends ChangeEntry> observable, ChangeEntry oldValue, ChangeEntry newValue) {
                updateUndoMenuItem(newValue);
            }
        });
        mRedoMenuItem.setOnAction(this::onRedoAction);
        mUIController.nextRedoOperationProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends ChangeEntry> observable, ChangeEntry oldValue, ChangeEntry newValue) {
                updateRedoMenuItem(newValue);
            }
        });
        updateUndoMenuItem(null);
        updateRedoMenuItem(null);

        mOpenLibraryManagerMenuItem.setOnAction(this::onOpenLibraryManagerAction);
        ObservableList<MenuItem> windowMenuItems = mWindowMenu.getItems();
        int i = windowMenuItems.indexOf(mInvisibleWindowMenuItem);
        windowMenuItems.addAll(i + 1, createWindowMenuItems());
        mResetPerspectiveMenuItem.setOnAction(this::onResetPerspectiveAction);
        mInfoMenuItem.setOnAction(this::onInfoAction);
    }

    protected List<MenuItem> createWindowMenuItems() {
        MenuItem constructionViewItem = new MenuItem(Strings.WINDOW_MENU_ITEM_CONSTRUCTION_VIEW);
        constructionViewItem.setOnAction(event -> {
            mConstructionViewManager.ensureVisible(getStage(), true);
        });
        MenuItem threeDViewItem = new MenuItem(Strings.WINDOW_MENU_ITEM_3D_VIEW);
        threeDViewItem.setOnAction(event -> {
            mThreeDViewManager.ensureVisible(getStage(), true);
        });
        MenuItem propertiesViewItem = new MenuItem(Strings.WINDOW_MENU_ITEM_PROPERTIES_VIEW);
        propertiesViewItem.setOnAction(event -> {
            mPropertiesViewManager.ensureVisible(getStage(), true);
        });
        MenuItem objectsViewItem = new MenuItem(Strings.WINDOW_MENU_ITEM_OBJECTS_VIEW);
        objectsViewItem.setOnAction(event -> {
            mObjectTreeViewManager.ensureVisible(getStage(), true);
        });
        MenuItem scriptConsoleViewItem = new MenuItem(Strings.WINDOW_MENU_ITEM_SCRIPT_CONSOLE_VIEW);
        scriptConsoleViewItem.setOnAction(event -> {
            mScriptConsoleViewManager.ensureVisible(getStage(), true);
        });
        MenuItem logOutputViewItem = new MenuItem(Strings.WINDOW_MENU_ITEM_LOG_OUTPUT_VIEW);
        logOutputViewItem.setOnAction(event -> {
            mLogOutputViewManager.ensureVisible(getStage(), true);
        });
        return Arrays.asList(
            constructionViewItem,
            threeDViewItem,
            propertiesViewItem,
            objectsViewItem,
            scriptConsoleViewItem,
            logOutputViewItem);
    }

    protected void updateUndoMenuItem(ChangeEntry nextOperation) {
        mUndoMenuItem.setDisable(nextOperation == null);
        mUndoMenuItem.setText(nextOperation == null ? Strings.MAIN_WINDOW_UNDO_MENU_ITEM_INVALID : MessageFormat.format(Strings.MAIN_WINDOW_UNDO_MENU_ITEM_VALID, nextOperation.getChangeDescription()));
    }

    protected void updateRedoMenuItem(ChangeEntry nextOperation) {
        mRedoMenuItem.setDisable(nextOperation == null);
        mRedoMenuItem.setText(nextOperation == null ? Strings.MAIN_WINDOW_REDO_MENU_ITEM_INVALID : MessageFormat.format(Strings.MAIN_WINDOW_REDO_MENU_ITEM_VALID, nextOperation.getChangeDescription()));
    }

    public void initializeAfterShow() {
        Platform.runLater(() -> {
            Optional<ViewsLayoutState> oViewsLayoutState = Optional.empty();
            try {
                Path settingsPath = Paths.get(ViewsLayoutStateIO.DEFAULT_FILE_NAME);
                if (Files.exists(settingsPath)) {
                    try (BufferedReader br = Files.newBufferedReader(settingsPath)) {
                        oViewsLayoutState = Optional.of(ViewsLayoutStateIO.deserialize(br));
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to load settings", e);
            }
            initializeViewsAndDockZones(oViewsLayoutState);
        });
    }

    public void saveSettings() {
        try {
            ViewsLayoutState viewsLayoutState = DockSystem.saveLayoutState();

            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(ViewsLayoutStateIO.DEFAULT_FILE_NAME))) {
                ViewsLayoutStateIO.serialize(viewsLayoutState, bw);
            }
        } catch (Exception e) {
            log.warn("Unable to store settings", e);
        }
    }

    public void shutdown() {
        saveSettings();
    }

    public void updateRecentFilesMenu() {
        ObservableList<MenuItem> items = mFileRecentMenu.getItems();
        items.clear();
        Configuration config = mApplicationController.getConfig();
        List<Path> planFilesHistory = config.getPlanFilesHistory();
        for (Path path : planFilesHistory) {
            String planName = ApplicationController.getPlanName(path);
            Label l = new Label(planName);
            Tooltip tt = new Tooltip(path.toString());
            Tooltip.install(l, tt);
            CustomMenuItem item = new CustomMenuItem(l);
            item.setOnAction(event -> {
                if (mApplicationController.queryLoadPlanFile(getStage(), path)) {
                    updateRecentFilesMenu();
                }
            });
            items.add(item);
        }
        items.add(new SeparatorMenuItem());
        if (!planFilesHistory.isEmpty()) {
            MenuItem item = new MenuItem(Strings.PLAN_FILES_HISTORY_CLEAR_HISTORY_MENU_ITEM);
            item.setOnAction(event -> {
                config.clearPlanFilesHistory();
                updateRecentFilesMenu();
            });
            items.add(item);
        }
    }

    public MainWindowState getMainWindowState() {
        MainWindowState result = new MainWindowState();
        for (AbstractPlanView<? extends IModelBasedObject, ? extends Node> planView : mPlanViews) {
            Optional<? extends ViewState> oVs = planView.getViewState();
            if (oVs.isPresent()) {
                result.addViewState(oVs.get());
            }
        }
        return result;
    }

    public void setMainWindowState(MainWindowState mainWindowState) {
        for (AbstractPlanView<? extends IModelBasedObject, ? extends Node> planView : mPlanViews) {
            Class<? extends ViewState> vsc = planView.getViewStateClass();
            if (vsc == null) {
                continue;
            }
            Optional<? extends ViewState> oVS = mainWindowState.getViewState(vsc);
            oVS.ifPresent(vs -> {
                planView.setViewState(vs);
            });
        }
    }

    protected void addInteractionsControl(Node control, String title, boolean bringToFront) {
        ObservableList<Node> children = mInteractionsPaneParent.getChildren();
        if (children.contains(control)) {
            return;
        }
        removeInteractionsControl();
        children.add(control);
        mInteractionsPaneViewManager.ensureVisible(getStage(), false);
        mInteractionsPaneViewManager.getDockable().ifPresent(d -> {
            d.setDockableTitle(title);
            if (bringToFront) {
                // TODO
            }
        });
    }

    protected void removeInteractionsControl() {
        mInteractionsPaneParent.getChildren().clear();
        mInteractionsPaneViewManager.close();
    }

    protected void deactivateView() {
        if (mActiveView == null) {
            return;
        }
        mActiveView.behaviorProperty().removeListener(VIEW_BEHAVIOR_LISTENER);
        unbindViewBehavior();
        mMenuArea.getChildren().remove(mActiveView.getViewMenuArea());
        mActiveView = null;
        mUIController.setCurrentView(null);
    }

    /**
     * Activates the given view as result of a focus change.
     */
    protected void activateView(AbstractPlanView<? extends IModelBasedObject, ? extends Node> view) {
        if (mActiveView == view) {
            return;
        }
        deactivateView();
        mActiveView = view;
        mUIController.setCurrentView(view);
        AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> behavior = view.getBehavior();
        mMenuArea.getChildren().add(mActiveView.getViewMenuArea());
        bindViewBehavior(behavior);
        view.behaviorProperty().addListener(VIEW_BEHAVIOR_LISTENER);
    }

    /**
     * Hides the given view, also deactivating it.
     */
    protected void closeView(AbstractPlanView<? extends IModelBasedObject, ? extends Node> view) {
        if (view == mActiveView) {
            deactivateView();
        }
        view.dispose();
    }

    /**
     * Shows the given view.
     */
    protected void openView(AbstractPlanView<? extends IModelBasedObject, ? extends Node> view) {
        view.revive();
        if (mActiveView == null) {
            activateView(view);
        }
    }

    protected void unbindViewBehavior() {
        if (mViewBehavior != null) {
            mViewBehavior.interactionsControlProperty().removeListener(INTERACTIONS_CONTROL_LISTENER);
            mViewBehavior = null;
        }
        mUserHintLabel.textProperty().unbind();
        mUserHintLabel.setText("");
        removeInteractionsControl();
    }

    protected void bindViewBehavior(AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> behavior) {
        unbindViewBehavior();
        if (behavior == null) {
            return;
        }
        mViewBehavior = behavior;
        mUserHintLabel.textProperty().bind(behavior.userHintProperty());
        InteractionsControl interactionsControl = behavior.getInteractionsControl();
        if (interactionsControl != null) {
            addInteractionsControl(interactionsControl.getControl(), interactionsControl.getTitle(), interactionsControl.hasPriority());
        }
        behavior.interactionsControlProperty().addListener(INTERACTIONS_CONTROL_LISTENER);
    }

    public void show(Stage primaryStage) {
        Scene scene = new Scene(mRoot, WINDOW_SIZE_X, WINDOW_SIZE_Y);

        scene.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public PropertiesControl getPropertiesControl() {
        return mPropertiesControl;
    }

    public ObjectTreeControl getObjectTreeControl() {
        return mObjectTreeControl;
    }

    public Window getStage() {
        return mRoot.getScene().getWindow();
    }

    protected void onFileNewAction(ActionEvent event) {
        mApplicationController.queryNewPlan(getStage());
    }

    protected void onFileOpenAction(ActionEvent event) {
        mApplicationController.queryOpen(getStage());
        updateRecentFilesMenu();
    }

    protected void onFileSaveAction(ActionEvent event) {
        mApplicationController.saveOrQueryPath(getStage());
    }

    protected void onFileSaveAsAction(ActionEvent event) {
        mApplicationController.querySaveAs(getStage());
        updateRecentFilesMenu();
    }

    protected void onFileQuitAction(ActionEvent event) {
        mApplicationController.queryQuitApplication(getStage());
    }

    protected void onUndoAction(ActionEvent event) {
        mUIController.undo();
    }

    protected void onRedoAction(ActionEvent event) {
        mUIController.redo();
    }

    protected void onOpenLibraryManagerAction(ActionEvent event) {
        LibraryManagerMainWindow window = LibraryManagerMainWindow.create(mApplicationController.getAssetManager().buildAssetLoader());
        window.show(new Stage());
        window.reloadAssets();
    }

    protected void onResetPerspectiveAction(ActionEvent event) {
        DockSystem.resetPerspective();
    }

    protected void onInfoAction(ActionEvent event) {
        Alert dialog = new Alert(AlertType.INFORMATION);
        dialog.setTitle(Strings.INFO_DIALOG_TITLE_TEXT);
        dialog.setHeaderText(Strings.INFO_DIALOG_HEADER_TEXT);
        dialog.setContentText(Strings.INFO_DIALOG_CONTENT_TEXT);
        dialog.showAndWait();
    }
}
