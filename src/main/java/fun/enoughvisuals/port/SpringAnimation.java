package fun.enoughvisuals.port;

import lombok.Generated;

public final class SpringAnimation {
   private static final float FLOAT_VALUE = 0.004166667F;
   private static final float FLOAT_VALUE_2 = 1.0E-4F;
   private static final float FLOAT_VALUE_3 = 0.016666668F;
   private static final float FLOAT_VALUE_4 = 0.25F;
   private static final int INT_VALUE = 60;
   private float floatValue;
   private float floatValue2;
   private float floatValue3;
   private long timestamp = Long.MIN_VALUE;

   public SpringAnimation(float f) {
      this.floatValue = f;
   }

   public float measure(float f, SpringSpec springSpec) {
      float floatValue = this.measure10();
      if (floatValue < 0.0F) {
         return this.floatValue;
      } else {
         SpringSpec springSpec2 = springSpec == null ? SpringSpec.resolve2() : springSpec;
         this.floatValue3 += floatValue;
         int intValue = 0;

         while (this.floatValue3 >= 0.004166667F && intValue < 60) {
            this.invoke2(f, springSpec2);
            this.floatValue3 -= 0.004166667F;
            intValue++;
            if (this.check(f, springSpec2)) {
               this.invoke(f);
               break;
            }
         }

         if (intValue == 60) {
            this.floatValue3 = 0.0F;
         }

         return this.floatValue;
      }
   }

   public float measure2(float f, SpringAnimation.MotionSpec motionSpec) {
      float floatValue2 = this.measure10();
      if (floatValue2 < 0.0F) {
         return this.floatValue;
      } else {
         SpringAnimation.MotionSpec motionSpec2 = motionSpec == null ? SpringAnimation.MotionSpec.resolve() : motionSpec;
         float floatValue3 = this.floatValue;
         this.floatValue = measure4(this.floatValue, f, floatValue2, motionSpec2.floatValue);
         this.floatValue2 = (this.floatValue - floatValue3) / Math.max(floatValue2, 1.0E-4F);
         this.floatValue2 = this.floatValue2 * (float)Math.exp(-motionSpec2.floatValue2 * floatValue2);
         this.floatValue3 = 0.0F;
         if (Math.abs(f - this.floatValue) <= motionSpec2.floatValue3 && Math.abs(this.floatValue2) <= motionSpec2.floatValue4) {
            this.invoke(f);
         }

         return this.floatValue;
      }
   }

   public void invoke(float f) {
      this.floatValue = f;
      this.floatValue2 = 0.0F;
      this.floatValue3 = 0.0F;
   }

   public boolean check(float f, SpringSpec springSpec3) {
      SpringSpec springSpec4 = springSpec3 == null ? SpringSpec.resolve2() : springSpec3;
      return Math.abs(f - this.floatValue) <= springSpec4.getFloatValue3() && Math.abs(this.floatValue2) <= springSpec4.getFloatValue4();
   }

   public static float measure3() {
      AnimationSystem animationSystem = AnimationSystem.getINSTANCE();
      float floatValue4 = animationSystem.getFloatValue();
      if (!Float.isFinite(floatValue4) || floatValue4 <= 0.0F) {
         return 0.016666668F;
      } else {
         return floatValue4 < 1.0E-4F ? 1.0E-4F : Math.min(0.25F, floatValue4);
      }
   }

   public static float measure4(float f, float g, float h, float i) {
      float floatValue5 = measure11(h);
      float floatValue6 = 1.0F - (float)Math.exp(-Math.max(0.001F, i) * floatValue5);
      return f + (g - f) * floatValue6;
   }

   public static float measure5(float f, float g, float h, float i) {
      float floatValue7 = 1.0F - (float)Math.exp(-Math.max(0.0F, h) / Math.max(1.0F, i));
      return f + (g - f) * floatValue7;
   }

   public static float measure6(float f, float g, float h) {
      return f * (float)Math.exp(-Math.max(0.001F, h) * measure11(g));
   }

   public static float measure7(float f, float g, float h) {
      return Math.abs(f) <= 1.0E-6F ? 0.0F : f * (float)Math.exp(-Math.max(0.0F, g) / Math.max(1.0F, h));
   }

   public static float measure8(float f) {
      return Math.max(0.0F, Math.min(1.0F, f));
   }

   public static float measure9(float f, float g, float h) {
      float floatValue8 = measure8(h);
      return f + (g - f) * floatValue8;
   }

   private void invoke2(float f, SpringSpec springSpec5) {
      this.floatValue2 = this.floatValue2
         + ((f - this.floatValue) * springSpec5.getFloatValue() - this.floatValue2 * springSpec5.getFloatValue2());
      this.floatValue = this.floatValue + this.floatValue2;
   }

   private float measure10() {
      AnimationSystem animationSystem2 = AnimationSystem.getINSTANCE();
      long longValue = animationSystem2.getTimestamp2();
      if (longValue == this.timestamp) {
         return -1.0F;
      } else {
         this.timestamp = longValue;
         return measure3();
      }
   }

   private static float measure11(float f) {
      if (!Float.isFinite(f) || f <= 0.0F) {
         return 0.016666668F;
      } else {
         return f < 1.0E-4F ? 1.0E-4F : Math.min(0.25F, f);
      }
   }

   @Generated
   public float getFloatValue() {
      return this.floatValue;
   }

   @Generated
   public float getFloatValue2() {
      return this.floatValue2;
   }

   @Generated
   public float getFloatValue3() {
      return this.floatValue3;
   }

   @Generated
   public long getTimestamp() {
      return this.timestamp;
   }

   @Generated
   public void setFloatValue(float f) {
      this.floatValue = f;
   }

   @Generated
   public void setFloatValue2(float f) {
      this.floatValue2 = f;
   }

   @Generated
   public void setFloatValue3(float f) {
      this.floatValue3 = f;
   }

   @Generated
   public void setTimestamp(long l) {
      this.timestamp = l;
   }

   public static final class MotionSpec {
      final float floatValue;
      final float floatValue2;
      final float floatValue3;
      final float floatValue4;

      public MotionSpec(float f, float g, float h, float i) {
         this.floatValue = f;
         this.floatValue2 = g;
         this.floatValue3 = h;
         this.floatValue4 = i;
      }

      public static SpringAnimation.MotionSpec resolve() {
         return new SpringAnimation.MotionSpec(18.5F, 1.8F, 0.35F, 18.0F);
      }

      public static SpringAnimation.MotionSpec resolve2() {
         return new SpringAnimation.MotionSpec(15.5F, 2.2F, 0.12F, 8.0F);
      }

      public static SpringAnimation.MotionSpec resolve3() {
         return new SpringAnimation.MotionSpec(9.5F, 1.4F, 0.001F, 0.001F);
      }

      @Generated
      public float getFloatValue() {
         return this.floatValue;
      }

      @Generated
      public float getFloatValue2() {
         return this.floatValue2;
      }

      @Generated
      public float getFloatValue3() {
         return this.floatValue3;
      }

      @Generated
      public float getFloatValue4() {
         return this.floatValue4;
      }
   }
}
