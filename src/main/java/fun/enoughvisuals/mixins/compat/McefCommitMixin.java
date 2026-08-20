package fun.enoughvisuals.mixins.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies MCEF's pinned java-cef revision when the protected loader exposes
 * the outer encrypted archive as CodeSource instead of the nested MCEF JAR.
 */
@Mixin(targets = "com.cinemamod.mcef.MCEF", remap = false)
public abstract class McefCommitMixin {
    private static final String JAVA_CEF_COMMIT = "eaeb3d4370aa3526ee237ad1981ad59af3de4dd1";

    @Inject(method = "getJavaCefCommit", at = @At("HEAD"), cancellable = true, remap = false)
    private static void enough$provideJavaCefCommit(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(JAVA_CEF_COMMIT);
    }
}
