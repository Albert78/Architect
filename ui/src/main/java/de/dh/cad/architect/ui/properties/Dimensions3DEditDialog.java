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

import de.dh.cad.architect.model.coords.Dimensions3D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.controls.LengthControl;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class Dimensions3DEditDialog extends Dialog<Dimensions3D> {
    public static final String FXML = "Dimensions3DDialogContent.fxml";

    protected LengthControl mWidth;
    protected LengthControl mHeight;
    protected LengthControl mDepth;

    public Dimensions3DEditDialog(String title, NumberFormat numberFormat) {
        setTitle(title);
        // TODO: Nice graphic
        ObservableList<ButtonType> buttonTypes = getDialogPane().getButtonTypes();
        buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL);

        FXMLLoader fxmlLoader = new FXMLLoader(Dimensions3DEditDialog.class.getResource(FXML));
        fxmlLoader.setController(this);
        try {
            GridPane content = fxmlLoader.load();
            mWidth = new LengthControl(numberFormat);
            mHeight = new LengthControl(numberFormat);
            mDepth = new LengthControl(numberFormat);
            content.add(mWidth, 1, 0);
            content.add(mHeight, 1, 1);
            content.add(mDepth, 1, 2);
            ChangeListener<? super Length> validationListener = (observable, oldValue, newValue) -> {
                validate();
            };
            mWidth.lengthProperty().addListener(validationListener);
            mHeight.lengthProperty().addListener(validationListener);
            mDepth.lengthProperty().addListener(validationListener);
            validate();

            setResultConverter(new Callback<ButtonType, Dimensions3D>() {
                @Override
                public Dimensions3D call(ButtonType dialogButton) {
                    ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    return data == ButtonData.OK_DONE
                            ? new Dimensions3D(mWidth.getLength(), mHeight.getLength(), mDepth.getLength())
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
        okButton.setDisable(mWidth.getLength() == null || mHeight.getLength() == null || mDepth.getLength() == null);
    }

    public void setDimensions(Dimensions3D value) {
        Length width = value.getWidth();
        mWidth.setLength(width);
        Length height = value.getHeight();
        mHeight.setLength(height);
        Length depth = value.getDepth();
        mDepth.setLength(depth);
    }
}
