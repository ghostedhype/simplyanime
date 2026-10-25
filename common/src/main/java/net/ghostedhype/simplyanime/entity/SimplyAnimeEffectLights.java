package net.ghostedhype.simplyanime.entity;

import net.ghostedhype.simplyanime.config.ClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SimplyAnimeEffectLights {

    private static final Map<Entity, Set<BlockPos>> LIGHTS = new WeakHashMap<>();
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public static void set(Entity owner, List<Vec3> spots) {
        Level world = owner.level();
        if (!world.isClientSide()) {
            return;
        }
        if (!ClientConfig.get().dynamicLight || spots.isEmpty()) {
            clear(owner);
            return;
        }
        Set<BlockPos> wanted = new HashSet<>();
        for (Vec3 spot : spots) {
            wanted.add(BlockPos.containing(spot));
        }
        Set<BlockPos> placed = LIGHTS.computeIfAbsent(owner, entity -> new HashSet<>());
        Iterator<BlockPos> old = placed.iterator();
        while (old.hasNext()) {
            BlockPos blockPos = old.next();
            if (!wanted.contains(blockPos)) {
                remove(world, blockPos);
                old.remove();
            }
        }
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        for (BlockPos blockPos : wanted) {
            if (!placed.contains(blockPos) && world.getBlockState(blockPos).isAir()) {
                world.setBlock(blockPos, light, FLAGS);
                placed.add(blockPos);
            }
        }
    }

    public static void clear(Entity owner) {
        Set<BlockPos> placed = LIGHTS.remove(owner);
        if (placed != null) {
            for (BlockPos blockPos : placed) {
                remove(owner.level(), blockPos);
            }
        }
    }

    private static void remove(Level world, BlockPos blockPos) {
        if (world.getBlockState(blockPos).is(Blocks.LIGHT)) {
            world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }
}
