package de.dh.cad.architect.model.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.objects.BaseObject;

public class MacroChange implements IModelChange {
    protected final Stack<IModelChange> mChanges = new Stack<>();

    protected final Set<BaseObject> mMergedAdditions;
    protected final Set<BaseObject> mMergedModifications;
    protected final Set<BaseObject> mMergedRemovals;

    public MacroChange(List<IModelChange> changes,
        Set<BaseObject> mergedAdditions,
        Set<BaseObject> mergedModifications,
        Set<BaseObject> mergedRemovals) {
        for (IModelChange change : changes) {
            mChanges.push(change);
        }

        mMergedAdditions = mergedAdditions;
        mMergedModifications = mergedModifications;
        mMergedRemovals = mergedRemovals;
    }

    public static MacroChange create(List<IModelChange> changes, boolean tryMergeChanges) {
        List<IModelChange> consolidatedChanges = new ArrayList<>();
        IModelChange lastChange = null;
        for (IModelChange change : changes) {
            if (tryMergeChanges && lastChange != null) {
                Optional<IModelChange> oMerged = change.tryMerge(lastChange);
                if (oMerged.isPresent()) {
                    IModelChange merged = oMerged.get();
                    consolidatedChanges.set(consolidatedChanges.size() - 1, merged);
                    lastChange = merged;
                    continue;
                }
            }
            consolidatedChanges.add(change);
            lastChange = change;
        }

        // When consolidating additions and removals, it is important to identify objects which
        // between
        Set<BaseObject> initialAdditions = new HashSet<>(); // Objects which were not present before
        Set<BaseObject> initiallyPresent = new HashSet<>(); // Objects which were present before

        Set<BaseObject> mergedAdditions = new HashSet<>();
        Set<BaseObject> mergedModifications = new HashSet<>();
        Set<BaseObject> mergedRemovals = new HashSet<>();
        for (IModelChange change : consolidatedChanges) {
            Collection<BaseObject> removals = change.getRemovals();
            Collection<BaseObject> additions = change.getAdditions();
            Collection<BaseObject> modifications = change.getModifications();

            Collection<BaseObject> presentBefore = CollectionUtils.subtract(removals, initialAdditions); // Those were present before
            initiallyPresent.addAll(presentBefore);

            Collection<BaseObject> reallyAdded = CollectionUtils.subtract(additions, initiallyPresent); // Those were not present before
            initialAdditions.addAll(reallyAdded);

            mergedAdditions.removeAll(removals);
            mergedRemovals.addAll(removals);
            mergedRemovals.removeAll(additions);
            mergedAdditions.addAll(additions);
            mergedModifications.removeAll(removals);
            mergedModifications.addAll(modifications);
        }

        mergedAdditions.removeAll(initiallyPresent);

        mergedModifications.addAll(initiallyPresent); // Those were not added because they were present before, but changed during the consolidation
        mergedModifications.removeAll(mergedAdditions);

        mergedRemovals.removeAll(initialAdditions);
        return new MacroChange(consolidatedChanges, mergedAdditions, mergedModifications, mergedRemovals);
    }

    public static MacroChange create(boolean tryMergeChanges, IModelChange... changes) {
        return create(Arrays.asList(changes), tryMergeChanges);
    }

    @Override
    public void undo(List<IModelChange> undoChangeTrace) {
        List<IModelChange> childUndoChangeTrace = new ArrayList<>();
        while (!mChanges.empty()) {
            IModelChange change = mChanges.pop();
            change.undo(childUndoChangeTrace);
        }
        undoChangeTrace.add(MacroChange.create(childUndoChangeTrace, false));
    }

    @Override
    public Collection<BaseObject> getAdditions() {
        return mMergedAdditions;
    }

    @Override
    public Collection<BaseObject> getModifications() {
        return mMergedModifications;
    }

    @Override
    public Collection<BaseObject> getRemovals() {
        return mMergedRemovals;
    }
}
