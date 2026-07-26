package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SelectSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.LinkedHashSet;
import java.util.Set;

@Register(name="GlowESP",id="glow_esp",description="Шейдерное свечение сущностей и мира",category=Category.VISUAL)
public final class GlowESP extends Module {
    public static final String PLAYERS="Игроки",MOBS="Мобы",ITEMS="Предметы",SELF="Себя";
    private static GlowESP INSTANCE;
    public final ModeSetting area=add(new ModeSetting("Область","area","","Сущности","Сущности","Мир"));
    public final SelectSetting targets=add(new SelectSetting("Цели","targets","",new LinkedHashSet<>(Set.of(PLAYERS)),PLAYERS,MOBS,ITEMS,SELF));
    public final SliderSetting distance=add(new SliderSetting("Дистанция","distance","",96,8,256,1));
    public final ModeSetting effect=add(new ModeSetting("Эффект","effect","","Свечение + контур","Свечение + контур","Свечение","Контур"));
    public final SliderSetting glowSize=add(new SliderSetting("Размер свечения","glow_size","",10,2,32,1));
    public final SliderSetting glowBrightness=add(new SliderSetting("Яркость свечения","glow_brightness","",2,.25,5,.05));
    public final SliderSetting outlineWidth=add(new SliderSetting("Толщина контура","outline_width","",2,.5,6,.5));
    public final SliderSetting opacity=add(new SliderSetting("Прозрачность","opacity","",.92,.05,1,.01));
    public final ModeSetting colorMode=add(new ModeSetting("Режим цвета","color_mode","","Градиент","Градиент","Статичный"));
    public final ModeSetting colorSource=add(new ModeSetting("Источник цвета","color_source","","Тема","Тема","Свой"));
    public final ColorSetting primary=add(new ColorSetting("Основной цвет","primary","",0xFF5E7CFF));
    public final ColorSetting secondary=add(new ColorSetting("Второй цвет","secondary","",0xFFAA55FF));
    public final BooleanSetting highlightFriends=add(new BooleanSetting("Выделять друзей","highlight_friends","",true));
    public final ColorSetting friendColor=add(new ColorSetting("Цвет друзей","friend_color","",0xFFFFCC33));
    public GlowESP(){INSTANCE=this;glowSize.visibleWhen(this::hasGlow);glowBrightness.visibleWhen(this::hasGlow);outlineWidth.visibleWhen(this::hasOutline);primary.visibleWhen(()->colorSource.is("Свой"));secondary.visibleWhen(()->colorSource.is("Свой")&&colorMode.is("Градиент"));friendColor.visibleWhen(highlightFriends::get);}
    @Override protected void onDisable(){ShaderEspRenderer.reset();}
    public static GlowESP active(){return INSTANCE!=null&&INSTANCE.isEnabled()?INSTANCE:null;}
    public boolean hasGlow(){return !effect.is("Контур");}public boolean hasOutline(){return !effect.is("Свечение");}
    public int primaryColor(){return colorSource.is("Тема")?CustomTheme.hudAccent():primary.get();}
    public int secondaryColor(){return colorMode.is("Статичный")?primaryColor():(colorSource.is("Тема")?CustomTheme.hudAccentLight():secondary.get());}
    public double distanceSquared(){double d=distance.get();return d*d;}
    public boolean self(){return targets.isSelected(SELF);}
    public boolean worldMode(){return area.is("Мир");}
    public boolean shouldRender(Entity e){if(e==null||e.isRemoved()||!e.isAlive()||e.isSpectator()||e.isInvisible())return false;if(e==mc.player)return self();if(e instanceof PlayerEntity)return targets.isSelected(PLAYERS);if(e instanceof MobEntity)return targets.isSelected(MOBS);return e instanceof ItemEntity&&targets.isSelected(ITEMS);}
}
