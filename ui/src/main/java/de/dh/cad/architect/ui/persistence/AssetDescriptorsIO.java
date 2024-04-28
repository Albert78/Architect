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

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.utils.jaxb.IDeserializationHandler;
import de.dh.cad.architect.utils.jaxb.JAXBUtility;

public class AssetDescriptorsIO {
    public static final String ASSET_FILE_SCHEMA_URL = "http://www.dh-software.de/architect/v2_2/assets";

    protected static final JAXBContext mSO_JAXBContext = JAXBUtility.initializeJAXBContext(SupportObjectDescriptor.class);
    protected static final JAXBContext mMS_JAXBContext = JAXBUtility.initializeJAXBContext(MaterialSetDescriptor.class);

    public static void serializeSupportObjectDescriptor(SupportObjectDescriptor descriptor, Writer writer) throws IOException {
        try {
            Marshaller m = mSO_JAXBContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, ASSET_FILE_SCHEMA_URL);
            JAXBUtility.configureMarshaller(m);
            m.marshal(descriptor, writer);
        } catch (JAXBException e) {
            throw new IOException("Error serializing support object descriptor", e);
        }
    }

    public static void serializeSupportObjectDescriptor(SupportObjectDescriptor descriptor, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            serializeSupportObjectDescriptor(descriptor, writer);
        } catch (Exception e) {
            throw new IOException("Error writing support object descriptor to path '" + path + "'", e);
        }
    }

    public static void serializeMaterialSetDescriptor(MaterialSetDescriptor descriptor, Writer writer) throws IOException {
        try {
            Marshaller m = mMS_JAXBContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, ASSET_FILE_SCHEMA_URL);
            JAXBUtility.configureMarshaller(m);
            m.marshal(descriptor, writer);
        } catch (JAXBException e) {
            throw new IOException("Error serializing material set descriptor", e);
        }
    }

    public static void serializeMaterialSetDescriptor(MaterialSetDescriptor descriptor, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            serializeMaterialSetDescriptor(descriptor, writer);
        } catch (Exception e) {
            throw new IOException("Error writing material set descriptor to path '" + path + "'", e);
        }
    }

    static class PendingUnmarshalCall {
        private final IDeserializationHandler mHandler;
        private final Object mParent;

        PendingUnmarshalCall(IDeserializationHandler handler, Object parent) {
            mHandler = handler;
            mParent = parent;
        }

        public IDeserializationHandler getHandler() {
            return mHandler;
        }

        public Object getParent() {
            return mParent;
        }
    }

    public static SupportObjectDescriptor deserializeSupportObjectDescriptor(Reader reader, AssetRefPath descriptorRef) throws IOException {
        try {
            Unmarshaller u = mSO_JAXBContext.createUnmarshaller();
            SupportObjectDescriptor result = (SupportObjectDescriptor) u.unmarshal(reader);
            result.setSelfRef(descriptorRef);
            return result;
        } catch (JAXBException e) {
            throw new IOException("Error deserializing support object descriptor", e);
        }
    }

    public static SupportObjectDescriptor deserializeSupportObjectDescriptor(Path path, AssetRefPath descriptorRef) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return deserializeSupportObjectDescriptor(reader, descriptorRef);
        } catch (Exception e) {
            throw new IOException("Error loading support object descriptor from path '" + path + "'", e);
        }
    }

    public static MaterialSetDescriptor deserializeMaterialSetDescriptor(Reader reader, AssetRefPath descriptorRef) throws IOException {
        try {
            Unmarshaller u = mMS_JAXBContext.createUnmarshaller();
            MaterialSetDescriptor result = (MaterialSetDescriptor) u.unmarshal(reader);
            result.setSelfRef(descriptorRef);
            return result;
        } catch (JAXBException e) {
            throw new IOException("Error deserializing material set descriptor", e);
        }
    }

    public static MaterialSetDescriptor deserializeMaterialSetDescriptor(Path path, AssetRefPath descriptorRef) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return deserializeMaterialSetDescriptor(reader, descriptorRef);
        } catch (Exception e) {
            throw new IOException("Error loading material set descriptor from path '" + path + "'", e);
        }
    }
}
