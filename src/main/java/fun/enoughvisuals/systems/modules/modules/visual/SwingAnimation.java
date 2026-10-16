package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Register(name = "SwingAnimation", id = "swing_animation", description = "Плавные анимации руки", category = Category.VISUAL)
public final class SwingAnimation extends Module {
    public static SwingAnimation INSTANCE;
    public final ModeSetting mode=add(new ModeSetting("Анимация","mode","","Smooth","Smooth","Swipe","Swipe back","SwipeD","Down","Spin","Off"));
    public final SliderSetting speed=add(new SliderSetting("Скорость анимации","speed","",1.0,.1,3.0,.1));
    public final SliderSetting animationSize=add(new SliderSetting("Размер анимации","animation_size","",3.7,1.0,10.0,.1));
    public SwingAnimation(){INSTANCE=this;animationSize.visibleWhen(()->!mode.is("Off"));}
    public boolean shouldApplyAnimation(ItemStack stack){if(mode.is("Off")||stack==null||stack.isEmpty())return false;
        if(stack.isOf(Items.FILLED_MAP)||stack.isOf(Items.BOW)||stack.isOf(Items.CROSSBOW)||stack.isOf(Items.TRIDENT))return false;
        UseAction action=stack.getItem().getUseAction(stack);return action!=UseAction.EAT&&action!=UseAction.DRINK;}
    public float getSpeedMultiplier(){return speed.get().floatValue();}
    public float itemScale(Arm arm){return 1.0f;}

    public void applyTransform(MatrixStack matrices,float swing,Arm arm){
        int side=arm==Arm.RIGHT?1:-1;float f=MathHelper.clamp(swing,0,1);float sin=(float)Math.sin(f*Math.PI);float root=MathHelper.sin(MathHelper.sqrt(f)*(float)Math.PI);float amount=animationSize.get().floatValue();
        switch(mode.get()){
            case "Swipe"->{matrices.translate(side*.67F,-.32F,-1);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90*side));matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60*side));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin*-amount*10));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));}
            case "Swipe back"->{matrices.translate(side*.67F,-.32F,-1);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90*side));matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60*side));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin*amount*10));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));}
            case "SwipeD"->{matrices.translate(side*.67F,-.32F,-1);matrices.translate(root*-amount/35F,0,root*-amount/35F);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(root*-amount*5));matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(50));}
            case "Down"->{matrices.translate(side*.67F,-.32F,-1);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80*side));matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30*side));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin*-amount*10));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));}
            case "Spin"->{matrices.translate(side*.56F,-.42F,-.72F);matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f*360));matrices.translate(0,-.1,0);}
            default->{matrices.translate(side*.56F,-.42F,-.72F);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side*(45+sin*-amount*3)));matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side*sin*-amount*2));matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin*-amount*10));matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side*-45));matrices.translate(0,-.1,0);}
        }
    }
    public void applyHandOffset(MatrixStack matrices,Arm arm){}
}
