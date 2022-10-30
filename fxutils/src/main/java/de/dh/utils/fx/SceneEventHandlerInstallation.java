package de.dh.utils.fx;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Scene;

/**
 * Registration of a JavaFX event handler in a given scene.
 * Objects of this class act as a client-side container/handle for such a registration, which can be
 * installed and uninstalled multiple times.
 */
public class SceneEventHandlerInstallation<T extends Event> {
    protected final Scene mScene;
    protected final EventType<T> mEventType;
    protected final EventHandler<? super T> mEventHandler;

    protected SceneEventHandlerInstallation(Scene scene, EventType<T> eventType, EventHandler<? super T> eventHandler) {
        mScene = scene;
        mEventType = eventType;
        mEventHandler = eventHandler;
    }

    public void install() {
        mScene.addEventHandler(mEventType, mEventHandler);
    }

    public void uninstall() {
        mScene.removeEventHandler(mEventType, mEventHandler);
    }
}
