package fun.enoughvisuals.systems.modules.modules.utils;

import com.mojang.authlib.GameProfile;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.visual.HitParticles;
import fun.enoughvisuals.systems.modules.modules.visual.TargetESP;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    private final BooleanSetting immortal = add(new BooleanSetting("Снятие тотемов", "remove_totems", "Получает урон и использует тотем при смерти", true));
    private final ModeSetting armorType = add(new ModeSetting(
            "Броня",
            "armor_type",
            "Комплект брони фейк-игрока",
            "Копировать",
            "Копировать", "Без брони", "Кожаная", "Кольчужная", "Золотая", "Железная", "Алмазная", "Незеритовая"
    ));
    private final ModeSetting behavior = add(new ModeSetting("Поведение", "behavior", "", "Манекен", "Манекен", "Подвижный"));
    private final SliderSetting activity = add(new SliderSetting("Активность", "activity", "", 1.0, 0.3, 1.5, 0.05));
    private final BooleanSetting jumps = add(new BooleanSetting("Прыжки", "jumps", "", true));
    private final BooleanSetting swings = add(new BooleanSetting("Замахи", "swings", "", true));
    private double moveX, moveY, moveZ;
    private double orbitDistance = 3.0;
    private int orbitDirection = 1, directionTicks, distanceTicks, jumpCooldown, swingCooldown, idleTicks;

    public FakePlayer() {
        INSTANCE = this;
        activity.visibleWhen(() -> behavior.is("Подвижный"));
        jumps.visibleWhen(() -> behavior.is("Подвижный"));
        swings.visibleWhen(() -> behavior.is("Подвижный"));
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
        boolean died = fakeDead;
        fakeDead = false;
        removeFakePlayer(!died);
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
                fakePlayer.setHealth(fakePlayer.getMaxHealth());
                fakePlayer.setAbsorptionAmount(0.0f);
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

        GameProfile profile = new GameProfile(FAKE_UUID, "FakePlayer");
        profile.getProperties().putAll(mc.player.getGameProfile().getProperties());
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
        Vec3d look = mc.player.getRotationVec(1.0f).multiply(1.0, 0.0, 1.0);
        look = look.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 0.0, 1.0) : look.normalize();
        float yaw = mc.player.getYaw() + 180.0f;
        fakePlayer.refreshPositionAndAngles(mc.player.getX() + look.x * 2.5, mc.player.getY(), mc.player.getZ() + look.z * 2.5, yaw, 0.0f);
        fakePlayer.setHeadYaw(yaw);
        fakePlayer.setBodyYaw(yaw);
        fakePlayer.setHealth(20.0f);
        fakePlayer.setAbsorptionAmount(0.0f);
        syncHands();
        equipArmor();
        mc.world.addEntity(fakePlayer);
        moveX = moveY = moveZ = 0.0;
        directionTicks = distanceTicks = jumpCooldown = swingCooldown = idleTicks = 0;
        ClientActionUtil.clientMessage("FakePlayer включён");
    }

    private void syncHands() {
        if (fakePlayer == null || mc.player == null) return;
        fakePlayer.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
        fakePlayer.setStackInHand(Hand.OFF_HAND, immortal.get() ? new ItemStack(Items.TOTEM_OF_UNDYING) : ItemStack.EMPTY);
        fakePlayer.setMainArm(mc.player.getMainArm());
    }

    private void tickMovement() {
        if (!behavior.is("Подвижный")) {
            moveX = moveZ = 0.0;
            fakePlayer.setSprinting(false);
            if (fakePlayer.isOnGround()) {
                moveY = 0.0;
                fakePlayer.setVelocity(Vec3d.ZERO);
            } else {
                moveY = (moveY - 0.08) * 0.98;
                fakePlayer.move(MovementType.SELF, new Vec3d(0.0, moveY, 0.0));
                fakePlayer.setVelocity(0.0, moveY, 0.0);
            }
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double dx = mc.player.getX() - fakePlayer.getX();
        double dz = mc.player.getZ() - fakePlayer.getZ();
        double distance = Math.hypot(dx, dz);
        if (fakePlayer.getY() < mc.player.getY() - 24.0 || distance > 16.0) {
            double angle = random.nextDouble(Math.PI * 2.0);
            fakePlayer.refreshPositionAndAngles(mc.player.getX() + Math.cos(angle) * 3.0, mc.player.getY(), mc.player.getZ() + Math.sin(angle) * 3.0, fakePlayer.getYaw(), 0.0f);
            moveX = moveY = moveZ = 0.0;
            return;
        }

        float active = activity.get().floatValue();
        if (--directionTicks <= 0) {
            orbitDirection = random.nextBoolean() ? 1 : -1;
            directionTicks = random.nextInt(18, 60);
        }
        if (--distanceTicks <= 0) {
            orbitDistance = random.nextDouble(1.6, 4.4);
            distanceTicks = random.nextInt(40, 110);
        }
        if (idleTicks > 0) idleTicks--; else if (random.nextFloat() < 0.005f) idleTicks = random.nextInt(6, 18);

        double inv = distance < 1.0E-4 ? 0.0 : 1.0 / distance;
        double nx = dx * inv, nz = dz * inv;
        double radial = MathHelper.clamp((distance - orbitDistance) * 0.45, -1.0, 1.0);
        double wantedX = nx * radial - nz * orbitDirection * 0.9;
        double wantedZ = nz * radial + nx * orbitDirection * 0.9;
        double wantedLength = Math.hypot(wantedX, wantedZ);
        if (wantedLength > 1.0) { wantedX /= wantedLength; wantedZ /= wantedLength; }
        double speed = idleTicks > 0 ? 0.0 : 0.26 * active;
        double easing = fakePlayer.isOnGround() ? 0.3 : 0.1;
        moveX += (wantedX * speed - moveX) * easing;
        moveZ += (wantedZ * speed - moveZ) * easing;
        if (jumpCooldown > 0) jumpCooldown--;
        if (fakePlayer.isOnGround()) {
            moveY = -0.0784;
            if (jumps.get() && jumpCooldown == 0 && (fakePlayer.horizontalCollision || random.nextFloat() < 0.035f * active)) {
                moveY = 0.42;
                jumpCooldown = random.nextInt(25, 70);
            }
        } else moveY = (moveY - 0.08) * 0.98;

        fakePlayer.setSprinting(Math.hypot(moveX, moveZ) > 0.18);
        fakePlayer.move(MovementType.SELF, new Vec3d(moveX, moveY, moveZ));
        fakePlayer.setVelocity(moveX, moveY, moveZ);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        double eyeDy = mc.player.getEyeY() - fakePlayer.getEyeY();
        float targetPitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(eyeDy, Math.max(distance, 1.0E-4))), -60.0, 60.0);
        fakePlayer.headYaw += MathHelper.clamp(MathHelper.wrapDegrees(targetYaw - fakePlayer.headYaw), -30.0f, 30.0f);
        fakePlayer.setYaw(fakePlayer.headYaw);
        fakePlayer.setPitch(fakePlayer.getPitch() + MathHelper.clamp(targetPitch - fakePlayer.getPitch(), -15.0f, 15.0f));
        if (swingCooldown > 0) swingCooldown--;
        if (swings.get() && swingCooldown == 0 && distance < 3.2 && random.nextFloat() < 0.3f) {
            fakePlayer.swingHand(Hand.MAIN_HAND);
            swingCooldown = random.nextInt(11, 22);
        }
    }

    private void tickRegeneration() {
        if (fakePlayer.getHealth() < fakePlayer.getMaxHealth()) {
            fakePlayer.setHealth(Math.min(fakePlayer.getMaxHealth(), fakePlayer.getHealth() + 0.1f));
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
        fakePlayer.setHealth(fakePlayer.getMaxHealth());
    }

    private void equipArmor() {
        if (fakePlayer == null) return;
        if (armorType.is("Копировать")) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                fakePlayer.equipStack(slot, mc.player.getEquippedStack(slot).copy());
            return;
        }
        String type = armorType.get();
        fakePlayer.equipStack(EquipmentSlot.HEAD, armorStack(type, "helmet"));
        fakePlayer.equipStack(EquipmentSlot.CHEST, armorStack(type, "chestplate"));
        fakePlayer.equipStack(EquipmentSlot.LEGS, armorStack(type, "leggings"));
        fakePlayer.equipStack(EquipmentSlot.FEET, armorStack(type, "boots"));
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
        disableSilent();
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
        float toughness = 0.0f;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = fakePlayer.getEquippedStack(slot);
            if (stack.isOf(Items.NETHERITE_HELMET) || stack.isOf(Items.NETHERITE_CHESTPLATE)
                    || stack.isOf(Items.NETHERITE_LEGGINGS) || stack.isOf(Items.NETHERITE_BOOTS)) toughness += 3.0f;
            else if (stack.isOf(Items.DIAMOND_HELMET) || stack.isOf(Items.DIAMOND_CHESTPLATE)
                    || stack.isOf(Items.DIAMOND_LEGGINGS) || stack.isOf(Items.DIAMOND_BOOTS)) toughness += 2.0f;
        }
        return toughness;
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

    private static ItemStack armorStack(String type, String piece) {
        if (type.equals("Без брони")) return ItemStack.EMPTY;
        Item item = getArmorItem(type, piece);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Item getArmorItem(String type, String piece) {
        return switch (type) {
            case "Кожаная" -> switch (piece) {
                case "helmet" -> Items.LEATHER_HELMET;
                case "chestplate" -> Items.LEATHER_CHESTPLATE;
                case "leggings" -> Items.LEATHER_LEGGINGS;
                default -> Items.LEATHER_BOOTS;
            };
            case "Кольчужная" -> switch (piece) {
                case "helmet" -> Items.CHAINMAIL_HELMET;
                case "chestplate" -> Items.CHAINMAIL_CHESTPLATE;
                case "leggings" -> Items.CHAINMAIL_LEGGINGS;
                default -> Items.CHAINMAIL_BOOTS;
            };
            case "Железная" -> switch (piece) {
                case "helmet" -> Items.IRON_HELMET;
                case "chestplate" -> Items.IRON_CHESTPLATE;
                case "leggings" -> Items.IRON_LEGGINGS;
                default -> Items.IRON_BOOTS;
            };
            case "Золотая" -> switch (piece) {
                case "helmet" -> Items.GOLDEN_HELMET;
                case "chestplate" -> Items.GOLDEN_CHESTPLATE;
                case "leggings" -> Items.GOLDEN_LEGGINGS;
                default -> Items.GOLDEN_BOOTS;
            };
            case "Алмазная" -> switch (piece) {
                case "helmet" -> Items.DIAMOND_HELMET;
                case "chestplate" -> Items.DIAMOND_CHESTPLATE;
                case "leggings" -> Items.DIAMOND_LEGGINGS;
                default -> Items.DIAMOND_BOOTS;
            };
            case "Незеритовая" -> switch (piece) {
                case "helmet" -> Items.NETHERITE_HELMET;
                case "chestplate" -> Items.NETHERITE_CHESTPLATE;
                case "leggings" -> Items.NETHERITE_LEGGINGS;
                default -> Items.NETHERITE_BOOTS;
            };
            default -> null;
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
