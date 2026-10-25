package net.ghostedhype.simplyanime.ability;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// one Enuma Elish in progress
class EaCast {
    final UUID actorId;
    @Nullable final UUID ownerId;
    final ItemStack stack;
    final InteractionHand hand;
    final long startTime;
    final UUID visualId;
    @Nullable final UUID targetId;
    Stage stage = Stage.CHARGE;
    Vec3 origin = Vec3.ZERO;
    Vec3 direction = new Vec3(0, 0, 1);
    double length;
    boolean hitTerrain;
    final Set<UUID> blasted = new HashSet<>();

    EaCast(UUID actorId, @Nullable UUID ownerId, ItemStack stack, InteractionHand hand, long startTime, UUID visualId,
           @Nullable UUID targetId) {
        this.actorId = actorId;
        this.ownerId = ownerId;
        this.stack = stack;
        this.hand = hand;
        this.startTime = startTime;
        this.visualId = visualId;
        this.targetId = targetId;
    }

    Vec3 end() {
        return this.origin.add(this.direction.scale(this.length));
    }

    enum Stage {
        CHARGE, RELEASE, BEAM, FADE
    }
}
