package org.reactfx;

import org.reactfx.util.Experimental;

/**
 * An event stream that suspends a suspendable object during emission.
 * More precisely, before a {@linkplain SuspenderStream} emits a value, it
 * suspends a {@linkplain Suspendable} object and unsuspends it after the
 * emission has completed.
 */
@Experimental
public interface SuspenderStream<T, S extends Suspendable>
extends EventStream<T>, Suspender<S> {}