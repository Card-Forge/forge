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
package forge.gamemodes.quest;

import java.util.ArrayList;
import java.util.List;

import forge.deck.Deck;

/**
 * <p>
 * QuestQuest class.
 * </p>
 *
 * MODEL - A single quest event data instance, including meta, deck, and
 * quest-specific properties.
 *
 */
public class QuestEventChallenge extends QuestEvent {

    // ID (default -1, should be explicitly set at later time.)
    /** The id. */
    private String id = "-1";

    // Default vals if none provided for this ID
    /** The ai life. */
    private int aiLife = 25;

    private Integer humanLife = null;

    /** The credits reward. */
    private int creditsReward = 100;

    /** The repeatable. */
    private boolean repeatable = false;

    private boolean useBazaar = true;
    private Boolean forceAnte = null;

    /** The wins reqd. */
    private int winsReqd = 20;

    // Other cards used in assignment: starting, and reward.
    /** The human extra cards. */
    private List<String> humanExtraCards = new ArrayList<>();

    /** The ai extra cards. */
    private List<String> aiExtraCards = new ArrayList<>();

    /** If persistent, the challenge does not disappear if the player fails it. */
    private boolean persistent = false;

    /** a text message that appears when the player wins the challenge match. */
    private String winMessage = "";

    private Deck humanDeck = null;

    /**
     * Instantiates a new quest challenge.
     */
    public QuestEventChallenge() {
        super();
        setCardReward("1 colorless rare"); // Guaranteed extra reward for challenges if not specified
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
    public final String getId() {
        return this.id;
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
     * getHumanExtraCards.
     * </p>
     * Retrieves list of cards human has in play at the beginning of this quest.
     *
     * @return the human extra cards
     */
    @Override
    public final List<String> getHumanExtraCards() {
        return this.humanExtraCards;
    }

    /**
     * Gets the ai extra cards.
     *
     * @return the aiExtraCards
     */
    @Override
    public List<String> getAiExtraCards() {
        return this.aiExtraCards;
    }

    /**
     * Sets the ai extra cards.
     *
     * @param aiExtraCards0
     *            the aiExtraCards to set
     */
    public void setAiExtraCards(final List<String> aiExtraCards0) {
        this.aiExtraCards = aiExtraCards0;
    }

    /**
     * Sets the id.
     *
     * @param id0
     *            the id to set
     */
    public void setId(final String id0) {
        this.id = id0;
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
     * @param repeatable0
     *            the repeatable to set
     */
    public void setRepeatable(final boolean repeatable0) {
        this.repeatable = repeatable0;
    }

    /**
     * Checks if is persistent.
     *
     * @return the persistent
     */
    public boolean isPersistent() {
        return this.persistent;
    }

    /**
     * Sets the persistent.
     *
     * @param persistent0
     *            the repeatable to set
     */
    public void setPersistent(final boolean persistent0) {
        this.persistent = persistent0;
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
     * @param aiLife0
     *            the aiLife to set
     */
    public void setAiLife(final int aiLife0) {
        this.aiLife = aiLife0;
    }

    /**
     * Sets the wins reqd.
     *
     * @param winsReqd0
     *            the winsReqd to set
     */
    public void setWinsReqd(final int winsReqd0) {
        this.winsReqd = winsReqd0;
    }

    /**
     * Sets the credits reward.
     *
     * @param creditsReward0
     *            the creditsReward to set
     */
    public void setCreditsReward(final int creditsReward0) {
        this.creditsReward = creditsReward0;
    }

    /**
     * Sets the human extra cards.
     *
     * @param humanExtraCards0
     *            the humanExtraCards to set
     */
    public void setHumanExtraCards(final List<String> humanExtraCards0) {
        this.humanExtraCards = humanExtraCards0;
    }

    /**
     * @return the humanLife
     */
    public Integer getHumanLife() {
        return humanLife;
    }

    /**
     * @param humanLife the humanLife to set
     */
    public void setHumanLife(Integer humanLife) {
        this.humanLife = humanLife;
    }

    /**
     * @return the useBazaar
     */
    public boolean isUseBazaar() {
        return useBazaar;
    }

    /**
     * @param useBazaar the useBazaar to set
     */
    public void setUseBazaar(boolean useBazaar) {
        this.useBazaar = useBazaar;
    }

    /**
     * @return the forceAnte
     */
    public Boolean isForceAnte() {
        return forceAnte;
    }

    /**
     * @param forceAnte the forceAnte to set
     */
    public void setForceAnte(Boolean forceAnte) {
        this.forceAnte = forceAnte;
    }

    /**
     * Gets the win message.
     *
     * @return the winMessage
     */
    public String getWinMessage() {
        return this.winMessage;
    }

    /**
     * Sets the win message
     *
     * @param winMessage0
     *            the winMessage to set
     */
    public void setWinMessage(final String winMessage0) {
        this.winMessage = winMessage0;
    }

    /**
     * @return the humanDeck
     */
    public Deck getHumanDeck() {
        return humanDeck;
    }

    /**
     * @param humanDeck the humanDeck to set
     */
    public void setHumanDeck(Deck humanDeck) {
        this.humanDeck = humanDeck;
    }
}
