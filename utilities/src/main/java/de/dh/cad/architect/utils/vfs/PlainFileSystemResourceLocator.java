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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlainFileSystemResourceLocator extends PlainFileSystemPathLocator implements IResourceLocator {
    public PlainFileSystemResourceLocator(Path path) {
        super(path);
    }

    @Override
    public InputStream inputStream() throws IOException {
        return Files.newInputStream(mPath);
    }

    @Override
    public OutputStream outputStream() throws IOException {
        return Files.newOutputStream(mPath);
    }

    @Override
    public void delete() throws IOException {
        Files.deleteIfExists(mPath);
    }

    @Override
    public void mkParentDirs() throws IOException {
        Files.createDirectories(mPath.getParent());
    }
}
