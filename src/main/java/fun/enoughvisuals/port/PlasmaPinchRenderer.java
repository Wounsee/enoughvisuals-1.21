package fun.enoughvisuals.port;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class PlasmaPinchRenderer {
   public static final int INT_VALUE = 12;
   private static final String ASSETS_PORTED_SHADERS_WORLD_PLASMA_PINCH_VSH = "assets/enoughfx/shaders/world/plasma_pinch.vsh";
   private static final String ASSETS_PORTED_SHADERS_WORLD_PLASMA_PINCH_FRAG = "assets/enoughfx/shaders/world/plasma_pinch.frag";
   private static final PlasmaPinchRenderer INSTANCE = new PlasmaPinchRenderer();
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
   private int intValue12;
   private int intValue13;
   private int intValue14;
   private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
   private final FloatBuffer floatBuffer2 = BufferUtils.createFloatBuffer(16);
   private final FloatBuffer floatBuffer3 = BufferUtils.createFloatBuffer(36);
   private final FloatBuffer floatBuffer4 = BufferUtils.createFloatBuffer(12);
   private final FloatBuffer floatBuffer5 = BufferUtils.createFloatBuffer(48);
   private final FloatBuffer floatBuffer6 = BufferUtils.createFloatBuffer(36);
   private final Matrix4f matrix4f = new Matrix4f();
   private final Vector4f vector4f = new Vector4f();

   private PlasmaPinchRenderer() {
   }

   public static PlasmaPinchRenderer getINSTANCE() {
      return INSTANCE;
   }

   public void invoke(MinecraftClient minecraftClient, List<PlasmaPinchEffect> list, Matrix4f matrix4f, Matrix4f matrix4f2, Vec3d vec3d, float f, boolean depthOcclusion) {
      if (RenderSystem.isOnRenderThread()) {
         if (minecraftClient != null && list != null && !list.isEmpty() && matrix4f != null && matrix4f2 != null && vec3d != null) {
            Window window = minecraftClient.getWindow();
            if (window != null && !window.hasZeroWidthOrHeight()) {
               int intValue = window.getFramebufferWidth();
               int intValue2 = window.getFramebufferHeight();
               if (intValue > 0 && intValue2 > 0) {
                  long longValue = System.currentTimeMillis();
                  this.floatBuffer3.clear();
                  this.floatBuffer4.clear();
                  this.floatBuffer5.clear();
                  this.floatBuffer6.clear();
                  int intValue3 = 0;

                  for (PlasmaPinchEffect plasmaPinchEffect : list) {
                     if (intValue3 >= 12) {
                        break;
                     }

                     float floatValue = plasmaPinchEffect.measure(longValue);
                     if (!(floatValue >= 1.0F)) {
                        this.vector4f
                           .set((float)(plasmaPinchEffect.getDoubleValue() - vec3d.x), (float)(plasmaPinchEffect.getDoubleValue2() - vec3d.y), (float)(plasmaPinchEffect.getDoubleValue3() - vec3d.z), 1.0F);
                        matrix4f.transform(this.vector4f);
                        float floatValue2 = this.vector4f.x;
                        float floatValue3 = this.vector4f.y;
                        float floatValue4 = this.vector4f.z;
                        if (!(floatValue4 > plasmaPinchEffect.getFloatValue5() + 0.1F)) {
                           this.floatBuffer3.put(floatValue2).put(floatValue3).put(floatValue4);
                           this.floatBuffer4.put(floatValue);
                           this.floatBuffer5.put(plasmaPinchEffect.getFloatValue()).put(plasmaPinchEffect.getFloatValue2()).put(plasmaPinchEffect.getFloatValue4()).put(plasmaPinchEffect.getFloatValue3());
                           int intValue4 = plasmaPinchEffect.getIntValue();
                           this.floatBuffer6.put((intValue4 >> 16 & 0xFF) / 255.0F).put((intValue4 >> 8 & 0xFF) / 255.0F).put((intValue4 & 0xFF) / 255.0F);
                           intValue3++;
                        }
                     }
                  }

                  if (intValue3 != 0) {
                     this.floatBuffer3.flip();
                     this.floatBuffer4.flip();
                     this.floatBuffer5.flip();
                     this.floatBuffer6.flip();
                     this.invoke2();
                     if (this.glShaderProgram != null && this.intValue13 != 0) {
                        int intValue5 = this.compute(minecraftClient);
                        if (intValue5 > 0) {
                           int intValue6 = this.compute2(minecraftClient);
                           this.floatBuffer.clear();
                           matrix4f2.get(this.floatBuffer);
                           this.matrix4f.set(matrix4f2).invert();
                           this.floatBuffer2.clear();
                           this.matrix4f.get(this.floatBuffer2);
                           FramebufferUtils.GlStateSnapshot glStateSnapshot = FramebufferUtils.captureGlState();
                           boolean flag = false ;

                           label267: {
                              try {
                                 flag = true;
                                 if (this.intValue12 == 0) {
                                    this.intValue12 = GL30.glGenFramebuffers();
                                 }

                                 GL30.glBindFramebuffer(36160, this.intValue12);
                                 GL30.glFramebufferTexture2D(36160, 36064, 3553, intValue5, 0);
                                 GL11.glDrawBuffer(36064);
                                 if (GL30.glCheckFramebufferStatus(36160) != 36053) {
                                    flag = false;
                                    break label267;
                                 }

                                 GL11.glViewport(0, 0, Math.max(0, intValue), Math.max(0, intValue2));
                                 GL11.glDisable(3089);
                                 GL11.glDisable(2884);
                                 GL11.glDisable(2929);
                                 GL11.glDisable(36281);
                                 GL11.glColorMask(true, true, true, true);
                                 GL11.glDepthMask(false);
                                 GL11.glEnable(3042);
                                 GL14.glBlendEquation(32774);
                                 GL11.glBlendFunc(1, 769);
                                 this.glShaderProgram.invoke();
                                 if (this.intValue >= 0) {
                                    GL20.glUniform1i(this.intValue, 0);
                                 }

                                 if (this.intValue2 >= 0) {
                                    GL20.glUniformMatrix4fv(this.intValue2, false, this.floatBuffer);
                                 }

                                 if (this.intValue3 >= 0) {
                                    GL20.glUniformMatrix4fv(this.intValue3, false, this.floatBuffer2);
                                 }

                                 if (this.intValue4 >= 0) {
                                    GL20.glUniform2f(this.intValue4, intValue, intValue2);
                                 }

                                 if (this.intValue5 >= 0) {
                                    GL20.glUniform1f(this.intValue5, f);
                                 }

                                 if (this.intValue6 >= 0) {
                                    GL20.glUniform1i(this.intValue6, intValue3);
                                 }

                                 if (this.intValue7 >= 0) {
                                    GL20.glUniform1i(this.intValue7, depthOcclusion && intValue6 > 0 ? 1 : 0);
                                 }

                                 if (this.intValue8 >= 0) {
                                    GL20.glUniform3fv(this.intValue8, this.floatBuffer3);
                                 }

                                 if (this.intValue9 >= 0) {
                                    GL20.glUniform1fv(this.intValue9, this.floatBuffer4);
                                 }

                                 if (this.intValue10 >= 0) {
                                    GL20.glUniform4fv(this.intValue10, this.floatBuffer5);
                                 }

                                 if (this.intValue11 >= 0) {
                                    GL20.glUniform3fv(this.intValue11, this.floatBuffer6);
                                 }

                                 GL13.glActiveTexture(33984);
                                 GL11.glBindTexture(3553, intValue6);
                                 GL30.glBindVertexArray(this.intValue13);
                                 GL11.glDrawArrays(4, 0, 6);
                                 GL30.glBindVertexArray(0);
                                 flag = false;
                              } finally {
                                 if (flag) {
                                    if (this.intValue12 != 0) {
                                       GL30.glBindFramebuffer(36160, this.intValue12);
                                       GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                                    }

                                    GL13.glActiveTexture(33984);
                                    GL11.glBindTexture(3553, 0);
                                    GL20.glUseProgram(0);
                                    FramebufferUtils.restoreGlState(glStateSnapshot);
                                 }
                              }

                              if (this.intValue12 != 0) {
                                 GL30.glBindFramebuffer(36160, this.intValue12);
                                 GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                              }

                              GL13.glActiveTexture(33984);
                              GL11.glBindTexture(3553, 0);
                              GL20.glUseProgram(0);
                              FramebufferUtils.restoreGlState(glStateSnapshot);
                              return;
                           }

                           if (this.intValue12 != 0) {
                              GL30.glBindFramebuffer(36160, this.intValue12);
                              GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                           }

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

   private int compute(MinecraftClient minecraftClient) {
      Framebuffer framebuffer = minecraftClient.getFramebuffer();
      if (framebuffer == null) {
         return 0;
      } else {
         return framebuffer.getColorAttachment();
      }
   }

   private int compute2(MinecraftClient minecraftClient) {
      Framebuffer framebuffer2 = minecraftClient.getFramebuffer();
      if (framebuffer2 == null) {
         return 0;
      } else {
         return framebuffer2.getDepthAttachment();
      }
   }

   private void invoke2() {
      if (this.intValue13 == 0) {
         FramebufferUtils.GlStateSnapshot glStateSnapshot2 = FramebufferUtils.captureGlState();

         try {
            this.intValue13 = GL30.glGenVertexArrays();
            this.intValue14 = GL15.glGenBuffers();
            GL30.glBindVertexArray(this.intValue13);
            GL15.glBindBuffer(34962, this.intValue14);
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
         } finally {
            FramebufferUtils.restoreGlState(glStateSnapshot2);
         }
      }

      if (this.glShaderProgram == null) {
         try {
            this.glShaderProgram = GlShaderProgram.resolve("assets/enoughfx/shaders/world/plasma_pinch.vsh", "assets/enoughfx/shaders/world/plasma_pinch.frag");
            this.intValue = this.glShaderProgram.compute2("u_DepthTexture");
            this.intValue2 = this.glShaderProgram.compute2("u_Proj");
            this.intValue3 = this.glShaderProgram.compute2("u_InvProj");
            this.intValue4 = this.glShaderProgram.compute2("u_Resolution");
            this.intValue5 = this.glShaderProgram.compute2("u_Time");
            this.intValue6 = this.glShaderProgram.compute2("u_Count");
            this.intValue7 = this.glShaderProgram.compute2("u_DepthAvailable");
            this.intValue8 = this.glShaderProgram.compute2("u_Center[0]");
            this.intValue9 = this.glShaderProgram.compute2("u_LifeTime[0]");
            this.intValue10 = this.glShaderProgram.compute2("u_Params[0]");
            this.intValue11 = this.glShaderProgram.compute2("u_CoreTint[0]");
         } catch (Throwable exception) {
            this.glShaderProgram = null;
         }
      }
   }

   public void invoke3() {
      if (RenderSystem.isOnRenderThread()) {
         if (this.intValue12 != 0) {
            GL30.glDeleteFramebuffers(this.intValue12);
            this.intValue12 = 0;
         }

         if (this.intValue14 != 0) {
            GL15.glDeleteBuffers(this.intValue14);
            this.intValue14 = 0;
         }

         if (this.intValue13 != 0) {
            GL30.glDeleteVertexArrays(this.intValue13);
            this.intValue13 = 0;
         }

         if (this.glShaderProgram != null) {
            this.glShaderProgram.invoke2();
            this.glShaderProgram = null;
         }
      }
   }
}
