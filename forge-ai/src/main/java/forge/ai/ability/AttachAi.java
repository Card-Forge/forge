package forge.ai.ability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.ai.AiCardMemory;
import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.PlayerControllerAi;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.GlobalRuleChange;
import forge.game.ability.AbilityFactory;
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
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class AttachAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();

        // TODO: improve this so that the AI can use a flash aura buff as a means of killing opposing creatures
        // and gaining card advantage
        if (source.hasKeyword("MayFlashSac") && !ai.couldCastSorcery(sa)) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source, sa)) {
                return false;
            }
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, sa)) {
                return false;
            }
        }

        if (!ai.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)
                && source.getType().isLegendary() && sa instanceof SpellPermanent
                && ai.isCardInPlay(source.getName())) {
            // Don't play the second copy of a legendary enchantment already in play

            // TODO: Add some extra checks for where the AI may want to cast a replacement aura
            // on another creature and keep it when the original enchanted creature is useless
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
        	return false;
        }

        // Attach spells always have a target
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (!attachPreference(sa, tgt, false)) {
                return false;
            }
        }

        // Flash logic
        boolean advancedFlash = false;
        if (ai.getController().isAI()) {
            advancedFlash = ((PlayerControllerAi)ai.getController()).getAi().getBooleanProperty(AiProps.FLASH_ENABLE_ADVANCED_LOGIC);
        }
        if ((source.hasKeyword(Keyword.FLASH) || (!ai.canCastSorcery() && sa.canCastTiming(ai)))
                && source.isAura() && advancedFlash && !doAdvancedFlashAuraLogic(ai, sa, sa.getTargetCard())) {
            return false;
        }

        if (abCost.getTotalMana().countX() > 0 && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value. (Endless Scream and Venarian
            // Gold)
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);

            if (xPay == 0) {
                return false;
            }

            sa.setXManaCostPaid(xPay);
        }

        if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Chained to the Rocks")) {
            final SpellAbility effectExile = AbilityFactory.getAbility(source.getSVar("TrigExile"), source);
            final ZoneType origin = ZoneType.listValueOf(effectExile.getParam("Origin")).get(0);
            final TargetRestrictions exile_tgt = effectExile.getTargetRestrictions();
            final CardCollection list = CardLists.getValidCards(ai.getGame().getCardsIn(origin), exile_tgt.getValidTgts(), ai, source, effectExile);
            final CardCollection targets = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !(c.hasProtectionFrom(source) || c.hasKeyword(Keyword.SHROUD) || c.hasKeyword(Keyword.HEXPROOF));
                }
            });
            return !targets.isEmpty();
        }

        return true;
    }

    private boolean doAdvancedFlashAuraLogic(Player ai, SpellAbility sa, Card attachTarget) {
        Card source = sa.getHostCard();
        Game game = ai.getGame();
        Combat combat = game.getCombat();
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();

        if (!aic.getBooleanProperty(AiProps.FLASH_USE_BUFF_AURAS_AS_COMBAT_TRICKS)) {
            // Currently this only works with buff auras, so if the relevant toggle is disabled, just return true
            // for instant speed use. To be improved later.
            return true;
        }

        int power = 0, toughness = 0;
        List<String> keywords = Lists.newArrayList();
        for (StaticAbility stAb : source.getStaticAbilities()) {
            if ("Continuous".equals(stAb.getParam("Mode"))) {
                if (stAb.hasParam("AddPower")) {
                    power += AbilityUtils.calculateAmount(source, stAb.getParam("AddPower"), stAb);
                }
                if (stAb.hasParam("AddToughness")) {
                    toughness += AbilityUtils.calculateAmount(source, stAb.getParam("AddToughness"), stAb);
                }
                if (stAb.hasParam("AddKeyword")) {
                    keywords.addAll(Lists.newArrayList(stAb.getParam("AddKeyword").split(" & ")));
                }
            }
        }

        boolean isBuffAura = !sa.isCurse() && (power > 0 || toughness > 0 || !keywords.isEmpty());
        if (!isBuffAura) {
            // Currently only works with buff auras, otherwise returns true for instant speed use. To be improved later.
            return true;
        }

        boolean canRespondToStack = false;
        if (!game.getStack().isEmpty()) {
            SpellAbility peekSa = game.getStack().peekAbility();
            Player activator = peekSa.getActivatingPlayer();
            if (activator != null && activator.isOpponentOf(ai)
                    && (!peekSa.usesTargeting() || peekSa.getTargets().getTargetCards().contains(attachTarget))) {
                if (peekSa.getApi() == ApiType.DealDamage || peekSa.getApi() == ApiType.DamageAll) {
                    int dmg = AbilityUtils.calculateAmount(peekSa.getHostCard(), peekSa.getParam("NumDmg"), peekSa);
                    if (dmg < toughness + attachTarget.getNetToughness()) {
                        canRespondToStack = true;
                    }
                } else if (peekSa.getApi() == ApiType.Destroy || peekSa.getApi() == ApiType.DestroyAll) {
                    if (!attachTarget.hasKeyword(Keyword.INDESTRUCTIBLE) && !ComputerUtil.canRegenerate(ai, attachTarget)
                            && keywords.contains("Indestructible")) {
                        canRespondToStack = true;
                    }
                } else if (peekSa.getApi() == ApiType.Pump || peekSa.getApi() == ApiType.PumpAll) {
                    int p = AbilityUtils.calculateAmount(peekSa.getHostCard(), peekSa.getParam("NumAtt"), peekSa);
                    int t = AbilityUtils.calculateAmount(peekSa.getHostCard(), peekSa.getParam("NumDef"), peekSa);
                    if (t < 0 && toughness > 0 && attachTarget.getNetToughness() + t + toughness > 0) {
                        canRespondToStack = true;
                    } else if (p < 0 && power > 0 && attachTarget.getNetPower() + p + power > 0
                            && attachTarget.getNetToughness() + t + toughness > 0) {
                        // Yep, still need to ensure that the net toughness will be positive here even if buffing for power
                        canRespondToStack = true;
                    }
                }
            }
        }

        boolean canSurviveCombat = true;
        if (combat != null && combat.isBlocked(attachTarget)) {
            if (!attachTarget.hasKeyword(Keyword.INDESTRUCTIBLE) && !ComputerUtil.canRegenerate(ai, attachTarget)) {
                boolean dangerous = false;
                int totalAtkPower = 0;
                for (Card attacker : combat.getBlockers(attachTarget)) {
                    if (attacker.hasKeyword(Keyword.DEATHTOUCH) || attacker.hasKeyword(Keyword.INFECT)
                            || attacker.hasKeyword(Keyword.WITHER)) {
                        dangerous = true;
                    }
                    totalAtkPower += attacker.getNetPower();
                }
                if (totalAtkPower > attachTarget.getNetToughness() + toughness || dangerous) {
                    canSurviveCombat = false;
                }
            }
        }

        if (!canSurviveCombat || (attachTarget.isCreature() && ComputerUtilCard.isUselessCreature(ai, attachTarget))) {
            // don't buff anything that will die or get seriously crippled in combat, it's pointless anyway
            return false;
        }

        int chanceToCastAtEOT = aic.getIntProperty(AiProps.FLASH_BUFF_AURA_CHANCE_CAST_AT_EOT);
        int chanceToCastEarly = aic.getIntProperty(AiProps.FLASH_BUFF_AURA_CHANCE_TO_CAST_EARLY);
        int chanceToRespondToStack = aic.getIntProperty(AiProps.FLASH_BUFF_AURA_CHANCE_TO_RESPOND_TO_STACK);

        boolean hasFloatMana = ai.getManaPool().totalMana() > 0;
        boolean willDiscardNow = game.getPhaseHandler().is(PhaseType.END_OF_TURN, ai)
                && ai.getCardsIn(ZoneType.Hand).size() > ai.getMaxHandSize();
        boolean willDieNow = combat != null && ComputerUtilCombat.lifeInSeriousDanger(ai, combat);
        boolean willRespondToStack = canRespondToStack && MyRandom.percentTrue(chanceToRespondToStack);
        boolean willCastEarly = MyRandom.percentTrue(chanceToCastEarly);
        boolean willCastAtEOT = game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getNextTurn().equals(ai) && MyRandom.percentTrue(chanceToCastAtEOT);

        boolean alternativeConsiderations = hasFloatMana || willDiscardNow || willDieNow || willRespondToStack || willCastAtEOT || willCastEarly;

        if (!alternativeConsiderations) {
            if (combat == null ||
                    game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return false;
            }

            return combat.isAttacking(attachTarget) || combat.isBlocking(attachTarget);
        }

        return true;
    }

    /**
     * Acceptable choice.
     *
     * @param c
     *            the c
     * @param mandatory
     *            the mandatory
     * @return the card
     */
    private static Card acceptableChoice(final Card c, final boolean mandatory) {
        if (mandatory) {
            return c;
        }

        // TODO If Not Mandatory, make sure the card is "good enough"
        if (c.isCreature()) {
            final int eval = ComputerUtilCard.evaluateCreature(c);
            if (eval < 130) {
                return null;
            }
        }

        return c;
    }

    /**
     * Choose unpreferred.
     *
     * @param mandatory
     *            the mandatory
     * @param list
     *            the list
     * @return the card
     */
    private static Card chooseUnpreferred(final boolean mandatory, final List<Card> list) {
        if (!mandatory) {
            return null;
        }

        return ComputerUtilCard.getWorstPermanentAI(list, true, true, true, false);
    }

    /**
     * Choose less preferred.
     *
     * @param mandatory
     *            the mandatory
     * @param list
     *            the list
     * @return the card
     */
    private static Card chooseLessPreferred(final boolean mandatory, final List<Card> list) {
        if (!mandatory) {
            return null;
        }

        return ComputerUtilCard.getBestAI(list);
    }

    /**
     * Attach ai change type preference.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIChangeTypePreference(final SpellAbility sa, List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For Cards like Evil Presence or Spreading Seas

        String type = "";

        for (final StaticAbility stAb : attachSource.getStaticAbilities()) {
            final Map<String, String> stab = stAb.getMapParams();
            if (stab.get("Mode").equals("Continuous") && stab.containsKey("AddType")) {
                type = stab.get("AddType");
            }
        }

        if ("ChosenType".equals(type)) {
            // TODO ChosenTypeEffect should have exact same logic that's here
            // For now, Island is as good as any for a default value
            type = "Island";
        }

        list = CardLists.getNotType(list, type); // Filter out Basic Lands that have the same type as the changing type

        // Don't target fetchlands
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                //Check for cards that can be sacrificed in response
                for (final SpellAbility ability : c.getAllSpellAbilities()) {
                    if (ability.isAbility()) {
                        final Cost cost = ability.getPayCosts();
                        for (final CostPart part : cost.getCostParts()) {
                            if (!(part instanceof CostSacrifice)) {
                                continue;
                            }
                            CostSacrifice sacCost = (CostSacrifice) part;
                            if (sacCost.payCostFromSource() && ComputerUtilCost.canPayCost(ability, c.getController())) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        });

        final Card c = ComputerUtilCard.getBestAI(list);

        // TODO Port over some of the existing code, but rewrite most of it.
        // Ultimately, these spells need to be used to reduce mana base of a
        // color. So it might be better to choose a Basic over a Nonbasic
        // Although a nonbasic card with a nasty ability, might be worth it to
        // cast on

        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return acceptableChoice(c, mandatory);
    }

    /**
     * Attach ai keep tapped preference.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIKeepTappedPreference(final SpellAbility sa, final List<Card> list, final boolean mandatory, final Card attachSource) {
        // AI For Cards like Paralyzing Grasp and Glimmerdust Nap
        final List<Card> prefList = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                // Don't do Untapped Vigilance cards
                if (c.isCreature() && c.hasKeyword(Keyword.VIGILANCE) && c.isUntapped()) {
                    return false;
                }

                if (!mandatory) {
                    if (!c.isCreature() && !c.getType().hasSubtype("Vehicle") && !c.isTapped()) {
                        // try to identify if this thing can actually tap
                        for (SpellAbility ab : c.getAllSpellAbilities()) {
                            if (ab.getPayCosts().hasTapCost()) {
                                return true;
                            }
                        }
                        return false;
                    }
                }

                if (!c.isEnchanted()) {
                    return true;
                }

                final Iterable<Card> auras = c.getEnchantedBy();
                final Iterator<Card> itr = auras.iterator();
                while (itr.hasNext()) {
                    final Card aura = itr.next();
                    SpellAbility auraSA = aura.getSpells().get(0);
                    if (auraSA.getApi() == ApiType.Attach) {
                        if ("KeepTapped".equals(auraSA.getParam("AILogic"))) {
                            // Don't attach multiple KeepTapped Auras to one
                            // card
                            return false;
                        }
                    }
                }

                return true;
            }
        });

        final Card c = ComputerUtilCard.getBestAI(prefList);

        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return acceptableChoice(c, mandatory);
    }

    /**
     * Attach to player ai preferences.
     *
     * @param sa
     *            the sa
     * @param mandatory
     *            the mandatory
     * @return the player
     */
    private static Player attachToPlayerAIPreferences(final Player aiPlayer, final SpellAbility sa,
            final boolean mandatory) {
        List<Player> targetable = new ArrayList<>();
        for (final Player player : aiPlayer.getGame().getPlayers()) {
            if (sa.canTarget(player)) {
                targetable.add(player);
            }
        }

        if ("Curse".equals(sa.getParam("AILogic"))) {
            if (!mandatory) {
                targetable.removeAll(aiPlayer.getAllies());
                targetable.remove(aiPlayer);
            }
            if (!targetable.isEmpty()) {
                // first try get weakest opponent to reduce opponents faster
                if (targetable.contains(aiPlayer.getWeakestOpponent())) {
                    return aiPlayer.getWeakestOpponent();
                } else {
                    // then try any other opponent
                    for (final Player curseChoice : targetable) {
                        if (curseChoice.isOpponentOf(aiPlayer)) {
                            return curseChoice;
                        }
                    }
                    // only reaches here if no preferred targets are targetable and sa is mandatory
                    return targetable.get(0);
                }
            }
        } else {
            if (!mandatory) {
                targetable.removeAll(aiPlayer.getOpponents());
            }
            if (!targetable.isEmpty()) {
                // first try self
                if (targetable.contains(aiPlayer)) {
                    return aiPlayer;
                } else {
                    // then try allies
                    for (final Player boonChoice : targetable) {
                        if (!boonChoice.isOpponentOf(aiPlayer)) {
                            return boonChoice;
                        }
                    }
                    // only reaches here if no preferred choices are targetable and sa is mandatory
                    return targetable.get(0);
                }
            }
        }

        return null;
    }

    /**
     * Attach ai control preference.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIAnimatePreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
    	if (list.isEmpty()) {
    		return null;
    	}
    	Card c = null;
        // AI For choosing a Card to Animate.
        List<Card> betterList = CardLists.getNotType(list, "Creature");
        if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Animate Artifact")) {
            betterList = CardLists.filter(betterList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getCMC() > 0;
                }
            });
            c = ComputerUtilCard.getMostExpensivePermanentAI(betterList);
        } else {
        	List<Card> evenBetterList = CardLists.filter(betterList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.hasKeyword(Keyword.INDESTRUCTIBLE) || c.hasKeyword(Keyword.HEXPROOF);
                }
            });
        	if (!evenBetterList.isEmpty()) {
        		betterList = evenBetterList;
        	}
        	evenBetterList = CardLists.filter(betterList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.isUntapped();
                }
            });
        	if (!evenBetterList.isEmpty()) {
        		betterList = evenBetterList;
        	}
        	evenBetterList = CardLists.filter(betterList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getTurnInZone() != c.getGame().getPhaseHandler().getTurn();
                }
            });
        	if (!evenBetterList.isEmpty()) {
        		betterList = evenBetterList;
        	}
        	evenBetterList = CardLists.filter(betterList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (final SpellAbility sa : c.getSpellAbilities()) {
                        if (sa.isAbility() && sa.getPayCosts().hasTapCost()) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        	if (!evenBetterList.isEmpty()) {
        		betterList = evenBetterList;
        	}
        	c = ComputerUtilCard.getWorstAI(betterList);
        }


        // If Mandatory (brought directly into play without casting) gotta
        // choose something
        if (c == null && mandatory) {
            return chooseLessPreferred(mandatory, list);
        }

        return c;
    }

    /**
     * Attach ai reanimate preference.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIReanimatePreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For choosing a Card to Animate.
        final Player ai = sa.getActivatingPlayer();
        final Card attachSourceLki = CardUtil.getLKICopy(attachSource);
        attachSourceLki.setLastKnownZone(ai.getZone(ZoneType.Battlefield));
        // Suppress original attach Spell to replace it with another
        attachSourceLki.getFirstAttachSpell().setSuppressed(true);

        //TODO for Reanimate Auras i need the new Attach Spell, in later versions it might be part of the Enchant Keyword
        attachSourceLki.addSpellAbility(AbilityFactory.getAbility(attachSourceLki, "NewAttach"));
        List<Card> betterList = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                final Card lki = CardUtil.getLKICopy(c);
                // need to fake it as if lki would be on the battlefield
                lki.setLastKnownZone(ai.getZone(ZoneType.Battlefield));

                // Reanimate Auras use "Enchant creature put onto the battlefield with CARDNAME" with Remembered
                attachSourceLki.clearRemembered();
                attachSourceLki.addRemembered(lki);

                // need to check what the cards would be on the battlefield
                // do not attach yet, that would cause Events
                CardCollection preList = new CardCollection(lki);
                preList.add(attachSourceLki);
                c.getGame().getAction().checkStaticAbilities(false, Sets.newHashSet(preList), preList);
                boolean result = lki.canBeAttached(attachSourceLki);

                //reset static abilities
                c.getGame().getAction().checkStaticAbilities(false);

                return result;
            }
        });

        final Card c = ComputerUtilCard.getBestCreatureAI(betterList);

        // If Mandatory (brought directly into play without casting) gotta
        // choose something
        if (c == null && mandatory) {
            return chooseLessPreferred(mandatory, list);
        }

        return c;
    }

    // Cards that trigger on dealing damage
    private static Card attachAICuriosityPreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
                                                    final Card attachSource) {
        Card chosen = null;
        int priority = 0;
        for (Card card : list) {
            int cardPriority = 0;
            // Prefer Evasion
            if (card.hasKeyword(Keyword.TRAMPLE)) {
                cardPriority += 10;
            }
            if (card.hasKeyword(Keyword.MENACE)) {
                cardPriority += 10;
            }
            // Avoid this for Sleepers Robe?
            if (card.hasKeyword(Keyword.FEAR)) {
                cardPriority += 15;
            }
            if (card.hasKeyword(Keyword.FLYING)) {
                cardPriority += 20;
            }
            if (card.hasKeyword(Keyword.SHADOW)) {
                cardPriority += 30;
            }
            if (card.hasKeyword(Keyword.HORSEMANSHIP)) {
                cardPriority += 40;
            }
            if (card.hasKeyword("Unblockable")) {
                cardPriority += 50;
            }
            // Prefer "tap to deal damage"
            // TODO : Skip this one if triggers on combat damage only?
            for (SpellAbility sa2 : card.getSpellAbilities()) {
                if (ApiType.DealDamage.equals(sa2.getApi())
                        && (sa2.getTargetRestrictions().canTgtPlayer())) {
                    cardPriority += 300;
                }
            }
            // Prefer stronger creatures, avoid if can't attack
            cardPriority += card.getCurrentToughness() * 2;
            cardPriority += card.getCurrentPower();
            if (card.getCurrentPower() <= 0) {
                cardPriority = -100;
            }
            if (card.hasKeyword(Keyword.DEFENDER)) {
                cardPriority = -100;
            }
            if (card.hasKeyword(Keyword.INDESTRUCTIBLE)) {
                cardPriority += 15;
            }
            if (cardPriority > priority) {
                priority = cardPriority;
                chosen = card;
            }
        }


        return chosen;
    }
    /**
     * Attach ai specific card preference.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAISpecificCardPreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        final Player ai = sa.getActivatingPlayer();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        Card chosen = null;

        if ("Guilty Conscience".equals(sourceName)) {
            chosen = SpecialCardAi.GuiltyConscience.getBestAttachTarget(ai, sa, list);
        } else if (sa.hasParam("AIValid")) {
            // TODO: Make the AI recognize which cards to pump based on the card's abilities alone
            chosen = doPumpOrCurseAILogic(ai, sa, list, sa.getParam("AIValid"));
        }

        // If Mandatory (brought directly into play without casting) gotta
        // choose something
        if (chosen == null && mandatory) {
            return chooseLessPreferred(mandatory, list);
        }

        return chosen;
    }

    private static Card attachAIInstantReequipPreference(final SpellAbility sa, final Card attachSource) {
        // e.g. Cranial Plating
        PhaseHandler ph = attachSource.getGame().getPhaseHandler();
        Combat combat = attachSource.getGame().getCombat();
        Card equipped = sa.getHostCard().getEquipping();
        if (equipped == null) {
            return null;
        }

        int powerBuff = 0;
        for (StaticAbility stAb : sa.getHostCard().getStaticAbilities()) {
            if ("Card.EquippedBy".equals(stAb.getParam("Affected")) && stAb.hasParam("AddPower")) {
                powerBuff = AbilityUtils.calculateAmount(sa.getHostCard(), stAb.getParam("AddPower"), null);
            }
        }
        if (combat != null && combat.isAttacking(equipped) && ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS, sa.getActivatingPlayer())) {
            int damage = 0;
            for (Card c : combat.getUnblockedAttackers()) {
                damage += ComputerUtilCombat.predictDamageTo(combat.getDefenderPlayerByAttacker(equipped), c.getNetCombatDamage(), c, true);
            }
            if (combat.isBlocked(equipped)) {
                for (Card atk : combat.getAttackers()) {
                    if (!combat.isBlocked(atk) && !ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), null).contains(atk)) {
                        if (ComputerUtilCombat.predictDamageTo(combat.getDefenderPlayerByAttacker(atk),
                                atk.getNetCombatDamage(), atk, true) > 0) {
                            if (damage + powerBuff >= combat.getDefenderPlayerByAttacker(atk).getLife()) {
                                sa.resetTargets(); // this is needed to avoid bugs with adding two targets to a single SA
                                return atk; // lethal damage, we can win right now, so why not?
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    // Should generalize this code a bit since they all have similar structures
    /**
     * Attach ai control preference.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIControlPreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For choosing a Card to Gain Control of.

        if (sa.getTargetRestrictions().canTgtPermanent()) {
            // If can target all Permanents, and Life isn't in eminent danger,
            // grab Planeswalker first, then Creature
            // if Life < 5 grab Creature first, then Planeswalker. Lands,
            // Enchantments and Artifacts are probably "not good enough"

        }

        final Card c = ComputerUtilCard.getBestAI(list);

        // If Mandatory (brought directly into play without casting) gotta
        // choose something
        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return acceptableChoice(c, mandatory);
    }

    /**
     * Attach ai highest evaluated preference.
     *
     * @param list          the initial valid list
     * @return the card
     */
    private static Card attachAIHighestEvaluationPreference(final List<Card> list) {
        return ComputerUtilCard.getBestAI(list);
    }

    /**
     * Attach ai curse preference.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAICursePreference(final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource, final Player ai) {
        // AI For choosing a Card to Curse of.

        // TODO Figure out some way to combine The "gathering of data" from
        // statics used in both Pump and Curse
        String stCheck = null;
        if (attachSource.isAura()) {
            stCheck = "EnchantedBy";
        } else if (attachSource.isEquipment()) {
            stCheck = "EquippedBy";
        }

        int totToughness = 0;
        int totPower = 0;
        final List<String> keywords = new ArrayList<>();

        for (final StaticAbility stAbility : attachSource.getStaticAbilities()) {
            final Map<String, String> stabMap = stAbility.getMapParams();

            if (!stabMap.get("Mode").equals("Continuous")) {
                continue;
            }

            final String affected = stabMap.get("Affected");

            if (affected == null) {
                continue;
            }
            if ((affected.contains(stCheck) || affected.contains("AttachedBy"))) {
                totToughness += AbilityUtils.calculateAmount(attachSource, stabMap.get("AddToughness"), sa);
                totPower += AbilityUtils.calculateAmount(attachSource, stabMap.get("AddPower"), sa);

                String kws = stabMap.get("AddKeyword");
                if (kws != null) {
                    keywords.addAll(Arrays.asList(kws.split(" & ")));
                }
                kws = stabMap.get("AddHiddenKeyword");
                if (kws != null) {
                    keywords.addAll(Arrays.asList(kws.split(" & ")));
                }
            }
        }

        List<Card> prefList = null;
        if (totToughness < 0) {
            // Kill a creature if we can
            final int tgh = totToughness;
            prefList = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!c.hasKeyword(Keyword.INDESTRUCTIBLE) && (c.getLethalDamage() <= Math.abs(tgh))) {
                        return true;
                    }

                    return c.getNetToughness() <= Math.abs(tgh);
                }
            });
        }

        Card c = null;
        if (prefList == null || prefList.isEmpty()) {
            prefList = new ArrayList<>(list);
        } else {
            c = ComputerUtilCard.getBestAI(prefList);
            if (c != null) {
                return c;
            }
        }

        if (!keywords.isEmpty()) {
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return containsUsefulCurseKeyword(keywords, c, sa);
                }
            });
        } else if (totPower < 0) {
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return ComputerUtilCombat.canAttackNextTurn(c) && c.getNetPower() > 0;
                }
            });
        }

        //some auras aren't useful in multiples
        if (attachSource.hasSVar("NonStackingAttachEffect")) {
            prefList = CardLists.filter(prefList,
                Predicates.not(CardPredicates.isEnchantedBy(attachSource.getName()))
            );
        }

        // If this is already attached and there's a sac cost, make sure we attach to something that's
        // seriously better than whatever the attachment is currently attached to (e.g. Bound by Moonsilver)
        if (sa.getHostCard().getAttachedTo() != null && sa.getHostCard().getAttachedTo().isCreature()
                && sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
            final int oldEvalRating = ComputerUtilCard.evaluateCreature(sa.getHostCard().getAttachedTo());
            final int threshold = ai.isAI() ? ((PlayerControllerAi)ai.getController()).getAi().getIntProperty(AiProps.SAC_TO_REATTACH_TARGET_EVAL_THRESHOLD) : Integer.MAX_VALUE;
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    if (!card.isCreature()) {
                        return false;
                    }

                    return ComputerUtilCard.evaluateCreature(card) >= oldEvalRating + threshold;
                }
            });
        }

        c = ComputerUtilCard.getBestAI(prefList);

        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return acceptableChoice(c, mandatory);
    }


    /**
     * Attach do trigger ai.
     * @param sa
     *            the sa
     * @param mandatory
     *            the mandatory
     *
     * @return true, if successful
     */
    @Override
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card card = sa.getHostCard();
        // Check if there are any valid targets
        List<GameObject> targets = new ArrayList<>();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            targets = AbilityUtils.getDefinedObjects(sa.getHostCard(), sa.getParam("Defined"), sa);
        } else {
            AttachAi.attachPreference(sa, tgt, mandatory);
            targets = sa.getTargets();
        }

        if (!mandatory && card.isEquipment() && !targets.isEmpty()) {
            Card newTarget = (Card) targets.get(0);
            //don't equip human creatures
            if (newTarget.getController().isOpponentOf(ai)) {
                return false;
            }

            //don't equip a worse creature
            if (card.isEquipping()) {
                Card oldTarget = card.getEquipping();
                if (ComputerUtilCard.evaluateCreature(oldTarget) > ComputerUtilCard.evaluateCreature(newTarget)) {
                    return false;
                }
                // don't equip creatures that don't gain anything
                return !card.hasSVar("NonStackingAttachEffect") || !newTarget.isEquippedBy(card.getName());
            }
        }

        return true;
    }

    /**
     * Attach preference.
     *
     * @param sa
     *            the sa
     * @param sa
     *            the sa
     * @param tgt
     *            the tgt
     * @param mandatory
     *            the mandatory
     * @return true, if successful
     */
    private static boolean attachPreference(final SpellAbility sa, final TargetRestrictions tgt, final boolean mandatory) {
        GameObject o;
        if (tgt.canTgtPlayer()) {
            o = attachToPlayerAIPreferences(sa.getActivatingPlayer(), sa, mandatory);
        } else {
            o = attachToCardAIPreferences(sa.getActivatingPlayer(), sa, mandatory);
        }

        if (o == null) {
            return false;
        }

        sa.getTargets().add(o);
        return true;
    }

    /**
     * Attach ai pump preference.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @return the card
     */
    private static Card attachAIPumpPreference(final Player ai, final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource) {
        // AI For choosing a Card to Pump
        Card c = null;
        List<Card> magnetList = null;
        String stCheck = null;
        if (attachSource.isAura() || sa.isBestow()) {
            stCheck = "EnchantedBy";
            magnetList = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!c.isCreature()) {
                        return false;
                    }
                    String sVar = c.getSVar("EnchantMe");
                    return sVar.equals("Multiple") || (sVar.equals("Once") && !c.isEnchanted());
                }
            });
        } else if (attachSource.isEquipment()) {
            stCheck = "EquippedBy";
            magnetList = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!c.isCreature()) {
                        return false;
                    }
                    String sVar = c.getSVar("EquipMe");
                    return sVar.equals("Multiple") || (sVar.equals("Once") && !c.isEquipped());
                }
            });
        } else if (attachSource.isFortification()) {
            stCheck = "FortifiedBy";
            magnetList = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.isCreature() && !c.isFortified();
                }
            });
        }

        // Look for triggers that will damage the creature and remove AI-owned creatures that will die
        CardCollection toRemove = new CardCollection();
        for (Trigger t : attachSource.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone) {
                final Map<String, String> params = t.getMapParams();
                if ("Card.Self".equals(params.get("ValidCard"))
                        && "Battlefield".equals(params.get("Destination"))) {
                    SpellAbility trigSa = t.ensureAbility();
                    if (trigSa != null && trigSa.getApi() == ApiType.DealDamage && "Enchanted".equals(trigSa.getParam("Defined"))) {
                        for (Card target : list) {
                            if (!target.getController().isOpponentOf(ai)) {
                                int numDmg = AbilityUtils.calculateAmount(target, trigSa.getParam("NumDmg"), trigSa);
                                if (target.getNetToughness() - target.getDamage() <= numDmg && !target.hasKeyword(Keyword.INDESTRUCTIBLE)) {
                                    toRemove.add(target);
                                }
                            }
                        }
                    }
                }
            }
        }
        list.removeAll(toRemove);

        if (magnetList != null) {

            // Look for Heroic triggers
            if (magnetList.isEmpty() && sa.isSpell()) {
                for (Card target : list) {
                    for (Trigger t : target.getTriggers()) {
                        if (t.getMode() == TriggerType.SpellCast) {
                            if ("Card.Self".equals(t.getParam("TargetsValid")) && "You".equals(t.getParam("ValidActivatingPlayer"))) {
                                magnetList.add(target);
                                break;
                            }
                        }
                    }
                }
            }

            if (!magnetList.isEmpty()) {
                // Always choose something from the Magnet List.
                // Probably want to "weight" the list by amount of Enchantments and
                // choose the "lightest"

            	List<Card> betterList = CardLists.filter(magnetList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return CombatUtil.canAttack(c, ai.getWeakestOpponent());
                    }
                });
            	if (!betterList.isEmpty()) {
            		return ComputerUtilCard.getBestAI(betterList);
            	}

            	// Magnet List should not be attached when they are useless
            	betterList = CardLists.filter(magnetList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return !ComputerUtilCard.isUselessCreature(ai, c);
                    }
                });

            	if (!betterList.isEmpty()) {
            		return ComputerUtilCard.getBestAI(betterList);
            	}

                //return ComputerUtilCard.getBestAI(magnetList);
            }
        }

        int totToughness = 0;
        int totPower = 0;
        final List<String> keywords = new ArrayList<>();
        boolean grantingAbilities = false;
        boolean grantingExtraBlock = false;

        for (final StaticAbility stAbility : attachSource.getStaticAbilities()) {
            final Map<String, String> stabMap = stAbility.getMapParams();

            if (!"Continuous".equals(stabMap.get("Mode"))) {
                continue;
            }

            final String affected = stabMap.get("Affected");

            if (affected == null) {
                continue;
            }
            if ((affected.contains(stCheck) || affected.contains("AttachedBy"))) {
                totToughness += AbilityUtils.calculateAmount(attachSource, stabMap.get("AddToughness"), stAbility);
                totPower += AbilityUtils.calculateAmount(attachSource, stabMap.get("AddPower"), stAbility);

                grantingAbilities |= stabMap.containsKey("AddAbility");
                grantingExtraBlock |= stabMap.containsKey("CanBlockAmount") || stabMap.containsKey("CanBlockAny");

                String kws = stabMap.get("AddKeyword");
                if (kws != null) {
                    keywords.addAll(Arrays.asList(kws.split(" & ")));
                }
                kws = stabMap.get("AddHiddenKeyword");
                if (kws != null) {
                    keywords.addAll(Arrays.asList(kws.split(" & ")));
                }
            }
        }

        CardCollection prefList = new CardCollection(list);

        // Filter AI-specific targets if provided
        prefList = ComputerUtil.filterAITgts(sa, ai, (CardCollection)list, false);

        if (totToughness < 0) {
            // Don't kill my own stuff with Negative toughness Auras
            final int tgh = totToughness;
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.getLethalDamage() > Math.abs(tgh);
                }
            });
        }

        //only add useful keywords unless P/T bonus is significant
        if (totToughness + totPower < 4 && (!keywords.isEmpty() || grantingExtraBlock)) {
            final int pow = totPower;
            final boolean extraBlock = grantingExtraBlock;
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (!keywords.isEmpty()) {
                        for (final String keyword : keywords) {
                            if (isUsefulAttachKeyword(keyword, c, sa, pow)) {
                                return true;
                            }
                        }
                    }

                    if (c.hasKeyword(Keyword.INFECT) && pow >= 2) {
                        // consider +2 power a significant bonus on Infect creatures
                        return true;
                    }
                    if (extraBlock && CombatUtil.canBlock(c, true) && !c.canBlockAny()) {
                        return true;
                    }
                    return false;
                }
            });
        }

        //some auras/equipments aren't useful in multiples
        if (attachSource.hasSVar("NonStackingAttachEffect")) {
            prefList = CardLists.filter(prefList, Predicates.not(Predicates.or(
                CardPredicates.isEquippedBy(attachSource.getName()),
                CardPredicates.isEnchantedBy(attachSource.getName())
            )));
        }

        // Don't pump cards that will die.
        prefList = ComputerUtil.getSafeTargets(ai, sa, prefList);

        if (attachSource.isAura()) {
        	if (!attachSource.getName().equals("Daybreak Coronet")) {
	            // TODO For Auras like Rancor, that aren't as likely to lead to
	            // card disadvantage, this check should be skipped
	            prefList = CardLists.filter(prefList, new Predicate<Card>() {
	                @Override
	                public boolean apply(final Card c) {
	                    return !c.isEnchanted() || c.hasKeyword(Keyword.HEXPROOF);
	                }
	            });
        	}

        	// should not attach Auras to creatures that does leave the play
        	// TODO also should not attach Auras to creatures cast with Dash
            prefList = CardLists.filter(prefList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.hasSVar("EndOfTurnLeavePlay");
                }
            });
        }

        if (!grantingAbilities) {
            // Probably prefer to Enchant Creatures that Can Attack
            // Filter out creatures that can't Attack or have Defender
            if (keywords.isEmpty()) {
            	final int powerBonus = totPower;
                prefList = CardLists.filter(prefList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                    	if (!c.isCreature()) {
                    		return true;
                    	}
                        return ComputerUtilCombat.canAttackNextTurn(c) && powerBonus + c.getNetPower() > 0;
                    }
                });
            }
            c = ComputerUtilCard.getBestAI(prefList);
        } else {
            for (Card pref : prefList) {
                if (pref.isLand() && pref.isUntapped()) {
                    return pref;
                }
            }
            // If we grant abilities, we may want to put it on something Weak?
            // Possibly more defensive?
            c = ComputerUtilCard.getWorstPermanentAI(prefList, false, false, false, false);
        }

        if (c == null) {
            return chooseLessPreferred(mandatory, list);
        }

        return c;
    }

    /**
     * Attach to card ai preferences.
     *
     * @param sa
     *            the sa
     * @param sa
     *            the sa
     * @param mandatory
     *            the mandatory
     * @return the card
     */
    private static Card attachToCardAIPreferences(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card attachSource = sa.getHostCard();
        // TODO AttachSource is currently set for the Source of the Spell, but
        // at some point can support attaching a different card

        // Don't equip if DontEquip SVar is set
        if (attachSource.hasSVar("DontEquip")) {
            return null;
        }

        // is no attachment so no using attach
        if (!attachSource.isAttachment()) {
            return null;
        }

        // Don't fortify if already fortifying
        if (attachSource.isFortification() && attachSource.getAttachedTo() != null
                && attachSource.getAttachedTo().getController() == aiPlayer) {
            return null;
        }

        CardCollection list = null;
        if (tgt == null) {
            list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
        } else {
            list = CardLists.getValidCards(aiPlayer.getGame().getCardsIn(tgt.getZone()), tgt.getValidTgts(), sa.getActivatingPlayer(), attachSource, sa);

            list = CardLists.filter(list, CardPredicates.canBeAttached(attachSource));

            // TODO If Attaching without casting, don't need to actually target.
            // I believe this is the only case where mandatory will be true, so just
            // check that when starting that work
            // But we shouldn't attach to things with Protection
            if (!mandatory) {
                list = CardLists.getTargetableCards(list, sa);
            } else {
                list = CardLists.filter(list, Predicates.not(CardPredicates.isProtectedFrom(attachSource)));
            }
        }

        if (list.isEmpty()) {
            return null;
        }
        CardCollection prefList = list;

        // Filter AI-specific targets if provided
        prefList = ComputerUtil.filterAITgts(sa, aiPlayer, list, true);

        Card c = attachGeneralAI(aiPlayer, sa, prefList, mandatory, attachSource, sa.getParam("AILogic"));

        AiController aic = ((PlayerControllerAi)aiPlayer.getController()).getAi();
        if (c != null && attachSource.isEquipment()
                && attachSource.isEquipping()
                && attachSource.getEquipping().getController() == aiPlayer) {
            if (c.equals(attachSource.getEquipping()) && !mandatory) {
                // Do not equip if equipping the same card already
                return null;
            }

            if ("InstantReequipPowerBuff".equals(sa.getParam("AILogic"))) {
                return c;
            }

            boolean uselessCreature = ComputerUtilCard.isUselessCreature(aiPlayer, attachSource.getEquipping());

            if (aic.getProperty(AiProps.MOVE_EQUIPMENT_TO_BETTER_CREATURES).equals("never") && !mandatory) {
                // Do not equip other creatures if the AI profile does not allow moving equipment around
                return null;
            } else if (aic.getProperty(AiProps.MOVE_EQUIPMENT_TO_BETTER_CREATURES).equals("from_useless_only")) {
                // Do not equip other creatures if the AI profile only allows moving equipment from useless creatures
                // and the equipped creature is still useful (not non-untapping+tapped and not set to can't attack/block)
                if (!uselessCreature && !mandatory) {
                    return null;
                }
            }

            // make sure to prioritize casting spells in main 2 (creatures, other equipment, etc.) rather than moving equipment around
            boolean decideMoveFromUseless = uselessCreature && aic.getBooleanProperty(AiProps.PRIORITIZE_MOVE_EQUIPMENT_IF_USELESS);

            if (!decideMoveFromUseless && AiCardMemory.isMemorySetEmpty(aiPlayer, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2)) {
                SpellAbility futureSpell = aic.predictSpellToCastInMain2(ApiType.Attach);
                if (futureSpell != null && futureSpell.getHostCard() != null) {
                    aic.reserveManaSources(futureSpell);
                }
            }

            // avoid randomly moving the equipment back and forth between several creatures in one turn
            if (AiCardMemory.isRememberedCard(aiPlayer, sa.getHostCard(), AiCardMemory.MemorySet.ATTACHED_THIS_TURN) && !mandatory) {
                return null;
            }

            // do not equip if the new creature is not significantly better than the previous one (evaluates at least better by evalT)
            int evalT = aic.getIntProperty(AiProps.MOVE_EQUIPMENT_CREATURE_EVAL_THRESHOLD);
            if (!decideMoveFromUseless && ComputerUtilCard.evaluateCreature(c) - ComputerUtilCard.evaluateCreature(attachSource.getEquipping()) < evalT && !mandatory) {
                return null;
            }
        }

        AiCardMemory.rememberCard(aiPlayer, sa.getHostCard(), AiCardMemory.MemorySet.ATTACHED_THIS_TURN);

        if (c == null && mandatory) {
            CardLists.shuffle(list);
            c = list.getFirst();
        }
        return c;
    }

    /**
     * Attach general ai.
     *
     * @param sa
     *            the sa
     * @param list
     *            the list
     * @param mandatory
     *            the mandatory
     * @param attachSource
     *            the attach source
     * @param logic
     *            the logic
     * @return the card
     */
    private static Card attachGeneralAI(final Player ai, final SpellAbility sa, final List<Card> list, final boolean mandatory,
            final Card attachSource, final String logic) {
        Player prefPlayer = ai.getWeakestOpponent();
        if ("Pump".equals(logic) || "Animate".equals(logic) || "Curiosity".equals(logic)) {
            prefPlayer = ai;
        }
        // Some ChangeType cards are beneficial, and PrefPlayer should be
        // changed to represent that
        final List<Card> prefList;

        if ("Reanimate".equals(logic) || "SpecificCard".equals(logic)) {
            // Reanimate or SpecificCard aren't so restrictive
            prefList = list;
        } else {
            prefList = CardLists.filterControlledBy(list, prefPlayer);
        }

        // AI logic types that do not require a prefList and that evaluate the
        // usefulness of attach action autonomously
        if ("InstantReequipPowerBuff".equals(logic)) {
            return attachAIInstantReequipPreference(sa, attachSource);
        }

        // If there are no preferred cards, and not mandatory bail out
        if (prefList.isEmpty()) {
            return chooseUnpreferred(mandatory, list);
        }

        // Preferred list has at least one card in it to make to the actual
        // Logic
        Card c = null;
        if ("GainControl".equals(logic)) {
            c = attachAIControlPreference(sa, prefList, mandatory, attachSource);
        } else if ("Curse".equals(logic)) {
            c = attachAICursePreference(sa, prefList, mandatory, attachSource, ai);
        } else if ("Pump".equals(logic)) {
            c = attachAIPumpPreference(ai, sa, prefList, mandatory, attachSource);
        } else if ("Curiosity".equals(logic)) {
            c = attachAICuriosityPreference(sa, prefList, mandatory, attachSource);
        } else if ("ChangeType".equals(logic)) {
            c = attachAIChangeTypePreference(sa, prefList, mandatory, attachSource);
        } else if ("KeepTapped".equals(logic)) {
            c = attachAIKeepTappedPreference(sa, prefList, mandatory, attachSource);
        } else if ("Animate".equals(logic)) {
            c = attachAIAnimatePreference(sa, prefList, mandatory, attachSource);
        } else if ("Reanimate".equals(logic)) {
            c = attachAIReanimatePreference(sa, prefList, mandatory, attachSource);
        } else if ("SpecificCard".equals(logic)) {
            c = attachAISpecificCardPreference(sa, prefList, mandatory, attachSource);
        } else if ("HighestEvaluation".equals(logic)) {
            c = attachAIHighestEvaluationPreference(prefList);
        }

        // Consider exceptional cases which break the normal evaluation rules
        if (!isUsefulAttachAction(ai, c, sa)) {
            return null;
        }

        return c;
    }

    /**
     * Contains useful curse keyword.
     *
     * @param keywords
     *            the keywords
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if successful
     */
    private static boolean containsUsefulCurseKeyword(final List<String> keywords, final Card card, final SpellAbility sa) {
        for (final String keyword : keywords) {
            if (isUsefulCurseKeyword(keyword, card, sa)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if is useful keyword.
     *
     * @param keyword
     *            the keyword
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if is useful keyword
     */
    private static boolean isUsefulAttachKeyword(final String keyword, final Card card, final SpellAbility sa, final int powerBonus) {
        final Player ai = sa.getActivatingPlayer();
        final PhaseHandler ph = ai.getGame().getPhaseHandler();

        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        }

        // Don't play if would choose a color the target is already protected from
        if (sa.getHostCard().hasSVar("ChosenProtection")) {
            CardCollectionView oppAllCards = ai.getOpponents().getCardsIn(ZoneType.Battlefield);
            String cc = ComputerUtilCard.getMostProminentColor(oppAllCards);
            if (card.hasKeyword("Protection from " + cc.toLowerCase())) {
                return false;
            }
            // Also don't play if it would destroy own Aura
            for (Card c : card.getEnchantedBy()) {
                if ((c.getController().equals(ai)) && (c.isOfColor(cc))) {
                    return false;
                }
            }
        }

        final boolean evasive = (keyword.equals("Unblockable") || keyword.equals("Fear")
                || keyword.equals("Intimidate") || keyword.equals("Shadow")
                || keyword.equals("Flying") || keyword.equals("Horsemanship")
                || keyword.endsWith("walk") || keyword.startsWith("CantBeBlockedBy")
                || keyword.equals("All creatures able to block CARDNAME do so."));
        // give evasive keywords to creatures that can attack and deal damage

        boolean canBeBlocked = false;
        for (Player opp : ai.getOpponents()) {
            if (CombatUtil.canBeBlocked(card, opp)) {
                canBeBlocked = true;
            }
        }

        if (evasive) {
            return card.getNetCombatDamage() + powerBonus > 0
                    && ComputerUtilCombat.canAttackNextTurn(card)
                    && canBeBlocked;
        } else if (keyword.equals("Haste")) {
            return card.hasSickness() && ph.isPlayerTurn(sa.getActivatingPlayer()) && !card.isTapped()
                    && card.getNetCombatDamage() + powerBonus > 0
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && ComputerUtilCombat.canAttackNextTurn(card);
        } else if (keyword.endsWith("Indestructible")) {
            return true;
        } else if (keyword.endsWith("Deathtouch") || keyword.endsWith("Wither")) {
            return card.getNetCombatDamage() + powerBonus > 0
                    && ((canBeBlocked && ComputerUtilCombat.canAttackNextTurn(card))
                    || CombatUtil.canBlock(card, true));
        } else if (keyword.equals("Double Strike") || keyword.equals("Lifelink")) {
            return card.getNetCombatDamage() + powerBonus > 0
                    && (ComputerUtilCombat.canAttackNextTurn(card) || CombatUtil.canBlock(card, true));
        } else if (keyword.equals("First Strike")) {
            return card.getNetCombatDamage() + powerBonus > 0 && !card.hasKeyword(Keyword.DOUBLE_STRIKE)
                    && (ComputerUtilCombat.canAttackNextTurn(card) || CombatUtil.canBlock(card, true));
        } else if (keyword.startsWith("Flanking")) {
            return card.getNetCombatDamage() + powerBonus > 0
                    && ComputerUtilCombat.canAttackNextTurn(card)
                    && canBeBlocked;
        } else if (keyword.startsWith("Bushido")) {
            return (canBeBlocked && ComputerUtilCombat.canAttackNextTurn(card))
                    || CombatUtil.canBlock(card, true);
        } else if (keyword.equals("Trample")) {
            return card.getNetCombatDamage() + powerBonus > 1
                    && canBeBlocked
                    && ComputerUtilCombat.canAttackNextTurn(card);
        } else if (keyword.equals("Infect")) {
            return card.getNetCombatDamage() + powerBonus > 0
                    && ComputerUtilCombat.canAttackNextTurn(card);
        } else if (keyword.equals("Vigilance")) {
            return card.getNetCombatDamage() + powerBonus > 0
                    && ComputerUtilCombat.canAttackNextTurn(card)
                    && CombatUtil.canBlock(card, true);
        } else if (keyword.equals("Reach")) {
            return !card.hasKeyword(Keyword.FLYING) && CombatUtil.canBlock(card, true);
        } else if (keyword.equals("CARDNAME can attack as though it didn't have defender.")) {
            return card.hasKeyword(Keyword.DEFENDER) && card.getNetCombatDamage() + powerBonus > 0;
        } else if (keyword.equals("Shroud") || keyword.equals("Hexproof")) {
            return !card.hasKeyword(Keyword.SHROUD) && !card.hasKeyword(Keyword.HEXPROOF);
        } else return !keyword.equals("Defender");
    }

    /**
     * Checks if is useful curse keyword.
     *
     * @param keyword
     *            the keyword
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if is useful keyword
     */
    private static boolean isUsefulCurseKeyword(final String keyword, final Card card, final SpellAbility sa) {
        final Player ai = sa.getActivatingPlayer();
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        }

        if (keyword.endsWith("CARDNAME can't attack.") || keyword.equals("Defender")
                || keyword.endsWith("CARDNAME can't attack or block.")) {
            return ComputerUtilCombat.canAttackNextTurn(card) && card.getNetCombatDamage() >= 1;
        } else if (keyword.endsWith("CARDNAME attacks each turn if able.") || keyword.endsWith("CARDNAME attacks each combat if able.")) {
            return ComputerUtilCombat.canAttackNextTurn(card) && CombatUtil.canBlock(card, true) && !ai.getCreaturesInPlay().isEmpty();
        } else if (keyword.endsWith("CARDNAME can't block.") || keyword.contains("CantBlock")) {
            return CombatUtil.canBlock(card, true);
        } else if (keyword.endsWith("CARDNAME's activated abilities can't be activated.")) {
            for (SpellAbility ability : card.getSpellAbilities()) {
                if (ability.isAbility()) {
                    return true;
                }
            }
            return false;
        } else if (keyword.endsWith("Prevent all combat damage that would be dealt by CARDNAME.")) {
            return ComputerUtilCombat.canAttackNextTurn(card) && card.getNetCombatDamage() >= 1;
        } else if (keyword.endsWith("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
                || keyword.endsWith("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
            return ComputerUtilCombat.canAttackNextTurn(card) && card.getNetCombatDamage() >= 2;
        } else if (keyword.endsWith("CARDNAME doesn't untap during your untap step.")) {
            return !card.isUntapped();
        }
        return true;
    }

    /**
     * Checks if it is useful to execute the attach action given the current context.
     *
     * @param c
     *            the card
     * @param sa SpellAbility
     * @return true, if the action is useful (beneficial) in the current minimal context (Card vs. Attach SpellAbility)
     */
    private static boolean isUsefulAttachAction(Player ai, Card c, SpellAbility sa) {
        if (c == null) {
            return false;
        }
        if (sa.getHostCard() == null) {
            // FIXME: Not sure what should the resolution be if a SpellAbility has no host card. This should
            // not happen normally. Possibly remove this block altogether? (if it's an impossible condition).
            System.out.println("AttachAi: isUsefulAttachAction unexpectedly called with SpellAbility with no host card. Assuming it's a determined useful action.");
            return true;
        }

        // useless to equip a creature that can't attack or block.
        return !sa.getHostCard().isEquipment() || !ComputerUtilCard.isUselessCreature(ai, c);
    }

    public static Card doPumpOrCurseAILogic(final Player ai, final SpellAbility sa, final List<Card> list, final String type) {
        Card chosen = null;

        List<Card> aiType = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                // Don't buff opponent's creatures of given type
                if (!c.getController().equals(ai)) {
                    return false;
                }
                return c.isValid(type, ai, sa.getHostCard(), sa);
            }
        });
        List<Card> oppNonType = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                // Don't debuff AI's own creatures not of given type
                if (c.getController().equals(ai)) {
                    return false;
                }
                return !c.isValid(type, ai, sa.getHostCard(), sa) && !ComputerUtilCard.isUselessCreature(ai, c);
            }
        });

        if (!aiType.isEmpty() && !oppNonType.isEmpty()) {
            Card bestAi = ComputerUtilCard.getBestCreatureAI(aiType);
            Card bestOpp = ComputerUtilCard.getBestCreatureAI(oppNonType);
            chosen = ComputerUtilCard.evaluateCreature(bestAi) > ComputerUtilCard.evaluateCreature(bestOpp) ? bestAi : bestOpp;
        } else if (!aiType.isEmpty()) {
            chosen = ComputerUtilCard.getBestCreatureAI(aiType);
        } else if (!oppNonType.isEmpty()) {
            chosen = ComputerUtilCard.getBestCreatureAI(oppNonType);
        }

        return chosen;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        return attachToCardAIPreferences(ai, sa, true);
    }

    @Override
    protected Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        return attachToPlayerAIPreferences(ai, sa, true);
    }
}
