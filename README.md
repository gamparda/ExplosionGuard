# ExplosionGuard

Minecraft Paper/Spigot plugin that prevents explosions from breaking blocks in the overworld while keeping explosion damage enabled. Nether and End explosions are allowed normally.

## Behavior

- Overworld TNT, creepers, end crystals, fireballs, etc. do not destroy blocks.
- Nether and End explosions, including beds and respawn anchors, can destroy blocks normally.
- Explosion damage and knockback still apply normally to players/entities.
- No commands or permissions required.

## Build

```bash
mvn clean package
```

The jar will be created at:

```text
target/ExplosionGuard-1.0.0.jar
```

## Install

1. Build or download the jar.
2. Put it in your server's `plugins/` folder.
3. Restart the server.
