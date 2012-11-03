package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class  DamageAllAi extends SpellAiLogic
{
    
    /**
     * <p>
     * damageAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        String validP = "";

        final String damage = params.get("NumDmg");
        int dmg = AbilityFactory.calculateAmount(sa.getSourceCard(), damage, sa); 


        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }

        if (params.containsKey("ValidPlayers")) {
            validP = params.get("ValidPlayers");
        }

        Player opp = ai.getOpponent();
        
        final List<Card> humanList = this.getKillableCreatures(params, sa.getSourceCard(), opp, dmg);
        List<Card> computerList = this.getKillableCreatures(params, sa.getSourceCard(), opp, dmg);

        
        final Target tgt = sa.getTarget();
        if (tgt != null && sa.canTarget(opp)) {
            tgt.resetTargets();
            sa.getTarget().addTarget(opp);
            computerList = new ArrayList<Card>();
        }

        // abCost stuff that should probably be centralized...
        if (abCost != null) {
            // AI currently disabled for some costs
            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }

        // TODO: if damage is dependant on mana paid, maybe have X be human's max life
        // Don't kill yourself
        if (validP.contains("Each") && (ai.getLife() <= ai.predictDamage(dmg, source, false))) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (r.nextFloat() > Math.pow(.9, sa.getActivationsThisTurn())) {
            return false;
        }

        // if we can kill human, do it
        if ((validP.contains("Each") || validP.contains("EachOpponent"))
                && (opp.getLife() <= opp.predictDamage(dmg, source, false))) {
            return true;
        }

        // wait until stack is empty (prevents duplicate kills)
        if (!Singletons.getModel().getGame().getStack().isEmpty()) {
            return false;
        }

        int minGain = 200; // The minimum gain in destroyed creatures
        if (sa.getPayCosts().isReusuableResource()) {
            minGain = 100;
        }

        // evaluate both lists and pass only if human creatures are more valuable
        if ((CardFactoryUtil.evaluateCreatureList(computerList) + minGain) >= CardFactoryUtil
                .evaluateCreatureList(humanList)) {
            return false;
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        // check AI life before playing this drawback?
        return true;
    }

    /**
     * <p>
     * getKillableCreatures.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param dmg
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    private List<Card> getKillableCreatures(final Map<String, String> params, final Card source, final Player player, final int dmg) {
        String validC = params.containsKey("ValidCards") ? params.get("ValidCards") : ""; 

        // TODO: X may be something different than X paid
        List<Card> list = 
                CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), validC.split(","), source.getController(), source);

        final Predicate<Card> filterKillable = new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return (c.predictDamage(dmg, source, false) >= c.getKillDamage());
            }
        };

        list = CardLists.getNotKeyword(list, "Indestructible");
        list = CardLists.filter(list, filterKillable);

        return list;
    }

    /**
     * <p>
     * damageAllDoTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    @Override
    public boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getSourceCard();
        String validP = "";

        final String damage = params.get("NumDmg");
        int dmg = AbilityFactory.calculateAmount(sa.getSourceCard(), damage, sa); 

        
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        } 
        
        if (params.containsKey("ValidPlayers")) {
            validP = params.get("ValidPlayers");
        }

        Player enemy = ai.getOpponent();
        final Target tgt = sa.getTarget();
        do { // A little trick to still check the SubAbilities, once we know we
             // want to play it
            if (tgt == null) {
                // If it's not mandatory check a few things
                if (mandatory) {
                    return true;
                } else {
                    // Don't get yourself killed
                    if (validP.contains("Each")
                            && (ai.getLife() <= ai.predictDamage(dmg,
                                    source, false))) {
                        return false;
                    }

                    // if we can kill human, do it
                    if ((validP.contains("Each") || validP.contains("EachOpponent") || validP.contains("Targeted"))
                            && (enemy.getLife() <= enemy.predictDamage(dmg,
                                    source, false))) {
                        break;
                    }

                    // Evaluate creatures getting killed
                    final List<Card> humanList = this.getKillableCreatures(params, sa.getSourceCard(), enemy, dmg);
                    final List<Card> computerList = this.getKillableCreatures(params, sa.getSourceCard(), ai, dmg);
                    if ((CardFactoryUtil.evaluateCreatureList(computerList) + 50) >= CardFactoryUtil
                            .evaluateCreatureList(humanList)) {
                        return false;
                    }
                }
            } else {
                // DamageAll doesn't really target right now
            }
        } while (false);

        if (sa.getSubAbility() != null) {
            return sa.getSubAbility().doTrigger(mandatory);
        }

        return true;
    }
}