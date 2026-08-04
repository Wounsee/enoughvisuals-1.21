package fun.wounsee.cosmetic.model.rendertasks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.ducks.FiguraItemRendererExtension;
import fun.wounsee.cosmetic.lua.LuaNotNil;
import fun.wounsee.cosmetic.lua.LuaWhitelist;
import fun.wounsee.cosmetic.lua.api.world.ItemStackAPI;
import fun.wounsee.cosmetic.lua.api.world.WorldAPI;
import fun.wounsee.cosmetic.lua.docs.LuaMethodDoc;
import fun.wounsee.cosmetic.lua.docs.LuaMethodOverload;
import fun.wounsee.cosmetic.lua.docs.LuaTypeDoc;
import fun.wounsee.cosmetic.model.CosmeticModelPart;
import fun.wounsee.cosmetic.utils.LuaUtils;
import org.luaj.vm2.LuaError;

import java.util.Locale;

@LuaWhitelist
@LuaTypeDoc(
        name = "ItemTask",
        value = "item_task"
)
public class ItemTask extends RenderTask {

    private ItemStack item;
    private ItemDisplayContext displayMode = ItemDisplayContext.NONE;
    private boolean left = false;
    private int cachedComplexity;

    public ItemTask(String name, Avatar owner, CosmeticModelPart parent) {
        super(name, owner, parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        poseStack.scale(-16, 16, -16);

        LivingEntity entity = owner.renderer.entity instanceof LivingEntity living ? living : null;
        int newLight = this.customization.light != null ? this.customization.light : light;
        int newOverlay = this.customization.overlay != null ? this.customization.overlay : overlay;
        int seed = entity != null ? entity.getId() + displayMode.ordinal() : 0;

        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity, item, displayMode, left,
                poseStack, buffer, WorldAPI.getCurrentWorld(),
                newLight, newOverlay, seed
        );
    }

    @Override
    public int getComplexity() {
        return cachedComplexity;
    }

    @Override
    public boolean shouldRender() {
        return super.shouldRender() && item != null && !item.isEmpty();
    }

    // -- lua -- // 


    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "item"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = ItemStackAPI.class,
                            argumentNames = "item"
                    )
            },
            aliases = "item",
            value = "item_task.set_item"
    )
    public ItemTask setItem(Object item) {
        this.item = LuaUtils.parseItemStack("item", item);
        Minecraft client = Minecraft.getInstance();
        RandomSource random = client.level != null ? client.level.random : RandomSource.create();
        cachedComplexity = ((FiguraItemRendererExtension)client.getItemRenderer()).figura$getModelComplexity(this.item, random);
        return this;
    }

    @LuaWhitelist
    public ItemTask item(Object item) {
        return setItem(item);
    }

    @LuaWhitelist
    @LuaMethodDoc("item_task.get_display_mode")
    public String getDisplayMode() {
        return this.displayMode.name();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                        argumentTypes = String.class,
                        argumentNames = "displayMode"
            ),
            aliases = "displayMode",
            value = "item_task.set_display_mode"
    )
    public ItemTask setDisplayMode(@LuaNotNil String mode) {
        try {
            this.displayMode = ItemDisplayContext.valueOf(mode.toUpperCase(Locale.US));
            this.left = this.displayMode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || this.displayMode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            return this;
        } catch (Exception ignored) {
            throw new LuaError("Illegal display mode: \"" + mode + "\".");
        }
    }

    @LuaWhitelist
    public ItemTask displayMode(@LuaNotNil String mode) {
        return setDisplayMode(mode);
    }

    @Override
    public String toString() {
        return name + " (Item Render Task)";
    }
}
