package fun.enoughvisuals.port;

import fun.enoughvisuals.port.api.Module;
import fun.enoughvisuals.port.api.ModuleRegister;

@ModuleRegister(
   name = "ColorPlus",
   category = Category.Visuals,
   description = "Цветокоррекция мира — пресеты и тонкая настройка"
)
public class ColorPlus extends Module {
   public final ModeSetting preset = new ModeSetting("Пресет", "Cinematic", ColorPlusPreset.resolve2());
   public final NumberSetting silaEffekta = new NumberSetting("Сила эффекта", 1.0F, 0.0F, 1.0F, 0.01F, true);
   public final BooleanSetting rezkostCas = new BooleanSetting("Резкость (CAS)", true);
   public final NumberSetting ekspozitsiya = new NumberSetting("Экспозиция", 0.0F, -0.5F, 0.5F, 0.01F, false);
   public final NumberSetting kontrast = new NumberSetting("Контраст", 0.0F, -0.5F, 0.5F, 0.01F, false);
   public final NumberSetting nasyschennost = new NumberSetting("Насыщенность", 0.0F, -0.5F, 0.5F, 0.01F, false);
   public final NumberSetting vibrance = new NumberSetting("Vibrance", 0.0F, -0.5F, 0.5F, 0.01F, false);
   public final NumberSetting gamma = new NumberSetting("Гамма", 0.0F, -0.5F, 0.5F, 0.01F, false);
   public final NumberSetting temperatura = new NumberSetting("Температура", 0.0F, -0.5F, 0.5F, 0.01F, false);
   public final NumberSetting ottenokZelyonyyMadzhenta = new NumberSetting("Оттенок (зелёный/маджента)", 0.0F, -0.5F, 0.5F, 0.01F, false);
   public final NumberSetting intensivnostBloom = new NumberSetting("Интенсивность Bloom", 0.0F, -0.3F, 0.3F, 0.01F, false);
   public final NumberSetting silaRezkosti = new NumberSetting("Сила резкости", 0.0F, -0.3F, 0.3F, 0.01F, false);
   public final NumberSetting vinetka = new NumberSetting("Виньетка", 0.0F, -0.3F, 0.3F, 0.01F, false);

   public ColorPlus() {
      this.addSettings(
         new Setting[]{
            this.preset,
            this.silaEffekta,
            this.rezkostCas,
            this.ekspozitsiya,
            this.kontrast,
            this.nasyschennost,
            this.vibrance,
            this.gamma,
            this.temperatura,
            this.ottenokZelyonyyMadzhenta,
            this.intensivnostBloom,
            this.silaRezkosti,
            this.vinetka
         }
      );
   }

   public ColorPlusPreset resolve() {
      return ColorPlusPreset.resolve(this.preset.getValue());
   }
}
