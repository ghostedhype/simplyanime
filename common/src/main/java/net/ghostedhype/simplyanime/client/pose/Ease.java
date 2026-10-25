package net.ghostedhype.simplyanime.client.pose;

public class Ease {

    public static float out(float tickProgress) {
        return 1.0F - (1.0F - tickProgress) * (1.0F - tickProgress);
    }

    public static float inOut(float tickProgress) {
        return tickProgress * tickProgress * (3.0F - 2.0F * tickProgress);
    }
}
