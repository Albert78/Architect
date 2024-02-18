package org.reactfx;

/**
 * See {@link EventStream#repeatOn(EventStream)}
 */
class RepeatOnStream<T> extends EventStreamBase<T> {
    private final EventStream<T> source;
    private final EventStream<?> impulse;

    private boolean hasValue = false;
    private T value = null;

    public RepeatOnStream(EventStream<T> source, EventStream<?> impulse) {
        this.source = source;
        this.impulse = impulse;
    }

    @Override
    protected Subscription observeInputs() {
        Subscription s1 = source.subscribe(v -> {
            hasValue = true;
            value = v;
            emit(v);
        });

        Subscription s2 = impulse.subscribe(i -> {
            if(hasValue) {
                emit(value);
            }
        });

        return s1.and(s2);
    }
}