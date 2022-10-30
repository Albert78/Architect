package de.dh.utils.fx.viewsfx.utils;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JAXBUtility {
    private static final Logger log = LoggerFactory.getLogger(JAXBUtility.class);

    public static ThreadLocal<Collection<Runnable>> mFinishCallbacksTL = new ThreadLocal<>();

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
