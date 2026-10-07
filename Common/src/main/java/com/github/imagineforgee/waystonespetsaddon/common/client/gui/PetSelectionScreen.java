package com.github.imagineforgee.waystonespetsaddon.common.client.gui;

import com.github.imagineforgee.waystonespetsaddon.common.network.PetListMessage;
import com.github.imagineforgee.waystonespetsaddon.common.network.RequestPetListMessage;
import com.github.imagineforgee.waystonespetsaddon.common.network.SetPetFollowsMessage;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lists the player's pets in teleport range and lets them choose which ones follow through waystones.
 * Opened from the Waystones menu and returns to it when closed.
 */
public class PetSelectionScreen extends Screen {
    private static final int LIST_TOP = 42;
    private static final int LIST_BOTTOM_MARGIN = 36;
    private static final int NO_RESPONSE_TICKS = 100;

    private final Screen parent;
    @Nullable
    private PetListMessage petList;
    // Kept apart from petList so toggles survive the widgets being rebuilt, e.g. on resize.
    private final Map<UUID, Boolean> followStates = new HashMap<>();
    private int ticksWaiting;

    private PetSelectionScreen(Screen parent) {
        super(Component.translatable("gui.waystonespetsaddon.pet_selection"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new PetSelectionScreen(parent));
        Balm.getNetworking().sendToServer(new RequestPetListMessage());
    }

    public static void handlePetList(PetListMessage message) {
        if (Minecraft.getInstance().screen instanceof PetSelectionScreen screen) {
            screen.petList = message;
            screen.followStates.clear();
            for (PetListMessage.Pet pet : message.pets()) {
                screen.followStates.put(pet.id(), pet.follows());
            }
            screen.rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        PetList list = new PetList(minecraft, width, height, LIST_TOP, height - LIST_BOTTOM_MARGIN);
        if (petList != null) {
            for (PetListMessage.Pet pet : petList.pets()) {
                list.addPet(pet, followStates.get(pet.id()), petList.teleportSittingPets(),
                        follows -> setFollows(pet, follows));
            }
        }
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build());
    }

    private void setFollows(PetListMessage.Pet pet, boolean follows) {
        followStates.put(pet.id(), follows);
        Balm.getNetworking().sendToServer(new SetPetFollowsMessage(pet.id(), follows));
    }

    @Override
    public void tick() {
        if (petList == null) {
            ticksWaiting++;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);

        Component status = null;
        if (petList == null) {
            status = Component.translatable(ticksWaiting < NO_RESPONSE_TICKS
                    ? "gui.waystonespetsaddon.loading"
                    : "gui.waystonespetsaddon.no_response");
        } else {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.waystonespetsaddon.pet_selection.subtitle",
                    formatBlocks(petList.teleportRadius()), petList.maxPetsToTeleport()), width / 2, 27, 0xA0A0A0);
            if (petList.pets().isEmpty()) {
                status = Component.translatable("gui.waystonespetsaddon.no_pets");
            }
        }

        if (status != null) {
            List<FormattedCharSequence> lines = font.split(status, width - 40);
            int y = (LIST_TOP + height - LIST_BOTTOM_MARGIN) / 2 - lines.size() * font.lineHeight / 2;
            for (FormattedCharSequence line : lines) {
                guiGraphics.drawCenteredString(font, line, width / 2, y, 0xA0A0A0);
                y += font.lineHeight;
            }
        }
    }

    private static String formatBlocks(double blocks) {
        return blocks == Math.floor(blocks) ? String.valueOf((int) blocks) : String.valueOf(blocks);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
