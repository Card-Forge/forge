package forge.ai.simulation;

import forge.ai.ComputerUtilAbility;
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

    /** The damage a Scapeshift cast now would do through Valakut, in play or still in the library: 0 for none. */
    public static int scapeshiftDamage(final Player ai) {
        boolean valakutInPlay = false;
        int lands = 0;
        int mountainsInPlay = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isValakut(c)) {
                valakutInPlay = true;
            } else if (c.isLand()) {
                lands++;
                if (isMountain(c)) {
                    mountainsInPlay++;
                }
            }
        }
        boolean valakutInLibrary = false;
        int mountainsInLibrary = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (isValakut(c)) {
                valakutInLibrary = true;
            } else if (isMountain(c)) {
                mountainsInLibrary++;
            }
        }
        if (!valakutInPlay && !valakutInLibrary) {
            return 0;
        }
        final int extra = !valakutInPlay ? 1 : 0; // the Valakut fetched takes one of the searched lands
        final int sacrificed = Math.min(lands, mountainsInLibrary + extra);
        final int entering = sacrificed - extra;
        if (entering <= 0) {
            return 0;
        }
        // the lands sacrificed are the ones that are not Mountains first
        final int nonMountains = lands - mountainsInPlay;
        final int keptMountains = mountainsInPlay - Math.max(0, sacrificed - nonMountains);
        // each Mountain that enters sees the others: the ones that stay and the ones that enter with it
        return keptMountains + entering - 1 >= OTHER_MOUNTAINS ? DAMAGE * entering : 0;
    }

    /** Whether a Scapeshift should be held because its current Valakut line is not lethal. */
    public static boolean scapeshiftMustWait(final Player ai) {
        final int damage = scapeshiftDamage(ai);
        for (final Player opponent : ai.getOpponents()) {
            if (opponent.canLoseLife() && !opponent.cantLoseForZeroOrLessLife() && damage >= opponent.getLife()) {
                return false;
            }
        }
        return true;
    }

    /** The number of lands to sacrifice for the best Scapeshift fetch this turn. Valakut itself is not sacrificed. */
    public static int scapeshiftLandsToSacrifice(final Player ai) {
        boolean valakutInPlay = false;
        boolean valakutInLibrary = false;
        int lands = 0;
        int mountainsInLibrary = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isValakut(c)) {
                valakutInPlay = true;
            } else if (c.isLand()) {
                lands++;
            }
        }
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (isValakut(c)) {
                valakutInLibrary = true;
            } else if (isMountain(c)) {
                mountainsInLibrary++;
            }
        }
        if (!valakutInPlay && !valakutInLibrary) {
            return 0;
        }
        final int valakutSlot = valakutInPlay ? 0 : 1;
        return Math.min(lands, mountainsInLibrary + valakutSlot);
    }

    /** Existing Mountains that the Scapeshift sacrifice chooser must preserve for the fetched Mountains to trigger. */
    public static int scapeshiftMountainsToKeep(final Player ai) {
        int lands = 0;
        int mountainsInPlay = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isValakut(c) || !c.isLand()) {
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

    /**
     * The lands to search for, or null for no opinion: Valakut first while it is not in play, then Mountains. The pool has to be lands (a creature search is not this plan's).
     */
    public static CardCollection picks(final Player ai, final Iterable<Card> pool, final int n) {
        if (ai == null || pool == null || n <= 0 || !isValakutDeck(ai)) {
            return null;
        }
        final CardCollection lands = new CardCollection();
        for (final Card c : pool) {
            if (!c.isLand()) {
                return null;
            }
            lands.add(c);
        }
        boolean valakutInPlay = false;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            valakutInPlay |= isValakut(c);
        }
        final CardCollection out = new CardCollection();
        if (!valakutInPlay) {
            for (final Card c : lands) {
                if (isValakut(c) && out.size() < n) {
                    out.add(c);
                }
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
        return out.isEmpty() ? null : out;
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
