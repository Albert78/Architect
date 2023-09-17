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

public interface IResourceLocator extends IPathLocator {
    InputStream inputStream() throws IOException;
    OutputStream outputStream() throws IOException;
    void delete() throws IOException;
    void mkParentDirs() throws IOException;

    default void copyFrom(InputStream inputStream) throws IOException {
        mkParentDirs();
        try (OutputStream os = outputStream()) {
            inputStream.transferTo(os);
        }
    }

    default void copyTo(OutputStream outputStream) throws IOException {
        try (InputStream is = inputStream()) {
            is.transferTo(outputStream);
        }
    }

    default void copyFrom(IResourceLocator source) throws IOException {
        try (InputStream is = source.inputStream()) {
            copyFrom(is);
        }
    }

    default void copyTo(IResourceLocator target) throws IOException {
        try (OutputStream os = target.outputStream()) {
            copyTo(os);
        }
    }
    default void copyTo(IDirectoryLocator target) throws IOException {
        IResourceLocator targetResource = target.resolveResource(getFileName());
        copyTo(targetResource);
    }
}
