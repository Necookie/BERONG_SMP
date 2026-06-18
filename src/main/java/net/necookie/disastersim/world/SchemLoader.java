package net.necookie.disastersim.world;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.necookie.disastersim.BerongSMP;

import java.io.InputStream;

/**
 * Places a Sponge Schematic v2/v3 (.schem) file into the world at a given origin.
 * Supports 0–3 counter-clockwise 90° rotations; directional block states are
 * also rotated so stairs, doors, etc. face the correct direction.
 */
public class SchemLoader implements StructurePlacer {

    private final Identifier resourcePath;
    /** Number of CCW 90° rotations to apply (0–3). */
    private final int ccwRotations;

    public SchemLoader(Identifier resourcePath) {
        this(resourcePath, 0);
    }

    public SchemLoader(Identifier resourcePath, int ccwRotations) {
        this.resourcePath = resourcePath;
        this.ccwRotations = ((ccwRotations % 4) + 4) % 4;
    }

    @Override
    public boolean place(ServerLevel level, BlockPos origin) {
        var resourceOpt = level.getServer().getResourceManager().getResource(resourcePath);
        if (resourceOpt.isEmpty()) {
            BerongSMP.LOGGER.error("Schematic not found: {}", resourcePath);
            return false;
        }

        try (InputStream is = resourceOpt.get().open()) {
            CompoundTag root = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());

            // Sponge v3 wraps everything under a "Schematic" key; v2 uses the root directly.
            CompoundTag nbt = root.getCompound("Schematic").orElse(root);

            int version = nbt.getInt("Version").orElse(0);
            if (version != 2 && version != 3) {
                BerongSMP.LOGGER.error("Unsupported schematic version {} in {}", version, resourcePath);
                return false;
            }

            int width  = nbt.getShort("Width").map(s  -> s & 0xFFFF).orElse(0);
            int height = nbt.getShort("Height").map(s -> s & 0xFFFF).orElse(0);
            int length = nbt.getShort("Length").map(s -> s & 0xFFFF).orElse(0);

            // v3 nests palette + data under a "Blocks" sub-compound; v2 keeps them at top level.
            CompoundTag blocksTag = version == 3
                    ? nbt.getCompound("Blocks").orElse(nbt)
                    : nbt;

            CompoundTag paletteTag = blocksTag.getCompound("Palette").orElse(new CompoundTag());

            int paletteMax = blocksTag.getInt("PaletteMax")
                    .or(() -> nbt.getInt("PaletteMax"))
                    .orElse(paletteTag.size() + 1);

            BlockState[] palette = new BlockState[paletteMax];
            var blockRegistry = level.registryAccess().lookupOrThrow(Registries.BLOCK);
            for (String key : paletteTag.keySet()) {
                int idx = paletteTag.getInt(key).orElse(0);
                try {
                    palette[idx] = BlockStateParser.parseForBlock(blockRegistry, key, false).blockState();
                } catch (Exception e) {
                    BerongSMP.LOGGER.warn("Unknown block state '{}' in {}, using air", key, resourcePath);
                    palette[idx] = Blocks.AIR.defaultBlockState();
                }
            }

            // v3 calls the byte array "Data"; v2 calls it "BlockData"
            byte[] raw = blocksTag.getByteArray("Data")
                    .or(() -> blocksTag.getByteArray("BlockData"))
                    .orElse(new byte[0]);

            int total = width * height * length;
            int[] indices = new int[total];
            int bytePos = 0;
            for (int i = 0; i < total; i++) {
                int value = 0, shift = 0;
                while (bytePos < raw.length) {
                    byte b = raw[bytePos++];
                    value |= (b & 0x7F) << shift;
                    if ((b & 0x80) == 0) break;
                    shift += 7;
                }
                indices[i] = value;
            }

            // Map ccwRotations to Minecraft's Rotation enum for block-state transforms.
            Rotation mcRotation = switch (ccwRotations) {
                case 1 -> Rotation.COUNTERCLOCKWISE_90;
                case 2 -> Rotation.CLOCKWISE_180;
                case 3 -> Rotation.CLOCKWISE_90;
                default -> Rotation.NONE;
            };

            // XZY order: index = (y*length + z)*width + x
            int i = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        BlockState state = palette[indices[i++]];
                        if (state == null) state = Blocks.AIR.defaultBlockState();

                        // Rotate the block state (handles stairs, doors, etc.)
                        state = state.rotate(mcRotation);

                        // Rotate the placement offset to match.
                        // CCW 90°:  (x,z) → (z, W-1-x),  new W=L, new L=W
                        // 180°:     (x,z) → (W-1-x, L-1-z)
                        // CW 90°:   (x,z) → (L-1-z, x),  new W=L, new L=W
                        int px, pz;
                        switch (ccwRotations) {
                            case 1  -> { px = z;         pz = width - 1 - x; }
                            case 2  -> { px = width - 1 - x; pz = length - 1 - z; }
                            case 3  -> { px = length - 1 - z; pz = x; }
                            default -> { px = x;         pz = z; }
                        }
                        level.setBlock(origin.offset(px, y, pz), state, 2);
                    }
                }
            }

            BerongSMP.LOGGER.info("Placed schematic {} v{} ({}x{}x{}) rot={}CCW at {}",
                    resourcePath, version, width, height, length, ccwRotations * 90, origin);
            return true;

        } catch (Exception e) {
            BerongSMP.LOGGER.error("Failed to load schematic {}: {}", resourcePath, e.getMessage());
            return false;
        }
    }
}
