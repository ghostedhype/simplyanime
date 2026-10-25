package net.ghostedhype.simplyanime.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

// effects are queued and drawn after water, ice and glass so they show up over them
public class Later {

    public interface Draw {
        void now(PoseStack poseStack, MultiBufferSource buffers);
    }

    private record Job(PoseStack poseStack, Draw draw) {
    }

    private static final List<Job> JOBS = new ArrayList<>();
    private static boolean running;
    // the buffers of Iris and Oculus, see group()
    private static Class<?> grouping;
    private static Method groupStart;
    private static Method groupEnd;

    public static boolean take(Entity entity, PoseStack poseStack, Draw draw) {
        if (running || !possible() || submerged(entity)) {
            return false;
        }
        PoseStack copy = new PoseStack();
        copy.last().pose().set(poseStack.last().pose());
        copy.last().normal().set(poseStack.last().normal());
        if (JOBS.size() > 256) {
            JOBS.clear();
        }
        JOBS.add(new Job(copy, draw));
        return true;
    }

    private static boolean possible() {
        if (ShaderCompat.packInUse() || ShaderCompat.shadowPass() || Minecraft.useShaderTransparency()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        return mc.level != null && camera.isInitialized() && !translucent(mc.level.getBlockState(camera.getBlockPosition()));
    }

    private static boolean translucent(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        return !state.getFluidState().isEmpty() || ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.translucent();
    }

    private static boolean submerged(Entity entity) {
        Level level = entity.level();
        Vec3 at = entity.position();
        return !level.getFluidState(BlockPos.containing(at.x, at.y + 0.2, at.z)).isEmpty()
                && !level.getFluidState(BlockPos.containing(at.x, at.y + 1.6, at.z)).isEmpty();
    }

    // called when the translucent blocks are in
    public static void flush() {
        if (JOBS.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        running = true;
        try {
            for (Job job : JOBS) {
                boolean grouped = group(buffers, true);
                try {
                    job.draw.now(job.poseStack, buffers);
                } finally {
                    if (grouped) {
                        group(buffers, false);
                    }
                }
            }
        } finally {
            running = false;
            JOBS.clear();
            buffers.endBatch();
        }
    }

    private static boolean group(Object buffers, boolean start) {
        try {
            if (grouping != buffers.getClass()) {
                grouping = buffers.getClass();
                groupStart = null;
                groupEnd = null;
                try {
                    groupStart = grouping.getMethod("maybeStartGroup");
                    groupEnd = grouping.getMethod("endGroup");
                } catch (NoSuchMethodException plain) {
                    // the game's own buffers
                }
            }
            if (groupStart == null || groupEnd == null) {
                return false;
            }
            if (start) {
                return Boolean.TRUE.equals(groupStart.invoke(buffers));
            }
            groupEnd.invoke(buffers);
            return true;
        } catch (ReflectiveOperationException | RuntimeException changed) {
            groupStart = null;
            groupEnd = null;
            return false;
        }
    }
}
