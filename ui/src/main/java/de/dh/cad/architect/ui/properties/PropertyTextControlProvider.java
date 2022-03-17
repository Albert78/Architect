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

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

/**
 * Property control provider providing a {@link TextField} for editing and a {@link Label} for view.
 * Can be used for all properties which can be converted to and from text, using a {@link StringConverter}.
 */
public class PropertyTextControlProvider<T> implements IPropertyControlProvider<T> {
    protected TextField mTextField = new TextField();
    protected Label mLabel = new Label();
    protected StringConverter<T> mStringConverter;

    public PropertyTextControlProvider(StringConverter<T> stringConverter) {
        mStringConverter = stringConverter;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Node getEditor(TreeTableCell cell) {
        mTextField.setOnAction(event -> {
            T value = mStringConverter.fromString(mTextField.getText());
            cell.commitEdit(value);
            event.consume();
        });
        mTextField.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
                t.consume();
            }
        });
        T item = (T) cell.getItem();
        String text = item == null ? "" : mStringConverter.toString(item);
        mTextField.setText(text);
        mTextField.selectAll();
        return mTextField;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Node getView(TreeTableCell cell) {
        T val = (T) cell.getItem();
        String text = val == null ? "" : mStringConverter.toString(val);
        mLabel.setText(text);
        return mLabel;
    }
}