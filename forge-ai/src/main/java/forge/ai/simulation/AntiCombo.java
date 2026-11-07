package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;

/**
 * What the opponents' decks can do to the AI that it can see: their combo pieces and win conditions on the battlefield,
 * on the stack and in the graveyard (where Reanimate, Dredge and Dread Return find them). The cards come from the roles
 * ANTI_COMBO_KEY (the heart of a combo or a win condition) and ANTI_COMBO_PIECE (the rituals, tutors and draw around it).
 *
 * <p>It is a term of the rollouts' evaluation: the more of it the opponents have, the worse the position, so the plays
 * that remove a key card (a destroy, an exile, a bounce, a counterspell on the stack, an exile of the graveyard) come out
 * ahead of the plays that do not, and the target of a removal spell is ranked by it. Nothing in it looks at a hidden
 * zone: a card in an opponent's hand or library is not counted.
 */
public final class AntiCombo {
    private AntiCombo() {
    }

    /** What a key card on the battlefield or on the stack is worth to the opponent. */
    static final int KEY_WEIGHT = 60;
    /** What a piece around the keys is worth. */
    static final int PIECE_WEIGHT = 20;
    /** A card in the graveyard counts for this part of its weight: it has to be brought back first. */
    static final int GRAVEYARD_PERCENT = 40;
    /** Two key cards in view are more than the sum of them: the combo is half built. */
    static final int SYNERGY_PERCENT = 150;
    /** The most the term takes off, so that it never outweighs a lost game. */
    static final int CAP = 300;
    /** Added to the rank of a target that is a key card, above any creature's evaluation. */
    static final int RANK_BONUS = 10000;

    public static boolean isKey(final Card c) {
        return c != null && (AiCardLists.has(AiCardLists.Role.ANTI_COMBO_KEY, c.getName())
                || AiCardLists.has(AiCardLists.Role.ANTI_COMBO_SEEN, c.getName()));
    }

    /** The weight of a card: a key, a piece or nothing. */
    public static int weight(final Card c) {
        if (c == null) {
            return 0;
        }
        if (isKey(c)) {
            return KEY_WEIGHT;
        }
        return AiCardLists.has(AiCardLists.Role.ANTI_COMBO_PIECE, c.getName()) || ComboCatalog.isPiece(c) ? PIECE_WEIGHT : 0;
    }

    /** The rank a target gets for being dangerous: only a card the opponents of the AI control. */
    public static int rank(final Player ai, final Card c) {
        if (ai == null || c == null || c.getController() == null || !c.getController().isOpponentOf(ai)) {
            return 0;
        }
        if (DonateCombo.isGiftedTarget(ai, c)) {
            return 0; // the AI deliberately gave this harmful permanent to its opponent
        }
        // a card that fills a slot of a known combo (ComboCatalog) is aimed at as well, more so when it completes one
        return Math.max(weight(c) * RANK_BONUS / KEY_WEIGHT, ComboCatalog.rankPercent(ai, c) * RANK_BONUS / 100);
    }

    /** The visible threat of the opponents, 0 when none. */
    public static int threat(final Player ai) {
        if (ai == null) {
            return 0;
        }
        int total = 0;
        int keys = 0;
        for (final Player opp : ai.getOpponents()) {
            for (final Card c : opp.getCardsIn(ZoneType.Battlefield)) {
                if (DonateCombo.isGiftedTarget(ai, c)) {
                    continue; // an AI-owned DonateMe permanent is the AI's payoff, not an opposing threat
                }
                total += weight(c);
                keys += isKey(c) ? 1 : 0;
            }
            for (final Card c : opp.getCardsIn(ZoneType.Graveyard)) {
                total += weight(c) * GRAVEYARD_PERCENT / 100;
            }
        }
        for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
            final Card host = si.getSourceCard();
            if (host != null && si.getActivatingPlayer() != null && si.getActivatingPlayer().isOpponentOf(ai)) {
                total += weight(host);
                keys += isKey(host) ? 1 : 0;
            }
        }
        if (keys >= 2) {
            total = total * SYNERGY_PERCENT / 100;
        }
        total += ComboCatalog.visibleThreat(ai); // every slot of a known combo in view
        return Math.min(total, CAP);
    }

    /** The term of the evaluation: minus the threat. */
    public static int term(final Player ai) {
        return -threat(ai);
    }

    /** The cards that make up the threat, the most dangerous first (battlefield and stack before the graveyard). */
    public static List<Card> threats(final Player ai) {
        final List<Card> result = new ArrayList<>();
        for (final Player opp : ai.getOpponents()) {
            for (final ZoneType zone : new ZoneType[] { ZoneType.Battlefield, ZoneType.Graveyard }) {
                for (final Card c : opp.getCardsIn(zone)) {
                    if (DonateCombo.isGiftedTarget(ai, c)) {
                        continue;
                    }
                    if (weight(c) > 0) {
                        result.add(c);
                    }
                }
            }
        }
        result.sort(Comparator.comparingInt((Card c) -> -weight(c)));
        return result;
    }

    /** For a failure message: what the AI sees of the opponents' combos. */
    public static String describe(final Player ai) {
        return "threat " + threat(ai) + " from " + threats(ai) + ", combos in view " + ComboCatalog.visibleCombos(ai);
    }
}
