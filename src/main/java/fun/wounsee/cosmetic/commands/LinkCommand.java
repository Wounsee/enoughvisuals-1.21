package fun.wounsee.cosmetic.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.utils.ColorUtils;
import fun.wounsee.cosmetic.utils.FiguraClientCommandSource;
import fun.wounsee.cosmetic.utils.FiguraText;

import java.util.ArrayList;
import java.util.List;

class LinkCommand {

    private static final List<CosmeticMod.Links> LINKS = new ArrayList<>() {{
            add(CosmeticMod.Links.Wiki);
            add(CosmeticMod.Links.Kofi);
            add(CosmeticMod.Links.OpenCollective);
            add(null);
            add(CosmeticMod.Links.Discord);
            add(CosmeticMod.Links.Github);
            add(null);
            add(CosmeticMod.Links.Modrinth);
            add(CosmeticMod.Links.Curseforge);
    }};

    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        // get links
        LiteralArgumentBuilder<FiguraClientCommandSource> links = LiteralArgumentBuilder.literal("links");
        links.executes(context -> {
            // header
            MutableComponent message = Component.empty().withStyle(ColorUtils.Colors.AWESOME_BLUE.style)
                    .append(Component.literal("•*+•* ")
                            .append(FiguraText.of())
                            .append(" Links *•+*•").withStyle(ChatFormatting.UNDERLINE))
                    .append("\n");

            // add links
            for (CosmeticMod.Links link : LINKS) {
                message.append("\n");

                if (link == null)
                    continue;

                message.append(Component.literal("• [" + link.name() + "]")
                        .withStyle(link.style)
                        .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link.url)))
                        .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(link.url)))));
            }

            CosmeticMod.sendChatMessage(message);
            return 1;
        });

        return links;
    }
}
