package org.reactfx.util;

import static org.reactfx.util.Either.*;
import static org.reactfx.util.LL.*;
import static org.reactfx.util.Tuples.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.reactfx.util.LL.Cons;

public abstract class FingerTree<T, S> {

    public static abstract class NonEmptyFingerTree<T, S> extends FingerTree<T, S> {

        private NonEmptyFingerTree(ToSemigroup<? super T, S> semigroup) {
            super(semigroup);
        }

        @Override
        public Either<FingerTree<T, S>, NonEmptyFingerTree<T, S>> caseEmpty() {
            return right(this);
        }

        public abstract S getSummary();

        @Override
        public BiIndex locate(
                BiFunction<? super S, Integer, Either<Integer, Integer>> navigate,
                int position) {
            if(navigate.apply(getSummary(), position).isRight()) {
                throw new IndexOutOfBoundsException("Position " + position + " is out of bounds");
            }
            return locate0(navigate, position);
        }

        @Override
        public BiIndex locateProgressively(
                ToIntFunction<? super S> metric,
                int position) {
            Lists.checkPosition(position, measure(metric));
            return locateProgressively0(metric, position);
        }

        @Override
        public BiIndex locateRegressively(
                ToIntFunction<? super S> metric,
                int position) {
            Lists.checkPosition(position, measure(metric));
            return locateRegressively0(metric, position);
        }

        public Tuple3<FingerTree<T, S>, T, FingerTree<T, S>> splitAt(int leaf) {
            Lists.checkIndex(leaf, getLeafCount());
            return split0(leaf).map((l, r0) ->
                   r0.split0(1).map((m, r) ->
                           t(l, m.getLeaf0(0), r)));
        }

        public Tuple3<FingerTree<T, S>, Tuple2<T, Integer>, FingerTree<T, S>> split(
                ToIntFunction<? super S> metric, int position) {
            Lists.checkPosition(position, measure(metric));
            return split((s, i) -> {
                int n = metric.applyAsInt(s);
                return i <= n ? left(i) : right(i - n);
            }, position);
        }

        public Tuple3<FingerTree<T, S>, Tuple2<T, Integer>, FingerTree<T, S>> split(
                BiFunction<? super S, Integer, Either<Integer, Integer>> navigate,
                int position) {
            if(navigate.apply(getSummary(), position).isRight()) {
                throw new IndexOutOfBoundsException("Position " + position + " is out of bounds");
            }

            BiIndex loc = locate0(navigate, position);
            return splitAt(loc.major).map((l, m, r) -> t(l, t(m, loc.minor), r));
        }

        @Override
        public NonEmptyFingerTree<T, S> join(FingerTree<T, S> rightTree) {
            return appendTree(rightTree);
        }

        final NonEmptyFingerTree<T, S> appendTree(FingerTree<T, S> right) {
            if(this.getDepth() >= right.getDepth()) {
                return appendLte(right).unify(
                        Function.identity(),
                        two -> two.map(FingerTree::branch));
            } else {
                return ((NonEmptyFingerTree<T, S>) right).prependTree(this);
            }
        }

        final NonEmptyFingerTree<T, S> prependTree(FingerTree<T, S> left) {
            if(this.getDepth() >= left.getDepth()) {
                return prependLte(left).unify(
                        Function.identity(),
                        two -> two.map(FingerTree::branch));
            } else {
                return ((NonEmptyFingerTree<T, S>) left).appendTree(this);
            }
        }

        abstract BiIndex locate0(
                BiFunction<? super S, Integer, Either<Integer, Integer>> navigate,
                int position);

        abstract BiIndex locateProgressively0(
                ToIntFunction<? super S> metric,
                int position);

        abstract BiIndex locateRegressively0(
                ToIntFunction<? super S> metric,
                int position);

        abstract Either<? extends NonEmptyFingerTree<T, S>, Tuple2<NonEmptyFingerTree<T, S>, NonEmptyFingerTree<T, S>>> appendLte(FingerTree<T, S> right);
        abstract Either<? extends NonEmptyFingerTree<T, S>, Tuple2<NonEmptyFingerTree<T, S>, NonEmptyFingerTree<T, S>>> prependLte(FingerTree<T, S> left);
    }

    private static final class Empty<T, S> extends FingerTree<T, S> {

        Empty(ToSemigroup<? super T, S> semigroup) {
            super(semigroup);
        }

        @Override
        public String toString() {
            return"<emtpy tree>";
        }

        @Override
        public Either<FingerTree<T, S>, NonEmptyFingerTree<T, S>> caseEmpty() {
            return left(this);
        }

        @Override
        public
        int getDepth() {
            return 0;
        }

        @Override
        public
        int getLeafCount() {
            return 0;
        }

        @Override
        public FingerTree<T, S> join(FingerTree<T, S> rightTree) {
            return rightTree;
        }

        @Override
        public List<T> asList() {
            return Collections.emptyList();
        }

        @Override
        T getLeaf0(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        NonEmptyFingerTree<T, S> updateLeaf0(int index, T data) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        T getData() {
            throw new NoSuchElementException();
        }

        @Override
        public
        Optional<S> getSummaryOpt() {
            return Optional.empty();
        }

        @Override
        public <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return acc;
        }

        @Override
        <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf, int endLeaf) {
            assert Lists.isValidRange(startLeaf, endLeaf, 0);
            return acc;
        }

        @Override
        <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {
            assert Lists.isValidRange(startPosition, endPosition, 0);
            return acc;
        }

        @Override
        S getSummaryBetween0(int startLeaf, int endLeaf) {
            throw new AssertionError("Unreachable code");
        }

        @Override
        S getSummaryBetween0(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subSummary) {
            throw new AssertionError("Unreachable code");
        }

        @Override
        Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(
                int beforeLeaf) {
            assert beforeLeaf == 0;
            return t(this, this);
        }
    }

    private static class Leaf<T, S> extends NonEmptyFingerTree<T, S> {
        private final T data;
        private final S summary;

        Leaf(ToSemigroup<? super T, S> semigroup, T data) {
            super(semigroup);
            this.data = data;
            this.summary = semigroup.apply(data);
        }

        @Override
        public String toString() {
            return "Leaf(" + data + ")";
        }

        @Override
        public int getDepth() {
            return 1;
        }

        @Override
        public int getLeafCount() {
            return 1;
        }

        @Override
        public List<T> asList() {
            return Collections.singletonList(data);
        }

        @Override
        T getLeaf0(int index) {
            assert index == 0;
            return data;
        }

        @Override
        NonEmptyFingerTree<T, S> updateLeaf0(int index, T data) {
            assert index == 0;
            return leaf(data);
        }

        @Override
        T getData() {
            return data;
        }

        @Override
        public S getSummary() {
            return summary;
        }

        @Override
        public Optional<S> getSummaryOpt() {
            return Optional.of(summary);
        }

        @Override
        BiIndex locateProgressively0(ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            return new BiIndex(0, position);
        }

        @Override
        BiIndex locateRegressively0(ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            return new BiIndex(0, position);
        }

        @Override
        public <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return reduction.apply(acc, data);
        }

        @Override
        <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf, int endLeaf) {

            assert 0 <= startLeaf;
            assert endLeaf <= 1;

            if(startLeaf < endLeaf) {
                return reduction.apply(acc, data);
            } else {
                return acc;
            }
        }

        @Override
        <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {

            assert Lists.isValidRange(startPosition, endPosition, measure(metric));
            return rangeReduction.apply(acc, data, startPosition, endPosition);
        }

        @Override
        S getSummaryBetween0(int startLeaf, int endLeaf) {
            assert startLeaf == 0 && endLeaf == 1;
            return summary;
        }

        @Override
        S getSummaryBetween0(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subSummary) {

            assert Lists.isNonEmptyRange(startPosition, endPosition, measure(metric))
                    : "Didn't expect empty range [" + startPosition + ", " + endPosition + ")";

            if(startPosition == 0 && endPosition == measure(metric)) {
                return summary;
            } else {
                return subSummary.apply(data, startPosition, endPosition);
            }
        }

        @Override
        Either<Leaf<T, S>, Tuple2<NonEmptyFingerTree<T, S>, NonEmptyFingerTree<T, S>>> appendLte(
                FingerTree<T, S> right) {
            assert right.getDepth() <= this.getDepth();
            return right.caseEmpty()
                    .mapLeft(emptyRight -> this)
                    .mapRight(nonEmptyRight -> t(this, nonEmptyRight));
        }

        @Override
        Either<Leaf<T, S>, Tuple2<NonEmptyFingerTree<T, S>, NonEmptyFingerTree<T, S>>> prependLte(
                FingerTree<T, S> left) {
            assert left.getDepth() <= this.getDepth();
            return left.caseEmpty()
                    .mapLeft(emptyLeft -> this)
                    .mapRight(nonEmptyLeft -> t(nonEmptyLeft, this));
        }

        @Override
        Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(int beforeLeaf) {
            assert Lists.isValidPosition(beforeLeaf, 1);
            if(beforeLeaf == 0) {
                return t(empty(), this);
            } else {
                return t(this, empty());
            }
        }

        @Override
        BiIndex locate0(
                BiFunction<? super S, Integer, Either<Integer, Integer>> navigate,
                int position) {
            return navigate.apply(summary,  position).unify(
                    inl -> new BiIndex(0, inl),
                    inr -> { throw new AssertionError("Unreachable code"); });
        }
    }

    private static final class Branch<T, S> extends NonEmptyFingerTree<T, S> {
        private final Cons<NonEmptyFingerTree<T, S>> children;
        private final int depth;
        private final int leafCount;
        private final S summary;

        private Branch(Cons<NonEmptyFingerTree<T, S>> children) {
            super(children.head().semigroup);
            assert children.size() == 2 || children.size() == 3;
            FingerTree<T, S> head = children.head();
            int headDepth = head.getDepth();
            assert children.all(n -> n.getDepth() == headDepth);
            this.children = children;
            this.depth  = 1 + headDepth;
            this.leafCount = children.fold(0, (s, n) -> s + n.getLeafCount());
            this.summary = children.mapReduce1(
                    NonEmptyFingerTree<T, S>::getSummary,
                    semigroup::reduce);
        }

        @Override
        public String toString() {
            return "Branch" + children;
        }

        @Override
        public int getDepth() {
            return depth;
        }

        @Override
        public int getLeafCount() {
            return leafCount;
        }

        @Override
        public List<T> asList()  {
            return subList0(0, leafCount);
        }

        /**
         * Complexity of calling once: O(log(n)).
         * Complexity of calling subList recursively on the resulting list,
         * i.e. {@code tree.asList().subList(...).subList(...). ... .subList(...)}
         * up to n times: O(n).
         * When the resulting list has size m, it may prevent up to m additional
         * elements of the original tree from being garbage collected.
         */
        private List<T> subList(int from, int to) {
            int len = to - from;

            if(2*len >= getLeafCount()) {
                return subList0(from, to);
            } else {
                FingerTree<T, S> tree = this;
                if(2*(getLeafCount() - to) > len) {
                    tree = tree.split(to)._1;
                }
                if(2*from > len) {
                    tree = tree.split(from)._2;
                    to -= from;
                    from = 0;
                }
                return tree.asList().subList(from, to);
            }
        }

        private List<T> subList0(int from, int to) {
            return new AbstractList<T>() {

                @Override
                public T get(int index) {
                    Lists.checkIndex(index, to - from);
                    return getLeaf(from + index);
                }

                @Override
                public int size() {
                    return to - from;
                }

                @Override
                public List<T> subList(int start, int end) {
                    Lists.checkRange(start, end, to - from);
                    return Branch.this.subList(from + start, from + end);
                }

                @Override
                public Iterator<T> iterator() {
                    return listIterator(0);
                }

                /**
                 * Iterates in both directions in time O(n),
                 * with at most O(log(n)) allocations (as the stack expands).
                 */
                @Override
                public ListIterator<T> listIterator(int pos) {
                    final int dd = 3; // maximum depth for which we directly call get(idx)
                    Lists.checkPosition(pos, size());
                    return new ListIterator<T>() {
                        private int position = from + pos; // position within this finger tree
                        private int topOffset = 0; // absolute offset of top of the stack relative to this finger tree
                        private Stack<NonEmptyFingerTree<T, S>> stack = new Stack<>();
                        { stack.push(Branch.this); }

                        @Override public boolean hasNext() { return position < to; }
                        @Override public boolean hasPrevious() { return position > from; }
                        @Override public int nextIndex() { return position - from; }
                        @Override public int previousIndex() { return position - from - 1; }

                        @Override public void remove() { throw new UnsupportedOperationException(); }
                        @Override public void set(T e) { throw new UnsupportedOperationException(); }
                        @Override public void add(T e) { throw new UnsupportedOperationException(); }

                        @Override
                        public T next() {
                            if(position == topOffset + stack.peek().getLeafCount()) {
                                up();
                                return next();
                            } else if(stack.peek().getDepth() <= dd) {
                                return stack.peek().getLeaf(position++ - topOffset);
                            } else {
                                downR();
                                return next();
                            }
                        }

                        @Override
                        public T previous() {
                            if(position == topOffset) {
                                up();
                                return previous();
                            } else if(stack.peek().getDepth() <= dd) {
                                return stack.peek().getLeaf(--position - topOffset);
                            } else {
                                downL();
                                return previous();
                            }
                        }

                        private void up() {
                            NonEmptyFingerTree<T, S> child = stack.pop();
                            Branch<T, S> top = (Branch<T, S>) stack.peek();
                            int chOffsetInParent = 0;
                            LL<? extends NonEmptyFingerTree<T, S>> children = top.children;
                            while(children.head() != child) {
                                chOffsetInParent += children.head().getLeafCount();
                                children = children.tail();
                            }
                            topOffset -= chOffsetInParent;
                        }

                        private void downR() {
                            downR(((Branch<T, S>) stack.peek()).children);
                        }

                        private void downR(LL<? extends NonEmptyFingerTree<T, S>> children) {
                            NonEmptyFingerTree<T, S> head = children.head();
                            if(position - topOffset < head.getLeafCount()) {
                                stack.push(head);
                            } else {
                                topOffset += head.getLeafCount();
                                downR(children.tail());
                            }
                        }

                        private void downL() {
                            downL(((Branch<T, S>) stack.peek()).children);
                        }

                        private void downL(LL<? extends NonEmptyFingerTree<T, S>> children) {
                            NonEmptyFingerTree<T, S> head = children.head();
                            if(position - topOffset <= head.getLeafCount()) {
                                stack.push(head);
                            } else {
                                topOffset += head.getLeafCount();
                                downL(children.tail());
                            }
                        }
                    };
                }
            };
        }

        @Override
        final T getData() {
            throw new UnsupportedOperationException("Only leaf nodes hold data");
        }

        @Override
        final T getLeaf0(int index) {
            assert Lists.isValidIndex(index, getLeafCount());
            return getLeaf0(index, children);
        }

        private T getLeaf0(int index, LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(index < headSize) {
                return head.getLeaf0(index);
            } else {
                return getLeaf0(index - headSize, nodes.tail());
            }
        }

        @Override
        NonEmptyFingerTree<T, S> updateLeaf0(int index, T data) {
            assert Lists.isValidIndex(index, getLeafCount());
            return branch(updateLeaf0(index, data, children));
        }

        private Cons<NonEmptyFingerTree<T, S>> updateLeaf0(
                int index, T data, LL<? extends NonEmptyFingerTree<T, S>> nodes) {
            NonEmptyFingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(index < headSize) {
                return cons(head.updateLeaf0(index, data), nodes.tail());
            } else {
                return cons(head, updateLeaf0(index - headSize, data, nodes.tail()));
            }
        }

        @Override
        final BiIndex locateProgressively0(ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            return locateProgressively0(metric, position, children);
        }

        private BiIndex locateProgressively0(
                ToIntFunction<? super S> metric,
                int position,
                LL<? extends NonEmptyFingerTree<T, S>> nodes) {
            NonEmptyFingerTree<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            if(position < headLen ||
                    (position == headLen && nodes.tail().isEmpty())) {
                return head.locateProgressively0(metric, position);
            } else {
                return locateProgressively0(metric, position - headLen, nodes.tail())
                        .adjustMajor(head.getLeafCount());
            }
        }

        @Override
        final BiIndex locateRegressively0(ToIntFunction<? super S> metric, int position) {
            assert Lists.isValidPosition(position, measure(metric));
            return locateRegressively0(metric, position, children);
        }

        private BiIndex locateRegressively0(
                ToIntFunction<? super S> metric,
                int position,
                LL<? extends NonEmptyFingerTree<T, S>> nodes) {
            NonEmptyFingerTree<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            if(position <= headLen) {
                return head.locateRegressively0(metric, position);
            } else {
                return locateRegressively0(metric, position - headLen, nodes.tail())
                        .adjustMajor(head.getLeafCount());
            }
        }

        @Override
        public final <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return children.fold(acc, (r, n) -> n.fold(r, reduction));
        }

        @Override
        final <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf,
                int endLeaf) {
            assert Lists.isNonEmptyRange(startLeaf, endLeaf, getLeafCount());
            return foldBetween0(acc, reduction, startLeaf, endLeaf, children);
        }

        private <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                int startLeaf,
                int endLeaf,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            int headTo = Math.min(endLeaf, headSize);
            int tailFrom = Math.max(startLeaf - headSize, 0);
            int tailTo = endLeaf - headSize;
            if(startLeaf < headTo) {
                acc = head.foldBetween0(acc, reduction, startLeaf, headTo);
            }
            if(tailFrom < tailTo) {
                acc = foldBetween0(acc, reduction, tailFrom, tailTo, nodes.tail());
            }
            return acc;
        }

        @Override
        final <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {
            assert Lists.isNonEmptyRange(startPosition, endPosition, measure(metric));
            return foldBetween0(acc, reduction, metric, startPosition, endPosition, rangeReduction, children);
        }

        private <R> R foldBetween0(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction,
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            int headTo = Math.min(endPosition, headLen);
            int tailFrom = Math.max(startPosition - headLen, 0);
            int tailTo = endPosition - headLen;
            if(startPosition < headTo) {
                acc = head.foldBetween0(acc, reduction, metric, startPosition, headTo, rangeReduction);
            }
            if(tailFrom < tailTo) {
                acc = foldBetween0(acc, reduction, metric, tailFrom, tailTo, rangeReduction, nodes.tail());
            }
            return acc;
        }

        @Override
        public S getSummary() {
            return summary;
        }

        @Override
        public Optional<S> getSummaryOpt() {
            return Optional.of(summary);
        }

        @Override
        final S getSummaryBetween0(int startLeaf, int endLeaf) {
            assert Lists.isNonEmptyRange(startLeaf, endLeaf, getLeafCount());
            if(startLeaf == 0 && endLeaf == getLeafCount()) {
                return summary;
            } else {
                return getSummaryBetween0(startLeaf, endLeaf, children);
            }
        }

        private S getSummaryBetween0(
                int startLeaf,
                int endLeaf,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            int headTo = Math.min(endLeaf, headSize);
            int tailFrom = Math.max(startLeaf - headSize, 0);
            int tailTo = endLeaf - headSize;
            if(startLeaf < headTo && tailFrom < tailTo) {
                return semigroup.reduce(
                        head.getSummaryBetween0(startLeaf, headTo),
                        getSummaryBetween0(tailFrom, tailTo, nodes.tail()));
            } else if(startLeaf < headTo) {
                return head.getSummaryBetween0(startLeaf, headTo);
            } else if(tailFrom < tailTo) {
                return getSummaryBetween0(tailFrom, tailTo, nodes.tail());
            } else {
                throw new AssertionError("Didn't expect empty range: "
                        + "[" + startLeaf + ", " + endLeaf + ")");
            }
        }

        @Override
        final S getSummaryBetween0(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subSummary) {
            int len = measure(metric);
            assert Lists.isNonEmptyRange(startPosition, endPosition, len);
            if(startPosition == 0 && endPosition == len) {
                return getSummary();
            } else {
                return getSummaryBetween0(metric, startPosition, endPosition, subSummary, children);
            }
        }

        private S getSummaryBetween0(
                ToIntFunction<? super S> metric,
                int startPosition,
                int endPosition,
                TriFunction<? super T, Integer, Integer, ? extends S> subSummary,
                LL<? extends FingerTree<T, S>> nodes) {
            FingerTree<T, S> head = nodes.head();
            int headLen = head.measure(metric);
            int headTo = Math.min(endPosition, headLen);
            int tailFrom = Math.max(startPosition - headLen, 0);
            int tailTo = endPosition - headLen;
            if(startPosition < headTo && tailFrom < tailTo) {
                return semigroup.reduce(
                        head.getSummaryBetween0( metric, startPosition, headTo, subSummary),
                        getSummaryBetween0(metric, tailFrom, tailTo, subSummary, nodes.tail()));
            } else if(startPosition < headTo) {
                return head.getSummaryBetween0(metric, startPosition, headTo, subSummary);
            } else if(tailFrom < tailTo) {
                return getSummaryBetween0(metric, tailFrom, tailTo, subSummary, nodes.tail());
            } else {
                throw new AssertionError("Didn't expect empty range: [" + startPosition + ", " + endPosition + ")");
            }
        }

        @Override
        Either<Branch<T, S>, Tuple2<NonEmptyFingerTree<T, S>, NonEmptyFingerTree<T, S>>> appendLte(
                FingerTree<T, S> suffix) {
            assert suffix.getDepth() <= this.getDepth();
            if(suffix.getDepth() == this.getDepth()) {
                return right(t(this, (NonEmptyFingerTree<T, S>) suffix));
            } else if(children.size() == 2) {
                return children.mapFirst2((left, right) -> {
                    return right.appendLte(suffix).unify(
                            r -> left(branch(left, r)),
                            mr -> left(mr.map((m, r) -> branch(left, m, r))));
                });
            } else {
                assert children.size() == 3;
                return children.mapFirst3((left, middle, right) -> {
                    return right.appendLte(suffix)
                            .mapLeft(r -> branch(left, middle, r))
                            .mapRight(mr -> t(branch(left, middle), mr.map(FingerTree::branch)));
                });
            }
        }

        @Override
        Either<Branch<T, S>, Tuple2<NonEmptyFingerTree<T, S>, NonEmptyFingerTree<T, S>>> prependLte(
                FingerTree<T, S> prefix) {
            assert prefix.getDepth() <= this.getDepth();
            if(prefix.getDepth() == this.getDepth()) {
                return right(t((NonEmptyFingerTree<T, S>) prefix, this));
            } else if(children.size() == 2) {
                return children.mapFirst2((left, right) -> {
                    return left.prependLte(prefix).unify(
                            l -> left(branch(l, right)),
                            lm -> left(lm.map((l, m) -> branch(l, m, right))));
                });
            } else {
                assert children.size() == 3;
                return children.mapFirst3((left, middle, right) -> {
                    return left.prependLte(prefix)
                            .mapLeft(l -> branch(l, middle, right))
                            .mapRight(lm -> t(lm.map(FingerTree::branch), branch(middle, right)));
                });
            }
        }

        @Override
        Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(int beforeLeaf) {
            assert Lists.isValidPosition(beforeLeaf, getLeafCount());
            if(beforeLeaf == 0) {
                return t(empty(), this);
            } else {
                return split0(beforeLeaf, children);
            }
        }

        private Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(
                int beforeLeaf, LL<? extends FingerTree<T, S>> nodes) {
            assert beforeLeaf > 0;
            FingerTree<T, S> head = nodes.head();
            int headSize = head.getLeafCount();
            if(beforeLeaf <= headSize) {
                return head.split0(beforeLeaf)
                        .map((l, r) -> t(l, concat(cons(r, nodes.tail()))));
            } else {
                return split0(beforeLeaf - headSize, nodes.tail())
                        .map((l, r) -> t(head.join(l), r));
            }
        }

        @Override
        BiIndex locate0(
                BiFunction<? super S, Integer, Either<Integer, Integer>> navigate,
                int position) {
            assert navigate.apply(summary, position).isLeft();
            return locate0(navigate, position, children);
        }

        private BiIndex locate0(
                BiFunction<? super S, Integer, Either<Integer, Integer>> navigate,
                int position,
                LL<? extends NonEmptyFingerTree<T, S>> nodes) {
            NonEmptyFingerTree<T, S> head = nodes.head();
            return navigate.apply(head.getSummary(), position).unify(
                    posInl -> head.locate0(navigate, posInl),
                    posInr -> locate0(navigate, posInr, nodes.tail())
                                  .adjustMajor(head.getLeafCount()));
        }
    }

    public static <T, S> FingerTree<T, S> empty(
            ToSemigroup<? super T, S> statisticsProvider) {
        return new Empty<>(statisticsProvider);
    }

    public static <T> FingerTree<T, Void> mkTree(List<? extends T> items) {
        return mkTree(items, new ToSemigroup<T, Void>() {
            @Override public Void apply(T t) { return null; }
            @Override public Void reduce(Void left, Void right) { return null; }
        });
    }

    public static <T, S> FingerTree<T, S> mkTree(
            List<? extends T> items,
            ToSemigroup<? super T, S> summaryProvider) {

        if(items.isEmpty()) {
            return new Empty<>(summaryProvider);
        }

        ArrayList<NonEmptyFingerTree<T, S>> trees = new ArrayList<>(items.size());
        for(T item: items) {
            trees.add(new Leaf<T, S>(summaryProvider, item));
        }

        while(trees.size() > 1) {
            final int n = trees.size();
            int src = 0;
            int tgt = 0;
            while(src < n) {
                if(n - src >= 5 || n - src == 3) {
                    NonEmptyFingerTree<T, S> t1 = trees.get(src++);
                    NonEmptyFingerTree<T, S> t2 = trees.get(src++);
                    NonEmptyFingerTree<T, S> t3 = trees.get(src++);
                    Branch<T, S> t = branch(t1, t2, t3);
                    trees.set(tgt++, t);
                } else { // (n - i) is 4 or 2
                    NonEmptyFingerTree<T, S> t1 = trees.get(src++);
                    NonEmptyFingerTree<T, S> t2 = trees.get(src++);
                    Branch<T, S> t = branch(t1, t2);
                    trees.set(tgt++, t);
                }
            }
            trees.subList(tgt, n).clear();
        }

        return trees.get(0);
    }

    private static <T, S> Branch<T, S> branch(NonEmptyFingerTree<T, S> left, NonEmptyFingerTree<T, S> right) {
        return branch(LL.of(left, right));
    }

    private static <T, S> Branch<T, S> branch(
            NonEmptyFingerTree<T, S> left,
            NonEmptyFingerTree<T, S> middle,
            NonEmptyFingerTree<T, S> right) {
        return branch(LL.of(left, middle, right));
    }

    private static <T, S> Branch<T, S> branch(Cons<NonEmptyFingerTree<T, S>> children) {
        return new Branch<>(children);
    }

    private static <T, S> FingerTree<T, S> concat(
            Cons<? extends FingerTree<T, S>> nodes) {
        FingerTree<T, S> head = nodes.head();
        return nodes.tail().fold(
                head,
                FingerTree::join);
    }

    final ToSemigroup<? super T, S> semigroup;

    private FingerTree(ToSemigroup<? super T, S> semigroup) {
        this.semigroup = semigroup;
    }

    public abstract int getDepth();
    public abstract int getLeafCount();
    public abstract Optional<S> getSummaryOpt();
    public abstract Either<FingerTree<T, S>, NonEmptyFingerTree<T, S>> caseEmpty();

    public final boolean isEmpty() {
        return getDepth() == 0;
    }

    public S getSummary(S whenEmpty) {
        return getSummaryOpt().orElse(whenEmpty);
    }

    public T getLeaf(int index) {
        Lists.checkIndex(index, getLeafCount());
        return getLeaf0(index);
    }

    abstract T getLeaf0(int index);

    public Tuple2<T, BiIndex> get(
            ToIntFunction<? super S> metric,
            int index) {
        return caseEmpty().unify(
                emptyTree -> { throw new IndexOutOfBoundsException("empty tree"); },
                neTree -> {
                    int size = metric.applyAsInt(neTree.getSummary());
                    Lists.checkIndex(index, size);
                    BiIndex location = locateProgressively(metric, index);
                    return t(getLeaf(location.major), location);
                });
    }

    public <E> E get(
            ToIntFunction<? super S> metric,
            int index,
            BiFunction<? super T, Integer, ? extends E> leafAccessor) {
        return locateProgressively(metric, index)
                .map((major, minor) -> leafAccessor.apply(getLeaf(major), minor));
    }

    public NonEmptyFingerTree<T, S> updateLeaf(int index, T data) {
        Lists.checkIndex(index, getLeafCount());
        return updateLeaf0(index, data);
    }

    abstract NonEmptyFingerTree<T, S> updateLeaf0(int index, T data);

    public BiIndex locate(
            BiFunction<? super S, Integer, Either<Integer, Integer>> navigate,
            int position) {

        return caseEmpty().unify(
                emptyTree -> { throw new IndexOutOfBoundsException("no leafs to locate in"); },
                neTree -> { throw new AssertionError("This method must be overridden in non-empty tree"); });
    }

    public BiIndex locateProgressively(
            ToIntFunction<? super S> metric,
            int position) {

        return caseEmpty().unify(
                emptyTree -> { throw new IndexOutOfBoundsException("no leafs to locate in"); },
                neTree -> { throw new AssertionError("This method must be overridden in non-empty tree"); });
    }

    public BiIndex locateRegressively(
            ToIntFunction<? super S> metric,
            int position) {

        return caseEmpty().unify(
                emptyTree -> { throw new IndexOutOfBoundsException("no leafs to locate in"); },
                neTree -> { throw new AssertionError("This method must be overridden in non-empty tree"); });
    }

    public abstract <R> R fold(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction);

    public <R> R foldBetween(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            int startLeaf,
            int endLeaf) {
        Lists.checkRange(startLeaf, endLeaf, getLeafCount());
        if(startLeaf == endLeaf) {
            return acc;
        } else {
            return foldBetween0(acc, reduction, startLeaf, endLeaf);
        }
    }

    abstract <R> R foldBetween0(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            int startLeaf, int endLeaf);

    public <R> R foldBetween(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction) {
        Lists.checkRange(startPosition, endPosition, measure(metric));
        if(startPosition == endPosition) {
            return acc;
        } else {
            return foldBetween0(
                    acc, reduction, metric, startPosition, endPosition, rangeReduction);
        }
    }

    abstract <R> R foldBetween0(
            R acc,
            BiFunction<? super R, ? super T, ? extends R> reduction,
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TetraFunction<? super R, ? super T, Integer, Integer, ? extends R> rangeReduction);

    public Optional<S> getSummaryBetween(int startLeaf, int endLeaf) {
        Lists.checkRange(startLeaf, endLeaf, getLeafCount());
        return startLeaf == endLeaf
            ? Optional.empty()
            : Optional.of(getSummaryBetween0(startLeaf, endLeaf));
    }

    abstract S getSummaryBetween0(
            int startLeaf,
            int endLeaf);

    public Optional<S> getSummaryBetween(
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TriFunction<? super T, Integer, Integer, ? extends S> subSummary) {
        Lists.checkRange(startPosition, endPosition, measure(metric));
        return startPosition == endPosition
            ? Optional.empty()
            : Optional.of(getSummaryBetween0(metric, startPosition, endPosition, subSummary));
    }

    abstract S getSummaryBetween0(
            ToIntFunction<? super S> metric,
            int startPosition,
            int endPosition,
            TriFunction<? super T, Integer, Integer, ? extends S> subSummary);

    public Tuple2<FingerTree<T, S>, FingerTree<T, S>> split(int beforeLeaf) {
        Lists.checkPosition(beforeLeaf, getLeafCount());
        return split0(beforeLeaf);
    }

    abstract Tuple2<FingerTree<T, S>, FingerTree<T, S>> split0(int beforeLeaf);

    public FingerTree<T, S> removeLeafs(int fromLeaf, int toLeaf) {
        Lists.checkRange(fromLeaf, toLeaf, getLeafCount());
        if(fromLeaf == toLeaf) {
            return this;
        } else if(fromLeaf == 0 && toLeaf == getLeafCount()) {
            return empty();
        } else {
            FingerTree<T, S> left = split0(fromLeaf)._1;
            FingerTree<T, S> right = split0(toLeaf)._2;
            return left.join(right);
        }
    }

    public FingerTree<T, S> insertLeaf(int position, T data) {
        Lists.checkPosition(position, getLeafCount());
        return split0(position)
                .map((l, r) -> l.join(leaf(data)).join(r));
    }

    public abstract FingerTree<T, S> join(FingerTree<T, S> rightTree);

    public NonEmptyFingerTree<T, S> append(T data) {
        return leaf(data).prependTree(this);
    }

    public NonEmptyFingerTree<T, S> prepend(T data) {
        return leaf(data).appendTree(this);
    }

    /**
     * Returns a list view of this tree.
     * Complexity of operations on the returned list:
     * <ul>
     *   <li>{@code size()}: O(1);</li>
     *   <li>{@code get}: O(log(n));</li>
     *   <li><b>iteration</b>: O(n) in either direction,
     *     with O(log(n)) total allocations;</li>
     *   <li>{@code subList}: O(log(n));</li>
     *   <li><b>iterative {@code subList},</b> i.e. calling {@code subList}
     *     on the result of previous {@code subList}, up to n times: O(n).</li>
     * </ul>
     */
    public abstract List<T> asList();

    abstract T getData(); // valid for leafs only

    Empty<T, S> empty() {
        return new Empty<>(semigroup);
    }

    Leaf<T, S> leaf(T data) {
        return new Leaf<>(semigroup, data);
    }

    final int measure(ToIntFunction<? super S> metric) {
        return getSummaryOpt().map(metric::applyAsInt).orElse(0);
    }
}