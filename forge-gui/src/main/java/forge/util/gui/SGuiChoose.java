package forge.util.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import forge.GuiBase;

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

    // Nothing to choose here. Code uses this to just reveal one or more items
    public static <T> void reveal(final String message, final Collection<T> items) {
        SGuiChoose.getChoices(message, -1, -1, items);
    }

    // Get Integer in range
    public static Integer getInteger(final String message, final int min, final int max) {
        return getInteger(message, min, max, false);
    }

    public static Integer getInteger(final String message, final int min, final int max, final boolean sortDesc) {
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

    public static Integer getInteger(final String message, final int min, final int max, final int cutoff) {
        if (max <= min || cutoff < min) { return min; } //just return min if max <= min or cutoff < min

        if (cutoff >= max) { //fallback to regular integer prompt if cutoff at or after max
            return getInteger(message, min, max);
        }

        final List<Object> choices = new ArrayList<Object>();
        for (int i = min; i <= cutoff; i++) {
            choices.add(Integer.valueOf(i));
        }
        choices.add("...");

        final Object choice = SGuiChoose.oneOrNone(message, choices);
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
            final String str = SOptionPane.showInputDialog(prompt, message);
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
        return GuiBase.getInterface().getChoices(message, min, max, choices, selected, display);
    }

    public static <T> List<T> many(final String title, final String topCaption, final int cnt, final List<T> sourceChoices) {
        return many(title, topCaption, cnt, cnt, sourceChoices);
    }

    public static <T> List<T> many(final String title, final String topCaption, final int min, final int max, final List<T> sourceChoices) {
        final int m2 = min >= 0 ? sourceChoices.size() - min : -1;
        final int m1 = max >= 0 ? sourceChoices.size() - max : -1;
        return order(title, topCaption, m1, m2, sourceChoices, null);
    }

    public static <T> List<T> order(final String title, final String top, final List<T> sourceChoices) {
        return order(title, top, 0, 0, sourceChoices, null);
    }

    private static <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices) {
        return GuiBase.getInterface().order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices);
    }

}
