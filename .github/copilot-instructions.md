# Review instructions for ZyElevators

ZyElevators is a Minecraft server plugin for **Paper and Folia**, written in Java 21 and built with Maven.
Players jump on an elevator block to go up and sneak to go down.

## Check the pull request description

The description must follow `.github/pull_request_template.md`:

- **Summary** explains what changes and why.
- **Type of change** has at least one box checked, and the right one.
- **How was it tested?** names the server software and version and what was tried in game.
  "Not tested" or a vague answer is not enough for changes to gameplay or threading.
- **Checklist**: every item is checked or deleted.

If something is missing or doesn't match the code (for example "Documentation" is checked but Java code changed),
say so in the review.

## Rules for the code

- **Oldest supported version: Minecraft 1.21.4.** The code compiles against `paper-api` 1.21.4. Flag any API that
  only exists in newer versions, and any change to the `paper-api` version in `pom.xml`.
- **Folia threading:**
  - Never use `Bukkit.getScheduler()` or `BukkitRunnable`. Use the region, entity, global or async schedulers.
  - Blocks, chunks and worlds may only be read or changed on the thread that owns that region. An entity may only
    be changed on its own scheduler.
  - State shared between players must be thread-safe (for example `ConcurrentHashMap`).
  - No blocking I/O on region threads.
- **Saved data:** elevator positions are stored in the chunk's PersistentDataContainer by `ElevatorStore`, packed
  into ints. Changing the key or the packing format breaks elevators on existing worlds; that needs a migration.
- **Settings:** `config/Settings` is an immutable record that is swapped on reload. Every new option needs a safe
  default in `Settings.load`, a comment in `config.yml` and a line in the README.
- **Messages:** text shown to players or in commands goes in `lang/en.yml` **and** `lang/es.yml` (MiniMessage),
  never as a string in the Java code. Console log messages may stay in English in the code.
- **Permissions:** new permissions are declared in `plugin.yml` and listed in the README.
- **Item and block safety:** look for ways to duplicate items, or to get elevator items or the blocks used in the
  recipe for free (breaking, explosions, pistons, crafting, creative mode).

## Style

- Keep changes small and focused. Prefer clear code over clever code.
- Write review comments in English, short and specific, with a suggested fix when possible.
