package forge.ai.ability;

import com.google.common.collect.Iterables;

import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public abstract class DamageAiBase extends SpellAbilityAi {
    protected boolean shouldTgtP(final Player comp, final SpellAbility sa, final int d, final boolean noPrevention) {
        int restDamage = d;
        final Game game = comp.getGame();
        final Player enemy = comp.getOpponent();
        if (!sa.canTarget(enemy)) {
            return false;
        }
        if (sa.getTargets() != null && sa.getTargets().getTargets().contains(enemy)) {
            return false;
        }
        
        // burn Planeswalkers
        if (Iterables.any(enemy.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS)) {
            return true;
        }

        if (!noPrevention) {
            restDamage = ComputerUtilCombat.predictDamageTo(enemy, restDamage, sa.getHostCard(), false);
        } else {
            restDamage = enemy.staticReplaceDamage(restDamage, sa.getHostCard(), false);
        }

        if (restDamage == 0) {
            return false;
        }

        if (!enemy.canLoseLife()) {
            return false;
        }

        final CardCollectionView hand = comp.getCardsIn(ZoneType.Hand);

        if ((enemy.getLife() - restDamage) < 5) {
            // drop the human to less than 5
            // life
            return true;
        }

        if (sa.isSpell()) {
            PhaseHandler phase = game.getPhaseHandler();
            // If this is a spell, cast it instead of discarding
            if ((phase.is(PhaseType.END_OF_TURN) || phase.is(PhaseType.MAIN2))
                    && phase.isPlayerTurn(comp) && (hand.size() > comp.getMaxHandSize())) {
                return true;
            }
            
            // chance to burn player based on current hand size
            if (hand.size() > 2) {
                float value = 0;
                if (SpellAbilityAi.isSorcerySpeed(sa)) {
                    //lower chance for sorcery as other spells may be cast in main2
                    if (phase.isPlayerTurn(comp) && phase.is(PhaseType.MAIN2)) {
                        value = 1.0f * restDamage / enemy.getLife();
                    }
                } else {
                    if (phase.isPlayerTurn(enemy) && phase.is(PhaseType.END_OF_TURN)) {
                        value = 1.5f * restDamage / enemy.getLife();
                    }
                }
                if (value > 0) {    //more likely to burn with larger hand
                    for (int i = 3; i < hand.size(); i++) {
                        value *= 1.1f;
                    }
                }
                if (value < 0.2f) {   //hard floor to reduce ridiculous odds for instants over time
                    return false;
                } else {
                    final float chance = MyRandom.getRandom().nextFloat();
                    return chance < value;
                }
            }
        }

        return false;
    }
}
