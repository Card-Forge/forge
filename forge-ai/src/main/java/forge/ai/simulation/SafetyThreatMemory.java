package forge.ai.simulation;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

import java.util.HashMap;
import java.util.Map;

public class SafetyThreatMemory {
    private static final int SAFETY_THREAT_BONUS = 150;

    private final Player player;
    private final Map<Integer, SafetyThreatFact> facts = new HashMap<>();

    private static class SafetyThreatFact {
        final String name;
        final String unsafeAction;
        final long gameTimestamp;
        final int gameId;

        SafetyThreatFact(Card card, SpellAbility unsafeAction) {
            this.name = card.getName();
            this.unsafeAction = unsafeAction.getHostCard().getName();
            this.gameTimestamp = card.getGameTimestamp();
            this.gameId = card.getGame().getId();
        }

        boolean matches(Card card) {
            return gameId == card.getGame().getId()
                    && gameTimestamp == card.getGameTimestamp()
                    && name.equals(card.getName())
                    && card.isInPlay();
        }
    }

    public SafetyThreatMemory(Player player) {
        this.player = player;
    }

    public void rememberSuspectsForUnsafeAction(SpellAbility unsafeAction) {
        for (Card card : player.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (!card.getController().isOpponentOf(player)) {
                continue;
            }
            if (isPotentialSafetyThreat(card)) {
                facts.put(card.getId(), new SafetyThreatFact(card, unsafeAction));
            }
        }
    }

    public int getThreatAssessmentBonus(Card card) {
        SafetyThreatFact fact = facts.get(card.getId());
        if (fact == null) {
            return 0;
        }
        if (!fact.matches(card)) {
            facts.remove(card.getId());
            return 0;
        }
        return SAFETY_THREAT_BONUS;
    }

    public static boolean isPotentialSafetyThreat(Card card) {
        return hasRelevantTrigger(card)
                || hasRelevantReplacementEffect(card)
                || hasRelevantStaticAbility(card);
    }

    private static boolean hasRelevantTrigger(Card card) {
        for (Trigger trigger : card.getTriggers()) {
            if (!trigger.isIntrinsic()) {
                continue;
            }
            SpellAbility ability = trigger.ensureAbility();
            if (ability == null || ability.getApi() != ApiType.Mana) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRelevantReplacementEffect(Card card) {
        for (ReplacementEffect replacementEffect : card.getReplacementEffects()) {
            if (replacementEffect.isIntrinsic()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRelevantStaticAbility(Card card) {
        if (card.isCreature()) {
            return false;
        }
        for (StaticAbility staticAbility : card.getStaticAbilities()) {
            if (staticAbility.isIntrinsic()) {
                return true;
            }
        }
        return false;
    }
}
