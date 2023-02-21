package com.ravingarinc.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class Sync {

    private Sync() {
        throw new AssertionError("Sync should not be instantiated");
    }

    /**
     * Indicates that the marked method should only ever be called from an asynchronous context. Most likely as it deals
     * with objects that are accessed by other asynchronous threads.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
    public @interface AsyncOnly {
    }

    /**
     * Indicates that the marked method should only ever be called from a synchronous context. Most likely as it deals
     * with objects that are accessed by other synchronous threads. Additionally, this method may use Bukkit API which
     * can only be done on a synchronous context.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    public @interface SyncOnly {
    }
}
