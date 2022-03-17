/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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
package de.dh.cad.architect.model.assets;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.utils.jaxb.PathJavaTypeAdapter;

/**
 * A model in form of a file.
 */
public class FileModelResource extends AbstractModelResource {
    protected Path mRelativeFilePath;

    public FileModelResource() {
        // For JAXB
    }

    public FileModelResource(Path relativeFilePath) {
        mRelativeFilePath = relativeFilePath;
    }

    /**
     * Returns the path of this resource relative to a known base directory.
     */
    @XmlAttribute(name = "resource")
    @XmlJavaTypeAdapter(PathJavaTypeAdapter.class)
    public Path getRelativePath() {
        return mRelativeFilePath;
    }

    public void setRelativePath(Path value) {
        mRelativeFilePath = value;
    }

    @Override
    public String toString() {
        return mRelativeFilePath.toString();
    }
}
