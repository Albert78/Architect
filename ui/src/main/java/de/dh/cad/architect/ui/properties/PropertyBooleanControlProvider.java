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

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

/**
 * Property control provider for {@link Boolean} values providing a {@link ComboBox} for editing and a {@link Label} for view.
 * Can be used for all properties which can be converted to and from Boolean, using a {@link StringConverter}.
 */
public class PropertyBooleanControlProvider implements IPropertyControlProvider<Boolean> {
    protected ComboBox<Boolean> mComboBox = new ComboBox<>();
    protected Label mLabel = new Label();
    protected StringConverter<Boolean> mStringConverter;

    public PropertyBooleanControlProvider(StringConverter<Boolean> stringConverter) {
        mStringConverter = stringConverter;
        mComboBox.setConverter(mStringConverter);
        mComboBox.getItems().setAll(Boolean.TRUE, Boolean.FALSE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Node getEditor(TreeTableCell cell) {
        mComboBox.setOnAction(event -> {
            Boolean value = mComboBox.getValue();
            cell.commitEdit(value);
            event.consume();
        });
        mComboBox.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
                t.consume();
            }
        });
        Boolean value = (Boolean) cell.getItem();
        mComboBox.setValue(value);
        return mComboBox;
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public Node getView(TreeTableCell cell) {
        Boolean val = (Boolean) cell.getItem();
        String text = val == null ? "" : mStringConverter.toString(val);
        mLabel.setText(text);
        return mLabel;
    }
}