QuickClip for Forge 1.8.9
===

**QuickClip** by NotThatLonely is an instant-replay mod for Minecraft 1.8.9
(Forge 11.15.1.2318, Java 8). It keeps a rolling buffer of recent gameplay and
saves the last seconds as an MP4 with game audio when you press the
save-replay hotkey (default `F8`). No manual recording is needed.

## Features

* Always-on replay buffer (default 30 s; 10-300 s slider in settings).
* Save-replay hotkey (`F8`, rebindable in Minecraft Controls).
* Game audio capture, existing quality and encoder settings.
* QuickClip PNG watermark burned into the MP4 (enable, position, scale,
  opacity in the Watermarks screen). Defaults to the bottom-right.
* Clip manager (`F12`): folders, move, rename, delete, play, open folder.
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
