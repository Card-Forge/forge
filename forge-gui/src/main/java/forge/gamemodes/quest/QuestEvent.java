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

import com.google.common.base.Function;

import forge.deck.Deck;
import forge.item.InventoryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * QuestEvent.
 * </p>
 *
 * MODEL - A basic event instance in Quest mode. Can be extended for use in
 * unique event types: battles, quests, and others.
 */
public abstract class QuestEvent implements IQuestEvent {
    // Default vals if none provided in the event file.
    protected Deck eventDeck = null;
    private String title = "Mystery Event";
    private String description = "";
    private QuestEventDifficulty difficulty = QuestEventDifficulty.MEDIUM;
    private boolean showDifficulty = true;
    private String imageKey = "";
    private String name = "Noname";
    private String cardReward = null;
    private List<InventoryItem> cardRewardList = null;
    private String profile = "Default";
    // Opponent name if different from the challenge name
    private String opponentName = null;
    private boolean isRandomMatch = false;


    public static final Function<QuestEvent, String> FN_GET_NAME = new Function<QuestEvent, String>() {
        @Override public final String apply(QuestEvent qe) { return qe.name; }
    };

    public final String getTitle() {
        return title;
    }

    /**
     * Returns null for standard quest events, may return something different for challenges.
     */
    public String getOpponentName() {
        return opponentName;
    }

    /**
     * Sets the opponent's name.
     *
     * @param newName
     *            the name to set
     */
    public void setOpponentName(final String newName) {
        this.opponentName = newName;
    }

    public final QuestEventDifficulty getDifficulty() {
        return difficulty;
    }

    public final String getDescription() {
        return description;
    }

    public Deck getEventDeck() {
        return eventDeck;
    }

    @Override
    public final String getIconImageKey() {
        return imageKey;
    }

    public final String getName() {
        return name;
    }

    public final String getProfile() {
        return profile;
    }

    public void setProfile(final String profile0) {
        profile = profile0;
    }

    public void setName(final String name0) {
        name = name0;
    }

    public void setTitle(final String title0) {
        title = title0;
    }

    public void setDifficulty(final QuestEventDifficulty difficulty0) {
        difficulty = difficulty0;
    }

    public void setDescription(final String description0) {
        description = description0;
    }

    public void setEventDeck(final Deck eventDeck0) {
        eventDeck = eventDeck0;
    }

    @Override
    public void setIconImageKey(final String s0) {
        imageKey = s0;
    }

    public final List<InventoryItem> getCardRewardList() {
        if (cardReward == null) {
            return null;
        }
        if (cardRewardList == null) {
            cardRewardList = new ArrayList<>(BoosterUtils.generateCardRewardList(cardReward));
        }
        return cardRewardList;
    }

    public void setCardReward(final String cardReward0) {
        cardReward = cardReward0;
    }

    public List<String> getHumanExtraCards() {
        return Collections.emptyList();
    }

    public List<String> getAiExtraCards() {
        return Collections.emptyList();
    }

    @Override
    public final String getFullTitle() {
        return title + (showDifficulty ? " (" + difficulty.getTitle() + ")" : "");
    }

    @Override
    public void select() {
        QuestUtil.setEvent(this);
    }

    @Override
    public boolean hasImage() {
        return true;
    }

    public boolean showDifficulty() {
        return showDifficulty;
    }

    public void setShowDifficulty(final boolean showDifficulty) {
        this.showDifficulty = showDifficulty;
    }

    public boolean getIsRandomMatch(){return isRandomMatch;}

    public void setIsRandomMatch(boolean b){isRandomMatch = b;}
}
