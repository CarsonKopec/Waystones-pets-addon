package com.github.imagineforgee.waystonespetsaddon.common.client.gui;

import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The bone icon button next to the Waystones search box that opens {@link PetSelectionScreen}. */
public class PetsButton extends Button {
    private static final int SIZE = 20;
    private static final ItemStack ICON = new ItemStack(Items.BONE);

    private PetsButton(int x, int y, OnPress onPress) {
        super(x, y, SIZE, SIZE, Component.translatable("gui.waystonespetsaddon.pets_button"), onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.translatable("gui.waystonespetsaddon.pets_button.tooltip")));
    }

    public static void addTo(WaystoneSelectionScreenBase screen) {
        // Waystones keeps its search box private, so find it among the screen's widgets.
        EditBox searchBox = null;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox box) {
                searchBox = box;
                break;
            }
        }
        if (searchBox == null) {
            return;
        }

        // Go past anything Waystones puts after the search box on its row, like the sort button some versions have,
        // which would otherwise sit under this button and take its clicks.
        int searchBoxRight = searchBox.getX() + searchBox.getWidth();
        int right = searchBoxRight;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.getX() >= searchBoxRight
                    && widget.getY() < searchBox.getY() + searchBox.getHeight() && searchBox.getY() < widget.getY() + widget.getHeight()) {
                right = Math.max(right, widget.getX() + widget.getWidth());
            }
        }
        BalmClient.getScreens().addRenderableWidget(screen,
                new PetsButton(right + 4, searchBox.getY(), button -> PetSelectionScreen.open(screen)));
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int color) {
        guiGraphics.renderItem(ICON, getX() + (SIZE - 16) / 2, getY() + (SIZE - 16) / 2);
    }
}
