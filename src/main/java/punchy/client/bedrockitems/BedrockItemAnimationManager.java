package punchy.client.bedrockitems;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.runtime.ObjectMethods;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1657;
import net.minecraft.class_1753;
import net.minecraft.class_1764;
import net.minecraft.class_1799;
import net.minecraft.class_1835;
import net.minecraft.class_1839;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_5321;
import net.minecraft.class_630;
import net.minecraft.class_638;
import net.minecraft.class_6862;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_9334;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.AttackActionTracker;
import punchy.client.state.ConsumeStateMachine;
import punchy.client.state.HandEquipStateMachine;
import punchy.client.state.SpearStateMachine;
import punchy.client.state.UseItemStateMachine;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemAnimationManager.class */
public final class BedrockItemAnimationManager {
    private static final EnumMap<class_1306, PoseHandler> HANDLERS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, AnimationClip> ACTIVE_CLIPS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, String> ACTIVE_TYPES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, PunchyTuningConfig.CustomAnimationEntry> ACTIVE_ENTRIES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, PunchyTuningConfig.CustomAnimationEntry> CLIP_ENTRIES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, PunchyTuningConfig.CustomAnimationEntry> KEEP_LOOP_ACTIVE_ENTRY = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, String> KEEP_LOOP_ACTIVE_NAME = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Boolean> LAST_PLAYING = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Boolean> SWIM_SUPPRESSION = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Integer> RAIN_GRACE_TICKS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Integer> INSIDE_WATER_GRACE_TICKS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Integer> KEEP_LOOP_FALSE_GRACE_TICKS_MAP = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Boolean> ANIM_MIRROR = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, String> LAST_STACK_KEY = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, AnimationClip> IDLE_CLIPS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Float> IDLE_TIMES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> LAST_ATTACK_TRIGGER_EPOCH = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Long> LAST_VISUAL_USE_ITEM_EPOCH = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1268, ForcedAction> FORCED_TYPES = new EnumMap<>(class_1268.class);
    private static final EnumMap<class_1268, PersistentAction> PERSISTENT_ACTIONS = new EnumMap<>(class_1268.class);
    private static final float ITEM_BLEND_IN_SECONDS = 0.0f;
    private static final float ITEM_BLEND_TRANSITION_SECONDS = 0.4f;
    private static final float ITEM_KEEP_LOOP_BLEND_SECONDS = 0.55f;
    private static final float ITEM_IDLE_BLEND_SECONDS = 0.6f;
    private static final float ITEM_TRIGGER_BLEND_OUT_SECONDS = 1.0f;
    private static final float ITEM_INSPECT_BLEND_IN_SECONDS = 0.0f;
    private static final float ITEM_INSPECT_BLEND_OUT_SECONDS = 0.18f;
    private static final float ITEM_INSPECT_BLEND_OUT_SLOW = 1.0f;
    private static final int RAIN_FALSE_GRACE_TICKS = 100;
    private static final float ITEM_BLEND_OUT_SECONDS = 3.0f;
    private static final float ITEM_BLEND_OUT_SLOW = 3.6f;
    private static final float ITEM_BASE_DELTA_SECONDS = 0.05f;
    private static final int INSIDE_WATER_FALSE_GRACE_TICKS = 4;
    private static final int KEEP_LOOP_FALSE_GRACE_TICKS = 6;
    private static final String TYPE_USE_DRINK = "useDrink";
    private static final String TYPE_USE_EAT = "useEat";

    private BedrockItemAnimationManager() {
    }

    public static void forceTypeOnce(class_1268 hand, String type, boolean immediate) {
        if (type == null || type.isBlank()) {
            return;
        }
        if (hand == null) {
            putForcedType(class_1268.field_5808, type, immediate);
            putForcedType(class_1268.field_5810, type, immediate);
        } else {
            putForcedType(hand, type, immediate);
        }
    }

    public static void setPersistentType(class_1268 hand, String type, Boolean loopOverride, boolean immediate) {
        if (hand == null || type == null || type.isBlank()) {
            return;
        }
        PERSISTENT_ACTIONS.put(hand, new PersistentAction(type, loopOverride, immediate));
    }

    public static void clearPersistentType(class_1268 hand, String type) {
        PersistentAction current;
        if (hand == null || (current = PERSISTENT_ACTIONS.get(hand)) == null) {
            return;
        }
        if (type == null || type.isBlank() || type.equals(current.type)) {
            PERSISTENT_ACTIONS.remove(hand);
        }
    }

    public static void clearAllPersistentTypes() {
        PERSISTENT_ACTIONS.clear();
    }

    public static boolean isAnyDualHandedActive() {
        for (class_1306 arm : class_1306.values()) {
            PoseHandler handler = HANDLERS.get(arm);
            if (handler != null) {
                if (handler.isPlaying() && handler.isCurrentDualHandedActive()) {
                    return true;
                }
                AnimationClip secondary = handler.getSecondaryClip();
                if (secondary != null && PunchyAnimationManager.isDualHandedActive(secondary, handler.getSecondaryTime())) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:113:0x048d  */
    /* JADX WARN: Removed duplicated region for block: B:146:0x0595  */
    /* JADX WARN: Removed duplicated region for block: B:174:0x05fd  */
    /* JADX WARN: Removed duplicated region for block: B:190:0x063b  */
    /* JADX WARN: Removed duplicated region for block: B:193:0x0643  */
    /* JADX WARN: Removed duplicated region for block: B:201:0x066b  */
    /* JADX WARN: Removed duplicated region for block: B:218:0x06b6  */
    /* JADX WARN: Removed duplicated region for block: B:221:0x06be  */
    /* JADX WARN: Removed duplicated region for block: B:233:0x0711  */
    /* JADX WARN: Removed duplicated region for block: B:235:0x0721  */
    /* JADX WARN: Removed duplicated region for block: B:239:0x0745  */
    /* JADX WARN: Removed duplicated region for block: B:247:0x0788  */
    /* JADX WARN: Removed duplicated region for block: B:250:0x079f  */
    /* JADX WARN: Removed duplicated region for block: B:251:0x07a3  */
    /* JADX WARN: Removed duplicated region for block: B:257:0x07d6  */
    /* JADX WARN: Removed duplicated region for block: B:261:0x07e2  */
    /* JADX WARN: Removed duplicated region for block: B:263:0x07e7  */
    /* JADX WARN: Removed duplicated region for block: B:264:0x07ec  */
    /* JADX WARN: Removed duplicated region for block: B:270:0x0810  */
    /* JADX WARN: Removed duplicated region for block: B:276:0x082a  */
    /* JADX WARN: Removed duplicated region for block: B:281:0x0837  */
    /* JADX WARN: Removed duplicated region for block: B:285:0x0843  */
    /* JADX WARN: Removed duplicated region for block: B:427:0x0b6b  */
    /* JADX WARN: Removed duplicated region for block: B:451:0x0c1b  */
    /* JADX WARN: Removed duplicated region for block: B:452:0x0c23  */
    /* JADX WARN: Removed duplicated region for block: B:480:0x0ce5  */
    /* JADX WARN: Removed duplicated region for block: B:482:0x0cfd  */
    /* JADX WARN: Removed duplicated region for block: B:507:0x0db3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static void tick(net.minecraft.class_310 r8) {
        /*
            Method dump skipped, instruction units count: 3531
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: punchy.client.bedrockitems.BedrockItemAnimationManager.tick(net.minecraft.class_310):void");
    }

    public static void applyToParts(class_1306 arm, Map<String, class_630> parts, float partialTicks) {
        PoseHandler handler;
        if (arm == null || parts == null || parts.isEmpty() || (handler = HANDLERS.get(arm)) == null) {
            return;
        }
        boolean mirrorX = Boolean.TRUE.equals(ANIM_MIRROR.get(arm));
        handler.setMirrorX(mirrorX);
        handler.applyTransformations(parts, partialTicks, true);
    }

    public static boolean isSwimSuppressed(class_1306 arm) {
        if (arm == null) {
            return false;
        }
        return Boolean.TRUE.equals(SWIM_SUPPRESSION.get(arm));
    }

    public static boolean isAnimMirrorActive(class_1306 arm) {
        if (arm == null) {
            return false;
        }
        return Boolean.TRUE.equals(ANIM_MIRROR.get(arm));
    }

    public static PunchyTuningConfig.CustomAnimationEntry getActiveEntry(class_1306 arm) {
        if (arm == null) {
            return null;
        }
        return ACTIVE_ENTRIES.get(arm);
    }

    private static boolean hasItemSuppression(BedrockItemDefinition def, class_1799 stack, boolean swim) {
        List<PunchyTuningConfig.CustomAnimationEntry> entries;
        if (def == null || stack == null || stack.method_7960() || (entries = def.getModelPartsAnimation()) == null || entries.isEmpty()) {
            return false;
        }
        for (PunchyTuningConfig.CustomAnimationEntry entry : entries) {
            if (entry != null && (entry.items == null || entry.items.isEmpty() || BedrockItemSystem.matchesItemList(stack, entry.items))) {
                if (swim && Boolean.TRUE.equals(entry.disable_swim_animation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getStackKey(class_1799 stack, BedrockItemDefinition def) {
        class_2960 itemId;
        if (stack == null || stack.method_7960() || (itemId = class_7923.field_41178.method_10221(stack.method_7909())) == null) {
            return null;
        }
        int cmd = -1;
        try {
            Object cmdObj = stack.method_58694(class_9334.field_49637);
            if (cmdObj instanceof Integer) {
                Integer i = (Integer) cmdObj;
                cmd = i.intValue();
            } else if (cmdObj != null) {
                try {
                    Method method = cmdObj.getClass().getMethod("value", new Class[0]);
                    Object value = method.invoke(cmdObj, new Object[0]);
                    if (value instanceof Integer) {
                        Integer i2 = (Integer) value;
                        cmd = i2.intValue();
                    } else if (value instanceof List) {
                        List<?> list = (List) value;
                        if (!list.isEmpty()) {
                            Object obj = list.get(0);
                            if (obj instanceof Integer) {
                                Integer i22 = (Integer) obj;
                                cmd = i22.intValue();
                            }
                        }
                    }
                } catch (Throwable th) {
                }
                if (cmd == -1) {
                    try {
                        Method method2 = cmdObj.getClass().getMethod("getValue", new Class[0]);
                        Object value2 = method2.invoke(cmdObj, new Object[0]);
                        if (value2 instanceof Integer) {
                            Integer i3 = (Integer) value2;
                            cmd = i3.intValue();
                        } else if (value2 instanceof List) {
                            List<?> list2 = (List) value2;
                            if (!list2.isEmpty()) {
                                Object obj2 = list2.get(0);
                                if (obj2 instanceof Integer) {
                                    Integer i23 = (Integer) obj2;
                                    cmd = i23.intValue();
                                }
                            }
                        }
                    } catch (Throwable th2) {
                    }
                }
            }
        } catch (Throwable th3) {
        }
        String customName = "";
        try {
            class_2561 hover = stack.method_7964();
            if (hover != null) {
                String raw = hover.getString();
                customName = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            }
        } catch (Throwable th4) {
        }
        String defKey = (def == null || def.getId() == null) ? "null" : def.getId().toString();
        return String.valueOf(itemId) + ":" + cmd + "|name=" + customName + "|def=" + defKey;
    }

    private static void stopHandler(PoseHandler handler, class_1306 arm) {
        handler.setLooping(false);
        handler.resetImmediate();
        ACTIVE_CLIPS.put(arm, (AnimationClip) null);
        ACTIVE_TYPES.remove(arm);
        ACTIVE_ENTRIES.remove(arm);
    }

    private static ActionState resolveDesiredState(class_310 client, class_1306 arm, class_1799 stack) {
        if (client == null || client.field_1724 == null) {
            return ActionState.EMPTY;
        }
        ActionState forcedVisualUseItem = resolveVisualForcedUseItemState(client, arm, stack);
        if (forcedVisualUseItem != null) {
            return forcedVisualUseItem;
        }
        ForcedAction forced = consumeForcedTypeForArm(client, arm);
        if (forced != null) {
            return new ActionState(resolveTypeWithFallback(stack, forced.type), null, forced.immediate);
        }
        PersistentAction persistent = getPersistentForArm(client, arm);
        if (persistent != null) {
            return new ActionState(resolveTypeWithFallback(stack, persistent.type), persistent.loopOverride, persistent.immediate);
        }
        String consumeTypeFinishing = resolveConsumeTypeForArmDuringFinish(client, arm);
        if (consumeTypeFinishing != null) {
            return new ActionState(resolveTypeWithFallback(stack, consumeTypeFinishing), null, true);
        }
        if (consumeAttackPulseForArm(client, arm)) {
            return new ActionState(resolveTypeWithFallback(stack, "attack"), null, true);
        }
        String consumeType = resolveConsumeTypeForArm(client, arm);
        if (consumeType != null) {
            return new ActionState(resolveTypeWithFallback(stack, consumeType), null, true);
        }
        if (isChargeUseActiveForArm(client, arm)) {
            return new ActionState(resolveTypeWithFallback(stack, "useItem"), true, true);
        }
        return new ActionState(resolveTypeWithFallback(stack, "idle"), null, false);
    }

    private static ActionState resolveVisualForcedUseItemState(class_310 client, class_1306 arm, class_1799 stack) {
        AnimationClip poseClip;
        String poseName;
        String resolved;
        if (client == null || client.field_1724 == null || arm == null || stack == null || stack.method_7960() || !PunchyAnimationManager.isItemForceShown(arm) || (poseClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || (poseName = poseClip.getName()) == null || poseName.isBlank() || !isVisualUseItemPose(poseName) || (resolved = resolveTypeWithFallback(stack, "useItem")) == null) {
            return null;
        }
        return new ActionState(resolved, false, true);
    }

    private static boolean isVisualUseItemPose(String poseName) {
        String normalized = normalizeType(poseName);
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.contains("strikefire") || normalized.contains("interact") || normalized.contains("interaction") || normalized.startsWith("use") || normalized.contains("flintsteel");
    }

    private static boolean shouldRestartVisualForcedUseItem(class_1306 arm, String type, AnimationClip desired) {
        if (arm == null || desired == null || !"useItem".equals(type) || !PunchyAnimationManager.isItemForceShown(arm)) {
            return false;
        }
        long currentEpoch = UseItemStateMachine.getVisualUseItemEpoch();
        if (currentEpoch <= 0) {
            return false;
        }
        long lastEpoch = ((Long) LAST_VISUAL_USE_ITEM_EPOCH.getOrDefault(arm, 0L)).longValue();
        LAST_VISUAL_USE_ITEM_EPOCH.put(arm, Long.valueOf(currentEpoch));
        return currentEpoch > lastEpoch;
    }

    private static String resolveConsumeTypeForArm(class_310 client, class_1306 arm) {
        class_1799 usingStack;
        if (client == null || client.field_1724 == null || arm == null || !client.field_1724.method_6115() || client.field_1690 == null || !client.field_1690.field_1904.method_1434()) {
            return null;
        }
        class_1268 usedHand = client.field_1724.method_6058();
        class_1268 expected = resolveHand(client, arm);
        if (usedHand != expected || (usingStack = client.field_1724.method_6030()) == null || usingStack.method_7960()) {
            return null;
        }
        class_1839 useAnim = usingStack.method_7976();
        if (useAnim == class_1839.field_8946) {
            return TYPE_USE_DRINK;
        }
        if (useAnim == class_1839.field_8950) {
            return TYPE_USE_EAT;
        }
        return null;
    }

    private static String resolveConsumeTypeForArmDuringFinish(class_310 client, class_1306 arm) {
        AnimationClip clip;
        if (!ConsumeStateMachine.isEquipBlockedByConsumeAnimation() || client == null || client.field_1724 == null || arm == null) {
            return null;
        }
        String forcedType = ConsumeStateMachine.getForcedConsumeTypeForArm(arm);
        if (forcedType != null) {
            return forcedType;
        }
        if (!PunchyAnimationManager.POSE_HANDLER.isPlaying() || (clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null) {
            return null;
        }
        class_1306 clipArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
        if (clipArm != null && clipArm != arm) {
            return null;
        }
        if (PunchyAnimationManager.isClipNamed(clip, "use_drink")) {
            return TYPE_USE_DRINK;
        }
        if (PunchyAnimationManager.isClipNamed(clip, "use_eat")) {
            return TYPE_USE_EAT;
        }
        return null;
    }

    private static boolean isChargeUseActiveForArm(class_310 client, class_1306 arm) {
        class_1799 usingStack;
        if (client == null || client.field_1724 == null || arm == null || !client.field_1724.method_6115() || client.field_1690 == null || !client.field_1690.field_1904.method_1434()) {
            return false;
        }
        class_1268 usedHand = client.field_1724.method_6058();
        class_1268 expected = resolveHand(client, arm);
        if (usedHand != expected || (usingStack = client.field_1724.method_6030()) == null || usingStack.method_7960()) {
            return false;
        }
        return (usingStack.method_7909() instanceof class_1753) || (usingStack.method_7909() instanceof class_1764) || (usingStack.method_7909() instanceof class_1835) || SpearStateMachine.isSpearStack(usingStack);
    }

    private static ForcedAction consumeForcedTypeForArm(class_310 client, class_1306 arm) {
        class_1268 hand = resolveHand(client, arm);
        return FORCED_TYPES.remove(hand);
    }

    private static PersistentAction getPersistentForArm(class_310 client, class_1306 arm) {
        class_1306 sourceArm;
        AnimationClip clip;
        class_1268 hand = resolveHand(client, arm);
        PersistentAction current = PERSISTENT_ACTIONS.get(hand);
        if (current != null) {
            return current;
        }
        if (client == null || client.field_1724 == null || arm == null || !PunchyAnimationManager.isItemForceShown(arm) || (sourceArm = PunchyAnimationManager.getSourceArm()) == null || sourceArm == arm || (clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
            return null;
        }
        float time = PunchyAnimationManager.POSE_HANDLER.getCurrentTime();
        if (!PunchyAnimationManager.isDualHandedActive(clip, time)) {
            return null;
        }
        class_1268 sourceHand = resolveHand(client, sourceArm);
        return PERSISTENT_ACTIONS.get(sourceHand);
    }

    private static boolean consumeAttackPulseForArm(class_310 client, class_1306 arm) {
        if (client == null || client.field_1724 == null || arm == null) {
            return false;
        }
        class_1268 armHand = resolveHand(client, arm);
        class_1268 attackHand = AttackActionTracker.peekLastRecordedHand();
        if (attackHand != armHand) {
            return false;
        }
        long epoch = AttackActionTracker.peekEpoch();
        long seen = ((Long) LAST_ATTACK_TRIGGER_EPOCH.getOrDefault(arm, 0L)).longValue();
        if (epoch <= seen) {
            return false;
        }
        LAST_ATTACK_TRIGGER_EPOCH.put(arm, Long.valueOf(epoch));
        return true;
    }

    private static class_1799 resolveStack(class_310 client, class_1306 arm) {
        class_1268 class_1268Var;
        class_1799 baseStack;
        class_1799 forcedConsumeStack = ConsumeStateMachine.getForcedRenderStack(arm);
        if (forcedConsumeStack != null && !forcedConsumeStack.method_7960()) {
            return forcedConsumeStack;
        }
        if (client == null || client.field_1724 == null || arm == null) {
            return class_1799.field_8037;
        }
        boolean mainRight = client.field_1724.method_6068() == class_1306.field_6183;
        if (arm == class_1306.field_6183) {
            class_1268Var = mainRight ? class_1268.field_5808 : class_1268.field_5810;
        } else {
            class_1268Var = mainRight ? class_1268.field_5810 : class_1268.field_5808;
        }
        class_1268 hand = class_1268Var;
        if (hand == class_1268.field_5810) {
            baseStack = HandEquipStateMachine.getVisualOffhandStack(client.field_1724);
        } else {
            baseStack = client.field_1724.method_6047();
        }
        return PunchyAnimationManager.resolveRenderStack(client.field_1724, arm, baseStack);
    }

    private static class_1268 resolveHand(class_310 client, class_1306 arm) {
        boolean mainRight = client.field_1724.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? class_1268.field_5808 : class_1268.field_5810 : mainRight ? class_1268.field_5810 : class_1268.field_5808;
    }

    private static AnimationClip resolveDesiredClip(BedrockItemDefinition def, class_1799 stack, String type, class_310 client, class_1306 arm) {
        if (def == null || type == null || type.isBlank()) {
            return null;
        }
        PunchyTuningConfig.CustomAnimationEntry entry = resolveEntry(def, stack, type, client, arm);
        return resolveDesiredClip(def, entry, arm, null);
    }

    private static AnimationClip resolveDesiredClip(BedrockItemDefinition def, PunchyTuningConfig.CustomAnimationEntry entry, class_1306 arm, String overrideName) {
        AnimationClip overrideClip;
        if (def == null) {
            return null;
        }
        if (overrideName != null && !overrideName.isBlank() && (overrideClip = resolveClipByName(def, overrideName.trim())) != null) {
            if (arm != null) {
                String suffix = arm == class_1306.field_6182 ? "_left" : "_right";
                AnimationClip sided = resolveClipByName(def, overrideName.trim() + suffix);
                if (sided != null) {
                    return sided;
                }
            }
            return overrideClip;
        }
        String name = resolveEntryName(entry);
        if (name == null || name.isBlank()) {
            return null;
        }
        if (arm != null) {
            String suffix2 = arm == class_1306.field_6182 ? "_left" : "_right";
            AnimationClip sided2 = resolveClipByName(def, name + suffix2);
            if (sided2 != null) {
                return sided2;
            }
        }
        return resolveClipByName(def, name);
    }

    private static AnimationClip resolveClipByName(BedrockItemDefinition def, String name) {
        if (def == null || name == null || name.isBlank()) {
            return null;
        }
        AnimationClip clip = def.getAnimations().get(name);
        if (clip != null) {
            return clip;
        }
        return def.getAnimations().get(def.getId().method_12836() + ":" + name);
    }

    private static AnimationClip resolveKeepLoopClip(BedrockItemDefinition def, String name, class_1306 arm) {
        if (def == null || name == null || name.isBlank()) {
            return null;
        }
        if (arm != null) {
            String suffix = arm == class_1306.field_6182 ? "_left" : "_right";
            AnimationClip sided = resolveClipByName(def, name + suffix);
            if (sided != null) {
                return sided;
            }
        }
        return resolveClipByName(def, name);
    }

    private static boolean isEndedClipForEntry(BedrockItemDefinition def, AnimationClip ended, PunchyTuningConfig.CustomAnimationEntry entry, class_1306 arm) {
        String baseName;
        if (def == null || ended == null || entry == null || (baseName = resolveEntryName(entry)) == null || baseName.isBlank()) {
            return false;
        }
        AnimationClip base = resolveClipByName(def, baseName);
        if (base != null && ended == base) {
            return true;
        }
        if (arm != null) {
            String suffix = arm == class_1306.field_6182 ? "_left" : "_right";
            AnimationClip sided = resolveClipByName(def, baseName + suffix);
            return ended == sided;
        }
        return false;
    }

    private static boolean startKeepLoopTransition(BedrockItemDefinition def, PoseHandler handler, class_1306 arm, String type, PunchyTuningConfig.CustomAnimationEntry clipEntry, String keepName, float keepStartTime) {
        AnimationClip keepClip;
        if (def == null || handler == null || clipEntry == null || keepName == null || keepName.isBlank() || (keepClip = resolveKeepLoopClip(def, keepName, arm)) == null) {
            return false;
        }
        boolean skipBlendIn = Boolean.TRUE.equals(clipEntry.skip_blend_in);
        boolean blendFromCurrentOrigin = shouldBlendFromCurrentOrigin(clipEntry);
        boolean noActiveClipNow = !handler.isPlaying() || handler.getCurrentClip() == null;
        handler.setLooping(true);
        if (skipBlendIn || noActiveClipNow) {
            handler.playAtTime(keepClip, 1.0f, true, false, keepStartTime);
        } else {
            handler.setBlendInDurationNext(ITEM_KEEP_LOOP_BLEND_SECONDS);
            if (blendFromCurrentOrigin) {
                handler.setBlendInEasingNext(PoseHandler.BlendInEasing.STRONG_OUT_BACK);
                handler.playAtTimeRebasedFromCurrentReturnToOrigin(keepClip, 1.0f, false, keepStartTime);
            } else {
                handler.playAtTime(keepClip, 1.0f, true, false, keepStartTime);
            }
        }
        ACTIVE_CLIPS.put(arm, keepClip);
        ACTIVE_TYPES.put(arm, type);
        KEEP_LOOP_ACTIVE_ENTRY.put(arm, clipEntry);
        KEEP_LOOP_ACTIVE_NAME.put(arm, keepName);
        return true;
    }

    private static PunchyTuningConfig.CustomAnimationEntry resolveEntry(BedrockItemDefinition def, class_1799 stack, String type, class_310 client, class_1306 arm) {
        List<PunchyTuningConfig.CustomAnimationEntry> entries;
        if (def == null || (entries = def.getModelPartsAnimation()) == null || entries.isEmpty()) {
            return null;
        }
        String normalized = normalizeType(type);
        PunchyTuningConfig.CustomAnimationEntry bestTriggered = null;
        int bestTriggeredScore = -1;
        PunchyTuningConfig.CustomAnimationEntry bestFallback = null;
        int bestFallbackScore = -1;
        for (PunchyTuningConfig.CustomAnimationEntry entry : entries) {
            if (entry != null && entry.type != null && matchesRequestedType(normalized, normalizeType(entry.type)) && (entry.items == null || entry.items.isEmpty() || BedrockItemSystem.matchesItemList(stack, entry.items))) {
                if (matchesWorldTriggersStable(entry, client, arm)) {
                    int score = triggerSpecificityScore(entry);
                    if (score > 0) {
                        if (score > bestTriggeredScore) {
                            bestTriggered = entry;
                            bestTriggeredScore = score;
                        }
                    } else if (score > bestFallbackScore) {
                        bestFallback = entry;
                        bestFallbackScore = score;
                    }
                }
            }
        }
        return bestTriggered != null ? bestTriggered : bestFallback;
    }

    private static String resolveEntryName(PunchyTuningConfig.CustomAnimationEntry entry) {
        if (entry == null) {
            return null;
        }
        if (entry.animation != null && !entry.animation.isBlank()) {
            return entry.animation.trim();
        }
        if (entry.name != null && !entry.name.isBlank()) {
            return entry.name.trim();
        }
        if (entry.var_1 != null && !entry.var_1.isBlank()) {
            return entry.var_1.trim();
        }
        return null;
    }

    private static float updateIdleTime(class_1306 arm, PoseHandler handler, AnimationClip idleClip, float baseDeltaSeconds) {
        float idleTime;
        if (idleClip == null) {
            IDLE_CLIPS.remove(arm);
            IDLE_TIMES.remove(arm);
            return 0.0f;
        }
        AnimationClip previous = IDLE_CLIPS.put(arm, idleClip);
        Float stored = IDLE_TIMES.get(arm);
        float idleTime2 = stored == null ? 0.0f : stored.floatValue();
        if (previous != idleClip) {
            idleTime2 = 0.0f;
        }
        if (handler != null && handler.isPlaying() && handler.getCurrentClip() == idleClip) {
            idleTime = handler.getCurrentTime();
        } else if (handler != null && handler.getSecondaryClip() == idleClip) {
            idleTime = handler.getSecondaryTime();
        } else {
            idleTime = advanceLoopTime(idleClip, idleTime2, baseDeltaSeconds);
        }
        IDLE_TIMES.put(arm, Float.valueOf(idleTime));
        return idleTime;
    }

    private static void syncIdleOverlay(PoseHandler handler, class_1306 arm, AnimationClip idleClip, float idleTime, AnimationClip primaryClip) {
        if (handler == null) {
            return;
        }
        if (idleClip == null || primaryClip == null || primaryClip == idleClip) {
            if (handler.getSecondaryClip() != null) {
                handler.clearSecondaryClip();
            }
        } else if (handler.getSecondaryClip() != idleClip) {
            handler.setSecondaryClipAtTime(idleClip, arm, 1.0f, idleTime);
        }
    }

    private static float advanceLoopTime(AnimationClip clip, float time, float delta) {
        if (clip == null) {
            return 0.0f;
        }
        float length = clip.getLength();
        if (length <= 0.0f) {
            return 0.0f;
        }
        float next = time + delta;
        if (clip.isLoop()) {
            float next2 = next % length;
            if (next2 < 0.0f) {
                next2 += length;
            }
            return next2;
        }
        return Math.min(next, length);
    }

    private static boolean shouldLoop(String type) {
        if (type == null) {
            return false;
        }
        return "mining".equals(type) || "useItem".equals(type) || "idle".equals(type);
    }

    private static boolean isConsumeType(String type) {
        return TYPE_USE_DRINK.equals(type) || TYPE_USE_EAT.equals(type);
    }

    private static boolean shouldBlendFromCurrentOrigin(PunchyTuningConfig.CustomAnimationEntry entry) {
        return entry != null && Boolean.TRUE.equals(entry.blend_from_current_origin);
    }

    private static String resolveTypeWithFallback(class_1799 stack, String type) {
        return type;
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        return type.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    private static void putForcedType(class_1268 hand, String type, boolean immediate) {
        if (hand == null || type == null || type.isBlank()) {
            return;
        }
        ForcedAction incoming = new ForcedAction(type, immediate);
        ForcedAction current = FORCED_TYPES.get(hand);
        if (current == null) {
            FORCED_TYPES.put(hand, incoming);
            return;
        }
        int currentPriority = forcedTypePriority(current.type);
        int incomingPriority = forcedTypePriority(incoming.type);
        if (incomingPriority > currentPriority) {
            FORCED_TYPES.put(hand, incoming);
        } else if (incomingPriority == currentPriority) {
            FORCED_TYPES.put(hand, incoming);
        }
    }

    private static int forcedTypePriority(String type) {
        String normalized = normalizeType(type);
        if ("attack".equals(normalized)) {
            return RAIN_FALSE_GRACE_TICKS;
        }
        if ("triggerable".equals(normalized)) {
            return 90;
        }
        if ("inspect".equals(normalized)) {
            return 80;
        }
        if ("handout".equals(normalized)) {
            return 60;
        }
        if ("handin".equals(normalized)) {
            return 55;
        }
        if ("mining".equals(normalized)) {
            return 50;
        }
        if ("useitem".equals(normalized)) {
            return 45;
        }
        if ("usedrink".equals(normalized) || "useeat".equals(normalized)) {
            return 44;
        }
        return "idle".equals(normalized) ? 0 : 40;
    }

    private static boolean matchesRequestedType(String requestedType, String entryType) {
        if (requestedType == null || entryType == null) {
            return false;
        }
        if (requestedType.equals(entryType)) {
            return true;
        }
        return "idle".equals(requestedType) && "inwater".equals(entryType);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemAnimationManager$ForcedAction.class */
    private static final class ForcedAction extends Record {
        private final String type;
        private final boolean immediate;

        private ForcedAction(String type, boolean immediate) {
            this.type = type;
            this.immediate = immediate;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ForcedAction.class), ForcedAction.class, "type;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ForcedAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ForcedAction;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ForcedAction.class), ForcedAction.class, "type;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ForcedAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ForcedAction;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ForcedAction.class, Object.class), ForcedAction.class, "type;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ForcedAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ForcedAction;->immediate:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String type() {
            return this.type;
        }

        public boolean immediate() {
            return this.immediate;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction.class */
    private static final class PersistentAction extends Record {
        private final String type;
        private final Boolean loopOverride;
        private final boolean immediate;

        private PersistentAction(String type, Boolean loopOverride, boolean immediate) {
            this.type = type;
            this.loopOverride = loopOverride;
            this.immediate = immediate;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, PersistentAction.class), PersistentAction.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, PersistentAction.class), PersistentAction.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, PersistentAction.class, Object.class), PersistentAction.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$PersistentAction;->immediate:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String type() {
            return this.type;
        }

        public Boolean loopOverride() {
            return this.loopOverride;
        }

        public boolean immediate() {
            return this.immediate;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemAnimationManager$ActionState.class */
    private static final class ActionState extends Record {
        private final String type;
        private final Boolean loopOverride;
        private final boolean immediate;
        private static final ActionState EMPTY = new ActionState(null, null, false);

        private ActionState(String type, Boolean loopOverride, boolean immediate) {
            this.type = type;
            this.loopOverride = loopOverride;
            this.immediate = immediate;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ActionState.class), ActionState.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ActionState.class), ActionState.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ActionState.class, Object.class), ActionState.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationManager$ActionState;->immediate:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String type() {
            return this.type;
        }

        public Boolean loopOverride() {
            return this.loopOverride;
        }

        public boolean immediate() {
            return this.immediate;
        }
    }

    private static int triggerSpecificityScore(PunchyTuningConfig.CustomAnimationEntry entry) {
        if (entry == null) {
            return 0;
        }
        int score = 0;
        if (entry.in_water != null) {
            score = 0 + 2;
        }
        if (entry.inside_water != null) {
            score += 3;
        }
        if (entry.block_detection != null) {
            score++;
        }
        if (entry.weather_detection != null) {
            score++;
        }
        if (entry.time_detection != null) {
            score++;
        }
        if (entry.biome_detection != null) {
            score++;
        }
        return score;
    }

    private static boolean matchesWorldTriggers(PunchyTuningConfig.CustomAnimationEntry entry, class_310 client, class_1306 arm) {
        if (entry == null) {
            return false;
        }
        if (entry.in_water == null && entry.inside_water == null && entry.block_detection == null && entry.weather_detection == null && entry.time_detection == null && entry.biome_detection == null) {
            return true;
        }
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_746 player = client.field_1724;
        class_638 class_638Var = client.field_1687;
        if (class_638Var == null) {
            return false;
        }
        String normalizedType = normalizeType(entry.type);
        if ("inwater".equals(normalizedType) && entry.in_water == null && !isInWaterContact(class_638Var, player)) {
            return false;
        }
        if (entry.in_water != null) {
            boolean inWater = isInWaterContact(class_638Var, player);
            if (entry.in_water.booleanValue() != inWater) {
                return false;
            }
        }
        if (entry.inside_water != null) {
            boolean inside = player.method_5777(class_3486.field_15517);
            if (Boolean.TRUE.equals(entry.inside_water) && arm != null) {
                int grace = ((Integer) INSIDE_WATER_GRACE_TICKS.getOrDefault(arm, 0)).intValue();
                if (inside) {
                    grace = INSIDE_WATER_FALSE_GRACE_TICKS;
                } else if (grace > 0) {
                    grace--;
                    inside = true;
                }
                INSIDE_WATER_GRACE_TICKS.put(arm, Integer.valueOf(grace));
            }
            if (entry.inside_water.booleanValue() != inside) {
                return false;
            }
        }
        if (entry.weather_detection != null && !matchesWeather(entry.weather_detection, class_638Var, player, arm)) {
            return false;
        }
        if (entry.time_detection != null && !matchesTime(entry.time_detection, class_638Var)) {
            return false;
        }
        if (entry.block_detection != null && !matchesBlock(entry.block_detection, class_638Var, player.method_24515())) {
            return false;
        }
        if (entry.biome_detection != null && !matchesBiome(entry.biome_detection, class_638Var, player.method_24515())) {
            return false;
        }
        return true;
    }

    private static boolean matchesWorldTriggersStable(PunchyTuningConfig.CustomAnimationEntry entry, class_310 client, class_1306 arm) {
        boolean matched = matchesWorldTriggers(entry, client, arm);
        if (entry != null && entry.weather_detection != null) {
            return matched;
        }
        if (arm == null || entry == null || entry.keep_looping == null || entry.keep_looping.isBlank()) {
            return matched;
        }
        int grace = ((Integer) KEEP_LOOP_FALSE_GRACE_TICKS_MAP.getOrDefault(arm, 0)).intValue();
        if (matched) {
            KEEP_LOOP_FALSE_GRACE_TICKS_MAP.put(arm, Integer.valueOf(KEEP_LOOP_FALSE_GRACE_TICKS));
            return true;
        }
        if (grace > 0) {
            KEEP_LOOP_FALSE_GRACE_TICKS_MAP.put(arm, Integer.valueOf(grace - 1));
            return true;
        }
        KEEP_LOOP_FALSE_GRACE_TICKS_MAP.put(arm, 0);
        return false;
    }

    private static boolean isInWaterContact(class_1937 level, class_1657 player) {
        if (level == null || player == null) {
            return false;
        }
        if (player.method_5799() || player.method_5777(class_3486.field_15517)) {
            return true;
        }
        double x = player.method_23317();
        double y = player.method_23318();
        double z = player.method_23321();
        double[] ys = {y + 0.05d, y + 0.6d, y + 1.1d};
        double[] xs = {x, x - 0.3d, x + 0.3d};
        double[] zs = {z, z - 0.3d, z + 0.3d};
        for (double sampleY : ys) {
            for (double sampleX : xs) {
                for (double sampleZ : zs) {
                    if (level.method_8316(class_2338.method_49637(sampleX, sampleY, sampleZ)).method_15767(class_3486.field_15517)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesWeather(PunchyTuningConfig.WeatherDetection weather, class_1937 level, class_1657 player, class_1306 arm) {
        if (weather == null || level == null) {
            return true;
        }
        boolean isRainingAt = level.method_8419();
        boolean isThundering = level.method_8546();
        if (player != null) {
            class_2338 pos = class_2338.method_49637(player.method_23317(), player.method_23318() + 1.0d, player.method_23321());
            isRainingAt = level.method_8419() || level.method_8520(pos) || level.method_8520(pos.method_10084());
            isThundering = level.method_8546() && isRainingAt;
        }
        if (weather.raining != null) {
            boolean desired = weather.raining.booleanValue();
            boolean effectiveRaining = isRainingAt;
            if (desired && arm != null) {
                int grace = ((Integer) RAIN_GRACE_TICKS.getOrDefault(arm, 0)).intValue();
                if (isRainingAt) {
                    grace = RAIN_FALSE_GRACE_TICKS;
                } else if (grace > 0) {
                    grace--;
                    effectiveRaining = true;
                }
                RAIN_GRACE_TICKS.put(arm, Integer.valueOf(grace));
            }
            if (desired != effectiveRaining) {
                return false;
            }
        }
        if (weather.thundering != null && weather.thundering.booleanValue() != isThundering) {
            return false;
        }
        if (weather.clear != null) {
            boolean clear = (isRainingAt || isThundering) ? false : true;
            if (weather.clear.booleanValue() != clear) {
                return false;
            }
            return true;
        }
        return true;
    }

    private static boolean matchesTime(PunchyTuningConfig.TimeDetection time, class_1937 level) {
        if (time == null || level == null) {
            return true;
        }
        int min = time.min != null ? time.min.intValue() : -1;
        int max = time.max != null ? time.max.intValue() : -1;
        if (time.range != null && time.range.size() >= 2) {
            min = time.range.get(0).intValue();
            max = time.range.get(1).intValue();
        }
        if (min < 0 && max < 0) {
            return true;
        }
        int dayTime = (int) (level.method_8532() % 24000);
        return min < 0 ? dayTime <= max : max < 0 ? dayTime >= min : min <= max ? dayTime >= min && dayTime <= max : dayTime >= min || dayTime <= max;
    }

    private static boolean matchesBlock(PunchyTuningConfig.BlockDetection block, class_1937 level, class_2338 origin) {
        if (block == null || level == null || origin == null) {
            return true;
        }
        List<String> targets = block.blocks;
        if ((targets == null || targets.isEmpty()) && block.block != null) {
            targets = List.of(block.block);
        }
        if (targets == null || targets.isEmpty()) {
            return true;
        }
        int radius = block.radius != null ? Math.max(0, block.radius.intValue()) : 0;
        int offX = block.offsetX != null ? block.offsetX.intValue() : 0;
        int offY = block.offsetY != null ? block.offsetY.intValue() : 0;
        int offZ = block.offsetZ != null ? block.offsetZ.intValue() : 0;
        class_2338 center = origin.method_10069(offX, offY, offZ);
        if (radius == 0) {
            return matchesBlockList(level.method_8320(center), targets);
        }
        class_2338.class_2339 cursor = new class_2338.class_2339();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.method_10103(center.method_10263() + dx, center.method_10264() + dy, center.method_10260() + dz);
                    if (matchesBlockList(level.method_8320(cursor), targets)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesBiome(PunchyTuningConfig.BiomeDetection biome, class_1937 level, class_2338 origin) {
        if (biome == null || level == null || origin == null) {
            return true;
        }
        List<String> targets = biome.biomes;
        if ((targets == null || targets.isEmpty()) && biome.biome != null) {
            targets = List.of(biome.biome);
        }
        if (targets == null || targets.isEmpty()) {
            return true;
        }
        int radius = biome.radius != null ? Math.max(0, biome.radius.intValue()) : 0;
        int offX = biome.offsetX != null ? biome.offsetX.intValue() : 0;
        int offY = biome.offsetY != null ? biome.offsetY.intValue() : 0;
        int offZ = biome.offsetZ != null ? biome.offsetZ.intValue() : 0;
        class_2338 center = origin.method_10069(offX, offY, offZ);
        if (radius == 0) {
            return matchesBiomeList(level, center, targets);
        }
        class_2338.class_2339 cursor = new class_2338.class_2339();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.method_10103(center.method_10263() + dx, center.method_10264() + dy, center.method_10260() + dz);
                    if (matchesBiomeList(level, cursor, targets)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesBiomeList(class_1937 level, class_2338 pos, List<String> targets) {
        if (level == null || pos == null || targets == null || targets.isEmpty()) {
            return false;
        }
        class_6880<class_1959> holder = level.method_23753(pos);
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

    private static boolean matchesBlockList(class_2680 state, List<String> targets) {
        if (state == null || targets == null || targets.isEmpty()) {
            return false;
        }
        class_2960 id = class_7923.field_41175.method_10221(state.method_26204());
        String current = id == null ? null : id.toString();
        for (String entry : targets) {
            if (entry != null && !entry.isBlank()) {
                String trimmed = entry.trim();
                if (trimmed.startsWith("#")) {
                    class_2960 tagId = class_2960.method_12829(trimmed.substring(1));
                    if (tagId != null) {
                        class_6862<class_2248> tag = class_6862.method_40092(class_7924.field_41254, tagId);
                        if (state.method_26164(tag)) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                } else {
                    class_2960 parsed = class_2960.method_12829(trimmed);
                    if (parsed != null && current != null && current.equals(parsed.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
