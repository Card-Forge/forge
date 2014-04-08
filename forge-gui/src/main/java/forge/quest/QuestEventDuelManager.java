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

import forge.model.FModel;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.DifficultyPrefs;
import forge.quest.io.QuestDuelReader;
import forge.util.CollectionSuppliers;
import forge.util.maps.EnumMapOfLists;
import forge.util.maps.MapOfLists;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;

import java.io.File;
import java.util.*;

/**
 * QuestEventManager.
 * 
 * @author Forge
 * @version $Id: QuestEventManager.java 20404 2013-03-17 05:34:13Z myk $
 */
public class QuestEventDuelManager {

    private final MapOfLists<QuestEventDifficulty, QuestEventDuel> sortedDuels = new EnumMapOfLists<QuestEventDifficulty, QuestEventDuel>(QuestEventDifficulty.class, CollectionSuppliers.<QuestEventDuel>arrayLists());
    private final IStorage<QuestEventDuel> allDuels;


    /** Instantiate all events and difficulty lists.
     * @param dir &emsp; File object */
    public QuestEventDuelManager(final File dir) {
        allDuels = new StorageBase<QuestEventDuel>("Quest duels", new QuestDuelReader(dir));
        assembleDuelDifficultyLists();
    } // End assembleAllEvents()

    /** @return List<QuestEventDuel> */
    public Iterable<QuestEventDuel> getAllDuels() {
        return allDuels;
    }

    // define fallback orders if there aren't enough opponents defined for a particular difficultly level
    private static List<QuestEventDifficulty> _easyOrder = Arrays.asList(QuestEventDifficulty.EASY, QuestEventDifficulty.MEDIUM, QuestEventDifficulty.HARD, QuestEventDifficulty.EXPERT);
    private static List<QuestEventDifficulty> _mediumOrder = Arrays.asList(QuestEventDifficulty.MEDIUM, QuestEventDifficulty.HARD, QuestEventDifficulty.EASY, QuestEventDifficulty.EXPERT);
    private static List<QuestEventDifficulty> _hardOrder = Arrays.asList(QuestEventDifficulty.HARD, QuestEventDifficulty.MEDIUM, QuestEventDifficulty.EASY, QuestEventDifficulty.EXPERT);
    private static List<QuestEventDifficulty> _expertOrder = Arrays.asList(QuestEventDifficulty.EXPERT, QuestEventDifficulty.HARD, QuestEventDifficulty.MEDIUM, QuestEventDifficulty.EASY);
    
    private void _addDuel(List<QuestEventDuel> outList, QuestEventDifficulty targetDifficulty, int toAdd) {
        // if there's no way we can satisfy the request, return now
        if (allDuels.size() <= toAdd) {
            return;
        }
        
        final List<QuestEventDifficulty> difficultyOrder;
        switch (targetDifficulty) {
        case EASY:   difficultyOrder = _easyOrder;   break;
        case MEDIUM: difficultyOrder = _mediumOrder; break;
        case HARD:   difficultyOrder = _hardOrder;   break;
        case EXPERT: difficultyOrder = _expertOrder; break;
        default:
            throw new RuntimeException("unhandled difficulty: " + targetDifficulty);
        }
        
        for (QuestEventDifficulty d : difficultyOrder) { // will add duels from preferred difficulty, will use others if the former has too few options. 
            for( QuestEventDuel duel : sortedDuels.get(d)) {
                if(toAdd <= 0)
                    return;

                if (!outList.contains(duel)) { 
                    outList.add(duel);
                    toAdd--;
                }
            }
        }
    }
    
    /** Generates an array of new duel opponents based on current win conditions.
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public final List<QuestEventDuel> generateDuels() {
        final QuestPreferences qpref = FModel.getQuestPreferences();
        if (FModel.getQuest().getAchievements() == null) {
            return null;
        }

        final QuestController qCtrl = FModel.getQuest();
        final int cntWins = qCtrl.getAchievements().getWin();

        final int index = qCtrl.getAchievements().getDifficulty();
        final List<QuestEventDuel> duelOpponents = new ArrayList<QuestEventDuel>();

        if (cntWins < qpref.getPrefInt(DifficultyPrefs.WINS_MEDIUMAI, index)) {
            _addDuel(duelOpponents, QuestEventDifficulty.EASY, 3);
        } else if (cntWins == qpref.getPrefInt(DifficultyPrefs.WINS_MEDIUMAI, index)) {
            _addDuel(duelOpponents, QuestEventDifficulty.EASY, 1);
            _addDuel(duelOpponents, QuestEventDifficulty.MEDIUM, 2);
        } else if (cntWins < qpref.getPrefInt(DifficultyPrefs.WINS_HARDAI, index)) {
            _addDuel(duelOpponents, QuestEventDifficulty.MEDIUM, 3);
        } else if (cntWins == qpref.getPrefInt(DifficultyPrefs.WINS_HARDAI, index)) {
            _addDuel(duelOpponents, QuestEventDifficulty.MEDIUM, 1);
            _addDuel(duelOpponents, QuestEventDifficulty.HARD, 2);
        } else if (cntWins < qpref.getPrefInt(DifficultyPrefs.WINS_EXPERTAI, index)) {
            _addDuel(duelOpponents, QuestEventDifficulty.HARD, 3);
        } else {
            _addDuel(duelOpponents, QuestEventDifficulty.HARD, 2);
            _addDuel(duelOpponents, QuestEventDifficulty.EXPERT, 1);
        }

        return duelOpponents;
    }

    /**
     * <p>
     * assembleDuelDifficultyLists.
     * </p>
     * Assemble duel deck difficulty lists
     */
    private void assembleDuelDifficultyLists() {
        sortedDuels.clear();
        sortedDuels.put(QuestEventDifficulty.EASY, new ArrayList<QuestEventDuel>());
        sortedDuels.put(QuestEventDifficulty.MEDIUM, new ArrayList<QuestEventDuel>());
        sortedDuels.put(QuestEventDifficulty.HARD, new ArrayList<QuestEventDuel>());
        sortedDuels.put(QuestEventDifficulty.EXPERT, new ArrayList<QuestEventDuel>());



        for (final QuestEventDuel qd : allDuels) {
            sortedDuels.add(qd.getDifficulty(), qd);
        }
    }

    /** */
    public void randomizeOpponents() {
        final long seed = new Random().nextLong();
        final Random r = new Random(seed);
        for(QuestEventDifficulty qd : sortedDuels.keySet()) {
            List<QuestEventDuel> list = (List<QuestEventDuel>) sortedDuels.get(qd); 
            Collections.shuffle(list, r);
        }
    }

}
