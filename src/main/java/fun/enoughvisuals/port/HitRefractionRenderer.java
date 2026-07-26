package fun.enoughvisuals.port;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class HitRefractionRenderer {
   public static final int INT_VALUE = 10;
   private static final String ASSETS_PORTED_SHADERS_WORLD_HIT_REFRACTION_VSH = "assets/enoughfx/shaders/world/hit_refraction.vsh";
   private static final String ASSETS_PORTED_SHADERS_WORLD_HIT_REFRACTION_FRAG = "assets/enoughfx/shaders/world/hit_refraction.frag";
   private static final HitRefractionRenderer INSTANCE = new HitRefractionRenderer();
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
   private int intValue14;
   private int intValue15;
   private int intValue16;
   private int intValue17;
   private int intValue18;
   private int intValue19;
   private int intValue20;
   private int intValue21;
   private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
   private final FloatBuffer floatBuffer2 = BufferUtils.createFloatBuffer(16);
   private final FloatBuffer floatBuffer3 = BufferUtils.createFloatBuffer(30);
   private final FloatBuffer floatBuffer4 = BufferUtils.createFloatBuffer(30);
   private final FloatBuffer floatBuffer5 = BufferUtils.createFloatBuffer(40);
   private final FloatBuffer floatBuffer6 = BufferUtils.createFloatBuffer(30);
   private final Matrix4f matrix4f = new Matrix4f();
   private final Vector4f vector4f = new Vector4f();
   private final Vector3f vector3f = new Vector3f();

   private HitRefractionRenderer() {
   }

   public static HitRefractionRenderer getINSTANCE() {
      return INSTANCE;
   }

   public void invoke(
      MinecraftClient minecraftClient, List<HitRefractionEffect> list, Matrix4f matrix4f, Matrix4f matrix4f2, Vec3d vec3d, boolean bl, boolean bl2, float f
   ) {
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

                  for (HitRefractionEffect hitRefractionEffect : list) {
                     if (intValue3 >= 10) {
                        break;
                     }

                     float floatValue = hitRefractionEffect.measure(longValue);
                     if (!(floatValue >= 1.0F)) {
                        float floatValue2 = 1.0F - (float)Math.pow(2.0, -9.0 * floatValue);
                        float floatValue3 = hitRefractionEffect.getFloatValue() * floatValue2;
                        float floatValue4 = hitRefractionEffect.getFloatValue() * 0.16F * (0.45F + 0.55F * (1.0F - floatValue));
                        float floatValue5 = measure(0.0F, 0.1F, floatValue);
                        float floatValue6 = 1.0F - measure(0.5F, 1.0F, floatValue);
                        float floatValue7 = measure2(floatValue5 * floatValue6, 0.0F, 1.0F);
                        if (!(floatValue7 <= 8.0E-4F)) {
                           float floatValue8 = hitRefractionEffect.getFloatValue2() * (0.7F + 0.3F * (1.0F - measure(0.15F, 1.0F, floatValue)));
                           this.vector4f
                              .set((float)(hitRefractionEffect.getDoubleValue() - vec3d.x), (float)(hitRefractionEffect.getDoubleValue2() - vec3d.y), (float)(hitRefractionEffect.getDoubleValue3() - vec3d.z), 1.0F);
                           matrix4f.transform(this.vector4f);
                           float floatValue9 = this.vector4f.x;
                           float floatValue10 = this.vector4f.y;
                           float floatValue11 = this.vector4f.z;
                           if (!(floatValue11 > floatValue3 + floatValue4 + 0.1F)) {
                              this.vector3f.set((float)hitRefractionEffect.getDoubleValue4(), (float)hitRefractionEffect.getDoubleValue5(), (float)hitRefractionEffect.getDoubleValue6());
                              matrix4f.transformDirection(this.vector3f);
                              float floatValue12 = this.vector3f.length();
                              if (floatValue12 < 1.0E-5F) {
                                 this.vector3f.set(0.0F, 1.0F, 0.0F);
                              } else {
                                 this.vector3f.div(floatValue12);
                              }

                              this.floatBuffer3.put(floatValue9).put(floatValue10).put(floatValue11);
                              this.floatBuffer4.put(this.vector3f.x).put(this.vector3f.y).put(this.vector3f.z);
                              this.floatBuffer5.put(floatValue3).put(floatValue4).put(floatValue7).put(floatValue8);
                              int intValue4 = hitRefractionEffect.getIntValue();
                              this.floatBuffer6.put((intValue4 >> 16 & 0xFF) / 255.0F).put((intValue4 >> 8 & 0xFF) / 255.0F).put((intValue4 & 0xFF) / 255.0F);
                              intValue3++;
                           }
                        }
                     }
                  }

                  if (intValue3 != 0) {
                     this.floatBuffer3.flip();
                     this.floatBuffer4.flip();
                     this.floatBuffer5.flip();
                     this.floatBuffer6.flip();
                     this.invoke2();
                     if (this.glShaderProgram != null && this.intValue20 != 0 && this.intValue >= 0) {
                        if (this.check2(intValue, intValue2)) {
                           int intValue5 = this.compute(minecraftClient);
                           if (intValue5 > 0) {
                              int intValue6 = bl2 ? this.compute2(minecraftClient) : 0;
                              if (this.check(intValue5, intValue, intValue2)) {
                                 this.floatBuffer.clear();
                                 matrix4f2.get(this.floatBuffer);
                                 this.matrix4f.set(matrix4f2).invert();
                                 this.floatBuffer2.clear();
                                 this.matrix4f.get(this.floatBuffer2);
                                 FramebufferUtils.GlStateSnapshot glStateSnapshot = FramebufferUtils.captureGlState();
                                 boolean flag = false ;

                                 label317: {
                                    try {
                                       flag = true;
                                       if (this.intValue19 == 0) {
                                          this.intValue19 = GL30.glGenFramebuffers();
                                       }

                                       GL30.glBindFramebuffer(36160, this.intValue19);
                                       GL30.glFramebufferTexture2D(36160, 36064, 3553, intValue5, 0);
                                       GL11.glDrawBuffer(36064);
                                       if (GL30.glCheckFramebufferStatus(36160) != 36053) {
                                          flag = false;
                                          break label317;
                                       }

                                       GL11.glViewport(0, 0, Math.max(0, intValue), Math.max(0, intValue2));
                                       GL11.glDisable(3089);
                                       GL11.glDisable(2884);
                                       GL11.glDisable(2929);
                                       GL11.glDisable(3042);
                                       GL11.glDisable(36281);
                                       GL11.glColorMask(true, true, true, true);
                                       GL11.glDepthMask(false);
                                       this.glShaderProgram.invoke();
                                       GL20.glUniform1i(this.intValue, 0);
                                       if (this.intValue2 >= 0) {
                                          GL20.glUniform1i(this.intValue2, 1);
                                       }

                                       if (this.intValue3 >= 0) {
                                          GL20.glUniformMatrix4fv(this.intValue3, false, this.floatBuffer);
                                       }

                                       if (this.intValue4 >= 0) {
                                          GL20.glUniformMatrix4fv(this.intValue4, false, this.floatBuffer2);
                                       }

                                       if (this.intValue5 >= 0) {
                                          GL20.glUniform2f(this.intValue5, intValue, intValue2);
                                       }

                                       if (this.intValue6 >= 0) {
                                          GL20.glUniform1f(this.intValue6, f);
                                       }

                                       if (this.intValue7 >= 0) {
                                          GL20.glUniform1i(this.intValue7, intValue3);
                                       }

                                       if (this.intValue8 >= 0) {
                                          GL20.glUniform1i(this.intValue8, bl ? 1 : 0);
                                       }

                                       if (this.intValue9 >= 0) {
                                          GL20.glUniform1i(this.intValue9, intValue6 > 0 ? 1 : 0);
                                       }

                                       if (this.intValue10 >= 0) {
                                          GL20.glUniform3fv(this.intValue10, this.floatBuffer3);
                                       }

                                       if (this.intValue11 >= 0) {
                                          GL20.glUniform3fv(this.intValue11, this.floatBuffer4);
                                       }

                                       if (this.intValue12 >= 0) {
                                          GL20.glUniform4fv(this.intValue12, this.floatBuffer5);
                                       }

                                       if (this.intValue13 >= 0) {
                                          GL20.glUniform3fv(this.intValue13, this.floatBuffer6);
                                       }

                                       GL13.glActiveTexture(33984);
                                       GL11.glBindTexture(3553, this.intValue15);
                                       GL13.glActiveTexture(33985);
                                       GL11.glBindTexture(3553, intValue6);
                                       GL13.glActiveTexture(33984);
                                       GL30.glBindVertexArray(this.intValue20);
                                       GL11.glDrawArrays(4, 0, 6);
                                       GL30.glBindVertexArray(0);
                                       flag = false;
                                    } finally {
                                       if (flag) {
                                          if (this.intValue19 != 0) {
                                             GL30.glBindFramebuffer(36160, this.intValue19);
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

                                    if (this.intValue19 != 0) {
                                       GL30.glBindFramebuffer(36160, this.intValue19);
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

                                 if (this.intValue19 != 0) {
                                    GL30.glBindFramebuffer(36160, this.intValue19);
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

   private boolean check(int i, int j, int k) {
      FramebufferUtils.GlStateSnapshot glStateSnapshot2 = FramebufferUtils.captureGlState();
      boolean flag2 = false ;

      boolean flag3;
      label70: {
         try {
            flag2 = true;
            if (this.intValue18 == 0) {
               this.intValue18 = GL30.glGenFramebuffers();
            }

            GL11.glDisable(3089);
            GL11.glDisable(3042);
            GL11.glDisable(2884);
            GL11.glDisable(2929);
            GL11.glDisable(36281);
            GL30.glBindFramebuffer(36008, this.intValue18);
            GL30.glFramebufferTexture2D(36008, 36064, 3553, i, 0);
            if (GL30.glCheckFramebufferStatus(36008) != 36053) {
               flag3 = false;
               flag2 = false;
               break label70;
            }

            GL30.glBindFramebuffer(36009, this.intValue14);
            GL11.glReadBuffer(36064);
            GL11.glDrawBuffer(36064);
            GL30.glBlitFramebuffer(0, 0, j, k, 0, 0, j, k, 16384, 9728);
            flag3 = true;
            flag2 = false;
         } finally {
            if (flag2) {
               if (this.intValue18 != 0) {
                  GL30.glBindFramebuffer(36008, this.intValue18);
                  GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
               }

               FramebufferUtils.restoreGlState(glStateSnapshot2);
            }
         }

         if (this.intValue18 != 0) {
            GL30.glBindFramebuffer(36008, this.intValue18);
            GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
         }

         FramebufferUtils.restoreGlState(glStateSnapshot2);
         return flag3;
      }

      if (this.intValue18 != 0) {
         GL30.glBindFramebuffer(36008, this.intValue18);
         GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
      }

      FramebufferUtils.restoreGlState(glStateSnapshot2);
      return flag3;
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

   private boolean check2(int i, int j) {
      if (this.intValue15 != 0 && (this.intValue16 != i || this.intValue17 != j || this.intValue14 == 0)) {
         this.invoke3();
      }

      FramebufferUtils.GlStateSnapshot glStateSnapshot3;
      boolean flag4;
      label70: {
         if (this.intValue15 == 0) {
            glStateSnapshot3 = FramebufferUtils.captureGlState();
            boolean flag5 = false ;

            try {
               flag5 = true;
               this.intValue15 = GL11.glGenTextures();
               GL11.glBindTexture(3553, this.intValue15);
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9729);
               GL11.glTexParameteri(3553, 10242, 33071);
               GL11.glTexParameteri(3553, 10243, 33071);
               RenderCapabilities.invoke(32856, i, j, 6408, 5121);
               this.intValue14 = GL30.glGenFramebuffers();
               GL30.glBindFramebuffer(36160, this.intValue14);
               GL30.glFramebufferTexture2D(36160, 36064, 3553, this.intValue15, 0);
               GL11.glDrawBuffer(36064);
               if (GL30.glCheckFramebufferStatus(36160) != 36053) {
                  this.invoke3();
                  flag4 = false;
                  flag5 = false;
                  break label70;
               }

               flag5 = false;
            } finally {
               if (flag5) {
                  FramebufferUtils.restoreGlState(glStateSnapshot3);
               }
            }

            FramebufferUtils.restoreGlState(glStateSnapshot3);
         }

         this.intValue16 = i;
         this.intValue17 = j;
         return true;
      }

      FramebufferUtils.restoreGlState(glStateSnapshot3);
      return flag4;
   }

   private void invoke2() {
      if (this.intValue20 == 0) {
         FramebufferUtils.GlStateSnapshot glStateSnapshot4 = FramebufferUtils.captureGlState();

         try {
            this.intValue20 = GL30.glGenVertexArrays();
            this.intValue21 = GL15.glGenBuffers();
            GL30.glBindVertexArray(this.intValue20);
            GL15.glBindBuffer(34962, this.intValue21);
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
            FramebufferUtils.restoreGlState(glStateSnapshot4);
         }
      }

      if (this.glShaderProgram == null) {
         try {
            this.glShaderProgram = GlShaderProgram.resolve("assets/enoughfx/shaders/world/hit_refraction.vsh", "assets/enoughfx/shaders/world/hit_refraction.frag");
            this.intValue = this.glShaderProgram.compute2("u_scene");
            this.intValue2 = this.glShaderProgram.compute2("u_depth");
            this.intValue3 = this.glShaderProgram.compute2("u_proj");
            this.intValue4 = this.glShaderProgram.compute2("u_invProj");
            this.intValue5 = this.glShaderProgram.compute2("u_resolution");
            this.intValue6 = this.glShaderProgram.compute2("u_time");
            this.intValue7 = this.glShaderProgram.compute2("u_count");
            this.intValue8 = this.glShaderProgram.compute2("u_chroma");
            this.intValue9 = this.glShaderProgram.compute2("u_depthOcclusion");
            this.intValue10 = this.glShaderProgram.compute2("u_center[0]");
            this.intValue11 = this.glShaderProgram.compute2("u_axis[0]");
            this.intValue12 = this.glShaderProgram.compute2("u_shape[0]");
            this.intValue13 = this.glShaderProgram.compute2("u_glow[0]");
         } catch (Throwable exception) {
            this.glShaderProgram = null;
         }
      }
   }

   private void invoke3() {
      if (this.intValue14 != 0) {
         GL30.glDeleteFramebuffers(this.intValue14);
         this.intValue14 = 0;
      }

      if (this.intValue15 != 0) {
         GL11.glDeleteTextures(this.intValue15);
         this.intValue15 = 0;
      }

      this.intValue16 = 0;
      this.intValue17 = 0;
   }

   public void invoke4() {
      if (RenderSystem.isOnRenderThread()) {
         this.invoke3();
         if (this.intValue18 != 0) {
            GL30.glDeleteFramebuffers(this.intValue18);
            this.intValue18 = 0;
         }

         if (this.intValue19 != 0) {
            GL30.glDeleteFramebuffers(this.intValue19);
            this.intValue19 = 0;
         }

         if (this.intValue21 != 0) {
            GL15.glDeleteBuffers(this.intValue21);
            this.intValue21 = 0;
         }

         if (this.intValue20 != 0) {
            GL30.glDeleteVertexArrays(this.intValue20);
            this.intValue20 = 0;
         }

         if (this.glShaderProgram != null) {
            this.glShaderProgram.invoke2();
            this.glShaderProgram = null;
         }
      }
   }

   private static float measure(float f, float g, float h) {
      if (f == g) {
         return h < f ? 0.0F : 1.0F;
      } else {
         float floatValue13 = measure2((h - f) / (g - f), 0.0F, 1.0F);
         return floatValue13 * floatValue13 * (3.0F - 2.0F * floatValue13);
      }
   }

   private static float measure2(float f, float g, float h) {
      return Math.max(g, Math.min(h, f));
   }
}
