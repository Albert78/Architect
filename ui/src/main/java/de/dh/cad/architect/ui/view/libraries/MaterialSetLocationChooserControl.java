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
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.AssetRefPath.LibraryAssetPathAnchor;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

public class MaterialSetLocationChooserControl extends BorderPane implements Initializable {
    public static interface IMaterialSetLocation {
        // Just a marker interface for the record types
    }

    public static record LibraryRootMaterialSetLocation(String LibraryId) implements IMaterialSetLocation {}
    public static record SupportObjectMaterialSetLocation(AssetRefPath supportObjectRefPath) implements IMaterialSetLocation {}

    public static final String FXML = "MaterialSetLocationChooserControl.fxml";

    protected final AssetManager mAssetManager;
    protected final SimpleObjectProperty<IMaterialSetLocation> mMaterialSetLocationProperty = new SimpleObjectProperty<>();

    @FXML
    protected ChoiceBox<LibraryData> mLibraryChoiceBox;

    @FXML
    protected CheckBox mCreateUnderSupportObjectCheckBox;

    @FXML
    protected ChoiceBox<SupportObjectDescriptor> mSupportObjectChoiceBox;

    public MaterialSetLocationChooserControl(AssetManager assetManager) {
        mAssetManager = assetManager;
        FXMLLoader fxmlLoader = new FXMLLoader(MaterialSetLocationChooserControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Map<String, LibraryData> libraries = mAssetManager.getAssetLibraries().values()
                        .stream()
                        .collect(Collectors.<LibraryData, String, LibraryData>toMap(ld -> ld.getLibrary().getName(), Function.identity()));
        mLibraryChoiceBox.setConverter(new StringConverter<AssetManager.LibraryData>() {
            @Override
            public String toString(LibraryData libraryData) {
                return libraryData == null ? Strings.NO_ENTRY_CHOOSEN : libraryData.getLibrary().getName();
            }

            @Override
            public LibraryData fromString(String libraryName) {
                return libraryName == null ? null : libraries.get(libraryName);
            }
        });
        SingleSelectionModel<LibraryData> libraryChoiceBoxSelectionModel = mLibraryChoiceBox.getSelectionModel();
        libraryChoiceBoxSelectionModel.selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends LibraryData> observable, LibraryData oldValue, LibraryData newValue) {
                checkUpdateSupportObjects();
                updateResultLocation();
            }
        });
        mLibraryChoiceBox.setItems(FXCollections.observableArrayList(libraries.values()));
        if (!libraries.isEmpty()) {
            libraryChoiceBoxSelectionModel.select(0);
        }

        mCreateUnderSupportObjectCheckBox.selectedProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                checkUpdateSupportObjects();
                updateResultLocation();
            }
        });
        mSupportObjectChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends SupportObjectDescriptor> observable, SupportObjectDescriptor oldValue, SupportObjectDescriptor newValue) {
                updateResultLocation();
            }
        });
        checkUpdateSupportObjects();
    }

    public LibraryData getSelectedLibrary() {
        return mLibraryChoiceBox.getValue();
    }

    public boolean isCreateUnderSupportObject() {
        return mCreateUnderSupportObjectCheckBox.isSelected();
    }

    public SupportObjectDescriptor getSelectedSupportObject() {
        return mSupportObjectChoiceBox.getValue();
    }

    public Optional<SupportObjectDescriptor> getParentSupportObject() {
        return isCreateUnderSupportObject() ? Optional.ofNullable(getSelectedSupportObject()) : Optional.empty();
    }

    public ReadOnlyObjectProperty<IMaterialSetLocation> materialSetRootLocationProperty() {
        return mMaterialSetLocationProperty;
    }

    /**
     * Asset location either pointing to an asset collection's root folder or to a support object's asset folder.
     * To finish the process of adding the new material set, the material sets directory and the new material set's ID have to be added to the
     * returned path.
     */
    public IMaterialSetLocation getMaterialSetLocation() {
        return mMaterialSetLocationProperty.get();
    }

    protected void checkUpdateSupportObjects() {
        boolean createUnderSupportObject = isCreateUnderSupportObject();
        mSupportObjectChoiceBox.setDisable(!createUnderSupportObject);
        LibraryData libraryData = getSelectedLibrary();
        ObservableList<SupportObjectDescriptor> items = mSupportObjectChoiceBox.getItems();
        if (createUnderSupportObject && libraryData != null) {
            fillSupportObjectsChoiceBox(libraryData.getLibrary().getId());
        } else {
            items.clear();
        }
    }

    protected Optional<? extends IMaterialSetLocation> calculateResultRootLocation() {
        LibraryData library = getSelectedLibrary();
        if (library == null) {
            return Optional.empty();
        }
        if (isCreateUnderSupportObject()) {
            SupportObjectDescriptor supportObject = getSelectedSupportObject();
            if (supportObject == null) {
                return Optional.empty();
            }
            return Optional.of(new SupportObjectMaterialSetLocation(supportObject.getSelfRef()));
        } else {
            return Optional.of(new LibraryRootMaterialSetLocation(library.getLibrary().getId()));
        }
    }

    protected void updateResultLocation() {
        Optional<? extends IMaterialSetLocation> oResultLocation = calculateResultRootLocation();
        mMaterialSetLocationProperty.set(oResultLocation.orElse(null));
    }

    protected static String getNameForDescriptor(SupportObjectDescriptor descriptor) {
        return descriptor.getName() + " (" + descriptor.getId() + ")";
    }

    protected void fillSupportObjectsChoiceBox(String libraryId) {
        try {
            Map<String, SupportObjectDescriptor> descriptors = mAssetManager
                            .loadSupportObjectDescriptors(new LibraryAssetPathAnchor(libraryId))
                            .stream()
                            .collect(Collectors.toMap(MaterialSetLocationChooserControl::getNameForDescriptor, Function.identity()));
            mSupportObjectChoiceBox.setConverter(new StringConverter<SupportObjectDescriptor>() {
                @Override
                public String toString(SupportObjectDescriptor descriptor) {
                    return descriptor == null ? Strings.NO_ENTRY_CHOOSEN : getNameForDescriptor(descriptor);
                }

                @Override
                public SupportObjectDescriptor fromString(String descriptorName) {
                    return descriptorName == null ? null : descriptors.get(descriptorName);
                }
            });
            mSupportObjectChoiceBox.setItems(FXCollections.observableArrayList(descriptors.values()));
            if (!descriptors.isEmpty()) {
                mSupportObjectChoiceBox.getSelectionModel().select(0);
            }
        } catch (IOException e) {
            new Alert(AlertType.ERROR, Strings.ERROR_LOADING_DATA).showAndWait();
        }
    }
}
