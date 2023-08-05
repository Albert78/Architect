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

import de.dh.cad.architect.model.assets.AssetRefPath.IAssetPathAnchor;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.assets.AssetManager;
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

public class SupportObjectAnchorChooserDialog extends Dialog<IAssetPathAnchor> {
    protected static final int DIALOG_WIDTH = 400;
    protected static final int DIALOG_HEIGHT = 200;

    protected SupportObjectAnchorChooserControl mSupportObjectAnchorChooserControl;

    public SupportObjectAnchorChooserDialog(AssetManager assetManager, String dialogTitle) {
        mSupportObjectAnchorChooserControl = new SupportObjectAnchorChooserControl(assetManager);

        setTitle(dialogTitle);
        // TODO: Nice graphic
        DialogPane dialogPane = getDialogPane();
        ObservableList<ButtonType> buttonTypes = dialogPane.getButtonTypes();
        buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL);

        dialogPane.setContent(mSupportObjectAnchorChooserControl);
        mSupportObjectAnchorChooserControl.supportObjectAnchorProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends IAssetPathAnchor> observable, IAssetPathAnchor oldValue, IAssetPathAnchor newValue) {
                validate();
            }
        });
        setResizable(true);

        dialogPane.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        Scene scene = dialogPane.getScene();

        Stage stage = (Stage) scene.getWindow();
        stage.setHeight(DIALOG_HEIGHT);
        stage.setWidth(DIALOG_WIDTH);

        validate();

        setResultConverter(new Callback<ButtonType, IAssetPathAnchor>() {
            @Override
            public IAssetPathAnchor call(ButtonType dialogButton) {
                if (dialogButton == ButtonType.OK) {
                    return getSupportObjectAnchor();
                }
                return null;
            }
        });
        mSupportObjectAnchorChooserControl.requestFocus();
    }

    protected void validate() {
        DialogPane dialogPane = getDialogPane();
        Node okButton = dialogPane.lookupButton(ButtonType.OK);

        okButton.setDisable(getSupportObjectAnchor() == null);
    }

    public IAssetPathAnchor getSupportObjectAnchor() {
        return mSupportObjectAnchorChooserControl.getSupportObjectAnchor();
    }
}
