package fun.enoughvisuals.systems.webpanel;

import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Reflection bridge to MCEF (Minecraft Chromium Embedded Framework).
 * Allows compiling and running without MCEF on the compile classpath.
 * When MCEF is present, WebPanels will render a live Chromium browser.
 */
public final class WebBrowserBridge {

    private static Boolean present;
    private static Class<?> mcefClass;
    private static Method createBrowser;
    private static Method isInitialized;
    private static Method initialize;

    private static volatile boolean initStarted = false;

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
                initialize = mcefClass.getMethod("initialize");
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

    /** Trigger CEF initialization once asynchronously if not yet started. */
    public static void requestInit() {
        if (!resolve() || initStarted) return;
        initStarted = true;
        try {
            if (isInitialized != null) {
                Object r = isInitialized.invoke(null);
                if (r instanceof Boolean b && b) {
                    return;
                }
            }
            if (initialize != null) {
                new Thread(() -> {
                    try {
                        initialize.invoke(null);
                    } catch (Throwable ignored) {
                    }
                }, "MCEF-Async-Init").start();
            }
        } catch (Throwable ignored) {
        }
    }

    /** Returns true only when Chromium/CEF is fully initialized and ready to create browsers. */
    public static boolean isReady() {
        if (!resolve()) {
            return false;
        }
        try {
            if (isInitialized != null) {
                Object r = isInitialized.invoke(null);
                if (r instanceof Boolean b) {
                    if (!b && !initStarted) {
                        requestInit();
                    }
                    return b;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** True only when MCEF is loaded AND initialized (Chromium ready). */
    public static boolean available() {
        return isReady();
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
            disableCursorListener(browser);
            return browser;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void disableCursorListener(Object browser) {
        if (browser == null) return;
        try {
            Class<?> listenerClass = Class.forName("com.cinemamod.mcef.listeners.MCEFCursorChangeListener");
            Object noopListener = java.lang.reflect.Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    (proxy, method, args) -> null
            );
            Method setListener = browser.getClass().getMethod("setCursorChangeListener", listenerClass);
            setListener.setAccessible(true);
            setListener.invoke(browser, noopListener);
        } catch (Throwable ignored) {
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

    /** The Minecraft Identifier of the browser's current frame texture, or null. */
    public static Identifier textureIdentifier(Object browser) {
        if (browser == null) {
            return null;
        }
        try {
            Object id = invoke(browser, "getTextureIdentifier");
            if (id instanceof Identifier ident) {
                return ident;
            }
            Object renderer = invoke(browser, "getRenderer");
            if (renderer != null) {
                Object rId = invoke(renderer, "getTextureIdentifier");
                if (rId instanceof Identifier ident) {
                    return ident;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean isTextureReady(Object browser) {
        if (browser == null) return false;
        try {
            Object ready = invoke(browser, "isTextureReady");
            if (ready instanceof Boolean b) return b;
            Object renderer = invoke(browser, "getRenderer");
            if (renderer != null) {
                Object rReady = invoke(renderer, "isTextureReady");
                if (rReady instanceof Boolean b) return b;
            }
        } catch (Throwable ignored) {
        }
        return false;
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
        if (!call(browser, "sendKeyPress", new Class<?>[]{int.class, long.class, int.class}, keyCode, (long) scanCode, modifiers)) {
            call(browser, "sendKeyPress", new Class<?>[]{int.class, int.class, int.class}, keyCode, scanCode, modifiers);
        }
    }

    public static void keyRelease(Object browser, int keyCode, int scanCode, int modifiers) {
        if (!call(browser, "sendKeyRelease", new Class<?>[]{int.class, long.class, int.class}, keyCode, (long) scanCode, modifiers)) {
            call(browser, "sendKeyRelease", new Class<?>[]{int.class, int.class, int.class}, keyCode, scanCode, modifiers);
        }
    }

    public static void keyTyped(Object browser, char chr, int modifiers) {
        call(browser, "sendKeyTyped", new Class<?>[]{char.class, int.class}, chr, modifiers);
    }

    public static void loadUrl(Object browser, String url) {
        if (!call(browser, "loadURL", new Class<?>[]{String.class}, url)) {
            call(browser, "loadUrl", new Class<?>[]{String.class}, url);
        }
    }

    public static void goBack(Object browser) {
        call(browser, "goBack", new Class<?>[]{});
    }

    public static void goForward(Object browser) {
        call(browser, "goForward", new Class<?>[]{});
    }

    public static void reload(Object browser) {
        if (!call(browser, "reload", new Class<?>[]{boolean.class}, false)) {
            call(browser, "reload", new Class<?>[]{});
        }
    }

    public static String getUrl(Object browser) {
        if (browser == null) return null;
        Object url = invoke(browser, "getURL");
        if (url == null) {
            url = invoke(browser, "getUrl");
        }
        return url instanceof String s ? s : null;
    }

    public static void runMessageLoop() {
        if (!resolve()) return;
        try {
            Object app = invoke(mcefClass, "getApp");
            if (app != null) {
                Object handle = invoke(app, "getHandle");
                if (handle != null) {
                    Method m = handle.getClass().getMethod("N_DoMessageLoopWork");
                    m.setAccessible(true);
                    m.invoke(handle);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void close(Object browser) {
        call(browser, "close", new Class<?>[]{});
        flushPersistentStorage();
    }

    /** Flushes the global cookie store; LocalStorage/IndexedDB share the same persistent CEF profile. */
    public static void flushPersistentStorage() {
        try {
            Class<?> managerClass = Class.forName("org.cef.network.CefCookieManager");
            Object manager = managerClass.getMethod("getGlobalManager").invoke(null);
            if (manager == null) return;
            Class<?> callbackClass = Class.forName("org.cef.callback.CefCompletionCallback");
            Object callback = Proxy.newProxyInstance(callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass}, (proxy, method, args) -> null);
            Method flush = managerClass.getMethod("flushStore", callbackClass);
            flush.invoke(manager, callback);
        } catch (Throwable ignored) {
        }
    }

    private static Object invoke(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean call(Object target, String method, Class<?>[] types, Object... args) {
        if (target == null) {
            return false;
        }
        try {
            Method m = target.getClass().getMethod(method, types);
            m.setAccessible(true);
            m.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
