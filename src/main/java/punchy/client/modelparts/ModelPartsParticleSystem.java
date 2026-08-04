package punchy.client.modelparts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongConsumer;
import net.minecraft.class_1044;
import net.minecraft.class_10799;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1306;
import net.minecraft.class_1921;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_2394;
import net.minecraft.class_2396;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_2769;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_5321;
import net.minecraft.class_5819;
import net.minecraft.class_630;
import net.minecraft.class_638;
import net.minecraft.class_6862;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_761;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_9851;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.data.AnimationClip;
import punchy.client.bedrockitems.BedrockItemAnimationManager;
import punchy.client.modelparts.util.ModelPartMath;
import punchy.client.state.AttackActionTracker;
import punchy.client.state.MiningActionTracker;
import punchy.client.state.UseActionTracker;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;
import punchy.mixin.client.accessor.ModelPartAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem.class */
public final class ModelPartsParticleSystem {
    private static final float EPS = 1.0E-4f;
    private static final float TWO_PI = 6.2831855f;
    private static final float EMISSIVE_BLUR_SCALE = 1.08f;
    private static final float EMISSIVE_BLUR_ALPHA = 0.4f;
    private static final float EMISSIVE_BLUR_COLOR = 0.6f;
    private static final long ITEM_PARTICLE_WORLD_RELEASE_DELAY_NANOS = 25000000;
    private static final long BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS = 0;
    private static final float DEFAULT_WORLD_PREVIEW_BEHIND_LOCAL_BIAS = 0.0015f;
    private static final double BLOCK_FACE_SURFACE_EPSILON = 0.0015d;
    private static final double PARTICLE_COLLISION_SURFACE_EPSILON = 0.0025d;
    private static final double PARTICLE_COLLISION_REST_EPSILON = 0.0035d;
    private static final double PARTICLE_OCCLUSION_ENTRY_MARGIN = 0.22d;
    private static final double PARTICLE_OCCLUSION_EPSILON = 0.01d;
    private static final float PARTICLE_TEXTURE_ANIMATION_FPS = 12.0f;
    private static final int MAX_PARTICLES_PER_EVENT = 120;
    private static final Vector3f GHOST_ANCHOR_RENDER_OFFSET = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final EnumMap<class_1306, PlaybackState> PLAYBACK = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, List<PendingParticleEvent>> PENDING = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> PENDING_TICK = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> EMITTED_TICK = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, List<ActiveEmitter>> EMITTERS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> LAST_EMITTER_RENDER_NANOS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Map<String, PassiveEmitter>> DEFINITION_EMITTERS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> LAST_DEFINITION_EMITTER_NANOS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, TriggerRuntimeState> LAST_TRIGGER_STATE = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> LAST_USE_TRIGGER_EPOCH = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> LAST_ATTACK_TRIGGER_EPOCH = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> LAST_MINING_TRIGGER_EPOCH = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, List<RenderParticle>> ACTIVE_RENDER_PARTICLES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, AnimationTriggerWindow> ANIMATION_TRIGGER_WINDOWS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Matrix4f> LAST_SIM_ROOT = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> LAST_SIM_NANOS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, class_243> LAST_CAMERA_POS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Quaternionf> LAST_CAMERA_ROT = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Quaternionf> LAST_CAMERA_TURN_DELTA_CAMERA_LOCAL = new EnumMap<>(class_1306.class);
    private static final Map<String, class_2960> PARTICLE_TEXTURE_CACHE = new HashMap();
    private static final Map<class_2960, Integer> PARTICLE_ANIM_FRAME_COUNT_CACHE = new HashMap();
    private static final EnumMap<class_1306, Matrix4f> RENDER_ROOT = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Matrix4f> RENDER_ROOT_INV = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Matrix4f> EXACT_INHAND_GHOST_ROOT = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, String> ACTIVE_DEFINITION_SOURCE = new EnumMap<>(class_1306.class);
    private static final class_2960 DEFAULT_PARTICLE_TEXTURE = class_2960.method_60655("minecraft", "textures/particle/flame.png");
    private static final RenderPipeline PARTICLE_EMISSIVE_OVERLAY_PIPELINE = createParticleEmissiveOverlayPipeline();
    private static final Map<ParticleEmissiveKey, class_1921> PARTICLE_EMISSIVE_OVERLAY_TYPES = new HashMap();

    private ModelPartsParticleSystem() {
    }

    public static void resetArm(class_1306 arm) {
        if (arm == null) {
            return;
        }
        clearArmControllerState(arm);
        purgeArmParticles(arm);
    }

    public static void clearArmAnimationState(class_1306 arm) {
        if (arm == null) {
            return;
        }
        PLAYBACK.remove(arm);
        PENDING.remove(arm);
        PENDING_TICK.remove(arm);
        EMITTED_TICK.remove(arm);
        EMITTERS.remove(arm);
        LAST_EMITTER_RENDER_NANOS.remove(arm);
        ANIMATION_TRIGGER_WINDOWS.remove(arm);
    }

    public static void clearArmControllerState(class_1306 arm) {
        if (arm == null) {
            return;
        }
        clearArmAnimationState(arm);
        DEFINITION_EMITTERS.remove(arm);
        LAST_DEFINITION_EMITTER_NANOS.remove(arm);
        ACTIVE_DEFINITION_SOURCE.remove(arm);
        LAST_TRIGGER_STATE.remove(arm);
        LAST_ATTACK_TRIGGER_EPOCH.remove(arm);
        LAST_MINING_TRIGGER_EPOCH.remove(arm);
    }

    public static void resetArmPlayback(class_1306 arm) {
        clearArmAnimationState(arm);
    }

    public static void purgeArmParticles(class_1306 arm) {
        if (arm == null) {
            return;
        }
        ACTIVE_RENDER_PARTICLES.remove(arm);
        LAST_SIM_ROOT.remove(arm);
        LAST_SIM_NANOS.remove(arm);
        LAST_CAMERA_POS.remove(arm);
        LAST_CAMERA_ROT.remove(arm);
        LAST_CAMERA_TURN_DELTA_CAMERA_LOCAL.remove(arm);
        RENDER_ROOT.remove(arm);
        RENDER_ROOT_INV.remove(arm);
        EXACT_INHAND_GHOST_ROOT.remove(arm);
    }

    public static void resetAll() {
        for (class_1306 arm : class_1306.values()) {
            resetArm(arm);
        }
    }

    public static void setRenderRoot(class_1306 arm, Matrix4f rootRenderMatrix) {
        if (arm == null || rootRenderMatrix == null) {
            return;
        }
        Matrix4f root = new Matrix4f(rootRenderMatrix);
        RENDER_ROOT.put(arm, root);
        RENDER_ROOT_INV.put(arm, new Matrix4f(root).invert());
    }

    public static void setExactInHandGhostRoot(class_1306 arm, Matrix4f rootRenderMatrix) {
        if (arm == null || rootRenderMatrix == null) {
            return;
        }
        EXACT_INHAND_GHOST_ROOT.put(arm, new Matrix4f(rootRenderMatrix));
    }

    public static void onHandlerTick(class_310 client, class_1306 arm, PoseHandler handler) {
        if (!PunchyConfig.enableParticles()) {
            if (arm != null) {
                resetArm(arm);
                return;
            }
            return;
        }
        if (client == null || client.field_1687 == null || arm == null || handler == null) {
            return;
        }
        long gameTick = client.field_1687.method_8510();
        PlaybackState state = (PlaybackState) PLAYBACK.computeIfAbsent(arm, key -> {
            return new PlaybackState();
        });
        if (state.lastProcessedTick == gameTick) {
            return;
        }
        state.lastProcessedTick = gameTick;
        AnimationClip clip = handler.getCurrentClip();
        boolean active = handler.isPlaying() && clip != null;
        if (!active) {
            state.lastClip = null;
            state.lastTime = 0.0f;
            PENDING.remove(arm);
            PENDING_TICK.remove(arm);
            EMITTERS.remove(arm);
            LAST_EMITTER_RENDER_NANOS.remove(arm);
            ANIMATION_TRIGGER_WINDOWS.remove(arm);
            return;
        }
        float currentTime = handler.getCurrentTime();
        updateAnimationTriggerWindow(arm, state.lastClip, state.lastTime, clip, currentTime);
        PENDING.remove(arm);
        PENDING_TICK.remove(arm);
        EMITTERS.remove(arm);
        LAST_EMITTER_RENDER_NANOS.remove(arm);
        state.lastClip = clip;
        state.lastTime = currentTime;
    }

    public static void emitPendingForParts(class_310 client, class_1306 arm, Map<String, class_630> parts) {
        Long pendingTick;
        if (!PunchyConfig.enableParticles()) {
            if (arm != null) {
                resetArm(arm);
                return;
            }
            return;
        }
        if (client == null || client.field_1687 == null || client.field_1724 == null || arm == null || parts == null || parts.isEmpty() || (pendingTick = PENDING_TICK.get(arm)) == null || pendingTick.longValue() != client.field_1687.method_8510() || EMITTED_TICK.getOrDefault(arm, Long.MIN_VALUE) == pendingTick) {
            return;
        }
        List<PendingParticleEvent> events = PENDING.get(arm);
        if (events == null || events.isEmpty()) {
            EMITTED_TICK.put(arm, pendingTick);
            return;
        }
        class_746 player = client.field_1724;
        for (PendingParticleEvent pending : events) {
            spawnEvent(client, player, arm, parts, pending);
        }
        events.clear();
        EMITTED_TICK.put(arm, pendingTick);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public static void emitPendingForRenderBones(class_310 client, class_1306 arm, Map<String, Matrix4f> renderBoneTransforms) throws MatchException {
        if (!PunchyConfig.enableParticles()) {
            if (arm != null) {
                resetArm(arm);
                return;
            }
            return;
        }
        if (isParticleSuppressedByActiveItemAnimation(arm)) {
            resetArm(arm);
            return;
        }
        if (client == null || client.field_1687 == null || client.field_1724 == null || arm == null || renderBoneTransforms == null || renderBoneTransforms.isEmpty()) {
            return;
        }
        class_746 player = client.field_1724;
        emitFromActiveEmittersRender(client, player, arm, renderBoneTransforms);
        Long pendingTick = PENDING_TICK.get(arm);
        if (pendingTick == null || pendingTick.longValue() != client.field_1687.method_8510() || EMITTED_TICK.getOrDefault(arm, Long.MIN_VALUE) == pendingTick) {
            return;
        }
        List<PendingParticleEvent> events = PENDING.get(arm);
        if (events == null || events.isEmpty()) {
            EMITTED_TICK.put(arm, pendingTick);
            return;
        }
        for (PendingParticleEvent pending : events) {
            spawnEventFromRender(client, player, arm, renderBoneTransforms, pending);
        }
        events.clear();
        EMITTED_TICK.put(arm, pendingTick);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public static void emitDefinitionForRenderBones(class_310 client, class_1306 arm, Map<String, Matrix4f> renderBoneTransforms, List<String> definitionEffects) throws MatchException {
        if (!PunchyConfig.enableParticles()) {
            if (arm != null) {
                resetArm(arm);
                return;
            }
            return;
        }
        if (isParticleSuppressedByActiveItemAnimation(arm)) {
            resetArm(arm);
            return;
        }
        if (client == null || client.field_1687 == null || client.field_1724 == null || arm == null || renderBoneTransforms == null || renderBoneTransforms.isEmpty()) {
            return;
        }
        resetDefinitionEmittersIfSourceChanged(arm, "render_bones");
        if (definitionEffects == null || definitionEffects.isEmpty()) {
            DEFINITION_EMITTERS.remove(arm);
            LAST_DEFINITION_EMITTER_NANOS.remove(arm);
            return;
        }
        long now = System.nanoTime();
        long last = ((Long) LAST_DEFINITION_EMITTER_NANOS.getOrDefault(arm, Long.valueOf(BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS))).longValue();
        LAST_DEFINITION_EMITTER_NANOS.put(arm, Long.valueOf(now));
        float dt = class_3532.method_15363(last <= BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS ? 0.016666668f : (float) ((now - last) / 1.0E9d), 0.0f, 0.1f);
        Map<String, PassiveEmitter> emitters = (Map) DEFINITION_EMITTERS.computeIfAbsent(arm, key -> {
            return new HashMap();
        });
        Set<String> activeKeys = new HashSet<>();
        class_746 player = client.field_1724;
        long gameTick = client.field_1687.method_8510();
        TriggerContext triggerContext = resolveTriggerContext(client, arm);
        for (String rawEffect : definitionEffects) {
            if (rawEffect != null && !rawEffect.isBlank()) {
                String effect = rawEffect.trim();
                activeKeys.add(effect);
                PassiveEmitter emitter = emitters.get(effect);
                if (emitter == null) {
                    ParticleScriptSpec spec = parseParticleSpec(effect);
                    if (spec != null) {
                        emitter = new PassiveEmitter(spec);
                        emitters.put(effect, emitter);
                    }
                }
                emitter.ageSeconds += dt;
                if (emitter.spec.emitForSeconds <= 0.0f || emitter.ageSeconds <= emitter.spec.emitForSeconds + EPS) {
                    if (matchesDefinitionTriggers(client, emitter.spec, triggerContext)) {
                        boolean pulseOnly = hasPulseTriggers(emitter.spec);
                        boolean pulseConfigured = hasAnyPulseTriggers(emitter.spec);
                        if (pulseConfigured) {
                            if (consumePulseEvents(emitter, arm, triggerContext)) {
                                spawnEventFromRender(client, player, arm, renderBoneTransforms, new PendingParticleEvent(emitter.spec, -1.0f, 1.0f));
                            }
                            if (pulseOnly) {
                            }
                        }
                        if (emitter.spec.emitIntervalSeconds > 0.0f) {
                            if (canEmitByInterval(emitter.spec, emitter, now)) {
                                spawnEventFromRender(client, player, arm, renderBoneTransforms, new PendingParticleEvent(emitter.spec, -1.0f, 1.0f));
                            }
                        } else if (emitter.spec.ratePerSecond > 0.0f) {
                            emitter.accumulator += (double) (emitter.spec.ratePerSecond * dt);
                            int pulses = class_3532.method_15340((int) Math.floor(emitter.accumulator), 0, 24);
                            if (pulses > 0) {
                                emitter.accumulator -= (double) pulses;
                                for (int i = 0; i < pulses; i++) {
                                    spawnEventFromRender(client, player, arm, renderBoneTransforms, new PendingParticleEvent(emitter.spec, -1.0f, 1.0f));
                                }
                            }
                        } else if (emitter.lastTick != gameTick) {
                            emitter.lastTick = gameTick;
                            if (canEmitByInterval(emitter.spec, emitter, now)) {
                                spawnEventFromRender(client, player, arm, renderBoneTransforms, new PendingParticleEvent(emitter.spec, -1.0f, 1.0f));
                            }
                        }
                    }
                }
            }
        }
        if (!emitters.isEmpty()) {
            emitters.entrySet().removeIf(entry -> {
                return !activeKeys.contains(entry.getKey());
            });
            if (emitters.isEmpty()) {
                DEFINITION_EMITTERS.remove(arm);
            }
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public static void emitDefinitionForItemRender(class_310 client, class_1306 arm, Matrix4f itemRenderMatrix, List<String> definitionEffects) throws MatchException {
        if (!PunchyConfig.enableParticles()) {
            if (arm != null) {
                resetArm(arm);
                return;
            }
            return;
        }
        if (isParticleSuppressedByActiveItemAnimation(arm)) {
            resetArm(arm);
            return;
        }
        if (client == null || client.field_1687 == null || client.field_1724 == null || arm == null || itemRenderMatrix == null) {
            return;
        }
        resetDefinitionEmittersIfSourceChanged(arm, "item_render");
        if (definitionEffects == null || definitionEffects.isEmpty()) {
            DEFINITION_EMITTERS.remove(arm);
            LAST_DEFINITION_EMITTER_NANOS.remove(arm);
            return;
        }
        long now = System.nanoTime();
        long last = ((Long) LAST_DEFINITION_EMITTER_NANOS.getOrDefault(arm, Long.valueOf(BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS))).longValue();
        LAST_DEFINITION_EMITTER_NANOS.put(arm, Long.valueOf(now));
        float dt = class_3532.method_15363(last <= BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS ? 0.016666668f : (float) ((now - last) / 1.0E9d), 0.0f, 0.1f);
        Map<String, PassiveEmitter> emitters = (Map) DEFINITION_EMITTERS.computeIfAbsent(arm, key -> {
            return new HashMap();
        });
        Set<String> activeKeys = new HashSet<>();
        class_746 player = client.field_1724;
        long gameTick = client.field_1687.method_8510();
        TriggerContext triggerContext = resolveTriggerContext(client, arm);
        for (String rawEffect : definitionEffects) {
            if (rawEffect != null && !rawEffect.isBlank()) {
                String effect = rawEffect.trim();
                activeKeys.add(effect);
                PassiveEmitter emitter = emitters.get(effect);
                if (emitter == null) {
                    ParticleScriptSpec spec = parseParticleSpec(effect);
                    if (spec != null) {
                        emitter = new PassiveEmitter(spec);
                        emitters.put(effect, emitter);
                    }
                }
                emitter.ageSeconds += dt;
                if (emitter.spec.emitForSeconds <= 0.0f || emitter.ageSeconds <= emitter.spec.emitForSeconds + EPS) {
                    if (matchesDefinitionTriggers(client, emitter.spec, triggerContext)) {
                        boolean pulseOnly = hasPulseTriggers(emitter.spec);
                        boolean pulseConfigured = hasAnyPulseTriggers(emitter.spec);
                        if (pulseConfigured) {
                            if (consumePulseEvents(emitter, arm, triggerContext)) {
                                spawnEventForItemRender(client, player, arm, itemRenderMatrix, new PendingParticleEvent(emitter.spec, -1.0f, 1.0f));
                            }
                            if (pulseOnly) {
                            }
                        }
                        if (emitter.spec.emitIntervalSeconds > 0.0f) {
                            if (canEmitByInterval(emitter.spec, emitter, now)) {
                                spawnEventForItemRender(client, player, arm, itemRenderMatrix, new PendingParticleEvent(emitter.spec, -1.0f, 1.0f));
                            }
                        } else if (emitter.spec.ratePerSecond > 0.0f) {
                            emitter.accumulator += (double) (emitter.spec.ratePerSecond * dt);
                            int pulses = class_3532.method_15340((int) Math.floor(emitter.accumulator), 0, 24);
                            if (pulses > 0) {
                                emitter.accumulator -= (double) pulses;
                                for (int i = 0; i < pulses; i++) {
                                    spawnEventForItemRender(client, player, arm, itemRenderMatrix, new PendingParticleEvent(emitter.spec, -1.0f, 1.0f));
                                }
                            }
                        } else if (emitter.lastTick != gameTick) {
                            emitter.lastTick = gameTick;
                            if (canEmitByInterval(emitter.spec, emitter, now)) {
                                spawnEventForItemRender(client, player, arm, itemRenderMatrix, new PendingParticleEvent(emitter.spec, -1.0f, 1.0f));
                            }
                        }
                    }
                }
            }
        }
        if (!emitters.isEmpty()) {
            emitters.entrySet().removeIf(entry -> {
                return !activeKeys.contains(entry.getKey());
            });
            if (emitters.isEmpty()) {
                DEFINITION_EMITTERS.remove(arm);
            }
        }
    }

    private static void resetDefinitionEmittersIfSourceChanged(class_1306 arm, String sourceKey) {
        if (arm == null || sourceKey == null) {
            return;
        }
        String last = ACTIVE_DEFINITION_SOURCE.get(arm);
        if (last != null && !last.equals(sourceKey)) {
            DEFINITION_EMITTERS.remove(arm);
            LAST_DEFINITION_EMITTER_NANOS.remove(arm);
            purgeArmParticles(arm);
        }
        ACTIVE_DEFINITION_SOURCE.put(arm, sourceKey);
    }

    private static TriggerContext resolveTriggerContext(class_310 client, class_1306 arm) {
        boolean z;
        if (client == null || client.field_1724 == null || client.field_1687 == null || arm == null) {
            return TriggerContext.EMPTY;
        }
        class_746 player = client.field_1724;
        TriggerRuntimeState runtime = (TriggerRuntimeState) LAST_TRIGGER_STATE.computeIfAbsent(arm, key -> {
            return new TriggerRuntimeState();
        });
        boolean onGround = player.method_24828();
        class_243 move = player.method_18798();
        double horizontalSq = (move.field_1352 * move.field_1352) + (move.field_1350 * move.field_1350);
        boolean movingHorizontal = horizontalSq > 1.0E-4d;
        boolean movementInput = client.field_1690 != null && (client.field_1690.field_1894.method_1434() || client.field_1690.field_1881.method_1434() || client.field_1690.field_1913.method_1434() || client.field_1690.field_1849.method_1434());
        boolean swimming = player.method_5681() || player.method_5799();
        boolean sprinting = player.method_5624() && (movingHorizontal || movementInput);
        boolean walking = (movingHorizontal || movementInput) && onGround && !sprinting && !swimming;
        float fallDistance = Math.max(0.0f, (float) player.field_6017);
        boolean falling = !onGround && move.field_1351 < -0.03d && fallDistance > 0.0f;
        boolean attackKeyDown = client.field_1690 != null && client.field_1690.field_1886.method_1434();
        boolean useKeyDown = client.field_1690 != null && client.field_1690.field_1904.method_1434();
        class_239 hit = client.field_1765;
        boolean aimingBlock = hit != null && hit.method_17783() == class_239.class_240.field_1332;
        boolean isMainArm = arm == player.method_6068();
        class_1268 armHand = resolveHand(player, arm);
        boolean usingItem = player.method_6115() && useKeyDown;
        class_1268 usedHand = usingItem ? player.method_6058() : null;
        boolean useItem = usingItem && usedHand == armHand;
        UseActionTracker.Trigger useTrigger = UseActionTracker.peekTrigger();
        long useEpoch = UseActionTracker.peekEpoch();
        class_1268 useEventHand = useTrigger != null ? useTrigger.hand() : UseActionTracker.peekLastHand();
        boolean useHandMatches = useEventHand == armHand;
        class_1268 trackedAttackHand = AttackActionTracker.peekHand();
        long attackEpoch = AttackActionTracker.peekEpoch();
        long miningEpoch = MiningActionTracker.peekEpoch();
        if (trackedAttackHand == null) {
            z = isMainArm;
        } else {
            z = trackedAttackHand == armHand;
        }
        boolean attackHandMatches = z;
        boolean mining = isMainArm && MiningActionTracker.isMiningActive() && aimingBlock;
        boolean jumpPulse = false;
        boolean landingPulse = false;
        boolean attackingPulse = false;
        boolean miningPulse = false;
        boolean useItemPulse = false;
        boolean attackTrackerPulse = false;
        if (attackHandMatches) {
            long lastSeenAttackEpoch = ((Long) LAST_ATTACK_TRIGGER_EPOCH.getOrDefault(arm, Long.valueOf(BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS))).longValue();
            if (attackEpoch > lastSeenAttackEpoch) {
                attackTrackerPulse = true;
                LAST_ATTACK_TRIGGER_EPOCH.put(arm, Long.valueOf(attackEpoch));
            }
        }
        boolean miningTrackerPulse = false;
        if (isMainArm) {
            long lastSeenMiningEpoch = ((Long) LAST_MINING_TRIGGER_EPOCH.getOrDefault(arm, Long.valueOf(BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS))).longValue();
            if (miningEpoch > lastSeenMiningEpoch) {
                miningTrackerPulse = true;
                LAST_MINING_TRIGGER_EPOCH.put(arm, Long.valueOf(miningEpoch));
            }
        }
        if (runtime.initialized) {
            boolean jumpTransition = !onGround && runtime.lastOnGround && (move.field_1351 > 0.04d || (client.field_1690 != null && client.field_1690.field_1903.method_1434()));
            if (jumpTransition) {
                runtime.jumpEventId++;
                runtime.jumpGraceTicks = 2;
            }
            if (runtime.jumpGraceTicks > 0) {
                jumpPulse = true;
                runtime.jumpGraceTicks--;
            }
            boolean landingTransition = onGround && !runtime.lastOnGround;
            if (landingTransition) {
                runtime.landingEventId++;
                runtime.landingGraceTicks = 2;
            }
            if (onGround && runtime.landingGraceTicks > 0) {
                landingPulse = true;
                runtime.landingGraceTicks--;
            } else if (!onGround) {
                runtime.landingGraceTicks = 0;
            }
            boolean attackTransition = attackHandMatches && ((player.field_6252 && !runtime.lastSwinging) || (attackKeyDown && !runtime.lastAttackKeyDown));
            if (attackTransition || attackTrackerPulse) {
                runtime.attackEventId++;
                runtime.attackGraceTicks = 2;
            }
            if (runtime.attackGraceTicks > 0) {
                attackingPulse = true;
                runtime.attackGraceTicks--;
            }
            if (miningTrackerPulse) {
                runtime.miningEventId++;
                runtime.miningGraceTicks = 2;
            }
            if (runtime.miningGraceTicks > 0) {
                miningPulse = true;
                runtime.miningGraceTicks--;
            }
            if (useHandMatches) {
                long lastSeenEpoch = ((Long) LAST_USE_TRIGGER_EPOCH.getOrDefault(arm, Long.valueOf(BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS))).longValue();
                if (useEpoch > lastSeenEpoch) {
                    useItemPulse = true;
                    LAST_USE_TRIGGER_EPOCH.put(arm, Long.valueOf(useEpoch));
                }
            }
        } else {
            runtime.initialized = true;
            if (attackTrackerPulse) {
                runtime.attackEventId++;
                runtime.attackGraceTicks = 1;
                attackingPulse = true;
            }
            if (miningTrackerPulse) {
                runtime.miningEventId++;
                runtime.miningGraceTicks = 1;
                miningPulse = true;
            }
            if (useHandMatches) {
                long lastSeenEpoch2 = ((Long) LAST_USE_TRIGGER_EPOCH.getOrDefault(arm, Long.valueOf(BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS))).longValue();
                if (useEpoch > lastSeenEpoch2) {
                    useItemPulse = true;
                    LAST_USE_TRIGGER_EPOCH.put(arm, Long.valueOf(useEpoch));
                }
            }
        }
        runtime.lastOnGround = onGround;
        runtime.lastAttackKeyDown = attackKeyDown;
        runtime.lastSwinging = player.field_6252;
        boolean mining2 = mining || miningPulse;
        boolean useItem2 = useItem || useItemPulse;
        long useItemEventId = useItemPulse ? useEpoch : BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        return new TriggerContext(jumpPulse, landingPulse, walking, sprinting, swimming, falling, fallDistance, attackingPulse, useItem2, mining2, runtime.jumpEventId, runtime.landingEventId, runtime.attackEventId, runtime.miningEventId, useItemEventId);
    }

    private static boolean isDamageableAttackTarget(class_239 hit) {
        if (!(hit instanceof class_3966)) {
            return false;
        }
        class_3966 entityHit = (class_3966) hit;
        class_1297 entity = entityHit.method_17782();
        return (entity == null || !entity.method_5805() || entity.method_7325() || !entity.method_5732() || entity.method_5655()) ? false : true;
    }

    private static class_1268 resolveHand(class_746 player, class_1306 arm) {
        if (player == null || arm == null) {
            return class_1268.field_5808;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? class_1268.field_5808 : class_1268.field_5810 : mainRight ? class_1268.field_5810 : class_1268.field_5808;
    }

    private static boolean isParticleSuppressedByActiveItemAnimation(class_1306 arm) {
        PunchyTuningConfig.CustomAnimationEntry entry;
        return (arm == null || (entry = BedrockItemAnimationManager.getActiveEntry(arm)) == null || !Boolean.TRUE.equals(entry.disable_all_particles)) ? false : true;
    }

    private static boolean matchesDefinitionTriggers(class_310 client, ParticleScriptSpec spec, TriggerContext context) {
        if (spec == null || context == null || !hasDefinitionTriggers(spec)) {
            return true;
        }
        MiningActionTracker.Target miningTarget = resolveMiningTarget(client, spec);
        if (spec.triggerModeAny) {
            boolean anyConfigured = false;
            boolean anyMatched = false;
            if (spec.triggerJump) {
                anyConfigured = true;
                anyMatched = false | context.jump;
            }
            if (spec.triggerLanding) {
                anyConfigured = true;
                anyMatched |= context.landing;
            }
            if (spec.triggerWalking) {
                anyConfigured = true;
                anyMatched |= context.walking;
            }
            if (spec.triggerSprinting) {
                anyConfigured = true;
                anyMatched |= context.sprinting;
            }
            if (spec.triggerSwimming) {
                anyConfigured = true;
                anyMatched |= context.swimming;
            }
            if (spec.triggerFalling) {
                anyConfigured = true;
                anyMatched |= context.falling && context.fallDistance + EPS >= spec.triggerFallingMinBlocks;
            }
            if (spec.triggerAttacking) {
                anyConfigured = true;
                anyMatched |= context.attacking;
            }
            if (spec.triggerUseItem) {
                anyConfigured = true;
                anyMatched |= context.useItem;
            }
            if (spec.triggerMining) {
                anyConfigured = true;
                anyMatched |= context.mining;
            }
            if (spec.triggerBiomeNames != null && !spec.triggerBiomeNames.isEmpty()) {
                anyConfigured = true;
                anyMatched |= matchesBiomeNames(client, spec.triggerBiomeNames);
            }
            if (hasMiningTargetFilter(spec)) {
                if (!anyConfigured) {
                    return matchesMiningTarget(spec, miningTarget);
                }
                if (!matchesMiningTarget(spec, miningTarget)) {
                    return false;
                }
            }
            return !anyConfigured || anyMatched;
        }
        if (spec.triggerJump && !context.jump) {
            return false;
        }
        if (spec.triggerLanding && !context.landing) {
            return false;
        }
        if (spec.triggerWalking && !context.walking) {
            return false;
        }
        if (spec.triggerSprinting && !context.sprinting) {
            return false;
        }
        if (spec.triggerSwimming && !context.swimming) {
            return false;
        }
        if (spec.triggerFalling && (!context.falling || context.fallDistance + EPS < spec.triggerFallingMinBlocks)) {
            return false;
        }
        if (spec.triggerAttacking && !context.attacking) {
            return false;
        }
        if (spec.triggerUseItem && !context.useItem) {
            return false;
        }
        if (spec.triggerMining && !context.mining) {
            return false;
        }
        if (spec.triggerBiomeNames != null && !spec.triggerBiomeNames.isEmpty() && !matchesBiomeNames(client, spec.triggerBiomeNames)) {
            return false;
        }
        if (hasMiningTargetFilter(spec) && !matchesMiningTarget(spec, miningTarget)) {
            return false;
        }
        return true;
    }

    private static boolean matchesBiomeNames(class_310 client, Set<String> targets) {
        if (client == null || client.field_1687 == null || client.field_1724 == null || targets == null || targets.isEmpty()) {
            return false;
        }
        class_638 class_638Var = client.field_1687;
        class_2338 pos = client.field_1724.method_24515();
        class_6880<class_1959> holder = class_638Var.method_23753(pos);
        for (String entry : targets) {
            if (entry != null && !entry.isBlank()) {
                String trimmed = entry.trim();
                if (trimmed.startsWith("#")) {
                    class_2960 tagId = class_2960.method_12829(trimmed.substring(1));
                    if (tagId != null) {
                        class_6862<class_1959> tag = class_6862.method_40092(class_7924.field_41236, tagId);
                        if (holder.method_40220(tag)) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                } else {
                    class_2960 parsed = class_2960.method_12829(trimmed);
                    if (parsed != null) {
                        class_5321<class_1959> key = class_5321.method_29179(class_7924.field_41236, parsed);
                        if (holder.method_40225(key)) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        return false;
    }

    private static MiningActionTracker.Target resolveMiningTarget(class_310 client, ParticleScriptSpec spec) {
        if (!hasMiningTargetFilter(spec)) {
            return null;
        }
        MiningActionTracker.Target tracked = MiningActionTracker.peekTarget();
        if (tracked != null) {
            return tracked;
        }
        MiningActionTracker.Target tracked2 = MiningActionTracker.peekRecentTarget(250000000L);
        if (tracked2 != null) {
            return tracked2;
        }
        if (!isAnyBlockFaceTarget(spec) || client == null || client.field_1687 == null) {
            return null;
        }
        class_3965 class_3965Var = client.field_1765;
        if (!(class_3965Var instanceof class_3965)) {
            return null;
        }
        class_3965 blockHit = class_3965Var;
        class_2338 pos = blockHit.method_17777();
        class_2680 state = client.field_1687.method_8320(pos);
        if (pos == null || state == null || state.method_26215()) {
            return null;
        }
        return new MiningActionTracker.Target(pos, blockHit.method_17780(), state, blockHit.method_17784());
    }

    private static boolean hasMiningTargetFilter(ParticleScriptSpec spec) {
        if (spec == null) {
            return false;
        }
        return isAnyBlockFaceTarget(spec) || !((spec.triggerBlockIds == null || spec.triggerBlockIds.isEmpty()) && (spec.triggerBlockState == null || spec.triggerBlockState.isEmpty()));
    }

    private static boolean matchesMiningTarget(ParticleScriptSpec spec, MiningActionTracker.Target target) {
        if (!hasMiningTargetFilter(spec)) {
            return true;
        }
        if (target == null || target.state() == null || target.state().method_26215()) {
            return false;
        }
        if (spec.triggerBlockIds != null && !spec.triggerBlockIds.isEmpty() && !matchesBlockSelectors(target.state(), spec.triggerBlockIds)) {
            return false;
        }
        if (spec.triggerBlockState != null && !spec.triggerBlockState.isEmpty() && !matchesRequiredBlockState(target.state(), spec.triggerBlockState)) {
            return false;
        }
        return true;
    }

    private static boolean matchesBlockSelectors(class_2680 state, Set<String> selectors) {
        if (state == null || selectors == null || selectors.isEmpty()) {
            return true;
        }
        class_2960 blockId = class_7923.field_41175.method_10221(state.method_26204());
        if (blockId == null) {
            return false;
        }
        String current = blockId.toString();
        for (String entry : selectors) {
            if (entry != null && !entry.isBlank()) {
                String normalized = entry.trim().toLowerCase(Locale.ROOT);
                if (normalized.startsWith("#")) {
                    class_2960 tagId = class_2960.method_12829(normalized.substring(1));
                    if (tagId != null && state.method_26164(class_6862.method_40092(class_7924.field_41254, tagId))) {
                        return true;
                    }
                } else {
                    class_2960 parsed = class_2960.method_12829(normalized);
                    if (parsed != null && current.equals(parsed.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesRequiredBlockState(class_2680 state, Map<String, String> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        if (state == null) {
            return false;
        }
        for (Map.Entry<String, String> entry : required.entrySet()) {
            String key = entry.getKey();
            String wanted = entry.getValue();
            if (key != null && !key.isBlank() && wanted != null && !wanted.isBlank()) {
                class_2769<?> property = findBlockProperty(state, key.trim());
                if (property == null) {
                    return false;
                }
                String actual = String.valueOf(state.method_11654(property));
                if (!wanted.trim().equalsIgnoreCase(actual)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static class_2769<?> findBlockProperty(class_2680 state, String name) {
        if (state == null || name == null || name.isBlank()) {
            return null;
        }
        for (class_2769<?> property : state.method_28501()) {
            if (property.method_11899().equalsIgnoreCase(name)) {
                return property;
            }
        }
        return null;
    }

    private static boolean hasDefinitionTriggers(ParticleScriptSpec spec) {
        if (spec == null) {
            return false;
        }
        return spec.triggerJump || spec.triggerLanding || spec.triggerWalking || spec.triggerSprinting || spec.triggerSwimming || spec.triggerFalling || spec.triggerAttacking || spec.triggerUseItem || spec.triggerMining || spec.hasAnimationTrigger() || !(spec.triggerBiomeNames == null || spec.triggerBiomeNames.isEmpty()) || hasMiningTargetFilter(spec);
    }

    private static boolean hasPulseTriggers(ParticleScriptSpec spec) {
        if (spec == null) {
            return false;
        }
        boolean hasPulse = hasAnyPulseTriggers(spec);
        if (!hasPulse) {
            return false;
        }
        boolean hasContinuous = spec.triggerWalking || spec.triggerSprinting || spec.triggerSwimming || spec.triggerFalling || spec.triggerMining || !(spec.triggerBiomeNames == null || spec.triggerBiomeNames.isEmpty());
        return !hasContinuous;
    }

    private static boolean hasAnyPulseTriggers(ParticleScriptSpec spec) {
        if (spec == null) {
            return false;
        }
        return spec.triggerJump || spec.triggerLanding || spec.triggerAttacking || spec.triggerUseItem || spec.triggerMining || spec.hasAnimationTrigger();
    }

    private static boolean consumePulseEvents(PassiveEmitter emitter, class_1306 arm, TriggerContext context) {
        if (emitter == null || context == null || emitter.spec == null) {
            return false;
        }
        boolean emit = false;
        if (emitter.spec.triggerJump && context.jump && context.jumpEventId > emitter.lastJumpEventId) {
            emitter.lastJumpEventId = context.jumpEventId;
            emit = true;
        }
        if (emitter.spec.triggerLanding && context.landing && context.landingEventId > emitter.lastLandingEventId) {
            emitter.lastLandingEventId = context.landingEventId;
            emit = true;
        }
        if (emitter.spec.triggerAttacking && context.attacking && context.attackEventId > emitter.lastAttackEventId) {
            emitter.lastAttackEventId = context.attackEventId;
            emit = true;
        }
        if (emitter.spec.triggerMining && context.mining && context.miningEventId > emitter.lastMiningEventId) {
            emitter.lastMiningEventId = context.miningEventId;
            emit = true;
        }
        if (emitter.spec.triggerUseItem && context.useItemEventId > emitter.lastUseItemEventId) {
            emitter.lastUseItemEventId = context.useItemEventId;
            emit = true;
        }
        if (consumeAnimationPulse(emitter, arm)) {
            emit = true;
        }
        return emit;
    }

    private static boolean consumeAnimationPulse(PassiveEmitter emitter, class_1306 arm) {
        AnimationTriggerWindow window;
        if (emitter == null || emitter.spec == null || arm == null || !emitter.spec.hasAnimationTrigger() || (window = ANIMATION_TRIGGER_WINDOWS.get(arm)) == null || !window.active || window.serial <= BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS || emitter.lastAnimationWindowSerial == window.serial) {
            return false;
        }
        emitter.lastAnimationWindowSerial = window.serial;
        String wantedName = emitter.spec.triggerAnimationName == null ? "" : emitter.spec.triggerAnimationName.trim();
        String currentName = window.currentClipName == null ? "" : window.currentClipName;
        if (wantedName.isBlank() || currentName.isBlank() || !wantedName.equalsIgnoreCase(currentName)) {
            return false;
        }
        float targetTime = class_3532.method_15363(emitter.spec.triggerAnimationTimecode, 0.0f, Math.max(0.0f, window.clipLengthSeconds));
        float start = window.previousTimeSeconds;
        float end = window.currentTimeSeconds;
        if (window.clipChanged) {
            start = -1.0E-4f;
        }
        if (!window.looping || end + EPS >= start || window.clipLengthSeconds <= EPS) {
            return targetTime > start + EPS && targetTime <= end + EPS;
        }
        boolean tail = targetTime > start + EPS && targetTime <= window.clipLengthSeconds + EPS;
        boolean head = targetTime >= -1.0E-4f && targetTime <= end + EPS;
        return tail || head;
    }

    private static void updateAnimationTriggerWindow(class_1306 arm, AnimationClip previousClip, float previousTime, AnimationClip currentClip, float currentTime) {
        if (arm == null || currentClip == null) {
            ANIMATION_TRIGGER_WINDOWS.remove(arm);
            return;
        }
        AnimationTriggerWindow window = (AnimationTriggerWindow) ANIMATION_TRIGGER_WINDOWS.computeIfAbsent(arm, key -> {
            return new AnimationTriggerWindow();
        });
        window.serial++;
        window.active = true;
        window.currentClipName = currentClip.getName();
        window.currentTimeSeconds = currentTime;
        window.previousTimeSeconds = previousClip != currentClip ? -1.0E-4f : previousTime;
        window.clipLengthSeconds = Math.max(0.0f, currentClip.getLength());
        window.looping = currentClip.isLoop();
        window.clipChanged = previousClip != currentClip;
    }

    private static boolean canEmitByInterval(ParticleScriptSpec spec, PassiveEmitter emitter, long nowNanos) {
        if (spec == null || emitter == null) {
            return false;
        }
        return canEmitByInterval(spec.emitIntervalSeconds, nowNanos, emitter.lastEmitNanos, n -> {
            emitter.lastEmitNanos = n;
        });
    }

    private static boolean canEmitByInterval(ParticleScriptSpec spec, ActiveEmitter emitter, long nowNanos) {
        if (spec == null || emitter == null) {
            return false;
        }
        return canEmitByInterval(spec.emitIntervalSeconds, nowNanos, emitter.lastEmitNanos, n -> {
            emitter.lastEmitNanos = n;
        });
    }

    private static boolean canEmitByInterval(float intervalSeconds, long nowNanos, long lastEmitNanos, LongConsumer setter) {
        if (intervalSeconds <= 0.0f) {
            if (setter != null) {
                setter.accept(nowNanos);
                return true;
            }
            return true;
        }
        long intervalNanos = (long) Math.max(1.0f, intervalSeconds * 1.0E9f);
        if (lastEmitNanos > BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS && nowNanos - lastEmitNanos < intervalNanos) {
            return false;
        }
        if (setter != null) {
            setter.accept(nowNanos);
            return true;
        }
        return true;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void emitFromActiveEmittersRender(class_310 client, class_746 player, class_1306 arm, Map<String, Matrix4f> renderBoneTransforms) throws MatchException {
        List<ActiveEmitter> emitters = EMITTERS.get(arm);
        if (emitters == null || emitters.isEmpty() || client == null || renderBoneTransforms == null || renderBoneTransforms.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        long last = ((Long) LAST_EMITTER_RENDER_NANOS.getOrDefault(arm, Long.valueOf(BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS))).longValue();
        LAST_EMITTER_RENDER_NANOS.put(arm, Long.valueOf(now));
        float dt = class_3532.method_15363(last <= BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS ? 0.016666668f : (float) ((now - last) / 1.0E9d), 0.0f, 0.1f);
        for (int i = emitters.size() - 1; i >= 0; i--) {
            ActiveEmitter emitter = emitters.get(i);
            if (emitter == null || emitter.spec == null) {
                emitters.remove(i);
            } else {
                emitter.ageSeconds += dt;
                float totalSeconds = Math.max(0.05f, emitter.totalTicks / 20.0f);
                if (emitter.ageSeconds > totalSeconds + EPS) {
                    emitters.remove(i);
                } else {
                    float progress = class_3532.method_15363(emitter.ageSeconds / totalSeconds, 0.0f, 1.0f);
                    float lifeScale = emitter.spec.emitFullAnimation ? 1.0f : applyEase(emitter.spec.emitEase, progress);
                    if (emitter.spec.emitIntervalSeconds > 0.0f) {
                        if (canEmitByInterval(emitter.spec, emitter, now)) {
                            spawnEventFromRender(client, player, arm, renderBoneTransforms, new PendingParticleEvent(emitter.spec, -1.0f, lifeScale));
                        }
                    } else {
                        emitter.accumulator += (double) (emitter.spec.ratePerSecond * lifeScale * dt);
                        int pulses = (int) Math.floor(emitter.accumulator);
                        if (pulses > 0) {
                            emitter.accumulator -= (double) pulses;
                            for (int j = 0; j < pulses; j++) {
                                spawnEventFromRender(client, player, arm, renderBoneTransforms, new PendingParticleEvent(emitter.spec, -1.0f, lifeScale));
                            }
                        }
                    }
                }
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:189:0x02a2 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:190:0x029c A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static void renderForArm(net.minecraft.class_310 r14, net.minecraft.class_1306 r15, net.minecraft.class_4597 r16, net.minecraft.class_4587 r17, int r18) {
        /*
            Method dump skipped, instruction units count: 1440
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: punchy.client.modelparts.ModelPartsParticleSystem.renderForArm(net.minecraft.class_310, net.minecraft.class_1306, net.minecraft.class_4597, net.minecraft.class_4587, int):void");
    }

    public static void renderReleasedParticlesInWorld(class_310 client, class_4597 buffer, class_4587 worldBasePose) {
        float fMethod_15363;
        class_243 class_243Var;
        if (client == null || client.field_1687 == null || buffer == null || worldBasePose == null) {
            return;
        }
        class_4184 camera = client.field_1773 == null ? null : client.field_1773.method_19418();
        if (camera == null || camera.method_19326() == null || camera.method_23767() == null) {
            return;
        }
        Quaternionf cameraRot = new Quaternionf(camera.method_23767());
        class_243 cameraPos = camera.method_19326();
        boolean shaderPackInUse = isShaderPackInUse();
        int submitted = 0;
        for (class_1306 arm : class_1306.values()) {
            List<RenderParticle> particles = ACTIVE_RENDER_PARTICLES.get(arm);
            if (particles != null && !particles.isEmpty()) {
                for (RenderParticle particle : particles) {
                    if (particle != null && particle.renderInWorldPass && ((!particle.brightAtDark && !particle.glowTargetMenu) || PunchyConfig.enableBlurGlowAtDark())) {
                        if (!particle.lockInModel) {
                            boolean previewBehindLocal = particle.localSpace && particle.pendingWorldRelease && particle.pendingReleaseSnapshotReady && particle.worldPreviewBehindLocal;
                            if (!particle.localSpace || previewBehindLocal) {
                                if (particle.totalTicks <= 1) {
                                    fMethod_15363 = 1.0f;
                                } else {
                                    fMethod_15363 = class_3532.method_15363(particle.ageTicks / particle.totalTicks, 0.0f, 1.0f);
                                }
                                float progress = fMethod_15363;
                                float eased = applyEase(particle.ease, progress);
                                float size = class_3532.method_16439(eased, particle.sizeStart, particle.sizeEnd);
                                float alpha = class_3532.method_16439(eased, particle.alphaStart, particle.alphaEnd);
                                if (size > EPS && alpha > 0.001f) {
                                    if (previewBehindLocal && particle.pendingReleaseWorldPos != null) {
                                        class_243Var = particle.pendingReleaseWorldPos;
                                    } else {
                                        class_243Var = particle.position;
                                    }
                                    class_243 worldPos = class_243Var;
                                    if (previewBehindLocal) {
                                        worldPos = pushBehindLocalLayer(cameraPos, worldPos, particle.worldPreviewBehindLocalBias, cameraRot);
                                    }
                                    if (!isParticleOccludedByWorld(client, cameraPos, worldPos)) {
                                        class_243 delta = worldPos.method_1020(cameraPos);
                                        Vector3f renderPos = new Vector3f((float) delta.field_1352, (float) delta.field_1351, (float) delta.field_1350);
                                        if (isFinite(renderPos)) {
                                            class_4587 worldStack = new class_4587();
                                            worldStack.method_23760().method_23761().set(worldBasePose.method_23760().method_23761());
                                            worldStack.method_23760().method_23762().set(worldBasePose.method_23760().method_23762());
                                            worldStack.method_46416(renderPos.x, renderPos.y, renderPos.z);
                                            worldStack.method_22907(cameraRot);
                                            class_2960 baseTexture = particle.texture != null ? particle.texture : DEFAULT_PARTICLE_TEXTURE;
                                            class_2960 texture = resolveAnimatedParticleTexture(client, particle, baseTexture);
                                            int sampledLight = resolveParticleWorldLight(client, particle, arm, cameraPos, renderPos, 0);
                                            float darkGlowFactor = resolveBrightAtDarkGlowFactor(client, particle, sampledLight);
                                            float alpha2 = resolveParticleRenderAlpha(client, particle, sampledLight, alpha);
                                            if (alpha2 > 0.001f) {
                                                int light = resolveParticleLight(client, particle, sampledLight);
                                                float baseRed = class_3532.method_15363(particle.red, 0.0f, 1.0f);
                                                float baseGreen = class_3532.method_15363(particle.green, 0.0f, 1.0f);
                                                float baseBlue = class_3532.method_15363(particle.blue, 0.0f, 1.0f);
                                                boolean allowEmissiveOverlay = (isParticleEmissiveActive(client, particle) || darkGlowFactor > 0.001f) && (particle.lightPercent < 0.0f || particle.lightPercent >= 99.999f);
                                                boolean emissiveOnlyPass = allowEmissiveOverlay && !particle.emissiveBlur && particle.emissiveStrength >= 100;
                                                if (!emissiveOnlyPass) {
                                                    class_4588 baseConsumer = buffer.getBuffer(class_1921.method_23580(texture));
                                                    addBillboardQuad(worldStack.method_23760(), baseConsumer, new Vector3f(0.0f, 0.0f, 0.0f), size, alpha2, light, baseRed, baseGreen, baseBlue);
                                                    submitted++;
                                                }
                                                if (allowEmissiveOverlay) {
                                                    float emissiveBase = emissionToBaseIntensity(particle.emissiveStrength);
                                                    float emissiveGlow = emissionToExtraGlow(particle.emissiveStrength);
                                                    if (emissiveBase > 0.0f || emissiveGlow > 0.0f) {
                                                        float colorScale = Math.min(1.0f, emissiveBase);
                                                        float red = class_3532.method_15363(baseRed * colorScale, 0.0f, 1.0f);
                                                        float green = class_3532.method_15363(baseGreen * colorScale, 0.0f, 1.0f);
                                                        float blue = class_3532.method_15363(baseBlue * colorScale, 0.0f, 1.0f);
                                                        float overlayAlpha = emissiveOnlyPass ? alpha2 : Math.min(1.0f, alpha2 * 0.55f);
                                                        if (particle.brightAtDark) {
                                                            overlayAlpha *= darkGlowFactor;
                                                        }
                                                        class_1921 emissiveType = emissiveOverlayRenderType(texture, shaderPackInUse, particle.emissiveBlur, particle.emissiveClamp);
                                                        class_4588 emissiveConsumer = buffer.getBuffer(emissiveType);
                                                        addBillboardQuad(worldStack.method_23760(), emissiveConsumer, new Vector3f(0.0f, 0.0f, 0.0f), size, overlayAlpha, 15728880, red, green, blue);
                                                    }
                                                    if (particle.emissiveBlur && emissiveGlow > 0.0f) {
                                                        float red2 = class_3532.method_15363(baseRed * Math.min(1.0f, emissiveBase), 0.0f, 1.0f);
                                                        float green2 = class_3532.method_15363(baseGreen * Math.min(1.0f, emissiveBase), 0.0f, 1.0f);
                                                        float blue2 = class_3532.method_15363(baseBlue * Math.min(1.0f, emissiveBase), 0.0f, 1.0f);
                                                        float glowBoost = 1.0f + (emissiveGlow * 1.5f);
                                                        float blurScale = EMISSIVE_BLUR_SCALE + (emissiveGlow * 0.08f);
                                                        float blurColor = Math.min(1.0f, EMISSIVE_BLUR_COLOR + (emissiveGlow * EMISSIVE_BLUR_ALPHA));
                                                        float blurAlpha = Math.min(1.0f, alpha2 * EMISSIVE_BLUR_ALPHA * glowBoost);
                                                        if (particle.brightAtDark) {
                                                            blurAlpha *= darkGlowFactor;
                                                        }
                                                        addEmissiveBlurHalo(worldStack.method_23760(), buffer, texture, shaderPackInUse, particle.emissiveClamp, size, blurScale, blurAlpha, class_3532.method_15363(red2 * blurColor, 0.0f, 1.0f), class_3532.method_15363(green2 * blurColor, 0.0f, 1.0f), class_3532.method_15363(blue2 * blurColor, 0.0f, 1.0f));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void collectFromRange(AnimationClip clip, float startExclusive, float endInclusive, List<PendingParticleEvent> out) {
        if (clip == null || out == null || endInclusive + EPS < startExclusive) {
            return;
        }
        for (Map.Entry<Float, Set<String>> entry : clip.getTimelineEffectsUpTo(endInclusive)) {
            float time = entry.getKey().floatValue();
            if (time >= startExclusive + EPS && time <= endInclusive + EPS) {
                for (String effect : entry.getValue()) {
                    ParticleScriptSpec spec = parseParticleSpec(effect);
                    if (spec != null) {
                        out.add(new PendingParticleEvent(spec, time, 1.0f));
                    }
                }
            }
        }
    }

    private static void spawnEvent(class_310 client, class_746 player, class_1306 arm, Map<String, class_630> parts, PendingParticleEvent pending) {
        ParticleScriptSpec spec = pending.spec;
        if (spec == null) {
            return;
        }
        float eased = applyEase(spec.ease, spec.t) * class_3532.method_15363(pending.scale, 0.0f, 1.0f);
        int count = class_3532.method_15340(Math.round(spec.count * eased), 0, MAX_PARTICLES_PER_EVENT);
        if (count <= 0) {
            return;
        }
        if (spec.chance < 1.0f && client.field_1687.field_9229.method_43057() > spec.chance) {
            return;
        }
        class_2394 options = resolveParticle(spec.typeId);
        class_243 base = resolveBoneBase(player, arm, parts, spec.boneName);
        class_243 orientedOffset = applyBasis(player, arm, effectiveParticleOffset(spec, arm, "bone"));
        class_243 velocityBase = applyBasis(player, arm, new Vector3f(spec.velocity).mul(eased));
        class_243 spread = new class_243(spec.spread.x, spec.spread.y, spec.spread.z);
        for (int i = 0; i < count; i++) {
            double px = base.field_1352 + orientedOffset.field_1352 + randomSpread(client, spread.field_1352);
            double py = base.field_1351 + orientedOffset.field_1351 + randomSpread(client, spread.field_1351);
            double pz = base.field_1350 + orientedOffset.field_1350 + randomSpread(client, spread.field_1350);
            double vx = velocityBase.field_1352 + randomSpread(client, spread.field_1352 * 0.25d);
            double vy = velocityBase.field_1351 + randomSpread(client, spread.field_1351 * 0.25d);
            double vz = velocityBase.field_1350 + randomSpread(client, spread.field_1350 * 0.25d);
            client.field_1687.method_8406(options, px, py, pz, vx, vy, vz);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void spawnEventFromRender(class_310 client, class_746 player, class_1306 arm, Map<String, Matrix4f> renderBoneTransforms, PendingParticleEvent pending) throws MatchException {
        class_243 class_243VarNormalizeAxis;
        ParticleScriptSpec spec = pending.spec;
        if (spec == null) {
            return;
        }
        float eased = applyEase(spec.ease, spec.t) * class_3532.method_15363(pending.scale, 0.0f, 1.0f);
        int count = class_3532.method_15340(Math.round(spec.count * eased), 0, MAX_PARTICLES_PER_EVENT);
        if (count <= 0) {
            return;
        }
        if (spec.chance < 1.0f) {
            float roll = client.field_1687.field_9229.method_43057();
            if (roll > spec.chance) {
                return;
            }
        }
        if (isWorldBlockFaceTarget(spec)) {
            spawnEventFromBlockFace(client, player, arm, pending, eased, count);
            return;
        }
        String bone = resolveRenderBoneName(renderBoneTransforms, spec.boneName, arm);
        Matrix4f boneTransform = bone == null ? null : renderBoneTransforms.get(bone);
        Matrix4f rootInv = RENDER_ROOT_INV.get(arm);
        Matrix4f localBoneTransform = toLocalBoneTransform(boneTransform, rootInv);
        class_243 handFallback = resolveHandFallback(client, player, arm);
        class_243 base = resolveRenderBoneBase(client, player, handFallback, boneTransform, localBoneTransform, arm, spec.projectionMode);
        if (base == null) {
            return;
        }
        if (!isFinite(base)) {
            base = handFallback;
        } else if (handFallback != null && base.method_1022(handFallback) > 8.0d) {
            base = handFallback;
        }
        Vector3f renderBoneOffset = effectiveParticleOffset(spec, arm, spec.offsetSpace);
        class_243 orientedOffset = applyRenderBasis(client, boneTransform, localBoneTransform, renderBoneOffset, player, arm, spec.offsetSpace, false, handFallback, spec.projectionMode, spec.ignoreModelRotationForEmissionPoint);
        class_243 velocityBase = applyRenderBasis(client, boneTransform, localBoneTransform, new Vector3f(spec.velocity).mul(eased), player, arm, spec.velocitySpace, true, handFallback, spec.projectionMode, false);
        if (orientedOffset == null || !isFinite(orientedOffset)) {
            orientedOffset = class_243.field_1353;
        }
        if (velocityBase == null || !isFinite(velocityBase)) {
            velocityBase = class_243.field_1353;
        }
        class_243 spread = new class_243(spec.spread.x, spec.spread.y, spec.spread.z);
        resolveParticle(spec.typeId);
        class_2960 texture = resolveCustomParticleTexture(spec);
        Vector3f tint = resolveCustomParticleTint(spec);
        int totalTicks = spec.lockInModel ? Integer.MAX_VALUE : Math.max(1, Math.round(spec.lifetimeSeconds * 20.0f));
        float sizeEnd = spec.lockInModel ? spec.sizeStart : spec.sizeEnd;
        float alphaEnd = spec.lockInModel ? spec.alphaStart : spec.alphaEnd;
        float drag = spec.lockInModel ? 1.0f : spec.drag;
        float gravity = spec.lockInModel ? 0.0f : spec.gravity;
        String motionMode = spec.lockInModel ? "linear" : parseMotionMode(spec.motionMode, "linear");
        boolean orbit = !spec.lockInModel && "orbit".equals(motionMode);
        float orbitRadiusWorld = Math.max(0.0f, spec.orbitRadius);
        float orbitRadiusPixels = orbitRadiusWorld * 16.0f;
        float orbitAngularVelocity = (spec.orbitSpeed * TWO_PI) / 20.0f;
        float orbitPhaseBase = (float) Math.toRadians(spec.orbitPhaseDegrees);
        List<RenderParticle> active = (List) ACTIVE_RENDER_PARTICLES.computeIfAbsent(arm, key -> {
            return new ArrayList();
        });
        boolean localSimulation = localBoneTransform != null;
        if (spec.lockInModel && !localSimulation) {
            return;
        }
        Vector3f localBasePixels = localSimulation ? localBoneTransform.getTranslation(new Vector3f()) : null;
        Vector3f localOffsetPixels = null;
        Vector3f localVelocityPixels = null;
        Vector3f localSpreadPixels = null;
        Vector3f localCenterOffsetPixels = null;
        Vector3f localOrbitAxis = null;
        class_243 worldCenterOffset = null;
        class_243 worldOrbitAxis = null;
        if (localSimulation) {
            localOffsetPixels = applyRenderBasisLocal(localBoneTransform, new Vector3f(renderBoneOffset).mul(16.0f), spec.offsetSpace, false, spec.ignoreModelRotationForEmissionPoint);
            localVelocityPixels = applyRenderBasisLocal(localBoneTransform, new Vector3f(spec.velocity).mul(eased * 16.0f), spec.velocitySpace, true, false);
            localSpreadPixels = new Vector3f(spec.spread).mul(16.0f);
            localCenterOffsetPixels = applyRenderBasisLocal(localBoneTransform, new Vector3f(spec.orbitCenterOffset).mul(16.0f), spec.motionSpace, false, spec.ignoreModelRotationForEmissionPoint);
            localOrbitAxis = applyRenderBasisLocal(localBoneTransform, new Vector3f(spec.orbitAxis), spec.motionSpace, false, false);
        } else {
            worldCenterOffset = applyRenderBasis(client, boneTransform, localBoneTransform, spec.orbitCenterOffset, player, arm, spec.motionSpace, false, handFallback, spec.projectionMode, spec.ignoreModelRotationForEmissionPoint);
            worldOrbitAxis = applyRenderBasis(client, boneTransform, localBoneTransform, spec.orbitAxis, player, arm, spec.motionSpace, false, handFallback, spec.projectionMode, false);
        }
        if (worldCenterOffset == null || !isFinite(worldCenterOffset)) {
            worldCenterOffset = class_243.field_1353;
        }
        if (worldOrbitAxis == null || !isFinite(worldOrbitAxis)) {
            worldOrbitAxis = new class_243(0.0d, 1.0d, 0.0d);
        }
        class_243 normalizedWorldOrbitAxis = normalizeAxis(worldOrbitAxis);
        if (localOrbitAxis == null) {
            class_243VarNormalizeAxis = new class_243(0.0d, 1.0d, 0.0d);
        } else {
            class_243VarNormalizeAxis = normalizeAxis(new class_243(localOrbitAxis.x, localOrbitAxis.y, localOrbitAxis.z));
        }
        class_243 normalizedLocalOrbitAxis = class_243VarNormalizeAxis;
        String lockKeyBase = spec.lockInModel ? spec.lockStableKey : null;
        for (int i = 0; i < count; i++) {
            String lockKey = lockKeyBase == null ? null : lockKeyBase + ":" + i;
            float particleOrbitPhase = orbitPhaseBase + (count > 1 ? TWO_PI * (i / count) : 0.0f);
            if (localSimulation) {
                double cx = ((double) (localBasePixels.x + localOffsetPixels.x + localCenterOffsetPixels.x)) + randomSpread(client, localSpreadPixels.x);
                double cy = ((double) (localBasePixels.y + localOffsetPixels.y + localCenterOffsetPixels.y)) + randomSpread(client, localSpreadPixels.y);
                double cz = ((double) (localBasePixels.z + localOffsetPixels.z + localCenterOffsetPixels.z)) + randomSpread(client, localSpreadPixels.z);
                class_243 center = new class_243(cx, cy, cz);
                class_243 orbitOffset = orbit ? orbitOffset(normalizedLocalOrbitAxis, orbitRadiusPixels, particleOrbitPhase) : class_243.field_1353;
                class_243 spawn = center.method_1019(orbitOffset);
                double vx = spec.lockInModel ? 0.0d : ((double) localVelocityPixels.x) + randomSpread(client, ((double) localSpreadPixels.x) * 0.25d);
                double vy = spec.lockInModel ? 0.0d : ((double) localVelocityPixels.y) + randomSpread(client, ((double) localSpreadPixels.y) * 0.25d);
                double vz = spec.lockInModel ? 0.0d : ((double) localVelocityPixels.z) + randomSpread(client, ((double) localSpreadPixels.z) * 0.25d);
                RenderParticle particle = new RenderParticle(spawn, new class_243(vx, vy, vz), totalTicks, spec.ease, spec.sizeStart, sizeEnd, spec.alphaStart, alphaEnd, drag, gravity, spec.fullBright, spec.fullDayLight, spec.brightAtDark, spec.glowTargetMenu, spec.lightPercent, spec.emissiveStrength, spec.emissiveBlur, spec.emissiveClamp, texture, tint.x, tint.y, tint.z, spec.debug, spec.lockInModel, lockKey, true, motionMode, center, normalizedLocalOrbitAxis, orbitRadiusPixels, particleOrbitPhase, orbitAngularVelocity);
                addInHandPipelineParticle(active, particle, false, BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, spec.worldPreviewBehindLocal, spec.worldPreviewBehindLocalBias, spec.ignoreModelRotationForEmissionPoint, spec.syntheticWorldLocal);
            } else {
                double cx2 = base.field_1352 + orientedOffset.field_1352 + worldCenterOffset.field_1352 + randomSpread(client, spread.field_1352);
                double cy2 = base.field_1351 + orientedOffset.field_1351 + worldCenterOffset.field_1351 + randomSpread(client, spread.field_1351);
                double cz2 = base.field_1350 + orientedOffset.field_1350 + worldCenterOffset.field_1350 + randomSpread(client, spread.field_1350);
                class_243 center2 = new class_243(cx2, cy2, cz2);
                class_243 orbitOffset2 = orbit ? orbitOffset(normalizedWorldOrbitAxis, orbitRadiusWorld, particleOrbitPhase) : class_243.field_1353;
                class_243 spawn2 = center2.method_1019(orbitOffset2);
                double vx2 = spec.lockInModel ? 0.0d : velocityBase.field_1352 + randomSpread(client, spread.field_1352 * 0.25d);
                double vy2 = spec.lockInModel ? 0.0d : velocityBase.field_1351 + randomSpread(client, spread.field_1351 * 0.25d);
                double vz2 = spec.lockInModel ? 0.0d : velocityBase.field_1350 + randomSpread(client, spread.field_1350 * 0.25d);
                RenderParticle particle2 = new RenderParticle(spawn2, new class_243(vx2, vy2, vz2), totalTicks, spec.ease, spec.sizeStart, sizeEnd, spec.alphaStart, alphaEnd, drag, gravity, spec.fullBright, spec.fullDayLight, spec.brightAtDark, spec.glowTargetMenu, spec.lightPercent, spec.emissiveStrength, spec.emissiveBlur, spec.emissiveClamp, texture, tint.x, tint.y, tint.z, false, spec.lockInModel, lockKey, false, motionMode, center2, normalizedWorldOrbitAxis, orbitRadiusWorld, particleOrbitPhase, orbitAngularVelocity);
                addInHandPipelineParticle(active, particle2, false, BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, spec.worldPreviewBehindLocal, spec.worldPreviewBehindLocalBias, spec.ignoreModelRotationForEmissionPoint, spec.syntheticWorldLocal);
            }
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void spawnEventForItemRender(class_310 client, class_746 player, class_1306 arm, Matrix4f itemRenderMatrix, PendingParticleEvent pending) throws MatchException {
        ParticleScriptSpec spec = pending.spec;
        if (spec == null || client == null || arm == null || itemRenderMatrix == null) {
            return;
        }
        float eased = applyEase(spec.ease, spec.t) * class_3532.method_15363(pending.scale, 0.0f, 1.0f);
        int count = class_3532.method_15340(Math.round(spec.count * eased), 0, MAX_PARTICLES_PER_EVENT);
        if (count <= 0) {
            return;
        }
        if (spec.chance < 1.0f && client.field_1687 != null && client.field_1687.field_9229.method_43057() > spec.chance) {
            return;
        }
        if (isWorldBlockFaceTarget(spec)) {
            spawnEventFromBlockFace(client, player, arm, pending, eased, count);
            return;
        }
        if (isHeldBlockFaceTarget(spec)) {
            spawnEventForHeldItemBlockFace(client, player, arm, itemRenderMatrix, pending, eased, count);
            return;
        }
        class_243 localBase = resolveItemTargetLocalBase(spec);
        if (localBase == null) {
            localBase = class_243.field_1353;
        }
        Vector3f localOffset = resolveItemLocalVector(effectiveParticleOffset(spec, arm, spec.offsetSpace), spec.offsetSpace, itemRenderMatrix, player, arm, false, spec.ignoreModelRotationForEmissionPoint);
        Vector3f localVelocity = resolveItemLocalVector(new Vector3f(spec.velocity).mul(eased), spec.velocitySpace, itemRenderMatrix, player, arm, true, false);
        Vector3f localSpread = new Vector3f(spec.spread);
        class_2960 texture = resolveCustomParticleTexture(spec);
        Vector3f tint = resolveCustomParticleTint(spec);
        int totalTicks = spec.lockInModel ? Integer.MAX_VALUE : Math.max(1, Math.round(spec.lifetimeSeconds * 20.0f));
        float sizeEnd = spec.lockInModel ? spec.sizeStart : spec.sizeEnd;
        float alphaEnd = spec.lockInModel ? spec.alphaStart : spec.alphaEnd;
        float drag = spec.lockInModel ? 1.0f : spec.drag;
        float gravity = spec.lockInModel ? 0.0f : spec.gravity;
        String motionMode = spec.lockInModel ? "linear" : parseMotionMode(spec.motionMode, "linear");
        boolean orbit = !spec.lockInModel && "orbit".equals(motionMode);
        float orbitRadius = Math.max(0.0f, spec.orbitRadius);
        float orbitAngularVelocity = (spec.orbitSpeed * TWO_PI) / 20.0f;
        float orbitPhaseBase = (float) Math.toRadians(spec.orbitPhaseDegrees);
        Vector3f localCenterOffset = resolveItemLocalVector(spec.orbitCenterOffset, spec.motionSpace, itemRenderMatrix, player, arm, false, spec.ignoreModelRotationForEmissionPoint);
        Vector3f localOrbitAxisVec = resolveItemLocalVector(spec.orbitAxis, spec.motionSpace, itemRenderMatrix, player, arm, false, false);
        class_243 localOrbitAxis = normalizeAxis(new class_243(localOrbitAxisVec.x, localOrbitAxisVec.y, localOrbitAxisVec.z));
        List<RenderParticle> active = (List) ACTIVE_RENDER_PARTICLES.computeIfAbsent(arm, key -> {
            return new ArrayList();
        });
        String lockKeyBase = spec.lockInModel ? spec.lockStableKey : null;
        for (int i = 0; i < count; i++) {
            String lockKey = lockKeyBase == null ? null : lockKeyBase + ":" + i;
            float particleOrbitPhase = orbitPhaseBase + (count > 1 ? TWO_PI * (i / count) : 0.0f);
            double cx = localBase.field_1352 + ((double) localOffset.x) + ((double) localCenterOffset.x) + randomSpread(client, localSpread.x);
            double cy = localBase.field_1351 + ((double) localOffset.y) + ((double) localCenterOffset.y) + randomSpread(client, localSpread.y);
            double cz = localBase.field_1350 + ((double) localOffset.z) + ((double) localCenterOffset.z) + randomSpread(client, localSpread.z);
            class_243 center = new class_243(cx, cy, cz);
            class_243 orbitOffset = orbit ? orbitOffset(localOrbitAxis, orbitRadius, particleOrbitPhase) : class_243.field_1353;
            class_243 spawn = center.method_1019(orbitOffset);
            double vx = spec.lockInModel ? 0.0d : ((double) localVelocity.x) + randomSpread(client, ((double) localSpread.x) * 0.25d);
            double vy = spec.lockInModel ? 0.0d : ((double) localVelocity.y) + randomSpread(client, ((double) localSpread.y) * 0.25d);
            double vz = spec.lockInModel ? 0.0d : ((double) localVelocity.z) + randomSpread(client, ((double) localSpread.z) * 0.25d);
            RenderParticle particle = new RenderParticle(spawn, new class_243(vx, vy, vz), totalTicks, spec.ease, spec.sizeStart, sizeEnd, spec.alphaStart, alphaEnd, drag, gravity, spec.fullBright, spec.fullDayLight, spec.brightAtDark, spec.glowTargetMenu, spec.lightPercent, spec.emissiveStrength, spec.emissiveBlur, spec.emissiveClamp, texture, tint.x, tint.y, tint.z, spec.debug, spec.lockInModel, lockKey, true, motionMode, center, localOrbitAxis, orbitRadius, particleOrbitPhase, orbitAngularVelocity);
            addInHandPipelineParticle(active, particle, false, BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, spec.worldPreviewBehindLocal, spec.worldPreviewBehindLocalBias, spec.ignoreModelRotationForEmissionPoint, spec.syntheticWorldLocal);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void spawnEventFromBlockFace(class_310 client, class_746 player, class_1306 arm, PendingParticleEvent pending, float eased, int count) throws MatchException {
        double dMethod_16436;
        double dMethod_164362;
        ParticleScriptSpec spec = pending.spec;
        if (spec == null || client == null || client.field_1687 == null || arm == null) {
            return;
        }
        MiningActionTracker.Target target = resolveMiningTarget(client, spec);
        if (!matchesMiningTarget(spec, target) || target == null || target.pos() == null) {
            return;
        }
        class_2350 face = resolveBlockFaceSelector(spec.blockFaceSelector, target.face());
        FaceFrame frame = createFaceFrame(target.pos(), face);
        if (frame == null) {
            return;
        }
        class_243 baseOffset = resolveBlockFaceVector(effectiveParticleOffset(spec, arm, spec.offsetSpace), spec.offsetSpace, frame, player, arm);
        class_243 velocityBase = resolveBlockFaceVector(new Vector3f(spec.velocity).mul(eased), spec.velocitySpace, frame, player, arm);
        class_243 centerOffset = resolveBlockFaceVector(spec.orbitCenterOffset, spec.motionSpace, frame, player, arm);
        class_243 orbitAxis = resolveBlockFaceVector(spec.orbitAxis, spec.motionSpace, frame, player, arm);
        if (baseOffset == null || !isFinite(baseOffset)) {
            baseOffset = class_243.field_1353;
        }
        if (velocityBase == null || !isFinite(velocityBase)) {
            velocityBase = class_243.field_1353;
        }
        if (centerOffset == null || !isFinite(centerOffset)) {
            centerOffset = class_243.field_1353;
        }
        if (orbitAxis == null || !isFinite(orbitAxis)) {
            orbitAxis = new class_243(0.0d, 1.0d, 0.0d);
        }
        class_2960 texture = resolveCustomParticleTexture(spec);
        Vector3f tint = resolveCustomParticleTint(spec);
        int totalTicks = Math.max(1, Math.round(spec.lifetimeSeconds * 20.0f));
        float sizeEnd = spec.sizeEnd;
        float alphaEnd = spec.alphaEnd;
        float drag = spec.drag;
        float gravity = spec.gravity;
        String motionMode = parseMotionMode(spec.motionMode, "linear");
        boolean orbit = "orbit".equals(motionMode);
        float orbitRadiusWorld = Math.max(0.0f, spec.orbitRadius);
        float orbitAngularVelocity = (spec.orbitSpeed * TWO_PI) / 20.0f;
        float orbitPhaseBase = (float) Math.toRadians(spec.orbitPhaseDegrees);
        class_243 normalizedOrbitAxis = normalizeAxis(orbitAxis);
        List<RenderParticle> active = (List) ACTIVE_RENDER_PARTICLES.computeIfAbsent(arm, key -> {
            return new ArrayList();
        });
        FaceUv impactUv = resolveMiningImpactFaceUv(target);
        for (int i = 0; i < count; i++) {
            if (impactUv != null && shouldUseImpactFacePoint(spec)) {
                dMethod_16436 = impactUv.u();
            } else {
                dMethod_16436 = class_3532.method_16436(client.field_1687.field_9229.method_43058(), spec.faceMinU, spec.faceMaxU);
            }
            double u = dMethod_16436;
            if (impactUv != null && shouldUseImpactFacePoint(spec)) {
                dMethod_164362 = impactUv.v();
            } else {
                dMethod_164362 = class_3532.method_16436(client.field_1687.field_9229.method_43058(), spec.faceMinV, spec.faceMaxV);
            }
            double v = dMethod_164362;
            class_243 facePoint = frame.corner().method_1019(frame.axisU().method_1021(u)).method_1019(frame.axisV().method_1021(v)).method_1019(frame.normal().method_1021(BLOCK_FACE_SURFACE_EPSILON));
            class_243 positionJitter = randomBlockFaceJitter(client, spec.spread, frame, 1.0d);
            class_243 center = facePoint.method_1019(baseOffset).method_1019(centerOffset).method_1019(positionJitter);
            float particleOrbitPhase = orbitPhaseBase + (count > 1 ? TWO_PI * (i / count) : 0.0f);
            class_243 orbitOffset = orbit ? orbitOffset(normalizedOrbitAxis, orbitRadiusWorld, particleOrbitPhase) : class_243.field_1353;
            class_243 spawn = center.method_1019(orbitOffset);
            class_243 velocityJitter = randomBlockFaceJitter(client, spec.spread, frame, 0.25d);
            RenderParticle particle = new RenderParticle(spawn, new class_243(velocityBase.field_1352 + velocityJitter.field_1352, velocityBase.field_1351 + velocityJitter.field_1351, velocityBase.field_1350 + velocityJitter.field_1350), totalTicks, spec.ease, spec.sizeStart, sizeEnd, spec.alphaStart, alphaEnd, drag, gravity, spec.fullBright, spec.fullDayLight, spec.brightAtDark, spec.glowTargetMenu, spec.lightPercent, spec.emissiveStrength, spec.emissiveBlur, spec.emissiveClamp, texture, tint.x, tint.y, tint.z, false, false, null, false, motionMode, center, normalizedOrbitAxis, orbitRadiusWorld, particleOrbitPhase, orbitAngularVelocity);
            addInHandPipelineParticle(active, particle, false, BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, spec.worldPreviewBehindLocal, spec.worldPreviewBehindLocalBias, false, false);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void spawnEventForHeldItemBlockFace(class_310 client, class_746 player, class_1306 arm, Matrix4f itemRenderMatrix, PendingParticleEvent pending, float eased, int count) throws MatchException {
        double dMethod_16436;
        double dMethod_164362;
        ParticleScriptSpec spec = pending.spec;
        if (spec == null || client == null || arm == null || itemRenderMatrix == null) {
            return;
        }
        MiningActionTracker.Target target = resolveMiningTarget(client, spec);
        if (!matchesMiningTarget(spec, target)) {
            return;
        }
        class_2350 face = resolveBlockFaceSelector(spec.blockFaceSelector, target == null ? null : target.face());
        FaceFrame frame = createUnitCubeFaceFrame(face);
        if (frame == null) {
            return;
        }
        Vector3f localOffset = resolveHeldItemBlockFaceVector(effectiveParticleOffset(spec, arm, spec.offsetSpace), spec.offsetSpace, frame, itemRenderMatrix, player, arm, false);
        Vector3f localVelocity = resolveHeldItemBlockFaceVector(new Vector3f(spec.velocity).mul(eased), spec.velocitySpace, frame, itemRenderMatrix, player, arm, true);
        Vector3f localCenterOffset = resolveHeldItemBlockFaceVector(spec.orbitCenterOffset, spec.motionSpace, frame, itemRenderMatrix, player, arm, false);
        Vector3f localOrbitAxisVec = resolveHeldItemBlockFaceVector(spec.orbitAxis, spec.motionSpace, frame, itemRenderMatrix, player, arm, false);
        if (localOffset == null) {
            localOffset = new Vector3f();
        }
        if (localVelocity == null) {
            localVelocity = new Vector3f();
        }
        if (localCenterOffset == null) {
            localCenterOffset = new Vector3f();
        }
        if (localOrbitAxisVec == null) {
            localOrbitAxisVec = new Vector3f(0.0f, 1.0f, 0.0f);
        }
        class_2960 texture = resolveCustomParticleTexture(spec);
        Vector3f tint = resolveCustomParticleTint(spec);
        int totalTicks = spec.lockInModel ? Integer.MAX_VALUE : Math.max(1, Math.round(spec.lifetimeSeconds * 20.0f));
        float sizeEnd = spec.lockInModel ? spec.sizeStart : spec.sizeEnd;
        float alphaEnd = spec.lockInModel ? spec.alphaStart : spec.alphaEnd;
        float drag = spec.lockInModel ? 1.0f : spec.drag;
        float gravity = spec.lockInModel ? 0.0f : spec.gravity;
        String motionMode = spec.lockInModel ? "linear" : parseMotionMode(spec.motionMode, "linear");
        boolean orbit = !spec.lockInModel && "orbit".equals(motionMode);
        float orbitRadius = Math.max(0.0f, spec.orbitRadius);
        float orbitAngularVelocity = (spec.orbitSpeed * TWO_PI) / 20.0f;
        float orbitPhaseBase = (float) Math.toRadians(spec.orbitPhaseDegrees);
        class_243 localOrbitAxis = normalizeAxis(new class_243(localOrbitAxisVec.x, localOrbitAxisVec.y, localOrbitAxisVec.z));
        List<RenderParticle> active = (List) ACTIVE_RENDER_PARTICLES.computeIfAbsent(arm, key -> {
            return new ArrayList();
        });
        String lockKeyBase = spec.lockInModel ? spec.lockStableKey : null;
        FaceUv impactUv = resolveMiningImpactFaceUv(target);
        for (int i = 0; i < count; i++) {
            String lockKey = lockKeyBase == null ? null : lockKeyBase + ":" + i;
            if (impactUv != null && shouldUseImpactFacePoint(spec)) {
                dMethod_16436 = impactUv.u();
            } else {
                dMethod_16436 = class_3532.method_16436(client.field_1687.field_9229.method_43058(), spec.faceMinU, spec.faceMaxU);
            }
            double u = dMethod_16436;
            if (impactUv != null && shouldUseImpactFacePoint(spec)) {
                dMethod_164362 = impactUv.v();
            } else {
                dMethod_164362 = class_3532.method_16436(client.field_1687.field_9229.method_43058(), spec.faceMinV, spec.faceMaxV);
            }
            double v = dMethod_164362;
            class_243 facePoint = frame.corner().method_1019(frame.axisU().method_1021(u)).method_1019(frame.axisV().method_1021(v)).method_1019(frame.normal().method_1021(BLOCK_FACE_SURFACE_EPSILON));
            Vector3f spreadJitter = randomHeldItemBlockFaceJitter(client, spec.spread, frame, 1.0f);
            double cx = facePoint.field_1352 + ((double) localOffset.x) + ((double) localCenterOffset.x) + ((double) spreadJitter.x);
            double cy = facePoint.field_1351 + ((double) localOffset.y) + ((double) localCenterOffset.y) + ((double) spreadJitter.y);
            double cz = facePoint.field_1350 + ((double) localOffset.z) + ((double) localCenterOffset.z) + ((double) spreadJitter.z);
            class_243 center = new class_243(cx, cy, cz);
            float particleOrbitPhase = orbitPhaseBase + (count > 1 ? TWO_PI * (i / count) : 0.0f);
            class_243 orbitOffset = orbit ? orbitOffset(localOrbitAxis, orbitRadius, particleOrbitPhase) : class_243.field_1353;
            class_243 spawn = center.method_1019(orbitOffset);
            Vector3f velocityJitter = randomHeldItemBlockFaceJitter(client, spec.spread, frame, 0.25f);
            RenderParticle particle = new RenderParticle(spawn, new class_243(spec.lockInModel ? 0.0d : localVelocity.x + velocityJitter.x, spec.lockInModel ? 0.0d : localVelocity.y + velocityJitter.y, spec.lockInModel ? 0.0d : localVelocity.z + velocityJitter.z), totalTicks, spec.ease, spec.sizeStart, sizeEnd, spec.alphaStart, alphaEnd, drag, gravity, spec.fullBright, spec.fullDayLight, spec.brightAtDark, spec.glowTargetMenu, spec.lightPercent, spec.emissiveStrength, spec.emissiveBlur, spec.emissiveClamp, texture, tint.x, tint.y, tint.z, spec.debug, spec.lockInModel, lockKey, true, motionMode, center, localOrbitAxis, orbitRadius, particleOrbitPhase, orbitAngularVelocity);
            addInHandPipelineParticle(active, particle, false, BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, spec.worldPreviewBehindLocal, spec.worldPreviewBehindLocalBias, spec.ignoreModelRotationForEmissionPoint, spec.syntheticWorldLocal);
        }
    }

    private static void addInHandPipelineParticle(List<RenderParticle> active, RenderParticle particle, boolean delayedWorldRelease, long releaseDelayNanos, boolean worldPreviewBehindLocal, float worldPreviewBehindLocalBias, boolean ignoreItemRotationForLayer, boolean syntheticWorldLocal) {
        if (active == null || particle == null) {
            return;
        }
        particle.renderInWorldPass = !particle.localSpace;
        if (particle.localSpace && syntheticWorldLocal) {
            particle.enableSyntheticWorldLocal();
        }
        if (particle.lockInModel) {
            particle.localSpace = true;
            particle.renderInWorldPass = false;
            particle.pendingWorldRelease = false;
            particle.pendingReleaseSnapshotReady = false;
            particle.worldPreviewBehindLocal = false;
        }
        if (particle.lockInModel && particle.lockKey != null && !particle.lockKey.isBlank()) {
            active.removeIf(existing -> {
                return existing != null && existing.lockInModel && particle.lockKey.equals(existing.lockKey);
            });
        }
        active.add(particle);
    }

    private static class_243 resolveItemTargetLocalBase(ParticleScriptSpec spec) {
        if (spec == null) {
            return class_243.field_1353;
        }
        String targetType = normalizeTargetType(spec.targetType, "item_center");
        if ("item_center".equals(targetType)) {
            return new class_243(0.5d, 0.5d, 0.5d);
        }
        if ("bone".equals(targetType)) {
            return new class_243(0.5d, 0.5d, 0.5d);
        }
        return new class_243(0.5d, 0.5d, 0.5d);
    }

    private static Vector3f resolveHeldItemBlockFaceVector(Vector3f source, String space, FaceFrame frame, Matrix4f itemRenderMatrix, class_746 player, class_1306 arm, boolean directionOnly) {
        Vector3f value = source == null ? new Vector3f() : new Vector3f(source);
        String resolved = normalizeSpace(space, directionOnly);
        if ("block_face".equals(resolved)) {
            class_243 transformed = frame.axisU().method_1021(value.x).method_1019(frame.axisV().method_1021(value.y)).method_1019(frame.normal().method_1021(value.z));
            return new Vector3f((float) transformed.field_1352, (float) transformed.field_1351, (float) transformed.field_1350);
        }
        if ("block".equals(resolved) || "bone".equals(resolved)) {
            return value;
        }
        return resolveItemLocalVector(value, resolved, itemRenderMatrix, player, arm, directionOnly, false);
    }

    private static Vector3f randomHeldItemBlockFaceJitter(class_310 client, Vector3f spread, FaceFrame frame, float scale) {
        if (client == null || spread == null || frame == null || scale <= 0.0f) {
            return new Vector3f();
        }
        return resolveHeldItemBlockFaceVector(new Vector3f((float) randomSpread(client, spread.x * scale), (float) randomSpread(client, spread.y * scale), (float) randomSpread(client, spread.z * scale)), "block_face", frame, null, null, null, false);
    }

    private static Vector3f resolveItemLocalVector(Vector3f source, String space, Matrix4f itemRenderMatrix, class_746 player, class_1306 arm, boolean directionOnly, boolean ignoreBoneRotation) {
        Vector3f value = source == null ? new Vector3f() : new Vector3f(source);
        String normalized = normalizeSpace(space, directionOnly);
        if (arm == class_1306.field_6182 && !"world".equals(normalized) && !"camera".equals(normalized)) {
            value.x = -value.x;
        }
        if ("bone".equals(normalized) || ignoreBoneRotation) {
            return value;
        }
        class_243 worldOrCamera = applyBasis(player, arm, value);
        Matrix4f inv = new Matrix4f(itemRenderMatrix).invert();
        Vector4f v4 = new Vector4f((float) worldOrCamera.field_1352, (float) worldOrCamera.field_1351, (float) worldOrCamera.field_1350, 0.0f);
        inv.transform(v4);
        return new Vector3f(v4.x, v4.y, v4.z);
    }

    private static boolean isHeldBlockFaceTarget(ParticleScriptSpec spec) {
        return spec != null && "block_face".equals(normalizeTargetType(spec.targetType, spec.targetType));
    }

    private static boolean isWorldBlockFaceTarget(ParticleScriptSpec spec) {
        return spec != null && "world_block_face".equals(normalizeTargetType(spec.targetType, spec.targetType));
    }

    private static boolean isAnyBlockFaceTarget(ParticleScriptSpec spec) {
        return isHeldBlockFaceTarget(spec) || isWorldBlockFaceTarget(spec);
    }

    private static boolean shouldUseImpactFacePoint(ParticleScriptSpec spec) {
        if (spec == null) {
            return false;
        }
        if (spec.useImpactFacePoint) {
            return true;
        }
        return spec.faceMinU <= EPS && spec.faceMinV <= EPS && spec.faceMaxU >= 0.9999f && spec.faceMaxV >= 0.9999f;
    }

    private static class_243 resolveBlockFaceVector(Vector3f value, String space, FaceFrame frame, class_746 player, class_1306 arm) {
        if (value == null) {
            return class_243.field_1353;
        }
        String resolved = normalizeSpace(space, false);
        if ("world".equals(resolved)) {
            return new class_243(value.x, value.y, value.z);
        }
        if ("camera".equals(resolved)) {
            class_243 viaCamera = applyBasis(player, arm, value);
            return viaCamera == null ? class_243.field_1353 : viaCamera;
        }
        if ("block".equals(resolved)) {
            return new class_243(value.x, value.y, value.z);
        }
        if (frame == null) {
            return class_243.field_1353;
        }
        return frame.axisU().method_1021(value.x).method_1019(frame.axisV().method_1021(value.y)).method_1019(frame.normal().method_1021(value.z));
    }

    private static class_243 randomBlockFaceJitter(class_310 client, Vector3f spread, FaceFrame frame, double scale) {
        if (client == null || spread == null || frame == null || scale <= 0.0d) {
            return class_243.field_1353;
        }
        Vector3f local = new Vector3f((float) randomSpread(client, ((double) spread.x) * scale), (float) randomSpread(client, ((double) spread.y) * scale), (float) randomSpread(client, ((double) spread.z) * scale));
        return resolveBlockFaceVector(local, "block_face", frame, null, null);
    }

    private static FaceUv resolveMiningImpactFaceUv(MiningActionTracker.Target target) {
        FaceFrame hitFrame;
        if (target == null || target.pos() == null || target.face() == null || target.hitLocation() == null || (hitFrame = createFaceFrame(target.pos(), target.face())) == null) {
            return null;
        }
        class_243 relative = target.hitLocation().method_1020(hitFrame.corner());
        double u = class_3532.method_15350(relative.method_1026(hitFrame.axisU()), 0.0d, 1.0d);
        double v = class_3532.method_15350(relative.method_1026(hitFrame.axisV()), 0.0d, 1.0d);
        return new FaceUv(u, v);
    }

    private static class_2350 resolveBlockFaceSelector(String selector, class_2350 hitFace) {
        String normalized = normalizeBlockFaceSelector(selector, "hit");
        switch (normalized) {
            case "down":
                return class_2350.field_11033;
            case "up":
                return class_2350.field_11036;
            case "north":
                return class_2350.field_11043;
            case "south":
                return class_2350.field_11035;
            case "west":
                return class_2350.field_11039;
            case "east":
                return class_2350.field_11034;
            case "opposite":
                if (hitFace == null) {
                    return null;
                }
                return hitFace.method_10153();
            case "random":
                return class_2350.method_10162(class_5819.method_43047());
            case "hit":
                return hitFace;
            default:
                return hitFace;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static FaceFrame createFaceFrame(class_2338 pos, class_2350 face) throws MatchException {
        if (pos == null || face == null) {
            return null;
        }
        class_243 origin = new class_243(pos.method_10263(), pos.method_10264(), pos.method_10260());
        switch (AnonymousClass1.$SwitchMap$net$minecraft$core$Direction[face.ordinal()]) {
            case 1:
                return new FaceFrame(origin, new class_243(1.0d, 0.0d, 0.0d), new class_243(0.0d, 1.0d, 0.0d), new class_243(0.0d, 0.0d, -1.0d));
            case 2:
                return new FaceFrame(origin.method_1031(1.0d, 0.0d, 1.0d), new class_243(-1.0d, 0.0d, 0.0d), new class_243(0.0d, 1.0d, 0.0d), new class_243(0.0d, 0.0d, 1.0d));
            case 3:
                return new FaceFrame(origin.method_1031(0.0d, 0.0d, 1.0d), new class_243(0.0d, 0.0d, -1.0d), new class_243(0.0d, 1.0d, 0.0d), new class_243(-1.0d, 0.0d, 0.0d));
            case 4:
                return new FaceFrame(origin.method_1031(1.0d, 0.0d, 0.0d), new class_243(0.0d, 0.0d, 1.0d), new class_243(0.0d, 1.0d, 0.0d), new class_243(1.0d, 0.0d, 0.0d));
            case 5:
                return new FaceFrame(origin.method_1031(0.0d, 0.0d, 1.0d), new class_243(1.0d, 0.0d, 0.0d), new class_243(0.0d, 0.0d, -1.0d), new class_243(0.0d, -1.0d, 0.0d));
            case 6:
                return new FaceFrame(origin.method_1031(0.0d, 1.0d, 0.0d), new class_243(1.0d, 0.0d, 0.0d), new class_243(0.0d, 0.0d, 1.0d), new class_243(0.0d, 1.0d, 0.0d));
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: renamed from: punchy.client.modelparts.ModelPartsParticleSystem$1, reason: invalid class name */
    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$net$minecraft$core$Direction = new int[class_2350.values().length];

        static {
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11043.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11035.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11039.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11034.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11033.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11036.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static FaceFrame createUnitCubeFaceFrame(class_2350 face) throws MatchException {
        if (face == null) {
            return null;
        }
        switch (AnonymousClass1.$SwitchMap$net$minecraft$core$Direction[face.ordinal()]) {
            case 1:
                return new FaceFrame(new class_243(0.0d, 0.0d, 0.0d), new class_243(1.0d, 0.0d, 0.0d), new class_243(0.0d, 1.0d, 0.0d), new class_243(0.0d, 0.0d, -1.0d));
            case 2:
                return new FaceFrame(new class_243(1.0d, 0.0d, 1.0d), new class_243(-1.0d, 0.0d, 0.0d), new class_243(0.0d, 1.0d, 0.0d), new class_243(0.0d, 0.0d, 1.0d));
            case 3:
                return new FaceFrame(new class_243(0.0d, 0.0d, 1.0d), new class_243(0.0d, 0.0d, -1.0d), new class_243(0.0d, 1.0d, 0.0d), new class_243(-1.0d, 0.0d, 0.0d));
            case 4:
                return new FaceFrame(new class_243(1.0d, 0.0d, 0.0d), new class_243(0.0d, 0.0d, 1.0d), new class_243(0.0d, 1.0d, 0.0d), new class_243(1.0d, 0.0d, 0.0d));
            case 5:
                return new FaceFrame(new class_243(0.0d, 0.0d, 1.0d), new class_243(1.0d, 0.0d, 0.0d), new class_243(0.0d, 0.0d, -1.0d), new class_243(0.0d, -1.0d, 0.0d));
            case 6:
                return new FaceFrame(new class_243(0.0d, 1.0d, 0.0d), new class_243(1.0d, 0.0d, 0.0d), new class_243(0.0d, 0.0d, 1.0d), new class_243(0.0d, 1.0d, 0.0d));
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    private static void tickRenderParticles(class_310 client, class_1306 arm) {
        if (client == null || client.field_1687 == null || arm == null) {
            return;
        }
        long nowNanos = System.nanoTime();
        long lastNanos = ((Long) LAST_SIM_NANOS.getOrDefault(arm, Long.valueOf(BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS))).longValue();
        LAST_SIM_NANOS.put(arm, Long.valueOf(nowNanos));
        float dtSeconds = lastNanos <= BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS ? 0.016666668f : (float) ((nowNanos - lastNanos) / 1.0E9d);
        float tickScale = class_3532.method_15363(dtSeconds, 0.0f, 0.1f) * 20.0f;
        Matrix4f currentRoot = RENDER_ROOT.get(arm);
        Matrix4f lastRoot = LAST_SIM_ROOT.get(arm);
        Matrix4f localDelta = null;
        if (currentRoot != null && lastRoot != null) {
            localDelta = new Matrix4f(currentRoot).invert().mul(lastRoot);
        }
        if (currentRoot != null) {
            LAST_SIM_ROOT.put(arm, new Matrix4f(currentRoot));
        }
        class_243 cameraMotionPixels = resolveCameraMotionPixels(client, arm);
        class_243 cameraTurnPixels = resolveCameraTurnPixels(client, arm, tickScale);
        boolean shaderPackInUse = isShaderPackInUse();
        Matrix4f syntheticCameraTurnLocalDelta = null;
        if (shaderPackInUse && currentRoot != null) {
            Matrix4f currentRootInv = RENDER_ROOT_INV.get(arm);
            Quaternionf cameraTurnDeltaCameraLocal = LAST_CAMERA_TURN_DELTA_CAMERA_LOCAL.get(arm);
            if (currentRootInv != null && cameraTurnDeltaCameraLocal != null) {
                Matrix4f cameraDelta = new Matrix4f().rotation(new Quaternionf(cameraTurnDeltaCameraLocal));
                syntheticCameraTurnLocalDelta = new Matrix4f(currentRootInv).mul(cameraDelta).mul(currentRoot);
            }
        }
        List<RenderParticle> particles = ACTIVE_RENDER_PARTICLES.get(arm);
        if (particles == null || particles.isEmpty()) {
            return;
        }
        for (int i = particles.size() - 1; i >= 0; i--) {
            RenderParticle particle = particles.get(i);
            if (particle == null) {
                particles.remove(i);
            } else {
                particle.ageTicks += tickScale;
                if (particle.ageTicks > particle.totalTicks) {
                    particles.remove(i);
                } else if ((!particle.localSpace || !particle.pendingWorldRelease || !particle.pendingReleaseSnapshotReady || !applyPendingWorldReleaseSnapshot(particle)) && ((!particle.localSpace || !particle.pendingWorldRelease) && !particle.lockInModel)) {
                    if (particle.localSpace && localDelta != null) {
                        if (particle.ignoreItemRotationForLayer) {
                            Vector3f deltaT = localDelta.getTranslation(new Vector3f());
                            particle.position = particle.position.method_1031(deltaT.x, deltaT.y, deltaT.z);
                        } else {
                            Vector4f p4 = new Vector4f((float) particle.position.field_1352, (float) particle.position.field_1351, (float) particle.position.field_1350, 1.0f);
                            localDelta.transform(p4);
                            particle.position = new class_243(p4.x, p4.y, p4.z);
                            Vector4f v4 = new Vector4f((float) particle.velocity.field_1352, (float) particle.velocity.field_1351, (float) particle.velocity.field_1350, 0.0f);
                            localDelta.transform(v4);
                            particle.velocity = new class_243(v4.x, v4.y, v4.z);
                        }
                        if (particle.orbit()) {
                            if (particle.ignoreItemRotationForLayer) {
                                Vector3f deltaT2 = localDelta.getTranslation(new Vector3f());
                                particle.orbitCenter = particle.orbitCenter.method_1031(deltaT2.x, deltaT2.y, deltaT2.z);
                            } else {
                                Vector4f c4 = new Vector4f((float) particle.orbitCenter.field_1352, (float) particle.orbitCenter.field_1351, (float) particle.orbitCenter.field_1350, 1.0f);
                                localDelta.transform(c4);
                                particle.orbitCenter = new class_243(c4.x, c4.y, c4.z);
                                Vector4f a4 = new Vector4f((float) particle.orbitAxis.field_1352, (float) particle.orbitAxis.field_1351, (float) particle.orbitAxis.field_1350, 0.0f);
                                localDelta.transform(a4);
                                particle.orbitAxis = normalizeAxis(new class_243(a4.x, a4.y, a4.z));
                            }
                        }
                    }
                    if (particle.localSpace && cameraMotionPixels != null && !particle.ignoreItemRotationForLayer) {
                        particle.position = particle.position.method_1020(cameraMotionPixels);
                        if (particle.orbit()) {
                            particle.orbitCenter = particle.orbitCenter.method_1020(cameraMotionPixels);
                        }
                    }
                    if (particle.localSpace && particle.syntheticWorldLocal && syntheticCameraTurnLocalDelta != null && !particle.ignoreItemRotationForLayer) {
                        Vector4f pTurn = new Vector4f((float) particle.position.field_1352, (float) particle.position.field_1351, (float) particle.position.field_1350, 1.0f);
                        syntheticCameraTurnLocalDelta.transform(pTurn);
                        particle.position = new class_243(pTurn.x, pTurn.y, pTurn.z);
                        Vector4f vTurn = new Vector4f((float) particle.velocity.field_1352, (float) particle.velocity.field_1351, (float) particle.velocity.field_1350, 0.0f);
                        syntheticCameraTurnLocalDelta.transform(vTurn);
                        particle.velocity = new class_243(vTurn.x, vTurn.y, vTurn.z);
                        if (particle.orbit()) {
                            Vector4f cTurn = new Vector4f((float) particle.orbitCenter.field_1352, (float) particle.orbitCenter.field_1351, (float) particle.orbitCenter.field_1350, 1.0f);
                            syntheticCameraTurnLocalDelta.transform(cTurn);
                            particle.orbitCenter = new class_243(cTurn.x, cTurn.y, cTurn.z);
                            Vector4f aTurn = new Vector4f((float) particle.orbitAxis.field_1352, (float) particle.orbitAxis.field_1351, (float) particle.orbitAxis.field_1350, 0.0f);
                            syntheticCameraTurnLocalDelta.transform(aTurn);
                            particle.orbitAxis = normalizeAxis(new class_243(aTurn.x, aTurn.y, aTurn.z));
                        }
                    }
                    if (particle.localSpace && cameraTurnPixels != null && !particle.syntheticWorldLocal && !particle.ignoreItemRotationForLayer) {
                        particle.position = particle.position.method_1019(cameraTurnPixels);
                        if (particle.orbit()) {
                            particle.orbitCenter = particle.orbitCenter.method_1019(cameraTurnPixels);
                        }
                    }
                    if (particle.orbit()) {
                        particle.orbitAngle += particle.orbitAngularVelocity * tickScale;
                        class_243 target = particle.orbitCenter.method_1019(orbitOffset(particle.orbitAxis, particle.orbitRadius, particle.orbitAngle));
                        double inv = 1.0d / ((double) Math.max(0.001f, tickScale));
                        particle.velocity = target.method_1020(particle.position).method_1021(inv);
                        particle.position = target;
                    } else {
                        float dragTick = class_3532.method_15363(particle.drag, 0.0f, 1.2f);
                        double dragScale = Math.pow(dragTick, tickScale);
                        particle.velocity = particle.velocity.method_1021(dragScale);
                        particle.velocity = particle.velocity.method_1031(0.0d, (particle.gravity / 20.0f) * tickScale, 0.0d);
                        class_243 motion = particle.velocity.method_1021(tickScale);
                        if (particle.localSpace) {
                            particle.position = particle.position.method_1019(motion);
                        } else {
                            applyWorldCollisionStep(client, particle, motion);
                        }
                    }
                }
            }
        }
    }

    private static void applyWorldCollisionStep(class_310 client, RenderParticle particle, class_243 motion) {
        if (particle == null || motion == null) {
            return;
        }
        class_243 start = particle.position;
        class_243 end = start.method_1019(motion);
        if (client == null || client.field_1687 == null || !isFinite(start) || !isFinite(end) || motion.method_1027() <= 1.0E-8d) {
            particle.position = end;
            return;
        }
        class_3965 class_3965VarMethod_17742 = client.field_1687.method_17742(new class_3959(start, end, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, client.field_1724));
        if (class_3965VarMethod_17742 instanceof class_3965) {
            class_3965 blockHit = class_3965VarMethod_17742;
            if (blockHit.method_17783() == class_239.class_240.field_1332 && blockHit.method_17784() != null) {
                class_2350 face = blockHit.method_17780();
                class_243 normal = new class_243(face.method_10148(), face.method_10164(), face.method_10165());
                particle.position = blockHit.method_17784().method_1019(normal.method_1021(PARTICLE_COLLISION_SURFACE_EPSILON));
                double normalVelocity = particle.velocity.method_1026(normal);
                class_243 adjustedVelocity = particle.velocity;
                if (normalVelocity < 0.0d) {
                    adjustedVelocity = adjustedVelocity.method_1020(normal.method_1021(normalVelocity));
                }
                double friction = face.method_10166().method_10178() ? 0.68d : 0.55d;
                class_243 adjustedVelocity2 = adjustedVelocity.method_1021(friction);
                if (face == class_2350.field_11036 && adjustedVelocity2.field_1351 < PARTICLE_COLLISION_REST_EPSILON) {
                    adjustedVelocity2 = new class_243(adjustedVelocity2.field_1352, 0.0d, adjustedVelocity2.field_1350);
                } else if (face == class_2350.field_11033 && adjustedVelocity2.field_1351 > -0.0035d) {
                    adjustedVelocity2 = new class_243(adjustedVelocity2.field_1352, 0.0d, adjustedVelocity2.field_1350);
                } else if (face.method_10166().method_10179()) {
                    adjustedVelocity2 = new class_243(Math.abs(adjustedVelocity2.field_1352) < PARTICLE_COLLISION_REST_EPSILON ? 0.0d : adjustedVelocity2.field_1352, adjustedVelocity2.field_1351, Math.abs(adjustedVelocity2.field_1350) < PARTICLE_COLLISION_REST_EPSILON ? 0.0d : adjustedVelocity2.field_1350);
                }
                particle.velocity = adjustedVelocity2;
                return;
            }
        }
        particle.position = end;
    }

    private static boolean tryReleaseParticleFromRenderedPose(class_310 client, class_1306 arm, RenderParticle particle, Matrix4f pose, class_243 cameraPos, Quaternionf cameraRot, Vector3f currentRenderPos, boolean shaderPackInUse) {
        class_243 class_243VarResolveHandFallback;
        Vector3f vector3f;
        class_243 worldPos;
        class_243 worldVel;
        class_243 worldAxis;
        class_243 centerDeltaWorld;
        class_243 class_243Var;
        if (particle == null || !particle.localSpace || !particle.pendingWorldRelease || pose == null || cameraPos == null) {
            return false;
        }
        long requiredDelay = shaderPackInUse ? BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS : particle.worldReleaseDelayNanos;
        if (System.nanoTime() - particle.spawnNanos < requiredDelay || currentRenderPos == null || !isFinite(currentRenderPos)) {
            return false;
        }
        if (client != null && client.field_1724 != null && arm != null) {
            class_243VarResolveHandFallback = resolveHandFallback(client, client.field_1724, arm);
        } else {
            class_243VarResolveHandFallback = null;
        }
        class_243 handFallback = class_243VarResolveHandFallback;
        if (particle.ignoreItemRotationForLayer) {
            vector3f = ghostReleaseOffsetForRenderPos(arm, cameraRot, shaderPackInUse);
        } else {
            vector3f = new Vector3f();
        }
        Vector3f ghostReleaseOffset = vector3f;
        Vector3f adjustedRenderPos = new Vector3f(currentRenderPos).add(ghostReleaseOffset);
        if (particle.ignoreItemRotationForLayer) {
            class_243 ghostRootWorld = resolveRenderRootBase(client, arm, handFallback);
            Vector3f ghostRootRender = resolveRenderRootRenderDelta(arm);
            if (ghostRootWorld != null && ghostRootRender != null) {
                Vector3f rel = new Vector3f(adjustedRenderPos).sub(ghostRootRender);
                if (shaderPackInUse) {
                    class_243Var = releaseVectorFromRenderSpace(new Vector4f(rel.x, rel.y, rel.z, 0.0f), cameraRot, true);
                } else {
                    class_243Var = new class_243(rel.x, rel.y, rel.z);
                }
                class_243 relWorld = class_243Var;
                worldPos = ghostRootWorld.method_1019(relWorld);
            } else if (shaderPackInUse) {
                worldPos = cameraSpaceToWorld(client, adjustedRenderPos, arm, handFallback);
                if (worldPos == null || !isFinite(worldPos)) {
                    worldPos = releasePointFromRenderSpace(cameraPos, adjustedRenderPos, cameraRot, true);
                }
            } else {
                worldPos = releasePointFromRenderSpace(cameraPos, adjustedRenderPos, cameraRot, false);
            }
        } else {
            worldPos = releasePointFromRenderSpace(cameraPos, currentRenderPos, cameraRot, shaderPackInUse);
        }
        Vector4f v4 = new Vector4f((float) particle.velocity.field_1352, (float) particle.velocity.field_1351, (float) particle.velocity.field_1350, 0.0f);
        if (!particle.ignoreItemRotationForLayer) {
            pose.transform(v4);
        } else {
            Vector3f scaled = transformVectorIgnoringPoseRotation(pose, new Vector3f(v4.x, v4.y, v4.z));
            v4.set(scaled.x, scaled.y, scaled.z, 0.0f);
        }
        if (particle.ignoreItemRotationForLayer) {
            class_243 converted = cameraSpaceDirectionToWorld(client, new Vector3f(v4.x, v4.y, v4.z), arm, handFallback);
            worldVel = converted != null ? converted : releaseVectorFromRenderSpace(v4, cameraRot, true);
        } else {
            worldVel = releaseVectorFromRenderSpace(v4, cameraRot, shaderPackInUse);
        }
        class_243 worldCenter = worldPos;
        if (particle.orbitCenter != null) {
            Vector4f centerDelta4 = new Vector4f((float) (particle.orbitCenter.field_1352 - particle.position.field_1352), (float) (particle.orbitCenter.field_1351 - particle.position.field_1351), (float) (particle.orbitCenter.field_1350 - particle.position.field_1350), 0.0f);
            if (!particle.ignoreItemRotationForLayer) {
                pose.transform(centerDelta4);
            } else {
                Vector3f scaled2 = transformVectorIgnoringPoseRotation(pose, new Vector3f(centerDelta4.x, centerDelta4.y, centerDelta4.z));
                centerDelta4.set(scaled2.x, scaled2.y, scaled2.z, 0.0f);
            }
            if (particle.ignoreItemRotationForLayer) {
                class_243 converted2 = cameraSpaceDirectionToWorld(client, new Vector3f(centerDelta4.x, centerDelta4.y, centerDelta4.z), arm, handFallback);
                centerDeltaWorld = converted2 != null ? converted2 : releaseVectorFromRenderSpace(centerDelta4, cameraRot, true);
            } else {
                centerDeltaWorld = releaseVectorFromRenderSpace(centerDelta4, cameraRot, shaderPackInUse);
            }
            worldCenter = worldPos.method_1019(centerDeltaWorld);
        }
        class_243 worldAxis2 = particle.orbitAxis == null ? new class_243(0.0d, 1.0d, 0.0d) : particle.orbitAxis;
        Vector4f a4 = new Vector4f((float) worldAxis2.field_1352, (float) worldAxis2.field_1351, (float) worldAxis2.field_1350, 0.0f);
        if (!particle.ignoreItemRotationForLayer) {
            pose.transform(a4);
        } else {
            Vector3f scaled3 = transformVectorIgnoringPoseRotation(pose, new Vector3f(a4.x, a4.y, a4.z));
            a4.set(scaled3.x, scaled3.y, scaled3.z, 0.0f);
        }
        if (particle.ignoreItemRotationForLayer) {
            class_243 converted3 = cameraSpaceDirectionToWorld(client, new Vector3f(a4.x, a4.y, a4.z), arm, handFallback);
            worldAxis = normalizeAxis(converted3 != null ? converted3 : releaseVectorFromRenderSpace(a4, cameraRot, true));
        } else {
            worldAxis = normalizeAxis(releaseVectorFromRenderSpace(a4, cameraRot, shaderPackInUse));
        }
        if (!isFinite(worldPos) || !isFinite(worldVel) || !isFinite(worldCenter) || !isFinite(worldAxis)) {
            return false;
        }
        class_243 class_243Var2 = particle.position;
        new Vector3f(currentRenderPos);
        particle.pendingReleaseWorldPos = worldPos;
        particle.pendingReleaseWorldVel = worldVel;
        particle.pendingReleaseWorldCenter = worldCenter;
        particle.pendingReleaseWorldAxis = worldAxis;
        particle.pendingReleaseSnapshotReady = true;
        return false;
    }

    private static boolean applyPendingWorldReleaseSnapshot(RenderParticle particle) {
        if (particle == null || !particle.localSpace || !particle.pendingWorldRelease || !particle.pendingReleaseSnapshotReady) {
            return false;
        }
        if (!isFinite(particle.pendingReleaseWorldPos) || !isFinite(particle.pendingReleaseWorldVel) || !isFinite(particle.pendingReleaseWorldCenter) || !isFinite(particle.pendingReleaseWorldAxis)) {
            particle.pendingReleaseSnapshotReady = false;
            return false;
        }
        particle.position = particle.pendingReleaseWorldPos;
        particle.velocity = particle.pendingReleaseWorldVel;
        particle.orbitCenter = particle.pendingReleaseWorldCenter;
        particle.orbitAxis = particle.pendingReleaseWorldAxis;
        particle.localSpace = false;
        particle.pendingWorldRelease = false;
        particle.pendingReleaseSnapshotReady = false;
        return true;
    }

    private static class_243 releasePointFromRenderSpace(class_243 cameraPos, Vector3f renderPos, Quaternionf cameraRot, boolean shaderPackInUse) {
        if (cameraPos == null || renderPos == null) {
            return cameraPos == null ? class_243.field_1353 : cameraPos;
        }
        if (shaderPackInUse && cameraRot != null) {
            Vector3f rotated = new Vector3f(renderPos).rotate(cameraRot);
            return cameraPos.method_1031(rotated.x, rotated.y, rotated.z);
        }
        return cameraPos.method_1031(renderPos.x, renderPos.y, renderPos.z);
    }

    private static Vector3f transformRenderPoint(Matrix4f pose, class_243 localPos) {
        if (localPos == null) {
            return new Vector3f();
        }
        Vector4f view4 = new Vector4f((float) localPos.field_1352, (float) localPos.field_1351, (float) localPos.field_1350, 1.0f);
        if (pose != null) {
            pose.transform(view4);
        }
        return new Vector3f(view4.x, view4.y, view4.z);
    }

    private static Vector3f renderPointIgnoringPoseRotation(Matrix4f pose, class_243 localPos, Quaternionf cameraRot, boolean shaderPackInUse) {
        if (localPos == null) {
            return new Vector3f();
        }
        if (pose == null) {
            return new Vector3f((float) localPos.field_1352, (float) localPos.field_1351, (float) localPos.field_1350);
        }
        Vector3f translation = pose.getTranslation(new Vector3f());
        float sx = (float) Math.sqrt((pose.m00() * pose.m00()) + (pose.m10() * pose.m10()) + (pose.m20() * pose.m20()));
        float sy = (float) Math.sqrt((pose.m01() * pose.m01()) + (pose.m11() * pose.m11()) + (pose.m21() * pose.m21()));
        float sz = (float) Math.sqrt((pose.m02() * pose.m02()) + (pose.m12() * pose.m12()) + (pose.m22() * pose.m22()));
        if (!Float.isFinite(sx) || sx <= 0.0f) {
            sx = 1.0f;
        }
        if (!Float.isFinite(sy) || sy <= 0.0f) {
            sy = 1.0f;
        }
        if (!Float.isFinite(sz) || sz <= 0.0f) {
            sz = 1.0f;
        }
        Vector3f localScaled = new Vector3f(((float) localPos.field_1352) * sx, ((float) localPos.field_1351) * sy, ((float) localPos.field_1350) * sz);
        if (!shaderPackInUse && cameraRot != null) {
            localScaled.rotate(cameraRot);
        }
        return new Vector3f(translation.x + localScaled.x, translation.y + localScaled.y, translation.z + localScaled.z);
    }

    private static Vector3f transformVectorIgnoringPoseRotation(Matrix4f pose, Vector3f localVec) {
        if (localVec == null) {
            return new Vector3f();
        }
        if (pose == null) {
            return new Vector3f(localVec);
        }
        float sx = (float) Math.sqrt((pose.m00() * pose.m00()) + (pose.m10() * pose.m10()) + (pose.m20() * pose.m20()));
        float sy = (float) Math.sqrt((pose.m01() * pose.m01()) + (pose.m11() * pose.m11()) + (pose.m21() * pose.m21()));
        float sz = (float) Math.sqrt((pose.m02() * pose.m02()) + (pose.m12() * pose.m12()) + (pose.m22() * pose.m22()));
        if (!Float.isFinite(sx) || sx <= 0.0f) {
            sx = 1.0f;
        }
        if (!Float.isFinite(sy) || sy <= 0.0f) {
            sy = 1.0f;
        }
        if (!Float.isFinite(sz) || sz <= 0.0f) {
            sz = 1.0f;
        }
        return new Vector3f(localVec.x * sx, localVec.y * sy, localVec.z * sz);
    }

    private static class_243 releaseVectorFromRenderSpace(Vector4f renderVec, Quaternionf cameraRot, boolean shaderPackInUse) {
        if (renderVec == null) {
            return class_243.field_1353;
        }
        Vector3f v = new Vector3f(renderVec.x, renderVec.y, renderVec.z);
        if (shaderPackInUse && cameraRot != null) {
            v.rotate(cameraRot);
        }
        return new class_243(v.x, v.y, v.z);
    }

    private static class_243 pushBehindLocalLayer(class_243 cameraPos, class_243 worldPos, float bias, Quaternionf cameraRot) {
        if (worldPos == null || !isFinite(worldPos)) {
            return worldPos;
        }
        double distSq = cameraPos == null ? 0.0d : worldPos.method_1025(cameraPos);
        double amount = Math.max(1.0E-4d, bias);
        if (cameraPos != null && distSq > 1.0E-10d) {
            class_243 dir = worldPos.method_1020(cameraPos).method_1029();
            return worldPos.method_1019(dir.method_1021(amount));
        }
        if (cameraRot != null) {
            Vector3f forward = new Vector3f(0.0f, 0.0f, 1.0f).rotate(cameraRot);
            return worldPos.method_1031(((double) forward.x) * amount, ((double) forward.y) * amount, ((double) forward.z) * amount);
        }
        return worldPos.method_1031(0.0d, 0.0d, amount);
    }

    private static boolean isParticleOccludedByWorld(class_310 client, class_243 cameraPos, class_243 worldPos) {
        class_3965 class_3965VarMethod_17742;
        if (client == null || client.field_1687 == null || cameraPos == null || worldPos == null || !isFinite(cameraPos) || !isFinite(worldPos) || (class_3965VarMethod_17742 = client.field_1687.method_17742(new class_3959(cameraPos, worldPos, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, client.field_1724))) == null || class_3965VarMethod_17742.method_17783() != class_239.class_240.field_1332 || class_3965VarMethod_17742.method_17784() == null) {
            return false;
        }
        double hitDist = Math.sqrt(class_3965VarMethod_17742.method_17784().method_1025(cameraPos));
        double particleDist = Math.sqrt(worldPos.method_1025(cameraPos));
        return Double.isFinite(hitDist) && Double.isFinite(particleDist) && hitDist + PARTICLE_OCCLUSION_ENTRY_MARGIN < particleDist - PARTICLE_OCCLUSION_EPSILON;
    }

    private static class_243 resolveCameraMotionPixels(class_310 client, class_1306 arm) {
        Vector3f vector3f;
        if (client == null || client.field_1773 == null || arm == null) {
            return class_243.field_1353;
        }
        class_4184 camera = client.field_1773.method_19418();
        if (camera == null || camera.method_19326() == null || camera.method_23767() == null) {
            return class_243.field_1353;
        }
        class_243 pos = camera.method_19326();
        class_243 last = LAST_CAMERA_POS.put(arm, pos);
        if (last == null) {
            return class_243.field_1353;
        }
        class_243 delta = pos.method_1020(last);
        if (!isFinite(delta) || delta.method_1027() > 4.0d) {
            return class_243.field_1353;
        }
        Quaternionf inv = new Quaternionf(camera.method_23767()).invert();
        Vector3f cameraLocalDelta = new Vector3f((float) delta.field_1352, (float) delta.field_1351, (float) delta.field_1350).rotate(inv);
        Matrix4f invRoot = RENDER_ROOT_INV.get(arm);
        if (invRoot != null) {
            boolean shaderPackInUse = isShaderPackInUse();
            if (shaderPackInUse) {
                vector3f = cameraLocalDelta;
            } else {
                vector3f = new Vector3f((float) delta.field_1352, (float) delta.field_1351, (float) delta.field_1350);
            }
            Vector3f source = vector3f;
            Vector4f local = new Vector4f(source.x, source.y, source.z, 0.0f);
            invRoot.transform(local);
            if (Float.isFinite(local.x) && Float.isFinite(local.y) && Float.isFinite(local.z)) {
                return new class_243(local.x, local.y, local.z);
            }
        }
        Vector3f localDelta = cameraLocalDelta.mul(16.0f);
        float lx = localDelta.x;
        float lz = localDelta.z;
        localDelta.x = lz;
        localDelta.z = -lx;
        return new class_243(localDelta.x, localDelta.y, localDelta.z);
    }

    private static class_243 resolveCameraTurnPixels(class_310 client, class_1306 arm, float tickScale) {
        float simX;
        float simY;
        float simZ;
        if (client == null || client.field_1773 == null || arm == null) {
            return class_243.field_1353;
        }
        class_4184 camera = client.field_1773.method_19418();
        if (camera == null || camera.method_23767() == null) {
            return class_243.field_1353;
        }
        Quaternionf current = new Quaternionf(camera.method_23767());
        Quaternionf previous = LAST_CAMERA_ROT.put(arm, new Quaternionf(current));
        if (previous == null) {
            LAST_CAMERA_TURN_DELTA_CAMERA_LOCAL.remove(arm);
            return class_243.field_1353;
        }
        Vector3f prevForward = new Vector3f(0.0f, 0.0f, 1.0f).rotate(previous);
        Vector3f currForward = new Vector3f(0.0f, 0.0f, 1.0f).rotate(current);
        Vector3f deltaWorld = currForward.sub(prevForward);
        Quaternionf invCurrent = new Quaternionf(current).invert();
        Vector3f deltaCameraLocal = deltaWorld.rotate(invCurrent);
        Quaternionf cameraTurnDeltaCameraLocal = new Quaternionf(invCurrent).mul(previous).normalize();
        LAST_CAMERA_TURN_DELTA_CAMERA_LOCAL.put(arm, cameraTurnDeltaCameraLocal);
        Matrix4f invRoot = RENDER_ROOT_INV.get(arm);
        if (invRoot != null) {
            boolean shaderPackInUse = isShaderPackInUse();
            Vector3f source = shaderPackInUse ? deltaCameraLocal : deltaWorld;
            Vector4f local = new Vector4f(source.x, source.y, source.z, 0.0f);
            invRoot.transform(local);
            if (Float.isFinite(local.x) && Float.isFinite(local.y) && Float.isFinite(local.z)) {
                simX = local.x;
                simY = local.y;
                simZ = local.z;
            } else {
                float lx = deltaCameraLocal.x;
                float ly = deltaCameraLocal.y;
                float lz = deltaCameraLocal.z;
                simX = lz;
                simY = ly;
                simZ = -lx;
            }
        } else {
            float lx2 = deltaCameraLocal.x;
            float ly2 = deltaCameraLocal.y;
            float lz2 = deltaCameraLocal.z;
            simX = lz2;
            simY = ly2;
            simZ = -lx2;
        }
        float strength = 12.8f * class_3532.method_15363(tickScale, 0.0f, 3.0f);
        return new class_243(simX * strength, simY * strength, simZ * strength);
    }

    private static class_243 orbitOffset(class_243 axis, float radius, float angleRad) {
        if (radius <= EPS) {
            return class_243.field_1353;
        }
        class_243 normalizedAxis = normalizeAxis(axis);
        class_243 perpendicular = perpendicularUnit(normalizedAxis);
        Quaternionf rotation = new Quaternionf().fromAxisAngleRad((float) normalizedAxis.field_1352, (float) normalizedAxis.field_1351, (float) normalizedAxis.field_1350, angleRad);
        Vector3f rotated = new Vector3f((float) perpendicular.field_1352, (float) perpendicular.field_1351, (float) perpendicular.field_1350).rotate(rotation).mul(radius);
        return new class_243(rotated.x, rotated.y, rotated.z);
    }

    private static class_243 normalizeAxis(class_243 axis) {
        if (!isFinite(axis) || axis.method_1027() <= 1.0E-8d) {
            return new class_243(0.0d, 1.0d, 0.0d);
        }
        return axis.method_1029();
    }

    private static class_243 perpendicularUnit(class_243 axis) {
        class_243 normalized = normalizeAxis(axis);
        class_243 reference = Math.abs(normalized.field_1351) > 0.98d ? new class_243(1.0d, 0.0d, 0.0d) : new class_243(0.0d, 1.0d, 0.0d);
        class_243 perpendicular = normalized.method_1036(reference);
        if (perpendicular.method_1027() <= 1.0E-8d) {
            perpendicular = normalized.method_1036(new class_243(0.0d, 0.0d, 1.0d));
        }
        return perpendicular.method_1027() <= 1.0E-8d ? new class_243(1.0d, 0.0d, 0.0d) : perpendicular.method_1029();
    }

    private static int resolveParticleLight(class_310 client, RenderParticle particle, int packedLight) {
        float fMethod_15363;
        if (particle == null) {
            return packedLight;
        }
        if (PunchyConfig.enableModelPartsGlow() && particle.fullBright && particle.lightPercent < 0.0f) {
            return 15728880;
        }
        if (particle.lightPercent < 0.0f) {
            fMethod_15363 = (PunchyConfig.enableModelPartsGlow() && particle.fullBright) ? 1.0f : 0.0f;
        } else {
            fMethod_15363 = class_3532.method_15363(particle.lightPercent / 100.0f, 0.0f, 1.0f);
        }
        float mix = fMethod_15363;
        int current = packedLight;
        if (mix > 0.0f) {
            if (mix >= 1.0f) {
                current = 15728880;
            } else {
                current = lerpPackedLight(current, 15728880, mix);
            }
        }
        if (PunchyConfig.enableModelPartsGlow() && isFullDayLightActive(client, particle)) {
            current = 15728880;
        }
        return current;
    }

    private static boolean isParticleEmissiveActive(class_310 client, RenderParticle particle) {
        if (particle == null || !PunchyConfig.enableModelPartsGlow()) {
            return false;
        }
        return particle.fullBright || isFullDayLightActive(client, particle);
    }

    private static boolean isFullDayLightActive(class_310 client, RenderParticle particle) {
        if (particle == null || !particle.fullDayLight || client == null || client.field_1687 == null || !client.field_1687.method_8597().comp_642()) {
            return false;
        }
        long dayTime = client.field_1687.method_8532() % 24000;
        return dayTime >= BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS && dayTime < 12000;
    }

    private static float resolvePackedLightBrightness(class_310 client, int packedLight) {
        int block = class_3532.method_15340((packedLight >> 4) & 65535, 0, 15);
        int sky = class_3532.method_15340((packedLight >> 20) & 65535, 0, 15);
        float blockBrightness = block / 15.0f;
        float skyBrightness = sky / 15.0f;
        if (client != null && client.field_1687 != null && client.field_1687.method_8597().comp_642()) {
            float skyDarken = class_3532.method_15363(client.field_1687.method_8594() / 15.0f, 0.0f, 1.0f);
            skyBrightness *= 1.0f - skyDarken;
        }
        return class_3532.method_15363(Math.max(blockBrightness, skyBrightness), 0.0f, 1.0f);
    }

    private static float smoothBrightAtDark(float current, float target) {
        if (!Float.isFinite(current)) {
            return target;
        }
        float rate = target > current ? 0.18f : 0.05f;
        return class_3532.method_16439(rate, current, target);
    }

    private static float resolveParticleRenderAlpha(class_310 client, RenderParticle particle, int sampledLight, float alpha) {
        if (particle == null || !particle.brightAtDark) {
            return alpha;
        }
        return resolveBrightAtDarkVisibilityAlpha(client, particle, sampledLight, alpha);
    }

    private static float resolveBrightAtDarkVisibilityAlpha(class_310 client, RenderParticle particle, int packedLight, float maxAlpha) {
        if (particle == null || !particle.brightAtDark) {
            return maxAlpha;
        }
        float brightness = resolvePackedLightBrightness(client, packedLight);
        float darkness = 1.0f - brightness;
        float clampedMaxAlpha = class_3532.method_15363(maxAlpha, 0.1f, 1.0f);
        float target = class_3532.method_16439(darkness, 0.1f, clampedMaxAlpha);
        particle.brightAtDarkVisibilityFactor = smoothBrightAtDark(particle.brightAtDarkVisibilityFactor, target);
        return particle.brightAtDarkVisibilityFactor;
    }

    private static float resolveBrightAtDarkGlowFactor(class_310 client, RenderParticle particle, int packedLight) {
        if (particle == null || !particle.brightAtDark) {
            return 0.0f;
        }
        float brightness = resolvePackedLightBrightness(client, packedLight);
        float darkness = 1.0f - brightness;
        float target = class_3532.method_16439(darkness, 0.2f, 1.0f);
        particle.brightAtDarkGlowFactor = smoothBrightAtDark(particle.brightAtDarkGlowFactor, target);
        return particle.brightAtDarkGlowFactor;
    }

    private static int lerpPackedLight(int from, int to, float mix) {
        float t = class_3532.method_15363(mix, 0.0f, 1.0f);
        int fromBlock = (from >> 4) & 65535;
        int fromSky = (from >> 20) & 65535;
        int toBlock = (to >> 4) & 65535;
        int toSky = (to >> 20) & 65535;
        int outBlock = class_3532.method_15340(Math.round(class_3532.method_16439(t, fromBlock, toBlock)), 0, 65535);
        int outSky = class_3532.method_15340(Math.round(class_3532.method_16439(t, fromSky, toSky)), 0, 65535);
        return (outBlock << 4) | (outSky << 20);
    }

    private static class_2960 resolveAnimatedParticleTexture(class_310 client, RenderParticle particle, class_2960 baseTexture) {
        if (client == null || particle == null || baseTexture == null) {
            return baseTexture;
        }
        if (particle.lockInModel) {
            return baseTexture;
        }
        String path = baseTexture.method_12832();
        if (path == null || !path.endsWith(".png")) {
            return baseTexture;
        }
        int dot = path.length() - 4;
        int underscore = path.lastIndexOf(95, dot - 1);
        if (underscore < 0 || underscore >= dot - 1) {
            return baseTexture;
        }
        String suffix = path.substring(underscore + 1, dot);
        if (!"0".equals(suffix)) {
            return baseTexture;
        }
        int frameCount = getParticleAnimationFrameCount(client, baseTexture);
        if (frameCount <= 1) {
            return baseTexture;
        }
        float animFps = Math.max(1.0f, PARTICLE_TEXTURE_ANIMATION_FPS);
        int frame = ((int) Math.floor(Math.max(0.0f, particle.ageTicks) * (animFps / 20.0f))) % frameCount;
        if (frame <= 0) {
            return baseTexture;
        }
        String animatedPath = path.substring(0, underscore + 1) + frame + ".png";
        return class_2960.method_60655(baseTexture.method_12836(), animatedPath);
    }

    private static int getParticleAnimationFrameCount(class_310 client, class_2960 baseTexture) {
        Integer cached = PARTICLE_ANIM_FRAME_COUNT_CACHE.get(baseTexture);
        if (cached != null) {
            return cached.intValue();
        }
        int detected = detectParticleAnimationFrameCount(client, baseTexture);
        PARTICLE_ANIM_FRAME_COUNT_CACHE.put(baseTexture, Integer.valueOf(detected));
        return detected;
    }

    private static int detectParticleAnimationFrameCount(class_310 client, class_2960 baseTexture) {
        String path;
        if (client == null) {
            return 1;
        }
        try {
            if (client.method_1478() == null || baseTexture == null || (path = baseTexture.method_12832()) == null || !path.endsWith(".png")) {
                return 1;
            }
            int dot = path.length() - 4;
            int underscore = path.lastIndexOf(95, dot - 1);
            if (underscore < 0 || underscore >= dot - 1 || !"0".equals(path.substring(underscore + 1, dot))) {
                return 1;
            }
            int count = 1;
            for (int frame = 1; frame < 64; frame++) {
                class_2960 candidate = class_2960.method_60655(baseTexture.method_12836(), path.substring(0, underscore + 1) + frame + ".png");
                Optional<class_3298> resource = client.method_1478().method_14486(candidate);
                if (!resource.isPresent()) {
                    break;
                }
                count++;
            }
            return count;
        } catch (Exception e) {
            return 1;
        }
    }

    private static int resolveParticleWorldLight(class_310 client, RenderParticle particle, class_1306 arm, class_243 cameraPos, Vector3f renderPos, int fallbackPackedLight) {
        class_243 worldPos;
        if (client == null || client.field_1687 == null || particle == null) {
            return fallbackPackedLight;
        }
        try {
            if (particle.localSpace) {
                if (client.field_1724 == null) {
                    return fallbackPackedLight;
                }
                worldPos = resolveParticleLightSampleWorld(client, client.field_1724, arm, null, true);
                if (worldPos == null && cameraPos != null) {
                    worldPos = cameraPos;
                }
            } else {
                worldPos = particle.position;
            }
            if (worldPos == null || !isFinite(worldPos)) {
                return fallbackPackedLight;
            }
            class_2338 pos = class_2338.method_49637(worldPos.field_1352, worldPos.field_1351, worldPos.field_1350);
            return class_761.method_23794(client.field_1687, pos);
        } catch (Throwable th) {
            return fallbackPackedLight;
        }
    }

    private static class_243 resolveParticleLightSampleWorld(class_310 client, class_746 player, class_1306 arm, MiningActionTracker.Target target, boolean preferHand) {
        if (target != null) {
            if (target.hitLocation() != null && isFinite(target.hitLocation())) {
                return target.hitLocation();
            }
            if (target.pos() != null) {
                return class_243.method_24953(target.pos());
            }
        }
        if (player != null) {
            class_243 handFallback = resolveHandFallback(client, player, arm);
            if (handFallback != null && isFinite(handFallback)) {
                return handFallback;
            }
            class_243 eye = player.method_5836(1.0f);
            if (eye != null && isFinite(eye)) {
                return eye;
            }
        }
        if (!preferHand && client != null && client.field_1765 != null && client.field_1765.method_17784() != null && isFinite(client.field_1765.method_17784())) {
            return client.field_1765.method_17784();
        }
        return null;
    }

    private static void addBillboardQuad(class_4587.class_4665 pose, class_4588 consumer, Vector3f center, float size, float alpha, int packedLight, float red, float green, float blue) {
        if (pose == null || consumer == null || center == null) {
            return;
        }
        int r = scaleToByte(red);
        int g = scaleToByte(green);
        int b = scaleToByte(blue);
        int a = scaleToByte(alpha);
        if (a <= 0) {
            return;
        }
        float x0 = center.x - size;
        float x1 = center.x + size;
        float y0 = center.y - size;
        float y1 = center.y + size;
        float z = center.z;
        consumer.method_22918(pose.method_23761(), x0, y0, z).method_1336(r, g, b, a).method_22913(0.0f, 1.0f).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, 0.0f, 0.0f, 1.0f);
        consumer.method_22918(pose.method_23761(), x1, y0, z).method_1336(r, g, b, a).method_22913(1.0f, 1.0f).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, 0.0f, 0.0f, 1.0f);
        consumer.method_22918(pose.method_23761(), x1, y1, z).method_1336(r, g, b, a).method_22913(1.0f, 0.0f).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, 0.0f, 0.0f, 1.0f);
        consumer.method_22918(pose.method_23761(), x0, y1, z).method_1336(r, g, b, a).method_22913(0.0f, 0.0f).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, 0.0f, 0.0f, 1.0f);
    }

    private static void addEmissiveBlurHalo(class_4587.class_4665 sourcePose, class_4597 buffer, class_2960 texture, boolean shaderPackInUse, boolean clamp, float size, float baseScale, float baseAlpha, float red, float green, float blue) {
        if (sourcePose == null || buffer == null || baseAlpha <= 0.0f) {
            return;
        }
        class_1921 blurRenderType = emissiveOverlayRenderType(texture, shaderPackInUse, true, clamp);
        class_4588 blurConsumer = buffer.getBuffer(blurRenderType);
        float[] alphaWeights = {0.52f, 0.31f, 0.17f};
        float[] scaleOffsets = {0.0f, 0.08f, 0.18f};
        for (int i = 0; i < alphaWeights.length; i++) {
            float layerAlpha = Math.min(1.0f, baseAlpha * alphaWeights[i]);
            if (layerAlpha > 0.0f) {
                class_4587 blurStack = new class_4587();
                blurStack.method_23760().method_23761().set(sourcePose.method_23761());
                blurStack.method_23760().method_23762().set(sourcePose.method_23762());
                float layerScale = baseScale + scaleOffsets[i];
                blurStack.method_22905(layerScale, layerScale, layerScale);
                float layerColorScale = 1.0f - (i * 0.08f);
                addBillboardQuad(blurStack.method_23760(), blurConsumer, new Vector3f(0.0f, 0.0f, 0.0f), size, layerAlpha, 15728880, class_3532.method_15363(red * layerColorScale, 0.0f, 1.0f), class_3532.method_15363(green * layerColorScale, 0.0f, 1.0f), class_3532.method_15363(blue * layerColorScale, 0.0f, 1.0f));
            }
        }
    }

    private static class_2960 resolveCustomParticleTexture(ParticleScriptSpec spec) {
        class_2960 parsed;
        if (spec != null && spec.textureId != null && !spec.textureId.isBlank() && (parsed = class_2960.method_12829(spec.textureId.trim())) != null) {
            class_2960 resolved = resolveExplicitParticleTexture(parsed);
            if (resolved != null) {
                return resolved;
            }
            String fallbackPath = parsed.method_12832().endsWith(".png") ? parsed.method_12832() : parsed.method_12832() + ".png";
            return class_2960.method_60655(parsed.method_12836(), fallbackPath);
        }
        if (spec == null || spec.typeId == null || spec.typeId.isBlank()) {
            return DEFAULT_PARTICLE_TEXTURE;
        }
        class_2960 fromDefinition = resolveTextureFromParticleDefinition(spec.typeId);
        if (fromDefinition != null) {
            return fromDefinition;
        }
        class_2960 fromType = resolveTextureFromType(spec.typeId);
        if (fromType != null) {
            return fromType;
        }
        String lowered = spec.typeId.toLowerCase(Locale.ROOT);
        if (lowered.contains("soul")) {
            return class_2960.method_60655("minecraft", "textures/particle/soul_0.png");
        }
        if (lowered.contains("smoke")) {
            return class_2960.method_60655("minecraft", "textures/particle/generic_0.png");
        }
        if (lowered.contains("campfire") || lowered.contains("ash")) {
            return class_2960.method_60655("minecraft", "textures/particle/ash.png");
        }
        return DEFAULT_PARTICLE_TEXTURE;
    }

    private static class_2960 resolveExplicitParticleTexture(class_2960 parsed) {
        if (parsed == null) {
            return null;
        }
        String path = parsed.method_12832();
        if (path == null || path.isBlank()) {
            return null;
        }
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String direct = path.endsWith(".png") ? path : path + ".png";
        candidates.add(direct);
        if (!path.startsWith("textures/")) {
            String texturesPath = "textures/" + path;
            candidates.add(texturesPath.endsWith(".png") ? texturesPath : texturesPath + ".png");
            String particlePath = "textures/particle/" + path;
            candidates.add(particlePath.endsWith(".png") ? particlePath : particlePath + ".png");
        }
        if (path.startsWith("particle/")) {
            String fromParticle = "textures/" + path;
            candidates.add(fromParticle.endsWith(".png") ? fromParticle : fromParticle + ".png");
        }
        for (String candidate : candidates) {
            class_2960 id = class_2960.method_60655(parsed.method_12836(), candidate);
            if (textureExists(id)) {
                return id;
            }
        }
        return null;
    }

    private static class_2960 resolveTextureFromParticleDefinition(String typeId) {
        Reader reader;
        JsonArray textures;
        if (typeId == null || typeId.isBlank()) {
            return null;
        }
        String key = typeId.trim().toLowerCase(Locale.ROOT);
        if (PARTICLE_TEXTURE_CACHE.containsKey(key)) {
            return PARTICLE_TEXTURE_CACHE.get(key);
        }
        class_2960 type = class_2960.method_12829(key);
        if (type == null) {
            PARTICLE_TEXTURE_CACHE.put(key, null);
            return null;
        }
        class_2960 particleDef = class_2960.method_60655(type.method_12836(), "particles/" + type.method_12832() + ".json");
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.method_1478() == null) {
            PARTICLE_TEXTURE_CACHE.put(key, null);
            return null;
        }
        Optional<class_3298> res = mc.method_1478().method_14486(particleDef);
        if (res.isEmpty()) {
            PARTICLE_TEXTURE_CACHE.put(key, null);
            return null;
        }
        try {
            reader = res.get().method_43039();
            try {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                textures = root.getAsJsonArray("textures");
            } finally {
            }
        } catch (Exception e) {
        }
        if (textures == null || textures.isEmpty()) {
            PARTICLE_TEXTURE_CACHE.put(key, null);
            if (reader != null) {
                reader.close();
            }
            return null;
        }
        JsonElement first = textures.get(0);
        if (first == null || !first.isJsonPrimitive()) {
            PARTICLE_TEXTURE_CACHE.put(key, null);
            if (reader != null) {
                reader.close();
            }
            return null;
        }
        class_2960 texRef = class_2960.method_12829(first.getAsString());
        if (texRef == null) {
            PARTICLE_TEXTURE_CACHE.put(key, null);
            if (reader != null) {
                reader.close();
            }
            return null;
        }
        String path = texRef.method_12832();
        if (path.startsWith("particle/")) {
            path = path.substring("particle/".length());
        }
        class_2960 texture = class_2960.method_60655(texRef.method_12836(), "textures/particle/" + path + ".png");
        if (textureExists(texture)) {
            PARTICLE_TEXTURE_CACHE.put(key, texture);
            if (reader != null) {
                reader.close();
            }
            return texture;
        }
        if (reader != null) {
            reader.close();
        }
        PARTICLE_TEXTURE_CACHE.put(key, null);
        return null;
    }

    private static class_2960 resolveTextureFromType(String typeId) {
        class_2960 type = class_2960.method_12829(typeId.trim());
        if (type == null) {
            return null;
        }
        String ns = type.method_12836();
        String path = type.method_12832();
        List<String> bases = new ArrayList<>();
        bases.add(path);
        if (path.endsWith("_particle")) {
            bases.add(path.substring(0, path.length() - "_particle".length()));
        }
        if (path.startsWith("particle/")) {
            bases.add(path.substring("particle/".length()));
        }
        for (String base : bases) {
            if (base != null && !base.isBlank()) {
                class_2960 direct = class_2960.method_60655(ns, "textures/particle/" + base + ".png");
                if (textureExists(direct)) {
                    return direct;
                }
                class_2960 frame0 = class_2960.method_60655(ns, "textures/particle/" + base + "_0.png");
                if (textureExists(frame0)) {
                    return frame0;
                }
            }
        }
        return null;
    }

    private static boolean textureExists(class_2960 texture) {
        class_310 mc;
        if (texture == null || (mc = class_310.method_1551()) == null || mc.method_1478() == null) {
            return false;
        }
        return mc.method_1478().method_14486(texture).isPresent();
    }

    private static Vector3f resolveCustomParticleTint(ParticleScriptSpec spec) {
        if (spec != null && spec.tint != null) {
            return new Vector3f(class_3532.method_15363(spec.tint.x, 0.0f, 1.0f), class_3532.method_15363(spec.tint.y, 0.0f, 1.0f), class_3532.method_15363(spec.tint.z, 0.0f, 1.0f));
        }
        String typeId = spec == null ? null : spec.typeId;
        if (typeId == null || typeId.isBlank()) {
            return new Vector3f(1.0f, 1.0f, 1.0f);
        }
        String id = typeId.trim().toLowerCase(Locale.ROOT);
        if (id.contains("falling_lava") || id.contains("landing_lava") || id.contains("dripping_lava") || id.contains("dripstone_lava")) {
            return new Vector3f(1.0f, 0.2857143f, 0.083333336f);
        }
        return new Vector3f(1.0f, 1.0f, 1.0f);
    }

    private static int scaleToByte(float value) {
        float clamped = class_3532.method_15363(value, 0.0f, 1.0f);
        return Math.max(0, Math.min(255, Math.round(clamped * 255.0f)));
    }

    private static double randomSpread(class_310 client, double amount) {
        if (amount <= 0.0d) {
            return 0.0d;
        }
        return ((client.field_1687.field_9229.method_43058() * 2.0d) - 1.0d) * amount;
    }

    private static class_243 resolveBoneBase(class_746 player, class_1306 arm, Map<String, class_630> parts, String requestedBone) {
        class_243 right;
        class_243 eye = player.method_5836(1.0f);
        class_243 forward = player.method_5828(1.0f).method_1029();
        class_243 upWorld = new class_243(0.0d, 1.0d, 0.0d);
        class_243 right2 = upWorld.method_1036(forward);
        if (right2.method_1027() < 1.0E-6d) {
            right = new class_243(arm == class_1306.field_6183 ? 1.0d : -1.0d, 0.0d, 0.0d);
        } else {
            right = right2.method_1029();
        }
        class_243 up = forward.method_1036(right).method_1029();
        class_243 handBase = eye.method_1019(forward.method_1021(0.45d)).method_1019(up.method_1021(-0.25d)).method_1019(right.method_1021(arm == class_1306.field_6183 ? PARTICLE_OCCLUSION_ENTRY_MARGIN : -0.22d));
        String bone = resolveBoneName(parts, requestedBone, arm);
        if (bone == null) {
            return handBase;
        }
        Matrix4f world = computeBoneWorld(parts, bone);
        if (world == null) {
            return handBase;
        }
        Vector3f translationPixels = world.getTranslation(new Vector3f());
        class_243 local = new class_243(((double) translationPixels.x) / 16.0d, ((double) translationPixels.y) / 16.0d, ((double) translationPixels.z) / 16.0d);
        return handBase.method_1019(right.method_1021(local.field_1352)).method_1019(up.method_1021(local.field_1351)).method_1019(forward.method_1021(local.field_1350));
    }

    private static class_243 applyBasis(class_746 player, class_1306 arm, Vector3f local) {
        class_243 right;
        class_243 forward = player.method_5828(1.0f).method_1029();
        class_243 upWorld = new class_243(0.0d, 1.0d, 0.0d);
        class_243 right2 = upWorld.method_1036(forward);
        if (right2.method_1027() < 1.0E-6d) {
            right = new class_243(arm == class_1306.field_6183 ? 1.0d : -1.0d, 0.0d, 0.0d);
        } else {
            right = right2.method_1029();
        }
        class_243 up = forward.method_1036(right).method_1029();
        return right.method_1021(local.x).method_1019(up.method_1021(local.y)).method_1019(forward.method_1021(local.z));
    }

    private static class_243 resolveRenderBoneBase(class_310 client, class_746 player, class_243 fallback, Matrix4f boneTransform, Matrix4f localBoneTransform, class_1306 arm, String projectionMode) {
        if (fallback == null) {
            return class_243.field_1353;
        }
        if (boneTransform == null) {
            return fallback;
        }
        Vector3f renderPos = boneTransform.getTranslation(new Vector3f());
        class_243 exact = cameraSpaceToWorld(client, renderPos, arm, fallback);
        if (exact != null && isFinite(exact)) {
            return exact;
        }
        if ("raw".equals(projectionMode)) {
            if (localBoneTransform == null) {
                return fallback;
            }
            Vector3f localPos = localBoneTransform.getTranslation(new Vector3f()).mul(0.0625f);
            class_243 localOffset = applyCameraBasis(client, localPos);
            if (localOffset == null) {
                localOffset = applyBasis(player, arm, localPos);
            }
            return fallback.method_1019(localOffset);
        }
        return fallback;
    }

    private static class_243 applyRenderBasis(class_310 client, Matrix4f boneTransform, Matrix4f localBoneTransform, Vector3f local, class_746 player, class_1306 arm, String space, boolean worldDefault, class_243 fallback, String projectionMode, boolean ignoreBoneRotation) {
        String resolved = normalizeSpace(space, worldDefault);
        if ("world".equals(resolved)) {
            return new class_243(local.x, local.y, local.z);
        }
        if ("raw".equals(projectionMode)) {
            if ("camera".equals(resolved)) {
                class_243 viaCamera = applyCameraBasis(client, local);
                return viaCamera != null ? viaCamera : applyBasis(player, arm, local);
            }
            Vector3f oriented = new Vector3f(local);
            Matrix4f basis = ignoreBoneRotation ? null : localBoneTransform != null ? localBoneTransform : boneTransform;
            if (basis != null) {
                transformVectorByMatrixBasis(basis, oriented);
            }
            class_243 viaCamera2 = applyCameraBasis(client, oriented);
            return viaCamera2 != null ? viaCamera2 : applyBasis(player, arm, oriented);
        }
        if ("camera".equals(resolved)) {
            class_243 transformed = cameraSpaceDirectionToWorld(client, local, arm, fallback);
            if (transformed != null) {
                return transformed;
            }
            return applyBasis(player, arm, local);
        }
        Vector3f orientedLocal = new Vector3f(local);
        Matrix4f basis2 = ignoreBoneRotation ? null : localBoneTransform != null ? localBoneTransform : boneTransform;
        if (basis2 != null) {
            transformVectorByMatrixBasis(basis2, orientedLocal);
        }
        class_243 transformed2 = cameraSpaceDirectionToWorld(client, orientedLocal, arm, fallback);
        if (transformed2 != null) {
            return transformed2;
        }
        return applyBasis(player, arm, orientedLocal);
    }

    private static Vector3f applyRenderBasisLocal(Matrix4f boneTransform, Vector3f local, String space, boolean worldDefault, boolean ignoreBoneRotation) {
        String resolved = normalizeSpace(space, worldDefault);
        Vector3f out = new Vector3f(local);
        if (!ignoreBoneRotation && "bone".equals(resolved) && boneTransform != null) {
            transformVectorByMatrixBasis(boneTransform, out);
        }
        return out;
    }

    private static void transformVectorByMatrixBasis(Matrix4f matrix, Vector3f vector) {
        if (matrix == null || vector == null) {
            return;
        }
        Vector3f bxRaw = new Vector3f(matrix.m00(), matrix.m10(), matrix.m20());
        Vector3f byRaw = new Vector3f(matrix.m01(), matrix.m11(), matrix.m21());
        Vector3f bzRaw = new Vector3f(matrix.m02(), matrix.m12(), matrix.m22());
        Vector3f bx = normalizeOrFallback(bxRaw, new Vector3f(1.0f, 0.0f, 0.0f));
        Vector3f by = new Vector3f(byRaw);
        by.sub(new Vector3f(bx).mul(by.dot(bx)));
        Vector3f by2 = normalizeOrFallback(by, pickPerpendicular(bx));
        Vector3f bz = normalizeOrFallback(new Vector3f(bx).cross(by2), normalizeOrFallback(bzRaw, new Vector3f(0.0f, 0.0f, 1.0f)));
        float handedness = bxRaw.cross(new Vector3f(byRaw)).dot(bzRaw);
        if (Float.isFinite(handedness) && handedness < 0.0f) {
            bz.negate();
        }
        float x = vector.x;
        float y = vector.y;
        float z = vector.z;
        vector.set((bx.x * x) + (by2.x * y) + (bz.x * z), (bx.y * x) + (by2.y * y) + (bz.y * z), (bx.z * x) + (by2.z * y) + (bz.z * z));
    }

    private static Vector3f normalizeOrFallback(Vector3f value, Vector3f fallback) {
        Vector3f out = value == null ? new Vector3f() : new Vector3f(value);
        if (out.lengthSquared() > 1.0E-12f && Float.isFinite(out.x) && Float.isFinite(out.y) && Float.isFinite(out.z)) {
            out.normalize();
            return out;
        }
        if (fallback != null && fallback.lengthSquared() > 1.0E-12f) {
            return new Vector3f(fallback).normalize();
        }
        return new Vector3f(1.0f, 0.0f, 0.0f);
    }

    private static Vector3f pickPerpendicular(Vector3f axis) {
        Vector3f vector3f;
        if (axis == null) {
            return new Vector3f(0.0f, 1.0f, 0.0f);
        }
        if (Math.abs(axis.y) < 0.9f) {
            vector3f = new Vector3f(0.0f, 1.0f, 0.0f);
        } else {
            vector3f = new Vector3f(1.0f, 0.0f, 0.0f);
        }
        Vector3f base = vector3f;
        Vector3f perp = new Vector3f(base).sub(new Vector3f(axis).mul(base.dot(axis)));
        if (perp.lengthSquared() <= 1.0E-12f) {
            return new Vector3f(0.0f, 0.0f, 1.0f);
        }
        return perp.normalize();
    }

    private static Matrix4f toLocalBoneTransform(Matrix4f boneTransform, Matrix4f invRoot) {
        if (boneTransform == null) {
            return null;
        }
        if (invRoot == null) {
            return new Matrix4f(boneTransform);
        }
        return new Matrix4f(invRoot).mul(boneTransform);
    }

    private static String normalizeSpace(String value, boolean worldDefault) {
        if (value == null || value.isBlank()) {
            return worldDefault ? "world" : "bone";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("world".equals(normalized) || "camera".equals(normalized) || "bone".equals(normalized)) {
            return normalized;
        }
        return worldDefault ? "world" : "bone";
    }

    private static class_243 resolveHandFallback(class_310 client, class_746 player, class_1306 arm) {
        class_243 cameraBased = resolveCameraHandFallback(client, arm);
        if (cameraBased != null) {
            return cameraBased;
        }
        return resolvePlayerHandFallback(player, arm);
    }

    private static class_243 resolvePlayerHandFallback(class_746 player, class_1306 arm) {
        class_243 right;
        class_243 eye = player.method_5836(1.0f);
        class_243 forward = player.method_5828(1.0f).method_1029();
        class_243 upWorld = new class_243(0.0d, 1.0d, 0.0d);
        class_243 right2 = upWorld.method_1036(forward);
        if (right2.method_1027() < 1.0E-6d) {
            right = new class_243(arm == class_1306.field_6183 ? 1.0d : -1.0d, 0.0d, 0.0d);
        } else {
            right = right2.method_1029();
        }
        class_243 up = forward.method_1036(right).method_1029();
        return eye.method_1019(forward.method_1021(0.45d)).method_1019(up.method_1021(-0.25d)).method_1019(right.method_1021(arm == class_1306.field_6183 ? PARTICLE_OCCLUSION_ENTRY_MARGIN : -0.22d));
    }

    private static class_243 resolveCameraHandFallback(class_310 client, class_1306 arm) {
        class_4184 camera;
        if (client == null || client.field_1773 == null || arm == null || (camera = client.field_1773.method_19418()) == null || camera.method_19326() == null || camera.method_23767() == null) {
            return null;
        }
        class_243 cameraPos = camera.method_19326();
        Quaternionf rot = camera.method_23767();
        Vector3f rightV = new Vector3f(1.0f, 0.0f, 0.0f).rotate(rot);
        Vector3f upV = new Vector3f(0.0f, 1.0f, 0.0f).rotate(rot);
        Vector3f fwdV = new Vector3f(0.0f, 0.0f, 1.0f).rotate(rot);
        class_243 right = new class_243(rightV.x, rightV.y, rightV.z);
        class_243 up = new class_243(upV.x, upV.y, upV.z);
        class_243 forward = new class_243(fwdV.x, fwdV.y, fwdV.z);
        return cameraPos.method_1019(forward.method_1021(0.45d)).method_1019(up.method_1021(-0.25d)).method_1019(right.method_1021(arm == class_1306.field_6183 ? PARTICLE_OCCLUSION_ENTRY_MARGIN : -0.22d));
    }

    private static class_243 applyCameraBasis(class_310 client, Vector3f local) {
        class_4184 camera;
        if (client == null || client.field_1773 == null || local == null || (camera = client.field_1773.method_19418()) == null || camera.method_23767() == null) {
            return null;
        }
        Quaternionf rot = camera.method_23767();
        Vector3f rightV = new Vector3f(1.0f, 0.0f, 0.0f).rotate(rot);
        Vector3f upV = new Vector3f(0.0f, 1.0f, 0.0f).rotate(rot);
        Vector3f fwdV = new Vector3f(0.0f, 0.0f, 1.0f).rotate(rot);
        class_243 right = new class_243(rightV.x, rightV.y, rightV.z);
        class_243 up = new class_243(upV.x, upV.y, upV.z);
        class_243 forward = new class_243(fwdV.x, fwdV.y, fwdV.z);
        return right.method_1021(local.x).method_1019(up.method_1021(local.y)).method_1019(forward.method_1021(local.z));
    }

    private static class_243 cameraSpaceToWorld(class_310 client, Vector3f cameraSpacePos, class_1306 arm, class_243 fallback) {
        class_4184 camera;
        if (client == null || client.field_1773 == null || cameraSpacePos == null || (camera = client.field_1773.method_19418()) == null) {
            return null;
        }
        class_243 cameraPos = camera.method_19326();
        Quaternionf rotation = camera.method_23767();
        if (cameraPos == null || rotation == null) {
            return null;
        }
        Vector3f worldOffset = transformByCalibration(cameraSpacePos, camera, arm, fallback);
        return cameraPos.method_1031(worldOffset.x, worldOffset.y, worldOffset.z);
    }

    private static class_243 resolveRenderRootBase(class_310 client, class_1306 arm, class_243 fallback) {
        if (arm == null) {
            return fallback == null ? class_243.field_1353 : fallback;
        }
        Matrix4f root = EXACT_INHAND_GHOST_ROOT.get(arm);
        if (root == null) {
            root = RENDER_ROOT.get(arm);
        }
        if (root == null) {
            return fallback == null ? class_243.field_1353 : fallback;
        }
        Vector3f rootPos = root.getTranslation(new Vector3f());
        class_4184 camera = (client == null || client.field_1773 == null) ? null : client.field_1773.method_19418();
        class_243 cameraPos = camera == null ? null : camera.method_19326();
        Quaternionf cameraRot = camera == null ? null : camera.method_23767();
        boolean shaderPackInUse = isShaderPackInUse();
        class_243 resolved = releasePointFromRenderSpace(cameraPos, rootPos, cameraRot, shaderPackInUse);
        if (resolved == null || !isFinite(resolved)) {
            resolved = cameraSpaceToWorld(client, rootPos, arm, fallback);
        }
        if (resolved == null || !isFinite(resolved)) {
            return fallback == null ? class_243.field_1353 : fallback;
        }
        return resolved;
    }

    private static Vector3f ghostAnchorRenderOffsetForArm(class_1306 arm) {
        if (GHOST_ANCHOR_RENDER_OFFSET == null) {
            return new Vector3f();
        }
        Vector3f out = new Vector3f(GHOST_ANCHOR_RENDER_OFFSET);
        if (arm == class_1306.field_6182) {
            out.x = -out.x;
        }
        return out;
    }

    private static Vector3f ghostReleaseOffsetForRenderPos(class_1306 arm, Quaternionf cameraRot, boolean shaderPackInUse) {
        Vector3f raw = ghostAnchorRenderOffsetForArm(arm);
        if (raw == null) {
            return new Vector3f();
        }
        Vector3f out = new Vector3f(raw);
        if (!shaderPackInUse && cameraRot != null) {
            out.rotate(cameraRot);
        }
        return out;
    }

    private static Vector3f resolveRenderRootRenderDelta(class_1306 arm) {
        if (arm == null) {
            return null;
        }
        Matrix4f root = EXACT_INHAND_GHOST_ROOT.get(arm);
        if (root == null) {
            root = RENDER_ROOT.get(arm);
        }
        if (root == null) {
            return null;
        }
        Vector3f t = root.getTranslation(new Vector3f());
        if (Float.isFinite(t.x) && Float.isFinite(t.y) && Float.isFinite(t.z)) {
            return t;
        }
        return null;
    }

    private static class_243 cameraSpaceDirectionToWorld(class_310 client, Vector3f cameraSpaceDir, class_1306 arm, class_243 fallback) {
        class_4184 camera;
        if (client == null || client.field_1773 == null || cameraSpaceDir == null || (camera = client.field_1773.method_19418()) == null) {
            return null;
        }
        Quaternionf rotation = camera.method_23767();
        if (rotation == null) {
            return null;
        }
        Vector3f worldOffset = transformByCalibration(cameraSpaceDir, camera, arm, fallback);
        return new class_243(worldOffset.x, worldOffset.y, worldOffset.z);
    }

    private static Vector3f transformByCalibration(Vector3f source, class_4184 camera, class_1306 arm, class_243 fallback) {
        if (source == null) {
            return new Vector3f();
        }
        Quaternionf rotation = camera == null ? null : camera.method_23767();
        if (camera == null || rotation == null) {
            return new Vector3f(source);
        }
        return new Vector3f(source).rotate(rotation);
    }

    private static boolean isFinite(class_243 vec) {
        return vec != null && Double.isFinite(vec.field_1352) && Double.isFinite(vec.field_1351) && Double.isFinite(vec.field_1350);
    }

    private static boolean isFinite(Vector3f vec) {
        return vec != null && Float.isFinite(vec.x) && Float.isFinite(vec.y) && Float.isFinite(vec.z);
    }

    private static boolean isShaderPackInUse() {
        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApi.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Object result = irisApi.getMethod("isShaderPackInUse", new Class[0]).invoke(instance, new Object[0]);
            if (result instanceof Boolean) {
                Boolean b = (Boolean) result;
                if (b.booleanValue()) {
                    return true;
                }
            }
            return false;
        } catch (Throwable th) {
            try {
                Class<?> irisApi2 = Class.forName("net.coderbot.iris.api.v0.IrisApi");
                Object instance2 = irisApi2.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
                Object result2 = irisApi2.getMethod("isShaderPackInUse", new Class[0]).invoke(instance2, new Object[0]);
                if (result2 instanceof Boolean) {
                    Boolean b2 = (Boolean) result2;
                    if (b2.booleanValue()) {
                        return true;
                    }
                }
                return false;
            } catch (Throwable th2) {
                return false;
            }
        }
    }

    private static class_1921 emissiveOverlayRenderType(class_2960 texture, boolean shaderPackInUse, boolean blur, boolean clamp) {
        if (shaderPackInUse) {
            return class_1921.method_42600(texture);
        }
        return particleEmissiveOverlay(texture, blur, clamp);
    }

    private static class_1921 particleEmissiveOverlay(class_2960 texture, boolean blur, boolean clamp) {
        ParticleEmissiveKey key = new ParticleEmissiveKey(texture, blur, clamp);
        class_1921 cached = PARTICLE_EMISSIVE_OVERLAY_TYPES.get(key);
        if (cached != null) {
            return cached;
        }
        class_1921 created = tryCreateParticleEmissiveOverlay(texture, blur, clamp);
        if (created == null) {
            created = class_1921.method_42600(texture);
        }
        PARTICLE_EMISSIVE_OVERLAY_TYPES.put(key, created);
        return created;
    }

    private static RenderPipeline createParticleEmissiveOverlayPipeline() {
        try {
            Class<?> pipelinesClass = Class.forName("net.minecraft.client.renderer.RenderPipelines");
            Field snippetField = pipelinesClass.getDeclaredField("MATRICES_COLOR_FOG_LIGHT_DIR_SNIPPET");
            snippetField.setAccessible(true);
            Object snippet = snippetField.get(null);
            if (!(snippet instanceof RenderPipeline.Snippet)) {
                return class_10799.field_56907;
            }
            RenderPipeline.Snippet snippetValue = (RenderPipeline.Snippet) snippet;
            Method register = pipelinesClass.getDeclaredMethod("register", RenderPipeline.class);
            register.setAccessible(true);
            RenderPipeline pipeline = RenderPipeline.builder(new RenderPipeline.Snippet[]{snippetValue}).withLocation("pipeline/punchy_particle_emissive_overlay").withVertexShader("core/entity").withFragmentShader("core/entity").withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING").withSampler("Sampler0").withBlend(BlendFunction.TRANSLUCENT).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withCull(false).withVertexFormat(class_290.field_1580, VertexFormat.class_5596.field_27382).build();
            Object result = register.invoke(null, pipeline);
            if (!(result instanceof RenderPipeline)) {
                return class_10799.field_56907;
            }
            RenderPipeline value = (RenderPipeline) result;
            return value;
        } catch (Throwable th) {
            return class_10799.field_56907;
        }
    }

    private static class_1921 tryCreateParticleEmissiveOverlay(class_2960 texture, boolean blur, boolean clamp) {
        try {
            Class<?> compositeStateClass = Class.forName("net.minecraft.client.renderer.RenderType$CompositeState");
            Method builderMethod = compositeStateClass.getDeclaredMethod("builder", new Class[0]);
            builderMethod.setAccessible(true);
            Object builder = builderMethod.invoke(null, new Object[0]);
            Class<?> builderClass = builder.getClass();
            Class<?> emptyTextureStateClass = Class.forName("net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard");
            Class<?> overlayStateClass = Class.forName("net.minecraft.client.renderer.RenderStateShard$OverlayStateShard");
            Class<?> layeringStateClass = Class.forName("net.minecraft.client.renderer.RenderStateShard$LayeringStateShard");
            Constructor<?> textureCtor = emptyTextureStateClass.getDeclaredConstructor(Runnable.class, Runnable.class);
            textureCtor.setAccessible(true);
            Runnable setupTexture = () -> {
                class_1044 boundTexture;
                class_310 client = class_310.method_1551();
                if (client == null || (boundTexture = client.method_1531().method_4619(texture)) == null) {
                    return;
                }
                boundTexture.method_65923(blur ? class_9851.field_52394 : class_9851.field_52395, blur);
                boundTexture.method_65924(clamp);
                RenderSystem.setShaderTexture(0, boundTexture.method_68004());
            };
            Object textureState = textureCtor.newInstance(setupTexture, () -> {
            });
            Method setTextureState = builderClass.getDeclaredMethod("setTextureState", emptyTextureStateClass);
            setTextureState.setAccessible(true);
            Object builder2 = setTextureState.invoke(builder, textureState);
            Object overlayState = getRenderStateShardField("NO_OVERLAY");
            if (overlayState != null) {
                Method setOverlayState = builderClass.getDeclaredMethod("setOverlayState", overlayStateClass);
                setOverlayState.setAccessible(true);
                builder2 = setOverlayState.invoke(builder2, overlayState);
            }
            Object layeringState = getRenderStateShardField("VIEW_OFFSET_Z_LAYERING");
            if (layeringState != null) {
                Method setLayeringState = builderClass.getDeclaredMethod("setLayeringState", layeringStateClass);
                setLayeringState.setAccessible(true);
                builder2 = setLayeringState.invoke(builder2, layeringState);
            }
            Method createCompositeState = builderClass.getDeclaredMethod("createCompositeState", Boolean.TYPE);
            createCompositeState.setAccessible(true);
            Object compositeState = createCompositeState.invoke(builder2, true);
            Method create = class_1921.class.getDeclaredMethod("create", String.class, Integer.TYPE, Boolean.TYPE, Boolean.TYPE, RenderPipeline.class, compositeStateClass);
            create.setAccessible(true);
            Object renderType = create.invoke(null, "punchy_particle_emissive_overlay", 1536, true, true, PARTICLE_EMISSIVE_OVERLAY_PIPELINE, compositeState);
            if (!(renderType instanceof class_1921)) {
                return null;
            }
            class_1921 value = (class_1921) renderType;
            return value;
        } catch (Throwable th) {
            return null;
        }
    }

    private static Object getRenderStateShardField(String name) {
        try {
            Class<?> renderStateClass = Class.forName("net.minecraft.client.renderer.RenderStateShard");
            Field field = renderStateClass.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable th) {
            return null;
        }
    }

    private static float emissionToBaseIntensity(int emission) {
        if (emission <= 0) {
            return 0.0f;
        }
        if (emission <= 15) {
            return emission / 15.0f;
        }
        return 1.0f;
    }

    private static float emissionToExtraGlow(int emission) {
        if (emission <= 15) {
            return 0.0f;
        }
        return Math.min(1.0f, (emission - 15) / 85.0f);
    }

    private static float estimateUniformScale(Matrix4f matrix) {
        if (matrix == null) {
            return 1.0f;
        }
        Vector3f x = new Vector3f(matrix.m00(), matrix.m01(), matrix.m02());
        Vector3f y = new Vector3f(matrix.m10(), matrix.m11(), matrix.m12());
        Vector3f z = new Vector3f(matrix.m20(), matrix.m21(), matrix.m22());
        float sx = x.length();
        float sy = y.length();
        float sz = z.length();
        return Math.max(EPS, ((sx + sy) + sz) / 3.0f);
    }

    private static String resolveRenderBoneName(Map<String, Matrix4f> renderBoneTransforms, String requested, class_1306 arm) {
        if (renderBoneTransforms != null && !renderBoneTransforms.isEmpty() && requested != null && !requested.isBlank()) {
            if (renderBoneTransforms.containsKey(requested)) {
                return requested;
            }
            String lowered = requested.toLowerCase(Locale.ROOT);
            for (String key : renderBoneTransforms.keySet()) {
                if (key != null && key.toLowerCase(Locale.ROOT).equals(lowered)) {
                    return key;
                }
            }
            if (lowered.contains("grip")) {
                String side = arm == class_1306.field_6183 ? "right" : "left";
                Iterator<String> it = renderBoneTransforms.keySet().iterator();
                while (it.hasNext()) {
                    String key2 = it.next();
                    String n = key2 == null ? "" : key2.toLowerCase(Locale.ROOT);
                    if (n.contains("grip") && n.contains(side)) {
                        return key2;
                    }
                }
            }
            if (lowered.contains("item")) {
                String side2 = arm == class_1306.field_6183 ? "right" : "left";
                Iterator<String> it2 = renderBoneTransforms.keySet().iterator();
                while (it2.hasNext()) {
                    String key3 = it2.next();
                    String n2 = key3 == null ? "" : key3.toLowerCase(Locale.ROOT);
                    if (n2.contains("item") && n2.contains(side2)) {
                        return key3;
                    }
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private static String resolveBoneName(Map<String, class_630> parts, String requested, class_1306 arm) {
        if (requested != null && !requested.isBlank()) {
            if (parts.containsKey(requested)) {
                return requested;
            }
            String lowered = requested.toLowerCase(Locale.ROOT);
            for (String key : parts.keySet()) {
                if (key != null && key.toLowerCase(Locale.ROOT).equals(lowered)) {
                    return key;
                }
            }
            if (lowered.contains("grip")) {
                String side = arm == class_1306.field_6183 ? "right" : "left";
                Iterator<String> it = parts.keySet().iterator();
                while (it.hasNext()) {
                    String key2 = it.next();
                    String n = key2 == null ? "" : key2.toLowerCase(Locale.ROOT);
                    if (n.contains("grip") && n.contains(side)) {
                        return key2;
                    }
                }
            }
            if (lowered.contains("item")) {
                String side2 = arm == class_1306.field_6183 ? "right" : "left";
                Iterator<String> it2 = parts.keySet().iterator();
                while (it2.hasNext()) {
                    String key3 = it2.next();
                    String n2 = key3 == null ? "" : key3.toLowerCase(Locale.ROOT);
                    if (n2.contains("item") && n2.contains(side2)) {
                        return key3;
                    }
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private static Matrix4f computeBoneWorld(Map<String, class_630> parts, String boneName) {
        String byIdentity;
        if (parts == null || boneName == null) {
            return null;
        }
        Map<class_630, String> reverse = new IdentityHashMap<>();
        for (Map.Entry<String, class_630> entry : parts.entrySet()) {
            if (entry.getValue() != null) {
                reverse.put(entry.getValue(), entry.getKey());
            }
        }
        Map<String, String> parentByChild = new HashMap<>();
        for (Map.Entry<String, class_630> entry2 : parts.entrySet()) {
            String parentName = entry2.getKey();
            ModelPartAccessor modelPartAccessor = (class_630) entry2.getValue();
            if (modelPartAccessor != null && (modelPartAccessor instanceof ModelPartAccessor)) {
                ModelPartAccessor accessor = modelPartAccessor;
                for (Map.Entry<String, class_630> child : accessor.punchy$getChildren().entrySet()) {
                    String childName = child.getKey();
                    if (!parts.containsKey(childName) && (byIdentity = reverse.get(child.getValue())) != null) {
                        childName = byIdentity;
                    }
                    if (parts.containsKey(childName)) {
                        parentByChild.put(childName, parentName);
                    }
                }
            }
        }
        Map<String, Matrix4f> cache = new HashMap<>();
        return computeBoneWorldRecursive(parts, parentByChild, boneName, cache);
    }

    private static Matrix4f computeBoneWorldRecursive(Map<String, class_630> parts, Map<String, String> parentByChild, String boneName, Map<String, Matrix4f> cache) {
        Matrix4f world;
        Matrix4f parentWorld;
        Matrix4f cached = cache.get(boneName);
        if (cached != null) {
            return new Matrix4f(cached);
        }
        class_630 part = parts.get(boneName);
        if (part == null) {
            return null;
        }
        Matrix4f local = ModelPartMath.getLocalMatrix(part);
        String parent = parentByChild.get(boneName);
        if (parent != null && !parent.equals(boneName) && (parentWorld = computeBoneWorldRecursive(parts, parentByChild, parent, cache)) != null) {
            world = new Matrix4f(parentWorld).mul(local);
        } else {
            world = local;
        }
        cache.put(boneName, new Matrix4f(world));
        return world;
    }

    private static float resolveEmitDurationSeconds(ParticleScriptSpec spec, AnimationClip clip, float timelineTime) {
        if (spec == null || spec.ratePerSecond <= 0.0f) {
            return 0.0f;
        }
        if (spec.emitFullAnimation && clip != null && clip.getLength() > EPS) {
            return clip.getLength();
        }
        if (spec.emitForSeconds > 0.0f) {
            return spec.emitForSeconds;
        }
        if (clip == null) {
            return 0.0f;
        }
        float length = clip.getLength();
        if (length <= EPS) {
            return 0.0f;
        }
        float keyTime = class_3532.method_15363(timelineTime, 0.0f, length);
        float remaining = length - keyTime;
        if (remaining <= EPS && clip.isLoop()) {
            remaining = length;
        }
        return Math.max(0.0f, remaining);
    }

    private static class_2394 resolveParticle(String typeId) {
        class_2960 id;
        if (typeId != null && !typeId.isBlank() && (id = class_2960.method_12829(typeId.trim())) != null) {
            class_2394 class_2394Var = (class_2396) class_7923.field_41180.method_17966(id).orElse(null);
            if (class_2394Var instanceof class_2394) {
                class_2394 options = class_2394Var;
                return options;
            }
        }
        return class_2398.field_11205;
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private static ParticleScriptSpec parseParticleSpec(String effect) {
        int eq;
        if (effect == null || effect.isBlank()) {
            return null;
        }
        String normalized = effect.trim();
        if (!normalized.startsWith("particle(") || !normalized.endsWith(")")) {
            return null;
        }
        String body = normalized.substring("particle(".length(), normalized.length() - 1).trim();
        if (body.isEmpty()) {
            return null;
        }
        ParticleScriptSpec spec = new ParticleScriptSpec();
        String[] tokens = body.split(",");
        for (String token : tokens) {
            String kv = token.trim();
            if (!kv.isEmpty() && (eq = kv.indexOf(61)) > 0 && eq < kv.length() - 1) {
                String key = kv.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String value = kv.substring(eq + 1).trim();
                byte b = -1;
                switch (key.hashCode()) {
                    case -2145924580:
                        if (key.equals("light_percent")) {
                            b = 51;
                        }
                        break;
                    case -2084558552:
                        if (key.equals("target_type")) {
                            b = 4;
                        }
                        break;
                    case -2076913094:
                        if (key.equals("timecode")) {
                            b = 161;
                        }
                        break;
                    case -2076267817:
                        if (key.equals("face_bounds")) {
                            b = 124;
                        }
                        break;
                    case -2070410554:
                        if (key.equals("surface_region")) {
                            b = 126;
                        }
                        break;
                    case -1992012396:
                        if (key.equals("duration")) {
                            b = 15;
                        }
                        break;
                    case -1949141387:
                        if (key.equals("emit_ignore_model_rotation")) {
                            b = 76;
                        }
                        break;
                    case -1928611755:
                        if (key.equals("emit_full_anim")) {
                            b = 24;
                        }
                        break;
                    case -1927217219:
                        if (key.equals("bright_at_dark")) {
                            b = 43;
                        }
                        break;
                    case -1858012633:
                        if (key.equals("ignore_model_rotation_for_emit")) {
                            b = 74;
                        }
                        break;
                    case -1853231955:
                        if (key.equals("surface")) {
                            b = 125;
                        }
                        break;
                    case -1820119341:
                        if (key.equals("trigger_attacking")) {
                            b = 146;
                        }
                        break;
                    case -1804068774:
                        if (key.equals("swiming")) {
                            b = 139;
                        }
                        break;
                    case -1771845996:
                        if (key.equals("fake_world_local")) {
                            b = 79;
                        }
                        break;
                    case -1677551167:
                        if (key.equals("full_anim")) {
                            b = 26;
                        }
                        break;
                    case -1627858154:
                        if (key.equals("face_region")) {
                            b = 123;
                        }
                        break;
                    case -1580821023:
                        if (key.equals("ignore_model_rotation")) {
                            b = 75;
                        }
                        break;
                    case -1546254366:
                        if (key.equals("release_preview_layer")) {
                            b = 84;
                        }
                        break;
                    case -1492292349:
                        if (key.equals("full_day_light")) {
                            b = 38;
                        }
                        break;
                    case -1481614678:
                        if (key.equals("full_bright")) {
                            b = 37;
                        }
                        break;
                    case -1469102045:
                        if (key.equals("lock_in_model")) {
                            b = 89;
                        }
                        break;
                    case -1447318938:
                        if (key.equals("animation_name")) {
                            b = 157;
                        }
                        break;
                    case -1437276973:
                        if (key.equals("emissive_clamp")) {
                            b = 62;
                        }
                        break;
                    case -1417816805:
                        if (key.equals("texture")) {
                            b = 68;
                        }
                        break;
                    case -1407259064:
                        if (key.equals("attack")) {
                            b = 148;
                        }
                        break;
                    case -1388925997:
                        if (key.equals("biomes")) {
                            b = 166;
                        }
                        break;
                    case -1386164858:
                        if (key.equals("blocks")) {
                            b = 117;
                        }
                        break;
                    case -1373394254:
                        if (key.equals("trigger_falling_min_blocks")) {
                            b = 142;
                        }
                        break;
                    case -1361636556:
                        if (key.equals("chance")) {
                            b = 8;
                        }
                        break;
                    case -1354448893:
                        if (key.equals("emission_clamp")) {
                            b = 63;
                        }
                        break;
                    case -1348071903:
                        if (key.equals("world_layer_behind_local")) {
                            b = 82;
                        }
                        break;
                    case -1334338611:
                        if (key.equals("trigger_animation_name")) {
                            b = 156;
                        }
                        break;
                    case -1310959531:
                        if (key.equals("mining_blocks")) {
                            b = 119;
                        }
                        break;
                    case -1289215010:
                        if (key.equals("mining_block")) {
                            b = 118;
                        }
                        break;
                    case -1251149991:
                        if (key.equals("trigger_biome")) {
                            b = 167;
                        }
                        break;
                    case -1251060922:
                        if (key.equals("trigger_block")) {
                            b = 114;
                        }
                        break;
                    case -1241743849:
                        if (key.equals("trigger_logic")) {
                            b = 170;
                        }
                        break;
                    case -1215271696:
                        if (key.equals("mining_block_state")) {
                            b = 122;
                        }
                        break;
                    case -1083815289:
                        if (key.equals("falling")) {
                            b = 141;
                        }
                        break;
                    case -1074038704:
                        if (key.equals("mining")) {
                            b = 154;
                        }
                        break;
                    case -1068484150:
                        if (key.equals("world_preview_behind_local")) {
                            b = 81;
                        }
                        break;
                    case -1068318794:
                        if (key.equals("motion")) {
                            b = 94;
                        }
                        break;
                    case -1065205499:
                        if (key.equals("interval_seconds")) {
                            b = 21;
                        }
                        break;
                    case -1019779949:
                        if (key.equals("offset")) {
                            b = 9;
                        }
                        break;
                    case -991378413:
                        if (key.equals("min_blocks")) {
                            b = 145;
                        }
                        break;
                    case -983895235:
                        if (key.equals("motion_space")) {
                            b = 97;
                        }
                        break;
                    case -938578798:
                        if (key.equals("radius")) {
                            b = 100;
                        }
                        break;
                    case -903802626:
                        if (key.equals("motion_center")) {
                            b = 109;
                        }
                        break;
                    case -895684237:
                        if (key.equals("spread")) {
                            b = 10;
                        }
                        break;
                    case -880905839:
                        if (key.equals("target")) {
                            b = 2;
                        }
                        break;
                    case -870605675:
                        if (key.equals("animation_timecode")) {
                            b = 160;
                        }
                        break;
                    case -863202164:
                        if (key.equals("motion_mode")) {
                            b = 96;
                        }
                        break;
                    case -817972131:
                        if (key.equals("min_fall_blocks")) {
                            b = 144;
                        }
                        break;
                    case -741077248:
                        if (key.equals("trigger_falling")) {
                            b = 140;
                        }
                        break;
                    case -688089189:
                        if (key.equals("glow_target_menu")) {
                            b = 50;
                        }
                        break;
                    case -681218649:
                        if (key.equals("triggers_mode")) {
                            b = 171;
                        }
                        break;
                    case -593327494:
                        if (key.equals("alpha_end")) {
                            b = 33;
                        }
                        break;
                    case -542876180:
                        if (key.equals("space_offset")) {
                            b = 70;
                        }
                        break;
                    case -478894950:
                        if (key.equals("attacking")) {
                            b = 147;
                        }
                        break;
                    case -394481466:
                        if (key.equals("on_animation_name")) {
                            b = 155;
                        }
                        break;
                    case -363042231:
                        if (key.equals("fullbright")) {
                            b = 36;
                        }
                        break;
                    case -310301674:
                        if (key.equals("pinned_in_model")) {
                            b = 93;
                        }
                        break;
                    case -284721879:
                        if (key.equals("emissive_strength")) {
                            b = 55;
                        }
                        break;
                    case -283391285:
                        if (key.equals("use_item")) {
                            b = 151;
                        }
                        break;
                    case -147380966:
                        if (key.equals("useitem")) {
                            b = 152;
                        }
                        break;
                    case -130943942:
                        if (key.equals("trigger_biomes")) {
                            b = 164;
                        }
                        break;
                    case -128182803:
                        if (key.equals("trigger_blocks")) {
                            b = 115;
                        }
                        break;
                    case -107376172:
                        if (key.equals("full_animation")) {
                            b = 27;
                        }
                        break;
                    case -103677777:
                        if (key.equals("movement")) {
                            b = 95;
                        }
                        break;
                    case -91442467:
                        if (key.equals("swimming")) {
                            b = 138;
                        }
                        break;
                    case -52151785:
                        if (key.equals("landing")) {
                            b = 131;
                        }
                        break;
                    case 116:
                        if (key.equals("t")) {
                            b = 13;
                        }
                        break;
                    case 111219:
                        if (key.equals("pps")) {
                            b = 18;
                        }
                        break;
                    case 112845:
                        if (key.equals("rgb")) {
                            b = 67;
                        }
                        break;
                    case 3008417:
                        if (key.equals("axis")) {
                            b = 104;
                        }
                        break;
                    case 3027047:
                        if (key.equals("blur")) {
                            b = 61;
                        }
                        break;
                    case 3029700:
                        if (key.equals("bone")) {
                            b = 1;
                        }
                        break;
                    case 3091764:
                        if (key.equals("drag")) {
                            b = 34;
                        }
                        break;
                    case 3105774:
                        if (key.equals("ease")) {
                            b = 12;
                        }
                        break;
                    case 3135069:
                        if (key.equals("face")) {
                            b = 113;
                        }
                        break;
                    case 3273774:
                        if (key.equals("jump")) {
                            b = 129;
                        }
                        break;
                    case 3321596:
                        if (key.equals("life")) {
                            b = 28;
                        }
                        break;
                    case 3493088:
                        if (key.equals("rate")) {
                            b = 16;
                        }
                        break;
                    case 3530753:
                        if (key.equals("size")) {
                            b = 30;
                        }
                        break;
                    case 3560187:
                        if (key.equals("tint")) {
                            b = 65;
                        }
                        break;
                    case 3575610:
                        if (key.equals("type")) {
                            b = 0;
                        }
                        break;
                    case 88584641:
                        if (key.equals("trigger_sprinting")) {
                            b = 135;
                        }
                        break;
                    case 92909918:
                        if (key.equals("alpha")) {
                            b = 32;
                        }
                        break;
                    case 93743264:
                        if (key.equals("biome")) {
                            b = 168;
                        }
                        break;
                    case 93832333:
                        if (key.equals("block")) {
                            b = 116;
                        }
                        break;
                    case 94103840:
                        if (key.equals("burst")) {
                            b = 7;
                        }
                        break;
                    case 94742715:
                        if (key.equals("clamp")) {
                            b = 64;
                        }
                        break;
                    case 94842723:
                        if (key.equals("color")) {
                            b = 66;
                        }
                        break;
                    case 94851343:
                        if (key.equals("count")) {
                            b = 6;
                        }
                        break;
                    case 102970646:
                        if (key.equals("light")) {
                            b = 53;
                        }
                        break;
                    case 106629499:
                        if (key.equals("phase")) {
                            b = 106;
                        }
                        break;
                    case 134199076:
                        if (key.equals("velocity_space")) {
                            b = 71;
                        }
                        break;
                    case 183943351:
                        if (key.equals("trigger_mining")) {
                            b = 153;
                        }
                        break;
                    case 195357075:
                        if (key.equals("trigger_useitem")) {
                            b = 150;
                        }
                        break;
                    case 277347197:
                        if (key.equals("emit_target")) {
                            b = 3;
                        }
                        break;
                    case 280523342:
                        if (key.equals("gravity")) {
                            b = 35;
                        }
                        break;
                    case 283837234:
                        if (key.equals("behind_local_bias")) {
                            b = 87;
                        }
                        break;
                    case 290586256:
                        if (key.equals("trigger_landing")) {
                            b = 130;
                        }
                        break;
                    case 356137946:
                        if (key.equals("triggers_logic")) {
                            b = 172;
                        }
                        break;
                    case 371920927:
                        if (key.equals("emission_blur")) {
                            b = 60;
                        }
                        break;
                    case 375532117:
                        if (key.equals("trigger_jump")) {
                            b = 128;
                        }
                        break;
                    case 375615434:
                        if (key.equals("trigger_mode")) {
                            b = 169;
                        }
                        break;
                    case 389741501:
                        if (key.equals("center_offset")) {
                            b = 108;
                        }
                        break;
                    case 395271345:
                        if (key.equals("bright_at_night")) {
                            b = 46;
                        }
                        break;
                    case 434138756:
                        if (key.equals("synthetic_world_local")) {
                            b = 78;
                        }
                        break;
                    case 438672253:
                        if (key.equals("pseudo_world_local")) {
                            b = 80;
                        }
                        break;
                    case 473068277:
                        if (key.equals("on_animation_timecode")) {
                            b = 158;
                        }
                        break;
                    case 553533533:
                        if (key.equals("world_preview_bias")) {
                            b = 85;
                        }
                        break;
                    case 626338739:
                        if (key.equals("world_behind_local")) {
                            b = 83;
                        }
                        break;
                    case 641644619:
                        if (key.equals("trigger_on_animation")) {
                            b = 163;
                        }
                        break;
                    case 686004792:
                        if (key.equals("light_pct")) {
                            b = 52;
                        }
                        break;
                    case 706070027:
                        if (key.equals("bright_in_dark")) {
                            b = 45;
                        }
                        break;
                    case 754431625:
                        if (key.equals("fulldaylight")) {
                            b = 39;
                        }
                        break;
                    case 781113864:
                        if (key.equals("release_preview_bias")) {
                            b = 88;
                        }
                        break;
                    case 801074776:
                        if (key.equals("trigger_block_state")) {
                            b = MAX_PARTICLES_PER_EVENT;
                        }
                        break;
                    case 847294365:
                        if (key.equals("size_end")) {
                            b = 31;
                        }
                        break;
                    case 857523403:
                        if (key.equals("falling_min_blocks")) {
                            b = 143;
                        }
                        break;
                    case 877282294:
                        if (key.equals("space_velocity")) {
                            b = 72;
                        }
                        break;
                    case 877510262:
                        if (key.equals("orbit_axis")) {
                            b = 103;
                        }
                        break;
                    case 886856912:
                        if (key.equals("night_fullbright")) {
                            b = 49;
                        }
                        break;
                    case 905297498:
                        if (key.equals("offset_space")) {
                            b = 69;
                        }
                        break;
                    case 910675439:
                        if (key.equals("emit_every_seconds")) {
                            b = 20;
                        }
                        break;
                    case 928896975:
                        if (key.equals("no_model_rot_emit")) {
                            b = 77;
                        }
                        break;
                    case 933918938:
                        if (key.equals("emit_ease")) {
                            b = 23;
                        }
                        break;
                    case 933948233:
                        if (key.equals("emit_face")) {
                            b = 111;
                        }
                        break;
                    case 960570313:
                        if (key.equals("lifetime")) {
                            b = 29;
                        }
                        break;
                    case 976722212:
                        if (key.equals("on_animation")) {
                            b = 162;
                        }
                        break;
                    case 999634519:
                        if (key.equals("emissive")) {
                            b = 57;
                        }
                        break;
                    case 999959133:
                        if (key.equals("emit_for")) {
                            b = 14;
                        }
                        break;
                    case 1030073065:
                        if (key.equals("biome_names")) {
                            b = 165;
                        }
                        break;
                    case 1118813539:
                        if (key.equals("walkgin")) {
                            b = 134;
                        }
                        break;
                    case 1118815609:
                        if (key.equals("walking")) {
                            b = 133;
                        }
                        break;
                    case 1146969012:
                        if (key.equals("world_layer_bias")) {
                            b = 86;
                        }
                        break;
                    case 1185348865:
                        if (key.equals("night_bright")) {
                            b = 47;
                        }
                        break;
                    case 1193443964:
                        if (key.equals("trigger_animation_timecode")) {
                            b = 159;
                        }
                        break;
                    case 1227525727:
                        if (key.equals("block_state")) {
                            b = 121;
                        }
                        break;
                    case 1229412117:
                        if (key.equals("lock_model")) {
                            b = 90;
                        }
                        break;
                    case 1255056305:
                        if (key.equals("emit_interval")) {
                            b = 19;
                        }
                        break;
                    case 1286118095:
                        if (key.equals("block_face")) {
                            b = 110;
                        }
                        break;
                    case 1294479844:
                        if (key.equals("vel_space")) {
                            b = 73;
                        }
                        break;
                    case 1429809032:
                        if (key.equals("sprinting")) {
                            b = 136;
                        }
                        break;
                    case 1446382918:
                        if (key.equals("orbit_phase")) {
                            b = 105;
                        }
                        break;
                    case 1449391313:
                        if (key.equals("orbit_space")) {
                            b = 98;
                        }
                        break;
                    case 1449395218:
                        if (key.equals("orbit_speed")) {
                            b = 101;
                        }
                        break;
                    case 1459352252:
                        if (key.equals("emit_target_type")) {
                            b = 5;
                        }
                        break;
                    case 1459608616:
                        if (key.equals("blur_glow_at_dark")) {
                            b = 42;
                        }
                        break;
                    case 1461553650:
                        if (key.equals("trigger_walking")) {
                            b = 132;
                        }
                        break;
                    case 1482541292:
                        if (key.equals("mining_face")) {
                            b = 112;
                        }
                        break;
                    case 1513636330:
                        if (key.equals("orbit_center")) {
                            b = 107;
                        }
                        break;
                    case 1542176682:
                        if (key.equals("angular_speed")) {
                            b = 102;
                        }
                        break;
                    case 1562619007:
                        if (key.equals("full_night_light")) {
                            b = 48;
                        }
                        break;
                    case 1675062446:
                        if (key.equals("daylight_fullbright")) {
                            b = 41;
                        }
                        break;
                    case 1751553394:
                        if (key.equals("trigger_use_item")) {
                            b = 149;
                        }
                        break;
                    case 1752834764:
                        if (key.equals("day_fullbright")) {
                            b = 40;
                        }
                        break;
                    case 1776147079:
                        if (key.equals("surface_bounds")) {
                            b = 127;
                        }
                        break;
                    case 1791316033:
                        if (key.equals("strength")) {
                            b = 58;
                        }
                        break;
                    case 1796387577:
                        if (key.equals("pin_in_model")) {
                            b = 92;
                        }
                        break;
                    case 1814092240:
                        if (key.equals("rate_per_sec")) {
                            b = 17;
                        }
                        break;
                    case 1935381497:
                        if (key.equals("emission_strength")) {
                            b = 56;
                        }
                        break;
                    case 1939071527:
                        if (key.equals("orbit_radius")) {
                            b = 99;
                        }
                        break;
                    case 1943502212:
                        if (key.equals("trigger_swimming")) {
                            b = 137;
                        }
                        break;
                    case 1948282816:
                        if (key.equals("emit_full_animation")) {
                            b = 25;
                        }
                        break;
                    case 1970624087:
                        if (key.equals("brightness_percent")) {
                            b = 54;
                        }
                        break;
                    case 2031817039:
                        if (key.equals("emissive_blur")) {
                            b = 59;
                        }
                        break;
                    case 2044955619:
                        if (key.equals("dark_bright")) {
                            b = 44;
                        }
                        break;
                    case 2056258852:
                        if (key.equals("locked_in_model")) {
                            b = 91;
                        }
                        break;
                    case 2130029463:
                        if (key.equals("spawn_every_seconds")) {
                            b = 22;
                        }
                        break;
                    case 2134260957:
                        if (key.equals("velocity")) {
                            b = 11;
                        }
                        break;
                }
                switch (b) {
                    case 0:
                        spec.typeId = value;
                        break;
                    case 1:
                        spec.boneName = value;
                        break;
                    case 2:
                    case 3:
                        applyTargetToken(spec, value);
                        break;
                    case 4:
                    case 5:
                        spec.targetType = normalizeTargetType(value, spec.targetType);
                        break;
                    case 6:
                    case 7:
                        spec.count = clampInt(parseInt(value, spec.count), 0, MAX_PARTICLES_PER_EVENT);
                        break;
                    case 8:
                        spec.chance = class_3532.method_15363(parseFloat(value, spec.chance), 0.0f, 1.0f);
                        break;
                    case 9:
                        spec.offset = parseVec3(value, spec.offset);
                        break;
                    case 10:
                        spec.spread = parseVec3(value, spec.spread);
                        break;
                    case 11:
                        spec.velocity = parseVec3(value, spec.velocity);
                        break;
                    case 12:
                        spec.ease = value.toLowerCase(Locale.ROOT);
                        break;
                    case 13:
                        spec.t = class_3532.method_15363(parseFloat(value, spec.t), 0.0f, 1.0f);
                        break;
                    case 14:
                    case 15:
                        spec.emitForSeconds = class_3532.method_15363(parseFloat(value, spec.emitForSeconds), 0.0f, 30.0f);
                        break;
                    case 16:
                    case 17:
                    case 18:
                        spec.ratePerSecond = class_3532.method_15363(parseFloat(value, spec.ratePerSecond), 0.0f, 120.0f);
                        break;
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                        spec.emitIntervalSeconds = class_3532.method_15363(parseFloat(value, spec.emitIntervalSeconds), 0.0f, 60.0f);
                        break;
                    case 23:
                        spec.emitEase = value.toLowerCase(Locale.ROOT);
                        break;
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                        spec.emitFullAnimation = parseBoolean(value, spec.emitFullAnimation);
                        break;
                    case 28:
                    case 29:
                        spec.lifetimeSeconds = class_3532.method_15363(parseFloat(value, spec.lifetimeSeconds), 0.05f, 60.0f);
                        break;
                    case 30:
                        spec.sizeStart = class_3532.method_15363(parseFloat(value, spec.sizeStart), 0.001f, 2.0f);
                        break;
                    case 31:
                        spec.sizeEnd = class_3532.method_15363(parseFloat(value, spec.sizeEnd), 0.0f, 2.0f);
                        break;
                    case 32:
                        spec.alphaStart = parseNormalizedAlpha(value, spec.alphaStart);
                        break;
                    case 33:
                        spec.alphaEnd = parseNormalizedAlpha(value, spec.alphaEnd);
                        break;
                    case 34:
                        spec.drag = class_3532.method_15363(parseFloat(value, spec.drag), 0.0f, 1.2f);
                        break;
                    case 35:
                        spec.gravity = class_3532.method_15363(parseFloat(value, spec.gravity), -2.0f, 2.0f);
                        break;
                    case 36:
                    case 37:
                        spec.fullBright = parseBoolean(value, spec.fullBright);
                        spec.fullBrightExplicit = true;
                        break;
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                        spec.fullDayLight = parseBoolean(value, spec.fullDayLight);
                        break;
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                    case 48:
                    case 49:
                        spec.brightAtDark = parseBoolean(value, spec.brightAtDark);
                        if (spec.brightAtDark && !spec.fullBrightExplicit) {
                            spec.fullBright = false;
                        }
                        break;
                    case 50:
                        spec.glowTargetMenu = parseBoolean(value, spec.glowTargetMenu);
                        break;
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                        spec.lightPercent = class_3532.method_15363(parseFloat(value, spec.lightPercent), 0.0f, 100.0f);
                        spec.lightPercentExplicit = true;
                        break;
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                        spec.emissiveStrength = clampInt(parseInt(value, spec.emissiveStrength), 0, 100);
                        spec.emissiveStrengthExplicit = true;
                        break;
                    case 59:
                    case 60:
                    case 61:
                        spec.emissiveBlur = parseBoolean(value, spec.emissiveBlur);
                        break;
                    case 62:
                    case 63:
                    case 64:
                        spec.emissiveClamp = parseBoolean(value, spec.emissiveClamp);
                        break;
                    case 65:
                    case 66:
                    case 67:
                        spec.tint = parseColorVec3(value, spec.tint);
                        break;
                    case 68:
                        spec.textureId = value;
                        break;
                    case 69:
                    case 70:
                        spec.offsetSpace = value;
                        break;
                    case 71:
                    case 72:
                    case 73:
                        spec.velocitySpace = value;
                        break;
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                        spec.ignoreModelRotationForEmissionPoint = parseBoolean(value, spec.ignoreModelRotationForEmissionPoint);
                        break;
                    case 78:
                    case 79:
                    case 80:
                        spec.syntheticWorldLocal = parseBoolean(value, spec.syntheticWorldLocal);
                        break;
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                        spec.worldPreviewBehindLocal = parseBoolean(value, spec.worldPreviewBehindLocal);
                        break;
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                        spec.worldPreviewBehindLocalBias = class_3532.method_15363(parseFloat(value, spec.worldPreviewBehindLocalBias), 0.0f, 0.05f);
                        break;
                    case 89:
                    case 90:
                    case 91:
                    case 92:
                    case 93:
                        spec.lockInModel = parseBoolean(value, spec.lockInModel);
                        break;
                    case 94:
                    case 95:
                    case 96:
                        spec.motionMode = parseMotionMode(value, spec.motionMode);
                        break;
                    case 97:
                    case 98:
                        spec.motionSpace = normalizeSpace(value, false);
                        break;
                    case 99:
                    case 100:
                        spec.orbitRadius = class_3532.method_15363(parseFloat(value, spec.orbitRadius), 0.0f, 4.0f);
                        break;
                    case 101:
                    case 102:
                        spec.orbitSpeed = class_3532.method_15363(parseFloat(value, spec.orbitSpeed), -20.0f, 20.0f);
                        break;
                    case 103:
                    case 104:
                        spec.orbitAxis = parseVec3(value, spec.orbitAxis);
                        break;
                    case 105:
                    case 106:
                        spec.orbitPhaseDegrees = parseFloat(value, spec.orbitPhaseDegrees);
                        break;
                    case 107:
                    case 108:
                    case 109:
                        spec.orbitCenterOffset = parseVec3(value, spec.orbitCenterOffset);
                        break;
                    case 110:
                    case 111:
                    case 112:
                    case 113:
                        spec.blockFaceSelector = normalizeBlockFaceSelector(value, spec.blockFaceSelector);
                        break;
                    case 114:
                    case 115:
                    case 116:
                    case 117:
                    case 118:
                    case 119:
                        spec.triggerBlockIds = parseNameSet(value);
                        break;
                    case MAX_PARTICLES_PER_EVENT /* 120 */:
                    case 121:
                    case 122:
                        spec.triggerBlockState = parseBlockStateRequirements(value);
                        break;
                    case 123:
                    case 124:
                    case 125:
                    case 126:
                    case 127:
                        applyFaceRegion(spec, value);
                        break;
                    case 128:
                    case 129:
                        spec.triggerJump = parseBoolean(value, spec.triggerJump);
                        break;
                    case 130:
                    case 131:
                        spec.triggerLanding = parseBoolean(value, spec.triggerLanding);
                        break;
                    case 132:
                    case 133:
                    case 134:
                        spec.triggerWalking = parseBoolean(value, spec.triggerWalking);
                        break;
                    case 135:
                    case 136:
                        spec.triggerSprinting = parseBoolean(value, spec.triggerSprinting);
                        break;
                    case 137:
                    case 138:
                    case 139:
                        spec.triggerSwimming = parseBoolean(value, spec.triggerSwimming);
                        break;
                    case 140:
                    case 141:
                        spec.triggerFalling = parseBoolean(value, spec.triggerFalling);
                        break;
                    case 142:
                    case 143:
                    case 144:
                    case 145:
                        spec.triggerFallingMinBlocks = class_3532.method_15363(parseFloat(value, spec.triggerFallingMinBlocks), 0.0f, 512.0f);
                        break;
                    case 146:
                    case 147:
                    case 148:
                        spec.triggerAttacking = parseBoolean(value, spec.triggerAttacking);
                        break;
                    case 149:
                    case 150:
                    case 151:
                    case 152:
                        spec.triggerUseItem = parseBoolean(value, spec.triggerUseItem);
                        break;
                    case 153:
                    case 154:
                        spec.triggerMining = parseBoolean(value, spec.triggerMining);
                        break;
                    case 155:
                    case 156:
                    case 157:
                        spec.triggerAnimationName = value;
                        break;
                    case 158:
                    case 159:
                    case 160:
                    case 161:
                        spec.triggerAnimationTimecode = Math.max(0.0f, parseFloat(value, spec.triggerAnimationTimecode));
                        break;
                    case 162:
                    case 163:
                        applyOnAnimationToken(spec, value);
                        break;
                    case 164:
                    case 165:
                    case 166:
                    case 167:
                    case 168:
                        spec.triggerBiomeNames = parseNameSet(value);
                        break;
                    case 169:
                    case 170:
                    case 171:
                    case 172:
                        spec.triggerModeAny = "any".equalsIgnoreCase(value) || "or".equalsIgnoreCase(value);
                        break;
                }
            }
        }
        spec.projectionMode = "raw";
        spec.renderMode = "custom";
        spec.debug = false;
        applyParticleVisualDefaults(spec);
        spec.lockStableKey = buildLockStableKey(spec);
        return spec;
    }

    private static String buildLockStableKey(ParticleScriptSpec spec) {
        if (spec == null) {
            return "lock";
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("type=").append(spec.typeId == null ? "" : spec.typeId.trim().toLowerCase(Locale.ROOT));
        sb.append("|bone=").append(spec.boneName == null ? "" : spec.boneName.trim().toLowerCase(Locale.ROOT));
        sb.append("|target=").append(spec.targetType == null ? "" : spec.targetType.trim().toLowerCase(Locale.ROOT));
        sb.append("|texture=").append(spec.textureId == null ? "" : spec.textureId.trim().toLowerCase(Locale.ROOT));
        sb.append("|count=").append(spec.count);
        sb.append("|lock=").append(spec.lockInModel);
        return Integer.toHexString(sb.toString().hashCode());
    }

    private static float applyEase(String name, float t) {
        float clamped = class_3532.method_15363(t, 0.0f, 1.0f);
        if (name == null || name.isBlank() || "linear".equals(name)) {
            return clamped;
        }
        switch (name) {
            case "ease_in_out_cubic":
                if (clamped >= 0.5f) {
                    break;
                } else {
                    break;
                }
                break;
        }
        return clamped;
    }

    private static Vector3f effectiveParticleOffset(ParticleScriptSpec spec, class_1306 arm, String offsetSpace) {
        return (spec == null || spec.offset == null) ? new Vector3f() : new Vector3f(spec.offset);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static float parseNormalizedAlpha(String value, float fallback) {
        float parsed = parseFloat(value, fallback);
        if (!Float.isFinite(parsed)) {
            return class_3532.method_15363(fallback, 0.0f, 1.0f);
        }
        if (parsed > 1.0f) {
            parsed /= 255.0f;
        }
        return class_3532.method_15363(parsed, 0.0f, 1.0f);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static String parseProjectionMode(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("rotation".equals(normalized) || "rotate".equals(normalized)) {
            normalized = "rotated";
        }
        if ("raw".equals(normalized) || "rotated".equals(normalized)) {
            return normalized;
        }
        return fallback;
    }

    private static String parseRenderMode(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("vanilla".equals(normalized)) {
            return "world";
        }
        if ("world".equals(normalized) || "custom".equals(normalized) || "hand".equals(normalized)) {
            return normalized;
        }
        return fallback;
    }

    private static String parseMotionMode(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("linear".equals(normalized) || "default".equals(normalized) || "drag".equals(normalized)) {
            return "linear";
        }
        if ("orbit".equals(normalized) || "orbital".equals(normalized) || "circle".equals(normalized)) {
            return "orbit";
        }
        return fallback;
    }

    private static Vector3f parseVec3(String text, Vector3f fallback) {
        if (text == null || text.isBlank()) {
            return new Vector3f(fallback);
        }
        String[] parts = text.trim().split(":");
        if (parts.length != 3) {
            return new Vector3f(fallback);
        }
        return new Vector3f(parseFloat(parts[0], fallback.x), parseFloat(parts[1], fallback.y), parseFloat(parts[2], fallback.z));
    }

    private static Vector3f parseColorVec3(String text, Vector3f fallback) {
        Vector3f base = fallback == null ? new Vector3f(1.0f, 1.0f, 1.0f) : new Vector3f(fallback);
        Vector3f parsed = parseVec3(text, base);
        if (parsed.x > 1.0f || parsed.y > 1.0f || parsed.z > 1.0f) {
            parsed.x /= 255.0f;
            parsed.y /= 255.0f;
            parsed.z /= 255.0f;
        }
        parsed.x = class_3532.method_15363(parsed.x, 0.0f, 1.0f);
        parsed.y = class_3532.method_15363(parsed.y, 0.0f, 1.0f);
        parsed.z = class_3532.method_15363(parsed.z, 0.0f, 1.0f);
        return parsed;
    }

    private static void applyParticleVisualDefaults(ParticleScriptSpec spec) {
        if (spec == null) {
            return;
        }
        if (spec.brightAtDark && !spec.fullBrightExplicit) {
            spec.fullBright = false;
        }
        if (isLeafLikeParticleType(spec.typeId)) {
            if (!spec.fullBrightExplicit) {
                spec.fullBright = false;
            }
            if (!spec.emissiveStrengthExplicit) {
                spec.emissiveStrength = 0;
            }
        }
    }

    private static boolean isLeafLikeParticleType(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return false;
        }
        String normalized = typeId.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("leaves") || normalized.contains("leaf");
    }

    private static Set<String> parseNameSet(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String[] parts = text.split("[|;]");
        Set<String> out = new HashSet<>();
        for (String part : parts) {
            if (part != null) {
                String normalized = part.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isBlank()) {
                    out.add(normalized);
                }
            }
        }
        return out.isEmpty() ? Set.of() : out;
    }

    private static Map<String, String> parseBlockStateRequirements(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }
        String[] parts = text.split("[|;]");
        Map<String, String> out = new HashMap<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                int sep = part.indexOf(58);
                if (sep <= 0 || sep >= part.length() - 1) {
                    sep = part.indexOf(61);
                }
                if (sep > 0 && sep < part.length() - 1) {
                    String key = part.substring(0, sep).trim();
                    String value = part.substring(sep + 1).trim();
                    if (!key.isBlank() && !value.isBlank()) {
                        out.put(key, value);
                    }
                }
            }
        }
        return out.isEmpty() ? Map.of() : out;
    }

    private static void applyFaceRegion(ParticleScriptSpec spec, String value) {
        if (spec == null || value == null || value.isBlank()) {
            return;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("hit".equals(normalized) || "impact".equals(normalized) || "exact".equals(normalized)) {
            spec.useImpactFacePoint = true;
            spec.faceMinU = 0.0f;
            spec.faceMinV = 0.0f;
            spec.faceMaxU = 1.0f;
            spec.faceMaxV = 1.0f;
            return;
        }
        String[] parts = value.trim().split(":");
        if (parts.length != 4) {
            return;
        }
        spec.useImpactFacePoint = false;
        float minU = class_3532.method_15363(parseFloat(parts[0], spec.faceMinU), 0.0f, 1.0f);
        float minV = class_3532.method_15363(parseFloat(parts[1], spec.faceMinV), 0.0f, 1.0f);
        float maxU = class_3532.method_15363(parseFloat(parts[2], spec.faceMaxU), 0.0f, 1.0f);
        float maxV = class_3532.method_15363(parseFloat(parts[3], spec.faceMaxV), 0.0f, 1.0f);
        spec.faceMinU = Math.min(minU, maxU);
        spec.faceMinV = Math.min(minV, maxV);
        spec.faceMaxU = Math.max(minU, maxU);
        spec.faceMaxV = Math.max(minV, maxV);
    }

    private static String normalizeBlockFaceSelector(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
        }
        return fallback;
    }

    private static void applyTargetToken(ParticleScriptSpec spec, String value) {
        if (spec == null || value == null || value.isBlank()) {
            return;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        spec.targetType = normalizeTargetType(normalized, spec.targetType);
    }

    private static void applyOnAnimationToken(ParticleScriptSpec spec, String value) {
        if (spec == null || value == null || value.isBlank()) {
            return;
        }
        String token = value.trim();
        int at = token.lastIndexOf(64);
        if (at > 0 && at < token.length() - 1) {
            spec.triggerAnimationName = token.substring(0, at).trim();
            spec.triggerAnimationTimecode = Math.max(0.0f, parseFloat(token.substring(at + 1).trim(), spec.triggerAnimationTimecode));
        } else {
            spec.triggerAnimationName = token;
        }
    }

    private static String normalizeTargetType(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
        }
        return fallback;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$ParticleScriptSpec.class */
    private static final class ParticleScriptSpec {
        private String typeId = "minecraft:crit";
        private String boneName = "";
        private String targetType = "bone";
        private int count = 1;
        private float chance = 1.0f;
        private Vector3f offset = new Vector3f();
        private Vector3f spread = new Vector3f();
        private Vector3f velocity = new Vector3f();
        private String ease = "linear";
        private float t = 1.0f;
        private float emitForSeconds = 0.0f;
        private float ratePerSecond = 0.0f;
        private String emitEase = "linear";
        private boolean emitFullAnimation = false;
        private float emitIntervalSeconds = 0.0f;
        private float lifetimeSeconds = 1.1f;
        private float sizeStart = 0.028f;
        private float sizeEnd = 0.006f;
        private float alphaStart = 0.95f;
        private float alphaEnd = 0.0f;
        private float drag = 0.93f;
        private float gravity = 0.0f;
        private boolean fullBright = true;
        private boolean fullDayLight = false;
        private boolean brightAtDark = false;
        private boolean glowTargetMenu = false;
        private float lightPercent = -1.0f;
        private int emissiveStrength = 100;
        private boolean emissiveBlur = false;
        private boolean emissiveClamp = true;
        private boolean fullBrightExplicit = false;
        private boolean lightPercentExplicit = false;
        private boolean emissiveStrengthExplicit = false;
        private Vector3f tint = null;
        private String textureId = "";
        private String offsetSpace = "bone";
        private String velocitySpace = "world";
        private boolean ignoreModelRotationForEmissionPoint = true;
        private boolean syntheticWorldLocal = true;
        private boolean worldPreviewBehindLocal = true;
        private float worldPreviewBehindLocalBias = ModelPartsParticleSystem.DEFAULT_WORLD_PREVIEW_BEHIND_LOCAL_BIAS;
        private boolean lockInModel = false;
        private String motionMode = "linear";
        private String motionSpace = "bone";
        private float orbitRadius = 0.0f;
        private float orbitSpeed = 0.0f;
        private Vector3f orbitAxis = new Vector3f(0.0f, 1.0f, 0.0f);
        private float orbitPhaseDegrees = 0.0f;
        private Vector3f orbitCenterOffset = new Vector3f();
        private String blockFaceSelector = "hit";
        private float faceMinU = 0.0f;
        private float faceMinV = 0.0f;
        private float faceMaxU = 1.0f;
        private float faceMaxV = 1.0f;
        private boolean useImpactFacePoint = false;
        private boolean triggerJump = false;
        private boolean triggerLanding = false;
        private boolean triggerWalking = false;
        private boolean triggerSprinting = false;
        private boolean triggerSwimming = false;
        private boolean triggerFalling = false;
        private float triggerFallingMinBlocks = 0.0f;
        private boolean triggerAttacking = false;
        private boolean triggerUseItem = false;
        private boolean triggerMining = false;
        private Set<String> triggerBlockIds = Set.of();
        private Map<String, String> triggerBlockState = Map.of();
        private String triggerAnimationName = "";
        private float triggerAnimationTimecode = -1.0f;
        private Set<String> triggerBiomeNames = Set.of();
        private boolean triggerModeAny = false;
        private String projectionMode = "raw";
        private boolean debug = false;
        private String renderMode = "custom";
        private String lockStableKey = "lock";

        private ParticleScriptSpec() {
        }

        private boolean hasAnimationTrigger() {
            return (this.triggerAnimationName == null || this.triggerAnimationName.isBlank() || this.triggerAnimationTimecode < 0.0f) ? false : true;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$FaceFrame.class */
    private static final class FaceFrame extends Record {
        private final class_243 corner;
        private final class_243 axisU;
        private final class_243 axisV;
        private final class_243 normal;

        private FaceFrame(class_243 corner, class_243 axisU, class_243 axisV, class_243 normal) {
            this.corner = corner;
            this.axisU = axisU;
            this.axisV = axisV;
            this.normal = normal;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, FaceFrame.class), FaceFrame.class, "corner;axisU;axisV;normal", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->corner:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->axisU:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->axisV:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->normal:Lnet/minecraft/class_243;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, FaceFrame.class), FaceFrame.class, "corner;axisU;axisV;normal", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->corner:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->axisU:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->axisV:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->normal:Lnet/minecraft/class_243;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, FaceFrame.class, Object.class), FaceFrame.class, "corner;axisU;axisV;normal", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->corner:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->axisU:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->axisV:Lnet/minecraft/class_243;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceFrame;->normal:Lnet/minecraft/class_243;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_243 corner() {
            return this.corner;
        }

        public class_243 axisU() {
            return this.axisU;
        }

        public class_243 axisV() {
            return this.axisV;
        }

        public class_243 normal() {
            return this.normal;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$FaceUv.class */
    private static final class FaceUv extends Record {
        private final double u;
        private final double v;

        private FaceUv(double u, double v) {
            this.u = u;
            this.v = v;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, FaceUv.class), FaceUv.class, "u;v", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceUv;->u:D", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceUv;->v:D").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, FaceUv.class), FaceUv.class, "u;v", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceUv;->u:D", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceUv;->v:D").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, FaceUv.class, Object.class), FaceUv.class, "u;v", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceUv;->u:D", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$FaceUv;->v:D").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public double u() {
            return this.u;
        }

        public double v() {
            return this.v;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent.class */
    private static final class PendingParticleEvent extends Record {
        private final ParticleScriptSpec spec;
        private final float timelineTime;
        private final float scale;

        private PendingParticleEvent(ParticleScriptSpec spec, float timelineTime, float scale) {
            this.spec = spec;
            this.timelineTime = timelineTime;
            this.scale = scale;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, PendingParticleEvent.class), PendingParticleEvent.class, "spec;timelineTime;scale", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->spec:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleScriptSpec;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->timelineTime:F", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->scale:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, PendingParticleEvent.class), PendingParticleEvent.class, "spec;timelineTime;scale", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->spec:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleScriptSpec;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->timelineTime:F", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->scale:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, PendingParticleEvent.class, Object.class), PendingParticleEvent.class, "spec;timelineTime;scale", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->spec:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleScriptSpec;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->timelineTime:F", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$PendingParticleEvent;->scale:F").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public ParticleScriptSpec spec() {
            return this.spec;
        }

        public float timelineTime() {
            return this.timelineTime;
        }

        public float scale() {
            return this.scale;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$TriggerContext.class */
    private static final class TriggerContext extends Record {
        private final boolean jump;
        private final boolean landing;
        private final boolean walking;
        private final boolean sprinting;
        private final boolean swimming;
        private final boolean falling;
        private final float fallDistance;
        private final boolean attacking;
        private final boolean useItem;
        private final boolean mining;
        private final long jumpEventId;
        private final long landingEventId;
        private final long attackEventId;
        private final long miningEventId;
        private final long useItemEventId;
        private static final TriggerContext EMPTY = new TriggerContext(false, false, false, false, false, false, 0.0f, false, false, false, ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS);

        private TriggerContext(boolean jump, boolean landing, boolean walking, boolean sprinting, boolean swimming, boolean falling, float fallDistance, boolean attacking, boolean useItem, boolean mining, long jumpEventId, long landingEventId, long attackEventId, long miningEventId, long useItemEventId) {
            this.jump = jump;
            this.landing = landing;
            this.walking = walking;
            this.sprinting = sprinting;
            this.swimming = swimming;
            this.falling = falling;
            this.fallDistance = fallDistance;
            this.attacking = attacking;
            this.useItem = useItem;
            this.mining = mining;
            this.jumpEventId = jumpEventId;
            this.landingEventId = landingEventId;
            this.attackEventId = attackEventId;
            this.miningEventId = miningEventId;
            this.useItemEventId = useItemEventId;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, TriggerContext.class), TriggerContext.class, "jump;landing;walking;sprinting;swimming;falling;fallDistance;attacking;useItem;mining;jumpEventId;landingEventId;attackEventId;miningEventId;useItemEventId", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->jump:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->landing:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->walking:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->sprinting:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->swimming:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->falling:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->fallDistance:F", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->attacking:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->useItem:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->mining:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->jumpEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->landingEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->attackEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->miningEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->useItemEventId:J").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, TriggerContext.class), TriggerContext.class, "jump;landing;walking;sprinting;swimming;falling;fallDistance;attacking;useItem;mining;jumpEventId;landingEventId;attackEventId;miningEventId;useItemEventId", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->jump:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->landing:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->walking:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->sprinting:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->swimming:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->falling:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->fallDistance:F", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->attacking:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->useItem:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->mining:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->jumpEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->landingEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->attackEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->miningEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->useItemEventId:J").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, TriggerContext.class, Object.class), TriggerContext.class, "jump;landing;walking;sprinting;swimming;falling;fallDistance;attacking;useItem;mining;jumpEventId;landingEventId;attackEventId;miningEventId;useItemEventId", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->jump:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->landing:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->walking:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->sprinting:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->swimming:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->falling:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->fallDistance:F", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->attacking:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->useItem:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->mining:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->jumpEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->landingEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->attackEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->miningEventId:J", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$TriggerContext;->useItemEventId:J").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public boolean jump() {
            return this.jump;
        }

        public boolean landing() {
            return this.landing;
        }

        public boolean walking() {
            return this.walking;
        }

        public boolean sprinting() {
            return this.sprinting;
        }

        public boolean swimming() {
            return this.swimming;
        }

        public boolean falling() {
            return this.falling;
        }

        public float fallDistance() {
            return this.fallDistance;
        }

        public boolean attacking() {
            return this.attacking;
        }

        public boolean useItem() {
            return this.useItem;
        }

        public boolean mining() {
            return this.mining;
        }

        public long jumpEventId() {
            return this.jumpEventId;
        }

        public long landingEventId() {
            return this.landingEventId;
        }

        public long attackEventId() {
            return this.attackEventId;
        }

        public long miningEventId() {
            return this.miningEventId;
        }

        public long useItemEventId() {
            return this.useItemEventId;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$TriggerRuntimeState.class */
    private static final class TriggerRuntimeState {
        private boolean initialized = false;
        private boolean lastOnGround = true;
        private boolean lastAttackKeyDown = false;
        private boolean lastSwinging = false;
        private int jumpGraceTicks = 0;
        private int landingGraceTicks = 0;
        private int attackGraceTicks = 0;
        private int miningGraceTicks = 0;
        private long jumpEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        private long landingEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        private long attackEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        private long miningEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;

        private TriggerRuntimeState() {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$PlaybackState.class */
    private static final class PlaybackState {
        private AnimationClip lastClip;
        private float lastTime;
        private long lastProcessedTick = Long.MIN_VALUE;

        private PlaybackState() {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$ActiveEmitter.class */
    private static final class ActiveEmitter {
        private final ParticleScriptSpec spec;
        private final int totalTicks;
        private float ageSeconds = 0.0f;
        private double accumulator = 0.0d;
        private long lastEmitNanos = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;

        private ActiveEmitter(ParticleScriptSpec spec, int totalTicks) {
            this.spec = spec;
            this.totalTicks = Math.max(1, totalTicks);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$PassiveEmitter.class */
    private static final class PassiveEmitter {
        private final ParticleScriptSpec spec;
        private float ageSeconds = 0.0f;
        private double accumulator = 0.0d;
        private long lastTick = Long.MIN_VALUE;
        private long lastJumpEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        private long lastLandingEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        private long lastAttackEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        private long lastMiningEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        private long lastUseItemEventId = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
        private long lastAnimationWindowSerial = Long.MIN_VALUE;
        private long lastEmitNanos = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;

        private PassiveEmitter(ParticleScriptSpec spec) {
            this.spec = spec;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$AnimationTriggerWindow.class */
    private static final class AnimationTriggerWindow {
        private long serial;
        private boolean active;
        private boolean clipChanged;
        private boolean looping;
        private String currentClipName = "";
        private float previousTimeSeconds;
        private float currentTimeSeconds;
        private float clipLengthSeconds;

        private AnimationTriggerWindow() {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$RenderParticle.class */
    private static final class RenderParticle {
        private class_243 position;
        private class_243 velocity;
        private final int totalTicks;
        private float ageTicks = 0.0f;
        private final String ease;
        private final float sizeStart;
        private final float sizeEnd;
        private final float alphaStart;
        private final float alphaEnd;
        private final float drag;
        private final float gravity;
        private final boolean fullBright;
        private final boolean fullDayLight;
        private final boolean brightAtDark;
        private final boolean glowTargetMenu;
        private final float lightPercent;
        private final int emissiveStrength;
        private final boolean emissiveBlur;
        private final boolean emissiveClamp;
        private final class_2960 texture;
        private final float red;
        private final float green;
        private final float blue;
        private final boolean debug;
        private final boolean lockInModel;
        private final String lockKey;
        private boolean localSpace;
        private boolean pendingWorldRelease;
        private long spawnNanos;
        private long worldReleaseDelayNanos;
        private boolean renderInWorldPass;
        private boolean worldPreviewBehindLocal;
        private float worldPreviewBehindLocalBias;
        private boolean ignoreItemRotationForLayer;
        private boolean syntheticWorldLocal;
        private boolean pendingReleaseSnapshotReady;
        private class_243 pendingReleaseWorldPos;
        private class_243 pendingReleaseWorldVel;
        private class_243 pendingReleaseWorldCenter;
        private class_243 pendingReleaseWorldAxis;
        private float brightAtDarkVisibilityFactor;
        private float brightAtDarkGlowFactor;
        private final String motionMode;
        private class_243 orbitCenter;
        private class_243 orbitAxis;
        private final float orbitRadius;
        private float orbitAngle;
        private final float orbitAngularVelocity;

        private RenderParticle(class_243 position, class_243 velocity, int totalTicks, String ease, float sizeStart, float sizeEnd, float alphaStart, float alphaEnd, float drag, float gravity, boolean fullBright, boolean fullDayLight, boolean brightAtDark, boolean glowTargetMenu, float lightPercent, int emissiveStrength, boolean emissiveBlur, boolean emissiveClamp, class_2960 texture, float red, float green, float blue, boolean debug, boolean lockInModel, String lockKey, boolean localSpace, String motionMode, class_243 orbitCenter, class_243 orbitAxis, float orbitRadius, float orbitAngle, float orbitAngularVelocity) {
            this.position = position;
            this.velocity = velocity;
            this.totalTicks = Math.max(1, totalTicks);
            this.ease = (ease == null || ease.isBlank()) ? "linear" : ease;
            this.sizeStart = sizeStart;
            this.sizeEnd = sizeEnd;
            this.alphaStart = alphaStart;
            this.alphaEnd = alphaEnd;
            this.drag = drag;
            this.gravity = gravity;
            this.fullBright = fullBright;
            this.fullDayLight = fullDayLight;
            this.brightAtDark = brightAtDark;
            this.glowTargetMenu = glowTargetMenu;
            this.lightPercent = Float.isFinite(lightPercent) ? lightPercent : -1.0f;
            this.emissiveStrength = class_3532.method_15340(emissiveStrength, 0, 100);
            this.emissiveBlur = emissiveBlur;
            this.emissiveClamp = emissiveClamp;
            this.texture = texture;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.debug = debug;
            this.lockInModel = lockInModel;
            this.lockKey = lockKey;
            this.localSpace = localSpace;
            this.pendingWorldRelease = false;
            this.spawnNanos = System.nanoTime();
            this.worldReleaseDelayNanos = ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS;
            this.renderInWorldPass = false;
            this.worldPreviewBehindLocal = false;
            this.worldPreviewBehindLocalBias = ModelPartsParticleSystem.DEFAULT_WORLD_PREVIEW_BEHIND_LOCAL_BIAS;
            this.ignoreItemRotationForLayer = false;
            this.syntheticWorldLocal = false;
            this.pendingReleaseSnapshotReady = false;
            this.pendingReleaseWorldPos = null;
            this.pendingReleaseWorldVel = null;
            this.pendingReleaseWorldCenter = null;
            this.pendingReleaseWorldAxis = null;
            this.brightAtDarkVisibilityFactor = Float.NaN;
            this.brightAtDarkGlowFactor = Float.NaN;
            this.motionMode = ModelPartsParticleSystem.parseMotionMode(motionMode, "linear");
            this.orbitCenter = orbitCenter == null ? position : orbitCenter;
            this.orbitAxis = ModelPartsParticleSystem.normalizeAxis(orbitAxis);
            this.orbitRadius = Math.max(0.0f, orbitRadius);
            this.orbitAngle = orbitAngle;
            this.orbitAngularVelocity = orbitAngularVelocity;
        }

        private boolean orbit() {
            return "orbit".equals(this.motionMode) && this.orbitRadius > ModelPartsParticleSystem.EPS;
        }

        private void enableDelayedWorldRelease(long delayNanos) {
            if (!this.localSpace) {
                return;
            }
            this.pendingWorldRelease = true;
            this.spawnNanos = System.nanoTime();
            this.worldReleaseDelayNanos = Math.max(ModelPartsParticleSystem.BEDROCK_BONE_PARTICLE_WORLD_RELEASE_DELAY_NANOS, delayNanos);
            this.pendingReleaseSnapshotReady = false;
            this.pendingReleaseWorldPos = null;
            this.pendingReleaseWorldVel = null;
            this.pendingReleaseWorldCenter = null;
            this.pendingReleaseWorldAxis = null;
        }

        private void enableWorldPreviewBehindLocal(float bias) {
            this.worldPreviewBehindLocal = true;
            this.worldPreviewBehindLocalBias = class_3532.method_15363(bias, 0.0f, 0.05f);
        }

        private void enableIgnoreItemRotationForLayer() {
            this.ignoreItemRotationForLayer = true;
        }

        private void enableSyntheticWorldLocal() {
            this.syntheticWorldLocal = true;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey.class */
    private static final class ParticleEmissiveKey extends Record {
        private final class_2960 texture;
        private final boolean blur;
        private final boolean clamp;

        private ParticleEmissiveKey(class_2960 texture, boolean blur, boolean clamp) {
            this.texture = texture;
            this.blur = blur;
            this.clamp = clamp;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ParticleEmissiveKey.class), ParticleEmissiveKey.class, "texture;blur;clamp", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->texture:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->blur:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->clamp:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ParticleEmissiveKey.class), ParticleEmissiveKey.class, "texture;blur;clamp", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->texture:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->blur:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->clamp:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ParticleEmissiveKey.class, Object.class), ParticleEmissiveKey.class, "texture;blur;clamp", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->texture:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->blur:Z", "FIELD:Lpunchy/client/modelparts/ModelPartsParticleSystem$ParticleEmissiveKey;->clamp:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_2960 texture() {
            return this.texture;
        }

        public boolean blur() {
            return this.blur;
        }

        public boolean clamp() {
            return this.clamp;
        }
    }
}
