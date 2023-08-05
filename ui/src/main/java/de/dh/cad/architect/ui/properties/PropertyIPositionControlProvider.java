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
package de.dh.cad.architect.ui.properties;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Optional;

import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.ui.Strings;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;

public class PropertyIPositionControlProvider implements IPropertyControlProvider<IPosition> {
    protected final NumberFormat mNumberFormat;
    protected final Label mLabel = new Label();

    public PropertyIPositionControlProvider(NumberFormat numberFormat) {
        mNumberFormat = numberFormat;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Node getEditor(TreeTableCell cell) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                IPosition value = (IPosition) cell.getItem();
                PositionEditDialog dialog = new PositionEditDialog(value, Strings.PROPERTIES_POSITION_DIALOG_TITLE, mNumberFormat);
                Optional<IPosition> result = dialog.showAndWait();
                if (result.isPresent()) {
                    cell.commitEdit(result.get());
                } else {
                    cell.cancelEdit();
                }
            }
        });
        return getView(cell);
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public Node getView(TreeTableCell cell) {
        IPosition position = (IPosition) cell.getItem();
        String text = position == null ? "" : toString(position);
        mLabel.setText(text);
        return mLabel;
    }

    protected String toString(IPosition pos) {
        Length x = pos.getX();
        Length y = pos.getY();
        if (pos instanceof Position3D p3d) {
            Length z = p3d.getZ();
            return MessageFormat.format("[{0}; {1}; {2}]",
                x.toHumanReadableString(x.getBestUnitForDisplay(), 2, true),
                y.toHumanReadableString(y.getBestUnitForDisplay(), 2, true),
                z.toHumanReadableString(z.getBestUnitForDisplay(), 2, true));
        } else {
            return MessageFormat.format("[{0}; {1}]",
                    x.toHumanReadableString(x.getBestUnitForDisplay(), 2, true),
                    y.toHumanReadableString(y.getBestUnitForDisplay(), 2, true));
        }
    }
}