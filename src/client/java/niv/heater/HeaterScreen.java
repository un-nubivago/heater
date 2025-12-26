package niv.heater;

import static net.minecraft.resources.ResourceLocation.tryBuild;
import static niv.heater.Heater.MOD_ID;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import niv.heater.screen.HeaterMenu;

@Environment(EnvType.CLIENT)
public class HeaterScreen extends AbstractContainerScreen<HeaterMenu> {
    private static final ResourceLocation TEXTURE = tryBuild(MOD_ID, "textures/gui/container/heater.png");

    public HeaterScreen(HeaterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.isLit()) {
            int h = Mth.ceil(this.menu.getLitProgress() * 13f) + 1;
            guiGraphics.blit(TEXTURE, x + 80, y + 39 - h, 176, 12 - h, 14, h + 1);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return mouseX < guiLeft || mouseY < guiTop
                || mouseX >= guiLeft + this.imageWidth
                || mouseY >= guiTop + this.imageHeight;
    }
}
