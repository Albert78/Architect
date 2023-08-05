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

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;

import de.dh.cad.architect.utils.jaxb.IDeserializationHandler;
import de.dh.cad.architect.utils.jaxb.JAXBUtility;

public class PlanFileIO {
    /**
     * Plan file extension without {@code '.'}.
     */
    public static final String PLAN_FILE_EXTENSION = "xml";
    public static final String DEFAULT_ROOT_PATH_NAME = "PlanFile" + "." + PLAN_FILE_EXTENSION;

    public static final String PLAN_FILE_SCHEMA_URL = "http://www.dh-software.de/architect/v2/planfile";

    public static void serializePlanFile(PlanFile planFile, Writer writer) {
        try {
            JAXBContext context = JAXBContext.newInstance(PlanFile.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, PLAN_FILE_SCHEMA_URL);
            JAXBUtility.configureMarshaller(m);
            m.marshal(planFile, writer);
        } catch (JAXBException e) {
            throw new RuntimeException("Error serializing plan file", e);
        }
    }

    public static void serializePlanFile(PlanFile planFile, Path path) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            serializePlanFile(planFile, writer);
        } catch (Exception e) {
            throw new RuntimeException("Error writing plan file to path '" + path + "'", e);
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

    public static PlanFile deserializePlanFile(Reader reader) {
        try {
            JAXBContext context = JAXBContext.newInstance(PlanFile.class);
            Unmarshaller u = context.createUnmarshaller();
            List<PendingUnmarshalCall> pendingUnmarshalCalls = new ArrayList<>();
            u.setListener(new Listener() {
                @Override
                public void afterUnmarshal(Object target, Object parent) {
                    if (target instanceof IDeserializationHandler handler) {
                        pendingUnmarshalCalls.add(new PendingUnmarshalCall(handler, parent));
                    }
                }
            });
            PlanFile result = (PlanFile) u.unmarshal(reader);
            for (PendingUnmarshalCall pendingUnmarshalCall : pendingUnmarshalCalls) {
                pendingUnmarshalCall.getHandler().afterDeserialize(pendingUnmarshalCall.getParent());
            }
            return result;
        } catch (JAXBException e) {
            throw new RuntimeException("Error deserializing plan", e);
        }
    }

    public static PlanFile deserializePlanFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            return deserializePlanFile(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error loading root plan from path '" + path + "'", e);
        }
    }
}
