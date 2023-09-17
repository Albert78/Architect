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
package de.dh.cad.architect.ui.view.libraries;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import de.dh.cad.architect.fx.nodes.objviewer.CoordinateSystemConfiguration;
import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewConfiguration;
import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewConfiguration.CameraType;
import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewControl;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.utils.ObjectStringAdapter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.IntegerStringConverter;

public class TakeSnapshotControl extends BorderPane implements Initializable {
    public static interface IImageAvailableHandler {
        void apply(Image img);
    }
    public static class ImageSizeSpinnerValueFactory extends SpinnerValueFactory<Integer> {
        public ImageSizeSpinnerValueFactory(int defaultSize) {
            setValue(defaultSize);
            setConverter(new IntegerStringConverter());
        }

        @Override
        public void increment(int steps) {
            int value = getValue();
            for (int i = 0; i < steps; i++) {
                value = inc(value);
            }
            setValue(value);
        }

        @Override
        public void decrement(int steps) {
            int value = getValue();
            for (int i = 0; i < steps; i++) {
                value = dec(value);
            }
            setValue(value);
        }

        protected static int inc(int value) {
            int log = (int) (Math.log(value) / Math.log(2));
            if (log > 16) {
                return 100000;
            }
            return 1 << (log + 1);
        }

        protected static int dec(int value) {
            int log = (int) (Math.log(value) / Math.log(2));
            int logVal = 1 << log;
            if (log < 5) {
                return 10;
            }
            if (value > logVal) {
                return logVal;
            }
            return 1 << (log - 1);
        }
    }

    public static final String FXML = "TakeSnapshotControl.fxml";

    protected static final String INTENSITY_TO_STRING_FORMAT = "%.0f";
    protected static int MIN_IMAGE_SIZE = 50;
    protected static int MAX_IMAGE_SIZE = 2048;
    protected final static int DEFAULT_ICON_IMAGE_WIDTH = 256;
    protected final static int DEFAULT_ICON_IMAGE_HEIGHT = 256;

    @FXML
    protected BorderPane mThreeDObjectViewParent;

    @FXML
    protected Button mDirectionXButton;

    @FXML
    protected Button mDirectionYButton;

    @FXML
    protected Button mDirectionZButton;

    @FXML
    protected ChoiceBox<ObjectStringAdapter<ThreeDObjectViewConfiguration.CameraType>> mCameraChoiceBox;

    @FXML
    protected CheckBox mPointLightCheckBox;

    @FXML
    protected Slider mPointLightIntensitySlider;

    @FXML
    protected Label mPointLightIntensityValueLabel;

    @FXML
    protected CheckBox mAmbientLightCheckBox;

    @FXML
    protected Slider mAmbientLightIntensitySlider;

    @FXML
    protected Label mAmbientLightIntensityValueLabel;

    @FXML
    protected Spinner<Integer> mImageWidthSpinner;

    @FXML
    protected Spinner<Integer> mImageHeightSpinner;

    @FXML
    protected Button mDefaultIconValuesButton;

    @FXML
    protected Button mDefaultPlanViewValuesButton;

    protected ThreeDObjectViewControl mThreeDObjectView;
    protected int mDefaultPlanViewImageWidth = 200;
    protected int mDefaultPlanViewImageHeight = 200;

    protected final ImageSizeSpinnerValueFactory mImageWidthValueFactory = new ImageSizeSpinnerValueFactory(DEFAULT_ICON_IMAGE_WIDTH);
    protected final ImageSizeSpinnerValueFactory mImageHeightValueFactory = new ImageSizeSpinnerValueFactory(DEFAULT_ICON_IMAGE_HEIGHT);

    public TakeSnapshotControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(TakeSnapshotControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mThreeDObjectView = new ThreeDObjectViewControl(CoordinateSystemConfiguration.architect());
        mThreeDObjectViewParent.setCenter(mThreeDObjectView);

        mDirectionXButton.setOnAction(event -> {
            mThreeDObjectView.setRotationAngleX(-90);
            mThreeDObjectView.setRotationAngleY(90); // Other side: -90
        });
        mDirectionYButton.setOnAction(event -> {
            mThreeDObjectView.setRotationAngleX(0);
            mThreeDObjectView.setRotationAngleY(0);
        });
        mDirectionZButton.setOnAction(event -> {
            mThreeDObjectView.setRotationAngleX(-90);
            mThreeDObjectView.setRotationAngleY(0);
        });
        mCameraChoiceBox.setItems(FXCollections.observableArrayList(
            new ObjectStringAdapter<>(ThreeDObjectViewConfiguration.CameraType.Perspective, Strings.PERSPECTIVE_RENDERING),
            new ObjectStringAdapter<>(ThreeDObjectViewConfiguration.CameraType.Parallel, Strings.PARALLEL_RENDERING)));
        mCameraChoiceBox.getSelectionModel().select(0);
        mCameraChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends ObjectStringAdapter<CameraType>> observable, ObjectStringAdapter<CameraType> oldValue,
                ObjectStringAdapter<CameraType> newValue) {
                mThreeDObjectView.setCameraType(newValue.getObj());
            }
        });
        mThreeDObjectView.cameraTypeProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends CameraType> observable, CameraType oldValue, CameraType newValue) {
                mCameraChoiceBox.getSelectionModel().select(ObjectStringAdapter.compareDummy(newValue));
            }
        });
        BooleanProperty pointLightCheckedProperty = mPointLightCheckBox.selectedProperty();
        pointLightCheckedProperty.bindBidirectional(mThreeDObjectView.pointLightOnProperty());
        mPointLightIntensitySlider.valueProperty().bindBidirectional(mThreeDObjectView.pointLightIntensityProperty());
        mPointLightIntensitySlider.disableProperty().bind(pointLightCheckedProperty.not());
        mPointLightIntensityValueLabel.textProperty().bind(mPointLightIntensitySlider.valueProperty().multiply(100).asString(INTENSITY_TO_STRING_FORMAT));
        BooleanProperty ambientLightCheckedProperty = mAmbientLightCheckBox.selectedProperty();
        ambientLightCheckedProperty.bindBidirectional(mThreeDObjectView.ambientLightOnProperty());
        mAmbientLightIntensitySlider.valueProperty().bindBidirectional(mThreeDObjectView.ambientLightIntensityProperty());
        mAmbientLightIntensitySlider.disableProperty().bind(ambientLightCheckedProperty.not());
        mAmbientLightIntensityValueLabel.textProperty().bind(mAmbientLightIntensitySlider.valueProperty().multiply(100).asString(INTENSITY_TO_STRING_FORMAT));
        mImageWidthSpinner.setValueFactory(mImageWidthValueFactory);
        mImageWidthSpinner.setPromptText("X");
        mImageHeightSpinner.setValueFactory(mImageHeightValueFactory);
        mImageHeightSpinner.setPromptText("Y");
        mDefaultIconValuesButton.setOnAction(event -> {
            setIconImageDefaults();
        });
        mDefaultPlanViewValuesButton.setOnAction(event -> {
            setPlanViewImageDefaults();
        });
        setIconImageDefaults();
    }

    public int getImageWidth() {
        return mImageWidthValueFactory.getValue();
    }

    public int getImageHeight() {
        return mImageHeightValueFactory.getValue();
    }

    protected void setIconImageDefaults() {
        mThreeDObjectView.applyConfiguration(ThreeDObjectViewConfiguration.standardPerspective());
        mImageWidthValueFactory.setValue(DEFAULT_ICON_IMAGE_WIDTH);
        mImageHeightValueFactory.setValue(DEFAULT_ICON_IMAGE_HEIGHT);
    }

    protected void setPlanViewImageDefaults() {
        mThreeDObjectView.applyConfiguration(ThreeDObjectViewConfiguration.standardPlanView());
        mImageWidthValueFactory.setValue(mDefaultPlanViewImageWidth);
        mImageHeightValueFactory.setValue(mDefaultPlanViewImageHeight);
    }

    public void setObject(Node value, int defaultPlanViewImageWidth, int defaultPlanViewImageHeight) {
        mThreeDObjectView.setObjView(value);
        mDefaultPlanViewImageWidth = defaultPlanViewImageWidth;
        mDefaultPlanViewImageHeight = defaultPlanViewImageHeight;
    }

    public Image createSnapshot() {
        return mThreeDObjectView.takeSnapshot(getImageWidth(), getImageHeight());
    }
}
