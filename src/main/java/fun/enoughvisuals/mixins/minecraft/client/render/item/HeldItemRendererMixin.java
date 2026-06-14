package fun.enoughvisuals.mixins.minecraft.client.render.item;

import fun.enoughvisuals.mixins.accessors.PlayerEntityRendererAccessor;
import fun.enoughvisuals.systems.modules.modules.visual.SwingAnimation;
import fun.enoughvisuals.systems.modules.modules.visual.BetterHands;
import fun.enoughvisuals.systems.modules.modules.visual.ViewModel;
import fun.enoughvisuals.utils.render.ShaderHandsRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Unique
    private boolean enough$viewModelPushed = false;
    @Unique
    private static final double enough$HMI_MAX_DELTA = 0.05D;
    @Unique
    private static final double enough$HMI_ANIMATION_SPEED = 18.0D;
    @Unique
    private static final float enough$HMI_DEFAULT_SIZE = 0.65F;
    @Unique
    private static final float enough$HMI_DEFAULT_HAND_Y = 0.4F;
    @Unique
    private static final float enough$HMI_DEFAULT_HAND_Z = 0.25F;
    @Unique
    private double enough$hmiPrevFrameTime = System.nanoTime() / 1.0E9D;
    @Unique
    private double enough$hmiDeltaTime;
    @Unique
    private double enough$hmiPreviousSwimRotation;
    @Unique
    private float enough$hmiSwingAngleY;
    @Unique
    private float enough$hmiSwingAngleX;
    @Unique
    private float enough$hmiSwingVelocityY;
    @Unique
    private float enough$hmiSwingVelocityX;
    @Unique
    private float enough$hmiSwingVelocityZ;
    @Unique
    private float enough$hmiVertAngleY;
    @Unique
    private float enough$hmiVertVelocityY;
    @Unique
    private float enough$hmiVertAngleYSlime;
    @Unique
    private float enough$hmiClimbBlend;
    @Unique
    private float enough$hmiCrawlCount;
    @Unique
    private float enough$hmiDirectionalCrawlCount;
    @Unique
    private float enough$hmiClimbCount;
    @Unique
    private float enough$hmiWaterCounter;
    @Unique
    private boolean enough$hmiPhysicsUpdatedThisFrame;

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack item, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void enough$onRenderHandsHead(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ShaderHandsRenderer.captureBeforeHands();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("RETURN"))
    private void enough$onRenderHandsReturn(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ShaderHandsRenderer.captureAfterHands();
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void enough$onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        boolean isMainHand = hand == Hand.MAIN_HAND;
        Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();

        if (enough$shouldUseBetterHands(player, hand, item)) {
            ci.cancel();
            enough$updateHmiDeltaTime();
            if (enough$shouldUseBetterHandsBow(player, hand, item)) {
                enough$renderBetterHandsBow(player, tickDelta, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light, arm);
            } else if (enough$shouldUseBetterHandsConsume(player, hand, item)) {
                enough$renderBetterHandsConsume(player, tickDelta, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light, arm);
            } else {
                enough$renderBetterHands(player, tickDelta, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light, arm);
            }
            return;
        }

        SwingAnimation swing = SwingAnimation.INSTANCE;
        boolean useCustomSwing = swing != null
                && swing.isEnabled()
                && swing.shouldApplyAnimation(item)
                && arm == Arm.RIGHT;

        ViewModel viewModel = ViewModel.INSTANCE;
        boolean useViewModel = viewModel != null && viewModel.isEnabled();

        if (useCustomSwing) {
            ci.cancel();

            matrices.push();
            try {
                if (useViewModel) {
                    viewModel.applyTransform(matrices, arm);
                }

                SwingAnimation.SwingTransformations t = swing.getTransform(swingProgress);

                matrices.translate(t.anchorX(), t.anchorY(), t.anchorZ());
                matrices.translate(t.moveX(), t.moveY(), t.moveZ());
                matrices.multiply(new Quaternionf().rotationXYZ(
                        (float) Math.toRadians(t.rotateX()),
                        (float) Math.toRadians(t.rotateY()),
                        (float) Math.toRadians(t.rotateZ())
                ));
                matrices.translate(-t.anchorX(), -t.anchorY(), -t.anchorZ());

                int side = arm == Arm.RIGHT ? 1 : -1;
                matrices.translate(side * 0.56f, -0.52f, -0.72f);

                if (!item.isEmpty()) {
                    boolean isRightArm = arm == Arm.RIGHT;
                    this.renderItem(
                            player,
                            item,
                            isRightArm ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                            !isRightArm,
                            matrices,
                            vertexConsumers,
                            light
                    );
                }
            } finally {
                matrices.pop();
            }
            return;
        }

        if (useViewModel) {
            matrices.push();
            viewModel.applyTransform(matrices, arm);
            enough$viewModelPushed = true;
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void enough$onRenderFirstPersonItemReturn(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (enough$viewModelPushed) {
            enough$viewModelPushed = false;
            matrices.pop();
        }
    }

    @Unique
    private boolean enough$shouldUseBetterHands(AbstractClientPlayerEntity player, Hand hand, ItemStack stack) {
        BetterHands betterHands = BetterHands.INSTANCE;
        if (betterHands == null || !betterHands.isEnabled()) return false;
        if (stack.getItem() == Items.FILLED_MAP || stack.getItem() instanceof CrossbowItem) return false;
        return true;
    }

    @Unique
    private boolean enough$shouldUseBetterHandsBow(AbstractClientPlayerEntity player, Hand hand, ItemStack stack) {
        return enough$useAction(stack) == UseAction.BOW && player.isUsingItem() && player.getActiveHand() == hand;
    }

    @Unique
    private boolean enough$shouldUseBetterHandsConsume(AbstractClientPlayerEntity player, Hand hand, ItemStack stack) {
        ItemStack active = !player.getActiveItem().isEmpty() && player.getActiveHand() == hand ? player.getActiveItem() : stack;
        UseAction action = enough$useAction(active);
        return (action == UseAction.EAT || action == UseAction.DRINK) && player.isUsingItem() && player.getActiveHand() == hand;
    }

    @Unique
    private UseAction enough$useAction(ItemStack stack) {
        return stack.isEmpty() ? UseAction.NONE : stack.getItem().getUseAction(stack);
    }

    @Unique
    private void enough$updateHmiDeltaTime() {
        double currentTime = System.nanoTime() / 1.0E9D;
        enough$hmiDeltaTime = Math.min(enough$HMI_MAX_DELTA, Math.max(0.0D, currentTime - enough$hmiPrevFrameTime));
        enough$hmiPrevFrameTime = currentTime;
        enough$hmiPhysicsUpdatedThisFrame = false;
    }

    @Unique
    private void enough$renderBetterHands(AbstractClientPlayerEntity player, float tickDelta, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm arm) {
        boolean rightHand = arm == Arm.RIGHT;
        matrices.push();
        if (stack.isEmpty()) {
            matrices.pop();
            return;
        }

        enough$applyHmiSwing(matrices, player, hand, stack, swingProgress);
        enough$applyHmiEnvironment(matrices, player, hand, arm, stack, swingProgress, tickDelta);
        enough$applyHmiArmPrePose(matrices, stack, arm);
        enough$applyHmiBaseHandPose(matrices, arm, equipProgress, swingProgress);
        enough$renderArmFirstPerson(player, matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
        enough$applyHmiItemPose(matrices, player, hand, arm, stack, swingProgress);
        renderItem(player, stack, rightHand ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !rightHand, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Unique
    private void enough$renderBetterHandsBow(AbstractClientPlayerEntity player, float tickDelta, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm arm) {
        boolean rightHand = arm == Arm.RIGHT;
        int direction = rightHand ? 1 : -1;
        float useTicks = player.getItemUseTime() + tickDelta;
        float draw = MathHelper.clamp(useTicks / 20.0F, 0.0F, 1.0F);

        matrices.push();
        enough$applyHmiEnvironment(matrices, player, hand, arm, stack, swingProgress, tickDelta);

        matrices.push();
        enough$applyHmiUseJitter(matrices, useTicks, draw);
        matrices.translate(rightHand ? -0.1D : 0.1D, 0.0D, draw * 0.15D);
        enough$renderArmFirstPerson(player, matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
        matrices.pop();

        matrices.push();
        matrices.translate(rightHand ? -0.5D : 0.5D, -0.45D, 0.1D);
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3F));
        matrices.multiply((rightHand ? RotationAxis.NEGATIVE_Z : RotationAxis.POSITIVE_Z).rotation(-0.3F));
        matrices.multiply((rightHand ? RotationAxis.NEGATIVE_Y : RotationAxis.POSITIVE_Y).rotation(1.0F));
        enough$renderArmFirstPerson(player, matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
        matrices.pop();

        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0F));
        matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(-15.0F * direction));
        matrices.translate(0.8D * direction, -equipProgress * 0.3D, -0.1D);
        enough$applyHmiUseJitter(matrices, useTicks, draw);
        enough$applyHmiItemPose(matrices, player, hand, arm, stack, swingProgress);
        renderItem(player, stack, rightHand ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !rightHand, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Unique
    private void enough$renderBetterHandsConsume(AbstractClientPlayerEntity player, float tickDelta, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm arm) {
        ItemStack consumeStack = !player.getActiveItem().isEmpty() && player.getActiveHand() == hand ? player.getActiveItem() : stack;
        int direction = arm == Arm.RIGHT ? 1 : -1;
        float useTicks = player.getItemUseTime() + tickDelta;
        float progress = MathHelper.clamp(useTicks / 5.0F, 0.0F, 1.0F);
        float wobble = MathHelper.sin(useTicks / 2.0F * (float) Math.PI) * 0.1F;

        matrices.push();
        matrices.translate(direction, 0.1D, 0.3D);
        matrices.translate(0.2D * direction * progress, -0.7D * progress, -0.2D * progress);
        matrices.translate(0.0D, -0.2D * wobble, -0.2D * wobble);
        matrices.translate(0.0D, 0.1D * enough$hmiEase(MathHelper.sin(progress * (float) Math.PI)), 0.0D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * direction));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * direction));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
        matrices.scale(0.9F, 0.9F, 0.9F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * progress * direction));
        enough$renderArmFirstPerson(player, matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
        enough$applyHmiItemPose(matrices, player, hand, arm, consumeStack, swingProgress);
        renderItem(player, consumeStack, arm == Arm.RIGHT ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, arm != Arm.RIGHT, matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Unique
    private void enough$renderArmFirstPerson(AbstractClientPlayerEntity player, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm) {
        boolean rightHand = arm == Arm.RIGHT;
        float direction = rightHand ? 1.0F : -1.0F;
        float swingRoot = MathHelper.sqrt(swingProgress);
        float xSwing = -0.3F * MathHelper.sin(swingRoot * (float) Math.PI);
        float ySwing = 0.4F * MathHelper.sin(swingRoot * ((float) Math.PI * 2.0F));
        float zSwing = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);
        matrices.translate(direction * (xSwing + 0.64000005F), ySwing - 0.6F + equipProgress * -0.6F, zSwing - 0.71999997F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 45.0F));
        float swingSquared = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float swingSqrt = MathHelper.sin(swingRoot * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * swingSqrt * 70.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * swingSquared * -20.0F));
        matrices.translate(direction * -1.0F, 3.6F, 3.5D);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * 120.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -135.0F));
        matrices.translate(direction * 5.6F, 0.0D, 0.0D);

        matrices.translate(0.0F, enough$HMI_DEFAULT_HAND_Y, enough$HMI_DEFAULT_HAND_Z);

        enough$renderPlayerArm(player, matrices, vertexConsumers, light, arm);
    }

    @Unique
    private void enough$renderPlayerArm(AbstractClientPlayerEntity player, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm arm) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerEntityRenderer renderer)) return;

        boolean slim = String.valueOf(player.getSkinTextures().model()).toLowerCase().contains("slim");
        if (arm == Arm.RIGHT) {
            ((PlayerEntityRendererAccessor) renderer).enoughvisuals$renderRightArm(matrices, vertexConsumers, light, player.getSkinTextures().texture(), slim);
        } else {
            ((PlayerEntityRendererAccessor) renderer).enoughvisuals$renderLeftArm(matrices, vertexConsumers, light, player.getSkinTextures().texture(), slim);
        }
    }

    @Unique
    private void enough$applyHmiBaseHandPose(MatrixStack matrices, Arm arm, float equipProgress, float swingProgress) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        BetterHands better = BetterHands.INSTANCE;

        float spread = better != null ? better.spread.get().floatValue() : 0.65F;
        float scale = 0.9F * enough$HMI_DEFAULT_SIZE;
        matrices.translate(direction * spread, 0.0D, 0.3D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * direction));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * direction));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
        matrices.scale(scale, scale, scale);
    }

    @Unique
    private void enough$applyHmiArmPrePose(MatrixStack matrices, ItemStack stack, Arm arm) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        if (enough$isLantern(stack)) {
            matrices.translate(0.1D * direction, 0.0D, -0.1D);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
            return;
        }

        if (enough$useAction(stack) == UseAction.BLOCK) {
            matrices.translate(0.0D, -0.2D, 0.0D);
        }
    }

    @Unique
    private void enough$applyHmiEnvironment(MatrixStack matrices, AbstractClientPlayerEntity player, Hand hand, Arm arm, ItemStack stack, float swingProgress, float tickDelta) {
        float yaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevYaw, player.getYaw());
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        Vec3d motion = player.getVelocity();
        double dot = motion.x * forwardX + motion.z * forwardZ;
        double cross = motion.x * forwardZ - motion.z * forwardX;
        boolean climbing = player.isClimbing() && !player.isOnGround() && Math.abs(motion.y) > 0.0D;
        boolean crawling = player.isCrawling();
        double tt = enough$hmiDeltaTime * enough$getHmiAnimationSpeed();
        float handDirection = hand == Hand.MAIN_HAND ? 1.0F : -1.0F;
        int armDirection = arm == Arm.RIGHT ? 1 : -1;

        if (!enough$hmiPhysicsUpdatedThisFrame) {
            if (motion.length() >= 0.08D) {
                double speed = Math.min(motion.length(), 0.22D);
                double clampedDot = MathHelper.clamp(dot, -0.22D, 0.22D);
                double clampedCross = MathHelper.clamp(cross, -0.22D, 0.22D);
                enough$hmiCrawlCount += (float) (0.1D * speed * 2.0D * tt);
                enough$hmiDirectionalCrawlCount += (float) (0.1D * clampedDot * 4.0D * tt);
                enough$hmiDirectionalCrawlCount += (float) (0.1D * Math.abs(clampedCross) * (clampedDot > 0.0D ? 4.0D : -4.0D) * tt);
            }

            enough$hmiClimbCount += (float) ((motion.y > 0.0D ? 0.1D : motion.y < 0.0D ? -0.1D : 0.0D) * tt);
            float motionY = player.isOnGround() ? 0.0F : (float) MathHelper.clamp(motion.y, -0.42D, 0.42D);
            enough$hmiVertAngleY += (float) (motionY * 0.015D * tt);
            enough$hmiVertAngleY -= (float) (0.1D * enough$hmiVertAngleY * tt);
            enough$hmiVertAngleY *= (float) Math.pow(0.88D, tt);
            enough$hmiVertVelocityY += (float) (motionY * 0.015D * tt);
            enough$hmiVertVelocityY -= (float) (0.1D * enough$hmiVertAngleYSlime * tt);
            enough$hmiVertVelocityY *= (float) Math.pow(0.88D, tt);
            enough$hmiVertAngleYSlime += (float) (enough$hmiVertVelocityY * tt);
            enough$hmiWaterCounter = player.isTouchingWater() && !player.isSwimming() ? Math.min(1.0F, enough$hmiWaterCounter + (float) (0.1D * tt)) : (float) (enough$hmiWaterCounter * Math.pow(0.88D, tt));
            enough$hmiPhysicsUpdatedThisFrame = true;
        }

        if ((crawling || climbing) && !player.isUsingItem() && swingProgress == 0.0F) {
            enough$hmiClimbBlend = Math.min(1.0F, enough$hmiClimbBlend + (float) (0.1D * tt));
            if (!enough$isLantern(stack)) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * enough$hmiClimbBlend));
        } else {
            enough$hmiClimbBlend *= (float) Math.pow(0.88D, tt);
        }

        if (swingProgress == 0.0F) {
            float pitch = player.getPitch();
            matrices.translate(handDirection > 0.0F ? pitch / -650.0F * enough$hmiClimbBlend : pitch / 650.0F * enough$hmiClimbBlend, 0.0F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * enough$hmiClimbBlend));
        }

        matrices.translate(0.0D, 0.02D * enough$hmiWaterCounter - enough$hmiVertAngleY, 0.0D);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * handDirection * enough$hmiWaterCounter));
        matrices.translate(0.0D, Math.sin((player.age + tickDelta) * 0.1D) * 0.007D * armDirection, 0.0D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin((player.age + tickDelta) * 0.15F) * armDirection));

        if (!stack.isEmpty() || crawling || climbing || player.isSwimming()) {
            if (enough$useAction(stack) != UseAction.BLOCK) matrices.translate(0.0D, -0.1D, 0.1D);
        }

        if (player.isSwimming() && swingProgress == 0.0F) {
            double swim = Math.sin((player.age + tickDelta) * 0.2D) * 1.5D;
            double smooth = swim * 0.8D + enough$hmiPreviousSwimRotation * 0.2D;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (hand == Hand.MAIN_HAND ? smooth : -smooth)));
            matrices.translate(0.0D, 0.0D, smooth * 0.2D);
            enough$hmiPreviousSwimRotation = smooth;
        }
    }

    @Unique
    private void enough$applyHmiItemPose(MatrixStack matrices, AbstractClientPlayerEntity player, Hand hand, Arm arm, ItemStack stack, float swingProgress) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        boolean mainHand = hand == Hand.MAIN_HAND;
        if (player.getMainArm() == Arm.LEFT) mainHand = !mainHand;

        matrices.translate(-0.3D * direction, 0.65D, -0.1D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-65.0F * direction));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));

        if (stack.getItem() instanceof BlockItem && !(stack.getItem() instanceof BucketItem) && enough$useAction(stack) != UseAction.EAT) {
            if (enough$isTorch(stack)) {
                matrices.scale(1.5F, 1.5F, 1.5F);
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25.0F * direction));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction));
                matrices.translate(0.2D * direction, 0.2D, 0.05D);
                return;
            }
            if (enough$isLantern(stack)) {
                enough$applyHmiLanternPose(matrices, player, arm, swingProgress);
                return;
            }
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25.0F * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction));
            matrices.translate(0.2D * direction, 0.2D, 0.05D);
            return;
        }

        if (enough$isSmallItem(stack)) {
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5.0F * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction));
            matrices.translate(0.0D, -0.05D, -0.1D);
            matrices.scale(0.7F, 0.7F, 0.7F);
            return;
        }

        if (enough$useAction(stack) == UseAction.BLOCK) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(160.0F * direction));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60.0F * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
            matrices.scale(0.75F, 0.75F, 0.75F);
            matrices.translate(0.15D * direction, mainHand ? 0.35D : 0.45D, mainHand ? -0.15D : -0.1D);
            matrices.translate(0.17D * direction, 0.0D, 0.3D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F * direction));
            return;
        }

        if (enough$useAction(stack) == UseAction.SPEAR) {
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75.0F * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45.0F * direction));
            matrices.translate(-0.3D * direction, 0.0D, 0.0D);
            return;
        }

        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75.0F * direction));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45.0F * direction));
        matrices.scale(1.2F, 1.2F, 1.2F);
    }

    @Unique
    private void enough$applyHmiSwing(MatrixStack matrices, AbstractClientPlayerEntity player, Hand hand, ItemStack stack, float swingProgress) {
        boolean mainHand = hand == Hand.MAIN_HAND;
        if (player.getMainArm() == Arm.LEFT) mainHand = !mainHand;
        float direction = mainHand ? 1.0F : -1.0F;
        float handDirection = hand == Hand.MAIN_HAND ? 1.0F : -1.0F;
        float adjustedSwingProgress = enough$adjustHmiSwingProgress(swingProgress);
        float animationDistance = enough$getHmiAnimationDistance();
        float swingRot = enough$getHmiSwingRot(adjustedSwingProgress) * animationDistance;
        float swing = enough$hmiEase(MathHelper.sin(adjustedSwingProgress * (float) Math.PI)) * animationDistance;
        BetterHands better = BetterHands.INSTANCE;

        if (stack.getItem() instanceof SwordItem && better != null && better.useForwardAttack()) {
            matrices.translate(0.08D * direction * swingRot, 0.03D * swingRot, -0.62D * swing);
            matrices.translate(0.01D * direction * swing, 0.06D * swing, -0.06D * swingRot);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(6.0F * swingRot * direction));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-9.0F * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-12.0F * swingRot * direction));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(22.0F * swing));
            return;
        }

        if (stack.getItem() instanceof SwordItem && better != null && better.useNormalAttack()) {
            enough$applyHmiGenericSwing(matrices, direction, swingRot, swing);
            return;
        }

        if ((stack.getItem() instanceof AxeItem || enough$useAction(stack) == UseAction.SPEAR || enough$useAction(stack) == UseAction.BLOCK) && !(stack.getItem() instanceof ShovelItem)) {
            if (enough$isWeapon(stack)) {
                matrices.translate(0.48D * direction * swingRot, 0.18D * swingRot, -0.34D * swing);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swingRot * direction));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-13.0F * swingRot));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-42.0F * swingRot * direction));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees((stack.getItem() instanceof SwordItem ? 26.0F : 20.0F) * swing));
                return;
            }

            if (enough$useAction(stack) == UseAction.SPEAR) {
                matrices.translate(0.0D, 0.0D, 0.3D * swingRot);
                matrices.translate(-0.16D * handDirection * swing, -0.22D * swingRot, -0.38D * swing);
                matrices.translate(0.0D, 0.06D * swing, 0.0D);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swingRot * direction));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(20.0F * swingRot * direction));
                return;
            }
        }

        if (stack.getItem() instanceof SwordItem) {
            matrices.translate(-0.34D * direction * swingRot, 0.0D, -0.5D * swing);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(4.0F * swingRot * direction));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-18.0F * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(42.0F * swingRot * direction));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(32.0F * swing));
            return;
        }

        if (enough$isTool(stack)) {
            matrices.translate(0.07D * direction * swingRot, 0.06D * swingRot, -0.32D * swing);
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-18.0F * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-13.0F * swingRot * direction));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(26.0F * swing));
            return;
        }

        enough$applyHmiGenericSwing(matrices, direction, swingRot, swing);
    }

    @Unique
    private void enough$applyHmiGenericSwing(MatrixStack matrices, float direction, float swingRot, float swing) {
        matrices.translate(0.07D * direction * swingRot, 0.06D * swingRot, -0.08D * swing);
        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-18.0F * swingRot));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-7.0F * swingRot * direction));
        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(24.0F * swing));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(7.0F * swing * direction));
    }

    @Unique
    private void enough$applyHmiLanternPose(MatrixStack matrices, AbstractClientPlayerEntity player, Arm arm, float swingProgress) {
        float dt = (float) (enough$hmiDeltaTime * enough$getHmiAnimationSpeed());
        int direction = arm == Arm.RIGHT ? 1 : -1;
        float yawDelta = player.prevYaw - player.getYaw();
        float pitchDelta = player.prevPitch - player.getPitch();
        enough$hmiSwingVelocityY += yawDelta * 0.015F * dt + swingProgress * 2.0F * dt;
        enough$hmiSwingVelocityX += pitchDelta * 0.015F * dt;
        enough$hmiSwingVelocityY -= 0.1F * enough$hmiSwingAngleY * dt;
        enough$hmiSwingVelocityX -= 0.1F * enough$hmiSwingAngleX * dt;
        enough$hmiSwingVelocityY *= (float) Math.pow(0.88D, dt);
        enough$hmiSwingVelocityX *= (float) Math.pow(0.88D, dt);
        enough$hmiSwingAngleY += enough$hmiSwingVelocityY * dt;
        enough$hmiSwingAngleX += enough$hmiSwingVelocityX * dt;
        double speed = player.getVelocity().length();
        enough$hmiSwingVelocityZ += (float) ((direction > 0 ? speed * -15.0D : speed * 15.0D) - enough$hmiSwingVelocityZ) * 0.1F * dt;

        matrices.translate(0.0D, 0.0D, -0.1D);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(35.0F * direction + enough$hmiSwingAngleY));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + enough$hmiSwingAngleX));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction + enough$hmiSwingVelocityZ));
        matrices.translate(0.3D * direction, -0.35D, 0.0D);
        matrices.translate(0.0D, 0.0D, 0.1D);
        matrices.scale(1.5F, 1.5F, 1.5F);
    }

    @Unique
    private void enough$applyHmiUseJitter(MatrixStack matrices, float useTicks, float progress) {
        if (progress <= 0.1F) return;
        float pulse = MathHelper.sin((useTicks - 0.1F) * 1.3F);
        matrices.translate(0.0D, pulse * (progress - 0.1F) * 0.004D, 0.0D);
    }

    @Unique
    private float enough$getHmiSwingRot(float swingProgress) {
        float clamped = MathHelper.clamp(swingProgress, 0.0F, 1.0F);
        float arc = MathHelper.sin(clamped * (float) Math.PI);
        float fade = 1.0F - 0.35F * clamped * clamped;
        return arc * fade;
    }

    @Unique
    private float enough$adjustHmiSwingProgress(float swingProgress) {
        float speed = (float) MathHelper.clamp(enough$getHmiAnimationSpeedMultiplier(), 0.25D, 2.5D);
        float clamped = MathHelper.clamp(swingProgress, 0.0F, 1.0F);
        if (Math.abs(speed - 1.0F) < 0.001F) {
            return clamped;
        }
        if (speed > 1.0F) {
            return 1.0F - (float) Math.pow(1.0F - clamped, speed);
        }
        return (float) Math.pow(clamped, 1.0F / speed);
    }

    @Unique
    private double enough$getHmiAnimationSpeed() {
        return enough$HMI_ANIMATION_SPEED * enough$getHmiAnimationSpeedMultiplier();
    }

    @Unique
    private double enough$getHmiAnimationSpeedMultiplier() {
        BetterHands better = BetterHands.INSTANCE;
        return better != null ? MathHelper.clamp(better.animationSpeed(), 0.25F, 2.5F) : 1.0D;
    }

    @Unique
    private float enough$getHmiAnimationDistance() {
        BetterHands better = BetterHands.INSTANCE;
        return better != null ? (float) MathHelper.clamp(better.animationDistance(), 0.15F, 2.0F) : 1.0F;
    }

    @Unique
    private float enough$hmiEase(float value) {
        value = MathHelper.clamp(value, 0.0F, 1.0F);
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }

    @Unique
    private boolean enough$isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    @Unique
    private boolean enough$isTool(ItemStack stack) {
        return stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof PickaxeItem
                || stack.getItem() instanceof ShovelItem
                || stack.getItem() instanceof HoeItem
                || stack.getItem() instanceof TridentItem;
    }

    @Unique
    private boolean enough$isLantern(ItemStack stack) {
        return stack.getItem() == Items.LANTERN || stack.getItem() == Items.SOUL_LANTERN;
    }

    @Unique
    private boolean enough$isTorch(ItemStack stack) {
        return stack.getItem() == Items.TORCH || stack.getItem() == Items.SOUL_TORCH || stack.getName().getString().toLowerCase().contains("torch");
    }

    @Unique
    private boolean enough$isSmallItem(ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem)
                && !enough$isTool(stack)
                && !enough$isWeapon(stack)
                && !(stack.getItem() instanceof FishingRodItem)
                && !(stack.getItem() instanceof BucketItem)
                && enough$useAction(stack) != UseAction.BOW
                && enough$useAction(stack) != UseAction.SPEAR
                && enough$useAction(stack) != UseAction.BLOCK;
    }
}
