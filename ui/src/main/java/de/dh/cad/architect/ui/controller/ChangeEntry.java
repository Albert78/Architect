package de.dh.cad.architect.ui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.MacroChange;

public class ChangeEntry {
    protected final IModelChange mModelChange;
    protected final String mChangeDescription;

    public ChangeEntry(IModelChange modelChange, String changeDescription) {
        mModelChange = modelChange;
        mChangeDescription = changeDescription;
    }

    public IModelChange getModelChange() {
        return mModelChange;
    }

    public String getChangeDescription() {
        return mChangeDescription;
    }

    public Optional<ChangeEntry> tryMerge(ChangeEntry oldChangeEntry) {
        return mModelChange.tryMerge(oldChangeEntry.getModelChange())
                .map(mc -> {
                    String otherChangeDescription = oldChangeEntry.getChangeDescription();
                    String mergedChangeDescription = otherChangeDescription.equals(mChangeDescription) ?
                            mChangeDescription : otherChangeDescription + ", " + mChangeDescription;
                    return new ChangeEntry(mc, mergedChangeDescription);
                });
    }

    public ChangeEntry undo() {
        List<IModelChange> undoChangeTrace = new ArrayList<>();
        mModelChange.undo(undoChangeTrace);
        IModelChange undoChange = undoChangeTrace.size() == 1 ? undoChangeTrace.get(0) : MacroChange.create(undoChangeTrace, false);
        return new ChangeEntry(undoChange, mChangeDescription);
    }

    @Override
    public String toString() {
        return mChangeDescription;
    }
}
