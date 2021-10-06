package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.AiAttackController;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

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
            if (!willPayCosts(ai, sa, abCost, source)) {
                return false;
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
            if (sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else {
                return false;
            }
        }
        if (sa.hasParam("AILogic")) {
            final Game game = ai.getGame();
            if (sa.getParam("AILogic").equals("NeedsPrevention")) {
                if (!game.getStack().isEmpty()) {
                    final SpellAbility topStack = game.getStack().peekAbility();
                    if (sa.hasParam("Choices") && !topStack.matchesValid(topStack.getHostCard(), sa.getParam("Choices").split(","))) {
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
                    return ComputerUtilCombat.predictDamageTo(ai, dmg, threatSource, false) > 0;
                }
                if (game.getPhaseHandler().getPhase() != PhaseType.COMBAT_DECLARE_BLOCKERS) {
                    return false;
                }
                CardCollectionView choices = game.getCardsIn(ZoneType.Battlefield);
                if (sa.hasParam("Choices")) {
                    choices = CardLists.getValidCards(choices, sa.getParam("Choices"), host.getController(), host, sa);
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
                return !choices.isEmpty();
            }
        }

        return true;
    }

    @Override
    public Card chooseSingleCard(final Player aiChoser, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        if ("NeedsPrevention".equals(sa.getParam("AILogic"))) {
            final Player ai = sa.getActivatingPlayer();
            final Game game = ai.getGame();
            if (!game.getStack().isEmpty()) {
                Card chosenCard = chooseCardOnStack(sa, ai, game);
                if (chosenCard != null) {
                    return chosenCard;
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

            // Try to choose the best creature for damage prevention.
            Card bestCreature = ComputerUtilCard.getBestCreatureAI(permanentSources);
            if (bestCreature != null) {
                return bestCreature;
            }
            // No optimal creature was found above, so try to broaden the choice.
            if (!Iterables.isEmpty(options)) {
                List<Card> oppCreatures = CardLists.filter(options, Predicates.and(CardPredicates.Presets.CREATURES,
                        Predicates.not(CardPredicates.isOwner(aiChoser))));
                List<Card> aiNonCreatures = CardLists.filter(options, Predicates.and(Predicates.not(CardPredicates.Presets.CREATURES),
                        CardPredicates.Presets.PERMANENTS, CardPredicates.isOwner(aiChoser)));

                if (!oppCreatures.isEmpty()) {
                    return ComputerUtilCard.getBestCreatureAI(oppCreatures);
                } else if (!aiNonCreatures.isEmpty()) {
                    return Aggregates.random(aiNonCreatures);
                } else {
                    return Aggregates.random(options);
                }
            } else if (!game.getStack().isEmpty()) {
                // No permanent for the AI to choose. Should normally not happen unless using dev mode or something,
                // but when it does happen, choose the top card on stack if possible (generally it'll be the SA
                // source) in order to choose at least something, or the game will hang.
                return game.getStack().peekAbility().getHostCard();
            }

            // Should never get here
            System.err.println("Unexpected behavior: The AI was unable to choose anything for AF ChooseSource in "
                    + sa.getHostCard() + ", the game will likely hang.");
            return null;
        } else {
            return ComputerUtilCard.getBestAI(options);
        }
    }

    private Card chooseCardOnStack(SpellAbility sa, Player ai, Game game) {
        for (SpellAbilityStackInstance si : game.getStack()) {
            final Card source = si.getSourceCard();
            final SpellAbility abilityOnStack = si.getSpellAbility(true);

            if (sa.hasParam("Choices") && !abilityOnStack.matchesValid(source, sa.getParam("Choices").split(","))) {
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
                ? Lists.newArrayList(sa.getTargets())
                : AbilityUtils.getDefinedObjects(sa.getHostCard(), sa.getParam("Defined"), sa);
    }
}
