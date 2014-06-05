package forge.toolbox;

import java.io.File;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;

public class FFileChooser extends FDialog {
    public static void getFilename(String title0, boolean forSave0, Callback<File> callback0) {
        getFilename(title0, forSave0, null, "", callback0);
    }
    public static void getFilename(String title0, boolean forSave0, File baseDir0, Callback<File> callback0) {
        getFilename(title0, forSave0, baseDir0, "", callback0);
    }
    public static void getFilename(String title0, boolean forSave0, File baseDir0, String defaultFilename0, Callback<File> callback0) {
        FFileChooser dialog = new FFileChooser(title0, forSave0, baseDir0, defaultFilename0, callback0);
        dialog.show();
    }

    private final boolean forSave;
    private final File baseDir;
    private final Callback<File> callback;
    private File currentDir;

    private final FList<File> lstFiles = add(new FileList());
    private final FTextField txtFilename = add(new FTextField());
    private final FButton btnNewFolder    = add(new FButton("New Folder", new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            //TODO: Add new folder
        }
    }));
    private final FButton btnOK = add(new FButton("OK", new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            acceptSelectedFile();
        }
    }));
    private final FButton btnCancel  = add(new FButton("Cancel", new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            hide();
        }
    }));

    private FFileChooser(String title0, boolean forSave0, File baseDir0, String defaultFilename0, Callback<File> callback0) {
        super(title0);
        forSave = forSave0;
        baseDir = baseDir0;
        currentDir = baseDir;
        txtFilename.setText(defaultFilename0);
        callback = callback0;
        refreshFileList();
    }

    private void refreshFileList() {
        if (currentDir != null) {
            lstFiles.setListData(currentDir.listFiles());
        }
        else {
            lstFiles.setListData(File.listRoots());
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        // TODO Auto-generated method stub
        return 0;
    }

    private void acceptSelectedFile() {
        //callback.run(result);
        hide();
    }

    private class FileList extends FList<File> {
        private int selectedIndex;

        private FileList() {
            setListItemRenderer(new ListItemRenderer<File>() {
                private Integer prevTapIndex = -1;

                @Override
                public float getItemHeight() {
                    return ListChooser.DEFAULT_ITEM_HEIGHT;
                }

                @Override
                public boolean tap(Integer index, File value, float x, float y, int count) {
                    selectedIndex = index;
                    if (count == 2 && index == prevTapIndex) {
                        acceptSelectedFile();
                    }
                    prevTapIndex = index;
                    return true;
                }

                @Override
                public void drawValue(Graphics g, Integer index, File value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
                    //TODO: Draw icon for folder vs. file
                    g.drawText(value.toString(), font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);
                }
            });
        }

        @Override
        protected void drawBackground(Graphics g) {
            //draw no background
        }

        @Override
        public void drawOverlay(Graphics g) {
            g.drawRect(1.5f, ListChooser.BORDER_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        protected FSkinColor getItemFillColor(int index) {
            if (selectedIndex == index) {
                return ListChooser.SEL_COLOR; //don't show SEL_COLOR if in multi-select mode
            }
            if (index % 2 == 1) {
                return ListChooser.ALT_ITEM_COLOR;
            }
            return ListChooser.ITEM_COLOR;
        }

        @Override
        protected boolean drawLineSeparators() {
            return false;
        }
    }
}
