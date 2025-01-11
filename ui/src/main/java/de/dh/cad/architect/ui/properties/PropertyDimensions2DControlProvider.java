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

import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.Strings;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;

public class PropertyDimensions2DControlProvider implements IPropertyControlProvider<Dimensions2D> {
    protected final NumberFormat mNumberFormat;
    protected final Label mLabel = new Label();

    protected final String mWidthLabel;
    protected final String mHeightLabel;

    public PropertyDimensions2DControlProvider(NumberFormat numberFormat, String widthLabel, String heightLabel) {
        mNumberFormat = numberFormat;
        mWidthLabel = widthLabel;
        mHeightLabel = heightLabel;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Node getEditor(TreeTableCell cell, String propertyDisplayName) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Dimensions2D value = (Dimensions2D) cell.getItem();
                Dimensions2DEditDialog dialog = new Dimensions2DEditDialog(String.format(Strings.PROPERTIES_DIMENSIONS_DIALOG_TITLE, propertyDisplayName), mNumberFormat);
                dialog.setWidthLabel(mWidthLabel);
                dialog.setHeightLabel(mHeightLabel);
                dialog.setDimensions(value);
                Optional<Dimensions2D> result = dialog.showAndWait();
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
        Dimensions2D val = (Dimensions2D) cell.getItem();
        String text = val == null ? null : toString(val);
        mLabel.setText(text);
        return mLabel;
    }

    protected String toString(Dimensions2D dim) {
        Length width = dim.getX();
        Length height = dim.getY();
        return MessageFormat.format("[{0}; {1}]",
            width.toHumanReadableString(width.getBestUnitForDisplay(), 2, true),
            height.toHumanReadableString(height.getBestUnitForDisplay(), 2, true));
    }
}
