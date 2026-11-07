package dev.quickclip;

/**
 * QuickClip no longer draws a live HUD watermark. The logo, server address
 * and date/time are burned into saved clips by {@link ClipOverlay}.
 */
public final class DefaultWatermark {
    private DefaultWatermark() {
    }

    public static void ensureSeeded() {
        QuickClipConfig config = QuickClipConfig.get();
        config.watermarkSlots.clear();
        config.watermarksEnabled = false;
        config.showWatermarksLive = false;
    }
}
