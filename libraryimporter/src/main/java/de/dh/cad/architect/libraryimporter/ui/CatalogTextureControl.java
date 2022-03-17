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
package de.dh.cad.architect.libraryimporter.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.libraryimporter.sh3d.textures.CatalogTexture;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

public class CatalogTextureControl extends BorderPane implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(CatalogTextureControl.class);

    public static final String FXML = "CatalogTextureControl.fxml";
    public static final String BROKEN_IMAGE_IMAGE = "broken-image.png";

    protected final AssetManager mAssetManager;
    protected final CatalogTexture mTextureData;

    @FXML
    protected Label mIdLabel;

    @FXML
    protected TextField mNameTextField;

    @FXML
    protected TextField mCategoryTextField;

    @FXML
    protected ImageView mTextureImageView;

    @FXML
    protected Label mWidthLabel;

    @FXML
    protected Label mHeightLabel;

    public CatalogTextureControl(CatalogTexture textureData, AssetManager assetManager) {
        mAssetManager = assetManager;
        mTextureData = textureData;

        FXMLLoader fxmlLoader = new FXMLLoader(CatalogTextureControl.class.getResource(FXML));
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
        mIdLabel.setText(mTextureData.getId());
        mNameTextField.setText(StringUtils.trimToEmpty(mTextureData.getName()));
        mCategoryTextField.setText(StringUtils.trimToEmpty(mTextureData.getCategory()));
        updateDiffuseMapImage();
        mWidthLabel.setText("Höhe: " + lengthOrDefault(mTextureData.getWidth()));
        mHeightLabel.setText("Breite: " + lengthOrDefault(mTextureData.getHeight()));
    }

    protected String lengthOrDefault(float value) {
        if (value == 0) {
            return "-";
        }
        return Float.toString(value) + " cm";
    }

    protected Image loadImage(IResourceLocator resource) {
        Image image = null;
        if (resource != null) {
            try (InputStream is = resource.inputStream()) {
                image = new Image(is);
            } catch (IOException e) {
                log.error("Image '" + resource.getFileName() + "' could not be loaded", e);
            }
        }
        if (image == null) {
            image = AssetLoader.loadBrokenImageSmall();
        }
        return image;
    }

    protected void updateDiffuseMapImage() {
        Image image = loadImage(mTextureData.getImage());
        mTextureImageView.setImage(image);
    }
}
