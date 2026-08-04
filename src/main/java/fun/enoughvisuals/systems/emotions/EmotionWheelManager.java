package fun.enoughvisuals.systems.emotions;

import com.mojang.blaze3d.systems.RenderSystem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fun.enoughvisuals.systems.modules.modules.visual.Emotions;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import fun.enoughvisuals.ui.screen.EmotionWheelScreen;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EmotionWheelManager {
    private static final EmotionType[] TYPES = EmotionType.values();
    private static final float FADE_IN_MS = 280f, FADE_OUT_MS = 320f;
    private static final EmotionWheelManager INSTANCE = new EmotionWheelManager();
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<UUID, ActiveEmotion> active = new ConcurrentHashMap<>();
    private final float[] slotHover = new float[TYPES.length];
    private boolean open;
    private float animation;
    private long lastFrame;
    private double cursorX, cursorY;
    private int stickySlot = -1;
    private EmotionType hovered;
    private EmotionType previewType;
    private long previewStarted;

    private EmotionWheelManager() {}
    public static EmotionWheelManager getInstance() { return INSTANCE; }
    public boolean capturesLook() { return open; }

    public boolean onKey(int key, int action) {
        Emotions module = Emotions.getInstance();
        if (module == null || !module.isEnabled() || key != module.wheelKey()) return false;
        if (action == GLFW.GLFW_PRESS && mc.player != null && mc.world != null && mc.currentScreen == null) {
            open();
            return true;
        }
        if (action == GLFW.GLFW_RELEASE && open) {
            EmotionType selected = hovered;
            close(false);
            if (mc.currentScreen instanceof EmotionWheelScreen) mc.setScreen(null);
            if (selected != null) play(selected);
            return true;
        }
        return open;
    }

    public void updatePointer(double mouseX, double mouseY, int width, int height) {
        if (!open) return;
        cursorX = mouseX - width * .5;
        cursorY = mouseY - height * .5;
    }

    public void tick() {
        if (mc.player == null || mc.world == null) { stopAll(); return; }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof EmotionWheelScreen) && open) close(true);
        active.entrySet().removeIf(entry -> {
            ActiveEmotion value = entry.getValue();
            if (!value.expired()) return false;
            if (value.local && entry.getKey().equals(mc.player.getUuid())) mc.options.setPerspective(value.previous);
            return true;
        });
    }

    public void stopAll() {
        close(true);
        if (mc.player != null) {
            ActiveEmotion emotion = active.get(mc.player.getUuid());
            if (emotion != null && emotion.local) mc.options.setPerspective(emotion.previous);
        }
        active.clear();
    }

    private void open() {
        open = true;
        cursorX = cursorY = 0;
        stickySlot = -1;
        hovered = null;
        previewType = EmotionType.HELLO;
        previewStarted = System.currentTimeMillis();
        lastFrame = previewStarted;
        mc.setScreen(new EmotionWheelScreen());
    }

    private void close(boolean force) {
        open = false;
        if (force) { stickySlot = -1; hovered = null; }
    }

    public void cancelWheel() { close(true); }

    private void play(EmotionType type) {
        if (mc.player == null) return;
        ActiveEmotion previous = active.get(mc.player.getUuid());
        Perspective perspective = previous != null && previous.local ? previous.previous : mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
        active.put(mc.player.getUuid(), new ActiveEmotion(type, System.currentTimeMillis(), perspective, true));
    }

    public void appendSync(JsonObject request) {
        if (mc.getCurrentServerEntry() == null || mc.player == null) return;
        request.addProperty("game_server", mc.getCurrentServerEntry().address);
        ActiveEmotion current = active.get(mc.player.getUuid());
        if (current != null && !current.expired()) {
            JsonObject emotion = new JsonObject();
            emotion.addProperty("type", current.type.wireName());
            request.add("emotion", emotion);
        }
    }

    public void applySync(JsonObject response) {
        if (mc.player == null || mc.world == null || mc.getCurrentServerEntry() == null) return;
        long now = System.currentTimeMillis();
        JsonArray values = response.has("active_emotions") && response.get("active_emotions").isJsonArray()
                ? response.getAsJsonArray("active_emotions") : new JsonArray();
        java.util.Set<UUID> received = new java.util.HashSet<>();
        for (JsonElement element : values) {
            try {
                JsonObject item = element.getAsJsonObject();
                UUID uuid = UUID.fromString(item.get("uuid").getAsString());
                EmotionType type = EmotionType.fromWireName(item.get("type").getAsString());
                if (type == null || uuid.equals(mc.player.getUuid())) continue;
                long elapsed = MathHelper.clamp(item.get("elapsed_ms").getAsLong(), 0L, type.durationMs());
                active.put(uuid, new ActiveEmotion(type, now - elapsed, Perspective.FIRST_PERSON, false));
                received.add(uuid);
            } catch (RuntimeException ignored) {}
        }
        active.entrySet().removeIf(entry -> !entry.getValue().local && !received.contains(entry.getKey()));
    }

    public void render(DrawContext context) {
        MenuScreen.syncTheme();
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? .016f : MathHelper.clamp((now - lastFrame) / 1000f, .001f, .05f);
        lastFrame = now;
        animation = approach(animation, open ? 1f : 0f, dt, 10f);
        if (animation <= .001f && !open) return;
        updateHovered();
        for (int i = 0; i < slotHover.length; i++) slotHover[i] = approach(slotHover[i], stickySlot == i ? 1f : 0f, dt, 14f);

        float a = easeOutCubic(MathHelper.clamp(animation, 0f, 1f));
        float width = context.getScaledWindowWidth(), height = context.getScaledWindowHeight();
        float cx = width / 2f, cy = height / 2f;
        context.fill(0, 0, (int) width, (int) height, alpha(0xFF000000, .38f * a));
        float ring = 105f * (.74f + .26f * a), base = 39f, hub = 47f;
        RenderUtil.drawRoundedRect(context, cx - hub, cy - hub, hub * 2, hub * 2, 16f,
                alpha(MenuScreen.CARD_BG, .96f * a));
        CustomFontRenderer.drawCenteredText(context, hovered == null ? "Эмоции" : hovered.title(), cx, cy - 5, 8f, alpha(0xFFFFFFFF, a), FontWeight.SEMIBOLD);

        for (int i = 0; i < TYPES.length; i++) {
            float hover = slotHover[i];
            double angle = Math.toRadians(-90 + i * 360.0 / TYPES.length);
            float sx = cx + (float) Math.cos(angle) * ring, sy = cy + (float) Math.sin(angle) * ring;
            float radius = base * (.94f + .06f * hover) * (.87f + .13f * a);
            RenderUtil.drawRoundedRect(context, sx-radius, sy-radius, radius*2, radius*2, 12f,
                    alpha(lerpColor(MenuScreen.CARD_BG, MenuScreen.ACCENT, hover * .34f), a));
            int icon = Math.max(1, Math.round(radius * (1.12f + .08f * hover)));
            RenderSystem.setShaderColor(1f, 1f, 1f, a * (.70f + .30f * hover));
            context.drawTexture(RenderLayer::getGuiTextured, TYPES[i].texture(), Math.round(sx-icon/2f), Math.round(sy-icon/2f), 0, 0, icon, icon, icon, icon);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        float titleY = cy - ring - base - 31 - 18 * (1-a);
        CustomFontRenderer.drawCenteredText(context, "Выбери нужную эмоцию", cx, titleY, 10f, alpha(0xFFFFFFFF, a), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawCenteredText(context, "Отпусти клавишу, чтобы применить", cx, titleY + 14, 6.5f, alpha(0xFFA8B4C4, a), FontWeight.REGULAR);
        float pointerX = cx+(float)cursorX, pointerY = cy+(float)cursorY;
        RenderUtil.drawRoundedRect(context, pointerX-2.5f, pointerY-2.5f, 5, 5, 2.5f,
                alpha(MenuScreen.ACCENT_LIGHT, a));
    }

    private void updateHovered() {
        int hit = -1;
        double bestDistance = Double.MAX_VALUE;
        double ring = 105.0;
        double half = 39.0;
        for (int i = 0; i < TYPES.length; i++) {
            double angle = Math.toRadians(-90 + i * 360.0 / TYPES.length);
            double x = Math.cos(angle) * ring;
            double y = Math.sin(angle) * ring;
            if (cursorX >= x - half && cursorX <= x + half && cursorY >= y - half && cursorY <= y + half) {
                double distance = Math.hypot(cursorX - x, cursorY - y);
                if (distance < bestDistance) { bestDistance = distance; hit = i; }
            }
        }
        stickySlot = hit;
        hovered = hit < 0 ? null : TYPES[hit];
        if (previewType != hovered) {
            previewType = hovered;
            previewStarted = System.currentTimeMillis();
        }
    }

    public void apply(PlayerEntityRenderState state, ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        if (mc.world == null) return;
        Entity entity = mc.world.getEntityById(state.id);
        if (!(entity instanceof PlayerEntity player)) return;
        EmotionType type = null; float blend = 0; long started = 0;
        ActiveEmotion current = active.get(player.getUuid());
        if (current != null) { type=current.type; blend=current.blend(); started=current.started; }
        else if ((open || animation > .01f) && player == mc.player && hovered != null) { type=hovered; blend=MathHelper.clamp(animation*.85f,0,1); started=previewStarted; }
        if (type == null || blend <= .001f) return;
        long elapsed = Math.max(0, System.currentTimeMillis()-started);
        boolean hands = player != mc.player || !mc.options.getPerspective().isFirstPerson();
        switch (type) {
            case DEB -> { if(hands){arm(rightArm,blend,-.70f,-2f);arm(leftArm,blend,-.70f,-2f);} head.pitch=MathHelper.lerp(blend,head.pitch,.7f);head.yaw=MathHelper.lerp(blend,head.yaw,.5f);head.roll=MathHelper.lerp(blend,head.roll,0); }
            case FLOSS -> { float action=tri(elapsed,250), right=tri(elapsed,500), left=tri(elapsed+250,500); if(hands){arm(rightArm,blend,.5f-right,1-2*action);arm(leftArm,blend,.5f-left,1.5f-2*action);} body.roll=MathHelper.lerp(blend,body.roll,.3f*action-.15f);rightLeg.pivotX=MathHelper.lerp(blend,rightLeg.pivotX,-.5f-3*action);leftLeg.pivotX=MathHelper.lerp(blend,leftLeg.pivotX,3-2.5f*action); }
            case HELLO -> { if(hands) arm(rightArm,blend,-.2f,2.75f-.25f*tri(elapsed,250)); }
            case HAPPY -> { if(hands){arm(rightArm,blend,0,2.75f);arm(leftArm,blend,0,-2.75f);} }
            case MASTURBATE -> { float action=tri(elapsed,100); if(!player.isSneaking()){if(hands){arm(rightArm,blend,-.3f-.7f*action,-.55f);arm(leftArm,blend,.3f,1);}body.pitch=MathHelper.lerp(blend,body.pitch,-.1f);rightLeg.pivotZ=MathHelper.lerp(blend,rightLeg.pivotZ,-1);leftLeg.pivotZ=MathHelper.lerp(blend,leftLeg.pivotZ,-1);} else if(hands) arm(rightArm,blend,.7f-.7f*action,-.55f); }
            case GET_GRIDDY -> { float action=tri(elapsed,350), leg=tri(elapsed,300), eye=griddyEye(elapsed); if(hands){float x=1-1.5f*action-3.5f*eye;arm(rightArm,blend,x,.5f-.75f*action);arm(leftArm,blend,x,-.5f+.75f*action);}body.pitch=MathHelper.lerp(blend,body.pitch,.2f-.05f*action);rightLeg.pivotZ=MathHelper.lerp(blend,rightLeg.pivotZ,2-.5f*action);leftLeg.pivotZ=MathHelper.lerp(blend,leftLeg.pivotZ,2-.5f*action);rightLeg.pitch=MathHelper.lerp(blend,rightLeg.pitch,.3f*leg-.3f);leftLeg.pitch=MathHelper.lerp(blend,leftLeg.pitch,-.3f*leg-.1f);head.yaw=MathHelper.lerp(blend,head.yaw,-.4f+.4f*eye);head.pitch=MathHelper.lerp(blend,head.pitch,-.3f*eye);head.roll=MathHelper.lerp(blend,head.roll,0); }
        }
        hat.pitch=head.pitch;hat.yaw=head.yaw;hat.roll=head.roll;
    }

    private static void arm(ModelPart part,float b,float pitch,float roll){part.pitch=MathHelper.lerp(b,part.pitch,pitch);part.roll=MathHelper.lerp(b,part.roll,roll);}
    private static float tri(long t,long half){long cycle=half*2,m=t%cycle;return m<half?m/(float)half:2f-m/(float)half;}
    private static float griddyEye(long t){long cycle=t%3100;return cycle<2100?0:tri(cycle-2100,500);}
    private static float approach(float c,float t,float dt,float speed){return c+(t-c)*(1-(float)Math.exp(-speed*dt));}
    private static float easeOutCubic(float x){float inv=1-x;return 1-inv*inv*inv;}
    private static int alpha(int color,float a){return (MathHelper.clamp((int)(((color>>>24)&255)*MathHelper.clamp(a,0,1)),0,255)<<24)|(color&0xFFFFFF);}
    private static int lerpColor(int a,int b,float t){t=MathHelper.clamp(t,0,1);int aa=a>>>24,ar=a>>16&255,ag=a>>8&255,ab=a&255,ba=b>>>24,br=b>>16&255,bg=b>>8&255,bb=b&255;return ((int)(aa+(ba-aa)*t)<<24)|((int)(ar+(br-ar)*t)<<16)|((int)(ag+(bg-ag)*t)<<8)|(int)(ab+(bb-ab)*t);}

    private record ActiveEmotion(EmotionType type,long started,Perspective previous,boolean local){
        long elapsed(){return System.currentTimeMillis()-started;}
        boolean expired(){return elapsed()>=type.durationMs();}
        float blend(){long e=elapsed(),left=Math.max(0,type.durationMs()-e);float in=MathHelper.clamp(e/FADE_IN_MS,0,1),out=MathHelper.clamp(left/FADE_OUT_MS,0,1),v=Math.min(in,out);return v*v*(3-2*v);}
    }
}
