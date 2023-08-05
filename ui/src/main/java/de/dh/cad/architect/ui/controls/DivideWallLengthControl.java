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

import de.dh.cad.architect.model.coords.Length;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;

public class DivideWallLengthControl extends BorderPane {
    public static final String FXML = "DivideWallLengthControlContent.fxml";

    @FXML
    protected Label mWallLengthLabel;

    @FXML
    protected Label mWallEndADistanceLabel;

    @FXML
    protected Label mWallEndBDistanceLabel;

    @FXML
    protected Slider mWallDividerSlider;

    protected Length mWallLength;

    // For transport of distance value to the outside
    protected ObjectProperty<Length> mWallDividerDistanceProperty = new SimpleObjectProperty<>();

    public DivideWallLengthControl(Length wallLength) {
        FXMLLoader fxmlLoader = new FXMLLoader(DivideWallLengthControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initialize();
        setWallLength(wallLength);
        updateWallDividerDistance(wallLength.times(0.5));
    }

    public void setWallLength(Length value) {
        mWallLength = value;
        mWallLengthLabel.setText(toString(value));
    }

    public ReadOnlyObjectProperty<Length> dividerDistanceProperty() {
        return mWallDividerDistanceProperty;
    }

    public Length getDividerDistance() {
        return mWallDividerDistanceProperty.get();
    }

    protected void initialize() {
        mWallDividerSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                updateWallDividerDistance();
            }
        });
    }

    protected String toString(Length value) {
        return createLengthString(value);
    }

    protected void updateWallDividerDistance(Length wallEndADistance) {
        mWallDividerDistanceProperty.setValue(wallEndADistance);
        Length wallEndBDistance = mWallLength.minus(wallEndADistance);
        mWallEndADistanceLabel.setText(createLengthString(wallEndADistance));
        mWallEndBDistanceLabel.setText(createLengthString(wallEndBDistance));
    }

    protected String createLengthString(Length value) {
        return value.toHumanReadableString(value.getBestUnitForDisplay(), 2, true);
    }

    protected void updateWallDividerDistance() {
        double min = mWallDividerSlider.getMin();
        double range = mWallDividerSlider.getMax() - min;
        double value = mWallDividerSlider.getValue() - min;
        updateWallDividerDistance(mWallLength.times(value / range));
    }
}
