package de.dh.utils.fx.viewsfx;

/**
 * Event handlers to be implemented for target UI objects for a drag/dock operation.
 * The event handler methods {@link #handleDragEntered(DockDragOperation, double, double)} and {@link #handleDragOver(DockDragOperation, double, double)}
 * are expected to call {@link DockDragOperation#setCurrentDockTarget(IDockTarget, java.util.Optional)} if the given drag position represents a docking target,
 * else they should call {@link DockDragOperation#removeDockFeedback()}.
 * Handler method {@link #handleDragExited(DockDragOperation)} should call {@link DockDragOperation#removeDockFeedback()} (and clean up possible local dock feedback resources).
 *
 * {@link #handleDragDropped(DockDragOperation, double, double)} doesn't need to remove the dragging feedback because method
 * {@link #handleDragExited(DockDragOperation)} will also be called in case of a drop.
 */
public interface IDockDragTargetHandlers {
    /**
     * Called when the mouse enters the underlaying UI element while dragging a dockable.
     */
    boolean handleDragEntered(DockDragOperation operation, double dragPositionX, double dragPositionY);

    /**
     * Called when the mouse drags over the underlaying UI element while dragging a dockable.
     */
    boolean handleDragOver(DockDragOperation operation, double dragPositionX, double dragPositionY);

    /**
     * Called when the mouse leaves the underlaying UI element while dragging a dockable.
     * This method is always called when {@link #handleDragEntered(DockDragOperation, double, double)} was called before.
     */
    boolean handleDragExited(DockDragOperation operation);

    /**
     * Called when the mouse drops a currently dragged dockable on the underlaying UI element.
     */
    boolean handleDragDropped(DockDragOperation operation, double dragPositionX, double dragPositionY);
}
