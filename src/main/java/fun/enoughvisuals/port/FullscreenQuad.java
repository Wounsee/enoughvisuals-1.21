package fun.enoughvisuals.port;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class FullscreenQuad implements AutoCloseable {
   private final Map<String, FullscreenQuad.FullscreenQuadResources> valuesByKey = new LinkedHashMap<>();
   static int intValue = -1;

   public FullscreenQuad.FullscreenQuadResources resolve(String string, String string2, String string3) {
      FullscreenQuad.FullscreenQuadResources fullscreenQuadResources = this.valuesByKey.get(string);
      if (fullscreenQuadResources != null) {
         return fullscreenQuadResources;
      } else {
         FullscreenQuad.FullscreenQuadResources fullscreenQuadResources2 = new FullscreenQuad.FullscreenQuadResources(GlShaderProgram.resolve(string2, string3));
         this.valuesByKey.put(string, fullscreenQuadResources2);
         return fullscreenQuadResources2;
      }
   }

   @Override
   public void close() {
      for (FullscreenQuad.FullscreenQuadResources fullscreenQuadResources3 : this.valuesByKey.values()) {
         fullscreenQuadResources3.close();
      }

      this.valuesByKey.clear();
      intValue = -1;
   }

   public void invoke() {
      intValue = -1;
   }

   public static final class FullscreenQuadResources implements AutoCloseable {
      private final GlShaderProgram glShaderProgram;
      private final Map<String, Integer> valuesByKey = new HashMap<>();
      private final Map<String, FullscreenQuad.FullscreenQuadState> valuesByKey2 = new HashMap<>();

      FullscreenQuadResources(GlShaderProgram glShaderProgram) {
         this.glShaderProgram = glShaderProgram;
      }

      public void invoke() {
         int intValue = this.glShaderProgram.getIntValue();
         if (FullscreenQuad.intValue != intValue || GL11.glGetInteger(35725) != intValue) {
            GL20.glUseProgram(intValue);
            FullscreenQuad.intValue = intValue;
         }
      }

      public void invoke2(String string, int i) {
         int intValue2 = this.compute(string);
         if (intValue2 >= 0 && this.resolve(string).check(0, i, 0.0F, 0.0F, 0.0F, 0.0F)) {
            GL20.glUniform1i(intValue2, i);
         }
      }

      public void invoke3(String string, float f) {
         int intValue3 = this.compute(string);
         if (intValue3 >= 0 && this.resolve(string).check(1, 0, f, 0.0F, 0.0F, 0.0F)) {
            GL20.glUniform1f(intValue3, f);
         }
      }

      public void invoke4(String string, float f, float g) {
         int intValue4 = this.compute(string);
         if (intValue4 >= 0 && this.resolve(string).check(2, 0, f, g, 0.0F, 0.0F)) {
            GL20.glUniform2f(intValue4, f, g);
         }
      }

      public void invoke5(String string, float f, float g, float h) {
         int intValue5 = this.compute(string);
         if (intValue5 >= 0 && this.resolve(string).check(3, 0, f, g, h, 0.0F)) {
            GL20.glUniform3f(intValue5, f, g, h);
         }
      }

      public void invoke6(String string, float f, float g, float h, float i) {
         int intValue6 = this.compute(string);
         if (intValue6 >= 0 && this.resolve(string).check(4, 0, f, g, h, i)) {
            GL20.glUniform4f(intValue6, f, g, h, i);
         }
      }

      private int compute(String string) {
         return this.valuesByKey.computeIfAbsent(string, this.glShaderProgram::compute2);
      }

      private FullscreenQuad.FullscreenQuadState resolve(String string) {
         return this.valuesByKey2.computeIfAbsent(string, stringx -> new FullscreenQuad.FullscreenQuadState());
      }

      @Override
      public void close() {
         this.glShaderProgram.invoke2();
         this.valuesByKey.clear();
         this.valuesByKey2.clear();
         FullscreenQuad.intValue = -1;
      }
   }

   static final class FullscreenQuadState {
      private int intValue = -1;
      private int intValue2;
      private float floatValue;
      private float floatValue2;
      private float floatValue3;
      private float floatValue4;

      boolean check(int i, int j, float f, float g, float h, float k) {
         if (this.intValue == i
            && this.intValue2 == j
            && Float.floatToIntBits(this.floatValue) == Float.floatToIntBits(f)
            && Float.floatToIntBits(this.floatValue2) == Float.floatToIntBits(g)
            && Float.floatToIntBits(this.floatValue3) == Float.floatToIntBits(h)
            && Float.floatToIntBits(this.floatValue4) == Float.floatToIntBits(k)) {
            return false;
         } else {
            this.intValue = i;
            this.intValue2 = j;
            this.floatValue = f;
            this.floatValue2 = g;
            this.floatValue3 = h;
            this.floatValue4 = k;
            return true;
         }
      }
   }
}
