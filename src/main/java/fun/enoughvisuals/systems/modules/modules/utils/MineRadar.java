package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Register(name="Mine Checker", id="mine_checker", description="Детектит след шахту (работает токо на спавне)", category=Category.UTILS)
public class MineRadar extends Module {

    private static MineRadar INSTANCE;
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{2}:\\d{2}");

    private String lastType = null;
    private String lastTime = null;
    private long lastUpdateTime = 0;

    private static String currentMineText = null;

    public MineRadar() {
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        lastType = null;
        lastTime = null;
        currentMineText = null;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;

                                     
        if (mc.player.age % 10 != 0) return;

        String foundType = null;
        String foundTime = null;

                                                                                  
        Box searchBox = mc.player.getBoundingBox().expand(64.0);
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox, Entity::hasCustomName)) {
            if (entity.hasCustomName()) {
                                                                                    
                String rawText = entity.getCustomName().getString();
                String cleanText = rawText.replaceAll("(?i)[§&][0-9A-FK-OR]", "").trim();
                String lowerText = cleanText.toLowerCase();

                                 
                if (lowerText.contains("следующая:")) {
                    if (lowerText.contains("обычная")) foundType = "Обычная";
                    else if (lowerText.contains("мифическая")) foundType = "Мифическая";
                    else if (lowerText.contains("легендарная")) foundType = "Легендарная";
                }

                              
                Matcher matcher = TIME_PATTERN.matcher(cleanText);
                if (matcher.find()) {
                    foundTime = matcher.group();
                }
            }
        }

                                            
        if (foundType != null) {
            lastType = foundType;
            lastUpdateTime = System.currentTimeMillis();
        }
        if (foundTime != null) {
            lastTime = foundTime;
            lastUpdateTime = System.currentTimeMillis();
        }

                                                                         
        if (lastType != null && lastTime != null) {
            currentMineText = lastType + ": " + lastTime;
        }

                                                                                   
        if (System.currentTimeMillis() - lastUpdateTime > 15000) {
            lastType = null;
            lastTime = null;
            currentMineText = null;
        }
    }

                              
    public static String getMineText() {
                                                                                       
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return null;
        }
        return currentMineText;
    }
}
