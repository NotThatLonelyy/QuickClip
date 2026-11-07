package dev.quickclip.screen;

import dev.quickclip.PlatformUtils;
import dev.quickclip.QuickClipConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Minimal QuickClip settings: only the instant-replay essentials. */
public final class QuickClipSettingsScreen extends GuiScreen {
    private static final int[] LENGTHS = {10, 20, 30, 60};
    private static final String[] QUALITIES = {
        "high", "balanced", "performance"};
    private static final String[] QUALITY_LABELS = {
        "High (720p / 60 FPS)",
        "Balanced (720p / 30 FPS)",
        "Performance (480p / 30 FPS)"};
    private static final int[] VOLUMES = {0, 25, 50, 75, 100};

    private static final int ID_LENGTH = 1;
    private static final int ID_MINUS = 2;
    private static final int ID_PLUS = 3;
    private static final int ID_FPS = 4;
    private static final int ID_QUALITY = 5;
    private static final int ID_VOLUME = 6;
    private static final int ID_NOTIFY = 7;
    private static final int ID_FOLDER = 8;
    private static final int ID_DONE = 9;

    private final GuiScreen parent;

    public QuickClipSettingsScreen() {
        this(null);
    }

    public QuickClipSettingsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        QuickClipConfig c = QuickClipConfig.get();
        int x = width / 2 - 100;
        int y = height / 2 - 78;
        buttonList.add(new GuiButton(ID_MINUS, x, y, 24, 20, "-"));
        buttonList.add(new GuiButton(ID_LENGTH, x + 26, y, 148, 20,
                "Replay length: " + c.replayBufferDurationSeconds + "s"));
        buttonList.add(new GuiButton(ID_PLUS, x + 176, y, 24, 20, "+"));
        buttonList.add(new GuiButton(ID_FPS, x, y + 24, 200, 20,
                "Frame rate: " + c.fps + " FPS"));
        buttonList.add(new GuiButton(ID_QUALITY, x, y + 48, 200, 20,
                "Resolution: " + qualityLabel(c.replayBufferQuality)));
        buttonList.add(new GuiButton(ID_VOLUME, x, y + 72, 200, 20,
                "Game audio volume: " + c.gameAudioVolume + "%"));
        buttonList.add(new GuiButton(ID_NOTIFY, x, y + 96, 200, 20,
                "Save notification: "
                        + (c.replayBufferNotify ? "ON" : "OFF")));
        buttonList.add(new GuiButton(ID_FOLDER, x, y + 120, 200, 20,
                "Open clips folder"));
        buttonList.add(new GuiButton(ID_DONE, x, y + 148, 200, 20,
                "Done"));
    }

    private static String qualityLabel(String quality) {
        for (int i = 0; i < QUALITIES.length; i++) {
            if (QUALITIES[i].equals(quality)) return QUALITY_LABELS[i];
        }
        return QUALITY_LABELS[0];
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        QuickClipConfig c = QuickClipConfig.get();
        switch (button.id) {
            case ID_LENGTH: {
                int next = LENGTHS[0];
                for (int i = 0; i < LENGTHS.length; i++) {
                    if (c.replayBufferDurationSeconds < LENGTHS[i]) {
                        next = LENGTHS[i];
                        break;
                    }
                }
                c.replayBufferDurationSeconds = next;
                break;
            }
            case ID_MINUS:
                c.replayBufferDurationSeconds = Math.max(10,
                        c.replayBufferDurationSeconds - 10);
                break;
            case ID_PLUS:
                c.replayBufferDurationSeconds = Math.min(600,
                        c.replayBufferDurationSeconds + 10);
                break;
            case ID_FPS:
                c.fps = c.fps >= 60 ? 30 : 60;
                break;
            case ID_QUALITY: {
                int index = 0;
                for (int i = 0; i < QUALITIES.length; i++) {
                    if (QUALITIES[i].equals(c.replayBufferQuality)) {
                        index = i;
                    }
                }
                c.replayBufferQuality =
                        QUALITIES[(index + 1) % QUALITIES.length];
                break;
            }
            case ID_VOLUME: {
                int next = VOLUMES[0];
                for (int i = 0; i < VOLUMES.length; i++) {
                    if (c.gameAudioVolume < VOLUMES[i]) {
                        next = VOLUMES[i];
                        break;
                    }
                }
                c.gameAudioVolume = next;
                break;
            }
            case ID_NOTIFY:
                c.replayBufferNotify = !c.replayBufferNotify;
                break;
            case ID_FOLDER:
                PlatformUtils.open(c.getOutputDirectory());
                return;
            case ID_DONE:
                c.save();
                mc.displayGuiScreen(parent);
                return;
            default:
                return;
        }
        c.save();
        initGui();
    }

    @Override
    protected void keyTyped(char typed, int code)
            throws java.io.IOException {
        if (code == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
            QuickClipConfig.get().save();
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "QuickClip Settings",
                width / 2, height / 2 - 104, 0xFFFFFF);
        drawCenteredString(fontRendererObj,
                "Save hotkey: set in Controls (default F8). "
                        + "Changes apply right away.",
                width / 2, height / 2 + 98, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
