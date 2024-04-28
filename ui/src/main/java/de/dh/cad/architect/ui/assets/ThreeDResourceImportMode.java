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

package de.dh.cad.architect.ui.assets;

public enum ThreeDResourceImportMode {
    /**
     * Only the given {@code .obj} file will be imported, nothing else.
     * This mode makes sense if the 3D model only consists of a single file.
     */
    ObjFile,

    /**
     * The whole directory of the given {@code .obj} file will be imported.
     * This mode makes sense if the 3D model includes more files like {@code .mtl} files,
     * images, a license file etc.
     * In this case, all those files must be located in a single directory and that directory should only
     * contain the files for that 3D model.
     */
    Directory
}
