package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.config.CfgCommand;
import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.hud.ContextMenu;
import fun.enoughvisuals.systems.hud.DragAndDrop;
import fun.enoughvisuals.systems.hud.Grid;
import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.visual.MouseParticles;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private boolean enough$dragging = false;

    @Unique
    private String enough$friendSuggestion = null;

    @Unique
    private boolean enough$chatFieldTransformPushed = false;

    @Unique
    private boolean enough$chatFieldBackgroundTransformPushed = false;

    protected ChatScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void enough$chatOpened(CallbackInfo ci) {
        Animations.markChatOpened();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
            ordinal = 0))
    private void enough$pushChatInputBackgroundAnimation(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        enough$chatFieldBackgroundTransformPushed = false;
        Animations animations = Animations.active();
        if (animations == null) return;

        float progress = animations.chatOpenProgress(true);
        float offsetY = (1f - progress) * 18f;
        float alpha = 0.28f + 0.72f * progress;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0f, offsetY, 0f);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        enough$chatFieldBackgroundTransformPushed = true;
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
            ordinal = 0,
            shift = At.Shift.AFTER))
    private void enough$popChatInputBackgroundAnimation(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        if (!enough$chatFieldBackgroundTransformPushed) return;
        ctx.getMatrices().pop();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        enough$chatFieldBackgroundTransformPushed = false;
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void enough$pushChatInputAnimation(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        enough$chatFieldTransformPushed = false;
        Animations animations = Animations.active();
        if (chatField == null || animations == null) return;

        float progress = animations.chatOpenProgress(true);
        float offsetY = (1f - progress) * 18f;
        float alpha = 0.28f + 0.72f * progress;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0f, offsetY, 0f);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        enough$chatFieldTransformPushed = true;
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            shift = At.Shift.AFTER))
    private void enough$popChatInputAnimation(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        if (!enough$chatFieldTransformPushed) return;
        ctx.getMatrices().pop();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        enough$chatFieldTransformPushed = false;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void enough$render(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        DragAndDrop dad = DragAndDrop.getInstance();
        if (!ContextMenu.isVisible()) {
            dad.updateHover(mx, my);
        }
        GlassUtil.BLUR_OFFSET = CustomTheme.hudBlurLevel();
        if (CustomTheme.hudBlurEnabled()) {
            GlassUtil.beginFrame();
        } else {
            GlassUtil.skipFrame();
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        Grid.render(ctx, delta, dad.isDragging());
        dad.renderHoverOverlay(ctx, mx, my, delta);
        ContextMenu.render(ctx, mx, my, delta);
        MouseParticles.renderScreen(ctx, mx, my, delta);
        enough$updateFriendSuggestion();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void enough$mouseClicked(double mouseX, double mouseY, int button,
                                     CallbackInfoReturnable<Boolean> cir) {
        float mx = (float) mouseX;
        float my = (float) mouseY;

        if (ContextMenu.isVisible() && ContextMenu.mouseClicked(mx, my, button)) {
            cir.setReturnValue(true);
            return;
        }

        DragAndDrop dad = DragAndDrop.getInstance();
        dad.updateHover(mx, my);

        if (button == 0 && dad.getHovered() != null && !enough$isOverChatInput(my)) {
            if (dad.startDrag(mx, my)) {
                enough$dragging = true;
                cir.setReturnValue(true);
                return;
            }
        }

        if (button == 1 && !enough$isOverChatInput(my)) {
            if (dad.getHovered() != null) {
                ContextMenu.openSettings(dad.getHovered());
            } else {
                ContextMenu.openAdd(mx, my);
            }
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (ContextMenu.mouseReleased((float) mouseX, (float) mouseY, button)) {
            return true;
        }

        if (button == 0 && enough$dragging) {
            DragAndDrop.getInstance().stopDrag();
            enough$dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (ContextMenu.mouseDragged((float) mouseX, (float) mouseY, button)) {
            return true;
        }

        if (button == 0 && enough$dragging) {
            DragAndDrop.getInstance().drag((float) mouseX, (float) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void enough$mouseScrolled(double mouseX, double mouseY,
                                      double hAmount, double vAmount,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (ContextMenu.mouseScrolled((float) mouseX, (float) mouseY, vAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void enough$friendSuggestionTab(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_TAB && enough$friendSuggestion != null && !enough$friendSuggestion.isEmpty()) {
            chatField.write(enough$friendSuggestion);
            enough$updateFriendSuggestion();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void enough$gpsCommand(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (chatText.trim().startsWith(".gps")) {
            GpsSystem.getInstance().onChatMessage(chatText);
            if (addToHistory) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            }
            ci.cancel();
        }
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void enough$cfgCommand(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (CfgCommand.handle(chatText, addToHistory)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void enough$friendCommand(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (FriendRepo.handleCommand(chatText, addToHistory)) {
            ci.cancel();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void enough$onRemoved(CallbackInfo ci) {
        DragAndDrop.getInstance().resetCursor();
    }

    @Unique
    private void enough$updateFriendSuggestion() {
        if (chatField == null) {
            enough$friendSuggestion = null;
            return;
        }

        enough$friendSuggestion = FriendRepo.getSuggestionSuffix(chatField.getText());
        chatField.setSuggestion(enough$friendSuggestion);
    }

    @Unique
    private boolean enough$isOverChatInput(float mouseY) {
        return mouseY >= this.height - 18;
    }
}
