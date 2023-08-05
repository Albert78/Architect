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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controls.LengthControl;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class AddWallInteractionsPaneControl extends VBox implements Initializable {
    public static final String FXML = "AddWallInteractionsPaneControl.fxml";

    @FXML
    protected TitledPane mStartWallsTitledPane;

    @FXML
    protected VBox mStartWallsVBox;

    @FXML
    protected CheckBox mDrawStartWallCwCheckBox;

    @FXML
    protected CheckBox mDrawStartWallCcwCheckBox;

    @FXML
    protected CheckBox mStartWallThicknessLikeMainWallCheckBox;

    @FXML
    protected VBox mEndWallsVBox;

    @FXML
    protected CheckBox mDrawEndWallCwCheckBox;

    @FXML
    protected CheckBox mDrawEndWallCcwCheckBox;

    @FXML
    protected CheckBox mEndWallThicknessLikeMainWallCheckBox;

    @FXML
    protected VBox mThicknessVBox;

    @FXML
    protected VBox mWallBevelStartVBox;

    @FXML
    protected VBox mWallBevelEndVBox;

    @FXML
    protected VBox mHeightStartVBox;

    @FXML
    protected VBox mHeightEndVBox;

    protected LengthControl mThicknessControl;
    protected ComboBox<WallBevelType> mWallBevelStartComboBox;
    protected ComboBox<WallBevelType> mWallBevelEndComboBox;
    protected LengthControl mHeightStartControl;
    protected LengthControl mHeightEndControl;
    protected LengthControl mStartWallsThicknessControl;
    protected ObjectProperty<Length> mStartWallsThicknessProperty = new SimpleObjectProperty<>();
    protected LengthControl mEndWallsThicknessControl;
    protected ObjectProperty<Length> mEndWallsThicknessProperty = new SimpleObjectProperty<>();

    protected final NumberFormat mNumberFormat;

    public AddWallInteractionsPaneControl(NumberFormat numberFormat) {
        mNumberFormat = numberFormat;
        FXMLLoader fxmlLoader = new FXMLLoader(AddWallInteractionsPaneControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mThicknessControl = new LengthControl(mNumberFormat);
        mThicknessVBox.getChildren().add(mThicknessControl);

        mHeightStartControl = new LengthControl(mNumberFormat);
        mHeightStartVBox.getChildren().add(mHeightStartControl);

        mHeightEndControl = new LengthControl(mNumberFormat);
        mHeightEndVBox.getChildren().add(mHeightEndControl);

        mWallBevelStartComboBox = new ComboBox<>(FXCollections.observableArrayList(WallBevelType.values()));
        mWallBevelStartComboBox.setConverter(Strings.WALL_BEVEL_TYPE_TITLE_PROVIDER);
        mWallBevelStartVBox.getChildren().add(mWallBevelStartComboBox);

        mStartWallThicknessLikeMainWallCheckBox.selectedProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                updateStartWallThicknessPropertyConnection();
            }
        });
        updateStartWallThicknessPropertyConnection();
        mStartWallsThicknessControl = new LengthControl(mNumberFormat);
        mStartWallsThicknessControl.disableProperty().bind(mStartWallThicknessLikeMainWallCheckBox.selectedProperty());
        mStartWallsVBox.getChildren().add(mStartWallsThicknessControl);

        mWallBevelEndComboBox = new ComboBox<>(FXCollections.observableArrayList(WallBevelType.values()));
        mWallBevelEndComboBox.setConverter(Strings.WALL_BEVEL_TYPE_TITLE_PROVIDER);
        mWallBevelEndVBox.getChildren().add(mWallBevelEndComboBox);

        mEndWallThicknessLikeMainWallCheckBox.selectedProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                updateEndWallThicknessPropertyConnection();
            }
        });
        updateEndWallThicknessPropertyConnection();
        mEndWallsThicknessControl = new LengthControl(mNumberFormat);
        mEndWallsThicknessControl.disableProperty().bind(mEndWallThicknessLikeMainWallCheckBox.selectedProperty());
        mEndWallsVBox.getChildren().add(mEndWallsThicknessControl);
    }

    protected void updateStartWallThicknessPropertyConnection() {
        if (mStartWallThicknessLikeMainWallCheckBox.isSelected()) {
            mStartWallsThicknessProperty.bind(getThicknessProperty());
        } else {
            mStartWallsThicknessProperty.bind(mStartWallsThicknessControl.lengthProperty());
        }
    }

    protected void updateEndWallThicknessPropertyConnection() {
        if (mEndWallThicknessLikeMainWallCheckBox.isSelected()) {
            mEndWallsThicknessProperty.bind(getThicknessProperty());
        } else {
            mEndWallsThicknessProperty.bind(mEndWallsThicknessControl.lengthProperty());
        }
    }

    public void initializeValues(Length thickness,
        WallBevelType wallBevelStart, WallBevelType wallBevelEnd,
        Length heightStart, Length heightEnd,
        boolean drawStartWallCw, boolean drawStartWallCcw,
        boolean drawEndWallCw, boolean drawEndWallCcw) {
        mThicknessControl.setLength(thickness);
        mWallBevelStartComboBox.getSelectionModel().select(wallBevelStart);
        mWallBevelEndComboBox.getSelectionModel().select(wallBevelEnd);
        mHeightStartControl.setLength(heightStart);
        mHeightEndControl.setLength(heightEnd);
        mDrawStartWallCwCheckBox.setSelected(drawStartWallCw);
        mDrawStartWallCcwCheckBox.setSelected(drawStartWallCcw);
        mStartWallsThicknessControl.setLength(thickness);
        mDrawEndWallCwCheckBox.setSelected(drawEndWallCw);
        mDrawEndWallCcwCheckBox.setSelected(drawEndWallCcw);
        mEndWallsThicknessControl.setLength(thickness);
    }

    public void disableStartOrthogonalWallSettings() {
        mStartWallsTitledPane.setDisable(true);
        mDrawStartWallCwCheckBox.setSelected(false);
        mDrawStartWallCcwCheckBox.setSelected(false);
    }

    public BooleanProperty getDrawStartWallCwProperty() {
        return mDrawStartWallCwCheckBox.selectedProperty();
    }

    public BooleanProperty getDrawStartWallCcwProperty() {
        return mDrawStartWallCcwCheckBox.selectedProperty();
    }

    public ReadOnlyObjectProperty<Length> getStartWallsThicknessProperty() {
        return mStartWallsThicknessProperty;
    }

    public BooleanProperty getDrawEndWallCwProperty() {
        return mDrawEndWallCwCheckBox.selectedProperty();
    }

    public BooleanProperty getDrawEndWallCcwProperty() {
        return mDrawEndWallCcwCheckBox.selectedProperty();
    }

    public ReadOnlyObjectProperty<Length> getEndWallsThicknessProperty() {
        return mEndWallsThicknessProperty;
    }

    public ReadOnlyObjectProperty<Length> getThicknessProperty() {
        return mThicknessControl.lengthProperty();
    }

    public ReadOnlyObjectProperty<WallBevelType> getWallBevelStartProperty() {
        return mWallBevelStartComboBox.getSelectionModel().selectedItemProperty();
    }

    public ReadOnlyObjectProperty<WallBevelType> getWallBevelEndProperty() {
        return mWallBevelEndComboBox.getSelectionModel().selectedItemProperty();
    }

    public ReadOnlyObjectProperty<Length> getHeightStartProperty() {
        return mHeightStartControl.lengthProperty();
    }

    public ReadOnlyObjectProperty<Length> getHeightEndProperty() {
        return mHeightEndControl.lengthProperty();
    }
}
