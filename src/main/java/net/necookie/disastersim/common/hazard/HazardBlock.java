package net.necookie.disastersim.common.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Base for symmetric (no-facing) hazard prop blocks. Adds the {@code hazardous} (developing
 * danger) and {@code on_fire} (actually burning) boolean states, wires {@link #animateTick} to
 * call {@link #spawnHazardParticles} and intensify while on fire, and offers a bare-hand
 * "prevention" right-click that resets a hazardous-but-not-yet-burning prop back to safe.
 */
public abstract class HazardBlock extends Block {

    public static final BooleanProperty HAZARDOUS = BooleanProperty.create("hazardous");
    public static final BooleanProperty ON_FIRE = BooleanProperty.create("on_fire");

    protected HazardBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(HAZARDOUS, false).setValue(ON_FIRE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAZARDOUS, ON_FIRE);
    }

    /** Emit client-side particles that signal the hazardous state. Called every animateTick. */
    protected abstract void spawnHazardParticles(Level level, BlockPos pos, BlockState state, RandomSource rand);

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(HAZARDOUS)) return;
        spawnHazardParticles(level, pos, state, rand);
        if (state.getValue(ON_FIRE)) {
            spawnHazardParticles(level, pos, state, rand);
            if (rand.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0, 0.02, 0);
            }
        }
    }

    /**
     * Bare-hand "prevention" action: fixing a hazard while it's merely hazardous (unplug the
     * frayed cord, clear the vent, etc.) before it becomes a real fire. Once {@code on_fire} is
     * true it's too late for a bare-handed fix — an extinguisher (or evacuation) is required.
     */
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(HAZARDOUS) && !state.getValue(ON_FIRE)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(HAZARDOUS, false), 3);
                level.levelEvent(null, 1009, pos, 0);
                player.sendSystemMessage(Component.literal(preventMessage()));
                if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                    HazardManager.onManualPrevention(serverLevel, serverPlayer, pos, state);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Chat flavor text shown when a player prevents this hazard via bare-hand right-click. */
    public String preventMessage() {
        return "§a✔ Prevented! You fixed it before it could catch fire.";
    }

    /** Ticks a hazardous prop stays active before its failure consequence fires. Override to tune pacing. */
    public int failureDelayTicks() {
        return 300;
    }

    /** Chat flavor text broadcast to the player when this prop's failure consequence triggers. */
    public String failureMessage() {
        return "§c⚠ A neglected hazard just started a fire!";
    }

    /** World mutation performed when this prop is left hazardous for {@link #failureDelayTicks()}. */
    public void onHazardFailure(Level level, BlockPos pos, BlockState state) {
        igniteAdjacent(level, pos, 1);
    }

    /**
     * Lights up to {@code maxBlocks} adjacent air blocks on fire, never a block a player is
     * standing in/near. Returns the positions actually lit (callers with a live
     * {@code SimulationSession} use this to log fire-spread rows — see
     * {@code SimulationSession#bufferFireLogRow}); ignore the return value if unneeded.
     */
    public static List<BlockPos> igniteAdjacent(Level level, BlockPos pos, int maxBlocks) {
        List<BlockPos> lit = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (lit.size() >= maxBlocks) break;
            BlockPos target = pos.relative(dir);
            if (level.getBlockState(target).isAir() && !isPlayerNear(level, target) && !isFrameNear(level, target)) {
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                lit.add(target.immutable());
            }
        }
        return lit;
    }

    /**
     * Lights up to {@code maxBlocks} air blocks within a horizontal {@code radius} — for explosive
     * failures. Returns the positions actually lit, same convention as {@link #igniteAdjacent}.
     */
    public static List<BlockPos> igniteRadius(Level level, BlockPos pos, int radius, int maxBlocks) {
        List<BlockPos> lit = new ArrayList<>();
        for (BlockPos target : BlockPos.betweenClosed(
                pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
            if (lit.size() >= maxBlocks) break;
            if (level.getBlockState(target).isAir() && !isPlayerNear(level, target) && !isFrameNear(level, target)) {
                level.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                lit.add(target.immutable());
            }
        }
        return lit;
    }

    /**
     * Finds the {@code maxTargets} nearest {@code BlockTags.PLANKS} blocks within
     * {@code searchRadius} of {@code origin} and lights an adjacent air cell next to each — real
     * fuel for vanilla fire to consume and spread through, instead of a lone fire block floating
     * with nothing flammable nearby to catch (which can fizzle out immediately on a stone/tile
     * floor). Falls back to {@link #igniteAdjacent} if no planks are found in range, so a failure
     * always visibly starts a fire even in an all-non-flammable room.
     */
    public static List<BlockPos> igniteNearestFlammable(Level level, BlockPos origin, int maxTargets, int searchRadius) {
        List<BlockPos> planks = new ArrayList<>();
        for (BlockPos check : BlockPos.betweenClosed(
                origin.offset(-searchRadius, -searchRadius, -searchRadius),
                origin.offset(searchRadius, searchRadius, searchRadius))) {
            if (level.getBlockState(check).is(BlockTags.PLANKS)) {
                planks.add(check.immutable());
            }
        }
        if (planks.isEmpty()) {
            return igniteAdjacent(level, origin, maxTargets);
        }
        planks.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
        List<BlockPos> lit = new ArrayList<>();
        for (BlockPos plank : planks) {
            if (lit.size() >= maxTargets) break;
            BlockPos ignited = igniteAdjacentTo(level, plank);
            if (ignited != null) lit.add(ignited);
        }
        // Every nearest plank's neighbors happened to be occupied/non-air — still guarantee a fire.
        if (lit.isEmpty()) return igniteAdjacent(level, origin, maxTargets);
        return lit;
    }

    /** Lights the first available adjacent air cell touching {@code target}. Returns the lit position, or null. */
    private static BlockPos igniteAdjacentTo(Level level, BlockPos target) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = target.relative(dir);
            if (level.getBlockState(adj).isAir() && !isPlayerNear(level, adj) && !isFrameNear(level, adj)) {
                level.setBlockAndUpdate(adj, Blocks.FIRE.defaultBlockState());
                return adj.immutable();
            }
        }
        return null;
    }

    /**
     * True if any player occupies or is standing close enough to {@code pos} that placing fire
     * there would touch them — vanilla fire both damages and ignites any entity standing in it, and
     * nothing here previously checked for that, which is what let the Academy's scripted hazard
     * failures set the player "randomly" alight while they were right there defusing the prop.
     */
    protected static boolean isPlayerNear(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(0.6)).isEmpty();
    }

    /**
     * True if an item frame (or glow item frame — a subclass) occupies {@code pos}. Item frames
     * render inside what {@code BlockState.isAir()} reports as an empty cell (see
     * {@code SchemLoader}'s own item-frame placement model), so every ignite helper here was
     * treating a frame's own floating position as fair game for a fresh fire block — vanilla fire
     * damages any entity touching it, and a damaged {@code ItemFrame} instantly pops its held item
     * out as a dropped {@code ItemEntity} (then breaks the frame itself on a second hit). New Sim
     * Building 2.0's ~24 extinguisher-holding frames and hazard props are scattered through the same
     * building volume, so this collision was routine, not rare — see
     * docs/history/item-frame-fire-collision-fix-log-2026-07-20.md for the full trace. No inflate
     * needed (unlike {@link #isPlayerNear}) since a frame's bounding box is already contained within
     * its own block cell.
     */
    protected static boolean isFrameNear(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(ItemFrame.class, new AABB(pos)).isEmpty();
    }
}
