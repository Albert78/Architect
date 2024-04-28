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
package de.dh.cad.architect.ui.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import de.dh.cad.architect.model.assets.AssetLibrary;
import de.dh.cad.architect.utils.jaxb.JAXBUtility;

public class LibraryIO {
    /**
     * Library file extension without {@code '.'}.
     */
    public static final String ASSET_LIBRARY_FILE_EXTENSION = "xml";
    public static final String DEFAULT_ASSET_LIBRARY_FILE_NAME = "AssetLibrary" + "." + ASSET_LIBRARY_FILE_EXTENSION;

    public static final String ASSET_LIBRARY_FILE_SCHEMA_URL = "http://www.dh-software.de/architect/v2_1/assetlibrary";

    protected static final JAXBContext mJAXBContext = JAXBUtility.initializeJAXBContext(AssetLibrary.class);

    public static void serializeAssetLibrary(AssetLibrary library, Writer writer) throws IOException {
        try {
            Marshaller m = mJAXBContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, ASSET_LIBRARY_FILE_SCHEMA_URL);
            JAXBUtility.configureMarshaller(m);
            m.marshal(library, writer);
        } catch (JAXBException e) {
            throw new IOException("Error serializing asset library", e);
        }
    }

    public static void serializeAssetLibrary(AssetLibrary library, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            serializeAssetLibrary(library, writer);
        } catch (Exception e) {
            throw new IOException("Error writing asset library to path '" + path + "'", e);
        }
    }

    public static AssetLibrary deserializeAssetLibrary(Reader reader) throws IOException {
        try {
            Unmarshaller u = mJAXBContext.createUnmarshaller();
            AssetLibrary result = (AssetLibrary) u.unmarshal(reader);
            return result;
        } catch (JAXBException e) {
            throw new IOException("Error deserializing asset library", e);
        }
    }

    public static AssetLibrary deserializeAssetLibrary(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return deserializeAssetLibrary(reader);
        } catch (Exception e) {
            throw new IOException("Error loading asset library from path '" + path + "'", e);
        }
    }
}
