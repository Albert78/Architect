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
package de.dh.cad.architect.libraryimporter.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.libraryimporter.ExternalSupportObjectDescriptor;
import de.dh.cad.architect.libraryimporter.sh3d.DefaultLibrary;
import de.dh.cad.architect.libraryimporter.sh3d.furniture.CatalogDoorOrWindow;
import de.dh.cad.architect.libraryimporter.sh3d.furniture.CatalogLight;
import de.dh.cad.architect.libraryimporter.sh3d.furniture.CatalogPieceOfFurniture;
import de.dh.cad.architect.libraryimporter.sh3d.furniture.DefaultFurnitureCatalog;
import de.dh.cad.architect.libraryimporter.sh3d.furniture.DefaultFurnitureCatalog.SH3DFurnitureLibrary;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import de.dh.cad.architect.ui.assets.AssetManagerConfiguration;
import de.dh.cad.architect.utils.ObjectStringAdapter;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.PlainFileSystemDirectoryLocator;
import de.dh.utils.fx.FxUtils;
import de.dh.utils.fx.dialogs.ProgressDialog;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

public class FurnitureImporterWindow implements Initializable {
    protected class SupportObjectTreeEntry {
        protected final ExternalSupportObjectDescriptor mSODescriptor;
        protected final BooleanProperty mImportItem;

        public SupportObjectTreeEntry(ExternalSupportObjectDescriptor soDescriptor) {
            mSODescriptor = soDescriptor;
            mImportItem = new SimpleBooleanProperty(true);
        }

        public ExternalSupportObjectDescriptor getSODescriptor() {
            return mSODescriptor;
        }

        public BooleanProperty getImportItem() {
            return mImportItem;
        }
    }

    public static final int WINDOW_SIZE_X = 1000;
    public static final int WINDOW_SIZE_Y = 800;

    private static final Logger log = LoggerFactory.getLogger(FurnitureImporterWindow.class);

    public static final String FXML = "SupportObjectsImporterWindow.fxml";

    protected Stage mStage = null;
    protected final AssetManager mAssetManager;
    protected SH3DFurnitureLibrary mCurrentLibrary = null;
    protected CatalogPieceOfFurnitureControl mCurrentSOControl = null;
    protected Collection<SupportObjectTreeEntry> mTreeEntries = new ArrayList<>();

    @FXML
    protected Parent mRoot;

    @FXML
    protected Label mCurrentLibraryLabel;

    @FXML
    protected TreeTableView<SupportObjectTreeEntry> mTreeTableView;

    @FXML
    protected TreeTableColumn<SupportObjectTreeEntry, BooleanProperty> mImportColumn;

    @FXML
    protected TreeTableColumn<SupportObjectTreeEntry, String> mNameColumn;

    @FXML
    protected TreeTableColumn<SupportObjectTreeEntry, String> mCategoryColumn;

    @FXML
    protected TreeTableColumn<SupportObjectTreeEntry, Boolean> mIsDoorWindowColumn;

    @FXML
    protected TreeTableColumn<SupportObjectTreeEntry, Boolean> mIsStairCaseColumn;

    @FXML
    protected TreeTableColumn<SupportObjectTreeEntry, Boolean> mIsLightSourceColumn;

    @FXML
    protected TreeTableColumn<SupportObjectTreeEntry, String> mIdColumn;

    @FXML
    protected TreeTableView<SupportObjectTreeEntry> mAssetsTreeTableView;

    @FXML
    protected ComboBox<ObjectStringAdapter<LibraryData>> mImportAssetLibrariesComboBox;

    @FXML
    protected Button mImportButton;

    @FXML
    protected BorderPane mContentPane;

    protected FurnitureImporterWindow(AssetManager assetManager) {
        mAssetManager = assetManager;
        FXMLLoader fxmlLoader = new FXMLLoader(FurnitureImporterWindow.class.getResource(FXML));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FurnitureImporterWindow create(AssetManager assetManager) {
        log.info("Creating support objects importer window");
        return new FurnitureImporterWindow(assetManager);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mImportColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<SupportObjectTreeEntry, BooleanProperty>, ObservableValue<BooleanProperty>>() {
            @Override
            public ObservableValue<BooleanProperty> call(CellDataFeatures<SupportObjectTreeEntry, BooleanProperty> param) {
                SupportObjectTreeEntry value = param.getValue().getValue();
                SimpleObjectProperty<BooleanProperty> result = new SimpleObjectProperty<>();
                if (value == null) {
                    return result;
                }
                result.set(value.getImportItem());
                return result;
            }
        });
        mImportColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(Integer index) {
                TreeItem<SupportObjectTreeEntry> treeItem = mAssetsTreeTableView.getTreeItem(index);
                if (treeItem == null || treeItem.getValue() == null) {
                    return null;
                }
                return treeItem.getValue().getImportItem();
            }
        }));
        styleCheckboxColumn(mImportColumn);

        mNameColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<SupportObjectTreeEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<SupportObjectTreeEntry, String> param) {
                SupportObjectTreeEntry value = param.getValue().getValue();
                if (value == null) {
                    return new SimpleStringProperty();
                }
                return new SimpleStringProperty(value.getSODescriptor().getSourcePieceOfFurniture().getName());
            }
        });
        mCategoryColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<SupportObjectTreeEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<SupportObjectTreeEntry, String> param) {
                SupportObjectTreeEntry value = param.getValue().getValue();
                if (value == null) {
                    return new SimpleStringProperty();
                }
                return new SimpleStringProperty(value.getSODescriptor().getSourcePieceOfFurniture().getCategory());
            }
        });

        mIsDoorWindowColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<SupportObjectTreeEntry, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(CellDataFeatures<SupportObjectTreeEntry, Boolean> param) {
                SupportObjectTreeEntry value = param.getValue().getValue();
                if (value == null) {
                    return new SimpleBooleanProperty();
                }
                return new SimpleBooleanProperty(value.getSODescriptor().getSourcePieceOfFurniture() instanceof CatalogDoorOrWindow);
            }
        });
        mIsDoorWindowColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(mIsDoorWindowColumn));
        styleCheckboxColumn(mIsDoorWindowColumn);

        mIsStairCaseColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<SupportObjectTreeEntry, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(CellDataFeatures<SupportObjectTreeEntry, Boolean> param) {
                SupportObjectTreeEntry value = param.getValue().getValue();
                if (value == null) {
                    return new SimpleBooleanProperty();
                }
                return new SimpleBooleanProperty(!StringUtils.isEmpty(value.getSODescriptor().getSourcePieceOfFurniture().getStaircaseCutOutShape()));
            }
        });
        mIsStairCaseColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(mIsStairCaseColumn));
        styleCheckboxColumn(mIsStairCaseColumn);

        mIsLightSourceColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<SupportObjectTreeEntry, Boolean>, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(CellDataFeatures<SupportObjectTreeEntry, Boolean> param) {
                SupportObjectTreeEntry value = param.getValue().getValue();
                if (value == null) {
                    return new SimpleBooleanProperty();
                }
                return new SimpleBooleanProperty(value.getSODescriptor().getSourcePieceOfFurniture() instanceof CatalogLight);
            }
        });
        mIsLightSourceColumn.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(mIsLightSourceColumn));
        styleCheckboxColumn(mIsLightSourceColumn);

        mIdColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<SupportObjectTreeEntry, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<SupportObjectTreeEntry, String> param) {
                SupportObjectTreeEntry value = param.getValue().getValue();
                if (value == null) {
                    return new SimpleStringProperty();
                }
                return new SimpleStringProperty(value.getSODescriptor().getSourcePieceOfFurniture().getId());
            }
        });

        TreeTableViewSelectionModel<SupportObjectTreeEntry> selectionModel = mAssetsTreeTableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        selectionModel.getSelectedItems().addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends TreeItem<SupportObjectTreeEntry>> c) {
                TreeItem<SupportObjectTreeEntry> selectedItem = selectionModel.getSelectedItem();
                SupportObjectTreeEntry supportObjectTreeEntry = selectedItem.getValue();
                mCurrentSOControl = new CatalogPieceOfFurnitureControl(supportObjectTreeEntry.getSODescriptor().getSourcePieceOfFurniture(), mAssetManager, getStage());
                mContentPane.setCenter(mCurrentSOControl);
            }
        });

        updateAvailableAssetLibraries();
        mImportAssetLibrariesComboBox.setOnAction(actionEvent -> {
            updateImportButton();
        });

        mImportButton.setOnAction(actionEvent -> {
            ObjectStringAdapter<LibraryData> selectedImportLibrary = mImportAssetLibrariesComboBox.getSelectionModel().getSelectedItem();
            LibraryData importLibrary = selectedImportLibrary.getObj();
            if (importLibrary == null) {
                Optional<LibraryData> createdLibrary = createNewLibrary();
                if (createdLibrary.isEmpty()) {
                    return;
                }
                updateAvailableAssetLibraries();
                int index = mImportAssetLibrariesComboBox.getItems().indexOf(new ObjectStringAdapter<>(createdLibrary, null));
                mImportAssetLibrariesComboBox.getSelectionModel().clearAndSelect(index);
                importLibrary = createdLibrary.get();
            }
            importIntoLibrary(importLibrary);
        });
        updateImportButton();
    }

    protected void updateAvailableAssetLibraries() {
        ObservableList<ObjectStringAdapter<LibraryData>> assetLibraries = FXCollections.observableArrayList();
        for (LibraryData libraryData : mAssetManager.getAssetLibraries().values()) {
            assetLibraries.add(new ObjectStringAdapter<>(libraryData, libraryData.getLibrary().getName()));
        }
        assetLibraries.add(new ObjectStringAdapter<>(null, "Neue Bibliothek erstellen..."));
        mImportAssetLibrariesComboBox.setItems(assetLibraries);
    }

    protected void updateImportButton() {
        ObjectStringAdapter<LibraryData> selectedImportLibrary = mImportAssetLibrariesComboBox.getSelectionModel().getSelectedItem();
        mImportButton.setDisable(selectedImportLibrary == null);
    }

    protected void styleCheckboxColumn(TreeTableColumn<?, ?> column) {
        column.setStyle("-fx-alignment: center;");
    }

    public void show(Stage stage) {
        Scene scene = new Scene(mRoot, WINDOW_SIZE_X, WINDOW_SIZE_Y);
        mStage = stage;

        stage.setScene(scene);
        stage.setTitle("Architect SH3D-Möbelbibliotheks-Importer");
        stage.show();

        mStage.onCloseRequestProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends EventHandler<WindowEvent>> observable, EventHandler<WindowEvent> oldValue,
                EventHandler<WindowEvent> newValue) {
                // TODO
            }
        });
    }

    public Stage getStage() {
        return (Stage) mRoot.getScene().getWindow();
    }

    protected void loadSourceLibrary(Path libraryPath) {
        try {
            mCurrentLibrary = DefaultFurnitureCatalog.readFurniture(new PlainFileSystemDirectoryLocator(libraryPath));
        } catch (IOException e) {
            throw new RuntimeException("Error loading SH3D furniture catalog", e);
        }
        mCurrentLibraryLabel.setText(mCurrentLibrary.getLibrary().getName());
        mCurrentLibraryLabel.setTooltip(new Tooltip(mCurrentLibrary.getLibrary().getLocation()));

        Collection<CatalogPieceOfFurniture> furniture = mCurrentLibrary.getFurniture().values();

        mTreeEntries.clear();
        for (CatalogPieceOfFurniture cpof : furniture) {
            mTreeEntries.add(new SupportObjectTreeEntry(new ExternalSupportObjectDescriptor(cpof, mCurrentLibrary, libraryPath)));
        }
        updateImportTable();
    }

    protected void updateImportTable() {
        Collection<TreeItem<SupportObjectTreeEntry>> assetItems = new ArrayList<>();
        for (SupportObjectTreeEntry supportObjectTreeEntry : mTreeEntries) {
            assetItems.add(new TreeItem<>(supportObjectTreeEntry));
        }
        TreeItem<SupportObjectTreeEntry> root = new TreeItem<>();
        root.getChildren().addAll(assetItems);
        mAssetsTreeTableView.setRoot(root);
    }

    protected DirectoryChooser getLibraryDirectoryChooser(String dialogTitle) {
        DirectoryChooser result = new DirectoryChooser();
        result.setTitle(dialogTitle);
        return result;
    }

    protected Optional<LibraryData> createNewLibrary() {
        DirectoryChooser dialog = getLibraryDirectoryChooser("Leeres Verzeichnis für neue Bibliothek wählen");
        AssetManagerConfiguration configuration = mAssetManager.getConfiguration();
        Optional<Path> oPath = configuration.getLastChoosenExternalLbraryPath();
        oPath.ifPresent(path -> FxUtils.trySetInitialDirectory(dialog, path));
        File libraryDir = dialog.showDialog(getStage());
        if (libraryDir == null) {
            return Optional.empty();
        }
        Path libraryPath = libraryDir.toPath();
        DefaultLibrary library = mCurrentLibrary == null ? null : mCurrentLibrary.getLibrary();
        return Optional.of(mAssetManager.createNewAssetLibrary(new PlainFileSystemDirectoryLocator(libraryPath), library == null ? "Neue Möbelbibliothek" : library.getName()));
    }

    protected void importIntoLibrary(LibraryData targetLibrary) {
        ProgressDialog progressDialog = new ProgressDialog("Importiere Inhalte...", getStage());

        var task = new Task<Void>() {
            protected int mNumImported = 0;
            protected int mNumErroneous = 0;

            @Override
            public Void call() throws InterruptedException {
                updateProgress(0, mTreeEntries.size());
                doImportIntoLibrary();
                AssetManagerConfiguration configuration = mAssetManager.getConfiguration();
                IDirectoryLocator rootDirectory = targetLibrary.getRootDirectory();
                if (rootDirectory instanceof PlainFileSystemDirectoryLocator) {
                    configuration.setLastChoosenExternalLibraryPath(((PlainFileSystemDirectoryLocator) rootDirectory).getPath());
                }
                mAssetManager.saveAssetLibrary(targetLibrary);
                return null;
            }

            private void doImportIntoLibrary() {
                int progress = 0;
                for (SupportObjectTreeEntry supportObjectTreeEntry : mTreeEntries) {
                    updateProgress(progress, mTreeEntries.size());
                    progress++;
                    if (!supportObjectTreeEntry.getImportItem().get()) {
                        continue;
                    }
                    ExternalSupportObjectDescriptor soDescriptor = supportObjectTreeEntry.getSODescriptor();
                    SupportObjectDescriptor desc = soDescriptor.importSupportObject(targetLibrary, mAssetManager);
                    if (desc == null) {
                        mNumErroneous++;
                    } else {
                        mNumImported++;
                    }
                }
            }

            public int getNumImported() {
                return mNumImported;
            }

            public int getNumErroneous() {
                return mNumErroneous;
            }
        };
        task.setOnSucceeded(event -> {
            progressDialog.close();
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Import-Prozess");
            int numImported = task.getNumImported();
            int numErroneous = task.getNumErroneous();
            if (numErroneous > 0) {
                if (numImported > 0) {
                    alert.setAlertType(AlertType.WARNING);
                    alert.setHeaderText("Importergebnis");
                    alert.setContentText("Die SweetHome3D-Bibliothek '" + mCurrentLibrary.getLibrary().getName() + "' wurde nach '" + targetLibrary.getRootDirectory().toString()
                        + "' importiert. Erfolgreich übertragen: " + numImported + ", Anzahl Fehler: " + numErroneous);
                } else { // numImported == 0
                    alert.setAlertType(AlertType.ERROR);
                    alert.setHeaderText("Fehler bei Import");
                    alert.setContentText("Die SweetHome3D-Bibliothek '" + mCurrentLibrary.getLibrary().getName() + "' konnte nicht nach '" + targetLibrary.getRootDirectory().toString()
                        + "' importiert werden.");
                }
            } else { // numErroneous == 0
                if (numImported > 0) {
                    alert.setAlertType(AlertType.INFORMATION);
                    alert.setHeaderText("Import erfolgreich");
                    alert.setContentText("Die SweetHome3D-Bibliothek '" + mCurrentLibrary.getLibrary().getName() + "' wurde erfolgreich nach '" + targetLibrary.getRootDirectory().toString()
                        + "' importiert. Erfolgreich importiert: " + numImported);
                } else { // numImported == 0
                    alert.setAlertType(AlertType.ERROR);
                    alert.setHeaderText("Kein Inhalt");
                    alert.setContentText("Keine Objekte in SweetHome3D-Bibliothek '" + mCurrentLibrary.getLibrary().getName() + "' gefunden");
                }
            }

            alert.showAndWait();
        });

        progressDialog.start(task);
    }

    // TODO: Show better open dialog which marks valid directories, same in TextureImporterWindow
    @FXML
    protected void onReadSH3DFurnitureLibrary(ActionEvent event) {
        DirectoryChooser dialog = getLibraryDirectoryChooser("SH3D Möbelbibliotheksverzeichnis wählen");
        AssetManagerConfiguration configuration = mAssetManager.getConfiguration();
        Optional<Path> oPath = configuration.getLastImportedLibraryPath();
        oPath.ifPresent(path -> FxUtils.trySetInitialDirectory(dialog, path));
        File libraryDir = dialog.showDialog(getStage());
        if (libraryDir == null) {
            return;
        }
        Path libraryPath = libraryDir.toPath();
        Path mainFile = libraryPath.resolve(DefaultFurnitureCatalog.PLUGIN_FURNITURE_CATALOG_MAIN_FILE);
        if (!Files.exists(mainFile)) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText("Import abgebrochen");
            alert.setContentText("Das gewählte Verzeichnis '" + libraryDir + "' enthält anscheinend keine SweetHome3D Möbel-Biblithek, Datei '" + DefaultFurnitureCatalog.PLUGIN_FURNITURE_CATALOG_MAIN_FILE + "' fehlt!");

            alert.showAndWait();
            return;
        }
        configuration.setLastImportedLibraryPath(libraryPath);

        loadSourceLibrary(libraryPath);
    }
}
