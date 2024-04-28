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
package de.dh.utils.io.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.utils.Vector2D;
import de.dh.utils.io.obj.RawMaterialData;

/**
 * Contains all data about a material which is needed by {@link FxMeshBuilder} to apply a
 * material to a mesh object.
 * This is similar to the {@link RawMaterialData} class but additionally contains an optional
 * {@link #getTileSize() tile size}.
 * This class is intentionally not derived or otherwise related to {@link RawMaterialData} to
 * have an independent format to convert the material model of the application to.
 */
public class MaterialData {
    protected String mName;
    protected final List<String> mLines;
    protected final Optional<Vector2D> mTileSize;
    protected final IDirectoryLocator mBaseDirectory;

    public MaterialData(String name, List<String> lines, Optional<Vector2D> tileSize, IDirectoryLocator baseDirectory) {
        mName = name;
        mLines = new ArrayList<>(lines);
        mTileSize = tileSize;
        mBaseDirectory = baseDirectory;
    }

    public static Map<String, MaterialData> wrap(Map<String, RawMaterialData> rmdMap) {
        return rmdMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    RawMaterialData rmd = e.getValue();
                    return MaterialData.from(rmd);
                }));
    }

    public static MaterialData from(RawMaterialData rmd) {
        return from(rmd, Optional.empty());
    }

    public static MaterialData from(RawMaterialData rmd, Optional<Vector2D> tileSize) {
        return new MaterialData(rmd.getName(), rmd.getLines(), tileSize, rmd.getBaseDirectory());
    }

    public String getName() {
        return mName;
    }

    public List<String> getLines() {
        return mLines;
    }

    public Optional<Vector2D> getTileSize() {
        return mTileSize;
    }

    /**
     * Base directory to resolve referenced texture and other files.
     */
    public IDirectoryLocator getBaseDirectory() {
        return mBaseDirectory;
    }

    @Override
    public int hashCode() {
        return mLines.hashCode() + mBaseDirectory.getAbsolutePath().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MaterialData other = (MaterialData) obj;
        return Objects.equals(mLines, other.mLines) && Objects.equals(mBaseDirectory.getAbsolutePath(), other.mBaseDirectory.getAbsolutePath());
    }
}
