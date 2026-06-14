package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;

@Register(name="Auto Resell", id="auto_resell", description="Авто перевыставление предметов на фт", category = Category.UTILS)
public class AutoResell extends Module {

    private long timer = 0L;
    private State currentState = State.IDLE;

    private enum State {
        IDLE,
        COMMAND_SEND,
        OPEN_STORAGE,
        CLICK_RESELL,
        CLOSE_AND_JUMP,
        WAIT_CYCLE
    }

    @Override
    public void onEnable() {
        timer = System.currentTimeMillis();
        currentState = State.COMMAND_SEND;
    }

    @Override
    public void onDisable() {
        currentState = State.IDLE;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        long delay = 1100L;

        switch (currentState) {
            case COMMAND_SEND -> {
                if (now - timer >= delay) {
                    mc.player.networkHandler.sendChatCommand("ah");
                    timer = now;
                    currentState = State.OPEN_STORAGE;
                }
            }

            case OPEN_STORAGE -> {
                if (now - timer >= delay && mc.currentScreen instanceof HandledScreen<?> screen) {
                    int slot = findSlotByText(screen, "хранилище");
                    if (slot != -1) {
                        click(screen, slot);
                        timer = now;
                        currentState = State.CLICK_RESELL;
                    }
                }
            }

            case CLICK_RESELL -> {
                if (now - timer >= delay && mc.currentScreen instanceof HandledScreen<?> screen) {
                    int slot = findSlotByText(screen, "перевыставить");
                    if (slot != -1) {
                        click(screen, slot);
                        timer = now;
                        currentState = State.CLOSE_AND_JUMP;
                    }
                }
            }

            case CLOSE_AND_JUMP -> {
                if (now - timer >= delay) {
                    mc.player.closeHandledScreen();
                    mc.options.jumpKey.setPressed(true);
                    new Thread(() -> {
                        try { Thread.sleep(150); } catch (Exception ignored) {}
                        mc.options.jumpKey.setPressed(false);
                    }).start();

                    timer = now;
                    currentState = State.WAIT_CYCLE;
                }
            }

            case WAIT_CYCLE -> {
                if (now - timer >= 61000L) {
                    timer = now;
                    currentState = State.COMMAND_SEND;
                }
            }
        }
    }

    private int findSlotByText(HandledScreen<?> screen, String text) {
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            ItemStack stack = screen.getScreenHandler().getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(text.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    private void click(HandledScreen<?> screen, int slotId) {
        mc.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slotId,
                0,
                SlotActionType.PICKUP,
                mc.player
        );
    }
}