package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.mixins.accessors.HandledScreenAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

@Register(name = "Item Scroller", id = "item_scroller", description = "Быстро переносит предметы при зажатом Shift + ЛКМ", category = Category.UTILS)
public class ItemScroller extends Module {

    // Настройка задержки в миллисекундах (от 0 до 500)
    public final SliderSetting delay = add(new SliderSetting("Задержка (мс)", "delay", "Задержка между кликами", 50, 0, 500, 10));

    private long lastClickTime = 0;
    private int lastSlotId = -1;

    @Override
    public void onTick() {
        if (!isInGame() || mc.currentScreen == null) return;

        // Работаем только если мы в каком-то инвентаре (сундук, инвентарь игрока и т.д.)
        if (mc.currentScreen instanceof HandledScreen<?> handledScreen) {

            // Проверяем, зажата ли ЛКМ и Shift
            boolean isLmbDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean isShiftDown = Screen.hasShiftDown();

            if (isShiftDown && isLmbDown) {
                // Получаем слот под мышкой через наш Accessor
                Slot hoveredSlot = ((HandledScreenAccessor) handledScreen).getFocusedSlot();

                // Проверяем, есть ли там предмет
                if (hoveredSlot != null && hoveredSlot.hasStack()) {
                    long now = System.currentTimeMillis();

                    // Кликаем, если прошло время задержки ИЛИ мы перевели мышку на новый слот
                    if (now - lastClickTime >= delay.get() || hoveredSlot.id != lastSlotId) {

                        // Отправляем пакет клика с зажатым шифтом (QUICK_MOVE)
                        mc.interactionManager.clickSlot(
                                handledScreen.getScreenHandler().syncId,
                                hoveredSlot.id,
                                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                                SlotActionType.QUICK_MOVE,
                                mc.player
                        );

                        lastClickTime = now;
                        lastSlotId = hoveredSlot.id;
                    }
                }
            } else {
                // Если кнопки отпущены — сбрасываем последний слот
                lastSlotId = -1;
            }
        }
    }
}
