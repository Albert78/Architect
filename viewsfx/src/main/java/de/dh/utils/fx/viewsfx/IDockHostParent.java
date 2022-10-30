package de.dh.utils.fx.viewsfx;

/**
 * Splitter dock host or root = dock host control.
 */
public interface IDockHostParent {
    /**
     * Tells this parent that the given child is invalid, i.e. doesn't have any more valid content to
     * show. This parent will decide if the child should be removed or retained.
     */
    void invalidateLeaf(TabDockHost child);

    /**
     * Tells this parent that the given {@code obsoleteSplitter} is not necessary any more because
     * it only has a single child left. This method maks this {@code moveUpChild} replace the
     * {@code obsoleteSplitter}.
     */
    void compressDockHierarchy(SplitterDockHost obsoleteSplitter, IDockZone moveUpChild);

    /**
     * Tells this parent to replace the given {@code replaceChild} with a {@link SplitterDockHost}.
     */
    SplitterDockHost replaceWithSplitter(IDockZone replaceChild);
}
