package forge.ai.simulation;

import forge.card.MagicColor;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Psychatog and Upheaval. Upheaval ({4}{U}{U}) returns every permanent to its owner's hand, the lands too, so the
 * turn it is cast is the turn the lands are gone: the deck wins by floating the mana for Psychatog before the spell
 * resolves, casting the Psychatog from the pool (with the land drop, if it is still free) onto a board that the opponent
 * has to start again, and attacking with it next turn, fed by the cards of the hand and the graveyard.
 * <ul>
 * <li><b>When.</b> Upheaval is held until the lands (and what is already in the pool) are six for the spell and three
 * for Psychatog, one less when the land drop is free, with a blue source for each of the three blue pips and a black
 * one, a Psychatog in the hand or on the battlefield (it comes back to the hand), and the opponent has something to
 * lose: four permanents or more.</li>
 * <li><b>How.</b> Every untapped land is tapped for mana first, the colours that Psychatog needs first, so that the pool
 * holds what the Upheaval does not use; the Upheaval is then paid from the pool.</li>
 * <li><b>After.</b> In the same turn: Psychatog from the pool; when the pool is short, a land from the hand first.</li>
 * </ul>
 */
public final class UpheavalPlan {
    private static final String UPHEAVAL = "Upheaval";
    private static final String PSYCHATOG = "Psychatog";
    private static final int UPHEAVAL_COST = 6;
    private static final int PSYCHATOG_COST = 3;
    private static final int OPPONENT_PERMANENTS = 4;

    private UpheavalPlan() {
    }

    /** Whether the deck has both cards: only then is Upheaval a combo piece and not a spell on its own. */
    public static boolean isUpheavalDeck(final Player ai) {
        boolean upheaval = false;
        boolean atog = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield, ZoneType.Graveyard)) {
            upheaval |= c.getName().equals(UPHEAVAL);
            atog |= c.getName().equals(PSYCHATOG);
        }
        return upheaval && atog;
    }

    private static boolean upheavalCastThisTurn(final Player ai) {
        return ai.getGame().getStack().getSpellsCastThisTurn().stream()
                .anyMatch(sp -> ai.equals(sp.getActivatingPlayer()) && sp.getHostCard() != null
                        && sp.getHostCard().getName().equals(UPHEAVAL));
    }

    private static boolean landDropFree(final Player ai) {
        return ai.getLandsPlayedThisTurn() < ai.getMaxLandPlays();
    }

    /**
     * Mana from the untapped lands and the pool, and what the lands can do about the colours: {total, blue in the pool, black in
     * the pool, lands that make blue and not black, lands that make black and not blue, lands that make either}. A land is
     * one mana, so a dual land is a blue source or a black one, not both at once.
     */
    private static int[] mana(final Player ai) {
        int total = ai.getManaPool().totalMana();
        int blueOnly = 0;
        int blackOnly = 0;
        int either = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isLand() || c.isTapped()) {
                continue;
            }
            total++;
            final byte colors = OpeningHand.producedColors(c);
            final boolean blue = (colors & MagicColor.BLUE) != 0;
            final boolean black = (colors & MagicColor.BLACK) != 0;
            if (blue && black) {
                either++;
            } else if (blue) {
                blueOnly++;
            } else if (black) {
                blackOnly++;
            }
        }
        return new int[] {total, ai.getManaPool().getAmountOfColor(MagicColor.BLUE),
                ai.getManaPool().getAmountOfColor(MagicColor.BLACK), blueOnly, blackOnly, either};
    }

    /** Whether the blue and the black that are still missing from the pool can come from different lands. */
    static boolean colorsCoverable(final int needBlue, final int needBlack, final int poolBlue, final int poolBlack,
                                   final int blueOnly, final int blackOnly, final int either) {
        final int blueShort = Math.max(0, needBlue - poolBlue - blueOnly);
        final int blackShort = Math.max(0, needBlack - poolBlack - blackOnly);
        return blueShort + blackShort <= either;
    }

    /** Whether Upheaval is worth casting now: the mana, the Psychatog to put back, and something to bounce. */
    public static boolean ready(final Player ai) {
        if (!isUpheavalDeck(ai) || !LoopRunner.inHand(ai, UPHEAVAL)) {
            return false;
        }
        boolean atog = LoopRunner.inHand(ai, PSYCHATOG);
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            atog |= c.getName().equals(PSYCHATOG);
        }
        if (!atog) {
            return false;
        }
        final int[] m = mana(ai);
        final int need = UPHEAVAL_COST + PSYCHATOG_COST - (landDropFree(ai) ? 1 : 0);
        if (m[0] < need || !colorsCoverable(3, 1, m[1], m[2], m[3], m[4], m[5])) {
            return false;
        }
        int theirs = 0;
        for (final Player opp : ai.getOpponents()) {
            theirs = Math.max(theirs, opp.getCardsIn(ZoneType.Battlefield).size());
        }
        return theirs >= OPPONENT_PERMANENTS;
    }

    /** The Upheaval waits for {@link #ready}: cast by the generic code it was a six-mana spell that took the AI's lands away. */
    public static boolean held(final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        return host != null && sa.isSpell() && host.getName().equals(UPHEAVAL) && host.isInZone(ZoneType.Hand)
                && isUpheavalDeck(ai) && !ready(ai) && !upheavalCastThisTurn(ai);
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        if (!isUpheavalDeck(ai) || !ai.getGame().getStack().isEmpty()
                || !(ai.getGame().getPhaseHandler().is(forge.game.phase.PhaseType.MAIN1, ai)
                || ai.getGame().getPhaseHandler().is(forge.game.phase.PhaseType.MAIN2, ai))) {
            return null;
        }
        if (upheavalCastThisTurn(ai)) {
            return afterwards(ai, candidates, approve);
        }
        if (!ready(ai)) {
            return null;
        }
        final SpellAbility tap = floatMana(ai, candidates, approve);
        if (tap != null) {
            return tap;
        }
        final SpellAbility upheaval = LoopRunner.castFromHand(ai, candidates, UPHEAVAL);
        if (upheaval != null && approve.test(upheaval)) {
            VetoLog.note(ai, "upheaval-plan | cast with the pool");
            return upheaval;
        }
        return null;
    }

    /**
     * One untapped land into the pool. The colours that few lands make go first: a land that makes only black or only blue
     * for what the pool lacks, and the dual lands last, for the colour that is still missing, black before blue (it is one
     * mana and only a dual land may be left to make it). With the first matching ability, two Underground Seas could both be
     * tapped for blue ahead of the Islands, and no black was left for the Psychatog.
     */
    private static SpellAbility floatMana(final Player ai, final List<SpellAbility> candidates,
                                          final Predicate<SpellAbility> approve) {
        final boolean needBlack = ai.getManaPool().getAmountOfColor(MagicColor.BLACK) == 0;
        final boolean needBlue = ai.getManaPool().getAmountOfColor(MagicColor.BLUE) < 3;
        SpellAbility blackOnly = null;
        SpellAbility blueOnly = null;
        SpellAbility dualForBlack = null;
        SpellAbility dualForBlue = null;
        SpellAbility any = null;
        SpellAbility anyDual = null;
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isManaAbility() || !host.isLand() || host.isTapped() || !ai.equals(host.getController())
                    || !host.isInZone(ZoneType.Battlefield)) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            if (!approve.test(sa)) {
                continue;
            }
            final byte landColors = OpeningHand.producedColors(host); // what the land can make, not this one ability
            final boolean dual = (landColors & MagicColor.BLACK) != 0 && (landColors & MagicColor.BLUE) != 0;
            final byte colors = ManaConversion.producedColors(sa);
            final boolean black = (colors & MagicColor.BLACK) != 0;
            final boolean blue = (colors & MagicColor.BLUE) != 0;
            if (needBlack && black) {
                if (dual) {
                    dualForBlack = dualForBlack == null ? sa : dualForBlack;
                } else {
                    blackOnly = blackOnly == null ? sa : blackOnly;
                }
            }
            if (needBlue && blue) {
                if (dual) {
                    dualForBlue = dualForBlue == null ? sa : dualForBlue;
                } else {
                    blueOnly = blueOnly == null ? sa : blueOnly;
                }
            }
            if (any == null && !dual) {
                any = sa;
            }
            if (anyDual == null && dual) {
                anyDual = sa; // every land is tapped in the end: the dual lands that no colour is wanted from, last
            }
        }
        // the lands that make one colour only, then the dual lands for the missing black, then for the missing blue
        for (final SpellAbility pick : new SpellAbility[] {blackOnly, blueOnly, dualForBlack, dualForBlue, any, anyDual}) {
            if (pick != null) {
                return pick;
            }
        }
        return null;
    }

    /** The same turn: Psychatog out of the pool, and a land first when the pool is short of it. */
    private static SpellAbility afterwards(final Player ai, final List<SpellAbility> candidates,
                                           final Predicate<SpellAbility> approve) {
        final SpellAbility atog = LoopRunner.castFromHand(ai, candidates, PSYCHATOG);
        if (atog != null && forge.ai.ComputerUtilMana.canPayManaCost(atog, ai, 0, false) && approve.test(atog)) {
            return atog;
        }
        if (atog != null && landDropFree(ai)) {
            for (final SpellAbility sa : candidates) {
                final Card host = sa == null ? null : sa.getHostCard();
                if (host != null && sa.isLandAbility() && host.isInZone(ZoneType.Hand) && approve.test(sa)) {
                    return sa;
                }
            }
        }
        return null;
    }
}
