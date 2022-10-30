package de.dh.utils.fx.viewsfx.io;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import de.dh.utils.fx.viewsfx.utils.JAXBUtility;

public class DesktopSettingsIO {
    /**
     * Desktop settings file extension without {@code '.'}.
     */
    public static final String FILE_EXTENSION = "xml";
    public static final String DEFAULT_SETTINGS_FILE_NAME = "DesktopSettings" + "." + FILE_EXTENSION;

    public static final String DESKTOP_SETTINGS_FILE_SCHEMA_URL = "http://www.dh-software.de/utils/desktopsettings";

    public static void serializeDesktopSettings(DesktopSettings settings, Writer writer) {
        try {
            JAXBContext context = JAXBContext.newInstance(DesktopSettings.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, DESKTOP_SETTINGS_FILE_SCHEMA_URL);
            JAXBUtility.configureMarshaller(m);
            m.marshal(settings, writer);
        } catch (JAXBException e) {
            throw new RuntimeException("Error serializing desktop settings", e);
        }
    }

    public static void serializeDesktopSettings(DesktopSettings settings, Path path) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            serializeDesktopSettings(settings, writer);
        } catch (Exception e) {
            throw new RuntimeException("Error writing desktop settings to path '" + path + "'", e);
        }
    }

    public static DesktopSettings deserializeDesktopSettings(Reader reader) {
        try {
            JAXBContext context = JAXBContext.newInstance(DesktopSettings.class);
            Unmarshaller u = context.createUnmarshaller();
            return (DesktopSettings) u.unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException("Error deserializing desktop settings", e);
        }
    }

    public static DesktopSettings deserializeDesktopSettings(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            return deserializeDesktopSettings(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error loading desktop settings from path '" + path + "'", e);
        }
    }
}
