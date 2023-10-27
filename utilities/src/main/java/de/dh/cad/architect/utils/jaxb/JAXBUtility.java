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
package de.dh.cad.architect.utils.jaxb;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JAXBUtility {
    private static final Logger log = LoggerFactory.getLogger(JAXBUtility.class);

    public static ThreadLocal<Collection<Runnable>> mFinishCallbacksTL = new ThreadLocal<>();

    public static JAXBContext initializeJAXBContext(Class<?>... classesToBeBound) {
        try {
            return JAXBContext.newInstance(classesToBeBound);
        } catch (JAXBException e) {
            log.error("Error initializing JAXB context for classes " + StringUtils.join(classesToBeBound, ", "), e);
            throw new RuntimeException(e);
        }
    }

    public static void addFinishCallback(Runnable r) {
        Collection<Runnable> finishCallbacks = mFinishCallbacksTL.get();
        if (finishCallbacks == null) {
            mFinishCallbacksTL.set(finishCallbacks = new ArrayList<>());
        }
        finishCallbacks.add(r);
    }

    public static void executeAndClearFinishCallbacks() {
        Collection<Runnable> finishCallbacks = mFinishCallbacksTL.get();
        if (finishCallbacks != null) {
            mFinishCallbacksTL.remove();
            for (Runnable r: finishCallbacks) {
                r.run();
            }
        }
    }

    public static void configureMarshaller(Marshaller m) {
        try {
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF8");
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (PropertyException e) {
            log.warn("Error configuring JAXB marshaller", e);
        }
    }
}
