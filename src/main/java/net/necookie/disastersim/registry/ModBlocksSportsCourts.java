package net.necookie.disastersim.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.necookie.disastersim.block.BadmintonNetMeshBlock;
import net.necookie.disastersim.block.BadmintonNetPostBlock;
import net.necookie.disastersim.block.BasketballHoopBlock;
import net.necookie.disastersim.block.BasketballHoopPostBlock;
import net.necookie.disastersim.block.BasketballPoleSegmentBlock;
import net.necookie.disastersim.block.CourtLineBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Badminton/basketball court-building block registrations — split out of {@link ModBlocks} (see
 * its class javadoc). Package-private: {@link ModBlocks} re-exports every field below in the same
 * declared order.
 */
final class ModBlocksSportsCourts {

    private ModBlocksSportsCourts() {}

    /** Smooth white auto-connecting court-marking line — see {@link CourtLineBlock}. */
    static final DeferredBlock<CourtLineBlock> COURT_LINE = ModBlocks.BLOCKS.registerBlock(
            "court_line", CourtLineBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.2f, 0.5f).sound(SoundType.WOOL).noOcclusion());

    /** Badminton net anchor post ("stitch") — see {@link BadmintonNetPostBlock}. */
    static final DeferredBlock<BadmintonNetPostBlock> BADMINTON_NET_POST = ModBlocks.BLOCKS.registerBlock(
            "badminton_net_post", BadmintonNetPostBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0f, 2.0f).sound(SoundType.METAL).noOcclusion());

    /** Auto-filled badminton net panel — see {@link BadmintonNetMeshBlock}. */
    static final DeferredBlock<BadmintonNetMeshBlock> BADMINTON_NET_MESH = ModBlocks.BLOCKS.registerBlock(
            "badminton_net_mesh", BadmintonNetMeshBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.SNOW)
                    .strength(0.3f, 0.5f).sound(SoundType.WOOL).noOcclusion());

    /** Basketball hoop stand base (bottom anchor of the expandable pole) — see {@link BasketballHoopPostBlock}. */
    static final DeferredBlock<BasketballHoopPostBlock> BASKETBALL_HOOP_POST = ModBlocks.BLOCKS.registerBlock(
            "basketball_hoop_post", BasketballHoopPostBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Auto-filled basketball pole segment — see {@link BasketballPoleSegmentBlock}. */
    static final DeferredBlock<BasketballPoleSegmentBlock> BASKETBALL_POLE = ModBlocks.BLOCKS.registerBlock(
            "basketball_pole", BasketballPoleSegmentBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.0f, 4.0f).sound(SoundType.METAL).noOcclusion());

    /** Basketball backboard + rim + net (top anchor of the expandable pole) — see {@link BasketballHoopBlock}. */
    static final DeferredBlock<BasketballHoopBlock> BASKETBALL_HOOP = ModBlocks.BLOCKS.registerBlock(
            "basketball_hoop", BasketballHoopBlock::new,
            () -> Block.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5f, 3.0f).sound(SoundType.METAL).noOcclusion());
}
