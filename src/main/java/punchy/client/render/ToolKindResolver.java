package punchy.client.render;

import java.util.Set;
import net.minecraft.class_1747;
import net.minecraft.class_1749;
import net.minecraft.class_1753;
import net.minecraft.class_1755;
import net.minecraft.class_1764;
import net.minecraft.class_1785;
import net.minecraft.class_1786;
import net.minecraft.class_1787;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1808;
import net.minecraft.class_1819;
import net.minecraft.class_1835;
import net.minecraft.class_1839;
import net.minecraft.class_2244;
import net.minecraft.class_2248;
import net.minecraft.class_2269;
import net.minecraft.class_2338;
import net.minecraft.class_2440;
import net.minecraft.class_2527;
import net.minecraft.class_2533;
import net.minecraft.class_2577;
import net.minecraft.class_2680;
import net.minecraft.class_2682;
import net.minecraft.class_2960;
import net.minecraft.class_3489;
import net.minecraft.class_4739;
import net.minecraft.class_5538;
import net.minecraft.class_6862;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_9334;
import net.minecraft.class_9362;
import punchy.client.renderer.layer.PlayerArmModelLayer;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolKindResolver.class */
public final class ToolKindResolver {
    private static final class_6862<class_1792> SPEAR_TAG = class_6862.method_40092(class_7924.field_41197, class_2960.method_60655("minecraft", "spears"));
    private static final class_6862<class_1792> BUILDING_BLOCKS_TAG = class_6862.method_40092(class_7924.field_41197, class_2960.method_60655("minecraft", "building_blocks"));
    private static final class_6862<class_1792> COLORED_BLOCKS_TAG = class_6862.method_40092(class_7924.field_41197, class_2960.method_60655("minecraft", "colored_blocks"));
    private static final class_6862<class_1792> NATURAL_BLOCKS_TAG = class_6862.method_40092(class_7924.field_41197, class_2960.method_60655("minecraft", "natural_blocks"));
    private static final class_6862<class_1792> FUNCTIONAL_BLOCKS_TAG = class_6862.method_40092(class_7924.field_41197, class_2960.method_60655("minecraft", "functional_blocks"));
    private static final class_6862<class_1792> FISHING_RODS_TAG = class_6862.method_40092(class_7924.field_41197, class_2960.method_60655("minecraft", "fishing_rods"));
    private static final Set<class_1792> REDSTONE_COMPONENTS = Set.of((Object[]) new class_1792[]{class_1802.field_8725, class_1802.field_8793, class_1802.field_8530, class_1802.field_8619, class_1802.field_8857, class_1802.field_8230, class_1802.field_8537, class_1802.field_8865, class_1802.field_8357, class_1802.field_8878, class_1802.field_8643, class_1802.field_8566, class_1802.field_8366, class_1802.field_22420, class_1802.field_8239, class_1802.field_8249, class_1802.field_8105, class_1802.field_27051, class_1802.field_8848, class_1802.field_8211, class_1802.field_8655, class_1802.field_28101, class_1802.field_43194});

    private ToolKindResolver() {
    }

    private static boolean isRedstoneComponent(class_1792 item) {
        return REDSTONE_COMPONENTS.contains(item);
    }

    private static boolean isCreatureBucket(class_1792 item) {
        return item == class_1802.field_28354 || item == class_1802.field_8478 || item == class_1802.field_8108 || item == class_1802.field_8666 || item == class_1802.field_8714 || item == class_1802.field_37533;
    }

    private static boolean isBucket(class_1792 item) {
        if (item == null) {
            return false;
        }
        if ((item instanceof class_1785) || (item instanceof class_1755)) {
            return true;
        }
        class_2960 id = class_7923.field_41178.method_10221(item);
        return id != null && id.method_12832().contains("bucket");
    }

    private static boolean isMinecart(class_1792 item) {
        return item instanceof class_1808;
    }

    private static boolean isBoat(class_1792 item) {
        return item instanceof class_1749;
    }

    private static boolean isButtonOrPlate(class_1747 blockItem) {
        class_2248 block = blockItem.method_7711();
        return (block instanceof class_2269) || (block instanceof class_2440);
    }

    private static boolean isTrapdoor(class_1747 blockItem) {
        return blockItem.method_7711() instanceof class_2533;
    }

    private static boolean isCarpet(class_1747 blockItem) {
        return blockItem.method_7711() instanceof class_2577;
    }

    private static boolean isBed(class_1747 blockItem) {
        return blockItem.method_7711() instanceof class_2244;
    }

    private static boolean isChest(class_1747 blockItem) {
        return blockItem.method_7711() instanceof class_4739;
    }

    private static boolean isCopperLantern(class_1792 item) {
        class_2960 id = class_7923.field_41178.method_10221(item);
        return id != null && id.method_12832().contains("copper_lantern");
    }

    private static boolean isFullCube(class_1747 blockItem) {
        class_2680 state = blockItem.method_7711().method_9564();
        return state.method_26234(class_2682.field_12294, class_2338.field_10980) && class_2248.method_9614(state.method_26220(class_2682.field_12294, class_2338.field_10980));
    }

    private static boolean isSpearStack(class_1799 stack) {
        if (stack == null || stack.method_7960() || ToolTuning.getSpecificCustomAnimation(stack) != null || (stack.method_7909() instanceof class_1835)) {
            return false;
        }
        return stack.method_31573(SPEAR_TAG) || stack.method_7976() == class_1839.field_8951;
    }

    private static boolean isFishingRodStack(class_1799 stack) {
        class_1792 item;
        if (stack == null || stack.method_7960()) {
            return false;
        }
        if (stack.method_31573(FISHING_RODS_TAG) || (item = stack.method_7909()) == class_1802.field_8184 || item == class_1802.field_23254) {
            return true;
        }
        return item instanceof class_1787;
    }

    public static VanillaFirstPersonItemLayer.ToolKind resolveItemKind(class_1799 stack, VanillaFirstPersonItemLayer.ToolKind fallback) {
        if (stack == null || stack.method_7960()) {
            return VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND;
        }
        VanillaFirstPersonItemLayer.ToolKind overridden = ToolTuning.resolveOverriddenKind(stack);
        if (overridden != null) {
            return overridden;
        }
        class_1792 item = stack == null ? null : stack.method_7909();
        if (item == null) {
            return fallback;
        }
        if (stack.method_31573(class_3489.field_42611)) {
            return VanillaFirstPersonItemLayer.ToolKind.SWORD;
        }
        if (stack.method_31573(class_3489.field_42612)) {
            return VanillaFirstPersonItemLayer.ToolKind.AXE;
        }
        if (stack.method_31573(class_3489.field_42614)) {
            return VanillaFirstPersonItemLayer.ToolKind.PICKAXE;
        }
        if (stack.method_31573(class_3489.field_42615)) {
            return VanillaFirstPersonItemLayer.ToolKind.SHOVEL;
        }
        if (stack.method_31573(class_3489.field_42613)) {
            return VanillaFirstPersonItemLayer.ToolKind.HOE;
        }
        if (isSpearStack(stack)) {
            return VanillaFirstPersonItemLayer.ToolKind.SPEAR;
        }
        if (stack.method_58694(class_9334.field_50075) != null) {
            return VanillaFirstPersonItemLayer.ToolKind.FOOD;
        }
        if (item == class_1802.field_16539 || item == class_1802.field_22016 || isCopperLantern(item)) {
            return VanillaFirstPersonItemLayer.ToolKind.LANTERN;
        }
        if (isCreatureBucket(item)) {
            return VanillaFirstPersonItemLayer.ToolKind.CREATURE_BUCKET;
        }
        if (isBucket(item)) {
            return VanillaFirstPersonItemLayer.ToolKind.BUCKET;
        }
        if (isMinecart(item)) {
            return VanillaFirstPersonItemLayer.ToolKind.MINECART;
        }
        if (isBoat(item)) {
            return VanillaFirstPersonItemLayer.ToolKind.BOAT;
        }
        if (item instanceof class_1753) {
            return VanillaFirstPersonItemLayer.ToolKind.BOW;
        }
        if (item == class_1802.field_8107) {
            return VanillaFirstPersonItemLayer.ToolKind.ARROW;
        }
        if (item instanceof class_1764) {
            return VanillaFirstPersonItemLayer.ToolKind.CROSSBOW;
        }
        if (item instanceof class_1819) {
            return VanillaFirstPersonItemLayer.ToolKind.SHIELD;
        }
        if (isFishingRodStack(stack)) {
            return VanillaFirstPersonItemLayer.ToolKind.FISHING_ROD;
        }
        if (item instanceof class_1835) {
            return VanillaFirstPersonItemLayer.ToolKind.TRIDENT;
        }
        if (item instanceof class_5538) {
            return VanillaFirstPersonItemLayer.ToolKind.SPYGLASS;
        }
        if (item instanceof class_1786) {
            return VanillaFirstPersonItemLayer.ToolKind.FLINT;
        }
        if (item instanceof class_9362) {
            return VanillaFirstPersonItemLayer.ToolKind.MACE;
        }
        if (item instanceof class_1747) {
            class_1747 blockItem = (class_1747) item;
            if (blockItem.method_7711() instanceof class_2527) {
                return VanillaFirstPersonItemLayer.ToolKind.TORCH;
            }
            if (isChest(blockItem)) {
                return VanillaFirstPersonItemLayer.ToolKind.CHEST;
            }
            if (isButtonOrPlate(blockItem)) {
                return VanillaFirstPersonItemLayer.ToolKind.BUTTON_PLATE;
            }
            if (isTrapdoor(blockItem)) {
                return VanillaFirstPersonItemLayer.ToolKind.TRAPDOOR;
            }
            if (isCarpet(blockItem)) {
                return VanillaFirstPersonItemLayer.ToolKind.CARPET;
            }
            if (isBed(blockItem)) {
                return VanillaFirstPersonItemLayer.ToolKind.BED;
            }
            if (isFullCube(blockItem)) {
                return VanillaFirstPersonItemLayer.ToolKind.FULL_BLOCK;
            }
            if (stack.method_31573(BUILDING_BLOCKS_TAG)) {
                return VanillaFirstPersonItemLayer.ToolKind.BUILDING_BLOCKS;
            }
            if (stack.method_31573(COLORED_BLOCKS_TAG)) {
                return VanillaFirstPersonItemLayer.ToolKind.COLORED_BLOCKS;
            }
            if (stack.method_31573(NATURAL_BLOCKS_TAG)) {
                return VanillaFirstPersonItemLayer.ToolKind.NATURAL_BLOCKS;
            }
            if (stack.method_31573(FUNCTIONAL_BLOCKS_TAG)) {
                return VanillaFirstPersonItemLayer.ToolKind.FUNCTIONAL_BLOCKS;
            }
            return VanillaFirstPersonItemLayer.ToolKind.BLOCK;
        }
        if (isRedstoneComponent(item)) {
            return VanillaFirstPersonItemLayer.ToolKind.REDSTONE;
        }
        return VanillaFirstPersonItemLayer.ToolKind.INGREDIENT;
    }

    public static PlayerArmModelLayer.ToolKind resolveArmKind(class_1799 stack, PlayerArmModelLayer.ToolKind fallback) {
        if (stack == null || stack.method_7960()) {
            return PlayerArmModelLayer.ToolKind.EMPTY_HAND;
        }
        VanillaFirstPersonItemLayer.ToolKind itemKind = resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        if (itemKind != null) {
            try {
                return PlayerArmModelLayer.ToolKind.valueOf(itemKind.name());
            } catch (IllegalArgumentException e) {
            }
        }
        class_1792 item = stack == null ? null : stack.method_7909();
        if (item == null) {
            return fallback;
        }
        if (stack.method_31573(class_3489.field_42611)) {
            return PlayerArmModelLayer.ToolKind.SWORD;
        }
        if (stack.method_31573(class_3489.field_42612)) {
            return PlayerArmModelLayer.ToolKind.AXE;
        }
        if (stack.method_31573(class_3489.field_42614)) {
            return PlayerArmModelLayer.ToolKind.PICKAXE;
        }
        if (stack.method_31573(class_3489.field_42615)) {
            return PlayerArmModelLayer.ToolKind.SHOVEL;
        }
        if (stack.method_31573(class_3489.field_42613)) {
            return PlayerArmModelLayer.ToolKind.HOE;
        }
        if (isSpearStack(stack)) {
            return PlayerArmModelLayer.ToolKind.SPEAR;
        }
        if (stack.method_58694(class_9334.field_50075) != null) {
            return PlayerArmModelLayer.ToolKind.FOOD;
        }
        if (item instanceof class_1753) {
            return PlayerArmModelLayer.ToolKind.BOW;
        }
        if (item instanceof class_1764) {
            return PlayerArmModelLayer.ToolKind.CROSSBOW;
        }
        if (item instanceof class_1819) {
            return PlayerArmModelLayer.ToolKind.SHIELD;
        }
        if (item instanceof class_1835) {
            return PlayerArmModelLayer.ToolKind.TRIDENT;
        }
        if (item instanceof class_9362) {
            return PlayerArmModelLayer.ToolKind.MACE;
        }
        if (item instanceof class_1747) {
            return PlayerArmModelLayer.ToolKind.BLOCK;
        }
        return fallback;
    }
}
