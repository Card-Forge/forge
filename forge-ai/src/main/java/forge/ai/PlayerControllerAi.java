package forge.ai;

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
import forge.deck.DeckSection;
import forge.game.*;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.*;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostEnlist;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.*;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.ITriggerEvent;
import forge.util.MyRandom;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.security.InvalidParameterException;
import java.util.*;


/**
 * A prototype for player controller class
 *
 * Handles phase skips for now.
 */
public class PlayerControllerAi extends PlayerController {
    private final AiController brains;

    private boolean pilotsNonAggroDeck = false;

    public PlayerControllerAi(Game game, Player p, LobbyPlayer lp) {
        super(game, p, lp);

        brains = new AiController(p, game);
    }

    public boolean pilotsNonAggroDeck() {
        return pilotsNonAggroDeck;
    }

    public void setupAutoProfile(Deck deck) {
        pilotsNonAggroDeck = deck.getName().contains("Control") || Deck.getAverageCMC(deck) > 3;
    }

    public void allowCheatShuffle(boolean value) {
        brains.allowCheatShuffle(value);
    }

    public void setUseSimulation(boolean value) {
        brains.setUseSimulation(value);
    }

    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        if (abilities.isEmpty()) {
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
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder) {
        return ComputerUtilCombat.distributeAIDamage(player, attacker, blockers, remaining, damageDealt, defender, overrideOrder);
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

                    int number = ComputerUtilMana.determineLeftoverMana(ability, player, false);

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
            if (selected != null) {
                remaining.remove(selected);
                selecteds.add(selected);
            }
        } while (selected != null && selecteds.size() < num);
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
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return getAi().confirmAction(sa, mode, message, params);
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
        return AiAttackController.exertAttackers(attackers, brains.getAttackAggression());
    }
 
    @Override
    public List<Card> enlistAttackers(List<Card> attackers) {
        CardCollection cards = CostEnlist.getCardsForEnlisting(brains.getPlayer());
        cards = CardLists.filter(cards, CardPredicates.hasGreaterPowerThan(0));
        CardCollection chosenAttackers = new CardCollection(attackers);
        ComputerUtilCard.sortByEvaluateCreature(chosenAttackers);

        // do not enlist more than available payment choices (currently ignores multiple instances of Enlist, but can that even happen?)
        if (attackers.size() > cards.size()) {
            chosenAttackers = chosenAttackers.subList(0, cards.size());
        }
        // TODO check if not needed as defender
        return chosenAttackers;
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
        for (Card c : cards) {
            AiCardMemory.rememberCard(player, c, AiCardMemory.MemorySet.REVEALED_CARDS);
        }
    }

    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix) {
        for (CardView cv : cards) {
            AiCardMemory.rememberCard(player, player.getGame().findByView(cv), AiCardMemory.MemorySet.REVEALED_CARDS);
        }
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
        CardLists.shuffle(toTop);
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

        CardLists.shuffle(toTop);
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
            if (Iterables.any(getGame().getCardsInGame(), new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    // need a custom predicate here since Volrath's Shapeshifter may have a different name OTB
                    return card.getOriginalState(CardStateName.Original).getName().equals("Volrath's Shapeshifter");
                }
            })) {
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

            int landsOTB = CardLists.count(p.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.LANDS_PRODUCING_MANA);

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
        ComputerUtil.playNoStack(player, effectSA, getGame(), true);
    }

    @Override
    public CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave) {
        return getAi().chooseCardsToDelve(genericAmount, grave);
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
        String chosen = ComputerUtil.chooseSomeType(player, kindOfType, sa, validTypes, invalidTypes);
        if (StringUtils.isBlank(chosen) && !validTypes.isEmpty()) {
            chosen = validTypes.iterator().next();
            System.err.println("AI has no idea how to choose " + kindOfType +", defaulting to arbitrary element: " + chosen);
        }
        return chosen;
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer) {
        return ComputerUtil.vote(player, options, sa, votes, forPlayer);
    }

    @Override
    public String chooseSector(Card assignee, String ai, List<String> sectors) {
        return Aggregates.random(sectors);
    }

    @Override
    public PlanarDice choosePDRollToIgnore(List<PlanarDice> rolls) {
        //TODO create AI logic for this
        return Aggregates.random(rolls);
    }

    @Override
    public boolean mulliganKeepHand(Player firstPlayer, int cardsToReturn)  {
        return !ComputerUtil.wantMulligan(player, cardsToReturn);
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
            int numLandsInHand = landsInHand.size() - CardLists.count(toReturn, Presets.LANDS);

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
        // TODO replace with EmptySa
        final Ability ability = new AbilityStatic(c, cost, null) { @Override public void resolve() {} };
        ability.setActivatingPlayer(c.getController(), true);
        ability.setCardState(sa.getCardState());

        if (ComputerUtil.playNoStack(c.getController(), ability, getGame(), true)) {
            // transfer this info for Balduvian Fallen
            sa.setPayingMana(ability.getPayingMana());
            return true;
        }
        return false;
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
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
        switch (kindOfChoice) {
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
                            if (!source.hasRemembered()) {
                                return true;
                            } else {
                                Card rem = (Card) source.getFirstRemembered();
                                if (!rem.isInPlay()) {
                                    return true;
                                }
                            }
                            break;
                        case "BetterTgtThanRemembered":
                            if (source.hasRemembered()) {
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
        // short cut if there is no options to choose
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseCounterType(options, sa, params);
    }

    @Override
    public String chooseKeywordForPump(final List<String> options, final SpellAbility sa, final String prompt, final Card tgtCard) {
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        List<String> possible = Lists.newArrayList();
        CardCollection oppUntappedCreatures = CardLists.filter(player.getOpponents().getCreaturesInPlay(), CardPredicates.Presets.UNTAPPED);
        if (tgtCard != null) {
            for (String kw : options) {
                if (tgtCard.hasKeyword(kw)) {
                    continue;
                } else if ("Indestructible".equals(kw)) {
                    if (oppUntappedCreatures.isEmpty()) {
                        continue; // no threats on battlefield - removal still a concern perhaps?
                    } else {
                        possible.clear();
                        possible.add(kw); // prefer Indestructible above all else
                        break;
                    }
                } else if ("Flying".equals(kw)) {
                    if (oppUntappedCreatures.isEmpty()) {
                        continue; // no need for evasion
                    } else {
                        boolean flyingGood = true;
                        for (Card c : oppUntappedCreatures) {
                            if (c.hasKeyword(Keyword.FLYING) || c.hasKeyword(Keyword.REACH)) {
                                flyingGood = false;
                                break;
                            }
                        }
                        if (flyingGood) {
                            possible.clear();
                            possible.add(kw); // flying is great when no one else has it
                            break;
                        } // even if opp has flying or reach, flying might still be useful so we won't skip it
                    }
                } else if (kw.startsWith("Protection from ")) {
                    //currently, keyword choice lists only include color protection
                    final String fromWhat = kw.substring(16);
                    boolean found = false;
                    for (String color : MagicColor.Constant.ONLY_COLORS) {
                        if (color.equalsIgnoreCase(fromWhat)) {
                            CardCollection known = player.getOpponents().getCardsIn(ZoneType.Battlefield);
                            for (final Card c : known) {
                                if (c.associatedWithColor(color)) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }
                possible.add(kw);
            }
        }
        if (!possible.isEmpty()) {
            return Aggregates.random(possible);
        } else {
            return Aggregates.random(options); // if worst comes to worst, at least do something
        }
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String prompt, SpellAbility sa) {
        return brains.confirmPayment(costPart); // AI is expected to know what it is paying for at the moment (otherwise add another parameter to this method)
    }

    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question) {
        return brains.aiShouldRun(replacementEffect, effectSA, affected);
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
                SpellAbility spell = si.getSpellAbility();
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
            CardCollection list = player.getOpponents().getCreaturesInPlay();
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
        // TODO replace with EmptySa
        final Ability emptyAbility = new AbilityStatic(source, cost, sa.getTargetRestrictions()) { @Override public void resolve() { } };
        emptyAbility.setActivatingPlayer(player, true);
        emptyAbility.setTriggeringObjects(sa.getTriggeringObjects());
        emptyAbility.setSVars(sa.getSVars());
        emptyAbility.setCardState(sa.getCardState());
        emptyAbility.setXManaCostPaid(sa.getRootAbility().getXManaCostPaid());

        if (ComputerUtilCost.willPayUnlessCost(sa, player, cost, alreadyPaid, allPayers)) {
            boolean result = ComputerUtil.playNoStack(player, emptyAbility, getGame(), true); // AI needs something to resolve to pay that cost
            if (!emptyAbility.getPaidHash().isEmpty()) {
                // report info to original sa (Argentum Masticore)
                sa.setPaidHash(emptyAbility.getPaidHash());
            }
            return result;
        }
        return false;
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        for (final SpellAbility sa : getAi().orderPlaySa(activePlayerSAs)) {
            if (sa.isTrigger() && !sa.isCopied()) {
                if (prepareSingleSa(sa.getHostCard(), sa, true)) {
                    ComputerUtil.playStack(sa, player, getGame());
                }
            } else {
                if (sa.isCopied()) {
                    if (sa.isSpell()) {
                        if (!sa.getHostCard().isInZone(ZoneType.Stack)) {
                            sa.setHostCard(getGame().getAction().moveToStack(sa.getHostCard(), sa));
                        } else {
                            getGame().getStackZone().add(sa.getHostCard());
                        }
                    }

                    if (sa.isMayChooseNewTargets()) {
                        TargetChoices tc = sa.getTargets();
                        if (!sa.setupTargets()) {
                            // if AI can't choose targets need to keep old one even if illegal
                            sa.setTargets(tc);
                        }
                        // FIXME: the new implementation (below) requires implementing setupNewTargets in the AI controller, among other possible changes, otherwise breaks AI
                        // sa.setupNewTargets(player);
                    }
                }
                // need finally add the new spell to the stack
                getGame().getStack().add(sa);
            }
        }
    }

    private boolean prepareSingleSa(final Card host, final SpellAbility sa, boolean isMandatory) {
        if (sa.getApi() == ApiType.Charm) {
            return CharmEffect.makeChoices(sa);
        }
        if (sa.hasParam("TargetingPlayer")) {
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(host, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            return targetingPlayer.getController().chooseTargetsFor(sa);
        } else {
            return brains.doTrigger(sa, isMandatory);
        }
    }

    @Override
    public boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        if (prepareSingleSa(host, wrapperAbility, isMandatory)) {
            return ComputerUtil.playNoStack(wrapperAbility.getActivatingPlayer(), wrapperAbility, getGame(), true);
        }
        return false;
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        boolean optional = tgtSA.hasParam("Optional");
        boolean noManaCost = tgtSA.hasParam("WithoutManaCost");
        if (tgtSA instanceof Spell) { // Isn't it ALWAYS a spell?
            Spell spell = (Spell) tgtSA;
            // TODO if mandatory AI is only forced to use mana when it's already in the pool
            if (brains.canPlayFromEffectAI(spell, !optional, noManaCost) == AiPlayDecision.WillPlay || !optional) {
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
    public TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional) {
        // AI currently can't do this. But when it can it will need to be based on Ability API
        return null;
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
    public void revealAISkipCards(String message, Map<Player, Map<DeckSection, List<? extends PaperCard>>> deckCards) {
        // Ai won't understand that anyway
    }

    @Override
    public Map<DeckSection, List<? extends PaperCard>> complainCardsCantPlayWell(Deck myDeck) {
        // TODO check if profile detection set to Auto
        setupAutoProfile(myDeck);

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
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt /* ai needs hints as well */, ManaConversionMatrix matrix, boolean effect) {
        return ComputerUtilMana.payManaCost(player, sa, effect);
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
            CardCollectionView oppLibrary = player.getStrongestOpponent().getCardsIn(ZoneType.Library);
            final Card source = sa.getHostCard();
            final String logic = sa.getParam("AILogic");

            // Filter for valid options only
            if (!valid.isEmpty()) {
                aiLibrary = CardLists.getValidCards(aiLibrary, valid, source.getController(), source, sa);
                oppLibrary = CardLists.getValidCards(oppLibrary, valid, source.getController(), source, sa);
            }

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

            String name = "";
            if (logic.equals("MostProminentInComputerDeck")) {
                name = ComputerUtilCard.getMostProminentCardName(aiLibrary);
            } else if (logic.equals("MostProminentInHumanDeck")) {
                name = ComputerUtilCard.getMostProminentCardName(oppLibrary);
            } else if (logic.equals("MostProminentCreatureInComputerDeck")) {
                CardCollectionView cards = CardLists.getValidCards(aiLibrary, "Creature", player, sa.getHostCard(), sa);
                name = ComputerUtilCard.getMostProminentCardName(cards);
            } else if (logic.equals("BestCreatureInComputerDeck")) {
                Card bestCreature = ComputerUtilCard.getBestCreatureAI(aiLibrary);
                name = bestCreature != null ? bestCreature.getName() : "";
            } else if (logic.equals("RandomInComputerDeck")) {
                name = aiLibrary.isEmpty() ? "" : Aggregates.random(aiLibrary).getName();
            } else if (logic.equals("MostProminentSpellInComputerDeck")) {
                CardCollectionView cards = CardLists.getValidCards(aiLibrary, "Card.Instant,Card.Sorcery", player, sa.getHostCard(), sa);
                name = ComputerUtilCard.getMostProminentCardName(cards);
            } else if (logic.equals("CursedScroll")) {
                name = SpecialCardAi.CursedScroll.chooseCard(player, sa);
            } else if (logic.equals("PithingNeedle")) {
                name = SpecialCardAi.PithingNeedle.chooseCard(player, sa);
            }

            if (!StringUtils.isBlank(name)) {
                return name;
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
    public List<OptionalCostValue> chooseOptionalCosts(SpellAbility chosen, List<OptionalCostValue> optionalCostValues) {
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
                copy.setCastSA(kickedSaCopy);
                if (ComputerUtilCard.checkNeedsToPlayReqs(copy, kickedSaCopy) != AiPlayDecision.WillPlay) {
                    continue; // don't choose kickers we don't want to play
                }
            }

            if (ComputerUtilCost.canPayCost(fullCostSa, player, false)) {
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

        if (keyword.getKeyword() == Keyword.CASUALTY
                && "true".equalsIgnoreCase(sa.getHostCard().getSVar("AINoCasualtyPayment"))) {
            // TODO: Grisly Sigil - currently will be misplayed if Casualty is paid (the cost is always paid, targeting is wrong).
            return 0;
        }

        int chosenAmount = 0;

        Cost costSoFar = sa.getPayCosts().copy();

        for (int i = 0; i < max; i++) {
            costSoFar.add(cost);
            SpellAbility fullCostSa = sa.copyWithDefinedCost(costSoFar);
            if (ComputerUtilCost.canPayCost(fullCostSa, player, sa.isTrigger())) {
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
