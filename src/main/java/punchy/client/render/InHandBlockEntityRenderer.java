package punchy.client.render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.class_1047;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1921;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2605;
import net.minecraft.class_2665;
import net.minecraft.class_2671;
import net.minecraft.class_2680;
import net.minecraft.class_2764;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_3481;
import net.minecraft.class_3532;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_746;
import net.minecraft.class_776;
import net.minecraft.class_7833;
import net.minecraft.class_824;
import net.minecraft.class_827;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import punchy.client.debug.PunchyDebug;
import punchy.client.render.EnchantParticleProxyTuning;
import punchy.compat.EmfEtfCemCompat;
import punchy.compat.entityculling.EntityCullingCompat;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/InHandBlockEntityRenderer.class */
public final class InHandBlockEntityRenderer {
    private static final float ENCHANT_TABLE_SCALE = 0.52f;
    private static final float ENCHANT_TABLE_Y_OFFSET = -0.15f;
    private static final float ENCHANT_TABLE_Z_OFFSET = 0.05f;
    private static final float ENCHANT_TABLE_TILT_DEG = 45.0f;
    private static final float ENCHANT_BOOK_FLOAT_MAX = 0.65f;
    private static final float ENCHANT_BOOK_RISE_LERP = 0.1f;
    private static final float ENCHANT_BOOK_FALL_LERP = 0.06f;
    private static final int ENCHANT_OPEN_DELAY_TICKS = 20;
    private static final int ENCHANT_REEQUIP_RESET_TICKS = 4;
    private static final float ENCHANT_BOOK_SCALE = 1.25f;
    private static final float BOOK_ATTRACT_X = 0.0f;
    private static final float BOOK_ATTRACT_Y = 0.75f;
    private static final float BOOK_ATTRACT_Z = 0.0f;
    private static final float PISTON_SCALE = 0.62f;
    private static final float PISTON_Y_OFFSET = -0.12f;
    private static final float PISTON_Z_OFFSET = 0.08f;
    private static final float PISTON_EXTEND_DISTANCE = 1.0f;
    private static final float PISTON_FALL_MAX = 0.65f;
    private static final float PISTON_FALL_SPEED_MAX = 0.6f;
    private static final float PISTON_FALL_DISTANCE_MAX = 6.0f;
    private static final float PISTON_FALL_RISE_LERP = 0.08f;
    private static final float PISTON_FALL_DROP_LERP = 0.35f;
    private static final class_2960 PARTICLE_ATLAS_ID = class_1059.field_17898;
    private static final EnumMap<class_1306, class_2605> ENCHANT_ENTITIES = new EnumMap<>(class_1306.class);
    private static class_2680 enchantingState = class_2246.field_10485.method_9564();
    private static final EnumMap<class_1306, HeldBookState> ENCHANT_STATES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, HeldPistonState> PISTON_STATES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, class_243> HELD_PIVOT_WORLD = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> HELD_PIVOT_TICK = new EnumMap<>(class_1306.class);
    private static final List<HandEnchantParticle> HAND_PARTICLES = new ArrayList();
    private static long lastParticleUpdateTick = -1;

    private InHandBlockEntityRenderer() {
    }

    public static boolean renderIfSupported(class_1799 stack, class_746 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int combinedLight, float partialTicks) {
        if (stack == null || stack.method_7960() || player == null) {
            return false;
        }
        if (stack.method_31574(class_1802.field_8657)) {
            if (PunchyConfig.disableEnchantingTableModels()) {
                return false;
            }
            return renderEnchantingTable(player, arm, poseStack, buffer, combinedLight, partialTicks);
        }
        if ((!stack.method_31574(class_1802.field_8249) && !stack.method_31574(class_1802.field_8105)) || PunchyConfig.disablePistonModels()) {
            return false;
        }
        return renderPiston(player, arm, stack, poseStack, buffer, combinedLight);
    }

    private static boolean renderEnchantingTable(class_746 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int combinedLight, float partialTicks) {
        class_310 mc = class_310.method_1551();
        class_4184 camera = mc.field_1773.method_19418();
        class_1937 level = player.method_37908();
        if (level == null) {
            return false;
        }
        class_2605 enchantingTableEntity = ensureEnchantingTable(level, player.method_24515(), arm != null ? arm : class_1306.field_6183);
        if (enchantingTableEntity == null) {
            return false;
        }
        class_824 dispatcher = mc.method_31975();
        dispatcher.method_3549(level, camera, mc.field_1765);
        class_827<class_2605> renderer = dispatcher.method_3550(enchantingTableEntity);
        if (renderer == null) {
            return false;
        }
        poseStack.method_22903();
        poseStack.method_46416(0.0f, ENCHANT_TABLE_Y_OFFSET, ENCHANT_TABLE_Z_OFFSET);
        poseStack.method_22907(class_7833.field_40714.rotationDegrees(ENCHANT_TABLE_TILT_DEG));
        poseStack.method_22905(ENCHANT_TABLE_SCALE, ENCHANT_TABLE_SCALE, ENCHANT_TABLE_SCALE);
        poseStack.method_46416(-0.5f, -0.5f, -0.5f);
        class_776 blockRenderer = mc.method_1541();
        blockRenderer.method_3353(enchantingState, poseStack, buffer, combinedLight, class_4608.field_21444);
        poseStack.method_22903();
        HeldBookState state = (HeldBookState) ENCHANT_STATES.computeIfAbsent(arm != null ? arm : class_1306.field_6183, k -> {
            return new HeldBookState();
        });
        boolean disablePhysics = EmfEtfCemCompat.disableProxyPhysics(EmfEtfCemCompat.ProxyPhysicsTarget.ENCHANTING_TABLE);
        if (disablePhysics) {
            state.bookOffset = 0.0f;
            resetEnchantingBookState(enchantingTableEntity);
        }
        if (state.bookOffset != 0.0f) {
            poseStack.method_46416(0.0f, state.bookOffset, 0.0f);
        }
        poseStack.method_46416(0.5f, BOOK_ATTRACT_Y, 0.5f);
        poseStack.method_22905(ENCHANT_BOOK_SCALE, ENCHANT_BOOK_SCALE, ENCHANT_BOOK_SCALE);
        poseStack.method_46416(-0.5f, -0.75f, -0.5f);
        Vector3f baseTarget = new Vector3f(0.0f, BOOK_ATTRACT_Y, 0.0f);
        Vector3f localTarget = applyParticleProxy(new Vector3f(baseTarget), arm);
        if (PunchyDebug.DEBUG_POSITION_ENABLED && localTarget != null) {
            poseStack.method_22903();
            poseStack.method_46416(localTarget.x, localTarget.y, localTarget.z);
            renderDebugMarker(buffer, poseStack, combinedLight);
            poseStack.method_22909();
        }
        class_243 pivotPos = resolvePoseWorld(poseStack, camera, baseTarget);
        Vector3f targetLocal = localTarget != null ? localTarget : baseTarget;
        if (!disablePhysics) {
            updateEnchantingBook(player, state, enchantingTableEntity, level.method_8510(), pivotPos);
            renderHandParticles(buffer, camera, partialTicks, combinedLight, arm, poseStack, targetLocal);
        }
        EntityCullingCompat.withBlockEntityFrustumCullingDisabled(() -> {
            renderer.method_3569(enchantingTableEntity, partialTicks, poseStack, buffer, combinedLight, class_4608.field_21444, camera.method_19326());
            return null;
        });
        poseStack.method_22909();
        poseStack.method_22909();
        return true;
    }

    private static boolean renderPiston(class_746 player, class_1306 arm, class_1799 stack, class_4587 poseStack, class_4597 buffer, int combinedLight) {
        class_1937 level;
        class_310 mc = class_310.method_1551();
        if (mc == null || (level = player.method_37908()) == null) {
            return false;
        }
        HeldPistonState state = (HeldPistonState) PISTON_STATES.computeIfAbsent(arm != null ? arm : class_1306.field_6183, k -> {
            return new HeldPistonState();
        });
        boolean sticky = stack.method_31574(class_1802.field_8105);
        boolean powered = isRedstoneBlock(level.method_8320(player.method_24515().method_10074()));
        boolean disablePhysics = EmfEtfCemCompat.disableProxyPhysics(EmfEtfCemCompat.ProxyPhysicsTarget.PISTON);
        if (disablePhysics) {
            state.extension = 0.0f;
            state.fallOffset = 0.0f;
            state.extended = false;
        } else {
            state.update(player, level, powered, player.method_24828(), (float) player.field_6017, (float) player.method_18798().field_1351);
        }
        boolean showHead = state.extension > 0.01f || state.fallOffset > 0.01f;
        class_2680 baseState = (class_2680) ((class_2680) (sticky ? class_2246.field_10615 : class_2246.field_10560).method_9564().method_11657(class_2665.field_12191, Boolean.valueOf(showHead))).method_11657(class_2665.field_10927, class_2350.field_11036);
        class_2680 headState = (class_2680) ((class_2680) class_2246.field_10379.method_9564().method_11657(class_2671.field_10927, class_2350.field_11036)).method_11657(class_2671.field_12224, sticky ? class_2764.field_12634 : class_2764.field_12637);
        poseStack.method_22903();
        poseStack.method_46416(0.0f, PISTON_Y_OFFSET, 0.08f);
        poseStack.method_22905(PISTON_SCALE, PISTON_SCALE, PISTON_SCALE);
        poseStack.method_46416(-0.5f, -0.5f, -0.5f);
        class_776 blockRenderer = mc.method_1541();
        blockRenderer.method_3353(baseState, poseStack, buffer, combinedLight, class_4608.field_21444);
        if (showHead) {
            poseStack.method_22903();
            poseStack.method_46416(0.0f, (state.extension * 1.0f) + state.fallOffset, 0.0f);
            blockRenderer.method_3353(headState, poseStack, buffer, combinedLight, class_4608.field_21444);
            poseStack.method_22909();
        }
        poseStack.method_22909();
        return true;
    }

    private static boolean isRedstoneBlock(class_2680 state) {
        if (state == null) {
            return false;
        }
        if (state.method_27852(class_2246.field_10002) || state.method_27852(class_2246.field_10523) || state.method_27852(class_2246.field_10301) || state.method_27852(class_2246.field_10091) || state.method_27852(class_2246.field_10450) || state.method_27852(class_2246.field_10377) || state.method_27852(class_2246.field_10363) || state.method_27852(class_2246.field_10282) || state.method_27852(class_2246.field_10429) || state.method_27852(class_2246.field_22422) || state.method_27852(class_2246.field_10348) || state.method_27852(class_2246.field_10179) || state.method_27852(class_2246.field_10200) || state.method_27852(class_2246.field_10228) || state.method_27852(class_2246.field_10312) || state.method_27852(class_2246.field_27171) || state.method_27852(class_2246.field_10425) || state.method_27852(class_2246.field_10025) || state.method_27852(class_2246.field_10546) || state.method_27852(class_2246.field_28108) || state.method_27852(class_2246.field_43231) || state.method_26164(class_3481.field_15493) || state.method_26164(class_3481.field_24076)) {
            return true;
        }
        return false;
    }

    private static void updateEnchantingBook(class_746 player, HeldBookState state, class_2605 enchantingTableEntity, long tick, class_243 pivotPos) {
        state.onSeen(tick);
        state.updateFallPhysics(player.method_24828(), (float) player.field_6017, (float) player.method_18798().field_1351);
        enchantingTableEntity.field_11965 = enchantingTableEntity.field_11966;
        enchantingTableEntity.field_11963 = enchantingTableEntity.field_11964;
        enchantingTableEntity.field_11960 = enchantingTableEntity.field_11958;
        float targetOpen = state.shouldOpen(tick) ? 1.0f : 0.0f;
        enchantingTableEntity.field_11966 = class_3532.method_16439(0.18f, enchantingTableEntity.field_11966, targetOpen);
        enchantingTableEntity.field_11962 = 0.0f;
        enchantingTableEntity.field_11964 = 0.0f;
        enchantingTableEntity.field_11963 = 0.0f;
        boolean particlesThisTick = false;
        if (state.shouldOpen(tick) && state.lastParticleTick != tick) {
            state.lastParticleTick = tick;
            class_2338 centerPos = player.method_24515();
            for (int dx = -2; dx <= 2; dx++) {
                int dz = -2;
                while (dz <= 2) {
                    if (dx > -2 && dx < 2 && dz == -1) {
                        dz = 2;
                    }
                    if (player.method_59922().method_43048(16) == 0) {
                        for (int dy = 0; dy <= 1; dy++) {
                            class_2338 shelfPos = centerPos.method_10069(dx, dy, dz);
                            if (player.method_37908().method_8320(shelfPos).method_27852(class_2246.field_10504) && player.method_37908().method_22347(centerPos.method_10069(dx / 2, dy, dz / 2))) {
                                double sX = ((double) shelfPos.method_10263()) + player.method_59922().method_43058();
                                double sY = ((double) shelfPos.method_10264()) + player.method_59922().method_43058();
                                double sZ = ((double) shelfPos.method_10260()) + player.method_59922().method_43058();
                                spawnHandParticle(pivotPos, new class_243(sX, sY, sZ));
                                particlesThisTick = true;
                            }
                        }
                    }
                    dz++;
                }
            }
        }
        if (state.lastFlipTick != tick) {
            state.lastFlipTick = tick;
            if (particlesThisTick && state.shouldOpen(tick)) {
                enchantingTableEntity.field_11969 += (player.method_59922().method_43057() - 0.5f) * PISTON_FALL_SPEED_MAX;
            } else {
                enchantingTableEntity.field_11969 *= PISTON_FALL_SPEED_MAX;
            }
            float delta = enchantingTableEntity.field_11969 - enchantingTableEntity.field_11958;
            enchantingTableEntity.field_11967 += delta * 0.4f;
            enchantingTableEntity.field_11967 *= 0.9f;
            enchantingTableEntity.field_11958 += enchantingTableEntity.field_11967;
        }
        enchantingTableEntity.field_11961++;
    }

    private static void resetEnchantingBookState(class_2605 enchantingTableEntity) {
        if (enchantingTableEntity == null) {
            return;
        }
        enchantingTableEntity.field_11966 = 0.0f;
        enchantingTableEntity.field_11965 = 0.0f;
        enchantingTableEntity.field_11964 = 0.0f;
        enchantingTableEntity.field_11963 = 0.0f;
        enchantingTableEntity.field_11958 = 0.0f;
        enchantingTableEntity.field_11960 = 0.0f;
        enchantingTableEntity.field_11967 = 0.0f;
        enchantingTableEntity.field_11969 = 0.0f;
        enchantingTableEntity.field_11962 = 0.0f;
        enchantingTableEntity.field_11961 = 0;
    }

    private static class_2605 ensureEnchantingTable(class_1937 level, class_2338 desiredPos, class_1306 arm) {
        if (desiredPos == null) {
            desiredPos = class_2338.field_10980;
        }
        if (arm == null) {
            arm = class_1306.field_6183;
        }
        class_2605 current = ENCHANT_ENTITIES.get(arm);
        if (current == null) {
            enchantingState = class_2246.field_10485.method_9564();
            current = new class_2605(desiredPos, enchantingState);
            ENCHANT_ENTITIES.put(arm, current);
        } else if (!desiredPos.equals(current.method_11016())) {
            enchantingState = class_2246.field_10485.method_9564();
            class_2605 next = new class_2605(desiredPos, enchantingState);
            next.field_11961 = current.field_11961;
            next.field_11958 = current.field_11958;
            next.field_11960 = current.field_11960;
            next.field_11969 = current.field_11969;
            next.field_11967 = current.field_11967;
            next.field_11966 = current.field_11966;
            next.field_11965 = current.field_11965;
            next.field_11964 = current.field_11964;
            next.field_11963 = current.field_11963;
            next.field_11962 = current.field_11962;
            current = next;
            ENCHANT_ENTITIES.put(arm, current);
        }
        current.method_31662(level);
        return current;
    }

    public static void updateHeldPivot(class_746 player, class_1306 arm, class_4587 poseStack) {
        if (player == null || poseStack == null || player.method_37908() == null) {
            return;
        }
        if (arm == null) {
            arm = class_1306.field_6183;
        }
        class_4184 camera = class_310.method_1551().field_1773.method_19418();
        class_243 worldPos = resolvePoseWorld(poseStack, camera);
        if (worldPos == null) {
            return;
        }
        HELD_PIVOT_WORLD.put(arm, worldPos);
        HELD_PIVOT_TICK.put(arm, Long.valueOf(player.method_37908().method_8510()));
    }

    private static class_243 getHeldPivotWorld(class_1306 arm, long tick) {
        if (arm == null) {
            arm = class_1306.field_6183;
        }
        class_243 stored = HELD_PIVOT_WORLD.get(arm);
        if (stored == null) {
            return null;
        }
        Long lastTick = HELD_PIVOT_TICK.get(arm);
        if (lastTick == null || lastTick.longValue() != tick) {
            return stored;
        }
        return stored;
    }

    private static class_243 resolvePoseWorld(class_4587 poseStack, class_4184 camera) {
        if (poseStack == null || camera == null) {
            return null;
        }
        Matrix4f pose = new Matrix4f(poseStack.method_23760().method_23761());
        Vector4f center = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        pose.transform(center);
        Vector3f viewOffset = new Vector3f(center.x(), center.y(), center.z());
        viewOffset.rotate(camera.method_23767());
        return camera.method_19326().method_1031(viewOffset.x(), viewOffset.y(), viewOffset.z());
    }

    private static class_243 resolvePoseWorld(class_4587 poseStack, class_4184 camera, Vector3f local) {
        if (poseStack == null || camera == null || local == null) {
            return null;
        }
        Matrix4f pose = new Matrix4f(poseStack.method_23760().method_23761());
        Vector4f point = new Vector4f(local.x, local.y, local.z, 1.0f);
        pose.transform(point);
        Vector3f viewOffset = new Vector3f(point.x(), point.y(), point.z());
        viewOffset.rotate(camera.method_23767());
        return camera.method_19326().method_1031(viewOffset.x(), viewOffset.y(), viewOffset.z());
    }

    private static Vector3f applyParticleProxy(Vector3f base, class_1306 arm) {
        if (base == null) {
            return null;
        }
        boolean left = arm == class_1306.field_6182;
        EnchantParticleProxyTuning.Profile profile = EnchantParticleProxyTuning.get(EnchantParticleProxyTuning.Kind.BOOK, left);
        if (profile == null) {
            return base;
        }
        Matrix4f transform = new Matrix4f().scale(profile.scale).rotateX((float) Math.toRadians(profile.rotX)).rotateY((float) Math.toRadians(profile.rotY)).rotateZ((float) Math.toRadians(profile.rotZ)).translate(profile.offX, profile.offY, profile.offZ);
        Vector4f point = new Vector4f(base.x, base.y, base.z, 1.0f);
        transform.transform(point);
        return new Vector3f(point.x, point.y, point.z);
    }

    private static void renderDebugMarker(class_4597 buffer, class_4587 poseStack, int combinedLight) {
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        poseStack.method_22903();
        poseStack.method_22905(0.15f, 0.15f, 0.15f);
        poseStack.method_46416(-0.5f, -0.5f, -0.5f);
        class_776 blockRenderer = mc.method_1541();
        blockRenderer.method_3353(class_2246.field_10058.method_9564(), poseStack, buffer, combinedLight, class_4608.field_21444);
        poseStack.method_22909();
    }

    private static void spawnHandParticle(class_243 pivotPos, class_243 shelfPos) {
        if (pivotPos == null || shelfPos == null) {
            return;
        }
        int lifetime = 30 + ((int) (Math.random() * 10.0d));
        HAND_PARTICLES.add(new HandEnchantParticle(shelfPos, lifetime));
    }

    private static void tickHandParticles(long tick) {
        if (tick == lastParticleUpdateTick) {
            return;
        }
        lastParticleUpdateTick = tick;
        Iterator<HandEnchantParticle> it = HAND_PARTICLES.iterator();
        while (it.hasNext()) {
            HandEnchantParticle p = it.next();
            p.age++;
            if (p.age >= p.lifetime) {
                it.remove();
            }
        }
    }

    private static void renderHandParticles(class_4597 buffer, class_4184 camera, float partialTicks, int combinedLight, class_1306 arm, class_4587 poseStack, Vector3f targetLocal) {
        if (camera == null || buffer == null) {
            return;
        }
        long tick = class_310.method_1551().field_1687 != null ? class_310.method_1551().field_1687.method_8510() : -1L;
        if (tick >= 0) {
            tickHandParticles(tick);
        }
        if (HAND_PARTICLES.isEmpty() || poseStack == null || targetLocal == null) {
            return;
        }
        Matrix4f localToView = new Matrix4f(poseStack.method_23760().method_23761());
        Matrix4f viewToLocal = new Matrix4f(localToView).invert();
        Quaternionf invLocalRot = localToView.getNormalizedRotation(new Quaternionf()).invert();
        Quaternionf invCam = new Quaternionf(camera.method_23767()).invert();
        class_243 camPos = camera.method_19326();
        for (HandEnchantParticle p : HAND_PARTICLES) {
            class_1058 sprite = getGlyphSprite(p.spriteId);
            if (sprite != null) {
                float age = p.age + partialTicks;
                float t = age / p.lifetime;
                float inv = 1.0f - class_3532.method_15363(t, 0.0f, 1.0f);
                float curve = 1.0f - inv;
                float curve2 = curve * curve;
                float curve3 = curve2 * curve2;
                class_243 shelfDelta = p.shelf.method_1020(camPos);
                Vector3f shelfView = new Vector3f((float) shelfDelta.field_1352, (float) shelfDelta.field_1351, (float) shelfDelta.field_1350);
                shelfView.rotate(invCam);
                Vector4f shelfLocal4 = new Vector4f(shelfView.x, shelfView.y, shelfView.z, 1.0f);
                viewToLocal.transform(shelfLocal4);
                Vector3f shelfLocal = new Vector3f(shelfLocal4.x, shelfLocal4.y, shelfLocal4.z);
                Vector3f deltaLocal = new Vector3f(shelfLocal).sub(targetLocal);
                float x = targetLocal.x + (deltaLocal.x * inv);
                float y = (targetLocal.y + (deltaLocal.y * inv)) - (curve3 * 1.2f);
                float z = targetLocal.z + (deltaLocal.z * inv);
                poseStack.method_22903();
                poseStack.method_46416(x, y, z);
                poseStack.method_22907(invLocalRot);
                renderParticleQuad(buffer, poseStack, sprite, 0.04f, 15728880);
                poseStack.method_22909();
            }
        }
    }

    private static void renderParticleQuad(class_4597 buffer, class_4587 poseStack, class_1058 sprite, float size, int packedLight) {
        float half = size * 0.5f;
        float u0 = sprite.method_4594();
        float u1 = sprite.method_4577();
        float v0 = sprite.method_4593();
        float v1 = sprite.method_4575();
        class_4588 consumer = buffer.getBuffer(class_1921.method_23580(class_1059.field_17898));
        class_4587.class_4665 pose = poseStack.method_23760();
        consumer.method_22918(pose.method_23761(), -half, -half, 0.0f).method_1336(255, 255, 255, 255).method_22913(u0, v1).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, 0.0f, 0.0f, 1.0f);
        consumer.method_22918(pose.method_23761(), half, -half, 0.0f).method_1336(255, 255, 255, 255).method_22913(u1, v1).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, 0.0f, 0.0f, 1.0f);
        consumer.method_22918(pose.method_23761(), half, half, 0.0f).method_1336(255, 255, 255, 255).method_22913(u1, v0).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, 0.0f, 0.0f, 1.0f);
        consumer.method_22918(pose.method_23761(), -half, half, 0.0f).method_1336(255, 255, 255, 255).method_22913(u0, v0).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, 0.0f, 0.0f, 1.0f);
    }

    private static class_1058 getGlyphSprite(class_2960 id) {
        class_310 mc;
        Function<class_2960, class_1058> atlas;
        if (id == null || (mc = class_310.method_1551()) == null || (atlas = mc.method_1549(PARTICLE_ATLAS_ID)) == null) {
            return null;
        }
        class_1058 missing = atlas.apply(class_1047.method_4539());
        class_1058 sprite = atlas.apply(id);
        if (sprite != null && sprite != missing) {
            return sprite;
        }
        class_2960 fallback = class_2960.method_60655("minecraft", "enchanted_hit");
        class_1058 fallbackSprite = atlas.apply(fallback);
        if (fallbackSprite == null || fallbackSprite == missing) {
            return null;
        }
        return fallbackSprite;
    }

    private static class_2960 randomGlyphId() {
        int idx = (int) (Math.random() * 26.0d);
        char letter = (char) (97 + idx);
        return class_2960.method_60655("minecraft", "sga_" + letter);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/InHandBlockEntityRenderer$HandEnchantParticle.class */
    private static final class HandEnchantParticle {
        private final class_243 shelf;
        private final int lifetime;
        private final class_2960 spriteId = InHandBlockEntityRenderer.randomGlyphId();
        private int age;

        private HandEnchantParticle(class_243 shelf, int lifetime) {
            this.shelf = shelf;
            this.lifetime = lifetime;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/InHandBlockEntityRenderer$HeldBookState.class */
    private static final class HeldBookState {
        private long lastEquipTick;
        private long lastSeenTick;
        private long lastParticleTick;
        private float bookOffset;
        private boolean wasOnGround = true;
        private long lastFlipTick = Long.MIN_VALUE;

        private HeldBookState() {
        }

        private void onSeen(long tick) {
            if (tick - this.lastSeenTick > 4) {
                this.lastEquipTick = tick;
            }
            this.lastSeenTick = tick;
        }

        private boolean shouldOpen(long tick) {
            return tick - this.lastEquipTick >= 20;
        }

        private void updateFallPhysics(boolean onGround, float fallDistance, float deltaY) {
            float targetOffset = 0.0f;
            if (!onGround) {
                float fallSpeed = class_3532.method_15363(-deltaY, 0.0f, 0.3f);
                float speedFactor = fallSpeed / 0.3f;
                float distanceFactor = class_3532.method_15363(fallDistance / 2.0f, 0.0f, 1.0f);
                targetOffset = 0.65f * class_3532.method_15363(speedFactor + (distanceFactor * InHandBlockEntityRenderer.BOOK_ATTRACT_Y), 0.0f, 1.0f);
            }
            float lerp = targetOffset > this.bookOffset ? InHandBlockEntityRenderer.ENCHANT_BOOK_RISE_LERP : InHandBlockEntityRenderer.ENCHANT_BOOK_FALL_LERP;
            this.bookOffset = class_3532.method_16439(lerp, this.bookOffset, targetOffset);
            this.wasOnGround = onGround;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/InHandBlockEntityRenderer$HeldPistonState.class */
    private static final class HeldPistonState {
        private boolean extended;
        private float extension;
        private float fallOffset;
        private long lastUpdateTime = System.nanoTime();

        private HeldPistonState() {
        }

        private void update(class_746 player, class_1937 level, boolean powered, boolean onGround, float fallDistance, float deltaY) {
            if (player == null || level == null) {
                return;
            }
            long now = System.nanoTime();
            float dt = (now - this.lastUpdateTime) / 1.0E9f;
            this.lastUpdateTime = now;
            if (dt <= 0.0f) {
                return;
            }
            if (dt > InHandBlockEntityRenderer.ENCHANT_BOOK_RISE_LERP) {
                dt = 0.1f;
            }
            float tickLerp = dt * 20.0f;
            float extendLerp = 1.0f - ((float) Math.pow(0.25d, tickLerp));
            float riseLerp = 1.0f - ((float) Math.pow(0.9200000166893005d, tickLerp));
            float dropLerp = 1.0f - ((float) Math.pow(0.6499999761581421d, tickLerp));
            float target = powered ? 0.95f : 0.0f;
            this.extension = class_3532.method_16439(extendLerp, this.extension, target);
            float targetFall = 0.0f;
            if (!onGround) {
                float fallSpeed = class_3532.method_15363(-deltaY, 0.0f, InHandBlockEntityRenderer.PISTON_FALL_SPEED_MAX);
                float speedFactor = fallSpeed / InHandBlockEntityRenderer.PISTON_FALL_SPEED_MAX;
                float distanceFactor = class_3532.method_15363(fallDistance / InHandBlockEntityRenderer.PISTON_FALL_DISTANCE_MAX, 0.0f, 1.0f);
                targetFall = 0.65f * Math.max(speedFactor, distanceFactor) * 1.15f;
            }
            float lerp = targetFall > this.fallOffset ? riseLerp : dropLerp;
            if (onGround && this.fallOffset > 0.0f && targetFall == 0.0f) {
                lerp = Math.min(1.0f, lerp * 1.5f);
            }
            this.fallOffset = class_3532.method_16439(lerp, this.fallOffset, targetFall);
            if (powered != this.extended) {
                this.extended = powered;
                float pitch = powered ? 0.7f : InHandBlockEntityRenderer.PISTON_FALL_SPEED_MAX;
                level.method_8486(player.method_23317(), player.method_23318(), player.method_23321(), powered ? class_3417.field_15134 : class_3417.field_15228, class_3419.field_15245, 0.3f, pitch, false);
            }
        }
    }
}
