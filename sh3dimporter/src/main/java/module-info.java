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
module de.dh.cad.architect.libraryimporter.sh3dimporter {
    exports de.dh.cad.architect.libraryimporter;
    exports de.dh.cad.architect.libraryimporter.sh3d;
    exports de.dh.cad.architect.libraryimporter.sh3d.furniture;
    exports de.dh.cad.architect.libraryimporter.sh3d.textures;

    opens de.dh.cad.architect.libraryimporter.ui;

    requires transitive javafx.graphics;
    requires transitive org.slf4j;
    requires transitive java.xml.bind;
    requires javafx.base;
    requires transitive de.dh.cad.architect.ui;
    requires javafx.controls;
    requires de.dh.cad.architect.fxutils;
    requires de.dh.cad.architect.utils;
    requires org.apache.commons.lang3;
    requires org.apache.commons.io;
}
