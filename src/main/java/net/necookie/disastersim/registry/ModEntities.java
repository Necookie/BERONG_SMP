package net.necookie.disastersim.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.entity.CustomNpcEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All {@link EntityType} registrations for the mod. Extracted from {@code BerongSMP} so the entry
 * point stays a thin bootstrap; {@link #register(IEventBus)} is called from its constructor and
 * also wires the attribute-creation listener.
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BerongSMP.MODID);

    /** Single entity type shared by all instructor/character NPCs; NpcType stored in NBT selects the skin. */
    public static final DeferredHolder<EntityType<?>, EntityType<CustomNpcEntity>> CUSTOM_NPC =
            ENTITY_TYPES.register("custom_npc", id ->
                    EntityType.Builder.<CustomNpcEntity>of(CustomNpcEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    private ModEntities() {}

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::onEntityAttributes);
    }

    /** Registers the base attribute set (health, speed, etc.) for the custom NPC entity type. */
    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(CUSTOM_NPC.get(), CustomNpcEntity.createAttributes().build());
    }
}
