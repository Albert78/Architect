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
package de.dh.cad.architect.fx.controls;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

public class DirectoryPathChooserControl extends GridPane implements Initializable {
    protected static final String FXML = "DirectoryPathChooserControl.fxml";

    @FXML
    protected TextField mDirectoryPathTextField;

    @FXML
    protected Button mPathExplorerButton;

    protected ObjectProperty<Path> mPathProperty = new SimpleObjectProperty<>();
    protected String mOpenDialogTitle = "Open Directory";

    public DirectoryPathChooserControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(DirectoryPathChooserControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DirectoryPathChooserControl create() {
        return new DirectoryPathChooserControl();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mPathExplorerButton.setOnAction(new EventHandler<>() {
            @Override
            public void handle(ActionEvent event) {
                showPathChooserDialog();
            }
        });
        mDirectoryPathTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            update();
        });
    }

    protected void showPathChooserDialog() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(mOpenDialogTitle);
        File selectedDirectory = directoryChooser.showDialog(getScene().getWindow());
        if (selectedDirectory == null) {
            return;
        }
        mDirectoryPathTextField.setText(selectedDirectory.getAbsolutePath());
    }

    public ReadOnlyObjectProperty<Path> pathProperty() {
        return mPathProperty;
    }

    public Path getPath() {
        return mPathProperty.get();
    }

    public void setPath(Path value) {
        mPathProperty.set(value);
    }

    protected void update() {
        Path path;
        try {
            path = Path.of(mDirectoryPathTextField.getText());
        } catch (InvalidPathException e) {
            path = null;
        }
        mPathProperty.set(path);
    }

    public void setOpenDialogTitle(String openDialogTitle) {
        mOpenDialogTitle = openDialogTitle;
    }
}