package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import forge.ai.simulation.ComboPlan;
import forge.card.CardRules;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.item.PaperCard;

/**
 * Plans the sideboarding of an AI that knows the deck of its opponent (it has played against it in the games before),
 * the way a person sideboards: cards that do nothing against this opponent come out, cards that are strong against it
 * come in.
 * <p>
 * Nothing here is tied to a card name. Cards are read by what their text does ({@link Role}) and the opponent's deck
 * is read once into a {@link Profile} (how many creatures, flyers, artifacts and enchantments, graveyard cards,
 * counterspell targets, combos). Every role then has a <em>need</em> between 0 and 3 against that profile, a card is
 * worth the need of its role (1 for an ordinary card), and a swap is only made when the card coming in is worth clearly
 * more than the card going out, and only between cards that fit together: the same kind of card (creature for
 * creature), a similar cost, colours the deck already plays, never a land, a mana source or a piece of the AI's own
 * combos.
 * <p>
 * The deck size never changes: every card that comes in replaces one that goes out.
 */
public final class SideboardAdvisor {
    /** What a card is for against another deck. */
    public enum Role {
        ARTIFACT_ENCHANTMENT_HATE("artifact/enchantment removal"),
        GRAVEYARD_HATE("graveyard hate"),
        CREATURE_REMOVAL("creature removal"),
        MASS_REMOVAL("sweeper"),
        COUNTERSPELL("counterspell"),
        FLYER_DEFENSE("defence against flyers"),
        HAND_DISRUPTION("hand disruption");

        final String label;

        Role(final String label) {
            this.label = label;
        }
    }

    /** One card out, one card in. */
    public static final class Swap {
        public final PaperCard out;
        public final PaperCard in;
        public final double gain;
        public final String reason;

        Swap(final PaperCard out, final PaperCard in, final double gain, final String reason) {
            this.out = out;
            this.in = in;
            this.gain = gain;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "-" + out.getName() + " +" + in.getName() + " (" + reason + ")";
        }
    }

    /** What the AI knows about the decks it plays against, boiled down to the numbers sideboarding needs. */
    public static final class Profile {
        int spells;
        int creatures;
        int flyers;
        int artifactsAndEnchantments;
        int graveyardUsers;
        int nonCreatureSpells;
        int instantsAndSorceries;
        boolean graveyardCombo;
        int combos;

        @Override
        public String toString() {
            return spells + " spells: " + creatures + " creatures, " + flyers + " flyers, " + artifactsAndEnchantments
                    + " artifacts/enchantments, " + graveyardUsers + " graveyard cards"
                    + (combos > 0 ? ", " + combos + " combo(s)" : "");
        }
    }

    /** The outcome: the swaps, and a few lines for the person watching to read. */
    public static final class Result {
        public final List<Swap> swaps;
        public final Profile profile;
        public final List<String> report;

        Result(final List<Swap> swaps, final Profile profile, final List<String> report) {
            this.swaps = swaps;
            this.profile = profile;
            this.report = report;
        }
    }

    /** A swap must gain at least this much (an ordinary card is worth 1). */
    static final double MARGIN = 0.7;
    /** A removal spell or counterspell with nothing to do is still worth a little: opponents add cards and bluff. */
    private static final double DEAD_ANSWER = 0.25;
    private static final double BASELINE = 1.0;
    private static final double DIMINISHING = 0.5;

    private static final Pattern GRAVEYARD_WORDS = Pattern.compile("\\b(graveyard|flashback|dredge|unearth|escape|delve|embalm|eternalize)\\b");
    private static final Pattern ARTIFACT_ENCHANTMENT_REMOVAL = Pattern.compile(
            "(destroy|exile|return|counter)( up to (one|two|three))? target (artifact|enchantment|nonland permanent|permanent)"
                    + "|destroy (all|each|target) (artifact|enchantment)|destroy target (artifact or enchantment|artifact, enchantment)"
                    + "|exile target (artifact|enchantment)");
    private static final Pattern GRAVEYARD_REMOVAL = Pattern.compile(
            "exile (target player's|all|each|target opponent's|that player's|up to \\w+ target cards? from)[^.]*graveyard"
                    + "|exile (all|each) graveyard|exile target card from a graveyard|exile all cards from (all|each|target)");
    private static final Pattern CREATURE_REMOVAL = Pattern.compile(
            "(destroy|exile) target (nonblack |nonartifact |attacking |tapped |nonland )?(creature|permanent)"
                    + "|deals? \\d+ damage to (any target|target creature)|target creature gets -\\d+/-\\d+"
                    + "|deals damage equal to [^.]* to target creature|fights? target creature");
    private static final Pattern MASS_REMOVAL = Pattern.compile(
            "destroy all creatures|deals? \\d+ damage to each creature|all creatures get -\\d+/-\\d+|exile all creatures"
                    + "|each creature (gets|loses|is dealt)|destroy each creature");
    private static final Pattern COUNTER = Pattern.compile("counter target (noncreature |creature |instant |sorcery )?spell");
    /** "if it's blue", "that's blue": answers that only work against one colour are not general answers. */
    private static final Pattern COLOR_LOCKED = Pattern.compile("(if it's|that's|is|are) (a |an )?(white|blue|black|red|green|monocolored|multicolored)");
    private static final Pattern HAND_ATTACK = Pattern.compile(
            "target (opponent|player) (reveals|discards)|target opponent discards|each opponent discards|look at target opponent's hand");

    private SideboardAdvisor() {
    }

    // ------------------------------------------------------------------------------------------ reading cards

    /** What the card does, by its text; empty for most cards. */
    public static Set<Role> rolesOf(final CardRules rules) {
        final EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        if (rules == null || rules.getType().isLand()) {
            return roles;
        }
        final String text = rules.getOracleText() == null ? "" : rules.getOracleText().toLowerCase(Locale.ROOT);
        if (ARTIFACT_ENCHANTMENT_REMOVAL.matcher(text).find()
                && (text.contains("artifact") || text.contains("enchantment"))) {
            roles.add(Role.ARTIFACT_ENCHANTMENT_HATE);
        }
        if (GRAVEYARD_REMOVAL.matcher(text).find()) {
            roles.add(Role.GRAVEYARD_HATE);
        }
        final boolean general = !COLOR_LOCKED.matcher(text).find();
        if (MASS_REMOVAL.matcher(text).find()) {
            roles.add(Role.MASS_REMOVAL);
        } else if (general && CREATURE_REMOVAL.matcher(text).find() && !rules.getType().isCreature()) {
            roles.add(Role.CREATURE_REMOVAL);
        }
        if (general && COUNTER.matcher(text).find()) {
            roles.add(Role.COUNTERSPELL);
        }
        if (HAND_ATTACK.matcher(text).find()) {
            roles.add(Role.HAND_DISRUPTION);
        }
        if (rules.getType().isCreature() && (text.contains("reach") || text.contains("flying"))) {
            roles.add(Role.FLYER_DEFENSE);
        }
        return roles;
    }

    /** Reads the decks the AI plays against. {@code owner} only lends its game to create card objects for the combo scan. */
    public static Profile profileOf(final Player owner, final List<List<PaperCard>> opponentMains) {
        final Profile p = new Profile();
        for (final List<PaperCard> main : opponentMains) {
            for (final PaperCard pc : main) {
                final CardRules r = pc.getRules();
                if (r.getType().isLand()) {
                    continue;
                }
                p.spells++;
                final String text = r.getOracleText() == null ? "" : r.getOracleText().toLowerCase(Locale.ROOT);
                if (r.getType().isCreature()) {
                    p.creatures++;
                    if (text.contains("flying")) {
                        p.flyers++;
                    }
                } else {
                    p.nonCreatureSpells++;
                }
                if (r.getType().isInstant() || r.getType().isSorcery()) {
                    p.instantsAndSorceries++;
                }
                if (r.getType().isArtifact() || r.getType().isEnchantment()) {
                    p.artifactsAndEnchantments++;
                }
                if (GRAVEYARD_WORDS.matcher(text).find() && !GRAVEYARD_REMOVAL.matcher(text).find()) {
                    p.graveyardUsers++;
                }
            }
            try {
                final List<Card> cards = new ArrayList<>();
                for (final PaperCard pc : main) {
                    cards.add(Card.fromPaperCard(pc, owner));
                }
                final ComboPlan combos = ComboPlan.scan(cards);
                p.combos += combos.getCombos().size();
                for (final ComboPlan.Combo c : combos.getCombos()) {
                    if (c.kind == ComboPlan.Kind.MASS_REANIMATION || c.kind == ComboPlan.Kind.REANIMATOR) {
                        p.graveyardCombo = true;
                    }
                }
            } catch (final RuntimeException e) {
                // the combo read is a refinement: the counts above are enough to sideboard with
            }
        }
        return p;
    }

    private static double clamp(final double v, final double lo, final double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** How much a card of this role is worth against this opponent: 0 (nothing to hit) to 3 (the best card in the deck). */
    static double need(final Role role, final Profile p, final double ownCreatureShare) {
        switch (role) {
            case ARTIFACT_ENCHANTMENT_HATE:
                return clamp(p.artifactsAndEnchantments / 2.0, 0, 3);
            case GRAVEYARD_HATE:
                return clamp(p.graveyardUsers / 2.0 + (p.graveyardCombo ? 2 : 0), 0, 3);
            case CREATURE_REMOVAL:
                return clamp(p.creatures / 6.0, 0, 2.5);
            case MASS_REMOVAL:
                return clamp((p.creatures - 6) / 3.0, 0, 3) * (ownCreatureShare > 0.4 ? 0.3 : 1.0);
            case COUNTERSPELL: {
                final double share = p.spells == 0 ? 0 : (double) p.nonCreatureSpells / p.spells;
                return clamp(share * 3.0 + (p.combos > 0 ? 1 : 0), 0, 3);
            }
            case FLYER_DEFENSE:
                return clamp(p.flyers / 3.0, 0, 2.5);
            case HAND_DISRUPTION: {
                final double control = p.spells == 0 ? 0 : (double) p.instantsAndSorceries / p.spells;
                return p.combos > 0 ? 2.5 : control >= 0.4 ? 1.5 : 0.7;
            }
            default:
                return BASELINE;
        }
    }

    /** What the card is worth in this matchup, and the role that makes it so (null for an ordinary card). */
    private static double value(final PaperCard pc, final Profile p, final double ownCreatureShare,
                                final Map<Role, Integer> alreadyChanged, final Role[] best) {
        final Set<Role> roles = rolesOf(pc.getRules());
        if (roles.isEmpty()) {
            return BASELINE;
        }
        final boolean creature = pc.getRules().getType().isCreature();
        double value = creature ? BASELINE : DEAD_ANSWER;
        for (final Role r : roles) {
            // each card already brought in for the role makes the next one a little less needed
            final int done = alreadyChanged.getOrDefault(r, 0);
            final double n = Math.max(0, need(r, p, ownCreatureShare) - DIMINISHING * done);
            if (n > value) {
                value = n;
                if (best != null) {
                    best[0] = r;
                }
            }
        }
        return value;
    }

    // ------------------------------------------------------------------------------------------ the plan

    /**
     * @param owner         the AI player (its game is only used to build card objects for reading combos)
     * @param main          the main deck as it is now
     * @param sideboard     the sideboard as it is now
     * @param opponentMains the main decks of the opponents as they were played in the last game
     * @param opponentNames the names to show, one per deck
     */
    public static Result advise(final Player owner, final List<PaperCard> main, final List<PaperCard> sideboard,
                                final List<List<PaperCard>> opponentMains, final List<String> opponentNames,
                                final int maxSwaps) {
        final Profile profile = profileOf(owner, opponentMains);
        final List<String> report = new ArrayList<>();
        final String who = String.join(" and ", opponentNames);
        if (profile.spells == 0 || sideboard.isEmpty()) {
            report.add("studied the deck of " + who + " and made no changes");
            return new Result(Collections.<Swap>emptyList(), profile, report);
        }
        final Set<String> ownKeyCards = ownComboPieces(owner, main);
        byte mainColors = 0;
        int creatures = 0;
        int nonLand = 0;
        for (final PaperCard pc : main) {
            if (!pc.getRules().getType().isLand()) {
                mainColors |= pc.getRules().getColor().getColor();
                nonLand++;
                if (pc.getRules().getType().isCreature()) {
                    creatures++;
                }
            }
        }
        final double ownCreatureShare = nonLand == 0 ? 0 : (double) creatures / nonLand;

        final List<PaperCard> mainLeft = new ArrayList<>(main);
        final List<PaperCard> sideLeft = new ArrayList<>(sideboard);
        final List<Swap> swaps = new ArrayList<>();
        final Map<Role, Integer> changed = new EnumMap<>(Role.class);
        final Set<PaperCard> justIn = new HashSet<>();
        for (int round = 0; round < maxSwaps; round++) {
            PaperCard bestIn = null;
            PaperCard bestOut = null;
            double bestGain = MARGIN;
            Role bestRole = null;
            for (final PaperCard in : sideLeft) {
                if (!canComeIn(in, mainColors)) {
                    continue;
                }
                final Role[] inRole = new Role[1];
                final double vIn = value(in, profile, ownCreatureShare, changed, inRole);
                for (final PaperCard out : mainLeft) {
                    if (justIn.contains(out) || !fits(out, in, ownKeyCards)) {
                        continue;
                    }
                    final double gain = vIn - value(out, profile, ownCreatureShare, Collections.<Role, Integer>emptyMap(), null);
                    // ties go to the cheaper-to-explain pair: the smaller cost difference, then the name, for a stable plan
                    if (gain > bestGain + 1e-9 || bestIn != null && Math.abs(gain - bestGain) < 1e-9 && better(in, out, bestIn, bestOut)) {
                        bestGain = gain;
                        bestIn = in;
                        bestOut = out;
                        bestRole = inRole[0];
                    }
                }
            }
            if (bestIn == null) {
                break;
            }
            swaps.add(new Swap(bestOut, bestIn, bestGain, reasonFor(bestRole, profile)));
            mainLeft.remove(bestOut);
            sideLeft.remove(bestIn);
            mainLeft.add(bestIn);
            sideLeft.add(bestOut);
            justIn.add(bestIn); // a card that just came in is not taken out again
            if (bestRole != null) {
                changed.merge(bestRole, 1, Integer::sum);
            }
        }

        report.add("studied the deck of " + who + " (" + profile + ")");
        if (swaps.isEmpty()) {
            report.add("made no changes from the sideboard");
        }
        for (final Swap s : swaps) {
            report.add(s.toString());
        }
        return new Result(swaps, profile, report);
    }

    /** How many cards the AI may change: a sixth of a 60 card deck, never fewer than two. */
    public static int maxSwapsFor(final int mainSize) {
        return Math.max(2, Math.min(7, mainSize / 8));
    }

    private static boolean better(final PaperCard in, final PaperCard out, final PaperCard bestIn, final PaperCard bestOut) {
        final int d = Math.abs(in.getRules().getManaCost().getCMC() - out.getRules().getManaCost().getCMC());
        final int bd = Math.abs(bestIn.getRules().getManaCost().getCMC() - bestOut.getRules().getManaCost().getCMC());
        if (d != bd) {
            return d < bd;
        }
        final int c = (in.getName() + "/" + out.getName()).compareTo(bestIn.getName() + "/" + bestOut.getName());
        return c < 0;
    }

    private static String reasonFor(final Role role, final Profile p) {
        if (role == null) {
            return "a better card for this matchup";
        }
        switch (role) {
            case ARTIFACT_ENCHANTMENT_HATE:
                return role.label + ": the opponent plays " + p.artifactsAndEnchantments + " artifacts/enchantments";
            case GRAVEYARD_HATE:
                return role.label + ": " + (p.graveyardCombo ? "the opponent's deck plays from the graveyard" : p.graveyardUsers + " graveyard cards");
            case CREATURE_REMOVAL:
                return role.label + ": the opponent plays " + p.creatures + " creatures";
            case MASS_REMOVAL:
                return role.label + ": the opponent plays " + p.creatures + " creatures";
            case COUNTERSPELL:
                return role.label + ": " + p.nonCreatureSpells + " of their " + p.spells + " spells are not creatures";
            case FLYER_DEFENSE:
                return role.label + ": the opponent plays " + p.flyers + " flyers";
            default:
                return role.label + (p.combos > 0 ? ": the opponent plays a combo" : "");
        }
    }

    // ------------------------------------------------------------------------------------------ what fits what

    /** A card that the deck can cast with the colours it already has; lands and cards the AI cannot play stay out. */
    private static boolean canComeIn(final PaperCard in, final byte mainColors) {
        final CardRules r = in.getRules();
        if (r.getType().isLand() || r.getAiHints().getRemAIDecks()) {
            return false; // don't sideboard in anything that we don't know how to play
        }
        return r.getColor().hasNoColorsExcept(mainColors);
    }

    /** Whether {@code in} may take the place of {@code out}. */
    private static boolean fits(final PaperCard out, final PaperCard in, final Set<String> ownKeyCards) {
        final CardRules ro = out.getRules();
        final CardRules ri = in.getRules();
        if (ro.getType().isLand() || out.getName().equals(in.getName()) || ownKeyCards.contains(out.getName())) {
            return false;
        }
        if (ro.getType().isCreature() != ri.getType().isCreature()) {
            return false; // creature for creature: the shape of the deck stays
        }
        if (Math.abs(ro.getManaCost().getCMC() - ri.getManaCost().getCMC()) > 1) {
            return false; // the mana curve stays
        }
        return !hasManaAbility(ro);
    }

    private static boolean hasManaAbility(final CardRules r) {
        final String text = r.getOracleText() == null ? "" : r.getOracleText();
        return text.contains("Add {") || text.contains("Add one mana");
    }

    /** The cards of the AI's own combos: never taken out. */
    private static Set<String> ownComboPieces(final Player owner, final List<PaperCard> main) {
        final Set<String> keys = new HashSet<>();
        try {
            final List<Card> cards = new ArrayList<>();
            for (final PaperCard pc : main) {
                cards.add(Card.fromPaperCard(pc, owner));
            }
            final ComboPlan plan = ComboPlan.scan(cards);
            for (final Card c : cards) {
                if (plan.isKeyCard(c)) {
                    keys.add(c.getName());
                }
            }
        } catch (final RuntimeException e) {
            // no combo information: only the card-type and cost rules protect the deck
        }
        return keys;
    }
}
