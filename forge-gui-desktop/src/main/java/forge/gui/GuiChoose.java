package forge.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.JList;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import forge.FThreads;
import forge.GuiBase;
import forge.Singletons;
import forge.item.InventoryItem;
import forge.screens.match.CMatchUI;
import forge.toolbox.FOptionPane;
import forge.view.CardView;

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
    }

    public static <T> T one(final String message, final Collection<T> choices) {
        if (choices == null || choices.isEmpty())
            return null;
        if( choices.size() == 1)
            return Iterables.getFirst(choices, null);

        final List<T> choice = GuiChoose.getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    }

    public static <T> List<T> noneOrMany(final String message, final Collection<T> choices) {
        return GuiChoose.getChoices(message, 0, choices.size(), choices, null, null);
    }

    // Nothing to choose here. Code uses this to just reveal one or more items
    public static <T> void reveal(final String message, final T item) {
        List<T> items = new ArrayList<T>();
        items.add(item);
        reveal(message, items);
    }
    public static <T> void reveal(final String message, final T[] items) {
        GuiChoose.getChoices(message, -1, -1, items);
    }
    public static <T> void reveal(final String message, final Collection<T> items) {
        GuiChoose.getChoices(message, -1, -1, items);
    }

    // Get Integer in range
    public static Integer getInteger(final String message) {
        return getInteger(message, 0, Integer.MAX_VALUE);
    }
    public static Integer getInteger(final String message, int min) {
        return getInteger(message, min, Integer.MAX_VALUE);
    }
    public static Integer getInteger(final String message, int min, int max) {
        if (max <= min) { return min; } //just return min if max <= min

        //force cutting off after 100 numbers at most
        if (max == Integer.MAX_VALUE) {
            return getInteger(message, min, max, min + 99);
        }
        int count = max - min + 1;
        if (count > 100) { 
            return getInteger(message, min, max, min + 99);
        }

        final Integer[] choices = new Integer[count];
        for (int i = 0; i < count; i++) {
            choices[i] = Integer.valueOf(i + min);
        }
        return GuiChoose.oneOrNone(message, choices);
    }
    public static Integer getInteger(final String message, int min, int max, int cutoff) {
        if (max <= min || cutoff < min) { return min; } //just return min if max <= min or cutoff < min

        if (cutoff >= max) { //fallback to regular integer prompt if cutoff at or after max
            return getInteger(message, min, max);
        }

        List<Object> choices = new ArrayList<Object>();
        for (int i = min; i <= cutoff; i++) {
            choices.add(Integer.valueOf(i));
        }
        choices.add("Other...");

        Object choice = GuiChoose.oneOrNone(message, choices);
        if (choice instanceof Integer || choice == null) {
            return (Integer)choice;
        }

        //if Other option picked, prompt for number input
        String prompt = "Enter a number";
        if (min != Integer.MIN_VALUE) {
            if (max != Integer.MAX_VALUE) {
                prompt += " between " + min + " and " + max;
            }
            else {
                prompt += " greater than or equal to " + min;
            }
        }
        else if (max != Integer.MAX_VALUE) {
            prompt += " less than or equal to " + max;
        }
        prompt += ":";

        while (true) {
            String str = FOptionPane.showInputDialog(prompt, message);
            if (str == null) { return null; } // that is 'cancel'

            if (StringUtils.isNumeric(str)) {
                Integer val = Integer.valueOf(str);
                if (val >= min && val <= max) {
                    return val;
                }
            }
        }
    }

    // returned Object will never be null
    public static <T> List<T> getChoices(final String message, final int min, final int max, final T[] choices) {
        return getChoices(message, min, max, Arrays.asList(choices), null, null);
    }

    public static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices) {
        return getChoices(message, min, max, choices, null, null);
    }

    public static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
        if (choices == null || choices.isEmpty()) {
            if (min == 0) {
                return new ArrayList<T>();
            }
            throw new RuntimeException("choice required from empty list");
        }

        Callable<List<T>> showChoice = new Callable<List<T>>() {
            @Override
            public List<T> call() {
                ListChooser<T> c = new ListChooser<T>(message, min, max, choices, display);
                final JList<T> list = c.getLstChoices();
                list.addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(final ListSelectionEvent ev) {
                        if (list.getSelectedValue() instanceof CardView) {
                            final CardView card = (CardView) list.getSelectedValue();
                            if (card.isFaceDown() && Singletons.getControl().mayShowCard(card)) {
                                CMatchUI.SINGLETON_INSTANCE.setCard(card, true);
                            }
                            else {
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

                if (selected != null) {
                    c.show(selected);
                }
                else {
                    c.show();
                }

                GuiUtils.clearPanelSelections();
                return c.getSelectedValues();
            }
        };

        FutureTask<List<T>> future = new FutureTask<List<T>>(showChoice);
        FThreads.invokeInEdtAndWait(GuiBase.getInterface(), future);
        try {
            return future.get();
        } catch (Exception e) { // should be no exception here
            e.printStackTrace();
        }
        return null;
    }

    public static <T> List<T> many(final String title, final String topCaption, int cnt, final List<T> sourceChoices, final CardView referenceCard) {
        return order(title, topCaption, cnt, cnt, sourceChoices, null, referenceCard, false);
    }

    public static <T> List<T> many(final String title, final String topCaption, int min, int max, final List<T> sourceChoices, final CardView referenceCard) {
        int m2 = min >= 0 ? sourceChoices.size() - min : -1;
        int m1 = max >= 0 ? sourceChoices.size() - max : -1;
        return order(title, topCaption, m1, m2, sourceChoices, null, referenceCard, false);
    }

    public static <T> List<T> order(final String title, final String top, final List<T> sourceChoices, final CardView referenceCard) {
        return order(title, top, 0, 0, sourceChoices, null, referenceCard, false);
    }

    public static <T extends Comparable<? super T>> List<T> sideboard(List<T> sideboard, List<T> deck) {
        Collections.sort(deck);
        Collections.sort(sideboard);
        return order("Sideboard", "Main Deck", -1, -1, sideboard, deck, null, true);
    }

    public static <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        // An input box for handling the order of choices.

        Callable<List<T>> callable = new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                DualListBox<T> dual = new DualListBox<T>(remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices);
                dual.setSecondColumnLabelText(top);

                dual.setSideboardMode(sideboardingMode);

                dual.setTitle(title);
                dual.pack();
                dual.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                if (referenceCard != null) {
                    CMatchUI.SINGLETON_INSTANCE.setCard(referenceCard);
                    // MARKED FOR UPDATE
                }
                dual.setVisible(true);

                List<T> objects = dual.getOrderedList();

                dual.dispose();
                GuiUtils.clearPanelSelections();
                return objects;
            }
        };

        FutureTask<List<T>> ft = new FutureTask<List<T>>(callable);
        FThreads.invokeInEdtAndWait(GuiBase.getInterface(), ft);
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

