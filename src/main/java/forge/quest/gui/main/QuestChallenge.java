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
package forge.quest.gui.main;

import java.util.ArrayList;
import java.util.List;

import forge.item.CardPrinted;

/**
 * <p>
 * QuestQuest class.
 * </p>
 * 
 * MODEL - A single quest event data instance, including meta, deck, and
 * quest-specific properties.
 * 
 */
public class QuestChallenge extends QuestEvent {
    // ID (default -1, should be explicitly set at later time.)
    /** The id. */
    private int id = -1;

    // Default vals if none provided for this ID
    /** The ai life. */
    private int aiLife = 25;

    /** The credits reward. */
    private int creditsReward = 100;

    /** The card reward. */
    private String cardReward = "1 colorless rare";

    /** The repeatable. */
    private boolean repeatable = false;

    /** The wins reqd. */
    private int winsReqd = 20;

    // Other cards used in assignment: starting, and reward.
    /** The human extra cards. */
    private List<String> humanExtraCards = new ArrayList<String>();

    /** The ai extra cards. */
    private List<String> aiExtraCards = new ArrayList<String>();

    /** The card reward list. */
    private List<CardPrinted> cardRewardList = new ArrayList<CardPrinted>();

    /**
     * Instantiates a new quest challenge.
     */
    public QuestChallenge() {
        super();
        this.setEventType("challenge");
    }

    /**
     * <p>
     * getAILife.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getAILife() {
        return this.getAiLife();
    }

    /**
     * <p>
     * getCardReward.
     * </p>
     * 
     * @return {@link java.lang.String}.
     */
    public final String getCardReward() {
        return this.cardReward;
    }

    /**
     * <p>
     * getCreditsReward.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getCreditsReward() {
        return this.creditsReward;
    }

    /**
     * <p>
     * getId.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getId() {
        return this.id;
    }

    /**
     * <p>
     * getRepeatable.
     * </p>
     * 
     * @return {@link java.lang.Boolean}.
     */
    public final boolean getRepeatable() {
        return this.isRepeatable();
    }

    /**
     * <p>
     * getWinsReqd.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getWinsReqd() {
        return this.winsReqd;
    }

    /**
     * <p>
     * getAIExtraCards.
     * </p>
     * Retrieves list of cards AI has in play at the beginning of this quest.
     * 
     * @return the aI extra cards
     */
    public final List<String> getAIExtraCards() {
        return this.getAiExtraCards();
    }

    /**
     * <p>
     * getHumanExtraCards.
     * </p>
     * Retrieves list of cards human has in play at the beginning of this quest.
     * 
     * @return the human extra cards
     */
    public final List<String> getHumanExtraCards() {
        return this.humanExtraCards;
    }

    /**
     * <p>
     * getCardRewardList.
     * </p>
     * 
     * @return the card reward list
     */
    public final List<CardPrinted> getCardRewardList() {
        return this.cardRewardList;
    }

    /**
     * Gets the ai extra cards.
     * 
     * @return the aiExtraCards
     */
    public List<String> getAiExtraCards() {
        return this.aiExtraCards;
    }

    /**
     * Sets the ai extra cards.
     * 
     * @param aiExtraCards
     *            the aiExtraCards to set
     */
    public void setAiExtraCards(final List<String> aiExtraCards) {
        this.aiExtraCards = aiExtraCards; // TODO: Add 0 to parameter's name.
    }

    /**
     * Sets the id.
     * 
     * @param id
     *            the id to set
     */
    public void setId(final int id) {
        this.id = id; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is repeatable.
     * 
     * @return the repeatable
     */
    public boolean isRepeatable() {
        return this.repeatable;
    }

    /**
     * Sets the repeatable.
     * 
     * @param repeatable
     *            the repeatable to set
     */
    public void setRepeatable(final boolean repeatable) {
        this.repeatable = repeatable; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the ai life.
     * 
     * @return the aiLife
     */
    public int getAiLife() {
        return this.aiLife;
    }

    /**
     * Sets the ai life.
     * 
     * @param aiLife
     *            the aiLife to set
     */
    public void setAiLife(final int aiLife) {
        this.aiLife = aiLife; // TODO: Add 0 to parameter's name.
    }

    /**
     * Sets the wins reqd.
     * 
     * @param winsReqd
     *            the winsReqd to set
     */
    public void setWinsReqd(final int winsReqd) {
        this.winsReqd = winsReqd; // TODO: Add 0 to parameter's name.
    }

    /**
     * Sets the credits reward.
     * 
     * @param creditsReward
     *            the creditsReward to set
     */
    public void setCreditsReward(final int creditsReward) {
        this.creditsReward = creditsReward; // TODO: Add 0 to parameter's name.
    }

    /**
     * Sets the card reward.
     * 
     * @param cardReward
     *            the cardReward to set
     */
    public void setCardReward(final String cardReward) {
        this.cardReward = cardReward; // TODO: Add 0 to parameter's name.
    }

    /**
     * Sets the card reward list.
     * 
     * @param cardRewardList
     *            the cardRewardList to set
     */
    public void setCardRewardList(final List<CardPrinted> cardRewardList) {
        this.cardRewardList = cardRewardList; // TODO: Add 0 to parameter's
                                              // name.
    }

    /**
     * Sets the human extra cards.
     * 
     * @param humanExtraCards
     *            the humanExtraCards to set
     */
    public void setHumanExtraCards(final List<String> humanExtraCards) {
        this.humanExtraCards = humanExtraCards; // TODO: Add 0 to parameter's
                                                // name.
    }
}
