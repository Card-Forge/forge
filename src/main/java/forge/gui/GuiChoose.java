package forge.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.Card;
import forge.FThreads;
import forge.Singletons;
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
        if ((choices == null) || choices.isEmpty()) {
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

    public static <T> T one(final String message, final Collection<T> choices) {
        if ((choices == null) || (choices.size() == 0)) {
            return null;
        }
        final List<T> choice = GuiChoose.getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    }

    public static <T> List<T> noneOrMany(final String message, final Collection<T> choices) {
        return GuiChoose.getChoices(message, 0, choices.size(), choices, null);
    }

    // returned Object will never be null
    public static <T> List<T> getChoices(final String message, final int min, final int max, final T[] choices) {
        return getChoices(message, min, max, Arrays.asList(choices), null);
    }
    
    public static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices) {
        return getChoices(message, min, max, choices, null);
    }

    public static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices,final T selected) {
        if (null == choices || choices.isEmpty()) {
            if (0 == min) {
                return new ArrayList<T>();
            } else {
                throw new RuntimeException("choice required from empty list");
            }
        }
        
        Callable<List<T>> showChoice = new Callable<List<T>>() {
            @Override
            public List<T> call() {
                ListChooser<T> c = new ListChooser<T>(message, min, max, choices);
                final JList list = c.getJList();
                list.addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(final ListSelectionEvent ev) {
                        if (list.getSelectedValue() instanceof Card) {
                            Card card = (Card) list.getSelectedValue();
                            if (card.isFaceDown() && Singletons.getControl().mayShowCard(card)) {
                                CMatchUI.SINGLETON_INSTANCE.setCard(card, true);
                            } else {
                                CMatchUI.SINGLETON_INSTANCE.setCard(card);
                            }
        
                            GuiUtils.clearPanelSelections();
                            GuiUtils.setPanelSelection(card);
                        }
                        if (list.getSelectedValue() instanceof InventoryItem) {
                            CMatchUI.SINGLETON_INSTANCE.setCard((InventoryItem) list.getSelectedValue());
                        }
                    }
                });
                
                if(selected != null)
                    c.show(selected);
                else
                    c.show();
                
                GuiUtils.clearPanelSelections();
                return c.getSelectedValues();
            }
        };

        FutureTask<List<T>> future = new FutureTask<List<T>>(showChoice);
        FThreads.invokeInEdtAndWait(future);
        try { 
            return future.get();
        } catch (Exception e) { // should be no exception here
            e.printStackTrace();
        }
        return null;
    }

    // Nothing to choose here. Code uses this to just show a card.
    public static Card show(final String message, final Card singleChoice) {
        List<Card> choices = new ArrayList<Card>();
        choices.add(singleChoice);
        return one(message, choices);
    }

    public static <T> List<T> order(final String title, final String top, int remainingObjects,
            final List<T> sourceChoices, List<T> destChoices, Card referenceCard) {
        return order(title, top, remainingObjects, sourceChoices, destChoices, referenceCard, false);
    }

    public static <T extends Comparable<? super T>> List<T> sideboard(List<T> sideboard, List<T> deck) {
        Collections.sort(deck);
        Collections.sort(sideboard);
        return order("Sideboard", "Main Deck", -1, sideboard, deck, null, true);
    }

    
    public static <T> List<T> order(final String title, final String top, final int remainingObjects,
            final List<T> sourceChoices, final List<T> destChoices, final Card referenceCard, final boolean sideboardingMode) {
        // An input box for handling the order of choices.
        
        Callable<List<T>> callable = new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                final JFrame frame = new JFrame();
                DualListBox<T> dual = new DualListBox<T>(remainingObjects, sourceChoices, destChoices);
                dual.setSecondColumnLabelText(top);
        
                frame.setLayout(new BorderLayout());
                frame.setSize(dual.getPreferredSize());
                frame.add(dual);
                frame.setTitle(title);
                frame.setVisible(false);
        
                dual.setSideboardMode(sideboardingMode);
        
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
        };

        FutureTask<List<T>> ft = new FutureTask<List<T>>(callable);
        FThreads.invokeInEdtAndWait(ft);
        try {
            return ft.get();
        } catch (Exception e) { // we have waited enough
            e.printStackTrace();
        }
        return null;
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> T sortedOneOrNone(final String message, final T[] choices, Comparator<T> comparer) {
        if ((choices == null) || (choices.length == 0)) {
            return null;
        }
        final List<T> choice = GuiChoose.sortedGetChoices(message, 0, 1, choices, comparer);
        return choice.isEmpty() ? null : choice.get(0);
    } // getChoiceOptional(String,T...)

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> T sortedOneOrNone(final String message, final List<T> choices, Comparator<T> comparer) {
        if ((choices == null) || choices.isEmpty()) {
            return null;
        }
        final List<T> choice = GuiChoose.sortedGetChoices(message, 0, 1, choices, comparer);
        return choice.isEmpty() ? null : choice.get(0);
    } // getChoiceOptional(String,T...)


    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> T sortedOne(final String message, final T[] choices, Comparator<T> comparer) {
        final List<T> choice = GuiChoose.sortedGetChoices(message, 1, 1, choices, comparer);
        assert choice.size() == 1;
        return choice.get(0);
    } // getChoice()

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> T sortedOne(final String message, final List<T> choices, Comparator<T> comparer) {
        if ((choices == null) || (choices.size() == 0)) {
            return null;
        }
        final List<T> choice = GuiChoose.sortedGetChoices(message, 1, 1, choices, comparer);
        assert choice.size() == 1;
        return choice.get(0);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> List<T> sortedNoneOrMany(final String message, final List<T> choices, Comparator<T> comparer) {
        return GuiChoose.sortedGetChoices(message, 0, choices.size(), choices, comparer);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> List<T> sortedGetChoices(final String message, final int min, final int max, final T[] choices, Comparator<T> comparer) {
        // You may create a copy of source array if callers expect the collection to be unchanged
        Arrays.sort(choices, comparer);
        return getChoices(message, min, max, choices);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> List<T> sortedGetChoices(final String message, final int min, final int max, final List<T> choices, Comparator<T> comparer) {
        // You may create a copy of source list if callers expect the collection to be unchanged
        Collections.sort(choices, comparer);
        return getChoices(message, min, max, choices);
    }

}

