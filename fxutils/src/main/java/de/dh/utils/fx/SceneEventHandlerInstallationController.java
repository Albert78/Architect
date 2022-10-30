package de.dh.utils.fx;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Installation of a JavaFX event handler on a {@link Node}'s scene which is potentially not yet set.
 * This class will track the state of the node's scene and will install the even handler if possible,
 * it will remove it when the scene is lost (scene property's value becomes {@code null}).
 */
public class SceneEventHandlerInstallationController<T extends Event>  {
    protected final ReadOnlyObjectProperty<Scene> mSceneProperty;

    protected final ChangeListener<Scene> SCENE_LISTENER = (obs, oldVal, newVal) -> {
        if (oldVal != null) {
            uninstallEventHandler();
        }
        if (newVal != null) {
            installEventHandler(newVal);
        }
    };

    protected final EventType<T> mEventType;
    protected final EventHandler<? super T> mEventHandler;

    protected SceneEventHandlerInstallation<T> mEventHandlerInstallation = null;

    /**
     * Installs the scene event handler installation controller to the scene property of the given node.
     * If the scene is already attached to the given node,
     * the event handler will be installed at once, else this method will attach a change listener to the scene property
     * to await its value, installing the handler at assignment time of the scene and uninstalling it when the scene is removed.
     */
    public SceneEventHandlerInstallationController(Node node, EventType<T> eventType, EventHandler<? super T> eventHandler) {
        mEventType = eventType;
        mEventHandler = eventHandler;

        mSceneProperty = node.sceneProperty();
        mSceneProperty.addListener(SCENE_LISTENER);
        Scene scene = mSceneProperty.get();
        if (scene != null) {
            installEventHandler(scene);
        } // Else scene change handler will trigger installation later
    }

    public boolean isInstalled() {
        return mEventHandlerInstallation != null;
    }

    protected void installEventHandler(Scene scene) {
        mEventHandlerInstallation = new SceneEventHandlerInstallation<>(scene, mEventType, mEventHandler);
        mEventHandlerInstallation.install();
    }

    protected void uninstallEventHandler() {
        mEventHandlerInstallation.uninstall();
        mEventHandlerInstallation = null;
    }

    /**
     * Call this to cancel the deferred handler installation trigger and the event handler installation, if it has already
     * been installed.
     */
    public void dispose() {
        mSceneProperty.removeListener(SCENE_LISTENER);
        if (mEventHandlerInstallation != null) {
            uninstallEventHandler();
        }
    }
}
