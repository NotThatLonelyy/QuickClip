package dev.quickclip.screen;

import net.minecraft.client.gui.GuiScreen;

/**
 * Public settings entry point backed by the V1-0.09 parity composition.
 */
public final class QuickClipSettingsScreen
        extends QuickClipSettingsScreenV109 {
    public QuickClipSettingsScreen() {
        this(null);
    }

    public QuickClipSettingsScreen(GuiScreen parent) {
        super(parent);
    }
}
