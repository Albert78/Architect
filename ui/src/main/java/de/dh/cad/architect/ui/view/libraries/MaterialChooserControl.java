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
package de.dh.cad.architect.ui.view.libraries;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

import de.dh.cad.architect.model.assets.AssetLibrary;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;

public class MaterialChooserControl extends BorderPane implements Initializable {
    public static final String FXML = "MaterialChooserControl.fxml";

    protected final AssetLoader mAssetLoader;
    protected AssetsTableControl<MaterialSetDescriptor> mAssetsTableControl;

    protected Node mCurrentContentControl = null;
    protected final SimpleObjectProperty<AssetRefPath> mSelectedMaterialProperty = new SimpleObjectProperty<>(null);

    @FXML
    protected BorderPane mTreeParentPane;

    @FXML
    protected BorderPane mContentPane;

    public MaterialChooserControl(AssetLoader assetLoader) {
        mAssetLoader = assetLoader;
        FXMLLoader fxmlLoader = new FXMLLoader(MaterialChooserControl.class.getResource(FXML));
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
        showUserHintControl(Strings.LIBRARY_MANAGER_NO_ASSET_SELECTED);
        mAssetsTableControl = AssetsTableControl.create(mAssetLoader, AssetType.MaterialSet);
        mAssetsTableControl.setTableControlSelectionMode(SelectionMode.SINGLE);
        ReadOnlyObjectProperty<MaterialSetDescriptor> selectedItemProperty = mAssetsTableControl.selectedItemProperty();
        selectedItemProperty.addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends MaterialSetDescriptor> observable, MaterialSetDescriptor oldValue, MaterialSetDescriptor newValue) {
                if (newValue != null) {
                    showMaterialSet(newValue);
                } else {
                    mSelectedMaterialProperty.set(null);
                    showUserHintControl(Strings.LIBRARY_MANAGER_NO_ASSET_SELECTED);
                }
            }
        });
        mTreeParentPane.setCenter(mAssetsTableControl);
    }

    public ReadOnlyObjectProperty<AssetRefPath> selectedMaterialProperty() {
        return mSelectedMaterialProperty;
    }

    public Optional<AssetRefPath> getSelectedMaterial() {
        return Optional.ofNullable(selectedMaterialProperty().get());
    }

    public void selectMaterial(AssetRefPath materialRef) {
        if (materialRef == null) {
            mAssetsTableControl.selectObject(null);
        } else {
            mAssetsTableControl.selectObject(materialRef.getAssetId());
            if (mCurrentContentControl instanceof ShowMaterialSetControl smsc) {
                smsc.selectMaterial(materialRef.getOMaterialName().get());
            }
        }
    }

    public void loadLibraries() {
        Collection<AssetLibrary> libraries = mAssetLoader.getAssetManager().getAssetLibraries().values()
                        .stream()
                        .map(LibraryData::getLibrary)
                        .toList();
        loadLibraries(libraries);
    }

    public void loadLibraries(Collection<AssetLibrary> libraries) {
        mAssetsTableControl.loadLibraries_progress_async(libraries);
    }

    ChangeListener<MaterialDescriptor> SELECTED_MATERIAL_CHANGE_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends MaterialDescriptor> observable, MaterialDescriptor oldValue,
            MaterialDescriptor newValue) {
            mSelectedMaterialProperty.set(newValue.getMaterialRef());
        }
    };

    protected void setContentControl(Node control) {
        if (mCurrentContentControl instanceof ShowMaterialSetControl smsc) {
            smsc.selectedMaterialProperty().removeListener(SELECTED_MATERIAL_CHANGE_LISTENER);
        }
        mCurrentContentControl = control;
        if (mCurrentContentControl instanceof ShowMaterialSetControl smsc) {
            smsc.selectedMaterialProperty().addListener(SELECTED_MATERIAL_CHANGE_LISTENER);
            mSelectedMaterialProperty.set(smsc.getSelectedMaterialDescriptor().getMaterialRef());
        }
        mContentPane.setCenter(control);
    }

    protected void showMaterialSet(MaterialSetDescriptor descriptor) {
        ShowMaterialSetControl control = new ShowMaterialSetControl(descriptor, mAssetLoader);
        setContentControl(control);
    }

    protected void showUserHintControl(String userHint) {
        UserHintControl control = new UserHintControl(userHint);
        setContentControl(control);
    }
}
