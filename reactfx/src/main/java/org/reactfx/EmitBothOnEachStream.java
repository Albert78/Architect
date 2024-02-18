package org.reactfx;

import static org.reactfx.util.Tuples.*;

import org.reactfx.util.Tuple2;

/**
 * {@link EventStream#emitBothOnEach(EventStream)}
 */
class EmitBothOnEachStream<A, I> extends EventStreamBase<Tuple2<A, I>> {
    private final EventStream<A> source;
    private final EventStream<I> impulse;

    private boolean hasValue = false;
    private A a = null;

    public EmitBothOnEachStream(EventStream<A> source, EventStream<I> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription observeInputs() {
        Subscription s1 = source.subscribe(a -> {
            hasValue = true;
            this.a = a;
        });

        Subscription s2 = impulse.subscribe(i -> {
            if(hasValue) {
                emit(t(a, i));
            }
        });

        return s1.and(s2);
    }
}