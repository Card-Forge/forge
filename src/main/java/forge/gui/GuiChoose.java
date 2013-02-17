package forge.gui;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.Card;
import forge.gui.match.CMatchUI;
import forge.item.CardPrinted;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GuiChoose {
    public static <T extends Comparable<? super T>> T oneOrNone(String message, List<T> choices, boolean sort, Comparator<T> sortComparator) {
        if ((choices == null) || (choices.size() == 0)) {
            return null;
        }
        final List<T> choice = _getChoices(message, 0, 1, choices, sort, sortComparator);
        return choice.isEmpty() ? null : choice.get(0);
    }
    public static <T extends Comparable<? super T>> T oneOrNone(String message, List<T> choices) {
        return oneOrNone(message, choices, false, null);
    }
    public static <T extends Comparable<? super T>> T oneOrNone(String message, T[] choices) {
        if ((choices == null)) { return null; }
        return oneOrNone(message, Arrays.asList(choices));
    }

    /**
     * Presents a list of choices where the player must choose exactly *amount* options.
     * @param message
     *          the message to display to the player.
     * @param choices
     *          the choices to choose from.
     * @param amount
     *          the amount of options that must be chosen.
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> amount(String message, List<T> choices, int amt, boolean sort, Comparator<T> sortComparator) {
        if (null == choices || 0 == choices.size() || 0 == amt) {
            return null;
        }
        final List<T> choice = _getChoices(message, amt, amt, choices, sort, sortComparator);
        assert choice.size() == amt;
        return choice;
    }
    public static <T extends Comparable<? super T>> List<T> amount(String message, List<T> choices, int amt) {
        return amount(message, choices, amt, false, null);
    }
    public static <T extends Comparable<? super T>> List<T> amount(String message, T[] choices, int amt) {
        if ((choices == null)) { return null; }
        return amount(message, Arrays.asList(choices), amt);
    }

    public static <T extends Comparable<? super T>> T one(String message, List<T> choices, boolean sort, Comparator<T> sortComparator) {
        List<T> choice = amount(message, choices, 1, sort, sortComparator);
        if (null == choice || choice.isEmpty()) {
            return null;
        }
        return choice.get(0);
    }
    public static <T extends Comparable<? super T>> T one(String message, List<T> choices) {
        return one(message, choices, false, null);
    }
    public static <T extends Comparable<? super T>> T one(String message, T[] choices) {
        if ((choices == null)) { return null; }
        return one(message, Arrays.asList(choices));
    }
    // Nothing to choose here. Code uses this to just show a card.
    public static Card one(String message, Card singleChoice) {
        List<Card> choices = Arrays.asList(singleChoice);
        return one(message, choices);
    }

    public static <T extends Comparable<? super T>> List<T> noneOrMany(String message, List<T> choices, boolean sort, Comparator<T> sortComparator) {
        if ((choices == null) || (choices.size() == 0)) {
            return null;
        }
        final List<T> choice = _getChoices(message, 0, choices.size(), choices, sort, sortComparator);
        return choice.isEmpty() ? null : choice;
    }
    public static <T extends Comparable<? super T>> List<T> noneOrMany(String message, List<T> choices) {
        return noneOrMany(message, choices, false, null);
    }

    public static <T extends Comparable<? super T>> List<T> oneOrMany(String message, List<T> choices, boolean sort, Comparator<T> sortComparator) {
        if ((choices == null) || (choices.size() == 0)) {
            return null;
        }
        final List<T> choice = _getChoices(message, 1, choices.size(), choices, sort, sortComparator);
        return choice.isEmpty() ? null : choice;
    }
    public static <T extends Comparable<? super T>> List<T> oneOrMany(String message, List<T> choices) {
        return oneOrMany(message, choices, false, null);
    }
    public static <T extends Comparable<? super T>> List<T> oneOrMany(String message, T[] choices) {
        if ((choices == null)) { return null; }
        return oneOrMany(message, Arrays.asList(choices));
    }

    // returned Object will never be null
    private static <T extends Comparable<? super T>> List<T> _getChoices(String message, int min, int max, List<T> choices,
            boolean sort, Comparator<T> sortComparator) {
        return _getChoices(new ListChooser<T>(message, min, max, choices, sort, sortComparator));
    }
    private static <T extends Comparable<? super T>> List<T> _getChoices(ListChooser<T> c) {
        final JList list = c.getJList();
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent ev) {
                if (list.getSelectedValue() instanceof Card) {
                    CMatchUI.SINGLETON_INSTANCE.setCard((Card) list.getSelectedValue());
                    GuiUtils.clearPanelSelections();
                    GuiUtils.setPanelSelection((Card) list.getSelectedValue());
                }
                if (list.getSelectedValue() instanceof InventoryItem) {
                    CMatchUI.SINGLETON_INSTANCE.setCard((InventoryItem) list.getSelectedValue());
                }
            }
        });
        
        c.show();
        GuiUtils.clearPanelSelections();
        return c.getSelectedValues();
    }

    public static <T extends Comparable<? super T>> List<T> order(String title, String top, int remainingObjects,
            List<T> sourceChoices, List<T> destChoices, Card referenceCard, boolean sort, Comparator<T> sortComparator) {
        return _order(title, top, remainingObjects, sourceChoices, destChoices, referenceCard, sort, sortComparator, false);
    }
    public static <T extends Comparable<? super T>> List<T> order(String title, String top, int remainingObjects,
            List<T> sourceChoices, List<T> destChoices, Card referenceCard) {
        return order(title, top, remainingObjects, sourceChoices, destChoices, referenceCard, false, null);
    }
    public static List<CardPrinted> sideboard(List<CardPrinted> sideboard, List<CardPrinted> deck) {
        return _order("Sideboard", "Main Deck", sideboard.size(), sideboard, deck, null, true, null, true);
    }

    private static <T extends Comparable<? super T>> List<T> _order(
            String title, String top, int remainingObjects, List<T> sourceChoices, List<T> destChoices,
            Card referenceCard, boolean startSorted, Comparator<T> sortComparator, boolean sideboardingMode) {
        // An input box for handling the order of choices.
        final JFrame frame = new JFrame();
        DualListBox<T> dual = new DualListBox<T>(remainingObjects, top, sourceChoices, destChoices, startSorted, sortComparator);
        dual.setSideboardMode(sideboardingMode);

        frame.setLayout(new BorderLayout());
        frame.setSize(dual.getPreferredSize());
        frame.add(dual);
        frame.setTitle(title);
        frame.setVisible(false);

        final JDialog dialog = new JDialog(frame, true);
        dialog.setTitle(title);
        dialog.setContentPane(dual);
        dialog.setSize(dual.getPreferredSize());
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        if (referenceCard != null) {
            CMatchUI.SINGLETON_INSTANCE.setCard(referenceCard);
            // MARKED FOR UPDATE
        }
        
        dialog.setVisible(true);

        List<T> objects = dual.getOrderedList();

        dialog.dispose();
        GuiUtils.clearPanelSelections();
        return objects;
    }
}
