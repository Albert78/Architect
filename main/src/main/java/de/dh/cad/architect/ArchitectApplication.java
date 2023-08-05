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

import de.dh.cad.architect.ui.ApplicationController;
import de.dh.cad.architect.ui.Configuration;
import javafx.application.Application;
import javafx.stage.Stage;

public class ArchitectApplication extends Application {
    private org.slf4j.Logger log = LoggerFactory.getLogger(ArchitectApplication.class);

    protected ApplicationController mApplicationController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            Configuration config = Configuration.from(Preferences.userNodeForPackage(ArchitectApplication.class));
            mApplicationController = ApplicationController.create(config, primaryStage);

            primaryStage.setOnCloseRequest(event -> {
                if (!mApplicationController.queryQuitApplication(primaryStage)) {
                    event.consume();
                }
            });

            mApplicationController.startup();
        } catch (Exception e) {
            log.error("Error starting application", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
