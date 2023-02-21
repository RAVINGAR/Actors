package com.ravingarinc.api.concurrent;

import com.ravingarinc.api.I;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

public class CompletableRunnable<V> implements Runnable, Future<V> {
    private final Future<V> future;
    private final Consumer<V> consumer;

    private final long timeout;

    private final Semaphore semaphore;

    private final AtomicReference<V> reference;

    public CompletableRunnable(final Future<V> future, final Consumer<V> consumer) throws AsynchronousException {
        this(future, consumer, 1000);
    }

    public CompletableRunnable(final Future<V> future, final Consumer<V> consumer, final long timeout) throws AsynchronousException {
        this.future = future;
        this.consumer = consumer;
        this.timeout = timeout;
        this.reference = new AtomicReference<>(null);
        this.semaphore = new Semaphore(1);
        try {
            semaphore.acquire();
        } catch (final InterruptedException e) {
            throw new AsynchronousException("Acquiring semaphore was interrupted in CompletableRunnable!", e);
        }
    }

    @Override
    public void run() {
        try {
            reference.setRelease(future.get(timeout, TimeUnit.MILLISECONDS));
        } catch (final InterruptedException e) {
            I.logIfDebug(() -> "CompletableRunnable thread was interrupted!", e);
        } catch (final ExecutionException e) {
            I.log(Level.WARNING, "Encountered execution exception in CompletableRunnable!", e);
        } catch (final TimeoutException e) {
            I.log(Level.WARNING, "CompletableFuture timed out!");
        } finally {
            consumer.accept(reference.getAcquire());
            semaphore.release();
        }
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
    @Nullable
    @Blocking
    public V get() {
        return get(1000, TimeUnit.MILLISECONDS);
    }

    @Override
    @Blocking
    public V get(final long timeout, @NotNull final TimeUnit unit) {
        if (isCancelled()) {
            throw new CancellationException("CompletableRunnable was cancelled!");
        }
        try {
            if (!semaphore.tryAcquire(timeout, unit)) {
                throw new TimeoutException("Getting value from task callback timed out!");
            }
        } catch (final InterruptedException | TimeoutException e) {
            I.log(Level.SEVERE, "Encountered multiple exceptions in task callback!", e);
        } finally {
            semaphore.release();
        }
        return reference.getAcquire();
    }
}
