package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

import forge.ai.*;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.mana.ManaCostBeingPaid;
import forge.card.mana.ManaCost;
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
    public static final String[] OPTIONAL_PARAMS = {
        "AITgts",
    };

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Card source = sa.getHostCard();
        if ("DiscardFattyForReanimation".equals(sa.getParam("AILogic"))) {
            return true; // the logic decides by the cards in hand, at any time of the turn
        }
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
            int x = AbilityUtils.calculateAmount(source, tokenAmount, sa);
            if (source.getSVar("X").equals("Count$Converge")) {
                x = ComputerUtilMana.getConvergeCount(sa, ai);
            }
            if (sa.getSVar("X").equals("Count$xPaid")) {
                x = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
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

        if ("DiscardFattyForReanimation".equals(sa.getParam("AILogic"))) {
            return discardFattyForReanimation(ai, sa);
        }

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
                }
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }

            if (isTargetedAuraToken(actualToken, sa)) {
                return chooseTargetedAuraToken(ai, sa, actualToken, false)
                        ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                        : new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
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
     * The Underworld Cookbook: a card is discarded for a Food only when that is the card's best use, a creature too
     * expensive to ever cast (the card script's DiscardCost preference) while a reanimation spell in hand will bring
     * it back from the graveyard.
     */
    private static AiAbilityDecision discardFattyForReanimation(final Player ai, final SpellAbility sa) {
        final CardCollection hand = new CardCollection(ai.getCardsIn(ZoneType.Hand));
        final Card fatty = ComputerUtil.getCardPreference(ai, sa.getHostCard(), "DiscardCost", hand);
        if (fatty == null || fatty.getCMC() <= ai.getLandsInPlay().size() + 1) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        for (final Card c : hand) {
            if (c == fatty) {
                continue;
            }
            for (final SpellAbility spell : c.getSpellAbilities()) {
                if (spell.getApi() == ApiType.ChangeZone && spell.hasParam("Origin") && spell.hasParam("Destination")
                        && ZoneType.listValueOf(spell.getParam("Origin")).contains(ZoneType.Graveyard)
                        && "Battlefield".equals(spell.getParam("Destination"))) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
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
                }
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }

            if (isTargetedAuraToken(actualToken, sa)) {
                if (!chooseTargetedAuraToken(ai, sa, actualToken, mandatory)) {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
            } else if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            } else if (mandatory || tgt.canOnlyTgtOpponent()) {
                PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                if (targetableOpps.isEmpty()) {
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                }
                Player opp = targetableOpps.min(PlayerPredicates.compareByLife());
                sa.getTargets().add(opp);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
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
                    x = ComputerUtilCost.setMaxXValue(sa, ai, true);
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
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if ("AttackTaxLand".equals(sa.getParam("AILogic"))) {
            return takesTheAttackTaxLand(player, sa);
        }
        if ("AttackTax".equals(sa.getParam("AILogic"))) {
            return AttackRestrictionDeal.worthTaking(player, sa.getHostCard().getController()); // City Hall: two Treasures
        }
        // TODO: AILogic
        return true;
    }

    /**
     * Zagorka, Mother of Sanctum: each player may make a tapped land, and each opponent who does cannot attack her controller
     * during their next turn. Her controller takes it. An opponent takes it only when the land is worth more than the attack
     * it gives up: it has no creature that could attack next turn, and not so many lands that one more adds nothing. Taking it
     * always would build a board that never attacks, until the library is empty.
     */
    private static boolean takesTheAttackTaxLand(final Player player, final SpellAbility sa) {
        final Player owner = sa.getHostCard().getController();
        if (player.equals(owner) || !player.isOpponentOf(owner)) {
            return true;
        }
        for (final Card c : player.getCreaturesInPlay()) {
            // next turn it is untapped and no longer sick: what can attack then is what would stay at home
            if (c.getNetCombatDamage() > 0 && CombatUtil.canAttackNextTurn(c, owner)) {
                return false;
            }
        }
        return player.getLandsInPlay().size() < 7;
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
        ComputerUtilCard.applyStaticContPT(ai.getGame(), result, null);
        return result;
    }

    /**
     * Targeted Aura tokens with an attachment AI logic must use the same controller preference as Aura spells.
     * In particular, "Pump" is beneficial and should prefer our own permanents, not the opponent's.
     */
    private static boolean isTargetedAuraToken(final Card token, final SpellAbility sa) {
        return token.isAura() && "Targeted".equals(sa.getParam("AttachedTo")) && token.hasSVar("AttachAILogic");
    }

    /** Select and add the target using the token's own AttachAILogic (e.g. Pump for Estrid's Mask). */
    private static boolean chooseTargetedAuraToken(final Player ai, final SpellAbility sa,
            final Card token, final boolean mandatory) {
        // attachGeneralAI may filter the collection in place, so do not pass CardUtil's shared target list directly.
        final CardCollection targetable = new CardCollection(CardUtil.getValidCardsToTarget(sa));
        final Card target = AttachAi.attachGeneralAI(ai, sa, targetable, mandatory, token, token.getSVar("AttachAILogic"));
        if (target == null) {
            return false;
        }
        sa.getTargets().add(target);
        return true;
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

    /**
     * A Treasure, a Clue or a Food for an opponent is worth about one mana or a card to them, and it costs the payer the mana of
     * the price now: it is paid when the mana is not needed for anything the payer could cast this turn. Any other price (life,
     * cards) is judged by the general rules.
     */
    private boolean paysToDenyNonCreatureToken(final Player payer, final Cost cost, final Card source, final SpellAbility sa) {
        if (cost.getTotalMana() == null || cost.getCostParts().size() != 1) {
            return ComputerUtilCost.checkLifeCost(payer, cost, source, 4, sa)
                    && ComputerUtilCost.checkDamageCost(payer, cost, source, 4, sa);
        }
        final ManaCost price = cost.getTotalMana();
        if (price.getCMC() > ComputerUtilMana.getAvailableManaEstimate(payer)) {
            return false;
        }
        // Real mana allocation, not converted mana cost: four Plains cannot cast Counterspell with or without the payment, but
        // paying {2} out of Plains + Island + Island + Mountain would take the blue away from it. Pay unless it costs a spell
        // that can be cast right now.
        for (final Card inHand : payer.getCardsIn(ZoneType.Hand)) {
            if (inHand.isLand()) {
                continue;
            }
            // every way to cast it, at the cost it really has now: Affinity and cost reducers count
            for (final SpellAbility spell : castOptions(inHand, payer)) {
                if (!spell.isSpell() || spell.getPayCosts() == null || spell.getPayCosts().getTotalMana() == null
                        || spell.getPayCosts().getTotalMana().countX() > 0) {
                    continue;
                }
                if (spell.getActivatingPlayer() == null) {
                    spell.setActivatingPlayer(payer);
                }
                // The cost it has now (cost reducers, Affinity), with and without the price added to it. Both go through the same
                // mana allocation; the price is added to the cost itself rather than passed as the "extra mana" of
                // canPayManaCost(sa, ...), which is meant for X costs and did not make the AI keep mana for Glorious Anthem.
                if (!ComputerUtilMana.canPayManaCost(currentCost(spell, payer), spell, payer, false)) {
                    continue; // not castable now anyway
                }
                final ManaCostBeingPaid withPrice = currentCost(spell, payer);
                // plain count first: what is left after the price must still be as much as the spell costs
                if (ComputerUtilMana.getAvailableManaEstimate(payer) - price.getCMC() < withPrice.getConvertedManaCost()) {
                    return false; // paying would leave it uncastable
                }
                withPrice.addManaCost(price);
                if (!ComputerUtilMana.canPayManaCost(withPrice, spell, payer, false)) {
                    return false; // paying would leave it uncastable
                }
            }
        }
        return true;
    }

    /**
     * The ways to cast a card: its spells and their alternative costs, whatever the phase. Card.getAllPossibleAbilities drops what
     * cannot be played at this moment even when asked to keep it, and a sorcery-speed spell such as Glorious Anthem cannot be played
     * while a trigger is on the stack, so the AI would have found nothing to keep its mana for. The mana it will take on its own
     * turn is what matters here. Include abilities from alternate card states as well: a modal double-faced card may have a
     * castable back face even when its front face cannot be cast with the available colors.
     */
    private static List<SpellAbility> castOptions(final Card card, final Player payer) {
        final List<SpellAbility> options = new ArrayList<>();
        for (final SpellAbility sa : card.getAllSpellAbilities()) {
            if (sa.isAdventure() && card.isOnAdventure()) {
                continue; // already cast as the adventure
            }
            if (sa.getActivatingPlayer() == null) {
                sa.setActivatingPlayer(payer);
            }
            options.add(sa);
            options.addAll(GameActionUtil.getAlternativeCosts(sa, payer, false));
        }
        return options;
    }

    /** The mana the spell costs the payer right now, after cost increases and reductions. A new object on every call. */
    private static ManaCostBeingPaid currentCost(final SpellAbility spell, final Player payer) {
        return ComputerUtilMana.calculateManaCost(spell.getPayCosts(), spell, payer, true, 0, false);
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
            // in this cases Token might be preferred even if they would not survive
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

            // Smothering Tithe: pay {2} or they get a Treasure. It is no creature, so the comparison of boards below says nothing
            // about it, and the AI never paid
            if (!tokenCard.isCreature()) {
                return paysToDenyNonCreatureToken(payer, cost, source, sa);
            }
            // Token would not survive
            if (tokenCard.getNetToughness() < 1) {
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
