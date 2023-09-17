package de.dh.cad.architect.model.changes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.objects.BaseObject;

/**
 * Represents a change or set of changes which was executed in the model.
 *
 * ATTENTION: Object identities have to be maintained when deleting / adding objects; when undoing a removal change or
 * redoing an addition change, the added/removed object identity must remain the same because further changes potentially
 * store references to objects in their undo operation using their ids. This means, an object creation typically breaks down
 * into a create operation and an add operation, where only the add operation is typically added to the undo stack - the
 * object creation itself can not be undone. An object instance, which was created, will typically remain in the undo history entry
 * of the add operation when the creation is undone and will be restored when the operation is redone. Delete operations
 * are typically implemented as removal of the object from its referencing objects, which also can
 * be added to the undo stack as a revertible operation.
 */
public interface IModelChange {
    Collection<BaseObject> getModifications();
    Collection<BaseObject> getAdditions();
    Collection<BaseObject> getRemovals();

    /**
     * Undo this change.
     * The given undo change trace will be filled with one or more {@link IModelChange} instances
     * representing the changes of the undo operation. Executing method {@link #undo(List)} on those
     * changes constitutes the <i>redo()</i> operation from this change's point of view.
     */
    void undo(List<IModelChange> undoChangeTrace);

    /**
     * Tries to merges this (new) change with the given other (already existing) change, if supported.
     * This method returns a new change containing the merged change. The merge will actually merge
     * the undo data and the modifications/additions/removals for both changes. The returned change might
     * simply be the already existing change, if it's undo data will also undo this change's impact.
     */
    default Optional<IModelChange> tryMerge(IModelChange oldChange) {
        return Optional.empty();
    }
}
