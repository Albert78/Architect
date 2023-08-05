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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public class ClassLoaderFileSystemResourceLocator extends ClassLoaderFileSystemPathLocator implements IResourceLocator {
    /**
     * Creates a new {@link ClassLoaderFileSystemResourceLocator} for a resource which is accessible by the classloader
     * of the given module.
     * @param path Path of the resource, e.g. {@code "/de/dh/x/y/z/resource.png"}.
     * @param module Module which contains the resource, i.e. whose classloader is able to access the resource of the given name.
     * @see ClassLoaderFileSystemPathLocator#ClassLoaderFileSystemPathLocator(Path, Module)
     */
    public ClassLoaderFileSystemResourceLocator(Path path, Module module) {
        super(path, module);
    }

    @Override
    public InputStream inputStream() throws IOException {
        return getResourceFromClassLoader()
                .orElseThrow(() -> new FileNotFoundException("Resource '" + getPathStr() + "' is not present"));
    }

    @Override
    public OutputStream outputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void mkParentDirs() throws IOException {
        throw new UnsupportedOperationException();
    }
}
