package fun.enoughvisuals.systems.modules.api;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.network.HolyWorldFeatureControl;
import fun.enoughvisuals.systems.network.RemoteConfig;
import fun.enoughvisuals.utils.player.ServerScopeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ModuleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/Modules");
    private static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new CopyOnWriteArrayList<>();
    private final Map<String, Module> byId = new LinkedHashMap<>();
    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();

    private boolean initialized = false;
    private final Set<String> hiddenModules = new HashSet<>();
    private ServerScopeUtil.Scope lastScope = ServerScopeUtil.Scope.FULL;

    private ModuleManager() {}

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        if (initialized) return;
        initialized = true;

        LOGGER.info("{} модулей загружено", modules.size());
    }

    public void register(Module module) {
        Register reg = module.getClass().getAnnotation(Register.class);
        if (reg == null) {
            LOGGER.error("Cannot register module {} — missing @Register annotation",
                    module.getClass().getSimpleName());
            return;
        }

        String id = reg.id();

        if (byId.containsKey(id)) {
            LOGGER.warn("Module with id '{}' already registered, skipping", id);
            return;
        }

        modules.add(module);
        byId.put(id, module);
        byClass.put(module.getClass(), module);

        LOGGER.debug("Registered module: {} ({})", reg.name(), id);
    }

    public void register(Module... mods) {
        for (Module mod : mods) {
            register(mod);
        }
    }

    public List<Module> getModules() {
        return modules.stream()
                .filter(m -> !isHidden(m))
                .filter(m -> !isBlocked(m))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Module> getAllModules() {
        return List.copyOf(modules);
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .filter(m -> !isHidden(m))
                .filter(m -> !isBlocked(m))
                .collect(Collectors.toList());
    }

    public Module getById(String id) {
        return byId.get(id);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz) {
        return (T) byClass.get(clazz);
    }

    public boolean isEnabled(Class<? extends Module> clazz) {
        Module mod = byClass.get(clazz);
        return mod != null && mod.isEnabled();
    }

    public void onTick() {
        updateHiddenModules();
        for (Module mod : modules) {
            if (isHidden(mod)) {
                if (mod.isEnabled()) {
                    try { mod.disableSilent(); } catch (Exception ignored) {}
                }
                continue;
            }
            if (mod.isEnabled() && isBlocked(mod)) {
                try { mod.disableSilent(); } catch (Exception ignored) {}
                continue;
            }
            if (mod.isEnabled() && !isBlocked(mod)) {
                try {
                    mod.onTick();
                } catch (Exception e) {
                    LOGGER.error("Error in {}.onTick()", mod.getId(), e);
                }
            }
        }
    }

    public void onRender(float tickDelta) {
        for (Module mod : modules) {
            if (isHidden(mod)) continue;
            if (mod.isEnabled() && !isBlocked(mod)) {
                try {
                    mod.onRender(tickDelta);
                } catch (Exception e) {
                    LOGGER.error("Error in {}.onRender()", mod.getId(), e);
                }
            }
        }
    }

    public void onKeyPress(int keyCode) {
        if (keyCode < 0) return;
        for (Module mod : modules) {
            if (isHidden(mod)) continue;
            if (isBlocked(mod)) continue;
            try {
                mod.onKeyPress(keyCode);
            } catch (Exception e) {
                LOGGER.error("Error in {}.onKeyPress()", mod.getId(), e);
            }
            if (mod.getKeyBind() < 0) continue;
            if (mod.getKeyBind() == keyCode) {
                mod.toggle();
                LOGGER.debug("Toggled {} via keybind (key={})", mod.getName(), keyCode);
            }
        }
    }

    public int size() {
        return modules.size();
    }

    public int enabledCount() {
        return (int) modules.stream()
                .filter(m -> !isHidden(m))
                .filter(Module::isEnabled)
                .filter(m -> !isBlocked(m))
                .count();
    }

    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(m -> !isHidden(m))
                .filter(Module::isEnabled)
                .filter(m -> !isBlocked(m))
                .collect(Collectors.toList());
    }

    public boolean isHidden(Module module) {
        return module != null && hiddenModules.contains(module.getId());
    }

    private void updateHiddenModules() {
        ServerScopeUtil.Scope scope = ServerScopeUtil.getCurrentScope();
        if (scope == lastScope) return;
        lastScope = scope;
        hiddenModules.clear();
        hiddenModules.addAll(ServerScopeUtil.getHiddenModules());
        for (Module mod : modules) {
            if (isHidden(mod) && mod.isEnabled()) {
                try { mod.disableSilent(); } catch (Exception ignored) {}
            }
        }
        LOGGER.info("Server scope changed to {}. Hidden modules: {}", scope, hiddenModules);
    }

    public boolean isBlocked(Module module) {
        if (module == null) return false;
        try {
            return RemoteConfig.getInstance().isModuleBlocked(module.getId())
                    || HolyWorldFeatureControl.getInstance().isModuleBlocked(module);
        } catch (Throwable t) {
            return false;
        }
    }

    public void applyFeatureRestrictions() {
        for (Module module : modules) {
            if (isBlocked(module) && module.isEnabled()) {
                try {
                    module.disableSilent();
                } catch (RuntimeException exception) {
                    LOGGER.warn("Failed to disable blocked module {}", module.getId(), exception);
                }
            }
        }
    }
}
