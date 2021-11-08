package forge.ai.ability;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

public class CopyPermanentAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        Card source = sa.getHostCard();
        PhaseHandler ph = aiPlayer.getGame().getPhaseHandler();
        String aiLogic = sa.getParamOrDefault("AILogic", "");

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if ("MomirAvatar".equals(aiLogic)) {
            return SpecialCardAi.MomirVigAvatar.consider(aiPlayer, sa);
        } else if ("MimicVat".equals(aiLogic)) {
            return SpecialCardAi.MimicVat.considerCopy(aiPlayer, sa);
        } else if ("AtEOT".equals(aiLogic)) {
            return ph.is(PhaseType.END_OF_TURN);
        } else if ("AtOppEOT".equals(aiLogic)) {
            return ph.is(PhaseType.END_OF_TURN) && ph.getPlayerTurn() != aiPlayer;
        } else if ("DuplicatePerms".equals(aiLogic)) {
            final List<Card> valid = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            if (valid.size() < 2) {
                return false;
            }
        }

        if (sa.hasParam("AtEOT") && !ph.is(PhaseType.MAIN1)) {
            return false;
        }

        if (sa.hasParam("Defined")) {
            // If there needs to be an imprinted card, don't activate the ability if nothing was imprinted yet (e.g. Mimic Vat)
            if (sa.getParam("Defined").equals("Imprinted.ExiledWithSource") && source.getImprintedCards().isEmpty()) {
                return false;
            }
        }

        if (sa.hasParam("Embalm") || sa.hasParam("Eternalize")) {
            // E.g. Vizier of Many Faces: check to make sure it makes sense to make the token now
            if (ComputerUtilCard.checkNeedsToPlayReqs(sa.getHostCard(), sa) != AiPlayDecision.WillPlay) {
                return false;
            }
        }

        if (sa.costHasManaX() && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value. (Osgir)
            final int xPay = ComputerUtilCost.getMaxXValue(sa, aiPlayer);

            sa.setXManaCostPaid(xPay);
        }

        if (sa.usesTargeting() && sa.hasParam("TargetingPlayer")) {
            sa.resetTargets();
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            return targetingPlayer.getController().chooseTargetsFor(sa);
        } else if (sa.usesTargeting() && sa.getTargetRestrictions().canTgtPlayer()) {
                if (!sa.isCurse()) {
                    if (sa.canTarget(aiPlayer)) {
                        sa.getTargets().add(aiPlayer);
                        return true;
                    } else {
                        for (Player p : aiPlayer.getTeamMates(true)) {
                            if (sa.canTarget(p)) {
                                sa.getTargets().add(p);
                                return true;
                            }
                        }
                        return false;
                    }
                } else {
                    for (Player p : aiPlayer.getOpponents()) {
                        if (sa.canTarget(p)) {
                            sa.getTargets().add(p);
                            return true;
                        }
                    }
                    return false;
                }
        } else {
            return doTriggerAINoCost(aiPlayer, sa, false);
        }
    }

    @Override
    protected boolean doTriggerAINoCost(final Player aiPlayer, SpellAbility sa, boolean mandatory) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = host.getGame();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final boolean canCopyLegendary = sa.hasParam("NonLegendary");

        // ////
        // Targeting
        if (sa.usesTargeting()) {
            sa.resetTargets();

            CardCollection list = new CardCollection(CardUtil.getValidCardsToTarget(sa.getTargetRestrictions(), sa));

            list = CardLists.filter(list, Predicates.not(CardPredicates.isRemAIDeck()));
            //Nothing to target
            if (list.isEmpty()) {
            	return false;
            }
            
            // Saheeli Rai + Felidar Guardian combo support
            if ("Saheeli Rai".equals(sourceName)) {
                CardCollection felidarGuardian = CardLists.filter(list, CardPredicates.nameEquals("Felidar Guardian"));
                if (felidarGuardian.size() > 0) {
                    // can copy a Felidar Guardian and combo off, so let's do it
                    sa.getTargets().add(felidarGuardian.get(0));
                    return true;
                }
            }

            // target loop
            while (sa.canAddMoreTarget()) {
                if (list.isEmpty()) {
                    if (!sa.isTargetNumberValid() || (sa.getTargets().size() == 0)) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return (!c.getType().isLegendary() || canCopyLegendary) || !c.getController().equals(aiPlayer);
                    }
                });
                Card choice;
                if (Iterables.any(list, Presets.CREATURES)) {
                    if (sa.hasParam("TargetingPlayer")) {
                        choice = ComputerUtilCard.getWorstCreatureAI(list);
                    } else {
                        choice = ComputerUtilCard.getBestCreatureAI(list);
                    }
                } else {
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(list);
                }

                if (choice == null) { // can't find anything left
                    if (!sa.isTargetNumberValid() || (sa.getTargets().size() == 0)) {
                        sa.resetTargets();
                        return false;
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
                return mandatory;
            }
        } else {
            // if no targeting, it should always be ok
        }

        if ("TriggeredCardController".equals(sa.getParam("Controller"))) {
            Card trigCard = (Card)sa.getTriggeringObject(AbilityKey.Card);
            if (!mandatory && trigCard != null && trigCard.getController().isOpponentOf(aiPlayer)) {
                return false;
            }
        }

        return true;
    }
    
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
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
        return CardLists.getValidCards(options, filter.split(","), ctrl, host, sa);
    }

    @Override
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        if (params.containsKey("Attacker")) {
            return (Player) ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), new FCollection<GameEntity>(options));
        }
        final List<Card> cards = new PlayerCollection(options).getCreaturesInPlay();
        Card chosen = ComputerUtilCard.getBestCreatureAI(cards);
        return chosen != null ? chosen.getController() : Iterables.getFirst(options, null);
    }

    @Override
    protected GameEntity chooseSinglePlayerOrPlaneswalker(Player ai, SpellAbility sa, Iterable<GameEntity> options, Map<String, Object> params) {
        if (params.containsKey("Attacker")) {
            return ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), new FCollection<GameEntity>(options));
        }
        // should not be reached
        return super.chooseSinglePlayerOrPlaneswalker(ai, sa, options, params);
    }

}
