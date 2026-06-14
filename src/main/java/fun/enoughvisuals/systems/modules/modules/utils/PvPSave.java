package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

@Register(
        name = "PvP Save",
        id = "pvp_save",
        description = "Защищает от случайного выхода во время PvP",
        category = Category.UTILS
)
public final class PvPSave extends Module {
    private static final long PVP_DURATION_MS = 30_000L;
    private static final long ATTACK_CONFIRM_WINDOW_MS = 1_500L;
    private static PvPSave instance;

    private int pendingTargetId = -1;
    private long pendingAttackAt;
    private long pvpUntil;
    private String blockedCommand;
    private long blockedCommandAt;
    private boolean exitBypass;

    public PvPSave() {
        instance = this;
    }

    public static void recordAttack(Entity target) {
        PvPSave module = active();
        if (module == null || !(target instanceof PlayerEntity)) return;
        module.pendingTargetId = target.getId();
        module.pendingAttackAt = System.currentTimeMillis();
    }

    public static void confirmDamage(int entityId, int sourceEntityId) {
        PvPSave module = active();
        if (module == null) return;

        long now = System.currentTimeMillis();
        if (module.pendingTargetId == entityId
                && now - module.pendingAttackAt <= ATTACK_CONFIRM_WINDOW_MS) {
            module.pvpUntil = now + PVP_DURATION_MS;
            module.pendingTargetId = -1;
        }
    }

    public static boolean blockCommand(String command) {
        PvPSave module = active();
        if (module == null || !module.isInPvP() || !isProtectedCommand(command)) return false;

        String normalized = normalizeCommand(command);
        long now = System.currentTimeMillis();
        if (normalized.equals(module.blockedCommand) && now - module.blockedCommandAt <= 5_000L) {
            module.blockedCommand = null;
            return false;
        }

        module.blockedCommand = normalized;
        module.blockedCommandAt = now;
        ClientActionUtil.clientMessage("Вы в PvP. Повторите /" + normalized + ", чтобы выполнить команду.");
        return true;
    }

    public static boolean requestExit(Screen returnScreen, Runnable exitAction) {
        PvPSave module = active();
        if (module == null || !module.isInPvP() || module.exitBypass) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        long seconds = Math.max(1L, (module.pvpUntil - System.currentTimeMillis() + 999L) / 1_000L);
        GLFW.glfwSetWindowShouldClose(client.getWindow().getHandle(), false);
        client.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        module.exitBypass = true;
                        exitAction.run();
                    } else {
                        client.setScreen(returnScreen);
                    }
                },
                Text.literal("Выйти во время PvP?"),
                Text.literal("До конца PvP осталось " + seconds + " сек."),
                Text.literal("Да, выйти"),
                Text.literal("Нет")
        ));
        return true;
    }

    public static boolean isExitBypassed() {
        PvPSave module = active();
        return module != null && module.exitBypass;
    }

    @Override
    protected void onDisable() {
        reset();
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        if (!isInGame()) {
            reset();
            return;
        }
        if (now - pendingAttackAt > ATTACK_CONFIRM_WINDOW_MS) pendingTargetId = -1;
        if (now >= pvpUntil) pvpUntil = 0L;
        if (now - blockedCommandAt > 5_000L) blockedCommand = null;
    }

    private boolean isInPvP() {
        return System.currentTimeMillis() < pvpUntil;
    }

    private void reset() {
        pendingTargetId = -1;
        pendingAttackAt = 0L;
        pvpUntil = 0L;
        blockedCommand = null;
        blockedCommandAt = 0L;
        exitBypass = false;
    }

    private static PvPSave active() {
        return instance != null && instance.isEnabled() ? instance : null;
    }

    private static boolean isProtectedCommand(String command) {
        return normalizeCommand(command).equals("hub");
    }

    private static String normalizeCommand(String command) {
        if (command == null) return "";
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        int space = normalized.indexOf(' ');
        return space < 0 ? normalized : normalized.substring(0, space);
    }
}
