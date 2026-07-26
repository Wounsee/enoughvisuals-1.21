package fun.enoughvisuals.port;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class ShaderProgram implements AutoCloseable {
   private final int intValue = GL30.glGenVertexArrays();
   private final int intValue2 = GL15.glGenBuffers();

   public ShaderProgram() {
      GL30.glBindVertexArray(this.intValue);
      GL15.glBindBuffer(34962, this.intValue2);
      float[] floatValues = new float[]{0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F};
      GL15.glBufferData(34962, floatValues, 35044);
      GL20.glEnableVertexAttribArray(0);
      GL20.glVertexAttribPointer(0, 2, 5126, false, 8, 0L);
      GL15.glBindBuffer(34962, 0);
      GL30.glBindVertexArray(0);
   }

   public void invoke() {
      GL30.glBindVertexArray(this.intValue);
      GL11.glDrawArrays(4, 0, 6);
      GL30.glBindVertexArray(0);
   }

   @Override
   public void close() {
      GL30.glDeleteVertexArrays(this.intValue);
      GL15.glDeleteBuffers(this.intValue2);
   }
}
