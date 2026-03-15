package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.*;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class CopyPermanentAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision checkApiLogic(Player aiPlayer, SpellAbility sa) {
        Card source = sa.getHostCard();
        PhaseHandler ph = aiPlayer.getGame().getPhaseHandler();
        String aiLogic = sa.getParamOrDefault("AILogic", "");

        if ("MomirAvatar".equals(aiLogic)) {
            return SpecialCardAi.MomirVigAvatar.consider(aiPlayer, sa);
        } else if ("MimicVat".equals(aiLogic)) {
            return SpecialCardAi.MimicVat.considerCopy(aiPlayer, sa);
        } else if ("AtEOT".equals(aiLogic)) {
            if (ph.is(PhaseType.END_OF_TURN)) {
                if (ph.getPlayerTurn() == aiPlayer) {
                    // If it's the AI's turn, it can activate at EOT
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    // If it's not the AI's turn, it can't activate at EOT
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else {
                // Not at EOT phase
                return new AiAbilityDecision(0, AiPlayDecision.WaitForEndOfTurn);
            }
        } else if ("DuplicatePerms".equals(aiLogic)) {
            final List<Card> valid = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            if (valid.size() < 2) {
                return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
            }
        }

        if (sa.hasParam("AtEOT") && !ph.is(PhaseType.MAIN1)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }

        if (sa.hasParam("Defined")) {
            // If there needs to be an imprinted card, don't activate the ability if nothing was imprinted yet (e.g. Mimic Vat)
            if (sa.getParam("Defined").equals("Imprinted.ExiledWithSource") && source.getImprintedCards().isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
            }
        }

        if (sa.isEmbalm() || sa.isEternalize()) {
            // E.g. Vizier of Many Faces: check to make sure it makes sense to make the token now
            AiPlayDecision decision = ComputerUtilCard.checkNeedsToPlayReqs(sa.getHostCard(), sa);

            if (decision != AiPlayDecision.WillPlay) {
                return new AiAbilityDecision(0, decision);
            }
        }

        if (sa.costHasManaX() && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value. (Osgir)
            final int xPay = ComputerUtilCost.getMaxXValue(sa, aiPlayer, sa.isTrigger());

            sa.setXManaCostPaid(xPay);
        }

        if (sa.usesTargeting() && sa.hasParam("TargetingPlayer")) {
            sa.resetTargets();
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            if (CardLists.getTargetableCards(aiPlayer.getGame().getCardsIn(sa.getTargetRestrictions().getZone()), sa).isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else if (sa.usesTargeting() && sa.getTargetRestrictions().canTgtPlayer()) {
                if (!sa.isCurse()) {
                    if (sa.canTarget(aiPlayer)) {
                        sa.getTargets().add(aiPlayer);
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    } else {
                        for (Player p : aiPlayer.getYourTeam()) {
                            if (sa.canTarget(p)) {
                                sa.getTargets().add(p);
                                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                            }
                        }
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    }
                } else {
                    for (Player p : aiPlayer.getOpponents()) {
                        if (sa.canTarget(p)) {
                            sa.getTargets().add(p);
                            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                        }
                    }
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
        } else {
            return doTriggerNoCost(aiPlayer, sa, false);
        }
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = host.getGame();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        final boolean canCopyLegendary = sa.hasParam("NonLegendary");

        if (sa.usesTargeting()) {
            sa.resetTargets();

            List<Card> list = CardUtil.getValidCardsToTarget(sa);

            if (aiLogic.equals("Different")) {
                // TODO: possibly improve the check, currently only checks if the name is the same
                // Possibly also check if the card is threatened, and then allow to copy (this will, however, require a bit
                // of a rewrite in canPlayAI to allow a response form of CopyPermanentAi)
                Predicate<Card> nameEquals = CardPredicates.nameEquals(host.getName());
                list = CardLists.filter(list, nameEquals.negate());
            }

            //Nothing to target
            if (list.isEmpty()) {
            	return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }

            CardCollection betterList = CardLists.filter(list, CardPredicates.isRemAIDeck().negate());
            if (betterList.isEmpty()) {
                if (!mandatory) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else {
                list = betterList;
            }

            // Saheeli Rai + Felidar Guardian combo support
            if ("Saheeli Rai".equals(sourceName)) {
                CardCollection felidarGuardian = CardLists.filter(list, CardPredicates.nameEquals("Felidar Guardian"));
                if (felidarGuardian.size() > 0) {
                    // can copy a Felidar Guardian and combo off, so let's do it
                    sa.getTargets().add(felidarGuardian.get(0));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }

            // target loop
            while (sa.canAddMoreTarget()) {
                list = CardLists.canSubsequentlyTarget(list, sa);

                if (list.isEmpty()) {
                    if (!sa.isTargetNumberValid() || sa.getTargets().isEmpty()) {
                        sa.resetTargets();
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                list = CardLists.filter(list, c -> (!c.getType().isLegendary() || canCopyLegendary) || !c.getController().equals(aiPlayer));
                Card choice;
                if (list.stream().anyMatch(CardPredicates.CREATURES)) {
                    if (sa.hasParam("TargetingPlayer")) {
                        choice = ComputerUtilCard.getWorstCreatureAI(list);
                    } else {
                        choice = ComputerUtilCard.getBestCreatureAI(list);
                    }
                } else {
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(list);
                }

                if (choice == null) { // can't find anything left
                    if (!sa.isTargetNumberValid() || sa.getTargets().isEmpty()) {
                        sa.resetTargets();
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                sa.getTargets().add(choice);
            }
        } else if (sa.hasParam("Choices")) {
            // only check for options, does not select there
            CardCollectionView choices = game.getCardsIn(ZoneType.Battlefield);
            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host, sa);
            Collection<Card> betterChoices = getBetterOptions(aiPlayer, sa, choices, !mandatory);
            if (betterChoices.isEmpty()) {
                if (mandatory) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
                }
            }
        }

        if ("TriggeredCardController".equals(sa.getParam("Controller"))) {
            Card trigCard = (Card)sa.getTriggeringObject(AbilityKey.Card);
            if (!mandatory && trigCard != null && trigCard.getController().isOpponentOf(aiPlayer)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        //TODO: add logic here
        return true;
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        // Select a card to attach to
        CardCollection betterOptions = getBetterOptions(ai, sa, options, isOptional);
        if (!betterOptions.isEmpty()) {
            options = betterOptions;
        }
        return ComputerUtilCard.getBestAI(options);
    }

    private CardCollection getBetterOptions(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional) {
        final Card host = sa.getHostCard();
        final Player ctrl = host.getController();
        final boolean canCopyLegendary = sa.hasParam("NonLegendary");
        final String filter = canCopyLegendary ? "Permanent" : "Permanent.YouDontCtrl,Permanent.nonLegendary";
        // TODO add filter to not select Legendary from Other Player when ai already have a Legendary with that name
        return CardLists.getValidCards(options, filter, ctrl, host, sa);
    }

    @Override
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        if (params != null && params.containsKey("Attacker")) {
            return (Player) ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), options);
        }
        final List<Card> cards = new PlayerCollection(options).getCreaturesInPlay();
        Card chosen = ComputerUtilCard.getBestCreatureAI(cards);
        return chosen != null ? chosen.getController() : Iterables.getFirst(options, null);
    }

    @Override
    protected GameEntity chooseSingleAttackableEntity(Player ai, SpellAbility sa, Iterable<GameEntity> options, Map<String, Object> params) {
        if (params != null && params.containsKey("Attacker")) {
            return ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), options);
        }
        // should not be reached
        return super.chooseSingleAttackableEntity(ai, sa, options, params);
    }

}
