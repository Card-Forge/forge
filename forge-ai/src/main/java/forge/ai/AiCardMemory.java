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

import forge.game.card.Card;
import forge.game.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>
 * AiCardMemory class.
 * </p>
 * 
 * A simple class that allows the AI to "memorize" different cards on the battlefield (and possibly in other zones
 * too, for instance as revealed from the opponent's hand) and assign them to different memory sets in order to help
 * make somewhat more "educated" decisions to attack with certain cards or play certain spell abilities. Each 
 * AiController has its own memory that is created when the AI player is spawned. The card memory is accessible 
 * via AiController.getCardMemory. AiCardMemory can memorize cards belonging to different players. For example, 
 * it's possible to distinctly store cards revealed by different players in a single AiCardMemory.
 * Methods without the Player parameter operate on the "default" memory sets that belong directly to the owner
 * of the card memory (the AI player itself).
 * 
 * @author Forge
 */
public class AiCardMemory {

    private HashMap<Player, HashSet<Card>> mapMandatoryAttackers = new HashMap<Player, HashSet<Card>>();
    private HashMap<Player, HashSet<Card>> mapHeldManaSources = new HashMap<Player, HashSet<Card>>();
    private HashMap<Player, HashSet<Card>> mapRevealedCards = new HashMap<Player, HashSet<Card>>();
    private Player self = null;

    /**
     * Defines the memory set in which the card is remembered
     * (which, in its turn, defines how the AI utilizes the information
     * about remembered cards).
     */
    public enum MemorySet {
        MANDATORY_ATTACKERS,
        HELD_MANA_SOURCES, // stub, not linked to AI code yet
        REVEALED_CARDS; // stub, not linked to AI code yet
    }

    public AiCardMemory(Player self) {
        this.self = self;
    }

    private Set<Card> getMemorySet(Player p, MemorySet set) {
        if (p == null) {
            return null;
        }

        switch (set) {
            case MANDATORY_ATTACKERS:
                if (!mapMandatoryAttackers.containsKey(p)) {
                    mapMandatoryAttackers.put(p, new HashSet<Card>());
                }
                return mapMandatoryAttackers.get(p);
            case HELD_MANA_SOURCES:
                if (!mapHeldManaSources.containsKey(p)) {
                    mapHeldManaSources.put(p, new HashSet<Card>());
                }
                return mapHeldManaSources.get(p);
            case REVEALED_CARDS:
                if (!mapRevealedCards.containsKey(p)) {
                    mapRevealedCards.put(p, new HashSet<Card>());
                }
                return mapRevealedCards.get(p);
            default:
                return null;
        }
    }

    /**
     * Checks if the given card was remembered in a certain memory set that stores cards
     * memorized from a particular player.
     * 
     * @param p
     *            player that was the controller of the card at the time the card was remembered
     * @param c
     *            the card
     * @param set the memory set that is to be checked 
     * @return true, if the card is remembered in the given memory set
     */
    public boolean isRememberedCard(Player p, Card c, MemorySet set) {
        if (c == null || p == null) {
            return false;
        }

        Set<Card> memorySet = getMemorySet(p, set);

        return memorySet == null ? false : memorySet.contains(c);
    }
    
    /**
     * Checks if the given card was remembered in a certain memory set for the default player (owner of this card memory).
     * 
     * @param c
     *            the card
     * @param set the memory set that is to be checked 
     * @return true, if the card is remembered in the given memory set
     */
    public boolean isRememberedCard(Card c, MemorySet set) {
        return isRememberedCard(self, c, set);
    }

    /**
     * Checks if at least one card of the given name was remembered in a certain memory set
     * that stores cards memorized from a particular player.
     * 
     * @param p
     *            player that was the controller of the card at the time the card was remembered
     * @param cardName
     *            the card name
     * @param set the memory set that is to be checked 
     * @return true, if at least one card with the given name is remembered in the given memory set
     */
    public boolean isRememberedCardByName(Player p, String cardName, MemorySet set) {
        if (p == null) {
            return false;
        }

        Set<Card> memorySet = getMemorySet(p, set);
        Iterator<Card> it = memorySet.iterator();

        while (it.hasNext()) {
            Card c = it.next();
            if (c.getName().equals(cardName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if at least one card of the given name was remembered in a certain memory set for the default player (owner of this card memory).
     * 
     * @param cardName
     *            the card name
     * @param set the memory set that is to be checked 
     * @return true, if at least one card with the given name is remembered in the given memory set
     */
    public boolean isRememberedCardByName(String cardName, MemorySet set) {
        return isRememberedCardByName(self, cardName, set);
    }

    /**
     * Remembers the given card in the given memory set that stores cards memorized from a particular player.
     * @param p
     *            player that is the controller of the card at the time the card is remembered
     * @param c
     *            the card
     * @param set the memory set to remember the card in
     * @return true, if the card is successfully stored in the given memory set 
     */
    public boolean rememberCard(Player p, Card c, MemorySet set) {
        if (c == null || p == null)
            return false;

        //System.out.println ("AiCardMemory: remembering card " + c.getName() + "(ID=" + c.getUniqueNumber() + ")" + " for player " + p.getName() + ", set = " + set.name() + ".");

        getMemorySet(p, set).add(c);
        return true;
    }

    /**
     * Remembers the given card in the given memory set for the default player (owner of this card memory).
     * @param c
     *            the card
     * @param set the memory set to remember the card in
     * @return true, if the card is successfully stored in the given memory set 
     */
    public boolean rememberCard(Card c, MemorySet set) {
        return rememberCard(self, c, set);
    }

    /**
     * Forgets the given card in the given memory set that stores cards memorized from a particular player.
     * @param p
     *            player that is the controller of the card at the time the card is remembered
     * @param c
     *            the card
     * @param set the memory set to forget the card in
     * @return true, if the card was previously remembered in the given memory set and was successfully forgotten
     */
    public boolean forgetCard(Player p, Card c, MemorySet set) {
        if (c == null || p == null) {
            return false;
        }
        if (!isRememberedCard(p, c, set)) {
            return false;
        }

        //System.out.println ("AiCardMemory: forgetting card " + c.getName() + "(ID=" + c.getUniqueNumber() + ")" + " for player " + p.getName() + ", set = " + set.name() + ".");

        getMemorySet(p, set).remove(c);
        return true;
    }

    /**
     * Forgets the given card in the given memory set for the default player (owner of this card memory).
     * @param c
     *            the card
     * @param set the memory set to forget the card in
     * @return true, if the card was previously remembered in the given memory set and was successfully forgotten
     */
    public boolean forgetCard(Card c, MemorySet set) {
        return forgetCard(self, c, set);
    }

    /**
     * Forgets a single card with the given name in the given memory set that stores cards memorized from a particular player.
     * @param p
     *            player that is the controller of the card at the time the card is remembered
     * @param cardName
     *            the card name
     * @param set the memory set to forget the card in
     * @return true, if at least one card with the given name was previously remembered in the given memory set and was successfully forgotten
     */
    public boolean forgetAnyCardWithName(Player p, String cardName, MemorySet set) {
        if (p == null) {
            return false;
        }

        Set<Card> memorySet = getMemorySet(p, set);
        Iterator<Card> it = memorySet.iterator();

        while (it.hasNext()) { 
            Card c = it.next();
            if (c.getName().equals(cardName)) {
                return forgetCard(p, c, set);
            }
        }
        
        return false;
    }

    /**
     * Forgets a single card with the given name in the given memory set for the default player (owner of this card memory).
     * 
     * @param cardName
     *            the card name
     * @param set the memory set to forget the card in
     * @return true, if at least one card with the given name was previously remembered in the given memory set and was successfully forgotten
     */
    public boolean forgetAnyCardWithName(String cardName, MemorySet set) {
        return forgetAnyCardWithName(self, cardName, set);
    }

    /**
     * Clears the "remembered attackers" memory set stored in this card memory for the given player.
     * @param p
     *            player for whom the remembered attackers are to be cleared
     */
    public void clearRememberedAttackers(Player p) {
        getMemorySet(p, MemorySet.MANDATORY_ATTACKERS).clear();
    }

    /**
     * Clears the "remembered attackers" memory set for the default player (owner of this card memory).
     */
    public void clearRememberedAttackers() {
        clearRememberedAttackers(self);
    }

    /**
     * Clears the "remembered mana sources" memory set stored in this card memory for the given player.
     * @param p
     *            player for whom the remembered attackers are to be cleared
     */
    public void clearRememberedManaSources(Player p) {
        getMemorySet(p, MemorySet.HELD_MANA_SOURCES).clear();
    }

    /**
     * Clears the "remembered mana sources" memory set for the default player (owner of this card memory).
     */
    public void clearRememberedManaSources() {
        clearRememberedManaSources(self);
    }

    /**
     * Clears the "remembered revealed cards" memory set stored in this card memory for the given player.
     * @param p
     *            player for whom the remembered attackers are to be cleared
     */
    public void clearRememberedRevealedCards(Player p) {
        getMemorySet(p, MemorySet.REVEALED_CARDS).clear();
    }

    /**
     * Clears the "remembered revealed cards" memory set for the default player (owner of this card memory).
     */
    public void clearRememberedRevealedCards() {
        clearRememberedRevealedCards(self);
    }

    /**
     * Clears all memory sets stored in this card memory for the given player.
     * @param p
     *            player for whom the remembered attackers are to be cleared
     */
    public void clearAllRemembered(Player p) {
        clearRememberedAttackers(p);
        clearRememberedManaSources(p);
        clearRememberedRevealedCards(p);
    }

    /**
     * Clears all memory sets stored for the default player (owner of this card memory).
     */
    public void clearAllRemembered() {
        clearAllRemembered(self);
    }

    /**
     * Clears all memory sets stored for all players in this card memory.
     */
    public void wipeMemory() {
        mapMandatoryAttackers.clear();
        mapHeldManaSources.clear();
        mapRevealedCards.clear();
    }
}