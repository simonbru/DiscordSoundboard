# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A Discord soundboard bot: a Spring Boot 2.6 app (Java 17 toolchain) that logs into Discord through JDA 6, plays audio files with lavaplayer, and serves a single-page web UI and REST API on port 8080. Users trigger sounds from chat commands (`?<soundname>`), slash commands (`/play`, `/listsounds`), the web UI, or entrance/leave events.

## Commands

```bash
./gradlew build                 # compile + run tests
./gradlew test                  # run all tests (JUnit 5 + Mockito)
./gradlew test --tests 'net.dirtydeeds.discordsoundboard.commands.HelpCommandTest'   # single test class
./gradlew test --tests '*CommandEventTest.someMethod'                                  # single test method
./gradlew bootRun               # run locally (needs bot_token, see Configuration)
./gradlew assembleBootDist      # release zip -> build/distributions/DiscordSoundboard-<version>.zip
docker compose up --build       # build and run the image from ./Dockerfile
```

There is no linter. The version lives in `gradle.properties` (`projectVersion`) and gets substituted into `application.yml` during `processResources`.

## Configuration

- `src/main/resources/application.yml` holds internal Spring config: the H2 file DB at `./discordDB/`, `ddl-auto: update`, and Spring Data REST under `/api`.
- User config lives in `application.properties`. The template is `src/main/resources/application.properties.example`, and `BotConfig` reads it through `@Value`. `bot_token` and `username_to_join_channel` are required. `application.properties` is excluded from the boot jar, so it must sit in the working directory (a root-level `application.properties` is used for local runs).
- Sounds are read from `sounds_directory`, which defaults to `./sounds/`. Subdirectories become categories.
- Setting `spring.main.web-application-type=none` disables the web UI. `ServletWebServerApplicationContext` is then injected as `Optional`, so code must handle its absence.
- Docker: `docker-entrypoint.sh` uses `sed` to patch `application.properties` from the env vars `bottoken`, `username` and `commandcharacter`, then starts the jar with `--spring.config.location` pointing at `/etc/DiscordSoundboard/config/`.

## Architecture

All code lives under `src/main/java/net/dirtydeeds/discordsoundboard/`.

- **`SoundPlayer`** is the central singleton and the hub of the app. Its constructor (`init()`) builds the `JDABot`, registers the guild slash commands, syncs sound files and users into the DB, registers every chat `Command` with `CommandListener`, attaches all JDA event listeners, and starts `MainWatch`. Almost every command, listener and controller takes a `SoundPlayer` and calls its play, stop, volume and disconnect methods. If the bot token is missing, it calls `ShutdownManager` to exit.
- **`JDABot`** builds the JDA client (gateway intents, member cache, auto-reconnect) and configures **DAVE E2EE voice** through `moe.kyokobot.libdave` (`LDJDADaveSessionFactory`). Discord voice playback needs this. The only native library in the build is `natives-linux-x86-64`, so playback works only on Linux x86-64 unless more natives are added.
- **Audio**: `handlers/PlayerManager` wraps lavaplayer's `AudioPlayerManager`. `handlers/AudioHandler` is the per-guild `AudioSendHandler`, which `SoundPlayer` retrieves with `guild.getAudioManager().getSendingHandler()`. `FileLoadResultHandler` handles lavaplayer load callbacks.
- **Chat commands**: each command in `commands/` subclasses `Command` (`name`, `help`, `execute(CommandEvent)`) and is registered manually in `SoundPlayer.init()`. `listeners/CommandListener` parses the prefix (`command_character`), checks the allowed and banned user lists, and dispatches. If no command matches a name, the input is treated as a sound to play. To add a command, create the class and register it in `init()`.
- **Event listeners** (`listeners/`) are plain JDA `ListenerAdapter`s, not Spring beans: entrance and leave sounds, channel moves, the bot leaving an empty channel, DM file attachments (saved into the sounds directory), and slash commands.
- **Persistence**: JPA entities `beans/SoundFile` and `beans/User`, stored in H2. Spring Data REST exposes the repositories under `/api` with IDs included (`MainController.configureRepositoryRestConfiguration`). `MainWatch` is an `@Async` `WatchService` loop on the sounds directory that calls `SoundPlayer.updateFileList()` when files change.
- **Web**: `controllers/BotCommandController` (`/bot/playFile`, `/playUrl`, `/random`, `/stop`, `/volume`, `/channels`), plus `SoundController` and `UserController` (paged `/api/.../findAll`). The UI is a single static file, `src/main/webapp/index.html`, which the distribution copies into `bin/static`. springdoc-openapi is included.

## Dependency gotchas (see `build.gradle`)

- JDA 6 pulls in SLF4J 2.x and a newer snakeyaml. Both are forced down (`slf4j-api:1.7.36`, `snakeyaml:1.28`) to stay compatible with Spring Boot 2.6 and logback 1.2. Upgrading JDA or Spring means revisiting these overrides.
- Extra repos: jitpack (lavaplayer transitive deps) and `maven.lavalink.dev` (libdave).
- Lombok is used throughout (`@Getter`, etc.).
- The GitHub CI workflow (`.github/workflows/gradle.yml`) still sets up JDK 1.8, even though the toolchain requires 17.
