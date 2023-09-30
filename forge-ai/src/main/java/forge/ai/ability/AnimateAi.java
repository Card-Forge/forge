package forge.ai.ability;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.ai.*;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.game.CardTraitPredicates;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.AnimateEffectBase;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.CostPutCounter;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityContinuous;
import forge.game.staticability.StaticAbilityLayer;
import forge.game.zone.ZoneType;
import forge.util.FileSection;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AbilityFactoryAnimate class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryAnimate.java 17608 2012-10-20 22:27:27Z Max mtg $
 */

public class AnimateAi extends SpellAbilityAi {
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        if ("Attacking".equals(aiLogic)) { // Launch the Fleet
            if (ph.getPlayerTurn().isOpponentOf(ai) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
            List<Card> list = CardLists.getTargetableCards(ai.getCreaturesInPlay(), sa);
            for (Card c : list) {
                if (ComputerUtilCard.doesCreatureAttackAI(ai, c)) {
                    sa.getTargets().add(c);
                }
            }
            return !sa.getTargets().isEmpty();
        }
        if ("EOT".equals(aiLogic) && ph.getPhase().isBefore(PhaseType.MAIN2)) {
            return false;
        }
        if ("BoneManCantRegenerate".equals(aiLogic)) {
            Card host = sa.getHostCard();
            String svar = AbilityUtils.getSVar(sa, sa.getParam("staticAbilities"));
            if (svar == null) {
                return false;
            }
            Map<String, String> map = FileSection.parseToMap(svar, FileSection.DOLLAR_SIGN_KV_SEPARATOR);
            if (!map.containsKey("Description")) {
                return false;
            }

            // check for duplicate static ability
            if (Iterables.any(host.getStaticAbilities(), CardTraitPredicates.hasParam("Description", map.get("Description")))) {
                return false;
            }
            // TODO check if Bone Man would deal damage to something that otherwise would regenerate
        }
        return super.checkAiLogic(ai, sa, aiLogic);
    }

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();
        // Interrupt sacrifice effect
        if (!game.getStack().isEmpty()) {
            SpellAbility topStack = game.getStack().peekAbility();
            if (topStack.getApi() == ApiType.Sacrifice) {
                final String valid = topStack.getParamOrDefault("SacValid", "Card.Self");
                String num = topStack.getParamOrDefault("Amount", "1");
                final int nToSac = AbilityUtils.calculateAmount(topStack.getHostCard(), num, topStack);
                CardCollection list = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid,
                		ai.getWeakestOpponent(), topStack.getHostCard(), topStack);
                list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack, true));
                ComputerUtilCard.sortByEvaluateCreature(list);
                if (!list.isEmpty() && list.size() == nToSac && ComputerUtilCost.canPayCost(sa, ai, sa.isTrigger())) {
                    Card animatedCopy = becomeAnimated(source, sa);
                    list.add(animatedCopy);
                    list = CardLists.getValidCards(list, valid, ai.getWeakestOpponent(), topStack.getHostCard(),
                            topStack);
                    list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(topStack, true));
                    if (ComputerUtilCard.evaluateCreature(animatedCopy) < ComputerUtilCard.evaluateCreature(list.get(0))
                            && list.contains(animatedCopy)) {
                        return true;
                    }
                }
            }
        }
        // Don't use instant speed animate abilities before AI's COMBAT_BEGIN
        if (!ph.is(PhaseType.COMBAT_BEGIN) && ph.isPlayerTurn(ai) && !isSorcerySpeed(sa, ai)
                && !sa.hasParam("ActivationPhases") && !"Permanent".equals(sa.getParam("Duration"))) {
            return false;
        }
        // Don't use instant speed animate abilities outside human's
        // COMBAT_DECLARE_ATTACKERS or if no attackers
        if (ph.getPlayerTurn().isOpponentOf(ai) && !"Permanent".equals(sa.getParam("Duration"))
                && (!ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        || ph.inCombat() && game.getCombat().getAttackersOf(ai).isEmpty())) {
            return false;
        }
        // Don't activate during MAIN2 unless this effect is permanent
        if (ph.is(PhaseType.MAIN2) && !"Permanent".equals(sa.getParam("Duration")) && !"UntilYourNextTurn".equals(sa.getParam("Duration"))) {
            return false;
        }
        // Don't animate if the AI won't attack anyway or use as a potential blocker
        Player opponent = ai.getWeakestOpponent();
        // Activating as a potential blocker is only viable if it's an ability activated from a permanent, otherwise
        // the AI will waste resources
        boolean activateAsPotentialBlocker = "UntilYourNextTurn".equals(sa.getParam("Duration"))
                && game.getPhaseHandler().getNextTurn() != ai
                && source.isPermanent();
        if (ph.isPlayerTurn(ai) && ai.getLife() < 6 && opponent.getLife() > 6
                && opponent.getZone(ZoneType.Battlefield).contains(CardPredicates.Presets.CREATURES)
                && !sa.hasParam("AILogic") && !"Permanent".equals(sa.getParam("Duration")) && !activateAsPotentialBlocker) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkApiLogic(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = aiPlayer.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        if (!sa.metConditions() && sa.getSubAbility() == null) {
            return false; // what is this for?
        }
        if (!game.getStack().isEmpty() && game.getStack().peekAbility().getApi() == ApiType.Sacrifice) {
            if (!isAnimatedThisTurn(aiPlayer, source)) {
                rememberAnimatedThisTurn(aiPlayer, source);
                return true;  // interrupt sacrifice
            }
        }
        if (!ComputerUtilCost.checkTapTypeCost(aiPlayer, sa.getPayCosts(), source, sa, new CardCollection())) {
            return false; // prevent crewing with equal or better creatures
        }

        if (sa.costHasManaX() && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, aiPlayer, sa.isTrigger());

            sa.setXManaCostPaid(xPay);
        }

        if (!sa.usesTargeting()) {
            final List<Card> defined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            boolean bFlag = false;
            boolean givesHaste = sa.hasParam("Keywords") && sa.getParam("Keywords").contains("Haste");
            for (final Card c : defined) {
                bFlag |= !c.isCreature() && !c.isTapped()
                        && (!c.hasSickness() || givesHaste || !ph.isPlayerTurn(aiPlayer))
                        && !c.isEquipping();
                
                // for creatures that could be improved (like Figure of Destiny)
                if (!bFlag && c.isCreature() && ("Permanent".equals(sa.getParam("Duration")) || (!c.isTapped() && !c.isSick()))) {
                    int power = -5;
                    if (sa.hasParam("Power")) {
                        power = AbilityUtils.calculateAmount(c, sa.getParam("Power"), sa);
                    }
                    int toughness = -5;
                    if (sa.hasParam("Toughness")) {
                        toughness = AbilityUtils.calculateAmount(c, sa.getParam("Toughness"), sa);
                    }
                    if (sa.hasParam("Keywords")) {
                        for (String keyword : sa.getParam("Keywords").split(" & ")) {
                            if (!c.hasKeyword(keyword)) {
                                bFlag = true;
                            }
                        }
                    }
                    if (power + toughness > c.getCurrentPower() + c.getCurrentToughness()) {
                        if (!c.isTapped() || (ph.inCombat() && game.getCombat().isAttacking(c))) {
                            bFlag = true;
                        }
                    }
                }

                if (!isSorcerySpeed(sa, aiPlayer) && !"Permanent".equals(sa.getParam("Duration"))) {
                    if (sa.hasParam("Crew") && c.isCreature()) {
                        // Do not try to crew a vehicle which is already a creature
                        return false;
                    }
                    Card animatedCopy = becomeAnimated(c, sa);
                    if (ph.isPlayerTurn(aiPlayer)
                            && !ComputerUtilCard.doesSpecifiedCreatureAttackAI(aiPlayer, animatedCopy)) {
                        return false;
                    }
                    if (ph.getPlayerTurn().isOpponentOf(aiPlayer)
                            && !ComputerUtilCard.doesSpecifiedCreatureBlock(aiPlayer, animatedCopy)) {
                        return false;
                    }
                    // also check if maybe there are static effects applied to the animated copy that would matter
                    // (e.g. Myth Realized)
                    if (animatedCopy.getCurrentPower() + animatedCopy.getCurrentToughness() >
                            c.getCurrentPower() + c.getCurrentToughness()) {
                        if (!isAnimatedThisTurn(aiPlayer, sa.getHostCard())) {
                            if (!c.isTapped() || (ph.inCombat() && game.getCombat().isAttacking(c))) {
                                bFlag = true;
                            }
                        }
                    }
                }
            }
            if (bFlag) {
                rememberAnimatedThisTurn(aiPlayer, sa.getHostCard());
            }
            return bFlag; // All of the defined stuff is animated, not very useful
        } else {
            sa.resetTargets();
            return animateTgtAI(sa);
        }

    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        if (sa.usesTargeting()) {
            sa.resetTargets();
            return animateTgtAI(sa);
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting() && !animateTgtAI(sa) && !mandatory) {
            return false;
        } else if (sa.usesTargeting() && mandatory) {
            // fallback if animate is mandatory
            sa.resetTargets();
            List<Card> list = CardUtil.getValidCardsToTarget(sa);
            if (list.isEmpty()) {
                return false;
            }
            Card toAnimate = ComputerUtilCard.getWorstAI(list);
            rememberAnimatedThisTurn(aiPlayer, toAnimate);
            sa.getTargets().add(toAnimate);
        }
        return true;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return player.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2);
    }
    
    private boolean animateTgtAI(final SpellAbility sa) {
        final Player ai = sa.getActivatingPlayer();
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final String logic = sa.getParamOrDefault("AILogic", "");
        final boolean alwaysActivatePWAbility = sa.isPwAbility()
                && sa.getPayCosts().hasSpecificCostType(CostPutCounter.class)
                && sa.usesTargeting()
                && sa.getTargetRestrictions().getMinTargets(sa.getHostCard(), sa) == 0;
        
        final CardType types = new CardType(true);
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }

        // something is used for animate into creature
        if (types.isCreature()) {
            final Game game = ai.getGame();
            CardCollection list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);

            // Filter AI-specific targets if provided
            list = ComputerUtil.filterAITgts(sa, ai, list, false);

            // list is empty, no possible targets
            if (list.isEmpty() && !alwaysActivatePWAbility) {
                return false;
            }

            Map<Card, Integer> data = Maps.newHashMap();
            for (final Card c : list) {
                // don't use Permanent animate on something that would leave the field
                if (c.hasSVar("EndOfTurnLeavePlay") && "Permanent".equals(sa.getParam("Duration"))) {
                    continue;
                }

                // non-Curse Animate not on Opponent Stuff if able
                if (c.getController().isOpponentOf(ai) && !sa.isCurse()) {
                    continue;
                }

                // if tapped it might not attack or block
                if (c.isTapped()) {
                    continue;
                }

                // make Animated copy and evaluate it
                final Card animatedCopy = becomeAnimated(c, sa);
                int aValue = ComputerUtilCard.evaluateCreature(animatedCopy);

                // animated creature has zero toughness, don't do that
                if (animatedCopy.getNetToughness() <= 0) {
                    continue;
                }

                // if original is already a Creature,
                // evaluate their value to check if it becomes better
                if (c.isCreature()) {
                    int cValue = ComputerUtilCard.evaluateCreature(c);
                    if (cValue >= aValue)
                        continue;
                }

                // if its player turn,
                // check if its Permanent or that creature would attack
                if (ph.isPlayerTurn(ai)) {
                    if (!"Permanent".equals(sa.getParam("Duration"))
                            && !ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, animatedCopy)
                            && !"UntilHostLeavesPlay".equals(sa.getParam("Duration"))) {
                        continue;
                    }
                }

                // store in map
                data.put(c, aValue);
            }

            // data is empty, no good targets
            if (data.isEmpty() && !alwaysActivatePWAbility) {
                return false;
            }

            // get the best creature to be animated 
            List<Card> maxList = Lists.newArrayList();
            int maxValue = 0;
            for (final Map.Entry<Card, Integer> e : data.entrySet()) {
                int v = e.getValue(); 
                if (v > maxValue) {
                    maxValue = v;
                    maxList.clear();
                }
                maxList.add(e.getKey());
            }

            // select the worst of the best
            final Card worst = ComputerUtilCard.getWorstAI(maxList);
            if (worst != null) {
                if (worst.isLand()) {
                    // e.g. Clan Guildmage, make sure we're not using the same land we want to animate to activate the ability
                    holdAnimatedTillMain2(ai, worst);
                    if (!ComputerUtilMana.canPayManaCost(sa, ai, 0, sa.isTrigger())) {
                        releaseHeldTillMain2(ai, worst);
                        return false;
                    }
                }
                rememberAnimatedThisTurn(ai, worst);
                sa.getTargets().add(worst);
            }
            return true;
        }

        if (logic.equals("SetPT")) {
            // TODO: 1. Teach the AI to use this to save the creature from direct damage; 2. Determine the best target in a smarter way?
            Card worst = ComputerUtilCard.getWorstCreatureAI(ai.getCreaturesInPlay());
            Card buffed = becomeAnimated(worst, sa);

            if (ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, buffed)
                    && (buffed.getNetPower() - worst.getNetPower() >= 3 || !ComputerUtilCard.doesCreatureAttackAI(ai, worst))) {
                sa.getTargets().add(worst);
                rememberAnimatedThisTurn(ai, worst);
                return true;
            }
        }

        if (logic.equals("ValuableAttackerOrBlocker")) {
            if (ph.inCombat()) {
                final Combat combat = ph.getCombat();
                CardCollection list = CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa);
                for (Card c : list) {
                    Card animated = becomeAnimated(c, sa);
                    boolean isValuableAttacker = ph.is(PhaseType.MAIN1, ai) && ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, animated);
                    boolean isValuableBlocker = combat != null && combat.getDefendingPlayers().contains(ai) && ComputerUtilCard.doesSpecifiedCreatureBlock(ai, animated);
                    if (isValuableAttacker || isValuableBlocker)
                        return true;
                }
            }
        }
        // This is reasonable for now. Kamahl, Fist of Krosa and a sorcery or
        // two are the only things
        // that animate a target. Those can just use AI:RemoveDeck:All until
        // this can do a reasonably good job of picking a good target
        return false;
    }

    public static Card becomeAnimated(final Card card, final SpellAbility sa) {
        final Card copy = CardUtil.getLKICopy(card);
        becomeAnimated(copy, card.hasSickness(), sa);
        return copy;
    }
    private static void becomeAnimated(final Card card, final boolean hasOriginalCardSickness, final SpellAbility sa) {
        // duplicating AnimateEffect.resolve
        final Card source = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();
        final long timestamp = game.getNextTimestamp();
        card.setSickness(hasOriginalCardSickness);

        // AF specific sa
        Integer power = null;
        if (sa.hasParam("Power")) {
            power = AbilityUtils.calculateAmount(source, sa.getParam("Power"), sa);
            if (power == 0 && "PTByCMC".equals(sa.getParam("AILogic"))) {
                power = card.getManaCost().getCMC();
            }
        }
        Integer toughness = null;
        if (sa.hasParam("Toughness")) {
            toughness = AbilityUtils.calculateAmount(source, sa.getParam("Toughness"), sa);
            if (toughness == 0 && "PTByCMC".equals(sa.getParam("AILogic"))) {
                toughness = card.getManaCost().getCMC();
            }
        }

        final CardType types = new CardType(true);
        if (sa.hasParam("Types")) {
            types.addAll(Arrays.asList(sa.getParam("Types").split(",")));
        }

        final CardType removeTypes = new CardType(true);
        if (sa.hasParam("RemoveTypes")) {
            removeTypes.addAll(Arrays.asList(sa.getParam("RemoveTypes").split(",")));
        }

        // allow ChosenType - overrides anything else specified
        if (types.hasSubtype("ChosenType")) {
            types.clear();
            types.add(source.getChosenType());
        }

        final List<String> keywords = Lists.newArrayList();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }

        final List<String> removeKeywords = Lists.newArrayList();
        if (sa.hasParam("RemoveKeywords")) {
            removeKeywords.addAll(Arrays.asList(sa.getParam("RemoveKeywords").split(" & ")));
        }

        final List<String> hiddenKeywords = Lists.newArrayList();
        if (sa.hasParam("HiddenKeywords")) {
            hiddenKeywords.addAll(Arrays.asList(sa.getParam("HiddenKeywords").split(" & ")));
        }
        // allow SVar substitution for keywords
        for (int i = 0; i < keywords.size(); i++) {
            final String k = keywords.get(i);
            if (source.hasSVar(k)) {
                keywords.add(source.getSVar(k));
                keywords.remove(k);
            }
        }

        // colors to be added or changed to
        ColorSet finalColors = ColorSet.getNullColor();
        if (sa.hasParam("Colors")) {
            final String colors = sa.getParam("Colors");
            if (colors.equals("ChosenColor")) {
                finalColors = ColorSet.fromNames(source.getChosenColors());
            } else {
                finalColors = ColorSet.fromNames(colors.split(","));
            }
        }

        // abilities to add to the animated being
        final List<String> abilities = Lists.newArrayList();
        if (sa.hasParam("Abilities")) {
            abilities.addAll(Arrays.asList(sa.getParam("Abilities").split(",")));
        }

        // replacement effects to add to the animated being
        final List<String> replacements = Lists.newArrayList();
        if (sa.hasParam("Replacements")) {
            replacements.addAll(Arrays.asList(sa.getParam("Replacements").split(",")));
        }

        // triggers to add to the animated being
        final List<String> triggers = Lists.newArrayList();
        if (sa.hasParam("Triggers")) {
            triggers.addAll(Arrays.asList(sa.getParam("Triggers").split(",")));
        }

        // static abilities to add to the animated being
        final List<String> stAbs = Lists.newArrayList();
        if (sa.hasParam("staticAbilities")) {
            stAbs.addAll(Arrays.asList(sa.getParam("staticAbilities").split(",")));
        }

        // sVars to add to the animated being
        final List<String> sVars = Lists.newArrayList();
        if (sa.hasParam("sVars")) {
            sVars.addAll(Arrays.asList(sa.getParam("sVars").split(",")));
        }

        AnimateEffectBase.doAnimate(card, sa, power, toughness, types, removeTypes, finalColors,
                keywords, removeKeywords, hiddenKeywords,
                abilities, triggers, replacements, stAbs,
                timestamp, "Permanent");

        // check if animate added static Abilities
        CardTraitChanges traits = card.getChangedCardTraits().get(timestamp, 0);
        if (traits != null) {
            for (StaticAbility stAb : traits.getStaticAbilities()) {
                if ("Continuous".equals(stAb.getParam("Mode"))) {
                    for (final StaticAbilityLayer layer : stAb.getLayers()) {
                        StaticAbilityContinuous.applyContinuousAbility(stAb, new CardCollection(card), layer);
                    }
                }
            }
        }

        // give sVars
        if (sa.hasParam("sVars")) {
            Map<String, String> sVarsMap = Maps.newHashMap();
            for (final String s : sa.getParam("sVars").split(",")) {
                String actualsVar = AbilityUtils.getSVar(sa, s);
                String name = s;
                if (actualsVar.startsWith("SVar:")) {
                    actualsVar = actualsVar.split("SVar:")[1];
                    name = actualsVar.split(":")[0];
                    actualsVar = actualsVar.split(":")[1];
                }
                sVarsMap.put(name, actualsVar);
            }
            card.addChangedSVars(sVarsMap, timestamp, 0);
        }
        ComputerUtilCard.applyStaticContPT(game, card, null);
    }

    private void rememberAnimatedThisTurn(Player ai, Card c) {
        AiCardMemory.rememberCard(ai, c, AiCardMemory.MemorySet.ANIMATED_THIS_TURN);
    }

    public static boolean isAnimatedThisTurn(Player ai, Card c) {
        return AiCardMemory.isRememberedCard(ai, c, AiCardMemory.MemorySet.ANIMATED_THIS_TURN);
    }

    private void holdAnimatedTillMain2(Player ai, Card c) {
        AiCardMemory.rememberCard(ai, c, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2);
    }

    private void releaseHeldTillMain2(Player ai, Card c) {
        AiCardMemory.forgetCard(ai, c, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2);
    }
}
