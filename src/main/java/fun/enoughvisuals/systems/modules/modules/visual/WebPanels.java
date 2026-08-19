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
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
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
        WebBrowserBridge.requestInit();
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
        WebBrowserBridge.requestInit();
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
        Matrix4f proj = context.projectionMatrix();

        // Browser upkeep
        for (Panel p : panels) {
            if (!p.closing) {
                ensureBrowser(p);
            }
        }

        // Capture in-world blur frame if theme blur enabled
        if (!CustomTheme.blurDisabled()) {
            GlassUtil.beginFrame();
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
            // 1. Draw In-World Real Frosted Glass Blur (from blurred world framebuffer)
            int blurTex = GlassUtil.getResultTexture();
            if (blurTex > 0) {
                GlStateManager._bindTexture(blurTex);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder blurBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Panel p : panels) {
                    if (p.anim > 0.01f) {
                        drawCurvedFrostedBlur(blurBuf, matrix, proj, cam, p);
                    }
                }
                BuiltBuffer builtBlur = blurBuf.endNullable();
                if (builtBlur != null) {
                    BufferRenderer.drawWithGlobalProgram(builtBlur);
                }
            }

            // 2. Draw panel chrome / glass tint / borders / controls / vector icons
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder colorBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Panel p : panels) {
                p.anim += ((p.closing ? 0f : 1f) - p.anim) * 0.22f;
                if (p.anim > 0.01f) {
                    drawPanelChrome(colorBuf, matrix, cam, p, hit(p, eye, look));
                }
            }
            BuiltBuffer builtColor = colorBuf.endNullable();
            if (builtColor != null) {
                BufferRenderer.drawWithGlobalProgram(builtColor);
            }

            // 3. Draw browser content (Chromium frame texture)
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            for (Panel p : panels) {
                int glTex = p.browser != null ? WebBrowserBridge.textureId(p.browser) : 0;
                if (glTex > 0 && p.anim > 0.01f) {
                    GlStateManager._bindTexture(glTex);
                    BufferBuilder texBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    drawContent(texBuf, matrix, cam, p);
                    BuiltBuffer builtTex = texBuf.endNullable();
                    if (builtTex != null) {
                        BufferRenderer.drawWithGlobalProgram(builtTex);
                    }
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
        if (p.browser != null || !WebBrowserBridge.isReady()) {
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

    private void drawCurvedFrostedBlur(BufferBuilder buffer, Matrix4f m, Matrix4f proj, Vec3d cam, Panel p) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        double hw = p.halfW * (0.6 + 0.4 * a) + 0.04;
        double hh = p.halfH * (0.6 + 0.4 * a) + 0.04;
        double d = 0.001;
        float alpha = 0.92f * a;
        int cols = 28;
        for (int i = 0; i < cols; i++) {
            double u0 = -hw + 2 * hw * i / cols;
            double u1 = -hw + 2 * hw * (i + 1) / cols;
            screenTexVert(buffer, m, proj, c, p, n, u0, hh, d, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u1, hh, d, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u1, -hh, d, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u0, -hh, d, alpha);
        }
    }

    private void drawContent(BufferBuilder buffer, Matrix4f m, Vec3d cam, Panel p) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        double hw = p.halfW * (0.6 + 0.4 * a) - 0.02;
        double hh = p.halfH * (0.6 + 0.4 * a) - 0.02;
        double d = 0.005;
        float alpha = a;
        int cols = 28;
        for (int i = 0; i < cols; i++) {
            double u0 = -hw + 2 * hw * i / cols;
            double u1 = -hw + 2 * hw * (i + 1) / cols;
            float tu0 = (float) i / cols, tu1 = (float) (i + 1) / cols;

            texVert(buffer, m, c, p, n, u0, hh, d, tu0, 0, alpha);
            texVert(buffer, m, c, p, n, u1, hh, d, tu1, 0, alpha);
            texVert(buffer, m, c, p, n, u1, -hh, d, tu1, 1, alpha);
            texVert(buffer, m, c, p, n, u0, -hh, d, tu0, 1, alpha);
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
                if (region == 3 || region == 9) { // Close
                    p.closing = true;
                    break;
                } else if (region == 2) { // Resize
                    grabbed = p;
                    grabMode = 2;
                    break;
                } else if (region == 1) { // Grab / Move
                    grabbed = p;
                    grabMode = 1;
                    grabDist = p.center.distanceTo(eye);
                    break;
                } else if (region == 5) { // Address bar modal
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
        return p.halfW + 0.10;
    }

    private double resizeV(Panel p) {
        return -p.halfH - 0.10;
    }

    private int region(Panel p, double lu, double lv) {
        // Bottom right resize grip
        double ru = resizeU(p), rv = resizeV(p), rr = 0.09;
        if (lu > ru - rr && lu < ru + rr && lv > rv - rr && lv < rv + rr) {
            return 2; // resize handle
        }

        // Bottom grab bar
        if (lv < -p.halfH - 0.04 && lv > -p.halfH - 0.20 && Math.abs(lu) < p.halfW * 0.36) {
            return 1; // grab bar
        }

        // Top bar items
        if (lv > p.halfH + 0.03 && lv < p.halfH + 0.22) {
            double abHalfW = p.halfW * 0.55;
            if (Math.abs(lu) <= abHalfW + 0.05) {
                double backU = -abHalfW + 0.09;
                double fwdU = -abHalfW + 0.20;
                double reloadU = -abHalfW + 0.31;
                double closeTopU = abHalfW - 0.07;

                if (Math.abs(lu - backU) < 0.045) return 6; // Back
                if (Math.abs(lu - fwdU) < 0.045) return 7; // Forward
                if (Math.abs(lu - reloadU) < 0.045) return 8; // Reload
                if (Math.abs(lu - closeTopU) < 0.045) return 9; // Close (top right)
                return 5; // Address / Search pill
            }
        }

        // Close button near bottom bar
        double cu = closeU(p), cv = closeV(p), cr = 0.08;
        if (lu > cu - cr && lu < cu + cr && lv > cv - cr && lv < cv + cr) {
            return 3; // close
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

        int accent = MenuScreen.ACCENT;
        int ar = (accent >> 16) & 0xFF;
        int ag = (accent >> 8) & 0xFF;
        int ab = accent & 0xFF;

        int accentLight = MenuScreen.ACCENT_LIGHT;
        int alr = (accentLight >> 16) & 0xFF;
        int alg = (accentLight >> 8) & 0xFF;
        int alb = accentLight & 0xFF;

        // 1. Frosted Glass Body Tint & Outline
        drawCurvedSlicedPanel(buf, m, c, p, n, 2, 0, 0, hw + 0.03, hh + 0.03, 0x0A, 0x0C, 0x14, (int) (210 * a));
        fillOutline(buf, m, c, p, n, 3, 0, 0, hw + 0.03, hh + 0.03, 0.12, 0.012, ar, ag, ab, (int) (65 * a));
        fillOutline(buf, m, c, p, n, 4, 0, 0, hw + 0.03, hh + 0.03, 0.12, 0.005, alr, alg, alb, (int) (150 * a));

        // Specular top edge shine (frosted glass reflection)
        drawThickLine(buf, m, c, p, n, -hw * 0.75, hh + 0.02, hw * 0.75, hh + 0.02, 0.006, 255, 255, 255, (int) (45 * a));

        // 2. Loading State (If Chromium is initializing or frame is not yet ready)
        int glTex = p.browser != null ? WebBrowserBridge.textureId(p.browser) : 0;
        if (glTex <= 0) {
            float timeSec = (System.currentTimeMillis() % 100000L) / 1000f;
            drawSpinner(buf, m, c, p, n, 0, 0.05, 0.16, timeSec * 1.5f, alr, alg, alb, (int) (235 * a));
            drawGlobe(buf, m, c, p, n, 0, 0.05, 0.08, 255, 255, 255, (int) (185 * a));
            // Loading pill below spinner
            fillRound(buf, m, c, p, n, 5, 0, -0.22, hw * 0.35, 0.035, 0.035, 0x14, 0x17, 0x22, (int) (210 * a));
            fillOutline(buf, m, c, p, n, 6, 0, -0.22, hw * 0.35, 0.035, 0.035, 0.004, ar, ag, ab, (int) (110 * a));
        }

        // 3. Bottom Grab Bar (Apple Vision floating pill)
        boolean overBar = h != null && (region(p, h[0], h[1]) == 1 || region(p, h[0], h[1]) == 3);
        double barH = 0.045, barGap = 0.09, barHalfW = hw * 0.32;
        double barY = -hh - barGap - barH;
        fillRound(buf, m, c, p, n, 4, 0, barY, barHalfW, barH, barH, 0x12, 0x15, 0x20, (int) (235 * a));
        fillOutline(buf, m, c, p, n, 5, 0, barY, barHalfW, barH, barH, 0.005,
                ar, ag, ab, (int) ((overBar ? 230 : 110) * a));

        // 3 Drag Dots in Grab Bar
        double dotGap = 0.06;
        for (int d = -1; d <= 1; d++) {
            fillRound(buf, m, c, p, n, 6, d * dotGap, barY, 0.016, 0.016, 0.016,
                    overBar ? alr : 200, overBar ? alg : 210, overBar ? alb : 230, (int) ((overBar ? 255 : 180) * a));
        }

        // 4. Resize Grip Handle (Bottom-Right Corner)
        boolean overGrip = h != null && region(p, h[0], h[1]) == 2;
        double gu = resizeU(p), gv = resizeV(p), gr = 0.065;
        fillRound(buf, m, c, p, n, 5, gu, gv, gr, gr, gr,
                0x14, 0x17, 0x22, (int) (235 * a));
        fillOutline(buf, m, c, p, n, 6, gu, gv, gr, gr, gr, 0.005,
                ar, ag, ab, (int) ((overGrip ? 240 : 120) * a));
        drawResizeIcon(buf, m, c, p, n, gu, gv, 0.05,
                overGrip ? alr : 220, overGrip ? alg : 225, overGrip ? alb : 240, (int) ((overGrip ? 255 : 180) * a));

        // 5. Top Navigation & Address Bar (Elevated Frosted Glass Capsule)
        double abH = 0.055, abGap = 0.09, abHalfW = hw * 0.55;
        double navY = hh + abGap + abH;
        boolean overTop = h != null && region(p, h[0], h[1]) >= 5;

        fillRound(buf, m, c, p, n, 4, 0, navY, abHalfW, abH, abH,
                0x12, 0x15, 0x20, (int) (240 * a));
        fillOutline(buf, m, c, p, n, 5, 0, navY, abHalfW, abH, abH, 0.005,
                ar, ag, ab, (int) ((overTop ? 230 : 110) * a));

        // Navigation Buttons geometry
        double backU = -abHalfW + 0.09;
        double fwdU = -abHalfW + 0.20;
        double reloadU = -abHalfW + 0.31;
        double closeTopU = abHalfW - 0.07;

        boolean overBack = h != null && region(p, h[0], h[1]) == 6;
        boolean overFwd = h != null && region(p, h[0], h[1]) == 7;
        boolean overReload = h != null && region(p, h[0], h[1]) == 8;
        boolean overClose = h != null && (region(p, h[0], h[1]) == 9 || region(p, h[0], h[1]) == 3);
        boolean overPill = h != null && region(p, h[0], h[1]) == 5;

        // Button circles
        double btnR = 0.040;
        fillRound(buf, m, c, p, n, 6, backU, navY, btnR, btnR, btnR,
                overBack ? 0x2A : 0x1A, overBack ? 0x2E : 0x1D, overBack ? 0x3D : 0x26, (int) (240 * a));
        if (overBack) fillOutline(buf, m, c, p, n, 7, backU, navY, btnR, btnR, btnR, 0.004, alr, alg, alb, (int) (220 * a));
        drawChevron(buf, m, c, p, n, backU, navY, 0.042, false,
                overBack ? alr : 210, overBack ? alg : 215, overBack ? alb : 230, (int) ((overBack ? 255 : 180) * a));

        fillRound(buf, m, c, p, n, 6, fwdU, navY, btnR, btnR, btnR,
                overFwd ? 0x2A : 0x1A, overFwd ? 0x2E : 0x1D, overFwd ? 0x3D : 0x26, (int) (240 * a));
        if (overFwd) fillOutline(buf, m, c, p, n, 7, fwdU, navY, btnR, btnR, btnR, 0.004, alr, alg, alb, (int) (220 * a));
        drawChevron(buf, m, c, p, n, fwdU, navY, 0.042, true,
                overFwd ? alr : 210, overFwd ? alg : 215, overFwd ? alb : 230, (int) ((overFwd ? 255 : 180) * a));

        fillRound(buf, m, c, p, n, 6, reloadU, navY, btnR, btnR, btnR,
                overReload ? 0x2A : 0x1A, overReload ? 0x2E : 0x1D, overReload ? 0x3D : 0x26, (int) (240 * a));
        if (overReload) fillOutline(buf, m, c, p, n, 7, reloadU, navY, btnR, btnR, btnR, 0.004, alr, alg, alb, (int) (220 * a));
        drawRefresh(buf, m, c, p, n, reloadU, navY, 0.022,
                overReload ? alr : 210, overReload ? alg : 215, overReload ? alb : 230, (int) ((overReload ? 255 : 180) * a));

        // URL / Domain Pill in center
        double pillLeft = reloadU + 0.07;
        double pillRight = closeTopU - 0.07;
        double pillU = (pillLeft + pillRight) * 0.5;
        double pillHW = (pillRight - pillLeft) * 0.5;
        double pillHH = 0.038;

        fillRound(buf, m, c, p, n, 6, pillU, navY, pillHW, pillHH, pillHH,
                overPill ? 0x1E : 0x16, overPill ? 0x22 : 0x19, overPill ? 0x30 : 0x24, (int) (240 * a));
        fillOutline(buf, m, c, p, n, 7, pillU, navY, pillHW, pillHH, pillHH, 0.004,
                ar, ag, ab, (int) ((overPill ? 220 : 90) * a));

        // Vector Globe icon on left of URL pill
        drawGlobe(buf, m, c, p, n, pillU - pillHW + 0.05, navY, 0.020,
                overPill ? alr : 160, overPill ? alg : 170, overPill ? alb : 190, (int) ((overPill ? 255 : 180) * a));

        // Close button (Red accent circle with X on top-right)
        fillRound(buf, m, c, p, n, 6, closeTopU, navY, btnR, btnR, btnR,
                overClose ? 0xE0 : 0x3A, overClose ? 0x35 : 0x1E, overClose ? 0x45 : 0x24, (int) (245 * a));
        if (overClose) fillOutline(buf, m, c, p, n, 7, closeTopU, navY, btnR, btnR, btnR, 0.004, 0xFF, 0x6B, 0x7A, (int) (230 * a));
        drawCross(buf, m, c, p, n, closeTopU, navY, 0.032,
                255, 255, 255, (int) ((overClose ? 255 : 200) * a));
    }

    // ----------------- Curved & Vector Drawing Helpers in 3D -----------------

    private void drawCurvedSlicedPanel(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n, int layer,
                                      double cu, double cv, double hw, double hh, int r, int g, int b, int a) {
        if (a <= 0) return;
        double d = layer * 0.0015;
        int cols = 28;
        for (int i = 0; i < cols; i++) {
            double u0 = -hw + 2 * hw * i / cols;
            double u1 = -hw + 2 * hw * (i + 1) / cols;
            colorVert(buf, m, c, p, n, u0, hh, d, r, g, b, a);
            colorVert(buf, m, c, p, n, u1, hh, d, r, g, b, a);
            colorVert(buf, m, c, p, n, u1, -hh, d, r, g, b, a);
            colorVert(buf, m, c, p, n, u0, -hh, d, r, g, b, a);
        }
    }

    private void drawChevron(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                             double u, double v, double size, boolean right, int r, int g, int b, int a) {
        double dir = right ? 1.0 : -1.0;
        double x0 = u - dir * size * 0.28, y0 = v + size * 0.40;
        double x1 = u + dir * size * 0.22, y1 = v;
        double x2 = u - dir * size * 0.28, y2 = v - size * 0.40;
        double th = 0.006;
        drawThickLine(buf, m, c, p, n, x0, y0, x1, y1, th, r, g, b, a);
        drawThickLine(buf, m, c, p, n, x1, y1, x2, y2, th, r, g, b, a);
    }

    private void drawCross(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                           double u, double v, double size, int r, int g, int b, int a) {
        double s = size * 0.35;
        double th = 0.0055;
        drawThickLine(buf, m, c, p, n, u - s, v - s, u + s, v + s, th, r, g, b, a);
        drawThickLine(buf, m, c, p, n, u - s, v + s, u + s, v - s, th, r, g, b, a);
    }

    private void drawRefresh(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                            double u, double v, double rad, int r, int g, int b, int a) {
        int segs = 10;
        double startAng = Math.toRadians(35.0);
        double totalAng = Math.toRadians(280.0);
        double th = 0.005;
        for (int i = 0; i < segs; i++) {
            double a0 = startAng + totalAng * i / segs;
            double a1 = startAng + totalAng * (i + 1) / segs;
            double x0 = u + rad * Math.cos(a0), y0 = v + rad * Math.sin(a0);
            double x1 = u + rad * Math.cos(a1), y1 = v + rad * Math.sin(a1);
            drawThickLine(buf, m, c, p, n, x0, y0, x1, y1, th, r, g, b, a);
        }
        double endA = startAng + totalAng;
        double ex = u + rad * Math.cos(endA), ey = v + rad * Math.sin(endA);
        drawThickLine(buf, m, c, p, n, ex, ey, ex + 0.015, ey + 0.010, th, r, g, b, a);
        drawThickLine(buf, m, c, p, n, ex, ey, ex - 0.005, ey + 0.016, th, r, g, b, a);
    }

    private void drawGlobe(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                          double u, double v, double rad, int r, int g, int b, int a) {
        int segs = 12;
        double th = 0.004;
        for (int i = 0; i < segs; i++) {
            double a0 = Math.toRadians(360.0 * i / segs);
            double a1 = Math.toRadians(360.0 * (i + 1) / segs);
            drawThickLine(buf, m, c, p, n, u + rad * Math.cos(a0), v + rad * Math.sin(a0),
                    u + rad * Math.cos(a1), v + rad * Math.sin(a1), th, r, g, b, a);
        }
        drawThickLine(buf, m, c, p, n, u - rad * 0.90, v, u + rad * 0.90, v, th, r, g, b, a);
        for (int i = 0; i < segs; i++) {
            double a0 = Math.toRadians(360.0 * i / segs);
            double a1 = Math.toRadians(360.0 * (i + 1) / segs);
            drawThickLine(buf, m, c, p, n, u + rad * 0.45 * Math.cos(a0), v + rad * Math.sin(a0),
                    u + rad * 0.45 * Math.cos(a1), v + rad * Math.sin(a1), th, r, g, b, (int) (a * 0.8));
        }
    }

    private void drawResizeIcon(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                               double u, double v, double size, int r, int g, int b, int a) {
        double th = 0.0045;
        double s = size * 0.5;
        drawThickLine(buf, m, c, p, n, u - s * 0.8, v + s * 0.8, u + s * 0.8, v - s * 0.8, th, r, g, b, a);
        drawThickLine(buf, m, c, p, n, u - s * 0.2, v + s * 0.8, u + s * 0.8, v - s * 0.2, th, r, g, b, a);
    }

    private void drawSpinner(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                             double u, double v, double rad, float time, int r, int g, int b, int a) {
        int segs = 14;
        double startAng = time * Math.PI * 2.0;
        double totalAng = Math.toRadians(270.0);
        double th = 0.008;
        for (int i = 0; i < segs; i++) {
            float frac = (float) i / segs;
            double a0 = startAng + totalAng * frac;
            double a1 = startAng + totalAng * (float) (i + 1) / segs;
            double x0 = u + rad * Math.cos(a0), y0 = v + rad * Math.sin(a0);
            double x1 = u + rad * Math.cos(a1), y1 = v + rad * Math.sin(a1);
            int segAlpha = (int) (a * frac * frac);
            drawThickLine(buf, m, c, p, n, x0, y0, x1, y1, th, r, g, b, segAlpha);
        }
    }

    private void drawThickLine(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                              double x0, double y0, double x1, double y1, double thickness,
                              int r, int g, int b, int a) {
        if (a <= 0) return;
        double dx = x1 - x0, dy = y1 - y0;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) return;
        double nx = -dy / len * (thickness * 0.5);
        double ny = dx / len * (thickness * 0.5);
        double d = 0.008;

        colorVert(buf, m, c, p, n, x0 + nx, y0 + ny, d, r, g, b, a);
        colorVert(buf, m, c, p, n, x1 + nx, y1 + ny, d, r, g, b, a);
        colorVert(buf, m, c, p, n, x1 - nx, y1 - ny, d, r, g, b, a);
        colorVert(buf, m, c, p, n, x0 - nx, y0 - ny, d, r, g, b, a);
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

    private void screenTexVert(BufferBuilder buf, Matrix4f m, Matrix4f proj, Vec3d c, Panel p, Vec3d n,
                               double u, double v, double d, float alpha) {
        double curve = curveSetting.get() * p.halfW;
        double zc = curve * (u / p.halfW) * (u / p.halfW);
        double x = c.x + p.right.x * u + p.up.x * v + n.x * (d + zc);
        double y = c.y + p.right.y * u + p.up.y * v + n.y * (d + zc);
        double z = c.z + p.right.z * u + p.up.z * v + n.z * (d + zc);

        org.joml.Vector4f clip = new org.joml.Vector4f((float) x, (float) y, (float) z, 1.0f);
        m.transform(clip);
        if (proj != null) {
            proj.transform(clip);
        }
        float invW = clip.w != 0 ? (1.0f / clip.w) : 1.0f;
        float tu = MathHelper.clamp((clip.x * invW) * 0.5f + 0.5f, 0f, 1f);
        float tv = MathHelper.clamp(1.0f - ((clip.y * invW) * 0.5f + 0.5f), 0f, 1f);

        buf.vertex(m, (float) x, (float) y, (float) z).texture(tu, tv).color(1f, 1f, 1f, alpha);
    }

    private void colorVert(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                           double u, double v, double d, int r, int g, int b, int a) {
        double curve = curveSetting.get() * p.halfW;
        double zc = curve * (u / p.halfW) * (u / p.halfW);
        double x = c.x + p.right.x * u + p.up.x * v + n.x * (d + zc);
        double y = c.y + p.right.y * u + p.up.y * v + n.y * (d + zc);
        double z = c.z + p.right.z * u + p.up.z * v + n.z * (d + zc);
        buf.vertex(m, (float) x, (float) y, (float) z).color(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    private void texVert(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                         double u, double v, double d, float texU, float texV, float alpha) {
        double curve = curveSetting.get() * p.halfW;
        double zc = curve * (u / p.halfW) * (u / p.halfW);
        double x = c.x + p.right.x * u + p.up.x * v + n.x * (d + zc);
        double y = c.y + p.right.y * u + p.up.y * v + n.y * (d + zc);
        double z = c.z + p.right.z * u + p.up.z * v + n.z * (d + zc);
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
