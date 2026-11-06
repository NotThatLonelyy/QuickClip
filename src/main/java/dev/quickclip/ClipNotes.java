package dev.quickclip;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Optional note and uploaded-link metadata for one clip. Stored in a small
 * sidecar file next to the video ({@code clip.mp4.qcnote}) so it travels with
 * the video when it is moved or renamed through QuickClip.
 */
public final class ClipNotes {
    public static final String SIDECAR_SUFFIX = ".qcnote";
    public static final int MAX_LENGTH = 300;

    public final String note;
    public final String link;

    public ClipNotes(String note, String link) {
        this.note = note == null ? "" : note;
        this.link = link == null ? "" : link;
    }

    public boolean isEmpty() {
        return note.isEmpty() && link.isEmpty();
    }

    public static Path sidecar(Path video) {
        return video.resolveSibling(video.getFileName() + SIDECAR_SUFFIX);
    }

    public static ClipNotes read(Path video) {
        Path file = sidecar(video);
        if (!Files.isRegularFile(file)) {
            return new ClipNotes("", "");
        }
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                Files.newInputStream(file), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | RuntimeException exception) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not read clip notes {}: {}",
                    file, exception.getMessage());
            return new ClipNotes("", "");
        }
        return new ClipNotes(
                properties.getProperty("note", ""),
                properties.getProperty("link", ""));
    }

    /** Writes the sidecar, or deletes it when both fields are empty. */
    public static boolean write(Path video, String note, String link) {
        Path file = sidecar(video);
        String cleanNote = clean(note);
        String cleanLink = clean(link);
        try {
            if (cleanNote.isEmpty() && cleanLink.isEmpty()) {
                Files.deleteIfExists(file);
                return true;
            }
            Properties properties = new Properties();
            properties.setProperty("note", cleanNote);
            properties.setProperty("link", cleanLink);
            try (Writer writer = new OutputStreamWriter(
                    Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                properties.store(writer, "QuickClip clip notes");
            }
            return true;
        } catch (IOException exception) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not write clip notes {}: {}",
                    file, exception.getMessage());
            return false;
        }
    }

    public static boolean isWebLink(String value) {
        if (value == null) return false;
        String lower = value.trim().toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /** Opens an http(s) link in the default browser. */
    public static boolean openLink(String link) {
        if (!isWebLink(link)) return false;
        try {
            java.awt.Desktop.getDesktop().browse(
                    new java.net.URI(link.trim()));
            return true;
        } catch (Throwable throwable) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not open link: {}",
                    throwable.getMessage());
            return false;
        }
    }

    private static String clean(String value) {
        if (value == null) return "";
        String trimmed = value.replace('\r', ' ').replace('\n', ' ').trim();
        return trimmed.length() > MAX_LENGTH
                ? trimmed.substring(0, MAX_LENGTH)
                : trimmed;
    }
}
