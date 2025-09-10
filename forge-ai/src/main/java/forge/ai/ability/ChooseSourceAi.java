package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.*;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ChooseSourceAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, SpellAbility sa) {
        // TODO: AI Support! Currently this is copied from AF ChooseCard.
        //       When implementing AI, I believe AI also needs to be made aware of the damage sources chosen
        //       to be prevented (e.g. so the AI doesn't attack with a creature that will not deal any damage
        //       to the player because a CoP was pre-activated on it - unless, of course, there's another
        //       possible reason to attack with that creature).
        final Card host = sa.getHostCard();

        if (sa.usesTargeting()) {
            sa.resetTargets();
            Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
            if (sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }
        if (sa.hasParam("AILogic")) {
            final Game game = ai.getGame();
            if (sa.getParam("AILogic").equals("NeedsPrevention")) {
                if (!game.getStack().isEmpty()) {
                    final SpellAbility topStack = game.getStack().peekAbility();
                    if (sa.hasParam("Choices") && !topStack.matchesValid(topStack.getHostCard(), sa.getParam("Choices").split(","))) {
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    }
                    final ApiType threatApi = topStack.getApi();
                    if (threatApi != ApiType.DealDamage && threatApi != ApiType.DamageAll) {
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    }

                    final Card threatSource = topStack.getHostCard();
                    List<? extends GameObject> objects;
                    if (!topStack.usesTargeting() && topStack.hasParam("ValidPlayers") && !topStack.hasParam("Defined")) {
                        objects = AbilityUtils.getDefinedPlayers(threatSource, topStack.getParam("ValidPlayers"), topStack);
                    } else {
                        objects = getTargets(topStack);
                    }

                    if (!objects.contains(ai) || topStack.hasParam("NoPrevention")) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                    int dmg = AbilityUtils.calculateAmount(threatSource, topStack.getParam("NumDmg"), topStack);
                    if (ComputerUtilCombat.predictDamageTo(ai, dmg, threatSource, false) > 0) {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    } else {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
                if (game.getPhaseHandler().getPhase() != PhaseType.COMBAT_DECLARE_BLOCKERS) {
                    return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
                }
                CardCollectionView choices = game.getCardsIn(ZoneType.Battlefield);
                if (sa.hasParam("Choices")) {
                    choices = CardLists.getValidCards(choices, sa.getParam("Choices"), host.getController(), host, sa);
                }
                final Combat combat = game.getCombat();
                choices = CardLists.filter(choices, c -> {
                    if (combat == null || !combat.isAttacking(c, ai) || !combat.isUnblocked(c)) {
                        return false;
                    }
                    return ComputerUtilCombat.damageIfUnblocked(c, ai, combat, true) > 0;
                });
                if (choices.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
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

            List<Card> permanentSources = CardLists.filter(options, c -> {
                if (c == null || c.getZone() == null || c.getZone().getZoneType() != ZoneType.Battlefield
                        || combat == null || !combat.isAttacking(c, ai) || !combat.isUnblocked(c)) {
                    return false;
                }
                return ComputerUtilCombat.damageIfUnblocked(c, ai, combat, true) > 0;
            });

            // Try to choose the best creature for damage prevention.
            Card bestCreature = ComputerUtilCard.getBestCreatureAI(permanentSources);
            if (bestCreature != null) {
                return bestCreature;
            }
            // No optimal creature was found above, so try to broaden the choice.
            if (!Iterables.isEmpty(options)) {
                List<Card> oppCreatures = CardLists.filter(options, Predicate.not(
                        CardPredicates.CREATURES.and(CardPredicates.isOwner(aiChoser))
                ));
                List<Card> aiNonCreatures = CardLists.filter(options,
                        CardPredicates.NON_CREATURES
                                .and(CardPredicates.PERMANENTS)
                                .and(CardPredicates.isOwner(aiChoser))
                );

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
            final SpellAbility abilityOnStack = si.getSpellAbility();

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
                ? sa.getTargets()
                : AbilityUtils.getDefinedObjects(sa.getHostCard(), sa.getParam("Defined"), sa);
    }
}
