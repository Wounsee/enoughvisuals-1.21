package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.client.gui.DrawContext;

@Register(name="Shulker View", id="shulker_view", description="Показывает содержимое шалкеров", category=Category.VISUAL)
public class ShulkerView extends Module {
    public static void renderOverlay(DrawContext ctx, float tickDelta) {}
}
