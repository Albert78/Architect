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

import java.text.NumberFormat;
import java.text.ParseException;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.controls.LengthControl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.util.Callback;

public class LengthEditDialog extends Dialog<Length> {
    protected LengthControl mLengthControl;

    public LengthEditDialog(String title, NumberFormat numberFormat) {
        setTitle(title);
        // TODO: Nice graphic
        ObservableList<ButtonType> buttonTypes = getDialogPane().getButtonTypes();
        buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL);

        mLengthControl = new LengthControl(numberFormat);
        getDialogPane().setContent(mLengthControl);
        mLengthControl.lengthProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Length> observable, Length oldValue, Length newValue) {
                validate();
            }
        });
        validate();

        setResultConverter(new Callback<ButtonType, Length>() {
            @Override
            public Length call(ButtonType dialogButton) {
                ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                try {
                    return data == ButtonData.OK_DONE ? getLength() : null;
                } catch (ParseException e) {
                    return null;
                }
            }
        });
        mLengthControl.requestFocus();
    }

    protected void validate() {
        DialogPane dialogPane = getDialogPane();
        Node okButton = dialogPane.lookupButton(ButtonType.OK);

        okButton.setDisable(mLengthControl.getLength() == null);
    }

    public void setLength(Length value) {
        mLengthControl.setLength(value, null);
    }

    public Length getLength() throws ParseException {
        return mLengthControl.getLength();
    }
}
