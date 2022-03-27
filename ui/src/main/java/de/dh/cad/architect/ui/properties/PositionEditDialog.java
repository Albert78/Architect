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
import java.util.Optional;

import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
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

/**
 * Edit dialog for a 2D or 3D {@link IPosition position}.
 */
public class PositionEditDialog extends Dialog<IPosition> {
    public static final String FXML_3D = "Position3DDialogContent.fxml";
    public static final String FXML_2D = "Position2DDialogContent.fxml";

    protected final LengthControl mX;
    protected final LengthControl mY;
    protected final Optional<LengthControl> mZ;

    public PositionEditDialog(IPosition position, String title, NumberFormat numberFormat) {
        setTitle(title);
        // TODO: Nice graphic
        ObservableList<ButtonType> buttonTypes = getDialogPane().getButtonTypes();
        buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL);

        boolean is3D = position instanceof Position3D;

        FXMLLoader fxmlLoader = new FXMLLoader(PositionEditDialog.class.getResource(is3D ? FXML_3D : FXML_2D));
        fxmlLoader.setController(this);
        try {
            GridPane content = fxmlLoader.load();
            mX = new LengthControl(numberFormat);
            mY = new LengthControl(numberFormat);

            content.add(mX, 1, 0);
            content.add(mY, 1, 1);
            ChangeListener<? super Length> validationListener = (observable, oldValue, newValue) -> {
                validate();
            };

            Length x = position.getX();
            mX.setLength(x);
            Length y = position.getY();
            mY.setLength(y);

            if (is3D) {
                Position3D pos3D = (Position3D) position;
                LengthControl zControl = new LengthControl(numberFormat);
                content.add(zControl, 1, 2);

                Length z = pos3D.getZ();
                zControl.setLength(z);

                mZ = Optional.of(zControl);

                zControl.lengthProperty().addListener(validationListener);
            } else {
                mZ = Optional.empty();
            }
            mX.lengthProperty().addListener(validationListener);
            mY.lengthProperty().addListener(validationListener);

            validate();

            setResultConverter(new Callback<ButtonType, IPosition>() {
                @Override
                public IPosition call(ButtonType dialogButton) {
                    ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    if (data == ButtonData.OK_DONE) {
                        return mZ.map(zControl -> (IPosition) new Position3D(mX.getLength(), mY.getLength(), zControl.getLength()))
                                        .orElse(new Position2D(mX.getLength(), mY.getLength()));
                    } else {
                        return null;
                    }
                }
            });
            mX.requestFocus();
            getDialogPane().setContent(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void validate() {
        DialogPane dialogPane = getDialogPane();
        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(mX.getLength() == null || mY.getLength() == null || (mZ.map(zControl -> zControl.getLength() == null).orElse(false)));
    }
}
