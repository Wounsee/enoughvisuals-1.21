package fun.enoughvisuals.utils.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.enoughvisuals.systems.modules.modules.visual.CustomModel;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CustomModelRenderer {
    private static final Map<String, GlbModel> MODELS = new HashMap<>();

    private CustomModelRenderer() {
    }

    public static void renderInWorld(WorldRenderContext context) {
        CustomModel module = CustomModel.active();
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null || client.player == null || client.world == null || context.matrixStack() == null || context.camera() == null) {
            return;
        }
        if (client.options.getPerspective() == Perspective.FIRST_PERSON) {
            return;
        }

        GlbModel model = model(module.modelPath());
        if (model == null || context.consumers() == null) {
            return;
        }

        float tickDelta = context.tickCounter().getTickDelta(true);
        AbstractClientPlayerEntity player = client.player;
        Vec3d pos = player.getLerpedPos(tickDelta);
        Vec3d camera = context.camera().getPos();
        float bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float headYaw = MathHelper.wrapDegrees(player.getYaw(tickDelta) - bodyYaw);
        float pitch = player.getPitch(tickDelta);
        float limbPos = player.limbAnimator.getPos(tickDelta);
        float limbSpeed = Math.min(player.limbAnimator.getSpeed(tickDelta), 1.0f);

        Matrix4f base = new Matrix4f(context.matrixStack().peek().getPositionMatrix());
        base.translate((float) (pos.x - camera.x), (float) (pos.y - camera.y), (float) (pos.z - camera.z));
        String modelPath = module.modelPath();
        base.rotateY((float) Math.toRadians(180.0f - bodyYaw + module.yawOffset.get().floatValue() + module.modelYawOffset()));
        if (player.isSneaking()) {
            base.translate(0.0f, -0.12f, 0.0f);
            base.rotateX((float) Math.toRadians(8.0f));
        }
        float scale = module.size.get().floatValue();
        base.scale(scale, scale, scale);

        Pose pose = new Pose(modelPath, headYaw, pitch, limbPos, limbSpeed, player.age + tickDelta);
        model.render(base, context.consumers(), pose);
        if (context.consumers() instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    public static boolean shouldHideVanillaPlayer(int entityId) {
        CustomModel module = CustomModel.active();
        MinecraftClient client = MinecraftClient.getInstance();
        return module != null
                && client.player != null
                && client.options.getPerspective() != Perspective.FIRST_PERSON
                && client.player.getId() == entityId;
    }

    public static void renderModel(String path, Matrix4f base, VertexConsumerProvider consumers, float headYaw, float pitch, float limbPos, float limbSpeed, float age) {
        GlbModel model = model(path);
        if (model == null || consumers == null) {
            return;
        }
        model.render(base, consumers, new Pose(path, headYaw, pitch, limbPos, limbSpeed, age));
    }

    private static GlbModel model(String path) {
        return MODELS.computeIfAbsent(path, CustomModelRenderer::loadModel);
    }

    private static GlbModel loadModel(String path) {
        try (InputStream in = CustomModelRenderer.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return GlbModel.read(path, readAll(in));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private record Pose(String modelKey, float headYaw, float pitch, float limbPos, float limbSpeed, float age) {
    }

    private static final class GlbModel {
        private final List<Node> nodes = new ArrayList<>();
        private final List<Integer> roots = new ArrayList<>();
        private final List<Mesh> meshes = new ArrayList<>();
        private final List<MaterialInfo> materials = new ArrayList<>();
        private final List<Skin> skins = new ArrayList<>();
        private Matrix4f normalize = new Matrix4f();

        static GlbModel read(String key, byte[] bytes) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if (buffer.getInt(0) != 0x46546C67) return null;
            int length = buffer.getInt(8);
            int offset = 12;
            JsonObject json = null;
            byte[] bin = null;
            while (offset + 8 <= length) {
                int chunkLength = buffer.getInt(offset);
                int chunkType = buffer.getInt(offset + 4);
                offset += 8;
                byte[] chunk = new byte[chunkLength];
                buffer.position(offset);
                buffer.get(chunk);
                offset += chunkLength;
                if (chunkType == 0x4E4F534A) {
                    json = JsonParser.parseString(new String(chunk, StandardCharsets.UTF_8).trim()).getAsJsonObject();
                } else if (chunkType == 0x004E4942) {
                    bin = chunk;
                }
            }
            if (json == null || bin == null) return null;

            GlbModel model = new GlbModel();
            JsonArray bufferViewsJson = json.getAsJsonArray("bufferViews");
            JsonArray accessorsJson = json.getAsJsonArray("accessors");
            List<BufferView> views = readViews(bufferViewsJson);
            List<Accessor> accessors = readAccessors(accessorsJson);

            model.readTextures(key, json, bin, views);
            model.readSkins(json, bin, views, accessors);
            model.readMeshes(json, bin, views, accessors);
            model.readNodes(json);
            model.readScene(json);
            model.computeNodeBounds();
            model.computeNormalize();
            return model;
        }

        private static List<BufferView> readViews(JsonArray array) {
            List<BufferView> views = new ArrayList<>();
            if (array == null) return views;
            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                views.add(new BufferView(
                        intValue(object, "byteOffset", 0),
                        intValue(object, "byteLength", 0),
                        intValue(object, "byteStride", 0)
                ));
            }
            return views;
        }

        private static List<Accessor> readAccessors(JsonArray array) {
            List<Accessor> accessors = new ArrayList<>();
            if (array == null) return accessors;
            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                accessors.add(new Accessor(
                        intValue(object, "bufferView", -1),
                        intValue(object, "byteOffset", 0),
                        intValue(object, "componentType", 5126),
                        intValue(object, "count", 0),
                        stringValue(object, "type", "SCALAR")
                ));
            }
            return accessors;
        }

        private void readTextures(String key, JsonObject json, byte[] bin, List<BufferView> views) throws Exception {
            JsonArray images = json.getAsJsonArray("images");
            JsonArray textures = json.getAsJsonArray("textures");
            JsonArray materials = json.getAsJsonArray("materials");
            List<Identifier> imageIds = new ArrayList<>();
            if (images != null) {
                for (int i = 0; i < images.size(); i++) {
                    JsonObject image = images.get(i).getAsJsonObject();
                    int viewIndex = intValue(image, "bufferView", -1);
                    if (viewIndex < 0 || viewIndex >= views.size()) {
                        imageIds.add(TextureManagerFallback.WHITE);
                        continue;
                    }
                    BufferView view = views.get(viewIndex);
                    byte[] png = new byte[view.length];
                    System.arraycopy(bin, view.offset, png, 0, view.length);
                    NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(png));
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                    texture.setFilter(false, false);
                    Identifier id = Identifier.of("enough", "custom_model/" + safeKey(key) + "/image_" + i);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                    imageIds.add(id);
                }
            }

            if (materials != null) {
                for (JsonElement materialElement : materials) {
                    int textureIndex = -1;
                    JsonObject material = materialElement.getAsJsonObject();
                    float[] factor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
                    if (material.has("pbrMetallicRoughness")) {
                        JsonObject pbr = material.getAsJsonObject("pbrMetallicRoughness");
                        JsonArray factorJson = pbr.getAsJsonArray("baseColorFactor");
                        if (factorJson != null) {
                            for (int i = 0; i < factor.length && i < factorJson.size(); i++) {
                                factor[i] = factorJson.get(i).getAsFloat();
                            }
                        }
                        if (pbr.has("baseColorTexture")) {
                            textureIndex = intValue(pbr.getAsJsonObject("baseColorTexture"), "index", -1);
                        }
                    }
                    if (textureIndex < 0 && material.has("emissiveTexture")) {
                        textureIndex = intValue(material.getAsJsonObject("emissiveTexture"), "index", -1);
                    }
                    if (textureIndex < 0 && material.has("normalTexture")) {
                        textureIndex = intValue(material.getAsJsonObject("normalTexture"), "index", -1);
                    }
                    if (textureIndex < 0 && material.has("extensions")) {
                        JsonObject extensions = material.getAsJsonObject("extensions");
                        if (extensions.has("KHR_materials_pbrSpecularGlossiness")) {
                            JsonObject specGloss = extensions.getAsJsonObject("KHR_materials_pbrSpecularGlossiness");
                            if (specGloss.has("diffuseTexture")) {
                                textureIndex = intValue(specGloss.getAsJsonObject("diffuseTexture"), "index", -1);
                            }
                        }
                    }
                    int imageIndex = -1;
                    if (textures != null && textureIndex >= 0 && textureIndex < textures.size()) {
                        imageIndex = intValue(textures.get(textureIndex).getAsJsonObject(), "source", -1);
                    }
                    Identifier texture = imageIndex >= 0 && imageIndex < imageIds.size() ? imageIds.get(imageIndex) : TextureManagerFallback.WHITE;
                    boolean doubleSided = material.has("doubleSided") && material.get("doubleSided").getAsBoolean();
                    String alphaMode = stringValue(material, "alphaMode", "OPAQUE");
                    this.materials.add(new MaterialInfo(texture, factor[0], factor[1], factor[2], factor[3], doubleSided, alphaMode));
                }
            }
            if (this.materials.isEmpty()) {
                this.materials.add(new MaterialInfo(TextureManagerFallback.WHITE, 1.0f, 1.0f, 1.0f, 1.0f, true, "OPAQUE"));
            }
        }

        private void readMeshes(JsonObject json, byte[] bin, List<BufferView> views, List<Accessor> accessors) {
            JsonArray meshesJson = json.getAsJsonArray("meshes");
            if (meshesJson == null) return;
            for (JsonElement meshElement : meshesJson) {
                Mesh mesh = new Mesh();
                JsonArray primitives = meshElement.getAsJsonObject().getAsJsonArray("primitives");
                if (primitives != null) {
                    for (JsonElement primitiveElement : primitives) {
                        JsonObject primitive = primitiveElement.getAsJsonObject();
                        JsonObject attributes = primitive.getAsJsonObject("attributes");
                        float[] positions = readFloatAccessor(bin, views, accessors, intValue(attributes, "POSITION", -1), 3);
                        float[] normals = readFloatAccessor(bin, views, accessors, intValue(attributes, "NORMAL", -1), 3);
                        float[] uvs = readFloatAccessor(bin, views, accessors, intValue(attributes, "TEXCOORD_0", -1), 2);
                        int[] joints = readJointAccessor(bin, views, accessors, intValue(attributes, "JOINTS_0", -1), positions.length / 3);
                        float[] weights = readFloatAccessor(bin, views, accessors, intValue(attributes, "WEIGHTS_0", -1), 4);
                        int[] indices = readIndexAccessor(bin, views, accessors, intValue(primitive, "indices", -1), positions.length / 3);
                        mesh.primitives.add(new Primitive(positions, normals, uvs, joints, weights, indices, intValue(primitive, "material", 0)));
                    }
                }
                meshes.add(mesh);
            }
        }

        private void readNodes(JsonObject json) {
            JsonArray nodesJson = json.getAsJsonArray("nodes");
            if (nodesJson == null) return;
            for (JsonElement nodeElement : nodesJson) {
                JsonObject object = nodeElement.getAsJsonObject();
                Node node = new Node();
                node.index = nodes.size();
                node.name = stringValue(object, "name", "");
                node.mesh = intValue(object, "mesh", -1);
                node.skin = intValue(object, "skin", -1);
                node.local = localMatrix(object);
                JsonArray children = object.getAsJsonArray("children");
                if (children != null) {
                    for (JsonElement child : children) {
                        node.children.add(child.getAsInt());
                    }
                }
                nodes.add(node);
            }
        }

        private void readScene(JsonObject json) {
            int sceneIndex = intValue(json, "scene", 0);
            JsonArray scenes = json.getAsJsonArray("scenes");
            if (scenes != null && sceneIndex >= 0 && sceneIndex < scenes.size()) {
                JsonArray sceneNodes = scenes.get(sceneIndex).getAsJsonObject().getAsJsonArray("nodes");
                if (sceneNodes != null) {
                    for (JsonElement root : sceneNodes) roots.add(root.getAsInt());
                }
            }
            if (roots.isEmpty() && !nodes.isEmpty()) {
                roots.add(0);
            }
        }

        private void computeNodeBounds() {
            for (int i = 0; i < nodes.size(); i++) {
                Bounds bounds = new Bounds();
                collectLocalBounds(i, new Matrix4f(), bounds);
                nodes.get(i).bounds = bounds;
            }
        }

        private void computeNormalize() {
            Bounds bounds = new Bounds();
            for (int root : roots) {
                collectBounds(root, new Matrix4f(), bounds);
            }
            if (!bounds.valid) return;
            float height = Math.max(0.001f, bounds.maxY - bounds.minY);
            float scale = 1.82f / height;
            float centerX = (bounds.minX + bounds.maxX) * 0.5f;
            float centerZ = (bounds.minZ + bounds.maxZ) * 0.5f;
            normalize = new Matrix4f().scale(scale).translate(-centerX, -bounds.minY, -centerZ);
        }

        void render(Matrix4f base, VertexConsumerProvider consumers, Pose pose) {
            Matrix4f renderBase = new Matrix4f(base).mul(normalize);
            Matrix4f[] localWorld = new Matrix4f[nodes.size()];
            for (int root : roots) {
                buildWorld(root, new Matrix4f(), localWorld, pose);
            }
            Matrix4f[] world = new Matrix4f[nodes.size()];
            for (int i = 0; i < localWorld.length; i++) {
                if (localWorld[i] != null) {
                    world[i] = new Matrix4f(renderBase).mul(localWorld[i]);
                }
            }
            for (int root : roots) {
                renderNode(root, localWorld, world, consumers, pose);
            }
        }

        private void buildWorld(int index, Matrix4f parent, Matrix4f[] world, Pose pose) {
            if (index < 0 || index >= nodes.size()) return;
            Node node = nodes.get(index);
            if (shouldSkipNode(node, pose)) return;
            Matrix4f local = new Matrix4f(parent).mul(node.local).mul(animationFor(node, pose));
            world[index] = local;
            for (int child : node.children) {
                buildWorld(child, local, world, pose);
            }
        }

        private void renderNode(int index, Matrix4f[] localWorld, Matrix4f[] world, VertexConsumerProvider consumers, Pose pose) {
            if (index < 0 || index >= nodes.size()) return;
            Node node = nodes.get(index);
            if (shouldSkipNode(node, pose)) return;
            if (node.mesh >= 0 && node.mesh < meshes.size()) {
                Skin skin = node.skin >= 0 && node.skin < skins.size() ? skins.get(node.skin) : null;
                Matrix4f transform = world[index] != null ? world[index] : new Matrix4f();
                Matrix4f localTransform = localWorld[index] != null ? localWorld[index] : new Matrix4f();
                renderMesh(meshes.get(node.mesh), skin, localWorld, localTransform, transform, consumers);
            }
            for (int child : node.children) {
                renderNode(child, localWorld, world, consumers, pose);
            }
        }

        private void renderMesh(Mesh mesh, Skin skin, Matrix4f[] world, Matrix4f localTransform, Matrix4f transform, VertexConsumerProvider consumers) {
            Matrix4f inverseMeshTransform = skin != null ? new Matrix4f(localTransform).invert() : new Matrix4f();
            for (Primitive primitive : mesh.primitives) {
                MaterialInfo material = materials.get(Math.max(0, Math.min(materials.size() - 1, primitive.material)));
                VertexConsumer consumer = consumers.getBuffer(materialLayer(material));
                for (int i = 0; i + 2 < primitive.indices.length; i += 3) {
                    int i0 = primitive.indices[i];
                    int i1 = primitive.indices[i + 1];
                    int i2 = primitive.indices[i + 2];
                    writeVertex(consumer, transform, primitive, skin, world, inverseMeshTransform, i0, material, false);
                    writeVertex(consumer, transform, primitive, skin, world, inverseMeshTransform, i1, material, false);
                    writeVertex(consumer, transform, primitive, skin, world, inverseMeshTransform, i2, material, false);
                    if (material.doubleSided()) {
                        writeVertex(consumer, transform, primitive, skin, world, inverseMeshTransform, i2, material, true);
                        writeVertex(consumer, transform, primitive, skin, world, inverseMeshTransform, i1, material, true);
                        writeVertex(consumer, transform, primitive, skin, world, inverseMeshTransform, i0, material, true);
                    }
                }
            }
        }

        private void writeVertex(VertexConsumer consumer, Matrix4f matrix, Primitive primitive, Skin skin, Matrix4f[] localWorld, Matrix4f inverseMeshTransform, int index, MaterialInfo material, boolean flipNormal) {
            int pi = index * 3;
            int ti = index * 2;
            if (pi < 0 || pi + 2 >= primitive.positions.length) {
                return;
            }
            Vector3f pos = new Vector3f(primitive.positions[pi], primitive.positions[pi + 1], primitive.positions[pi + 2]);
            Vector3f normal = primitive.normals.length >= pi + 3 ? new Vector3f(primitive.normals[pi], primitive.normals[pi + 1], primitive.normals[pi + 2]) : new Vector3f(0.0f, 1.0f, 0.0f);
            if (skin != null && primitive.joints.length >= index * 4 + 4 && primitive.weights.length >= index * 4 + 4) {
                Vector3f skinnedPos = new Vector3f();
                Vector3f skinnedNormal = new Vector3f();
                for (int i = 0; i < 4; i++) {
                    float weight = primitive.weights[index * 4 + i];
                    if (weight <= 0.0f) continue;
                    int jointIndex = primitive.joints[index * 4 + i];
                    Matrix4f jointMatrix = skin.jointMatrix(jointIndex, localWorld, inverseMeshTransform);
                    Matrix3f normalMatrix = new Matrix3f(jointMatrix).invert().transpose();
                    Vector3f jointPos = new Vector3f(pos).mulPosition(jointMatrix).mul(weight);
                    Vector3f jointNormal = new Vector3f(normal).mul(normalMatrix).mul(weight);
                    skinnedPos.add(jointPos);
                    skinnedNormal.add(jointNormal);
                }
                pos = skinnedPos;
                normal = skinnedNormal.normalize();
            }
            Matrix3f normalMatrix = new Matrix3f(matrix).invert().transpose();
            Vector3f transformedNormal = normal.mul(normalMatrix).normalize();
            if (flipNormal) {
                transformedNormal.negate();
            }
            float u = primitive.uvs.length >= ti + 2 ? primitive.uvs[ti] : 0.0f;
            float v = primitive.uvs.length >= ti + 2 ? primitive.uvs[ti + 1] : 0.0f;
            consumer.vertex(matrix, pos.x, pos.y, pos.z)
                    .color(
                            MathHelper.clamp((int) (material.r() * 255.0f), 0, 255),
                            MathHelper.clamp((int) (material.g() * 255.0f), 0, 255),
                            MathHelper.clamp((int) (material.b() * 255.0f), 0, 255),
                            MathHelper.clamp((int) (material.a() * 255.0f), 0, 255)
                    )
                    .texture(u, v)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                    .normal(transformedNormal.x, transformedNormal.y, transformedNormal.z);
        }

        private void readSkins(JsonObject json, byte[] bin, List<BufferView> views, List<Accessor> accessors) {
            JsonArray skinsJson = json.getAsJsonArray("skins");
            if (skinsJson == null) return;
            for (JsonElement skinElement : skinsJson) {
                JsonObject skinObject = skinElement.getAsJsonObject();
                JsonArray jointsJson = skinObject.getAsJsonArray("joints");
                if (jointsJson == null || jointsJson.isEmpty()) {
                    skins.add(Skin.empty());
                    continue;
                }
                int[] joints = new int[jointsJson.size()];
                for (int i = 0; i < jointsJson.size(); i++) {
                    joints[i] = jointsJson.get(i).getAsInt();
                }
                int inverseIndex = intValue(skinObject, "inverseBindMatrices", -1);
                float[] inverseValues = readFloatAccessor(bin, views, accessors, inverseIndex, 16);
                Matrix4f[] inverse = new Matrix4f[joints.length];
                for (int i = 0; i < joints.length; i++) {
                    int base = i * 16;
                    if (base + 15 < inverseValues.length) {
                        float[] values = new float[16];
                        System.arraycopy(inverseValues, base, values, 0, 16);
                        inverse[i] = new Matrix4f().setTransposed(values);
                    } else {
                        inverse[i] = new Matrix4f();
                    }
                }
                skins.add(new Skin(joints, inverse));
            }
        }

        private void collectBounds(int index, Matrix4f parent, Bounds bounds) {
            if (index < 0 || index >= nodes.size()) return;
            Node node = nodes.get(index);
            Matrix4f transform = new Matrix4f(parent).mul(node.local);
            if (node.mesh >= 0 && node.mesh < meshes.size()) {
                for (Primitive primitive : meshes.get(node.mesh).primitives) {
                    for (int i = 0; i + 2 < primitive.positions.length; i += 3) {
                        Vector3f point = new Vector3f(primitive.positions[i], primitive.positions[i + 1], primitive.positions[i + 2]).mulPosition(transform);
                        bounds.include(point);
                    }
                }
            }
            for (int child : node.children) {
                collectBounds(child, transform, bounds);
            }
        }

        private void collectLocalBounds(int index, Matrix4f transform, Bounds bounds) {
            if (index < 0 || index >= nodes.size()) return;
            Node node = nodes.get(index);
            if (node.mesh >= 0 && node.mesh < meshes.size()) {
                for (Primitive primitive : meshes.get(node.mesh).primitives) {
                    for (int i = 0; i + 2 < primitive.positions.length; i += 3) {
                        Vector3f point = new Vector3f(primitive.positions[i], primitive.positions[i + 1], primitive.positions[i + 2]).mulPosition(transform);
                        bounds.include(point);
                    }
                }
            }
            for (int child : node.children) {
                if (child < 0 || child >= nodes.size()) continue;
                collectLocalBounds(child, new Matrix4f(transform).mul(nodes.get(child).local), bounds);
            }
        }
    }

    private static Matrix4f animationFor(Node node, Pose pose) {
        String lower = node.name == null ? "" : node.name.toLowerCase();
        String model = pose.modelKey == null ? "" : pose.modelKey.toLowerCase();
        Matrix4f matrix = new Matrix4f();
        float swing = MathHelper.sin(pose.limbPos * 0.6662f) * 0.52f * pose.limbSpeed;
        boolean freddy = model.contains("glamrock_freddy");
        boolean car = model.contains("car");
        boolean repo = model.contains("repo");
        boolean trashcan = model.contains("trashcan");
        boolean allay = model.contains("allay");
        boolean skinned = allay || model.contains("bear") || model.contains("warden");

        if (car) {
            return matrix;
        }
        if (lower.contains("_end")) {
            return matrix;
        }
        if (skinned) {
            return matrix;
        }
        if (trashcan) {
            if (node.index == 2) {
                float step = MathHelper.sin(pose.limbPos * 0.6662f) * pose.limbSpeed;
                matrix.rotateX(step * 0.035f);
                matrix.rotateZ(step * 0.08f);
                return aroundBottomPivot(node, matrix);
            }
            return matrix;
        }
        if (repo) {
            float step = MathHelper.sin(pose.limbPos * 0.6662f) * pose.limbSpeed;
            if (node.index == 11) {
                matrix.rotateX(-step * 0.55f);
                return aroundTopPivot(node, matrix, 0.08f);
            } else if (node.index == 16) {
                matrix.rotateX(step * 0.55f);
                return aroundTopPivot(node, matrix, 0.08f);
            } else if (node.index == 6) {
                matrix.rotateX(step * 0.65f);
                return aroundTopPivot(node, matrix, 0.0f);
            } else if (node.index == 8) {
                matrix.rotateX(-step * 0.65f);
                return aroundTopPivot(node, matrix, 0.0f);
            }
            return matrix;
        }

        boolean rightArm = lower.startsWith("rarm") || lower.startsWith("armr") || lower.contains("right_arm") || lower.contains("arm_right");
        boolean leftArm = lower.startsWith("larm") || lower.startsWith("arml") || lower.contains("left_arm") || lower.contains("arm_left");
        boolean rightLeg = lower.startsWith("rleg") || lower.startsWith("legr") || lower.startsWith("legrf") || lower.startsWith("legrb") || lower.contains("right_leg");
        boolean leftLeg = lower.startsWith("lleg") || lower.startsWith("legl") || lower.startsWith("leglf") || lower.startsWith("leglb") || lower.contains("left_leg");

        if (lower.startsWith("head") || lower.contains("_head")) {
            if (freddy) {
                return matrix;
            }
            matrix.rotateY((float) Math.toRadians(-pose.headYaw * 0.75f));
            matrix.rotateX((float) Math.toRadians(pose.pitch * 0.75f));
        } else if (lower.contains("wing")) {
            float flap = MathHelper.sin(pose.age * 1.85f) * (allay ? 0.42f : 0.32f);
            boolean leftWing = lower.contains("wingl") || lower.contains("left") || lower.contains("_1");
            float side = leftWing ? -1.0f : 1.0f;
            matrix.rotateY(side * (0.34f + flap * 0.30f));
            matrix.rotateZ(side * (0.12f + flap * 0.13f));
        } else if (rightArm) {
            float rest = armRest(model, true);
            matrix.rotateZ((float) Math.toRadians(rest));
            if (freddy) {
                matrix.rotateZ(swing);
            } else {
                matrix.rotateX(swing * 0.75f);
            }
        } else if (leftArm) {
            float rest = armRest(model, false);
            matrix.rotateZ((float) Math.toRadians(rest));
            if (freddy) {
                matrix.rotateZ(-swing);
            } else {
                matrix.rotateX(-swing * 0.75f);
            }
        } else if (rightLeg) {
            if (freddy) {
                matrix.rotateZ(swing);
            } else {
                matrix.rotateX(-swing);
            }
        } else if (leftLeg) {
            if (freddy) {
                matrix.rotateZ(-swing);
            } else {
                matrix.rotateX(swing);
            }
        } else if (lower.startsWith("torso") || lower.startsWith("spine") || lower.startsWith("body")) {
            float bob = pose.limbSpeed > 0.05f ? MathHelper.sin(pose.age * 0.12f) * 0.025f : 0.0f;
            matrix.rotateX(bob);
        }
        if (skinned) {
            return matrix;
        }
        return aroundPivot(node, matrix);
    }

    private static boolean shouldSkipNode(Node node, Pose pose) {
        return false;
    }

    private static float armRest(String model, boolean right) {
        if (model.contains("allay")) {
            return right ? -58.0f : 58.0f;
        }
        if (model.contains("bear") || model.contains("warden") || model.contains("glamrock_freddy")) {
            return 0.0f;
        }
        if (model.contains("car")) {
            return right ? -45.0f : 45.0f;
        }
        return right ? -70.0f : 70.0f;
    }

    private static Matrix4f aroundPivot(Node node, Matrix4f rotation) {
        if (node.bounds == null || !node.bounds.valid) {
            return rotation;
        }

        String lower = node.name == null ? "" : node.name.toLowerCase();
        float pivotX = (node.bounds.minX + node.bounds.maxX) * 0.5f;
        float pivotY = (node.bounds.minY + node.bounds.maxY) * 0.5f;
        float pivotZ = (node.bounds.minZ + node.bounds.maxZ) * 0.5f;

        if (lower.contains("wing")) {
            boolean left = lower.contains("wingl") || lower.contains("_1") || lower.contains("left");
            pivotX = left ? node.bounds.maxX : node.bounds.minX;
            pivotY = node.bounds.maxY;
            pivotZ = (node.bounds.minZ + node.bounds.maxZ) * 0.5f;
        } else if (lower.contains("arm") || lower.contains("leg") || lower.contains("spine") || lower.startsWith("body")) {
            pivotY = node.bounds.maxY;
        } else if (lower.startsWith("head") || lower.contains("_head")) {
            pivotY = node.bounds.minY;
        }

        return new Matrix4f()
                .translate(pivotX, pivotY, pivotZ)
                .mul(rotation)
                .translate(-pivotX, -pivotY, -pivotZ);
    }

    private static Matrix4f aroundTopPivot(Node node, Matrix4f rotation, float pivotYExtra) {
        if (node.bounds == null || !node.bounds.valid) {
            return rotation;
        }

        return aroundExplicitPivotY(node, rotation, node.bounds.maxY + pivotYExtra);
    }

    private static Matrix4f aroundBottomPivot(Node node, Matrix4f rotation) {
        if (node.bounds == null || !node.bounds.valid) {
            return rotation;
        }

        return aroundExplicitPivotY(node, rotation, node.bounds.minY);
    }

    private static Matrix4f aroundExplicitPivotY(Node node, Matrix4f rotation, float pivotY) {
        float pivotX = (node.bounds.minX + node.bounds.maxX) * 0.5f;
        float pivotZ = (node.bounds.minZ + node.bounds.maxZ) * 0.5f;

        return new Matrix4f()
                .translate(pivotX, pivotY, pivotZ)
                .mul(rotation)
                .translate(-pivotX, -pivotY, -pivotZ);
    }

    private static RenderLayer materialLayer(MaterialInfo material) {
        if ("BLEND".equalsIgnoreCase(material.alphaMode())) {
            return RenderLayer.getEntityTranslucent(material.texture());
        }
        if ("MASK".equalsIgnoreCase(material.alphaMode()) || material.doubleSided()) {
            return RenderLayer.getEntityCutoutNoCull(material.texture());
        }
        return RenderLayer.getEntityCutout(material.texture());
    }

    private static Matrix4f localMatrix(JsonObject object) {
        if (object.has("matrix")) {
            JsonArray array = object.getAsJsonArray("matrix");
            float[] values = new float[16];
            for (int i = 0; i < 16 && i < array.size(); i++) {
                values[i] = array.get(i).getAsFloat();
            }
            return new Matrix4f().set(values);
        }
        float[] t = array(object, "translation", 3, 0.0f);
        float[] r = array(object, "rotation", 4, 0.0f);
        float[] s = array(object, "scale", 3, 1.0f);
        if (!object.has("rotation")) r[3] = 1.0f;
        return new Matrix4f()
                .translation(t[0], t[1], t[2])
                .rotate(new Quaternionf(r[0], r[1], r[2], r[3]))
                .scale(s[0], s[1], s[2]);
    }

    private static float[] readFloatAccessor(byte[] bin, List<BufferView> views, List<Accessor> accessors, int accessorIndex, int components) {
        if (accessorIndex < 0 || accessorIndex >= accessors.size()) return new float[0];
        Accessor accessor = accessors.get(accessorIndex);
        if (accessor.view < 0 || accessor.view >= views.size()) return new float[0];
        BufferView view = views.get(accessor.view);
        int stride = view.stride > 0 ? view.stride : components * componentSize(accessor.componentType);
        ByteBuffer data = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        float[] result = new float[accessor.count * components];
        for (int i = 0; i < accessor.count; i++) {
            int base = view.offset + accessor.offset + i * stride;
            for (int c = 0; c < components; c++) {
                result[i * components + c] = switch (accessor.componentType) {
                    case 5126 -> data.getFloat(base + c * 4);
                    case 5125 -> data.getInt(base + c * 4);
                    case 5123 -> data.getShort(base + c * 2);
                    case 5121 -> data.get(base + c) & 0xFF;
                    default -> 0.0f;
                };
            }
        }
        return result;
    }

    private static int[] readIndexAccessor(byte[] bin, List<BufferView> views, List<Accessor> accessors, int accessorIndex, int vertexCount) {
        if (accessorIndex < 0 || accessorIndex >= accessors.size()) {
            int[] sequential = new int[vertexCount];
            for (int i = 0; i < sequential.length; i++) sequential[i] = i;
            return sequential;
        }
        Accessor accessor = accessors.get(accessorIndex);
        if (accessor.view < 0 || accessor.view >= views.size()) return new int[0];
        BufferView view = views.get(accessor.view);
        ByteBuffer data = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        int[] result = new int[accessor.count];
        int stride = view.stride > 0 ? view.stride : componentSize(accessor.componentType);
        for (int i = 0; i < accessor.count; i++) {
            int base = view.offset + accessor.offset + i * stride;
            result[i] = switch (accessor.componentType) {
                case 5121 -> data.get(base) & 0xFF;
                case 5123 -> data.getShort(base) & 0xFFFF;
                case 5125 -> data.getInt(base);
                default -> 0;
            };
        }
        return result;
    }

    private static int[] readJointAccessor(byte[] bin, List<BufferView> views, List<Accessor> accessors, int accessorIndex, int vertexCount) {
        if (accessorIndex < 0 || accessorIndex >= accessors.size()) {
            return new int[vertexCount * 4];
        }
        Accessor accessor = accessors.get(accessorIndex);
        if (accessor.view < 0 || accessor.view >= views.size()) return new int[0];
        BufferView view = views.get(accessor.view);
        ByteBuffer data = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        int[] result = new int[accessor.count * 4];
        int stride = view.stride > 0 ? view.stride : componentSize(accessor.componentType) * 4;
        for (int i = 0; i < accessor.count; i++) {
            int base = view.offset + accessor.offset + i * stride;
            for (int c = 0; c < 4; c++) {
                result[i * 4 + c] = switch (accessor.componentType) {
                    case 5121 -> data.get(base + c) & 0xFF;
                    case 5123 -> data.getShort(base + c * 2) & 0xFFFF;
                    case 5125 -> data.getInt(base + c * 4);
                    default -> 0;
                };
            }
        }
        return result;
    }

    private static int componentSize(int componentType) {
        return switch (componentType) {
            case 5121 -> 1;
            case 5123 -> 2;
            case 5125, 5126 -> 4;
            default -> 4;
        };
    }

    private static float[] array(JsonObject object, String key, int size, float fallback) {
        float[] result = new float[size];
        for (int i = 0; i < size; i++) result[i] = fallback;
        JsonArray array = object.getAsJsonArray(key);
        if (array != null) {
            for (int i = 0; i < size && i < array.size(); i++) {
                result[i] = array.get(i).getAsFloat();
            }
        }
        return result;
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) ? object.get(key).getAsString() : fallback;
    }

    private static String safeKey(String key) {
        return key.replace('/', '_').replace('\\', '_').replace('.', '_').replace(':', '_');
    }

    private record BufferView(int offset, int length, int stride) {
    }

    private record Accessor(int view, int offset, int componentType, int count, String type) {
    }

    private static final class Mesh {
        private final List<Primitive> primitives = new ArrayList<>();
    }

    private record Primitive(float[] positions, float[] normals, float[] uvs, int[] joints, float[] weights, int[] indices, int material) {
    }

    private record MaterialInfo(Identifier texture, float r, float g, float b, float a, boolean doubleSided, String alphaMode) {
    }

    private static final class Skin {
        private final int[] joints;
        private final Matrix4f[] inverseBind;

        private Skin(int[] joints, Matrix4f[] inverseBind) {
            this.joints = joints;
            this.inverseBind = inverseBind;
        }

        private static Skin empty() {
            return new Skin(new int[0], new Matrix4f[0]);
        }

        private Matrix4f jointMatrix(int jointIndex, Matrix4f[] localWorld, Matrix4f inverseMeshTransform) {
            if (jointIndex < 0 || jointIndex >= joints.length) {
                return new Matrix4f();
            }
            int nodeIndex = joints[jointIndex];
            if (nodeIndex < 0 || nodeIndex >= localWorld.length || localWorld[nodeIndex] == null) {
                return new Matrix4f();
            }
            Matrix4f matrix = new Matrix4f(inverseMeshTransform).mul(localWorld[nodeIndex]);
            if (jointIndex < inverseBind.length && inverseBind[jointIndex] != null) {
                matrix.mul(inverseBind[jointIndex]);
            }
            return matrix;
        }
    }

    private static final class Node {
        private int index;
        private String name = "";
        private int mesh = -1;
        private int skin = -1;
        private Matrix4f local = new Matrix4f();
        private Bounds bounds;
        private final List<Integer> children = new ArrayList<>();
    }

    private static final class Bounds {
        private boolean valid;
        private float minX;
        private float minY;
        private float minZ;
        private float maxX;
        private float maxY;
        private float maxZ;

        private void include(Vector3f p) {
            if (!valid) {
                minX = maxX = p.x;
                minY = maxY = p.y;
                minZ = maxZ = p.z;
                valid = true;
                return;
            }
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
    }

    private static final class TextureManagerFallback {
        private static final Identifier WHITE = Identifier.of("minecraft", "textures/block/white_concrete.png");
    }
}
