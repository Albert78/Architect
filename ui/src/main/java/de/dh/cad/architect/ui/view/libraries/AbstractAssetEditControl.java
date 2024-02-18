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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManagerConfiguration;
import de.dh.cad.architect.utils.vfs.PlainFileSystemResourceLocator;
import de.dh.utils.fx.FxUtils;
import de.dh.utils.fx.dialogs.DialogUtils;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public abstract class AbstractAssetEditControl extends AbstractEditControl {
    private final static Logger log = LoggerFactory.getLogger(AbstractAssetEditControl.class);

    protected final AssetManager mAssetManager;
    protected final AssetLoader mAssetLoader;

    protected AbstractAssetDescriptor mDescriptor = null;

    public AbstractAssetEditControl(AssetManager assetManager) {
        mAssetManager = assetManager;
        mAssetLoader = assetManager.buildAssetLoader();
    }

    protected void initialize(AbstractAssetDescriptor descriptor) {
        mDescriptor = descriptor;
    }

    public AbstractAssetDescriptor getDescriptor() {
        return mDescriptor;
    }

    public Window getStage() {
        return getScene().getWindow();
    }

    protected abstract void updateIconImage();

    protected void showErrorImportDialog(String text, Throwable exception) {
        DialogUtils.showExceptionDialog(Strings.LIBRARY_MANAGER_ERROR_IMPORTING_RESOURCE_TITLE,
            Strings.LIBRARY_MANAGER_ERROR_IMPORTING_RESOURCE_HEADER, text, exception);
    }

    protected void importIconImage(Path path) {
        try {
            mAssetLoader.importAssetIconImage(mDescriptor, new PlainFileSystemResourceLocator(path),
                Optional.of(AssetManager.ICON_IMAGE_DEFAULT_BASE_NAME + "." + AssetManager.STORE_IMAGE_EXTENSION));
            updateIconImage();
        } catch (IOException e) {
            showErrorImportDialog(Strings.LIBRARY_MANAGER_ERROR_IMPORTING_ICON_IMAGE_TEXT, e);
        }
    }

    protected void importIconImage(Image image, String imageName) {
        try {
            mAssetLoader.importAssetIconImage(mDescriptor, image, imageName);
            updateIconImage();
        } catch (IOException e) {
            showErrorImportDialog(Strings.LIBRARY_MANAGER_ERROR_IMPORTING_ICON_IMAGE_TEXT, e);
        }
    }

    protected Image loadImage(Path imageFile) {
        try {
            return AssetManager.loadImage(new PlainFileSystemResourceLocator(imageFile), Optional.empty());
        } catch (IOException e) {
            return AssetLoader.loadBrokenImageBig(Optional.empty());
        }
    }

    protected Image loadIconImage() {
        Image image = null;
        try {
            image = mAssetLoader.loadAssetIconImage(mDescriptor);
        } catch (IOException e) {
            log.error("Icon image of asset <" + mDescriptor + "> could not be loaded", e);
        }
        if (image == null) {
            image = AssetLoader.loadBrokenImageSmall(Optional.empty());
        }
        return image;
    }

    protected Path openImageDialog(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        AssetManagerConfiguration configuration = mAssetManager.getConfiguration();
        Optional<Path> oLastImagePath = configuration.getLastImagePath();
        oLastImagePath.ifPresent(path -> FxUtils.trySetInitialDirectory(fc, path));
        fc.getExtensionFilters().addAll(getImageExtensionFilters());
        File imageFile = fc.showOpenDialog(getStage());
        if (imageFile == null) {
            return null;
        }
        Path imagePath = imageFile.toPath();
        configuration.setLastImagePath(imagePath);
        return imagePath;
    }

    protected Path openIconImageDialog() {
        return openImageDialog(Strings.SELECT_ICON_IMAGE_DIALOG_TITLE);
    }
}
