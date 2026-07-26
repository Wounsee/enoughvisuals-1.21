package fun.enoughvisuals.port;

public abstract class BezierCurve {
   private final MutablePoint2D mutablePoint2D = new MutablePoint2D(0.0, 0.0);
   private final MutablePoint2D mutablePoint2D2 = new MutablePoint2D(1.0, 1.0);
   private MutablePoint2D mutablePoint2D3;
   private MutablePoint2D mutablePoint2D4;

   public BezierCurve(MutablePoint2D mutablePoint2D, MutablePoint2D mutablePoint2D2) {
      this.setMutablePoint2D3(mutablePoint2D);
      this.setMutablePoint2D4(mutablePoint2D2);
   }

   public BezierCurve() {
   }

   public abstract double measure(double d);

   public MutablePoint2D getMutablePoint2D() {
      return this.mutablePoint2D;
   }

   public MutablePoint2D getMutablePoint2D2() {
      return this.mutablePoint2D2;
   }

   public void setMutablePoint2D3(MutablePoint2D mutablePoint2D3) {
      this.mutablePoint2D3 = mutablePoint2D3;
   }

   public void setMutablePoint2D4(MutablePoint2D mutablePoint2D4) {
      this.mutablePoint2D4 = mutablePoint2D4;
   }

   public MutablePoint2D getMutablePoint2D3() {
      return this.mutablePoint2D3;
   }

   public MutablePoint2D getMutablePoint2D4() {
      return this.mutablePoint2D4;
   }
}
