package fun.enoughvisuals.ui.clickgui.components;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.cosmetic.CosmeticBridge;
import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog;
import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog.Entry;
import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog.Kind;
import fun.enoughvisuals.systems.cosmetic.pulse.PulseCosmeticBridge;
import fun.enoughvisuals.systems.cosmetic.pulse.PulseModelGeometry;
import fun.enoughvisuals.port.FramebufferUtils;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.GuiAlphaLayer;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryStack;

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
    private static final int MAX_AVATAR_LOADS_PER_FRAME = 3;
    /** Holds every Figura model in the current catalog while keeping future catalogs bounded. */
    private static final int MAX_CACHED_FIGURA_PREVIEWS = 96;
    /** Icon strip of a catalog card, in unscaled units below the card's top edge. */
    private static final float CARD_ICON_TOP = 4f;
    private static final float CARD_ICON_HEIGHT = 44f;
    /** Three-quarter turn applied to catalog previews so flat models still read as 3D. */
    private static final float CARD_PREVIEW_TURN = 28f;
    /** GUI depth of the catalog previews, kept in front of the card background. */
    private static final float PREVIEW_DEPTH = 120f;
    private final Map<String, Float> hover = new HashMap<>();
    private final Map<Entry, Bounds> cards = new LinkedHashMap<>();
    private final Map<Kind, Bounds> tabs = new LinkedHashMap<>();
    private final Map<Kind, Bounds> slotRemoveButtons = new LinkedHashMap<>();
    private static final ScopedLruCache<PreviewKey, PreviewActor> PREVIEW_ACTORS =
            new ScopedLruCache<>(MAX_CACHED_FIGURA_PREVIEWS, CosmeticsToolPage::releasePreviewActor);
    private Kind kind = Kind.MODEL;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private float gridX, gridY, gridW, gridH, scale;
    private Bounds inspectorPreview;
    private boolean rotatingPreview;
    private float inspectorYaw;
    private float inspectorPitch = -6f;
    private long openedAt = System.currentTimeMillis();
    private int avatarLoadsThisFrame;

    void activate() {
        CosmeticCatalog.initialize();
        MinecraftClient client = MinecraftClient.getInstance();
        PREVIEW_ACTORS.switchScope(client == null ? null : client.world);
        hover.clear();
        openedAt = System.currentTimeMillis();
    }

    void deactivate() {
        rotatingPreview = false;
        CosmeticBridge.setPreviewAlpha(1f);
        CosmeticBridge.setPreviewHideVanillaModel(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    void render(DrawContext context, float x, float y, float width, float height, float scale,
                int mouseX, int mouseY, float alpha, String searchQuery) {
        this.scale = scale;
        MenuScreen.syncTheme();
        cards.clear();
        tabs.clear();
        slotRemoveButtons.clear();
        avatarLoadsThisFrame = 0;
        scroll = RenderUtil.animate(scroll, scrollTarget, 0.28f, RenderUtil.getFrameDelta());
        float intro = RenderUtil.easeOutQuart(MathHelper.clamp((System.currentTimeMillis() - openedAt) / 280f, 0f, 1f));
        float gap = 7f * scale;
        float inspectorW = 112f * scale;
        float catalogW = width - inspectorW - gap;
        float catalogX = x - (1f - intro) * 9f * scale;
        float inspectorX = x + catalogW + gap + (1f - intro) * 9f * scale;

        RenderUtil.drawRoundedRect(context, catalogX, y, catalogW, height, 8f * scale,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.89f * alpha));
        RenderUtil.drawRoundedRect(context, inspectorX, y, inspectorW, height, 8f * scale,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.89f * alpha));

        CustomFontRenderer.drawText(context, SolarIcons.COSMETICS, catalogX + 10f * scale, y + 9.2f * scale,
                10.5f * scale, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.SOLAR_ICONS);
        CustomFontRenderer.drawText(context, ClientLanguage.text("ui.cosmetics.title", "Косметика"), catalogX + 25f * scale, y + 9.8f * scale,
                8.8f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawRightText(context, ClientLanguage.text("ui.cosmetics.items_count", "%d предметов").formatted(CosmeticCatalog.all().size()), catalogX + catalogW - 10f * scale,
                y + 10.3f * scale, 6.5f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.MEDIUM);

        float tabY = y + 27f * scale;
        float tabsX = catalogX + 10f * scale;
        float tabsW = catalogW - 20f * scale;
        float tabsH = 20f * scale;
        float tabW = tabsW / Kind.values().length;
        RenderUtil.drawRoundedRect(context, tabsX, tabY, tabsW, tabsH, 6.5f * scale,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.82f * alpha));
        int tabIndex = 0;
        for (Kind tab : Kind.values()) {
            float tabX = tabsX + tabIndex++ * tabW;
            boolean hovered = hit(mouseX, mouseY, tabX, tabY, tabW, tabsH);
            float h = animate("tab:" + tab, hovered);
            int idleText = RenderUtil.lerpColor(MenuScreen.TEXT_GRAY, MenuScreen.TEXT_WHITE, h * 0.24f);
            int tabColor = tab == kind ? MenuScreen.ACCENT_LIGHT : idleText;
            CustomFontRenderer.drawCenteredText(context, tab.title(), tabX + tabW * 0.5f, tabY + 6.3f * scale,
                    6.6f * scale, RenderUtil.withAlpha(tabColor, alpha), FontWeight.MEDIUM);
            tabs.put(tab, new Bounds(tabX, tabY, tabW, tabsH));
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
                        ? RenderUtil.withAlpha(MenuScreen.ACCENT, 0.14f * alpha)
                        : RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.82f * alpha);
                int over = equipped
                        ? RenderUtil.withAlpha(MenuScreen.ACCENT, 0.20f * alpha)
                        : RenderUtil.withAlpha(MenuScreen.CARD_HOVER, 0.89f * alpha);
                RenderUtil.drawRoundedRect(context, cardX, cardY, cardW, cardH, 7f * scale,
                        RenderUtil.lerpColor(idle, over, h));
                cards.put(entry, new Bounds(cardX, cardY, cardW, cardH));
            }
            context.draw();
            // All flat card backgrounds must be flushed before the shared entity buffer is
            // used. Restoring the complete GL context between individual models invalidates
            // that buffer and was the reason only the first visible card rendered.
            FramebufferUtils.GlStateSnapshot previewPassState = FramebufferUtils.captureGlState();
            GuiAlphaLayer.render(context, alpha, () -> {
                FramebufferUtils.GlStateSnapshot layerState = FramebufferUtils.captureGlState();
                try {
                    for (Map.Entry<Entry, Bounds> card : cards.entrySet()) {
                        Bounds bounds = card.getValue();
                        renderSimplePreview(context, card.getKey(), bounds.x(), bounds.y(), bounds.width(), 1f);
                    }
                } finally {
                    FramebufferUtils.restoreGlState(layerState);
                    resyncGlState();
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }
            });
            context.draw();
            // Captions come last with depth testing off: the 3D previews render closer to
            // the camera than flat GUI content, so otherwise they punch holes in the text.
            RenderSystem.disableDepthTest();
            for (Map.Entry<Entry, Bounds> card : cards.entrySet()) {
                Entry entry = card.getKey();
                Bounds bounds = card.getValue();
                boolean equipped = CosmeticCatalog.isEquipped(entry);
                String name = CustomFontRenderer.trimToWidth(entry.name(), bounds.width() - 8f * scale,
                        6.7f * scale, FontWeight.MEDIUM);
                CustomFontRenderer.drawCenteredText(context, name, bounds.x() + bounds.width() * 0.5f,
                        bounds.y() + 53.5f * scale, 6.7f * scale,
                        RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.MEDIUM);
                CustomFontRenderer.drawCenteredText(context, equipped
                                ? ClientLanguage.text("ui.cosmetics.equipped", "Надето")
                                : ClientLanguage.text("ui.cosmetics.equip", "Надеть"),
                        bounds.x() + bounds.width() * 0.5f, bounds.y() + 63f * scale, 5.7f * scale,
                        RenderUtil.withAlpha(equipped ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_DIM, alpha),
                        FontWeight.SEMIBOLD);
            }
            context.draw();
            if (previewPassState.flag2) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        } finally {
            RenderUtil.disableScissor();
        }
        if (entries.isEmpty()) {
            CustomFontRenderer.drawCenteredText(context, ClientLanguage.text("ui.nothing_found", "Ничего не найдено"), gridX + gridW * 0.5f,
                    gridY + 35f * scale, 5.8f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        }

        renderInspector(context, inspectorX, y, inspectorW, height, mouseX, mouseY, alpha);
    }

    private void renderInspector(DrawContext context, float x, float y, float width, float height,
                                 int mouseX, int mouseY, float alpha) {
        CustomFontRenderer.drawText(context, ClientLanguage.text("ui.cosmetics.outfit", "Образ"), x + 9f * scale, y + 10f * scale, 7.8f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, ClientLanguage.text("ui.cosmetics.preview_subtitle", "Модель + косметика"), x + 9f * scale, y + 21.5f * scale, 5.7f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.MEDIUM);
        float previewX = x + 8f * scale;
        float previewY = y + 31f * scale;
        float previewW = width - 16f * scale;
        float contentUnits = height / scale;
        // Header + gap + five 23px slots/four 4px gaps + bottom breathing room.
        // Keeping this reserve explicit prevents the last slot from touching the
        // rounded inspector edge at the normal 251-unit content height.
        float slotBlockUnits = Kind.values().length * 23f + (Kind.values().length - 1) * 4f;
        float previewReserveUnits = 31f + 7f + slotBlockUnits + 8f;
        float previewH = MathHelper.clamp(contentUnits - previewReserveUnits, 52f, 82f) * scale;
        RenderUtil.drawRoundedRect(context, previewX, previewY, previewW, previewH, 6f * scale,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.92f * alpha));
        inspectorPreview = new Bounds(previewX, previewY, previewW, previewH);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            float modelArea = previewH - 12f * scale;
            context.draw();
            FramebufferUtils.GlStateSnapshot state = FramebufferUtils.captureGlState();
            GuiAlphaLayer.render(context, alpha, () -> {
                FramebufferUtils.GlStateSnapshot layerState = FramebufferUtils.captureGlState();
                boolean scissorEnabled = false;
                try {
                    RenderUtil.enableScissor(previewX, previewY, previewW, previewH);
                    scissorEnabled = true;
                    clearPreviewDepth();
                    enableDepth();
                    CosmeticBridge.setPreviewAlpha(1f);
                    CosmeticBridge.drawPreviewEntity(context, client.player,
                            previewX + previewW * 0.5f, previewY + modelArea * 0.5f + 4f * scale,
                            entityPreviewScale(client.player.getHeight(), modelArea),
                            inspectorPitch, inspectorYaw, true);
                } catch (Throwable ignored) {
                } finally {
                    if (scissorEnabled) RenderUtil.disableScissor();
                    FramebufferUtils.restoreGlState(layerState);
                    resyncGlState();
                    CosmeticBridge.setPreviewAlpha(1f);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }
            });
            RenderSystem.disableDepthTest();
            CustomFontRenderer.drawCenteredText(context, ClientLanguage.text("ui.cosmetics.rotate_hint", "ЛКМ — вращать"),
                    previewX + previewW * 0.5f, previewY + previewH - 9f * scale,
                    5.2f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.82f * alpha), FontWeight.MEDIUM);
            context.draw();
            if (state.flag2) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }

        float slotY = previewY + previewH + 7f * scale;
        float slotH = 23f * scale;
        float slotGap = 4f * scale;
        for (Kind slot : Kind.values()) {
            Entry selected = CosmeticCatalog.equipped(slot);
            boolean hovered = hit(mouseX, mouseY, x + 8f * scale, slotY, width - 16f * scale, slotH);
            RenderUtil.drawRoundedRect(context, x + 8f * scale, slotY, width - 16f * scale, slotH,
                    5.5f * scale, RenderUtil.withAlpha(hovered ? MenuScreen.CARD_HOVER : MenuScreen.INSET_BG, 0.90f * alpha));
            CustomFontRenderer.drawText(context, slotName(slot), x + 15f * scale, slotY + 4f * scale,
                    5.2f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.MEDIUM);
            String value = selected == null ? ClientLanguage.text("ui.none_selected", "Не выбрано") : selected.name();
            value = CustomFontRenderer.trimToWidth(value, width - 38f * scale, 5.8f * scale, FontWeight.MEDIUM);
            CustomFontRenderer.drawText(context, value, x + 15f * scale, slotY + 11.5f * scale,
                    5.8f * scale, RenderUtil.withAlpha(selected == null ? MenuScreen.TEXT_DIM : MenuScreen.TEXT_WHITE, alpha), FontWeight.MEDIUM);
            if (selected != null) {
                float removeX = x + width - 24f * scale;
                float removeW = 16f * scale;
                boolean removeHovered = hit(mouseX, mouseY, removeX, slotY + 3.5f * scale,
                        removeW, slotH - 7f * scale);
                if (removeHovered) {
                    RenderUtil.drawRoundedRect(context, removeX, slotY + 3.5f * scale,
                            removeW, slotH - 7f * scale, 4f * scale,
                            RenderUtil.withAlpha(MenuScreen.ACCENT, 0.13f * alpha));
                }
                CustomFontRenderer.drawCenteredText(context, "X", x + width - 16f * scale,
                        slotY + 7.5f * scale, 5f * scale,
                        RenderUtil.withAlpha(removeHovered ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM, alpha),
                        FontWeight.SEMIBOLD);
                slotRemoveButtons.put(slot,
                        new Bounds(removeX, slotY + 3.5f * scale, removeW, slotH - 7f * scale));
            }
            slotY += slotH + slotGap;
        }
    }

    boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button != 0) return false;
        if (inspectorPreview != null && inspectorPreview.contains(mouseX, mouseY)) {
            rotatingPreview = true;
            return true;
        }
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
        for (Map.Entry<Kind, Bounds> slot : slotRemoveButtons.entrySet()) {
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

    boolean mouseDragged(float mouseX, float mouseY, int button, float deltaX, float deltaY) {
        if (!rotatingPreview || button != 0) return false;
        inspectorYaw = (inspectorYaw + deltaX * 1.35f) % 360f;
        inspectorPitch = MathHelper.clamp(inspectorPitch - deltaY * 0.65f, -22f, 22f);
        return true;
    }

    boolean mouseReleased(int button) {
        if (button != 0 || !rotatingPreview) return false;
        rotatingPreview = false;
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
            case MODEL -> ClientLanguage.text("ui.cosmetics.slot.model", "МОДЕЛЬ");
            case WINGS -> ClientLanguage.text("ui.cosmetics.slot.wings", "КРЫЛЬЯ");
            case BODYWEAR -> ClientLanguage.text("ui.cosmetics.slot.bodywear", "НА ТЕЛО");
            case PET -> ClientLanguage.text("ui.cosmetics.slot.pet", "ПИТОМЕЦ");
            case HAT -> ClientLanguage.text("ui.cosmetics.slot.hat", "ГОЛОВНОЙ УБОР");
        };
    }

    private void renderSimplePreview(DrawContext context, Entry entry, float x, float y,
                                     float width, float alpha) {
        if (entry.isPulse()) {
            if (renderPulseGeometry(context, entry, x, y, width)) return;
        } else if (renderFiguraPreview(context, entry, x, y, width, alpha)) {
            return;
        }
        float boxY = y + CARD_ICON_TOP * scale;
        float boxH = CARD_ICON_HEIGHT * scale;
        float clipX = Math.max(gridX, x);
        float clipY = Math.max(gridY, boxY);
        float clipRight = Math.min(gridX + gridW, x + width);
        float clipBottom = Math.min(gridY + gridH, boxY + boxH);
        if (clipRight <= clipX || clipBottom <= clipY) return;
        context.draw();
        RenderUtil.enableScissor(clipX, clipY, clipRight - clipX, clipBottom - clipY);
        try {
            int size = Math.max(1, Math.round(38f * scale));
            int px = Math.round(x + (width - size) * 0.5f);
            int py = Math.round(y + 6f * scale);
            RenderUtil.drawRoundedRect(context, px, py, size, size, 8f * scale,
                    RenderUtil.withAlpha(0x6A6A72, 0.48f * alpha));
            String name = entry.name() == null ? "" : entry.name().trim();
            String initial = name.isEmpty()
                    ? "?"
                    : new String(Character.toChars(Character.toUpperCase(name.codePointAt(0))));
            CustomFontRenderer.drawCenteredText(context, initial,
                    px + size * 0.5f, py + size * 0.28f,
                    13f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, alpha), FontWeight.SEMIBOLD);
            context.draw();
        } finally {
            RenderUtil.disableScissor();
        }
    }

    private boolean renderFiguraPreview(DrawContext context, Entry entry, float x, float y,
                                        float width, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || entry.directory() == null) return false;
        PreviewKey key = new PreviewKey(entry.id(), entry.directory().toAbsolutePath().normalize().toString());
        PreviewActor actor = PREVIEW_ACTORS.getOrCreate(client.world, key,
                () -> createPreviewActor(entry, client));
        if (actor == null) return false;
        if (!actor.loaded && avatarLoadsThisFrame < MAX_AVATAR_LOADS_PER_FRAME) {
            CosmeticBridge.equipPreview(actor.uuid, List.of(entry.directory()));
            actor.loaded = true;
            avatarLoadsThisFrame++;
        }
        if (!actor.loaded || !CosmeticBridge.previewReady(actor.uuid)) return false;

        actor.entity.age = client.player.age;
        float boxY = y + CARD_ICON_TOP * scale;
        float boxH = CARD_ICON_HEIGHT * scale;
        float clipX = Math.max(gridX, x);
        float clipY = Math.max(gridY, boxY);
        float clipRight = Math.min(gridX + gridW, x + width);
        float clipBottom = Math.min(gridY + gridH, boxY + boxH);
        if (clipRight <= clipX || clipBottom <= clipY) return false;
        context.draw();
        boolean scissorEnabled = false;
        boolean drawn = false;
        try {
            RenderUtil.enableScissor(clipX, clipY, clipRight - clipX, clipBottom - clipY);
            scissorEnabled = true;
            clearPreviewDepth();
            enableDepth();
            CosmeticBridge.setPreviewAlpha(alpha);
            CosmeticBridge.setPreviewHideVanillaModel(true);
            CosmeticBridge.drawPreviewEntity(context, actor.entity,
                    x + width * 0.5f, boxY + boxH * 0.5f,
                    entityPreviewScale(actor.entity.getHeight(), boxH), 0f, CARD_PREVIEW_TURN, true);
            drawn = true;
        } catch (Throwable ignored) {
        } finally {
            if (scissorEnabled) RenderUtil.disableScissor();
            CosmeticBridge.setPreviewAlpha(1f);
            CosmeticBridge.setPreviewHideVanillaModel(false);
            restorePreviewRenderState();
        }
        return drawn;
    }

    private boolean renderPulseGeometry(DrawContext context, Entry entry, float x, float y, float width) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        boolean pet = entry.kind() == Kind.PET;
        PulseModelGeometry.Bounds bounds = PulseModelGeometry.bounds(entry.pulseId(), pet);
        float boxX = x + 4f * scale;
        float boxY = y + CARD_ICON_TOP * scale;
        float boxW = width - 8f * scale;
        float boxH = CARD_ICON_HEIGHT * scale;
        float clipX = Math.max(gridX, x);
        float clipY = Math.max(gridY, boxY);
        float clipRight = Math.min(gridX + gridW, x + width);
        float clipBottom = Math.min(gridY + gridH, boxY + boxH);
        if (clipRight <= clipX || clipBottom <= clipY) return true;

        boolean drawn = false;
        float turn = previewTurn(bounds, pet);
        float modelScale = fitScale(bounds, boxW, boxH, turn);
        context.draw();
        RenderUtil.enableScissor(clipX, clipY, clipRight - clipX, clipBottom - clipY);
        clearPreviewDepth();
        DiffuseLighting.disableGuiDepthLighting();
        enableDepth();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.getMatrices().push();
        try {
            context.getMatrices().translate(boxX + boxW * 0.5f, boxY + boxH * 0.5f, PREVIEW_DEPTH);
            context.getMatrices().scale(modelScale, modelScale, -modelScale);
            // Enter the upright, player-relative frame the cosmetic is measured in.
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
            context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(turn));
            if (bounds != null && bounds.usable()) {
                context.getMatrices().translate(-bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
            }
            if (pet) {
                // Pets expect world space, which is the frame we are already in.
                drawn = PulseCosmeticBridge.renderPet(entry.pulseId(), context.getMatrices(), immediate,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, previewSeconds());
            }
            if (!drawn) {
                // Worn cosmetics expect entity model space, so hand them the flipped frame.
                // Pets whose runtime refuses the pet path still draw through the generic one.
                context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
                drawn = PulseCosmeticBridge.render(entry.pulseId(), context.getMatrices(), immediate,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, previewSeconds());
            }
        } catch (Throwable ignored) {
        } finally {
            context.getMatrices().pop();
            immediate.draw();
            DiffuseLighting.enableGuiDepthLighting();
            RenderUtil.disableScissor();
            restorePreviewRenderState();
        }
        return drawn;
    }

    /**
     * Entity geometry needs a live depth buffer, otherwise cosmetics show through the body.
     * Goes through the raw {@code GlStateManager._*} calls on purpose: the RenderSystem
     * cache may disagree with the real GL state after a snapshot restore, in which case
     * the cached setters silently do nothing.
     */
    private static void enableDepth() {
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(true);
    }

    /**
     * The world depth buffer is still populated behind ClickGUI. Each preview owns an
     * independent scissored rectangle, so clear just that rectangle before drawing it;
     * otherwise whichever card happens to pass the old world depth test is the only one
     * visible on screen.
     */
    private static void clearPreviewDepth() {
        GlStateManager._depthMask(true);
        GlStateManager._clearDepth(1.0D);
        GlStateManager._clear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    /** Return to the ordinary GUI state without replacing GL programs, VAOs or buffers. */
    private static void restorePreviewRenderState() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * {@link FramebufferUtils#restoreGlState} pokes raw GL behind RenderSystem's back, so
     * the cached depth/blend state goes stale and every later cached setter no-ops. This
     * mirrors the real GL values back into the cache.
     */
    private static void resyncGlState() {
        GlStateManager._depthMask(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        GlStateManager._depthFunc(GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
        if (GL11.glIsEnabled(GL11.GL_DEPTH_TEST)) GlStateManager._enableDepthTest();
        else GlStateManager._disableDepthTest();
        if (GL11.glIsEnabled(GL11.GL_BLEND)) GlStateManager._enableBlend();
        else GlStateManager._disableBlend();
        GlStateManager._blendFuncSeparate(
                GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA));
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer mask = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            GlStateManager._colorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
        }
    }

    /** Pets face along the player's view, worn cosmetics face away from it. */
    private static float previewTurn(PulseModelGeometry.Bounds bounds, boolean pet) {
        if (pet) return 180f + CARD_PREVIEW_TURN;
        boolean wornOnBack = bounds != null && bounds.usable() && bounds.centerZ() > 0.05f;
        return (wornOnBack ? 180f : 0f) + CARD_PREVIEW_TURN;
    }

    private float fitScale(PulseModelGeometry.Bounds bounds, float boxWidth, float boxHeight, float turn) {
        if (bounds == null || !bounds.usable()) return Math.min(boxWidth, boxHeight) * 0.58f;
        double radians = Math.toRadians(turn);
        float spread = (float) (Math.abs(bounds.width() * Math.cos(radians))
                + Math.abs(bounds.depth() * Math.sin(radians)));
        float byWidth = boxWidth / Math.max(spread, 0.02f);
        float byHeight = boxHeight / Math.max(bounds.height(), 0.02f);
        return Math.min(byWidth, byHeight) * 0.82f;
    }

    private static float entityPreviewScale(float entityHeight, float boxHeight) {
        return boxHeight * 0.78f / Math.max(entityHeight, 0.5f);
    }

    private static float previewSeconds() {
        return Math.floorMod(System.nanoTime(), 1_000_000_000_000L) / 1.0e9f;
    }

    private static PreviewActor createPreviewActor(Entry entry, MinecraftClient client) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(("enough-cosmetic-preview:" + entry.id() + ":"
                    + entry.directory().toAbsolutePath().normalize())
                    .getBytes(StandardCharsets.UTF_8));
            GameProfile profile = new GameProfile(uuid, "CosmeticPreview");
            profile.getProperties().putAll(client.player.getGameProfile().getProperties());
            OtherClientPlayerEntity entity = new OtherClientPlayerEntity(client.world, profile);
            entity.setId(-1_000_000 - (uuid.hashCode() & 0x7FFFF));
            entity.setInvisible(false);
            entity.setNoGravity(true);
            CosmeticBridge.bindPreviewEntity(uuid, entity);
            return new PreviewActor(uuid, entity);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static void invalidatePreviewCache() {
        PREVIEW_ACTORS.clear();
    }

    private static void releasePreviewActor(PreviewActor actor) {
        try {
            if (actor.loaded) CosmeticBridge.clearRemote(actor.uuid);
        } finally {
            CosmeticBridge.unbindPreviewEntity(actor.uuid, actor.entity);
        }
    }

    private static boolean hit(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Bounds(float x, float y, float width, float height) {
        boolean contains(float mouseX, float mouseY) { return hit(mouseX, mouseY, x, y, width, height); }
    }

    private record PreviewKey(String id, String directory) {
    }

    private static final class PreviewActor {
        final UUID uuid;
        final OtherClientPlayerEntity entity;
        boolean loaded;

        private PreviewActor(UUID uuid, OtherClientPlayerEntity entity) {
            this.uuid = uuid;
            this.entity = entity;
        }
    }
}
