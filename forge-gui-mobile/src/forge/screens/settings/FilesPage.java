package forge.screens.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import forge.gui.GuiBase;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.gui.download.GuiDownloadAchievementImages;
import forge.gui.download.GuiDownloadPicturesLQ;
import forge.gui.download.GuiDownloadPrices;
import forge.gui.download.GuiDownloadQuestImages;
import forge.gui.download.GuiDownloadService;
import forge.gui.download.GuiDownloadSetPicturesLQ;
import forge.gui.download.GuiDownloadSkins;
import forge.gui.download.GuiDownloadZipService;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgeProfileProperties;
import forge.screens.TabPageScreen.TabPage;
import forge.toolbox.FFileChooser;
import forge.toolbox.FFileChooser.ChoiceType;
import forge.toolbox.FGroupList;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.GuiChoose;
import forge.util.Callback;
import forge.util.FileUtil;
import forge.util.Localizer;

public class FilesPage extends TabPage<SettingsScreen> {
    private final FGroupList<FilesItem> lstItems = add(new FGroupList<>());
    private final Localizer localizer = Localizer.getInstance();

    protected FilesPage() {
        super(Localizer.getInstance().getMessage("lblFiles"), Forge.hdbuttons ? FSkinImage.HDOPEN : FSkinImage.OPEN);

        lstItems.setListItemRenderer(new FilesItemRenderer());

        lstItems.addGroup(localizer.getMessage("ContentDownloaders"));
        lstItems.addGroup(localizer.getMessage("lblStorageLocations"));
        //lstItems.addGroup("Data Import");

        //content downloaders
        lstItems.addItem(new ContentDownloader(localizer.getMessage("btnDownloadPics"),
                localizer.getMessage("lblDownloadPics")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadPicturesLQ();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader(localizer.getMessage("btnDownloadSetPics"),
                localizer.getMessage("lblDownloadSetPics")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadSetPicturesLQ();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader(localizer.getMessage("btnDownloadQuestImages"),
                localizer.getMessage("lblDownloadQuestImages")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadQuestImages();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader(localizer.getMessage("btnDownloadAchievementImages"),
                localizer.getMessage("lblDownloadAchievementImages")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadAchievementImages();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader(localizer.getMessage("btnDownloadPrices"),
                localizer.getMessage("lblDownloadPrices")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadPrices();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader(localizer.getMessage("btnDownloadSkins"),
                localizer.getMessage("lblDownloadSkins")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadSkins();
            }
            @Override
            protected void finishCallback() {
                SettingsScreen.getSettingsScreen().getSettingsPage().refreshSkinsList();
            }
        }, 0);
        lstItems.addItem(new OptionContentDownloader(localizer.getMessage("btnDownloadCJKFonts"),
                localizer.getMessage("lblDownloadCJKFonts"),
                localizer.getMessage("lblDownloadCJKFontPrompt")) {
            @Override
            protected Map<String, String> getCategories() {
                // read CJK font list
                Map<String, String> categories = new TreeMap<>();
                List<String> lines = FileUtil.readFile(ForgeConstants.CJK_FONTS_LIST_FILE);
                List<String> options = new ArrayList<>();
                for (String line : lines) {
                    int idx = line.indexOf('|');
                    if (idx != -1) {
                        String name = line.substring(0, idx).trim();
                        String url = line.substring(idx + 1).trim();
                        categories.put(name, url);
                        options.add(name);
                    }
                }
                return categories;
            }
            @Override
            protected void finishCallback() {
                SettingsScreen.getSettingsScreen().getSettingsPage().refreshCJKFontsList();
            }
        }, 0);
        //storage locations
        final StorageOption cardPicsOption = new StorageOption(localizer.getMessage("lblCardPicsLocation"), ForgeProfileProperties.getCardPicsDir()) {
            @Override
            protected void onDirectoryChanged(String newDir) {
                ForgeProfileProperties.setCardPicsDir(newDir);
            }
        };
        final StorageOption decksOption = new StorageOption(localizer.getMessage("lblDecksLocation"), ForgeProfileProperties.getDecksDir()) {
            @Override
            protected void onDirectoryChanged(String newDir) {
                ForgeProfileProperties.setDecksDir(newDir);
            }
        };
        if (!GuiBase.isUsingAppDirectory()) {
            lstItems.addItem(new StorageOption(localizer.getMessage("lblDataLocation"), ForgeProfileProperties.getUserDir()) {
                @Override
                protected void onDirectoryChanged(String newDir) {
                    ForgeProfileProperties.setUserDir(newDir);

                    //ensure decks option is updated if needed
                    decksOption.updateDir(ForgeProfileProperties.getDecksDir());
                }
            }, 1);
            lstItems.addItem(new StorageOption(localizer.getMessage("lblImageCacheLocation"), ForgeProfileProperties.getCacheDir()) {
                @Override
                protected void onDirectoryChanged(String newDir) {
                    ForgeProfileProperties.setCacheDir(newDir);

                    //ensure card pics option is updated if needed
                    cardPicsOption.updateDir(ForgeProfileProperties.getCardPicsDir());
                }
            }, 1);
            lstItems.addItem(cardPicsOption, 1);
            lstItems.addItem(decksOption, 1);
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        lstItems.setBounds(0, 0, width, height);
    }

    private abstract class FilesItem {
        protected String label;
        protected String description;

        FilesItem(String label0, String description0) {
            label = label0;
            description = description0;
        }

        public abstract void select();
    }

    private static class FilesItemRenderer extends FList.ListItemRenderer<FilesItem> {
        @Override
        public float getItemHeight() {
            return SettingsScreen.SETTING_HEIGHT;
        }

        @Override
        public boolean tap(Integer index, FilesItem value, float x, float y, int count) {
            value.select();
            return true;
        }

        @Override
        public void drawValue(Graphics g, Integer index, FilesItem value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
            float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
            x += offset;
            y += offset;
            w -= 2 * offset;
            h -= 2 * offset;

            float totalHeight = h;
            h = font.getMultiLineBounds(value.label).height + SettingsScreen.SETTING_PADDING;

            g.drawText(value.label, font, foreColor, x, y, w, h, false, Align.left, false);
            h += SettingsScreen.SETTING_PADDING;
            g.drawText(value.description, SettingsScreen.DESC_FONT, SettingsScreen.DESC_COLOR, x, y + h, w, totalHeight - h + SettingsScreen.getInsets(w), true, Align.left, false);
        }
    }

    private abstract class ContentDownloader extends FilesItem {
        ContentDownloader(String label0, String description0) {
            super(label0, description0);
        }

        @Override
        public void select() {
            new GuiDownloader(createService(), new Callback<Boolean>() {
                @Override
                public void run(Boolean finished) {
                    if (finished) {
                        finishCallback();
                    }
                }
            }).show();
        }
        protected abstract GuiDownloadService createService();

        protected void finishCallback() {
        }
    }

    private abstract class OptionContentDownloader extends FilesItem {
        private final String prompt;

        OptionContentDownloader(String label0, String description0, String propmt0) {
            super(label0, description0);
            prompt = propmt0;
        }

        @Override
        public void select() {
            final Map<String, String> categories = getCategories();
            GuiChoose.one(prompt, categories.keySet(), new Callback<String>() {
                @Override
                public void run(String result) {
                    final String url = categories.get(result);
                    final String name = url.substring(url.lastIndexOf("/") + 1);
                    new GuiDownloader(new GuiDownloadZipService(name, name, url, ForgeConstants.FONTS_DIR, null, null), new Callback<Boolean>() {
                        @Override
                        public void run(Boolean finished) {
                            if (finished) {
                                finishCallback();
                            }
                        }
                    }).show();
                }
            });
        }
        protected abstract Map<String, String> getCategories();

        protected void finishCallback() {
        }
    }

    private abstract class StorageOption extends FilesItem {
        StorageOption(String name0, String dir0) {
            super(name0, dir0);
        }

        private void updateDir(String dir0) {
            description = dir0;
        }

        @Override
        public void select() {
            FFileChooser.show(localizer.getMessage("lblSelect").replace("%s", label), ChoiceType.GetDirectory, description, new Callback<String>() {
                @Override
                public void run(String result) {
                    if (StringUtils.isEmpty(result) || description.equals(result)) { return; }
                    updateDir(result);
                    onDirectoryChanged(result);
                    FOptionPane.showMessageDialog(localizer.getMessage("lblRestartForgeMoveFilesNewLocation"), localizer.getMessage("lblRestartRequired"), FOptionPane.INFORMATION_ICON);
                }
            });
        }
        protected abstract void onDirectoryChanged(String newDir);
    }
}
