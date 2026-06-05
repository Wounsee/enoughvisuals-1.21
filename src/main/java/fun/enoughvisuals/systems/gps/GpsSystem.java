package fun.enoughvisuals.systems.gps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fun.enoughvisuals.mixins.accessors.GameRendererAccessor;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class GpsSystem {
                                         
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

                                                                                 
    private static final GpsSystem INSTANCE = new GpsSystem();

                                                     
    private static class Waypoint {
        String name;
        double x, y, z;
        String scope;                                           
        boolean isEvent;
        long createdAt;

        Waypoint(String name, double x, double y, double z, String scope, boolean isEvent) {
            this.name = name; this.x = x; this.y = y; this.z = z;
            this.scope = scope; this.isEvent = isEvent;
            this.createdAt = System.currentTimeMillis();
        }

        Vec3d getPos() { return new Vec3d(x, y, z); }
    }

    private final List<Waypoint> waypoints = new CopyOnWriteArrayList<>();

    private GpsSystem() {
        load();
    }

    public static GpsSystem getInstance() { return INSTANCE; }

    public boolean openGuiIfBound(int key, MinecraftClient client) {
        return false;
    }

                            
    private Path getSaveFile() {
        return mc.runDirectory.toPath().resolve("enoughvisuals").resolve("gps.json");
    }

    private void load() {
        Path file = getSaveFile();
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            List<Waypoint> loaded = GSON.fromJson(reader, new TypeToken<List<Waypoint>>(){}.getType());
            if (loaded != null) {
                waypoints.clear();
                waypoints.addAll(loaded);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void save() {
        Path file = getSaveFile();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(waypoints, writer);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

                                        
    private String getCurrentScope() {
        if (mc.isInSingleplayer()) return "singleplayer";
        if (mc.getCurrentServerEntry() == null) return "unknown";
        String ip = mc.getCurrentServerEntry().address.toLowerCase();

        String subServer = "unknown";
        if (mc.world != null && mc.world.getScoreboard() != null) {
            var obj = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (obj != null && obj.getDisplayName() != null) {
                String title = obj.getDisplayName().getString().toLowerCase();
                Matcher m = Pattern.compile("анархия\\s*[- ]?\\s*(\\d+)?").matcher(title);
                if (m.find()) subServer = "anarchy-" + (m.group(1) != null ? m.group(1) : "unknown");
            }
        }
        return ip + "|" + subServer;
    }

    public void addWaypoint(String name, double x, double y, double z, boolean isEvent) {
        String scope = getCurrentScope();
        waypoints.removeIf(w -> w.scope.equals(scope) && w.name.equalsIgnoreCase(name));
        waypoints.add(new Waypoint(name, x, y, z, scope, isEvent));
        save();
    }

    public void onChatMessage(String message) {
        String trimmed = message.trim();
        if (trimmed.length() <= 4) {
            sendHelp();
            return;
        }

        String[] args = trimmed.substring(5).trim().split("\\s+");
        if (args.length == 0 || args[0].isEmpty()) {
            sendHelp();
            return;
        }

        String cmd = args[0].toLowerCase();
        String scope = getCurrentScope();

                                                                       
        List<Waypoint> activeWaypoints = waypoints.stream().filter(w -> w.scope.equals(scope)).collect(Collectors.toList());

        switch (cmd) {
            case "list" -> {
                if (activeWaypoints.isEmpty()) {
                    sendMessage("На этом сервере активных меток нет.");
                } else {
                    sendMessage("Активные метки:");
                    for (int i = 0; i < activeWaypoints.size(); i++) {
                        Waypoint w = activeWaypoints.get(i);
                        sendMessage(" §8" + (i + 1) + ". §d" + w.name + " §7(" + (int)w.x + " " + (int)w.y + " " + (int)w.z + ")");
                    }
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    sendMessage("Укажите номер: §f.gps remove <номер>");
                    return;
                }
                try {
                    int index = Integer.parseInt(args[1]) - 1;
                    if (index >= 0 && index < activeWaypoints.size()) {
                        Waypoint toRemove = activeWaypoints.get(index);
                        waypoints.remove(toRemove);
                        save();
                        sendMessage("Метка '§d" + toRemove.name + "§f' удалена.");
                    } else {
                        sendMessage("§cМетка с таким номером не найдена.");
                    }
                } catch (NumberFormatException e) {
                    sendMessage("§cНомер должен быть числом!");
                }
            }
            case "add" -> {
                if (args.length < 5) {
                    sendMessage("Использование: §f.gps add <имя> <x> <y> <z>");
                    return;
                }
                try {
                    double z = Double.parseDouble(args[args.length - 1]);
                    double y = Double.parseDouble(args[args.length - 2]);
                    double x = Double.parseDouble(args[args.length - 3]);

                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 1; i < args.length - 3; i++) {
                        nameBuilder.append(args[i]).append(i == args.length - 4 ? "" : " ");
                    }
                    String name = nameBuilder.toString();

                    addWaypoint(name, x, y, z, false);
                    sendMessage("Метка '§d" + name + "§f' установлена на §a" + (int)x + " " + (int)y + " " + (int)z);
                } catch (NumberFormatException e) {
                    sendMessage("§cКоординаты должны быть числами!");
                }
            }
            default -> sendHelp();
        }
    }

    private void sendHelp() {
        sendMessage("Помощь:");
        sendMessage(" §d.gps add <имя> <x> <y> <z> §f- поставить метку");
        sendMessage(" §d.gps list §f- список всех меток");
        sendMessage(" §d.gps remove <номер> §f- удалить метку");
    }

    private void sendMessage(String text) {
        if (mc.inGameHud != null) {
            MutableText prefix = fun.enoughvisuals.utils.font.TextGradientUtil.gradient("EnoughVisuals » ", 0x6D3BFF, 0xD2D5FF);
            MutableText message = Text.literal(text).setStyle(net.minecraft.text.Style.EMPTY.withColor(0xFFFFFF));
            mc.inGameHud.getChatHud().addMessage(prefix.append(message));
        }
    }

    public void renderOverlay(DrawContext ctx, float tickDelta) {
        if (mc.player == null || mc.world == null || waypoints.isEmpty()) return;

        long now = System.currentTimeMillis();
                                                      
        boolean removedOld = waypoints.removeIf(w -> w.isEvent && (now - w.createdAt > 900_000L));
        if (removedOld) save();

        String scope = getCurrentScope();
        Camera camera = mc.gameRenderer.getCamera();

        for (Waypoint w : waypoints) {
            if (!w.scope.equals(scope)) continue;

            Vec3d pos2d = projectToScreen(w.x, w.y + 1.5, w.z, camera, tickDelta);

            if (pos2d != null) {
                double dist = mc.player.getPos().distanceTo(w.getPos());
                drawGpsTag(ctx, w.name, (int)dist, (float)pos2d.x, (float)pos2d.y);
            }
        }
    }

    private void drawGpsTag(DrawContext ctx, String name, int distance, float x, float y) {
        String distText = distance + " блоков";
        float nameSize = 8.5f;
        float distSize = 7.0f;
        float iconSize = 18f;

        float ry = y - 20f;

        drawCenteredShadow(ctx, "\uF000", x, ry + 2, iconSize, 0xFF8B5CFF, CustomFontRenderer.FontWeight.MARK);
        drawCenteredShadow(ctx, name, x, ry + 22, nameSize, 0xFFFFFFFF, CustomFontRenderer.FontWeight.SEMIBOLD);
        drawCenteredShadow(ctx, distText, x, ry + 31, distSize, 0xFFFFFFFF, CustomFontRenderer.FontWeight.REGULAR);
    }

    private void drawCenteredShadow(DrawContext ctx, String text, float cx, float y, float size, int color, CustomFontRenderer.FontWeight weight) {
        float width = CustomFontRenderer.getTextWidth(text, size, weight);
        float startX = cx - width / 2f;
        CustomFontRenderer.drawText(ctx, text, startX, y, size, color, weight);
    }

    private static Vec3d projectToScreen(double x, double y, double z, Camera camera, float tickDelta) {
        Vec3d camPos = camera.getPos();
        Vector4f pos = new Vector4f((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z), 1.0f);

        Quaternionf rotation = new Quaternionf(camera.getRotation()).conjugate();
        pos.rotate(rotation);

        if (pos.z >= 0) return null;

        GameRendererAccessor accessor = (GameRendererAccessor) mc.gameRenderer;
        double fov = accessor.invokeGetFov(camera, tickDelta, true);
        float halfFovTan = (float) Math.tan(Math.toRadians(fov) / 2.0);

        float screenHeight = mc.getWindow().getScaledHeight();
        float scale = (screenHeight / 2.0f) / halfFovTan;

        return new Vec3d(
                mc.getWindow().getScaledWidth() / 2.0 + (pos.x / -pos.z) * scale,
                screenHeight / 2.0 - (pos.y / -pos.z) * scale,
                0
        );
    }
}