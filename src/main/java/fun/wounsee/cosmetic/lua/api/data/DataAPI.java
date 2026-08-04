package fun.wounsee.cosmetic.lua.api.data;

import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.lua.LuaWhitelist;
import fun.wounsee.cosmetic.lua.docs.LuaMethodDoc;
import fun.wounsee.cosmetic.lua.docs.LuaMethodOverload;
import fun.wounsee.cosmetic.lua.docs.LuaTypeDoc;

@LuaWhitelist
@LuaTypeDoc(name = "DataAPI", value = "data")
public class DataAPI {

    private final Avatar parent;

    public DataAPI(Avatar parent) {
        this.parent = parent;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "data.create_buffer",
            overloads = {
                    @LuaMethodOverload(
                            returnType = FiguraBuffer.class
                    ),
                    @LuaMethodOverload(
                            returnType = FiguraBuffer.class,
                            argumentNames = "capacity",
                            argumentTypes = Integer.class
                    )
            }
    )
    public FiguraBuffer createBuffer(Integer len) {
        return len == null ? new FiguraBuffer(parent) : new FiguraBuffer(parent, len);
    }

    @Override
    public String toString() {
        return "DataAPI";
    }
}
