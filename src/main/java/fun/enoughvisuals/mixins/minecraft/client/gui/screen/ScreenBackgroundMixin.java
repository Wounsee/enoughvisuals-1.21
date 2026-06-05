package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.modules.visual.CustomTheme;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void enough$renderPauseBlur(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!((Object) this instanceof GameMenuScreen) || mc.world == null) {
            return;
        }

        StencilUtil.resetRenderState();
        RenderSystem.disableDepthTest();
        RenderSystem.defaultBlendFunc();
        GlassUtil.BLUR_OFFSET = CustomTheme.clickGuiBlurLevel();
        if (CustomTheme.clickGuiBlurEnabled()) {
            GlassUtil.beginFrame();
        } else {
            GlassUtil.skipFrame();
        }
        GlassUtil.drawGlassPanel(
                context,
                0.0f,
                0.0f,
                mc.getWindow().getScaledWidth(),
                mc.getWindow().getScaledHeight(),
                0.0f,
                0x000000,
                0.18f * CustomTheme.clickGuiOpacity(),
                1.0f
        );
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        ci.cancel();
    }
}
