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
package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;





/**
 * A collection of methods containing full
 * meta and gameplay properties of a card.
 * 
 * @author Forge
 * @version $Id: CardRules.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardRules {

    private final CardRuleCharacteristics characteristics;

    private int iPower = -1;
    private int iToughness = -1;
    private String power = null;
    private String toughness = null;

    private String loyalty = null;

    //Vanguard avatar modifiers
    private Integer life = null;
    private Integer hand = null;

    Map<String, CardInSet> setsPrinted = null;

    boolean isRemovedFromAIDecks = false;
    boolean isRemovedFromRandomDecks = false;

    private final CardRules slavePart;

    private final boolean hasOtherFace;

    private List<String> originalScript;

    // Ctor and builders are needed here
    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.characteristics.getCardName();
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public CardType getType() {
        return this.characteristics.getCardType();
    }

    /**
     * Gets the mana cost.
     * 
     * @return the mana cost
     */
    public CardManaCost getManaCost() {
        return this.characteristics.getManaCost();
    }

    /**
     * Gets the color.
     * 
     * @return the color
     */
    public CardColor getColor() {
        return this.characteristics.getColor();
    }

    /**
     * Gets the rules.
     * 
     * @return the rules
     */
    public String[] getRules() {
        return this.characteristics.getCardRules();
    }

    /**
     * 
     * Gets Slave Part.
     * 
     * @return CardRules
     */
    public CardRules getSlavePart() {
        return this.slavePart;
    }

    /**
     * Gets the sets printed.
     * 
     * @return the sets printed
     */
    public Set<Entry<String, CardInSet>> getSetsPrinted() {
        return this.characteristics.getSetsData().entrySet();
    }

    /**
     * Gets the power.
     * 
     * @return the power
     */
    public String getPower() {
        return this.power;
    }

    /**
     * Gets the int power.
     * 
     * @return the int power
     */
    public int getIntPower() {
        return this.iPower;
    }

    /**
     * Gets the toughness.
     * 
     * @return the toughness
     */
    public String getToughness() {
        return this.toughness;
    }

    /**
     * Gets the int toughness.
     * 
     * @return the int toughness
     */
    public int getIntToughness() {
        return this.iToughness;
    }

    /**
     * Gets the loyalty.
     * 
     * @return the loyalty
     */
    public String getLoyalty() {
        return this.loyalty;
    }

    /**
     * Gets the rem ai decks.
     * 
     * @return the rem ai decks
     */
    public boolean getRemAIDecks() {
        return this.isRemovedFromAIDecks;
    }

    /**
     * Gets the rem random decks.
     * 
     * @return the rem random decks
     */
    public boolean getRemRandomDecks() {
        return this.isRemovedFromRandomDecks;
    }

    /**
     * Gets the p tor loyalty.
     * 
     * @return the p tor loyalty
     */
    public String getPTorLoyalty() {
        if (this.getType().isCreature()) {
            return this.power + "/" + this.toughness;
        }
        if (this.getType().isPlaneswalker()) {
            return this.loyalty;
        }
        return "";
    }

    /**
     * Checks if is alt state.
     * 
     * @return true, if is alt state
     */
    public boolean isAltState() {
        return this.isDoubleFaced() && (this.slavePart == null);
    }

    /**
     * Checks if is double faced.
     * 
     * @return true, if is double faced
     */
    public boolean isDoubleFaced() {
        return this.hasOtherFace;
    }

    /**
     * Instantiates a new card rules.
     * 
     * @param chars
     *            the chars
     * @param isDoubleFacedCard
     *            the is double faced card
     * @param otherPart
     *            the otherPart
     * @param removedFromRandomDecks
     *            the removed from random decks
     * @param removedFromAIDecks
     *            the removed from ai decks
     */
    public CardRules(final CardRuleCharacteristics chars, List<String> forgeScript, final boolean isDoubleFacedCard,
            final CardRules otherPart, final boolean removedFromRandomDecks, final boolean removedFromAIDecks) {
        this.characteristics = chars;
        this.slavePart = otherPart;
        this.hasOtherFace = isDoubleFacedCard;
        this.isRemovedFromAIDecks = removedFromAIDecks;
        this.isRemovedFromRandomDecks = removedFromRandomDecks;
        this.originalScript = forgeScript == null ? null : new ArrayList<String>(forgeScript);
        // System.out.println(cardName);

        if (this.getType().isCreature()) {
            final int slashPos = this.characteristics.getPtLine() == null ? -1 : this.characteristics.getPtLine()
                    .indexOf('/');
            if (slashPos == -1) {
                throw new RuntimeException(String.format("Creature '%s' has bad p/t stats", this.getName()));
            }
            this.power = this.characteristics.getPtLine().substring(0, slashPos);
            this.toughness = this.characteristics.getPtLine().substring(slashPos + 1,
                    this.characteristics.getPtLine().length());
            this.iPower = StringUtils.isNumeric(this.power) ? Integer.parseInt(this.power) : 0;
            this.iToughness = StringUtils.isNumeric(this.toughness) ? Integer.parseInt(this.toughness) : 0;
        } else if (this.getType().isPlaneswalker()) {
            this.loyalty = this.characteristics.getPtLine();
        } else if (this.getType().isVanguard()) {
            String pt = this.characteristics.getPtLine();
            final int slashPos = this.characteristics.getPtLine() == null ? -1 : this.characteristics.getPtLine()
                    .indexOf('/');
            if (slashPos == -1) {
                throw new RuntimeException(String.format("Vanguard '%s' has bad hand/life stats", this.getName()));
            }
            this.hand = Integer.parseInt(pt.substring(0, pt.indexOf('/')).replace("+", ""));
            this.life = Integer.parseInt(pt.substring(pt.indexOf('/') + 1).replace("+", ""));
        }

        if (this.characteristics.getSetsData().isEmpty()) {
            this.characteristics.getSetsData().put("???", new CardInSet(CardRarity.Unknown, 1));
        }
        this.setsPrinted = this.characteristics.getSetsData();
    }

    /**
     * Rules contain.
     * 
     * @param text
     *            the text
     * @return true, if successful
     */
    public boolean rulesContain(final String text) {
        if (this.characteristics.getCardRules() == null) {
            return false;
        }
        for (final String r : this.characteristics.getCardRules()) {
            if (StringUtils.containsIgnoreCase(r, text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the latest set printed.
     * 
     * @return the latest set printed
     */
    public String getLatestSetPrinted() {
        String lastSet = null;
        // TODO: Make a true release-date based sorting
        for (final String cs : this.setsPrinted.keySet()) {
            lastSet = cs;
        }
        return lastSet;
    }

    /**
     * Gets the sets the info.
     * 
     * @param setCode
     *            the set code
     * @return the sets the info
     */
    public CardInSet getEditionInfo(final String setCode) {
        final CardInSet result = this.setsPrinted.get(setCode);
        if (result != null) {
            return result;
        }
        throw new RuntimeException(String.format("Card '%s' was never printed in set '%s'", this.getName(), setCode));

    }

    /**
     * Gets the rarity from latest set.
     * 
     * @return the rarity from latest set
     */
    public CardRarity getRarityFromLatestSet() {
        final CardInSet cis = this.setsPrinted.get(this.getLatestSetPrinted());
        return cis.getRarity();
    }

    /**
     * Gets the ai status.
     * 
     * @return the ai status
     */
    public String getAiStatus() {
        return this.isRemovedFromAIDecks ? (this.isRemovedFromRandomDecks ? "AI ?" : "AI")
                : (this.isRemovedFromRandomDecks ? "?" : "");
    }

    /**
     * Gets the ai status comparable.
     * 
     * @return the ai status comparable
     */
    public Integer getAiStatusComparable() {
        if (this.isRemovedFromAIDecks && this.isRemovedFromRandomDecks) {
            return Integer.valueOf(3);
        } else if (this.isRemovedFromAIDecks) {
            return Integer.valueOf(4);
        } else if (this.isRemovedFromRandomDecks) {
            return Integer.valueOf(2);
        } else {
            return Integer.valueOf(1);
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public Iterable<String> getCardScript() {
        return originalScript;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public String getPictureUrl() {
        return characteristics.getDlUrl();
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckHints() {
        return characteristics.getDeckHints();
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckNeeds() {
        return characteristics.getDeckNeeds();
    }

    /**
     * @return the keywords
     */
    public List<String> getKeywords() {
        return characteristics.getKeywords();
    }

    /**
     * @return the hand
     */
    public Integer getHand() {
        return hand;
    }

    /**
     * @return the life
     */
    public Integer getLife() {
        return life;
    }


}
