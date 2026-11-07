# Changelog

All notable changes to the unofficial Forge 1.8.9 port are recorded here.
Versions follow [Semantic Versioning](https://semver.org/).

## [1.1.0] - 2026-07-26

### Changed

- Renamed the Forge port, mod ID, Java packages, resources, JARs, releases,
  documentation, and repository identity to Recordish.
- Added safe compatibility fallbacks for legacy settings, managed FFmpeg,
  watermarks, replay recovery, thumbnails, and FFmpeg environment variables.
- Added an explicit duplicate-JAR guard for old `recordable-*.jar` installs.

## [1.0.0] - 2026-07-26

### Added

- Initial Forge 1.8.9 port of Record-able.
- In-game recording, replay buffering, game-audio and microphone capture.
- Managed FFmpeg setup, diagnostics, recovery, and the record-ish website.

[1.1.0]: https://github.com/ErDreiwen/recordish/compare/0b05278d90d3568011258a9826484ba3abee5363...v1.1.0
[1.0.0]: https://github.com/ErDreiwen/recordish/commit/0b05278d90d3568011258a9826484ba3abee5363

## Unreleased

- Rebranded to QuickClip (mod id `quickclip`, package `dev.quickclip`).
- Replay buffer on by default, save hotkey F8, clips in
  `QuickClip/Clips/Unsorted`, config in `QuickClip/config`.
- Default QuickClip logo watermark (bottom-right) seeded on first launch.
- Clip manager: new folder, move, rename, delete, external-change refresh.
- Optional per-clip note and uploaded link.
- Removed manual/auto recording keys, auto-record, microphone, auto clips,
  streamer mode and share controls (code paths are inert, not deleted).
- New workflow `build.yml` uploads artifact `QuickClip-1.8.9`.
- Replay buffer frame cap raised 30 -> 60 FPS; default quality High
  (720p/60). 120 FPS is not supported by the replay buffer.
- Settings reduced to replay essentials; watermark settings and unused
  screens (streamer, censor editor, theme, performance, diagnostics, overlay
  position, storage manager, watermark) deleted.
- Folder browser screen (nested folders) and notes shown under clip names.
- Folders and clips shown together in one list; per-clip view screens removed.
- Resolution (480p/720p/1080p) and frame rate are separate settings.
- Folders starting with "." are hidden and cannot be created.
- Watermark moved from live HUD to a save-time overlay (logo, server IP,
  date/time). "Capture while in menus" option added (default off).
- Clip thumbnails (saved next to each clip), centered overlay text in gray at 50%, no seconds; "capture join/disconnect screens" option (default off).
