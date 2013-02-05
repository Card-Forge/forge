package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtil;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class PumpAllAi extends PumpAiBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(final AIPlayer ai, final SpellAbility sa) {
        String valid = "";
        final Card source = sa.getSourceCard();

        final int power = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("NumAtt"), sa);
        final int defense = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("NumDef"), sa);
        final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & ")) : new ArrayList<String>();

        final PhaseType phase = Singletons.getModel().getGame().getPhaseHandler().getPhase();

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        final Player opp = ai.getOpponent();
        List<Card> comp = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, source.getController(), source);
        List<Card> human = CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), valid, source.getController(), source);

        final Target tgt = sa.getTarget();
        if (tgt != null && sa.canTarget(opp) && sa.hasParam("IsCurse")) {
            tgt.resetTargets();
            sa.getTarget().addTarget(opp);
            comp = new ArrayList<Card>();
        }

        if (sa.hasParam("IsCurse")) {
            if (defense < 0) { // try to destroy creatures
                comp = CardLists.filter(comp, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (c.getNetDefense() <= -defense) {
                            return true; // can kill indestructible creatures
                        }
                        return ((c.getKillDamage() <= -defense) && !c.hasKeyword("Indestructible"));
                    }
                }); // leaves all creatures that will be destroyed
                human = CardLists.filter(human, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (c.getNetDefense() <= -defense) {
                            return true; // can kill indestructible creatures
                        }
                        return ((c.getKillDamage() <= -defense) && !c.hasKeyword("Indestructible"));
                    }
                }); // leaves all creatures that will be destroyed
            } // -X/-X end
            else if (power < 0) { // -X/-0
                if (phase.isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                        || phase.isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                        || Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())
                        || Singletons.getModel().getGame().getPhaseHandler().isPreventCombatDamageThisTurn()) {
                    return false;
                }
                int totalPower = 0;
                for (Card c : human) {
                    if (!c.isAttacking()) {
                        continue;
                    }
                    totalPower += Math.min(c.getNetAttack(), power * -1);
                    if (phase == PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY
                            && Singletons.getModel().getGame().getCombat().getUnblockedAttackers().contains(c)) {
                        if (ComputerUtilCombat.lifeInDanger(sa.getActivatingPlayer(), Singletons.getModel().getGame().getCombat())) {
                            return true;
                        }
                        totalPower += Math.min(c.getNetAttack(), power * -1);
                    }
                    if (totalPower >= power * -2) {
                        return true;
                    }
                }
                return false;
            } // -X/-0 end

            // evaluate both lists and pass only if human creatures are more
            // valuable
            if ((CardFactoryUtil.evaluateCreatureList(comp) + 200) >= CardFactoryUtil.evaluateCreatureList(human)) {
                return false;
            }
            return true;
        } // end Curse

        // don't use non curse PumpAll after Combat_Begin until AI is improved
        if (phase.isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)) {
            return false;
        }

        // only count creatures that can attack
        comp = CardLists.filter(comp, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (power <= 0 && !containsUsefulKeyword(ai, keywords, c, sa, power)) {
                    return false;
                }
                if (phase.equals(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY) && c.isAttacking()) {
                    return true;
                }
                if (phase.isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && CombatUtil.canAttack(c, opp)) {
                    return true;
                }
                return false;
            }
        });

        if ((comp.size() <= human.size()) || (comp.size() <= 1)) {
            return false;
        }

        return true;
    } // pumpAllCanPlayAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer aiPlayer) {
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }

}
