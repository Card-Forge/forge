package forge.ai.simulation;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The land-ramp decks: Scapeshift and Valakut, Primeval Titan, Kessig Wolf Run.
 * <ul>
 * <li><b>Scapeshift</b> is cast only when it kills. {@link #scapeshiftDamage} counts the damage exactly: the lands that
 * can be sacrificed (Valakut stays), the Mountains in the library, a Valakut still in the library (it comes in with
 * the Mountains and sees them all), and the Mountains that stay in play (a land sacrificed that is not a Mountain
 * leaves the five that Valakut needs).</li>
 * <li><b>Land searches</b> (Primeval Titan, Scapeshift, Prismatic Vista...) take Valakut first while it is not in play,
 * then Mountains (the AI took Forests and utility lands with Valakut waiting).</li>
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
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Battlefield)) {
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
}
