package fun.enoughvisuals.port;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public final class MotionBlurRenderer implements AutoCloseable {
   private static final MotionBlurRenderer INSTANCE = new MotionBlurRenderer();
   private static final String ASSETS_PORTED_SHADERS_WORLD_WORLD_VOLUME_VERT = "assets/enoughfx/shaders/world/world_volume.vert";
   private static final String ASSETS_PORTED_SHADERS_POSTFX_MOTION_BLUR_FRAG = "assets/enoughfx/shaders/postfx/motion_blur.frag";
   private static final float FLOAT_VALUE = 1.0E-5F;
   private final MotionBlurRenderer.MotionBlurRendererState4 motionBlurRendererState4 = new MotionBlurRenderer.MotionBlurRendererState4();
   private final Matrix4f matrix4f = new Matrix4f();
   private final Matrix4f matrix4f2 = new Matrix4f();
   private Vec3d vec3d = Vec3d.ZERO;
   private float floatValue;
   private float floatValue2;
   private MotionBlurRenderer.MotionBlurRendererState3 motionBlurRendererState3;
   private int intValue;
   private int intValue2;
   private int intValue3;
   private int intValue4;
   private boolean flag;
   private boolean flag2;
   private boolean flag3;

   private MotionBlurRenderer() {
   }

   public static MotionBlurRenderer getINSTANCE() {
      return INSTANCE;
   }

   public void invoke(MinecraftClient minecraftClient, Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, MotionBlurRenderer.MotionBlurRendererState2 motionBlurRendererState2) {
      if (!this.flag2 && minecraftClient != null && camera != null && matrix4f != null && matrix4f2 != null && motionBlurRendererState2 != null) {
         if (minecraftClient.world != null && minecraftClient.player != null && check4(minecraftClient)) {
            Window window = minecraftClient.getWindow();
            int intValue = window.getFramebufferWidth();
            int intValue2 = window.getFramebufferHeight();
            if (intValue > 1 && intValue2 > 1 && !(motionBlurRendererState2.floatValue <= 1.0E-5F)) {
               Framebuffer framebuffer = minecraftClient.getFramebuffer();
               if (framebuffer == null) {
                  this.invoke3();
               } else {
                  int intValue3 = compute(framebuffer.getColorAttachment());
                  int intValue4 = compute(framebuffer.getDepthAttachment());
                  if (intValue3 > 0 && intValue4 > 0) {
                     float floatValue = this.measure(camera);
                     float floatValue2 = this.flag3 ? measure3(measure2(camera.getYaw() - this.floatValue) * 0.0062F, -0.24F, 0.24F) : 0.0F;
                     float floatValue3 = this.flag3 ? measure3((camera.getPitch() - this.floatValue2) * -0.0074F, -0.24F, 0.24F) : 0.0F;
                     if (this.flag3 && !(floatValue < motionBlurRendererState2.floatValue8 * 4.0E-5F)) {
                        FramebufferUtils.GlStateSnapshot glStateSnapshot = FramebufferUtils.captureGlState();
                        boolean flag = false;
                        boolean flag2 = false ;

                        label228: {
                           label216: {
                              label229: {
                                 try {
                                    flag2 = true;
                                    this.invoke5();
                                    if (!this.flag2 && this.check3(this.motionBlurRendererState4, intValue, intValue2)) {
                                       if (!this.check2(intValue3, intValue, intValue2, this.motionBlurRendererState4)) {
                                          this.invoke6(camera, matrix4f, matrix4f2);
                                          flag2 = false;
                                          break label228;
                                       }

                                       Matrix4f matrix4f3 = new Matrix4f(matrix4f2).invert();
                                       Matrix4f matrix4f4 = new Matrix4f(matrix4f).invert();
                                       Vec3d vec3d2 = camera.getPos();
                                       matrix4f4.m30((float)vec3d2.x);
                                       matrix4f4.m31((float)vec3d2.y);
                                       matrix4f4.m32((float)vec3d2.z);
                                       MotionBlurRenderer.MotionBlurRendererState motionBlurRendererState = new MotionBlurRenderer.MotionBlurRendererState(
                                          intValue, intValue2, this.motionBlurRendererState4.intValue2, intValue4, vec3d2, matrix4f3, matrix4f4, motionBlurRendererState2, floatValue, floatValue2, floatValue3
                                       );
                                       flag = this.check(intValue3, motionBlurRendererState);
                                       this.invoke6(camera, matrix4f, matrix4f2);
                                       flag2 = false;
                                       break label216;
                                    }

                                    this.invoke6(camera, matrix4f, matrix4f2);
                                    flag2 = false;
                                    break label229;
                                 } catch (Throwable exception) {
                                    this.flag2 = true;
                                    System.err.println("[SilkFlow] renderer disabled: " + exception.getMessage());
                                    exception.printStackTrace();
                                    flag2 = false;
                                 } finally {
                                    if (flag2) {
                                       if (flag && this.intValue2 != 0) {
                                          GL30.glBindFramebuffer(36160, this.intValue2);
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

                                 if (flag && this.intValue2 != 0) {
                                    GL30.glBindFramebuffer(36160, this.intValue2);
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

                              if (flag && this.intValue2 != 0) {
                                 GL30.glBindFramebuffer(36160, this.intValue2);
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

                           if (flag && this.intValue2 != 0) {
                              GL30.glBindFramebuffer(36160, this.intValue2);
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

                        if (flag && this.intValue2 != 0) {
                           GL30.glBindFramebuffer(36160, this.intValue2);
                           GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                        }

                        GL13.glActiveTexture(33985);
                        GL11.glBindTexture(3553, 0);
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(3553, 0);
                        GL20.glUseProgram(0);
                        FramebufferUtils.restoreGlState(glStateSnapshot);
                     } else {
                        this.invoke6(camera, matrix4f, matrix4f2);
                     }
                  } else {
                     this.invoke6(camera, matrix4f, matrix4f2);
                  }
               }
            } else {
               this.invoke6(camera, matrix4f, matrix4f2);
            }
         } else {
            this.invoke3();
         }
      }
   }

   public void invoke2(int i, int j) {
      this.invoke3();
      if (i > 0 && j > 0) {
         if (this.motionBlurRendererState4.intValue3 > 0 && (this.motionBlurRendererState4.intValue3 != i || this.motionBlurRendererState4.intValue4 != j)) {
            this.invoke12(this.motionBlurRendererState4);
         }
      } else {
         this.invoke12(this.motionBlurRendererState4);
      }
   }

   public void invoke3() {
      this.flag3 = false;
      this.vec3d = Vec3d.ZERO;
      this.floatValue = 0.0F;
      this.floatValue2 = 0.0F;
      this.matrix4f.identity();
      this.matrix4f2.identity();
   }

   private boolean check(int i, MotionBlurRenderer.MotionBlurRendererState motionBlurRendererState3) {
      if (this.intValue2 == 0) {
         this.intValue2 = GL30.glGenFramebuffers();
      }

      GL30.glBindFramebuffer(36160, this.intValue2);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, i, 0);
      GL11.glDrawBuffer(36064);
      if (GL30.glCheckFramebufferStatus(36160) != 36053) {
         return true;
      } else {
         GL11.glViewport(0, 0, motionBlurRendererState3.intValue, motionBlurRendererState3.intValue2);
         GL11.glDisable(3089);
         GL11.glDisable(2929);
         GL11.glDisable(2884);
         GL11.glDisable(3042);
         GL11.glDisable(36281);
         GL11.glColorMask(true, true, true, true);
         GL11.glDepthMask(false);
         this.motionBlurRendererState3.glShaderProgram.invoke();
         this.invoke4(motionBlurRendererState3);
         GL30.glBindVertexArray(this.intValue3);
         GlStateGuard.getINSTANCE().invoke2(2);
         GL11.glDrawArrays(4, 0, 6);
         GL30.glBindVertexArray(0);
         return true;
      }
   }

   private void invoke4(MotionBlurRenderer.MotionBlurRendererState motionBlurRendererState4) {
      invoke7(this.motionBlurRendererState3.intValue, 0);
      invoke7(this.motionBlurRendererState3.intValue2, 1);
      invoke9(this.motionBlurRendererState3.intValue3, (float)motionBlurRendererState4.intValue, (float)motionBlurRendererState4.intValue2);
      invoke11(this.motionBlurRendererState3.intValue4, motionBlurRendererState4.matrix4f);
      invoke11(this.motionBlurRendererState3.intValue5, motionBlurRendererState4.matrix4f2);
      invoke11(this.motionBlurRendererState3.intValue6, this.matrix4f2);
      invoke11(this.motionBlurRendererState3.intValue7, this.matrix4f);
      invoke10(this.motionBlurRendererState3.intValue8, (float)motionBlurRendererState4.vec3d.x, (float)motionBlurRendererState4.vec3d.y, (float)motionBlurRendererState4.vec3d.z);
      invoke10(this.motionBlurRendererState3.intValue9, (float)this.vec3d.x, (float)this.vec3d.y, (float)this.vec3d.z);
      invoke8(this.motionBlurRendererState3.intValue10, measure3(motionBlurRendererState4.motionBlurRendererState2.floatValue, 0.0F, 1.0F));
      invoke8(this.motionBlurRendererState3.intValue11, measure3(motionBlurRendererState4.motionBlurRendererState2.floatValue2, 0.05F, 4.0F));
      invoke8(this.motionBlurRendererState3.intValue12, measure3(motionBlurRendererState4.motionBlurRendererState2.floatValue3, 1.0F, 128.0F));
      invoke8(this.motionBlurRendererState3.intValue13, measure3(motionBlurRendererState4.motionBlurRendererState2.floatValue4, 0.02F, 4.0F));
      invoke8(this.motionBlurRendererState3.intValue14, measure3(motionBlurRendererState4.motionBlurRendererState2.floatValue5, 0.0F, 3.0F));
      invoke8(this.motionBlurRendererState3.intValue15, measure3(motionBlurRendererState4.motionBlurRendererState2.floatValue6, 0.0F, 1.0F));
      invoke8(this.motionBlurRendererState3.intValue16, measure3(motionBlurRendererState4.motionBlurRendererState2.floatValue7, 0.2F, 8.0F));
      invoke8(this.motionBlurRendererState3.intValue17, measure3(motionBlurRendererState4.motionBlurRendererState2.floatValue8, 0.01F, 4.0F));
      invoke8(this.motionBlurRendererState3.intValue18, motionBlurRendererState4.floatValue);
      invoke9(this.motionBlurRendererState3.intValue19, motionBlurRendererState4.floatValue2, motionBlurRendererState4.floatValue3);
      invoke7(this.motionBlurRendererState3.intValue20, Math.max(3, Math.min(12, motionBlurRendererState4.motionBlurRendererState2.intValue)));
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, motionBlurRendererState4.intValue3);
      GL13.glActiveTexture(33985);
      GL11.glBindTexture(3553, motionBlurRendererState4.intValue4);
      GL13.glActiveTexture(33984);
   }

   private boolean check2(int i, int j, int k, MotionBlurRenderer.MotionBlurRendererState4 motionBlurRendererState42) {
      if (i > 0 && motionBlurRendererState42 != null && motionBlurRendererState42.intValue > 0 && j > 0 && k > 0) {
         if (this.intValue == 0) {
            this.intValue = GL30.glGenFramebuffers();
         }

         GL30.glBindFramebuffer(36008, this.intValue);
         GL30.glFramebufferTexture2D(36008, 36064, 3553, i, 0);
         if (GL30.glCheckFramebufferStatus(36008) != 36053) {
            GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
            return false;
         } else {
            GL30.glBindFramebuffer(36009, motionBlurRendererState42.intValue);
            GL11.glReadBuffer(36064);
            GL11.glDrawBuffer(36064);
            GL30.glBlitFramebuffer(0, 0, j, k, 0, 0, j, k, 16384, 9728);
            GL30.glBindFramebuffer(36008, this.intValue);
            GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean check3(MotionBlurRenderer.MotionBlurRendererState4 motionBlurRendererState43, int i, int j) {
      if (motionBlurRendererState43 != null && i > 0 && j > 0) {
         if (motionBlurRendererState43.intValue2 != 0 && (motionBlurRendererState43.intValue3 != i || motionBlurRendererState43.intValue4 != j || motionBlurRendererState43.intValue == 0)) {
            this.invoke12(motionBlurRendererState43);
         }

         if (motionBlurRendererState43.intValue2 == 0) {
            motionBlurRendererState43.intValue2 = GL11.glGenTextures();
            GL11.glBindTexture(3553, motionBlurRendererState43.intValue2);
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            RenderCapabilities.invoke(32856, i, j, 6408, 5121);
            motionBlurRendererState43.intValue = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(36160, motionBlurRendererState43.intValue);
            GL30.glFramebufferTexture2D(36160, 36064, 3553, motionBlurRendererState43.intValue2, 0);
            GL11.glDrawBuffer(36064);
            if (GL30.glCheckFramebufferStatus(36160) != 36053) {
               this.invoke12(motionBlurRendererState43);
               return false;
            }
         }

         motionBlurRendererState43.intValue3 = i;
         motionBlurRendererState43.intValue4 = j;
         return true;
      } else {
         return false;
      }
   }

   private void invoke5() {
      if (!this.flag) {
         this.intValue3 = GL30.glGenVertexArrays();
         this.intValue4 = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.intValue3);
         GL15.glBindBuffer(34962, this.intValue4);
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
         this.motionBlurRendererState3 = new MotionBlurRenderer.MotionBlurRendererState3();
         this.flag = true;
      }
   }

   private float measure(Camera camera) {
      if (this.flag3 && camera != null) {
         Vec3d vec3d3 = camera.getPos();
         float floatValue4 = measure2(camera.getYaw() - this.floatValue);
         float floatValue5 = camera.getPitch() - this.floatValue2;
         double doubleValue = vec3d3.distanceTo(this.vec3d);
         float floatValue6 = Math.abs(floatValue4) * 0.00175F + Math.abs(floatValue5) * 0.00225F;
         float floatValue7 = (float)Math.min(0.12, doubleValue * 0.045);
         return floatValue6 + floatValue7;
      } else {
         return 1.0F;
      }
   }

   private void invoke6(Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2) {
      if (camera != null && matrix4f != null && matrix4f2 != null) {
         this.matrix4f.set(matrix4f);
         this.matrix4f2.set(matrix4f2);
         this.vec3d = camera.getPos();
         this.floatValue = camera.getYaw();
         this.floatValue2 = camera.getPitch();
         this.flag3 = true;
      } else {
         this.invoke3();
      }
   }

   private static int compute(Object object) {
      // Framebuffer attachments are raw OpenGL ids in Minecraft 1.21.4.
      return object instanceof Integer textureId ? textureId : 0;
   }

   private static boolean check4(MinecraftClient minecraftClient) {
      if (minecraftClient != null && minecraftClient.getWindow() != null) {
         Window window2 = minecraftClient.getWindow();
         return !window2.hasZeroWidthOrHeight() && window2.getFramebufferWidth() > 0 && window2.getFramebufferHeight() > 0;
      } else {
         return false;
      }
   }

   private static boolean check5() {
      return RenderSystem.isOnRenderThread() && GLFW.glfwGetCurrentContext() != 0L;
   }

   private static float measure2(float f) {
      f %= 360.0F;
      if (f >= 180.0F) {
         f -= 360.0F;
      }

      if (f < -180.0F) {
         f += 360.0F;
      }

      return f;
   }

   private static float measure3(float f, float g, float h) {
      return !Float.isFinite(f) ? g : Math.max(g, Math.min(h, f));
   }

   private static void invoke7(int i, int j) {
      if (i >= 0) {
         GL20.glUniform1i(i, j);
      }
   }

   private static void invoke8(int i, float f) {
      if (i >= 0) {
         GL20.glUniform1f(i, f);
      }
   }

   private static void invoke9(int i, float f, float g) {
      if (i >= 0) {
         GL20.glUniform2f(i, f, g);
      }
   }

   private static void invoke10(int i, float f, float g, float h) {
      if (i >= 0) {
         GL20.glUniform3f(i, f, g, h);
      }
   }

   private static void invoke11(int i, Matrix4f matrix4f) {
      if (i >= 0 && matrix4f != null) {
         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            FloatBuffer floatBuffer = memoryStack.mallocFloat(16);
            matrix4f.get(floatBuffer);
            GL20.glUniformMatrix4fv(i, false, floatBuffer);
         } catch (Throwable exception2) {
            if (memoryStack != null) {
               try {
                  memoryStack.close();
               } catch (Throwable exception3) {
                  exception2.addSuppressed(exception3);
               }
            }

            throw exception2;
         }

         if (memoryStack != null) {
            memoryStack.close();
         }
      }
   }

   private void invoke12(MotionBlurRenderer.MotionBlurRendererState4 motionBlurRendererState44) {
      if (motionBlurRendererState44 != null) {
         if (motionBlurRendererState44.intValue != 0 && check5()) {
            GL30.glDeleteFramebuffers(motionBlurRendererState44.intValue);
         }

         if (motionBlurRendererState44.intValue2 != 0 && check5()) {
            GL11.glDeleteTextures(motionBlurRendererState44.intValue2);
         }

         motionBlurRendererState44.intValue = 0;
         motionBlurRendererState44.intValue2 = 0;
         motionBlurRendererState44.intValue3 = 0;
         motionBlurRendererState44.intValue4 = 0;
      }
   }

   @Override
   public void close() {
      this.invoke3();
      if (!check5()) {
         this.invoke13();
      } else {
         this.invoke12(this.motionBlurRendererState4);
         if (this.intValue != 0) {
            GL30.glDeleteFramebuffers(this.intValue);
            this.intValue = 0;
         }

         if (this.intValue2 != 0) {
            GL30.glDeleteFramebuffers(this.intValue2);
            this.intValue2 = 0;
         }

         if (this.intValue3 != 0) {
            GL30.glDeleteVertexArrays(this.intValue3);
            this.intValue3 = 0;
         }

         if (this.intValue4 != 0) {
            GL15.glDeleteBuffers(this.intValue4);
            this.intValue4 = 0;
         }

         if (this.motionBlurRendererState3 != null) {
            this.motionBlurRendererState3.glShaderProgram.invoke2();
            this.motionBlurRendererState3 = null;
         }

         this.flag = false;
         this.flag2 = false;
      }
   }

   private void invoke13() {
      this.motionBlurRendererState4.intValue = 0;
      this.motionBlurRendererState4.intValue2 = 0;
      this.motionBlurRendererState4.intValue3 = 0;
      this.motionBlurRendererState4.intValue4 = 0;
      this.intValue = 0;
      this.intValue2 = 0;
      this.intValue3 = 0;
      this.intValue4 = 0;
      this.motionBlurRendererState3 = null;
      this.flag = false;
      this.flag2 = false;
   }

   static final class MotionBlurRendererState {
      final int intValue;
      final int intValue2;
      final int intValue3;
      final int intValue4;
      final Vec3d vec3d;
      final Matrix4f matrix4f;
      final Matrix4f matrix4f2;
      final MotionBlurRenderer.MotionBlurRendererState2 motionBlurRendererState2;
      final float floatValue;
      final float floatValue2;
      final float floatValue3;

      MotionBlurRendererState(int i, int j, int k, int l, Vec3d vec3d, Matrix4f matrix4f, Matrix4f matrix4f2, MotionBlurRenderer.MotionBlurRendererState2 motionBlurRendererState22, float f, float g, float h) {
         this.intValue = i;
         this.intValue2 = j;
         this.intValue3 = k;
         this.intValue4 = l;
         this.vec3d = vec3d;
         this.matrix4f = matrix4f;
         this.matrix4f2 = matrix4f2;
         this.motionBlurRendererState2 = motionBlurRendererState22;
         this.floatValue = f;
         this.floatValue2 = g;
         this.floatValue3 = h;
      }
   }

   public static final class MotionBlurRendererState2 {
      public float floatValue = 0.72F;
      public float floatValue2 = 1.05F;
      public int intValue = 7;
      public float floatValue3 = 34.0F;
      public float floatValue4 = 0.54F;
      public float floatValue5 = 0.58F;
      public float floatValue6 = 0.72F;
      public float floatValue7 = 2.25F;
      public float floatValue8 = 0.42F;
   }

   static final class MotionBlurRendererState3 {
      final GlShaderProgram glShaderProgram = GlShaderProgram.resolve("assets/enoughfx/shaders/world/world_volume.vert", "assets/enoughfx/shaders/postfx/motion_blur.frag");
      final int intValue = this.glShaderProgram.compute2("u_ScreenTexture");
      final int intValue2 = this.glShaderProgram.compute2("u_DepthTexture");
      final int intValue3 = this.glShaderProgram.compute2("u_Resolution");
      final int intValue4 = this.glShaderProgram.compute2("u_InverseProjectionMatrix");
      final int intValue5 = this.glShaderProgram.compute2("u_InverseViewMatrix");
      final int intValue6 = this.glShaderProgram.compute2("u_PreviousProjectionMatrix");
      final int intValue7 = this.glShaderProgram.compute2("u_PreviousViewMatrix");
      final int intValue8 = this.glShaderProgram.compute2("u_CameraPos");
      final int intValue9 = this.glShaderProgram.compute2("u_PreviousCameraPos");
      final int intValue10 = this.glShaderProgram.compute2("u_Strength");
      final int intValue11 = this.glShaderProgram.compute2("u_TemporalScale");
      final int intValue12 = this.glShaderProgram.compute2("u_MaxRadius");
      final int intValue13 = this.glShaderProgram.compute2("u_EdgeFocus");
      final int intValue14 = this.glShaderProgram.compute2("u_ChromaticPhase");
      final int intValue15 = this.glShaderProgram.compute2("u_DepthGuard");
      final int intValue16 = this.glShaderProgram.compute2("u_Decay");
      final int intValue17 = this.glShaderProgram.compute2("u_Activation");
      final int intValue18 = this.glShaderProgram.compute2("u_GlobalMotion");
      final int intValue19 = this.glShaderProgram.compute2("u_CameraVelocity");
      final int intValue20 = this.glShaderProgram.compute2("u_Samples");
   }

   static final class MotionBlurRendererState4 {
      int intValue;
      int intValue2;
      int intValue3;
      int intValue4;
   }
}
