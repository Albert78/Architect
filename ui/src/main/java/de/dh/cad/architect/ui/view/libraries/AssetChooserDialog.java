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

import java.util.Optional;
import java.util.function.Consumer;

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

/**
 * Dialog for choosing an asset in form of an asset descriptor from the asset library.
 */
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

        dialogPane.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        Scene scene = dialogPane.getScene();

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
        return createWithProgressIndicator(assetLoader, dialogTitle, assetType, Optional.empty());
    }

    public static <T extends AbstractAssetDescriptor> AssetChooserDialog<T> createWithProgressIndicator(AssetLoader assetLoader, String dialogTitle, AssetType assetType, Consumer<AssetChooserDialog<T>> onFinishedLoading) {
        return createWithProgressIndicator(assetLoader, dialogTitle, assetType, Optional.of(onFinishedLoading));
    }

    public static <T extends AbstractAssetDescriptor> AssetChooserDialog<T> createWithProgressIndicator(AssetLoader assetLoader, String dialogTitle, AssetType assetType, Optional<Consumer<AssetChooserDialog<T>>> oOnFinishedLoading) {
        AssetChooserDialog<T> result = new AssetChooserDialog<>(assetLoader, dialogTitle, assetType);
        result.loadLibraries(oOnFinishedLoading);
        return result;
    }

    protected void loadLibraries(Optional<Consumer<AssetChooserDialog<T>>> oOnFinishedLoading) {
        mAssetChooserControl.loadLibraries(oOnFinishedLoading.map(onFinishedLoading -> acc -> onFinishedLoading.accept(AssetChooserDialog.this)));
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
