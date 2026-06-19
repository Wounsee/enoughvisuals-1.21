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
    public final BooleanSetting vanillaTabAnimation = add(new BooleanSetting("Vanilla TAB", "vanilla_tab_animation", "Плавное открытие стандартного списка игроков", true));
    public final BooleanSetting perspectiveAnimation = add(new BooleanSetting("Перспектива", "perspective_animation", "Плавная смена дистанции камеры от первого к третьему лицу", true));

    private static final long VANILLA_TAB_DURATION_MS = 350L;
    private static final long PERSPECTIVE_DURATION_MS = 500L;

    private long chatOpenedAtMs = 0L;
    private long chatMessageAtMs = 0L;
    private long debugHudChangedAtMs = 0L;
    private boolean debugHudTargetVisible = false;
    private final TimedToggle playerList = new TimedToggle();
    private final TimedToggle perspective = new TimedToggle();

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

    public void updatePlayerListShown(boolean shown) {
        playerList.setTarget(shown, VANILLA_TAB_DURATION_MS);
        playerList.update();
    }

    public float playerListProgress() {
        return vanillaTabAnimation.get() ? playerList.value() : 1f;
    }

    public boolean shouldRenderPlayerList() {
        return !vanillaTabAnimation.get() || playerList.renderable();
    }

    public void updatePerspective(boolean thirdPerson) {
        perspective.setTarget(thirdPerson, PERSPECTIVE_DURATION_MS);
        perspective.update();
    }

    public float perspectiveDistance(float vanillaDistance) {
        if (!perspectiveAnimation.get()) return vanillaDistance;
        float progress = perspective.value();
        return 1.0f + (vanillaDistance - 1.0f) * progress;
    }

    private float progress(long startedAtMs, float durationMs) {
        float raw = (System.currentTimeMillis() - startedAtMs) / durationMs;
        return Math.max(0f, Math.min(1f, RenderUtil.easeInOutCubic(raw)));
    }

    private static final class TimedToggle {
        private float value;
        private float startValue;
        private float targetValue;
        private long startTimeMs;
        private long durationMs = 200L;
        private boolean targetVisible;
        private boolean renderable;

        private TimedToggle() {
            this(false);
        }

        private TimedToggle(boolean visible) {
            value = visible ? 1f : 0f;
            startValue = value;
            targetValue = value;
            targetVisible = visible;
            renderable = visible;
        }

        private void setTarget(boolean visible, long durationMs) {
            durationMs = Math.max(1L, durationMs);
            if (visible == targetVisible && this.durationMs == durationMs) return;
            this.durationMs = durationMs;
            this.startValue = value;
            this.targetValue = visible ? 1f : 0f;
            this.startTimeMs = System.currentTimeMillis();
            this.targetVisible = visible;
            this.renderable = true;
        }

        private void update() {
            if (startTimeMs == 0L) return;
            float raw = (System.currentTimeMillis() - startTimeMs) / (float) durationMs;
            if (raw >= 1f) {
                value = targetValue;
                startTimeMs = 0L;
                renderable = targetVisible || value > 0.001f;
                return;
            }
            float eased = RenderUtil.easeInOutQuad(Math.max(0f, Math.min(1f, raw)));
            value = startValue + (targetValue - startValue) * eased;
        }

        private float value() {
            return Math.max(0f, Math.min(1f, value));
        }

        private boolean renderable() {
            return renderable || value > 0.001f;
        }
    }
}
