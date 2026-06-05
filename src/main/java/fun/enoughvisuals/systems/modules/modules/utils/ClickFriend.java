package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

@Register(
        name = "Click Friend",
        id = "click_friend",
        description = "Добавляет или удаляет друга кликом по игроку",
        category = Category.UTILS
)
public class ClickFriend extends Module {

    public final BindSetting button = add(new BindSetting(
            "Кнопка",
            "button",
            "Кнопка добавления или удаления друга",
            BindKeyUtil.mouseButtonToCode(GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
    ));

    private boolean bindPressed;

    @Override
    public void onTick() {
        if (!isInGame() || mc.currentScreen != null) {
            bindPressed = false;
            return;
        }

        long window = mc.getWindow().getHandle();
        boolean pressed = isBindPressed(window);
        if (!pressed) {
            bindPressed = false;
            return;
        }

        if (bindPressed) {
            return;
        }

        bindPressed = true;

        if (!(mc.crosshairTarget instanceof EntityHitResult hitResult)) {
            return;
        }

        Entity entity = hitResult.getEntity();
        if (!(entity instanceof PlayerEntity playerEntity)) {
            return;
        }

        if (mc.player != null && playerEntity == mc.player) {
            return;
        }

        FriendRepo.toggleFriend(playerEntity.getName().getString());
    }

    private boolean isBindPressed(long window) {
        if (!button.isBound()) {
            return false;
        }

        int bindCode = button.get();
        if (bindCode >= 1000) {
            return GLFW.glfwGetMouseButton(window, bindCode - 1000) == GLFW.GLFW_PRESS;
        }

        return GLFW.glfwGetKey(window, bindCode) == GLFW.GLFW_PRESS;
    }
}
