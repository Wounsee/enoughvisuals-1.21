package fun.enoughvisuals.ui.clienttools;

import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.gps.GpsSystem.WaypointView;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class GpsScreen extends ClientToolScreen {
    public static final String ICON = SolarIcons.WAYPOINT;

    private final TextBox nameBox = new TextBox("name", "", ClientLanguage.text("ui.client_tools.gps.name_placeholder", "Point name"));
    private final TextBox xBox = new TextBox("x", "", "X");
    private final TextBox yBox = new TextBox("y", "", "Y");
    private final TextBox zBox = new TextBox("z", "", "Z");

    public GpsScreen() {
        super("GPS");
    }

    @Override
    protected String icon() {
        return ICON;
    }

    @Override
    protected String title() {
        return ClientLanguage.text("ui.client_tools.gps.title", "GPS");
    }

    @Override
    protected String statusText(int visibleRows) {
        String scope = GpsSystem.getInstance().currentScopeLabel();
        if (scope.length() > 30) {
            scope = scope.substring(0, 27) + "...";
        }
        return String.format(ClientLanguage.text("ui.client_tools.gps.status", "%d shown - %s"), visibleRows, scope);
    }

    @Override
    protected String emptyText() {
        return ClientLanguage.text("ui.client_tools.gps.empty", "No waypoints found");
    }

    @Override
    protected List<ToolRow> rows() {
        List<ToolRow> rows = new ArrayList<>();
        for (WaypointView waypoint : GpsSystem.getInstance().listActiveWaypoints()) {
            String subtitle = String.format("%d %d %d - %s",
                    (int) waypoint.x(), (int) waypoint.y(), (int) waypoint.z(),
                    String.format(ClientLanguage.text("ui.client_tools.gps.distance", "%d blocks"), waypoint.distance()));
            if (waypoint.event()) {
                subtitle = ClientLanguage.text("ui.client_tools.gps.event", "Event") + " - " + subtitle;
            }
            rows.add(new ToolRow(String.valueOf(waypoint.activeIndex()), waypoint.name(), subtitle, ICON_PIN));
        }
        return rows;
    }

    @Override
    protected void onRowSelected(ToolRow row) {
        WaypointView waypoint = selectedWaypoint();
        if (waypoint == null) {
            return;
        }
        nameBox.setText(waypoint.name());
        xBox.setText(String.valueOf((int) waypoint.x()));
        yBox.setText(String.valueOf((int) waypoint.y()));
        zBox.setText(String.valueOf((int) waypoint.z()));
    }

    @Override
    protected void renderActions(DrawContext ctx, Layout layout, int mx, int my, float alpha) {
        float s = layout.scale();
        float x = layout.actionX() + 10f * s;
        float y = layout.actionY() + 10f * s;
        float w = layout.actionW() - 20f * s;

        drawSectionTitle(ctx, x, y, ClientLanguage.text("ui.client_tools.gps.editor", "Waypoint"), alpha, s);
        y += 14f * s;
        drawFieldLabel(ctx, x, y, ClientLanguage.text("ui.client_tools.gps.name", "Name"), alpha, s);
        y += 9f * s;
        drawTextBox(ctx, nameBox, x, y, w, 18f * s, alpha, mx, my, false);
        y += 26f * s;

        float gap = 5f * s;
        float coordW = (w - gap * 2f) / 3f;
        drawTextBox(ctx, xBox, x, y, coordW, 18f * s, alpha, mx, my, false);
        drawTextBox(ctx, yBox, x + coordW + gap, y, coordW, 18f * s, alpha, mx, my, false);
        drawTextBox(ctx, zBox, x + (coordW + gap) * 2f, y, coordW, 18f * s, alpha, mx, my, false);
        y += 26f * s;

        drawButton(ctx, "gps_current", ClientLanguage.text("ui.client_tools.gps.current_pos", "Current Pos"), ICON_CROSSHAIR,
                x, y, w, 17f * s, true, mx, my, alpha);
        y += 20f * s;
        drawButton(ctx, "gps_add", ClientLanguage.text("ui.client_tools.gps.add_update", "Add / Update"), ICON_PLUS,
                x, y, w, 17f * s, canSave(), mx, my, alpha);
        y += 20f * s;
        drawButton(ctx, "gps_delete", ClientLanguage.text("ui.client_tools.delete", "Delete"), ICON_TRASH,
                x, y, w, 17f * s, selectedWaypoint() != null, mx, my, alpha);
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
        float gap = 5f * s;
        float coordW = (w - gap * 2f) / 3f;
        if (clickTextBox(xBox, mx, my, x, y, coordW, 18f * s)
                || clickTextBox(yBox, mx, my, x + coordW + gap, y, coordW, 18f * s)
                || clickTextBox(zBox, mx, my, x + (coordW + gap) * 2f, y, coordW, 18f * s)) {
            return true;
        }
        y += 26f * s;
        if (clickButton("gps_current", mx, my, x, y, w, 17f * s, true)) {
            useCurrentPos();
            return true;
        }
        y += 20f * s;
        if (clickButton("gps_add", mx, my, x, y, w, 17f * s, canSave())) {
            saveWaypoint();
            return true;
        }
        y += 20f * s;
        if (clickButton("gps_delete", mx, my, x, y, w, 17f * s, selectedWaypoint() != null)) {
            deleteWaypoint();
            return true;
        }
        return false;
    }

    private boolean canSave() {
        return !nameBox.text().isBlank()
                && !xBox.text().isBlank()
                && !yBox.text().isBlank()
                && !zBox.text().isBlank();
    }

    private void useCurrentPos() {
        BlockPos pos = GpsSystem.getInstance().currentPlayerBlockPos();
        if (pos == null) {
            notifyStatus(ClientLanguage.text("ui.client_tools.gps.no_player", "Player position unavailable"), false);
            return;
        }
        if (nameBox.text().isBlank()) {
            nameBox.setText(ClientLanguage.text("ui.client_tools.gps.default_name", "Point"));
        }
        xBox.setText(String.valueOf(pos.getX()));
        yBox.setText(String.valueOf(pos.getY()));
        zBox.setText(String.valueOf(pos.getZ()));
        notifyStatus(ClientLanguage.text("ui.client_tools.gps.coords_filled", "Coordinates filled"), true);
    }

    private void saveWaypoint() {
        String name = nameBox.text();
        if (name.isBlank()) {
            notifyStatus(ClientLanguage.text("ui.client_tools.gps.name_required", "Enter waypoint name"), false);
            return;
        }
        Double x = parseNumber(xBox.text());
        Double y = parseNumber(yBox.text());
        Double z = parseNumber(zBox.text());
        if (x == null || y == null || z == null) {
            notifyStatus(ClientLanguage.text("ui.client_tools.gps.coords_invalid", "Invalid coordinates"), false);
            return;
        }

        GpsSystem.getInstance().addOrReplaceWaypoint(name, x, y, z);
        notifyStatus(ClientLanguage.text("ui.client_tools.gps.saved", "Waypoint saved"), true);
        for (WaypointView waypoint : GpsSystem.getInstance().listActiveWaypoints()) {
            if (waypoint.name().equalsIgnoreCase(name)) {
                selectedId = String.valueOf(waypoint.activeIndex());
                break;
            }
        }
    }

    private void deleteWaypoint() {
        WaypointView waypoint = selectedWaypoint();
        if (waypoint == null) {
            return;
        }
        if (GpsSystem.getInstance().removeActiveWaypoint(waypoint.activeIndex())) {
            selectedId = "";
            notifyStatus(ClientLanguage.text("ui.client_tools.gps.deleted", "Waypoint deleted"), true);
        } else {
            notifyStatus(ClientLanguage.text("ui.client_tools.gps.delete_fail", "Delete failed"), false);
        }
    }

    private WaypointView selectedWaypoint() {
        if (selectedId.isBlank()) {
            return null;
        }
        try {
            int activeIndex = Integer.parseInt(selectedId);
            for (WaypointView waypoint : GpsSystem.getInstance().listActiveWaypoints()) {
                if (waypoint.activeIndex() == activeIndex) {
                    return waypoint;
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private Double parseNumber(String raw) {
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (Exception ignored) {
            return null;
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
