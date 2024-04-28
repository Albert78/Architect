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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class RotationEditControl extends HBox {
    public static final String FXML = "RotationEditControl.fxml";

    @FXML
    protected Spinner<Integer> mAngleSpinner;

    @FXML
    protected Slider mAngleSlider;

    protected DoubleProperty mAngleProperty = new SimpleDoubleProperty(0);
    protected boolean mBlockUpdates = false;

    public RotationEditControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(DivideWallLengthControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initialize();
    }

    protected void initialize() {
        mAngleSpinner.setPrefWidth(100);
        SpinnerValueFactory.IntegerSpinnerValueFactory spinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 360);
        mAngleSpinner.setValueFactory(spinnerValueFactory);
        mAngleSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mBlockUpdates) {
                return;
            }
            mBlockUpdates = true;
            try {
                int angle = spinnerValueFactory.getValue();
                mAngleSlider.setValue(angle);
                mAngleProperty.setValue(angle);
            } finally {
                mBlockUpdates = false;
            }
        });
        mAngleSlider.setPrefWidth(100);
        mAngleSlider.setMin(0);
        mAngleSlider.setMax(360);
        mAngleSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mBlockUpdates) {
                return;
            }
            mBlockUpdates = true;
            try {
                int angle = (int) mAngleSlider.getValue();
                spinnerValueFactory.setValue(angle);
                mAngleProperty.setValue(angle);
            } finally {
                mBlockUpdates = false;
            }
        });
        mAngleProperty.addListener((observable, oldValue, newValue) -> {
            if (mBlockUpdates) {
                return;
            }
            mBlockUpdates = true;
            try {
                int angle = (int) getAngle();
                spinnerValueFactory.setValue(angle);
                mAngleSlider.setValue(angle);
            } finally {
                mBlockUpdates = false;
            }
        });
    }

    public double getAngle() {
        return mAngleProperty.getValue();
    }

    public void setAngle(double value) {
        mAngleProperty.setValue(value);
    }

    public DoubleProperty getAngleProperty() {
        return mAngleProperty;
    }
}
