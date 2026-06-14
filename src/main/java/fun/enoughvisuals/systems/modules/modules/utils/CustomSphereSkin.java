package fun.enoughvisuals.systems.modules.modules.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import net.minecraft.component.type.ProfileComponent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Register(
        name = "Custom Sphere Skin",
        id = "custom_sphere_skin",
        description = "Changes the skin texture used by worn player heads",
        category = Category.UTILS
)
public class CustomSphereSkin extends Module {
    private static final String PUMPKIN =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjAzMTM0ZThiMDAxOGExNDYyYmU2NDMwYzJmMTdiM2NlMzVmNzZhM2IwZGFmYWZhNGFkOWM4OWY0NTk5Mjk0YSJ9fX0=";
    private static final String SKELETON =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmJlY2M2NDVmMTI5YzhiYzJmYWE0ZDgxNDU0ODFmYWIxMWFkMmVlNzU3NDlkNjI4ZGNkOTk5YWE5NGU3In19fQ==";
    private static final String GHOST =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGZhMTMwMzJmYTkzOWYxODRkYWE2YTRlMTFlNmYzYTkxM2U0OGYyNTA0OTgxNjVjNTY2NWNjZjQ5YzcyYTE0MCJ9fX0=";
    private static final String STEVE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmJjZmYwZTNiNzY0ZjFkYzkwNzYxZjIzN2IwZTgwYjQ4ZTU3YzU1NDE5NDgwZmNjNzA2OTVkNzIwZTZjYTI5In19fQ==";

    public static CustomSphereSkin INSTANCE;

    private ProfileComponent cachedProfile;
    private String cachedValue = "";

    private final ModeSetting skin = add(new ModeSetting(
            "Skin",
            "skin",
            "Head texture preset",
            "Pumpkin",
            "Pumpkin",
            "Skeleton",
            "Ghost",
            "Custom",
            "Steve"
    ));

    public final StringSetting customCode = (StringSetting) add(new StringSetting(
            "Custom Code",
            "custom_code",
            "Base64 texture value",
            PUMPKIN,
            2048,
            "Value",
            StringSetting.InputMode.ANY
    )).visibleWhen(() -> skin.is("Custom"));

    public CustomSphereSkin() {
        INSTANCE = this;
    }

    public ProfileComponent getProfileForRender() {
        String value = getTextureValue();
        if (value == null || value.isBlank()) {
            value = PUMPKIN;
        }

        if (!value.equals(cachedValue) || cachedProfile == null) {
            cachedValue = value;
            cachedProfile = createProfile(value);
        }

        return cachedProfile;
    }

    public static ProfileComponent getActiveProfileForRender() {
        CustomSphereSkin module = INSTANCE;
        if (module == null || !module.isEnabled()) {
            return null;
        }

        return module.getProfileForRender();
    }

    private String getTextureValue() {
        return switch (skin.getIndex()) {
            case 1 -> SKELETON;
            case 2 -> GHOST;
            case 3 -> customCode.get();
            case 4 -> STEVE;
            default -> PUMPKIN;
        };
    }

    private ProfileComponent createProfile(String value) {
        UUID id = UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(id, profileName(id));
        profile.getProperties().put("textures", new Property("textures", value));
        return new ProfileComponent(profile);
    }

    private String profileName(UUID id) {
        return "Sphere" + id.toString().replace("-", "").substring(0, 10);
    }

    public static CustomSphereSkin getInstance() {
        return INSTANCE;
    }
}
