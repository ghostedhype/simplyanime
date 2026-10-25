package net.ghostedhype.simplyanime.callout;

// ability name shown above the hotbar
public enum Callout {
    EA("callout.simplyanime.ea", Look.RUPTURE),
    CRUEL_SUN("callout.simplyanime.cruel_sun", Look.FIRE),
    VORPAL_STRIKE("callout.simplyanime.vorpal_strike", Look.CRIMSON),
    HORIZONTAL_SQUARE("callout.simplyanime.horizontal_square", Look.FROST),
    STARBURST_STREAM("callout.simplyanime.starburst_stream", Look.STARS);

    public final String key;
    public final Look look;

    Callout(String key, Look look) {
        this.key = key;
        this.look = look;
    }

    public enum Look {
        RUPTURE,
        FIRE,
        CRIMSON,
        FROST,
        STARS
    }
}
