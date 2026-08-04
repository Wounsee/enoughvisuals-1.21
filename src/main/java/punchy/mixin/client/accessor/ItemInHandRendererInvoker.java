package punchy.mixin.client.accessor;

import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/ItemInHandRendererInvoker.class */
@Mixin({class_759.class})
public interface ItemInHandRendererInvoker {
    @Invoker("method_3219")
    void punchy$renderPlayerArm(class_4587 class_4587Var, class_4597 class_4597Var, int i, float f, float f2, class_1306 class_1306Var);

    @Invoker("method_3223")
    void punchy$renderMap(class_4587 class_4587Var, class_4597 class_4597Var, int i, class_1799 class_1799Var);
}
