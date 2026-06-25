package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.lang.reflect.Constructor;
import java.net.URI;

public final class AccountSwitcherBridgeScreen extends Screen {
    private static final String IAS_SCREEN = "ru.vidtu.ias.screen.AccountScreen";
    private static final URI IAS_DOWNLOAD = URI.create("https://minecraft-inside.ru/download/487739/");

    private final Screen parent;
    private boolean attempted;

    public AccountSwitcherBridgeScreen(Screen parent) {
        super(Text.literal("Account Switcher"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (attempted) return;
        attempted = true;

        Screen iasScreen = createIasScreen();
        if (iasScreen != null) {
            client.setScreen(iasScreen);
            return;
        }

        try {
            Util.getOperatingSystem().open(IAS_DOWNLOAD);
        } catch (Throwable ignored) {
        }
        client.setScreen(parent);
    }

    private Screen createIasScreen() {
        try {
            Class<?> type = Class.forName(IAS_SCREEN);
            if (!Screen.class.isAssignableFrom(type)) return null;

            Constructor<?> constructor = type.getDeclaredConstructor(Screen.class);
            constructor.setAccessible(true);
            return (Screen) constructor.newInstance(parent);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        CustomFontRenderer.drawCenteredText(ctx, "Opening account switcher...",
                width * 0.5f, height * 0.5f - 5.0f, 9.0f, 0xFFFFFFFF, FontWeight.SEMIBOLD);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
