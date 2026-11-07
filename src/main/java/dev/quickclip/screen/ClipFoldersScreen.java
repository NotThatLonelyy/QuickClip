package dev.quickclip.screen;

import dev.quickclip.ClipLibrary;
import dev.quickclip.QuickClipConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Notes-app style folder browser. Shows the sub-folders of the current
 * folder (any depth) with a folder icon, name and counts. "Clips" opens the
 * clip list for the current folder.
 */
public final class ClipFoldersScreen extends GuiScreen {
    private static final int ROW_H = 26;
    private static final int ID_OPEN = 1;
    private static final int ID_UP = 2;
    private static final int ID_NEW = 3;
    private static final int ID_RENAME = 4;
    private static final int ID_DELETE = 5;
    private static final int ID_CLIPS = 6;
    private static final int ID_SETTINGS = 7;
    private static final int ID_CLOSE = 8;

    private final GuiScreen parent;
    private Path current;
    private List<Path> children = new ArrayList<Path>();
    private int selected = -1;
    private int scroll;
    private int listTop;
    private int listBottom;
    private long lastClickMs;
    private int lastClickRow = -1;
    private String status = "";
    private int refreshTicks;
    private long fingerprint = Long.MIN_VALUE;

    public ClipFoldersScreen(GuiScreen parent) {
        this.parent = parent;
        this.current = ClipLibrary.getRoot(QuickClipConfig.get());
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(false);
        ClipLibrary.ensureLayout(QuickClipConfig.get());
        reload();
        buttonList.clear();
        int cx = width / 2;
        int y1 = height - 52;
        int y2 = height - 28;
        listTop = 44;
        listBottom = y1 - 8;
        buttonList.add(new GuiButton(ID_UP, cx - 205, y1, 66, 20, "< Back"));
        buttonList.add(new GuiButton(ID_OPEN, cx - 137, y1, 66, 20, "Open"));
        buttonList.add(new GuiButton(ID_NEW, cx - 69, y1, 90, 20,
                "New Folder"));
        buttonList.add(new GuiButton(ID_RENAME, cx + 23, y1, 90, 20,
                "Rename"));
        buttonList.add(new GuiButton(ID_DELETE, cx + 115, y1, 90, 20,
                "Delete"));
        buttonList.add(new GuiButton(ID_CLIPS, cx - 205, y2, 150, 20,
                "View clips here"));
        buttonList.add(new GuiButton(ID_SETTINGS, cx - 51, y2, 150, 20,
                "Settings"));
        buttonList.add(new GuiButton(ID_CLOSE, cx + 105, y2, 100, 20,
                "Close"));
        updateEnabled();
    }

    private void reload() {
        children = ClipLibrary.listChildFolders(current);
        if (selected >= children.size()) selected = -1;
        int max = Math.max(0, children.size() - visibleRows());
        if (scroll > max) scroll = max;
        fingerprint = ClipLibrary.fingerprint(QuickClipConfig.get());
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW_H);
    }

    private boolean isProtected(Path folder) {
        Path root = ClipLibrary.getRoot(QuickClipConfig.get());
        return folder.getParent() != null
                && folder.getParent().equals(root)
                && ClipLibrary.UNSORTED.equalsIgnoreCase(
                        folder.getFileName().toString());
    }

    private void updateEnabled() {
        boolean has = selected >= 0 && selected < children.size();
        boolean atRoot = current.equals(
                ClipLibrary.getRoot(QuickClipConfig.get()));
        for (Object o : buttonList) {
            GuiButton b = (GuiButton) o;
            if (b.id == ID_OPEN) b.enabled = has;
            if (b.id == ID_RENAME) {
                b.enabled = has && !isProtected(children.get(selected));
            }
            if (b.id == ID_DELETE) {
                b.enabled = has && !isProtected(children.get(selected));
            }
            if (b.id == ID_UP) b.enabled = !atRoot;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case ID_UP:
                goUp();
                break;
            case ID_OPEN:
                openSelected();
                break;
            case ID_NEW:
                mc.displayGuiScreen(new ClipTextPromptScreen(
                        this, "New folder name", "", name -> {
                    try {
                        ClipLibrary.createFolder(current, name);
                        status = "Folder created.";
                    } catch (IOException exception) {
                        status = exception.getMessage();
                    }
                    reload();
                    updateEnabled();
                }));
                break;
            case ID_RENAME:
                if (selected >= 0 && selected < children.size()) {
                    final Path folder = children.get(selected);
                    mc.displayGuiScreen(new ClipTextPromptScreen(
                            this, "Rename folder",
                            folder.getFileName().toString(), name -> {
                        try {
                            ClipLibrary.renameFolder(folder, name);
                            status = "Folder renamed.";
                        } catch (IOException exception) {
                            status = exception.getMessage();
                        }
                        reload();
                        updateEnabled();
                    }));
                }
                break;
            case ID_DELETE:
                if (selected >= 0 && selected < children.size()) {
                    try {
                        ClipLibrary.deleteEmptyFolder(children.get(selected));
                        status = "Folder deleted.";
                        selected = -1;
                    } catch (IOException exception) {
                        status = exception.getMessage();
                    }
                    reload();
                }
                break;
            case ID_CLIPS:
                mc.displayGuiScreen(new VideoCollectionScreen(this, current));
                break;
            case ID_SETTINGS:
                mc.displayGuiScreen(new QuickClipSettingsScreen(this));
                break;
            case ID_CLOSE:
                mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
        updateEnabled();
    }

    private void goUp() {
        Path root = ClipLibrary.getRoot(QuickClipConfig.get());
        if (!current.equals(root) && current.getParent() != null) {
            current = current.getParent();
            selected = -1;
            scroll = 0;
            reload();
            updateEnabled();
        }
    }

    private void openSelected() {
        if (selected >= 0 && selected < children.size()) {
            current = children.get(selected);
            selected = -1;
            scroll = 0;
            reload();
            updateEnabled();
        }
    }

    @Override
    public void updateScreen() {
        if (++refreshTicks >= 60) {
            refreshTicks = 0;
            long now = ClipLibrary.fingerprint(QuickClipConfig.get());
            if (now != fingerprint) {
                reload();
                updateEnabled();
            }
        }
    }

    @Override
    protected void keyTyped(char typed, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        } else if (code == Keyboard.KEY_BACK) {
            goUp();
        } else if (code == Keyboard.KEY_RETURN) {
            openSelected();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int max = Math.max(0, children.size() - visibleRows());
            scroll = Math.max(0, Math.min(max, scroll + (wheel > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button)
            throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        int left = width / 2 - 205;
        int right = width / 2 + 205;
        if (button == 0 && mouseX >= left && mouseX <= right
                && mouseY >= listTop && mouseY < listBottom) {
            int row = scroll + (mouseY - listTop) / ROW_H;
            if (row >= 0 && row < children.size()) {
                long now = System.currentTimeMillis();
                if (row == lastClickRow && now - lastClickMs < 400L) {
                    selected = row;
                    openSelected();
                } else {
                    selected = row;
                }
                lastClickRow = row;
                lastClickMs = now;
                updateEnabled();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        Path root = ClipLibrary.getRoot(QuickClipConfig.get());
        String path = "Clips";
        if (!current.equals(root)) {
            path += " / " + root.relativize(current).toString()
                    .replace('\\', '/').replace("/", " / ");
        }
        drawCenteredString(fontRendererObj, "QuickClip  -  " + path,
                width / 2, 12, 0xFFFFFF);
        drawCenteredString(fontRendererObj,
                ClipLibrary.countClips(current) + " clip(s) in this folder",
                width / 2, 26, 0xAAAAAA);
        int left = width / 2 - 205;
        int right = width / 2 + 205;
        drawRect(left, listTop - 2, right, listBottom, 0x80000000);
        if (children.isEmpty()) {
            drawCenteredString(fontRendererObj,
                    "No sub-folders. Use New Folder, or View clips here.",
                    width / 2, listTop + 14, 0x888888);
        }
        int rows = visibleRows();
        for (int i = 0; i < rows && scroll + i < children.size(); i++) {
            int index = scroll + i;
            Path folder = children.get(index);
            int top = listTop + i * ROW_H;
            if (index == selected) {
                drawRect(left + 1, top, right - 1, top + ROW_H - 2,
                        0x60FFFFFF);
            } else if (mouseX >= left && mouseX <= right
                    && mouseY >= top && mouseY < top + ROW_H - 2) {
                drawRect(left + 1, top, right - 1, top + ROW_H - 2,
                        0x20FFFFFF);
            }
            drawFolderIcon(left + 8, top + 5);
            fontRendererObj.drawStringWithShadow(
                    folder.getFileName().toString(), left + 32, top + 4,
                    0xFFFFFF);
            int subs = ClipLibrary.listChildFolders(folder).size();
            fontRendererObj.drawString(
                    ClipLibrary.countClips(folder) + " clip(s)"
                            + (subs > 0 ? "  -  " + subs + " folder(s)" : ""),
                    left + 32, top + 14, 0xAAAAAA);
        }
        if (!status.isEmpty()) {
            drawCenteredString(fontRendererObj, status, width / 2,
                    listBottom + 1, 0xFFAA55);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Simple drawn folder icon (16x12). */
    private void drawFolderIcon(int x, int y) {
        Gui.drawRect(x, y, x + 7, y + 3, 0xFFD9A441);
        Gui.drawRect(x, y + 2, x + 18, y + 14, 0xFFF2C14E);
        Gui.drawRect(x, y + 5, x + 18, y + 6, 0xFFD9A441);
    }
}
