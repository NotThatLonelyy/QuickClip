package dev.quickclip;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small preview image stored next to a clip ({@code clip.mp4.qcthumb.png}). */
public final class ClipThumbnail {
    public static final String SUFFIX = ".qcthumb.png";
    private static final int WIDTH = 224;

    private ClipThumbnail() {
    }

    public static Path sidecar(Path video) {
        return video.resolveSibling(video.getFileName() + SUFFIX);
    }

    /** Writes a scaled PNG from one RGB24 frame. Failures are logged only. */
    public static void write(byte[] rgb, int width, int height, Path target) {
        try {
            if (rgb == null || width <= 0 || height <= 0
                    || rgb.length < width * height * 3) {
                return;
            }
            BufferedImage full = new BufferedImage(
                    width, height, BufferedImage.TYPE_INT_RGB);
            int[] row = new int[width];
            for (int y = 0; y < height; y++) {
                int p = y * width * 3;
                for (int x = 0; x < width; x++, p += 3) {
                    row[x] = ((rgb[p] & 0xFF) << 16)
                            | ((rgb[p + 1] & 0xFF) << 8)
                            | (rgb[p + 2] & 0xFF);
                }
                full.setRGB(0, y, width, 1, row, 0, width);
            }
            int th = Math.max(2, Math.round(WIDTH * height / (float) width));
            BufferedImage small = new BufferedImage(
                    WIDTH, th, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = small.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(full, 0, 0, WIDTH, th, null);
            } finally {
                g.dispose();
            }
            try (java.io.OutputStream out = Files.newOutputStream(target)) {
                ImageIO.write(small, "png", out);
            }
        } catch (Throwable throwable) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not write thumbnail: {}",
                    throwable.getMessage());
        }
    }
}
