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
package de.dh.cad.architect.ui.view.libraries;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.model.assets.AssetLibrary;
import de.dh.cad.architect.model.assets.AssetRefPath.IAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.LibraryAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.PlanAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.SupportObjectAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import de.dh.utils.fx.dialogs.ProgressDialog;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

public class AssetsTableControl<T extends AbstractAssetDescriptor> extends BorderPane implements Initializable {
    protected class TableEntry {
        protected T mAssetDescriptor;
        protected ImageView mIcon;

        public TableEntry(T assetDescriptor, ImageView icon) {
            mAssetDescriptor = assetDescriptor;
            mIcon = icon;
        }

        public T getAssetDescriptor() {
            return mAssetDescriptor;
        }

        public ImageView getIcon() {
            return mIcon;
        }
    }

    protected class ConcurrentUpdater {
        protected Object mMutex = new Object();
        protected Task<Void> mUpdateTask = null;
        protected ProgressDialog mDialog = null;

        public void startUpdate(Task<Void> updateTask, String progressDialogTitle, Window ownerWindow) {
            synchronized (mMutex) {
                if (mUpdateTask != null && mUpdateTask.isRunning()) {
                    log.warn("Update task is still running, rejecting new update task");
                    return;
                }
                mUpdateTask = updateTask;
                mDialog = new ProgressDialog(progressDialogTitle, ownerWindow);
                mDialog.start(mUpdateTask);
            }
        }

        public void cancelUpdate() {
            synchronized (mMutex) {
                if (mUpdateTask == null) {
                    return;
                }
                mUpdateTask.cancel();
            }
        }

        public boolean isRunning() {
            synchronized (mMutex) {
                return mUpdateTask != null && mUpdateTask.isRunning();
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(AssetsTableControl.class);

    public static final String FXML = "AssetsTableControl.fxml";
    protected static final String CLEAR_FILTER_ICON = "clear-filter.png";
    protected static final int TABLE_VIEW_ICON_IMAGE_SIZE = 20;

    protected final AssetLoader mAssetLoader;
    protected final AssetType mAssetType;

    protected ObservableList<TableEntry> mBackingList = FXCollections.observableArrayList();
    protected FilteredList<TableEntry> mFilteredList = new FilteredList<>(mBackingList);

    protected final ConcurrentUpdater mConcurrentUpdater = new ConcurrentUpdater();

    @FXML
    protected TextField mAssetsFilterTextField;

    @FXML
    protected Button mClearAssetsFilterButton;

    @FXML
    protected TableView<TableEntry> mAssetsTableView;

    @FXML
    protected TableColumn<TableEntry, ImageView> mIconColumn;

    @FXML
    protected TableColumn<TableEntry, String> mNameColumn;

    @FXML
    protected TableColumn<TableEntry, String> mCategoryColumn;

    @FXML
    protected TableColumn<TableEntry, String> mTypeColumn;

    @FXML
    protected TableColumn<TableEntry, String> mIdColumn;

    @FXML
    protected TableColumn<TableEntry, String> mLocationColumn;

    protected SimpleObjectProperty<T> mSelectedItemProperty = new SimpleObjectProperty<>();
    protected ObservableList<T> mSelectedItemsProperty = FXCollections.observableArrayList();

    protected IAssetChoosenHandler<T> mAssetChoosenHandler = null;

    protected AssetsTableControl(AssetLoader assetLoader, AssetType assetType) {
        mAssetLoader = assetLoader;
        mAssetType = assetType;
        FXMLLoader fxmlLoader = new FXMLLoader(AssetsTableControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractAssetDescriptor> AssetsTableControl<T> create(AssetLoader assetLoader, AssetType assetType) {
        return new AssetsTableControl<>(assetLoader, assetType);
    }

    protected String buildAnchorStr(IAssetPathAnchor pathAnchor) {
        if (pathAnchor instanceof PlanAssetPathAnchor) {
            return Strings.LIBRARY_MANAGER_ASSET_PATH_ANCHOR_PLAN;
        } else if (pathAnchor instanceof LibraryAssetPathAnchor libraryPathAnchor) {
            String libraryId = libraryPathAnchor.getLibraryId();
            LibraryData libraryData = mAssetLoader.getAssetManager().getAssetLibraries().get(libraryId);
            String libraryName = libraryData == null ? libraryId : libraryData.getLibrary().getName();
            return MessageFormat.format(Strings.LIBRARY_MANAGER_ASSET_PATH_ANCHOR_LIBRARY, libraryName);
        } else if (pathAnchor instanceof SupportObjectAssetPathAnchor supportObjectPathAnchor) {
            return MessageFormat.format(Strings.LIBRARY_MANAGER_ASSET_PATH_ANCHOR_SUPPORT_OBJECT, supportObjectPathAnchor.getSupportObjectRef());
        } else {
            throw new NotImplementedException("Handling of asset path anchor <" + pathAnchor + "> is not implemented");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getAssetDescriptor().getName()));
        mIconColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(loadIcon(param.getValue().getAssetDescriptor())));
        mCategoryColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getAssetDescriptor().getCategory()));
        mTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getAssetDescriptor().getType()));
        mIdColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getAssetDescriptor().getId()));
        mLocationColumn.setCellValueFactory(param -> {
            IAssetPathAnchor anchor = param.getValue().getAssetDescriptor().getSelfRef().getAnchor();
            return new SimpleStringProperty(buildAnchorStr(anchor));
        });

        mAssetsTableView.setOnMouseClicked(event -> {
            if (!event.getButton().equals(MouseButton.PRIMARY)) {
                return;
            }
            if (!event.isStillSincePress()) {
                return;
            }
            if (event.getClickCount() != 2) {
                return;
            }
            event.consume();
            fireAssetChoosen();
        });
        mAssetsTableView.setOnKeyPressed(event -> {
            String character = event.getCharacter();
            if ("\n".equals(character)) {
                fireAssetChoosen();
            }
        });

        TableViewSelectionModel<TableEntry> selectionModel = mAssetsTableView.getSelectionModel();
        selectionModel.selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends TableEntry> observable, TableEntry oldValue, TableEntry newValue) {
                mSelectedItemProperty.set(newValue == null || selectionModel.getSelectedIndices().size() != 1 ? null : newValue.getAssetDescriptor());
            }
        });
        ObservableList<TableEntry> selectedItems = selectionModel.getSelectedItems();
        selectedItems.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends TableEntry> c) {
                mSelectedItemsProperty.setAll(selectedItems
                    .stream()
                    .map(te -> te.getAssetDescriptor())
                    .collect(Collectors.toList()));
            }
        });

        ImageView clearAssetsFilterIcon = new ImageView(new Image(AssetsTableControl.class.getResource(CLEAR_FILTER_ICON).toString()));
        clearAssetsFilterIcon.setFitWidth(20);
        clearAssetsFilterIcon.setFitHeight(20);
        mClearAssetsFilterButton.setGraphic(clearAssetsFilterIcon);
        GridPane.setFillWidth(mClearAssetsFilterButton, true);
        GridPane.setFillHeight(mClearAssetsFilterButton, true);
        mClearAssetsFilterButton.setOnAction(event -> {
            mAssetsFilterTextField.setText("");
        });

        mAssetsTableView.setItems(mFilteredList);

        mAssetsFilterTextField.textProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                updateFilterPredicate();
            }
        });

        if (AssetType.MaterialSet.equals(mAssetType)) {
            mAssetsTableView.setPlaceholder(new Label(Strings.LIBRARY_MANAGER_NO_MATERIAL_SET_IN_TABLE_HINT));
        } else if (AssetType.SupportObject.equals(mAssetType)) {
            mAssetsTableView.setPlaceholder(new Label(Strings.LIBRARY_MANAGER_NO_SUPPORT_OBJECTS_IN_TABLE_HINT));
        } else {
            throw new NotImplementedException("Asset table placeholder for asset type '" + mAssetType + "' is not implemented");
        }
    }

    protected void updateFilterPredicate() {
        Predicate<TableEntry> predicate = buildFilterPredicate(mAssetsFilterTextField.getText());
        mFilteredList.setPredicate(predicate);
        mAssetsTableView.sort(); // Seems to be necessary so ensure a sorted table after the filter text was changed
    }

    protected void fireAssetChoosen() {
        T selectedItem = getSelectedItem();
        if (selectedItem != null && mAssetChoosenHandler != null) {
            mAssetChoosenHandler.onAssetChoosen(AssetsTableControl.this, selectedItem);
        }
    }

    public ReadOnlyObjectProperty<T> selectedItemProperty() {
        return mSelectedItemProperty;
    }

    public T getSelectedItem() {
        return mSelectedItemProperty.get();
    }

    public ObservableList<T> getSelectedItems() {
        return mSelectedItemsProperty;
    }

    public void setTableControlSelectionMode(SelectionMode mode) {
        mAssetsTableView.getSelectionModel().setSelectionMode(mode);
    }

    public IAssetChoosenHandler<T> getAssetChoosenHandler() {
        return mAssetChoosenHandler;
    }

    public void setAssetChoosenHandler(IAssetChoosenHandler<T> value) {
        mAssetChoosenHandler = value;
    }

    protected void deselectAll() {
        mAssetsTableView.getSelectionModel().clearSelection();
    }

    protected ImageView loadIcon(T assetDescriptor) {
        Image img = mAssetType == AssetType.SupportObject
                        ? mAssetLoader.loadSupportObjectIconImage((SupportObjectDescriptor) assetDescriptor, true)
                        : mAssetLoader.loadMaterialSetIconImage((MaterialSetDescriptor) assetDescriptor, true);
        ImageView result = new ImageView(img);
        result.setFitWidth(TABLE_VIEW_ICON_IMAGE_SIZE);
        result.setFitHeight(TABLE_VIEW_ICON_IMAGE_SIZE);
        return result;
    }

    protected Predicate<TableEntry> buildFilterPredicate(String filterStr) {
        return te -> {
            AbstractAssetDescriptor descriptor = te.getAssetDescriptor();
            return checkProperties(filterStr, descriptor.getTags(), descriptor.getId(), descriptor.getName(), descriptor.getCategory(), descriptor.getType(), descriptor.getAuthor());
        };
    }

    protected static boolean checkProperties(String filterStr, Collection<String> tags, String... propertiesToCheck) {
        filterStr = filterStr.toUpperCase();
        for (String property : propertiesToCheck) {
            if (StringUtils.trimToEmpty(property).toUpperCase().contains(filterStr)) {
                return true;
            }
        }
        for (String tag : tags) {
            if (StringUtils.trimToEmpty(tag).toUpperCase().contains(filterStr)) {
                return true;
            }
        }
        return false;
    }

    protected double calculateProgress(int numLibraries, int currentLibrary, int numDescriptors, int currentDescriptor) {
        return (currentLibrary + currentDescriptor / (double) numDescriptors) / numLibraries;
    }

    public void loadLibraries_progress_async(Collection<AssetLibrary> libraries) {
        Scene scene = getScene();
        if (scene == null) {
            // Scene not initialized yet, postpone loading of libraries
            Platform.runLater(() -> {
                Window window = getScene().getWindow();
                loadLibraries(libraries, window);
            });
            return;
        }
        Window window = scene.getWindow();
        loadLibraries(libraries, window);
    }

    protected void loadLibraries(Collection<AssetLibrary> libraries, Window window) {
        mConcurrentUpdater.startUpdate(new Task<>() {
            protected volatile boolean mCancelled = false;

            @Override
            protected void cancelled() {
                mCancelled = true;
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected Void call() throws Exception {
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        mBackingList.clear();
                    }
                });
                AssetManager assetManager = mAssetLoader.getAssetManager();
                int libraryCounterBase0 = 0;
                for (AssetLibrary library : libraries) {
                    if (mCancelled) {
                        return null;
                    }
                    LibraryAssetPathAnchor libraryAnchor = new LibraryAssetPathAnchor(library.getId());
                    try {
                        Collection<T> assets;
                        if (AssetType.MaterialSet.equals(mAssetType)) {
                            assets = (Collection) assetManager.loadMaterialSetDescriptors(libraryAnchor);
                        } else if (AssetType.SupportObject.equals(mAssetType)) {
                            assets = (Collection) assetManager.loadSupportObjectDescriptors(libraryAnchor);
                        } else {
                            throw new NotImplementedException("Loading table entries for asset type '" + mAssetType + "' is not implemented");
                        }
                        final int currentLibraryCounterBase0 = libraryCounterBase0; // Final variable for inner class
                        Platform.runLater(() -> {
                            int counterBase0 = 0;
                            for (T assetDescriptor : assets) {
                                if (mCancelled) {
                                    return;
                                }
                                double progress = calculateProgress(libraries.size(), currentLibraryCounterBase0, assets.size(), counterBase0);
                                updateProgress(progress, 1.0);
                                counterBase0++;
                                ImageView icon = loadIcon(assetDescriptor);
                                mBackingList.add(new TableEntry(assetDescriptor, icon));
                            }
                            mAssetsTableView.refresh();
                        });
                    } catch (Exception e) {
                        log.warn("Error loading assets of type " + mAssetType + " from library '" + library.getId() + "'", e);
                    }
                    libraryCounterBase0++;
                }
                return null;
            }
        }, getProgressDialogTitle(mAssetType), window);
    }

    protected String getProgressDialogTitle(AssetType assetType) {
        switch (assetType) {
        case MaterialSet:
            return Strings.LIBRARY_MANAGER_LOADING_MATERIAL_SETS_PROGRESS_TITLE;
        case SupportObject:
            return Strings.LIBRARY_MANAGER_LOADING_SUPPORT_OBJECTS_PROGRESS_TITLE;
        default:
            throw new RuntimeException("Unknown asset type '" + assetType + "'");
        }
    }

    protected int indexOf(String assetId) {
        int index = 0;
        for (TableEntry te : mAssetsTableView.getItems()) {
            if (te.getAssetDescriptor().getId().equals(assetId)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public void selectObject(String assetId) {
        TableViewSelectionModel<AssetsTableControl<T>.TableEntry> selectionModel = mAssetsTableView.getSelectionModel();
        if (assetId == null) {
            selectionModel.clearSelection();
            return;
        }
        int index = indexOf(assetId);
        selectionModel.select(index);
        mAssetsTableView.scrollTo(index);
    }
}
