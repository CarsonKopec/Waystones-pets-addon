package com.github.imagineforgee.waystonespetsaddon.mixin;

import com.github.imagineforgee.waystonespetsaddon.compat.EventBusCompat;
import net.blay09.mods.balm.neoforge.event.NeoForgeBalmEvents;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Balm runs its own handlers (Waystones' and this addon's) and then forwards every event to NeoForge's bus.
 * For Waystones' teleport events that forward throws on NeoForge 20.2.86+ and kills the teleport, so skip it
 * for event types the bus would reject. Only NeoForge-native subscribers to those events miss out, and the
 * bus can't deliver to them anyway.
 */
@Mixin(value = NeoForgeBalmEvents.class, remap = false)
public class NeoForgeBalmEventsMixin {

    @Redirect(method = "fireEvent", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"))
    private Event waystonespetsaddon$postIfBusAccepts(IEventBus bus, Event event) {
        return EventBusCompat.accepts(event.getClass()) ? bus.post(event) : event;
    }
}
