package dev.quickclip.screen;

import dev.quickclip.ClipLibrary;
import dev.quickclip.QuickClipConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Lets the user choose the destination folder for a clip. */
public final class ClipFolderPickerScreen extends GuiScreen {
    private static final int PAGE_SIZE = 7;
    private final GuiScreen parent;
    private final Consumer<Path> onPick;
    private List<Path> folders;
    private int page;

    public ClipFolderPickerScreen(GuiScreen parent, Consumer<Path> onPick) {
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    public void initGui() {
        folders = ClipLibrary.listFolders(QuickClipConfig.get());
        buttonList.clear();
        int top = height / 2 - 80;
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < folders.size(); i++) {
            buttonList.add(new GuiButton(100 + i, width / 2 - 100,
                    top + i * 22, 200, 20,
                    folders.get(start + i).getFileName().toString()));
        }
        int bottom = top + PAGE_SIZE * 22 + 4;
        GuiButton prev = new GuiButton(1, width / 2 - 100, bottom, 60, 20,
                "< Prev");
        GuiButton next = new GuiButton(2, width / 2 + 40, bottom, 60, 20,
                "Next >");
        prev.enabled = page > 0;
        next.enabled = start + PAGE_SIZE < folders.size();
        buttonList.add(prev);
        buttonList.add(next);
        buttonList.add(new GuiButton(3, width / 2 - 35, bottom, 70, 20,
                "Cancel"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            page--;
            initGui();
        } else if (button.id == 2) {
            page++;
            initGui();
        } else if (button.id == 3) {
            mc.displayGuiScreen(parent);
        } else if (button.id >= 100) {
            int index = page * PAGE_SIZE + (button.id - 100);
            if (index >= 0 && index < folders.size()) {
                Path chosen = folders.get(index);
                mc.displayGuiScreen(parent);
                onPick.accept(chosen);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Move clip to folder",
                width / 2, height / 2 - 98, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
