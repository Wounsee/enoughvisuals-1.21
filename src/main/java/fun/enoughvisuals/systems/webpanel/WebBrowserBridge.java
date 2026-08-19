package fun.enoughvisuals.systems.webpanel;

import java.lang.reflect.Method;

/**
 * Reflection bridge to MCEF (Minecraft Chromium Embedded Framework).
 * Allows compiling and running without MCEF on the compile classpath.
 * If MCEF is present in the mods folder, WebPanels will render a live Chromium browser.
 * Otherwise, a clean placeholder is shown.
 */
public final class WebBrowserBridge {

    private static Boolean present;
    private static Class<?> mcefClass;
    private static Method createBrowser;
    private static Method isInitialized;

    private WebBrowserBridge() {
    }

    private static boolean resolve() {
        if (present != null) {
            return present;
        }
        try {
            mcefClass = Class.forName("com.cinemamod.mcef.MCEF");
            try {
                isInitialized = mcefClass.getMethod("isInitialized");
            } catch (NoSuchMethodException ignored) {
            }
            try {
                createBrowser = mcefClass.getMethod("createBrowser", String.class, boolean.class, int.class, int.class);
            } catch (NoSuchMethodException e) {
                createBrowser = mcefClass.getMethod("createBrowser", String.class, boolean.class);
            }
            present = true;
        } catch (Throwable t) {
            present = false;
        }
        return present;
    }

    /** True only when MCEF is loaded AND initialized (Chromium ready). */
    public static boolean available() {
        if (!resolve()) {
            return false;
        }
        if (isInitialized == null) {
            return true;
        }
        try {
            Object r = isInitialized.invoke(null);
            return r instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Create a browser for the URL, or null if MCEF is unavailable / failed. */
    public static Object create(String url, int width, int height) {
        if (!available()) {
            return null;
        }
        try {
            Object browser;
            if (createBrowser.getParameterCount() == 4) {
                browser = createBrowser.invoke(null, url, false, width, height);
            } else {
                browser = createBrowser.invoke(null, url, false);
                resize(browser, width, height);
            }
            return browser;
        } catch (Throwable t) {
            return null;
        }
    }

    /** The OpenGL texture id of the browser's current frame, or 0. */
    public static int textureId(Object browser) {
        if (browser == null) {
            return 0;
        }
        try {
            Object renderer = invoke(browser, "getRenderer");
            if (renderer == null) {
                return 0;
            }
            Object id = invoke(renderer, "getTextureID");
            if (id == null) {
                id = invoke(renderer, "getTextureId");
            }
            return id instanceof Integer i ? i : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void resize(Object browser, int w, int h) {
        call(browser, "resize", new Class<?>[]{int.class, int.class}, w, h);
    }

    public static void mouseMove(Object browser, int x, int y) {
        call(browser, "sendMouseMove", new Class<?>[]{int.class, int.class}, x, y);
    }

    public static void mousePress(Object browser, int x, int y, int button) {
        call(browser, "sendMousePress", new Class<?>[]{int.class, int.class, int.class}, x, y, button);
    }

    public static void mouseRelease(Object browser, int x, int y, int button) {
        call(browser, "sendMouseRelease", new Class<?>[]{int.class, int.class, int.class}, x, y, button);
    }

    public static void mouseWheel(Object browser, int x, int y, double delta) {
        if (!call(browser, "sendMouseWheel", new Class<?>[]{int.class, int.class, double.class, int.class}, x, y, delta, 0)) {
            call(browser, "sendMouseWheel", new Class<?>[]{int.class, int.class, double.class}, x, y, delta);
        }
    }

    public static void keyPress(Object browser, int keyCode, int scanCode, int modifiers) {
        call(browser, "sendKeyPress", new Class<?>[]{int.class, long.class, int.class}, keyCode, (long) scanCode, modifiers);
    }

    public static void keyRelease(Object browser, int keyCode, int scanCode, int modifiers) {
        call(browser, "sendKeyRelease", new Class<?>[]{int.class, long.class, int.class}, keyCode, (long) scanCode, modifiers);
    }

    public static void keyTyped(Object browser, char c, int modifiers) {
        call(browser, "sendKeyTyped", new Class<?>[]{char.class, int.class}, c, modifiers);
    }

    public static void loadUrl(Object browser, String url) {
        if (!call(browser, "loadURL", new Class<?>[]{String.class}, url)) {
            call(browser, "loadUrl", new Class<?>[]{String.class}, url);
        }
    }

    public static void close(Object browser) {
        call(browser, "close", new Class<?>[]{});
    }

    // ---- reflection helpers ----

    private static Object invoke(Object target, String name) throws Exception {
        Method m = target.getClass().getMethod(name);
        return m.invoke(target);
    }

    private static boolean call(Object target, String name, Class<?>[] sig, Object... args) {
        if (target == null) {
            return false;
        }
        try {
            Method m = target.getClass().getMethod(name, sig);
            m.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
