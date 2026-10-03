package net.hussain.simplyanime.client;

import net.hussain.simplyanime.config.ClientConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// light blocks in the air near an effect so it lights the ground. client world only, the server never sees them
public class EffectLights {

    private static final Map<Entity, Set<BlockPos>> LIGHTS = new HashMap<>();
    private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

    public static void set(Entity owner, List<Vec3d> spots) {
        World world = owner.getWorld();
        if (!world.isClient()) {
            return;
        }
        if (!ClientConfig.get().dynamicLight || spots.isEmpty()) {
            clear(owner);
            return;
        }
        Set<BlockPos> wanted = new HashSet<>();
        for (Vec3d spot : spots) {
            wanted.add(BlockPos.ofFloored(spot));
        }
        Set<BlockPos> placed = LIGHTS.computeIfAbsent(owner, e -> new HashSet<>());
        Iterator<BlockPos> old = placed.iterator();
        while (old.hasNext()) {
            BlockPos pos = old.next();
            if (!wanted.contains(pos)) {
                remove(world, pos);
                old.remove();
            }
        }
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15);
        for (BlockPos pos : wanted) {
            if (!placed.contains(pos) && world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, light, FLAGS);
                placed.add(pos);
            }
        }
    }

    public static void clear(Entity owner) {
        Set<BlockPos> placed = LIGHTS.remove(owner);
        if (placed != null) {
            for (BlockPos pos : placed) {
                remove(owner.getWorld(), pos);
            }
        }
    }

    private static void remove(World world, BlockPos pos) {
        if (world.getBlockState(pos).isOf(Blocks.LIGHT)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), FLAGS);
        }
    }
}
