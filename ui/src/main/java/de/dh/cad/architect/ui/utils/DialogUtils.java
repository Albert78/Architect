/*******************************************************************************
 * Architect - A free 2D/3D home and interior designer
 * Copyright (c) 2024 Daniel Höh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 ******************************************************************************/

package de.dh.cad.architect.ui.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetManagerConfiguration;
import de.dh.utils.fx.FxUtils;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class DialogUtils {
    public static Collection<FileChooser.ExtensionFilter> getImageExtensionFilters() {
        return Arrays.asList(
                new FileChooser.ExtensionFilter(Strings.ALL_FILES_EXTENSION_FILTER_NAME, "*.*"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
    }

    public static Path openImageDialog(String title, AssetManagerConfiguration configuration, Window window) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        Optional<Path> oLastImagePath = configuration.getLastImagePath();
        oLastImagePath.ifPresent(path -> FxUtils.trySetInitialDirectory(fc, path));
        fc.getExtensionFilters().addAll(getImageExtensionFilters());
        File imageFile = fc.showOpenDialog(window);
        if (imageFile == null) {
            return null;
        }
        Path imagePath = imageFile.toPath();
        configuration.setLastImagePath(imagePath);
        return imagePath;
    }

    public static Path openObject3DResourceDialog(AssetManagerConfiguration configuration, Window window) {
        FileChooser fc = new FileChooser();
        fc.setTitle(Strings.SELECT_OBJECT_3D_RESOURCE_DIALOG_TITLE);
        Optional<Path> oLast3DResourcePath = configuration.getLast3DResourcePath();
        oLast3DResourcePath.ifPresent(path -> FxUtils.trySetInitialDirectory(fc, path));
        fc.getExtensionFilters().addAll(getImageExtensionFilters());
        File resource3DFile = fc.showOpenDialog(window);
        if (resource3DFile == null) {
            return null;
        }
        Path resource3DFilePath = resource3DFile.toPath();
        Path resource3DDirectoryPath = resource3DFilePath.getParent();
        configuration.setLast3DResourcePath(resource3DDirectoryPath);
        return resource3DFilePath;
    }
}
