package de.dh.cad.architect.ui.controller;

import java.util.Optional;
import java.util.Stack;

public class ChangeHistory {
    protected final Stack<ChangeEntry> mUndoStack = new Stack<>();
    protected final Stack<ChangeEntry> mRedoStack = new Stack<>();

    public void pushChange(ChangeEntry change, boolean tryMergeChange) {
        if (!tryMergeChange || !tryMergeChange(change)) {
            mUndoStack.push(change);
        }
        mRedoStack.clear();
    }

    protected void exchangeRecentChange(ChangeEntry changeEntry) {
        mUndoStack.pop();
        mUndoStack.push(changeEntry);
    }

    protected boolean tryMergeChange(ChangeEntry changeEntry) {
        Optional<ChangeEntry> oRecentChange = tryPeekNextUndoChange();
        if (oRecentChange.isPresent()) {
            ChangeEntry rc = oRecentChange.get();
            Optional<ChangeEntry> oMerged = changeEntry.tryMerge(rc);
            if (oMerged.isPresent()) {
                exchangeRecentChange(oMerged.get());
                return true;
            }
        }
        return false;
    }

    public boolean canUndo() {
        return !mUndoStack.empty();
    }

    public ChangeEntry undo() {
        if (mUndoStack.empty()) {
            throw new IllegalStateException("No changes in history to undo");
        }
        ChangeEntry change = mUndoStack.pop();
        ChangeEntry undoneChange = change.undo();
        mRedoStack.push(undoneChange);
        return undoneChange;
    }

    public boolean canRedo() {
        return !mRedoStack.empty();
    }

    public ChangeEntry redo() {
        if (mRedoStack.empty()) {
            throw new IllegalStateException("No changes in history to redo");
        }
        ChangeEntry change = mRedoStack.pop();
        ChangeEntry redoneChange = change.undo();
        mUndoStack.push(redoneChange);
        return redoneChange;
    }

    public void clear() {
        mUndoStack.clear();
        mRedoStack.clear();
    }

    public Optional<ChangeEntry> tryPeekNextUndoChange() {
        return mUndoStack.isEmpty() ? Optional.empty() : Optional.of(mUndoStack.peek());
    }

    public Optional<ChangeEntry> tryPeekNextRedoChange() {
        return mRedoStack.isEmpty() ? Optional.empty() : Optional.of(mRedoStack.peek());
    }
}
