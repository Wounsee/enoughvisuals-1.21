package fun.enoughvisuals.ui.clickgui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.settings.*;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuSettings {

    private final Module module;

    private static final Identifier DROP_ICON = Identifier.of("enough", "icons/drop.png");
    private static final int MARQUEE_MIN_CHARS = 22;

    private static final float PADDING        = 6;
    private static final float ENABLED_ROW_H  = 19;
    private static final float SEP_H          = 5;
    private static final float ROW_H          = 17;                                       
    private static final float SLIDER_ROW_H   = 24;
    private static final float RANGE_ROW_H    = 24;
    private static final float MODE_HEADER_H  = 18;
    private static final float MODE_OPT_H     = 16;
    private static final float ITEM_SEARCH_H  = 13;
    private static final float ITEM_OPTION_GAP = 7f;
    private static final float COLOR_ROW_H          = 17;
    private static final float COLOR_PICKER_FULL = 85;
    private static final float STRING_ROW_H   = 26;
    private static final float BIND_ROW_H          = 17;
    private static final float CLICK_ROW_H         = 21;
    private static final float SELECT_OPT_H   = 16;
    private static final float TOGGLE_W       = 13f;
    private static final float TOGGLE_H       = 8f;
    private static final float TOGGLE_RIGHT_PAD = 9f;

    private float scrollOffset = 0, scrollTarget = 0, maxScroll = 0;

    private Setting<?>   draggingSetting  = null;
    private boolean      draggingLow      = true;
    private int          draggingColorPart = -1;
    private float        pickerX, pickerY, pickerW, pickerH;
    private float        hueBarX, hueBarY, hueBarW, hueBarH;

    private boolean      bindingMode   = false;
    private BindSetting  bindingTarget = null;

    private ColorSetting expandedColor = null;
    private float[]      editHSB       = new float[3];

    private StringSetting editingString = null;
    private int           cursorPos     = 0;

    private ModeSetting expandedMode = null;
    private final Map<ModeSetting, Float> modeExpandAnims = new HashMap<>();
    private final Map<ClickSetting, Float> clickHoverAnims = new HashMap<>();
    private final Map<ClickSetting, Float> clickPressAnims = new HashMap<>();
    private ItemSelectSetting expandedItemSelect = null;
    private final Map<ItemSelectSetting, Float> itemExpandAnims = new HashMap<>();
    private ItemSelectSetting editingItemSearch = null;
    private MultiItemSelectSetting expandedMultiItemSelect = null;
    private final Map<MultiItemSelectSetting, Float> multiItemExpandAnims = new HashMap<>();
    private MultiItemSelectSetting editingMultiItemSearch = null;
    private int itemSearchCursor = 0;
    private final Map<ItemSelectSetting, Float> itemListScrollOffsets = new HashMap<>();
    private final Map<ItemSelectSetting, Float> itemListScrollTargets = new HashMap<>();
    private final Map<MultiItemSelectSetting, Float> multiItemListScrollOffsets = new HashMap<>();
    private final Map<MultiItemSelectSetting, Float> multiItemListScrollTargets = new HashMap<>();
    private StringSetting typedStringSetting = null;
    private int typedStringCharIndex = -1;
    private long typedStringCharStartMs = 0L;
    private ItemSelectSetting typedItemSearchSetting = null;
    private int typedItemSearchCharIndex = -1;
    private long typedItemSearchCharStartMs = 0L;
    private MultiItemSelectSetting typedMultiItemSearchSetting = null;
    private int typedMultiItemSearchCharIndex = -1;
    private long typedMultiItemSearchCharStartMs = 0L;

    private float enableAnim = 0;
    private final float[] settingAnims = new float[64];
    private final Map<ColorSetting, Float> colorExpandAnims = new HashMap<>();
    private float renderOriginX = 0f;
    private float renderOriginY = 0f;
    private float renderScale = 1f;
    private int renderMouseX = 0;
    private int renderMouseY = 0;

    public MenuSettings(Module module) {
        this.module = module;
    }

    public void setRenderTransform(float originX, float originY, float scale) {
        this.renderOriginX = originX;
        this.renderOriginY = originY;
        this.renderScale = Math.max(0.05f, scale);
    }

                                                                                                                                                                            
              
                                                                                                                                                                            

    public void render(DrawContext ctx, float px, float cy, float pw, float ch,
                       int mx, int my, float delta, float alpha,
                       MenuScreen.PanelData panel) {

        renderMouseX = mx;
        renderMouseY = my;

        float speed = RenderUtil.getFrameDelta() / 22f;
        scrollOffset = smooth(scrollOffset, scrollTarget, 14f, speed);
        enableAnim = smooth(enableAnim, module.isEnabled() ? 1f : 0f, 7f, speed);

        float sy = cy + scrollOffset + PADDING;

        sy = renderEnabledToggle(ctx, px, sy, pw, alpha, speed);

        RenderUtil.drawRect(ctx, px + 10, sy, pw - 20, 0.5f,
                applyAlpha(MenuScreen.SEPARATOR, alpha * 0.15f));
        sy += SEP_H;

        List<Setting<?>> settings = module.getVisibleSettings();
        int idx = 0;
        for (Setting<?> s : settings) {
            if (idx < settingAnims.length) {
                if (s instanceof BooleanSetting bs)
                    settingAnims[idx] = smooth(settingAnims[idx], bs.get() ? 1f : 0f, 5.5f, speed);
                else if (s instanceof SliderSetting ss)
                    settingAnims[idx] = smooth(settingAnims[idx], ss.getProgress(), 14f, speed);
            }

            if (s instanceof ColorSetting cs) {
                float target = (cs == expandedColor) ? 1f : 0f;
                colorExpandAnims.put(cs, smooth(colorExpandAnims.getOrDefault(cs, 0f), target, 14f, speed));
            }

            if (s instanceof ModeSetting ms) {
                float target = (ms == expandedMode) ? 1f : 0f;
                modeExpandAnims.put(ms, smooth(modeExpandAnims.getOrDefault(ms, 0f), target, 7.5f, speed));
            }
            if (s instanceof ItemSelectSetting itemSelect) {
                float target = (itemSelect == expandedItemSelect) ? 1f : 0f;
                itemExpandAnims.put(itemSelect, smooth(itemExpandAnims.getOrDefault(itemSelect, 0f), target, 14f, speed));

                int visible = getItemSelectVisibleCount(itemSelect);
                int total = itemSelect.getFilteredOptions().size();
                float maxListScroll = Math.max(0f, (total - visible) * MODE_OPT_H);
                float targetScroll = clamp(itemListScrollTargets.getOrDefault(itemSelect, 0f), 0f, maxListScroll);
                float currentScroll = itemListScrollOffsets.getOrDefault(itemSelect, 0f);

                if (maxListScroll <= 0.01f) {
                    targetScroll = 0f;
                    currentScroll = 0f;
                } else {
                    currentScroll = clamp(currentScroll, 0f, maxListScroll);
                }

                itemListScrollTargets.put(itemSelect, targetScroll);
                itemListScrollOffsets.put(itemSelect, smooth(currentScroll, targetScroll, 16f, speed));
            }
            if (s instanceof MultiItemSelectSetting multiItemSelect) {
                float target = (multiItemSelect == expandedMultiItemSelect) ? 1f : 0f;
                multiItemExpandAnims.put(multiItemSelect, smooth(multiItemExpandAnims.getOrDefault(multiItemSelect, 0f), target, 14f, speed));

                int visible = getMultiItemSelectVisibleCount(multiItemSelect);
                int total = multiItemSelect.getFilteredOptions().size();
                float maxListScroll = Math.max(0f, (total - visible) * MODE_OPT_H);
                float targetScroll = clamp(multiItemListScrollTargets.getOrDefault(multiItemSelect, 0f), 0f, maxListScroll);
                float currentScroll = multiItemListScrollOffsets.getOrDefault(multiItemSelect, 0f);

                if (maxListScroll <= 0.01f) {
                    targetScroll = 0f;
                    currentScroll = 0f;
                } else {
                    currentScroll = clamp(currentScroll, 0f, maxListScroll);
                }

                multiItemListScrollTargets.put(multiItemSelect, targetScroll);
                multiItemListScrollOffsets.put(multiItemSelect, smooth(currentScroll, targetScroll, 16f, speed));
            }

            float h = getSettingHeight(s);
            float anim = idx < settingAnims.length ? settingAnims[idx] : 0;

            renderSetting(ctx, s, px, sy, pw, h, alpha, mx, my, anim, idx);

            sy += h;
            idx++;
        }

        maxScroll = Math.max(0, (sy - cy - scrollOffset + PADDING) - ch);
    }

    private void renderSetting(DrawContext ctx, Setting<?> s, float px, float sy, float pw,
                               float h, float alpha, int mx, int my,
                               float anim, int idx) {
        if (s instanceof BooleanSetting bs)      renderBoolean(ctx, bs, px, sy, pw, alpha, anim);
        else if (s instanceof SliderSetting ss)  renderSlider(ctx, ss, px, sy, pw, alpha, anim);
        else if (s instanceof RangeSetting rs)   renderRange(ctx, rs, px, sy, pw, alpha);
        else if (s instanceof ItemSelectSetting itemSelect) renderItemSelect(ctx, itemSelect, px, sy, pw, alpha);
        else if (s instanceof MultiItemSelectSetting multiItemSelect) renderMultiItemSelect(ctx, multiItemSelect, px, sy, pw, alpha);
        else if (s instanceof ModeSetting ms)    renderMode(ctx, ms, px, sy, pw, alpha);
        else if (s instanceof SelectSetting sel) renderSelect(ctx, sel, px, sy, pw, alpha);
        else if (s instanceof ColorSetting cs)   renderColor(ctx, cs, px, sy, pw, alpha, mx, my);
        else if (s instanceof StringSetting str) renderString(ctx, str, px, sy, pw, alpha);
        else if (s instanceof BindSetting bind)  renderBind(ctx, bind, px, sy, pw, alpha);
        else if (s instanceof ClickSetting click) renderClick(ctx, click, px, sy, pw, alpha);
    }

    private float getSettingHeight(Setting<?> s) {
        if (s instanceof BooleanSetting)  return ROW_H;
        if (s instanceof SliderSetting)   return SLIDER_ROW_H;
        if (s instanceof RangeSetting)    return RANGE_ROW_H;
        if (s instanceof ItemSelectSetting itemSelect) {
            float anim = itemExpandAnims.getOrDefault(itemSelect, 0f);
            return MODE_HEADER_H + getItemSelectListHeight(itemSelect) * anim;
        }
        if (s instanceof MultiItemSelectSetting multiItemSelect) {
            float anim = multiItemExpandAnims.getOrDefault(multiItemSelect, 0f);
            return MODE_HEADER_H + getMultiItemSelectListHeight(multiItemSelect) * anim;
        }
        if (s instanceof ModeSetting ms) {
            float anim = modeExpandAnims.getOrDefault(ms, 0f);
            return MODE_HEADER_H + (ms.getModes().size() * MODE_OPT_H + 6) * anim;
        }
        if (s instanceof SelectSetting sel)
            return ROW_H + sel.getOptions().size() * SELECT_OPT_H + 6;
        if (s instanceof ColorSetting cs)
            return COLOR_ROW_H + COLOR_PICKER_FULL * colorExpandAnims.getOrDefault(cs, 0f);
        if (s instanceof StringSetting)   return STRING_ROW_H;
        if (s instanceof BindSetting)     return BIND_ROW_H;
        if (s instanceof ClickSetting)    return CLICK_ROW_H;
        return ROW_H;
    }

    private int getItemSelectVisibleCount(ItemSelectSetting setting) {
        int filtered = setting.getFilteredOptions().size();
        if (filtered <= 0) return 1;
        return Math.min(setting.getMaxVisibleOptions(), filtered);
    }

    private float getItemSelectListHeight(ItemSelectSetting setting) {
        return ITEM_SEARCH_H + ITEM_OPTION_GAP + getItemSelectVisibleCount(setting) * MODE_OPT_H + 6f;
    }

    private int getMultiItemSelectVisibleCount(MultiItemSelectSetting setting) {
        int filtered = setting.getFilteredOptions().size();
        if (filtered <= 0) return 1;
        return Math.min(setting.getMaxVisibleOptions(), filtered);
    }

    private float getMultiItemSelectListHeight(MultiItemSelectSetting setting) {
        return ITEM_SEARCH_H + ITEM_OPTION_GAP + getMultiItemSelectVisibleCount(setting) * MODE_OPT_H + 6f;
    }

                                                                                                                                                                            
                         
                                                                                                                                                                            

    private float renderEnabledToggle(DrawContext ctx, float px, float sy, float pw,
                                      float alpha, float speed) {
        CustomFontRenderer.drawText(ctx, "\u0412\u043a\u043b\u044e\u0447\u0435\u043d", px + 10, sy + 6.5f, 7.5f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * (0.6f + 0.4f * enableAnim)),
                FontWeight.REGULAR);

        renderToggleSwitch(ctx, getToggleX(px, pw), sy + (ENABLED_ROW_H - TOGGLE_H) / 2f, enableAnim, alpha);
        return sy + ENABLED_ROW_H;
    }

    private void drawSettingLabel(DrawContext ctx, String text, float x, float y, float maxWidth,
                                  float size, int color, FontWeight weight) {
        if (text == null || text.isBlank() || maxWidth <= 2f) return;

        float textW = CustomFontRenderer.getTextWidth(text, size, weight);
        if (textW <= maxWidth) {
            CustomFontRenderer.drawText(ctx, text, x, y, size, color, weight);
            return;
        }

        if (text.length() < MARQUEE_MIN_CHARS) {
            String trimmed = CustomFontRenderer.trimToWidth(text, maxWidth, size, weight);
            CustomFontRenderer.drawText(ctx, trimmed, x, y, size, color, weight);
            return;
        }

        float gap = Math.max(16f, maxWidth * 0.35f);
        float cycleW = textW + gap;
        boolean hovered = isHovered(x, y - 2f, maxWidth, size + 8f);
        float offset = hovered ? marqueeOffset(cycleW, 16f) : 0f;
        float fadeW = Math.min(16f, Math.max(8f, maxWidth * 0.32f));

        ctx.draw();
        enableLocalScissor(x, y - 1f, maxWidth, size + 4f);
        RenderUtil.drawTextWithEdgeFade(ctx, text, x - offset, y, x, maxWidth, fadeW, size, color, weight);
        if (hovered) {
            RenderUtil.drawTextWithEdgeFade(ctx, text, x - offset + cycleW, y, x, maxWidth, fadeW, size, color, weight);
        }
        ctx.draw();
        StencilUtil.disableScissor();
    }

    private void drawModeText(DrawContext ctx, String text, float x, float y, float maxWidth,
                              float size, int color, FontWeight weight, boolean alignRight) {
        if (text == null || text.isBlank() || maxWidth <= 2f) return;

        float textW = CustomFontRenderer.getTextWidth(text, size, weight);
        if (textW <= maxWidth) {
            if (alignRight) {
                CustomFontRenderer.drawRightText(ctx, text, x + maxWidth, y, size, color, weight);
            } else {
                CustomFontRenderer.drawText(ctx, text, x, y, size, color, weight);
            }
            return;
        }

        if (text.length() < MARQUEE_MIN_CHARS) {
            String trimmed = CustomFontRenderer.trimToWidth(text, maxWidth, size, weight);
            if (alignRight) {
                CustomFontRenderer.drawRightText(ctx, trimmed, x + maxWidth, y, size, color, weight);
            } else {
                CustomFontRenderer.drawText(ctx, trimmed, x, y, size, color, weight);
            }
            return;
        }

        float gap = Math.max(14f, maxWidth * 0.35f);
        float cycleW = textW + gap;
        boolean hovered = isHovered(x, y - 2f, maxWidth, size + 8f);
        float offset = hovered ? marqueeOffset(cycleW, 15f) : 0f;
        float fadeW = Math.min(16f, Math.max(8f, maxWidth * 0.32f));

        ctx.draw();
        enableLocalScissor(x, y - 1f, maxWidth, size + 4f);
        RenderUtil.drawTextWithEdgeFade(ctx, text, x - offset, y, x, maxWidth, fadeW, size, color, weight);
        if (hovered) {
            RenderUtil.drawTextWithEdgeFade(ctx, text, x - offset + cycleW, y, x, maxWidth, fadeW, size, color, weight);
        }
        ctx.draw();
        StencilUtil.disableScissor();
    }

    private float marqueeOffset(float cycleWidth, float pixelsPerSecond) {
        float speed = Math.max(8f, pixelsPerSecond);
        long cycleMs = Math.max(1L, (long) ((cycleWidth / speed) * 1000.0f));
        return (System.currentTimeMillis() % cycleMs) / (float) cycleMs * cycleWidth;
    }

    private boolean isHovered(float x, float y, float width, float height) {
        return renderMouseX >= x && renderMouseX <= x + width
                && renderMouseY >= y && renderMouseY <= y + height;
    }

    private void enableLocalScissor(float x, float y, float width, float height) {
        float sx = renderOriginX + (x - renderOriginX) * renderScale;
        float sy = renderOriginY + (y - renderOriginY) * renderScale;
        StencilUtil.enableScissor(sx, sy, width * renderScale, height * renderScale);
    }

    private void renderBoolean(DrawContext ctx, BooleanSetting s, float px, float sy, float pw,
                               float alpha, float anim) {
        drawSettingLabel(ctx, s.getName(), px + 10, sy + 4.5f, pw - 42f, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * (0.6f + 0.4f * anim)), FontWeight.REGULAR);
        renderToggleSwitch(ctx, getToggleX(px, pw), sy + (ROW_H - TOGGLE_H) / 2f, anim, alpha);
    }

    private void renderToggleSwitch(DrawContext ctx, float tx, float ty, float anim, float alpha) {
        float tw = TOGGLE_W;
        float th = TOGGLE_H;
        float base = clamp(anim, 0f, 1f);
        float smooth = RenderUtil.easeInOutCubic(base);
        float spring = RenderUtil.easeOutBack(base);
        float progress = clamp(smooth * 0.8f + spring * 0.2f, 0f, 1f);

        int track = lerpColor(
                applyAlpha(MenuScreen.TOGGLE_OFF, alpha * 0.95f),
                applyAlpha(MenuScreen.TOGGLE_ON, alpha),
                progress
        );

                                                                                        
        float r = th * 0.5f;
        RenderUtil.drawRect(ctx, tx + r, ty, tw - 2f * r, th, track);
        RenderUtil.drawCircle(ctx, tx + r, ty + r, r, track);
        RenderUtil.drawCircle(ctx, tx + tw - r, ty + r, r, track);

                                                          
        float jelly = 1f - Math.abs(progress * 2f - 1f);
        float knobW = 6.2f - jelly * 0.9f;
        float knobH = 6.2f + jelly * 0.5f;
        float overshoot = Math.max(0f, spring - 1f) * 0.8f;

        float knobTravel = tw - knobW - 2f;
        float knobX = tx + 1f + knobTravel * progress + overshoot;
        knobX = clamp(knobX, tx + 1f, tx + tw - 1f - knobW);
        float knobY = ty + (th - knobH) * 0.5f;

        int knob = applyAlpha(MenuScreen.TOGGLE_KNOB, alpha * (0.75f + 0.25f * progress));
        RenderUtil.drawEllipse(ctx, knobX + knobW * 0.5f, knobY + knobH * 0.5f, knobW * 0.5f, knobH * 0.5f, knob);
    }

    private float getToggleX(float px, float pw) {
        return px + pw - TOGGLE_RIGHT_PAD - TOGGLE_W;
    }

                                                                                                                                                                            
                      
                                                                                                                                                                            

    private void renderSlider(DrawContext ctx, SliderSetting s, float px, float sy, float pw, float alpha, float anim) {
        String displayValue = s.getDisplayValue();
        float valueW = CustomFontRenderer.getTextWidth(displayValue, 6.5f, FontWeight.REGULAR);
        drawSettingLabel(ctx, s.getName(), px + 10, sy + 3, pw - 30f - valueW, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);
        CustomFontRenderer.drawRightText(ctx, displayValue, px + pw - 10, sy + 3.5f, 6.5f,
                applyAlpha(MenuScreen.TEXT_DIM, alpha * 0.9f), FontWeight.REGULAR);

        float sx = px + 10, trackY = sy + 16, sw = pw - 20, sh = 3f;

        RenderUtil.drawRoundedRect(ctx, sx, trackY, sw, sh, 1.5f, applyAlpha(MenuScreen.SLIDER_BG, alpha * 0.6f));

        float fillW = sw * anim;
        if (fillW > 0.5f)
            RenderUtil.drawRoundedRect(ctx, sx, trackY, fillW, sh, 1.5f, applyAlpha(MenuScreen.SLIDER_FILL, alpha));

        float kx = sx + fillW, ky = trackY + sh / 2f;
        RenderUtil.drawCircle(ctx, kx, ky + 0.5f, 4f, applyAlpha(0xFF000000, alpha * 0.2f));
        RenderUtil.drawCircle(ctx, kx, ky, 3.5f, applyAlpha(MenuScreen.TEXT_WHITE, alpha));
    }

    private void renderRange(DrawContext ctx, RangeSetting s, float px, float sy, float pw, float alpha) {
        String val = String.format("%.1f - %.1f", s.getLow(), s.getHigh());
        float valueW = CustomFontRenderer.getTextWidth(val, 6.5f, FontWeight.REGULAR);
        drawSettingLabel(ctx, s.getName(), px + 10, sy + 3, pw - 30f - valueW, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);
        CustomFontRenderer.drawRightText(ctx, val, px + pw - 10, sy + 3.5f, 6.5f,
                applyAlpha(MenuScreen.TEXT_DIM, alpha * 0.9f), FontWeight.REGULAR);

        float sx = px + 10, trackY = sy + 16, sw = pw - 20, sh = 3f;

        RenderUtil.drawRoundedRect(ctx, sx, trackY, sw, sh, 1.5f, applyAlpha(MenuScreen.SLIDER_BG, alpha * 0.6f));

        float lowX  = sx + sw * s.getLowProgress();
        float highX = sx + sw * s.getHighProgress();
        if (highX - lowX > 0.5f)
            RenderUtil.drawRoundedRect(ctx, lowX, trackY, highX - lowX, sh, 1.5f, applyAlpha(MenuScreen.SLIDER_FILL, alpha));

        float ky = trackY + sh / 2f;
        for (float kx : new float[]{lowX, highX}) {
            RenderUtil.drawCircle(ctx, kx, ky + 0.5f, 4f, applyAlpha(0xFF000000, alpha * 0.2f));
            RenderUtil.drawCircle(ctx, kx, ky, 3.5f, applyAlpha(MenuScreen.TEXT_WHITE, alpha));
        }
    }

                                                                                                                                                                            
                                                                               
                                                                                                                                                                            

    private void renderItemSelect(DrawContext ctx, ItemSelectSetting s, float px, float sy, float pw, float alpha) {
        float expandAnim = itemExpandAnims.getOrDefault(s, 0f);

        drawSettingLabel(ctx, s.getName(), px + 10, sy + 4.5f, pw - 50f, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);

        if (expandAnim < 1f) {
            float fade = alpha * (1f - expandAnim);
            String selected = CustomFontRenderer.trimToWidth(s.getSelectedLabel(), pw - 44f, 6.5f, FontWeight.REGULAR);
            CustomFontRenderer.drawRightText(ctx, selected, px + pw - 24, sy + 4.5f, 6.5f,
                    applyAlpha(MenuScreen.TEXT_DIM, fade), FontWeight.REGULAR);
            renderDropIcon(ctx, px + pw - 14, sy + MODE_HEADER_H / 2f, 8, 4, expandAnim, fade);
        }

        if (expandAnim <= 0.01f) return;

        List<ItemSelectSetting.ItemOption> filtered = s.getFilteredOptions();
        int visible = getItemSelectVisibleCount(s);
        float scroll = itemListScrollOffsets.getOrDefault(s, 0f);

        float listY = sy + MODE_HEADER_H;
        float listH = getItemSelectListHeight(s);
        float visH = listH * expandAnim;

        ctx.draw();
        enableLocalScissor(px + 6, listY, pw - 12, visH);

        RenderUtil.drawRoundedRect(ctx, px + 8, listY, pw - 16, listH, 5,
                applyAlpha(MenuScreen.SETTING_PANEL_OUTER, alpha * 0.78f * expandAnim));
        RenderUtil.drawRoundedRect(ctx, px + 9, listY + 1, pw - 18, listH - 2, 4,
                applyAlpha(MenuScreen.SETTING_PANEL_INNER, alpha * 0.35f * expandAnim));

        float searchX = px + 12f;
        float searchY = listY + 4f;
        float searchW = pw - 24f;
        float searchH = ITEM_SEARCH_H - 2f;
        boolean editing = editingItemSearch == s;

        RenderUtil.drawRoundedRect(ctx, searchX, searchY, searchW, searchH, 3f,
                applyAlpha(MenuScreen.SETTING_INPUT_BG, alpha * expandAnim * 0.64f));

        String query = s.getSearchQuery();
        if (query.isEmpty()) {
            CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.search_item", "Поиск предмета..."), searchX + 4f, searchY + 2f, 6f,
                    applyAlpha(MenuScreen.TEXT_DIM, alpha * expandAnim * 0.9f), FontWeight.REGULAR);
        } else {
            drawAnimatedInlineText(ctx, query, searchX + 4f, searchY + 2f, 6f, searchW - 8f,
                    applyAlpha(MenuScreen.TEXT_WHITE, alpha * expandAnim * 0.95f),
                    typedItemSearchSetting == s ? typedItemSearchCharIndex : -1,
                    typedItemSearchSetting == s ? typedItemSearchCharStartMs : 0L);

        }

        if (editing && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cursor = Math.max(0, Math.min(itemSearchCursor, query.length()));
            float caretX = searchX + 4f + CustomFontRenderer.getTextWidth(query.substring(0, cursor), 6f, FontWeight.REGULAR);
            RenderUtil.drawRect(ctx, caretX, searchY + 2f, 0.5f, searchH - 4f,
                    applyAlpha(MenuScreen.TEXT_WHITE, alpha * expandAnim));
        }

        float optY = searchY + ITEM_SEARCH_H + ITEM_OPTION_GAP;
        float textX = px + 27f;
        if (filtered.isEmpty()) {
            CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.nothing_found", "Ничего не найдено"), px + 16f, optY + 4f, 6.5f,
                    applyAlpha(MenuScreen.TEXT_DIM, alpha * expandAnim * 0.9f), FontWeight.REGULAR);
        } else {
            float rowY = optY - scroll;
            for (ItemSelectSetting.ItemOption option : filtered) {
                boolean selected = option.id().equals(s.get());
                float optAlpha = alpha * expandAnim * (selected ? 1f : 0.55f);

                if (rowY + MODE_OPT_H >= optY - 1f && rowY <= optY + visible * MODE_OPT_H + 1f) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(px + 13f, rowY + 5.0f, 0f);
                    ctx.getMatrices().scale(0.5f, 0.5f, 1f);
                    ctx.drawItem(s.getItemStack(option.id()), 0, 0);
                    ctx.getMatrices().pop();

                    String label = CustomFontRenderer.trimToWidth(option.label(), pw - 58f, 6.5f, FontWeight.REGULAR);
                    CustomFontRenderer.drawText(ctx, label, textX, rowY + 4f, 6.5f,
                            applyAlpha(MenuScreen.TEXT_WHITE, optAlpha), FontWeight.REGULAR);

                    if (selected) {
                        Draw.image(ctx, MenuScreen.MODULE_ON_ICON, px + pw - 26, rowY + 5.1f, 8, 8,
                                MenuScreen.MOD_ICON_TEX, MenuScreen.MOD_ICON_TEX, alpha * expandAnim);
                    }
                }
                rowY += MODE_OPT_H;
            }
        }

        ctx.draw();
        StencilUtil.disableScissor();
    }

    private void renderMultiItemSelect(DrawContext ctx, MultiItemSelectSetting s, float px, float sy, float pw, float alpha) {
        float expandAnim = multiItemExpandAnims.getOrDefault(s, 0f);

        drawSettingLabel(ctx, s.getName(), px + 10, sy + 4.5f, pw - 50f, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);

        if (expandAnim < 1f) {
            float fade = alpha * (1f - expandAnim);
            int selectedCount = s.get().size();
            String selected = selectedCount <= 0
                    ? ClientLanguage.text("ui.none_selected", "Не выбрано")
                    : String.format(ClientLanguage.text("ui.selected_count", "Выбрано: %d"), selectedCount);
            selected = CustomFontRenderer.trimToWidth(selected, pw - 44f, 6.5f, FontWeight.REGULAR);
            CustomFontRenderer.drawRightText(ctx, selected, px + pw - 24, sy + 4.5f, 6.5f,
                    applyAlpha(MenuScreen.TEXT_DIM, fade), FontWeight.REGULAR);
            renderDropIcon(ctx, px + pw - 14, sy + MODE_HEADER_H / 2f, 8, 4, expandAnim, fade);
        }

        if (expandAnim <= 0.01f) return;

        List<MultiItemSelectSetting.ItemOption> filtered = s.getFilteredOptions();
        int visible = getMultiItemSelectVisibleCount(s);
        float scroll = multiItemListScrollOffsets.getOrDefault(s, 0f);

        float listY = sy + MODE_HEADER_H;
        float listH = getMultiItemSelectListHeight(s);
        float visH = listH * expandAnim;

        ctx.draw();
        enableLocalScissor(px + 6, listY, pw - 12, visH);

        RenderUtil.drawRoundedRect(ctx, px + 8, listY, pw - 16, listH, 5,
                applyAlpha(MenuScreen.SETTING_PANEL_OUTER, alpha * 0.78f * expandAnim));
        RenderUtil.drawRoundedRect(ctx, px + 9, listY + 1, pw - 18, listH - 2, 4,
                applyAlpha(MenuScreen.SETTING_PANEL_INNER, alpha * 0.35f * expandAnim));

        float searchX = px + 12f;
        float searchY = listY + 4f;
        float searchW = pw - 24f;
        float searchH = ITEM_SEARCH_H - 2f;
        boolean editing = editingMultiItemSearch == s;

        RenderUtil.drawRoundedRect(ctx, searchX, searchY, searchW, searchH, 3f,
                applyAlpha(MenuScreen.SETTING_INPUT_BG, alpha * expandAnim * 0.64f));

        String query = s.getSearchQuery();
        if (query.isEmpty()) {
            CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.search_item", "Поиск предмета..."), searchX + 4f, searchY + 2f, 6f,
                    applyAlpha(MenuScreen.TEXT_DIM, alpha * expandAnim * 0.9f), FontWeight.REGULAR);
        } else {
            drawAnimatedInlineText(ctx, query, searchX + 4f, searchY + 2f, 6f, searchW - 8f,
                    applyAlpha(MenuScreen.TEXT_WHITE, alpha * expandAnim * 0.95f),
                    typedMultiItemSearchSetting == s ? typedMultiItemSearchCharIndex : -1,
                    typedMultiItemSearchSetting == s ? typedMultiItemSearchCharStartMs : 0L);
        }

        if (editing && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cursor = Math.max(0, Math.min(itemSearchCursor, query.length()));
            float caretX = searchX + 4f + CustomFontRenderer.getTextWidth(query.substring(0, cursor), 6f, FontWeight.REGULAR);
            RenderUtil.drawRect(ctx, caretX, searchY + 2f, 0.5f, searchH - 4f,
                    applyAlpha(MenuScreen.TEXT_WHITE, alpha * expandAnim));
        }

        float optY = searchY + ITEM_SEARCH_H + ITEM_OPTION_GAP;
        float textX = px + 27f;
        if (filtered.isEmpty()) {
            CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.nothing_found", "Ничего не найдено"), px + 16f, optY + 4f, 6.5f,
                    applyAlpha(MenuScreen.TEXT_DIM, alpha * expandAnim * 0.9f), FontWeight.REGULAR);
        } else {
            float rowY = optY - scroll;
            for (MultiItemSelectSetting.ItemOption option : filtered) {
                boolean selected = s.isSelected(option.id());
                float optAlpha = alpha * expandAnim * (selected ? 1f : 0.55f);

                if (rowY + MODE_OPT_H >= optY - 1f && rowY <= optY + visible * MODE_OPT_H + 1f) {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(px + 13f, rowY + 5.0f, 0f);
                    ctx.getMatrices().scale(0.5f, 0.5f, 1f);
                    ctx.drawItem(s.getItemStack(option.id()), 0, 0);
                    ctx.getMatrices().pop();

                    String label = CustomFontRenderer.trimToWidth(option.label(), pw - 58f, 6.5f, FontWeight.REGULAR);
                    CustomFontRenderer.drawText(ctx, label, textX, rowY + 4f, 6.5f,
                            applyAlpha(MenuScreen.TEXT_WHITE, optAlpha), FontWeight.REGULAR);

                    if (selected) {
                        Draw.image(ctx, MenuScreen.MODULE_ON_ICON, px + pw - 26, rowY + 5.1f, 8, 8,
                                MenuScreen.MOD_ICON_TEX, MenuScreen.MOD_ICON_TEX, alpha * expandAnim);
                    }
                }
                rowY += MODE_OPT_H;
            }
        }

        ctx.draw();
        StencilUtil.disableScissor();
    }

    private void renderMode(DrawContext ctx, ModeSetting s, float px, float sy, float pw, float alpha) {
        float expandAnim = modeExpandAnims.getOrDefault(s, 0f);

        drawSettingLabel(ctx, s.getName(), px + 10, sy + 4.5f, pw - 50f, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);

        if (expandAnim < 1f) {
            float fade = alpha * (1f - expandAnim);
            float valueW = Math.min(45f, pw * 0.42f);
            drawModeText(ctx, s.getDisplayValue(), px + pw - 24f - valueW, sy + 4.5f, valueW, 6.5f,
                    applyAlpha(MenuScreen.TEXT_DIM, fade), FontWeight.REGULAR, true);
            renderDropIcon(ctx, px + pw - 14, sy + MODE_HEADER_H / 2f, 8, 4, expandAnim, fade);
        }

        if (expandAnim > 0.01f) {
            float listY = sy + MODE_HEADER_H;
            float listH = s.getModes().size() * MODE_OPT_H + 6;
            float visH = listH * expandAnim;

            ctx.draw();
            enableLocalScissor(px + 6, listY, pw - 12, visH);

                                                       
            RenderUtil.drawRoundedRect(ctx, px + 8, listY, pw - 16, listH, 5,
                    applyAlpha(MenuScreen.SETTING_PANEL_OUTER, alpha * 0.78f * expandAnim));
            RenderUtil.drawRoundedRect(ctx, px + 9, listY + 1, pw - 18, listH - 2, 4,
                    applyAlpha(MenuScreen.SETTING_PANEL_INNER, alpha * 0.35f * expandAnim));

            float optY = listY + 3;
            for (String mode : s.getModes()) {
                boolean sel = mode.equals(s.get());
                float optAlpha = alpha * expandAnim * (sel ? 1f : 0.5f);

                drawModeText(ctx, s.getDisplayMode(mode), px + 16, optY + 4f, pw - 48f, 6.5f,
                        applyAlpha(MenuScreen.TEXT_WHITE, optAlpha), FontWeight.REGULAR, false);

                if (sel) {
                    Draw.image(ctx, MenuScreen.MODULE_ON_ICON, px + pw - 26, optY + 5.1f, 8, 8,
                            MenuScreen.MOD_ICON_TEX, MenuScreen.MOD_ICON_TEX, alpha * expandAnim);
                }
                optY += MODE_OPT_H;
            }

            ctx.draw();
            StencilUtil.disableScissor();
        }
    }

    private void renderSelect(DrawContext ctx, SelectSetting s, float px, float sy, float pw, float alpha) {
        String count = s.get().size() + " \u0438\u0437 " + s.getOptions().size();
        float countW = CustomFontRenderer.getTextWidth(count, 6.5f, FontWeight.REGULAR);
        drawSettingLabel(ctx, s.getName(), px + 10, sy + 4.5f, pw - 30f - countW, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);
        CustomFontRenderer.drawRightText(ctx, count, px + pw - 10, sy + 4.5f, 6.5f,
                applyAlpha(MenuScreen.TEXT_DIM, alpha * 0.8f), FontWeight.REGULAR);

        float listY = sy + ROW_H;
        float listH = s.getOptions().size() * SELECT_OPT_H + 6;

        RenderUtil.drawRoundedRect(ctx, px + 8, listY, pw - 16, listH, 5,
                applyAlpha(MenuScreen.SETTING_PANEL_OUTER, alpha * 0.78f));
        RenderUtil.drawRoundedRect(ctx, px + 9, listY + 1, pw - 18, listH - 2, 4,
                applyAlpha(MenuScreen.SETTING_PANEL_INNER, alpha * 0.35f));

        float optY = listY + 3;
        for (String opt : s.getOptions()) {
            boolean sel = s.isSelected(opt);
            float optAlpha = alpha * (sel ? 1f : 0.5f);

            CustomFontRenderer.drawText(ctx, opt, px + 16, optY + 4f, 6.5f,
                    applyAlpha(MenuScreen.TEXT_WHITE, optAlpha), FontWeight.REGULAR);

            if (sel) {
                Draw.image(ctx, MenuScreen.MODULE_ON_ICON, px + pw - 26, optY + 5.1f, 8, 8,
                        MenuScreen.MOD_ICON_TEX, MenuScreen.MOD_ICON_TEX, alpha);
            }
            optY += SELECT_OPT_H;
        }
    }

                                                                                                                                                                            
                     
                                                                                                                                                                            

    private void renderString(DrawContext ctx, StringSetting s, float px, float sy, float pw, float alpha) {
        drawSettingLabel(ctx, s.getName(), px + 10, sy + 3, pw - 20f, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);

        float fx = px + 10, fy = sy + 13, fw = pw - 20, fh = 12;
        boolean editing = editingString == s;

        RenderUtil.drawRoundedRect(ctx, fx, fy, fw, fh, 3,
                applyAlpha(MenuScreen.SETTING_INPUT_BG, alpha * (editing ? 0.66f : 0.58f)));

        String text = s.get().isEmpty() ? s.getPlaceholder() : s.get();
        int tc = s.get().isEmpty() ? applyAlpha(MenuScreen.TEXT_DIM, alpha) : applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.9f);

        if (s.get().isEmpty()) {
            String trimmed = CustomFontRenderer.trimToWidth(text, fw - 6, 6.5f, FontWeight.REGULAR);
            CustomFontRenderer.drawText(ctx, trimmed, fx + 4, fy + 2.5f, 6.5f, tc, FontWeight.REGULAR);
        } else {
            drawAnimatedInlineText(ctx, text, fx + 4, fy + 2.5f, 6.5f, fw - 6f, tc,
                    typedStringSetting == s ? typedStringCharIndex : -1,
                    typedStringSetting == s ? typedStringCharStartMs : 0L);
        }

        if (editing && (System.currentTimeMillis() / 500) % 2 == 0) {
            float curX = fx + 4 + CustomFontRenderer.getTextWidth(text.substring(0, cursorPos), 6.5f, FontWeight.REGULAR);
            RenderUtil.drawRect(ctx, curX, fy + 2, 0.5f, fh - 4, applyAlpha(MenuScreen.TEXT_WHITE, alpha));
        }
    }

    private void renderBind(DrawContext ctx, BindSetting s, float px, float sy, float pw, float alpha) {
        boolean isBinding = bindingMode && bindingTarget == s;
        String keyText = isBinding ? "..." : s.getKeyName();

        float bw = Math.max(22, CustomFontRenderer.getTextWidth(keyText, 6f, FontWeight.REGULAR) + 10);
        float bx = px + pw - bw - 10;
        float by = sy + 2;
        float bh = 14;

        drawSettingLabel(ctx, s.getName(), px + 10, sy + 4.5f, bx - px - 16f, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);

        int bg = isBinding ? applyAlpha(MenuScreen.ACCENT, alpha * 0.3f) : applyAlpha(MenuScreen.SETTING_BG, alpha * 0.6f);
        RenderUtil.drawRoundedRect(ctx, bx, by, bw, bh, 4, bg);

        int tc = isBinding ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_WHITE;
        CustomFontRenderer.drawCenteredText(ctx, keyText, bx + bw / 2f, by + 3.5f, 6f,
                applyAlpha(tc, alpha * 0.9f), FontWeight.REGULAR);
    }

    private void renderClick(DrawContext ctx, ClickSetting s, float px, float sy, float pw, float alpha) {
        float bx = px + 10f;
        float by = sy + 3f;
        float bw = pw - 20f;
        float bh = 15f;
        boolean hovered = isHovered(bx, by, bw, bh);
        float speed = RenderUtil.getFrameDelta() / 22f;
        float hover = smooth(clickHoverAnims.getOrDefault(s, 0f), hovered ? 1f : 0f, 12f, speed);
        float press = smooth(clickPressAnims.getOrDefault(s, 0f), 0f, 16f, speed);
        clickHoverAnims.put(s, hover);
        clickPressAnims.put(s, press);

        float scale = 1f - press * 0.035f;
        float sx = bx + bw * (1f - scale) * 0.5f;
        float syy = by + bh * (1f - scale) * 0.5f;
        float sw = bw * scale;
        float sh = bh * scale;

        int bg = lerpColor(
                applyAlpha(MenuScreen.SETTING_BG, alpha * 0.68f),
                applyAlpha(0xFFFFFF, alpha * 0.14f),
                hover
        );
        RenderUtil.drawRoundedRect(ctx, sx, syy, sw, sh, 5f, bg);
        RenderUtil.drawRoundedRect(ctx, sx + 0.5f, syy + 0.5f, sw - 1f, sh - 1f, 4.5f,
                applyAlpha(0xFFFFFF, alpha * (0.025f + hover * 0.045f)));

        String label = CustomFontRenderer.trimToWidth(s.getName(), sw - 16f, 6.8f, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawCenteredText(ctx, label, sx + sw * 0.5f, syy + (sh - 6.8f) * 0.5f - 0.45f, 6.8f,
                applyAlpha(hover > 0.25f ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_GRAY, alpha * 0.98f),
                FontWeight.SEMIBOLD);
    }

                                                                                                                                                                            
                    
                                                                                                                                                                            

    private void renderColor(DrawContext ctx, ColorSetting s, float px, float sy, float pw, float alpha, int mx, int my) {
        float expandAnim = colorExpandAnims.getOrDefault(s, 0f);
        int currentColor = s.get();

        drawSettingLabel(ctx, s.getName(), px + 10, sy + 4.5f, pw - 48f, 7f,
                applyAlpha(MenuScreen.TEXT_WHITE, alpha * 0.7f), FontWeight.REGULAR);

                                                     
        RenderUtil.drawCircle(ctx, px + pw - 20, sy + COLOR_ROW_H / 2f, 4f, applyAlpha(currentColor | 0xFF000000, alpha));
        renderDropIcon(ctx, px + pw - 10, sy + COLOR_ROW_H / 2f, 8, 4, expandAnim, alpha * 0.5f);

        if (expandAnim > 0.05f) {
            float partAlpha = alpha * expandAnim;
            float margin = 10, topY = sy + COLOR_ROW_H + 2;
            float sqW = pw - margin * 2 - 16, sqX = px + margin, sqY = topY, sqH = Math.min(sqW, 70);

            pickerX = sqX; pickerY = sqY; pickerW = sqW; pickerH = sqH;

            if (draggingColorPart < 0 && expandedColor == s)
                Color.RGBtoHSB((currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF, editHSB);

                                     
            RenderUtil.drawRoundedRect(ctx, sqX - 1, sqY - 1, sqW + 2, sqH + 2, 2, applyAlpha(MenuScreen.SETTING_PANEL_OUTER, partAlpha * 0.3f));
            drawSVRect(ctx, sqX, sqY, sqW, sqH, editHSB[0], partAlpha);

            float indX = clamp(sqX + sqW * editHSB[1], sqX, sqX + sqW);
            float indY = clamp(sqY + sqH * (1f - editHSB[2]), sqY, sqY + sqH);
            RenderUtil.drawCircle(ctx, indX, indY, 3.5f, applyAlpha(0xFFFFFFFF, partAlpha));
            RenderUtil.drawCircle(ctx, indX, indY, 2.5f, applyAlpha(Color.HSBtoRGB(editHSB[0], editHSB[1], editHSB[2]) | 0xFF000000, partAlpha));

            float hueW = 8, hueX = px + pw - margin - hueW, hueY = sqY, hueH = sqH;
            hueBarX = hueX; hueBarY = hueY; hueBarW = hueW; hueBarH = hueH;

            RenderUtil.drawRoundedRect(ctx, hueX - 1, hueY - 1, hueW + 2, hueH + 2, 2, applyAlpha(MenuScreen.SETTING_PANEL_OUTER, partAlpha * 0.3f));
            drawVerticalHueBar(ctx, hueX, hueY, hueW, hueH, partAlpha);

            float hueIndY = clamp(hueY + hueH * editHSB[0], hueY + 2, hueY + hueH - 2);
            RenderUtil.drawRoundedRect(ctx, hueX - 1f, hueIndY - 1.5f, hueW + 2, 3, 1.5f, applyAlpha(0xFFFFFFFF, partAlpha));
        }
    }

    private void drawSVRect(DrawContext ctx, float x, float y, float w, float h, float hue, float alpha) {
        ctx.draw(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int ix = 0; ix < 20; ix++) {
            for (int iy = 0; iy < 15; iy++) {
                float s1 = (float) ix / 20, s2 = (float) (ix + 1) / 20;
                float v1 = 1f - (float) iy / 15, v2 = 1f - (float) (iy + 1) / 15;
                float px1 = x + w / 20 * ix, px2 = x + w / 20 * (ix + 1);
                float py1 = y + h / 15 * iy, py2 = y + h / 15 * (iy + 1);
                putHSBVertex(builder, mat, px1, py1, hue, s1, v1, alpha);
                putHSBVertex(builder, mat, px1, py2, hue, s1, v2, alpha);
                putHSBVertex(builder, mat, px2, py2, hue, s2, v2, alpha);
                putHSBVertex(builder, mat, px2, py1, hue, s2, v1, alpha);
            }
        }
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private void drawVerticalHueBar(DrawContext ctx, float x, float y, float w, float h, float alpha) {
        ctx.draw(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < 20; i++) {
            int c1 = Color.HSBtoRGB((float) i / 20, 1f, 1f), c2 = Color.HSBtoRGB((float) (i + 1) / 20, 1f, 1f);
            float sy1 = y + h / 20 * i, sy2 = y + h / 20 * (i + 1);
            builder.vertex(mat, x, sy1, 0).color(((c1>>16)&0xFF)/255f, ((c1>>8)&0xFF)/255f, (c1&0xFF)/255f, alpha);
            builder.vertex(mat, x, sy2, 0).color(((c2>>16)&0xFF)/255f, ((c2>>8)&0xFF)/255f, (c2&0xFF)/255f, alpha);
            builder.vertex(mat, x + w, sy2, 0).color(((c2>>16)&0xFF)/255f, ((c2>>8)&0xFF)/255f, (c2&0xFF)/255f, alpha);
            builder.vertex(mat, x + w, sy1, 0).color(((c1>>16)&0xFF)/255f, ((c1>>8)&0xFF)/255f, (c1&0xFF)/255f, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private void putHSBVertex(BufferBuilder b, Matrix4f m, float x, float y, float h, float s, float v, float a) {
        int c = Color.HSBtoRGB(h, s, v);
        b.vertex(m, x, y, 0).color(((c>>16)&0xFF)/255f, ((c>>8)&0xFF)/255f, (c&0xFF)/255f, a);
    }

    private void renderDropIcon(DrawContext ctx, float cx, float cy, float w, float h, float anim, float alpha) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().multiply(new org.joml.Quaternionf().rotateZ((float) Math.toRadians(180f * anim)));
        ctx.getMatrices().translate(-cx, -cy, 0);
        Draw.image(ctx, DROP_ICON, cx - w / 2f, cy - h / 2f, w, h, 16, 16, alpha);
        ctx.getMatrices().pop();
    }

                                                                                                                                                                            
                      
                                                                                                                                                                            

    public void mouseClicked(float mx, float my, int button, float px, float cy, float pw, float ch, MenuScreen.PanelData panel) {
        if (bindingMode && bindingTarget != null) {
            bindingTarget.set(fun.enoughvisuals.utils.system.BindKeyUtil.mouseButtonToCode(button));
            bindingMode = false;
            bindingTarget = null;
            return;
        }

        float sy = cy + scrollOffset + PADDING;

        float enabledToggleX = getToggleX(px, pw);
        if (mx >= enabledToggleX - 2 && mx <= enabledToggleX + TOGGLE_W + 2
                && my >= sy && my <= sy + ENABLED_ROW_H) {
            module.toggle();
            return;
        }
        sy += ENABLED_ROW_H + SEP_H;

        boolean handled = false;
        for (Setting<?> s : module.getVisibleSettings()) {
            float h = getSettingHeight(s);
            if (my >= sy && my <= sy + h) {
                if (handleClick(s, mx, my, button, px, sy, pw)) {
                    handled = true;
                    break;
                }
            }
            sy += h;
        }
        if (!handled && button == 0) {
            editingString = null;
            editingItemSearch = null;
            editingMultiItemSearch = null;
            typedStringSetting = null;
            typedItemSearchSetting = null;
            typedMultiItemSearchSetting = null;
            typedItemSearchCharIndex = -1;
            typedItemSearchCharStartMs = 0L;
            typedMultiItemSearchCharIndex = -1;
            typedMultiItemSearchCharStartMs = 0L;
        }
    }

    private boolean handleClick(Setting<?> s, float mx, float my, int button,
                                float px, float sy, float pw) {
        if (!(s instanceof ItemSelectSetting) && button == 0) {
            editingItemSearch = null;
            typedItemSearchSetting = null;
        }
        if (!(s instanceof MultiItemSelectSetting) && button == 0) {
            editingMultiItemSearch = null;
            typedMultiItemSearchSetting = null;
        }
        if (!(s instanceof StringSetting) && button == 0) {
            typedStringSetting = null;
        }
        if (s instanceof BooleanSetting bs) {
            float toggleX = getToggleX(px, pw);
            if (mx >= toggleX - 2 && mx <= toggleX + TOGGLE_W + 2) { bs.toggle(); return true; }
        } else if (s instanceof SliderSetting ss) {
            if (my >= sy + 12 && my <= sy + 22) {
                ss.setFromProgress(clamp((mx - (px + 10)) / (pw - 20)));
                draggingSetting = ss; return true;
            }
        } else if (s instanceof RangeSetting rs) {
            if (my >= sy + 12 && my <= sy + 22) {
                float prog = clamp((mx - (px + 10)) / (pw - 20));
                draggingLow = Math.abs(prog - rs.getLowProgress()) < Math.abs(prog - rs.getHighProgress());
                if (draggingLow) rs.setLow(rs.getMin() + (rs.getMax() - rs.getMin()) * prog);
                else rs.setHigh(rs.getMin() + (rs.getMax() - rs.getMin()) * prog);
                draggingSetting = rs; return true;
            }
        } else if (s instanceof ItemSelectSetting itemSelect) {
            if (my >= sy && my <= sy + MODE_HEADER_H && button == 0) {
                if (expandedItemSelect == itemSelect) {
                    expandedItemSelect = null;
                    editingItemSearch = null;
                } else {
                    expandedItemSelect = itemSelect;
                    expandedMultiItemSelect = null;
                    expandedMode = null;
                    editingMultiItemSearch = null;
                    editingItemSearch = null;
                    itemSearchCursor = itemSelect.getSearchQuery().length();
                }
                return true;
            }
            if (itemExpandAnims.getOrDefault(itemSelect, 0f) > 0.5f) {
                float listY = sy + MODE_HEADER_H;
                float searchX = px + 12f;
                float searchY = listY + 4f;
                float searchW = pw - 24f;
                float searchH = ITEM_SEARCH_H - 2f;

                if (mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + searchH) {
                    editingItemSearch = itemSelect;
                    editingString = null;
                    typedStringSetting = null;
                    itemSearchCursor = itemSelect.getSearchQuery().length();
                    typedItemSearchSetting = itemSelect;
                    return true;
                }

                List<ItemSelectSetting.ItemOption> filtered = itemSelect.getFilteredOptions();
                int visible = getItemSelectVisibleCount(itemSelect);
                float optY = searchY + ITEM_SEARCH_H + ITEM_OPTION_GAP;
                float scroll = itemListScrollOffsets.getOrDefault(itemSelect, 0f);
                float rowY = optY - scroll;
                float visibleBottom = optY + visible * MODE_OPT_H;
                for (ItemSelectSetting.ItemOption option : filtered) {
                    if (rowY + MODE_OPT_H >= optY && rowY <= visibleBottom
                            && mx >= px + 8 && mx <= px + pw - 8 && my >= rowY && my <= rowY + MODE_OPT_H) {
                        itemSelect.set(option.id());
                        expandedItemSelect = null;
                        editingItemSearch = null;
                        typedItemSearchSetting = null;
                        typedItemSearchCharIndex = -1;
                        typedItemSearchCharStartMs = 0L;
                        return true;
                    }
                    rowY += MODE_OPT_H;
                }

                if (button == 0 && editingItemSearch == itemSelect) {
                    editingItemSearch = null;
                    typedItemSearchSetting = null;
                }
            }
        } else if (s instanceof MultiItemSelectSetting multiItemSelect) {
            if (my >= sy && my <= sy + MODE_HEADER_H && button == 0) {
                if (expandedMultiItemSelect == multiItemSelect) {
                    expandedMultiItemSelect = null;
                    editingMultiItemSearch = null;
                } else {
                    expandedMultiItemSelect = multiItemSelect;
                    expandedMode = null;
                    expandedItemSelect = null;
                    editingItemSearch = null;
                    editingMultiItemSearch = null;
                    itemSearchCursor = multiItemSelect.getSearchQuery().length();
                }
                return true;
            }
            if (multiItemExpandAnims.getOrDefault(multiItemSelect, 0f) > 0.5f) {
                float listY = sy + MODE_HEADER_H;
                float searchX = px + 12f;
                float searchY = listY + 4f;
                float searchW = pw - 24f;
                float searchH = ITEM_SEARCH_H - 2f;

                if (mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + searchH) {
                    editingMultiItemSearch = multiItemSelect;
                    editingString = null;
                    typedStringSetting = null;
                    itemSearchCursor = multiItemSelect.getSearchQuery().length();
                    typedMultiItemSearchSetting = multiItemSelect;
                    return true;
                }

                List<MultiItemSelectSetting.ItemOption> filtered = multiItemSelect.getFilteredOptions();
                int visible = getMultiItemSelectVisibleCount(multiItemSelect);
                float optY = searchY + ITEM_SEARCH_H + ITEM_OPTION_GAP;
                float scroll = multiItemListScrollOffsets.getOrDefault(multiItemSelect, 0f);
                float rowY = optY - scroll;
                float visibleBottom = optY + visible * MODE_OPT_H;
                for (MultiItemSelectSetting.ItemOption option : filtered) {
                    if (rowY + MODE_OPT_H >= optY && rowY <= visibleBottom
                            && mx >= px + 8 && mx <= px + pw - 8 && my >= rowY && my <= rowY + MODE_OPT_H) {
                        multiItemSelect.toggle(option.id());
                        return true;
                    }
                    rowY += MODE_OPT_H;
                }

                if (button == 0 && editingMultiItemSearch == multiItemSelect) {
                    editingMultiItemSearch = null;
                    typedMultiItemSearchSetting = null;
                }
            }
        } else if (s instanceof ModeSetting ms) {
            if (my >= sy && my <= sy + MODE_HEADER_H && button == 0) {
                expandedMode = (expandedMode == ms) ? null : ms;
                if (expandedMode != null) {
                    expandedItemSelect = null;
                    expandedMultiItemSelect = null;
                    editingItemSearch = null;
                    editingMultiItemSearch = null;
                }
                return true;
            }
            if (modeExpandAnims.getOrDefault(ms, 0f) > 0.5f) {
                float optY = sy + MODE_HEADER_H + 3;
                for (String mode : ms.getModes()) {
                    if (mx >= px + 8 && mx <= px + pw - 8 && my >= optY && my <= optY + MODE_OPT_H) {
                        ms.set(mode); expandedMode = null; return true;
                    }
                    optY += MODE_OPT_H;
                }
            }
        } else if (s instanceof SelectSetting sel) {
            float optY = sy + ROW_H + 3;
            for (String opt : sel.getOptions()) {
                if (mx >= px + 8 && mx <= px + pw - 8 && my >= optY && my <= optY + SELECT_OPT_H) {
                    sel.toggle(opt); return true;
                }
                optY += SELECT_OPT_H;
            }
        } else if (s instanceof ColorSetting cs) {
            if (my >= sy && my <= sy + COLOR_ROW_H && button == 0) {
                expandedColor = (expandedColor == cs) ? null : cs; return true;
            }
            if (cs == expandedColor && colorExpandAnims.getOrDefault(cs, 0f) > 0.5f) {
                if (mx >= pickerX && mx <= pickerX + pickerW && my >= pickerY && my <= pickerY + pickerH) {
                    draggingColorPart = 0; updateSVFromMouse(cs, mx, my); draggingSetting = cs; return true;
                }
                if (mx >= hueBarX && mx <= hueBarX + hueBarW && my >= hueBarY && my <= hueBarY + hueBarH) {
                    draggingColorPart = 1; updateHueFromMouse(cs, my); draggingSetting = cs; return true;
                }
            }
        } else if (s instanceof StringSetting ss) {
            float fx = px + 10, fy = sy + 13, fw = pw - 20, fh = 12;
            if (mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh) {
                editingString = ss;
                editingItemSearch = null;
                editingMultiItemSearch = null;
                cursorPos = ss.get().length();
                typedItemSearchSetting = null;
                typedMultiItemSearchSetting = null;
                typedStringSetting = ss;
                return true;
            } else {
                editingString = null;
                typedStringSetting = null;
            }
        } else if (s instanceof BindSetting bs) {
            if (mx >= px + pw - 40 && my >= sy + 2 && my <= sy + 16) {
                bindingMode = true; bindingTarget = bs; return true;
            }
        } else if (s instanceof ClickSetting click) {
            if (button == 0 && mx >= px + 10f && mx <= px + pw - 10f
                    && my >= sy + 3f && my <= sy + 18f) {
                clickPressAnims.put(click, 1f);
                EnoughSoundPlayer.playScroll();
                click.click();
                return true;
            }
        }
        return false;
    }

    private void updateSVFromMouse(ColorSetting cs, float mx, float my) {
        editHSB[1] = clamp((mx - pickerX) / pickerW);
        editHSB[2] = 1f - clamp((my - pickerY) / pickerH);
        int alpha = cs.get() & 0xFF000000;
        int rgb = Color.HSBtoRGB(editHSB[0], editHSB[1], editHSB[2]) & 0x00FFFFFF;
        cs.set(alpha | rgb);
    }

    private void updateHueFromMouse(ColorSetting cs, float my) {
        editHSB[0] = clamp((my - hueBarY) / hueBarH);
        int alpha = cs.get() & 0xFF000000;
        int rgb = Color.HSBtoRGB(editHSB[0], editHSB[1], editHSB[2]) & 0x00FFFFFF;
        cs.set(alpha | rgb);
    }

    public void mouseReleased(float mx, float my, int button) {
        draggingSetting = null; draggingColorPart = -1;
    }

    public boolean mouseDragged(float mx, float my, int button, float px, float cy, float pw) {
        if (draggingSetting instanceof SliderSetting ss) {
            ss.setFromProgress(clamp((mx - (px + 10)) / (pw - 20))); return true;
        }
        if (draggingSetting instanceof RangeSetting rs) {
            float prog = clamp((mx - (px + 10)) / (pw - 20));
            double val = rs.getMin() + (rs.getMax() - rs.getMin()) * prog;
            if (draggingLow) rs.setLow(val); else rs.setHigh(val); return true;
        }
        if (draggingSetting instanceof ColorSetting cs && draggingColorPart >= 0) {
            if (draggingColorPart == 0) updateSVFromMouse(cs, mx, my); else updateHueFromMouse(cs, my); return true;
        }
        return false;
    }

    public void mouseScrolled(float mx, float my, double amount, float px, float cy, float pw) {
        if (scrollItemDropdown(mx, my, amount, px, cy, pw)) {
            return;
        }
        scrollTarget += (float) (amount * 18);
        scrollTarget = Math.max(-maxScroll, Math.min(0, scrollTarget));
    }

    private boolean scrollItemDropdown(float mx, float my, double amount, float px, float cy, float pw) {
        float sy = cy + scrollOffset + PADDING + ENABLED_ROW_H + SEP_H;
        for (Setting<?> setting : module.getVisibleSettings()) {
            float h = getSettingHeight(setting);
            if (setting instanceof ItemSelectSetting itemSelect
                    && itemSelect == expandedItemSelect
                    && itemExpandAnims.getOrDefault(itemSelect, 0f) > 0.2f) {
                float listY = sy + MODE_HEADER_H;
                float searchY = listY + 4f;
                float optionsY = searchY + ITEM_SEARCH_H + ITEM_OPTION_GAP;
                float optionsH = getItemSelectVisibleCount(itemSelect) * MODE_OPT_H;
                if (mx >= px + 8f && mx <= px + pw - 8f
                        && my >= optionsY && my <= optionsY + optionsH) {
                    int visible = getItemSelectVisibleCount(itemSelect);
                    int total = itemSelect.getFilteredOptions().size();
                    float maxListScroll = Math.max(0f, (total - visible) * MODE_OPT_H);
                    float currentTarget = itemListScrollTargets.getOrDefault(itemSelect, 0f);
                    float nextTarget = clamp(currentTarget - (float) (amount * MODE_OPT_H), 0f, maxListScroll);
                    itemListScrollTargets.put(itemSelect, nextTarget);
                    return true;
                }
            }
            if (setting instanceof MultiItemSelectSetting multiItemSelect
                    && multiItemSelect == expandedMultiItemSelect
                    && multiItemExpandAnims.getOrDefault(multiItemSelect, 0f) > 0.2f) {
                float listY = sy + MODE_HEADER_H;
                float searchY = listY + 4f;
                float optionsY = searchY + ITEM_SEARCH_H + ITEM_OPTION_GAP;
                float optionsH = getMultiItemSelectVisibleCount(multiItemSelect) * MODE_OPT_H;
                if (mx >= px + 8f && mx <= px + pw - 8f
                        && my >= optionsY && my <= optionsY + optionsH) {
                    int visible = getMultiItemSelectVisibleCount(multiItemSelect);
                    int total = multiItemSelect.getFilteredOptions().size();
                    float maxListScroll = Math.max(0f, (total - visible) * MODE_OPT_H);
                    float currentTarget = multiItemListScrollTargets.getOrDefault(multiItemSelect, 0f);
                    float nextTarget = clamp(currentTarget - (float) (amount * MODE_OPT_H), 0f, maxListScroll);
                    multiItemListScrollTargets.put(multiItemSelect, nextTarget);
                    return true;
                }
            }
            sy += h;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode) {
        if (bindingMode && bindingTarget != null) {
            if (keyCode == 256 || keyCode == 261) bindingTarget.unbind(); else bindingTarget.set(keyCode);
            bindingMode = false; bindingTarget = null; return true;
        }
        if (editingItemSearch != null) {
            String query = editingItemSearch.getSearchQuery();
            itemSearchCursor = Math.max(0, Math.min(itemSearchCursor, query.length()));
            switch (keyCode) {
                case 256, 257 -> {
                    editingItemSearch = null;
                    typedItemSearchSetting = null;
                    typedItemSearchCharIndex = -1;
                    typedItemSearchCharStartMs = 0L;
                    return true;
                }
                case 259 -> {
                    if (itemSearchCursor > 0) {
                        editingItemSearch.setSearchQuery(
                                query.substring(0, itemSearchCursor - 1) + query.substring(itemSearchCursor)
                        );
                        itemSearchCursor--;
                        typedItemSearchCharIndex = -1;
                        typedItemSearchCharStartMs = 0L;
                        EnoughSoundPlayer.playScroll();
                    }
                    return true;
                }
                case 261 -> {
                    if (itemSearchCursor < query.length()) {
                        editingItemSearch.setSearchQuery(
                                query.substring(0, itemSearchCursor) + query.substring(itemSearchCursor + 1)
                        );
                        typedItemSearchCharIndex = -1;
                        typedItemSearchCharStartMs = 0L;
                        EnoughSoundPlayer.playScroll();
                    }
                    return true;
                }
                case 263 -> {
                    if (itemSearchCursor > 0) itemSearchCursor--;
                    return true;
                }
                case 262 -> {
                    if (itemSearchCursor < query.length()) itemSearchCursor++;
                    return true;
                }
                default -> {
                    return true;
                }
            }
        }
        if (editingMultiItemSearch != null) {
            String query = editingMultiItemSearch.getSearchQuery();
            itemSearchCursor = Math.max(0, Math.min(itemSearchCursor, query.length()));
            switch (keyCode) {
                case 256, 257 -> {
                    editingMultiItemSearch = null;
                    typedMultiItemSearchSetting = null;
                    typedMultiItemSearchCharIndex = -1;
                    typedMultiItemSearchCharStartMs = 0L;
                    return true;
                }
                case 259 -> {
                    if (itemSearchCursor > 0) {
                        editingMultiItemSearch.setSearchQuery(
                                query.substring(0, itemSearchCursor - 1) + query.substring(itemSearchCursor)
                        );
                        itemSearchCursor--;
                        typedMultiItemSearchCharIndex = -1;
                        typedMultiItemSearchCharStartMs = 0L;
                        EnoughSoundPlayer.playScroll();
                    }
                    return true;
                }
                case 261 -> {
                    if (itemSearchCursor < query.length()) {
                        editingMultiItemSearch.setSearchQuery(
                                query.substring(0, itemSearchCursor) + query.substring(itemSearchCursor + 1)
                        );
                        typedMultiItemSearchCharIndex = -1;
                        typedMultiItemSearchCharStartMs = 0L;
                        EnoughSoundPlayer.playScroll();
                    }
                    return true;
                }
                case 263 -> {
                    if (itemSearchCursor > 0) itemSearchCursor--;
                    return true;
                }
                case 262 -> {
                    if (itemSearchCursor < query.length()) itemSearchCursor++;
                    return true;
                }
                default -> {
                    return true;
                }
            }
        }
        if (editingString != null) {
            String text = editingString.get();
            cursorPos = Math.max(0, Math.min(cursorPos, text.length()));
            switch (keyCode) {
                case 256, 257 -> {
                    editingString = null;
                    typedStringSetting = null;
                    typedStringCharIndex = -1;
                    typedStringCharStartMs = 0L;
                    return true;
                }
                case 259 -> {
                    if (cursorPos > 0) {
                        editingString.set(text.substring(0, cursorPos - 1) + text.substring(cursorPos));
                        cursorPos--;
                        typedStringCharIndex = -1;
                        typedStringCharStartMs = 0L;
                        EnoughSoundPlayer.playScroll();
                    }
                    return true;
                }
                case 261 -> {
                    if (cursorPos < text.length()) {
                        editingString.set(text.substring(0, cursorPos) + text.substring(cursorPos + 1));
                        typedStringCharIndex = -1;
                        typedStringCharStartMs = 0L;
                        EnoughSoundPlayer.playScroll();
                    }
                    return true;
                }
                case 263 -> { if (cursorPos > 0) cursorPos--; return true; }
                case 262 -> { if (cursorPos < text.length()) cursorPos++; return true; }
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (editingItemSearch != null) {
            if (Character.isISOControl(chr)) return true;
            String query = editingItemSearch.getSearchQuery();
            itemSearchCursor = Math.max(0, Math.min(itemSearchCursor, query.length()));
            typedItemSearchSetting = editingItemSearch;
            typedItemSearchCharIndex = itemSearchCursor;
            typedItemSearchCharStartMs = System.currentTimeMillis();
            editingItemSearch.setSearchQuery(query.substring(0, itemSearchCursor) + chr + query.substring(itemSearchCursor));
            itemSearchCursor++;
            EnoughSoundPlayer.playScroll();
            return true;
        }
        if (editingMultiItemSearch != null) {
            if (Character.isISOControl(chr)) return true;
            String query = editingMultiItemSearch.getSearchQuery();
            itemSearchCursor = Math.max(0, Math.min(itemSearchCursor, query.length()));
            typedMultiItemSearchSetting = editingMultiItemSearch;
            typedMultiItemSearchCharIndex = itemSearchCursor;
            typedMultiItemSearchCharStartMs = System.currentTimeMillis();
            editingMultiItemSearch.setSearchQuery(query.substring(0, itemSearchCursor) + chr + query.substring(itemSearchCursor));
            itemSearchCursor++;
            EnoughSoundPlayer.playScroll();
            return true;
        }
        if (editingString != null) {
            if (!editingString.isCharAllowed(chr)) {
                return true;
            }
            String text = editingString.get();
            cursorPos = Math.max(0, Math.min(cursorPos, text.length()));
            typedStringSetting = editingString;
            typedStringCharIndex = cursorPos;
            typedStringCharStartMs = System.currentTimeMillis();
            editingString.set(text.substring(0, cursorPos) + chr + text.substring(cursorPos));
            cursorPos++;
            EnoughSoundPlayer.playScroll();
            return true;
        }
        return false;
    }

    private void drawAnimatedInlineText(DrawContext ctx, String text, float x, float y, float size, float maxWidth,
                                        int color, int typedIndex, long typedStartMs) {
        if (text == null) text = "";
        if (text.isEmpty()) return;

        String trimmed = CustomFontRenderer.trimToWidth(text, maxWidth, size, FontWeight.REGULAR);
        int safeTypedIndex = typedIndex;
        if (trimmed.length() != text.length()) {
            text = trimmed;
            if (safeTypedIndex >= text.length()) {
                safeTypedIndex = -1;
            }
        }

        RenderUtil.drawAnimatedInputText(ctx, text, x, y, size, color, FontWeight.REGULAR, safeTypedIndex, typedStartMs);
    }

    private static int applyAlpha(int color, float alpha) {
        return ((int) (((color >> 24) & 0xFF) * clamp(alpha, 0f, 1f)) << 24) | (color & 0x00FFFFFF);
    }
    private static int lerpColor(int c1, int c2, float t) {
        t = clamp(t, 0f, 1f);
        int a = (int) (((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >> 8) & 0xFF)  + (((c2 >> 8) & 0xFF)  - ((c1 >> 8) & 0xFF))  * t);
        int b = (int) ((c1 & 0xFF)         + ((c2 & 0xFF)         - (c1 & 0xFF))         * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    private static float smooth(float c, float t, float s, float d) { return c + (t - c) * Math.min(1f, s * d); }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    public Module getModule() { return module; }
}
