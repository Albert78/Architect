/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.ui.ApplicationController;
import de.dh.cad.architect.ui.Configuration;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.IModelBasedObject;
import de.dh.cad.architect.ui.objecttree.ObjectTreeControl;
import de.dh.cad.architect.ui.persistence.MainWindowState;
import de.dh.cad.architect.ui.persistence.ViewState;
import de.dh.cad.architect.ui.properties.PropertiesControl;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.libraries.LibraryManagerMainWindow;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.fx.viewsfx.AbstractDockableViewLocation;
import de.dh.utils.fx.viewsfx.DockHostControl;
import de.dh.utils.fx.viewsfx.DockSide;
import de.dh.utils.fx.viewsfx.DockSystem;
import de.dh.utils.fx.viewsfx.DockViewLocation;
import de.dh.utils.fx.viewsfx.Dockable;
import de.dh.utils.fx.viewsfx.DockableFloatingStage;
import de.dh.utils.fx.viewsfx.IDockZoneProvider;
import de.dh.utils.fx.viewsfx.TabDockHost;
import de.dh.utils.fx.viewsfx.ViewsRegistry;
import de.dh.utils.fx.viewsfx.ViewsRegistry.ViewLifecycleManager;
import de.dh.utils.fx.viewsfx.io.DesktopSettings;
import de.dh.utils.fx.viewsfx.io.DesktopSettingsIO;
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

    public static final String MAIN_DOCK_HOST_ID = "Dock-Host";

    public static final String DOCK_ZONE_ID_MAIN = "Main";
    public static final String DOCK_ZONE_ID_LEFT_UPPER = "LeftUpper";
    public static final String DOCK_ZONE_ID_LEFT_LOWER = "LeftLower";
    public static final String DOCK_ZONE_ID_BOTTOM = "Bottom";

    public static final String VIEW_ID_PROPERTIES = "Properties";
    public static final String VIEW_ID_INTERACTIONS_PANE = "InteractionsPane";
    public static final String VIEW_ID_OBJECT_TREE = "ObjectTree";
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

    protected ViewLifecycleManager<ConstructionView> mConstructionViewManager = null;
    protected ViewLifecycleManager<ThreeDView> mThreeDViewManager = null;
    protected ViewLifecycleManager<PropertiesControl> mPropertiesViewManager = null;
    protected ViewLifecycleManager<Parent> mInteractionsPaneViewManager = null;
    protected ViewLifecycleManager<ObjectTreeControl> mObjectTreeViewManager = null;

    protected final List<AbstractPlanView<? extends IModelBasedObject, ? extends Node>> mPlanViews = new ArrayList<>();
    protected AbstractPlanView<? extends IModelBasedObject, ? extends Node> mActiveView = null;
    protected AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> mViewBehavior = null;
    protected StackPane mInteractionsPaneParent = new StackPane();

    protected DockHostControl mMainDockHost = null;

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
    protected MenuItem mOpenLibraryManagerMenuItem;

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
    protected void initializeViewsAndDockZones(Optional<DesktopSettings> oSettings) {
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
            protected Optional<AbstractDockableViewLocation> getOrCreatePreferredViewLocation(IDockZoneProvider dzp) {
                return dzp.getOrTryCreateDockZone(DOCK_ZONE_ID_MAIN).map(DockViewLocation::new);
            }
        });
        viewsRegistry.addView(mThreeDViewManager = new ViewLifecycleManager<>(VIEW_ID_3D_PRESENTATION, true) {
            @Override
            protected Dockable<ThreeDView> createDockable() {
                return createDockableForView(mThreeDView, VIEW_ID_3D_PRESENTATION, mThreeDView.getTitle());
            }

            @Override
            protected Optional<AbstractDockableViewLocation> getOrCreatePreferredViewLocation(IDockZoneProvider dzp) {
                return dzp.getOrTryCreateDockZone(DOCK_ZONE_ID_MAIN).map(DockViewLocation::new);
            }
        });
        viewsRegistry.addView(mPropertiesViewManager = new ViewLifecycleManager<>(VIEW_ID_PROPERTIES, true) {
            @Override
            protected Dockable<PropertiesControl> createDockable() {
                Dockable<PropertiesControl> result = Dockable.of(mPropertiesControl, VIEW_ID_PROPERTIES, Strings.PROPERTIES_VIEW_TITLE, false);
                return result;
            }

            @Override
            protected Optional<AbstractDockableViewLocation> getOrCreatePreferredViewLocation(IDockZoneProvider dzp) {
                return dzp.getOrTryCreateDockZone(DOCK_ZONE_ID_LEFT_UPPER).map(DockViewLocation::new);
            }
        });
        viewsRegistry.addView(mInteractionsPaneViewManager = new ViewLifecycleManager<>(VIEW_ID_INTERACTIONS_PANE, true) {
            @Override
            protected Dockable<Parent> createDockable() {
                Dockable<Parent> result = Dockable.of(mInteractionsPaneParent, VIEW_ID_INTERACTIONS_PANE, "-", false);
                return result;
            }

            @Override
            protected Optional<AbstractDockableViewLocation> getOrCreatePreferredViewLocation(IDockZoneProvider dzp) {
                return dzp.getOrTryCreateDockZone(DOCK_ZONE_ID_LEFT_UPPER).map(DockViewLocation::new);
            }
        });
        viewsRegistry.addView(mObjectTreeViewManager = new ViewLifecycleManager<>(VIEW_ID_OBJECT_TREE, true) {
            @Override
            protected Dockable<ObjectTreeControl> createDockable() {
                Dockable<ObjectTreeControl> result = Dockable.of(mObjectTreeControl, VIEW_ID_OBJECT_TREE, Strings.OBJECT_TREE_VIEW_TITLE, false);
                return result;
            }

            @Override
            protected Optional<AbstractDockableViewLocation> getOrCreatePreferredViewLocation(IDockZoneProvider dzp) {
                return dzp.getOrTryCreateDockZone(DOCK_ZONE_ID_LEFT_LOWER).map(DockViewLocation::new);
            }
        });

        // Step 2: Create all needed dock host controls using builder instances and install the controls in the node hierarchy.
        DockHostControl.Builder builder = new DockHostControl.Builder(MAIN_DOCK_HOST_ID, DOCK_ZONE_ID_MAIN)
                .addDockZone(DOCK_ZONE_ID_BOTTOM, (dh, dzp) -> {
                    TabDockHost mainDockHost = dzp.getOrTryCreateDockZone(DOCK_ZONE_ID_MAIN).orElseGet(() -> dh.getFirstLeaf());
                    return mainDockHost.split(DockSide.South, DOCK_ZONE_ID_BOTTOM, 0.7);
                })

                .addDockZone(DOCK_ZONE_ID_LEFT_UPPER, (dh, dzp) -> {
                    TabDockHost mainDockHost = dzp.getOrTryCreateDockZone(DOCK_ZONE_ID_MAIN).orElseGet(() -> dh.getFirstLeaf());
                    return mainDockHost.split(DockSide.West, DOCK_ZONE_ID_LEFT_UPPER, 0.2);
                })

                .addDockZone(DOCK_ZONE_ID_LEFT_LOWER, (dh, dzp) -> {
                    TabDockHost mainDockHost = dzp.getOrTryCreateDockZone(DOCK_ZONE_ID_LEFT_UPPER).orElseGet(() -> dh.getFirstLeaf());
                    return mainDockHost.split(DockSide.South, DOCK_ZONE_ID_LEFT_LOWER, 0.5);
                });
        // When restoring the dock host control, all views and dock zones must have been declared.
        mMainDockHost = builder.createOrRestoreFromSettings(oSettings);
        mDockHostParent.getChildren().add(mMainDockHost);

        // Step 3: Restore or initialize default floating views.
        if (oSettings.isPresent()) {
            DesktopSettings settings = oSettings.get();
            viewsRegistry.restoreFloatingViews(settings, getStage());
        } else {
            showDefaultViews();
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

    protected void showDefaultViews() {
        Stage stage = mApplicationController.getPrimaryStage();
        mConstructionViewManager.ensureVisible(stage);
        mThreeDViewManager.ensureVisible(stage);
        mPropertiesViewManager.ensureVisible(stage);
        mObjectTreeViewManager.ensureVisible(stage);
    }

    public void saveSettings() {
        try {
            DesktopSettings settings = new DesktopSettings();

            // For each dock host control:
            mMainDockHost.saveViewHierarchy(settings);

            DockSystem.getViewsRegistry().storeFloatingViewsSettings(settings);

            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(DesktopSettingsIO.DEFAULT_SETTINGS_FILE_NAME))) {
                DesktopSettingsIO.serializeDesktopSettings(settings, bw);
            }
        } catch (Exception e) {
            log.warn("Unable to store settings", e);
        }
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

        mFileNewMenuItem.setOnAction(this::onFileNewAction);
        mFileOpenMenuItem.setOnAction(this::onFileOpenAction);
        mFileSaveMenuItem.setOnAction(this::onFileSaveAction);
        mFileSaveAsMenuItem.setOnAction(this::onFileSaveAsAction);
        mFileQuitMenuItem.setOnAction(this::onFileQuitAction);
        mOpenLibraryManagerMenuItem.setOnAction(this::onOpenLibraryManagerAction);
    }

    public void initializeAfterShow() {
        Platform.runLater(() -> {
            Optional<DesktopSettings> oSettings = Optional.empty();
            try {
                Path settingsPath = Paths.get(DesktopSettingsIO.DEFAULT_SETTINGS_FILE_NAME);
                if (Files.exists(settingsPath)) {
                    try (BufferedReader br = Files.newBufferedReader(settingsPath)) {
                        oSettings = Optional.of(DesktopSettingsIO.deserializeDesktopSettings(br));
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to load settings", e);
            }
            initializeViewsAndDockZones(oSettings);
        });
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
        mInteractionsPaneViewManager.ensureVisible(getStage());
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

    protected void onOpenLibraryManagerAction(ActionEvent event) {
        LibraryManagerMainWindow window = LibraryManagerMainWindow.create(mApplicationController.getAssetManager().buildAssetLoader());
        window.show(new Stage());
        window.reloadAssets();
    }
}
