package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerDamageDone;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public abstract class DamageAiBase extends SpellAbilityAi {
    protected boolean avoidTargetP(final Player comp, final SpellAbility sa) {
        Player enemy = ComputerUtil.getOpponentFor(comp);
        // Logic for cards that damage owner, like Fireslinger
        // Do not target a player if they aren't below 75% of our health.
        // Unless Lifelink will cancel the damage to us
        Card hostcard = sa.getHostCard();
        boolean lifelink = hostcard.hasKeyword("Lifelink");
        for (Card ench : hostcard.getEnchantedBy(false)) {
            // Treat cards enchanted by older cards with "when enchanted creature deals damage, gain life" as if they had lifelink.
            if (ench.hasSVar("LikeLifeLink")) {
                if ("True".equals(ench.getSVar("LikeLifeLink"))) {
                    lifelink = true;
                }
            }
        }
        if ("SelfDamage".equals(sa.getParam("AILogic"))) {
            if (comp.getLife() * 0.75 < enemy.getLife()) {
                if (!lifelink) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean shouldTgtP(final Player comp, final SpellAbility sa, final int d, final boolean noPrevention) {
        int restDamage = d;
        final Game game = comp.getGame();
        Player enemy = ComputerUtil.getOpponentFor(comp);
        boolean dmgByCardsInHand = false;

        if ("X".equals(sa.getParam("NumDmg")) && sa.getHostCard() != null && sa.hasSVar(sa.getParam("NumDmg")) &&
                sa.getHostCard().getSVar(sa.getParam("NumDmg")).equals("TargetedPlayer$CardsInHand")) {
            dmgByCardsInHand = true;
        }

        if (!sa.canTarget(enemy)) {
            return false;
        }
        if (sa.getTargets() != null && sa.getTargets().getTargets().contains(enemy)) {
            return false;
        }

        // Benefits hitting players?
        // If has triggered ability on dealing damage to an opponent, go for it!
        Card hostcard = sa.getHostCard();
        for (Trigger trig : hostcard.getTriggers()) {
            if (trig instanceof TriggerDamageDone) {
                if (("Opponent".equals(trig.getParam("ValidTarget")))
                        && (!"True".equals(trig.getParam("CombatDamage")))) {
                    return true;
                }
            }
        }

        // burn Planeswalkers
        if (Iterables.any(enemy.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS)) {
            return true;
        }

        if (avoidTargetP(comp, sa)) {
            return false;
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
                    if (phase.isPlayerTurn(enemy)) {
                        if (phase.is(PhaseType.END_OF_TURN)
                                || ((dmgByCardsInHand && phase.getPhase().isAfter(PhaseType.UPKEEP)))) {
                            value = 1.5f * restDamage / enemy.getLife();
                        }
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
