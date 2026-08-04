package fun.wounsee.cosmetic.mixin.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Badges;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.utils.ColorUtils;
import fun.wounsee.cosmetic.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Mixin(SplashManager.class)
public class SplashManagerMixin {

    @Shadow @Final private static RandomSource RANDOM;
    @Shadow @Final private List<String> splashes;

    @Unique
    private static final List<Component> FIGURA_SPLASHES = List.of(
            Component.literal("Also try ears ")
                    .append(Component.literal("\uD83D\uDC3E").withStyle(Style.EMPTY.withFont(UIHelper.SPECIAL_FONT).withColor(ChatFormatting.WHITE)))
                    .append("!")
    );

    @Inject(at = @At("RETURN"), method = "getSplash")
    private void init(CallbackInfoReturnable<String> cir) {
        CosmeticMod.splashText = null;
        if (!Configs.EASTER_EGGS.value)
            return;

        Calendar calendar = CosmeticMod.CALENDAR;
        calendar.setTime(new Date());

        String who = null;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        switch (calendar.get(Calendar.MONTH)) {
            case Calendar.MARCH -> {
                if (day == 24) who = CosmeticMod.MOD_NAME;
            }
            case Calendar.JULY -> {
                if (day == 4) who = "Skylar";
            }
        }

        if (who != null) {
            CosmeticMod.splashText = Component.literal("Happy birthday " + who + " ")
                    .append(Badges.System.DEFAULT.badge.copy().withStyle(Style.EMPTY.withFont(Badges.FONT).withColor(ColorUtils.Colors.DEFAULT.hex)))
                    .append("!");
        } else {
            int size = this.splashes.size();
            int random = RANDOM.nextInt(size + FIGURA_SPLASHES.size());
            if (random >= size)
                CosmeticMod.splashText = FIGURA_SPLASHES.get(random - size);
        }
    }
}
