package fun.enoughvisuals.systems.modules.modules.utils;

import com.mojang.authlib.GameProfile;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.visual.HitParticles;
import fun.enoughvisuals.systems.modules.modules.visual.TargetESP;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.Locale;
import java.util.UUID;

@Register(
        name = "FakePlayer",
        id = "fake_player",
        description = "Фейк игрок",
        category = Category.UTILS
)
public class FakePlayer extends Module {
    private static final UUID FAKE_UUID = UUID.fromString("70ee432d-0a96-4137-a2c0-37cc9df67f03");
    private static FakePlayer INSTANCE;

    public static OtherClientPlayerEntity fakePlayer;
    private static boolean suppressLocalAttackSounds;
    private static long suppressAttackSoundsUntilMs;
    private static boolean suppressSwingHitModules;
    private boolean fakeDead;

    private final BooleanSetting immortal = add(new BooleanSetting("Бессмертный", "immortal", "Восстанавливает здоровье и использует тотем", true));

    private final BooleanSetting helmet = add(new BooleanSetting("Шлем", "helmet", "Надеть шлем", true));
    private final BooleanSetting chestplate = add(new BooleanSetting("Нагрудник", "chestplate", "Надеть нагрудник", true));
    private final BooleanSetting leggings = add(new BooleanSetting("Поножи", "leggings", "Надеть поножи", true));
    private final BooleanSetting boots = add(new BooleanSetting("Ботинки", "boots", "Надеть ботинки", true));
    private final ModeSetting armorType = add(new ModeSetting(
            "Тип брони",
            "armor_type",
            "Материал брони",
            "Netherite",
            "Leather",
            "Chainmail",
            "Iron",
            "Golden",
            "Diamond",
            "Netherite"
    ));

    public FakePlayer() {
        INSTANCE = this;
    }

    public static boolean handleLocalAttack(Entity target) {
        FakePlayer module = INSTANCE;
        if (module == null || !module.isEnabled()) return false;
        return module.handleLocalAttackInternal(target);
    }

    public static boolean shouldSuppressLocalAttackSound() {
        return suppressLocalAttackSounds || System.currentTimeMillis() < suppressAttackSoundsUntilMs;
    }

    public static boolean shouldSuppressSwingHitModules() {
        return suppressSwingHitModules;
    }

    @Override
    protected void onEnable() {
        fakeDead = false;
        ensureFakePlayer();
    }

    @Override
    protected void onDisable() {
        fakeDead = false;
        removeFakePlayer(true);
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            removeFakePlayer(false);
            fakeDead = false;
            return;
        }

        if (fakeDead) return;

        ensureFakePlayer();
        if (fakePlayer == null) return;

        syncHands();
        equipArmor();
        if (immortal.get()) {
            tickRegeneration();
        } else {
            tickMortalState();
        }
        tickMovement();
    }

    private boolean handleLocalAttackInternal(Entity target) {
        if (!(target instanceof OtherClientPlayerEntity attacked) || attacked != fakePlayer) return false;
        if (!isInGame()) return false;
        if (fakePlayer.hurtTime > 0) return true;

        float cooldown = mc.player.getAttackCooldownProgress(0.5f);
        float damage = getWeaponDamage(mc.player.getMainHandStack());
        damage *= 0.2f + cooldown * cooldown * 0.8f;

        boolean crit = isCriticalHit(mc.player, cooldown);
        if (crit) {
            damage *= 1.5f;
        }

        DamageSource source = mc.world.getDamageSources().playerAttack(mc.player);
        float afterArmor = DamageUtil.getDamageLeft(
                fakePlayer,
                damage,
                source,
                computeArmorValue(),
                computeArmorToughness()
        );

        fakePlayer.onDamaged(source);
        fakePlayer.hurtTime = 10;
        fakePlayer.maxHurtTime = 10;
        fakePlayer.animateDamage(fakePlayer.getYaw());
        suppressSwingHitModules = true;
        try {
            mc.player.swingHand(Hand.MAIN_HAND);
        } finally {
            suppressSwingHitModules = false;
        }

        suppressLocalAttackSounds = HitSound.INSTANCE != null && HitSound.INSTANCE.isEnabled();
        if (suppressLocalAttackSounds) {
            suppressAttackSoundsUntilMs = System.currentTimeMillis() + 250L;
        }
        try {
            playHurtAndAttackFeedback(crit, cooldown);
        } finally {
            suppressLocalAttackSounds = false;
        }
        TargetESP.onAttack(target);
        notifyHitModules(target);

        float nextHealth = fakePlayer.getHealth() - afterArmor;
        if (nextHealth <= 0.0f) {
            if (immortal.get()) {
                popTotem();
            } else {
                killFakePlayer(source);
            }
        } else {
            fakePlayer.setHealth(nextHealth);
        }
        return true;
    }

    private void ensureFakePlayer() {
        if (fakeDead) return;
        if (!isInGame()) return;
        if (fakePlayer != null && fakePlayer.getWorld() == mc.world && !fakePlayer.isRemoved()) return;

        removeFakePlayer(false);

        fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(FAKE_UUID, "FakePlayer"));
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setYaw(mc.player.getYaw());
        fakePlayer.setHeadYaw(mc.player.getHeadYaw());
        fakePlayer.setBodyYaw(mc.player.getBodyYaw());
        fakePlayer.setHealth(20.0f);
        fakePlayer.setAbsorptionAmount(0.0f);
        syncHands();
        equipArmor();
        if (immortal.get()) {
            fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 9999, 5, false, false));
        }
        mc.world.addEntity(fakePlayer);
        ClientActionUtil.clientMessage("FakePlayer включён");
    }

    private void syncHands() {
        if (fakePlayer == null || mc.player == null) return;
        fakePlayer.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
        fakePlayer.setStackInHand(Hand.OFF_HAND, mc.player.getOffHandStack().copy());
        fakePlayer.setMainArm(mc.player.getMainArm());
    }

    private void tickMovement() {
        fakePlayer.setSprinting(false);
        fakePlayer.setVelocity(0.0, fakePlayer.getVelocity().y, 0.0);
    }

    private void tickRegeneration() {
        if (!fakePlayer.hasStatusEffect(StatusEffects.REGENERATION)) {
            fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 9999, 5, false, false));
        }
        if (fakePlayer.age % 20 == 0 && fakePlayer.getHealth() < fakePlayer.getMaxHealth()) {
            fakePlayer.setHealth(Math.min(fakePlayer.getMaxHealth(), fakePlayer.getHealth() + 15.0f));
        }
    }

    private void tickMortalState() {
        if (fakePlayer.hasStatusEffect(StatusEffects.REGENERATION)) {
            fakePlayer.removeStatusEffect(StatusEffects.REGENERATION);
        }
        if (fakePlayer.hasStatusEffect(StatusEffects.ABSORPTION)) {
            fakePlayer.removeStatusEffect(StatusEffects.ABSORPTION);
        }
        if (fakePlayer.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            fakePlayer.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
        }
        if (fakePlayer.getAbsorptionAmount() > 0.0f) {
            fakePlayer.setAbsorptionAmount(0.0f);
        }
    }

    private void equipArmor() {
        if (fakePlayer == null) return;
        String type = armorType.get().toLowerCase(Locale.ROOT);
        fakePlayer.equipStack(EquipmentSlot.HEAD, helmet.get() ? new ItemStack(getArmorItem(type, "helmet")) : ItemStack.EMPTY);
        fakePlayer.equipStack(EquipmentSlot.CHEST, chestplate.get() ? new ItemStack(getArmorItem(type, "chestplate")) : ItemStack.EMPTY);
        fakePlayer.equipStack(EquipmentSlot.LEGS, leggings.get() ? new ItemStack(getArmorItem(type, "leggings")) : ItemStack.EMPTY);
        fakePlayer.equipStack(EquipmentSlot.FEET, boots.get() ? new ItemStack(getArmorItem(type, "boots")) : ItemStack.EMPTY);
    }

    private void playHurtAndAttackFeedback(boolean crit, float cooldown) {
        mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        if (crit) {
            if (!isReplacingAttackSound()) {
                mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            mc.player.addCritParticles(fakePlayer);
            mc.particleManager.addEmitter(fakePlayer, ParticleTypes.CRIT, 3);
            new EntityStatusS2CPacket(fakePlayer, (byte) 36).apply(mc.player.networkHandler);
        } else if (cooldown > 0.9f) {
            if (!isReplacingAttackSound()) {
                mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        } else {
            if (!isReplacingAttackSound()) {
                mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }
    }

    private static boolean isReplacingAttackSound() {
        return HitSound.INSTANCE != null && HitSound.INSTANCE.isEnabled();
    }

    private void notifyHitModules(Entity target) {
        HitSound.playHit();
        HitParticles.onHit(target);
    }

    private void popTotem() {
        new EntityStatusS2CPacket(fakePlayer, (byte) 35).apply(mc.player.networkHandler);
        fakePlayer.clearStatusEffects();
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1, false, true));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 880, 1, false, true));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0, false, true));
        fakePlayer.setAbsorptionAmount(8.0f);
        fakePlayer.setHealth(1.0f);
    }

    private void killFakePlayer(DamageSource source) {
        if (fakePlayer == null || mc.world == null || mc.player == null) return;

        fakePlayer.setHealth(0.0f);
        fakePlayer.setAbsorptionAmount(0.0f);
        fakePlayer.clearStatusEffects();
        fakePlayer.onDeath(source);
        fakePlayer.handleStatus((byte) 3);
        mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        removeFakePlayer(false, Entity.RemovalReason.KILLED);
        fakeDead = true;
        ClientActionUtil.clientMessage("FakePlayer умер");
    }

    private static boolean isCriticalHit(PlayerEntity player, float cooldown) {
        return cooldown > 0.9f
                && (player.fallDistance > 0.0f || player.getVelocity().y < -0.08)
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !player.hasVehicle();
    }

    private static float getWeaponDamage(ItemStack stack) {
        float damage = (float) mc.player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        if (damage > 1.0f) return damage;
        if (stack.isOf(Items.WOODEN_SWORD)) return 4.0f;
        if (stack.isOf(Items.STONE_SWORD)) return 5.0f;
        if (stack.isOf(Items.IRON_SWORD)) return 6.0f;
        if (stack.isOf(Items.DIAMOND_SWORD)) return 7.0f;
        if (stack.isOf(Items.NETHERITE_SWORD)) return 8.0f;
        if (stack.isOf(Items.WOODEN_AXE)) return 7.0f;
        if (stack.isOf(Items.STONE_AXE)) return 9.0f;
        if (stack.isOf(Items.IRON_AXE) || stack.isOf(Items.DIAMOND_AXE)) return 9.0f;
        if (stack.isOf(Items.NETHERITE_AXE)) return 10.0f;
        return 1.0f;
    }

    private float computeArmorValue() {
        return armorValue(fakePlayer.getEquippedStack(EquipmentSlot.HEAD))
                + armorValue(fakePlayer.getEquippedStack(EquipmentSlot.CHEST))
                + armorValue(fakePlayer.getEquippedStack(EquipmentSlot.LEGS))
                + armorValue(fakePlayer.getEquippedStack(EquipmentSlot.FEET));
    }

    private float computeArmorToughness() {
        String type = armorType.get().toLowerCase(Locale.ROOT);
        if (!type.equals("diamond") && !type.equals("netherite")) return 0.0f;
        float pieces = 0.0f;
        if (helmet.get()) pieces++;
        if (chestplate.get()) pieces++;
        if (leggings.get()) pieces++;
        if (boots.get()) pieces++;
        return pieces * (type.equals("netherite") ? 3.0f : 2.0f);
    }

    private static float armorValue(ItemStack stack) {
        if (stack.isOf(Items.LEATHER_HELMET) || stack.isOf(Items.GOLDEN_HELMET) || stack.isOf(Items.CHAINMAIL_BOOTS) || stack.isOf(Items.LEATHER_BOOTS) || stack.isOf(Items.GOLDEN_BOOTS)) return 1.0f;
        if (stack.isOf(Items.IRON_HELMET) || stack.isOf(Items.DIAMOND_HELMET) || stack.isOf(Items.NETHERITE_HELMET) || stack.isOf(Items.CHAINMAIL_HELMET) || stack.isOf(Items.IRON_BOOTS) || stack.isOf(Items.DIAMOND_BOOTS) || stack.isOf(Items.NETHERITE_BOOTS)) return 2.0f;
        if (stack.isOf(Items.LEATHER_LEGGINGS)) return 2.0f;
        if (stack.isOf(Items.GOLDEN_LEGGINGS) || stack.isOf(Items.CHAINMAIL_LEGGINGS)) return 3.0f;
        if (stack.isOf(Items.IRON_LEGGINGS)) return 5.0f;
        if (stack.isOf(Items.DIAMOND_LEGGINGS) || stack.isOf(Items.NETHERITE_LEGGINGS)) return 6.0f;
        if (stack.isOf(Items.LEATHER_CHESTPLATE)) return 3.0f;
        if (stack.isOf(Items.GOLDEN_CHESTPLATE) || stack.isOf(Items.CHAINMAIL_CHESTPLATE)) return 5.0f;
        if (stack.isOf(Items.IRON_CHESTPLATE)) return 6.0f;
        if (stack.isOf(Items.DIAMOND_CHESTPLATE) || stack.isOf(Items.NETHERITE_CHESTPLATE)) return 8.0f;
        return 0.0f;
    }

    private static Item getArmorItem(String type, String piece) {
        return switch (type) {
            case "leather" -> switch (piece) {
                case "helmet" -> Items.LEATHER_HELMET;
                case "chestplate" -> Items.LEATHER_CHESTPLATE;
                case "leggings" -> Items.LEATHER_LEGGINGS;
                default -> Items.LEATHER_BOOTS;
            };
            case "chainmail" -> switch (piece) {
                case "helmet" -> Items.CHAINMAIL_HELMET;
                case "chestplate" -> Items.CHAINMAIL_CHESTPLATE;
                case "leggings" -> Items.CHAINMAIL_LEGGINGS;
                default -> Items.CHAINMAIL_BOOTS;
            };
            case "iron" -> switch (piece) {
                case "helmet" -> Items.IRON_HELMET;
                case "chestplate" -> Items.IRON_CHESTPLATE;
                case "leggings" -> Items.IRON_LEGGINGS;
                default -> Items.IRON_BOOTS;
            };
            case "golden" -> switch (piece) {
                case "helmet" -> Items.GOLDEN_HELMET;
                case "chestplate" -> Items.GOLDEN_CHESTPLATE;
                case "leggings" -> Items.GOLDEN_LEGGINGS;
                default -> Items.GOLDEN_BOOTS;
            };
            case "diamond" -> switch (piece) {
                case "helmet" -> Items.DIAMOND_HELMET;
                case "chestplate" -> Items.DIAMOND_CHESTPLATE;
                case "leggings" -> Items.DIAMOND_LEGGINGS;
                default -> Items.DIAMOND_BOOTS;
            };
            default -> switch (piece) {
                case "helmet" -> Items.NETHERITE_HELMET;
                case "chestplate" -> Items.NETHERITE_CHESTPLATE;
                case "leggings" -> Items.NETHERITE_LEGGINGS;
                default -> Items.NETHERITE_BOOTS;
            };
        };
    }

    private void removeFakePlayer(boolean notify) {
        removeFakePlayer(notify, Entity.RemovalReason.DISCARDED);
    }

    private void removeFakePlayer(boolean notify, Entity.RemovalReason reason) {
        if (fakePlayer != null) {
            fakePlayer.remove(reason);
            if (mc.world != null) {
                mc.world.removeEntity(fakePlayer.getId(), reason);
            }
            fakePlayer = null;
            if (notify) ClientActionUtil.clientMessage("FakePlayer выключен");
        }
    }
}
