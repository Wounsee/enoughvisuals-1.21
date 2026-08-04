package punchy.client.tuning;

import java.awt.Desktop;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_156;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_410;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_7923;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.io.PunchyAnimationLoader;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/MixThePacksScreen.class */
public final class MixThePacksScreen extends class_437 {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 4;
    private static final int ROW_STEP = 24;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 6;
    private static final int SCROLLBAR_RESERVE = 10;
    private static final int EXPANDED_CHILD_INSET = 10;
    private static final int MAX_PROFILE_ROWS = 6;
    private static final int PROFILE_ROW_HEIGHT = 14;
    private static final int PANEL_GAP = 12;
    private final class_437 parent;
    private final Set<String> expandedSlots;
    private final List<RowEntry> rows;
    private final List<Integer> rowBaseY;
    private final List<class_339> rowWidgets;
    private final List<class_4185> profileRowButtons;
    private List<String> profiles;
    private class_4185 doneButton;
    private class_4185 resetOrderButton;
    private class_342 filterBox;
    private class_342 profileNameBox;
    private class_4185 saveProfileButton;
    private class_4185 loadProfileButton;
    private class_4185 deleteProfileButton;
    private class_4185 openProfilesButton;
    private int selectedProfile;
    private int profileListScroll;
    private String statusMessage;
    private int scrollOffset;
    private int maxScroll;
    private int listLeft;
    private int listRight;
    private int listTop;
    private int listBottom;
    private int containerLeft;
    private int containerTop;
    private int containerRight;
    private int containerBottom;
    private int selectorWidth;
    private int rowWidth;
    private int profilePanelX;
    private int profilePanelW;
    private int listTitleX;
    private int listTitleY;
    private int profilePanelTop;
    private int profilesTitleX;
    private int profilesTitleY;
    private int profileListX;
    private int profileListY;
    private int profileListW;
    private int profileListH;
    private int profileVisibleRows;
    private int profilePanelScroll;
    private int profilePanelMaxScroll;
    private int footerRowY;
    private boolean compactLayout;
    private String filterText;
    private boolean hasUnsavedProfileChanges;
    private PunchyConfig.Data mixSessionBaseline;
    private boolean initializedOnce;
    private final Map<String, String> stagedPackSelections;
    private final Map<String, String> stagedPackTypeSelections;
    private final Map<String, String> previewPackSelections;
    private final Map<String, String> previewPackTypeSelections;
    private boolean deleteProfileConfirmOpen;
    private String pendingDeleteProfileName;
    private int deleteModalLeft;
    private int deleteModalTop;
    private int deleteModalRight;
    private int deleteModalBottom;
    private int deleteConfirmButtonLeft;
    private int deleteConfirmButtonTop;
    private int deleteConfirmButtonRight;
    private int deleteConfirmButtonBottom;
    private int deleteCancelButtonLeft;
    private int deleteCancelButtonTop;
    private int deleteCancelButtonRight;
    private int deleteCancelButtonBottom;

    public MixThePacksScreen(class_437 parent) {
        super(class_2561.method_43471("punchy.mixpacks.title"));
        this.expandedSlots = new HashSet();
        this.rows = new ArrayList();
        this.rowBaseY = new ArrayList();
        this.rowWidgets = new ArrayList();
        this.profileRowButtons = new ArrayList();
        this.profiles = List.of();
        this.selectedProfile = -1;
        this.profileListScroll = 0;
        this.statusMessage = "";
        this.scrollOffset = 0;
        this.maxScroll = 0;
        this.listLeft = 0;
        this.listRight = 0;
        this.listTop = 0;
        this.listBottom = 0;
        this.containerLeft = 0;
        this.containerTop = 0;
        this.containerRight = 0;
        this.containerBottom = 0;
        this.selectorWidth = 0;
        this.rowWidth = 0;
        this.profilePanelX = 0;
        this.profilePanelW = 0;
        this.listTitleX = 0;
        this.listTitleY = 0;
        this.profilePanelTop = 0;
        this.profilesTitleX = 0;
        this.profilesTitleY = 0;
        this.profileListX = 0;
        this.profileListY = 0;
        this.profileListW = 0;
        this.profileListH = 0;
        this.profileVisibleRows = 6;
        this.profilePanelScroll = 0;
        this.profilePanelMaxScroll = 0;
        this.footerRowY = 0;
        this.compactLayout = false;
        this.filterText = "";
        this.hasUnsavedProfileChanges = false;
        this.mixSessionBaseline = null;
        this.initializedOnce = false;
        this.stagedPackSelections = new LinkedHashMap();
        this.stagedPackTypeSelections = new LinkedHashMap();
        this.previewPackSelections = new LinkedHashMap();
        this.previewPackTypeSelections = new LinkedHashMap();
        this.deleteProfileConfirmOpen = false;
        this.pendingDeleteProfileName = null;
        this.deleteModalLeft = 0;
        this.deleteModalTop = 0;
        this.deleteModalRight = 0;
        this.deleteModalBottom = 0;
        this.deleteConfirmButtonLeft = 0;
        this.deleteConfirmButtonTop = 0;
        this.deleteConfirmButtonRight = 0;
        this.deleteConfirmButtonBottom = 0;
        this.deleteCancelButtonLeft = 0;
        this.deleteCancelButtonTop = 0;
        this.deleteCancelButtonRight = 0;
        this.deleteCancelButtonBottom = 0;
        this.parent = parent;
    }

    protected void method_25426() {
        method_37067();
        this.rows.clear();
        this.rowBaseY.clear();
        this.rowWidgets.clear();
        this.profileRowButtons.clear();
        this.scrollOffset = 0;
        boolean preserveStagedChanges = this.initializedOnce && this.hasUnsavedProfileChanges;
        if (!preserveStagedChanges) {
            this.mixSessionBaseline = PunchyConfig.snapshot();
            this.hasUnsavedProfileChanges = false;
            initializeStagedSelectionsFromCurrent();
        }
        this.initializedOnce = true;
        int safeWidth = Math.max(420, this.field_22789 - (ROW_STEP * 2));
        int safeHeight = Math.max(300, this.field_22790 - ROW_STEP);
        int containerWidth = Math.min(760, safeWidth);
        int containerHeight = Math.min(266, safeHeight);
        int containerWidth2 = Math.min(containerWidth, Math.max(1, this.field_22789 - 8));
        int containerHeight2 = Math.min(containerHeight, Math.max(1, this.field_22790 - 8));
        this.containerLeft = (this.field_22789 - containerWidth2) / 2;
        this.containerTop = (this.field_22790 - containerHeight2) / 2;
        this.containerRight = this.containerLeft + containerWidth2;
        this.containerBottom = this.containerTop + containerHeight2;
        this.footerRowY = (this.containerBottom - PROFILE_ROW_HEIGHT) - ROW_HEIGHT;
        this.profilePanelW = class_3532.method_15340(containerWidth2 / 3, 220, 300);
        int contentTop = this.containerTop + PANEL_GAP;
        int contentBottom = this.footerRowY - 8;
        this.listLeft = this.containerLeft + PROFILE_ROW_HEIGHT;
        int leftAreaWidth = (containerWidth2 - this.profilePanelW) - PANEL_GAP;
        this.compactLayout = leftAreaWidth < 300 || contentBottom - contentTop < 210;
        if (this.compactLayout) {
            this.profilePanelX = this.containerLeft + PROFILE_ROW_HEIGHT;
            this.profilePanelW = containerWidth2 - (PROFILE_ROW_HEIGHT * 2);
            this.listRight = this.containerRight - PROFILE_ROW_HEIGHT;
            this.listTop = contentTop;
            this.profilePanelTop = Math.max(contentTop + 120, contentBottom - 210);
            this.listBottom = this.profilePanelTop - 8;
            if (this.listBottom < this.listTop + 80) {
                this.listBottom = this.listTop + 80;
                this.profilePanelTop = this.listBottom + 8;
            }
        } else {
            this.profilePanelX = (this.containerRight - PROFILE_ROW_HEIGHT) - this.profilePanelW;
            this.profilePanelTop = contentTop;
            this.listRight = this.profilePanelX - PANEL_GAP;
            this.listTop = contentTop;
            this.listBottom = contentBottom;
        }
        this.selectorWidth = Math.max(92, Math.min(150, Math.max(96, (this.listRight - this.listLeft) / 3)));
        this.rowWidth = Math.max(100, (((this.listRight - this.listLeft) - this.selectorWidth) - 6) - 10);
        this.listTitleX = this.listLeft;
        this.listTitleY = this.listTop - PROFILE_ROW_HEIGHT;
        this.filterBox = method_37063(new class_342(this.field_22793, this.listLeft, this.listTop, this.rowWidth + 6 + this.selectorWidth, ROW_HEIGHT, class_2561.method_43471("punchy.mixpacks.search")));
        this.filterBox.method_1880(80);
        this.filterBox.method_1852(this.filterText == null ? "" : this.filterText);
        this.filterBox.method_47404(class_2561.method_43471("punchy.mixpacks.search"));
        this.filterBox.method_1863(value -> {
            this.filterText = value == null ? "" : value;
            rebuildRows();
        });
        this.listTop += 26;
        this.resetOrderButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.mixpacks.reset_resourcepack_order"), button -> {
            applyResetToResourcePackOrderAndUnloadProfile();
        }).method_46433(0, 0).method_46437(220, ROW_HEIGHT).method_46431());
        this.doneButton = method_37063(class_4185.method_46430(class_2561.method_43471("gui.done"), button2 -> {
            method_25419();
        }).method_46433(0, 0).method_46437(this.profilePanelW, ROW_HEIGHT).method_46431());
        initProfilesPanel();
        if (this.compactLayout) {
            int halfW = (this.profilePanelW - 4) / 2;
            this.resetOrderButton.method_46421(this.profilePanelX);
            this.resetOrderButton.method_46419(this.footerRowY);
            this.resetOrderButton.method_25358(halfW);
            this.doneButton.method_46421(this.profilePanelX + halfW + 4);
            this.doneButton.method_46419(this.footerRowY);
            this.doneButton.method_25358((this.profilePanelW - halfW) - 4);
        } else {
            this.resetOrderButton.method_46421(this.listLeft);
            this.resetOrderButton.method_46419(this.footerRowY);
            this.resetOrderButton.method_25358(this.listRight - this.listLeft);
            this.doneButton.method_46421(this.profilePanelX);
            this.doneButton.method_46419(this.footerRowY);
            this.doneButton.method_25358(this.profilePanelW);
        }
        recalcProfilePanelScroll();
        refreshProfiles(true);
        rebuildRows();
        recalcDeleteConfirmLayout();
    }

    private void initProfilesPanel() {
        this.profilesTitleX = this.profilePanelX;
        this.profilesTitleY = this.profilePanelTop;
        this.profileListX = this.profilePanelX;
        this.profileListY = this.profilePanelTop;
        this.profileListW = this.profilePanelW;
        int openProfilesY = this.footerRowY - ROW_STEP;
        int profileBottomLimit = openProfilesY - 8;
        int maxRowsFit = ((profileBottomLimit - this.profilePanelTop) - 66) / PROFILE_ROW_HEIGHT;
        this.profileVisibleRows = class_3532.method_15340(maxRowsFit, 2, 6);
        this.profileListH = this.profileVisibleRows * PROFILE_ROW_HEIGHT;
        this.profileNameBox = method_37063(new class_342(this.field_22793, this.profilePanelX, this.profileListY + this.profileListH + 6, this.profilePanelW, ROW_HEIGHT, class_2561.method_43471("punchy.tuning.ui.profile_name")));
        this.profileNameBox.method_1880(64);
        int halfW = (this.profilePanelW - 4) / 2;
        this.saveProfileButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.save_profile"), button -> {
            String string;
            String name = this.profileNameBox.method_1882();
            if (name == null || name.isBlank()) {
                name = "mix_profile";
                this.profileNameBox.method_1852(name);
            }
            PunchyConfig.Data previous = PunchyConfig.snapshot();
            PunchyConfig.replaceAnimationPackSelections(this.stagedPackSelections, this.stagedPackTypeSelections, previous.mixSpecificForceOverride);
            boolean ok = MixProfileManager.saveCurrentAs(name);
            if (!ok) {
                PunchyConfig.replaceAnimationPackSelections(previous.animationPackSelections, previous.animationPackTypeSelections, previous.mixSpecificForceOverride);
            } else {
                PunchyAnimationManager.requestTuningReload();
            }
            if (ok) {
                string = class_2561.method_43469("punchy.tuning.ui.status.profile_saved", new Object[]{name}).getString();
            } else {
                string = class_2561.method_43471("punchy.tuning.ui.status.profile_save_failed").getString();
            }
            this.statusMessage = string;
            if (ok) {
                this.hasUnsavedProfileChanges = false;
                this.mixSessionBaseline = PunchyConfig.snapshot();
                initializeStagedSelectionsFromCurrent();
            }
            refreshProfiles(true);
            rebuildRows();
        }).method_46433(this.profilePanelX, this.profileNameBox.method_46427() + ROW_STEP).method_46437(halfW, ROW_HEIGHT).method_46431());
        this.loadProfileButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.load_profile"), button2 -> {
            String string;
            String name = selectedProfileName();
            boolean ok = name != null && MixProfileManager.loadProfile(name);
            if (ok) {
                string = class_2561.method_43469("punchy.tuning.ui.status.profile_loaded", new Object[]{name}).getString();
            } else {
                string = class_2561.method_43471("punchy.tuning.ui.status.profile_load_failed").getString();
            }
            this.statusMessage = string;
            if (ok) {
                this.hasUnsavedProfileChanges = false;
                this.mixSessionBaseline = PunchyConfig.snapshot();
                initializeStagedSelectionsFromCurrent();
            }
            refreshProfiles(false);
            rebuildRows();
        }).method_46433(this.profilePanelX + halfW + 4, this.profileNameBox.method_46427() + ROW_STEP).method_46437((this.profilePanelW - halfW) - 4, ROW_HEIGHT).method_46431());
        this.deleteProfileButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.delete_profile"), button3 -> {
            openDeleteProfileConfirm();
        }).method_46433(this.profilePanelX, this.profileNameBox.method_46427() + 46).method_46437(this.profilePanelW, ROW_HEIGHT).method_46431());
        this.openProfilesButton = method_37063(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.open_profiles_folder"), button4 -> {
            String string;
            boolean ok = openProfilesFolder();
            if (ok) {
                string = class_2561.method_43471("punchy.tuning.ui.status.profiles_folder_opened").getString();
            } else {
                string = class_2561.method_43471("punchy.tuning.ui.status.profiles_folder_open_failed").getString();
            }
            this.statusMessage = string;
        }).method_46433(this.profilePanelX, openProfilesY).method_46437(this.profilePanelW, ROW_HEIGHT).method_46431());
        for (int i = 0; i < this.profileVisibleRows; i++) {
            int row = i;
            class_4185 rowButton = method_37063(class_4185.method_46430(class_2561.method_43473(), button5 -> {
                selectProfileAtVisibleRow(row);
            }).method_46433(this.profileListX, this.profileListY + (i * PROFILE_ROW_HEIGHT)).method_46437(this.profileListW, 13).method_46431());
            this.profileRowButtons.add(rowButton);
        }
    }

    public boolean method_25421() {
        return false;
    }

    public void method_25394(class_332 graphics, int mouseX, int mouseY, float partialTick) {
        graphics.method_25294(0, 0, this.field_22789, this.field_22790, -1610612736);
        graphics.method_25294(this.containerLeft, this.containerTop, this.containerRight, this.containerBottom, -15657446);
        graphics.method_25294(this.containerLeft, this.containerTop, this.containerRight, this.containerTop + 1, -14011848);
        graphics.method_25294(this.containerLeft, this.containerBottom - 1, this.containerRight, this.containerBottom, -14011848);
        graphics.method_25294(this.containerLeft, this.containerTop, this.containerLeft + 1, this.containerBottom, -14011848);
        graphics.method_25294(this.containerRight - 1, this.containerTop, this.containerRight, this.containerBottom, -14011848);
        int sectionTop = (this.filterBox != null ? this.filterBox.method_46427() : this.listTop) - 6;
        int leftPanelLeft = this.listLeft - 4;
        int leftPanelRight = this.listRight + 4;
        int leftPanelBottom = this.resetOrderButton.method_46427() + this.resetOrderButton.method_25364() + 6;
        graphics.method_25294(leftPanelLeft, sectionTop, leftPanelRight, leftPanelBottom, 857944122);
        graphics.method_25294(leftPanelLeft, sectionTop, leftPanelRight, sectionTop + 1, 1430145621);
        graphics.method_25294(leftPanelLeft, leftPanelBottom - 1, leftPanelRight, leftPanelBottom, 1430145621);
        graphics.method_25294(leftPanelLeft, sectionTop, leftPanelLeft + 1, leftPanelBottom, 1430145621);
        graphics.method_25294(leftPanelRight - 1, sectionTop, leftPanelRight, leftPanelBottom, 1430145621);
        int rightPanelLeft = this.profilePanelX - 4;
        int rightPanelRight = this.profilePanelX + this.profilePanelW + 4;
        int rightPanelBottom = this.doneButton.method_46427() + this.doneButton.method_25364() + 6;
        graphics.method_25294(rightPanelLeft, sectionTop, rightPanelRight, rightPanelBottom, 857944122);
        graphics.method_25294(rightPanelLeft, sectionTop, rightPanelRight, sectionTop + 1, 1430145621);
        graphics.method_25294(rightPanelLeft, rightPanelBottom - 1, rightPanelRight, rightPanelBottom, 1430145621);
        graphics.method_25294(rightPanelLeft, sectionTop, rightPanelLeft + 1, rightPanelBottom, 1430145621);
        graphics.method_25294(rightPanelRight - 1, sectionTop, rightPanelRight, rightPanelBottom, 1430145621);
        graphics.method_25294(this.profileListX, this.profileListY, this.profileListX + this.profileListW, this.profileListY + this.profileListH, 1426985752);
        drawExpandedTypeGroups(graphics);
        List<class_4185> styledButtons = new ArrayList<>();
        List<class_2561> originalMessages = new ArrayList<>();
        for (class_4185 class_4185Var : method_25396()) {
            if (class_4185Var instanceof class_4185) {
                class_4185 button = class_4185Var;
                if (button.field_22764) {
                    styledButtons.add(button);
                    originalMessages.add(button.method_25369());
                    button.method_25355(class_2561.method_43473());
                }
            }
        }
        super.method_25394(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < styledButtons.size(); i++) {
            styledButtons.get(i).method_25355(originalMessages.get(i));
        }
        graphics.method_25303(this.field_22793, this.statusMessage == null ? "" : this.statusMessage, this.profilesTitleX, this.openProfilesButton.method_46427() - PANEL_GAP, 11075504);
        drawStyledButtons(graphics, mouseX, mouseY);
        drawRowIcons(graphics);
        drawScrollbar(graphics);
        drawDeleteProfileConfirmOverlay(graphics, mouseX, mouseY);
    }

    private void drawStyledButtons(class_332 graphics, int mouseX, int mouseY) {
        for (class_339 widget : this.rowWidgets) {
            if (widget instanceof class_4185) {
                class_4185 button = (class_4185) widget;
                drawBlacklistButtonStyle(graphics, button, mouseX, mouseY, false);
            }
        }
        for (class_4185 rowButton : this.profileRowButtons) {
            drawBlacklistButtonStyle(graphics, rowButton, mouseX, mouseY, false);
        }
        drawBlacklistButtonStyle(graphics, this.resetOrderButton, mouseX, mouseY, false);
        drawBlacklistButtonStyle(graphics, this.doneButton, mouseX, mouseY, false);
        drawBlacklistButtonStyle(graphics, this.saveProfileButton, mouseX, mouseY, false);
        drawBlacklistButtonStyle(graphics, this.loadProfileButton, mouseX, mouseY, false);
        drawBlacklistButtonStyle(graphics, this.deleteProfileButton, mouseX, mouseY, true);
        drawBlacklistButtonStyle(graphics, this.openProfilesButton, mouseX, mouseY, false);
    }

    private void drawBlacklistButtonStyle(class_332 graphics, class_4185 button, int mouseX, int mouseY, boolean destructive) {
        int fill;
        int border;
        int textColor;
        if (button == null || !button.field_22764) {
            return;
        }
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
        int maxTextWidth = Math.max(8, button.method_25368() - PANEL_GAP);
        String renderLabel = resolveScrollableButtonLabel(label, maxTextWidth, hovered);
        int textX = left + ((button.method_25368() - this.field_22793.method_1727(renderLabel)) / 2);
        int textY = top + ((button.method_25364() - 8) / 2);
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

    private void drawExpandedTypeGroups(class_332 graphics) {
        if (this.rows.isEmpty()) {
            return;
        }
        int contentLeft = this.listLeft;
        int contentRight = this.listLeft + this.rowWidth + 6 + this.selectorWidth;
        int i = 0;
        while (i < this.rows.size()) {
            RowEntry row = this.rows.get(i);
            if (!row.child()) {
                i++;
            } else {
                String slotId = row.slotId();
                int start = i;
                int end = i;
                while (end + 1 < this.rows.size()) {
                    RowEntry next = this.rows.get(end + 1);
                    if (!next.child() || !slotId.equals(next.slotId())) {
                        break;
                    } else {
                        end++;
                    }
                }
                int startY = this.rowBaseY.get(start).intValue() - this.scrollOffset;
                int endY = (this.rowBaseY.get(end).intValue() - this.scrollOffset) + ROW_HEIGHT;
                int boxLeft = contentLeft - 0;
                int boxRight = contentRight + 0;
                int boxTop = Math.max(this.listTop, startY - 3);
                int boxBottom = Math.min(this.listBottom, endY + 3);
                if (boxBottom > boxTop) {
                    graphics.method_25294(boxLeft, boxTop, boxRight, boxBottom, 856890146);
                    graphics.method_25294(boxLeft, boxTop, boxRight, boxTop + 1, -1439020458);
                    graphics.method_25294(boxLeft, boxBottom - 1, boxRight, boxBottom, -1439020458);
                    graphics.method_25294(boxLeft, boxTop, boxLeft + 1, boxBottom, -1439020458);
                    graphics.method_25294(boxRight - 1, boxTop, boxRight, boxBottom, -1439020458);
                }
                i = end + 1;
            }
        }
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (this.deleteProfileConfirmOpen) {
            recalcDeleteConfirmLayout();
            if (button == 0 || button == 0) {
                if (isMouseInside(mouseX, mouseY, this.deleteConfirmButtonLeft, this.deleteConfirmButtonTop, this.deleteConfirmButtonRight, this.deleteConfirmButtonBottom)) {
                    confirmDeleteProfile();
                    return true;
                }
                if (isMouseInside(mouseX, mouseY, this.deleteCancelButtonLeft, this.deleteCancelButtonTop, this.deleteCancelButtonRight, this.deleteCancelButtonBottom)) {
                    cancelDeleteProfileConfirm();
                    return true;
                }
                if (!isMouseInside(mouseX, mouseY, this.deleteModalLeft, this.deleteModalTop, this.deleteModalRight, this.deleteModalBottom)) {
                    cancelDeleteProfileConfirm();
                    return true;
                }
                return true;
            }
            return true;
        }
        if (button == 0 && isMouseOverList(mouseX, mouseY) && isMouseOverScrollBar(mouseX, mouseY)) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.method_25402(mouseX, mouseY, button);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.deleteProfileConfirmOpen) {
            return true;
        }
        if (verticalAmount == 0.0d) {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (isMouseOverProfileList(mouseX, mouseY)) {
            int maxListScroll = Math.max(0, this.profiles.size() - this.profileVisibleRows);
            this.profileListScroll = class_3532.method_15340(this.profileListScroll - ((int) Math.signum(verticalAmount)), 0, maxListScroll);
            updateProfileRowButtons();
            return true;
        }
        if (isMouseOverProfilePanel(mouseX, mouseY) && this.profilePanelMaxScroll > 0) {
            int delta = (int) Math.round(verticalAmount * 16.0d);
            setProfilePanelScroll(class_3532.method_15340(this.profilePanelScroll - delta, 0, this.profilePanelMaxScroll));
            return true;
        }
        if (!isMouseOverList(mouseX, mouseY) || this.maxScroll <= 0) {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int delta2 = (int) Math.round(verticalAmount * 24.0d);
        this.scrollOffset = class_3532.method_15340(this.scrollOffset - delta2, 0, this.maxScroll);
        updateRowVisibility();
        return true;
    }

    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (this.deleteProfileConfirmOpen) {
            if (keyCode == 256) {
                cancelDeleteProfileConfirm();
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                confirmDeleteProfile();
                return true;
            }
            return true;
        }
        if (keyCode == 299) {
            method_25419();
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    public void method_25419() {
        if (this.field_22787 == null) {
            return;
        }
        if (this.deleteProfileConfirmOpen) {
            cancelDeleteProfileConfirm();
        } else if (!this.hasUnsavedProfileChanges) {
            this.field_22787.method_1507(this.parent);
        } else {
            this.field_22787.method_1507(new class_410(confirmed -> {
                if (this.field_22787 == null) {
                    return;
                }
                if (!confirmed) {
                    this.field_22787.method_1507(this);
                    return;
                }
                initializeStagedSelectionsFromCurrent();
                this.hasUnsavedProfileChanges = false;
                this.field_22787.method_1507(this.parent);
            }, class_2561.method_43471("punchy.tuning.ui.confirm_unsaved_profile.title"), class_2561.method_43471("punchy.tuning.ui.confirm_unsaved_profile.message"), class_2561.method_43471("punchy.tuning.ui.confirm_unsaved_profile.ignore_exit"), class_2561.method_43471("punchy.tuning.ui.confirm_unsaved_profile.cancel")));
        }
    }

    private void recalcScroll() {
        int totalHeight = this.rows.isEmpty() ? 0 : (this.rows.size() * ROW_STEP) - 4;
        int viewport = Math.max(0, this.listBottom - this.listTop);
        this.maxScroll = Math.max(0, totalHeight - viewport);
        this.scrollOffset = class_3532.method_15340(this.scrollOffset, 0, this.maxScroll);
    }

    private void rebuildRows() {
        if (!this.rows.isEmpty()) {
            for (class_339 widget : this.rowWidgets) {
                method_37066(widget);
            }
        }
        this.rows.clear();
        this.rowBaseY.clear();
        this.rowWidgets.clear();
        List<String> sourceSlotIds = PunchyAnimationManager.getMixPackAnimationNames();
        List<String> slotIds = sourceSlotIds == null ? new ArrayList<>() : new ArrayList<>(sourceSlotIds);
        if (slotIds.remove("kind:empty_hand")) {
            slotIds.add(0, "kind:empty_hand");
        }
        for (String slotId : slotIds) {
            if (matchesFilter(slotId)) {
                addMainRow(slotId);
            }
        }
        recalcScroll();
        updateRowVisibility();
    }

    private void addMainRow(String slotId) {
        List<PunchyAnimationManager.PackOption> options = PunchyAnimationManager.getMixPackOptions(slotId);
        String selectedId = stagedMainSelection(slotId);
        int index = selectedIndex(options, selectedId);
        int y = this.listTop + (this.rows.size() * ROW_STEP);
        String friendlyName = PunchyAnimationManager.getMixPackEntryLabel(slotId);
        if (friendlyName == null || friendlyName.isBlank()) {
            friendlyName = slotId;
        }
        class_339 class_339Var = (class_4185) method_37063(class_4185.method_46430(class_2561.method_43470(friendlyName), button -> {
        }).method_46433(this.listLeft, y).method_46437(this.rowWidth, ROW_HEIGHT).method_46431());
        class_339 class_339Var2 = (class_4185) method_37063(class_4185.method_46430(class_2561.method_43470(optionLabel(options, index)), button2 -> {
            String current = stagedMainSelection(slotId);
            int currentIndex = selectedIndex(options, current);
            int nextIndex = options.isEmpty() ? -1 : (currentIndex + 1) % options.size();
            if (nextIndex >= 0) {
                PunchyAnimationManager.PackOption next = (PunchyAnimationManager.PackOption) options.get(nextIndex);
                stageMainSelection(slotId, next.optionId());
                this.hasUnsavedProfileChanges = true;
                rebuildRows();
            }
        }).method_46433(this.listLeft + this.rowWidth + 6, y).method_46437(this.selectorWidth, ROW_HEIGHT).method_46431());
        this.rows.add(new RowEntry(slotId, null, false, class_339Var, class_339Var2));
        this.rowBaseY.add(Integer.valueOf(y));
        this.rowWidgets.add(class_339Var);
        this.rowWidgets.add(class_339Var2);
    }

    private boolean matchesFilter(String slotId) {
        if (this.filterText == null || this.filterText.isBlank()) {
            return true;
        }
        String query = this.filterText.toLowerCase().trim();
        String slotKey = slotId == null ? "" : slotId.toLowerCase();
        String label = PunchyAnimationManager.getMixPackEntryLabel(slotId);
        String normalizedLabel = label == null ? "" : label.toLowerCase();
        return slotKey.contains(query) || normalizedLabel.contains(query);
    }

    private void drawRowIcons(class_332 graphics) {
        class_1799 stack;
        for (RowEntry row : this.rows) {
            if (row != null && !row.child() && row.nameButton().field_22764 && (stack = resolveDisplayStack(row.slotId())) != null && !stack.method_7960()) {
                class_4185 button = row.nameButton();
                int iconX = button.method_46426() + 5;
                int iconY = button.method_46427() + Math.max(0, (button.method_25364() - 16) / 2);
                graphics.method_51427(stack, iconX, iconY);
            }
        }
    }

    private class_1799 resolveDisplayStack(String slotId) {
        class_2960 id;
        class_1792 item;
        if (slotId == null || slotId.isBlank()) {
            return class_1799.field_8037;
        }
        if (slotId.startsWith("specific:") && (id = class_2960.method_12829(slotId.substring("specific:".length()))) != null && (item = (class_1792) class_7923.field_41178.method_17966(id).orElse(class_1802.field_8162)) != class_1802.field_8162) {
            return new class_1799(item);
        }
        if (slotId.startsWith("kind:")) {
            String rawKind = slotId.substring("kind:".length()).trim();
            try {
                VanillaFirstPersonItemLayer.ToolKind kind = VanillaFirstPersonItemLayer.ToolKind.valueOf(rawKind.toUpperCase(Locale.ROOT));
                return representativeStack(kind);
            } catch (IllegalArgumentException e) {
            }
        }
        return class_1799.field_8037;
    }

    /* JADX INFO: renamed from: punchy.client.tuning.MixThePacksScreen$1, reason: invalid class name */
    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/MixThePacksScreen$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind = new int[VanillaFirstPersonItemLayer.ToolKind.values().length];

        static {
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.SWORD.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.AXE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.PICKAXE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.SHOVEL.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.HOE.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.SPEAR.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.TRIDENT.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.FOOD.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.LANTERN.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.CREATURE_BUCKET.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.BUCKET.ordinal()] = MixThePacksScreen.PANEL_GAP;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.MINECART.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.BOAT.ordinal()] = MixThePacksScreen.PROFILE_ROW_HEIGHT;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.BOW.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.ARROW.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.CROSSBOW.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.SHIELD.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.FISHING_ROD.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.SPYGLASS.ordinal()] = MixThePacksScreen.ROW_HEIGHT;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.FLINT.ordinal()] = 21;
            } catch (NoSuchFieldError e21) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.MACE.ordinal()] = 22;
            } catch (NoSuchFieldError e22) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.TORCH.ordinal()] = 23;
            } catch (NoSuchFieldError e23) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.CHEST.ordinal()] = MixThePacksScreen.ROW_STEP;
            } catch (NoSuchFieldError e24) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.BUTTON_PLATE.ordinal()] = 25;
            } catch (NoSuchFieldError e25) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.TRAPDOOR.ordinal()] = 26;
            } catch (NoSuchFieldError e26) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.CARPET.ordinal()] = 27;
            } catch (NoSuchFieldError e27) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.BED.ordinal()] = 28;
            } catch (NoSuchFieldError e28) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.FULL_BLOCK.ordinal()] = 29;
            } catch (NoSuchFieldError e29) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.BLOCK.ordinal()] = 30;
            } catch (NoSuchFieldError e30) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.BUILDING_BLOCKS.ordinal()] = 31;
            } catch (NoSuchFieldError e31) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.COLORED_BLOCKS.ordinal()] = 32;
            } catch (NoSuchFieldError e32) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.NATURAL_BLOCKS.ordinal()] = 33;
            } catch (NoSuchFieldError e33) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.FUNCTIONAL_BLOCKS.ordinal()] = 34;
            } catch (NoSuchFieldError e34) {
            }
            try {
                $SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[VanillaFirstPersonItemLayer.ToolKind.REDSTONE.ordinal()] = 35;
            } catch (NoSuchFieldError e35) {
            }
        }
    }

    private class_1799 representativeStack(VanillaFirstPersonItemLayer.ToolKind kind) {
        class_1792 class_1792VarResolveSpearDisplayItem;
        switch (AnonymousClass1.$SwitchMap$punchy$client$renderer$layer$VanillaFirstPersonItemLayer$ToolKind[kind.ordinal()]) {
            case 1:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8162;
                break;
            case 2:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8802;
                break;
            case 3:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8556;
                break;
            case 4:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8377;
                break;
            case 5:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8250;
                break;
            case 6:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8527;
                break;
            case 7:
                class_1792VarResolveSpearDisplayItem = resolveSpearDisplayItem();
                break;
            case 8:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8547;
                break;
            case 9:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8176;
                break;
            case 10:
                class_1792VarResolveSpearDisplayItem = class_1802.field_16539;
                break;
            case 11:
                class_1792VarResolveSpearDisplayItem = class_1802.field_28354;
                break;
            case PANEL_GAP /* 12 */:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8705;
                break;
            case 13:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8045;
                break;
            case PROFILE_ROW_HEIGHT /* 14 */:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8533;
                break;
            case 15:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8102;
                break;
            case 16:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8107;
                break;
            case 17:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8399;
                break;
            case 18:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8255;
                break;
            case 19:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8378;
                break;
            case ROW_HEIGHT /* 20 */:
                class_1792VarResolveSpearDisplayItem = class_1802.field_27070;
                break;
            case 21:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8884;
                break;
            case 22:
                class_1792VarResolveSpearDisplayItem = class_1802.field_49814;
                break;
            case 23:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8810;
                break;
            case ROW_STEP /* 24 */:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8106;
                break;
            case 25:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8667;
                break;
            case 26:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8376;
                break;
            case 27:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8482;
                break;
            case 28:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8789;
                break;
            case 29:
            case 30:
            case 31:
                class_1792VarResolveSpearDisplayItem = class_1802.field_20391;
                break;
            case 32:
                class_1792VarResolveSpearDisplayItem = class_1802.field_19058;
                break;
            case 33:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8270;
                break;
            case 34:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8465;
                break;
            case 35:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8725;
                break;
            default:
                class_1792VarResolveSpearDisplayItem = class_1802.field_8407;
                break;
        }
        class_1792 item = class_1792VarResolveSpearDisplayItem;
        if (item == class_1802.field_8162) {
            return class_1799.field_8037;
        }
        return new class_1799(item);
    }

    private class_1792 resolveSpearDisplayItem() {
        class_1792 spear;
        class_1792 diamondSpear;
        class_2960 diamondSpearId = class_2960.method_12829("minecraft:diamond_spear");
        if (diamondSpearId != null && (diamondSpear = (class_1792) class_7923.field_41178.method_17966(diamondSpearId).orElse(class_1802.field_8162)) != class_1802.field_8162) {
            return diamondSpear;
        }
        class_2960 spearId = class_2960.method_12829("minecraft:spear");
        if (spearId != null && (spear = (class_1792) class_7923.field_41178.method_17966(spearId).orElse(class_1802.field_8162)) != class_1802.field_8162) {
            return spear;
        }
        return class_1802.field_8547;
    }

    private void addTypeRows(String slotId) {
        List<String> types = PunchyAnimationManager.getMixPackExpandableEntryTypes(slotId);
        for (String typeId : types) {
            List<PunchyAnimationManager.PackOption> typeOptions = buildTypeOptionsForDisplay(slotId, typeId);
            String selectedType = stagedTypeSelection(slotId, typeId);
            int typeIndex = selectedIndex(typeOptions, selectedType);
            int childY = this.listTop + (this.rows.size() * ROW_STEP);
            int childInset = Math.min(10, Math.max(4, this.rowWidth / 8));
            int childNameX = this.listLeft + childInset;
            int childNameWidth = Math.max(80, this.rowWidth - (childInset * 2));
            int childSelectorX = ((this.listLeft + this.rowWidth) + 6) - childInset;
            String typeLabel = "  - " + prettyTypeLabel(typeId);
            class_339 class_339Var = (class_4185) method_37063(class_4185.method_46430(class_2561.method_43470(typeLabel), button -> {
            }).method_46433(childNameX, childY).method_46437(childNameWidth, ROW_HEIGHT).method_46431());
            ((class_4185) class_339Var).field_22763 = false;
            class_339 class_339Var2 = (class_4185) method_37063(class_4185.method_46430(class_2561.method_43470(optionLabel(typeOptions, typeIndex)), button2 -> {
                String currentType = stagedTypeSelection(slotId, typeId);
                int currentTypeIndex = selectedIndex(typeOptions, currentType);
                int nextTypeIndex = typeOptions.isEmpty() ? -1 : (currentTypeIndex + 1) % typeOptions.size();
                if (nextTypeIndex >= 0) {
                    PunchyAnimationManager.PackOption next = (PunchyAnimationManager.PackOption) typeOptions.get(nextTypeIndex);
                    stageTypeSelection(slotId, typeId, next.optionId());
                    this.hasUnsavedProfileChanges = true;
                    rebuildRows();
                }
            }).method_46433(childSelectorX, childY).method_46437(this.selectorWidth, ROW_HEIGHT).method_46431());
            this.rows.add(new RowEntry(slotId, typeId, true, class_339Var, class_339Var2));
            this.rowBaseY.add(Integer.valueOf(childY));
            this.rowWidgets.add(class_339Var);
            this.rowWidgets.add(class_339Var2);
        }
    }

    private void updateRowVisibility() {
        for (int i = 0; i < this.rows.size(); i++) {
            RowEntry row = this.rows.get(i);
            int base = this.rowBaseY.get(i).intValue();
            int y = base - this.scrollOffset;
            row.nameButton().method_46419(y);
            row.selectorButton().method_46419(y);
            boolean visible = y + ROW_HEIGHT > this.listTop && y < this.listBottom;
            row.nameButton().field_22764 = visible;
            row.nameButton().field_22763 = visible && !row.child();
            row.selectorButton().field_22764 = visible;
            row.selectorButton().field_22763 = visible;
        }
    }

    private void drawScrollbar(class_332 graphics) {
        if (this.maxScroll <= 0) {
            return;
        }
        int barX = this.listRight - 4;
        int barTop = this.listTop;
        int barBottom = this.listBottom;
        graphics.method_25294(barX, barTop, barX + 4, barBottom, 1427118368);
        int trackHeight = Math.max(1, barBottom - barTop);
        int totalHeight = (this.rows.size() * ROW_STEP) - 4;
        int thumbHeight = Math.max(10, Math.round(trackHeight * ((this.listBottom - this.listTop) / totalHeight)));
        int thumbMax = trackHeight - thumbHeight;
        int thumbOffset = this.maxScroll == 0 ? 0 : Math.round(thumbMax * (this.scrollOffset / this.maxScroll));
        int thumbTop = barTop + thumbOffset;
        graphics.method_25294(barX, thumbTop, barX + 4, Math.min(barBottom, thumbTop + thumbHeight), -2827553);
    }

    private boolean isMouseOverList(double x, double y) {
        return x >= ((double) this.listLeft) && x <= ((double) this.listRight) && y >= ((double) this.listTop) && y <= ((double) this.listBottom);
    }

    private boolean isMouseOverScrollBar(double x, double y) {
        int barX = this.listRight - 4;
        return x >= ((double) barX) && x <= ((double) this.listRight) && y >= ((double) this.listTop) && y <= ((double) this.listBottom);
    }

    private boolean isMouseOverProfileList(double x, double y) {
        return x >= ((double) this.profileListX) && x <= ((double) (this.profileListX + this.profileListW)) && y >= ((double) this.profileListY) && y <= ((double) (this.profileListY + this.profileListH));
    }

    private boolean isMouseOverProfilePanel(double x, double y) {
        int top = this.profilesTitleY - 2;
        int bottom = this.doneButton.method_46427() - 6;
        return x >= ((double) this.profilePanelX) && x <= ((double) (this.profilePanelX + this.profilePanelW)) && y >= ((double) top) && y <= ((double) bottom);
    }

    private void updateScrollFromMouse(double mouseY) {
        int trackHeight = this.listBottom - this.listTop;
        if (trackHeight <= 0) {
            return;
        }
        float ratio = (float) ((mouseY - ((double) this.listTop)) / ((double) trackHeight));
        this.scrollOffset = (int) (class_3532.method_15363(ratio, 0.0f, 1.0f) * this.maxScroll);
        updateRowVisibility();
    }

    private void recalcProfilePanelScroll() {
        this.profilePanelScroll = 0;
        this.profilePanelMaxScroll = 0;
    }

    private void openDeleteProfileConfirm() {
        String typedName;
        String name = selectedProfileName();
        if ((name == null || name.isBlank()) && this.profileNameBox != null && (typedName = this.profileNameBox.method_1882()) != null) {
            String typedName2 = typedName.trim();
            if (!typedName2.isBlank() && this.profiles.contains(typedName2)) {
                this.selectedProfile = this.profiles.indexOf(typedName2);
                ensureSelectedVisibleInProfileList();
                updateProfileRowButtons();
                name = typedName2;
            }
        }
        if (name == null || name.isBlank()) {
            this.statusMessage = class_2561.method_43471("punchy.tuning.ui.status.profile_delete_failed").getString();
            return;
        }
        this.pendingDeleteProfileName = name;
        recalcDeleteConfirmLayout();
        this.deleteProfileConfirmOpen = true;
    }

    private void cancelDeleteProfileConfirm() {
        this.deleteProfileConfirmOpen = false;
        this.pendingDeleteProfileName = null;
    }

    private void confirmDeleteProfile() {
        String string;
        String name = this.pendingDeleteProfileName;
        cancelDeleteProfileConfirm();
        boolean ok = (name == null || name.isBlank() || !MixProfileManager.deleteProfile(name)) ? false : true;
        if (ok) {
            applyResetToResourcePackOrderAndUnloadProfile();
        }
        if (ok) {
            string = class_2561.method_43469("punchy.tuning.ui.status.profile_deleted", new Object[]{name}).getString();
        } else {
            string = class_2561.method_43471("punchy.tuning.ui.status.profile_delete_failed").getString();
        }
        this.statusMessage = string;
        refreshProfiles(true);
    }

    private void applyResetToResourcePackOrderAndUnloadProfile() {
        PunchyAnimationManager.resetMixPacksToResourcePackOrder();
        MixProfileManager.unloadActiveProfileToDefault();
        this.stagedPackSelections.clear();
        this.stagedPackTypeSelections.clear();
        setPreviewToResourcePackOrder();
        this.hasUnsavedProfileChanges = false;
        this.mixSessionBaseline = PunchyConfig.snapshot();
        initializeStagedSelectionsFromCurrent();
        refreshProfiles(true);
        rebuildRows();
    }

    private void recalcDeleteConfirmLayout() {
        int modalWidth = Math.min(380, Math.max(260, (this.containerRight - this.containerLeft) - 48));
        this.deleteModalLeft = this.containerLeft + (((this.containerRight - this.containerLeft) - modalWidth) / 2);
        this.deleteModalTop = this.containerTop + (((this.containerBottom - this.containerTop) - 112) / 2);
        this.deleteModalRight = this.deleteModalLeft + modalWidth;
        this.deleteModalBottom = this.deleteModalTop + 112;
        int buttonWidth = ((modalWidth - 32) - 8) / 2;
        this.deleteCancelButtonLeft = this.deleteModalLeft + 16;
        this.deleteCancelButtonTop = this.deleteModalBottom - 30;
        this.deleteCancelButtonRight = this.deleteCancelButtonLeft + buttonWidth;
        this.deleteCancelButtonBottom = this.deleteCancelButtonTop + ROW_HEIGHT;
        this.deleteConfirmButtonLeft = this.deleteCancelButtonRight + 8;
        this.deleteConfirmButtonTop = this.deleteCancelButtonTop;
        this.deleteConfirmButtonRight = this.deleteConfirmButtonLeft + buttonWidth;
        this.deleteConfirmButtonBottom = this.deleteConfirmButtonTop + ROW_HEIGHT;
    }

    private void drawDeleteProfileConfirmOverlay(class_332 graphics, int mouseX, int mouseY) {
        if (!this.deleteProfileConfirmOpen) {
            return;
        }
        graphics.method_51448().method_22903();
        graphics.method_51448().method_46416(0.0f, 0.0f, 500.0f);
        graphics.method_25294(0, 0, this.field_22789, this.field_22790, -805306368);
        graphics.method_25294(this.deleteModalLeft, this.deleteModalTop, this.deleteModalRight, this.deleteModalBottom, -15657446);
        graphics.method_25294(this.deleteModalLeft, this.deleteModalTop, this.deleteModalRight, this.deleteModalTop + 1, -11641751);
        graphics.method_25294(this.deleteModalLeft, this.deleteModalBottom - 1, this.deleteModalRight, this.deleteModalBottom, -11641751);
        graphics.method_25294(this.deleteModalLeft, this.deleteModalTop, this.deleteModalLeft + 1, this.deleteModalBottom, -11641751);
        graphics.method_25294(this.deleteModalRight - 1, this.deleteModalTop, this.deleteModalRight, this.deleteModalBottom, -11641751);
        class_2561 title = nonBlankTranslatable("punchy.tuning.ui.confirm_delete_profile.title", "Você realmente quer fazer isso?", new Object[0]);
        String targetName = this.pendingDeleteProfileName == null ? "-" : this.pendingDeleteProfileName;
        class_2561 message = nonBlankTranslatable("punchy.tuning.ui.confirm_delete_profile.message", "Delete profile \"" + targetName + "\"?", targetName);
        String titleText = title.getString();
        String messageText = message.getString();
        int titleX = this.deleteModalLeft + (((this.deleteModalRight - this.deleteModalLeft) - this.field_22793.method_1727(titleText)) / 2);
        int messageX = this.deleteModalLeft + (((this.deleteModalRight - this.deleteModalLeft) - this.field_22793.method_1727(messageText)) / 2);
        graphics.method_25303(this.field_22793, titleText, titleX, this.deleteModalTop + PANEL_GAP, -1);
        graphics.method_25303(this.field_22793, messageText, messageX, this.deleteModalTop + 34, -3747624);
        boolean hoverCancel = isMouseInside(mouseX, mouseY, this.deleteCancelButtonLeft, this.deleteCancelButtonTop, this.deleteCancelButtonRight, this.deleteCancelButtonBottom);
        boolean hoverConfirm = isMouseInside(mouseX, mouseY, this.deleteConfirmButtonLeft, this.deleteConfirmButtonTop, this.deleteConfirmButtonRight, this.deleteConfirmButtonBottom);
        drawModalButton(graphics, this.deleteCancelButtonLeft, this.deleteCancelButtonTop, this.deleteCancelButtonRight, this.deleteCancelButtonBottom, hoverCancel, false, nonBlankTranslatable("gui.cancel", "Cancel", new Object[0]));
        drawModalButton(graphics, this.deleteConfirmButtonLeft, this.deleteConfirmButtonTop, this.deleteConfirmButtonRight, this.deleteConfirmButtonBottom, hoverConfirm, true, nonBlankTranslatable("punchy.tuning.ui.confirm_delete_profile.confirm", "Delete", new Object[0]));
        graphics.method_51448().method_22909();
    }

    private void drawModalButton(class_332 graphics, int left, int top, int right, int bottom, boolean hovered, boolean destructive, class_2561 label) {
        int fill = destructive ? hovered ? -7459282 : -9690590 : hovered ? -12958126 : -13682107;
        int border = destructive ? -3048067 : -7429710;
        graphics.method_25294(left, top, right, bottom, fill);
        graphics.method_25294(left, top, right, top + 1, border);
        graphics.method_25294(left, bottom - 1, right, bottom, border);
        graphics.method_25294(left, top, left + 1, bottom, border);
        graphics.method_25294(right - 1, top, right, bottom, border);
        String labelText = label.getString();
        int textX = left + (((right - left) - this.field_22793.method_1727(labelText)) / 2);
        int textY = top + (((bottom - top) - 8) / 2);
        graphics.method_25303(this.field_22793, labelText, textX, textY, -1);
    }

    private static boolean isMouseInside(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        return mouseX >= ((double) left) && mouseX <= ((double) right) && mouseY >= ((double) top) && mouseY <= ((double) bottom);
    }

    private static class_2561 nonBlankTranslatable(String key, String fallback, Object... args) {
        class_5250 class_5250VarMethod_43469 = class_2561.method_43469(key, args);
        String resolved = class_5250VarMethod_43469.getString();
        if (resolved == null || resolved.isBlank() || resolved.equals(key)) {
            return class_2561.method_43470(fallback);
        }
        return class_5250VarMethod_43469;
    }

    private void setProfilePanelScroll(int newValue) {
        int clamped = class_3532.method_15340(newValue, 0, this.profilePanelMaxScroll);
        if (clamped == this.profilePanelScroll) {
            return;
        }
        int delta = clamped - this.profilePanelScroll;
        this.profilePanelScroll = clamped;
        int shift = -delta;
        this.profilesTitleY += shift;
        this.profileListY += shift;
        this.profileNameBox.method_46419(this.profileNameBox.method_46427() + shift);
        this.saveProfileButton.method_46419(this.saveProfileButton.method_46427() + shift);
        this.loadProfileButton.method_46419(this.loadProfileButton.method_46427() + shift);
        this.deleteProfileButton.method_46419(this.deleteProfileButton.method_46427() + shift);
        this.openProfilesButton.method_46419(this.openProfilesButton.method_46427() + shift);
        for (class_4185 rowButton : this.profileRowButtons) {
            rowButton.method_46419(rowButton.method_46427() + shift);
        }
    }

    private void refreshProfiles(boolean clampSelection) {
        this.profiles = MixProfileManager.listProfiles();
        this.profileListScroll = class_3532.method_15340(this.profileListScroll, 0, Math.max(0, this.profiles.size() - this.profileVisibleRows));
        if (this.profiles.isEmpty()) {
            this.selectedProfile = -1;
            if (this.profileNameBox != null) {
                this.profileNameBox.method_1852("");
            }
            updateProfileRowButtons();
            return;
        }
        if (this.selectedProfile < 0 || this.selectedProfile >= this.profiles.size() || clampSelection) {
            String active = MixProfileManager.getActiveProfileName();
            if (active != null) {
                int activeIndex = this.profiles.indexOf(active);
                if (activeIndex >= 0) {
                    this.selectedProfile = activeIndex;
                }
            } else if (clampSelection) {
                this.selectedProfile = -1;
            }
            if (this.selectedProfile < 0 && !clampSelection) {
                this.selectedProfile = 0;
            } else if (this.selectedProfile >= 0) {
                this.selectedProfile = class_3532.method_15340(this.selectedProfile, 0, this.profiles.size() - 1);
            }
        }
        ensureSelectedVisibleInProfileList();
        String selected = selectedProfileName();
        if (this.profileNameBox != null) {
            if (selected != null && !selected.isBlank()) {
                this.profileNameBox.method_1852(selected);
            } else {
                this.profileNameBox.method_1852("");
            }
        }
        updateProfileRowButtons();
    }

    private void ensureSelectedVisibleInProfileList() {
        if (this.selectedProfile < 0) {
            return;
        }
        int maxScroll = Math.max(0, this.profiles.size() - this.profileVisibleRows);
        if (this.selectedProfile < this.profileListScroll) {
            this.profileListScroll = this.selectedProfile;
        } else if (this.selectedProfile >= this.profileListScroll + this.profileVisibleRows) {
            this.profileListScroll = (this.selectedProfile - this.profileVisibleRows) + 1;
        }
        this.profileListScroll = class_3532.method_15340(this.profileListScroll, 0, maxScroll);
    }

    private void updateProfileRowButtons() {
        for (int i = 0; i < this.profileRowButtons.size(); i++) {
            class_4185 rowButton = this.profileRowButtons.get(i);
            int index = this.profileListScroll + i;
            if (index >= 0 && index < this.profiles.size()) {
                String name = this.profiles.get(index);
                boolean selected = index == this.selectedProfile;
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
        if (selectedName != null && this.profileNameBox != null) {
            this.profileNameBox.method_1852(selectedName);
        }
        ensureSelectedVisibleInProfileList();
        updateProfileRowButtons();
    }

    private String selectedProfileName() {
        if (this.selectedProfile < 0 || this.selectedProfile >= this.profiles.size()) {
            return null;
        }
        return this.profiles.get(this.selectedProfile);
    }

    private boolean openProfilesFolder() {
        try {
            Path dir = MixProfileManager.profilesDirectoryPath();
            class_156.method_668().method_60932(dir);
            return true;
        } catch (Exception e) {
            try {
                Path dir2 = MixProfileManager.profilesDirectoryPath();
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dir2.toFile());
                    return true;
                }
                return false;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    private void initializeStagedSelectionsFromCurrent() {
        PunchyConfig.Data snapshot = PunchyConfig.snapshot();
        this.stagedPackSelections.clear();
        this.stagedPackTypeSelections.clear();
        this.previewPackSelections.clear();
        this.previewPackTypeSelections.clear();
        if (snapshot == null) {
            return;
        }
        if (snapshot.animationPackSelections != null) {
            this.stagedPackSelections.putAll(snapshot.animationPackSelections);
        }
        if (snapshot.animationPackTypeSelections != null) {
            this.stagedPackTypeSelections.putAll(snapshot.animationPackTypeSelections);
        }
    }

    private String stagedMainSelection(String slotId) {
        if (this.stagedPackSelections.containsKey(slotId)) {
            String selected = this.stagedPackSelections.get(slotId);
            if (selected == null || selected.isBlank()) {
                return PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID;
            }
            return selected;
        }
        String previewSelected = this.previewPackSelections.get(slotId);
        if (previewSelected != null && !previewSelected.isBlank()) {
            return previewSelected;
        }
        String selected2 = this.stagedPackSelections.get(slotId);
        if (selected2 == null || selected2.isBlank()) {
            String effective = PunchyAnimationManager.getSelectedMixPackOption(slotId);
            if (effective == null || effective.isBlank()) {
                return PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID;
            }
            return effective;
        }
        return selected2;
    }

    private String stagedTypeSelection(String slotId, String typeId) {
        String key = slotId + "|" + (typeId == null ? "" : typeId.trim().toLowerCase(Locale.ROOT));
        String selected = this.stagedPackTypeSelections.get(key);
        if (selected != null && !selected.isBlank()) {
            return selected;
        }
        String previewSelected = this.previewPackTypeSelections.get(key);
        if (previewSelected != null && !previewSelected.isBlank()) {
            return previewSelected;
        }
        String effectiveType = PunchyAnimationManager.getSelectedMixPackTypeOption(slotId, typeId);
        if (effectiveType != null && !effectiveType.isBlank()) {
            return effectiveType;
        }
        return stagedMainSelection(slotId);
    }

    private void stageMainSelection(String slotId, String optionId) {
        if (slotId == null || slotId.isBlank()) {
            return;
        }
        String keyPrefix = slotId + "|";
        this.stagedPackTypeSelections.keySet().removeIf(key -> {
            return key != null && key.startsWith(keyPrefix);
        });
        this.previewPackSelections.remove(slotId);
        this.previewPackTypeSelections.keySet().removeIf(key2 -> {
            return key2 != null && key2.startsWith(keyPrefix);
        });
        if (optionId == null || optionId.isBlank()) {
            this.stagedPackSelections.remove(slotId);
            return;
        }
        String normalized = optionId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            this.stagedPackSelections.remove(slotId);
        } else {
            this.stagedPackSelections.put(slotId, normalized);
        }
    }

    private void stageTypeSelection(String slotId, String typeId, String optionId) {
        if (slotId == null || slotId.isBlank() || typeId == null || typeId.isBlank()) {
            return;
        }
        String normalizedType = typeId.trim().toLowerCase(Locale.ROOT);
        String key = slotId + "|" + normalizedType;
        this.previewPackTypeSelections.remove(key);
        String normalizedOption = optionId == null ? "" : optionId.trim().toLowerCase(Locale.ROOT);
        String main = stagedMainSelection(slotId);
        if (normalizedOption.isBlank() || PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID.equals(normalizedOption) || normalizedOption.equals(main)) {
            this.stagedPackTypeSelections.remove(key);
        } else {
            this.stagedPackTypeSelections.put(key, normalizedOption);
        }
    }

    private void setPreviewToResourcePackOrder() {
        this.previewPackSelections.clear();
        this.previewPackTypeSelections.clear();
        List<String> slots = PunchyAnimationManager.getMixPackAnimationNames();
        if (slots == null || slots.isEmpty()) {
            return;
        }
        for (String slotId : slots) {
            if (slotId != null && !slotId.isBlank()) {
                String mainOption = resolveResourceOrderMainOption(slotId);
                this.previewPackSelections.put(slotId, mainOption);
                List<String> types = PunchyAnimationManager.getMixPackEntryTypes(slotId);
                if (types != null && !types.isEmpty()) {
                    for (String typeId : types) {
                        if (typeId != null && !typeId.isBlank()) {
                            String key = slotId + "|" + typeId.trim().toLowerCase(Locale.ROOT);
                            String typeOption = resolveResourceOrderTypeOption(slotId, typeId, mainOption);
                            if (typeOption != null && !typeOption.isBlank()) {
                                this.previewPackTypeSelections.put(key, typeOption);
                            }
                        }
                    }
                }
            }
        }
    }

    private static String resolveResourceOrderMainOption(String slotId) {
        List<PunchyAnimationManager.PackOption> options = PunchyAnimationManager.getMixPackOptions(slotId);
        if (options == null || options.isEmpty()) {
            return PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID;
        }
        for (PunchyAnimationManager.PackOption option : options) {
            if (option != null && option.optionId() != null && !option.optionId().isBlank() && !PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID.equals(option.optionId())) {
                return option.optionId();
            }
        }
        return PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID;
    }

    private static String resolveResourceOrderTypeOption(String slotId, String typeId, String mainOption) {
        List<String> options = PunchyTuningConfig.getMixPackSlotEntryTypeOptionIds(slotId, typeId);
        if (options == null || options.isEmpty()) {
            return (mainOption == null || mainOption.isBlank()) ? PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID : mainOption;
        }
        for (String option : options) {
            if (option != null && !option.isBlank() && !PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID.equals(option)) {
                return option;
            }
        }
        if (mainOption != null && !mainOption.isBlank()) {
            return mainOption;
        }
        return PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID;
    }

    private List<PunchyAnimationManager.PackOption> buildTypeOptionsForDisplay(String slotId, String typeId) {
        List<String> optionIds = PunchyTuningConfig.getMixPackSlotEntryTypeOptionIds(slotId, typeId);
        List<String> normalized = new ArrayList<>(optionIds == null ? List.of() : optionIds);
        String main = stagedMainSelection(slotId);
        if (main != null && !main.isBlank() && !normalized.contains(main)) {
            normalized.add(0, main);
        }
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<PunchyAnimationManager.PackOption> result = new ArrayList<>(normalized.size());
        for (String optionId : normalized) {
            if (!PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID.equals(optionId)) {
                result.add(new PunchyAnimationManager.PackOption(optionId, displayPackOptionLabel(optionId)));
            }
        }
        return result;
    }

    private String displayPackOptionLabel(String optionId) {
        if (optionId == null || optionId.isBlank()) {
            return "Unknown";
        }
        if ("pack:punchy:punchy".equals(optionId)) {
            return "§bPunchy! §eDefault";
        }
        String cleaned = optionId;
        if (cleaned.startsWith("pack:")) {
            cleaned = cleaned.substring("pack:".length());
        }
        if (cleaned.startsWith("file/")) {
            cleaned = cleaned.substring("file/".length());
        }
        if (cleaned.startsWith("file\\")) {
            cleaned = cleaned.substring("file\\".length());
        }
        if (cleaned.endsWith(".zip")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        if (cleaned.endsWith(".jar")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        String cleaned2 = cleaned.replace('_', ' ').replace('-', ' ').trim();
        if (cleaned2.isBlank()) {
            return optionId;
        }
        return cleaned2;
    }

    private static int selectedIndex(List<PunchyAnimationManager.PackOption> options, String selected) {
        if (options == null || options.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).optionId().equals(selected)) {
                return i;
            }
        }
        return 0;
    }

    private static String optionLabel(List<PunchyAnimationManager.PackOption> options, int selectedIndex) {
        if (options == null || options.isEmpty() || selectedIndex < 0 || selectedIndex >= options.size()) {
            return "-";
        }
        return options.get(selectedIndex).label();
    }

    private static String prettyTypeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "Type";
        }
        String value = type.trim().replace('_', ' ');
        String[] parts = value.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                if (!out.isEmpty()) {
                    out.append(' ');
                }
                out.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    out.append(part.substring(1));
                }
            }
        }
        return out.toString();
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/MixThePacksScreen$RowEntry.class */
    private static final class RowEntry extends Record {
        private final String slotId;
        private final String entryType;
        private final boolean child;
        private final class_4185 nameButton;
        private final class_4185 selectorButton;

        private RowEntry(String slotId, String entryType, boolean child, class_4185 nameButton, class_4185 selectorButton) {
            this.slotId = slotId;
            this.entryType = entryType;
            this.child = child;
            this.nameButton = nameButton;
            this.selectorButton = selectorButton;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, RowEntry.class), RowEntry.class, "slotId;entryType;child;nameButton;selectorButton", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->slotId:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->entryType:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->child:Z", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->nameButton:Lnet/minecraft/class_4185;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->selectorButton:Lnet/minecraft/class_4185;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, RowEntry.class), RowEntry.class, "slotId;entryType;child;nameButton;selectorButton", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->slotId:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->entryType:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->child:Z", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->nameButton:Lnet/minecraft/class_4185;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->selectorButton:Lnet/minecraft/class_4185;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, RowEntry.class, Object.class), RowEntry.class, "slotId;entryType;child;nameButton;selectorButton", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->slotId:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->entryType:Ljava/lang/String;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->child:Z", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->nameButton:Lnet/minecraft/class_4185;", "FIELD:Lpunchy/client/tuning/MixThePacksScreen$RowEntry;->selectorButton:Lnet/minecraft/class_4185;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String slotId() {
            return this.slotId;
        }

        public String entryType() {
            return this.entryType;
        }

        public boolean child() {
            return this.child;
        }

        public class_4185 nameButton() {
            return this.nameButton;
        }

        public class_4185 selectorButton() {
            return this.selectorButton;
        }
    }
}
