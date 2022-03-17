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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
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
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;

public class MainWindow implements Initializable {
    private ChangeListener<InteractionsTab> INTERACTIONS_TAB_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends InteractionsTab> observable, InteractionsTab oldValue, InteractionsTab newValue) {
            removeInteractionsTab();
            if (newValue != null) {
                addInteractionsTab(newValue.getTab(), newValue.hasPriority());
            }
        }
    };

    public static final int WINDOW_SIZE_X = 1000;
    public static final int WINDOW_SIZE_Y = 800;
    public static final int LEFT_AREA_WIDTH = 400;

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    protected static final String FXML = "MainWindow.fxml";

    protected final ApplicationController mApplicationController;
    protected final UiController mUIController;

    protected PropertiesControl mPropertiesControl;
    protected ObjectTreeControl mObjectTreeControl;

    protected final List<AbstractPlanView<? extends IModelBasedObject, ? extends Node>> mPlanViews = new ArrayList<>();

    protected AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> mViewBehavior = null;
    protected Menu mViewMenu = null;
    protected Label mBehaviorTitleLabel = null;
    protected ToolBar mBehaviorActionsToolBar = null;
    protected Tab mInteractionsTab = null;

    @FXML
    protected MenuBar mMenuBar;

    @FXML
    protected Parent mRoot;

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
    protected Menu mInsertionPointMenu;

    @FXML
    protected Pane mMenuArea;

    @FXML
    protected Pane mDefaultToolButtons;

    @FXML
    protected Pane mViewToolButtons;

    @FXML
    protected SplitPane mMainSplitPane;

    @FXML
    protected Accordion mLeftAccordion;

    @FXML
    protected TabPane mPropertiesInteractionsTabPane;

    @FXML
    protected Tab mPropertiesTab;

    @FXML
    protected BorderPane mPropertiesPane;

    @FXML
    protected BorderPane mObjectsTreePane;

    @FXML
    protected Label mUserHintLabel;

    @FXML
    protected TabPane mPlansTabPane;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mFileNewMenuItem.setOnAction(this::onFileNewAction);
        mFileOpenMenuItem.setOnAction(this::onFileOpenAction);
        mFileSaveMenuItem.setOnAction(this::onFileSaveAction);
        mFileSaveAsMenuItem.setOnAction(this::onFileSaveAsAction);
        mFileQuitMenuItem.setOnAction(this::onFileQuitAction);
        mOpenLibraryManagerMenuItem.setOnAction(this::onOpenLibraryManagerAction);

        mPropertiesControl = PropertiesControl.create();
        mPropertiesPane.setCenter(mPropertiesControl);
        mObjectTreeControl = ObjectTreeControl.create();
        mObjectTreeControl.setObjectsVisibilityChanger((objs, hidden) -> {
            mUIController.setObjectsVisibility(objs, hidden);
        });
        mObjectsTreePane.setCenter(mObjectTreeControl);

        addPlanView(new ConstructionView(mUIController));
        addPlanView(new ThreeDView(mUIController));
//        addPlanView(new Test3DPlanView(mUIController));
        //addPlanView(new Test2DPlanView(mUIController));
        SingleSelectionModel<Tab> selectionModel = mPlansTabPane.getSelectionModel();
        selectionModel.selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                if (oldValue != null) {
                    AbstractPlanView<? extends IModelBasedObject, ? extends Node> view = (AbstractPlanView<?, ?>) oldValue.getUserData();
                    closeView(view);
                }
                if (newValue != null) {
                    AbstractPlanView<? extends IModelBasedObject, ? extends Node> view = (AbstractPlanView<?, ?>) newValue.getUserData();
                    openView(view);
                }
            }
        });
        mApplicationController.planProperty().addListener(new ChangeListener<Plan>() {
            @Override
            public void changed(ObservableValue<? extends Plan> observable, Plan oldValue, Plan newValue) {
                closeView(getActivePlanView());
                openView(getActivePlanView());
                updateRecentFilesMenu();
            }
        });

        updateRecentFilesMenu();
    }

    public void initializeAfterShow() {
        Platform.runLater(() -> {
            double mainWidth = mMainSplitPane.getWidth();
            double leftAreaWidth = mApplicationController.getConfig().getLastMainWindowLeftAreaWidth().orElse(LEFT_AREA_WIDTH);
            double first = leftAreaWidth / mainWidth;
            mMainSplitPane.setDividerPositions(first, 1 - first);
        });
    }

    public void shutdown() {
        try {
            mApplicationController.getConfig().setLastMainWindowLeftAreaWidth((int) (mMainSplitPane.getWidth() * mMainSplitPane.getDividerPositions()[0]));
        } catch (Exception e) {
            log.warn("Unable to write settings", e);
        }
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

    public void initView() {
        openView(getActivePlanView());
    }

    public List<AbstractPlanView<? extends IModelBasedObject, ? extends Node>> getPlanViews() {
        return mPlanViews;
    }

    public AbstractPlanView<? extends IModelBasedObject, ? extends Node> getActivePlanView() {
        return (AbstractPlanView<?, ?>) mPlansTabPane.getSelectionModel().getSelectedItem().getUserData();
    }

    protected void addInteractionsTab(Tab tab, boolean bringToFront) {
        if (tab.equals(mInteractionsTab)) {
            return;
        }
        removeInteractionsTab();
        mInteractionsTab = tab;
        ObservableList<Tab> tabs = mPropertiesInteractionsTabPane.getTabs();
        tabs.add(mInteractionsTab);
        if (bringToFront) {
            mPropertiesInteractionsTabPane.getSelectionModel().select(mInteractionsTab);
        }
    }

    protected void removeInteractionsTab() {
        if (mInteractionsTab != null) {
            mPropertiesInteractionsTabPane.getTabs().remove(mInteractionsTab);
            mInteractionsTab = null;
        }
    }

    protected void closeView(AbstractPlanView<? extends IModelBasedObject, ? extends Node> view) {
        unbindViewBehavior();
        unbindView();
        mUIController.setCurrentView(null);
        view.dispose();
    }

    protected void openView(AbstractPlanView<? extends IModelBasedObject, ? extends Node> view) {
        try {
            view.revive();
            mUIController.setCurrentView(view);
            view.behaviorProperty().addListener(new ChangeListener<AbstractViewBehavior<? extends IModelBasedObject, ? extends Node>>() {
                @Override
                public void changed(ObservableValue<? extends AbstractViewBehavior<? extends IModelBasedObject, ? extends Node>> observable,
                    AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> oldValue,
                    AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> newValue) {
                    unbindViewBehavior();
                    bindViewBehavior(newValue);
                }
            });
            AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> behavior = view.getBehavior();
            bindView(view);
            bindViewBehavior(behavior);
        } catch (Exception e) {
            log.error("Error opening view '" + view + "'", e);
        }
    }

    protected void unbindView() {
        mViewToolButtons.getChildren().clear();
    }

    protected void bindView(AbstractPlanView<? extends IModelBasedObject, ? extends Node> view) {
        if (view == null) {
            return;
        }
        ObservableList<Node> viewToolButtons = mViewToolButtons.getChildren();
        viewToolButtons.addAll(view.getToolBarContributionItems());
    }

    protected void unbindViewBehavior() {
        if (mViewBehavior != null) {
            mViewBehavior.interactionsTabProperty().removeListener(INTERACTIONS_TAB_LISTENER);
            mViewBehavior = null;
        }
        if (mViewMenu != null) {
            mMenuBar.getMenus().remove(mViewMenu);
            mViewMenu = null;
        }
        mUserHintLabel.textProperty().unbind();
        mUserHintLabel.setText("");
        removeInteractionsTab();
        ObservableList<Node> menuAreaChildren = mMenuArea.getChildren();
        if (mBehaviorActionsToolBar != null) {
            menuAreaChildren.remove(mBehaviorActionsToolBar);
            mBehaviorActionsToolBar = null;
        }
        if (mBehaviorTitleLabel != null) {
            menuAreaChildren.remove(mBehaviorTitleLabel);
            mBehaviorTitleLabel = null;
        }
    }

    protected void bindViewBehavior(AbstractViewBehavior<? extends IModelBasedObject, ? extends Node> behavior) {
        unbindViewBehavior();
        if (behavior == null) {
            return;
        }
        mViewBehavior = behavior;
        mViewMenu = behavior.getBehaviorMenu();
        if (mViewMenu != null) {
            ObservableList<Menu> menus = mMenuBar.getMenus();
            int index = menus.indexOf(mInsertionPointMenu);
            menus.add(index + 1, mViewMenu);
        }
        mUserHintLabel.textProperty().bind(behavior.userHintProperty());
        InteractionsTab interactionsTab = behavior.getInteractionsTab();
        if (interactionsTab != null) {
            addInteractionsTab(interactionsTab.getTab(), interactionsTab.hasPriority());
        }
        behavior.interactionsTabProperty().addListener(INTERACTIONS_TAB_LISTENER);
        ObservableList<Node> menuAreaChildren = mMenuArea.getChildren();
        mBehaviorActionsToolBar = behavior.getActionsToolBar();
        mBehaviorTitleLabel = new Label(behavior.getTitle());
        mBehaviorTitleLabel.setPadding(new Insets(5, 5, 0, 5));
        mBehaviorTitleLabel.setStyle(Constants.BEHAVIOR_TITLE_STYLE);
        menuAreaChildren.add(mBehaviorTitleLabel);
        if (mBehaviorActionsToolBar != null) {
            menuAreaChildren.add(mBehaviorActionsToolBar);
        }
    }

    public void addPlanView(AbstractPlanView<? extends IModelBasedObject, ? extends Node> view) {
        mPlanViews.add(view);
        ObservableList<Tab> tabs = mPlansTabPane.getTabs();
        Tab tab = new Tab(view.getTitle());
        tab.setUserData(view);
        tab.setOnCloseRequest(event -> {
            if (!view.canClose()) {
                event.consume();
            }
        });
        tab.setOnClosed(event -> {
            closeView(view);
        });
        tab.setContent(view);
        tabs.add(tab);
    }

    public void removePlanView(AbstractPlanView<? extends IModelBasedObject, ? extends Node> view) {
        mPlanViews.remove(view);
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
