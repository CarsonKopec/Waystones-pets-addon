package com.github.imagineforgee.waystonespetsaddon.common.client.gui;

import com.github.imagineforgee.waystonespetsaddon.common.network.PetListMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.List;
import java.util.function.Consumer;

/** One row per pet: a small model, its name and details, and a Follows/Stays toggle on the right. */
public class PetList extends ContainerObjectSelectionList<PetList.Entry> {
    private static final int ROW_WIDTH = 280;
    private static final int ROW_HEIGHT = 24;
    private static final int MODEL_SIZE = 24;
    private static final int BUTTON_WIDTH = 80;
    private static final Component FOLLOWS = Component.translatable("gui.waystonespetsaddon.follows").withStyle(ChatFormatting.GREEN);
    private static final Component STAYS = Component.translatable("gui.waystonespetsaddon.stays").withStyle(ChatFormatting.GRAY);
    private static final Tooltip FOLLOWS_TOOLTIP = Tooltip.create(Component.translatable("gui.waystonespetsaddon.follows.tooltip"));
    private static final Tooltip STAYS_TOOLTIP = Tooltip.create(Component.translatable("gui.waystonespetsaddon.stays.tooltip"));

    public PetList(Minecraft minecraft, int width, int height, int top, int bottom) {
        // Selection lists pass rows 4px less than the item height, leaving a gap between them.
        super(minecraft, width, height, top, bottom, ROW_HEIGHT + 4);
        setRenderBackground(false);
    }

    public void addPet(PetListMessage.Pet pet, boolean follows, boolean teleportSittingPets, Consumer<Boolean> onFollowsChanged) {
        addEntry(new Entry(pet, follows, teleportSittingPets, onFollowsChanged));
    }

    @Override
    public int getRowWidth() {
        return ROW_WIDTH;
    }

    @Override
    protected int getScrollbarPosition() {
        return width / 2 + ROW_WIDTH / 2 + 6;
    }

    public class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private final PetListMessage.Pet pet;
        private final boolean teleportSittingPets;
        private final CycleButton<Boolean> followButton;
        private Component details;
        private boolean detailsSitting;
        private int detailsBlocks;

        private Entry(PetListMessage.Pet pet, boolean follows, boolean teleportSittingPets, Consumer<Boolean> onFollowsChanged) {
            this.pet = pet;
            this.teleportSittingPets = teleportSittingPets;
            this.followButton = CycleButton.booleanBuilder(FOLLOWS, STAYS)
                    .withInitialValue(follows)
                    .withTooltip(value -> value ? FOLLOWS_TOOLTIP : STAYS_TOOLTIP)
                    .displayOnlyValue()
                    .create(0, 0, BUTTON_WIDTH, 20, pet.name(), (button, value) -> onFollowsChanged.accept(value));
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            // Read the pet live so the row keeps up, e.g. when switching to Follows stands it up. If it has walked
            // out of the client's tracking range, show no model and the state from when the screen opened.
            boolean sitting = pet.sitting();
            float distance = pet.distance();
            Entity found = minecraft.level != null ? minecraft.level.getEntity(pet.entityId()) : null;
            if (found instanceof LivingEntity entity) {
                renderModel(guiGraphics, entity, left, top + (height - MODEL_SIZE) / 2, mouseX, mouseY);
                if (entity instanceof TamableAnimal tamable) {
                    sitting = tamable.isInSittingPose();
                }
                if (minecraft.player != null) {
                    distance = entity.distanceTo(minecraft.player);
                }
            }

            Font font = minecraft.font;
            int textLeft = left + MODEL_SIZE + 4;
            int textWidth = width - (textLeft - left) - BUTTON_WIDTH - 8;
            int textTop = top + (height - 19) / 2;
            guiGraphics.drawString(font, clip(font, pet.name(), textWidth), textLeft, textTop, 0xFFFFFF);
            guiGraphics.drawString(font, clip(font, details(sitting, distance), textWidth), textLeft, textTop + 10, 0xA0A0A0);

            followButton.setX(left + width - BUTTON_WIDTH - 4);
            followButton.setY(top + (height - 20) / 2);
            followButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // Rows render every frame, so only rebuild the text when what it shows has changed.
        private Component details(boolean sitting, float distance) {
            int blocks = Math.max(1, Mth.ceil(distance));
            if (details == null || sitting != detailsSitting || blocks != detailsBlocks) {
                details = describe(pet, blocks, sitting, teleportSittingPets);
                detailsSitting = sitting;
                detailsBlocks = blocks;
            }
            return details;
        }

        // Scaled by the pet's hitbox so parrots, cats and large modded pets all fill the square about equally.
        // The vanilla renderer clips to the square, so rows scrolled half out of the list don't spill over.
        private static void renderModel(GuiGraphics guiGraphics, LivingEntity entity, int x, int y, int mouseX, int mouseY) {
            float largestSide = Math.max(entity.getBbHeight(), entity.getBbWidth());
            int scale = Math.max(1, (int) (MODEL_SIZE * 0.8f / largestSide));
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x, y, x + MODEL_SIZE, y + MODEL_SIZE,
                    scale, 0.0625f, mouseX, mouseY, entity);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(followButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(followButton);
        }
    }

    private static Component describe(PetListMessage.Pet pet, int blocks, boolean sitting, boolean teleportSittingPets) {
        MutableComponent details = Component.empty();
        if (pet.hasCustomName()) {
            details.append(pet.typeName()).append(" - ");
        }

        details.append(Component.translatable(blocks == 1
                ? "gui.waystonespetsaddon.block_away"
                : "gui.waystonespetsaddon.blocks_away", blocks));

        if (sitting) {
            details.append(" - ");
            details.append(teleportSittingPets
                    ? Component.translatable("gui.waystonespetsaddon.sitting")
                    : Component.translatable("gui.waystonespetsaddon.sitting_stays").withStyle(ChatFormatting.GOLD));
        }
        return details;
    }

    private static FormattedCharSequence clip(Font font, Component text, int maxWidth) {
        return Language.getInstance().getVisualOrder(font.substrByWidth(text, maxWidth));
    }
}
