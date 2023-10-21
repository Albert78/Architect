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
package de.dh.cad.architect;

import java.util.prefs.Preferences;

import org.slf4j.LoggerFactory;

import de.dh.cad.architect.libraryeditor.LibraryEditorConfiguration;
import de.dh.cad.architect.libraryeditor.LibraryEditorMainWindow;
import de.dh.cad.architect.ui.assets.AssetManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class LibraryEditorApplication extends Application {
    private org.slf4j.Logger log = LoggerFactory.getLogger(LibraryEditorApplication.class);

    protected AssetManager mAssetManager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            mAssetManager = AssetManager.create();
            mAssetManager.start();
            LibraryEditorConfiguration config = LibraryEditorConfiguration.from(Preferences.userNodeForPackage(LibraryEditorApplication.class));
            LibraryEditorMainWindow mainWindow = LibraryEditorMainWindow.create(mAssetManager, config);
            mainWindow.show(primaryStage);
        } catch (Exception e) {
            log.error("Error starting application", e);
        }
    }

    @Override
    public void stop() throws Exception {
        mAssetManager.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
