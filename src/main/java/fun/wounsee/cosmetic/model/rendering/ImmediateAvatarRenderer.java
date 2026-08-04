package fun.wounsee.cosmetic.model.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.lua.api.ClientAPI;
import fun.wounsee.cosmetic.math.matrix.FiguraMat3;
import fun.wounsee.cosmetic.math.matrix.FiguraMat4;
import fun.wounsee.cosmetic.math.vector.FiguraVec3;
import fun.wounsee.cosmetic.math.vector.FiguraVec4;
import fun.wounsee.cosmetic.model.*;
import fun.wounsee.cosmetic.model.rendering.texture.FiguraTexture;
import fun.wounsee.cosmetic.model.rendering.texture.FiguraTextureSet;
import fun.wounsee.cosmetic.model.rendering.texture.RenderTypes;
import fun.wounsee.cosmetic.model.rendertasks.RenderTask;
import fun.wounsee.cosmetic.utils.ColorUtils;
import fun.wounsee.cosmetic.utils.ui.UIHelper;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ImmediateAvatarRenderer extends AvatarRenderer {

    protected final PartCustomization.PartCustomizationStack customizationStack = new PartCustomization.PartCustomizationStack();

    public static final FiguraMat4 CAMERA_POS_TO_WORLD_MATRIX = FiguraMat4.of();

    private static final PartCustomization pivotOffsetter = new PartCustomization();
    protected static final VertexBuffer VERTEX_BUFFER = new VertexBuffer();

    public ImmediateAvatarRenderer(Avatar avatar) {
        super(avatar);

        // Vertex data, read model parts
        root = CosmeticModelPartReader.read(avatar, avatar.nbt.getCompound("models"), textureSets, false);

        sortParts();
    }

    public void checkEmpty() {
        if (!customizationStack.isEmpty())
            throw new IllegalStateException("Customization stack not empty!");
    }

    @Override
    public int render() {
        return commonRender(1.5d);
    }

    @Override
    public int renderSpecialParts() {
        return commonRender(0);
    }

    @Override
    public void updateMatrices() {
        // flag rendering state
        this.isRendering = true;

        // setup root customizations
        PartCustomization customization = setupRootCustomization(1.5d);

        // Push transform
        customizationStack.push(customization);

        // world matrices
        CAMERA_POS_TO_WORLD_MATRIX.set(AvatarRenderer.worldToCameraPosMatrix().invert());

        // calculate each part matrices
        calculatePartMatrices(root);

        // finish rendering
        customizationStack.pop();
        checkEmpty();

        this.isRendering = false;
    }

    protected int commonRender(double vertOffset) {
        // flag rendering state
        this.isRendering = true;

        // iris fix
        int irisConfig = UIHelper.paperdoll || !ClientAPI.hasShaderPackMod() ? 0 : Configs.IRIS_COMPATIBILITY_FIX.value;
        doIrisEmissiveFix = (irisConfig >= 2 && ClientAPI.hasShaderPack()) || (avatar.renderMode != EntityRenderMode.RENDER && avatar.renderMode != EntityRenderMode.WORLD);
        offsetRenderLayers = irisConfig >= 1;

        // custom textures
        for (FiguraTextureSet set : textureSets)
            set.uploadIfNeeded();
        for (FiguraTexture texture : customTextures.values())
            texture.uploadIfDirty();

        // Set shouldRenderPivots
        int config = Configs.RENDER_DEBUG_PARTS_PIVOT.value;
        if (!Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes() || (!avatar.isHost && config < 2))
            shouldRenderPivots = 0;
        else
            shouldRenderPivots = config;

        // world matrices
        if (allowMatrixUpdate) {
            CAMERA_POS_TO_WORLD_MATRIX.set(AvatarRenderer.worldToCameraPosMatrix().invert());
        }

        // complexity
        int prev = avatar.complexity.remaining;
        int[] remainingComplexity = new int[] {prev};

        // render all model parts
        if (root.customization.visible) {
            if (currentFilterScheme.parentType.isSeparate) {
                List<CosmeticModelPart> parts = separatedParts.get(currentFilterScheme.parentType);
                if (parts != null) {
                    boolean renderLayer = !currentFilterScheme.parentType.isRenderLayer;
                    if (renderLayer) {
                        PartCustomization customization = setupRootCustomization(vertOffset);
                        customizationStack.push(customization); // push root
                        customizationStack.push(root.customization); // push "models"
                    }

                    for (CosmeticModelPart part : parts) {
                        if (currentFilterScheme.parentType == ParentType.Item && part != itemToRender)
                            continue;

                        boolean saved = part.savedCustomization != null;
                        if (saved) {
                            customizationStack.push(part.savedCustomization);
                            part.savedCustomization = null;
                        }

                        renderPart(part, remainingComplexity, currentFilterScheme.initialValue);

                        if (saved) customizationStack.pop();
                    }

                    if (renderLayer) {
                        customizationStack.pop(); // pop "models"
                        customizationStack.pop(); // pop root
                    }
                }
            } else {
                PartCustomization customization = setupRootCustomization(vertOffset);
                customizationStack.push(customization);
                renderPart(root, remainingComplexity, currentFilterScheme.initialValue);
                customizationStack.pop();
            }

            // push vertices to vertex consumer
            CosmeticMod.pushProfiler("draw");
            CosmeticMod.pushProfiler("primary");
            VERTEX_BUFFER.consume(true, bufferSource);
            CosmeticMod.popPushProfiler("secondary");
            VERTEX_BUFFER.consume(false, bufferSource);
            CosmeticMod.popProfiler(2);

            // finish rendering
            checkEmpty();
        }

        this.isRendering = false;
        if (this.dirty)
            clean();

        return prev - Math.max(remainingComplexity[0], 0);
    }

    protected PartCustomization setupRootCustomization(double vertOffset) {
        PartCustomization customization = new PartCustomization();

        customization.setPrimaryRenderType(RenderTypes.TRANSLUCENT);
        customization.setSecondaryRenderType(RenderTypes.EMISSIVE);

        double s = 1.0 / 16;
        customization.positionMatrix.scale(s, s, s);
        customization.positionMatrix.rotateZ(180);
        customization.positionMatrix.translate(0, vertOffset, 0);
        customization.normalMatrix.rotateZ(180);

        customization.positionMatrix.multiply(posMat);
        customization.normalMatrix.multiply(normalMat);

        customization.light = light;
        customization.alpha = alpha;
        customization.overlay = overlay;

        customization.primaryTexture = new TextureCustomization(FiguraTextureSet.OverrideType.PRIMARY, null);
        customization.secondaryTexture = new TextureCustomization(FiguraTextureSet.OverrideType.SECONDARY, null);

        return customization;
    }

    protected boolean renderPart(CosmeticModelPart part, int[] remainingComplexity, boolean prevPredicate) {
        CosmeticMod.pushProfiler(part.name);

        PartCustomization custom = part.customization;

        // test the current filter scheme
        CosmeticMod.pushProfiler("predicate");
        Boolean thisPassedPredicate = currentFilterScheme.test(part.parentType, prevPredicate);
        if (thisPassedPredicate == null || (!custom.visible)) {
            if (part.parentType.isRenderLayer)
                part.savedCustomization = customizationStack.peek();
            CosmeticMod.popProfiler(2);
            return true;
        }

        // calculate part transforms

        // calculate vanilla parent
        CosmeticMod.popPushProfiler("copyVanillaPart");
        part.applyVanillaTransforms(vanillaModelData);
        part.applyExtraTransforms(customizationStack.peek());

        // visibility
        CosmeticMod.popPushProfiler("checkVanillaVisible");
        if (!ignoreVanillaVisibility && custom.vanillaVisible != null && !custom.vanillaVisible) {
            CosmeticMod.popPushProfiler("removeVanillaTransforms");
            part.resetVanillaTransforms();
            CosmeticMod.popProfiler(2);
            return true;
        }

        // pre render function
        if (part.preRender != null) {
            CosmeticMod.popPushProfiler("preRenderFunction");
            avatar.run(part.preRender, avatar.render, tickDelta, avatar.renderMode.name(), part);
        }

        // recalculate stuff
        CosmeticMod.popPushProfiler("calculatePartMatrices");
        custom.recalculate();

        // void blocked matrices
        // that's right, check only for previous predicate
        FiguraMat4 positionCopy = null;
        FiguraMat3 normalCopy = null;
        boolean voidMatrices = !allowHiddenTransforms && !prevPredicate;
        if (voidMatrices) {
            CosmeticMod.popPushProfiler("clearMatrices");
            positionCopy = custom.positionMatrix.copy();
            normalCopy = custom.normalMatrix.copy();
            custom.positionMatrix.reset();
            custom.normalMatrix.reset();
        }

        // push stack
        CosmeticMod.popPushProfiler("pushCustomizationStack");
        customizationStack.push(custom);

        // restore variables
        if (voidMatrices) {
            CosmeticMod.popPushProfiler("restoreMatrices");
            custom.positionMatrix.set(positionCopy);
            custom.normalMatrix.set(normalCopy);
        }

        if (thisPassedPredicate) {
            // recalculate world matrices
            CosmeticMod.popPushProfiler("worldMatrices");
            if (allowMatrixUpdate) {
                FiguraMat4 mat = partToWorldMatrices(custom);
                part.savedPartToWorldMat.set(mat);
            }

            // recalculate light
            CosmeticMod.popPushProfiler("calculateLight");
            Level l;
            if (custom.light != null) {
                updateLight = false;
                pivotOffsetter.light = custom.light;
            }
            else if (updateLight && (l = Minecraft.getInstance().level) != null) {
                FiguraVec3 pos = part.savedPartToWorldMat.apply(0d, 0d, 0d);
                int block = l.getBrightness(LightLayer.BLOCK, pos.asBlockPos());
                int sky = l.getBrightness(LightLayer.SKY, pos.asBlockPos());
                customizationStack.peek().light = LightTexture.pack(block, sky);
            }

            if (custom.alpha != null)
                pivotOffsetter.alpha = custom.alpha;
            if (custom.overlay != null)
                pivotOffsetter.overlay = custom.overlay;
        }

        // mid render function
        if (part.midRender != null) {
            CosmeticMod.popPushProfiler("midRenderFunction");
            avatar.run(part.midRender, avatar.render, tickDelta, avatar.renderMode.name(), part);
        }

        // render this
        CosmeticMod.popPushProfiler("pushVertices");
        boolean breakRender = thisPassedPredicate && !part.pushVerticesImmediate(this, remainingComplexity);

        // render extras
        CosmeticMod.popPushProfiler("extras");
        if (!breakRender && thisPassedPredicate) {
            boolean renderPivot = shouldRenderPivots > 0;
            boolean renderTasks = !part.renderTasks.isEmpty();
            boolean renderPivotParts = part.parentType.isPivot && allowPivotParts;

            if (renderPivot || renderTasks || renderPivotParts) {
                // fix pivots
                CosmeticMod.pushProfiler("fixMatricesPivot");

                FiguraVec3 pivot = custom.getPivot().copy().add(custom.getOffsetPivot());
                pivotOffsetter.setPos(pivot);
                pivotOffsetter.recalculate();
                customizationStack.push(pivotOffsetter);

                PartCustomization peek = customizationStack.peek();

                // render pivot indicators
                if (renderPivot) {
                    CosmeticMod.popPushProfiler("renderPivotCube");
                    renderPivot(part, peek);
                }

                // render tasks
                if (renderTasks) {
                    CosmeticMod.popPushProfiler("renderTasks");
                    int light = peek.light;
                    int overlay = peek.overlay;
                    interceptRendersIntoFigura = false;
                    for (RenderTask task : part.renderTasks.values()) {
                        if (!task.shouldRender())
                            continue;
                        int neededComplexity = task.getComplexity();
                        if (neededComplexity > remainingComplexity[0])
                            break;
                        CosmeticMod.pushProfiler(task.getName());
                        task.render(customizationStack, bufferSource, light, overlay);
                        remainingComplexity[0] -= neededComplexity;
                        CosmeticMod.popProfiler();
                    }
                    interceptRendersIntoFigura = true;
                }

                // render pivot parts
                if (renderPivotParts && part.parentType.isPivot) {
                    CosmeticMod.popPushProfiler("savePivotParts");
                    savePivotTransform(part.parentType, peek);
                }

                customizationStack.pop();
                CosmeticMod.popProfiler();
            }
        }

        // render children
        CosmeticMod.popPushProfiler("children");
        for (CosmeticModelPart child : List.copyOf(part.children)) {
            if (!renderPart(child, remainingComplexity, thisPassedPredicate)) {
                breakRender = true;
                break;
            }
        }

        // reset the parent
        CosmeticMod.popPushProfiler("removeVanillaTransforms");
        part.resetVanillaTransforms();

        // post render function
        if (part.postRender != null) {
            CosmeticMod.popPushProfiler("postRenderFunction");
            avatar.run(part.postRender, avatar.render, tickDelta, avatar.renderMode.name(), part);
        }

        // pop
        customizationStack.pop();
        CosmeticMod.popProfiler(2);

        return !breakRender;
    }

    protected void renderPivot(CosmeticModelPart part, PartCustomization customization) {
        boolean group = part.customization.partType == PartCustomization.PartType.GROUP;
        FiguraVec3 color = group ? ColorUtils.Colors.FIGURA_BLUE.vec : ColorUtils.Colors.AWESOME_BLUE.vec;
        double boxSize = group ? 1 / 16d : 1 / 32d;
        boxSize /= Math.max(Math.cbrt(part.savedPartToWorldMat.det()), 0.02);

        PoseStack stack = customization.copyIntoGlobalPoseStack();

        ShapeRenderer.renderLineBox(stack, bufferSource.getBuffer(RenderType.LINES),
                -boxSize, -boxSize, -boxSize,
                boxSize, boxSize, boxSize,
                (float) color.x, (float) color.y, (float) color.z, 1f);
    }

    protected void savePivotTransform(ParentType parentType, PartCustomization customization) {
        FiguraMat4 currentPosMat = customization.getPositionMatrix();
        FiguraMat3 currentNormalMat = customization.getNormalMatrix();
        ConcurrentLinkedQueue<Pair<FiguraMat4, FiguraMat3>> queue = pivotCustomizations.computeIfAbsent(parentType, p -> new ConcurrentLinkedQueue<>());
        queue.add(new Pair<>(currentPosMat, currentNormalMat)); // These are COPIES, so ok to add
    }

    protected FiguraMat4 partToWorldMatrices(PartCustomization cust) {
        FiguraMat4 customizePeek = customizationStack.peek().positionMatrix.copy();
        // Translate by the inverse matrix of the camera position, as of 1.20.5 it is no longer dependent on camera rot.
        customizePeek.multiply(CAMERA_POS_TO_WORLD_MATRIX);
        FiguraVec3 piv = cust.getPivot();

        FiguraMat4 translation = FiguraMat4.of();
        translation.translate(piv);
        customizePeek.rightMultiply(translation);

        return customizePeek;
    }

    protected void calculatePartMatrices(CosmeticModelPart part) {
        CosmeticMod.pushProfiler(part.name);

        PartCustomization custom = part.customization;

        // Store old visibility, but overwrite it in case we only want to render certain parts
        CosmeticMod.pushProfiler("predicate");
        Boolean thisPassedPredicate = currentFilterScheme.test(part.parentType, true);
        if (thisPassedPredicate == null) {
            CosmeticMod.popProfiler(2);
            return;
        }

        // calculate part transforms

        // calculate vanilla parent
        CosmeticMod.popPushProfiler("copyVanillaPart");
        part.applyVanillaTransforms(vanillaModelData);
        part.applyExtraTransforms(customizationStack.peek());

        // push customization stack
        CosmeticMod.popPushProfiler("calculatePartMatrices");
        custom.recalculate();
        CosmeticMod.popPushProfiler("applyOnStack");
        customizationStack.push(custom);

        // render extras
        if (thisPassedPredicate) {
            // part to world matrices
            CosmeticMod.popPushProfiler("worldMatrices");
            FiguraMat4 mat = partToWorldMatrices(custom);
            part.savedPartToWorldMat.set(mat);
        }

        // render children
        CosmeticMod.popPushProfiler("children");
        for (CosmeticModelPart child : part.children)
            calculatePartMatrices(child);

        // reset the parent
        part.resetVanillaTransforms();

        // pop
        customizationStack.pop();
        CosmeticMod.popProfiler(2);
    }

    public void pushFaces(int faceCount, int[] remainingComplexity, FiguraTextureSet textureSet, List<Vertex> vertices) {
        // Handle cases that we can quickly
        if (faceCount == 0 || vertices.isEmpty())
            return;

        PartCustomization customization = customizationStack.peek();

        VertexData primary = getTexture(customization, textureSet, true);
        VertexData secondary = getTexture(customization, textureSet, false);

        if (primary.renderType == null && secondary.renderType == null) {
            remainingComplexity[0] += faceCount;
            return;
        }

        if (primary.renderType != null)
            pushToBuffer(faceCount, primary, customization, textureSet, vertices);
        if (secondary.renderType != null)
            pushToBuffer(faceCount, secondary, customization, textureSet, vertices);
    }

    private VertexData getTexture(PartCustomization customization, FiguraTextureSet textureSet, boolean primary) {
        RenderTypes types = primary ? customization.getPrimaryRenderType() : customization.getSecondaryRenderType();
        TextureCustomization texture = primary ? customization.primaryTexture : customization.secondaryTexture;
        VertexData ret = new VertexData();

        if (types == RenderTypes.NONE)
            return ret;

        // get texture
        ResourceLocation id = textureSet.getOverrideTexture(avatar.owner, texture);

        // color
        ret.color = primary ? customization.color : customization.color2;

        // primary
        ret.primary = primary;

        // get render type
        if (id != null) {
            if (translucent) {
                ret.renderType = RenderType.itemEntityTranslucentCull(id);
                return ret;
            }
            if (glowing) {
                ret.renderType = RenderType.outline(id);
                return ret;
            }
        }

        if (types == null)
            return ret;

        if (offsetRenderLayers && !primary && types.isOffset())
            ret.vertexOffset = CosmeticMod.VERTEX_OFFSET;

        // Switch to cutout with fullbright if the iris emissive fix is enabled
        if (doIrisEmissiveFix && types == RenderTypes.EMISSIVE) {
            ret.fullBright = true;
            ret.renderType = RenderTypes.TRANSLUCENT_CULL.get(id);
        } else {
            ret.renderType = types.get(id);
        }

        return ret;
    }

    private static final FiguraVec4 pos = FiguraVec4.of();
    private static final FiguraVec3 normal = FiguraVec3.of();
    private static final FiguraVec3 uv = FiguraVec3.of(0, 0, 1);
    private void pushToBuffer(int faceCount, VertexData vertexData, PartCustomization customization, FiguraTextureSet textureSet, List<Vertex> vertices) {
        int vertCount = faceCount * 4;

        FiguraVec3 uvFixer = FiguraVec3.of();
        uvFixer.set(textureSet.getWidth(), textureSet.getHeight(), 1); // Dividing by this makes uv 0 to 1

        int overlay = customization.overlay;
        int light = vertexData.fullBright ? LightTexture.FULL_BRIGHT : customization.light;

        VERTEX_BUFFER.getBufferFor(vertexData.renderType, vertexData.primary, vertexConsumer -> {
            for (int i = 0; i < vertCount; i++) {
                Vertex vertex = vertices.get(i);

                pos.set(vertex.x, vertex.y, vertex.z, 1);
                pos.transform(customization.positionMatrix);
                pos.add(pos.normalized().scale(vertexData.vertexOffset));
                normal.set(vertex.nx, vertex.ny, vertex.nz);
                normal.transform(customization.normalMatrix);
                uv.set(vertex.u, vertex.v, 1);
                uv.divide(uvFixer);
                uv.transform(customization.uvMatrix);

                vertexConsumer
                        .addVertex((float) pos.x, (float) pos.y, (float) pos.z)
                        .setColor((float) vertexData.color.x, (float) vertexData.color.y, (float) vertexData.color.z, customization.alpha)
                        .setUv((float) uv.x, (float) uv.y)
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
            }
        });
    }

    private static class VertexData {
        public RenderType renderType;
        public boolean fullBright;
        public float vertexOffset;
        public FiguraVec3 color;
        public boolean primary;
    }

    private static class VertexBuffer {
        private final HashMap<RenderType, List<Consumer<VertexConsumer>>> primaryBuffers = new LinkedHashMap<>();
        private final HashMap<RenderType, List<Consumer<VertexConsumer>>> secondaryBuffers = new LinkedHashMap<>();

        public void getBufferFor(RenderType renderType, boolean primary, Consumer<VertexConsumer> consumer) {
            HashMap<RenderType, List<Consumer<VertexConsumer>>> buffer = primary ? primaryBuffers : secondaryBuffers;
            List<Consumer<VertexConsumer>> list = buffer.computeIfAbsent(renderType, renderType1 -> new ArrayList<>());
            list.add(consumer);
        }

        public void consume(boolean primary, MultiBufferSource bufferSource) {
            HashMap<RenderType, List<Consumer<VertexConsumer>>> map = primary ? primaryBuffers : secondaryBuffers;
            for (Map.Entry<RenderType, List<Consumer<VertexConsumer>>> entry : map.entrySet()) {
                VertexConsumer vertexConsumer = bufferSource.getBuffer(entry.getKey());
                List<Consumer<VertexConsumer>> consumers = entry.getValue();
                for (Consumer<VertexConsumer> consumer : consumers)
                    consumer.accept(vertexConsumer);
            }
            map.clear();
        }
    }
}
