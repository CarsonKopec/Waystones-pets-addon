package com.github.imagineforgee.waystonespetsaddon.compat;

import net.neoforged.bus.api.Event;

import java.lang.reflect.Modifier;

/**
 * Since NeoForge 20.2.86 the event bus refuses to post an event whose hierarchy has an abstract class sitting on
 * a non-abstract one. Waystones 15.2.0's teleport events are built exactly like that (abstract
 * WaystoneTeleportEvent on Balm's concrete BalmEvent), and no fixed Waystones, Balm or NeoForge was released
 * for 1.20.2.
 */
public final class EventBusCompat {
    private static final ClassValue<Boolean> ACCEPTED = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> eventType) {
            return busAccepts(eventType);
        }
    };

    private EventBusCompat() {
    }

    /** Whether NeoForge's event bus can post events of this type without throwing. */
    public static boolean accepts(Class<?> eventType) {
        return ACCEPTED.get(eventType);
    }

    // Mirrors the bus's own check: once a class in the hierarchy is abstract, every superclass up to Event must be too.
    private static boolean busAccepts(Class<?> eventType) {
        boolean belowAbstract = false;
        for (Class<?> type = eventType; type != null && type != Event.class; type = type.getSuperclass()) {
            boolean isAbstract = Modifier.isAbstract(type.getModifiers());
            if (belowAbstract && !isAbstract) {
                return false;
            }
            belowAbstract |= isAbstract;
        }
        return true;
    }
}
