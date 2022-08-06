package forge.toolbox;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import forge.Forge;
import forge.game.card.CardView;
import forge.util.Callback;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
    public static <T> void oneOrNone(final String message, final T[] choices, final Callback<T> callback) {
        if ((choices == null) || (choices.length == 0)) {
            callback.run(null);
            return;
        }
        getChoices(message, 0, 1, choices, new Callback<List<T>>() {
            @Override
            public void run(final List<T> result) {
                callback.run(result.isEmpty() ? null : result.get(0));
            }
        });
    }

    public static <T> void oneOrNone(final String message, final Collection<T> choices, final Callback<T> callback) {
        if ((choices == null) || choices.isEmpty()) {
            callback.run(null);
            return;
        }
        getChoices(message, 0, 1, choices, new Callback<List<T>>() {
            @Override
            public void run(final List<T> result) {
                callback.run(result.isEmpty() ? null : result.get(0));
            }
        });
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
    public static <T> void one(final String message, final T[] choices, final Callback<T> callback) {
        if (choices == null || choices.length == 0) {
            callback.run(null);
            return;
        }
        if (choices.length == 1) {
            callback.run(choices[0]);
            return;
        }

        getChoices(message, 1, 1, choices, new Callback<List<T>>() {
            @Override
            public void run(final List<T> result) {
                assert result.size() == 1;
                callback.run(result.get(0));
            }
        });
    }

    public static <T> void one(final String message, final Collection<T> choices, final Callback<T> callback) {
        if (choices == null || choices.isEmpty()) {
            callback.run(null);
            return;
        }
        if (choices.size() == 1) {
            callback.run(Iterables.getFirst(choices, null));
            return;
        }

        getChoices(message, 1, 1, choices, new Callback<List<T>>() {
            @Override
            public void run(final List<T> result) {
                assert result.size() == 1;
                callback.run(result.get(0));
            }
        });
    }

    public static <T> void noneOrMany(final String message, final Collection<T> choices, final Callback<List<T>> callback) {
        getChoices(message, 0, choices.size(), choices, null, null, callback);
    }

    // Nothing to choose here. Code uses this to just reveal one or more items
    public static <T> void reveal(final String message, final T item) {
        List<T> items = new ArrayList<>();
        items.add(item);
        reveal(message, items);
    }
    public static <T> void reveal(final String message, final T[] items) {
        getChoices(message, -1, -1, items, null);
    }
    public static <T> void reveal(final String message, final Collection<T> items) {
        getChoices(message, -1, -1, items, null);
    }

    // Get Integer in range
    public static void getInteger(final String message, final Callback<Integer> callback) {
        getInteger(message, 0, Integer.MAX_VALUE, callback);
    }
    public static void getInteger(final String message, int min, final Callback<Integer> callback) {
        getInteger(message, min, Integer.MAX_VALUE, callback);
    }
    public static void getInteger(final String message, int min, int max, final Callback<Integer> callback) {
        if (max <= min) { //just return min if max <= min
            callback.run(min);
            return;
        }

        //force cutting off after 100 numbers at most
        if (max == Integer.MAX_VALUE) {
            getInteger(message, min, max, min + 99, callback);
            return;
        }
        int count = max - min + 1;
        if (count > 100) { 
            getInteger(message, min, max, min + 99, callback);
            return;
        }

        final Integer[] choices = new Integer[count];
        for (int i = 0; i < count; i++) {
            choices[i] = Integer.valueOf(i + min);
        }
        oneOrNone(message, choices, callback);
    }
    public static void getInteger(final String message, final int min, final int max, final int cutoff, final Callback<Integer> callback) {
        if (max <= min || cutoff < min) { //just return min if max <= min or cutoff < min
            callback.run(min);
            return;
        }

        if (cutoff >= max) { //fallback to regular integer prompt if cutoff at or after max
            getInteger(message, min, max, callback);
            return;
        }

        List<Object> choices = new ArrayList<>();
        for (int i = min; i <= cutoff; i++) {
            choices.add(Integer.valueOf(i));
        }
        choices.add(Forge.getLocalizer().getMessage("lblOther") + "...");

        oneOrNone(message, choices, new Callback<Object>() {
            @Override
            public void run(Object choice) {
                if (choice instanceof Integer || choice == null) {
                    callback.run((Integer)choice);
                    return;
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
                getNumberInput(prompt, message, min, max, callback);
            }
        });
    }
    
    private static void getNumberInput(final String prompt, final String message, final int min, final int max, final Callback<Integer> callback) {
        FOptionPane.showInputDialog(prompt, message, new Callback<String>() {
            @Override
            public void run(String result) {
                if (result == null) { //that is 'cancel'
                    callback.run(null);
                    return;
                }
                if (StringUtils.isNumeric(result)) {
                    Integer val = Integer.valueOf(result);
                    if (val >= min && val <= max) {
                        callback.run(val);
                        return;
                    }
                }

                //re-prompt if invalid input
                getNumberInput(prompt, message, min, max, callback);
            }
        });
    }

    // returned Object will never be null
    public static <T> void getChoices(final String message, final int min, final int max, final T[] choices, final Callback<List<T>> callback) {
        getChoices(message, min, max, Arrays.asList(choices), null, null, callback);
    }

    public static <T> void getChoices(final String message, final int min, final int max, final Collection<T> choices, final Callback<List<T>> callback) {
        getChoices(message, min, max, choices, null, null, callback);
    }

    public static <T> void getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display, final Callback<List<T>> callback) {
        if (choices == null || choices.isEmpty()) {
            if (min == 0) {
                callback.run(new ArrayList<>());
                return;
            }
            throw new RuntimeException("choice required from empty list");
        }

        ListChooser<T> c = new ListChooser<>(message, min, max, choices, display, callback);
        c.show(selected, false);
    }

    public static <T> void many(final String title, final String topCaption, int cnt, final List<T> sourceChoices, CardView referenceCard, final Callback<List<T>> callback) {
        order(title, topCaption, cnt, cnt, sourceChoices, null, referenceCard, callback);
    }

    public static <T> void many(final String title, final String topCaption, int min, int max, final List<T> sourceChoices, CardView referenceCard, final Callback<List<T>> callback) {
        int m1 = max >= 0 ? sourceChoices.size() - max : -1;
        int m2 = min >= 0 ? sourceChoices.size() - min : -1;
        order(title, topCaption, m1, m2, sourceChoices, null, referenceCard, callback);
    }

    public static <T> void order(final String title, final String top, final List<T> sourceChoices, CardView referenceCard, final Callback<List<T>> callback) {
        order(title, top, 0, 0, sourceChoices, null, referenceCard, callback);
    }

    public static <T> void order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final Callback<List<T>> callback) {
        // An input box for handling the order of choices.
        DualListBox<T> dual = new DualListBox<>(title, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, callback);
        dual.setSecondColumnLabelText(top);
        dual.show();
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> void sortedOneOrNone(final String message, final T[] choices, Comparator<T> comparer, final Callback<T> callback) {
        if ((choices == null) || (choices.length == 0)) {
            callback.run(null);
            return;
        }
        sortedGetChoices(message, 0, 1, choices, comparer, new Callback<List<T>>() {
            @Override
            public void run(List<T> result) {
                callback.run(result.isEmpty() ? null : result.get(0));
            }
        });
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> void sortedOneOrNone(final String message, final List<T> choices, Comparator<T> comparer, final Callback<T> callback) {
        if ((choices == null) || choices.isEmpty()) {
            callback.run(null);
            return;
        }
        sortedGetChoices(message, 0, 1, choices, comparer, new Callback<List<T>>() {
            @Override
            public void run(List<T> result) {
                callback.run(result.isEmpty() ? null : result.get(0));
            }
        });
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> void sortedOne(final String message, final T[] choices, Comparator<T> comparer, final Callback<T> callback) {
        if ((choices == null) || (choices.length == 0)) {
            callback.run(null);
            return;
        }
        sortedGetChoices(message, 1, 1, choices, comparer, new Callback<List<T>>() {
            @Override
            public void run(List<T> result) {
                assert result.size() == 1;
                callback.run(result.get(0));
            }
        });
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> void sortedOne(final String message, final List<T> choices, Comparator<T> comparer, final Callback<T> callback) {
        if ((choices == null) || (choices.size() == 0)) {
            callback.run(null);
            return;
        }
        sortedGetChoices(message, 1, 1, choices, comparer, new Callback<List<T>>() {
            @Override
            public void run(List<T> result) {
                assert result.size() == 1;
                callback.run(result.get(0));
            }
        });
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> void sortedNoneOrMany(final String message, final List<T> choices, Comparator<T> comparer, final Callback<List<T>> callback) {
        sortedGetChoices(message, 0, choices.size(), choices, comparer, callback);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> void sortedGetChoices(final String message, final int min, final int max, final T[] choices, Comparator<T> comparer, final Callback<List<T>> callback) {
        // You may create a copy of source array if callers expect the collection to be unchanged
        Arrays.sort(choices, comparer);
        getChoices(message, min, max, choices, callback);
    }

    // If comparer is NULL, T has to be comparable. Otherwise you'll get an exception from inside the Arrays.sort() routine
    public static <T> void sortedGetChoices(final String message, final int min, final int max, final List<T> choices, Comparator<T> comparer, final Callback<List<T>> callback) {
        // You may create a copy of source list if callers expect the collection to be unchanged
        Collections.sort(choices, comparer);
        getChoices(message, min, max, choices, callback);
    }
}

