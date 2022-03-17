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

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

/**
 * Property control provider providing a {@link ComboBox} for editing and a {@link Label} for view.
 * Can be used for all enums.
 */
public class PropertyEnumControlProvider<T> implements IPropertyControlProvider<T> {
    protected ComboBox<T> mComboBox = new ComboBox<>();
    protected Label mLabel = new Label();

    protected final T[] mPossibleValues;

    public PropertyEnumControlProvider(T[] possibleValues, StringConverter<T> converter) {
        mPossibleValues = possibleValues;
        mComboBox.setConverter(converter);
    }

    public PropertyEnumControlProvider(T[] possibleValues) {
        mPossibleValues = possibleValues;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Node getEditor(TreeTableCell cell) {
        mComboBox.setItems(FXCollections.observableArrayList(mPossibleValues));
        mComboBox.setOnAction(event -> {
            T value = mComboBox.getValue();
            cell.commitEdit(value);
            event.consume();
        });
        mComboBox.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cell.cancelEdit();
                t.consume();
            }
        });
        T value = (T) cell.getItem();
        mComboBox.setValue(value);
        return mComboBox;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Node getView(TreeTableCell cell) {
        T val = (T) cell.getItem();
        StringConverter<T> converter = mComboBox.getConverter();
        if (val == null) {
            mLabel.setText("");
        } else {
            mLabel.setText(converter == null ? String.valueOf(val) : converter.toString(val));
        }
        return mLabel;
    }
}