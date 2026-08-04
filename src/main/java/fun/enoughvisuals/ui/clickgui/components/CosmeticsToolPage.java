package fun.enoughvisuals.ui.clickgui.components;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.cosmetic.CosmeticBridge;
import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog;
import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog.Entry;
import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog.Kind;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class CosmeticsToolPage {
    private static final int MAX_AVATAR_LOADS_PER_FRAME = 1;
    private static final int MAX_SNAPSHOTS_PER_FRAME = 1;
    private static final long SNAPSHOT_WARMUP_MS = 350L;
    private static final long PREVIEW_TTL_MS = 400L;
    private static final Map<String, PreviewTexture> PREVIEW_TEXTURES = new HashMap<>();
    private final Map<String, Float> hover = new HashMap<>();
    private final Map<Entry, Bounds> cards = new LinkedHashMap<>();
    private final Map<Kind, Bounds> tabs = new LinkedHashMap<>();
    private final Map<Kind, Bounds> slots = new LinkedHashMap<>();
    private final Map<String, PreviewActor> previewActors = new LinkedHashMap<>();
    private Kind kind = Kind.MODEL;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private float gridX, gridY, gridW, gridH, scale;
    private long openedAt = System.currentTimeMillis();
    private int avatarLoadsThisFrame;
    private int snapshotsThisFrame;

    void activate() {
        CosmeticCatalog.initialize();
        hover.clear();
        openedAt = System.currentTimeMillis();
    }

    void deactivate() {
        CosmeticBridge.setPreviewAlpha(1f);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    void render(DrawContext context, float x, float y, float width, float height, float scale,
                int mouseX, int mouseY, float alpha, String searchQuery) {
        this.scale = scale;
        MenuScreen.syncTheme();
        cards.clear();
        tabs.clear();
        slots.clear();
        scroll = RenderUtil.animate(scroll, scrollTarget, 0.28f, RenderUtil.getFrameDelta());
        float intro = RenderUtil.easeOutQuart(MathHelper.clamp((System.currentTimeMillis() - openedAt) / 280f, 0f, 1f));
        float gap = 7f * scale;
        float inspectorW = 112f * scale;
        float catalogW = width - inspectorW - gap;
        float catalogX = x - (1f - intro) * 9f * scale;
        float inspectorX = x + catalogW + gap + (1f - intro) * 9f * scale;

        RenderUtil.drawRoundedRect(context, catalogX, y, catalogW, height, 8f * scale,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.92f * alpha));
        RenderUtil.drawRoundedRect(context, inspectorX, y, inspectorW, height, 8f * scale,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.92f * alpha));

        CustomFontRenderer.drawText(context, "\uF009", catalogX + 10f * scale, y + 9.2f * scale,
                10.5f * scale, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.CLICK_GUI);
        CustomFontRenderer.drawText(context, "Косметика", catalogX + 25f * scale, y + 9.8f * scale,
                8.2f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawRightText(context, CosmeticCatalog.all().size() + " предмета", catalogX + catalogW - 10f * scale,
                y + 10.5f * scale, 6.1f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);

        float tabY = y + 27f * scale;
        float tabGap = 4f * scale;
        float tabW = (catalogW - 20f * scale - tabGap * 2f) / 3f;
        int tabIndex = 0;
        for (Kind tab : Kind.values()) {
            float tabX = catalogX + 10f * scale + tabIndex++ * (tabW + tabGap);
            boolean hovered = hit(mouseX, mouseY, tabX, tabY, tabW, 20f * scale);
            float h = animate("tab:" + tab, hovered);
            int idle = tab == kind
                    ? RenderUtil.withAlpha(MenuScreen.ACCENT, 0.28f * alpha)
                    : RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.92f * alpha);
            int over = tab == kind
                    ? RenderUtil.withAlpha(MenuScreen.ACCENT, 0.38f * alpha)
                    : RenderUtil.withAlpha(MenuScreen.CARD_HOVER, 0.96f * alpha);
            RenderUtil.drawRoundedRect(context, tabX, tabY, tabW, 20f * scale, 6.5f * scale,
                    RenderUtil.lerpColor(idle, over, h));
            CustomFontRenderer.drawCenteredText(context, tab.title(), tabX + tabW * 0.5f, tabY + 6.3f * scale,
                    6.1f * scale, RenderUtil.withAlpha(tab == kind ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_GRAY, alpha), FontWeight.SEMIBOLD);
            tabs.put(tab, new Bounds(tabX, tabY, tabW, 20f * scale));
        }

        gridX = catalogX + 8f * scale;
        gridY = tabY + 27f * scale;
        gridW = catalogW - 16f * scale;
        gridH = height - (gridY - y) - 8f * scale;
        List<Entry> entries = filtered(searchQuery);
        int columns = 3;
        float cardGap = 5f * scale;
        float cardW = (gridW - cardGap * (columns - 1)) / columns;
        float cardH = 72f * scale;
        int rows = (entries.size() + columns - 1) / columns;
        maxScroll = Math.max(0f, rows * (cardH + cardGap) - cardGap - gridH);
        scrollTarget = MathHelper.clamp(scrollTarget, -maxScroll, 0f);
        scroll = MathHelper.clamp(scroll, -maxScroll, 0f);

        context.draw();
        RenderUtil.enableScissor(gridX, gridY, gridW, gridH);
        try {
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                float cardX = gridX + (i % columns) * (cardW + cardGap);
                float cardY = gridY + (i / columns) * (cardH + cardGap) + scroll;
                if (cardY + cardH < gridY || cardY > gridY + gridH) continue;
                boolean hovered = mouseY >= gridY && mouseY <= gridY + gridH
                        && hit(mouseX, mouseY, cardX, cardY, cardW, cardH);
                float h = animate("card:" + entry.id(), hovered);
                boolean equipped = CosmeticCatalog.isEquipped(entry);
                int idle = equipped
                        ? RenderUtil.withAlpha(MenuScreen.ACCENT, 0.25f * alpha)
                        : RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.92f * alpha);
                int over = equipped
                        ? RenderUtil.withAlpha(MenuScreen.ACCENT, 0.36f * alpha)
                        : RenderUtil.withAlpha(MenuScreen.CARD_HOVER, 0.98f * alpha);
                RenderUtil.drawRoundedRect(context, cardX, cardY, cardW, cardH, 7f * scale,
                        RenderUtil.lerpColor(idle, over, h));
                renderSimplePreview(context, entry, cardX, cardY, cardW, cardH, alpha);
                String name = CustomFontRenderer.trimToWidth(entry.name(), cardW - 8f * scale,
                        6.2f * scale, FontWeight.SEMIBOLD);
                CustomFontRenderer.drawCenteredText(context, name, cardX + cardW * 0.5f, cardY + 54f * scale,
                        6.2f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
                CustomFontRenderer.drawCenteredText(context, equipped ? "Надето" : "Надеть",
                        cardX + cardW * 0.5f, cardY + 63f * scale, 5.2f * scale,
                        RenderUtil.withAlpha(equipped ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
                cards.put(entry, new Bounds(cardX, cardY, cardW, cardH));
            }
            context.draw();
        } finally {
            RenderUtil.disableScissor();
        }
        if (entries.isEmpty()) {
            CustomFontRenderer.drawCenteredText(context, "Ничего не найдено", gridX + gridW * 0.5f,
                    gridY + 35f * scale, 5.8f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        }

        renderInspector(context, inspectorX, y, inspectorW, height, mouseX, mouseY, alpha);
    }

    private void renderInspector(DrawContext context, float x, float y, float width, float height,
                                 int mouseX, int mouseY, float alpha) {
        CustomFontRenderer.drawText(context, "Образ", x + 9f * scale, y + 10f * scale, 7.8f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, "Можно сочетать с оружием", x + 9f * scale, y + 21.5f * scale, 5.1f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        float previewX = x + 8f * scale;
        float previewY = y + 31f * scale;
        float previewW = width - 16f * scale;
        float previewH = 95f * scale;
        RenderUtil.drawRoundedRect(context, previewX, previewY, previewW, previewH, 6f * scale,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.92f * alpha));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            try {
                CosmeticBridge.setPreviewAlpha(alpha);
                InventoryScreen.drawEntity(context, Math.round(previewX), Math.round(previewY),
                        Math.round(previewX + previewW), Math.round(previewY + previewH),
                        Math.round(40f * scale), 0.0625f,
                        previewX + previewW * 0.5f, previewY + previewH * 0.42f, client.player);
                context.draw();
                client.getBufferBuilders().getEntityVertexConsumers().draw();
            } catch (Throwable ignored) {
            } finally {
                CosmeticBridge.setPreviewAlpha(1f);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
        }

        float slotY = previewY + previewH + 8f * scale;
        for (Kind slot : Kind.values()) {
            Entry selected = CosmeticCatalog.equipped(slot);
            boolean hovered = hit(mouseX, mouseY, x + 8f * scale, slotY, width - 16f * scale, 26f * scale);
            RenderUtil.drawRoundedRect(context, x + 8f * scale, slotY, width - 16f * scale, 26f * scale,
                    5.5f * scale, RenderUtil.withAlpha(hovered ? MenuScreen.CARD_HOVER : MenuScreen.INSET_BG, 0.90f * alpha));
            CustomFontRenderer.drawText(context, slotName(slot), x + 15f * scale, slotY + 5f * scale,
                    5.0f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
            String value = selected == null ? "Не выбрано" : selected.name();
            value = CustomFontRenderer.trimToWidth(value, width - 38f * scale, 5.2f * scale, FontWeight.SEMIBOLD);
            CustomFontRenderer.drawText(context, value, x + 15f * scale, slotY + 13.5f * scale,
                    5.7f * scale, RenderUtil.withAlpha(selected == null ? MenuScreen.TEXT_DIM : MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
            if (selected != null) CustomFontRenderer.drawCenteredText(context, "X", x + width - 16f * scale,
                    slotY + 9f * scale, 5f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
            slots.put(slot, new Bounds(x + 8f * scale, slotY, width - 16f * scale, 26f * scale));
            slotY += 31f * scale;
        }
    }

    boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button != 0) return false;
        for (Map.Entry<Kind, Bounds> tab : tabs.entrySet()) {
            if (tab.getValue().contains(mouseX, mouseY)) {
                kind = tab.getKey();
                scroll = scrollTarget = 0f;
                EnoughSoundPlayer.playScroll();
                return true;
            }
        }
        for (Map.Entry<Entry, Bounds> card : cards.entrySet()) {
            if (mouseY >= gridY && mouseY <= gridY + gridH && card.getValue().contains(mouseX, mouseY)) {
                CosmeticCatalog.toggle(card.getKey());
                EnoughSoundPlayer.playScroll();
                return true;
            }
        }
        for (Map.Entry<Kind, Bounds> slot : slots.entrySet()) {
            if (slot.getValue().contains(mouseX, mouseY) && CosmeticCatalog.equipped(slot.getKey()) != null) {
                CosmeticCatalog.unequip(slot.getKey());
                EnoughSoundPlayer.playScroll();
                return true;
            }
        }
        return true;
    }

    boolean mouseScrolled(float mouseX, float mouseY, double amount) {
        if (!hit(mouseX, mouseY, gridX, gridY, gridW, gridH) || maxScroll <= 0f) return false;
        scrollTarget = MathHelper.clamp(scrollTarget + (float) amount * 22f * scale, -maxScroll, 0f);
        EnoughSoundPlayer.playScroll();
        return true;
    }

    private List<Entry> filtered(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        for (Entry entry : CosmeticCatalog.of(kind)) {
            if (needle.isEmpty() || entry.name().toLowerCase(Locale.ROOT).contains(needle)) result.add(entry);
        }
        return result;
    }

    private float animate(String id, boolean target) {
        float value = RenderUtil.animate(hover.getOrDefault(id, 0f), target ? 1f : 0f,
                0.16f, RenderUtil.getFrameDelta());
        hover.put(id, value);
        return value;
    }

    private static String slotName(Kind kind) {
        return switch (kind) {
            case MODEL -> "МОДЕЛЬ";
            case HEAD -> "ГОЛОВНОЙ УБОР";
            case WEAPON -> "ОРУЖИЕ";
        };
    }

    private void renderSimplePreview(DrawContext context, Entry entry, float x, float y,
                                     float width, float height, float alpha) {
        int size = Math.max(1, Math.round(38f * scale));
        int px = Math.round(x + (width - size) * 0.5f);
        int py = Math.round(y + 6f * scale);
        Identifier texture = CosmeticCatalog.preview(entry);
        if (texture != null) {
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            context.drawTexture(RenderLayer::getGuiTextured, texture, px, py,
                    0f, 0f, size, size, size, size);
            context.draw();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            return;
        }

        RenderUtil.drawRoundedRect(context, px, py, size, size, 8f * scale,
                RenderUtil.withAlpha(0x6A6A72, 0.48f * alpha));
        String name = entry.name() == null ? "" : entry.name().trim();
        String initial = name.isEmpty()
                ? "?"
                : new String(Character.toChars(Character.toUpperCase(name.codePointAt(0))));
        CustomFontRenderer.drawCenteredText(context, initial,
                px + size * 0.5f, py + size * 0.28f,
                13f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, alpha), FontWeight.SEMIBOLD);
    }

    private void preparePreviewSnapshot(DrawContext context, Entry entry, float x, float y,
                                        float width, float height) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || PREVIEW_TEXTURES.containsKey(entry.id())) return;
        PreviewActor actor = previewActors.get(entry.id());
        if (actor == null) {
            if (!previewActors.isEmpty()) return;
            actor = createPreviewActor(entry, client);
            if (actor == null) return;
            previewActors.put(entry.id(), actor);
        }
        if (actor == null) return;
        actor.lastVisibleAt = System.currentTimeMillis();
        if (!actor.loaded) {
            if (avatarLoadsThisFrame >= MAX_AVATAR_LOADS_PER_FRAME) return;
            CosmeticBridge.equipPreview(actor.uuid, List.of(entry.directory()));
            actor.loaded = true;
            actor.loadedAt = System.currentTimeMillis();
            avatarLoadsThisFrame++;
        }
        if (!CosmeticBridge.previewReady(actor.uuid)
                || System.currentTimeMillis() - actor.loadedAt < SNAPSHOT_WARMUP_MS
                || snapshotsThisFrame >= MAX_SNAPSHOTS_PER_FRAME) return;
        actor.entity.age = client.player.age;
        float feetY = y + height - 14f * scale;
        PreviewTexture snapshot = capturePreview(context, entry, actor,
                x, y, width, Math.max(scale, feetY - y));
        if (snapshot != null) {
            PREVIEW_TEXTURES.put(entry.id(), snapshot);
            snapshotsThisFrame++;
            releasePreviewActor(entry.id());
        }
    }

    private void drawPreview(DrawContext context, Entry entry, float x, float y,
                             float width, float height, float alpha) {
        PreviewTexture cached = PREVIEW_TEXTURES.get(entry.id());
        if (cached != null) drawCachedPreview(context, cached, x, y, width, height, alpha);
    }

    private PreviewTexture capturePreview(DrawContext context, Entry entry, PreviewActor actor,
                                          float x, float y, float width, float previewHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        double pixelScale = client.getWindow().getScaleFactor();
        int readX = Math.max(0, (int) Math.floor(x * pixelScale));
        int readY = Math.max(0, client.getWindow().getFramebufferHeight()
                - (int) Math.ceil((y + previewHeight) * pixelScale));
        int readW = Math.max(1, Math.min(client.getWindow().getFramebufferWidth() - readX,
                (int) Math.ceil(width * pixelScale)));
        int readH = Math.max(1, Math.min(client.getWindow().getFramebufferHeight() - readY,
                (int) Math.ceil(previewHeight * pixelScale)));
        if (readW <= 0 || readH <= 0) return null;

        context.draw();
        ByteBuffer pixels = null;
        try {
            RenderUtil.enableScissor(x, y, width, previewHeight);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClearDepth(1d);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

            CosmeticBridge.setPreviewAlpha(1f);
            InventoryScreen.drawEntity(context,
                    Math.round(x + 2f * scale), Math.round(y + 2f * scale),
                    Math.round(x + width - 2f * scale), Math.round(y + previewHeight),
                    Math.max(10, Math.round(17f * scale)), 0.0625f,
                    x + width * 0.66f, y + previewHeight * 0.42f, actor.entity);
            context.draw();

            pixels = MemoryUtil.memAlloc(readW * readH * 4);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(readX, readY, readW, readH, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

            NativeImage image = new NativeImage(readW, readH, true);
            for (int py = 0; py < readH; py++) {
                int targetY = readH - 1 - py;
                for (int px = 0; px < readW; px++) {
                    int index = (py * readW + px) * 4;
                    int red = pixels.get(index) & 0xFF;
                    int green = pixels.get(index + 1) & 0xFF;
                    int blue = pixels.get(index + 2) & 0xFF;
                    int a = pixels.get(index + 3) & 0xFF;
                    image.setColorArgb(px, targetY, (a << 24) | (red << 16) | (green << 8) | blue);
                }
            }
            NativeImage cropped = cropSnapshot(image);
            if (cropped != image) image.close();
            Identifier id = Identifier.of("enough", "cosmetics/rendered_"
                    + entry.id().replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase(Locale.ROOT)
                    + "_" + Integer.toUnsignedString(entry.id().hashCode(), 36));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(cropped);
            texture.setFilter(true, true);
            client.getTextureManager().registerTexture(id, texture);
            return new PreviewTexture(id, cropped.getWidth(), cropped.getHeight());
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (pixels != null) MemoryUtil.memFree(pixels);
            CosmeticBridge.setPreviewAlpha(1f);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            RenderUtil.enableScissor(gridX, gridY, gridW, gridH);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    private static NativeImage cropSnapshot(NativeImage source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if ((source.getColorArgb(x, y) >>> 24) < 8) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) return source;
        int padding = Math.max(2, Math.round(Math.max(maxX - minX, maxY - minY) * 0.08f));
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(source.getWidth() - 1, maxX + padding);
        maxY = Math.min(source.getHeight() - 1, maxY + padding);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        if (width == source.getWidth() && height == source.getHeight()) return source;
        NativeImage result = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setColorArgb(x, y, source.getColorArgb(minX + x, minY + y));
            }
        }
        return result;
    }

    private void drawCachedPreview(DrawContext context, PreviewTexture texture, float x, float y,
                                   float width, float height, float alpha) {
        float maxW = width - 10f * scale;
        float maxH = height - 23f * scale;
        float fit = Math.min(maxW / texture.width, maxH / texture.height);
        int targetW = Math.max(1, Math.round(texture.width * fit));
        int targetH = Math.max(1, Math.round(texture.height * fit));
        int targetX = Math.round(x + (width - targetW) * 0.5f);
        int targetY = Math.round(y + 4f * scale + (maxH - targetH) * 0.5f);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        context.drawTexture(RenderLayer::getGuiTextured, texture.id,
                targetX, targetY, 0f, 0f,
                targetW, targetH, targetW, targetH);
        context.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private PreviewActor createPreviewActor(Entry entry, MinecraftClient client) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(("enough-cosmetic-preview:" + entry.id())
                    .getBytes(StandardCharsets.UTF_8));
            GameProfile profile = new GameProfile(uuid, "CosmeticPreview");
            profile.getProperties().putAll(client.player.getGameProfile().getProperties());
            OtherClientPlayerEntity entity = new OtherClientPlayerEntity(client.world, profile);
            entity.setId(-1_000_000 - (entry.id().hashCode() & 0x7FFFF));
            entity.setInvisible(false);
            entity.setNoGravity(true);
            CosmeticBridge.bindPreviewEntity(uuid, entity);
            return new PreviewActor(uuid, entity);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void prunePreviewActors() {
        long now = System.currentTimeMillis();
        var iterator = previewActors.entrySet().iterator();
        while (iterator.hasNext()) {
            PreviewActor actor = iterator.next().getValue();
            if (now - actor.lastVisibleAt <= PREVIEW_TTL_MS) continue;
            if (actor.loaded) CosmeticBridge.clearRemote(actor.uuid);
            CosmeticBridge.unbindPreviewEntity(actor.uuid, actor.entity);
            iterator.remove();
        }
    }

    private void releasePreviewActor(String id) {
        PreviewActor actor = previewActors.remove(id);
        if (actor == null) return;
        if (actor.loaded) CosmeticBridge.clearRemote(actor.uuid);
        CosmeticBridge.unbindPreviewEntity(actor.uuid, actor.entity);
    }

    private static boolean hit(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Bounds(float x, float y, float width, float height) {
        boolean contains(float mouseX, float mouseY) { return hit(mouseX, mouseY, x, y, width, height); }
    }

    private record PreviewTexture(Identifier id, int width, int height) {}

    private static final class PreviewActor {
        final UUID uuid;
        final OtherClientPlayerEntity entity;
        long lastVisibleAt = System.currentTimeMillis();
        long loadedAt;
        boolean loaded;

        private PreviewActor(UUID uuid, OtherClientPlayerEntity entity) {
            this.uuid = uuid;
            this.entity = entity;
        }
    }

}
