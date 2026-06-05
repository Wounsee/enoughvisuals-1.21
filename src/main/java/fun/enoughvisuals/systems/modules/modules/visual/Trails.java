package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Register(name = "Trails", id = "trails", description = "Красивые светящиеся следы за игроком", category = Category.VISUAL)
public class Trails extends Module {

    private static final String SELF = "Только я";
    private static final String PLAYERS = "Игроки";

    private static Trails INSTANCE;

    public final ModeSetting targets = add(new ModeSetting(
            "Цели",
            "targets",
            "Кому рисовать след",
            SELF,
            SELF,
            PLAYERS
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Цвет",
            "color",
            "Цвет следа",
            0xFF55E7FF
    ));

    public final BooleanSetting rainbow = add(new BooleanSetting(
            "Радуга",
            "rainbow",
            "Плавный радужный цвет",
            false
    ));

    public final SliderSetting lifetime = add(new SliderSetting(
            "Время",
            "lifetime",
            "Сколько секунд живет след",
            1.45,
            0.25,
            5.0,
            0.05,
            "s"
    ));

    public final SliderSetting width = add(new SliderSetting(
            "Толщина",
            "width",
            "Толщина яркой линии",
            2.2,
            1.0,
            8.0,
            0.1
    ));

    public final SliderSetting glowWidth = add(new SliderSetting(
            "Свечение",
            "glow_width",
            "Ширина мягкого свечения",
            7.0,
            1.0,
            18.0,
            0.5
    ));

    public final SliderSetting height = add(new SliderSetting(
            "Высота",
            "height",
            "Смещение следа по высоте",
            0.08,
            -0.3,
            1.2,
            0.01,
            "m"
    ));

    public final SliderSetting minStep = add(new SliderSetting(
            "Шаг",
            "min_step",
            "Минимальная дистанция между точками следа",
            0.075,
            0.02,
            0.35,
            0.005,
            "m"
    ));

    public final BooleanSetting throughWalls = add(new BooleanSetting(
            "Через стены",
            "through_walls",
            "Рисовать след сквозь блоки",
            false
    ));

    private final Map<Integer, ArrayDeque<TrailPoint>> trails = new HashMap<>();

    public Trails() {
        INSTANCE = this;
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    @Override
    protected void onDisable() {
        trails.clear();
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            trails.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Set<Integer> alive = new HashSet<>();

        if (targets.is(SELF)) {
            addPoint(mc.player, now, alive);
        } else {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == null || player.isRemoved()) continue;
                addPoint(player, now, alive);
            }
        }

        long lifeMs = (long) (lifetime.get() * 1000.0);
        trails.entrySet().removeIf(entry -> {
            ArrayDeque<TrailPoint> points = entry.getValue();
            points.removeIf(point -> now - point.timeMs > lifeMs);
            return points.isEmpty() || (!alive.contains(entry.getKey()) && points.size() < 2);
        });
    }

    private void addPoint(PlayerEntity player, long now, Set<Integer> alive) {
        int id = player.getId();
        alive.add(id);

        ArrayDeque<TrailPoint> points = trails.computeIfAbsent(id, ignored -> new ArrayDeque<>());
        Vec3d pos = player.getPos().add(0.0, height.get(), 0.0);
        TrailPoint last = points.peekLast();
        if (last != null && last.pos.squaredDistanceTo(pos) < minStep.get() * minStep.get()) {
            return;
        }

        points.addLast(new TrailPoint(pos, now));
        while (points.size() > 120) {
            points.removeFirst();
        }
    }

    private void render(WorldRenderContext context) {
        if (trails.isEmpty()) return;

        long now = System.currentTimeMillis();
        Vec3d cam = context.camera().getPos();
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }

        drawLines(matrix, cam, now, glowWidth.get().floatValue(), 0.22f);
        drawLines(matrix, cam, now, width.get().floatValue(), 0.86f);

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void drawLines(Matrix4f matrix, Vec3d cam, long now, float lineWidth, float alphaMultiplier) {
        RenderSystem.lineWidth(Math.max(1.0f, lineWidth));
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        long lifeMs = (long) (lifetime.get() * 1000.0);
        int segmentIndex = 0;
        for (ArrayDeque<TrailPoint> points : trails.values()) {
            TrailPoint previous = null;
            for (TrailPoint point : points) {
                if (previous != null) {
                    float previousAlpha = alphaFor(previous, now, lifeMs) * alphaMultiplier;
                    float currentAlpha = alphaFor(point, now, lifeMs) * alphaMultiplier;
                    if (previousAlpha > 0.004f || currentAlpha > 0.004f) {
                        int c1 = colorFor(segmentIndex);
                        int c2 = colorFor(segmentIndex + 1);
                        vertex(buffer, matrix, previous.pos, cam, c1, previousAlpha);
                        vertex(buffer, matrix, point.pos, cam, c2, currentAlpha);
                    }
                }
                previous = point;
                segmentIndex++;
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private float alphaFor(TrailPoint point, long now, long lifeMs) {
        float age = (now - point.timeMs) / (float) Math.max(1L, lifeMs);
        return MathHelper.clamp(1.0f - age, 0.0f, 1.0f);
    }

    private int colorFor(int index) {
        if (!rainbow.get()) return color.get() & 0x00FFFFFF;
        float hue = ((System.currentTimeMillis() % 5500L) / 5500f + index * 0.018f) % 1.0f;
        return java.awt.Color.HSBtoRGB(hue, 0.62f, 1.0f) & 0x00FFFFFF;
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3d pos, Vec3d cam, int rgb, float alpha) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        buffer.vertex(matrix, (float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z)).color(r, g, b, alpha);
    }

    private record TrailPoint(Vec3d pos, long timeMs) {
    }
}
