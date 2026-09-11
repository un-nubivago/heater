package niv.heater;

import static net.minecraft.resources.Identifier.fromNamespaceAndPath;
import static niv.heater.Heater.MOD_ID;

import org.jspecify.annotations.NullMarked;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import niv.heater.screen.HeaterMenu;

@Environment(EnvType.CLIENT)
@NullMarked
public class HeaterScreen extends AbstractContainerScreen<HeaterMenu> {
    private static final Identifier LIT_PROGRESS_SPRITE = fromNamespaceAndPath(MOD_ID, "container/heater/lit_progress");
    private static final Identifier TEXTURE = fromNamespaceAndPath(MOD_ID, "textures/gui/container/heater.png");

    public HeaterScreen(HeaterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, .0F, .0F, this.imageWidth, this.imageHeight, 256,
                256);
        if (this.menu.isLit()) {
            int h = Mth.ceil(this.menu.getLitProgress() * 13f) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - h, x + 80,
                    y + 42 - h, 14, h);
        }
    }
}
