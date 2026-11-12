package forge.ai;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every spell, ability, triggered ability and charm mode of every unique card, as the stack would show it, checked with
 * the same lint as {@link StackDescriptionTest} (#5634) plus a set of grammar heuristics. It takes a few minutes, so it
 * only runs on request: {@code mvn test -Dtest=StackDescriptionSweepTest -Dforge.sweep=true}.
 * <p>
 * Output (nothing fails: the list is the work to do):
 * <ul>
 * <li>{@code target/stack-description-sweep.txt}: the findings grouped by problem ("grammar: repeated word", "raw
 * CARDNAME/NICKNAME", "threw NullPointerException", ...), one line each: card, kind of ability, the text.
 * A line with several problems is listed under each of them.</li>
 * <li>{@code target/stack-description-sweep-skipped.txt}: cards the test game could not build, so were not checked.</li>
 * <li>the log: a count per problem and the same lines, prefixed {@code SWEEP}.</li>
 * </ul>
 * The grammar checks are heuristics: a hit is a place to look, not always a bug.
 */
public class StackDescriptionSweepTest extends AITest {
    private static final int BATCH = 400;

    /** Pattern and name of what to look for on top of {@link StackDescriptionTest#lint}. */
    private static final String[][] GRAMMAR = {
            { "\\b([A-Za-z]+) \\1\\b", "grammar: repeated word" },
            { "\\b(?:a|an|the) (?:a|an|the)\\b", "grammar: doubled article" },
            { "\\ba (?!one\\b|once\\b|unicorn|us[aeiou]|uni|euro|ubiq|ut[ie])[aeiouAEIOU]\\w*", "grammar: 'a' before a vowel" },
            { "\\ban (?!hour|honou?r|heir|honest|[xX]\\b|MVP)[b-df-hj-np-tv-zB-DF-HJ-NP-TV-Z]\\w*", "grammar: 'an' before a consonant" },
            { "\\b(?:[Ii]t|[Tt]hat \\w+|[Ee]ach \\w+|[Tt]arget \\w+) "
                    + "(?:gain|draw|lose|put|exile|reveal|mill|sacrifice|discard|search|shuffle|create|deal|destroy|return)\\b",
                    "grammar: singular subject, base verb" },
            { "\\b(?:[Tt]hey|[Yy]ou) "
                    + "(?:gains|draws|loses|puts|exiles|reveals|mills|sacrifices|discards|searches|shuffles|creates|deals|destroys|returns)\\b",
                    "grammar: plural subject, -s verb" },
            { "(?<![/+\\-\\d])\\b1 (?!less\\b|this\\b)[a-z]+s\\b", "grammar: 1 with a plural noun" },
            { "(?<![/+\\-\\d])\\b(?:[2-9]|\\d{2,}) (?:card|creature|land|permanent|counter|token|spell|ability|player|opponent)\\b",
                    "grammar: number with a singular noun" },
            { "[a-z][.!?][A-Z][a-z]", "grammar: no space after full stop" },
            { "[.,;:]{2,}|,\\.|\\.,", "grammar: doubled punctuation" },
            { "^[a-z]", "grammar: starts lowercase" },
            { "\\(OPTIONAL\\)", "grammar: raw (OPTIONAL) marker" },
    };
    private static final Pattern[] GRAMMAR_RE = Arrays.stream(GRAMMAR).map(g -> Pattern.compile(g[0])).toArray(Pattern[]::new);
    private static final Pattern TAG = Pattern.compile("\\[([^\\]]+)\\]");

    private int abilities;
    private int triggers;
    private int modes;

    @Test
    public void sweep() throws Exception {
        if (!Boolean.getBoolean("forge.sweep")) {
            System.out.println("StackDescriptionSweepTest skipped (-Dforge.sweep=true to run it)");
            return;
        }
        abilities = 0;
        triggers = 0;
        modes = 0;
        final List<String> names = new ArrayList<>();
        for (final PaperCard pc : StaticData.instance().getCommonCards().getUniqueCards()) {
            names.add(pc.getName());
        }
        final List<String> findings = new ArrayList<>();
        final List<String> skipped = new ArrayList<>();
        final Map<String, List<String>> byProblem = new TreeMap<>();
        int built = 0;
        Game game = null;
        Player ai = null;
        Player opp = null;
        for (int i = 0; i < names.size(); i++) {
            if (i % BATCH == 0) {
                game = initAndCreateGame();
                opp = game.getPlayers().get(0);
                ai = game.getPlayers().get(1);
                // things for the spells to point at, on both sides ("target creature you control")
                addCardToZone("Grizzly Bears", opp, ZoneType.Battlefield);
                addCardToZone("Darksteel Relic", opp, ZoneType.Battlefield);
                addCardToZone("Reanimate", opp, ZoneType.Graveyard);
                addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
                addCardToZone("Reanimate", ai, ZoneType.Graveyard);
            }
            final Card card;
            try {
                card = addCardToZone(names.get(i), ai, ZoneType.Hand);
            } catch (final RuntimeException e) {
                // a card the test game cannot build (conspiracy, vanguard, ...): listed, not silently dropped
                skipped.add(names.get(i) + " | " + e);
                continue;
            }
            built++;
            final String cardName = card.getName();

            // spells and abilities, and the modes of the charms among them
            for (final SpellAbility sa : card.getAllSpellAbilities()) {
                abilities++;
                record(check(cardName, "ability", sa, ai, opp), findings, byProblem);
                if (ApiType.Charm.equals(sa.getApi()) && sa.hasParam("Choices")) {
                    for (final String mode : sa.getParam("Choices").split(",")) {
                        if (!card.hasSVar(mode)) {
                            continue;
                        }
                        try {
                            final SpellAbility modeSa = AbilityFactory.getAbility(card, mode);
                            modes++;
                            record(check(cardName, "charm mode " + mode, modeSa, ai, opp), findings, byProblem);
                        } catch (final RuntimeException e) {
                            record(threw(cardName, "charm mode " + mode, e), findings, byProblem);
                        }
                    }
                }
            }

            // triggered abilities: the effect text of what the trigger executes
            for (final Trigger t : card.getTriggers()) {
                try {
                    final SpellAbility sa = t.ensureAbility();
                    if (sa == null) {
                        continue;
                    }
                    triggers++;
                    record(check(cardName, "trigger", sa, ai, opp), findings, byProblem);
                } catch (final RuntimeException e) {
                    record(threw(cardName, "trigger", e), findings, byProblem);
                }
            }
        }

        System.out.println("StackDescriptionSweepTest: " + built + " of " + names.size() + " unique cards built, " + skipped.size()
                + " skipped; " + abilities + " abilities, " + triggers + " triggers, " + modes + " charm modes; "
                + findings.size() + " with findings");
        for (final Map.Entry<String, List<String>> e : byProblem.entrySet()) {
            System.out.println("SWEEP-COUNT  " + e.getValue().size() + "  " + e.getKey());
        }
        for (final String f : findings) {
            System.out.println("SWEEP  " + f);
        }
        try (PrintWriter w = new PrintWriter("target/stack-description-sweep.txt", "UTF-8")) {
            for (final Map.Entry<String, List<String>> e : byProblem.entrySet()) {
                w.println("== " + e.getKey() + " (" + e.getValue().size() + ")");
                e.getValue().forEach(w::println);
                w.println();
            }
        }
        try (PrintWriter w = new PrintWriter("target/stack-description-sweep-skipped.txt", "UTF-8")) {
            skipped.forEach(w::println);
        }
    }

    /** Aims the ability at something the test game has, the opponent first. */
    private static void pickTargets(final SpellAbility sa, final Player ai, final Player opp) {
        if (!sa.usesTargeting()) {
            return;
        }
        if (sa.canTarget(opp)) {
            sa.getTargets().add(opp);
            return;
        }
        for (final Player p : Arrays.asList(opp, ai)) {
            for (final Card c : p.getCardsIn(Arrays.asList(ZoneType.Battlefield, ZoneType.Graveyard))) {
                if (sa.canTarget(c)) {
                    sa.getTargets().add(c);
                    return;
                }
            }
        }
        if (sa.canTarget(ai)) {
            sa.getTargets().add(ai);
        }
    }

    /** The stack text of one ability, linted. Null when it reads fine, else {line for the report, the problems}. */
    private String[] check(final String cardName, final String kind, final SpellAbility sa, final Player ai, final Player opp) {
        String text;
        String problems;
        try {
            sa.setActivatingPlayer(ai);
            pickTargets(sa, ai, opp);
            text = sa.getStackDescription();
            problems = text == null ? " [null description]" : StackDescriptionTest.lint(text) + grammarLint(text);
        } catch (final RuntimeException e) {
            return threw(cardName, kind, e);
        }
        return problems.isEmpty() ? null
                : new String[] { cardName + " | " + kind + " " + sa.getApi() + " | \"" + text + "\"" + problems, problems };
    }

    private static String[] threw(final String cardName, final String kind, final RuntimeException e) {
        return new String[] { cardName + " | " + kind + " | threw " + e, " [threw " + e.getClass().getSimpleName() + "]" };
    }

    /** Grammar a reader would notice, as " [problem]" per hit. Heuristics: a hit is a place to look, not always a bug. */
    static String grammarLint(final String text) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < GRAMMAR_RE.length; i++) {
            if (GRAMMAR_RE[i].matcher(text).find()) {
                sb.append(" [").append(GRAMMAR[i][1]).append("]");
            }
        }
        if (text.chars().filter(c -> c == '(').count() != text.chars().filter(c -> c == ')').count()) {
            sb.append(" [grammar: unbalanced parentheses]");
        }
        if (text.chars().filter(c -> c == '"').count() % 2 != 0) {
            sb.append(" [grammar: unbalanced quotes]");
        }
        final String trimmed = text.trim();
        if (!trimmed.isEmpty() && ".!?\")'".indexOf(trimmed.charAt(trimmed.length() - 1)) < 0) {
            sb.append(" [grammar: no final punctuation]");
        }
        return sb.toString();
    }

    private static void record(final String[] finding, final List<String> findings, final Map<String, List<String>> byProblem) {
        if (finding == null) {
            return;
        }
        findings.add(finding[0]);
        final Matcher m = TAG.matcher(finding[1]);
        while (m.find()) {
            byProblem.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(finding[0]);
        }
    }
}
