package forge.ai.ability;

import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
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
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.collect.FCollection;

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
        String tokenAmount = sa.getParamOrDefault("TokenAmount", "1");

        Card actualToken = spawnToken(ai, sa);

        if (actualToken == null || actualToken.getNetToughness() < 1) {
            final AbilitySub sub = sa.getSubAbility();
            // useful
            // no token created
            return pwPlus || (sub != null && SpellApiToAi.Converter.get(sub.getApi()).chkAIDrawback(sub, ai)); // planeswalker plus ability or sub-ability is
        }

        String tokenPower = sa.getParamOrDefault("TokenPower", actualToken.getBasePowerString());
        String tokenToughness = sa.getParamOrDefault("TokenToughness", actualToken.getBaseToughnessString());

        // X-cost spells
        if ("X".equals(tokenAmount) || "X".equals(tokenPower) || "X".equals(tokenToughness)) {
            int x = AbilityUtils.calculateAmount(sa.getHostCard(), tokenAmount, sa);
            if (source.getSVar("X").equals("Count$Converge")) {
                x = ComputerUtilMana.getConvergeCount(sa, ai);
            }
            if (sa.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                x = ComputerUtilCost.getMaxXValue(sa, ai);
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
        boolean isCreature = actualToken.getType().isCreature();

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
                && !sa.hasParam("ActivationPhases") && !sa.hasParam("PlayerTurn") && !SpellAbilityAi.isSorcerySpeed(sa)
                && !haste && !pwMinus) {
            return false;
        }
        return (!ph.getPhase().isAfter(PhaseType.COMBAT_BEGIN) && ph.isPlayerTurn(ai)) || !oneShot;
    }
    
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        /*
         * readParameters() is called in checkPhaseRestrictions
         */
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();
        final Player opp = ai.getWeakestOpponent();

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false; // prevent infinite tokens?
        }
        Card actualToken = spawnToken(ai, sa);

        // Don't kill AIs Legendary tokens
        if (actualToken.getType().isLegendary() && ai.isCardInPlay(actualToken.getName())) {
            // TODO Check if Token is useless due to an aura or counters?
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (tgt.canOnlyTgtOpponent() || "Opponent".equals(sa.getParam("AITgts"))) {
                if (sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                } else {
                    return false;
                }
            } else {
                if (sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                } else {
                    // Flash Foliage
                    CardCollection list = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield),
                            ai.getOpponents());
                    list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source, sa);
                    list = CardLists.getTargetableCards(list, sa);
                    CardCollection betterList = CardLists.filter(list, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card c) {
                            return c.getLethalDamage() == 1;
                        }
                    });
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
                        return false;
                    }
                }
            }
        }

        double chance = 1.0F; // 100%
        boolean alwaysFromPW = true;
        boolean alwaysOnOppAttack = true;

        if (ai.getController().isAI()) {
            AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
            chance = (double)aic.getIntProperty(AiProps.TOKEN_GENERATION_ABILITY_CHANCE) / 100;
            alwaysFromPW = aic.getBooleanProperty(AiProps.TOKEN_GENERATION_ALWAYS_IF_FROM_PLANESWALKER);
            alwaysOnOppAttack = aic.getBooleanProperty(AiProps.TOKEN_GENERATION_ALWAYS_IF_OPP_ATTACKS);
        }

        if (sa.isPwAbility() && alwaysFromPW) {
            return true;
        } else if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)
                && game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)
                && game.getCombat() != null
                && !game.getCombat().getAttackers().isEmpty()
                && alwaysOnOppAttack) {
            return true;
        }

        return MyRandom.getRandom().nextFloat() <= chance;
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
        CardCollection list = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","),
        		ai.getWeakestOpponent(), topStack.getHostCard(), sa);
        list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack));
        // only care about saving single creature for now
        if (!list.isEmpty() && nTokens > 0 && list.size() == nToSac) {
            ComputerUtilCard.sortByEvaluateCreature(list);
            list.add(token);
            list = CardLists.getValidCards(list, valid.split(","), ai.getWeakestOpponent(), topStack.getHostCard(), sa);
            list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack));
            return ComputerUtilCard.evaluateCreature(token) < ComputerUtilCard.evaluateCreature(list.get(0))
                    && list.contains(token);
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                if (mandatory && targetableOpps.isEmpty()) {
                    return false;
                }
                Player opp = targetableOpps.min(PlayerPredicates.compareByLife());
                sa.getTargets().add(opp);
            } else {
                sa.getTargets().add(ai);
            }
        }

        Card actualToken = spawnToken(ai, sa);
        String tokenPower = sa.getParamOrDefault("TokenPower", actualToken.getBasePowerString());
        String tokenToughness = sa.getParamOrDefault("TokenToughness", actualToken.getBaseToughnessString());
        String tokenAmount = sa.getParamOrDefault("TokenAmount", "1");
        final Card source = sa.getHostCard();

        if ("X".equals(tokenAmount) || "X".equals(tokenPower) || "X".equals(tokenToughness)) {
            int x = AbilityUtils.calculateAmount(source, tokenAmount, sa);
            if (sa.getSVar("X").equals("Count$xPaid")) {
                if (x == 0) { // already paid outside trigger
                    // Set PayX here to maximum value.
                    x = ComputerUtilCost.getMaxXValue(sa, ai);
                    sa.setXManaCostPaid(x);
                }
            }
            if (x <= 0) {
                return false;
            }
        }

        if (mandatory) {
            // Necessary because the AI goes into this method twice, first to set up targets (with mandatory=true)
            // and then the second time to confirm the trigger (where mandatory may be set to false).
            return true;
        }

        if ("OnlyOnAlliedAttack".equals(sa.getParam("AILogic"))) {
            Combat combat = ai.getGame().getCombat();
            return combat != null && combat.getAttackingPlayer() != null
                    && !combat.getAttackingPlayer().isOpponentOf(ai);
        }

        return true;
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        // TODO: AILogic
        return true;
    }

    /*
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayer(forge.game.player.Player, forge.card.spellability.SpellAbility, Iterable<forge.game.player.Player> options)
     */
    @Override
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        if (params != null && params.containsKey("Attacker")) {
            return (Player) ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), new FCollection<GameEntity>(options));
        }
        return Iterables.getFirst(options, null);
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayerOrPlaneswalker(forge.game.player.Player, forge.card.spellability.SpellAbility, Iterable<forge.game.GameEntity> options)
     */
    @Override
    protected GameEntity chooseSinglePlayerOrPlaneswalker(Player ai, SpellAbility sa, Iterable<GameEntity> options, Map<String, Object> params) {
        if (params != null && params.containsKey("Attacker")) {
            return ComputerUtilCombat.addAttackerToCombat(sa, (Card) params.get("Attacker"), new FCollection<GameEntity>(options));
        }
        // should not be reached
        return super.chooseSinglePlayerOrPlaneswalker(ai, sa, options, params);
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

        result.setOwner(ai);

        // Apply static abilities
        final Game game = ai.getGame();
        ComputerUtilCard.applyStaticContPT(game, result, null);
        return result;
    }

}
