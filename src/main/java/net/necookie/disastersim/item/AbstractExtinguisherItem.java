package net.necookie.disastersim.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Shared behaviour for the handheld fire extinguishers (ABC dry-chemical and CO2). The two units
 * differ only in which blocks they suppress, their particle/sound flavour, and the telemetry they
 * emit — everything else (the safety-pin gate, the held-spray ray geometry, durability drain, and
 * the charge tooltip) is identical and lives here.
 *
 * <h2>Use flow</h2>
 * First right-click pulls the safety pin (one-time, persisted in {@link DataComponents#CUSTOM_DATA}).
 * Subsequent right-click-and-hold discharges: every use-tick {@link #sprayServer} casts a short cone
 * of sample points out to {@link #SPRAY_RANGE} and asks {@link #extinguishAt} to suppress each one.
 *
 * <h2>Subclass hooks</h2>
 * {@link #extinguishAt} (block-specific suppression), {@link #onSprayResolved} (scoring + telemetry),
 * {@link #spawnSprayParticlesServer}/{@link #spawnSprayParticlesClient}, {@link #sprayPitch}, and the
 * optional {@link #pinPulledMessage}/{@link #onPinPulled}/{@link #onSprayStart} flavour hooks.
 */
public abstract class AbstractExtinguisherItem extends Item {

    /** Maximum reach of the spray cone, in blocks. */
    protected static final double SPRAY_RANGE = 5.5D;
    /** Held-use cap; effectively "until released" (1 hour at 20 tps). */
    protected static final int MAX_USE_TICKS = 72_000;
    /** Damage-from-full at which the unit starts sputtering (weaker, faster-pulsing spray). */
    private static final int SPUTTER_MARGIN = 60;
    private static final String TAG_PIN_PULLED = "pin_pulled";

    /** Number of steps along the spray axis, and the per-step lateral/vertical sample offsets. */
    private static final int SPRAY_STEPS = 7;

    protected AbstractExtinguisherItem(Properties properties) {
        super(properties);
    }

    // ── Safety-pin state (persisted on the stack) ───────────────────────────

    public static boolean isPinPulled(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        return data.copyTag().getBoolean(TAG_PIN_PULLED).orElse(false);
    }

    protected static void pullPin(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TAG_PIN_PULLED, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // ── Item lifecycle ──────────────────────────────────────────────────────

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // First click only arms the unit; it does not yet discharge.
        if (!isPinPulled(stack)) {
            if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
            pullPin(stack);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.sendSystemMessage(Component.literal(pinPulledMessage()));
            if (player instanceof ServerPlayer sp) onPinPulled(sp);
            return InteractionResult.SUCCESS;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;
        if (!isPinPulled(stack)) {
            user.stopUsingItem();
            return;
        }

        boolean sputtering = stack.getDamageValue() >= stack.getMaxDamage() - SPUTTER_MARGIN;
        // A near-empty unit pulses (skips every other tick) for a dying-spray feel.
        if (sputtering && remainingUseTicks % 2 != 0) return;

        boolean playSound = remainingUseTicks % 6 == 0;

        if (level instanceof ServerLevel serverLevel) {
            sprayServer(serverLevel, player, stack, playSound, sputtering);
            if (remainingUseTicks == MAX_USE_TICKS) onSprayStart(player);
        } else {
            sprayClient(level, player, playSound, sputtering);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return MAX_USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    // ── Spray ───────────────────────────────────────────────────────────────

    private void sprayServer(ServerLevel level, Player user, ItemStack stack,
                             boolean playSound, boolean sputtering) {
        Vec3 look   = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(look.scale(0.75D));
        Vec3 side   = orthogonalSide(look);
        Vec3 up     = side.cross(look).normalize();

        // Sample a small cross of points at each step so the spray "fills" a cone, not a line.
        boolean anyHit = false;
        for (int i = 1; i <= SPRAY_STEPS; i++) {
            double distance = i * (SPRAY_RANGE / SPRAY_STEPS);
            Vec3 center = origin.add(look.scale(distance));
            anyHit |= extinguishAt(level, BlockPos.containing(center), user);
            anyHit |= extinguishAt(level, BlockPos.containing(center.add(side.scale( 0.35D))), user);
            anyHit |= extinguishAt(level, BlockPos.containing(center.add(side.scale(-0.35D))), user);
            anyHit |= extinguishAt(level, BlockPos.containing(center.add(up.scale(  0.25D))), user);
            anyHit |= extinguishAt(level, BlockPos.containing(center.add(up.scale( -0.20D))), user);
        }

        // Scoring + telemetry are read before the durability tick below, matching legacy ordering.
        if (user instanceof ServerPlayer sp) onSprayResolved(level, sp, stack, anyHit);

        spawnSprayParticlesServer(level, origin, look, side, up, sputtering);

        if (playSound) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F,
                    sprayPitch(sputtering, level.getRandom()));
        }

        drainCharge(stack);
    }

    private void sprayClient(Level level, Player user, boolean playSound, boolean sputtering) {
        Vec3 look   = user.getViewVector(1.0F).normalize();
        Vec3 origin = user.getEyePosition().add(look.scale(0.75D));
        spawnSprayParticlesClient(level, origin, look, sputtering);

        if (playSound) {
            level.playLocalSound(user.getX(), user.getY(), user.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F,
                    sprayPitch(sputtering, level.getRandom()), false);
        }
    }

    /** Returns a normalized horizontal-ish vector perpendicular to {@code look} (stable near vertical). */
    private static Vec3 orthogonalSide(Vec3 look) {
        Vec3 side = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        return side.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
    }

    /** Advances durability by one, destroying the stack when empty. */
    private static void drainCharge(ItemStack stack) {
        int newDamage = stack.getDamageValue() + 1;
        if (newDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    // ── Shared tooltip helpers ──────────────────────────────────────────────

    /** Appends the "[PIN IN]/[PIN PULLED]" status line; {@code pulledText} is the armed-state hint. */
    protected static void appendPinStatusTooltip(ItemStack stack, Consumer<Component> tooltip, String pulledText) {
        tooltip.accept(Component.literal(isPinPulled(stack)
                ? "§a[PIN PULLED] " + pulledText
                : "§c[PIN IN] Right-click to pull the pin first."));
    }

    /** Appends the remaining-charge percentage line, reddened when nearly empty. */
    protected static void appendChargeTooltip(ItemStack stack, Consumer<Component> tooltip) {
        int maxDmg = stack.getMaxDamage();
        if (maxDmg <= 0) return;
        int charge = (int) ((1.0 - (double) stack.getDamageValue() / maxDmg) * 100);
        tooltip.accept(Component.literal(charge < 20
                ? "§cAlmost empty! Charge: " + charge + "%"
                : "§7Charge: " + charge + "%"));
    }

    /** Counts other players within {@code radius} blocks of {@code self}. */
    protected static int countNearbyPlayers(ServerLevel level, ServerPlayer self, double radius) {
        AABB box = new AABB(
                self.getX() - radius, self.getY() - radius, self.getZ() - radius,
                self.getX() + radius, self.getY() + radius, self.getZ() + radius);
        return level.getEntitiesOfClass(Player.class, box, p -> !p.getUUID().equals(self.getUUID())).size();
    }

    // ── Subclass hooks ──────────────────────────────────────────────────────

    /** Suppresses the block at {@code pos} if this extinguisher type handles it; returns true on a hit. */
    protected abstract boolean extinguishAt(ServerLevel level, BlockPos pos, Player user);

    /** Emits scoring + telemetry for one server spray tick (called before the durability drain). */
    protected abstract void onSprayResolved(ServerLevel level, ServerPlayer player, ItemStack stack, boolean anyHit);

    protected abstract void spawnSprayParticlesServer(ServerLevel level, Vec3 origin, Vec3 look,
                                                      Vec3 side, Vec3 up, boolean sputtering);

    protected abstract void spawnSprayParticlesClient(Level level, Vec3 origin, Vec3 look, boolean sputtering);

    /** Spray sound pitch; CO2 hisses higher than dry-chemical. */
    protected abstract float sprayPitch(boolean sputtering, RandomSource random);

    /** Chat line shown when the pin is first pulled. */
    protected abstract String pinPulledMessage();

    /** Called server-side the moment the pin is pulled (e.g. to log a telemetry event). No-op by default. */
    protected void onPinPulled(ServerPlayer player) {}

    /** Called server-side on the first tick of a held spray. No-op by default. */
    protected void onSprayStart(Player player) {}
}
