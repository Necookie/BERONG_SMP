package net.necookie.disastersim.registration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persists student registration data (name, school ID, section) across server restarts. */
public class RegisteredPlayerData extends SavedData {

    private static final Codec<StudentRegistration> REG_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.STRING.fieldOf("studentName").forGetter(StudentRegistration::studentName),
        Codec.STRING.fieldOf("studentId").forGetter(StudentRegistration::studentId),
        Codec.STRING.fieldOf("section").forGetter(StudentRegistration::section)
    ).apply(inst, StudentRegistration::new));

    public static final Codec<RegisteredPlayerData> CODEC =
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, REG_CODEC)
            .xmap(map -> {
                RegisteredPlayerData d = new RegisteredPlayerData();
                d.registrations.putAll(map);
                return d;
            }, d -> new HashMap<>(d.registrations));

    public static final SavedDataType<RegisteredPlayerData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("berongsmp", "registrations"),
        RegisteredPlayerData::new,
        CODEC
    );

    private final Map<UUID, StudentRegistration> registrations = new HashMap<>();

    public void register(UUID uuid, String name, String studentId, String section) {
        registrations.put(uuid, new StudentRegistration(name, studentId, section));
        setDirty();
    }

    public boolean isRegistered(UUID uuid) {
        return registrations.containsKey(uuid);
    }

    public StudentRegistration get(UUID uuid) {
        return registrations.get(uuid);
    }

    public void reset(UUID uuid) {
        registrations.remove(uuid);
        setDirty();
    }

    public static RegisteredPlayerData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
