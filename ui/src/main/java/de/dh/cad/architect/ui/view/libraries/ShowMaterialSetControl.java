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

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.utils.io.fx.MaterialData;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class ShowMaterialSetControl extends BorderPane implements Initializable {
    public static final String FXML = "ShowMaterialSetControl.fxml";

    protected final AssetLoader mAssetLoader;
    protected final MaterialSetDescriptor mAssetDescriptor;

    @FXML
    protected TextField mAssetCollectionNameTextField;

    @FXML
    protected TextField mIdTextField;

    @FXML
    protected TextField mAssetRefPathTextField;

    @FXML
    protected TextField mNameTextField;

    @FXML
    protected TextArea mDescriptionTextArea;

    @FXML
    protected ImageView mIconImageView;

    @FXML
    protected Pane mMaterialChoiceControlParent;

    protected MaterialPreviewChoiceControl mMaterialControl = null;

    public ShowMaterialSetControl(MaterialSetDescriptor descriptor, AssetLoader assetLoader) {
        mAssetLoader = assetLoader;
        mAssetDescriptor = descriptor;

        FXMLLoader fxmlLoader = new FXMLLoader(ShowMaterialSetControl.class.getResource(FXML));
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
        AssetRefPath assetRefPath = mAssetDescriptor.getSelfRef();
        mAssetCollectionNameTextField.setText(assetRefPath.getAnchor().toString());
        mIdTextField.setText(mAssetDescriptor.getId());
        mAssetRefPathTextField.setText(assetRefPath.toPathString());
        mNameTextField.setText(StringUtils.trimToEmpty(mAssetDescriptor.getName()));
        mDescriptionTextArea.setText(StringUtils.trimToEmpty(mAssetDescriptor.getDescription()));

        mMaterialControl = new MaterialPreviewChoiceControl(mAssetLoader);
        mMaterialControl.initialize(mAssetDescriptor);
        mMaterialChoiceControlParent.getChildren().add(mMaterialControl);

        updateIconImage();
    }

    public SimpleObjectProperty<MaterialDescriptor> selectedMaterialProperty() {
        return mMaterialControl.selectedMaterialProperty();
    }

    public MaterialData getSelectedMaterial() {
        return mMaterialControl.getSelectedMaterial();
    }

    public String getSelectedMaterialName() {
        return mMaterialControl.getSelectedMaterialName();
    }

    public MaterialDescriptor getSelectedMaterialDescriptor() {
        return mMaterialControl.getSelectedMaterialDescriptor();
    }

    public void selectMaterial(String materialName) {
        mMaterialControl.selectMaterial(materialName);
    }

    protected void updateIconImage() {
        Image image = mAssetLoader.loadMaterialSetIconImage(mAssetDescriptor, true);
        mIconImageView.setImage(image);
    }
}
