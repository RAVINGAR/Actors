package com.ravingarinc.actor.api;

import com.ravingarinc.actor.api.async.AsynchronousException;

@FunctionalInterface
public interface AsyncFunction<A, B, C, R> {
    R apply(A a, B b, C c) throws AsynchronousException;
}
