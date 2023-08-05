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
package de.dh.cad.architect.utils.vfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public abstract class PathBasedFileSystemPathLocator implements IPathLocator {
    protected final Path mPath;

    public PathBasedFileSystemPathLocator(Path path) {
        mPath = path;
    }

    public Path getPath() {
        return mPath;
    }

    @Override
    public String getFileName() {
        return mPath.getFileName().toString();
    }

    @Override
    public abstract IDirectoryLocator getParentDirectory() throws IOException;

    @Override
    public String getAbsolutePath() {
        return mPath.toString();
    }

    @Override
    public abstract IDirectoryLocator resolveDirectory(Path relativePathToDirectory);

    @Override
    public abstract IResourceLocator resolveResource(Path relativePathToResource);

    @Override
    public abstract boolean exists();

    @Override
    public int compareTo(IPathLocator o) {
        return getAbsolutePath().compareTo(o.getAbsolutePath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPath);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PathBasedFileSystemPathLocator other = (PathBasedFileSystemPathLocator) obj;
        return Objects.equals(mPath, other.mPath);
    }

    @Override
    public String toString() {
        return mPath.toString();
    }
}
