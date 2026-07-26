package fun.enoughvisuals.port;

public final class GlStateGuard {
   private static final GlStateGuard INSTANCE = new GlStateGuard();
   private final Object object = new Object();
   private long timestamp = 0L;
   private long timestamp2 = 0L;
   private int intValue = 0;
   private int intValue2 = 0;
   private int intValue3 = 0;
   private int intValue4 = 0;

   private GlStateGuard() {
   }

   public static GlStateGuard getINSTANCE() {
      return INSTANCE;
   }

   public void invoke(int i, int j) {
      if (i > 0 && j > 0) {
         long longValue = System.nanoTime();
         synchronized (this.object) {
            this.timestamp = longValue;
            this.intValue = 0;
            this.intValue2 = 0;
         }
      }
   }

   public void invoke2(int i) {
      if (i < 0) {
         i = 0;
      }

      synchronized (this.object) {
         this.intValue++;
         this.intValue2 += i;
      }
   }

   public void invoke3() {
      long longValue2 = System.nanoTime();
      synchronized (this.object) {
         if (this.timestamp <= 0L) {
            this.timestamp = longValue2;
            this.timestamp2 = 0L;
            this.intValue3 = this.intValue;
            this.intValue4 = this.intValue2;
            this.intValue = 0;
            this.intValue2 = 0;
         } else {
            this.timestamp2 = Math.max(0L, longValue2 - this.timestamp);
            this.intValue3 = this.intValue;
            this.intValue4 = this.intValue2;
            this.timestamp = longValue2;
            this.intValue = 0;
            this.intValue2 = 0;
         }
      }
   }

   public GlStateGuard.GlStateGuardData resolve() {
      synchronized (this.object) {
         return new GlStateGuard.GlStateGuardData(this.timestamp2, this.intValue3, this.intValue4);
      }
   }

   public record GlStateGuardData(long frameDurationNanos, int drawCalls, int triangles) {
      public GlStateGuardData(long frameDurationNanos, int drawCalls, int triangles) {
         frameDurationNanos = Math.max(0L, frameDurationNanos);
         drawCalls = Math.max(0, drawCalls);
         triangles = Math.max(0, triangles);
         this.frameDurationNanos = frameDurationNanos;
         this.drawCalls = drawCalls;
         this.triangles = triangles;
      }

      public double frameTimeMillis() {
         return this.frameDurationNanos / 1000000.0;
      }

      public double framesPerSecond() {
         return this.frameDurationNanos > 0L ? 1.0E9 / this.frameDurationNanos : 0.0;
      }
   }
}
