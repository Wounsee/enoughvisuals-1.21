package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.Setting;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DragAndDrop {

    private static final DragAndDrop INSTANCE = new DragAndDrop();

    public static class HudEntry {
        public final String id;
        public final String displayName;
        public final Identifier icon;
        public final HudFactory factory;

        public HudEntry(String id, String displayName, String iconFile, HudFactory factory) {
            this.id = id;
            this.displayName = displayName;
            this.icon = iconFile != null
                    ? Identifier.of("enough", "icons/hud/" + iconFile)
                    : null;
            this.factory = factory;
        }
    }

    @FunctionalInterface
    public interface HudFactory {
        HudElement create(float x, float y);
    }

    private final List<HudEntry> registry = new ArrayList<>();
    private final Map<String, HudEntry> entryMap = new HashMap<>();
    private final List<HudElement> activeElements = new ArrayList<>();
    private final Set<String> activeTypeIds = new HashSet<>();
    private final Map<String, Map<String, Object>> rememberedSettings = new HashMap<>();

    private HudElement dragging;
    private HudElement hovered;
    private float dragOffsetX;
    private float dragOffsetY;
    private float hoverAnim;
    private float dragTargetX;
    private float dragTargetY;
    private long handCursorHandle = 0L;
    private boolean handCursorApplied = false;

    public static DragAndDrop getInstance() {
        return INSTANCE;
    }

    public void register(String id, String displayName, String iconFile, HudFactory factory) {
        HudEntry entry = new HudEntry(id, displayName, iconFile, factory);
        registry.add(entry);
        entryMap.put(id, entry);
    }

    public List<HudEntry> getRegistry() {
        return Collections.unmodifiableList(registry);
    }

    public boolean isElementActive(String typeId) {
        return activeTypeIds.contains(typeId);
    }

    public HudElement addElement(String id, float x, float y) {
        HudEntry entry = entryMap.get(id);
        if (entry == null || activeTypeIds.contains(id)) {
            return null;
        }

        HudElement element = entry.factory.create(x, y);
        element.setTypeId(id);
        element.setDisplayName(entry.displayName);
        element.setIcon(entry.icon);
        applyRememberedSettings(element);

        clampToScreen(element);

        activeElements.add(element);
        activeTypeIds.add(id);
        return element;
    }

    public void removeElement(HudElement element) {
        removeElement(element, false);
    }

    public void removeElementForced(HudElement element) {
        removeElement(element, true);
    }

    private void removeElement(HudElement element, boolean forced) {
        if (element == null || element.isRemoving() || (!forced && !element.isRemovable())) {
            return;
        }

        rememberElementSettings(element);
        element.markRemoving();
        if (element.getTypeId() != null) {
            activeTypeIds.remove(element.getTypeId());
        }
        if (dragging == element) {
            element.clearDragVelocity();
            element.clearDragGrip();
            dragging = null;
        }
        if (hovered == element) {
            hovered = null;
        }
    }

    public List<HudElement> getActiveElements() {
        return Collections.unmodifiableList(activeElements);
    }

    public HudElement getActiveElementByTypeId(String typeId) {
        if (typeId == null) return null;
        for (HudElement element : activeElements) {
            if (typeId.equals(element.getTypeId())) {
                return element;
            }
        }
        return null;
    }

    public void clearAllImmediate() {
        for (HudElement element : activeElements) {
            rememberElementSettings(element);
        }
        activeElements.clear();
        activeTypeIds.clear();
        dragging = null;
        hovered = null;
        dragTargetX = 0f;
        dragTargetY = 0f;
        resetCursor();
    }

    public void renderAll(DrawContext ctx, float delta) {
        long now = System.currentTimeMillis();
        HudElement targetHud = null;
        for (int i = activeElements.size() - 1; i >= 0; i--) {
            HudElement element = activeElements.get(i);
            if (element.isExpired(now)) {
                activeElements.remove(i);
                continue;
            }

            if ("targethud".equals(element.getTypeId())) {
                targetHud = element;
                continue;
            }

            renderElement(ctx, delta, element);
        }

                                                                             
        renderElement(ctx, delta, targetHud);
    }

    private void renderElement(DrawContext ctx, float delta, HudElement element) {
        if (element == null) return;
        if (element == dragging) {
            float follow = RenderUtil.getFrameDelta();
            element.setDragVelocity(dragTargetX - element.getX(), dragTargetY - element.getY());
            float nextX = RenderUtil.animate(element.getX(), dragTargetX, 0.22f, follow);
            float nextY = RenderUtil.animate(element.getY(), dragTargetY, 0.22f, follow);
            element.setX(nextX);
            element.setY(nextY);
        }
        try {
            element.render(ctx, delta);
        } finally {
            StencilUtil.resetRenderState();
        }
    }

    public void renderHoverOverlay(DrawContext ctx, int mx, int my, float delta) {
        boolean hasHover = hovered != null && !isDragging() && !ContextMenu.isVisible();
        hoverAnim = RenderUtil.animate(hoverAnim, hasHover ? 1f : 0f, 0.24f, RenderUtil.getFrameDelta());
        applyHoverCursor(hasHover);

        if (hoverAnim <= 0.02f || hovered == null) {
            return;
        }

        float x = hovered.getX() - 2f;
        float y = hovered.getY() - 2f;
        float w = hovered.getWidth() + 4f;
        float h = hovered.getHeight() + 4f;
        float a = RenderUtil.easeOutQuad(hoverAnim);

        float hudScale = HudElement.hudScale();
        ctx.getMatrices().push();
        ctx.getMatrices().scale(hudScale, hudScale, 1f);
        RenderUtil.drawRoundedOutline(ctx, x, y, w, h, 9f, 1f,
                RenderUtil.withAlpha(0xFFFFFF, a * 0.40f));
        RenderUtil.drawRoundedOutline(ctx, x + 0.5f, y + 0.5f, w - 1f, h - 1f, 8.5f, 0.6f,
                RenderUtil.withAlpha(CustomTheme.hudAccent(), a * 0.26f));
        ctx.getMatrices().pop();
    }

    public HudElement getHovered() {
        return hovered;
    }

    public HudElement getDragging() {
        return dragging;
    }

    public boolean isDragging() {
        return dragging != null;
    }

    public void updateHover(float mx, float my) {
        if (isDragging() || ContextMenu.isVisible()) {
            return;
        }

        mx = HudElement.screenToHud(mx);
        my = HudElement.screenToHud(my);
        hovered = null;
        for (int i = activeElements.size() - 1; i >= 0; i--) {
            HudElement element = activeElements.get(i);
            if (!element.isRemoving() && element.contains(mx, my)) {
                hovered = element;
                break;
            }
        }
    }

    private void applyHoverCursor(boolean hover) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;

        long window = client.getWindow().getHandle();
        if (window == 0L) return;

        if (hover) {
            if (!handCursorApplied) {
                if (handCursorHandle == 0L) {
                    handCursorHandle = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
                }
                GLFW.glfwSetCursor(window, handCursorHandle);
                handCursorApplied = true;
            }
        } else if (handCursorApplied) {
            GLFW.glfwSetCursor(window, 0L);
            handCursorApplied = false;
        }
    }

    public boolean startDrag(float mx, float my) {
        if (hovered == null || hovered.isRemoving() || !hovered.isDraggable()) {
            return false;
        }

        mx = HudElement.screenToHud(mx);
        my = HudElement.screenToHud(my);
        dragging = hovered;
        bringToFront(dragging);
        dragOffsetX = mx - dragging.getX();
        dragOffsetY = my - dragging.getY();
        dragTargetX = dragging.getX();
        dragTargetY = dragging.getY();
        float gripX = (mx - dragging.getX()) / Math.max(1f, dragging.getWidth());
        float gripY = (my - dragging.getY()) / Math.max(1f, dragging.getHeight());
        dragging.setDragGrip(gripX, gripY);
        dragging.clearDragVelocity();
        return true;
    }

    public void drag(float mx, float my) {
        if (dragging == null) {
            return;
        }

        mx = HudElement.screenToHud(mx);
        my = HudElement.screenToHud(my);
        float newX = mx - dragOffsetX;
        float newY = my - dragOffsetY;

        float[] snapped = Grid.liveSnapAt(dragging, newX, newY);
        dragTargetX = snapped[0];
        dragTargetY = snapped[1];
        clampTargetsToScreen(dragging);
    }

    public void stopDrag() {
        if (dragging == null) {
            return;
        }

                                                                               
        dragging.clearDragVelocity();
        dragging.clearDragGrip();
        dragging = null;
    }

    public void resetCursor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        long window = client.getWindow().getHandle();
        if (window != 0L) {
            GLFW.glfwSetCursor(window, 0L);
        }
        handCursorApplied = false;
    }

    private void bringToFront(HudElement element) {
        if (activeElements.remove(element)) {
            activeElements.add(element);
        }
    }

    private void clampToScreen(HudElement element) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) {
            return;
        }

        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();
        element.setX(Math.max(4f, Math.min(screenW - element.getWidth() - 4f, element.getX())));
        element.setY(Math.max(4f, Math.min(screenH - element.getHeight() - 4f, element.getY())));
    }

    private void clampTargetsToScreen(HudElement element) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) {
            return;
        }

        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();
        dragTargetX = Math.max(4f, Math.min(screenW - element.getWidth() - 4f, dragTargetX));
        dragTargetY = Math.max(4f, Math.min(screenH - element.getHeight() - 4f, dragTargetY));
    }

    public Map<String, Map<String, Object>> getRememberedSettingsCopy() {
        Map<String, Map<String, Object>> out = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : rememberedSettings.entrySet()) {
            out.put(e.getKey(), new HashMap<>(e.getValue()));
        }
        return out;
    }

    public void setRememberedSettings(Map<String, Map<String, Object>> incoming) {
        rememberedSettings.clear();
        if (incoming == null) return;
        for (Map.Entry<String, Map<String, Object>> e : incoming.entrySet()) {
            rememberedSettings.put(e.getKey(), new HashMap<>(e.getValue()));
        }
    }

    private void rememberElementSettings(HudElement element) {
        String typeId = element.getTypeId();
        if (typeId == null || typeId.isBlank()) return;
        Map<String, Object> map = rememberedSettings.computeIfAbsent(typeId, k -> new HashMap<>());
        map.put("__x", element.getX());
        map.put("__y", element.getY());
        for (Setting<?> setting : element.getSettings()) {
            map.put(setting.getId(), setting.serialize());
        }
    }

    private void applyRememberedSettings(HudElement element) {
        String typeId = element.getTypeId();
        if (typeId == null || typeId.isBlank()) return;
        Map<String, Object> map = rememberedSettings.get(typeId);
        if (map == null || map.isEmpty()) return;
        Object x = map.get("__x");
        Object y = map.get("__y");
        if (x instanceof Number nx) {
            element.setX(nx.floatValue());
        }
        if (y instanceof Number ny) {
            element.setY(ny.floatValue());
        }
        for (Setting<?> setting : element.getSettings()) {
            if (!map.containsKey(setting.getId())) continue;
            try {
                setting.deserialize(map.get(setting.getId()));
            } catch (Exception ignored) {}
        }
    }
}
