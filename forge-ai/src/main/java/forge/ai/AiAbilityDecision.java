package forge.ai;

import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public record AiAbilityDecision(int rating, AiPlayDecision decision, SpellAbility sa) {
    private static int MIN_RATING = 30;

    public AiAbilityDecision(int rating, AiPlayDecision decision) {
        this(rating, decision, null);
    }

    public boolean willingToPlay() {
        if (!decision.willingToPlay()) {
            return false;
        }
        if (rating > MIN_RATING) {
            return true;
        }
        if (sa == null) {
            return false;
        }
        // passive turns don't win games
        int boosted = rating;
        Player ai = sa.getActivatingPlayer();
        // TODO turn into proactive AI profile preference
        int actionsThisTurn = CardUtil.getThisTurnActivated("Ability.YouCtrl", sa.getHostCard(), sa, ai).size()
                + CardUtil.getThisTurnCast("Spell.YouCtrl", sa.getHostCard(), sa, ai).size();
        if (actionsThisTurn == 0) {
            PhaseHandler ph = sa.getHostCard().getGame().getPhaseHandler();
            if (ph.getNextTurn() == ai) {
                // try not to waste open mana
                boosted += 10;
            } else if (ph.getPhase() == PhaseType.MAIN2 && SpellAbilityAi.isSorcerySpeed(sa, ai)) {
                boosted = 2;
            }
            if (ph.getPhase() == PhaseType.END_OF_TURN) {
                boosted = 5;
            }
        }
        return boosted > MIN_RATING;
    }
}
