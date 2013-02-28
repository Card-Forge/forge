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
package forge.game.ai;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.GameEntity;
import forge.CardPredicates.Presets;
import forge.Constant;
import forge.Singletons;

import forge.card.ability.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.CostDiscard;
import forge.card.cost.CostPart;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.game.GameActionUtil;
import forge.game.GameState;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Expressions;

/**
 * <p>
 * ComputerAI_General class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AiController {

    private final AIPlayer player;
    private final GameState game;
    public GameState getGame()
    {
        return game;
    }

    public AIPlayer getPlayer()
    {
        return player;
    }

    /**
     * <p>
     * Constructor for ComputerAI_General.
     * </p>
     */
    public AiController(final AIPlayer computerPlayer, final GameState game0) {
        player = computerPlayer;
        game = game0;
    }

    public final SpellAbility getSpellAbilityToPlay() {
        // if top of stack is owned by me
        if (!game.getStack().isEmpty() && game.getStack().peekInstance().getActivatingPlayer().equals(player)) {
            // probably should let my stuff resolve
            return null;
        }
        final List<Card> cards = getAvailableCards();
    
        if ( !game.getStack().isEmpty() ) {
            SpellAbility counter = chooseCounterSpell(getPlayableCounters(cards));
            if( counter != null ) return counter;
    
            SpellAbility counterETB = chooseSpellAbilities(this.getPossibleETBCounters(), false);
            if( counterETB != null )
                return counterETB;
        }
    
        return chooseSpellAbilities(getSpellAbilities(cards), true);
    }

    /**
     * <p>
     * getAvailableSpellAbilities.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private List<Card> getAvailableCards() {
        List<Card> all = new ArrayList<Card>(player.getCardsIn(ZoneType.Hand));

        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        all.addAll(player.getCardsIn(ZoneType.Command));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            all.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        for(Player p : game.getPlayers()) {
            all.addAll(p.getCardsIn(ZoneType.Exile));
            all.addAll(p.getCardsIn(ZoneType.Battlefield));
        }
        return all;
    }

    /**
     * <p>
     * getPossibleETBCounters.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPossibleETBCounters() {

        final Player opp = player.getOpponent();
        List<Card> all = new ArrayList<Card>(player.getCardsIn(ZoneType.Hand));
        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            all.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        all.addAll(opp.getCardsIn(ZoneType.Exile));

        final ArrayList<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : all) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                if (sa instanceof SpellPermanent) {
                    // TODO ArsenalNut (13 Oct 2012) added line to set activating player to fix NPE problem
                    // in checkETBEffects.  There is SpellPermanent.checkETBEffects where the player can be
                    // directly input but it is currently a private method.
                    sa.setActivatingPlayer(player);
                    if (SpellPermanent.checkETBEffects(c, sa, ApiType.Counter)) {
                        spellAbilities.add(sa);
                    }
                }
            }
        }
        return spellAbilities;
    }

    private List<SpellAbility> getOriginalAndAltCostAbilities(final List<SpellAbility> possibleCounters)
    {
        final ArrayList<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : possibleCounters) {
            sa.setActivatingPlayer(player);
            //add alternative costs as additional spell abilities
            newAbilities.add(sa);
            newAbilities.addAll(GameActionUtil.getAlternativeCosts(sa));
        }
    
        final List<SpellAbility> result = new ArrayList<SpellAbility>();
        for (SpellAbility sa : newAbilities) {
            sa.setActivatingPlayer(player);
            result.addAll(GameActionUtil.getOptionalAdditionalCosts(sa));
        }
        result.addAll(newAbilities);
        return result;
    }

    /**
     * Returns the spellAbilities from the card list.
     * 
     * @param l
     *            a {@link forge.CardList} object.
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    private ArrayList<SpellAbility> getSpellAbilities(final List<Card> l) {
        final ArrayList<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                spellAbilities.add(sa);
            }
        }
        return spellAbilities;
    }

    /**
     * <p>
     * getPlayableCounters.
     * </p>
     * 
     * @param l
     *            a {@link forge.CardList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPlayableCounters(final List<Card> l) {
        final ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                // Check if this AF is a Counterpsell
                if (sa.getApi() == ApiType.Counter) {
                    spellAbility.add(sa);
                }
            }
        }

        return spellAbility;
    }

    // plays a land if one is available
    /**
     * <p>
     * chooseLandsToPlay.
     * </p>
     * 
     * @return a boolean.
     */
    public List<Card> getLandsToPlay() {
    
        final List<Card> hand = player.getCardsIn(ZoneType.Hand);
        List<Card> landList = CardLists.filter(hand, Presets.LANDS);
        List<Card> nonLandList = CardLists.filter(hand, Predicates.not(CardPredicates.Presets.LANDS));
        
        //filter out cards that can't be played
        landList = CardLists.filter(landList, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return player.canPlayLand(c);
            }
        });
    
        final List<Card> landsNotInHand = new ArrayList<Card>(player.getCardsIn(ZoneType.Graveyard));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            landsNotInHand.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        for (final Card crd : landsNotInHand) {
            if (crd.isLand() && crd.hasKeyword("May be played")) {
                landList.add(crd);
            }
        }
        if (landList.isEmpty()) {
            return null;
        }
        if (landList.size() == 1 && nonLandList.size() < 3) {
            List<Card> cardsInPlay = player.getCardsIn(ZoneType.Battlefield);
            List<Card> landsInPlay = CardLists.filter(cardsInPlay, Presets.LANDS);
            List<Card> allCards = new ArrayList<Card>(player.getCardsIn(ZoneType.Graveyard));
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
                if (c.getSVar("NeedsToPlay").length() > 0) {
                    final String needsToPlay = c.getSVar("NeedsToPlay");
                    List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
    
                    list = CardLists.getValidCards(list, needsToPlay.split(","), c.getController(), c);
                    if (list.isEmpty()) {
                        return false;
                    }
                }
                if (c.getSVar("NeedsToPlayVar").length() > 0) {
                    final String needsToPlay = c.getSVar("NeedsToPlayVar");
                    int x = 0;
                    int y = 0;
                    String sVar = needsToPlay.split(" ")[0];
                    String comparator = needsToPlay.split(" ")[1];
                    String compareTo = comparator.substring(2);
                    try {
                        x = Integer.parseInt(sVar);
                    } catch (final NumberFormatException e) {
                        x = CardFactoryUtil.xCount(c, c.getSVar(sVar));
                    }
                    try {
                        y = Integer.parseInt(compareTo);
                    } catch (final NumberFormatException e) {
                        y = CardFactoryUtil.xCount(c, c.getSVar(compareTo));
                    }
                    if (!Expressions.compare(x, comparator, y)) {
                        return false;
                    }
                }
                if (c.isType("Legendary") && !c.getName().equals("Flagstones of Trokair")) {
                    final List<Card> list = player.getCardsIn(ZoneType.Battlefield);
                    if (Iterables.any(list, CardPredicates.nameEquals(c.getName()))) {
                        return false;
                    }
                }
    
                // don't play the land if it has cycling and enough lands are
                // available
                final ArrayList<SpellAbility> spellAbilities = c.getSpellAbilities();
    
                final List<Card> hand = player.getCardsIn(ZoneType.Hand);
                List<Card> lands = player.getCardsIn(ZoneType.Battlefield);
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

    public Card chooseBestLandToPlay(List<Card> landList)
    {
        if (landList.isEmpty())
            return null;
    
        // play as many lands as you can
        int ix = 0;
        while (landList.get(ix).isReflectedLand() && ((ix + 1) < landList.size())) {
            // Skip through reflected lands. Choose last if they are all
            // reflected.
            ix++;
        }
    
        Card land = landList.get(ix);
        //play basic lands that are needed the most
        if (Iterables.any(landList, CardPredicates.Presets.BASIC_LANDS)) {
            final List<Card> combined = player.getCardsIn(ZoneType.Battlefield);
    
            final ArrayList<String> basics = new ArrayList<String>();
    
            // what types can I go get?
            for (final String name : Constant.CardTypes.BASIC_TYPES) {
                if (Iterables.any(landList, CardPredicates.isType(name))) {
                    basics.add(name);
                }
            }
    
            // Which basic land is least available from hand and play, that I still
            // have in my deck
            int minSize = Integer.MAX_VALUE;
            String minType = null;
    
            for (int i = 0; i < basics.size(); i++) {
                final String b = basics.get(i);
                final int num = CardLists.getType(combined, b).size();
                if (num < minSize) {
                    minType = b;
                    minSize = num;
                }
            }
    
            if (minType != null) {
                landList = CardLists.getType(landList, minType);
            }
    
            land = landList.get(0);
        }
        return land;
    }

    // if return true, go to next phase
    /**
     * <p>
     * playCounterSpell.
     * </p>
     * 
     * @param possibleCounters
     *            a {@link java.util.ArrayList} object.
     * @return a boolean.
     */
    private SpellAbility chooseCounterSpell(final List<SpellAbility> possibleCounters) {
        if ( possibleCounters == null || possibleCounters.isEmpty())
            return null;;
        
        SpellAbility bestSA = null;
        int bestRestriction = Integer.MIN_VALUE;


        for (final SpellAbility sa : getOriginalAndAltCostAbilities(possibleCounters)) {
            SpellAbility currentSA = sa;
            sa.setActivatingPlayer(player);
            // check everything necessary
            if (canPlayAndPayFor(currentSA)) {
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
    } // playCounterSpell()

    // if return true, go to next phase
    /**
     * <p>
     * playSpellAbilities.
     * </p>
     * 
     * @param all
     *            an array of {@link forge.card.spellability.SpellAbility}
     *            objects.
     * @return a boolean.
     */
    private SpellAbility chooseSpellAbilities(final List<SpellAbility> all, boolean skipCounter) {
        if ( all == null || all.isEmpty() )
            return null;

        Collections.sort(all, saComparator); // put best spells first
        
        for (final SpellAbility sa : getOriginalAndAltCostAbilities(all)) {
            // Don't add Counterspells to the "normal" playcard lookups
            if (sa.getApi() == ApiType.Counter && skipCounter) {
                continue;
            }
            sa.setActivatingPlayer(player);
            
            if (!canPlayAndPayFor(sa))
                continue;
    
            return sa;
        }
        
        return null;
    } // playCards()


    // This is for playing spells regularly (no Cascade/Ripple etc.)
    private boolean canPlayAndPayFor(final SpellAbility sa) {
        return sa.canPlay() && sa.canPlayAI() && ComputerUtilCost.canPayCost(sa, player);
    }
    
    // not sure "playing biggest spell" matters?
     private final static Comparator<SpellAbility> saComparator = new Comparator<SpellAbility>() {
        @Override
        public int compare(final SpellAbility a, final SpellAbility b) {
            // sort from highest cost to lowest
            // we want the highest costs first
            int a1 = a.getManaCost().getCMC();
            int b1 = b.getManaCost().getCMC();

            // cast 0 mana cost spells first (might be a Mox)
            if (a1 == 0) {
                b1 = -2;
            } else if (b1 == 0) {
                a1 = -2;
            }

            a1 += getSpellAbilityPriority(a);
            b1 += getSpellAbilityPriority(b);

            return b1 - a1;
        }
        
        private int getSpellAbilityPriority(SpellAbility sa) {
            int p = 0;
            // puts creatures in front of spells
            if (sa.getSourceCard().isCreature()) {
                p += 1;
            }
            // sort planeswalker abilities for ultimate
            if (sa.getRestrictions().getPlaneswalker()) {
                if (sa.hasParam("Ultimate")) {
                    p += 9;
                }
            }
    
            if (ApiType.DestroyAll == sa.getApi()) {
                p += 4;
            }
    
            return p;
        }
    }; // Comparator
    
    /**
     * <p>
     * AI_discardNumType.
     * </p>
     * 
     * @param numDiscard
     *            a int.
     * @param uTypes
     *            an array of {@link java.lang.String} objects. May be null for
     *            no restrictions.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a List<Card> of discarded cards.
     */
    public List<Card> getCardsToDiscard(final int numDiscard, final String[] uTypes, final SpellAbility sa) {
        List<Card> hand = new ArrayList<Card>(player.getCardsIn(ZoneType.Hand));
        Card sourceCard = null;
    
        if ((uTypes != null) && (sa != null)) {
            hand = CardLists.getValidCards(hand, uTypes, sa.getActivatingPlayer(), sa.getSourceCard());
        }
    
        if (hand.size() < numDiscard) {
            return null;
        }
    
        final List<Card> discardList = new ArrayList<Card>();
        int count = 0;
        if (sa != null) {
            sourceCard = sa.getSourceCard();
        }
    
        // look for good discards
        while (count < numDiscard) {
            Card prefCard = null;
            if (sa != null && sa.getActivatingPlayer() != null && sa.getActivatingPlayer().isOpponentOf(player)) {
                for (Card c : hand) {
                    if (c.hasKeyword("If a spell or ability an opponent controls causes you to discard CARDNAME,"
                            + " put it onto the battlefield instead of putting it into your graveyard.")) {
                        prefCard = c;
                        break;
                    }
                }
            }
            if (prefCard == null) {
                prefCard = ComputerUtil.getCardPreference(player, sourceCard, "DiscardCost", hand);
            }
            if (prefCard != null) {
                discardList.add(prefCard);
                hand.remove(prefCard);
                count++;
            } else {
                break;
            }
        }
    
        final int discardsLeft = numDiscard - count;
    
        // choose rest
        for (int i = 0; i < discardsLeft; i++) {
            if (hand.isEmpty()) {
                continue;
            }
            final int numLandsInPlay = Iterables.size(Iterables.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.LANDS));
            final List<Card> landsInHand = CardLists.filter(hand, CardPredicates.Presets.LANDS);
            final int numLandsInHand = landsInHand.size();
    
            // Discard a land
            boolean canDiscardLands = numLandsInHand > 3  || (numLandsInHand > 2 && numLandsInPlay > 0)
            || (numLandsInHand > 1 && numLandsInPlay > 2) || (numLandsInHand > 0 && numLandsInPlay > 5);
    
            if (canDiscardLands) {
                discardList.add(landsInHand.get(0));
                hand.remove(landsInHand.get(0));
            } else { // Discard other stuff
                CardLists.sortByCmcDesc(hand);
                int numLandsAvailable = numLandsInPlay;
                if (numLandsInHand > 0) {
                    numLandsAvailable++;
                }
                //Discard unplayable card
                if (hand.get(0).getCMC() > numLandsAvailable) {
                    discardList.add(hand.get(0));
                    hand.remove(hand.get(0));
                } else { //Discard worst card
                    Card worst = ComputerUtilCard.getWorstAI(hand);
                    discardList.add(worst);
                    hand.remove(worst);
                }
            }
        }
    
        return discardList;
    }

    // These methods might be moved into matching SpellAbilityAi classes just without all these switches here
    public Card chooseSingleCardForEffect(List<Card> options, SpellAbility sa, String title, boolean isOptional) {
        ApiType api = sa.getApi();
        if ( null == api ) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }

        Card choice = null;
        Card host = sa.getSourceCard();
        String logic = sa.getParam("AILogic");

        switch(api) {
            case Bond: 
                return ComputerUtilCard.getBestCreatureAI(options);
                
            case ChooseCard:
                if (logic == null) {
                    // Base Logic is choose "best"
                    choice = ComputerUtilCard.getBestAI(options);
                } else if ("WorstCard".equals(logic)) {
                    choice = ComputerUtilCard.getWorstAI(options);
                } else if (logic.equals("BestBlocker")) {
                    if (!CardLists.filter(options, Presets.UNTAPPED).isEmpty()) {
                        options = CardLists.filter(options, Presets.UNTAPPED);
                    }
                    choice = ComputerUtilCard.getBestCreatureAI(options);
                } else if (logic.equals("Clone")) {
                    if (!CardLists.getValidCards(options, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host).isEmpty()) {
                        options = CardLists.getValidCards(options, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host);
                    }
                    choice = ComputerUtilCard.getBestAI(options);
                } else if (logic.equals("Untap")) {
                    if (!CardLists.getValidCards(options, "Permanent.YouCtrl,Permanent.tapped", host.getController(), host).isEmpty()) {
                        options = CardLists.getValidCards(options, "Permanent.YouCtrl,Permanent.tapped", host.getController(), host);
                    }
                    choice = ComputerUtilCard.getBestAI(options);
                }
                return choice;
            
            case Encode:
                if (logic == null) {
                    // Base Logic is choose "best"
                    choice = ComputerUtilCard.getBestAI(options);
                } else if ("WorstCard".equals(logic)) {
                    choice = ComputerUtilCard.getWorstAI(options);
                } else if (logic.equals("BestBlocker")) {
                    if (!CardLists.filter(options, Presets.UNTAPPED).isEmpty()) {
                        options = CardLists.filter(options, Presets.UNTAPPED);
                    }
                    choice = ComputerUtilCard.getBestCreatureAI(options);
                } else if (logic.equals("Clone")) {
                    if (!CardLists.getValidCards(options, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host).isEmpty()) {
                        options = CardLists.getValidCards(options, "Permanent.YouDontCtrl,Permanent.nonLegendary", host.getController(), host);
                    }
                    choice = ComputerUtilCard.getBestAI(options);
                }
                return choice;
                
            default: throw new InvalidParameterException("AI chooseSingleCard does not know how to choose card for " + api);
        }
    }


    public boolean confirmAction(SpellAbility sa, String mode, String message) {
        ApiType api = sa.getApi();
        if ( null == api ) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        
        switch(api) {
            case Discard:
                if ( mode.startsWith("Random") ) { //
                    // TODO For now AI will always discard Random used currently with: Balduvian Horde and similar cards
                    return true;
                }
            break;
            
            case Encode:
                return true;
                
            default: 
        }
        String exMsg = String.format("AI confirmAction does not know what to decide about %s with %s mode.", api, mode);
        throw new InvalidParameterException(exMsg);
    }

    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        if (logic.equalsIgnoreCase("ProtectFriendly")) {
            final Player controller = hostCard.getController();
            if (affected instanceof Player) {
                return !((Player) affected).isOpponentOf(controller);
            } else if (affected instanceof Card) {
                return !((Card) affected).getController().isOpponentOf(controller);
            }
        }
        return true;
    }
}

