# Contributing to ZyElevators

Thanks for helping! Bug reports, ideas and pull requests are welcome.

## Reporting a bug or suggesting a feature

Open an [issue](https://github.com/Skzyyx/ZyElevators/issues/new/choose) and fill in the form.

## Pull requests

`main` is protected: every change goes through a pull request.

1. Fork the repository and create a branch from `main`.
2. Make your change. Keep it small and focused on one thing.
3. Build it with `mvn package` (Java 21).
4. Test it on a server, ideally on **Folia** as well as Paper.
5. Open the pull request and fill in the template. A check fails if a section is missing or empty.

Before merging, a pull request needs:

- the **build** and **pr-format** checks passing,
- the automatic Copilot review, with every review conversation resolved,
- to be up to date with `main`.

Pull requests are merged with squash.

## Code guidelines

- Folia first: use the region, entity or global schedulers, never `Bukkit.getScheduler()`.
- The code must keep compiling against Paper API **1.21.4**, the oldest supported version.
- Player-facing text goes in `lang/en.yml` and `lang/es.yml`, not in the Java code.
- New config options need a default, a comment in `config.yml` and a mention in the README.
