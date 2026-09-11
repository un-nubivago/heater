package niv.heater;

import static net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;
import static net.minecraft.resources.Identifier.fromNamespaceAndPath;
import static niv.heater.Heater.MOD_ID;

import org.jspecify.annotations.NullMarked;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import niv.heater.screen.ThermostatMenu;

@Environment(EnvType.CLIENT)
@NullMarked
public class ThermostatScreen extends AbstractContainerScreen<ThermostatMenu> {
    private static final Identifier TEXTURE = fromNamespaceAndPath(MOD_ID, "textures/gui/container/thermostat.png");

    public ThermostatScreen(ThermostatMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(GUI_TEXTURED, TEXTURE, xo, yo, .0F, .0F, this.imageWidth, this.imageHeight, 256,256);
    }
}
