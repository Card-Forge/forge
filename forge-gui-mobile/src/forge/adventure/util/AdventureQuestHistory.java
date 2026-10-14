package forge.adventure.util;

import java.util.HashSet;
import java.util.Set;

/** Successful quest completions, stored separately from serialized quest instances. */
public class AdventureQuestHistory implements SaveFileContent {
    private final Set<Integer> completed = new HashSet<>();

    public boolean contains(int questID) {
        return completed.contains(questID);
    }

    public void record(int questID) {
        if (questID > 0) {
            completed.add(questID);
        }
    }

    public void clear() {
        completed.clear();
    }

    @Override
    public void load(SaveFileData data) {
        clear();
        if (data != null && data.containsKey("completedQuestIds")) {
            int[] ids = (int[]) data.readObject("completedQuestIds");
            if (ids != null) {
                for (int id : ids) {
                    record(id);
                }
            }
        }
    }

    @Override
    public SaveFileData save() {
        SaveFileData data = new SaveFileData();
        data.storeObject("completedQuestIds", completed.stream().mapToInt(Integer::intValue).sorted().toArray());
        return data;
    }
}
