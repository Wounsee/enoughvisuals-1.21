package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1661;
import net.minecraft.class_1764;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1819;
import net.minecraft.class_1835;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import net.minecraft.class_9331;
import net.minecraft.class_9334;
import net.minecraft.class_9336;
import org.joml.Vector3f;
import punchy.client.PunchyClient;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.animation.data.BoneTrack;
import punchy.client.animation.data.Keyframe;
import punchy.client.state.CustomAnimationResolver;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/HandEquipStateMachine.class */
public class HandEquipStateMachine {
    private static final boolean LOG_OFFHAND_DETACH_DEBUG = false;
    private static final boolean LOG_WALK_ON_WATER_EQUIP_DEBUG = true;
    private static final long OFFHAND_USE_UNSUPPRESS_WINDOW_NANOS = 1800000000;
    private static HandEquipStateMachine instance;
    private static volatile boolean equipActive;
    private static volatile String activeType;
    private static volatile int handOutGraceTicks;
    private static volatile boolean syncRequested;
    private static volatile int vanillaHandOutMainTicks;
    private static volatile int vanillaHandOutOffTicks;
    private static volatile int lastEquipTransitionPlayerTick = -1;
    private static final String EFFECT_SWAP_END = "swap_end";
    private static final String EFFECT_SWAP = "swap_items";
    private static final String EFFECT_SWAP_ITEM = "swap_item";
    private static final int VANILLA_HAND_OUT_TICKS = 4;
    private static final int HOTBAR_RAPID_WINDOW_TICKS = 4;
    private static final int HOTBAR_RAPID_MIN_STREAK = 2;
    private static final int F_SWAP_BURST_WINDOW_TICKS = 10;
    private static final int F_SWAP_BURST_MIN_COUNT = 2;
    private static final float HOTBAR_HAND_OUT_SPEED_BOOST = 2.6f;
    private static final float HAND_OUT_ACTION_BLEND_SECONDS = 0.1f;
    private static final float HAND_OUT_TO_HAND_IN_EARLY_TRANSITION_SECONDS = 0.05f;
    private static final float WALK_ON_WATER_MIN_REBASE_TIME = 5.0E-4f;
    private boolean initialized;
    private int hotbarRapidWindowTicks;
    private int hotbarChangeStreak;
    private boolean pendingMainChange;
    private boolean pendingOffChange;
    private boolean pickupRevealMainOnRaise;
    private boolean pickupRevealOffOnRaise;
    private boolean deferSwapWithoutEquip;
    private boolean skipNextEquipMain;
    private boolean skipNextEquipOff;
    private boolean swapTriggered;
    private boolean cycleMain;
    private boolean cycleOff;
    private boolean pausedByUseClip;
    private boolean suppressOffhandRender;
    private boolean hideOffhandTridentThrowCache;
    private boolean offhandRevealPending;
    private boolean offhandAwaitingHandIn;
    private boolean pendingBlacklistHandInMain;
    private boolean pendingBlacklistHandInOff;
    private boolean blacklistTransitionHandIn;
    private boolean pendingMainActionClear;
    private boolean pendingOffActionClear;
    private AnimationClip pendingMainActionClearClip;
    private AnimationClip pendingOffActionClearClip;
    private float pendingMainActionClearTime;
    private float pendingOffActionClearTime;
    private AnimationClip activeHandInClip;
    private AnimationClip activeHandOutClip;
    private class_1306 activeHandInArm;
    private boolean detachedOffhandEquip;
    private boolean shieldOppositeEquip;
    private Phase phase = Phase.IDLE;
    private int lastHotbarSlot = -1;
    private final Deque<Integer> recentFunctionalSwapTicks = new ArrayDeque();
    private class_1799 renderedMain = class_1799.field_8037;
    private class_1799 renderedOff = class_1799.field_8037;
    private class_1799 pendingMain = class_1799.field_8037;
    private class_1799 pendingOff = class_1799.field_8037;
    private boolean lastOffhandEmpty = true;
    private String lastOffhandDebugSnapshot = "";

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/HandEquipStateMachine$Phase.class */
    private enum Phase {
        IDLE,
        LOWERING,
        RAISING
    }

    public void tick(class_310 client) {
        String str;
        if (client == null || client.field_1724 == null) {
            return;
        }
        class_746 player = client.field_1724;
        if (!this.initialized) {
            initialize(player);
        }
        if (syncRequested && !shouldDeferRenderedSyncDuringShieldOppositeEquip()) {
            syncRequested = false;
            syncRenderedItems(player);
        }
        class_1799 currentMain = player.method_6047();
        class_1799 currentOff = player.method_6079();
        updateSkipEquipLatches(currentMain, currentOff);
        boolean hotbarChanged = updateHotbarSlot(player);
        if (hotbarChanged) {
            this.hotbarChangeStreak = Math.min(8, this.hotbarChangeStreak + LOG_WALK_ON_WATER_EQUIP_DEBUG);
            this.hotbarRapidWindowTicks = 4;
        } else if (this.hotbarRapidWindowTicks > 0) {
            this.hotbarRapidWindowTicks -= LOG_WALK_ON_WATER_EQUIP_DEBUG;
        } else {
            this.hotbarChangeStreak = LOG_OFFHAND_DETACH_DEBUG;
        }
        boolean blockingClipActive = isBlockingEquipClipActive(currentMain);
        if (blockingClipActive) {
            this.pausedByUseClip = true;
            if (currentMain.method_7960() && !this.renderedMain.method_7960()) {
                this.renderedMain = class_1799.field_8037;
            }
        } else if (this.pausedByUseClip && this.phase != Phase.IDLE) {
            resumeEquipClip(player);
            this.pausedByUseClip = false;
        } else if (this.pausedByUseClip) {
            this.pausedByUseClip = false;
        }
        boolean suppressMainEquipChange = updatePendingActionClearMain(currentMain, currentOff);
        boolean suppressOffEquipChange = updatePendingActionClearOff(currentOff, currentMain);
        boolean mainChanged = !suppressMainEquipChange && (hasItemChanged(this.renderedMain, currentMain) || (hotbarChanged && shouldAnimateHotbarChange(this.renderedMain, currentMain)));
        boolean offChanged = !suppressOffEquipChange && hasItemChanged(this.renderedOff, currentOff);
        updateOffhandRevealPending(player, currentOff, offChanged);
        updateOffhandAwaitingHandIn(player, currentOff);
        this.suppressOffhandRender = shouldSuppressOffhandRender(currentOff);
        switch (this.phase.ordinal()) {
            case LOG_OFFHAND_DETACH_DEBUG /* 0 */:
                handleIdle(client, player, currentMain, currentOff, mainChanged, offChanged);
                break;
            case LOG_WALK_ON_WATER_EQUIP_DEBUG /* 1 */:
                handleLowering(client, player, currentMain, currentOff, hotbarChanged);
                break;
            case 2:
                handleRaising(client, player, currentMain, currentOff, hotbarChanged);
                break;
        }
        equipActive = this.phase != Phase.IDLE;
        switch (this.phase.ordinal()) {
            case LOG_OFFHAND_DETACH_DEBUG /* 0 */:
                str = null;
                break;
            case LOG_WALK_ON_WATER_EQUIP_DEBUG /* 1 */:
                str = "hand_out";
                break;
            case 2:
                str = "hand_in";
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        activeType = str;
        if ("hand_out".equals(activeType) && this.cycleOff) {
            handOutGraceTicks = 2;
        } else if (handOutGraceTicks > 0) {
            handOutGraceTicks -= LOG_WALK_ON_WATER_EQUIP_DEBUG;
        }
        if (this.phase == Phase.LOWERING) {
            if (this.cycleMain) {
                ItemAnimationManager.setPersistentType(class_1268.field_5808, "hand_out", false);
            } else {
                ItemAnimationManager.clearPersistentType(class_1268.field_5808, "hand_out");
            }
            if (this.cycleOff && !shouldSuppressOffhandHandOut(player, currentOff)) {
                ItemAnimationManager.setPersistentType(class_1268.field_5810, "hand_out", false);
            } else {
                ItemAnimationManager.clearPersistentType(class_1268.field_5810, "hand_out");
            }
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "hand_in");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "hand_in");
        } else if (this.phase == Phase.RAISING) {
            if (this.cycleMain) {
                ItemAnimationManager.setPersistentType(class_1268.field_5808, "hand_in", false);
            } else {
                ItemAnimationManager.clearPersistentType(class_1268.field_5808, "hand_in");
            }
            if (this.cycleOff) {
                ItemAnimationManager.setPersistentType(class_1268.field_5810, "hand_in", false);
            } else {
                ItemAnimationManager.clearPersistentType(class_1268.field_5810, "hand_in");
            }
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "hand_out");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "hand_out");
        } else {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "hand_out");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "hand_out");
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "hand_in");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "hand_in");
        }
        logOffhandState(player, currentOff);
    }

    public HandEquipStateMachine() {
        instance = this;
    }

    public static void requestRenderedSync() {
        syncRequested = true;
    }

    public static void triggerPickupEquipCycle(class_310 client, class_1268 hand) {
        if (instance == null || client == null) {
            return;
        }
        class_746 localPlayer = client.field_1724;
        if (!(localPlayer instanceof class_746)) {
            return;
        }
        class_1268 targetHand = hand == null ? class_1268.field_5808 : hand;
        instance.forcePickupEquipCycle(localPlayer, targetHand);
    }

    public static void triggerImmediateEquipCycle(class_310 client, class_1268 hand) {
        if (instance == null || client == null) {
            return;
        }
        class_746 localPlayer = client.field_1724;
        if (!(localPlayer instanceof class_746)) {
            return;
        }
        class_1268 targetHand = hand == null ? class_1268.field_5808 : hand;
        instance.forceImmediateEquipCycle(localPlayer, targetHand);
    }

    private void handleIdle(class_310 client, class_746 player, class_1799 currentMain, class_1799 currentOff, boolean mainChanged, boolean offChanged) {
        boolean walkOnWaterActive = isWalkOnWaterPoseActive() || CustomAnimationResolver.shouldAutoBlendFromWalkOnWater();
        boolean functionalOffhandSwap = isFunctionalOffhandSwap(this.renderedMain, this.renderedOff, currentMain, currentOff, mainChanged, offChanged, false);
        boolean allowWalkOnWaterEquip = walkOnWaterActive && ((mainChanged && !offChanged) || functionalOffhandSwap);
        if (handleBlacklistToPunchyTransition(player, currentMain, currentOff, mainChanged, offChanged)) {
            return;
        }
        if (this.deferSwapWithoutEquip) {
            if (mainChanged || offChanged) {
                queuePending(player, currentMain, currentOff, mainChanged, offChanged, false);
            }
            if (!isSkipHandEquipWindowActive()) {
                applyPending(player);
                this.deferSwapWithoutEquip = false;
                this.skipNextEquipMain = false;
                this.skipNextEquipOff = false;
                return;
            }
            applyEquipOverrides(player);
            return;
        }
        if ((mainChanged || offChanged) && shouldPreserveCurrentClipForHandOut() && (ItemAnimationManager.isTypeActive("mining") || MiningActionTracker.wasRecentMining(250000000L))) {
            queuePending(player, currentMain, currentOff, mainChanged, offChanged, false);
            startLowering(player);
            return;
        }
        if (isLoopingActionClip() && !allowWalkOnWaterEquip) {
            if (mainChanged) {
                this.renderedMain = copyStack(currentMain);
            }
            if (offChanged) {
                this.renderedOff = copyStack(currentOff);
            }
            this.pendingMainChange = false;
            this.pendingOffChange = false;
            applyEquipOverrides(player);
            return;
        }
        boolean blockEquip = shouldBlockEquip(client, currentMain, mainChanged, offChanged);
        if (!allowWalkOnWaterEquip && blockEquip && (mainChanged || offChanged)) {
            clearRenderedEmptyHandsWhileUseBlocksEquip(player, currentMain, currentOff, mainChanged, offChanged);
            queuePending(player, currentMain, currentOff, mainChanged, offChanged, false);
            applyEquipOverrides(player);
            return;
        }
        if ((allowWalkOnWaterEquip || !blockEquip) && (mainChanged || offChanged)) {
            if (shouldSkipHandEquipOnSwap(currentMain, currentOff, mainChanged, offChanged)) {
                queuePending(player, currentMain, currentOff, mainChanged, offChanged, false);
                this.deferSwapWithoutEquip = true;
                applyEquipOverrides(player);
                return;
            } else {
                queuePending(player, currentMain, currentOff, mainChanged, offChanged, false);
                this.hideOffhandTridentThrowCache = shouldHideOffhandTridentThrowCache(currentOff, offChanged);
                startLowering(player);
                return;
            }
        }
        this.renderedMain = copyStack(currentMain);
        this.renderedOff = copyStack(currentOff);
        this.hideOffhandTridentThrowCache = false;
        applyEquipOverrides(player);
    }

    private void forcePickupEquipCycle(class_746 player, class_1268 hand) {
        if (player == null) {
            return;
        }
        class_1799 currentMain = player.method_6047();
        class_1799 currentOff = player.method_6079();
        if (hand == class_1268.field_5810) {
            this.pendingOffChange = true;
            this.pendingOff = copyStack(currentOff);
            this.renderedOff = class_1799.field_8037;
            this.pendingMainChange = false;
            this.pickupRevealOffOnRaise = true;
            this.pickupRevealMainOnRaise = false;
        } else {
            this.pendingMainChange = true;
            this.pendingMain = copyStack(currentMain);
            this.renderedMain = class_1799.field_8037;
            this.pendingOffChange = false;
            this.pickupRevealMainOnRaise = true;
            this.pickupRevealOffOnRaise = false;
        }
        this.deferSwapWithoutEquip = false;
        this.skipNextEquipMain = false;
        this.skipNextEquipOff = false;
        this.pausedByUseClip = false;
        if (this.phase == Phase.IDLE) {
            startLowering(player);
            return;
        }
        if (this.phase == Phase.RAISING) {
            this.activeHandInClip = null;
            this.activeHandInArm = null;
            this.phase = Phase.IDLE;
            startLowering(player);
            return;
        }
        applyEquipOverrides(player);
    }

    private void forceImmediateEquipCycle(class_746 player, class_1268 hand) {
        if (player == null) {
            return;
        }
        class_1799 currentMain = player.method_6047();
        class_1799 currentOff = player.method_6079();
        if (hand == class_1268.field_5810) {
            this.pendingOffChange = true;
            this.pendingOff = copyStack(currentOff);
            this.pendingMainChange = false;
        } else {
            this.pendingMainChange = true;
            this.pendingMain = copyStack(currentMain);
            this.pendingOffChange = false;
        }
        this.pickupRevealMainOnRaise = false;
        this.pickupRevealOffOnRaise = false;
        this.deferSwapWithoutEquip = false;
        this.skipNextEquipMain = false;
        this.skipNextEquipOff = false;
        this.pausedByUseClip = false;
        if (this.phase == Phase.RAISING) {
            this.activeHandInClip = null;
            this.activeHandInArm = null;
            this.phase = Phase.IDLE;
        }
        if (this.phase == Phase.IDLE) {
            startLowering(player);
        } else {
            applyEquipOverrides(player);
        }
    }

    private boolean handleBlacklistToPunchyTransition(class_746 player, class_1799 currentMain, class_1799 currentOff, boolean mainChanged, boolean offChanged) {
        if (player == null) {
            return false;
        }
        boolean queued = LOG_OFFHAND_DETACH_DEBUG;
        if (!this.pendingBlacklistHandInMain && mainChanged && wasItemBlacklisted(this.renderedMain) && !isHandBlacklisted(player, class_1268.field_5808)) {
            this.pendingBlacklistHandInMain = true;
            this.pendingMainChange = true;
            this.pendingMain = copyStack(currentMain);
            vanillaHandOutMainTicks = 4;
            queued = LOG_WALK_ON_WATER_EQUIP_DEBUG;
        }
        if (!this.pendingBlacklistHandInOff && offChanged && wasItemBlacklisted(this.renderedOff) && !isHandBlacklisted(player, class_1268.field_5810)) {
            this.pendingBlacklistHandInOff = true;
            this.pendingOffChange = true;
            this.pendingOff = copyStack(currentOff);
            vanillaHandOutOffTicks = 4;
            queued = LOG_WALK_ON_WATER_EQUIP_DEBUG;
        }
        if (!queued && !this.pendingBlacklistHandInMain && !this.pendingBlacklistHandInOff) {
            return false;
        }
        if (this.pendingBlacklistHandInMain && vanillaHandOutMainTicks > 0) {
            vanillaHandOutMainTicks -= LOG_WALK_ON_WATER_EQUIP_DEBUG;
        }
        if (this.pendingBlacklistHandInOff && vanillaHandOutOffTicks > 0) {
            vanillaHandOutOffTicks -= LOG_WALK_ON_WATER_EQUIP_DEBUG;
        }
        boolean readyMain = this.pendingBlacklistHandInMain && vanillaHandOutMainTicks <= 0;
        boolean readyOff = this.pendingBlacklistHandInOff && vanillaHandOutOffTicks <= 0;
        if (!readyMain && !readyOff) {
            applyEquipOverrides(player);
            return true;
        }
        if (readyMain) {
            this.pendingBlacklistHandInMain = false;
            this.pendingMainChange = true;
            this.pendingMain = copyStack(currentMain);
        }
        if (readyOff) {
            this.pendingBlacklistHandInOff = false;
            this.pendingOffChange = true;
            this.pendingOff = copyStack(currentOff);
        }
        this.cycleMain = readyMain;
        this.cycleOff = readyOff;
        this.blacklistTransitionHandIn = true;
        applyPending(player);
        startRaising(player);
        return true;
    }

    private void handleLowering(class_310 client, class_746 player, class_1799 currentMain, class_1799 currentOff, boolean hotbarChanged) {
        if (!this.shieldOppositeEquip && isLoopingActionClip()) {
            resetToCurrent(player);
            return;
        }
        if (!this.shieldOppositeEquip && isBlockingEquipClipActive(currentMain)) {
            this.pausedByUseClip = true;
            return;
        }
        if (isInterruptedByOtherClip()) {
            if (tryDetachOffhandDuringAction(player)) {
                return;
            }
            exitEquipPreservingSecondary(player);
            return;
        }
        boolean changedNow = updatePendingIfChanged(player, currentMain, currentOff, hotbarChanged);
        if (changedNow) {
            if (isRapidHotbarScrollActive() && this.pendingMainChange && !this.pendingOffChange) {
                this.swapTriggered = true;
                applyPending(player);
                startRaising(player);
                return;
            } else if (shouldForceDirectHandInOnRapidRepeat(player)) {
                forceDirectHandInFromPending(player);
                return;
            } else {
                startLowering(player);
                return;
            }
        }
        boolean handOutReadyToSwap = shouldSwapNow() || isHandOutInEarlyTransitionWindow();
        if (!this.swapTriggered && handOutReadyToSwap) {
            this.swapTriggered = true;
            boolean skipRaise = shouldSkipRaiseForOffhandRemovalBefore();
            applyPending(player);
            if (skipRaise || shouldSkipRaiseForOffhandRemovalAfter(player)) {
                finishLoweringWithoutRaise(player);
                return;
            } else {
                startRaising(player);
                return;
            }
        }
        if (!isClipPlaying(this.activeHandOutClip)) {
            boolean skipRaise2 = shouldSkipRaiseForOffhandRemovalBefore();
            applyPending(player);
            if (skipRaise2 || shouldSkipRaiseForOffhandRemovalAfter(player)) {
                finishLoweringWithoutRaise(player);
            } else {
                startRaising(player);
            }
        }
    }

    private void handleRaising(class_310 client, class_746 player, class_1799 currentMain, class_1799 currentOff, boolean hotbarChanged) {
        revealPickupStackOnRaise(player);
        if (!this.shieldOppositeEquip && isLoopingActionClip()) {
            resetToCurrent(player);
            return;
        }
        if (!this.shieldOppositeEquip && isBlockingEquipClipActive(currentMain)) {
            this.pausedByUseClip = true;
            return;
        }
        if (isInterruptedByOtherClip()) {
            if (tryDetachOffhandDuringAction(player)) {
                return;
            }
            exitEquipPreservingSecondary(player);
            return;
        }
        boolean changedNow = updatePendingIfChanged(player, currentMain, currentOff, hotbarChanged);
        if (changedNow) {
            if (shouldForceDirectHandInOnRapidRepeat(player)) {
                forceDirectHandInFromPending(player);
                return;
            } else {
                startLowering(player);
                return;
            }
        }
        if (!isClipPlaying(this.activeHandInClip)) {
            finishRaise(player);
        }
    }

    private boolean shouldForceDirectHandInOnRapidRepeat(class_746 player) {
        return wasFunctionalSwapRecordedThisTick(player) || isFunctionalSwapBurstActive(player);
    }

    private void initialize(class_746 player) {
        this.renderedMain = copyStack(player.method_6047());
        this.renderedOff = copyStack(player.method_6079());
        this.lastHotbarSlot = resolveSelectedSlot(player);
        this.lastOffhandEmpty = this.renderedOff == null || this.renderedOff.method_7960();
        this.offhandAwaitingHandIn = false;
        this.offhandRevealPending = false;
        this.suppressOffhandRender = false;
        this.activeHandInClip = null;
        this.activeHandOutClip = null;
        this.activeHandInArm = null;
        this.detachedOffhandEquip = false;
        this.shieldOppositeEquip = false;
        this.pendingBlacklistHandInMain = false;
        this.pendingBlacklistHandInOff = false;
        this.blacklistTransitionHandIn = false;
        this.pickupRevealMainOnRaise = false;
        this.pickupRevealOffOnRaise = false;
        vanillaHandOutMainTicks = LOG_OFFHAND_DETACH_DEBUG;
        vanillaHandOutOffTicks = LOG_OFFHAND_DETACH_DEBUG;
        this.hotbarRapidWindowTicks = LOG_OFFHAND_DETACH_DEBUG;
        this.hotbarChangeStreak = LOG_OFFHAND_DETACH_DEBUG;
        this.recentFunctionalSwapTicks.clear();
        clearPendingActionClears();
        PunchyAnimationManager.POSE_HANDLER.clearSecondaryClip();
        this.initialized = true;
    }

    private void resetToCurrent(class_746 player) {
        resetEquipState(player, true);
        this.phase = Phase.IDLE;
        markEquipTransitionTick(player);
        applyEquipOverrides(player);
    }

    private void resetEquipState(class_746 player, boolean clearSecondaryClip) {
        this.renderedMain = copyStack(player.method_6047());
        this.renderedOff = copyStack(player.method_6079());
        this.pendingMainChange = false;
        this.pendingOffChange = false;
        this.deferSwapWithoutEquip = false;
        this.skipNextEquipMain = false;
        this.skipNextEquipOff = false;
        this.swapTriggered = false;
        this.cycleMain = false;
        this.cycleOff = false;
        this.suppressOffhandRender = false;
        this.offhandRevealPending = false;
        this.offhandAwaitingHandIn = false;
        this.lastOffhandEmpty = this.renderedOff == null || this.renderedOff.method_7960();
        this.activeHandInClip = null;
        this.activeHandOutClip = null;
        this.activeHandInArm = null;
        this.detachedOffhandEquip = false;
        this.shieldOppositeEquip = false;
        this.pendingBlacklistHandInMain = false;
        this.pendingBlacklistHandInOff = false;
        this.blacklistTransitionHandIn = false;
        vanillaHandOutMainTicks = LOG_OFFHAND_DETACH_DEBUG;
        vanillaHandOutOffTicks = LOG_OFFHAND_DETACH_DEBUG;
        this.hotbarRapidWindowTicks = LOG_OFFHAND_DETACH_DEBUG;
        this.hotbarChangeStreak = LOG_OFFHAND_DETACH_DEBUG;
        clearPendingActionClears();
        PunchyAnimationManager.POSE_HANDLER.setIgnoreDualHanded(false);
        if (clearSecondaryClip) {
            PunchyAnimationManager.POSE_HANDLER.clearSecondaryClip();
        }
    }

    private void exitEquipPreservingSecondary(class_746 player) {
        resetEquipState(player, false);
        this.phase = Phase.IDLE;
        markEquipTransitionTick(player);
        applyEquipOverrides(player);
    }

    private void syncRenderedItems(class_746 player) {
        this.renderedMain = copyStack(player.method_6047());
        this.renderedOff = copyStack(player.method_6079());
        this.pendingMainChange = false;
        this.pendingOffChange = false;
        this.deferSwapWithoutEquip = false;
        this.skipNextEquipMain = false;
        this.skipNextEquipOff = false;
        this.suppressOffhandRender = false;
        this.offhandRevealPending = false;
        this.offhandAwaitingHandIn = false;
        this.lastOffhandEmpty = this.renderedOff == null || this.renderedOff.method_7960();
        this.pendingBlacklistHandInMain = false;
        this.pendingBlacklistHandInOff = false;
        this.blacklistTransitionHandIn = false;
        this.detachedOffhandEquip = false;
        this.shieldOppositeEquip = false;
        vanillaHandOutMainTicks = LOG_OFFHAND_DETACH_DEBUG;
        vanillaHandOutOffTicks = LOG_OFFHAND_DETACH_DEBUG;
        this.hotbarRapidWindowTicks = LOG_OFFHAND_DETACH_DEBUG;
        this.hotbarChangeStreak = LOG_OFFHAND_DETACH_DEBUG;
        this.recentFunctionalSwapTicks.clear();
        clearPendingActionClears();
        PunchyAnimationManager.POSE_HANDLER.setIgnoreDualHanded(false);
        if (!shouldPreserveWalkOnWaterOffhandSecondary()) {
            PunchyAnimationManager.POSE_HANDLER.clearSecondaryClip();
        }
        applyEquipOverrides(player);
    }

    private void finishRaise(class_746 player) {
        this.phase = Phase.IDLE;
        this.cycleMain = false;
        this.cycleOff = false;
        this.deferSwapWithoutEquip = false;
        this.skipNextEquipMain = false;
        this.skipNextEquipOff = false;
        this.suppressOffhandRender = false;
        this.offhandRevealPending = false;
        this.offhandAwaitingHandIn = false;
        this.activeHandInClip = null;
        this.activeHandInArm = null;
        this.detachedOffhandEquip = false;
        this.shieldOppositeEquip = false;
        this.pendingBlacklistHandInMain = false;
        this.pendingBlacklistHandInOff = false;
        this.blacklistTransitionHandIn = false;
        vanillaHandOutMainTicks = LOG_OFFHAND_DETACH_DEBUG;
        vanillaHandOutOffTicks = LOG_OFFHAND_DETACH_DEBUG;
        boolean preserveWalkOnWaterSecondary = shouldPreserveWalkOnWaterOffhandSecondary();
        debugWalkOnWaterEquip("finishRaise", preserveWalkOnWaterSecondary);
        PunchyAnimationManager.POSE_HANDLER.setIgnoreDualHanded(false);
        if (!preserveWalkOnWaterSecondary) {
            PunchyAnimationManager.POSE_HANDLER.clearSecondaryClip();
        }
        clearPendingActionClears();
        markEquipTransitionTick(player);
        applyEquipOverrides(player);
        if (player != null) {
            PunchyClient.getWalkOnWaterStateMachine().reattachMainhandAfterEquip(player);
        }
        if (this.pendingMainChange || this.pendingOffChange) {
            restartRaisingFromPending(player);
        }
    }

    private void restartRaisingFromPending(class_746 player) {
        if (!this.pendingMainChange && !this.pendingOffChange) {
            return;
        }
        this.cycleMain = this.pendingMainChange;
        this.cycleOff = this.pendingOffChange;
        applyPending(player);
        startRaising(player);
    }

    private void forceDirectHandInFromPending(class_746 player) {
        if (!this.pendingMainChange && !this.pendingOffChange) {
            return;
        }
        clearSecondaryClipUnlessPreservingWalkOnWater("forceDirectHandInFromPending");
        this.activeHandOutClip = null;
        this.activeHandInClip = null;
        this.activeHandInArm = null;
        this.detachedOffhandEquip = false;
        this.shieldOppositeEquip = false;
        this.swapTriggered = true;
        this.cycleMain = this.pendingMainChange;
        this.cycleOff = this.pendingOffChange;
        applyPending(player);
        startRaising(player);
    }

    private void queuePending(class_746 player, class_1799 currentMain, class_1799 currentOff, boolean mainChanged, boolean offChanged, boolean hotbarChanged) {
        recordFunctionalSwapIfNeeded(player, currentMain, currentOff, mainChanged, offChanged, hotbarChanged);
        this.pendingMainChange = mainChanged;
        this.pendingOffChange = offChanged;
        if (mainChanged) {
            this.pendingMain = copyStack(currentMain);
        }
        if (offChanged) {
            this.pendingOff = copyStack(currentOff);
        }
    }

    private void startLowering(class_746 player) {
        class_1306 previousActionArm;
        class_1268 class_1268Var;
        class_1268 class_1268Var2;
        this.deferSwapWithoutEquip = false;
        this.cycleMain = this.pendingMainChange;
        this.cycleOff = this.pendingOffChange;
        if (!this.cycleOff || this.pendingOff == null || !this.pendingOff.method_7960() || this.renderedOff == null || !(this.renderedOff.method_7909() instanceof class_1835)) {
            this.hideOffhandTridentThrowCache = false;
        }
        boolean autoWalkOnWaterBlend = isWalkOnWaterPoseActive() || CustomAnimationResolver.shouldAutoBlendFromWalkOnWater();
        if (autoWalkOnWaterBlend && shouldDelayWalkOnWaterHandOut()) {
            return;
        }
        boolean rapidHotbarLowering = this.cycleMain && isRapidHotbarScrollActive();
        this.shieldOppositeEquip = shouldRunOppositeHandEquipOverShield(player);
        if (!this.shieldOppositeEquip && !shouldPreserveCurrentClipForHandOut()) {
            stopConflictingClip();
        }
        EquipSelection selection = resolveHandOutClip();
        AnimationClip clip = selection.clip;
        boolean customSingleHand = this.cycleMain && this.cycleOff && selection.explicitCustom && selection.hand != null;
        boolean splitRebasedSwapHands = LOG_OFFHAND_DETACH_DEBUG;
        class_1268 customSingleHandOther = LOG_OFFHAND_DETACH_DEBUG;
        AnimationClip customSingleHandSecondary = LOG_OFFHAND_DETACH_DEBUG;
        if (customSingleHand) {
            class_310 client = class_310.method_1551();
            if (selection.hand == class_1268.field_5808) {
                class_1268Var2 = class_1268.field_5810;
            } else {
                class_1268Var2 = class_1268.field_5808;
            }
            class_1268 other = class_1268Var2;
            AnimationClip defaultClip = PunchyAnimationManager.resolveHandedClip("hand_out", client, other);
            class_1306 otherArm = player == null ? null : resolveArm(player, other);
            if (defaultClip != null && !clipHasTracksForArm(selection.clip, otherArm)) {
                customSingleHandOther = other;
                customSingleHandSecondary = defaultClip;
                clip = selection.clip;
                CustomAnimationResolver.applyLoopSpeed(selection.entry);
            } else {
                customSingleHand = LOG_OFFHAND_DETACH_DEBUG;
                CustomAnimationResolver.applyLoopSpeed(selection.entry);
            }
        }
        if (clip == null) {
            CustomAnimationResolver.applyLoopSpeed(null);
            class_310 client2 = class_310.method_1551();
            class_1268 hand = (!this.cycleOff || this.cycleMain) ? class_1268.field_5808 : class_1268.field_5810;
            clip = PunchyAnimationManager.resolveHandedClip("hand_out", client2, hand);
        } else if (!customSingleHand) {
            CustomAnimationResolver.applyLoopSpeed(selection.entry);
        }
        if (!customSingleHand && this.cycleMain && this.cycleOff && !autoWalkOnWaterBlend && shouldBlendHandOutFromCurrent() && player != null && (previousActionArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm()) != null) {
            class_1268 primaryHand = resolveHandForArm(player, previousActionArm);
            if (primaryHand == class_1268.field_5808) {
                class_1268Var = class_1268.field_5810;
            } else {
                class_1268Var = class_1268.field_5808;
            }
            class_1268 otherHand = class_1268Var;
            class_310 client3 = class_310.method_1551();
            AnimationClip primaryClip = PunchyAnimationManager.resolveHandedClip("hand_out", client3, primaryHand);
            AnimationClip secondaryClip = PunchyAnimationManager.resolveHandedClip("hand_out", client3, otherHand);
            if (primaryClip != null && secondaryClip != null) {
                clip = primaryClip;
                customSingleHandOther = otherHand;
                customSingleHandSecondary = secondaryClip;
                splitRebasedSwapHands = LOG_WALK_ON_WATER_EQUIP_DEBUG;
            }
        }
        if (clip == null) {
            if (this.cycleMain) {
                ItemAnimationManager.forceTypeOnce(class_1268.field_5808, "hand_out");
            }
            if (this.cycleOff && !shouldSuppressOffhandHandOut(player, player.method_6079())) {
                ItemAnimationManager.forceTypeOnce(class_1268.field_5810, "hand_out");
            }
            clearSecondaryClipUnlessPreservingWalkOnWater("startLowering:noClip");
            applyPending(player);
            startRaising(player);
            return;
        }
        this.activeHandOutClip = clip;
        this.swapTriggered = false;
        applyEquipOverrides(player);
        class_1306 arm = resolveActiveArm(player);
        if (customSingleHand) {
            arm = resolveArm(player, selection.hand);
        } else if (splitRebasedSwapHands && player != null) {
            arm = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
        }
        if (autoWalkOnWaterBlend && this.cycleMain && this.cycleOff) {
            arm = LOG_OFFHAND_DETACH_DEBUG;
        }
        boolean forceSingleArmEquip = this.cycleMain ^ this.cycleOff;
        boolean customDualHandedEquip = selection.explicitCustom && PunchyAnimationManager.hasAnyDualHandedEffect(clip);
        boolean ignoreDynamicDualHanded = autoWalkOnWaterBlend || (forceSingleArmEquip && !customDualHandedEquip);
        if (!ignoreDynamicDualHanded && PunchyAnimationManager.isDualHandedActive(class_310.method_1551(), clip, 0.0f)) {
            arm = LOG_OFFHAND_DETACH_DEBUG;
        }
        float speedMultiplier = resolveEquipSpeedMultiplier();
        if (rapidHotbarLowering) {
            speedMultiplier *= HOTBAR_HAND_OUT_SPEED_BOOST;
        }
        if (this.shieldOppositeEquip) {
            float handOutStartTime = resolveEquipBlendStartTime(clip);
            PunchyAnimationManager.POSE_HANDLER.setSecondaryClipAtTime(clip, arm, speedMultiplier, handOutStartTime);
            this.phase = Phase.LOWERING;
            return;
        }
        PunchyAnimationManager.POSE_HANDLER.setIgnoreDualHanded(ignoreDynamicDualHanded);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        preserveWalkOnWaterOffhandDuringMainhandEquip(player, autoWalkOnWaterBlend);
        if ((customSingleHand || splitRebasedSwapHands) && customSingleHandSecondary != null && customSingleHandOther != null) {
            class_1306 otherArm2 = resolveArm(player, customSingleHandOther);
            PunchyAnimationManager.POSE_HANDLER.setSecondaryClip(customSingleHandSecondary, otherArm2, speedMultiplier);
        }
        boolean skipBlendIn = rapidHotbarLowering || selection.entry == null || CustomAnimationResolver.shouldSkipBlendIn(selection.entry);
        if (autoWalkOnWaterBlend) {
            float handOutStartTime2 = resolveEquipBlendStartTime(clip);
            if (this.cycleMain && this.cycleOff) {
                PunchyAnimationManager.setSourceHand(class_310.method_1551(), class_1268.field_5810);
                PunchyAnimationManager.POSE_HANDLER.setRebaseUseBothArmsForNextPlay(true);
            } else {
                PunchyAnimationManager.setSourceHand(class_310.method_1551(), resolveHandForArm(player, arm));
                if (arm != null) {
                    PunchyAnimationManager.POSE_HANDLER.setRebaseSourceArmForNextPlay(arm);
                }
            }
            PunchyAnimationManager.POSE_HANDLER.setRebaseReturnSpeedNext(0.95f);
            PunchyAnimationManager.POSE_HANDLER.setBlendInDurationNext(0.0f);
            PunchyAnimationManager.POSE_HANDLER.playAtTimeRebasedFromCurrent(clip, speedMultiplier, false, handOutStartTime2);
        } else if (shouldBlendHandOutFromSwim()) {
            float handOutStartTime3 = resolveEquipBlendStartTime(clip);
            PunchyAnimationManager.POSE_HANDLER.playAtTimeRebasedFromHandlerReturnToOrigin(PunchyAnimationManager.SWIM_HANDLER, clip, speedMultiplier, false, handOutStartTime3);
        } else if (CustomAnimationResolver.shouldBlendFromCurrentOrigin(selection.entry, clip)) {
            float handOutStartTime4 = resolveEquipBlendStartTime(clip);
            PunchyAnimationManager.POSE_HANDLER.playAtTimeRebasedFromCurrentReturnToOrigin(clip, speedMultiplier, false, handOutStartTime4);
        } else if (shouldBlendHandOutFromCurrent()) {
            float handOutStartTime5 = resolveEquipBlendStartTime(clip);
            PunchyAnimationManager.POSE_HANDLER.playAtTimeWithBlendFromCurrent(clip, speedMultiplier, false, handOutStartTime5);
        } else {
            PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, skipBlendIn, false);
        }
        this.phase = Phase.LOWERING;
    }

    private void startRaising(class_746 player) {
        class_1268 class_1268Var;
        class_1799 currentOff;
        if (this.detachedOffhandEquip && this.cycleOff && !this.cycleMain) {
            startDetachedOffhandRaising(player);
            return;
        }
        this.suppressOffhandRender = false;
        boolean autoWalkOnWaterBlend = CustomAnimationResolver.shouldAutoBlendFromWalkOnWater();
        if (!this.shieldOppositeEquip) {
            stopConflictingClip();
        }
        if (player != null && this.cycleOff && ((currentOff = player.method_6079()) == null || currentOff.method_7960())) {
            if (!this.cycleMain) {
                finishLoweringWithoutRaise(player);
                return;
            }
            this.cycleOff = false;
        }
        EquipSelection selection = resolveHandInClip(player);
        AnimationClip clip = selection.clip;
        boolean splitDualHandIn = LOG_OFFHAND_DETACH_DEBUG;
        EquipSelection splitOffSelection = LOG_OFFHAND_DETACH_DEBUG;
        if (this.cycleMain && this.cycleOff && player != null) {
            class_1799 mainTarget = resolveRaiseTargetStack(class_1268.field_5808, this.pendingMain, this.renderedMain, player);
            class_1799 offTarget = resolveRaiseTargetStack(class_1268.field_5810, this.pendingOff, this.renderedOff, player);
            EquipSelection splitMainSelection = resolveCustomEquipClip(mainTarget, class_1268.field_5808, "hand_in");
            splitOffSelection = resolveCustomEquipClip(offTarget, class_1268.field_5810, "hand_in");
            if (splitMainSelection.clip != null && splitOffSelection.clip != null) {
                String mainClipName = clipName(splitMainSelection.clip);
                String offClipName = clipName(splitOffSelection.clip);
                if (!Objects.equals(mainClipName, offClipName)) {
                    splitDualHandIn = LOG_WALK_ON_WATER_EQUIP_DEBUG;
                    selection = splitMainSelection;
                    clip = selection.clip;
                }
            }
        }
        boolean customSingleHand = !splitDualHandIn && this.cycleMain && this.cycleOff && selection.explicitCustom && selection.hand != null;
        class_1268 customSingleHandOther = LOG_OFFHAND_DETACH_DEBUG;
        AnimationClip customSingleHandSecondary = LOG_OFFHAND_DETACH_DEBUG;
        if (customSingleHand) {
            class_310 client = class_310.method_1551();
            if (selection.hand == class_1268.field_5808) {
                class_1268Var = class_1268.field_5810;
            } else {
                class_1268Var = class_1268.field_5808;
            }
            class_1268 other = class_1268Var;
            AnimationClip defaultClip = PunchyAnimationManager.resolveHandedClip("hand_in", client, other);
            class_1306 otherArm = player == null ? null : resolveArm(player, other);
            if (defaultClip != null && !clipHasTracksForArm(selection.clip, otherArm)) {
                customSingleHandOther = other;
                customSingleHandSecondary = defaultClip;
                clip = selection.clip;
                CustomAnimationResolver.applyLoopSpeed(selection.entry);
            } else {
                customSingleHand = LOG_OFFHAND_DETACH_DEBUG;
                CustomAnimationResolver.applyLoopSpeed(selection.entry);
            }
        }
        if (clip == null) {
            CustomAnimationResolver.applyLoopSpeed(null);
            class_310 client2 = class_310.method_1551();
            class_1268 hand = (!this.cycleOff || this.cycleMain) ? class_1268.field_5808 : class_1268.field_5810;
            clip = PunchyAnimationManager.resolveHandedClip("hand_in", client2, hand);
        } else if (!customSingleHand) {
            CustomAnimationResolver.applyLoopSpeed(selection.entry);
        }
        if (clip == null) {
            if (this.cycleMain) {
                ItemAnimationManager.forceTypeOnce(class_1268.field_5808, "hand_in");
            }
            if (this.cycleOff) {
                ItemAnimationManager.forceTypeOnce(class_1268.field_5810, "hand_in");
            }
            PunchyAnimationManager.clearAllEquipOverrides();
            clearSecondaryClipUnlessPreservingWalkOnWater("startRaising:noClip");
            this.phase = Phase.IDLE;
            this.cycleMain = false;
            this.cycleOff = false;
            this.blacklistTransitionHandIn = false;
            this.pickupRevealMainOnRaise = false;
            this.pickupRevealOffOnRaise = false;
            return;
        }
        this.activeHandInClip = clip;
        applyEquipOverrides(player);
        if (!this.shieldOppositeEquip && (!autoWalkOnWaterBlend || !this.cycleMain || this.cycleOff)) {
            clearSecondaryClipUnlessPreservingWalkOnWater("startRaising");
        }
        class_1306 poseArm = resolveActiveArm(player);
        if (customSingleHand) {
            poseArm = resolveArm(player, selection.hand);
        }
        boolean forceSingleArmEquip = this.cycleMain ^ this.cycleOff;
        boolean customDualHandedEquip = selection.explicitCustom && PunchyAnimationManager.hasAnyDualHandedEffect(clip);
        boolean ignoreDynamicDualHanded = splitDualHandIn || autoWalkOnWaterBlend || (forceSingleArmEquip && !customDualHandedEquip);
        if (splitDualHandIn) {
            poseArm = resolveArm(player, class_1268.field_5808);
        }
        if (!ignoreDynamicDualHanded && PunchyAnimationManager.isDualHandedActive(class_310.method_1551(), clip, 0.0f)) {
            poseArm = LOG_OFFHAND_DETACH_DEBUG;
        }
        this.activeHandInArm = customSingleHand ? null : poseArm;
        float speedMultiplier = resolveEquipSpeedMultiplier();
        if (this.shieldOppositeEquip) {
            PunchyAnimationManager.POSE_HANDLER.setSecondaryClipAtTime(clip, poseArm, speedMultiplier, 0.0f);
            this.blacklistTransitionHandIn = false;
            this.phase = Phase.RAISING;
            return;
        }
        PunchyAnimationManager.POSE_HANDLER.setIgnoreDualHanded(ignoreDynamicDualHanded);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(poseArm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        preserveWalkOnWaterOffhandDuringMainhandEquip(player, autoWalkOnWaterBlend);
        if (splitDualHandIn && splitOffSelection != null && splitOffSelection.clip != null && player != null) {
            class_1306 offArm = resolveArm(player, class_1268.field_5810);
            PunchyAnimationManager.POSE_HANDLER.requestClearArm(offArm);
            PunchyAnimationManager.POSE_HANDLER.setSecondaryClip(splitOffSelection.clip, offArm, speedMultiplier);
        }
        if (customSingleHand && customSingleHandSecondary != null && customSingleHandOther != null) {
            class_1306 otherArm2 = resolveArm(player, customSingleHandOther);
            PunchyAnimationManager.POSE_HANDLER.setSecondaryClip(customSingleHandSecondary, otherArm2, speedMultiplier);
        }
        boolean skipBlendIn = this.blacklistTransitionHandIn || selection.entry == null || CustomAnimationResolver.shouldSkipBlendIn(selection.entry);
        if (autoWalkOnWaterBlend || CustomAnimationResolver.shouldBlendFromCurrentOrigin(selection.entry, clip) || shouldBlendHandInFromCurrentDuringSwim()) {
            if (autoWalkOnWaterBlend) {
                PunchyAnimationManager.POSE_HANDLER.setRebaseReturnSpeedNext(0.95f);
            }
            if (poseArm == null) {
                PunchyAnimationManager.POSE_HANDLER.setRebaseUseBothArmsForNextPlay(true);
            }
            PunchyAnimationManager.POSE_HANDLER.playAtTimeRebasedFromCurrentReturnToOrigin(clip, speedMultiplier, false, 0.0f);
        } else {
            PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, skipBlendIn, false);
        }
        this.blacklistTransitionHandIn = false;
        this.phase = Phase.RAISING;
    }

    private void applyPending(class_746 player) {
        boolean deferMainForPickupReveal = this.pickupRevealMainOnRaise && this.phase == Phase.LOWERING;
        boolean deferOffForPickupReveal = this.pickupRevealOffOnRaise && this.phase == Phase.LOWERING;
        if (this.pendingMainChange && !deferMainForPickupReveal) {
            this.renderedMain = copyStack(this.pendingMain);
            this.pendingMainChange = false;
            this.pickupRevealMainOnRaise = false;
        }
        if (this.pendingOffChange && !deferOffForPickupReveal) {
            this.renderedOff = copyStack(this.pendingOff);
            this.pendingOffChange = false;
            this.pickupRevealOffOnRaise = false;
        }
        applyEquipOverrides(player);
    }

    private void revealPickupStackOnRaise(class_746 player) {
        boolean changed = LOG_OFFHAND_DETACH_DEBUG;
        if (this.phase != Phase.RAISING) {
            return;
        }
        if (this.pickupRevealMainOnRaise && this.pendingMainChange) {
            this.renderedMain = copyStack(this.pendingMain);
            this.pendingMainChange = false;
            this.pickupRevealMainOnRaise = false;
            changed = LOG_WALK_ON_WATER_EQUIP_DEBUG;
        }
        if (this.pickupRevealOffOnRaise && this.pendingOffChange) {
            this.renderedOff = copyStack(this.pendingOff);
            this.pendingOffChange = false;
            this.pickupRevealOffOnRaise = false;
            changed = LOG_WALK_ON_WATER_EQUIP_DEBUG;
        }
        if (changed) {
            applyEquipOverrides(player);
        }
    }

    private void applyEquipOverrides(class_746 player) {
        class_1306 mainArm = resolveArm(player, class_1268.field_5808);
        class_1306 offArm = resolveArm(player, class_1268.field_5810);
        PunchyAnimationManager.setEquipOverride(mainArm, this.renderedMain);
        if (this.cycleOff || !this.renderedOff.method_7960()) {
            if (this.suppressOffhandRender && (this.renderedOff == null || this.renderedOff.method_7960())) {
                PunchyAnimationManager.clearEquipOverride(offArm);
                return;
            }
            PunchyAnimationManager.setEquipOverride(offArm, this.renderedOff);
        } else {
            PunchyAnimationManager.clearEquipOverride(offArm);
        }
        if (this.detachedOffhandEquip && this.cycleOff && !this.cycleMain) {
            PunchyAnimationManager.POSE_HANDLER.requestClearArm(offArm);
        }
    }

    private boolean updatePendingIfChanged(class_746 player, class_1799 currentMain, class_1799 currentOff, boolean hotbarChanged) {
        boolean mainChanged;
        boolean offChanged;
        class_1799 previousMain = this.pendingMainChange ? this.pendingMain : this.renderedMain;
        class_1799 previousOff = this.pendingOffChange ? this.pendingOff : this.renderedOff;
        if (this.pendingMainChange) {
            mainChanged = hasItemChanged(this.pendingMain, currentMain);
        } else {
            mainChanged = hasItemChanged(this.renderedMain, currentMain) || (hotbarChanged && shouldAnimateHotbarChange(this.renderedMain, currentMain));
        }
        if (this.pendingOffChange) {
            offChanged = hasItemChanged(this.pendingOff, currentOff);
        } else {
            offChanged = hasItemChanged(this.renderedOff, currentOff);
        }
        recordFunctionalSwapIfNeeded(player, previousMain, previousOff, currentMain, currentOff, mainChanged, offChanged, hotbarChanged);
        if (mainChanged) {
            this.pendingMainChange = true;
            this.pendingMain = copyStack(currentMain);
        }
        if (offChanged) {
            this.pendingOffChange = true;
            this.pendingOff = copyStack(currentOff);
        }
        return mainChanged || offChanged;
    }

    private boolean shouldBlockEquip(class_310 client, class_1799 currentMain, boolean mainChanged, boolean offChanged) {
        if (client == null || client.field_1724 == null || TridentStateMachine.isRecentActionBlockingEquip(client) || isLoopingActionClip() || ConsumeStateMachine.isEquipBlockedByConsumeAnimation()) {
            return true;
        }
        if ((client.field_1724.method_6115() && !isUsingStaleSwappedItem(client.field_1724) && !isOppositeHandSwapDuringShieldUse(client.field_1724, mainChanged, offChanged)) || isBlockingEquipClipActive(currentMain)) {
            return true;
        }
        return false;
    }

    private boolean isUsingStaleSwappedItem(class_746 player) {
        class_1799 useStack;
        class_1268 usedHand;
        if (player == null || !player.method_6115() || (useStack = player.method_6030()) == null || useStack.method_7960() || (usedHand = player.method_6058()) == null) {
            return false;
        }
        class_1799 currentStack = player.method_5998(usedHand);
        return currentStack == null || currentStack.method_7960() || !class_1799.method_31577(currentStack, useStack);
    }

    private boolean isOppositeHandSwapDuringShieldUse(class_746 player, boolean mainChanged, boolean offChanged) {
        class_1268 usedHand;
        if (player == null || (usedHand = resolveActiveShieldHand(player)) == null) {
            return false;
        }
        return (usedHand == class_1268.field_5810 && mainChanged && !offChanged) || (usedHand == class_1268.field_5808 && offChanged && !mainChanged);
    }

    private boolean shouldRunOppositeHandEquipOverShield(class_746 player) {
        return isOppositeHandSwapDuringShieldUse(player, this.cycleMain, this.cycleOff);
    }

    private boolean shouldDeferRenderedSyncDuringShieldOppositeEquip() {
        return this.shieldOppositeEquip && this.phase != Phase.IDLE && ((this.cycleMain && !this.cycleOff) || (this.cycleOff && !this.cycleMain));
    }

    private class_1268 resolveActiveShieldHand(class_746 player) {
        class_1799 useStack;
        if (player == null) {
            return null;
        }
        if (player.method_6115() && (useStack = player.method_6030()) != null && (useStack.method_7909() instanceof class_1819)) {
            return player.method_6058();
        }
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1690 == null || !client.field_1690.field_1904.method_1434()) {
            return null;
        }
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (!PunchyAnimationManager.POSE_HANDLER.isPlaying() || !PunchyAnimationManager.isClipNamed(clip, "use_shield")) {
            return null;
        }
        if (player.method_6079().method_7909() instanceof class_1819) {
            return class_1268.field_5810;
        }
        if (player.method_6047().method_7909() instanceof class_1819) {
            return class_1268.field_5808;
        }
        return null;
    }

    private boolean isInterruptedByOtherClip() {
        AnimationClip secondary;
        AnimationClip secondary2;
        if (this.detachedOffhandEquip && this.cycleOff && !this.cycleMain && (secondary2 = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip()) != null && isEquipClip(secondary2)) {
            return false;
        }
        if (this.shieldOppositeEquip && (secondary = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip()) != null && isEquipClip(secondary)) {
            return false;
        }
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        return (isBlockingEquipClip(clip) || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || isEquipClip(clip)) ? false : true;
    }

    private void stopConflictingClip() {
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (clip == null || isEquipClip(clip) || isBlockingEquipClip(clip)) {
            return;
        }
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
        PunchyAnimationManager.clearSourceHand();
    }

    public static boolean isEquipInProgress() {
        return equipActive;
    }

    public static boolean isOppositeShieldEquipInProgress(class_1268 shieldHand) {
        if (instance == null || shieldHand == null || !instance.shieldOppositeEquip) {
            return false;
        }
        return (shieldHand == class_1268.field_5810 && instance.cycleMain && !instance.cycleOff) || (shieldHand == class_1268.field_5808 && instance.cycleOff && !instance.cycleMain);
    }

    public static String getActiveType() {
        return activeType;
    }

    public static boolean shouldTreatHandOutActive() {
        return "hand_out".equals(activeType) || handOutGraceTicks > 0;
    }

    public static boolean shouldAllowVanillaHandOut(class_1268 hand) {
        if (isDualHandedPoseBlockingVanillaHandOut()) {
            return false;
        }
        class_1268 resolved = hand == null ? class_1268.field_5808 : hand;
        return resolved == class_1268.field_5810 ? vanillaHandOutOffTicks > 0 : vanillaHandOutMainTicks > 0;
    }

    public static boolean shouldKeepOffhandVisibleUntilHandOutEnds() {
        return instance != null && instance.phase == Phase.LOWERING && instance.cycleOff && instance.pendingOffChange && (instance.pendingOff == null || instance.pendingOff.method_7960());
    }

    public static boolean isOffhandEquipTransitionActive() {
        if (instance == null || instance.phase == Phase.IDLE) {
            return false;
        }
        return instance.cycleOff || instance.pendingOffChange || instance.offhandRevealPending || instance.offhandAwaitingHandIn;
    }

    public static boolean cancelOffhandTransitionForPoseOverride(class_746 player) {
        if (instance == null || player == null) {
            return false;
        }
        return instance.cancelOffhandTransitionForPoseOverrideInternal(player);
    }

    public static boolean shouldHideIncomingOffhandUntilHandInStarts(class_746 player) {
        class_1799 currentOff;
        if (instance == null || player == null || (currentOff = player.method_6079()) == null || currentOff.method_7960()) {
            return false;
        }
        if (instance.isHandInActive()) {
            class_1306 offArm = instance.resolveArm(player, class_1268.field_5810);
            boolean offhandHandInActive = instance.cycleOff || instance.activeHandInArm == null || instance.activeHandInArm == offArm;
            if (offhandHandInActive) {
                return false;
            }
        }
        boolean renderedEmpty = instance.renderedOff == null || instance.renderedOff.method_7960();
        boolean pendingHasItem = (instance.pendingOff == null || instance.pendingOff.method_7960()) ? false : true;
        return instance.offhandRevealPending || (instance.offhandAwaitingHandIn && !instance.isHandInActive()) || (renderedEmpty && !pendingHasItem);
    }

    public static class_1799 getVisualOffhandStack(class_746 player) {
        if (player == null) {
            return class_1799.field_8037;
        }
        if (instance == null) {
            class_1799 off = player.method_6079();
            return off == null ? class_1799.field_8037 : off.method_7972();
        }
        boolean offhandTransitionActive = instance.cycleOff || instance.pendingOffChange || instance.detachedOffhandEquip || instance.offhandRevealPending || instance.offhandAwaitingHandIn;
        if (offhandTransitionActive) {
            if (instance.renderedOff != null && !instance.renderedOff.method_7960()) {
                return instance.renderedOff.method_7972();
            }
            if (instance.pendingOff != null && !instance.pendingOff.method_7960()) {
                return instance.pendingOff.method_7972();
            }
        }
        class_1799 off2 = player.method_6079();
        return off2 == null ? class_1799.field_8037 : off2.method_7972();
    }

    private static boolean isDualHandedPoseBlockingVanillaHandOut() {
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        return clip != null && PunchyAnimationManager.POSE_HANDLER.isPlaying() && PunchyAnimationManager.hasAnyDualHandedEffect(clip);
    }

    public static boolean abortEquipForAction(class_310 client) {
        return abortEquipForAction(client, null);
    }

    public static boolean abortEquipForAction(class_310 client, class_1268 hand) {
        if (instance == null || client == null || client.field_1724 == null) {
            return false;
        }
        return instance.abortEquipForActionInternal(client.field_1724, hand);
    }

    public static boolean prepareOffhandForMainhandAction(class_310 client) {
        if (instance == null || client == null || client.field_1724 == null) {
            return false;
        }
        return instance.prepareDetachedOffhandForMainhandAction(client.field_1724);
    }

    public static void requestFlightOffhandLandingHide(class_746 player) {
        if (instance == null || player == null) {
            return;
        }
        instance.requestFlightOffhandLandingHideInternal(player);
    }

    private boolean abortEquipForActionInternal(class_746 player, class_1268 hand) {
        if (!equipActive) {
            return false;
        }
        if (isOppositeShieldEquipInProgress(hand)) {
            return true;
        }
        if (hand == class_1268.field_5808 && prepareDetachedOffhandForMainhandAction(player)) {
            return true;
        }
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        class_1306 actionArm = hand == null ? null : resolveArm(player, hand);
        if (clip != null && isEquipClip(clip) && PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
            class_1306 equipArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
            if (actionArm != null && equipArm != null && equipArm != actionArm) {
                return true;
            }
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            PunchyAnimationManager.clearSourceHand();
        }
        resetToCurrent(player);
        return true;
    }

    private boolean prepareDetachedOffhandForMainhandAction(class_746 player) {
        class_1306 offArm;
        PoseHandlerState preserved;
        if (player == null || this.detachedOffhandEquip || !this.cycleOff || (preserved = captureOffhandEquipState((offArm = resolveArm(player, class_1268.field_5810)))) == null) {
            return false;
        }
        this.renderedMain = copyStack(player.method_6047());
        this.pendingMain = copyStack(player.method_6047());
        this.pendingMainChange = false;
        this.cycleMain = false;
        this.skipNextEquipMain = false;
        this.pendingBlacklistHandInMain = false;
        vanillaHandOutMainTicks = LOG_OFFHAND_DETACH_DEBUG;
        clearPendingMainActionClear();
        this.detachedOffhandEquip = true;
        if (this.phase == Phase.LOWERING) {
            this.activeHandOutClip = preserved.clip();
            this.swapTriggered = true;
            applyPending(player);
            startDetachedOffhandRaising(player);
            debugOffhandDetach("prepareDetachedOffhandForMainhandAction.forceRaise", player, player.method_6079());
            return true;
        }
        if (this.phase == Phase.RAISING) {
            this.activeHandInClip = preserved.clip();
            this.activeHandInArm = offArm;
        }
        PunchyAnimationManager.POSE_HANDLER.setSecondaryClipAtTime(preserved.clip(), offArm, resolveEquipSpeedMultiplier(), preserved.time());
        debugOffhandDetach("prepareDetachedOffhandForMainhandAction", player, player.method_6079());
        applyEquipOverrides(player);
        return true;
    }

    private void requestFlightOffhandLandingHideInternal(class_746 player) {
        if (player == null || this.phase != Phase.IDLE) {
            return;
        }
        class_1799 currentOff = player.method_6079();
        if ((currentOff != null && !currentOff.method_7960()) || player.method_6128() || player.method_31549().field_7479 || !player.method_24828() || this.pendingMainChange || this.pendingOffChange || this.cycleMain || this.cycleOff) {
            return;
        }
        this.pendingMainChange = false;
        this.pendingOffChange = true;
        this.pendingOff = class_1799.field_8037;
        this.suppressOffhandRender = false;
        this.offhandRevealPending = false;
        this.offhandAwaitingHandIn = false;
        startLowering(player);
    }

    private PoseHandlerState captureOffhandEquipState(class_1306 offArm) {
        AnimationClip secondary = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
        if (secondary != null && isEquipClip(secondary) && PunchyAnimationManager.POSE_HANDLER.getSecondaryArm() == offArm) {
            return new PoseHandlerState(secondary, PunchyAnimationManager.POSE_HANDLER.getSecondaryTime());
        }
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (current == null || !isEquipClip(current)) {
            return null;
        }
        class_1306 activeArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
        if (activeArm != null && activeArm != offArm) {
            return null;
        }
        return new PoseHandlerState(current, PunchyAnimationManager.POSE_HANDLER.getCurrentTime());
    }

    private boolean isEquipClip(AnimationClip clip) {
        if (clip == null) {
            return false;
        }
        if (clip == this.activeHandInClip || clip == this.activeHandOutClip) {
            return true;
        }
        String name = clip.getName();
        return (name != null && (name.equals("hand_in") || name.equals("hand_out") || name.startsWith("hand_in_") || name.startsWith("hand_out_"))) || PunchyAnimationManager.isClipNamed(clip, "hand_in") || PunchyAnimationManager.isClipNamed(clip, "hand_out");
    }

    private boolean isLoopingActionClip() {
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (clip == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || isEquipClip(clip) || isBlockingEquipClip(clip) || isInspectLikeClip(clip)) {
            return false;
        }
        return clip.isLoop() || PunchyAnimationManager.POSE_HANDLER.isForceLooping();
    }

    private boolean isActionClipActive() {
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (clip == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || isEquipClip(clip) || isBlockingEquipClip(clip)) {
            return false;
        }
        return true;
    }

    private boolean updatePendingActionClearMain(class_1799 currentMain, class_1799 currentOff) {
        if (currentMain != null && !currentMain.method_7960()) {
            clearPendingMainActionClear();
            return false;
        }
        if (this.pendingMainActionClear) {
            if (shouldFinishPendingActionClear(this.pendingMainActionClearClip, this.pendingMainActionClearTime)) {
                this.renderedMain = class_1799.field_8037;
                this.pendingMainChange = false;
                clearPendingMainActionClear();
                return false;
            }
            return true;
        }
        if (!shouldDelayActionClear(currentMain, currentOff, this.renderedMain, this.renderedOff, this.pendingMainChange, this.cycleMain)) {
            return false;
        }
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        this.pendingMainActionClear = true;
        this.pendingMainActionClearClip = clip;
        this.pendingMainActionClearTime = resolveActionClearTime(clip);
        return true;
    }

    private boolean updatePendingActionClearOff(class_1799 currentOff, class_1799 currentMain) {
        if (currentOff != null && !currentOff.method_7960()) {
            clearPendingOffActionClear();
            return false;
        }
        if (this.pendingOffActionClear) {
            if (shouldFinishPendingActionClear(this.pendingOffActionClearClip, this.pendingOffActionClearTime)) {
                this.renderedOff = class_1799.field_8037;
                this.pendingOffChange = false;
                clearPendingOffActionClear();
                return false;
            }
            return true;
        }
        if (!shouldDelayActionClear(currentOff, currentMain, this.renderedOff, this.renderedMain, this.pendingOffChange, this.cycleOff)) {
            return false;
        }
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        this.pendingOffActionClear = true;
        this.pendingOffActionClearClip = clip;
        this.pendingOffActionClearTime = resolveActionClearTime(clip);
        return true;
    }

    private boolean shouldDelayActionClear(class_1799 current, class_1799 oppositeCurrent, class_1799 rendered, class_1799 oppositeRendered, boolean pendingChange, boolean cycle) {
        if (isActionClipActive() && !isFunctionalOffhandSwap(rendered, oppositeRendered, current, oppositeCurrent, true, true, false)) {
            return ((shouldPreserveCurrentClipForHandOut() && !shouldFinishUseClipBeforeClearing(current, rendered)) || this.phase != Phase.IDLE || pendingChange || cycle || current == null || !current.method_7960() || rendered == null || rendered.method_7960()) ? false : true;
        }
        return false;
    }

    private boolean shouldFinishUseClipBeforeClearing(class_1799 current, class_1799 rendered) {
        AnimationClip clip;
        if (current == null || !current.method_7960() || rendered == null || rendered.method_7960() || (clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
            return false;
        }
        return ItemAnimationManager.isTypeActive("useItem") || PunchyAnimationManager.isClipNamed(clip, "interact");
    }

    private float resolveActionClearTime(AnimationClip clip) {
        if (clip == null) {
            return 0.0f;
        }
        return Math.max(0.0f, clip.getLength() * 0.5f);
    }

    private boolean shouldFinishPendingActionClear(AnimationClip expectedClip, float clearTime) {
        AnimationClip currentClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        return expectedClip == null || currentClip != expectedClip || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.POSE_HANDLER.getCurrentTime() >= clearTime;
    }

    private void clearPendingActionClears() {
        clearPendingMainActionClear();
        clearPendingOffActionClear();
    }

    private void clearPendingMainActionClear() {
        this.pendingMainActionClear = false;
        this.pendingMainActionClearClip = null;
        this.pendingMainActionClearTime = 0.0f;
    }

    private void clearPendingOffActionClear() {
        this.pendingOffActionClear = false;
        this.pendingOffActionClearClip = null;
        this.pendingOffActionClearTime = 0.0f;
    }

    private boolean shouldBlendHandOutFromCurrent() {
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (current != null) {
            if ((!PunchyAnimationManager.POSE_HANDLER.isPlaying() && !PunchyAnimationManager.POSE_HANDLER.isBlendOutActive()) || isBlockingEquipClip(current)) {
                return false;
            }
            if (current != this.activeHandInClip && !PunchyAnimationManager.isClipNamed(current, "hand_in") && isEquipClip(current)) {
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean shouldBlendHandInFromCurrentDuringSwim() {
        AnimationClip current;
        if (!PunchyAnimationManager.SWIM_HANDLER.isPlaying()) {
            return false;
        }
        AnimationClip swimClip = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip();
        if ((swimClip == PunchyAnimationManager.SWIMING_CLIP || swimClip == PunchyAnimationManager.SWIMING_ABOVE_CLIP) && (current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) != null) {
            if ((PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.POSE_HANDLER.isBlendOutActive()) && !isBlockingEquipClip(current)) {
                return current == this.activeHandOutClip || PunchyAnimationManager.isClipNamed(current, "hand_out");
            }
            return false;
        }
        return false;
    }

    private boolean shouldBlendHandOutFromSwim() {
        if (!PunchyAnimationManager.SWIM_HANDLER.isPlaying()) {
            return false;
        }
        AnimationClip swimClip = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip();
        if (swimClip != PunchyAnimationManager.SWIMING_CLIP && swimClip != PunchyAnimationManager.SWIMING_ABOVE_CLIP) {
            return false;
        }
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (current != null) {
            if (PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.POSE_HANDLER.isBlendOutActive()) {
                return false;
            }
            return true;
        }
        return true;
    }

    private boolean isInspectLikeClip(AnimationClip clip) {
        String name;
        if (clip == null || (name = clip.getName()) == null || name.isBlank()) {
            return false;
        }
        return name.trim().toLowerCase(Locale.ROOT).contains("inspect");
    }

    private boolean isWalkOnWaterPoseActive() {
        String name;
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (current == null) {
            return false;
        }
        if ((!PunchyAnimationManager.POSE_HANDLER.isPlaying() && !PunchyAnimationManager.POSE_HANDLER.isBlendOutActive()) || (name = current.getName()) == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("walk_on_water_start") || normalized.contains("walk_on_water_loop") || normalized.contains("walk_on_water_end");
    }

    private void preserveWalkOnWaterOffhandDuringMainhandEquip(class_746 player, boolean autoWalkOnWaterBlend) {
        if (!autoWalkOnWaterBlend || player == null || !this.cycleMain || this.cycleOff) {
            return;
        }
        class_1306 offArm = resolveArm(player, class_1268.field_5810);
        AnimationClip secondary = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
        if (secondary != null && PunchyAnimationManager.POSE_HANDLER.getSecondaryArm() == offArm) {
            syncWalkOnWaterSecondaryPause(secondary);
            return;
        }
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (current == null || !containsWalkOnWaterName(current)) {
            return;
        }
        PunchyAnimationManager.POSE_HANDLER.setSecondaryClipAtTime(current, offArm, PunchyAnimationManager.clipSpeedMultiplier(current), PunchyAnimationManager.POSE_HANDLER.getCurrentTime());
        syncWalkOnWaterSecondaryPause(current);
    }

    private void syncWalkOnWaterSecondaryPause(AnimationClip clip) {
        if (clip == null || !containsWalkOnWaterLoopName(clip)) {
            PunchyAnimationManager.POSE_HANDLER.setSecondaryPaused(false);
            return;
        }
        class_310 client = class_310.method_1551();
        boolean movementActive = (client == null || client.field_1690 == null || (!client.field_1690.field_1894.method_1434() && !client.field_1690.field_1881.method_1434() && !client.field_1690.field_1913.method_1434() && !client.field_1690.field_1849.method_1434())) ? false : true;
        PunchyAnimationManager.POSE_HANDLER.setSecondaryPaused(!movementActive);
    }

    private boolean shouldPreserveWalkOnWaterOffhandSecondary() {
        if (this.cycleOff || this.pendingOffChange || this.detachedOffhandEquip || this.offhandRevealPending || this.offhandAwaitingHandIn) {
            return false;
        }
        AnimationClip secondary = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
        class_1306 secondaryArm = PunchyAnimationManager.POSE_HANDLER.getSecondaryArm();
        if (secondary == null || secondaryArm == null || !containsWalkOnWaterName(secondary)) {
            return false;
        }
        class_310 client = class_310.method_1551();
        class_746 player = client == null ? null : client.field_1724;
        return player != null && secondaryArm == resolveArm(player, class_1268.field_5810);
    }

    private void debugWalkOnWaterEquip(String event, boolean preserveSecondary) {
        PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
    }

    private void clearSecondaryClipUnlessPreservingWalkOnWater(String event) {
        boolean preserveWalkOnWaterSecondary = shouldPreserveWalkOnWaterOffhandSecondary();
        debugWalkOnWaterEquip(event, preserveWalkOnWaterSecondary);
        if (!preserveWalkOnWaterSecondary) {
            PunchyAnimationManager.POSE_HANDLER.clearSecondaryClip();
        }
    }

    private static String debugClipName(AnimationClip clip) {
        return clip == null ? "null" : String.valueOf(clip.getName());
    }

    private boolean shouldDelayWalkOnWaterHandOut() {
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        return containsWalkOnWaterStartName(current) && PunchyAnimationManager.POSE_HANDLER.isPlaying() && PunchyAnimationManager.POSE_HANDLER.getCurrentTime() <= WALK_ON_WATER_MIN_REBASE_TIME;
    }

    private boolean containsWalkOnWaterStartName(AnimationClip clip) {
        return containsWalkOnWaterNamedPhase(clip, "walk_on_water_start");
    }

    private boolean containsWalkOnWaterLoopName(AnimationClip clip) {
        return containsWalkOnWaterNamedPhase(clip, "walk_on_water_loop");
    }

    private boolean containsWalkOnWaterName(AnimationClip clip) {
        return containsWalkOnWaterNamedPhase(clip, "walk_on_water_");
    }

    private boolean containsWalkOnWaterNamedPhase(AnimationClip clip, String needle) {
        if (clip == null || clip.getName() == null) {
            return false;
        }
        String normalized = clip.getName().trim().toLowerCase(Locale.ROOT);
        return normalized.contains(needle);
    }

    private boolean shouldPreserveCurrentClipForHandOut() {
        return shouldBlendHandOutFromCurrent();
    }

    private float resolveEquipBlendStartTime(AnimationClip clip) {
        if (clip == null) {
            return 0.0f;
        }
        float firstActiveTime = Float.POSITIVE_INFINITY;
        for (BoneTrack track : clip.getTracks().values()) {
            if (track != null) {
                Iterator<Keyframe> it = track.getKeyframes().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Keyframe keyframe = it.next();
                    if (keyframe != null) {
                        float time = keyframe.timeStamp();
                        if (time > 0.0f && !isNeutralKeyframe(keyframe)) {
                            firstActiveTime = Math.min(firstActiveTime, time);
                            break;
                        }
                    }
                }
            }
        }
        if (!Float.isFinite(firstActiveTime)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(firstActiveTime, clip.getLength()));
    }

    private boolean isNeutralKeyframe(Keyframe keyframe) {
        return isZeroVector(keyframe.position()) && isZeroVector(keyframe.rotationEuler()) && isIdentityScale(keyframe.scale());
    }

    private boolean isZeroVector(Vector3f vector) {
        return vector == null || (Math.abs(vector.x) < 1.0E-4f && Math.abs(vector.y) < 1.0E-4f && Math.abs(vector.z) < 1.0E-4f);
    }

    private boolean clipHasTracksForArm(AnimationClip clip, class_1306 arm) {
        if (clip == null || arm == null || clip.getTracks().isEmpty()) {
            return false;
        }
        for (String boneName : clip.getTracks().keySet()) {
            if (isBoneForArm(boneName, arm)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBoneForArm(String boneName, class_1306 arm) {
        if (boneName == null || arm == null) {
            return false;
        }
        String normalized = boneName.toLowerCase(Locale.ROOT);
        if (arm == class_1306.field_6182) {
            return normalized.contains("left");
        }
        return normalized.contains("right");
    }

    private boolean isIdentityScale(Vector3f vector) {
        return vector == null || (Math.abs(vector.x - 1.0f) < 1.0E-4f && Math.abs(vector.y - 1.0f) < 1.0E-4f && Math.abs(vector.z - 1.0f) < 1.0E-4f);
    }

    private boolean isBlockingEquipClip(AnimationClip clip) {
        return PunchyAnimationManager.isClipNamed(clip, "use_trident_throw") || PunchyAnimationManager.isClipNamed(clip, "use_trident_throw_loyalty") || PunchyAnimationManager.isClipNamed(clip, "use_trident_throw_riptide") || PunchyAnimationManager.isClipNamed(clip, "use_crossbow_shot");
    }

    private boolean isBlockingEquipClipActive(class_1799 currentMain) {
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (!PunchyAnimationManager.POSE_HANDLER.isPlaying() || !isBlockingEquipClip(clip)) {
            return false;
        }
        return true;
    }

    private void clearRenderedEmptyHandsWhileUseBlocksEquip(class_746 player, class_1799 currentMain, class_1799 currentOff, boolean mainChanged, boolean offChanged) {
        if (player == null || !player.method_6115()) {
            return;
        }
        class_1268 usedHand = player.method_6058();
        if (mainChanged && usedHand != class_1268.field_5808 && (currentMain == null || currentMain.method_7960())) {
            this.renderedMain = class_1799.field_8037;
        }
        if (offChanged && usedHand != class_1268.field_5810) {
            if (currentOff == null || currentOff.method_7960()) {
                this.renderedOff = class_1799.field_8037;
            }
        }
    }

    private void resumeEquipClip(class_746 player) {
        if (player == null) {
            return;
        }
        if (this.phase == Phase.LOWERING && !isClipPlaying(this.activeHandOutClip)) {
            startLowering(player);
        } else if (this.phase == Phase.RAISING && !isClipPlaying(this.activeHandInClip)) {
            startRaising(player);
        }
    }

    private boolean shouldSkipRaiseForOffhandRemovalBefore() {
        if (!this.cycleMain && this.cycleOff && this.pendingOffChange) {
            return this.pendingOff == null || this.pendingOff.method_7960();
        }
        return false;
    }

    private boolean shouldSkipRaiseForOffhandRemovalAfter(class_746 player) {
        if (this.cycleMain || !this.cycleOff) {
            return false;
        }
        if (this.renderedOff == null || this.renderedOff.method_7960()) {
            class_1799 currentOff = player == null ? class_1799.field_8037 : player.method_6079();
            return currentOff == null || currentOff.method_7960();
        }
        return false;
    }

    private void finishLoweringWithoutRaise(class_746 player) {
        this.phase = Phase.IDLE;
        this.cycleMain = false;
        this.cycleOff = false;
        this.swapTriggered = false;
        this.suppressOffhandRender = false;
        this.offhandRevealPending = false;
        this.offhandAwaitingHandIn = false;
        this.activeHandOutClip = null;
        this.detachedOffhandEquip = false;
        this.shieldOppositeEquip = false;
        this.pendingBlacklistHandInMain = false;
        this.pendingBlacklistHandInOff = false;
        this.blacklistTransitionHandIn = false;
        vanillaHandOutMainTicks = LOG_OFFHAND_DETACH_DEBUG;
        vanillaHandOutOffTicks = LOG_OFFHAND_DETACH_DEBUG;
        boolean preserveWalkOnWaterSecondary = shouldPreserveWalkOnWaterOffhandSecondary();
        debugWalkOnWaterEquip("finishLoweringWithoutRaise", preserveWalkOnWaterSecondary);
        PunchyAnimationManager.POSE_HANDLER.setIgnoreDualHanded(false);
        if (!preserveWalkOnWaterSecondary) {
            PunchyAnimationManager.POSE_HANDLER.clearSecondaryClip();
        }
        markEquipTransitionTick(player);
        applyEquipOverrides(player);
        if (player != null) {
            PunchyClient.getWalkOnWaterStateMachine().reattachMainhandAfterEquip(player);
        }
        if (this.pendingMainChange || this.pendingOffChange) {
            startLowering(player);
        }
    }

    private void markEquipTransitionTick(class_746 player) {
        if (player == null) {
            return;
        }
        lastEquipTransitionPlayerTick = player.field_6012;
    }

    public static boolean wasEquipTransitionRecent(class_310 client, int maxTicks) {
        int mark;
        int now;
        return client != null && client.field_1724 != null && maxTicks >= 0 && (mark = lastEquipTransitionPlayerTick) >= 0 && (now = client.field_1724.field_6012) >= mark && now - mark <= maxTicks;
    }

    private boolean shouldBypassTridentThrowBlock(class_1799 currentMain) {
        if (currentMain == null || currentMain.method_7960()) {
            return false;
        }
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (!PunchyAnimationManager.isClipNamed(clip, "use_trident_throw") && !PunchyAnimationManager.isClipNamed(clip, "use_trident_throw_loyalty") && !PunchyAnimationManager.isClipNamed(clip, "use_trident_throw_riptide")) {
            return false;
        }
        return hasItemChanged(this.renderedMain, currentMain);
    }

    public boolean isOffhandRenderSuppressed() {
        if (UseActionTracker.wasRecentUseForHand(class_1268.field_5810, OFFHAND_USE_UNSUPPRESS_WINDOW_NANOS)) {
            return false;
        }
        return this.suppressOffhandRender || (this.offhandAwaitingHandIn && !isHandInActive());
    }

    public static boolean shouldHideOffhandTridentThrowCacheRender() {
        HandEquipStateMachine instance2 = instance;
        return instance2 != null && instance2.hideOffhandTridentThrowCache && instance2.phase == Phase.LOWERING && instance2.cycleOff;
    }

    public boolean isHandInActiveForOffhand() {
        return isHandInActive();
    }

    private boolean shouldSuppressOffhandHandOut(class_746 player, class_1799 currentOff) {
        if (isFabricPlatform()) {
            return currentOff == null || currentOff.method_7960();
        }
        return false;
    }

    private static boolean isFabricPlatform() {
        try {
            if (Services.PLATFORM != null) {
                if ("fabric".equalsIgnoreCase(Services.PLATFORM.getPlatformName())) {
                    return true;
                }
            }
            return false;
        } catch (Throwable th) {
            return false;
        }
    }

    public boolean shouldLowerOffhand(class_746 player) {
        if (player == null || UseActionTracker.wasRecentUseForHand(class_1268.field_5810, OFFHAND_USE_UNSUPPRESS_WINDOW_NANOS)) {
            return false;
        }
        if (this.suppressOffhandRender || this.offhandRevealPending) {
            return true;
        }
        if (this.offhandAwaitingHandIn) {
            if (!isHandInActive()) {
                return true;
            }
            class_1306 offArm = resolveArm(player, class_1268.field_5810);
            return (this.activeHandInArm == null || this.activeHandInArm == offArm) ? false : true;
        }
        return false;
    }

    private boolean shouldSuppressOffhandRender(class_1799 currentOff) {
        if (this.offhandRevealPending) {
            return true;
        }
        if (this.phase != Phase.LOWERING || !this.cycleOff) {
            return false;
        }
        if (this.renderedOff == null || this.renderedOff.method_7960()) {
            return true;
        }
        return false;
    }

    private void updateOffhandRevealPending(class_746 player, class_1799 currentOff, boolean offChanged) {
        if (isHandInActive() && shouldClearOffhandReveal(player)) {
            this.offhandRevealPending = false;
            return;
        }
        boolean renderedEmpty = this.renderedOff == null || this.renderedOff.method_7960();
        boolean currentHasItem = (currentOff == null || currentOff.method_7960()) ? false : true;
        if (renderedEmpty && currentHasItem && (offChanged || this.pendingOffChange)) {
            this.offhandRevealPending = true;
            return;
        }
        if (currentHasItem) {
            return;
        }
        if ((this.pendingOff == null || this.pendingOff.method_7960()) && !this.pendingOffChange) {
            this.offhandRevealPending = false;
        }
    }

    private void updateOffhandAwaitingHandIn(class_746 player, class_1799 currentOff) {
        boolean handInActive = isHandInActive();
        if (handInActive && shouldClearOffhandReveal(player)) {
            this.offhandAwaitingHandIn = false;
            this.lastOffhandEmpty = currentOff == null || currentOff.method_7960();
            return;
        }
        boolean currentEmpty = currentOff == null || currentOff.method_7960();
        if (this.lastOffhandEmpty && !currentEmpty) {
            this.offhandAwaitingHandIn = true;
        } else if (currentEmpty) {
            this.offhandAwaitingHandIn = false;
        }
        this.lastOffhandEmpty = currentEmpty;
    }

    private boolean isHandInActive() {
        AnimationClip currentClip;
        boolean detachedOffhandHandIn = this.detachedOffhandEquip && this.cycleOff && !this.cycleMain;
        if (detachedOffhandHandIn) {
            currentClip = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
        } else {
            currentClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        }
        AnimationClip clip = currentClip;
        return this.phase == Phase.RAISING && clip != null && clip == this.activeHandInClip && (detachedOffhandHandIn || PunchyAnimationManager.POSE_HANDLER.isPlaying());
    }

    private boolean shouldClearOffhandReveal(class_746 player) {
        if (player == null || this.cycleOff) {
            return true;
        }
        class_1306 offArm = resolveArm(player, class_1268.field_5810);
        return this.activeHandInArm != null && this.activeHandInArm == offArm;
    }

    private boolean cancelOffhandTransitionForPoseOverrideInternal(class_746 player) {
        if (player == null) {
            return false;
        }
        class_1306 offArm = resolveArm(player, class_1268.field_5810);
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        AnimationClip secondary = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
        boolean primaryOffhandEquip = current != null && PunchyAnimationManager.POSE_HANDLER.isPlaying() && isEquipClip(current) && PunchyAnimationManager.POSE_HANDLER.getActiveArm() == offArm;
        boolean secondaryOffhandEquip = secondary != null && isEquipClip(secondary);
        boolean offhandStateActive = this.cycleOff || this.pendingOffChange || this.offhandRevealPending || this.offhandAwaitingHandIn || primaryOffhandEquip || secondaryOffhandEquip;
        if (!offhandStateActive) {
            return false;
        }
        this.pendingOff = copyStack(player.method_6079());
        this.renderedOff = copyStack(player.method_6079());
        this.pendingOffChange = false;
        this.cycleOff = false;
        this.suppressOffhandRender = false;
        this.offhandRevealPending = false;
        this.offhandAwaitingHandIn = false;
        this.detachedOffhandEquip = false;
        this.shieldOppositeEquip = false;
        this.pendingBlacklistHandInOff = false;
        vanillaHandOutOffTicks = LOG_OFFHAND_DETACH_DEBUG;
        if (primaryOffhandEquip) {
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            PunchyAnimationManager.clearSourceHand();
        }
        if (secondaryOffhandEquip) {
            clearSecondaryClipUnlessPreservingWalkOnWater("finishPrimaryOffhandEquip");
        }
        if (!this.cycleMain) {
            this.phase = Phase.IDLE;
            this.activeHandInClip = null;
            this.activeHandOutClip = null;
            this.activeHandInArm = null;
        }
        applyEquipOverrides(player);
        return true;
    }

    private void startDetachedOffhandRaising(class_746 player) {
        class_1799 class_1799VarMethod_6079;
        this.suppressOffhandRender = false;
        if (player != null) {
            if (this.pendingOff != null && !this.pendingOff.method_7960()) {
                class_1799VarMethod_6079 = this.pendingOff;
            } else {
                class_1799VarMethod_6079 = player.method_6079();
            }
            class_1799 targetOff = class_1799VarMethod_6079;
            if (targetOff == null || targetOff.method_7960()) {
                finishLoweringWithoutRaise(player);
                return;
            }
            this.renderedOff = copyStack(targetOff);
            this.pendingOff = copyStack(targetOff);
            this.pendingOffChange = false;
            debugOffhandDetach("startDetachedOffhandRaising.sync", player, targetOff);
        }
        EquipSelection selection = resolveHandInClip(player);
        AnimationClip clip = selection.clip;
        if (clip == null) {
            CustomAnimationResolver.applyLoopSpeed(null);
            clip = PunchyAnimationManager.resolveHandedClip("hand_in", class_310.method_1551(), class_1268.field_5810);
        } else {
            CustomAnimationResolver.applyLoopSpeed(selection.entry);
        }
        if (clip == null) {
            ItemAnimationManager.forceTypeOnce(class_1268.field_5810, "hand_in");
            this.phase = Phase.IDLE;
            this.cycleOff = false;
            this.detachedOffhandEquip = false;
            this.shieldOppositeEquip = false;
            this.blacklistTransitionHandIn = false;
            return;
        }
        this.activeHandInClip = clip;
        this.activeHandInArm = player == null ? null : resolveArm(player, class_1268.field_5810);
        applyEquipOverrides(player);
        if (this.activeHandInArm != null) {
            PunchyAnimationManager.POSE_HANDLER.requestClearArm(this.activeHandInArm);
        }
        PunchyAnimationManager.POSE_HANDLER.setSecondaryClipAtTime(clip, this.activeHandInArm, resolveEquipSpeedMultiplier(), 0.0f);
        debugOffhandDetach("startDetachedOffhandRaising.play", player, player != null ? player.method_6079() : class_1799.field_8037);
        this.blacklistTransitionHandIn = false;
        this.phase = Phase.RAISING;
    }

    private void debugOffhandTransition(String event, class_746 player) {
    }

    private boolean tryDetachOffhandDuringAction(class_746 player) {
        if (player == null || this.detachedOffhandEquip || !this.cycleOff) {
            return false;
        }
        AnimationClip secondary = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
        class_1306 offArm = resolveArm(player, class_1268.field_5810);
        if (secondary == null || !isEquipClip(secondary) || PunchyAnimationManager.POSE_HANDLER.getSecondaryArm() != offArm) {
            return false;
        }
        this.renderedMain = copyStack(player.method_6047());
        this.pendingMain = copyStack(player.method_6047());
        this.pendingMainChange = false;
        this.cycleMain = false;
        this.skipNextEquipMain = false;
        this.pendingBlacklistHandInMain = false;
        vanillaHandOutMainTicks = LOG_OFFHAND_DETACH_DEBUG;
        clearPendingMainActionClear();
        this.detachedOffhandEquip = true;
        if (this.phase == Phase.RAISING) {
            this.activeHandInArm = offArm;
        }
        applyEquipOverrides(player);
        debugOffhandDetach("tryDetachOffhandDuringAction.detach", player, player.method_6079());
        return true;
    }

    private void logOffhandState(class_746 player, class_1799 currentOff) {
    }

    private boolean shouldSwapNow() {
        if ((this.detachedOffhandEquip && this.cycleOff && !this.cycleMain) || this.shieldOppositeEquip) {
            AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
            if (clip == null) {
                return true;
            }
            float time = PunchyAnimationManager.POSE_HANDLER.getSecondaryTime();
            boolean swapNow = clip.isEffectActive(EFFECT_SWAP_END, time) || clip.isEffectActive(EFFECT_SWAP, time) || clip.isEffectActive(EFFECT_SWAP_ITEM, time);
            return swapNow;
        }
        AnimationClip clip2 = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (clip2 == null) {
            return true;
        }
        float time2 = PunchyAnimationManager.POSE_HANDLER.getCurrentTime();
        return clip2.isEffectActive(EFFECT_SWAP_END, time2) || clip2.isEffectActive(EFFECT_SWAP, time2) || clip2.isEffectActive(EFFECT_SWAP_ITEM, time2);
    }

    private boolean isClipPlaying(AnimationClip clip) {
        return ((this.detachedOffhandEquip && this.cycleOff && !this.cycleMain) || this.shieldOppositeEquip) ? clip != null && PunchyAnimationManager.POSE_HANDLER.getSecondaryClip() == clip : clip != null && PunchyAnimationManager.POSE_HANDLER.isPlaying() && PunchyAnimationManager.POSE_HANDLER.getCurrentClip() == clip;
    }

    private boolean isHandOutInEarlyTransitionWindow() {
        if ((this.detachedOffhandEquip && this.cycleOff && !this.cycleMain) || this.shieldOppositeEquip) {
            AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getSecondaryClip();
            if (clip == null || clip != this.activeHandOutClip) {
                return false;
            }
            float length = clip.getLength();
            if (length <= 0.0f) {
                return true;
            }
            float time = PunchyAnimationManager.POSE_HANDLER.getSecondaryTime();
            return time > HAND_OUT_TO_HAND_IN_EARLY_TRANSITION_SECONDS && time >= Math.max(0.0f, length - HAND_OUT_TO_HAND_IN_EARLY_TRANSITION_SECONDS);
        }
        AnimationClip clip2 = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (clip2 == null || clip2 != this.activeHandOutClip || !PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
            return false;
        }
        float length2 = clip2.getLength();
        return length2 <= 0.0f || PunchyAnimationManager.POSE_HANDLER.getCurrentTime() >= Math.max(0.0f, length2 - HAND_OUT_TO_HAND_IN_EARLY_TRANSITION_SECONDS);
    }

    private boolean updateHotbarSlot(class_746 player) {
        int slot = resolveSelectedSlot(player);
        if (slot < 0) {
            return false;
        }
        boolean changed = slot != this.lastHotbarSlot;
        this.lastHotbarSlot = slot;
        return changed;
    }

    private boolean shouldAnimateHotbarChange(class_1799 rendered, class_1799 current) {
        return (rendered.method_7960() && current.method_7960()) ? false : true;
    }

    private boolean isRapidHotbarScrollActive() {
        return this.hotbarRapidWindowTicks > 0 && this.hotbarChangeStreak >= 2;
    }

    private void recordFunctionalSwapIfNeeded(class_746 player, class_1799 currentMain, class_1799 currentOff, boolean mainChanged, boolean offChanged, boolean hotbarChanged) {
        class_1799 previousMain = this.pendingMainChange ? this.pendingMain : this.renderedMain;
        class_1799 previousOff = this.pendingOffChange ? this.pendingOff : this.renderedOff;
        recordFunctionalSwapIfNeeded(player, previousMain, previousOff, currentMain, currentOff, mainChanged, offChanged, hotbarChanged);
    }

    private void recordFunctionalSwapIfNeeded(class_746 player, class_1799 previousMain, class_1799 previousOff, class_1799 currentMain, class_1799 currentOff, boolean mainChanged, boolean offChanged, boolean hotbarChanged) {
        if (player == null || !isFunctionalOffhandSwap(previousMain, previousOff, currentMain, currentOff, mainChanged, offChanged, hotbarChanged)) {
            return;
        }
        trimFunctionalSwapTicks(player.field_6012);
        this.recentFunctionalSwapTicks.addLast(Integer.valueOf(player.field_6012));
    }

    private boolean isFunctionalSwapBurstActive(class_746 player) {
        if (player == null) {
            return false;
        }
        trimFunctionalSwapTicks(player.field_6012);
        return this.recentFunctionalSwapTicks.size() >= 2;
    }

    private boolean wasFunctionalSwapRecordedThisTick(class_746 player) {
        if (player == null) {
            return false;
        }
        trimFunctionalSwapTicks(player.field_6012);
        Integer latestTick = this.recentFunctionalSwapTicks.peekLast();
        return latestTick != null && latestTick.intValue() == player.field_6012;
    }

    private void trimFunctionalSwapTicks(int now) {
        while (!this.recentFunctionalSwapTicks.isEmpty()) {
            int tick = this.recentFunctionalSwapTicks.peekFirst().intValue();
            if (now - tick > F_SWAP_BURST_WINDOW_TICKS) {
                this.recentFunctionalSwapTicks.removeFirst();
            } else {
                return;
            }
        }
    }

    private boolean isFunctionalOffhandSwap(class_1799 previousMain, class_1799 previousOff, class_1799 currentMain, class_1799 currentOff, boolean mainChanged, boolean offChanged, boolean hotbarChanged) {
        return !hotbarChanged && mainChanged && offChanged && areEquivalentStacks(previousMain, currentOff) && areEquivalentStacks(previousOff, currentMain);
    }

    private boolean areEquivalentStacks(class_1799 left, class_1799 right) {
        return !hasItemChanged(left, right);
    }

    private class_1306 resolveActiveArm(class_746 player) {
        if (this.cycleMain && this.cycleOff) {
            return null;
        }
        if (this.cycleMain) {
            return resolveArm(player, class_1268.field_5808);
        }
        if (this.cycleOff) {
            return resolveArm(player, class_1268.field_5810);
        }
        return null;
    }

    private class_1306 resolveArm(class_746 player, class_1268 hand) {
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        return hand == class_1268.field_5808 ? mainRight ? class_1306.field_6183 : class_1306.field_6182 : mainRight ? class_1306.field_6182 : class_1306.field_6183;
    }

    private class_1268 resolveHandForArm(class_746 player, class_1306 arm) {
        if (player == null || arm == null) {
            return class_1268.field_5808;
        }
        if (resolveArm(player, class_1268.field_5808) == arm) {
            return class_1268.field_5808;
        }
        return class_1268.field_5810;
    }

    private EquipSelection resolveHandOutClip() {
        EquipSelection equipSelection;
        EquipSelection equipSelection2;
        if (this.cycleMain) {
            equipSelection = resolveCustomEquipClip(this.renderedMain, class_1268.field_5808, "hand_out");
        } else {
            equipSelection = new EquipSelection(null, null, class_1268.field_5808, false);
        }
        EquipSelection mainSelection = equipSelection;
        if (this.cycleOff) {
            equipSelection2 = resolveCustomEquipClip(this.renderedOff, class_1268.field_5810, "hand_out");
        } else {
            equipSelection2 = new EquipSelection(null, null, class_1268.field_5810, false);
        }
        EquipSelection offSelection = equipSelection2;
        return chooseEquipSelection(mainSelection, offSelection);
    }

    private EquipSelection resolveHandInClip(class_746 player) {
        EquipSelection equipSelection;
        EquipSelection equipSelection2;
        class_1799 mainTarget = resolveRaiseTargetStack(class_1268.field_5808, this.pendingMain, this.renderedMain, player);
        class_1799 offTarget = resolveRaiseTargetStack(class_1268.field_5810, this.pendingOff, this.renderedOff, player);
        if (this.cycleMain) {
            equipSelection = resolveCustomEquipClip(mainTarget, class_1268.field_5808, "hand_in");
        } else {
            equipSelection = new EquipSelection(null, null, class_1268.field_5808, false);
        }
        EquipSelection mainSelection = equipSelection;
        if (this.cycleOff) {
            equipSelection2 = resolveCustomEquipClip(offTarget, class_1268.field_5810, "hand_in");
        } else {
            equipSelection2 = new EquipSelection(null, null, class_1268.field_5810, false);
        }
        EquipSelection offSelection = equipSelection2;
        return chooseEquipSelection(mainSelection, offSelection);
    }

    private class_1799 resolveRaiseTargetStack(class_1268 hand, class_1799 pending, class_1799 rendered, class_746 player) {
        if (pending != null && !pending.method_7960()) {
            return pending;
        }
        class_1799 live = class_1799.field_8037;
        if (player != null) {
            live = hand == class_1268.field_5810 ? player.method_6079() : player.method_6047();
        }
        if (live != null && !live.method_7960()) {
            return live;
        }
        if (rendered != null && !rendered.method_7960()) {
            return rendered;
        }
        return class_1799.field_8037;
    }

    private EquipSelection resolveCustomEquipClip(class_1799 stack, class_1268 hand, String type) {
        if (stack == null || type == null) {
            return new EquipSelection(null, null, hand, false);
        }
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, type);
        class_310 mc = class_310.method_1551();
        CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, type, entry, mc, hand);
        if (lookup.clip() == null) {
            return new EquipSelection(null, entry, hand, false);
        }
        boolean explicitCustom = (lookup.usedTypeFallback() || lookup.resolvedName() == null || lookup.resolvedName().isBlank()) ? false : true;
        return new EquipSelection(lookup.clip(), entry, hand, explicitCustom);
    }

    private EquipSelection chooseEquipSelection(EquipSelection mainSelection, EquipSelection offSelection) {
        if (mainSelection.clip == null) {
            return offSelection;
        }
        if (offSelection.clip == null) {
            return mainSelection;
        }
        if (isWalkOnWaterPoseActive() && this.cycleOff && !this.cycleMain) {
            return offSelection;
        }
        if (mainSelection.explicitCustom != offSelection.explicitCustom) {
            return mainSelection.explicitCustom ? mainSelection : offSelection;
        }
        return mainSelection;
    }

    private float resolveEquipSpeedMultiplier() {
        return 1.0f / Math.max(0.01f, PunchyAnimationManager.ANIMATION_SPEED);
    }

    private class_1799 copyStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return class_1799.field_8037;
        }
        return stack.method_7972();
    }

    private String debugStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return "empty";
        }
        return String.valueOf(class_7923.field_41178.method_10221(stack.method_7909()));
    }

    private String clipName(AnimationClip clip) {
        if (clip == null) {
            return "null";
        }
        String name = clip.getName();
        return (name == null || name.isBlank()) ? "<unnamed>" : name;
    }

    private String formatDebugTime(float value) {
        return String.format(Locale.ROOT, "%.3f", Float.valueOf(value));
    }

    private void debugOffhandDetach(String event, class_746 player, class_1799 currentOff) {
    }

    private boolean wasItemBlacklisted(class_1799 stack) {
        return PunchyConfig.isItemBlacklisted(stack);
    }

    private boolean isHandBlacklisted(class_746 player, class_1268 hand) {
        return PunchyConfig.isHandBlacklisted(player, hand);
    }

    private boolean hasItemChanged(class_1799 previous, class_1799 current) {
        class_1799 prev = previous == null ? class_1799.field_8037 : previous;
        class_1799 now = current == null ? class_1799.field_8037 : current;
        if (!prev.method_31574(now.method_7909())) {
            return true;
        }
        if ((prev.method_7909() instanceof class_1764) && (now.method_7909() instanceof class_1764)) {
            return false;
        }
        for (class_9336<?> dataComponent : now.method_57353()) {
            if (!shouldIgnoreComponentForEquipDiff(dataComponent.comp_2443()) && (!prev.method_57353().method_57832(dataComponent.comp_2443()) || !Objects.equals(prev.method_58694(dataComponent.comp_2443()), dataComponent.comp_2444()))) {
                return true;
            }
        }
        for (class_9336<?> dataComponent2 : prev.method_57353()) {
            if (!shouldIgnoreComponentForEquipDiff(dataComponent2.comp_2443()) && !now.method_57353().method_57832(dataComponent2.comp_2443())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldIgnoreComponentForEquipDiff(class_9331<?> type) {
        if (type == null) {
            return false;
        }
        if (type == class_9334.field_49629) {
            return true;
        }
        class_2960 id = class_7923.field_49658.method_10221(type);
        return (id == null || "minecraft".equals(id.method_12836())) ? false : true;
    }

    private boolean shouldSkipHandEquipOnSwap(class_1799 currentMain, class_1799 currentOff, boolean mainChanged, boolean offChanged) {
        if (!mainChanged && !offChanged) {
            return false;
        }
        if (mainChanged && shouldIgnoreMainhandTridentSwap(currentMain)) {
            this.skipNextEquipMain = false;
            return true;
        }
        if (mainChanged && (hasSkipHandEquipFlag(this.renderedMain, currentMain) || this.skipNextEquipMain)) {
            this.skipNextEquipMain = false;
            return true;
        }
        if (!offChanged) {
            return false;
        }
        if (hasSkipHandEquipFlag(this.renderedOff, currentOff) || this.skipNextEquipOff) {
            this.skipNextEquipOff = false;
            return true;
        }
        return false;
    }

    private boolean shouldIgnoreMainhandTridentSwap(class_1799 currentMain) {
        class_1799 previousMain = this.renderedMain == null ? class_1799.field_8037 : this.renderedMain;
        class_1799 nextMain = currentMain == null ? class_1799.field_8037 : currentMain;
        boolean tridentRelated = (previousMain.method_7909() instanceof class_1835) || (nextMain.method_7909() instanceof class_1835);
        if (!tridentRelated) {
            return false;
        }
        class_310 client = class_310.method_1551();
        if (TridentStateMachine.isRecentActionBlockingEquip(client)) {
            return true;
        }
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (PunchyAnimationManager.isClipNamed(clip, "use_trident") || PunchyAnimationManager.isClipNamed(clip, "use_trident_throw") || PunchyAnimationManager.isClipNamed(clip, "use_trident_throw_loyalty") || PunchyAnimationManager.isClipNamed(clip, "use_trident_throw_riptide")) {
            return true;
        }
        return PunchyAnimationManager.POSE_HANDLER.isBlendOutActive() && (PunchyAnimationManager.isClipNamed(clip, "use_trident_throw") || PunchyAnimationManager.isClipNamed(clip, "use_trident_throw_loyalty") || PunchyAnimationManager.isClipNamed(clip, "use_trident_throw_riptide"));
    }

    private boolean shouldHideOffhandTridentThrowCache(class_1799 currentOff, boolean offChanged) {
        if (!offChanged || currentOff == null || !currentOff.method_7960()) {
            return false;
        }
        class_1799 previousOff = this.renderedOff == null ? class_1799.field_8037 : this.renderedOff;
        return previousOff.method_7909() instanceof class_1835;
    }

    private void updateSkipEquipLatches(class_1799 currentMain, class_1799 currentOff) {
        if (!isSkipHandEquipWindowActive()) {
            return;
        }
        if (hasSkipHandEquipFlagForStack(currentMain)) {
            this.skipNextEquipMain = true;
        }
        if (hasSkipHandEquipFlagForStack(currentOff)) {
            this.skipNextEquipOff = true;
        }
    }

    private boolean hasSkipHandEquipFlag(class_1799 previous, class_1799 current) {
        return isThrowableConsumedToEmpty(previous, current) || hasSkipHandEquipFlagForStack(previous) || hasSkipHandEquipFlagForStack(current);
    }

    private boolean isThrowableConsumedToEmpty(class_1799 previous, class_1799 current) {
        AnimationClip activeClip;
        if (!isThrowableSkipEquipStack(previous)) {
            return false;
        }
        if ((current != null && !current.method_7960()) || (activeClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null) {
            return false;
        }
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(previous, "useItem");
        if (entry != null && isEntryActiveForCurrentUseClip(entry, "useItem", activeClip)) {
            return true;
        }
        return PunchyAnimationManager.isClipNamed(activeClip, "throw_item");
    }

    private boolean isThrowableSkipEquipStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        return stack.method_31574(class_1802.field_8436) || stack.method_31574(class_1802.field_8150) || stack.method_31574(class_1802.field_8287) || stack.method_31574(class_1802.field_8543) || stack.method_31574(class_1802.field_8803) || stack.method_31574(class_1802.field_8634) || stack.method_31574(class_1802.field_8449);
    }

    private boolean hasSkipHandEquipFlagForStack(class_1799 stack) {
        AnimationClip activeClip;
        if (stack == null || stack.method_7960() || (activeClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null) {
            return false;
        }
        String[] types = {"useDrink", "useEat", "useItem"};
        int length = types.length;
        for (int i = LOG_OFFHAND_DETACH_DEBUG; i < length; i += LOG_WALK_ON_WATER_EQUIP_DEBUG) {
            String type = types[i];
            PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, type);
            if (CustomAnimationResolver.shouldSkipHandEquip(entry) && isEntryActiveForCurrentUseClip(entry, type, activeClip)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSkipHandEquipWindowActive() {
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (clip == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || isEquipClip(clip) || isBlockingEquipClip(clip)) {
            return false;
        }
        return true;
    }

    private boolean isEntryActiveForCurrentUseClip(PunchyTuningConfig.CustomAnimationEntry entry, String type, AnimationClip activeClip) {
        if (activeClip == null || type == null) {
            return false;
        }
        String customName = CustomAnimationResolver.resolveSingleName(entry);
        if (customName != null && !customName.isBlank()) {
            return PunchyAnimationManager.isClipNamed(activeClip, customName);
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT).replace("_", "");
        if ("usedrink".equals(normalized)) {
            return PunchyAnimationManager.isClipNamed(activeClip, "use_drink");
        }
        if ("useeat".equals(normalized)) {
            return PunchyAnimationManager.isClipNamed(activeClip, "use_eat");
        }
        if ("useitem".equals(normalized)) {
            return PunchyAnimationManager.isClipNamed(activeClip, "interact") || PunchyAnimationManager.isClipNamed(activeClip, "mining_use_foward") || PunchyAnimationManager.isClipNamed(activeClip, "mining_use_digging");
        }
        return false;
    }

    private int resolveSelectedSlot(class_746 player) {
        class_1661 inventory;
        if (player == null || (inventory = player.method_31548()) == null) {
            return -1;
        }
        try {
            return inventory.method_67532();
        } catch (Exception e) {
            return -1;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/HandEquipStateMachine$EquipSelection.class */
    private static final class EquipSelection extends Record {
        private final AnimationClip clip;
        private final PunchyTuningConfig.CustomAnimationEntry entry;
        private final class_1268 hand;
        private final boolean explicitCustom;

        private EquipSelection(AnimationClip clip, PunchyTuningConfig.CustomAnimationEntry entry, class_1268 hand, boolean explicitCustom) {
            this.clip = clip;
            this.entry = entry;
            this.hand = hand;
            this.explicitCustom = explicitCustom;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, EquipSelection.class), EquipSelection.class, "clip;entry;hand;explicitCustom", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->explicitCustom:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, EquipSelection.class), EquipSelection.class, "clip;entry;hand;explicitCustom", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->explicitCustom:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, EquipSelection.class, Object.class), EquipSelection.class, "clip;entry;hand;explicitCustom", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$EquipSelection;->explicitCustom:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public AnimationClip clip() {
            return this.clip;
        }

        public PunchyTuningConfig.CustomAnimationEntry entry() {
            return this.entry;
        }

        public class_1268 hand() {
            return this.hand;
        }

        public boolean explicitCustom() {
            return this.explicitCustom;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/HandEquipStateMachine$PoseHandlerState.class */
    private static final class PoseHandlerState extends Record {
        private final AnimationClip clip;
        private final float time;

        private PoseHandlerState(AnimationClip clip, float time) {
            this.clip = clip;
            this.time = time;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, PoseHandlerState.class), PoseHandlerState.class, "clip;time", "FIELD:Lpunchy/client/state/HandEquipStateMachine$PoseHandlerState;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$PoseHandlerState;->time:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, PoseHandlerState.class), PoseHandlerState.class, "clip;time", "FIELD:Lpunchy/client/state/HandEquipStateMachine$PoseHandlerState;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$PoseHandlerState;->time:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, PoseHandlerState.class, Object.class), PoseHandlerState.class, "clip;time", "FIELD:Lpunchy/client/state/HandEquipStateMachine$PoseHandlerState;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/HandEquipStateMachine$PoseHandlerState;->time:F").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public AnimationClip clip() {
            return this.clip;
        }

        public float time() {
            return this.time;
        }
    }
}
