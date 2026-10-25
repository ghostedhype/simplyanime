package net.ghostedhype.simplyanime.client.renderer;

final class ShaderPackProfiles {

    // This allows for targeting the Beacon, specifically. That way the gain
    //   for other render paths are not adjusted.
    enum RenderPath {
        BEACON,
        FORWARD_EMISSIVE,
        BEACON_BLENDED
    }

    private ShaderPackProfiles() {
    }

    static RenderPath renderPath(String name) {
        if (name.contains("bliss")) {
            return RenderPath.FORWARD_EMISSIVE;
        }
        if (name.contains("photon")) {
            return RenderPath.BEACON_BLENDED;
        }
        return RenderPath.BEACON;
    }

    // bsl, makeup and solas blow beacons out to white, solid parts the worst
    static float gain(String name, boolean solid) {
        if (name.contains("bsl")) {
            return solid ? 0.22F : 0.4F;
        }
        if (name.contains("makeup")) {
            return solid ? 0.2F : 0.35F;
        }
        if (name.contains("solas")) {
            return solid ? 0.3F : 0.55F;
        }
        if (name.contains("photon")) {
            return solid ? 0.8F : 1.0F;
        }
        return 1.0F;
    }
}