package forge.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.Card;
import forge.deck.DeckTempStorage;
import forge.gui.match.CMatchUI;
import forge.item.InventoryItem;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GuiChoose {

    /**
     * Convenience for getChoices(message, 0, 1, choices).
     * 
     * @param <T>
     *            is automatically inferred.
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @return null if choices is missing, empty, or if the users' choices are
     *         empty; otherwise, returns the first item in the List returned by
     *         getChoices.
     * @see #getChoices(String, int, int, Object...)
     */
    public static <T> T oneOrNone(final String message, final T[] choices) {
        if ((choices == null) || (choices.length == 0)) {
            return null;
        }
        final List<T> choice = GuiChoose.getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    } // getChoiceOptional(String,T...)

    public static <T> T oneOrNone(final String message, final Collection<T> choices) {
        if ((choices == null) || (choices.size() == 0)) {
            return null;
        }
        final List<T> choice = GuiChoose.getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    } // getChoiceOptional(String,T...)

    // returned Object will never be null
    /**
     * <p>
     * getChoice.
     * </p>
     * 
     * @param <T>
     *            a T object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @return a T object.
     */
    public static <T> T one(final String message, final T[] choices) {
        final List<T> choice = GuiChoose.getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    } // getChoice()

    // Nothing to choose here. Code uses this to just show a card.
    public static Card one(final String message, final Card singleChoice) {
        List<Card> choices = new ArrayList<Card>();
        choices.add(singleChoice);
        return one(message, choices);
    }

    public static <T> T one(final String message, final Collection<T> choices) {
        final List<T> choice = GuiChoose.getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    }

    // returned Object will never be null
    /**
     * <p>
     * getChoicesOptional.
     * </p>
     * 
     * @param <T>
     *            a T object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @return a {@link java.util.List} object.
     */
    public static <T> List<T> noneOrMany(final String message, final T[] choices) {
        return GuiChoose.getChoices(message, 0, choices.length, choices);
    } // getChoice()

    public static <T> List<T> noneOrMany(final String message, final List<T> choices) {
        return GuiChoose.getChoices(message, 0, choices.size(), choices);
    }

    // returned Object will never be null
    /**
     * <p>
     * getChoices.
     * </p>
     * 
     * @param <T>
     *            a T object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @return a {@link java.util.List} object.
     */
    public static <T> List<T> oneOrMany(final String message, final T[] choices) {
        return GuiChoose.getChoices(message, 1, choices.length, choices);
    } // getChoice()

    //returned Object will never be null
    /**
     * <p>
     * Presents a list of choices where the player must choose exactly *amount* options.
     * @param message
     *          the message to display to the player.
     * @param choices
     *          the choices to choose from.
     * @param amount
     *          the amount of options that must be chosen.
     * @return
     */
    public static <T> List<T> amount(final String message, final T[] choices, final int amount) {
        if ((choices == null) || (choices.length == 0) || choices.length < amount) {
            return null;
        }

        return GuiChoose.getChoices(message, amount, amount, choices);
    }

    public static <T> List<T> amount(final String message, final Collection<T> choices, final int amount) {
        if ((choices == null) || (choices.size() == 0) || choices.size() < amount) {
            return null;
        }

        return GuiChoose.getChoices(message, amount, amount, choices);
    }

    // returned Object will never be null
    /**
     * <p>
     * getChoices.
     * </p>
     * 
     * @param <T>
     *            a T object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param min
     *            a int.
     * @param max
     *            a int.
     * @param choices
     *            a T object.
     * @return a {@link java.util.List} object.
     */
    private static <T> List<T> getChoices(final String message, final int min, final int max, final T[] choices) {
        final ListChooser<T> c = new ListChooser<T>(message, null, min, max, choices);
        return getChoices(c);
    }

    private static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices) {
        final ListChooser<T> c = new ListChooser<T>(message, min, max, choices);
        return getChoices(c);
    }

    private static <T> List<T> getChoices(final ListChooser<T> c) {
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
    } // getChoice()

    public static <T> List<T> getOrderChoices(final String title, final String top, int remainingObjects,
            final List<T> sourceChoices, List<T> destChoices, Card referenceCard) {
        return getOrderChoices(title, top, remainingObjects, sourceChoices, destChoices, referenceCard, false);
    }

    public static <T> List<T> getOrderChoices(final String title, final String top, int remainingObjects,
            final List<T> sourceChoices, List<T> destChoices, Card referenceCard, boolean sideboardingMode) {
        // An input box for handling the order of choices.
        final JFrame frame = new JFrame();
        DualListBox<T> dual = new DualListBox<T>(remainingObjects, top, sourceChoices, destChoices, referenceCard, sideboardingMode);

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
        if (sideboardingMode) {
            DeckTempStorage.setHumanMain((List<Card>)objects);
            DeckTempStorage.setHumanSideboard((List<Card>)dual.getRemainingSourceList());
        }

        dialog.dispose();
        GuiUtils.clearPanelSelections();
        return objects;
    }

}

