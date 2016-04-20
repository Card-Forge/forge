package forge.ai.ability;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class ChooseSourceAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(final Player ai, SpellAbility sa) {
        // TODO: AI Support! Currently this is copied from AF ChooseCard.
        //       When implementing AI, I believe AI also needs to be made aware of the damage sources chosen
        //       to be prevented (e.g. so the AI doesn't attack with a creature that will not deal any damage
        //       to the player because a CoP was pre-activated on it - unless, of course, there's another
        //       possible reason to attack with that creature).
        final Card host = sa.getHostCard();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (sa.canTarget(ai.getOpponent())) {
                sa.getTargets().add(ai.getOpponent());
            } else {
                return false;
            }
        }
        if (sa.hasParam("AILogic")) {
            final Game game = ai.getGame();
            if (sa.getParam("AILogic").equals("NeedsPrevention")) {
                if (!game.getStack().isEmpty()) {
                    final SpellAbility topStack = game.getStack().peekAbility();
                    if (sa.hasParam("Choices") && !topStack.getHostCard().isValid(sa.getParam("Choices"), ai, source, sa)) {
                        return false;
                    }
                    final ApiType threatApi = topStack.getApi();
                    if (threatApi != ApiType.DealDamage && threatApi != ApiType.DamageAll) {
                        return false;
                    }

                    final Card threatSource = topStack.getHostCard();
                    List<? extends GameObject> objects = getTargets(topStack);
                    if (!topStack.usesTargeting() && topStack.hasParam("ValidPlayers") && !topStack.hasParam("Defined")) {
                        objects = AbilityUtils.getDefinedPlayers(threatSource, topStack.getParam("ValidPlayers"), topStack);
                    }
                    
                    if (!objects.contains(ai) || topStack.hasParam("NoPrevention")) {
                        return false;
                    }
                    int dmg = AbilityUtils.calculateAmount(threatSource, topStack.getParam("NumDmg"), topStack);
                    if (ComputerUtilCombat.predictDamageTo(ai, dmg, threatSource, false) <= 0) {
                        return false;
                    }
                    return true;
                }
                if (game.getPhaseHandler().getPhase() != PhaseType.COMBAT_DECLARE_BLOCKERS) {
                    return false;
                }
                CardCollectionView choices = game.getCardsIn(ZoneType.Battlefield);
                if (sa.hasParam("Choices")) {
                    choices = CardLists.getValidCards(choices, sa.getParam("Choices"), host.getController(), host);
                }
                final Combat combat = game.getCombat();
                choices = CardLists.filter(choices, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (combat == null || !combat.isAttacking(c, ai) || !combat.isUnblocked(c)) {
                            return false;
                        }
                        return ComputerUtilCombat.damageIfUnblocked(c, ai, combat, true) > 0;
                    }
                });
                if (choices.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }
    
    
    
    @Override
    public Card chooseSingleCard(final Player aiChoser, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer) {
        if ("NeedsPrevention".equals(sa.getParam("AILogic"))) {
            final Player ai = sa.getActivatingPlayer();
            final Game game = ai.getGame();
            if (!game.getStack().isEmpty()) {
                Card choseCard = chooseCardOnStack(sa, ai, game);
                if (choseCard != null) {
                    return choseCard;
                }
            }

            final Combat combat = game.getCombat();
            
            List<Card> permanentSources = CardLists.filter(options, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (c == null || c.getZone() == null || c.getZone().getZoneType() != ZoneType.Battlefield 
                    		|| combat == null || !combat.isAttacking(c, ai) || !combat.isUnblocked(c)) {
                        return false;
                    }
                    return ComputerUtilCombat.damageIfUnblocked(c, ai, combat, true) > 0;
                }
            });
            return ComputerUtilCard.getBestCreatureAI(permanentSources);
            
        } else {
            return ComputerUtilCard.getBestAI(options);
        }
    }
    
    private Card chooseCardOnStack(SpellAbility sa, Player ai, Game game) {
        for (SpellAbilityStackInstance si : game.getStack()) {
            final Card source = si.getSourceCard();
            final SpellAbility abilityOnStack = si.getSpellAbility(true);
            
            if (sa.hasParam("Choices") && !abilityOnStack.getHostCard().isValid(sa.getParam("Choices"), ai, sa.getHostCard(), sa)) {
                continue;
            }
            final ApiType threatApi = abilityOnStack.getApi();
            if (threatApi != ApiType.DealDamage && threatApi != ApiType.DamageAll) {
                continue;
            }

            List<? extends GameObject> objects = getTargets(abilityOnStack);

            if (!abilityOnStack.usesTargeting() && !abilityOnStack.hasParam("Defined") && abilityOnStack.hasParam("ValidPlayers")) 
                objects = AbilityUtils.getDefinedPlayers(source, abilityOnStack.getParam("ValidPlayers"), abilityOnStack);
            
            if (!objects.contains(ai) || abilityOnStack.hasParam("NoPrevention")) {
                continue;
            }
            int dmg = AbilityUtils.calculateAmount(source, abilityOnStack.getParam("NumDmg"), abilityOnStack);
            if (ComputerUtilCombat.predictDamageTo(ai, dmg, source, false) <= 0) {
                continue;
            }
            return source;
        }
        return null;
    }    

    private static List<GameObject> getTargets(final SpellAbility sa) {
        return sa.usesTargeting() && (!sa.hasParam("Defined"))
                ? Lists.newArrayList(sa.getTargets().getTargets()) 
                : AbilityUtils.getDefinedObjects(sa.getHostCard(), sa.getParam("Defined"), sa);
    }
}
