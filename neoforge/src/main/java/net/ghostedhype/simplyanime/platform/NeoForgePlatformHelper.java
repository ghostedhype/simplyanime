package net.ghostedhype.simplyanime.platform;

import net.ghostedhype.simplyanime.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.entity.PartEntity;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public Entity getParentPart(Entity entity) {
        if (entity instanceof PartEntity<?> part) {
            return part.getParent();
        }
        return IPlatformHelper.super.getParentPart(entity);
    }

    @Override
    public boolean isPart(Entity entity) {
        return entity instanceof PartEntity<?>
                || IPlatformHelper.super.isPart(entity);
    }

    @Override
    public boolean isFlammable(BlockState blockState, BlockGetter level, BlockPos pos, Direction face) {
        return blockState.isFlammable(level, pos, face);
    }
}
