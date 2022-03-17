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
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClassLoaderFileSystemDirectoryLocator extends ClassLoaderFileSystemPathLocator implements IDirectoryLocator {
    public ClassLoaderFileSystemDirectoryLocator(Path path, Module module) {
        super(path, module);
    }

    @Override
    public Collection<IPathLocator> list(Predicate<IPathLocator> predicate) throws IOException {
        Collection<IPathLocator> result = new ArrayList<>();
        for (Path childPath : Files.list(mPath).collect(Collectors.toList())) {
            IPathLocator childLocator = PlainFileSystemPathLocator.fromExistingPath(childPath);
            if (predicate.test(childLocator)) {
                result.add(childLocator);
            }
        }
        return result;
    }

    @Override
    public void deleteRecursively() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clean() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void mkDirs() throws IOException {
        throw new UnsupportedOperationException();
    }
}
