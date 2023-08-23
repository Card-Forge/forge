package forge.ai.ability;

import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.AiCardMemory;
import forge.ai.AiController;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.CardTraitPredicates;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
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
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.TextUtil;

public class EffectAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(final Player ai,final SpellAbility sa) {
        final Game game = ai.getGame();
        boolean randomReturn = MyRandom.getRandom().nextFloat() <= .6667;
        String logic = "";

        if (sa.hasParam("AILogic")) {
            logic = sa.getParam("AILogic");
            final PhaseHandler phase = game.getPhaseHandler();
            if (logic.equals("BeginningOfOppTurn")) {
                if (!phase.getPlayerTurn().isOpponentOf(ai) || phase.getPhase().isAfter(PhaseType.DRAW)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("EndOfOppTurn")) {
                if (!phase.getPlayerTurn().isOpponentOf(ai) || phase.getPhase().isBefore(PhaseType.END_OF_TURN)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("KeepOppCreatsLandsTapped")) {
                for (Player opp : ai.getOpponents()) {
                    boolean worthHolding = false;
                    CardCollectionView oppCreatsLands = CardLists.filter(opp.getCardsIn(ZoneType.Battlefield),
                        Predicates.or(CardPredicates.Presets.LANDS, CardPredicates.Presets.CREATURES));
                    CardCollectionView oppCreatsLandsTapped = CardLists.filter(oppCreatsLands, CardPredicates.Presets.TAPPED);

                    if (oppCreatsLandsTapped.size() >= 3 || oppCreatsLands.size() == oppCreatsLandsTapped.size()) {
                        worthHolding = true;
                    }
                    if (!worthHolding) {
                        return false;
                    }
                    randomReturn = true;
                }
            } else if (logic.equals("Fog")) {
                FogAi fogAi = new FogAi();
                if (!fogAi.canPlayAI(ai, sa)) {
                    return false;
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
                            return false;
                        }
                    } else {
                        List<Card> list = game.getCombat().getAttackers();
                        list = CardLists.getTargetableCards(list, sa);
                        Card target = ComputerUtilCard.getBestCreatureAI(list);
                        if (target == null) {
                            return false;
                        }
                        sa.getTargets().add(target);
                    }
                }
                randomReturn = true;
            } else if (logic.equals("ChainVeil")) {
                if (!phase.isPlayerTurn(ai) || !phase.getPhase().equals(PhaseType.MAIN2) || ai.getPlaneswalkersInPlay().isEmpty()) {
                    return false;
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
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("Main2")) {
                if (phase.getPhase().isBefore(PhaseType.MAIN2)) {
                    return false;
                }
                randomReturn = true;
            } else if (logic.equals("Evasion")) {
            	if (!phase.isPlayerTurn(ai)) {
            		return false;
            	}

                boolean shouldPlay = false;

                List<Card> comp = ai.getCreaturesInPlay();

                for (final Player opp : ai.getOpponents()) {
                    List<Card> human = opp.getCreaturesInPlay();

                    // only count creatures that can attack or block
                    comp = CardLists.filter(comp, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return CombatUtil.canAttack(c, opp);
                        }
                    });
                    if (comp.size() < 2) {
                        continue;
                    }
                    final List<Card> attackers = comp;
                    human = CardLists.filter(human, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return CombatUtil.canBlockAtLeastOne(c, attackers);
                        }
                    });
                    if (human.isEmpty()) {
                        continue;
                    }

                    shouldPlay = true;
                    break;
                }

                return shouldPlay;
            } else if (logic.equals("RedirectSpellDamageFromPlayer")) {
                if (game.getStack().isEmpty()) {
                    return false;
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
                    return false;
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
                        return true;
                    }
                }
                return false;
            } else if (logic.equals("NoGain")) {
            	// basic logic to cancel GainLife on stack
                if (game.getStack().isEmpty()) {
                    return false;
                }
                final SpellAbility topStack = game.getStack().peekAbility();
                return topStack.getActivatingPlayer().isOpponentOf(ai) && topStack.getApi() == ApiType.GainLife;
            } else if (logic.equals("Fight")) {
                return FightAi.canFightAi(ai, sa, 0, 0);
            } else if (logic.equals("Pump")) {
                sa.resetTargets();
                List<Card> options = CardUtil.getValidCardsToTarget(sa);
                options = CardLists.filterControlledBy(options, ai);
                if (sa.getPayCosts().hasTapCost()) {
                    options.remove(sa.getHostCard());
                }
                if (!options.isEmpty() && phase.isPlayerTurn(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(options));
                    return true;
                }
                return false;
            } else if (logic.equals("Burn")) {
                // for DamageDeal sub-abilities (eg. Wild Slash, Skullcrack)
                SpellAbility burn = sa.getSubAbility();
                return SpellApiToAi.Converter.get(burn.getApi()).canPlayAIWithSubs(ai, burn);
            } else if (logic.equals("YawgmothsWill")) {
                return SpecialCardAi.YawgmothsWill.consider(ai, sa);
            } else if (logic.startsWith("NeedCreatures")) {
                if (ai.getCreaturesInPlay().isEmpty()) {
                    return false;
                }
                if (logic.contains(":")) {
                    String[] k = logic.split(":");
                    Integer i = Integer.valueOf(k[1]);
                    return ai.getCreaturesInPlay().size() >= i;
                }
                return true;
            } else if (logic.equals("ReplaySpell")) {
                CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Graveyard), sa.getTargetRestrictions().getValidTgts(), ai, sa.getHostCard(), sa);
                if (!ComputerUtil.targetPlayableSpellCard(ai, list, sa, false, false)) {
                    return false;
                }
            } else if (logic.equals("PeaceTalks")) {
                Player nextPlayer = game.getNextPlayerAfter(ai);

                // If opponent doesn't have creatures, preventing attacks don't mean as much
                if (nextPlayer.getCreaturesInPlay().isEmpty()) {
                    return false;
                }

                // Only cast Peace Talks after you attack just in case you have creatures
                if (!phase.is(PhaseType.MAIN2)) {
                    return false;
                }

                // Create a pseudo combat and see if my life is in danger
                return randomReturn;
            } else if (logic.equals("Bribe")) {
                Card host = sa.getHostCard();
                Combat combat = game.getCombat();
                if (combat != null && combat.isAttacking(host, ai) && !combat.isBlocked(host)
                        && phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && !AiCardMemory.isRememberedCard(ai, host, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN)) {
                    AiCardMemory.rememberCard(ai, host, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN); // ideally needs once per combat or something
                    return true;
                }
                return false;
            } else if (logic.equals("CantRegenerate")) {
                if (sa.usesTargeting()) {
                    CardCollection list = CardLists.getTargetableCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), sa);
                    list = CardLists.filter(list, CardPredicates.Presets.CAN_BE_DESTROYED, new Predicate<Card>() {

                        @Override
                        public boolean apply(@Nullable Card input) {
                            Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(input);
                            runParams.put(AbilityKey.Regeneration, true);
                            List<ReplacementEffect> repDestoryList = game.getReplacementHandler().getReplacementList(ReplacementType.Destroy, runParams, ReplacementLayer.Other);
                            // no Destroy Replacement, or one non-Regeneration one like Totem-Armor
                            if (repDestoryList.isEmpty() || Iterables.any(repDestoryList, Predicates.not(CardTraitPredicates.hasParam("Regeneration")))) {
                                return false;
                            }

                            if (cantRegenerateCheckCombat(input) || cantRegenerateCheckStack(input)) {
                                return true;
                            }

                            return false;
                        }
                    });

                    if (list.isEmpty()) {
                        return false;
                    }
                    // TODO check Stack for Effects that would destroy the selected card?
                    sa.getTargets().add(ComputerUtilCard.getBestAI(list));
                    return true;
                } else if (sa.getParent() != null) {
                    // sub ability should be okay
                    return true;
                } else if ("Self".equals(sa.getParam("RememberObjects"))) {
                    // the ones affecting itself are Nimbus cards, were opponent can activate this effect
                    Card host = sa.getHostCard();
                    if (!host.canBeDestroyed()) {
                        return false;
                    }

                    Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(sa.getHostCard());
                    runParams.put(AbilityKey.Regeneration, true);
                    List<ReplacementEffect> repDestoryList = game.getReplacementHandler().getReplacementList(ReplacementType.Destroy, runParams, ReplacementLayer.Other);
                    // no Destroy Replacement, or one non-Regeneration one like Totem-Armor
                    if (repDestoryList.isEmpty() || Iterables.any(repDestoryList, Predicates.not(CardTraitPredicates.hasParam("Regeneration")))) {
                        return false;
                    }

                    if (cantRegenerateCheckCombat(host) || cantRegenerateCheckStack(host)) {
                        return true;
                    }

                    return false;
                }
            }
        } else { //no AILogic
            return false;
        }

        if ("False".equals(sa.getParam("Stackable"))) {
            String name = sa.getParam("Name");
            if (name == null) {
                name = sa.getHostCard().getName() + "'s Effect";
            }
            if (sa.getActivatingPlayer().isCardInCommand(name)) {
                return false;
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
                return canTgt;
            } else {
                sa.getTargets().add(ai);
            }
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        // E.g. Nova Pentacle
        if (sa.usesTargeting() && !sa.getTargetRestrictions().canTgtPlayer()) {
            // try to target the opponent's best targetable permanent, if able
            CardCollection oppPerms = CardLists.getValidCards(aiPlayer.getOpponents().getCardsIn(sa.getTargetRestrictions().getZone()), sa.getTargetRestrictions().getValidTgts(), aiPlayer, sa.getHostCard(), sa);
            if (!oppPerms.isEmpty()) {
                sa.resetTargets();
                sa.getTargets().add(ComputerUtilCard.getBestAI(oppPerms));
                return true;
            }

            if (mandatory) {
                // try to target the AI's worst targetable permanent, if able
                CardCollection aiPerms = CardLists.getValidCards(aiPlayer.getCardsIn(sa.getTargetRestrictions().getZone()), sa.getTargetRestrictions().getValidTgts(), aiPlayer, sa.getHostCard(), sa);
                if (!aiPerms.isEmpty()) {
                    sa.resetTargets();
                    sa.getTargets().add(ComputerUtilCard.getWorstAI(aiPerms));
                    return true;
                }
            }

            return false;
        }

        return super.doTriggerAINoCost(aiPlayer, sa, mandatory);
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
                } else {
                    if (AbilityUtils.getDefinedObjects(subAbility.getHostCard(), subAbility.getParam("Defined"), subAbility).contains(host)) {
                        return true;
                    }
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
                } else {
                    if (AbilityUtils.getDefinedObjects(subAbility.getHostCard(), subAbility.getParam("Defined"), subAbility).contains(host)) {
                        targeting = true;
                    }
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
}
