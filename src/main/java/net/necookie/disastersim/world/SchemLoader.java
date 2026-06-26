package net.necookie.disastersim.world;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.necookie.disastersim.BerongSMP;

import java.io.InputStream;

/**
 * Places a Sponge Schematic v2/v3 (.schem) file into the world at a given origin.
 * Supports 0–3 counter-clockwise 90° rotations; directional block states are
 * also rotated so stairs, doors, etc. face the correct direction.
 * Entities (item frames, paintings) are also spawned from the Entities tag.
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

            // Spawn entities (item frames, paintings, etc.) from the Entities tag.
            placeEntities(level, origin, nbt, width, height, length);
            // Apply tile-entity data (sign text, chest contents, etc.) from BlockEntities tag.
            placeBlockEntities(level, origin, nbt, width, height, length);

            return true;

        } catch (Exception e) {
            BerongSMP.LOGGER.error("Failed to load schematic {}: {}", resourcePath, e.getMessage());
            return false;
        }
    }

    private void placeEntities(ServerLevel level, BlockPos origin, CompoundTag nbt,
                                int schemWidth, int schemHeight, int schemLength) {
        // Entities list is at the root nbt level in both Sponge v2 and v3.
        Tag entitiesRaw = nbt.get("Entities");
        if (!(entitiesRaw instanceof ListTag entityList)) {
            BerongSMP.LOGGER.info("[SchemLoader] No Entities tag in {}", resourcePath);
            return;
        }
        if (entityList.isEmpty()) {
            BerongSMP.LOGGER.info("[SchemLoader] Entities tag is empty in {}", resourcePath);
            return;
        }
        BerongSMP.LOGGER.info("[SchemLoader] Found {} entities in {}", entityList.size(), resourcePath);
        // Debug: dump first entity's full NBT so we can see exact field names and values.
        if (!entityList.isEmpty() && entityList.get(0) instanceof CompoundTag first) {
            BerongSMP.LOGGER.info("[SchemLoader] First entity NBT: {}", first);
        }

        // Footprint of the placed schematic after rotation (dimensions swap on 90°/270°).
        int placedW = (ccwRotations % 2 == 0) ? schemWidth  : schemLength;
        int placedL = (ccwRotations % 2 == 0) ? schemLength : schemWidth;

        // Remove existing item frames in the footprint to prevent duplicates on re-place.
        AABB bounds = new AABB(
                origin.getX(), origin.getY() - 1, origin.getZ(),
                origin.getX() + placedW, origin.getY() + schemHeight + 1, origin.getZ() + placedL);
        level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class, bounds, e -> true)
             .forEach(Entity::discard);

        int spawned = 0;
        for (int i = 0; i < entityList.size(); i++) {
            if (!(entityList.get(i) instanceof CompoundTag entityTag)) continue;

            // Sponge uses "Id" (capital I); MC entity NBT uses "id" (lowercase).
            String entityId = getStringTag(entityTag, "Id");
            if (entityId.isEmpty()) entityId = getStringTag(entityTag, "id");
            if (entityId.isEmpty()) {
                BerongSMP.LOGGER.warn("[SchemLoader] Entity {} has no Id, skipping", i);
                continue;
            }

            // Relative position from schematic min corner (WorldEdit stores Pos relative).
            Tag posRaw = entityTag.get("Pos");
            if (!(posRaw instanceof ListTag posList) || posList.size() < 3) {
                BerongSMP.LOGGER.warn("[SchemLoader] Entity {} ({}) has no Pos, skipping", i, entityId);
                continue;
            }

            double relX = tagToDouble(posList.get(0));
            double relY = tagToDouble(posList.get(1));
            double relZ = tagToDouble(posList.get(2));

            // Rotate continuous position offset (no -1 correction — floating-point, not block grid).
            double rotX, rotZ;
            switch (ccwRotations) {
                case 1  -> { rotX = relZ;               rotZ = schemWidth  - relX; }
                case 2  -> { rotX = schemWidth  - relX;  rotZ = schemLength - relZ; }
                case 3  -> { rotX = schemLength - relZ;  rotZ = relX; }
                default -> { rotX = relX;                rotZ = relZ; }
            }

            double worldX = origin.getX() + rotX;
            double worldY = origin.getY() + relY;
            double worldZ = origin.getZ() + rotZ;

            // Build spawn NBT: copy entity data, fix id casing, and write world-space Pos.
            CompoundTag spawnNbt = entityTag.copy();
            spawnNbt.putString("id", entityId);

            ListTag worldPos = new ListTag();
            worldPos.add(DoubleTag.valueOf(worldX));
            worldPos.add(DoubleTag.valueOf(worldY));
            worldPos.add(DoubleTag.valueOf(worldZ));
            spawnNbt.put("Pos", worldPos);

            // Rotate Facing byte: 0=Down,1=Up,2=North,3=South,4=West,5=East
            byte rotatedFacing = 2; // default NORTH
            Tag facingTag = spawnNbt.get("Facing");
            if (facingTag instanceof net.minecraft.nbt.NumericTag facingNum) {
                rotatedFacing = rotateFacingByte((byte) facingNum.intValue(), ccwRotations);
                spawnNbt.putByte("Facing", rotatedFacing);
            }

            BerongSMP.LOGGER.info("[SchemLoader] Spawning {} at world ({}, {}, {})",
                    entityId, (int) worldX, (int) worldY, (int) worldZ);

            // Item frames: Sponge v3 top-level Pos = entity's OWN block (the air block where the
            // frame floats, NOT the wall block). Data.Facing = OUTWARD direction (frame face toward
            // viewer). Wall = entityBlock.relative(outward.getOpposite()).
            // MC 26.x ItemFrame(level, pos, direction): pos = entity block, direction = outward.
            if (entityId.equals("minecraft:item_frame") || entityId.equals("minecraft:glow_item_frame")) {
                int blkX = (int) relX, blkY = (int) relY, blkZ = (int) relZ;
                int rotBX, rotBZ;
                switch (ccwRotations) {
                    case 1  -> { rotBX = blkZ;                    rotBZ = schemWidth  - 1 - blkX; }
                    case 2  -> { rotBX = schemWidth  - 1 - blkX; rotBZ = schemLength - 1 - blkZ; }
                    case 3  -> { rotBX = schemLength - 1 - blkZ; rotBZ = blkX; }
                    default -> { rotBX = blkX;                    rotBZ = blkZ; }
                }

                // Data.Facing = OUTWARD (frame face direction toward viewer) — rotate it directly.
                byte rawFacing = 2;
                var dataOptFrame = entityTag.getCompound("Data");
                if (dataOptFrame.isPresent()) {
                    Tag ft = dataOptFrame.get().get("Facing");
                    if (ft instanceof net.minecraft.nbt.NumericTag fn) rawFacing = (byte) fn.intValue();
                }
                byte rotatedFacingByte = rotateFacingByte(rawFacing, ccwRotations);
                Direction outward = Direction.from3DDataValue(rotatedFacingByte & 0xFF);
                if (outward == null) outward = Direction.SOUTH;

                // entityBlock = the air block where the frame entity floats (= rotated Pos).
                BlockPos entityBlock = new BlockPos(
                        origin.getX() + rotBX, origin.getY() + blkY, origin.getZ() + rotBZ);
                BlockPos wallBlock = entityBlock.relative(outward.getOpposite());

                BerongSMP.LOGGER.info("[SchemLoader] ItemFrame entityBlock={} wallBlock={} wallBlockState={} rawFacing={} outward={}",
                        entityBlock, wallBlock, level.getBlockState(wallBlock).getBlock(), rawFacing, outward);

                if (spawnItemFrame(level, entityTag, entityId, entityBlock, outward)) {
                    spawned++;
                }
                continue;
            }

            try (ProblemReporter.ScopedCollector reporter =
                         new ProblemReporter.ScopedCollector(() -> resourcePath.toString(), BerongSMP.LOGGER)) {
                var input = TagValueInput.create(reporter, level.registryAccess(), spawnNbt);
                boolean ok = EntityType.create(input, level, EntitySpawnReason.LOAD)
                          .map(e -> { level.addFreshEntity(e); return true; })
                          .orElse(false);
                if (!ok) BerongSMP.LOGGER.warn("[SchemLoader] EntityType.create returned empty for {}", entityId);
                else spawned++;
            }
        }

        BerongSMP.LOGGER.info("[SchemLoader] Spawned {}/{} entities from {} at {}",
                spawned, entityList.size(), resourcePath, origin);
    }

    /**
     * Applies tile-entity (block-entity) data from the Sponge Schematic BlockEntities list.
     * Sponge v3 stores these under Blocks.BlockEntities; v2 uses TileEntities at root level.
     * Each entry has Pos (Int_Array [x,y,z]) and Data (raw Minecraft tile-entity NBT).
     * The same CCW rotation applied to blocks is applied to the position offset here.
     */
    private void placeBlockEntities(ServerLevel level, BlockPos origin, CompoundTag nbt,
                                     int schemWidth, int schemHeight, int schemLength) {
        // v3: Blocks.BlockEntities; v2 fallback: TileEntities at root
        CompoundTag blocksTag = nbt.getCompound("Blocks").orElse(nbt);
        Tag beRaw = blocksTag.get("BlockEntities");
        if (!(beRaw instanceof ListTag beList)) beRaw = nbt.get("TileEntities");
        if (!(beRaw instanceof ListTag)) return;
        ListTag beList2 = (ListTag) beRaw;
        if (beList2.isEmpty()) return;

        int applied = 0;
        for (int i = 0; i < beList2.size(); i++) {
            if (!(beList2.get(i) instanceof CompoundTag beTag)) continue;

            // Position: TAG_Int_Array [x, y, z] relative to schematic origin
            Tag posRaw = beTag.get("Pos");
            if (!(posRaw instanceof IntArrayTag posArr)) continue;
            int[] pos = posArr.getAsIntArray();
            if (pos.length < 3) continue;
            int relX = pos[0], relY = pos[1], relZ = pos[2];

            // Apply same CCW rotation as block placement
            int rotX, rotZ;
            switch (ccwRotations) {
                case 1  -> { rotX = relZ;                    rotZ = schemWidth  - 1 - relX; }
                case 2  -> { rotX = schemWidth  - 1 - relX; rotZ = schemLength - 1 - relZ; }
                case 3  -> { rotX = schemLength - 1 - relZ; rotZ = relX; }
                default -> { rotX = relX;                    rotZ = relZ; }
            }
            BlockPos worldPos = origin.offset(rotX, relY, rotZ);

            var dataOpt = beTag.getCompound("Data");
            if (dataOpt.isEmpty()) continue;
            CompoundTag data = dataOpt.get();

            BlockEntity be = level.getBlockEntity(worldPos);
            if (be == null) {
                BerongSMP.LOGGER.warn("[SchemLoader] No BlockEntity at {} (BE #{})", worldPos, i);
                continue;
            }

            try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(() -> resourcePath.toString(), BerongSMP.LOGGER)) {
                var input = TagValueInput.create(reporter, level.registryAccess(), data);
                be.loadWithComponents(input);
                be.setChanged();
                applied++;
            } catch (Exception e) {
                BerongSMP.LOGGER.warn("[SchemLoader] BlockEntity at {} load failed: {}", worldPos, e.getMessage());
            }
        }
        BerongSMP.LOGGER.info("[SchemLoader] Applied {}/{} BlockEntities at {}", applied, beList2.size(), origin);
    }

    /**
     * Creates an ItemFrame or GlowItemFrame directly, bypassing NBT deserialization.
     * entityBlock = the air block the frame entity occupies (Sponge v3 top-level Pos).
     * outward     = direction FROM wall TOWARD viewer (Data.Facing, already rotated).
     * In MC 26.x ItemFrame(level, pos, direction): pos is the entity's own block, not the wall.
     */
    private boolean spawnItemFrame(ServerLevel level, CompoundTag entityTag, String entityId,
                                   BlockPos entityBlock, Direction outward) {
        BlockPos wallBlock = entityBlock.relative(outward.getOpposite());
        if (level.getBlockState(wallBlock).isAir()) {
            BerongSMP.LOGGER.warn("[SchemLoader] Wall block {} behind frame is air — skipped", wallBlock);
            return false;
        }

        ItemFrame frame = entityId.equals("minecraft:glow_item_frame")
                ? new GlowItemFrame(level, entityBlock, outward)
                : new ItemFrame(level, entityBlock, outward);

        // Item NBT is nested under Data in Sponge v3.
        entityTag.getCompound("Data").ifPresent(data ->
            data.getCompound("Item").ifPresent(itemTag -> {
                var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                ItemStack.OPTIONAL_CODEC.parse(ops, itemTag).result().ifPresent(stack -> {
                    if (!stack.isEmpty()) {
                        frame.setItem(stack, false);
                        BerongSMP.LOGGER.info("[SchemLoader] Frame item: {}", stack.getItem());
                    }
                });
            })
        );

        return level.addFreshEntity(frame);
    }

    /** Converts a Facing byte to an (x,y,z) step vector. */
    private static int[] facingByteToStep(byte facing) {
        return switch (facing) {
            case 0 -> new int[]{  0, -1,  0 }; // DOWN
            case 1 -> new int[]{  0,  1,  0 }; // UP
            case 2 -> new int[]{  0,  0, -1 }; // NORTH
            case 3 -> new int[]{  0,  0,  1 }; // SOUTH
            case 4 -> new int[]{ -1,  0,  0 }; // WEST
            case 5 -> new int[]{  1,  0,  0 }; // EAST
            default -> new int[]{  0,  0,  0 };
        };
    }

    /** Reads a string value from a CompoundTag key using raw Tag access (avoids Optional vs String API ambiguity). */
    private static String getStringTag(CompoundTag tag, String key) {
        Tag t = tag.get(key);
        if (t instanceof net.minecraft.nbt.StringTag st) return st.value();
        return "";
    }

    /** Safely reads a numeric Tag as a double via the NumericTag interface. */
    private static double tagToDouble(Tag tag) {
        if (tag instanceof net.minecraft.nbt.NumericTag nt) return nt.doubleValue();
        return 0.0;
    }

    /**
     * Rotates a Minecraft facing byte by the given number of CCW 90° steps.
     * Cycle per CCW step: North(2) → West(4) → South(3) → East(5) → North(2).
     * Up(1) and Down(0) are invariant.
     */
    private static byte rotateFacingByte(byte facing, int ccwRotations) {
        if (facing < 2) return facing;
        int[] cycle = {2, 4, 3, 5}; // N, W, S, E
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i] == facing) return (byte) cycle[(i + ccwRotations) % 4];
        }
        return facing;
    }
}
