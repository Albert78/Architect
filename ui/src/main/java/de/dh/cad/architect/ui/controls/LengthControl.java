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
package de.dh.cad.architect.ui.controls;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class LengthControl extends BorderPane implements Initializable {
    public static final String FXML = "LengthControlContent.fxml";

    protected final NumberFormat mNumberFormat;

    @FXML
    protected TextField mLengthTextField;

    @FXML
    protected ComboBox<LengthUnit> mLengthUnitCombo;

    // For transport of value to the outside
    protected ObjectProperty<Length> mLengthProperty = new SimpleObjectProperty<>();

    public LengthControl(NumberFormat numberFormat) {
        mNumberFormat = numberFormat;

        FXMLLoader fxmlLoader = new FXMLLoader(LengthControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Length parseValue() throws IllegalStateException {
        String text = mLengthTextField.getText();
        LengthUnit unit = mLengthUnitCombo.getValue();
        if (StringUtils.isEmpty(text) || unit == null) {
            throw new IllegalStateException("Invalid Length value");
        }
        ParsePosition pp = new ParsePosition(0);
        Number number = mNumberFormat.parse(text, pp);
        if (pp.getIndex() < text.length()) {
            throw new IllegalStateException("Text cannot be parsed to a double number");
        }
        return Length.of(number.doubleValue(), unit);
    }

    public ReadOnlyObjectProperty<Length> lengthProperty() {
        return mLengthProperty;
    }

    public Length getLength() {
        return mLengthProperty.get();
    }

    public void setLength(Length value) {
        setLength(value, null);
    }

    public void setLength(Length value, LengthUnit lengthUnit) {
        LengthUnit unit = lengthUnit == null ? (value == null ? LengthUnit.M : value.getBestUnitForEdit()) : lengthUnit;
        mLengthUnitCombo.setValue(unit);
        setTextField(value, unit);
        update();

        mLengthTextField.selectAll();
        mLengthTextField.requestFocus();
    }

    protected void setTextField(Length value, LengthUnit unit) {
        mLengthTextField.setText(value == null ? "0.0" : MessageFormat.format("{0,number,#.##}", value.inUnit(unit)));
    }

    protected void update() {
        try {
            Length length = parseValue();
            mLengthProperty.set(length);
        } catch (IllegalStateException e) {
            // Ignore, maybe user is editing
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mLengthUnitCombo.getItems().setAll(LengthUnit.values());
        mLengthTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            update();
        });
        mLengthUnitCombo.setOnAction(event -> {
            Length length = getLength();
            if (length != null) {
                LengthUnit unit = mLengthUnitCombo.getValue();
                if (unit == null) {
                    unit = LengthUnit.M;
                }
                setTextField(length, unit);
            }
            update();
        });
        setLength(Length.ZERO, null);
    }
}
