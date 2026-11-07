package dev.quickclip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * QuickClip's permanent watermark: the bundled logo, bottom-right, 50%
 * opacity, 10% of the screen height (sized in LiveEffectsRenderer). Enforced
 * on every launch; there is no user-facing watermark setting.
 */
public final class DefaultWatermark {
    private static final String RESOURCE = "/assets/quickclip/watermark.png";
    private static final String FILE_NAME = "quickclip-logo.png";

    private DefaultWatermark() {
    }

    public static void ensureSeeded() {
        QuickClipConfig config = QuickClipConfig.get();
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
            config.watermarkSlots.clear();
            WatermarkSlot slot = new WatermarkSlot();
            slot.enabled = true;
            slot.name = "QuickClip";
            slot.kind = WatermarkSlot.Kind.IMAGE;
            slot.imagePath = FILE_NAME;
            slot.position = WatermarkSlot.Position.BOTTOM_RIGHT;
            slot.opacity = 50;
            slot.scale = 100;
            config.watermarkSlots.add(slot);
            config.watermarksEnabled = true;
            config.showWatermarksLive = true;
        } catch (IOException | RuntimeException exception) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not install watermark: {}",
                    exception.getMessage());
        }
    }
}
