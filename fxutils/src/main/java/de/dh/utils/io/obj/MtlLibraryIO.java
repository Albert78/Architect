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
package de.dh.utils.io.obj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;


/**
 * Reader and writer for OBJ file MTL material library files.
 * This class handles materials in form of {@link RawMaterialData} instances which just contain the
 * raw (uninterpreted) material data string lines.
 */
public class MtlLibraryIO {
    private static final Logger log = LoggerFactory.getLogger(MtlLibraryIO.class);

    /**
     * Reads material entries from the given {@ .mtl} file.
     * The {@code .mtl} format uses file names to reference additional image resources so it is not possible to
     * completely abstract from the file system (e.g. by just passing a {@link Reader}). Instead, we
     * abstract from the disc file system and allow arbitrary file systems by using {@link IResourceLocator}.
     * @return Map of material names to material data.
     */
    // To read a material library file from file system, use new PlainFileSystemResourceLocator(mtlFile)
    public static Map<String, RawMaterialData> readMaterialSet(IResourceLocator mtlResource) throws IOException {
        IDirectoryLocator baseDirectory = mtlResource.getParentDirectory();
        try (Reader r = new InputStreamReader(mtlResource.inputStream())) {
            return readMaterialSet(r, baseDirectory);
        }
    }

    public static void writeMaterialSet(IResourceLocator mtlResource, Collection<RawMaterialData> materials) throws IOException {
        try (Writer w = new OutputStreamWriter(mtlResource.outputStream())) {
            writeMaterialSet(w, materials);
        }
    }

    public static void writeMaterialSet(Writer w, Collection<RawMaterialData> materials) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(w)) {
            for (RawMaterialData material : materials) {
                bw.write("newmtl " + material.getName() + "\n");
                for (String line : material.getLines()) {
                    bw.write(line + "\n");
                }
            }
        }
    }

    static Map<String, RawMaterialData> readMaterialSet(Reader r, IDirectoryLocator baseDirectory) throws IOException {
        Map<String, RawMaterialData> result = new TreeMap<>();
        String line;
        String name = "default";
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(r)) {
            while ((line = br.readLine()) != null) {
                if (StringUtils.isEmpty(line) || line.startsWith("#")) {
                    // Ignore comments and empty lines
                } else if (line.startsWith("newmtl ")) {
                    if (!lines.isEmpty()) {
                        // Finish last material
                        result.put(name, new RawMaterialData(name, lines, baseDirectory));
                        lines = new ArrayList<>();
                    }
                    name = line.substring(7).trim();
                } else {
                    lines.add(line);
                }
            }
            if (!lines.isEmpty()) {
                result.put(name, new RawMaterialData(name, lines, baseDirectory));
            }
        }
        return result;
    }

    public static Collection<IResourceLocator> getAllFiles(IResourceLocator mtlResource) throws IOException {
        Collection<IResourceLocator> result = new ArrayList<>();
        result.add(mtlResource);
        IDirectoryLocator parentDirectory = mtlResource.getParentDirectory();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(mtlResource.inputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    if (line.startsWith("map_Kd ")) {
                        String imageFileName = line.substring("map_Kd ".length());
                        result.add(parentDirectory.resolveResource(imageFileName));
                    }
                } catch (Exception ex) {
                    log.error("Failed to parse line: " + line, ex);
                }
            }
        } catch (IOException ex) {
            log.error("Error reading material file '" + mtlResource + "'", ex);
        }
        return result;
    }
}
