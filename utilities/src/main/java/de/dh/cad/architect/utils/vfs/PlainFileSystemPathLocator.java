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
package de.dh.cad.architect.utils.vfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class PlainFileSystemPathLocator extends PathBasedFileSystemPathLocator {
    public PlainFileSystemPathLocator(Path path) {
        super(path);
    }

    public static IPathLocator fromExistingPath(Path absoluteFileSystemPath) throws IOException {
        if (!Files.exists(absoluteFileSystemPath)) {
            throw new IOException("Path '" + absoluteFileSystemPath + "' doesn't exist");
        }
        return Files.isDirectory(absoluteFileSystemPath) ? new PlainFileSystemDirectoryLocator(absoluteFileSystemPath) : new PlainFileSystemResourceLocator(absoluteFileSystemPath);
    }

    @Override
    public IDirectoryLocator resolveDirectory(Path relativePathToDirectory) {
        return new PlainFileSystemDirectoryLocator(mPath.resolve(relativePathToDirectory));
    }

    @Override
    public IResourceLocator resolveResource(Path relativePathToResource) {
        return new PlainFileSystemResourceLocator(mPath.resolve(relativePathToResource));
    }

    @Override
    public IDirectoryLocator getParentDirectory() throws IOException {
        return new PlainFileSystemDirectoryLocator(mPath.getParent());
    }

    @Override
    public boolean exists() {
        return Files.exists(mPath);
    }
}
