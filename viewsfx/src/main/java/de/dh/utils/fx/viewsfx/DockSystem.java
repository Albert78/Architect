package de.dh.utils.fx.viewsfx;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

/**
 * Static context for transferring dragged content.
 */
public class DockSystem {
    protected static final DataFormat DND_DATAFORMAT = new DataFormat("DockSystem");
    protected static final String DND_PLACEHOLDER = "DockSystem DND Operation";

    protected static Optional<DockDragOperation> mCurrentDragOperation = Optional.empty();
    protected static Stage mMainWindow = null;
    protected static ViewsRegistry mViewsRegistry = new ViewsRegistry();
    protected static Map<String, DockHostControl> mDockHostControls = new HashMap<>();

    protected static ObservableList<DockableFloatingStage> mFloatingStages = FXCollections.observableArrayList();

    static void notifyNewStage(DockableFloatingStage stage) {
        mFloatingStages.add(stage);
    }

    static void notifyStageClosed(DockableFloatingStage stage) {
        mFloatingStages.remove(stage);
    }

    static Optional<DockDragOperation> getCurrentDragOperation() {
        return mCurrentDragOperation;
    }

    static void installDragOperation(Dockable<?> dockable) {
        if (mCurrentDragOperation.isPresent()) {
            throw new IllegalStateException("A drag operation is already in progress");
        }

        DockDragOperation operation = new DockDragOperation(dockable);
        mCurrentDragOperation = Optional.of(operation);
    }

    public static void finishDragOperation() {
        if (mCurrentDragOperation.isEmpty()) {
            return;
        }
        DockDragOperation operation = mCurrentDragOperation.get();
        operation.dispose();
        mCurrentDragOperation = Optional.empty();
    }

    /**
     * This method needs to be called on each possible object which acts as a drag source for a dock operation,
     * i.e. all tabs of our tab dock hosts.
     * @param dockable The dockable object to be dragged/docked when the drag source is dragged.
     * @param dragSource UI object wich is a possible starting point for a mouse press-drag gesture for a dock operation.
     */
    static void installDragSourceEventHandlers(Dockable<?> dockable, Node dragSource) {
        dragSource.setOnDragDetected(event -> {
            // Neither of the different dragging modes implemented in JavaFX seem to work very well for us.
            // - In simple mode, drag events are only delivered to the source node.
            // - In full mode, drag events are delivered only in the same stage --> floatable stages don't work.
            // - System DND mode works but needs a workaround to store the object to be dragged, since the drag content
            // which is normally transferred via the Dragboard needs to be serializable.
            // It doesn't make sense to serialize our draggable object, so we must workaround it and store a static
            // drag operation object.
            Dragboard db = dragSource.startDragAndDrop(TransferMode.MOVE);

            ClipboardContent content = new ClipboardContent();
            content.put(DND_DATAFORMAT, DND_PLACEHOLDER);

            db.setContent(content);

            // Drag start -> initialize drag operation
            installDragOperation(dockable);

            event.consume();
        });
        dragSource.setOnDragDone(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                // Drag did not find a drop target nor was cancelled, i.e. the user dropped the object on a different
                // place in screen.
                // In this case, we will create a floating stage as drop target.

                // JavaFX 17:
                // - Bug: We are also routed to this position if the user had pressed the escape key. In that situation,
                //   the drop operation actually should be cancelled. But unfortunately, we cannot distinguish between
                //   "normal" mouse release and a press of the escape key here, can we?
                // - The given DragEvent doesn't contain sensible mouse coordinates, that's why we use Robot.
                //   Is there a better solution?
                Robot r = new Robot();
                DockableFloatingStage dfs = dockable.toFloatingState(mMainWindow, r.getMouseX(), r.getMouseY());
                dfs.requestFocus();
                DockSystem.finishDragOperation();
            });
        });
    }

    /**
     * Cleanup of the drag sources which have been prepared via {@link #installDragSourceEventHandlers(Dockable, Node)}
     */
    static void removeDragSourceEventHandlers(Node dragSource) {
        dragSource.setOnDragDetected(null);
        dragSource.setOnMouseReleased(null);
    }

    /**
     * This method needs to be called on each UI object which can act as drag target object, i.e. our tab dock hosts.
     */
    static void installDragTargetEventHandlers(Node dragTarget, IDockDragTargetHandlers handlers) {
        dragTarget.setOnDragEntered(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                if (handlers.handleDragEntered(operation, event.getX(), event.getY())) {
                    event.consume();
                }
            });
        });
        dragTarget.setOnDragOver(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                if (handlers.handleDragOver(operation, event.getX(), event.getY())) {
                    event.acceptTransferModes(TransferMode.ANY);
                    event.consume();
                }
            });
        });
        dragTarget.setOnDragExited(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                if (handlers.handleDragExited(operation)) {
                    event.consume();
                }
            });
        });
        dragTarget.setOnDragDropped(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                if (handlers.handleDragDropped(operation, event.getX(), event.getY())) {
                    event.setDropCompleted(true);
                    finishDragOperation();
                    event.consume();
                }
            });
        });
    }

    /**
     * Cleanup of event handlers which had been installed via {@link #installDragTargetEventHandlers(Node, IDockDragTargetHandlers)}.
     */
    static void removeDragTargetEventHandlers(Node dragTarget) {
        dragTarget.setOnDragEntered(null);
        dragTarget.setOnDragOver(null);
        dragTarget.setOnDragExited(null);
        dragTarget.setOnDragDropped(null);
    }

    public static ViewsRegistry getViewsRegistry() {
        return mViewsRegistry;
    }

    public static Map<String, DockHostControl> getDockHostControlsRegistry() {
        return mDockHostControls;
    }

    public static ObservableList<DockableFloatingStage> getFloatingStages() {
        return mFloatingStages;
    }

    public static Optional<TabDockHost> tryGetOrCreateDockZone(String dockZoneId) {
        for (DockHostControl dockHostControl : mDockHostControls.values()) {
            Optional<TabDockHost> res = dockHostControl.getOrTryCreateDockZone(dockZoneId);
            if (res.isPresent()) {
                return Optional.of(res.get());
            }
        }
        return Optional.empty();
    }

    public static Stage getMainWindow() {
        return mMainWindow;
    }

    public static void setMainWindow(Stage primaryStage) {
        mMainWindow = primaryStage;
    }
}
