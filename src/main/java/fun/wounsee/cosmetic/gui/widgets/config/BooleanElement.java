package fun.wounsee.cosmetic.gui.widgets.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.config.ConfigType;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.gui.widgets.ParentedButton;
import fun.wounsee.cosmetic.gui.widgets.SwitchButton;
import fun.wounsee.cosmetic.gui.widgets.lists.ConfigList;

public class BooleanElement extends AbstractConfigElement {

    private final ParentedButton button;

    public BooleanElement(int width, ConfigType.BoolConfig config, ConfigList parentList, CategoryWidget parentCategory) {
        super(width, config, parentList, parentCategory);

        // button
        children.add(0, button = new ParentedButton(0, 0, 90, 20, config.tempValue ? SwitchButton.ON : SwitchButton.OFF, this, button -> config.tempValue = !(boolean) config.tempValue));
        button.setActive(CosmeticMod.debugModeEnabled() || !config.disabled);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        // reset enabled
        this.resetButton.setActive(!this.isDefault());

        // button text
        Component text = (boolean) config.tempValue ? SwitchButton.ON : SwitchButton.OFF;

        // edited colour
        if (isChanged())
            text = text.copy().setStyle(CosmeticMod.getAccentColor());

        // set text
        this.button.setMessage(text);

        // super render
        super.render(gui, mouseX, mouseY, delta);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.button.setX(x + getWidth() - 154);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.button.setY(y);
    }
}
