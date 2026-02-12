package forge.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.util.lang.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Static library containing language-related utility methods.
 */
public abstract class Lang {

    private static Lang instance;
    private static Lang englishInstance;

    protected String languageCode;
    protected String countryCode;

    public static void createInstance(String localeID) {
        String[] splitLocale = localeID.split("-");
        String language = splitLocale[0];
        String country = splitLocale[1];
        if (language.equals("de")) {
            instance = new LangGerman();
        } else if (language.equals("es")) {
            instance = new LangSpanish();
        } else if (language.equals("it")) {
            instance = new LangItalian();
        } else if (language.equals("zh")) {
            instance = new LangChinese();
        } else if (language.equals("ja")) {
            instance = new LangJapanese();
        } else if (language.equals("fr")) {
            instance = new LangFrench();
        } else { // default is English
            instance = new LangEnglish();
        }
        instance.languageCode = language;
        instance.countryCode = country;

        // Create english instance for internal usage
        englishInstance = new LangEnglish();
        englishInstance.languageCode = "en";
        englishInstance.countryCode = "US";
    }

    public static Lang getInstance() {
        return instance;
    }

    /**
     * Return a name that is unique among {@code existingNames}, applying an
     * ordinal prefix ("2nd", "3rd", ...) if necessary.
     */
    public static String deduplicateName(final String name, final Collection<String> existingNames) {
        String candidate = name;
        for (int i = 2; i <= 8; i++) {
            if (!existingNames.contains(candidate)) {
                return candidate;
            }
            candidate = getInstance().getOrdinal(i) + " " + name;
        }
        return candidate;
    }

    public static Lang getEnglishInstance() {
        return englishInstance;
    }

    protected Lang() {
    }

    /**
     * Return the ordinal suffix (2 characters) for the textual representation
     * of a numbers, eg. "st" for 1 ("first") and "th" for 4 ("fourth").
     * 
     * @param position
     *                 the number to get the ordinal suffix for.
     * @return a string containing two characters.
     */
    public abstract String getOrdinal(final int position);

    public static String joinHomogenous(final String s1, final String s2) {
        final boolean has1 = StringUtils.isNotBlank(s1);
        final boolean has2 = StringUtils.isNotBlank(s2);
        return has1 ? (has2 ? s1 + " and " + s2 : s1) : (has2 ? s2 : "");
    }

    public static <T> String joinHomogenous(final Iterable<T> objects) {
        return joinHomogenous(Lists.newArrayList(objects));
    }

    public static <T> String joinHomogenous(final Collection<T> objects) {
        return joinHomogenous(objects, null, "and");
    }

    public static <T> String joinHomogenous(final Collection<T> objects, final Function<T, String> accessor) {
        return joinHomogenous(objects, accessor, "and");
    }

    public static <T> String joinHomogenous(final Collection<T> objects, final Function<T, String> accessor,
            final String lastUnion) {
        int remaining = objects.size();
        final StringBuilder sb = new StringBuilder();
        for (final T obj : objects) {
            remaining--;
            if (accessor != null) {
                sb.append(accessor.apply(obj));
            } else {
                sb.append(obj);
            }
            if (remaining > 1) {
                sb.append(", ");
            } else if (remaining == 1) {
                sb.append(" ").append(lastUnion).append(" ");
            }
        }
        return sb.toString();
    }

    public static <T> String joinVerb(final List<T> subjects, final String verb) {
        return subjects.size() > 1 || !subjectIsSingle3rdPerson(Iterables.getFirst(subjects, "it").toString()) ? verb
                : verbs3rdPersonSingular(verb);
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
        return noun + (noun.endsWith("s") && !noun.endsWith("ds") || noun.endsWith("x") || noun.endsWith("ch") ? "es"
                : noun.endsWith("ds") ? "" : "s");
    }

    public static String nounWithAmount(final int cnt, final String noun) {
        final String countedForm = cnt == 1 ? noun : getPlural(noun);
        final String strCount;
        if (cnt == 1) {
            strCount = startsWithVowel(noun) ? "an " : "a ";
        } else {
            strCount = cnt + " ";
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

    public static String nounWithNumeralExceptOne(final int cnt, final String noun) {
        final String countedForm = cnt == 1 ? noun : getPlural(noun);
        final String desc = cnt == 1 ? (Lang.startsWithVowel(countedForm) ? "an" : "a") : getNumeral(cnt);
        return desc + " " + countedForm;
    }

    public static String nounWithNumeralExceptOne(final String cnt, final String noun) {
        if (StringUtils.isNumeric(cnt)) {
            return nounWithNumeralExceptOne(Integer.parseInt(cnt), noun);
        } else {
            // for X
            return cnt + " " + getPlural(noun);
        }
    }

    public abstract String getPossesive(final String name);

    public abstract String getPossessedObject(final String owner, final String object);

    public static boolean startsWithVowel(final String word) {
        return isVowel(word.trim().charAt(0));
    }

    private static final Pattern VOWEL_PATTERN = Pattern.compile("[aeiou]", Pattern.CASE_INSENSITIVE);

    public static boolean isVowel(final char letter) {
        return VOWEL_PATTERN.matcher(String.valueOf(letter)).find();
    }

    public final static String[] numbers0 = new String[] {
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
            "nineteen" };
    public final static String[] numbers20 = new String[] { "twenty", "thirty", "forty", "fifty", "sixty", "seventy",
            "eighty", "ninety" };

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

    public String getNickName(final String name) {
        if (name.contains(",")) {
            return name.split(",")[0];
        }
        if (name.contains(":")) {
            return name.split(":")[0];
        }
        return name.split(" ")[0];
    }

    public String buildValidDesc(Collection<String> valid, boolean multiple) {
        return joinHomogenous(valid.stream().map(s -> formatValidDesc(s)).collect(Collectors.toList()), null, multiple ? "and/or" : "or");
    }

    public String formatValidDesc(String valid) {
        List<String> commonStuff = List.of(
                //list of common one word non-core type ValidTgts that should be lowercase in the target prompt
                "Player", "Opponent", "Card", "Spell", "Permanent"
        );
        if (commonStuff.contains(valid) || CardType.isACardType(valid)) {
            valid = valid.toLowerCase();
        }
        return valid;
    }
}
