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

import java.util.Optional;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.assets.AssetLoader;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;
import javafx.util.Callback;

public class AssetChooserDialog<T extends AbstractAssetDescriptor> extends Dialog<T> {
    protected static final int DIALOG_MIN_WIDTH = 1200;
    protected static final int DIALOG_MIN_HEIGHT = 400;

    protected final AssetChooserControl<T> mAssetChooserControl;
    protected final AssetType mAssetType;

    public AssetChooserDialog(AssetLoader assetLoader, String dialogTitle, AssetType assetType) {
        mAssetChooserControl = new AssetChooserControl<>(assetLoader, assetType);
        mAssetChooserControl.setSelectionMode(SelectionMode.SINGLE);
        mAssetType = assetType;

        setTitle(dialogTitle);
        // TODO: Nice graphic
        DialogPane dialogPane = getDialogPane();
        ObservableList<ButtonType> buttonTypes = dialogPane.getButtonTypes();
        buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL);

        dialogPane.setContent(mAssetChooserControl);
        mAssetChooserControl.selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                validate();
            }
        });
        mAssetChooserControl.setAssetChoosenHandler((source, assetData) -> {
            setResult(assetData);
        });
        setResizable(true);

        Scene scene = dialogPane.getScene();

        scene.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        Stage stage = (Stage) scene.getWindow();
        stage.setMinHeight(DIALOG_MIN_HEIGHT);
        stage.setMinWidth(DIALOG_MIN_WIDTH);

        validate();

        setResultConverter(new Callback<ButtonType, T>() {
            @Override
            public T call(ButtonType dialogButton) {
                if (dialogButton == ButtonType.OK) {
                    return getAssetDescriptor().orElse(null);
                }
                return null;
            }
        });
        mAssetChooserControl.requestFocus();
    }

    public static <T extends AbstractAssetDescriptor> AssetChooserDialog<T> createWithProgressIndicator(AssetLoader assetLoader, String dialogTitle, AssetType assetType) {
        AssetChooserDialog<T> result = new AssetChooserDialog<>(assetLoader, dialogTitle, assetType);
        result.loadLibraries();
        return result;
    }

    protected void loadLibraries() {
        mAssetChooserControl.loadLibraries();
    }

    protected void validate() {
        DialogPane dialogPane = getDialogPane();
        Node okButton = dialogPane.lookupButton(ButtonType.OK);

        okButton.setDisable(getAssetDescriptor().isEmpty());
    }

    public void selectAsset(String assetId) {
        mAssetChooserControl.selectObject(assetId);
    }

    public Optional<T> getAssetDescriptor() {
        return mAssetChooserControl.getSelectedAsset();
    }
}
