package org.reactfx;

import java.util.Optional;
import java.util.function.Function;

/**
 * See {@link EventStream#flatMap(Function)}
 */
class FlatMapStream<T, U> extends EventStreamBase<U> {
    private final EventStream<T> source;
    private final Function<? super T, ? extends EventStream<U>> mapper;

    private Subscription mappedSubscription = Subscription.EMPTY;

    public FlatMapStream(
            EventStream<T> src,
            Function<? super T, ? extends EventStream<U>> f) {
        this.source = src;
        this.mapper = f;
    }

    @Override
    protected Subscription observeInputs() {
        Subscription s = source.subscribe(t -> {
            mappedSubscription.unsubscribe();
            mappedSubscription = mapper.apply(t).subscribe(this::emit);
        });
        return () -> {
            s.unsubscribe();
            mappedSubscription.unsubscribe();
            mappedSubscription = Subscription.EMPTY;
        };
    }
}

class FlatMapOptStream<T, U> extends EventStreamBase<U> {
    private final EventStream<T> source;
    private final Function<? super T, Optional<U>> mapper;

    public FlatMapOptStream(
            EventStream<T> src,
            Function<? super T, Optional<U>> f) {
        this.source = src;
        this.mapper = f;
    }

    @Override
    protected Subscription observeInputs() {
        return source.subscribe(t -> mapper.apply(t).ifPresent(this::emit));
    }
}
