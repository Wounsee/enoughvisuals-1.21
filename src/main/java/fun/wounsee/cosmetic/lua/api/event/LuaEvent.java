package fun.wounsee.cosmetic.lua.api.event;

import com.google.common.collect.HashMultimap;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.lua.LuaNotNil;
import fun.wounsee.cosmetic.lua.LuaWhitelist;
import fun.wounsee.cosmetic.lua.docs.LuaMetamethodDoc;
import fun.wounsee.cosmetic.lua.docs.LuaMetamethodDoc.LuaMetamethodOverload;
import fun.wounsee.cosmetic.lua.docs.LuaMethodDoc;
import fun.wounsee.cosmetic.lua.docs.LuaMethodOverload;
import fun.wounsee.cosmetic.lua.docs.LuaTypeDoc;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.Varargs;

import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

@LuaWhitelist
@LuaTypeDoc(
        name = "Event",
        value = "event"
)
public class LuaEvent {

    private static final int MAX_FUNCTIONS = 1024;

    private final boolean piped;

    private final Deque<LuaFunction> functions = new ConcurrentLinkedDeque<>();
    private final Deque<LuaFunction> queue = new ConcurrentLinkedDeque<>();
    private final Deque<LuaFunction> removalQueue = new ConcurrentLinkedDeque<>();
    private final HashMultimap<String, LuaFunction> names = HashMultimap.create();

    public LuaEvent() {
        this(false);
    }

    public LuaEvent(boolean piped) {
        this.piped = piped;
    }

    // Add all waiting functions from the queues
    protected void flushQueue() {
        for (LuaFunction function : removalQueue)
            functions.removeFirstOccurrence(function);
        removalQueue.clear();

        for (LuaFunction function : queue)
            functions.addLast(function);
        queue.clear();
    }

    // Calls all the functions in the order they were registered, using the given args for all calls.
    // If piped, the result of one function is passed through to the next, repeatedly, eventually returning the result.
    public Varargs call(Varargs args) {
        flushQueue();

        if (piped)
            return callPiped(args);

        LuaTable result = new LuaTable();
        for (LuaFunction function : functions) {
            CosmeticMod.pushProfiler(function.name());
            Varargs val = function.invoke(args);
            for (int i = 0; i < val.narg(); i++)
                result.insert(0, val.arg(i + 1));
            CosmeticMod.popProfiler();
        }
        return result.unpack();
    }

    private Varargs callPiped(Varargs args) {
        Varargs vars = args;
        for (LuaFunction function : functions) {
            CosmeticMod.pushProfiler(function.name());
            vars = function.invoke(vars);
            CosmeticMod.popProfiler();
        }
        return vars;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = LuaFunction.class,
                            argumentNames = "func"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {LuaFunction.class, String.class},
                            argumentNames = {"func", "name"}
                    )
            },
            value = "event.register"
    )
    public LuaEvent register(@LuaNotNil LuaFunction func, String name) {
        if (__len() >= MAX_FUNCTIONS)
            throw new LuaError("Reached maximum limit of " + MAX_FUNCTIONS + " functions in one event!");
        queue.addLast(func);
        if (name != null)
            names.put(name, func);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc("event.clear")
    public void clear() {
        functions.clear();
        queue.clear();
        removalQueue.clear();
        names.clear();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "name"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = LuaFunction.class,
                            argumentNames = "function"
                    )
            },
            value = "event.remove"
    )
    public int remove(@LuaNotNil Object toRemove) {
        flushQueue();
        if (toRemove instanceof LuaFunction func) {
            removalQueue.add(func);
            names.values().remove(func);
            return 1;
        } else if (toRemove instanceof String name) {
            int removed = 0;

            Set<LuaFunction> set = names.removeAll(name);
            for (LuaFunction function : set) {
                if (removalQueue.add(function))
                    removed++;
            }

            return removed;
        } else {
            throw new LuaError("Illegal argument to remove(): " + toRemove.getClass().getSimpleName());
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "name"
            ),
            value = "event.get_registered_count"
    )
    public int getRegisteredCount(@LuaNotNil String name) {
        return names.get(name).size();
    }

    @LuaWhitelist
    @LuaMetamethodDoc(overloads = {
            @LuaMetamethodOverload(
                    types = {int.class, LuaEvent.class}
            )
    })
    public int __len() {
        return functions.size() + queue.size();
    }

    @Override
    public String toString() {
        return "Event";
    }
}
