package org.reactfx.collection;

import java.util.List;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;

import org.reactfx.Subscription;
import org.reactfx.util.Lists;
import org.reactfx.util.SparseList;

public interface MemoizationList<E> extends LiveList<E> {
    LiveList<E> memoizedItems();
    boolean isMemoized(int index);
    Optional<E> getIfMemoized(int index);
    int getMemoizedCount();
    int getMemoizedCountBefore(int position);
    int getMemoizedCountAfter(int position);
    void forget(int from, int to);
    int indexOfMemoizedItem(int index);
    IndexRange getMemoizedItemsRange();
    void force(int from, int to);
}

class MemoizationListImpl<E>
extends LiveListBase<E>
implements MemoizationList<E>, UnmodifiableByDefaultLiveList<E> {

    private class MemoizedView
    extends LiveListBase<E>
    implements UnmodifiableByDefaultLiveList<E> {

        @Override
        protected Subscription observeInputs() {
            return MemoizationListImpl.this.pin();
        }

        @Override
        public E get(int index) {
            return sparseList.getPresent(index);
        }

        @Override
        public int size() {
            return sparseList.getPresentCount();
        }

        private void prepareNotifications(QuasiListChange<? extends E> change) {
            enqueueNotifications(change);
        }

        private void prepareNotifications(QuasiListModification<? extends E> mod) {
            enqueueNotifications(mod.asListChange());
        }

        private void publishNotifications() {
            notifyObservers();
        }
    }

    private final SparseList<E> sparseList = new SparseList<>();
    private final MemoizedView memoizedItems = new MemoizedView();
    private final ObservableList<E> source;

    MemoizationListImpl(ObservableList<E> source) {
        this.source = source;
    }

    @Override
    protected Subscription observeInputs() {
        sparseList.insertVoid(0, source.size());
        return LiveList.<E>observeQuasiChanges(source, this::sourceChanged)
            .and(sparseList::clear);
    }

    private void sourceChanged(QuasiListChange<? extends E> qc) {
        ListChangeAccumulator<E> acc = new ListChangeAccumulator<>();
        for(QuasiListModification<? extends E> mod: qc) {
            int from = mod.getFrom();
            int removedSize = mod.getRemovedSize();
            int memoFrom = sparseList.getPresentCountBefore(from);
            List<E> memoRemoved = sparseList.collect(from, from + removedSize);
            sparseList.spliceByVoid(from, from + removedSize, mod.getAddedSize());
            acc.add(new QuasiListModificationImpl<>(memoFrom, memoRemoved, 0));
        }
        memoizedItems.prepareNotifications(acc.fetch());
        notifyObservers(qc);
        memoizedItems.publishNotifications();
    }

    @Override
    public E get(int index) {
        if(!isObservingInputs()) { // memoization is off
            return source.get(index);
        } else if(sparseList.isPresent(index)) {
            return sparseList.getOrThrow(index);
        } else {
            E elem = source.get(index); // may cause recursive get(), so we
                                        // need to check again for absence
            if(sparseList.setIfAbsent(index, elem)) {
                memoizedItems.fireElemInsertion(
                        sparseList.getPresentCountBefore(index));
            }
            return elem;
        }
    }

    @Override
    public void force(int from, int to) {
        if(!isObservingInputs()) { // memoization is off
            throw new IllegalStateException(
                    "Cannot force items when memoization is off."
                    + " To turn memoization on, you have to be observing this"
                    + " list or its memoizedItems.");
        }

        Lists.checkRange(from, to, size());
        for(int i = from; i < to; ++i) {
            if(!sparseList.isPresent(i)) {
                E elem = source.get(i);
                if(sparseList.setIfAbsent(i, elem)) {
                    int presentBefore = sparseList.getPresentCountBefore(i);
                    memoizedItems.prepareNotifications(ProperLiveList.elemInsertion(presentBefore));
                }
            }
        }
        memoizedItems.publishNotifications();
    }

    @Override
    public int size() {
        return source.size();
    }

    @Override
    public LiveList<E> memoizedItems() {
        return memoizedItems;
    }

    @Override
    public boolean isMemoized(int index) {
        return isObservingInputs() && sparseList.isPresent(index);
    }

    @Override
    public Optional<E> getIfMemoized(int index) {
        Lists.checkIndex(index, size());
        return isObservingInputs()
                ? sparseList.get(index)
                : Optional.empty();
    }

    @Override
    public int getMemoizedCountBefore(int position) {
        Lists.checkPosition(position, size());
        return isObservingInputs()
                ? sparseList.getPresentCountBefore(position)
                : 0;
    }

    @Override
    public int getMemoizedCountAfter(int position) {
        Lists.checkPosition(position, size());
        return isObservingInputs()
                ? sparseList.getPresentCountAfter(position)
                : 0;
    }

    @Override
    public int getMemoizedCount() {
        return memoizedItems.size();
    }

    @Override
    public void forget(int from, int to) {
        if(!isObservingInputs()) { // memoization is off
            throw new IllegalStateException(
                    "There is nothing to forget, because memoization is off."
                    + " To turn memoization on, you have to be observing this"
                    + " list or its memoizedItems.");
        }

        Lists.checkRange(from, to, size());
        int memoChangeFrom = sparseList.getPresentCountBefore(from);
        List<E> memoRemoved = sparseList.collect(from, to);
        sparseList.spliceByVoid(from, to, to - from);
        memoizedItems.fireRemoveRange(memoChangeFrom, memoRemoved);
    }

    @Override
    public int indexOfMemoizedItem(int index) {
        return sparseList.indexOfPresentItem(index);
    }

    @Override
    public IndexRange getMemoizedItemsRange() {
        return sparseList.getPresentItemsRange();
    }
}