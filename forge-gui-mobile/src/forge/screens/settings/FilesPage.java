package forge.screens.settings;

import forge.download.*;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.properties.ForgeProfileProperties;
import forge.screens.TabPageScreen.TabPage;
import forge.toolbox.FFileChooser;
import forge.toolbox.FFileChooser.ChoiceType;
import forge.toolbox.FGroupList;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

public class FilesPage extends TabPage<SettingsScreen> {
    private final FGroupList<FilesItem> lstItems = add(new FGroupList<FilesItem>());

    protected FilesPage() {
        super("Files", FSkinImage.OPEN);

        lstItems.setListItemRenderer(new FilesItemRenderer());

        lstItems.addGroup("Content Downloaders");
        lstItems.addGroup("Storage Locations");
        //lstItems.addGroup("Data Import");

        //content downloaders
        lstItems.addItem(new ContentDownloader("Download LQ Card Pictures",
                "Download default card picture for each card.") {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadPicturesLQ();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader("Download LQ Set Pictures",
                "Download all pictures of each card (one for each set the card appeared in)") {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadSetPicturesLQ();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader("Download Quest Images",
                "Download tokens and icons used in Quest mode.") {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadQuestImages();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader("Download Achievement Images",
                "Download achievement images to really make your trophies stand out.") {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadAchievementImages();
            }
        }, 0);
        lstItems.addItem(new ContentDownloader("Download Card Prices",
                "Download up-to-date price list for in-game card shops.") {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadPrices();
            }
        }, 0);

        //storage locations
        lstItems.addItem(new StorageOption("Data Location (e.g. Settings, Decks, Quests)", ForgeProfileProperties.getUserDir()) {
            @Override
            protected void onDirectoryChanged(String newDir) {
                ForgeProfileProperties.setUserDir(newDir);
            }
        }, 1);
        final StorageOption cardPicsOption = new StorageOption("Card Pics Location", ForgeProfileProperties.getCardPicsDir()) {
            @Override
            protected void onDirectoryChanged(String newDir) {
                ForgeProfileProperties.setCardPicsDir(newDir);
            }
        };
        lstItems.addItem(new StorageOption("Image Cache Location", ForgeProfileProperties.getCacheDir()) {
            @Override
            protected void onDirectoryChanged(String newDir) {
                ForgeProfileProperties.setCacheDir(newDir);

                //ensure card pics option is updated if needed
                cardPicsOption.updateDir(ForgeProfileProperties.getCardPicsDir());
            }
        }, 1);
        lstItems.addItem(cardPicsOption, 1);
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

            g.drawText(value.label, font, foreColor, x, y, w, h, false, HAlignment.LEFT, false);
            h += SettingsScreen.SETTING_PADDING;
            g.drawText(value.description, SettingsScreen.DESC_FONT, SettingsScreen.DESC_COLOR, x, y + h, w, totalHeight - h + SettingsScreen.getInsets(w), true, HAlignment.LEFT, false);
        }
    }

    private abstract class ContentDownloader extends FilesItem {
        ContentDownloader(String label0, String description0) {
            super(label0, description0);
        }

        @Override
        public void select() {
            new GuiDownloader(createService()).show();
        }
        protected abstract GuiDownloadService createService();
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
            FFileChooser.show("Select " + label, ChoiceType.GetDirectory, description, new Callback<String>() {
                @Override
                public void run(String result) {
                    if (StringUtils.isEmpty(result) || description.equals(result)) { return; }
                    updateDir(result);
                    onDirectoryChanged(result);
                    FOptionPane.showMessageDialog("You'll need to restart Forge for this change to take effect. Be sure to move any necessary files to the new location before you do.", "Restart Required", FOptionPane.INFORMATION_ICON);
                }
            });
        }
        protected abstract void onDirectoryChanged(String newDir);
    }
}
