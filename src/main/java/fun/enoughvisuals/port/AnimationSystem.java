package fun.enoughvisuals.port;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AnimationSystem {
   public static final float FLOAT_VALUE = 240.0F;
   public static final float FLOAT_VALUE_2 = 0.004166667F;
   public static final int INT_VALUE = 60;
   private static final float FLOAT_VALUE_3 = 1.0E-4F;
   private static final float FLOAT_VALUE_4 = 0.016666668F;
   private static final float FLOAT_VALUE_5 = 0.1F;
   private static final AnimationSystem INSTANCE = new AnimationSystem();
   private final Object object = new Object();
   private final List<AnimationSystem.AnimationSystemPredicate> items = new ArrayList<>();
   private long timestamp = System.nanoTime();
   private float floatValue = 0.016666668F;
   private long timestamp2 = 0L;

   private AnimationSystem() {
   }

   public static AnimationSystem getINSTANCE() {
      return INSTANCE;
   }

   public void invoke() {
      long longValue = System.nanoTime();
      long longValue2 = longValue - this.timestamp;
      this.timestamp = longValue;
      if (longValue2 < 0L) {
         longValue2 = 0L;
      }

      float floatValue = (float)longValue2 / 1.0E9F;
      if (floatValue < 1.0E-4F) {
         floatValue = 1.0E-4F;
      } else if (floatValue > 0.1F) {
         floatValue = 0.016666668F;
      }

      this.floatValue = floatValue;
      this.timestamp2++;
      synchronized (this.object) {
         if (!this.items.isEmpty()) {
            Iterator iterator = this.items.iterator();

            while (iterator.hasNext()) {
               AnimationSystem.AnimationSystemPredicate animationSystemPredicate = (AnimationSystem.AnimationSystemPredicate)iterator.next();
               boolean flag = animationSystemPredicate.check2(floatValue);
               if (!flag) {
                  iterator.remove();
               }
            }
         }
      }
   }

   public float getFloatValue() {
      return this.floatValue;
   }

   public long getTimestamp2() {
      return this.timestamp2;
   }

   public void invoke2() {
      this.timestamp = System.nanoTime();
      this.floatValue = 0.016666668F;
      this.timestamp2++;
   }

   public void invoke3(AnimationSystem.AnimationSystemPredicate animationSystemPredicate2) {
      if (animationSystemPredicate2 != null) {
         synchronized (this.object) {
            if (!this.items.contains(animationSystemPredicate2)) {
               this.items.add(animationSystemPredicate2);
            }
         }
      }
   }

   public void invoke4(AnimationSystem.AnimationSystemPredicate animationSystemPredicate3) {
      if (animationSystemPredicate3 != null) {
         synchronized (this.object) {
            this.items.remove(animationSystemPredicate3);
         }
      }
   }

   public interface AnimationSystemPredicate {
      boolean check2(float f);
   }
}
