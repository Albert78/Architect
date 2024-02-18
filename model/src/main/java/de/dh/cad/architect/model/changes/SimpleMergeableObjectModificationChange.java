package de.dh.cad.architect.model.changes;

import java.util.Collection;
import java.util.Optional;

import de.dh.cad.architect.model.objects.BaseObject;

/**
 * Change class to be overridden for typical simple object modifications.
 * This type of change works if multiple successive calls to a simple setter method with different values yield the same
 * result as a single call to that setter with the last value.
 *
 * This class implements the {@link #tryMerge(IModelChange)} method in a trivial way which means that the merge of multiple successive changes
 * of the same kind will just yield the last change of that sequence.
 *
 * Example:
 * <pre>
 * <code>
 *   public void setHeight(Length value, List<IModelChange> changeTrace) {
 *       Length oldHeight = mHeight;
 *       mHeight = value;
 *       changeTrace.add(new SimpleMergeableObjectModificationChange(this) {
 *           @Override
 *           public void undo(List<IModelChange> undoChangeTrace) {
 *               setHeight(oldHeight, undoChangeTrace);
 *           }
 *       });
 *   }
 * </code>
 * </pre>
 */
public abstract class SimpleMergeableObjectModificationChange extends ObjectModificationChange {
    protected SimpleMergeableObjectModificationChange(Collection<BaseObject> targetObjects) {
        super(targetObjects);
    }

    protected SimpleMergeableObjectModificationChange(BaseObject... targetObjects) {
        super(targetObjects);
    }

    @Override
    public Optional<IModelChange> tryMerge(IModelChange oldChange) {
        if (!sameKindOfChange(oldChange)) {
            return Optional.empty();
        }
        return Optional.of(oldChange);
    }
}
