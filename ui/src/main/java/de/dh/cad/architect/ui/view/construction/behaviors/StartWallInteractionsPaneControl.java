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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controls.LengthControl;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

public class StartWallInteractionsPaneControl extends VBox implements Initializable {
    public static final String FXML = "StartWallInteractionsPaneControl.fxml";

    @FXML
    protected VBox mThicknessVBox;

    @FXML
    protected VBox mWallBevelStartVBox;

    protected LengthControl mThicknessControl;
    protected ComboBox<WallBevelType> mWallBevelStartComboBox;

    protected final NumberFormat mNumberFormat;

    public StartWallInteractionsPaneControl(NumberFormat numberFormat) {
        mNumberFormat = numberFormat;
        FXMLLoader fxmlLoader = new FXMLLoader(StartWallInteractionsPaneControl.class.getResource(FXML));
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

        mWallBevelStartComboBox = new ComboBox<>(FXCollections.observableArrayList(WallBevelType.values()));
        mWallBevelStartComboBox.setConverter(Strings.WALL_BEVEL_TYPE_TITLE_PROVIDER);
        mWallBevelStartVBox.getChildren().add(mWallBevelStartComboBox);
    }

    public void initializeValues(Length thickness, WallBevelType wallBevelStart) {
        mThicknessControl.setLength(thickness);
        mWallBevelStartComboBox.getSelectionModel().select(wallBevelStart);
    }

    public ReadOnlyObjectProperty<Length> getThicknessProperty() {
        return mThicknessControl.lengthProperty();
    }

    public ReadOnlyObjectProperty<WallBevelType> getWallBevelStartProperty() {
        return mWallBevelStartComboBox.getSelectionModel().selectedItemProperty();
    }
}
