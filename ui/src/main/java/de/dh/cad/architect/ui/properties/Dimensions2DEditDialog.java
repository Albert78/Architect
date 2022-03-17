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
package de.dh.cad.architect.ui.properties;

import java.io.IOException;
import java.text.NumberFormat;

import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.controls.LengthControl;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class Dimensions2DEditDialog extends Dialog<Dimensions2D> {
    public static final String FXML = "Dimensions2DDialogContent.fxml";

    @FXML
    protected Label mWidthLabel;

    @FXML
    protected Label mHeightLabel;

    protected LengthControl mWidth;
    protected LengthControl mHeight;

    public Dimensions2DEditDialog(NumberFormat numberFormat) {
        setTitle("Ausmaße bearbeiten");
        // TODO: Nice graphic
        ObservableList<ButtonType> buttonTypes = getDialogPane().getButtonTypes();
        buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL);

        FXMLLoader fxmlLoader = new FXMLLoader(Dimensions3DEditDialog.class.getResource(FXML));
        fxmlLoader.setController(this);
        try {
            GridPane content = fxmlLoader.load();
            mWidth = new LengthControl(numberFormat);
            mHeight = new LengthControl(numberFormat);
            content.add(mWidth, 1, 0);
            content.add(mHeight, 1, 1);
            ChangeListener<? super Length> validationListener = (observable, oldValue, newValue) -> {
                validate();
            };
            mWidth.lengthProperty().addListener(validationListener);
            mHeight.lengthProperty().addListener(validationListener);
            validate();

            setResultConverter(new Callback<ButtonType, Dimensions2D>() {
                @Override
                public Dimensions2D call(ButtonType dialogButton) {
                    ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    return data == ButtonData.OK_DONE
                            ? new Dimensions2D(mWidth.getLength(), mHeight.getLength())
                            : null;
                }
            });
            mWidth.requestFocus();
            getDialogPane().setContent(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void validate() {
        DialogPane dialogPane = getDialogPane();
        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(mWidth.getLength() == null || mHeight.getLength() == null);
    }

    public void setDimensions(Dimensions2D value) {
        Length width = value.getX();
        mWidth.setLength(width);
        Length height = value.getY();
        mHeight.setLength(height);
    }

    public void setWidthLabel(String text) {
        mWidthLabel.setText(text);
    }

    public void setHeightLabel(String text) {
        mHeightLabel.setText(text);
    }
}
