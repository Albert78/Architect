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
package de.dh.cad.architect.model.assets;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.jaxb.Matrix3x3JavaTypeAdapter;

/**
 * Some 3D model file.
 */
public class ThreeDModelResource extends FileModelResource {
    protected float[][] mModelRotationMatrix = null;

    public ThreeDModelResource() {
        // For JAXB
    }

    public ThreeDModelResource(Path relativeFilePath) {
        super(relativeFilePath);
    }

    /**
     * Returns the rotation matrix which must be applied to the model 3D object to transform it to the canonical coordinate system, or {@code null}.
     */
    @XmlElement(name = "ModelRotationMatrix")
    @XmlJavaTypeAdapter(Matrix3x3JavaTypeAdapter.class)
    public float[][] getModelRotationMatrix() {
        return mModelRotationMatrix;
    }

    public void setModelRotationMatrix(float[][] value) {
        mModelRotationMatrix = value;
    }
}
