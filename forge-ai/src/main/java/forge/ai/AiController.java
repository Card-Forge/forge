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

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.ai.ability.ChangeZoneAi;
import forge.ai.ability.ExploreAi;
import forge.ai.ability.LearnAi;
import forge.ai.simulation.SpellAbilityPicker;
import forge.card.CardStateName;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.*;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellApiBased;
import forge.game.card.*;
import forge.game.card.CardPredicates.Accessors;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.*;
import forge.game.keyword.Keyword;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.replacement.ReplaceMoved;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.ComparatorUtil;
import forge.util.Expressions;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * AiController class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AiController {
    private final Player player;
    private final Game game;
    private final AiCardMemory memory;
    private Combat predictedCombat;
    private Combat predictedCombatNextTurn;
    private boolean cheatShuffle;
    private boolean useSimulation;
    private SpellAbilityPicker simPicker;

    public boolean canCheatShuffle() {
        return cheatShuffle;
    }

    public void allowCheatShuffle(boolean canCheatShuffle) {
        this.cheatShuffle = canCheatShuffle;
    }

    public boolean usesSimulation() {
        return this.useSimulation;
    }
    public void setUseSimulation(boolean value) {
        this.useSimulation = value;
    }

    public SpellAbilityPicker getSimulationPicker() {
        return simPicker;
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

    public Combat getPredictedCombat() {
        if (predictedCombat == null) {
            AiAttackController aiAtk = new AiAttackController(player);
            predictedCombat = new Combat(player);
            aiAtk.declareAttackers(predictedCombat);
        }
        return predictedCombat;
    }

    public Combat getPredictedCombatNextTurn() {
        if (predictedCombatNextTurn == null) {
            AiAttackController aiAtk = new AiAttackController(player, true);
            predictedCombatNextTurn = new Combat(player);
            aiAtk.declareAttackers(predictedCombatNextTurn);
        }
        return predictedCombatNextTurn;
    }

    public AiController(final Player computerPlayer, final Game game0) {
        player = computerPlayer;
        game = game0;
        memory = new AiCardMemory();
        simPicker = new SpellAbilityPicker(game, player);
    }

    private List<SpellAbility> getPossibleETBCounters() {
        CardCollection all = new CardCollection(player.getCardsIn(ZoneType.Hand));
        CardCollectionView ccvPlayerLibrary = player.getCardsIn(ZoneType.Library);
        
        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        if (!ccvPlayerLibrary.isEmpty()) {
            all.add(ccvPlayerLibrary.get(0));
        }

        for (final Player opp : player.getOpponents()) {
            all.addAll(opp.getCardsIn(ZoneType.Exile));
        }

        final List<SpellAbility> spellAbilities = Lists.newArrayList();
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
        CardCollectionView ccvGameBattlefield = game.getCardsIn(ZoneType.Battlefield);
        for (final Card c : ccvGameBattlefield) {
            if (c.hasSVar("AICurseEffect")) {
                final String curse = c.getSVar("AICurseEffect");
                if ("NonActive".equals(curse) && !player.equals(game.getPhaseHandler().getPlayerTurn())) {
                    return true;
                } else {
                    final Card host = sa.getHostCard();
                    if ("DestroyCreature".equals(curse) && sa.isSpell() && host.isCreature()
                            && !host.hasKeyword(Keyword.INDESTRUCTIBLE)) {
                        return true;
                    } else if ("CounterEnchantment".equals(curse) && sa.isSpell() && host.isEnchantment()
                            && CardFactoryUtil.isCounterable(host)) {
                        return true;
                    } else if ("ChaliceOfTheVoid".equals(curse) && sa.isSpell() && CardFactoryUtil.isCounterable(host)
                            && host.getCMC() == c.getCounters(CounterEnumType.CHARGE)) {
                        return true;
                    }  else if ("BazaarOfWonders".equals(curse) && sa.isSpell() && CardFactoryUtil.isCounterable(host)) {
                        String hostName = host.getName();
                        for (Card card : ccvGameBattlefield) {
                            if (!card.isToken() && card.getName().equals(hostName)) {
                                return true;
                            }
                        }
                        for (Card card : game.getCardsIn(ZoneType.Graveyard)) {
                            if (card.getName().equals(hostName)) {
                                return true;
                            }
                        }
                    }
                } 
            }
        }
        return false;
    }

    public boolean checkETBEffects(final Card card, final SpellAbility sa, final ApiType api) {
        if (card.isCreature()
                && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noCreatureETBTriggers)) {
            return api == null;
        }
        boolean rightapi = false;
        Player activatingPlayer = sa.getActivatingPlayer();

        // for xPaid stuff
        card.setCastSA(sa);

        // Trigger play improvements
        for (final Trigger tr : card.getTriggers()) {
            // These triggers all care for ETB effects

            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!ZoneType.Battlefield.toString().equals(tr.getParam("Destination"))) {
                continue;
            }

            if (tr.hasParam("ValidCard")) {
                String validCard = tr.getParam("ValidCard");
                if (!validCard.contains("Self")) {
                    continue;
                }
                if (validCard.contains("notkicked")) {
                    if (sa.isKicked()) {
                        continue;
                    }
                } else if (validCard.contains("kicked")) {
                    if (validCard.contains("kicked ")) { // want a specific kicker
                        String s = validCard.split("kicked ")[1];
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

            // if trigger is not mandatory - no problem
            if (tr.hasParam("OptionalDecider") && api == null) {
                continue;
            }

            SpellAbility exSA = tr.ensureAbility().copy(activatingPlayer);

            if (api != null) {
                if (exSA.getApi() != api) {
                    continue;
                }
                rightapi = true;
                if (!(exSA instanceof AbilitySub)) {
                    if (!ComputerUtilCost.canPayCost(exSA, player)) {
                        return false;
                    }
                }
            }

            exSA.setTrigger(tr);
            // need to set TriggeredObject
            exSA.setTriggeringObject(AbilityKey.Card, card);

            // for trigger test, need to ignore the conditions
            SpellAbilityCondition cons = exSA.getConditions();
            if (cons != null) {
                String pres = cons.getIsPresent();
                if (pres != null && pres.matches("Card\\.(Strictly)?Self")) {
                    cons.setIsPresent(null);
                }
            }

            // Run non-mandatory trigger.
            // These checks only work if the Executing SpellAbility is an Ability_Sub.
            if (exSA instanceof AbilitySub && !doTrigger(exSA, false)) {
                // AI would not run this trigger if given the chance
                if (api == null && card.isCreature() && exSA.usesTargeting() && !exSA.getTargetRestrictions().hasCandidates(exSA) && ComputerUtil.aiLifeInDanger(activatingPlayer, true, 0)) {
                    // trigger will not run due to lack of targets and we desperately need a creature
                    continue;
                }
                return false;
            }
        }
        if (api != null && !rightapi) {
            return false;
        }

        // Replacement effects
        for (final ReplacementEffect re : card.getReplacementEffects()) {
            // These Replacements all care for ETB effects
            if (!(re instanceof ReplaceMoved)) {
                continue;
            }

            if (!ZoneType.Battlefield.toString().equals(re.getParam("Destination"))) {
                continue;
            }

            if (re.hasParam("ValidCard")) {
                String validCard = re.getParam("ValidCard");
                if (!validCard.contains("Self")) {
                    continue;
                }
                if (validCard.contains("notkicked")) {
                    if (sa.isKicked()) {
                        continue;
                    }
                } else if (validCard.contains("kicked")) {
                    if (validCard.contains("kicked ")) { // want a specific kicker
                        String s = validCard.split("kicked ")[1];
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
            SpellAbility exSA = re.getOverridingAbility();

            if (exSA != null) {
                exSA = exSA.copy(activatingPlayer);

                // ETBReplacement uses overriding abilities.
                // These checks only work if the Executing SpellAbility is an Ability_Sub.
                if ((exSA instanceof AbilitySub) && !doTrigger(exSA, false)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<SpellAbility> getPlayableCounters(final CardCollection l) {
        final List<SpellAbility> spellAbility = Lists.newArrayList();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getNonManaAbilities()) {
                // Check if this AF is a Counterspell
                if (sa.getApi() == ApiType.Counter) {
                    spellAbility.add(sa);
                }
            }
        }
        return spellAbility;
    }

    private CardCollection filterLandsToPlay(CardCollection landList) {
        final CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        CardCollection nonLandList = CardLists.filter(hand, Predicates.not(CardPredicates.Presets.LANDS));
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
                        Cost payCosts = sa.getPayCosts();
                        if (payCosts != null) {
                            for (CostPart part : payCosts.getCostParts()) {
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
                CardCollectionView battlefield = player.getCardsIn(ZoneType.Battlefield);
                if (canPlaySpellBasic(c, null) != AiPlayDecision.WillPlay) {
                    return false;
                }
                String name = c.getName();
                if (c.getType().isLegendary() && !name.equals("Flagstones of Trokair")) {
                    if (Iterables.any(battlefield, CardPredicates.nameEquals(name))) {
                        return false;
                    }
                }

                final CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
                CardCollection lands = new CardCollection(battlefield);
                lands.addAll(hand);
                lands = CardLists.filter(lands, CardPredicates.Presets.LANDS);
                int maxCmcInHand = Aggregates.max(hand, CardPredicates.Accessors.fnGetCmc);

                if (lands.size() >= Math.max(maxCmcInHand, 6)) {
                    // don't play MDFC land if other side is spell and enough lands are available
                    if (!c.isLand() || (c.isModal() && !c.getState(CardStateName.Modal).getType().isLand())) {
                        return false;
                    }

                    // don't play the land if it has cycling and enough lands are available
                    final FCollectionView<SpellAbility> spellAbilities = c.getSpellAbilities();
                    for (final SpellAbility sa : spellAbilities) {
                        if (sa.isCycling()) {
                            return false;
                        }
                    }
                }

                return player.canPlayLand(c);
            }
        });
        return landList;
    }

    private Card chooseBestLandToPlay(CardCollection landList) {
        if (landList.isEmpty()) {
            return null;
        }

        CardCollection nonLandsInHand = CardLists.filter(player.getCardsIn(ZoneType.Hand), Predicates.not(CardPredicates.Presets.LANDS));

        // Some considerations for Momir/MoJhoSto
        boolean hasMomir = !CardLists.filter(player.getCardsIn(ZoneType.Command),
                CardPredicates.nameEquals("Momir Vig, Simic Visionary Avatar")).isEmpty();
        if (hasMomir && nonLandsInHand.isEmpty()) {
            // Only do this if we have an all-basic land hand, which covers both stock Momir and MoJhoSto modes
            // and also a custom Vanguard setup with a customized basic land deck and Momir as the avatar.
            String landStrategy = getProperty(AiProps.MOMIR_BASIC_LAND_STRATEGY);
            if (landStrategy.equalsIgnoreCase("random")) {
                // Pick a completely random basic land
                return Aggregates.random(landList);
            } else if (landStrategy.toLowerCase().startsWith("preforder:")) {
                // Pick a basic land in order of preference, or play a random one if nothing is preferred
                String order = landStrategy.substring(10);
                for (char c : order.toCharArray()) {
                    byte color = MagicColor.fromName(c);
                    for (Card land : landList) {
                        for (final SpellAbility m : ComputerUtilMana.getAIPlayableMana(land)) {
                            if (m.canProduce(MagicColor.toShortString(color))) {
                                return land;
                            }
                        }
                    }
                }
                return Aggregates.random(landList);
            }
            // If nothing is done here, proceeds to the default land picking strategy
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

        //try to skip lands that enter the battlefield tapped
        if (!nonLandsInHand.isEmpty()) {
            CardCollection nonTappedLands = new CardCollection();
            for (Card land : landList) {
                // check replacement effects if land would enter tapped or not
                final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(land);
                repParams.put(AbilityKey.Origin, land.getZone().getZoneType());
                repParams.put(AbilityKey.Destination, ZoneType.Battlefield);
                repParams.put(AbilityKey.Source, land);

                boolean foundTapped = false;
                for (ReplacementEffect re : player.getGame().getReplacementHandler().getReplacementList(ReplacementType.Moved, repParams, ReplacementLayer.Other)) {
                    SpellAbility reSA = re.ensureAbility();
                    if (reSA == null || !ApiType.Tap.equals(reSA.getApi())) {
                        continue;
                    }
                    reSA.setActivatingPlayer(reSA.getHostCard().getController());
                    if (reSA.metConditions()) {
                        foundTapped = true;
                        break;
                    }
                }

                // TODO if this is the only source for a color we need badly prioritize it instead
                if (foundTapped) {
                    continue;
                }

                nonTappedLands.add(land);
            }
            if (!nonTappedLands.isEmpty()) {
                landList = nonTappedLands;
            }
        }

        // Choose first land to be able to play a one drop
        if (player.getLandsInPlay().isEmpty()) {
            CardCollection oneDrops = CardLists.filter(nonLandsInHand, CardPredicates.hasCMC(1));
            for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                byte color = MagicColor.WUBRG[i];
                if (Iterables.any(oneDrops, CardPredicates.isColor(color))) {
                    for (Card land : landList) {
                        if (land.getType().hasSubtype(MagicColor.Constant.BASIC_LANDS.get(i))) {
                            return land;
                        }
                        for (final SpellAbility m : ComputerUtilMana.getAIPlayableMana(land)) {
                            if (m.canProduce(MagicColor.toShortString(color))) {
                                return land;
                            }
                        }
                    }
                }
            }
        }

        //play lands with a basic type that is needed the most
        final CardCollectionView landsInBattlefield = player.getCardsIn(ZoneType.Battlefield);
        final List<String> basics = Lists.newArrayList();

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
    private SpellAbility predictSpellToCastInMain2(ApiType exceptSA, boolean handOnly) {
        if (!getBooleanProperty(AiProps.PREDICT_SPELLS_FOR_MAIN2)) {
            return null;
        }

        final CardCollectionView cards = handOnly ? player.getCardsIn(ZoneType.Hand) :
            ComputerUtilAbility.getAvailableCards(game, player);

        List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(cards, player);

        try {
            Collections.sort(all, saComparator); // put best spells first
        }
        catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            String assertex = ComparatorUtil.verifyTransitivity(saComparator, all);
            Sentry.capture(ex.getMessage() + "\nAssertionError [verifyTransitivity]: " + assertex);
        }

        for (final SpellAbility sa : ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player)) {
            ApiType saApi = sa.getApi();
            
            if (saApi == ApiType.Counter || saApi == exceptSA) {
                continue;
            }
            sa.setActivatingPlayer(player);
            // TODO: this currently only works as a limited prediction of permanent spells.
            // Ideally this should cast canPlaySa to determine that the AI is truly able/willing to cast a spell,
            // but that is currently difficult to implement due to various side effects leading to stack overflow.
            Card host = sa.getHostCard();
            if (!ComputerUtil.castPermanentInMain1(player, sa) && host != null && !host.isLand() && ComputerUtilCost.canPayCost(sa, player)) {
                if (sa instanceof SpellPermanent) {
                    return sa;
                }
            }
        }
        return null;
    }

    public boolean reserveManaSourcesForNextSpell(SpellAbility sa, SpellAbility exceptForSa) {
        return reserveManaSources(sa, null, false, true, exceptForSa);
    }

    public boolean reserveManaSources(SpellAbility sa) {
        return reserveManaSources(sa, PhaseType.MAIN2, false, false, null);
    }
    public boolean reserveManaSources(SpellAbility sa, PhaseType phaseType, boolean enemy) {
        return reserveManaSources(sa, phaseType, enemy, true, null);
    }
    public boolean reserveManaSources(SpellAbility sa, PhaseType phaseType, boolean enemy, boolean forNextSpell, SpellAbility exceptForThisSa) {
        ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(sa, true, 0);
        CardCollection manaSources = ComputerUtilMana.getManaSourcesToPayCost(cost, sa, player);

        // used for chained spells where two spells need to be cast in succession
        if (exceptForThisSa != null) {
            manaSources.removeAll(ComputerUtilMana.getManaSourcesToPayCost(ComputerUtilMana.calculateManaCost(exceptForThisSa, true, 0), exceptForThisSa, player));
        }

        if (manaSources.isEmpty()) {
            return false;
        }

        AiCardMemory.MemorySet memSet = null;
        if (phaseType == null && forNextSpell) {
            memSet = AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL;
        } else if (phaseType != null) {
            switch (phaseType) {
                case MAIN2:
                    memSet = AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2;
                    break;
                case COMBAT_DECLARE_BLOCKERS:
                    memSet = enemy ? AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK
                            : AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_DECLBLK;
                    break;
                default:
                    System.out.println("Warning: unsupported mana reservation phase specified for reserveManaSources: "
                            + phaseType.name() + ", reserving until Main 2 instead. Consider adding support for the phase if needed.");
                    memSet = AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2;
                    break;
            }
        }

        // This is a simplification, since one mana source can produce more than one mana,
        // but should work in most circumstances to ensure safety in whatever the AI is using this for.
        if (manaSources.size() >= cost.getConvertedManaCost()) {
            for (Card c : manaSources) {
                AiCardMemory.rememberCard(player, c, memSet);
            }
            return true;
        }

        return false;
    }

    // This is for playing spells regularly (no Cascade/Ripple etc.)
    private AiPlayDecision canPlayAndPayFor(final SpellAbility sa) {
        if (!sa.canPlay()) {
            return AiPlayDecision.CantPlaySa;
        }

        // Check a predefined condition
        if (sa.hasParam("AICheckSVar")) {
            final Card host = sa.getHostCard();
            final String svarToCheck = sa.getParam("AICheckSVar");
            String comparator = "GE";
            int compareTo = 1;

            if (sa.hasParam("AISVarCompare")) {
                final String fullCmp = sa.getParam("AISVarCompare");
                comparator = fullCmp.substring(0, 2);
                final String strCmpTo = fullCmp.substring(2);
                try {
                    compareTo = Integer.parseInt(strCmpTo);
                } catch (final Exception ignored) {
                    compareTo = AbilityUtils.calculateAmount(host, host.getSVar(strCmpTo), sa);
                }
            }

            int left = AbilityUtils.calculateAmount(host, svarToCheck, sa);
            if (!Expressions.compare(left, comparator, compareTo)) {
                return AiPlayDecision.AnotherTime;
            }
        }

        int oldCMC = -1;
        boolean xCost = sa.getPayCosts().hasXInAnyCostPart() || sa.getHostCard().hasStartOfKeyword("Strive");
        if (!xCost) {
            if (!ComputerUtilCost.canPayCost(sa, player)) {
                // for most costs, it's OK to check if they can be paid early in order to avoid running a heavy API check
                // when the AI won't even be able to play the spell in the first place (even if it could afford it)
                return AiPlayDecision.CantAfford;
            }
            // TODO check for Reduce too, e.g. Battlefield Thaumaturge could make it castable
            if (sa.usesTargeting()) {
                oldCMC = CostAdjustment.adjust(sa.getPayCosts(), sa).getTotalMana().getCMC();
            }
        }

        // state needs to be switched here so API checks evaluate the right face
        if (sa.getCardState() != null && !sa.getHostCard().isInPlay() && sa.getCardState().getStateName() == CardStateName.Modal) {
            sa.getHostCard().setState(CardStateName.Modal, false);
        }

        AiPlayDecision canPlay = canPlaySa(sa); // this is the "heaviest" check, which also sets up targets, defines X, etc.

        if (sa.getCardState() != null && !sa.getHostCard().isInPlay() && sa.getCardState().getStateName() == CardStateName.Modal) {
            sa.getHostCard().setState(CardStateName.Original, false);
        }

        if (canPlay != AiPlayDecision.WillPlay) {
            return canPlay;
        }

        // Account for possible Ward after the spell is fully targeted
        // TODO: ideally, this should be done while targeting, so that a different target can be preferred if the best
        // one is warded and can't be paid for.
        if (sa.usesTargeting()) {
            for (Card tgt : sa.getTargets().getTargetCards()) {
                // TODO some older cards don't use the keyword, so check for trigger instead
                if (tgt.hasKeyword(Keyword.WARD) && tgt.isInPlay() && tgt.getController().isOpponentOf(sa.getHostCard().getController())) {
                    int amount = 0;
                    Cost wardCost = ComputerUtilCard.getTotalWardCost(tgt);
                    if (wardCost.hasManaCost()) {
                        amount = wardCost.getTotalMana().getCMC();
                        if (amount > 0 && !ComputerUtilCost.canPayCost(sa, player)) {
                            return AiPlayDecision.CantAfford;
                        }
                    }
                    if (wardCost.hasSpecificCostType(CostPayLife.class)) {
                        int lifeToPay = wardCost.getCostPartByType(CostPayLife.class).convertAmount();
                        if (lifeToPay > player.getLife() || (lifeToPay == player.getLife() && !player.cantLoseForZeroOrLessLife())) {
                            return AiPlayDecision.CantAfford;
                        }
                    }
                    if (wardCost.hasSpecificCostType(CostDiscard.class)
                            && wardCost.getCostPartByType(CostDiscard.class).convertAmount() > player.getCardsIn(ZoneType.Hand).size()) {
                        return AiPlayDecision.CantAfford;
                    }
                }
            }
        }

        // check if some target raised cost
        if (oldCMC > -1) {
            int finalCMC = CostAdjustment.adjust(sa.getPayCosts(), sa).getTotalMana().getCMC();
            if (finalCMC > oldCMC) {
                xCost = true;
            }
        }

        if (xCost && !ComputerUtilCost.canPayCost(sa, player)) {
            // for dependent costs with X, e.g. Repeal, which require a valid target to be specified before a decision can be made
            // on whether the cost can be paid, this can only be checked late after canPlaySa has been run (or the AI will misplay)
            return AiPlayDecision.CantAfford;
        }

        // if we got here, looks like we can play the final cost and we could properly set up and target the API and
        // are willing to play the SA
        return AiPlayDecision.WillPlay;
    }

    public AiPlayDecision canPlaySa(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final boolean isRightTiming = sa.canCastTiming(player);

        if (!checkAiSpecificRestrictions(sa)) {
            return AiPlayDecision.CantPlayAi;
        }
        if (sa instanceof WrappedAbility) {
            return canPlaySa(((WrappedAbility) sa).getWrappedAbility());
        }

        // Trying to play a card that has Buyback without a Buyback cost, look for possible additional considerations
        if (getBooleanProperty(AiProps.TRY_TO_PRESERVE_BUYBACK_SPELLS)) {
            if (card.hasKeyword(Keyword.BUYBACK) && !sa.isBuyBackAbility() && !canPlaySpellWithoutBuyback(card, sa)) {
                return AiPlayDecision.NeedsToPlayCriteriaNotMet;
            }
        }

        // When processing a new SA, clear the previously remembered cards that have been marked to avoid re-entry
        // which might potentially cause a stack overflow.
        AiCardMemory.clearMemorySet(this, AiCardMemory.MemorySet.MARKED_TO_AVOID_REENTRY);

        // TODO before suspending some spells try to predict if relevant targets can be expected
        if (sa.getApi() != null) {

            String msg = "AiController:canPlaySa: AI checks for if can PlaySa";
            Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Api", sa.getApi().toString())
                    .withData("Card", card.getName()).withData("SA", sa.toString()).build()
            );

            // add Extra for debugging
            Sentry.getContext().addExtra("Card", card);
            Sentry.getContext().addExtra("SA", sa.toString());

            boolean canPlay = SpellApiToAi.Converter.get(sa.getApi()).canPlayAIWithSubs(player, sa);

            // remove added extra
            Sentry.getContext().removeExtra("Card");
            Sentry.getContext().removeExtra("SA");

            if (!canPlay) {
                return AiPlayDecision.CantPlayAi;
            }
        } else {
            Cost payCosts = sa.getPayCosts();
            if (payCosts != null) {
                ManaCost mana = payCosts.getTotalMana();
                if (mana != null) {
                    if (mana.countX() > 0) {
                        // Set PayX here to maximum value.
                        final int xPay = ComputerUtilCost.getMaxXValue(sa, player);
                        if (xPay <= 0) {
                            return AiPlayDecision.CantAffordX;
                        }
                        sa.setXManaCostPaid(xPay);
                    } else if (mana.isZero()) {
                        // if mana is zero, but card mana cost does have X, then something is wrong
                        ManaCost cardCost = card.getManaCost();
                        if (cardCost != null && cardCost.countX() > 0) {
                            return AiPlayDecision.CantPlayAi;
                        }
                    }
                }
            }
        }
        if (checkCurseEffects(sa)) {
            return AiPlayDecision.CurseEffects;
        }
        Card spellHost = card;
        if (sa.isSpell()) {
            spellHost = CardUtil.getLKICopy(spellHost);
            spellHost.setLKICMC(-1); // to reset the cmc
            spellHost.setLastKnownZone(game.getStackZone()); // need to add to stack to make check Restrictions respect stack cmc
            spellHost.setCastFrom(card.getZone().getZoneType());
        }
        if (!sa.checkRestrictions(spellHost, player)) {
            return AiPlayDecision.AnotherTime;
        }
        if (sa instanceof SpellPermanent) {
            if (!isRightTiming) {
                return AiPlayDecision.AnotherTime;
            }
            return canPlayFromEffectAI((SpellPermanent)sa, false, true);
        }
        if (sa.usesTargeting()) {
            if (!sa.isTargetNumberValid() && !sa.getTargetRestrictions().hasCandidates(sa)) {
                return AiPlayDecision.TargetingFailed;
            }
            if (!StaticAbilityMustTarget.meetsMustTargetRestriction(sa)) {
                return AiPlayDecision.TargetingFailed;
            }
        }
        if (sa instanceof Spell) {
            if (ComputerUtil.getDamageForPlaying(player, sa) >= player.getLife() 
                    && !player.cantLoseForZeroOrLessLife() && player.canLoseLife()) {
                return AiPlayDecision.CurseEffects;
            }
            if (!isRightTiming) {
                return AiPlayDecision.AnotherTime;
            }
            return canPlaySpellBasic(card, sa);
        }
        if (!isRightTiming) {
            return AiPlayDecision.AnotherTime;
        }
        return AiPlayDecision.WillPlay;
    }

    public boolean isNonDisabledCardInPlay(final String cardName) {
        for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
            if (card.getName().equals(cardName)) {
                // TODO - Better logic to determine if a permanent is disabled by local effects
                // currently assuming any permanent enchanted by another player
                // is disabled and a second copy is necessary
                // will need actual logic that determines if the enchantment is able
                // to disable the permanent or it's still functional and a duplicate is unneeded.
                boolean disabledByEnemy = false;
                for (Card card2 : card.getEnchantedBy()) {
                    if (card2.getOwner() != player) {
                        disabledByEnemy = true;
                    }
                }
                if (!disabledByEnemy) {
                    return true;
                }
            }
        }
        return false;
    }

    private AiPlayDecision canPlaySpellBasic(final Card card, final SpellAbility sa) {
        if ("True".equals(card.getSVar("NonStackingEffect")) && isNonDisabledCardInPlay(card.getName())) {
            return AiPlayDecision.NeedsToPlayCriteriaNotMet;
        }

        // add any other necessary logic to play a basic spell here
        return ComputerUtilCard.checkNeedsToPlayReqs(card, sa);
    }

    private boolean canPlaySpellWithoutBuyback(Card card, SpellAbility sa) {
        boolean wasteBuybackAllowed = false;

        // About to lose game : allow
        if (ComputerUtil.aiLifeInDanger(player, true, 0)) {
            wasteBuybackAllowed = true;
        }

        int copies = CardLists.filter(player.getCardsIn(ZoneType.Hand), CardPredicates.nameEquals(card.getName())).size();
        // Have two copies : allow
        if (copies >= 2) {
            wasteBuybackAllowed = true;
        }

        int neededMana = 0;
        boolean dangerousRecurringCost = false;

        Cost costWithBuyback = sa.getPayCosts().copy();
        for (OptionalCostValue opt : GameActionUtil.getOptionalCostValues(sa)) {
            if (opt.getType() == OptionalCost.Buyback) {
                costWithBuyback.add(opt.getCost());
            }
        }
        CostAdjustment.adjust(costWithBuyback, sa);
        if (costWithBuyback.getCostMana() != null) {
            neededMana = costWithBuyback.getCostMana().getMana().getCMC();
        }
        if (costWithBuyback.hasSpecificCostType(CostPayLife.class)
                || costWithBuyback.hasSpecificCostType(CostDiscard.class)
                || costWithBuyback.hasSpecificCostType(CostSacrifice.class)) {
            dangerousRecurringCost = true;
        }

        // won't be able to afford buyback any time soon
        // if Buyback cost includes sacrifice, life, discard
        if (dangerousRecurringCost) {
            wasteBuybackAllowed = true;
        }

        // Memory Crystal-like effects need special handling
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            for (StaticAbility s : c.getStaticAbilities()) {
                if ("ReduceCost".equals(s.getParam("Mode"))
                        && "Spell.Buyback".equals(s.getParam("ValidSpell"))) {
                    neededMana -= AbilityUtils.calculateAmount(c, s.getParam("Amount"), s);
                }
            }
        }
        if (neededMana < 0) {
            neededMana = 0;
        }

        int hasMana = ComputerUtilMana.getAvailableManaEstimate(player, false);
        if (hasMana < neededMana - 1) {
            wasteBuybackAllowed = true;
        }

        return wasteBuybackAllowed;
    }

    // not sure "playing biggest spell" matters?
    private final static Comparator<SpellAbility> saComparator = new Comparator<SpellAbility>() {
        @Override
        public int compare(final SpellAbility a, final SpellAbility b) {
            // sort from highest cost to lowest
            // we want the highest costs first
            int a1 = a.getPayCosts().getTotalMana().getCMC();
            int b1 = b.getPayCosts().getTotalMana().getCMC();

            // deprioritize SAs explicitly marked as preferred to be activated last compared to all other SAs
            if (a.hasParam("AIActivateLast") && !b.hasParam("AIActivateLast")) {
                return 1;
            } else if (b.hasParam("AIActivateLast") && !a.hasParam("AIActivateLast")) {
                return -1;
            }

            // deprioritize planar die roll marked with AIRollPlanarDieParams:LowPriority$ True
            if (ApiType.RollPlanarDice == a.getApi() && a.getHostCard() != null && a.getHostCard().hasSVar("AIRollPlanarDieParams") && a.getHostCard().getSVar("AIRollPlanarDieParams").toLowerCase().matches(".*lowpriority\\$\\s*true.*")) {
                return 1;
            } else if (ApiType.RollPlanarDice == b.getApi() && b.getHostCard() != null && b.getHostCard().hasSVar("AIRollPlanarDieParams") && b.getHostCard().getSVar("AIRollPlanarDieParams").toLowerCase().matches(".*lowpriority\\$\\s*true.*")) {
                return -1;
            }

            // deprioritize pump spells with pure energy cost (can be activated last,
            // since energy is generally scarce, plus can benefit e.g. Electrostatic Pummeler)
            int a2 = 0, b2 = 0;
            if (a.getApi() == ApiType.Pump && a.getPayCosts().getCostEnergy() != null) {
                if (a.getPayCosts().hasOnlySpecificCostType(CostPayEnergy.class)) {
                    a2 = a.getPayCosts().getCostEnergy().convertAmount();
                }
            }
            if (b.getApi() == ApiType.Pump && b.getPayCosts().getCostEnergy() != null) {
                if (b.getPayCosts().hasOnlySpecificCostType(CostPayEnergy.class)) {
                    b2 = b.getPayCosts().getCostEnergy().convertAmount();
                }
            }
            if (a2 == 0 && b2 > 0) {
                return -1;
            } else if (b2 == 0 && a2 > 0) {
                return 1;
            }

            // cast 0 mana cost spells first (might be a Mox)
            if (a1 == 0 && b1 > 0 && ApiType.Mana != a.getApi()) {
                return -1;
            } else if (a1 > 0 && b1 == 0 && ApiType.Mana != b.getApi()) {
                return 1;
            }
            
            if (a.getHostCard() != null && a.getHostCard().hasSVar("FreeSpellAI")) {
                return -1;
            } else if (b.getHostCard() != null && b.getHostCard().hasSVar("FreeSpellAI")) {
                return 1;
            }

            if (a.getHostCard().equals(b.getHostCard()) && a.getApi() == b.getApi()) {
                // Cheaper Spectacle costs should be preferred
                // FIXME: Any better way to identify that these are the same ability, one with Spectacle and one not?
                // (looks like it's not a full-fledged alternative cost as such, and is not processed with other alt costs)
                if (a.isSpectacle() && !b.isSpectacle()
                        && a.getPayCosts().getTotalMana().getCMC() < b.getPayCosts().getTotalMana().getCMC()) {
                    return 1;
                } else if (b.isSpectacle() && !a.isSpectacle()
                        && b.getPayCosts().getTotalMana().getCMC() < a.getPayCosts().getTotalMana().getCMC()) {
                    return 1;
                }
            }

            a1 += getSpellAbilityPriority(a);
            b1 += getSpellAbilityPriority(b);

            return b1 - a1;
        }
        
        private int getSpellAbilityPriority(SpellAbility sa) {
            int p = 0;
            Card source = sa.getHostCard();
            final Player ai = source == null ? sa.getActivatingPlayer() : source.getController();
            if (ai == null) {
                System.err.println("Error: couldn't figure out the activating player and host card for SA: " + sa);
                return 0;
            }
            final boolean noCreatures = ai.getCreaturesInPlay().isEmpty();

            if (source != null) {
                // puts creatures in front of spells
                if (source.isCreature()) {
                    p += 1;
                }
                if (source.hasSVar("AIPriorityModifier")) {
                    p += Integer.parseInt(source.getSVar("AIPriorityModifier"));
                }
                // don't play equipments before having any creatures
                if (source.isEquipment() && noCreatures) {
                    p -= 9;
                }
                // don't equip stuff in main 2 if there's more stuff to cast at the moment
                if (sa.getApi() == ApiType.Attach && !sa.isCurse() && source.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    p -= 1;
                }
                // 1. increase chance of using Surge effects
                // 2. non-surged versions are usually inefficient
                if (source.getOracleText().contains("surge cost") && !sa.isSurged()) {
                    p -= 9;
                }
                // move snap-casted spells to front
                if (source.isInZone(ZoneType.Graveyard)) {
                    if (sa.getMayPlay() != null && source.mayPlay(sa.getMayPlay()) != null) {
                        p += 50;
                    }
                }
                // if the profile specifies it, deprioritize Storm spells in an attempt to build up storm count
                if (source.hasKeyword(Keyword.STORM) && ai.getController() instanceof PlayerControllerAi) {
                    p -= (((PlayerControllerAi) ai.getController()).getAi().getIntProperty(AiProps.PRIORITY_REDUCTION_FOR_STORM_SPELLS));
                }
            }

            // use Surge and Prowl costs when able to
            if (sa.isSurged() || sa.isProwl()) {
                p += 9;
            }
            // sort planeswalker abilities with most costly first
            if (sa.isPwAbility()) {
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
            } else if (ApiType.Mana == sa.getApi()) {
                p -= 9;
            }

            // try to cast mana ritual spells before casting spells to maximize potential mana
            if ("ManaRitual".equals(sa.getParam("AILogic"))) {
                p += 9;
            }

            return p;
        }
    };

    public CardCollection getCardsToDiscard(final int numDiscard, final String[] uTypes, final SpellAbility sa) {
        return getCardsToDiscard(numDiscard, uTypes, sa, CardCollection.EMPTY);
    }

    public CardCollection getCardsToDiscard(final int numDiscard, final String[] uTypes, final SpellAbility sa, final CardCollectionView exclude) {
        boolean noFiltering = (sa != null) && "DiscardCMCX".equals(sa.getParam("AILogic")); // list AI logic for which filtering is taken care of elsewhere
        CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        hand.removeAll(exclude);
        if ((uTypes != null) && (sa != null) && !noFiltering) {
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
            String logic = sa.getParamOrDefault("AILogic", "");
            sourceCard = sa.getHostCard();
            if ("Always".equals(logic) && !validCards.isEmpty()) {
                min = 1;
            } else if (logic.startsWith("UnlessAtLife.")) {
                int threshold = AbilityUtils.calculateAmount(sourceCard, logic.substring(logic.indexOf(".") + 1), sa);
                if (player.getLife() <= threshold) {
                    min = 1;
                }
            } else if ("VolrathsShapeshifter".equals(logic)) {
                return SpecialCardAi.VolrathsShapeshifter.targetBestCreature(player, sa);
            } else if ("DiscardCMCX".equals(logic)) {
                final int cmc = sa.getXManaCostPaid();
                CardCollection discards = CardLists.filter(player.getCardsIn(ZoneType.Hand), CardPredicates.hasCMC(cmc));
                if (discards.isEmpty()) {
                    return null;
                }
                return new CardCollection(ComputerUtilCard.getWorstAI(discards));
            }

            if (sa.hasParam("AnyNumber")) {
                if ("DiscardUncastableAndExcess".equals(sa.getParam("AILogic"))) {
                    CardCollection discards = new CardCollection();
                    final CardCollectionView inHand = player.getCardsIn(ZoneType.Hand);
                    final int numLandsOTB = CardLists.filter(player.getCardsIn(ZoneType.Hand), CardPredicates.Presets.LANDS).size();
                    int numOppInHand = 0;
                    for (Player p : player.getGame().getPlayers()) {
                        if (p.getCardsIn(ZoneType.Hand).size() > numOppInHand) {
                            numOppInHand = p.getCardsIn(ZoneType.Hand).size();
                        }
                    }
                    for (Card c : inHand) {
                        if (c.hasSVar("DoNotDiscardIfAble") || c.hasSVar("IsReanimatorCard")) { continue; }
                        if (c.isCreature() && !ComputerUtilMana.hasEnoughManaSourcesToCast(c.getSpellPermanent(), player)) {
                            discards.add(c);
                        }
                        if ((c.isLand() && numLandsOTB >= 5) || (c.getFirstSpellAbility() != null && !ComputerUtilMana.hasEnoughManaSourcesToCast(c.getFirstSpellAbility(), player))) {
                            if (discards.size() + 1 <= numOppInHand) {
                                discards.add(c);
                            }
                        }
                    }
                    return discards;
                }
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
                if (prefCard != null && prefCard.hasSVar("DoNotDiscardIfAble")) {
                    prefCard = null;
                }
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
            final int numLandsInPlay = CardLists.count(player.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.LANDS);
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

                // Discard unplayable card (checks by CMC)
                // But check if there is a card in play that allows casting spells for free!
                // if yes, nothing is unplayable based on CMC alone
                boolean discardedUnplayable = false;
                boolean freeCastAllowed = ComputerUtilCost.isFreeCastAllowedByPermanent(player, null);

                for (int j = 0; j < validCards.size(); j++) {
                    if ((validCards.get(j).getCMC() > numLandsAvailable || freeCastAllowed) && !validCards.get(j).hasSVar("DoNotDiscardIfAble")) {
                        discardList.add(validCards.get(j));
                        validCards.remove(validCards.get(j));
                        discardedUnplayable = true;
                        break;
                    } else if (validCards.get(j).getCMC() <= numLandsAvailable) {
                        // cut short to avoid looping over cards which are guaranteed not to fit the criteria
                        break;
                    }
                }

                if (!discardedUnplayable) {
                    // discard worst card
                    Card worst = ComputerUtilCard.getWorstAI(validCards);
                    if (worst == null) {
                        // there were only instants and sorceries, and maybe cards that are not good to discard, so look
                        // for more discard options
                        worst = ComputerUtilCard.getCheapestSpellAI(validCards);
                    }
                    if (worst == null && !validCards.isEmpty()) {
                        // still nothing chosen, so choose the first thing that works, trying not to make DoNotDiscardIfAble
                        // discards
                        for (Card c : validCards) {
                            if (!c.hasSVar("DoNotDiscardIfAble")) {
                                worst = c;
                                break;
                            }
                        }
                        // Only DoNotDiscardIfAble cards? If we have a duplicate for something, discard it
                        if (worst == null) {
                            for (Card c : validCards) {
                                if (CardLists.filter(player.getCardsIn(ZoneType.Hand), CardPredicates.nameEquals(c.getName())).size() > 1) {
                                    worst = c;
                                    break;
                                }
                            }
                            if (worst == null) {
                                // Otherwise just grab a random card and discard it
                                worst = Aggregates.random(validCards);
                            }
                        }
                    }
                    discardList.add(worst);
                    validCards.remove(worst);
                }
            }
        }
        return discardList;
    }

    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        ApiType api = sa.getApi();

        // Abilities without api may also use this routine, However they should provide a unique mode value ?? How could this work?
        if (api == null) {
            String exMsg = String.format("AI confirmAction does not know what to decide about %s mode (api is null).",
                    mode);
            throw new IllegalArgumentException(exMsg);
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
        return true;
    }

    public String getProperty(AiProps propName) {
        return AiProfileUtil.getAIProp(getPlayer().getLobbyPlayer(), propName);
    }

    public int getIntProperty(AiProps propName) {
        String prop = AiProfileUtil.getAIProp(getPlayer().getLobbyPlayer(), propName);

        if (prop == null || prop.isEmpty()) {
            return Integer.parseInt(propName.getDefault());
        }

        return Integer.parseInt(prop);
    }

    public boolean getBooleanProperty(AiProps propName) {
        String prop = AiProfileUtil.getAIProp(getPlayer().getLobbyPlayer(), propName);

        if (prop == null || prop.isEmpty()) {
            return Boolean.parseBoolean(propName.getDefault());
        }

        return Boolean.parseBoolean(prop);
    }

    public AiPlayDecision canPlayFromEffectAI(Spell spell, boolean mandatory, boolean withoutPayingManaCost) {
        int damage = ComputerUtil.getDamageForPlaying(player, spell);
        if (!mandatory && damage >= player.getLife() && !player.cantLoseForZeroOrLessLife() && player.canLoseLife()) {
            return AiPlayDecision.CurseEffects;
        }

        final Card card = spell.getHostCard();
        if (spell instanceof SpellApiBased) {
            boolean chance = false;
            if (withoutPayingManaCost) {
                chance = SpellApiToAi.Converter.get(spell.getApi()).doTriggerNoCostWithSubs(player, spell, mandatory);
            } else {
                chance = SpellApiToAi.Converter.get(spell.getApi()).doTriggerAI(player, spell, mandatory);
            }
            if (!chance)
                return AiPlayDecision.TargetingFailed;

            if (spell instanceof SpellPermanent) {
                if (mandatory) {
                    return AiPlayDecision.WillPlay;
                }

                if (!checkETBEffects(card, spell, null)) {
                    return AiPlayDecision.BadEtbEffects;
                }
                if (damage + ComputerUtil.getDamageFromETB(player, card) >= player.getLife()
                        && !player.cantLoseForZeroOrLessLife() && player.canLoseLife()) {
                    return AiPlayDecision.BadEtbEffects;
                }
            }
        }

        return canPlaySpellBasic(card, spell);
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

    private List<SpellAbility> singleSpellAbilityList(SpellAbility sa) {
        if (sa == null) { return null; }

        final List<SpellAbility> abilities = Lists.newArrayList();
        abilities.add(sa);
        return abilities;
    }

    public List<SpellAbility> chooseSpellAbilityToPlay() {
        // Reset cached predicted combat, as it may be stale. It will be
        // re-created if needed and used for any AI logic that needs it.
        predictedCombat = null;
        // Also reset predicted combat for next turn here
        predictedCombatNextTurn = null;

        // Reset priority mana reservation that's meant to work for one spell only
        AiCardMemory.clearMemorySet(player, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL);

        if (useSimulation) {
            return singleSpellAbilityList(simPicker.chooseSpellAbilityToPlay(null));
        }

        CardCollection landsWannaPlay = ComputerUtilAbility.getAvailableLandsToPlay(game, player);
        CardCollection playBeforeLand = CardLists.filter(
            player.getCardsIn(ZoneType.Hand), CardPredicates.hasSVar("PlayBeforeLandDrop")
        );

        if (!playBeforeLand.isEmpty()) {
            SpellAbility wantToPlayBeforeLand = chooseSpellAbilityToPlayFromList(
                ComputerUtilAbility.getSpellAbilities(playBeforeLand, player), false
            );
            if (wantToPlayBeforeLand != null) {
                return singleSpellAbilityList(wantToPlayBeforeLand);
            }
        }

        if (landsWannaPlay != null) {
            landsWannaPlay = filterLandsToPlay(landsWannaPlay);
            Log.debug("Computer " + game.getPhaseHandler().getPhase().nameForUi);
            if (landsWannaPlay != null && !landsWannaPlay.isEmpty()) {
                // TODO search for other land it might want to play?
                Card land = chooseBestLandToPlay(landsWannaPlay);
                if (ComputerUtil.getDamageFromETB(player, land) < player.getLife() || !player.canLoseLife() 
                        || player.cantLoseForZeroOrLessLife() ) {
                    if (!game.getPhaseHandler().is(PhaseType.MAIN1) || !isSafeToHoldLandDropForMain2(land)) {
                        final List<SpellAbility> abilities = Lists.newArrayList();

                        // TODO extend this logic to evaluate MDFC with both sides land
                        // this can only happen if its a MDFC land
                        if (!land.isLand()) {
                            land.setState(CardStateName.Modal, true);
                            land.setBackSide(true);
                        }

                        LandAbility la = new LandAbility(land, player, null);
                        la.setCardState(land.getCurrentState());
                        if (la.canPlay()) {
                            abilities.add(la);
                        }

                        // add mayPlay option
                        for (CardPlayOption o : land.mayPlay(player)) {
                            la = new LandAbility(land, player, o.getAbility());
                            la.setCardState(land.getCurrentState());
                            if (la.canPlay()) {
                                abilities.add(la);
                            }
                        }
                        if (!abilities.isEmpty()) {
                            return abilities;
                        }
                    }
                }
            }
        }

        return singleSpellAbilityList(getSpellAbilityToPlay());
    }

    private boolean isSafeToHoldLandDropForMain2(Card landToPlay) {
        boolean hasMomir = !CardLists.filter(player.getCardsIn(ZoneType.Command),
                CardPredicates.nameEquals("Momir Vig, Simic Visionary Avatar")).isEmpty();
        if (hasMomir) {
            // Don't do this in Momir variants since it messes with the AI decision making for the avatar.
            return false;
        }

        if (!MyRandom.percentTrue(getIntProperty(AiProps.HOLD_LAND_DROP_FOR_MAIN2_IF_UNUSED))) {
            // check against the chance specified in the profile
            return false;
        }
        if (game.getPhaseHandler().getTurn() <= 2) {
            // too obvious when doing it on the very first turn of the game
            return false;
        }

        CardCollection inHand = CardLists.filter(player.getCardsIn(ZoneType.Hand),
                Predicates.not(CardPredicates.Presets.LANDS));
        CardCollectionView otb = player.getCardsIn(ZoneType.Battlefield);

        if (getBooleanProperty(AiProps.HOLD_LAND_DROP_ONLY_IF_HAVE_OTHER_PERMS)) {
            if (!Iterables.any(otb, Predicates.not(CardPredicates.Presets.LANDS))) {
                return false;
            }
        }

        // TODO: improve the detection of taplands
        boolean isTapLand = false;
        for (ReplacementEffect repl : landToPlay.getReplacementEffects()) {
            if (repl.getParamOrDefault("Description", "").equals("CARDNAME enters the battlefield tapped.")) {
                isTapLand = true;
            }
        }

        int totalCMCInHand = Aggregates.sum(inHand, CardPredicates.Accessors.fnGetCmc);
        int minCMCInHand = Aggregates.min(inHand, CardPredicates.Accessors.fnGetCmc);
        if (minCMCInHand == Integer.MAX_VALUE)
            minCMCInHand = 0;
        int predictedMana = ComputerUtilMana.getAvailableManaEstimate(player, true);

        boolean canCastWithLandDrop = (predictedMana + 1 >= minCMCInHand) && minCMCInHand > 0 && !isTapLand;
        boolean cantCastAnythingNow = predictedMana < minCMCInHand;

        boolean hasRelevantAbsOTB = !CardLists.filter(otb, new Predicate<Card>() {
            @Override
            public boolean apply(Card card) {
                boolean isTapLand = false;
                for (ReplacementEffect repl : card.getReplacementEffects()) {
                    // TODO: improve the detection of taplands
                    if (repl.getParamOrDefault("Description", "").equals("CARDNAME enters the battlefield tapped.")) {
                        isTapLand = true;
                    }
                }

                for (SpellAbility sa : card.getSpellAbilities()) {
                    if (sa.isAbility()
                            && sa.getPayCosts().getCostMana() != null
                            && sa.getPayCosts().getCostMana().getMana().getCMC() > 0
                            && (!sa.getPayCosts().hasTapCost() || !isTapLand)
                            && (!sa.hasParam("ActivationZone") || sa.getParam("ActivationZone").contains("Battlefield"))) {
                        return true;
                    }
                }
                return false;
            }
        }).isEmpty();

        boolean hasLandBasedEffect = !CardLists.filter(otb, new Predicate<Card>() {
            @Override
            public boolean apply(Card card) {
                for (Trigger t : card.getTriggers()) {
                    Map<String, String> params = t.getMapParams();
                    if ("ChangesZone".equals(params.get("Mode"))
                            && params.containsKey("ValidCard")
                            && !params.get("ValidCard").contains("nonLand")
                            && ((params.get("ValidCard").contains("Land")) || (params.get("ValidCard").contains("Permanent")))
                            && "Battlefield".equals(params.get("Destination"))) {
                        // Landfall and other similar triggers
                        return true;
                    }
                }
                for (String sv : card.getSVars().keySet()) {
                    String varValue = card.getSVar(sv);
                    if (varValue.startsWith("Count$Valid") || sv.equals("BuffedBy")) {
                        if (varValue.contains("Land") || varValue.contains("Plains") || varValue.contains("Forest")
                                || varValue.contains("Mountain") || varValue.contains("Island") || varValue.contains("Swamp")
                                || varValue.contains("Wastes")) {
                            // In presence of various cards that get buffs like "equal to the number of lands you control",
                            // safer for our AI model to just play the land earlier rather than make a blunder
                            return true;
                        }
                    }
                }
                return false;
            }
        }).isEmpty();

        // TODO: add prediction for effects that will untap a tapland as it enters the battlefield
        if (!canCastWithLandDrop && cantCastAnythingNow && !hasLandBasedEffect && (!hasRelevantAbsOTB || isTapLand)) {
            // Hopefully there's not much to do with the extra mana immediately, can wait for Main 2
            return true;
        }
        if ((predictedMana <= totalCMCInHand && canCastWithLandDrop) || (hasRelevantAbsOTB && !isTapLand) || hasLandBasedEffect) {
            // Might need an extra land to cast something, or for some kind of an ETB ability with a cost or an
            // alternative cost (if we cast it in Main 1), or to use an activated ability on the battlefield
            return false;
        }

        return true;
    }

    private final SpellAbility getSpellAbilityToPlay() {
        final CardCollection cards = ComputerUtilAbility.getAvailableCards(game, player);
        List<SpellAbility> saList = Lists.newArrayList();

        SpellAbility top = null;
        if (!game.getStack().isEmpty()) {
            top = game.getStack().peekAbility();
        }
        final boolean topOwnedByAI = top != null && top.getActivatingPlayer().equals(player);
        final boolean mustRespond = top != null && top.hasParam("AIRespondsToOwnAbility");

        if (topOwnedByAI) {
            // AI's own spell: should probably let my stuff resolve first, but may want to copy the SA or respond to it
            // in a scripted timed fashion.

            if (!mustRespond) {
                saList = ComputerUtilAbility.getSpellAbilities(cards, player); // get the SA list early to check for copy SAs
                if (ComputerUtilAbility.getFirstCopySASpell(saList) == null) {
                    // Nothing to copy the spell with, so do nothing.
                    return null;
                }
            }
        }

        if (!game.getStack().isEmpty()) {
            SpellAbility counter = chooseCounterSpell(getPlayableCounters(cards));
            if (counter != null) return counter;
    
            SpellAbility counterETB = chooseSpellAbilityToPlayFromList(getPossibleETBCounters(), false);
            if (counterETB != null)
                return counterETB;
        }

        if (saList.isEmpty()) {
            saList = ComputerUtilAbility.getSpellAbilities(cards, player);
        }

        Iterables.removeIf(saList, new Predicate<SpellAbility>() {
            @Override
            public boolean apply(final SpellAbility spellAbility) {
                return spellAbility instanceof LandAbility;
            }
        });

        SpellAbility chosenSa = chooseSpellAbilityToPlayFromList(saList, true);

        if (topOwnedByAI && !mustRespond && chosenSa != ComputerUtilAbility.getFirstCopySASpell(saList)) {
            return null; // not planning to copy the spell and not marked as something the AI would respond to
        }

        return chosenSa;
    }

    private SpellAbility chooseSpellAbilityToPlayFromList(final List<SpellAbility> all, boolean skipCounter) {
        if (all == null || all.isEmpty())
            return null;

        try {
            Collections.sort(all, saComparator); // put best spells first
        }
        catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            String assertex = ComparatorUtil.verifyTransitivity(saComparator, all);
            Sentry.capture(ex.getMessage() + "\nAssertionError [verifyTransitivity]: " + assertex);
        }

        for (final SpellAbility sa : ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player)) {
            // Don't add Counterspells to the "normal" playcard lookups
            if (skipCounter && sa.getApi() == ApiType.Counter) {
                continue;
            }

            if (sa.getHostCard().hasKeyword(Keyword.STORM)
                    && sa.getApi() != ApiType.Counter // AI would suck at trying to deliberately proc a Storm counterspell
                    && CardLists.filter(player.getCardsIn(ZoneType.Hand), Predicates.not(Predicates.or(CardPredicates.Presets.LANDS, CardPredicates.hasKeyword("Storm")))).size() > 0) {
                if (game.getView().getStormCount() < this.getIntProperty(AiProps.MIN_COUNT_FOR_STORM_SPELLS)) {
                    // skip evaluating Storm unless we reached the minimum Storm count
                    continue;
                }
            }

            sa.setActivatingPlayer(player);
            SpellAbility root = sa.getRootAbility();

            if (root.isSpell() || root.isTrigger() || root.isReplacementAbility()) {
                sa.setLastStateBattlefield(game.getLastStateBattlefield());
                sa.setLastStateGraveyard(game.getLastStateGraveyard());
            }

            AiPlayDecision opinion = canPlayAndPayFor(sa);

            // reset LastStateBattlefield
            sa.setLastStateBattlefield(CardCollection.EMPTY);
            sa.setLastStateGraveyard(CardCollection.EMPTY);
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
                // case we can revive. Might wanna do some additional
                // checking here for Flashback and the like.
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
        if (spell instanceof WrappedAbility)
            return doTrigger(((WrappedAbility)spell).getWrappedAbility(), mandatory);
        if (spell.getApi() != null)
            return SpellApiToAi.Converter.get(spell.getApi()).doTriggerAI(player, spell, mandatory);
        if (spell.getPayCosts() == Cost.Zero && !spell.usesTargeting()) {
            // For non-converted triggers (such as Cumulative Upkeep) that don't have costs or targets to worry about
            return true;
        }
        return false;
    }
    
    /**
     * Ai should run.
     *
     * @param sa the sa
     * @return true, if successful
     */
    public final boolean aiShouldRun(final ReplacementEffect effect, final SpellAbility sa, GameEntity affected) {
        Card hostCard = effect.getHostCard();
        if (hostCard.hasAlternateState()) {
            hostCard = game.getCardState(hostCard);
        }

        if (effect.hasParam("AILogic") && effect.getParam("AILogic").equalsIgnoreCase("ProtectFriendly")) {
            final Player controller = hostCard.getController();
            if (affected instanceof Player) {
                return !((Player) affected).isOpponentOf(controller);
            }
            if (affected instanceof Card) {
                return !((Card) affected).getController().isOpponentOf(controller);
            }
        }
        if (effect.hasParam("AICheckSVar")) {
            System.out.println("aiShouldRun?" + sa);
            final String svarToCheck = effect.getParam("AICheckSVar");
            String comparator = "GE";
            int compareTo = 1;

            if (effect.hasParam("AISVarCompare")) {
                final String fullCmp = effect.getParam("AISVarCompare");
                comparator = fullCmp.substring(0, 2);
                final String strCmpTo = fullCmp.substring(2);
                try {
                    compareTo = Integer.parseInt(strCmpTo);
                } catch (final Exception ignored) {
                    if (sa == null) {
                        compareTo = AbilityUtils.calculateAmount(hostCard, hostCard.getSVar(strCmpTo), effect);
                    } else {
                        compareTo = AbilityUtils.calculateAmount(hostCard, hostCard.getSVar(strCmpTo), sa);
                    }
                }
            }

            int left = 0;

            if (sa == null) {
                left = AbilityUtils.calculateAmount(hostCard, svarToCheck, effect);
            } else {
                left = AbilityUtils.calculateAmount(hostCard, svarToCheck, sa);
            }
            System.out.println("aiShouldRun?" + left + comparator + compareTo);
            return Expressions.compare(left, comparator, compareTo);
        } else if (effect.hasParam("AICheckDredge")) {
            return player.getCardsIn(ZoneType.Library).size() > 8 || player.isCardInPlay("Laboratory Maniac");
        } else return sa != null && doTrigger(sa, false);

    }

    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        // AI would play everything. But limits to one copy of (Leyline of Singularity) and (Gemstone Caverns)
        
        List<SpellAbility> result = Lists.newArrayList();
        for (SpellAbility sa : usableFromOpeningHand) {
            // Is there a better way for the AI to decide this?
            if (doTrigger(sa, false)) {
                result.add(sa);
            }
        }
        
        boolean hasLeyline1 = false;
        SpellAbility saGemstones = null;

        List<SpellAbility> toRemove = Lists.newArrayList();
        for (SpellAbility sa : result) {
            String srcName = sa.getHostCard().getName();
            if ("Gemstone Caverns".equals(srcName)) {
                if (saGemstones == null)
                    saGemstones = sa;
                else
                    toRemove.add(sa);
            } else if ("Leyline of Singularity".equals(srcName)) {
                if (!hasLeyline1)
                    hasLeyline1 = true;
                else
                    toRemove.add(sa);
            }
        }
        for(SpellAbility sa : toRemove) {
            result.remove(sa);
        }
        
        // Play them last
        if (saGemstones != null) {
            result.remove(saGemstones);
            result.add(saGemstones);
        }
        
        return result;
    }

    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        final Card source = sa.getHostCard();
        final String logic = sa.getParamOrDefault("AILogic", "Max");
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
            int cardsInLib = player.getCardsIn(ZoneType.Library).size();
            if (cardsInLib >= max && player.isCardInPlay("Laboratory Maniac")) {
                return max;
            }
            int cardsInHand = player.getCardsIn(ZoneType.Hand).size();
            int maxDraw = Math.min(player.getMaxHandSize() + 2 - cardsInHand, max);
            int maxCheckLib = Math.min(maxDraw, cardsInLib);
            return Math.max(min, maxCheckLib);
        } else if ("RepeatDraw".equals(logic)) {
            int remaining = player.getMaxHandSize() - player.getCardsIn(ZoneType.Hand).size()
                    + MyRandom.getRandom().nextInt(3);
            return Math.max(remaining, min) / 2;
        } else if ("LowestLoseLife".equals(logic)) {
            return MyRandom.getRandom().nextInt(Math.min(player.getLife() / 3, player.getWeakestOpponent().getLife())) + 1;
        } else if ("HighestLoseLife".equals(logic)) {
            return Math.min(player.getLife() -1,MyRandom.getRandom().nextInt(Math.max(player.getLife() / 3, player.getWeakestOpponent().getLife())) + 1);
        } else if ("HighestGetCounter".equals(logic)) {
            return MyRandom.getRandom().nextInt(3);
        } else if (source.hasSVar("EnergyToPay")) {
            return AbilityUtils.calculateAmount(source, source.getSVar("EnergyToPay"), sa);
        } else if ("Vermin".equals(logic)) {
            return MyRandom.getRandom().nextInt(Math.max(player.getLife() - 5, 0));
        } else if ("SweepCreatures".equals(logic)) {
            int minAllowedChoice = AbilityUtils.calculateAmount(source, sa.getParam("Min"), sa);
            int choiceLimit = AbilityUtils.calculateAmount(source, sa.getParam("Max"), sa);
            int maxCreatures = 0;
            for (Player opp : player.getOpponents()) {
                maxCreatures = Math.max(maxCreatures, opp.getCreaturesInPlay().size());
            }
            return Math.min(choiceLimit, Math.max(minAllowedChoice, maxCreatures));
        }
        return max;
    }

    public boolean confirmPayment(CostPart costPart) {
        throw new UnsupportedOperationException("AI is not supposed to reach this code at the moment");
    }

    public CardCollection chooseCardsForEffect(CardCollectionView pool, SpellAbility sa, int min, int max, boolean isOptional, Map<String, Object> params) {
        if (sa == null || sa.getApi() == null) {
            throw new UnsupportedOperationException();
        }
        CardCollection result = new CardCollection();
        if (sa.hasParam("AIMaxAmount")) {
            max = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("AIMaxAmount"), sa);
        }
        switch (sa.getApi()) {
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
            case FlipOntoBattlefield:
                if ("DamageCreatures".equals(sa.getParam("AILogic"))) {
                    int maxToughness = Integer.valueOf(sa.getSubAbility().getParam("NumDmg"));
                    CardCollectionView rightToughness = CardLists.filter(pool, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card card) {
                            return card.getController().isOpponentOf(sa.getActivatingPlayer())
                                    && card.getNetToughness() <= maxToughness
                                    && card.canBeDestroyed();
                        }
                    });
                    Card bestCreature = ComputerUtilCard.getBestCreatureAI(rightToughness.isEmpty() ? pool : rightToughness);
                    if (bestCreature != null) {
                        result.add(bestCreature);
                    } else {
                        result.add(Aggregates.random(pool)); // should ideally never get here
                    }
                } else {
                    CardCollectionView viableOptions = CardLists.filter(pool, Predicates.and(CardPredicates.isControlledByAnyOf(sa.getActivatingPlayer().getOpponents())),
                            new Predicate<Card>() {
                                @Override
                                public boolean apply(Card card) {
                                    return card.canBeDestroyed();
                                }
                            });
                    Card best = ComputerUtilCard.getBestAI(viableOptions);
                    if (best == null) {
                        best = Aggregates.random(pool); // should ideally never get here either
                    }
                    result.add(best);
                }
                break;
            default:
                CardCollection editablePool = new CardCollection(pool);
                for (int i = 0; i < max; i++) {
                    Card c = player.getController().chooseSingleEntityForEffect(editablePool, sa, null, isOptional, params);
                    if (c != null) {
                        result.add(c);
                        editablePool.remove(c);
                    } else {
                        break;
                    }

                    // Special case for Bow to My Command which simulates a complex tap cost via ChooseCard
                    // TODO: consider enhancing support for tapXType<Any/...> in UnlessCost to get rid of this hack
                    if ("BowToMyCommand".equals(sa.getParam("AILogic"))) {
                        if (!sa.getHostCard().isInZone(ZoneType.Command)) {
                            // Make sure that other opponents do not tap for an already abandoned scheme
                            result.clear();
                            break;
                        }

                        int totPower = 0;
                        for (Card p : result) {
                            totPower += p.getNetPower();
                        }
                        if (totPower >= 8) {
                            break;
                        }
                    }
                }
        }

        // TODO: Hack for Phyrexian Dreadnought. Might need generalization (possibly its own AI logic)
        if ("Phyrexian Dreadnought".equals(ComputerUtilAbility.getAbilitySourceName(sa))) {
            result = SpecialCardAi.PhyrexianDreadnought.reviseCreatureSacList(player, sa, result);
        }

        return result;
    }

    public Collection<? extends PaperCard> complainCardsCantPlayWell(Deck myDeck) {
        List<PaperCard> result = Lists.newArrayList();
        // When using simulation, AI should be able to figure out most cards.
        if (!useSimulation) {
            for (Entry<DeckSection, CardPool> ds : myDeck) {
                for (Entry<PaperCard, Integer> cp : ds.getValue()) {
                    if (cp.getKey().getRules().getAiHints().getRemAIDecks())
                        result.add(cp.getKey());
                }
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
        if ("Aminatou".equals(sa.getParam("AILogic")) && game.getPlayers().size() > 2) {
            CardCollection all = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.NONLAND_PERMANENTS);
            CardCollection left = CardLists.filterControlledBy(all, game.getNextPlayerAfter(player, Direction.Left));
            CardCollection right = CardLists.filterControlledBy(all, game.getNextPlayerAfter(player, Direction.Right));
            return Aggregates.sum(left, Accessors.fnGetCmc) > Aggregates.sum(right, Accessors.fnGetCmc);
        }
        return MyRandom.getRandom().nextBoolean();
    }

    public boolean chooseEvenOdd(SpellAbility sa) {
        String aiLogic = sa.getParamOrDefault("AILogic", "");

        if (aiLogic.equals("AlwaysEven")) {
            return false; // false is Even
        } else if (aiLogic.equals("AlwaysOdd")) {
            return true; // true is Odd
        } else if (aiLogic.equals("Random")) {
            return MyRandom.getRandom().nextBoolean();
        } else if (aiLogic.equals("CMCInHand")) {
            CardCollectionView hand = sa.getActivatingPlayer().getCardsIn(ZoneType.Hand);
            int numEven = CardLists.filter(hand, CardPredicates.evenCMC()).size();
            int numOdd = CardLists.filter(hand, CardPredicates.oddCMC()).size();
            return numOdd > numEven;
        } else if (aiLogic.equals("CMCOppControls")) {
            CardCollectionView hand = sa.getActivatingPlayer().getOpponents().getCardsIn(ZoneType.Battlefield);
            int numEven = CardLists.filter(hand, CardPredicates.evenCMC()).size();
            int numOdd = CardLists.filter(hand, CardPredicates.oddCMC()).size();
            return numOdd > numEven;
        } else if (aiLogic.equals("CMCOppControlsByPower")) {
            // TODO: improve this to check for how dangerous those creatures actually are relative to host card
            CardCollectionView hand = sa.getActivatingPlayer().getOpponents().getCardsIn(ZoneType.Battlefield);
            int powerEven = Aggregates.sum(CardLists.filter(hand, CardPredicates.evenCMC()), Accessors.fnGetNetPower);
            int powerOdd = Aggregates.sum(CardLists.filter(hand, CardPredicates.oddCMC()), Accessors.fnGetNetPower);
            return powerOdd > powerEven;
        }
        return MyRandom.getRandom().nextBoolean(); // outside of any specific logic, choose randomly
    }

    public Card chooseCardToHiddenOriginChangeZone(ZoneType destination, List<ZoneType> origin, SpellAbility sa,
            CardCollection fetchList, Player player2, Player decider) {
        if (useSimulation) {
            return simPicker.chooseCardToHiddenOriginChangeZone(destination, origin, sa, fetchList, player2, decider);
        }

        if (sa.getApi() == ApiType.Explore) {
            return ExploreAi.shouldPutInGraveyard(fetchList, decider);
        } else if (sa.getApi() == ApiType.Learn) {
            return LearnAi.chooseCardToLearn(fetchList, decider, sa);
        } else {
            return ChangeZoneAi.chooseCardToHiddenOriginChangeZone(destination, origin, sa, fetchList, player2, decider);
        }
    }

    public List<SpellAbility> orderPlaySa(List<SpellAbility> activePlayerSAs) {
        // list is only one or empty, no need to filter
        if (activePlayerSAs.size() < 2) {
            return activePlayerSAs;
        }

        List<SpellAbility> result = Lists.newArrayList();

        // filter list by ApiTypes
        List<SpellAbility> discard = filterListByApi(activePlayerSAs, ApiType.Discard);
        List<SpellAbility> mandatoryDiscard = filterList(discard, SpellAbilityPredicates.isMandatory());

        List<SpellAbility> draw = filterListByApi(activePlayerSAs, ApiType.Draw);

        List<SpellAbility> putCounter = filterListByApi(activePlayerSAs, ApiType.PutCounter);
        List<SpellAbility> putCounterAll = filterListByApi(activePlayerSAs, ApiType.PutCounterAll);

        List<SpellAbility> evolve = filterList(putCounter, SpellAbilityPredicates.hasParam("Evolve"));

        List<SpellAbility> token = filterListByApi(activePlayerSAs, ApiType.Token);
        List<SpellAbility> pump = filterListByApi(activePlayerSAs, ApiType.Pump);
        List<SpellAbility> pumpAll = filterListByApi(activePlayerSAs, ApiType.PumpAll);

        // do mandatory discard early if hand is empty or has DiscardMe card
        boolean discardEarly = false;
        CardCollectionView playerHand = player.getCardsIn(ZoneType.Hand);
        if (playerHand.isEmpty() || CardLists.count(playerHand, CardPredicates.hasSVar("DiscardMe")) > 0) {
            discardEarly = true;
            result.addAll(mandatoryDiscard);
        }

        // token should be added first so they might get the pump bonus
        result.addAll(token);
        result.addAll(pump);
        result.addAll(pumpAll);

        // do Evolve Trigger before other PutCounter SpellAbilities
        // do putCounter before Draw/Discard because it can cause a Draw Trigger
        result.addAll(evolve);
        result.addAll(putCounter);
        result.addAll(putCounterAll);

        // do Draw before Discard
        result.addAll(draw);
        result.addAll(discard); // optional Discard, probably combined with Draw

        if (!discardEarly) {
            result.addAll(mandatoryDiscard);
        }

        result.addAll(activePlayerSAs);

        //need to reverse because of magic stack
        Collections.reverse(result);
        return result;
    }

    // TODO move to more common place
    private <T> List<T> filterList(List<T> input, Predicate<? super T> pred) {
        List<T> filtered = Lists.newArrayList(Iterables.filter(input, pred));
        input.removeAll(filtered);
        return filtered;
    }

    // TODO move to more common place
    private List<SpellAbility> filterListByApi(List<SpellAbility> input, ApiType type) {
        return filterList(input, SpellAbilityPredicates.isApi(type));
    }

    private <T extends CardTraitBase> List<T> filterListByAiLogic(List<T> list, final String logic) {
        return filterList(list, CardTraitPredicates.hasParam("AiLogic", logic));
    }

    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat) {
        if (simPicker != null) {
            return simPicker.chooseModeForAbility(sa, possible, min, num, allowRepeat);
        }
        return null;
    }

    public CardCollectionView chooseSacrificeType(String type, SpellAbility ability, int amount, final CardCollectionView exclude) {
        if (simPicker != null) {
            return simPicker.chooseSacrificeType(type, ability, amount, exclude);
        }
        return ComputerUtil.chooseSacrificeType(player, type, ability, ability.getTargetCard(), amount, exclude);
    }

    private boolean checkAiSpecificRestrictions(final SpellAbility sa) {
        // AI-specific restrictions specified as activation parameters in spell abilities

        if (sa.hasParam("AILifeThreshold")) {
            return player.getLife() > Integer.parseInt(sa.getParam("AILifeThreshold"));
        }

        return true;
    }

    // AI logic for choosing which replacement effect to apply happens here.
    public ReplacementEffect chooseSingleReplacementEffect(List<ReplacementEffect> list) {
        // no need to choose anything
        if (list.size() <= 1) {
            return Iterables.getFirst(list, null);
        }

        ReplacementType mode = Iterables.getFirst(list, null).getMode();

        // replace lifegain effects
        if (mode.equals(ReplacementType.GainLife)) {
            List<ReplacementEffect> noGain = filterListByAiLogic(list, "NoLife");
            List<ReplacementEffect> loseLife = filterListByAiLogic(list, "LoseLife");
            List<ReplacementEffect> doubleLife = filterListByAiLogic(list, "DoubleLife");
            List<ReplacementEffect> lichDraw = filterListByAiLogic(list, "LichDraw");

            if (!noGain.isEmpty()) {
                // no lifegain is better than lose life
                return Iterables.getFirst(noGain, null);
            } else if (!loseLife.isEmpty()) {
                // lose life before double life to prevent lose double
                return Iterables.getFirst(loseLife, null);
            } else if (!lichDraw.isEmpty()) {
                // lich draw before double life to prevent to draw to much
                return Iterables.getFirst(lichDraw, null);
            } else if (!doubleLife.isEmpty()) {
                // other than that, do double life
                return Iterables.getFirst(doubleLife, null);
            }
        } else if (mode.equals(ReplacementType.DamageDone)) {
            List<ReplacementEffect> prevention = filterList(list, CardTraitPredicates.hasParam("Prevention"));

            // TODO when Protection is done as ReplacementEffect do them
            // before normal prevention
            if (!prevention.isEmpty()) {
                return Iterables.getFirst(prevention, null);
            }
        }

        // TODO always lower counters with Vorinclex first, might turn it from 1 to 0 as final

        return Iterables.getFirst(list, null);
    }

}
