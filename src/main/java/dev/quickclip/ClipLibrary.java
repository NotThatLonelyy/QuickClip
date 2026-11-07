package dev.quickclip;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Folder organisation for saved clips. Folders are real directories under the
 * clip root, so they are identical in Windows File Explorer. Moving or
 * renaming a clip moves the real file and its notes sidecar.
 */
public final class ClipLibrary {
    public static final String UNSORTED = "Unsorted";

    private ClipLibrary() {
    }

    /** The folder that contains Unsorted and all user folders. */
    public static Path getRoot(QuickClipConfig config) {
        Path output = config.getOutputDirectory();
        Path name = output.getFileName();
        if (name != null && UNSORTED.equalsIgnoreCase(name.toString())
                && output.getParent() != null) {
            return output.getParent();
        }
        return output;
    }

    public static void ensureLayout(QuickClipConfig config) {
        try {
            Files.createDirectories(getRoot(config).resolve(UNSORTED));
            Files.createDirectories(config.getOutputDirectory());
        } catch (IOException exception) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not create clip folders: {}",
                    exception.getMessage());
        }
    }

    /** All folders under the root (any depth), Unsorted first. */
    public static List<Path> listFolders(QuickClipConfig config) {
        Path root = getRoot(config);
        List<Path> folders = new ArrayList<Path>();
        try (Stream<Path> stream = Files.walk(root, 6)) {
            stream.filter(p -> !p.equals(root) && Files.isDirectory(p))
                    .forEach(folders::add);
        } catch (IOException | RuntimeException exception) {
            return folders;
        }
        final Path base = root;
        Collections.sort(folders, (x, y) -> {
            String a1 = base.relativize(x).toString();
            String b1 = base.relativize(y).toString();
            boolean au = UNSORTED.equalsIgnoreCase(a1);
            boolean bu = UNSORTED.equalsIgnoreCase(b1);
            if (au != bu) return au ? -1 : 1;
            return a1.compareToIgnoreCase(b1);
        });
        return folders;
    }

    /** Direct sub-folders of a folder, sorted by name. */
    public static List<Path> listChildFolders(Path folder) {
        List<Path> out = new ArrayList<Path>();
        try (Stream<Path> stream = Files.list(folder)) {
            stream.filter(Files::isDirectory).forEach(out::add);
        } catch (IOException | RuntimeException exception) {
            return out;
        }
        Collections.sort(out, (x, y) -> x.getFileName().toString()
                .compareToIgnoreCase(y.getFileName().toString()));
        return out;
    }

    /** Number of video files directly inside a folder. */
    public static int countClips(Path folder) {
        try (Stream<Path> stream = Files.list(folder)) {
            return (int) stream.filter(p -> Files.isRegularFile(p)
                    && p.getFileName().toString().toLowerCase(
                            java.util.Locale.ROOT).endsWith(".mp4")).count();
        } catch (IOException | RuntimeException exception) {
            return 0;
        }
    }

    public static Path createFolder(Path parent, String name)
            throws IOException {
        String clean = sanitizeName(name);
        if (clean.isEmpty()) {
            throw new IOException("Invalid folder name.");
        }
        Path folder = parent.resolve(clean);
        if (Files.exists(folder)) {
            throw new IOException("A folder named '" + clean + "' exists.");
        }
        Files.createDirectories(folder);
        return folder;
    }

    public static Path renameFolder(Path folder, String name)
            throws IOException {
        String clean = sanitizeName(name);
        if (clean.isEmpty()) {
            throw new IOException("Invalid folder name.");
        }
        Path target = folder.resolveSibling(clean);
        if (target.equals(folder)) return folder;
        if (Files.exists(target)) {
            throw new IOException("A folder named '" + clean + "' exists.");
        }
        moveFile(folder, target);
        return target;
    }

    public static void deleteEmptyFolder(Path folder) throws IOException {
        try (Stream<Path> stream = Files.list(folder)) {
            if (stream.findAny().isPresent()) {
                throw new IOException(
                        "Folder is not empty. Move or delete its contents.");
            }
        }
        Files.delete(folder);
    }

    /** Folder name shown for a clip, relative to the root. */
    public static String folderLabel(QuickClipConfig config, Path file) {
        Path root = getRoot(config);
        Path parent = file.getParent();
        if (parent == null) return UNSORTED;
        if (parent.equals(root)) return "(root)";
        try {
            return root.relativize(parent).toString().replace('\\', '/');
        } catch (IllegalArgumentException exception) {
            return parent.getFileName().toString();
        }
    }

    public static String sanitizeName(String raw) {
        if (raw == null) return "";
        String value = raw
                .replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "")
                .replaceAll("[ .]+$", "")
                .trim();
        if (value.equals(".") || value.equals("..")) return "";
        String upper = value.toUpperCase(java.util.Locale.ROOT);
        if (upper.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) return "";
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    public static Path createFolder(QuickClipConfig config, String name)
            throws IOException {
        String clean = sanitizeName(name);
        if (clean.isEmpty()) {
            throw new IOException("Invalid folder name.");
        }
        Path folder = getRoot(config).resolve(clean);
        if (Files.exists(folder)) {
            throw new IOException("A folder named '" + clean + "' exists.");
        }
        Files.createDirectories(folder);
        return folder;
    }

    /** Moves a clip into a folder; never overwrites. Returns the new path. */
    public static Path move(Path file, Path folder) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Clip no longer exists.");
        }
        if (!Files.isDirectory(folder)) {
            throw new IOException("Destination folder is missing.");
        }
        if (folder.equals(file.getParent())) {
            return file;
        }
        Path target = uniqueTarget(folder, file.getFileName().toString());
        moveFile(file, target);
        moveSidecar(file, target);
        return target;
    }

    /** Renames a clip (extension kept). Fails if the name is taken. */
    public static Path rename(Path file, String newBaseName)
            throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Clip no longer exists.");
        }
        String clean = sanitizeName(newBaseName);
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot > 0 ? name.substring(dot) : "";
        if (clean.toLowerCase(java.util.Locale.ROOT)
                .endsWith(extension.toLowerCase(java.util.Locale.ROOT))
                && !extension.isEmpty()) {
            clean = clean.substring(0, clean.length() - extension.length());
        }
        if (clean.isEmpty()) {
            throw new IOException("Invalid name.");
        }
        Path target = file.resolveSibling(clean + extension);
        if (target.equals(file)) {
            return file;
        }
        if (Files.exists(target)) {
            throw new IOException("A clip with that name already exists.");
        }
        moveFile(file, target);
        moveSidecar(file, target);
        return target;
    }

    /** Removes the notes sidecar after a clip has been deleted. */
    public static void deleteSidecar(Path file) {
        try {
            Files.deleteIfExists(ClipNotes.sidecar(file));
        } catch (IOException ignored) {
            // leftover sidecar is harmless
        }
    }

    /** Cheap change detector for external Explorer edits. */
    public static long fingerprint(QuickClipConfig config) {
        Path root = getRoot(config);
        long[] hash = {17L};
        try (Stream<Path> stream = Files.walk(root, 6)) {
            stream.forEach(path -> {
                try {
                    hash[0] = hash[0] * 31L + path.toString().hashCode();
                    hash[0] = hash[0] * 31L
                            + Files.getLastModifiedTime(path).toMillis();
                } catch (IOException ignored) {
                    hash[0] += 1L;
                }
            });
        } catch (IOException | RuntimeException exception) {
            return -1L;
        }
        return hash[0];
    }

    private static Path uniqueTarget(Path folder, String filename) {
        Path target = folder.resolve(filename);
        if (!Files.exists(target)) return target;
        int dot = filename.lastIndexOf('.');
        String stem = dot > 0 ? filename.substring(0, dot) : filename;
        String ext = dot > 0 ? filename.substring(dot) : "";
        for (int i = 2; i < 10000; i++) {
            target = folder.resolve(stem + " (" + i + ")" + ext);
            if (!Files.exists(target)) return target;
        }
        return folder.resolve(stem + "-" + System.nanoTime() + ext);
    }

    private static void moveFile(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(from, to);
        }
    }

    private static void moveSidecar(Path oldVideo, Path newVideo) {
        Path from = ClipNotes.sidecar(oldVideo);
        if (!Files.isRegularFile(from)) return;
        try {
            Files.move(from, ClipNotes.sidecar(newVideo),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not move clip notes: {}",
                    exception.getMessage());
        }
    }
}
