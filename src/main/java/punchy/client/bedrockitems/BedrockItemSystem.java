package punchy.client.bedrockitems;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1306;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3300;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_6862;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import org.joml.Matrix4f;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.bedrockitems.BedrockAxolPhysicsStateMachine;
import punchy.client.bedrockitems.BedrockItemDefinition;
import punchy.client.bedrockitems.BedrockPufferPhysicsStateMachine;
import punchy.client.render.ToolTuning;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemSystem.class */
public final class BedrockItemSystem {
    private static final BedrockItemSystem INSTANCE = new BedrockItemSystem();
    private static final EnumMap<class_1306, Matrix4f> CURRENT_PHYSICS_DELTA = new EnumMap<>(class_1306.class);
    private final List<BedrockItemDefinition> definitions = new ArrayList();
    private final BedrockAxolPhysicsStateMachine axolPhysicsState = new BedrockAxolPhysicsStateMachine();
    private final BedrockPufferPhysicsStateMachine pufferPhysicsState = new BedrockPufferPhysicsStateMachine();

    private BedrockItemSystem() {
    }

    public static BedrockItemSystem get() {
        return INSTANCE;
    }

    public static void setCurrentPhysicsDelta(class_1306 arm, Matrix4f delta) {
        if (arm == null) {
            return;
        }
        if (delta == null) {
            CURRENT_PHYSICS_DELTA.remove(arm);
        } else {
            CURRENT_PHYSICS_DELTA.put(arm, new Matrix4f(delta));
        }
    }

    public static Matrix4f getCurrentPhysicsDelta(class_1306 arm) {
        Matrix4f stored;
        if (arm == null || (stored = CURRENT_PHYSICS_DELTA.get(arm)) == null) {
            return null;
        }
        return new Matrix4f(stored);
    }

    public static void clearCurrentPhysicsDelta(class_1306 arm) {
        if (arm != null) {
            CURRENT_PHYSICS_DELTA.remove(arm);
        }
    }

    public void onResourceReload(class_3300 manager) {
        if (PunchyConfig.disableResourcePackModelParts()) {
            return;
        }
        this.definitions.clear();
        this.definitions.addAll(BedrockItemDefinition.loadAll(manager));
        for (BedrockItemDefinition def : this.definitions) {
            def.loadResources(manager);
        }
    }

    public void tick(class_310 client) {
        if (PunchyConfig.disableResourcePackModelParts()) {
            return;
        }
        PunchyAnimationManager.updateItemOverrides(client);
        BedrockItemAnimationManager.tick(client);
        this.axolPhysicsState.tick(client);
        this.pufferPhysicsState.tick(client);
    }

    BedrockAxolPhysicsStateMachine.FeedbackPose getAxolPhysicsPose(class_1306 arm, float partialTicks) {
        return this.axolPhysicsState.getPose(arm, partialTicks);
    }

    BedrockPufferPhysicsStateMachine.FeedbackPose getPufferPhysicsPose(class_1306 arm, float partialTicks) {
        return this.pufferPhysicsState.getPose(arm, partialTicks);
    }

    public boolean hasSlimeBonePhysics(class_1799 stack) {
        if (PunchyConfig.disableResourcePackModelParts() || stack == null || stack.method_7960()) {
            return false;
        }
        BedrockItemDefinition def = resolveDefinition(stack);
        if (def != null && def.hasSlimeBonePhysics()) {
            return true;
        }
        JsonElement tuningBonePhysics = ToolTuning.getSpecificBonePhysics(stack);
        if (tuningBonePhysics == null) {
            return false;
        }
        Map<String, BedrockItemDefinition.BonePhysicsSpec> parsed = BedrockItemDefinition.parseBonePhysics(tuningBonePhysics);
        for (BedrockItemDefinition.BonePhysicsSpec spec : parsed.values()) {
            if (spec != null && spec.enableSlimePhysics()) {
                return true;
            }
        }
        return false;
    }

    public boolean tryRenderItem(class_1799 stack, class_746 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int packedLight, float partialTicks, boolean applyDisplay) {
        BedrockItemDefinition def;
        if (PunchyConfig.disableResourcePackModelParts() || stack == null || stack.method_7960() || player == null || arm == null || isBlockedByCustomName(stack) || (def = resolveDefinition(stack)) == null) {
            return false;
        }
        return BedrockItemRenderer.render(def, stack, player, arm, poseStack, buffer, packedLight, partialTicks, applyDisplay);
    }

    BedrockItemDefinition resolveDefinition(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return null;
        }
        BedrockItemDefinition fallback = null;
        boolean hasNamedMatch = false;
        for (BedrockItemDefinition def : this.definitions) {
            if (def != null && matchesItemList(stack, def.getItems())) {
                if (def.hasCustomName()) {
                    hasNamedMatch = true;
                    if (def.matchesCustomName(stack)) {
                        return def;
                    }
                } else if (fallback == null) {
                    fallback = def;
                }
            }
        }
        if (hasNamedMatch) {
            return null;
        }
        return fallback;
    }

    static boolean matchesItemList(class_1799 stack, List<String> items) {
        class_2960 itemId;
        if (stack == null || stack.method_7960() || items == null || items.isEmpty() || (itemId = class_7923.field_41178.method_10221(stack.method_7909())) == null) {
            return false;
        }
        String current = itemId.toString();
        for (String entry : items) {
            if (entry != null && !entry.isBlank()) {
                String trimmed = entry.trim();
                if (trimmed.startsWith("#")) {
                    class_2960 tagId = class_2960.method_12829(trimmed.substring(1));
                    if (tagId != null) {
                        class_6862<class_1792> tagKey = class_6862.method_40092(class_7924.field_41197, tagId);
                        if (stack.method_31573(tagKey)) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                } else {
                    class_2960 parsed = class_2960.method_12829(trimmed);
                    if (parsed != null && current.equals(parsed.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    boolean isBlockedByCustomName(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        boolean namedExists = false;
        for (BedrockItemDefinition def : this.definitions) {
            if (def != null && matchesItemList(stack, def.getItems()) && def.hasCustomName()) {
                namedExists = true;
                if (def.matchesCustomName(stack)) {
                    return false;
                }
            }
        }
        return namedExists;
    }
}
