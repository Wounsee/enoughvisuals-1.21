package fun.enoughvisuals.port;

import net.minecraft.util.math.Vec3d;

public final class PlasmaPinchEffect {
   private final double doubleValue;
   private final double doubleValue2;
   private final double doubleValue3;
   private final double doubleValue4;
   private final double doubleValue5;
   private final double doubleValue6;
   private final long timestamp;
   private final long timestamp2;
   private final float floatValue;
   private final float floatValue2;
   private final float floatValue3;
   private final float floatValue4;
   private final float floatValue5;
   private final int intValue;

   public PlasmaPinchEffect(Vec3d vec3d, Vec3d vec3d2, long l, long m, float f, float g, float h, float i, int j) {
      this.doubleValue = vec3d.x;
      this.doubleValue2 = vec3d.y;
      this.doubleValue3 = vec3d.z;
      double doubleValue = vec3d2.x;
      double doubleValue2 = vec3d2.y;
      double doubleValue3 = vec3d2.z;
      double doubleValue4 = Math.sqrt(doubleValue * doubleValue + doubleValue2 * doubleValue2 + doubleValue3 * doubleValue3);
      if (doubleValue4 < 1.0E-6) {
         doubleValue = 0.0;
         doubleValue2 = 0.0;
         doubleValue3 = 1.0;
      } else {
         doubleValue /= doubleValue4;
         doubleValue2 /= doubleValue4;
         doubleValue3 /= doubleValue4;
      }

      this.doubleValue4 = doubleValue;
      this.doubleValue5 = doubleValue2;
      this.doubleValue6 = doubleValue3;
      this.timestamp = l;
      this.timestamp2 = Math.max(1L, m);
      this.floatValue = Math.max(0.1F, f);
      this.floatValue2 = Math.max(0.1F, g);
      this.floatValue3 = Math.max(0.05F, h);
      this.floatValue4 = i;
      this.floatValue5 = this.floatValue * 1.8F + 0.5F;
      this.intValue = j & 16777215;
   }

   public double getDoubleValue() {
      return this.doubleValue;
   }

   public double getDoubleValue2() {
      return this.doubleValue2;
   }

   public double getDoubleValue3() {
      return this.doubleValue3;
   }

   public double getDoubleValue4() {
      return this.doubleValue4;
   }

   public double getDoubleValue5() {
      return this.doubleValue5;
   }

   public double getDoubleValue6() {
      return this.doubleValue6;
   }

   public long getTimestamp() {
      return this.timestamp;
   }

   public long getTimestamp2() {
      return this.timestamp2;
   }

   public float getFloatValue() {
      return this.floatValue;
   }

   public float getFloatValue2() {
      return this.floatValue2;
   }

   public float getFloatValue3() {
      return this.floatValue3;
   }

   public float getFloatValue4() {
      return this.floatValue4;
   }

   public float getFloatValue5() {
      return this.floatValue5;
   }

   public int getIntValue() {
      return this.intValue;
   }

   public float measure(long l) {
      float floatValue = (float)(l - this.timestamp) / (float)this.timestamp2;
      return floatValue < 0.0F ? 0.0F : Math.min(floatValue, 1.0F);
   }

   public boolean check(long l) {
      return l - this.timestamp >= this.timestamp2;
   }
}
