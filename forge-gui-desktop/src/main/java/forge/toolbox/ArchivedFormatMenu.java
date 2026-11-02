package forge.toolbox;

import java.awt.Container;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import forge.deck.FormatChoices;
import forge.deck.FormatChoices.ArchivedFolder;
import forge.game.GameFormat;
import forge.gui.GuiUtils;
import forge.gui.MenuScroller;

/** The menu that FormatChoices.ARCHIVED opens, with a submenu for each folder of archived formats. */
public final class ArchivedFormatMenu {
    private static final int MAX_VISIBLE_ITEMS = 20;

    private ArchivedFormatMenu() {
    }

    /** Opens the menu beside the combo. */
    public static void show(final FComboBox<Object> combo, final Consumer<GameFormat> onPick) {
        final JPopupMenu menu = new JPopupMenu();
        fill(menu, FormatChoices.getArchivedFolders(), onPick);
        MenuScroller.setScrollerFor(menu, MAX_VISIBLE_ITEMS);
        menu.show(combo, combo.getWidth(), 0);
    }

    private static void fill(final Container menu, final ArchivedFolder folder, final Consumer<GameFormat> onPick) {
        for (final Map.Entry<String, ArchivedFolder> entry : folder.folders().entrySet()) {
            final JMenu submenu = GuiUtils.createMenu(entry.getKey());
            fill(submenu, entry.getValue(), onPick);
            MenuScroller.setScrollerFor(submenu, MAX_VISIBLE_ITEMS);
            menu.add(submenu);
        }
        for (final GameFormat format : folder.formats()) {
            menu.add(GuiUtils.createMenuItem(format.getName(), null, () -> onPick.accept(format), true, false));
        }
    }
}
