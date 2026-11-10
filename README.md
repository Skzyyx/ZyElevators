# ZyElevators

**Jump to go up, sneak to go down.** Lightweight block elevators for **Paper** and **Folia**.

Place two or more elevator blocks in the same vertical line. Stand on one and jump to
travel to the next elevator above, or sneak to go to the next one below.

## Features

- **Folia support.** Everything runs on the right region thread, so it works on Folia as
  well as on regular Paper servers.
- **Any solid block can be an elevator.** Wool by default, but you can use concrete,
  shulker boxes, or any block you like. Only blocks placed from an elevator item become
  elevators, so normal blocks of the same type keep working as usual.
- **No database or data files.** Elevators are saved inside the world's chunks.
- **Independent lines.** By default an elevator only connects with elevators of the same
  block, so you can build several lines with different colors side by side.
- **Craftable.** 8 elevator blocks around an ender pearl (configurable), with a
  permission to restrict it.
- **Protected.** Explosions, pistons, fire, endermen and hoppers can't break, move or use
  elevators. Container elevators (shulker boxes, barrels…) can't be opened.
- **Safe destinations.** Skips elevators that don't have two free blocks above them.
- **Configurable.** Distance, cooldown, directions, sound (custom resource pack sounds
  work too), particle colors and disabled worlds.
- **Translatable.** English and Spanish included, using
  [MiniMessage](https://docs.advntr.dev/minimessage/format.html). Block names are shown
  in each player's own game language.

## Requirements

- Paper or Folia **1.21.4** or newer
- Java 21

## Installation

1. Download `ZyElevators-<version>.jar` and put it in your `plugins` folder.
2. Restart the server.
3. Edit `plugins/ZyElevators/config.yml` if you want, then run `/zye reload`.

## Getting elevators

- **Craft them:** 8 blocks of the same type (for example white wool) around an ender pearl.
- **Give them:** `/zye give <player> <block> [amount]`, for example
  `/zye give Steve white_wool 16`.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/zyelevators give <player> <block> [amount]` | Give elevator items | `zyelevators.admin` |
| `/zyelevators reload` | Reload the config and language files | `zyelevators.admin` |
| `/zyelevators help` | Show the command list | `zyelevators.admin` |

Aliases: `/zye`, `/elevators`.

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `zyelevators.use` | Use elevators | everyone |
| `zyelevators.place` | Place elevator blocks | everyone |
| `zyelevators.craft` | Craft elevator blocks | everyone |
| `zyelevators.bypass.cooldown` | Skip the cooldown | op |
| `zyelevators.admin` | Use `/zyelevators give` and `/zyelevators reload` | op |

## Configuration

The default [`config.yml`](src/main/resources/config.yml) is commented. The main options:

```yaml
language: en

elevator:
  blocks: [WHITE_WOOL, RED_WOOL, ...]
  same-block-only: true
  max-distance: 64
  cooldown: 1.0
  jump-to-go-up: true
  sneak-to-go-down: true
  safe-destination: true
  explosion-proof: true
  disabled-worlds: []

crafting:
  enabled: true
  center: ENDER_PEARL
  amount: 1
```

### Languages

Messages live in `plugins/ZyElevators/lang/`. To add a language, copy `en.yml` to a new
file (for example `fr.yml`), translate it and set `language: fr` in `config.yml`. Any
message you leave out falls back to English.

## Building

```bash
mvn package
```

The jar is created in `target/`.

## Español

ZyElevators es un plugin de elevadores para Paper y Folia: salta para subir y agáchate
para bajar. Incluye los mensajes en español; pon `language: es` en `config.yml` y usa
`/zye reload`.

## License

[MIT](LICENSE)
