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

import de.dh.cad.architect.model.assets.AssetRefPath;
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
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Dialog for choosing a material from the asset library.
 */
public class MaterialChooserDialog extends Dialog<AssetRefPath> {
    protected static final int DIALOG_MIN_WIDTH = 1200;
    protected static final int DIALOG_MIN_HEIGHT = 400;

    protected MaterialChooserControl mMaterialChooserControl;

    public MaterialChooserDialog(AssetLoader assetLoader, String dialogTitle) {
        mMaterialChooserControl = new MaterialChooserControl(assetLoader);

        setTitle(dialogTitle);
        // TODO: Nice graphic
        DialogPane dialogPane = getDialogPane();
        ObservableList<ButtonType> buttonTypes = dialogPane.getButtonTypes();
        buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL);

        dialogPane.setContent(mMaterialChooserControl);
        mMaterialChooserControl.selectedMaterialProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends AssetRefPath> observable, AssetRefPath oldValue, AssetRefPath newValue) {
                validate();
            }
        });
        setResizable(true);

        dialogPane.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        Scene scene = dialogPane.getScene();
        Stage stage = (Stage) scene.getWindow();
        stage.setMinHeight(DIALOG_MIN_HEIGHT);
        stage.setMinWidth(DIALOG_MIN_WIDTH);

        validate();

        setResultConverter(new Callback<ButtonType, AssetRefPath>() {
            @Override
            public AssetRefPath call(ButtonType dialogButton) {
                if (dialogButton == ButtonType.OK) {
                    return getMaterialDescriptor().orElse(null);
                }
                return null;
            }
        });
        mMaterialChooserControl.requestFocus();
    }

    public static MaterialChooserDialog createWithProgressIndicator(AssetLoader assetLoader, String dialogTitle) {
        return createWithProgressIndicator(assetLoader, dialogTitle, Optional.empty());
    }

    public static MaterialChooserDialog createWithProgressIndicator(AssetLoader assetLoader, String dialogTitle, Consumer<MaterialChooserDialog> onFinishedLoading) {
        return createWithProgressIndicator(assetLoader, dialogTitle, Optional.of(onFinishedLoading));
    }

    public static MaterialChooserDialog createWithProgressIndicator(AssetLoader assetLoader, String dialogTitle, Optional<Consumer<MaterialChooserDialog>> oOnFinishedLoading) {
        MaterialChooserDialog result = new MaterialChooserDialog(assetLoader, dialogTitle);
        result.loadLibraries(oOnFinishedLoading);
        return result;
    }

    protected void loadLibraries(Optional<Consumer<MaterialChooserDialog>> oOnFinishedLoading) {
        mMaterialChooserControl.loadLibraries(oOnFinishedLoading
            .map(onFinishedLoading -> mcc -> onFinishedLoading.accept(MaterialChooserDialog.this)));
    }

    protected void validate() {
        DialogPane dialogPane = getDialogPane();
        Node okButton = dialogPane.lookupButton(ButtonType.OK);

        okButton.setDisable(getMaterialDescriptor().isEmpty());
    }

    public void selectMaterial(AssetRefPath materialRef) {
        mMaterialChooserControl.selectMaterial(materialRef);
    }

    public Optional<AssetRefPath> getMaterialDescriptor() {
        return mMaterialChooserControl.getSelectedMaterial();
    }
}
