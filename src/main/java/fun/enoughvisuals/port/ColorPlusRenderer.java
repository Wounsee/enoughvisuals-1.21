package fun.enoughvisuals.port;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class ColorPlusRenderer implements AutoCloseable {
   private static final String ASSETS_PORTED_SHADERS_MAINMENU_MENU_QUAD_VERT = "assets/enoughfx/shaders/mainmenu/menu_quad.vert";
   private static final String ASSETS_PORTED_SHADERS_COLORPLUS_CP_GRADE_FRAG = "assets/enoughfx/shaders/colorplus/cp_grade.frag";
   private static final String ASSETS_PORTED_SHADERS_COLORPLUS_CP_BLOOM_EXTRACT_FRAG = "assets/enoughfx/shaders/colorplus/cp_bloom_extract.frag";
   private static final String ASSETS_PORTED_SHADERS_COLORPLUS_CP_BLOOM_BLUR_FRAG = "assets/enoughfx/shaders/colorplus/cp_bloom_blur.frag";
   private static final int INT_VALUE = 4;
   private static volatile ColorPlusRenderer instance;
   private final FullscreenQuad fullscreenQuad = new FullscreenQuad();
   private ShaderProgram shaderProgram;
   private FullscreenQuad.FullscreenQuadResources fullscreenQuadResources;
   private FullscreenQuad.FullscreenQuadResources fullscreenQuadResources2;
   private FullscreenQuad.FullscreenQuadResources fullscreenQuadResources3;
   private int intValue;
   private int intValue2;
   private int intValue3;
   private int intValue4;
   private int intValue5;
   private int intValue6;
   private int intValue7;
   private int intValue8;
   private int intValue9;
   private int intValue10;
   private boolean flag;
   private boolean flag2;
   private boolean flag3;

   public static ColorPlusRenderer resolve() {
      ColorPlusRenderer colorPlusRenderer = instance;
      if (colorPlusRenderer != null) {
         return colorPlusRenderer;
      } else {
         Class<ColorPlusRenderer> type = ColorPlusRenderer.class;
         synchronized (ColorPlusRenderer.class){}

         try {
            if (instance == null) {
               instance = new ColorPlusRenderer();
            }

            return instance;
         } finally {
         }
      }
   }

   private ColorPlusRenderer() {
   }

   public void invoke(int i, int j, int k, ColorPlusRenderer.ColorPlusRendererState colorPlusRendererState) {
      if (!this.flag2 && colorPlusRendererState != null) {
         if (i > 0) {
            if (j > 1 && k > 1) {
               if (!(colorPlusRendererState.floatValue <= 0.001F)) {
                  int intValue = GL11.glGetInteger(36006);
                  int intValue2 = GL11.glGetInteger(36010);
                  int intValue3 = GL11.glGetInteger(36006);
                  FramebufferUtils.GlStateSnapshot glStateSnapshot = FramebufferUtils.captureGlState();
                  boolean flag = false;
                  boolean flag2 = false ;

                  label229: {
                     label230: {
                        label245: {
                           label232: {
                              label246: {
                                 try {
                                    flag2 = true;
                                    this.invoke6();
                                    if (this.flag2) {
                                       flag2 = false;
                                       break label229;
                                    }

                                    this.invoke4(j, k);
                                    if (this.intValue10 != 0) {
                                       if (this.intValue6 != 0) {
                                          if (this.intValue7 != 0) {
                                             GL30.glBindFramebuffer(36160, this.intValue10);
                                             GL30.glFramebufferTexture2D(36160, 36064, 3553, i, 0);
                                             GL11.glDrawBuffer(36064);
                                             GL11.glReadBuffer(36064);
                                             flag = true;
                                             if (GL30.glCheckFramebufferStatus(36160) != 36053) {
                                                flag2 = false;
                                                break label230;
                                             }

                                             GL11.glDisable(2929);
                                             GL11.glDisable(2884);
                                             GL11.glDisable(3089);
                                             GL11.glDisable(3042);
                                             GL11.glColorMask(true, true, true, true);
                                             this.invoke3(j, k);
                                             if (this.intValue == 0) {
                                                flag2 = false;
                                                break label245;
                                             }

                                             float floatValue = colorPlusRendererState.floatValue18;
                                             if (floatValue > 0.001F) {
                                                this.invoke2(colorPlusRendererState);
                                             }

                                             GL30.glBindFramebuffer(36160, this.intValue10);
                                             GL11.glViewport(0, 0, Math.max(0, j), Math.max(0, k));
                                             this.fullscreenQuadResources.invoke();
                                             this.fullscreenQuadResources.invoke4("uViewport", j, k);
                                             this.fullscreenQuadResources.invoke6("uRect", 0.0F, 0.0F, j, k);
                                             this.fullscreenQuadResources.invoke2("uScene", 0);
                                             this.fullscreenQuadResources.invoke2("uBloomTex", 1);
                                             this.fullscreenQuadResources.invoke4("uResolution", j, k);
                                             this.fullscreenQuadResources.invoke3("uStrength", measure(colorPlusRendererState.floatValue));
                                             this.fullscreenQuadResources.invoke3("uExposure", measure2(colorPlusRendererState.floatValue2));
                                             this.fullscreenQuadResources.invoke3("uContrast", measure2(colorPlusRendererState.floatValue3));
                                             this.fullscreenQuadResources.invoke3("uSaturation", measure2(colorPlusRendererState.floatValue4));
                                             this.fullscreenQuadResources.invoke3("uVibrance", measure2(colorPlusRendererState.floatValue5));
                                             this.fullscreenQuadResources.invoke3("uGamma", measure2(colorPlusRendererState.floatValue6));
                                             this.fullscreenQuadResources.invoke3("uTemperature", measure2(colorPlusRendererState.floatValue7));
                                             this.fullscreenQuadResources.invoke3("uTint", measure2(colorPlusRendererState.floatValue8));
                                             this.fullscreenQuadResources.invoke5("uLift", colorPlusRendererState.floatValue9, colorPlusRendererState.floatValue10, colorPlusRendererState.floatValue11);
                                             this.fullscreenQuadResources
                                                .invoke5("uGammaRgb", colorPlusRendererState.floatValue12, colorPlusRendererState.floatValue13, colorPlusRendererState.floatValue14);
                                             this.fullscreenQuadResources
                                                .invoke5("uGain", colorPlusRendererState.floatValue15, colorPlusRendererState.floatValue16, colorPlusRendererState.floatValue17);
                                             this.fullscreenQuadResources.invoke3("uBloomIntensity", measure(floatValue));
                                             this.fullscreenQuadResources.invoke3("uBloomThreshold", Math.max(0.05F, colorPlusRendererState.floatValue19));
                                             this.fullscreenQuadResources.invoke3("uBloomRadius", Math.max(2.0F, colorPlusRendererState.floatValue20));
                                             this.fullscreenQuadResources.invoke3("uSharpness", measure(colorPlusRendererState.floatValue21));
                                             this.fullscreenQuadResources.invoke3("uVignette", measure(colorPlusRendererState.floatValue22));
                                             this.fullscreenQuadResources.invoke3("uFlipY", colorPlusRendererState.flag ? 1.0F : 0.0F);
                                             GL13.glActiveTexture(33985);
                                             GL11.glBindTexture(3553, this.intValue4);
                                             GL13.glActiveTexture(33984);
                                             GL11.glBindTexture(3553, this.intValue);
                                             this.shaderProgram.invoke();
                                             if (!this.flag3) {
                                                System.out.println("[ColorPlus] First successful render at " + j + "x" + k + " bloom=" + (floatValue > 0.001F));
                                                this.flag3 = true;
                                                flag2 = false;
                                             } else {
                                                flag2 = false;
                                             }
                                             break label232;
                                          }

                                          flag2 = false;
                                       } else {
                                          flag2 = false;
                                       }
                                    } else {
                                       flag2 = false;
                                    }
                                    break label246;
                                 } catch (Throwable exception) {
                                    this.flag2 = true;
                                    System.err.println("[ColorPlus] BROKEN: " + exception.getMessage());
                                    exception.printStackTrace();
                                    flag2 = false;
                                 } finally {
                                    if (flag2) {
                                       GL13.glActiveTexture(33985);
                                       GL11.glBindTexture(3553, 0);
                                       GL13.glActiveTexture(33984);
                                       GL11.glBindTexture(3553, 0);
                                       GL20.glUseProgram(0);
                                       if (flag && this.intValue10 != 0) {
                                          GL30.glBindFramebuffer(36160, this.intValue10);
                                          GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                                       }

                                       GL30.glBindFramebuffer(36009, intValue);
                                       GL30.glBindFramebuffer(36008, intValue2);
                                       GL30.glBindFramebuffer(36160, intValue3);
                                       FramebufferUtils.restoreGlState(glStateSnapshot);
                                    }
                                 }

                                 GL13.glActiveTexture(33985);
                                 GL11.glBindTexture(3553, 0);
                                 GL13.glActiveTexture(33984);
                                 GL11.glBindTexture(3553, 0);
                                 GL20.glUseProgram(0);
                                 if (flag && this.intValue10 != 0) {
                                    GL30.glBindFramebuffer(36160, this.intValue10);
                                    GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                                 }

                                 GL30.glBindFramebuffer(36009, intValue);
                                 GL30.glBindFramebuffer(36008, intValue2);
                                 GL30.glBindFramebuffer(36160, intValue3);
                                 FramebufferUtils.restoreGlState(glStateSnapshot);
                                 return;
                              }

                              GL13.glActiveTexture(33985);
                              GL11.glBindTexture(3553, 0);
                              GL13.glActiveTexture(33984);
                              GL11.glBindTexture(3553, 0);
                              GL20.glUseProgram(0);
                              if (flag && this.intValue10 != 0) {
                                 GL30.glBindFramebuffer(36160, this.intValue10);
                                 GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                              }

                              GL30.glBindFramebuffer(36009, intValue);
                              GL30.glBindFramebuffer(36008, intValue2);
                              GL30.glBindFramebuffer(36160, intValue3);
                              FramebufferUtils.restoreGlState(glStateSnapshot);
                              return;
                           }

                           GL13.glActiveTexture(33985);
                           GL11.glBindTexture(3553, 0);
                           GL13.glActiveTexture(33984);
                           GL11.glBindTexture(3553, 0);
                           GL20.glUseProgram(0);
                           if (flag && this.intValue10 != 0) {
                              GL30.glBindFramebuffer(36160, this.intValue10);
                              GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                           }

                           GL30.glBindFramebuffer(36009, intValue);
                           GL30.glBindFramebuffer(36008, intValue2);
                           GL30.glBindFramebuffer(36160, intValue3);
                           FramebufferUtils.restoreGlState(glStateSnapshot);
                           return;
                        }

                        GL13.glActiveTexture(33985);
                        GL11.glBindTexture(3553, 0);
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(3553, 0);
                        GL20.glUseProgram(0);
                        if (flag && this.intValue10 != 0) {
                           GL30.glBindFramebuffer(36160, this.intValue10);
                           GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                        }

                        GL30.glBindFramebuffer(36009, intValue);
                        GL30.glBindFramebuffer(36008, intValue2);
                        GL30.glBindFramebuffer(36160, intValue3);
                        FramebufferUtils.restoreGlState(glStateSnapshot);
                        return;
                     }

                     GL13.glActiveTexture(33985);
                     GL11.glBindTexture(3553, 0);
                     GL13.glActiveTexture(33984);
                     GL11.glBindTexture(3553, 0);
                     GL20.glUseProgram(0);
                     if (flag && this.intValue10 != 0) {
                        GL30.glBindFramebuffer(36160, this.intValue10);
                        GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                     }

                     GL30.glBindFramebuffer(36009, intValue);
                     GL30.glBindFramebuffer(36008, intValue2);
                     GL30.glBindFramebuffer(36160, intValue3);
                     FramebufferUtils.restoreGlState(glStateSnapshot);
                     return;
                  }

                  GL13.glActiveTexture(33985);
                  GL11.glBindTexture(3553, 0);
                  GL13.glActiveTexture(33984);
                  GL11.glBindTexture(3553, 0);
                  GL20.glUseProgram(0);
                  if (flag && this.intValue10 != 0) {
                     GL30.glBindFramebuffer(36160, this.intValue10);
                     GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                  }

                  GL30.glBindFramebuffer(36009, intValue);
                  GL30.glBindFramebuffer(36008, intValue2);
                  GL30.glBindFramebuffer(36160, intValue3);
                  FramebufferUtils.restoreGlState(glStateSnapshot);
               }
            }
         }
      }
   }

   private void invoke2(ColorPlusRenderer.ColorPlusRendererState colorPlusRendererState2) {
      GL30.glBindFramebuffer(36160, this.intValue6);
      GL11.glViewport(0, 0, this.intValue8, this.intValue9);
      this.fullscreenQuadResources2.invoke();
      this.fullscreenQuadResources2.invoke4("uViewport", this.intValue8, this.intValue9);
      this.fullscreenQuadResources2.invoke6("uRect", 0.0F, 0.0F, this.intValue8, this.intValue9);
      this.fullscreenQuadResources2.invoke2("uScene", 0);
      this.fullscreenQuadResources2.invoke4("uResolution", this.intValue8, this.intValue9);
      this.fullscreenQuadResources2.invoke3("uThreshold", Math.max(0.05F, colorPlusRendererState2.floatValue19));
      this.fullscreenQuadResources2.invoke3("uSoftness", 0.4F);
      this.fullscreenQuadResources2.invoke3("uFlipY", colorPlusRendererState2.flag ? 1.0F : 0.0F);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, this.intValue);
      this.shaderProgram.invoke();
      GL30.glBindFramebuffer(36160, this.intValue7);
      GL11.glViewport(0, 0, this.intValue8, this.intValue9);
      this.fullscreenQuadResources3.invoke();
      this.fullscreenQuadResources3.invoke4("uViewport", this.intValue8, this.intValue9);
      this.fullscreenQuadResources3.invoke6("uRect", 0.0F, 0.0F, this.intValue8, this.intValue9);
      this.fullscreenQuadResources3.invoke2("uScene", 0);
      this.fullscreenQuadResources3.invoke4("uResolution", this.intValue8, this.intValue9);
      this.fullscreenQuadResources3.invoke4("uDirection", 1.0F, 0.0F);
      float floatValue2 = Math.max(2.0F, colorPlusRendererState2.floatValue20 / 4.0F);
      this.fullscreenQuadResources3.invoke3("uRadius", floatValue2);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, this.intValue4);
      this.shaderProgram.invoke();
      GL30.glBindFramebuffer(36160, this.intValue6);
      GL11.glViewport(0, 0, this.intValue8, this.intValue9);
      this.fullscreenQuadResources3.invoke();
      this.fullscreenQuadResources3.invoke4("uViewport", this.intValue8, this.intValue9);
      this.fullscreenQuadResources3.invoke6("uRect", 0.0F, 0.0F, this.intValue8, this.intValue9);
      this.fullscreenQuadResources3.invoke2("uScene", 0);
      this.fullscreenQuadResources3.invoke4("uResolution", this.intValue8, this.intValue9);
      this.fullscreenQuadResources3.invoke4("uDirection", 0.0F, 1.0F);
      this.fullscreenQuadResources3.invoke3("uRadius", floatValue2);
      GL11.glBindTexture(3553, this.intValue5);
      this.shaderProgram.invoke();
   }

   private void invoke3(int i, int j) {
      if (this.intValue == 0) {
         this.intValue = GL11.glGenTextures();
         if (this.intValue == 0) {
            return;
         }

         GL11.glBindTexture(3553, this.intValue);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         this.intValue2 = 0;
         this.intValue3 = 0;
      } else {
         GL11.glBindTexture(3553, this.intValue);
      }

      if (this.intValue2 == i && this.intValue3 == j) {
         GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, i, j);
      } else {
         GL11.glCopyTexImage2D(3553, 0, 32856, 0, 0, i, j, 0);
         this.intValue2 = i;
         this.intValue3 = j;
      }
   }

   private void invoke4(int i, int j) {
      int intValue4 = Math.max(2, i / 4);
      int intValue5 = Math.max(2, j / 4);
      if (this.intValue10 == 0) {
         this.intValue10 = GL30.glGenFramebuffers();
      }

      if (this.intValue4 == 0 || this.intValue8 != intValue4 || this.intValue9 != intValue5) {
         this.invoke5();
         this.intValue8 = intValue4;
         this.intValue9 = intValue5;
         this.intValue4 = compute(this.intValue8, this.intValue9);
         this.intValue5 = compute(this.intValue8, this.intValue9);
         this.intValue6 = compute2(this.intValue4);
         this.intValue7 = compute2(this.intValue5);
      }
   }

   private static int compute(int i, int j) {
      int intValue6 = GL11.glGenTextures();
      GL11.glBindTexture(3553, intValue6);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexImage2D(3553, 0, 32856, i, j, 0, 6408, 5121, (ByteBuffer)null);
      GL11.glBindTexture(3553, 0);
      return intValue6;
   }

   private static int compute2(int i) {
      int intValue7 = GL30.glGenFramebuffers();
      GL30.glBindFramebuffer(36160, intValue7);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, i, 0);
      GL11.glDrawBuffer(36064);
      return intValue7;
   }

   private void invoke5() {
      if (this.intValue6 != 0) {
         GL30.glDeleteFramebuffers(this.intValue6);
         this.intValue6 = 0;
      }

      if (this.intValue7 != 0) {
         GL30.glDeleteFramebuffers(this.intValue7);
         this.intValue7 = 0;
      }

      if (this.intValue4 != 0) {
         GL11.glDeleteTextures(this.intValue4);
         this.intValue4 = 0;
      }

      if (this.intValue5 != 0) {
         GL11.glDeleteTextures(this.intValue5);
         this.intValue5 = 0;
      }

      this.intValue8 = 0;
      this.intValue9 = 0;
   }

   private void invoke6() {
      if (!this.flag) {
         try {
            this.shaderProgram = new ShaderProgram();
            this.fullscreenQuadResources = this.fullscreenQuad
               .resolve("colorplus_grade", "assets/enoughfx/shaders/mainmenu/menu_quad.vert", "assets/enoughfx/shaders/colorplus/cp_grade.frag");
            this.fullscreenQuadResources2 = this.fullscreenQuad
               .resolve("colorplus_bloom_extract", "assets/enoughfx/shaders/mainmenu/menu_quad.vert", "assets/enoughfx/shaders/colorplus/cp_bloom_extract.frag");
            this.fullscreenQuadResources3 = this.fullscreenQuad
               .resolve("colorplus_bloom_blur", "assets/enoughfx/shaders/mainmenu/menu_quad.vert", "assets/enoughfx/shaders/colorplus/cp_bloom_blur.frag");
            this.flag = true;
            System.out.println("[ColorPlus] Shaders loaded successfully");
         } catch (Throwable exception2) {
            this.flag2 = true;
            System.err.println("[ColorPlus] Shader load FAILED: " + exception2.getMessage());
            exception2.printStackTrace();
         }
      }
   }

   private static float measure(float f) {
      if (Float.isNaN(f)) {
         return 0.0F;
      } else if (f < 0.0F) {
         return 0.0F;
      } else {
         return f > 1.0F ? 1.0F : f;
      }
   }

   private static float measure2(float f) {
      if (Float.isNaN(f)) {
         return 0.0F;
      } else if (f < -1.0F) {
         return -1.0F;
      } else {
         return f > 1.0F ? 1.0F : f;
      }
   }

   @Override
   public void close() {
      try {
         if (this.intValue != 0) {
            GL11.glDeleteTextures(this.intValue);
            this.intValue = 0;
         }

         this.invoke5();
         if (this.intValue10 != 0) {
            GL30.glDeleteFramebuffers(this.intValue10);
            this.intValue10 = 0;
         }

         if (this.shaderProgram != null) {
            this.shaderProgram.close();
            this.shaderProgram = null;
         }

         this.fullscreenQuad.close();
      } catch (Throwable exception3) {
      }

      this.flag = false;
      this.flag2 = false;
      this.flag3 = false;
   }

   public static final class ColorPlusRendererState {
      public float floatValue = 1.0F;
      public float floatValue2;
      public float floatValue3;
      public float floatValue4;
      public float floatValue5;
      public float floatValue6;
      public float floatValue7;
      public float floatValue8;
      public float floatValue9;
      public float floatValue10;
      public float floatValue11;
      public float floatValue12;
      public float floatValue13;
      public float floatValue14;
      public float floatValue15;
      public float floatValue16;
      public float floatValue17;
      public float floatValue18;
      public float floatValue19 = 0.9F;
      public float floatValue20 = 64.0F;
      public float floatValue21;
      public float floatValue22;
      public boolean flag = true;
   }
}
