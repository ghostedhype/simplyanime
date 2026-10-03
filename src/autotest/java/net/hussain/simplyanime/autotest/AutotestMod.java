package net.hussain.simplyanime.autotest;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

// dev only, runClient -Pautotest
@Mod("simplyanime_autotest")
public class AutotestMod {

    public AutotestMod() {
        if (!Boolean.getBoolean("simplyanime.autotest")) {
            return;
        }
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> CastCommand.register(dispatcher));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientTickEvent.CLIENT_POST.register(Autotest::tick);
        }
    }
}
