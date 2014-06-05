package forge.toolbox;

import java.io.File;
import java.io.FilenameFilter;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;

public class FFileChooser extends FDialog {
    public enum ChoiceType {
        OpenFile,
        SaveFile,
        GetDirectory
    }

    public static void show(String title0, ChoiceType choiceType0, Callback<String> callback0) {
        show(title0, choiceType0, "", "", callback0);
    }
    public static void show(String title0, ChoiceType choiceType0, String defaultFilename0, Callback<String> callback0) {
        show(title0, choiceType0, defaultFilename0, "", callback0);
    }
    public static void show(String title0, ChoiceType choiceType0, String defaultFilename0, String baseDir0, Callback<String> callback0) {
        FFileChooser dialog = new FFileChooser(title0, choiceType0, defaultFilename0, baseDir0, callback0);
        dialog.show();
    }

    private final ChoiceType choiceType;
    private final String baseDir;
    private final Callback<String> callback;

    private final FList<File> lstFiles   = add(new FileList());
    private final FTextField txtFilename = add(new FTextField());

    private final FButton btnNewFolder = add(new FButton("New Folder", new FEventHandler() {
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

    private FFileChooser(String title0, ChoiceType choiceType0, String defaultFilename0, String baseDir0, Callback<String> callback0) {
        super(title0);
        choiceType = choiceType0;
        if (choiceType == ChoiceType.GetDirectory) {
            if (defaultFilename0.endsWith(File.separator)) { //if getting directory, don't end with a slash
                defaultFilename0 = defaultFilename0.substring(0, defaultFilename0.length() - 1);
            }
        }
        txtFilename.setFont(FSkinFont.get(12));
        txtFilename.setText(defaultFilename0);
        txtFilename.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                refreshFileList();
            }
        });
        baseDir = baseDir0;
        callback = callback0;
        refreshFileList();
    }

    private void refreshFileList() {
        String filename = txtFilename.getText();
        File dir = new File(baseDir + filename);
        if (!dir.exists() || !dir.isDirectory()) {
            int idx = filename.lastIndexOf(File.separatorChar);
            if (idx == -1) {
                dir = new File(baseDir);
            }
            else {
                dir = new File(baseDir + filename.substring(0, idx));
            }
        }
        if (dir.exists() && dir.isDirectory()) {
            FilenameFilter filter = null;
            if (choiceType == ChoiceType.GetDirectory) {
                //don't list files if getting directory
                dir = dir.getParentFile(); //show parent folder's files and select folder
                if (dir == null) {
                    lstFiles.setListData(File.listRoots());
                    return;
                }
                filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return new File(dir, name).isDirectory();
                    }
                };
            }
            lstFiles.setListData(dir.listFiles(filter));
        }
        else {
            lstFiles.setListData(File.listRoots());
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float w = width - 2 * padding;

        //layout buttons
        float gapBetweenButtons = padding / 2;
        float buttonWidth = (w - gapBetweenButtons * 2) / 3;
        float buttonHeight = FOptionPane.BUTTON_HEIGHT;
        float x = padding;
        float y = maxHeight - FOptionPane.GAP_BELOW_BUTTONS - buttonHeight;
        btnNewFolder.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + gapBetweenButtons;
        btnOK.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + gapBetweenButtons;
        btnCancel.setBounds(x, y, buttonWidth, buttonHeight);

        float fieldHeight = txtFilename.getHeight();
        float listHeight = y - fieldHeight - 3 * padding;
        x = padding;
        y = padding;
        txtFilename.setBounds(x, y, w, fieldHeight);
        y += fieldHeight + padding;
        lstFiles.setBounds(x, y, w, listHeight);
        return maxHeight;
    }

    private String getSelectedFilename() {
        return baseDir + txtFilename.getText();
    }

    private void acceptSelectedFile() {
        hide();
        callback.run(getSelectedFilename());
    }

    private class FileList extends FList<File> {
        private FileList() {
            setListItemRenderer(new ListItemRenderer<File>() {
                private Integer prevTapIndex = -1;

                @Override
                public float getItemHeight() {
                    return ListChooser.DEFAULT_ITEM_HEIGHT;
                }

                @Override
                public boolean tap(Integer index, File value, float x, float y, int count) {
                    if (count == 2 && index == prevTapIndex) {
                        acceptSelectedFile();
                    }
                    txtFilename.setText(value.getAbsolutePath());
                    prevTapIndex = index;
                    return true;
                }

                @Override
                public void drawValue(Graphics g, Integer index, File value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
                    if (value.isDirectory()) {
                        float iconSize = h;
                        g.drawImage(FSkinImage.FOLDER, x, y + (h - iconSize) / 2, iconSize, iconSize);
                        x += iconSize + FList.PADDING; 
                    }
                    g.drawText(value.getName(), font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);
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
            if (getItemAt(index).getAbsolutePath().equals(getSelectedFilename())) {
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

    @Override
    public boolean keyDown(int keyCode) {
        switch (keyCode) {
        case Keys.ENTER:
            acceptSelectedFile();
            return true;
        case Keys.ESCAPE:
            if (Forge.endKeyInput()) { return true; }
            break; //let FDialog handle it
        case Keys.BACK:
        case Keys.BACKSPACE:
            //TODO: Navigate back to previous directory if possible
            return true;
        }
        return super.keyDown(keyCode);
    }
}
