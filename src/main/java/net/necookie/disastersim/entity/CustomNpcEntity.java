package net.necookie.disastersim.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A static, invulnerable humanoid NPC that displays one of the five BFP instructor skins.
 * NpcType is synced to the client via SynchedEntityData (so the renderer can pick the right
 * skin) and persisted in NBT (so the skin survives world reload).
 */
public class CustomNpcEntity extends Mob {

    private static final EntityDataAccessor<String> DATA_NPC_TYPE =
            SynchedEntityData.defineId(CustomNpcEntity.class, EntityDataSerializers.STRING);

    public CustomNpcEntity(EntityType<? extends CustomNpcEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setSilent(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_NPC_TYPE, NpcType.SGT_REYES.id);
    }

    @Override
    protected void registerGoals() {
        // No AI — static placement NPC
    }

    public NpcType getNpcType() {
        return NpcType.fromId(this.entityData.get(DATA_NPC_TYPE));
    }

    public void setNpcType(NpcType type) {
        this.entityData.set(DATA_NPC_TYPE, type.id);
        this.setCustomName(Component.literal(type.displayName));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("NpcType", getNpcType().id);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setNpcType(NpcType.fromId(input.getStringOr("NpcType", NpcType.SGT_REYES.id)));
    }
}
