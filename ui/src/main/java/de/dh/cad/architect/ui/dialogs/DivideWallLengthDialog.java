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
package de.dh.cad.architect.ui.dialogs;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controls.DivideWallLengthControl;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

public class DivideWallLengthDialog extends Dialog<Length> {
    protected DivideWallLengthControl mDivideWallLenghControl;

    public DivideWallLengthDialog(Length wallLength) {
     // Create the custom dialog.
        setTitle(Strings.DIVIDE_WALL_LENGTH_DIALOG_TITLE);
        setHeaderText(Strings.DIVIDE_WALL_LENGTH_DIALOG_HEADER);

        // TODO: Icon
        //dialog.setGraphic(new ImageView(DivideWallLengthDialog.getResource("DivideWallLengthDialogIcon.png").toString()));

        DialogPane dialogPane = getDialogPane();

        mDivideWallLenghControl = new DivideWallLengthControl(wallLength);
        dialogPane.setContent(mDivideWallLenghControl);

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return mDivideWallLenghControl.getDividerDistance();
            }
            return null;
        });
    }

    public Length getBreakPointDistance() {
        return mDivideWallLenghControl.getDividerDistance();
    }
}
