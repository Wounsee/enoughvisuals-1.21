package fun.enoughvisuals.ui.clienttools;

import fun.enoughvisuals.systems.config.ConfigSystem;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

public class ConfigsScreen extends ClientToolScreen {
    public static final Identifier ICON = Identifier.of("enough", "icons/client/configs.png");

    private final TextBox nameBox = new TextBox(
            "config",
            "",
            ClientLanguage.text("ui.client_tools.configs.name_placeholder", "Config name")
    );

    public ConfigsScreen() {
        super("Configs");
    }

    @Override
    protected void onOpen() {
        ConfigSystem.getInstance().initialize();
        nameBox.setText(ConfigSystem.getInstance().getLastConfigName());
    }

    @Override
    protected Identifier icon() {
        return ICON;
    }

    @Override
    protected String title() {
        return ClientLanguage.text("ui.client_tools.configs.title", "Configs");
    }

    @Override
    protected String statusText(int visibleRows) {
        String active = ConfigSystem.getInstance().getLastConfigName();
        return String.format(ClientLanguage.text("ui.client_tools.configs.status", "%d shown - active: %s"), visibleRows, active);
    }

    @Override
    protected String emptyText() {
        return ClientLanguage.text("ui.client_tools.configs.empty", "No configs found");
    }

    @Override
    protected List<ToolRow> rows() {
        ConfigSystem cfg = ConfigSystem.getInstance();
        List<ToolRow> rows = new ArrayList<>();
        String active = cfg.getLastConfigName();
        for (String config : cfg.listConfigs()) {
            boolean isActive = config.equalsIgnoreCase(active);
            String subtitle = isActive
                    ? ClientLanguage.text("ui.client_tools.configs.active", "Active config")
                    : ClientLanguage.text("ui.client_tools.configs.saved", "Saved config");
            rows.add(new ToolRow(config, config, subtitle, ICON));
        }
        return rows;
    }

    @Override
    protected void onRowSelected(ToolRow row) {
        if (row != null) {
            nameBox.setText(row.title());
        }
    }

    @Override
    protected void renderActions(DrawContext ctx, Layout layout, int mx, int my, float alpha) {
        float s = layout.scale();
        float x = layout.actionX() + 10f * s;
        float y = layout.actionY() + 10f * s;
        float w = layout.actionW() - 20f * s;

        drawSectionTitle(ctx, x, y, ClientLanguage.text("ui.client_tools.configs.editor", "Config"), alpha, s);
        y += 14f * s;
        drawFieldLabel(ctx, x, y, ClientLanguage.text("ui.client_tools.configs.name", "Name"), alpha, s);
        y += 9f * s;
        drawTextBox(ctx, nameBox, x, y, w, 18f * s, alpha, mx, my, false);
        y += 26f * s;

        drawButton(ctx, "cfg_save_current", ClientLanguage.text("ui.client_tools.configs.save_current", "Save Current"), ICON_SAVE,
                x, y, w, 17f * s, true, mx, my, alpha);
        y += 20f * s;
        drawButton(ctx, "cfg_save_as", ClientLanguage.text("ui.client_tools.configs.save_as", "Save As"), ICON_PLUS,
                x, y, w, 17f * s, !nameBox.text().isBlank(), mx, my, alpha);
        y += 20f * s;
        drawButton(ctx, "cfg_load", ClientLanguage.text("ui.client_tools.configs.load", "Load"), ICON_LOAD,
                x, y, w, 17f * s, selectedRow() != null, mx, my, alpha);
        y += 20f * s;
        drawButton(ctx, "cfg_delete", ClientLanguage.text("ui.client_tools.delete", "Delete"), ICON_TRASH,
                x, y, w, 17f * s, selectedRow() != null, mx, my, alpha);
        y += 20f * s;
        drawButton(ctx, "cfg_folder", ClientLanguage.text("ui.client_tools.configs.open_folder", "Open Folder"), ICON_FOLDER,
                x, y, w, 17f * s, true, mx, my, alpha);
    }

    @Override
    protected boolean handleActionClick(float mx, float my, Layout layout) {
        float s = layout.scale();
        float x = layout.actionX() + 10f * s;
        float y = layout.actionY() + 33f * s;
        float w = layout.actionW() - 20f * s;
        if (clickTextBox(nameBox, mx, my, x, y, w, 18f * s)) {
            return true;
        }
        y += 26f * s;
        if (clickButton("cfg_save_current", mx, my, x, y, w, 17f * s, true)) {
            saveCurrent();
            return true;
        }
        y += 20f * s;
        if (clickButton("cfg_save_as", mx, my, x, y, w, 17f * s, !nameBox.text().isBlank())) {
            saveAs();
            return true;
        }
        y += 20f * s;
        if (clickButton("cfg_load", mx, my, x, y, w, 17f * s, selectedRow() != null)) {
            loadSelected();
            return true;
        }
        y += 20f * s;
        if (clickButton("cfg_delete", mx, my, x, y, w, 17f * s, selectedRow() != null)) {
            deleteSelected();
            return true;
        }
        y += 20f * s;
        if (clickButton("cfg_folder", mx, my, x, y, w, 17f * s, true)) {
            openFolder();
            return true;
        }
        return false;
    }

    private void saveCurrent() {
        ConfigSystem cfg = ConfigSystem.getInstance();
        if (cfg.saveCurrent()) {
            selectedId = cfg.getLastConfigName();
            nameBox.setText(selectedId);
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.saved_ok", "Config saved"), true);
        } else {
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.save_fail", "Save failed"), false);
        }
    }

    private void saveAs() {
        ConfigSystem cfg = ConfigSystem.getInstance();
        String name = nameBox.text();
        if (name.isBlank()) {
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.name_required", "Enter config name"), false);
            return;
        }
        if (cfg.saveAs(name)) {
            selectedId = cfg.getLastConfigName();
            nameBox.setText(selectedId);
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.saved_ok", "Config saved"), true);
        } else {
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.save_fail", "Save failed"), false);
        }
    }

    private void loadSelected() {
        ToolRow row = selectedRow();
        if (row == null) {
            return;
        }
        ConfigSystem cfg = ConfigSystem.getInstance();
        if (cfg.load(row.id())) {
            selectedId = cfg.getLastConfigName();
            nameBox.setText(selectedId);
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.loaded", "Config loaded"), true);
        } else {
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.load_fail", "Load failed"), false);
        }
    }

    private void deleteSelected() {
        ToolRow row = selectedRow();
        if (row == null) {
            return;
        }
        if (ConfigSystem.getInstance().remove(row.id())) {
            selectedId = "";
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.deleted", "Config deleted"), true);
        } else {
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.delete_fail", "Delete failed"), false);
        }
    }

    private void openFolder() {
        try {
            Util.getOperatingSystem().open(ConfigSystem.getInstance().getConfigDirectory().toFile());
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.folder_opened", "Folder opened"), true);
        } catch (Exception ignored) {
            notifyStatus(ClientLanguage.text("ui.client_tools.configs.folder_fail", "Cannot open folder"), false);
        }
    }

    private void drawSectionTitle(DrawContext ctx, float x, float y, String text, float alpha, float scale) {
        CustomFontRenderer.drawText(ctx, text, x, y, 7.2f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
    }

    private void drawFieldLabel(DrawContext ctx, float x, float y, String text, float alpha, float scale) {
        CustomFontRenderer.drawText(ctx, text, x, y, 5.8f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.9f), FontWeight.REGULAR);
    }
}
