package fun.enoughvisuals.port;

public class MutablePoint2D {
   private double doubleValue;
   private double doubleValue2;

   public MutablePoint2D(double d, double e) {
      this.setDoubleValue(d);
      this.setDoubleValue2(e);
   }

   public MutablePoint2D(MutablePoint2D mutablePoint2D) {
      this.setDoubleValue(mutablePoint2D.getDoubleValue());
      this.setDoubleValue2(mutablePoint2D.getDoubleValue2());
   }

   public MutablePoint2D resolve() {
      return new MutablePoint2D(this);
   }

   public MutablePoint2D resolve2(double d, double e) {
      this.setDoubleValue(d);
      this.setDoubleValue2(e);
      return this;
   }

   public MutablePoint2D resolve3(double d, double e) {
      this.setDoubleValue(this.getDoubleValue() * d);
      this.setDoubleValue2(this.getDoubleValue2() * e);
      return this;
   }

   public MutablePoint2D resolve4(double d) {
      this.setDoubleValue(this.getDoubleValue() * d);
      this.setDoubleValue2(this.getDoubleValue2() * d);
      return this;
   }

   public MutablePoint2D resolve5(double d, double e) {
      this.setDoubleValue(this.getDoubleValue() + d);
      this.setDoubleValue2(this.getDoubleValue2() + e);
      return this;
   }

   public MutablePoint2D resolve6(MutablePoint2D mutablePoint2D2) {
      this.setDoubleValue(mutablePoint2D2.getDoubleValue());
      this.setDoubleValue2(mutablePoint2D2.getDoubleValue2());
      return this;
   }

   public MutablePoint2D resolve7(MutablePoint2D mutablePoint2D3) {
      this.setDoubleValue(this.getDoubleValue() + mutablePoint2D3.getDoubleValue());
      this.setDoubleValue2(this.getDoubleValue2() + mutablePoint2D3.getDoubleValue2());
      return this;
   }

   public double getDoubleValue() {
      return this.doubleValue;
   }

   public double getDoubleValue2() {
      return this.doubleValue2;
   }

   public void setDoubleValue(double d) {
      this.doubleValue = d;
   }

   public void setDoubleValue2(double d) {
      this.doubleValue2 = d;
   }
}
