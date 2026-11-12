package forge.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.CardType;
import forge.util.lang.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Static library containing language-related utility methods.
 */
public abstract class Lang {

    private static Map<String, Lang> languages = Collections.synchronizedMap(Maps.newHashMap());

    private static Lang instance;
    private static Lang englishInstance;

    public static void createInstance(String localeID) {
        instance = initInstance(localeID);

        // Create english instance for internal usage
        englishInstance = initInstance("en-US");
    }

    public static Lang initInstance(String localeID) {
        return languages.computeIfAbsent(localeID, k -> switch (k.split("-")[0]) {
            case "de" -> new LangGerman();
            case "es" -> new LangSpanish();
            case "it" -> new LangItalian();
            case "zh" -> new LangChinese();
            case "ja" -> new LangJapanese();
            case "ko" -> new LangKorean();
            case "fr" -> new LangFrench();
            case "ru" -> new LangRussian();
            default -> new LangEnglish();
        });
    }

    public static Lang getInstance() {
        return instance;
    }

    public static Lang getEnglishInstance() {
        return englishInstance;
    }

    protected Lang() {
    }

    public String getFontFile() {
        return null;
    }

    public char canDisplayCheck() {
        return ' ';
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
        return buildValidDesc(valid, multiple ? "and/or" : "or");
    }
    public String buildValidDesc(Collection<String> valid, String join) {
        return joinHomogenous(valid.stream().map(s -> formatValidDesc(s)).collect(Collectors.toList()), null, join);
    }

    /** Like {@link #buildValidDesc(Collection, boolean)}, with the card types described as cards ("land card you own"). */
    public String buildValidCardDesc(Collection<String> valid, boolean multiple) {
        return joinHomogenous(valid.stream().map(s -> formatValidDesc(s, false, true)).collect(Collectors.toList()), null, multiple ? "and/or" : "or");
    }

    public String formatValidDesc(String valid) {
        return formatValidDesc(valid, false, false);
    }

    /** A comma separated Valid list ("Creature,Planeswalker") as words: "creature or planeswalker". */
    public String describeValidList(final String valid) {
        return joinHomogenous(java.util.Arrays.asList(valid.split(",")), v -> formatValidDesc(v), "or");
    }

    private static final Pattern VALID_SCRIPT = Pattern.compile("[A-Za-z]+([.+][A-Za-z0-9]+)*");
    private static final Pattern VALID_CMC_LIKE = Pattern.compile("(cmc|power|toughness)(LE|GE|EQ|LT|GT|NE)(\\w+)");
    private static final List<String> VALID_ADJECTIVES = List.of("Basic", "Legendary", "Snow", "White", "Blue", "Black", "Red", "Green",
            "Colorless", "Multicolor", "Monocolored", "Historic");

    /**
     * Turns a script "Valid" string ("Permanent.nonColorless", "Land.Basic", "Creature.Other+YouCtrl") into words
     * ("colored permanent", "basic land", "other creature you control"). A string that is not a plain script string
     * (several restrictions, variables, ...) comes back as it was.
     *
     * @param plural   the noun is made plural ("colored permanents")
     * @param asCards  a card type as the noun gets "card" after it ("land card you own")
     */
    public String formatValidDesc(String valid, final boolean plural, final boolean asCards) {
        final List<String> commonStuff = List.of(
                //list of common one word non-core type ValidTgts that should be lowercase in the target prompt
                "Player", "Opponent", "Card", "Spell", "Permanent"
        );
        if (!valid.contains(".") && !valid.contains("+")) {
            if (commonStuff.contains(valid) || CardType.isACardType(valid)) {
                valid = valid.toLowerCase();
            }
            return valid;
        }
        if (!VALID_SCRIPT.matcher(valid).matches()) {
            return valid;
        }
        final String[] parts = valid.split("[.+]");
        String noun = commonStuff.contains(parts[0]) || CardType.isACardType(parts[0]) ? parts[0].toLowerCase() : parts[0];
        final List<String> adjectives = Lists.newArrayList();
        final List<String> suffixes = Lists.newArrayList();
        for (int i = 1; i < parts.length; i++) {
            final String r = parts[i];
            final java.util.regex.Matcher m = VALID_CMC_LIKE.matcher(r);
            if (r.equals("nonColorless")) {
                adjectives.add("colored");
            } else if (r.equals("nonColored")) {
                adjectives.add("colorless");
            } else if (r.length() > 3 && r.startsWith("non") && Character.isUpperCase(r.charAt(3))) {
                adjectives.add("non" + r.substring(3).toLowerCase());
            } else if (r.equals("Other")) {
                adjectives.add("other");
            } else if (VALID_ADJECTIVES.contains(r)) {
                adjectives.add(r.toLowerCase());
            } else if (CardType.isACardType(r) && (noun.equals("card") || noun.equals("permanent"))) {
                noun = r.toLowerCase();
            } else if (CardType.isACardType(r)) {
                adjectives.add(r.toLowerCase());
            } else if (Character.isUpperCase(r.charAt(0)) && !r.endsWith("Ctrl") && !r.endsWith("Own") && !r.equals("Self")
                    && (CardType.isASubType(r) || CardType.isASupertype(r))) {
                adjectives.add(r);
            } else if (r.equals("YouCtrl")) {
                suffixes.add("you control");
            } else if (r.equals("OppCtrl")) {
                suffixes.add("an opponent controls");
            } else if (r.equals("YouOwn")) {
                suffixes.add("you own");
            } else if (r.equals("OppOwn")) {
                suffixes.add("an opponent owns");
            } else if (r.equals("tapped") || r.equals("untapped") || r.equals("attacking") || r.equals("blocking") || r.equals("token")
                    || r.equals("nontoken")) {
                adjectives.add(r);
            } else if (m.matches() && (m.group(3).matches("\\d+") || m.group(3).equals("X"))) {
                final String what = m.group(1).equals("cmc") ? "mana value" : m.group(1);
                final String how;
                switch (m.group(2)) {
                    case "LE": how = " or less"; break;
                    case "GE": how = " or greater"; break;
                    case "LT": how = " less than "; break;
                    case "GT": how = " greater than "; break;
                    default: how = "";
                }
                suffixes.add(m.group(2).equals("NE") ? "without " + what + " " + m.group(3)
                        : how.endsWith(" ") ? "with " + what + how + m.group(3) : "with " + what + " " + m.group(3) + how);
            }
            // anything else is a condition the words cannot carry: left out
        }
        if (asCards && CardType.isACardType(StringUtils.capitalize(noun)) && !noun.equals("card") && !noun.equals("permanent") && !noun.equals("spell")) {
            noun = noun + " card";
        }
        if (plural) {
            noun = getPlural(noun);
        }
        final List<String> words = Lists.newArrayList(adjectives);
        words.add(noun);
        words.addAll(suffixes);
        return String.join(" ", words);
    }
}
