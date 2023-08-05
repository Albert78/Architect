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

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.assets.AssetLibrary;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class AssetLibraryEditControl extends AbstractEditControl implements Initializable {
    public static final String FXML = "AssetLibraryEditControl.fxml";

    @FXML
    protected Label mPathLabel;

    @FXML
    protected Label mIdLabel;

    @FXML
    protected TextField mNameTextField;

    @FXML
    protected TextArea mDescriptionTextArea;

    public AssetLibraryEditControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(AssetLibraryEditControl.class.getResource(FXML));
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
        // Nothing to do ATM
    }

    public void initializeValues(AssetLibrary library, IDirectoryLocator libraryDirectory) {
        mPathLabel.setText(libraryDirectory.toString());
        mIdLabel.setText(library.getId());
        mNameTextField.setText(StringUtils.trimToEmpty(library.getName()));
        mDescriptionTextArea.setText(StringUtils.trimToEmpty(library.getDescription()));
    }

    public void updateValues(AssetLibrary library) {
        library.setName(mNameTextField.getText());
        library.setDescription(mDescriptionTextArea.getText());
    }
}
