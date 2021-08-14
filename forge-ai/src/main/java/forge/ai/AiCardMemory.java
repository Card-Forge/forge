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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import forge.game.card.Card;
import forge.game.player.Player;

/**
 * <p>
 * AiCardMemory class.
 * </p>
 * 
 * A simple class that allows the AI to "memorize" different cards on the battlefield (and possibly in other zones
 * too, for instance as revealed from the opponent's hand) and assign them to different memory sets in order to help
 * make somewhat more "educated" decisions to attack with certain cards or play certain spell abilities. Each 
 * AiController has its own memory that is created when the AI player is spawned. The card memory is accessible 
 * via AiController.getCardMemory. 
 * 
 * @author Forge
 */
public class AiCardMemory {

    /**
     * Defines the memory set in which the card is remembered
     * (which, in its turn, defines how the AI utilizes the information
     * about remembered cards).
     */
    public enum MemorySet {
        MANDATORY_ATTACKERS, // These creatures must attack this turn
        TRICK_ATTACKERS, // These creatures will attack to try to provoke the opponent to block them into a combat trick
        HELD_MANA_SOURCES_FOR_MAIN2, // These mana sources will not be used before Main 2
        HELD_MANA_SOURCES_FOR_DECLBLK, // These mana sources will not be used before Combat - Declare Blockers
        HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK, // These mana sources will not be used before the opponent's Combat - Declare Blockers
        HELD_MANA_SOURCES_FOR_NEXT_SPELL, // These mana sources will not be used until the next time the AI chooses a spell to cast
        ATTACHED_THIS_TURN, // These equipments were attached to something already this turn
        ANIMATED_THIS_TURN, // These cards had their AF Animate effect activated this turn
        BOUNCED_THIS_TURN, // These cards were bounced this turn
        ACTIVATED_THIS_TURN, // These cards had their ability activated this turn
        CHOSEN_FOG_EFFECT, // These cards are marked as the Fog-like effect the AI is planning to cast this turn
        MARKED_TO_AVOID_REENTRY, // These cards may cause a stack smash when processed recursively, and are thus marked to avoid a crash
        PAYS_TAP_COST, // These cards will be tapped as part of a cost and cannot be chosen in another part
        PAYS_SAC_COST // These cards will be sacrificed as part of a cost and cannot be chosen in another part
        //REVEALED_CARDS // stub, not linked to AI code yet
    }

    private final Set<Card> memMandatoryAttackers;
    private final Set<Card> memTrickAttackers;
    private final Set<Card> memHeldManaSources;
    private final Set<Card> memHeldManaSourcesForCombat;
    private final Set<Card> memHeldManaSourcesForEnemyCombat;
    private final Set<Card> memHeldManaSourcesForNextSpell;
    private final Set<Card> memAttachedThisTurn;
    private final Set<Card> memAnimatedThisTurn;
    private final Set<Card> memBouncedThisTurn;
    private final Set<Card> memActivatedThisTurn;
    private final Set<Card> memChosenFogEffect;
    private final Set<Card> memMarkedToAvoidReentry;
    private final Set<Card> memPaysTapCost;
    private final Set<Card> memPaysSacCost;

    public AiCardMemory() {
        this.memMandatoryAttackers = new HashSet<>();
        this.memHeldManaSources = new HashSet<>();
        this.memHeldManaSourcesForCombat = new HashSet<>();
        this.memHeldManaSourcesForEnemyCombat = new HashSet<>();
        this.memAttachedThisTurn = new HashSet<>();
        this.memAnimatedThisTurn = new HashSet<>();
        this.memBouncedThisTurn = new HashSet<>();
        this.memActivatedThisTurn = new HashSet<>();
        this.memTrickAttackers = new HashSet<>();
        this.memChosenFogEffect = new HashSet<>();
        this.memMarkedToAvoidReentry = new HashSet<>();
        this.memHeldManaSourcesForNextSpell = new HashSet<>();
        this.memPaysTapCost = new HashSet<>();
        this.memPaysSacCost = new HashSet<>();
    }

    private Set<Card> getMemorySet(MemorySet set) {
        switch (set) {
            case MANDATORY_ATTACKERS:
                return memMandatoryAttackers;
            case TRICK_ATTACKERS:
                return memTrickAttackers;
            case HELD_MANA_SOURCES_FOR_MAIN2:
                return memHeldManaSources;
            case HELD_MANA_SOURCES_FOR_DECLBLK:
                return memHeldManaSourcesForCombat;
            case HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK:
                return memHeldManaSourcesForEnemyCombat;
            case HELD_MANA_SOURCES_FOR_NEXT_SPELL:
                return memHeldManaSourcesForNextSpell;
            case ATTACHED_THIS_TURN:
                return memAttachedThisTurn;
            case ANIMATED_THIS_TURN:
                return memAnimatedThisTurn;
            case BOUNCED_THIS_TURN:
                return memBouncedThisTurn;
            case ACTIVATED_THIS_TURN:
                return memActivatedThisTurn;
            case CHOSEN_FOG_EFFECT:
                return memChosenFogEffect;
            case MARKED_TO_AVOID_REENTRY:
                return memMarkedToAvoidReentry;
            case PAYS_TAP_COST:
                return memPaysTapCost;
            case PAYS_SAC_COST:
                return memPaysSacCost;
            //case REVEALED_CARDS:
            //    return memRevealedCards;
            default:
                return null;
        }
    }

    /**
     * Checks if the given card was remembered in the given memory set. 
     * 
     * @param c
     *            the card
     * @param set the memory set that is to be checked 
     * @return true, if the card is remembered in the given memory set
     */
    public boolean isRememberedCard(Card c, MemorySet set) {
        if (c == null) {
            return false;
        }

        Set<Card> memorySet = getMemorySet(set);

        return memorySet != null && memorySet.contains(c);
    }

    /**
     * Checks if at least one card of the given name was remembered in the given memory set.
     * 
     * @param cardName
     *            the card name
     * @param set the memory set that is to be checked 
     * @return true, if at least one card with the given name is remembered in the given memory set
     */
    public boolean isRememberedCardByName(String cardName, MemorySet set) {
        Set<Card> memorySet = getMemorySet(set);

        if (memorySet != null) {
            Iterator<Card> it = memorySet.iterator();

            while (it.hasNext()) {
                Card c = it.next();
                if (c.getName().equals(cardName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if at least one card of the given name was remembered in the given memory set such
     * that its owner is the given player.
     * 
     * @param cardName
     *            the card name
     * @param set the memory set that is to be checked 
     * @param owner the owner of the card
     * @return true, if at least one card with the given name is remembered in the given memory set
     */
    public boolean isRememberedCardByName(String cardName, MemorySet set, Player owner) {
        Set<Card> memorySet = getMemorySet(set);

        if (memorySet != null) {
            Iterator<Card> it = memorySet.iterator();

            while (it.hasNext()) {
                Card c = it.next();
                if (c.getName().equals(cardName) && c.getOwner().equals(owner)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Remembers the given card in the given memory set.
     * @param c
     *            the card
     * @param set the memory set to remember the card in
     * @return true, if the card is successfully stored in the given memory set 
     */
    public boolean rememberCard(Card c, MemorySet set) {
        if (c == null)
            return false;

        Set<Card> memorySet = getMemorySet(set);

        if (memorySet != null) {
            memorySet.add(c);
        }

        return true;
    }

    /**
     * Forgets the given card in the given memory set.
     * @param c
     *            the card
     * @param set the memory set to forget the card in
     * @return true, if the card was previously remembered in the given memory set and was successfully forgotten
     */
    public boolean forgetCard(Card c, MemorySet set) {
        if (c == null) {
            return false;
        }
        if (!isRememberedCard(c, set)) {
            return false;
        }

        Set<Card> memorySet = getMemorySet(set);

        if (memorySet != null) {
            memorySet.remove(c);
        }

        return true;
    }

    /**
     * Forgets a single card with the given name in the given memory set.
     * @param cardName
     *            the card name
     * @param set the memory set to forget the card in
     * @return true, if at least one card with the given name was previously remembered in the given memory set and was successfully forgotten
     */
    public boolean forgetAnyCardWithName(String cardName, MemorySet set) {
        Set<Card> memorySet = getMemorySet(set);

        if (memorySet != null) {
            Iterator<Card> it = memorySet.iterator();

            while (it.hasNext()) {
                Card c = it.next();
                if (c.getName().equals(cardName)) {
                    return forgetCard(c, set);
                }
            }
        }
        
        return false;
    }

    /**
     * Forgets a single card with the given name owned by the given player in the given memory set.
     * @param cardName
     *            the card name
     * @param set the memory set to forget the card in
     * @param owner the owner of the card
     * @return true, if at least one card with the given name was previously remembered in the given memory set and was successfully forgotten
     */
    public boolean forgetAnyCardWithName(String cardName, MemorySet set, Player owner) {
        Set<Card> memorySet = getMemorySet(set);

        if (memorySet != null) {
            Iterator<Card> it = memorySet.iterator();

            while (it.hasNext()) {
                Card c = it.next();
                if (c.getName().equals(cardName) && c.getOwner().equals(owner)) {
                    return forgetCard(c, set);
                }
            }
        }

        return false;
    }

    /**
     * Determines if the memory set is empty.
     * @param set the memory set to inspect.
     * @return true, if the given memory set contains no remembered cards.
     */
    public boolean isMemorySetEmpty(MemorySet set) {
        return set == null || getMemorySet(set).isEmpty();
    }
    
    /**
     * Clears the given memory set.
     */
    public void clearMemorySet(MemorySet set) {
        if (set != null) {
            getMemorySet(set).clear();
        }
    }

    /**
     * Clears all memory sets stored in this card memory for the given player.
     */
    public void clearAllRemembered() {
        for (MemorySet memSet : MemorySet.values()) {
            clearMemorySet(memSet);
        }
    }

    // Static functions to simplify access to AI card memory of a given AI player.
    public static Set<Card> getMemorySet(Player ai, MemorySet set) {
        if (!ai.getController().isAI()) {
            return null;
        }
        return ((PlayerControllerAi)ai.getController()).getAi().getCardMemory().getMemorySet(set);
    }
    public static void rememberCard(Player ai, Card c, MemorySet set) {
        if (!ai.getController().isAI()) {
            return;
        }
        ((PlayerControllerAi)ai.getController()).getAi().getCardMemory().rememberCard(c, set);
    }
    public static void rememberCard(AiController aic, Card c, MemorySet set) {
        aic.getCardMemory().rememberCard(c, set);
    }
    public static void forgetCard(Player ai, Card c, MemorySet set) {
        if (!ai.getController().isAI()) {
            return;
        }
        ((PlayerControllerAi)ai.getController()).getAi().getCardMemory().forgetCard(c, set);
    }
    public static void forgetCard(AiController aic, Card c, MemorySet set) {
        aic.getCardMemory().forgetCard(c, set);
    }
    public static boolean isRememberedCard(Player ai, Card c, MemorySet set) {
        if (!ai.getController().isAI()) {
            return false;
        }
        return ((PlayerControllerAi)ai.getController()).getAi().getCardMemory().isRememberedCard(c, set);
    }
    public static boolean isRememberedCard(AiController aic, Card c, MemorySet set) {
        return aic.getCardMemory().isRememberedCard(c, set);
    }
    public static boolean isRememberedCardByName(Player ai, String name, MemorySet set) {
        if (!ai.getController().isAI()) {
            return false;
        }
        return ((PlayerControllerAi)ai.getController()).getAi().getCardMemory().isRememberedCardByName(name, set);
    }
    public static boolean isRememberedCardByName(AiController aic, String name, MemorySet set) {
        return aic.getCardMemory().isRememberedCardByName(name, set);
    }
    public static void clearMemorySet(Player ai, MemorySet set) {
        if (!ai.getController().isAI()) {
            return;
        }
        ((PlayerControllerAi)ai.getController()).getAi().getCardMemory().clearMemorySet(set);
    }
    public static void clearMemorySet(AiController aic, MemorySet set) {
        if (!isMemorySetEmpty(aic, set)) {
            aic.getCardMemory().clearMemorySet(set);
        }
    }
    public static boolean isMemorySetEmpty(Player ai, MemorySet set) {
        if (!ai.getController().isAI()) {
            return false;
        }
        return ((PlayerControllerAi)ai.getController()).getAi().getCardMemory().isMemorySetEmpty(set);
    }
    public static boolean isMemorySetEmpty(AiController aic, MemorySet set) {
        return aic.getCardMemory().isMemorySetEmpty(set);
    }
}