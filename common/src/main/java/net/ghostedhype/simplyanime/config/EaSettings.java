package net.ghostedhype.simplyanime.config;

import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;

public class EaSettings extends ConfigSection {

    public float damageModifier = 5.0f;
    public float attackSpeed = -2.9f;
    public boolean craftable = true;
    public boolean lootable = true;

    @ValidatedInt.Restrict(min = 0)
    public int cooldown = 1800;

    // charge
    @ValidatedInt.Restrict(min = 1)
    public int chargeDuration = 80;
    @ValidatedInt.Restrict(min = -1, max = 9)
    public int chargeSlowness = 3;
    @ValidatedInt.Restrict(min = 0)
    public int nameTextLead = 40;

    // beam
    @ValidatedInt.Restrict(min = 1)
    public int beamDuration = 120;
    @ValidatedDouble.Restrict(min = 4.0, max = 512.0)
    public double beamRange = 160.0;
    @ValidatedDouble.Restrict(min = 0.5, max = 32.0)
    public double beamRadius = 8.0;
    // blocks never break either way
    public boolean stopAtBlocks = false;
    @ValidatedDouble.Restrict(min = 0.0, max = 32.0)
    public double impactRadius = 5.0;
    @ValidatedFloat.Restrict(min = 0f)
    public float blastDamageScaling = 9.0f;
    @ValidatedFloat.Restrict(min = 0f)
    public float blastSpellScaling = 25.0f;
    @ValidatedFloat.Restrict(min = 0f)
    public float tickDamageScaling = 0.5f;
    @ValidatedFloat.Restrict(min = 0f)
    public float tickSpellScaling = 1.5f;
    @ValidatedInt.Restrict(min = 1)
    public int tickInterval = 5;
    @ValidatedDouble.Restrict(min = 0.0, max = 5.0)
    public double beamKnockback = 1.2;

    // volume above 1 only makes it carry further
    @ValidatedFloat.Restrict(min = 0f, max = 16f)
    public float chargeVolume = 3.0f;
    @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
    public float chargePitch = 1.0f;
    public boolean nameText = true;
    @ValidatedFloat.Restrict(min = 0f, max = 16f)
    public float releaseVolume = 5.0f;
    @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
    public float releasePitch = 1.0f;
    @ValidatedFloat.Restrict(min = 0f, max = 16f)
    public float beamVolume = 6.0f;
    @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
    public float beamPitch = 1.0f;
    @ValidatedFloat.Restrict(min = 0f, max = 16f)
    public float impactVolume = 6.0f;
    @ValidatedFloat.Restrict(min = 0.5f, max = 2f)
    public float impactPitch = 1.0f;
}
