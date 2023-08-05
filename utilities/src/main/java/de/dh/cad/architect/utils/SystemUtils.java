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
package de.dh.cad.architect.utils;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtils {
    private static Logger log = LoggerFactory.getLogger(SystemUtils.class);

    public static void openDirectoryInExplorer(Path path) {
        if (org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS) {
            try {
                new ProcessBuilder("explorer.exe", "\"" + path + "\"").start();
            } catch (IOException e) {
                log.error("Error opening file explorer in path '" + path + "'");
            }
        } else {
            log.warn("Opening file explorer on platform " + org.apache.commons.lang3.SystemUtils.OS_NAME + " not supported yet");
        }
    }
}
