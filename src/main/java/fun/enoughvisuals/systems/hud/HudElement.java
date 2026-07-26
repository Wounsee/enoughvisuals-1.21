package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.Setting;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class HudElement {

    private static final long APPEAR_MS = 340L;
    private static final long REMOVE_MS = 360L;
    private static final long CONTENT_SHOW_MS = 420L;
    private static final long CONTENT_HIDE_MS = 380L;
    private static final float REFERENCE_GUI_SCALE = 2.0f;
    private static float activeScissorScale = 1.0f;

    private final List<Setting<?>> settings = new ArrayList<>();

    private String typeId;
    private String displayName;
    private Identifier icon;

    private float x;
    private float y;
    private float width;
    private float height;

    private final long spawnTime;
    private long removeTime = -1L;

    private float dragVelocityX;
    private float dragVelocityY;
    private float tiltAnimDeg;
    private float contentAnim = 1f;
    private float contentAnimFrom = 1f;
    private float contentAnimTo = 1f;
    private long contentAnimStartMs = 0L;
    private boolean contentVisibleTarget = true;
    private boolean contentVisibilityManaged = false;
    private float headerRenderAlpha = 1f;
    private float bodyRenderAlpha = 1f;
    private float bodyRevealProgress = 1f;
    private float bodySlideOffset = 0f;

    public HudElement(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.spawnTime = System.currentTimeMillis();
    }

    public final void render(DrawContext ctx, float delta) {
        float frameDelta = RenderUtil.getFrameDelta();
        update(delta);

        if (contentVisibilityManaged) {
            long nowMs = System.currentTimeMillis();
            long duration = contentAnimTo > contentAnimFrom ? CONTENT_SHOW_MS : CONTENT_HIDE_MS;
            if (duration <= 0L) {
                contentAnim = contentAnimTo;
            } else {
                float p = clamp01((nowMs - contentAnimStartMs) / (float) duration);
                contentAnim = contentAnimFrom + (contentAnimTo - contentAnimFrom) * p;
            }
        } else {
            contentAnim = 1f;
        }

        if (usesSplitAppearanceAnimation()) {
            renderSplitAppearance(ctx, delta, frameDelta);
            return;
        }

        long now = System.currentTimeMillis();
        float appearProgress = clamp01((now - spawnTime) / (float) APPEAR_MS);
        float openCurve = easeOutBack(appearProgress);
        float closeProgress = removeTime < 0L
                ? 0f
                : clamp01((now - removeTime) / (float) REMOVE_MS);
        float closeCurve = RenderUtil.easeInOutCubic(closeProgress);
        float scale = removeTime < 0L
                ? (2f - openCurve)
                : (1f - 0.22f * closeCurve);
        float alpha = removeTime < 0L
                ? easeOutQuad(appearProgress)
                : (1f - closeCurve);

        float runtimeScale = 1f;
        if (contentVisibilityManaged) {
            float p = clamp01(contentAnim);
            float runtimeFade = contentVisibleTarget ? easeOutQuad(p) : RenderUtil.easeInOutCubic(p);
            alpha *= runtimeFade;

            float runtimeClosePart = 1f - RenderUtil.easeInOutCubic(p);
            runtimeScale = contentVisibleTarget
                    ? (2f - easeOutBack(p))
                    : (1f + runtimeClosePart * 0.45f);
        }

        if (alpha <= 0.01f) {
            return;
        }

        boolean dragging = DragAndDrop.getInstance().getDragging() == this;
        float targetTiltDeg = dragging ? clamp(dragVelocityX * 2.8f, -9f, 9f) : 0f;
        tiltAnimDeg = RenderUtil.animate(tiltAnimDeg, targetTiltDeg, dragging ? 0.16f : 0.22f, frameDelta);

        float centerX = x + width * 0.5f;
        float centerY = y + height * 0.5f;
        float finalScale = scale * runtimeScale;
        float hudScale = hudScale();

        ctx.getMatrices().push();
        ctx.getMatrices().scale(hudScale, hudScale, 1f);
        float previousScissorScale = setActiveScissorScale(hudScale);
        ctx.getMatrices().translate(centerX, centerY, 0);
        ctx.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.toRadians(tiltAnimDeg)));
        ctx.getMatrices().scale(finalScale, finalScale, 1f);
        ctx.getMatrices().translate(-centerX, -centerY, 0);

        boolean clipElement = removeTime >= 0L;
        try {
            if (clipElement) {
                RenderUtil.enableScissor(x, y, width, height);
            }
            renderContent(ctx, x, y, width, height, delta, alpha);
        } finally {
            if (clipElement) {
                RenderUtil.disableScissor();
            }
            setActiveScissorScale(previousScissorScale);
            ctx.getMatrices().pop();
        }
    }

    private void renderSplitAppearance(DrawContext ctx, float delta, float frameDelta) {
        long now = System.currentTimeMillis();
        float appearProgress = clamp01((now - spawnTime) / (float) APPEAR_MS);
        float closeProgress = removeTime < 0L
                ? 0f
                : clamp01((now - removeTime) / (float) REMOVE_MS);

        if (removeTime < 0L) {
            headerRenderAlpha = easeOutQuad(clamp01(appearProgress / 0.42f));
            float bodyProgress = clamp01((appearProgress - 0.38f) / 0.62f);
            bodyRevealProgress = RenderUtil.easeInOutCubic(bodyProgress);
            bodyRenderAlpha = easeOutQuad(bodyProgress);
        } else {
            float bodyClose = RenderUtil.easeInOutCubic(clamp01(closeProgress / 0.64f));
            bodyRevealProgress = 1f - bodyClose;
            bodyRenderAlpha = 1f - bodyClose;
            float headerClose = RenderUtil.easeInOutCubic(clamp01((closeProgress - 0.60f) / 0.40f));
            headerRenderAlpha = 1f - headerClose;
        }

        if (contentVisibilityManaged) {
            float p = clamp01(contentAnim);
            if (contentVisibleTarget) {
                float headerProgress = clamp01(p / 0.42f);
                float bodyProgress = clamp01((p - 0.38f) / 0.62f);
                headerRenderAlpha *= easeOutQuad(headerProgress);
                bodyRevealProgress *= RenderUtil.easeInOutCubic(bodyProgress);
                bodyRenderAlpha *= easeOutQuad(bodyProgress);
            } else {
                headerRenderAlpha *= easeOutQuad(clamp01(p / 0.40f));
                bodyRevealProgress *= RenderUtil.easeInOutCubic(p);
                bodyRenderAlpha *= easeOutQuad(p);
            }
        }
        bodySlideOffset = (1f - bodyRevealProgress) * -8f;

        float alpha = Math.max(headerRenderAlpha, bodyRenderAlpha);
        if (alpha <= 0.01f) {
            return;
        }

        boolean dragging = DragAndDrop.getInstance().getDragging() == this;
        float targetTiltDeg = dragging ? clamp(dragVelocityX * 2.8f, -9f, 9f) : 0f;
        tiltAnimDeg = RenderUtil.animate(tiltAnimDeg, targetTiltDeg, dragging ? 0.16f : 0.22f, frameDelta);

        float centerX = x + width * 0.5f;
        float centerY = y + height * 0.5f;
        float hudScale = hudScale();

        ctx.getMatrices().push();
        ctx.getMatrices().scale(hudScale, hudScale, 1f);
        float previousScissorScale = setActiveScissorScale(hudScale);
        ctx.getMatrices().translate(centerX, centerY, 0);
        ctx.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.toRadians(tiltAnimDeg)));
        ctx.getMatrices().translate(-centerX, -centerY, 0);

        try {
            renderContent(ctx, x, y, width, height, delta, alpha);
        } finally {
            setActiveScissorScale(previousScissorScale);
            ctx.getMatrices().pop();
        }
    }

    protected void update(float delta) {
    }

    protected abstract void renderContent(DrawContext ctx, float x, float y,
                                          float w, float h, float delta, float alpha);

    protected final void drawMenuStyleBackground(DrawContext ctx, float x, float y, float w, float h,
                                                 float radius, float alpha) {
        radius *= CustomTheme.hudRadiusScale();
        RenderUtil.drawShadow(ctx, x, y, w, h, radius, 8.5f,
                RenderUtil.withAlpha(0x000000, alpha * 0.22f));
        GlassUtil.drawGlassPanel(ctx, x, y, w, h, radius,
                CustomTheme.hudPanelTint(),
                alpha * CustomTheme.hudPanelTintAlpha(),
                alpha * alpha);
    }

    protected final <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    protected final void setContentVisibleTarget(boolean visible) {
        contentVisibilityManaged = true;
        if (contentVisibleTarget == visible && contentAnimTo == (visible ? 1f : 0f)) {
            return;
        }

        contentVisibleTarget = visible;
        contentAnimFrom = clamp01(contentAnim);
        contentAnimTo = visible ? 1f : 0f;
        contentAnimStartMs = System.currentTimeMillis();

        if (Math.abs(contentAnimFrom - contentAnimTo) < 0.001f) {
            contentAnim = contentAnimTo;
        }
    }

    protected final float getContentAnim() {
        return contentAnim;
    }

    protected boolean usesSplitAppearanceAnimation() {
        return false;
    }

    protected final float getHeaderRenderAlpha() {
        return headerRenderAlpha;
    }

    protected final float getBodyRenderAlpha() {
        return bodyRenderAlpha;
    }

    protected final float getBodyRevealProgress() {
        return bodyRevealProgress;
    }

    protected final float getBodySlideOffset() {
        return bodySlideOffset;
    }

    protected final boolean isContentVisibleTarget() {
        return contentVisibleTarget;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public List<Setting<?>> getVisibleSettings() {
        List<Setting<?>> visible = new ArrayList<>();
        for (Setting<?> setting : settings) {
            if (setting.isVisible()) {
                visible.add(setting);
            }
        }
        return visible;
    }

    public boolean contains(float mx, float my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public static float hudScale() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return 1.0f;
        }
        float guiScale = (float) client.getWindow().getScaleFactor();
        if (guiScale <= 0.001f) {
            return 1.0f;
        }
        return REFERENCE_GUI_SCALE / guiScale;
    }

    public static float screenToHud(float coordinate) {
        return coordinate / hudScale();
    }

    public static float hudScreenWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getWindow().getScaledWidth() / hudScale();
    }

    public static float hudScreenHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getWindow().getScaledHeight() / hudScale();
    }

    public static float activeScissorScale() {
        return activeScissorScale;
    }

    public static float setActiveScissorScale(float scale) {
        float previous = activeScissorScale;
        activeScissorScale = Math.max(0.001f, scale);
        return previous;
    }

    public boolean isDraggable() {
        return true;
    }

    public boolean isRemovable() {
        return true;
    }
    public void moveSetting(Setting<?> setting, int offset) {
        int index = settings.indexOf(setting);
        if (index < 0) return;
        int newIndex = (int) clamp(index + offset, 0, settings.size() - 1);
        if (index != newIndex) {
            settings.remove(index);
            settings.add(newIndex, setting);
        }
    }

    public void markRemoving() {
        if (removeTime < 0L) {
            removeTime = System.currentTimeMillis();
        }
    }

    public boolean isRemoving() {
        return removeTime >= 0L;
    }

    public boolean isExpired(long now) {
        return removeTime >= 0L && now - removeTime >= REMOVE_MS;
    }

    public void setDragVelocity(float dragVelocityX, float dragVelocityY) {
        this.dragVelocityX = dragVelocityX;
        this.dragVelocityY = dragVelocityY;
    }

    public void clearDragVelocity() {
        this.dragVelocityX = 0f;
        this.dragVelocityY = 0f;
    }

    public void setDragGrip(float gripX, float gripY) {
                                                                                           
    }

    public void clearDragGrip() {
                                                                     
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
        for (Setting<?> setting : settings) {
            setting.setOwnerModuleId(typeId);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Identifier getIcon() {
        return icon;
    }

    public void setIcon(Identifier icon) {
        this.icon = icon;
    }

    protected static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    protected static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    protected static float easeOutQuad(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    protected static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
    }

    protected static float easeInOutQuad(float t) {
        return t < 0.5f
                ? 2f * t * t
                : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }
}
