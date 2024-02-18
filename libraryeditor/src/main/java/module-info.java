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
module de.dh.cad.architect.libraryeditor {
    requires transitive de.dh.cad.architect.fxutils;
    requires transitive de.dh.cad.architect.model;
    requires transitive de.dh.cad.architect.ui;

    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires org.fxmisc.richtext;
    requires reactfx;
    requires de.dh.cad.architect.codeeditors;
    requires de.dh.utils.fx.viewsfx;
    requires org.apache.commons.io;
    requires org.codehaus.groovy;
    requires org.apache.commons.lang3;

    exports de.dh.cad.architect.libraryeditor;
    opens de.dh.cad.architect.libraryeditor;
}
