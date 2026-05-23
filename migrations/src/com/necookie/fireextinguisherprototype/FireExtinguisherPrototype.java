package com.necookie.fireextinguisherprototype;

import com.necookie.fireextinguisherprototype.items.FireExtinguisherItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FireExtinguisherPrototype implements ModInitializer {
    public static final String MOD_ID = "fireextinguisherprototype";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Item FIRE_EXTINGUISHER = new FireExtinguisherItem(
            new Item.Settings().maxCount(1)
    );

    @Override
    public void onInitialize() {
        Registry.register(
                Registries.ITEM,
                Identifier.of(MOD_ID, "fire_extinguisher"),
                FIRE_EXTINGUISHER
        );

        LOGGER.info("Fire extinguisher prototype loaded.");
    }
}
