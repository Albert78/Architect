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
package de.dh.utils.io.obj;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.dh.cad.architect.utils.vfs.IDirectoryLocator;

/**
 * Contains the raw data from a material ({@code .mtl}) file.
 * We store the raw data and postpone the interpretation because I don't have enough knowledge about the model behind the format
 * to be able to decide how to store the data format independent. This way, we move the job of interpreting the raw {@code .mtl} text
 * lines to the point where the concrete 3D object material settings are configured.
 * This is bad because the model is completely fixed to the capabilities of the material file specification,
 * but it allows us a direct transformation from material file commands to the 3D objects (e.g. JavaFX).
 *
 * TODO: Create an MTL-format-independent model for the material
 */
public class RawMaterialData {
    protected final String mName;
    protected final List<String> mLines;
    protected final IDirectoryLocator mBaseDirectory;

    public RawMaterialData(String name, List<String> lines, IDirectoryLocator baseDirectory) {
        mName = name;
        mLines = new ArrayList<>(lines);
        mBaseDirectory = baseDirectory;
    }

    /**
     * Returns the name of this material as it was defined in the material file ({@code newmtl [Name]}).
     */
    public String getName() {
        return mName;
    }

    public List<String> getLines() {
        return mLines;
    }

    /**
     * Base directory of this material file - necessary to resolve referenced image files.
     */
    public IDirectoryLocator getBaseDirectory() {
        return mBaseDirectory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RawMaterialData other = (RawMaterialData) obj;
        return Objects.equals(mName, other.mName);
    }
}
