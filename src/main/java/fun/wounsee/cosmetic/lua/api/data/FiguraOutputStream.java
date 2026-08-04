package fun.wounsee.cosmetic.lua.api.data;

import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.lua.LuaNotNil;
import fun.wounsee.cosmetic.lua.LuaWhitelist;
import fun.wounsee.cosmetic.lua.docs.LuaMethodDoc;
import fun.wounsee.cosmetic.lua.docs.LuaMethodOverload;
import fun.wounsee.cosmetic.lua.docs.LuaTypeDoc;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;

import java.io.IOException;
import java.io.OutputStream;

@LuaWhitelist
@LuaTypeDoc(name = "OutputStream", value = "output_stream")
public class FiguraOutputStream extends OutputStream {
    private final Avatar parent;
    private final OutputStream destinationStream;

    public FiguraOutputStream(Avatar parent, OutputStream destinationStream) {
        this.destinationStream = destinationStream;
        this.parent = parent;
        parent.openOutputStreams.add(this);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "output_stream.write",
            overloads = @LuaMethodOverload(
                    argumentNames = "b",
                    argumentTypes = Integer.class
            )
    )
    @Override
    public void write(int b) {
        try {
            destinationStream.write(b);
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc("output_stream.flush")
    public void flush() throws IOException {
        destinationStream.flush();
    }

    @Override
    @LuaWhitelist
    @LuaMethodDoc("output_stream.close")
    public void close() throws IOException {
        destinationStream.close();
        parent.openOutputStreams.remove(this);
    }

    @Override
    public String toString() {
        return "OutputStream";
    }
}
