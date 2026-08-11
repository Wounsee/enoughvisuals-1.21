package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Register(
        name = "Optimization",
        id = "optimization",
        description = "Комплексная оптимизация клиента и плавности кадров",
        category = Category.UTILS
)
public final class Optimization extends Module {
    private static final int VANILLA_UNLIMITED_FPS = 260;
    private static final int FULL_RENDER_BUDGET_FPS = 240;
    private static Optimization instance;

    public final BooleanSetting minecraftSettings = add(new BooleanSetting(
            "Настройки Minecraft", "minecraft_settings", "Применяет быстрые встроенные настройки графики", true));
    public final SliderSetting chunkDistance = add(new SliderSetting(
            "Прогрузка чанков", "chunk_distance", "Дальность прорисовки при активной оптимизации",
            8, 2, 32, 1, " чанков"));
    public final BooleanSetting fastGraphics = add(new BooleanSetting(
            "Быстрая графика", "fast_graphics", "Отключает AO и включает быстрый режим графики", true));
    public final BooleanSetting minimalParticles = add(new BooleanSetting(
            "Минимум частиц", "minimal_particles", "Уменьшает количество ванильных частиц", true));
    public final BooleanSetting limitParticles = add(new BooleanSetting(
            "Лимит частиц", "limit_particles", "Ограничивает дальность и число частиц за тик", true));
    public final SliderSetting particleDistance = add(new SliderSetting(
            "Дистанция частиц", "particle_distance", "Максимальная дистанция создания частиц",
            32, 8, 128, 4, " блоков"));
    public final SliderSetting particleCap = add(new SliderSetting(
            "Потолок частиц", "particle_cap", "Максимум новых частиц за игровой тик",
            1500, 100, 5000, 100));
    public final BooleanSetting simpleLeaves = add(new BooleanSetting(
            "Упрощённая листва", "simple_leaves", "Не рисует внутренние грани соседних листьев", true));
    public final SliderSetting entityDistance = add(new SliderSetting(
            "Дистанция сущностей", "entity_distance", "Множитель встроенной дистанции сущностей",
            50, 50, 100, 5, "%"));
    public final BooleanSetting threadBoost = add(new BooleanSetting(
            "Приоритет потоков", "thread_boost", "Повышает приоритет render и chunk-worker потоков", true));

    private Integer originalViewDistance;
    private Boolean originalAo;
    private Integer originalMaxFps;
    private Boolean originalRawMouse;
    private Boolean originalVsync;
    private GraphicsMode originalGraphicsMode;
    private ParticlesMode originalParticlesMode;
    private Double originalEntityDistance;

    private Thread renderThread;
    private int originalRenderThreadPriority = Thread.NORM_PRIORITY;
    private final Map<Thread, Integer> originalWorkerPriorities = new ConcurrentHashMap<>();
    private int tickCounter;
    private int currentParticleCount;
    private final OptimizationFrameScheduler frameScheduler = new OptimizationFrameScheduler();

    public Optimization() {
        instance = this;
        chunkDistance.visibleWhen(minecraftSettings::get);
        fastGraphics.visibleWhen(minecraftSettings::get);
        minimalParticles.visibleWhen(minecraftSettings::get);
        simpleLeaves.visibleWhen(minecraftSettings::get);
        entityDistance.visibleWhen(minecraftSettings::get);
        particleDistance.visibleWhen(limitParticles::get);
        particleCap.visibleWhen(limitParticles::get);
    }

    @Override
    protected void onEnable() {
        instance = this;
        tickCounter = 0;
        currentParticleCount = 0;
        captureOriginalOptions();
        applyThreadBoost();
        applyMinecraftSettings();
        frameScheduler.allowAll();
    }

    @Override
    protected void onDisable() {
        restoreOptions();
        restoreThreadPriorities();
        frameScheduler.allowAll();
    }

    @Override
    public void onTick() {
        tickCounter++;
        currentParticleCount = 0;
        if (tickCounter % 20 == 0) applyMinecraftSettings();
        if (threadBoost.get()) {
            if (renderThread == null) applyThreadBoost();
            else if (tickCounter % 100 == 0) tuneWorkerThreads();
        } else if (renderThread != null || !originalWorkerPriorities.isEmpty()) {
            restoreThreadPriorities();
        }
    }

    public static boolean shouldSpawnParticle(double x, double y, double z) {
        Optimization optimization = instance;
        if (optimization == null || !optimization.isEnabled() || !optimization.limitParticles.get()) return true;
        if (mc.player == null) return true;
        double maxDistance = optimization.particleDistance.get();
        if (mc.player.squaredDistanceTo(x, y, z) > maxDistance * maxDistance) return false;
        if (optimization.currentParticleCount >= optimization.particleCap.get().intValue()) return false;
        optimization.currentParticleCount++;
        return true;
    }

    public static boolean useSimpleLeaves() {
        Optimization optimization = instance;
        return optimization != null && optimization.isEnabled()
                && optimization.minecraftSettings.get() && optimization.simpleLeaves.get();
    }

    public static boolean forceUnlimitedFps() {
        Optimization optimization = instance;
        return optimization != null && optimization.isEnabled();
    }

    public static void tickFrame() {
        Optimization optimization = instance;
        if (optimization == null || !optimization.isEnabled()) {
            if (optimization != null) optimization.frameScheduler.allowAll();
            return;
        }
        optimization.frameScheduler.tick(System.nanoTime(), FULL_RENDER_BUDGET_FPS, true);
    }

    public static boolean shouldRenderFrame() {
        Optimization optimization = instance;
        return optimization == null || !optimization.isEnabled() || optimization.frameScheduler.shouldRender();
    }

    private void captureOriginalOptions() {
        if (mc.options == null) return;
        if (originalViewDistance == null) originalViewDistance = mc.options.getViewDistance().getValue();
        if (originalAo == null) originalAo = mc.options.getAo().getValue();
        if (originalMaxFps == null) originalMaxFps = mc.options.getMaxFps().getValue();
        if (originalRawMouse == null) originalRawMouse = mc.options.getRawMouseInput().getValue();
        if (originalVsync == null) originalVsync = mc.options.getEnableVsync().getValue();
        if (originalGraphicsMode == null) originalGraphicsMode = mc.options.getGraphicsMode().getValue();
        if (originalParticlesMode == null) originalParticlesMode = mc.options.getParticles().getValue();
        if (originalEntityDistance == null) originalEntityDistance = mc.options.getEntityDistanceScaling().getValue();
    }

    private void applyMinecraftSettings() {
        if (mc.options == null) return;
        captureOriginalOptions();
        boolean reloadRenderer = false;

        if (mc.options.getEnableVsync().getValue()) mc.options.getEnableVsync().setValue(false);
        if (!mc.options.getRawMouseInput().getValue()) mc.options.getRawMouseInput().setValue(true);
        if (mc.options.getMaxFps().getValue() != VANILLA_UNLIMITED_FPS) {
            mc.options.getMaxFps().setValue(VANILLA_UNLIMITED_FPS);
        }

        if (minecraftSettings.get()) {
            int viewDistance = chunkDistance.get().intValue();
            if (mc.options.getViewDistance().getValue() != viewDistance) {
                mc.options.getViewDistance().setValue(viewDistance);
                reloadRenderer = true;
            }
            if (fastGraphics.get()) {
                if (mc.options.getAo().getValue()) mc.options.getAo().setValue(false);
                if (mc.options.getGraphicsMode().getValue() != GraphicsMode.FAST) {
                    mc.options.getGraphicsMode().setValue(GraphicsMode.FAST);
                    reloadRenderer = true;
                }
            }
            if (minimalParticles.get() && mc.options.getParticles().getValue() != ParticlesMode.MINIMAL) {
                mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
            }
            double distanceScale = entityDistance.get() / 100.0;
            if (Math.abs(mc.options.getEntityDistanceScaling().getValue() - distanceScale) > 0.001) {
                mc.options.getEntityDistanceScaling().setValue(distanceScale);
            }
        }

        if (reloadRenderer && mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    private void restoreOptions() {
        if (mc.options == null) return;
        boolean reloadRenderer = false;
        if (originalViewDistance != null) {
            mc.options.getViewDistance().setValue(originalViewDistance);
            reloadRenderer = true;
        }
        if (originalAo != null) mc.options.getAo().setValue(originalAo);
        if (originalMaxFps != null) mc.options.getMaxFps().setValue(originalMaxFps);
        if (originalRawMouse != null) mc.options.getRawMouseInput().setValue(originalRawMouse);
        if (originalVsync != null) mc.options.getEnableVsync().setValue(originalVsync);
        if (originalGraphicsMode != null) {
            mc.options.getGraphicsMode().setValue(originalGraphicsMode);
            reloadRenderer = true;
        }
        if (originalParticlesMode != null) mc.options.getParticles().setValue(originalParticlesMode);
        if (originalEntityDistance != null) mc.options.getEntityDistanceScaling().setValue(originalEntityDistance);
        if (reloadRenderer && mc.worldRenderer != null) mc.worldRenderer.reload();

        originalViewDistance = null;
        originalAo = null;
        originalMaxFps = null;
        originalRawMouse = null;
        originalVsync = null;
        originalGraphicsMode = null;
        originalParticlesMode = null;
        originalEntityDistance = null;
    }

    private void applyThreadBoost() {
        if (!threadBoost.get()) return;
        try {
            renderThread = Thread.currentThread();
            originalRenderThreadPriority = renderThread.getPriority();
            renderThread.setPriority(Math.min(Thread.MAX_PRIORITY, 8));
        } catch (Throwable ignored) {
        }
        tuneWorkerThreads();
    }

    private void tuneWorkerThreads() {
        if (!threadBoost.get()) return;
        try {
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread == null || !thread.isAlive()) continue;
                String name = thread.getName();
                boolean chunkWorker = name.contains("Chunk Render")
                        || name.contains("Chunk Renderer")
                        || name.contains("Sodium Chunk")
                        || name.contains("Main-Worker")
                        || name.contains("Worker-Main");
                if (!chunkWorker) continue;
                originalWorkerPriorities.putIfAbsent(thread, thread.getPriority());
                if (thread.getPriority() < 7) thread.setPriority(7);
            }
        } catch (Throwable ignored) {
        }
    }

    private void restoreThreadPriorities() {
        if (renderThread != null) {
            try {
                renderThread.setPriority(originalRenderThreadPriority);
            } catch (Throwable ignored) {
            }
            renderThread = null;
        }
        for (Map.Entry<Thread, Integer> entry : originalWorkerPriorities.entrySet()) {
            try {
                if (entry.getKey().isAlive()) entry.getKey().setPriority(entry.getValue());
            } catch (Throwable ignored) {
            }
        }
        originalWorkerPriorities.clear();
    }
}
