/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.ai;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;



import com.esotericsoftware.minlog.Log;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.simulation.SpellAbilityPicker;
import forge.card.CardStateName;
import forge.card.MagicColor;
import forge.card.CardType.Supertype;
import forge.card.mana.ManaCost;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Direction;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GlobalRuleChange;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellApiBased;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostPart;
import forge.game.cost.CostPutCounter;
import forge.game.cost.CostRemoveCounter;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.replacement.ReplaceMoved;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.Ability;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.Expressions;
import forge.util.collect.FCollectionView;
import forge.util.MyRandom;

/**
 * <p>
 * ComputerAI_General class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AiController {
    private final Player player;
    private final Game game;
    private final AiCardMemory memory;
    private boolean cheatShuffle;
    private boolean useSimulation;
    private SpellAbilityPicker simPicker;

    public boolean canCheatShuffle() {
        return cheatShuffle;
    }

    public void allowCheatShuffle(boolean canCheatShuffle) {
        this.cheatShuffle = canCheatShuffle;
    }

    public void setUseSimulation(boolean value) {
        this.useSimulation = value;
    }
    
    public Game getGame() {
        return game;
    }

    public Player getPlayer() {
        return player;
    }

    public AiCardMemory getCardMemory() {
        return memory;
    }

    public AiController(final Player computerPlayer, final Game game0) {
        player = computerPlayer;
        game = game0;
        memory = new AiCardMemory();
        simPicker = new SpellAbilityPicker(game, player);
    }

    private List<SpellAbility> getPossibleETBCounters() {
        final Player opp = player.getOpponent();
        CardCollection all = new CardCollection(player.getCardsIn(ZoneType.Hand));
        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            all.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        all.addAll(opp.getCardsIn(ZoneType.Exile));

        final List<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : all) {
            for (final SpellAbility sa : c.getNonManaAbilities()) {
                if (sa instanceof SpellPermanent) {
                    sa.setActivatingPlayer(player);
                    if (checkETBEffects(c, sa, ApiType.Counter)) {
                        spellAbilities.add(sa);
                    }
                }
            }
        }
        return spellAbilities;
    }
    
    // look for cards on the battlefield that should prevent the AI from using that spellability
    private boolean checkCurseEffects(final SpellAbility sa) {
        for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.hasSVar("AICurseEffect")) {
                final String curse = c.getSVar("AICurseEffect");
                final Card host = sa.getHostCard();
                if ("NonActive".equals(curse) && !player.equals(game.getPhaseHandler().getPlayerTurn())) {
                    return true;
                } else if ("DestroyCreature".equals(curse) && sa.isSpell() && host.isCreature()
                        && !sa.getHostCard().hasKeyword("Indestructible")) {
                    return true;
                } else if ("CounterEnchantment".equals(curse) && sa.isSpell() && host.isEnchantment()
                        && !sa.getHostCard().hasKeyword("CARDNAME can't be countered.")) {
                    return true;
                } else if ("ChaliceOfTheVoid".equals(curse) && sa.isSpell() && !host.hasKeyword("CARDNAME can't be countered.")
                		&& host.getCMC() == c.getCounters(CounterType.CHARGE)) {
                    return true;
                }  else if ("BazaarOfWonders".equals(curse) && sa.isSpell() && !host.hasKeyword("CARDNAME can't be countered.")) {
                	for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
                		if (!card.isToken() && card.getName().equals(host.getName())) {
                			return true;
                		}
                	}
                	for (Card card : game.getCardsIn(ZoneType.Graveyard)) {
                		if (card.getName().equals(host.getName())) {
                			return true;
                		}
                	}
                } 
            }
        }
        return false;
    }

    public boolean checkETBEffects(final Card card, final SpellAbility sa, final ApiType api) {
        boolean rightapi = false;

        if (card.isCreature()
                && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noCreatureETBTriggers)) {
            return api == null;
        }

        // Trigger play improvements
        for (final Trigger tr : card.getTriggers()) {
            // These triggers all care for ETB effects

            final Map<String, String> params = tr.getMapParams();
            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard")) {
                if (!params.get("ValidCard").contains("Self")) {
                    continue;
                }
                if (params.get("ValidCard").contains("notkicked")) {
                    if (sa.isKicked()) {
                        continue;
                    }
                } else if (params.get("ValidCard").contains("kicked")) {
                    if (params.get("ValidCard").contains("kicked ")) { // want a specific kicker
                        String s = params.get("ValidCard").split("kicked ")[1];
                        if ("1".equals(s) && !sa.isOptionalCostPaid(OptionalCost.Kicker1)) continue;
                        if ("2".equals(s) && !sa.isOptionalCostPaid(OptionalCost.Kicker2)) continue;
                    } else if (!sa.isKicked()) { 
                        continue;
                    }
                }
            }

            if (!tr.requirementsCheck(game)) {
                continue;
            }

            if (tr.getOverridingAbility() != null) {
                // Abilities yet
                continue;
            }
            
            // if trigger is not mandatory - no problem
            if (params.get("OptionalDecider") != null && api == null) {
                continue;
            }

            // Maybe better considerations
            final String execute = params.get("Execute");
            if (execute == null) {
                continue;
            }
            final SpellAbility exSA = AbilityFactory.getAbility(card.getSVar(execute), card);

            if (api != null) {
                if (exSA.getApi() != api) {
                    continue;
                } else {
                    rightapi = true;
                }
                if (!(exSA instanceof AbilitySub)) {
                	if (!ComputerUtilCost.canPayCost(exSA, player)) {
                		return false;
                	}
                }
            }

            if (sa != null) {
                exSA.setActivatingPlayer(sa.getActivatingPlayer());
            }
            else {
                exSA.setActivatingPlayer(player);
            }
            exSA.setTrigger(true);

            // Run non-mandatory trigger.
            // These checks only work if the Executing SpellAbility is an Ability_Sub.
            if (exSA instanceof AbilitySub && !doTrigger(exSA, false)) {
                // AI would not run this trigger if given the chance
                return false;
            }
        }
        if (api != null && !rightapi) {
            return false;
        }

        // Replacement effects
        for (final ReplacementEffect re : card.getReplacementEffects()) {
            // These Replacements all care for ETB effects

            final Map<String, String> params = re.getMapParams();
            if (!(re instanceof ReplaceMoved)) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard")) {
                if (!params.get("ValidCard").contains("Self")) {
                    continue;
                }
                if (params.get("ValidCard").contains("notkicked")) {
                    if (sa.isKicked()) {
                        continue;
                    }
                } else if (params.get("ValidCard").contains("kicked")) {
                    if (params.get("ValidCard").contains("kicked ")) { // want a specific kicker
                        String s = params.get("ValidCard").split("kicked ")[1];
                        if ("1".equals(s) && !sa.isOptionalCostPaid(OptionalCost.Kicker1)) continue;
                        if ("2".equals(s) && !sa.isOptionalCostPaid(OptionalCost.Kicker2)) continue;
                    } else if (!sa.isKicked()) { // otherwise just any must be present
                        continue;
                    }
                }
            }

            if (!re.requirementsCheck(game)) {
                continue;
            }
            final SpellAbility exSA = re.getOverridingAbility();

            if (exSA != null) {
                if (sa != null) {
                    exSA.setActivatingPlayer(sa.getActivatingPlayer());
                }
                else {
                    exSA.setActivatingPlayer(player);
                }

                if (exSA.getActivatingPlayer() == null) {
                    throw new InvalidParameterException("Executing SpellAbility for Replacement Effect has no activating player");
                }
            }

            // ETBReplacement uses overriding abilities.
            // These checks only work if the Executing SpellAbility is an Ability_Sub.
            if (exSA != null && (exSA instanceof AbilitySub) && !doTrigger(exSA, false)) {
                return false;
            }
        }
        return true;
    }

    private static List<SpellAbility> getPlayableCounters(final CardCollection l) {
        final List<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getNonManaAbilities()) {
                // Check if this AF is a Counterpsell
                if (sa.getApi() == ApiType.Counter) {
                    spellAbility.add(sa);
                }
            }
        }
        return spellAbility;
    }

    // plays a land if one is available
    public CardCollection getLandsToPlay() {
        final CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        hand.addAll(player.getCardsIn(ZoneType.Exile));
        CardCollection landList = CardLists.filter(hand, Presets.LANDS);
        CardCollection nonLandList = CardLists.filter(hand, Predicates.not(CardPredicates.Presets.LANDS));
        
        //filter out cards that can't be played
        landList = CardLists.filter(landList, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!c.getSVar("NeedsToPlay").isEmpty()) {
                    final String needsToPlay = c.getSVar("NeedsToPlay");
                    CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), needsToPlay.split(","), c.getController(), c, null);
                    if (list.isEmpty()) {
                        return false;
                    }
                }
                return player.canPlayLand(c);
            }
        });
    
        final CardCollection landsNotInHand = new CardCollection(player.getCardsIn(ZoneType.Graveyard));
        landsNotInHand.addAll(game.getCardsIn(ZoneType.Exile));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            landsNotInHand.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        for (final Card crd : landsNotInHand) {
            if (!(crd.isLand() || (crd.isFaceDown() && crd.getState(CardStateName.Original).getType().isLand()))) {
                continue;
            }
            if (crd.hasKeyword("May be played") || crd.mayPlay(player) != null) {
                landList.add(crd);
            }
        }
        if (landList.isEmpty()) {
            return null;
        }
        if (landList.size() == 1 && nonLandList.size() < 3) {
            CardCollectionView cardsInPlay = player.getCardsIn(ZoneType.Battlefield);
            CardCollection landsInPlay = CardLists.filter(cardsInPlay, Presets.LANDS);
            CardCollection allCards = new CardCollection(player.getCardsIn(ZoneType.Graveyard));
            allCards.addAll(player.getCardsIn(ZoneType.Command));
            allCards.addAll(cardsInPlay);
            int maxCmcInHand = Aggregates.max(hand, CardPredicates.Accessors.fnGetCmc);
            int max = Math.max(maxCmcInHand, 6);
            // consider not playing lands if there are enough already and an ability with a discard cost is present
            if (landsInPlay.size() + landList.size() > max) {
                for (Card c : allCards) {
                    for (SpellAbility sa : c.getSpellAbilities()) {
                        if (sa.getPayCosts() != null) {
                            for (CostPart part : sa.getPayCosts().getCostParts()) {
                                if (part instanceof CostDiscard) {
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
        }
    
        landList = CardLists.filter(landList, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                canPlaySpellBasic(c);
                if (c.getType().isLegendary() && !c.getName().equals("Flagstones of Trokair")) {
                    final CardCollectionView list = player.getCardsIn(ZoneType.Battlefield);
                    if (Iterables.any(list, CardPredicates.nameEquals(c.getName()))) {
                        return false;
                    }
                }

                // don't play the land if it has cycling and enough lands are available
                final FCollectionView<SpellAbility> spellAbilities = c.getSpellAbilities();

                final CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
                CardCollection lands = new CardCollection(player.getCardsIn(ZoneType.Battlefield));
                lands.addAll(hand);
                lands = CardLists.filter(lands, CardPredicates.Presets.LANDS);
                int maxCmcInHand = Aggregates.max(hand, CardPredicates.Accessors.fnGetCmc);
                for (final SpellAbility sa : spellAbilities) {
                    if (sa.isCycling()) {
                        if (lands.size() >= Math.max(maxCmcInHand, 6)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        });
        return landList;
    }

    public Card chooseBestLandToPlay(CardCollection landList) {
        if (landList.isEmpty()) {
            return null;
        }
        //Skip reflected lands.
        CardCollection unreflectedLands = new CardCollection(landList);
        for (Card l : landList) {
            if (l.isReflectedLand()) {
                unreflectedLands.remove(l);
            }
        }
        if (!unreflectedLands.isEmpty()) {
            landList = unreflectedLands;
        }

        CardCollection nonLandsInHand = CardLists.filter(player.getCardsIn(ZoneType.Hand), Predicates.not(CardPredicates.Presets.LANDS));

        //try to skip lands that enter the battlefield tapped
        if (!nonLandsInHand.isEmpty()) {
	        CardCollection nonTappeddLands = new CardCollection();
	        for (Card land : landList) {
	            // Is this the best way to check if a land ETB Tapped?
	            if (land.hasSVar("ETBTappedSVar")) {
	                continue;
	            }
	            // Glacial Fortress and friends
	            if (land.hasSVar("ETBCheckSVar") && CardFactoryUtil.xCount(land, land.getSVar("ETBCheckSVar")) == 0) {
	                continue;
	            }
	            nonTappeddLands.add(land);
	        }
	        if (!nonTappeddLands.isEmpty()) {
	            landList = nonTappeddLands;
	        }
        }

        // Choose first land to be able to play a one drop
        if (player.getLandsInPlay().isEmpty()) {
            CardCollection oneDrops = CardLists.filter(nonLandsInHand, CardPredicates.hasCMC(1));
            for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                byte color = MagicColor.WUBRG[i];
                if (!CardLists.filter(oneDrops, CardPredicates.isColor(color)).isEmpty()) {
                    for (Card land : landList) {
                        if (land.getType().hasSubtype(MagicColor.Constant.BASIC_LANDS.get(i))) {
                            return land;
                        }
                        for (final SpellAbility m : ComputerUtilMana.getAIPlayableMana(land)) {
                            AbilityManaPart mp = m.getManaPart();
                            if (mp.canProduce(MagicColor.toShortString(color), m)) {
                                return land;
                            }
                        }
                    }
                }
            }
        }

        //play lands with a basic type that is needed the most
        final CardCollectionView landsInBattlefield = player.getCardsIn(ZoneType.Battlefield);
        final List<String> basics = new ArrayList<String>();

        // what types can I go get?
        for (final String name : MagicColor.Constant.BASIC_LANDS) {
            if (!CardLists.getType(landList, name).isEmpty()) {
                basics.add(name);
            }
        }
        if (!basics.isEmpty()) {
	        // Which basic land is least available
	        int minSize = Integer.MAX_VALUE;
	        String minType = null;

	        for (String b : basics) {
	            final int num = CardLists.getType(landsInBattlefield, b).size();
	            if (num < minSize) {
	                minType = b;
	                minSize = num;
	            }
	        }

	        if (minType != null) {
	            landList = CardLists.getType(landList, minType);
	        }

	        // pick dual lands if available
	        if (Iterables.any(landList, Predicates.not(CardPredicates.Presets.BASIC_LANDS))) {
	            landList = CardLists.filter(landList, Predicates.not(CardPredicates.Presets.BASIC_LANDS));
	        }
        }
        return landList.get(0);
    }

    // if return true, go to next phase
    private SpellAbility chooseCounterSpell(final List<SpellAbility> possibleCounters) {
        if (possibleCounters == null || possibleCounters.isEmpty()) {
            return null;
        }
        SpellAbility bestSA = null;
        int bestRestriction = Integer.MIN_VALUE;

        for (final SpellAbility sa : ComputerUtilAbility.getOriginalAndAltCostAbilities(possibleCounters, player)) {
            SpellAbility currentSA = sa;
            sa.setActivatingPlayer(player);
            // check everything necessary
            
            
            AiPlayDecision opinion = canPlayAndPayFor(currentSA);
            //PhaseHandler ph = game.getPhaseHandler();
            // System.out.printf("Ai thinks '%s' of %s @ %s %s >>> \n", opinion, sa, Lang.getPossesive(ph.getPlayerTurn().getName()), ph.getPhase());
            if (opinion == AiPlayDecision.WillPlay) {
                if (bestSA == null) {
                    bestSA = currentSA;
                    bestRestriction = ComputerUtil.counterSpellRestriction(player, currentSA);
                } else {
                    // Compare bestSA with this SA
                    final int restrictionLevel = ComputerUtil.counterSpellRestriction(player, currentSA);
    
                    if (restrictionLevel > bestRestriction) {
                        bestRestriction = restrictionLevel;
                        bestSA = currentSA;
                    }
                }
            }
        }

        // TODO - "Look" at Targeted SA and "calculate" the threshold
        // if (bestRestriction < targetedThreshold) return false;
        return bestSA;
    }

    public SpellAbility predictSpellToCastInMain2(ApiType exceptSA) {
        return predictSpellToCastInMain2(exceptSA, true);
    }

    public SpellAbility predictSpellToCastInMain2(ApiType exceptSA, boolean handOnly) {
        if (!getBooleanProperty(AiProps.PREDICT_SPELLS_FOR_MAIN2)) {
            return null;
        }

        final CardCollectionView cards = handOnly ? player.getCardsIn(ZoneType.Hand) :
            ComputerUtilAbility.getAvailableCards(game, player);

        List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(cards, player);
        Collections.sort(all, saComparator); // put best spells first

        for (final SpellAbility sa : ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player)) {
            if (sa.getApi() == ApiType.Counter || sa.getApi() == exceptSA) {
                continue;
            }
            sa.setActivatingPlayer(player);
            // TODO: this currently only works as a limited prediction of permanent spells.
            // Ideally this should cast canPlaySa to determine that the AI is truly able/willing to cast a spell,
            // but that is currently difficult to implement due to various side effects leading to stack overflow.
            if (!ComputerUtil.castPermanentInMain1(player, sa) && sa.getHostCard() != null && !sa.getHostCard().isLand() && ComputerUtilCost.canPayCost(sa, player)) {
                if (sa instanceof SpellPermanent) {
                    return sa;
                }
            }
        }
        return null;
    }

    public void reserveManaSourcesForMain2(SpellAbility sa) {
        ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(sa, true, 0);
        CardCollection manaSources = ComputerUtilMana.getManaSourcesToPayCost(cost, sa, player);
        for (Card c : manaSources) {
            ((PlayerControllerAi)player.getController()).getAi().getCardMemory().rememberCard(c, AiCardMemory.MemorySet.HELD_MANA_SOURCES);
        }
    }

    // This is for playing spells regularly (no Cascade/Ripple etc.)
    private AiPlayDecision canPlayAndPayFor(final SpellAbility sa) {
        if (!sa.canPlay()) {
            return AiPlayDecision.CantPlaySa;
        }

        AiPlayDecision op = canPlaySa(sa);
        if (op != AiPlayDecision.WillPlay) {
            return op;
        }
        return ComputerUtilCost.canPayCost(sa, player) ? AiPlayDecision.WillPlay : AiPlayDecision.CantAfford;
    }

    public AiPlayDecision canPlaySa(SpellAbility sa) {
        final Card card = sa.getHostCard();
        if (sa instanceof WrappedAbility) {
            return canPlaySa(((WrappedAbility) sa).getWrappedAbility());
        }
        if (sa.getApi() != null) {
            boolean canPlay = SpellApiToAi.Converter.get(sa.getApi()).canPlayAIWithSubs(player, sa);
            if (!canPlay) {
                return AiPlayDecision.CantPlayAi;
            }
        }
        else if (sa.getPayCosts() != null){
            Cost payCosts = sa.getPayCosts();
            ManaCost mana = payCosts.getTotalMana();
            if (mana!= null && mana.countX() > 0) {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtilMana.determineLeftoverMana(sa, player);
                if (xPay <= 0) {
                    return AiPlayDecision.CantAffordX;
                }
                card.setSVar("PayX", Integer.toString(xPay));
            }
        }
        if (checkCurseEffects(sa)) {
            return AiPlayDecision.CurseEffects;
        }
        if (sa instanceof SpellPermanent) {
            ManaCost mana = sa.getPayCosts().getTotalMana();
            if (mana.countX() > 0) {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtilMana.determineLeftoverMana(sa, player);
                final Card source = sa.getHostCard();
                if (source.hasConverge()) {
                	card.setSVar("PayX", Integer.toString(0));
                	int nColors = ComputerUtilMana.getConvergeCount(sa, player);
                	for (int i = 1; i <= xPay; i++) {
                		card.setSVar("PayX", Integer.toString(i));
                		int newColors = ComputerUtilMana.getConvergeCount(sa, player);
                		if (newColors > nColors) {
                			nColors = newColors;
                		} else {
                			card.setSVar("PayX", Integer.toString(i - 1));
                			break;
                		}
                	}
                } else {
	                if (xPay <= 0) {
	                    return AiPlayDecision.CantAffordX;
	                }
	                card.setSVar("PayX", Integer.toString(xPay));
                }
            }
            
            // Check for valid targets before casting
            if (card.getSVar("OblivionRing").length() > 0) {
                SpellAbility effectExile = AbilityFactory.getAbility(card.getSVar("TrigExile"), card);
                final ZoneType origin = ZoneType.listValueOf(effectExile.getParam("Origin")).get(0);
                final TargetRestrictions tgt = effectExile.getTargetRestrictions();
                final CardCollection list = CardLists.getValidCards(game.getCardsIn(origin), tgt.getValidTgts(), player, card, effectExile);
                CardCollection targets = CardLists.getTargetableCards(list, sa);
                if (sa.getHostCard().getName().equals("Suspension Field")) {
                    //existing "exile until leaves" enchantments only target opponent's permanents
                    final Player ai = sa.getActivatingPlayer();
                    targets = CardLists.filter(targets, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return !c.getController().equals(ai);
                        }
                    });
                }
                if (targets.isEmpty()) {
                    return AiPlayDecision.AnotherTime;
                }
            }
            
            if (sa.hasParam("Announce") && sa.getParam("Announce").startsWith("Multikicker")) {
            	//String announce = sa.getParam("Announce");
                ManaCost mkCost = sa.getMultiKickerManaCost();
                ManaCost mCost = sa.getPayCosts().getTotalMana();
                for (int i = 0; i < 10; i++) {
                	mCost = ManaCost.combine(mCost, mkCost);
                	ManaCostBeingPaid mcbp = new ManaCostBeingPaid(mCost);
                	if (!ComputerUtilMana.canPayManaCost(mcbp, sa, player)) {
                		card.setKickerMagnitude(i);
                		break;
                	}
                	card.setKickerMagnitude(i+1);
                }
            }
            
            // Prevent the computer from summoning Ball Lightning type creatures after attacking
            if (card.hasSVar("EndOfTurnLeavePlay")
                    && (game.getPhaseHandler().isPlayerTurn(player.getOpponent())
                         || game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                         || player.hasKeyword("Skip your next combat phase."))) {
                return AiPlayDecision.AnotherTime;
            }

            // Prevent the computer from summoning Ball Lightning type creatures after attacking
            if (card.hasStartOfKeyword("You may cast CARDNAME as though it had flash. If") && !card.getController().couldCastSorcery(sa)) {
                return AiPlayDecision.AnotherTime;
            }
            
            // Wait for Main2 if possible
            if (game.getPhaseHandler().is(PhaseType.MAIN1)
                    && game.getPhaseHandler().isPlayerTurn(player)
                    && player.getManaPool().totalMana() <= 0
                    && !ComputerUtil.castPermanentInMain1(player, sa)) {
                return AiPlayDecision.WaitForMain2;
            }
            
            // save cards with flash for surprise blocking
            if (card.hasKeyword("Flash")
                    && (player.isUnlimitedHandSize() || player.getCardsIn(ZoneType.Hand).size() <= player.getMaxHandSize()
                    	|| game.getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN))
                    && player.getManaPool().totalMana() <= 0
                    && (game.getPhaseHandler().isPlayerTurn(player)
                            || game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS))
                    && (!card.hasETBTrigger(true) || card.hasSVar("AmbushAI"))
                    && game.getStack().isEmpty()
                    && !ComputerUtil.castPermanentInMain1(player, sa)) {
                return AiPlayDecision.AnotherTime;
            }
            
            // don't play cards without being able to pay the upkeep for
            for (String ability : card.getKeywords()) {
                if (ability.startsWith("At the beginning of your upkeep, sacrifice CARDNAME unless you pay")) {
                    final String[] k = ability.split(" pay ");
                    final String costs = k[1].replaceAll("[{]", "").replaceAll("[}]", " ");
                    Cost cost = new Cost(costs, true);
                    final Ability emptyAbility = new AbilityStatic(card, cost, sa.getTargetRestrictions()) { @Override public void resolve() { } };
                    emptyAbility.setActivatingPlayer(player);
                    if (!ComputerUtilCost.canPayCost(emptyAbility, player)) {
                    	return AiPlayDecision.AnotherTime;
                    }
                }
            }
            return canPlayFromEffectAI((SpellPermanent)sa, false, true);
        }
        if (sa instanceof Spell) {
            
            if (ComputerUtil.getDamageForPlaying(player, sa) >= player.getLife() 
            		&& !player.cantLoseForZeroOrLessLife() && player.canLoseLife()) {
                return AiPlayDecision.CurseEffects;
            }
            return canPlaySpellBasic(card);
        }
        return AiPlayDecision.WillPlay;
    }

    private AiPlayDecision canPlaySpellBasic(final Card card) {
        if (card.hasSVar("NeedsToPlay")) {
            final String needsToPlay = card.getSVar("NeedsToPlay");
            CardCollectionView list = game.getCardsIn(ZoneType.Battlefield);

            list = CardLists.getValidCards(list, needsToPlay.split(","), card.getController(), card, null);
            if (list.isEmpty()) {
                return AiPlayDecision.MissingNeededCards;
            }
        }
        if (card.getSVar("NeedsToPlayVar").length() > 0) {
            final String needsToPlay = card.getSVar("NeedsToPlayVar");
            int x = 0;
            int y = 0;
            String sVar = needsToPlay.split(" ")[0];
            String comparator = needsToPlay.split(" ")[1];
            String compareTo = comparator.substring(2);
            try {
                x = Integer.parseInt(sVar);
            } catch (final NumberFormatException e) {
                x = CardFactoryUtil.xCount(card, card.getSVar(sVar));
            }
            try {
                y = Integer.parseInt(compareTo);
            } catch (final NumberFormatException e) {
                y = CardFactoryUtil.xCount(card, card.getSVar(compareTo));
            }
            if (!Expressions.compare(x, comparator, y)) {
                return AiPlayDecision.NeedsToPlayCriteriaNotMet;
            }
        }
        return AiPlayDecision.WillPlay;
    }
    
    // not sure "playing biggest spell" matters?
    private final static Comparator<SpellAbility> saComparator = new Comparator<SpellAbility>() {
        @Override
        public int compare(final SpellAbility a, final SpellAbility b) {
            // sort from highest cost to lowest
            // we want the highest costs first
            int a1 = a.getPayCosts() == null ? 0 : a.getPayCosts().getTotalMana().getCMC();
            int b1 = b.getPayCosts() == null ? 0 : b.getPayCosts().getTotalMana().getCMC();

            // deprioritize planar die roll marked with AIRollPlanarDieParams:LowPriority$ True
            if (ApiType.RollPlanarDice == a.getApi() && a.getHostCard().hasSVar("AIRollPlanarDieParams") && a.getHostCard().getSVar("AIRollPlanarDieParams").toLowerCase().matches(".*lowpriority\\$\\s*true.*")) {
                return 1;
            } else if (ApiType.RollPlanarDice == b.getApi() && b.getHostCard().hasSVar("AIRollPlanarDieParams") && b.getHostCard().getSVar("AIRollPlanarDieParams").toLowerCase().matches(".*lowpriority\\$\\s*true.*")) {
                return -1;
            }
    
            // cast 0 mana cost spells first (might be a Mox)
            if (a1 == 0 && b1 > 0 && ApiType.Mana != a.getApi()) {
                return -1;
            } else if (a1 > 0 && b1 == 0 && ApiType.Mana != b.getApi()) {
                return 1;
            }
            
            if (a.getHostCard().hasSVar("FreeSpellAI")) {
            	return -1;
            } else if (b.getHostCard().hasSVar("FreeSpellAI")) {
            	return 1;
            }

            a1 += getSpellAbilityPriority(a);
            b1 += getSpellAbilityPriority(b);

            return b1 - a1;
        }
        
        private int getSpellAbilityPriority(SpellAbility sa) {
            int p = 0;
            Card source = sa.getHostCard();
            final Player ai = source.getController();
            final boolean noCreatures = ai.getCreaturesInPlay().isEmpty();
            // puts creatures in front of spells
            if (source.isCreature()) {
                p += 1;
            }
            // don't play equipments before having any creatures
            if (source.isEquipment() && noCreatures) {
                p -= 9;
            }
            // use Surge and Prowl costs when able to
            if (sa.isSurged() || 
            		(sa.getRestrictions().getProwlTypes() != null && !sa.getRestrictions().getProwlTypes().isEmpty())) {
                p += 9;
            }
            // 1. increase chance of using Surge effects
            // 2. non-surged versions are usually inefficient
            if (sa.getHostCard().getOracleText().contains("surge cost") && !sa.isSurged()) {
                p -= 9;
            }
            // move snap-casted spells to front
            if (source.isInZone(ZoneType.Graveyard) && source.hasKeyword("May be played")) {
                p += 50;
            }
            // artifacts and enchantments with effects that do not stack
            if ("True".equals(source.getSVar("NonStackingEffect")) && ai.isCardInPlay(source.getName())) {
                p -= 9;
            }
            // sort planeswalker abilities with most costly first
            if (sa.getRestrictions().isPwAbility()) {
                final CostPart cost = sa.getPayCosts().getCostParts().get(0);
                if (cost instanceof CostRemoveCounter) {
                    p += cost.convertAmount() == null ? 1 : cost.convertAmount();
                } else if (cost instanceof CostPutCounter) {
                    p -= cost.convertAmount();
                }
                if (sa.hasParam("Ultimate")) {
                    p += 9;
                }
            }

            if (ApiType.DestroyAll == sa.getApi()) {
                p += 4;
            }

            else if (ApiType.Mana == sa.getApi()) {
                p -= 9;
            }

            return p;
        }
    };

    public CardCollection getCardsToDiscard(final int numDiscard, final String[] uTypes, final SpellAbility sa) {
        CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        if ((uTypes != null) && (sa != null)) {
            hand = CardLists.getValidCards(hand, uTypes, sa.getActivatingPlayer(), sa.getHostCard(), sa);
        }
        return getCardsToDiscard(numDiscard, numDiscard, hand, sa);
    }

    public CardCollection getCardsToDiscard(int min, final int max, final CardCollection validCards, final SpellAbility sa) {
        if (validCards.size() < min) {
            return null;
        }

        Card sourceCard = null;
        final CardCollection discardList = new CardCollection();
        int count = 0;
        if (sa != null) {
            sourceCard = sa.getHostCard();
            if ("Always".equals(sa.getParam("AILogic")) && !validCards.isEmpty()) {
            	min = 1;
            }
        }
    
        // look for good discards
        while (count < min) {
            Card prefCard = null;
            if (sa != null && sa.getActivatingPlayer() != null && sa.getActivatingPlayer().isOpponentOf(player)) {
                for (Card c : validCards) {
                    if (c.hasKeyword("If a spell or ability an opponent controls causes you to discard CARDNAME,"
                            + " put it onto the battlefield instead of putting it into your graveyard.")
                            || !c.getSVar("DiscardMeByOpp").isEmpty()) {
                        prefCard = c;
                        break;
                    }
                }
            }
            if (prefCard == null) {
                prefCard = ComputerUtil.getCardPreference(player, sourceCard, "DiscardCost", validCards);
            }
            if (prefCard != null) {
                discardList.add(prefCard);
                validCards.remove(prefCard);
                count++;
            }
            else {
                break;
            }
        }

        final int discardsLeft = min - count;

        // choose rest
        for (int i = 0; i < discardsLeft; i++) {
            if (validCards.isEmpty()) {
                continue;
            }
            final int numLandsInPlay = Iterables.size(Iterables.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.LANDS));
            final CardCollection landsInHand = CardLists.filter(validCards, CardPredicates.Presets.LANDS);
            final int numLandsInHand = landsInHand.size();
    
            // Discard a land
            boolean canDiscardLands = numLandsInHand > 3  || (numLandsInHand > 2 && numLandsInPlay > 0)
            || (numLandsInHand > 1 && numLandsInPlay > 2) || (numLandsInHand > 0 && numLandsInPlay > 5);
    
            if (canDiscardLands) {
                discardList.add(landsInHand.get(0));
                validCards.remove(landsInHand.get(0));
            }
            else { // Discard other stuff
                CardLists.sortByCmcDesc(validCards);
                int numLandsAvailable = numLandsInPlay;
                if (numLandsInHand > 0) {
                    numLandsAvailable++;
                }
                //Discard unplayable card
                if (validCards.get(0).getCMC() > numLandsAvailable) {
                    discardList.add(validCards.get(0));
                    validCards.remove(validCards.get(0));
                }
                else { //Discard worst card
                    Card worst = ComputerUtilCard.getWorstAI(validCards);
                    discardList.add(worst);
                    validCards.remove(worst);
                }
            }
        }
        return discardList;
    }

    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        ApiType api = sa.getApi();

        // Abilities without api may also use this routine, However they should provide a unique mode value
        if (api == null) {
            if (sa instanceof SpellPermanent) {
                Card card = sa.getHostCard();
                if (card.isCreature()) {
                    api = ApiType.PermanentCreature;
                } else {
                    api = ApiType.PermanentNoncreature;
                }
            } else {
                String exMsg = String.format("AI confirmAction does not know what to decide about %s mode (api is null).", mode);
                throw new IllegalArgumentException(exMsg);
            }
        }
        return SpellApiToAi.Converter.get(api).confirmAction(player, sa, mode, message);
    }

    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, int bid, Player winner) {
        if (mode != null) switch (mode) {
            case BidLife:
                if (sa.hasParam("AIBidMax")) {
                    return !player.equals(winner) && bid < Integer.parseInt(sa.getParam("AIBidMax")) && player.getLife() > bid + 5;
                }
                return false;
            default:
                return false;
        } 
        return false;
    }

    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        if (logic.equalsIgnoreCase("ProtectFriendly")) {
            final Player controller = hostCard.getController();
            if (affected instanceof Player) {
                return !((Player) affected).isOpponentOf(controller);
            }
            if (affected instanceof Card) {
                return !((Card) affected).getController().isOpponentOf(controller);
            }
        }
        return true;
    }

    public String getProperty(AiProps propName) {
        return AiProfileUtil.getAIProp(getPlayer().getLobbyPlayer(), propName);
    }

    public int getIntProperty(AiProps propName) {
        String prop = AiProfileUtil.getAIProp(getPlayer().getLobbyPlayer(), propName);

        if (prop == null || prop.equals("")) {
            return Integer.parseInt(propName.getDefault());
        }

        return Integer.parseInt(prop);
    }

    public boolean getBooleanProperty(AiProps propName) {
        String prop = AiProfileUtil.getAIProp(getPlayer().getLobbyPlayer(), propName);

        if (prop == null || prop.equals("")) {
            return Boolean.parseBoolean(propName.getDefault());
        }

        return Boolean.parseBoolean(prop);
    }

    /** Returns the spell ability which has already been played - use it for reference only */ 
    public SpellAbility chooseAndPlaySa(boolean mandatory, boolean withoutPayingManaCost, final SpellAbility... list) {
        return chooseAndPlaySa(Arrays.asList(list), mandatory, withoutPayingManaCost);
    }
    /** Returns the spell ability which has already been played - use it for reference only */
    public SpellAbility chooseAndPlaySa(final List<SpellAbility> choices, boolean mandatory, boolean withoutPayingManaCost) {
        for (final SpellAbility sa : choices) {
            sa.setActivatingPlayer(player);
            //Spells
            if (sa instanceof Spell) {
                if (AiPlayDecision.WillPlay != canPlayFromEffectAI((Spell) sa, mandatory, withoutPayingManaCost)) {
                    continue;
                }
            }
            else {
                if (AiPlayDecision.WillPlay == canPlaySa(sa)) {
                    continue;
                }
            }

            if (withoutPayingManaCost) {
                ComputerUtil.playSpellAbilityWithoutPayingManaCost(player, sa, game);
            }
            else if (!ComputerUtilCost.canPayCost(sa, player)) {
                continue;
            }
            else {
                ComputerUtil.playStack(sa, player, game);
            }
            return sa;
        }
        return null;
    }

    public AiPlayDecision canPlayFromEffectAI(Spell spell, boolean mandatory, boolean withoutPayingManaCost) {
        final Card card = spell.getHostCard();
        
        int damage = ComputerUtil.getDamageForPlaying(player, spell);
        
        if (damage >= player.getLife() && !player.cantLoseForZeroOrLessLife() && player.canLoseLife()) {
            return AiPlayDecision.CurseEffects;
        }

        if (spell instanceof SpellApiBased) {
            boolean chance = false;
            if (withoutPayingManaCost) {
                chance = SpellApiToAi.Converter.get(spell.getApi()).doTriggerNoCostWithSubs(player, spell, mandatory);
            } else {
                chance = SpellApiToAi.Converter.get(spell.getApi()).doTriggerAI(player, spell, mandatory);
            }
            if (!chance)
                return AiPlayDecision.TargetingFailed;

            return canPlaySpellBasic(card);
        }
        
        if (spell instanceof SpellPermanent) {
            if (mandatory) {
                return AiPlayDecision.WillPlay;
            }
            ManaCost mana = spell.getPayCosts().getTotalMana();
            final Cost cost = spell.getPayCosts();
    
            if (cost != null) {
                // AI currently disabled for these costs
                if (!ComputerUtilCost.checkLifeCost(player, cost, card, 4, null)) {
                    return AiPlayDecision.CostNotAcceptable;
                }
    
                if (!ComputerUtilCost.checkDiscardCost(player, cost, card)) {
                    return AiPlayDecision.CostNotAcceptable;
                }
    
                if (!ComputerUtilCost.checkSacrificeCost(player, cost, card)) {
                    return AiPlayDecision.CostNotAcceptable;
                }
    
                if (!ComputerUtilCost.checkRemoveCounterCost(cost, card)) {
                    return AiPlayDecision.CostNotAcceptable;
                }
            }
    
            // check on legendary
            if (card.getType().isLegendary() && !game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {
                if (player.isCardInPlay(card.getName())) {
                    return AiPlayDecision.WouldDestroyLegend;
                }
            }
            if (card.isPlaneswalker()) {
                CardCollection list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS);
                for (String type : card.getType().getSubtypes()) { //determine planewalker subtype
                    final CardCollection cl = CardLists.getType(list, type);
                    if (!cl.isEmpty()) {
                        return AiPlayDecision.WouldDestroyOtherPlaneswalker;
                    }
                    break;
                }
            }
            if (card.getType().hasSupertype(Supertype.World)) {
                CardCollection list = CardLists.getType(player.getCardsIn(ZoneType.Battlefield), "World");
                if (!list.isEmpty()) {
                    return AiPlayDecision.WouldDestroyWorldEnchantment;
                }
            }

            if (card.isCreature()) {
                /*
                 * Checks if the creature will have non-positive toughness
                 * after applying static effects. Exceptions:
                 *  1. has "etbCounter" keyword (eg. Endless One)
                 *  2. paid non-zero for X cost
                 *  3. has ETB trigger
                 *  4. has ETB replacement
                 *  5. has NoZeroToughnessAI svar (eg. Veteran Warleader)
                 *  
                 *  1. and 2. should probably be merged and applied on the
                 *  card after checking for effects like Doubling Season for
                 *  getNetToughness to see the true value.
                 *  3. currently allows the AI to suicide creatures as long as
                 *  it has an ETB. Maybe it should check if said ETB is
                 *  actually worth it.
                 *  Not sure what 4. is for.
                 *  5. needs to be updated to ensure that the net toughness is
                 *  still positive after static effects.
                 */
                final Card creature = CardFactory.copyCard(card, true);
                ComputerUtilCard.applyStaticContPT(game, creature, null);
                if (creature.getNetToughness() <= 0 && !creature.hasStartOfKeyword("etbCounter") && mana.countX() == 0
                        && !creature.hasETBTrigger(false) && !creature.hasETBReplacement()
                        && !creature.hasSVar("NoZeroToughnessAI")) {
                    return AiPlayDecision.WouldBecomeZeroToughnessCreature;
                }
            }

            if (!checkETBEffects(card, spell, null)) {
                return AiPlayDecision.BadEtbEffects;
            }
            if (damage + ComputerUtil.getDamageFromETB(player, card) >= player.getLife() && !player.cantLoseForZeroOrLessLife() 
                    && player.canLoseLife()) {
                return AiPlayDecision.BadEtbEffects;
            }
        }
        return canPlaySpellBasic(card);
    }

    public List<SpellAbility> chooseSpellAbilityToPlay() {
        final PhaseType phase = game.getPhaseHandler().getPhase();

        if (game.getStack().isEmpty() && phase.isMain()) {
            Log.debug("Computer " + phase.nameForUi);
            CardCollection landsWannaPlay = getLandsToPlay();
            if (landsWannaPlay != null && !landsWannaPlay.isEmpty() && player.canPlayLand(null)) {
                Card land = chooseBestLandToPlay(landsWannaPlay);
                if (ComputerUtil.getDamageFromETB(player, land) < player.getLife() || !player.canLoseLife() 
                        || player.cantLoseForZeroOrLessLife() ) {
                    game.PLAY_LAND_SURROGATE.setHostCard(land);
                    final List<SpellAbility> abilities = new ArrayList<SpellAbility>();
                    abilities.add(game.PLAY_LAND_SURROGATE);
                    return abilities;
                }
            }
        }

        SpellAbility sa = getSpellAbilityToPlay();
        if (sa == null) { return null; }

        // System.out.println("Chosen to play: " + sa);

        final List<SpellAbility> abilities = new ArrayList<SpellAbility>();
        abilities.add(sa);
        return abilities;
    }

    // declares blockers for given defender in a given combat
    public void declareBlockersFor(Player defender, Combat combat) {
        AiBlockController block = new AiBlockController(defender);
        // When player != defender, AI should declare blockers for its benefit.
        block.assignBlockersForCombat(combat);
    }
    

    public void declareAttackers(Player attacker, Combat combat) {
        // 12/2/10(sol) the decision making here has moved to getAttackers()
        AiAttackController aiAtk = new AiAttackController(attacker); 
        aiAtk.declareAttackers(combat);

        // if invalid: just try an attack declaration that we know to be legal
        if (!CombatUtil.validateAttackers(combat)) {
            combat.clearAttackers();
            final Map<Card, GameEntity> legal = combat.getAttackConstraints().getLegalAttackers().getLeft();
            System.err.println("AI Attack declaration invalid, defaulting to: " + legal);
            for (final Map.Entry<Card, GameEntity> mandatoryAttacker : legal.entrySet()) {
                combat.addAttacker(mandatoryAttacker.getKey(), mandatoryAttacker.getValue());
            }
            if (!CombatUtil.validateAttackers(combat)) {
                aiAtk.declareAttackers(combat);
            }
        }

        for (final Card element : combat.getAttackers()) {
            // tapping of attackers happens after Propaganda is paid for
            final StringBuilder sb = new StringBuilder();
            sb.append("Computer just assigned ").append(element.getName()).append(" as an attacker.");
            Log.debug(sb.toString());
        }
    }

    private final SpellAbility getSpellAbilityToPlay() {
        // if top of stack is owned by me
        if (!game.getStack().isEmpty() && game.getStack().peekAbility().getActivatingPlayer().equals(player)) {
            // probably should let my stuff resolve
            return null;
        }
        final CardCollection cards = ComputerUtilAbility.getAvailableCards(game, player);
    
        if (!game.getStack().isEmpty()) {
            SpellAbility counter = chooseCounterSpell(getPlayableCounters(cards));
            if (counter != null) return counter;
    
            SpellAbility counterETB = chooseSpellAbilityToPlay(this.getPossibleETBCounters(), false);
            if (counterETB != null)
                return counterETB;
        }
    
        SpellAbility result = chooseSpellAbilityToPlay(ComputerUtilAbility.getSpellAbilities(cards, player), true);
        if (null == result) 
            return null;
        return result;
    }
    
    private SpellAbility chooseSpellAbilityToPlay(final List<SpellAbility> all, boolean skipCounter) {
        if (all == null || all.isEmpty())
            return null;
        
        if (useSimulation) {
            return simPicker.chooseSpellAbilityToPlay(null, all, skipCounter);
        }
        
        Collections.sort(all, saComparator); // put best spells first
        
        for (final SpellAbility sa : ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player)) {
            // Don't add Counterspells to the "normal" playcard lookups
            if (skipCounter && sa.getApi() == ApiType.Counter) {
                continue;
            }
            sa.setActivatingPlayer(player);
            
            AiPlayDecision opinion = canPlayAndPayFor(sa);
            // PhaseHandler ph = game.getPhaseHandler();
            // System.out.printf("Ai thinks '%s' of %s -> %s @ %s %s >>> \n", opinion, sa.getHostCard(), sa, Lang.getPossesive(ph.getPlayerTurn().getName()), ph.getPhase());
            
            if (opinion != AiPlayDecision.WillPlay)
                continue;
    
            return sa;
        }
        
        return null;
    }     

    public CardCollection chooseCardsToDelve(int genericCost, CardCollection grave) {
        CardCollection toExile = new CardCollection();
        int numToExile = Math.min(grave.size(), genericCost);
        
        for (int i = 0; i < numToExile; i++) {
            Card chosen = null;
            for (final Card c : grave) { // Exile noncreatures first in
                // case we can revive. Might
                // wanna do some additional
                // checking here for Flashback
                // and the like.
                if (!c.isCreature()) {
                    chosen = c;
                    break;
                }
            }
            if (chosen == null) {
                chosen = ComputerUtilCard.getWorstCreatureAI(grave);
            }

            if (chosen == null) {
                // Should never get here but... You know how it is.
                chosen = grave.get(0);
            }

            toExile.add(chosen);
            grave.remove(chosen);
        }
        return toExile;
    }
    
    public boolean doTrigger(SpellAbility spell, boolean mandatory) {
        
        if (spell.getApi() != null)
            return SpellApiToAi.Converter.get(spell.getApi()).doTriggerAI(player, spell, mandatory);
        if (spell instanceof WrappedAbility)
            return doTrigger(((WrappedAbility)spell).getWrappedAbility(), mandatory);
        
        return false;
    }
    
    /**
     * Ai should run.
     *
     * @param sa the sa
     * @return true, if successful
     */
    public final boolean aiShouldRun(final ReplacementEffect effect, final SpellAbility sa) {
        Card hostCard = effect.getHostCard();
        if (effect.getMapParams().containsKey("AICheckSVar")) {
            System.out.println("aiShouldRun?" + sa);
            final String svarToCheck = effect.getMapParams().get("AICheckSVar");
            String comparator = "GE";
            int compareTo = 1;

            if (effect.getMapParams().containsKey("AISVarCompare")) {
                final String fullCmp = effect.getMapParams().get("AISVarCompare");
                comparator = fullCmp.substring(0, 2);
                final String strCmpTo = fullCmp.substring(2);
                try {
                    compareTo = Integer.parseInt(strCmpTo);
                } catch (final Exception ignored) {
                    if (sa == null) {
                        compareTo = CardFactoryUtil.xCount(hostCard, hostCard.getSVar(strCmpTo));
                    } else {
                        compareTo = AbilityUtils.calculateAmount(hostCard, hostCard.getSVar(strCmpTo), sa);
                    }
                }
            }

            int left = 0;

            if (sa == null) {
                left = CardFactoryUtil.xCount(hostCard, hostCard.getSVar(svarToCheck));
            } else {
                left = AbilityUtils.calculateAmount(hostCard, svarToCheck, sa);
            }
            System.out.println("aiShouldRun?" + left + comparator + compareTo);
            if (Expressions.compare(left, comparator, compareTo)) {
                return true;
            }
        } else if (effect.getMapParams().containsKey("AICheckDredge")) {
            return player.getCardsIn(ZoneType.Library).size() > 8 || player.isCardInPlay("Laboratory Maniac");
        } else if (sa != null && doTrigger(sa, false)) {
            return true;
        }

        return false;
    }    
    
    
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        // AI would play everything. But limits to one copy of (Leyline of Singularity) and (Gemstone Caverns)
        
        List<SpellAbility> result = new ArrayList<SpellAbility>();
        for(SpellAbility sa : usableFromOpeningHand) {
            // Is there a better way for the AI to decide this?
            if (doTrigger(sa, false)) {
                result.add(sa);
            }
        }
        
        boolean hasLeyline1 = false;
        SpellAbility saGemstones = null;
        
        for(int i = 0; i < result.size(); i++) {
            SpellAbility sa = result.get(i);
            
            String srcName = sa.getHostCard().getName();
            if ("Gemstone Caverns".equals(srcName)) {
                if (saGemstones == null)
                    saGemstones = sa;
                else
                    result.remove(i--);
            } else if ("Leyline of Singularity".equals(srcName)) {
                if (!hasLeyline1)
                    hasLeyline1 = true;
                else
                    result.remove(i--);
            }
        }
        
        // Play them last
        if (saGemstones != null) {
            result.remove(saGemstones);
            result.add(saGemstones);
        }
        
        return result;
    }

    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        final String logic = sa.getParam("AILogic");
        if ("GainLife".equals(logic)) {
            if (player.getLife() < 5 || player.getCardsIn(ZoneType.Hand).size() >= player.getMaxHandSize()) {
                return min;
            }
        } else if ("LoseLife".equals(logic)) {
            if (player.getLife() > 5) {
                return min;
            }
        } else if ("Min".equals(logic)) {
            return min;
        } else if ("DigACard".equals(logic)) {
            int random = MyRandom.getRandom().nextInt(Math.min(4, max)) + 1;
            if (player.getLife() < random + 5) {
                return min;
            } else {
                return random;
            }
        } else if ("Damnation".equals(logic)) {
            int chosenMax = player.getLife() - 1;
            int cardsInPlay = player.getCardsIn(ZoneType.Battlefield).size();
            return Math.min(chosenMax, cardsInPlay);
        } else if ("OptionalDraw".equals(logic)) {
            int cardsInHand = player.getCardsIn(ZoneType.Hand).size();
            int maxDraw = Math.min(player.getMaxHandSize() + 2 - cardsInHand, max);
            int maxCheckLib = Math.min(maxDraw, player.getCardsIn(ZoneType.Library).size());
            return Math.max(min, maxCheckLib);
        } else if ("RepeatDraw".equals(logic)) {
            int remaining = player.getMaxHandSize() - player.getCardsIn(ZoneType.Hand).size()
                    + MyRandom.getRandom().nextInt(3);
            return Math.max(remaining, min) / 2;
        } else if ("LowestLoseLife".equals(logic)) {
            return MyRandom.getRandom().nextInt(Math.min(player.getLife() / 3, player.getOpponent().getLife())) + 1;
        } else if ("HighestGetCounter".equals(logic)) {
            return MyRandom.getRandom().nextInt(3);
        }
        return max;
    }

    public boolean confirmPayment(CostPart costPart) {
        throw new UnsupportedOperationException("AI is not supposed to reach this code at the moment");
    }

    public Map<GameEntity, CounterType> chooseProliferation() {
        final Map<GameEntity, CounterType> result = new HashMap<>();  
        
        final List<Player> allies = player.getAllies();
        allies.add(player);
        final List<Player> enemies = player.getOpponents();
        final Function<Card, CounterType> predProliferate = new Function<Card, CounterType>() {
            @Override
            public CounterType apply(Card crd) {
                for (final Entry<CounterType, Integer> c1 : crd.getCounters().entrySet()) {
                    if (ComputerUtil.isNegativeCounter(c1.getKey(), crd) && enemies.contains(crd.getController())) {
                        return c1.getKey();
                    }
                    if (!ComputerUtil.isNegativeCounter(c1.getKey(), crd) && allies.contains(crd.getController())) {
                        return c1.getKey();
                    }
                }
                return null;
            }
        };

        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            CounterType ct = predProliferate.apply(c);
            if (ct != null)
                result.put(c, ct);
        }
        
        for (Player e : enemies) {
            // TODO In the future check of enemies can get poison counters and give them some other bad counter type
            if (e.getCounters(CounterType.POISON) > 0) {
                result.put(e, CounterType.POISON);
            }
        }

        for (Player pl : allies) {
            if (pl.getCounters(CounterType.EXPERIENCE) > 0) {
                result.put(pl, CounterType.EXPERIENCE);
            }
        }

        return result;
    }

    public CardCollection chooseCardsForEffect(CardCollectionView pool, SpellAbility sa, int min, int max, boolean isOptional) {
        if (sa == null || sa.getApi() == null) {
            throw new UnsupportedOperationException();
        }
        CardCollection result = new CardCollection();
        switch(sa.getApi()) {
            case TwoPiles:
                // TODO: improve AI
                Card biggest = null;
                Card smallest = null;
                biggest = pool.get(0);
                smallest = pool.get(0);

                for (Card c : pool) {
                    if (c.getCMC() >= biggest.getCMC()) {
                        biggest = c;
                    } else if (c.getCMC() <= smallest.getCMC()) {
                        smallest = c;
                    }
                }
                result.add(biggest);

                if (max > 3 && !result.contains(smallest)) {
                    result.add(smallest);
                }
                break;
            case MultiplePiles:
                // Whims of the Fates {all, 0, 0}
                result.addAll(pool);
                break;
            default:
                CardCollection editablePool = new CardCollection(pool);
                for (int i = 0; i < max; i++) {
                    Card c = player.getController().chooseSingleEntityForEffect(editablePool, sa, null, isOptional);
                    if (c != null) {
                        result.add(c);
                        editablePool.remove(c);
                    } else {
                        break;
                    }
                }
        }
        return result;
        
    }

    public Collection<? extends PaperCard> complainCardsCantPlayWell(Deck myDeck) {
        List<PaperCard> result = new ArrayList<PaperCard>();
        for (Entry<DeckSection, CardPool> ds : myDeck) {
            for (Entry<PaperCard, Integer> cp : ds.getValue()) {
                if (cp.getKey().getRules().getAiHints().getRemAIDecks()) 
                    result.add(cp.getKey());
            }
        }
        return result;
    }


    // this is where the computer cheats
    // changes AllZone.getComputerPlayer().getZone(Zone.Library)
    
    /**
     * <p>
     * smoothComputerManaCurve.
     * </p>
     * 
     * @param in
     *            an array of {@link forge.game.card.Card} objects.
     * @return an array of {@link forge.game.card.Card} objects.
     */
    public CardCollectionView cheatShuffle(CardCollectionView in) {
        if (in.size() < 20 || !canCheatShuffle()) {
            return in;
        }

        final CardCollection library = new CardCollection(in);
        CardLists.shuffle(library);
    
        // remove all land, keep non-basicland in there, shuffled
        CardCollection land = CardLists.filter(library, CardPredicates.Presets.LANDS);
        for (Card c : land) {
            if (c.isLand()) {
                library.remove(c);
            }
        }
    
        try {
            // mana weave, total of 7 land
            // The Following have all been reduced by 1, to account for the
            // computer starting first.
            library.add(5, land.get(0));
            library.add(6, land.get(1));
            library.add(8, land.get(2));
            library.add(9, land.get(3));
            library.add(10, land.get(4));
    
            library.add(12, land.get(5));
            library.add(15, land.get(6));
        } catch (final IndexOutOfBoundsException e) {
            System.err.println("Error: cannot smooth mana curve, not enough land");
            return in;
        }
    
        // add the rest of land to the end of the deck
        for (int i = 0; i < land.size(); i++) {
            if (!library.contains(land.get(i))) {
                library.add(land.get(i));
            }
        }
    
        // check
        for (int i = 0; i < library.size(); i++) {
            System.out.println(library.get(i));
        }
    
        return library;
    } // smoothComputerManaCurve()

    
    public int chooseNumber(SpellAbility sa, String title,List<Integer> options, Player relatedPlayer) {
        switch(sa.getApi())
        {
            case SetLife:
                if (relatedPlayer.equals(sa.getHostCard().getController())) {
                    return Collections.max(options);
                } else if (relatedPlayer.isOpponentOf(sa.getHostCard().getController())) {
                    return Collections.min(options);
                } else {
                    return options.get(0);
                }
            default:
                return 0;
        }
    }

    public boolean chooseDirection(SpellAbility sa) {
        if (sa == null || sa.getApi() == null) {
            throw new UnsupportedOperationException();
        }
        // Left:True; Right:False
        if ("GainControl".equals(sa.getParam("AILogic")) && game.getPlayers().size() > 2) {
            CardCollection creats = CardLists.getType(game.getCardsIn(ZoneType.Battlefield), "Creature");
            CardCollection left = CardLists.filterControlledBy(creats, game.getNextPlayerAfter(player, Direction.Left));
            CardCollection right = CardLists.filterControlledBy(creats, game.getNextPlayerAfter(player, Direction.Right));
            if (!left.isEmpty() || !right.isEmpty()) {
                CardCollection all = new CardCollection(left);
                all.addAll(right);
                return left.contains(ComputerUtilCard.getBestCreatureAI(all));
            }
        }
        return MyRandom.getRandom().nextBoolean();
    }

}

