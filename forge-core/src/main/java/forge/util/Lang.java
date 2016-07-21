package forge.util;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Static library containing language-related utility methods.
 */
public final class Lang {

    /**
     * Private constructor to prevent instantiation.
     */
    private Lang() {
    }

    /**
     * Return the ordinal suffix (2 characters) for the textual representation
     * of a numbers, eg. "st" for 1 ("first") and "th" for 4 ("fourth").
     * 
     * @param position
     *            the number to get the ordinal suffix for.
     * @return a string containing two characters.
     */
    public static String getOrdinal(final int position) {
        final String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (position % 100) {
        case 11:
        case 12:
        case 13:
            return position + "th";
        default:
            return position + sufixes[position % 10];
        }
    }

    public static String joinHomogenous(final String s1, final String s2) {
        final boolean has1 = StringUtils.isNotBlank(s1);
        final boolean has2 = StringUtils.isNotBlank(s2);
        return has1 ? (has2 ? s1 + " and " + s2 : s1) : (has2 ? s2 : "");
    }

    public static <T> String joinHomogenous(final Iterable<T> objects) { return joinHomogenous(Lists.newArrayList(objects)); }
    public static <T> String joinHomogenous(final Collection<T> objects) { return joinHomogenous(objects, null, "and"); }
    public static <T> String joinHomogenous(final Collection<T> objects, final Function<T, String> accessor) {
        return joinHomogenous(objects, accessor, "and");
    }
    public static <T> String joinHomogenous(final Collection<T> objects, final Function<T, String> accessor, final String lastUnion) {
        int remaining = objects.size();
        final StringBuilder sb = new StringBuilder();
        for (final T obj : objects) {
            remaining--;
            if (accessor != null) {
                sb.append(accessor.apply(obj));
            }
            else {
                sb.append(obj);
            }
            if (remaining > 1) {
                sb.append(", ");
            }
            else if (remaining == 1) {
                sb.append(" ").append(lastUnion).append(" ");
            }
        }
        return sb.toString();
    }

    public static <T> String joinVerb(final List<T> subjects, final String verb) {
        return subjects.size() > 1 || !subjectIsSingle3rdPerson(Iterables.getFirst(subjects, "it").toString()) ? verb : verbs3rdPersonSingular(verb);
    }

    public static String joinVerb(final String subject, final String verb) {
        return !Lang.subjectIsSingle3rdPerson(subject) ? verb : verbs3rdPersonSingular(verb);
    }

    public static boolean subjectIsSingle3rdPerson(final String subject) {
        // Will be most simple
        return !"You".equalsIgnoreCase(subject);
    }

    public static String verbs3rdPersonSingular(final String verb) {
        // English is simple - just add (s) for multiple objects.
        return verb + "s";
    }

    public static String getPlural(final String noun) {
        return noun + (noun.endsWith("s") || noun.endsWith("x") || noun.endsWith("ch") ? "es" : "s");
    }

    public static String nounWithAmount(final int cnt, final String noun) {
        final String countedForm = cnt == 1 ? noun : getPlural(noun);
        final String strCount;
        if (cnt == 1) {
            strCount = startsWithVowel(noun) ? "an " : "a ";
        }
        else {
            strCount = String.valueOf(cnt) + " ";
        }
        return strCount + countedForm;
    }

    public static String nounWithNumeral(final int cnt, final String noun) {
        final String countedForm = cnt == 1 ? noun : getPlural(noun);
        return getNumeral(cnt) + " " + countedForm;
    }

    public static String nounWithNumeral(final String cnt, final String noun) {
        if (StringUtils.isNumeric(cnt)) {
            return nounWithNumeral(Integer.parseInt(cnt), noun);
        } else {
            // for X
            return cnt + " " + getPlural(noun);
        }
    }

    public static String getPossesive(final String name) {
        if ("You".equalsIgnoreCase(name)) {
            return name + "r"; // to get "your"
        }
        return name.endsWith("s") ? name + "'" : name + "'s";
    }

    public static String getPossessedObject(final String owner, final String object) {
        return getPossesive(owner) + " " + object;
    }

    public static boolean startsWithVowel(final String word) {
        return isVowel(word.trim().charAt(0));
    }

    private static final Pattern VOWEL_PATTERN = Pattern.compile("[aeiou]", Pattern.CASE_INSENSITIVE);
    public static boolean isVowel(final char letter) {
        return VOWEL_PATTERN.matcher(String.valueOf(letter)).find();
    }

    public final static String[] numbers0 = new String[] {
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eightteen", "nineteen" };
    public final static String[] numbers20 = new String[] {"twenty", "thirty", "fourty", "fifty", "sixty", "seventy", "eighty", "ninety" };

    public static String getNumeral(int n) {
        final String prefix = n < 0 ? "minus " : "";
        n = Math.abs(n);
        if (n >= 0 && n < 20) {
            return prefix + numbers0[n];
        }
        if (n < 100) {
            final int n1 = n % 10;
            final String ones = n1 == 0 ? "" : numbers0[n1];
            return prefix + numbers20[(n / 10) - 2] + " " + ones;
        }
        return Integer.toString(n);
    }
}
