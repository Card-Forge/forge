package forge.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.ListSelectionModel;

import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.util.Localizer;
import forge.view.FDialog;

/**
 * Lets the player pick one entry from a list, previewing the card behind each entry.
 * Used by the lobby to choose a commander (and optionally a partner) for the next match.
 * Unlike {@link CardListChooser}, entries carry their own labels and the dialog can be cancelled.
 */
@SuppressWarnings("serial")
public class CommanderChooser<T> extends FDialog {
    private final List<T> items;
    private final Function<T, PaperCard> previewCard;
    private final FList<String> list;
    private final CardDetailPanel detail = new CardDetailPanel();
    private final CardPicturePanel picture = new CardPicturePanel();
    private boolean confirmed = false;

    /**
     * Shows the dialog and waits for the player.
     * @param label the text shown for an entry
     * @param previewCard the card to preview for an entry, or null for none
     * @return the chosen entry, or null if the dialog was cancelled
     */
    public static <T> T choose(final String title, final String message, final List<T> items, final int selectedIndex,
            final Function<T, String> label, final Function<T, PaperCard> previewCard) {
        final CommanderChooser<T> chooser = new CommanderChooser<>(title, message, items, label, previewCard);
        chooser.list.setSelectedIndex(Math.max(0, Math.min(selectedIndex, items.size() - 1)));
        chooser.list.ensureIndexIsVisible(chooser.list.getSelectedIndex());
        chooser.setVisible(true);
        final int chosen = chooser.list.getSelectedIndex();
        chooser.dispose();
        return chooser.confirmed && chosen >= 0 ? items.get(chosen) : null;
    }

    private CommanderChooser(final String title, final String message, final List<T> items,
            final Function<T, String> label, final Function<T, PaperCard> previewCard) {
        this.items = items;
        this.previewCard = previewCard;
        
        picture.setOpaque(false);

        final List<String> labels = new ArrayList<>(items.size());
        for (final T item : items) {
            labels.add(label.apply(item));
        }
        list = new FList<>();
        list.setListData(labels.toArray(new String[0]));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> updatePreview());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedIndex() >= 0) {
                    confirm();
                }
            }
        });

        setTitle(title);
        final boolean large = FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS);
        setSize(large ? 1200 : 760, large ? 825 : 420);

        final Localizer localizer = Localizer.getInstance();
        final FButton btnOK = new FButton(localizer.getMessage("lblOK"));
        btnOK.addActionListener(e -> confirm());
        final FButton btnCancel = new FButton(localizer.getMessage("lblCancel"));
        btnCancel.addActionListener(e -> setVisible(false));

        add(new FLabel.Builder().text(message).build(), "cell 0 0, spanx 3, gapbottom 4");
        if (large) {
            add(new FScrollPane(list, true), "cell 0 1, w 225, h 450, ax c");
            add(picture, "cell 1 1, w 480, growy, pushy, ax c");
            add(detail, "cell 2 1, w 320, h 500, ax c");
        } else {
            add(new FScrollPane(list, true), "cell 0 1, w 260, growy, pushy, ax c");
            add(picture, "cell 1 1, w 225, growy, pushy, ax c");
            add(detail, "cell 2 1, w 225, growy, pushy, ax c");
        }
        add(btnOK, "cell 0 2, spanx 3, split 2, w 120, h 30, ax c, gaptop 6");
        add(btnCancel, "w 120, h 30, gaptop 6");
        getRootPane().setDefaultButton(btnOK);
    }

    private void confirm() {
        confirmed = true;
        setVisible(false);
    }

    private void updatePreview() {
        final int row = list.getSelectedIndex();
        final PaperCard card = row >= 0 && row < items.size() ? previewCard.apply(items.get(row)) : null;
        if (card == null) {
            detail.setCard(null);
            picture.setItem((BufferedImage) null);
        } else {
            detail.setCard(CardView.getCardForUi(card));
            picture.setItem(card);
        }
    }
}
