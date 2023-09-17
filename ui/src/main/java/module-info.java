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
module de.dh.cad.architect.ui {
    exports de.dh.cad.architect.ui;
    exports de.dh.cad.architect.ui.view; // For ObjectReconcileOperation - could be moved to the core?
    exports de.dh.cad.architect.ui.utils;
    exports de.dh.cad.architect.ui.assets;
    exports de.dh.cad.architect.ui.persistence;
    exports de.dh.cad.architect.ui.view.libraries;
    opens de.dh.cad.architect.ui.view;
    opens de.dh.cad.architect.ui.controls;
    opens de.dh.cad.architect.ui.properties;
    opens de.dh.cad.architect.ui.objecttree;
    opens de.dh.cad.architect.ui.persistence;
    opens de.dh.cad.architect.ui.view.libraries;
    opens de.dh.cad.architect.ui.objects;
    opens de.dh.cad.architect.ui.assets;
    opens de.dh.cad.architect.ui.view.construction;
    opens de.dh.cad.architect.ui.view.construction.behaviors;
    opens de.dh.cad.architect.ui.view.construction.feedback.supportobjects;
    opens de.dh.cad.architect.ui.view.threed;
    opens de.dh.cad.architect.ui.view.threed.behaviors;

    requires de.dh.cad.architect.utils;
    requires transitive de.dh.cad.architect.model;
    requires transitive de.dh.cad.architect.fxutils;
    requires de.dh.utils.fx.viewsfx;

    requires javafx.web;
    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.fxml;
    requires transitive javafx.controls;
    requires org.slf4j;
    requires java.desktop;
    requires org.apache.commons.collections4;
    requires org.apache.commons.lang3;
    requires org.apache.commons.io;
    requires org.controlsfx.controls;
    requires de.dh.cad.architect.codeeditors;
    requires org.fxmisc.richtext;
}