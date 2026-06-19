package net.necookie.disastersim.tutorial;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persists each player's tutorial stage to the world's data storage. */
public class TutorialSavedData extends SavedData {

    private static final Codec<TutorialStage> STAGE_CODEC = Codec.STRING.xmap(
        s -> { try { return TutorialStage.valueOf(s); } catch (Exception e) { return TutorialStage.NOT_STARTED; } },
        TutorialStage::name
    );

    public static final Codec<TutorialSavedData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, STAGE_CODEC)
        .xmap(map -> {
            TutorialSavedData data = new TutorialSavedData();
            data.stages.putAll(map);
            return data;
        }, data -> new HashMap<>(data.stages));

    public static final SavedDataType<TutorialSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("berongsmp", "tutorial"),
        TutorialSavedData::new,
        CODEC
    );

    private final Map<UUID, TutorialStage> stages = new HashMap<>();

    public TutorialStage getStage(UUID uuid) {
        return stages.getOrDefault(uuid, TutorialStage.NOT_STARTED);
    }

    public void setStage(UUID uuid, TutorialStage stage) {
        stages.put(uuid, stage);
        setDirty();
    }

    public static TutorialSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
