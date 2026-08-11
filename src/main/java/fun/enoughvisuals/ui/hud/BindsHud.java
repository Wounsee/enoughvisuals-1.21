package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.hud.HudListAnimator;
import fun.enoughvisuals.systems.hud.HudMotion;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.HudIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BindsHud extends HudElement {
    private static final float FIGMA_WIDTH = 237f;
    private static final float FIGMA_BASE_HEIGHT = 97f;
    private static final float FIGMA_ROW_STEP = 28f;
    private static final float FIGMA_TO_HUD = 1.8f;

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    private final HudListAnimator rows = new HudListAnimator();
    private final Map<String, BindLine> lines = new LinkedHashMap<>();
    private float stableVisibleHeight = 76.5f;

    public BindsHud(float x, float y) {
        super(x, y, 118.5f, 76.5f);
    }

    @Override
    protected void update(float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        float frameDelta = RenderUtil.getFrameDelta();
        float s = HudDesign.scale(scale, 0.90f);
        float u = s / FIGMA_TO_HUD;
        boolean editing = client.currentScreen instanceof ChatScreen;

        Map<String, BindLine> next = new LinkedHashMap<>();
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.isEnabled() && module.getKeyBind() != BindKeyUtil.NONE) {
                next.put(module.getId(), new BindLine(module.getName(),
                        BindKeyUtil.getBindName(module.getKeyBind())));
            }
        }
        if (next.isEmpty() && editing) {
            next.put("preview-fthelper", new BindLine("FTHelper", "V"));
            next.put("preview-sprint", new BindLine("Auto Sprint", "Z"));
            next.put("preview-swap", new BindLine("Item Swap", "X"));
        }

        lines.putAll(next);
        List<String> keys = new ArrayList<>(next.keySet());
        rows.update(keys, FIGMA_ROW_STEP * u, frameDelta);
        List<HudListAnimator.Row> animatedRows = rows.rows();
        lines.keySet().removeIf(key -> animatedRows.stream().noneMatch(row -> row.key().equals(key)));
        boolean visible = !animatedRows.isEmpty();
        setContentVisibleTarget(visible);

        float targetH = stableVisibleHeight;
        if (visible) {
            targetH = FIGMA_BASE_HEIGHT * u
                    + animatedRows.get(animatedRows.size() - 1).position();
            stableVisibleHeight = targetH;
        }
        setWidth(HudMotion.damp(getWidth(), FIGMA_WIDTH * u, 0.22f, frameDelta));
        setHeight(HudMotion.damp(getHeight(), targetH, 0.22f, frameDelta));
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        if (h <= 0.5f) return;
        float s = HudDesign.scale(scale, 0.90f);
        float u = s / FIGMA_TO_HUD;

        HudDesign.drawRounderPanel(ctx, x, y, w, h, 23f * u, alpha);
        RenderUtil.enableScissor(x, y, w, h);
        CustomFontRenderer.drawText(ctx, HudIcons.KEYBOARD, x + 16f * u, y + 6f * u,
                30f * u, RenderUtil.withAlpha(HudDesign.ACCENT, alpha), FontWeight.HUD_ICONS);
        CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.hud.binds", "Бинды"), x + 55f * u, y + 11f * u,
                16f * u, RenderUtil.withAlpha(HudDesign.TEXT, alpha), FontWeight.MEDIUM);
        RenderUtil.drawRoundedRect(ctx, x, y + 41f * u, w, 2f * u, 1f * u,
                RenderUtil.withAlpha(0xFF1B1B1C, alpha * 0.89f));

        float bodyStart = y + 57f * u;
        for (HudListAnimator.Row row : rows.rows()) {
            BindLine line = lines.get(row.key());
            if (line == null) continue;
            float panelReveal = clamp01((getContentAnim() - 0.18f) / 0.82f);
            float rowAlpha = alpha * RenderUtil.easeOutQuad(row.visibility())
                    * RenderUtil.easeOutQuad(panelReveal);
            float rowY = bodyStart + row.position();
            if (rowAlpha <= 0.01f || rowY > y + h) continue;

            float rowScale = 0.94f + 0.06f * row.visibility();
            float centerY = rowY + 11f * u;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x + w * 0.5f, centerY, 0f);
            ctx.getMatrices().scale(rowScale, rowScale, 1f);
            ctx.getMatrices().translate(-(x + w * 0.5f), -centerY, 0f);

            float rightEdge = x + w - 13f * u;
            float textWidth = CustomFontRenderer.getTextWidth(line.key(), 14f * u, FontWeight.MEDIUM);
            float capW = Math.max(22f * u, textWidth + 8f * u);
            float capH = 22f * u;
            float capX = rightEdge - capW;
            String name = CustomFontRenderer.trimToWidth(line.name(),
                    Math.max(10f * u, capX - (x + 20f * u) - 8f * u),
                    14f * u, FontWeight.MEDIUM);
            CustomFontRenderer.drawText(ctx, name, x + 20f * u, rowY,
                    14f * u, RenderUtil.withAlpha(HudDesign.TEXT, rowAlpha), FontWeight.MEDIUM);

            RenderUtil.drawRoundedRect(ctx, capX, rowY, capW, capH,
                    6f * u, RenderUtil.withAlpha(HudDesign.ACCENT, rowAlpha * 0.20f));
            CustomFontRenderer.drawCenteredText(ctx, line.key(), capX + capW * 0.5f,
                    rowY + 2f * u, 14f * u,
                    RenderUtil.withAlpha(HudDesign.ACCENT, rowAlpha), FontWeight.MEDIUM);
            ctx.getMatrices().pop();
        }
        RenderUtil.disableScissor();
    }

    private record BindLine(String name, String key) {
    }

    @Override
    protected float getHiddenContentScale() {
        return 0.84f;
    }
}
