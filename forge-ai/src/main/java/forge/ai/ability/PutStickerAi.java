package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.ability.effects.PutStickerEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerSheet;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Chooses both the object and the sticker by scoring the same (sticker, object) pairs.
 */
public class PutStickerAi extends SpellAbilityAi {

    // in the units of ComputerUtilCard.evaluateCreature
    private static final int POWER_VALUE = 15;
    private static final int TOUGHNESS_VALUE = 10;
    private static final int ABILITY_VALUE = 30;
    private static final int NONCREATURE_VALUE = 4;
    private static final int MARKER_VALUE = 5;
    private static final int TICKET_VALUE = 2;

    private static int score(Sticker sticker, Card target, boolean free) {
        int value = switch (sticker.getKind()) {
            // CR 123.8 - sets rather than adds, so a small one on a big creature is a downgrade
            case PT -> !target.isCreature() ? 0
                    : (sticker.getPower() - target.getCurrentPower()) * POWER_VALUE
                            + (sticker.getToughness() - target.getCurrentToughness()) * TOUGHNESS_VALUE;
            case ABILITY -> target.isCreature() ? ABILITY_VALUE : NONCREATURE_VALUE;
            case NAME -> MARKER_VALUE + sticker.getUniqueVowelCount()
                    + (target.stickerWouldFillBlank() ? MARKER_VALUE : 0);
            case ART -> MARKER_VALUE;
        };
        return free ? value : value - sticker.getTickets() * TICKET_VALUE;
    }

    public static Sticker chooseSticker(List<Sticker> options, Card target, SpellAbility sa, boolean isOptional) {
        final boolean free = sa != null && sa.hasParam("NoTicketCost");
        Sticker best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Sticker s : options) {
            int value = score(s, target, free);
            if (value > bestScore) {
                bestScore = value;
                best = s;
            }
        }
        return isOptional && bestScore <= 0 ? null : best;
    }

    private static Card bestTarget(Player ai, SpellAbility sa, Iterable<Card> pool) {
        final List<Sticker> options = PutStickerEffect.availableStickers(sa, ai);
        final boolean free = sa.hasParam("NoTicketCost");
        Card best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Card c : pool) {
            int value = Integer.MIN_VALUE;
            for (Sticker s : options) {
                value = Math.max(value, score(s, c, free));
            }
            if (best == null || value > bestScore || (value == bestScore && preferred(c, best))) {
                bestScore = value;
                best = c;
            }
        }
        return best;
    }

    private static boolean preferred(Card candidate, Card best) {
        if (candidate.isCreature() != best.isCreature()) {
            return candidate.isCreature();
        }
        if (candidate.isStickered() != best.isStickered()) {
            return !candidate.isStickered();
        }
        if (!candidate.isCreature()) {
            return candidate.getCMC() > best.getCMC();
        }
        return ComputerUtilCard.evaluateCreature(candidate) > ComputerUtilCard.evaluateCreature(best);
    }

    private static boolean hasSomethingToSticker(Player ai, SpellAbility sa) {
        if (!sa.hasParam("Choices")) {
            return true;
        }
        ZoneType zone = sa.hasParam("ChoiceZone")
                ? ZoneType.smartValueOf(sa.getParam("ChoiceZone")) : ZoneType.Battlefield;
        // CR 123.3b
        return !CardLists.filter(CardLists.getValidCards(ai.getGame().getCardsIn(zone),
                sa.getParam("Choices"), ai, sa.getHostCard(), sa),
                CardPredicates.isOwner(ai)).isEmpty();
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional,
            Player targetedPlayer, Map<String, Object> params) {
        return bestTarget(ai, sa, options);
    }

    // a sticker is permanent, so one that costs mana can wait for main 2
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        return sa.getPayCosts() == null || !sa.getPayCosts().hasManaCost()
                || !ph.isPlayerTurn(ai) || !ph.getPhase().isBefore(PhaseType.MAIN2);
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!StickerSheet.hasAvailableSticker(ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (!hasSomethingToSticker(ai, sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message,
            Map<String, Object> params) {
        return StickerSheet.hasAvailableSticker(player);
    }
}
