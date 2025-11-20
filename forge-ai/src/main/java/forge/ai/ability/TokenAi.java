package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

import forge.ai.*;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostDraw;
import forge.game.cost.CostPart;
import forge.game.cost.CostPutCounter;
import forge.game.cost.CostRemoveCounter;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;

/**
 * <p>
 * AbilityFactory_Token class.
 * </p>
 *
 * @author Forge
 * @version $Id: AbilityFactoryToken.java 17656 2012-10-22 19:32:56Z Max mtg $
 */
public class TokenAi extends SpellAbilityAi {

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Card source = sa.getHostCard();
        // Planeswalker-related flags
        boolean pwMinus = false;
        boolean pwPlus = false;
        if (sa.isPwAbility()) {
            /*
             * Planeswalker token ability with loyalty costs should be played in
             * Main1 or it might never be used due to other positive abilities.
             * AI is kept from spamming them by the loyalty cost of each usage.
             * Zero/loyalty gain token abilities can be evaluated as per normal.
             */
            for (CostPart c : sa.getPayCosts().getCostParts()) {
                if (c instanceof CostRemoveCounter) {
                    pwMinus = true;
                    break;
                }
                if (c instanceof CostPutCounter && c.convertAmount() > 0) {
                    pwPlus = true;
                    break;
                }
            }
        }

        Card actualToken = spawnToken(ai, sa);

        String tokenAmount = sa.getParamOrDefault("TokenAmount", "1");
        String tokenPower = sa.getParamOrDefault("TokenPower", actualToken.getBasePowerString());
        String tokenToughness = sa.getParamOrDefault("TokenToughness", actualToken.getBaseToughnessString());

        // Don't check toughness yet if token has variable P/T based on X
        boolean tokenHasX = "X".equals(tokenAmount) || "X".equals(tokenPower) || "X".equals(tokenToughness);

        if (!tokenHasX && (actualToken == null || (actualToken.isCreature() && actualToken.getNetToughness() < 1))) {
            // planeswalker plus ability or sub-ability is useful
            return pwPlus || sa.getSubAbility() != null;
        }

        // X-cost spells
        if (tokenHasX) {
            int x = AbilityUtils.calculateAmount(sa.getHostCard(), tokenAmount, sa);
            if (source.getSVar("X").equals("Count$Converge")) {
                x = ComputerUtilMana.getConvergeCount(sa, ai);
            }
            if (sa.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                x = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
                sa.getRootAbility().setXManaCostPaid(x);
            }
            if (x <= 0) {
                if ("RandomPT".equals(sa.getParam("AILogic"))) {
                    // e.g. Necropolis of Azar - we're guaranteed at least 1 toughness from the ability
                    x = 1;
                } else {
                    return false; // 0 tokens or 0 toughness token(s)
                }
            }
        }

        if (canInterruptSacrifice(ai, sa, actualToken, tokenAmount)) {
            return true;
        }

        boolean haste = actualToken.hasKeyword(Keyword.HASTE);
        boolean oneShot = sa.getSubAbility() != null
                && sa.getSubAbility().getApi() == ApiType.DelayedTrigger;
        boolean isCreature = actualToken.isCreature();

        // Don't generate tokens without haste before main 2 if possible
        if (ph.getPhase().isBefore(PhaseType.MAIN2) && ph.isPlayerTurn(ai) && !haste && !sa.hasParam("ActivationPhases")
                && !ComputerUtil.castSpellInMain1(ai, sa)) {
            boolean buff = false;
            for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                if (isCreature && "Creature".equals(c.getSVar("BuffedBy"))) {
                    buff = true;
                }
            }
            if (!buff && !pwMinus) {
                return false;
            }
        }
        if ((ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS))
                && !sa.hasParam("ActivationPhases") && !sa.hasParam("PlayerTurn") && !isSorcerySpeed(sa, ai)
                && !haste && !pwMinus) {
            return false;
        }
        return (!ph.getPhase().isAfter(PhaseType.COMBAT_BEGIN) && ph.isPlayerTurn(ai)) || !oneShot;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Player opp = ai.getWeakestOpponent();

        Card actualToken = spawnToken(ai, sa);

        // Don't kill AIs Legendary tokens
        if (actualToken.getType().isLegendary() && ai.isCardInPlay(actualToken.getName())) {
            // TODO Check if Token is useless due to an aura or counters?
            return new AiAbilityDecision(0, AiPlayDecision.WouldDestroyLegend);
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();

            if (actualToken.getType().hasSubtype("Role")) {
                if (tgtRoleAura(ai, sa, actualToken, false)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
            }

            if (tgt.canOnlyTgtOpponent() || "Opponent".equals(sa.getParam("AITgts"))) {
                if (sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else {
                if (sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                } else {
                    // Flash Foliage
                    CardCollection list = CardLists.getTargetableCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), sa);
                    CardCollection betterList = CardLists.filter(list, c -> c.getLethalDamage() == 1);
                    if (!betterList.isEmpty()) {
                        list = betterList;
                    }
                    betterList = CardLists.getNotKeyword(list, Keyword.TRAMPLE);
                    if (!betterList.isEmpty()) {
                        list = betterList;
                    }
                    if (!list.isEmpty()) {
                        sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(list));
                    } else {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
            }
        }

        double chance = (double)AiProfileUtil.getIntProperty(ai, AiProps.TOKEN_GENERATION_ABILITY_CHANCE) / 100;
        boolean alwaysFromPW = AiProfileUtil.getBoolProperty(ai, AiProps.TOKEN_GENERATION_ALWAYS_IF_FROM_PLANESWALKER);
        boolean alwaysOnOppAttack = AiProfileUtil.getBoolProperty(ai, AiProps.TOKEN_GENERATION_ALWAYS_IF_OPP_ATTACKS);

        if (sa.isPwAbility() && alwaysFromPW) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)
                && game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)
                && game.getCombat() != null
                && !game.getCombat().getAttackers().isEmpty()
                && alwaysOnOppAttack
                && actualToken.isCreature()) {
            for (Card attacker : game.getCombat().getAttackers()) {
                if (CombatUtil.canBlock(attacker, actualToken)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            // if the token can't block, then what's the point?
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
        }

        if (MyRandom.getRandom().nextFloat() <= chance) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /**
     * Checks if the token(s) can save a creature from a sacrifice effect
     */
    private boolean canInterruptSacrifice(final Player ai, final SpellAbility sa, final Card token, final String tokenAmount) {
        final Game game = ai.getGame();
        if (game.getStack().isEmpty()) {
            return false; // nothing to interrupt
        }
        final SpellAbility topStack = game.getStack().peekAbility();
        if (topStack.getApi() != ApiType.Sacrifice) {
            return false; // not sacrifice effect
        }
        final int nTokens = AbilityUtils.calculateAmount(sa.getHostCard(), tokenAmount, sa);
        final String valid = topStack.getParamOrDefault("SacValid", "Card.Self");
        String num = sa.getParamOrDefault("Amount", "1");
        final int nToSac = AbilityUtils.calculateAmount(topStack.getHostCard(), num, topStack);
        CardCollection list = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid,
        		ai.getWeakestOpponent(), topStack.getHostCard(), sa);
        list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack, true));
        // only care about saving single creature for now
        if (!list.isEmpty() && nTokens > 0 && list.size() == nToSac) {
            ComputerUtilCard.sortByEvaluateCreature(list);
            list.add(token);
            list = CardLists.getValidCards(list, valid, ai.getWeakestOpponent(), topStack.getHostCard(), sa);
            list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack, true));
            return ComputerUtilCard.evaluateCreature(token) < ComputerUtilCard.evaluateCreature(list.get(0))
                    && list.contains(token);
        }
        return false;
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        Card actualToken = spawnToken(ai, sa);

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();

            if (actualToken.getType().hasSubtype("Role")) {
                if (tgtRoleAura(ai, sa, actualToken, mandatory)) {
                    // Targeting handled in tgtRoleAura
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
            }

            if (tgt.canOnlyTgtOpponent()) {
                PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                if (mandatory && targetableOpps.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                Player opp = targetableOpps.min(PlayerPredicates.compareByLife());
                sa.getTargets().add(opp);
            } else {
                sa.getTargets().add(ai);
            }
        }

        String tokenPower = sa.getParamOrDefault("TokenPower", actualToken.getBasePowerString());
        String tokenToughness = sa.getParamOrDefault("TokenToughness", actualToken.getBaseToughnessString());
        String tokenAmount = sa.getParamOrDefault("TokenAmount", "1");
        final Card source = sa.getHostCard();

        if ("X".equals(tokenAmount) || "X".equals(tokenPower) || "X".equals(tokenToughness)) {
            int x = AbilityUtils.calculateAmount(source, tokenAmount, sa);
            if (sa.getSVar("X").equals("Count$xPaid")) {
                if (x == 0) { // already paid outside trigger
                    // Set PayX here to maximum value.
                    x = ComputerUtilCost.getMaxXValue(sa, ai, true);
                    sa.setXManaCostPaid(x);
                }
            }
            if (x <= 0 && !mandatory) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (mandatory) {
            // Necessary because the AI goes into this method twice, first to set up targets (with mandatory=true)
            // and then the second time to confirm the trigger (where mandatory may be set to false).
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if ("OnlyOnAlliedAttack".equals(sa.getParam("AILogic"))) {
            Combat combat = ai.getGame().getCombat();
            if (combat != null && combat.getAttackingPlayer() != null
                    && !combat.getAttackingPlayer().isOpponentOf(ai)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
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
        // TODO: AILogic
        return true;
    }

    /*
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayer(forge.game.player.Player, forge.card.spellability.SpellAbility, Iterable<forge.game.player.Player> options)
     */
    @Override
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        if (params != null && params.containsKey("Attacker")) {
            return (Player) ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), options);
        }
        return Iterables.getFirst(options, null);
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayerOrPlaneswalker(forge.game.player.Player, forge.card.spellability.SpellAbility, Iterable<forge.game.GameEntity> options)
     */
    @Override
    protected GameEntity chooseSingleAttackableEntity(Player ai, SpellAbility sa, Iterable<GameEntity> options, Map<String, Object> params) {
        if (params != null && params.containsKey("Attacker")) {
            return ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), options);
        }
        // should not be reached
        return super.chooseSingleAttackableEntity(ai, sa, options, params);
    }

    /**
     * Create the token as a Card object.
     * @param ai owner of the new token
     * @param sa Token SpellAbility
     * @return token creature created by ability
     */
    public static Card spawnToken(Player ai, SpellAbility sa) {
        if (!sa.hasParam("TokenScript")) {
            throw new RuntimeException("Spell Ability has no TokenScript: " + sa);
        }
        // TODO for now, only checking the first token is good enough
        Card result = TokenInfo.getProtoType(sa.getParam("TokenScript").split(",")[0], sa, ai);

        if (result == null) {
            throw new RuntimeException("don't find Token for TokenScript: " + sa.getParam("TokenScript"));
        }

        // set battlefield zone for LKI checks
        result.setLastKnownZone(ai.getZone(ZoneType.Battlefield));

        // Apply static abilities
        final Game game = ai.getGame();
        ComputerUtilCard.applyStaticContPT(game, result, null);
        return result;
    }

    private boolean tgtRoleAura(final Player ai, final SpellAbility sa, final Card tok, final boolean mandatory) {
        boolean isCurse = "Curse".equals(sa.getParam("AILogic")) || "Curse".equals(tok.getSVar("AttachAILogic"));
        List<Card> tgts = CardUtil.getValidCardsToTarget(sa);

        // look for card without role from ai
        List<Card> prefListSBA = CardLists.filter(tgts, c ->
                !c.getAttachedCards().anyMatch(att ->
                        att.getController() == ai && att.getType().hasSubtype("Role")));

        List<Card> prefList;
        if (isCurse) {
            prefList = CardLists.filterControlledBy(prefListSBA, ai.getOpponents());
        } else {
            prefList = CardLists.filterControlledBy(prefListSBA, ai.getYourTeam());
        }

        if (prefList.isEmpty()) {
            if (mandatory) {
                if (sa.isTargetNumberValid()) {
                    // TODO try replace Curse <-> Pump depending on target controller
                    return true;
                }
                if (!prefListSBA.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getWorstCreatureAI(prefListSBA));
                    return true;
                }
                if (!tgts.isEmpty()) {
                    sa.getTargets().add(ComputerUtilCard.getWorstCreatureAI(tgts));
                    return true;
                }
            }
        } else {
            sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(prefList));
            return true;
        }

        return false;
    }

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final Card source = sa.getHostCard();
        Player p = sa.getActivatingPlayer();
        if (sa.isKeyword(Keyword.FABRICATE)) {
            final int n = Integer.parseInt(sa.getParam("TokenAmount"));

            // if host would leave the play or if host is useless, create tokens
            if (source.hasSVar("EndOfTurnLeavePlay") || ComputerUtilCard.isUselessCreature(payer, source)) {
                return false;
            }

            // need a copy for one with extra +1/+1 counter boost,
            // without causing triggers to run
            final Card copy = CardCopyService.getLKICopy(source);
            copy.setCounters(CounterEnumType.P1P1, copy.getCounters(CounterEnumType.P1P1) + n);
            copy.setZone(source.getZone());

            // if host would put into the battlefield attacking
            Combat combat = source.getGame().getCombat();
            if (combat != null && combat.isAttacking(source)) {
                final Player defender = combat.getDefenderPlayerByAttacker(source);
                if (defender.canLoseLife() && !ComputerUtilCard.canBeBlockedProfitably(defender, copy, true)) {
                    return true;
                }
                return false;
            }

            // if the host has haste and can attack
            if (CombatUtil.canAttack(copy)) {
                for (final Player opp : payer.getOpponents()) {
                    if (CombatUtil.canAttack(copy, opp) &&
                            opp.canLoseLife() &&
                            !ComputerUtilCard.canBeBlockedProfitably(opp, copy, true))
                        return true;
                }
            }

            // TODO check for trigger to turn token ETB into +1/+1 counter for host
            // TODO check for trigger to turn token ETB into damage or life loss for opponent
            // in this cases Token might be prefered even if they would not survive
            final Card tokenCard = TokenAi.spawnToken(payer, sa);

            // Token would not survive
            if (!tokenCard.isCreature() || tokenCard.getNetToughness() < 1) {
                return true;
            }

            // Special Card logic, this one try to median its power with the number of artifacts
            if ("Marionette Master".equals(source.getName())) {
                CardCollection list = CardLists.filter(payer.getCardsIn(ZoneType.Battlefield), CardPredicates.ARTIFACTS);
                return list.size() >= copy.getNetPower();
            } else if ("Cultivator of Blades".equals(source.getName())) {
                // Cultivator does try to median with number of Creatures
                CardCollection list = payer.getCreaturesInPlay();
                return list.size() >= copy.getNetPower();
            }

            // evaluate Creature with +1/+1
            int evalCounter = ComputerUtilCard.evaluateCreature(copy);

            final CardCollection tokenList = new CardCollection(source);
            for (int i = 0; i < n; ++i) {
                tokenList.add(TokenAi.spawnToken(payer, sa));
            }

            // evaluate Host with Tokens
            int evalToken = ComputerUtilCard.evaluateCreatureList(tokenList);

            return evalToken < evalCounter;
        }

        // Development effect, Payer can let Opponent draw, or they get a token
        if (payer.isOpponentOf(sa.getActivatingPlayer())) {
            if (cost.hasSpecificCostType(CostDraw.class)) {
                CostDraw draw = cost.getCostPartByType(CostDraw.class);
                // try to deck out opponent
                if (draw.getPotentialPlayers(payer, sa).contains(p) && p.getCardsIn(ZoneType.Library).size() < 5) {
                    if (!p.isCardInPlay("Laboratory Maniac") || p.cantWin()) {
                        return true;
                    }
                }
            }

            if (alreadyPaid) {
                return false;
            }
            final Card tokenCard = TokenAi.spawnToken(p, sa);

            // Token would not survive
            if (!tokenCard.isCreature() || tokenCard.getNetToughness() < 1) {
                return false;
            }
            int evalActivator = ComputerUtilCard.evaluateCreature(tokenCard) + ComputerUtilCard.evaluateCreatureList(p.getCreaturesInPlay());
            int evalPayerCreatures = ComputerUtilCard.evaluateCreatureList(payer.getCreaturesInPlay());

            if (evalActivator > evalPayerCreatures) {
                return true;
            }
        }
        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }
}
