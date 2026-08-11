package fun.enoughvisuals.ui.clickgui.components;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScopedLruCacheTest {
    @Test
    void reusesValueWithoutInvokingLoaderAgainInTheSameWorld() {
        ScopedLruCache<String, Object> cache = new ScopedLruCache<>(4, ignored -> {});
        Object world = new Object();
        AtomicInteger loads = new AtomicInteger();

        Object first = cache.getOrCreate(world, "model", () -> {
            loads.incrementAndGet();
            return new Object();
        });
        Object second = cache.getOrCreate(world, "model", () -> {
            loads.incrementAndGet();
            return new Object();
        });

        assertSame(first, second);
        assertEquals(1, loads.get());
    }

    @Test
    void disposesOldWorldBeforeCreatingReplacement() {
        List<String> disposed = new ArrayList<>();
        ScopedLruCache<String, String> cache = new ScopedLruCache<>(4, disposed::add);
        Object firstWorld = new Object();
        Object secondWorld = new Object();

        cache.getOrCreate(firstWorld, "model", () -> "old");
        assertEquals("new", cache.getOrCreate(secondWorld, "model", () -> "new"));

        assertEquals(List.of("old"), disposed);
        assertEquals(1, cache.size());
    }

    @Test
    void evictsOnlyTheLeastRecentlyUsedValueAtTheBound() {
        List<String> disposed = new ArrayList<>();
        ScopedLruCache<String, String> cache = new ScopedLruCache<>(2, disposed::add);
        Object world = new Object();

        cache.getOrCreate(world, "a", () -> "A");
        cache.getOrCreate(world, "b", () -> "B");
        cache.getOrCreate(world, "a", () -> "unexpected");
        cache.getOrCreate(world, "c", () -> "C");

        assertEquals(List.of("B"), disposed);
        assertEquals(2, cache.size());
        assertEquals("A", cache.getOrCreate(world, "a", () -> "unexpected"));
    }

    @Test
    void explicitInvalidationDisposesEveryCachedValue() {
        List<String> disposed = new ArrayList<>();
        ScopedLruCache<String, String> cache = new ScopedLruCache<>(4, disposed::add);
        Object world = new Object();
        cache.getOrCreate(world, "a", () -> "A");
        cache.getOrCreate(world, "b", () -> "B");

        cache.clear();

        assertEquals(List.of("A", "B"), disposed);
        assertEquals(0, cache.size());
    }
}
