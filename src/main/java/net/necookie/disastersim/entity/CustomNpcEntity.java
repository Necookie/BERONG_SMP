package net.necookie.disastersim.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

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
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                // Default FOLLOW_RANGE (16) also caps the pathfinding node budget — too small for
                // an escort route spanning the Academy's Movement School, and the root cause of
                // Cruz "getting lost" on longer legs.
                .add(Attributes.FOLLOW_RANGE, 48.0)
                // The jump zone's hurdles are 1 block tall; 1.1 lets an escorting NPC walk them
                // without needing a jump. Only escorting NPCs ever move, and Cruz never targets
                // past the Go/Stop staging line, so this can't carry her over the tunnel walls.
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_NPC_TYPE, NpcType.SGT_REYES.id);
    }

    // --- Look-at-player behavior -------------------------------------------
    /** Players within this many blocks are tracked by the NPC's gaze. */
    private static final double LOOK_RANGE = 10.0;
    /** Max degrees the head turns per tick while escorting/walking — lower is slower and smoother. */
    private static final float HEAD_TURN_SPEED = 12.0f;
    /**
     * Head turn speed while stationary (not escorting) — noticeably snappier than
     * {@link #HEAD_TURN_SPEED}, since an idle NPC that's just standing and talking should react to
     * the player right away instead of easing over like she does mid-walk.
     */
    private static final float HEAD_TURN_SPEED_IDLE = 28.0f;
    /** Head won't deviate more than this many degrees from the body's facing (no owl spins). */
    private static final float MAX_HEAD_YAW = 60.0f;
    /** Max up/down head pitch in degrees. */
    private static final float MAX_HEAD_PITCH = 40.0f;
    /** Degrees per tick the body swings once the desired gaze exceeds the head cone — slower than
     *  the head so the turn reads as a natural "head leads, body follows". */
    private static final float BODY_TURN_SPEED = 6.0f;
    /** Body turn speed while stationary — same faster-when-idle treatment as {@link #HEAD_TURN_SPEED_IDLE}. */
    private static final float BODY_TURN_SPEED_IDLE = 14.0f;

    /** Speed used for the occasional tiny idle step on NpcTypes with minimalWander=true. */
    private static final double WANDER_SPEED = 0.35;

    @Override
    protected void registerGoals() {
        // Keeps a moving NPC afloat instead of sinking/drowning if an escort route ever crosses
        // water. Only ticks while noAi=false (escorting or minimalWander), so static NPCs pay
        // nothing for it.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // No vanilla AI beyond this — the gaze is driven manually in tick(). This goal only
        // ever runs for NpcTypes with minimalWander=true, since setNpcType() is the sole place
        // that flips setNoAi(false) for those, and noAi mobs never tick their goal selector.
        this.goalSelector.addGoal(1, new MinimalWanderGoal(this, WANDER_SPEED));
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
            // When the player is beyond the head cone (e.g. standing behind), swing the whole body
            // around toward them — head tracks first, body follows, so a static instructor turns
            // to face whoever they're talking to instead of side-eyeing at a 60° clamp. Never
            // while escorting: MoveControl owns body yaw during pathfinding.
            if (!escorting) {
                float bodyDelta = Mth.wrapDegrees(rawYaw - this.yBodyRot);
                if (Math.abs(bodyDelta) > MAX_HEAD_YAW) {
                    float newBody = approachDegrees(this.yBodyRot, rawYaw, BODY_TURN_SPEED_IDLE);
                    this.setYBodyRot(newBody);
                    this.setYRot(newBody); // keep entity yaw in sync so the turn syncs/persists
                }
            }
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
        // Turn faster whenever she isn't actively walking somewhere (escorting) — a stationary NPC
        // reacts to the player right away instead of the slower, smoother mid-walk ease.
        float headSpeed = escorting ? HEAD_TURN_SPEED : HEAD_TURN_SPEED_IDLE;
        this.setYHeadRot(approachDegrees(this.getYHeadRot(), desiredYaw, headSpeed));
        this.setXRot(approachDegrees(this.getXRot(), desiredPitch, headSpeed));
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
        this.setNoAi(!type.minimalWander);
        var movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(type.minimalWander ? 0.05 : 0.0);
        }
    }

    /** Walk speed (attribute base value) used while {@link #setEscorting} is active. */
    private static final double ESCORT_SPEED = 0.4;

    private boolean escorting = false;

    /**
     * Toggles real pathfinding movement on/off for NPCs that need to actively walk somewhere
     * (e.g. Officer Cruz escorting a player through the Academy's Movement School), independent of
     * {@link NpcType#minimalWander}'s idle-shuffle behavior. Idempotent — a no-op if already in the
     * requested state, so callers driving this from a per-tick loop across multiple players don't
     * needlessly reset synced entity data/attributes every tick.
     *
     * <p>{@code setNoAi(false)} is what actually lets {@code serverAiStep()} (goal selector,
     * {@code PathNavigation}, {@code MoveControl}) tick at all; the movement speed attribute is
     * bumped separately since {@code getNavigation().moveTo(...)}'s speed parameter scales that
     * attribute's base value rather than replacing it — leaving it at the default {@code 0.0} for
     * non-wander NpcTypes would mean no amount of {@code moveTo} ever actually moves them.
     */
    public void setEscorting(boolean escorting) {
        if (this.escorting == escorting) return;
        this.escorting = escorting;
        this.setNoAi(!escorting);
        var movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(escorting ? ESCORT_SPEED : (getNpcType().minimalWander ? 0.05 : 0.0));
        }
        // Configure navigation for indoor escort routes each time escort mode turns on: door
        // passage and an explicit path budget matching FOLLOW_RANGE (createAttributes) so long
        // legs through the building don't get truncated mid-route.
        if (escorting && this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.getNodeEvaluator().setCanPassDoors(true);
            nav.setRequiredPathLength(48.0f);
            nav.setMaxVisitedNodesMultiplier(4.0f);
        }
        if (!escorting) {
            // Without this, whatever path was in progress stays loaded (noAi just stops it
            // ticking) and silently resumes the next time escorting turns back on, for up to a
            // full re-issue cadence before a fresh moveTo call replaces it.
            this.getNavigation().stop();
        }
    }

    /**
     * Shrunk hitbox used while {@link #crouchingForObstacle} is set, matching vanilla's own
     * crouching dimensions (0.6×1.5) — just short of the Academy's crouch-tunnel clearance (1.5
     * blocks under a top-slab ceiling), which her normal 0.6×1.8 standing box can never fit under.
     * Unlike a player, a {@code Mob}'s bounding box doesn't change with {@link Pose} on its own —
     * this override plus {@link #refreshDimensions()} in {@link #setCrouchingForObstacle} is what
     * actually shrinks her hitbox, matching {@code DuckCoverHoldManager.allowCrawlUnderTable}'s
     * player-side crouch trick but adapted for a non-player {@code Mob}.
     */
    private static final EntityDimensions CROUCH_DIMENSIONS = EntityDimensions.scalable(0.6f, 1.5f);

    private boolean crouchingForObstacle = false;

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return crouchingForObstacle ? CROUCH_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    /**
     * Toggles the shrunk crouch hitbox — called every escort tick by whichever room manager is
     * driving her (e.g. {@code CruzRoomManager} while she's in/near the Go/Stop tunnel), based on
     * her current position, so pathfinding through a low-ceiling obstacle becomes a normal
     * reachable path instead of the provably-unreachable one the poof-recovery used to have to
     * bail out of.
     */
    public void setCrouchingForObstacle(boolean crouching) {
        if (this.crouchingForObstacle == crouching) return;
        this.crouchingForObstacle = crouching;
        this.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
        this.refreshDimensions();
    }

    // -----------------------------------------------------------------------
    // Escort bookkeeping — owned by the entity itself, not by whichever room manager is driving it
    // -----------------------------------------------------------------------

    /**
     * Per-tick escort pathing state, formerly kept as {@code static} fields on {@code
     * CruzRoomManager} (the only current caller). Living on the manager class meant this state
     * described "the escort in progress" rather than "this specific NPC" — harmless while exactly
     * one {@code OFFICER_CRUZ} instance exists (duplicates are swept on boot and every tick, see
     * {@code NewTutBuildingManager.sweepStrayCruz}), but a latent trap if that invariant were ever
     * violated: two entities would silently corrupt each other's stuck-cycle/target bookkeeping.
     * Moving it here means it travels with the object it actually describes. This does NOT enable
     * escorting two students at once — there is still exactly one physical NPC, so a second student
     * in the same room still isn't escorted (an already-accepted limitation, not a bug this closes).
     */
    private long nextEscortMoveTick;
    private Vec3 lastEscortPos = Vec3.ZERO;
    private Vec3 lastEscortTarget;
    private int escortStuckCycles;
    private long lastTooFarNudgeTick = Long.MIN_VALUE;

    public long getNextEscortMoveTick() { return nextEscortMoveTick; }
    public void setNextEscortMoveTick(long tick) { this.nextEscortMoveTick = tick; }
    public Vec3 getLastEscortPos() { return lastEscortPos; }
    public void setLastEscortPos(Vec3 pos) { this.lastEscortPos = pos; }
    public Vec3 getLastEscortTarget() { return lastEscortTarget; }
    public void setLastEscortTarget(Vec3 target) { this.lastEscortTarget = target; }
    public int getEscortStuckCycles() { return escortStuckCycles; }
    public void setEscortStuckCycles(int cycles) { this.escortStuckCycles = cycles; }
    public long getLastTooFarNudgeTick() { return lastTooFarNudgeTick; }
    public void setLastTooFarNudgeTick(long tick) { this.lastTooFarNudgeTick = tick; }

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
