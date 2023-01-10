package com.ravingarinc.actor.api.async;

import com.ravingarinc.actor.RavinPlugin;
import com.ravingarinc.actor.api.util.I;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Singleton class to handle async/sync operations
 */
public class AsyncHandler {
    private static RavinPlugin plugin;
    private static BukkitScheduler scheduler;

    private AsyncHandler() {
    }

    public static void load(final RavinPlugin plugin) {
        AsyncHandler.plugin = plugin;
        AsyncHandler.scheduler = plugin.getServer().getScheduler();
    }

    /**
     * Executes a computation on the main thread, then waits for the result to be returned. Can be used in an
     * asynchronous environment.
     */
    @Async.Execute
    @Blocking
    public static <V> V executeBlockingSyncComputation(final Callable<V> callable) throws AsynchronousException {
        final TaskCallback<V> callback = new TaskCallback<>(callable);
        scheduler.scheduleSyncDelayedTask(plugin, callback);
        return callback.get();
    }

    /**
     * Executes a computation of the given callable as an asynchronous operation. Once the result is complete the given
     * consumer will consume the value on the synchronous thread. It is expected this method is called on as sync.
     *
     * @param callable Callable executed as async
     * @param consumer Consumer is consumed as synchronous
     * @param <V>      The type
     */
    @NonBlocking
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public static <V> void executeAsyncComputation(final Callable<V> callable, final Consumer<V> consumer) {
        scheduler.runTaskAsynchronously(plugin, () -> {
            try {
                applySynchronously(callable.call(), consumer);
            } catch (final Exception e) {
                I.log(Level.SEVERE, "Encountered issue running async computation!", e);
            }
        });
    }

    /**
     * Apply the given consumer to this CharacterEntity object synchronously.
     *
     * @param consumer The consumer
     */
    @NonBlocking
    public static <V> void applySynchronously(final V value, final Consumer<V> consumer) {
        scheduler.scheduleSyncDelayedTask(plugin, () -> consumer.accept(value));
    }

    /**
     * Run a task asynchronously on a new thread
     *
     * @param runnable The runnable
     */
    public static void runAsynchronously(final Runnable runnable) {
        scheduler.runTaskAsynchronously(plugin, runnable);
    }

    /**
     * Run a task synchronously on a new thread.
     *
     * @param runnable The runnable
     */
    public static void runSynchronously(final Runnable runnable) {
        scheduler.runTask(plugin, runnable);
    }
}
