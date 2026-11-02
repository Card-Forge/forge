package forge.screens.constructed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.FormatChoices;
import forge.deck.FormatChoices.ArchivedFolder;
import forge.game.GameFormat;
import forge.screens.FScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FList;
import forge.util.Utils;

/** The screen that FormatChoices.ARCHIVED opens. It lists one folder of archived formats at a time, and Back goes up a folder. */
public class ArchivedFormatScreen extends FScreen {
    private final FList<Object> lstEntries = add(new FList<>());
    private final Deque<Map.Entry<String, ArchivedFolder>> openFolders = new ArrayDeque<>();
    private final ArchivedFolder root = FormatChoices.getArchivedFolders();
    private final Consumer<GameFormat> onPick;
    private GameFormat picked;

    public ArchivedFormatScreen(final Consumer<GameFormat> onPick) {
        super(FormatChoices.ARCHIVED.toString());
        this.onPick = onPick;
        lstEntries.setListItemRenderer(new EntryRenderer());
        showFolder();
    }

    private void showFolder() {
        final ArchivedFolder folder = openFolders.isEmpty() ? root : openFolders.peek().getValue();
        final List<Object> entries = new ArrayList<>(folder.folders().entrySet());
        entries.addAll(folder.formats());
        lstEntries.setListData(entries);
        lstEntries.setScrollPosition(0);
        setHeaderCaption(openFolders.isEmpty() ? FormatChoices.ARCHIVED.toString() : openFolders.peek().getKey());
    }

    @Override
    public void onClose(final Consumer<Boolean> canCloseCallback) {
        // The callback is null when the app exits, which closes the screen whatever folder is open
        if (picked == null && !openFolders.isEmpty() && canCloseCallback != null) {
            openFolders.pop();
            showFolder();
            canCloseCallback.accept(false);
            return;
        }
        if (picked != null) {
            onPick.accept(picked);
        }
        super.onClose(canCloseCallback);
    }

    @Override
    protected void doLayout(final float startY, final float width, final float height) {
        lstEntries.setBounds(0, startY, width, height - startY);
    }

    private class EntryRenderer extends FList.ListItemRenderer<Object> {
        @Override
        public float getItemHeight() {
            return Utils.AVG_FINGER_HEIGHT;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean tap(final Integer index, final Object value, final float x, final float y, final int count) {
            if (value instanceof GameFormat format) {
                picked = format;
                Forge.back();
            } else {
                openFolders.push((Map.Entry<String, ArchivedFolder>) value);
                showFolder();
            }
            return true;
        }

        @Override
        public void drawValue(final Graphics g, final Integer index, final Object value, final FSkinFont font, final FSkinColor foreColor,
                final FSkinColor backColor, final boolean pressed, float x, float y, float w, float h) {
            final float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
            x += offset;
            y += offset;
            w -= 2 * offset;
            h -= 2 * offset;
            final String text;
            if (value instanceof GameFormat format) {
                text = format.getName();
            } else {
                g.drawImage(Forge.hdbuttons ? FSkinImage.HDFOLDER : FSkinImage.FOLDER, x, y, h, h);
                x += h + FList.PADDING;
                w -= h + FList.PADDING;
                text = ((Map.Entry<?, ?>) value).getKey().toString();
            }
            g.drawText(text, font, foreColor, x, y, w, h, false, Align.left, true);
        }
    }
}
