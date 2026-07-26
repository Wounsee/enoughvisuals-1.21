package fun.enoughvisuals.port;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public final class GlowEspRenderer implements AutoCloseable {
   private static final Logger LOGGER = LogManager.getLogger("GlowESP");
   private static final String ASSETS_PORTED_SHADERS_GLOWESP_FULLSCREEN_VERT = "assets/enoughfx/shaders/glowesp/fullscreen.vert";
   private static final String ASSETS_PORTED_SHADERS_GLOWESP_MASK_FRAG = "assets/enoughfx/shaders/glowesp/mask.frag";
   private static final String ASSETS_PORTED_SHADERS_GLOWESP_SHADOW_FRAG = "assets/enoughfx/shaders/glowesp/shadow.frag";
   private static final String ASSETS_PORTED_SHADERS_GLOWESP_GRADIENT_FRAG = "assets/enoughfx/shaders/glowesp/gradient.frag";
   private static final String ASSETS_PORTED_SHADERS_GLOWESP_DOMINANT_COLOR_FRAG = "assets/enoughfx/shaders/glowesp/dominant_color.frag";
   private static final String ASSETS_PORTED_SHADERS_BLUR_BLUR_DOWNSAMPLE_FRAG = "assets/enoughfx/shaders/blur/blur_downsample.frag";
   private static final int INT_VALUE = 63;
   private static final int INT_VALUE_2 = 2;
   private static final float FLOAT_VALUE = 8.0F;
   private final GlowEspRenderer.GlowEspRendererState glowEspRendererState = new GlowEspRenderer.GlowEspRendererState();
   private final GlowEspRenderer.GlowEspRendererState glowEspRendererState2 = new GlowEspRenderer.GlowEspRendererState();
   private final GlowEspRenderer.GlowEspRendererState glowEspRendererState3 = new GlowEspRenderer.GlowEspRendererState();
   private final GlowEspRenderer.GlowEspRendererState glowEspRendererState4 = new GlowEspRenderer.GlowEspRendererState();
   private final GlowEspRenderer.GlowEspRendererState glowEspRendererState5 = new GlowEspRenderer.GlowEspRendererState();
   private GlShaderProgram glShaderProgram;
   private GlShaderProgram glShaderProgram2;
   private GlShaderProgram glShaderProgram3;
   private GlShaderProgram glShaderProgram4;
   private GlShaderProgram glShaderProgram5;
   private int intValue;
   private int intValue2;
   private boolean flag;
   private boolean flag2;
   private String notRun = "not-run";
   private int intValue3;
   private int intValue4;
   private int intValue5 = -1;
   private final float[] floats = new float[64];
   public static final int INT_VALUE_3 = 0;
   public static final int INT_VALUE_4 = 1;
   public static final int INT_VALUE_5 = 2;

   public boolean check(int i, int j, int k, int l, GlowEspRenderer.GlowEspRendererData glowEspRendererData) {
      return this.check5(i, j, i, k, l, glowEspRendererData, null);
   }

   public boolean check2(int i, int j, int k, int l, GlowEspRenderer.GlowEspRendererData glowEspRendererData2, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds) {
      return this.check5(i, j, i, k, l, glowEspRendererData2, glowEspRendererBounds);
   }

   public boolean check3(int i, int j, int k, int l, GlowEspRenderer.GlowEspRendererData glowEspRendererData3, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds2, int m, int n, int o) {
      return this.check6(i, j, i, k, l, glowEspRendererData3, glowEspRendererBounds2, m, n, o);
   }

   public boolean check4(int i, int j, int k, int l, int m, GlowEspRenderer.GlowEspRendererData glowEspRendererData4) {
      return this.check5(i, j, k, l, m, glowEspRendererData4, null);
   }

   public boolean check5(int i, int j, int k, int l, int m, GlowEspRenderer.GlowEspRendererData glowEspRendererData5, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds3) {
      return this.check6(i, j, k, l, m, glowEspRendererData5, glowEspRendererBounds3, 0, 0, 0);
   }

   public boolean check6(int i, int j, int k, int l, int m, GlowEspRenderer.GlowEspRendererData glowEspRendererData6, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds4, int n, int o, int p) {
      if (this.flag2) {
         this.notRun = "renderer-broken";
         return false;
      } else if (i <= 0 || l <= 0 || m <= 0 || glowEspRendererData6 == null) {
         this.notRun = "invalid-input";
         return false;
      } else if (!check10()) {
         this.notRun = "no-render-context";
         return false;
      } else {
         try {
            boolean flag = this.check7(i, j, k, l, m, glowEspRendererData6, glowEspRendererBounds4, n, o, p);
            this.notRun = flag ? "rendered" : this.notRun;
            return flag;
         } catch (Throwable exception) {
            this.flag2 = true;
            this.notRun = "exception:" + exception.getClass().getSimpleName() + ":" + exception.getMessage();
            LOGGER.warn("GlowESP renderer disabled", exception);
            return false;
         }
      }
   }

   private boolean check7(int i, int j, int k, int l, int m, GlowEspRenderer.GlowEspRendererData glowEspRendererData7, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds5, int n, int o, int p) {
      FramebufferUtils.GlStateSnapshot glStateSnapshot = FramebufferUtils.captureGlState();
      boolean flag2 = false ;

      int intValue;
      label90: {
         int intValue2;
         label89: {
            boolean flag3;
            label88: {
               try {
                  flag2 = true;
                  this.invoke();
                  GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds6 = resolve2(glowEspRendererBounds5, l, m);
                  if (!this.check9(this.glowEspRendererState, l, m)) {
                     this.notRun = "mask-target-incomplete";
                     intValue = 0;
                     flag2 = false;
                     break label90;
                  }

                  if (glowEspRendererBounds6 != null) {
                     this.invoke4(this.glowEspRendererState, glowEspRendererBounds6);
                  }

                  intValue = this.compute(i, j, l, m, glowEspRendererBounds6, n, o, p);
                  int intValue3 = 0;
                  if (glowEspRendererData7.autoColor != 0) {
                     if (!this.check9(this.glowEspRendererState5, 1, 1)) {
                        this.notRun = "dominant-color-target-incomplete";
                        intValue2 = 0;
                        flag2 = false;
                        break label89;
                     }

                     intValue3 = this.compute4(k > 0 ? k : i);
                  }

                  intValue2 = intValue;
                  if (glowEspRendererData7.glowStrength > 0.001F || glowEspRendererData7.debugView == 2) {
                     intValue2 = this.compute2(intValue, l, m, glowEspRendererData7.radius, glowEspRendererBounds6);
                     if (intValue2 == 0) {
                        this.notRun = "blur-target-incomplete";
                        flag3 = false;
                        flag2 = false;
                        break label88;
                     }
                  }

                  flag3 = this.check8(intValue, intValue2, intValue3, l, m, glowEspRendererData7, glStateSnapshot, glowEspRendererBounds6);
                  flag2 = false;
               } finally {
                  if (flag2) {
                     GL20.glUseProgram(0);
                     GL30.glBindVertexArray(0);
                     GL13.glActiveTexture(33987);
                     GL11.glBindTexture(3553, 0);
                     GL13.glActiveTexture(33986);
                     GL11.glBindTexture(3553, 0);
                     GL13.glActiveTexture(33985);
                     GL11.glBindTexture(3553, 0);
                     GL13.glActiveTexture(33984);
                     GL11.glBindTexture(3553, 0);
                     FramebufferUtils.restoreGlState(glStateSnapshot);
                  }
               }

               GL20.glUseProgram(0);
               GL30.glBindVertexArray(0);
               GL13.glActiveTexture(33987);
               GL11.glBindTexture(3553, 0);
               GL13.glActiveTexture(33986);
               GL11.glBindTexture(3553, 0);
               GL13.glActiveTexture(33985);
               GL11.glBindTexture(3553, 0);
               GL13.glActiveTexture(33984);
               GL11.glBindTexture(3553, 0);
               FramebufferUtils.restoreGlState(glStateSnapshot);
               return flag3;
            }

            GL20.glUseProgram(0);
            GL30.glBindVertexArray(0);
            GL13.glActiveTexture(33987);
            GL11.glBindTexture(3553, 0);
            GL13.glActiveTexture(33986);
            GL11.glBindTexture(3553, 0);
            GL13.glActiveTexture(33985);
            GL11.glBindTexture(3553, 0);
            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, 0);
            FramebufferUtils.restoreGlState(glStateSnapshot);
            return flag3;
         }

         GL20.glUseProgram(0);
         GL30.glBindVertexArray(0);
         GL13.glActiveTexture(33987);
         GL11.glBindTexture(3553, 0);
         GL13.glActiveTexture(33986);
         GL11.glBindTexture(3553, 0);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, 0);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, 0);
         FramebufferUtils.restoreGlState(glStateSnapshot);
         return intValue2 != 0;
      }

      GL20.glUseProgram(0);
      GL30.glBindVertexArray(0);
      GL13.glActiveTexture(33987);
      GL11.glBindTexture(3553, 0);
      GL13.glActiveTexture(33986);
      GL11.glBindTexture(3553, 0);
      GL13.glActiveTexture(33985);
      GL11.glBindTexture(3553, 0);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, 0);
      FramebufferUtils.restoreGlState(glStateSnapshot);
      return intValue != 0;
   }

   private int compute(int i, int j, int k, int l, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds7, int m, int n, int o) {
      boolean flag4 = o != 0 && m > 0;
      this.invoke6();
      this.glShaderProgram.invoke();
      invoke10(this.glShaderProgram, "uSource", 0);
      invoke10(this.glShaderProgram, "uDepthSource", 1);
      invoke10(this.glShaderProgram, "uTagged", 2);
      invoke10(this.glShaderProgram, "uTaggedDepth", 3);
      invoke10(this.glShaderProgram, "uHasDepth", j > 0 ? 1 : 0);
      invoke10(this.glShaderProgram, "uTagMode", flag4 ? o : 0);
      invoke11(this.glShaderProgram, "uThreshold", 0.05F);
      this.invoke3(this.glowEspRendererState, glowEspRendererBounds7);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, i);
      GL13.glActiveTexture(33985);
      GL11.glBindTexture(3553, j > 0 ? j : i);
      GL13.glActiveTexture(33986);
      GL11.glBindTexture(3553, flag4 ? m : i);
      GL13.glActiveTexture(33987);
      GL11.glBindTexture(3553, flag4 && n > 0 ? n : i);
      this.invoke9();
      return this.glowEspRendererState.intValue2;
   }

   private int compute2(int i, int j, int k, float f, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds8) {
      int intValue4 = compute5(f, j, k);
      int intValue5 = intValue4 > 1 ? Math.max(1, j / intValue4) : j;
      int intValue6 = intValue4 > 1 ? Math.max(1, k / intValue4) : k;
      int intValue7 = i;
      GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds9 = resolve3(glowEspRendererBounds8, intValue5, intValue6, j, k);
      if (intValue4 > 1) {
         if (!this.check9(this.glowEspRendererState2, intValue5, intValue6)) {
            return 0;
         }

         if (glowEspRendererBounds8 != null) {
            this.invoke4(this.glowEspRendererState2, glowEspRendererBounds9);
         }

         intValue7 = this.compute3(i, j, k, glowEspRendererBounds9);
      }

      if (this.check9(this.glowEspRendererState3, intValue5, intValue6) && this.check9(this.glowEspRendererState4, intValue5, intValue6)) {
         if (glowEspRendererBounds8 != null) {
            this.invoke4(this.glowEspRendererState3, glowEspRendererBounds9);
            this.invoke4(this.glowEspRendererState4, glowEspRendererBounds9);
         }

         int intValue8 = Math.max(1, Math.min(63, Math.round(f / intValue4)));
         float[] floatValues = this.resolve(intValue8);
         this.invoke6();
         this.glShaderProgram2.invoke();
         invoke10(this.glShaderProgram2, "uSource", 0);
         invoke12(this.glShaderProgram2, "uTexelSize", 1.0F / intValue5, 1.0F / intValue6);
         invoke10(this.glShaderProgram2, "uRadius", intValue8);
         int intValue9 = this.glShaderProgram2.compute2("uKernel[0]");
         if (intValue9 >= 0) {
            GL20.glUniform1fv(intValue9, floatValues);
         }

         this.invoke3(this.glowEspRendererState3, glowEspRendererBounds9);
         invoke12(this.glShaderProgram2, "uDirection", 1.0F, 0.0F);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, intValue7);
         this.invoke9();
         this.invoke3(this.glowEspRendererState4, glowEspRendererBounds9);
         invoke12(this.glShaderProgram2, "uDirection", 0.0F, 1.0F);
         GL11.glBindTexture(3553, this.glowEspRendererState3.intValue2);
         this.invoke9();
         return this.glowEspRendererState4.intValue2;
      } else {
         return 0;
      }
   }

   private int compute3(int i, int j, int k, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds10) {
      this.invoke6();
      this.glShaderProgram5.invoke();
      invoke10(this.glShaderProgram5, "uSource", 0);
      invoke12(this.glShaderProgram5, "uTexelSize", 1.0F / j, 1.0F / k);
      invoke11(this.glShaderProgram5, "uOffset", 1.0F);
      this.invoke3(this.glowEspRendererState2, glowEspRendererBounds10);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, i);
      this.invoke9();
      return this.glowEspRendererState2.intValue2;
   }

   private int compute4(int i) {
      this.invoke6();
      this.glShaderProgram4.invoke();
      invoke10(this.glShaderProgram4, "uSource", 0);
      this.invoke2(this.glowEspRendererState5);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, i);
      this.invoke9();
      return this.glowEspRendererState5.intValue2;
   }

   private boolean check8(int i, int j, int k, int l, int m, GlowEspRenderer.GlowEspRendererData glowEspRendererData8, FramebufferUtils.GlStateSnapshot glStateSnapshot2, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds11) {
      GL30.glBindFramebuffer(36009, glStateSnapshot2.intValue);
      this.intValue3 = glStateSnapshot2.intValue;
      this.intValue4 = GL30.glCheckFramebufferStatus(36009);
      if (this.intValue4 != 36053) {
         this.notRun = "output-framebuffer-incomplete";
         return false;
      } else {
         GL11.glDrawBuffer(glStateSnapshot2.intValue3);
         GL11.glViewport(glStateSnapshot2.ints[0], glStateSnapshot2.ints[1], glStateSnapshot2.ints[2], glStateSnapshot2.ints[3]);
         invoke8(glowEspRendererBounds11, l, m, glStateSnapshot2);
         GL11.glDisable(2929);
         GL11.glDisable(2884);
         GL11.glDisable(36281);
         GL11.glEnable(3042);
         GL14.glBlendFuncSeparate(770, 771, 1, 771);
         GL11.glColorMask(true, true, true, true);
         GL11.glDepthMask(false);
         this.glShaderProgram3.invoke();
         invoke10(this.glShaderProgram3, "uMask", 0);
         invoke10(this.glShaderProgram3, "uBlur", 1);
         invoke10(this.glShaderProgram3, "uAutoColor", 2);
         invoke10(this.glShaderProgram3, "uAutoColorEnabled", glowEspRendererData8.autoColor);
         invoke12(this.glShaderProgram3, "uTexelSize", 1.0F / l, 1.0F / m);
         invoke11(this.glShaderProgram3, "uOutlineWidth", glowEspRendererData8.outlineWidth);
         invoke11(this.glShaderProgram3, "uGlowStrength", glowEspRendererData8.glowStrength);
         invoke11(this.glShaderProgram3, "uOutlineStrength", glowEspRendererData8.outlineStrength);
         invoke11(this.glShaderProgram3, "uOpacity", glowEspRendererData8.opacity);
         invoke10(this.glShaderProgram3, "uDebugView", glowEspRendererData8.debugView);
         invoke10(this.glShaderProgram3, "uColorStyle", glowEspRendererData8.colorStyle);
         invoke11(this.glShaderProgram3, "uTime", (float)(System.nanoTime() % 30000000000L) / 1.0E9F);
         invoke13(this.glShaderProgram3, "uColorTop", glowEspRendererData8.topR, glowEspRendererData8.topG, glowEspRendererData8.topB, 1.0F);
         invoke13(this.glShaderProgram3, "uColorBottom", glowEspRendererData8.bottomR, glowEspRendererData8.bottomG, glowEspRendererData8.bottomB, 1.0F);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, i);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, j);
         GL13.glActiveTexture(33986);
         GL11.glBindTexture(3553, k);
         GL30.glBindVertexArray(this.intValue);
         this.invoke9();
         return true;
      }
   }

   public String getNotRun() {
      return this.notRun;
   }

   public int getIntValue3() {
      return this.intValue3;
   }

   public int getIntValue4() {
      return this.intValue4;
   }

   private void invoke() {
      if (!this.flag) {
         this.glShaderProgram = GlShaderProgram.resolve("assets/enoughfx/shaders/glowesp/fullscreen.vert", "assets/enoughfx/shaders/glowesp/mask.frag");
         this.glShaderProgram2 = GlShaderProgram.resolve("assets/enoughfx/shaders/glowesp/fullscreen.vert", "assets/enoughfx/shaders/glowesp/shadow.frag");
         this.glShaderProgram3 = GlShaderProgram.resolve("assets/enoughfx/shaders/glowesp/fullscreen.vert", "assets/enoughfx/shaders/glowesp/gradient.frag");
         this.glShaderProgram4 = GlShaderProgram.resolve("assets/enoughfx/shaders/glowesp/fullscreen.vert", "assets/enoughfx/shaders/glowesp/dominant_color.frag");
         this.glShaderProgram5 = GlShaderProgram.resolve("assets/enoughfx/shaders/glowesp/fullscreen.vert", "assets/enoughfx/shaders/blur/blur_downsample.frag");
         this.intValue = GL30.glGenVertexArrays();
         this.intValue2 = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.intValue);
         GL15.glBindBuffer(34962, this.intValue2);
         float[] floatValues2 = new float[]{-1.0F, -1.0F, 0.0F, 0.0F, 1.0F, -1.0F, 1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};
         GL15.glBufferData(34962, floatValues2, 35044);
         byte byteValue = 16;
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, byteValue, 0L);
         GL20.glEnableVertexAttribArray(1);
         GL20.glVertexAttribPointer(1, 2, 5126, false, byteValue, 8L);
         this.flag = true;
      }
   }

   private boolean check9(GlowEspRenderer.GlowEspRendererState glowEspRendererState, int i, int j) {
      if (glowEspRendererState.intValue2 != 0 && (glowEspRendererState.intValue3 != i || glowEspRendererState.intValue4 != j || glowEspRendererState.intValue == 0)) {
         this.invoke14(glowEspRendererState);
      }

      if (glowEspRendererState.intValue2 == 0) {
         glowEspRendererState.intValue2 = GL11.glGenTextures();
         GL11.glBindTexture(3553, glowEspRendererState.intValue2);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         RenderCapabilities.invoke(32856, i, j, 6408, 5121);
         glowEspRendererState.intValue = GL30.glGenFramebuffers();
         GL30.glBindFramebuffer(36160, glowEspRendererState.intValue);
         GL30.glFramebufferTexture2D(36160, 36064, 3553, glowEspRendererState.intValue2, 0);
         GL11.glDrawBuffer(36064);
         if (GL30.glCheckFramebufferStatus(36160) != 36053) {
            this.invoke14(glowEspRendererState);
            return false;
         }

         glowEspRendererState.flag = true;
         glowEspRendererState.glowEspRendererBounds = null;
      }

      glowEspRendererState.intValue3 = i;
      glowEspRendererState.intValue4 = j;
      return true;
   }

   private void invoke2(GlowEspRenderer.GlowEspRendererState glowEspRendererState2) {
      GL30.glBindFramebuffer(36160, glowEspRendererState2.intValue);
      GL11.glDrawBuffer(36064);
      GL11.glViewport(0, 0, glowEspRendererState2.intValue3, glowEspRendererState2.intValue4);
   }

   private void invoke3(GlowEspRenderer.GlowEspRendererState glowEspRendererState3, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds12) {
      this.invoke2(glowEspRendererState3);
      invoke7(glowEspRendererBounds12, glowEspRendererState3.intValue3, glowEspRendererState3.intValue4);
      if (glowEspRendererBounds12 == null) {
         glowEspRendererState3.flag = true;
         glowEspRendererState3.glowEspRendererBounds = null;
      }
   }

   private void invoke4(GlowEspRenderer.GlowEspRendererState glowEspRendererState4, GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds13) {
      this.invoke2(glowEspRendererState4);
      GL11.glColorMask(true, true, true, true);
      if (glowEspRendererBounds13 != null && !glowEspRendererState4.flag) {
         GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds14 = glowEspRendererState4.glowEspRendererBounds;
         if (glowEspRendererBounds14 != null) {
            invoke7(glowEspRendererBounds14, glowEspRendererState4.intValue3, glowEspRendererState4.intValue4);
            this.invoke5();
         }

         invoke7(glowEspRendererBounds13, glowEspRendererState4.intValue3, glowEspRendererState4.intValue4);
         this.invoke5();
         glowEspRendererState4.glowEspRendererBounds = glowEspRendererBounds13;
      } else {
         GL11.glDisable(3089);
         this.invoke5();
         glowEspRendererState4.flag = false;
         glowEspRendererState4.glowEspRendererBounds = glowEspRendererBounds13;
      }
   }

   private void invoke5() {
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         FloatBuffer floatBuffer = memoryStack.floats(0.0F, 0.0F, 0.0F, 0.0F);
         GL30.glClearBufferfv(6144, 0, floatBuffer);
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

   private void invoke6() {
      GL11.glDisable(3089);
      GL11.glDisable(2929);
      GL11.glDisable(2884);
      GL11.glDisable(3042);
      GL11.glDisable(36281);
      GL11.glColorMask(true, true, true, true);
      GL11.glDepthMask(false);
      GL30.glBindVertexArray(this.intValue);
   }

   private float[] resolve(int i) {
      if (this.intValue5 == i) {
         return this.floats;
      } else {
         for (int intValue10 = 0; intValue10 < this.floats.length; intValue10++) {
            this.floats[intValue10] = 0.0F;
         }

         float floatValue = Math.max(i * 0.5F, 0.5F);
         float floatValue2 = 2.0F * floatValue * floatValue;
         float floatValue3 = 0.0F;

         for (int intValue11 = 0; intValue11 <= i; intValue11++) {
            float floatValue4 = (float)Math.exp(-(intValue11 * intValue11) / floatValue2);
            this.floats[intValue11] = floatValue4;
            floatValue3 += intValue11 == 0 ? floatValue4 : floatValue4 * 2.0F;
         }

         float floatValue5 = floatValue3 > 0.0F ? 1.0F / floatValue3 : 1.0F;

         for (int intValue12 = 0; intValue12 <= i; intValue12++) {
            this.floats[intValue12] = this.floats[intValue12] * floatValue5;
         }

         this.intValue5 = i;
         return this.floats;
      }
   }

   private static int compute5(float f, int i, int j) {
      return !(f < 8.0F) && i >= 2 && j >= 2 ? 2 : 1;
   }

   private static GlowEspRenderer.GlowEspRendererBounds resolve2(GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds15, int i, int j) {
      if (glowEspRendererBounds15 != null && i > 0 && j > 0) {
         int intValue13 = Math.max(0, glowEspRendererBounds15.x);
         int intValue14 = Math.max(0, glowEspRendererBounds15.y);
         int intValue15 = Math.min(i, glowEspRendererBounds15.x + glowEspRendererBounds15.width);
         int intValue16 = Math.min(j, glowEspRendererBounds15.y + glowEspRendererBounds15.height);
         int intValue17 = intValue15 - intValue13;
         int intValue18 = intValue16 - intValue14;
         if (intValue17 > 0 && intValue18 > 0) {
            long longValue = (long)intValue17 * intValue18;
            long longValue2 = (long)i * j;
            return longValue >= longValue2 * 9L / 10L ? null : new GlowEspRenderer.GlowEspRendererBounds(intValue13, intValue14, intValue17, intValue18);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static GlowEspRenderer.GlowEspRendererBounds resolve3(GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds16, int i, int j, int k, int l) {
      if (glowEspRendererBounds16 != null && i > 0 && j > 0 && k > 0 && l > 0) {
         float floatValue6 = (float)i / k;
         float floatValue7 = (float)j / l;
         int intValue19 = (int)Math.floor(glowEspRendererBounds16.x * floatValue6);
         int intValue20 = (int)Math.floor(glowEspRendererBounds16.y * floatValue7);
         int intValue21 = (int)Math.ceil((glowEspRendererBounds16.x + glowEspRendererBounds16.width) * floatValue6);
         int intValue22 = (int)Math.ceil((glowEspRendererBounds16.y + glowEspRendererBounds16.height) * floatValue7);
         return resolve2(new GlowEspRenderer.GlowEspRendererBounds(intValue19, intValue20, intValue21 - intValue19, intValue22 - intValue20), i, j);
      } else {
         return null;
      }
   }

   private static void invoke7(GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds17, int i, int j) {
      if (glowEspRendererBounds17 == null) {
         GL11.glDisable(3089);
      } else {
         GL11.glEnable(3089);
         GL11.glScissor(glowEspRendererBounds17.x, j - glowEspRendererBounds17.y - glowEspRendererBounds17.height, glowEspRendererBounds17.width, glowEspRendererBounds17.height);
      }
   }

   private static void invoke8(GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds18, int i, int j, FramebufferUtils.GlStateSnapshot glStateSnapshot3) {
      GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds19 = resolve3(glowEspRendererBounds18, glStateSnapshot3.ints[2], glStateSnapshot3.ints[3], i, j);
      if (glowEspRendererBounds19 == null) {
         GL11.glDisable(3089);
      } else {
         GL11.glEnable(3089);
         GL11.glScissor(
            glStateSnapshot3.ints[0] + glowEspRendererBounds19.x, glStateSnapshot3.ints[1] + glStateSnapshot3.ints[3] - glowEspRendererBounds19.y - glowEspRendererBounds19.height, glowEspRendererBounds19.width, glowEspRendererBounds19.height
         );
      }
   }

   private void invoke9() {
      GlStateGuard.getINSTANCE().invoke2(2);
      GL11.glDrawArrays(5, 0, 4);
   }

   private static void invoke10(GlShaderProgram glShaderProgram, String string, int i) {
      int intValue23 = glShaderProgram.compute2(string);
      if (intValue23 >= 0) {
         GL20.glUniform1i(intValue23, i);
      }
   }

   private static void invoke11(GlShaderProgram glShaderProgram2, String string, float f) {
      int intValue24 = glShaderProgram2.compute2(string);
      if (intValue24 >= 0) {
         GL20.glUniform1f(intValue24, f);
      }
   }

   private static void invoke12(GlShaderProgram glShaderProgram3, String string, float f, float g) {
      int intValue25 = glShaderProgram3.compute2(string);
      if (intValue25 >= 0) {
         GL20.glUniform2f(intValue25, f, g);
      }
   }

   private static void invoke13(GlShaderProgram glShaderProgram4, String string, float f, float g, float h, float i) {
      int intValue26 = glShaderProgram4.compute2(string);
      if (intValue26 >= 0) {
         GL20.glUniform4f(intValue26, f, g, h, i);
      }
   }

   private static boolean check10() {
      return RenderSystem.isOnRenderThread() && GLFW.glfwGetCurrentContext() != 0L;
   }

   private void invoke14(GlowEspRenderer.GlowEspRendererState glowEspRendererState5) {
      if (glowEspRendererState5.intValue != 0) {
         GL30.glDeleteFramebuffers(glowEspRendererState5.intValue);
      }

      if (glowEspRendererState5.intValue2 != 0) {
         GL11.glDeleteTextures(glowEspRendererState5.intValue2);
      }

      glowEspRendererState5.intValue = 0;
      glowEspRendererState5.intValue2 = 0;
      glowEspRendererState5.intValue3 = 0;
      glowEspRendererState5.intValue4 = 0;
      glowEspRendererState5.flag = true;
      glowEspRendererState5.glowEspRendererBounds = null;
   }

   @Override
   public void close() {
      if (!check10()) {
         this.flag = false;
         this.flag2 = false;
      } else {
         this.invoke14(this.glowEspRendererState);
         this.invoke14(this.glowEspRendererState2);
         this.invoke14(this.glowEspRendererState3);
         this.invoke14(this.glowEspRendererState4);
         this.invoke14(this.glowEspRendererState5);
         if (this.intValue != 0) {
            GL30.glDeleteVertexArrays(this.intValue);
            this.intValue = 0;
         }

         if (this.intValue2 != 0) {
            GL15.glDeleteBuffers(this.intValue2);
            this.intValue2 = 0;
         }

         if (this.glShaderProgram != null) {
            this.glShaderProgram.invoke2();
            this.glShaderProgram = null;
         }

         if (this.glShaderProgram2 != null) {
            this.glShaderProgram2.invoke2();
            this.glShaderProgram2 = null;
         }

         if (this.glShaderProgram3 != null) {
            this.glShaderProgram3.invoke2();
            this.glShaderProgram3 = null;
         }

         if (this.glShaderProgram4 != null) {
            this.glShaderProgram4.invoke2();
            this.glShaderProgram4 = null;
         }

         if (this.glShaderProgram5 != null) {
            this.glShaderProgram5.invoke2();
            this.glShaderProgram5 = null;
         }

         this.intValue5 = -1;
         this.flag = false;
         this.flag2 = false;
      }
   }

   public record GlowEspRendererBounds(int x, int y, int width, int height) {
   }

   public record GlowEspRendererData(
      float radius,
      float outlineWidth,
      float glowStrength,
      float outlineStrength,
      float opacity,
      int debugView,
      int colorStyle,
      int autoColor,
      float topR,
      float topG,
      float topB,
      float bottomR,
      float bottomG,
      float bottomB
   ) {
   }

   static final class GlowEspRendererState {
      int intValue;
      int intValue2;
      int intValue3;
      int intValue4;
      boolean flag = true;
      GlowEspRenderer.GlowEspRendererBounds glowEspRendererBounds;
   }
}
