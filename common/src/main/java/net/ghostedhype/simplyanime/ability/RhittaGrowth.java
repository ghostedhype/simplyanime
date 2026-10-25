package net.ghostedhype.simplyanime.ability;

import net.ghostedhype.simplyanime.config.RhittaConfig;
import net.ghostedhype.simplyanime.entity.CruelSunEntity;
import net.ghostedhype.simplyanime.item.DivineAxeRhittaItem;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

public class RhittaGrowth {

    // prev, current, the scale the hitbox was last built with
    private static final Map<Player, float[]> SERVER = new WeakHashMap<>();
    private static final Map<Player, float[]> CLIENT = new WeakHashMap<>();

    public static boolean holding(LivingEntity entity) {
        return entity.getMainHandItem().getItem() instanceof DivineAxeRhittaItem;
    }

    public static float scaleDamage(LivingEntity target, DamageSource source, float amount) {
        if (CruelSunEntity.dealing || !(source.getEntity() instanceof LivingEntity attacker)
                || source.getDirectEntity() != attacker || !holding(attacker)) {
            return amount;
        }
        if (!source.is(DamageTypes.PLAYER_ATTACK) && !source.is(DamageTypes.MOB_ATTACK)
                && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
            return amount;
        }
        float power = SunPower.sunPower(attacker.level(), attacker);
        RhittaConfig.AxeSettings config = RhittaConfig.get().axe;
        float seconds = config.burnSeconds + config.burnSunScaling * Math.max(0.0F, power - 1.0F);
        if (seconds > 0) {
            target.igniteForSeconds(seconds);
        }
        return amount * power;
    }

    public static float targetScale(Player player) {
        RhittaConfig.GrowthSettings config = RhittaConfig.get().growth;
        if (!config.enabled || !holding(player)) {
            return 1.0F;
        }
        float sun = SunPower.fraction(SunPower.sunPower(player.level(), player));
        return Mth.lerp(sun, 1.0F, Math.max(1.0F, config.maxScale));
    }

    public static float renderScale(Player player, float partialTick) {
        float[] scale = CLIENT.get(player);
        return scale == null ? 1.0F : Mth.lerp(partialTick, scale[0], scale[1]);
    }

    public static void tick(Player player) {
        Map<Player, float[]> map = player.level().isClientSide() ? CLIENT : SERVER;
        float[] scale = map.computeIfAbsent(player, p -> new float[]{1.0F, 1.0F, 1.0F});
        scale[0] = scale[1];
        float target = targetScale(player);
        float next = scale[1] + (target - scale[1]) * 0.08F;
        if (Math.abs(target - next) < 0.002F) {
            next = target;
        }

        boolean hitbox = RhittaConfig.get().growth.scaleHitbox;
        // never grow into a ceiling or a wall
        if (hitbox && next > scale[1] && !roomFor(player, next)) {
            next = scale[1];
        }
        scale[1] = next;

        float built = hitbox ? next : 1.0F;
        if (Math.abs(built - scale[2]) > 0.01F || (built == target && built != scale[2])) {
            scale[2] = built;
            player.refreshDimensions();
            if (!player.level().isClientSide()) {
                AbilityHelper.setReach(player, built);
            }
        }
    }

    private static boolean roomFor(Player player, float scale) {
        EntityDimensions size = player.getDimensions(player.getPose()).scale(scale);
        return player.level().noCollision(player, size.makeBoundingBox(player.position()).deflate(1.0E-7));
    }

    public static float sizeScale(Entity entity) {
        if (!(entity instanceof Player player) || player.level() == null) {
            return 1.0F;
        }
        float[] scale = (player.level().isClientSide() ? CLIENT : SERVER).get(player);
        return scale == null ? 1.0F : scale[2];
    }
}
