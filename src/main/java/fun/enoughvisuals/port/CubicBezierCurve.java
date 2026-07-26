package fun.enoughvisuals.port;

public class CubicBezierCurve extends BezierCurve {
   @Override
   public double measure(double d) {
      double doubleValue = 1.0 - d;
      double doubleValue2 = doubleValue * doubleValue;
      double doubleValue3 = d * d;
      MutablePoint2D mutablePoint2D = this.getMutablePoint2D3().resolve();
      return this.getMutablePoint2D()
         .resolve()
         .resolve3(doubleValue2, doubleValue)
         .resolve7(mutablePoint2D.resolve4(3.0 * doubleValue2 * d))
         .resolve7(mutablePoint2D.resolve6(this.getMutablePoint2D4()).resolve4(3.0 * doubleValue * doubleValue3))
         .resolve7(mutablePoint2D.resolve6(this.getMutablePoint2D2()).resolve4(doubleValue3 * d))
         .getDoubleValue2();
   }

   public static class CubicBezierCurveState {
      private CubicBezierCurve cubicBezierCurve = new CubicBezierCurve();

      public CubicBezierCurveState(CubicBezierCurve cubicBezierCurve) {
         this.cubicBezierCurve = cubicBezierCurve;
      }

      public CubicBezierCurveState() {
      }

      public CubicBezierCurve.CubicBezierCurveState resolve(MutablePoint2D mutablePoint2D2) {
         this.cubicBezierCurve.setMutablePoint2D3(mutablePoint2D2);
         return this;
      }

      public CubicBezierCurve.CubicBezierCurveState resolve2(MutablePoint2D mutablePoint2D3) {
         this.cubicBezierCurve.setMutablePoint2D4(mutablePoint2D3);
         return this;
      }

      public CubicBezierCurve getCubicBezierCurve() {
         return this.cubicBezierCurve;
      }
   }
}
