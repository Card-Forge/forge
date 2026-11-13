package forge.ai;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every spell, ability, triggered ability and charm mode of every unique card, as the stack would show it, checked with
 * the same lint as {@link StackDescriptionTest} (#5634) plus a set of grammar heuristics. It takes a few minutes, so it
 * only runs on request: {@code mvn test -Dtest=StackDescriptionSweepTest -Dforge.sweep=true}.
 * <p>
 * Output (nothing fails: the todo list is the work to do), all in {@code target/}:
 * <ul>
 * <li>{@code stack-description-todo_grammar.txt}: only the texts that need updating, grouped by problem (biggest group
 * first), one line each: card, kind of ability, the text. A line with several problems is listed under each of them.</li>
 * <li>{@code stack-description-ok_grammar.txt}: the texts that read fine.</li>
 * <li>{@code stack-description-errors.txt}: triggers that could not be checked (they need a triggering object, a combat,
 * ... that the test game does not have); not a finding.</li>
 * <li>{@code stack-description-skipped.txt}: cards the test game could not build, so were not checked.</li>
 * <li>the log: a count per problem and the todo lines, prefixed {@code SWEEP}.</li>
 * </ul>
 * The checks run on the text with the card's own name taken out ("Welcome to...", "H.E.R.B.I.E."), so a name never
 * counts as a problem. The grammar checks are heuristics: a hit is a place to look, not always a bug.
 */
public class StackDescriptionSweepTest extends AITest {
    private static final int BATCH = 400;

    /** Pattern and name of what to look for on top of {@link StackDescriptionTest#lint}. Run on the text without the card's name. */
    private static final String[][] GRAMMAR = {
            { "\\b([A-Za-z]{2,}) \\1\\b", "grammar: repeated word" },
            { "\\b(?:a|an|the) (?:a|an|the)\\b", "grammar: doubled article" },
            { "\\ba (?!one\\b|once\\b|unicorn|us[aeiou]|uni|euro|ubiq|ut[ie])[aeiouAEIOU]\\w*", "grammar: 'a' before a vowel" },
            { "\\ban (?!hour|honou?r|heir|honest|[xX]\\b|MVP)[b-df-hj-np-tv-zB-DF-HJ-NP-TV-Z]\\w*", "grammar: 'an' before a consonant" },
            // "have it gain" is fine, "it gain" is not
            { "(?<!\\bhave )(?<!\\bhaving )(?<!\\bmake )(?<!\\bmakes )(?<!\\blet )(?<!\\blets )\\b[Ii]t "
                    + "(?:gain|draw|lose|put|exile|reveal|mill|sacrifice|discard|search|shuffle|create|deal|destroy|return)\\b",
                    "grammar: 'it' with a base verb" },
            { "\\b(?:[Tt]hey|[Yy]ou) "
                    + "(?:gains|draws|loses|puts|exiles|reveals|mills|sacrifices|discards|searches|shuffles|creates|deals|destroys|returns)\\b",
                    "grammar: plural subject, -s verb" },
            { "(?:^|[.!?] |- )\\s*(?:Sacrifices|Destroys|Exiles|Draws|Discards|Returns|Gains|Loses|Mills|Reveals|Searches)\\b",
                    "grammar: -s verb with no subject" },
            { "(?<![/+\\-\\d])\\b1 (?:cards|creatures|lands|permanents|counters|tokens|spells|abilities|players|opponents)\\b",
                    "grammar: 1 with a plural noun" },
            { "(?<![/+\\-\\d])\\b(?:[2-9]|\\d{2,}) (?:card|creature|land|permanent|counter|token|spell|ability|player|opponent)\\b"
                    + "(?!\\s+(?:cards?|tokens?|counters?)\\b)", "grammar: number with a singular noun" },
            { "\\bmana value \\d+ cards?\\b", "grammar: 'mana value N' before 'card'" },
            { "\\w\\(s\\)", "grammar: raw (s) plural marker" },
            { "\\b(?:cards|lands|creatures|permanents|tokens|counters|spells)s\\b", "grammar: double plural" },
            // a Valid string ("Creature.YouCtrl.") is the raw-Valid lint, not this
            { "(?<=[a-z]{2})[.!?](?=[A-Z][a-z]+ [a-z])", "grammar: no space after full stop" },
            { "[.,;:]{2,}|,\\.|\\.,", "grammar: doubled punctuation" },
            // "p1"/"p2" are the test game's player names
            { "(?:^|[.!?] |- )\\s*(?!p\\d\\b)[a-z]", "grammar: sentence starts lowercase (no subject?)" },
            { "\\(OPTIONAL\\)", "grammar: raw (OPTIONAL) marker" },
            { "(?:^|\\s)'s\\b", "grammar: possessive with no owner" },
            { "(?:^|[.!?] |- )\\s*p\\d\\s*$", "grammar: cut off after a player name" },
            { "\\b(?:Attach to|to|from|on|onto|into|of|and|or|the|a|an)\\s*$", "grammar: cut off mid-phrase" },
            { "\\bthe rest (?:library|exile|graveyard|hand|battlefield)\\b", "grammar: preposition missing after 'the rest'" },
            { "\\bABILITY\\b", "grammar: raw ABILITY placeholder" },
            { "\\[failed to", "grammar: failure marker in the text" },
    };
    private static final Pattern[] GRAMMAR_RE = Arrays.stream(GRAMMAR).map(g -> Pattern.compile(g[0])).toArray(Pattern[]::new);
    private static final Pattern TAG = Pattern.compile("\\[([^\\]]+)\\]");
    /** Nothing after the card name and its id: "Dark Depths (115) -". */
    private static final Pattern EMPTY = Pattern.compile("\\s*(?:~(?:\\s*\\(\\d+\\))?\\s*-)?\\s*");

    /** The api of a charm: its own text is empty, the modes carry the words. */
    private static final String CHARM = "Charm";

    private int abilities;
    private int triggers;
    private int modes;

    /** The outcome for one ability: the report line, what is wrong with it, and whether it could not be checked at all. */
    private static final class Result {
        final String line;
        final String problems;
        final boolean error;

        Result(final String line, final String problems, final boolean error) {
            this.line = line;
            this.problems = problems;
            this.error = error;
        }
    }

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
        final Set<String> ok = new LinkedHashSet<>();
        final Set<String> todo = new LinkedHashSet<>();
        final Set<String> errors = new LinkedHashSet<>();
        final List<String> skipped = new ArrayList<>();
        final Map<String, Set<String>> byProblem = new HashMap<>();
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
                addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);
                addCardToZone("Island", opp, ZoneType.Battlefield);
                addCardToZone("Pacifism", opp, ZoneType.Battlefield);
                addCardToZone("Jace Beleren", opp, ZoneType.Battlefield);
                addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
                addCardToZone("Island", ai, ZoneType.Battlefield);
                addCardToZone("Darksteel Relic", ai, ZoneType.Battlefield);
                addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
                addCardToZone("Reanimate", ai, ZoneType.Graveyard);
                addTargetFixtures(game, ai, opp);
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
                record(check(cardName, "ability", sa, ai, opp, false), ok, todo, errors, byProblem);
                if (ApiType.Charm.equals(sa.getApi()) && sa.hasParam("Choices")) {
                    for (final String mode : sa.getParam("Choices").split(",")) {
                        if (!card.hasSVar(mode)) {
                            continue;
                        }
                        try {
                            final SpellAbility modeSa = AbilityFactory.getAbility(card, mode);
                            modes++;
                            record(check(cardName, "charm mode " + mode, modeSa, ai, opp, false), ok, todo, errors, byProblem);
                        } catch (final RuntimeException e) {
                            record(threw(cardName, "charm mode " + mode, e, false), ok, todo, errors, byProblem);
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
                    // what the stack shows for a trigger is the trigger's own text; the effect text is only read for the
                    // "ABILITY" placeholder, so an effect that says nothing is fine there
                    record(checkTrigger(cardName, t, sa, ai), ok, todo, errors, byProblem);
                    if (t.hasParam("TriggerDescription") && t.getParam("TriggerDescription").contains("ABILITY")) {
                        record(check(cardName, "trigger effect", sa, ai, opp, true), ok, todo, errors, byProblem);
                    }
                } catch (final RuntimeException e) {
                    record(threw(cardName, "trigger", e, true), ok, todo, errors, byProblem);
                }
            }
        }

        System.out.println("StackDescriptionSweepTest: " + built + " of " + names.size() + " unique cards built, " + skipped.size()
                + " skipped; " + abilities + " abilities, " + triggers + " triggers, " + modes + " charm modes; "
                + ok.size() + " ok, " + todo.size() + " todo, " + errors.size() + " could not be checked");
        final List<Map.Entry<String, Set<String>>> groups = new ArrayList<>(byProblem.entrySet());
        groups.sort((x, y) -> y.getValue().size() != x.getValue().size()
                ? y.getValue().size() - x.getValue().size() : x.getKey().compareTo(y.getKey()));
        for (final Map.Entry<String, Set<String>> e : groups) {
            System.out.println("SWEEP-COUNT  " + e.getValue().size() + "  " + e.getKey());
        }
        for (final String f : todo) {
            System.out.println("SWEEP  " + f);
        }
        try (PrintWriter w = new PrintWriter("target/stack-description-todo_grammar.txt", "UTF-8")) {
            for (final Map.Entry<String, Set<String>> e : groups) {
                w.println("== " + e.getKey() + " (" + e.getValue().size() + ")");
                e.getValue().forEach(w::println);
                w.println();
            }
        }
        try (PrintWriter w = new PrintWriter("target/stack-description-ok_grammar.txt", "UTF-8")) {
            ok.forEach(w::println);
        }
        try (PrintWriter w = new PrintWriter("target/stack-description-errors.txt", "UTF-8")) {
            errors.forEach(w::println);
        }
        try (PrintWriter w = new PrintWriter("target/stack-description-skipped.txt", "UTF-8")) {
            skipped.forEach(w::println);
        }
    }

    /**
     * More of what abilities ask to aim at: a flier, a black and a legendary creature, a wall, an artifact creature, an
     * equipment, a tapped creature, a creature with a counter, a nonbasic and a snow land, cards in hand, library and exile,
     * a combat with a blocked attacker, and spells on the stack.
     */
    private void addTargetFixtures(final Game game, final Player ai, final Player opp) {
        try {
            final Card angel = addCardToZone("Serra Angel", opp, ZoneType.Battlefield);
            final Card knight = addCardToZone("Black Knight", opp, ZoneType.Battlefield);
            knight.setTapped(true);
            addCardToZone("Wall of Wood", opp, ZoneType.Battlefield);
            addCardToZone("Ornithopter", opp, ZoneType.Battlefield);
            addCardToZone("Isamaru, Hound of Konda", opp, ZoneType.Battlefield);
            addCardToZone("Bonesplitter", opp, ZoneType.Battlefield);
            addCardToZone("Command Tower", opp, ZoneType.Battlefield);
            addCardToZone("Snow-Covered Forest", opp, ZoneType.Battlefield);
            addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
            addCardToZone("Grizzly Bears", opp, ZoneType.Library);
            addCardToZone("Grizzly Bears", opp, ZoneType.Exile);
            addCardToZone("Serra Angel", ai, ZoneType.Battlefield);
            addCardToZone("Bonesplitter", ai, ZoneType.Battlefield);
            addCardToZone("Glorious Anthem", ai, ZoneType.Battlefield);
            addCardToZone("Command Tower", ai, ZoneType.Battlefield);
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
            addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
            final Card counters = addCardToZone("Isamaru, Hound of Konda", ai, ZoneType.Battlefield);
            counters.addCounterInternal(forge.game.card.CounterType.getType("P1P1"), 1, ai, false, null, null);
            // a combat: the opponent's creature attacks and is blocked
            final Card attacker = ai.getCardsIn(ZoneType.Battlefield).get(0);
            final Combat combat = new Combat(ai);
            combat.addAttacker(attacker, opp);
            combat.addBlocker(attacker, angel);
            game.getPhaseHandler().setCombat(combat);
        } catch (final RuntimeException e) {
            System.out.println("StackDescriptionSweepTest: some target fixtures could not be set up: " + e);
        }
        for (final String spellName : Arrays.asList("Grizzly Bears", "Lightning Bolt")) {
            try {
                final Card spellCard = addCardToZone(spellName, opp, ZoneType.Hand);
                final SpellAbility spell = spellCard.getFirstSpellAbility();
                spell.setActivatingPlayer(opp);
                if (spell.usesTargeting()) {
                    spell.getTargets().add(ai);
                }
                game.getAction().moveToStack(spellCard, spell);
                game.getStack().add(spell);
            } catch (final RuntimeException e) {
                System.out.println("StackDescriptionSweepTest: no spell on the stack for " + spellName + ": " + e);
            }
        }
    }

    /** Aims the ability at something the test game has, the opponent first. */
    private static boolean pickTargets(final SpellAbility sa, final Player ai, final Player opp) {
        if (!sa.usesTargeting()) {
            return true;
        }
        // up to two targets: "exchange control of two target creatures", "fights", "move a counter from ... onto ..."
        final java.util.List<forge.game.GameObject> candidates = new java.util.ArrayList<>();
        candidates.add(opp);
        for (final Player p : Arrays.asList(opp, ai)) {
            candidates.addAll(p.getCardsIn(Arrays.asList(ZoneType.Battlefield, ZoneType.Graveyard)));
        }
        candidates.add(ai);
        for (final Player p : Arrays.asList(opp, ai)) {
            candidates.addAll(p.getCardsIn(Arrays.asList(ZoneType.Hand, ZoneType.Library, ZoneType.Exile)));
        }
        for (final forge.game.spellability.SpellAbilityStackInstance si : ai.getGame().getStack()) {
            candidates.add(si.getSpellAbility());
        }
        for (final forge.game.GameObject o : candidates) {
            if (sa.getTargets().size() >= 2) {
                break;
            }
            if (!sa.getTargets().contains(o) && sa.canTarget(o)) {
                sa.getTargets().add(o);
            }
        }
        return !sa.getTargets().isEmpty();
    }

    private static final java.util.Set<String> TRIG_CARD = new java.util.HashSet<>(Arrays.asList("Card", "CardLKI", "NewCard", "Attacker",
            "Blocker", "Fighter", "Token", "Source", "DamageSource", "Devoured", "Exploited", "Explorer", "Crew", "Championed", "Enlisted", "AttachSource", "AttachTarget"));
    private static final java.util.Set<String> TRIG_CARDS = new java.util.HashSet<>(Arrays.asList("Cards", "Attackers", "Blockers",
            "OtherAttackers", "Fighters", "Sources", "DamageSources", "CardsFiltered", "Objects"));
    private static final java.util.Set<String> TRIG_PLAYER = new java.util.HashSet<>(Arrays.asList("Player", "Activator", "AttackingPlayer",
            "DefendingPlayer", "Defender", "Attacked", "OriginalController", "DamageTarget", "Target", "OriginalDefender", "AttackedTarget"));
    private static final java.util.Set<String> TRIG_NUMBER = new java.util.HashSet<>(Arrays.asList("Amount", "CounterAmount", "LifeAmount",
            "DamageAmount", "Num", "Number", "NewCounterAmount", "LifeGained", "TokenNum", "PreventedAmount", "ScryNum", "SurveilNum"));

    /** What a trigger would be handed in a game: a card, a player, an amount, so that its text can name them. */
    private static void seedTriggeringObjects(final SpellAbility sa, final Player ai) {
        final forge.game.card.Card any = ai.getCardsIn(ZoneType.Battlefield).isEmpty() ? null : ai.getCardsIn(ZoneType.Battlefield).get(0);
        final Player other = ai.getOpponents().isEmpty() ? ai : ai.getOpponents().get(0);
        for (final forge.game.ability.AbilityKey k : forge.game.ability.AbilityKey.values()) {
            final String n = k.name();
            if (sa.getTriggeringObject(k) != null) {
                continue;
            }
            if (TRIG_CARD.contains(n) && any != null) {
                sa.setTriggeringObject(k, any);
            } else if (TRIG_CARDS.contains(n) && any != null) {
                sa.setTriggeringObject(k, new forge.game.card.CardCollection(any));
            } else if (TRIG_PLAYER.contains(n)) {
                sa.setTriggeringObject(k, other);
            } else if (TRIG_NUMBER.contains(n)) {
                sa.setTriggeringObject(k, 2);
            } else if (n.equals("Produced") || n.equals("Mana")) {
                sa.setTriggeringObject(k, "G");
            } else if (n.equals("Map")) {
                sa.setTriggeringObject(k, new java.util.HashMap<>(java.util.Collections.singletonMap(other, 2)));
            } else if (n.equals("OpponentVotedSame") || n.equals("OpponentVotedDiff")) {
                sa.setTriggeringObject(k, new java.util.ArrayList<>());
            } else if (n.equals("Cause") || n.equals("SpellAbility") || n.equals("SourceSA") || n.equals("StackSa")) {
                sa.setTriggeringObject(k, sa);
            }
        }
    }

    /** The text the stack shows for a triggered ability: the trigger's description, with the effect where it says ABILITY. */
    private Result checkTrigger(final String cardName, final Trigger t, final SpellAbility sa, final Player ai) {
        try {
            sa.setActivatingPlayer(ai);
            seedTriggeringObjects(sa, ai);
            // the "[Zone Changer: null]" tail lists what triggered it: there is nothing in the test game
            final String text = new forge.game.trigger.WrappedAbility(t, sa, ai).getStackDescription(false)
                    .replaceAll("\\s*\\[[A-Z][A-Za-z ]*: .*\\]\\s*$", "");
            final String line = cardName + " | trigger | \"" + text + "\"";
            return new Result(line, text.isEmpty() ? "" : problems(text, cardName, String.valueOf(sa.getApi())), false);
        } catch (final RuntimeException e) {
            return threw(cardName, "trigger", e, true);
        }
    }

    /**
     * The stack text of one ability, linted.
     *
     * @param trigger a trigger runs without what triggered it (a combat, a triggering card, ...), so when it throws that is
     *                a test limit and goes to the errors list, not the todo list
     */
    private Result check(final String cardName, final String kind, final SpellAbility sa, final Player ai, final Player opp,
            final boolean trigger) {
        final String text;
        try {
            sa.setActivatingPlayer(ai);
            for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
                sub.setActivatingPlayer(ai);
                pickTargets(sub, ai, opp);
            }
            if (!pickTargets(sa, ai, opp)) {
                // nothing in the test game to aim it at: the text would be the "no target" one, which says nothing
                return new Result(cardName + " | " + kind + " " + sa.getApi() + " | no legal target in the test game", "", true);
            }
            text = sa.getStackDescription();
        } catch (final RuntimeException e) {
            return threw(cardName, kind, e, trigger);
        }
        final String api = String.valueOf(sa.getApi());
        final String line = cardName + " | " + kind + " " + api + " | \"" + text + "\"";
        if (text == null) {
            return new Result(line, " [null description]", false);
        }
        if (EMPTY.matcher(text.replace(cardName, "~")).matches() && "None".equals(sa.getParam("StackDescription"))) {
            return new Result(line, "", false); // the script says the spell shows nothing of its own
        }
        if (EMPTY.matcher(text.replace(cardName, "~")).matches() && (sa.hasParam("Defined") || sa.hasParam("Enchant"))) {
            // "Defined$ Enchanted" and the like: the test card is not attached to anything
            return new Result(line, "", true);
        }
        if (text.trim().equals("ABILITY") && kind.equals("trigger effect")) {
            return new Result(line, "", false); // the saga chapter's text goes where ABILITY stands
        }
        if ((text.isBlank() || kind.equals("trigger effect") && text.replaceFirst("^.*\\(\\d+\\) -", "").isBlank())
                && (sa.getApi() == null || kind.equals("trigger effect"))) {
            // a land's mana ability or a keyword has no effect text, and a trigger's effect text is not what is shown
            return new Result(line, "", false);
        }
        return new Result(line, problems(text, cardName, api), false);
    }

    private static Result threw(final String cardName, final String kind, final RuntimeException e, final boolean trigger) {
        final StackTraceElement[] st = e.getStackTrace();
        String where = "";
        for (final StackTraceElement f : st) {
            if (f.getClassName().startsWith("forge.")) {
                where = " at " + f.getClassName().replaceFirst("^.*\\.", "") + "." + f.getMethodName() + ":" + f.getLineNumber();
                break;
            }
        }
        return new Result(cardName + " | " + kind + " | threw " + e + where, " [threw " + e.getClass().getSimpleName() + "]", trigger);
    }

    /**
     * What is wrong with a stack text, as " [problem]" per hit, empty when it reads fine. The card's own name is taken out
     * first, so "Welcome to..." or "H.E.R.B.I.E." are not punctuation problems, and the three dots of a name are not
     * doubled punctuation. Heuristics: a hit is a place to look, not always a bug.
     */
    static String problems(final String text, final String cardName, final String api) {
        final String scan = text.replace(cardName, "~").replace("...", "\u2026");
        final StringBuilder sb = new StringBuilder(StackDescriptionTest.lint(scan));
        for (int i = 0; i < GRAMMAR_RE.length; i++) {
            if (GRAMMAR_RE[i].matcher(scan).find()) {
                sb.append(" [").append(GRAMMAR[i][1]).append("]");
            }
        }
        if (scan.chars().filter(c -> c == '(').count() != scan.chars().filter(c -> c == ')').count()) {
            sb.append(" [grammar: unbalanced parentheses]");
        }
        if (scan.chars().filter(c -> c == '"').count() % 2 != 0) {
            sb.append(" [grammar: unbalanced quotes]");
        }
        // a charm itself says nothing, its modes are checked on their own; anything else with an empty text is a gap
        if (!CHARM.equals(api) && EMPTY.matcher(scan).matches()) {
            sb.append(" [grammar: empty text]");
        }
        return sb.toString();
    }

    private static void record(final Result r, final Set<String> ok, final Set<String> todo, final Set<String> errors,
            final Map<String, Set<String>> byProblem) {
        if (r.error) {
            errors.add(r.line);
        } else if (r.problems.isEmpty()) {
            ok.add(r.line);
        } else {
            todo.add(r.line);
            final Matcher m = TAG.matcher(r.problems);
            while (m.find()) {
                byProblem.computeIfAbsent(m.group(1), k -> new LinkedHashSet<>()).add(r.line);
            }
        }
    }
}
