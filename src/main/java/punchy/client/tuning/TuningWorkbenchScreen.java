package punchy.client.tuning;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_156;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_342;
import net.minecraft.class_3532;
import net.minecraft.class_357;
import net.minecraft.class_410;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_7923;
import punchy.client.render.FishingLineProxyTuning;
import punchy.client.render.ToolKindResolver;
import punchy.client.render.ToolTuning;
import punchy.client.renderer.layer.ArmToolTuning;
import punchy.client.renderer.layer.PlayerArmModelLayer;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.client.state.SpearStateMachine;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningWorkbenchScreen.class */
public final class TuningWorkbenchScreen extends class_437 {
    private Scope scope;
    private boolean leftHand;
    private VanillaFirstPersonItemLayer.ToolKind selectedKind;
    private class_4185 scopeButton;
    private class_4185 handButton;
    private class_4185 targetButton;
    private class_4185 kindButton;
    private class_4185 resetButton;
    private class_4185 resetCurrentButton;
    private class_4185 mirrorButton;
    private class_4185 reloadTuningButton;
    private class_4185 saveProfileButton;
    private class_4185 loadProfileButton;
    private class_4185 deleteProfileButton;
    private class_4185 importProfileButton;
    private class_4185 doneButton;
    private FloatSlider offX;
    private FloatSlider offY;
    private FloatSlider offZ;
    private FloatSlider rotX;
    private FloatSlider rotY;
    private FloatSlider rotZ;
    private FloatSlider scale;
    private FloatSlider pivotX;
    private FloatSlider pivotY;
    private FloatSlider pivotZ;
    private final List<FloatSlider> sliders;
    private final List<class_339> uiWidgets;
    private FloatSlider activeSlider;
    private class_342 profileNameBox;
    private List<String> profiles;
    private int selectedProfile;
    private String statusMessage;
    private boolean syncing;
    private boolean compactLayout;
    private int scrollOffset;
    private int maxScroll;
    private int profilesTitleX;
    private int profilesTitleY;
    private int profileListX;
    private int profileListY;
    private int profileListW;
    private int profileListH;
    private int profileListScroll;
    private boolean draggingMainScrollbar;
    private boolean draggingProfileScrollbar;
    private final List<class_4185> profileRowButtons;
    private int containerLeft;
    private int containerRight;
    private int mainScrollX;
    private int mainScrollTop;
    private int mainScrollBottom;
    private int profileScrollX;
    private int profileScrollTop;
    private int profileScrollBottom;
    private int mainPanelLeft;
    private int mainPanelTop;
    private int mainPanelRight;
    private int mainPanelBottom;
    private int leftPanelLeft;
    private int leftPanelTop;
    private int leftPanelRight;
    private int leftPanelBottom;
    private int rightPanelLeft;
    private int rightPanelTop;
    private int rightPanelRight;
    private int rightPanelBottom;
    private EditorTarget editorTarget;
    private boolean hasUnsavedProfileChanges;
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
    private final class_437 parentScreen;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningWorkbenchScreen$EditorTarget.class */
    private enum EditorTarget {
        ITEM,
        ARM,
        FISHING_LINE
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningWorkbenchScreen$Scope.class */
    private enum Scope {
        GLOBAL("punchy.tuning.ui.scope.global"),
        SPECIFIC("punchy.tuning.ui.scope.specific");

        final String key;

        Scope(String key) {
            this.key = key;
        }
    }

    public TuningWorkbenchScreen() {
        this(null);
    }

    public TuningWorkbenchScreen(class_437 parentScreen) {
        super(class_2561.method_43471("punchy.tuning.ui.title"));
        this.scope = Scope.SPECIFIC;
        this.leftHand = false;
        this.selectedKind = VanillaFirstPersonItemLayer.ToolKind.AXE;
        this.sliders = new ArrayList();
        this.uiWidgets = new ArrayList();
        this.activeSlider = null;
        this.profiles = List.of();
        this.selectedProfile = -1;
        this.statusMessage = "";
        this.syncing = false;
        this.compactLayout = false;
        this.scrollOffset = 0;
        this.maxScroll = 0;
        this.profilesTitleX = 0;
        this.profilesTitleY = 0;
        this.profileListX = 0;
        this.profileListY = 0;
        this.profileListW = 0;
        this.profileListH = 0;
        this.profileListScroll = 0;
        this.draggingMainScrollbar = false;
        this.draggingProfileScrollbar = false;
        this.profileRowButtons = new ArrayList();
        this.containerLeft = 0;
        this.containerRight = 0;
        this.mainScrollX = 0;
        this.mainScrollTop = 0;
        this.mainScrollBottom = 0;
        this.profileScrollX = 0;
        this.profileScrollTop = 0;
        this.profileScrollBottom = 0;
        this.mainPanelLeft = 0;
        this.mainPanelTop = 0;
        this.mainPanelRight = 0;
        this.mainPanelBottom = 0;
        this.leftPanelLeft = 0;
        this.leftPanelTop = 0;
        this.leftPanelRight = 0;
        this.leftPanelBottom = 0;
        this.rightPanelLeft = 0;
        this.rightPanelTop = 0;
        this.rightPanelRight = 0;
        this.rightPanelBottom = 0;
        this.editorTarget = EditorTarget.ITEM;
        this.hasUnsavedProfileChanges = false;
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
        this.parentScreen = parentScreen;
    }

    public static boolean shouldRenderPivotMarker() {
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return false;
        }
        class_437 class_437Var = mc.field_1755;
        if (!(class_437Var instanceof TuningWorkbenchScreen)) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    protected void method_25426() throws MatchException {
        method_37067();
        this.sliders.clear();
        this.uiWidgets.clear();
        this.profileRowButtons.clear();
        this.scopeButton = addUiWidget(class_4185.method_46430(scopeLabel(), button -> {
            Scope scope;
            if (!isArmEditor()) {
                return;
            }
            switch (this.scope) {
                case GLOBAL:
                    scope = Scope.SPECIFIC;
                    break;
                case SPECIFIC:
                    scope = Scope.GLOBAL;
                    break;
                default:
                    throw new MatchException((String) null, (Throwable) null);
            }
            this.scope = scope;
            refreshHeaderButtons();
            loadFromCurrentProfile();
            layoutWidgets();
        }).method_46433(0, 0).method_46437(110, 20).method_46431());
        this.handButton = addUiWidget(class_4185.method_46430(handLabel(), button2 -> {
            this.leftHand = !this.leftHand;
            autoKindFromHeldItem();
            refreshHeaderButtons();
            loadFromCurrentProfile();
            layoutWidgets();
        }).method_46433(0, 0).method_46437(90, 20).method_46431());
        this.targetButton = addUiWidget(class_4185.method_46430(targetLabel(), button3 -> {
            cycleEditorTarget();
            normalizeScopeForTarget();
            this.activeSlider = null;
            refreshHeaderButtons();
            loadFromCurrentProfile();
            layoutWidgets();
        }).method_46433(0, 0).method_46437(92, 20).method_46431());
        this.kindButton = addUiWidget(class_4185.method_46430(kindLabel(), button4 -> {
        }).method_46433(0, 0).method_46437(160, 20).method_46431());
        this.kindButton.field_22763 = false;
        this.offX = addSlider(0, 0, 200, 20, "offX", -2.0f, 2.0f);
        this.offY = addSlider(0, 0, 200, 20, "offY", -2.0f, 2.0f);
        this.offZ = addSlider(0, 0, 200, 20, "offZ", -2.0f, 2.0f);
        this.rotX = addSlider(0, 0, 200, 20, "rotX", -180.0f, 180.0f);
        this.rotY = addSlider(0, 0, 200, 20, "rotY", -180.0f, 180.0f);
        this.rotZ = addSlider(0, 0, 200, 20, "rotZ", -180.0f, 180.0f);
        this.scale = addSlider(0, 0, 200, 20, "scale", 0.1f, 3.0f);
        this.pivotX = addSlider(0, 0, 200, 20, "pivotX", -1.0f, 1.0f);
        this.pivotY = addSlider(0, 0, 200, 20, "pivotY", -1.0f, 1.0f);
        this.pivotZ = addSlider(0, 0, 200, 20, "pivotZ", -1.0f, 1.0f);
        this.resetButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.reset_to_default"), button5 -> {
            String string;
            boolean ok = TuningProfileManager.unloadActiveProfileToDefault();
            if (ok) {
                string = class_2561.method_43471("punchy.tuning.ui.status.reset_to_default_done").getString();
            } else {
                string = class_2561.method_43471("punchy.tuning.ui.status.reset_to_default_failed").getString();
            }
            this.statusMessage = string;
            if (ok) {
                this.hasUnsavedProfileChanges = false;
            }
            refreshProfiles(true);
            loadFromCurrentProfile();
        }).method_46433(0, 0).method_46437(120, 20).method_46431());
        this.resetCurrentButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.reset_current"), button6 -> {
            resetCurrentProfile();
            loadFromCurrentProfile();
        }).method_46433(0, 0).method_46437(120, 20).method_46431());
        this.mirrorButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.mirror_hand"), button7 -> {
            mirrorToOtherHand();
            loadFromCurrentProfile();
        }).method_46433(0, 0).method_46437(120, 20).method_46431());
        this.reloadTuningButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.export_tuning_helper_dev"), button8 -> {
            String string;
            boolean ok = PunchyTuningConfig.exportTuningHelperDevAndOpen();
            if (ok) {
                string = class_2561.method_43471("punchy.tuning.ui.status.export_tuning_helper_dev_done").getString();
            } else {
                string = class_2561.method_43471("punchy.tuning.ui.status.export_tuning_helper_dev_failed").getString();
            }
            this.statusMessage = string;
        }).method_46433(0, 0).method_46437(120, 20).method_46431());
        this.profileNameBox = new class_342(this.field_22793, 0, 0, 220, 20, class_2561.method_43471("punchy.tuning.ui.profile_name"));
        this.profileNameBox.method_1880(64);
        addUiWidget(this.profileNameBox);
        this.saveProfileButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.save_profile"), button9 -> {
            String string;
            String name = this.profileNameBox.method_1882();
            if (name == null || name.isBlank()) {
                name = defaultProfileName();
                this.profileNameBox.method_1852(name);
            }
            boolean ok = TuningProfileManager.saveCurrentAs(name);
            if (ok) {
                string = class_2561.method_43469("punchy.tuning.ui.status.profile_saved", new Object[]{name}).getString();
            } else {
                string = class_2561.method_43471("punchy.tuning.ui.status.profile_save_failed").getString();
            }
            this.statusMessage = string;
            if (ok) {
                this.hasUnsavedProfileChanges = false;
            }
            refreshProfiles(true);
        }).method_46433(0, 0).method_46437(106, 20).method_46431());
        this.loadProfileButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.load_profile"), button10 -> {
            String string;
            String name = selectedProfileName();
            boolean ok = name != null && TuningProfileManager.loadProfile(name);
            if (ok) {
                string = class_2561.method_43469("punchy.tuning.ui.status.profile_loaded", new Object[]{name}).getString();
            } else {
                string = class_2561.method_43471("punchy.tuning.ui.status.profile_load_failed").getString();
            }
            this.statusMessage = string;
            if (ok) {
                this.hasUnsavedProfileChanges = false;
            }
            loadFromCurrentProfile();
            refreshProfiles(false);
        }).method_46433(0, 0).method_46437(106, 20).method_46431());
        this.deleteProfileButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.delete_profile"), button11 -> {
            openDeleteProfileConfirm();
        }).method_46433(0, 0).method_46437(220, 20).method_46431());
        this.importProfileButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("punchy.tuning.ui.open_profiles_folder"), button12 -> {
            String string;
            boolean ok = openProfilesFolder();
            if (ok) {
                string = class_2561.method_43471("punchy.tuning.ui.status.profiles_folder_opened").getString();
            } else {
                string = class_2561.method_43471("punchy.tuning.ui.status.profiles_folder_open_failed").getString();
            }
            this.statusMessage = string;
        }).method_46433(0, 0).method_46437(220, 20).method_46431());
        this.doneButton = addUiWidget(class_4185.method_46430(class_2561.method_43471("gui.done"), button13 -> {
            method_25419();
        }).method_46433(0, 0).method_46437(220, 20).method_46431());
        for (int i = 0; i < 6; i++) {
            int row = i;
            class_4185 rowButton = addUiWidget(class_4185.method_46430(class_2561.method_43473(), button14 -> {
                selectProfileAtVisibleRow(row);
            }).method_46433(0, 0).method_46437(220, 14).method_46431());
            this.profileRowButtons.add(rowButton);
        }
        normalizeScopeForTarget();
        this.hasUnsavedProfileChanges = false;
        refreshProfiles(true);
        autoKindFromHeldItem();
        refreshHeaderButtons();
        loadFromCurrentProfile();
        layoutWidgets();
    }

    public boolean method_25421() {
        return false;
    }

    public void method_25419() {
        if (this.hasUnsavedProfileChanges) {
            confirmDiscardUnsavedProfileChanges();
        } else {
            discardUnsavedChangesAndClose();
        }
    }

    private void discardUnsavedChangesAndClose() {
        class_310 client = this.field_22787;
        if (client != null) {
            PunchyTuningConfig.reloadActiveProfileFast(client.method_1478());
            if (this.parentScreen != null) {
                client.method_1507(this.parentScreen);
                return;
            }
        }
        super.method_25419();
    }

    private void confirmDiscardUnsavedProfileChanges() {
        class_310 client = this.field_22787;
        if (client == null) {
            discardUnsavedChangesAndClose();
        } else {
            client.method_1507(new class_410(result -> {
                if (result) {
                    this.hasUnsavedProfileChanges = false;
                    discardUnsavedChangesAndClose();
                } else {
                    client.method_1507(this);
                }
            }, class_2561.method_43471("punchy.tuning.ui.confirm_unsaved_profile.title"), class_2561.method_43471("punchy.tuning.ui.confirm_unsaved_profile.message"), class_2561.method_43471("punchy.tuning.ui.confirm_unsaved_profile.ignore_exit"), class_2561.method_43471("punchy.tuning.ui.confirm_unsaved_profile.cancel")));
        }
    }

    public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
        applyFocusVisibility();
        if (this.activeSlider != null) {
            this.activeSlider.drawStyled(guiGraphics, mouseX, mouseY);
            return;
        }
        guiGraphics.method_25294(0, 0, this.field_22789, this.field_22790, -2013265920);
        drawWorkbenchPanels(guiGraphics);
        List<class_4185> styledButtons = new ArrayList<>();
        List<class_2561> originalButtonMessages = new ArrayList<>();
        List<FloatSlider> styledSliders = new ArrayList<>();
        List<class_2561> originalSliderMessages = new ArrayList<>();
        Iterator<class_339> it = this.uiWidgets.iterator();
        while (it.hasNext()) {
            class_4185 class_4185Var = (class_339) it.next();
            if (class_4185Var instanceof class_4185) {
                class_4185 button = class_4185Var;
                if (button.field_22764) {
                    styledButtons.add(button);
                    originalButtonMessages.add(button.method_25369());
                    button.method_25355(class_2561.method_43473());
                }
            }
            if (class_4185Var instanceof FloatSlider) {
                FloatSlider slider = (FloatSlider) class_4185Var;
                if (slider.field_22764) {
                    styledSliders.add(slider);
                    originalSliderMessages.add(slider.method_25369());
                    slider.method_25355(class_2561.method_43473());
                }
            }
        }
        super.method_25394(guiGraphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < styledButtons.size(); i++) {
            styledButtons.get(i).method_25355(originalButtonMessages.get(i));
        }
        for (int i2 = 0; i2 < styledSliders.size(); i2++) {
            styledSliders.get(i2).method_25355(originalSliderMessages.get(i2));
        }
        renderStyledSliders(guiGraphics, mouseX, mouseY);
        renderStyledButtons(guiGraphics, mouseX, mouseY);
        guiGraphics.method_27535(this.field_22793, this.field_22785, 14, 4, 16777215);
        guiGraphics.method_27535(this.field_22793, class_2561.method_43469("punchy.tuning.ui.held_item", new Object[]{currentHeldItemString()}), 14, this.field_22790 - 28, 14737632);
        renderKindButtonContent(guiGraphics);
        renderProfileList(guiGraphics);
        renderProfileScrollbar(guiGraphics);
        renderMainScrollbar(guiGraphics);
        guiGraphics.method_25303(this.field_22793, this.statusMessage == null ? "" : this.statusMessage, this.profilesTitleX, this.field_22790 - 14, 11075504);
        drawDeleteProfileConfirmOverlay(guiGraphics, mouseX, mouseY);
    }

    private void renderStyledSliders(class_332 guiGraphics, int mouseX, int mouseY) {
        for (FloatSlider slider : this.sliders) {
            if (slider.field_22764) {
                slider.drawStyled(guiGraphics, mouseX, mouseY);
            }
        }
    }

    private void renderStyledButtons(class_332 guiGraphics, int mouseX, int mouseY) {
        for (class_339 widget : this.uiWidgets) {
            if (widget instanceof class_4185) {
                class_4185 button = (class_4185) widget;
                if (button.field_22764) {
                    boolean destructive = button == this.deleteProfileButton;
                    drawBlacklistButtonStyle(guiGraphics, button, mouseX, mouseY, destructive);
                }
            }
        }
    }

    private void drawBlacklistButtonStyle(class_332 guiGraphics, class_4185 button, int mouseX, int mouseY, boolean destructive) {
        int fill;
        int border;
        int textColor;
        int left = button.method_46426();
        int top = button.method_46427();
        int right = left + button.method_25368();
        int bottom = top + button.method_25364();
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
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
        guiGraphics.method_25294(left, top, right, bottom, fill);
        guiGraphics.method_25294(left, top, right, top + 1, border);
        guiGraphics.method_25294(left, bottom - 1, right, bottom, border);
        guiGraphics.method_25294(left, top, left + 1, bottom, border);
        guiGraphics.method_25294(right - 1, top, right, bottom, border);
        String label = button.method_25369() == null ? "" : button.method_25369().getString();
        int maxTextWidth = Math.max(8, button.method_25368() - 12);
        String renderLabel = resolveScrollableButtonLabel(label, maxTextWidth, hovered);
        int textX = left + ((button.method_25368() - this.field_22793.method_1727(renderLabel)) / 2);
        int textY = top + ((button.method_25364() - 8) / 2);
        guiGraphics.method_25303(this.field_22793, renderLabel, textX, textY, textColor);
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

    private void drawWorkbenchPanels(class_332 guiGraphics) {
        guiGraphics.method_25294(this.mainPanelLeft, this.mainPanelTop, this.mainPanelRight, this.mainPanelBottom, -267381478);
        guiGraphics.method_25294(this.mainPanelLeft, this.mainPanelTop, this.mainPanelRight, this.mainPanelTop + 1, -11641751);
        guiGraphics.method_25294(this.mainPanelLeft, this.mainPanelBottom - 1, this.mainPanelRight, this.mainPanelBottom, -11641751);
        guiGraphics.method_25294(this.mainPanelLeft, this.mainPanelTop, this.mainPanelLeft + 1, this.mainPanelBottom, -11641751);
        guiGraphics.method_25294(this.mainPanelRight - 1, this.mainPanelTop, this.mainPanelRight, this.mainPanelBottom, -11641751);
        guiGraphics.method_25294(this.leftPanelLeft, this.leftPanelTop, this.leftPanelRight, this.leftPanelBottom, 857944122);
        guiGraphics.method_25294(this.leftPanelLeft, this.leftPanelTop, this.leftPanelRight, this.leftPanelTop + 1, 1430145621);
        guiGraphics.method_25294(this.leftPanelLeft, this.leftPanelBottom - 1, this.leftPanelRight, this.leftPanelBottom, 1430145621);
        guiGraphics.method_25294(this.leftPanelLeft, this.leftPanelTop, this.leftPanelLeft + 1, this.leftPanelBottom, 1430145621);
        guiGraphics.method_25294(this.leftPanelRight - 1, this.leftPanelTop, this.leftPanelRight, this.leftPanelBottom, 1430145621);
        guiGraphics.method_25294(this.rightPanelLeft, this.rightPanelTop, this.rightPanelRight, this.rightPanelBottom, 857944122);
        guiGraphics.method_25294(this.rightPanelLeft, this.rightPanelTop, this.rightPanelRight, this.rightPanelTop + 1, 1430145621);
        guiGraphics.method_25294(this.rightPanelLeft, this.rightPanelBottom - 1, this.rightPanelRight, this.rightPanelBottom, 1430145621);
        guiGraphics.method_25294(this.rightPanelLeft, this.rightPanelTop, this.rightPanelLeft + 1, this.rightPanelBottom, 1430145621);
        guiGraphics.method_25294(this.rightPanelRight - 1, this.rightPanelTop, this.rightPanelRight, this.rightPanelBottom, 1430145621);
    }

    public void method_25420(class_332 guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.activeSlider == null) {
            super.method_25420(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    public void method_52752(class_332 guiGraphics) {
        if (this.activeSlider == null) {
            super.method_52752(guiGraphics);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public void method_25393() throws MatchException {
        super.method_25393();
        autoKindFromHeldItem();
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public boolean method_25404(int keyCode, int scanCode, int modifiers) throws MatchException {
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
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public boolean method_25402(double mouseX, double mouseY, int button) throws MatchException {
        if (this.deleteProfileConfirmOpen) {
            if (button == 0) {
                if (isMouseInside(mouseX, mouseY, this.deleteConfirmButtonLeft, this.deleteConfirmButtonTop, this.deleteConfirmButtonRight, this.deleteConfirmButtonBottom)) {
                    confirmDeleteProfile();
                    return true;
                }
                if (isMouseInside(mouseX, mouseY, this.deleteCancelButtonLeft, this.deleteCancelButtonTop, this.deleteCancelButtonRight, this.deleteCancelButtonBottom)) {
                    cancelDeleteProfileConfirm();
                    return true;
                }
                return true;
            }
            return true;
        }
        if (button == 0 && this.activeSlider == null) {
            if (this.maxScroll > 0 && isMouseOverMainScrollbar(mouseX, mouseY)) {
                this.draggingMainScrollbar = true;
                updateMainScrollFromMouse(mouseY);
                return true;
            }
            if (maxProfileScroll() > 0 && isMouseOverProfileScrollbar(mouseX, mouseY)) {
                this.draggingProfileScrollbar = true;
                updateProfileScrollFromMouse(mouseY);
                return true;
            }
        }
        boolean handled = super.method_25402(mouseX, mouseY, button);
        if (button == 0) {
            this.activeSlider = null;
            Iterator<FloatSlider> it = this.sliders.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                FloatSlider slider = it.next();
                if (slider.field_22764 && slider.field_22763 && slider.method_25405(mouseX, mouseY)) {
                    this.activeSlider = slider;
                    break;
                }
            }
            if (this.activeSlider == null && trySelectProfileFromList(mouseX, mouseY)) {
                handled = true;
            }
            applyFocusVisibility();
        }
        return handled;
    }

    public boolean method_25406(double mouseX, double mouseY, int button) {
        if (this.deleteProfileConfirmOpen) {
            return true;
        }
        if (button == 0) {
            this.draggingMainScrollbar = false;
            this.draggingProfileScrollbar = false;
        }
        boolean handled = super.method_25406(mouseX, mouseY, button);
        if (button == 0 && this.activeSlider != null) {
            this.activeSlider = null;
            applyFocusVisibility();
        }
        return handled;
    }

    public boolean method_25403(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.deleteProfileConfirmOpen) {
            return true;
        }
        if (button == 0) {
            if (this.draggingMainScrollbar) {
                updateMainScrollFromMouse(mouseY);
                return true;
            }
            if (this.draggingProfileScrollbar) {
                updateProfileScrollFromMouse(mouseY);
                return true;
            }
        }
        return super.method_25403(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.deleteProfileConfirmOpen) {
            return true;
        }
        if (this.activeSlider == null && verticalAmount != 0.0d && isMouseOverProfileList(mouseX, mouseY)) {
            int visible = visibleProfileRows();
            int maxListScroll = Math.max(0, this.profiles.size() - visible);
            if (maxListScroll > 0) {
                this.profileListScroll = class_3532.method_15340(this.profileListScroll - ((int) Math.signum(verticalAmount)), 0, maxListScroll);
                updateProfileRowButtons();
                return true;
            }
        }
        if (this.activeSlider == null && this.maxScroll > 0 && verticalAmount != 0.0d) {
            this.scrollOffset = class_3532.method_15340(this.scrollOffset - ((int) Math.round(verticalAmount * 24.0d)), 0, this.maxScroll);
            layoutWidgets();
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private void autoKindFromHeldItem() throws MatchException {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null) {
            return;
        }
        class_1799 stack = mc.field_1724.method_5998(this.leftHand ? class_1268.field_5810 : class_1268.field_5808);
        if (stack == null || stack.method_7960()) {
            class_1799 other = mc.field_1724.method_5998(this.leftHand ? class_1268.field_5808 : class_1268.field_5810);
            if (other != null && !other.method_7960()) {
                this.leftHand = !this.leftHand;
                stack = other;
            }
        }
        if (stack != null && !stack.method_7960()) {
            VanillaFirstPersonItemLayer.ToolKind resolved = resolveTuningKind(stack);
            if (isFishingLineEditor() && !isFishingRodStack(stack)) {
                this.editorTarget = EditorTarget.ITEM;
                normalizeScopeForTarget();
                loadFromCurrentProfile();
            }
            if (resolved != this.selectedKind) {
                this.selectedKind = resolved;
                refreshHeaderButtons();
                loadFromCurrentProfile();
                return;
            }
            refreshHeaderButtons();
            return;
        }
        refreshHeaderButtons();
    }

    private String currentHeldItemString() {
        if (isFishingLineEditor()) {
            return "fishing line proxy: rod";
        }
        if (isArmSpecificEmptyHandTarget()) {
            return class_2561.method_43471("punchy.tuning.selection.empty_hand").getString();
        }
        class_2960 id = heldItemId();
        return id == null ? class_2561.method_43471("punchy.tuning.selection.none_item").getString() : id.toString();
    }

    private class_1799 heldItemStack() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null) {
            return class_1799.field_8037;
        }
        class_1799 stack = mc.field_1724.method_5998(this.leftHand ? class_1268.field_5810 : class_1268.field_5808);
        return stack == null ? class_1799.field_8037 : stack;
    }

    private VanillaFirstPersonItemLayer.ToolKind resolveTuningKind(class_1799 stack) {
        if (SpearStateMachine.isSpearStack(stack)) {
            return VanillaFirstPersonItemLayer.ToolKind.SPEAR;
        }
        return ToolKindResolver.resolveItemKind(stack, this.selectedKind);
    }

    private FloatSlider addSlider(int x, int y, int width, int height, String key, float min, float max) {
        FloatSlider slider = new FloatSlider(x, y, width, height, key, min, max, value -> {
            if (!this.syncing) {
                applyToCurrentProfile();
            }
        });
        this.sliders.add(slider);
        addUiWidget(slider);
        return slider;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private void loadFromCurrentProfile() throws MatchException {
        configureSliderRangesForCurrentTarget();
        ToolTuning.Profile itemProfile = isItemEditor() ? getCurrentProfile() : null;
        ArmToolTuning.Profile armProfile = isArmEditor() ? getCurrentArmProfile() : null;
        FishingLineProxyTuning.Profile fishingLineProfile = isFishingLineEditor() ? getCurrentFishingLineProfile() : null;
        boolean has = (itemProfile == null && armProfile == null && fishingLineProfile == null) ? false : true;
        boolean hideArmGlobalRotation = shouldHideArmGlobalRotationControls();
        this.syncing = true;
        if (itemProfile != null) {
            this.offX.setFloat(itemProfile.offX);
            this.offY.setFloat(itemProfile.offY);
            this.offZ.setFloat(itemProfile.offZ);
            this.rotX.setFloat(itemProfile.rotX);
            this.rotY.setFloat(itemProfile.rotY);
            this.rotZ.setFloat(itemProfile.rotZ);
            this.scale.setFloat(itemProfile.scale);
            this.pivotX.setFloat(itemProfile.pivotX);
            this.pivotY.setFloat(itemProfile.pivotY);
            this.pivotZ.setFloat(itemProfile.pivotZ);
        } else if (armProfile != null) {
            ArmToolTuning.Profile base = armEditorBaseProfile();
            this.offX.setFloat(armProfile.offX - base.offX);
            this.offY.setFloat(armProfile.offY - base.offY);
            this.offZ.setFloat(armProfile.offZ - base.offZ);
            this.rotX.setFloat(armProfile.rotX - base.rotX);
            this.rotY.setFloat(armProfile.rotY - base.rotY);
            this.rotZ.setFloat(armProfile.rotZ - base.rotZ);
            this.scale.setFloat(armProfile.scale);
            this.pivotX.setFloat(0.0f);
            this.pivotY.setFloat(0.0f);
            this.pivotZ.setFloat(0.0f);
        } else if (fishingLineProfile != null) {
            this.offX.setFloat(fishingLineProfile.offX);
            this.offY.setFloat(fishingLineProfile.offY);
            this.offZ.setFloat(fishingLineProfile.offZ);
            this.rotX.setFloat(fishingLineProfile.rotX);
            this.rotY.setFloat(fishingLineProfile.rotY);
            this.rotZ.setFloat(fishingLineProfile.rotZ);
            this.scale.setFloat(fishingLineProfile.scale);
            this.pivotX.setFloat(0.0f);
            this.pivotY.setFloat(0.0f);
            this.pivotZ.setFloat(0.0f);
        }
        this.syncing = false;
        for (FloatSlider slider : this.sliders) {
            slider.field_22763 = has;
        }
        if (isArmEditor()) {
            this.rotX.field_22763 = !hideArmGlobalRotation && has;
            this.rotY.field_22763 = !hideArmGlobalRotation && has;
            this.rotZ.field_22763 = !hideArmGlobalRotation && has;
            this.rotX.field_22764 = !hideArmGlobalRotation;
            this.rotY.field_22764 = !hideArmGlobalRotation;
            this.rotZ.field_22764 = !hideArmGlobalRotation;
            this.pivotX.field_22763 = false;
            this.pivotY.field_22763 = false;
            this.pivotZ.field_22763 = false;
            this.pivotX.field_22764 = false;
            this.pivotY.field_22764 = false;
            this.pivotZ.field_22764 = false;
            if (this.activeSlider != null) {
                if (this.activeSlider.isPivotSlider() || (hideArmGlobalRotation && this.activeSlider.isRotationSlider())) {
                    this.activeSlider = null;
                    return;
                }
                return;
            }
            return;
        }
        if (isFishingLineEditor()) {
            this.pivotX.field_22763 = false;
            this.pivotY.field_22763 = false;
            this.pivotZ.field_22763 = false;
            this.pivotX.field_22764 = false;
            this.pivotY.field_22764 = false;
            this.pivotZ.field_22764 = false;
            if (this.activeSlider != null && this.activeSlider.isPivotSlider()) {
                this.activeSlider = null;
            }
        }
    }

    private void configureSliderRangesForCurrentTarget() {
        if (isFishingLineEditor()) {
            this.offX.setRange(-32.0f, 32.0f);
            this.offY.setRange(-32.0f, 32.0f);
            this.offZ.setRange(-32.0f, 32.0f);
            this.rotX.setRange(-720.0f, 720.0f);
            this.rotY.setRange(-720.0f, 720.0f);
            this.rotZ.setRange(-720.0f, 720.0f);
            this.scale.setRange(0.01f, 10.0f);
            this.pivotX.setRange(-1.0f, 1.0f);
            this.pivotY.setRange(-1.0f, 1.0f);
            this.pivotZ.setRange(-1.0f, 1.0f);
            return;
        }
        this.offX.setRange(-2.0f, 2.0f);
        this.offY.setRange(-2.0f, 2.0f);
        this.offZ.setRange(-2.0f, 2.0f);
        this.rotX.setRange(-180.0f, 180.0f);
        this.rotY.setRange(-180.0f, 180.0f);
        this.rotZ.setRange(-180.0f, 180.0f);
        this.scale.setRange(0.1f, 3.0f);
        this.pivotX.setRange(-1.0f, 1.0f);
        this.pivotY.setRange(-1.0f, 1.0f);
        this.pivotZ.setRange(-1.0f, 1.0f);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private ToolTuning.Profile getCurrentProfile() throws MatchException {
        boolean targetLeftArm = selectedHandTargetsLeftArm();
        switch (this.scope) {
            case GLOBAL:
                return ToolTuning.getGlobal(targetLeftArm);
            case SPECIFIC:
                class_1799 held = heldItemStack();
                if (held == null || held.method_7960()) {
                    return null;
                }
                return ToolTuning.getSpecificProfile(held, this.selectedKind, targetLeftArm);
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private void applyToCurrentProfile() throws MatchException {
        ToolTuning.Profile specificProfile;
        ToolTuning.Profile specificProfile2;
        if (isFishingLineEditor()) {
            FishingLineProxyTuning.Profile profile = getCurrentFishingLineProfile();
            if (profile == null) {
                this.statusMessage = "Hold a fishing rod to edit fishing line proxy";
                return;
            }
            profile.offX = this.offX.getFloat();
            profile.offY = this.offY.getFloat();
            profile.offZ = this.offZ.getFloat();
            profile.rotX = this.rotX.getFloat();
            profile.rotY = this.rotY.getFloat();
            profile.rotZ = this.rotZ.getFloat();
            profile.scale = this.scale.getFloat();
            FishingLineProxyTuning.apply(FishingLineProxyTuning.Kind.ROD, selectedHandTargetsLeftArm(), profile);
            this.hasUnsavedProfileChanges = true;
            return;
        }
        if (isArmEditor()) {
            ArmToolTuning.Profile profile2 = getCurrentArmProfile();
            if (profile2 == null) {
                this.statusMessage = class_2561.method_43471("punchy.tuning.toast.hold_item_edit_specific").getString();
                return;
            }
            ArmToolTuning.Profile base = armEditorBaseProfile();
            profile2.offX = base.offX + this.offX.getFloat();
            profile2.offY = base.offY + this.offY.getFloat();
            profile2.offZ = base.offZ + this.offZ.getFloat();
            profile2.rotX = base.rotX + this.rotX.getFloat();
            profile2.rotY = base.rotY + this.rotY.getFloat();
            profile2.rotZ = base.rotZ + this.rotZ.getFloat();
            profile2.scale = this.scale.getFloat();
            currentArmKind();
            boolean targetLeftArm = selectedHandTargetsLeftArm();
            switch (this.scope) {
                case GLOBAL:
                    ArmToolTuning.commitGlobal(targetLeftArm);
                    break;
                case SPECIFIC:
                    class_2960 id = heldItemId();
                    if (id == null) {
                        ArmToolTuning.commitKind(PlayerArmModelLayer.ToolKind.EMPTY_HAND, targetLeftArm);
                    } else {
                        ArmToolTuning.commitSpecific(id, targetLeftArm);
                    }
                    break;
            }
            this.hasUnsavedProfileChanges = true;
            return;
        }
        ToolTuning.Profile profile3 = getCurrentProfile();
        if (profile3 == null) {
            this.statusMessage = class_2561.method_43471("punchy.tuning.toast.hold_item_edit_specific").getString();
            return;
        }
        profile3.offX = this.offX.getFloat();
        profile3.offY = this.offY.getFloat();
        profile3.offZ = this.offZ.getFloat();
        profile3.rotX = this.rotX.getFloat();
        profile3.rotY = this.rotY.getFloat();
        profile3.rotZ = this.rotZ.getFloat();
        profile3.scale = this.scale.getFloat();
        profile3.pivotX = this.pivotX.getFloat();
        profile3.pivotY = this.pivotY.getFloat();
        profile3.pivotZ = this.pivotZ.getFloat();
        profile3.recomputeRadians();
        boolean targetLeftArm2 = selectedHandTargetsLeftArm();
        switch (this.scope) {
            case GLOBAL:
                ToolTuning.commitGlobal(targetLeftArm2);
                break;
            case SPECIFIC:
                class_2960 id2 = heldItemId();
                if (id2 == null) {
                    return;
                }
                class_1799 held = heldItemStack();
                Integer cmd = null;
                String renamed = null;
                if (held != null && !held.method_7960()) {
                    cmd = ToolTuning.resolveCustomModelDataForDebug(held);
                    renamed = ToolTuning.resolveStackRenamedKeyForDebug(held);
                }
                if (held != null && !held.method_7960()) {
                    specificProfile = ToolTuning.getSpecificProfile(held, this.selectedKind, false);
                } else {
                    specificProfile = ToolTuning.getSpecificProfile(id2, this.selectedKind, false);
                }
                ToolTuning.Profile right = specificProfile;
                if (held != null && !held.method_7960()) {
                    specificProfile2 = ToolTuning.getSpecificProfile(held, this.selectedKind, true);
                } else {
                    specificProfile2 = ToolTuning.getSpecificProfile(id2, this.selectedKind, true);
                }
                ToolTuning.Profile left = specificProfile2;
                ToolTuning.applySpecific(id2, cmd, this.selectedKind, renamed, (String) null, (List<PunchyTuningConfig.CustomAnimationEntry>) null, (List<PunchyTuningConfig.CustomAnimationEntry>) null, (List<String>) null, (List<PunchyTuningConfig.BetterCombatEntry>) null, right, left);
                break;
                break;
        }
        this.hasUnsavedProfileChanges = true;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private void resetCurrentProfile() throws MatchException {
        if (isFishingLineEditor()) {
            FishingLineProxyTuning.reset(FishingLineProxyTuning.Kind.ROD, selectedHandTargetsLeftArm());
            loadFromCurrentProfile();
            this.hasUnsavedProfileChanges = true;
            this.statusMessage = class_2561.method_43471("punchy.tuning.ui.status.current_reset").getString();
            return;
        }
        if (isArmEditor()) {
            ArmToolTuning.Profile profile = getCurrentArmProfile();
            if (profile == null) {
                return;
            }
            this.syncing = true;
            this.offX.setFloat(0.0f);
            this.offY.setFloat(0.0f);
            this.offZ.setFloat(0.0f);
            this.rotX.setFloat(0.0f);
            this.rotY.setFloat(0.0f);
            this.rotZ.setFloat(0.0f);
            this.scale.setFloat(armEditorBaseProfile().scale);
            this.syncing = false;
            applyToCurrentProfile();
            this.statusMessage = class_2561.method_43471("punchy.tuning.ui.status.current_reset").getString();
            return;
        }
        ToolTuning.Profile profile2 = getCurrentProfile();
        if (profile2 == null) {
            return;
        }
        this.syncing = true;
        this.offX.setFloat(0.0f);
        this.offY.setFloat(0.0f);
        this.offZ.setFloat(0.0f);
        this.rotX.setFloat(0.0f);
        this.rotY.setFloat(0.0f);
        this.rotZ.setFloat(0.0f);
        this.scale.setFloat(1.0f);
        this.pivotX.setFloat(0.0f);
        this.pivotY.setFloat(0.0f);
        this.pivotZ.setFloat(0.0f);
        this.syncing = false;
        applyToCurrentProfile();
        this.statusMessage = class_2561.method_43471("punchy.tuning.ui.status.current_reset").getString();
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private void mirrorToOtherHand() throws MatchException {
        ToolTuning.Profile specificProfile;
        ToolTuning.Profile specificProfile2;
        if (isFishingLineEditor()) {
            FishingLineProxyTuning.Profile src = getCurrentFishingLineProfile();
            if (src == null) {
                return;
            }
            FishingLineProxyTuning.apply(FishingLineProxyTuning.Kind.ROD, !selectedHandTargetsLeftArm(), FishingLineProxyTuning.mirror(src));
            this.hasUnsavedProfileChanges = true;
            this.statusMessage = class_2561.method_43471("punchy.tuning.ui.status.mirrored").getString();
            return;
        }
        if (isArmEditor()) {
            ArmToolTuning.Profile src2 = getCurrentArmProfile();
            if (src2 == null) {
                return;
            }
            ArmToolTuning.Profile mirrored = src2.mirror();
            boolean other = !selectedHandTargetsLeftArm();
            PlayerArmModelLayer.ToolKind armKind = currentArmKind();
            switch (this.scope) {
                case GLOBAL:
                    ArmToolTuning.applyGlobal(other, mirrored);
                    ArmToolTuning.commitGlobal(other);
                    break;
                case SPECIFIC:
                    class_2960 id = heldItemId();
                    if (id == null) {
                        ArmToolTuning.apply(PlayerArmModelLayer.ToolKind.EMPTY_HAND, other, mirrored);
                        ArmToolTuning.commitKind(PlayerArmModelLayer.ToolKind.EMPTY_HAND, other);
                    } else {
                        ArmToolTuning.Profile right = other ? ArmToolTuning.getSpecificProfile(id, armKind, false) : mirrored;
                        ArmToolTuning.Profile left = other ? mirrored : ArmToolTuning.getSpecificProfile(id, armKind, true);
                        ArmToolTuning.applySpecific(id, armKind, right, left);
                    }
                    break;
            }
            this.statusMessage = class_2561.method_43471("punchy.tuning.ui.status.mirrored").getString();
            return;
        }
        ToolTuning.Profile src3 = getCurrentProfile();
        if (src3 == null) {
            return;
        }
        ToolTuning.Profile mirrored2 = src3.mirror();
        boolean other2 = !selectedHandTargetsLeftArm();
        switch (this.scope) {
            case GLOBAL:
                ToolTuning.applyGlobal(other2, mirrored2);
                ToolTuning.commitGlobal(other2);
                break;
            case SPECIFIC:
                class_2960 id2 = heldItemId();
                if (id2 == null) {
                    return;
                }
                class_1799 held = heldItemStack();
                Integer cmd = null;
                String renamed = null;
                if (held != null && !held.method_7960()) {
                    cmd = ToolTuning.resolveCustomModelDataForDebug(held);
                    renamed = ToolTuning.resolveStackRenamedKeyForDebug(held);
                }
                if (other2) {
                    if (held != null && !held.method_7960()) {
                        specificProfile = ToolTuning.getSpecificProfile(held, this.selectedKind, false);
                    } else {
                        specificProfile = ToolTuning.getSpecificProfile(id2, this.selectedKind, false);
                    }
                } else {
                    specificProfile = mirrored2;
                }
                ToolTuning.Profile right2 = specificProfile;
                if (other2) {
                    specificProfile2 = mirrored2;
                } else if (held != null && !held.method_7960()) {
                    specificProfile2 = ToolTuning.getSpecificProfile(held, this.selectedKind, true);
                } else {
                    specificProfile2 = ToolTuning.getSpecificProfile(id2, this.selectedKind, true);
                }
                ToolTuning.Profile left2 = specificProfile2;
                ToolTuning.applySpecific(id2, cmd, this.selectedKind, renamed, (String) null, (List<PunchyTuningConfig.CustomAnimationEntry>) null, (List<PunchyTuningConfig.CustomAnimationEntry>) null, (List<String>) null, (List<PunchyTuningConfig.BetterCombatEntry>) null, right2, left2);
                break;
                break;
        }
        this.statusMessage = class_2561.method_43471("punchy.tuning.ui.status.mirrored").getString();
    }

    private class_2561 scopeLabel() {
        return class_2561.method_43471(this.scope.key);
    }

    private class_2561 handLabel() {
        if (this.leftHand) {
            return class_2561.method_43471("punchy.tuning.ui.hand.off");
        }
        return class_2561.method_43471("punchy.tuning.ui.hand.main");
    }

    private class_2561 kindLabel() {
        if (isFishingLineEditor()) {
            return class_2561.method_43470("Editing fishing line proxy: rod");
        }
        return class_2561.method_43469("punchy.tuning.ui.editing_item", new Object[]{currentHeldItemString()});
    }

    private void renderKindButtonContent(class_332 guiGraphics) {
        if (this.kindButton == null || !this.kindButton.field_22764) {
            return;
        }
        int x = this.kindButton.method_46426();
        int y = this.kindButton.method_46427();
        int height = this.kindButton.method_25364();
        class_1799 stack = heldItemStack();
        if (stack != null && !stack.method_7960()) {
            int iconX = x + 5;
            int iconY = y + Math.max(0, (height - 16) / 2);
            renderItemIcon(guiGraphics, stack, iconX, iconY);
        }
    }

    private void renderItemIcon(class_332 guiGraphics, class_1799 stack, int x, int y) {
        if (stack == null || stack.method_7960() || guiGraphics == null) {
            return;
        }
        guiGraphics.method_51427(stack, x, y);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private class_2561 targetLabel() throws MatchException {
        switch (this.editorTarget) {
            case ITEM:
                return class_2561.method_43471("punchy.tuning.ui.editor.item");
            case ARM:
                return class_2561.method_43471("punchy.tuning.ui.editor.arm");
            case FISHING_LINE:
                return class_2561.method_43470("Line");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    private class_2960 heldItemId() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null) {
            return null;
        }
        class_1799 stack = mc.field_1724.method_5998(this.leftHand ? class_1268.field_5810 : class_1268.field_5808);
        if (stack == null || stack.method_7960()) {
            return null;
        }
        return class_7923.field_41178.method_10221(stack.method_7909());
    }

    private void refreshProfiles(boolean clampSelection) {
        this.profiles = TuningProfileManager.listProfiles();
        this.profileListScroll = class_3532.method_15340(this.profileListScroll, 0, Math.max(0, this.profiles.size() - visibleProfileRows()));
        if (this.profiles.isEmpty()) {
            this.selectedProfile = -1;
            if (this.profileNameBox != null) {
                this.profileNameBox.method_1852("");
            }
            updateProfileRowButtons();
            return;
        }
        if (this.selectedProfile < 0 || this.selectedProfile >= this.profiles.size() || clampSelection) {
            String active = TuningProfileManager.getActiveProfileName();
            if (active != null) {
                int activeIndex = this.profiles.indexOf(active);
                if (activeIndex >= 0) {
                    this.selectedProfile = activeIndex;
                }
            } else if (clampSelection) {
                this.selectedProfile = -1;
            }
            if (this.selectedProfile < 0) {
                if (!clampSelection) {
                    this.selectedProfile = 0;
                }
            } else {
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

    private String selectedProfileName() {
        if (this.selectedProfile < 0 || this.selectedProfile >= this.profiles.size()) {
            return null;
        }
        return this.profiles.get(this.selectedProfile);
    }

    private void refreshHeaderButtons() {
        if (this.scopeButton != null) {
            this.scopeButton.method_25355(scopeLabel());
            this.scopeButton.field_22763 = isArmEditor();
        }
        if (this.handButton != null) {
            this.handButton.method_25355(handLabel());
        }
        if (this.targetButton != null) {
            this.targetButton.method_25355(targetLabel());
        }
        if (this.kindButton != null) {
            this.kindButton.method_25355(kindButtonLabel());
            this.kindButton.field_22763 = false;
        }
    }

    private class_2561 kindButtonLabel() {
        String fullText = kindLabel().getString();
        int width = this.kindButton != null ? this.kindButton.method_25368() : 160;
        int availableWidth = Math.max(40, width - 34);
        String renderText = this.field_22793.method_27523(fullText, availableWidth);
        return class_2561.method_43470("    " + renderText);
    }

    private String defaultProfileName() {
        class_2960 id = heldItemId();
        String base = isFishingLineEditor() ? "fishing_line_proxy" : isArmSpecificEmptyHandTarget() ? "empty_hand" : id == null ? "tuning" : id.method_12832();
        String hand = this.leftHand ? "off" : "main";
        String sc = this.scope.name().toLowerCase(Locale.ROOT);
        return base + "_" + hand + "_" + sc;
    }

    private void layoutWidgets() {
        if (this.scopeButton == null) {
            return;
        }
        int availableWidth = Math.max(260, this.field_22789 - (24 * 2));
        int totalWidth = Math.min(800, availableWidth);
        int targetPanelWidth = class_3532.method_15340(totalWidth / 3, 220, 300);
        int leftWidth = (totalWidth - targetPanelWidth) - 12;
        this.compactLayout = leftWidth < 280;
        if (this.compactLayout) {
            leftWidth = totalWidth;
            targetPanelWidth = totalWidth;
        }
        int top = computeContainerTop(this.compactLayout, 20, 6);
        int leftX = (this.field_22789 - totalWidth) / 2;
        this.containerLeft = leftX;
        this.containerRight = leftX + totalWidth;
        int rightX = this.compactLayout ? leftX : leftX + leftWidth + 12;
        if (this.compactLayout) {
            int headerW = (leftWidth - (6 * 3)) / 4;
            place(this.scopeButton, leftX, top, headerW);
            place(this.handButton, leftX + headerW + 6, top, headerW);
            place(this.targetButton, leftX + ((headerW + 6) * 2), top, headerW);
            place(this.kindButton, leftX + ((headerW + 6) * 3), top, leftWidth - ((headerW + 6) * 3));
        } else {
            int scopeW = Math.min(120, Math.max(96, leftWidth / 4));
            int handW = Math.min(95, Math.max(80, leftWidth / 5));
            int targetW = Math.min(110, Math.max(92, leftWidth / 6));
            int kindW = (((leftWidth - scopeW) - handW) - targetW) - (6 * 3);
            place(this.scopeButton, leftX, top, scopeW);
            place(this.handButton, leftX + scopeW + 6, top, handW);
            place(this.targetButton, leftX + scopeW + 6 + handW + 6, top, targetW);
            place(this.kindButton, leftX + scopeW + 6 + handW + 6 + targetW + 6, top, kindW);
        }
        int y = top + 20 + 2;
        place(this.offX, leftX, y, leftWidth);
        int y2 = y + 20 + 2;
        place(this.offY, leftX, y2, leftWidth);
        int y3 = y2 + 20 + 2;
        place(this.offZ, leftX, y3, leftWidth);
        int y4 = y3 + 20 + 2;
        if (!shouldHideArmGlobalRotationControls()) {
            place(this.rotX, leftX, y4, leftWidth);
            int y5 = y4 + 20 + 2;
            place(this.rotY, leftX, y5, leftWidth);
            int y6 = y5 + 20 + 2;
            place(this.rotZ, leftX, y6, leftWidth);
            y4 = y6 + 20 + 2;
        }
        place(this.scale, leftX, y4, leftWidth);
        int y7 = y4 + 20 + 2;
        place(this.pivotX, leftX, y7, leftWidth);
        int y8 = y7 + 20 + 2;
        place(this.pivotY, leftX, y8, leftWidth);
        int y9 = y8 + 20 + 2;
        place(this.pivotZ, leftX, y9, leftWidth);
        int y10 = y9 + 20 + 6;
        int profileY = this.compactLayout ? y10 + 20 + 6 + 12 : top;
        int listHeight = (6 * 14) + (4 * 2);
        this.profileListX = rightX;
        this.profileListY = profileY - this.scrollOffset;
        this.profileListW = targetPanelWidth;
        this.profileListH = listHeight;
        layoutProfileRowButtons();
        int profileY2 = profileY + listHeight + 6;
        place(this.profileNameBox, rightX, profileY2, targetPanelWidth);
        int profileY3 = profileY2 + 20 + 4;
        int profileActionW = (targetPanelWidth - 6) / 2;
        place(this.saveProfileButton, rightX, profileY3, profileActionW);
        place(this.loadProfileButton, rightX + profileActionW + 6, profileY3, (targetPanelWidth - profileActionW) - 6);
        int profileY4 = profileY3 + 20 + 2;
        place(this.deleteProfileButton, rightX, profileY4, targetPanelWidth);
        int profileY5 = profileY4 + 20 + 14;
        int doneY = profileY5 + 20 + 2;
        int openFolderY = Math.max(profileY5, doneY - (20 + 8));
        place(this.importProfileButton, rightX, openFolderY, targetPanelWidth);
        place(this.doneButton, rightX, doneY, targetPanelWidth);
        int profileY6 = doneY + 20 + 6;
        int actionY = Math.max(y10, doneY);
        int actionW = (leftWidth - (6 * 3)) / 4;
        int lastActionW = (leftWidth - (actionW * 3)) - (6 * 3);
        place(this.resetButton, leftX, actionY, actionW);
        place(this.resetCurrentButton, leftX + actionW + 6, actionY, actionW);
        place(this.mirrorButton, leftX + ((actionW + 6) * 2), actionY, actionW);
        place(this.reloadTuningButton, leftX + ((actionW + 6) * 3), actionY, lastActionW);
        int leftBottom = actionY + 20 + 6;
        int contentBottom = Math.max(leftBottom, profileY6) + 8;
        this.maxScroll = Math.max(0, contentBottom - (this.field_22790 - 8));
        this.scrollOffset = class_3532.method_15340(this.scrollOffset, 0, this.maxScroll);
        if (this.maxScroll > 0) {
            reflowWithScroll(totalWidth, leftX, leftWidth, rightX, targetPanelWidth, top, 6, 20);
        } else {
            this.profilesTitleX = rightX;
            this.profilesTitleY = profileTitleY(top, this.compactLayout, leftBottom);
        }
        updateWorkbenchPanelRects(leftX, leftWidth, rightX, targetPanelWidth);
        updateMainScrollbarRects();
        updateProfileScrollbarRects();
        if (this.kindButton != null) {
            this.kindButton.method_25355(kindButtonLabel());
        }
    }

    private int computeContainerTop(boolean compact, int row, int gap) {
        int iMax;
        int headerHeight = row + 2;
        int sliderHeight = ((row + 2) * 9) + row + gap;
        int actionHeight = row + gap;
        int leftColumnHeight = headerHeight + sliderHeight + actionHeight;
        int listHeight = (6 * 14) + (4 * 2);
        int rightColumnHeight = listHeight + 6 + row + 4 + row + 2 + row + 14 + row + 2 + row + gap;
        if (compact) {
            iMax = leftColumnHeight + 12 + rightColumnHeight;
        } else {
            iMax = Math.max(leftColumnHeight, rightColumnHeight);
        }
        int contentHeight = iMax;
        return Math.max(12, (this.field_22790 - contentHeight) / 2);
    }

    private void reflowWithScroll(int totalWidth, int leftX, int leftWidth, int rightX, int panelWidth, int top, int gap, int row) {
        if (this.compactLayout) {
            int headerW = (leftWidth - (gap * 3)) / 4;
            place(this.scopeButton, leftX, top, headerW);
            place(this.handButton, leftX + headerW + gap, top, headerW);
            place(this.targetButton, leftX + ((headerW + gap) * 2), top, headerW);
            place(this.kindButton, leftX + ((headerW + gap) * 3), top, leftWidth - ((headerW + gap) * 3));
        } else {
            int scopeW = Math.min(120, Math.max(96, leftWidth / 4));
            int handW = Math.min(95, Math.max(80, leftWidth / 5));
            int targetW = Math.min(110, Math.max(92, leftWidth / 6));
            int kindW = (((leftWidth - scopeW) - handW) - targetW) - (gap * 3);
            place(this.scopeButton, leftX, top, scopeW);
            place(this.handButton, leftX + scopeW + gap, top, handW);
            place(this.targetButton, leftX + scopeW + gap + handW + gap, top, targetW);
            place(this.kindButton, leftX + scopeW + gap + handW + gap + targetW + gap, top, kindW);
        }
        int y = top + row + 2;
        place(this.offX, leftX, y, leftWidth);
        int y2 = y + row + 2;
        place(this.offY, leftX, y2, leftWidth);
        int y3 = y2 + row + 2;
        place(this.offZ, leftX, y3, leftWidth);
        int y4 = y3 + row + 2;
        if (!shouldHideArmGlobalRotationControls()) {
            place(this.rotX, leftX, y4, leftWidth);
            int y5 = y4 + row + 2;
            place(this.rotY, leftX, y5, leftWidth);
            int y6 = y5 + row + 2;
            place(this.rotZ, leftX, y6, leftWidth);
            y4 = y6 + row + 2;
        }
        place(this.scale, leftX, y4, leftWidth);
        int y7 = y4 + row + 2;
        place(this.pivotX, leftX, y7, leftWidth);
        int y8 = y7 + row + 2;
        place(this.pivotY, leftX, y8, leftWidth);
        int y9 = y8 + row + 2;
        place(this.pivotZ, leftX, y9, leftWidth);
        int y10 = y9 + row + gap;
        int profileY = this.compactLayout ? y10 + row + gap + 12 : top;
        int listHeight = (6 * 14) + (4 * 2);
        this.profileListX = rightX;
        this.profileListY = profileY - this.scrollOffset;
        this.profileListW = panelWidth;
        this.profileListH = listHeight;
        layoutProfileRowButtons();
        int profileY2 = profileY + listHeight + 6;
        place(this.profileNameBox, rightX, profileY2, panelWidth);
        int profileY3 = profileY2 + row + 4;
        int profileActionW = (panelWidth - gap) / 2;
        place(this.saveProfileButton, rightX, profileY3, profileActionW);
        place(this.loadProfileButton, rightX + profileActionW + gap, profileY3, (panelWidth - profileActionW) - gap);
        int profileY4 = profileY3 + row + 2;
        place(this.deleteProfileButton, rightX, profileY4, panelWidth);
        int profileY5 = profileY4 + row + 14;
        int doneY = profileY5 + row + 2;
        int openFolderY = Math.max(profileY5, doneY - (row + 8));
        place(this.importProfileButton, rightX, openFolderY, panelWidth);
        place(this.doneButton, rightX, doneY, panelWidth);
        int actionY = Math.max(y10, doneY);
        int actionW = (leftWidth - (gap * 3)) / 4;
        int lastActionW = (leftWidth - (actionW * 3)) - (gap * 3);
        place(this.resetButton, leftX, actionY, actionW);
        place(this.resetCurrentButton, leftX + actionW + gap, actionY, actionW);
        place(this.mirrorButton, leftX + ((actionW + gap) * 2), actionY, actionW);
        place(this.reloadTuningButton, leftX + ((actionW + gap) * 3), actionY, lastActionW);
        int leftBottom = actionY + row + gap;
        this.profilesTitleX = rightX;
        this.profilesTitleY = profileTitleY(top, this.compactLayout, leftBottom);
        if (this.kindButton != null) {
            this.kindButton.method_25355(kindButtonLabel());
        }
        recalcDeleteConfirmLayout();
    }

    private int profileTitleY(int top, boolean compact, int leftBottom) {
        int profileTop = compact ? leftBottom + 12 : top;
        return (profileTop - 12) - this.scrollOffset;
    }

    private void renderProfileList(class_332 guiGraphics) {
        if (this.profileListW <= 0 || this.profileListH <= 0) {
            return;
        }
        int x0 = this.profileListX;
        int y0 = this.profileListY;
        int x1 = this.profileListX + this.profileListW;
        int y1 = this.profileListY + this.profileListH;
        guiGraphics.method_25294(x0, y0, x1, y1, 1141641495);
        if (this.profiles.isEmpty()) {
            guiGraphics.method_27535(this.field_22793, class_2561.method_43471("punchy.tuning.selection.none_item"), x0 + 6, y0 + 6, 10526880);
        }
    }

    private void renderMainScrollbar(class_332 guiGraphics) {
        if (this.maxScroll <= 0) {
            return;
        }
        guiGraphics.method_25294(this.mainScrollX, this.mainScrollTop, this.mainScrollX + 4, this.mainScrollBottom, -1442840576);
        int trackH = Math.max(1, this.mainScrollBottom - this.mainScrollTop);
        int viewH = Math.max(1, this.field_22790 - 16);
        int contentH = viewH + this.maxScroll;
        int thumbH = Math.max(10, Math.round(trackH * (viewH / contentH)));
        int thumbMax = Math.max(0, trackH - thumbH);
        int thumbOffset = this.maxScroll == 0 ? 0 : Math.round(thumbMax * (this.scrollOffset / this.maxScroll));
        int thumbTop = this.mainScrollTop + thumbOffset;
        guiGraphics.method_25294(this.mainScrollX, thumbTop, this.mainScrollX + 4, Math.min(this.mainScrollBottom, thumbTop + thumbH), -1);
    }

    private void renderProfileScrollbar(class_332 guiGraphics) {
        int maxListScroll = maxProfileScroll();
        if (maxListScroll <= 0 || this.profileListH <= 0) {
            return;
        }
        guiGraphics.method_25294(this.profileScrollX, this.profileScrollTop, this.profileScrollX + 4, this.profileScrollBottom, -1442840576);
        int trackH = Math.max(1, this.profileScrollBottom - this.profileScrollTop);
        int visible = Math.max(1, visibleProfileRows());
        int total = Math.max(visible, this.profiles.size());
        int thumbH = Math.max(10, Math.round(trackH * (visible / total)));
        int thumbMax = Math.max(0, trackH - thumbH);
        int thumbOffset = maxListScroll == 0 ? 0 : Math.round(thumbMax * (this.profileListScroll / maxListScroll));
        int thumbTop = this.profileScrollTop + thumbOffset;
        guiGraphics.method_25294(this.profileScrollX, thumbTop, this.profileScrollX + 4, Math.min(this.profileScrollBottom, thumbTop + thumbH), -1);
    }

    private int visibleProfileRows() {
        if (this.profileListH <= 0) {
            return 1;
        }
        int rows = (this.profileListH - 8) / 14;
        return Math.max(1, rows);
    }

    private boolean isMouseOverProfileList(double mouseX, double mouseY) {
        return mouseX >= ((double) this.profileListX) && mouseX < ((double) (this.profileListX + this.profileListW)) && mouseY >= ((double) this.profileListY) && mouseY < ((double) (this.profileListY + this.profileListH));
    }

    private boolean isMouseOverMainScrollbar(double mouseX, double mouseY) {
        return mouseX >= ((double) this.mainScrollX) && mouseX < ((double) (this.mainScrollX + 4)) && mouseY >= ((double) this.mainScrollTop) && mouseY < ((double) this.mainScrollBottom);
    }

    private boolean isMouseOverProfileScrollbar(double mouseX, double mouseY) {
        return mouseX >= ((double) this.profileScrollX) && mouseX < ((double) (this.profileScrollX + 4)) && mouseY >= ((double) this.profileScrollTop) && mouseY < ((double) this.profileScrollBottom);
    }

    private boolean trySelectProfileFromList(double mouseX, double mouseY) {
        int index;
        if (!isMouseOverProfileList(mouseX, mouseY) || this.profiles.isEmpty()) {
            return false;
        }
        int relativeY = ((int) mouseY) - (this.profileListY + 4);
        int row = relativeY / 14;
        if (row < 0 || (index = this.profileListScroll + row) < 0 || index >= this.profiles.size()) {
            return false;
        }
        this.selectedProfile = index;
        String selectedName = selectedProfileName();
        if (selectedName != null) {
            this.profileNameBox.method_1852(selectedName);
        }
        updateProfileRowButtons();
        return true;
    }

    private void ensureSelectedVisibleInProfileList() {
        if (this.selectedProfile < 0) {
            return;
        }
        int visible = visibleProfileRows();
        if (this.selectedProfile < this.profileListScroll) {
            this.profileListScroll = this.selectedProfile;
        } else if (this.selectedProfile >= this.profileListScroll + visible) {
            this.profileListScroll = (this.selectedProfile - visible) + 1;
        }
        int maxListScroll = Math.max(0, this.profiles.size() - visible);
        this.profileListScroll = class_3532.method_15340(this.profileListScroll, 0, maxListScroll);
    }

    private void updateMainScrollbarRects() {
        this.mainScrollX = Math.max(6, this.containerRight + 6);
        if (this.mainScrollX > this.field_22789 - 8) {
            this.mainScrollX = this.field_22789 - 8;
        }
        this.mainScrollTop = 18;
        this.mainScrollBottom = Math.max(this.mainScrollTop + 12, this.field_22790 - 18);
    }

    private void updateProfileScrollbarRects() {
        this.profileScrollX = Math.max(this.profileListX + 2, (this.profileListX + this.profileListW) - 6);
        this.profileScrollTop = this.profileListY + 2;
        this.profileScrollBottom = this.profileListY + Math.max(6, this.profileListH - 2);
    }

    private void updateMainScrollFromMouse(double mouseY) {
        int trackH = Math.max(1, this.mainScrollBottom - this.mainScrollTop);
        int knobCenter = class_3532.method_15340(((int) Math.round(mouseY)) - this.mainScrollTop, 0, trackH);
        int newOffset = (int) Math.round((((double) knobCenter) / ((double) trackH)) * ((double) this.maxScroll));
        this.scrollOffset = class_3532.method_15340(newOffset, 0, this.maxScroll);
        layoutWidgets();
    }

    private void updateProfileScrollFromMouse(double mouseY) {
        int maxListScroll = maxProfileScroll();
        int trackH = Math.max(1, this.profileScrollBottom - this.profileScrollTop);
        int knobCenter = class_3532.method_15340(((int) Math.round(mouseY)) - this.profileScrollTop, 0, trackH);
        int newOffset = (int) Math.round((((double) knobCenter) / ((double) trackH)) * ((double) maxListScroll));
        this.profileListScroll = class_3532.method_15340(newOffset, 0, maxListScroll);
        updateProfileRowButtons();
    }

    private int maxProfileScroll() {
        return Math.max(0, this.profiles.size() - visibleProfileRows());
    }

    private void updateWorkbenchPanelRects(int leftX, int leftWidth, int rightX, int rightWidth) {
        int mainLeft = Math.min(leftX, rightX) - 10;
        int mainRight = Math.max(leftX + leftWidth, rightX + rightWidth) + 10;
        int topY = Math.min(this.scopeButton.method_46427(), this.profileListY) - 12;
        int bottomY = Math.max(this.reloadTuningButton.method_46427() + this.reloadTuningButton.method_25364(), this.doneButton.method_46427() + this.doneButton.method_25364()) + 10;
        this.mainPanelLeft = class_3532.method_15340(mainLeft, 4, this.field_22789 - 4);
        this.mainPanelRight = class_3532.method_15340(mainRight, 4, this.field_22789 - 4);
        this.mainPanelTop = class_3532.method_15340(topY, 4, this.field_22790 - 4);
        this.mainPanelBottom = class_3532.method_15340(bottomY, 4, this.field_22790 - 4);
        if (this.mainPanelBottom <= this.mainPanelTop + 2) {
            this.mainPanelBottom = Math.min(this.field_22790 - 4, this.mainPanelTop + 3);
        }
        this.leftPanelLeft = class_3532.method_15340(leftX - 4, 4, this.field_22789 - 4);
        this.leftPanelRight = class_3532.method_15340(leftX + leftWidth + 4, 4, this.field_22789 - 4);
        this.leftPanelTop = class_3532.method_15340(this.scopeButton.method_46427() - 6, 4, this.field_22790 - 4);
        this.leftPanelBottom = class_3532.method_15340(this.reloadTuningButton.method_46427() + this.reloadTuningButton.method_25364() + 6, 4, this.field_22790 - 4);
        if (this.leftPanelBottom <= this.leftPanelTop + 2) {
            this.leftPanelBottom = Math.min(this.field_22790 - 4, this.leftPanelTop + 3);
        }
        this.rightPanelLeft = class_3532.method_15340(rightX - 4, 4, this.field_22789 - 4);
        this.rightPanelRight = class_3532.method_15340(rightX + rightWidth + 4, 4, this.field_22789 - 4);
        this.rightPanelTop = class_3532.method_15340(this.profileListY - 6, 4, this.field_22790 - 4);
        this.rightPanelBottom = class_3532.method_15340(this.doneButton.method_46427() + this.doneButton.method_25364() + 6, 4, this.field_22790 - 4);
        if (this.rightPanelBottom <= this.rightPanelTop + 2) {
            this.rightPanelBottom = Math.min(this.field_22790 - 4, this.rightPanelTop + 3);
        }
        recalcDeleteConfirmLayout();
    }

    private void openDeleteProfileConfirm() {
        String name = selectedProfileName();
        if (name == null || name.isBlank()) {
            this.statusMessage = class_2561.method_43471("punchy.tuning.ui.status.profile_delete_failed").getString();
            return;
        }
        this.pendingDeleteProfileName = name;
        this.deleteProfileConfirmOpen = true;
        this.activeSlider = null;
        applyFocusVisibility();
        recalcDeleteConfirmLayout();
    }

    private void cancelDeleteProfileConfirm() {
        this.deleteProfileConfirmOpen = false;
        this.pendingDeleteProfileName = null;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private void confirmDeleteProfile() throws MatchException {
        String string;
        String name = this.pendingDeleteProfileName;
        boolean ok = (name == null || name.isBlank() || !TuningProfileManager.deleteProfile(name)) ? false : true;
        if (ok) {
            ok = TuningProfileManager.unloadActiveProfileToDefault();
            if (ok) {
                this.hasUnsavedProfileChanges = false;
                loadFromCurrentProfile();
            }
        }
        if (ok) {
            string = class_2561.method_43469("punchy.tuning.ui.status.profile_deleted", new Object[]{name}).getString();
        } else {
            string = class_2561.method_43471("punchy.tuning.ui.status.profile_delete_failed").getString();
        }
        this.statusMessage = string;
        cancelDeleteProfileConfirm();
        refreshProfiles(true);
    }

    private void recalcDeleteConfirmLayout() {
        int modalWidth = Math.min(380, Math.max(260, (this.mainPanelRight - this.mainPanelLeft) - 48));
        this.deleteModalLeft = this.mainPanelLeft + (((this.mainPanelRight - this.mainPanelLeft) - modalWidth) / 2);
        this.deleteModalTop = this.mainPanelTop + (((this.mainPanelBottom - this.mainPanelTop) - 112) / 2);
        this.deleteModalRight = this.deleteModalLeft + modalWidth;
        this.deleteModalBottom = this.deleteModalTop + 112;
        int buttonWidth = ((modalWidth - 32) - 8) / 2;
        this.deleteCancelButtonLeft = this.deleteModalLeft + 16;
        this.deleteCancelButtonTop = this.deleteModalBottom - 30;
        this.deleteCancelButtonRight = this.deleteCancelButtonLeft + buttonWidth;
        this.deleteCancelButtonBottom = this.deleteCancelButtonTop + 20;
        this.deleteConfirmButtonLeft = this.deleteCancelButtonRight + 8;
        this.deleteConfirmButtonTop = this.deleteCancelButtonTop;
        this.deleteConfirmButtonRight = this.deleteConfirmButtonLeft + buttonWidth;
        this.deleteConfirmButtonBottom = this.deleteConfirmButtonTop + 20;
    }

    private void drawDeleteProfileConfirmOverlay(class_332 guiGraphics, int mouseX, int mouseY) {
        if (!this.deleteProfileConfirmOpen) {
            return;
        }
        guiGraphics.method_51448().method_22903();
        guiGraphics.method_51448().method_46416(0.0f, 0.0f, 500.0f);
        guiGraphics.method_25294(this.mainPanelLeft, this.mainPanelTop, this.mainPanelRight, this.mainPanelBottom, -1610612736);
        guiGraphics.method_25294(this.deleteModalLeft, this.deleteModalTop, this.deleteModalRight, this.deleteModalBottom, -15657446);
        guiGraphics.method_25294(this.deleteModalLeft, this.deleteModalTop, this.deleteModalRight, this.deleteModalTop + 1, -11641751);
        guiGraphics.method_25294(this.deleteModalLeft, this.deleteModalBottom - 1, this.deleteModalRight, this.deleteModalBottom, -11641751);
        guiGraphics.method_25294(this.deleteModalLeft, this.deleteModalTop, this.deleteModalLeft + 1, this.deleteModalBottom, -11641751);
        guiGraphics.method_25294(this.deleteModalRight - 1, this.deleteModalTop, this.deleteModalRight, this.deleteModalBottom, -11641751);
        class_2561 title = nonBlankTranslatable("punchy.tuning.ui.confirm_delete_profile.title", "Voce realmente quer fazer isso?", new Object[0]);
        String targetName = this.pendingDeleteProfileName == null ? "-" : this.pendingDeleteProfileName;
        class_2561 message = nonBlankTranslatable("punchy.tuning.ui.confirm_delete_profile.message", "Delete profile \"" + targetName + "\"?", targetName);
        String titleText = title.getString();
        String messageText = message.getString();
        int titleX = this.deleteModalLeft + (((this.deleteModalRight - this.deleteModalLeft) - this.field_22793.method_1727(titleText)) / 2);
        int messageX = this.deleteModalLeft + (((this.deleteModalRight - this.deleteModalLeft) - this.field_22793.method_1727(messageText)) / 2);
        guiGraphics.method_25303(this.field_22793, titleText, titleX, this.deleteModalTop + 12, -1);
        guiGraphics.method_25303(this.field_22793, messageText, messageX, this.deleteModalTop + 34, -3747624);
        boolean hoverCancel = isMouseInside(mouseX, mouseY, this.deleteCancelButtonLeft, this.deleteCancelButtonTop, this.deleteCancelButtonRight, this.deleteCancelButtonBottom);
        boolean hoverConfirm = isMouseInside(mouseX, mouseY, this.deleteConfirmButtonLeft, this.deleteConfirmButtonTop, this.deleteConfirmButtonRight, this.deleteConfirmButtonBottom);
        drawModalButton(guiGraphics, this.deleteCancelButtonLeft, this.deleteCancelButtonTop, this.deleteCancelButtonRight, this.deleteCancelButtonBottom, hoverCancel, false, nonBlankTranslatable("gui.cancel", "Cancel", new Object[0]));
        drawModalButton(guiGraphics, this.deleteConfirmButtonLeft, this.deleteConfirmButtonTop, this.deleteConfirmButtonRight, this.deleteConfirmButtonBottom, hoverConfirm, true, nonBlankTranslatable("punchy.tuning.ui.confirm_delete_profile.confirm", "Delete", new Object[0]));
        guiGraphics.method_51448().method_22909();
    }

    private void drawModalButton(class_332 guiGraphics, int left, int top, int right, int bottom, boolean hovered, boolean destructive, class_2561 label) {
        int fill = destructive ? hovered ? -7459282 : -9690590 : hovered ? -12958126 : -13682107;
        int border = destructive ? -3048067 : -7429710;
        guiGraphics.method_25294(left, top, right, bottom, fill);
        guiGraphics.method_25294(left, top, right, top + 1, border);
        guiGraphics.method_25294(left, bottom - 1, right, bottom, border);
        guiGraphics.method_25294(left, top, left + 1, bottom, border);
        guiGraphics.method_25294(right - 1, top, right, bottom, border);
        String labelText = label.getString();
        int textX = left + (((right - left) - this.field_22793.method_1727(labelText)) / 2);
        int textY = top + (((bottom - top) - 8) / 2);
        guiGraphics.method_25303(this.field_22793, labelText, textX, textY, -1);
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

    private void layoutProfileRowButtons() {
        int rowButtonHeight = 14 - 1;
        int startY = this.profileListY + 4;
        int rowWidth = Math.max(10, this.profileListW);
        for (int i = 0; i < this.profileRowButtons.size(); i++) {
            class_4185 rowButton = this.profileRowButtons.get(i);
            rowButton.method_46421(this.profileListX);
            rowButton.method_46419(startY + (i * 14));
            rowButton.method_25358(rowWidth);
            rowButton.method_53533(rowButtonHeight);
        }
        updateProfileRowButtons();
    }

    private void updateProfileRowButtons() {
        int visible = visibleProfileRows();
        int startIndex = class_3532.method_15340(this.profileListScroll, 0, Math.max(0, this.profiles.size() - visible));
        for (int i = 0; i < this.profileRowButtons.size(); i++) {
            class_4185 rowButton = this.profileRowButtons.get(i);
            int index = startIndex + i;
            if (index >= this.profiles.size() || i >= visible) {
                rowButton.field_22764 = false;
                rowButton.field_22763 = false;
                rowButton.method_25355(class_2561.method_43473());
            } else {
                String name = this.profiles.get(index);
                boolean selected = index == this.selectedProfile;
                rowButton.field_22764 = true;
                rowButton.field_22763 = true;
                rowButton.method_25355(class_2561.method_43470((selected ? "> " : "") + name));
            }
        }
    }

    private void selectProfileAtVisibleRow(int row) {
        int index;
        int visible = visibleProfileRows();
        if (row < 0 || row >= visible || (index = this.profileListScroll + row) < 0 || index >= this.profiles.size()) {
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

    private boolean openProfilesFolder() {
        try {
            Path dir = TuningProfileManager.profilesDirectoryPath();
            class_156.method_668().method_60932(dir);
            return true;
        } catch (Exception e) {
            try {
                Path dir2 = TuningProfileManager.profilesDirectoryPath();
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

    private void place(class_339 widget, int x, int baseY, int width) {
        widget.method_46421(x);
        widget.method_46419(baseY - this.scrollOffset);
        widget.method_25358(width);
    }

    private void applyFocusVisibility() {
        boolean hideArmGlobalRotation = shouldHideArmGlobalRotationControls();
        boolean hidePivotInCurrentMode = isArmEditor() || isFishingLineEditor();
        if (this.activeSlider == null) {
            for (class_339 widget : this.uiWidgets) {
                widget.field_22764 = true;
                widget.field_22763 = true;
            }
            ToolTuning.Profile profile = isItemEditor() ? getCurrentProfile() : null;
            ArmToolTuning.Profile armProfile = isArmEditor() ? getCurrentArmProfile() : null;
            FishingLineProxyTuning.Profile fishingLineProfile = isFishingLineEditor() ? getCurrentFishingLineProfile() : null;
            boolean has = isArmEditor() ? armProfile != null : isFishingLineEditor() ? fishingLineProfile != null : profile != null;
            for (FloatSlider slider : this.sliders) {
                slider.field_22763 = has;
                if (hidePivotInCurrentMode && slider.isPivotSlider()) {
                    slider.field_22764 = false;
                    slider.field_22763 = false;
                } else if (hideArmGlobalRotation && slider.isRotationSlider()) {
                    slider.field_22764 = false;
                    slider.field_22763 = false;
                } else {
                    slider.field_22764 = true;
                }
            }
            if (this.kindButton != null) {
                this.kindButton.field_22763 = false;
                return;
            }
            return;
        }
        Iterator<class_339> it = this.uiWidgets.iterator();
        while (it.hasNext()) {
            FloatSlider floatSlider = (class_339) it.next();
            boolean keep = floatSlider == this.activeSlider;
            ((class_339) floatSlider).field_22764 = keep;
            ((class_339) floatSlider).field_22763 = keep;
        }
    }

    private <T extends class_339> T addUiWidget(T t) {
        T tMethod_37063 = method_37063(t);
        this.uiWidgets.add(tMethod_37063);
        return tMethod_37063;
    }

    private void normalizeScopeForTarget() {
        if (!isArmEditor()) {
            this.scope = Scope.SPECIFIC;
        }
        if (isFishingLineEditor() && !isHeldFishingRod()) {
            this.editorTarget = EditorTarget.ITEM;
        }
    }

    private void cycleEditorTarget() {
        if (this.editorTarget == EditorTarget.ITEM) {
            this.editorTarget = EditorTarget.ARM;
        } else if (this.editorTarget == EditorTarget.ARM) {
            this.editorTarget = isHeldFishingRod() ? EditorTarget.FISHING_LINE : EditorTarget.ITEM;
        } else {
            this.editorTarget = EditorTarget.ITEM;
        }
    }

    private boolean isItemEditor() {
        return this.editorTarget == EditorTarget.ITEM;
    }

    private boolean isArmEditor() {
        return this.editorTarget == EditorTarget.ARM;
    }

    private boolean isFishingLineEditor() {
        return this.editorTarget == EditorTarget.FISHING_LINE;
    }

    private FishingLineProxyTuning.Profile getCurrentFishingLineProfile() {
        if (!isHeldFishingRod()) {
            return null;
        }
        return FishingLineProxyTuning.get(FishingLineProxyTuning.Kind.ROD, selectedHandTargetsLeftArm());
    }

    private boolean isHeldFishingRod() {
        return isFishingRodStack(heldItemStack());
    }

    private boolean isFishingRodStack(class_1799 stack) {
        class_2960 id;
        return (stack == null || stack.method_7960() || (id = class_7923.field_41178.method_10221(stack.method_7909())) == null || !"minecraft".equals(id.method_12836()) || !"fishing_rod".equals(id.method_12832())) ? false : true;
    }

    private static FishingLineProxyTuning.Profile copyFishingLineProfile(FishingLineProxyTuning.Profile src) {
        if (src == null) {
            return null;
        }
        return new FishingLineProxyTuning.Profile(src.offX, src.offY, src.offZ, src.rotX, src.rotY, src.rotZ, src.scale);
    }

    private boolean isArmSpecificEmptyHandTarget() {
        return isArmEditor() && this.scope == Scope.SPECIFIC && heldItemId() == null;
    }

    private boolean shouldHideArmGlobalRotationControls() {
        return isArmEditor() && this.scope == Scope.GLOBAL;
    }

    private PlayerArmModelLayer.ToolKind currentArmKind() {
        PlayerArmModelLayer.ToolKind mappedKind = armKindForSelectedItemKind();
        if (mappedKind != null) {
            return mappedKind;
        }
        class_310 mc = class_310.method_1551();
        class_1799 stack = class_1799.field_8037;
        if (mc != null && mc.field_1724 != null) {
            stack = mc.field_1724.method_5998(this.leftHand ? class_1268.field_5810 : class_1268.field_5808);
        }
        return ToolKindResolver.resolveArmKind(stack, PlayerArmModelLayer.ToolKind.AXE);
    }

    private PlayerArmModelLayer.ToolKind armKindForSelectedItemKind() {
        try {
            return PlayerArmModelLayer.ToolKind.valueOf(this.selectedKind.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private ArmToolTuning.Profile getCurrentArmProfile() throws MatchException {
        PlayerArmModelLayer.ToolKind armKind = currentArmKind();
        boolean targetLeftArm = selectedHandTargetsLeftArm();
        switch (this.scope) {
            case GLOBAL:
                return ArmToolTuning.getGlobal(targetLeftArm);
            case SPECIFIC:
                class_2960 id = heldItemId();
                if (id == null) {
                    return ArmToolTuning.get(PlayerArmModelLayer.ToolKind.EMPTY_HAND, targetLeftArm);
                }
                return ArmToolTuning.getSpecificProfile(id, armKind, targetLeftArm);
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private ArmToolTuning.Profile armEditorBaseProfile() throws MatchException {
        if (!isArmEditor()) {
            return new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        boolean targetLeftArm = selectedHandTargetsLeftArm();
        switch (this.scope) {
            case GLOBAL:
                return PunchyTuningConfig.getDefaultGlobalArmBase(targetLeftArm);
            case SPECIFIC:
                return new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    private boolean selectedHandTargetsLeftArm() {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null) {
            return this.leftHand;
        }
        boolean mainArmIsLeft = mc.field_1724.method_6068() == class_1306.field_6182;
        return this.leftHand ? !mainArmIsLeft : mainArmIsLeft;
    }

    private void applyArmGlobalToAllSpecifics(boolean left, ArmToolTuning.Profile newGlobal) {
        if (newGlobal == null) {
            return;
        }
        Map<class_2960, ArmToolTuning.SpecificSnapshot> specifics = ArmToolTuning.snapshotSpecifics();
        for (Map.Entry<class_2960, ArmToolTuning.SpecificSnapshot> entry : specifics.entrySet()) {
            class_2960 id = entry.getKey();
            ArmToolTuning.SpecificSnapshot snap = entry.getValue();
            if (id != null && snap != null) {
                ArmToolTuning.Profile right = snap.right();
                ArmToolTuning.Profile leftProfile = snap.left();
                ArmToolTuning.Profile target = left ? leftProfile : right;
                if (target != null) {
                    target.offX = newGlobal.offX;
                    target.offY = newGlobal.offY;
                    target.offZ = newGlobal.offZ;
                    target.rotX = newGlobal.rotX;
                    target.rotY = newGlobal.rotY;
                    target.rotZ = newGlobal.rotZ;
                    target.scale = newGlobal.scale;
                }
                ArmToolTuning.applySpecific(id, snap.kind(), right, leftProfile);
            }
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningWorkbenchScreen$FloatSlider.class */
    private static final class FloatSlider extends class_357 {
        private final String key;
        private float min;
        private float max;
        private final Consumer<Float> onChanged;
        private boolean draggingRelative;
        private double dragStartMouseX;
        private double dragStartValue;

        private FloatSlider(int x, int y, int width, int height, String key, float min, float max, Consumer<Float> onChanged) {
            super(x, y, width, height, class_2561.method_43473(), 0.0d);
            this.key = key;
            this.min = min;
            this.max = max;
            this.onChanged = onChanged;
            setFloat(0.0f);
        }

        private float getFloat() {
            return class_3532.method_16439((float) this.field_22753, this.min, this.max);
        }

        private void setFloat(float value) {
            float clamped = class_3532.method_15363(value, this.min, this.max);
            this.field_22753 = (clamped - this.min) / (this.max - this.min);
            method_25346();
        }

        private void setRange(float min, float max) {
            if (max <= min) {
                return;
            }
            float current = getFloat();
            this.min = min;
            this.max = max;
            setFloat(current);
        }

        public boolean method_25402(double mouseX, double mouseY, int button) {
            if (!this.field_22764 || !this.field_22763 || button != 0 || !method_25405(mouseX, mouseY)) {
                return false;
            }
            this.draggingRelative = true;
            this.dragStartMouseX = mouseX;
            this.dragStartValue = this.field_22753;
            method_25365(true);
            return true;
        }

        public boolean method_25403(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (!this.draggingRelative || button != 0) {
                return false;
            }
            int travel = Math.max(1, method_25368() - 8);
            double delta = (mouseX - this.dragStartMouseX) / ((double) travel);
            this.field_22753 = class_3532.method_15350(this.dragStartValue + delta, 0.0d, 1.0d);
            method_25346();
            method_25344();
            return true;
        }

        public boolean method_25406(double mouseX, double mouseY, int button) {
            if (button == 0 && this.draggingRelative) {
                this.draggingRelative = false;
                return true;
            }
            return false;
        }

        private void drawStyled(class_332 guiGraphics, int mouseX, int mouseY) {
            class_310 mc;
            int fill;
            int border;
            int textColor;
            if (!this.field_22764 || (mc = class_310.method_1551()) == null || mc.field_1772 == null) {
                return;
            }
            int left = method_46426();
            int top = method_46427();
            int right = left + method_25368();
            int bottom = top + method_25364();
            boolean hovered = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
            boolean focused = method_25370();
            String label = displayKey(this.key);
            String valueText = String.format(Locale.ROOT, "%.3f", Float.valueOf(getFloat()));
            int textY = top + ((method_25364() - 8) / 2);
            int labelX = left + 10;
            int valueX = (right - 10) - mc.field_1772.method_1727(valueText);
            int maxLabelWidth = Math.max(10, (valueX - labelX) - 16);
            String clippedLabel = mc.field_1772.method_27523(label, maxLabelWidth);
            int trackLeft = labelX + mc.field_1772.method_1727(clippedLabel) + 10;
            int trackRight = valueX - 8;
            if (trackRight <= trackLeft + 12) {
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
                border = -7485697;
                textColor = -1;
            } else {
                fill = -13682107;
                border = -7429710;
                textColor = -1;
            }
            guiGraphics.method_25294(left, top, right, bottom, fill);
            guiGraphics.method_25294(left, top, right, top + 1, border);
            guiGraphics.method_25294(left, bottom - 1, right, bottom, border);
            guiGraphics.method_25294(left, top, left + 1, bottom, border);
            guiGraphics.method_25294(right - 1, top, right, bottom, border);
            int trackTop = (top + (method_25364() / 2)) - 2;
            int trackBottom = trackTop + 4;
            guiGraphics.method_25294(trackLeft, trackTop, trackRight, trackBottom, -1441719774);
            int knobTravel = Math.max(0, trackWidth - 8);
            int knobLeft = trackLeft + ((int) Math.round(this.field_22753 * ((double) knobTravel)));
            int knobRight = knobLeft + 8;
            int fillRight = knobLeft + (8 / 2);
            guiGraphics.method_25294(trackLeft, trackTop, fillRight, trackBottom, -13869680);
            int knobFill = !this.field_22763 ? -11641751 : (focused || hovered) ? -7485697 : -9798515;
            guiGraphics.method_25294(knobLeft, top + 2, knobRight, bottom - 2, knobFill);
            guiGraphics.method_25294(knobLeft, top + 2, knobRight, top + 3, -4732456);
            guiGraphics.method_25294(knobLeft, bottom - 3, knobRight, bottom - 2, -14404542);
            guiGraphics.method_25303(mc.field_1772, clippedLabel, labelX, textY, textColor);
            guiGraphics.method_25303(mc.field_1772, valueText, valueX, textY, textColor);
        }

        protected void method_25346() {
            String v = String.format(Locale.ROOT, "%.3f", Float.valueOf(getFloat()));
            method_25355(class_2561.method_43470(this.key + ": " + v));
        }

        protected void method_25344() {
            if (this.onChanged != null) {
                this.onChanged.accept(Float.valueOf(getFloat()));
            }
        }

        private boolean isPivotSlider() {
            return "pivotX".equals(this.key) || "pivotY".equals(this.key) || "pivotZ".equals(this.key);
        }

        private boolean isRotationSlider() {
            return "rotX".equals(this.key) || "rotY".equals(this.key) || "rotZ".equals(this.key);
        }

        private static String displayKey(String key) {
            if (key == null || key.isBlank()) {
                return "";
            }
            switch (key) {
                case "offX":
                    return "Offset X";
                case "offY":
                    return "Offset Y";
                case "offZ":
                    return "Offset Z";
                case "rotX":
                    return "Rotation X";
                case "rotY":
                    return "Rotation Y";
                case "rotZ":
                    return "Rotation Z";
                case "pivotX":
                    return "Pivot X";
                case "pivotY":
                    return "Pivot Y";
                case "pivotZ":
                    return "Pivot Z";
                case "scale":
                    return "Scale";
                default:
                    return key;
            }
        }
    }
}
