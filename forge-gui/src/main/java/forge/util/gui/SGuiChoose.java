package forge.util.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.GuiBase;
import forge.view.CardView;

public class SGuiChoose {
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
        final List<T> choice = SGuiChoose.getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    }

    public static <T> T oneOrNone(final String message, final Collection<T> choices) {
        if ((choices == null) || choices.isEmpty()) {
            return null;
        }
        final List<T> choice = SGuiChoose.getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    }

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
        final List<T> choice = SGuiChoose.getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    }

    public static <T> T one(final String message, final Collection<T> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        if (choices.size() == 1) {
            return Iterables.getFirst(choices, null);
        }

        final List<T> choice = SGuiChoose.getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    }

    public static <T> List<T> noneOrMany(final String message, final Collection<T> choices) {
        return SGuiChoose.getChoices(message, 0, choices.size(), choices, null, null);
    }

    // Nothing to choose here. Code uses this to just reveal one or more items
    public static <T> void reveal(final String message, final T item) {
        List<T> items = new ArrayList<T>();
        items.add(item);
        reveal(message, items);
    }
    public static <T> void reveal(final String message, final T[] items) {
        SGuiChoose.getChoices(message, -1, -1, items);
    }
    public static <T> void reveal(final String message, final Collection<T> items) {
        SGuiChoose.getChoices(message, -1, -1, items);
    }

    // Get Integer in range
    public static Integer getInteger(final String message) {
        return getInteger(message, 0, Integer.MAX_VALUE, false);
    }
    public static Integer getInteger(final String message, int min) {
        return getInteger(message, min, Integer.MAX_VALUE, false);
    }
    public static Integer getInteger(final String message, int min, int max) {
        return getInteger(message, min, max, false);
    }
    public static Integer getInteger(final String message, int min, int max, boolean sortDesc) {
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
        if (sortDesc) {
            for (int i = 0; i < count; i++) {
                choices[count - i - 1] = Integer.valueOf(i + min);
            }
        }
        else {
            for (int i = 0; i < count; i++) {
                choices[i] = Integer.valueOf(i + min);
            }
        }
        return SGuiChoose.oneOrNone(message, choices);
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

        Object choice = SGuiChoose.oneOrNone(message, choices);
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
            String str = SOptionPane.showInputDialog(prompt, message);
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
        return GuiBase.getInterface().getChoices(message, min, max, choices, selected, display);
    }

    public static <T> List<T> many(final String title, final String topCaption, int cnt, final List<T> sourceChoices, final CardView referenceCard) {
        return many(title, topCaption, cnt, cnt, sourceChoices, referenceCard);
    }

    public static <T> List<T> many(final String title, final String topCaption, int min, int max, final List<T> sourceChoices, final CardView referenceCard) {
        int m2 = min >= 0 ? sourceChoices.size() - min : -1;
        int m1 = max >= 0 ? sourceChoices.size() - max : -1;
        return order(title, topCaption, m1, m2, sourceChoices, null, referenceCard, false);
    }

    public static <T> List<T> order(final String title, final String top, final List<T> sourceChoices, final CardView referenceCard) {
        return order(title, top, 0, 0, sourceChoices, null, referenceCard, false);
    }

    /**
     * Ask the user to insert an object into a list of other objects. The
     * current implementation requires the user to cancel in order to get the
     * new item to be the first item in the resulting list.
     * 
     * @param title the dialog title.
     * @param newItem the object to insert.
     * @param oldItems the list of objects.
     * @return A shallow copy of the list of objects, with newItem inserted.
     */
    public static <T> List<T> insertInList(final String title, final T newItem, final List<T> oldItems) {
		final T placeAfter = oneOrNone(title, oldItems);
    	final int indexAfter = (placeAfter == null ? 0 : oldItems.indexOf(placeAfter) + 1);
    	final List<T> result = Lists.newArrayListWithCapacity(oldItems.size() + 1);
    	result.addAll(oldItems);
    	result.add(indexAfter, newItem);
    	return result;
    }

    private static <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        return GuiBase.getInterface().order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> T sortedOneOrNone(final String message, final T[] choices, Comparator<T> comparer) {
        if ((choices == null) || (choices.length == 0)) {
            return null;
        }
        final List<T> choice = SGuiChoose.sortedGetChoices(message, 0, 1, choices, comparer);
        return choice.isEmpty() ? null : choice.get(0);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> T sortedOneOrNone(final String message, final List<T> choices, Comparator<T> comparer) {
        if ((choices == null) || choices.isEmpty()) {
            return null;
        }
        final List<T> choice = SGuiChoose.sortedGetChoices(message, 0, 1, choices, comparer);
        return choice.isEmpty() ? null : choice.get(0);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> T sortedOne(final String message, final T[] choices, Comparator<T> comparer) {
        final List<T> choice = SGuiChoose.sortedGetChoices(message, 1, 1, choices, comparer);
        assert choice.size() == 1;
        return choice.get(0);
    } 

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> T sortedOne(final String message, final List<T> choices, Comparator<T> comparer) {
        if ((choices == null) || (choices.size() == 0)) {
            return null;
        }
        final List<T> choice = SGuiChoose.sortedGetChoices(message, 1, 1, choices, comparer);
        assert choice.size() == 1;
        return choice.get(0);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> List<T> sortedNoneOrMany(final String message, final List<T> choices, Comparator<T> comparer) {
        return SGuiChoose.sortedGetChoices(message, 0, choices.size(), choices, comparer);
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
