package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.ability.ApiType;
import forge.card.CardType;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.GameLossReason;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Collection;
import java.util.Map;

/** DanDan-only text choices for Magical Hack and Crystal Spray. */
public class ChangeTextAi extends SpellAbilityAi {
    private static final String MAGICAL_HACK = "Magical Hack";
    private static final String DANDAN = "Dandân";
    private static final String ORIGINAL_ISLAND = "Island";

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        if (!MAGICAL_HACK.equals(sa.getHostCard().getName()) || !ai.getGame().getRules().isDanDan()) {
            return cantPlay();
        }
        return chooseHelpfulTarget(ai, sa)
                ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : cantPlay();
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        if (!isCrystalSprayTypeChoice(ai, sa)) {
            return cantPlay();
        }
        return hasHelpfulTarget(ai, sa)
                ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : cantPlay();
    }

    private static boolean isCrystalSprayTypeChoice(Player ai, SpellAbility sa) {
        if (!ai.getGame().getRules().isDanDan()) {
            return false;
        }
        SpellAbility root = sa.getRootAbility();
        if (root == null || !"Crystal Spray".equals(root.getHostCard().getName())
                || root.getApi() != ApiType.GenericChoice) {
            return false;
        }
        for (SpellAbility current = sa; current != null; current = current.getParent()) {
            if (current.hasParam("ChangeTypeWord")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasHelpfulTarget(Player ai, SpellAbility sa) {
        if (!ai.getGame().getRules().isDanDan()) {
            return false;
        }
        Card selectedTarget = getTargetCard(sa);
        if (selectedTarget != null) {
            return isHelpfulTarget(ai, selectedTarget);
        }
        if (sa.getTargetRestrictions() == null) {
            return false;
        }
        for (GameEntity candidate : sa.getTargetRestrictions().getAllCandidates(sa)) {
            if (candidate instanceof Card card && isHelpfulTarget(ai, card)) {
                return true;
            }
        }
        return false;
    }

    /** Decline only a mandatory cantrip draw that would actually lose the game. */
    public static boolean canCastCrystalSpray(Player ai, SpellAbility sa) {
        for (AbilitySub choice : sa.getAdditionalAbilityList("Choices")) {
            if (!choice.hasParam("ChangeTypeWord")) {
                continue;
            }
            SpellAbility draw = choice.findSubAbilityByType(ApiType.Draw);
            return draw != null && (!ai.canDraw() || !ai.getCardsIn(ZoneType.Library).isEmpty()
                    || ai.cantLoseCheck(GameLossReason.Milled));
        }
        return false;
    }

    /** Selects only a target whose public current text can strand an opponent's fish. */
    public static boolean chooseHelpfulTarget(Player ai, SpellAbility sa) {
        sa.resetTargets();
        if (!ai.getGame().getRules().isDanDan() || sa.getTargetRestrictions() == null) {
            return false;
        }

        for (GameEntity candidate : sa.getTargetRestrictions().getAllCandidates(sa)) {
            if (candidate instanceof Card card && isHelpfulTarget(ai, card)) {
                sa.getTargets().add(card);
                return true;
            }
        }
        return false;
    }

    private static boolean isHelpfulTarget(Player ai, Card card) {
        if (!card.isInPlay() || !DANDAN.equals(card.getName())
                || !card.getController().isOpponentOf(ai)) {
            return false;
        }

        String currentRequirement = currentIslandRequirement(card);
        if (!CardType.getBasicTypes().contains(currentRequirement)
                || !controlsBasicLandType(card.getController(), currentRequirement)) {
            return false;
        }

        for (String type : CardType.getBasicTypes()) {
            if (!currentRequirement.equals(type) && !controlsBasicLandType(card.getController(), type)) {
                return true;
            }
        }
        return false;
    }

    /** The first callback chooses the current word; the second chooses a land type the target lacks. */
    public static String chooseBasicLandTypeWord(Player ai, SpellAbility sa, Collection<String> validTypes) {
        if (validTypes == null || validTypes.isEmpty()) {
            return "";
        }
        Card target = getTargetCard(sa);
        if (target == null || !isHelpfulTarget(ai, target)) {
            return validTypes.iterator().next();
        }

        String currentRequirement = currentIslandRequirement(target);
        if (validTypes.contains(currentRequirement)) {
            return currentRequirement;
        }
        for (String type : validTypes) {
            if (!currentRequirement.equals(type) && !controlsBasicLandType(target.getController(), type)) {
                return type;
            }
        }
        return validTypes.iterator().next();
    }

    private static Card getTargetCard(SpellAbility sa) {
        for (SpellAbility current = sa; current != null; current = current.getParent()) {
            Card target = current.getTargetCard();
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private static String currentIslandRequirement(Card card) {
        Map<String, String> changedTypes = card.getChangedTextTypeWords();
        return changedTypes.getOrDefault(ORIGINAL_ISLAND, ORIGINAL_ISLAND);
    }

    private static boolean controlsBasicLandType(Player player, String type) {
        return player.getCardsIn(ZoneType.Battlefield).stream()
                .anyMatch(card -> card.getType().hasSubtype(type));
    }

    private static AiAbilityDecision cantPlay() {
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
