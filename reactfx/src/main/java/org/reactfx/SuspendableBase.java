package org.reactfx;

import org.reactfx.util.AccumulationFacility;
import org.reactfx.util.AccumulatorSize;
import org.reactfx.util.NotificationAccumulator;

public abstract class SuspendableBase<O, T, A>
extends ObservableBase<O, T>
implements Suspendable {
    private final EventStream<T> input;
    private final AccumulationFacility<T, A> af;

    private int suspended = 0;
    private boolean hasValue = false;
    private A accumulatedValue = null;

    protected SuspendableBase(
            EventStream<T> input,
            NotificationAccumulator<O, T, A> pn) {
        super(pn);
        this.input = input;
        this.af = pn.getAccumulationFacility();
    }

    protected abstract AccumulatorSize sizeOf(A accum);
    protected abstract T headOf(A accum);
    protected abstract A tailOf(A accum);

    protected A initialAccumulator(T value) {
        return af.initialAccumulator(value);
    }

    protected A reduce(A accum, T value) {
        return af.reduce(accum, value);
    }

    protected final boolean isSuspended() {
        return suspended > 0;
    }

    @Override
    public final Guard suspend() {
        ++suspended;
        return Guard.closeableOnce(this::resume);
    }

    @Override
    protected final Subscription observeInputs() {
        Subscription sub = input.subscribe(this::handleEvent);
        return sub.and(this::reset);
    }

    private void resume() {
        --suspended;
        if(suspended == 0 && hasValue) {
            while(sizeOf(accumulatedValue) == AccumulatorSize.MANY) {
                enqueueNotifications(headOf(accumulatedValue));
                accumulatedValue = tailOf(accumulatedValue);
            }
            if(sizeOf(accumulatedValue) == AccumulatorSize.ONE) {
                enqueueNotifications(headOf(accumulatedValue));
            }
            reset();
            notifyObservers();
        }
    }

    private void reset() {
        hasValue = false;
        accumulatedValue = null;
    }

    private void handleEvent(T event) {
        if(isSuspended()) {
            if(hasValue) {
                accumulatedValue = reduce(accumulatedValue, event);
            } else {
                accumulatedValue = initialAccumulator(event);
                hasValue = true;
            }
        } else {
            notifyObservers(event);
        }
    }
}