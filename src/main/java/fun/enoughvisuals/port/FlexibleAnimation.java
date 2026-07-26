package fun.enoughvisuals.port;

public class FlexibleAnimation {
   private long timestamp;
   private double doubleValue;
   private double doubleValue2;
   private double doubleValue3;
   private double doubleValue4;
   private EasingFunction easingFunction;
   private BezierCurve bezierCurve;
   private InterpolationMode interpolationMode;
   private boolean flag;

   public FlexibleAnimation() {
      this.easingFunction = Easings.EASING_FUNCTION;
      this.bezierCurve = new CubicBezierCurve();
      this.interpolationMode = InterpolationMode.EASING;
      this.flag = false;
   }

   public FlexibleAnimation resolve(double d, double e) {
      return this.resolve5(d, e, Easings.EASING_FUNCTION, false);
   }

   public FlexibleAnimation resolve2(double d, double e, EasingFunction easingFunction) {
      return this.resolve5(d, e, easingFunction, false);
   }

   public FlexibleAnimation resolve3(double d, double e, BezierCurve bezierCurve) {
      return this.resolve6(d, e, bezierCurve, false);
   }

   public FlexibleAnimation resolve4(double d, double e, boolean bl) {
      return this.resolve5(d, e, Easings.EASING_FUNCTION, bl);
   }

   public FlexibleAnimation resolve5(double d, double e, EasingFunction easingFunction2, boolean bl) {
      if (this.check4(bl, d)) {
         if (this.isFlag()) {
            System.out.println("Animate cancelled due to target val equals from val");
         }

         return this;
      } else {
         this.setInterpolationMode(InterpolationMode.EASING)
            .setEasingFunction(easingFunction2)
            .setDoubleValue(e * 1000.0)
            .setTimestamp(System.currentTimeMillis())
            .setDoubleValue2(this.getDoubleValue4())
            .setDoubleValue3(d);
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

         return this;
      }
   }

   public FlexibleAnimation resolve6(double d, double e, BezierCurve bezierCurve2, boolean bl) {
      if (this.check4(bl, d)) {
         if (this.isFlag()) {
            System.out.println("Animate cancelled due to target val equals from val");
         }

         return this;
      } else {
         this.setInterpolationMode(InterpolationMode.BEZIER)
            .setBezierCurve(bezierCurve2)
            .setDoubleValue(e * 1000.0)
            .setTimestamp(System.currentTimeMillis())
            .setDoubleValue2(this.getDoubleValue4())
            .setDoubleValue3(d);
         if (this.isFlag()) {
            System.out
               .println(
                  "#animate {\n    to value: "
                     + this.getDoubleValue3()
                     + "\n    from value: "
                     + this.getDoubleValue4()
                     + "\n    duration: "
                     + this.getDoubleValue()
                     + "\n    type: "
                     + this.getInterpolationMode().name()
                     + "\n}"
               );
         }

         return this;
      }
   }

   public boolean check() {
      boolean flag = this.check2();
      if (flag) {
         if (this.getInterpolationMode().equals(InterpolationMode.BEZIER)) {
            this.setDoubleValue4(this.measure2(this.getDoubleValue2(), this.getDoubleValue3(), this.getBezierCurve().measure(this.measure())));
         } else {
            this.setDoubleValue4(this.measure2(this.getDoubleValue2(), this.getDoubleValue3(), this.getEasingFunction().ease(this.measure())));
         }
      } else {
         this.setTimestamp(0L);
         this.setDoubleValue4(this.getDoubleValue3());
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
      return (System.currentTimeMillis() - this.getTimestamp()) / this.getDoubleValue();
   }

   public boolean check4(boolean bl, double d) {
      return bl && this.check2() && (d == this.getDoubleValue2() || d == this.getDoubleValue3() || d == this.getDoubleValue4());
   }

   public double measure2(double d, double e, double f) {
      return d + (e - d) * f;
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

   public boolean isFlag() {
      return this.flag;
   }

   public InterpolationMode getInterpolationMode() {
      return this.interpolationMode;
   }

   public EasingFunction getEasingFunction() {
      return this.easingFunction;
   }

   public BezierCurve getBezierCurve() {
      return this.bezierCurve;
   }

   public FlexibleAnimation setTimestamp(long l) {
      this.timestamp = l;
      return this;
   }

   public FlexibleAnimation setDoubleValue(double d) {
      this.doubleValue = d;
      return this;
   }

   public FlexibleAnimation setDoubleValue2(double d) {
      this.doubleValue2 = d;
      return this;
   }

   public FlexibleAnimation setDoubleValue3(double d) {
      this.doubleValue3 = d;
      return this;
   }

   public FlexibleAnimation setDoubleValue4(double d) {
      this.doubleValue4 = d;
      return this;
   }

   public FlexibleAnimation setEasingFunction(EasingFunction easingFunction3) {
      this.easingFunction = easingFunction3;
      return this;
   }

   public FlexibleAnimation setFlag(boolean bl) {
      this.flag = bl;
      return this;
   }

   public FlexibleAnimation setBezierCurve(BezierCurve bezierCurve3) {
      this.bezierCurve = bezierCurve3;
      return this;
   }

   public FlexibleAnimation setInterpolationMode(InterpolationMode interpolationMode) {
      this.interpolationMode = interpolationMode;
      return this;
   }
}
