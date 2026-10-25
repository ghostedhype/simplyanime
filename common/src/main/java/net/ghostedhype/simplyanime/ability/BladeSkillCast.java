package net.ghostedhype.simplyanime.ability;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// one sword skill in progress
class BladeSkillCast {
    final UUID actorId;
    @Nullable final UUID ownerId;
    @Nullable final UUID targetId;
    final ItemStack stack;
    final BladeSkill skill;
    final long startTime;
    final UUID visualId;
    final boolean mirror;
    boolean finished;
    // the effect entity stays where it is from here on
    boolean anchored;
    float yaw;
    Vec3 aim = new Vec3(0, 0, 1);
    Vec3 center = Vec3.ZERO;
    int releaseTick = -1;
    final List<SlashWave> waves = new ArrayList<>();
    final Set<UUID> struck = new HashSet<>();

    BladeSkillCast(UUID actorId, @Nullable UUID ownerId, @Nullable UUID targetId, ItemStack stack, BladeSkill skill,
                   long startTime, UUID visualId, boolean mirror) {
        this.actorId = actorId;
        this.ownerId = ownerId;
        this.targetId = targetId;
        this.stack = stack;
        this.skill = skill;
        this.startTime = startTime;
        this.visualId = visualId;
        this.mirror = mirror;
    }
}
