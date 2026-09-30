package fun.enoughvisuals.systems.modules.settings;

import fun.enoughvisuals.utils.system.BindKeyUtil;

public class BindSetting extends Setting<Integer> {

    public BindSetting(String name, String id, String description, int defaultKey) {
        super(name, id, description, defaultKey);
    }

    public BindSetting(String name, String id, int defaultKey) {
        this(name, id, "", defaultKey);
    }

    public BindSetting(String name, String id) {
        this(name, id, "", -1);
    }

    public boolean isBound() {
        return get() > BindKeyUtil.NONE;
    }

    public void unbind() {
        set(BindKeyUtil.NONE);
    }

    public boolean isKey(int keyCode) {
        return isBound() && get() == keyCode;
    }

    public String getKeyName() {
        return BindKeyUtil.getBindName(get());
    }

    @Override
    public Object serialize() {
        return get();
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof Number n) {
            set(n.intValue());
        } else if (data instanceof String s) {
            try { set(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
        }
    }
}
