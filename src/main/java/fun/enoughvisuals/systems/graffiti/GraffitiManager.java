package fun.enoughvisuals.systems.graffiti;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.client.Graffiti;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.ui.screen.GraffitiWheelScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Graffiti wheel, raycast placement, sync state and batched world rendering. */
public final class GraffitiManager {
    private static final GraffitiManager INSTANCE = new GraffitiManager();
    private static final int PAGE_SIZE = 8;
    private static final int LOCAL_LIMIT = 25;
    private static final long TTL_MS = 30L * 60L * 1000L;
    private static final double MAX_PLACE_DISTANCE = 6.0;
    private static final double MAX_RENDER_DISTANCE_SQ = 96.0 * 96.0;
    private static final float INNER_RADIUS = 48f;
    private static final float OUTER_RADIUS = 108f;
    private static final float GAP_DEGREES = 3.2f;
    private static final float DECAL_HALF_SIZE = 0.72f;
    private static final float SURFACE_OFFSET = 0.0035f;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<GraffitiCatalog.Entry> catalog = GraffitiCatalog.entries();
    private final Map<String, Placement> placements = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<PendingAction> pendingActions = new ConcurrentLinkedDeque<>();
    private final float[] slotHover = new float[PAGE_SIZE];

    private boolean open;
    private float animation;
    private long lastFrame;
    private double cursorX;
    private double cursorY;
    private int page;
    private int hoveredSlot = -1;

    private GraffitiManager() {
    }

    public static GraffitiManager getInstance() {
        return INSTANCE;
    }

    public boolean onKey(int key, int action) {
        Graffiti module = Graffiti.getInstance();
        if (module == null || !module.isEnabled()) return false;
        if (key == module.quickKey() && action == GLFW.GLFW_PRESS
                && key != module.wheelKey() && mc.player != null && mc.world != null && mc.currentScreen == null) {
            GraffitiCatalog.Entry last = GraffitiCatalog.find(module.lastAsset());
            if (last == null) return false;
            place(last);
            return true;
        }
        if (key != module.wheelKey()) return false;
        if (action == GLFW.GLFW_PRESS && mc.player != null && mc.world != null && mc.currentScreen == null) {
            open = true;
            page = 0;
            hoveredSlot = -1;
            cursorX = cursorY = 0.0;
            lastFrame = System.currentTimeMillis();
            mc.setScreen(new GraffitiWheelScreen());
            return true;
        }
        if (action == GLFW.GLFW_RELEASE && open) {
            updateHovered();
            GraffitiCatalog.Entry selected = selectedEntry();
            open = false;
            hoveredSlot = -1;
            if (mc.currentScreen instanceof GraffitiWheelScreen) mc.setScreen(null);
            if (selected != null) place(selected);
            return true;
        }
        return open;
    }

    public void cancelWheel() {
        open = false;
        hoveredSlot = -1;
        if (mc.currentScreen instanceof GraffitiWheelScreen) mc.setScreen(null);
    }

    public void tick() {
        if (mc.world == null || mc.player == null) {
            cancelWheel();
            placements.clear();
            pendingActions.clear();
            return;
        }
        if (open && mc.currentScreen != null && !(mc.currentScreen instanceof GraffitiWheelScreen)) {
            cancelWheel();
        }
        long now = System.currentTimeMillis();
        placements.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    public void clearWorld() {
        cancelWheel();
        placements.clear();
        pendingActions.clear();
    }

    public void updatePointer(double mouseX, double mouseY, int width, int height) {
        if (!open) return;
        cursorX = mouseX - width * 0.5;
        cursorY = mouseY - height * 0.5;
        updateHovered();
    }

    public boolean scrollPage(double amount) {
        if (!open || amount == 0.0 || pageCount() <= 1) return false;
        page = Math.floorMod(page + (amount > 0 ? -1 : 1), pageCount());
        hoveredSlot = -1;
        updateHovered();
        return true;
    }

    private void place(GraffitiCatalog.Entry entry) {
        if (mc.player == null || mc.world == null) return;
        HitResult result = mc.player.raycast(MAX_PLACE_DISTANCE, 1f, false);
        if (!(result instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        long now = System.currentTimeMillis();
        Direction side = hit.getSide();
        float rotation = side.getAxis() == Direction.Axis.Y ? MathHelper.wrapDegrees(mc.player.getYaw()) : 0f;
        if (overlapsExisting(hit.getPos(), side, rotation)) return;
        String id = UUID.randomUUID().toString();
        Placement placement = new Placement(id, mc.player.getUuid().toString(), entry.id(), dimensionId(),
                hit.getPos(), side, rotation, now, now + TTL_MS);
        placements.put(id, placement);
        enforceLocalLimit(mc.player.getUuid().toString());
        pendingActions.addLast(PendingAction.place(placement));
        Graffiti module = Graffiti.getInstance();
        if (module != null) module.rememberAsset(entry.id());
    }

    private void enforceLocalLimit(String owner) {
        List<Placement> owned = placements.values().stream()
                .filter(value -> value.owner.equalsIgnoreCase(owner))
                .sorted(Comparator.comparingLong(value -> value.createdAt))
                .toList();
        for (int i = 0; i < owned.size() - LOCAL_LIMIT; i++) placements.remove(owned.get(i).id);
    }

    /** Deletes the nearest owned decal under the crosshair and consumes that attack click. */
    public boolean deleteLookedAtOwn() {
        Graffiti module = Graffiti.getInstance();
        if (module == null || !module.isEnabled() || mc.player == null || mc.world == null
                || mc.currentScreen != null) return false;
        String owner = mc.player.getUuid().toString();
        Vec3d origin = mc.player.getEyePos();
        Vec3d direction = mc.player.getRotationVec(1f).normalize();
        HitResult blockHit = mc.player.raycast(MAX_PLACE_DISTANCE, 1f, false);
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? MAX_PLACE_DISTANCE + 1.0 : blockHit.getPos().distanceTo(origin);

        Placement nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Placement placement : placements.values()) {
            if (!placement.owner.equalsIgnoreCase(owner) || !placement.dimension.equals(dimensionId())) continue;
            Basis basis = basis(placement.side, placement.rotation);
            Vec3d center = placement.position.add(basis.normal.multiply(SURFACE_OFFSET));
            double denominator = direction.dotProduct(basis.normal);
            if (Math.abs(denominator) < 1.0e-6) continue;
            double distance = center.subtract(origin).dotProduct(basis.normal) / denominator;
            if (distance < 0.0 || distance > MAX_PLACE_DISTANCE || distance > blockDistance + 0.08
                    || distance >= nearestDistance) continue;
            Vec3d offset = origin.add(direction.multiply(distance)).subtract(center);
            if (Math.abs(offset.dotProduct(basis.right)) <= DECAL_HALF_SIZE + 0.02
                    && Math.abs(offset.dotProduct(basis.up)) <= DECAL_HALF_SIZE + 0.02) {
                nearest = placement;
                nearestDistance = distance;
            }
        }
        if (nearest == null) return false;
        placements.remove(nearest.id);
        pendingActions.addLast(PendingAction.delete(nearest.id));
        return true;
    }

    private boolean overlapsExisting(Vec3d position, Direction side, float rotation) {
        String dimension = dimensionId();
        for (Placement placement : placements.values()) {
            if (placement.dimension.equals(dimension)
                    && overlaps(position, side, rotation, placement.position, placement.side, placement.rotation)) {
                return true;
            }
        }
        return false;
    }

    static boolean overlaps(Vec3d firstPosition, Direction firstSide, float firstRotation,
                            Vec3d secondPosition, Direction secondSide, float secondRotation) {
        if (firstSide != secondSide) return false;
        Basis first = basis(firstSide, firstRotation);
        Basis second = basis(secondSide, secondRotation);
        Vec3d delta = secondPosition.subtract(firstPosition);
        if (Math.abs(delta.dotProduct(first.normal)) > 0.045) return false;
        Vec3d[] axes = {first.right, first.up, second.right, second.up};
        for (Vec3d axis : axes) {
            double centerDistance = Math.abs(delta.dotProduct(axis));
            double firstRadius = DECAL_HALF_SIZE * (Math.abs(first.right.dotProduct(axis))
                    + Math.abs(first.up.dotProduct(axis)));
            double secondRadius = DECAL_HALF_SIZE * (Math.abs(second.right.dotProduct(axis))
                    + Math.abs(second.up.dotProduct(axis)));
            if (centerDistance >= firstRadius + secondRadius - 0.01) return false;
        }
        return true;
    }

    public void appendSync(JsonObject request) {
        if (mc.player == null || mc.getCurrentServerEntry() == null) return;
        request.addProperty("game_server", mc.getCurrentServerEntry().address);
        PendingAction current = pendingActions.peekFirst();
        if (current == null) return;
        JsonObject action = new JsonObject();
        action.addProperty("op", current.type == ActionType.DELETE ? "delete" : "place");
        action.addProperty("id", current.id);
        if (current.placement != null) {
            Placement placement = current.placement;
            action.addProperty("asset", placement.asset);
            action.addProperty("dimension", placement.dimension);
            action.addProperty("x", placement.position.x);
            action.addProperty("y", placement.position.y);
            action.addProperty("z", placement.position.z);
            action.addProperty("side", placement.side.getId());
            action.addProperty("rotation", placement.rotation);
        }
        request.add("graffiti_action", action);
    }

    public void applySync(JsonObject response) {
        if (mc.player == null || mc.world == null || mc.getCurrentServerEntry() == null) return;
        String ack = string(response, "graffiti_ack");
        PendingAction currentPending = pendingActions.peekFirst();
        if (currentPending != null && currentPending.id.equals(ack)) {
            pendingActions.pollFirst();
        }

        JsonArray array = response.has("active_graffiti") && response.get("active_graffiti").isJsonArray()
                ? response.getAsJsonArray("active_graffiti") : new JsonArray();
        Map<String, Placement> received = new LinkedHashMap<>();
        for (JsonElement element : array) {
            try {
                JsonObject item = element.getAsJsonObject();
                String asset = item.get("asset").getAsString();
                if (GraffitiCatalog.find(asset) == null) continue;
                Direction side = Direction.byId(item.get("side").getAsInt());
                Placement placement = new Placement(
                        item.get("id").getAsString(), item.get("uuid").getAsString(), asset,
                        item.get("dimension").getAsString(),
                        new Vec3d(item.get("x").getAsDouble(), item.get("y").getAsDouble(), item.get("z").getAsDouble()),
                        side, item.get("rotation").getAsFloat(),
                        item.get("created_at_ms").getAsLong(), item.get("expires_at_ms").getAsLong());
                received.put(placement.id, placement);
            } catch (RuntimeException ignored) {
            }
        }

        for (PendingAction action : pendingActions) {
            if (action.type == ActionType.PLACE && action.placement != null) {
                received.putIfAbsent(action.id, action.placement);
            } else if (action.type == ActionType.DELETE) {
                received.remove(action.id);
            }
        }
        placements.clear();
        placements.putAll(received);
    }

    public void renderWheel(DrawContext context) {
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 1f / 60f : MathHelper.clamp((now - lastFrame) / 1000f, 0.001f, 0.05f);
        lastFrame = now;
        animation = approach(animation, open ? 1f : 0f, dt, 12f);
        if (animation <= 0.001f && !open) return;
        updateHovered();
        for (int i = 0; i < PAGE_SIZE; i++) slotHover[i] = approach(slotHover[i], hoveredSlot == i ? 1f : 0f, dt, 16f);

        float alpha = easeOutCubic(MathHelper.clamp(animation, 0f, 1f));
        float cx = context.getScaledWindowWidth() * 0.5f;
        float cy = context.getScaledWindowHeight() * 0.5f;
        float scale = 0.82f + 0.18f * alpha;
        float inner = INNER_RADIUS * scale;
        float outer = OUTER_RADIUS * scale;
        GlassUtil.BLUR_OFFSET = CustomTheme.clickGuiBlurLevel();
        if (!CustomTheme.blurDisabled()) GlassUtil.beginFrame(); else GlassUtil.skipFrame();
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), withAlpha(0xFF000000, 0.30f * alpha));
        context.draw();

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            float hover = slotHover[slot];
            float start = -90f + slot * 45f + GAP_DEGREES * 0.5f;
            float end = -90f + (slot + 1) * 45f - GAP_DEGREES * 0.5f;
            float pieceAlpha = 0.90f + (0.80f - 0.90f) * hover;
            GlassUtil.drawGlassRingSector(context, cx, cy, inner, outer + hover * 3f, start, end,
                    0xFF121214, pieceAlpha * alpha, 0.65f * alpha);
        }
        GlassUtil.drawGlassCircle(context, cx, cy, Math.max(1f, inner - 3f),
                0xFF121214, 0.90f * alpha, 0.65f * alpha);

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = page * PAGE_SIZE + slot;
            if (index >= catalog.size()) continue;
            GraffitiCatalog.Entry entry = catalog.get(index);
            float angle = (float) Math.toRadians(-67.5f + slot * 45f);
            float radius = (inner + outer) * 0.5f + slotHover[slot] * 1.5f;
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            drawThumbnail(context, entry, x - 16f, y - 20f, 32f, alpha);
            CustomFontRenderer.drawCenteredText(context, compact(entry.title()), x, y + 16f,
                    4.5f, withAlpha(0xFFE1E1E5, alpha), FontWeight.SEMIBOLD);
        }

        GraffitiCatalog.Entry selected = selectedEntry();
        CustomFontRenderer.drawCenteredText(context, selected == null ? "Граффити" : selected.title(), cx, cy - 6f,
                selected != null && selected.title().length() > 14 ? 4.9f : 6.4f,
                withAlpha(0xFFFFFFFF, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawCenteredText(context, (page + 1) + " / " + pageCount(), cx, cy + 7f,
                4.8f, withAlpha(0xFF8D8D96, alpha), FontWeight.SEMIBOLD);
        float dotsY = cy + outer + 15f;
        for (int i = 0; i < pageCount(); i++) {
            float x = cx + (i - (pageCount() - 1) * 0.5f) * 8f;
            RenderUtil.drawCrispCircle(context, x, dotsY, i == page ? 2.2f : 1.5f,
                    withAlpha(i == page ? MenuScreen.ACCENT_LIGHT : 0xFF74747B, alpha));
        }
        RenderUtil.drawCrispCircle(context, cx + (float) cursorX, cy + (float) cursorY, 3.1f,
                withAlpha(0xFFFFFFFF, alpha));
    }

    private static void drawThumbnail(DrawContext context, GraffitiCatalog.Entry entry,
                                      float x, float y, float size, float alpha) {
        int cell = entry.animated() ? 128 : 256;
        int atlasWidth = entry.columns() * cell;
        int atlasHeight = entry.rows() * cell;
        int frame = entry.frame(System.currentTimeMillis());
        int sourceX = (frame % entry.columns()) * cell;
        int sourceY = (frame / entry.columns()) * cell;
        RenderUtil.drawTextureUV(context, entry.texture(), x, y, size, size,
                sourceX, sourceY, cell, cell,
                atlasWidth, atlasHeight, alpha);
    }

    public void renderWorld(WorldRenderContext context) {
        Graffiti module = Graffiti.getInstance();
        if (module == null || !module.isEnabled() || mc.world == null || context.camera() == null
                || context.matrixStack() == null || placements.isEmpty()) return;
        String dimension = dimensionId();
        Vec3d camera = context.camera().getPos();
        long now = System.currentTimeMillis();
        Map<Identifier, RenderBatch> batches = new HashMap<>();
        for (Placement placement : placements.values()) {
            if (!placement.dimension.equals(dimension) || placement.expiresAt <= now
                    || placement.position.squaredDistanceTo(camera) > MAX_RENDER_DISTANCE_SQ) continue;
            GraffitiCatalog.Entry entry = GraffitiCatalog.find(placement.asset);
            if (entry == null) continue;
            batches.computeIfAbsent(entry.texture(), ignored -> new RenderBatch(entry)).placements.add(placement);
        }
        if (batches.isEmpty()) return;

        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        try {
            for (RenderBatch batch : batches.values()) {
                RenderSystem.setShaderTexture(0, batch.entry.texture());
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Placement placement : batch.placements) appendQuad(buffer, matrix, camera, placement, batch.entry, now);
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
        } finally {
            RenderSystem.depthMask(oldDepthMask);
            RenderSystem.depthFunc(oldDepthFunc);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    private static void appendQuad(BufferBuilder buffer, Matrix4f matrix, Vec3d camera,
                                   Placement placement, GraffitiCatalog.Entry entry, long now) {
        Basis basis = basis(placement.side, placement.rotation);
        Vec3d center = placement.position.add(basis.normal.multiply(SURFACE_OFFSET)).subtract(camera);
        Vec3d right = basis.right.multiply(DECAL_HALF_SIZE);
        Vec3d up = basis.up.multiply(DECAL_HALF_SIZE);
        Vec3d a = center.subtract(right).add(up);
        Vec3d b = center.subtract(right).subtract(up);
        Vec3d c = center.add(right).subtract(up);
        Vec3d d = center.add(right).add(up);
        int frame = entry.frame(Math.max(0L, now - placement.createdAt));
        float u0 = entry.u(frame);
        float v0 = entry.v(frame);
        float u1 = u0 + entry.uSize();
        float v1 = v0 + entry.vSize();
        vertex(buffer, matrix, a, u0, v0);
        vertex(buffer, matrix, b, u0, v1);
        vertex(buffer, matrix, c, u1, v1);
        vertex(buffer, matrix, d, u1, v0);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3d value, float u, float v) {
        buffer.vertex(matrix, (float) value.x, (float) value.y, (float) value.z).texture(u, v).color(1f, 1f, 1f, 1f);
    }

    static Basis basis(Direction side, float rotation) {
        Vec3i vector = side.getVector();
        Vec3d normal = new Vec3d(vector.getX(), vector.getY(), vector.getZ());
        Vec3d up;
        if (side.getAxis() == Direction.Axis.Y) {
            double radians = Math.toRadians(rotation);
            up = new Vec3d(-Math.sin(radians), 0.0, Math.cos(radians)).normalize();
        } else {
            up = new Vec3d(0.0, 1.0, 0.0);
        }
        Vec3d right = up.crossProduct(normal).normalize();
        return new Basis(right, up, normal);
    }

    private void updateHovered() {
        int slot = -1;
        double distance = Math.hypot(cursorX, cursorY);
        if (distance >= INNER_RADIUS && distance <= OUTER_RADIUS + 7.0) {
            double degrees = Math.toDegrees(Math.atan2(cursorY, cursorX));
            double normalized = (degrees + 90.0 + 360.0) % 360.0;
            int candidate = Math.min(PAGE_SIZE - 1, (int) (normalized / 45.0));
            double within = normalized - candidate * 45.0;
            if (within >= GAP_DEGREES * 0.5 && within <= 45.0 - GAP_DEGREES * 0.5
                    && page * PAGE_SIZE + candidate < catalog.size()) slot = candidate;
        }
        hoveredSlot = slot;
    }

    private GraffitiCatalog.Entry selectedEntry() {
        int index = hoveredSlot < 0 ? -1 : page * PAGE_SIZE + hoveredSlot;
        return index >= 0 && index < catalog.size() ? catalog.get(index) : null;
    }

    private int pageCount() {
        return Math.max(1, (catalog.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private String dimensionId() {
        return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString();
    }

    private static String string(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String compact(String value) {
        if (value.length() <= 15) return value;
        return value.substring(0, 13) + "…";
    }

    private static float approach(float current, float target, float dt, float speed) {
        return current + (target - current) * (1f - (float) Math.exp(-speed * dt));
    }

    private static float easeOutCubic(float value) {
        float inverse = 1f - value;
        return 1f - inverse * inverse * inverse;
    }

    private static int withAlpha(int color, float alpha) {
        return (MathHelper.clamp((int) ((color >>> 24) * alpha), 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    record Basis(Vec3d right, Vec3d up, Vec3d normal) {
    }

    private enum ActionType {
        PLACE,
        DELETE
    }

    private record PendingAction(ActionType type, String id, Placement placement) {
        private static PendingAction place(Placement placement) {
            return new PendingAction(ActionType.PLACE, placement.id, placement);
        }

        private static PendingAction delete(String id) {
            return new PendingAction(ActionType.DELETE, id, null);
        }
    }

    private record Placement(String id, String owner, String asset, String dimension, Vec3d position,
                             Direction side, float rotation, long createdAt, long expiresAt) {
    }

    private static final class RenderBatch {
        final GraffitiCatalog.Entry entry;
        final List<Placement> placements = new ArrayList<>();

        private RenderBatch(GraffitiCatalog.Entry entry) {
            this.entry = entry;
        }
    }
}
