package com.ravingarinc.actor.api;

import com.ravingarinc.actor.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BiMap<K, B, V> implements Map<Pair<K, B>, V> {
    private final Map<K, V> firstMap;
    private final Map<B, V> secondMap;

    private final Class<K> firstKeyType;
    private final Class<B> secondKeyType;

    public BiMap(final Class<K> firstKeyType, final Class<B> secondKeyType) {
        this.firstMap = new ConcurrentHashMap<>();
        this.secondMap = new ConcurrentHashMap<>();
        this.firstKeyType = firstKeyType;
        this.secondKeyType = secondKeyType;
    }

    @Override
    public int size() {
        return firstMap.size();
    }

    @Override
    public boolean isEmpty() {
        return firstMap.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        if (key.getClass().equals(firstKeyType)) {
            return firstMap.containsKey(key);
        } else if (key.getClass().equals(secondKeyType)) {
            return secondMap.containsKey(key);
        }
        throw new ClassCastException("Key was of inappropriate type!");
    }

    @Override
    public boolean containsValue(final Object value) {
        return firstMap.containsValue(value);
    }

    @Override
    @Nullable
    public V get(@NotNull final Object key) {
        if (key.getClass().equals(firstKeyType)) {
            return firstMap.get(key);
        } else if (key.getClass().equals(secondKeyType)) {
            return secondMap.get(key);
        } else if (key instanceof Pair<?, ?> pair) {
            if (pair.getLeft().equals(firstKeyType) && pair.getRight().equals(secondKeyType)) {
                return firstMap.get(pair.getLeft());
            }
        }
        throw new ClassCastException("Key was of inappropriate type!");
    }

    @Nullable
    public V put(@NotNull final K firstKey, @NotNull final B secondKey, final V value) {
        firstMap.put(firstKey, value);
        secondMap.put(secondKey, value);
        return value;
    }

    @Nullable
    @Override
    @Deprecated
    public V put(final Pair<K, B> key, final V value) {
        return put(key.getLeft(), key.getRight(), value);
    }

    @Nullable
    public V removeBoth(final K firstKey, final B secondKey) {
        final V v1 = firstMap.remove(firstKey);
        final V v2 = secondMap.remove(secondKey);
        if (v1 == null || v2 == null) {
            return null;
        }
        if (v1.equals(v2)) {
            return v1;
        } else {
            throw new IllegalArgumentException("Value was different between both maps!");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(final Object key) {
        if (key instanceof Pair<?, ?> pair) {
            if (pair.getLeft().getClass().equals(firstKeyType) && pair.getRight().getClass().equals(secondKeyType)) {
                return removeBoth((K) pair.getLeft(), (B) pair.getRight());
            }
        }
        throw new ClassCastException("Key was of inappropriate type!");
    }

    @Override
    @Deprecated
    public void putAll(@NotNull final Map<? extends Pair<K, B>, ? extends V> m) {
        m.forEach((pair, value) -> {
            firstMap.put(pair.getLeft(), value);
            secondMap.put(pair.getRight(), value);
        });
    }

    @Override
    public void clear() {
        firstMap.clear();
        secondMap.clear();
    }

    @NotNull
    @Override
    public Set<Pair<K, B>> keySet() {
        final List<K> firstKeys = new ArrayList<>(firstMap.keySet());
        final List<B> secondKeys = new ArrayList<>(secondMap.keySet());

        final Set<Pair<K, B>> set = new HashSet<>();
        for (int i = 0; i < size(); i++) {
            set.add(new Pair<>(firstKeys.get(i), secondKeys.get(i)));
        }
        return set;
    }

    /**
     * Same functionality as specified in {@link Map#keySet()}
     *
     * @return A set of the first key types
     */
    public Set<K> firstKeys() {
        return firstMap.keySet();
    }

    /**
     * Same functionality as specified in {@link Map#keySet()}
     *
     * @return A set of the first key types
     */
    public Set<B> secondKeys() {
        return secondMap.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return firstMap.values();
    }

    @NotNull
    @Override
    public Set<Entry<Pair<K, B>, V>> entrySet() {
        final List<Entry<K, V>> firstKeyValues = new ArrayList<>(firstMap.entrySet());
        final List<B> secondKeys = new ArrayList<>(secondMap.keySet());

        final Set<Entry<Pair<K, B>, V>> set = new HashSet<>();
        for (int i = 0; i < size(); i++) {
            final Entry<K, V> entry = firstKeyValues.get(i);
            set.add(new BiEntry<>(entry.getKey(), secondKeys.get(i), entry.getValue()));
        }
        return set;
    }

    public static class BiEntry<U1, U2, R> implements Entry<Pair<U1, U2>, R> {
        private final U1 firstKey;
        private final U2 secondKey;
        private R value;

        public BiEntry(final U1 firstKey, final U2 secondKey, final R value) {
            this.firstKey = firstKey;
            this.secondKey = secondKey;
            this.value = value;
        }

        @Override
        public Pair<U1, U2> getKey() {
            return new Pair<>(firstKey, secondKey);
        }

        @Override
        public R getValue() {
            return value;
        }

        @Override
        public R setValue(final R value) {
            this.value = value;
            return value;
        }
    }
}
