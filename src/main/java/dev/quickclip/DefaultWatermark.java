package dev.quickclip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Installs the bundled QuickClip logo as the default bottom-right image
 * watermark on first launch. Afterwards the user's own watermark settings are
 * never overwritten.
 */
public final class DefaultWatermark {
    private static final String RESOURCE = "/assets/quickclip/watermark.png";
    private static final String FILE_NAME = "quickclip-logo.png";

    private DefaultWatermark() {
    }

    public static void ensureSeeded() {
        QuickClipConfig config = QuickClipConfig.get();
        // Re-apply the fixed branding policy on startup so old editable
        // watermark configurations are migrated automatically.
        try (InputStream in =
                DefaultWatermark.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                QuickClipMod.LOGGER.warn(
                        "[QuickClip] Bundled logo resource is missing.");
                return;
            }
            Path target = WatermarkImageStore.getWatermarksDir()
                    .resolve(FILE_NAME);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            // QuickClip branding is intentionally fixed: one logo, bottom-right,
            // 50% opacity. It is baked into recordings only (not a permanent HUD).
            config.watermarkSlots.clear();
            WatermarkSlot slot = new WatermarkSlot();
            slot.enabled = true;
            slot.name = "QuickClip";
            slot.kind = WatermarkSlot.Kind.IMAGE;
            slot.imagePath = FILE_NAME;
            slot.position = WatermarkSlot.Position.BOTTOM_RIGHT;
            slot.opacity = 50;
            slot.scale = 100;
            slot.padding = 8;
            config.watermarkSlots.add(slot);
            config.watermarksEnabled = true;
            config.showWatermarksLive = false;
            config.defaultWatermarkSeeded = true;
            config.save();
        } catch (IOException | RuntimeException exception) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not install default watermark: {}",
                    exception.getMessage());
        }
    }
}
