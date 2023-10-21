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
package de.dh.cad.architect.ui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.controller.ObjectsChangeHandler;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.persistence.PlanFile;
import de.dh.cad.architect.ui.persistence.PlanFileIO;
import de.dh.cad.architect.utils.vfs.PlainFileSystemDirectoryLocator;
import de.dh.utils.fx.StageState;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Root application controller, responsible for managing the open file, the configuration and the UI controller.
 */
public class ApplicationController {
    private static Logger log = LoggerFactory.getLogger(ApplicationController.class);

    protected final Stage mPrimaryStage;
    protected final Configuration mConfig;
    protected final ObjectProperty<Plan> mPlanProperty = new SimpleObjectProperty<>();
    protected final ObjectProperty<Path> mPlanFilePathProperty = new SimpleObjectProperty<>();
    protected final ObjectProperty<Boolean> mDirtyProperty = new SimpleObjectProperty<>(false);
    protected final UiController mUiController;
    protected final AssetManager mAssetManager;

    public ApplicationController(Configuration config, Stage primaryStage) {
        mPrimaryStage = primaryStage;
        mConfig = config;
        mUiController = new UiController(mPlanProperty, mConfig);
        mUiController.addChangeHandler(new ObjectsChangeHandler() {
            @Override
            public void objectsRemoved(Collection<BaseObject> removedObjects) {
                setDirty(true);
            }

            @Override
            public void objectsChanged(Collection<BaseObject> changedObjects) {
                setDirty(true);
            }

            @Override
            public void objectsAdded(Collection<BaseObject> addedObjects) {
                setDirty(true);
            }
        });
        mAssetManager = AssetManager.create();
    }

    public static ApplicationController create(Configuration config, Stage primaryStage) {
        log.info("Creating application controller");
        ApplicationController result = new ApplicationController(config, primaryStage);
        result.setPlan(Plan.newPlan(), null); // To avoid initialization issues in main window, which always needs an active plan object
        return result;
    }

    protected void saveStageState() {
        Optional<StageState> oFormerStageState = StageState.buildStageState(mConfig.getLastWindowState());
        String stageState = StageState.fromStage(mPrimaryStage, oFormerStageState).serializeState();
        mConfig.setLastWindowState(stageState);
    }

    protected void restoreStageState() {
        Optional<StageState> oFormerStageState = StageState.buildStageState(mConfig.getLastWindowState());
        oFormerStageState.ifPresent(formerStageState -> {
            formerStageState.applyToStage(mPrimaryStage);
        });
    }

    public void startup() {
        ChangeListener<Object> updateTitleChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                updateTitle();
            }
        };
        ChangeListener<Plan> updateAssetManagerListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Plan> observable, Plan oldValue, Plan newValue) {
                // Attention: Ensure that mPlanFilePath is always set before setting mPlanProperty
                mAssetManager.setCurrentPlan(newValue.getId(), new PlainFileSystemDirectoryLocator(getPlanFilePath()));
            }
        };
        mPlanFilePathProperty.addListener(updateTitleChangeListener);
        mPlanProperty.addListener(updateAssetManagerListener);
        mDirtyProperty.addListener(updateTitleChangeListener);

        mAssetManager.start();
        mUiController.initialize(mPrimaryStage, this);
        restoreStageState();

        updateTitle();

        Path path = mConfig.getLastPlanFilePath();
        if (path == null) {
            newPlan();
        } else {
            try {
                loadPlanFile(path);
            } catch (Exception e) {
                log.error("Unable to load last plan '" + path + "', starting with a new one", e);
                newPlan();
            }
        }
    }

    public void shutdown() {
        log.info("Exiting from application");
        mUiController.shutdown();
        mAssetManager.shutdown();
        saveStageState();
        Platform.exit();
    }

    public static String getPlanName(Path planFilePath) {
        return planFilePath.getParent().getFileName().toString();
    }

    protected void updateTitle() {
        Path planFilePath = getPlanFilePath();
        String dirtyMarker = isDirty() ? "*" : "";
        String firstPart = planFilePath == null ? (Strings.NEW_PLAN_FILE + dirtyMarker) : (getPlanName(planFilePath) + dirtyMarker + " - " + planFilePath.toString());
        String title = firstPart + " - " + Strings.MAIN_WINDOW_TITLE;
        mPrimaryStage.setTitle(title);
    }

    public Stage getPrimaryStage() {
        return mPrimaryStage;
    }

    public Configuration getConfig() {
        return mConfig;
    }

    public ReadOnlyObjectProperty<Plan> planProperty() {
        return mPlanProperty;
    }

    public Plan getPlan() {
        return mPlanProperty.getValue();
    }

    public void setPlan(Plan plan, Path planFilePath) {
        // Attention: Plan's file path must be set first, listeners are attached to mPlanProperty and assume
        // the plan's directory already to be updated
        mPlanFilePathProperty.set(planFilePath);
        mPlanProperty.setValue(plan);
    }

    public Path getPlanFilePath() {
        return mPlanFilePathProperty.get();
    }

    protected void changePlanFilePath(Path value) {
        mPlanFilePathProperty.set(value);
    }

    public Property<Boolean> isDirtyProperty() {
        return mDirtyProperty;
    }

    /**
     * Returns if the current plan was changed and needs to be saved.
     */
    public boolean isDirty() {
        return mDirtyProperty.getValue();
    }

    /**
     * Sets the dirty flag as a result of a change of the current plan.
     */
    public void setDirty(boolean value) {
        mDirtyProperty.setValue(value);
    }

    public AssetManager getAssetManager() {
        return mAssetManager;
    }

    /**
     * Creates a new plan.
     */
    public void newPlan() {
        log.info("Creating new root plan");
        mConfig.setLastPlanFilePath(null);
        setPlan(Plan.newPlan(), null);
        setDirty(false);
        updateTitle();
    }

    /**
     * Loads the plan from the given file.
     */
    public void loadPlanFile(Path planFilePath) {
        log.info("Loading plan from '" + planFilePath + "'");
        PlanFile planFile = PlanFileIO.deserializePlanFile(planFilePath);
        setPlan(planFile.getPlan(), planFilePath);
        try {
            mUiController.setUiState(planFile.getUiState());
        } catch (Exception e) {
            log.warn("Error loading UI state for plan file '" + planFilePath + "'", e);
        }
        mConfig.setLastPlanFilePath(planFilePath);
        setDirty(false);
        updateTitle();
    }

    /**
     * Saves the plan under the given file path without user query.
     */
    public void savePlanAs(Path planFilePath) {
        log.info("Saving current plan as '" + planFilePath + "'");
        PlanFileIO.serializePlanFile(new PlanFile(getPlan(), mUiController.getUiState()), planFilePath);
        changePlanFilePath(planFilePath);
        mConfig.setLastPlanFilePath(planFilePath);
        setDirty(false);
        updateTitle();
    }

    /**
     * Checks if the current plan is unsaved, queries the user to save the plan and quits the platform, if not cancelled.
     */
    public boolean queryQuitApplication(Window parentWindow) {
        if (querySavePlanBeforeClose(parentWindow)) {
            shutdown();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the currnt plan is unsaved, queries the user to save the plan and opens a new plan, if not cancelled.
     */
    public boolean queryNewPlan(Window parentWindow) {
        if (!querySavePlanBeforeClose(parentWindow)) {
            return false;
        }
        newPlan();
        return true;
    }

    public boolean queryLoadPlanFile(Window parentWindow, Path planFilePath) {
        if (!querySavePlanBeforeClose(parentWindow)) {
            return false;
        }
        loadPlanFile(planFilePath);
        return true;
    }

    /**
     * Checks if the user wants to save an potentially unsaved plan before an operation which would
     * close the current plan. This method will save the plan as side-effect if the user wants to do that,
     * returning the information whether the ongoing process can continue or not.
     * Returns {@code true} if the process can continue, {@code false} if not.
     */
    protected boolean querySavePlanBeforeClose(Window parentWindow) {
        if (!isDirty()) {
            log.debug("Plan is unchanged, no need to save");
            return true;
        }
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(Strings.DIALOG_SAVE_PLAN_ON_CLOSE_TITLE);
        alert.setHeaderText(Strings.DIALOG_SAVE_PLAN_ON_QUIT_HEADER);
        //alert.setContentText("...");

        ButtonType buttonTypeYes = new ButtonType(Strings.YES);
        ButtonType buttonTypeNo = new ButtonType(Strings.NO);
        ButtonType buttonTypeCancel = new ButtonType(Strings.CANCEL, ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeYes) {
            log.debug("Plan was changed and user wants to save");
            return saveOrQueryPath(parentWindow);
        } else if (result.get() == buttonTypeNo) {
            log.debug("Plan was changed but user wants to discard changes");
            return true;
        } else {
            // ... user chose CANCEL or closed the dialog
            return false;
        }
    }

    /**
     * Saves the plan if it is attached to a file path, else queries the user for path to save as.
     */
    public boolean saveOrQueryPath(Window parentWindow) {
        Path currentPath = getPlanFilePath();
        if (currentPath == null) {
            return querySaveAs(parentWindow);
        } else {
            savePlanAs(currentPath);
            return true;
        }
    }

    // TODO: Replace by nice class which chooses the plan file directory instead of the file itself, with preview etc.
    class PlanFileChooser {
        protected final FileChooser mFileChooser = new FileChooser();

        public void setTitle(String dialogTitle) {
            mFileChooser.setTitle(dialogTitle);
        }

        public void setInitialFileName(String fileName) {
            mFileChooser.setInitialFileName(fileName);
        }

        public void setInitialDirectory(Path directory) {
            if (Files.exists(directory)) { // FileChooser crashes on open if the initial directory doesn't exist
                mFileChooser.setInitialDirectory(directory.toFile());
            }
        }

        public Optional<Path> showOpenDialog(Window ownerWindow) {
            File file = mFileChooser.showOpenDialog(ownerWindow);
            return file == null ? Optional.empty() : Optional.of(file.toPath());
        }

        public Path showSaveDialog(Window ownerWindow) {
            File file = mFileChooser.showSaveDialog(ownerWindow);
            return file == null ? null : file.toPath();
        }

        public void addExtensionFilter(ExtensionFilter filter) {
            mFileChooser.getExtensionFilters().add(filter);
        }
    }

    protected void addFileChooserExtensionFilters(PlanFileChooser fileChooser) {
        fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter(Strings.FILE_TYPE_ROOT_PLAN_EXTENSION_NAME, "*." + PlanFileIO.PLAN_FILE_EXTENSION));
    }

    /**
     * Shows a save-as dialog to the user.
     * @return {@code true} if the user saved the file, else {@code false}.
     */
    public boolean querySaveAs(Window parentWindow) {
        PlanFileChooser fileChooser = new PlanFileChooser();
        fileChooser.setTitle(Strings.DIALOG_SAVE_PLAN_TITLE);
        fileChooser.setInitialFileName(PlanFileIO.DEFAULT_ROOT_PATH_NAME);
        Path lastPath = mConfig.getLastPlanFilePath();
        if (lastPath == null) {
            Path lastDirectory = mConfig.getLastPlanFileDirectory();
            if (lastDirectory != null) {
                fileChooser.setInitialDirectory(lastDirectory);
            } else {
                // TODO: Set default save directory
            }
        } else {
            fileChooser.setInitialDirectory(lastPath.getParent());
            fileChooser.setInitialFileName(lastPath.getFileName().toString());
        }
        addFileChooserExtensionFilters(fileChooser);
        Path path = fileChooser.showSaveDialog(parentWindow);
        if (path != null) {
            savePlanAs(path);
            return true;
        }
        return false;
    }

    /**
     * Shows an open dialog to the user and loads the choosen plan, if the user didn't cancel.
     */
    public boolean queryOpen(Window parentWindow) {
        PlanFileChooser fileChooser = new PlanFileChooser();
        fileChooser.setTitle(Strings.DIALOG_OPEN_PLAN_TITLE);
        Path lastPath = mConfig.getLastPlanFilePath();
        if (lastPath == null) {
            // TODO: Set default save directory
        } else {
            fileChooser.setInitialDirectory(lastPath.getParent());
            fileChooser.setInitialFileName(lastPath.getFileName().toString());
        }
        addFileChooserExtensionFilters(fileChooser);
        Optional<Path> oPath = fileChooser.showOpenDialog(parentWindow);
        return oPath.map(path -> queryLoadPlanFile(mPrimaryStage, path)).orElse(false);
    }
}
