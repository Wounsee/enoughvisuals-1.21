package fun.enoughvisuals.systems.emotions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fun.enoughvisuals.systems.emotions.render.BobjRig;
import fun.enoughvisuals.systems.emotions.render.BobjSpace;
import fun.enoughvisuals.systems.emotions.render.PulseEmotionRenderer;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.client.Emotions;
import fun.enoughvisuals.systems.network.RemoteConfig;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.ui.screen.EmotionWheelScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Controller for the Pulse emote wheel, playback and multiplayer state. */
public final class EmotionWheelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/Emotions");
    private static final EmotionType[] TYPES = EmotionType.values();
    private static final int PAGE_SIZE = 8;
    private static final float FADE_IN_MS = 180f;
    private static final float FADE_OUT_MS = 240f;
    private static final float INNER_RADIUS = 48f;
    private static final float OUTER_RADIUS = 108f;
    private static final float SECTOR_GAP_DEGREES = 3.2f;
    private static final int WHEEL_PIECE_COLOR = 0xFF121214;
    private static final float WHEEL_SCRIM_ALPHA = 0.30f;
    private static final float WHEEL_BLUR_ALPHA = 0.65f;
    private static final float WHEEL_BASE_ALPHA = 0.90f;
    private static final float WHEEL_HOVER_ALPHA = 0.80f;
    private static final EmotionWheelManager INSTANCE = new EmotionWheelManager();

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<UUID, ActiveEmotion> active = new ConcurrentHashMap<>();
    private final Map<EmotionType, PulseEmotionModel> models = new EnumMap<>(EmotionType.class);
    private final Set<EmotionType> failedModels = new HashSet<>();
    private final float[] slotHover = new float[PAGE_SIZE];

    private boolean open;
    private float animation;
    private long lastFrame;
    private double cursorX;
    private double cursorY;
    private int page;
    private int stickySlot = -1;
    private EmotionType hovered;
    private Perspective savedPerspective;

    private EmotionWheelManager() {
    }

    public static EmotionWheelManager getInstance() {
        return INSTANCE;
    }

    public boolean capturesLook() {
        return open;
    }

    public boolean onKey(int key, int action) {
        Emotions module = Emotions.getInstance();
        if (module == null || !module.isEnabled() || key != module.wheelKey()) return false;

        if (action == GLFW.GLFW_PRESS && mc.player != null && mc.world != null && mc.currentScreen == null) {
            open();
            return true;
        }
        if (action == GLFW.GLFW_RELEASE && open) {
            updateHovered();
            EmotionType selected = hovered;
            close(false);
            if (mc.currentScreen instanceof EmotionWheelScreen) mc.setScreen(null);
            if (selected != null) play(selected);
            return true;
        }
        return open;
    }

    public void updatePointer(double mouseX, double mouseY, int width, int height) {
        if (!open) return;
        cursorX = mouseX - width * 0.5;
        cursorY = mouseY - height * 0.5;
        updateHovered();
    }

    public boolean scrollPage(double amount) {
        if (!open || amount == 0.0 || pageCount() <= 1) return false;
        int direction = amount > 0.0 ? -1 : 1;
        page = Math.floorMod(page + direction, pageCount());
        stickySlot = -1;
        hovered = null;
        updateHovered();
        return true;
    }

    public void tick() {
        if (mc.player == null || mc.world == null) {
            stopAll();
            return;
        }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof EmotionWheelScreen) && open) close(true);

        long now = System.currentTimeMillis();
        UUID localId = mc.player.getUuid();
        ActiveEmotion local = active.get(localId);
        if (local != null && local.local) {
            boolean finished = local.expired(now);
            boolean invalidState = mc.player.isSpectator()
                    || !mc.player.isAlive()
                    || mc.player.isSleeping()
                    || mc.player.hasVehicle()
                    || mc.player.isGliding();
            boolean moved = now - local.started > 300L
                    && mc.player.getPos().squaredDistanceTo(local.startedAt) > 0.0016;
            if (finished || moved || invalidState) stopLocal(localId, local);
        }

        active.entrySet().removeIf(entry -> !entry.getValue().local && entry.getValue().expired(now));
    }

    public void stopAll() {
        close(true);
        if (mc.currentScreen instanceof EmotionWheelScreen) mc.setScreen(null);
        restorePerspective();
        active.clear();
    }

    /** Stops only this client's playback; remote users must remain visible even when the module is off. */
    public void stopLocalOnly() {
        close(true);
        if (mc.currentScreen instanceof EmotionWheelScreen) mc.setScreen(null);
        if (mc.player != null) {
            UUID uuid = mc.player.getUuid();
            ActiveEmotion emotion = active.get(uuid);
            if (emotion != null && emotion.local) stopLocal(uuid, emotion);
        } else {
            restorePerspective();
        }
    }

    /** Clears data and GPU caches after client resources are reloaded. */
    public void reloadResources() {
        synchronized (models) {
            models.clear();
            failedModels.clear();
        }
        PulseEmotionRenderer.clearCache();
    }

    private void open() {
        open = true;
        cursorX = cursorY = 0.0;
        page = 0;
        stickySlot = -1;
        hovered = null;
        lastFrame = System.currentTimeMillis();
        mc.setScreen(new EmotionWheelScreen());
    }

    private void close(boolean force) {
        open = false;
        if (force) {
            stickySlot = -1;
            hovered = null;
        }
    }

    public void cancelWheel() {
        close(true);
    }

    private void play(EmotionType type) {
        if (mc.player == null || mc.player.isSpectator() || model(type) == null) return;

        UUID uuid = mc.player.getUuid();
        ActiveEmotion previous = active.get(uuid);
        if (previous != null && previous.local && previous.type == type) {
            stopLocal(uuid, previous);
            return;
        }

        if (savedPerspective == null) {
            savedPerspective = previous != null && previous.local ? previous.previous : mc.options.getPerspective();
        }
        mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
        active.put(uuid, new ActiveEmotion(type, UUID.randomUUID().toString(), System.currentTimeMillis(),
                savedPerspective, true, mc.player.getPos()));
        RemoteConfig.getInstance().requestImmediateSync();
    }

    private void stopLocal(UUID uuid, ActiveEmotion emotion) {
        if (active.remove(uuid, emotion)) {
            restorePerspective();
            RemoteConfig.getInstance().requestImmediateSync();
        }
    }

    private void restorePerspective() {
        if (savedPerspective != null) {
            mc.options.setPerspective(savedPerspective);
            savedPerspective = null;
        }
    }

    public void appendSync(JsonObject request) {
        if (mc.getCurrentServerEntry() == null || mc.player == null) return;
        request.addProperty("game_server", mc.getCurrentServerEntry().address);
        ActiveEmotion current = active.get(mc.player.getUuid());
        if (current != null && !current.expired(System.currentTimeMillis())) {
            JsonObject emotion = new JsonObject();
            emotion.addProperty("type", current.type.wireName());
            emotion.addProperty("playback_id", current.playbackId);
            request.add("emotion", emotion);
        }
    }

    public void applySync(JsonObject response) {
        if (mc.player == null || mc.world == null || mc.getCurrentServerEntry() == null) return;
        long now = System.currentTimeMillis();
        JsonArray values = response.has("active_emotions") && response.get("active_emotions").isJsonArray()
                ? response.getAsJsonArray("active_emotions") : new JsonArray();
        Set<UUID> received = new HashSet<>();
        for (JsonElement element : values) {
            try {
                JsonObject item = element.getAsJsonObject();
                UUID uuid = UUID.fromString(item.get("uuid").getAsString());
                EmotionType type = EmotionType.fromWireName(item.get("type").getAsString());
                if (type == null || uuid.equals(mc.player.getUuid()) || model(type) == null) continue;
                String playbackId = item.has("playback_id") && !item.get("playback_id").isJsonNull()
                        ? item.get("playback_id").getAsString() : "";
                long elapsed = Math.max(0L, item.get("elapsed_ms").getAsLong());
                if (!type.looping()) elapsed = Math.min(elapsed, type.durationMs());
                active.put(uuid, new ActiveEmotion(type, playbackId, now - elapsed,
                        Perspective.FIRST_PERSON, false, Vec3d.ZERO));
                received.add(uuid);
            } catch (RuntimeException ignored) {
            }
        }
        active.entrySet().removeIf(entry -> !entry.getValue().local && !received.contains(entry.getKey()));
    }

    public void render(DrawContext context) {
        MenuScreen.syncTheme();
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.016f : MathHelper.clamp((now - lastFrame) / 1000f, 0.001f, 0.05f);
        lastFrame = now;
        animation = approach(animation, open ? 1f : 0f, dt, 12f);
        if (animation <= 0.001f && !open) return;

        updateHovered();
        for (int i = 0; i < slotHover.length; i++) {
            slotHover[i] = approach(slotHover[i], stickySlot == i ? 1f : 0f, dt, 16f);
        }

        float a = easeOutCubic(MathHelper.clamp(animation, 0f, 1f));
        float width = context.getScaledWindowWidth();
        float height = context.getScaledWindowHeight();
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        float scale = 0.82f + 0.18f * a;
        float inner = INNER_RADIUS * scale;
        float outer = OUTER_RADIUS * scale;

        // Capture the untouched world once. The blur texture is then clipped into
        // the individual wheel pieces instead of being painted over the whole screen.
        GlassUtil.BLUR_OFFSET = CustomTheme.clickGuiBlurLevel();
        if (!CustomTheme.blurDisabled()) GlassUtil.beginFrame(); else GlassUtil.skipFrame();

        // The scrim stays below the glass pieces.
        context.fill(0, 0, (int) width, (int) height,
                alpha(0xFF000000, WHEEL_SCRIM_ALPHA * a));
        context.draw();

        // The blur is emitted as the actual wedge geometry. Do not regress this to a
        // bounding quad + stencil: stencil availability changes across framebuffer passes
        // and previously exposed one large translucent square around the wheel.
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            float hover = slotHover[slot];
            float start = -90f + slot * 45f + SECTOR_GAP_DEGREES * 0.5f;
            float end = -90f + (slot + 1) * 45f - SECTOR_GAP_DEGREES * 0.5f;
            float sectorOuter = outer + hover * 3f;
            float pieceAlpha = WHEEL_BASE_ALPHA
                    + (WHEEL_HOVER_ALPHA - WHEEL_BASE_ALPHA) * hover;

            GlassUtil.drawGlassRingSector(context, cx, cy, inner, sectorOuter, start, end,
                    WHEEL_PIECE_COLOR, pieceAlpha * a,
                    WHEEL_BLUR_ALPHA * a);
        }

        float centerRadius = Math.max(1f, inner - 3f);
        GlassUtil.drawGlassCircle(context, cx, cy, centerRadius,
                WHEEL_PIECE_COLOR, WHEEL_BASE_ALPHA * a, WHEEL_BLUR_ALPHA * a);

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = page * PAGE_SIZE + slot;
            float hover = slotHover[slot];
            float start = -90f + slot * 45f + SECTOR_GAP_DEGREES * 0.5f;
            float end = -90f + (slot + 1) * 45f - SECTOR_GAP_DEGREES * 0.5f;

            if (index < TYPES.length) {
                EmotionType type = TYPES[index];
                double angle = Math.toRadians((start + end) * 0.5);
                float labelRadius = (inner + outer) * 0.5f + hover * 1.5f;
                float labelX = cx + (float) Math.cos(angle) * labelRadius;
                float labelY = cy + (float) Math.sin(angle) * labelRadius - 2f;
                int labelColor = alpha(hover > 0.1f ? 0xFFFFFFFF : 0xFFD7D7DA, a * (0.82f + hover * 0.18f));
                CustomFontRenderer.drawCenteredText(context, compactTitle(type.title()), labelX, labelY,
                        5.4f, labelColor, hover > 0.35f ? FontWeight.SEMIBOLD : FontWeight.REGULAR);
            }
        }

        String centerTitle = hovered == null ? "Эмоции" : hovered.title();
        CustomFontRenderer.drawCenteredText(context, centerTitle, cx, cy - 6f,
                centerTitle.length() > 13 ? 5.2f : 6.6f, alpha(0xFFFFFFFF, a), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawCenteredText(context, (page + 1) + " / " + pageCount(), cx, cy + 7f,
                4.8f, alpha(0xFF8D8D96, a), FontWeight.REGULAR);

        float dotsY = cy + outer + 15f;
        for (int i = 0; i < pageCount(); i++) {
            float x = cx + (i - (pageCount() - 1) * 0.5f) * 10f;
            RenderUtil.drawCrispCircle(context, x, dotsY, i == page ? 2.4f : 1.7f,
                    alpha(i == page ? MenuScreen.ACCENT_LIGHT : 0xFF74747B, a));
        }

        float pointerX = cx + (float) cursorX;
        float pointerY = cy + (float) cursorY;
        RenderUtil.drawCrispCircle(context, pointerX, pointerY, 3.1f, alpha(0xFFFFFFFF, a));
    }

    private void updateHovered() {
        double distance = Math.hypot(cursorX, cursorY);
        int slot = -1;
        if (distance >= INNER_RADIUS && distance <= OUTER_RADIUS + 7.0) {
            double degrees = Math.toDegrees(Math.atan2(cursorY, cursorX));
            double normalized = (degrees + 90.0 + 360.0) % 360.0;
            int candidate = Math.min(PAGE_SIZE - 1, (int) (normalized / 45.0));
            double within = normalized - candidate * 45.0;
            if (within >= SECTOR_GAP_DEGREES * 0.5 && within <= 45.0 - SECTOR_GAP_DEGREES * 0.5) {
                int index = page * PAGE_SIZE + candidate;
                if (index < TYPES.length) slot = candidate;
            }
        }

        stickySlot = slot;
        hovered = slot < 0 ? null : TYPES[page * PAGE_SIZE + slot];
    }

    private int pageCount() {
        return Math.max(1, (TYPES.length + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /** Returns the sampled clip data used by the skinned player renderer. */
    public RenderEmotion getRenderEmotion(PlayerEntityRenderState state) {
        if (mc.world == null || state.spectator || state.deathTime > 0.0f || state.sleepingDirection != null
                || state.hasVehicle || state.isGliding || state.usingRiptide) return null;
        Entity entity = mc.world.getEntityById(state.id);
        if (!(entity instanceof PlayerEntity player)) return null;

        EmotionType type;
        long started;
        float blend;
        ActiveEmotion current = active.get(player.getUuid());
        if (current != null) {
            type = current.type;
            started = current.started;
            blend = current.blend(System.currentTimeMillis());
        } else {
            return null;
        }

        PulseEmotionModel model = model(type);
        if (model == null || blend <= 0.001f) return null;
        double elapsedSeconds = Math.max(0L, System.currentTimeMillis() - started) / 1000.0;
        return new RenderEmotion(type, model, elapsedSeconds, blend);
    }

    /**
     * Mirrors the upper Pulse bones onto vanilla parts so armor, held items and
     * body-attached cosmetics stay reasonably aligned with the skinned body.
     */
    public BobjRig.Pose apply(PlayerEntityRenderState state, ModelPart head, ModelPart hat, ModelPart body,
                              ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        RenderEmotion render = getRenderEmotion(state);
        if (render == null) return null;
        try {
            // Evaluate one exact hierarchical pose. The renderer and all vanilla attachment
            // proxies therefore use the same parent transforms, translations and scale.
            BobjRig.Pose pose = PulseEmotionRenderer.pose(
                    state, render.model, render.elapsedSeconds, render.blend);
            applyBone(pose, "head", head);
            // Pulse splits the torso at the waist: "body" is the hip/lower segment while
            // wings, backpacks and vanilla chest features are anchored to the upper torso.
            applyBone(pose, "low_body", body);
            applyBone(pose, "right_arm", rightArm);
            applyBone(pose, "left_arm", leftArm);
            applyBone(pose, "right_leg", rightLeg);
            applyBone(pose, "left_leg", leftLeg);
            hat.copyTransform(head);
            return pose;
        } catch (IOException exception) {
            LOGGER.error("Unable to evaluate Pulse attachment pose", exception);
            return null;
        }
    }

    private static void applyBone(BobjRig.Pose pose, String name, ModelPart part) {
        Matrix4f skinning = pose.skinningMatrix(name).orElse(null);
        if (skinning == null) return;

        BobjSpace.applySkinningToPart(skinning, part);
    }

    private PulseEmotionModel model(EmotionType type) {
        PulseEmotionModel cached = models.get(type);
        if (cached != null) return cached;
        if (failedModels.contains(type)) return null;
        synchronized (models) {
            cached = models.get(type);
            if (cached != null) return cached;
            if (failedModels.contains(type)) return null;
            try {
                PulseEmotionCatalog.Entry entry = PulseEmotionCatalog.find(type.resourceId())
                        .orElseThrow(() -> new IOException("Missing catalog entry " + type.resourceId()));
                cached = PulseEmotionModel.load(entry);
                models.put(type, cached);
                return cached;
            } catch (IOException | RuntimeException exception) {
                failedModels.add(type);
                LOGGER.error("Unable to load Pulse emote {}", type.resourceId(), exception);
                return null;
            }
        }
    }

    private static String compactTitle(String title) {
        return switch (title) {
            case "Orange Justice" -> "Orange";
            case "Gangnam Style" -> "Gangnam";
            case "Победный танец" -> "Победный";
            default -> title;
        };
    }

    private static float approach(float current, float target, float dt, float speed) {
        return current + (target - current) * (1f - (float) Math.exp(-speed * dt));
    }

    private static float easeOutCubic(float x) {
        float inverse = 1f - x;
        return 1f - inverse * inverse * inverse;
    }

    private static int alpha(int color, float alpha) {
        int sourceAlpha = color >>> 24;
        int applied = MathHelper.clamp((int) (sourceAlpha * MathHelper.clamp(alpha, 0f, 1f)), 0, 255);
        return applied << 24 | color & 0xFFFFFF;
    }

    public record RenderEmotion(EmotionType type, PulseEmotionModel model,
                                double elapsedSeconds, float blend) {
    }

    private static final class ActiveEmotion {
        private final EmotionType type;
        private final String playbackId;
        private final long started;
        private final Perspective previous;
        private final boolean local;
        private final Vec3d startedAt;

        private ActiveEmotion(EmotionType type, String playbackId, long started, Perspective previous,
                              boolean local, Vec3d startedAt) {
            this.type = type;
            this.playbackId = playbackId;
            this.started = started;
            this.previous = previous;
            this.local = local;
            this.startedAt = startedAt;
        }

        private long elapsed(long now) {
            return Math.max(0L, now - started);
        }

        private boolean expired(long now) {
            return !type.looping() && elapsed(now) >= type.durationMs();
        }

        private float blend(long now) {
            long elapsed = elapsed(now);
            float fadeIn = MathHelper.clamp(elapsed / FADE_IN_MS, 0f, 1f);
            if (type.looping()) return smoothStep(fadeIn);
            long left = Math.max(0L, type.durationMs() - elapsed);
            float fadeOut = MathHelper.clamp(left / FADE_OUT_MS, 0f, 1f);
            return smoothStep(Math.min(fadeIn, fadeOut));
        }

        private static float smoothStep(float value) {
            return value * value * (3f - 2f * value);
        }
    }
}
