package fun.enoughvisuals.port;

import lombok.Generated;

public class Animation {
   private long timestamp;
   private double doubleValue;
   private double doubleValue2;
   private double doubleValue3;
   private double doubleValue4;
   private double doubleValue5;
   private LegacyEasingFunction legacyEasingFunction = LegacyEasingFunctions.LEGACY_EASING_FUNCTION;
   private boolean flag = false;
   private Runnable runnable;

   public Animation resolve(double d, double e) {
      return this.resolve4(d, e, LegacyEasingFunctions.LEGACY_EASING_FUNCTION, false);
   }

   public Animation resolve2(double d, double e, LegacyEasingFunction legacyEasingFunction) {
      return this.resolve4(d, e, legacyEasingFunction, false);
   }

   public Animation resolve3(double d, double e, boolean bl) {
      return this.resolve4(d, e, LegacyEasingFunctions.LEGACY_EASING_FUNCTION, bl);
   }

   public Animation resolve4(double d, double e, LegacyEasingFunction legacyEasingFunction2, boolean bl) {
      double doubleValue = Math.max(0.0, e * 1000.0);
      if (doubleValue <= 0.0) {
         this.setLegacyEasingFunction(legacyEasingFunction2).setDoubleValue(0.0).setTimestamp(0L).setDoubleValue2(d).setDoubleValue3(d).setDoubleValue4(d);
         return this;
      } else if (this.getDoubleValue3() != d || this.getTimestamp() == 0L && this.getDoubleValue4() != d) {
         if (this.check4(bl, d)) {
            if (this.isFlag()) {
               System.out.println("Animate cancelled due to target val equals from val");
            }
         } else {
            this.setLegacyEasingFunction(legacyEasingFunction2).setDoubleValue(doubleValue).setTimestamp(System.currentTimeMillis()).setDoubleValue2(this.getDoubleValue4()).setDoubleValue3(d);
            if (this.isFlag()) {
               System.out
                  .println(
                     "#animate {\n    to value: "
                        + this.getDoubleValue3()
                        + "\n    from value: "
                        + this.getDoubleValue4()
                        + "\n    duration: "
                        + this.getDoubleValue()
                        + "\n}"
                  );
            }
         }

         return this;
      } else {
         this.setLegacyEasingFunction(legacyEasingFunction2).setDoubleValue(doubleValue);
         return this;
      }
   }

   public boolean check() {
      this.setDoubleValue5(this.getDoubleValue4());
      boolean flag = this.check2();
      if (flag) {
         this.setDoubleValue4(this.measure2(this.getDoubleValue2(), this.getDoubleValue3(), this.getLegacyEasingFunction().ease(this.measure())));
      } else {
         this.setTimestamp(0L);
         this.setDoubleValue4(this.getDoubleValue3());
         if (this.runnable != null) {
            this.runnable.run();
            this.runnable = null;
         }
      }

      return flag;
   }

   public boolean check2() {
      return !this.check3();
   }

   public boolean check3() {
      return this.measure() >= 1.0;
   }

   public double measure() {
      if (!(this.getDoubleValue() <= 0.0) && this.getTimestamp() != 0L) {
         double doubleValue2 = (System.currentTimeMillis() - this.getTimestamp()) / this.getDoubleValue();
         return Math.max(0.0, Math.min(1.0, doubleValue2));
      } else {
         return 1.0;
      }
   }

   public boolean check4(boolean bl, double d) {
      return bl && this.check2() && (d == this.getDoubleValue2() || d == this.getDoubleValue3() || d == this.getDoubleValue4());
   }

   public double measure2(double d, double e, double f) {
      return d + (e - d) * f;
   }

   public Animation setTimestamp(long l) {
      this.timestamp = l;
      return this;
   }

   public Animation setDoubleValue(double d) {
      this.doubleValue = d;
      return this;
   }

   public Animation setDoubleValue2(double d) {
      this.doubleValue2 = d;
      return this;
   }

   public Animation setDoubleValue3(double d) {
      this.doubleValue3 = d;
      return this;
   }

   public Animation setDoubleValue4(double d) {
      this.doubleValue4 = d;
      return this;
   }

   public Animation setDoubleValue5(double d) {
      this.doubleValue5 = d;
      return this;
   }

   public Animation setLegacyEasingFunction(LegacyEasingFunction legacyEasingFunction3) {
      this.legacyEasingFunction = legacyEasingFunction3;
      return this;
   }

   public Animation setFlag(boolean bl) {
      this.flag = bl;
      return this;
   }

   public Animation setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   public float measure3() {
      return (float)this.getDoubleValue4();
   }

   public float measure4() {
      return (float)this.getDoubleValue5();
   }

   public void invoke(double d) {
      this.resolve(d, 0.0);
      this.check();
      this.setDoubleValue4(d);
   }

   @Generated
   public long getTimestamp() {
      return this.timestamp;
   }

   @Generated
   public double getDoubleValue() {
      return this.doubleValue;
   }

   @Generated
   public double getDoubleValue2() {
      return this.doubleValue2;
   }

   @Generated
   public double getDoubleValue3() {
      return this.doubleValue3;
   }

   @Generated
   public double getDoubleValue4() {
      return this.doubleValue4;
   }

   @Generated
   public double getDoubleValue5() {
      return this.doubleValue5;
   }

   @Generated
   public LegacyEasingFunction getLegacyEasingFunction() {
      return this.legacyEasingFunction;
   }

   @Generated
   public boolean isFlag() {
      return this.flag;
   }

   @Generated
   public Runnable getRunnable() {
      return this.runnable;
   }
}
