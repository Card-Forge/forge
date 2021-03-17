package forge.quest.data;

import forge.game.GameFormat;
import forge.gamemodes.quest.QuestMode;
import forge.gamemodes.quest.data.DeckConstructionRules;

@Deprecated
public final class QuestData extends forge.gamemodes.quest.data.QuestData {

    public QuestData() {
    }

    public QuestData(String name0, int diff, QuestMode mode0, GameFormat userFormat, boolean allowSetUnlocks,
            String startingWorld, DeckConstructionRules dcr) {
        super(name0, diff, mode0, userFormat, allowSetUnlocks, startingWorld, dcr);
    }

}
