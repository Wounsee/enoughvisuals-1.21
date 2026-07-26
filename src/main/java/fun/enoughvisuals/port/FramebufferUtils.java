package fun.enoughvisuals.port;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public final class FramebufferUtils {
   private static final int INT_VALUE = 32;

   private FramebufferUtils() {
   }

   public static FramebufferUtils.GlStateSnapshot captureGlState() {
      FramebufferUtils.GlStateSnapshot glStateSnapshot = new FramebufferUtils.GlStateSnapshot();
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         IntBuffer intBuffer = memoryStack.mallocInt(16);
         ByteBuffer byteBuffer = memoryStack.malloc(4);
         GL11.glGetIntegerv(36006, intBuffer);
         glStateSnapshot.intValue = intBuffer.get(0);
         GL11.glGetIntegerv(36010, intBuffer);
         glStateSnapshot.intValue2 = intBuffer.get(0);
         GL11.glGetIntegerv(3073, intBuffer);
         glStateSnapshot.intValue3 = intBuffer.get(0);
         GL11.glGetIntegerv(3074, intBuffer);
         glStateSnapshot.intValue4 = intBuffer.get(0);
         GL11.glGetIntegerv(35725, intBuffer);
         glStateSnapshot.intValue9 = intBuffer.get(0);
         GL11.glGetIntegerv(34229, intBuffer);
         glStateSnapshot.intValue10 = intBuffer.get(0);
         GL11.glGetIntegerv(34964, intBuffer);
         glStateSnapshot.intValue11 = intBuffer.get(0);
         GL11.glGetIntegerv(34965, intBuffer);
         glStateSnapshot.intValue12 = intBuffer.get(0);
         GL11.glGetIntegerv(34016, intBuffer);
         int intValue = intBuffer.get(0);
         glStateSnapshot.intValue13 = intValue == 0 ? '蓀' : intValue;

         for (int intValue2 = 0; intValue2 < 32; intValue2++) {
            GL13.glActiveTexture(33984 + intValue2);
            GL11.glGetIntegerv(32873, intBuffer);
            glStateSnapshot.ints3[intValue2] = intBuffer.get(0);
         }

         GL13.glActiveTexture(glStateSnapshot.intValue13);
         int intValue3 = glStateSnapshot.intValue13 - 33984;
         glStateSnapshot.intValue14 = intValue3 >= 0 && intValue3 < 32 ? glStateSnapshot.ints3[intValue3] : 0;
         GL11.glGetIntegerv(3317, intBuffer);
         glStateSnapshot.intValue15 = intBuffer.get(0) == 0 ? 4 : intBuffer.get(0);
         GL11.glGetIntegerv(2978, intBuffer);
         glStateSnapshot.ints[0] = intBuffer.get(0);
         glStateSnapshot.ints[1] = intBuffer.get(1);
         glStateSnapshot.ints[2] = intBuffer.get(2);
         glStateSnapshot.ints[3] = intBuffer.get(3);
         GL11.glGetIntegerv(3088, intBuffer);
         glStateSnapshot.ints2[0] = intBuffer.get(0);
         glStateSnapshot.ints2[1] = intBuffer.get(1);
         glStateSnapshot.ints2[2] = intBuffer.get(2);
         glStateSnapshot.ints2[3] = intBuffer.get(3);
         GL11.glGetIntegerv(32969, intBuffer);
         glStateSnapshot.intValue5 = intBuffer.get(0);
         GL11.glGetIntegerv(32968, intBuffer);
         glStateSnapshot.intValue6 = intBuffer.get(0);
         GL11.glGetIntegerv(32971, intBuffer);
         glStateSnapshot.intValue7 = intBuffer.get(0);
         GL11.glGetIntegerv(32970, intBuffer);
         glStateSnapshot.intValue8 = intBuffer.get(0);
         GL11.glGetBooleanv(3107, byteBuffer);
         glStateSnapshot.flag6 = byteBuffer.get(0) != 0;
         glStateSnapshot.flag7 = byteBuffer.get(1) != 0;
         glStateSnapshot.flag8 = byteBuffer.get(2) != 0;
         glStateSnapshot.flag9 = byteBuffer.get(3) != 0;
         GL11.glGetBooleanv(2930, byteBuffer);
         glStateSnapshot.flag10 = byteBuffer.get(0) != 0;
         glStateSnapshot.flag4 = GL11.glIsEnabled(3042);
         glStateSnapshot.flag2 = GL11.glIsEnabled(2929);
         glStateSnapshot.flag3 = GL11.glIsEnabled(2884);
         glStateSnapshot.flag = GL11.glIsEnabled(3089);
         glStateSnapshot.flag5 = GL11.glIsEnabled(36281);
      } catch (Throwable exception) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable exception2) {
               exception.addSuppressed(exception2);
            }
         }

         throw exception;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      return glStateSnapshot;
   }

   public static void restoreGlState(FramebufferUtils.GlStateSnapshot glStateSnapshot2) {
      if (glStateSnapshot2 != null) {
         invoke("restore-entry");
         int intValue4 = compute(glStateSnapshot2.intValue);
         int intValue5 = glStateSnapshot2.intValue2 == glStateSnapshot2.intValue ? intValue4 : compute(glStateSnapshot2.intValue2);
         GL30.glBindFramebuffer(36009, intValue4);
         GL30.glBindFramebuffer(36008, intValue5);
         GL11.glDrawBuffer(compute2(intValue4, glStateSnapshot2.intValue3));
         GL11.glReadBuffer(compute3(intValue5, glStateSnapshot2.intValue4));
         GL20.glUseProgram(glStateSnapshot2.intValue9);
         GL30.glBindVertexArray(glStateSnapshot2.intValue10);
         GL15.glBindBuffer(34962, glStateSnapshot2.intValue11);
         GL15.glBindBuffer(34963, glStateSnapshot2.intValue12);

         for (int intValue6 = 0; intValue6 < 32; intValue6++) {
            GL13.glActiveTexture(33984 + intValue6);
            GL11.glBindTexture(3553, glStateSnapshot2.ints3[intValue6]);
         }

         GL13.glActiveTexture(glStateSnapshot2.intValue13);
         GL11.glPixelStorei(3317, glStateSnapshot2.intValue15);
         invoke2(3042, glStateSnapshot2.flag4);
         invoke2(2929, glStateSnapshot2.flag2);
         invoke2(2884, glStateSnapshot2.flag3);
         invoke2(3089, glStateSnapshot2.flag);
         invoke2(36281, glStateSnapshot2.flag5);
         GL14.glBlendFuncSeparate(glStateSnapshot2.intValue5, glStateSnapshot2.intValue6, glStateSnapshot2.intValue7, glStateSnapshot2.intValue8);
         GL11.glColorMask(glStateSnapshot2.flag6, glStateSnapshot2.flag7, glStateSnapshot2.flag8, glStateSnapshot2.flag9);
         GL11.glDepthMask(glStateSnapshot2.flag10);
         GL11.glViewport(glStateSnapshot2.ints[0], glStateSnapshot2.ints[1], glStateSnapshot2.ints[2], glStateSnapshot2.ints[3]);
         invoke("restore-viewport");
         GL11.glScissor(glStateSnapshot2.ints2[0], glStateSnapshot2.ints2[1], glStateSnapshot2.ints2[2], glStateSnapshot2.ints2[3]);
         invoke("restore-scissor");
      }
   }

   private static void invoke(String stage) {
      int error = GL11.glGetError();
      if (error != 0) {
         System.err.println("[PortedGLProbe] stage=" + stage + " error=0x" + Integer.toHexString(error));
      }
   }

   public static boolean check(int i, int j) {
      int intValue7 = compute(j);
      GL30.glBindFramebuffer(i, intValue7);
      return intValue7 == j;
   }

   public static int compute(int i) {
      if (i <= 0) {
         return 0;
      } else {
         try {
            return GL30.glIsFramebuffer(i) ? i : 0;
         } catch (Throwable exception3) {
            return 0;
         }
      }
   }

   private static int compute2(int i, int j) {
      return i == 0 && j != 1029 && j != 1028 && j != 1032 ? 1029 : j;
   }

   private static int compute3(int i, int j) {
      return i == 0 && j != 1029 && j != 1028 && j != 1032 ? 1029 : j;
   }

   private static void invoke2(int i, boolean bl) {
      if (bl) {
         GL11.glEnable(i);
      } else {
         GL11.glDisable(i);
      }
   }

   public static final class GlStateSnapshot {
      public int intValue;
      public int intValue2;
      public int intValue3;
      public int intValue4;
      public final int[] ints = new int[4];
      public boolean flag;
      public final int[] ints2 = new int[4];
      public boolean flag2;
      public boolean flag3;
      public boolean flag4;
      public boolean flag5;
      public int intValue5;
      public int intValue6;
      public int intValue7;
      public int intValue8;
      public boolean flag6;
      public boolean flag7;
      public boolean flag8;
      public boolean flag9;
      public boolean flag10;
      public int intValue9;
      public int intValue10;
      public int intValue11;
      public int intValue12;
      public int intValue13;
      public int intValue14;
      public final int[] ints3 = new int[32];
      public int intValue15;
   }
}
