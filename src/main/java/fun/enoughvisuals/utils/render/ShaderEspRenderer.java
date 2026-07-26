package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.FramebufferAccessor;
import fun.enoughvisuals.mixins.accessors.MinecraftClientAccessor;
import fun.enoughvisuals.mixins.accessors.ShaderProgramAccessor;
import fun.enoughvisuals.systems.modules.modules.visual.Glow;
import fun.enoughvisuals.systems.modules.modules.visual.ShaderESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.List;
import java.util.Map;

public final class ShaderEspRenderer {

    private static final int GL_BLEND_EQUATION = 0x8009;
    private static final ShaderProgramKey BLUR_SHADER = new ShaderProgramKey(
            Identifier.of("enough", "core/shader_esp_blur"),
            VertexFormats.POSITION_COLOR,
            net.minecraft.client.gl.Defines.EMPTY
    );
    private static final ShaderProgramKey ESP_SHADER = new ShaderProgramKey(
            Identifier.of("enough", "core/shader_esp"),
            VertexFormats.POSITION_COLOR,
            net.minecraft.client.gl.Defines.EMPTY
    );

    private static SimpleFramebuffer mask;
    private static SimpleFramebuffer blurHorizontal;
    private static SimpleFramebuffer blurVertical;
    private static SimpleFramebuffer echoTrail;
    private static SimpleFramebuffer echoScratch;
    private static int width;
    private static int height;
    private static boolean frameReady;
    private static boolean captured;
    private static boolean depthCopied;
    private static long lastRenderNanos;
    private static boolean echoDirty;
    private static EspRenderConfig frameConfig;
    private static boolean renderingMask;

    private ShaderEspRenderer() {
    }

    public static void beginWorldFrame(WorldRenderContext context) {
        EspRenderConfig config = activeConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        if (config == null || client.world == null || client.player == null || client.getFramebuffer() == null) {
            frameReady = false;
            captured = false;
            frameConfig = null;
            return;
        }

        if (!prepare(client.getFramebuffer(), config.echoMode())) {
            frameReady = false;
            frameConfig = null;
            return;
        }

        clearFramebuffer(mask, true);
        frameReady = true;
        captured = false;
        depthCopied = false;
        frameConfig = config;
    }

    public static void captureLiving(
            LivingEntityRenderState state,
            MatrixStack matrices,
            Model model,
            List<FeatureRenderer<?, ?>> features,
            Identifier texture
    ) {
        EspRenderConfig config = frameConfig;
        int entityId = state instanceof PlayerEntityRenderState playerState ? playerState.id : -1;
        boolean playerState = state instanceof PlayerEntityRenderState;
        boolean selfState = playerState && MinecraftClient.getInstance().player != null
                && entityId == MinecraftClient.getInstance().player.getId();
        if (renderingMask || config == null || config.manualWorldCapture() || !frameReady || state.invisible
                || !config.shouldRender(entityId, state.squaredDistanceToCamera)
                || !config.acceptsLivingState(playerState, selfState)) {
            return;
        }

        RenderStateSnapshot stateSnapshot = RenderStateSnapshot.capture();
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer originalMain = client.getFramebuffer();
        boolean mainFramebufferSwapped = false;
        try {
            if (!depthCopied) {
                copyDepth(previousDraw, mask);
                depthCopied = true;
            }

            mask.beginWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

            try (BufferAllocator allocator = new BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE)) {
                VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);
                VertexConsumer consumer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
                model.render(
                        matrices,
                        consumer,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE,
                        OverlayTexture.DEFAULT_UV,
                        0xFFFFFFFF
                );
                ((MinecraftClientAccessor) client).enough$setFramebuffer(mask);
                mainFramebufferSwapped = true;
                mask.beginWrite(false);
                consumers.draw();
            }
            captured = true;
        } finally {
            if (mainFramebufferSwapped) {
                ((MinecraftClientAccessor) client).enough$setFramebuffer(originalMain);
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            stateSnapshot.restore();
        }
    }

    public static void renderInWorld(WorldRenderContext context) {
        EspRenderConfig config = frameConfig;
        if (config != null && config.manualWorldCapture()) {
            captureWorldEntities(context, config);
        }
        render();
    }

    public static boolean isRenderingMask() {
        return renderingMask;
    }

    private static void render() {
        EspRenderConfig config = frameConfig;
        MinecraftClient client = MinecraftClient.getInstance();
        if (config == null || !frameReady || !captured || client.getFramebuffer() == null) {
            return;
        }
        if (!prepare(client.getFramebuffer(), config.echoMode())) {
            return;
        }

        RenderStateSnapshot stateSnapshot = RenderStateSnapshot.capture();
        try {
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            renderBlur(config.glowRadius());

            if (config.echoMode()) {
                updateEcho(config);
            } else {
                clearEchoIfDirty();
                lastRenderNanos = 0L;
            }

            client.getFramebuffer().beginWrite(true);
            ShaderProgram shader = RenderSystem.setShader(ESP_SHADER);
            if (shader == null) {
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, mask.getColorAttachment());
            RenderSystem.setShaderTexture(1, blurVertical.getColorAttachment());
            RenderSystem.setShaderTexture(2, config.echoMode() && echoTrail != null ? echoTrail.getColorAttachment() : mask.getColorAttachment());
            setEspUniforms(shader, config, 1, 1.0F);
            drawFullscreenQuad();
        } finally {
            client.getFramebuffer().beginWrite(true);
            stateSnapshot.restore();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            frameReady = false;
            frameConfig = null;
        }
    }

    private static void renderBlur(float radius) {
        ShaderProgram shader = RenderSystem.setShader(BLUR_SHADER);
        if (shader == null) return;

        blurHorizontal.beginWrite(true);
        RenderSystem.setShaderTexture(0, mask.getColorAttachment());
        setUniform(shader, "TexelSize", 1.0F / width, 1.0F / height);
        setUniform(shader, "Direction", 1.0F, 0.0F);
        setUniform(shader, "Radius", radius);
        drawFullscreenQuad();

        blurVertical.beginWrite(true);
        shader = RenderSystem.setShader(BLUR_SHADER);
        if (shader == null) return;
        RenderSystem.setShaderTexture(0, blurHorizontal.getColorAttachment());
        setUniform(shader, "TexelSize", 1.0F / width, 1.0F / height);
        setUniform(shader, "Direction", 0.0F, 1.0F);
        setUniform(shader, "Radius", radius);
        drawFullscreenQuad();
    }

    private static void captureWorldEntities(WorldRenderContext context, EspRenderConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!frameReady || client.world == null || client.player == null || client.getFramebuffer() == null
                || context.matrixStack() == null || context.camera() == null) {
            return;
        }
        if (!prepare(client.getFramebuffer(), config.echoMode())) {
            return;
        }

        RenderStateSnapshot stateSnapshot = RenderStateSnapshot.capture();
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Framebuffer originalMain = client.getFramebuffer();
        boolean mainFramebufferSwapped = false;

        try {
            if (!config.throughWalls()) {
                copyDepth(previousDraw, mask);
                depthCopied = true;
            }

            mask.beginWrite(false);
            if (config.throughWalls()) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            renderingMask = true;
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
            dispatcher.setRenderShadows(false);

            // Entity RenderLayers bind MinecraftClient#getFramebuffer as their output target.
            // Point that target at our mask for the duration of this isolated pass so the
            // queued model buffers cannot silently jump back to the main world framebuffer.
            ((MinecraftClientAccessor) client).enough$setFramebuffer(mask);
            mainFramebufferSwapped = true;
            mask.beginWrite(false);

            try (BufferAllocator allocator = new BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE)) {
                VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);
                float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0F;
                Vec3d cameraPos = context.camera().getPos();
                MatrixStack matrices = context.matrixStack();
                double distance = Math.sqrt(config.distanceSquared());

                if (config.self()
                        && client.options.getPerspective() != Perspective.FIRST_PERSON
                        && isValidManualTarget(client.player, config, context, cameraPos, tickDelta)) {
                    renderEntityMask(client.player, dispatcher, matrices, consumers, cameraPos, tickDelta);
                }

                Box searchBox = client.player.getBoundingBox().expand(distance + 2.0);
                for (Entity entity : client.world.getOtherEntities(client.player, searchBox, config::accepts)) {
                    if (!isValidManualTarget(entity, config, context, cameraPos, tickDelta)) continue;
                    renderEntityMask(entity, dispatcher, matrices, consumers, cameraPos, tickDelta);
                }

                consumers.draw();
            } finally {
                dispatcher.setRenderShadows(true);
                renderingMask = false;
                if (mainFramebufferSwapped) {
                    ((MinecraftClientAccessor) client).enough$setFramebuffer(originalMain);
                    mainFramebufferSwapped = false;
                }
            }
        } finally {
            if (mainFramebufferSwapped) {
                ((MinecraftClientAccessor) client).enough$setFramebuffer(originalMain);
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            stateSnapshot.restore();
        }
    }

    private static void renderEntityMask(Entity entity, EntityRenderDispatcher dispatcher, MatrixStack matrices,
                                         VertexConsumerProvider.Immediate consumers, Vec3d cameraPos, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX()) - cameraPos.x;
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY()) - cameraPos.y;
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()) - cameraPos.z;
        float yaw = entity.getYaw(tickDelta);
        int light = dispatcher.getLight(entity, tickDelta);

        matrices.push();
        dispatcher.render(entity, x, y, z, yaw, matrices, consumers, light);
        matrices.pop();
        captured = true;
    }

    private static boolean isValidManualTarget(Entity entity, EspRenderConfig config, WorldRenderContext context,
                                               Vec3d cameraPos, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || entity == null || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()) {
            return false;
        }
        if (entity == client.player && !config.self()) {
            return false;
        }
        if (!config.accepts(entity)) {
            return false;
        }
        if (entity.isInvisible() && (!(entity instanceof LivingEntity living) || !isWearingFullHighTierArmor(living))) {
            return false;
        }

        Box box = interpolateBox(entity, tickDelta).expand(0.12, 0.12, 0.12);
        if (cameraPos.squaredDistanceTo(box.getCenter()) > config.distanceSquared()) {
            return false;
        }
        if (context.frustum() != null && !context.frustum().isVisible(box)) {
            return false;
        }
        return config.throughWalls() || entity == client.player
                || hasLineOfSight(client.player.getCameraPosVec(tickDelta), box);
    }

    private static Box interpolateBox(Entity entity, float tickDelta) {
        Box base = entity.getBoundingBox();
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        return base.offset(x - entity.getX(), y - entity.getY(), z - entity.getZ());
    }

    private static boolean hasLineOfSight(Vec3d eye, Box box) {
        Vec3d center = box.getCenter();
        if (isPointVisible(eye, center)) return true;
        if (isPointVisible(eye, new Vec3d(center.x, box.maxY - 0.05, center.z))) return true;
        if (isPointVisible(eye, new Vec3d(center.x, box.minY + 0.05, center.z))) return true;
        if (isPointVisible(eye, new Vec3d(box.minX + 0.06, center.y, center.z))) return true;
        if (isPointVisible(eye, new Vec3d(box.maxX - 0.06, center.y, center.z))) return true;
        if (isPointVisible(eye, new Vec3d(center.x, center.y, box.minZ + 0.06))) return true;
        return isPointVisible(eye, new Vec3d(center.x, center.y, box.maxZ - 0.06));
    }

    private static boolean isPointVisible(Vec3d eye, Vec3d point) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return false;
        }

        RaycastContext context = new RaycastContext(
                eye,
                point,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        );
        BlockHitResult hitResult = client.world.raycast(context);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return true;
        }

        double hitDistanceSq = eye.squaredDistanceTo(hitResult.getPos());
        double pointDistanceSq = eye.squaredDistanceTo(point);
        return hitDistanceSq + 1.0E-4 >= pointDistanceSq;
    }

    private static boolean isWearingFullHighTierArmor(LivingEntity entity) {
        int count = 0;
        for (ItemStack stack : entity.getArmorItems()) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            String name = stack.getItem().getTranslationKey().toLowerCase();
            if (!name.contains("diamond") && !name.contains("netherite")) {
                return false;
            }
            count++;
        }
        return count == 4;
    }

    private static void updateEcho(EspRenderConfig config) {
        if (echoTrail == null || echoScratch == null) {
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds = lastRenderNanos == 0L
                ? 1.0F / 60.0F
                : MathHelper.clamp((now - lastRenderNanos) / 1_000_000_000.0F, 1.0F / 240.0F, 0.1F);
        lastRenderNanos = now;
        float decay = (float) Math.pow(config.echoDecay(), deltaSeconds * 60.0F);

        echoScratch.beginWrite(true);
        ShaderProgram shader = RenderSystem.setShader(ESP_SHADER);
        if (shader == null) return;
        RenderSystem.setShaderTexture(0, mask.getColorAttachment());
        RenderSystem.setShaderTexture(1, blurVertical.getColorAttachment());
        RenderSystem.setShaderTexture(2, echoTrail.getColorAttachment());
        setEspUniforms(shader, config, 0, decay);
        drawFullscreenQuad();

        SimpleFramebuffer previous = echoTrail;
        echoTrail = echoScratch;
        echoScratch = previous;
        echoDirty = true;
    }

    private static void setEspUniforms(ShaderProgram shader, EspRenderConfig config, int pass, float echoDecay) {
        int fill = config.fillColor();
        int glow = config.glowColor();
        setUniform(shader, "FillColor",
                ((fill >> 16) & 0xFF) / 255.0F,
                ((fill >> 8) & 0xFF) / 255.0F,
                (fill & 0xFF) / 255.0F,
                1.0F);
        setUniform(shader, "GlowColor",
                ((glow >> 16) & 0xFF) / 255.0F,
                ((glow >> 8) & 0xFF) / 255.0F,
                (glow & 0xFF) / 255.0F,
                1.0F);
        setUniform(shader, "TexelSize", 1.0F / width, 1.0F / height);
        setUniform(shader, "FillOpacity", config.fillOpacity());
        setUniform(shader, "GlowStrength", config.glowStrength());
        setUniform(shader, "OutlineStrength", config.outlineStrength());
        setUniform(shader, "OutlineWidth", config.outlineWidth());
        setUniform(shader, "EchoDecay", echoDecay);
        setUniform(shader, "Mode", config.modeIndex());
        setUniform(shader, "Pass", pass);
    }

    private static boolean prepare(Framebuffer main, boolean needsEcho) {
        if (main.textureWidth <= 0 || main.textureHeight <= 0) return false;
        if (mask != null && width == main.textureWidth && height == main.textureHeight) {
            if (needsEcho) {
                ensureEchoBuffers();
            } else {
                resetEcho();
            }
            return true;
        }

        reset();
        width = main.textureWidth;
        height = main.textureHeight;
        mask = new SimpleFramebuffer(width, height, true);
        blurHorizontal = new SimpleFramebuffer(width, height, false);
        blurVertical = new SimpleFramebuffer(width, height, false);
        mask.setTexFilter(GL11.GL_LINEAR);
        blurHorizontal.setTexFilter(GL11.GL_LINEAR);
        blurVertical.setTexFilter(GL11.GL_LINEAR);
        clearFramebuffer(mask, true);
        if (needsEcho) {
            ensureEchoBuffers();
        }
        return true;
    }

    private static void ensureEchoBuffers() {
        if (echoTrail != null && echoScratch != null) {
            return;
        }

        resetEcho();
        echoTrail = new SimpleFramebuffer(width, height, false);
        echoScratch = new SimpleFramebuffer(width, height, false);
        echoTrail.setTexFilter(GL11.GL_LINEAR);
        echoScratch.setTexFilter(GL11.GL_LINEAR);
        clearFramebuffer(echoTrail, false);
        clearFramebuffer(echoScratch, false);
    }

    private static void clearEchoIfDirty() {
        if (!echoDirty) return;
        clearFramebuffer(echoTrail, false);
        clearFramebuffer(echoScratch, false);
        echoDirty = false;
    }

    private static void copyDepth(int sourceFbo, Framebuffer destination) {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ((FramebufferAccessor) destination).getFbo());
        GL30.glBlitFramebuffer(
                0, 0, width, height,
                0, 0, width, height,
                GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
        );
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
    }

    private static void clearFramebuffer(Framebuffer framebuffer, boolean depth) {
        if (framebuffer == null) return;
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        float[] clearColor = new float[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        framebuffer.beginWrite(false);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | (depth ? GL11.GL_DEPTH_BUFFER_BIT : 0));
        GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    private static void drawFullscreenQuad() {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(-1.0F, -1.0F, 0.0F).color(0xFFFFFFFF);
        builder.vertex(1.0F, -1.0F, 0.0F).color(0xFFFFFFFF);
        builder.vertex(1.0F, 1.0F, 0.0F).color(0xFFFFFFFF);
        builder.vertex(-1.0F, 1.0F, 0.0F).color(0xFFFFFFFF);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static void reset() {
        if (mask != null) mask.delete();
        if (blurHorizontal != null) blurHorizontal.delete();
        if (blurVertical != null) blurVertical.delete();
        mask = null;
        blurHorizontal = null;
        blurVertical = null;
        resetEcho();
        width = 0;
        height = 0;
        frameReady = false;
        captured = false;
        depthCopied = false;
        lastRenderNanos = 0L;
        frameConfig = null;
    }

    private static void resetEcho() {
        if (echoTrail != null) echoTrail.delete();
        if (echoScratch != null) echoScratch.delete();
        echoTrail = null;
        echoScratch = null;
        echoDirty = false;
    }

    private static EspRenderConfig activeConfig() {
        Glow glow = Glow.active();
        if (glow != null) {
            int color = glow.color();
            float strength = glow.strength.get().floatValue();
            boolean fill = glow.isFillMode();
            return new EspRenderConfig(
                    color,
                    color,
                    fill ? ((color >>> 24) & 0xFF) / 255.0F : 0.0F,
                    fill ? strength * 0.92F : strength,
                    fill ? strength * 0.72F : strength * 0.82F,
                    glow.outlineWidth.get().floatValue(),
                    glow.radius.get().floatValue(),
                    0.0F,
                    fill ? 2 : 0,
                    false,
                    glow.self.get(),
                    glow.distanceSquared(),
                    false,
                    false,
                    glow
            );
        }

        ShaderESP shader = ShaderESP.active();
        if (shader == null) {
            return null;
        }

        double distance = shader.distance.get();
        return new EspRenderConfig(
                shader.fillColor.get(),
                shader.glowColor.get(),
                shader.fillOpacity.get().floatValue(),
                shader.glowStrength.get().floatValue(),
                shader.outlineStrength.get().floatValue(),
                shader.outlineWidth.get().floatValue(),
                shader.glowRadius.get().floatValue(),
                shader.echoDecay.get().floatValue(),
                shader.modeIndex(),
                shader.mode.is("Echo"),
                shader.self.get(),
                distance * distance,
                false,
                false,
                null
        );
    }

    private record EspRenderConfig(
            int fillColor,
            int glowColor,
            float fillOpacity,
            float glowStrength,
            float outlineStrength,
            float outlineWidth,
            float glowRadius,
            float echoDecay,
            int modeIndex,
            boolean echoMode,
            boolean self,
            double distanceSquared,
            boolean manualWorldCapture,
            boolean throughWalls,
            Glow glow
    ) {
        private boolean shouldRender(int entityId, double squaredDistance) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return false;
            if (!self && entityId == client.player.getId()) return false;
            return squaredDistance <= distanceSquared;
        }

        private boolean accepts(Entity entity) {
            return glow == null ? entity instanceof LivingEntity : glow.shouldRender(entity);
        }

        private boolean acceptsLivingState(boolean player, boolean selfState) {
            if (glow == null) return !selfState || self;
            if (player) {
                return glow.entities.isSelected(Glow.ENTITY_PLAYERS) && (!selfState || glow.self.get());
            }
            return glow.entities.isSelected(Glow.ENTITY_MOBS);
        }
    }

    private static void setUniform(ShaderProgram shader, String name, float... values) {
        GlUniform uniform = getUniform(shader, name);
        if (uniform == null) return;
        switch (values.length) {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
        }
    }

    private static void setUniform(ShaderProgram shader, String name, int value) {
        GlUniform uniform = getUniform(shader, name);
        if (uniform != null) uniform.set(value);
    }

    private static GlUniform getUniform(ShaderProgram shader, String name) {
        try {
            Map<String, GlUniform> uniforms = ((ShaderProgramAccessor) shader).getUniformsByName();
            return uniforms == null ? null : uniforms.get(name);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record RenderStateSnapshot(
            boolean blend,
            boolean depthTest,
            boolean cull,
            boolean depthMask,
            int depthFunc,
            int blendSrcRgb,
            int blendDstRgb,
            int blendSrcAlpha,
            int blendDstAlpha,
            int blendEquation,
            ShaderProgram shader,
            int texture0,
            int texture1,
            int texture2,
            float[] shaderColor
    ) {
        private static RenderStateSnapshot capture() {
            return new RenderStateSnapshot(
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL_BLEND_EQUATION),
                    RenderSystem.getShader(),
                    RenderSystem.getShaderTexture(0),
                    RenderSystem.getShaderTexture(1),
                    RenderSystem.getShaderTexture(2),
                    RenderSystem.getShaderColor().clone()
            );
        }

        private void restore() {
            if (blend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (cull) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }

            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunc);
            RenderSystem.blendEquation(blendEquation);
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            if (shader != null) {
                RenderSystem.setShader(shader);
            }
            RenderSystem.setShaderTexture(0, texture0);
            RenderSystem.setShaderTexture(1, texture1);
            RenderSystem.setShaderTexture(2, texture2);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
        }
    }
}
