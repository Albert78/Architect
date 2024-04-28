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
import java.util.Collection;
import java.util.function.Predicate;

import org.apache.commons.lang3.NotImplementedException;

public interface IDirectoryLocator extends IPathLocator {
    /**
     * Returns a collection of each directory and file in this directory which matches the given predicate.
     */
    Collection<IPathLocator> list(Predicate<IPathLocator> predicate) throws IOException;
    void mkDirs() throws IOException;

    /**
     * Deletes this directory with all its contents recursively.
     */
    void deleteRecursively() throws IOException;

    /**
     * Deletes all directory contents without deleting this directory.
     * If this directory didn't exist before, this operation creates it.
     */
    void clean() throws IOException;

    default boolean isDirectory() {
        return true;
    }

    default boolean isFile() {
        return false;
    }

    default void copyContentsTo(IDirectoryLocator targetDirectoryLocator) throws IOException {
        targetDirectoryLocator.mkDirs();
        for (IPathLocator sourceItemLocator : list(e -> true)) {
            String fileName = sourceItemLocator.getFileName();
            if (sourceItemLocator instanceof IResourceLocator sourceResource) {
                IResourceLocator targetResource = targetDirectoryLocator.resolveResource(fileName);
                sourceResource.copyTo(targetResource);
            } else if (sourceItemLocator instanceof IDirectoryLocator sourceChildDirectory) {
                IDirectoryLocator targetChildDirectory = targetDirectoryLocator.resolveDirectory(fileName);
                sourceChildDirectory.copyContentsTo(targetChildDirectory);
            } else {
                throw new NotImplementedException("Cannot handle path locator " + sourceItemLocator);
            }
        }
    }
}
