package forge.ai.simulation;

import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The land-ramp decks: Scapeshift and Valakut, Primeval Titan, Kessig Wolf Run.
 * <ul>
 * <li><b>Scapeshift</b> is cast only when it kills. {@link #scapeshiftDamage} counts the damage exactly: the lands that
 * can be sacrificed (Valakut stays), the Mountains in the library, a Valakut still in the library (it comes in with
 * the Mountains and sees them all), and the Mountains that stay in play (a land sacrificed that is not a Mountain
 * leaves the five that Valakut needs).</li>
 * <li><b>Land searches</b> (Primeval Titan, Scapeshift, Prismatic Vista...) take Valakut first while it is not in play,
 * then Mountains (the AI took Forests and utility lands with Valakut waiting).</li>
 * <li><b>Khalni Heart Expedition</b> is a ramp engine, see {@link #expeditionStep}: cast before anything else spends the
 * mana, reached by a fetch land or a land drop when it is one mana short, fed with land entries (fetch lands first,
 * Explore for another drop) and activated at three quest counters.</li>
 * <li><b>Kessig Wolf Run</b> pumps a creature of the AI that is attacking, after the blockers: used before or outside
 * combat it spent {X}{R}{G} and the land for a creature that then did nothing.</li>
 * </ul>
 */
public final class RampPlan {
    private static final String SCAPESHIFT = "Scapeshift";
    private static final String WOLF_RUN = "Kessig Wolf Run";
    /** Mountains Valakut needs besides the one that enters. */
    private static final int OTHER_MOUNTAINS = 5;
    private static final int DAMAGE = 3;

    private RampPlan() {
    }

    private static boolean isValakut(final Card c) {
        return c.getName().startsWith("Valakut");
    }

    private static boolean isMountain(final Card c) {
        return c.getType().hasSubtype("Mountain");
    }

    /** Whether the deck is a Valakut deck: Valakut and a way to put Mountains onto the battlefield. */
    public static boolean isValakutDeck(final Player ai) {
        boolean valakut = false;
        boolean way = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Battlefield, ZoneType.Stack)) {
            valakut |= isValakut(c);
            way |= c.getName().equals(SCAPESHIFT) || c.getName().equals("Primeval Titan")
                    || c.getName().equals("Prismatic Vista");
        }
        return valakut && way;
    }

    private static boolean isValakutLand(final Card c) {
        return isValakut(c) && c.isLand();
    }

    /** Whether a Valakut land is in hand, in play or in the library: only then is Scapeshift a Valakut spell. */
    public static boolean valakutAccessible(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield)) {
            if (isValakutLand(c)) {
                return true;
            }
        }
        return false;
    }

    /** One Scapeshift: lands sacrificed, Valakuts among the lands fetched, and the damage that comes of it. */
    private static final class Line {
        int damage;
        int sacrificed;
        int valakuts;
    }

    /**
     * The best Scapeshift line. Every Valakut that is in play, or that enters together
     * with the Mountains, triggers for each Mountain that enters: fetching another Valakut doubles the damage for the
     * price of one Mountain.
     */
    private static Line bestLine(final Player ai) {
        int valakutsInPlay = 0;
        int lands = 0;
        int mountainsInPlay = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isValakutLand(c)) {
                valakutsInPlay++;
            } else if (c.isLand()) {
                lands++;
                if (isMountain(c)) {
                    mountainsInPlay++;
                }
            }
        }
        int valakutsInLibrary = 0;
        int mountainsInLibrary = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (isValakutLand(c)) {
                valakutsInLibrary++;
            } else if (isMountain(c)) {
                mountainsInLibrary++;
            }
        }
        final Line best = new Line();
        if (valakutsInPlay + valakutsInLibrary == 0) {
            return best;
        }
        // the plain line: one Valakut fetched when none is in play, as many lands as there are to fetch
        best.valakuts = valakutsInPlay == 0 ? 1 : 0;
        best.sacrificed = Math.min(lands, mountainsInLibrary + best.valakuts);
        best.damage = lineDamage(valakutsInPlay, lands, mountainsInPlay, mountainsInLibrary, best.sacrificed, best.valakuts);
        for (int j = 0; j <= valakutsInLibrary; j++) {
            for (int s = j + 1; s <= Math.min(lands, mountainsInLibrary + j); s++) {
                final int damage = lineDamage(valakutsInPlay, lands, mountainsInPlay, mountainsInLibrary, s, j);
                if (damage > best.damage) {
                    best.damage = damage;
                    best.sacrificed = s;
                    best.valakuts = j;
                }
            }
        }
        return best;
    }

    private static int lineDamage(final int valakutsInPlay, final int lands, final int mountainsInPlay,
                                  final int mountainsInLibrary, final int sacrificed, final int valakutsFetched) {
        final int entering = sacrificed - valakutsFetched;
        final int active = valakutsInPlay + valakutsFetched;
        if (active <= 0 || entering <= 0 || entering > mountainsInLibrary) {
            return 0;
        }
        // the lands sacrificed are the ones that are not Mountains first
        final int nonMountains = lands - mountainsInPlay;
        final int keptMountains = mountainsInPlay - Math.max(0, sacrificed - nonMountains);
        // each Mountain that enters sees the others: the ones that stay and the ones that enter with it
        return keptMountains + entering - 1 >= OTHER_MOUNTAINS ? DAMAGE * entering * active : 0;
    }

    /** The damage a Scapeshift cast now would do through Valakut, in play or still in the library: 0 for none. */
    public static int scapeshiftDamage(final Player ai) {
        return bestLine(ai).damage;
    }

    /** Whether the Valakut line kills an opponent now. */
    public static boolean scapeshiftLethal(final Player ai) {
        final int damage = scapeshiftDamage(ai);
        for (final Player opponent : ai.getOpponents()) {
            // Valakut's triggers aim at the player: hexproof (Leyline of Sanctity) and shroud keep them away
            if (opponent.canLoseLife() && !opponent.cantLoseForZeroOrLessLife() && damage >= opponent.getLife()
                    && !opponent.hasKeyword(forge.game.keyword.Keyword.HEXPROOF)
                    && !opponent.hasKeyword(forge.game.keyword.Keyword.SHROUD)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a Scapeshift should be held because its Valakut line is not lethal. A deck without an accessible Valakut
     * uses Scapeshift as ordinary ramp: no opinion, the normal evaluation decides.
     */
    public static boolean scapeshiftMustWait(final Player ai) {
        return valakutAccessible(ai) && !scapeshiftLethal(ai);
    }

    /** The number of lands to sacrifice for the best Scapeshift fetch this turn. Valakut itself is not sacrificed. */
    public static int scapeshiftLandsToSacrifice(final Player ai) {
        return bestLine(ai).sacrificed;
    }

    /** Existing Mountains that the Scapeshift sacrifice chooser must preserve for the fetched Mountains to trigger it. */
    public static int scapeshiftMountainsToKeep(final Player ai) {
        int lands = 0;
        int mountainsInPlay = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isValakutLand(c) || !c.isLand()) {
                continue;
            }
            lands++;
            if (isMountain(c)) {
                mountainsInPlay++;
            }
        }
        final int nonMountains = lands - mountainsInPlay;
        final int sacrificedMountains = Math.max(0, scapeshiftLandsToSacrifice(ai) - nonMountains);
        return Math.max(0, mountainsInPlay - sacrificedMountains);
    }

    /** How many of the {@code total} lands searched for should be Valakuts: the count that deals the most damage. */
    private static int valakutsToFetch(final Player ai, final int total, final int valakutsInLibrary) {
        if (!scapeshiftResolving(ai)) {
            // Before the sacrifice the lands about to go are still in play: the Scapeshift line already knows which
            // Mountains stay, so its Valakut count is the one to use (counting them here fetched one Valakut too many)
            final Line line = bestLine(ai);
            if (line.damage > 0 && line.sacrificed == total) {
                return line.valakuts;
            }
        }
        int valakutsInPlay = 0;
        int mountainsInPlay = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isValakutLand(c)) {
                valakutsInPlay++;
            } else if (c.isLand() && isMountain(c)) {
                mountainsInPlay++;
            }
        }
        int best = valakutsInPlay == 0 && valakutsInLibrary > 0 ? 1 : 0;
        int bestDamage = 0;
        for (int j = 0; j <= Math.min(valakutsInLibrary, total); j++) {
            final int entering = total - j;
            final int damage = valakutsInPlay + j > 0 && entering > 0 && mountainsInPlay + entering - 1 >= OTHER_MOUNTAINS
                    ? DAMAGE * entering * (valakutsInPlay + j) : 0;
            if (damage > bestDamage) {
                bestDamage = damage;
                best = j;
            }
        }
        return best;
    }

    /** Whether a Scapeshift is on the stack: its lands are sacrificed already and the board is the one that stays. */
    private static boolean scapeshiftResolving(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Stack)) {
            if (c.getName().equals(SCAPESHIFT)) {
                return true;
            }
        }
        return false;
    }

    private static CardCollection landsOnly(final Iterable<Card> pool) {
        final CardCollection lands = new CardCollection();
        for (final Card c : pool) {
            if (!c.isLand()) {
                return null;
            }
            lands.add(c);
        }
        return lands;
    }

    private static CardCollection order(final CardCollection lands, final int valakuts, final int n) {
        final CardCollection out = new CardCollection();
        for (final Card c : lands) {
            if (isValakutLand(c) && out.size() < Math.min(n, valakuts)) {
                out.add(c);
            }
        }
        // basic Mountains first: they thin the deck of the lands that count, and nothing else is needed from them
        for (final Card c : lands) {
            if (out.size() < n && !out.contains(c) && isMountain(c) && c.getType().isBasic()) {
                out.add(c);
            }
        }
        for (final Card c : lands) {
            if (out.size() < n && !out.contains(c) && isMountain(c)) {
                out.add(c);
            }
        }

        // The Valakut line is a priority, not a restriction on the rest of the search.
        // Fill any remaining slots with the normal AI land preference rather than returning
        // fewer cards than the search can legally find. Prefer other lands over extra Valakuts
        // that the damage calculation deliberately chose not to fetch.
        final CardCollection remaining = new CardCollection();
        final CardCollection extraValakuts = new CardCollection();
        for (final Card c : lands) {
            if (out.contains(c)) {
                continue;
            }
            if (isValakutLand(c)) {
                extraValakuts.add(c);
            } else {
                remaining.add(c);
            }
        }
        while (out.size() < n) {
            Card best = ComputerUtilCard.getBestLandAI(remaining);
            if (best == null) {
                best = ComputerUtilCard.getBestLandAI(extraValakuts);
            }
            if (best == null) {
                break;
            }
            out.add(best);
            remaining.remove(best);
            extraValakuts.remove(best);
        }
        return out;
    }

    private static int count(final Iterable<Card> cards) {
        int n = 0;
        for (final Card c : cards) {
            if (isValakutLand(c)) {
                n++;
            }
        }
        return n;
    }

    /**
     * The lands to search for, or null for no opinion: the Valakuts that add damage (one while none is in play, more
     * when another Valakut sees the Mountains that enter with it), then Mountains. The pool has to be lands (a creature
     * search is not this plan's).
     */
    public static CardCollection picks(final Player ai, final Iterable<Card> pool, final int n) {
        if (ai == null || pool == null || n <= 0 || !isValakutDeck(ai)) {
            return null;
        }
        final CardCollection lands = landsOnly(pool);
        if (lands == null) {
            return null;
        }
        final CardCollection out = order(lands, valakutsToFetch(ai, n, count(lands)), n);
        return out.isEmpty() ? null : out;
    }

    /**
     * The next land of a search that is made one card at a time, {@code total} cards in all: the cards picked already
     * are gone from the pool but still in the library, which tells how many Valakuts are done.
     */
    public static Card nextPick(final Player ai, final Iterable<Card> pool, final int total) {
        if (ai == null || pool == null || total <= 0 || !isValakutDeck(ai)) {
            return null;
        }
        final CardCollection lands = landsOnly(pool);
        if (lands == null) {
            return null;
        }
        final int inLibrary = count(ai.getCardsIn(ZoneType.Library));
        final int picked = inLibrary - count(lands);
        final int wanted = valakutsToFetch(ai, total, inLibrary) - picked;
        final CardCollection out = order(lands, Math.max(0, wanted), 1);
        return out.isEmpty() ? null : out.get(0);
    }

    /** Whether Kessig Wolf Run's pump waits: it is for an attacker of the AI after the blockers are declared. */
    public static boolean wolfRunWaits(final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (ai == null || host == null || !host.getName().equals(WOLF_RUN) || sa.isSpell() || sa.getApi() != ApiType.Pump
                || !host.isInZone(ZoneType.Battlefield) || !ai.equals(host.getController())) {
            return false;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return true;
        }
        for (final Card c : ai.getCreaturesInPlay()) {
            if (ai.getGame().getCombat() != null && ai.getGame().getCombat().isAttacking(c)) {
                return false;
            }
        }
        return true;
    }

    private static final String EXPEDITION = "Khalni Heart Expedition";
    private static final String EXPLORE = "Explore";
    private static final int QUESTS = 3;

    /** The basic lands left in the library: what the Expedition can still fetch. */
    public static int basicsInLibrary(final Player ai) {
        int basics = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (c.isBasicLand()) {
                basics++;
            }
        }
        return basics;
    }

    private static Card named(final Player ai, final ZoneType zone, final String name) {
        for (final Card c : ai.getCardsIn(zone)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    private static List<SpellAbility> abilitiesOf(final Player ai, final Card c) {
        return ComputerUtilAbility.getSpellAbilities(new CardCollection(c), ai);
    }

    private static SpellAbility approved(final Player ai, final SpellAbility sa, final Predicate<SpellAbility> approve) {
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        return approve.test(sa) ? sa : null;
    }

    /**
     * Identifies fetch lands even while they are in hand.
     *
     * Do not use ComputerUtilAbility.getSpellAbilities() here:
     * it filters abilities that cannot currently be activated,
     * including fetch abilities whose source is still in hand.
     */
    private static boolean isFetchLand(final Card land) {
        if (land == null || !land.isLand()) {
            return false;
        }
        for (final SpellAbility sa : land.getSpellAbilities()) {
            if (LandfallGuard.isFetch(sa)) {
                return true;
            }
        }
        return false;
    }

    /** The land drops that the hand can make now. */
    private static List<Card> landsToPlay(final Player ai) {
        final List<Card> out = new ArrayList<>();
        final CardCollection lands = ComputerUtilAbility.getAvailableLandsToPlay(ai.getGame(), ai);
        if (lands != null) {
            for (final Card c : lands) {
                if (c.isInZone(ZoneType.Hand)) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    /** The land play of this card, or null. */
    private static SpellAbility landPlay(final Player ai, final Card land, final Predicate<SpellAbility> approve) {
        for (final SpellAbility sa : abilitiesOf(ai, land)) {
            if (sa.isLandAbility()) {
                final SpellAbility ok = approved(ai, sa, approve);
                if (ok != null) {
                    return ok;
                }
            }
        }
        return null;
    }

    /** The crack of a fetch land of the battlefield (only the ones that bring the land in untapped when asked to). */
    private static SpellAbility crack(final Player ai, final boolean untappedOnly, final Predicate<SpellAbility> approve) {
        for (final Card land : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!land.isLand()) {
                continue;
            }
            for (final SpellAbility sa : abilitiesOf(ai, land)) {
                if (LandfallGuard.isFetch(sa) && !(untappedOnly && "True".equals(sa.getParam("Tapped")))) {
                    final SpellAbility ok = approved(ai, sa, approve);
                    if (ok != null) {
                        return ok;
                    }
                }
            }
        }
        return null;
    }

    /** The colours a land can make (a mana colour mask). */
    public static byte landColors(final Card land) {
        byte colors = 0;
        for (final SpellAbility sa : land.getManaAbilities()) {
            colors |= ManaConversion.producedColors(sa);
        }
        return colors;
    }

    /** The mana a spell leaves behind as a token that is sacrificed for it (Malevolent Rumble's Eldrazi Spawn, Treasure). */
    public static int tokenMana(final Card c) {
        int mana = 0;
        for (final SpellAbility spell : c.getSpellAbilities()) {
            if (!spell.isSpell()) {
                continue;
            }
            for (SpellAbility sa = spell; sa != null; sa = sa.getSubAbility()) {
                final String script = sa.getParamOrDefault("TokenScript", "");
                if (sa.getApi() == ApiType.Token && (script.contains("eldrazi_spawn") || script.contains("treasure"))) {
                    int amount = 1;
                    try {
                        amount = Integer.parseInt(sa.getParamOrDefault("TokenAmount", "1"));
                    } catch (final NumberFormatException e) {
                        // a variable amount: one is what is certain
                    }
                    mana += amount;
                }
            }
            return mana;
        }
        return 0;
    }

    /**
     * A spell that makes mana (Malevolent Rumble) is cast before the card that its token pays for: with two lands and a
     * third that came in untapped, the Rumble first and the Spawn for a two-drop is four mana out of three lands, the
     * other way round is one spell.
     */
    public static SpellAbility tokenManaStep(final Player ai, final Predicate<SpellAbility> approve) {
        if (ai == null || ai.getGame() == null || !ai.getGame().getStack().isEmpty()) {
            return null;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        try {
            final int mana = ComputerUtilMana.getAvailableManaEstimate(ai);
            for (final Card maker : ai.getCardsIn(ZoneType.Hand)) {
                final int bonus = maker.isLand() ? 0 : tokenMana(maker);
                if (bonus <= 0 || maker.getCMC() > mana) {
                    continue;
                }
                // another card that only fits because of the token: more than the lands pay, no more than lands and token
                boolean needed = false;
                for (final Card other : ai.getCardsIn(ZoneType.Hand)) {
                    if (other != maker && !other.isLand() && !other.isInstant() && !other.hasKeyword(forge.game.keyword.Keyword.FLASH)
                            && maker.getCMC() + other.getCMC() > mana && maker.getCMC() + other.getCMC() <= mana + bonus) {
                        needed = true;
                        break;
                    }
                }
                if (!needed) {
                    continue;
                }
                for (final SpellAbility sa : abilitiesOf(ai, maker)) {
                    if (sa.isSpell()) {
                        final SpellAbility ok = approved(ai, sa, approve);
                        if (ok != null) {
                            return ok;
                        }
                    }
                }
            }
        } catch (final RuntimeException e) {
            VetoLog.note(ai, "token-mana-step failed: " + e);
        }
        return null;
    }

    /**
     * The next step of the Khalni Heart Expedition ramp, or null when it has no opinion (no Expedition, no basic land
     * left to fetch, not the AI's main phase, something on the stack). Every call looks at the table as it is, so a land
     * that a fetch land has just brought in is seen at once. In this order:
     * <ol>
     * <li>three quest counters: activate it (two basic lands) before anything else is done with the mana;</li>
     * <li>in the hand and payable: cast it, before a fetch land is cracked or Explore is cast;</li>
     * <li>in the hand and one mana short: a fetch land of the battlefield that brings an untapped land (the land drop
     * stays for after the Expedition, where it earns a counter), else an untapped land of the hand;</li>
     * <li>in play with fewer counters: crack a fetch land, play a land (a fetch land first, it enters twice), and when the
     * land drop is spent and a land is still in the hand, cast Explore for another one.</li>
     * </ol>
     * @param approve the AI's own check that the play can be made
     * @param entersTapped whether a land of the hand enters tapped
     */
    public static SpellAbility expeditionStep(final Player ai, final Predicate<SpellAbility> approve,
                                              final Predicate<Card> entersTapped) {
        if (ai == null || ai.getGame() == null || !ai.getGame().getStack().isEmpty()) {
            return null;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        try {
            final Card inHand = named(ai, ZoneType.Hand, EXPEDITION);
            final Card inPlay = named(ai, ZoneType.Battlefield, EXPEDITION);
            if ((inHand == null && inPlay == null) || basicsInLibrary(ai) < 1) {
                return null; // nothing to fetch: the Expedition is no plan
            }
            if (inPlay != null && inPlay.getCounters(CounterEnumType.QUEST) >= QUESTS) {
                for (final SpellAbility sa : abilitiesOf(ai, inPlay)) {
                    if (!sa.isSpell() && sa.getApi() == ApiType.ChangeZone) {
                        final SpellAbility ok = approved(ai, sa, approve);
                        if (ok != null) {
                            return ok;
                        }
                    }
                }
            }
            if (inHand != null) {
                for (final SpellAbility sa : abilitiesOf(ai, inHand)) {
                    if (sa.isSpell()) {
                        final SpellAbility ok = approved(ai, sa, approve);
                        if (ok != null) {
                            return ok;
                        }
                    }
                }
                final int sources = ComputerUtilMana.getAvailableManaSources(ai, true).size();
                if (sources + 1 < inHand.getCMC() || sources >= inHand.getCMC()) {
                    return null; // two short, or short of something other than lands
                }
                final SpellAbility fetched = crack(ai, true, approve);
                if (fetched != null) {
                    return fetched;
                }
                final List<Card> drops = landsToPlay(ai);
                for (final boolean fetch : new boolean[] { false, true }) {
                    for (final Card land : drops) {
                        if (isFetchLand(land) == fetch && (fetch || !entersTapped.test(land))) {
                            final SpellAbility ok = landPlay(ai, land, approve);
                            if (ok != null) {
                                return ok;
                            }
                        }
                    }
                }
                return null;
            }
            // in play, counters to collect: every land that enters is one
            final SpellAbility cracked = crack(ai, false, approve);
            if (cracked != null) {
                return cracked;
            }
            final List<Card> drops = landsToPlay(ai);
            for (final boolean fetch : new boolean[] { true, false }) {
                for (final Card land : drops) {
                    if (isFetchLand(land) == fetch) {
                        final SpellAbility ok = landPlay(ai, land, approve);
                        if (ok != null) {
                            return ok;
                        }
                    }
                }
            }
            final Card explore = named(ai, ZoneType.Hand, EXPLORE);
            boolean landInHand = false;
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                landInHand |= c.isLand();
            }
            if (explore != null && drops.isEmpty() && landInHand) {
                for (final SpellAbility sa : abilitiesOf(ai, explore)) {
                    if (sa.isSpell()) {
                        final SpellAbility ok = approved(ai, sa, approve);
                        if (ok != null) {
                            return ok;
                        }
                    }
                }
            }
            return null;
        } catch (final RuntimeException e) {
            return null;
        }
    }
}
