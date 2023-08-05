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
package de.dh.cad.architect.fx.nodes.objviewer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import de.dh.cad.architect.utils.vfs.ClassLoaderFileSystemResourceLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.io.ObjData;
import de.dh.utils.io.fx.FxMeshBuilder;
import de.dh.utils.io.obj.ObjReader;
import javafx.scene.Group;
import javafx.scene.Node;

public class Flashlight extends Group {
    protected static final String FLASHLIGHT_RESOURCE_BASE = '/' + Flashlight.class.getPackageName().replace(".", "/");
    protected static final String FLASHLIGHT_OBJ_NAME = "Flashlight.obj";

    @SuppressWarnings("unchecked")
    protected Flashlight(Collection<? extends Node> children) {
        super((Collection<Node>) children);
    }

    public static Flashlight create() throws IOException {
        IResourceLocator flashLightResourceLocator = new ClassLoaderFileSystemResourceLocator(
            Path.of(FLASHLIGHT_RESOURCE_BASE, FLASHLIGHT_OBJ_NAME), Flashlight.class.getModule());
        ObjData data = ObjReader.readObj(flashLightResourceLocator);
        return new Flashlight(FxMeshBuilder.buildMeshViews(data.getMeshes(), data.getMeshIdsToMaterials(), true));
    }
}
