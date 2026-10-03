package net.hussain.simplyanime.rhitta.client;

import net.hussain.simplyanime.rhitta.RhittaRegistry;
import net.hussain.simplyanime.rhitta.config.RhittaConfig;
import net.hussain.simplyanime.rhitta.entity.CruelSunEntity;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

// roar that rides along with the sun while it flies
public class SunFlightSound extends MovingSoundInstance {

    private final CruelSunEntity sun;

    public SunFlightSound(CruelSunEntity sun) {
        super(RhittaRegistry.FLIGHT.get(), SoundCategory.PLAYERS, SoundInstance.createRandom());
        this.sun = sun;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = RhittaConfig.get().cruelSun.flightVolume * (0.7F + 0.3F * sun.getSun());
        this.pitch = 1.0F - 0.2F * sun.getSun();
        this.x = sun.getX();
        this.y = sun.getY();
        this.z = sun.getZ();
    }

    @Override
    public void tick() {
        if (this.sun.isRemoved() || this.sun.getPhase() != CruelSunEntity.PHASE_FLIGHT) {
            this.setDone();
            return;
        }
        this.x = this.sun.getX();
        this.y = this.sun.getY();
        this.z = this.sun.getZ();
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }
}
