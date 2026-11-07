QuickClip for Forge 1.8.9
===

**QuickClip** by NotThatLonely is an instant-replay mod for Minecraft 1.8.9
(Forge 11.15.1.2318, Java 8). It keeps a rolling buffer of recent gameplay and
saves the last seconds as an MP4 with game audio when you press the
save-replay hotkey (default `F8`). No manual recording is needed.

## Features

* Always-on replay buffer (default 30 s). Minimal settings: replay length,
  resolution (480p/720p/1080p), frame rate (30/60), game audio volume,
  capture while in menus (off by default).
* Save-replay hotkey (`F8`, rebindable in Minecraft Controls).
* Game audio capture, existing quality and encoder settings.
* Saved clips get a burned-in layer: server address on top, QuickClip logo
  (10% of video height, 50% opacity) and date/time below, bottom-right. Nothing
  is drawn in the live HUD.
* Clip manager (`F12`): notes-app style folders (nested, any depth) with
  icons, then per-folder clip lists: move, rename, delete, play, notes.
  Folders are real directories, so they match Windows File Explorer, and the
  list refreshes when files change outside the game.
* Optional note and optional uploaded link for each clip (`Notes` / `Link`).

## Install

1. Install Forge 1.8.9 (`11.15.1.2318`).
2. Put `quickclip-<version>-forge-1.8.9.jar` in your `mods` folder.
3. Launch Minecraft and follow the one-time FFmpeg setup prompt.

## Where files go

```
.minecraft/QuickClip/
  Clips/Unsorted/   new clips are saved here by default
  Clips/<folders>/  your own folders
  config/quickclip.json
  watermarks/
  ffmpeg/
```
The clip folder is configurable in settings. Notes are stored next to a clip
as `<clip>.mp4.qcnote` and move/rename/delete with it.

## Build

```
./gradlew clean build
```
Gradle needs JDK 17; compilation uses the Java 8 toolchain. The workflow
`.github/workflows/build.yml` uploads the JAR as the `QuickClip-1.8.9`
artifact (Actions -> run -> Artifacts).

## License and attribution

MIT. See [LICENSE](LICENSE) and [UPSTREAM.md](UPSTREAM.md) for provenance.
