package net.ghostedhype.simplyanime.ability;

import net.ghostedhype.simplyanime.Constants;
import net.ghostedhype.simplyanime.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

// bits every ability needs
public class AbilityHelper {

    public static void playSound(ServerLevel world, Vec3 position, SoundEvent sound, float volume, float pitch) {
        if (volume > 0) {
            world.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    public static double distanceToPoint(AABB box, Vec3 point) {
        double distanceX = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
        double distanceY = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
        double distanceZ = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
        return Math.sqrt(distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ);
    }

    public static boolean canHit(ServerLevel world, LivingEntity target, LivingEntity actor, @Nullable UUID ownerId) {
        if (target == actor || !target.isAlive() || !SimplySwordsAPI.isValidAbilityTarget(target, actor)) {
            return false;
        }
        return canHitOwner(world, target, ownerId);
    }

    public static boolean canHitOwner(ServerLevel world, LivingEntity target, @Nullable UUID ownerId) {
        if (ownerId == null) {
            return true;
        }
        Entity owner = world.getEntity(ownerId);
        return !(owner instanceof LivingEntity livingOwner)
                || target != livingOwner && SimplySwordsAPI.isValidAbilityTarget(target, livingOwner);
    }

    @Nullable
    public static Vec3 strike(ServerLevel world, LivingEntity actor, @Nullable UUID ownerId, ItemStack stack,
                              Entity entity, Entity root, float damage) {
        if (root instanceof EnderDragon dragon) {
            if (dragon.isAlive() && canHitOwner(world, dragon, ownerId)) {
                dragon.hurt(dragon.head, SimplySwordsAPI.getWeaponDamageSource(actor), damage);
                return dragon.head.position();
            }
            return null;
        }
        if (root instanceof LivingEntity target) {
            if (canHit(world, target, actor, ownerId) && SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, actor,
                    stack, target, damage, SpellScalingProfile.ARCANE)) {
                return target.position().add(0, target.getBbHeight() * 0.5, 0);
            }
            return null;
        }
        if (Services.PLATFORM.isPart(entity)) {
            entity.hurt(SimplySwordsAPI.getWeaponDamageSource(actor), damage);
        }

        if (Services.PLATFORM.isPart(entity)) {
            entity.hurt(SimplySwordsAPI.getWeaponDamageSource(actor), damage);
        }
        return null;
    }

    public static Vec3 look(LivingEntity entity) {
        return Vec3.directionFromRotation(entity.getXRot(), entity.getYHeadRot()).normalize();
    }

    public static Vec3 toWorld(Vector3f local, Vec3 origin, float yaw, boolean mirror) {
        float angle = (180.0F - yaw) * Mth.DEG_TO_RAD;
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float x = mirror ? -local.x : local.x;
        return origin.add(x * cos + local.z * sin, local.y, -x * sin + local.z * cos);
    }

    private static final ResourceLocation REACH_ID = Constants.tryId("rhitta_growth");

    // reach as a multiple of the normal one, 1 puts it back
    public static void setReach(Player player, float scale) {
        setReach(player, Attributes.ENTITY_INTERACTION_RANGE, scale);
        setReach(player, Attributes.BLOCK_INTERACTION_RANGE, scale);
    }

    public static void setReach(Player player, Holder<Attribute> attribute, float scale) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(REACH_ID);
        if (scale > 1.0F) {
            instance.addTransientModifier(new AttributeModifier(REACH_ID, scale - 1.0F,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
