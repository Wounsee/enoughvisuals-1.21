package fun.enoughvisuals.port;

public final class Easings {
   public static final double DOUBLE_VALUE = 1.70158;
   public static final double DOUBLE_VALUE_2 = 2.5949095;
   public static final double DOUBLE_VALUE_3 = 2.70158;
   public static final double DOUBLE_VALUE_4 = Math.PI * 2.0 / 3.0;
   public static final double DOUBLE_VALUE_5 = Math.PI * 4.0 / 9.0;
   public static final EasingFunction EASING_FUNCTION = d -> d;
   public static final EasingFunction EASING_FUNCTION_2 = resolve2(2);
   public static final EasingFunction EASING_FUNCTION_3 = resolve4(2);
   public static final EasingFunction EASING_FUNCTION_4 = resolve5(2.0);
   public static final EasingFunction EASING_FUNCTION_5 = resolve2(3);
   public static final EasingFunction EASING_FUNCTION_6 = resolve4(3);
   public static final EasingFunction EASING_FUNCTION_7 = resolve5(3.0);
   public static final EasingFunction EASING_FUNCTION_8 = resolve2(4);
   public static final EasingFunction EASE_OUT_QUART = resolve4(4);
   public static final EasingFunction EASING_FUNCTION_9 = resolve5(4.0);
   public static final EasingFunction EASING_FUNCTION_10 = resolve2(5);
   public static final EasingFunction EASING_FUNCTION_11 = resolve4(5);
   public static final EasingFunction EASING_FUNCTION_12 = resolve5(5.0);
   public static final EasingFunction EASING_FUNCTION_13 = d -> 1.0 - Math.cos(d * Math.PI / 2.0);
   public static final EasingFunction EASING_FUNCTION_14 = d -> Math.sin(d * Math.PI / 2.0);
   public static final EasingFunction EASING_FUNCTION_15 = d -> -(Math.cos(Math.PI * d) - 1.0) / 2.0;
   public static final EasingFunction EASING_FUNCTION_16 = d -> 1.0 - Math.sqrt(1.0 - Math.pow(d, 2.0));
   public static final EasingFunction EASING_FUNCTION_17 = d -> Math.sqrt(1.0 - Math.pow(d - 1.0, 2.0));
   public static final EasingFunction EASING_FUNCTION_18 = d -> d < 0.5
      ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * d, 2.0))) / 2.0
      : (Math.sqrt(1.0 - Math.pow(-2.0 * d + 2.0, 2.0)) + 1.0) / 2.0;
   public static final EasingFunction EASING_FUNCTION_19 = d -> d != 0.0 && d != 1.0
      ? Math.pow(-2.0, 10.0 * d - 10.0) * Math.sin((d * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0))
      : d;
   public static final EasingFunction EASING_FUNCTION_20 = d -> d != 0.0 && d != 1.0
      ? Math.pow(2.0, -10.0 * d) * Math.sin((d * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0
      : d;
   public static final EasingFunction EASING_FUNCTION_21 = d -> {
      if (d != 0.0 && d != 1.0) {
         return d < 0.5
            ? -(Math.pow(2.0, 20.0 * d - 10.0) * Math.sin((20.0 * d - 11.125) * (Math.PI * 4.0 / 9.0))) / 2.0
            : Math.pow(2.0, -20.0 * d + 10.0) * Math.sin((20.0 * d - 11.125) * (Math.PI * 4.0 / 9.0)) / 2.0 + 1.0;
      } else {
         return d;
      }
   };
   public static final EasingFunction EASING_FUNCTION_22 = d -> d != 0.0 ? Math.pow(2.0, 10.0 * d - 10.0) : d;
   public static final EasingFunction EASING_FUNCTION_23 = d -> d != 1.0 ? 1.0 - Math.pow(2.0, -10.0 * d) : d;
   public static final EasingFunction EASING_FUNCTION_24 = d -> {
      if (d != 0.0 && d != 1.0) {
         return d < 0.5 ? Math.pow(2.0, 20.0 * d - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * d + 10.0)) / 2.0;
      } else {
         return d;
      }
   };
   public static final EasingFunction EASING_FUNCTION_25 = d -> 2.70158 * Math.pow(d, 3.0) - 1.70158 * Math.pow(d, 2.0);
   public static final EasingFunction EASING_FUNCTION_26 = d -> 1.0 + 2.70158 * Math.pow(d - 1.0, 3.0) + 1.70158 * Math.pow(d - 1.0, 2.0);
   public static final EasingFunction EASING_FUNCTION_27 = d -> d < 0.5
      ? Math.pow(2.0 * d, 2.0) * (7.189819 * d - 2.5949095) / 2.0
      : (Math.pow(2.0 * d - 2.0, 2.0) * (3.5949095 * (d * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
   public static final EasingFunction EASING_FUNCTION_28 = d -> {
      double doubleValue = 7.5625;
      double doubleValue2 = 2.75;
      if (d < 1.0 / doubleValue2) {
         return doubleValue * Math.pow(d, 2.0);
      } else if (d < 2.0 / doubleValue2) {
         return doubleValue * Math.pow(d - 1.5 / doubleValue2, 2.0) + 0.75;
      } else {
         return d < 2.5 / doubleValue2 ? doubleValue * Math.pow(d - 2.25 / doubleValue2, 2.0) + 0.9375 : doubleValue * Math.pow(d - 2.625 / doubleValue2, 2.0) + 0.984375;
      }
   };
   public static final EasingFunction EASING_FUNCTION_29 = d -> 1.0 - EASING_FUNCTION_28.ease(1.0 - d);
   public static final EasingFunction EASING_FUNCTION_30 = d -> d < 0.5 ? (1.0 - EASING_FUNCTION_28.ease(1.0 - 2.0 * d)) / 2.0 : (1.0 + EASING_FUNCTION_28.ease(2.0 * d - 1.0)) / 2.0;

   private Easings() {
   }

   public static EasingFunction resolve(double d) {
      return e -> Math.pow(e, d);
   }

   public static EasingFunction resolve2(int i) {
      return resolve((double)i);
   }

   public static EasingFunction resolve3(double d) {
      return e -> 1.0 - Math.pow(1.0 - e, d);
   }

   public static EasingFunction resolve4(int i) {
      return resolve3((double)i);
   }

   public static EasingFunction resolve5(double d) {
      return e -> e < 0.5 ? Math.pow(2.0, d - 1.0) * Math.pow(e, d) : 1.0 - Math.pow(-2.0 * e + 2.0, d) / 2.0;
   }
}
