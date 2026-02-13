package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.*;
import forge.game.CardTraitPredicates;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbilityMode;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;
import forge.util.FileSection;
import forge.util.MyRandom;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class EffectAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final PhaseHandler phase = game.getPhaseHandler();
        boolean randomReturn = MyRandom.getRandom().nextFloat() <= .6667;
        String logic = "";

        if (sa.hasParam("AILogic")) {
            logic = sa.getParam("AILogic");
            if (logic.equals("BeginningOfOppTurn")) {
                if (!phase.getPlayerTurn().isOpponentOf(ai) || phase.getPhase().isAfter(PhaseType.DRAW)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                randomReturn = true;
            } else if (logic.equals("KeepOppCreatsLandsTapped")) {
                for (Player opp : ai.getOpponents()) {
                    boolean worthHolding = false;
                    CardCollectionView oppCreatsLands = CardLists.filter(opp.getCardsIn(ZoneType.Battlefield),
                            CardPredicates.LANDS.or(CardPredicates.CREATURES));
                    CardCollectionView oppCreatsLandsTapped = CardLists.filter(oppCreatsLands, CardPredicates.TAPPED);

                    if (oppCreatsLandsTapped.size() >= 3 || oppCreatsLands.size() == oppCreatsLandsTapped.size()) {
                        worthHolding = true;
                    }
                    if (!worthHolding) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                    randomReturn = true;
                }
            } else if (logic.equals("RestrictBlocking")) {
                if (!phase.isPlayerTurn(ai) || phase.getPhase().isBefore(PhaseType.COMBAT_BEGIN)
                        || phase.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                if (sa.getPayCosts().getTotalMana().countX() > 0 && sa.getHostCard().getSVar("X").equals("Count$xPaid")) {
                    // Set PayX here to half the remaining mana to allow for Main 2 and other combat shenanigans.
                    final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai, sa.isTrigger()) / 2;
                    if (xPay == 0) { return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi); }
                    sa.setXManaCostPaid(xPay);
                }

                Player opp = ai.getStrongestOpponent();
                List<Card> possibleAttackers = ai.getCreaturesInPlay();
                List<Card> possibleBlockers = opp.getCreaturesInPlay();
                possibleBlockers = CardLists.filter(possibleBlockers, CardPredicates.UNTAPPED);
                final Combat combat = game.getCombat();
                int oppLife = opp.getLife();
                int potentialDmg = 0;
                List<Card> currentAttackers = new ArrayList<>();

                if (possibleBlockers.isEmpty()) { return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi); }

                for (final Card creat : possibleAttackers) {
                    if (CombatUtil.canAttack(creat, opp) && possibleBlockers.size() > 1) {
                        potentialDmg += creat.getCurrentPower();
                        if (potentialDmg >= oppLife) { return new AiAbilityDecision(100, AiPlayDecision.WillPlay); }
                    }
                    if (combat != null && combat.isAttacking(creat)) {
                        currentAttackers.add(creat);
                    }
                }

                if (currentAttackers.size() > possibleBlockers.size()) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else if (logic.equals("Fog")) {
                FogAi fogAi = new FogAi();
                if (!fogAi.canPlay(ai, sa).willingToPlay()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                final TargetRestrictions tgt = sa.getTargetRestrictions();
                if (tgt != null) {
                    sa.resetTargets();
                    if (tgt.canOnlyTgtOpponent()) {
                        boolean canTgt = false;

                        for (Player opp2 : ai.getOpponents()) {
                            if (sa.canTarget(opp2)) {
                                sa.getTargets().add(opp2);
                                canTgt = true;
                                break;
                            }
                        }

                        if (!canTgt) {
                            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                        }
                    } else {
                        List<Card> list = game.getCombat().getAttackers();
                        list = CardLists.getTargetableCards(list, sa);
                        Card target = ComputerUtilCard.getBestCreatureAI(list);
                        if (target == null) {
                            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                        }
                        sa.getTargets().add(target);
                    }
                }
                randomReturn = true;
            } else if (logic.equals("ChainVeil")) {
                if (!phase.isPlayerTurn(ai) || !phase.getPhase().equals(PhaseType.MAIN2) || ai.getPlaneswalkersInPlay().isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                randomReturn = true;
            } else if (logic.equals("WillCastCreature") && ai.isAI()) {
                AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
                SpellAbility saCreature = aic.predictSpellToCastInMain2(ApiType.PermanentNoncreature);
                randomReturn = saCreature != null;
            } else if (logic.equals("Always")) {
                randomReturn = true;
            } else if (logic.equals("Main1")) {
                if (phase.getPhase().isBefore(PhaseType.MAIN1)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                randomReturn = true;
            } else if (logic.equals("Main2")) {
                if (phase.getPhase().isBefore(PhaseType.MAIN2)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                randomReturn = true;
            } else if (logic.equals("Evasion")) {
            	if (!phase.isPlayerTurn(ai)) {
            		return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            	}

                boolean shouldPlay = false;

                List<Card> comp = ai.getCreaturesInPlay();

                for (final Player opp : ai.getOpponents()) {
                    List<Card> human = opp.getCreaturesInPlay();

                    // only count creatures that can attack or block
                    comp = CardLists.filter(comp, c -> CombatUtil.canAttack(c, opp));
                    if (comp.size() < 2) {
                        continue;
                    }
                    final List<Card> attackers = comp;
                    human = CardLists.filter(human, c -> CombatUtil.canBlockAtLeastOne(c, attackers));
                    if (human.isEmpty()) {
                        continue;
                    }

                    shouldPlay = true;
                    break;
                }

                return shouldPlay ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.equals("RedirectSpellDamageFromPlayer")) {
                if (game.getStack().isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                boolean threatened = false;
                for (final SpellAbilityStackInstance stackInst : game.getStack()) {
                    if (!stackInst.isSpell()) { continue; }
                    SpellAbility stackSpellAbility = stackInst.getSpellAbility();
                    if (stackSpellAbility.getApi() == ApiType.DealDamage) {
                        final SpellAbility saTargeting = stackSpellAbility.getSATargetingPlayer();
                        if (saTargeting != null && Iterables.contains(saTargeting.getTargets().getTargetPlayers(), ai)) {
                            threatened = true;
                        }
                    }
                }
                randomReturn = threatened;
            } else if (logic.equals("Prevent")) { // prevent burn spell from opponent
                if (game.getStack().isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                final SpellAbility saTop = game.getStack().peekAbility();
                final Card host = saTop.getHostCard();
                if (saTop.getActivatingPlayer() != ai // from opponent
                        && host.canDamagePrevented(false) // no prevent damage
                        && (host.isInstant() || host.isSorcery())
                        && !host.hasKeyword("Prevent all damage that would be dealt by CARDNAME.")) { // valid target
                    final ApiType type = saTop.getApi();
                    if (type == ApiType.DealDamage || type == ApiType.DamageAll) { // burn spell
                        sa.getTargets().add(saTop);
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.equals("NoGain")) {
                // basic logic to cancel GainLife on stack
                if (!game.getStack().isEmpty()) {
                    SpellAbility topStack = game.getStack().peekAbility();
                    final Player activator = topStack.getActivatingPlayer();
                    if (activator.isOpponentOf(ai) && activator.canGainLife()) {
                        while (topStack != null) {
                            if (topStack.getApi() == ApiType.GainLife) {
                                if ("You".equals(topStack.getParam("Defined")) || topStack.isTargeting(activator) || (!topStack.usesTargeting() && !topStack.hasParam("Defined"))) {
                                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                                }
                            } else if (topStack.getApi() == ApiType.DealDamage && topStack.getHostCard().hasKeyword(Keyword.LIFELINK)) {
                                Card host = topStack.getHostCard();
                                for (GameEntity target : topStack.getTargets().getTargetEntities()) {
                                    if (ComputerUtilCombat.predictDamageTo(target,
                                            AbilityUtils.calculateAmount(host, topStack.getParam("NumDmg"), topStack), host, false) > 0) {
                                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                                    }
                                }
                            }
                            topStack = topStack.getSubAbility();
                        }
                    }
                }
                // also check for combat lifelink
                if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    final Combat combat = ai.getGame().getCombat();
                    final Player attackingPlayer = combat.getAttackingPlayer();
                    if (attackingPlayer.isOpponentOf(ai) && attackingPlayer.canGainLife()) {
                        if (ComputerUtilCombat.checkAttackerLifelinkDamage(combat) > 0) {
                            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                        }
                    }
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.equals("NonCastCreature")) {
                // TODO: add support for more cases with more convoluted API setups
                if (!game.getStack().isEmpty()) {
                    SpellAbility topStack = game.getStack().peekAbility();
                    final Player activator = topStack.getActivatingPlayer();
                    if (activator.isOpponentOf(ai)) {
                        boolean changeZone = topStack.getApi() == ApiType.ChangeZone || topStack.getApi() == ApiType.ChangeZoneAll;
                        boolean toBattlefield = "Battlefield".equals(topStack.getParam("Destination"));
                        boolean reanimator = "true".equalsIgnoreCase(topStack.getSVar("IsReanimatorCard"));
                        if (changeZone && (toBattlefield || reanimator)) {
                            if ("Creature".equals(topStack.getParam("ChangeType")) || topStack.getParamOrDefault("Defined", "").contains("Creature"))
                                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                        }
                    }
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.equals("Fight")) {
                return FightAi.canFight(ai, sa, 0,0);
            } else if (logic.equals("Pump")) {
                sa.resetTargets();
                List<Card> options = CardUtil.getValidCardsToTarget(sa);
                options = CardLists.filterControlledBy(options, ai);
                if (sa.getPayCosts().hasTapCost()) {
                    options.remove(sa.getHostCard());
                }
                if (!options.isEmpty() && phase.isPlayerTurn(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(options));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.equals("Burn")) {
                SpellAbility burn = sa.getSubAbility();
                return SpellApiToAi.Converter.get(burn).canPlayWithSubs(ai, burn).willingToPlay() ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.equals("YawgmothsWill")) {
                return SpecialCardAi.YawgmothsWill.consider(ai, sa) ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.startsWith("NeedCreatures")) {
                // TODO convert to AiCheckSVar
                if (ai.getCreaturesInPlay().isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                if (logic.contains(":")) {
                    String[] k = logic.split(":");
                    int i = Integer.parseInt(k[1]);
                    return ai.getCreaturesInPlay().size() >= i ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else if (logic.equals("ReplaySpell")) {
                CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Graveyard), sa.getTargetRestrictions().getValidTgts(), ai, sa.getHostCard(), sa);
                if (!ComputerUtil.targetPlayableSpellCard(ai, list, sa, false, false)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else if (logic.equals("PeaceTalks")) {
                Player nextPlayer = game.getNextPlayerAfter(ai);

                // If opponent doesn't have creatures, preventing attacks don't mean as much
                if (nextPlayer.getCreaturesInPlay().isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                // Only cast Peace Talks after you attack just in case you have creatures
                if (!phase.is(PhaseType.MAIN2)) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                // Create a pseudo combat and see if my life is in danger
                return randomReturn ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.equals("Bribe")) {
                Card host = sa.getHostCard();
                Combat combat = game.getCombat();
                if (combat != null && combat.isAttacking(host, ai) && !combat.isBlocked(host)
                        && phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && !host.getAbilityActivatedThisTurn().getActivators(sa).contains(ai)) {
                    // ideally needs once per combat or something
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (logic.equals("CantRegenerate")) {
                if (sa.usesTargeting()) {
                    CardCollection list = CardLists.getTargetableCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), sa);
                    list = CardLists.filter(list, Card::canBeDestroyed, input -> {
                        Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(input);
                        runParams.put(AbilityKey.Regeneration, true);
                        List<ReplacementEffect> repDestroyList = game.getReplacementHandler().getReplacementList(ReplacementType.Destroy, runParams, ReplacementLayer.Other);
                        // no Destroy Replacement, or one non-Regeneration one like Totem-Armor
                        if (repDestroyList.isEmpty() || repDestroyList.stream().anyMatch(CardTraitPredicates.hasParam("Regeneration").negate())) {
                            return false;
                        }

                        if (cantRegenerateCheckCombat(input) || cantRegenerateCheckStack(input)) {
                            return true;
                        }

                        return false;
                    });

                    if (list.isEmpty()) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                    // TODO check Stack for Effects that would destroy the selected card?
                    sa.getTargets().add(ComputerUtilCard.getBestAI(list));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else if (sa.getParent() != null) {
                    // sub ability should be okay
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else if ("Self".equals(sa.getParam("RememberObjects"))) {
                    // the ones affecting itself are Nimbus cards, were opponent can activate this effect
                    Card host = sa.getHostCard();
                    if (!host.canBeDestroyed()) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }

                    Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(sa.getHostCard());
                    runParams.put(AbilityKey.Regeneration, true);
                    List<ReplacementEffect> repDestroyList = game.getReplacementHandler().getReplacementList(ReplacementType.Destroy, runParams, ReplacementLayer.Other);
                    // no Destroy Replacement, or one non-Regeneration one like Totem-Armor
                    if (repDestroyList.isEmpty() || repDestroyList.stream().anyMatch(CardTraitPredicates.hasParam("Regeneration").negate())) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }

                    if (cantRegenerateCheckCombat(host) || cantRegenerateCheckStack(host)) {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }

                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
        } else if (sa.hasParam("RememberObjects")) { //generic
            boolean cantAttack = false;
            boolean cantBlock = false;
            boolean cantActivate = false;

            String duration = sa.getParam("Duration");
            String matchStr = "Card.IsRemembered";

            for (String st : sa.getParam("StaticAbilities").split(",")) {
                Map<String, String> params = FileSection.parseToMap(sa.getSVar(st), FileSection.DOLLAR_SIGN_KV_SEPARATOR);
                Set<StaticAbilityMode> modes = StaticAbilityMode.setValueOf(params.get("Mode"));

                if (modes.contains(StaticAbilityMode.CantAttack) && matchStr.equals(params.get("ValidCard"))) {
                    cantAttack = true;
                }
                if (modes.contains(StaticAbilityMode.CantBlock) && matchStr.equals(params.get("ValidCard"))) {
                    cantBlock = true;
                }
                if (modes.contains(StaticAbilityMode.CantBlockBy) && matchStr.equals(params.get("ValidBlocker"))) {
                    cantBlock = true;
                }
                if (modes.contains(StaticAbilityMode.CantBeActivated) && matchStr.equals(params.get("ValidCard"))) {
                    cantActivate = true;
                }
            }

            // TODO add more cases later
            if (!cantAttack && !cantBlock && !cantActivate) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            if (cantBlock && duration == null && phase.isPlayerTurn(ai) && !phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            if (sa.usesTargeting()) {
                final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);

                CardCollection list = new CardCollection(CardUtil.getValidCardsToTarget(sa));

                list = ComputerUtil.filterAITgts(sa, ai, list, true);

                if (list.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }

                List<Card> oppCreatures = CardLists.filterAsList(list, c -> {
                    return c.isCreature() && c.getController().isOpponentOf(ai);
                });

                List<Card> oppWithAbilities = CardLists.filterAsList(list, c -> {
                    return !c.isCreature() && c.getController().isOpponentOf(ai) && c.getSpellAbilities().anyMatch(SpellAbility::isActivatedAbility);
                });

                if (cantAttack || cantBlock) {
                    if (oppCreatures.isEmpty()) {
                        if (!cantActivate || oppWithAbilities.isEmpty()) {
                            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                        }
                    }
                }

                while (sa.canAddMoreTarget()) {
                    Card choice = null;
                    if (cantAttack && cantBlock && !oppCreatures.isEmpty()) {
                        Card primeTarget = ComputerUtil.getKilledByTargeting(sa, oppCreatures);
                        if (primeTarget != null) {
                            choice = primeTarget;
                        } else if (phase.isPlayerTurn(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                            // Tap creatures possible blockers before combat during AI's turn.
                            List<Card> attackers;
                            if (phase.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                                //Combat has already started
                                attackers = game.getCombat().getAttackers();
                            } else {
                                attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> CombatUtil.canAttack(c, opp));
                            }
                            List<Card> creatureList = CardLists.filter(list, CardPredicates.possibleBlockerForAtLeastOne(attackers));

                            // TODO check if own creature would be forced to attack and we want to keep it alive

                            if (!attackers.isEmpty() && !creatureList.isEmpty()) {
                                choice = ComputerUtilCard.getBestCreatureAI(creatureList);
                            } else if (sa.isTrigger() || ComputerUtil.castSpellInMain1(ai, sa)) {
                                choice = ComputerUtilCard.getMostExpensivePermanentAI(list);
                            }
                        }
                    } // TODO add logic to tap non creatures with activated abilities if cantActivate is true

                    if (choice == null) { // can't find anything left
                        if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                            sa.resetTargets();
                            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                        } else {
                            if (!ComputerUtil.shouldCastLessThanMax(ai, sa.getHostCard())) {
                                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                            }
                            break;
                        }
                    }

                    list.remove(choice);
                    oppCreatures.remove(choice);
                    sa.getTargets().add(choice);
                }
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else { //no AILogic
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if ("False".equals(sa.getParam("Stackable"))) {
            String name = sa.getParam("Name");
            if (name == null) {
                name = sa.getHostCard().getName() + "'s Effect";
            }
            if (sa.getActivatingPlayer().isCardInCommand(name)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null && tgt.canTgtPlayer()) {
            sa.resetTargets();
            if (tgt.canOnlyTgtOpponent() || logic.equals("BeginningOfOppTurn")) {
                boolean canTgt = false;
                for (Player opp : ai.getOpponents()) {
                    if (sa.canTarget(opp)) {
                        sa.getTargets().add(opp);
                        canTgt = true;
                        break;
                    }
                }
                return canTgt ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else {
                sa.getTargets().add(ai);
            }
        }

        return randomReturn ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        if (sa.hasParam("AILogic")) {
            if (canPlay(aiPlayer, sa).willingToPlay()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }

        // E.g. Nova Pentacle
        if (sa.usesTargeting() && !sa.getTargetRestrictions().canTgtPlayer()) {
            // try to target the opponent's best targetable permanent, if able
            CardCollection oppPerms = CardLists.getValidCards(aiPlayer.getOpponents().getCardsIn(sa.getTargetRestrictions().getZone()), sa.getTargetRestrictions().getValidTgts(), aiPlayer, sa.getHostCard(), sa);
            oppPerms = CardLists.filter(oppPerms, sa::canTarget);
            if (!oppPerms.isEmpty()) {
                sa.resetTargets();
                sa.getTargets().add(ComputerUtilCard.getBestAI(oppPerms));
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }

            if (mandatory) {
                // try to target the AI's worst targetable permanent, if able
                CardCollection aiPerms = CardLists.getValidCards(aiPlayer.getCardsIn(sa.getTargetRestrictions().getZone()), sa.getTargetRestrictions().getValidTgts(), aiPlayer, sa.getHostCard(), sa);
                aiPerms = CardLists.filter(aiPerms, sa::canTarget);
                if (!aiPerms.isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(ComputerUtilCard.getWorstAI(aiPerms));
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }

            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return super.doTriggerNoCost(aiPlayer, sa, mandatory);
    }

    protected boolean cantRegenerateCheckCombat(Card host) {
        final Game game = host.getGame();
        if (!game.getPhaseHandler().inCombat()) {
            return false;
        }
        if (!game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DAMAGE)) {
            return false;
        }

        Combat combat = game.getCombat();

        if (game.getPhaseHandler().isPlayerTurn(host.getController())) {
            // attacking player
            if (!combat.isAttacking(host)) {
                return false;
            }
            // TODO predict lethal combat damage
            return combat.isBlocked(host);
        } else {
            // TODO predict lethal combat damage
            return combat.isBlocking(host);
        }
    }

    protected boolean cantRegenerateCheckStack(Card host) {
        final Game game = host.getGame();

        // do this only in reaction to a threatening spell on directly on the stack
        MagicStack stack = game.getStack();
        if (stack.isEmpty()) {
            return false;
        }
        // TODO check Stack for Effects that would destroy host, either direct or indirect
        SpellAbility stackSa = stack.peekAbility();
        if (stackSa == null) {
            return false;
        }

        // regenerate is a replace destroy, meaning either destroyed by effect
        // or destroyed by state based action, when dying by lethal damage
        SpellAbility subAbility = stackSa;
        while (subAbility != null) {
            ApiType apiType = subAbility.getApi();
            if (apiType == null) {
                continue;
            }

            if (ApiType.DestroyAll == apiType) {
                // or skip to sub abilities?
                if (subAbility.hasParam("NoRegen")) {
                    return false;
                }
                if (subAbility.usesTargeting() && !Iterables.contains(subAbility.getTargets().getTargetPlayers(), host.getController())) {
                    return false;
                }
                String valid = subAbility.getParamOrDefault("ValidCards", "");

                // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
                // to use the X variable
                // We really need a better solution to this
                if (valid.contains("X")) {
                    valid = TextUtil.fastReplace(valid,
                            "X", Integer.toString(AbilityUtils.calculateAmount(subAbility.getHostCard(), "X", subAbility)));
                }

                // host card is valid
                if (host.isValid(valid.split(","), subAbility.getActivatingPlayer(), subAbility.getHostCard(), subAbility)) {
                    return true;
                }
                // failed to check via valid, need to pass through the filterList method
                CardCollectionView list = game.getCardsIn(ZoneType.Battlefield);

                if (subAbility.usesTargeting()) {
                    list = CardLists.filterControlledBy(list, new PlayerCollection(subAbility.getTargets().getTargetPlayers()));
                }

                list = AbilityUtils.filterListByType(list, valid, subAbility);
                if (list.contains(host)) {
                    return true;
                }
                // check for defined
            } else if (ApiType.Destroy == apiType) {
                if (subAbility.hasParam("NoRegen")) {
                    return false;
                }
                if (subAbility.hasParam("Sacrifice")) {
                    return false;
                }
                // simulate getTargetCards
                if (subAbility.usesTargeting()) {
                    // isTargeting checks parents, i think that might be wrong
                    if (subAbility.getTargets().contains(host)) {
                        return true;
                    }
                } else if (AbilityUtils.getDefinedObjects(subAbility.getHostCard(), subAbility.getParam("Defined"), subAbility).contains(host)) {
                    return true;
                }

                if (CardUtil.getRadiance(subAbility).contains(host)) {
                    return true;
                }

                // check for target or indirect target
            } else if (ApiType.DamageAll == apiType) {
                if (!subAbility.hasParam("ValidCards")) {
                    continue;
                }
                String valid = subAbility.getParamOrDefault("ValidCards", "");
                if (valid.isEmpty()) {
                    continue;
                }

                Card source = game.getChangeZoneLKIInfo(subAbility.getHostCard());
                if (source.isWitherDamage()) {
                    return false;
                }

                // host card is valid
                if (host.isValid(valid.split(","), subAbility.getActivatingPlayer(), subAbility.getHostCard(), subAbility)) {
                    // TODO check if damage would be lethal
                    return true;
                }
                // failed to check via valid, need to pass through the filterList method
                CardCollectionView list = game.getCardsIn(ZoneType.Battlefield);
                if (subAbility.usesTargeting()) {
                    list = CardLists.filterControlledBy(list, new PlayerCollection(subAbility.getTargets().getTargetPlayers()));
                }

                list = AbilityUtils.filterListByType(list, valid, subAbility);
                if (list.contains(host)) {
                    // TODO check if damage would be lethal
                    return true;
                }
            } else if (ApiType.DealDamage == apiType) {
                // skip choices
                if (subAbility.hasParam("CardChoices") || subAbility.hasParam("PlayerChoices")) {
                    continue;
                }

                final List<Card> definedSources = AbilityUtils.getDefinedCards(subAbility.getHostCard(), subAbility.getParam("DamageSource"), subAbility);
                if (definedSources == null || definedSources.isEmpty()) {
                    continue;
                }

                boolean targeting = false;
                // simulate getTargetCards
                if (subAbility.usesTargeting()) {
                    // isTargeting checks parents, i think that might be wrong
                    if (subAbility.getTargets().contains(host)) {
                        targeting = true;
                    }
                } else if (AbilityUtils.getDefinedObjects(subAbility.getHostCard(), subAbility.getParam("Defined"), subAbility).contains(host)) {
                    targeting = true;
                }

                for (Card source : definedSources) {
                    final Card sourceLKI = game.getChangeZoneLKIInfo(source);

                    if (sourceLKI.isWitherDamage()) {
                        return false;
                    }

                    if (subAbility.hasParam("RelativeTarget")) {
                        targeting = false;
                        if (AbilityUtils.getDefinedEntities(subAbility.getHostCard(), subAbility.getParam("Defined"), subAbility).contains(host)) {
                            targeting = true;
                        }
                    }
                    // TODO predict damage
                    if (targeting) {
                        return true;
                    }
                }

                if (CardUtil.getRadiance(subAbility).contains(host)) {
                    return true;
                }
            }

            subAbility = subAbility.getSubAbility();
        }

        return false;
    }

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final String aiLogic = sa.getParam("UnlessAI");
        if ("WillAttack".equals(aiLogic)) {
            // TODO use AiController::getPredictedCombat
            AiAttackController aiAtk = new AiAttackController(payer);
            Combat combat = new Combat(payer);
            aiAtk.declareAttackers(combat);
            if (combat.getAttackers().isEmpty()) {
                return false;
            }
        }
        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }
}
