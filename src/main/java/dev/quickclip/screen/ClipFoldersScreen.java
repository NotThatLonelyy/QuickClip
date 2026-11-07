package dev.quickclip.screen;

import dev.quickclip.ClipLibrary;
import dev.quickclip.ClipNotes;
import dev.quickclip.PlatformUtils;
import dev.quickclip.QuickClipConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import dev.quickclip.ClipThumbnail;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Notes-app style browser: folders (any depth) and the clips inside the
 * current folder in one list. Clips show their note under the file name.
 */
public final class ClipFoldersScreen extends GuiScreen {
    private static final int ROW_H = 36;
    private static final int ID_BACK = 1;
    private static final int ID_OPEN = 2;
    private static final int ID_NEW = 3;
    private static final int ID_RENAME = 4;
    private static final int ID_DELETE = 5;
    private static final int ID_MOVE = 6;
    private static final int ID_NOTES = 7;
    private static final int ID_SETTINGS = 8;
    private static final int ID_CLOSE = 9;

    private static final class Entry {
        Path path;
        boolean folder;
        String title;
        String line2;
        String line3;
        boolean hasNote;
    }

    private final GuiScreen parent;
    private Path current;
    private List<Entry> entries = new ArrayList<Entry>();
    private int selected = -1;
    private int scroll;
    private int listTop = 40;
    private int listBottom;
    private long lastClickMs;
    private int lastClickRow = -1;
    private String status = "";
    private int refreshTicks;
    private long fingerprint = Long.MIN_VALUE;
    private Path deleteArmed;
    private final Map<Path, ResourceLocation> thumbs =
            new HashMap<Path, ResourceLocation>();
    private final Set<Path> thumbTried = new HashSet<Path>();

    private void releaseThumbs() {
        if (mc != null && mc.getTextureManager() != null) {
            for (ResourceLocation location : thumbs.values()) {
                mc.getTextureManager().deleteTexture(location);
            }
        }
        thumbs.clear();
        thumbTried.clear();
    }

    @Override
    public void onGuiClosed() {
        releaseThumbs();
    }

    private ResourceLocation thumbFor(Path clip) {
        ResourceLocation cached = thumbs.get(clip);
        if (cached != null || thumbTried.contains(clip)) return cached;
        thumbTried.add(clip);
        try {
            Path file = ClipThumbnail.sidecar(clip);
            if (!Files.isRegularFile(file)) return null;
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) return null;
            ResourceLocation location = mc.getTextureManager()
                    .getDynamicTextureLocation("quickclip_thumb",
                            new DynamicTexture(image));
            thumbs.put(clip, location);
            return location;
        } catch (Throwable throwable) {
            return null;
        }
    }

    public ClipFoldersScreen(GuiScreen parent) {
        this.parent = parent;
        this.current = ClipLibrary.getRoot(QuickClipConfig.get());
    }

    @Override
    public void initGui() {
        ClipLibrary.ensureLayout(QuickClipConfig.get());
        buttonList.clear();
        int cx = width / 2;
        int y1 = height - 52;
        int y2 = height - 28;
        listBottom = y1 - 12;
        buttonList.add(new GuiButton(ID_BACK, cx - 205, y1, 60, 20, "< Back"));
        buttonList.add(new GuiButton(ID_OPEN, cx - 143, y1, 60, 20, "Open"));
        buttonList.add(new GuiButton(ID_NEW, cx - 81, y1, 86, 20,
                "New Folder"));
        buttonList.add(new GuiButton(ID_RENAME, cx + 7, y1, 96, 20,
                "Rename"));
        buttonList.add(new GuiButton(ID_DELETE, cx + 105, y1, 100, 20,
                "Delete"));
        buttonList.add(new GuiButton(ID_MOVE, cx - 205, y2, 96, 20,
                "Move to..."));
        buttonList.add(new GuiButton(ID_NOTES, cx - 107, y2, 96, 20,
                "Note / Link"));
        buttonList.add(new GuiButton(ID_SETTINGS, cx - 9, y2, 96, 20,
                "Settings"));
        buttonList.add(new GuiButton(ID_CLOSE, cx + 89, y2, 116, 20,
                "Close"));
        reload();
        updateButtons();
    }

    private void reload() {
        releaseThumbs();
        entries = new ArrayList<Entry>();
        for (Path folder : ClipLibrary.listChildFolders(current)) {
            Entry e = new Entry();
            e.path = folder;
            e.folder = true;
            e.title = folder.getFileName().toString();
            int subs = ClipLibrary.listChildFolders(folder).size();
            e.line2 = ClipLibrary.countClips(folder) + " clip(s)"
                    + (subs > 0 ? "  -  " + subs + " folder(s)" : "");
            entries.add(e);
        }
        List<Path> clips = new ArrayList<Path>();
        try (Stream<Path> stream = Files.list(current)) {
            stream.filter(p -> Files.isRegularFile(p) && isVideo(p))
                    .forEach(clips::add);
        } catch (IOException | RuntimeException ignored) {
            // empty list
        }
        Collections.sort(clips, (a, b) -> Long.compare(modified(b),
                modified(a)));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (Path clip : clips) {
            Entry e = new Entry();
            e.path = clip;
            e.title = baseName(clip);
            ClipNotes notes = ClipNotes.read(clip);
            e.hasNote = !notes.isEmpty();
            String info = sizeText(clip) + "  -  "
                    + format.format(new Date(modified(clip)));
            if (!notes.note.isEmpty()) {
                e.line2 = notes.note;
                e.line3 = info;
            } else if (!notes.link.isEmpty()) {
                e.line2 = "Link: " + notes.link;
                e.line3 = info;
            } else {
                e.line2 = info;
            }
            entries.add(e);
        }
        if (selected >= entries.size()) selected = -1;
        int max = Math.max(0, entries.size() - visibleRows());
        if (scroll > max) scroll = max;
        fingerprint = ClipLibrary.fingerprint(QuickClipConfig.get());
    }

    private static boolean isVideo(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return !n.startsWith(".") && (n.endsWith(".mp4") || n.endsWith(".webm")
                || n.endsWith(".mov") || n.endsWith(".mkv"));
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static String baseName(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static String sizeText(Path p) {
        try {
            long b = Files.size(p);
            if (b >= 1024L * 1024L) {
                return String.format(Locale.ROOT, "%.1f MB",
                        b / 1048576.0D);
            }
            return Math.max(1, b / 1024L) + " KB";
        } catch (IOException exception) {
            return "?";
        }
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW_H);
    }

    private Entry sel() {
        return selected >= 0 && selected < entries.size()
                ? entries.get(selected) : null;
    }

    private boolean isProtected(Entry e) {
        Path root = ClipLibrary.getRoot(QuickClipConfig.get());
        return e != null && e.folder && e.path.getParent() != null
                && e.path.getParent().equals(root)
                && ClipLibrary.UNSORTED.equalsIgnoreCase(
                        e.path.getFileName().toString());
    }

    private void updateButtons() {
        Entry e = sel();
        boolean atRoot = current.equals(
                ClipLibrary.getRoot(QuickClipConfig.get()));
        for (Object o : buttonList) {
            GuiButton b = (GuiButton) o;
            switch (b.id) {
                case ID_BACK: b.enabled = !atRoot; break;
                case ID_OPEN:
                    b.enabled = e != null;
                    b.displayString = e != null && !e.folder ? "Play" : "Open";
                    break;
                case ID_RENAME: b.enabled = e != null && !isProtected(e); break;
                case ID_DELETE:
                    b.enabled = e != null && !isProtected(e);
                    b.displayString = e != null && e.path.equals(deleteArmed)
                            ? "Confirm delete" : "Delete";
                    break;
                case ID_MOVE: b.enabled = e != null && !e.folder; break;
                case ID_NOTES: b.enabled = e != null && !e.folder; break;
                default: break;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        Entry e = sel();
        if (button.id != ID_DELETE) deleteArmed = null;
        switch (button.id) {
            case ID_BACK:
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
                    updateButtons();
                }));
                break;
            case ID_RENAME:
                if (e != null) promptRename(e);
                break;
            case ID_DELETE:
                if (e != null) deleteEntry(e);
                break;
            case ID_MOVE:
                if (e != null && !e.folder) {
                    final Path file = e.path;
                    mc.displayGuiScreen(new ClipFolderPickerScreen(this,
                            folder -> {
                        try {
                            ClipLibrary.move(file, folder);
                            status = "Moved to " + folder.getFileName() + ".";
                        } catch (IOException exception) {
                            status = "Move failed: " + exception.getMessage();
                        }
                        reload();
                        updateButtons();
                    }));
                }
                break;
            case ID_NOTES:
                if (e != null && !e.folder) {
                    mc.displayGuiScreen(new ClipNotesScreen(this, e.path,
                            () -> {
                        reload();
                        updateButtons();
                    }));
                }
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
        updateButtons();
    }

    private void promptRename(Entry e) {
        final Path path = e.path;
        final boolean folder = e.folder;
        mc.displayGuiScreen(new ClipTextPromptScreen(this,
                folder ? "Rename folder" : "Rename clip", e.title, name -> {
            try {
                if (folder) {
                    ClipLibrary.renameFolder(path, name);
                } else {
                    ClipLibrary.rename(path, name);
                }
                status = "Renamed.";
            } catch (IOException | RuntimeException exception) {
                status = "Rename failed: " + exception.getMessage();
            }
            reload();
            updateButtons();
        }));
    }

    private void deleteEntry(Entry e) {
        if (!e.path.equals(deleteArmed)) {
            deleteArmed = e.path;
            status = e.folder
                    ? "Click Delete again to remove this empty folder."
                    : "Click Delete again to permanently delete this clip.";
            return;
        }
        deleteArmed = null;
        try {
            if (e.folder) {
                ClipLibrary.deleteEmptyFolder(e.path);
            } else {
                Files.delete(e.path);
                ClipLibrary.deleteSidecar(e.path);
            }
            status = "Deleted.";
            selected = -1;
        } catch (IOException exception) {
            status = "Delete failed: " + exception.getMessage();
        }
        reload();
    }

    private void goUp() {
        Path root = ClipLibrary.getRoot(QuickClipConfig.get());
        if (!current.equals(root) && current.getParent() != null) {
            current = current.getParent();
            selected = -1;
            scroll = 0;
            deleteArmed = null;
            reload();
            updateButtons();
        }
    }

    private void openSelected() {
        Entry e = sel();
        if (e == null) return;
        if (e.folder) {
            current = e.path;
            selected = -1;
            scroll = 0;
            reload();
        } else if (!PlatformUtils.open(e.path)) {
            status = "Could not open the clip.";
        }
        updateButtons();
    }

    @Override
    public void updateScreen() {
        if (++refreshTicks >= 60) {
            refreshTicks = 0;
            if (ClipLibrary.fingerprint(QuickClipConfig.get())
                    != fingerprint) {
                reload();
                updateButtons();
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
            int max = Math.max(0, entries.size() - visibleRows());
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
            if (row >= 0 && row < entries.size()) {
                long now = System.currentTimeMillis();
                selected = row;
                if (row == lastClickRow && now - lastClickMs < 400L) {
                    openSelected();
                }
                lastClickRow = row;
                lastClickMs = now;
                updateButtons();
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
                "Double-click a folder to open it, a clip to play it",
                width / 2, 26, 0x999999);
        int left = width / 2 - 205;
        int right = width / 2 + 205;
        drawRect(left, listTop - 2, right, listBottom, 0x80000000);
        if (entries.isEmpty()) {
            drawCenteredString(fontRendererObj,
                    "Empty. Press F8 in-game to save a replay, or New Folder.",
                    width / 2, listTop + 14, 0x888888);
        }
        int rows = visibleRows();
        for (int i = 0; i < rows && scroll + i < entries.size(); i++) {
            int index = scroll + i;
            Entry e = entries.get(index);
            int top = listTop + i * ROW_H;
            if (index == selected) {
                drawRect(left + 1, top, right - 1, top + ROW_H - 2,
                        0x60FFFFFF);
            } else if (mouseX >= left && mouseX <= right
                    && mouseY >= top && mouseY < top + ROW_H - 2) {
                drawRect(left + 1, top, right - 1, top + ROW_H - 2,
                        0x20FFFFFF);
            }
            if (e.folder) {
                drawFolderIcon(left + 23, top + 11);
            } else {
                ResourceLocation thumb = thumbFor(e.path);
                if (thumb != null) {
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(thumb);
                    Gui.drawModalRectWithCustomSizedTexture(
                            left + 6, top + 3, 0.0F, 0.0F, 53, 30, 53.0F,
                            30.0F);
                } else {
                    Gui.drawRect(left + 6, top + 3, left + 59, top + 33,
                            0xFF1E1E1E);
                    drawClipIcon(left + 23, top + 11);
                }
            }
            int textX = left + 66;
            int maxW = right - textX - 6;
            fontRendererObj.drawStringWithShadow(
                    fontRendererObj.trimStringToWidth(e.title, maxW),
                    textX, top + 5, 0xFFFFFF);
            fontRendererObj.drawString(
                    fontRendererObj.trimStringToWidth(e.line2, maxW),
                    textX, top + 16, e.hasNote ? 0xFFD27F : 0xAAAAAA);
            if (e.line3 != null) {
                fontRendererObj.drawString(
                        fontRendererObj.trimStringToWidth(e.line3, maxW),
                        textX, top + 26, 0x888888);
            }
        }
        if (!status.isEmpty()) {
            drawCenteredString(fontRendererObj, status, width / 2,
                    listBottom + 2, 0xFFAA55);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawFolderIcon(int x, int y) {
        Gui.drawRect(x, y, x + 7, y + 3, 0xFFD9A441);
        Gui.drawRect(x, y + 2, x + 18, y + 14, 0xFFF2C14E);
        Gui.drawRect(x, y + 5, x + 18, y + 6, 0xFFD9A441);
    }

    private void drawClipIcon(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 14, 0xFF2F6FB5);
        Gui.drawRect(x + 7, y + 3, x + 9, y + 11, 0xFFFFFFFF);
        Gui.drawRect(x + 9, y + 4, x + 11, y + 10, 0xFFFFFFFF);
        Gui.drawRect(x + 11, y + 5, x + 13, y + 9, 0xFFFFFFFF);
    }
}
