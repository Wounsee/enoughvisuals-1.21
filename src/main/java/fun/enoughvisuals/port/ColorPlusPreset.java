package fun.enoughvisuals.port;

public enum ColorPlusPreset {
   CINEMATIC(
      "Cinematic",
      0.03F,
      0.08F,
      0.06F,
      0.1F,
      -0.02F,
      0.05F,
      -0.02F,
      new float[]{-0.02F, -0.01F, 0.02F},
      new float[]{0.01F, 0.0F, -0.01F},
      new float[]{0.03F, 0.01F, -0.02F},
      0.55F,
      0.86F,
      64.0F,
      0.22F,
      0.08F
   ),
   VIBRANT(
      "Vibrant",
      0.04F,
      0.1F,
      0.16F,
      0.18F,
      -0.02F,
      0.02F,
      0.0F,
      new float[]{0.0F, 0.0F, 0.0F},
      new float[]{0.0F, 0.0F, 0.0F},
      new float[]{0.02F, 0.02F, 0.02F},
      0.45F,
      0.84F,
      60.0F,
      0.28F,
      0.04F
   ),
   NATURAL(
      "Natural",
      0.01F,
      0.04F,
      0.03F,
      0.06F,
      0.0F,
      0.02F,
      0.0F,
      new float[]{0.0F, 0.0F, 0.0F},
      new float[]{0.0F, 0.0F, 0.0F},
      new float[]{0.0F, 0.0F, 0.0F},
      0.3F,
      0.92F,
      48.0F,
      0.16F,
      0.02F
   ),
   SUNNY_FIELD(
      "Sunny Field",
      0.05F,
      0.06F,
      0.1F,
      0.18F,
      -0.03F,
      0.16F,
      -0.04F,
      new float[]{0.02F, 0.01F, -0.03F},
      new float[]{0.02F, 0.01F, -0.02F},
      new float[]{0.04F, 0.02F, -0.03F},
      0.65F,
      0.8F,
      70.0F,
      0.2F,
      0.06F
   ),
   URBAN_RAIN(
      "Urban Rain",
      -0.03F,
      0.05F,
      -0.06F,
      0.04F,
      0.01F,
      -0.18F,
      0.04F,
      new float[]{-0.02F, -0.01F, 0.04F},
      new float[]{0.0F, 0.01F, 0.02F},
      new float[]{-0.02F, -0.01F, 0.04F},
      0.4F,
      0.92F,
      56.0F,
      0.22F,
      0.1F
   ),
   SUNSET(
      "Sunset",
      0.03F,
      0.1F,
      0.1F,
      0.16F,
      -0.02F,
      0.2F,
      -0.06F,
      new float[]{0.04F, 0.01F, -0.04F},
      new float[]{0.02F, 0.0F, -0.02F},
      new float[]{0.05F, 0.02F, -0.03F},
      0.75F,
      0.78F,
      80.0F,
      0.2F,
      0.06F
   ),
   FROST(
      "Frost",
      0.01F,
      0.05F,
      -0.04F,
      0.04F,
      0.01F,
      -0.2F,
      -0.02F,
      new float[]{-0.02F, -0.01F, 0.04F},
      new float[]{-0.01F, 0.01F, 0.02F},
      new float[]{-0.02F, 0.01F, 0.04F},
      0.5F,
      0.86F,
      64.0F,
      0.24F,
      0.08F
   ),
   TEAL_ORANGE(
      "Teal-Orange",
      0.03F,
      0.12F,
      0.1F,
      0.12F,
      -0.02F,
      0.04F,
      0.0F,
      new float[]{-0.04F, -0.01F, 0.04F},
      new float[]{0.01F, 0.0F, -0.01F},
      new float[]{0.06F, 0.02F, -0.04F},
      0.6F,
      0.82F,
      70.0F,
      0.24F,
      0.08F
   ),
   VINTAGE(
      "Vintage",
      -0.02F,
      -0.02F,
      -0.08F,
      -0.04F,
      0.04F,
      0.1F,
      0.02F,
      new float[]{0.02F, 0.01F, -0.02F},
      new float[]{0.02F, 0.01F, -0.02F},
      new float[]{0.02F, 0.0F, -0.04F},
      0.3F,
      0.92F,
      50.0F,
      0.1F,
      0.16F
   ),
   NEON_NIGHT(
      "Neon Night",
      -0.02F,
      0.1F,
      0.16F,
      0.2F,
      0.02F,
      -0.1F,
      0.08F,
      new float[]{-0.02F, -0.02F, 0.05F},
      new float[]{0.0F, -0.01F, 0.02F},
      new float[]{0.02F, -0.02F, 0.06F},
      0.95F,
      0.74F,
      90.0F,
      0.22F,
      0.1F
   );

   public final String text;
   public final float floatValue;
   public final float floatValue2;
   public final float floatValue3;
   public final float floatValue4;
   public final float floatValue5;
   public final float floatValue6;
   public final float floatValue7;
   public final float[] floats;
   public final float[] floats2;
   public final float[] floats3;
   public final float floatValue8;
   public final float floatValue9;
   public final float floatValue10;
   public final float floatValue11;
   public final float floatValue12;

   private ColorPlusPreset(
      String string2,
      float f,
      float g,
      float h,
      float j,
      float k,
      float l,
      float m,
      float[] fs,
      float[] gs,
      float[] hs,
      float n,
      float o,
      float p,
      float q,
      float r
   ) {
      this.text = string2;
      this.floatValue = f;
      this.floatValue2 = g;
      this.floatValue3 = h;
      this.floatValue4 = j;
      this.floatValue5 = k;
      this.floatValue6 = l;
      this.floatValue7 = m;
      this.floats = fs;
      this.floats2 = gs;
      this.floats3 = hs;
      this.floatValue8 = n;
      this.floatValue9 = o;
      this.floatValue10 = p;
      this.floatValue11 = q;
      this.floatValue12 = r;
   }

   public static ColorPlusPreset resolve(String string) {
      if (string == null) {
         return CINEMATIC;
      } else {
         for (ColorPlusPreset colorPlusPreset : values()) {
            if (colorPlusPreset.text.equalsIgnoreCase(string)) {
               return colorPlusPreset;
            }
         }

         return CINEMATIC;
      }
   }

   public static String[] resolve2() {
      ColorPlusPreset[] colorPlusPresets = values();
      String[] texts = new String[colorPlusPresets.length + 1];

      for (int intValue = 0; intValue < colorPlusPresets.length; intValue++) {
         texts[intValue] = colorPlusPresets[intValue].text;
      }

      texts[colorPlusPresets.length] = "Custom";
      return texts;
   }
}
