package fun.enoughvisuals.systems.emotions.render;

import fun.enoughvisuals.systems.emotions.PulseEmotionModel;
import fun.enoughvisuals.systems.emotions.PulseEmotionProp;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Entry point for rendering Pulse BOBJ animations with the player's actual skin. */
@Environment(EnvType.CLIENT)
public final class PulseEmotionRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/PulseEmotions");
    private static final Identifier WIDE_RIG = Identifier.of("enough", "emotions/rig/default.bobj");
    private static final Identifier SLIM_RIG = Identifier.of("enough", "emotions/rig/slim.bobj");
    private static final Object LOAD_LOCK = new Object();
    private static final Map<PulseEmotionProp, CachedProp> PROP_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Set<PulseEmotionProp> FAILED_PROPS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static volatile BobjRig wide;
    private static volatile BobjRig slim;
    private static volatile boolean loadFailureLogged;

    private PulseEmotionRenderer() {
    }

    /**
     * Draws a player at full alpha. The caller should invoke this while LivingEntityRenderer's
     * entity model matrix is still on the stack (immediately before its final
     * {@code MatrixStack.pop()}). Player skins always use Minecraft's translucent entity layer so
     * semi-transparent second-layer pixels retain their vanilla appearance.
     */
    public static boolean render(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            Identifier skinTexture,
            PulseEmotionModel emotion,
            double elapsedSeconds,
            float blend
    ) {
        return render(state, matrices, vertexConsumers, light, overlay, skinTexture, emotion, elapsedSeconds, blend, 1.0f);
    }

    /** Draws a player with the requested alpha through the translucent player-skin layer. */
    public static boolean render(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            Identifier skinTexture,
            PulseEmotionModel emotion,
            double elapsedSeconds,
            float blend,
            float alpha
    ) {
        if (state == null || matrices == null || vertexConsumers == null || skinTexture == null || emotion == null) {
            return false;
        }

        try {
            BobjRig rig = rigFor(state);
            BobjRig.Pose pose = rig.evaluate(emotion.sampleSeconds(elapsedSeconds), blend);
            float clampedAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
            if (clampedAlpha <= 0.0f) return true;

            // Player skins can contain semi-transparent second-layer pixels. Always use the
            // translucent entity path; per-part visibility is handled at triangle level below.
            RenderLayer layer = RenderLayer.getEntityTranslucent(skinTexture);
            VertexConsumer vertices = vertexConsumers.getBuffer(layer);
            rig.render(
                    pose,
                    matrices,
                    vertices,
                    light,
                    overlay,
                    clampedAlpha,
                    new BobjRig.SkinLayerVisibility(
                            state.hatVisible,
                            state.jacketVisible,
                            state.rightSleeveVisible,
                            state.leftSleeveVisible,
                            state.rightPantsLegVisible,
                            state.leftPantsLegVisible));
            renderProps(emotion, pose, matrices, vertexConsumers, light, overlay, clampedAlpha);
            return true;
        } catch (IOException | RuntimeException exception) {
            if (!loadFailureLogged) {
                loadFailureLogged = true;
                LOGGER.error("Unable to render Pulse BOBJ emotion", exception);
            }
            return false;
        }
    }

    /** Evaluates the same pose used by {@link #render}; useful for held-item/cosmetic attachment. */
    public static BobjRig.Pose pose(
            PlayerEntityRenderState state,
            PulseEmotionModel emotion,
            double elapsedSeconds,
            float blend
    ) throws IOException {
        return rigFor(state).evaluate(emotion.sampleSeconds(elapsedSeconds), blend);
    }

    /** Drops parsed rig caches after a resource reload. Safe to call from the client thread. */
    public static void clearCache() {
        synchronized (LOAD_LOCK) {
            wide = null;
            slim = null;
            loadFailureLogged = false;
        }
        Set<Identifier> textures = new HashSet<>();
        synchronized (PROP_CACHE) {
            for (CachedProp prop : PROP_CACHE.values()) textures.add(prop.texture);
            PROP_CACHE.clear();
        }
        synchronized (FAILED_PROPS) {
            FAILED_PROPS.clear();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        for (Identifier texture : textures) client.getTextureManager().destroyTexture(texture);
    }

    private static void renderProps(
            PulseEmotionModel emotion,
            BobjRig.Pose pose,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            float alpha
    ) {
        for (PulseEmotionProp prop : emotion.propModels()) {
            try {
                CachedProp cached = cachedProp(prop);
                RenderLayer layer = alpha < 0.999f
                        ? RenderLayer.getEntityTranslucent(cached.texture)
                        : RenderLayer.getEntityCutoutNoCull(cached.texture);
                cached.mesh.render(pose, matrices, vertexConsumers.getBuffer(layer), light, overlay, alpha);
            } catch (IOException | RuntimeException exception) {
                boolean firstFailure;
                synchronized (FAILED_PROPS) {
                    firstFailure = FAILED_PROPS.add(prop);
                }
                if (firstFailure) LOGGER.error("Unable to render Pulse emotion prop {}", prop.name(), exception);
            }
        }
    }

    private static CachedProp cachedProp(PulseEmotionProp prop) throws IOException {
        CachedProp cached = PROP_CACHE.get(prop);
        if (cached != null) return cached;
        synchronized (PROP_CACHE) {
            cached = PROP_CACHE.get(prop);
            if (cached != null) return cached;

            BobjPropMesh mesh = BobjPropMesh.parse(prop.objSource());
            byte[] png = prop.texturePng();
            Identifier textureId = Identifier.of(
                    "enough", "emotions/generated/prop_" + contentHash(prop.objSource(), png));
            NativeImage image = NativeImage.read(png);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            try {
                MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
            } catch (RuntimeException exception) {
                texture.close();
                throw exception;
            }

            cached = new CachedProp(mesh, textureId);
            PROP_CACHE.put(prop, cached);
            return cached;
        }
    }

    private static String contentHash(String objSource, byte[] png) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(objSource.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(png);
            return HexFormat.of().formatHex(digest.digest(), 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static BobjRig rigFor(PlayerEntityRenderState state) throws IOException {
        boolean useSlim = state.skinTextures != null && state.skinTextures.model() == SkinTextures.Model.SLIM;
        BobjRig cached = useSlim ? slim : wide;
        if (cached != null) return cached;

        synchronized (LOAD_LOCK) {
            cached = useSlim ? slim : wide;
            if (cached != null) return cached;
            cached = loadRig(useSlim ? SLIM_RIG : WIDE_RIG);
            if (useSlim) slim = cached;
            else wide = cached;
            return cached;
        }
    }

    private static BobjRig loadRig(Identifier identifier) throws IOException {
        Resource resource = MinecraftClient.getInstance().getResourceManager().getResource(identifier)
                .orElseThrow(() -> new IOException("Missing BOBJ player rig: " + identifier));
        try (InputStream stream = resource.getInputStream()) {
            return BobjRigLoader.load(stream);
        }
    }

    private record CachedProp(BobjPropMesh mesh, Identifier texture) {
    }
}
