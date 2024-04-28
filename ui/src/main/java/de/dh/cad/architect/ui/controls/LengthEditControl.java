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

import de.dh.cad.architect.model.coords.Length;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.text.NumberFormat;

public class LengthEditControl extends HBox {
    public static final String FXML = "LengthEditControl.fxml";

    @FXML
    protected Slider mSlider;

    protected NumberFormat mNumberFormat;
    protected LengthControl mLengthControl;
    protected ObjectProperty<Length> mValueProperty = new SimpleObjectProperty<>(Length.ZERO);
    protected ObjectProperty<Length> mMinValueProperty = new SimpleObjectProperty<>(Length.ZERO);
    protected ObjectProperty<Length> mMaxValueProperty = new SimpleObjectProperty<>(Length.ofM(10));
    protected boolean mBlockUpdates = false;

    public LengthEditControl(NumberFormat numberFormat) {
        mNumberFormat = numberFormat;
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
        mLengthControl = new LengthControl(mNumberFormat);
        mLengthControl.setPrefWidth(150);
        mLengthControl.lengthProperty().addListener((observable, oldValue, newValue) -> {
            if (mBlockUpdates) {
                return;
            }
            mBlockUpdates = true;
            try {
                Length val = mLengthControl.getLength();
                Length value = Length.max(Length.min(val, getMaxValue()), getMinValue());
                if (val.ne(value)) {
                    mLengthControl.setLength(value);
                }
                setSliderValue(value);
                mValueProperty.set(value);
            } finally {
                mBlockUpdates = false;
            }
        });
        getChildren().addFirst(mLengthControl);
        mSlider.setPrefWidth(100);
        updateSliderMinMax();
        mSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mBlockUpdates) {
                return;
            }
            mBlockUpdates = true;
            try {
                Length val = getSliderValue();
                Length value = Length.max(Length.min(val, getMaxValue()), getMinValue());
                if (val.ne(value)) {
                    setSliderValue(value);
                }
                mLengthControl.setLength(value);
                mValueProperty.set(value);
            } finally {
                mBlockUpdates = false;
            }
        });
        mValueProperty.addListener((obsVal, oldVal, newVal) -> {
            if (mBlockUpdates) {
                return;
            }
            mBlockUpdates = true;
            try {
                Length val = getValue();
                Length value = Length.max(Length.min(val, getMaxValue()), getMinValue());
                if (val.ne(value)) {
                    setValue(value);
                }
                setSliderValue(value);
                mLengthControl.setLength(value);
            } finally {
                mBlockUpdates = false;
            }
        });
        ChangeListener<Length> updateSliderChangeListener = (obsVal, oldVal, newVal) -> {
            updateSliderMinMax();
        };
        mMinValueProperty.addListener(updateSliderChangeListener);
        mMaxValueProperty.addListener(updateSliderChangeListener);
        mValueProperty.setValue(Length.ZERO);
    }

    protected void updateSliderMinMax() {
        mSlider.setMin(getMinValue().inInternalFormat());
        mSlider.setMax(getMaxValue().inInternalFormat());    }

    protected Length getSliderValue() {
        return Length.ofInternalFormat(mSlider.getValue());
    }

    protected void setSliderValue(Length value) {
        mSlider.setValue(value.inInternalFormat());
    }

    public Length getValue() {
        return mValueProperty.get();
    }

    public void setValue(Length value) {
        mValueProperty.setValue(value);
    }

    public ObjectProperty<Length> getValueProperty() {
        return mValueProperty;
    }

    public Length getMinValue() {
        return mMinValueProperty.get();
    }

    public void setMinValue(Length value) {
        mMinValueProperty.setValue(value);
    }

    public ObjectProperty<Length> getMinValueProperty() {
        return mMinValueProperty;
    }

    public Length getMaxValue() {
        return mMaxValueProperty.get();
    }

    public void setMaxValue(Length value) {
        mMaxValueProperty.setValue(value);
    }

    public ObjectProperty<Length> getMaxValueProperty() {
        return mMaxValueProperty;
    }
}
