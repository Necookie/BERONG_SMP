package net.necookie.disastersim.tutorial;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persists each player's tutorial stage to the world's data storage. */
public class TutorialSavedData extends SavedData {

    public static final String DATA_NAME = "berongsmp_tutorial";

    private final Map<UUID, TutorialStage> stages = new HashMap<>();

    public TutorialStage getStage(UUID uuid) {
        return stages.getOrDefault(uuid, TutorialStage.NOT_STARTED);
    }

    public void setStage(UUID uuid, TutorialStage stage) {
        stages.put(uuid, stage);
        setDirty();
    }

    public static TutorialSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TutorialSavedData::new, TutorialSavedData::load),
                DATA_NAME
        );
    }

    private static TutorialSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TutorialSavedData data = new TutorialSavedData();
        // Stored as a flat CompoundTag keyed by UUID string — avoids ListTag API uncertainty
        CompoundTag players = tag.getCompound("players").orElse(new CompoundTag());
        for (String key : players.keySet()) {
            try {
                UUID uuid = UUID.fromString(key);
                TutorialStage stage = TutorialStage.valueOf(players.getString(key).orElse("NOT_STARTED"));
                data.stages.put(uuid, stage);
            } catch (IllegalArgumentException ignored) {
                // Corrupted entry — skip it
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag players = new CompoundTag();
        stages.forEach((uuid, stage) -> players.putString(uuid.toString(), stage.name()));
        tag.put("players", players);
        return tag;
    }
}
