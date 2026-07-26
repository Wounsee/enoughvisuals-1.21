package fun.enoughvisuals.port;

import java.nio.IntBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

public final class GlShaderProgram {
   private final int intValue;

   public GlShaderProgram(String string, String string2) {
      int intValue = compute(35633, string);
      int intValue2 = compute(35632, string2);
      this.intValue = GL20.glCreateProgram();
      GL20.glAttachShader(this.intValue, intValue);
      GL20.glAttachShader(this.intValue, intValue2);
      GL20.glLinkProgram(this.intValue);
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         IntBuffer intBuffer = memoryStack.mallocInt(1);
         GL20.glGetProgramiv(this.intValue, 35714, intBuffer);
         if (intBuffer.get(0) == 0) {
            String text = GL20.glGetProgramInfoLog(this.intValue);
            GL20.glDeleteShader(intValue);
            GL20.glDeleteShader(intValue2);
            GL20.glDeleteProgram(this.intValue);
            throw new IllegalStateException("Program link failed: " + text);
         }
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

      GL20.glDetachShader(this.intValue, intValue);
      GL20.glDetachShader(this.intValue, intValue2);
      GL20.glDeleteShader(intValue);
      GL20.glDeleteShader(intValue2);
   }

   public static GlShaderProgram resolve(String string, String string2) {
      String text2 = ResourceUtils.resolve(string);
      String text3 = ResourceUtils.resolve(string2);
      return new GlShaderProgram(text2, text3);
   }

   private static int compute(int i, String string) {
      int intValue3 = GL20.glCreateShader(i);
      GL20.glShaderSource(intValue3, string);
      GL20.glCompileShader(intValue3);
      MemoryStack memoryStack2 = MemoryStack.stackPush();

      try {
         IntBuffer intBuffer2 = memoryStack2.mallocInt(1);
         GL20.glGetShaderiv(intValue3, 35713, intBuffer2);
         if (intBuffer2.get(0) == 0) {
            String text4 = GL20.glGetShaderInfoLog(intValue3);
            GL20.glDeleteShader(intValue3);
            throw new IllegalStateException("Shader compile failed: " + text4);
         }
      } catch (Throwable exception3) {
         if (memoryStack2 != null) {
            try {
               memoryStack2.close();
            } catch (Throwable exception4) {
               exception3.addSuppressed(exception4);
            }
         }

         throw exception3;
      }

      if (memoryStack2 != null) {
         memoryStack2.close();
      }

      return intValue3;
   }

   public void invoke() {
      GL20.glUseProgram(this.intValue);
   }

   public void invoke2() {
      GL20.glDeleteProgram(this.intValue);
   }

   public int getIntValue() {
      return this.intValue;
   }

   public int compute2(String string) {
      return GL20.glGetUniformLocation(this.intValue, string);
   }
}
