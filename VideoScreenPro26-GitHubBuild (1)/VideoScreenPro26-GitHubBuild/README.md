# VideoScreen Pro – Paper 26.3 (experimental source project)

Requirements: Paper 26.3, JDK 25, Gradle 8.14+ (or compatible), FFmpeg on PATH. yt-dlp on PATH for YouTube/Twitch URLs.

Build: `gradle build` and copy `build/libs/VideoScreenPro-1.0.0.jar` to `plugins/`. This project has NOT been compiled or tested in this environment.

Resource pack: ZIP the *contents* of `resourcepack/` (not its enclosing folder), host as a Minecraft server resource pack, configure `server.properties` `resource-pack` and `resource-pack-sha1`. The custom item uses `minecraft:item_model=videoscreenpro:remote` on a carrot-on-a-stick. The resource pack includes a simple volumetric 3D cuboid remote model with texture.

## Commands (OP)
- `/screen remote` – receive remote item
- `/screen create cinema 3 2` – create frame screen at player position; requires suitable supporting blocks
- `/screen select cinema`
- `/screen play film.mp4` – play local video from `plugins/VideoScreenPro/videos/`
- `/screen live https://example.com/live.m3u8` – play a direct stream
- `/screen play https://www.youtube.com/watch?v=...` – resolve with yt-dlp
- `/screen live https://www.twitch.tv/channel` – resolve with yt-dlp
- `/screen stop`
- `/screen resize 4 3`
- `/screen remove cinema`
- `/screen list`

## Limitations / safety
- This is an experimental implementation, NOT a production-ready fully tested plugin. No synchronized audio, pause/resume, seeking or video history; GUI controls stop only and explains chat commands for other actions.
- The remote has a basic 3D cuboid model, not a polished Blockbench model.
- Screen positions and map IDs are saved; video playback is not resumed after restarts.
- Item frames need support blocks; frames may fail to spawn in unsupported locations. Screen rotation/orientation should be tested in-game.
- Arbitrary URLs from trusted operators are passed to FFmpeg/yt-dlp and can access internal networks. Use only trusted operators and restrict network egress on the host. Do not expose this command to untrusted players.
- YouTube/Twitch extraction depends on yt-dlp and platform availability, may break or be blocked; DRM/paywalled streams not supported.
- Map render updates are CPU/network-intensive. Default 4 fps; test on a staging server first.
- To avoid incomplete or corrupted output, use a backup of your world before testing.
