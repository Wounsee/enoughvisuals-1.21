package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import fun.enoughvisuals.systems.webpanel.WebBrowserBridge;
import fun.enoughvisuals.systems.webpanel.WebUrlScreen;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Premium Apple Vision style spatial web browser module.
 * Features curved 3D glass panels, live Chromium rendering,
 * browser controls (Back, Forward, Refresh, Address), and fluid interaction.
 */
@Register(
        name = "WebPanels",
        id = "web_panels",
        description = "Плавающие окна браузера в мире (Apple Vision)",
        category = Category.VISUAL
)
public class WebPanels extends Module {

    private static final int MAX_PANELS = 3;
    private static final double MIN_HW = 0.65, MAX_HW = 3.4;
    private static final double MIN_HH = 0.45, MAX_HH = 2.2;

    private static WebPanels instance;

    private final StringSetting defaultUrl = add(new StringSetting(
            "Стартовый сайт", "default_url", "URL открываемый при создании окна", "https://www.google.com"));
    private final BindSetting createBind = add(new BindSetting(
            "Создать окно", "create_bind", "Клавиша для создания нового окна", GLFW.GLFW_KEY_UNKNOWN));
    private final SliderSetting curveSetting = add(new SliderSetting(
            "Изгиб", "curve", "Изогнутость экрана (Apple Vision стиль)", 0.12f, 0.0f, 0.5f, 0.02f));

    private final List<Panel> panels = new ArrayList<>();
    private int nextId = 1;
    private Panel grabbed;
    private int grabMode; // 1 = move, 2 = resize
    private double grabDist;
    private boolean prevRight;
    private boolean prevLeft;
    private boolean hovering;
    private Panel contentPanel;
    private double[] contentHit;
    private Panel focusedPanel;
    private final Set<Integer> capturedKeys = new HashSet<>();
    private boolean prevCreateHeld;

    public WebPanels() {
        instance = this;
    }

    public static WebPanels getInstance() {
        return instance;
    }

    public boolean isHovering() {
        return isEnabled() && hovering;
    }

    public boolean isMoving() {
        return grabbed != null && grabMode == 1;
    }

    public void scroll(double yOffset) {
        if (isMoving()) {
            grabDist = MathHelper.clamp(grabDist + yOffset * 0.45, 1.2, 16.0);
        } else if (isHoveringContent()) {
            forwardScroll(yOffset);
        }
    }

    public boolean isHoveringContent() {
        return isEnabled() && contentPanel != null && contentPanel.browser != null && grabbed == null;
    }

    public void forwardScroll(double yOffset) {
        if (contentPanel != null && contentPanel.browser != null && contentHit != null) {
            int[] px = pixel(contentPanel, contentHit);
            WebBrowserBridge.mouseWheel(contentPanel.browser, px[0], px[1], yOffset * 40.0);
        }
    }

    public boolean isBrowserFocused() {
        return isEnabled() && focusedPanel != null && focusedPanel.browser != null;
    }

    public boolean forwardKeySafe(int key, int scancode, int mods, int action) {
        if (focusedPanel == null || focusedPanel.browser == null) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            exitFocus();
            return true;
        }
        if (action == GLFW.GLFW_RELEASE) {
            if (capturedKeys.remove(key)) {
                WebBrowserBridge.keyRelease(focusedPanel.browser, key, scancode, mods);
                return true;
            }
            return false;
        }
        capturedKeys.add(key);
        WebBrowserBridge.keyPress(focusedPanel.browser, key, scancode, mods);
        return true;
    }

    public void exitFocus() {
        if (focusedPanel != null && focusedPanel.browser != null) {
            for (int k : capturedKeys) {
                WebBrowserBridge.keyRelease(focusedPanel.browser, k, 0, 0);
            }
        }
        capturedKeys.clear();
        focusedPanel = null;
    }

    public void forwardChar(char chr, int mods) {
        if (focusedPanel != null && focusedPanel.browser != null) {
            WebBrowserBridge.keyTyped(focusedPanel.browser, chr, mods);
        }
    }

    private int[] pixel(Panel p, double[] h) {
        int px = (int) ((h[0] / p.halfW * 0.5 + 0.5) * p.texW);
        int py = (int) ((1.0 - (h[1] / p.halfH * 0.5 + 0.5)) * p.texH);
        px = Math.max(0, Math.min(p.texW - 1, px));
        py = Math.max(0, Math.min(p.texH - 1, py));
        return new int[]{px, py};
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            setEnabled(false);
            return;
        }
        if (panels.isEmpty()) {
            spawnPanel();
        }
    }

    @Override
    public void onDisable() {
        for (Panel p : panels) {
            disposePanel(p);
        }
        panels.clear();
        grabbed = null;
        focusedPanel = null;
        contentPanel = null;
    }

    @Override
    public void onTick() {
        if (createBind.isBound()) {
            boolean held = isHeld(createBind.get());
            if (held && !prevCreateHeld) {
                spawnPanel();
            }
            prevCreateHeld = held;
        }
    }

    public void spawnPanel() {
        if (mc.player == null || activeCount() >= MAX_PANELS) {
            return;
        }
        Vec3d eye = mc.player.getEyePos();
        Vec3d fwd = mc.player.getRotationVec(1.0F).normalize();
        Panel p = new Panel();
        p.id = nextId++;
        p.center = eye.add(fwd.multiply(3.0));
        Vec3d worldUp = new Vec3d(0, 1, 0);
        p.right = fwd.crossProduct(worldUp).normalize();
        if (p.right.lengthSquared() < 1e-6) {
            p.right = new Vec3d(1, 0, 0);
        }
        p.up = p.right.crossProduct(fwd).normalize();
        p.halfW = 1.35;
        p.halfH = 0.78;
        p.url = defaultUrl.get();
        panels.add(p);
    }

    private int activeCount() {
        int n = 0;
        for (Panel p : panels) {
            if (!p.closing) n++;
        }
        return n;
    }

    private void openAddress(Panel p) {
        mc.setScreen(new WebUrlScreen(p.url, url -> {
            p.url = url;
            if (p.browser != null) {
                WebBrowserBridge.loadUrl(p.browser, url);
            }
        }));
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (instance == null || !instance.isEnabled() || instance.panels.isEmpty()) return;
        if (mc.player == null || mc.world == null || context.camera() == null || context.matrixStack() == null) return;
        instance.renderPanels(context);
    }

    private void renderPanels(WorldRenderContext context) {
        Vec3d cam = context.camera().getPos();
        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1f;
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(tickDelta).normalize();
        handleInput(eye, look);

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        // Browser upkeep
        for (Panel p : panels) {
            if (!p.closing) {
                ensureBrowser(p);
            }
        }

        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);

        try {
            // Draw panel chrome / controls
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder colorBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Panel p : panels) {
                p.anim += ((p.closing ? 0f : 1f) - p.anim) * 0.22f;
                drawPanelChrome(colorBuf, matrix, cam, p, hit(p, eye, look));
            }
            BufferRenderer.drawWithGlobalProgram(colorBuf.end());

            // Draw browser content (Chromium frame texture)
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            for (Panel p : panels) {
                int glTex = p.browser != null ? WebBrowserBridge.textureId(p.browser) : 0;
                if (glTex > 0) {
                    GlStateManager._bindTexture(glTex);
                    BufferBuilder texBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    drawContent(texBuf, matrix, cam, p);
                    BufferRenderer.drawWithGlobalProgram(texBuf.end());
                }
            }
        } finally {
            RenderSystem.depthMask(oldDepthMask);
            RenderSystem.depthFunc(oldDepthFunc);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        panels.removeIf(p -> {
            if (p.closing && p.anim < 0.04f) {
                disposePanel(p);
                return true;
            }
            return false;
        });
    }

    private void ensureBrowser(Panel p) {
        if (p.browser != null || !WebBrowserBridge.available()) {
            return;
        }
        p.texW = 1920;
        p.texH = 1080;
        p.browser = WebBrowserBridge.create(p.url, p.texW, p.texH);
    }

    private void disposePanel(Panel p) {
        if (p.browser != null) {
            WebBrowserBridge.close(p.browser);
            p.browser = null;
        }
    }

    private void drawContent(BufferBuilder buffer, Matrix4f m, Vec3d cam, Panel p) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        double hw = p.halfW * (0.6 + 0.4 * a) - 0.02;
        double hh = p.halfH * (0.6 + 0.4 * a) - 0.02;
        double dBase = 0.0035;
        double curve = curveSetting.get() * hw;
        float alpha = a;
        int cols = 20;
        for (int i = 0; i < cols; i++) {
            double u0 = -hw + 2 * hw * i / cols;
            double u1 = -hw + 2 * hw * (i + 1) / cols;
            double d0 = dBase + curve * (u0 / hw) * (u0 / hw);
            double d1 = dBase + curve * (u1 / hw) * (u1 / hw);
            float tu0 = (float) i / cols, tu1 = (float) (i + 1) / cols;

            texVert(buffer, m, c, p, n, u0, hh, d0, tu0, 0, alpha);
            texVert(buffer, m, c, p, n, u1, hh, d1, tu1, 0, alpha);
            texVert(buffer, m, c, p, n, u1, -hh, d1, tu1, 1, alpha);
            texVert(buffer, m, c, p, n, u0, -hh, d0, tu0, 1, alpha);
        }
    }

    private void handleInput(Vec3d eye, Vec3d look) {
        long win = mc.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean pressEdge = down && !prevRight;

        if (grabbed != null && down && !grabbed.closing) {
            double[] h = hit(grabbed, eye, look);
            if (grabMode == 2 && h != null) {
                grabbed.halfW = MathHelper.clamp(Math.abs(h[0]) - 0.12, MIN_HW, MAX_HW);
                grabbed.halfH = MathHelper.clamp(Math.abs(h[1]) - 0.12, MIN_HH, MAX_HH);
            } else if (grabMode == 1) {
                grabbed.center = eye.add(look.multiply(grabDist));
                Vec3d toEye = eye.subtract(grabbed.center);
                if (toEye.lengthSquared() > 1e-6) {
                    Vec3d fwd = toEye.normalize().multiply(-1.0);
                    Vec3d worldUp = new Vec3d(0, 1, 0);
                    Vec3d right = fwd.crossProduct(worldUp);
                    if (right.lengthSquared() > 1e-6) {
                        grabbed.right = right.normalize();
                        grabbed.up = grabbed.right.crossProduct(fwd).normalize();
                    }
                }
            }
        } else {
            grabbed = null;
        }

        if (pressEdge && grabbed == null) {
            for (int i = panels.size() - 1; i >= 0; i--) {
                Panel p = panels.get(i);
                if (p.closing) continue;
                double[] h = hit(p, eye, look);
                if (h == null) continue;
                int region = region(p, h[0], h[1]);
                if (region == 3) {
                    p.closing = true;
                    break;
                } else if (region == 2) {
                    grabbed = p;
                    grabMode = 2;
                    break;
                } else if (region == 1) {
                    grabbed = p;
                    grabMode = 1;
                    grabDist = p.center.distanceTo(eye);
                    break;
                } else if (region == 5) {
                    openAddress(p);
                    break;
                } else if (region == 6) { // Back
                    if (p.browser != null) WebBrowserBridge.goBack(p.browser);
                    break;
                } else if (region == 7) { // Forward
                    if (p.browser != null) WebBrowserBridge.goForward(p.browser);
                    break;
                } else if (region == 8) { // Reload
                    if (p.browser != null) WebBrowserBridge.reload(p.browser);
                    break;
                }
            }
        }
        prevRight = down;

        hovering = false;
        contentPanel = null;
        contentHit = null;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.closing) continue;
            double[] h = hit(p, eye, look);
            if (h == null) continue;
            int region = region(p, h[0], h[1]);
            if (region != -1) {
                hovering = true;
            }
            if (region == 0 && p.browser != null && contentPanel == null) {
                contentPanel = p;
                contentHit = h;
            }
        }

        boolean left = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (contentPanel != null && grabbed == null) {
            int[] px = pixel(contentPanel, contentHit);
            WebBrowserBridge.mouseMove(contentPanel.browser, px[0], px[1]);
            if (left && !prevLeft) {
                WebBrowserBridge.mousePress(contentPanel.browser, px[0], px[1], 0);
                focusedPanel = contentPanel;
            } else if (!left && prevLeft) {
                WebBrowserBridge.mouseRelease(contentPanel.browser, px[0], px[1], 0);
            }
        }
        prevLeft = left;

        if (focusedPanel != null && focusedPanel.closing) {
            exitFocus();
        }
    }

    private double[] hit(Panel p, Vec3d eye, Vec3d look) {
        Vec3d n = p.right.crossProduct(p.up).normalize();
        double denom = look.dotProduct(n);
        if (Math.abs(denom) < 1e-6) return null;
        double t = p.center.subtract(eye).dotProduct(n) / denom;
        if (t <= 0 || t > 64) return null;
        Vec3d hp = eye.add(look.multiply(t)).subtract(p.center);
        double lu = hp.dotProduct(p.right);
        double lv = hp.dotProduct(p.up);
        if (Math.abs(lu) > p.halfW + 0.6 || Math.abs(lv) > p.halfH + 0.4) return null;
        return new double[]{lu, lv};
    }

    private double closeU(Panel p) {
        return p.halfW * 0.32 + 0.16;
    }

    private double closeV(Panel p) {
        return -p.halfH - 0.10 - 0.025;
    }

    private double resizeU(Panel p) {
        return p.halfW + 0.12;
    }

    private double resizeV(Panel p) {
        return -p.halfH - 0.12;
    }

    private int region(Panel p, double lu, double lv) {
        double cu = closeU(p), cv = closeV(p), cr = 0.08;
        if (lu > cu - cr && lu < cu + cr && lv > cv - cr && lv < cv + cr) {
            return 3; // close
        }
        double ru = resizeU(p), rv = resizeV(p), rr = 0.10;
        if (lu > ru - rr && lu < ru + rr && lv > rv - rr && lv < rv + rr) {
            return 2; // resize handle
        }

        // Top bar items
        if (lv > p.halfH + 0.04 && lv < p.halfH + 0.22) {
            double abHalfW = p.halfW * 0.55;
            double backU = -abHalfW + 0.10;
            double fwdU = -abHalfW + 0.25;
            double reloadU = -abHalfW + 0.40;

            if (Math.abs(lu - backU) < 0.06) return 6; // Back
            if (Math.abs(lu - fwdU) < 0.06) return 7; // Forward
            if (Math.abs(lu - reloadU) < 0.06) return 8; // Reload
            if (Math.abs(lu) < abHalfW) return 5; // Address / Search bar
        }

        // Bottom grab bar
        if (lv < -p.halfH - 0.04 && lv > -p.halfH - 0.20 && Math.abs(lu) < p.halfW * 0.36) {
            return 1; // grab bar
        }
        if (Math.abs(lu) <= p.halfW && Math.abs(lv) <= p.halfH) {
            return 0; // content
        }
        return -1;
    }

    private void drawPanelChrome(BufferBuilder buf, Matrix4f m, Vec3d cam, Panel p, double[] h) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        if (a <= 0.01f) return;
        double hw = p.halfW * (0.6 + 0.4 * a);
        double hh = p.halfH * (0.6 + 0.4 * a);

        // Body dark frosted glass panel
        fillRound(buf, m, c, p, n, 1, 0, 0, hw + 0.03, hh + 0.03, 0.12, 0x10, 0x12, 0x18, (int) (220 * a));

        // Subtle glowing accent border around the whole panel
        int accent = MenuScreen.ACCENT;
        int ar = (accent >> 16) & 0xFF;
        int ag = (accent >> 8) & 0xFF;
        int ab = accent & 0xFF;
        fillOutline(buf, m, c, p, n, 2, 0, 0, hw + 0.03, hh + 0.03, 0.12, 0.008, ar, ag, ab, (int) (120 * a));

        // Bottom grabber bar (sleek frosted glass capsule)
        boolean overBar = h != null && (region(p, h[0], h[1]) == 1 || region(p, h[0], h[1]) == 3);
        double barH = 0.045, barGap = 0.09, barHalfW = hw * 0.32;
        fillRound(buf, m, c, p, n, 4, 0, -hh - barGap - barH, barHalfW, barH, barH,
                0x1A, 0x1D, 0x26, (int) (230 * a));
        fillRound(buf, m, c, p, n, 5, 0, -hh - barGap - barH, barHalfW * 0.45, barH * 0.28, barH * 0.28,
                ar, ag, ab, (int) ((overBar ? 255 : 180) * a));

        // Close button circle near the bar (revealed on hover)
        if (overBar) {
            boolean overClose = region(p, h[0], h[1]) == 3;
            double cu = closeU(p), cv = closeV(p);
            fillRound(buf, m, c, p, n, 5, cu, cv, 0.065, 0.065, 0.065,
                    overClose ? 255 : 220, overClose ? 60 : 75, overClose ? 60 : 75, (int) (245 * a));
        }

        // Resize grip handle on bottom-right corner
        boolean overGrip = h != null && region(p, h[0], h[1]) == 2;
        double gu = resizeU(p), gv = resizeV(p), gr = 0.065;
        fillRound(buf, m, c, p, n, 5, gu, gv, gr, gr, gr,
                0x1A, 0x1D, 0x26, (int) (230 * a));
        fillRound(buf, m, c, p, n, 6, gu, gv, gr * 0.5, gr * 0.5, gr * 0.5,
                255, 255, 255, (int) ((overGrip ? 255 : 180) * a));

        // Top Navigation & Address Bar (Capsule)
        boolean overAddr = h != null && region(p, h[0], h[1]) >= 5;
        double abH = 0.05, abGap = 0.09, abHalfW = hw * 0.55;
        fillRound(buf, m, c, p, n, 4, 0, hh + abGap + abH, abHalfW, abH, abH,
                0x15, 0x18, 0x22, (int) (240 * a));
        fillOutline(buf, m, c, p, n, 5, 0, hh + abGap + abH, abHalfW, abH, abH, 0.005,
                ar, ag, ab, (int) ((overAddr ? 200 : 100) * a));

        // Back / Forward / Refresh indicators inside top bar
        double backU = -abHalfW + 0.10;
        double fwdU = -abHalfW + 0.25;
        double reloadU = -abHalfW + 0.40;
        double navY = hh + abGap + abH;

        boolean overBack = h != null && region(p, h[0], h[1]) == 6;
        boolean overFwd = h != null && region(p, h[0], h[1]) == 7;
        boolean overReload = h != null && region(p, h[0], h[1]) == 8;

        fillRound(buf, m, c, p, n, 6, backU, navY, 0.035, 0.035, 0.035,
                ar, ag, ab, (int) ((overBack ? 255 : 150) * a));
        fillRound(buf, m, c, p, n, 6, fwdU, navY, 0.035, 0.035, 0.035,
                ar, ag, ab, (int) ((overFwd ? 255 : 150) * a));
        fillRound(buf, m, c, p, n, 6, reloadU, navY, 0.035, 0.035, 0.035,
                ar, ag, ab, (int) ((overReload ? 255 : 150) * a));
    }

    private void fillRound(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n, int layer,
                           double cu, double cv, double hw, double hh, double cr, int r, int g, int b, int a) {
        if (a <= 0) return;
        double d = layer * 0.0015;
        cr = Math.min(cr, Math.min(hw, hh));
        int seg = 5;
        double[][] corners = {
                {hw - cr, hh - cr, 0}, {-hw + cr, hh - cr, 90}, {-hw + cr, -hh + cr, 180}, {hw - cr, -hh + cr, 270}
        };
        List<double[]> pts = new ArrayList<>();
        for (double[] cn : corners) {
            for (int i = 0; i <= seg; i++) {
                double ang = Math.toRadians(cn[2] + 90.0 * i / seg);
                pts.add(new double[]{cu + cn[0] + cr * Math.cos(ang), cv + cn[1] + cr * Math.sin(ang)});
            }
        }
        int sz = pts.size();
        for (int i = 0; i < sz; i++) {
            double[] a0 = pts.get(i);
            double[] a1 = pts.get((i + 1) % sz);
            colorVert(buf, m, c, p, n, cu, cv, d, r, g, b, a);
            colorVert(buf, m, c, p, n, a0[0], a0[1], d, r, g, b, a);
            colorVert(buf, m, c, p, n, a1[0], a1[1], d, r, g, b, a);
            colorVert(buf, m, c, p, n, a1[0], a1[1], d, r, g, b, a);
        }
    }

    private void fillOutline(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n, int layer,
                            double cu, double cv, double hw, double hh, double cr, double thickness, int r, int g, int b, int a) {
        if (a <= 0) return;
        double d = layer * 0.0015;
        cr = Math.min(cr, Math.min(hw, hh));
        int seg = 4;
        double[][] corners = {
                {hw - cr, hh - cr, 0}, {-hw + cr, hh - cr, 90}, {-hw + cr, -hh + cr, 180}, {hw - cr, -hh + cr, 270}
        };
        List<double[]> outer = new ArrayList<>();
        List<double[]> inner = new ArrayList<>();
        for (double[] cn : corners) {
            for (int i = 0; i <= seg; i++) {
                double ang = Math.toRadians(cn[2] + 90.0 * i / seg);
                double cos = Math.cos(ang), sin = Math.sin(ang);
                outer.add(new double[]{cu + cn[0] + cr * cos, cv + cn[1] + cr * sin});
                inner.add(new double[]{cu + cn[0] + (cr - thickness) * cos, cv + cn[1] + (cr - thickness) * sin});
            }
        }
        int sz = outer.size();
        for (int i = 0; i < sz; i++) {
            double[] o0 = outer.get(i), o1 = outer.get((i + 1) % sz);
            double[] i0 = inner.get(i), i1 = inner.get((i + 1) % sz);
            colorVert(buf, m, c, p, n, o0[0], o0[1], d, r, g, b, a);
            colorVert(buf, m, c, p, n, o1[0], o1[1], d, r, g, b, a);
            colorVert(buf, m, c, p, n, i1[0], i1[1], d, r, g, b, a);
            colorVert(buf, m, c, p, n, i0[0], i0[1], d, r, g, b, a);
        }
    }

    private void colorVert(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                           double u, double v, double d, int r, int g, int b, int a) {
        double x = c.x + p.right.x * u + p.up.x * v + n.x * d;
        double y = c.y + p.right.y * u + p.up.y * v + n.y * d;
        double z = c.z + p.right.z * u + p.up.z * v + n.z * d;
        buf.vertex(m, (float) x, (float) y, (float) z).color(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    private void texVert(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                         double u, double v, double d, float texU, float texV, float alpha) {
        double x = c.x + p.right.x * u + p.up.x * v + n.x * d;
        double y = c.y + p.right.y * u + p.up.y * v + n.y * d;
        double z = c.z + p.right.z * u + p.up.z * v + n.z * d;
        buf.vertex(m, (float) x, (float) y, (float) z).texture(texU, texV).color(1f, 1f, 1f, alpha);
    }

    private static float ease(float t) {
        return t * t * (3f - 2f * t);
    }

    private static boolean isHeld(int bind) {
        if (mc.getWindow() == null || bind == BindKeyUtil.NONE) return false;
        long window = mc.getWindow().getHandle();
        if (bind >= 1000) {
            return GLFW.glfwGetMouseButton(window, bind - 1000) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(window, bind) == GLFW.GLFW_PRESS;
    }

    private static final class Panel {
        int id;
        Vec3d center = Vec3d.ZERO;
        Vec3d right = new Vec3d(1, 0, 0);
        Vec3d up = new Vec3d(0, 1, 0);
        double halfW, halfH;
        String url = "";
        float anim;
        boolean closing;
        Object browser;
        int texW, texH;
    }
}
