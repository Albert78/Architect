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
package de.dh.utils.fx.viewsfx.io;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import de.dh.utils.fx.viewsfx.state.ViewsLayoutState;
import de.dh.utils.fx.viewsfx.utils.JAXBUtility;

public class ViewsLayoutStateIO {
    /**
     * Desktop settings file extension without {@code '.'}.
     */
    public static final String FILE_EXTENSION = "xml";
    public static final String DEFAULT_FILE_NAME = "ViewsLayoutState" + "." + FILE_EXTENSION;

    public static final String FILE_SCHEMA_URL = "http://www.dh-software.de/utils/views-layout-state";

    public static void serialize(ViewsLayoutState layoutState, Writer writer) {
        try {
            JAXBContext context = JAXBContext.newInstance(ViewsLayoutState.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, FILE_SCHEMA_URL);
            JAXBUtility.configureMarshaller(m);
            m.marshal(layoutState, writer);
        } catch (JAXBException e) {
            throw new RuntimeException("Error serializing views layout state", e);
        }
    }

    public static void serializeDesktopSettings(ViewsLayoutState layoutState, Path path) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            serialize(layoutState, writer);
        } catch (Exception e) {
            throw new RuntimeException("Error writing views layout state to path '" + path + "'", e);
        }
    }

    public static ViewsLayoutState deserialize(Reader reader) {
        try {
            JAXBContext context = JAXBContext.newInstance(ViewsLayoutState.class);
            Unmarshaller u = context.createUnmarshaller();
            return (ViewsLayoutState) u.unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException("Error deserializing views layout state", e);
        }
    }

    public static ViewsLayoutState deserializeDesktopSettings(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            return deserialize(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error loading views layout state from path '" + path + "'", e);
        }
    }
}
