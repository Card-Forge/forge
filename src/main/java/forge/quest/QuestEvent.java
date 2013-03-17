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
package forge.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.deck.Deck;
import forge.game.player.IHasIcon;
import forge.item.InventoryItem;

/**
 * <p>
 * QuestEvent.
 * </p>
 * 
 * MODEL - A basic event instance in Quest mode. Can be extended for use in
 * unique event types: battles, quests, and others.
 */
public abstract class QuestEvent implements IHasIcon {
    // Default vals if none provided in the event file.
    private Deck eventDeck = null;
    private String title = "Mystery Event";
    private String description = "";
    private String difficulty = "Medium";
    private String imageKey = "";
    private String name = "Noname";
    private String cardReward = null;
    private List<InventoryItem> cardRewardList = null;

    public final String getTitle() {
        return this.title;
    }

    /**
     * Returns null for standard quest events, may return something different for challenges.
     */
    public String getOpponent() {
        return null;
    }

    public final String getDifficulty() {
        return this.difficulty;
    }

    public final String getDescription() {
        return this.description;
    }

    public final Deck getEventDeck() {
        return this.eventDeck;
    }

    @Override
    public final String getIconImageKey() {
        return this.imageKey;
    }

    public final String getName() {
        return this.name;
    }

    public void setName(final String name0) {
        this.name = name0;
    }

    public void setTitle(final String title0) {
        this.title = title0;
    }

    public void setDifficulty(final String difficulty0) {
        this.difficulty = difficulty0;
    }

    public void setDescription(final String description0) {
        this.description = description0;
    }

    public void setEventDeck(final Deck eventDeck0) {
        this.eventDeck = eventDeck0;
    }

    @Override
    public void setIconImageKey(final String s0) {
        this.imageKey = s0;
    }

    public final List<InventoryItem> getCardRewardList() {
        if (cardReward == null) {
            return null;
        }
        if (cardRewardList == null) {
            this.cardRewardList = new ArrayList<InventoryItem>(BoosterUtils.generateCardRewardList(cardReward));
        }
        return this.cardRewardList;
    }

    public void setCardReward(final String cardReward0) {
        this.cardReward = cardReward0;
    }

    public List<String> getHumanExtraCards() {
        return Collections.emptyList();
    }

    public List<String> getAiExtraCards() {
        return Collections.emptyList();
    }
}
