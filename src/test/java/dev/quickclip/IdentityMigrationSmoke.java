package dev.quickclip;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * No-network smoke coverage for the Record-able to QuickClip migration.
 */
public final class IdentityMigrationSmoke {
    private IdentityMigrationSmoke() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException(
                    "Expected a dedicated migration smoke directory.");
        }
        Path root = Paths.get(arguments[0]).toAbsolutePath().normalize();
        Path configDirectory = root.resolve("config");
        Files.createDirectories(configDirectory);

        Path legacyConfig = configDirectory.resolve("recordable.json");
        Files.write(
                legacyConfig,
                ("{\n"
                        + "  \"fps\": 30,\n"
                        + "  \"filenamePattern\": \"legacy-{datetime}\"\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));

        QuickClipConfig.initialize(root.toFile(), configDirectory.toFile());
        QuickClipConfig migrated = QuickClipConfig.get();
        if (migrated.fps != 30
                || !"legacy-{datetime}".equals(migrated.filenamePattern)) {
            throw new IllegalStateException(
                    "Legacy settings were not imported.");
        }
        if (!Files.isRegularFile(configDirectory.resolve("quickclip.json"))) {
            throw new IllegalStateException(
                    "Migrated QuickClip config was not written.");
        }
        if (!Files.isRegularFile(legacyConfig)) {
            throw new IllegalStateException(
                    "Legacy config should remain recoverable.");
        }

        Path dataRoot = root.resolve("data");
        Path legacyData = dataRoot.resolve("recordable");
        Files.createDirectories(legacyData);
        if (!legacyData.equals(QuickClipPaths.preferredDirectory(
                dataRoot,
                "quickclip",
                "recordable"))) {
            throw new IllegalStateException(
                    "Existing legacy data directory was not selected.");
        }
        Path currentData = dataRoot.resolve("quickclip");
        Files.createDirectories(currentData);
        if (!currentData.equals(QuickClipPaths.preferredDirectory(
                dataRoot,
                "quickclip",
                "recordable"))) {
            throw new IllegalStateException(
                    "QuickClip data directory did not take precedence.");
        }

        if (!QuickClipConfig.DEFAULT_FILENAME_PATTERN.startsWith(
                "quickclip-")) {
            throw new IllegalStateException(
                    "Default filename still has the old identity.");
        }
        System.out.println("QUICKCLIP_IDENTITY_MIGRATION_SMOKE_OK=" + root);
    }
}
