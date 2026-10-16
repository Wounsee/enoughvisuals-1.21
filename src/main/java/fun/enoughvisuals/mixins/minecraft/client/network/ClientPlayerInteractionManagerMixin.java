package fun.enoughvisuals.mixins.minecraft.client.network;

import fun.enoughvisuals.systems.modules.modules.utils.LockSlot;
import fun.enoughvisuals.systems.modules.modules.utils.PvPSave;
import fun.enoughvisuals.systems.modules.modules.utils.FakePlayer;
import fun.enoughvisuals.systems.modules.modules.utils.TrapTimer;
import fun.enoughvisuals.systems.modules.modules.visual.HealthIndicator;
import fun.enoughvisuals.systems.modules.modules.visual.AttackEffect;
import fun.enoughvisuals.systems.modules.modules.visual.TargetESP;
import fun.enoughvisuals.utils.sound.CombatSoundService;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void enough$recordPlayerAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEffect.onAttack(target);
        if (FakePlayer.handleLocalAttack(target)) {
            ci.cancel();
            return;
        }
        PvPSave.recordAttack(target);
        CombatSoundService.recordAttackTarget(target);
        TargetESP.onAttackAttempt(target);

        if (target instanceof PlayerEntity victim) {
            HealthIndicator indicator = HealthIndicator.getInstance();
            if (indicator != null) {
                indicator.recordAttack(victim);
            }
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void enough$onInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        TrapTimer trapTimer = TrapTimer.getInstance();
        if (trapTimer != null) {
            trapTimer.onItemUse(player, hand);
        }
    }

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void onClickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        LockSlot module = LockSlot.getInstance();
        if (module != null && module.isEnabled() && player.currentScreenHandler != null) {

            // 1. Блокируем перенос кнопками 1-9 (Свап предметов)
            if (actionType == SlotActionType.SWAP) {
                if (module.isLocked(button)) {
                    ci.cancel();
                    return;
                }
            }

            // 2. Блокируем обычные клики мышкой
            if (slotId >= 0 && slotId < player.currentScreenHandler.slots.size()) {
                Slot slot = player.currentScreenHandler.getSlot(slotId);

                // Проверяем через наш метод-распаковщик
                int trueIndex = module.getTruePlayerSlotIndex(slot);
                if (trueIndex != -1 && module.isLocked(trueIndex)) {
                    ci.cancel();
                }
            }
        }
    }
}
