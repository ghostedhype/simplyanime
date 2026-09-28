# Simply Anime

An addon for [Simply Swords](https://github.com/Sweenus/SimplySwords) that adds unique weapons from anime.
Forge 1.20.1.

## Weapons

**Inverted Spear of Heaven** (sai)
- Right click: dash forward and cut twice, the first hit knocks the target into the air.
- Sneak + right click: throw the blade out on its chain and whirl it around you, then fling it into the marked target.

**Ea, The Sword Of Rupture** (claymore)
- Right click, Enuma Elish: raise the sword and charge it (you can't be hurt while charging), then thrust
  to fire a huge beam. It deals one heavy hit when it fires and keeps damaging anything inside it for the rest
  of the beam. It goes through terrain and never breaks blocks.

Both are registered like any other Simply Swords unique: they show up in loot chests, take gems and
runic upgrades, and ship Better Combat weapon attributes. Ea can also be crafted (two nether stars, redstone blocks,
gold blocks and a netherite ingot).

Everything is configurable in `config/simplyanime/weapons.toml`, including damage, cooldowns, beam size,
sounds and whether Ea is craftable or found in loot.

`/SimplyAnime cooldown reset [players]` clears ability cooldowns (needs op), handy for testing.

## Requirements

- Minecraft 1.20.1, Forge 47.4 or newer
- Simply Swords 1.70.2 or newer (and its own dependencies)
- Architectury API 9.2.14 or newer

## Building

Gradle has to run on **Java 21** (Architectury Loom 1.13 needs it). The mod itself targets Java 17.
If your default Java isn't 21, point Gradle at one, for example in `~/.gradle/gradle.properties`:

```
org.gradle.java.home=/path/to/jdk-21
```

Then:

```
./gradlew build        # jar ends up in build/libs/simplyanime-<version>.jar
./gradlew runClient    # dev client with Simply Swords and its dependencies
```

Simply Swords and Simply Tooltips come from the Modrinth maven by version id (`gradle.properties`),
because the Forge and Fabric builds share the same version number and the plain number resolves to
the Fabric jar.

## Credits

- Simply Swords by Sweenus / Timefall Development
- The Enuma Elish sound effects are from Fate/Grand Order and belong to their owners

## License

Source available, all rights reserved. See [LICENSE](LICENSE).
