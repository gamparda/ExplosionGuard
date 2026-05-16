# ExplosionGuard

Minecraft Paper/Spigot plugin that prevents explosions from breaking blocks while keeping explosion damage enabled.

## Behavior

- TNT, creepers, end crystals, beds, respawn anchors, fireballs, etc. do not destroy blocks.
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
