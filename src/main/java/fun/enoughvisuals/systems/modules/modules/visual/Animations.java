package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.option.Perspective;

@Register(name = "Animations", id = "animations", description = "Анимирует стандартный интерфейс Minecraft", category = Category.VISUAL)
public class Animations extends Module {

    public static Animations INSTANCE;

    private long chatOpenedAtMs = 0L;
    private long chatMessageAtMs = 0L;
    private long perspectiveSwitchAtMs = 0L;
    private Perspective perspectiveFrom = Perspective.FIRST_PERSON;
    private Perspective perspectiveTo = Perspective.FIRST_PERSON;

    public Animations() {
        INSTANCE = this;
    }

    public static Animations active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public static void markChatOpened() {
        Animations module = active();
        if (module != null) {
            module.chatOpenedAtMs = System.currentTimeMillis();
        }
    }

    public static void markChatMessage() {
        Animations module = active();
        if (module != null) {
            module.chatMessageAtMs = System.currentTimeMillis();
        }
    }

    public static void markPerspectiveSwitch() {
        markPerspectiveSwitch(Perspective.FIRST_PERSON, Perspective.FIRST_PERSON);
    }

    public static void markPerspectiveSwitch(Perspective from, Perspective to) {
        Animations module = active();
        if (module != null) {
            module.perspectiveFrom = from == null ? Perspective.FIRST_PERSON : from;
            module.perspectiveTo = to == null ? Perspective.FIRST_PERSON : to;
            module.perspectiveSwitchAtMs = System.currentTimeMillis();
        }
    }

    public float chatOpenProgress(boolean focused) {
        if (!focused || chatOpenedAtMs <= 0L) return 1f;
        return progress(chatOpenedAtMs, 260f);
    }

    public float chatMessageProgress() {
        if (chatMessageAtMs <= 0L) return 1f;
        return progress(chatMessageAtMs, 310f);
    }

    public float perspectiveDistanceProgress() {
        if (perspectiveSwitchAtMs <= 0L) return 1f;
        return progress(perspectiveSwitchAtMs, 640f);
    }

    public boolean isFrontPerspectiveTransition() {
        return perspectiveTo == Perspective.THIRD_PERSON_FRONT;
    }

    public boolean isBackToFrontPerspectiveTransition() {
        return perspectiveFrom == Perspective.THIRD_PERSON_BACK && perspectiveTo == Perspective.THIRD_PERSON_FRONT;
    }

    private float progress(long startedAtMs, float durationMs) {
        float raw = (System.currentTimeMillis() - startedAtMs) / durationMs;
        return Math.max(0f, Math.min(1f, RenderUtil.easeInOutCubic(raw)));
    }
}
