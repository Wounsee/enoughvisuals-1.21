package fun.enoughvisuals.utils.render;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiAlphaLayerTest {
    @Test
    void restoresDrawThenReadThroughTheStateAwareBinder() {
        List<Binding> bindings = new ArrayList<>();

        GuiAlphaLayer.syncFramebufferBindings(17, 23, value -> value,
                (target, framebuffer) -> bindings.add(new Binding(target, framebuffer)));

        assertEquals(List.of(
                new Binding(GL30.GL_DRAW_FRAMEBUFFER, 17),
                new Binding(GL30.GL_READ_FRAMEBUFFER, 23)
        ), bindings);
    }

    @Test
    void validatesSharedFramebufferOnlyOnce() {
        AtomicInteger validations = new AtomicInteger();
        List<Binding> bindings = new ArrayList<>();

        GuiAlphaLayer.syncFramebufferBindings(17, 17, value -> {
            validations.incrementAndGet();
            return 5;
        }, (target, framebuffer) -> bindings.add(new Binding(target, framebuffer)));

        assertEquals(1, validations.get());
        assertEquals(List.of(
                new Binding(GL30.GL_DRAW_FRAMEBUFFER, 5),
                new Binding(GL30.GL_READ_FRAMEBUFFER, 5)
        ), bindings);
    }

    private record Binding(int target, int framebuffer) {
    }
}
