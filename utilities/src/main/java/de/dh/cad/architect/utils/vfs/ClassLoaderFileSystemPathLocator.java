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
import java.nio.file.Path;
import java.util.Optional;

public abstract class ClassLoaderFileSystemPathLocator extends PathBasedFileSystemPathLocator {
    protected final Class<?> mBase;

    /**
     * Creates a new {@link ClassLoaderFileSystemPathLocator} instance.
     * @param path Absolute path of the resource or directory in the module, e.g. <code>/de/foo/bar/Resource.txt</code>
     * or <code>/de/foo/bar/</code>.
     * @param base Class whose module contains the resource, e.g. <code>classNearResource.getModule()</code>.
     */
    protected ClassLoaderFileSystemPathLocator(Path path, Class<?> base) {
        super(path);
        mBase = base;
    }

    @Override
    public IDirectoryLocator resolveDirectory(Path relativePathToDirectory) {
        return new ClassLoaderFileSystemDirectoryLocator(mPath.resolve(relativePathToDirectory), mBase);
    }

    @Override
    public IResourceLocator resolveResource(Path relativePathToResource) {
        return new ClassLoaderFileSystemResourceLocator(mPath.resolve(relativePathToResource), mBase);
    }

    protected Optional<InputStream> getResourceFromClassLoader() {
        return Optional.ofNullable(mBase.getResourceAsStream(getPathStr()));
    }

    @Override
    public IDirectoryLocator getParentDirectory() throws IOException {
        return new ClassLoaderFileSystemDirectoryLocator(mPath.getParent(), mBase);
    }

    /**
     * Attention: {@link ClassLoaderFileSystemPathLocator} can only return correct values for (file) resources. For
     * directories, we do not have a way of getting that information via the classloader API.
     */
    @Override
    public boolean exists() {
        return getResourceFromClassLoader().isPresent();
    }

    protected String getPathStr() {
        return mPath.toString().replace('\\', '/');
    }

    @Override
    public String toString() {
        return getPathStr();
    }
}
