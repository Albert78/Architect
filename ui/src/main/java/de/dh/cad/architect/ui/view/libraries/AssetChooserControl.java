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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.model.assets.AssetLibrary;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;

public class AssetChooserControl<T extends AbstractAssetDescriptor> extends BorderPane implements Initializable {
    public static final String FXML = "AssetChooserControl.fxml";

    protected final AssetLoader mAssetLoader;
    protected final AssetType mAssetType;
    protected AssetsTableControl<T> mAssetsTableControl;

    protected Node mCurrentContentControl = null;

    @FXML
    protected BorderPane mTreeParentPane;

    @FXML
    protected BorderPane mContentPane;

    public AssetChooserControl(AssetLoader assetLoader, AssetType assetType) {
        mAssetLoader = assetLoader;
        mAssetType = assetType;
        FXMLLoader fxmlLoader = new FXMLLoader(AssetChooserControl.class.getResource(FXML));
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
        mAssetsTableControl = AssetsTableControl.create(mAssetLoader, mAssetType);
        ObservableList<T> selectedItems = mAssetsTableControl.getSelectedItems();
        selectedItems.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends T> c) {
                int numSelectedItems = selectedItems.size();
                if (numSelectedItems == 0) {
                    showUserHintControl(Strings.LIBRARY_MANAGER_NO_ASSET_SELECTED);
                } else if (numSelectedItems == 1) {
                    T selectedItem = selectedItems.get(0);
                    if (selectedItem instanceof MaterialSetDescriptor msd) {
                        showMaterialSet(msd);
                    } else if (selectedItem instanceof SupportObjectDescriptor sod) {
                        showSupportObject(sod);
                    } else {
                        showUserHintControl(Strings.LIBRARY_MANAGER_NO_ASSET_SELECTED);
                    }
                } else {
                    // More then 1 item selected
                    showUserHintControl(MessageFormat.format(Strings.LIBRARY_MANAGER_N_ASSETS_OF_TYPE_SELECTED, numSelectedItems, mAssetType));
                }
            }
        });
        mTreeParentPane.setCenter(mAssetsTableControl);
    }

    public IAssetChoosenHandler<T> getAssetChoosenHandler() {
        return mAssetsTableControl.getAssetChoosenHandler();
    }

    public void setAssetChoosenHandler(IAssetChoosenHandler<T> value) {
        mAssetsTableControl.setAssetChoosenHandler(value);
    }

    public ReadOnlyObjectProperty<T> selectedItemProperty() {
        return mAssetsTableControl.selectedItemProperty();
    }

    public void selectObject(String assetId) {
        mAssetsTableControl.selectObject(assetId);
    }

    public Optional<T> getSelectedAsset() {
        return Optional.ofNullable(mAssetsTableControl.getSelectedItem());
    }

    public ObservableList<T> getSelectedAssets() {
        return mAssetsTableControl.getSelectedItems();
    }

    public void setSelectionMode(SelectionMode value) {
        mAssetsTableControl.setTableControlSelectionMode(value);
    }

    public AssetsTableControl<T> getLibrariesTableControl() {
        return mAssetsTableControl;
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

    protected void setContentControl(Node control) {
        if (mCurrentContentControl != null) {
            // Uninitialize - currently nothing to do
        }
        mCurrentContentControl = control;
        if (mCurrentContentControl != null) {
            // Initialize - currently nothing to do
        }
        mContentPane.setCenter(control);
    }

    protected void showMaterialSet(MaterialSetDescriptor descriptor) {
        ShowMaterialSetControl control = new ShowMaterialSetControl(descriptor, mAssetLoader);
        setContentControl(control);
    }

    protected void showSupportObject(SupportObjectDescriptor descriptor) {
        ShowSupportObjectControl control = new ShowSupportObjectControl(descriptor, mAssetLoader);
        setContentControl(control);
    }

    protected void showUserHintControl(String userHint) {
        UserHintControl control = new UserHintControl(userHint);
        setContentControl(control);
    }
}
