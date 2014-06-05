package forge.screens.settings;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.download.GuiDownloadPicturesLQ;
import forge.download.GuiDownloadPrices;
import forge.download.GuiDownloadQuestImages;
import forge.download.GuiDownloadService;
import forge.download.GuiDownloadSetPicturesLQ;
import forge.screens.TabPageScreen.TabPage;
import forge.toolbox.FGroupList;
import forge.toolbox.FList;

public class FilesPage extends TabPage {
    private final FGroupList<FilesItem> lstItems = add(new FGroupList<FilesItem>());

    protected FilesPage() {
        super("Files", FSkinImage.OPEN);

        lstItems.setListItemRenderer(new FilesItemRenderer());

        lstItems.addGroup("Content Downloaders");
        lstItems.addGroup("Storage Options");
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
        lstItems.addItem(new ContentDownloader("Download Card Prices",
                "Download up-to-date price list for in-game card shops.") {
            @Override
            protected GuiDownloadService createService() {
                return new GuiDownloadPrices();
            }
        }, 0);
    }

    @Override
    protected void doLayout(float width, float height) {
        lstItems.setBounds(0, 0, width, height);
    }

    private abstract class FilesItem {
        protected String label;
        protected String description;

        public FilesItem(String label0, String description0) {
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
        public void drawValue(Graphics g, Integer index, FilesItem value, FSkinFont font, FSkinColor color, boolean pressed, float x, float y, float w, float h) {
            float offset = w * SettingsScreen.INSETS_FACTOR - FList.PADDING; //increase padding for settings items
            x += offset;
            y += offset;
            w -= 2 * offset;
            h -= 2 * offset;

            float totalHeight = h;
            h = font.getMultiLineBounds(value.label).height + SettingsScreen.SETTING_PADDING;

            g.drawText(value.label, font, color, x, y, w, h, false, HAlignment.LEFT, false);
            h += SettingsScreen.SETTING_PADDING;
            g.drawText(value.description, SettingsScreen.DESC_FONT, SettingsScreen.DESC_COLOR, x, y + h, w, totalHeight - h + w * SettingsScreen.INSETS_FACTOR, true, HAlignment.LEFT, false);            
        }
    }

    private abstract class ContentDownloader extends FilesItem {
        public ContentDownloader(String label0, String description0) {
            super(label0, description0);
        }

        @Override
        public void select() {
            new GuiDownloader(createService());
        }
        protected abstract GuiDownloadService createService();
    }

    private enum StorageOptions {
        
    }

    private class StorageOption extends FilesItem {
        public StorageOption(String label0, String description0) {
            super(label0, description0);
        }

        @Override
        public void select() {
        }
    }
}
