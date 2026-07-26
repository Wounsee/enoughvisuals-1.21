package fun.enoughvisuals.port;

import fun.enoughvisuals.port.api.Module;
import fun.enoughvisuals.port.api.ModuleRegister;

@ModuleRegister(
   name = "MotionBlur",
   category = Category.Visuals,
   description = "Физически основанный MotionBlur, очень сильно повышает плавность картинки.",
   riskLevels = {ModuleRiskLevel.NEW}
)
public final class MotionBlur extends Module {
   public final NumberSetting plavnost = new NumberSetting("Плавность", 0.68F, 0.0F, 1.0F, 0.01F, true);

   public MotionBlur() {
      this.addSettings(new Setting[]{this.plavnost});
   }

   @Override
   public void onEnable() {
      MotionBlurRenderer.getINSTANCE().invoke3();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      MotionBlurRenderer.getINSTANCE().invoke3();
   }

   public MotionBlurRenderer.MotionBlurRendererState2 resolve() {
      float floatValue = Math.max(0.0F, Math.min(1.0F, this.plavnost.getValue()));
      float floatValue2 = floatValue * floatValue * (3.0F - 2.0F * floatValue);
      MotionBlurRenderer.MotionBlurRendererState2 motionBlurRendererState2 = new MotionBlurRenderer.MotionBlurRendererState2();
      motionBlurRendererState2.floatValue = 0.38F + floatValue2 * 0.92F;
      motionBlurRendererState2.floatValue2 = 1.1F + floatValue2 * 2.25F;
      motionBlurRendererState2.intValue = 5 + Math.round(floatValue2 * 7.0F);
      motionBlurRendererState2.floatValue3 = 24.0F + floatValue2 * 82.0F;
      motionBlurRendererState2.floatValue4 = 0.3F + floatValue2 * 0.42F;
      motionBlurRendererState2.floatValue5 = 0.22F + floatValue2 * 1.12F;
      motionBlurRendererState2.floatValue6 = 0.68F;
      motionBlurRendererState2.floatValue7 = 2.85F - floatValue2 * 1.35F;
      motionBlurRendererState2.floatValue8 = 0.035F + (1.0F - floatValue2) * 0.075F;
      return motionBlurRendererState2;
   }
}
