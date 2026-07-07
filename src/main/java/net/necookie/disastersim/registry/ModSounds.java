package net.necookie.disastersim.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All {@link SoundEvent} registrations for the mod. Extracted from {@code BerongSMP} so the entry
 * point stays a thin bootstrap; {@link #register(IEventBus)} is called from its constructor.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, BerongSMP.MODID);

    /** Fire alarm ringing sound — loops via scheduled block ticks while ACTIVATED=true. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_ALARM_RING =
            SOUND_EVENTS.register("block.fire_alarm.ring",
                    () -> SoundEvent.createVariableRangeEvent(
                            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "block.fire_alarm.ring")));

    private ModSounds() {}

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
