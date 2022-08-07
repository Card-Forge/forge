package forge.screens.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import forge.StaticData;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.screens.LoadingOverlay;
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
import org.apache.commons.lang3.tuple.Pair;

public class FilesPage extends TabPage<SettingsScreen> {
    private final FGroupList<FilesItem> lstItems = add(new FGroupList<>());

    protected FilesPage() {
        super(Forge.getLocalizer().getMessage("lblFiles"), Forge.hdbuttons ? FSkinImage.HDOPEN : FSkinImage.OPEN);

        lstItems.setListItemRenderer(new FilesItemRenderer());

        lstItems.addGroup(Forge.getLocalizer().getMessage("lblCardAudit"));
        lstItems.addGroup(Forge.getLocalizer().getMessage("ContentDownloaders"));
        lstItems.addGroup(Forge.getLocalizer().getMessage("lblStorageLocations"));
        //lstItems.addGroup("Data Import");

        //Auditer
        lstItems.addItem(new Extra(Forge.getLocalizer().getMessage("btnListImageData"), Forge.getLocalizer().getMessage("lblListImageData")) {
            @Override
            public void select() {
                FThreads.invokeInEdtLater(() -> LoadingOverlay.show(Forge.getLocalizer().getMessage("lblProcessingCards"), true, () -> {
                    StringBuffer nifSB = new StringBuffer(); // NO IMAGE FOUND BUFFER
                    StringBuffer cniSB = new StringBuffer(); // CARD NOT IMPLEMENTED BUFFER

                    nifSB.append("\n\n-------------------\n");
                    nifSB.append("NO IMAGE FOUND LIST\n");
                    nifSB.append("-------------------\n\n");

                    cniSB.append("\n\n-------------------\n");
                    cniSB.append("UNIMPLEMENTED CARD LIST\n");
                    cniSB.append("-------------------\n\n");

                    Pair<Integer, Integer> totalAudit = StaticData.instance().audit(nifSB, cniSB);
                    String msg = nifSB.toString();
                    String title = "Missing images: " + totalAudit.getLeft() + "\nUnimplemented cards: " + totalAudit.getRight();
                    FOptionPane.showOptionDialog(msg, title, FOptionPane.INFORMATION_ICON, ImmutableList.of(Forge.getLocalizer().getMessage("lblCopy"), Forge.getLocalizer().getMessage("lblClose")), -1, new Callback<Integer>() {
                        @Override
                        public void run(Integer result) {
                            switch (result) {
                                case 0:
                                    Forge.getClipboard().setContents(msg);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                }));
            }
        }, 0);
        //content downloaders
        lstItems.addItem(new ContentDownloader(Forge.getLocalizer().getMessage("btnDownloadPics"),
                Forge.getLocalizer().getMessage("lblDownloadPics")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadPicturesLQ();
            }
        }, 1);
        lstItems.addItem(new ContentDownloader(Forge.getLocalizer().getMessage("btnDownloadSetPics"),
                Forge.getLocalizer().getMessage("lblDownloadSetPics")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadSetPicturesLQ();
            }
        }, 1);
        lstItems.addItem(new ContentDownloader(Forge.getLocalizer().getMessage("btnDownloadQuestImages"),
                Forge.getLocalizer().getMessage("lblDownloadQuestImages")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadQuestImages();
            }
        }, 1);
        lstItems.addItem(new ContentDownloader(Forge.getLocalizer().getMessage("btnDownloadAchievementImages"),
                Forge.getLocalizer().getMessage("lblDownloadAchievementImages")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadAchievementImages();
            }
        }, 1);
        lstItems.addItem(new ContentDownloader(Forge.getLocalizer().getMessage("btnDownloadPrices"),
                Forge.getLocalizer().getMessage("lblDownloadPrices")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadPrices();
            }
        }, 1);
        lstItems.addItem(new ContentDownloader(Forge.getLocalizer().getMessage("btnDownloadSkins"),
                Forge.getLocalizer().getMessage("lblDownloadSkins")) {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadSkins();
            }
            @Override
            protected void finishCallback() {
                SettingsScreen.getSettingsScreen().getSettingsPage().refreshSkinsList();
            }
        }, 1);
        lstItems.addItem(new OptionContentDownloader(Forge.getLocalizer().getMessage("btnDownloadCJKFonts"),
                Forge.getLocalizer().getMessage("lblDownloadCJKFonts"),
                Forge.getLocalizer().getMessage("lblDownloadCJKFontPrompt")) {
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
        }, 1);
        //storage locations
        final StorageOption cardPicsOption = new StorageOption(Forge.getLocalizer().getMessage("lblCardPicsLocation"), ForgeProfileProperties.getCardPicsDir()) {
            @Override
            protected void onDirectoryChanged(String newDir) {
                ForgeProfileProperties.setCardPicsDir(newDir);
            }
        };
        final StorageOption decksOption = new StorageOption(Forge.getLocalizer().getMessage("lblDecksLocation"), ForgeProfileProperties.getDecksDir()) {
            @Override
            protected void onDirectoryChanged(String newDir) {
                ForgeProfileProperties.setDecksDir(newDir);
            }
        };
        if (!GuiBase.isUsingAppDirectory()) {
            lstItems.addItem(new StorageOption(Forge.getLocalizer().getMessage("lblDataLocation"), ForgeProfileProperties.getUserDir()) {
                @Override
                protected void onDirectoryChanged(String newDir) {
                    ForgeProfileProperties.setUserDir(newDir);

                    //ensure decks option is updated if needed
                    decksOption.updateDir(ForgeProfileProperties.getDecksDir());
                }
            }, 2);
            lstItems.addItem(new StorageOption(Forge.getLocalizer().getMessage("lblImageCacheLocation"), ForgeProfileProperties.getCacheDir()) {
                @Override
                protected void onDirectoryChanged(String newDir) {
                    ForgeProfileProperties.setCacheDir(newDir);

                    //ensure card pics option is updated if needed
                    cardPicsOption.updateDir(ForgeProfileProperties.getCardPicsDir());
                }
            }, 2);
            lstItems.addItem(cardPicsOption, 2);
            lstItems.addItem(decksOption, 2);
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
    private abstract class Extra extends FilesItem {
        Extra(String label0, String description0) {
            super(label0, description0);
        }

        @Override
        public void select() {

        }

        protected void finishCallback() {
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
                    final String name = url.substring(url.lastIndexOf("/") + 2);
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
            FFileChooser.show(Forge.getLocalizer().getMessage("lblSelect").replace("%s", label), ChoiceType.GetDirectory, description, new Callback<String>() {
                @Override
                public void run(String result) {
                    if (StringUtils.isEmpty(result) || description.equals(result)) { return; }
                    updateDir(result);
                    onDirectoryChanged(result);
                    FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblRestartForgeMoveFilesNewLocation"), Forge.getLocalizer().getMessage("lblRestartRequired"), FOptionPane.INFORMATION_ICON);
                }
            });
        }
        protected abstract void onDirectoryChanged(String newDir);
    }
}
