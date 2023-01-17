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

public class BiMap<K1, K2, V> implements Map<Pair<K1, K2>, V> {
    private final Map<K1, V> firstMap;
    private final Map<K2, V> secondMap;

    private final Class<K1> firstKeyType;
    private final Class<K2> secondKeyType;

    public BiMap(final Class<K1> firstKeyType, final Class<K2> secondKeyType) {
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
    public V put(@NotNull final K1 firstKey, @NotNull final K2 secondKey, final V value) {
        firstMap.put(firstKey, value);
        secondMap.put(secondKey, value);
        return value;
    }

    @Nullable
    @Override
    @Deprecated
    public V put(final Pair<K1, K2> key, final V value) {
        return put(key.getLeft(), key.getRight(), value);
    }

    @Nullable
    public V removeBoth(final K1 firstKey, final K2 secondKey) {
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
                return removeBoth((K1) pair.getLeft(), (K2) pair.getRight());
            }
        }
        throw new ClassCastException("Key was of inappropriate type!");
    }

    @Override
    @Deprecated
    public void putAll(@NotNull final Map<? extends Pair<K1, K2>, ? extends V> m) {
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
    public Set<Pair<K1, K2>> keySet() {
        final List<K1> firstKeys = new ArrayList<>(firstMap.keySet());
        final List<K2> secondKeys = new ArrayList<>(secondMap.keySet());

        final Set<Pair<K1, K2>> set = new HashSet<>();
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
    public Set<K1> firstKeys() {
        return firstMap.keySet();
    }

    /**
     * Same functionality as specified in {@link Map#keySet()}
     *
     * @return A set of the first key types
     */
    public Set<K2> secondKeys() {
        return secondMap.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return firstMap.values();
    }

    @NotNull
    @Override
    public Set<Entry<Pair<K1, K2>, V>> entrySet() {
        final List<Entry<K1, V>> firstKeyValues = new ArrayList<>(firstMap.entrySet());
        final List<K2> secondKeys = new ArrayList<>(secondMap.keySet());

        final Set<Entry<Pair<K1, K2>, V>> set = new HashSet<>();
        for (int i = 0; i < size(); i++) {
            final Entry<K1, V> entry = firstKeyValues.get(i);
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
