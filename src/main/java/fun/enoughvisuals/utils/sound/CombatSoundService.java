package fun.enoughvisuals.utils.sound;

import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.KillSound;
import fun.enoughvisuals.systems.modules.modules.visual.KillEffect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CombatSoundService {
    private CombatSoundService() {}

    private static final long CANDIDATE_TIMEOUT_MS = 2_000L;

    private static boolean lastAttackPressed = false;

    private static final Map<Integer, Long> deathCandidates = new HashMap<>();
    private static final Map<Integer, Long> killPlayedAt = new HashMap<>();

    public static void recordAttackTarget(Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || !(target instanceof LivingEntity)) {
            return;
        }

        KillSound killModule = ModuleManager.getInstance().get(KillSound.class);
        boolean killEnabled = killModule != null && killModule.isEnabled();
        boolean effectEnabled = KillEffect.active();
        if (!killEnabled && !effectEnabled) {
            return;
        }

        deathCandidates.put(target.getId(), System.currentTimeMillis() + CANDIDATE_TIMEOUT_MS);
        KillEffect.rememberCandidate(target);
    }

    public static void onClientTick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            clear();
            return;
        }

        KillSound killModule = ModuleManager.getInstance().get(KillSound.class);
        boolean killEnabled = killModule != null && killModule.isEnabled();
        boolean effectEnabled = KillEffect.active();

        if (!killEnabled && !effectEnabled) {
            clear();
            return;
        }

        long now = System.currentTimeMillis();

        boolean attackPressed = client.options.attackKey.isPressed();
        if (attackPressed && !lastAttackPressed && client.currentScreen == null) {
            if (client.mouse != null && GLFW.glfwGetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_DISABLED) {
                if (client.crosshairTarget instanceof EntityHitResult hitResult) {
                    Entity target = hitResult.getEntity();
                    if (target instanceof LivingEntity) {
                        recordAttackTarget(target);
                    }
                }
            }
        }
        lastAttackPressed = attackPressed;

        Iterator<Map.Entry<Integer, Long>> it = deathCandidates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            int entityId = entry.getKey();
            long expiresAt = entry.getValue();

            if (now > expiresAt) {
                it.remove();
                continue;
            }

            Entity e = client.world.getEntityById(entityId);

            if (e instanceof LivingEntity living && (living.isDead() || living.getHealth() <= 0.0f)) {
                if (effectEnabled) {
                    KillEffect.onKill(e, entityId);
                }
                if (killEnabled && !wasRecentlyPlayed(killPlayedAt, entityId, now, 1000L)) {

                    EnoughSoundPlayer.playKill(killModule.getVolume());
                    killPlayedAt.put(entityId, now);
                }
                it.remove();
            } else if (e == null || e.isRemoved()) {

                if (effectEnabled) {
                    KillEffect.onKill(e, entityId);
                }
                if (killEnabled && !wasRecentlyPlayed(killPlayedAt, entityId, now, 1000L)) {
                    EnoughSoundPlayer.playKill(killModule.getVolume());
                    killPlayedAt.put(entityId, now);
                }
                it.remove();
            }
        }

        cleanupOld(killPlayedAt, now, 5_000L);
    }

    private static boolean wasRecentlyPlayed(Map<Integer, Long> played, int entityId, long now, long timeoutMs) {
        Long last = played.get(entityId);
        return last != null && (now - last) < timeoutMs;
    }

    private static void cleanupOld(Map<Integer, Long> map, long now, long timeoutMs) {
        map.entrySet().removeIf(e -> now - e.getValue() > timeoutMs);
    }

    private static void clear() {
        lastAttackPressed = false;
        deathCandidates.clear();
        killPlayedAt.clear();
    }
}
