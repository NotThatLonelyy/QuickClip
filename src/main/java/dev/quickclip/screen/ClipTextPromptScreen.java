package dev.quickclip.screen;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.function.Consumer;

/** Small single-line text prompt (new folder, rename clip). */
public final class ClipTextPromptScreen extends GuiScreen {
    private final GuiScreen parent;
    private final String title;
    private final String initial;
    private final Consumer<String> onConfirm;
    private GuiTextField field;

    public ClipTextPromptScreen(GuiScreen parent, String title,
            String initial, Consumer<String> onConfirm) {
        this.parent = parent;
        this.title = title;
        this.initial = initial == null ? "" : initial;
        this.onConfirm = onConfirm;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        field = new GuiTextField(0, fontRendererObj,
                width / 2 - 120, height / 2 - 10, 240, 20);
        field.setMaxStringLength(100);
        field.setText(initial);
        field.setFocused(true);
        buttonList.add(new GuiButton(1, width / 2 - 120, height / 2 + 20,
                118, 20, "OK"));
        buttonList.add(new GuiButton(2, width / 2 + 2, height / 2 + 20,
                118, 20, "Cancel"));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        field.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            confirm();
        } else if (button.id == 2) {
            mc.displayGuiScreen(parent);
        }
    }

    private void confirm() {
        String value = field.getText();
        mc.displayGuiScreen(parent);
        onConfirm.accept(value);
    }

    @Override
    protected void keyTyped(char typed, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        } else if (code == Keyboard.KEY_RETURN
                || code == Keyboard.KEY_NUMPADENTER) {
            confirm();
        } else {
            field.textboxKeyTyped(typed, code);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        field.mouseClicked(x, y, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, title, width / 2,
                height / 2 - 30, 0xFFFFFF);
        field.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
