package fun.enoughvisuals.ui.clickgui.components;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Map;

public class MenuModule {
    private static final Map<String, Identifier> MODULE_ICONS = Map.of(
            "configs", Identifier.of("enough", "icons/client/configs.png"),
            "friends", Identifier.of("enough", "icons/client/friends.png"),
            "gps", Identifier.of("enough", "icons/client/gps.png")
    );

    private final Module module;
    private final MenuScreen screen;
    private final MenuScreen.PanelData panel;

    private float hoverAnim = 0f;
    private float enableAnim = 0f;

    private boolean shaking = false;
    private long shakeStart = 0L;
    private float shakeOffset = 0f;
    private long searchHighlightStartMs = 0L;

    public MenuModule(Module module, MenuScreen screen, MenuScreen.PanelData panel) {
        this.module = module;
        this.screen = screen;
        this.panel = panel;
    }

    public void render(DrawContext ctx, float x, float y, float w, float h,
                       int mx, int my, float delta, float alpha, float uiScale) {
        if (alpha <= 0.01f) return;

        boolean hovered = isHovered(mx, my, x, y, w, h);
        boolean available = module.isAvailable();
        float frameDelta = RenderUtil.getFrameDelta();
        hoverAnim = RenderUtil.animate(hoverAnim, hovered ? 1f : 0f, 0.58f, frameDelta);
        enableAnim = RenderUtil.animate(enableAnim, module.isEnabled() ? 1f : 0f, 0.26f, frameDelta);

        if (shaking) {
            float elapsed = (System.currentTimeMillis() - shakeStart) / 350f;
            if (elapsed >= 1f) {
                shaking = false;
                shakeOffset = 0f;
            } else {
                shakeOffset = (float) Math.sin(elapsed * Math.PI * 5f) * (1f - elapsed) * 2f;
            }
        }

        float drawX = x + shakeOffset;
        float searchHighlight = Math.max(0f, 1f - (System.currentTimeMillis() - searchHighlightStartMs) / 900f);
        if (searchHighlight > 0f) {
            float pulse = RenderUtil.easeOutQuad(searchHighlight);
            RenderUtil.drawRoundedRect(ctx, drawX + 3f * uiScale, y + 1f * uiScale, w - 6f * uiScale, h - 2f * uiScale,
                    5f * uiScale, RenderUtil.withAlpha(MenuScreen.ACCENT, alpha * pulse * 0.34f));
        }
        float fontSize = 7f * uiScale;
        float leftPad = 8f * uiScale;
        Identifier moduleIcon = MODULE_ICONS.get(module.getId());
        float iconReserve = moduleIcon == null ? 0f : 11f * uiScale;
        float textOffset = leftPad + iconReserve + (moduleIcon == null ? enableAnim * 9f * uiScale : 0f);
        float rightPad = 8f * uiScale;
        boolean hasSettings = !module.getSettings().isEmpty();
        boolean binding = screen.isBindingModule(module);
        boolean blocked = ModuleManager.getInstance().isBlocked(module);

        if (hovered && module.getDescription() != null && !module.getDescription().isEmpty()) {
            screen.setHoveredDescription(module.getDescription());
        }

        if (moduleIcon != null) {
            float iconSize = 8f * uiScale;
            float iconX = drawX + leftPad;
            float iconY = y + (h - iconSize) * 0.5f;
            Draw.imageTinted(ctx, moduleIcon, iconX, iconY, iconSize, iconSize,
                    module.isEnabled() ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_DIM,
                    alpha * (0.72f + hoverAnim * 0.22f));
        }

        float bindW = 0f;
        if (!binding) {
            String bindText = BindKeyUtil.getBindName(module.getKeyBind());
            if (!"None".equals(bindText)) {
                bindW = Math.min(34f * uiScale, CustomFontRenderer.getTextWidth(bindText, 5.6f * uiScale, FontWeight.REGULAR) + 8f * uiScale);
            }
        } else {
            bindW = 18f * uiScale;
        }

        float rightReserve = (hasSettings ? 14f * uiScale : 8f * uiScale) + bindW;
        float maxTextW = Math.max(18f * uiScale, w - textOffset - rightReserve);
        String label = blocked ? module.getName() + " locked" : module.getName();
        String name = CustomFontRenderer.trimToWidth(label, maxTextW, fontSize, FontWeight.REGULAR);
        int nameColor = blocked ? RenderUtil.withAlpha(0xFFFF8F8F, alpha * 0.92f) : RenderUtil.lerpColor(
                RenderUtil.withAlpha(available ? MenuScreen.TEXT_GRAY : MenuScreen.TEXT_DIM, alpha * (available ? 0.95f : 0.48f)),
                RenderUtil.withAlpha(available ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_GRAY, alpha * (available ? 1.0f : 0.58f)),
                0.16f + 0.72f * enableAnim + 0.20f * hoverAnim
        );
        CustomFontRenderer.drawText(ctx, name, drawX + textOffset, y + (h - fontSize) / 2f, fontSize,
                nameColor, FontWeight.REGULAR);

        if (moduleIcon == null && (module.isEnabled() || enableAnim > 0.01f)) {
            float onSize = 7f * uiScale;
            float onX = drawX + leftPad + (1f - enableAnim) * (-2f * uiScale);
            float onY = y + (h - onSize) * 0.5f + 0.35f * uiScale;
            Draw.image(ctx, MenuScreen.MODULE_ON_ICON, onX, onY, onSize, onSize,
                    MenuScreen.MOD_ICON_TEX, MenuScreen.MOD_ICON_TEX, alpha * enableAnim);
        }

        if (hasSettings) {
            float arrowH = 8f * uiScale;
            float arrowW = arrowH * (19f / 38f);
            float arrowX = drawX + w - rightPad - arrowW;
            float arrowY = y + (h - arrowH) * 0.5f;
            Draw.image(ctx, MenuScreen.MOD_ARROW_ICON, arrowX, arrowY, arrowW, arrowH,
                    MenuScreen.CAT_ICON_TEX, MenuScreen.CAT_ICON_TEX, alpha * (0.65f + 0.35f * hoverAnim));
        }

        if (bindW > 0f) {
            String bindText = binding ? "..." : BindKeyUtil.getBindName(module.getKeyBind());
            float pillH = 9.2f * uiScale;
            float pillW = Math.max(18f * uiScale, bindW);
            float pillX = drawX + w - (hasSettings ? 17f * uiScale : 9f * uiScale) - pillW;
            float pillY = y + (h - pillH) * 0.5f;
            int pillColor = binding
                    ? RenderUtil.withAlpha(MenuScreen.ACCENT, alpha * 0.26f)
                    : RenderUtil.withAlpha(0xFFFFFF, alpha * 0.05f);
            RenderUtil.drawRoundedRect(ctx, pillX, pillY, pillW, pillH, 3.5f * uiScale, pillColor);
            CustomFontRenderer.drawCenteredText(ctx, bindText, pillX + pillW * 0.5f, pillY + 2.0f * uiScale, 5.2f * uiScale,
                    RenderUtil.withAlpha(binding ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_DIM, alpha * 0.95f), FontWeight.REGULAR);
        }
    }

    public boolean isHovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void shake() {
        shaking = true;
        shakeStart = System.currentTimeMillis();
    }

    public void highlightFromSearch() {
        searchHighlightStartMs = System.currentTimeMillis();
    }

    public Module getModule() { return module; }
    public MenuScreen.PanelData getPanel() { return panel; }

    public boolean isArrowHovered(float mx, float my, float x, float y, float w, float h, float uiScale) {
        if (module.getSettings().isEmpty()) return false;
        float arrowH = 8f * uiScale;
        float arrowW = arrowH * (19f / 38f);
        float rightPad = 8f * uiScale;
        float arrowX = x + w - rightPad - arrowW - 2f * uiScale;
        float arrowY = y + (h - arrowH) * 0.5f - 2f * uiScale;
        float hitW = arrowW + 4f * uiScale;
        float hitH = arrowH + 4f * uiScale;
        return mx >= arrowX && mx <= arrowX + hitW && my >= arrowY && my <= arrowY + hitH;
    }
}
