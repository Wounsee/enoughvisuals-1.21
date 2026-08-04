package fun.wounsee.cosmetic.lua.api.nameplate;

import net.minecraft.network.chat.Component;
import fun.wounsee.cosmetic.avatar.Badges;
import fun.wounsee.cosmetic.font.Emojis;
import fun.wounsee.cosmetic.lua.LuaWhitelist;
import fun.wounsee.cosmetic.lua.docs.LuaMethodDoc;
import fun.wounsee.cosmetic.lua.docs.LuaMethodOverload;
import fun.wounsee.cosmetic.lua.docs.LuaTypeDoc;
import fun.wounsee.cosmetic.utils.TextUtils;
import org.luaj.vm2.LuaError;

@LuaWhitelist
@LuaTypeDoc(
        name = "NameplateCustomization",
        value = "nameplate_customization"
)
public class NameplateCustomization {

    private Component json;
    private String text;

    private Component parseJsonText(String text) {
        Component component = TextUtils.tryParseJson(text);
        component = Badges.noBadges4U(component);
        component = TextUtils.removeClickableObjects(component);
        component = Emojis.applyEmojis(component);
        component = Emojis.removeBlacklistedEmojis(component);
        return component;
    }

    public Component getJson() {
        return json;
    }

    @LuaWhitelist
    @LuaMethodDoc("nameplate_customization.get_text")
    public String getText() {
        return this.text;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            value = "nameplate_customization.set_text"
    )
    public NameplateCustomization setText(String text) {
        this.text = text;
        if (text != null) {
            Component component = parseJsonText(text);
            if (component.getString().length() > 64)
                throw new LuaError("Text length exceeded limit of 64 characters");
            json = component;
        } else {
            json = null;
        }
        return this;
    }

    @Override
    public String toString() {
        return "NameplateCustomization";
    }
}
