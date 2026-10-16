package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11;

/**
 * Glow and smoke passes of the Shader Player module, ported from RivalVisuals.
 *
 * <p>Input is the entity mask captured by {@link ShaderEspRenderer} (color and
 * depth of the target models only). Glow is a three-level dual-Kawase blur of
 * that mask composited around the silhouette; smoke is a half-resolution
 * feedback trail that drifts upward from the silhouette edge.
 */
public final class ShaderPlayerRenderer {
    private static final String ROOT = "/assets/enough/shaders/shader_player/";
    private static final int LEVELS = 3;

    private static RawShaderProgram blurDown;
    private static RawShaderProgram blurUp;
    private static RawShaderProgram composite;
    private static RawShaderProgram smoke;
    private static boolean broken;

    private static SimpleFramebuffer sceneDepth;
    private static final SimpleFramebuffer[] levels = new SimpleFramebuffer[LEVELS];
    private static SimpleFramebuffer smokeTrail;
    private static SimpleFramebuffer smokeScratch;
    private static boolean smokeFresh = true;

    private ShaderPlayerRenderer() {
    }

    public record Settings(boolean glow, boolean smoke, boolean thinOutline, float glowStrength,
                           float intensity, float speed, int primary, int secondary, boolean gradient,
                           float alpha) {
    }

    /** Draws the effects onto {@code target}; the caller restores GL state afterwards. */
    public static void render(SimpleFramebuffer mask, Framebuffer target, Settings settings) {
        if (broken || mask == null || target == null) return;
        try {
            ensurePrograms();
        } catch (RuntimeException error) {
            broken = true;
            org.slf4j.LoggerFactory.getLogger("EnoughVisuals/ShaderPlayer").error("Shader Player disabled", error);
            return;
        }

        float time = (System.currentTimeMillis() % 120_000L) / 1000.0F;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (settings.smoke()) {
            renderSmoke(mask, target, settings, time * settings.speed());
        } else {
            smokeFresh = true;
        }

        if (settings.glow() || settings.thinOutline()) {
            ensureSceneDepth(target);
            sceneDepth.copyDepthFrom(target);
            SimpleFramebuffer glow = settings.glow() ? blur(mask, settings.glowStrength()) : mask;
            composite(mask, glow, target, settings, time);
        }
        RenderSystem.activeTexture(GL13_TEXTURE0);
    }

    private static final int GL13_TEXTURE0 = 0x84C0;

    private static SimpleFramebuffer blur(SimpleFramebuffer mask, float strength) {
        ensureLevels(mask.textureWidth, mask.textureHeight);
        RenderSystem.disableBlend();

        int source = mask.getColorAttachment();
        int width = mask.textureWidth;
        int height = mask.textureHeight;
        blurDown.bind();
        blurDown.set("CurrentInput", 0);
        blurDown.set("DepthTexture", 1);
        for (int i = 0; i < LEVELS; i++) {
            levels[i].beginWrite(true);
            bindTexture(0, source, true);
            bindTexture(1, mask.getDepthAttachment(), false);
            blurDown.set("HalfPixel", 0.5F / width, 0.5F / height);
            blurDown.set("FirstPass", i == 0 ? 1 : 0);
            blurDown.drawFullscreen();
            source = levels[i].getColorAttachment();
            width = levels[i].textureWidth;
            height = levels[i].textureHeight;
        }

        float offset = Math.min(Math.max(0.5F, strength / 4.0F), 4.0F);
        blurUp.bind();
        blurUp.set("CurrentInput", 0);
        blurUp.set("Offset", offset);
        for (int i = LEVELS - 1; i > 0; i--) {
            levels[i - 1].beginWrite(true);
            bindTexture(0, levels[i].getColorAttachment(), true);
            blurUp.set("HalfPixel", 0.5F / levels[i].textureWidth, 0.5F / levels[i].textureHeight);
            blurUp.drawFullscreen();
        }
        blurUp.unbind();
        return levels[0];
    }

    private static void composite(SimpleFramebuffer mask, SimpleFramebuffer glow, Framebuffer target,
                                  Settings settings, float time) {
        target.beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        bindTexture(0, glow.getColorAttachment(), true);
        bindTexture(1, mask.getColorAttachment(), true);
        bindTexture(2, sceneDepth.getDepthAttachment(), false);
        bindTexture(3, mask.getDepthAttachment(), false);
        composite.bind();
        composite.set("glowSampler", 0);
        composite.set("entitySampler", 1);
        composite.set("sceneDepthSampler", 2);
        composite.set("entityDepthSampler", 3);
        composite.setColor("primaryColor", settings.primary(), settings.alpha());
        composite.setColor("secondaryColor", settings.secondary(), settings.alpha());
        composite.set("gradient", settings.gradient() ? 1 : 0);
        composite.set("time", time * settings.speed());
        composite.set("glowAlphaMult", settings.alpha());
        composite.set("intensity", settings.intensity());
        composite.set("glowEnabled", settings.glow() ? 1 : 0);
        composite.set("thinOutline", settings.thinOutline() ? 1 : 0);
        composite.set("uTexelSize", 1.0F / mask.textureWidth, 1.0F / mask.textureHeight);
        composite.drawFullscreen();
        composite.unbind();
        RenderSystem.defaultBlendFunc();
    }

    private static void renderSmoke(SimpleFramebuffer mask, Framebuffer target, Settings settings, float time) {
        ensureSmoke(mask.textureWidth, mask.textureHeight);
        if (smokeFresh) {
            smokeTrail.clear();
            smokeScratch.clear();
            smokeFresh = false;
        }

        // Pass 0: advect the previous trail and add smoke born at the current silhouette.
        smokeScratch.clear();
        smokeScratch.beginWrite(true);
        RenderSystem.disableBlend();
        bindTexture(0, mask.getColorAttachment(), true);
        bindTexture(1, smokeTrail.getColorAttachment(), true);
        smoke.bind();
        setSmokeUniforms(mask, settings, time, 0);
        smoke.drawFullscreen();
        SimpleFramebuffer swap = smokeTrail;
        smokeTrail = smokeScratch;
        smokeScratch = swap;

        // Pass 1: blend the trail over the world.
        target.beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        bindTexture(0, mask.getColorAttachment(), true);
        bindTexture(1, smokeTrail.getColorAttachment(), true);
        smoke.bind();
        setSmokeUniforms(mask, settings, time, 1);
        smoke.drawFullscreen();
        smoke.unbind();
        RenderSystem.defaultBlendFunc();
    }

    private static void setSmokeUniforms(SimpleFramebuffer mask, Settings settings, float time, int pass) {
        smoke.set("MaskTexture", 0);
        smoke.set("TrailTexture", 1);
        smoke.set("time", time);
        smoke.setColor("baseColor", settings.primary(), 1.0F);
        smoke.set("effectAlpha", Math.max(0.0F, Math.min(1.0F, settings.alpha() * settings.intensity())));
        smoke.set("resolution", mask.textureWidth, mask.textureHeight);
        smoke.set("useTextureColor", 0);
        smoke.set("outlineOnly", 0);
        smoke.set("composeMode", pass);
    }

    /** Called when the module is switched off so the next enable starts with a clean trail. */
    public static void resetTrail() {
        smokeFresh = true;
    }

    public static void close() {
        if (sceneDepth != null) sceneDepth.delete();
        sceneDepth = null;
        for (int i = 0; i < LEVELS; i++) {
            if (levels[i] != null) levels[i].delete();
            levels[i] = null;
        }
        if (smokeTrail != null) smokeTrail.delete();
        if (smokeScratch != null) smokeScratch.delete();
        smokeTrail = null;
        smokeScratch = null;
        smokeFresh = true;
    }

    private static void ensurePrograms() {
        if (composite != null) return;
        String vertex = ROOT + "fullscreen.vsh";
        blurDown = new RawShaderProgram(vertex, ROOT + "blur_down.fsh");
        blurUp = new RawShaderProgram(vertex, ROOT + "blur_up.fsh");
        smoke = new RawShaderProgram(vertex, ROOT + "smoke.fsh");
        composite = new RawShaderProgram(vertex, ROOT + "glow_composite.fsh");
    }

    private static void ensureSceneDepth(Framebuffer target) {
        if (sceneDepth != null && sceneDepth.textureWidth == target.textureWidth
                && sceneDepth.textureHeight == target.textureHeight) return;
        if (sceneDepth != null) sceneDepth.delete();
        sceneDepth = new SimpleFramebuffer(target.textureWidth, target.textureHeight, true);
    }

    private static void ensureLevels(int width, int height) {
        int w = width;
        int h = height;
        for (int i = 0; i < LEVELS; i++) {
            w = Math.max(2, w / 2);
            h = Math.max(2, h / 2);
            if (levels[i] != null && levels[i].textureWidth == w && levels[i].textureHeight == h) continue;
            if (levels[i] != null) levels[i].delete();
            levels[i] = new SimpleFramebuffer(w, h, false);
            levels[i].setTexFilter(GL11.GL_LINEAR);
            levels[i].setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    private static void ensureSmoke(int width, int height) {
        int w = Math.max(1, width / 2);
        int h = Math.max(1, height / 2);
        if (smokeTrail != null && smokeTrail.textureWidth == w && smokeTrail.textureHeight == h) return;
        if (smokeTrail != null) smokeTrail.delete();
        if (smokeScratch != null) smokeScratch.delete();
        smokeTrail = new SimpleFramebuffer(w, h, false);
        smokeScratch = new SimpleFramebuffer(w, h, false);
        for (SimpleFramebuffer framebuffer : new SimpleFramebuffer[]{smokeTrail, smokeScratch}) {
            framebuffer.setTexFilter(GL11.GL_LINEAR);
            framebuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
        smokeFresh = true;
    }

    private static void bindTexture(int unit, int texture, boolean linear) {
        RenderSystem.activeTexture(GL13_TEXTURE0 + unit);
        RenderSystem.bindTexture(texture);
        int filter = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 0x812F);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 0x812F);
    }
}
