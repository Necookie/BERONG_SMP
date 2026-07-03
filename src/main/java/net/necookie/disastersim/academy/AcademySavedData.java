package net.necookie.disastersim.academy;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.necookie.disastersim.BerongSMP;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persists each player's {@link AcademyProgress} to the world's data storage. Pattern-cloned from
 * {@code tutorial/TutorialSavedData} (same {@code SavedData}+{@code Codec} idiom), but stores a
 * compound per-player record instead of a single enum — the Academy's 4 NPCs each progress
 * independently, which a single flat stage value can't represent.
 */
public class AcademySavedData extends SavedData {

    public static final Codec<AcademySavedData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, AcademyProgress.CODEC)
            .xmap(map -> {
                AcademySavedData data = new AcademySavedData();
                data.progress.putAll(map);
                return data;
            }, data -> new HashMap<>(data.progress));

    public static final SavedDataType<AcademySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(BerongSMP.MODID, "academy"),
            AcademySavedData::new,
            CODEC
    );

    private final Map<UUID, AcademyProgress> progress = new HashMap<>();

    /** Read-only lookup — always returns a live object, creating a fresh NOT_STARTED entry if needed. */
    public AcademyProgress get(UUID uuid) {
        return progress.computeIfAbsent(uuid, u -> new AcademyProgress());
    }

    /** The only mutation entry point — every change goes through here so {@code setDirty()} is never forgotten. */
    public void mutate(UUID uuid, Consumer<AcademyProgress> mutator) {
        mutator.accept(get(uuid));
        setDirty();
    }

    /** Wipes the player back to a fresh NOT_STARTED state (used on a Capt. Morfe fail-retry). */
    public void reset(UUID uuid) {
        mutate(uuid, AcademyProgress::resetAll);
    }

    public static AcademySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
