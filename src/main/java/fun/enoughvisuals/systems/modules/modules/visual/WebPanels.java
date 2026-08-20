package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import fun.enoughvisuals.systems.hud.HudNotificationCenter;
import fun.enoughvisuals.systems.webpanel.WebBrowserBridge;
import fun.enoughvisuals.systems.webpanel.MinecraftVirtualCamera;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Premium Apple Vision style spatial web browser module.
 * Features gently curved 3D glass panels, live Chromium rendering,
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
    private static final double MIN_HH = 0.38, MAX_HH = 2.2;
    private static final double BAR_GAP = 0.055;
    private static final double NAV_GAP = 0.115;
    private static final int PANEL_SEGMENTS = 1024;

    private static WebPanels instance;

    private final StringSetting defaultUrl = add(new StringSetting(
            "Стартовый сайт", "default_url", "URL открываемый при создании окна", "https://www.google.com"));
    private final BindSetting createBind = add(new BindSetting(
            "Создать окно", "create_bind", "Клавиша для создания нового окна", GLFW.GLFW_KEY_UNKNOWN));
    private final SliderSetting curveSetting = add(new SliderSetting(
            "Изгиб", "curve", "Мягкий цилиндрический изгиб окна", 0.11f, 0.0f, 0.22f, 0.01f));
    private final ModeSetting virtualCameraMode = add(new ModeSetting(
            "Камера браузера", "virtual_camera_mode", "Ракурс Minecraft для звонков в браузере",
            "Селфи", "Селфи", "От первого лица", "От третьего лица"));
    private final List<Panel> panels = new ArrayList<>();
    private int nextId = 1;
    private Panel grabbed;
    private int grabMode; // 1 = move, 2 = resize
    private int resizeSide;
    private double grabDist;
    private double grabLocalU;
    private double grabLocalV;
    private double grabVerticalOffset;
    private int grabButton;
    private Panel rotatingPanel;
    private int rotateButton = -1;
    private boolean prevLeft;
    private boolean prevRight;
    private boolean hovering;
    private Panel contentPanel;
    private double[] contentHit;
    private Panel pressedPanel;
    private int pressedMouseX;
    private int pressedMouseY;
    private int pressedMouseButton;
    private Panel focusedPanel;
    private final Set<Integer> capturedKeys = new HashSet<>();
    private boolean prevCreateHeld;
    private long lastFrameNanos = System.nanoTime();

    public WebPanels() {
        instance = this;
    }

    public static WebPanels getInstance() {
        return instance;
    }

    public boolean needsVirtualCameraFrames() {
        if (!isEnabled()) return false;
        for (Panel panel : panels) {
            if (!panel.closing && panel.browser != null) return true;
        }
        return false;
    }

    public Perspective virtualCameraPerspective() {
        if (virtualCameraMode.is("От первого лица")) return Perspective.FIRST_PERSON;
        if (virtualCameraMode.is("От третьего лица")) return Perspective.THIRD_PERSON_BACK;
        return Perspective.THIRD_PERSON_FRONT;
    }

    public boolean isHovering() {
        return isEnabled() && hovering;
    }

    public boolean isMoving() {
        return grabbed != null && grabMode == 1;
    }

    public boolean scroll(double yOffset, boolean controlDown) {
        Panel hoveredPanel = hoveredPanel();
        if (controlDown) {
            if (isMoving()) {
                grabVerticalOffset += yOffset * 0.12;
                return true;
            }
            if (hoveredPanel != null && rotatingPanel == null) {
                hoveredPanel.targetCenter = hoveredPanel.targetCenter.add(0.0, yOffset * 0.12, 0.0);
                return true;
            }
        }
        if (isMoving()) {
            grabDist = MathHelper.clamp(grabDist + yOffset * 0.24, 1.2, 16.0);
            return true;
        } else if (isHoveringContent()) {
            forwardScroll(yOffset);
            return true;
        }
        return false;
    }

    public boolean handleRotationKey(int key) {
        if (!isEnabled() || key != GLFW.GLFW_KEY_R || mc.getWindow() == null) return false;
        long window = mc.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        return rotatingPanel != null || mouseDown && rotationCandidate() != null;
    }

    /** Consumes Minecraft look deltas while LMB/RMB + R rotates the spatial panel. */
    public boolean rotateWithMouse(double yawDelta, double pitchDelta) {
        if (!isEnabled() || mc.getWindow() == null) return false;
        long window = mc.getWindow().getHandle();
        boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean rHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (!rHeld || (!left && !right)) {
            rotatingPanel = null;
            rotateButton = -1;
            return false;
        }
        if (!ensureRotationGesture(left, right)) return false;

        Panel panel = rotatingPanel;
        double yaw = Math.toRadians(-yawDelta * 0.15);
        panel.targetRight = rotateAroundAxis(panel.targetRight, new Vec3d(0, 1, 0), yaw).normalize();
        panel.targetUp = rotateAroundAxis(panel.targetUp, new Vec3d(0, 1, 0), yaw).normalize();

        double pitch = Math.toRadians(pitchDelta * 0.15);
        Vec3d pitchedUp = rotateAroundAxis(panel.targetUp, panel.targetRight, pitch).normalize();
        if (pitchedUp.y > 0.12) panel.targetUp = pitchedUp;
        panel.targetUp = panel.targetUp
                .subtract(panel.targetRight.multiply(panel.targetUp.dotProduct(panel.targetRight)))
                .normalize();
        return true;
    }

    private Panel hoveredPanel() {
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (!panel.closing && panel.hoveredRegion >= 0) return panel;
        }
        return null;
    }

    private Panel rotationCandidate() {
        if (grabbed != null && !grabbed.closing) return grabbed;
        if (pressedPanel != null && !pressedPanel.closing) return pressedPanel;
        return hoveredPanel();
    }

    private boolean ensureRotationGesture(boolean left, boolean right) {
        if (rotatingPanel != null) {
            boolean held = rotateButton == GLFW.GLFW_MOUSE_BUTTON_LEFT ? left : right;
            if (held && !rotatingPanel.closing) return true;
            rotatingPanel = null;
            rotateButton = -1;
        }
        Panel candidate = rotationCandidate();
        if (candidate == null) return false;
        rotatingPanel = candidate;
        rotateButton = left ? GLFW.GLFW_MOUSE_BUTTON_LEFT : GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        if (pressedPanel != null && pressedPanel.browser != null) {
            WebBrowserBridge.mouseRelease(pressedPanel.browser,
                    pressedMouseX, pressedMouseY, pressedMouseButton);
        }
        pressedPanel = null;
        grabbed = null;
        return true;
    }

    private static Vec3d rotateAroundAxis(Vec3d vector, Vec3d axis, double angle) {
        Vec3d normalizedAxis = axis.normalize();
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        return vector.multiply(cosine)
                .add(normalizedAxis.crossProduct(vector).multiply(sine))
                .add(normalizedAxis.multiply(normalizedAxis.dotProduct(vector) * (1.0 - cosine)));
    }

    public boolean isHoveringContent() {
        return isEnabled() && contentPanel != null && contentPanel.browser != null
                && grabbed == null && rotatingPanel == null;
    }

    public void forwardScroll(double yOffset) {
        if (contentPanel != null && contentPanel.browser != null && contentHit != null) {
            int[] px = pixel(contentPanel, contentHit);
            WebBrowserBridge.mouseWheel(contentPanel.browser, px[0], px[1], yOffset * 18.0);
        }
    }

    public boolean isBrowserFocused() {
        return isEnabled() && focusedPanel != null
                && (focusedPanel.addressEditing || focusedPanel.browser != null);
    }

    public boolean isAddressEditing() {
        return isEnabled() && focusedPanel != null && focusedPanel.addressEditing;
    }

    public boolean forwardKeySafe(int key, int scancode, int mods, int action) {
        if (focusedPanel == null) {
            return false;
        }
        if (focusedPanel.addressEditing) {
            return forwardAddressKey(focusedPanel, key, mods, action);
        }
        if (focusedPanel.browser == null) return false;
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
        if (focusedPanel != null) {
            focusedPanel.addressEditing = false;
            focusedPanel.addressSelectAll = false;
        }
        capturedKeys.clear();
        focusedPanel = null;
    }

    public void forwardChar(int codepoint, int mods) {
        if (focusedPanel != null && focusedPanel.addressEditing) {
            if (codepoint >= 32 && codepoint != 127 && Character.isValidCodePoint(codepoint)) {
                insertAddressText(focusedPanel, new String(Character.toChars(codepoint)));
            }
        } else if (focusedPanel != null && focusedPanel.browser != null
                && codepoint <= Character.MAX_VALUE) {
            WebBrowserBridge.keyTyped(focusedPanel.browser, (char) codepoint, mods);
        }
    }

    private boolean forwardAddressKey(Panel p, int key, int mods, int action) {
        // Inline URL editing owns the keyboard completely: movement and module
        // binds cannot leak into gameplay while the field is active.
        if (action == GLFW.GLFW_RELEASE) return true;
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) return true;

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            cancelAddressEdit(p);
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            submitAddressEdit(p);
        } else if (key == GLFW.GLFW_KEY_A && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            p.addressSelectAll = true;
            p.addressCursor = p.addressDraft.length();
        } else if (key == GLFW.GLFW_KEY_V && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clipboard = mc.keyboard.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) insertAddressText(p, clipboard);
        } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (p.addressSelectAll) {
                p.addressDraft = "";
                p.addressCursor = 0;
                p.addressSelectAll = false;
            } else if (p.addressCursor > 0) {
                int previous = p.addressDraft.offsetByCodePoints(p.addressCursor, -1);
                p.addressDraft = p.addressDraft.substring(0, previous)
                        + p.addressDraft.substring(p.addressCursor);
                p.addressCursor = previous;
            }
        } else if (key == GLFW.GLFW_KEY_DELETE) {
            if (p.addressSelectAll) {
                p.addressDraft = "";
                p.addressCursor = 0;
                p.addressSelectAll = false;
            } else if (p.addressCursor < p.addressDraft.length()) {
                int next = p.addressDraft.offsetByCodePoints(p.addressCursor, 1);
                p.addressDraft = p.addressDraft.substring(0, p.addressCursor)
                        + p.addressDraft.substring(next);
            }
        } else if (key == GLFW.GLFW_KEY_LEFT) {
            p.addressSelectAll = false;
            if (p.addressCursor > 0) p.addressCursor = p.addressDraft.offsetByCodePoints(p.addressCursor, -1);
        } else if (key == GLFW.GLFW_KEY_RIGHT) {
            p.addressSelectAll = false;
            if (p.addressCursor < p.addressDraft.length()) {
                p.addressCursor = p.addressDraft.offsetByCodePoints(p.addressCursor, 1);
            }
        } else if (key == GLFW.GLFW_KEY_HOME) {
            p.addressSelectAll = false;
            p.addressCursor = 0;
        } else if (key == GLFW.GLFW_KEY_END) {
            p.addressSelectAll = false;
            p.addressCursor = p.addressDraft.length();
        }
        return true;
    }

    private void insertAddressText(Panel p, String value) {
        if (p.addressSelectAll) {
            p.addressDraft = "";
            p.addressCursor = 0;
            p.addressSelectAll = false;
        }
        p.addressDraft = p.addressDraft.substring(0, p.addressCursor)
                + value + p.addressDraft.substring(p.addressCursor);
        p.addressCursor += value.length();
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
        exitFocus();
        for (Panel p : panels) {
            disposePanel(p);
        }
        panels.clear();
        grabbed = null;
        contentPanel = null;
        pressedPanel = null;
    }

    @Override
    public void onTick() {
        if (isBrowserFocused()) releaseGameplayMovementKeys();
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
        p.center = eye.add(fwd.multiply(3.2));
        p.targetCenter = p.center;
        Vec3d worldUp = new Vec3d(0, 1, 0);
        p.right = fwd.crossProduct(worldUp).normalize();
        if (p.right.lengthSquared() < 1e-6) {
            p.right = new Vec3d(1, 0, 0);
        }
        p.up = p.right.crossProduct(fwd).normalize();
        p.targetRight = p.right;
        p.targetUp = p.up;
        p.halfW = p.targetHalfW = 1.20;
        p.halfH = p.targetHalfH = 0.675;
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
        boolean enteringControl = focusedPanel != p || !p.addressEditing;
        if (focusedPanel != null && focusedPanel != p) exitFocus();
        focusedPanel = p;
        p.addressDraft = p.url == null ? "" : p.url;
        p.addressCursor = p.addressDraft.length();
        p.addressSelectAll = true;
        p.addressEditing = true;
        releaseGameplayMovementKeys();
        if (enteringControl) notifyControlEntered();
    }

    private void notifyControlEntered() {
        HudNotificationCenter.info("Нажмите ESC, чтобы выйти из управления браузером");
    }

    private void releaseGameplayMovementKeys() {
        if (mc.options == null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    private void cancelAddressEdit(Panel p) {
        p.addressEditing = false;
        p.addressSelectAll = false;
        p.addressDraft = p.url == null ? "" : p.url;
        capturedKeys.clear();
        if (focusedPanel == p) focusedPanel = null;
    }

    private void submitAddressEdit(Panel p) {
        String url = normalizeAddress(p.addressDraft);
        p.addressEditing = false;
        p.addressSelectAll = false;
        capturedKeys.clear();
        if (focusedPanel == p) focusedPanel = null;
        if (url.isEmpty()) return;
        p.url = url;
        p.addressDraft = url;
        p.addressCursor = url.length();
        if (p.browser != null) WebBrowserBridge.loadUrl(p.browser, url);
    }

    private static String normalizeAddress(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return "";
        if (value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) return value;
        if (value.contains(" ") || !value.contains(".")) {
            return "https://www.google.com/search?q="
                    + URLEncoder.encode(value, StandardCharsets.UTF_8).replace("%20", "+");
        }
        return "https://" + value;
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (MinecraftVirtualCamera.isRenderingVirtualView()) return;
        if (instance == null || !instance.isEnabled() || instance.panels.isEmpty()) return;
        if (mc.player == null || mc.world == null || context.camera() == null || context.matrixStack() == null) return;
        instance.renderPanels(context);
    }

    private void renderPanels(WorldRenderContext context) {
        Vec3d cam = context.camera().getPos();
        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1f;
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(tickDelta).normalize();
        long now = System.nanoTime();
        float frameDt = MathHelper.clamp((now - lastFrameNanos) / 1_000_000_000f, 1f / 240f, 0.05f);
        lastFrameNanos = now;
        handleInput(eye, look);
        for (Panel p : panels) {
            animatePanel(p, frameDt);
        }

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        Matrix4f proj = context.projectionMatrix();

        // Upkeep MCEF browsers and pump message loop
        WebBrowserBridge.runMessageLoop();
        for (Panel p : panels) {
            if (!p.closing) {
                ensureBrowser(p);
                pumpVirtualCamera(p);
            }
        }

        // Capture in-world blur frame if theme blur enabled
        if (!CustomTheme.blurDisabled()) {
            GlassUtil.BLUR_OFFSET = CustomTheme.hudBlurLevel();
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
            // 1. Front face only. The back never samples the screen framebuffer;
            // that was the cause of the mirrored/split world image on the reverse.
            int blurTex = GlassUtil.getResultTexture();
            if (blurTex > 0) {
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                RenderSystem.setShaderTexture(0, blurTex);
                GlStateManager._bindTexture(blurTex);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

                BufferBuilder blurBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Panel p : panels) {
                    if (p.anim > 0.01f && isFrontFacing(p, eye)) {
                        drawCurvedFrostedBlur(blurBuf, matrix, proj, cam, p);
                    }
                }
                BuiltBuffer builtBlur = blurBuf.endNullable();
                if (builtBlur != null) {
                    BufferRenderer.drawWithGlobalProgram(builtBlur);
                }
            }

            // 2. Opaque curved shell. Front and back are intentionally separate.
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder colorBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Panel p : panels) {
                if (p.anim > 0.01f) {
                    if (isFrontFacing(p, eye)) {
                        drawPanelChrome(colorBuf, matrix, cam, p, hit(p, eye, look));
                    } else {
                        drawPanelBack(colorBuf, matrix, cam, p);
                    }
                }
            }
            BuiltBuffer builtColor = colorBuf.endNullable();
            if (builtColor != null) {
                BufferRenderer.drawWithGlobalProgram(builtColor);
            }

            // 3. Draw browser content (Chromium frame texture) with crisp linear filtering
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            for (Panel p : panels) {
                int glTex = p.browser != null ? WebBrowserBridge.textureId(p.browser) : 0;
                if (glTex > 0 && p.anim > 0.01f && isFrontFacing(p, eye)) {
                    RenderSystem.setShaderTexture(0, glTex);
                    GlStateManager._bindTexture(glTex);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

                    BufferBuilder texBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    drawContent(texBuf, matrix, cam, p);
                    BuiltBuffer builtTex = texBuf.endNullable();
                    if (builtTex != null) {
                        BufferRenderer.drawWithGlobalProgram(builtTex);
                    }
                }
            }

            // 4. The controls and address use the real MTSDF atlases in world
            // space. This pass replaces all hand-built arrow/globe/trash paths.
            for (Panel p : panels) {
                if (p.anim > 0.01f && isFrontFacing(p, eye)) {
                    drawPanelMtsdf(matrix, cam, p, hit(p, eye, look));
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

    private void animatePanel(Panel p, float dt) {
        float targetAnim = p.closing ? 0f : 1f;
        p.animVelocity += (targetAnim - p.anim) * 150f * dt;
        p.animVelocity *= (float) Math.exp(-17f * dt);
        p.anim = MathHelper.clamp(p.anim + p.animVelocity * dt, 0f, 1.035f);

        double moveStiffness = grabbed == p && grabMode == 1 ? 125.0 : 92.0;
        double moveDamping = grabbed == p && grabMode == 1 ? 17.5 : 16.0;
        p.moveVelocity = p.moveVelocity.add(p.targetCenter.subtract(p.center).multiply(moveStiffness * dt));
        p.moveVelocity = p.moveVelocity.multiply(Math.exp(-moveDamping * dt));
        p.center = p.center.add(p.moveVelocity.multiply(dt));

        double sizeStiffness = grabbed == p && grabMode == 2 ? 145.0 : 105.0;
        double sizeDamping = grabbed == p && grabMode == 2 ? 18.5 : 17.0;
        p.widthVelocity += (p.targetHalfW - p.halfW) * sizeStiffness * dt;
        p.heightVelocity += (p.targetHalfH - p.halfH) * sizeStiffness * dt;
        double sizeDrag = Math.exp(-sizeDamping * dt);
        p.widthVelocity *= sizeDrag;
        p.heightVelocity *= sizeDrag;
        p.halfW += p.widthVelocity * dt;
        p.halfH += p.heightVelocity * dt;

        double orientationBlend = 1.0 - Math.exp(-((grabbed == p || rotatingPanel == p) ? 15.0 : 10.0) * dt);
        p.right = p.right.lerp(p.targetRight, orientationBlend).normalize();
        p.up = p.up.lerp(p.targetUp, orientationBlend);
        p.up = p.up.subtract(p.right.multiply(p.up.dotProduct(p.right))).normalize();

        float barTarget = p.hoveredRegion == 1 || p.hoveredRegion == 3
                || (grabbed == p && grabMode == 1) || rotatingPanel == p ? 1f : 0f;
        p.barReveal = smoothTowards(p.barReveal, barTarget, 15f, dt);
        for (int i = 0; i < p.edgeReveal.length; i++) {
            float edgeTarget = p.hoveredRegion == 10 + i || (grabbed == p && resizeSide == 10 + i) ? 1f : 0f;
            p.edgeReveal[i] = smoothTowards(p.edgeReveal[i], edgeTarget, 17f, dt);
        }
    }

    private static float smoothTowards(float current, float target, float speed, float dt) {
        return current + (target - current) * (1f - (float) Math.exp(-speed * dt));
    }

    private void ensureBrowser(Panel p) {
        if (p.browser != null || !WebBrowserBridge.isReady()) {
            return;
        }
        p.texW = 1920;
        p.texH = 1080;
        p.browser = WebBrowserBridge.create(p.url, p.texW, p.texH);
    }

    private void pumpVirtualCamera(Panel p) {
        if (p.browser == null) return;
        long now = System.currentTimeMillis();
        if (now - p.lastCameraBootstrapMs >= 1_000L) {
            WebBrowserBridge.installMinecraftCamera(p.browser);
            p.lastCameraBootstrapMs = now;
        }
        long sequence = MinecraftVirtualCamera.latestFrameSequence();
        String frame = MinecraftVirtualCamera.latestFrameDataUrl();
        if (sequence > p.lastCameraFrameSequence && frame != null) {
            WebBrowserBridge.pushMinecraftCameraFrame(p.browser, frame);
            p.lastCameraFrameSequence = sequence;
        }
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
        double scale = 0.88 + 0.12 * a;
        double hw = p.halfW * scale;
        double hh = p.halfH * scale;
        double d = 0.001;
        float alpha = 0.94f * a;
        int cols = PANEL_SEGMENTS;
        double corner = smartCorner(hw, hh);
        for (int i = 0; i < cols; i++) {
            double u0 = -hw + 2 * hw * i / cols;
            double u1 = -hw + 2 * hw * (i + 1) / cols;
            double v0 = squircleHalfHeight(u0, hw, hh, corner);
            double v1 = squircleHalfHeight(u1, hw, hh, corner);
            screenTexVert(buffer, m, proj, c, p, n, u0, v0, d, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u1, v1, d, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u1, -v1, d, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u0, -v0, d, alpha);
        }
    }

    private void drawContent(BufferBuilder buffer, Matrix4f m, Vec3d cam, Panel p) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        double scale = 0.88 + 0.12 * a;
        double hw = p.halfW * scale;
        double hh = p.halfH * scale;
        double d = 0.005;
        float alpha = a;
        int cols = PANEL_SEGMENTS;
        double corner = smartCorner(hw, hh);
        for (int i = 0; i < cols; i++) {
            double u0 = -hw + 2 * hw * i / cols;
            double u1 = -hw + 2 * hw * (i + 1) / cols;
            float tu0 = (float) i / cols, tu1 = (float) (i + 1) / cols;
            double v0 = squircleHalfHeight(u0, hw, hh, corner);
            double v1 = squircleHalfHeight(u1, hw, hh, corner);
            float tv0 = (float) ((hh - v0) / (2.0 * hh));
            float tv1 = (float) ((hh - v1) / (2.0 * hh));

            texVert(buffer, m, c, p, n, u0, v0, d, tu0, tv0, alpha);
            texVert(buffer, m, c, p, n, u1, v1, d, tu1, tv1, alpha);
            texVert(buffer, m, c, p, n, u1, -v1, d, tu1, 1f - tv1, alpha);
            texVert(buffer, m, c, p, n, u0, -v0, d, tu0, 1f - tv0, alpha);
        }
    }

    private void handleInput(Vec3d eye, Vec3d look) {
        long win = mc.getWindow().getHandle();
        boolean left = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean rHeld = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        boolean leftPress = left && !prevLeft;
        boolean rightPress = right && !prevRight;
        boolean pressEdge = leftPress || rightPress;
        int pressButton = leftPress ? 0 : 1;
        if (rHeld && (left || right)) {
            ensureRotationGesture(left, right);
        } else if (rotatingPanel != null) {
            rotatingPanel = null;
            rotateButton = -1;
        }
        boolean grabHeld = grabbed != null && (grabButton == 0 ? left : right);

        if (grabbed != null && grabHeld && !grabbed.closing) {
            double[] h = hitPlane(grabbed, eye, look, true);
            if (grabMode == 2 && h != null) {
                grabbed.targetHalfW = MathHelper.clamp(Math.abs(h[0]), MIN_HW, MAX_HW);
                grabbed.targetHalfH = MathHelper.clamp(Math.abs(h[1]), MIN_HH, MAX_HH);
            } else if (grabMode == 1) {
                Vec3d grabbedPoint = eye.add(look.multiply(grabDist));
                Vec3d normal = eye.subtract(grabbedPoint).normalize();
                Vec3d newRight = new Vec3d(0, 1, 0).crossProduct(normal);
                if (newRight.lengthSquared() < 1e-6) newRight = grabbed.targetRight;
                newRight = newRight.normalize();
                Vec3d newUp = normal.crossProduct(newRight).normalize();
                grabbed.targetRight = newRight;
                grabbed.targetUp = newUp;
                double[] curved = curvedLocal(grabbed, grabLocalU);
                grabbed.targetCenter = grabbedPoint
                        .subtract(newRight.multiply(curved[0]))
                        .subtract(newUp.multiply(grabLocalV))
                        .subtract(normal.multiply(curved[1]))
                        .add(0.0, grabVerticalOffset, 0.0);
            }
        } else {
            grabbed = null;
        }

        hovering = false;
        contentPanel = null;
        contentHit = null;
        for (Panel p : panels) {
            p.hoveredRegion = -1;
        }
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.closing) continue;
            double[] h = hit(p, eye, look);
            if (h == null) continue;
            int region = region(p, h[0], h[1]);
            p.hoveredRegion = region;
            if (region != -1) {
                hovering = true;
            }
            if (region == 0 && p.browser != null) {
                contentPanel = p;
                contentHit = h;
            }
            if (region != -1) break;
        }

        boolean uiPressHandled = false;
        if (pressEdge && grabbed == null && rotatingPanel == null) {
            for (int i = panels.size() - 1; i >= 0; i--) {
                Panel p = panels.get(i);
                if (p.closing) continue;
                double[] h = hit(p, eye, look);
                if (h == null) continue;
                int region = region(p, h[0], h[1]);
                if (region == -1) continue;
                if (focusedPanel != null && focusedPanel.addressEditing && region != 5) {
                    cancelAddressEdit(focusedPanel);
                }
                if (region == 3) {
                    p.closing = true;
                    exitFocusIf(p);
                    uiPressHandled = true;
                } else if (region == 10 || region == 11) {
                    grabbed = p;
                    grabMode = 2;
                    grabButton = pressButton;
                    resizeSide = region;
                    uiPressHandled = true;
                } else if (region == 1) {
                    grabbed = p;
                    grabMode = 1;
                    grabButton = pressButton;
                    Vec3d hitWorld = panelPoint(p, h[0], h[1]);
                    grabDist = hitWorld.distanceTo(eye);
                    grabLocalU = h[0];
                    grabLocalV = h[1];
                    grabVerticalOffset = 0.0;
                    uiPressHandled = true;
                } else if (region == 5) {
                    openAddress(p);
                    uiPressHandled = true;
                } else if (region == 6) {
                    if (p.browser != null) WebBrowserBridge.goBack(p.browser);
                    uiPressHandled = true;
                } else if (region == 7) {
                    if (p.browser != null) WebBrowserBridge.goForward(p.browser);
                    uiPressHandled = true;
                } else if (region == 8) {
                    if (p.browser != null) WebBrowserBridge.reload(p.browser);
                    uiPressHandled = true;
                }
                break;
            }
        }

        if (contentPanel != null && grabbed == null && rotatingPanel == null) {
            int[] px = pixel(contentPanel, contentHit);
            if (pressedPanel != contentPanel) {
                WebBrowserBridge.mouseMove(contentPanel.browser, px[0], px[1]);
            }
            if (pressEdge && !uiPressHandled && pressedPanel == null) {
                boolean enteringControl = focusedPanel != contentPanel || focusedPanel.addressEditing;
                WebBrowserBridge.mousePress(contentPanel.browser, px[0], px[1], pressButton);
                pressedPanel = contentPanel;
                pressedMouseButton = pressButton;
                pressedMouseX = px[0];
                pressedMouseY = px[1];
                focusedPanel = contentPanel;
                releaseGameplayMovementKeys();
                if (enteringControl) notifyControlEntered();
            }
        }
        if (pressedPanel != null) {
            boolean pressedButtonHeld = pressedMouseButton == 0 ? left : right;
            if (pressedButtonHeld && !pressedPanel.closing && pressedPanel.browser != null) {
                double[] heldHit = hitPlane(pressedPanel, eye, look, true);
                if (heldHit != null) {
                    int[] px = pixel(pressedPanel, heldHit);
                    pressedMouseX = px[0];
                    pressedMouseY = px[1];
                }
                // MCEF keeps its left/right button mask between press and release;
                // forwarding moves preserves native Chromium pointer capture/long-press.
                WebBrowserBridge.mouseMove(pressedPanel.browser, pressedMouseX, pressedMouseY);
            } else {
                if (pressedPanel.browser != null) {
                    WebBrowserBridge.mouseRelease(pressedPanel.browser,
                            pressedMouseX, pressedMouseY, pressedMouseButton);
                }
                pressedPanel = null;
            }
        }
        prevLeft = left;
        prevRight = right;

        if (focusedPanel != null && focusedPanel.closing) {
            exitFocus();
        }
    }

    private void exitFocusIf(Panel panel) {
        if (focusedPanel == panel) exitFocus();
    }

    private double[] hit(Panel p, Vec3d eye, Vec3d look) {
        if (!isFrontFacing(p, eye)) return null;
        return hitPlane(p, eye, look, false);
    }

    private static boolean isFrontFacing(Panel p, Vec3d eye) {
        Vec3d normal = p.right.crossProduct(p.up).normalize();
        return eye.subtract(p.center).dotProduct(normal) > 0.0;
    }

    private double[] hitPlane(Panel p, Vec3d eye, Vec3d look, boolean unbounded) {
        Vec3d n = p.right.crossProduct(p.up).normalize();
        Vec3d origin = eye.subtract(p.center);
        double ox = origin.dotProduct(p.right);
        double oy = origin.dotProduct(p.up);
        double oz = origin.dotProduct(n);
        double dx = look.dotProduct(p.right);
        double dy = look.dotProduct(p.up);
        double dz = look.dotProduct(n);

        double angle = curveAngle();
        double t;
        double lu;
        if (angle < 1e-5) {
            if (Math.abs(dz) < 1e-6) return null;
            t = -oz / dz;
            lu = ox + dx * t;
        } else {
            double radius = p.halfW / angle;
            double qa = dx * dx + dz * dz;
            if (qa < 1e-8) return null;
            double qb = 2.0 * (ox * dx + (oz - radius) * dz);
            double qc = ox * ox + (oz - radius) * (oz - radius) - radius * radius;
            double discriminant = qb * qb - 4.0 * qa * qc;
            if (discriminant < 0.0) return null;
            double root = Math.sqrt(discriminant);
            double t0 = (-qb - root) / (2.0 * qa);
            double t1 = (-qb + root) / (2.0 * qa);
            t = Double.POSITIVE_INFINITY;
            lu = 0.0;
            for (double candidate : new double[]{t0, t1}) {
                if (candidate <= 1e-5 || candidate >= t) continue;
                double hitX = ox + dx * candidate;
                double hitZ = oz + dz * candidate;
                double theta = Math.atan2(hitX, radius - hitZ);
                // Reject the remote backside intersection of the mathematical
                // cylinder; the panel only occupies its forward shallow arc.
                if (Math.abs(theta) >= Math.PI * 0.5) continue;
                t = candidate;
                lu = theta * radius;
            }
            if (!Double.isFinite(t)) return null;
        }
        if (t > 64) return null;
        double lv = oy + dy * t;
        if (!unbounded && (Math.abs(lu) > p.halfW + 0.36 || Math.abs(lv) > p.halfH + 0.30)) return null;
        return new double[]{lu, lv};
    }

    private double grabBarY(Panel p) {
        return -p.halfH - BAR_GAP * controlScale(p);
    }

    private double deleteU(Panel p) {
        return 0.165 * controlScale(p) * p.barReveal;
    }

    private double grabU(Panel p) {
        return -0.055 * controlScale(p) * p.barReveal;
    }

    private double navHalfWidth(Panel p) {
        return Math.min(p.halfW * 0.62, 0.86 * controlScale(p));
    }

    private double navY(Panel p) {
        return p.halfH + NAV_GAP * controlScale(p);
    }

    private static double controlScale(Panel p) {
        double widthScale = p.halfW / 1.20;
        double heightScale = p.halfH / 0.675;
        return MathHelper.clamp(Math.sqrt(Math.max(0.01, widthScale * heightScale)), 0.72, 1.75);
    }

    private int region(Panel p, double lu, double lv) {
        // A standalone iOS capsule. Hover reveals only the separate delete
        // action; there is deliberately no hover shell behind the grabber.
        double control = controlScale(p);
        double barY = grabBarY(p);
        if (p.barReveal > 0.35f
                && Math.hypot(lu - deleteU(p), lv - barY) < 0.052 * control) return 3;
        if (Math.abs(lv - barY) < 0.060 * control
                && Math.abs(lu - grabU(p)) < 0.155 * control) {
            return 1; // grab bar
        }

        // Compact navigation ornament sits close to the panel instead of floating far above it.
        double navY = navY(p), abHalfW = navHalfWidth(p);
        if (Math.abs(lv - navY) < 0.068 * control
                && Math.abs(lu) <= abHalfW + 0.025 * control) {
            double backU = -abHalfW + 0.075 * control;
            double fwdU = backU + 0.105 * control;
            double reloadU = fwdU + 0.105 * control;

            if (Math.abs(lu - backU) < 0.050 * control) return 6;
            if (Math.abs(lu - fwdU) < 0.050 * control) return 7;
            if (Math.abs(lu - reloadU) < 0.050 * control) return 8;
            return 5;
        }

        // Resize exists only at the two lower corners, matching the requested
        // outline corner controls instead of a ring around the perimeter.
        double cornerRadius = 0.135 * control;
        if (Math.hypot(lu + p.halfW, lv + p.halfH) < cornerRadius) return 10;
        if (Math.hypot(lu - p.halfW, lv + p.halfH) < cornerRadius) return 11;

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
        double scale = 0.88 + 0.12 * a;
        double hw = p.halfW * scale;
        double hh = p.halfH * scale;
        double control = controlScale(p) * scale;

        int accentLight = MenuScreen.ACCENT_LIGHT;
        int alr = (accentLight >> 16) & 0xFF;
        int alg = (accentLight >> 8) & 0xFF;
        int alb = accentLight & 0xFF;

        // One exact squircle on the shared cylindrical surface. No enlarged shell, specular line or glass
        // outline is allowed to peek around Chromium's texture.
        int glassAlpha = CustomTheme.blurDisabled() ? 242 : 174;
        drawSquircle(buf, m, c, p, n, 1, 0, 0, hw, hh,
                smartCorner(hw, hh), 0x10, 0x13, 0x1A, (int) (glassAlpha * a));

        // Loading state remains understated and monochrome.
        int glTex = p.browser != null ? WebBrowserBridge.textureId(p.browser) : 0;
        if (glTex <= 0) {
            float timeSec = (System.currentTimeMillis() % 100000L) / 1000f;
            drawSpinner(buf, m, c, p, n, 0, 0.035, 0.105, timeSec * 1.25f,
                    alr, alg, alb, (int) (205 * a));
        }

        // Large rounded iOS grabber. Hover never creates a background behind it.
        float br = p.barReveal;
        double barY = -hh - BAR_GAP * control;
        double grabU = -0.055 * control * br;
        fillRound(buf, m, c, p, n, 6, grabU, barY,
                0.140 * control, 0.014 * control, 0.014 * control,
                245, 247, 251, (int) ((220 + 35 * br) * a));

        if (br > 0.02f) {
            double trashU = 0.165 * control * br;
            fillRound(buf, m, c, p, n, 7, trashU, barY,
                    0.032 * control, 0.032 * control, 0.032 * control,
                    0xFF, 0x3B, 0x45, (int) (232 * br * a));
        }

        drawBottomCornerHandle(buf, m, c, p, n, -hw, -hh, false, control, p.edgeReveal[0] * a);
        drawBottomCornerHandle(buf, m, c, p, n, hw, -hh, true, control, p.edgeReveal[1] * a);

        // Compact navigation ornament, aligned to the window and using Solar icon geometry.
        double abH = 0.053 * control;
        double abHalfW = Math.min(hw * 0.62, 0.86 * control);
        double navY = hh + NAV_GAP * control;

        fillRound(buf, m, c, p, n, 4, 0, navY, abHalfW, abH, abH,
                0x16, 0x19, 0x20, (int) (218 * a));

        double backU = -abHalfW + 0.075 * control;
        double fwdU = backU + 0.105 * control;
        double reloadU = fwdU + 0.105 * control;

        boolean overBack = h != null && region(p, h[0], h[1]) == 6;
        boolean overFwd = h != null && region(p, h[0], h[1]) == 7;
        boolean overReload = h != null && region(p, h[0], h[1]) == 8;
        boolean overPill = h != null && region(p, h[0], h[1]) == 5;

        double btnR = 0.038 * control;
        fillRound(buf, m, c, p, n, 6, backU, navY, btnR, btnR, btnR,
                0xF2, 0xF4, 0xF8, (int) ((overBack ? 38 : 0) * a));

        fillRound(buf, m, c, p, n, 6, fwdU, navY, btnR, btnR, btnR,
                0xF2, 0xF4, 0xF8, (int) ((overFwd ? 38 : 0) * a));

        fillRound(buf, m, c, p, n, 6, reloadU, navY, btnR, btnR, btnR,
                0xF2, 0xF4, 0xF8, (int) ((overReload ? 38 : 0) * a));

        double pillLeft = reloadU + 0.060 * control;
        double pillRight = abHalfW - 0.028 * control;
        double pillU = (pillLeft + pillRight) * 0.5;
        double pillHW = (pillRight - pillLeft) * 0.5;
        double pillHH = 0.034 * control;

        fillRound(buf, m, c, p, n, 6, pillU, navY, pillHW, pillHH, pillHH,
                overPill ? 0x31 : 0x21, overPill ? 0x35 : 0x25, overPill ? 0x3D : 0x2C,
                (int) ((overPill ? 225 : 180) * a));

        if (p.addressEditing && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            String prefix = p.addressDraft.substring(0, Math.min(p.addressCursor, p.addressDraft.length()));
            double textSize = 0.032 * control;
            double textStart = pillU - pillHW + 0.072 * control;
            double caretU = textStart + CustomFontRenderer.getTextWidth(prefix, (float) textSize, FontWeight.MEDIUM);
            caretU = Math.min(caretU, pillU + pillHW - 0.020 * control);
            drawThickLine(buf, m, c, p, n,
                    caretU, navY - 0.020 * control, caretU, navY + 0.020 * control,
                    0.0035 * control, 255, 255, 255, (int) (235 * a));
        }
    }

    private void drawPanelBack(BufferBuilder buf, Matrix4f m, Vec3d cam, Panel p) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        double scale = 0.88 + 0.12 * a;
        // A self-contained matte reverse: no framebuffer sampling, Chromium
        // texture, seam or reflective rim.
        double hw = p.halfW * scale;
        double hh = p.halfH * scale;
        drawSquircle(buf, m, c, p, n, 0, 0, 0,
                hw, hh, smartCorner(hw, hh),
                0x0C, 0x0E, 0x13, (int) (248 * a));
    }

    private void drawPanelMtsdf(Matrix4f matrix, Vec3d cam, Panel p, double[] hit) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        double scale = 0.88 + 0.12 * a;
        double hw = p.halfW * scale;
        double hh = p.halfH * scale;
        double control = controlScale(p) * scale;
        double navY = hh + NAV_GAP * control;
        double abHalfW = Math.min(hw * 0.62, 0.86 * control);
        double backU = -abHalfW + 0.075 * control;
        double fwdU = backU + 0.105 * control;
        double reloadU = fwdU + 0.105 * control;
        int hovered = hit == null ? -1 : region(p, hit[0], hit[1]);
        int muted = argb((int) ((hovered == 6 ? 255 : 188) * a), 232, 235, 242);
        int forward = argb((int) ((hovered == 7 ? 255 : 188) * a), 232, 235, 242);
        int reload = argb((int) ((hovered == 8 ? 255 : 188) * a), 232, 235, 242);

        drawPanelWorldText(matrix, c, p, n,
                SolarIcons.ARROW_LEFT, backU, navY, 0.050 * control, 0.013,
                muted, FontWeight.SOLAR_ICONS, true);
        drawPanelWorldText(matrix, c, p, n,
                SolarIcons.ARROW_RIGHT, fwdU, navY, 0.050 * control, 0.013,
                forward, FontWeight.SOLAR_ICONS, true);
        drawPanelWorldText(matrix, c, p, n,
                SolarIcons.REFRESH, reloadU, navY, 0.047 * control, 0.013,
                reload, FontWeight.SOLAR_ICONS, true);

        double pillLeft = reloadU + 0.060 * control;
        double pillRight = abHalfW - 0.028 * control;
        double pillU = (pillLeft + pillRight) * 0.5;
        double pillHW = (pillRight - pillLeft) * 0.5;
        double iconU = pillU - pillHW + 0.043 * control;
        int addressColor = argb((int) ((hovered == 5 || p.addressEditing ? 240 : 172) * a),
                210, 215, 225);
        drawPanelWorldText(matrix, c, p, n,
                SolarIcons.GLOBAL, iconU, navY, 0.034 * control, 0.013,
                addressColor, FontWeight.SOLAR_ICONS, true);

        String address = p.addressEditing ? p.addressDraft : p.url;
        double textSize = 0.032 * control;
        double textU = pillU - pillHW + 0.072 * control;
        float maxTextWidth = (float) Math.max(0.04 * control, pillRight - 0.020 * control - textU);
        String visibleAddress = CustomFontRenderer.trimToWidth(
                address == null ? "" : address, maxTextWidth, (float) textSize, FontWeight.MEDIUM);
        drawPanelWorldText(matrix, c, p, n,
                visibleAddress, textU, navY, textSize, 0.013,
                argb((int) (226 * a), 238, 240, 245), FontWeight.MEDIUM, false);

        if (p.barReveal > 0.02f) {
            drawPanelWorldText(matrix, c, p, n,
                    SolarIcons.DELETE, 0.165 * control * p.barReveal,
                    -hh - BAR_GAP * control, 0.039 * control, 0.013,
                    argb((int) (245 * p.barReveal * a), 255, 255, 255),
                    FontWeight.SOLAR_ICONS, true);
        }

        int glTex = p.browser != null ? WebBrowserBridge.textureId(p.browser) : 0;
        if (glTex <= 0) {
            drawPanelWorldText(matrix, c, p, n,
                    SolarIcons.GLOBAL, 0, 0.035, 0.075, 0.013,
                    argb((int) (180 * a), 255, 255, 255), FontWeight.SOLAR_ICONS, true);
        }
    }

    private void drawPanelWorldText(Matrix4f matrix, Vec3d center, Panel p, Vec3d normal,
                                    String text, double anchorU, double centerV, double size, double depth,
                                    int color, FontWeight weight, boolean centered) {
        if (centered && weight == FontWeight.SOLAR_ICONS) {
            CustomFontRenderer.drawCurvedWorldGlyph(matrix, center, p.right, p.up, normal,
                    p.halfW, curveAngle(), text, anchorU, centerV, size, depth, color, weight);
        } else {
            CustomFontRenderer.drawCurvedWorldText(matrix, center, p.right, p.up, normal,
                    p.halfW, curveAngle(), text, anchorU, centerV, size, depth,
                    color, weight, centered);
        }
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (MathHelper.clamp(alpha, 0, 255) << 24)
                | (MathHelper.clamp(red, 0, 255) << 16)
                | (MathHelper.clamp(green, 0, 255) << 8)
                | MathHelper.clamp(blue, 0, 255);
    }

    // ----------------- Planar custom drawing helpers in 3D -----------------

    private void drawSquircle(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n, int layer,
                              double cu, double cv, double hw, double hh, double corner,
                              int r, int g, int b, int a) {
        if (a <= 0) return;
        double d = layer * 0.0015;
        int cols = PANEL_SEGMENTS;
        for (int i = 0; i < cols; i++) {
            double x0 = -hw + 2.0 * hw * i / cols;
            double x1 = -hw + 2.0 * hw * (i + 1) / cols;
            double y0 = squircleHalfHeight(x0, hw, hh, corner);
            double y1 = squircleHalfHeight(x1, hw, hh, corner);
            colorVert(buf, m, c, p, n, cu + x0, cv + y0, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu + x1, cv + y1, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu + x1, cv - y1, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu + x0, cv - y0, d, r, g, b, a);
        }
    }

    private static double squircleHalfHeight(double x, double hw, double hh, double corner) {
        corner = Math.min(corner, Math.min(hw, hh));
        double ax = Math.abs(x);
        double straight = hw - corner;
        if (ax <= straight) return hh;
        double t = MathHelper.clamp((ax - straight) / Math.max(corner, 1e-6), 0.0, 1.0);
        // A soft superellipse shoulder without the visually square power used
        // by the previous silhouette.
        double power = 2.20;
        double arc = Math.pow(Math.max(0.0, 1.0 - Math.pow(t, power)), 1.0 / power);
        return hh - corner + corner * arc;
    }

    private static double smartCorner(double hw, double hh) {
        // Roughly 28-40 Chromium pixels at the default 1920x1080 panel. This
        // keeps the silhouette rounded without eating page buttons or text.
        return MathHelper.clamp(Math.min(hw, hh) * 0.10, 0.045, 0.085);
    }

    private void drawArc(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                         double u, double v, double rad, double start, double sweep, double thickness,
                         int r, int g, int b, int a) {
        if (a <= 0 || rad <= 0.0 || thickness <= 0.0) return;
        int segments = 64;
        double inner = Math.max(0.0, rad - thickness * 0.5);
        double outer = rad + thickness * 0.5;
        double d = 0.008;
        for (int i = 0; i < segments; i++) {
            double a0 = start + sweep * i / segments;
            double a1 = start + sweep * (i + 1) / segments;
            colorVert(buf, m, c, p, n, u + outer * Math.cos(a0), v + outer * Math.sin(a0), d, r, g, b, a);
            colorVert(buf, m, c, p, n, u + outer * Math.cos(a1), v + outer * Math.sin(a1), d, r, g, b, a);
            colorVert(buf, m, c, p, n, u + inner * Math.cos(a1), v + inner * Math.sin(a1), d, r, g, b, a);
            colorVert(buf, m, c, p, n, u + inner * Math.cos(a0), v + inner * Math.sin(a0), d, r, g, b, a);
        }
    }

    private void drawBottomCornerHandle(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                                        double u, double v, boolean rightSide,
                                        double controlScale, float reveal) {
        if (reveal <= 0.01f) return;
        double growth = 0.82 + 0.18 * reveal;
        double radius = 0.030 * growth * controlScale;
        double leg = 0.072 * growth * controlScale;
        double gap = 0.010 * controlScale;
        double outerU = u + (rightSide ? gap : -gap);
        double bottomV = v - gap;
        double centerU = outerU + (rightSide ? -radius : radius);
        double centerV = bottomV + radius;
        double start = rightSide ? 0.0 : Math.PI;
        double sweep = rightSide ? -Math.PI * 0.5 : Math.PI * 0.5;
        double thickness = 0.0074 * controlScale;

        drawThickLine(buf, m, c, p, n,
                outerU, v + leg, outerU, centerV,
                thickness, 245, 247, 251, (int) (238 * reveal));
        drawArc(buf, m, c, p, n, centerU, centerV, radius, start, sweep,
                thickness, 245, 247, 251, (int) (232 * reveal));
        double horizontalEndU = u + (rightSide ? -leg : leg);
        drawThickLine(buf, m, c, p, n,
                centerU, bottomV, horizontalEndU, bottomV,
                thickness, 245, 247, 251, (int) (238 * reveal));

        double cap = thickness * 0.52;
        fillRound(buf, m, c, p, n, 7, outerU, v + leg, cap, cap, cap,
                245, 247, 251, (int) (238 * reveal));
        fillRound(buf, m, c, p, n, 7, horizontalEndU, bottomV, cap, cap, cap,
                245, 247, 251, (int) (238 * reveal));
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
        if (cr <= 1e-6) {
            colorVert(buf, m, c, p, n, cu - hw, cv + hh, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu + hw, cv + hh, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu + hw, cv - hh, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu - hw, cv - hh, d, r, g, b, a);
            return;
        }

        // Sample the round caps by angle. A uniform strip count spends almost
        // all vertices on the straight center and leaves the actual radius faceted.
        List<Double> xPositions = new ArrayList<>(112);
        int capSegments = 48;
        for (int i = 0; i <= capSegments; i++) {
            double angle = Math.PI * 0.5 * i / capSegments;
            xPositions.add(-hw + cr - cr * Math.cos(angle));
        }
        double straightLeft = -hw + cr;
        double straightRight = hw - cr;
        if (straightRight - straightLeft > 1e-6) {
            int straightSegments = MathHelper.clamp(
                    (int) Math.ceil((straightRight - straightLeft) * 96.0), 1, 384);
            for (int i = 1; i <= straightSegments; i++) {
                xPositions.add(straightLeft + (straightRight - straightLeft) * i / straightSegments);
            }
        }
        for (int i = 1; i <= capSegments; i++) {
            double angle = Math.PI * 0.5 * i / capSegments;
            xPositions.add(straightRight + cr * Math.sin(angle));
        }

        for (int i = 0; i + 1 < xPositions.size(); i++) {
            double x0 = xPositions.get(i);
            double x1 = xPositions.get(i + 1);
            double y0 = roundedHalfHeight(x0, hw, hh, cr);
            double y1 = roundedHalfHeight(x1, hw, hh, cr);
            colorVert(buf, m, c, p, n, cu + x0, cv + y0, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu + x1, cv + y1, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu + x1, cv - y1, d, r, g, b, a);
            colorVert(buf, m, c, p, n, cu + x0, cv - y0, d, r, g, b, a);
        }
    }

    private static double roundedHalfHeight(double x, double hw, double hh, double radius) {
        double ax = Math.abs(x);
        double straight = hw - radius;
        if (ax <= straight) return hh;
        double dx = Math.min(radius, ax - straight);
        return hh - radius + Math.sqrt(Math.max(0.0, radius * radius - dx * dx));
    }

    private void screenTexVert(BufferBuilder buf, Matrix4f m, Matrix4f proj, Vec3d c, Panel p, Vec3d n,
                               double u, double v, double d, float alpha) {
        double mappedU = u;
        double mappedDepth = d;
        double angle = curveAngle();
        if (angle >= 1e-5 && p.halfW >= 1e-5) {
            double radius = p.halfW / angle;
            double theta = u / radius;
            mappedU = radius * Math.sin(theta);
            mappedDepth += radius * (1.0 - Math.cos(theta));
        }
        double x = c.x + p.right.x * mappedU + p.up.x * v + n.x * mappedDepth;
        double y = c.y + p.right.y * mappedU + p.up.y * v + n.y * mappedDepth;
        double z = c.z + p.right.z * mappedU + p.up.z * v + n.z * mappedDepth;

        org.joml.Vector4f clip = new org.joml.Vector4f((float) x, (float) y, (float) z, 1.0f);
        m.transform(clip);
        if (proj != null) {
            proj.transform(clip);
        }
        float invW = clip.w != 0 ? (1.0f / clip.w) : 1.0f;
        float tu = MathHelper.clamp((clip.x * invW) * 0.5f + 0.5f, 0f, 1f);
        float tv = MathHelper.clamp((clip.y * invW) * 0.5f + 0.5f, 0f, 1f);

        buf.vertex(m, (float) x, (float) y, (float) z).texture(tu, tv).color(1f, 1f, 1f, alpha);
    }

    private void colorVert(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                           double u, double v, double d, int r, int g, int b, int a) {
        double mappedU = u;
        double mappedDepth = d;
        double angle = curveAngle();
        if (angle >= 1e-5 && p.halfW >= 1e-5) {
            double radius = p.halfW / angle;
            double theta = u / radius;
            mappedU = radius * Math.sin(theta);
            mappedDepth += radius * (1.0 - Math.cos(theta));
        }
        double x = c.x + p.right.x * mappedU + p.up.x * v + n.x * mappedDepth;
        double y = c.y + p.right.y * mappedU + p.up.y * v + n.y * mappedDepth;
        double z = c.z + p.right.z * mappedU + p.up.z * v + n.z * mappedDepth;
        buf.vertex(m, (float) x, (float) y, (float) z).color(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    private void texVert(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                         double u, double v, double d, float texU, float texV, float alpha) {
        double mappedU = u;
        double mappedDepth = d;
        double angle = curveAngle();
        if (angle >= 1e-5 && p.halfW >= 1e-5) {
            double radius = p.halfW / angle;
            double theta = u / radius;
            mappedU = radius * Math.sin(theta);
            mappedDepth += radius * (1.0 - Math.cos(theta));
        }
        double x = c.x + p.right.x * mappedU + p.up.x * v + n.x * mappedDepth;
        double y = c.y + p.right.y * mappedU + p.up.y * v + n.y * mappedDepth;
        double z = c.z + p.right.z * mappedU + p.up.z * v + n.z * mappedDepth;
        buf.vertex(m, (float) x, (float) y, (float) z).texture(texU, texV).color(1f, 1f, 1f, alpha);
    }

    private double curveAngle() {
        return Math.max(0.0, curveSetting.get());
    }

    private double[] curvedLocal(Panel p, double u) {
        double angle = curveAngle();
        if (angle < 1e-5 || p.halfW < 1e-5) return new double[]{u, 0.0};
        double radius = p.halfW / angle;
        double theta = u / radius;
        return new double[]{radius * Math.sin(theta), radius * (1.0 - Math.cos(theta))};
    }

    private Vec3d panelPoint(Panel p, double u, double v) {
        Vec3d normal = p.right.crossProduct(p.up).normalize();
        double[] curved = curvedLocal(p, u);
        return p.center.add(p.right.multiply(curved[0]))
                .add(p.up.multiply(v))
                .add(normal.multiply(curved[1]));
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
        Vec3d targetCenter = Vec3d.ZERO;
        Vec3d moveVelocity = Vec3d.ZERO;
        Vec3d right = new Vec3d(1, 0, 0);
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d targetRight = new Vec3d(1, 0, 0);
        Vec3d targetUp = new Vec3d(0, 1, 0);
        double halfW, halfH;
        double targetHalfW, targetHalfH;
        double widthVelocity, heightVelocity;
        String url = "";
        float anim;
        float animVelocity;
        float barReveal;
        final float[] edgeReveal = new float[2];
        int hoveredRegion = -1;
        boolean closing;
        boolean addressEditing;
        boolean addressSelectAll;
        String addressDraft = "";
        int addressCursor;
        Object browser;
        int texW, texH;
        long lastCameraBootstrapMs;
        long lastCameraFrameSequence;
    }
}
