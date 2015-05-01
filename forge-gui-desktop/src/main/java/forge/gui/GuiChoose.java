package forge.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.JList;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;

import forge.FThreads;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.item.InventoryItem;
import forge.screens.match.CMatchUI;
import forge.toolbox.FOptionPane;


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

    // Get Integer in range
    public static Integer getInteger(final String message, final int min, final int max) {
        if (max <= min) { return min; } //just return min if max <= min

        //force cutting off after 100 numbers at most
        if (max == Integer.MAX_VALUE) {
            return getInteger(message, min, max, min + 99);
        }
        final int count = max - min + 1;
        if (count > 100) {
            return getInteger(message, min, max, min + 99);
        }

        final Integer[] choices = new Integer[count];
        for (int i = 0; i < count; i++) {
            choices[i] = Integer.valueOf(i + min);
        }
        return GuiChoose.oneOrNone(message, choices);
    }
    public static Integer getInteger(final String message, final int min, final int max, final int cutoff) {
        if (max <= min || cutoff < min) { return min; } //just return min if max <= min or cutoff < min

        if (cutoff >= max) { //fallback to regular integer prompt if cutoff at or after max
            return getInteger(message, min, max);
        }

        final List<Object> choices = new ArrayList<Object>();
        for (int i = min; i <= cutoff; i++) {
            choices.add(Integer.valueOf(i));
        }
        choices.add("Other...");

        final Object choice = GuiChoose.oneOrNone(message, choices);
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
            final String str = FOptionPane.showInputDialog(prompt, message);
            if (str == null) { return null; } // that is 'cancel'

            if (StringUtils.isNumeric(str)) {
                final Integer val = Integer.valueOf(str);
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
        return getChoices(message, min, max, choices, selected, display, null);
    }
    public static <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display, final CMatchUI matchUI) {
        if (choices == null || choices.isEmpty()) {
            if (min == 0) {
                return new ArrayList<T>();
            }
            throw new RuntimeException("choice required from empty list");
        }

        final Callable<List<T>> showChoice = new Callable<List<T>>() {
            @Override
            public List<T> call() {
                final ListChooser<T> c = new ListChooser<T>(message, min, max, choices, display);
                final JList<T> list = c.getLstChoices();
                if (matchUI != null) {
                    list.addListSelectionListener(new ListSelectionListener() {
                        @Override
                        public void valueChanged(final ListSelectionEvent ev) {
                            final T sel = list.getSelectedValue();
                            if (sel instanceof InventoryItem) {
                                matchUI.setCard((InventoryItem) list.getSelectedValue());
                                return;
                            }

                            final CardView card;
                            if (sel instanceof CardStateView) {
                                card = ((CardStateView) sel).getCard();
                            } else if (sel instanceof CardView) {
                                card = (CardView) sel;
                            } else if (sel instanceof Card) {
                                card = CardView.get((Card) sel);
                            } else {
                                card = null;
                            }

                            matchUI.setCard(card);
                            matchUI.clearPanelSelections();
                            matchUI.setPanelSelection(card);
                        }
                    });
                }

                if (selected != null) {
                    c.show(selected);
                }
                else {
                    c.show();
                }

                if (matchUI != null) {
                    matchUI.clearPanelSelections();
                }
                return c.getSelectedValues();
            }
        };

        final FutureTask<List<T>> future = new FutureTask<List<T>>(showChoice);
        FThreads.invokeInEdtAndWait(future);
        try {
            return future.get();
        } catch (final Exception e) { // should be no exception here
            e.printStackTrace();
        }
        return null;
    }

    public static <T extends Comparable<? super T>> List<T> sideboard(final CMatchUI matchUI, final List<T> sideboard, final List<T> deck) {
        Collections.sort(deck);
        Collections.sort(sideboard);
        return order("Sideboard", "Main Deck", -1, -1, sideboard, deck, null, true, matchUI);
    }

    public static <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices) {
        return order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, null, false, null);
    }
    public static <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode, final CMatchUI matchUI) {
        // An input box for handling the order of choices.

        final Callable<List<T>> callable = new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                final DualListBox<T> dual = new DualListBox<T>(remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, matchUI);
                dual.setSecondColumnLabelText(top);

                dual.setSideboardMode(sideboardingMode);

                dual.setTitle(title);
                dual.pack();
                dual.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                if (matchUI != null && referenceCard != null) {
                    matchUI.setCard(referenceCard);
                    // MARKED FOR UPDATE
                }
                dual.setVisible(true);

                final List<T> objects = dual.getOrderedList();

                dual.dispose();
                if (matchUI != null) {
                    matchUI.clearPanelSelections();
                }
                return objects;
            }
        };

        final FutureTask<List<T>> ft = new FutureTask<List<T>>(callable);
        FThreads.invokeInEdtAndWait(ft);
        try {
            return ft.get();
        } catch (final Exception e) { // we have waited enough
            e.printStackTrace();
        }
        return null;
    }

}

