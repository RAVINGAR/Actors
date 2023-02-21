package com.ravingarinc.api.concurrent.key;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ConcurrentKeyedQueue<K, T extends Keyed<K>> implements Queue<T> {
    private final ConcurrentHashMap<K, AtomicReference<T>> map;
    private final ConcurrentLinkedQueue<AtomicReference<T>> queue;

    private final Function<T, Boolean> offerFunction;

    public ConcurrentKeyedQueue(final Mode mode) {
        super();
        map = new ConcurrentHashMap<>();
        queue = new ConcurrentLinkedQueue<>();

        offerFunction = switch (mode) {
            case UPDATE -> (element) -> {
                AtomicReference<T> reference = map.get(element.getKey());
                if (reference == null) {
                    reference = new AtomicReference<>(element);
                    map.put(element.getKey(), reference);
                    queue.add(reference);
                } else {
                    reference.setRelease(element);
                }
                return true;
            };
            case IGNORE -> (element) -> {
                AtomicReference<T> reference = map.get(element.getKey());
                if (reference == null) {
                    reference = new AtomicReference<>(element);
                    map.put(element.getKey(), reference);
                    queue.add(reference);
                    return true;
                }
                return false;
            };
        };
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        if (o instanceof Keyed k) {
            return map.containsKey(k.getKey());
        }
        return false;
    }

    @Override
    public boolean add(final T t) {
        return offer(t);
    }

    @Override
    public void clear() {
        map.clear();
        queue.clear();
    }

    @Override
    public boolean offer(final T element) {
        return offerFunction.apply(element);
    }

    @Override
    public T remove() {
        final T value = poll();
        if (value == null) {
            throw new IllegalStateException("Queue was empty!");
        }
        return value;
    }

    @Override
    public T poll() {
        final AtomicReference<T> reference = queue.poll();
        if (reference == null) {
            return null;
        }
        final T value = reference.getAcquire();
        reference.setRelease(null);
        if (value == null) {
            // already expired, if so then try poll again!;
            return poll();
        }
        map.remove(value.getKey());
        return value;
    }

    @Override
    public T element() {
        return queue.element().getAcquire();
    }

    @Override
    public T peek() {
        final AtomicReference<T> value = queue.peek();
        if (value == null) {
            return null;
        }
        return value.getAcquire();
    }

    @Override
    @Deprecated
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException("This method is not supported by ConcurrentKeyedQueue");
    }

    @Override
    @Deprecated
    public boolean containsAll(@NotNull final Collection<?> c) {
        throw new UnsupportedOperationException("This method is not supported by ConcurrentKeyedQueue");
    }

    @Override
    @Deprecated
    public boolean addAll(@NotNull final Collection<? extends T> c) {
        throw new UnsupportedOperationException("This method is not supported by ConcurrentKeyedQueue");
    }

    @Override
    @Deprecated
    public boolean removeAll(@NotNull final Collection<?> c) {
        throw new UnsupportedOperationException("This method is not supported by ConcurrentKeyedQueue");
    }

    @Override
    @Deprecated
    public boolean retainAll(@NotNull final Collection<?> c) {
        throw new UnsupportedOperationException("This method is not supported by ConcurrentKeyedQueue");
    }

    @NotNull
    @Override
    @Deprecated
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("This method is not supported by ConcurrentKeyedQueue");
    }

    @NotNull
    @Override
    @Deprecated
    public Object @NotNull [] toArray() {
        throw new UnsupportedOperationException("This method is not supported by ConcurrentKeyedQueue");
    }

    @NotNull
    @Override
    @Deprecated
    public <T1> T1 @NotNull [] toArray(final T1 @NotNull [] a) {
        throw new UnsupportedOperationException("This method is not supported by ConcurrentKeyedQueue");
    }

    public enum Mode {
        /**
         * Update mode means that if a similarly named keyed value already exists, then update the value.
         */
        UPDATE,
        /**
         * Ignore mode means that is a similarly named key value already exists, then do not update the value
         */
        IGNORE
    }
}
