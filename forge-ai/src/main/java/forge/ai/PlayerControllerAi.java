package forge.ai;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import forge.LobbyPlayer;
import forge.ai.ability.ProtectAi;
import forge.card.CardStateName;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardUtil;
import forge.game.card.CardView;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostAdjustment;
import forge.game.cost.CostExile;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.player.PlayerView;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.Ability;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.LandAbility;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.ITriggerEvent;
import forge.util.MyRandom;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;


/**
 * A prototype for player controller class
 *
 * Handles phase skips for now.
 */
public class PlayerControllerAi extends PlayerController {
    private final AiController brains;

    public PlayerControllerAi(Game game, Player p, LobbyPlayer lp) {
        super(game, p, lp);

        brains = new AiController(p, game);
    }

    public void allowCheatShuffle(boolean value) {
        brains.allowCheatShuffle(value);
    }

    public void setUseSimulation(boolean value) {
        brains.setUseSimulation(value);
    }

    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        if (abilities.size() == 0) {
            return null;
        }
        return abilities.get(0);
    }

    public AiController getAi() {
        return brains;
    }

    @Override
    public boolean isAI() {
        return true;
    }

    @Override
    public List<PaperCard> sideboard(Deck deck, GameType gameType, String message) {
        // AI does not know how to sideboard
        return null;
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, int damageDealt, GameEntity defender, boolean overrideOrder) {
        return ComputerUtilCombat.distributeAIDamage(attacker, blockers, damageDealt, defender, overrideOrder);
    }

    @Override
    public Map<GameEntity, Integer> divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount) {
        // TODO: AI current can't use this so this is not implemented.
        return new HashMap<>();
    }

    @Override
    public Map<Byte, Integer> specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different) {
        Map<Byte, Integer> result = new HashMap<>();
        for (int i = 0; i < manaAmount; ++i) {
            Byte chosen = chooseColor("", sa, colorSet);
            if (result.containsKey(chosen)) {
                result.put(chosen, result.get(chosen) + 1);
            } else {
                result.put(chosen, 1);
            }
            if (different) {
                colorSet = ColorSet.fromMask(colorSet.getColor() - chosen);
            }
        }
        return result;
    }

    @Override
    public Integer announceRequirements(SpellAbility ability, String announce) {
        // For now, these "announcements" are made within the AI classes of the appropriate SA effects
        if (ability.getApi() != null) {
            switch (ability.getApi()) {
                case ChooseNumber:
                    Player payingPlayer = ability.getActivatingPlayer();
                    String logic = ability.getParamOrDefault("AILogic", "");
                    boolean anyController = logic.equals("MaxForAnyController");

                    if (logic.startsWith("PowerLeakMaxMana.") && ability.getHostCard().isEnchantingCard()) {
                        // For cards like Power Leak, the payer will be the owner of the enchanted card
                        // TODO: is there any way to generalize this and avoid a special exclusion?
                        payingPlayer = ability.getHostCard().getEnchantingCard().getController();
                    }

                    int number = ComputerUtilMana.determineLeftoverMana(ability, player);

                    if (logic.startsWith("MaxMana.") || logic.startsWith("PowerLeakMaxMana.")) {
                        number = Math.min(number, Integer.parseInt(logic.substring(logic.indexOf(".") + 1)));
                    }

                    return payingPlayer.isOpponentOf(player) && !anyController ? 0 : number;
                case BidLife:
                    return 0;
                default:
                    return null;
            }
        }
        return null; // return incorrect value to indicate that
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message) {
        return ComputerUtil.choosePermanentsToSacrifice(player, validTargets, max, sa, false, min == 0);
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message) {
        return ComputerUtil.choosePermanentsToSacrifice(player, validTargets, max, sa, true, min == 0);
    }

    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional, Map<String, Object> params) {
        return brains.chooseCardsForEffect(sourceList, sa, min, max, isOptional, params);
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        if (delayedReveal != null) {
            reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
        }
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseSingleEntity(player, sa, (FCollection<T>)optionList, isOptional, targetedPlayer, params);
    }

    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(
            FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title,
            Player targetedPlayer, Map<String, Object> params) {
        if (delayedReveal != null) {
            reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
        }
        FCollection<T> remaining = new FCollection<>(optionList);
        List<T> selecteds = new ArrayList<>();
        T selected;
        do {
            selected = chooseSingleEntityForEffect(remaining, null, sa, title, selecteds.size()>=min, targetedPlayer, params);
            if ( selected != null ) {
                remaining.remove(selected);
                selecteds.add(selected);
            }
        } while ( (selected != null ) && (selecteds.size() < max) );
        return selecteds;
    }

    @Override
    public List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title,
            int num, Map<String, Object> params) {
        List<SpellAbility> remaining = Lists.newArrayList(spells);
        List<SpellAbility> selecteds = Lists.newArrayList();
        SpellAbility selected;
        do {
            selected = chooseSingleSpellForEffect(remaining, sa, title, params);
            if ( selected != null ) {
                remaining.remove(selected);
                selecteds.add(selected);
            }
        } while ( (selected != null ) && (selecteds.size() < num) );
        return selecteds;
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title,
            Map<String, Object> params) {
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseSingleSpellAbility(player, sa, spells, params);
    }

    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return getAi().confirmAction(sa, mode, message);
    }

    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode mode, String string,
            int bid, Player winner) {
        return getAi().confirmBidAction(sa, mode, string, bid, winner);
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        return getAi().confirmStaticApplication(hostCard, affected, logic, message);
    }

    @Override
    public boolean confirmTrigger(WrappedAbility wrapper) {
        final SpellAbility sa = wrapper.getWrappedAbility();
        //final Trigger regtrig = wrapper.getTrigger();
        if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Deathmist Raptor")) {
            return true;
        }
        if (wrapper.isMandatory()) {
            return true;
        }
        // Store/replace target choices more properly to get this SA cleared.
        TargetChoices tc = null;
        TargetChoices subtc = null;
        boolean storeChoices = sa.usesTargeting();
        final SpellAbility sub = sa.getSubAbility();
        boolean storeSubChoices = sub != null && sub.usesTargeting();
        boolean ret = true;

        if (storeChoices) {
            tc = sa.getTargets();
            sa.resetTargets();
        }
        if (storeSubChoices) {
            subtc = sub.getTargets();
            sub.resetTargets();
        }
        // There is no way this doTrigger here will have the same target as stored above
        // So it's possible it's making a different decision here than will actually happen
        if (!brains.doTrigger(sa, false)) {
            ret = false;
        }
        if (storeChoices) {
            sa.resetTargets();
            sa.setTargets(tc);
        }
        if (storeSubChoices) {
            sub.resetTargets();
            sub.setTargets(subtc);
        }

        return ret;
    }

    @Override
    public Player chooseStartingPlayer(boolean isFirstgame) {
        return this.player; // AI is brave :)
    }

    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        return AiBlockController.orderBlockers(attacker, blockers);
    }

    @Override
    public List<Card> exertAttackers(List<Card> attackers) {
        return AiAttackController.exertAttackers(attackers);
    }

    @Override
    public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
    	return AiBlockController.orderBlocker(attacker, blocker, oldBlockers);
    }

    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        return AiBlockController.orderAttackers(blocker, attackers);
    }

    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix) {
        // We don't know how to reveal cards to AI
    }

    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix) {
        // We don't know how to reveal cards to AI
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        CardCollection toBottom = new CardCollection();
        CardCollection toTop = new CardCollection();

        for (Card c: topN) {
            if (ComputerUtil.scryWillMoveCardToBottomOfLibrary(player, c)) {
                toBottom.add(c);
            } else {
                toTop.add(c);
            }
        }

        // put the rest on top in random order
        Collections.shuffle(toTop, MyRandom.getRandom());
        return ImmutablePair.of(toTop, toBottom);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#arrangeForSurveil(forge.game.card.CardCollection)
     */
    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN) {
        CardCollection toGraveyard = new CardCollection();
        CardCollection toTop = new CardCollection();

        // TODO: Currently this logic uses the same routine as Scry. Possibly differentiate this and implement
        // a specific logic for Surveil (e.g. maybe to interact better with Reanimator strategies etc.).
        if (getPlayer().getCardsIn(ZoneType.Library).size() <= getAi().getIntProperty(AiProps.SURVEIL_NUM_CARDS_IN_LIBRARY_TO_BAIL)) {
            toTop.addAll(topN);
        } else {
            for (Card c : topN) {
                if (ComputerUtil.scryWillMoveCardToBottomOfLibrary(player, c)) {
                    toGraveyard.add(c);
                } else {
                    toTop.add(c);
                }
            }
        }

        Collections.shuffle(toTop, MyRandom.getRandom());
        return ImmutablePair.of(toTop, toGraveyard);
    }

    @Override
    public boolean willPutCardOnTop(Card c) {
        // This is used for Clash. Currently uses Scry logic to determine whether the card should be put on top.
        // Note that the AI does not know what will happen next (another clash or that would become his topdeck)

        return !ComputerUtil.scryWillMoveCardToBottomOfLibrary(player, c);
    }

    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source) {
        //TODO Add more logic for AI ordering here

        if (cards.isEmpty()) {
            return cards;
        }

        if (destinationZone == ZoneType.Graveyard) {
            // In presence of Volrath's Shapeshifter in deck, try to place the best creature on top of the graveyard
            if (!CardLists.filter(getGame().getCardsInGame(), new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    // need a custom predicate here since Volrath's Shapeshifter may have a different name OTB
                    return card.getOriginalState(CardStateName.Original).getName().equals("Volrath's Shapeshifter");
                }
            }).isEmpty()) {
                int bestValue = 0;
                Card bestCreature = null;
                for (Card c : cards) {
                    int curValue = ComputerUtilCard.evaluateCreature(c);
                    if (c.isCreature() && curValue > bestValue) {
                        bestValue = curValue;
                        bestCreature = c;
                    }
                }

                if (bestCreature != null) {
                    CardCollection reordered = new CardCollection();
                    for (Card c : cards) {
                        if (!c.equals(bestCreature)) {
                            reordered.add(c);
                        }
                    }
                    reordered.add(bestCreature);
                    return reordered;
                }
            }
        } else if (destinationZone == ZoneType.Library) {
            // Ponder and similar cards
            Player p = cards.getFirst().getController(); // whose library are we reordering?
            CardCollection reordered = new CardCollection();

            // Try to use the Scry logic to figure out what should be closer to the top and what should be closer to the bottom
            CardCollection topLands = new CardCollection(), topNonLands = new CardCollection(), bottom = new CardCollection();
            for (Card c : cards) {
                if (ComputerUtil.scryWillMoveCardToBottomOfLibrary(p, c)) {
                    bottom.add(c);
                } else {
                    if (c.isLand()) {
                        topLands.add(c);
                    } else {
                        topNonLands.add(c);
                    }
                }
            }

            int landsOTB = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.LANDS_PRODUCING_MANA).size();

            if (!p.isOpponentOf(player)) {
                if (landsOTB <= 2) {
                    // too few lands, add all the lands from the "top" category first
                    reordered.addAll(topLands);
                    topLands.clear();
                } else {
                    // we would have scried a land to top, so add one land from the "top" category if it's available there, but not more
                    if (!topLands.isEmpty()) {
                        Card first = topLands.getFirst();
                        reordered.add(first);
                        topLands.remove(first);
                    }
                }
                // add everything that was deemed playable
                reordered.addAll(topNonLands);
                // then all the land extras that may be there
                reordered.addAll(topLands);
                // and then everything else that was deemed unplayable and thus scriable to the bottom
                reordered.addAll(bottom);
            } else {
                // try to screw the opponent up as much as possible by placing the uncastables first
                reordered.addAll(bottom);
                if (landsOTB <= 5) {
                    reordered.addAll(topNonLands);
                    reordered.addAll(topLands);
                } else {
                    reordered.addAll(topLands);
                    reordered.addAll(topNonLands);
                }
            }

            assert(reordered.size() == cards.size());

            return reordered;
        }

        // Default: return with the same order as was passed into this method
        return cards;
    }

    @Override
    public CardCollection chooseCardsToDiscardFrom(Player p, SpellAbility sa, CardCollection validCards, int min, int max) {
        if (p == player) {
            return brains.getCardsToDiscard(min, max, validCards, sa);
        }

        boolean isTargetFriendly = !p.isOpponentOf(player);

        return isTargetFriendly
               ? ComputerUtil.getCardsToDiscardFromFriend(player, p, sa, validCards, min, max)
               : ComputerUtil.getCardsToDiscardFromOpponent(player, p, sa, validCards, min, max);
    }

    @Override
    public void playSpellAbilityForFree(SpellAbility copySA, boolean mayChooseNewTargets) {
        // Ai is known to set targets in doTrigger, so if it cannot choose new targets, we won't call canPlays
        if (mayChooseNewTargets) {
            if (copySA instanceof Spell) {
                Spell spell = (Spell) copySA;
                ((PlayerControllerAi) player.getController()).getAi().canPlayFromEffectAI(spell, true, true);
            } else {
                getAi().canPlaySa(copySA);
            }
        }
        ComputerUtil.playSpellAbilityForFree(player, copySA);
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean canSetupTargets) {
        if (canSetupTargets)
            brains.doTrigger(effectSA, true); // first parameter does not matter, since return value won't be used
        ComputerUtil.playNoStack(player, effectSA, getGame());
    }

    @Override
    public CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave) {
        return getAi().chooseCardsToDelve(genericAmount, grave);
    }

    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional) {
        // AI currently can't do this. But when it can it will need to be based on Ability API
        return null;
    }

    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(int num, CardCollectionView hand, String uType, SpellAbility sa) {
        String [] splitUTypes = uType.split(",");
        CardCollection cardsOfType = new CardCollection();
        for (String part : splitUTypes) {
            CardCollection partCards = CardLists.getType(hand, part);
            if (!partCards.isEmpty()) {
                cardsOfType.addAll(partCards);
            }
        }
        if (!cardsOfType.isEmpty()) {
            Card toDiscard = Aggregates.itemWithMin(cardsOfType, CardPredicates.Accessors.fnGetCmc);
            return new CardCollection(toDiscard);
        }
        return getAi().getCardsToDiscard(num, null, sa);
    }

    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        return manaChoices.get(0); // no brains used
    }

    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, List<String> invalidTypes, boolean isOptional) {
        String chosen = ComputerUtil.chooseSomeType(player, kindOfType, sa.getParam("AILogic"), validTypes, invalidTypes);
        if (StringUtils.isBlank(chosen) && !validTypes.isEmpty()) {
            chosen = validTypes.iterator().next();
            System.err.println("AI has no idea how to choose " + kindOfType +", defaulting to arbitrary element: chosen");
        }
        return chosen;
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer) {
        return ComputerUtil.vote(player, options, sa, votes, forPlayer);
    }

    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question) {
        return brains.aiShouldRun(replacementEffect, effectSA, affected);
    }

    @Override
    public boolean mulliganKeepHand(Player firstPlayer, int cardsToReturn)  {
        return !ComputerUtil.wantMulligan(player, cardsToReturn);
    }

    @Override
    public CardCollectionView getCardsToMulligan(Player firstPlayer)  {
        if (!ComputerUtil.wantMulligan(player, 0)) {
            return null;
        }

        return player.getCardsIn(ZoneType.Hand);
    }

    @Override
    public CardCollectionView londonMulliganReturnCards(final Player mulliganingPlayer, int cardsToReturn) {
        // TODO This is better than it was before, but still suboptimal (but fast).
        // Maybe score a bunch of hands based on projected hand size and return the "duds"
        CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        int numLandsDesired = (mulliganingPlayer.getStartingHandSize() - cardsToReturn) / 2;

        CardCollection toReturn = new CardCollection();
        for (int i = 0; i < cardsToReturn; i++) {
            hand.removeAll(toReturn);

            CardCollection landsInHand = CardLists.filter(hand, Presets.LANDS);
            int numLandsInHand = landsInHand.size() - CardLists.filter(toReturn, Presets.LANDS).size();

            // If we're flooding with lands, get rid of the worst land we have
            if (numLandsInHand > 0 && numLandsInHand > numLandsDesired) {
                CardCollection producingLands = CardLists.filter(landsInHand, Presets.LANDS_PRODUCING_MANA);
                CardCollection nonProducingLands = CardLists.filter(landsInHand, Predicates.not(Presets.LANDS_PRODUCING_MANA));
                Card worstLand = nonProducingLands.isEmpty() ? ComputerUtilCard.getWorstLand(producingLands)
                        : ComputerUtilCard.getWorstLand(nonProducingLands);
                toReturn.add(worstLand);
                continue;
            }

            // See if we'd scry something to the bottom in this situation. If we want to, probably get rid of it.
            CardCollection scryBottom = new CardCollection();
            for (Card c : hand) {
                // Lands are evaluated separately above, factoring in the number of cards to be returned to the library
                if (!c.isLand() && !toReturn.contains(c) && !willPutCardOnTop(c)) {
                    scryBottom.add(c);
                }
            }
            if (!scryBottom.isEmpty()) {
                CardLists.sortByCmcDesc(scryBottom);
                toReturn.add(scryBottom.getFirst()); // assume the max CMC one is worse since we're not guaranteed to have lands for it
                continue;
            }

            // If we don't want to scry anything to the bottom, remove the worst card that we have in order to satisfy
            // the requirement
            toReturn.add(ComputerUtilCard.getWorstAI(hand));
        }

        return CardCollection.getView(toReturn);
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        brains.declareAttackers(attacker, combat);
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        brains.declareBlockersFor(defender, combat);
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        return brains.chooseSpellAbilityToPlay();
    }

    @Override
    public boolean playChosenSpellAbility(SpellAbility sa) {
        if (sa instanceof LandAbility) {
            if (sa.canPlay()) {
                sa.resolve();
                getGame().updateLastStateForCard(sa.getHostCard());
            }
        } else {
            ComputerUtil.handlePlayingSpellAbility(player, sa, getGame());
        }
        return true;
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        return brains.getCardsToDiscard(numDiscard, null, null);
    }

    @Override
    public CardCollection chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        int numCardsToReveal = Math.min(max, valid.size());
        return numCardsToReveal == 0 ? new CardCollection() : (CardCollection)valid.subList(0, numCardsToReveal);
    }

    @Override
    public boolean payManaOptional(Card c, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose) {
        final Ability ability = new AbilityStatic(c, cost, null) { @Override public void resolve() {} };
        ability.setActivatingPlayer(c.getController());

        // FIXME: This is a hack to check if the AI can play the "exile from library" pay costs (Cumulative Upkeep,
        // e.g. Thought Lash). We have to do it and bail early if the AI can't pay, because otherwise the AI will
        // pay the cost partially, which should not be possible
        int nExileLib = 0;
        List<CostPart> parts = CostAdjustment.adjust(cost, sa).getCostParts();
        for (final CostPart part : parts) {
            if (part instanceof CostExile) {
                CostExile exile = (CostExile) part;
                if (exile.from == ZoneType.Library) {
                    nExileLib += exile.convertAmount();
                }
            }
        }
        if (nExileLib > c.getController().getCardsIn(ZoneType.Library).size()) {
            return false;
        }
        // - End of hack for Exile a card from library Cumulative Upkeep -

        if (ComputerUtilCost.canPayCost(ability, c.getController())) {
            ComputerUtil.playNoStack(c.getController(), ability, getGame());
            return true;
        }
        return false;
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        // AI would play everything. But limits to one copy of (Leyline of Singularity) and (Gemstone Caverns)
        return brains.chooseSaToActivateFromOpeningHand(usableFromOpeningHand);
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        return brains.chooseNumber(sa, title, min, max);
    }

    @Override
    public int chooseNumber(SpellAbility sa, String string, int min, int max, Map<String, Object> params) {
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseNumber(player, sa, min, max, params);
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> options, Player relatedPlayer) {
        return brains.chooseNumber(sa, title, options, relatedPlayer);
    }

    /* (non-Javadoc)
     * @see forge.game.player.PlayerController#chooseFlipResult(forge.Card, forge.game.player.Player, java.lang.String[], boolean)
     */
    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        if (call) {
            // Win if possible
            boolean result = false;
            for (boolean s : results) {
                if (s) {
                    result = s;
                    break;
                }
            }
            return result;
        } else {
            // heads or tails, AI doesn't know which is better now
            int i = MyRandom.getRandom().nextInt(results.length);
            return results[i];
        }
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility saSrc, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        // TODO Teach AI how to determine the most damaging subability when retargeting a spell
        // with multiple targets (Arc Lightning, Cone of Flame, etc.) with Spellskite
        // (currently simply always returns the first valid target ability)
        return allTargets.get(0);
    }

    @Override
    public void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value) {
        // AI should take into consideration creature types, numbers and other information (mostly choices) arriving through this channel
    }

    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultVal) {
        switch(kindOfChoice) {
            case TapOrUntap: return true;
            case UntapOrLeaveTapped:
                Card source = sa.getHostCard();
                if (source != null && source.hasSVar("AIUntapPreference")) {
                    switch (source.getSVar("AIUntapPreference")) {
                        case "Always":
                            return true;
                        case "Never":
                            return false;
                        case "NothingRemembered":
                            if (source.getRememberedCount() == 0) {
                                return true;
                            } else {
                                Card rem = (Card) source.getFirstRemembered();
                                if (!rem.isInPlay()) {
                                    return true;
                                }
                            }
                            break;
                        case "BetterTgtThanRemembered":
                            if (source.getRememberedCount() > 0) {
                                Card rem = (Card) source.getFirstRemembered();
                                //  avoid pumping opponent creature
                                if (!rem.isInPlay() || rem.getController().isOpponentOf(source.getController())) {
                                    return true;
                                }
                                for (Card c : source.getController().getCreaturesInPlay()) {
                                    if (c != rem && ComputerUtilCard.evaluateCreature(c) > ComputerUtilCard.evaluateCreature(rem) + 30) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                            break;
                        default:
                            break;
                    }
                }
                return defaultVal != null && defaultVal.booleanValue();
            case UntapTimeVault: return false; // TODO Should AI skip his turn for time vault?
            case LeftOrRight: return brains.chooseDirection(sa);
            case OddsOrEvens: return brains.chooseEvenOdd(sa); // false is Odd, true is Even
            default:
                return MyRandom.getRandom().nextBoolean();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.game.player.PlayerController#chooseBinary(forge.game.spellability.
     * SpellAbility, java.lang.String,
     * forge.game.player.PlayerController.BinaryChoiceType, java.util.Map)
     */
    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice,
            Map<String, Object> params) {
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseBinary(kindOfChoice, sa, params);
    }

    @Override
    public Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap) {
        int i = MyRandom.getRandom().nextInt(options.size());
        return choiceMap.get(options.get(i));
    }

    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat) {
        List<AbilitySub> result = brains.chooseModeForAbility(sa, possible, min, num, allowRepeat);
        if (result != null) {
            return result;
        }
        /**
         * Called when CharmEffect resolves for the AI to select its choices.
         * The list of chosen options (sa.getChosenList()) should be set by
         * CharmAi.canPlayAi() for cast spells while CharmAi.doTrigger() deals
         * with triggers. The logic in CharmAi should only be called once to
         * account for probabilistic choices that may result in different
         * results in subsequent calls.
         */
        if (sa.getChosenList() == null) {
            getAi().doTrigger(sa, true);
        }
        return sa.getChosenList();
    }

    @Override
    public byte chooseColorAllowColorless(String message, Card card, ColorSet colors) {
        final String c = ComputerUtilCard.getMostProminentColor(player.getCardsIn(ZoneType.Hand));
        byte chosenColorMask = MagicColor.fromName(c);
        if ((colors.getColor() & chosenColorMask) != 0) {
            return chosenColorMask;
        }
        return Iterables.getFirst(colors, (byte)0);
    }

    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        if (colors.countColors() < 2) {
            return Iterables.getFirst(colors, MagicColor.WHITE);
        }
        // You may switch on sa.getApi() here and use sa.getParam("AILogic")
        CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        if (sa.getApi() == ApiType.Mana) {
            hand = CardCollection.combine(hand, player.getCardsIn(ZoneType.Stack));
        }
        final String c = ComputerUtilCard.getMostProminentColor(hand);
        byte chosenColorMask = MagicColor.fromName(c);

        if ((colors.getColor() & chosenColorMask) != 0) {
            return chosenColorMask;
        }
        return Iterables.getFirst(colors, MagicColor.WHITE);
    }

    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, String message,
            Predicate<ICardFace> cpp, String name) {
        throw new UnsupportedOperationException("Should not be called for AI"); // or implement it if you know how
    }

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        return ComputerUtilCard.chooseColor(sa, min, max, options);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.game.player.PlayerController#chooseCounterType(java.util.List,
     * forge.game.spellability.SpellAbility, java.lang.String, java.util.Map)
     */
    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt,
            Map<String, Object> params) {
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseCounterType(options, sa, params);
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String prompt, SpellAbility sa) {
        return brains.confirmPayment(costPart); // AI is expected to know what it is paying for at the moment (otherwise add another parameter to this method)
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(String prompt, List<ReplacementEffect> possibleReplacers) {
        return brains.chooseSingleReplacementEffect(possibleReplacers);
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        String choice = choices.get(0);
        SpellAbility hostsa = null;     //for Protect sub-ability
        if (getGame().stack.size() > 1) {
            for (SpellAbilityStackInstance si : getGame().getStack()) {
                SpellAbility spell = si.getSpellAbility(true);
                if (sa != spell && sa.getHostCard() != spell.getHostCard()) {
                    String s = ProtectAi.toProtectFrom(spell.getHostCard(), sa);
                    if (s != null) {
                        return s;
                    }
                    break;
                }
            }
        }
        final Combat combat = getGame().getCombat();
        if (combat != null) {
            if (getGame().stack.size() == 1) {
                SpellAbility topstack = getGame().stack.peekAbility();
                if (topstack.getSubAbility() == sa) {
                    hostsa = topstack;
                }
            }
            Card toSave = hostsa == null ? sa.getTargetCard() : hostsa.getTargetCard();
            CardCollection threats = null;
            if (toSave != null) {
                if (combat.isBlocked(toSave)) {
                    threats = combat.getBlockers(toSave);
                }
                if (combat.isBlocking(toSave)) {
                    threats = combat.getAttackersBlockedBy(toSave);
                }
            }
            if (threats != null && !threats.isEmpty()) {
                ComputerUtilCard.sortByEvaluateCreature(threats);
                String s = ProtectAi.toProtectFrom(threats.get(0), sa);
                if (s != null) {
                    return s;
                }
            }
        }
        final PhaseHandler ph = getGame().getPhaseHandler();
        if (ph.getPlayerTurn() == sa.getActivatingPlayer() && ph.getPhase() == PhaseType.MAIN1 && sa.getTargetCard() != null) {
            AiAttackController aiAtk = new AiAttackController(sa.getActivatingPlayer(), sa.getTargetCard());
            String s = aiAtk.toProtectAttacker(sa);
            if (s != null) {
                return s;
            }
        }
        final String logic = sa.getParam("AILogic");
        if (logic == null || logic.equals("MostProminentHumanCreatures")) {
            CardCollection list = new CardCollection();
            for (Player opp : player.getOpponents()) {
                list.addAll(opp.getCreaturesInPlay());
            }
            if (list.isEmpty()) {
                list = CardLists.filterControlledBy(getGame().getCardsInGame(), player.getOpponents());
            }
            if (!list.isEmpty()) {
                choice = ComputerUtilCard.getMostProminentColor(list);
            }
        }
        return choice;
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers) {
        final Card source = sa.getHostCard();
        final Ability emptyAbility = new AbilityStatic(source, cost, sa.getTargetRestrictions()) { @Override public void resolve() { } };
        emptyAbility.setActivatingPlayer(player);
        emptyAbility.setTriggeringObjects(sa.getTriggeringObjects());
        emptyAbility.setSVars(sa.getSVars());
        emptyAbility.setXManaCostPaid(sa.getRootAbility().getXManaCostPaid());
        if (ComputerUtilCost.willPayUnlessCost(sa, player, cost, alreadyPaid, allPayers) && ComputerUtilCost.canPayCost(emptyAbility, player)) {
            ComputerUtil.playNoStack(player, emptyAbility, getGame()); // AI needs something to resolve to pay that cost
            return true;
        }
        return false;
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        for (final SpellAbility sa : getAi().orderPlaySa(activePlayerSAs)) {
            if (sa.isTrigger()) {
                if (prepareSingleSa(sa.getHostCard(), sa, true)) {
                    ComputerUtil.playStack(sa, player, getGame());
                }
            } else {
                if (sa.isCopied()) {
                    if (sa.isSpell()) {
                        if (!sa.getHostCard().isInZone(ZoneType.Stack)) {
                            sa.setHostCard(player.getGame().getAction().moveToStack(sa.getHostCard(), sa));
                        } else {
                            player.getGame().getStackZone().add(sa.getHostCard());
                        }
                    }

                    /* FIXME: the new implementation (below) requires implementing setupNewTargets in the AI controller, among other possible changes, otherwise breaks AI
                    if (sa.isMayChooseNewTargets()) {
                        sa.setupNewTargets(player);
                    }
                    */
                    if (sa.isMayChooseNewTargets() && !sa.setupTargets()) {
                        if (sa.isSpell()) {
                            player.getGame().getAction().ceaseToExist(sa.getHostCard(), false);
                        }
                        continue;
                    }
                }
                // need finally add the new spell to the stack
                player.getGame().getStack().add(sa);
            }
        }
    }

    private boolean prepareSingleSa(final Card host, final SpellAbility sa, boolean isMandatory) {
        if (sa.hasParam("TargetingPlayer")) {
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(host, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            return targetingPlayer.getController().chooseTargetsFor(sa);
        } else {
            return brains.doTrigger(sa, isMandatory);
        }
    }

    @Override
    public void playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        if (prepareSingleSa(host, wrapperAbility, isMandatory)) {
            ComputerUtil.playNoStack(wrapperAbility.getActivatingPlayer(), wrapperAbility, getGame());
        }
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        boolean optional = tgtSA.hasParam("Optional");
        boolean noManaCost = tgtSA.hasParam("WithoutManaCost");
        if (tgtSA instanceof Spell) { // Isn't it ALWAYS a spell?
            Spell spell = (Spell) tgtSA;
            if (tgtSA.checkRestrictions(brains.getPlayer()) && (brains.canPlayFromEffectAI(spell, !optional, noManaCost) == AiPlayDecision.WillPlay || !optional)) {
                if (noManaCost) {
                    return ComputerUtil.playSpellAbilityWithoutPayingManaCost(player, tgtSA, getGame());
                }
                return ComputerUtil.playStack(tgtSA, player, getGame());
            }
            return false; // didn't play spell
        }
        return true;
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        return brains.doTrigger(currentAbility, true);
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceUp) {
        if (faceUp.equals("True")) {
            // AI will choose the first pile if it is larger or the same
            // TODO Improve this to be slightly more random to not be so predictable
            return pile1.size() >= pile2.size();
        } else if (faceUp.equals("One")) {
            // Probably want to see if the face up pile has anything "worth it", then potentially take face down pile
            return pile1.size() >= pile2.size();
        } else {
            boolean allCreatures = Iterables.all(Iterables.concat(pile1, pile2), CardPredicates.Presets.CREATURES);
            int cmc1 = allCreatures ? ComputerUtilCard.evaluateCreatureList(pile1) : ComputerUtilCard.evaluatePermanentList(pile1);
            int cmc2 = allCreatures ? ComputerUtilCard.evaluateCreatureList(pile2) : ComputerUtilCard.evaluatePermanentList(pile2);

            // for now, this assumes that the outcome will be bad
            // TODO: This should really have a ChooseLogic param to
            // figure this out
            return "Worst".equals(sa.getParam("AILogic")) ^ (cmc1 >= cmc2);
        }
    }

    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        // Ai won't understand that anyway
    }

    @Override
    public Collection<? extends PaperCard> complainCardsCantPlayWell(Deck myDeck) {
        return brains.complainCardsCantPlayWell(myDeck);
    }

    @Override
    public CardCollectionView cheatShuffle(CardCollectionView list) {
        return brains.getBooleanProperty(AiProps.CHEAT_WITH_MANA_ON_SHUFFLE) ? brains.cheatShuffle(list) : list;
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        // TODO AI takes all by default
        return losses;
    }

    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt /* ai needs hints as well */, ManaConversionMatrix matrix, boolean isActivatedSa) {
        // TODO Auto-generated method stub
        ManaCostBeingPaid cost = isActivatedSa ? ComputerUtilMana.calculateManaCost(sa, false, 0) : new ManaCostBeingPaid(toPay);
        return ComputerUtilMana.payManaCost(cost, sa, player);
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards, boolean improvise) {
        final Player ai = sa.getActivatingPlayer();
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        //Filter out mana sources that will interfere with payManaCost()
        CardCollection untapped = CardLists.filter(untappedCards, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getManaAbilities().isEmpty();
            }
        });

        // Filter out creatures if AI hasn't attacked yet
        if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            if (improvise) {
                untapped = CardLists.filter(untapped, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {return !c.isCreature();
                    }
                });
            } else {
                return new HashMap<>();
            }
        }

        //Do not convoke potential blockers until after opponent's attack
        final CardCollectionView blockers = ComputerUtilCard.getLikelyBlockers(ai, null);
        if ((ph.isPlayerTurn(ai) && ph.getPhase().isAfter(PhaseType.COMBAT_BEGIN)) ||
                (!ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS))) {
            untapped.removeAll((List<?>)blockers);
            //Add threatened creatures
            if (!ai.getGame().getStack().isEmpty()) {
                final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), null);
                for (Card c : blockers) {
                    if (objects.contains(c) && (!improvise || c.isArtifact())) {
                        untapped.add(c);
                    }
                }
            }
        }
        return ComputerUtilMana.getConvokeOrImproviseFromList(manaCost, untapped, improvise);
    }

    @Override
    public String chooseCardName(SpellAbility sa, Predicate<ICardFace> cpp, String valid, String message) {
        if (sa.hasParam("AILogic")) {
            CardCollectionView aiLibrary = player.getCardsIn(ZoneType.Library);
            CardCollectionView oppLibrary = player.getWeakestOpponent().getCardsIn(ZoneType.Library);
            final Card source = sa.getHostCard();
            final String logic = sa.getParam("AILogic");

            if (source != null && source.getState(CardStateName.Original).hasIntrinsicKeyword("Hidden agenda")) {
                // If any Conspiracies are present, try not to choose the same name twice
                // (otherwise the AI will spam the same name)
                for (Card consp : player.getCardsIn(ZoneType.Command)) {
                    if (consp.getState(CardStateName.Original).hasIntrinsicKeyword("Hidden agenda")) {
                        String chosenName = consp.getNamedCard();
                        if (!chosenName.isEmpty()) {
                            aiLibrary = CardLists.filter(aiLibrary, Predicates.not(CardPredicates.nameEquals(chosenName)));
                        }
                    }
                }
            }

            if (logic.equals("MostProminentInComputerDeck")) {
                return ComputerUtilCard.getMostProminentCardName(aiLibrary);
            } else if (logic.equals("MostProminentInHumanDeck")) {
                return ComputerUtilCard.getMostProminentCardName(oppLibrary);
            } else if (logic.equals("MostProminentCreatureInComputerDeck")) {
                CardCollectionView cards = CardLists.getValidCards(aiLibrary, "Creature", player, sa.getHostCard(), sa);
                return ComputerUtilCard.getMostProminentCardName(cards);
            } else if (logic.equals("BestCreatureInComputerDeck")) {
                Card bestCreature = ComputerUtilCard.getBestCreatureAI(aiLibrary);
                return bestCreature != null ? bestCreature.getName() : "Plains";
            } else if (logic.equals("RandomInComputerDeck")) {
                return Aggregates.random(aiLibrary).getName();
            } else if (logic.equals("MostProminentSpellInComputerDeck")) {
                CardCollectionView cards = CardLists.getValidCards(aiLibrary, "Card.Instant,Card.Sorcery", player, sa.getHostCard(), sa);
                return ComputerUtilCard.getMostProminentCardName(cards);
            } else if (logic.equals("CursedScroll")) {
                return SpecialCardAi.CursedScroll.chooseCard(player, sa);
            }
        } else {
            CardCollectionView list = CardLists.filterControlledBy(getGame().getCardsInGame(), player.getOpponents());
            list = CardLists.filter(list, Predicates.not(Presets.LANDS));
            if (!list.isEmpty()) {
                return list.get(0).getName();
            }
        }
        return "Morphling";
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination,
            List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal,
            String selectPrompt, boolean isOptional, Player decider) {
        if (delayedReveal != null) {
            reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
        }
        return brains.chooseCardToHiddenOriginChangeZone(destination, origin, sa, fetchList, player, decider);
    }

    @Override
    public List<Card> chooseCardsForZoneChange(
	    ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, int min, int max,
            DelayedReveal delayedReveal, String selectPrompt, Player decider) {
        // this isn't used
        return null;
    }

    @Override
    public void resetAtEndOfTurn() {
        // TODO - if card memory is ever used to remember something for longer than a turn, make sure it's not reset here.
        getAi().getCardMemory().clearAllRemembered();
    }

    @Override
    public void autoPassCancel() {
        // Do nothing
    }

    @Override
    public void awaitNextInput() {
        // Do nothing
    }
    @Override
    public void cancelAwaitNextInput() {
        // Do nothing
    }

    @Override
    public String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message) {
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseCardName(player, sa, faces);
    }

    @Override
    public Card chooseDungeon(Player ai, List<PaperCard> dungeonCards, String message) {
        // TODO: improve the conditions that define which dungeon is a viable option to choose
        List<String> dungeonNames = Lists.newArrayList();
        for (PaperCard pc : dungeonCards) {
            dungeonNames.add(pc.getName());
        }

        // Don't choose Tomb of Annihilation when life in danger unless we can win right away or can't lose for 0 life
        if (ai.getController().isAI()) { // FIXME: is this needed? Can simulation ever run this for a non-AI player?
            int lifeInDanger = (((PlayerControllerAi) ai.getController()).getAi().getIntProperty(AiProps.AI_IN_DANGER_THRESHOLD));
            if ((ai.getLife() <= lifeInDanger && !ai.cantLoseForZeroOrLessLife())
                    && !(ai.getLife() > 1 && ai.getWeakestOpponent().getLife() == 1)) {
                dungeonNames.remove("Tomb of Annihilation");
            }
        }

        int i = MyRandom.getRandom().nextInt(dungeonNames.size());
        return Card.fromPaperCard(dungeonCards.get(i), ai);
    }

    @Override
    public List<Card> chooseCardsForSplice(SpellAbility sa, List<Card> cards) {
        // sort from best to worst
        CardLists.sortByCmcDesc(cards);

        List<Card> result = Lists.newArrayList();

        SpellAbility oldSA = sa;
        // TODO maybe add some more Logic into it
        for (final Card c : cards) {
            SpellAbility newSA = oldSA.copy();
            AbilityUtils.addSpliceEffect(newSA, c);
            // check if AI still wants or can play the card with spliced effect
            if (AiPlayDecision.WillPlay == getAi().canPlayFromEffectAI((Spell) newSA, false, false)) {
                oldSA = newSA;
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public List<OptionalCostValue> chooseOptionalCosts(SpellAbility chosen,
            List<OptionalCostValue> optionalCostValues) {
        List<OptionalCostValue> chosenOptCosts = Lists.newArrayList();
        Cost costSoFar = chosen.getPayCosts().copy();

        for (OptionalCostValue opt : optionalCostValues) {
            // Choose the optional cost if it can be paid (to be improved later, check for playability and other conditions perhaps)
            Cost fullCost = opt.getCost().copy().add(costSoFar);
            SpellAbility fullCostSa = chosen.copyWithDefinedCost(fullCost);

            // Playability check for Kicker
            if (opt.getType() == OptionalCost.Kicker1 || opt.getType() == OptionalCost.Kicker2) {
                SpellAbility kickedSaCopy = fullCostSa.copy();
                kickedSaCopy.addOptionalCost(opt.getType());
                Card copy = CardUtil.getLKICopy(chosen.getHostCard());
                copy.addOptionalCostPaid(opt.getType());
                if (ComputerUtilCard.checkNeedsToPlayReqs(copy, kickedSaCopy) != AiPlayDecision.WillPlay) {
                    continue; // don't choose kickers we don't want to play
                }
            }

            if (ComputerUtilCost.canPayCost(fullCostSa, player)) {
                chosenOptCosts.add(opt);
                costSoFar.add(opt.getCost());
            }
        }

        return chosenOptCosts;
    }

    @Override
    public boolean confirmMulliganScry(Player p) {
        // Always true?
        return true;
    }

    @Override
    public int chooseNumberForKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt, int max) {
        // TODO: improve the logic depending on the keyword and the playability of the cost-modified SA (enough targets present etc.)
        int chosenAmount = 0;

        Cost costSoFar = sa.getPayCosts().copy();

        for (int i = 0; i < max; i++) {
            costSoFar.add(cost);
            SpellAbility fullCostSa = sa.copyWithDefinedCost(costSoFar);
            if (ComputerUtilCost.canPayCost(fullCostSa, player)) {
                chosenAmount++;
            } else {
                break;
            }
        }

        return chosenAmount;
    }

    @Override
    public CardCollection chooseCardsForEffectMultiple(Map<String, CardCollection> validMap, SpellAbility sa, String title, boolean isOptional) {
        CardCollection choices = new CardCollection();

        for (String mapKey: validMap.keySet()) {
            CardCollection cc = validMap.get(mapKey);
            cc.removeAll(choices);
            Card chosen = ComputerUtilCard.getBestAI(cc);
            if (chosen != null) {
                choices.add(chosen);
            }
        }

        return choices;
    }
}
