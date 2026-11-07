package dev.quickclip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Burned-in layer applied to replay frames only while a clip is saved:
 * server address (top), QuickClip logo (10% of video height, 50% opacity)
 * and date/time (bottom), right-aligned in the bottom-right corner.
 * Nothing is drawn in the live game HUD.
 */
public final class ClipOverlay {
    private static final String LOGO = "/assets/quickclip/watermark.png";

    private final int width;
    private final int height;
    private final int boxX;
    private final int boxY;
    private final int boxW;
    private final int boxH;
    private final int[] pixels; // ARGB of the bounding box

    private ClipOverlay(int width, int height, int boxX, int boxY,
            int boxW, int boxH, int[] pixels) {
        this.width = width;
        this.height = height;
        this.boxX = boxX;
        this.boxY = boxY;
        this.boxW = boxW;
        this.boxH = boxH;
        this.pixels = pixels;
    }

    /** Builds the overlay for one clip, or null if it cannot be drawn. */
    public static ClipOverlay create(int width, int height) {
        try {
            BufferedImage logo;
            try (InputStream in =
                    ClipOverlay.class.getResourceAsStream(LOGO)) {
                if (in == null) return null;
                logo = ImageIO.read(in);
            }
            if (logo == null) return null;

            int logoH = Math.max(8, Math.round(height * 0.10F));
            int logoW = Math.max(1,
                    Math.round(logo.getWidth() * (logoH
                            / (float) logo.getHeight())));
            int pad = Math.max(4, Math.round(height * 0.02F));
            String top = serverText();
            String bottom = new SimpleDateFormat("yyyy-MM-dd HH:mm")
                    .format(new Date());

            BufferedImage canvas = new BufferedImage(
                    width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                // Shrink the text until the widest element fits in the
                // right 40% of the frame.
                int fontSize = Math.max(10, Math.round(height * 0.022F));
                FontMetrics fm;
                int widest;
                while (true) {
                    g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                    fm = g.getFontMetrics();
                    widest = Math.max(logoW, Math.max(
                            fm.stringWidth(top), fm.stringWidth(bottom)));
                    if (widest <= width * 0.40F || fontSize <= 8) break;
                    fontSize--;
                }
                // One shared centre line for logo and both text lines,
                // clamped so nothing leaves the frame.
                float centerX = width - pad - widest / 2.0F;
                centerX = Math.max(pad + widest / 2.0F, centerX);

                int bottomY = height - pad - fm.getDescent();
                int logoY = bottomY - fm.getAscent() - 2 - logoH;
                int topY = logoY - 2 - fm.getDescent();

                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.5F));
                g.drawImage(logo, Math.round(centerX - logoW / 2.0F), logoY,
                        logoW, logoH, null);
                drawCentered(g, fm, top, centerX, topY);
                drawCentered(g, fm, bottom, centerX, bottomY);
            } finally {
                g.dispose();
            }

            int[] all = canvas.getRGB(0, 0, width, height, null, 0, width);
            int minX = width, minY = height, maxX = -1, maxY = -1;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if ((all[y * width + x] >>> 24) != 0) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
            if (maxX < minX || maxY < minY) return null;
            int bw = maxX - minX + 1;
            int bh = maxY - minY + 1;
            int[] box = new int[bw * bh];
            for (int y = 0; y < bh; y++) {
                System.arraycopy(all, (minY + y) * width + minX,
                        box, y * bw, bw);
            }
            return new ClipOverlay(width, height, minX, minY, bw, bh, box);
        } catch (Throwable throwable) {
            QuickClipMod.LOGGER.warn(
                    "[QuickClip] Could not build clip overlay: {}",
                    throwable.getMessage());
            return null;
        }
    }

    private static void drawCentered(Graphics2D g, FontMetrics fm,
            String text, float centerX, int baseline) {
        int x = Math.round(centerX - fm.stringWidth(text) / 2.0F);
        g.setColor(new Color(40, 40, 40));
        g.drawString(text, x + 1, baseline + 1);
        g.setColor(new Color(170, 170, 170));
        g.drawString(text, x, baseline);
    }

    private static String serverText() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            ServerData data = mc.getCurrentServerData();
            if (data != null && data.serverIP != null
                    && !data.serverIP.isEmpty()) {
                return data.serverIP;
            }
            return mc.isSingleplayer() ? "Singleplayer" : "Offline";
        } catch (Throwable throwable) {
            return "";
        }
    }

    /** Blends the overlay into one RGB24 frame, in place. */
    public void apply(byte[] rgb) {
        if (rgb.length < width * height * 3) return;
        for (int y = 0; y < boxH; y++) {
            int src = y * boxW;
            int dst = ((boxY + y) * width + boxX) * 3;
            for (int x = 0; x < boxW; x++, dst += 3) {
                int argb = pixels[src + x];
                int a = argb >>> 24;
                if (a == 0) continue;
                int r = (argb >> 16) & 0xFF;
                int gr = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int inv = 255 - a;
                rgb[dst] = (byte) ((r * a + (rgb[dst] & 0xFF) * inv) / 255);
                rgb[dst + 1] = (byte) ((gr * a
                        + (rgb[dst + 1] & 0xFF) * inv) / 255);
                rgb[dst + 2] = (byte) ((b * a
                        + (rgb[dst + 2] & 0xFF) * inv) / 255);
            }
        }
    }
}
