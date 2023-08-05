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

import de.dh.cad.architect.model.assets.AssetRefPath.IAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.LibraryAssetPathAnchor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

public class SupportObjectAnchorChooserControl extends BorderPane implements Initializable {
    public static final String FXML = "SupportObjectAnchorChooserControl.fxml";

    protected final AssetManager mAssetManager;
    protected final SimpleObjectProperty<IAssetPathAnchor> mSupportObjectAnchorProperty = new SimpleObjectProperty<>();

    @FXML
    protected ChoiceBox<LibraryData> mLibraryChoiceBox;

    public SupportObjectAnchorChooserControl(AssetManager assetManager) {
        mAssetManager = assetManager;
        FXMLLoader fxmlLoader = new FXMLLoader(SupportObjectAnchorChooserControl.class.getResource(FXML));
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
        mLibraryChoiceBox.setItems(FXCollections.observableArrayList(libraries.values()));
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
        mLibraryChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends LibraryData> observable, LibraryData oldValue, LibraryData newValue) {
                updateResultAnchor();
            }
        });
    }

    public LibraryData getSelectedLibrary() {
        return mLibraryChoiceBox.getValue();
    }

    public ReadOnlyObjectProperty<IAssetPathAnchor> supportObjectAnchorProperty() {
        return mSupportObjectAnchorProperty;
    }

    public IAssetPathAnchor getSupportObjectAnchor() {
        return mSupportObjectAnchorProperty.get();
    }

    protected Optional<IAssetPathAnchor> calculateResultAnchor() {
        LibraryData library = getSelectedLibrary();
        if (library == null) {
            return Optional.empty();
        }
        return Optional.of(new LibraryAssetPathAnchor(library.getLibrary().getId()));
    }

    protected void updateResultAnchor() {
        Optional<IAssetPathAnchor> oResultAnchor = calculateResultAnchor();
        mSupportObjectAnchorProperty.set(oResultAnchor.orElse(null));
    }
}
