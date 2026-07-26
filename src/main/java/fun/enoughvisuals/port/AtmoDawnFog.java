package fun.enoughvisuals.port;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.nio.FloatBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import fun.enoughvisuals.port.api.Module;
import fun.enoughvisuals.port.api.ModuleRegister;

@ModuleRegister(
   name = "AtmoDawnFog",
   description = "Кинематографичная атмосфера: туман, лучи света, заря",
   category = Category.Visuals
)
public class AtmoDawnFog extends Module {
   private static final String ASSETS_PORTED_SHADERS_WORLD_WORLD_VOLUME_VERT = "assets/enoughfx/shaders/world/world_volume.vert";
   private static final String ASSETS_PORTED_SHADERS_DAWNFOG_WORLD_FOG_FRESNEL_FRAG = "assets/enoughfx/shaders/dawnfog/world_fog_fresnel.frag";
   private static final String RASSVET = "Рассвет";
   private static final String SUMERKI = "Сумерки";
   private static final String TEMA = "Тема";
   private static final float FLOAT_VALUE = 18.0F;
   private static final float FLOAT_VALUE_2 = 1.0E-4F;
   private static final int INT_VALUE = 13203624;
   private static final int INT_VALUE_2 = 8230143;
   public final ModeSetting rezhim = new ModeSetting("Режим", "Рассвет", "Рассвет", "Сумерки", "Тема");
   public final NumberSetting plotnost = new NumberSetting("Плотность", 0.35F, 0.05F, 0.8F, 0.01F, false);
   public final NumberSetting vysotaRasseivaniya = new NumberSetting("Высота рассеивания", 76.0F, 60.0F, 120.0F, 1.0F, false);
   public final NumberSetting luchiSveta = new NumberSetting("Лучи света", 0.75F, 0.0F, 1.0F, 0.01F, true);
   public final NumberSetting myagkost = new NumberSetting("Мягкость", 0.6F, 0.0F, 1.0F, 0.01F, true);
   public final BooleanSetting raduga = new BooleanSetting("Радуга", true);
   public final NumberSetting yarkostRadugi = new NumberSetting("Яркость радуги", 0.55F, 0.1F, 1.0F, 0.01F, true)
      .setVisibilityCondition(() -> !this.raduga.isEnabled());
   public final NumberSetting razmerRadugi = new NumberSetting("Размер радуги", 54.0F, 46.0F, 60.0F, 0.5F, false)
      .setVisibilityCondition(() -> !this.raduga.isEnabled());
   public final HudColorSetting tsvetZari = new HudColorSetting("Цвет зари", new Color(255, 173, 122)).resolve(() -> !this.rezhim.is("Рассвет"));
   private final Matrix4f matrix4f = new Matrix4f();
   private final Matrix4f matrix4f2 = new Matrix4f();
   private final Matrix4f matrix4f3 = new Matrix4f();
   private final Matrix4f matrix4f4 = new Matrix4f();
   private final Vector4f vector4f = new Vector4f();
   private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
   private GlShaderProgram glShaderProgram;
   private int intValue = -1;
   private int intValue2 = -1;
   private int intValue3 = -1;
   private int intValue4 = -1;
   private int intValue5 = -1;
   private int intValue6 = -1;
   private int intValue7 = -1;
   private int intValue8 = -1;
   private int intValue9 = -1;
   private int intValue10 = -1;
   private int intValue11 = -1;
   private int intValue12 = -1;
   private int intValue13 = -1;
   private int intValue14 = -1;
   private int intValue15 = -1;
   private int intValue16 = -1;
   private int intValue17 = -1;
   private int intValue18 = -1;
   private int intValue19 = -1;
   private int intValue20 = -1;
   private int intValue21 = -1;
   private int intValue22 = -1;
   private int intValue23 = -1;
   private int intValue24 = -1;
   private int intValue25;
   private int intValue26;
   private int intValue27;
   private int intValue28;
   private int intValue29;
   private int intValue30;
   private int intValue31;
   private int intValue32;
   private boolean flag;
   private boolean flag2;
   private float floatValue = 0.5F;
   private float floatValue2 = 0.5F;
   private float floatValue3;
   private float floatValue4;
   private float floatValue5 = -0.39F;
   private final float[] floats = new float[18];

   public AtmoDawnFog() {
      this.addSettings(
         new Setting[]{
            this.rezhim,
            this.plotnost,
            this.vysotaRasseivaniya,
            this.luchiSveta,
            this.myagkost,
            this.raduga,
            this.yarkostRadugi,
            this.razmerRadugi,
            this.tsvetZari
         }
      );
   }

   public static boolean check() {
      return VisualPortRegistry.atmoDawnFog != null
         && VisualPortRegistry.atmoDawnFog.enabled
         && !VisualPortRegistry.atmoDawnFog.flag2;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.invoke8();
   }

   @EventHandler(
      priority = 1
   )
   public void onWorldRenderContext(WorldRenderContextEvent worldRenderContextEvent) {
      if (this.enabled && !this.flag2 && worldRenderContextEvent != null) {
         if (RenderSystem.isOnRenderThread()) {
            MinecraftClient client = worldRenderContextEvent.getClient();
            if (client != null && client.world != null && client.player != null && worldRenderContextEvent.getWorldRenderCapture() != null) {
               Camera camera = worldRenderContextEvent.getWorldRenderCapture().getCamera();
               if (camera != null) {
                  Window window = client.getWindow();
                  if (window != null && !window.hasZeroWidthOrHeight()) {
                     int intValue = window.getFramebufferWidth();
                     int intValue2 = window.getFramebufferHeight();
                     if (intValue > 1 && intValue2 > 1) {
                        Framebuffer framebuffer = client.getFramebuffer();
                        if (framebuffer != null) {
                           int intValue3 = compute3(framebuffer.getColorAttachment());
                           int intValue4 = compute3(framebuffer.getDepthAttachment());
                           if (intValue3 > 0 && intValue4 > 0) {
                              if (!(this.plotnost.getValue() <= 1.0E-4F)) {
                                 float floatValue = worldRenderContextEvent.getFloatValue();
                                 float floatValue2 = ((float)(client.world.getTime() % 100000L) + floatValue) * 0.05F;
                                 float floatValue3 = client.world.getSkyAngleRadians(floatValue);
                                 float floatValue4 = -((float)Math.sin(floatValue3));
                                 int intValue5 = this.compute();
                                 float floatValue5 = this.measure2(intValue5);
                                 float floatValue6 = floatValue4 >= 0.0F ? 1.0F : -1.0F;
                                 float floatValue7 = floatValue6 * (float)Math.cos(floatValue5);
                                 float floatValue8 = (float)Math.sin(floatValue5);
                                 float floatValue9 = 0.3F;
                                 this.floatValue4 = -floatValue6 * (float)Math.cos(floatValue9);
                                 this.floatValue5 = -((float)Math.sin(floatValue9));
                                 int intValue6 = this.compute2(intValue5);
                                 float floatValue10 = 192.0F;
                                 if (client.options != null) {
                                    floatValue10 = ((Integer)client.options.getViewDistance().getValue()).intValue() * 16.0F;
                                 }

                                 FramebufferUtils.GlStateSnapshot glStateSnapshot = FramebufferUtils.captureGlState();
                                 boolean flag = false ;

                                 label219: {
                                    label206: {
                                       label220: {
                                          try {
                                             flag = true;
                                             this.invoke7();
                                             if (!this.flag2) {
                                                if (this.check3(intValue, intValue2)) {
                                                   if (!this.check2(intValue3, intValue, intValue2)) {
                                                      flag = false;
                                                      break label219;
                                                   }

                                                   Vec3d vec3d2 = camera.getPos();
                                                   this.matrix4f3.set(worldRenderContextEvent.resolve2());
                                                   this.matrix4f4.set(worldRenderContextEvent.resolve3());
                                                   this.invoke2(floatValue7, floatValue8);
                                                   this.matrix4f.set(this.matrix4f4).invert();
                                                   this.matrix4f2.set(this.matrix4f3).invert();
                                                   this.matrix4f2.m30((float)vec3d2.x);
                                                   this.matrix4f2.m31((float)vec3d2.y);
                                                   this.matrix4f2.m32((float)vec3d2.z);
                                                   this.invoke(intValue3, intValue4, intValue, intValue2, vec3d2, floatValue2, floatValue7, floatValue8, intValue5, intValue6, floatValue10);
                                                   flag = false;
                                                   break label206;
                                                }

                                                flag = false;
                                             } else {
                                                flag = false;
                                             }
                                             break label220;
                                          } catch (Throwable exception) {
                                             this.flag2 = true;
                                             System.err.println("[AtmoDawnFog] renderer disabled: " + exception.getMessage());
                                             exception.printStackTrace();
                                             flag = false;
                                          } finally {
                                             if (flag) {
                                                if (this.intValue30 != 0) {
                                                   GL30.glBindFramebuffer(36160, this.intValue30);
                                                   GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                                                }

                                                GL13.glActiveTexture(33985);
                                                GL11.glBindTexture(3553, 0);
                                                GL13.glActiveTexture(33984);
                                                GL11.glBindTexture(3553, 0);
                                                GL20.glUseProgram(0);
                                                FramebufferUtils.restoreGlState(glStateSnapshot);
                                             }
                                          }

                                          if (this.intValue30 != 0) {
                                             GL30.glBindFramebuffer(36160, this.intValue30);
                                             GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                                          }

                                          GL13.glActiveTexture(33985);
                                          GL11.glBindTexture(3553, 0);
                                          GL13.glActiveTexture(33984);
                                          GL11.glBindTexture(3553, 0);
                                          GL20.glUseProgram(0);
                                          FramebufferUtils.restoreGlState(glStateSnapshot);
                                          return;
                                       }

                                       if (this.intValue30 != 0) {
                                          GL30.glBindFramebuffer(36160, this.intValue30);
                                          GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                                       }

                                       GL13.glActiveTexture(33985);
                                       GL11.glBindTexture(3553, 0);
                                       GL13.glActiveTexture(33984);
                                       GL11.glBindTexture(3553, 0);
                                       GL20.glUseProgram(0);
                                       FramebufferUtils.restoreGlState(glStateSnapshot);
                                       return;
                                    }

                                    if (this.intValue30 != 0) {
                                       GL30.glBindFramebuffer(36160, this.intValue30);
                                       GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                                    }

                                    GL13.glActiveTexture(33985);
                                    GL11.glBindTexture(3553, 0);
                                    GL13.glActiveTexture(33984);
                                    GL11.glBindTexture(3553, 0);
                                    GL20.glUseProgram(0);
                                    FramebufferUtils.restoreGlState(glStateSnapshot);
                                    return;
                                 }

                                 if (this.intValue30 != 0) {
                                    GL30.glBindFramebuffer(36160, this.intValue30);
                                    GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                                 }

                                 GL13.glActiveTexture(33985);
                                 GL11.glBindTexture(3553, 0);
                                 GL13.glActiveTexture(33984);
                                 GL11.glBindTexture(3553, 0);
                                 GL20.glUseProgram(0);
                                 FramebufferUtils.restoreGlState(glStateSnapshot);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void invoke(int i, int j, int k, int l, Vec3d vec3d, float f, float g, float h, int m, int n, float o) {
      GL30.glBindFramebuffer(36160, this.intValue30);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, i, 0);
      GL11.glDrawBuffer(36064);
      if (GL30.glCheckFramebufferStatus(36160) == 36053) {
         GL11.glViewport(0, 0, Math.max(0, k), Math.max(0, l));
         GL11.glDisable(3089);
         GL11.glDisable(2929);
         GL11.glDisable(2884);
         GL11.glDisable(3042);
         GL11.glDisable(36281);
         GL11.glColorMask(true, true, true, true);
         GL11.glDepthMask(false);
         this.glShaderProgram.invoke();
         float floatValue11 = this.vysotaRasseivaniya.getValue();
         float floatValue12 = floatValue11 - 18.0F;
         this.invoke3(m, n);
         if (this.intValue >= 0) {
            GL20.glUniform1i(this.intValue, 0);
         }

         if (this.intValue2 >= 0) {
            GL20.glUniform1i(this.intValue2, 1);
         }

         if (this.intValue3 >= 0) {
            GL20.glUniform2f(this.intValue3, k, l);
         }

         if (this.intValue4 >= 0) {
            GL20.glUniform1f(this.intValue4, f);
         }

         if (this.intValue5 >= 0) {
            GL20.glUniform3f(this.intValue5, (float)vec3d.x, (float)vec3d.y, (float)vec3d.z);
         }

         if (this.intValue6 >= 0) {
            this.invoke9(this.intValue6, this.matrix4f);
         }

         if (this.intValue7 >= 0) {
            this.invoke9(this.intValue7, this.matrix4f2);
         }

         if (this.intValue8 >= 0) {
            GL20.glUniform3f(this.intValue8, g, h, 0.0F);
         }

         if (this.intValue9 >= 0) {
            GL20.glUniform3f(this.intValue9, this.floatValue, this.floatValue2, this.floatValue3);
         }

         if (this.intValue10 >= 0) {
            GL20.glUniform1f(this.intValue10, measure3(this.plotnost.getValue(), 0.05F, 0.8F));
         }

         if (this.intValue11 >= 0) {
            GL20.glUniform1f(this.intValue11, floatValue12);
         }

         if (this.intValue12 >= 0) {
            GL20.glUniform1f(this.intValue12, floatValue11);
         }

         if (this.intValue13 >= 0) {
            GL20.glUniform1f(this.intValue13, o);
         }

         if (this.intValue14 >= 0) {
            GL20.glUniform3f(this.intValue14, this.floats[0], this.floats[1], this.floats[2]);
         }

         if (this.intValue15 >= 0) {
            GL20.glUniform3f(this.intValue15, this.floats[3], this.floats[4], this.floats[5]);
         }

         if (this.intValue16 >= 0) {
            GL20.glUniform3f(this.intValue16, this.floats[6], this.floats[7], this.floats[8]);
         }

         if (this.intValue17 >= 0) {
            GL20.glUniform3f(this.intValue17, this.floats[9], this.floats[10], this.floats[11]);
         }

         if (this.intValue18 >= 0) {
            GL20.glUniform3f(this.intValue18, this.floats[12], this.floats[13], this.floats[14]);
         }

         if (this.intValue19 >= 0) {
            GL20.glUniform3f(this.intValue19, this.floats[15], this.floats[16], this.floats[17]);
         }

         if (this.intValue20 >= 0) {
            GL20.glUniform1f(this.intValue20, this.raduga.isEnabled() ? measure3(this.yarkostRadugi.getValue(), 0.0F, 1.0F) : 0.0F);
         }

         if (this.intValue21 >= 0) {
            GL20.glUniform3f(this.intValue21, this.floatValue4, this.floatValue5, 0.0F);
         }

         if (this.intValue22 >= 0) {
            GL20.glUniform1f(this.intValue22, measure3(this.razmerRadugi.getValue(), 40.0F, 64.0F));
         }

         if (this.intValue23 >= 0) {
            GL20.glUniform1f(this.intValue23, measure3(this.luchiSveta.getValue(), 0.0F, 1.0F));
         }

         if (this.intValue24 >= 0) {
            GL20.glUniform1f(this.intValue24, measure3(this.myagkost.getValue(), 0.0F, 1.0F));
         }

         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, this.intValue26);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, j);
         GL13.glActiveTexture(33984);
         GL30.glBindVertexArray(this.intValue31);
         GlStateGuard.getINSTANCE().invoke2(2);
         GL11.glDrawArrays(4, 0, 6);
         GL30.glBindVertexArray(0);
      }
   }

   private void invoke2(float f, float g) {
      this.floatValue = 0.5F;
      this.floatValue2 = 0.5F;
      this.floatValue3 = 0.0F;
      this.vector4f.set(f, g, 0.0F, 0.0F);
      this.matrix4f3.transform(this.vector4f);
      float floatValue13 = this.vector4f.x;
      float floatValue14 = this.vector4f.y;
      float floatValue15 = this.vector4f.z;
      float floatValue16 = -floatValue15;
      if (!(floatValue16 <= 1.0E-4F)) {
         this.vector4f.set(floatValue13 * 1000.0F, floatValue14 * 1000.0F, floatValue15 * 1000.0F, 1.0F);
         this.matrix4f4.transform(this.vector4f);
         if (!(this.vector4f.w <= 1.0E-4F)) {
            this.floatValue = this.vector4f.x / this.vector4f.w * 0.5F + 0.5F;
            this.floatValue2 = this.vector4f.y / this.vector4f.w * 0.5F + 0.5F;
            this.floatValue3 = measure3(floatValue16 * 4.0F, 0.0F, 1.0F);
         }
      }
   }

   private void invoke3(int i, int j) {
      Color color = this.tsvetZari.getColor();
      float floatValue17 = color.getRed() / 255.0F;
      float floatValue18 = color.getGreen() / 255.0F;
      float floatValue19 = color.getBlue() / 255.0F;
      float floatValue20 = (j >> 16 & 0xFF) / 255.0F;
      float floatValue21 = (j >> 8 & 0xFF) / 255.0F;
      float floatValue22 = (j & 0xFF) / 255.0F;
      if (i == 1) {
         invoke4(this.floats, 0, 0.135F, 0.125F, 0.3F);
         invoke4(this.floats, 3, 0.89F, 0.46F, 0.55F);
         invoke4(this.floats, 6, 0.38F, 0.35F, 0.56F);
         invoke4(this.floats, 9, 0.8F, 0.52F, 0.62F);
         invoke4(this.floats, 12, 0.47F, 0.44F, 0.64F);
         invoke4(this.floats, 15, 0.92F, 0.56F, 0.72F);
      } else if (i == 2) {
         invoke5(this.floats, 0, 0.085F, 0.1F, 0.2F, floatValue20, floatValue21, floatValue22, 0.3F);
         invoke5(this.floats, 3, floatValue20, floatValue21, floatValue22, 1.0F, 0.93F, 0.82F, 0.35F);
         invoke5(this.floats, 6, 0.52F, 0.58F, 0.74F, floatValue20, floatValue21, floatValue22, 0.28F);
         invoke5(this.floats, 9, floatValue20, floatValue21, floatValue22, 0.97F, 0.93F, 0.88F, 0.45F);
         invoke5(this.floats, 12, 0.58F, 0.63F, 0.76F, floatValue20, floatValue21, floatValue22, 0.35F);
         invoke5(this.floats, 15, floatValue20, floatValue21, floatValue22, 1.0F, 0.96F, 0.88F, 0.3F);
      } else {
         invoke5(this.floats, 0, 0.16F, 0.19F, 0.38F, floatValue17, floatValue18, floatValue19, 0.14F);
         invoke4(this.floats, 3, measure3(floatValue17 * 1.12F, 0.0F, 1.0F), measure3(floatValue18 * 0.88F, 0.0F, 1.0F), measure3(floatValue19 * 0.62F, 0.0F, 1.0F));
         invoke4(this.floats, 6, 0.56F, 0.62F, 0.8F);
         invoke5(this.floats, 9, floatValue17, floatValue18, floatValue19, 0.95F, 0.55F, 0.63F, 0.42F);
         invoke4(this.floats, 12, 0.6F, 0.67F, 0.82F);
         invoke4(this.floats, 15, measure3(floatValue17 * 1.08F, 0.0F, 1.0F), measure3(floatValue18 * 0.94F, 0.0F, 1.0F), measure3(floatValue19 * 0.72F, 0.0F, 1.0F));
      }
   }

   private static void invoke4(float[] fs, int i, float f, float g, float h) {
      fs[i] = f;
      fs[i + 1] = g;
      fs[i + 2] = h;
   }

   private static void invoke5(float[] fs, int i, float f, float g, float h, float j, float k, float l, float m) {
      float floatValue23 = measure3(m, 0.0F, 1.0F);
      float floatValue24 = measure(f);
      float floatValue25 = measure(g);
      float floatValue26 = measure(h);
      float floatValue27 = measure(j);
      float floatValue28 = measure(k);
      float floatValue29 = measure(l);
      float floatValue30 = (float)Math.cbrt(0.41222146F * floatValue24 + 0.53633255F * floatValue25 + 0.051445995F * floatValue26);
      float floatValue31 = (float)Math.cbrt(0.2119035F * floatValue24 + 0.6806995F * floatValue25 + 0.10739696F * floatValue26);
      float floatValue32 = (float)Math.cbrt(0.08830246F * floatValue24 + 0.28171885F * floatValue25 + 0.6299787F * floatValue26);
      float floatValue33 = (float)Math.cbrt(0.41222146F * floatValue27 + 0.53633255F * floatValue28 + 0.051445995F * floatValue29);
      float floatValue34 = (float)Math.cbrt(0.2119035F * floatValue27 + 0.6806995F * floatValue28 + 0.10739696F * floatValue29);
      float floatValue35 = (float)Math.cbrt(0.08830246F * floatValue27 + 0.28171885F * floatValue28 + 0.6299787F * floatValue29);
      float floatValue36 = floatValue30 + (floatValue33 - floatValue30) * floatValue23;
      float floatValue37 = floatValue31 + (floatValue34 - floatValue31) * floatValue23;
      float floatValue38 = floatValue32 + (floatValue35 - floatValue32) * floatValue23;
      float floatValue39 = floatValue36 * floatValue36 * floatValue36;
      float floatValue40 = floatValue37 * floatValue37 * floatValue37;
      float floatValue41 = floatValue38 * floatValue38 * floatValue38;
      float floatValue42 = 4.0767417F * floatValue39 - 3.3077116F * floatValue40 + 0.23096994F * floatValue41;
      float floatValue43 = -1.268438F * floatValue39 + 2.6097574F * floatValue40 - 0.34131938F * floatValue41;
      float floatValue44 = -0.0041960864F * floatValue39 - 0.7034186F * floatValue40 + 1.7076147F * floatValue41;
      fs[i] = setF(floatValue42);
      fs[i + 1] = setF(floatValue43);
      fs[i + 2] = setF(floatValue44);
   }

   private static float measure(float f) {
      return f <= 0.04045F ? f / 12.92F : (float)Math.pow((f + 0.055F) / 1.055F, 2.4);
   }

   private static float setF(float f) {
      f = measure3(f, 0.0F, 1.0F);
      return f <= 0.0031308F ? f * 12.92F : (float)(1.055 * Math.pow(f, 0.4166666666666667) - 0.055);
   }

   private int compute() {
      if (this.rezhim.is("Сумерки")) {
         return 1;
      } else {
         return this.rezhim.is("Тема") ? 2 : 0;
      }
   }

   private float measure2(int i) {
      if (i == 1) {
         return -0.045F;
      } else {
         return i == 2 ? 0.13F : 0.11F;
      }
   }

   private int compute2(int i) {
      if (i == 1) {
         return 13203624;
      } else if (i == 2) {
         try {
            return fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccent() & 16777215;
         } catch (Throwable exception2) {
         }

         return 8230143;
      } else {
         return this.tsvetZari.compute() & 16777215;
      }
   }

   private boolean check2(int i, int j, int k) {
      if (i > 0 && this.intValue25 != 0) {
         if (this.intValue29 == 0) {
            this.intValue29 = GL30.glGenFramebuffers();
         }

         GL11.glDisable(3089);
         GL11.glDisable(3042);
         GL11.glDisable(2884);
         GL11.glDisable(2929);
         GL11.glDisable(36281);
         GL30.glBindFramebuffer(36008, this.intValue29);
         GL30.glFramebufferTexture2D(36008, 36064, 3553, i, 0);
         if (GL30.glCheckFramebufferStatus(36008) != 36053) {
            GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
            return false;
         } else {
            GL30.glBindFramebuffer(36009, this.intValue25);
            GL11.glReadBuffer(36064);
            GL11.glDrawBuffer(36064);
            GL30.glBlitFramebuffer(0, 0, j, k, 0, 0, j, k, 16384, 9728);
            GL30.glBindFramebuffer(36008, this.intValue29);
            GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean check3(int i, int j) {
      if (i > 0 && j > 0) {
         if (this.intValue26 != 0 && (this.intValue27 != i || this.intValue28 != j || this.intValue25 == 0)) {
            this.invoke6();
         }

         if (this.intValue26 == 0) {
            this.intValue26 = GL11.glGenTextures();
            GL11.glBindTexture(3553, this.intValue26);
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            RenderCapabilities.invoke(32856, i, j, 6408, 5121);
            this.intValue25 = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(36160, this.intValue25);
            GL30.glFramebufferTexture2D(36160, 36064, 3553, this.intValue26, 0);
            GL11.glDrawBuffer(36064);
            if (GL30.glCheckFramebufferStatus(36160) != 36053) {
               this.invoke6();
               return false;
            }
         }

         this.intValue27 = i;
         this.intValue28 = j;
         return true;
      } else {
         return false;
      }
   }

   private void invoke6() {
      if (this.intValue25 != 0) {
         GL30.glDeleteFramebuffers(this.intValue25);
         this.intValue25 = 0;
      }

      if (this.intValue26 != 0) {
         GL11.glDeleteTextures(this.intValue26);
         this.intValue26 = 0;
      }

      this.intValue27 = 0;
      this.intValue28 = 0;
   }

   private void invoke7() {
      if (!this.flag) {
         this.glShaderProgram = GlShaderProgram.resolve("assets/enoughfx/shaders/world/world_volume.vert", "assets/enoughfx/shaders/dawnfog/world_fog_fresnel.frag");
         this.intValue31 = GL30.glGenVertexArrays();
         this.intValue32 = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.intValue31);
         GL15.glBindBuffer(34962, this.intValue32);
         float[] floatValues = new float[]{
            -1.0F,
            -1.0F,
            0.0F,
            0.0F,
            1.0F,
            -1.0F,
            1.0F,
            0.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            0.0F,
            1.0F
         };
         GL15.glBufferData(34962, floatValues, 35044);
         byte byteValue = 16;
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, byteValue, 0L);
         GL20.glEnableVertexAttribArray(1);
         GL20.glVertexAttribPointer(1, 2, 5126, false, byteValue, 8L);
         GL15.glBindBuffer(34962, 0);
         GL30.glBindVertexArray(0);
         if (this.intValue30 == 0) {
            this.intValue30 = GL30.glGenFramebuffers();
         }

         this.intValue = this.glShaderProgram.compute2("u_ScreenTexture");
         this.intValue2 = this.glShaderProgram.compute2("u_DepthTexture");
         this.intValue3 = this.glShaderProgram.compute2("u_Resolution");
         this.intValue4 = this.glShaderProgram.compute2("u_Time");
         this.intValue5 = this.glShaderProgram.compute2("u_CameraPos");
         this.intValue6 = this.glShaderProgram.compute2("u_InverseProjectionMatrix");
         this.intValue7 = this.glShaderProgram.compute2("u_InverseViewMatrix");
         this.intValue8 = this.glShaderProgram.compute2("u_SunDirection");
         this.intValue9 = this.glShaderProgram.compute2("u_SunScreen");
         this.intValue10 = this.glShaderProgram.compute2("u_FogDensity");
         this.intValue11 = this.glShaderProgram.compute2("u_FogMinHeight");
         this.intValue12 = this.glShaderProgram.compute2("u_FogMaxHeight");
         this.intValue13 = this.glShaderProgram.compute2("u_ViewDistance");
         this.intValue14 = this.glShaderProgram.compute2("u_PaletteZenith");
         this.intValue15 = this.glShaderProgram.compute2("u_PaletteHorizonWarm");
         this.intValue16 = this.glShaderProgram.compute2("u_PaletteHorizonCool");
         this.intValue17 = this.glShaderProgram.compute2("u_PaletteFogWarm");
         this.intValue18 = this.glShaderProgram.compute2("u_PaletteFogCool");
         this.intValue19 = this.glShaderProgram.compute2("u_PaletteRay");
         this.intValue20 = this.glShaderProgram.compute2("u_Rainbow");
         this.intValue21 = this.glShaderProgram.compute2("u_RainbowDir");
         this.intValue22 = this.glShaderProgram.compute2("u_RainbowSize");
         this.intValue23 = this.glShaderProgram.compute2("u_GodRays");
         this.intValue24 = this.glShaderProgram.compute2("u_Softness");
         this.flag = true;
      }
   }

   private void invoke8() {
      if (RenderSystem.isOnRenderThread()) {
         this.invoke6();
         if (this.intValue29 != 0) {
            GL30.glDeleteFramebuffers(this.intValue29);
            this.intValue29 = 0;
         }

         if (this.intValue30 != 0) {
            GL30.glDeleteFramebuffers(this.intValue30);
            this.intValue30 = 0;
         }

         if (this.intValue31 != 0) {
            GL30.glDeleteVertexArrays(this.intValue31);
            this.intValue31 = 0;
         }

         if (this.intValue32 != 0) {
            GL15.glDeleteBuffers(this.intValue32);
            this.intValue32 = 0;
         }

         if (this.glShaderProgram != null) {
            this.glShaderProgram.invoke2();
            this.glShaderProgram = null;
         }

         this.flag = false;
         this.flag2 = false;
      }
   }

   private void invoke9(int i, Matrix4f matrix4f) {
      this.floatBuffer.clear();
      matrix4f.get(this.floatBuffer);
      GL20.glUniformMatrix4fv(i, false, this.floatBuffer);
   }

   private static int compute3(Object object) {
      // 1.21.4 exposes framebuffer attachments as raw OpenGL texture ids;
      // 1.21.8 wraps the same id in GlTexture.
      return object instanceof Integer textureId ? textureId : 0;
   }

   private static float measure3(float f, float g, float h) {
      return !Float.isFinite(f) ? g : Math.max(g, Math.min(h, f));
   }
}
