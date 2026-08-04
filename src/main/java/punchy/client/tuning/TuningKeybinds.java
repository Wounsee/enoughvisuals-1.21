package punchy.client.tuning;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.Locale;
import net.minecraft.class_10255;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_7923;
import punchy.client.debug.PunchyDebug;
import punchy.client.render.ChestProxyTuning;
import punchy.client.render.EnchantParticleProxyTuning;
import punchy.client.render.FishingLineProxyTuning;
import punchy.client.render.FluidProxyTuning;
import punchy.client.render.PaddleProxyTuning;
import punchy.client.render.ToolKindResolver;
import punchy.client.render.ToolTuning;
import punchy.client.renderer.layer.ArmMeshTuning;
import punchy.client.renderer.layer.ArmToolTuning;
import punchy.client.renderer.layer.PlayerArmModelLayer;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.client.state.SpearStateMachine;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningKeybinds.class */
public final class TuningKeybinds {
    private static final String CATEGORY = "key.categories.punchy.tuning";
    private static class_304 toggleContextKey;
    private static class_304 cycleKindKey;
    private static class_304 toggleSpecificityKey;
    private static class_304 toggleHandKey;
    private static class_304 cycleModeKey;
    private static class_304 cycleAxisKey;
    private static class_304 setAxisXKey;
    private static class_304 setAxisYKey;
    private static class_304 setAxisZKey;
    private static class_304 increaseKey;
    private static class_304 decreaseKey;
    private static class_304 saveKey;
    private static class_304 resetKey;
    private static class_304 mirrorKey;
    private static final VanillaFirstPersonItemLayer.ToolKind[] ITEM_KINDS = VanillaFirstPersonItemLayer.ToolKind.values();
    private static boolean registered = false;
    private static Target target = Target.ITEM_KIND;
    private static Axis axis = Axis.X;
    private static Mode mode = Mode.TRANSLATE;
    private static class_1268 hand = class_1268.field_5808;
    private static final boolean ENABLE_TUNING_HOTKEYS = false;
    private static VanillaFirstPersonItemLayer.ToolKind currentKind = ITEM_KINDS[ENABLE_TUNING_HOTKEYS];
    private static class_2960 currentItemId = null;
    private static ChestProxyTuning.Kind chestProxyKind = ChestProxyTuning.Kind.BOAT;
    private static FluidProxyTuning.Kind fluidProxyKind = FluidProxyTuning.Kind.WATER;
    private static EnchantParticleProxyTuning.Kind enchantParticleKind = EnchantParticleProxyTuning.Kind.BOOK;
    private static PaddleProxyTuning.Kind paddleProxyKind = PaddleProxyTuning.Kind.BOAT;
    private static FishingLineProxyTuning.Kind fishingLineProxyKind = FishingLineProxyTuning.Kind.ROD;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningKeybinds$Axis.class */
    private enum Axis {
        X,
        Y,
        Z
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningKeybinds$Target.class */
    private enum Target {
        ITEM_KIND,
        ITEM_SPECIFIC,
        ITEM_GLOBAL,
        ARM_KIND,
        ARM_SPECIFIC,
        ARM_GLOBAL,
        ARM_MESH,
        CHEST_PROXY,
        FLUID_PROXY,
        ENCHANT_PARTICLE_PROXY,
        PADDLE_PROXY,
        FISHING_LINE_PROXY
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningKeybinds$Mode.class */
    private enum Mode {
        TRANSLATE("offsets", true),
        ROTATE("rotation", true),
        SCALE("scale", false),
        PIVOT("pivot", true);

        final String label;
        final boolean usesAxis;

        Mode(String label, boolean usesAxis) {
            this.label = label;
            this.usesAxis = usesAxis;
        }
    }

    private TuningKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public static void handleClientTick(class_310 client) throws MatchException {
        if (!registered || client == null || client.field_1724 == null || !PunchyConfig.isTuningEnabled() || client.field_1755 != null || !PunchyDebug.DEBUG_POSITION_ENABLED) {
            return;
        }
        autoSelectCurrentItem(client);
        while (toggleContextKey.method_1436()) {
            toggleContext(client);
        }
        while (cycleKindKey.method_1436()) {
            cycleKind(client);
        }
        while (toggleSpecificityKey.method_1436()) {
            toggleSpecificity(client);
        }
        while (toggleHandKey.method_1436()) {
            hand = hand == class_1268.field_5808 ? class_1268.field_5810 : class_1268.field_5808;
            autoSelectCurrentItem(client);
            toast(client, describeState());
        }
        while (cycleModeKey.method_1436()) {
            mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
            toast(client, describeState());
        }
        while (cycleAxisKey.method_1436()) {
            cycleAxis(client);
        }
        if (setAxisXKey.method_1436()) {
            setAxis(client, Axis.X);
        }
        if (setAxisYKey.method_1436()) {
            setAxis(client, Axis.Y);
        }
        if (setAxisZKey.method_1436()) {
            setAxis(client, Axis.Z);
        }
        while (saveKey.method_1436()) {
            PunchyTuningConfig.saveCurrent();
            toast(client, class_2561.method_43471("punchy.tuning.toast.saved_config"));
        }
        while (resetKey.method_1436()) {
            resetCurrentTarget(client);
        }
        while (mirrorKey.method_1436()) {
            mirrorCurrentTarget(client);
        }
        boolean increased = increaseKey.method_1434() && !decreaseKey.method_1434();
        boolean decreased = decreaseKey.method_1434() && !increaseKey.method_1434();
        if (increased) {
            applyDelta(client, true);
        } else if (decreased) {
            applyDelta(client, false);
        }
    }

    private static void autoSelectCurrentItem(class_310 client) {
        if (client.field_1724 == null) {
            return;
        }
        autoSelectPaddleProxyKind(client);
        class_1799 stack = client.field_1724.method_5998(hand);
        if (stack.method_7960()) {
            currentItemId = null;
            return;
        }
        currentItemId = class_7923.field_41178.method_10221(stack.method_7909());
        currentKind = resolveTuningKind(stack);
        autoSelectChestProxyKind();
        autoSelectFluidProxyKind(stack);
    }

    private static VanillaFirstPersonItemLayer.ToolKind resolveTuningKind(class_1799 stack) {
        if (SpearStateMachine.isSpearStack(stack)) {
            return VanillaFirstPersonItemLayer.ToolKind.SPEAR;
        }
        return ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.AXE);
    }

    private static void autoSelectPaddleProxyKind(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return;
        }
        class_10255 class_10255VarMethod_5854 = client.field_1724.method_5854();
        if (!(class_10255VarMethod_5854 instanceof class_10255)) {
            return;
        }
        class_10255 boat = class_10255VarMethod_5854;
        class_2960 id = class_7923.field_41177.method_10221(boat.method_5864());
        if (id != null && id.method_12832().contains("raft")) {
            paddleProxyKind = PaddleProxyTuning.Kind.RAFT;
        } else {
            paddleProxyKind = PaddleProxyTuning.Kind.BOAT;
        }
    }

    private static void autoSelectChestProxyKind() {
        if (currentItemId == null) {
            return;
        }
        String path = currentItemId.method_12832();
        if (path.endsWith("_chest_boat")) {
            chestProxyKind = ChestProxyTuning.Kind.BOAT;
        } else if (path.endsWith("_chest_raft")) {
            chestProxyKind = ChestProxyTuning.Kind.RAFT;
        } else if ("chest_minecart".equals(path)) {
            chestProxyKind = ChestProxyTuning.Kind.MINECART;
        }
    }

    private static void autoSelectFluidProxyKind(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return;
        }
        if (stack.method_31574(class_1802.field_8705)) {
            fluidProxyKind = FluidProxyTuning.Kind.WATER;
        } else if (stack.method_31574(class_1802.field_8187)) {
            fluidProxyKind = FluidProxyTuning.Kind.LAVA;
        } else if (stack.method_31574(class_1802.field_8103)) {
            fluidProxyKind = FluidProxyTuning.Kind.MILK;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void toggleContext(class_310 client) throws MatchException {
        Target target2;
        switch (target.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                target2 = Target.ARM_KIND;
                break;
            case 1:
                target2 = Target.ARM_SPECIFIC;
                break;
            case 2:
                target2 = Target.ARM_GLOBAL;
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                target2 = Target.CHEST_PROXY;
                break;
            case 7:
                target2 = Target.FLUID_PROXY;
                break;
            case 8:
                target2 = Target.ENCHANT_PARTICLE_PROXY;
                break;
            case 9:
                target2 = Target.PADDLE_PROXY;
                break;
            case 10:
                target2 = Target.FISHING_LINE_PROXY;
                break;
            case 11:
                target2 = Target.ITEM_KIND;
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        target = target2;
        toast(client, describeState());
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void toggleSpecificity(class_310 client) throws MatchException {
        Target target2;
        if (target == Target.CHEST_PROXY || target == Target.FLUID_PROXY || target == Target.ENCHANT_PARTICLE_PROXY || target == Target.PADDLE_PROXY || target == Target.FISHING_LINE_PROXY) {
            toast(client, class_2561.method_43471("punchy.tuning.toast.use_home_proxy"));
            return;
        }
        switch (target.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                target2 = Target.ITEM_SPECIFIC;
                break;
            case 1:
                target2 = Target.ITEM_GLOBAL;
                break;
            case 2:
                target2 = Target.ITEM_KIND;
                break;
            case 3:
                target2 = Target.ARM_SPECIFIC;
                break;
            case 4:
                target2 = Target.ARM_GLOBAL;
                break;
            case 5:
                target2 = Target.ARM_MESH;
                break;
            case 6:
                target2 = Target.ARM_KIND;
                break;
            case 7:
                target2 = Target.CHEST_PROXY;
                break;
            case 8:
                target2 = Target.FLUID_PROXY;
                break;
            case 9:
                target2 = Target.ENCHANT_PARTICLE_PROXY;
                break;
            case 10:
                target2 = Target.PADDLE_PROXY;
                break;
            case 11:
                target2 = Target.FISHING_LINE_PROXY;
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        target = target2;
        toast(client, describeState());
    }

    private static void cycleKind(class_310 client) {
        if (target == Target.CHEST_PROXY) {
            ChestProxyTuning.Kind[] kinds = ChestProxyTuning.Kind.values();
            int idx = chestProxyKind.ordinal() + 1;
            if (idx >= kinds.length) {
                idx = ENABLE_TUNING_HOTKEYS;
            }
            chestProxyKind = kinds[idx];
            toast(client, describeState());
            return;
        }
        if (target == Target.FLUID_PROXY) {
            FluidProxyTuning.Kind[] kinds2 = FluidProxyTuning.Kind.values();
            int idx2 = fluidProxyKind.ordinal() + 1;
            if (idx2 >= kinds2.length) {
                idx2 = ENABLE_TUNING_HOTKEYS;
            }
            fluidProxyKind = kinds2[idx2];
            toast(client, describeState());
            return;
        }
        if (target == Target.PADDLE_PROXY) {
            PaddleProxyTuning.Kind[] kinds3 = PaddleProxyTuning.Kind.values();
            int idx3 = paddleProxyKind.ordinal() + 1;
            if (idx3 >= kinds3.length) {
                idx3 = ENABLE_TUNING_HOTKEYS;
            }
            paddleProxyKind = kinds3[idx3];
            toast(client, describeState());
            return;
        }
        if (target == Target.FISHING_LINE_PROXY) {
            toast(client, describeState());
            return;
        }
        if (target == Target.ENCHANT_PARTICLE_PROXY) {
            toast(client, describeState());
            return;
        }
        if (target == Target.ARM_SPECIFIC) {
            if (currentItemId == null) {
                toast(client, class_2561.method_43471("punchy.tuning.toast.hold_item_change_arm_kind"));
                return;
            }
            PlayerArmModelLayer.ToolKind[] kinds4 = PlayerArmModelLayer.ToolKind.values();
            PlayerArmModelLayer.ToolKind currentArmKind = toArmKind(currentKind);
            if (currentArmKind == null) {
                currentArmKind = PlayerArmModelLayer.ToolKind.AXE;
            }
            int idx4 = currentArmKind.ordinal() + 1;
            if (idx4 >= kinds4.length) {
                idx4 = ENABLE_TUNING_HOTKEYS;
            }
            PlayerArmModelLayer.ToolKind newKind = kinds4[idx4];
            VanillaFirstPersonItemLayer.ToolKind newItemKind = findMatchingItemKind(newKind);
            if (newItemKind != null) {
                currentKind = newItemKind;
            }
            boolean leftHand = hand == class_1268.field_5810;
            ArmToolTuning.Profile profile = ArmToolTuning.getSpecificProfile(currentItemId, newKind, leftHand);
            ArmToolTuning.applySpecific(currentItemId, newKind, leftHand ? null : profile, leftHand ? profile : null);
            ArmToolTuning.SpecificSnapshot fullSpec = ArmToolTuning.snapshotSpecifics().get(currentItemId);
            if (fullSpec != null) {
                ArmToolTuning.Profile right = fullSpec.right();
                ArmToolTuning.Profile left = fullSpec.left();
                ArmToolTuning.applySpecific(currentItemId, newKind, right, left);
            }
            toast(client, class_2561.method_43469("punchy.tuning.toast.arm_kind_set", new Object[]{newKind.name()}));
            return;
        }
        int idx5 = currentKind.ordinal() + 1;
        if (idx5 >= ITEM_KINDS.length) {
            idx5 = ENABLE_TUNING_HOTKEYS;
        }
        currentKind = ITEM_KINDS[idx5];
        if (target == Target.ITEM_KIND || target == Target.ARM_KIND) {
            toast(client, describeState());
        } else {
            toast(client, class_2561.method_43469("punchy.tuning.toast.kind_selected_group", new Object[]{currentKind.name()}));
        }
    }

    private static VanillaFirstPersonItemLayer.ToolKind findMatchingItemKind(PlayerArmModelLayer.ToolKind armKind) {
        try {
            return VanillaFirstPersonItemLayer.ToolKind.valueOf(armKind.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void cycleAxis(class_310 client) {
        if (!mode.usesAxis) {
            toast(client, class_2561.method_43469("punchy.tuning.toast.axis_not_used", new Object[]{modeLabel(mode)}));
        } else {
            axis = Axis.values()[(axis.ordinal() + 1) % Axis.values().length];
            toast(client, describeState());
        }
    }

    private static void setAxis(class_310 client, Axis desired) {
        if (!mode.usesAxis) {
            toast(client, class_2561.method_43469("punchy.tuning.toast.axis_not_used", new Object[]{modeLabel(mode)}));
        } else {
            axis = desired;
            toast(client, describeState());
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void applyDelta(class_310 client, boolean positive) throws MatchException {
        if (mode == Mode.PIVOT && (target == Target.ARM_GLOBAL || target == Target.ARM_MESH || target == Target.ARM_KIND || target == Target.ARM_SPECIFIC || target == Target.CHEST_PROXY || target == Target.FLUID_PROXY || target == Target.ENCHANT_PARTICLE_PROXY || target == Target.PADDLE_PROXY || target == Target.FISHING_LINE_PROXY)) {
            toast(client, class_2561.method_43471("punchy.tuning.toast.pivot_item_only"));
        }
        float delta = computeStep(client);
        if (!positive) {
            delta *= -1.0f;
        }
        boolean leftHand = hand == class_1268.field_5810;
        switch (target.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                AdjustmentResult result = adjustToolProfile(ToolTuning.get(currentKind, leftHand), delta);
                if (result == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.apply_failed"));
                } else {
                    ToolTuning.commitKind(currentKind, leftHand);
                    ToolTuning.markKindDetached(currentKind, leftHand);
                    report(client, result);
                }
                break;
            case 1:
                if (currentItemId == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.hold_item_edit_specific"));
                } else {
                    ToolTuning.Profile profile = ToolTuning.getSpecificProfile(currentItemId, currentKind, leftHand);
                    if (profile == null) {
                        toast(client, class_2561.method_43471("punchy.tuning.toast.prepare_specific_failed"));
                    } else {
                        AdjustmentResult result2 = adjustToolProfile(profile, delta);
                        if (result2 == null) {
                            toast(client, class_2561.method_43471("punchy.tuning.toast.apply_specific_failed"));
                        } else {
                            report(client, result2);
                        }
                    }
                }
                break;
            case 2:
                AdjustmentResult result3 = adjustToolProfile(ToolTuning.getGlobal(leftHand), delta);
                if (result3 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.apply_global_failed"));
                } else {
                    ToolTuning.commitGlobal(leftHand);
                    report(client, result3);
                }
                break;
            case 3:
                PlayerArmModelLayer.ToolKind armKind = toArmKind(currentKind);
                if (armKind == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.item_kind_no_arm_kind"));
                } else {
                    ArmToolTuning.Profile profile2 = ArmToolTuning.get(armKind, leftHand);
                    if (profile2 == null) {
                        toast(client, class_2561.method_43471("punchy.tuning.toast.arm_kind_profile_missing"));
                    } else {
                        AdjustmentResult result4 = adjustArmProfile(profile2, delta);
                        ArmToolTuning.commitKind(armKind, leftHand);
                        ArmToolTuning.markKindDetached(armKind, leftHand);
                        report(client, result4);
                    }
                }
                break;
            case 4:
                if (currentItemId == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.hold_item_edit_arm_specific"));
                } else {
                    PlayerArmModelLayer.ToolKind armKind2 = toArmKind(currentKind);
                    if (armKind2 == null) {
                        armKind2 = PlayerArmModelLayer.ToolKind.AXE;
                    }
                    report(client, adjustArmProfile(ArmToolTuning.getSpecificProfile(currentItemId, armKind2, leftHand), delta));
                    ArmToolTuning.commitSpecific(currentItemId, leftHand);
                }
                break;
            case 5:
                report(client, adjustArmProfile(ArmToolTuning.getGlobal(leftHand), delta));
                ArmToolTuning.commitGlobal(leftHand);
                break;
            case 6:
                report(client, adjustArmMeshProfile(ArmMeshTuning.getGlobal(leftHand), delta));
                break;
            case 7:
                ChestProxyTuning.Profile profile3 = ChestProxyTuning.get(chestProxyKind);
                if (profile3 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.chest_proxy_profile_missing"));
                } else {
                    AdjustmentResult result5 = adjustChestProxyProfile(profile3, delta);
                    report(client, result5);
                }
                break;
            case 8:
                FluidProxyTuning.Profile profile4 = FluidProxyTuning.get(fluidProxyKind);
                if (profile4 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.fluid_proxy_profile_missing"));
                } else {
                    AdjustmentResult result6 = adjustFluidProxyProfile(profile4, delta);
                    report(client, result6);
                }
                break;
            case 9:
                EnchantParticleProxyTuning.Profile profile5 = EnchantParticleProxyTuning.get(enchantParticleKind, leftHand);
                if (profile5 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.enchant_proxy_profile_missing"));
                } else {
                    AdjustmentResult result7 = adjustEnchantParticleProxyProfile(profile5, delta);
                    report(client, result7);
                }
                break;
            case 10:
                PaddleProxyTuning.Profile profile6 = PaddleProxyTuning.get(paddleProxyKind, leftHand);
                if (profile6 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.paddle_proxy_profile_missing"));
                } else {
                    AdjustmentResult result8 = adjustPaddleProxyProfile(profile6, delta);
                    report(client, result8);
                }
                break;
            case 11:
                FishingLineProxyTuning.Profile profile7 = FishingLineProxyTuning.get(fishingLineProxyKind, leftHand);
                if (profile7 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.fishing_line_proxy_profile_missing"));
                } else {
                    AdjustmentResult result9 = adjustFishingLineProxyProfile(profile7, delta);
                    report(client, result9);
                }
                break;
        }
    }

    private static PlayerArmModelLayer.ToolKind toArmKind(VanillaFirstPersonItemLayer.ToolKind vk) {
        try {
            return PlayerArmModelLayer.ToolKind.valueOf(vk.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void resetCurrentTarget(class_310 client) throws MatchException {
        ArmToolTuning.Profile profile;
        boolean leftHand = hand == class_1268.field_5810;
        switch (target.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                ToolTuning.Profile profile2 = ToolTuning.get(currentKind, leftHand);
                if (profile2 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.no_item_profile_loaded"));
                } else {
                    resetToolProfile(profile2);
                    ToolTuning.commitKind(currentKind, leftHand);
                    ToolTuning.markKindAttached(currentKind, leftHand);
                    toast(client, class_2561.method_43471("punchy.tuning.toast.offsets_reset_kind"));
                }
                break;
            case 1:
                if (currentItemId == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.hold_item_reset_specific"));
                } else {
                    ToolTuning.Profile profile3 = ToolTuning.getSpecificProfile(currentItemId, currentKind, leftHand);
                    if (profile3 == null) {
                        toast(client, class_2561.method_43471("punchy.tuning.toast.specific_profile_missing"));
                    } else {
                        resetToolProfile(profile3);
                        toast(client, class_2561.method_43471("punchy.tuning.toast.offsets_reset_specific"));
                    }
                }
                break;
            case 2:
                ToolTuning.Profile profile4 = ToolTuning.getGlobal(leftHand);
                if (profile4 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.no_global_item_profile"));
                } else {
                    resetToolProfile(profile4);
                    ToolTuning.commitGlobal(leftHand);
                    toast(client, class_2561.method_43471("punchy.tuning.toast.offsets_reset_global"));
                }
                break;
            case 3:
                PlayerArmModelLayer.ToolKind armKind = toArmKind(currentKind);
                if (armKind != null && (profile = ArmToolTuning.get(armKind, leftHand)) != null) {
                    resetArmProfile(profile, armKind, leftHand);
                    ArmToolTuning.commitKind(armKind, leftHand);
                    ArmToolTuning.markKindAttached(armKind, leftHand);
                    toast(client, class_2561.method_43469("punchy.tuning.toast.arm_reset_kind", new Object[]{armKind.name()}));
                    break;
                }
                break;
            case 4:
                if (currentItemId != null) {
                    PlayerArmModelLayer.ToolKind armKind2 = toArmKind(currentKind);
                    if (armKind2 == null) {
                        armKind2 = PlayerArmModelLayer.ToolKind.AXE;
                    }
                    ArmToolTuning.Profile profile5 = ArmToolTuning.getSpecificProfile(currentItemId, armKind2, leftHand);
                    if (profile5 != null) {
                        resetArmProfile(profile5, armKind2, leftHand);
                        ArmToolTuning.commitSpecific(currentItemId, leftHand);
                        toast(client, class_2561.method_43471("punchy.tuning.toast.arm_reset_specific"));
                    }
                }
                break;
            case 5:
                ArmToolTuning.Profile profile6 = ArmToolTuning.getGlobal(leftHand);
                if (profile6 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.arm_profile_missing"));
                } else {
                    resetArmProfile(profile6, null, leftHand);
                    ArmToolTuning.commitGlobal(leftHand);
                    toast(client, class_2561.method_43471("punchy.tuning.toast.arm_offsets_reset"));
                }
                break;
            case 6:
                ArmMeshTuning.Profile profile7 = ArmMeshTuning.getGlobal(leftHand);
                if (profile7 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.arm_profile_missing"));
                } else {
                    resetArmMeshProfile(profile7);
                    toast(client, class_2561.method_43471("punchy.tuning.toast.arm_mesh_offsets_reset"));
                }
                break;
            case 7:
                ChestProxyTuning.reset(chestProxyKind);
                toast(client, class_2561.method_43469("punchy.tuning.toast.chest_proxy_reset", new Object[]{chestProxyKind.name()}));
                break;
            case 8:
                FluidProxyTuning.reset(fluidProxyKind);
                toast(client, class_2561.method_43469("punchy.tuning.toast.fluid_proxy_reset", new Object[]{fluidProxyKind.name()}));
                break;
            case 9:
                EnchantParticleProxyTuning.reset(enchantParticleKind, leftHand);
                toast(client, class_2561.method_43469("punchy.tuning.toast.enchant_proxy_reset", new Object[]{enchantParticleKind.name()}));
                break;
            case 10:
                PaddleProxyTuning.reset(paddleProxyKind, leftHand);
                toast(client, class_2561.method_43469("punchy.tuning.toast.paddle_proxy_reset", new Object[]{paddleProxyKind.name()}));
                break;
            case 11:
                FishingLineProxyTuning.reset(fishingLineProxyKind, leftHand);
                toast(client, class_2561.method_43469("punchy.tuning.toast.fishing_line_proxy_reset", new Object[]{fishingLineProxyKind.name()}));
                break;
        }
    }

    private static void mirrorCurrentTarget(class_310 client) {
        boolean leftHand = hand == class_1268.field_5810;
        boolean otherLeft = !leftHand;
        switch (target.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                ToolTuning.Profile src = ToolTuning.get(currentKind, leftHand);
                if (src == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.item_profile_missing"));
                } else {
                    ToolTuning.apply(currentKind, otherLeft, src.mirror());
                    ToolTuning.markKindDetached(currentKind, otherLeft);
                    Object[] objArr = new Object[1];
                    objArr[ENABLE_TUNING_HOTKEYS] = otherLeft ? class_2561.method_43471("punchy.tuning.hand.off") : class_2561.method_43471("punchy.tuning.hand.main");
                    toast(client, class_2561.method_43469("punchy.tuning.toast.mirror_hand", objArr));
                }
                break;
            case 1:
                if (currentItemId == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.hold_item_mirror"));
                } else {
                    ToolTuning.Profile src2 = ToolTuning.getSpecificProfile(currentItemId, currentKind, leftHand);
                    if (src2 == null) {
                        toast(client, class_2561.method_43471("punchy.tuning.toast.specific_profile_missing_mirror"));
                    } else {
                        ToolTuning.Profile mirrored = src2.mirror();
                        ToolTuning.Profile right = leftHand ? mirrored : ToolTuning.getSpecificProfile(currentItemId, currentKind, false);
                        ToolTuning.Profile left = leftHand ? ToolTuning.getSpecificProfile(currentItemId, currentKind, true) : mirrored;
                        ToolTuning.applySpecific(currentItemId, currentKind, null, right, left);
                        Object[] objArr2 = new Object[1];
                        objArr2[ENABLE_TUNING_HOTKEYS] = otherLeft ? class_2561.method_43471("punchy.tuning.hand.off") : class_2561.method_43471("punchy.tuning.hand.main");
                        toast(client, class_2561.method_43469("punchy.tuning.toast.mirror_hand_specific", objArr2));
                    }
                }
                break;
            case 2:
                ToolTuning.Profile src3 = ToolTuning.getGlobal(leftHand);
                if (src3 == null) {
                    toast(client, class_2561.method_43471("punchy.tuning.toast.global_profile_missing"));
                } else {
                    ToolTuning.applyGlobal(otherLeft, src3.mirror());
                    ToolTuning.commitGlobal(otherLeft);
                    Object[] objArr3 = new Object[1];
                    objArr3[ENABLE_TUNING_HOTKEYS] = otherLeft ? class_2561.method_43471("punchy.tuning.hand.off") : class_2561.method_43471("punchy.tuning.hand.main");
                    toast(client, class_2561.method_43469("punchy.tuning.toast.global_mirror_hand", objArr3));
                }
                break;
            default:
                toast(client, class_2561.method_43471("punchy.tuning.toast.mirror_item_only"));
                break;
        }
    }

    private static void resetToolProfile(ToolTuning.Profile profile) {
        profile.offZ = 0.0f;
        profile.offY = 0.0f;
        profile.offX = 0.0f;
        profile.rotZ = 0.0f;
        profile.rotY = 0.0f;
        profile.rotX = 0.0f;
        profile.pivotZ = 0.0f;
        profile.pivotY = 0.0f;
        profile.pivotX = 0.0f;
        profile.recomputeRadians();
    }

    private static void resetArmProfile(ArmToolTuning.Profile profile, PlayerArmModelLayer.ToolKind kind, boolean leftHand) {
        ArmToolTuning.Profile profile2;
        if (kind == null) {
            profile2 = PunchyTuningConfig.getDefaultGlobalArmBase(leftHand);
        } else if (kind == PlayerArmModelLayer.ToolKind.EMPTY_HAND) {
            profile2 = PunchyTuningConfig.getDefaultArmEditorBase(kind, leftHand);
        } else {
            profile2 = new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        ArmToolTuning.Profile base = profile2;
        profile.offX = base.offX;
        profile.offY = base.offY;
        profile.offZ = base.offZ;
        profile.rotX = base.rotX;
        profile.rotY = base.rotY;
        profile.rotZ = base.rotZ;
        profile.scale = base.scale;
    }

    private static void resetArmMeshProfile(ArmMeshTuning.Profile profile) {
        profile.offZ = 0.0f;
        profile.offY = 0.0f;
        profile.offX = 0.0f;
        profile.rotZ = 0.0f;
        profile.rotY = 0.0f;
        profile.rotX = 0.0f;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static AdjustmentResult adjustToolProfile(ToolTuning.Profile profile, float delta) throws MatchException {
        AdjustmentResult adjustmentResult;
        char c;
        AdjustmentResult adjustmentResult2;
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f = profile.offX + delta;
                        profile.offX = f;
                        adjustmentResult = new AdjustmentResult("offX", f);
                        break;
                    case 1:
                        float f2 = profile.offY + delta;
                        profile.offY = f2;
                        adjustmentResult = new AdjustmentResult("offY", f2);
                        break;
                    case 2:
                        float f3 = profile.offZ + delta;
                        profile.offZ = f3;
                        adjustmentResult = new AdjustmentResult("offZ", f3);
                        break;
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
                break;
            case 1:
                ToolTuning.RotationOrder order = ToolTuning.rotationOrderForItemId(currentItemId);
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        c = 'X';
                        break;
                    case 1:
                        c = 'Y';
                        break;
                    case 2:
                        c = 'Z';
                        break;
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
                char rotationAxis = c;
                ToolTuning.rotateProfileLocal(profile, order, rotationAxis, delta);
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        adjustmentResult2 = new AdjustmentResult("rotX", profile.rotX);
                        break;
                    case 1:
                        adjustmentResult2 = new AdjustmentResult("rotY", profile.rotY);
                        break;
                    case 2:
                        adjustmentResult2 = new AdjustmentResult("rotZ", profile.rotZ);
                        break;
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
                AdjustmentResult res = adjustmentResult2;
                adjustmentResult = res;
                break;
            case 2:
                float fMax = Math.max(0.01f, profile.scale + delta);
                profile.scale = fMax;
                adjustmentResult = new AdjustmentResult("scale", fMax);
                break;
            case 3:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f4 = profile.pivotX + delta;
                        profile.pivotX = f4;
                        adjustmentResult = new AdjustmentResult("pivotX", f4);
                        break;
                    case 1:
                        float f5 = profile.pivotY + delta;
                        profile.pivotY = f5;
                        adjustmentResult = new AdjustmentResult("pivotY", f5);
                        break;
                    case 2:
                        float f6 = profile.pivotZ + delta;
                        profile.pivotZ = f6;
                        adjustmentResult = new AdjustmentResult("pivotZ", f6);
                        break;
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        AdjustmentResult result = adjustmentResult;
        return result;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static AdjustmentResult adjustArmProfile(ArmToolTuning.Profile profile, float delta) throws MatchException {
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f = profile.offX + delta;
                        profile.offX = f;
                        return new AdjustmentResult("offX", f);
                    case 1:
                        float f2 = profile.offY + delta;
                        profile.offY = f2;
                        return new AdjustmentResult("offY", f2);
                    case 2:
                        float f3 = profile.offZ + delta;
                        profile.offZ = f3;
                        return new AdjustmentResult("offZ", f3);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 1:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f4 = profile.rotX + delta;
                        profile.rotX = f4;
                        return new AdjustmentResult("rotX", f4);
                    case 1:
                        float f5 = profile.rotY + delta;
                        profile.rotY = f5;
                        return new AdjustmentResult("rotY", f5);
                    case 2:
                        float f6 = profile.rotZ + delta;
                        profile.rotZ = f6;
                        return new AdjustmentResult("rotZ", f6);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 2:
                float fMax = Math.max(0.25f, profile.scale + delta);
                profile.scale = fMax;
                return new AdjustmentResult("scale", fMax);
            case 3:
                throw new IllegalStateException("Pivot editing should be gated for arms");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static AdjustmentResult adjustArmMeshProfile(ArmMeshTuning.Profile profile, float delta) throws MatchException {
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f = profile.offX + delta;
                        profile.offX = f;
                        return new AdjustmentResult("offX", f);
                    case 1:
                        float f2 = profile.offY + delta;
                        profile.offY = f2;
                        return new AdjustmentResult("offY", f2);
                    case 2:
                        float f3 = profile.offZ + delta;
                        profile.offZ = f3;
                        return new AdjustmentResult("offZ", f3);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 1:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f4 = profile.rotX + delta;
                        profile.rotX = f4;
                        return new AdjustmentResult("rotX", f4);
                    case 1:
                        float f5 = profile.rotY + delta;
                        profile.rotY = f5;
                        return new AdjustmentResult("rotY", f5);
                    case 2:
                        float f6 = profile.rotZ + delta;
                        profile.rotZ = f6;
                        return new AdjustmentResult("rotZ", f6);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 2:
                float fMax = Math.max(0.25f, profile.scale + delta);
                profile.scale = fMax;
                return new AdjustmentResult("scale", fMax);
            case 3:
                throw new IllegalStateException("Pivot editing should be gated for arms");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static AdjustmentResult adjustChestProxyProfile(ChestProxyTuning.Profile profile, float delta) throws MatchException {
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f = profile.offX + delta;
                        profile.offX = f;
                        return new AdjustmentResult("offX", f);
                    case 1:
                        float f2 = profile.offY + delta;
                        profile.offY = f2;
                        return new AdjustmentResult("offY", f2);
                    case 2:
                        float f3 = profile.offZ + delta;
                        profile.offZ = f3;
                        return new AdjustmentResult("offZ", f3);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 1:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f4 = profile.rotX + delta;
                        profile.rotX = f4;
                        return new AdjustmentResult("rotX", f4);
                    case 1:
                        float f5 = profile.rotY + delta;
                        profile.rotY = f5;
                        return new AdjustmentResult("rotY", f5);
                    case 2:
                        float f6 = profile.rotZ + delta;
                        profile.rotZ = f6;
                        return new AdjustmentResult("rotZ", f6);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 2:
                float fMax = Math.max(0.01f, profile.scale + delta);
                profile.scale = fMax;
                return new AdjustmentResult("scale", fMax);
            case 3:
                throw new IllegalStateException("Pivot editing should be gated for chest proxy");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static AdjustmentResult adjustFluidProxyProfile(FluidProxyTuning.Profile profile, float delta) throws MatchException {
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f = profile.offX + delta;
                        profile.offX = f;
                        return new AdjustmentResult("offX", f);
                    case 1:
                        float f2 = profile.offY + delta;
                        profile.offY = f2;
                        return new AdjustmentResult("offY", f2);
                    case 2:
                        float f3 = profile.offZ + delta;
                        profile.offZ = f3;
                        return new AdjustmentResult("offZ", f3);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 1:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f4 = profile.rotX + delta;
                        profile.rotX = f4;
                        return new AdjustmentResult("rotX", f4);
                    case 1:
                        float f5 = profile.rotY + delta;
                        profile.rotY = f5;
                        return new AdjustmentResult("rotY", f5);
                    case 2:
                        float f6 = profile.rotZ + delta;
                        profile.rotZ = f6;
                        return new AdjustmentResult("rotZ", f6);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 2:
                float fMax = Math.max(0.01f, profile.scale + delta);
                profile.scale = fMax;
                return new AdjustmentResult("scale", fMax);
            case 3:
                throw new IllegalStateException("Pivot editing should be gated for fluid proxy");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static AdjustmentResult adjustEnchantParticleProxyProfile(EnchantParticleProxyTuning.Profile profile, float delta) throws MatchException {
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f = profile.offX + delta;
                        profile.offX = f;
                        return new AdjustmentResult("offX", f);
                    case 1:
                        float f2 = profile.offY + delta;
                        profile.offY = f2;
                        return new AdjustmentResult("offY", f2);
                    case 2:
                        float f3 = profile.offZ + delta;
                        profile.offZ = f3;
                        return new AdjustmentResult("offZ", f3);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 1:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f4 = profile.rotX + delta;
                        profile.rotX = f4;
                        return new AdjustmentResult("rotX", f4);
                    case 1:
                        float f5 = profile.rotY + delta;
                        profile.rotY = f5;
                        return new AdjustmentResult("rotY", f5);
                    case 2:
                        float f6 = profile.rotZ + delta;
                        profile.rotZ = f6;
                        return new AdjustmentResult("rotZ", f6);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 2:
                float fMax = Math.max(0.01f, profile.scale + delta);
                profile.scale = fMax;
                return new AdjustmentResult("scale", fMax);
            case 3:
                throw new IllegalStateException("Pivot editing should be gated for particle proxy");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static AdjustmentResult adjustPaddleProxyProfile(PaddleProxyTuning.Profile profile, float delta) throws MatchException {
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f = profile.offX + delta;
                        profile.offX = f;
                        return new AdjustmentResult("offX", f);
                    case 1:
                        float f2 = profile.offY + delta;
                        profile.offY = f2;
                        return new AdjustmentResult("offY", f2);
                    case 2:
                        float f3 = profile.offZ + delta;
                        profile.offZ = f3;
                        return new AdjustmentResult("offZ", f3);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 1:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f4 = profile.rotX + delta;
                        profile.rotX = f4;
                        return new AdjustmentResult("rotX", f4);
                    case 1:
                        float f5 = profile.rotY + delta;
                        profile.rotY = f5;
                        return new AdjustmentResult("rotY", f5);
                    case 2:
                        float f6 = profile.rotZ + delta;
                        profile.rotZ = f6;
                        return new AdjustmentResult("rotZ", f6);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 2:
                float fMax = Math.max(0.01f, profile.scale + delta);
                profile.scale = fMax;
                return new AdjustmentResult("scale", fMax);
            case 3:
                throw new IllegalStateException("Pivot editing should be gated for paddle proxy");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static AdjustmentResult adjustFishingLineProxyProfile(FishingLineProxyTuning.Profile profile, float delta) throws MatchException {
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f = profile.offX + delta;
                        profile.offX = f;
                        return new AdjustmentResult("offX", f);
                    case 1:
                        float f2 = profile.offY + delta;
                        profile.offY = f2;
                        return new AdjustmentResult("offY", f2);
                    case 2:
                        float f3 = profile.offZ + delta;
                        profile.offZ = f3;
                        return new AdjustmentResult("offZ", f3);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 1:
                switch (axis.ordinal()) {
                    case ENABLE_TUNING_HOTKEYS /* 0 */:
                        float f4 = profile.rotX + delta;
                        profile.rotX = f4;
                        return new AdjustmentResult("rotX", f4);
                    case 1:
                        float f5 = profile.rotY + delta;
                        profile.rotY = f5;
                        return new AdjustmentResult("rotY", f5);
                    case 2:
                        float f6 = profile.rotZ + delta;
                        profile.rotZ = f6;
                        return new AdjustmentResult("rotZ", f6);
                    default:
                        throw new MatchException((String) null, (Throwable) null);
                }
            case 2:
                float fMax = Math.max(0.01f, profile.scale + delta);
                profile.scale = fMax;
                return new AdjustmentResult("scale", fMax);
            case 3:
                throw new IllegalStateException("Pivot editing should be gated for fishing line proxy");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static float computeStep(class_310 client) throws MatchException {
        float f;
        switch (mode.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
            case 3:
                f = 0.01f;
                break;
            case 1:
                f = 1.0f;
                break;
            case 2:
                f = 0.01f;
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        float base = f;
        if (client.field_1690 == null) {
            return base;
        }
        if (client.field_1690.field_1867.method_1434()) {
            base *= 5.0f;
        }
        if (client.field_1690.field_1832.method_1434()) {
            base *= 0.2f;
        }
        return base;
    }

    private static void report(class_310 client, AdjustmentResult result) {
        if (result == null) {
            return;
        }
        toast(client, describeState(result));
    }

    private static class_2561 describeState() {
        return describeState(null);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 describeState(AdjustmentResult change) throws MatchException {
        class_2561 targetLabel = targetLabel(target);
        class_2561 selectionLabel = selectionLabel();
        class_2561 handLabel = handLabel(hand);
        class_2561 modeLabel = modeLabel(mode);
        class_2561 axisLabel = axisLabel(axis);
        if (change != null) {
            class_2561 property = propertyLabel(change.property);
            String valueStr = String.format(Locale.ROOT, "%.4f", Float.valueOf(change.value));
            if (mode.usesAxis) {
                return class_2561.method_43469("punchy.tuning.status.with_axis_change", new Object[]{targetLabel, selectionLabel, handLabel, modeLabel, axisLabel, property, valueStr});
            }
            return class_2561.method_43469("punchy.tuning.status.no_axis_change", new Object[]{targetLabel, selectionLabel, handLabel, modeLabel, property, valueStr});
        }
        if (mode.usesAxis) {
            return class_2561.method_43469("punchy.tuning.status.with_axis", new Object[]{targetLabel, selectionLabel, handLabel, modeLabel, axisLabel});
        }
        return class_2561.method_43469("punchy.tuning.status.no_axis", new Object[]{targetLabel, selectionLabel, handLabel, modeLabel});
    }

    private static void toast(class_310 client, class_2561 message) {
        if (client == null || client.field_1724 == null) {
            return;
        }
        client.field_1705.method_1758(message, false);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 targetLabel(Target target2) throws MatchException {
        switch (target2.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                return class_2561.method_43471("punchy.tuning.target.item_kind");
            case 1:
                return class_2561.method_43471("punchy.tuning.target.item_specific");
            case 2:
                return class_2561.method_43471("punchy.tuning.target.item_global");
            case 3:
                return class_2561.method_43471("punchy.tuning.target.arm_kind");
            case 4:
                return class_2561.method_43471("punchy.tuning.target.arm_specific");
            case 5:
                return class_2561.method_43471("punchy.tuning.target.arm_global");
            case 6:
                return class_2561.method_43471("punchy.tuning.target.arm_mesh");
            case 7:
                return class_2561.method_43471("punchy.tuning.target.chest_proxy");
            case 8:
                return class_2561.method_43471("punchy.tuning.target.fluid_proxy");
            case 9:
                return class_2561.method_43471("punchy.tuning.target.enchant_proxy");
            case 10:
                return class_2561.method_43471("punchy.tuning.target.paddle_proxy");
            case 11:
                return class_2561.method_43471("punchy.tuning.target.fishing_line_proxy");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 selectionLabel() throws MatchException {
        switch (target.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
            case 3:
                return class_2561.method_43470(friendlyName(currentKind.name()));
            case 1:
            case 4:
                if (currentItemId != null) {
                    return class_2561.method_43470(currentItemId.toString());
                }
                return class_2561.method_43471("punchy.tuning.selection.none_item");
            case 2:
                return class_2561.method_43471("punchy.tuning.selection.global_item");
            case 5:
                return class_2561.method_43471("punchy.tuning.selection.global_arm");
            case 6:
                return class_2561.method_43471("punchy.tuning.selection.arm_mesh");
            case 7:
                return class_2561.method_43469("punchy.tuning.selection.chest_proxy", new Object[]{chestProxyKind.name()});
            case 8:
                return class_2561.method_43469("punchy.tuning.selection.fluid_proxy", new Object[]{fluidProxyKind.name()});
            case 9:
                return class_2561.method_43469("punchy.tuning.selection.enchant_proxy", new Object[]{enchantParticleKind.name()});
            case 10:
                return class_2561.method_43469("punchy.tuning.selection.paddle_proxy", new Object[]{paddleProxyKind.name()});
            case 11:
                return class_2561.method_43469("punchy.tuning.selection.fishing_line_proxy", new Object[]{fishingLineProxyKind.name()});
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    private static class_2561 handLabel(class_1268 hand2) {
        if (hand2 == class_1268.field_5808) {
            return class_2561.method_43471("punchy.tuning.hand.main");
        }
        return class_2561.method_43471("punchy.tuning.hand.off");
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 modeLabel(Mode mode2) throws MatchException {
        switch (mode2.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                return class_2561.method_43471("punchy.tuning.mode.translate");
            case 1:
                return class_2561.method_43471("punchy.tuning.mode.rotate");
            case 2:
                return class_2561.method_43471("punchy.tuning.mode.scale");
            case 3:
                return class_2561.method_43471("punchy.tuning.mode.pivot");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 axisLabel(Axis axis2) throws MatchException {
        switch (axis2.ordinal()) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                return class_2561.method_43471("punchy.tuning.axis.x");
            case 1:
                return class_2561.method_43471("punchy.tuning.axis.y");
            case 2:
                return class_2561.method_43471("punchy.tuning.axis.z");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private static class_2561 propertyLabel(String property) {
        byte b = -1;
        switch (property.hashCode()) {
            case -987906986:
                if (property.equals("pivotX")) {
                    b = 7;
                }
                break;
            case -987906985:
                if (property.equals("pivotY")) {
                    b = 8;
                }
                break;
            case -987906984:
                if (property.equals("pivotZ")) {
                    b = 9;
                }
                break;
            case 3408073:
                if (property.equals("offX")) {
                    b = ENABLE_TUNING_HOTKEYS;
                }
                break;
            case 3408074:
                if (property.equals("offY")) {
                    b = 1;
                }
                break;
            case 3408075:
                if (property.equals("offZ")) {
                    b = 2;
                }
                break;
            case 3506529:
                if (property.equals("rotX")) {
                    b = 3;
                }
                break;
            case 3506530:
                if (property.equals("rotY")) {
                    b = 4;
                }
                break;
            case 3506531:
                if (property.equals("rotZ")) {
                    b = 5;
                }
                break;
            case 109250890:
                if (property.equals("scale")) {
                    b = 6;
                }
                break;
        }
        switch (b) {
            case ENABLE_TUNING_HOTKEYS /* 0 */:
                return class_2561.method_43471("punchy.tuning.property.off_x");
            case 1:
                return class_2561.method_43471("punchy.tuning.property.off_y");
            case 2:
                return class_2561.method_43471("punchy.tuning.property.off_z");
            case 3:
                return class_2561.method_43471("punchy.tuning.property.rot_x");
            case 4:
                return class_2561.method_43471("punchy.tuning.property.rot_y");
            case 5:
                return class_2561.method_43471("punchy.tuning.property.rot_z");
            case 6:
                return class_2561.method_43471("punchy.tuning.property.scale");
            case 7:
                return class_2561.method_43471("punchy.tuning.property.pivot_x");
            case 8:
                return class_2561.method_43471("punchy.tuning.property.pivot_y");
            case 9:
                return class_2561.method_43471("punchy.tuning.property.pivot_z");
            default:
                return class_2561.method_43470(property);
        }
    }

    private static String friendlyName(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(ENABLE_TUNING_HOTKEYS)) + lower.substring(1);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningKeybinds$AdjustmentResult.class */
    private static final class AdjustmentResult extends Record {
        private final String property;
        private final float value;

        private AdjustmentResult(String property, float value) {
            this.property = property;
            this.value = value;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, AdjustmentResult.class), AdjustmentResult.class, "property;value", "FIELD:Lpunchy/client/tuning/TuningKeybinds$AdjustmentResult;->property:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/TuningKeybinds$AdjustmentResult;->value:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, AdjustmentResult.class), AdjustmentResult.class, "property;value", "FIELD:Lpunchy/client/tuning/TuningKeybinds$AdjustmentResult;->property:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/TuningKeybinds$AdjustmentResult;->value:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, AdjustmentResult.class, Object.class), AdjustmentResult.class, "property;value", "FIELD:Lpunchy/client/tuning/TuningKeybinds$AdjustmentResult;->property:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/TuningKeybinds$AdjustmentResult;->value:F").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String property() {
            return this.property;
        }

        public float value() {
            return this.value;
        }
    }
}
