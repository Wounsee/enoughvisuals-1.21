package punchy.client.config;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.class_1074;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_357;
import net.minecraft.class_364;
import net.minecraft.class_407;
import net.minecraft.class_410;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5481;
import net.minecraft.class_7923;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.debug.PunchyDebug;
import punchy.client.tuning.MixThePacksScreen;
import punchy.client.tuning.TuningWorkbenchScreen;
import punchy.config.PunchyConfig;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen.class */
public final class PunchyConfigScreen extends class_437 {
    private static final int CONTENT_WIDTH = 320;
    private static final int SIDEBAR_WIDTH = 120;
    private static final int SIDEBAR_GAP = 16;
    private static final int MAIN_PANEL_GAP = 12;
    private static final int PROFILE_PANEL_WIDTH = 220;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 6;
    private static final int RESET_GAP = 10;
    private static final int RESET_WIDTH = 44;
    private static final int BLACKLIST_MODAL_ROW_HEIGHT = 22;
    private static final int BLACKLIST_MODAL_FILTER_ROWS = 2;
    private static final String BLACKLIST_BULK_ENABLE_KEY = "__bulk_enable__";
    private static final String BLACKLIST_BULK_DUAL_KEY = "__bulk_dual__";
    private final class_437 parent;
    private final PunchyConfig.Data data;
    private Tab tab;
    private class_342 blacklistInput;
    private String blacklistDraft;
    private int dividerX;
    private int dividerTop;
    private int dividerBottom;
    private List<class_5481> blacklistHelpLines;
    private int blacklistHelpX;
    private int blacklistHelpY;
    private int blacklistHelpWidth;
    private int blacklistHelpHeight;
    private final List<class_4185> blacklistEntryButtons;
    private final List<class_4185> blacklistDualButtons;
    private final List<class_4185> blacklistRemoveButtons;
    private int blacklistScrollOffset;
    private int blacklistListStartY;
    private int blacklistViewportHeight;
    private int blacklistEntryHeight;
    private int blacklistScrollbarReserve;
    private final List<class_339> animationWidgets;
    private final List<Integer> animationWidgetBaseY;
    private int animationScrollOffset;
    private int animationListStartY;
    private int animationViewportHeight;
    private int animationEntryHeight;
    private int animationRowCount;
    private boolean draggingBlacklistScrollbar;
    private boolean draggingAnimationScrollbar;
    private boolean draggingBlacklistModalScrollbar;
    private boolean draggingBlacklistNamespaceScrollbar;
    private boolean blacklistModalOpen;
    private class_342 blacklistSearchInput;
    private String blacklistSearchDraft;
    private int blacklistModalLeft;
    private int blacklistModalTop;
    private int blacklistModalRight;
    private int blacklistModalBottom;
    private int blacklistListLeft;
    private int blacklistListRight;
    private int blacklistListTop;
    private int blacklistListBottom;
    private int blacklistCloseButtonLeft;
    private int blacklistCloseButtonTop;
    private int blacklistCloseButtonRight;
    private int blacklistCloseButtonBottom;
    private int blacklistResetButtonLeft;
    private int blacklistResetButtonTop;
    private int blacklistResetButtonRight;
    private int blacklistResetButtonBottom;
    private int blacklistDoneButtonLeft;
    private int blacklistDoneButtonTop;
    private int blacklistDoneButtonRight;
    private int blacklistDoneButtonBottom;
    private int blacklistModalScrollOffset;
    private int blacklistModalMaxScroll;
    private int blacklistNamespaceScrollOffset;
    private int blacklistNamespaceMaxScroll;
    private int blacklistNamespaceAreaLeft;
    private int blacklistNamespaceAreaRight;
    private int blacklistNamespaceAreaTop;
    private int blacklistNamespaceAreaBottom;
    private final List<BlacklistCatalogEntry> blacklistCatalogEntries;
    private final List<BlacklistCatalogEntry> blacklistFilteredEntries;
    private final List<BlacklistModFilter> blacklistModFilters;
    private String blacklistSelectedNamespace;
    private final List<BlacklistHitbox> blacklistNamespaceHitboxes;
    private final List<BlacklistHitbox> blacklistEnableHitboxes;
    private final List<BlacklistHitbox> blacklistDualHitboxes;
    private final List<class_4185> profileRowButtons;
    private List<String> profiles;
    private class_342 profileNameInput;
    private class_4185 profileSaveButton;
    private class_4185 profileLoadButton;
    private class_4185 profileDeleteButton;
    private class_4185 profileOpenFolderButton;
    private class_4185 supportDonateButton;
    private int selectedProfile;
    private String activeProfileName;
    private int profileListScroll;
    private int profilePanelX;
    private int profilePanelY;
    private int profileListX;
    private int profileListY;
    private int profileListW;
    private int profileListH;
    private int mainPanelLeft;
    private int mainPanelTop;
    private int mainPanelRight;
    private int mainPanelBottom;
    private int leftPanelLeft;
    private int leftPanelTop;
    private int leftPanelRight;
    private int leftPanelBottom;
    private int centerPanelLeft;
    private int centerPanelTop;
    private int centerPanelRight;
    private int centerPanelBottom;
    private int rightPanelLeft;
    private int rightPanelTop;
    private int rightPanelRight;
    private int rightPanelBottom;
    private int contentPanelWidth;
    private int profilePanelWidth;
    private Integer previousGuiScaleValue;
    private boolean forcedGuiScale;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$Tab.class */
    private enum Tab {
        SUPPORT("punchy.config.tab.support"),
        BLACKLIST("punchy.config.tab.blacklist"),
        RENDERING("punchy.config.tab.rendering"),
        ANIMATION("punchy.config.tab.animation"),
        PHYSICS("punchy.config.tab.physics"),
        MODELS("punchy.config.tab.models"),
        COMPAT("punchy.config.tab.compat");

        private final String labelKey;

        Tab(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    public PunchyConfigScreen(class_437 parent) {
        super(class_2561.method_43471("punchy.config.title"));
        this.tab = Tab.SUPPORT;
        this.blacklistDraft = "";
        this.blacklistEntryButtons = new ArrayList();
        this.blacklistDualButtons = new ArrayList();
        this.blacklistRemoveButtons = new ArrayList();
        this.animationWidgets = new ArrayList();
        this.animationWidgetBaseY = new ArrayList();
        this.blacklistSearchDraft = "";
        this.blacklistCatalogEntries = new ArrayList();
        this.blacklistFilteredEntries = new ArrayList();
        this.blacklistModFilters = new ArrayList();
        this.blacklistSelectedNamespace = "";
        this.blacklistNamespaceHitboxes = new ArrayList();
        this.blacklistEnableHitboxes = new ArrayList();
        this.blacklistDualHitboxes = new ArrayList();
        this.profileRowButtons = new ArrayList();
        this.profiles = List.of();
        this.selectedProfile = -1;
        this.activeProfileName = null;
        this.profileListScroll = 0;
        this.profilePanelX = 0;
        this.profilePanelY = 0;
        this.profileListX = 0;
        this.profileListY = 0;
        this.profileListW = 0;
        this.profileListH = 0;
        this.mainPanelLeft = 0;
        this.mainPanelTop = 0;
        this.mainPanelRight = 0;
        this.mainPanelBottom = 0;
        this.leftPanelLeft = 0;
        this.leftPanelTop = 0;
        this.leftPanelRight = 0;
        this.leftPanelBottom = 0;
        this.centerPanelLeft = 0;
        this.centerPanelTop = 0;
        this.centerPanelRight = 0;
        this.centerPanelBottom = 0;
        this.rightPanelLeft = 0;
        this.rightPanelTop = 0;
        this.rightPanelRight = 0;
        this.rightPanelBottom = 0;
        this.contentPanelWidth = CONTENT_WIDTH;
        this.profilePanelWidth = PROFILE_PANEL_WIDTH;
        this.parent = parent;
        this.data = PunchyConfig.snapshot();
    }

    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (this.blacklistModalOpen && keyCode == 256) {
            closeBlacklistModal();
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    protected void method_25426() {
        applySmallScreenGuiScaleOverride();
        syncGuiScaleOverrideDimensions();
        rebuild();
    }

    private void rebuild() {
        int row;
        class_4185 tabButton;
        method_37067();
        this.blacklistInput = null;
        this.blacklistHelpLines = null;
        this.blacklistHelpHeight = 0;
        this.blacklistEntryButtons.clear();
        this.blacklistDualButtons.clear();
        this.blacklistRemoveButtons.clear();
        this.blacklistScrollOffset = 0;
        this.blacklistListStartY = 0;
        this.blacklistViewportHeight = 0;
        this.blacklistEntryHeight = 26;
        this.blacklistScrollbarReserve = 0;
        this.animationWidgets.clear();
        this.animationWidgetBaseY.clear();
        this.animationScrollOffset = 0;
        this.animationListStartY = 0;
        this.animationViewportHeight = 0;
        this.animationEntryHeight = 26;
        this.animationRowCount = 0;
        this.blacklistSearchInput = null;
        this.blacklistNamespaceHitboxes.clear();
        this.blacklistEnableHitboxes.clear();
        this.blacklistDualHitboxes.clear();
        this.blacklistModalMaxScroll = 0;
        this.blacklistNamespaceScrollOffset = 0;
        this.blacklistNamespaceMaxScroll = 0;
        this.profileRowButtons.clear();
        this.profileNameInput = null;
        this.profileSaveButton = null;
        this.profileLoadButton = null;
        this.profileDeleteButton = null;
        this.profileOpenFolderButton = null;
        this.supportDonateButton = null;
        this.profileListScroll = 0;
        if (this.tab == Tab.BLACKLIST) {
            this.tab = Tab.SUPPORT;
        }
        Tab[] tabs = Tab.values();
        int tabsHeight = ((tabs.length + BLACKLIST_MODAL_FILTER_ROWS) * (ROW_HEIGHT + ROW_SPACING)) - ROW_SPACING;
        int baseLayoutWidth = Math.min(800, Math.max(680, this.field_22789 - 48));
        int layoutWidth = Math.min(Math.max(520, (int) Math.round(((double) baseLayoutWidth) * 0.65d)), Math.max(520, this.field_22789 - SIDEBAR_GAP));
        this.profilePanelWidth = Math.max(170, (int) Math.round(143.0d));
        this.contentPanelWidth = Math.max(170, (((layoutWidth - SIDEBAR_WIDTH) - SIDEBAR_GAP) - MAIN_PANEL_GAP) - this.profilePanelWidth);
        int layoutX = Math.max(8, (this.field_22789 - layoutWidth) / BLACKLIST_MODAL_FILTER_ROWS);
        int availableHeight = Math.max(CONTENT_WIDTH, this.field_22790 - 24);
        int baseMenuHeight = Math.min(CONTENT_WIDTH, availableHeight);
        int menuHeight = Math.min(Math.max(190, (int) Math.round(((double) baseMenuHeight) * 0.6d)) + (BLACKLIST_MODAL_FILTER_ROWS * (ROW_HEIGHT + ROW_SPACING)), availableHeight);
        int menuTop = Math.max(8, (this.field_22790 - menuHeight) / BLACKLIST_MODAL_FILTER_ROWS);
        int menuBottom = menuTop + menuHeight;
        int tabY = menuTop + 8;
        int i = 0;
        while (i < tabs.length) {
            Tab current = tabs[i];
            int visualRow = i >= BLACKLIST_MODAL_FILTER_ROWS ? i + BLACKLIST_MODAL_FILTER_ROWS : i;
            if (current == Tab.BLACKLIST) {
                tabButton = class_4185.method_46430(class_2561.method_43471(current.labelKey), button -> {
                    openBlacklistModal();
                }).method_46433(layoutX, tabY + (visualRow * (ROW_HEIGHT + ROW_SPACING))).method_46437(SIDEBAR_WIDTH, ROW_HEIGHT).method_46431();
                tabButton.field_22763 = !this.blacklistModalOpen;
            } else {
                tabButton = class_4185.method_46430(class_2561.method_43471(current.labelKey), button2 -> {
                    this.tab = current;
                    rebuild();
                }).method_46433(layoutX, tabY + (visualRow * (ROW_HEIGHT + ROW_SPACING))).method_46437(SIDEBAR_WIDTH, ROW_HEIGHT).method_46431();
                tabButton.field_22763 = this.tab != current;
            }
            method_37063(tabButton);
            i++;
        }
        int handEditorY = tabY + (BLACKLIST_MODAL_FILTER_ROWS * (ROW_HEIGHT + ROW_SPACING));
        class_4185 handEditorShortcut = method_37063(class_4185.method_46430(class_2561.method_43470("Hand Editor"), button3 -> {
            if (this.field_22787 != null) {
                this.field_22787.method_1507(new TuningWorkbenchScreen(this));
            }
        }).method_46433(layoutX, handEditorY).method_46437(SIDEBAR_WIDTH, ROW_HEIGHT).method_46431());
        handEditorShortcut.field_22763 = !this.blacklistModalOpen;
        int mixPacksY = tabY + (3 * (ROW_HEIGHT + ROW_SPACING));
        class_4185 mixPacksShortcut = method_37063(class_4185.method_46430(class_2561.method_43470("Mix Packs"), button4 -> {
            if (this.field_22787 != null) {
                this.field_22787.method_1507(new MixThePacksScreen(this));
            }
        }).method_46433(layoutX, mixPacksY).method_46437(SIDEBAR_WIDTH, ROW_HEIGHT).method_46431());
        mixPacksShortcut.field_22763 = !this.blacklistModalOpen;
        int startX = layoutX + SIDEBAR_WIDTH + SIDEBAR_GAP;
        int profileX = startX + this.contentPanelWidth + MAIN_PANEL_GAP;
        int optionWidth = (this.contentPanelWidth - RESET_WIDTH) - RESET_GAP;
        this.dividerX = startX - 8;
        if ((this.dividerX & 1) == 1) {
            this.dividerX--;
        }
        this.dividerTop = tabY - 4;
        this.dividerBottom = tabY + tabsHeight + 4;
        if (this.tab == Tab.SUPPORT) {
            row = addLinkButton(addLinkButton(addLinkButton(addLinkButton(0, startX, tabY, this.contentPanelWidth, "punchy.config.support.donate", "https://ko-fi.com/modinpunchy"), startX, tabY, this.contentPanelWidth, "punchy.config.support.discord", "https://discord.gg/CXttaCyGCN"), startX, tabY, this.contentPanelWidth, "punchy.config.support.wiki", "https://github.com/punchy-mod/punchy-wiki/wiki"), startX, tabY, this.contentPanelWidth, "punchy.config.support.bug_report", "https://discord.gg/CXttaCyGCN");
        } else if (this.tab == Tab.BLACKLIST) {
            int inputWidth = (this.contentPanelWidth - 50) - RESET_GAP;
            String helpText = class_2561.method_43471("punchy.config.blacklist.examples").getString();
            this.blacklistHelpX = startX;
            this.blacklistHelpY = tabY + rowOffset(0);
            this.blacklistHelpWidth = this.contentPanelWidth;
            Objects.requireNonNull(this.field_22793);
            int helpLineHeight = (int) Math.ceil(9.0f * 1.0f);
            int maxChars = Math.max(RESET_GAP, (int) (this.blacklistHelpWidth / (this.field_22793.method_1727("abcdefghijklmnopqrstuvwxyz") / 26.0f)));
            this.blacklistHelpLines = splitHelpText(helpText, maxChars);
            this.blacklistHelpHeight = (this.blacklistHelpLines.size() * helpLineHeight) + 4;
            int baseY = tabY + this.blacklistHelpHeight;
            this.blacklistInput = new class_342(this.field_22793, startX, baseY + rowOffset(0), inputWidth, ROW_HEIGHT, class_2561.method_43471("punchy.config.blacklist.input"));
            this.blacklistInput.method_1852(this.blacklistDraft);
            method_37063(this.blacklistInput);
            method_37063(class_4185.method_46430(class_2561.method_43471("punchy.config.blacklist.add"), button5 -> {
                handleAddBlacklist();
            }).method_46433(startX + inputWidth + RESET_GAP, baseY + rowOffset(0)).method_46437(50, ROW_HEIGHT).method_46431());
            row = 0 + 1;
            if (this.data.itemBlacklist != null) {
                int visibleRows = this.data.itemBlacklist.size() >= 5 ? 4 : 5;
                this.blacklistScrollbarReserve = this.data.itemBlacklist.size() >= 5 ? 7 : 0;
                this.blacklistListStartY = baseY + rowOffset(row);
                this.blacklistViewportHeight = (visibleRows * this.blacklistEntryHeight) - ROW_SPACING;
                for (String entry : this.data.itemBlacklist) {
                    if (entry != null && !entry.isBlank()) {
                        int labelWidth = (((this.contentPanelWidth - 66) - ROW_HEIGHT) - ROW_HEIGHT) - this.blacklistScrollbarReserve;
                        class_4185 labelButton = class_4185.method_46430(class_2561.method_43470(entry), b -> {
                        }).method_46433(startX, this.blacklistListStartY + (this.blacklistEntryButtons.size() * this.blacklistEntryHeight)).method_46437(labelWidth, ROW_HEIGHT).method_46431();
                        labelButton.field_22763 = false;
                        method_37063(labelButton);
                        class_4185 dualButton = class_4185.method_46430(blacklistDualLabel(isBlacklistDualHanded(entry)), button6 -> {
                            setBlacklistDualHanded(entry, !isBlacklistDualHanded(entry));
                            rebuild();
                        }).method_46433(startX + labelWidth + RESET_GAP, this.blacklistListStartY + (this.blacklistDualButtons.size() * this.blacklistEntryHeight)).method_46437(66, ROW_HEIGHT).method_46431();
                        method_37063(dualButton);
                        class_4185 removeButton = class_4185.method_46430(class_2561.method_43470("X"), button7 -> {
                            this.data.itemBlacklist.remove(entry);
                            if (this.data.blacklistApplyDualHanded != null) {
                                this.data.blacklistApplyDualHanded.remove(entry);
                            }
                            rebuild();
                        }).method_46433(startX + labelWidth + RESET_GAP + 66 + RESET_GAP, this.blacklistListStartY + (this.blacklistRemoveButtons.size() * this.blacklistEntryHeight)).method_46437(ROW_HEIGHT, ROW_HEIGHT).method_46431();
                        method_37063(removeButton);
                        this.blacklistEntryButtons.add(labelButton);
                        this.blacklistDualButtons.add(dualButton);
                        this.blacklistRemoveButtons.add(removeButton);
                        row++;
                    }
                }
                updateBlacklistScroll();
            }
        } else if (this.tab == Tab.RENDERING) {
            row = addToggleWithReset(addToggleWithReset(addToggleWithReset(0, startX, tabY, optionWidth, "punchy.config.option.render_armor_arms_fp", () -> {
                return this.data.renderArmorArmsFP;
            }, value -> {
                this.data.renderArmorArmsFP = value.booleanValue();
            }, false), startX, tabY, optionWidth, "punchy.config.option.enable_enhanced_fire_arm_effects", () -> {
                return !this.data.disableEnhancedFireArmEffects;
            }, value2 -> {
                this.data.disableEnhancedFireArmEffects = !value2.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_blur_glow_at_dark", () -> {
                return this.data.enableBlurGlowAtDark;
            }, value3 -> {
                this.data.enableBlurGlowAtDark = value3.booleanValue();
            }, true);
        } else if (this.tab == Tab.ANIMATION) {
            this.animationListStartY = tabY;
            this.animationViewportHeight = Math.min(Math.max(0, ((menuBottom - (24 + 8)) - MAIN_PANEL_GAP) - this.animationListStartY), (7 * this.animationEntryHeight) - ROW_SPACING);
            int row2 = addToggleWithResetScrollable(addToggleWithResetScrollable(0, startX, tabY, optionWidth, "punchy.config.option.enable_particles", () -> {
                return this.data.enableParticles;
            }, value4 -> {
                this.data.enableParticles = value4.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_model_parts_glow", () -> {
                return this.data.enableModelPartsGlow;
            }, value5 -> {
                this.data.enableModelPartsGlow = value5.booleanValue();
            }, true);
            int row3 = row2 + 1;
            AnimationSpeedSlider slider = new AnimationSpeedSlider(startX, tabY + rowOffset(row2), optionWidth, ROW_HEIGHT, this.data);
            registerAnimationWidget(slider, slider.method_46427());
            registerAnimationWidget(class_4185.method_46430(class_2561.method_43471("punchy.config.reset.option"), button8 -> {
                this.data.animationSpeed = 5.5f;
                slider.setSpeed(this.data.animationSpeed);
                PunchyAnimationManager.setAnimationSpeed(this.data.animationSpeed);
            }).method_46433(startX + optionWidth + RESET_GAP, tabY + rowOffset(row3 - 1)).method_46437(RESET_WIDTH, ROW_HEIGHT).method_46431(), tabY + rowOffset(row3 - 1));
            int row4 = addToggleWithResetScrollable(addToggleWithResetScrollable(addToggleWithResetScrollable(addToggleWithResetScrollable(row3, startX, tabY, optionWidth, "punchy.config.option.enable_boat_fp_animations", () -> {
                return !this.data.disableBoatFirstPersonAnimations;
            }, value6 -> {
                this.data.disableBoatFirstPersonAnimations = !value6.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_custom_walk", () -> {
                return this.data.enableCustomWalk;
            }, value7 -> {
                this.data.enableCustomWalk = value7.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_sprint_arm_swing", () -> {
                return this.data.enableSprintArmSwing;
            }, value8 -> {
                this.data.enableSprintArmSwing = value8.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_fall_arm_animation", () -> {
                return this.data.enableFallArmAnimation;
            }, value9 -> {
                this.data.enableFallArmAnimation = value9.booleanValue();
            }, true);
            int row5 = row4 + 1;
            SprintArmSwingIntensitySlider sprintSlider = new SprintArmSwingIntensitySlider(startX, tabY + rowOffset(row4), optionWidth, ROW_HEIGHT, this.data);
            registerAnimationWidget(sprintSlider, sprintSlider.method_46427());
            registerAnimationWidget(class_4185.method_46430(class_2561.method_43471("punchy.config.reset.option"), button9 -> {
                this.data.sprintArmSwingIntensity = 1.0f;
                sprintSlider.setIntensity(this.data.sprintArmSwingIntensity);
            }).method_46433(startX + optionWidth + RESET_GAP, tabY + rowOffset(row5 - 1)).method_46437(RESET_WIDTH, ROW_HEIGHT).method_46431(), tabY + rowOffset(row5 - 1));
            int row6 = row5 + 1;
            SprintArmSwingSpeedSlider sprintSpeedSlider = new SprintArmSwingSpeedSlider(startX, tabY + rowOffset(row5), optionWidth, ROW_HEIGHT, this.data);
            registerAnimationWidget(sprintSpeedSlider, sprintSpeedSlider.method_46427());
            registerAnimationWidget(class_4185.method_46430(class_2561.method_43471("punchy.config.reset.option"), button10 -> {
                this.data.sprintArmSwingSpeed = 0.0f;
                sprintSpeedSlider.setSpeed(this.data.sprintArmSwingSpeed);
            }).method_46433(startX + optionWidth + RESET_GAP, tabY + rowOffset(row6 - 1)).method_46437(RESET_WIDTH, ROW_HEIGHT).method_46431(), tabY + rowOffset(row6 - 1));
            row = addToggleWithResetScrollable(addToggleWithResetScrollable(addToggleWithResetScrollable(addToggleWithResetScrollable(addToggleWithResetScrollable(addToggleWithResetScrollable(addToggleWithResetScrollable(addToggleWithResetScrollable(row6, startX, tabY, optionWidth, "punchy.config.option.enable_crawl_animation", () -> {
                return this.data.enableCrawlAnimation;
            }, value10 -> {
                this.data.enableCrawlAnimation = value10.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_climb_animation", () -> {
                return this.data.enableClimbAnimation;
            }, value11 -> {
                this.data.enableClimbAnimation = value11.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_swim_animation", () -> {
                return this.data.enableSwimAnimation;
            }, value12 -> {
                this.data.enableSwimAnimation = value12.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_elytra_flight_animation", () -> {
                return this.data.enableElytraFlightAnimation;
            }, value13 -> {
                this.data.enableElytraFlightAnimation = value13.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.freeze_shake", () -> {
                return this.data.enableFreezeShake;
            }, value14 -> {
                this.data.enableFreezeShake = value14.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.freeze_arm_overlay", () -> {
                return this.data.enableFreezeArmOverlay;
            }, value15 -> {
                this.data.enableFreezeArmOverlay = value15.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_mud", () -> {
                return this.data.enableMud;
            }, value16 -> {
                this.data.enableMud = value16.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_burn", () -> {
                return this.data.enableBurn;
            }, value17 -> {
                this.data.enableBurn = value17.booleanValue();
            }, true);
            this.animationRowCount = row;
            updateAnimationScroll();
        } else if (this.tab == Tab.PHYSICS) {
            row = addToggleWithReset(addToggleWithReset(0, startX, tabY, optionWidth, "punchy.config.option.enable_arm_physics", () -> {
                return !this.data.disableArmPhysics;
            }, value18 -> {
                this.data.disableArmPhysics = !value18.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_native_item_physics", () -> {
                return !this.data.disableNativeItemPhysics;
            }, value19 -> {
                this.data.disableNativeItemPhysics = !value19.booleanValue();
            }, true);
        } else if (this.tab == Tab.MODELS) {
            row = addToggleWithReset(addToggleWithReset(addToggleWithReset(addToggleWithReset(0, startX, tabY, optionWidth, "punchy.config.option.enable_boat_minecart_models", () -> {
                return !this.data.disableBoatMinecartRaftModels;
            }, value20 -> {
                this.data.disableBoatMinecartRaftModels = !value20.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_piston_models", () -> {
                return !this.data.disablePistonModels;
            }, value21 -> {
                this.data.disablePistonModels = !value21.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_chest_models", () -> {
                return !this.data.disableChestModels;
            }, value22 -> {
                this.data.disableChestModels = !value22.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_enchanting_table_models", () -> {
                return !this.data.disableEnchantingTableModels;
            }, value23 -> {
                this.data.disableEnchantingTableModels = !value23.booleanValue();
            }, true);
        } else {
            int row7 = addToggleWithReset(addInteractiveStuffCompatToggle(0, startX, tabY, optionWidth), startX, tabY, optionWidth, "punchy.config.option.firstperson_hide_enabled", () -> {
                return this.data.firstPersonModelHideEnabled;
            }, value24 -> {
                this.data.firstPersonModelHideEnabled = value24.booleanValue();
            }, true);
            int pitchRowY = tabY + rowOffset(row7);
            FirstPersonHidePitchSlider pitchSlider = new FirstPersonHidePitchSlider(startX, pitchRowY, optionWidth, ROW_HEIGHT, this.data);
            registerAnimationWidget(pitchSlider, pitchRowY);
            registerAnimationWidget(class_4185.method_46430(class_2561.method_43471("punchy.config.reset.option"), button11 -> {
                this.data.firstPersonModelHidePitch = 60.0f;
                pitchSlider.setPitch(this.data.firstPersonModelHidePitch);
            }).method_46433(startX + optionWidth + RESET_GAP, pitchRowY).method_46437(RESET_WIDTH, ROW_HEIGHT).method_46431(), pitchRowY);
            row = addToggleWithReset(addToggleWithReset(addToggleWithReset(addToggleWithReset(row7 + 1, startX, tabY, optionWidth, "punchy.config.option.enable_mod", () -> {
                return this.data.enableMod;
            }, value25 -> {
                this.data.enableMod = value25.booleanValue();
            }, true), startX, tabY, optionWidth, "punchy.config.option.enable_tuning", () -> {
                return this.data.enableTuning;
            }, value26 -> {
                this.data.enableTuning = value26.booleanValue();
            }, false), startX, tabY, optionWidth, "punchy.config.option.bettercombat_compat", () -> {
                return this.data.bettercombatCompat;
            }, value27 -> {
                this.data.bettercombatCompat = value27.booleanValue();
            }, false), startX, tabY, optionWidth, "punchy.config.option.swordblocking_compat", () -> {
                return this.data.swordBlockingCompat;
            }, value28 -> {
                this.data.swordBlockingCompat = value28.booleanValue();
            }, false);
        }
        int doneY = menuBottom - (ROW_HEIGHT + 8);
        configureCenterScroll(row, tabY, doneY);
        initProfilesPanel(profileX, tabY, doneY);
        refreshProfiles(true);
        updateProfileRowButtons();
        updateMainPanelBounds(layoutX, menuTop, layoutWidth, menuBottom);
        if (this.blacklistModalOpen) {
            suppressBaseWidgetsForBlacklistModal();
            initBlacklistModalWidgets();
        }
    }

    private /* synthetic */ boolean lambda$rebuild$43() {
        return this.data.enableSweat;
    }

    private /* synthetic */ void lambda$rebuild$44(Boolean value) {
        this.data.enableSweat = value.booleanValue();
    }

    private void applySmallScreenGuiScaleOverride() {
        if (this.forcedGuiScale || this.field_22787 == null || this.field_22787.field_1690 == null || !shouldForceSmallScreenGuiScale()) {
            return;
        }
        this.previousGuiScaleValue = (Integer) this.field_22787.field_1690.method_42474().method_41753();
        this.field_22787.method_22683().method_15997(1.0d);
        this.forcedGuiScale = true;
    }

    private void syncGuiScaleOverrideDimensions() {
        if (!this.forcedGuiScale || this.field_22787 == null || this.field_22787.method_22683() == null) {
            return;
        }
        this.field_22789 = this.field_22787.method_22683().method_4486();
        this.field_22790 = this.field_22787.method_22683().method_4502();
    }

    private void restoreGuiScaleOverride() {
        int iMethod_4476;
        if (!this.forcedGuiScale || this.field_22787 == null || this.field_22787.field_1690 == null || this.previousGuiScaleValue == null) {
            this.forcedGuiScale = false;
            this.previousGuiScaleValue = null;
            return;
        }
        this.field_22787.field_1690.method_42474().method_41748(this.previousGuiScaleValue);
        if (this.previousGuiScaleValue.intValue() > 0) {
            iMethod_4476 = this.previousGuiScaleValue.intValue();
        } else {
            iMethod_4476 = this.field_22787.method_22683().method_4476(this.previousGuiScaleValue.intValue(), false);
        }
        int restoredScale = iMethod_4476;
        if (restoredScale < 1) {
            restoredScale = 1;
        }
        this.field_22787.method_22683().method_15997(restoredScale);
        this.forcedGuiScale = false;
        this.previousGuiScaleValue = null;
    }

    private boolean shouldForceSmallScreenGuiScale() {
        if (this.field_22787 == null || this.field_22787.method_22683() == null) {
            return false;
        }
        int screenWidth = this.field_22787.method_22683().method_4480();
        int screenHeight = this.field_22787.method_22683().method_4507();
        return screenWidth < 1280 || screenHeight < 720;
    }

    private int rowOffset(int row) {
        return row * 26;
    }

    private <T extends class_339> T registerAnimationWidget(T widget, int baseY) {
        this.animationWidgets.add(widget);
        this.animationWidgetBaseY.add(Integer.valueOf(baseY));
        widget.method_46419(baseY - this.animationScrollOffset);
        method_37063(widget);
        return widget;
    }

    private void configureCenterScroll(int rowCount, int startY, int doneY) {
        this.animationListStartY = startY;
        int alignedBottom = this.centerPanelBottom - 8;
        this.animationViewportHeight = Math.max(0, alignedBottom - this.animationListStartY);
        this.animationRowCount = Math.max(0, rowCount);
        int totalHeight = (this.animationRowCount * this.animationEntryHeight) - ROW_SPACING;
        int maxScroll = Math.max(0, totalHeight - this.animationViewportHeight);
        this.animationScrollOffset = clamp(this.animationScrollOffset, 0, maxScroll);
        updateAnimationScroll();
    }

    private void initProfilesPanel(int x, int startY, int doneY) {
        this.profilePanelX = x;
        this.profilePanelY = startY;
        this.profileListX = x;
        this.profileListW = this.profilePanelWidth;
        this.profileListY = startY;
        int controlsBlockHeight = 80 + (BLACKLIST_MODAL_FILTER_ROWS * 3);
        int listBottomMax = (((doneY - ROW_SPACING) - ROW_HEIGHT) - ROW_SPACING) - controlsBlockHeight;
        int availableForList = Math.max(BLACKLIST_MODAL_FILTER_ROWS * 14, listBottomMax - this.profileListY);
        int listRows = class_3532.method_15340(availableForList / 14, BLACKLIST_MODAL_FILTER_ROWS, ROW_SPACING);
        this.profileListH = listRows * 14;
        int nameY = this.profileListY + this.profileListH + ROW_SPACING;
        int actionY = nameY + ROW_HEIGHT + BLACKLIST_MODAL_FILTER_ROWS;
        int deleteY = actionY + ROW_HEIGHT + BLACKLIST_MODAL_FILTER_ROWS;
        int openFolderY = deleteY + ROW_HEIGHT + BLACKLIST_MODAL_FILTER_ROWS;
        for (int i = 0; i < listRows; i++) {
            int row = i;
            class_4185 rowButton = method_37063(class_4185.method_46430(class_2561.method_43473(), b -> {
                selectProfileAtVisibleRow(row);
            }).method_46433(this.profileListX, this.profileListY + (i * 14)).method_46437(this.profileListW, 14 - 1).method_46431());
            this.profileRowButtons.add(rowButton);
        }
        this.profileNameInput = method_37063(new class_342(this.field_22793, x, nameY, this.profilePanelWidth, ROW_HEIGHT, class_2561.method_43471("punchy.tuning.ui.profile_name")));
        this.profileNameInput.method_1880(64);
        int halfW = (this.profilePanelWidth - ROW_SPACING) / BLACKLIST_MODAL_FILTER_ROWS;
        this.profileSaveButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.save_profile"), b2 -> {
            String raw = this.profileNameInput.method_1882();
            String safe = ConfigProfileManager.sanitizeProfileName(raw);
            if (safe.isBlank()) {
                safe = "config_profile";
                this.profileNameInput.method_1852(safe);
            }
            boolean ok = ConfigProfileManager.saveCurrentAs(safe, this.data);
            if (ok) {
                ConfigProfileManager.setActiveProfileName(safe);
                this.activeProfileName = safe;
                refreshProfiles(false);
                selectProfileByName(safe);
                updateProfileRowButtons();
            }
        }).method_46433(x, actionY).method_46437(halfW, ROW_HEIGHT).method_46431());
        this.profileLoadButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.load_profile"), b3 -> {
            String name = selectedProfileName();
            PunchyConfig.Data loaded = name == null ? null : ConfigProfileManager.load(name);
            if (loaded != null) {
                ConfigProfileManager.setActiveProfileName(name);
                this.activeProfileName = name;
                applyProfileDataIntoCurrent(loaded);
                rebuild();
            }
        }).method_46433(x + halfW + ROW_SPACING, actionY).method_46437((this.profilePanelWidth - halfW) - ROW_SPACING, ROW_HEIGHT).method_46431());
        this.profileDeleteButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.delete_profile"), b4 -> {
            openDeleteProfileConfirm();
        }).method_46433(x, deleteY).method_46437(this.profilePanelWidth, ROW_HEIGHT).method_46431());
        int openFolderWidth = this.profilePanelWidth;
        this.profileOpenFolderButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.open_profiles_folder"), b5 -> {
            openConfigProfilesFolder();
        }).method_46433(x, openFolderY).method_46437(openFolderWidth, ROW_HEIGHT).method_46431());
        int bottomButtonWidth = (this.profilePanelWidth - ROW_SPACING) / BLACKLIST_MODAL_FILTER_ROWS;
        method_37063(class_4185.method_46430(class_2561.method_43471("punchy.config.reset"), button -> {
            resetAll();
            rebuild();
        }).method_46433(x, doneY).method_46437(bottomButtonWidth, ROW_HEIGHT).method_46431());
        method_37063(class_4185.method_46430(class_2561.method_43471("gui.done"), button2 -> {
            saveAndClose();
        }).method_46433(x + bottomButtonWidth + ROW_SPACING, doneY).method_46437((this.profilePanelWidth - bottomButtonWidth) - ROW_SPACING, ROW_HEIGHT).method_46431());
    }

    private void updateMainPanelBounds(int layoutX, int menuTop, int layoutWidth, int menuBottom) {
        this.mainPanelLeft = Math.max(4, layoutX - RESET_GAP);
        this.mainPanelRight = Math.min(this.field_22789 - 4, layoutX + layoutWidth + RESET_GAP);
        this.mainPanelTop = Math.max(4, menuTop - 8);
        this.mainPanelBottom = Math.min(this.field_22790 - 4, menuBottom + 8);
        this.leftPanelLeft = Math.max(4, layoutX - 4);
        this.leftPanelTop = Math.max(4, menuTop);
        this.leftPanelRight = Math.min(this.field_22789 - 4, layoutX + SIDEBAR_WIDTH + 4);
        this.leftPanelBottom = Math.min(this.field_22790 - 4, menuBottom);
        int contentLeft = layoutX + SIDEBAR_WIDTH + SIDEBAR_GAP;
        this.centerPanelLeft = Math.max(4, contentLeft - 4);
        this.centerPanelTop = Math.max(4, menuTop);
        this.centerPanelRight = Math.min(this.field_22789 - 4, contentLeft + this.contentPanelWidth + 4);
        this.centerPanelBottom = Math.min(this.field_22790 - 4, menuBottom);
        this.rightPanelLeft = Math.max(4, this.profilePanelX - 4);
        this.rightPanelTop = Math.max(4, menuTop);
        this.rightPanelRight = Math.min(this.field_22789 - 4, this.profilePanelX + this.profilePanelWidth + 4);
        this.rightPanelBottom = Math.min(this.field_22790 - 4, menuBottom);
    }

    private void refreshProfiles(boolean clampSelection) {
        this.profiles = ConfigProfileManager.listProfiles();
        this.activeProfileName = ConfigProfileManager.getActiveProfileName();
        if (this.activeProfileName != null && !this.profiles.contains(this.activeProfileName)) {
            this.activeProfileName = null;
            ConfigProfileManager.setActiveProfileName(null);
        }
        int maxScroll = Math.max(0, this.profiles.size() - this.profileRowButtons.size());
        this.profileListScroll = class_3532.method_15340(this.profileListScroll, 0, maxScroll);
        if (this.profiles.isEmpty()) {
            this.selectedProfile = -1;
            if (this.profileNameInput != null) {
                this.profileNameInput.method_1852("");
                return;
            }
            return;
        }
        if (this.selectedProfile < 0 || this.selectedProfile >= this.profiles.size() || clampSelection) {
            if (this.activeProfileName != null) {
                this.selectedProfile = this.profiles.indexOf(this.activeProfileName);
            } else {
                this.selectedProfile = clampSelection ? -1 : 0;
            }
            this.selectedProfile = class_3532.method_15340(this.selectedProfile, -1, this.profiles.size() - 1);
        }
        ensureSelectedVisibleInProfileList();
        String selected = selectedProfileName();
        if (this.profileNameInput != null) {
            this.profileNameInput.method_1852(selected == null ? "" : selected);
        }
    }

    private void updateProfileRowButtons() {
        for (int i = 0; i < this.profileRowButtons.size(); i++) {
            class_4185 rowButton = this.profileRowButtons.get(i);
            int index = this.profileListScroll + i;
            if (index >= 0 && index < this.profiles.size()) {
                String name = this.profiles.get(index);
                boolean selected = name != null && name.equals(this.activeProfileName);
                rowButton.field_22764 = true;
                rowButton.field_22763 = true;
                rowButton.method_25355(class_2561.method_43470((selected ? "> " : "") + name));
            } else {
                rowButton.field_22764 = true;
                rowButton.field_22763 = false;
                rowButton.method_25355(class_2561.method_43473());
            }
        }
    }

    private void selectProfileAtVisibleRow(int row) {
        int index = this.profileListScroll + row;
        if (index < 0 || index >= this.profiles.size()) {
            return;
        }
        this.selectedProfile = index;
        String selectedName = selectedProfileName();
        if (selectedName != null && this.profileNameInput != null) {
            this.profileNameInput.method_1852(selectedName);
        }
        ensureSelectedVisibleInProfileList();
        updateProfileRowButtons();
    }

    private void ensureSelectedVisibleInProfileList() {
        if (this.selectedProfile < 0) {
            return;
        }
        int visible = this.profileRowButtons.size();
        if (this.selectedProfile < this.profileListScroll) {
            this.profileListScroll = this.selectedProfile;
        } else if (this.selectedProfile >= this.profileListScroll + visible) {
            this.profileListScroll = (this.selectedProfile - visible) + 1;
        }
        this.profileListScroll = class_3532.method_15340(this.profileListScroll, 0, Math.max(0, this.profiles.size() - visible));
    }

    private String selectedProfileName() {
        if (this.selectedProfile < 0 || this.selectedProfile >= this.profiles.size()) {
            return null;
        }
        return this.profiles.get(this.selectedProfile);
    }

    private void selectProfileByName(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return;
        }
        for (int i = 0; i < this.profiles.size(); i++) {
            if (profileName.equals(this.profiles.get(i))) {
                this.selectedProfile = i;
                ensureSelectedVisibleInProfileList();
                if (this.profileNameInput != null) {
                    this.profileNameInput.method_1852(profileName);
                    return;
                }
                return;
            }
        }
    }

    private void openDeleteProfileConfirm() {
        String name = selectedProfileName();
        class_310 client = this.field_22787;
        if (name == null || client == null) {
            return;
        }
        client.method_1507(new class_410(confirmed -> {
            if (confirmed && ConfigProfileManager.deleteProfile(name)) {
                refreshProfiles(true);
                updateProfileRowButtons();
            }
            client.method_1507(this);
        }, class_2561.method_43471("punchy.tuning.ui.confirm_delete_profile.title"), class_2561.method_43469("punchy.tuning.ui.confirm_delete_profile.message", new Object[]{name}), class_2561.method_43471("punchy.tuning.ui.confirm_delete_profile.confirm"), class_2561.method_43471("gui.cancel")));
    }

    private boolean isMouseOverProfileList(double x, double y) {
        return x >= ((double) this.profileListX) && x < ((double) (this.profileListX + this.profileListW)) && y >= ((double) this.profileListY) && y < ((double) (this.profileListY + this.profileListH));
    }

    private void applyProfileDataIntoCurrent(PunchyConfig.Data source) {
        LinkedHashMap linkedHashMap;
        if (source == null) {
            return;
        }
        this.data.renderArmorArmsFP = source.renderArmorArmsFP;
        this.data.interactiveStuffCompat = source.interactiveStuffCompat;
        this.data.bettercombatCompat = source.bettercombatCompat;
        this.data.swordBlockingCompat = source.swordBlockingCompat;
        this.data.animationSpeed = source.animationSpeed;
        this.data.enableMod = source.enableMod;
        this.data.enableTuning = source.enableTuning;
        this.data.disableResourcePackModelParts = source.disableResourcePackModelParts;
        this.data.disableArmPhysics = source.disableArmPhysics;
        this.data.disableNativeItemPhysics = source.disableNativeItemPhysics;
        this.data.disableBoatMinecartRaftModels = source.disableBoatMinecartRaftModels;
        this.data.disablePistonModels = source.disablePistonModels;
        this.data.disableChestModels = source.disableChestModels;
        this.data.disableEnchantingTableModels = source.disableEnchantingTableModels;
        this.data.disableBoatFirstPersonAnimations = source.disableBoatFirstPersonAnimations;
        this.data.disableEnhancedFireArmEffects = source.disableEnhancedFireArmEffects;
        this.data.enableBlurGlowAtDark = source.enableBlurGlowAtDark;
        this.data.enableCrawlAnimation = source.enableCrawlAnimation;
        this.data.enableClimbAnimation = source.enableClimbAnimation;
        this.data.enableSwimAnimation = source.enableSwimAnimation;
        this.data.enableElytraFlightAnimation = source.enableElytraFlightAnimation;
        this.data.enableCustomWalk = source.enableCustomWalk;
        this.data.enableSprintArmSwing = source.enableSprintArmSwing;
        this.data.enableFallArmAnimation = source.enableFallArmAnimation;
        this.data.sprintArmSwingIntensity = source.sprintArmSwingIntensity;
        this.data.sprintArmSwingSpeed = source.sprintArmSwingSpeed;
        this.data.enableFreezeShake = source.enableFreezeShake;
        this.data.enableFreezeArmOverlay = source.enableFreezeArmOverlay;
        this.data.enableMud = source.enableMud;
        this.data.enableSweat = source.enableSweat;
        this.data.enableBurn = source.enableBurn;
        this.data.enableParticles = source.enableParticles;
        this.data.enableModelPartsGlow = source.enableModelPartsGlow;
        this.data.firstPersonModelHideEnabled = source.firstPersonModelHideEnabled;
        this.data.firstPersonModelHidePitch = source.firstPersonModelHidePitch;
        this.data.itemBlacklist = source.itemBlacklist == null ? new ArrayList() : new ArrayList(source.itemBlacklist);
        PunchyConfig.Data data = this.data;
        if (source.blacklistApplyDualHanded == null) {
            linkedHashMap = new LinkedHashMap();
        } else {
            linkedHashMap = new LinkedHashMap(source.blacklistApplyDualHanded);
        }
        data.blacklistApplyDualHanded = linkedHashMap;
        PunchyAnimationManager.setAnimationSpeed(this.data.animationSpeed);
    }

    private void openConfigProfilesFolder() {
        try {
            Path dir = ConfigProfileManager.profilesDirectoryPath();
            Files.createDirectories(dir, new FileAttribute[0]);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir.toFile());
            }
        } catch (Exception e) {
        }
    }

    private void suppressBaseWidgetsForBlacklistModal() {
        for (class_339 class_339Var : method_25396()) {
            if (class_339Var instanceof class_339) {
                class_339 widget = class_339Var;
                widget.field_22763 = false;
                widget.field_22764 = false;
            }
        }
    }

    private int addToggleWithResetScrollable(int row, int startX, int startY, int optionWidth, String labelKey, BooleanSupplier getter, Consumer<Boolean> setter, boolean resetValue) {
        int y = startY + rowOffset(row);
        class_4185 toggle = createToggleButton(startX, y, optionWidth, labelKey, getter, setter);
        registerAnimationWidget(toggle, y);
        registerAnimationWidget(class_4185.method_46430(class_2561.method_43471("punchy.config.reset.option"), button -> {
            setter.accept(Boolean.valueOf(resetValue));
            toggle.method_25355(toggleLabel(labelKey, resetValue));
        }).method_46433(startX + optionWidth + RESET_GAP, y).method_46437(RESET_WIDTH, ROW_HEIGHT).method_46431(), y);
        return row + 1;
    }

    private void handleAddBlacklist() {
        String raw;
        if (this.blacklistInput == null || (raw = this.blacklistInput.method_1882()) == null) {
            return;
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.endsWith(":")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.indexOf(58) >= 0) {
            if (!isValidNamespacedBlacklistPattern(trimmed)) {
                return;
            }
        } else if (!isValidNamespace(trimmed)) {
            return;
        }
        if (this.data.itemBlacklist == null) {
            this.data.itemBlacklist = new ArrayList();
        }
        if (this.data.blacklistApplyDualHanded == null) {
            this.data.blacklistApplyDualHanded = new LinkedHashMap();
        }
        if (!this.data.itemBlacklist.contains(trimmed)) {
            this.data.itemBlacklist.add(trimmed);
        }
        this.data.blacklistApplyDualHanded.putIfAbsent(trimmed, false);
        this.blacklistDraft = "";
        this.blacklistInput.method_1852("");
        rebuild();
    }

    private static boolean isValidNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return false;
        }
        for (int i = 0; i < namespace.length(); i++) {
            char c = namespace.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidNamespacedBlacklistPattern(String value) {
        int firstColon;
        if (value == null || value.isBlank() || (firstColon = value.indexOf(58)) <= 0 || firstColon != value.lastIndexOf(58)) {
            return false;
        }
        String namespace = value.substring(0, firstColon);
        String path = value.substring(firstColon + 1);
        if (!isValidNamespace(namespace) || path.isBlank()) {
            return false;
        }
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/' || c == '*' || c == '?';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private void openBlacklistModal() {
        this.blacklistModalOpen = true;
        ensureBlacklistCatalog();
        refreshBlacklistFilteredEntries();
        this.blacklistModalScrollOffset = 0;
        this.blacklistNamespaceScrollOffset = 0;
        rebuild();
    }

    private void closeBlacklistModal() {
        if (!this.blacklistModalOpen) {
            return;
        }
        this.blacklistModalOpen = false;
        rebuild();
    }

    private void initBlacklistModalWidgets() {
        ensureBlacklistCatalog();
        computeBlacklistModalLayout();
        int searchX = this.blacklistModalLeft + 14;
        int searchWidth = Math.max(140, (this.blacklistModalRight - 14) - searchX);
        int searchY = this.blacklistModalTop + 30;
        this.blacklistSearchInput = new class_342(this.field_22793, searchX, searchY, searchWidth, ROW_HEIGHT, class_2561.method_43471("punchy.config.blacklist.search"));
        this.blacklistSearchInput.method_1852(this.blacklistSearchDraft);
        this.blacklistSearchInput.method_47404(class_2561.method_43471("punchy.config.blacklist.search"));
        this.blacklistSearchInput.method_1863(value -> {
            this.blacklistSearchDraft = value == null ? "" : value;
            this.blacklistModalScrollOffset = 0;
            refreshBlacklistFilteredEntries();
        });
        method_37063(this.blacklistSearchInput);
    }

    private void ensureBlacklistCatalog() {
        class_2960 id;
        if (!this.blacklistCatalogEntries.isEmpty()) {
            return;
        }
        Set<String> namespaces = new LinkedHashSet<>();
        for (class_1792 item : class_7923.field_41178) {
            if (item != class_1802.field_8162 && (id = class_7923.field_41178.method_10221(item)) != null) {
                class_1799 stack = class_1799.field_8037;
                String displayName = id.toString();
                try {
                    stack = new class_1799(item);
                    String resolved = stack.method_7964().getString();
                    if (resolved != null && !resolved.isBlank()) {
                        displayName = resolved;
                    }
                } catch (Throwable th) {
                }
                String key = id.toString().toLowerCase(Locale.ROOT);
                String namespace = id.method_12836().toLowerCase(Locale.ROOT);
                this.blacklistCatalogEntries.add(new BlacklistCatalogEntry(key, namespace, displayName, displayName.toLowerCase(Locale.ROOT), stack));
                namespaces.add(namespace);
            }
        }
        this.blacklistCatalogEntries.sort((left, right) -> {
            int byName = left.displayName.compareToIgnoreCase(right.displayName);
            if (byName != 0) {
                return byName;
            }
            return left.id.compareTo(right.id);
        });
        for (String namespace2 : namespaces) {
            this.blacklistModFilters.add(new BlacklistModFilter(namespace2, resolveModDisplayName(namespace2)));
        }
        this.blacklistModFilters.sort((left2, right2) -> {
            return left2.displayName.compareToIgnoreCase(right2.displayName);
        });
    }

    private void refreshBlacklistFilteredEntries() {
        this.blacklistFilteredEntries.clear();
        String query = this.blacklistSearchDraft == null ? "" : this.blacklistSearchDraft.trim().toLowerCase(Locale.ROOT);
        for (BlacklistCatalogEntry entry : this.blacklistCatalogEntries) {
            if (this.blacklistSelectedNamespace.isBlank() || this.blacklistSelectedNamespace.equals(entry.namespace)) {
                if (query.isEmpty() || entry.id.contains(query) || entry.searchName.contains(query)) {
                    this.blacklistFilteredEntries.add(entry);
                }
            }
        }
        this.blacklistFilteredEntries.sort((left, right) -> {
            boolean leftEnabled = isBlacklistEnabled(left.id);
            boolean rightEnabled = isBlacklistEnabled(right.id);
            if (leftEnabled != rightEnabled) {
                return leftEnabled ? -1 : 1;
            }
            int byName = left.displayName.compareToIgnoreCase(right.displayName);
            if (byName != 0) {
                return byName;
            }
            return left.id.compareTo(right.id);
        });
        this.blacklistModalScrollOffset = clamp(this.blacklistModalScrollOffset, 0, Math.max(0, this.blacklistModalMaxScroll));
    }

    private void computeBlacklistModalLayout() {
        int modalWidth = Math.min(620, Math.max(460, this.field_22789 - 80));
        int modalHeight = Math.min(302, Math.max(202, this.field_22790 - 96));
        this.blacklistModalLeft = (this.field_22789 - modalWidth) / BLACKLIST_MODAL_FILTER_ROWS;
        this.blacklistModalTop = (this.field_22790 - modalHeight) / BLACKLIST_MODAL_FILTER_ROWS;
        this.blacklistModalRight = this.blacklistModalLeft + modalWidth;
        this.blacklistModalBottom = this.blacklistModalTop + modalHeight;
        this.blacklistListLeft = this.blacklistModalLeft + 14;
        this.blacklistListRight = this.blacklistModalRight - 14;
        this.blacklistCloseButtonLeft = this.blacklistModalRight - 28;
        this.blacklistCloseButtonTop = this.blacklistModalTop + 8;
        this.blacklistCloseButtonRight = this.blacklistCloseButtonLeft + ROW_HEIGHT;
        this.blacklistCloseButtonBottom = this.blacklistCloseButtonTop + ROW_HEIGHT;
        int footerTotalWidth = (98 * BLACKLIST_MODAL_FILTER_ROWS) + 8;
        int footerStartX = this.blacklistModalLeft + ((modalWidth - footerTotalWidth) / BLACKLIST_MODAL_FILTER_ROWS);
        int footerY = this.blacklistModalBottom - 26;
        this.blacklistResetButtonLeft = footerStartX;
        this.blacklistResetButtonTop = footerY;
        this.blacklistResetButtonRight = footerStartX + 98;
        this.blacklistResetButtonBottom = footerY + 18;
        this.blacklistDoneButtonLeft = this.blacklistResetButtonRight + 8;
        this.blacklistDoneButtonTop = footerY;
        this.blacklistDoneButtonRight = this.blacklistDoneButtonLeft + 98;
        this.blacklistDoneButtonBottom = footerY + 18;
    }

    private void ensureBlacklistDataStructures() {
        if (this.data.itemBlacklist == null) {
            this.data.itemBlacklist = new ArrayList();
        }
        if (this.data.blacklistApplyDualHanded == null) {
            this.data.blacklistApplyDualHanded = new LinkedHashMap();
        }
    }

    private boolean isBlacklistEnabled(String entry) {
        if (entry == null || entry.isBlank() || this.data.itemBlacklist == null) {
            return false;
        }
        return this.data.itemBlacklist.contains(entry);
    }

    private void moveBlacklistEntryToTop(String entry) {
        ensureBlacklistDataStructures();
        this.data.itemBlacklist.removeIf(existing -> {
            return entry.equals(existing);
        });
        this.data.itemBlacklist.add(0, entry);
    }

    private void toggleBlacklistEnabled(String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }
        ensureBlacklistDataStructures();
        if (isBlacklistEnabled(entry)) {
            this.data.itemBlacklist.removeIf(existing -> {
                return entry.equals(existing);
            });
            this.data.blacklistApplyDualHanded.remove(entry);
        } else {
            moveBlacklistEntryToTop(entry);
            this.data.blacklistApplyDualHanded.putIfAbsent(entry, false);
            this.blacklistModalScrollOffset = 0;
        }
        refreshBlacklistFilteredEntries();
    }

    private void toggleBlacklistDual(String entry) {
        if (entry == null || entry.isBlank()) {
            return;
        }
        ensureBlacklistDataStructures();
        if (!isBlacklistEnabled(entry)) {
            moveBlacklistEntryToTop(entry);
            this.data.blacklistApplyDualHanded.put(entry, true);
            this.blacklistModalScrollOffset = 0;
            refreshBlacklistFilteredEntries();
            return;
        }
        boolean nextValue = !Boolean.TRUE.equals(this.data.blacklistApplyDualHanded.get(entry));
        this.data.blacklistApplyDualHanded.put(entry, Boolean.valueOf(nextValue));
    }

    private void toggleBlacklistNamespace(String namespace) {
        if (!isValidNamespace(namespace)) {
            return;
        }
        if (namespace.equals(this.blacklistSelectedNamespace)) {
            this.blacklistSelectedNamespace = "";
        } else {
            this.blacklistSelectedNamespace = namespace;
        }
        this.blacklistModalScrollOffset = 0;
        refreshBlacklistFilteredEntries();
    }

    private String resolveModDisplayName(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return "";
        }
        if ("minecraft".equals(namespace)) {
            return "Minecraft";
        }
        String resolved = Services.PLATFORM.getModDisplayName(namespace);
        if (resolved != null && !resolved.isBlank() && !resolved.equalsIgnoreCase(namespace)) {
            return resolved;
        }
        return humanizeNamespace(namespace);
    }

    private static String humanizeNamespace(String namespace) {
        String normalized = namespace.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isBlank()) {
            return namespace;
        }
        StringBuilder out = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == ' ') {
                out.append(c);
                capitalize = true;
            } else if (capitalize) {
                out.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private boolean allFilteredEnabled() {
        if (this.blacklistFilteredEntries.isEmpty()) {
            return false;
        }
        for (BlacklistCatalogEntry entry : this.blacklistFilteredEntries) {
            if (!isBlacklistEnabled(entry.id)) {
                return false;
            }
        }
        return true;
    }

    private boolean allFilteredDual() {
        if (this.blacklistFilteredEntries.isEmpty()) {
            return false;
        }
        for (BlacklistCatalogEntry entry : this.blacklistFilteredEntries) {
            if (!isBlacklistEnabled(entry.id) || !isBlacklistDualHanded(entry.id)) {
                return false;
            }
        }
        return true;
    }

    private void setFilteredEnableAll(boolean enable) {
        if (this.blacklistFilteredEntries.isEmpty()) {
            return;
        }
        ensureBlacklistDataStructures();
        if (enable) {
            for (int i = this.blacklistFilteredEntries.size() - 1; i >= 0; i--) {
                BlacklistCatalogEntry entry = this.blacklistFilteredEntries.get(i);
                moveBlacklistEntryToTop(entry.id);
                this.data.blacklistApplyDualHanded.putIfAbsent(entry.id, false);
            }
        } else {
            for (BlacklistCatalogEntry entry2 : this.blacklistFilteredEntries) {
                this.data.itemBlacklist.removeIf(existing -> {
                    return entry2.id.equals(existing);
                });
                this.data.blacklistApplyDualHanded.remove(entry2.id);
            }
        }
        this.blacklistModalScrollOffset = 0;
        refreshBlacklistFilteredEntries();
    }

    private void setFilteredDualAll(boolean dual) {
        if (this.blacklistFilteredEntries.isEmpty()) {
            return;
        }
        ensureBlacklistDataStructures();
        for (int i = this.blacklistFilteredEntries.size() - 1; i >= 0; i--) {
            BlacklistCatalogEntry entry = this.blacklistFilteredEntries.get(i);
            if (!isBlacklistEnabled(entry.id)) {
                moveBlacklistEntryToTop(entry.id);
            }
            this.data.blacklistApplyDualHanded.put(entry.id, Boolean.valueOf(dual));
        }
        this.blacklistModalScrollOffset = 0;
        refreshBlacklistFilteredEntries();
    }

    private void resetBlacklistOnly() {
        PunchyConfig.Data defaults = new PunchyConfig.Data();
        this.data.itemBlacklist = new ArrayList(defaults.itemBlacklist);
        this.data.blacklistApplyDualHanded = new LinkedHashMap(defaults.blacklistApplyDualHanded);
        this.blacklistModalScrollOffset = 0;
        refreshBlacklistFilteredEntries();
    }

    private boolean isMouseInside(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        return mouseX >= ((double) left) && mouseX <= ((double) right) && mouseY >= ((double) top) && mouseY <= ((double) bottom);
    }

    private int addLinkButton(int row, int startX, int startY, int width, String labelKey, String url) {
        int y = startY + rowOffset(row);
        class_4185 linkButton = method_37063(class_4185.method_46430(class_2561.method_43471(labelKey), button -> {
            openLink(url);
        }).method_46433(startX, y).method_46437(width, ROW_HEIGHT).method_46431());
        if ("punchy.config.support.donate".equals(labelKey)) {
            this.supportDonateButton = linkButton;
        }
        return row + 1;
    }

    private void openLink(String url) {
        if (url == null || url.isBlank() || this.field_22787 == null) {
            return;
        }
        class_310 client = this.field_22787;
        client.method_1507(new class_407(confirmed -> {
            if (confirmed) {
                openUri(url);
            }
            client.method_1507(this);
        }, url, true));
    }

    private static void openUri(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                return;
            }
            desktop.browse(new URI(url));
        } catch (Exception e) {
        }
    }

    private int addToggleWithReset(int row, int startX, int startY, int optionWidth, String labelKey, BooleanSupplier getter, Consumer<Boolean> setter, boolean resetValue) {
        int y = startY + rowOffset(row);
        class_4185 toggle = createToggleButton(startX, y, optionWidth, labelKey, getter, setter);
        registerAnimationWidget(toggle, y);
        registerAnimationWidget(class_4185.method_46430(class_2561.method_43471("punchy.config.reset.option"), button -> {
            setter.accept(Boolean.valueOf(resetValue));
            toggle.method_25355(toggleLabel(labelKey, resetValue));
        }).method_46433(startX + optionWidth + RESET_GAP, y).method_46437(RESET_WIDTH, ROW_HEIGHT).method_46431(), y);
        return row + 1;
    }

    private int addInteractiveStuffCompatToggle(int row, int startX, int startY, int optionWidth) {
        int y = startY + rowOffset(row);
        class_4185 toggle = class_4185.method_46430(interactiveStuffCompatLabel(this.data.interactiveStuffCompat), btn -> {
            this.data.interactiveStuffCompat = !this.data.interactiveStuffCompat;
            btn.method_25355(interactiveStuffCompatLabel(this.data.interactiveStuffCompat));
        }).method_46433(startX, y).method_46437(optionWidth, ROW_HEIGHT).method_46431();
        registerAnimationWidget(toggle, y);
        registerAnimationWidget(class_4185.method_46430(class_2561.method_43471("punchy.config.reset.option"), button -> {
            this.data.interactiveStuffCompat = false;
            toggle.method_25355(interactiveStuffCompatLabel(false));
        }).method_46433(startX + optionWidth + RESET_GAP, y).method_46437(RESET_WIDTH, ROW_HEIGHT).method_46431(), y);
        return row + 1;
    }

    private class_4185 createToggleButton(int x, int y, int width, String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) {
        class_4185 button = class_4185.method_46430(toggleLabel(labelKey, getter.getAsBoolean()), btn -> {
            boolean next = !getter.getAsBoolean();
            setter.accept(Boolean.valueOf(next));
            btn.method_25355(toggleLabel(labelKey, next));
        }).method_46433(x, y).method_46437(width, ROW_HEIGHT).method_46431();
        return button;
    }

    private class_2561 toggleLabel(String labelKey, boolean value) {
        return class_2561.method_43471(labelKey).method_10852(class_2561.method_43470(": ")).method_10852(class_2561.method_43471(value ? "punchy.toggle.on" : "punchy.toggle.off"));
    }

    private class_2561 interactiveStuffCompatLabel(boolean value) {
        return class_2561.method_43471("punchy.config.option.interactive_stuff_compat").method_10852(class_2561.method_43470(": ")).method_10852(class_2561.method_43471(value ? "punchy.toggle.on" : "punchy.toggle.off"));
    }

    private void saveAndClose() {
        LinkedHashMap linkedHashMap;
        LinkedHashMap linkedHashMap2;
        LinkedHashMap linkedHashMap3;
        if (this.data.enableTuning && !this.data.enableMod) {
            this.data.enableMod = true;
        }
        PunchyConfig.Data live = PunchyConfig.snapshot();
        PunchyConfig.Data data = this.data;
        if (live.animationPackSelections == null) {
            linkedHashMap = new LinkedHashMap();
        } else {
            linkedHashMap = new LinkedHashMap(live.animationPackSelections);
        }
        data.animationPackSelections = linkedHashMap;
        PunchyConfig.Data data2 = this.data;
        if (live.animationPackTypeSelections == null) {
            linkedHashMap2 = new LinkedHashMap();
        } else {
            linkedHashMap2 = new LinkedHashMap(live.animationPackTypeSelections);
        }
        data2.animationPackTypeSelections = linkedHashMap2;
        PunchyConfig.Data data3 = this.data;
        if (live.mixSpecificForceOverride == null) {
            linkedHashMap3 = new LinkedHashMap();
        } else {
            linkedHashMap3 = new LinkedHashMap(live.mixSpecificForceOverride);
        }
        data3.mixSpecificForceOverride = linkedHashMap3;
        restoreGuiScaleOverride();
        PunchyConfig.applyAndSave(this.data);
        PunchyDebug.setDebugPositionEnabled(this.data.enableTuning && this.data.enableMod);
        PunchyAnimationManager.setAnimationSpeed(PunchyConfig.getAnimationSpeed());
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
            restoreGuiScaleOverride();
        }
    }

    private void resetAll() {
        PunchyConfig.Data defaults = new PunchyConfig.Data();
        this.data.renderArmorArmsFP = defaults.renderArmorArmsFP;
        this.data.interactiveStuffCompat = defaults.interactiveStuffCompat;
        this.data.bettercombatCompat = defaults.bettercombatCompat;
        this.data.swordBlockingCompat = defaults.swordBlockingCompat;
        this.data.animationSpeed = defaults.animationSpeed;
        this.data.enableMod = defaults.enableMod;
        this.data.enableTuning = defaults.enableTuning;
        this.data.disableResourcePackModelParts = defaults.disableResourcePackModelParts;
        this.data.disableArmPhysics = defaults.disableArmPhysics;
        this.data.disableNativeItemPhysics = defaults.disableNativeItemPhysics;
        this.data.disableBoatMinecartRaftModels = defaults.disableBoatMinecartRaftModels;
        this.data.disablePistonModels = defaults.disablePistonModels;
        this.data.disableChestModels = defaults.disableChestModels;
        this.data.disableEnchantingTableModels = defaults.disableEnchantingTableModels;
        this.data.disableBoatFirstPersonAnimations = defaults.disableBoatFirstPersonAnimations;
        this.data.disableEnhancedFireArmEffects = defaults.disableEnhancedFireArmEffects;
        this.data.enableBlurGlowAtDark = defaults.enableBlurGlowAtDark;
        this.data.enableCrawlAnimation = defaults.enableCrawlAnimation;
        this.data.enableClimbAnimation = defaults.enableClimbAnimation;
        this.data.enableSwimAnimation = defaults.enableSwimAnimation;
        this.data.enableElytraFlightAnimation = defaults.enableElytraFlightAnimation;
        this.data.enableCustomWalk = defaults.enableCustomWalk;
        this.data.enableSprintArmSwing = defaults.enableSprintArmSwing;
        this.data.enableFallArmAnimation = defaults.enableFallArmAnimation;
        this.data.sprintArmSwingIntensity = defaults.sprintArmSwingIntensity;
        this.data.sprintArmSwingSpeed = defaults.sprintArmSwingSpeed;
        this.data.enableFreezeShake = defaults.enableFreezeShake;
        this.data.enableFreezeArmOverlay = defaults.enableFreezeArmOverlay;
        this.data.enableMud = defaults.enableMud;
        this.data.enableSweat = defaults.enableSweat;
        this.data.enableBurn = defaults.enableBurn;
        this.data.enableParticles = defaults.enableParticles;
        this.data.enableModelPartsGlow = defaults.enableModelPartsGlow;
        this.data.firstPersonModelHideEnabled = defaults.firstPersonModelHideEnabled;
        this.data.firstPersonModelHidePitch = defaults.firstPersonModelHidePitch;
        this.data.itemBlacklist = new ArrayList(defaults.itemBlacklist);
        this.data.blacklistApplyDualHanded = new LinkedHashMap(defaults.blacklistApplyDualHanded);
        PunchyAnimationManager.setAnimationSpeed(this.data.animationSpeed);
    }

    public void method_25419() {
        saveAndClose();
    }

    public void method_25394(class_332 graphics, int mouseX, int mouseY, float delta) {
        graphics.method_25294(0, 0, this.field_22789, this.field_22790, -1442840576);
        if (this.blacklistModalOpen) {
            drawConfigPanels(graphics);
            super.method_25394(graphics, mouseX, mouseY, delta);
            drawBlacklistModalOverlay(graphics, mouseX, mouseY);
            if (this.blacklistSearchInput != null && this.blacklistSearchInput.field_22764) {
                this.blacklistSearchInput.method_25394(graphics, mouseX, mouseY, delta);
                return;
            }
            return;
        }
        drawConfigPanels(graphics);
        List<class_4185> styledButtons = new ArrayList<>();
        List<class_2561> originalMessages = new ArrayList<>();
        List<StyledConfigSlider> styledSliders = new ArrayList<>();
        List<class_2561> originalSliderMessages = new ArrayList<>();
        for (class_4185 class_4185Var : method_25396()) {
            if (class_4185Var instanceof class_4185) {
                class_4185 button = class_4185Var;
                if (button.field_22764) {
                    styledButtons.add(button);
                    originalMessages.add(button.method_25369());
                    button.method_25355(class_2561.method_43473());
                }
            }
            if (class_4185Var instanceof StyledConfigSlider) {
                StyledConfigSlider slider = (StyledConfigSlider) class_4185Var;
                if (slider.field_22764) {
                    styledSliders.add(slider);
                    originalSliderMessages.add(slider.method_25369());
                    slider.method_25355(class_2561.method_43473());
                }
            }
        }
        super.method_25394(graphics, mouseX, mouseY, delta);
        for (int i = 0; i < styledButtons.size(); i++) {
            styledButtons.get(i).method_25355(originalMessages.get(i));
        }
        for (int i2 = 0; i2 < styledSliders.size(); i2++) {
            styledSliders.get(i2).method_25355(originalSliderMessages.get(i2));
        }
        renderStyledSliders(graphics, mouseX, mouseY);
        renderStyledButtons(graphics, mouseX, mouseY);
        drawProfileListPanel(graphics);
        if (this.blacklistHelpLines != null && !this.blacklistHelpLines.isEmpty()) {
            Objects.requireNonNull(this.field_22793);
            int lineHeight = (int) Math.ceil(9.0f * 1.0f);
            for (int i3 = 0; i3 < this.blacklistHelpLines.size(); i3++) {
                class_5481 line = this.blacklistHelpLines.get(i3);
                int textX = this.blacklistHelpX;
                int textY = this.blacklistHelpY + (i3 * lineHeight);
                graphics.method_51430(this.field_22793, line, textX, textY, -1, false);
            }
        }
        if (this.tab == Tab.BLACKLIST && this.blacklistViewportHeight > 0 && !this.blacklistEntryButtons.isEmpty()) {
            int totalHeight = (this.blacklistEntryButtons.size() * this.blacklistEntryHeight) - ROW_SPACING;
            int maxScroll = Math.max(0, totalHeight - this.blacklistViewportHeight);
            if (maxScroll > 0) {
                int barX = (this.blacklistHelpX + this.blacklistHelpWidth) - 4;
                int barTop = this.blacklistListStartY;
                int barBottom = this.blacklistListStartY + this.blacklistViewportHeight;
                graphics.method_25294(barX, barTop, barX + 4, barBottom, -1442840576);
                int trackHeight = Math.max(1, barBottom - barTop);
                int thumbHeight = Math.max(8, Math.round(trackHeight * (this.blacklistViewportHeight / totalHeight)));
                int thumbMax = trackHeight - thumbHeight;
                int thumbOffset = maxScroll == 0 ? 0 : Math.round(thumbMax * (this.blacklistScrollOffset / maxScroll));
                int thumbTop = barTop + thumbOffset;
                int thumbBottom = Math.min(barBottom, thumbTop + thumbHeight);
                graphics.method_25294(barX, thumbTop, barX + 4, thumbBottom, -1);
            }
        }
        if (this.animationViewportHeight > 0 && this.animationRowCount > 0) {
            int totalHeight2 = (this.animationRowCount * this.animationEntryHeight) - ROW_SPACING;
            int maxScroll2 = Math.max(0, totalHeight2 - this.animationViewportHeight);
            if (maxScroll2 > 0) {
                int barX2 = Math.max(this.centerPanelLeft + BLACKLIST_MODAL_FILTER_ROWS, this.centerPanelRight - 7);
                int barTop2 = this.animationListStartY;
                int barBottom2 = this.animationListStartY + this.animationViewportHeight;
                graphics.method_25294(barX2, barTop2, barX2 + 4, barBottom2, -1442840576);
                int trackHeight2 = Math.max(1, barBottom2 - barTop2);
                int thumbHeight2 = Math.max(8, Math.round(trackHeight2 * (this.animationViewportHeight / totalHeight2)));
                int thumbMax2 = trackHeight2 - thumbHeight2;
                int thumbOffset2 = maxScroll2 == 0 ? 0 : Math.round(thumbMax2 * (this.animationScrollOffset / maxScroll2));
                int thumbTop2 = barTop2 + thumbOffset2;
                int thumbBottom2 = Math.min(barBottom2, thumbTop2 + thumbHeight2);
                graphics.method_25294(barX2, thumbTop2, barX2 + 4, thumbBottom2, -1);
            }
        }
    }

    private void renderStyledSliders(class_332 graphics, int mouseX, int mouseY) {
        for (StyledConfigSlider styledConfigSlider : method_25396()) {
            if (styledConfigSlider instanceof StyledConfigSlider) {
                StyledConfigSlider slider = styledConfigSlider;
                if (slider.field_22764) {
                    slider.drawStyled(graphics, mouseX, mouseY);
                }
            }
        }
    }

    private void drawConfigPanels(class_332 graphics) {
        graphics.method_25294(this.mainPanelLeft, this.mainPanelTop, this.mainPanelRight, this.mainPanelBottom, -267381478);
        graphics.method_25294(this.mainPanelLeft, this.mainPanelTop, this.mainPanelRight, this.mainPanelTop + 1, -11641751);
        graphics.method_25294(this.mainPanelLeft, this.mainPanelBottom - 1, this.mainPanelRight, this.mainPanelBottom, -11641751);
        graphics.method_25294(this.mainPanelLeft, this.mainPanelTop, this.mainPanelLeft + 1, this.mainPanelBottom, -11641751);
        graphics.method_25294(this.mainPanelRight - 1, this.mainPanelTop, this.mainPanelRight, this.mainPanelBottom, -11641751);
        drawSectionPanel(graphics, this.leftPanelLeft, this.leftPanelTop, this.leftPanelRight, this.leftPanelBottom, 857944122, 1430145621);
        drawSectionPanel(graphics, this.centerPanelLeft, this.centerPanelTop, this.centerPanelRight, this.centerPanelBottom, 857944122, 1430145621);
        drawSectionPanel(graphics, this.rightPanelLeft, this.rightPanelTop, this.rightPanelRight, this.rightPanelBottom, 857944122, 1430145621);
    }

    private void drawSectionPanel(class_332 graphics, int left, int top, int right, int bottom, int fill, int border) {
        graphics.method_25294(left, top, right, bottom, fill);
        graphics.method_25294(left, top, right, top + 1, border);
        graphics.method_25294(left, bottom - 1, right, bottom, border);
        graphics.method_25294(left, top, left + 1, bottom, border);
        graphics.method_25294(right - 1, top, right, bottom, border);
    }

    private void drawProfileListPanel(class_332 graphics) {
        if (this.profileListW <= 0 || this.profileListH <= 0) {
            return;
        }
        int x0 = this.profileListX;
        int y0 = this.profileListY;
        int x1 = this.profileListX + this.profileListW;
        int y1 = this.profileListY + this.profileListH;
        graphics.method_25294(x0, y0, x1, y1, 1141641495);
    }

    private void renderStyledButtons(class_332 graphics, int mouseX, int mouseY) {
        for (class_364 child : method_25396()) {
            if (child instanceof class_4185) {
                class_4185 button = (class_4185) child;
                if (button.field_22764) {
                    boolean destructive = button == this.profileDeleteButton;
                    boolean positive = button == this.supportDonateButton;
                    drawStyledButton(graphics, button, mouseX, mouseY, destructive, positive);
                }
            }
        }
    }

    private void drawStyledButton(class_332 graphics, class_4185 button, int mouseX, int mouseY, boolean destructive, boolean positive) {
        int fill;
        int border;
        int textColor;
        int left = button.method_46426();
        int top = button.method_46427();
        int right = left + button.method_25368();
        int bottom = top + button.method_25364();
        boolean hovered = isMouseInside(mouseX, mouseY, left, top, right, bottom);
        boolean active = button.field_22763;
        if (!active) {
            fill = -14801620;
            border = -12694955;
            textColor = -8483429;
        } else if (positive) {
            fill = hovered ? -13726141 : -14521293;
            border = -8531567;
            textColor = -1;
        } else if (destructive) {
            fill = hovered ? -7459282 : -9690590;
            border = -3048067;
            textColor = -1;
        } else if (hovered) {
            fill = -12958126;
            border = -7485697;
            textColor = -1;
        } else {
            fill = -13682107;
            border = -7429710;
            textColor = -1;
        }
        graphics.method_25294(left, top, right, bottom, fill);
        graphics.method_25294(left, top, right, top + 1, border);
        graphics.method_25294(left, bottom - 1, right, bottom, border);
        graphics.method_25294(left, top, left + 1, bottom, border);
        graphics.method_25294(right - 1, top, right, bottom, border);
        String label = button.method_25369() == null ? "" : button.method_25369().getString();
        int maxTextWidth = Math.max(8, button.method_25368() - MAIN_PANEL_GAP);
        String renderLabel = resolveScrollableButtonLabel(label, maxTextWidth, hovered);
        int textX = left + ((button.method_25368() - this.field_22793.method_1727(renderLabel)) / BLACKLIST_MODAL_FILTER_ROWS);
        int textY = top + ((button.method_25364() - 8) / BLACKLIST_MODAL_FILTER_ROWS);
        graphics.method_25303(this.field_22793, renderLabel, textX, textY, textColor);
    }

    private String resolveScrollableButtonLabel(String label, int maxTextWidth, boolean hovered) {
        if (label == null || label.isEmpty()) {
            return "";
        }
        if (this.field_22793.method_1727(label) <= maxTextWidth) {
            return label;
        }
        if (!hovered) {
            if (maxTextWidth > this.field_22793.method_1727("...")) {
                return this.field_22793.method_27523(label, maxTextWidth - this.field_22793.method_1727("...")) + "...";
            }
            return this.field_22793.method_27523(label, maxTextWidth);
        }
        String cycle = label + "   ";
        int cycleLength = cycle.length();
        if (cycleLength <= 1) {
            return this.field_22793.method_27523(label, maxTextWidth);
        }
        long tick = System.currentTimeMillis() / 150;
        int offset = (int) (tick % ((long) cycleLength));
        String shifted = cycle.substring(offset) + cycle.substring(0, offset);
        String visible = this.field_22793.method_27523(shifted, maxTextWidth);
        if (visible.isBlank()) {
            visible = this.field_22793.method_27523(label, maxTextWidth);
        }
        return visible;
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (!this.blacklistModalOpen) {
            if (button == 0) {
                if (isMouseOverBlacklistScrollbar(mouseX, mouseY)) {
                    this.draggingBlacklistScrollbar = true;
                    updateBlacklistScrollFromMouse(mouseY);
                    return true;
                }
                if (isMouseOverAnimationScrollbar(mouseX, mouseY)) {
                    this.draggingAnimationScrollbar = true;
                    updateAnimationScrollFromMouse(mouseY);
                    return true;
                }
            }
            return super.method_25402(mouseX, mouseY, button);
        }
        if (button != 0) {
            return true;
        }
        if (isMouseOverBlacklistModalScrollbar(mouseX, mouseY)) {
            this.draggingBlacklistModalScrollbar = true;
            updateBlacklistModalScrollFromMouse(mouseY);
            return true;
        }
        if (super.method_25402(mouseX, mouseY, button)) {
            return true;
        }
        if (isMouseOverBlacklistNamespaceScrollbar(mouseX, mouseY)) {
            this.draggingBlacklistNamespaceScrollbar = true;
            updateBlacklistNamespaceScrollFromMouse(mouseY);
            return true;
        }
        if (isMouseInside(mouseX, mouseY, this.blacklistResetButtonLeft, this.blacklistResetButtonTop, this.blacklistResetButtonRight, this.blacklistResetButtonBottom)) {
            resetBlacklistOnly();
            return true;
        }
        if (isMouseInside(mouseX, mouseY, this.blacklistDoneButtonLeft, this.blacklistDoneButtonTop, this.blacklistDoneButtonRight, this.blacklistDoneButtonBottom)) {
            closeBlacklistModal();
            return true;
        }
        for (BlacklistHitbox hitbox : this.blacklistNamespaceHitboxes) {
            if (isMouseInside(mouseX, mouseY, hitbox.left, hitbox.top, hitbox.right, hitbox.bottom)) {
                toggleBlacklistNamespace(hitbox.key);
                return true;
            }
        }
        for (BlacklistHitbox hitbox2 : this.blacklistEnableHitboxes) {
            if (isMouseInside(mouseX, mouseY, hitbox2.left, hitbox2.top, hitbox2.right, hitbox2.bottom)) {
                if (BLACKLIST_BULK_ENABLE_KEY.equals(hitbox2.key)) {
                    setFilteredEnableAll(!allFilteredEnabled());
                    return true;
                }
                toggleBlacklistEnabled(hitbox2.key);
                return true;
            }
        }
        for (BlacklistHitbox hitbox3 : this.blacklistDualHitboxes) {
            if (isMouseInside(mouseX, mouseY, hitbox3.left, hitbox3.top, hitbox3.right, hitbox3.bottom)) {
                if (BLACKLIST_BULK_DUAL_KEY.equals(hitbox3.key)) {
                    setFilteredDualAll(!allFilteredDual());
                    return true;
                }
                toggleBlacklistDual(hitbox3.key);
                return true;
            }
        }
        if (isMouseInside(mouseX, mouseY, this.blacklistModalLeft, this.blacklistModalTop, this.blacklistModalRight, this.blacklistModalBottom)) {
            return true;
        }
        return super.method_25402(mouseX, mouseY, button);
    }

    public boolean method_25403(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (this.draggingBlacklistModalScrollbar) {
                updateBlacklistModalScrollFromMouse(mouseY);
                return true;
            }
            if (this.draggingBlacklistNamespaceScrollbar) {
                updateBlacklistNamespaceScrollFromMouse(mouseY);
                return true;
            }
            if (!this.blacklistModalOpen && this.draggingBlacklistScrollbar) {
                updateBlacklistScrollFromMouse(mouseY);
                return true;
            }
            if (!this.blacklistModalOpen && this.draggingAnimationScrollbar) {
                updateAnimationScrollFromMouse(mouseY);
                return true;
            }
        }
        return super.method_25403(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean method_25406(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingBlacklistModalScrollbar = false;
            this.draggingBlacklistNamespaceScrollbar = false;
            this.draggingBlacklistScrollbar = false;
            this.draggingAnimationScrollbar = false;
        }
        return super.method_25406(mouseX, mouseY, button);
    }

    public boolean method_25401(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        int maxScroll;
        if (!this.blacklistModalOpen && isMouseOverProfileList(mouseX, mouseY) && (maxScroll = Math.max(0, this.profiles.size() - this.profileRowButtons.size())) > 0) {
            this.profileListScroll = class_3532.method_15340(this.profileListScroll - ((int) Math.signum(scrollDeltaY)), 0, maxScroll);
            updateProfileRowButtons();
            return true;
        }
        if (handleBlacklistNamespaceScroll(mouseX, mouseY, scrollDeltaY) || handleBlacklistModalScroll(mouseX, mouseY, scrollDeltaY) || handleBlacklistScroll(mouseX, mouseY, scrollDeltaY) || handleAnimationScroll(mouseX, mouseY, scrollDeltaY)) {
            return true;
        }
        return super.method_25401(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    private boolean handleBlacklistModalScroll(double mouseX, double mouseY, double scrollDelta) {
        if (!this.blacklistModalOpen || this.blacklistModalMaxScroll <= 0 || !isMouseInside(mouseX, mouseY, this.blacklistListLeft, this.blacklistListTop, this.blacklistListRight, this.blacklistListBottom)) {
            return false;
        }
        int deltaPixels = (int) Math.round(scrollDelta * 22.0d);
        this.blacklistModalScrollOffset = clamp(this.blacklistModalScrollOffset - deltaPixels, 0, this.blacklistModalMaxScroll);
        return true;
    }

    private boolean handleBlacklistNamespaceScroll(double mouseX, double mouseY, double scrollDelta) {
        if (!this.blacklistModalOpen || this.blacklistNamespaceMaxScroll <= 0 || !isMouseInside(mouseX, mouseY, this.blacklistNamespaceAreaLeft, this.blacklistNamespaceAreaTop, this.blacklistNamespaceAreaRight, this.blacklistNamespaceAreaBottom)) {
            return false;
        }
        int deltaPixels = (int) Math.round(scrollDelta * 20.0d);
        this.blacklistNamespaceScrollOffset = clamp(this.blacklistNamespaceScrollOffset - deltaPixels, 0, this.blacklistNamespaceMaxScroll);
        return true;
    }

    private boolean isMouseOverBlacklistModalScrollbar(double mouseX, double mouseY) {
        if (!this.blacklistModalOpen || this.blacklistModalMaxScroll <= 0) {
            return false;
        }
        int barX = this.blacklistListRight - 4;
        return isMouseInside(mouseX, mouseY, barX, this.blacklistListTop, barX + 4, this.blacklistListBottom);
    }

    private boolean isMouseOverBlacklistNamespaceScrollbar(double mouseX, double mouseY) {
        if (!this.blacklistModalOpen || this.blacklistNamespaceMaxScroll <= 0) {
            return false;
        }
        int barX = this.blacklistNamespaceAreaRight - 4;
        return isMouseInside(mouseX, mouseY, barX, this.blacklistNamespaceAreaTop, barX + 4, this.blacklistNamespaceAreaBottom);
    }

    private void updateBlacklistModalScrollFromMouse(double mouseY) {
        if (this.blacklistModalMaxScroll <= 0) {
            return;
        }
        int barTop = this.blacklistListTop;
        int barBottom = this.blacklistListBottom;
        int track = Math.max(1, barBottom - barTop);
        double ratio = (mouseY - ((double) barTop)) / ((double) track);
        this.blacklistModalScrollOffset = clamp((int) Math.round(ratio * ((double) this.blacklistModalMaxScroll)), 0, this.blacklistModalMaxScroll);
    }

    private void updateBlacklistNamespaceScrollFromMouse(double mouseY) {
        if (this.blacklistNamespaceMaxScroll <= 0) {
            return;
        }
        int barTop = this.blacklistNamespaceAreaTop;
        int barBottom = this.blacklistNamespaceAreaBottom;
        int track = Math.max(1, barBottom - barTop);
        double ratio = (mouseY - ((double) barTop)) / ((double) track);
        this.blacklistNamespaceScrollOffset = clamp((int) Math.round(ratio * ((double) this.blacklistNamespaceMaxScroll)), 0, this.blacklistNamespaceMaxScroll);
    }

    private int computeBlacklistChipTotalHeight(int chipLeft, int chipContentRight, int chipHeight, int chipGap) {
        int chipLayoutX = chipLeft;
        int chipLayoutY = 0;
        int chipTotalHeight = chipHeight;
        for (BlacklistModFilter filter : this.blacklistModFilters) {
            int chipWidth = Math.max(50, this.field_22793.method_1727(filter.displayName) + 18);
            if (chipLayoutX + chipWidth > chipContentRight) {
                chipLayoutX = chipLeft;
                chipLayoutY += chipHeight + chipGap;
            }
            chipTotalHeight = Math.max(chipTotalHeight, chipLayoutY + chipHeight);
            chipLayoutX += chipWidth + chipGap;
        }
        return chipTotalHeight;
    }

    private void drawBlacklistModalOverlay(class_332 graphics, int mouseX, int mouseY) {
        int iRound;
        int iRound2;
        ensureBlacklistCatalog();
        computeBlacklistModalLayout();
        this.blacklistNamespaceHitboxes.clear();
        this.blacklistEnableHitboxes.clear();
        this.blacklistDualHitboxes.clear();
        graphics.method_25294(this.blacklistModalLeft, this.blacklistModalTop, this.blacklistModalRight, this.blacklistModalBottom, -267381478);
        graphics.method_25294(this.blacklistModalLeft, this.blacklistModalTop, this.blacklistModalRight, this.blacklistModalTop + 1, -11641751);
        graphics.method_25294(this.blacklistModalLeft, this.blacklistModalBottom - 1, this.blacklistModalRight, this.blacklistModalBottom, -11641751);
        graphics.method_25294(this.blacklistModalLeft, this.blacklistModalTop, this.blacklistModalLeft + 1, this.blacklistModalBottom, -11641751);
        graphics.method_25294(this.blacklistModalRight - 1, this.blacklistModalTop, this.blacklistModalRight, this.blacklistModalBottom, -11641751);
        String titleText = class_2561.method_43471("punchy.config.blacklist.modal_title").getString();
        if (titleText == null || titleText.isBlank() || titleText.equals("punchy.config.blacklist.modal_title")) {
            titleText = "Item Blacklist";
        }
        graphics.method_25303(this.field_22793, titleText, this.blacklistModalLeft + 14, this.blacklistModalTop + MAIN_PANEL_GAP, -1);
        int namespaceTitleY = this.blacklistModalTop + 58;
        String globalLabel = class_1074.method_4662("punchy.config.blacklist.global", new Object[0]);
        if (globalLabel == null || globalLabel.isBlank() || globalLabel.equals("punchy.config.blacklist.global")) {
            globalLabel = "Filtrar por mod";
        }
        graphics.method_25303(this.field_22793, globalLabel, this.blacklistModalLeft + 14, namespaceTitleY, -4799537);
        int chipLeft = this.blacklistModalLeft + 14;
        int chipRight = this.blacklistModalRight - 18;
        int chipViewportTop = namespaceTitleY + MAIN_PANEL_GAP;
        int chipViewportHeight = (SIDEBAR_GAP * BLACKLIST_MODAL_FILTER_ROWS) + (4 * 1);
        int chipViewportBottom = chipViewportTop + chipViewportHeight;
        int chipContentRight = chipRight;
        int chipTotalHeight = computeBlacklistChipTotalHeight(chipLeft, chipContentRight, SIDEBAR_GAP, 4);
        this.blacklistNamespaceMaxScroll = Math.max(0, chipTotalHeight - chipViewportHeight);
        if (this.blacklistNamespaceMaxScroll > 0) {
            chipContentRight = (chipRight - 4) - BLACKLIST_MODAL_FILTER_ROWS;
            chipTotalHeight = computeBlacklistChipTotalHeight(chipLeft, chipContentRight, SIDEBAR_GAP, 4);
            this.blacklistNamespaceMaxScroll = Math.max(0, chipTotalHeight - chipViewportHeight);
        }
        this.blacklistNamespaceScrollOffset = clamp(this.blacklistNamespaceScrollOffset, 0, this.blacklistNamespaceMaxScroll);
        this.blacklistNamespaceAreaLeft = chipLeft;
        this.blacklistNamespaceAreaRight = chipRight;
        this.blacklistNamespaceAreaTop = chipViewportTop;
        this.blacklistNamespaceAreaBottom = chipViewportBottom;
        graphics.method_25294(chipLeft, chipViewportTop, chipRight, chipViewportBottom, 705631516);
        graphics.method_25294(chipLeft, chipViewportTop, chipRight, chipViewportTop + 1, 1430145621);
        graphics.method_25294(chipLeft, chipViewportBottom - 1, chipRight, chipViewportBottom, 1430145621);
        int chipX = chipLeft;
        int chipY = 0;
        int chipRow = 0;
        int chipRowHeight = SIDEBAR_GAP + 4;
        int firstVisibleChipRow = Math.max(0, this.blacklistNamespaceScrollOffset / Math.max(1, chipRowHeight));
        int lastVisibleChipRowExclusive = firstVisibleChipRow + BLACKLIST_MODAL_FILTER_ROWS;
        for (BlacklistModFilter filter : this.blacklistModFilters) {
            String label = filter.displayName;
            int chipWidth = Math.max(50, this.field_22793.method_1727(label) + 18);
            if (chipX + chipWidth > chipContentRight) {
                chipX = chipLeft;
                chipY += SIDEBAR_GAP + 4;
                chipRow++;
            }
            if (chipRow < firstVisibleChipRow || chipRow >= lastVisibleChipRowExclusive) {
                chipX += chipWidth + 4;
            } else {
                int drawTop = (chipViewportTop + chipY) - this.blacklistNamespaceScrollOffset;
                int drawBottom = drawTop + SIDEBAR_GAP;
                if (drawBottom <= chipViewportTop || drawTop >= chipViewportBottom) {
                    chipX += chipWidth + 4;
                } else {
                    boolean selected = filter.namespace.equals(this.blacklistSelectedNamespace);
                    int chipFill = selected ? -13869680 : -14471110;
                    int chipBorder = selected ? -7485697 : -9798515;
                    graphics.method_25294(chipX, drawTop, chipX + chipWidth, drawBottom, chipFill);
                    graphics.method_25294(chipX, drawTop, chipX + chipWidth, drawTop + 1, chipBorder);
                    graphics.method_25294(chipX, drawBottom - 1, chipX + chipWidth, drawBottom, chipBorder);
                    graphics.method_25294(chipX, drawTop, chipX + 1, drawBottom, chipBorder);
                    graphics.method_25294((chipX + chipWidth) - 1, drawTop, chipX + chipWidth, drawBottom, chipBorder);
                    graphics.method_25303(this.field_22793, label, chipX + 8, drawTop + 4, -1);
                    int clippedTop = Math.max(drawTop, chipViewportTop);
                    int clippedBottom = Math.min(drawBottom, chipViewportBottom);
                    if (clippedBottom > clippedTop) {
                        this.blacklistNamespaceHitboxes.add(new BlacklistHitbox(chipX, clippedTop, chipX + chipWidth, clippedBottom, filter.namespace));
                    }
                    chipX += chipWidth + 4;
                }
            }
        }
        if (this.blacklistNamespaceMaxScroll > 0) {
            int barX = chipRight - 4;
            int trackHeight = Math.max(1, chipViewportBottom - chipViewportTop);
            int thumbHeight = Math.max(RESET_GAP, Math.round(trackHeight * (chipViewportHeight / chipTotalHeight)));
            int thumbMax = trackHeight - thumbHeight;
            if (this.blacklistNamespaceMaxScroll == 0) {
                iRound2 = 0;
            } else {
                iRound2 = Math.round(thumbMax * (this.blacklistNamespaceScrollOffset / this.blacklistNamespaceMaxScroll));
            }
            int thumbOffset = iRound2;
            graphics.method_25294(barX, chipViewportTop, barX + 4, chipViewportBottom, -1442840576);
            graphics.method_25294(barX, chipViewportTop + thumbOffset, barX + 4, Math.min(chipViewportBottom, chipViewportTop + thumbOffset + thumbHeight), -1);
        }
        this.blacklistListLeft = this.blacklistModalLeft + 14;
        this.blacklistListRight = this.blacklistModalRight - 14;
        this.blacklistListTop = chipViewportBottom + RESET_GAP;
        this.blacklistListBottom = this.blacklistResetButtonTop - 8;
        if (this.blacklistListBottom <= this.blacklistListTop) {
            this.blacklistListBottom = this.blacklistListTop + BLACKLIST_MODAL_ROW_HEIGHT;
        }
        int rawViewportHeight = this.blacklistListBottom - this.blacklistListTop;
        int viewportRows = Math.max(1, rawViewportHeight / BLACKLIST_MODAL_ROW_HEIGHT);
        int viewportHeight = viewportRows * BLACKLIST_MODAL_ROW_HEIGHT;
        this.blacklistListBottom = this.blacklistListTop + viewportHeight;
        int totalHeight = (this.blacklistFilteredEntries.size() + 1) * BLACKLIST_MODAL_ROW_HEIGHT;
        this.blacklistModalMaxScroll = Math.max(0, totalHeight - viewportHeight);
        this.blacklistModalScrollOffset = clamp(this.blacklistModalScrollOffset, 0, this.blacklistModalMaxScroll);
        graphics.method_25294(this.blacklistListLeft, this.blacklistListTop, this.blacklistListRight, this.blacklistListBottom, 1141641495);
        graphics.method_25294(this.blacklistListLeft, this.blacklistListTop, this.blacklistListRight, this.blacklistListTop + 1, 1430145621);
        graphics.method_25294(this.blacklistListLeft, this.blacklistListBottom - 1, this.blacklistListRight, this.blacklistListBottom, 1430145621);
        int contentRight = this.blacklistListRight - ROW_SPACING;
        int dualLeft = contentRight - 66;
        int enableLeft = (dualLeft - ROW_SPACING) - 66;
        boolean bulkEnableOn = allFilteredEnabled();
        boolean bulkDualOn = allFilteredDual();
        int bulkTop = this.blacklistListTop - this.blacklistModalScrollOffset;
        int bulkBottom = bulkTop + BLACKLIST_MODAL_ROW_HEIGHT;
        if (bulkBottom > this.blacklistListTop && bulkTop < this.blacklistListBottom) {
            graphics.method_25294(this.blacklistListLeft + 1, bulkTop, this.blacklistListRight - 1, bulkBottom, 859064927);
            String bulkLabel = class_1074.method_4662("punchy.config.blacklist.bulk", new Object[0]);
            if (bulkLabel == null || bulkLabel.isBlank() || bulkLabel.equals("punchy.config.blacklist.bulk")) {
                bulkLabel = "Acoes em massa (filtro atual)";
            }
            graphics.method_25303(this.field_22793, bulkLabel, this.blacklistListLeft + 8, bulkTop + 7, -1);
            drawBlacklistActionButton(graphics, enableLeft, bulkTop + BLACKLIST_MODAL_FILTER_ROWS, enableLeft + 66, bulkBottom - BLACKLIST_MODAL_FILTER_ROWS, isMouseInside(mouseX, mouseY, enableLeft, bulkTop + BLACKLIST_MODAL_FILTER_ROWS, enableLeft + 66, bulkBottom - BLACKLIST_MODAL_FILTER_ROWS), bulkEnableOn, this.blacklistFilteredEntries.isEmpty(), blacklistEnableLabel(bulkEnableOn));
            this.blacklistEnableHitboxes.add(new BlacklistHitbox(enableLeft, bulkTop + BLACKLIST_MODAL_FILTER_ROWS, enableLeft + 66, bulkBottom - BLACKLIST_MODAL_FILTER_ROWS, BLACKLIST_BULK_ENABLE_KEY));
            drawBlacklistActionButton(graphics, dualLeft, bulkTop + BLACKLIST_MODAL_FILTER_ROWS, dualLeft + 66, bulkBottom - BLACKLIST_MODAL_FILTER_ROWS, isMouseInside(mouseX, mouseY, dualLeft, bulkTop + BLACKLIST_MODAL_FILTER_ROWS, dualLeft + 66, bulkBottom - BLACKLIST_MODAL_FILTER_ROWS), bulkDualOn, this.blacklistFilteredEntries.isEmpty(), blacklistDualShortLabel(bulkDualOn));
            this.blacklistDualHitboxes.add(new BlacklistHitbox(dualLeft, bulkTop + BLACKLIST_MODAL_FILTER_ROWS, dualLeft + 66, bulkBottom - BLACKLIST_MODAL_FILTER_ROWS, BLACKLIST_BULK_DUAL_KEY));
        }
        for (int i = 0; i < this.blacklistFilteredEntries.size(); i++) {
            int rowTop = (this.blacklistListTop + ((i + 1) * BLACKLIST_MODAL_ROW_HEIGHT)) - this.blacklistModalScrollOffset;
            int rowBottom = rowTop + BLACKLIST_MODAL_ROW_HEIGHT;
            if (rowBottom > this.blacklistListTop && rowTop < this.blacklistListBottom) {
                BlacklistCatalogEntry entry = this.blacklistFilteredEntries.get(i);
                boolean enabled = isBlacklistEnabled(entry.id);
                boolean dual = isBlacklistDualHanded(entry.id);
                int rowFill = (i & 1) == 0 ? 706553648 : 438118192;
                graphics.method_25294(this.blacklistListLeft + 1, rowTop, this.blacklistListRight - 1, rowBottom, rowFill);
                if (enabled) {
                    graphics.method_25294(this.blacklistListLeft + 1, rowTop, this.blacklistListLeft + 3, rowBottom, -9713270);
                }
                int iconY = rowTop + Math.max(0, 3);
                graphics.method_51427(entry.icon, this.blacklistListLeft + 5, iconY);
                int textX = this.blacklistListLeft + 25;
                graphics.method_25303(this.field_22793, entry.displayName, textX, rowTop + 3, -1);
                graphics.method_25303(this.field_22793, entry.id, textX, rowTop + MAIN_PANEL_GAP, -7429710);
                drawBlacklistActionButton(graphics, enableLeft, rowTop + BLACKLIST_MODAL_FILTER_ROWS, enableLeft + 66, rowBottom - BLACKLIST_MODAL_FILTER_ROWS, isMouseInside(mouseX, mouseY, enableLeft, rowTop + BLACKLIST_MODAL_FILTER_ROWS, enableLeft + 66, rowBottom - BLACKLIST_MODAL_FILTER_ROWS), enabled, false, blacklistEnableLabel(enabled));
                this.blacklistEnableHitboxes.add(new BlacklistHitbox(enableLeft, rowTop + BLACKLIST_MODAL_FILTER_ROWS, enableLeft + 66, rowBottom - BLACKLIST_MODAL_FILTER_ROWS, entry.id));
                drawBlacklistActionButton(graphics, dualLeft, rowTop + BLACKLIST_MODAL_FILTER_ROWS, dualLeft + 66, rowBottom - BLACKLIST_MODAL_FILTER_ROWS, isMouseInside(mouseX, mouseY, dualLeft, rowTop + BLACKLIST_MODAL_FILTER_ROWS, dualLeft + 66, rowBottom - BLACKLIST_MODAL_FILTER_ROWS), dual, false, blacklistDualShortLabel(dual));
                this.blacklistDualHitboxes.add(new BlacklistHitbox(dualLeft, rowTop + BLACKLIST_MODAL_FILTER_ROWS, dualLeft + 66, rowBottom - BLACKLIST_MODAL_FILTER_ROWS, entry.id));
            }
        }
        if (this.blacklistModalMaxScroll > 0) {
            int barX2 = this.blacklistListRight - 4;
            int barTop = this.blacklistListTop;
            int barBottom = this.blacklistListBottom;
            int trackHeight2 = Math.max(1, barBottom - barTop);
            int thumbHeight2 = Math.max(RESET_GAP, Math.round(trackHeight2 * (viewportHeight / totalHeight)));
            int thumbMax2 = trackHeight2 - thumbHeight2;
            if (this.blacklistModalMaxScroll == 0) {
                iRound = 0;
            } else {
                iRound = Math.round(thumbMax2 * (this.blacklistModalScrollOffset / this.blacklistModalMaxScroll));
            }
            int thumbOffset2 = iRound;
            graphics.method_25294(barX2, barTop, barX2 + 4, barBottom, -1442840576);
            graphics.method_25294(barX2, barTop + thumbOffset2, barX2 + 4, Math.min(barBottom, barTop + thumbOffset2 + thumbHeight2), -1);
        }
        String resetLabel = class_1074.method_4662("punchy.config.reset", new Object[0]);
        if (resetLabel == null || resetLabel.isBlank() || resetLabel.equals("punchy.config.reset")) {
            resetLabel = "Reset";
        }
        String doneLabel = class_1074.method_4662("gui.done", new Object[0]);
        if (doneLabel == null || doneLabel.isBlank() || doneLabel.equals("gui.done")) {
            doneLabel = "Done";
        }
        drawBlacklistFooterButton(graphics, this.blacklistResetButtonLeft, this.blacklistResetButtonTop, this.blacklistResetButtonRight, this.blacklistResetButtonBottom, isMouseInside(mouseX, mouseY, this.blacklistResetButtonLeft, this.blacklistResetButtonTop, this.blacklistResetButtonRight, this.blacklistResetButtonBottom), true, resetLabel);
        drawBlacklistFooterButton(graphics, this.blacklistDoneButtonLeft, this.blacklistDoneButtonTop, this.blacklistDoneButtonRight, this.blacklistDoneButtonBottom, isMouseInside(mouseX, mouseY, this.blacklistDoneButtonLeft, this.blacklistDoneButtonTop, this.blacklistDoneButtonRight, this.blacklistDoneButtonBottom), false, doneLabel);
    }

    private void drawBlacklistActionButton(class_332 graphics, int left, int top, int right, int bottom, boolean hovered, boolean activeState, boolean disabled, String label) {
        int fill;
        int border;
        int textColor;
        if (disabled) {
            fill = -14801620;
            border = -12694955;
            textColor = -8483429;
        } else if (activeState) {
            fill = hovered ? -13994943 : -14393546;
            border = -8662895;
            textColor = -1;
        } else {
            fill = hovered ? -12958126 : -13682107;
            border = -7429710;
            textColor = -1;
        }
        graphics.method_25294(left, top, right, bottom, fill);
        graphics.method_25294(left, top, right, top + 1, border);
        graphics.method_25294(left, bottom - 1, right, bottom, border);
        graphics.method_25294(left, top, left + 1, bottom, border);
        graphics.method_25294(right - 1, top, right, bottom, border);
        int textX = left + (((right - left) - this.field_22793.method_1727(label)) / BLACKLIST_MODAL_FILTER_ROWS);
        int textY = top + (((bottom - top) - 8) / BLACKLIST_MODAL_FILTER_ROWS);
        graphics.method_25303(this.field_22793, label, textX, textY, textColor);
    }

    private void drawBlacklistFooterButton(class_332 graphics, int left, int top, int right, int bottom, boolean hovered, boolean destructive, String label) {
        int fill = destructive ? hovered ? -7459282 : -9690590 : hovered ? -12958126 : -13682107;
        int border = destructive ? -3048067 : -7429710;
        graphics.method_25294(left, top, right, bottom, fill);
        graphics.method_25294(left, top, right, top + 1, border);
        graphics.method_25294(left, bottom - 1, right, bottom, border);
        graphics.method_25294(left, top, left + 1, bottom, border);
        graphics.method_25294(right - 1, top, right, bottom, border);
        int textX = left + (((right - left) - this.field_22793.method_1727(label)) / BLACKLIST_MODAL_FILTER_ROWS);
        int textY = top + (((bottom - top) - 8) / BLACKLIST_MODAL_FILTER_ROWS);
        graphics.method_25303(this.field_22793, label, textX, textY, -1);
    }

    private boolean handleBlacklistScroll(double mouseX, double mouseY, double scrollDelta) {
        if (this.tab != Tab.BLACKLIST || this.blacklistEntryButtons.isEmpty()) {
            return false;
        }
        int viewTop = this.blacklistListStartY;
        int viewBottom = this.blacklistListStartY + this.blacklistViewportHeight;
        if (mouseX < this.blacklistHelpX || mouseX > this.blacklistHelpX + this.blacklistHelpWidth || mouseY < viewTop || mouseY > viewBottom) {
            return false;
        }
        int totalHeight = (this.blacklistEntryButtons.size() * this.blacklistEntryHeight) - ROW_SPACING;
        int maxScroll = Math.max(0, totalHeight - this.blacklistViewportHeight);
        if (maxScroll <= 0) {
            return false;
        }
        int deltaPixels = (int) Math.round(scrollDelta * ((double) this.blacklistEntryHeight));
        this.blacklistScrollOffset = clamp(this.blacklistScrollOffset - deltaPixels, 0, maxScroll);
        updateBlacklistScroll();
        return true;
    }

    private void updateBlacklistScroll() {
        int viewTop = this.blacklistListStartY;
        int viewBottom = this.blacklistListStartY + this.blacklistViewportHeight;
        for (int i = 0; i < this.blacklistEntryButtons.size(); i++) {
            int y = (this.blacklistListStartY + (i * this.blacklistEntryHeight)) - this.blacklistScrollOffset;
            class_4185 label = this.blacklistEntryButtons.get(i);
            class_4185 dual = this.blacklistDualButtons.get(i);
            class_4185 remove = this.blacklistRemoveButtons.get(i);
            label.method_46419(y);
            dual.method_46419(y);
            remove.method_46419(y);
            boolean visible = y + ROW_HEIGHT > viewTop && y < viewBottom;
            label.field_22764 = visible;
            dual.field_22764 = visible;
            remove.field_22764 = visible;
            label.field_22763 = visible;
            dual.field_22763 = visible;
            remove.field_22763 = visible;
        }
    }

    private boolean isMouseOverBlacklistScrollbar(double mouseX, double mouseY) {
        int totalHeight = (this.blacklistEntryButtons.size() * this.blacklistEntryHeight) - ROW_SPACING;
        int maxScroll = Math.max(0, totalHeight - this.blacklistViewportHeight);
        if (this.tab != Tab.BLACKLIST || maxScroll <= 0 || this.blacklistViewportHeight <= 0) {
            return false;
        }
        int barX = (this.blacklistHelpX + this.blacklistHelpWidth) - 4;
        return isMouseInside(mouseX, mouseY, barX, this.blacklistListStartY, barX + 4, this.blacklistListStartY + this.blacklistViewportHeight);
    }

    private void updateBlacklistScrollFromMouse(double mouseY) {
        int totalHeight = (this.blacklistEntryButtons.size() * this.blacklistEntryHeight) - ROW_SPACING;
        int maxScroll = Math.max(0, totalHeight - this.blacklistViewportHeight);
        if (maxScroll <= 0 || this.blacklistViewportHeight <= 0) {
            return;
        }
        int barTop = this.blacklistListStartY;
        int barBottom = this.blacklistListStartY + this.blacklistViewportHeight;
        int track = Math.max(1, barBottom - barTop);
        double ratio = (mouseY - ((double) barTop)) / ((double) track);
        this.blacklistScrollOffset = clamp((int) Math.round(ratio * ((double) maxScroll)), 0, maxScroll);
        updateBlacklistScroll();
    }

    private boolean isBlacklistDualHanded(String entry) {
        if (entry == null || entry.isBlank() || this.data.blacklistApplyDualHanded == null) {
            return false;
        }
        return Boolean.TRUE.equals(this.data.blacklistApplyDualHanded.get(entry));
    }

    private void setBlacklistDualHanded(String entry, boolean value) {
        if (entry == null || entry.isBlank()) {
            return;
        }
        if (this.data.blacklistApplyDualHanded == null) {
            this.data.blacklistApplyDualHanded = new LinkedHashMap();
        }
        this.data.blacklistApplyDualHanded.put(entry, Boolean.valueOf(value));
    }

    private class_2561 blacklistDualLabel(boolean value) {
        return class_2561.method_43471("punchy.config.blacklist.dual_handed.short").method_10852(class_2561.method_43470(": ")).method_10852(class_2561.method_43471(value ? "punchy.toggle.on" : "punchy.toggle.off"));
    }

    /* JADX WARN: Removed duplicated region for block: B:23:0x0058  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private java.lang.String blacklistEnableLabel(boolean r4) {
        /*
            r3 = this;
            java.lang.String r0 = "punchy.config.blacklist.enable"
            r1 = 0
            java.lang.Object[] r1 = new java.lang.Object[r1]
            java.lang.String r0 = net.minecraft.class_1074.method_4662(r0, r1)
            r5 = r0
            r0 = r5
            if (r0 == 0) goto L20
            r0 = r5
            boolean r0 = r0.isBlank()
            if (r0 != 0) goto L20
            r0 = r5
            java.lang.String r1 = "punchy.config.blacklist.enable"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L24
        L20:
            java.lang.String r0 = "Enable"
            r5 = r0
        L24:
            r0 = r4
            if (r0 == 0) goto L2e
            java.lang.String r0 = "punchy.toggle.on"
            goto L31
        L2e:
            java.lang.String r0 = "punchy.toggle.off"
        L31:
            r1 = 0
            java.lang.Object[] r1 = new java.lang.Object[r1]
            java.lang.String r0 = net.minecraft.class_1074.method_4662(r0, r1)
            r6 = r0
            r0 = r6
            if (r0 == 0) goto L58
            r0 = r6
            boolean r0 = r0.isBlank()
            if (r0 != 0) goto L58
            r0 = r6
            r1 = r4
            if (r1 == 0) goto L4f
            java.lang.String r1 = "punchy.toggle.on"
            goto L52
        L4f:
            java.lang.String r1 = "punchy.toggle.off"
        L52:
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L66
        L58:
            r0 = r4
            if (r0 == 0) goto L62
            java.lang.String r0 = "On"
            goto L65
        L62:
            java.lang.String r0 = "Off"
        L65:
            r6 = r0
        L66:
            r0 = r5
            r1 = r6
            java.lang.String r0 = r0 + ": " + r1
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: punchy.client.config.PunchyConfigScreen.blacklistEnableLabel(boolean):java.lang.String");
    }

    /* JADX WARN: Removed duplicated region for block: B:23:0x0057  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private java.lang.String blacklistDualShortLabel(boolean r4) {
        /*
            r3 = this;
            java.lang.String r0 = "punchy.config.blacklist.dual_handed.short"
            net.minecraft.class_5250 r0 = net.minecraft.class_2561.method_43471(r0)
            java.lang.String r0 = r0.getString()
            r5 = r0
            r0 = r5
            if (r0 == 0) goto L1f
            r0 = r5
            boolean r0 = r0.isBlank()
            if (r0 != 0) goto L1f
            r0 = r5
            java.lang.String r1 = "punchy.config.blacklist.dual_handed.short"
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L23
        L1f:
            java.lang.String r0 = "Dual"
            r5 = r0
        L23:
            r0 = r4
            if (r0 == 0) goto L2d
            java.lang.String r0 = "punchy.toggle.on"
            goto L30
        L2d:
            java.lang.String r0 = "punchy.toggle.off"
        L30:
            r1 = 0
            java.lang.Object[] r1 = new java.lang.Object[r1]
            java.lang.String r0 = net.minecraft.class_1074.method_4662(r0, r1)
            r6 = r0
            r0 = r6
            if (r0 == 0) goto L57
            r0 = r6
            boolean r0 = r0.isBlank()
            if (r0 != 0) goto L57
            r0 = r6
            r1 = r4
            if (r1 == 0) goto L4e
            java.lang.String r1 = "punchy.toggle.on"
            goto L51
        L4e:
            java.lang.String r1 = "punchy.toggle.off"
        L51:
            boolean r0 = r0.equals(r1)
            if (r0 == 0) goto L65
        L57:
            r0 = r4
            if (r0 == 0) goto L61
            java.lang.String r0 = "On"
            goto L64
        L61:
            java.lang.String r0 = "Off"
        L64:
            r6 = r0
        L65:
            r0 = r5
            r1 = r6
            java.lang.String r0 = r0 + ": " + r1
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: punchy.client.config.PunchyConfigScreen.blacklistDualShortLabel(boolean):java.lang.String");
    }

    private boolean handleAnimationScroll(double mouseX, double mouseY, double scrollDelta) {
        if (this.animationRowCount <= 0 || this.animationViewportHeight <= 0) {
            return false;
        }
        int viewTop = this.animationListStartY;
        int viewBottom = this.animationListStartY + this.animationViewportHeight;
        int viewLeft = this.dividerX + SIDEBAR_GAP;
        int viewRight = viewLeft + this.contentPanelWidth;
        if (mouseX < viewLeft || mouseX > viewRight || mouseY < viewTop || mouseY > viewBottom) {
            return false;
        }
        int totalHeight = (this.animationRowCount * this.animationEntryHeight) - ROW_SPACING;
        int maxScroll = Math.max(0, totalHeight - this.animationViewportHeight);
        if (maxScroll <= 0) {
            return false;
        }
        int deltaPixels = (int) Math.round(scrollDelta * ((double) this.animationEntryHeight));
        this.animationScrollOffset = clamp(this.animationScrollOffset - deltaPixels, 0, maxScroll);
        updateAnimationScroll();
        return true;
    }

    private boolean isMouseOverAnimationScrollbar(double mouseX, double mouseY) {
        int totalHeight = (this.animationRowCount * this.animationEntryHeight) - ROW_SPACING;
        int maxScroll = Math.max(0, totalHeight - this.animationViewportHeight);
        if (maxScroll <= 0 || this.animationViewportHeight <= 0) {
            return false;
        }
        int barX = Math.max(this.centerPanelLeft + BLACKLIST_MODAL_FILTER_ROWS, this.centerPanelRight - 7);
        return isMouseInside(mouseX, mouseY, barX, this.animationListStartY, barX + 4, this.animationListStartY + this.animationViewportHeight);
    }

    private void updateAnimationScrollFromMouse(double mouseY) {
        int totalHeight = (this.animationRowCount * this.animationEntryHeight) - ROW_SPACING;
        int maxScroll = Math.max(0, totalHeight - this.animationViewportHeight);
        if (maxScroll <= 0 || this.animationViewportHeight <= 0) {
            return;
        }
        int barTop = this.animationListStartY;
        int barBottom = this.animationListStartY + this.animationViewportHeight;
        int track = Math.max(1, barBottom - barTop);
        double ratio = (mouseY - ((double) barTop)) / ((double) track);
        this.animationScrollOffset = clamp((int) Math.round(ratio * ((double) maxScroll)), 0, maxScroll);
        updateAnimationScroll();
    }

    private void updateAnimationScroll() {
        int viewTop = this.animationListStartY;
        int viewBottom = this.animationListStartY + this.animationViewportHeight;
        for (int i = 0; i < this.animationWidgets.size(); i++) {
            class_339 widget = this.animationWidgets.get(i);
            int baseY = this.animationWidgetBaseY.get(i).intValue();
            int y = baseY - this.animationScrollOffset;
            widget.method_46419(y);
            int widgetHeight = Math.max(ROW_HEIGHT, widget.method_25364());
            boolean visible = y >= viewTop && y < viewBottom && y + widgetHeight <= viewBottom;
            widget.field_22764 = visible;
            widget.field_22763 = visible;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<class_5481> splitHelpText(String text, int maxChars) {
        List<class_5481> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() > maxChars) {
                lines.add(class_5481.method_30747(line.toString(), class_2583.field_24360));
                line.setLength(0);
                line.append(word);
            } else {
                line.append(' ').append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(class_5481.method_30747(line.toString(), class_2583.field_24360));
        }
        return lines;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$BlacklistCatalogEntry.class */
    private static final class BlacklistCatalogEntry {
        private final String id;
        private final String namespace;
        private final String displayName;
        private final String searchName;
        private final class_1799 icon;

        private BlacklistCatalogEntry(String id, String namespace, String displayName, String searchName, class_1799 icon) {
            this.id = id;
            this.namespace = namespace;
            this.displayName = displayName;
            this.searchName = searchName;
            this.icon = icon;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$BlacklistModFilter.class */
    private static final class BlacklistModFilter {
        private final String namespace;
        private final String displayName;

        private BlacklistModFilter(String namespace, String displayName) {
            this.namespace = namespace;
            this.displayName = displayName;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$BlacklistHitbox.class */
    private static final class BlacklistHitbox {
        private final int left;
        private final int top;
        private final int right;
        private final int bottom;
        private final String key;

        private BlacklistHitbox(int left, int top, int right, int bottom, String key) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.key = key;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$StyledConfigSlider.class */
    private static abstract class StyledConfigSlider extends class_357 {
        protected abstract String sliderLabel();

        protected abstract String sliderValueText();

        protected StyledConfigSlider(int x, int y, int width, int height, class_2561 message, double value) {
            super(x, y, width, height, message, value);
        }

        protected String sliderReservedValueText() {
            return sliderValueText();
        }

        protected void drawStyled(class_332 graphics, int mouseX, int mouseY) {
            int fill;
            int border;
            int textColor;
            class_310 mc = class_310.method_1551();
            if (!this.field_22764 || mc == null || mc.field_1772 == null) {
                return;
            }
            int left = method_46426();
            int top = method_46427();
            int right = left + method_25368();
            int bottom = top + method_25364();
            boolean hovered = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
            boolean focused = method_25370();
            String label = sliderLabel();
            String valueText = sliderValueText();
            String reservedValueText = sliderReservedValueText();
            int textY = top + ((method_25364() - 8) / PunchyConfigScreen.BLACKLIST_MODAL_FILTER_ROWS);
            int labelX = left + PunchyConfigScreen.RESET_GAP;
            int valueX = (right - PunchyConfigScreen.RESET_GAP) - mc.field_1772.method_1727(valueText);
            int reservedValueX = (right - PunchyConfigScreen.RESET_GAP) - mc.field_1772.method_1727(reservedValueText);
            int maxLabelWidth = Math.max(PunchyConfigScreen.RESET_GAP, (reservedValueX - labelX) - PunchyConfigScreen.SIDEBAR_GAP);
            String clippedLabel = mc.field_1772.method_27523(label, maxLabelWidth);
            int trackLeft = labelX + mc.field_1772.method_1727(clippedLabel) + PunchyConfigScreen.RESET_GAP;
            int trackRight = reservedValueX - 8;
            if (trackRight <= trackLeft + PunchyConfigScreen.MAIN_PANEL_GAP) {
                trackLeft = left + 8;
                trackRight = right - 8;
            }
            int trackWidth = Math.max(1, trackRight - trackLeft);
            if (!this.field_22763) {
                fill = -14801620;
                border = -12694955;
                textColor = -8483429;
            } else if (focused || hovered) {
                fill = -12958126;
                border = -3048067;
                textColor = -1;
            } else {
                fill = -13682107;
                border = -7429710;
                textColor = -1;
            }
            graphics.method_25294(left, top, right, bottom, fill);
            graphics.method_25294(left, top, right, top + 1, border);
            graphics.method_25294(left, bottom - 1, right, bottom, border);
            graphics.method_25294(left, top, left + 1, bottom, border);
            graphics.method_25294(right - 1, top, right, bottom, border);
            int trackTop = (top + (method_25364() / PunchyConfigScreen.BLACKLIST_MODAL_FILTER_ROWS)) - PunchyConfigScreen.BLACKLIST_MODAL_FILTER_ROWS;
            int trackBottom = trackTop + 4;
            graphics.method_25294(trackLeft, trackTop, trackRight, trackBottom, -1441719774);
            int knobTravel = Math.max(0, trackWidth - 8);
            int knobLeft = trackLeft + ((int) Math.round(this.field_22753 * ((double) knobTravel)));
            int knobRight = knobLeft + 8;
            int fillRight = knobLeft + (8 / PunchyConfigScreen.BLACKLIST_MODAL_FILTER_ROWS);
            graphics.method_25294(trackLeft, trackTop, fillRight, trackBottom, -7459282);
            int knobFill = !this.field_22763 ? -11641751 : (focused || hovered) ? -3048067 : -7709604;
            graphics.method_25294(knobLeft, top + PunchyConfigScreen.BLACKLIST_MODAL_FILTER_ROWS, knobRight, bottom - PunchyConfigScreen.BLACKLIST_MODAL_FILTER_ROWS, knobFill);
            graphics.method_25294(knobLeft, top + PunchyConfigScreen.BLACKLIST_MODAL_FILTER_ROWS, knobRight, top + 3, -4732456);
            graphics.method_25294(knobLeft, bottom - 3, knobRight, bottom - PunchyConfigScreen.BLACKLIST_MODAL_FILTER_ROWS, -14404542);
            graphics.method_25303(mc.field_1772, clippedLabel, labelX, textY, textColor);
            graphics.method_25303(mc.field_1772, valueText, valueX, textY, textColor);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$AnimationSpeedSlider.class */
    private static final class AnimationSpeedSlider extends StyledConfigSlider {
        private final PunchyConfig.Data data;

        private AnimationSpeedSlider(int x, int y, int width, int height, PunchyConfig.Data data) {
            super(x, y, width, height, class_2561.method_43473(), toSliderValue(data.animationSpeed));
            this.data = data;
            method_25346();
        }

        protected void method_25346() {
            float speed = sliderToSpeed(this.field_22753);
            String formatted = String.format(Locale.ROOT, "%.1f", Float.valueOf(speed));
            method_25355(class_2561.method_43471("punchy.config.option.animation_speed").method_10852(class_2561.method_43470(": ")).method_10852(class_2561.method_43470(formatted)));
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderLabel() {
            return class_2561.method_43471("punchy.config.option.animation_speed").getString();
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderValueText() {
            return String.format(Locale.ROOT, "%.1f", Float.valueOf(sliderToSpeed(this.field_22753)));
        }

        protected void method_25344() {
            float speed = Math.round(sliderToSpeed(this.field_22753) * 10.0f) / 10.0f;
            this.data.animationSpeed = speed;
            PunchyAnimationManager.setAnimationSpeed(speed);
        }

        private void setSpeed(float speed) {
            this.field_22753 = toSliderValue(speed);
            method_25346();
        }

        private static double toSliderValue(float speed) {
            return (speed - 0.5f) / (7.0f - 0.5f);
        }

        private static float sliderToSpeed(double sliderValue) {
            float clamped = (float) Math.min(1.0d, Math.max(0.0d, sliderValue));
            return 0.5f + ((7.0f - 0.5f) * clamped);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$FirstPersonHidePitchSlider.class */
    private static final class FirstPersonHidePitchSlider extends StyledConfigSlider {
        private final PunchyConfig.Data data;

        private FirstPersonHidePitchSlider(int x, int y, int width, int height, PunchyConfig.Data data) {
            super(x, y, width, height, class_2561.method_43473(), toSliderValue(data.firstPersonModelHidePitch));
            this.data = data;
            method_25346();
        }

        protected void method_25346() {
            float pitch = sliderToPitch(this.field_22753);
            String formatted = valueText(pitch);
            method_25355(class_2561.method_43471("punchy.config.option.firstperson_hide_pitch").method_10852(class_2561.method_43470(": ")).method_10852(class_2561.method_43470(formatted)));
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderLabel() {
            return class_2561.method_43471("punchy.config.option.firstperson_hide_pitch").getString();
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderValueText() {
            return valueText(sliderToPitch(this.field_22753));
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderReservedValueText() {
            return "never";
        }

        protected void method_25344() {
            float pitch = sliderToPitch(this.field_22753);
            this.data.firstPersonModelHidePitch = Math.round(pitch);
        }

        private static String valueText(float pitch) {
            return pitch > 85.0f ? "never" : String.format(Locale.ROOT, "%.0f", Float.valueOf(pitch));
        }

        private void setPitch(float pitch) {
            this.field_22753 = toSliderValue(pitch);
            method_25346();
        }

        private static double toSliderValue(float pitch) {
            return (pitch - 0.0f) / (90.0f - 0.0f);
        }

        private static float sliderToPitch(double sliderValue) {
            float clamped = (float) Math.min(1.0d, Math.max(0.0d, sliderValue));
            return 0.0f + ((90.0f - 0.0f) * clamped);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$SprintArmSwingIntensitySlider.class */
    private static final class SprintArmSwingIntensitySlider extends StyledConfigSlider {
        private final PunchyConfig.Data data;

        private SprintArmSwingIntensitySlider(int x, int y, int width, int height, PunchyConfig.Data data) {
            super(x, y, width, height, class_2561.method_43473(), toSliderValue(data.sprintArmSwingIntensity));
            this.data = data;
            method_25346();
        }

        protected void method_25346() {
            float intensity = sliderToIntensity(this.field_22753);
            int percent = Math.round(intensity * 100.0f);
            method_25355(class_2561.method_43471("punchy.config.option.sprint_arm_swing_intensity").method_10852(class_2561.method_43470(": ")).method_10852(class_2561.method_43470(percent + "%")));
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderLabel() {
            return class_2561.method_43471("punchy.config.option.sprint_arm_swing_intensity").getString();
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderValueText() {
            int percent = Math.round(sliderToIntensity(this.field_22753) * 100.0f);
            return percent + "%";
        }

        protected void method_25344() {
            float intensity = sliderToIntensity(this.field_22753);
            this.data.sprintArmSwingIntensity = Math.round(intensity * 100.0f) / 100.0f;
            method_25346();
        }

        private void setIntensity(float intensity) {
            this.field_22753 = toSliderValue(intensity);
            method_25346();
        }

        private static double toSliderValue(float intensity) {
            return (intensity - (-1.0f)) / (1.0f - (-1.0f));
        }

        private static float sliderToIntensity(double sliderValue) {
            float clamped = (float) Math.min(1.0d, Math.max(0.0d, sliderValue));
            return (-1.0f) + ((1.0f - (-1.0f)) * clamped);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/PunchyConfigScreen$SprintArmSwingSpeedSlider.class */
    private static final class SprintArmSwingSpeedSlider extends StyledConfigSlider {
        private final PunchyConfig.Data data;

        private SprintArmSwingSpeedSlider(int x, int y, int width, int height, PunchyConfig.Data data) {
            super(x, y, width, height, class_2561.method_43473(), toSliderValue(data.sprintArmSwingSpeed));
            this.data = data;
            method_25346();
        }

        protected void method_25346() {
            float speed = sliderToSpeed(this.field_22753);
            int percent = Math.round(speed * 100.0f);
            class_5250 class_5250VarMethod_43471 = class_2561.method_43471("punchy.config.option.sprint_arm_swing_speed");
            method_25355(class_5250VarMethod_43471.method_27661().method_27693(": ").method_10852(class_2561.method_43470(percent + "%")));
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderLabel() {
            return class_2561.method_43471("punchy.config.option.sprint_arm_swing_speed").getString();
        }

        @Override // punchy.client.config.PunchyConfigScreen.StyledConfigSlider
        protected String sliderValueText() {
            int percent = Math.round(sliderToSpeed(this.field_22753) * 100.0f);
            return percent + "%";
        }

        protected void method_25344() {
            float speed = sliderToSpeed(this.field_22753);
            this.data.sprintArmSwingSpeed = speed;
        }

        private void setSpeed(float speed) {
            this.field_22753 = toSliderValue(speed);
            method_25346();
        }

        private static double toSliderValue(float speed) {
            return class_3532.method_15350((speed - (-1.0f)) / 2.0f, 0.0d, 1.0d);
        }

        private static float sliderToSpeed(double sliderValue) {
            float value = (float) sliderValue;
            return (-1.0f) + (value * 2.0f);
        }
    }
}
