package net.hussain.simplyanime.rhitta;

import net.hussain.simplyanime.rhitta.config.RhittaConfig;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

// passive side: sun bonus on basic hits and the size change.
// Forge 1.20.1 has no scale attribute so the size lives here
public class RhittaGrowth {

    private static final UUID REACH_ID = UUID.fromString("5f2a7d1e-3c4b-4e8a-9b6d-2e1f0a9c8b71");

    // prev, current, the scale the hitbox was last built with
    private static final Map<PlayerEntity, float[]> SERVER = new WeakHashMap<>();
    private static final Map<PlayerEntity, float[]> CLIENT = new WeakHashMap<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(RhittaGrowth::onHurt);
        MinecraftForge.EVENT_BUS.addListener(RhittaGrowth::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(RhittaGrowth::onSize);
    }

    public static boolean holding(LivingEntity entity) {
        return entity.getMainHandStack().getItem() instanceof DivineAxeRhittaItem;
    }

    private static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (CruelSunEntity.dealing || !(source.getAttacker() instanceof LivingEntity attacker)
                || source.getSource() != attacker || !holding(attacker)) {
            return;
        }
        // plain melee only, gem and runic procs keep their own numbers
        if (!source.isOf(DamageTypes.PLAYER_ATTACK) && !source.isOf(DamageTypes.MOB_ATTACK)
                && !source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
            return;
        }
        float power = SunPower.sunPower(attacker.getWorld(), attacker);
        event.setAmount(event.getAmount() * power);
        RhittaConfig.AxeSettings cfg = RhittaConfig.get().axe;
        float seconds = cfg.burnSeconds + cfg.burnSunScaling * Math.max(0.0F, power - 1.0F);
        if (seconds > 0) {
            event.getEntity().setOnFireFor(Math.round(seconds));
        }
    }

    public static float targetScale(PlayerEntity player) {
        RhittaConfig.GrowthSettings cfg = RhittaConfig.get().growth;
        if (!cfg.enabled || !holding(player)) {
            return 1.0F;
        }
        float sun = SunPower.fraction(SunPower.sunPower(player.getWorld(), player));
        return MathHelper.lerp(sun, 1.0F, Math.max(1.0F, cfg.maxScale));
    }

    public static float renderScale(PlayerEntity player, float tickDelta) {
        float[] s = CLIENT.get(player);
        return s == null ? 1.0F : MathHelper.lerp(tickDelta, s[0], s[1]);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PlayerEntity player = event.player;
        Map<PlayerEntity, float[]> map = player.getWorld().isClient() ? CLIENT : SERVER;
        float[] s = map.computeIfAbsent(player, p -> new float[]{1.0F, 1.0F, 1.0F});
        s[0] = s[1];
        float target = targetScale(player);
        float next = s[1] + (target - s[1]) * 0.08F;
        if (Math.abs(target - next) < 0.002F) {
            next = target;
        }

        boolean hitbox = RhittaConfig.get().growth.scaleHitbox;
        // never grow into a ceiling or a wall
        if (hitbox && next > s[1] && !roomFor(player, next)) {
            next = s[1];
        }
        s[1] = next;

        float built = hitbox ? next : 1.0F;
        if (Math.abs(built - s[2]) > 0.01F || (built == target && built != s[2])) {
            s[2] = built;
            player.calculateDimensions();
            if (!player.getWorld().isClient()) {
                setReach(player, ForgeMod.ENTITY_REACH.get(), built);
                setReach(player, ForgeMod.BLOCK_REACH.get(), built);
            }
        }
    }

    private static boolean roomFor(PlayerEntity player, float scale) {
        EntityDimensions size = player.getDimensions(player.getPose()).scaled(scale);
        return player.getWorld().isSpaceEmpty(player, size.getBoxAt(player.getPos()).contract(1.0E-7));
    }

    private static void setReach(PlayerEntity player, EntityAttribute attribute, float scale) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(REACH_ID);
        if (scale > 1.0F) {
            instance.addTemporaryModifier(new EntityAttributeModifier(REACH_ID, "Rhitta growth", scale - 1.0F,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof PlayerEntity player) || player.getWorld() == null) {
            return;
        }
        float[] s = (player.getWorld().isClient() ? CLIENT : SERVER).get(player);
        if (s == null || s[2] == 1.0F) {
            return;
        }
        event.setNewSize(event.getNewSize().scaled(s[2]), false);
        event.setNewEyeHeight(event.getNewEyeHeight() * s[2]);
    }
}
