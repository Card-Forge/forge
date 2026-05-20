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


/**
 * <p>
 * QuestDuel class.
 * </p>
 * MODEL - A single duel event data instance, including meta and deck.
 * 
 */
public class QuestEventDuel extends QuestEvent {

    /**
     * Instantiates a new quest duel.
     */
    public QuestEventDuel() {
        super();
    }

    public QuestEventDuel getRandomOpponent(QuestEventDifficulty difficulty) {
        QuestEventDuel duel = new QuestEventDuel();
        duel.setTitle("Random Opponent");
        duel.setIconImageKey(getIconImageKey());
        duel.setOpponentName(getTitle());
        duel.setDifficulty(difficulty);
        duel.setProfile(getProfile());
        duel.setShowDifficulty(false);
        duel.setDescription("Fight a random opponent");
        duel.setEventDeck(getEventDeck());
        return duel;
    }
}
