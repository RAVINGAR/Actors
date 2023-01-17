package com.ravingarinc.actor.api;

import com.ravingarinc.actor.api.async.AsynchronousException;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface AsyncFunction<A, B, C, R> {
    R apply(@NotNull A a, @NotNull B b, @NotNull C c) throws AsynchronousException;
}
