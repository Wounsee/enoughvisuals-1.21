package fun.enoughvisuals.systems.modules.modules.utils;

final class OptimizationFrameScheduler {
    private long previousTimestamp;
    private long lastRenderedAt;
    private long accumulator;
    private boolean render = true;

    void tick(long now, int maxFps, boolean smooth) {
        long interval = 1_000_000_000L / Math.max(1, maxFps);

        if (!smooth) {
            accumulator = 0L;
            previousTimestamp = now;
            if (lastRenderedAt == 0L || now - lastRenderedAt >= interval) {
                lastRenderedAt = now;
                render = true;
            } else {
                render = false;
            }
            return;
        }

        if (previousTimestamp == 0L) {
            previousTimestamp = now;
            lastRenderedAt = now;
            accumulator = 0L;
            render = true;
            return;
        }

        long delta = Math.max(0L, now - previousTimestamp);
        previousTimestamp = now;
        delta = Math.min(delta, interval * 4L);
        accumulator = Math.min(accumulator + delta, interval + interval / 2L);

        if (accumulator >= interval) {
            accumulator -= interval;
            lastRenderedAt = now;
            render = true;
        } else {
            render = false;
        }
    }

    boolean shouldRender() {
        return render;
    }

    void allowAll() {
        previousTimestamp = 0L;
        lastRenderedAt = 0L;
        accumulator = 0L;
        render = true;
    }
}
