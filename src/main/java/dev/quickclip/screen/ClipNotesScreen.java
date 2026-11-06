package dev.quickclip.screen;

import dev.quickclip.ClipNotes;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.nio.file.Path;

/** Edit the optional note and uploaded-link for a clip. */
public final class ClipNotesScreen extends GuiScreen {
    private final GuiScreen parent;
    private final Path video;
    private final Runnable onSaved;
    private GuiTextField noteField;
    private GuiTextField linkField;
    private String status = "";

    public ClipNotesScreen(GuiScreen parent, Path video, Runnable onSaved) {
        this.parent = parent;
        this.video = video;
        this.onSaved = onSaved;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        ClipNotes existing = ClipNotes.read(video);
        String note = noteField == null ? existing.note : noteField.getText();
        String link = linkField == null ? existing.link : linkField.getText();
        buttonList.clear();
        int x = width / 2 - 150;
        int y = height / 2 - 40;
        noteField = new GuiTextField(0, fontRendererObj, x, y, 300, 20);
        noteField.setMaxStringLength(ClipNotes.MAX_LENGTH);
        noteField.setText(note);
        noteField.setFocused(true);
        linkField = new GuiTextField(1, fontRendererObj, x, y + 40, 300, 20);
        linkField.setMaxStringLength(ClipNotes.MAX_LENGTH);
        linkField.setText(link);
        buttonList.add(new GuiButton(1, x, y + 70, 96, 20, "Save"));
        buttonList.add(new GuiButton(2, x + 102, y + 70, 96, 20,
                "Open Link"));
        buttonList.add(new GuiButton(3, x + 204, y + 70, 96, 20, "Cancel"));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        noteField.updateCursorCounter();
        linkField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            String link = linkField.getText().trim();
            if (!link.isEmpty() && !ClipNotes.isWebLink(link)) {
                status = "Link must start with http:// or https://";
                return;
            }
            if (!ClipNotes.write(video, noteField.getText(), link)) {
                status = "Could not save notes.";
                return;
            }
            mc.displayGuiScreen(parent);
            if (onSaved != null) onSaved.run();
        } else if (button.id == 2) {
            if (!ClipNotes.openLink(linkField.getText())) {
                status = "No valid http(s) link to open.";
            }
        } else if (button.id == 3) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typed, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        } else if (code == Keyboard.KEY_TAB) {
            boolean noteFocused = noteField.isFocused();
            noteField.setFocused(!noteFocused);
            linkField.setFocused(noteFocused);
        } else {
            noteField.textboxKeyTyped(typed, code);
            linkField.textboxKeyTyped(typed, code);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        noteField.mouseClicked(x, y, button);
        linkField.mouseClicked(x, y, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = width / 2 - 150;
        int y = height / 2 - 40;
        drawCenteredString(fontRendererObj,
                "Clip notes: " + video.getFileName(), width / 2, y - 30,
                0xFFFFFF);
        drawString(fontRendererObj, "Note (optional)", x, y - 11, 0xAAAAAA);
        drawString(fontRendererObj, "Uploaded link (optional)", x, y + 29,
                0xAAAAAA);
        noteField.drawTextBox();
        linkField.drawTextBox();
        if (!status.isEmpty()) {
            drawCenteredString(fontRendererObj, status, width / 2,
                    y + 98, 0xFF5555);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
