package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.utils.render.RenderUtil;

@Register(name = "Animations", id = "animations", description = "Анимирует стандартный интерфейс Minecraft", category = Category.VISUAL)
public class Animations extends Module {

    public static Animations INSTANCE;

    public final BooleanSetting chatInputAnimation = add(new BooleanSetting("Поле чата", "chat_input_animation", "Анимация открытия поля чата", true));
    public final BooleanSetting chatMessageAnimation = add(new BooleanSetting("Сообщения чата", "chat_message_animation", "Анимация новых сообщений чата", true));
    public final BooleanSetting debugHudAnimation = add(new BooleanSetting("F3", "debug_hud_animation", "Плавное появление debug HUD", true));

    private long chatOpenedAtMs = 0L;
    private long chatMessageAtMs = 0L;
    private long debugHudChangedAtMs = 0L;
    private boolean debugHudTargetVisible = false;

    public Animations() {
        INSTANCE = this;
    }

    public static Animations active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public static void markChatOpened() {
        Animations module = active();
        if (module != null && module.chatInputAnimation.get()) {
            module.chatOpenedAtMs = System.currentTimeMillis();
        }
    }

    public static void markChatMessage() {
        Animations module = active();
        if (module != null && module.chatMessageAnimation.get()) {
            module.chatMessageAtMs = System.currentTimeMillis();
        }
    }

    public static void markDebugHudOpened() {
        markDebugHudToggled(true);
    }

    public static void markDebugHudToggled(boolean visible) {
        Animations module = active();
        if (module != null && module.debugHudAnimation.get()) {
            module.debugHudTargetVisible = visible;
            module.debugHudChangedAtMs = System.currentTimeMillis();
        }
    }

    public float chatOpenProgress(boolean focused) {
        if (!chatInputAnimation.get()) return 1f;
        if (!focused || chatOpenedAtMs <= 0L) return 1f;
        return progress(chatOpenedAtMs, 260f);
    }

    public float chatMessageProgress() {
        if (!chatMessageAnimation.get()) return 1f;
        if (chatMessageAtMs <= 0L) return 1f;
        return progress(chatMessageAtMs, 310f);
    }

    public float debugHudProgress() {
        if (!debugHudAnimation.get()) return 1f;
        if (debugHudChangedAtMs <= 0L) return debugHudTargetVisible ? 1f : 0f;
        float progress = progress(debugHudChangedAtMs, 430f);
        return debugHudTargetVisible ? progress : 1f - progress;
    }

    public boolean shouldRenderDebugHud() {
        if (!debugHudAnimation.get()) return debugHudTargetVisible;
        return debugHudTargetVisible || debugHudProgress() > 0.001f;
    }

    private float progress(long startedAtMs, float durationMs) {
        float raw = (System.currentTimeMillis() - startedAtMs) / durationMs;
        return Math.max(0f, Math.min(1f, RenderUtil.easeInOutCubic(raw)));
    }
}
