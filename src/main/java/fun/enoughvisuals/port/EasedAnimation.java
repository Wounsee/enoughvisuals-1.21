package fun.enoughvisuals.port;

public class EasedAnimation {
   private long timestamp;
   private double doubleValue;
   private double doubleValue2;
   private double doubleValue3;
   private double doubleValue4;
   private double doubleValue5;
   private EasingFunction easingFunction = Easings.EASING_FUNCTION_14;
   private boolean flag = false;
   private Runnable runnable;

   public EasedAnimation resolve(double d, double e) {
      return this.resolve3(d, e, Easings.EASING_FUNCTION_14, false);
   }

   public EasedAnimation animateTo(double d, double e, EasingFunction easingFunction) {
      return this.resolve3(d, e, easingFunction, false);
   }

   public EasedAnimation resolve2(double d, double e, boolean bl) {
      return this.resolve3(d, e, Easings.EASING_FUNCTION_14, bl);
   }

   public EasedAnimation resolve3(double d, double e, EasingFunction easingFunction2, boolean bl) {
      if (this.check4(bl, d)) {
         if (this.isFlag()) {
            System.out.println("Animate cancelled due to target val equals from val");
         }
      } else {
         this.setEasingFunction(easingFunction2).setDoubleValue(e * 1000.0).setTimestamp(System.currentTimeMillis()).setDoubleValue2(this.getDoubleValue4()).setDoubleValue3(d);
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
   }

   public boolean check() {
      this.setDoubleValue5(this.getDoubleValue4());
      boolean flag = this.check2();
      if (flag) {
         this.setDoubleValue4(this.measure2(this.getDoubleValue2(), this.getDoubleValue3(), this.getEasingFunction().ease(this.measure())));
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
      return this.doubleValue == 0.0 ? 1.0 : (System.currentTimeMillis() - this.getTimestamp()) / this.getDoubleValue();
   }

   public boolean check4(boolean bl, double d) {
      return bl && this.check2() && (d == this.getDoubleValue2() || d == this.getDoubleValue3() || d == this.getDoubleValue4());
   }

   public double measure2(double d, double e, double f) {
      return d + (e - d) * f;
   }

   public EasedAnimation setTimestamp(long l) {
      this.timestamp = l;
      return this;
   }

   public EasedAnimation setDoubleValue(double d) {
      this.doubleValue = d;
      return this;
   }

   public EasedAnimation setDoubleValue2(double d) {
      this.doubleValue2 = d;
      return this;
   }

   public EasedAnimation setDoubleValue3(double d) {
      this.doubleValue3 = d;
      return this;
   }

   public EasedAnimation setDoubleValue4(double d) {
      this.doubleValue4 = d;
      return this;
   }

   public EasedAnimation setDoubleValue5(double d) {
      this.doubleValue5 = d;
      return this;
   }

   public EasedAnimation setEasingFunction(EasingFunction easingFunction3) {
      this.easingFunction = easingFunction3;
      return this;
   }

   public EasedAnimation setFlag(boolean bl) {
      this.flag = bl;
      return this;
   }

   public EasedAnimation setRunnable(Runnable runnable) {
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
      this.resolve(d, 1.0E-13);
      this.check();
      this.setDoubleValue4(d);
   }

   public long getTimestamp() {
      return this.timestamp;
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

   public EasingFunction getEasingFunction() {
      return this.easingFunction;
   }

   public boolean isFlag() {
      return this.flag;
   }
}
