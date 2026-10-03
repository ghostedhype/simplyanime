package net.hussain.simplyanime.callout;

// ability name shown above the hotbar
public enum Callout {
    ENUMA_ELISH("callout.simplyanime.enuma_elish", Look.RUPTURE),
    CRUEL_SUN("callout.simplyanime.cruel_sun", Look.FIRE);

    public final String key;
    public final Look look;

    Callout(String key, Look look) {
        this.key = key;
        this.look = look;
    }

    public enum Look {
        RUPTURE,
        FIRE
    }
}
