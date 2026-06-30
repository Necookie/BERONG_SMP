package net.necookie.disastersim.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
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

    // --- Look-at-player behavior -------------------------------------------
    /** Players within this many blocks are tracked by the NPC's gaze. */
    private static final double LOOK_RANGE = 10.0;
    /** Max degrees the head turns per tick — lower is slower and smoother. */
    private static final float HEAD_TURN_SPEED = 12.0f;
    /** Head won't deviate more than this many degrees from the body's facing (no owl spins). */
    private static final float MAX_HEAD_YAW = 60.0f;
    /** Max up/down head pitch in degrees. */
    private static final float MAX_HEAD_PITCH = 40.0f;

    @Override
    protected void registerGoals() {
        // No vanilla AI — the gaze is driven manually in tick() so the NPC stays put.
    }

    @Override
    public void tick() {
        super.tick();
        // Rotation is computed server-side; the entity tracker syncs it to clients.
        if (this.level().isClientSide()) return;
        updateGaze();
    }

    /** Eases the head (yaw + pitch) toward the nearest player, returning to rest when alone. */
    private void updateGaze() {
        Player target = this.level().getNearestPlayer(this, LOOK_RANGE);
        float desiredYaw;
        float desiredPitch;
        if (target != null) {
            double dx = target.getX()    - this.getX();
            double dz = target.getZ()    - this.getZ();
            double dy = target.getEyeY() - this.getEyeY();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float rawYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            // Keep the head within a natural cone of the body so it never snaps fully around.
            desiredYaw = this.yBodyRot
                    + Mth.clamp(Mth.wrapDegrees(rawYaw - this.yBodyRot), -MAX_HEAD_YAW, MAX_HEAD_YAW);
            desiredPitch = Mth.clamp(
                    (float) (-(Mth.atan2(dy, horizontal) * (180.0 / Math.PI))),
                    -MAX_HEAD_PITCH, MAX_HEAD_PITCH);
        } else {
            desiredYaw = this.yBodyRot; // ease back to a resting forward gaze
            desiredPitch = 0.0f;
        }
        this.setYHeadRot(approachDegrees(this.getYHeadRot(), desiredYaw, HEAD_TURN_SPEED));
        this.setXRot(approachDegrees(this.getXRot(), desiredPitch, HEAD_TURN_SPEED));
    }

    /** Steps {@code current} toward {@code target} by at most {@code maxStep} degrees, the short way. */
    private static float approachDegrees(float current, float target, float maxStep) {
        return current + Mth.clamp(Mth.wrapDegrees(target - current), -maxStep, maxStep);
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
