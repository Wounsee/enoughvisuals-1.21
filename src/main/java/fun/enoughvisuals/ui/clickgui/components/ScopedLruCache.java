package fun.enoughvisuals.ui.clickgui.components;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Small identity-scoped LRU used for resources which are valid only in one client world. */
final class ScopedLruCache<K, V> {
    private final int capacity;
    private final Consumer<V> disposer;
    private final LinkedHashMap<K, V> values = new LinkedHashMap<>(16, 0.75f, true);
    private Object scope;
    private boolean scopeSet;

    ScopedLruCache(int capacity, Consumer<V> disposer) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        this.disposer = disposer;
    }

    synchronized V getOrCreate(Object currentScope, K key, Supplier<V> creator) {
        switchScope(currentScope);
        V cached = values.get(key);
        if (cached != null) return cached;

        V created = creator.get();
        if (created == null) return null;
        values.put(key, created);
        trimToCapacity();
        return created;
    }

    synchronized void switchScope(Object currentScope) {
        if (scopeSet && scope == currentScope) return;
        disposeAll();
        scope = currentScope;
        scopeSet = true;
    }

    synchronized void clear() {
        disposeAll();
        scope = null;
        scopeSet = false;
    }

    synchronized int size() {
        return values.size();
    }

    private void trimToCapacity() {
        while (values.size() > capacity) {
            Map.Entry<K, V> eldest = values.entrySet().iterator().next();
            V value = eldest.getValue();
            values.remove(eldest.getKey());
            disposer.accept(value);
        }
    }

    private void disposeAll() {
        var stale = new ArrayList<>(values.values());
        values.clear();
        RuntimeException failure = null;
        for (V value : stale) {
            try {
                disposer.accept(value);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }
}
