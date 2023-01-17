package com.ravingarinc.actor.api.async;

import com.ravingarinc.actor.api.util.I;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;

public class CompletableRunnable<V> implements Runnable, Future<V> {
    private final CompletableFuture<V> future;
    private final Consumer<V> consumer;

    private final long timeout;

    public CompletableRunnable(final CompletableFuture<V> future, final Consumer<V> consumer) {
        this(future, consumer, 1000);
    }

    public CompletableRunnable(final CompletableFuture<V> future, final Consumer<V> consumer, final long timeout) {
        this.future = future;
        this.consumer = consumer;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        V value = null;
        try {
            value = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            I.logIfDebug(() -> "CompletableRunnable thread was interrupted!", e);
        } catch (final ExecutionException e) {
            I.log(Level.WARNING, "Encountered execution exception in CompletableRunnable!", e);
        } catch (final TimeoutException e) {
            I.log(Level.WARNING, "CompletableFuture timed out!");
        }
        consumer.accept(value);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    @Deprecated
    public V get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    @Deprecated
    public V get(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }
}
