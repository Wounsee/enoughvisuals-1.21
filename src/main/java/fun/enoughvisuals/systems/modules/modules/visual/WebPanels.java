package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import fun.enoughvisuals.systems.network.RemoteConfig;
import fun.enoughvisuals.systems.hud.HudNotificationCenter;
import fun.enoughvisuals.systems.webpanel.WebBrowserBridge;
import fun.enoughvisuals.systems.webpanel.MinecraftVirtualCamera;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.render.WebPanelShaders;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.net.URLEncoder;
import java.net.URI;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Premium Apple Vision style spatial web browser module.
 * Features gently curved 3D glass panels, live Chromium rendering,
 * browser controls (Back, Forward, Refresh, Address), and fluid interaction.
 */
@Register(
        name = "WebPanels",
        id = "web_panels",
        description = "Плавающие окна браузера в мире (Apple Vision)",
        category = Category.CLIENT
)
public class WebPanels extends Module {

    private static final int MAX_PANELS = 10;
    private static final int MAX_TABS = 6;
    private static final double MIN_HW = 0.65, MAX_HW = 3.4;
    private static final double MIN_HH = 0.38, MAX_HH = 2.2;
    private static final double BAR_GAP = 0.055;
    private static final double NAV_GAP = 0.115;
    private static final int PANEL_SEGMENTS = 256;
    // Keep the complete /v1/sync JSON body below the launcher's 2 MiB bridge limit.
    private static final int MAX_SHARED_FRAME_BYTES = 1_150_000;

    private static WebPanels instance;

    private final StringSetting defaultUrl = add(new StringSetting(
            "Стартовый сайт", "default_url", "URL открываемый при создании окна", "https://www.google.com"));
    private final BindSetting createBind = add(new BindSetting(
            "Создать окно", "create_bind", "Клавиша для создания нового окна", GLFW.GLFW_KEY_UNKNOWN));
    private final SliderSetting curveSetting = add(new SliderSetting(
            "Изгиб", "curve", "Мягкий цилиндрический изгиб окна", 0.11f, 0.0f, 0.22f, 0.01f));
    private final BooleanSetting streamGui = add(new BooleanSetting(
            "Транслировать GUI", "stream_gui",
            "Показывать в виртуальной камере чат, HUD, инвентарь и другие экраны", false));
    private final BooleanSetting shareMyScreens = add(new BooleanSetting(
            "Показывать мои экраны другим", "share_my_screens",
            "Транслировать окна другим пользователям EnoughVisuals на этом сервере", true));
    private final BooleanSetting showOtherScreens = add(new BooleanSetting(
            "Показывать чужие экраны", "show_other_screens",
            "Отображать окна других пользователей EnoughVisuals", true));
    private final ModeSetting videoQuality = add(new ModeSetting(
            "Качество трансляции", "video_quality", "Разрешение кадров для других игроков",
            "Низкое", "Низкое", "Среднее", "Высокое", "Максимум"));
    private final BooleanSetting showTopPanel = add(new BooleanSetting(
            "Показывать верхнюю панель", "show_top_panel",
            "Показывать вкладки, адрес и кнопки навигации", true));
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
    private int queuedMouseButton = -1;
    private int queuedMouseAction = -1;
    private boolean browserLeftHeld;
    private boolean browserRightHeld;
    private Panel focusedPanel;
    private final Set<Integer> capturedKeys = new HashSet<>();
    private boolean prevCreateHeld;
    private long lastFrameNanos = System.nanoTime();
    private final Map<String, Panel> remotePanels = new HashMap<>();
    private final Map<String, Long> knownRemoteFrames = new ConcurrentHashMap<>();
    private final Map<String, Long> loadingRemoteFrames = new ConcurrentHashMap<>();
    private final Map<Integer, SharedFrame> pendingSharedFrames = new ConcurrentHashMap<>();
    private volatile List<SharedPanelState> sharedPanelSnapshot = List.of();
    private final AtomicBoolean sharedCaptureInFlight = new AtomicBoolean(false);
    private final AtomicLong sharedFrameSequence = new AtomicLong();
    private ByteBuffer sharedReadbackBuffer;
    private long nextSharedCaptureMs;
    private int sharedCaptureCursor;
    private int sharedOutgoingCursor;

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

    public boolean streamVirtualCameraGui() {
        return streamGui.get();
    }

    public boolean isHovering() {
        return isEnabled() && (hovering || grabbed != null || rotatingPanel != null);
    }

    public boolean isMoving() {
        return grabbed != null && grabMode == 1;
    }

    /** Keeps exact GLFW press/release edges instead of relying only on render-frame polling. */
    public boolean handleBrowserMouseButton(int button, int action) {
        if (!isEnabled() || (button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)) return false;
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) return false;
        if (!hovering && pressedPanel == null) return false;
        queuedMouseButton = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 0 : 1;
        queuedMouseAction = action;
        if (queuedMouseButton == 0) browserLeftHeld = action == GLFW.GLFW_PRESS;
        else browserRightHeld = action == GLFW.GLFW_PRESS;
        return true;
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
            forwardScroll(MathHelper.clamp(yOffset, -2.0, 2.0));
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
            if (panel.renderVisible && !panel.closing && panel.hoveredRegion >= 0) return panel;
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
            WebBrowserBridge.mouseWheel(contentPanel.browser, px[0], px[1], yOffset * 5.5);
        }
    }

    /** Adds privacy preferences, panel transforms and at most one fresh video frame to /v1/sync. */
    public void appendSync(JsonObject root) {
        JsonObject web = new JsonObject();
        boolean publish = isEnabled() && shareMyScreens.get();
        boolean receive = isEnabled() && showOtherScreens.get();
        web.addProperty("publish", publish);
        web.addProperty("receive", receive);
        web.addProperty("mode_2d", false);

        JsonObject known = new JsonObject();
        if (receive) {
            knownRemoteFrames.forEach(known::addProperty);
        }
        web.add("known", known);

        JsonArray states = new JsonArray();
        SharedFrame outgoingFrame = null;
        if (publish && !pendingSharedFrames.isEmpty()) {
            List<Integer> queued = pendingSharedFrames.keySet().stream().sorted().toList();
            if (!queued.isEmpty()) {
                Integer selected = queued.get(Math.floorMod(sharedOutgoingCursor++, queued.size()));
                outgoingFrame = pendingSharedFrames.remove(selected);
            }
        }
        if (publish) {
            for (SharedPanelState state : sharedPanelSnapshot) {
                JsonObject panel = new JsonObject();
                panel.addProperty("id", state.id());
                panel.add("center", vectorJson(state.center()));
                panel.add("right", vectorJson(state.right()));
                panel.add("up", vectorJson(state.up()));
                panel.addProperty("half_w", state.halfW());
                panel.addProperty("half_h", state.halfH());
                panel.addProperty("curve", state.curve());
                panel.addProperty("url", state.url());
                JsonArray tabs = new JsonArray();
                for (String tab : state.tabs()) tabs.add(tab);
                panel.add("tabs", tabs);
                panel.addProperty("active_tab", state.activeTab());
                panel.addProperty("frame_revision", state.frameRevision());
                if (outgoingFrame != null && outgoingFrame.panelId() == state.id()) {
                    panel.addProperty("frame", outgoingFrame.base64());
                    panel.addProperty("frame_revision", outgoingFrame.revision());
                }
                states.add(panel);
            }
        }
        web.add("panels", states);
        root.add("web_panels", web);
    }

    /** Applies other users' same-server panel metadata and asynchronously uploads changed JPEG frames. */
    public void applySync(JsonObject root) {
        if (!isEnabled() || !showOtherScreens.get()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) client.execute(this::clearRemotePanels);
            return;
        }
        JsonElement element = root.get("web_panels");
        if (element == null || !element.isJsonArray()) return;

        List<RemotePanelUpdate> updates = new ArrayList<>();
        for (JsonElement ownerElement : element.getAsJsonArray()) {
            try {
                JsonObject owner = ownerElement.getAsJsonObject();
                String uuid = normalizeRemoteKey(owner.get("uuid").getAsString());
                String username = owner.has("username") ? owner.get("username").getAsString() : "";
                if (uuid.isBlank() || !owner.has("panels") || !owner.get("panels").isJsonArray()) continue;
                for (JsonElement panelElement : owner.getAsJsonArray("panels")) {
                    JsonObject panel = panelElement.getAsJsonObject();
                    int id = panel.get("id").getAsInt();
                    if (id <= 0) continue;
                    String key = uuid + ":" + id;
                    List<String> tabs = new ArrayList<>();
                    if (panel.has("tabs") && panel.get("tabs").isJsonArray()) {
                        for (JsonElement tab : panel.getAsJsonArray("tabs")) {
                            if (tabs.size() >= MAX_TABS) break;
                            tabs.add(tab.getAsString());
                        }
                    }
                    long revision = panel.has("frame_revision") ? panel.get("frame_revision").getAsLong() : 0L;
                    String frame = panel.has("frame") ? panel.get("frame").getAsString() : "";
                    RemotePanelUpdate update = new RemotePanelUpdate(
                            key, uuid, username, id,
                            vector(panel.getAsJsonArray("center")),
                            vector(panel.getAsJsonArray("right")),
                            vector(panel.getAsJsonArray("up")),
                            panel.get("half_w").getAsDouble(), panel.get("half_h").getAsDouble(),
                            panel.get("curve").getAsDouble(),
                            panel.has("url") ? panel.get("url").getAsString() : "",
                            List.copyOf(tabs), panel.has("active_tab") ? panel.get("active_tab").getAsInt() : 0,
                            revision);
                    updates.add(update);
                    if (!frame.isBlank() && frame.length() <= 2_000_000
                            && revision > knownRemoteFrames.getOrDefault(key, 0L)) {
                        decodeRemoteFrame(key, revision, frame);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) client.execute(() -> applyRemoteMetadata(updates));
    }

    private void applyRemoteMetadata(List<RemotePanelUpdate> updates) {
        Set<String> seen = new HashSet<>();
        for (RemotePanelUpdate update : updates) {
            seen.add(update.key());
            Panel panel = remotePanels.computeIfAbsent(update.key(), key -> {
                Panel created = new Panel();
                created.remote = true;
                created.remoteKey = key;
                created.anim = 1.0f;
                created.center = update.center();
                created.right = update.right().normalize();
                created.up = update.up().normalize();
                created.halfW = update.halfW();
                created.halfH = update.halfH();
                return created;
            });
            panel.id = update.id();
            panel.ownerName = update.username();
            panel.targetCenter = update.center();
            panel.targetRight = update.right().normalize();
            panel.targetUp = update.up().normalize();
            panel.targetHalfW = MathHelper.clamp(update.halfW(), MIN_HW, MAX_HW);
            panel.targetHalfH = MathHelper.clamp(update.halfH(), MIN_HH, MAX_HH);
            panel.remoteCurve = MathHelper.clamp(update.curve(), 0.0, 0.5);
            panel.url = update.url();
            List<String> tabUrls = update.tabs().isEmpty() ? List.of(update.url()) : update.tabs();
            boolean tabsChanged = panel.tabs.size() != tabUrls.size();
            if (!tabsChanged) {
                for (int i = 0; i < tabUrls.size(); i++) {
                    if (!java.util.Objects.equals(panel.tabs.get(i).url, tabUrls.get(i))) {
                        tabsChanged = true;
                        break;
                    }
                }
            }
            if (tabsChanged) {
                panel.tabs.clear();
                for (String tabUrl : tabUrls) panel.tabs.add(new Tab(tabUrl, true));
                double width = tabWidth(panel);
                for (int i = 0; i < panel.tabs.size(); i++) {
                    panel.tabs.get(i).visualCenter = tabCenterU(panel, i);
                    panel.tabs.get(i).visualWidth = width;
                }
            }
            panel.activeTab = MathHelper.clamp(update.activeTab(), 0, Math.max(0, panel.tabs.size() - 1));
            panel.lastRemoteSeenMs = System.currentTimeMillis();
        }
        List<String> stale = new ArrayList<>();
        for (String key : remotePanels.keySet()) {
            if (!seen.contains(key)) stale.add(key);
        }
        for (String key : stale) removeRemotePanel(key);
    }

    private void decodeRemoteFrame(String key, long revision, String encoded) {
        Long loading = loadingRemoteFrames.putIfAbsent(key, revision);
        if (loading != null && loading >= revision) return;
        loadingRemoteFrames.put(key, revision);
        CompletableFuture.runAsync(() -> {
            NativeImage image = null;
            try {
                byte[] bytes = Base64.getDecoder().decode(encoded);
                if (bytes.length == 0 || bytes.length > MAX_SHARED_FRAME_BYTES) return;
                image = NativeImage.read(new ByteArrayInputStream(bytes));
                NativeImage ready = image;
                image = null;
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) client.execute(() -> registerRemoteFrame(client, key, revision, ready));
                else ready.close();
            } catch (Exception ignored) {
            } finally {
                if (image != null) image.close();
                loadingRemoteFrames.remove(key, revision);
            }
        });
    }

    private void registerRemoteFrame(MinecraftClient client, String key, long revision, NativeImage image) {
        if (revision <= knownRemoteFrames.getOrDefault(key, 0L)) {
            image.close();
            return;
        }
        Panel panel = remotePanels.get(key);
        if (panel == null) {
            image.close();
            return;
        }
        Identifier textureId = Identifier.of("enough", "webpanels/remote_"
                + Integer.toUnsignedString(key.hashCode(), 36));
        if (panel.remoteTexture != null) client.getTextureManager().destroyTexture(panel.remoteTexture);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        texture.setFilter(true, false);
        client.getTextureManager().registerTexture(textureId, texture);
        panel.remoteTexture = textureId;
        panel.remoteFrameRevision = revision;
        knownRemoteFrames.put(key, revision);
    }

    private void clearRemotePanels() {
        for (String key : new ArrayList<>(remotePanels.keySet())) removeRemotePanel(key);
        knownRemoteFrames.clear();
        loadingRemoteFrames.clear();
    }

    private void removeRemotePanel(String key) {
        Panel panel = remotePanels.remove(key);
        if (panel != null && panel.remoteTexture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(panel.remoteTexture);
        }
        knownRemoteFrames.remove(key);
        loadingRemoteFrames.remove(key);
    }

    private static JsonArray vectorJson(Vec3d vector) {
        JsonArray array = new JsonArray();
        array.add(vector.x);
        array.add(vector.y);
        array.add(vector.z);
        return array;
    }

    private static Vec3d vector(JsonArray array) {
        if (array == null || array.size() != 3) return Vec3d.ZERO;
        return new Vec3d(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    private static String normalizeRemoteKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT).replace("-", "");
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
        browserLeftHeld = false;
        browserRightHeld = false;
        clearRemotePanels();
        sharedPanelSnapshot = List.of();
        pendingSharedFrames.clear();
        RemoteConfig.getInstance().requestImmediateSync();
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
        int panelLimit = shareMyScreens.get() ? 2 : MAX_PANELS;
        if (mc.player == null || activeCount() >= panelLimit) {
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
        Tab firstTab = new Tab(defaultUrl.get(), true);
        p.tabs.add(firstTab);
        p.activeTab = 0;
        p.url = firstTab.url;
        firstTab.visualCenter = tabCenterU(p, 0);
        firstTab.visualWidth = tabWidth(p);
        panels.add(p);
    }

    private int activeCount() {
        int n = 0;
        for (Panel p : panels) {
            if (!p.closing) n++;
        }
        return n;
    }

    private Tab activeTab(Panel p) {
        if (p.tabs.isEmpty()) p.tabs.add(new Tab(defaultUrl.get()));
        p.activeTab = MathHelper.clamp(p.activeTab, 0, p.tabs.size() - 1);
        return p.tabs.get(p.activeTab);
    }

    private void persistActiveTab(Panel p) {
        Tab tab = activeTab(p);
        tab.browser = p.browser;
        tab.url = p.url;
        tab.lastCameraBootstrapMs = p.lastCameraBootstrapMs;
        tab.lastCameraFrameSequence = p.lastCameraFrameSequence;
    }

    private void activateTab(Panel p, int index) {
        if (index < 0 || index >= p.tabs.size() || index == p.activeTab) return;
        exitFocusIf(p);
        persistActiveTab(p);
        p.activeTab = index;
        Tab tab = activeTab(p);
        p.browser = tab.browser;
        p.url = tab.url;
        p.addressDraft = p.url == null ? "" : p.url;
        p.addressCursor = p.addressDraft.length();
        p.addressEditing = false;
        p.addressSelectAll = false;
        p.lastCameraBootstrapMs = tab.lastCameraBootstrapMs;
        p.lastCameraFrameSequence = tab.lastCameraFrameSequence;
    }

    private void createTab(Panel p) {
        if (p.tabs.size() >= MAX_TABS) return;
        exitFocusIf(p);
        persistActiveTab(p);
        p.tabs.add(new Tab(defaultUrl.get()));
        Tab created = p.tabs.get(p.tabs.size() - 1);
        created.visualCenter = tabAddU(p);
        created.visualWidth = 0.0;
        p.activeTab = p.tabs.size() - 1;
        Tab tab = activeTab(p);
        p.browser = null;
        p.url = tab.url;
        p.addressDraft = p.url;
        p.addressCursor = p.addressDraft.length();
        p.lastCameraBootstrapMs = 0L;
        p.lastCameraFrameSequence = 0L;
    }

    private void closeTab(Panel p, int index) {
        if (index < 0 || index >= p.tabs.size()) return;
        exitFocusIf(p);
        persistActiveTab(p);
        int previousActive = p.activeTab;
        Tab removed = p.tabs.remove(index);
        p.closingTabs.add(new ClosingTab(tabTitle(removed.url), removed.visualCenter,
                Math.max(0.0, removed.visualWidth), Math.max(0.0f, removed.reveal)));
        if (removed.browser != null) WebBrowserBridge.close(removed.browser);
        if (p.tabs.isEmpty()) p.tabs.add(new Tab(defaultUrl.get()));
        int next = index == previousActive
                ? Math.min(index, p.tabs.size() - 1)
                : previousActive - (index < previousActive ? 1 : 0);
        p.activeTab = next;
        Tab tab = activeTab(p);
        p.browser = tab.browser;
        p.url = tab.url;
        p.addressDraft = p.url == null ? "" : p.url;
        p.addressCursor = p.addressDraft.length();
        p.addressEditing = false;
        p.addressSelectAll = false;
        p.lastCameraBootstrapMs = tab.lastCameraBootstrapMs;
        p.lastCameraFrameSequence = tab.lastCameraFrameSequence;
    }

    private static String tabTitle(String url) {
        if (url == null || url.isBlank()) return "Новая вкладка";
        try {
            String host = URI.create(url).getHost();
            if (host != null && !host.isBlank()) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return url;
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
        activeTab(p).url = url;
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
        if (instance == null || !instance.isEnabled()
                || instance.panels.isEmpty() && instance.remotePanels.isEmpty()) return;
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
        for (Panel p : panels) {
            animatePanel(p, frameDt);
        }
        for (Panel p : remotePanels.values()) {
            animatePanel(p, frameDt);
        }
        for (Panel p : panels) updatePanelVisibility(context, p, eye);
        for (Panel p : remotePanels.values()) updatePanelVisibility(context, p, eye);
        handleInput(eye, look);
        updateSharedPanelSnapshot();

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        Matrix4f proj = context.projectionMatrix();

        // Upkeep MCEF browsers and pump message loop
        WebBrowserBridge.runMessageLoop();
        for (Panel p : panels) {
            if (!p.closing) {
                if (p.renderVisible) {
                    ensureBrowser(p);
                    syncBrowserLocation(p);
                    pumpVirtualCamera(p);
                }
                updateBrowserVisibility(p, p.renderVisible);
            }
        }
        maybeCaptureSharedFrame();

        List<Panel> visiblePanels = new ArrayList<>();
        for (Panel panel : panels) if (panel.geometryVisible) visiblePanels.add(panel);
        if (showOtherScreens.get()) {
            for (Panel panel : remotePanels.values()) if (panel.geometryVisible) visiblePanels.add(panel);
        }

        // Capture in-world blur frame if theme blur enabled
        if (!visiblePanels.isEmpty() && !CustomTheme.blurDisabled()) {
            GlassUtil.BLUR_OFFSET = CustomTheme.hudBlurLevel();
            GlassUtil.beginFrame();
        }

        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean oldDepthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
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
                RenderSystem.setShaderTexture(0, blurTex);
                GlStateManager._bindTexture(blurTex);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                for (Panel p : visiblePanels) {
                    if (p.anim > 0.01f && p.renderVisible) {
                        float a = ease(p.anim);
                        double scale = 0.88 + 0.12 * a;
                        double hw = p.halfW * scale;
                        double hh = p.halfH * scale;
                        WebPanelShaders.useBlur((float) (hw * 2.0), (float) (hh * 2.0),
                                (float) smartCorner(hw, hh));
                        BufferBuilder blurBuf = Tessellator.getInstance().begin(
                                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                        drawCurvedFrostedBlur(blurBuf, matrix, proj, cam, p);
                        BuiltBuffer builtBlur = blurBuf.endNullable();
                        if (builtBlur != null) BufferRenderer.drawWithGlobalProgram(builtBlur);
                    }
                }
            }

            // 2. Analytic SDF shell. The silhouette is evaluated per fragment,
            // so curved corners stay smooth without polygon stair-steps.
            for (Panel p : visiblePanels) {
                if (p.anim <= 0.01f) continue;
                float a = ease(p.anim);
                double scale = 0.88 + 0.12 * a;
                double hw = p.halfW * scale;
                double hh = p.halfH * scale;
                if (isFrontFacing(p, eye)) {
                    if (blurTex <= 0 || CustomTheme.blurDisabled()) {
                        drawSdfSurface(matrix, cam, p, hw, hh, 0.0015,
                                0x10, 0x13, 0x1A, (int) (242 * a));
                    }
                } else {
                    drawSdfSurface(matrix, cam, p, hw, hh, 0.0,
                            0x0C, 0x0E, 0x13, (int) (248 * a));
                }
            }

            for (Panel p : visiblePanels) {
                if (p.anim > 0.01f && p.renderVisible) {
                    drawPanelSdfControls(matrix, cam, p, hit(p, eye, look));
                }
            }

            // 3. Independent controls and loading ornaments.
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder colorBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Panel p : visiblePanels) {
                if (p.anim > 0.01f && p.renderVisible) {
                    drawPanelChrome(colorBuf, matrix, cam, p);
                }
            }
            BuiltBuffer builtColor = colorBuf.endNullable();
            if (builtColor != null) {
                BufferRenderer.drawWithGlobalProgram(builtColor);
            }

            // 4. Chromium content uses the same analytic SDF mask as the shell.
            for (Panel p : visiblePanels) {
                int glTex = panelTextureId(p);
                if (glTex > 0 && p.anim > 0.01f && p.renderVisible) {
                    float a = ease(p.anim);
                    double scale = 0.88 + 0.12 * a;
                    double hw = p.halfW * scale;
                    double hh = p.halfH * scale;
                    WebPanelShaders.useTexture((float) (hw * 2.0), (float) (hh * 2.0),
                            (float) smartCorner(hw, hh));
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

            // 5. The controls and address use the real MTSDF atlases in world
            // space. This pass replaces all hand-built arrow/globe/trash paths.
            for (Panel p : visiblePanels) {
                if (p.anim > 0.01f && p.renderVisible) {
                    drawPanelMtsdf(matrix, cam, p, hit(p, eye, look));
                }
            }
        } finally {
            RenderSystem.depthMask(oldDepthMask);
            RenderSystem.depthFunc(oldDepthFunc);
            if (oldDepthEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
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

    private void updatePanelVisibility(WorldRenderContext context, Panel panel, Vec3d eye) {
        if (panel.closing || panel.anim <= 0.005f) {
            panel.geometryVisible = false;
            panel.renderVisible = false;
            return;
        }
        double radius = Math.sqrt(panel.halfW * panel.halfW + panel.halfH * panel.halfH) + 0.28;
        if (eye.squaredDistanceTo(panel.center) > (24.0 + radius) * (24.0 + radius)) {
            panel.geometryVisible = false;
            panel.renderVisible = false;
            return;
        }
        Box bounds = new Box(
                panel.center.x - radius, panel.center.y - radius, panel.center.z - radius,
                panel.center.x + radius, panel.center.y + radius, panel.center.z + radius);
        if (context.frustum() != null && !context.frustum().isVisible(bounds)) {
            panel.geometryVisible = false;
            panel.renderVisible = false;
            return;
        }
        panel.geometryVisible = true;
        if (panel == grabbed || panel == rotatingPanel || panel == pressedPanel) {
            panel.renderVisible = true;
            return;
        }
        if (!isFrontFacing(panel, eye)) {
            panel.renderVisible = false;
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= panel.nextOcclusionCheckMs) {
            panel.notOccluded = panelHasLineOfSight(panel, eye);
            panel.nextOcclusionCheckMs = now + 120L;
        }
        panel.renderVisible = panel.notOccluded;
    }

    private boolean panelHasLineOfSight(Panel panel, Vec3d eye) {
        double u = panel.halfW * 0.78;
        double v = panel.halfH * 0.78;
        return clearLineOfSight(eye, panel.center)
                || clearLineOfSight(eye, panel.center.add(panel.right.multiply(u)).add(panel.up.multiply(v)))
                || clearLineOfSight(eye, panel.center.subtract(panel.right.multiply(u)).add(panel.up.multiply(v)))
                || clearLineOfSight(eye, panel.center.add(panel.right.multiply(u)).subtract(panel.up.multiply(v)))
                || clearLineOfSight(eye, panel.center.subtract(panel.right.multiply(u)).subtract(panel.up.multiply(v)));
    }

    private boolean clearLineOfSight(Vec3d eye, Vec3d target) {
        HitResult hit = mc.world.raycast(new RaycastContext(
                eye, target, RaycastContext.ShapeType.VISUAL,
                RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS
                || eye.squaredDistanceTo(hit.getPos()) + 0.04 >= eye.squaredDistanceTo(target);
    }

    private void updateBrowserVisibility(Panel panel, boolean panelVisible) {
        Tab active = activeTab(panel);
        for (Tab tab : panel.tabs) {
            boolean visible = panelVisible && tab == active && !panel.closing;
            if (tab.browser != null && tab.browserVisible != visible) {
                WebBrowserBridge.setVisible(tab.browser, visible);
                tab.browserVisible = visible;
            }
        }
    }

    private int panelTextureId(Panel panel) {
        if (panel.remoteTexture != null) {
            AbstractTexture texture = mc.getTextureManager().getTexture(panel.remoteTexture);
            return texture == null ? 0 : texture.getGlId();
        }
        return panel.browser == null ? 0 : WebBrowserBridge.textureId(panel.browser);
    }

    private void updateSharedPanelSnapshot() {
        if (!isEnabled() || !shareMyScreens.get()) {
            sharedPanelSnapshot = List.of();
            pendingSharedFrames.clear();
            return;
        }
        List<SharedPanelState> snapshot = new ArrayList<>();
        double curve = curveSetting.get();
        for (Panel panel : panels) {
            if (panel.closing) continue;
            List<String> tabs = new ArrayList<>();
            for (Tab tab : panel.tabs) tabs.add(tab.url == null ? "" : tab.url);
            snapshot.add(new SharedPanelState(panel.id, panel.center, panel.right, panel.up,
                    panel.halfW, panel.halfH, curve, panel.url == null ? "" : panel.url,
                    List.copyOf(tabs), panel.activeTab, panel.lastSharedFrameRevision));
        }
        sharedPanelSnapshot = List.copyOf(snapshot);
    }

    private void maybeCaptureSharedFrame() {
        if (!isEnabled() || !shareMyScreens.get() || panels.isEmpty()
                || !sharedCaptureInFlight.compareAndSet(false, true)) return;
        long now = System.currentTimeMillis();
        if (now < nextSharedCaptureMs) {
            sharedCaptureInFlight.set(false);
            return;
        }
        List<Panel> candidates = panels.stream()
                .filter(panel -> panel.renderVisible && !panel.closing && panel.browser != null
                        && WebBrowserBridge.isTextureReady(panel.browser))
                .toList();
        if (candidates.isEmpty()) {
            sharedCaptureInFlight.set(false);
            return;
        }
        Panel panel = candidates.get(Math.floorMod(sharedCaptureCursor++, candidates.size()));
        int texture = WebBrowserBridge.textureId(panel.browser);
        if (texture <= 0 || panel.texW <= 0 || panel.texH <= 0) {
            sharedCaptureInFlight.set(false);
            return;
        }
        VideoProfile profile = videoProfile();
        nextSharedCaptureMs = now + profile.intervalMs();
        try {
            int required = panel.texW * panel.texH * 4;
            if (sharedReadbackBuffer == null || sharedReadbackBuffer.capacity() < required) {
                sharedReadbackBuffer = ByteBuffer.allocateDirect(required);
            }
            sharedReadbackBuffer.clear();
            int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                    sharedReadbackBuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTexture);
            BufferedImage image = downsampleBrowserFrame(sharedReadbackBuffer,
                    panel.texW, panel.texH, profile.width(), profile.height());
            long revision = sharedFrameSequence.incrementAndGet();
            CompletableFuture.runAsync(() -> encodeSharedFrame(panel, image, profile.jpegQuality(), revision));
        } catch (Throwable ignored) {
            sharedCaptureInFlight.set(false);
        }
    }

    private void encodeSharedFrame(Panel panel, BufferedImage image, float quality, long revision) {
        try {
            if (Math.abs(panel.sharedJpegProfileQuality - quality) > 0.001f) {
                panel.sharedJpegProfileQuality = quality;
                panel.sharedJpegQuality = quality;
            }
            float adaptiveQuality = panel.sharedJpegQuality > 0.0f
                    ? Math.min(quality, panel.sharedJpegQuality) : quality;
            byte[] bytes = encodeJpeg(image, adaptiveQuality);
            // Usually one encode is enough. Detailed video gets at most two
            // corrective encodes, then reuses the learned quality next frame.
            for (int attempt = 0; attempt < 2 && bytes.length > MAX_SHARED_FRAME_BYTES; attempt++) {
                float ratio = MAX_SHARED_FRAME_BYTES / (float) bytes.length;
                adaptiveQuality *= MathHelper.clamp(ratio * 0.96f, 0.45f, 0.92f);
                adaptiveQuality = Math.max(0.22f, adaptiveQuality);
                bytes = encodeJpeg(image, adaptiveQuality);
            }
            if (bytes == null || bytes.length == 0 || bytes.length > MAX_SHARED_FRAME_BYTES) return;
            panel.sharedJpegQuality = Math.min(quality, adaptiveQuality + 0.012f);
            String encoded = Base64.getEncoder().encodeToString(bytes);
            panel.lastSharedFrameRevision = revision;
            pendingSharedFrames.put(panel.id, new SharedFrame(panel.id, revision, encoded));
            RemoteConfig.getInstance().requestImmediateSync();
        } catch (Exception ignored) {
        } finally {
            sharedCaptureInFlight.set(false);
        }
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            try {
                writer.setOutput(imageOutput);
                ImageWriteParam params = writer.getDefaultWriteParam();
                if (params.canWriteCompressed()) {
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(MathHelper.clamp(quality, 0.0f, 1.0f));
                }
                writer.write(null, new IIOImage(image, null, null), params);
            } finally {
                writer.dispose();
            }
            return output.toByteArray();
        }
    }

    private static BufferedImage downsampleBrowserFrame(ByteBuffer rgba, int sourceW, int sourceH,
                                                         int targetW, int targetH) {
        BufferedImage image = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < targetH; y++) {
            int sourceY = sourceH - 1 - y * sourceH / targetH;
            for (int x = 0; x < targetW; x++) {
                int sourceX = x * sourceW / targetW;
                int offset = (sourceY * sourceW + sourceX) * 4;
                int red = rgba.get(offset) & 0xFF;
                int green = rgba.get(offset + 1) & 0xFF;
                int blue = rgba.get(offset + 2) & 0xFF;
                pixels[y * targetW + x] = red << 16 | green << 8 | blue;
            }
        }
        return image;
    }

    private VideoProfile videoProfile() {
        if (videoQuality.is("Максимум")) return new VideoProfile(1920, 1080, 0.96f, 420L);
        if (videoQuality.is("Высокое")) return new VideoProfile(1280, 720, 0.88f, 330L);
        if (videoQuality.is("Среднее")) return new VideoProfile(854, 480, 0.74f, 250L);
        return new VideoProfile(480, 270, 0.48f, 180L);
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

        animateTabs(p, dt);
    }

    private void animateTabs(Panel p, float dt) {
        double targetWidth = tabWidth(p);
        double layoutBlend = 1.0 - Math.exp(-18.0 * dt);
        for (int i = 0; i < p.tabs.size(); i++) {
            Tab tab = p.tabs.get(i);
            double targetCenter = tabCenterU(p, i);
            if (!Double.isFinite(tab.visualCenter)) tab.visualCenter = targetCenter;
            tab.visualCenter += (targetCenter - tab.visualCenter) * layoutBlend;
            tab.visualWidth += (targetWidth - tab.visualWidth) * layoutBlend;
            tab.reveal = smoothTowards(tab.reveal, 1.0f, 15.0f, dt);
        }

        for (ClosingTab tab : p.closingTabs) {
            tab.reveal = smoothTowards(tab.reveal, 0.0f, 14.0f, dt);
            tab.width += (0.0 - tab.width) * (1.0 - Math.exp(-16.0 * dt));
            tab.center += 0.018 * dt;
        }
        p.closingTabs.removeIf(tab -> tab.reveal < 0.015f || tab.width < 0.002);
    }

    private static float smoothTowards(float current, float target, float speed, float dt) {
        return current + (target - current) * (1f - (float) Math.exp(-speed * dt));
    }

    private void ensureBrowser(Panel p) {
        Tab tab = activeTab(p);
        if (p.browser == null && tab.browser != null) p.browser = tab.browser;
        if (p.browser != null || !WebBrowserBridge.isReady()) {
            return;
        }
        p.texW = 1920;
        p.texH = 1080;
        p.browser = WebBrowserBridge.create(p.url, p.texW, p.texH);
        tab.browser = p.browser;
        tab.url = p.url;
    }

    private void syncBrowserLocation(Panel p) {
        if (p.browser == null) return;
        String actualUrl = WebBrowserBridge.getUrl(p.browser);
        if (actualUrl == null || actualUrl.isBlank() || actualUrl.equals(p.url)) return;
        p.url = actualUrl;
        activeTab(p).url = actualUrl;
        if (!p.addressEditing) {
            p.addressDraft = actualUrl;
            p.addressCursor = actualUrl.length();
            p.addressSelectAll = false;
        }
    }

    private void pumpVirtualCamera(Panel p) {
        if (p.browser == null) return;
        long now = System.currentTimeMillis();
        if (now - p.lastCameraBootstrapMs >= 1_000L) {
            WebBrowserBridge.installMinecraftCamera(p.browser);
            p.lastCameraBootstrapMs = now;
            activeTab(p).lastCameraBootstrapMs = now;
        }
        long sequence = MinecraftVirtualCamera.latestFrameSequence();
        String frame = MinecraftVirtualCamera.latestFrameDataUrl();
        if (sequence > p.lastCameraFrameSequence && frame != null) {
            WebBrowserBridge.pushMinecraftCameraFrame(p.browser, frame);
            p.lastCameraFrameSequence = sequence;
            activeTab(p).lastCameraFrameSequence = sequence;
        }
    }

    private void disposePanel(Panel p) {
        persistActiveTab(p);
        Set<Object> closed = new HashSet<>();
        for (Tab tab : p.tabs) {
            if (tab.browser != null && closed.add(tab.browser)) {
                WebBrowserBridge.close(tab.browser);
            }
            tab.browser = null;
        }
        p.browser = null;
    }

    private void drawCurvedFrostedBlur(BufferBuilder buffer, Matrix4f m, Matrix4f proj, Vec3d cam, Panel p) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        double scale = 0.88 + 0.12 * a;
        double hw = p.halfW * scale;
        double hh = p.halfH * scale;
        double d = 0.0015;
        float alpha = 0.94f * a;
        int cols = PANEL_SEGMENTS;
        for (int i = 0; i < cols; i++) {
            double u0 = -hw + 2 * hw * i / cols;
            double u1 = -hw + 2 * hw * (i + 1) / cols;
            float t0 = (float) i / cols;
            float t1 = (float) (i + 1) / cols;
            screenTexVert(buffer, m, proj, c, p, n, u0, hh, d, t0, 0f, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u1, hh, d, t1, 0f, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u1, -hh, d, t1, 1f, alpha);
            screenTexVert(buffer, m, proj, c, p, n, u0, -hh, d, t0, 1f, alpha);
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
        for (int i = 0; i < cols; i++) {
            double u0 = -hw + 2 * hw * i / cols;
            double u1 = -hw + 2 * hw * (i + 1) / cols;
            float tu0 = (float) i / cols, tu1 = (float) (i + 1) / cols;

            texVert(buffer, m, c, p, n, u0, hh, d, tu0, 0f, alpha);
            texVert(buffer, m, c, p, n, u1, hh, d, tu1, 0f, alpha);
            texVert(buffer, m, c, p, n, u1, -hh, d, tu1, 1f, alpha);
            texVert(buffer, m, c, p, n, u0, -hh, d, tu0, 1f, alpha);
        }
    }

    private void handleInput(Vec3d eye, Vec3d look) {
        long win = mc.getWindow().getHandle();
        boolean left = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean rHeld = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        boolean queuedPress = queuedMouseAction == GLFW.GLFW_PRESS;
        boolean queuedRelease = queuedMouseAction == GLFW.GLFW_RELEASE;
        boolean leftPress = queuedPress ? queuedMouseButton == 0 : left && !prevLeft;
        boolean rightPress = queuedPress ? queuedMouseButton == 1 : right && !prevRight;
        if (leftPress) browserLeftHeld = true;
        if (rightPress) browserRightHeld = true;
        if (queuedMouseAction == -1) {
            if (!left && prevLeft) browserLeftHeld = false;
            if (!right && prevRight) browserRightHeld = false;
        }
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
            if (p.closing || !p.renderVisible) continue;
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
                if (p.closing || !p.renderVisible) continue;
                double[] h = hit(p, eye, look);
                if (h == null) continue;
                int region = region(p, h[0], h[1]);
                if (region == -1) continue;
                if (focusedPanel != null && focusedPanel.addressEditing && region != 5) {
                    cancelAddressEdit(focusedPanel);
                }
                if (region == 40) {
                    createTab(p);
                    uiPressHandled = true;
                } else if (region >= 30 && region < 30 + MAX_TABS) {
                    closeTab(p, region - 30);
                    uiPressHandled = true;
                } else if (region >= 20 && region < 20 + MAX_TABS) {
                    activateTab(p, region - 20);
                    uiPressHandled = true;
                } else if (region == 3) {
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
            boolean pressedButtonHeld = !queuedRelease
                    && (pressedMouseButton == 0 ? browserLeftHeld : browserRightHeld);
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
        queuedMouseButton = -1;
        queuedMouseAction = -1;

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

        double angle = curveAngle(p);
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
        if (!unbounded && (Math.abs(lu) > p.halfW + 0.36 || Math.abs(lv) > p.halfH + 0.52)) return null;
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

    private double tabsY(Panel p) {
        return navY(p) + 0.092 * controlScale(p);
    }

    private double tabAddU(Panel p) {
        return navHalfWidth(p) - 0.050 * controlScale(p);
    }

    private double tabWidth(Panel p) {
        double control = controlScale(p);
        double left = -navHalfWidth(p) + 0.018 * control;
        double right = tabAddU(p) - 0.060 * control;
        return Math.min(0.270 * control, Math.max(0.12 * control,
                (right - left) / Math.max(1, p.tabs.size())));
    }

    private double tabCenterU(Panel p, int index) {
        double control = controlScale(p);
        double left = -navHalfWidth(p) + 0.018 * control;
        return left + tabWidth(p) * (index + 0.5);
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

        if (showTopPanel.get()) {
            double tabsY = tabsY(p);
            if (Math.abs(lv - tabsY) < 0.048 * control) {
                if (Math.abs(lu - tabAddU(p)) < 0.040 * control
                        && p.tabs.size() < MAX_TABS) return 40;
                for (int i = 0; i < p.tabs.size(); i++) {
                    Tab tab = p.tabs.get(i);
                    double width = Math.max(0.01 * control, tab.visualWidth);
                    double center = tab.visualCenter;
                    if (Math.abs(lu - center) > width * 0.5) continue;
                    if (lu > center + width * 0.5 - 0.052 * control) return 30 + i;
                    return 20 + i;
                }
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
        }

        // Resize exists only at the two lower corners, matching the requested
        // outline corner controls instead of a ring around the perimeter.
        double cornerRadius = 0.095 * control;
        if (Math.hypot(lu + p.halfW, lv + p.halfH) < cornerRadius) return 10;
        if (Math.hypot(lu - p.halfW, lv + p.halfH) < cornerRadius) return 11;

        if (Math.abs(lu) <= p.halfW && Math.abs(lv) <= p.halfH) {
            return 0; // content
        }
        return -1;
    }

    private void drawPanelChrome(BufferBuilder buf, Matrix4f m, Vec3d cam, Panel p) {
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

        // Loading state remains understated and monochrome.
        int glTex = p.browser != null ? WebBrowserBridge.textureId(p.browser) : 0;
        if (glTex <= 0) {
            float timeSec = (System.currentTimeMillis() % 100000L) / 1000f;
            drawSpinner(buf, m, c, p, n, 0, 0.035, 0.105, timeSec * 1.25f,
                    alr, alg, alb, (int) (205 * a));
        }

        if (p.addressEditing && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            double abHalfW = Math.min(hw * 0.62, 0.86 * control);
            double navY = hh + NAV_GAP * control;
            double reloadU = -abHalfW + 0.285 * control;
            double pillLeft = reloadU + 0.060 * control;
            double pillRight = abHalfW - 0.028 * control;
            double pillU = (pillLeft + pillRight) * 0.5;
            double pillHW = (pillRight - pillLeft) * 0.5;
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

    private void drawPanelSdfControls(Matrix4f matrix, Vec3d cam, Panel p, double[] hit) {
        float a = ease(p.anim);
        double scale = 0.88 + 0.12 * a;
        double hw = p.halfW * scale;
        double hh = p.halfH * scale;
        double control = controlScale(p) * scale;
        float reveal = p.barReveal;
        double barY = -hh - BAR_GAP * control;

        drawSdfRoundedSurface(matrix, cam, p, -0.055 * control * reveal, barY,
                0.140 * control, 0.014 * control, 0.014 * control, 0.009,
                245, 247, 251, (int) ((220 + 35 * reveal) * a), 2.0f);
        if (reveal > 0.02f) {
            drawSdfRoundedSurface(matrix, cam, p, 0.165 * control * reveal, barY,
                    0.032 * control, 0.032 * control, 0.032 * control, 0.0105,
                    0xFF, 0x3B, 0x45, (int) (232 * reveal * a));
        }
        if (!showTopPanel.get()) {
            drawBottomCornerHandle(matrix, cam, p, -hw, -hh, false,
                    control, p.edgeReveal[0] * a);
            drawBottomCornerHandle(matrix, cam, p, hw, -hh, true,
                    control, p.edgeReveal[1] * a);
            return;
        }

        double navHalfW = Math.min(hw * 0.62, 0.86 * control);
        double navHalfH = 0.053 * control;
        double navY = hh + NAV_GAP * control;
        double tabsY = navY + 0.092 * control;
        double tabsHalfH = 0.043 * control;
        double chromeTop = tabsY + tabsHalfH;
        double chromeBottom = navY - navHalfH;
        drawSdfRoundedSurface(matrix, cam, p, 0, (chromeTop + chromeBottom) * 0.5,
                navHalfW, (chromeTop - chromeBottom) * 0.5, 0.045 * control, 0.006,
                0x16, 0x19, 0x20, (int) (218 * a), 2.35f);

        double backU = -navHalfW + 0.075 * control;
        double fwdU = backU + 0.105 * control;
        double reloadU = fwdU + 0.105 * control;
        int hovered = hit == null ? -1 : region(p, hit[0], hit[1]);
        double buttonRadius = 0.038 * control;
        double[] buttonPositions = {backU, fwdU, reloadU};
        for (int i = 0; i < buttonPositions.length; i++) {
            double buttonU = buttonPositions[i];
            int buttonRegion = 6 + i;
            if (hovered == buttonRegion) {
                drawSdfRoundedSurface(matrix, cam, p, buttonU, navY,
                        buttonRadius, buttonRadius, buttonRadius, 0.009,
                        0xF2, 0xF4, 0xF8, (int) (38 * a));
            }
        }

        double pillLeft = reloadU + 0.060 * control;
        double pillRight = navHalfW - 0.028 * control;
        double pillU = (pillLeft + pillRight) * 0.5;
        double pillHalfW = (pillRight - pillLeft) * 0.5;
        double pillHalfH = 0.034 * control;
        boolean overPill = hovered == 5;
        drawSdfRoundedSurface(matrix, cam, p, pillU, navY,
                pillHalfW, pillHalfH, pillHalfH, 0.009,
                overPill ? 0x31 : 0x21, overPill ? 0x35 : 0x25, overPill ? 0x3D : 0x2C,
                (int) ((overPill ? 225 : 180) * a));

        double addU = navHalfW - 0.050 * control;
        for (int i = 0; i < p.tabs.size(); i++) {
            Tab tab = p.tabs.get(i);
            float tabReveal = ease(tab.reveal);
            double centerU = tab.visualCenter;
            double width = tab.visualWidth;
            boolean active = i == p.activeTab;
            boolean over = hovered == 20 + i || hovered == 30 + i;
            if ((active || over) && width > 0.004 * control && tabReveal > 0.01f) {
                drawSdfTopRoundedSurface(matrix, cam, p, centerU, tabsY,
                        width * 0.5 - 0.003 * control, 0.035 * control,
                        0.026 * control, 0.009,
                        active ? 0x2A : 0x21, active ? 0x2E : 0x25, active ? 0x37 : 0x2D,
                        (int) ((active ? 238 : 190) * a * tabReveal), 2.35f);
            }
        }
        for (ClosingTab tab : p.closingTabs) {
            float tabReveal = ease(tab.reveal);
            if (tab.width <= 0.004 * control || tabReveal <= 0.01f) continue;
            drawSdfTopRoundedSurface(matrix, cam, p, tab.center, tabsY,
                    Math.max(0.002 * control, tab.width * 0.5 - 0.003 * control),
                    0.035 * control, 0.026 * control, 0.009,
                    0x21, 0x25, 0x2D, (int) (176 * a * tabReveal), 2.35f);
        }
        if (p.tabs.size() < MAX_TABS && hovered == 40) {
            drawSdfRoundedSurface(matrix, cam, p, addU, tabsY,
                    0.030 * control, 0.030 * control, 0.030 * control, 0.009,
                    0xF2, 0xF4, 0xF8, (int) (34 * a), 2.0f);
        }

        drawBottomCornerHandle(matrix, cam, p, -hw, -hh, false,
                control, p.edgeReveal[0] * a);
        drawBottomCornerHandle(matrix, cam, p, hw, -hh, true,
                control, p.edgeReveal[1] * a);
    }

    private void drawPanelMtsdf(Matrix4f matrix, Vec3d cam, Panel p, double[] hit) {
        Vec3d c = p.center.subtract(cam);
        Vec3d n = p.right.crossProduct(p.up).normalize();
        float a = ease(p.anim);
        double scale = 0.88 + 0.12 * a;
        double hw = p.halfW * scale;
        double hh = p.halfH * scale;
        double control = controlScale(p) * scale;
        if (!showTopPanel.get()) {
            if (p.barReveal > 0.02f) {
                drawPanelWorldText(matrix, c, p, n,
                        SolarIcons.DELETE, 0.165 * control * p.barReveal,
                        -hh - BAR_GAP * control, 0.039 * control, 0.013,
                        argb((int) (245 * p.barReveal * a), 255, 255, 255),
                        FontWeight.SOLAR_ICONS, true);
            }
            if (panelTextureId(p) <= 0) {
                drawPanelWorldText(matrix, c, p, n,
                        SolarIcons.GLOBAL, 0, 0.035, 0.075, 0.013,
                        argb((int) (180 * a), 255, 255, 255), FontWeight.SOLAR_ICONS, true);
            }
            return;
        }
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

        double tabsY = navY + 0.092 * control;
        double addU = abHalfW - 0.050 * control;
        for (int i = 0; i < p.tabs.size(); i++) {
            Tab tab = p.tabs.get(i);
            float tabReveal = ease(tab.reveal);
            double centerU = tab.visualCenter;
            double tabWidth = tab.visualWidth;
            if (tabWidth <= 0.015 * control || tabReveal <= 0.01f) continue;
            boolean active = i == p.activeTab;
            boolean over = hovered == 20 + i || hovered == 30 + i;
            double titleU = centerU - tabWidth * 0.5 + 0.020 * control;
            float titleSize = (float) (0.027 * control);
            float maxTitleWidth = (float) Math.max(0.02 * control, tabWidth - 0.078 * control);
            String title = CustomFontRenderer.trimToWidth(
                    tabTitle(tab.url), maxTitleWidth, titleSize, FontWeight.MEDIUM);
            drawPanelWorldText(matrix, c, p, n, title, titleU, tabsY,
                    titleSize, 0.013,
                    argb((int) ((active ? 235 : over ? 205 : 148) * a * tabReveal), 232, 235, 242),
                    FontWeight.MEDIUM, false);
            drawPanelWorldText(matrix, c, p, n, SolarIcons.CLOSE,
                    centerU + tabWidth * 0.5 - 0.028 * control, tabsY,
                    0.026 * control, 0.014,
                    argb((int) ((hovered == 30 + i ? 255 : active ? 178 : 112) * a * tabReveal),
                            238, 240, 245), FontWeight.SOLAR_ICONS, true);
        }
        for (ClosingTab tab : p.closingTabs) {
            float tabReveal = ease(tab.reveal);
            if (tab.width <= 0.030 * control || tabReveal <= 0.01f) continue;
            double titleU = tab.center - tab.width * 0.5 + 0.020 * control;
            float titleSize = (float) (0.027 * control);
            float maxTitleWidth = (float) Math.max(0.02 * control, tab.width - 0.040 * control);
            String title = CustomFontRenderer.trimToWidth(
                    tab.title, maxTitleWidth, titleSize, FontWeight.MEDIUM);
            drawPanelWorldText(matrix, c, p, n, title, titleU, tabsY,
                    titleSize, 0.013, argb((int) (132 * a * tabReveal), 232, 235, 242),
                    FontWeight.MEDIUM, false);
        }
        if (p.tabs.size() < MAX_TABS) {
            drawPanelWorldText(matrix, c, p, n, SolarIcons.ADD, addU, tabsY,
                    0.031 * control, 0.014,
                    argb((int) ((hovered == 40 ? 255 : 172) * a), 232, 235, 242),
                    FontWeight.SOLAR_ICONS, true);
        }

        if (p.barReveal > 0.02f) {
            drawPanelWorldText(matrix, c, p, n,
                    SolarIcons.DELETE, 0.165 * control * p.barReveal,
                    -hh - BAR_GAP * control, 0.039 * control, 0.013,
                    argb((int) (245 * p.barReveal * a), 255, 255, 255),
                    FontWeight.SOLAR_ICONS, true);
        }

        if (p.remote && !p.ownerName.isBlank()) {
            drawPanelWorldText(matrix, c, p, n, p.ownerName,
                    -0.125 * control, -hh - BAR_GAP * control,
                    0.023 * control, 0.013, argb((int) (155 * a), 232, 235, 242),
                    FontWeight.MEDIUM, false);
        }

        int glTex = panelTextureId(p);
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
                    p.halfW, curveAngle(p), text, anchorU, centerV, size, depth, color, weight);
        } else {
            CustomFontRenderer.drawCurvedWorldText(matrix, center, p.right, p.up, normal,
                    p.halfW, curveAngle(p), text, anchorU, centerV, size, depth,
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

    private void drawSdfSurface(Matrix4f matrix, Vec3d cam, Panel p,
                                double hw, double hh, double depth,
                                int red, int green, int blue, int alpha) {
        drawSdfRoundedSurface(matrix, cam, p, 0.0, 0.0, hw, hh,
                smartCorner(hw, hh), depth, red, green, blue, alpha);
    }

    private void drawSdfRoundedSurface(Matrix4f matrix, Vec3d cam, Panel p,
                                       double centerU, double centerV,
                                       double hw, double hh, double radius, double depth,
                                       int red, int green, int blue, int alpha) {
        drawSdfRoundedSurface(matrix, cam, p, centerU, centerV, hw, hh, radius, depth,
                red, green, blue, alpha, 3.35f);
    }

    private void drawSdfRoundedSurface(Matrix4f matrix, Vec3d cam, Panel p,
                                       double centerU, double centerV,
                                       double hw, double hh, double radius, double depth,
                                       int red, int green, int blue, int alpha, float exponent) {
        if (alpha <= 0 || hw <= 0.0 || hh <= 0.0) return;
        WebPanelShaders.useColor((float) (hw * 2.0), (float) (hh * 2.0),
                (float) radius, exponent);
        Vec3d center = p.center.subtract(cam);
        Vec3d normal = p.right.crossProduct(p.up).normalize();
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int columns = PANEL_SEGMENTS;
        for (int i = 0; i < columns; i++) {
            double u0 = centerU - hw + 2.0 * hw * i / columns;
            double u1 = centerU - hw + 2.0 * hw * (i + 1) / columns;
            float t0 = (float) i / columns;
            float t1 = (float) (i + 1) / columns;
            sdfVert(buffer, matrix, center, p, normal, u0, centerV + hh, depth,
                    t0, 0f, red, green, blue, alpha);
            sdfVert(buffer, matrix, center, p, normal, u1, centerV + hh, depth,
                    t1, 0f, red, green, blue, alpha);
            sdfVert(buffer, matrix, center, p, normal, u1, centerV - hh, depth,
                    t1, 1f, red, green, blue, alpha);
            sdfVert(buffer, matrix, center, p, normal, u0, centerV - hh, depth,
                    t0, 1f, red, green, blue, alpha);
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private void drawSdfTopRoundedSurface(Matrix4f matrix, Vec3d cam, Panel p,
                                          double centerU, double centerV,
                                          double hw, double hh, double radius, double depth,
                                          int red, int green, int blue, int alpha, float exponent) {
        if (alpha <= 0 || hw <= 0.0 || hh <= 0.0) return;
        WebPanelShaders.useTopRoundedColor((float) (hw * 2.0), (float) (hh * 2.0),
                (float) radius, exponent);
        Vec3d center = p.center.subtract(cam);
        Vec3d normal = p.right.crossProduct(p.up).normalize();
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int columns = PANEL_SEGMENTS;
        for (int i = 0; i < columns; i++) {
            double u0 = centerU - hw + 2.0 * hw * i / columns;
            double u1 = centerU - hw + 2.0 * hw * (i + 1) / columns;
            float t0 = (float) i / columns;
            float t1 = (float) (i + 1) / columns;
            sdfVert(buffer, matrix, center, p, normal, u0, centerV + hh, depth,
                    t0, 0f, red, green, blue, alpha);
            sdfVert(buffer, matrix, center, p, normal, u1, centerV + hh, depth,
                    t1, 0f, red, green, blue, alpha);
            sdfVert(buffer, matrix, center, p, normal, u1, centerV - hh, depth,
                    t1, 1f, red, green, blue, alpha);
            sdfVert(buffer, matrix, center, p, normal, u0, centerV - hh, depth,
                    t0, 1f, red, green, blue, alpha);
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private static double smartCorner(double hw, double hh) {
        // Roughly 28-40 Chromium pixels at the default 1920x1080 panel. This
        // keeps the silhouette rounded without eating page buttons or text.
        return MathHelper.clamp(Math.min(hw, hh) * 0.10, 0.045, 0.085);
    }

    private void drawBottomCornerHandle(Matrix4f matrix, Vec3d cam, Panel p,
                                        double u, double v, boolean rightSide,
                                        double controlScale, float reveal) {
        if (reveal <= 0.01f) return;
        double growth = 0.82 + 0.18 * reveal;
        double radius = 0.030 * growth * controlScale;
        double leg = 0.052 * growth * controlScale;
        double gap = 0.014 * controlScale;
        double thickness = 0.0110 * controlScale;
        double padding = thickness;
        double pathWidth = leg + radius;
        double pathHeight = leg + radius;
        double outerU = u + (rightSide ? gap : -gap);
        double pathMinU = rightSide ? outerU - pathWidth : outerU;
        double pathMaxU = rightSide ? outerU : outerU + pathWidth;
        double pathMinV = v - gap;
        double pathMaxV = pathMinV + pathHeight;
        double minU = pathMinU - padding;
        double maxU = pathMaxU + padding;
        double minV = pathMinV - padding;
        double maxV = pathMaxV + padding;
        double width = maxU - minU;
        double height = maxV - minV;

        WebPanelShaders.useCorner((float) width, (float) height, (float) radius,
                (float) thickness, (float) padding, rightSide);
        Vec3d center = p.center.subtract(cam);
        Vec3d normal = p.right.crossProduct(p.up).normalize();
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int columns = 96;
        for (int i = 0; i < columns; i++) {
            double x0 = minU + (maxU - minU) * i / columns;
            double x1 = minU + (maxU - minU) * (i + 1) / columns;
            float t0 = (float) i / columns;
            float t1 = (float) (i + 1) / columns;
            sdfVert(buffer, matrix, center, p, normal, x0, maxV, 0.0105,
                    t0, 0f, 245, 247, 251, (int) (238 * reveal));
            sdfVert(buffer, matrix, center, p, normal, x1, maxV, 0.0105,
                    t1, 0f, 245, 247, 251, (int) (238 * reveal));
            sdfVert(buffer, matrix, center, p, normal, x1, minV, 0.0105,
                    t1, 1f, 245, 247, 251, (int) (238 * reveal));
            sdfVert(buffer, matrix, center, p, normal, x0, minV, 0.0105,
                    t0, 1f, 245, 247, 251, (int) (238 * reveal));
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
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

    private void screenTexVert(BufferBuilder buf, Matrix4f m, Matrix4f proj, Vec3d c, Panel p, Vec3d n,
                               double u, double v, double d, float shapeU, float shapeV, float alpha) {
        double mappedU = u;
        double mappedDepth = d;
        double angle = curveAngle(p);
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

        buf.vertex(m, (float) x, (float) y, (float) z)
                .texture(tu, tv).color(shapeU, shapeV, 1f, alpha);
    }

    private void colorVert(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                           double u, double v, double d, int r, int g, int b, int a) {
        double mappedU = u;
        double mappedDepth = d;
        double angle = curveAngle(p);
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
        double angle = curveAngle(p);
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

    private void sdfVert(BufferBuilder buf, Matrix4f m, Vec3d c, Panel p, Vec3d n,
                         double u, double v, double d, float texU, float texV,
                         int red, int green, int blue, int alpha) {
        double mappedU = u;
        double mappedDepth = d;
        double angle = curveAngle(p);
        if (angle >= 1e-5 && p.halfW >= 1e-5) {
            double radius = p.halfW / angle;
            double theta = u / radius;
            mappedU = radius * Math.sin(theta);
            mappedDepth += radius * (1.0 - Math.cos(theta));
        }
        double x = c.x + p.right.x * mappedU + p.up.x * v + n.x * mappedDepth;
        double y = c.y + p.right.y * mappedU + p.up.y * v + n.y * mappedDepth;
        double z = c.z + p.right.z * mappedU + p.up.z * v + n.z * mappedDepth;
        buf.vertex(m, (float) x, (float) y, (float) z)
                .texture(texU, texV)
                .color(red / 255f, green / 255f, blue / 255f, alpha / 255f);
    }

    private double curveAngle(Panel panel) {
        return panel.remote ? panel.remoteCurve : Math.max(0.0, curveSetting.get());
    }

    private double[] curvedLocal(Panel p, double u) {
        double angle = curveAngle(p);
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
        volatile long lastSharedFrameRevision;
        float sharedJpegProfileQuality = -1.0f;
        float sharedJpegQuality = -1.0f;
        boolean remote;
        String remoteKey = "";
        String ownerName = "";
        Identifier remoteTexture;
        long remoteFrameRevision;
        long lastRemoteSeenMs;
        double remoteCurve;
        boolean renderVisible;
        boolean geometryVisible;
        boolean notOccluded = true;
        long nextOcclusionCheckMs;
        final List<Tab> tabs = new ArrayList<>();
        final List<ClosingTab> closingTabs = new ArrayList<>();
        int activeTab;
    }

    private static final class Tab {
        String url;
        Object browser;
        long lastCameraBootstrapMs;
        long lastCameraFrameSequence;
        boolean browserVisible = true;
        float reveal;
        double visualCenter = Double.NaN;
        double visualWidth;

        Tab(String url) {
            this(url, false);
        }

        Tab(String url, boolean visible) {
            this.url = url == null ? "" : url;
            this.reveal = visible ? 1.0f : 0.0f;
        }
    }

    private static final class ClosingTab {
        final String title;
        double center;
        double width;
        float reveal;

        ClosingTab(String title, double center, double width, float reveal) {
            this.title = title;
            this.center = center;
            this.width = width;
            this.reveal = reveal;
        }
    }

    private record SharedPanelState(int id, Vec3d center, Vec3d right, Vec3d up,
                                    double halfW, double halfH, double curve, String url,
                                    List<String> tabs, int activeTab, long frameRevision) {
    }

    private record SharedFrame(int panelId, long revision, String base64) {
    }

    private record RemotePanelUpdate(String key, String uuid, String username, int id,
                                     Vec3d center, Vec3d right, Vec3d up,
                                     double halfW, double halfH, double curve, String url,
                                     List<String> tabs, int activeTab, long frameRevision) {
    }

    private record VideoProfile(int width, int height, float jpegQuality, long intervalMs) {
    }
}
