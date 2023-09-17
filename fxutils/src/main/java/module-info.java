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
module de.dh.cad.architect.fxutils {
    exports de.dh.cad.architect.fx.nodes;
    exports de.dh.cad.architect.fx.nodes.objviewer;
    exports de.dh.utils;
    exports de.dh.utils.fx;
    exports de.dh.utils.io;
    exports de.dh.utils.io.obj;
    exports de.dh.utils.io.fx;
    exports eu.mihosoft.vvecmath;
    exports eu.mihosoft.jcsg;
    exports eu.mihosoft.jcsg.ext.org.poly2tri;
    exports de.dh.utils.fx.dialogs;
    exports de.dh.utils.csg;
    opens de.dh.cad.architect.fx.nodes.objviewer;

    requires transitive javafx.base;
    requires transitive javafx.graphics;
    requires transitive javafx.swing; // Necessary for image IO utilities
    requires javafx.controls;
    requires org.slf4j;
    requires transitive de.dh.cad.architect.utils;
    requires org.apache.commons.lang3;
    requires javafx.fxml;
}