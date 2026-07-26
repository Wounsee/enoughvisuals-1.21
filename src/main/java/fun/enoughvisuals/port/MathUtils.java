package fun.enoughvisuals.port;

import java.util.concurrent.ThreadLocalRandom;

public final class MathUtils {
    private MathUtils() { }
    public static double measure18(double min, double max) { return min + ThreadLocalRandom.current().nextDouble() * (max - min); }
    public static float measure19(float min, float max) { return min + ThreadLocalRandom.current().nextFloat() * (max - min); }
    public static double measure25(double value, double step) { return Math.round(Math.round(value / step) * step * 100.0) / 100.0; }
}
