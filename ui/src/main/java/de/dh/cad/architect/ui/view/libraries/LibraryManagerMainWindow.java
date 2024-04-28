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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.model.assets.AssetLibrary;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.AssetRefPath.IAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.LibraryAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import de.dh.cad.architect.ui.assets.AssetManagerConfiguration;
import de.dh.cad.architect.ui.view.libraries.MaterialSetLocationChooserControl.IMaterialSetLocation;
import de.dh.cad.architect.ui.view.libraries.MaterialSetLocationChooserControl.LibraryRootMaterialSetLocation;
import de.dh.cad.architect.ui.view.libraries.MaterialSetLocationChooserControl.SupportObjectMaterialSetLocation;
import de.dh.cad.architect.utils.Namespace;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.PlainFileSystemDirectoryLocator;
import de.dh.utils.fx.FxUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

public class LibraryManagerMainWindow implements Initializable {
    public static final int WINDOW_SIZE_X = 1500;
    public static final int WINDOW_SIZE_Y = 800;

    private static final Logger log = LoggerFactory.getLogger(LibraryManagerMainWindow.class);

    public static final String FXML = "LibraryManagerMainWindow.fxml";

    protected final AssetLoader mAssetLoader;
    protected final AssetManager mAssetManager;
    protected Stage mPrimaryStage = null;

    protected ObservableList<CheckableLibraryEntry> mLibraries = FXCollections.observableArrayList();
    protected ListView<CheckableLibraryEntry> mLibrariesListView;

    protected AssetChooserControl<MaterialSetDescriptor> mMaterialSetChooserControl;
    protected AssetChooserControl<SupportObjectDescriptor> mSupportObjectsChooserControl;

    @FXML
    protected Control mRoot;

    @FXML
    protected StackPane mLibraryListParent;

    @FXML
    protected Button mNewLibraryButton;

    @FXML
    protected Button mOpenLibraryButton;

    @FXML
    protected Button mEditLibraryButton;

    @FXML
    protected Button mRemoveLibraryButton;

    @FXML
    protected Button mDeleteLibraryButton;

    @FXML
    protected TabPane mAssetsTabPane;

    @FXML
    protected Tab mSupportObjectsTab;

    @FXML
    protected Tab mMaterialSetsTab;

    @FXML
    protected BorderPane mMaterialSetsParentPane;

    @FXML
    protected BorderPane mSupportObjectsParentPane;

    @FXML
    protected Button mNewMaterialSetButton;

    @FXML
    protected Button mEditMaterialSetButton;

    @FXML
    protected Button mDeleteMaterialSetsButton;

    @FXML
    protected Button mNewSupportObjectButton;

    @FXML
    protected Button mEditSupportObjectButton;

    @FXML
    protected Button mDeleteSupportObjectsButton;

    protected LibraryManagerMainWindow(AssetLoader assetLoader) {
        mAssetLoader = assetLoader;
        mAssetManager = assetLoader.getAssetManager();
        FXMLLoader fxmlLoader = new FXMLLoader(LibraryManagerMainWindow.class.getResource(FXML));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LibraryManagerMainWindow create(AssetLoader assetLoader) {
        log.info("Creating asset library manager window");
        return new LibraryManagerMainWindow(assetLoader);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mLibrariesListView = new ListView<>(new SortedList<>(mLibraries, CheckableLibraryEntry.COMPARATOR_BY_NAME));
        mLibrariesListView.setCellFactory(CheckBoxListCell.forListView(CheckableLibraryEntry::selectedProperty));
        mLibraryListParent.getChildren().add(mLibrariesListView);

        mMaterialSetChooserControl = new AssetChooserControl<>(mAssetLoader, AssetType.MaterialSet);
        mMaterialSetChooserControl.setAssetChoosenHandler((source, assetDescriptor) -> {
            editSelectedMaterialSets();
        });
        mMaterialSetChooserControl.setSelectionMode(SelectionMode.MULTIPLE);
        mMaterialSetsParentPane.setCenter(mMaterialSetChooserControl);

        mSupportObjectsChooserControl = new AssetChooserControl<>(mAssetLoader, AssetType.SupportObject);
        mSupportObjectsChooserControl.setAssetChoosenHandler((source, assetDescriptor) -> {
            editSelectedSupportObjects();
        });
        mSupportObjectsChooserControl.setSelectionMode(SelectionMode.MULTIPLE);
        mSupportObjectsParentPane.setCenter(mSupportObjectsChooserControl);

        mNewLibraryButton.setOnAction(event -> {
            mMaterialSetChooserControl.selectObject(null);
            mSupportObjectsChooserControl.selectObject(null);
            clearAssetSelection();
            queryNewLibrary();
        });
        mOpenLibraryButton.setOnAction(event -> {
            clearAssetSelection();
            queryOpenLibraries();
        });
        mEditLibraryButton.setOnAction(event -> {
            List<CheckableLibraryEntry> selectedLibraries = getSelectedLibraries();
            if (selectedLibraries.size() == 1) {
                editAssetLibrary(selectedLibraries.get(0));
            }
        });
        mRemoveLibraryButton.setOnAction(event -> {
            clearAssetSelection();
            Collection<CheckableLibraryEntry> selectedLibraries = getSelectedLibraries();
            removeLibraries(selectedLibraries);
        });
        mDeleteLibraryButton.setOnAction(event -> {
            clearAssetSelection();
            Collection<CheckableLibraryEntry> selectedLibraries = getSelectedLibraries();
            queryDeleteLibraries(selectedLibraries);
        });

        mNewMaterialSetButton.setOnAction(event -> {
            queryNewMaterialSet();
        });
        mEditMaterialSetButton.setOnAction(event -> {
            editSelectedMaterialSets();
        });
        mDeleteMaterialSetsButton.setOnAction(event -> {
            Collection<MaterialSetDescriptor> selectedMaterialSets = getSelectedMaterialSets();
            queryDeleteMaterialSets(selectedMaterialSets);
        });

        mNewSupportObjectButton.setOnAction(event -> {
            queryNewSupportObject();
        });
        mEditSupportObjectButton.setOnAction(event -> {
            editSelectedSupportObjects();
        });
        mDeleteSupportObjectsButton.setOnAction(event -> {
            Collection<SupportObjectDescriptor> selectedSupportObjects = getSelectedSupportObjects();
            queryDeleteSupportObjects(selectedSupportObjects);
        });

        loadLibraries();
        updateButtonStates();

        ListChangeListener<Object> updateButtonsListener = c -> {
            updateButtonStates();
        };

        MultipleSelectionModel<CheckableLibraryEntry> librariesListSelectionModel = mLibrariesListView.getSelectionModel();
        librariesListSelectionModel.getSelectedItems().addListener(updateButtonsListener);
        mMaterialSetChooserControl.getSelectedAssets().addListener(updateButtonsListener);
        mSupportObjectsChooserControl.getSelectedAssets().addListener(updateButtonsListener);

        ListChangeListener<CheckableLibraryEntry> reloadLibrariesListener = new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends CheckableLibraryEntry> c) {
                reloadAssets();
            }
        };

        mLibraries.addListener(reloadLibrariesListener);
        librariesListSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);

    }

    protected CheckableLibraryEntry createLibraryEntry(LibraryData libraryData, boolean checked) {
        return new CheckableLibraryEntry(libraryData.getLibrary(), libraryData.getRootDirectory()) {
            {
                setSelected(checked);
                selectedProperty().addListener(new ChangeListener<>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        reloadAssets();
                        saveCheckedLibrariesState();
                    }
                });
            }
        };
    }

    protected Window getStage() {
        return mRoot.getScene().getWindow();
    }

    protected ObservableList<CheckableLibraryEntry> getSelectedLibraries() {
        return mLibrariesListView.getSelectionModel().getSelectedItems();
    }

    protected void selectLibrary(CheckableLibraryEntry library) {
        mLibrariesListView.getSelectionModel().select(library);
    }

    protected Collection<AssetLibrary> getCheckedLibraries() {
        return mLibraries
                        .stream()
                        .filter(cle -> cle.isSelected())
                        .map(CheckableLibraryEntry::getLibrary)
                        .toList();
    }

    protected ObservableList<MaterialSetDescriptor> getSelectedMaterialSets() {
        return mMaterialSetChooserControl.getSelectedAssets();
    }

    protected ObservableList<SupportObjectDescriptor> getSelectedSupportObjects() {
        return mSupportObjectsChooserControl.getSelectedAssets();
    }

    protected void clearAssetSelection() {
        getSelectedMaterialSets().clear();
        getSelectedSupportObjects().clear();
    }

    public void reloadMaterialSets() {
        Collection<AssetLibrary> checkedLibraries = getCheckedLibraries();
        Optional<MaterialSetDescriptor> oSelectedAsset = mMaterialSetChooserControl.getSelectedAsset();
        mMaterialSetChooserControl.loadLibraries(checkedLibraries, Optional.of(acc -> oSelectedAsset.ifPresent(ass -> acc.selectObject(ass.getId()))));
    }

    protected void reloadSupportObjects() {
        Collection<AssetLibrary> checkedLibraries = getCheckedLibraries();
        Optional<SupportObjectDescriptor> oSelectedAsset = mSupportObjectsChooserControl.getSelectedAsset();
        mSupportObjectsChooserControl.loadLibraries(checkedLibraries, Optional.of(acc -> oSelectedAsset.ifPresent(ass -> acc.selectObject(ass.getId()))));
    }

    public void loadLibraries() {
        Collection<String> checkedLibraries = mAssetManager.getConfiguration().getCheckedLibraries();
        Collection<CheckableLibraryEntry> libraries = mAssetLoader.getAssetManager().getAssetLibraries().values()
                        .stream()
                        .map(ld -> createLibraryEntry(ld, checkedLibraries.contains(ld.getLibrary().getId())))
                        .toList();
        mLibraries.setAll(libraries);
    }

    protected void saveCheckedLibrariesState() {
        mAssetManager.getConfiguration().setCheckedLibraries(
            getCheckedLibraries()
                .stream()
                .map(library -> library.getId())
                .toList());
    }

    public void reloadAssets() {
        reloadMaterialSets();
        reloadSupportObjects();
        updateButtonStates();
    }

    protected void updateButtonStates() {
        // Libraries
        mNewLibraryButton.setTooltip(new Tooltip(Strings.LIBRARY_MANAGER_NEW_LIBRARY_TOOLTIP));
        ObservableList<CheckableLibraryEntry> selectedLibraries = getSelectedLibraries();
        int numSelectedLibraries = selectedLibraries.size();
        String libraryName = numSelectedLibraries == 1 ? selectedLibraries.getFirst().getLibrary().getName() : "-";
        mEditLibraryButton.setDisable(numSelectedLibraries != 1);
        mRemoveLibraryButton.setDisable(numSelectedLibraries == 0);
        mDeleteLibraryButton.setDisable(numSelectedLibraries == 0);
        switch (numSelectedLibraries) {
        case 0:
            mEditLibraryButton.setTooltip(null);
            mRemoveLibraryButton.setTooltip(null);
            mDeleteLibraryButton.setTooltip(null);
            break;
        case 1:
            mEditLibraryButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_LIBRARY_TOOLTIP, libraryName)));
            mRemoveLibraryButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_REMOVE_LIBRARY_1_TOOLTIP, libraryName)));
            mDeleteLibraryButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_DELETE_LIBRARY_1_TOOLTIP, libraryName)));
            break;
        default:
            mEditLibraryButton.setTooltip(null);
            mRemoveLibraryButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_REMOVE_LIBRARY_N_TOOLTIP, numSelectedLibraries)));
            mDeleteLibraryButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_DELETE_LIBRARY_N_TOOLTIP, numSelectedLibraries)));
            break;
        }

        // MaterialSets
        mNewMaterialSetButton.setTooltip(new Tooltip(Strings.LIBRARY_MANAGER_NEW_MATERIAL_SET_TOOLTIP));
        ObservableList<MaterialSetDescriptor> selectedMaterialSets = getSelectedMaterialSets();
        int numSelectedMaterials = selectedMaterialSets.size();
        String materialSetName = numSelectedMaterials == 1 ? selectedMaterialSets.getFirst().getName() : "-";
        mEditMaterialSetButton.setDisable(numSelectedMaterials == 0);
        mDeleteMaterialSetsButton.setDisable(numSelectedMaterials == 0);
        switch (numSelectedMaterials) {
        case 0:
            mEditMaterialSetButton.setTooltip(null);
            mDeleteMaterialSetsButton.setTooltip(null);
            break;
        case 1:
            mEditMaterialSetButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_MATERIAL_SET_1_TOOLTIP, materialSetName)));
            mDeleteMaterialSetsButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_DELETE_MATERIAL_SET_1_TOOLTIP, materialSetName)));
            break;
        default:
            mEditMaterialSetButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_MATERIAL_SETS_N_TOOLTIP, numSelectedMaterials)));
            mDeleteMaterialSetsButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_DELETE_MATERIAL_SETS_N_TOOLTIP, numSelectedMaterials)));
            break;
        }

        // Support objects
        mNewSupportObjectButton.setTooltip(new Tooltip(Strings.LIBRARY_MANAGER_NEW_SUPPORT_OBJECT_TOOLTIP));
        ObservableList<SupportObjectDescriptor> selectedSupportObjects = getSelectedSupportObjects();
        int numSelectedSupportObjects = selectedSupportObjects.size();
        String supportObjectName = numSelectedSupportObjects == 1 ? selectedSupportObjects.getFirst().getName() : "-";
        mEditSupportObjectButton.setDisable(numSelectedSupportObjects == 0);
        mDeleteSupportObjectsButton.setDisable(numSelectedSupportObjects == 0);
        switch (numSelectedSupportObjects) {
        case 0:
            mEditSupportObjectButton.setTooltip(null);
            mDeleteSupportObjectsButton.setTooltip(null);
            break;
        case 1:
            mEditSupportObjectButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_SUPPORT_OBJECT_1_TOOLTIP, supportObjectName)));
            mDeleteSupportObjectsButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_DELETE_SUPPORT_OBJECT_1_TOOLTIP, supportObjectName)));
            break;
        default:
            mEditSupportObjectButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_SUPPORT_OBJECTS_N_TOOLTIP, numSelectedSupportObjects)));
            mDeleteSupportObjectsButton.setTooltip(new Tooltip(MessageFormat.format(Strings.LIBRARY_MANAGER_DELETE_SUPPORT_OBJECTS_N_TOOLTIP, numSelectedSupportObjects)));
            break;
        }
    }

    protected void editSelectedMaterialSets() {
        List<MaterialSetDescriptor> selectedMaterialSets = getSelectedMaterialSets();
        if (selectedMaterialSets.size() == 1) {
            editMaterialSet(selectedMaterialSets.getFirst());
        } else if (selectedMaterialSets.size() > 0) {
            editAssetDescriptors(selectedMaterialSets);
        }
    }

    protected void editSelectedSupportObjects() {
        List<SupportObjectDescriptor> selectedSupportObjects = getSelectedSupportObjects();
        if (selectedSupportObjects.size() == 1) {
            editSupportObject(selectedSupportObjects.getFirst());
        } else if (selectedSupportObjects.size() > 0) {
            editAssetDescriptors(selectedSupportObjects);
        }
    }

    public void show(Stage primaryStage) {
        Scene scene = new Scene(mRoot, WINDOW_SIZE_X, WINDOW_SIZE_Y);
        mPrimaryStage = primaryStage;

        scene.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle(Strings.LIBRARY_MANAGER_WINDOW_TITLE);
        primaryStage.show();

        mPrimaryStage.setOnCloseRequest(event -> {
            // TODO: Save content?
        });
    }

    protected boolean queryCanDeleteObjects(String dialogTitle, String textDelete1, String textDeleteN, Collection<String> names) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(dialogTitle);
        double prefHeight;
        if (names.size() == 1) {
            alert.setHeaderText(textDelete1);
            prefHeight = 50;
        } else {
            alert.setHeaderText(textDeleteN);
            prefHeight = 100;
        }
        Label label = new Label(Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_OBJECTS_QUERY);
        label.setPadding(new Insets(0, 5, 5, 5));
        ListView<String> namesListView = new ListView<>();
        namesListView.setItems(FXCollections.observableArrayList(names));
        namesListView.setPrefSize(150, prefHeight);
        BorderPane contentPane = new BorderPane();
        contentPane.setTop(label);
        contentPane.setCenter(namesListView);
        alert.getDialogPane().setContent(contentPane);

        ButtonType buttonTypeOk = new ButtonType(Strings.OK, ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType(Strings.CANCEL, ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeOk, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.orElse(null) == buttonTypeOk) {
            // User confirms to delete objects
            return true;
        } else {
            // ... user chose CANCEL or closed the dialog
            return false;
        }
    }

    protected boolean queryCanDeleteLibraries(Collection<CheckableLibraryEntry> libraries) {
        List<String> names = libraries
                        .stream()
                        .map(ld -> ld.getLibrary().getName())
                        .collect(Collectors.toList());
        return queryCanDeleteObjects(
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_LIBRARIES_TITLE,
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_LIBRARIES_HEADER_1,
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_LIBRARIES_HEADER_N,
            names);
    }

    protected boolean queryCanDeleteMaterials(Collection<MaterialSetDescriptor> materialSets) {
        List<String> names = materialSets
                        .stream()
                        .map(AbstractAssetDescriptor::getName)
                        .collect(Collectors.toList());
        return queryCanDeleteObjects(
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_MATERIAL_SET_TITLE,
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_MATERIAL_SET_HEADER_1,
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_MATERIAL_SET_HEADER_N,
            names);
    }

    protected boolean queryCanDeleteSupportObjects(Collection<SupportObjectDescriptor> supportObjects) {
        List<String> names = supportObjects
                        .stream()
                        .map(AbstractAssetDescriptor::getName)
                        .collect(Collectors.toList());
        return queryCanDeleteObjects(
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_SUPPORT_OBJECTS_TITLE,
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_SUPPORT_OBJECTS_HEADER_1,
            Strings.LIBRARY_MANAGER_DIALOG_QUERY_DELETE_SUPPORT_OBJECTS_HEADER_N,
            names);
    }

    protected void queryDeleteLibraries(Collection<CheckableLibraryEntry> libraries) {
        Map<String, LibraryData> assetLibraries = mAssetManager.getAssetLibraries();
        if (queryCanDeleteLibraries(libraries)) {
            for (CheckableLibraryEntry libraryEntry : libraries) {
                try {
                    LibraryData libraryData = assetLibraries.get(libraryEntry.getLibrary().getId());
                    if (libraryData == null) {
                        continue;
                    }
                    mAssetManager.deleteAssetLibrary(libraryData);
                } catch (IOException e) {
                    AssetLibrary library = libraryEntry.getLibrary();
                    log.error("Error deleting library '" + library.getName() + "', id: " + library.getId(), e);
                }
            }
            reloadAssets();
        }
    }

    protected void queryDeleteMaterialSets(Collection<MaterialSetDescriptor> materialSets) {
        if (queryCanDeleteMaterials(materialSets)) {
            for (MaterialSetDescriptor descriptor : materialSets) {
                try {
                    mAssetManager.deleteAsset(descriptor.getSelfRef());
                } catch (IOException e) {
                    log.error("Error deleting material '" + descriptor.getName() + "', id: " + descriptor.getId(), e);
                }
            }
            reloadMaterialSets();
        }
    }

    protected void queryNewMaterialSet() {
        Alert editDialog = new Alert(AlertType.NONE);
        editDialog.setTitle(Strings.LIBRARY_MANAGER_CREATE_MATERIAL_SET_DIALOG_TITLE);
        editDialog.setHeaderText(Strings.LIBRARY_MANAGER_CREATE_MATERIAL_SET_HEADER_DIALOG_HEADER);

        MaterialSetLocationChooserControl materialSetLocationChooserControl = new MaterialSetLocationChooserControl(mAssetManager);
        BooleanExpression invalidProperty = Bindings.isNull(materialSetLocationChooserControl.materialSetRootLocationProperty());

        DialogPane dialogPane = editDialog.getDialogPane();
        dialogPane.setContent(materialSetLocationChooserControl);
        Scene scene = dialogPane.getScene();

        scene.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        Stage stage = (Stage) scene.getWindow();
        stage.setHeight(200);
        stage.setWidth(400);

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(invalidProperty);
        materialSetLocationChooserControl.requestFocus();

        editDialog.showAndWait().ifPresent(res -> {
            if (res != ButtonType.OK) {
                return;
            }
            try {
                IMaterialSetLocation materialSetLocation = materialSetLocationChooserControl.getMaterialSetLocation();
                MaterialSetDescriptor newMaterialSetDescriptor;
                if (materialSetLocation instanceof LibraryRootMaterialSetLocation rmsl) {
                    String libraryId = rmsl.LibraryId();
                    newMaterialSetDescriptor = mAssetManager.createRootMaterialSet(libraryId);

                    Collection<MaterialSetDescriptor> msDescriptorsInLibrary =
                                    mAssetManager
                                        .loadMaterialSetDescriptors(new LibraryAssetPathAnchor(libraryId), false);

                    String newName = Namespace.generateName(Strings.NEW_MATERIAL_SET_NAME_PATTERN,
                        msDescriptorsInLibrary.stream().map(AbstractAssetDescriptor::getName).collect(Collectors.toList()), 1);
                    newMaterialSetDescriptor.setName(newName);
                } else if (materialSetLocation instanceof SupportObjectMaterialSetLocation somsl) {
                    AssetRefPath supportObjectDescriptorRef = somsl.supportObjectRefPath();
                    newMaterialSetDescriptor = mAssetManager.createSupportObjectMaterialSet(supportObjectDescriptorRef);

                    Collection<MaterialSetDescriptor> supportObjectMaterialSetDescriptors =
                                    mAssetManager.resolveAssetLocation(supportObjectDescriptorRef)
                                        .resolveLocalMaterialSetsDirectory()
                                        .loadMaterialSetDescriptors();

                    SupportObjectDescriptor soDescriptor = mAssetManager.loadSupportObjectDescriptor(supportObjectDescriptorRef);
                    String newName = Namespace.generateName(Strings.NEW_MATERIAL_SET_NAME_PATTERN,
                        supportObjectMaterialSetDescriptors.stream().map(AbstractAssetDescriptor::getName).collect(Collectors.toList()), 1);
                    newName = MessageFormat.format(Strings.NEW_MATERIAL_SET_IN_SO_NAME_PATTERN, newName, soDescriptor.getName());
                    newMaterialSetDescriptor.setName(newName);
                } else {
                    return;
                }

                mAssetManager.saveMaterialSetDescriptor(newMaterialSetDescriptor);
                editMaterialSet(newMaterialSetDescriptor);
                // editMaterialSet triggers reloading of assets
            } catch (IOException e) {
                log.error("Error while creating new material set", e);
            }
        });
    }

    protected void queryNewSupportObject() {
        SupportObjectAnchorChooserDialog dialog = new SupportObjectAnchorChooserDialog(mAssetManager, Strings.LIBRARY_MANAGER_CREATE_SUPPORT_OBJECT_DIALOG_TITLE);
        Optional<IAssetPathAnchor> oAnchor = dialog.showAndWait();
        if (oAnchor.isEmpty()) {
            return;
        }
        try {
            SupportObjectDescriptor descriptor = mAssetManager.createSupportObject(oAnchor.get());

            String newName = Namespace.generateName(Strings.NEW_SUPPORT_OBJECT_NAME_PATTERN,
                mAssetManager.loadAllLibrarySupportObjectDescriptors().stream().map(AbstractAssetDescriptor::getName).collect(Collectors.toList()), 1);
            descriptor.setName(newName);
            mAssetManager.saveSupportObjectDescriptor(descriptor);
            editSupportObject(descriptor);
        } catch (IOException e) {
            log.error("Error while creating new support object", e);
        }
        reloadAssets();
    }

    protected void queryDeleteSupportObjects(Collection<SupportObjectDescriptor> supportObjects) {
        if (queryCanDeleteSupportObjects(supportObjects)) {
            for (SupportObjectDescriptor descriptor : supportObjects) {
                try {
                    mAssetManager.deleteAsset(descriptor.getSelfRef());
                } catch (IOException e) {
                    log.error("Error deleting support object '" + descriptor.getName() + "', id: " + descriptor.getId(), e);
                }
            }
            reloadAssets();
        }
    }

    protected void editAssetDescriptors(Collection<? extends AbstractAssetDescriptor> descriptors) {
        Alert editDialog = new Alert(AlertType.NONE);
        int numObjects = descriptors.size();
        editDialog.setTitle(MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_N_OBJECTS_DIALOG_TITLE, numObjects));
        editDialog.setHeaderText(MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_N_OBJECTS_DIALOG_HEADER, numObjects));
        DialogPane dialogPane = editDialog.getDialogPane();
        MultipleAssetsEditControl control = new MultipleAssetsEditControl();
        control.initializeValues(descriptors, mAssetManager);
        dialogPane.setContent(control);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        editDialog.showAndWait().ifPresent(result -> {
            if (ButtonType.OK.equals(result)) {
                control.updateDescriptors(descriptors);
            }
            boolean materialSetsUpdated = false;
            boolean supportObjectsUpdated = false;
            for (AbstractAssetDescriptor descriptor : new ArrayList<>(descriptors)) {
                try {
                    mAssetManager.saveAssetDescriptor(descriptor);
                    if (descriptor instanceof MaterialSetDescriptor) {
                        materialSetsUpdated = true;
                    } else if (descriptor instanceof SupportObjectDescriptor) {
                        supportObjectsUpdated = true;
                    }
                } catch (IOException e) {
                    log.error("Error saving asset descriptor '" + descriptor.getId() + "'", e);
                }
                if (materialSetsUpdated) {
                    reloadMaterialSets();
                }
                if (supportObjectsUpdated) {
                    reloadSupportObjects();
                }
            }
        });
    }

    protected void editMaterialSet(MaterialSetDescriptor descriptor) {
        Alert editDialog = new Alert(AlertType.NONE);
        editDialog.setTitle(Strings.LIBRARY_MANAGER_EDIT_MATERIAL_SET_DIALOG_TITLE);
        editDialog.setHeaderText(Strings.LIBRARY_MANAGER_EDIT_MATERIAL_SET_DIALOG_HEADER);
        editDialog.setWidth(800);
        editDialog.setHeight(1000);
        editDialog.setResizable(true);
        DialogPane dialogPane = editDialog.getDialogPane();
        dialogPane.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());
        MaterialSetEditControl control = new MaterialSetEditControl(mAssetManager);
        control.initializeValues(descriptor);
        dialogPane.setContent(control);
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);
        editDialog.showAndWait().ifPresent(result -> {
            if (ButtonType.CLOSE.equals(result)) {
                control.saveValues(descriptor);
            }
            // The dialog can not be cancelled because the change process is so complex.
            // So we do not know if anything was changed, have to reload.
            reloadMaterialSets();
        });
    }

    protected void editSupportObject(SupportObjectDescriptor descriptor) {
        Alert editDialog = new Alert(AlertType.NONE);
        editDialog.setTitle(Strings.LIBRARY_MANAGER_EDIT_SUPPORT_OBJECT_DIALOG_TITLE);
        editDialog.setHeaderText(Strings.LIBRARY_MANAGER_EDIT_SUPPORT_OBJECT_DIALOG_HEADER);
        editDialog.setWidth(800);
        editDialog.setWidth(1000);
        editDialog.setResizable(true);
        DialogPane dialogPane = editDialog.getDialogPane();
        dialogPane.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());
        SupportObjectEditControl control = new SupportObjectEditControl(mAssetManager);
        control.initializeValues(descriptor);
        dialogPane.setContent(control);
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);
        editDialog.showAndWait().ifPresent(result -> {
            if (ButtonType.CLOSE.equals(result)) {
                control.saveValues(descriptor);
            }
            // The dialog can not be cancelled because the change process is so complex.
            // So we do not know if anything was changed, have to reload.
            reloadSupportObjects();
        });
    }

    protected void editAssetLibrary(CheckableLibraryEntry libraryEntry) {
        Alert editDialog = new Alert(AlertType.NONE);
        editDialog.setTitle(Strings.LIBRARY_MANAGER_EDIT_LIBRARY_DIALOG_TITLE);
        editDialog.setHeaderText(Strings.LIBRARY_MANAGER_EDIT_LIBRARY_DIALOG_HEADER);
        DialogPane dialogPane = editDialog.getDialogPane();
        dialogPane.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());
        AssetLibraryEditControl control = new AssetLibraryEditControl();
        AssetLibrary library = libraryEntry.getLibrary();
        control.initializeValues(library, libraryEntry.getRootDirectory());
        dialogPane.setContent(control);
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);
        editDialog.showAndWait().ifPresent(result -> {
            if (ButtonType.CLOSE.equals(result)) {
                control.updateValues(library);
                Map<String, LibraryData> assetLibraries = mAssetManager.getAssetLibraries();
                LibraryData libraryData = assetLibraries.get(library.getId());
                if (libraryData == null) {
                    return;
                }
                mAssetManager.saveAssetLibrary(libraryData);
            }
        });
    }

    protected DirectoryChooser getLibraryDirectoryChooser(String dialogTitle) {
        DirectoryChooser result = new DirectoryChooser();
        result.setTitle(dialogTitle);
        AssetManagerConfiguration configuration = mAssetManager.getConfiguration();
        Optional<Path> oPath = configuration.getLastAssetLibraryPath();
        oPath.ifPresent(path -> FxUtils.trySetInitialDirectory(result, path));
        return result;
    }

    protected void queryNewLibrary() {
        DirectoryChooser dialog = getLibraryDirectoryChooser(Strings.LIBRARY_MANAGER_NEW_ASSET_LIBRARY_DIALOG_TITLE);
        File libraryDir = dialog.showDialog(getStage());
        if (libraryDir == null) {
            return;
        }
        Path libraryPath = libraryDir.toPath();
        LibraryData libraryData = mAssetManager.createNewAssetLibrary(new PlainFileSystemDirectoryLocator(libraryPath), Strings.LIBRARY_MANAGER_NEW_ASSET_LIBRARY_NAME);
        AssetManagerConfiguration configuration = mAssetManager.getConfiguration();
        configuration.setLastAssetLibraryPath(libraryPath);
        CheckableLibraryEntry libraryEntry = createLibraryEntry(libraryData, true);
        mLibraries.add(libraryEntry);
        editAssetLibrary(libraryEntry);
        selectLibrary(libraryEntry);
    }

    protected void queryOpenLibraries() {
        LibraryImporterDialog dialog = new LibraryImporterDialog();
        AssetManagerConfiguration configuration = mAssetManager.getConfiguration();
        Optional<Path> oPath = configuration.getLastAssetLibraryPath();
        Collection<IDirectoryLocator> openLibraryDirectories = mAssetManager.getAssetLibraries()
                .values()
                .stream()
                .map(ld -> ld.getRootDirectory())
                .toList();
        dialog.setDisabledLibraries(openLibraryDirectories);
        oPath.ifPresent(path -> dialog.setRootPath(path));

        Optional<Collection<IDirectoryLocator>> oLibraryDirectories = dialog.showAndWait();
        oLibraryDirectories.ifPresent(directories -> {
            List<CheckableLibraryEntry> newLibraries = new ArrayList<>();
            for (IDirectoryLocator libraryDirectory : directories) {
                LibraryData libraryData = mAssetManager.openAssetLibrary(libraryDirectory);
                CheckableLibraryEntry libraryEntry = createLibraryEntry(libraryData, true);
                if (!mLibraries.contains(libraryEntry)) {
                    newLibraries.add(libraryEntry);
                }
            }
            mLibraries.addAll(newLibraries);
            dialog.getORootPath().ifPresent(rootPath -> {
                configuration.setLastAssetLibraryPath(rootPath);
            });
        });
    }

    protected void removeLibraries(Collection<CheckableLibraryEntry> libraries) {
        for (CheckableLibraryEntry library : libraries) {
            mAssetManager.closeAssetLibrary(library.getLibrary().getId());
        }
        mLibraries.removeAll(libraries);
    }
}
