package dev.quickclip.screen;

import dev.quickclip.ClipLibrary;
import dev.quickclip.QuickClipConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Hierarchical folder picker: folders can contain any number of subfolders. */
public final class ClipFolderPickerScreen extends GuiScreen {
    private static final int PAGE_SIZE = 7;
    private final GuiScreen parent;
    private final Consumer<Path> onPick;
    private Path current;
    private List<Path> folders;
    private int page;

    public ClipFolderPickerScreen(GuiScreen parent, Consumer<Path> onPick) {
        this.parent = parent;
        this.onPick = onPick;
        this.current = ClipLibrary.getRoot(QuickClipConfig.get());
    }

    @Override public void initGui() {
        folders = ClipLibrary.listFolders(current);
        buttonList.clear();
        int top = height / 2 - 80;
        int start = page * PAGE_SIZE;
        for (int i=0; i<PAGE_SIZE && start+i<folders.size(); i++) {
            Path folder = folders.get(start+i);
            buttonList.add(new GuiButton(100+i, width/2-100, top+i*22, 200,20,
                    "[Folder] " + folder.getFileName()));
        }
        int bottom = top + PAGE_SIZE*22 + 4;
        buttonList.add(new GuiButton(1,width/2-100,bottom,60,20,"< Prev"));
        buttonList.add(new GuiButton(2,width/2+40,bottom,60,20,"Next >"));
        buttonList.add(new GuiButton(3,width/2-35,bottom+24,70,20,"Use Here"));
        buttonList.add(new GuiButton(4,width/2-100,bottom+24,60,20,"Up"));
        buttonList.add(new GuiButton(5,width/2+40,bottom+24,60,20,"Cancel"));
        buttonList.get(buttonList.size()-5).enabled = page > 0;
        buttonList.get(buttonList.size()-4).enabled = start + PAGE_SIZE < folders.size();
        Path root = ClipLibrary.getRoot(QuickClipConfig.get());
        buttonList.get(buttonList.size()-2).enabled = current != null && !current.equals(root);
    }

    @Override protected void actionPerformed(GuiButton b) {
        if (b.id==1 && page>0) { page--; initGui(); return; }
        if (b.id==2) { page++; initGui(); return; }
        if (b.id==3) { mc.displayGuiScreen(parent); onPick.accept(current); return; }
        if (b.id==4) { Path root=ClipLibrary.getRoot(QuickClipConfig.get()); if(current!=null&&!current.equals(root)){current=current.getParent();page=0;initGui();} return; }
        if (b.id==5) { mc.displayGuiScreen(parent); return; }
        if (b.id>=100 && b.id<100+PAGE_SIZE) {
            int index=page*PAGE_SIZE+(b.id-100);
            if(index>=0&&index<folders.size()){current=folders.get(index);page=0;initGui();}
        }
    }

    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        drawDefaultBackground();
        String path=current==null?"":ClipLibrary.getRoot(QuickClipConfig.get()).relativize(current).toString();
        drawCenteredString(fontRendererObj,"Move clip - "+(path.isEmpty()?"/":path),width/2,height/2-98,0xFFFFFF);
        drawCenteredString(fontRendererObj,"Open folders to browse • Use Here to select current folder",width/2,height/2-86,0xAAAAAA);
        super.drawScreen(mouseX,mouseY,partialTicks);
    }
}
