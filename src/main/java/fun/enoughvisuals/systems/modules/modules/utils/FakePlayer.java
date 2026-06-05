package fun.enoughvisuals.systems.modules.modules.utils;

import com.mojang.authlib.GameProfile;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

@Register(
        name = "FakePlayer",
        id = "fake_player",
        description = "Локальный фейк игрок для теста ударов",
        category = Category.UTILS
)
public class FakePlayer extends Module {

    public static AbstractClientPlayerEntity fakePlayer;

    private static final UUID FAKE_UUID =
            UUID.fromString("70ee432d-0a96-4137-a2c0-37cc9df67f03");

    private boolean lastAttackPressed;

    @Override
    protected void onEnable() {
        if (!isInGame()) {
            disableSilent();
            return;
        }

        removeFakePlayer();

        fakePlayer = new AbstractClientPlayerEntity(
                mc.world,
                new GameProfile(FAKE_UUID, mc.player.getName().getString())
        ) {};

        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setPosition(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        fakePlayer.setYaw(mc.player.getYaw());
        fakePlayer.setHeadYaw(mc.player.getHeadYaw());
        fakePlayer.setBodyYaw(mc.player.getBodyYaw());
        fakePlayer.setPitch(mc.player.getPitch());

        fakePlayer.setHealth(20.0f);
        fakePlayer.setAbsorptionAmount(0.0f);

        fakePlayer.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
        fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 999999, 0, false, true));

        mc.world.addEntity(fakePlayer);
    }

    @Override
    protected void onDisable() {
        removeFakePlayer();
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            removeFakePlayer();
            return;
        }

        if (fakePlayer == null) {
            return;
        }

        facePlayer();
        syncHands();
        tickAttack();
    }

    private void syncHands() {
        fakePlayer.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
        fakePlayer.setMainArm(mc.player.getMainArm());

        if (mc.player.isUsingItem()) {
            fakePlayer.setCurrentHand(mc.player.getActiveHand());
        } else {
            fakePlayer.clearActiveItem();
        }
    }

    private void tickAttack() {
        boolean attackPressed = mc.options.attackKey.isPressed();

        if (!attackPressed) {
            lastAttackPressed = false;
            return;
        }

        if (lastAttackPressed) {
            return;
        }

        lastAttackPressed = true;

        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) {
            return;
        }

        if (hit.getEntity() != fakePlayer) {
            return;
        }

        if (mc.player.distanceTo(fakePlayer) > 6.0f) {
            return;
        }

        damageFakePlayer();
    }
    private float getWeaponDamage() {
        ItemStack stack = mc.player.getMainHandStack();

        float damage = 1.0f;

        if (stack.isOf(Items.WOODEN_SWORD)) damage = 4.0f;
        else if (stack.isOf(Items.STONE_SWORD)) damage = 5.0f;
        else if (stack.isOf(Items.IRON_SWORD)) damage = 6.0f;
        else if (stack.isOf(Items.DIAMOND_SWORD)) damage = 7.0f;
        else if (stack.isOf(Items.NETHERITE_SWORD)) damage = 8.0f;

        else if (stack.isOf(Items.WOODEN_AXE)) damage = 7.0f;
        else if (stack.isOf(Items.STONE_AXE)) damage = 9.0f;
        else if (stack.isOf(Items.IRON_AXE)) damage = 9.0f;
        else if (stack.isOf(Items.DIAMOND_AXE)) damage = 9.0f;
        else if (stack.isOf(Items.NETHERITE_AXE)) damage = 10.0f;

        if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            int amp = mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1;
            damage += 3.0f * amp;
        }

        if (mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
            int amp = mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1;
            damage -= 4.0f * amp;
        }

        return Math.max(0.0f, damage);
    }
    private void damageFakePlayer() {
        if (fakePlayer.hurtTime > 0) {
            play(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0f, 1.0f);
            return;
        }

        fakePlayer.hurtTime = 10;
        fakePlayer.maxHurtTime = 10;

        float cooldown = mc.player.getAttackCooldownProgress(1.0f);
        boolean cooled = cooldown >= 0.9f;

        float damage = getWeaponDamage();
        damage *= 0.2f + cooldown * cooldown * 0.8f;

        boolean crit = cooled
                && mc.player.fallDistance > 0.0f
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.hasVehicle()
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS);

        if (crit) {
            damage *= 1.5f;
            play(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        } else if (cooled) {
            play(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
        } else {
            play(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
        }

        HitSound.playHit();
        play(SoundEvents.ENTITY_PLAYER_HURT, 0.7f, 1.0f);

        fakePlayer.setHealth(MathHelper.clamp(fakePlayer.getHealth() - damage, 0.0f, 20.0f));

        if (fakePlayer.getHealth() <= 0.0f) {
            popTotem();
        }
    }

    private float getRealisticDamage() {
        ItemStack stack = mc.player.getMainHandStack();

        double damage = mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);

        if (stack.isOf(Items.WOODEN_SWORD)) damage = 4.0;
        else if (stack.isOf(Items.STONE_SWORD)) damage = 5.0;
        else if (stack.isOf(Items.IRON_SWORD)) damage = 6.0;
        else if (stack.isOf(Items.DIAMOND_SWORD)) damage = 7.0;
        else if (stack.isOf(Items.NETHERITE_SWORD)) damage = 8.0;

        else if (stack.isOf(Items.WOODEN_AXE)) damage = 7.0;
        else if (stack.isOf(Items.STONE_AXE)) damage = 9.0;
        else if (stack.isOf(Items.IRON_AXE)) damage = 9.0;
        else if (stack.isOf(Items.DIAMOND_AXE)) damage = 9.0;
        else if (stack.isOf(Items.NETHERITE_AXE)) damage = 10.0;

        if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            int amp = mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1;
            damage += 3.0 * amp;
        }

        if (mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
            int amp = mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1;
            damage -= 4.0 * amp;
        }

        return (float) Math.max(0.0, damage);
    }

    private void applyDamage(float damage) {
        float absorption = fakePlayer.getAbsorptionAmount();

        if (absorption > 0.0f) {
            float newAbsorption = MathHelper.clamp(absorption - damage, 0.0f, 20.0f);
            float leftDamage = MathHelper.clamp(damage - absorption, 0.0f, 100.0f);

            fakePlayer.setAbsorptionAmount(newAbsorption);
            fakePlayer.setHealth(MathHelper.clamp(fakePlayer.getHealth() - leftDamage, 0.0f, 20.0f));
        } else {
            fakePlayer.setHealth(MathHelper.clamp(fakePlayer.getHealth() - damage, 0.0f, 20.0f));
        }
    }

    private void popTotem() {
        fakePlayer.clearStatusEffects();

        mc.particleManager.addEmitter(fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
        play(SoundEvents.ITEM_TOTEM_USE, 1.0f, 1.0f);

        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1, false, true));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 880, 1, false, true));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0, false, true));

        fakePlayer.setAbsorptionAmount(8.0f);
        fakePlayer.setHealth(1.0f);
    }

    private void facePlayer() {
        Vec3d diff = mc.player.getPos().subtract(fakePlayer.getPos());

        float yaw = (float) MathHelper.wrapDegrees(
                Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0
        );

        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float pitch = (float) -Math.toDegrees(
                Math.atan2(diff.y + mc.player.getEyeHeight(mc.player.getPose()), horizontal)
        );

        fakePlayer.setYaw(yaw);
        fakePlayer.setHeadYaw(yaw);
        fakePlayer.setBodyYaw(yaw);
        fakePlayer.setPitch(pitch);
    }

    private void play(SoundEvent sound, float volume, float pitch) {
        if (mc.world == null || mc.player == null || fakePlayer == null) {
            return;
        }

        mc.world.playSound(
                mc.player,
                fakePlayer.getX(),
                fakePlayer.getY(),
                fakePlayer.getZ(),
                sound,
                fakePlayer.getSoundCategory(),
                volume,
                pitch
        );
    }

    private void removeFakePlayer() {
        if (mc.world != null && fakePlayer != null) {
            fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }

        fakePlayer = null;
    }
}