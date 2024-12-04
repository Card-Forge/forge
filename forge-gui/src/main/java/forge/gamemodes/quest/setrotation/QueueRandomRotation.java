package forge.gamemodes.quest.setrotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import forge.model.FModel;

import static java.lang.Integer.min;

/**
 * A quest world rotation style where N random concurrent sets are available at any given time.
 * S of these sets are rotated out after each set of W number of wins.
 */
public class QueueRandomRotation implements ISetRotation {

    private int concurrentSets;
    private int rotateAfterWins;
    private int setsPerRotation;

    @Override
    public List<String> getCurrentSetCodes(List<String> allSets) {
        if(FModel.getQuest() == null)
            return allSets;

        // Each unique quest (based on name) gets its own unique set order
        int seed = FModel.getQuest().getName().hashCode();
        Random rnd = new Random(seed);
        List<String> shuffledSets = new ArrayList<>(allSets);
        Collections.shuffle(shuffledSets, rnd);

        List<String> currentCodes = new ArrayList<>();
        int outRotations = FModel.getQuest().getAchievements().getWin() / rotateAfterWins;
        int outRotated = outRotations * setsPerRotation;
        int setsToAdd = min(concurrentSets, shuffledSets.size());
        for (int i = 0; i < setsToAdd; i++) {
            int setToAdd = (i + outRotated) % shuffledSets.size();
            currentCodes.add(shuffledSets.get(setToAdd));
        }
        return currentCodes;
    }

    public QueueRandomRotation(int concurrentSets, int rotateAfterWins, int setsPerRotation){
        this.concurrentSets = concurrentSets;
        this.rotateAfterWins = rotateAfterWins;
        this.setsPerRotation = setsPerRotation;
    }
}
