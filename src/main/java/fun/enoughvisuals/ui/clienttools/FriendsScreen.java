package fun.enoughvisuals.ui.clienttools;

import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FriendsScreen extends ClientToolScreen {
    public static final Identifier ICON = Identifier.of("enough", "icons/client/friends.png");

    private final TextBox nameBox = new TextBox(
            "name",
            "",
            ClientLanguage.text("ui.client_tools.friends.name_placeholder", "Nickname")
    );

    public FriendsScreen() {
        super("Friends");
    }

    @Override
    protected Identifier icon() {
        return ICON;
    }

    @Override
    protected String title() {
        return ClientLanguage.text("ui.client_tools.friends.title", "Friends");
    }

    @Override
    protected String statusText(int visibleRows) {
        int total = FriendRepo.listFriends().size();
        return String.format(ClientLanguage.text("ui.client_tools.friends.status", "%d shown / %d total"), visibleRows, total);
    }

    @Override
    protected String emptyText() {
        return ClientLanguage.text("ui.client_tools.friends.empty", "No friends found");
    }

    @Override
    protected List<ToolRow> rows() {
        List<ToolRow> rows = new ArrayList<>();
        for (String friend : FriendRepo.listFriends()) {
            rows.add(new ToolRow(friend, friend, ClientLanguage.text("ui.client_tools.friends.row_subtitle", "Friend"), ICON));
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
    protected void drawRowIcon(DrawContext ctx, Layout l, ToolRow row, float x, float y,
                               boolean selected, float hover, float alpha) {
        float size = 17f * l.scale();
        float avatarX = x + 6f * l.scale();
        float avatarY = y + 6.5f * l.scale();
        Identifier skin = findOnlineSkin(row.title());
        if (skin != null) {
            RenderUtil.drawRoundedRect(ctx, avatarX - 1f * l.scale(), avatarY - 1f * l.scale(),
                    size + 2f * l.scale(), size + 2f * l.scale(), 6f * l.scale(),
                    RenderUtil.withAlpha(0xFFFFFF, alpha * (selected ? 0.30f : 0.14f + hover * 0.10f)));
            Draw.imagePlayerHeadRounded(ctx, skin, avatarX, avatarY, size, 5f * l.scale(), alpha);
            return;
        }

        int bg = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, alpha * 0.82f),
                RenderUtil.withAlpha(0xFFFFFF, alpha * 0.14f),
                selected ? 1f : hover * 0.65f
        );
        RenderUtil.drawRoundedRect(ctx, avatarX, avatarY, size, size, 5f * l.scale(), bg);
        String letter = row.title().isBlank() ? "?" : row.title().substring(0, 1).toUpperCase(Locale.ROOT);
        CustomFontRenderer.drawCenteredText(ctx, letter, avatarX + size * 0.5f,
                avatarY + size * 0.5f - 3.9f * l.scale(), 7.1f * l.scale(),
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.92f), FontWeight.SEMIBOLD);
    }

    @Override
    protected float rowTextOffset(Layout l, ToolRow row) {
        return 33f * l.scale();
    }

    @Override
    protected void renderActions(DrawContext ctx, Layout layout, int mx, int my, float alpha) {
        float s = layout.scale();
        float x = layout.actionX() + 10f * s;
        float y = layout.actionY() + 10f * s;
        float w = layout.actionW() - 20f * s;

        drawSectionTitle(ctx, x, y, ClientLanguage.text("ui.client_tools.friends.editor", "Friend"), alpha, s);
        y += 14f * s;
        drawFieldLabel(ctx, x, y, ClientLanguage.text("ui.client_tools.friends.name", "Name"), alpha, s);
        y += 9f * s;
        drawTextBox(ctx, nameBox, x, y, w, 18f * s, alpha, mx, my, false);
        y += 26f * s;

        drawButton(ctx, "friends_add", ClientLanguage.text("ui.client_tools.add", "Add"), ICON_PLUS,
                x, y, w, 18f * s, !nameBox.text().isBlank(), mx, my, alpha);
        y += 22f * s;
        drawButton(ctx, "friends_delete", ClientLanguage.text("ui.client_tools.delete", "Delete"), ICON_TRASH,
                x, y, w, 18f * s, selectedRow() != null, mx, my, alpha);
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
        if (clickButton("friends_add", mx, my, x, y, w, 18f * s, !nameBox.text().isBlank())) {
            addFriend();
            return true;
        }
        y += 22f * s;
        if (clickButton("friends_delete", mx, my, x, y, w, 18f * s, selectedRow() != null)) {
            deleteFriend();
            return true;
        }
        return false;
    }

    private void addFriend() {
        String name = nameBox.text();
        if (name.isBlank()) {
            notifyStatus(ClientLanguage.text("ui.client_tools.friends.name_required", "Enter nickname"), false);
            return;
        }
        if (FriendRepo.addFriend(name)) {
            selectedId = name.toLowerCase(java.util.Locale.ROOT);
            nameBox.setText("");
            notifyStatus(ClientLanguage.text("ui.client_tools.friends.added", "Friend added"), true);
        } else {
            notifyStatus(ClientLanguage.text("ui.client_tools.friends.exists", "Already in friends"), false);
        }
    }

    private void deleteFriend() {
        ToolRow row = selectedRow();
        if (row == null) {
            return;
        }
        if (FriendRepo.removeFriend(row.id())) {
            nameBox.setText("");
            selectedId = "";
            notifyStatus(ClientLanguage.text("ui.client_tools.friends.removed", "Friend removed"), true);
        } else {
            notifyStatus(ClientLanguage.text("ui.client_tools.friends.missing", "Friend not found"), false);
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

    private Identifier findOnlineSkin(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null || name == null || name.isBlank()) {
            return null;
        }
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile() != null && name.equalsIgnoreCase(entry.getProfile().getName())) {
                try {
                    return entry.getSkinTextures().texture();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
