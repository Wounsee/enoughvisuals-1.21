package fun.enoughvisuals.mixins.accessors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int itemUseCooldown);

    @Mutable
    @Accessor("session")
    void enough$setSession(Session session);

    @Mutable
    @Accessor("profileKeys")
    void enough$setProfileKeys(ProfileKeys profileKeys);
}
