package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import fun.enoughvisuals.systems.modules.modules.utils.LockSlot;
import fun.enoughvisuals.systems.modules.modules.visual.Cooldowns;
import fun.enoughvisuals.systems.modules.modules.visual.MouseParticles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void enough$renderCooldowns(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Cooldowns.renderInventoryOverlay(context, delta, (HandledScreen<?>) (Object) this);
        MouseParticles.renderScreen(context, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        LockSlot module = LockSlot.getInstance();
        if (module != null && module.isEnabled() && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {

            // Получаем настоящий индекс
            int trueIndex = module.getTruePlayerSlotIndex(this.focusedSlot);

            if (trueIndex != -1) {
                module.toggleLock(trueIndex);
                cir.setReturnValue(true); // Отменяем ванильный клик
            }
        }
    }

    // Рисуем красный оверлей на заблокированных слотах
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        LockSlot module = LockSlot.getInstance();
        if (module != null && module.isEnabled()) {

            int trueIndex = module.getTruePlayerSlotIndex(slot);

            if (trueIndex != -1 && module.isLocked(trueIndex)) {
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 300f); // Рисуем поверх предмета в креативе
                context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x60FF0000);
                context.getMatrices().pop();
            }
        }
    }

    // Отрисовка текстовой подсказки на экране
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        LockSlot module = LockSlot.getInstance();
        if (module != null && module.isEnabled()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            String text = "§fНажми §eСКМ §fпо слоту, чтобы заблокировать его";
            context.drawTextWithShadow(mc.textRenderer, text, 515, 220, 0xFFFFFFFF);
        }
    }

}
