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
package de.dh.cad.architect.ui.assets;

import java.util.Collection;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Transform;

/**
 * Contains a 3D object, consisting of surfaces and an optional global normalization transformation.
 * Each surface is meant to be textured with an arbitrary material. The global normalization transform is used
 * to compensate a potential object rotation of the base object.
 */
public class ThreeDObject {
    protected final Collection<MeshView> mSurfaces;
    protected final Optional<Transform> mORootTransformation;

    public ThreeDObject(Collection<MeshView> surfaces, Optional<Transform> oTrans) {
        mSurfaces = surfaces;
        mORootTransformation = oTrans;
    }

    /**
     * Returns the surfaces of this 3D object. Each returned {@link MeshView} has the surface id set
     * in the {@link Node#getId() id property}.
     */
    public Collection<MeshView> getSurfaces() {
        return mSurfaces;
    }

    public Optional<Transform> getORootTransformation() {
        return mORootTransformation;
    }
}