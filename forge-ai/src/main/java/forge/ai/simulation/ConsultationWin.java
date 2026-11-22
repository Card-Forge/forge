package forge.ai.simulation;

import java.util.List;

import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Demonic Consultation with Thassa's Oracle: the Oracle is cast first, and with its "when it enters" trigger on the
 * stack the Consultation names a card that is not in the library: the whole library is exiled, and the trigger
 * resolves with no library left and wins. Nothing else makes the Consultation safe: it exiles six cards and then the
 * rest of the library (a wrong name decks the player), so every other cast is refused (ComboGuard,
 * {@code consultation-no-winner}). The Laboratory Maniac needs a draw and is not part of this.
 */
public final class ConsultationWin {
    /** The Oracle wins while the library is no bigger than the devotion to blue it counts. */
    private static final int LIBRARY_MAX = 2;

    private ConsultationWin() {
    }

    /** Demonic Consultation: name a card, then reveal until it, the rest exiled. Read from the abilities of the card. */
    static boolean isConsultation(final SpellAbility sa) {
        if (sa == null || !sa.isSpell()) {
            return false;
        }
        boolean name = false;
        boolean until = false;
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            name |= a.getApi() == ApiType.NameCard;
            until |= a.getApi() == ApiType.DigUntil && a.getParamOrDefault("Valid", "").contains("NamedCard");
        }
        return name && until;
    }

    /** A creature that wins when it enters with the library gone, with a trigger of its own (not the Laboratory Maniac). */
    static boolean isEnterWinner(final Card c) {
        return DreadReturnWin.isWinner(c) && !c.getReplacementEffects().iterator().hasNext();
    }

    /** Whether the trigger of such a creature is the top of the stack and the player's. */
    static boolean winnerOnStack(final Game game, final Player ai) {
        if (game == null || game.getStack().isEmpty()) {
            return false;
        }
        final SpellAbility top = game.getStack().peekAbility();
        return top != null && top.isTrigger() && ai.equals(top.getActivatingPlayer())
                && isEnterWinner(top.getHostCard());
    }

    private static SpellAbility consultationIn(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            for (final SpellAbility sa : c.getSpellAbilities()) {
                if (isConsultation(sa)) {
                    sa.setActivatingPlayer(ai);
                    return sa;
                }
            }
        }
        return null;
    }

    /**
     * Step one, in the main phase: the winner is cast when the Consultation can be paid for as well and the library is
     * still full. Null when this is not the time.
     */
    public static SpellAbility sureCast(final Player ai, final List<SpellAbility> candidates) {
        if (ai == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !game.getStack().isEmpty() || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))
                || ai.getCardsIn(ZoneType.Library).size() <= LIBRARY_MAX) {
            return null;
        }
        final SpellAbility consultation = consultationIn(ai);
        if (consultation == null) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || !ai.equals(host.getController())
                    || !isEnterWinner(host)) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            if (ComputerUtilMana.getAvailableManaEstimate(ai) >= host.getCMC() + consultation.getHostCard().getCMC()
                    && sa.canPlay() && ComputerUtilCost.canPayCost(sa, ai, false)) {
                return sa;
            }
        }
        return null;
    }

    /** Step two, with the winner's trigger on the stack: the Consultation in response. Null when this is not the time. */
    public static SpellAbility response(final Player ai) {
        if (ai == null || !winnerOnStack(ai.getGame(), ai) || ai.getCardsIn(ZoneType.Library).size() <= LIBRARY_MAX) {
            return null;
        }
        final SpellAbility consultation = consultationIn(ai);
        if (consultation != null && consultation.canPlay() && ComputerUtilCost.canPayCost(consultation, ai, false)) {
            return consultation;
        }
        return null;
    }

    /**
     * The name for the Consultation answering the trigger: a card that is not in the library, so that all of it is
     * exiled. Null when the Consultation is not part of this line (the normal choice is made then).
     */
    public static String nameOutsideLibrary(final Player ai, final SpellAbility sa) {
        if (ai == null || !isPartOfConsultation(sa) || !winnerOnStack(ai.getGame(), ai)) {
            return null;
        }
        final CardCollectionView library = ai.getCardsIn(ZoneType.Library);
        final String[] fallback = { "Morphling", "Ancestral Recall", "Black Lotus", "Time Walk", "Mox Sapphire" };
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (!c.isLand() && !inLibrary(library, c.getName())) {
                return c.getName();
            }
        }
        for (final String n : fallback) {
            if (!inLibrary(library, n)) {
                return n;
            }
        }
        return null;
    }

    private static boolean isPartOfConsultation(final SpellAbility sa) {
        for (SpellAbility a = sa == null ? null : sa.getRootAbility(); a != null; a = a.getSubAbility()) {
            if (isConsultation(a) || a.getApi() == ApiType.NameCard && a.getSubAbility() != null
                    && a.getSubAbility().getApi() == ApiType.Dig) {
                return true;
            }
        }
        return false;
    }

    private static boolean inLibrary(final CardCollectionView library, final String name) {
        for (final Card c : library) {
            if (name.equals(c.getName())) {
                return true;
            }
        }
        return false;
    }
}
