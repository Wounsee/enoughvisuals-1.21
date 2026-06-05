package fun.enoughvisuals.systems.modules.api;

import fun.enoughvisuals.systems.modules.Module;
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

    private ModuleManager() {}

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    
    
    

    
    public void init() {
        if (initialized) return;
        initialized = true;

        
        

        LOGGER.info("{} модулей загружено", modules.size());
        for (Category cat : Category.values()) {
            long count = modules.stream()
                    .filter(m -> m.getCategory() == cat)
                    .count();
            if (count > 0) {

            }
        }
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
        return Collections.unmodifiableList(modules);
    }

    
    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
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
        for (Module mod : modules) {
            if (mod.isEnabled()) {
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
            if (mod.isEnabled()) {
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
        return (int) modules.stream().filter(Module::isEnabled).count();
    }

    
    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(Module::isEnabled)
                .collect(Collectors.toList());
    }
}
